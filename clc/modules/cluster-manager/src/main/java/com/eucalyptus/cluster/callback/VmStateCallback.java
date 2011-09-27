package com.eucalyptus.cluster.callback;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.vm.VmBundleTask.BundleState;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.eucalyptus.vm.VmInstances.TerminatedInstanceException;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class VmStateCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
  private static Logger LOG = Logger.getLogger( VmStateCallback.class );
  
  public VmStateCallback( ) {
    super( new VmDescribeType( ) {
      {
        regarding( );
      }
    } );
  }
  
  @Override
  public void fire( VmDescribeResponseType reply ) {
    reply.setOriginCluster( this.getSubject( ).getConfiguration( ).getName( ) );
    
    for ( VmInfo vmInfo : reply.getVms( ) ) {
      vmInfo.setPlacement( this.getSubject( ).getConfiguration( ).getName( ) );
      VmTypeInfo typeInfo = vmInfo.getInstanceType( );
      if ( typeInfo.getName( ) == null || "".equals( typeInfo.getName( ) ) ) {
        for ( VmType t : VmTypes.list( ) ) {
          if ( t.getCpu( ).equals( typeInfo.getCores( ) ) && t.getDisk( ).equals( typeInfo.getDisk( ) ) && t.getMemory( ).equals( typeInfo.getMemory( ) ) ) {
            typeInfo.setName( t.getName( ) );
          }
        }
      }
    }
    
    for ( final VmInfo runVm : reply.getVms( ) ) {
      VmStateCallback.handleReportedState( runVm );
    }
    
    final List<String> unreportedVms = VmStateCallback.findUnreported( reply );
    
    VmStateCallback.handleUnreported( unreportedVms );
  }
  
  public static List<String> findUnreported( VmDescribeResponseType reply ) {
    final List<String> unreportedVms = Lists.transform( VmInstances.list( ), new Function<VmInstance, String>( ) {
      
      @Override
      public String apply( final VmInstance input ) {
        return input.getInstanceId( );
      }
    } );
    
    final List<String> runningVmIds = Lists.transform( reply.getVms( ), new Function<VmInfo, String>( ) {
      @Override
      public String apply( final VmInfo arg0 ) {
        final String vmId = arg0.getImageId( );
        unreportedVms.remove( vmId );
        return vmId;
      }
    } );
    return unreportedVms;
  }
  
  public static void handleUnreported( final List<String> unreportedVms ) {
    for ( final String vmId : unreportedVms ) {
      EntityTransaction db1 = Entities.get( VmInstance.class );
      try {
        VmInstance vm = VmInstances.cachedLookup( vmId );
        if ( VmInstances.Timeout.UNREPORTED.apply( vm ) ) {
          VmInstances.terminated( vm );
        } else if ( VmInstances.Timeout.SHUTTING_DOWN.apply( vm ) ) {
          VmInstances.terminated( vm );
        } else if ( VmInstances.Timeout.TERMINATED.apply( vm ) ) {
          VmInstances.delete( vm );
        }
        db1.commit( );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        db1.rollback( );
      }
    }
  }
  
  public static void handleReportedState( final VmInfo runVm ) {
    final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
    EntityTransaction db = Entities.get( VmInstance.class );
    try {
      try {
        VmInstance vm = VmInstances.lookup( runVm.getInstanceId( ) );
        if ( VmState.SHUTTING_DOWN.equals( runVmState ) ) {
          VmStateCallback.handleReportedTeardown( vm, runVm );
        } else if ( VmStateSet.RUN.apply( vm ) ) {
          vm.doUpdate( ).apply( runVm );
        } else if ( !VmStateSet.RUN.apply( vm ) && VmStateSet.RUN.contains( runVmState ) && vm.lastUpdateMillis( ) > ( VmInstances.VOLATILE_STATE_TIMEOUT_SEC * 1000l ) ) {
          vm.doUpdate( ).apply( runVm );
        }
      } catch ( TerminatedInstanceException ex1 ) {
        LOG.trace( "Ignore state update to terminated instance: " + runVm.getInstanceId( ) );
      } catch ( NoSuchElementException ex1 ) {
        if ( VmStateSet.RUN.contains( runVmState ) ) {
          VmStateCallback.handleRestore( runVm );
        }
      } catch ( Exception ex1 ) {
        LOG.error( ex1, ex1 );
      }
      db.commit( );
    } catch ( Exception ex ) {
      LOG.trace( ex, ex );
      db.rollback( );
    }
  }

  public static void handleRestore( final VmInfo runVm ) {
    try {
      if ( VmInstances.cachedLookup( runVm.getInstanceId( ) ) == null ) {
        try {
          VmInstance.RestoreAllocation.INSTANCE.apply( runVm );
        } catch ( Exception ex ) {
          LOG.error( ex , ex );
        }
      }
    } catch ( Exception ex2 ) {
      LOG.trace( ex2, ex2 );
    }
  }
  
  private static void handleReportedTeardown( VmInstance vm, final VmInfo runVm ) throws TransactionException {
    /**
     * TODO:GRZE: based on current local instance state we need to handle reported
     * SHUTTING_DOWN state differently
     **/
    BundleState bundleState = BundleState.mapper.apply( runVm.getBundleTaskStateName( ) );
    if ( !BundleState.none.equals( bundleState ) ) {
      vm.getRuntimeState( ).updateBundleTaskState( bundleState );
      VmInstances.terminated( vm );
    } else if ( VmState.SHUTTING_DOWN.apply( vm ) ) {
      VmInstances.terminated( vm );
    } else if ( VmState.STOPPING.apply( vm ) ) {
      VmInstances.stopped( vm );
    } else if ( VmStateSet.RUN.apply( vm ) ) {
      VmInstances.shutDown( vm );
    }
  }
  
  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }
  
  public static class VmPendingCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
    private final Predicate<VmInstance> clusterMatch = new Predicate<VmInstance>( ) {
                                                       
                                                       @Override
                                                       public boolean apply( VmInstance arg0 ) {
                                                         return arg0.getPartition( ).equals( VmPendingCallback.this.getSubject( ).getConfiguration( ).getPartition( ) )
                                                       ;
                                                     }
                                                     };
    private final Predicate<VmInstance> volumeState  = new Predicate<VmInstance>( ) {
                                                       
                                                       @Override
                                                       public boolean apply( VmInstance input ) {
                                                         return input.eachVolumeAttachment( new Predicate<AttachedVolume>( ) {
                                                           @Override
                                                           public boolean apply( AttachedVolume arg0 ) {
                                                             return !arg0.getStatus( ).endsWith( "ing" );
                                                           }
                                                         } );
                                                       }
                                                     };
    private final Predicate<VmInstance> filter       = Predicates.and( Predicates.or( VmStateSet.CHANGING, this.volumeState ), this.clusterMatch );
    
    public VmPendingCallback( Cluster cluster ) {
      super( cluster );
      super.setRequest( new VmDescribeType( ) {
        {
          regarding( );
          EntityTransaction db = Entities.get( VmInstance.class );
          try {
            for ( VmInstance vm : Iterables.filter( VmInstances.list( ), VmPendingCallback.this.filter ) ) {
              this.getInstancesSet( ).add( vm.getInstanceId( ) );
            }
            db.commit( );
          } catch ( Exception ex ) {
            Logs.exhaust( ).error( ex, ex );
            db.rollback( );
          }
        }
      } );
      if ( this.getRequest( ).getInstancesSet( ).isEmpty( ) ) {
        throw new CancellationException( );
      }
    }
    
    @Override
    public void fire( VmDescribeResponseType reply ) {
      for ( final VmInfo runVm : reply.getVms( ) ) {
        VmStateCallback.handleReportedState( runVm );
      }
    }
    
    /**
     * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
     * @param t
     */
    @Override
    public void fireException( FailedRequestException t ) {
      LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
    }
    
  }
}

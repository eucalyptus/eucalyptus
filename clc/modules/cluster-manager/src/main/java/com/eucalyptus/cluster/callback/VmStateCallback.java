package com.eucalyptus.cluster.callback;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.async.SubjectMessageCallback;
import com.eucalyptus.vm.VmBundleTask.BundleState;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmInstances.TerminatedInstanceException;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class VmStateCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
  private static Logger               LOG                       = Logger.getLogger( VmStateCallback.class );
  private static final int            VM_INITIAL_REPORT_TIMEOUT = 300000;
  private static final int            VM_STATE_SETTLE_TIME      = 20000;
  private final Supplier<Set<String>> initialInstances;
  
  public VmStateCallback( ) {
    super( new VmDescribeType( ) {
      {
        regarding( );
      }
    } );
    this.initialInstances = createInstanceSupplier( this, partitionFilter( this ) );
  }
  
  private static Supplier<Set<String>> createInstanceSupplier( final StateUpdateMessageCallback<Cluster, ?, ?> cb, final Predicate<VmInstance> filter ) {
    return Suppliers.memoize( new Supplier<Set<String>>( ) {
      
      @Override
      public Set<String> get( ) {
        EntityTransaction db = Entities.get( VmInstance.class );
        try {
          Collection<VmInstance> clusterInstances =  Collections2.filter( VmInstances.list( ), filter );
          Collection<String> instanceNames = Collections2.transform( clusterInstances, CloudMetadatas.toDisplayName( ) );
          Set<String> ret = Sets.newHashSet( instanceNames );
          db.rollback( );
          return ret;
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
          db.rollback( );
          return Sets.newHashSet( );
        }
      }
    } );
  }
  
  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }
  
  @Override
  public void fire( VmDescribeResponseType reply ) {
    if ( Databases.isVolatile( ) ) {
      return;
    } else {
      reply.setOriginCluster( this.getSubject( ).getConfiguration( ).getName( ) );
      final Set<String> reportedInstances = Sets.newHashSet( );
      for ( VmInfo vmInfo : reply.getVms( ) ) {
        reportedInstances.add( vmInfo.getInstanceId( ) );
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
      
      final Set<String> unreportedInstances = Sets.newHashSet( Sets.difference( this.initialInstances.get( ), reportedInstances ) );
      final Set<String> restoreInstances = Sets.newHashSet( Sets.difference( reportedInstances, this.initialInstances.get( ) ) );
      for ( final VmInfo runVm : reply.getVms( ) ) {
        if ( Databases.isVolatile( ) ) {
          return;
        } else if ( this.initialInstances.get( ).contains( runVm.getInstanceId( ) ) ) {
          VmStateCallback.handleReportedState( runVm );
        } else if ( restoreInstances.contains( runVm.getInstanceId( ) ) ) {
          VmStateCallback.handleRestore( runVm );
        }
      }
      for ( final String vmId : unreportedInstances ) {
        if ( Databases.isVolatile( ) ) {
          return;
        } else {
          VmStateCallback.handleUnreported( vmId );
        }
      }
    }
  }
  
  private static void handleUnreported( final String vmId ) {
    EntityTransaction db1 = Entities.get( VmInstance.class );
    try {
      VmInstance vm = VmInstances.cachedLookup( vmId );
      if ( VmState.PENDING.apply( vm ) && vm.lastUpdateMillis( ) < VM_INITIAL_REPORT_TIMEOUT ) {
        //do nothing during first VM_INITIAL_REPORT_TIMEOUT millis of instance life
        db1.rollback( );
        return;
      } else if ( vm.isBlockStorage( ) && VmInstances.Timeout.UNREPORTED.apply( vm ) ) {
        VmInstances.stopped( vm );
      } else if ( VmState.STOPPING.apply( vm ) ) {
        VmInstances.stopped( vm );
      } else if ( VmState.SHUTTING_DOWN.apply( vm ) ) {
        VmInstances.terminated( vm );
      } else if ( VmInstances.Timeout.TERMINATED.apply( vm ) ) {
        VmInstances.delete( vm );
      } else if ( VmInstances.Timeout.SHUTTING_DOWN.apply( vm ) ) {
        VmInstances.terminated( vm );
      } else if ( VmInstances.Timeout.STOPPING.apply( vm ) ) {
        VmInstances.stopped( vm );
      } else if ( VmInstances.Timeout.UNREPORTED.apply( vm ) ) {
        VmInstances.terminated( vm );
      } else {
        db1.rollback( );
        return;
      }
      Entities.commit( db1 );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      db1.rollback( );
    }
  }
  
  private static void handleReportedState( final VmInfo runVm ) {
    final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
    try {
      EntityTransaction db = Entities.get( VmInstance.class );
      try {
        VmInstance vm = VmInstances.lookup( runVm.getInstanceId( ) );
        if ( VmInstances.Timeout.EXPIRED.apply( vm ) ) {
          if ( vm.isBlockStorage( ) ) {
            VmInstances.stopped( vm );
          } else {
            VmInstances.shutDown( vm );
          }
        } else if ( VmState.SHUTTING_DOWN.equals( runVmState ) ) {
          VmStateCallback.handleReportedTeardown( vm, runVm );
        } else if ( VmStateSet.RUN.apply( vm ) ) {
          vm.doUpdate( ).apply( runVm );
        } else if ( !VmStateSet.RUN.apply( vm ) && VmStateSet.RUN.contains( runVmState )
                    && vm.lastUpdateMillis( ) > ( VmInstances.VOLATILE_STATE_TIMEOUT_SEC * 1000l ) ) {
          vm.doUpdate( ).apply( runVm );
        } else {
          db.rollback( );
          return;
        }
        Entities.commit( db );
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
        db.rollback( );
        throw ex;
      }
    } catch ( TerminatedInstanceException ex1 ) {
      LOG.trace( "Ignore state update to terminated instance: " + runVm.getInstanceId( ) );
    } catch ( NoSuchElementException ex1 ) {
//      VmStateCallback.handleRestore( runVm );
    } catch ( Exception ex1 ) {
      LOG.error( ex1 );
      Logs.extreme( ).error( ex1, ex1 );
    }
  }
  
  private static void handleRestore( final VmInfo runVm ) {
    final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
    if ( VmStateSet.RUN.contains( runVmState ) ) {
      try {
        if ( VmInstances.cachedLookup( runVm.getInstanceId( ) ) != null ) {
          return;
        }
      } catch ( Exception ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
      try {
        VmInstance.RestoreAllocation.INSTANCE.apply( runVm );
      } catch ( Throwable ex ) {
        LOG.error( ex );
        Logs.extreme( ).error( ex, ex );
      }
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
    } else if ( VmStateSet.RUN.apply( vm ) && vm.getSplitTime( ) > VM_STATE_SETTLE_TIME ) {
      if ( vm.isBlockStorage( ) ) {
        VmInstances.stopped( vm );
      } else {
        VmInstances.shutDown( vm );
      }
    }
  }
  
  private static Predicate<VmInstance> stateSettleFilter( ) {
    return new Predicate<VmInstance>( ) {
      
      @Override
      public boolean apply( VmInstance input ) {
        return input.getCreationSplitTime( ) > VM_STATE_SETTLE_TIME;
      }
    };
  }
  
  private static Predicate<VmInstance> partitionFilter( final SubjectMessageCallback<Cluster, ?, ?> cb ) {
    return new Predicate<VmInstance>( ) {
      
      @Override
      public boolean apply( VmInstance arg0 ) {
        return arg0.getPartition( ).equals( cb.getSubject( ).getConfiguration( ).getPartition( ) );
      }
    };
  }
  
  public static class VmPendingCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
    @SuppressWarnings( "unchecked" )
    private final Predicate<VmInstance> filter = Predicates.and( VmStateSet.TORNDOWN.not( ), stateSettleFilter( ), partitionFilter( this ) );
    private final Supplier<Set<String>> initialInstances;
    
    public VmPendingCallback( Cluster cluster ) {
      super( cluster );
      this.initialInstances = createInstanceSupplier( this, this.filter );
      this.setRequest( new VmDescribeType( ) {
        {
          regarding( );
          this.getInstancesSet( ).addAll( VmPendingCallback.this.initialInstances.get( ) );
        }
      } );
      if ( this.getRequest( ).getInstancesSet( ).isEmpty( ) ) {
        throw new CancellationException( );
      }
    }
    
    @Override
    public void fire( VmDescribeResponseType reply ) {
      for ( final VmInfo runVm : reply.getVms( ) ) {
        if ( Databases.isVolatile( ) ) {
          return;
        } else if ( this.initialInstances.get( ).contains( runVm.getInstanceId( ) ) ) {
          VmStateCallback.handleReportedState( runVm );
        }
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
  
  @Override
  public void setSubject( Cluster subject ) {
    super.setSubject( subject );
    this.initialInstances.get( );
  }
}

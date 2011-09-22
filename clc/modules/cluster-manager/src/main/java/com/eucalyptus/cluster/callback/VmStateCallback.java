package com.eucalyptus.cluster.callback;

import java.util.List;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
import com.eucalyptus.vm.VmInstance.Reason;
import com.eucalyptus.vm.VmInstance.VmState;
import com.eucalyptus.vm.VmInstance.VmStateSet;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
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
      final VmState runVmState = VmState.Mapper.get( runVm.getStateName( ) );
      EntityTransaction db = Entities.get( VmInstance.class );
      try {
        try {
          VmInstance vm = VmInstances.lookup( runVm.getInstanceId( ) );
          try {
            if ( VmStateSet.RUN.apply( vm ) || VmStateSet.CHANGING.apply( vm ) ) {
              vm.doUpdate( ).apply( runVm );
            } else {
              continue;
            }
          } catch ( Exception ex ) {
            LOG.error( ex );
          }
        } catch ( Exception ex1 ) {
          if ( VmStateSet.RUN.contains( runVmState ) ) {
            VmInstance.RestoreAllocation.INSTANCE.apply( runVm );
          }
        }
        db.commit( );
      } catch ( Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db.rollback( );
      }
    }
    
    final List<String> unreportedVms = Lists.transform( VmInstances.listValues( ), new Function<VmInstance, String>( ) {
      
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
    
    for ( final String vmId : unreportedVms ) {
      EntityTransaction db1 = Entities.get( VmInstance.class );
      try {
        VmInstance vm = VmInstances.lookup( vmId );
        if ( VmInstances.Timeout.UNREPORTED.apply( vm ) ) {
          VmInstances.terminated( vm );
        } else if ( VmInstances.Timeout.SHUTTING_DOWN.apply( vm ) ) {
          VmInstances.terminated( vm );
        } else if ( VmInstances.Timeout.TERMINATED.apply( vm ) ) {
          VmInstances.delete( vm );
        } else if ( VmInstances.Timeout.TERMINATED.apply( vm ) ) {
          VmInstances.delete( vm );
        } else if ( VmState.SHUTTING_DOWN.apply( vm ) ) {
          VmInstances.terminated( vm );
        } else if ( VmState.STOPPED.apply( vm ) ) {
          VmInstances.stopped( vm );
        }
        db1.commit( );
      } catch ( final Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db1.rollback( );
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

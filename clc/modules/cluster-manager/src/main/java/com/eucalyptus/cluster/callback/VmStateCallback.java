package com.eucalyptus.cluster.callback;

import java.util.List;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstance.Reason;
import com.eucalyptus.cluster.VmInstance.VmState;
import com.eucalyptus.cluster.VmInstance.VmStateSet;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.vm.VmType;
import com.eucalyptus.vm.VmTypes;
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
      final VmState state = VmState.Mapper.get( runVm.getStateName( ) );
      
      final EntityTransaction db1 = Entities.get( VmInstance.class );
      try {
        try {
          final VmInstance vm = Entities.uniqueResult( VmInstance.named( null, runVm.getInstanceId( ) ) );
          vm.doUpdate( ).apply( runVm );
        } catch ( final Exception ex ) {
          if ( VmStateSet.RUN.contains( state ) ) {
            VmInstance.RestoreAllocation.INSTANCE.apply( runVm );
          }
        }
        db1.commit( );
      } catch ( final Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        db1.rollback( );
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
        if ( VmStateSet.RUN.apply( vm ) ) {
          //noop.
        } else if ( VmState.SHUTTING_DOWN.apply( vm ) ) {
          vm.setState( VmState.TERMINATED, Reason.EXPIRED );
          vm = VmInstances.delete( vm );//TODO:GRZE:OMG:TEMPORARYA!!?@!!@!11
          
        } else if ( VmState.TERMINATED.apply( vm ) && vm.getSplitTime( ) > VmInstances.BURY_TIME ) {
          VmInstances.delete( vm );
        } else if ( VmState.BURIED.apply( vm ) ) {
          VmInstances.delete( vm );
        } else if ( VmStateSet.DONE.apply( vm ) && vm.getSplitTime( ) > VmInstances.SHUT_DOWN_TIME ) {
          VmInstances.terminate( vm );
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

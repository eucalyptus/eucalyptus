package com.eucalyptus.cluster.callback;

import java.util.concurrent.CancellationException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstance.Reason;
import com.eucalyptus.cluster.VmInstance.VmState;
import com.eucalyptus.cluster.VmInstance.VmStateSet;
import com.eucalyptus.cluster.VmNetworkConfig;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.vm.VmInstances;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;

public class VmPendingCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
  private static Logger               LOG          = Logger.getLogger( VmPendingCallback.class );
  
  private final Predicate<VmInstance> clusterMatch = new Predicate<VmInstance>( ) {
                                                     
                                                     @Override
                                                     public boolean apply( VmInstance arg0 ) {
                                                       return arg0.getPartition( ).equals( VmPendingCallback.this.getSubject( ).getConfiguration( ).getPartition( ) )
                                                     ;
                                                   }
                                                   };
  
  public VmPendingCallback( Cluster cluster ) {
    super( cluster );
    super.setRequest( new VmDescribeType( ) {
      {
        regarding( );
        Predicate<VmInstance> filter = Predicates.and( VmStateSet.CHANGING, VmPendingCallback.this.clusterMatch );
      }
    } );
    if ( this.getRequest( ).getInstancesSet( ).isEmpty( ) ) {
      throw new CancellationException( );
    }
  }
  
  @Override
  public void fire( VmDescribeResponseType reply ) {
    for ( final VmInfo runVm : reply.getVms( ) ) {
      runVm.setPlacement( this.getSubject( ).getConfiguration( ).getName( ) );
      VmState state = VmState.Mapper.get( runVm.getStateName( ) );
      EntityTransaction db = Entities.get( VmInstance.class );
      try {
        final VmInstance vm = VmInstances.lookup( runVm.getInstanceId( ) );
        vm.setServiceTag( runVm.getServiceTag( ) );
        if ( VmStateSet.RUN.apply( vm ) || VmStateSet.CHANGING.apply( vm ) ) {
          vm.doUpdate( ).apply( runVm );
        } else if ( VmInstances.Timeout.SHUTTING_DOWN.apply( vm ) ) {
          VmInstances.terminated( vm );
        } else if ( VmState.SHUTTING_DOWN.apply( vm ) && VmState.SHUTTING_DOWN.equals( state ) ) {
          VmInstances.terminated( vm );
        } else if ( VmStateSet.RUN.apply( vm ) &&  VmStateSet.RUN.contains( state ) ) { 
          if ( !VmNetworkConfig.DEFAULT_IP.equals( runVm.getNetParams( ).getIpAddress( ) ) ) {
            vm.updateAddresses( runVm.getNetParams( ).getIpAddress( ), runVm.getNetParams( ).getIgnoredPublicIp( ) );
          }
          vm.setState( VmState.Mapper.get( runVm.getStateName( ) ), Reason.APPEND, "UPDATE" );
          vm.updateVolumeAttachments( runVm.getVolumes( ) );
        }
        db.commit( );
      } catch ( Exception ex ) {
        LOG.debug( "Ignoring update for uncached vm: " + runVm.getInstanceId( ) );
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

package com.eucalyptus.cluster.callback;

import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.VmTypes;
import com.eucalyptus.entities.VmType;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.vm.SystemState;
import com.eucalyptus.vm.VmState;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

public class VmPendingCallback extends StateUpdateMessageCallback<Cluster, VmDescribeType, VmDescribeResponseType> {
  private static Logger LOG = Logger.getLogger( VmPendingCallback.class );
  
  public VmPendingCallback( ) {
    this.setRequest( new VmDescribeType( ) {
      {
        regarding( );
        Iterables.all( VmInstances.getInstance( ).listKeys( ), new Predicate<String>() {
          @Override
          public boolean apply( String arg0 ) {
            VmInstance vm = VmInstances.getInstance( ).lookup( arg0 );
            if( vm.getPlacement( ).equals( VmPendingCallback.this.getSubject( ).getName( ) ) ) {
              if( VmState.PENDING.equals( vm.getState( ) ) 
                  || vm.getState( ).ordinal( ) > VmState.RUNNING.ordinal( ) ) {
                VmPendingCallback.this.getRequest( ).getInstancesSet( ).add( arg0 );
              }
            }
            return false;
          }} ) ;
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
    SystemState.handle( reply );
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

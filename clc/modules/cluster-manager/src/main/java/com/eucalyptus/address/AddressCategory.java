package com.eucalyptus.address;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.async.Callback;
import com.eucalyptus.util.async.Callbacks;
import com.eucalyptus.util.async.NOOP;
import com.eucalyptus.util.async.Request;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class AddressCategory {
  private static Logger LOG = Logger.getLogger( AddressCategory.class );
  
  @SuppressWarnings( "unchecked" )
  public static Request unassign( final Address addr ) {
    final String instanceId = addr.getInstanceId( );
    if( !VmInstance.DEFAULT_IP.equals( addr.getInstanceAddress( ) ) ) {
      return Callbacks.newClusterRequest( addr.unassign( ).getCallback( ) ).then( new Callback.Success<BaseMessage>( ) {
        public void fire( BaseMessage response ) {
          try {
            VmInstance vm = VmInstances.getInstance( ).lookup( instanceId );
            EventRecord.here( AddressCategory.class, EventClass.ADDRESS, EventType.ADDRESS_UNASSIGNING )
               .withDetails( vm.getOwnerId( ), addr.getName( ), "instanceid", vm.getInstanceId( ) )
               .withDetails( "type", addr.isSystemOwned( ) ? "SYSTEM" : "USER" )
               .withDetails( "cluster", addr.getCluster( ) );
            Addresses.system( vm );
          } catch ( NoSuchElementException e ) {}
        }
      } );
    } else {
      return Callbacks.newClusterRequest( new NOOP() );
    }

  }
  
  @SuppressWarnings( "unchecked" )
  public static Request<BaseMessage, BaseMessage> assign( final Address addr, final VmInstance vm ) {
    EventRecord.here( AddressCategory.class, EventClass.ADDRESS, EventType.ADDRESS_ASSIGNING )
               .withDetails( vm.getOwnerId( ), addr.getName( ), "instanceid", vm.getInstanceId( ) )
               .withDetails( "type", addr.isSystemOwned( ) ? "SYSTEM" : "USER" )
               .withDetails( "cluster", addr.getCluster( ) );
    return Callbacks.newClusterRequest( addr.assign( vm.getInstanceId( ), vm.getPrivateAddress( ) ).getCallback( ) ).then( new Callback.Success<BaseMessage>() {
      public void fire( BaseMessage response ) {
        vm.updatePublicAddress( addr.getName( ) );
      }
    });
  }
  
}

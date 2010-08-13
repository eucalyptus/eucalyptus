package com.eucalyptus.address;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.SuccessCallback;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.cluster.VmInstances;
import com.eucalyptus.cluster.callback.QueuedEventCallback;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class AddressCategory {
  private static Logger LOG = Logger.getLogger( AddressCategory.class );
  
  @SuppressWarnings( "unchecked" )
  public static QueuedEventCallback unassign( final Address addr ) {
    final String instanceId = addr.getInstanceId( );
    final boolean systemOwned = addr.isSystemOwned( );
    if( !VmInstance.DEFAULT_IP.equals( addr.getInstanceAddress( ) ) ) {
      return addr.unassign( ).getCallback( ).then( new SuccessCallback( ) {
        public void apply( BaseMessage response ) {
          try {
            VmInstance vm = VmInstances.getInstance( ).lookup( instanceId );
            EventRecord.here( AddressCategory.class, EventClass.ADDRESS, EventType.ADDRESS_UNASSIGNING, "user="+vm.getOwnerId( ), "address="+addr.getName( ), "instanceid="+vm.getInstanceId( ), addr.isSystemOwned( ) ? "SYSTEM":"USER" ).info( );
            Addresses.system( vm );
          } catch ( NoSuchElementException e ) {}
        }
      } );
    } else if( systemOwned ) {
      addr.release( );
      return new QueuedEventCallback.NOOP();
    } else {
      return new QueuedEventCallback.NOOP();
    }
  }
  
  @SuppressWarnings( "unchecked" )
  public static QueuedEventCallback assign( final Address addr, final VmInstance vm ) {
    EventRecord.here( AddressCategory.class, EventClass.ADDRESS, EventType.ADDRESS_ASSIGNING, "user="+vm.getOwnerId( ), "address="+addr.getName( ), "instanceid="+vm.getInstanceId( ), addr.isSystemOwned( ) ? "SYSTEM":"USER", addr.toString( ) ).info( );
    return addr.assign( vm.getInstanceId( ), vm.getPrivateAddress( ) ).getCallback( ).then( new SuccessCallback() {
      public void apply( BaseMessage response ) {
        vm.updatePublicAddress( addr.getName( ) );
      }
    });
  }
  
}

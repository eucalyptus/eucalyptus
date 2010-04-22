package com.eucalyptus.address;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.SuccessCallback;
import com.eucalyptus.cluster.callback.QueuedEventCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class AddressCategory {
  private static Logger LOG = Logger.getLogger( AddressCategory.class );
  
  @SuppressWarnings( "unchecked" )
  public static QueuedEventCallback unassign( final Address addr ) {
    final String instanceId = addr.getInstanceId( );
      return addr.unassign( ).getCallback( ).then( new SuccessCallback( ) {
        public void apply( BaseMessage response ) {
          try {
            VmInstance vm = VmInstances.getInstance( ).lookup( instanceId );
            Addresses.system( vm );
          } catch ( NoSuchElementException e ) {}
        }
      } );
  }
  
  @SuppressWarnings( "unchecked" )
  public static QueuedEventCallback assign( final Address addr, final VmInstance vm ) {
    return addr.assign( vm.getInstanceId( ), vm.getNetworkConfig( ).getIpAddress( ) ).getCallback( ).then( new SuccessCallback() {
      public void apply( BaseMessage response ) {
        vm.getNetworkConfig( ).setIgnoredPublicIp( addr.getName( ) );
      }
    });
  }
  
}

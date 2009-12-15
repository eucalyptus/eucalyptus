package edu.ucsb.eucalyptus.cloud.cluster;

import java.util.NoSuchElementException;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.net.util.ClusterAddressInfo;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.UnassignAddressType;

public class OrphanAddressCallback extends QueuedEventCallback<UnassignAddressType> {
  private String address;
  
  public OrphanAddressCallback( String address ) {
    this.address = address;
  }
  public OrphanAddressCallback( Address addr ) {
    this( addr.getName( ) );
    super.setRequest( new UnassignAddressType( addr.getName( ), addr.getInstanceAddress( ) ) );
  }
  public OrphanAddressCallback( ClusterAddressInfo addrInfo ) {
    this( addrInfo.getAddress( ) );
    super.setRequest( new UnassignAddressType( addrInfo.getAddress( ), addrInfo.getInstanceIp( ) ) );
  }
  
  @Override
  public void fail( Throwable throwable ) {
    LOG.error( "Error unassigning address: " + super.getRequest( ).toString( ) );
    LOG.error( throwable, throwable );
    this.finish( );
  }
  
  private void finish( ) {
    try {
      Address addr = Addresses.getInstance( ).lookup( address );
      addr.clearPending( );
    } catch ( NoSuchElementException e ) {
      LOG.debug( e, e );
    }
  }
  
  @Override
  public void prepare( UnassignAddressType msg ) throws Exception {}
  
  @Override
  public void verify( EucalyptusMessage msg ) throws Exception {
    this.finish( );
    if( !msg.get_return( ) ) {
      this.fail( new RuntimeException("Cluster responded with false.") );
    }
  }
  
}

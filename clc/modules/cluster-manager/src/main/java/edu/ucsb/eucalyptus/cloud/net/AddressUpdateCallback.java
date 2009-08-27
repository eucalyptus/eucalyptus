package edu.ucsb.eucalyptus.cloud.net;

import edu.ucsb.eucalyptus.cloud.Pair;
import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.msgs.*;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;

import org.apache.log4j.Logger;

import java.util.NoSuchElementException;

public class AddressUpdateCallback extends QueuedEventCallback<DescribePublicAddressesType> implements Runnable {
  private static Logger LOG         = Logger.getLogger( AddressUpdateCallback.class );
  private static int    SLEEP_TIMER = 5 * 1000;

  public AddressUpdateCallback( ClusterConfiguration config ) {
    super( config );
  }

  public void process( final Client cluster, final DescribePublicAddressesType msg ) throws Exception {
    try {
      DescribePublicAddressesResponseType reply = ( DescribePublicAddressesResponseType ) cluster.send( msg );
      if ( reply.get_return( ) ) {
        EucalyptusProperties.disableNetworking = false;
        for ( Pair p : Pair.getPaired( reply.getAddresses( ), reply.getMapping( ) ) )
          try {
            Address blah = Addresses.getInstance( ).lookup( p.getLeft( ) );
            blah.setInstanceAddress( p.getRight( ) );
          } catch ( NoSuchElementException ex ) {
            Addresses.getInstance( ).registerDisabled( new Address( p.getLeft( ), super.getConfig( ).getName( ) ) );
          }
      } else {
        if ( !EucalyptusProperties.disableNetworking ) {
          LOG.warn( "Response from cluster [" + super.getConfig( ).getName( ) + "]: " + reply.getStatusMessage( ) );
        }
        EucalyptusProperties.disableNetworking = true;
      }
    } catch ( Exception e ) {
      LOG.error( e );
    }
    this.notifyHandler( );
  }

  public void run( ) {
    do {
      Cluster cluster = Clusters.getInstance( ).lookup( this.getConfig( ).getName( ) );
      DescribePublicAddressesType drMsg = new DescribePublicAddressesType( );
      drMsg.setUserId( Component.eucalyptus.name() );
      drMsg.setEffectiveUserId( Component.eucalyptus.name() );
      cluster.getMessageQueue( ).enqueue( new QueuedEvent( this, drMsg ) );
      this.waitForEvent( );
    } while ( !this.isStopped( ) && this.sleep( SLEEP_TIMER ) );
  }

}

package edu.ucsb.eucalyptus.cloud.net;

import edu.ucsb.eucalyptus.cloud.Pair;
import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.client.Client;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.axis2.AxisFault;
import org.apache.log4j.Logger;

import java.util.NoSuchElementException;

public class AddressUpdateCallback extends QueuedEventCallback<DescribePublicAddressesType> implements Runnable {

  private static Logger LOG = Logger.getLogger( AddressUpdateCallback.class );

  private Cluster parent;
  private static int SLEEP_TIMER = 5 * 1000;

  public AddressUpdateCallback( final Cluster parent ) {
    this.parent = parent;
  }

  public void process( final Client cluster, final DescribePublicAddressesType msg ) throws Exception {
    try {
      DescribePublicAddressesResponseType reply = ( DescribePublicAddressesResponseType ) cluster.send( msg );
      if ( reply.get_return() )
        for ( Pair p : Pair.getPaired( reply.getAddresses(), reply.getMapping() ) )
          try {
            Address blah = Addresses.getInstance().lookup( p.getLeft() );
            blah.setInstanceAddress( p.getRight() );
          } catch ( NoSuchElementException ex ) {
            Addresses.getInstance().registerDisabled( new Address( p.getLeft(), this.parent.getName() ) );
          }
    }
    catch ( AxisFault axisFault ) {
      LOG.error( axisFault );
    }
    this.notifyHandler();
  }

  public void run() {
    do {
      DescribePublicAddressesType drMsg = new DescribePublicAddressesType();
      drMsg.setUserId( EucalyptusProperties.NAME );
      drMsg.setEffectiveUserId( EucalyptusProperties.NAME );

      this.parent.getMessageQueue().enqueue( new QueuedEvent( this, drMsg ) );
      this.waitForEvent();
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );
  }

}


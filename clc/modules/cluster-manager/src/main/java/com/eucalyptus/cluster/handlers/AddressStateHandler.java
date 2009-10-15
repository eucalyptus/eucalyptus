package com.eucalyptus.cluster.handlers;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.net.Addresses;
import com.eucalyptus.net.util.AddressUtil;
import com.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpResponse;

import edu.ucsb.eucalyptus.cloud.Pair;
import edu.ucsb.eucalyptus.cloud.cluster.UnassignAddressCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.cloud.cluster.VmInstances;
import edu.ucsb.eucalyptus.cloud.entities.Address;
import edu.ucsb.eucalyptus.constants.VmState;
import edu.ucsb.eucalyptus.msgs.DescribePublicAddressesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribePublicAddressesType;

@ChannelPipelineCoverage( "one" )
public class AddressStateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NetworkStateHandler.class );
  
  public AddressStateHandler( Cluster cluster ) throws BindingException {
    super( cluster );
  }
  
  @Override
  public void trigger( ) {
    DescribePublicAddressesType drMsg = new DescribePublicAddressesType( );
    drMsg.setUserId( Component.eucalyptus.name( ) );
    drMsg.setEffectiveUserId( Component.eucalyptus.name( ) );
    this.write( drMsg );
  }
  
  @Override
  public void fireEvent( Event event ) {
    if ( this.timedTrigger( event ) ) {
      this.trigger( );
    } else if ( event instanceof GenericEvent ) {
      GenericEvent<Cluster> g = ( GenericEvent<Cluster> ) event;
      if ( !g.matches( this.getCluster( ) ) ) {
        return;
      }
      if ( g instanceof NewClusterEvent ) {
        this.trigger( );
      }
    } else {
      LOG.trace( "Ignoring unknown event: " + LogUtil.dumpObject( event ) );
    }
  }

  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    if ( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse resp = ( MappingHttpResponse ) e.getMessage( );
      DescribePublicAddressesResponseType reply = ( DescribePublicAddressesResponseType ) resp.getMessage( );
      if ( reply.get_return( ) ) {
        EucalyptusProperties.disableNetworking = false;
        if( !AddressUtil.initialize( this.getCluster( ).getName( ), Pair.getPaired( reply.getAddresses( ), reply.getMapping( ) ) ) ) {
          AddressUtil.update( this.getCluster( ).getName( ), Pair.getPaired( reply.getAddresses( ), reply.getMapping( ) ) );
        }
      } else {
        LOG.warn( "Response from cluster [" + this.getCluster( ).getName( ) + "]: " + reply.getStatusMessage( ) );
        EucalyptusProperties.disableNetworking = true;
      }
      this.verified = true;
    }
  }

    
  public static Address getAddress( String cluster, Pair p ) {
    Address address;
    try {
      try {
        address = Addresses.getInstance( ).lookup( p.getLeft( ) );
      } catch ( NoSuchElementException e1 ) {
        address = Addresses.getInstance( ).lookupDisabled( p.getLeft( ) );
      }
    } catch ( NoSuchElementException e ) {
      LOG.debug( e );
      address = new Address( p.getLeft( ), cluster );
      address.init( );
    }
    return address;
  }
  
  @Override
  public void advertiseEvent( Event event ) {}
  
}

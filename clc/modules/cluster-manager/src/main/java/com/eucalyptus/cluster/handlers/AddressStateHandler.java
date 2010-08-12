package com.eucalyptus.cluster.handlers;

import java.util.List;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.address.Address;
import com.eucalyptus.address.Addresses;
import com.eucalyptus.address.ClusterAddressInfo;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.util.LogUtil;
import edu.ucsb.eucalyptus.cloud.Pair;
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
    if ( event instanceof ClockTick ) {
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
      this.getCluster( ).getState( ).setPublicAddressing( reply.get_return( ) );
      
      if ( reply.get_return( ) ) {
        List<ClusterAddressInfo> addrInfo = ClusterAddressInfo.fromLists( reply.getAddresses( ), reply.getMapping( ) );
        if ( addrInfo != null ) {
          Addresses.getAddressManager( ).update( this.getCluster( ), addrInfo );
        }
      } else {
        LOG.warn( "Response from cluster [" + this.getCluster( ).getName( ) + "]: " + reply.getStatusMessage( ) );
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

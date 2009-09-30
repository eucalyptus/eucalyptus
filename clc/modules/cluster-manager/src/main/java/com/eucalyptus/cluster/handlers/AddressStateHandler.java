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
  
  private static ConcurrentNavigableMap<String,Integer> orphans = new ConcurrentSkipListMap<String,Integer>();
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
        AddressUtil.initialize( );
        for ( Pair p : Pair.getPaired( reply.getAddresses( ), reply.getMapping( ) ) ) {
          Address address = getAddress( p );
          try {
            InetAddress addr = Inet4Address.getByName( p.getRight( ) );
            address.setInstanceAddress( p.getRight( ) );
            VmInstance vm = getVmInstance( p );
            if ( vm != null ) {
              orphans.remove( address.getName( ) );
            } else if( !addr.isLoopbackAddress( ) && !this.checkForPendingVm( address ) ) {
              handleOrphan( address );
            } else {
              orphans.remove( address.getName( ) );
            }
          } catch ( UnknownHostException e1 ) {
            LOG.debug( e1, e1 );
            orphans.remove( address.getName( ) );
          }
        }
      } else {
        LOG.warn( "Response from cluster [" + this.getCluster( ).getName( ) + "]: " + reply.getStatusMessage( ) );
        EucalyptusProperties.disableNetworking = true;
      }
      this.verified = true;
    }
  }

  private void handleOrphan( Address address ) {
    Integer orphanCount = 1;
    orphanCount = orphans.putIfAbsent( address.getName( ), orphanCount );
    orphans.put( address.getName( ), orphanCount + 1 );
    LOG.warn( "Found orphaned public ip address: " + address + " count=" + orphanCount );
    if( orphanCount > 10 ) {
      orphans.remove( address.getName( ) );
      Clusters.dispatchClusterEvent( this.getCluster( ), new UnassignAddressCallback( address ) );
    }
  }

  private boolean checkForPendingVm( Address addr ) {
    for ( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
      if ( vm.getNetworkConfig( ).getIpAddress( ).equals( addr.getInstanceAddress( ) ) && ( VmState.PENDING.equals( vm.getState( ) ) || VmState.RUNNING.equals( vm.getState( ) ) )) {
        return true;
      }
    }
    return false;
  }
  
  private VmInstance getVmInstance( Pair p ) {
    VmInstance assignee = null;
    for ( VmInstance vm : VmInstances.getInstance( ).listValues( ) ) {
      if ( vm.getNetworkConfig( ).getIpAddress( ).equals( p.getLeft( ) ) ) {
        assignee = vm;
      }
    }
    return assignee;
  }
  
  private Address getAddress( Pair p ) {
    Address address;
    try {
      try {
        address = Addresses.getInstance( ).lookup( p.getLeft( ) );
      } catch ( NoSuchElementException e1 ) {
        address = Addresses.getInstance( ).lookupDisabled( p.getLeft( ) );
      }
    } catch ( NoSuchElementException e ) {
      LOG.debug( e );
      address = new Address( p.getLeft( ), this.getCluster( ).getName( ) );
      address.init( );
    }
    return address;
  }
  
  @Override
  public void advertiseEvent( Event event ) {}
  
}

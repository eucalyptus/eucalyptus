package com.eucalyptus.cluster.handlers;

import java.nio.channels.AlreadyConnectedException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.cluster.util.ClusterUtil;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpResponse;

import edu.ucsb.eucalyptus.msgs.GetKeysResponseType;
import edu.ucsb.eucalyptus.msgs.GetKeysType;

@ChannelPipelineCoverage("one")
public class ClusterCertificateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG = Logger.getLogger( ClusterCertificateHandler.class );
  private boolean verified = false;
  public ClusterCertificateHandler( Cluster cluster ) throws BindingException {
    super( cluster, false );
  }

  public void trigger( ) {
    this.write( new GetKeysType( "self" ) );
  }

  @Override
  public void advertiseEvent( Event event ) {
  }

  @SuppressWarnings( "unchecked" )
  @Override
  public void fireEvent( Event event ) {
    if ( this.timedTrigger( event ) ) {
      this.trigger( );
    } else if ( event instanceof GenericEvent ) {
      GenericEvent<Cluster> g = (GenericEvent<Cluster>) event;
      if( !g.matches( this.getCluster( ) ) ) {
        return;
      }
      if( g instanceof NewClusterEvent && !this.verified ) {
        this.trigger();
      } else if ( event instanceof TeardownClusterEvent ) {
        this.verified = false;
        this.cleanup( );
      }
    } else {
      LOG.debug( "Ignoring event which doesn't belong to me: " + LogUtil.dumpObject( event ) );
    }
  }

  @Override
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    if( e.getCause( ) instanceof AlreadyConnectedException ) {
    } else {
      this.exceptionCaught( e.getCause( ) );
    }
  }

  @Override
  public void exceptionCaught( Throwable cause ) {
    LOG.info( cause, cause );
  }

  @Override
  public void downstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    LOG.info( e.getMessage( ) );
    ctx.sendDownstream( e );
  }

  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    if( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse resp = (MappingHttpResponse) e.getMessage( );
      GetKeysResponseType msg = (GetKeysResponseType) resp.getMessage( );
      boolean certs = ClusterUtil.checkCerts( msg, this.getCluster( ) );
      if( certs && !this.verified ) {
        try {
          ClusterUtil.registerClusterStateHandler( this.getCluster( ), new NetworkStateHandler( this.getCluster( ) ) );
        } catch ( Exception e1 ) {
          LOG.error( e1, e1 );
        }
        LOG.info( LogUtil.header( "Starting threads for cluster: " + this.getCluster( ) ) );
        this.getCluster( ).getThreadGroup( ).startScaryThreads( );
        this.verified = true;
      } 
    } else {
      LOG.info( "Received unknown message type. " + e.getMessage( ) );
    }    
    ctx.getChannel( ).close( );
  }

}

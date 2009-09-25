package com.eucalyptus.cluster.handlers;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.timeout.IdleStateEvent;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.MappingHttpResponse;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.binding.BindingManager;
import com.eucalyptus.ws.client.NioBootstrap;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.NioHttpResponseDecoder;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.ClusterWsSecHandler;
import com.eucalyptus.ws.util.ChannelUtil;

public abstract class AbstractClusterMessageDispatcher implements ChannelPipelineFactory, ChannelUpstreamHandler, ChannelDownstreamHandler, EventListener {
  private static Logger     LOG            = Logger.getLogger( AbstractClusterMessageDispatcher.class );
  private NioBootstrap      clientBootstrap;
  private Cluster           cluster;
  private Binding           binding;
  private InetSocketAddress remoteAddr;
  private String            hostName;
  private int               port;
  private String            servicePath;
  protected boolean         verified       = false;
  private String            actionPrefix;
  private static String     SECURE_NAME    = "EucalyptusCC";
  private static String     SECURE_NC_NAME = "EucalyptusNC";
  private static String     INSECURE_NAME  = "EucalyptusGL";
  
  public AbstractClusterMessageDispatcher( Cluster cluster, boolean secure ) throws BindingException {
    this( cluster );
    if ( !secure ) {
      this.servicePath = makeInsecure( this.servicePath );
      this.actionPrefix = makeInsecure( this.actionPrefix );
    }
  }
  
  protected static String makeInsecure( String input ) {
    return input.replaceAll( SECURE_NAME, INSECURE_NAME ).replaceAll( SECURE_NC_NAME, INSECURE_NAME );
  }
  
  public AbstractClusterMessageDispatcher( Cluster cluster ) throws BindingException {
    this( );
    this.cluster = cluster;
    ClusterConfiguration config = this.getCluster( ).getConfiguration( );
    this.hostName = config.getHostName( );
    this.port = config.getPort( );
    this.servicePath = config.getServicePath( );
    this.actionPrefix = SECURE_NAME;
    this.remoteAddr = new InetSocketAddress( cluster.getConfiguration( ).getHostName( ), cluster.getConfiguration( ).getPort( ) );
  }
  
  private AbstractClusterMessageDispatcher( ) throws BindingException {
    this.clientBootstrap = ChannelUtil.getClientBootstrap( this );
    this.binding = BindingManager.getBinding( "eucalyptus_ucsb_edu" );
  }
  
  public abstract void trigger( );
  public abstract void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e );
  
  @Override
  public ChannelPipeline getPipeline( ) throws Exception {//FIXME: remove all these repetitions.
    ChannelPipeline pipeline = Channels.pipeline( );
    ChannelUtil.addPipelineMonitors( pipeline, 30 );
    pipeline.addLast( "encoder", new HttpRequestEncoder( ) );
    pipeline.addLast( "decoder", new NioHttpResponseDecoder( ) );
    pipeline.addLast( "aggregator", new HttpChunkAggregator( 1048576 ) );
    pipeline.addLast( "serializer", new SoapMarshallingHandler( ) );
    pipeline.addLast( "wssec", new ClusterWsSecHandler( ) );
    pipeline.addLast( "addressing", new AddressingHandler( this.actionPrefix + "#" ) );
    pipeline.addLast( "soap", new SoapHandler( ) );
    pipeline.addLast( "binding", new BindingHandler( this.binding ) );
    pipeline.addLast( "handler", this );
    return pipeline;
  }
  private ChannelFutureListener CLEAR_PENDING = new ChannelFutureListener( ) {
    @Override
    public void operationComplete( ChannelFuture future ) throws Exception {
      AbstractClusterMessageDispatcher.this.inFlightMessage.lazySet( false );                
    }
  };  
  private AtomicBoolean inFlightMessage = new AtomicBoolean( );
  
  public void write( Object o ) {
    if( inFlightMessage.compareAndSet( false, true ) ) {
      LOG.debug( this.hashCode() + " -> Sending request: " + LogUtil.lineObject( o ) );
      ChannelFuture channelConnectFuture = this.clientBootstrap.connect( this.remoteAddr );
      HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostName, this.port, this.servicePath, o );
      channelConnectFuture.addListener( ChannelUtil.WRITE( request ) );
      channelConnectFuture.getChannel( ).getCloseFuture( ).addListener( CLEAR_PENDING );
    } else {
      LOG.debug( this.hashCode() + " -> Discarding subsequent write on expired channel." + LogUtil.lineObject( o ) );
    }
  }
  
  @Override
  public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    LOG.trace( this.hashCode() + " -> Send upstream: " + e.getClass( ) );
    ctx.sendDownstream( e );
  }
  
  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    LOG.trace( this.hashCode() + " -> Received upstream: " + e.getClass( ) );
    if ( e instanceof MessageEvent ) {
      if( ( (MessageEvent)e).getMessage( ) instanceof MappingHttpResponse ) {
        MappingHttpResponse response = (MappingHttpResponse) ( (MessageEvent)e).getMessage( );
        if( HttpResponseStatus.OK.equals( response.getStatus( ) ) ) {
          this.upstreamMessage( ctx, ( MessageEvent ) e );          
        } else {
          throw new EucalyptusClusterException( response.getMessageString( ) );
        }
      }
      e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
      ctx.sendUpstream( e );
    } else if ( e instanceof ExceptionEvent ) {
      this.exceptionCaught( ctx, ( ExceptionEvent ) e );
      ctx.getChannel( ).close( );
      ctx.sendUpstream( e );
    } else if ( e instanceof IdleStateEvent ) {
      e.getChannel( ).close( );
      ctx.sendUpstream( e );
    } else if ( e instanceof ChannelStateEvent ) {
      ChannelStateEvent cse = (ChannelStateEvent) e;
      switch(cse.getState( )) {
        case CONNECTED: {
          if( cse.getValue( ) == null ) {
            //disconnected.
          }
        } break;
        case OPEN: {
          if( !Boolean.TRUE.equals( cse.getValue( ) ) ) {
            LOG.debug( "-> Operation completed." );
          }
        } break;
        case BOUND: { ctx.sendUpstream( e ); } break;
        case INTEREST_OPS: { ctx.sendUpstream( e ); } break;
        default:
          ctx.sendUpstream(e);
      }
    } else {
      ctx.sendUpstream( e );      
    }
  }
      
  protected void fireTimedStatefulTrigger( Event event ) {
    if ( this.timedTrigger( event ) ) {
      LOG.debug( "Tick/tock: " + LogUtil.lineObject( this ) );
      this.trigger( );
    } else if ( event instanceof GenericEvent ) {
      LOG.debug( "Fire event: " + LogUtil.lineObject( this ) );
      GenericEvent<Cluster> g = ( GenericEvent<Cluster> ) event;
      if ( !g.matches( this.getCluster( ) ) ) {
        return;
      }
      if ( g instanceof NewClusterEvent && !this.verified ) {
        this.trigger( );
      } else if ( event instanceof TeardownClusterEvent ) {
        this.verified = false;
      }
    } else {
      LOG.debug( "Ignoring event which doesn't belong to me: " + LogUtil.dumpObject( event ) );
    }
  }
  
  
  
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    Throwable cause = e.getCause( );
    LOG.debug( cause, cause );
//    if( cause instanceof ReadTimeoutException ) {      
//    } else if ( cause instanceof WriteTimeoutException ) {
//    }
  }

  public Cluster getCluster( ) {
    return cluster;
  }
  
  public ClusterConfiguration getConfiguration( ) {
    return cluster.getConfiguration( );
  }
  
  public ClusterCredentials getCredentials( ) {
    return cluster.getCredentials( );
  }
  
  public String getName( ) {
    return cluster.getName( );
  }
  
  public void setCluster( Cluster cluster ) {
    this.cluster = cluster;
  }
  
  public boolean timedTrigger( Event c ) {
    if ( c instanceof ClockTick ) {
      return ( ( ClockTick ) c ).isBackEdge( );
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( hostName == null ) ? 0 : hostName.hashCode( ) );
    result = prime * result + port;
    result = prime * result + ( ( servicePath == null ) ? 0 : servicePath.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    AbstractClusterMessageDispatcher other = ( AbstractClusterMessageDispatcher ) obj;
    if ( hostName == null ) {
      if ( other.hostName != null ) return false;
    } else if ( !hostName.equals( other.hostName ) ) return false;
    if ( port != other.port ) return false;
    if ( servicePath == null ) {
      if ( other.servicePath != null ) return false;
    } else if ( !servicePath.equals( other.servicePath ) ) return false;
    return true;
  }
  
}

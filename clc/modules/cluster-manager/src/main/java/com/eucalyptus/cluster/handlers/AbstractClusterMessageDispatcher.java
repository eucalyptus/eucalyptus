package com.eucalyptus.cluster.handlers;

import java.net.InetSocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.util.concurrent.Executors;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.binding.BindingManager;
import com.eucalyptus.ws.client.NioBootstrap;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.handlers.NioHttpResponseDecoder;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.http.NioHttpRequestEncoder;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.ClusterWsSecHandler;

public abstract class AbstractClusterMessageDispatcher implements ChannelPipelineFactory, ChannelHandler, ChannelUpstreamHandler, ChannelDownstreamHandler, EventListener {
  private static Logger     LOG = Logger.getLogger( AbstractClusterMessageDispatcher.class );
  private ChannelFactory    channelFactory;
  private ChannelFuture     channelWriteFuture;
  private Channel           channel;
  private NioBootstrap      clientBootstrap;
  private Cluster           cluster;
  private Binding           binding;
  private InetSocketAddress remoteAddr;
  private String            hostName;
  private int               port;
  private String            servicePath;
  private boolean           secure;
  protected boolean           verified = false;
  private String            actionPrefix;
  private static String SECURE_NAME = "EucalyptusCC";
  private static String SECURE_NC_NAME = "EucalyptusNC";
  private static String INSECURE_NAME = "EucalyptusGL";
  public AbstractClusterMessageDispatcher( Cluster cluster, boolean secure ) throws BindingException {
    this( cluster );
    if( !secure ) {
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
    this.channelFactory = new NioClientSocketChannelFactory( Executors.newCachedThreadPool( ), Executors.newCachedThreadPool( ) );
    this.clientBootstrap = new NioBootstrap( channelFactory );
    this.clientBootstrap.setPipelineFactory( this );
    this.binding = BindingManager.getBinding( "eucalyptus_ucsb_edu" );
    this.secure = true;
  }

  @Override
  public ChannelPipeline getPipeline( ) throws Exception {
    ChannelPipeline pipeline = Channels.pipeline( );
    pipeline.addLast( "decoder", new NioHttpResponseDecoder( ) );
    pipeline.addLast( "aggregator", new HttpChunkAggregator( 1048576 ) );
    pipeline.addLast( "encoder", new NioHttpRequestEncoder( ) );
    pipeline.addLast( "serializer", new SoapMarshallingHandler( ) );
    pipeline.addLast( "wssec", new ClusterWsSecHandler( ) );
    pipeline.addLast( "addressing", new AddressingHandler( this.actionPrefix + "#" ) );
    pipeline.addLast( "soap", new SoapHandler( ) );
    pipeline.addLast( "binding", new BindingHandler( this.binding ) );
    pipeline.addLast( "handler", this );
    return pipeline;
  }

  public void write( Object o ) {
    HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostName, this.port, this.servicePath , o );
    if ( this.channel == null || !this.channel.isOpen( ) || !this.channel.isConnected( ) ) {
      ChannelFuture channelOpenFuture = this.clientBootstrap.connect( this.remoteAddr );
      channelOpenFuture.addListener( new DeferedWriter( request ) );
    } else {
      this.channelWriteFuture = this.channel.write( request );
    }
  }

  public void handleDownstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      this.downstreamMessage( ctx, ( MessageEvent ) e );
    } else if ( e instanceof ExceptionEvent ) {
      this.exceptionCaught( ctx, ( ExceptionEvent ) e );
    } else {
      ctx.sendDownstream( e );
    }
  }

  public abstract void downstreamMessage( ChannelHandlerContext ctx, MessageEvent e );

  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      this.upstreamMessage( ctx, ( MessageEvent ) e );
    } else if ( e instanceof ExceptionEvent ) {
      this.exceptionCaught( ctx, ( ExceptionEvent ) e );
    } else {
      ctx.sendUpstream( e );
    }
  }

  public abstract void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e );

  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    if( e.getCause( ) instanceof AlreadyConnectedException ) {
      LOG.trace( e.getCause( ), e.getCause( ) );
    } else {
      if ( this.channel != null ) {
        this.channel.close( );
      }
      LOG.debug( e.getCause( ), e.getCause( ) );
      Channels.fireExceptionCaught( ctx, e.getCause( ) );
    }
  }


  protected void fireTimedStatefulTrigger( Event event ) {
    if ( this.timedTrigger( event ) ) {
      LOG.debug( "Fire " + LogUtil.dumpObject( event ) + " on " + LogUtil.dumpObject( this ) );
      this.trigger( );
    } else if ( event instanceof GenericEvent ) {
      LOG.debug( "Fire " + LogUtil.dumpObject( event ) + " on " + LogUtil.dumpObject( this ) );
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

  class DeferedWriter implements ChannelFutureListener {
    private Object              request;
    private MessageStackHandler handler;

    DeferedWriter( final Object request ) {
      this.request = request;
    }

    @Override
    public void operationComplete( ChannelFuture channelFuture ) throws Exception {
      if ( channelFuture.isSuccess( ) ) {
        channel = channelFuture.getChannel( );
        channelWriteFuture = channelFuture.getChannel( ).write( request );
      } 
    }
  }

  public void close( ) {
    this.channel.close( );
    LOG.debug( "Forcing the channel to close." );
  }

  public void cleanup( ) {
    if ( this.channel != null ) {
      this.close( );
    }
    this.channelFactory.releaseExternalResources( );
  }

  public abstract void trigger( );

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
    if( c instanceof ClockTick) {
      return ((ClockTick)c).isBackEdge( );
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

package com.eucalyptus.cluster.handlers;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.EucalyptusClusterException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.client.NioBootstrap;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.NioHttpResponseDecoder;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.handlers.soap.AddressingHandler;
import com.eucalyptus.ws.handlers.soap.SoapHandler;
import com.eucalyptus.ws.handlers.wssecurity.ClusterWsSecHandler;
import com.eucalyptus.ws.util.ChannelUtil;
import com.eucalyptus.records.EventRecord;
import edu.ucsb.eucalyptus.msgs.GetKeysResponseType;

public abstract class AbstractClusterMessageDispatcher extends SimpleChannelHandler implements ChannelPipelineFactory, EventListener {
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
    this.remoteAddr = new InetSocketAddress( cluster.getConfiguration( ).getHostName( ),
      cluster.getConfiguration( ).getPort( ) );
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
  
  private AtomicBoolean inFlightMessage = new AtomicBoolean( false );
  
  public void write( final Object o ) {
    if( !this.inFlightMessage.compareAndSet( false, true ) ) {
      LOG.trace( EventRecord.caller( AbstractClusterMessageDispatcher.class, EventType.MSG_REJECTED, LogUtil.dumpObject( o ) ) );    
      return;
    } else {
      ChannelFuture channelConnectFuture = this.clientBootstrap.connect( this.remoteAddr );
      final HttpRequest request = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, this.hostName, this.port, this.servicePath, o );
      channelConnectFuture.addListener( ChannelFutureListener.CLOSE_ON_FAILURE );
      channelConnectFuture.addListener( ChannelUtil.WRITE_AND_CALLBACK( request, new ChannelFutureListener( ) {
        @Override public void operationComplete( ChannelFuture future ) throws Exception {
          EventRecord.here( o.getClass(), EventType.MSG_SENT, LogUtil.dumpObject( o ) ).info( );
        } } ) );
    }
  }
  
  protected void fireTimedStatefulTrigger( Event event ) {
    if ( this.timedTrigger( event ) ) {
      this.trigger( );
    } else if ( event instanceof GenericEvent ) {
      GenericEvent<Cluster> g = ( GenericEvent<Cluster> ) event;
      if ( !g.matches( this.getCluster( ) ) ) {
        this.inFlightMessage.set( false );
        return;
      }
      if ( g instanceof NewClusterEvent && !this.verified ) {
        this.trigger( );
        this.inFlightMessage.set( false );
      } else if ( event instanceof TeardownClusterEvent ) {
        this.verified = false;
        this.inFlightMessage.set( false );
      }
    }
  }
  private void clearPending( Channel channel ) {
    channel.close( );
    this.inFlightMessage.set( false );
  }
  
  @Override
  public void channelInterestChanged( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    EventRecord.here( AbstractClusterMessageDispatcher.class, EventType.MSG_PENDING, e.toString( ) ).trace( );
    super.channelInterestChanged( ctx, e );
  }
  
  @Override
  public void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
    try {
      if ( e.getMessage( ) instanceof MappingHttpResponse ) {
        MappingHttpResponse response = ( MappingHttpResponse ) ( ( MessageEvent ) e ).getMessage( );
        if ( HttpResponseStatus.OK.equals( response.getStatus( ) ) ) {
          if(!( response.getMessage( ) instanceof GetKeysResponseType )) {
            EventRecord.here( response.getMessage( ).getClass(), EventType.MSG_SENT, LogUtil.dumpObject( response.getMessage( ) ) ).info( );
          }
          this.upstreamMessage( ctx, ( MessageEvent ) e );
        } else {
          throw new EucalyptusClusterException( response.getMessageString( ) );
        }
      }
    } finally {
      this.clearPending( ctx.getChannel( ) );      
    }
    super.messageReceived( ctx, e );
  }
  
  @Override
  public void writeComplete( ChannelHandlerContext ctx, WriteCompletionEvent e ) throws Exception {
    EventRecord.here( AbstractClusterMessageDispatcher.class, EventType.MSG_SERVICED, e.toString( ) ).trace( );
    super.writeComplete( ctx, e );
  }
    
  @Override
  public void channelClosed( ChannelHandlerContext ctx, ChannelStateEvent e ) throws Exception {
    this.inFlightMessage.set( false );
    super.channelClosed( ctx, e );
  }
  @Override
  public void writeRequested( ChannelHandlerContext ctx, MessageEvent e ) throws Exception {
    LOG.trace( EventRecord.here( AbstractClusterMessageDispatcher.class, EventType.MSG_PENDING,
                                 e.getMessage( ).toString( ) ) );
    super.writeRequested( ctx, e );
  }
  @Override
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    if(this.inFlightMessage.get( )) {
      if( e != null && e.getCause() != null ) {
        LOG.debug( e.getCause( ), e.getCause( ) );
      } else {
        Exception ex = new RuntimeException("Exception even has a null-valued cause.");
        LOG.error( ex, ex );
      }
      this.clearPending( ctx.getChannel( ) );
    }
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
    result = prime * result + ( ( this.cluster == null ) ? 0 : this.cluster.hashCode( ) );
    return result;
  }

  
    
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    AbstractClusterMessageDispatcher other = ( AbstractClusterMessageDispatcher ) obj;
    if ( this.cluster == null ) {
      if ( other.cluster != null ) return false;
    } else if ( !this.cluster.getName( ).equals( other.cluster.getName( ) ) ) return false;
    return true;
  }

  @Override
  public void advertiseEvent( Event event ) {}
  
  @Override
  public void fireEvent( Event event ) {}
  
  
}

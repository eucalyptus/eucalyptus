package com.eucalyptus.util.async;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.ws.WebServices;
import com.eucalyptus.ws.util.NioBootstrap;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @author decker
 * @param <Q>
 * @param <R>
 */
@ChannelPipelineCoverage( "one" )
public class AsyncRequestHandler<Q extends BaseMessage, R extends BaseMessage> implements RequestHandler<Q, R> {
  private static Logger                LOG           = Logger.getLogger( AsyncRequestHandler.class );
  
  private NioBootstrap                 clientBootstrap;
  private ChannelFuture                connectFuture;
  
  private final AtomicBoolean          writeComplete = new AtomicBoolean( false );
  private final CheckedListenableFuture<R>   response;
  private transient AtomicReference<Q> request       = new AtomicReference<Q>( null );
  
  AsyncRequestHandler( final CheckedListenableFuture<R> response ) {
    super( );
    this.response = response;
  }
  
  /**
   * @see com.eucalyptus.util.async.RequestHandler#fire(com.eucalyptus.component.ServiceEndpoint)
   * @param serviceEndpoint
   * @return
   */
  @Override
  public boolean fire( final ServiceConfiguration config, final Q request ) {
    if ( !this.request.compareAndSet( null, request ) ) {
      LOG.warn( "Duplicate write attempt for request: " + this.request.get( ).getClass( ).getSimpleName( ) );
      return false;
    } else {
      final SocketAddress serviceSocketAddress = config.getSocketAddress( );
      final ChannelPipelineFactory factory = config.getComponentId( ).getClientPipeline( );
      try {
        this.clientBootstrap = WebServices.clientBootstrap( new ChannelPipelineFactory( ) {
          @Override
          public ChannelPipeline getPipeline( ) throws Exception {
            final ChannelPipeline pipeline = factory.getPipeline( );
            pipeline.addLast( "request-handler", AsyncRequestHandler.this );
            return pipeline;
          }
        } );
//TODO:GRZE: better logging here        LOG.debug( request.getClass( ).getSimpleName( ) + ":" + request.getCorrelationId( ) + " connecting to " + serviceSocketAddress );
        EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_OPENING, request.getClass( ).getSimpleName( ),
                          request.getCorrelationId( ), serviceSocketAddress.toString( ) ).trace( );
        this.connectFuture = this.clientBootstrap.connect( serviceSocketAddress );
        final HttpRequest httpRequest = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, config, this.request.get( ) );
        
        this.connectFuture.addListener( new ChannelFutureListener( ) {
          @Override
          public void operationComplete( final ChannelFuture future ) throws Exception {
            try {
              if ( future.isSuccess( ) ) {
//TODO:GRZE: better logging here                LOG.debug( "Connected as: " + future.getChannel( ).getLocalAddress( ) );
                final InetAddress localAddr = ( ( InetSocketAddress ) future.getChannel( ).getLocalAddress( ) ).getAddress( );
                if ( !factory.getClass( ).getSimpleName( ).startsWith( "GatherLog" ) ) {
                  Topology.populateServices( config, AsyncRequestHandler.this.request.get( ) );
                }
                EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_OPEN, request.getClass( ).getSimpleName( ),
                                  request.getCorrelationId( ), serviceSocketAddress.toString( ), "" + future.getChannel( ).getLocalAddress( ),
                                  ""  + future.getChannel( ).getRemoteAddress( ) ).trace( );
                future.getChannel( ).getCloseFuture( ).addListener( new ChannelFutureListener( ) {
                  @Override
                  public void operationComplete( final ChannelFuture future ) throws Exception {
                    EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED, request.getClass( ).getSimpleName( ),
                                      request.getCorrelationId( ), serviceSocketAddress.toString( ), "" + future.getChannel( ).getLocalAddress( ),
                                      ""  + future.getChannel( ).getRemoteAddress( ) ).trace( );
                  }
                } );
                
                future.getChannel( ).write( httpRequest ).addListener( new ChannelFutureListener( ) {
                  @Override
                  public void operationComplete( final ChannelFuture future ) throws Exception {
                    AsyncRequestHandler.this.writeComplete.set( true );
                    EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_WRITE, request.getClass( ).getSimpleName( ),
                                      request.getCorrelationId( ), serviceSocketAddress.toString( ), "" + future.getChannel( ).getLocalAddress( ),
                                      ""  + future.getChannel( ).getRemoteAddress( ) ).trace( );
                  }
                } );
              } else {
                AsyncRequestHandler.this.teardown( future.getCause( ) );
              }
            } catch ( final Exception ex ) {
              LOG.error( ex, ex );
              AsyncRequestHandler.this.teardown( future.getCause( ) );
            }
          }
        } );
        return true;
      } catch ( final Exception t ) {
        LOG.error( t, t );
        this.teardown( t );
        return false;
      }
    }
  }
  
  private void teardown( final Throwable t ) {
    if ( ( t != null ) && !this.response.isDone( ) ) {
      Logs.extreme( ).debug( "RESULT:" + t.getMessage( )
                 + ":REQUEST:"
                 + ( ( this.request.get( ) != null )
                   ? this.request.get( ).getClass( )
                   : "REQUEST IS NULL" ) );
      if ( t instanceof RetryableConnectionException ) {
        Logs.extreme( ).trace( t.getMessage( ) );
      } else if ( t instanceof ConnectionException ) {
        Logs.extreme( ).trace( t.getMessage( ) );
      } else if ( t instanceof IOException ) {
        Logs.extreme( ).trace( t.getMessage( ) );
      }
      this.response.setException( t );
    } else if ( ( t != null ) && this.response.isDone( ) ) {
      Logs.extreme( ).trace( t.getMessage( ) );
      this.response.setException( t );
    }
    if ( this.connectFuture != null ) {
      if ( this.connectFuture.isDone( ) && this.connectFuture.isSuccess( ) ) {
        final Channel channel = this.connectFuture.getChannel( );
        if ( ( channel != null ) && channel.isOpen( ) ) {
          channel.close( ).addListener( new ChannelFutureListener( ) {
            @Override
            public void operationComplete( final ChannelFuture future ) throws Exception {
              EventRecord.here( AsyncRequestHandler.this.request.get( ).getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED ).trace( );
            }
          } );
        } else {
          EventRecord.here( AsyncRequestHandler.this.request.get( ).getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED, "ALREADY_CLOSED" ).trace( );
        }
      } else if ( !this.connectFuture.isDone( ) && !this.connectFuture.cancel( ) ) {
        LOG.error( "Failed to cancel in-flight connection request: " + this.connectFuture.toString( ) );
        final Channel channel = this.connectFuture.getChannel( );
        if ( channel != null ) {
          channel.close( );
        }
      } else if ( !this.connectFuture.isSuccess( ) ) {
        final Channel channel = this.connectFuture.getChannel( );
        if ( channel != null ) {
          channel.close( );
        }
//REVIEW: this is likely not needed.        LOG.error( this.connectFuture.getCause( ).getMessage( ) );
      }
    } else {
      this.response.setException( t );
    }
  }
  
  @Override
  public void handleUpstream( final ChannelHandlerContext ctx, final ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      this.messageReceived( ctx, ( MessageEvent ) e );
    } else if ( e instanceof ChannelStateEvent ) {
      final ChannelStateEvent evt = ( ChannelStateEvent ) e;
      switch ( evt.getState( ) ) {
        case OPEN:
          if ( Boolean.FALSE.equals( evt.getValue( ) ) ) {
            this.checkFinished( ctx, evt );
          }
          break;
        case CONNECTED:
          if ( evt.getValue( ) == null ) {
            this.checkFinished( ctx, evt );
          }
          break;
      }
    } else if ( e instanceof ExceptionEvent ) {
      this.exceptionCaught( ctx, ( ExceptionEvent ) e );
    }
    ctx.sendUpstream( e );
  }
  
  private void messageReceived( final ChannelHandlerContext ctx, final MessageEvent e ) {
    try {
      if ( e.getMessage( ) instanceof MappingHttpResponse ) {
        final MappingHttpResponse response = ( MappingHttpResponse ) e.getMessage( );
        try {
          final R msg = ( R ) response.getMessage( );
          e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
          if ( !msg.get_return( ) ) {
            this.teardown( new FailedRequestException( "Cluster response includes _return=false", msg ) );
          } else {
            this.response.set( msg );
          }
        } catch ( final Exception e1 ) {
          LOG.error( e1, e1 );
          this.teardown( e1 );
        }
      } else if ( e.getMessage( ) == null ) {
        final NoResponseException ex = new NoResponseException( "Channel received a null response.", this.request.get( ) );
        LOG.error( ex, ex );
        this.teardown( ex );
      } else {
        final UnknownMessageTypeException ex = new UnknownMessageTypeException( "Channel received a unknown response type: "
                                                                          + e.getMessage( ).getClass( ).getCanonicalName( ), this.request.get( ),
                                                                          e.getMessage( ) );
        LOG.error( ex, ex );
        this.teardown( ex );
      }
    } catch ( final Exception t ) {
      LOG.error( t, t );
      this.teardown( t );
    }
  }
  
  private void checkFinished( final ChannelHandlerContext ctx, final ChannelStateEvent evt ) {
    if ( ( this.connectFuture != null ) && !this.connectFuture.isSuccess( )
         && ( this.connectFuture.getCause( ) instanceof IOException ) ) {
      final Throwable ioError = this.connectFuture.getCause( );
      if ( !this.writeComplete.get( ) ) {
        this.teardown( new RetryableConnectionException( "Channel was closed before the write operation could be completed: " + ioError.getMessage( ), ioError,
                                                         this.request.get( ) ) );
      } else {
        this.teardown( new ConnectionException( "Channel was closed before the response was received: " + ioError.getMessage( ), ioError, this.request.get( ) ) );
      }
    } else {
      if ( !this.writeComplete.get( ) ) {
        this.teardown( new RetryableConnectionException( "Channel was closed before the write operation could be completed", this.request.get( ) ) );
      } else if ( !this.response.isDone( ) ) {
        this.teardown( new ConnectionException( "Channel was closed before the response was received.", this.request.get( ) ) );
      }
    }
  }
  
  private void exceptionCaught( final ChannelHandlerContext ctx, final ExceptionEvent e ) {
    this.teardown( e.getCause( ) );
  }
  
  private static class ChannelUtil {
    public static Long              CLIENT_IDLE_TIMEOUT_SECS       = 4 * 60l;
    public static Long              CLUSTER_IDLE_TIMEOUT_SECS      = 4 * 60l;
    public static Long              CLUSTER_CONNECT_TIMEOUT_MILLIS = 2000l;
    public static Long              PIPELINE_READ_TIMEOUT_SECONDS  = 20l;
    public static Long              PIPELINE_WRITE_TIMEOUT_SECONDS = 20l;
    public static Integer           CLIENT_POOL_MAX_THREADS        = 40;
    public static Long              CLIENT_POOL_MAX_MEM_PER_CONN   = 1048576l;
    public static Long              CLIENT_POOL_TOTAL_MEM          = 20 * 1024 * 1024l;
    public static Long              CLIENT_POOL_TIMEOUT_MILLIS     = 500l;
    
    private static Lock             canHas                         = new ReentrantLock( );
    private static HashedWheelTimer timer                          = new HashedWheelTimer( );
    private static ExecutorService  clientWorkerThreadPool;
    private static ExecutorService  clientBossThreadPool;
    private static ChannelFactory   clientSocketFactory;
    
    static {
      canHas.lock( );
      try {
        clientWorkerThreadPool = ChannelUtil.getClientWorkerThreadPool( );
        clientBossThreadPool = ChannelUtil.getClientBossThreadPool( );
        clientSocketFactory = ChannelUtil.getClientChannelFactory( );
      } finally {
        canHas.unlock( );
      }
    }
    
    public static ChannelPipeline addPipelineMonitors( final ChannelPipeline pipeline, final int i ) {
      pipeline.addLast( "idlehandler", new IdleStateHandler( ChannelUtil.timer, i, i, i ) );
      pipeline.addLast( "readTimeout", new ReadTimeoutHandler( ChannelUtil.timer, i, TimeUnit.SECONDS ) );
      pipeline.addLast( "writeTimeout", new WriteTimeoutHandler( ChannelUtil.timer, i, TimeUnit.SECONDS ) );
      return pipeline;
    }
    
    private static ExecutorService getClientWorkerThreadPool( ) {
      canHas.lock( );
      try {
        if ( clientWorkerThreadPool == null ) {
          LOG.info( LogUtil.subheader( "Creating client worker thread pool." ) );
          LOG.info( String.format( "-> Pool threads:              %8d", CLIENT_POOL_MAX_THREADS ) );
          LOG.info( String.format( "-> Pool timeout:              %8d ms", CLIENT_POOL_TIMEOUT_MILLIS ) );
          LOG.info( String.format( "-> Max memory per connection: %8.2f MB", CLIENT_POOL_MAX_MEM_PER_CONN / ( 1024f * 1024f ) ) );
          LOG.info( String.format( "-> Max total memory:          %8.2f MB", CLIENT_POOL_TOTAL_MEM / ( 1024f * 1024f ) ) );
          clientWorkerThreadPool = new OrderedMemoryAwareThreadPoolExecutor( CLIENT_POOL_MAX_THREADS, CLIENT_POOL_MAX_MEM_PER_CONN, CLIENT_POOL_TOTAL_MEM,
                                                                             CLIENT_POOL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS );
        }
      } finally {
        canHas.unlock( );
      }
      return clientWorkerThreadPool;
    }
    
    private static ExecutorService getClientBossThreadPool( ) {
      canHas.lock( );
      try {
        if ( clientBossThreadPool == null ) {
          LOG.info( LogUtil.subheader( "Creating client boss thread pool." ) );
          LOG.info( String.format( "-> Pool threads:              %8d", CLIENT_POOL_MAX_THREADS ) );
          LOG.info( String.format( "-> Pool timeout:              %8d ms", CLIENT_POOL_TIMEOUT_MILLIS ) );
          LOG.info( String.format( "-> Max memory per connection: %8.2f MB", CLIENT_POOL_MAX_MEM_PER_CONN / ( 1024f * 1024f ) ) );
          LOG.info( String.format( "-> Max total memory:          %8.2f MB", CLIENT_POOL_TOTAL_MEM / ( 1024f * 1024f ) ) );
          clientBossThreadPool = new OrderedMemoryAwareThreadPoolExecutor( CLIENT_POOL_MAX_THREADS, CLIENT_POOL_MAX_MEM_PER_CONN, CLIENT_POOL_TOTAL_MEM,
                                                                           CLIENT_POOL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS );
        }
      } finally {
        canHas.unlock( );
      }
      return clientBossThreadPool;
    }
    
    public static NioBootstrap getClientBootstrap( final ChannelPipelineFactory factory ) {
      final NioBootstrap bootstrap = new NioBootstrap( ChannelUtil.getClientChannelFactory( ) );//TODO: pass port host, etc here.
      bootstrap.setPipelineFactory( factory );
      bootstrap.setOption( "tcpNoDelay", false );
      bootstrap.setOption( "keepAlive", false );
      bootstrap.setOption( "reuseAddress", false );
      bootstrap.setOption( "connectTimeoutMillis", 3000 );
      return bootstrap;
    }
    
    private static ChannelFactory getClientChannelFactory( ) {
      canHas.lock( );
      try {
        if ( clientSocketFactory == null ) {
          clientSocketFactory = new NioClientSocketChannelFactory( ChannelUtil.getClientBossThreadPool( ), ChannelUtil.getClientWorkerThreadPool( ),
                                                                   CLIENT_POOL_MAX_THREADS );
        }
      } finally {
        canHas.unlock( );
      }
      return clientSocketFactory;
    }
    
  }
  
}

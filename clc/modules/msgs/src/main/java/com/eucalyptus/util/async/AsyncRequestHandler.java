package com.eucalyptus.util.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceEndpoint;
import com.eucalyptus.component.id.Cluster;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.empyrean.ServiceInfoType;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
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
  private CheckedListenableFuture<R>   response;
  private transient AtomicReference<Q> request       = new AtomicReference<Q>( null );
  
  AsyncRequestHandler( CheckedListenableFuture<R> response ) {
    super( );
    this.response = response;
  }
  
  /**
   * @see com.eucalyptus.util.async.RequestHandler#fire(com.eucalyptus.component.ServiceEndpoint)
   * @param serviceEndpoint
   * @return
   */
  @Override
  public boolean fire( final ServiceEndpoint endpoint, final ChannelPipelineFactory factory, final Q request ) {
    final ServiceEndpoint serviceEndpoint;
    if ( factory.getClass( ).getSimpleName( ).startsWith( "GatherLog" ) ) {
      serviceEndpoint = new ServiceEndpoint( endpoint.getParent( ), false, URI.create( endpoint.getUri( ).toASCIIString( ).replaceAll( "EucalyptusCC",
                                                                                                                                       "EucalyptusGL" ) ) );
    } else {
      serviceEndpoint = endpoint;
    }
    if ( !this.request.compareAndSet( null, request ) ) {
      LOG.warn( "Duplicate write attempt for request: " + this.request.get( ).getClass( ).getSimpleName( ) );
      return true;
    } else {
      try {
        this.clientBootstrap = ChannelUtil.getClientBootstrap( new ChannelPipelineFactory( ) {
          @Override
          public ChannelPipeline getPipeline( ) throws Exception {
            ChannelPipeline pipeline = factory.getPipeline( );
            ChannelUtil.addPipelineMonitors( pipeline, 30 );
            pipeline.addLast( "request-handler", AsyncRequestHandler.this );
            return pipeline;
          }
        } );
        LOG.debug( request.getClass( ).getSimpleName( ) + ":" + request.getCorrelationId( ) + " connecting to " + serviceEndpoint.getSocketAddress( ) );
        EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_OPENING, request.getClass( ).getSimpleName( ),
                          request.getCorrelationId( ), serviceEndpoint.getSocketAddress( ).toString( ) ).trace( );
        this.connectFuture = this.clientBootstrap.connect( serviceEndpoint.getSocketAddress( ) );
        final HttpRequest httpRequest = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, serviceEndpoint, this.request.get( ) );
        
        this.connectFuture.addListener( new ChannelFutureListener( ) {
          @Override
          public void operationComplete( ChannelFuture future ) throws Exception {
            if ( future.isSuccess( ) ) {
              LOG.debug( "Connected as: " + (future.getChannel( ).getLocalAddress( )) );
//              final String localhostAddr = ((InetSocketAddress)future.getChannel( ).getLocalAddress( )).getHostName( );
//              if ( !factory.getClass( ).getSimpleName( ).startsWith( "GatherLog" ) ) {
//                List<ServiceInfoType> serviceInfos = new ArrayList<ServiceInfoType>( ) {
//                  {
//                    addAll( Components.lookup( Eucalyptus.class ).getServiceSnapshot( localhostAddr ) );
//                    addAll( Components.lookup( Walrus.class ).getServiceSnapshot( localhostAddr ) );
//                    for ( ServiceInfoType s : Components.lookup( Storage.class ).getServiceSnapshot( localhostAddr ) ) {
//                      if ( serviceEndpoint.getParent( ).getServiceConfiguration( ).getPartition( ).equals( s.getPartition( ) ) ) {
//                        add( s );
//                      }
//                    }
//                    for ( ServiceInfoType s : Components.lookup( Cluster.class ).getServiceSnapshot( localhostAddr ) ) {
//                      if ( serviceEndpoint.getParent( ).getServiceConfiguration( ).getPartition( ).equals( s.getPartition( ) ) ) {
//                        add( s );
//                      }
//                    }
//                  }
//                };
//                AsyncRequestHandler.this.request.get( ).getBaseServices( ).addAll( serviceInfos );
//              }
              EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_OPEN, request.getClass( ).getSimpleName( ),
                                request.getCorrelationId( ), serviceEndpoint.getSocketAddress( ).toString( ), "" + future.getChannel( ).getLocalAddress( ),
                                "" + future.getChannel( ).getRemoteAddress( ) ).trace( );
              future.getChannel( ).getCloseFuture( ).addListener( new ChannelFutureListener( ) {
                @Override
                public void operationComplete( ChannelFuture future ) throws Exception {
                  EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED, request.getClass( ).getSimpleName( ),
                                    request.getCorrelationId( ), serviceEndpoint.getSocketAddress( ).toString( ), "" + future.getChannel( ).getLocalAddress( ),
                                    "" + future.getChannel( ).getRemoteAddress( ) ).trace( );
                }
              } );
              
              future.getChannel( ).write( httpRequest ).addListener( new ChannelFutureListener( ) {
                @Override
                public void operationComplete( ChannelFuture future ) throws Exception {
                  AsyncRequestHandler.this.writeComplete.set( true );
                  EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_WRITE, request.getClass( ).getSimpleName( ),
                                    request.getCorrelationId( ), serviceEndpoint.getSocketAddress( ).toString( ), "" + future.getChannel( ).getLocalAddress( ),
                                    "" + future.getChannel( ).getRemoteAddress( ) ).trace( );
                }
              } );
            } else {
              AsyncRequestHandler.this.teardown( future.getCause( ) );
            }
          }
        } );
        return true;
      } catch ( Throwable t ) {
        LOG.error( t, t );
        this.teardown( t );
        return false;
      }
    }
  }
  
  private void teardown( Throwable t ) {
    if ( t != null && !this.response.isDone( ) ) {
      LOG.debug( "RESULT:" + t.getMessage( ) + ":REQUEST:" + ( ( request.get( ) != null )
        ? request.get( ).toSimpleString( )
        : "REQUEST IS NULL" ) );
      if ( t instanceof RetryableConnectionException ) {

      } else if ( t instanceof ConnectionException ) {

      } else if ( t instanceof IOException ) {

      }
      this.response.setException( t );
    } else if ( t != null && this.response.isDone( ) ) {
      LOG.error( t.getMessage( ) );
    }
    if ( this.connectFuture != null ) {
      if ( this.connectFuture.isDone( ) && this.connectFuture.isSuccess( ) ) {
        Channel channel = this.connectFuture.getChannel( );
        if ( channel != null && channel.isOpen( ) ) {
          channel.close( ).addListener( new ChannelFutureListener( ) {
            @Override
            public void operationComplete( ChannelFuture future ) throws Exception {
              EventRecord.here( AsyncRequestHandler.this.request.get( ).getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED ).trace( );
            }
          } );
        } else {
          EventRecord.here( AsyncRequestHandler.this.request.get( ).getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_CLOSED, "ALREADY_CLOSED" ).trace( );
        }
      } else if ( !this.connectFuture.isDone( ) && !this.connectFuture.cancel( ) ) {
        LOG.error( "Failed to cancel in-flight connection request: " + this.connectFuture.toString( ) );
        Channel channel = this.connectFuture.getChannel( );
        if ( channel != null ) {
          channel.close( );
        }
      } else if ( !this.connectFuture.isSuccess( ) ) {
        Channel channel = this.connectFuture.getChannel( );
        if ( channel != null ) {
          channel.close( );
        }
//REVIEW: this is likely not needed.        LOG.error( this.connectFuture.getCause( ).getMessage( ) );
      }
    }
  }
  
  @Override
  public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
    if ( e instanceof MessageEvent ) {
      this.messageReceived( ctx, ( MessageEvent ) e );
    } else if ( e instanceof ChannelStateEvent ) {
      ChannelStateEvent evt = ( ChannelStateEvent ) e;
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
  
  private void messageReceived( ChannelHandlerContext ctx, MessageEvent e ) {
    try {
      if ( e.getMessage( ) instanceof MappingHttpResponse ) {
        MappingHttpResponse response = ( MappingHttpResponse ) e.getMessage( );
        try {
          R msg = ( R ) response.getMessage( );
          if ( !msg.get_return( ) ) {
            this.teardown( new FailedRequestException( "Cluster response includes _return=false", msg ) );
          } else {
            this.response.set( msg );
            e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
          }
        } catch ( Throwable e1 ) {
          LOG.error( e1, e1 );
          this.teardown( e1 );
        }
      } else if ( e.getMessage( ) == null ) {
        NoResponseException ex = new NoResponseException( "Channel received a null response.", this.request.get( ) );
        LOG.error( ex, ex );
        this.teardown( ex );
      } else {
        UnknownMessageTypeException ex = new UnknownMessageTypeException( "Channel received a unknown response type: "
                                                                          + e.getMessage( ).getClass( ).getCanonicalName( ), this.request.get( ),
                                                                          e.getMessage( ) );
        LOG.error( ex, ex );
        this.teardown( ex );
      }
    } catch ( Throwable t ) {
      LOG.error( t, t );
      this.teardown( t );
    }
  }
  
  private void checkFinished( ChannelHandlerContext ctx, ChannelStateEvent evt ) {
    if ( this.connectFuture != null && !this.connectFuture.isSuccess( ) && this.connectFuture.getCause( ) instanceof IOException ) {
      Throwable ioError = this.connectFuture.getCause( );
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
  
  private void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) {
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
    
    public static ChannelPipeline addPipelineMonitors( ChannelPipeline pipeline, int i ) {
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
    
    public static NioBootstrap getClientBootstrap( ChannelPipelineFactory factory ) {
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

  /**
   * TODO: DOCUMENT
   * @see org.jboss.netty.channel.ChannelDownstreamHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
   * @param arg0
   * @param arg1
   * @throws Exception
   */
  @Override
  public void handleDownstream( ChannelHandlerContext arg0, ChannelEvent arg1 ) throws Exception {}
  
}

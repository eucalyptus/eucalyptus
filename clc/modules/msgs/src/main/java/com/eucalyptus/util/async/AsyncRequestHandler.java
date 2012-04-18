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
import com.eucalyptus.util.Exceptions;
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
        Logs.extreme( ).debug( EventRecord.here( request.getClass( ), EventClass.SYSTEM_REQUEST, EventType.CHANNEL_OPENING, request.getClass( ).getSimpleName( ),
                          request.getCorrelationId( ), serviceSocketAddress.toString( ) ) );
        this.connectFuture = this.clientBootstrap.connect( serviceSocketAddress );
        final HttpRequest httpRequest = new MappingHttpRequest( HttpVersion.HTTP_1_1, HttpMethod.POST, config, this.request.get( ) );
        
        this.connectFuture.addListener( new ChannelFutureListener( ) {
          @Override
          public void operationComplete( final ChannelFuture future ) throws Exception {
            try {
              if ( future.isSuccess( ) ) {
                Logs.extreme( ).debug( "Connected as: " + future.getChannel( ).getLocalAddress( ) );
                
                final InetAddress localAddr = ( ( InetSocketAddress ) future.getChannel( ).getLocalAddress( ) ).getAddress( );
                if ( !factory.getClass( ).getSimpleName( ).startsWith( "GatherLog" ) ) {
                  Topology.populateServices( config, AsyncRequestHandler.this.request.get( ) );
                }

                Logs.extreme( ).debug(
                  EventRecord.here(
                    request.getClass( ),
                    EventClass.SYSTEM_REQUEST,
                    EventType.CHANNEL_OPEN,
                    request.getClass( ).getSimpleName( ),
                    request.getCorrelationId( ),
                    serviceSocketAddress.toString( ),
                    "" + future.getChannel( ).getLocalAddress( ),
                    "" + future.getChannel( ).getRemoteAddress( ) ) );
                Logs.extreme( ).debug( httpRequest );
                
                future.getChannel( ).write( httpRequest ).addListener( new ChannelFutureListener( ) {
                  @Override
                  public void operationComplete( final ChannelFuture future ) throws Exception {
                    AsyncRequestHandler.this.writeComplete.set( true );
                    
                    Logs.extreme( ).debug(
                      EventRecord.here(
                        request.getClass( ),
                        EventClass.SYSTEM_REQUEST,
                        EventType.CHANNEL_WRITE,
                        request.getClass( ).getSimpleName( ),
                        request.getCorrelationId( ),
                        serviceSocketAddress.toString( ),
                        "" + future.getChannel( ).getLocalAddress( ),
                        "" + future.getChannel( ).getRemoteAddress( ) ) );
                  }
                } );
              } else {
                AsyncRequestHandler.this.teardown( future.getCause( ) );
              }
            } catch ( final Exception ex ) {
              LOG.error( ex, ex );
              AsyncRequestHandler.this.teardown( ex );
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
  
  private void teardown( Throwable t ) {
    if ( t == null ) {
      t = new NullPointerException( "teardown() called with null argument." );
    }
    try {
      this.logRequestFailure( t );
      if ( this.connectFuture != null ) {
        this.maybeCloseChannel( );
      }
    } finally {
      this.response.setException( t );
    }
  }

  private void maybeCloseChannel( ) {
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
    }
  }

  private void logRequestFailure( Throwable t ) {
    try {
      Logs.extreme( ).debug( "RESULT:" + t.getMessage( )
                 + ":REQUEST:"
                 + ( ( this.request.get( ) != null )
                   ? this.request.get( ).getClass( )
                   : "REQUEST IS NULL" ) );
      if ( Exceptions.isCausedBy( t, RetryableConnectionException.class ) 
          || Exceptions.isCausedBy( t, ConnectionException.class )
          || Exceptions.isCausedBy( t, IOException.class ) ) {
        Logs.extreme( ).debug( "Failed request: " + this.request.get( ).toSimpleString( ) + " because of: " + t.getMessage( ), t );
      }
    } catch ( Exception ex ) {
      Logs.extreme( ).error( ex , ex );
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
          if ( !msg.get_return( ) ) {
            this.teardown( new FailedRequestException( "Cluster response includes _return=false", msg ) );
          } else {
            this.response.set( msg );
          }
          e.getFuture( ).addListener( ChannelFutureListener.CLOSE );
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
    Logs.extreme( ).error( e, e.getCause( ) );
    this.teardown( e.getCause( ) );
  }
  
}

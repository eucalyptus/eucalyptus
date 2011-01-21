package com.eucalyptus.util.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipelineFactory;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceEndpoint;
import com.eucalyptus.component.Services;
import com.eucalyptus.util.async.Callback.TwiceChecked;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class AsyncRequest<Q extends BaseMessage, R extends BaseMessage> implements Request<Q, R> {
  private static Logger LOG = Logger.getLogger( AsyncRequest.class );
  private final Callback.TwiceChecked<Q, R> callback;
  private final CheckedListenableFuture<R>  response;
  private Throwable                         callbackError = null;
  private final RequestHandler<Q, R>        handler;
  private final CallbackListenerSequence<R> callbackSequence;
  private Q                                 request;
  private final ChannelPipelineFactory      pipelineFactory;
  private CheckedListenableFuture<R>        callbackResult;
  
  protected AsyncRequest( TwiceChecked<Q, R> callback, ChannelPipelineFactory factory ) {
    super( );
    this.response = Futures.newAsyncMessageFuture( );
    this.callbackResult = Futures.newAsyncMessageFuture( );
    this.callback = callback;
    this.handler = new AsyncRequestHandler<Q, R>( this.response );
    this.callbackSequence = new CallbackListenerSequence<R>( );
    this.pipelineFactory = factory;
    Futures.addListenerHandler( response, this.callback );
    Futures.addListenerHandler( response, new Callback.Success<R>( ) {
      
      @Override
      public void fire( R r ) {
        try {
          AsyncRequest.this.callbackResult.set( AsyncRequest.this.response.get( ) );
        } catch ( Throwable ex ) {
          AsyncRequest.this.callbackResult.setException( ex.getCause( ) );
        }
      }
    } );
    Futures.addListenerHandler( this.callbackResult, this.callbackSequence );
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#dispatch(java.lang.String)
   * @param cluster
   * @return
   */
  @Override
  public CheckedListenableFuture<R> dispatch( String cluster ) {
    Services.lookupByName( Components.delegate.cluster, cluster ).enqueue( this );
    return this.getResponse( );
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#dispatch(java.lang.String)
   * @param cluster
   * @return
   */
  @Override
  public CheckedListenableFuture<R> dispatch( ServiceEndpoint serviceEndpoint ) {
    serviceEndpoint.enqueue( this );
    return this.getResponse( );
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#sendSync(com.eucalyptus.component.ServiceEndpoint)
   * @param endpoint
   * @return
   * @throws ExecutionException
   * @throws InterruptedException
   */
  @Override
  public R sendSync( ServiceEndpoint endpoint ) throws ExecutionException, InterruptedException {
    return this.execute( endpoint ).getResponse( ).get( );
  }
  
  private Request<Q, R> execute( ServiceEndpoint endpoint ) {
    Logger.getLogger( this.callback.getClass( ) ).trace( "initialize: endpoint " + endpoint );
    try {
      this.callback.initialize( this.request );
    } catch ( Throwable e ) {
      Logger.getLogger( this.callback.getClass( ) ).error( e.getMessage( ), e );
      RequestException ex = ( e instanceof RequestException )
        ? ( RequestException ) e
        : new RequestInitializationException( this.callback.getClass( ).getSimpleName( ) + " failed: " + e.getMessage( ), e, this.getRequest( ) );
      this.response.setException( ex );
      throw ex;
    }
    Logger.getLogger( this.callback.getClass( ) ).debug( "fire: endpoint " + endpoint );
    if ( !this.handler.fire( endpoint, this.pipelineFactory, this.request ) ) {
      if ( this.response.isDone( ) ) {
        try {
          R r = this.response.get( 1, TimeUnit.MILLISECONDS );
          throw new RequestException( "Request failed but produced a response: " + r, this.getRequest( ) );
        } catch ( ExecutionException e ) {
          if ( e.getCause( ) != null && e.getCause( ) instanceof RequestException ) {
            Logger.getLogger( this.callback.getClass( ) ).error( e.getCause( ) );
            throw ( RequestException ) e.getCause( );
          } else {
            Logger.getLogger( this.callback.getClass( ) ).error( e );
            throw new RequestException( "Request failed due to: " + e.getMessage( ), e, this.getRequest( ) );
          }
        } catch ( RequestException e ) {
          Logger.getLogger( this.callback.getClass( ) ).error( e );
          throw e;
        } catch ( Throwable e ) {
          Logger.getLogger( this.callback.getClass( ) ).error( e );
          throw new RequestException( "Request failed due to: " + e.getMessage( ), e, this.getRequest( ) );
        }
      } else {
        RequestException ex = new RequestException( "Error occured attempting to fire the request.", this.getRequest( ) );
        try {
          this.response.setException( ex );
        } catch ( Throwable t ) {}
        throw ex;
      }
    }
    return this;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#then(com.eucalyptus.util.async.UnconditionalCallback)
   * @param callback
   * @return
   */
  @Override
  public Request<Q, R> then( UnconditionalCallback callback ) {
    this.callbackSequence.addCallback( callback );
    return this;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#then(com.eucalyptus.util.async.Callback.Completion)
   * @param callback
   * @return
   */
  @Override
  public Request<Q, R> then( Callback.Completion callback ) {
    this.callbackSequence.addCallback( callback );
    return this;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#then(com.eucalyptus.util.async.Callback.Failure)
   * @param callback
   * @return
   */
  @Override
  public Request<Q, R> then( Callback.Failure<R> callback ) {
    this.callbackSequence.addFailureCallback( callback );
    return this;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#then(com.eucalyptus.util.async.Callback.Success)
   * @param callback
   * @return
   */
  @Override
  public Request<Q, R> then( Callback.Success<R> callback ) {
    this.callbackSequence.addSuccessCallback( callback );
    return this;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#getCallback()
   * @return
   */
  @Override
  public Callback.TwiceChecked<Q, R> getCallback( ) {
    return this.callback;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#getResponse()
   * @return
   */
  @Override
  public CheckedListenableFuture<R> getResponse( ) {
    return this.response;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#getRequest()
   * @return
   */
  @Override
  public Q getRequest( ) {
    return this.request;
  }
  
  protected void setRequest( Q request ) {
    this.request = request;
  }
  
}

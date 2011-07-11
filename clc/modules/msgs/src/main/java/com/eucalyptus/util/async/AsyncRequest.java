package com.eucalyptus.util.async;

import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.NoSuchServiceException;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceEndpoint;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Logs;
import com.eucalyptus.util.async.Callback.TwiceChecked;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class AsyncRequest<Q extends BaseMessage, R extends BaseMessage> implements Request<Q, R> {
  private static Logger                     LOG = Logger.getLogger( AsyncRequest.class );
  private final Callback.TwiceChecked<Q, R> wrapperCallback;
  private final Callback.TwiceChecked<Q, R> cb;
  private final CheckedListenableFuture<R>  requestResult;
  private final CheckedListenableFuture<R>  result;
  private final RequestHandler<Q, R>        handler;
  private final CallbackListenerSequence<R> callbackSequence;
  private Q                                 request;
  
  protected AsyncRequest( final TwiceChecked<Q, R> cb ) {
    super( );
    this.result = new AsyncResponseFuture<R>( );
    this.requestResult = new AsyncResponseFuture<R>( );
    this.handler = new AsyncRequestHandler<Q, R>( this.requestResult );
    this.callbackSequence = new CallbackListenerSequence<R>( );
    this.cb = cb;
    this.wrapperCallback = new TwiceChecked<Q, R>( ) {
      
      @Override
      public void fireException( Throwable t ) {
        try {
          cb.fireException( t );
          AsyncRequest.this.result.setException( t );
        } catch ( Throwable ex ) {
          AsyncRequest.this.result.setException( t );
          LOG.error( ex, ex );
        }
        try {
          AsyncRequest.this.callbackSequence.fireException( t );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
      
      @Override
      public void fire( R r ) {
        try {
          if ( Logs.EXTREME ) {
            Logs.extreme( ).debug( cb.getClass( ).getCanonicalName( ) + ".fire():\n" + r );
          }
          cb.fire( r );
          AsyncRequest.this.result.set( r );
          try {
            AsyncRequest.this.callbackSequence.fire( r );
          } catch ( Throwable ex ) {
            LOG.error( ex, ex );
            AsyncRequest.this.result.setException( ex );
          }
        } catch ( RuntimeException ex ) {
          LOG.error( ex, ex );
          AsyncRequest.this.result.setException( ex );
          AsyncRequest.this.callbackSequence.fireException( ex );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          AsyncRequest.this.result.setException( ex );
          AsyncRequest.this.callbackSequence.fireException( ex );
        }
      }
      
      @Override
      public void initialize( Q request ) throws Exception {
        if ( Logs.EXTREME ) {
          Logs.extreme( ).debug( cb.getClass( ).getCanonicalName( ) + ".initialize():\n" + request );
        }
        try {
          cb.initialize( request );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
          AsyncRequest.this.result.setException( ex );
          AsyncRequest.this.callbackSequence.fireException( ex );
        }
      }
    };
    Callbacks.addListenerHandler( requestResult, this.wrapperCallback );
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#dispatch(java.lang.String)
   * @param clusterOrPartition
   * @return
   */
  @Override
  public CheckedListenableFuture<R> dispatch( String clusterOrPartition ) {//TODO:GRZE:ASAP: get rid of this method
    ServiceConfiguration serviceConfig;
    try {
      serviceConfig = Components.lookup( ClusterController.class ).lookupServiceConfiguration( clusterOrPartition );
    } catch ( NoSuchElementException ex ) {
      serviceConfig = Partitions.lookupService( ClusterController.class, clusterOrPartition );
    }
    return this.dispatch( serviceConfig );
  }
  
//  @ConfigurableField( initial = "8", description = "Maximum number of concurrent messages sent to a single CC at a time." )
  public static Integer NUM_WORKERS = 8;
  
  /**
   * @see com.eucalyptus.util.async.Request#dispatch(java.lang.String)
   * @param cluster
   * @return
   */
  @Override
  public CheckedListenableFuture<R> dispatch( final ServiceConfiguration serviceConfig ) {
    try {
      serviceConfig.lookupService( ).enqueue( this );
      return this.getResponse( );
    } catch ( Exception ex1 ) {
      Future<CheckedListenableFuture<R>> res = Threads.lookup( Empyrean.class, AsyncRequest.class, serviceConfig.getFullName( ).toString( ) ).limitTo( NUM_WORKERS ).submit( new Callable<CheckedListenableFuture<R>>( ) {
                                                                                                                                                                               
                                                                                                                                                                               @Override
                                                                                                                                                                               public CheckedListenableFuture<R> call( ) throws Exception {
                                                                                                                                                                                 return AsyncRequest.this.execute( serviceConfig ).getResponse( );
                                                                                                                                                                               }
                                                                                                                                                                             } );
      try {
        res.get( ).get( );
      } catch ( ExecutionException ex ) {
        LOG.error( ex, ex );
      } catch ( InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
        LOG.error( ex, ex );
      }
      return this.getResponse( );
    }
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#sendSync(com.eucalyptus.component.ServiceEndpoint)
   * @param endpoint
   * @return
   * @throws ExecutionException
   * @throws InterruptedException
   */
  @Override
  public R sendSync( ServiceConfiguration serviceConfig ) throws ExecutionException, InterruptedException {
    return this.execute( serviceConfig ).getResponse( ).get( );
  }
  
  public Request<Q, R> execute( ServiceConfiguration config ) {
    this.doInitializeCallback( config );
    try {
      Logger.getLogger( this.cb.getClass( ) ).debug( "fire: endpoint " + config );
      if ( !this.handler.fire( config, this.request ) ) {
        LOG.error( "Error occurred while trying to send request: " + this.request );
        if ( !this.requestResult.isDone( ) ) {
          RequestException ex = new RequestException( "Error occured attempting to fire the request.", this.getRequest( ) );
          try {
            this.result.setException( ex );
          } catch ( Throwable t ) {}
        }
      } else {
        try {
          this.requestResult.get( );
        } catch ( ExecutionException ex ) {
          LOG.error( ex, ex );
        } catch ( InterruptedException ex ) {
          LOG.error( ex, ex );
        }
      }
    } catch ( RuntimeException ex ) {
      LOG.error( ex, ex );
      this.result.setException( ex );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      this.result.setException( ex );
      throw new RuntimeException( ex );
    }
    return this;
  }
  
  private void doInitializeCallback( ServiceConfiguration config ) throws RequestException {
    Logger.getLogger( this.wrapperCallback.getClass( ) ).trace( "initialize: endpoint " + config );
    try {
      this.wrapperCallback.initialize( this.request );
    } catch ( Throwable e ) {
      Logger.getLogger( this.wrapperCallback.getClass( ) ).error( e.getMessage( ), e );
      RequestException ex = ( e instanceof RequestException )
        ? ( RequestException ) e
        : new RequestInitializationException( this.wrapperCallback.getClass( ).getSimpleName( ) + " failed: " + e.getMessage( ), e, this.getRequest( ) );
      this.result.setException( ex );
      throw ex;
    }
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
    return this.wrapperCallback;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#getResponse()
   * @return
   */
  @Override
  public CheckedListenableFuture<R> getResponse( ) {
    return this.result;
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
  
  @Override
  public String toString( ) {
    return String.format( "AsyncRequest:callback=%s", this.wrapperCallback );
  }
  
}

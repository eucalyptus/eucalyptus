/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Callback.TwiceChecked;
import com.eucalyptus.util.Exceptions;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class AsyncRequest<Q extends BaseMessage, R extends BaseMessage> implements Request<Q, R> {
  private static Logger                     LOG = Logger.getLogger( AsyncRequest.class );
  private final Callback.TwiceChecked<Q, R> wrapperCallback;
  private final CheckedListenableFuture<R>  requestResult;
  private final CheckedListenableFuture<R>  result;
  private final RequestHandler<Q, R>        handler;
  private final CallbackListenerSequence<R> callbackSequence;
  private Q                                 request;
  
  protected AsyncRequest( final TwiceChecked<Q, R> cb ) {
    super( );
    this.result = new AsyncResponseFuture<R>( );
    this.requestResult = new AsyncResponseFuture<R>( );
    this.handler = new AsyncRequestHandler<Q, R>( this, this.requestResult );
    this.callbackSequence = new CallbackListenerSequence<R>( );
    this.wrapperCallback = new TwiceChecked<Q, R>( ) {
      
      @Override
      public void fireException( Throwable t ) {
        try {
          cb.fireException( t );
          AsyncRequest.this.result.setException( t );
        } catch ( Exception ex ) {
          AsyncRequest.this.result.setException( t );
          Logs.extreme( ).error( ex, ex );
        }
        try {
          AsyncRequest.this.callbackSequence.fireException( t );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
        }
      }
      
      @Override
      public void fire( R r ) {
        try {
          if ( Logs.isExtrrreeeme( ) ) {
            Logs.extreme( ).debug( cb.getClass( ).getCanonicalName( ) + ".fire():\n"
                + r );
          }
          cb.fire( r );
          AsyncRequest.this.result.set( r );
          try {
            AsyncRequest.this.callbackSequence.fire( r );
          } catch ( Exception ex ) {
            Logs.extreme( ).error( ex, ex );
            AsyncRequest.this.result.setException( ex );
          }
        } catch ( RuntimeException ex ) {
          Logs.extreme( ).error( ex, ex );
          try {
            cb.fireException( ex );
          } catch ( Exception ex1 ) {
            Logs.extreme( ).error( ex, ex );
          }
          AsyncRequest.this.result.setException( ex );
          AsyncRequest.this.callbackSequence.fireException( ex );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
          try {
            cb.fireException( ex );
          } catch ( Exception ex1 ) {
            Logs.extreme( ).error( ex1, ex1 );
          }
          AsyncRequest.this.result.setException( ex );
          AsyncRequest.this.callbackSequence.fireException( ex );
        }
      }
      
      @Override
      public void initialize( Q request ) throws Exception {
        if ( Logs.isExtrrreeeme( ) ) {
          Logs.extreme( ).debug( cb.getClass( ).getCanonicalName( ) + ".initialize():\n" + request );
        }
        try {
          cb.initialize( request );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
          AsyncRequest.this.result.setException( ex );
          AsyncRequest.this.callbackSequence.fireException( ex );
          throw ex;
        }
      }
      
      @Override
      public String toString( ) {
        return AsyncRequest.class.getSimpleName( ) + ":"
               + cb.toString( );
      }
    };
    Callbacks.addListenerHandler( this.requestResult, this.wrapperCallback );
  }

//  @ConfigurableField( initial = "8", description = "Maximum number of concurrent messages sent to a single CC at a time." )
  public static Integer NUM_WORKERS = 8;
  
  /**
   *
   */
  @Override
  public CheckedListenableFuture<R> dispatch( final ServiceConfiguration serviceConfig ) {
    Callable<CheckedListenableFuture<R>> call = new Callable<CheckedListenableFuture<R>>( ) {
      @Override
      public String toString( ) {
        return AsyncRequest.class.getSimpleName( ) + ":"
               + serviceConfig.getFullName( )
               + ":"
               + AsyncRequest.this.getRequest( ).toSimpleString( );
      }
      
      @Override
      public CheckedListenableFuture<R> call( ) throws Exception {
        try {
          Request<Q, R> execute = AsyncRequest.this.execute( serviceConfig );
          return execute.getResponse( );
        } catch ( Exception ex ) {
          AsyncRequest.this.result.setException( ex );
          Logs.extreme( ).error( ex, ex );
          throw ex;
        }
      }
    };
    try {
      try{
        this.getRequest().lookupAndSetCorrelationId();
      }catch(final Exception ex){
        ;
      }
      Future<CheckedListenableFuture<R>> res = Threads.enqueue( serviceConfig, call );//TODO:GRZE: what happens to 'res'?
      return this.getResponse( );
    } catch ( Exception ex1 ) {
      LOG.error( ex1 );
      Logs.extreme( ).error( ex1, ex1 );
      Future<CheckedListenableFuture<R>> res = Threads.lookup( Empyrean.class,
                                                               AsyncRequest.class,
                                                               serviceConfig.getFullName( ).toString( ) )
                                                      .limitTo( NUM_WORKERS )
                                                      .submit( call );
      return this.getResponse( );
    }
  }
  
  /**
   *
   */
  @Override
  public R sendSync( ServiceConfiguration serviceConfig ) throws ExecutionException, InterruptedException {
    try{
      this.getRequest().lookupAndSetCorrelationId();
    }catch(final Exception ex){
      ;
    }
    Request<Q, R> asyncRequest = this.execute( serviceConfig );
    return asyncRequest.getResponse( ).get( );
  }
  
  public Request<Q, R> execute( ServiceConfiguration config ) {
    this.doInitializeCallback( config );
    try {
     Logs.extreme( ).debug( "fire: endpoint " + config );
      if ( !this.handler.fire( config, this.request ) ) {
        Logs.extreme( ).error( "Error occurred while trying to send request: " + this.request );
        RequestException ex = new RequestException( "Error occured attempting to fire the request.", this.getRequest( ) );
        try {
          this.result.setException( ex );
        } catch ( Exception t ) {}
      }
    } catch ( Exception ex ) {
      Exceptions.maybeInterrupted( ex );
      Logs.extreme( ).error( ex, ex );
      this.result.setException( ex );
      throw Exceptions.toUndeclared( ex );
    }
    return this;
  }
  
  private void doInitializeCallback( ServiceConfiguration config ) throws RequestException {
    Logs.extreme( ).info( "initialize: endpoint " + config + " request " + this.request.getClass( ).getSimpleName( ) + ":" + this.request.toSimpleString( ) );
    try {
      this.wrapperCallback.initialize( this.request );
    } catch ( Exception e ) {
      Logs.extreme( ).error( e.getMessage( ), e );
      RequestException ex = ( e instanceof RequestException )
                                                             ? ( RequestException ) e
                                                             : new RequestInitializationException( this.wrapperCallback.getClass( ).getSimpleName( )
                                                                                                   + " failed: "
                                                                                                   + e.getMessage( ), e, this.getRequest( ) );
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
  public Request<Q, R> then( UnconditionalCallback<? super R> callback ) {
    this.callbackSequence.addCallback( callback );
    return this;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#then(com.eucalyptus.util.Callback.Completion)
   * @param callback
   * @return
   */
  @Override
  public Request<Q, R> then( Callback.Completion<? super R> callback ) {
    this.callbackSequence.addCallback( callback );
    return this;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#then(com.eucalyptus.util.Callback.Failure)
   * @param callback
   * @return
   */
  @Override
  public Request<Q, R> then( Callback.Failure<? super R> callback ) {
    this.callbackSequence.addFailureCallback( callback );
    return this;
  }
  
  /**
   * @see com.eucalyptus.util.async.Request#then(com.eucalyptus.util.Callback.Success)
   * @param callback
   * @return
   */
  @Override
  public Request<Q, R> then( Callback.Success<? super R> callback ) {
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

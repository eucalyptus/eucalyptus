package com.eucalyptus.util.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.Callback.Checked;
import com.eucalyptus.util.concurrent.GenericFuture;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Futures {
  
  public static <R> CheckedListenableFuture<R> newAsyncMessageFuture( ) {
    return new AsyncResponseFuture<R>( );
  }
  
  @SuppressWarnings( "unchecked" )
  public static Runnable addListenerHandler( CheckedListenableFuture<?> future, Callback<?> listener ) {
    Runnable r;
    future.addListener( r = new BasicCallbackProcessor( future, listener ), Threads.currentThreadExecutor( ) );
    return r;
  }
  
  public static <T> CheckedListenableFuture<T> predestinedFuture( final T initValue ) {
    return new GenericFuture<T>( ) {
      {
        set( initValue );
      }
    };
  }
  
  public static <T> CheckedListenableFuture<T> newGenericFuture( ) {
    return new GenericFuture<T>( );
  }
  
  static class BasicCallbackProcessor<R extends BaseMessage> implements Runnable {
    private final Callback<R> callback;
    private final Future<R>   future;
    private Logger            LOG;
    
    private BasicCallbackProcessor( Future<R> future, Callback<R> callback ) {
      this.callback = callback;
      this.future = future;
      this.LOG = Logger.getLogger( this.callback.getClass( ) );
    }
    
    @Override
    public void run( ) {
      R reply = null;
      try {
        reply = this.future.get( );
        if ( reply == null ) {
          this.LOG.warn( "Application of callback resulted in null value: " + this.getClass( ).getSimpleName( ) );
          Exceptions.eat( "Callback marked as done has null valued response: " + reply );
        }
        try {
          this.LOG.trace( EventRecord.here( this.getClass( ), EventType.CALLBACK, "fire(" + reply.getClass( ).getSimpleName( ) + ")" ).toString( ) );
          this.callback.fire( reply );
        } catch ( Throwable ex ) {
          this.LOG.error( EventRecord.here( this.getClass( ), EventType.CALLBACK, "FAILED", "fire(" + reply.getClass( ).getSimpleName( ) + ")", ex.getMessage( ) ).toString( ),
                          ex );
          this.doFail( ex );
        }
      } catch ( Throwable e ) {
        this.LOG.error( EventRecord.here( this.getClass( ), EventType.FUTURE, "FAILED", "get()", e.getMessage( ) ).toString( ), e );
        this.doFail( e );
      }
    }
    
    private final void doFail( Throwable failure ) {
      if ( ( failure instanceof ExecutionException ) && failure.getCause( ) != null ) {
        failure = failure.getCause( );
      }
      if ( Callback.Checked.class.isAssignableFrom( this.callback.getClass( ) ) ) {
        try {
          this.LOG.trace( EventRecord.here( this.callback.getClass( ), EventType.CALLBACK, "fireException(" + failure.getClass( ).getSimpleName( ) + ")",
                                            failure.getMessage( ) ), failure );
          ( ( Checked ) this.callback ).fireException( failure );
        } catch ( Throwable t ) {
          this.LOG.error( "BUG: an error occurred while trying to process an error.  Previous error was: " + failure.getMessage( ), t );
        }
      } else if ( Callback.Completion.class.isAssignableFrom( this.callback.getClass( ) ) ) {
        this.LOG.trace( EventRecord.here( this.callback.getClass( ), EventType.CALLBACK, "fire(" + failure.getClass( ).getSimpleName( ) + ")",
                                          failure.getMessage( ) ), failure );
        ( ( Callback.Completion ) this.callback ).fire( );
      }
    }
    
    @Override
    public String toString( ) {
      return String.format( "BasicCallbackProcessor:callback=%s", this.callback.getClass( ).getName( ).replaceAll( "^(\\w.)*", "" ) );
    }
    
  }
  
}

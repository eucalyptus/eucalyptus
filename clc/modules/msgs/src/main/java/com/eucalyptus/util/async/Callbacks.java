package com.eucalyptus.util.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.Callback.Checked;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Callbacks {
  private static Logger LOG = Logger.getLogger( Callbacks.class );
  
  public static Callback forCallable( final Callable callable ) {
    return new Callback( ) {
      
      @Override
      public void fire( final Object t ) {
        try {
          callable.call( );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };
  }
  
  public static Callback forRunnable( final Runnable runnable ) {
    return new Callback( ) {
      
      @Override
      public void fire( final Object t ) {
        try {
          runnable.run( );
        } catch ( final Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };
  }
  
  public static <T> Callback<T> noop( ) {
    return new NoopCallback<T>( );
  }
  
  private static final class NoopCallback<T> implements Callback<T> {
    @Override
    public final void fire( final T t ) {}
  }
  
  static class BasicCallbackProcessor<R extends BaseMessage> implements Runnable {
    private final Callback<R> callback;
    private final Future<R>   future;
    private final Logger      LOG;
    
    private BasicCallbackProcessor( final Future<R> future, final Callback<R> callback ) {
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
        } catch ( final Throwable ex ) {
          this.LOG.error( EventRecord.here( this.getClass( ), EventType.CALLBACK, "FAILED", "fire(" + reply.getClass( ).getSimpleName( ) + ")", ex.getMessage( ) ).toString( ) );
          this.doFail( ex );
        }
      } catch ( final Throwable e ) {
        this.LOG.error( EventRecord.here( this.getClass( ), EventType.FUTURE, "FAILED", "get()", e.getMessage( ) ).toString( ) );
        this.doFail( e );
      }
    }
    
    private final void doFail( Throwable failure ) {
      this.LOG.trace( EventRecord.here( BasicCallbackProcessor.class, EventType.CALLBACK, this.callback.getClass( ).toString( ), "fireException(" + failure.getClass( ).getSimpleName( ) + ")" ) );
      this.LOG.trace( failure.getMessage( ), failure );
      if ( this.callback instanceof Callback.Checked ) {
          ( ( Checked ) this.callback ).fireException( failure );
      } else if ( this.callback instanceof Callback.Completion ) {
        ( ( Callback.Completion ) this.callback ).fire( );
      }
    }
    
    @Override
    public String toString( ) {
      return String.format( "BasicCallbackProcessor:callback=%s", this.callback.getClass( ).getName( ).replaceAll( "^(\\w.)*", "" ) );
    }
    
  }
  
  @SuppressWarnings( "unchecked" )
  public static Runnable addListenerHandler( final CheckedListenableFuture<?> future, final Callback<?> listener ) {
    Runnable r;
    future.addListener( r = new Callbacks.BasicCallbackProcessor( future, listener ),
                        Threads.lookup( Empyrean.class, Callbacks.class, BasicCallbackProcessor.class.toString( ) ) );
    return r;
  }
  
}

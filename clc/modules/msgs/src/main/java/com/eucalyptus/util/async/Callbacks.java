package com.eucalyptus.util.async;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipelineFactory;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.Callback.Checked;
import com.eucalyptus.util.async.Callback.Completion;
import com.eucalyptus.util.async.Callbacks.BasicCallbackProcessor;
import com.google.common.base.Predicate;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Callbacks {
  private static Logger LOG = Logger.getLogger( Callbacks.class );
  
  public static Callback forCallable( final Callable callable ) {
    return new Callback( ) {
      
      @Override
      public void fire( Object t ) {
        try {
          callable.call( );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };
  }
  
  public static Callback forRunnable( final Runnable runnable ) {
    return new Callback( ) {
      
      @Override
      public void fire( Object t ) {
        try {
          runnable.run( );
        } catch ( Exception ex ) {
          LOG.error( ex, ex );
        }
      }
    };
  }
  
  public static <T> CheckedListenableFuture<T> chain( Callable<CheckedListenableFuture<T>>... callables ) {
    for( Callable<CheckedListenableFuture<T>> c : callables ) {
      c.call( ).addListener( new Runnable( ) {

        @Override
        public void run( ) {}} );
    }
  }
  
  public static <T> Callback<T> noop( ) {
    return new NoopCallback<T>( );
  }
  
  private static final class NoopCallback<T> implements Callback<T> {
    @Override
    public final void fire( T t ) {}
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
          this.LOG.error( EventRecord.here( this.getClass( ), EventType.CALLBACK, "FAILED", "fire(" + reply.getClass( ).getSimpleName( ) + ")", ex.getMessage( ) ).toString( ) );
          this.doFail( ex );
        }
      } catch ( Throwable e ) {
        this.LOG.error( EventRecord.here( this.getClass( ), EventType.FUTURE, "FAILED", "get()", e.getMessage( ) ).toString( ) );
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
                                            failure.getMessage( ) )/*, Exceptions.filterStackTrace( failure, 2 )*/);
          ( ( Checked ) this.callback ).fireException( failure );
        } catch ( Throwable t ) {
          this.LOG.error( "BUG: an error occurred while trying to process an error.  Previous error was: " + failure.getMessage( ), t );
        }
      } else if ( Callback.Completion.class.isAssignableFrom( this.callback.getClass( ) ) ) {
        this.LOG.trace( EventRecord.here( this.callback.getClass( ), EventType.CALLBACK, "fire(" + failure.getClass( ).getSimpleName( ) + ")",
                                          failure.getMessage( ) )/*, Exceptions.filterStackTrace( failure, 2 )*/);
        ( ( Callback.Completion ) this.callback ).fire( );
      }
    }
    
    @Override
    public String toString( ) {
      return String.format( "BasicCallbackProcessor:callback=%s", this.callback.getClass( ).getName( ).replaceAll( "^(\\w.)*", "" ) );
    }
    
  }

  @SuppressWarnings( "unchecked" )
  public static Runnable addListenerHandler( CheckedListenableFuture<?> future, Callback<?> listener ) {
    Runnable r;
    future.addListener( r = new Callbacks.BasicCallbackProcessor( future, listener ), Threads.currentThreadExecutor( ) );
    return r;
  }
  
}

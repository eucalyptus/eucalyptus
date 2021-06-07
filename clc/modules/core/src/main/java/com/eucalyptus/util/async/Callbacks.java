/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
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

  public static <R> Callback.Failure<R> noopFailure() {
    return new NoopFailure<R>();
  }

  private static final class NoopCallback<T> implements Callback<T> {
    @Override
    public final void fire( final T t ) {}
  }

  private static final class NoopFailure<R> extends Callback.Failure<R> {
    @Override
    public void fireException( final Throwable t ) {}
  }

  static class BasicCallbackProcessor<R extends BaseMessage> implements Runnable {
    private final Callback<R> callback;
    private final Future<R>   future;
    private final Logger      log;

    private BasicCallbackProcessor( final Future<R> future, final Callback<R> callback ) {
      this.callback = callback;
      this.future = future;
      this.log = Logger.getLogger( this.callback.getClass( ) );
    }

    @Override
    public void run( ) {
      R reply = null;
      try {
        reply = this.future.get( );
        try {
          EventRecord.here( this.getClass( ), EventType.CALLBACK, "fire(" + reply.getClass( ).getSimpleName( ) + ")" ).exhaust( );
          this.callback.fire( reply );
        } catch ( final Throwable ex ) {
          EventRecord.here( this.getClass( ), EventType.CALLBACK, "FAILED", "fire(" + reply.getClass( ).getSimpleName( ) + ")", ex.getMessage( ) ).exhaust( );
          this.doFail( ex );
        }
      } catch ( final Throwable e ) {
        EventRecord.here( this.getClass( ), EventType.FUTURE, "FAILED", "get()", e.getMessage( ) ).exhaust( );
        this.doFail( e instanceof ExecutionException ? e.getCause( ) : e );
      }
    }

    private final void doFail( Throwable failure ) {
      EventRecord.here( BasicCallbackProcessor.class, EventType.CALLBACK, this.callback.getClass( ).toString( ),
                        "fireException(" + failure.getClass( ).getSimpleName( ) + ")" ).exhaust( );
      this.log.trace( failure.getMessage( ), failure );
      if ( this.callback instanceof Callback.Checked ) {
        ( ( Checked ) this.callback ).fireException( failure );
      } else if ( this.callback instanceof Callback.Completion ) {
        ( ( Callback.Completion ) this.callback ).fireException( failure );
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
                        Threads.lookup( Empyrean.class, Callbacks.class, BasicCallbackProcessor.class.getSimpleName( ) ) );
    return r;
  }

}

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

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Callback;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @param <T>
 * @param <R>
 */
public class CallbackListenerSequence<R extends BaseMessage> implements Callback.Checked<R> {
  private Logger                    LOG              = Logger.getLogger( this.getClass( ) );
  private List<Callback<? super R>>         successCallbacks = Lists.newArrayList( );
  private List<Callback.Checked<? super R>> failureCallbacks = Lists.newArrayList( );
  
  /**
   * Add a callback which is to be invoked when the operation completes, regardless of the outcome.
   * 
   * @param c
   *          - callback to invoke
   * @return <tt>this</tt>
   */
  public CallbackListenerSequence<R> addCallback( final UnconditionalCallback<? super R> c ) {
    EventRecord.caller( CallbackListenerSequence.class, EventType.CALLBACK, UnconditionalCallback.class.getSimpleName( ), c.getClass( ) ).extreme( );
    this.successCallbacks.add( c );
    this.failureCallbacks.add( new Callback.Failure<Object> () {
      @Override
      public void fireException( Throwable t ) {
        c.fire( );
      }      
    } );
    return this;
  }
  
  /**
   * Add a callback which is to be invoked when the operation completes, regardless of the outcome.
   * 
   * @param c
   *          - callback to invoke
   * @return <tt>this</tt>
   */
  public CallbackListenerSequence<R> addCallback( Callback.Checked<? super R> c ) {
    EventRecord.caller( CallbackListenerSequence.class, EventType.CALLBACK, Callback.Checked.class.getSimpleName( ), c.getClass( ) ).extreme( );
    this.successCallbacks.add( c );
    this.failureCallbacks.add( c );
    return this;
  }

  /**
   * Add a callback which is to be invoked if the operation succeeds.
   * 
   * @param c
   *          - callback to invoke
   * @return <tt>this</tt>
   */
  public CallbackListenerSequence<R> addSuccessCallback( Callback.Success<? super R> c ) {
    EventRecord.caller( CallbackListenerSequence.class, EventType.CALLBACK, Callback.Success.class.getSimpleName( ), c.getClass( ) ).extreme( );
    this.successCallbacks.add( c );
    return this;
  }
  
  /**
   * Add a callback which is to be invoked if the operation fails.
   * 
   * @param c
   *          - callback to invoke
   * @return <tt>this</tt>
   */
  public CallbackListenerSequence<R> addFailureCallback( Callback.Failure<? super R> c ) {
    EventRecord.caller( CallbackListenerSequence.class, EventType.CALLBACK, Callback.Failure.class.getSimpleName( ), c.getClass( ) ).extreme( );
    this.failureCallbacks.add( c );
    return this;
  }
  
  /**
   * Fire the response on all listeners.
   *
   * @param response
   */
  @Override
  public void fire( R response ) {
    EventRecord.here( CallbackListenerSequence.class, EventType.CALLBACK, "fire(" + response.getClass( ).getName( ) + ")" ).extreme( );
    for ( Callback<? super R> cb : this.successCallbacks ) {
      try {
        EventRecord.here( this.getClass( ), EventType.CALLBACK, "" + cb.getClass( ), "fire(" + response.getClass( ).getCanonicalName( ) + ")" ).extreme( );
        cb.fire( response );
      } catch ( Exception t ) {
        this.LOG.error( "Exception occurred while trying to call: " + cb.getClass( ) + ".apply( " + t.getMessage( ) + " )" );
        this.LOG.error( t, t );
      }
    }
  }
  
  /**
   * Trigger the failure case.
   *
   * @param t
   */
  @Override
  public void fireException( Throwable t ) {
    EventRecord.here( CallbackListenerSequence.class, EventType.CALLBACK, "fireException(" + t.getClass( ).getName( ) + ")" ).extreme( );
    for ( Callback.Checked<? super R> cb : this.failureCallbacks ) {
      try {
        EventRecord.here( this.getClass( ), EventType.CALLBACK, "" + cb.getClass( ), "fireException(" + t.getClass( ).getCanonicalName( ) + ")" ).extreme( );
        cb.fireException( t );
      } catch ( Exception t2 ) {
        this.LOG.error( "Exception occurred while trying to call: " + cb.getClass( ) + ".failure( " + t.getMessage( ) + " )" );
        this.LOG.error( t2, t2 );
      }
    }
  }

  
}

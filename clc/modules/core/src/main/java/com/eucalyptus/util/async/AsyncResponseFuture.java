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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.concurrent.GenericCheckedListenableFuture;

public class AsyncResponseFuture<R> extends GenericCheckedListenableFuture<R> {
  private static Logger LOG = Logger.getLogger( AsyncResponseFuture.class );
  
  AsyncResponseFuture( ) {
    super( );
  }

  /**
   * @see com.eucalyptus.util.async.CheckedListenableFuture#setException(java.lang.Throwable)
   * @param exception
   * @return
   */
  @Override
  public boolean setException( Throwable exception ) {
    boolean r = super.setException( exception );
    if ( r ) {
      EventRecord.caller( this.getClass( ), EventType.FUTURE, "setException(" + exception.getClass( ).getCanonicalName( ) + "): " + exception.getMessage( ) ).trace( );
    } else {
      Logs.exhaust( ).debug( "Duplicate exception: " + exception.getMessage( ) );
    }
    return r;
  }
  
  /**
   * @see com.eucalyptus.util.async.CheckedListenableFuture#get(long, java.util.concurrent.TimeUnit)
   * @param timeout
   * @param unit
   * @return
   * @throws InterruptedException
   * @throws TimeoutException
   * @throws ExecutionException
   */
  @Override
  public R get( long timeout, TimeUnit unit ) throws InterruptedException, TimeoutException, ExecutionException {
    return super.get( timeout, unit );
  }
  
  /**
   * @see com.eucalyptus.util.async.CheckedListenableFuture#get()
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Override
  public R get( ) throws InterruptedException, ExecutionException {
    return super.get( );
  }
  
  /**
   * @see com.eucalyptus.util.async.CheckedListenableFuture#set(R)
   * @param reply
   * @return
   */
  @Override
  public boolean set( R reply ) {
    boolean r = super.set( reply );
    if( r ) {
      EventRecord.caller( this.getClass( ), EventType.FUTURE, "set(" + reply.getClass( ).getCanonicalName( ) + ")" ).trace( );
    } else {
      if ( Logs.exhaust( ).isDebugEnabled( ) ) {
        Logs.exhaust( ).debug( "Duplicate response: " + reply );
      }
    }
    return r;
  }
  
  /**
   * @see com.eucalyptus.util.async.CheckedListenableFuture#cancel(boolean)
   * @param mayInterruptIfRunning
   * @return
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return super.cancel();
  }

  /**
   * @see com.eucalyptus.util.async.CheckedListenableFuture#isCanceled()
   * @return
   */
  @Override
  public boolean isCanceled( ) {
    return super.isCancelled( );
  }
}

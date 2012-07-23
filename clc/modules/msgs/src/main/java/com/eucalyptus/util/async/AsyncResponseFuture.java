/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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
      Logs.exhaust( ).debug( "Duplicate response: " + reply );
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

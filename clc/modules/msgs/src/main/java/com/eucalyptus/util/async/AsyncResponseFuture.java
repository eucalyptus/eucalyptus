package com.eucalyptus.util.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.concurrent.AbstractListenableFuture;

public class AsyncResponseFuture<R> extends AbstractListenableFuture<R> implements CheckedListenableFuture<R> {
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
    EventRecord.caller( this.getClass( ), EventType.FUTURE, "setException(" + exception.getClass( ).getCanonicalName( ) + "): " + exception.getMessage( ) ).trace( );
    boolean r = false;
    if ( exception == null ) {
      exception = new IllegalArgumentException( "setException(Throwable) was called with a null argument" );
    }
    if ( !( r = super.setException( exception ) ) ) {
      LOG.error( "Duplicate exception: " + exception.getMessage( ) );
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
    EventRecord.caller( this.getClass( ), EventType.FUTURE, "set(" + reply.getClass( ).getCanonicalName( ) + ")" ).trace( );
    boolean r = false;
    if( !( r = super.set( reply ) ) ) {
      LOG.error( "Duplicate response: " + reply );
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

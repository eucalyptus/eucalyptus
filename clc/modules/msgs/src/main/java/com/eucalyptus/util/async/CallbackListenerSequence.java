package com.eucalyptus.util.async;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Callback;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @author decker
 * @param <T>
 * @param <R>
 */
public class CallbackListenerSequence<R extends BaseMessage> implements Callback.Checked<R> {
  private Logger                    LOG              = Logger.getLogger( this.getClass( ) );
  private List<Callback<R>>         successCallbacks = Lists.newArrayList( );
  private List<Callback.Checked<R>> failureCallbacks = Lists.newArrayList( );
  
  /**
   * Add a callback which is to be invoked when the operation completes, regardless of the outcome.
   * 
   * @param c
   *          - callback to invoke
   * @return <tt>this</tt>
   */
  public CallbackListenerSequence<R> addCallback( final UnconditionalCallback c ) {
    EventRecord.caller( CallbackListenerSequence.class, EventType.CALLBACK, UnconditionalCallback.class.getSimpleName( ), c.getClass( ) ).extreme( );
    this.successCallbacks.add( c );
    this.failureCallbacks.add( new Callback.Failure() {
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
  public CallbackListenerSequence<R> addCallback( Callback.Checked c ) {
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
  @SuppressWarnings( "unchecked" )
  public CallbackListenerSequence<R> addSuccessCallback( Callback.Success<R> c ) {
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
  public CallbackListenerSequence<R> addFailureCallback( Callback.Failure c ) {
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
    for ( Callback<R> cb : this.successCallbacks ) {
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
    for ( Callback.Checked<R> cb : this.failureCallbacks ) {
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

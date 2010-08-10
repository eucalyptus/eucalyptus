package com.eucalyptus.util.async;

import java.util.List;
import org.apache.log4j.Logger;
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
  private List<Callback<Throwable>> failureCallbacks = Lists.newArrayList( );
  
  /**
   * Add a callback which is to be invoked when the operation completes, regardless of the outcome.
   * 
   * @param c
   *          - callback to invoke
   * @return <tt>this</tt>
   */
  public CallbackListenerSequence<R> addCallback( UnconditionalCallback c ) {
    this.successCallbacks.add( c );
    this.failureCallbacks.add( c );
    return this;
  }
  
  /**
   * Add a callback which is to be invoked when the operation completes, regardless of the outcome.
   * 
   * @param c
   *          - callback to invoke
   * @return <tt>this</tt>
   */
  public CallbackListenerSequence<R> addCallback( Callback.Completion c ) {
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
    for ( Callback<R> cb : this.successCallbacks ) {
      try {
        cb.fire( response );
      } catch ( Throwable t ) {
        LOG.error( "Exception occurred while trying to call: " + cb.getClass( ).getSimpleName( ) + ".apply( " + t.getMessage( ) + " )" );
        LOG.error( t, t );
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
    for ( Callback cb : this.failureCallbacks ) {
      try {
        LOG.debug( this.getClass( ).getSimpleName( ) + ":failure(" + t.getClass( ).getSimpleName( ) + ") invoking callback: " + cb.getClass( ).getSimpleName( ) );
        cb.fire( t );
      } catch ( Throwable t2 ) {
        LOG.error( "Exception occurred while trying to call: " + cb.getClass( ).getSimpleName( ) + ".failure( " + t.getMessage( ) + " )" );
        LOG.error( t2, t2 );
      }
    }
  }

  
}

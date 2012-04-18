package com.eucalyptus.util.async;

import com.eucalyptus.util.Callback;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * An asynchronous callback which involves remote communication (read: possibly long lasting). Implementors of this interface are guaranteed that:
 * 
 * <ul>
 * <li>{@#initialize(BaseMessage)} will be invoked before communication begins.</li>
 * <li>Should the invokation of {@#initialize(BaseMessage)} terminate prematurely the callback will be disposed of -- preconditions
 * caused a failure.</li>
 * <li>Otherwise, either {@#fire(BaseMessage)} <b>OR</b> {@#fireException(Throwable)} will be called exactly
 * one time.</li>
 * <li>The argument to {@#fire(BaseMessage)}/{@#fireException(Throwable)} will be the outcome of the remote
 * operation.</li>
 * </ul>
 * @see Callback
 * @see Callback.Checked
 * @see Callback.TwiceChecked
 * @see RemoteCallback#fire(BaseMessage)
 * @see RemoteCallback#fireException(Throwable)
 */
public interface RemoteCallback<Q extends BaseMessage, R extends BaseMessage> extends Callback.TwiceChecked<Q, R> {
  
  /**
   * Get the request being made by this callback.
   * 
   * @return
   */
  public abstract Q getRequest( );
  
  /**
   * Initialize (e.g., check preconditions) before proceeding with the
   * connection and sending of <tt>request</tt>. Guaranteed to be called only
   * once per corresponding dispatch.
   * 
   * @param request
   * @throws Exception
   */
  public abstract void initialize( Q request ) throws Exception;
  
  /**
   * The operation completed with a response: <tt>msg</tt>.
   * 
   * @param msg
   * @throws Exception
   */
  public abstract void fire( R msg );
  
  /**
   * The operation completed with an exception.
   * 
   * @param t
   */
  public abstract void fireException( Throwable t );
  
}

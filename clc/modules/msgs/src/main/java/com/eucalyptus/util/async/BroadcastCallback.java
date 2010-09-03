/**
 * 
 */
package com.eucalyptus.util.async;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * Callback which will be broadcast to every cluster.  
 * @author decker
 *
 * @param <TYPE> Request type
 * @param <RTYPE> Response type
 */
public abstract class BroadcastCallback<TYPE extends BaseMessage,RTYPE extends BaseMessage> extends MessageCallback<TYPE,RTYPE> {

  public abstract BroadcastCallback<TYPE,RTYPE> newInstance( );
    
}

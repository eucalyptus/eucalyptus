/**
 * 
 */
package com.eucalyptus.cluster.callback;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class MultiClusterCallback<TYPE extends BaseMessage,RTYPE extends BaseMessage> extends QueuedEventCallback<TYPE,RTYPE> {
  
  public abstract MultiClusterCallback<TYPE,RTYPE> newInstance( );
    
}

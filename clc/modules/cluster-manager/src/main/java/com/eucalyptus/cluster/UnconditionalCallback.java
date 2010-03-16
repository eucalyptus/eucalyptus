package com.eucalyptus.cluster;

import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class UnconditionalCallback<T extends BaseMessage, R extends BaseMessage> implements FailureCallback<T,R>, SuccessCallback<T> {
  
  @Override
  public void failure( QueuedEventCallback<T,R> precedingCallback, Throwable t ) {
    this.apply( );
  }
  
  @Override
  public void apply( T t ) {
    this.apply( );
  }
  
  public abstract void apply( );
}

package com.eucalyptus.cluster;

import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;

public abstract class UnconditionalCallback<T> implements FailureCallback<T>, SuccessCallback<T> {
  
  @Override
  public void failure( QueuedEventCallback<T> precedingCallback, Throwable t ) {
    this.apply( );
  }
  
  @Override
  public void apply( T t ) {
    this.apply( );
  }
  
  public abstract void apply( );
}

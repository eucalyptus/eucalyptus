package com.eucalyptus.cluster;

import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;

public interface SuccessCallback<T> extends Callback {
  public void apply( T t );
  @SuppressWarnings( "unchecked" )
  public static SuccessCallback NOOP = new SuccessCallback() {public void apply( Object t ) {}};
}

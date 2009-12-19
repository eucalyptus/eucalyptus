package com.eucalyptus.cluster;

import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;

public interface FailureCallback<T> extends Callback {
  public void failure( QueuedEventCallback<T> precedingCallback, Throwable t );
  public static FailureCallback NOOP = new FailureCallback() {public void failure( QueuedEventCallback precedingCallback, Throwable t ) {}};
}

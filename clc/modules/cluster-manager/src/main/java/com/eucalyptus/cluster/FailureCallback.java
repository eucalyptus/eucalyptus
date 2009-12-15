package com.eucalyptus.cluster;

import edu.ucsb.eucalyptus.cloud.cluster.QueuedEventCallback;

public interface FailureCallback<REQ> {
  public void failure( QueuedEventCallback<REQ> precedingCallback, Throwable t );
}

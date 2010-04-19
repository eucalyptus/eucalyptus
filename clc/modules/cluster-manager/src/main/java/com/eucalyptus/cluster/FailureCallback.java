package com.eucalyptus.cluster;

import com.eucalyptus.cluster.callback.QueuedEventCallback;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface FailureCallback<T extends BaseMessage,R extends BaseMessage> extends Callback {
  public void failure( QueuedEventCallback<T,R> precedingCallback, Throwable t );
  public static FailureCallback NOOP = new FailureCallback() {public void failure( QueuedEventCallback precedingCallback, Throwable t ) {}};
}

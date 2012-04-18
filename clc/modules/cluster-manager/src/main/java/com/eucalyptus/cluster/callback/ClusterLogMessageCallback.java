package com.eucalyptus.cluster.callback;

import com.eucalyptus.cluster.Cluster;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class ClusterLogMessageCallback<Q extends BaseMessage, R extends BaseMessage> extends StateUpdateMessageCallback<Cluster, Q, R> {

  public ClusterLogMessageCallback( Q request ) {
    super( request );
  }

}

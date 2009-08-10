package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.util.*;
import com.eucalyptus.ws.util.Messaging;

/**
 * User: decker
 * Date: Dec 11, 2008
 * Time: 11:28:09 AM
 */
public class ClusterEnvelope {

  private String clusterName;
  private QueuedEvent event;

  public static void dispatch( String name, QueuedEvent event ) {
    Messaging.dispatch( EucalyptusProperties.CLUSTERSINK_REF, new ClusterEnvelope( name , event ) );
  }

  public ClusterEnvelope( final String clusterName, final QueuedEvent event ) {
    this.clusterName = clusterName;
    this.event = event;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName( final String clusterName ) {
    this.clusterName = clusterName;
  }

  public QueuedEvent getEvent() {
    return event;
  }

  public void setEvent( final QueuedEvent event ) {
    this.event = event;
  }
}

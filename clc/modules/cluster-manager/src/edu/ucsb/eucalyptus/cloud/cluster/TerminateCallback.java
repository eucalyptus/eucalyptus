package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.client.Client;
import org.apache.log4j.Logger;

public class TerminateCallback extends QueuedEventCallback<TerminateInstancesType> {

  private static Logger LOG = Logger.getLogger( TerminateCallback.class );

  public TerminateCallback() {
  }

  public void process( final Client cluster, final TerminateInstancesType msg ) throws Exception {
    cluster.send( msg );
  }

}

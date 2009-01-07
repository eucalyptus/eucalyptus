package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.RebootInstancesType;
import edu.ucsb.eucalyptus.transport.client.Client;
import org.apache.log4j.Logger;

public class RebootCallback extends QueuedEventCallback<RebootInstancesType> {

  private static Logger LOG = Logger.getLogger( RebootCallback.class );

  private Cluster parent;

  public RebootCallback( final Cluster parent )
  {
    this.parent = parent;
  }

  public void process( final Client cluster, final RebootInstancesType msg ) throws Exception
  {
    cluster.send( msg );
  }

}

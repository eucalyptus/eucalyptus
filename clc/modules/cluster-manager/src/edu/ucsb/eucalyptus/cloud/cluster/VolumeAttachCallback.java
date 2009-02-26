package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.AttachVolumeType;
import edu.ucsb.eucalyptus.transport.client.Client;
import org.apache.log4j.Logger;

public class VolumeAttachCallback extends QueuedEventCallback<AttachVolumeType> {

  private static Logger LOG = Logger.getLogger( VolumeAttachCallback.class );

  private Cluster parent;

  public VolumeAttachCallback( final Cluster parent )
  {
    this.parent = parent;
  }

  public void process( final Client cluster, final AttachVolumeType msg ) throws Exception
  {
    cluster.send( msg );
  }

}
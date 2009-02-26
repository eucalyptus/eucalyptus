package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.DetachVolumeType;
import edu.ucsb.eucalyptus.transport.client.Client;
import org.apache.log4j.Logger;

public class VolumeDetachCallback extends QueuedEventCallback<DetachVolumeType> {

  private static Logger LOG = Logger.getLogger( VolumeDetachCallback.class );

  public VolumeDetachCallback( ){}

  public void process( final Client cluster, final DetachVolumeType msg ) throws Exception
  {
    cluster.send( msg );
  }

}
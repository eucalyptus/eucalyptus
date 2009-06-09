package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.AttachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.AttachVolumeType;
import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.transport.client.Client;
import org.apache.log4j.Logger;

import java.util.NoSuchElementException;

public class VolumeAttachCallback extends QueuedEventCallback<AttachVolumeType> {

  private static Logger LOG = Logger.getLogger( VolumeAttachCallback.class );

  private Cluster parent;

  public VolumeAttachCallback( final Cluster parent )
  {
    this.parent = parent;
  }

  public void process( final Client cluster, final AttachVolumeType msg ) throws Exception
  {
    AttachVolumeResponseType reply = (AttachVolumeResponseType) cluster.send( msg );
    if( !reply.get_return() ) {
      LOG.debug( "Trying to remove invalid volume attachment " + msg.getVolumeId() + " from instance " + msg.getInstanceId() );
      try {
        VmInstance vm = VmInstances.getInstance().lookup( msg.getInstanceId() );
        AttachedVolume failVol = new AttachedVolume( msg.getVolumeId() );
        vm.getVolumes().remove( failVol );
        LOG.debug( "Removed failed attachment: " + failVol.getVolumeId() + " -> "+vm.getInstanceId());
        LOG.debug( "Final volume attachments for " + vm.getInstanceId() + " " + vm.getVolumes() );
      } catch ( NoSuchElementException e1 ) {}
    } else {
      try {
        VmInstance vm = VmInstances.getInstance().lookup( msg.getInstanceId() );
        AttachedVolume attachedVol = new AttachedVolume( msg.getVolumeId() );
        LOG.debug( "Volumes marked as attached " + vm.getVolumes() + " to " + vm.getInstanceId() );
        attachedVol.setStatus( "attached" );
      } catch ( NoSuchElementException e1 ) {}
    }
  }

}
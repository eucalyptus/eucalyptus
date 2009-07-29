package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.AttachedVolume;
import edu.ucsb.eucalyptus.msgs.DetachVolumeResponseType;
import edu.ucsb.eucalyptus.msgs.DetachVolumeType;
import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;

public class VolumeDetachCallback extends QueuedEventCallback<DetachVolumeType> {

  private static Logger LOG = Logger.getLogger( VolumeDetachCallback.class );

  public VolumeDetachCallback( ){}

  public void process( final Client cluster, final DetachVolumeType msg ) throws Exception
  {
    DetachVolumeResponseType reply = (DetachVolumeResponseType) cluster.send( msg );
    if( reply.get_return() ) {
      VmInstance vm = VmInstances.getInstance().lookup( msg.getInstanceId() );
      vm.getVolumes().remove( new AttachedVolume( msg.getVolumeId() ) );
    }
  }

}
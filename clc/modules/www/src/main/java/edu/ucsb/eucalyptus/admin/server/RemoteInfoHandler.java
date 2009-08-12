package edu.ucsb.eucalyptus.admin.server;

import com.google.common.collect.Sets;
import com.google.gwt.user.client.rpc.SerializableException;
import edu.ucsb.eucalyptus.admin.client.ClusterInfoWeb;
import edu.ucsb.eucalyptus.admin.client.StorageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.VmTypeWeb;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.cluster.Clusters;
import edu.ucsb.eucalyptus.cloud.cluster.VmTypes;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.ClusterStateType;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import com.eucalyptus.ws.util.Messaging;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RemoteInfoHandler {

  private static Logger LOG = Logger.getLogger( RemoteInfoHandler.class );

  public static synchronized void setClusterList( List<ClusterInfoWeb> newClusterList )
  {
    List<ClusterStateType> list = new ArrayList<ClusterStateType>();
    for ( ClusterInfoWeb cw : newClusterList ) {
      LOG.info( "Adding cluster for update: " + cw.getName() + " - " + cw.getHost() + ":" + cw.getPort() );
      list.add( new ClusterStateType( cw.getName(), cw.getHost(), cw.getPort() ) );
    }
    Messaging.dispatch( EucalyptusProperties.CLUSTERSINK_REF, list );
  }

  public static synchronized List<ClusterInfoWeb> getClusterList()
  {
    List<ClusterInfoWeb> clusterList = new ArrayList<ClusterInfoWeb>();
    for ( ClusterStateType c : Clusters.getInstance().getClusters() )
      clusterList.add( new ClusterInfoWeb( c.getName(), c.getHost(), c.getPort()) ); 
    return clusterList;
  }

  public static synchronized void setStorageList( List<StorageInfoWeb> newStorageList )
  {
	  //TODO: Chris do messaging stuff
  }

  public static synchronized List<StorageInfoWeb> getStorageList()
  {
    List<StorageInfoWeb> storageList = new ArrayList<StorageInfoWeb>();
	  //TODO: Chris iterate over StorageStateType and construct storageList
    return storageList;
  }

  public static List<VmTypeWeb> getVmTypes()
  {
    List<VmTypeWeb> ret = new ArrayList<VmTypeWeb>();
    for( VmType v : VmTypes.list() )
      ret.add( new VmTypeWeb( v.getName(), v.getCpu(), v.getMemory(), v.getDisk() ) );
    return ret;
  }

  public static void setVmTypes( final List<VmTypeWeb> vmTypes ) throws SerializableException {
    Set<VmType> newVms = Sets.newTreeSet();
    for ( VmTypeWeb vmw : vmTypes ) {
      newVms.add( new VmType( vmw.getName(), vmw.getCpu(), vmw.getDisk(), vmw.getMemory() ) );
    }
    try {
      VmTypes.update( newVms );
    }
    catch ( EucalyptusCloudException e ) {
      throw new SerializableException( e.getMessage() );
    }
  }
}

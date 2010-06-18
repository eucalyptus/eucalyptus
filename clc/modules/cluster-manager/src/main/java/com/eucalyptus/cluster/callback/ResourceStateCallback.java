package com.eucalyptus.cluster.callback;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmTypes;
import com.eucalyptus.entities.VmType;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesType;

public class ResourceStateCallback extends QueuedEventCallback<DescribeResourcesType,DescribeResourcesResponseType> {
  private static DescribeResourcesType getMessage() {
    DescribeResourcesType drMsg = new DescribeResourcesType( ).regarding( );
    for ( VmType v : VmTypes.list( ) ) {
      drMsg.getInstanceTypes( ).add( v.getAsVmTypeInfo( ) );
    }
    return drMsg;
  }
  private Cluster cluster;
  public ResourceStateCallback( Cluster cluster ) {
    this.setRequest( ResourceStateCallback.getMessage() );
  }

  @Override
  public void verify( DescribeResourcesResponseType reply ) throws Exception {
    if ( reply.get_return( ) ) {
      cluster.getNodeState( ).update( reply.getResources( ) );
      LOG.debug( "Adding node service tags: " + reply.getServiceTags( ) );
      cluster.updateNodeInfo( reply.getServiceTags( ) );
    }
  }

}

package com.eucalyptus.cluster.callback;

import org.apache.log4j.Logger;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmTypes;
import com.eucalyptus.entities.VmType;
import com.eucalyptus.util.async.FailedRequestException;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesType;

public class ResourceStateCallback extends StateUpdateMessageCallback<Cluster, DescribeResourcesType, DescribeResourcesResponseType> {
  private static Logger LOG = Logger.getLogger( ResourceStateCallback.class );
  
  public ResourceStateCallback( ) {
    this.setRequest( new DescribeResourcesType( ) {
      {
        regarding( );
        for ( VmType v : VmTypes.list( ) ) {
          getInstanceTypes( ).add( v.getAsVmTypeInfo( ) );
        }
      }
    } );
  }
  
  /**
   * @see com.eucalyptus.util.async.MessageCallback#fire(edu.ucsb.eucalyptus.msgs.BaseMessage)
   * @param reply
   */
  @Override
  public void fire( DescribeResourcesResponseType reply ) {
    this.getSubject( ).getNodeState( ).update( reply.getResources( ) );
    LOG.debug( "Adding node service tags: " + reply.getServiceTags( ) );
    if( !reply.getNodes( ).isEmpty( ) ) {
      this.getSubject( ).updateNodeInfo( reply.getNodes( ) );
    } else {
      this.getSubject( ).updateNodeInfo( reply.getServiceTags( ) );
    }
  }
  
  /**
   * @see com.eucalyptus.cluster.callback.StateUpdateMessageCallback#fireException(com.eucalyptus.util.async.FailedRequestException)
   * @param t
   */
  @Override
  public void fireException( FailedRequestException t ) {
    LOG.debug( "Request to " + this.getSubject( ).getName( ) + " failed: " + t.getMessage( ) );
  }

}

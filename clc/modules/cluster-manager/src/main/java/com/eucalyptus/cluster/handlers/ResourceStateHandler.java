package com.eucalyptus.cluster.handlers;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.VmTypes;
import com.eucalyptus.entities.VmType;
import com.eucalyptus.event.Event;
import com.eucalyptus.http.MappingHttpResponse;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesType;

@ChannelPipelineCoverage( "one" )
public class ResourceStateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NetworkStateHandler.class );
  
  public ResourceStateHandler( Cluster cluster ) throws BindingException {
    super( cluster );
  }
  
  @Override
  public void trigger( ) {
    DescribeResourcesType drMsg = new DescribeResourcesType( ).regarding( );
    for ( VmType v : VmTypes.list( ) ) {
      drMsg.getInstanceTypes( ).add( v.getAsVmTypeInfo( ) );
    }
    this.write( drMsg );
  }
  
  @Override
  public void fireEvent( Event event ) {
    super.fireTimedStatefulTrigger( event );
  }
  
  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    if ( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse resp = ( MappingHttpResponse ) e.getMessage( );
      DescribeResourcesResponseType reply = ( DescribeResourcesResponseType ) resp.getMessage( );
      if ( reply.get_return( ) ) {
        this.getCluster( ).getNodeState( ).update( reply.getResources( ) );
        LOG.debug( "Adding node service tags: " + reply.getServiceTags( ) );
        this.getCluster( ).updateNodeInfo( reply.getServiceTags( ) );
      }
      this.verified = true;
    }
  }
  
  @Override
  public void advertiseEvent( Event event ) {}
  
}
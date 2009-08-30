package com.eucalyptus.cluster.handlers;

import java.nio.channels.AlreadyConnectedException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.event.Event;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpResponse;

import edu.ucsb.eucalyptus.cloud.cluster.VmTypes;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeResourcesType;

@ChannelPipelineCoverage("one")
public class ResourceStateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NetworkStateHandler.class );
  public ResourceStateHandler( Cluster cluster ) throws BindingException {
    super( cluster );
  }

  @Override
  public void trigger( ) {
    DescribeResourcesType drMsg = new DescribeResourcesType();
    drMsg.setUserId( Component.eucalyptus.name() );
    drMsg.setEffectiveUserId( Component.eucalyptus.name() );
    for ( VmType v : VmTypes.list() ) drMsg.getInstanceTypes().add( v.getAsVmTypeInfo() );
    this.write( drMsg );
  }

  @Override
  public void fireEvent( Event event ) {
    super.fireTimedStatefulTrigger( event );
  }


  @Override
  public void downstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    ctx.sendDownstream( e );
  }

  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    if( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse resp = (MappingHttpResponse) e.getMessage( );
      DescribeResourcesResponseType reply = (DescribeResourcesResponseType) resp.getMessage( );   
      if ( reply.get_return( ) ) {
        this.getCluster( ).getNodeState( ).update( reply.getResources( ) );
        LOG.debug("Adding node service tags: " + reply.getServiceTags() );
        this.getCluster( ).updateNodeInfo( reply.getServiceTags() );
      }
      this.verified = true;
      ctx.getChannel( ).close( );
    }
  }

  @Override
  public void advertiseEvent( Event event ) {}

  @Override
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    if( e.getCause( ) instanceof AlreadyConnectedException ) {
    } else {
      this.exceptionCaught( e.getCause( ) );
    }
  }

  @Override
  public void exceptionCaught( Throwable cause ) {
    LOG.warn( cause, cause );
    
  }
}
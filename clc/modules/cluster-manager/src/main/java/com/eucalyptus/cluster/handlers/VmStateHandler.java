package com.eucalyptus.cluster.handlers;

import java.nio.channels.AlreadyConnectedException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.GenericEvent;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpResponse;

import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.cluster.VmTypes;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.cloud.ws.SystemState;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

@ChannelPipelineCoverage("one")
public class VmStateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG = Logger.getLogger( NetworkStateHandler.class );

  public VmStateHandler( Cluster cluster ) throws BindingException {
    super( cluster );
  }

  @Override
  public void trigger( ) {

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
    if ( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse resp = ( MappingHttpResponse ) e.getMessage( );
      VmDescribeResponseType reply = (VmDescribeResponseType) resp.getMessage( );
      if ( reply != null ) reply.setOriginCluster( this.getCluster( ).getConfiguration( ).getName( ) );
      for ( VmInfo vmInfo : reply.getVms( ) ) {
        vmInfo.setPlacement( this.getCluster( ).getConfiguration( ).getName( ) );
        VmTypeInfo typeInfo = vmInfo.getInstanceType( );
        if ( typeInfo.getName( ) == null || "".equals( typeInfo.getName( ) ) ) {
          for ( VmType t : VmTypes.list( ) ) {
            if ( t.getCpu( ).equals( typeInfo.getCores( ) ) && t.getDisk( ).equals( typeInfo.getDisk( ) ) && t.getMemory( ).equals( typeInfo.getMemory( ) ) ) {
              typeInfo.setName( t.getName( ) );
            }
          }
        }
      }
      SystemState.handle( reply );
      this.verified = true;
    }
    ctx.getChannel( ).close( );
  }

  @Override
  public void advertiseEvent( Event event ) {
  }

  @Override
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    if ( e.getCause( ) instanceof AlreadyConnectedException ) {
    } else {
      this.exceptionCaught( e.getCause( ) );
    }
  }

  @Override
  public void exceptionCaught( Throwable cause ) {
    LOG.info( cause, cause );
  }
}
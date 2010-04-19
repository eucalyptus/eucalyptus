package com.eucalyptus.cluster.handlers;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.util.ClusterUtil;
import com.eucalyptus.event.Event;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.MappingHttpResponse;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
import edu.ucsb.eucalyptus.cloud.cluster.VmTypes;
import edu.ucsb.eucalyptus.cloud.entities.VmType;
import edu.ucsb.eucalyptus.cloud.ws.SystemState;
import edu.ucsb.eucalyptus.msgs.VmTypeInfo;

@ChannelPipelineCoverage( "one" )
public class VmStateHandler extends AbstractClusterMessageDispatcher {
  private static Logger LOG  = Logger.getLogger( NetworkStateHandler.class );
  private AtomicInteger init = new AtomicInteger( 0 );
  
  public VmStateHandler( Cluster cluster ) throws BindingException {
    super( cluster );
  }
  
  @Override
  public void trigger( ) {
    VmDescribeType msg = new VmDescribeType( );
    msg.setUserId( Component.eucalyptus.name( ) );
    msg.setEffectiveUserId( Component.eucalyptus.name( ) );
    this.write( msg );
  }
  
  @Override
  public void fireEvent( Event event ) {
    super.fireTimedStatefulTrigger( event );
  }
  
  @Override
  public void upstreamMessage( ChannelHandlerContext ctx, MessageEvent e ) {
    if ( e.getMessage( ) instanceof MappingHttpResponse ) {
      MappingHttpResponse resp = ( MappingHttpResponse ) e.getMessage( );
      VmDescribeResponseType reply = ( VmDescribeResponseType ) resp.getMessage( );
      if ( reply != null ) {
        reply.setOriginCluster( this.getCluster( ).getConfiguration( ).getName( ) );
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
        if( this.init.addAndGet( 1 ) == 2 ) {
          try {
            ClusterUtil.registerClusterStateHandler( this.getCluster( ), new AddressStateHandler( this.getCluster( ) ) );
          } catch ( Exception e1 ) {
            LOG.error( e1, e1 );
          }
          this.getCluster( ).start( );
          LOG.info( LogUtil.header( "Starting threads for cluster: " + this.getCluster( ) ) );   
        }
      }      
    }
  }
  
  @Override
  public void advertiseEvent( Event event ) {}
}

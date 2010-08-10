package com.eucalyptus.cluster.handlers;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.VmTypes;
import com.eucalyptus.entities.VmType;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.vm.SystemState;
import edu.ucsb.eucalyptus.cloud.VmDescribeResponseType;
import edu.ucsb.eucalyptus.cloud.VmDescribeType;
import edu.ucsb.eucalyptus.cloud.VmInfo;
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
            Clusters.registerClusterStateHandler( this.getCluster( ), new AddressStateHandler( this.getCluster( ) ) );
            ListenerRegistry.getInstance( ).fireEvent( new ClockTick().setMessage( 1l ) );
          } catch ( Exception e1 ) {
            LOG.error( e1, e1 );
          }
          this.getCluster( ).start( );
          LOG.info( LogUtil.header( "Starting threads for cluster: " + this.getCluster( ) ) );   
        } else if( this.init.get( ) < 2 ) {
          try {
            ListenerRegistry.getInstance( ).fireEvent( new ClockTick().setMessage( 1l ) );
          } catch ( EventVetoedException e1 ) {
            LOG.debug( e1, e1 );
          }          
        }
      }      
    }
  }
  
  @Override
  public void advertiseEvent( Event event ) {}
}

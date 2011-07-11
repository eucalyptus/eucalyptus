package com.eucalyptus.component.id;

import java.util.ArrayList;
import java.util.List;
import org.jboss.netty.channel.ChannelPipelineFactory;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.util.Internets;

public class ClusterController extends ComponentId {
  
  public ClusterController( ) {
    super( "cluster" );
  }
  
  @Override
  public Integer getPort( ) {
    return 8774;
  }
  
  @Override
  public String getLocalEndpointName( ) {
    return String.format( getUriPattern( ), Internets.localHostAddress( ), this.getPort( ) );
  }
  
  @Override
  public String getUriPattern( ) {
    return "http://%s:%d/axis2/services/EucalyptusCC";
  }
  
  @Override
  public String getExternalUriPattern( ) {
    return "http://%s:%d/axis2/services/EucalyptusCC";
  }
  
  private static ChannelPipelineFactory clusterPipeline;
  
  @Override
  public ChannelPipelineFactory getClientPipeline( ) {//TODO:GRZE:fixme to use discovery
    return ( clusterPipeline = ( clusterPipeline != null
        ? clusterPipeline
          : helpGetClientPipeline( "com.eucalyptus.ws.client.pipeline.ClusterClientPipelineFactory" ) ) );
  }
  
  @Override
  public List<Class<? extends ComponentId>> serviceDependencies( ) {
    return new ArrayList( ) {
      {
        add( Eucalyptus.class );
      }
    };
  }
  
}

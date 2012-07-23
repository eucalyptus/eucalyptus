/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.component.id;

import java.util.ArrayList;
import java.util.List;
import org.jboss.netty.channel.ChannelPipelineFactory;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.Partition;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.util.Internets;

@Partition( value = { Eucalyptus.class } )
public class ClusterController extends ComponentId {
  
  private static final long       serialVersionUID = 1L;
  public static final ComponentId INSTANCE         = new ClusterController( );
  
  public ClusterController( ) {
    super( "cluster" );
  }
  
  @Override
  public Integer getPort( ) {
    return 8774;
  }
  
  @Override
  public String getLocalEndpointName( ) {
    return ServiceUris.remote( this, Internets.localHostInetAddress( ), this.getPort( ) ).toASCIIString( );
  }
  
  private static ChannelPipelineFactory clusterPipeline;
  
  @Override
  public ChannelPipelineFactory getClientPipeline( ) {//TODO:GRZE:fixme to use discovery
    ChannelPipelineFactory factory = null;
    if ( ( factory = super.getClientPipeline( ) ) == null ) {
      factory = ( clusterPipeline = ( clusterPipeline != null
        ? clusterPipeline
          : helpGetClientPipeline( "com.eucalyptus.ws.client.pipeline.ClusterClientPipelineFactory" ) ) );
    }
    return factory;
  }
  
  @Override
  public String getServicePath( final String... pathParts ) {
    return "/axis2/services/EucalyptusCC";
  }
  
  @Override
  public String getInternalServicePath( final String... pathParts ) {
    return this.getServicePath( pathParts );
  }
  
  @Partition( value = { ClusterController.class }, manyToOne = true )
  @InternalService
  public static class NodeController extends ComponentId {
    
    public NodeController( ) {
      super( "node" );
    }
    
    @Override
    public Integer getPort( ) {
      return 8775;
    }
    
    @Override
    public String getLocalEndpointName( ) {
      return ServiceUris.remote( this, Internets.localHostInetAddress( ), this.getPort( ) ).toASCIIString( );
    }
    
    @Override
    public String getServicePath( final String... pathParts ) {
      return "/axis2/services/EucalyptusNC";
    }
    
    @Override
    public String getInternalServicePath( final String... pathParts ) {
      return this.getServicePath( pathParts );
    }
    
  }
  
  @Partition( ClusterController.class )
  @InternalService
  public static class GatherLogService extends ComponentId {
    
    private static final long serialVersionUID = 1L;
    
    public GatherLogService( ) {
      super( "gatherlog" );
    }
    
    @Override
    public Integer getPort( ) {
      return 8774;
    }
    
    @Override
    public String getLocalEndpointName( ) {
      return ServiceUris.remote( this, Internets.localHostInetAddress( ), this.getPort( ) ).toASCIIString( );
    }
    
    @Override
    public String getServicePath( final String... pathParts ) {
      return "/axis2/services/EucalyptusGL";
    }
    
    @Override
    public String getInternalServicePath( final String... pathParts ) {
      return this.getServicePath( pathParts );
    }
    
    private static ChannelPipelineFactory logPipeline;
    
    /**
     * This was born under a bad sign. No touching.
     * 
     * @return
     */
    @Override
    public ChannelPipelineFactory getClientPipeline( ) {
      ChannelPipelineFactory factory = null;
      if ( ( factory = super.getClientPipeline( ) ) == null ) {
        factory = ( logPipeline = ( logPipeline != null
          ? logPipeline
          : helpGetClientPipeline( "com.eucalyptus.ws.client.pipeline.GatherLogClientPipeline" ) ) );
      }
      return factory;
    }
    
  }
}

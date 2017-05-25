/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cluster.service.node;

import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.cluster.common.Cluster;
import com.eucalyptus.cluster.common.ClusterRegistry;
import com.eucalyptus.cluster.common.msgs.CloudNodeMessage;
import com.eucalyptus.cluster.proxy.node.ProxyNodeController;
import com.eucalyptus.cluster.service.NodeService;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceBuilders;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.async.AsyncProxy;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import javaslang.collection.Stream;

/**
 *
 */
public class DefaultClusterNodeServiceFactory implements ClusterNodeServiceFactory {

  @Override
  public NodeService nodeService( final ClusterNode node, final int port ) {
    final Cluster cluster = localCluster( );
    final ServiceBuilder serviceBuilder = ServiceBuilders.lookup( ProxyNodeController.class );
    final ServiceConfiguration configuration =
        serviceBuilder.newInstance( cluster.getPartition( ), node.getNode( ), node.getNode( ), port );
    return AsyncProxy.client( NodeService.class, ( BaseMessage message) -> {
      Topology.populateServices( configuration, message, true );
      message.set_return( null );
      message.setUserId( Principals.systemUser( ).getUserId( ) );
      if ( message instanceof CloudNodeMessage ) {
        ((CloudNodeMessage)message).setNodeName( node.getNode( ) );
      }
      return message;
    }, configuration );
  }

  private static Cluster localCluster( ) {
    return Stream.ofAll( ClusterRegistry.getInstance( ).listValues( ) )
        .appendAll( ClusterRegistry.getInstance( ).listDisabledValues( ) )
        .find( cluster -> cluster.getConfiguration( ).isHostLocal( ) )
        .getOrElseThrow( ( ) -> new IllegalStateException( "No local cluster" ) );
  }

}

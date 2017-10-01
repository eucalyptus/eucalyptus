/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
import io.vavr.collection.Stream;

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
      if ( message.getUserId( ) == null ) {
        message.setUserId( Principals.systemUser( ).getUserId( ) );
      }
      if ( message instanceof CloudNodeMessage ) {
        ((CloudNodeMessage)message).setNodeName( node.getNode( ) );
      }
      return message;
    }, configuration );
  }

  private static Cluster localCluster( ) {
    return ClusterRegistry.getLocalCluster( false )
        .getOrElseThrow( ( ) -> new IllegalStateException( "No local cluster" ) );
  }

}

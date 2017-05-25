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
package com.eucalyptus.cluster.service.fake;

import java.util.concurrent.ConcurrentMap;
import com.eucalyptus.cluster.service.NodeService;
import com.eucalyptus.cluster.service.node.ClusterNode;
import com.eucalyptus.cluster.service.node.ClusterNodeServiceFactory;
import com.google.common.collect.Maps;

/**
 *
 */
public class FakeClusterNodeServiceFactory implements ClusterNodeServiceFactory {

  private final ConcurrentMap<String,NodeService> nodeServiceMap = Maps.newConcurrentMap( );
  private final boolean allowReload;

  public FakeClusterNodeServiceFactory( ) {
    this( true );
  }

  public FakeClusterNodeServiceFactory( final boolean allowReload ) {
    this.allowReload = allowReload;
  }


  @Override
  public NodeService nodeService( final ClusterNode node, final int port ) {
    return nodeServiceMap.computeIfAbsent( node.getNode( ), __ -> new FakeNodeService( node, allowReload ) );
  }
}

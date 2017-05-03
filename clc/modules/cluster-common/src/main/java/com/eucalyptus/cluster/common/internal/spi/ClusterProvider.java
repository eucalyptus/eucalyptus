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
package com.eucalyptus.cluster.common.internal.spi;

import java.util.List;
import com.eucalyptus.cluster.common.internal.Cluster;
import com.eucalyptus.cluster.common.msgs.NodeType;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;

/**
 * ClusterProvider is an internal API for providing cluster functionality.
 *
 * @see Cluster for the public API
 */
public interface ClusterProvider {
  String getName( );
  String getPartition( );
  String getHostName( );
  Partition lookupPartition( );
  ServiceConfiguration getConfiguration( );
  void init( Cluster cluster );
  void refreshResources( );
  void check( );
  void start( ) throws ServiceRegistrationException;
  void stop( ) throws ServiceRegistrationException;
  void enable( ) throws ServiceRegistrationException;
  void disable( ) throws ServiceRegistrationException;
  void updateNodeInfo( List<NodeType> nodes );
  boolean hasNode( String sourceHost );
  void cleanup( Cluster cluster, Exception ex );
}

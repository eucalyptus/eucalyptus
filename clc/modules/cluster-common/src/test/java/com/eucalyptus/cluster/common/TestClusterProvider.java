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
package com.eucalyptus.cluster.common;

import java.util.List;
import com.eucalyptus.cluster.common.internal.Cluster;
import com.eucalyptus.cluster.common.internal.spi.ClusterProvider;
import com.eucalyptus.cluster.common.msgs.NodeType;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;

/**
 *
 */
public class TestClusterProvider implements ClusterProvider {

  private String name;
  private String partition;
  private String hostname;

  public TestClusterProvider( ) {
  }

  @Override
  public String getName( ) {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  @Override
  public String getPartition( ) {
    return partition;
  }

  public void setPartition( final String partition ) {
    this.partition = partition;
  }

  public String getHostName( ) {
    return hostname;
  }

  public void setHostName( final String hostname ) {
    this.hostname = hostname;
  }

  @Override
  public Partition lookupPartition( ) {
    return null;
  }

  @Override
  public void init( final Cluster cluster ) {
  }

  @Override
  public ServiceConfiguration getConfiguration() {
    return null;
  }

  @Override
  public void refreshResources( ) {
  }

  @Override
  public void check( ) {
  }

  @Override
  public void start( ) throws ServiceRegistrationException {
  }

  @Override
  public void stop( ) throws ServiceRegistrationException {
  }

  @Override
  public void enable( ) throws ServiceRegistrationException {
  }

  @Override
  public void disable( ) throws ServiceRegistrationException {
  }

  @Override
  public void updateNodeInfo( final List<NodeType> nodes ) {
  }

  @Override
  public boolean hasNode( final String sourceHost ) {
    return false;
  }

  @Override
  public void cleanup( final Cluster cluster, final Exception ex ) {
  }
}

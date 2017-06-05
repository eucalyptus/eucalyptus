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
package com.eucalyptus.cluster.common;

import java.util.List;
import com.eucalyptus.cluster.common.provider.ClusterProvider;
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
  public void updateNodeInfo( final long time, final List<NodeType> nodes ) {
  }

  @Override
  public boolean hasNode( final String sourceHost ) {
    return false;
  }

  @Override
  public void cleanup( final Cluster cluster, final Exception ex ) {
  }
}

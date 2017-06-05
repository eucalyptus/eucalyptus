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
package com.eucalyptus.cluster.common.provider;

import java.util.List;
import com.eucalyptus.cluster.common.Cluster;
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
  void updateNodeInfo( long time, List<NodeType> nodes );
  boolean hasNode( String sourceHost );
  void cleanup( Cluster cluster, Exception ex );
}

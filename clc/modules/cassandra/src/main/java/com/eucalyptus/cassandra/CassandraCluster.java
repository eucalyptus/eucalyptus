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
package com.eucalyptus.cassandra;

import java.util.Set;
import java.util.stream.Collectors;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.bootstrap.SystemIds;
import com.eucalyptus.cassandra.common.Cassandra;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 *
 */
public class CassandraCluster {

  /**
   * We can start if we are the first seed, or if the first seed is already up.
   */
  public static boolean canStart( ) {
    boolean canStart = false;
    if ( Hosts.hasCoordinator( ) && !Databases.isVolatile( ) ) {
      final Set<ServiceConfiguration> configurations = seedOrderedConfigurations( );
      if ( !configurations.isEmpty( ) ) {
        final ServiceConfiguration configuration = Iterables.get( configurations, 0, null );
        if ( configuration != null && configuration.isHostLocal( ) ) {
          canStart = true;
        } else if ( configuration != null &&
            Component.State.ENABLED.apply( configuration ) &&
            Topology.isEnabled( Cassandra.class ) ) {
          canStart = true; //TODO:STEVE: should be one node at a time?
        }
      }
    }
    return canStart;
  }

  /**
   * Get the name for the eucalyptus cassandra cluster datacenter.
   */
  public static String datacenter( ) {
    return "eucalyptus";
  }

  /**
   * Get the name for the eucalyptus cassandra cluster rack.
   */
  public static String rack( ) {
    return Components.lookup( Cassandra.class ).getLocalServiceConfiguration( ).getPartition( );
  }

  /**
   * Get the name for the eucalyptus cassandra cluster.
   */
  public static String name( ) {
    return SystemIds.createShortCloudUniqueName( "cassandra" );
  }

  /**
   * Should a local cassandra node auto bootstrap when starting
   */
  public static boolean autoBootstrap( ) {
    // auto bootstrap unless we are the first up in the cluster and have no data
    return Topology.isEnabled( Cassandra.class ) || !CassandraDirectory.DATA.isEmpty( );
  }

  /**
   * Get the ordered seed list
   */
  public static Set<String> seeds( ) {
    return seedOrderedConfigurations( ).stream( )
        .map( ServiceConfiguration::getHostName )
        .collect( Collectors.toCollection( Sets::newLinkedHashSet ) );
  }

  /**
   * Get the ordered seed list
   */
  private static Set<ServiceConfiguration> seedOrderedConfigurations( ) {
    return Cassandra.sortedServiceConfigurations( );
  }
}

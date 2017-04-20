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

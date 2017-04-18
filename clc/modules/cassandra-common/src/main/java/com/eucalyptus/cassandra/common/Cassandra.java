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
package com.eucalyptus.cassandra.common;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.Partition;
import com.google.common.collect.Sets;

/**
 * Component identifier class for Cassandra
 */
@Partition( value = Cassandra.class, manyToOne = true )
@FaultLogPrefix( "cloud" )
@Description( "Eucalyptus Cassandra service" )
public class Cassandra extends ComponentId {
  private static final long serialVersionUID = 1L;

  @Override
  public Integer getPort( ) {
    return 8787;
  }

  public static Set<ServiceConfiguration> sortedServiceConfigurations( ) {
    final Set<ServiceConfiguration> sortedConfigurations = Sets.newLinkedHashSet( );
    final List<ServiceConfiguration> services = ServiceConfigurations.list( Cassandra.class );
    for ( final ServiceConfiguration configuration : services ) {
      if ( Hosts.isCoordinator( configuration.getInetAddress( ) ) ) {
        sortedConfigurations.add( configuration );
      }
    }
    sortedConfigurations.addAll( services.stream( )
        .sorted( )
        .collect( Collectors.toList( ) ) );
    return sortedConfigurations;
  }
}


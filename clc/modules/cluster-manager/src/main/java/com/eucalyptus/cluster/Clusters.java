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
package com.eucalyptus.cluster;

import java.util.List;
import java.util.NoSuchElementException;
import com.eucalyptus.cluster.common.internal.Cluster;
import com.eucalyptus.cluster.common.internal.ClusterRegistry;
import com.eucalyptus.component.ServiceConfiguration;
import javaslang.collection.Stream;

/**
 *
 */
public class Clusters {

  public static List<Cluster> list( ) {
    return registry( ).listValues( );
  }

  public static Stream<Cluster> stream( ) {
    return Stream.ofAll( list( ) );
  }

  public static List<Cluster> listDisabled( ) {
    return registry( ).listDisabledValues( );
  }

  public static Stream<Cluster> streamDisabled( ) {
    return Stream.ofAll( listDisabled( ) );
  }

  public static Cluster lookupAny( final ServiceConfiguration clusterConfig ) {
    return lookupAny( clusterConfig.getName( ) );
  }

  public static Cluster lookup( final String name ) {
    return registry( ).lookup( name );
  }

  public static Cluster lookupAny( final String name ) {
    try {
      return registry( ).lookup( name );
    } catch ( final NoSuchElementException ex ) {
      return registry( ).lookupDisabled( name );
    }
  }

  private static ClusterRegistry registry( ) {
    return ClusterRegistry.getInstance( );
  }
}

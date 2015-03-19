/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.euare.identity.region;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.util.Parameters;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 *
 */
public class RegionInfo {
  private final String name;
  private final Set<Integer> partitions;
  private final Set<RegionService> services;

  public RegionInfo(
      final String name,
      final Collection<Integer> partitions,
      final Collection<RegionService> services
  ) {
    Parameters.checkParam( "name", name, not( isEmptyOrNullString( ) ) );
    Parameters.checkParam( "partitions", partitions, hasSize( greaterThan( 0 ) ) );
    Parameters.checkParam( "services", services, hasSize( greaterThan( 0 ) ) );
    this.name = name;
    this.partitions = ImmutableSet.copyOf( Sets.newTreeSet( partitions ) );
    this.services = ImmutableSet.copyOf( Sets.newTreeSet( services ) );
  }

  public String getName( ) {
    return name;
  }

  public Set<Integer> getPartitions( ) {
    return partitions;
  }

  public Set<RegionService> getServices( ) {
    return services;
  }

  public static class RegionService implements Comparable<RegionService> {
    private final String type;
    private final Set<String> endpoints;

    public RegionService(
        final String type,
        final Collection<String> endpoints
    ) {
      Parameters.checkParam( "type", type, not( isEmptyOrNullString( ) ) );
      Parameters.checkParam( "endpoints", endpoints, hasSize( greaterThan( 0 ) ) );
      this.type = type;
      this.endpoints = ImmutableSet.copyOf( Sets.newTreeSet( endpoints ) );
    }

    public String getType( ) {
      return type;
    }

    public Set<String> getEndpoints( ) {
      return endpoints;
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final RegionService that = (RegionService) o;

      return endpoints.equals( that.endpoints ) && type.equals( that.type );
    }

    @Override
    public int hashCode() {
      int result = type.hashCode();
      result = 31 * result + endpoints.hashCode();
      return result;
    }

    @Override
    public int compareTo( @Nonnull final RegionService regionService ) {
      return type.compareTo( regionService.type );
    }
  }
}

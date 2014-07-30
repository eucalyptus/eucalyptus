/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.vpc;

import static com.eucalyptus.compute.common.CloudMetadata.RouteTableMetadata;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.RouteTableAssociationType;
import edu.ucsb.eucalyptus.msgs.RouteTableType;
import edu.ucsb.eucalyptus.msgs.RouteType;

/**
 *
 */
public interface RouteTables extends Lister<RouteTable> {

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String, String> aliases,
                    Predicate<? super RouteTable> filter,
                    Function<? super RouteTable, T> transform ) throws VpcMetadataException;

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super RouteTable, T> transform ) throws VpcMetadataException;

  <T> T lookupMain( String vpcId,
                    Function<? super RouteTable,T> transform ) throws VpcMetadataException;

  boolean delete( final RouteTableMetadata metadata ) throws VpcMetadataException;

  RouteTable save( RouteTable networkAcl ) throws VpcMetadataException;

  RouteTable updateByExample( RouteTable example,
                              OwnerFullName ownerFullName,
                              String key,
                              Callback<RouteTable> updateCallback ) throws VpcMetadataException;

  @TypeMapper
  public enum RouteTableToRouteTableTypeTransform implements Function<RouteTable, RouteTableType> {
    INSTANCE;

    @Nullable
    @Override
    public RouteTableType apply( @Nullable final RouteTable routeTable ) {
      return routeTable == null ?
          null :
          new RouteTableType(
              routeTable.getDisplayName( ),
              routeTable.getVpc( ).getDisplayName( ),
              Collections2.transform( routeTable.getRoutes(), RouteToRouteType.INSTANCE ),
              Collections2.transform( routeTable.getSubnets( ), SubnetToRouteTableAssociationType.INSTANCE )
          );
    }
  }

  @TypeMapper
  public enum RouteToRouteType implements Function<Route,RouteType> {
    INSTANCE;

    @Nullable
    @Override
    public RouteType apply( @Nullable final Route route ) {
      return route == null ?
          null :
          new RouteType(
            route.getDestinationCidr( ),
            Optional.fromNullable( route.getInternetGateway( ) ).transform( CloudMetadatas.toDisplayName( ) ).or( "local" ),
            Objects.toString( route.getState( ), null ),
            Objects.toString( route.getOrigin(), null )
          );
    }
  }

  @TypeMapper
  public enum SubnetToRouteTableAssociationType implements Function<Subnet,RouteTableAssociationType> {
    INSTANCE;

    @Nullable
    @Override
    public RouteTableAssociationType apply( @Nullable final Subnet subnet ) {
      return subnet == null ?
          null :
          new RouteTableAssociationType(
              subnet.getRouteTableAssociationId(),
              subnet.getRouteTable().getDisplayName( ),
              subnet.getDisplayName( ),
              subnet.getRouteTable().getMain()
          );
    }
  }

  public static class RouteTableFilterSupport extends FilterSupport<RouteTable> {
    public RouteTableFilterSupport() {
      super( builderFor( RouteTable.class )
              .withTagFiltering( RouteTableTag.class, "routeTable" )
              .withStringSetProperty( "association.route-table-association-id", FilterStringSetFunctions.ASSOCIATION_ID )
              .withStringProperty( "association.route-table-id", FilterStringFunctions.ASSOCIATION_ROUTE_TABLE_ID )
              .withStringSetProperty( "association.subnet-id", FilterStringSetFunctions.ASSOCIATION_SUBNET_ID )
              .withBooleanProperty( "association.main", FilterBooleanFunctions.ASSOCIATION_MAIN )
              .withStringSetProperty( "route.destination-cidr-block", FilterStringSetFunctions.ROUTE_DESTINATION_CIDR )
              .withStringSetProperty( "route.gateway-id", FilterStringSetFunctions.ROUTE_GATEWAY_ID )
              .withUnsupportedProperty( "route.instance-id" )
              .withUnsupportedProperty( "route.vpc-peering-connection-id" )
              .withStringSetProperty( "route.origin", FilterStringSetFunctions.ROUTE_ORIGIN )
              .withStringSetProperty( "route.state", FilterStringSetFunctions.ROUTE_STATE )
              .withStringProperty( "route-table-id", CloudMetadatas.toDisplayName() )
              .withStringProperty( "vpc-id", FilterStringFunctions.VPC_ID )
              .withPersistenceAlias( "subnets", "subnets" )
              .withPersistenceAlias( "routes", "routes" )
              .withPersistenceAlias( "vpc", "vpc" )
              .withPersistenceFilter( "association.route-table-association-id", "subnets.routeTableAssociationId" )
              .withPersistenceFilter( "association.subnet-id", "subnets.displayName" )
              .withPersistenceFilter( "route.destination-cidr-block", "routes.destinationCidr" )
              .withPersistenceFilter( "route.gateway-id", "routes.destinationCidr" )
              .withPersistenceFilter( "route.origin", "routes.origin", Enums.valueOfFunction( Route.RouteOrigin.class ) )
              .withPersistenceFilter( "route.state", "routes.state", Enums.valueOfFunction( Route.State.class ) )
              .withPersistenceFilter( "route-table-id", "displayName" )
              .withPersistenceFilter( "vpc-id", "vpc.displayName" )
      );
    }
  }

  public enum FilterStringFunctions implements Function<RouteTable, String> {
    ASSOCIATION_ROUTE_TABLE_ID {
      @Override
      public String apply( final RouteTable routeTable ) {
        return routeTable.getSubnets( ).isEmpty( ) ? null : routeTable.getDisplayName( );
      }
    },
    VPC_ID {
      @Override
      public String apply( final RouteTable routeTable ) {
        return routeTable.getVpc( ).getDisplayName( );
      }
    },
  }

  public enum RouteFilterStringFunctions implements Function<Route,String> {
    DESTINATION_CIDR {
      @Override
      public String apply( final Route route ) {
        return route.getDestinationCidr( );
      }
    },
    GATEWAY_ID {
      @Override
      public String apply( final Route route ) {
        return CloudMetadatas.toDisplayName( ).apply( route.getInternetGateway() );
      }
    },
    ORIGIN {
      @Override
      public String apply( final Route route ) {
        return Objects.toString( route.getOrigin(), null );
      }
    },
    STATE {
      @Override
      public String apply( final Route route ) {
        return Objects.toString( route.getState(), null );
      }
    },
  }

  public enum FilterBooleanFunctions implements Function<RouteTable, Boolean> {
    ASSOCIATION_MAIN {
      @Override
      public Boolean apply( final RouteTable routeTable ) {
        return routeTable.getMain( ) && !routeTable.getSubnets( ).isEmpty( );
      }
    },
  }

  public enum FilterStringSetFunctions implements Function<RouteTable, Set<String>> {
    ASSOCIATION_ID {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return subnetPropertySet( routeTable, Subnets.FilterStringFunctions.ROUTE_TABLE_ASSOCIATION_ID );
      }
    },
    ASSOCIATION_SUBNET_ID {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return subnetPropertySet( routeTable, CloudMetadatas.toDisplayName() );
      }
    },
    ROUTE_DESTINATION_CIDR {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return routePropertySet( routeTable, RouteFilterStringFunctions.DESTINATION_CIDR );
      }
    },
    ROUTE_GATEWAY_ID {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return routePropertySet( routeTable, RouteFilterStringFunctions.GATEWAY_ID );
      }
    },
    ROUTE_ORIGIN {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return routePropertySet( routeTable, RouteFilterStringFunctions.ORIGIN );
      }
    },
    ROUTE_STATE {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return routePropertySet( routeTable, RouteFilterStringFunctions.STATE );
      }
    },
    ;

    private static Set<String> routePropertySet( final RouteTable routeTable,
                                                 final Function<? super Route, String> propertyGetter ) {
      return Sets.newHashSet( Iterables.transform( routeTable.getRoutes( ), propertyGetter ) );
    }

    private static Set<String> subnetPropertySet( final RouteTable routeTable,
                                                  final Function<? super Subnet, String> propertyGetter ) {
      return Sets.newHashSet( Iterables.transform( routeTable.getSubnets(), propertyGetter ) );
    }
  }
}

/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.compute.common.internal.vpc;

import static com.eucalyptus.compute.common.CloudMetadata.RouteTableMetadata;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.RouteTableAssociationType;
import com.eucalyptus.compute.common.RouteTableType;
import com.eucalyptus.compute.common.RouteType;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 *
 */
public interface RouteTables extends Lister<RouteTable> {

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String, String> aliases,
                    Predicate<? super RouteTable> filter,
                    Function<? super RouteTable, T> transform ) throws VpcMetadataException;

  long countByExample( RouteTable example,
                       Criterion criterion,
                       Map<String,String> aliases ) throws VpcMetadataException;

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

  <T> T updateByAssociationId( String associationId,
                               OwnerFullName ownerFullName,
                               Function<RouteTable,T> updateTransform ) throws VpcMetadataException;

  AbstractPersistentSupport<RouteTableMetadata,RouteTable,VpcMetadataException> withRetries( );

  @TypeMapper
  enum RouteTableToRouteTableTypeTransform implements Function<RouteTable, RouteTableType> {
    INSTANCE;

    @Nullable
    @Override
    public RouteTableType apply( @Nullable final RouteTable routeTable ) {
      return routeTable == null ?
          null :
          new RouteTableType(
              routeTable.getDisplayName(),
              routeTable.getVpc().getDisplayName(),
              Collections2.transform( routeTable.getRoutes(), RouteToRouteType.INSTANCE ),
              Collections2.transform( routeTable.getRouteTableAssociations(), RouteTableAssociationToAssociationType.INSTANCE )
          );
    }
  }

  @TypeMapper
  enum RouteToRouteType implements Function<Route,RouteType> {
    INSTANCE;

    @Nullable
    @Override
    public RouteType apply( @Nullable final Route route ) {
      return route == null ?
          null :
          route.getNetworkInterfaceId( ) != null ?
              new RouteType(
                  route.getDestinationCidr( ),
                  Objects.toString( route.getInstanceId( ), null ),
                  Objects.toString( route.getInstanceAccountNumber( ), null ),
                  Objects.toString( route.getNetworkInterfaceId( ) ),
                  Objects.toString( route.getState(), null ),
                  Objects.toString( route.getOrigin(), null )
              ) :
              route.getNatGatewayId( ) != null ?
                  new RouteType(
                      route.getDestinationCidr( ),
                      Objects.toString( route.getState(), null ),
                      Objects.toString( route.getOrigin(), null )
                  ).withNatGatewayId( route.getNatGatewayId( ) ) :
                  new RouteType(
                    route.getDestinationCidr( ),
                    Objects.toString( route.getState(), null ),
                    Objects.toString( route.getOrigin(), null )
                  ).withGatewayId( Optional.fromNullable( route.getInternetGatewayId( ) ).or( "local" ) );
    }
  }

  @TypeMapper
  enum RouteTableAssociationToAssociationType implements Function<RouteTableAssociation,RouteTableAssociationType> {
    INSTANCE;

    @Nullable
    @Override
    public RouteTableAssociationType apply( @Nullable final RouteTableAssociation association ) {
      return association == null ?
          null :
          new RouteTableAssociationType(
              association.getAssociationId( ),
              association.getRouteTableId( ),
              association.getSubnetId( ),
              association.getMain( )
          );
    }
  }

  class RouteTableFilterSupport extends FilterSupport<RouteTable> {
    public RouteTableFilterSupport() {
      super( builderFor( RouteTable.class )
              .withTagFiltering( RouteTableTag.class, "routeTable" )
              .withStringSetProperty( "association.route-table-association-id", FilterStringSetFunctions.ASSOCIATION_ID )
              .withStringSetProperty( "association.route-table-id", FilterStringSetFunctions.ASSOCIATION_ROUTE_TABLE_ID )
              .withStringSetProperty( "association.subnet-id", FilterStringSetFunctions.ASSOCIATION_SUBNET_ID )
              .withBooleanSetProperty( "association.main", FilterBooleanSetFunctions.ASSOCIATION_MAIN )
              .withStringSetProperty( "route.destination-cidr-block", FilterStringSetFunctions.ROUTE_DESTINATION_CIDR )
              .withUnsupportedProperty( "route.destination-prefix-list-id" )
              .withStringSetProperty( "route.gateway-id", FilterStringSetFunctions.ROUTE_GATEWAY_ID )
              .withStringSetProperty( "route.instance-id", FilterStringSetFunctions.ROUTE_INSTANCE_ID )
              .withStringSetProperty( "route.nat-gateway-id", FilterStringSetFunctions.ROUTE_NAT_GATEWAY_ID )
              .withUnsupportedProperty( "route.vpc-peering-connection-id" )
              .withStringSetProperty( "route.origin", FilterStringSetFunctions.ROUTE_ORIGIN )
              .withStringSetProperty( "route.state", FilterStringSetFunctions.ROUTE_STATE )
              .withStringProperty( "route-table-id", CloudMetadatas.toDisplayName() )
              .withStringProperty( "vpc-id", FilterStringFunctions.VPC_ID )
              .withPersistenceAlias( "routeTableAssociations", "routeTableAssociations" )
              .withPersistenceAlias( "routes", "routes" )
              .withPersistenceAlias( "vpc", "vpc" )
              .withPersistenceFilter( "association.route-table-association-id", "routeTableAssociations.associationId" )
              .withPersistenceFilter( "association.route-table-id", "routeTableAssociations.routeTableId" )
              .withPersistenceFilter( "association.subnet-id", "routeTableAssociations.subnetId" )
              .withPersistenceFilter( "association.main", "routeTableAssociations.main", PersistenceFilter.Type.Boolean )
              .withPersistenceFilter( "route.destination-cidr-block", "routes.destinationCidr" )
              .withPersistenceFilter( "route.gateway-id", "routes.internetGatewayId" )
              .withPersistenceFilter( "route.instance-id", "routes.instanceId" )
              .withPersistenceFilter( "route.origin", "routes.origin", FUtils.valueOfFunction( Route.RouteOrigin.class ) )
              .withPersistenceFilter( "route.state", "routes.state", FUtils.valueOfFunction( Route.State.class ) )
              .withPersistenceFilter( "route-table-id", "displayName" )
              .withPersistenceFilter( "vpc-id", "vpc.displayName" )
      );
    }
  }

  enum FilterStringFunctions implements Function<RouteTable, String> {
    VPC_ID {
      @Override
      public String apply( final RouteTable routeTable ) {
        return routeTable.getVpc( ).getDisplayName();
      }
    },
  }

  enum RouteFilterStringFunctions implements Function<Route,String> {
    DESTINATION_CIDR {
      @Override
      public String apply( final Route route ) {
        return route.getDestinationCidr( );
      }
    },
    GATEWAY_ID {
      @Override
      public String apply( final Route route ) {
        return route.getInternetGatewayId( );
      }
    },
    INSTANCE_ID {
      @Override
      public String apply( final Route route ) {
        return route.getInstanceId( );
      }
    },
    NAT_GATEWAY_ID {
      @Override
      public String apply( final Route route ) {
        return route.getNatGatewayId( );
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

  enum FilterStringSetFunctions implements Function<RouteTable, Set<String>> {
    ASSOCIATION_ID {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return associationPropertySet( routeTable, AssociationFilterStringFunctions.ASSOCIATION_ID );
      }
    },
    ASSOCIATION_ROUTE_TABLE_ID {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return associationPropertySet( routeTable, AssociationFilterStringFunctions.ROUTE_TABLE_ID );
      }
    },
    ASSOCIATION_SUBNET_ID {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return associationPropertySet( routeTable, AssociationFilterStringFunctions.SUBNET_ID );
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
    ROUTE_INSTANCE_ID {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return routePropertySet( routeTable, RouteFilterStringFunctions.INSTANCE_ID );
      }
    },
    ROUTE_NAT_GATEWAY_ID {
      @Override
      public Set<String> apply( final RouteTable routeTable ) {
        return routePropertySet( routeTable, RouteFilterStringFunctions.NAT_GATEWAY_ID );
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

    static <T> Set<T> associationPropertySet( final RouteTable routeTable,
                                                      final Function<RouteTableAssociation, T> propertyGetter ) {
      return Sets.newHashSet( Iterables.filter(
          Iterables.transform( routeTable.getRouteTableAssociations( ), propertyGetter ),
          Predicates.notNull( ) ) );
    }
  }

  enum FilterBooleanSetFunctions implements Function<RouteTable, Set<Boolean>> {
    ASSOCIATION_MAIN {
      @Override
      public Set<Boolean> apply( final RouteTable routeTable ) {
        return FilterStringSetFunctions.associationPropertySet( routeTable, AssociationFilterBooleanFunctions.MAIN );
      }
    },
  }

  enum AssociationFilterStringFunctions implements Function<RouteTableAssociation, String> {
    ASSOCIATION_ID {
      @Override
      public String apply( final RouteTableAssociation association ) {
        return association.getAssociationId( );
      }
    },
    ROUTE_TABLE_ID {
      @Override
      public String apply( final RouteTableAssociation association ) {
        return association.getRouteTableId();
      }
    },
    SUBNET_ID {
      @Override
      public String apply( final RouteTableAssociation association ) {
        return association.getSubnetId();
      }
    },
  }

  enum AssociationFilterBooleanFunctions implements Function<RouteTableAssociation, Boolean> {
    MAIN {
      @Override
      public Boolean apply( final RouteTableAssociation association ) {
        return association.getMain();
      }
    },
  }
}

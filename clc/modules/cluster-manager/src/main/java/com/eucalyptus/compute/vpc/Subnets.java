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

import static com.eucalyptus.compute.common.CloudMetadata.SubnetMetadata;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import edu.ucsb.eucalyptus.msgs.SubnetType;

/**
 *
 */
public interface Subnets extends Lister<Subnet> {

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super Subnet> filter,
                    Function<? super Subnet,T> transform ) throws VpcMetadataException;

  <T> List<T> listByExample( Subnet example,
                             Predicate<? super Subnet> filter,
                             Function<? super Subnet,T> transform ) throws VpcMetadataException;

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super Subnet,T> transform ) throws VpcMetadataException;

  boolean delete( final SubnetMetadata metadata ) throws VpcMetadataException;

  Subnet save( Subnet subnet ) throws VpcMetadataException;

  Subnet updateByExample( Subnet example,
                          OwnerFullName ownerFullName,
                          String key,
                          Callback<Subnet> updateCallback ) throws VpcMetadataException;

  @RestrictedTypes.Resolver( Subnet.class )
  public enum Lookup implements Function<String, Subnet> {
    INSTANCE;

    @Override
    public Subnet apply( final String identifier ) {
      try ( final TransactionResource tx = Entities.transactionFor( Subnet.class ) ) {
        return Entities.uniqueResult( Subnet.exampleWithName( null, identifier ) );
      } catch ( TransactionException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }
  }

  @TypeMapper
  public enum SubnetToSubnetTypeTransform implements Function<Subnet,SubnetType> {
    INSTANCE;

    @Nullable
    @Override
    public SubnetType apply( @Nullable final Subnet subnet ) {
      return subnet == null ?
          null :
          new SubnetType(
              subnet.getDisplayName( ),
              Objects.toString( subnet.getState( ), null ),
              CloudMetadatas.toDisplayName().apply( subnet.getVpc( ) ),
              subnet.getCidr( ),
              subnet.getAvailableIpAddressCount( ),
              subnet.getAvailabilityZone( ),
              subnet.getDefaultForAz( ),
              subnet.getMapPublicIpOnLaunch( ) );
    }
  }

  public static class SubnetFilterSupport extends FilterSupport<Subnet> {
    public SubnetFilterSupport( ) {
      super( builderFor( Subnet.class )
          .withTagFiltering( SubnetTag.class, "subnet" )
          .withStringProperty( "availability-zone", FilterStringFunctions.AVAILABILITY_ZONE )
          .withIntegerProperty( "available-ip-address-count", FilterIntegerFunctions.AVAILABLE_IP_COUNT )
          .withStringProperty( "cidr-block", FilterStringFunctions.CIDR ) //TODO:STEVE: filter aliases (check how AWS/EC2 handles use of multiple aliases in a request)
          .withBooleanProperty( "default-for-az", FilterBooleanFunctions.DEFAULT_FOR_AZ )
          .withStringProperty( "state", FilterStringFunctions.STATE )
          .withStringProperty( "subnet-id", CloudMetadatas.toDisplayName( ) )
          .withStringProperty( "vpc-id", FilterStringFunctions.VPC_ID )
          .withPersistenceAlias( "vpc", "vpc" )
          .withPersistenceFilter( "availability-zone", "availabilityZone" )
          .withPersistenceFilter( "available-ip-address-count", "availableIpAddressCount", Collections.<String>emptySet(), PersistenceFilter.Type.Integer )
          .withPersistenceFilter( "cidr-block", "cidr" )
          .withPersistenceFilter( "default-for-az", "defaultForAz", Collections.<String>emptySet(), PersistenceFilter.Type.Boolean )
          .withPersistenceFilter( "state", "state", Enums.valueOfFunction( Subnet.State.class ) )
          .withPersistenceFilter( "subnet-id", "displayName" )
          .withPersistenceFilter( "vpc-id", "vpc.displayName" )
      );
    }
  }

  public enum FilterStringFunctions implements Function<Subnet,String> {
    AVAILABILITY_ZONE {
      @Override
      public String apply( final Subnet subnet ){
        return subnet.getAvailabilityZone();
      }
    },
    CIDR {
      @Override
      public String apply( final Subnet subnet ){
        return subnet.getCidr( );
      }
    },
    NETWORK_ACL_ASSOCIATION_ID {
      @Override
      public String apply( final Subnet subnet ){
        return subnet.getNetworkAclAssociationId( );
      }
    },
    ROUTE_TABLE_ASSOCIATION_ID {
      @Override
      public String apply( final Subnet subnet ){
        return subnet.getRouteTableAssociationId( );
      }
    },
    STATE {
      @Override
      public String apply( final Subnet subnet ){
        return Objects.toString( subnet.getState(), "" );
      }
    },
    VPC_ID {
      @Override
      public String apply( final Subnet subnet ){
        return subnet.getVpc( ).getDisplayName();
      }
    },
  }

  public enum FilterIntegerFunctions implements Function<Subnet,Integer> {
    AVAILABLE_IP_COUNT {
      @Override
      public Integer apply( final Subnet subnet ){
        return subnet.getAvailableIpAddressCount( );
      }
    },
  }

  public enum FilterBooleanFunctions implements Function<Subnet,Boolean> {
    DEFAULT_FOR_AZ {
      @Override
      public Boolean apply( final Subnet subnet ){
        return subnet.getDefaultForAz( );
      }
    },
  }
}

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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.compute.common.CloudMetadata.NatGatewayMetadata;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.NatGatewayAddressSetItemType;
import com.eucalyptus.compute.common.NatGatewayType;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public interface NatGateways extends Lister<NatGateway> {

  long EXPIRY_AGE = TimeUnit.HOURS.toMillis( 1 );

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super  NatGateway> filter,
                    Function<? super  NatGateway,T> transform ) throws VpcMetadataException;

  <T> List<T> listByExample( NatGateway example,
                             Predicate<? super  NatGateway> filter,
                             Function<? super  NatGateway,T> transform ) throws VpcMetadataException;

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super NatGateway,T> transform ) throws VpcMetadataException;

  <T> T lookupByClientToken( OwnerFullName ownerFullName,
                             String clientToken,
                             Function<? super NatGateway,T> transform ) throws VpcMetadataException;

  long countByZone( OwnerFullName ownerFullName, String availabilityZone ) throws VpcMetadataException;

  boolean delete( final NatGatewayMetadata metadata ) throws VpcMetadataException;

  NatGateway save( NatGateway natGateway ) throws VpcMetadataException;

  NatGateway updateByExample( NatGateway example,
                              OwnerFullName ownerFullName,
                              String key,
                              Callback<NatGateway> updateCallback ) throws VpcMetadataException;

  AbstractPersistentSupport<NatGatewayMetadata,NatGateway,VpcMetadataException> withRetries( );

  @TypeMapper
  enum NatGatewayToNatGatewayTypeTransform implements Function<NatGateway,NatGatewayType> {
    INSTANCE;

    @Nullable
    @Override
    public NatGatewayType apply( @Nullable final NatGateway natGateway ) {
      return natGateway == null ?
          null :
          new NatGatewayType(
              natGateway.getDisplayName( ),
              natGateway.getCreationTimestamp( ),
              natGateway.getDeletionTimestamp( ),
              natGateway.getFailureCode( ),
              natGateway.getFailureMessage( ),
              natGateway.getVpcId( ),
              natGateway.getSubnetId( ),
              Objects.toString( natGateway.getState( ), NatGateway.State.failed.toString( ) ),
              new NatGatewayAddressSetItemType(
                  natGateway.getNetworkInterfaceId( ),
                  natGateway.getPrivateIpAddress( ),
                  natGateway.getAllocationId( ),
                  natGateway.getPublicIpAddress( )
              )
          );
    }
  }

  class NatGatewayFilterSupport extends FilterSupport<NatGateway> {
    public NatGatewayFilterSupport( ) {
      super( builderFor( NatGateway.class )
          .withStringProperty( "nat-gateway-id", CloudMetadatas.toDisplayName( ) )
          .withStringProperty( "state", FilterStringFunctions.STATE )
          .withStringProperty( "subnet-id", FilterStringFunctions.SUBNET_ID )
          .withStringProperty( "vpc-id", FilterStringFunctions.VPC_ID )
          .withPersistenceAlias( "subnet", "subnet" )
          .withPersistenceAlias( "vpc", "vpc" )
          .withPersistenceFilter( "nat-gateway-id", "displayName" )
          .withPersistenceFilter( "state", "state", FUtils.valueOfFunction( NatGateway.State.class ) )
          .withPersistenceFilter( "subnet-id", "subnet.displayName" )
          .withPersistenceFilter( "vpc-id", "vpc.displayName" )
      );
    }
  }

   enum FilterStringFunctions implements Function<NatGateway,String> {
    STATE {
      @Override
      public String apply( final NatGateway natGateway ){
        return Objects.toString( natGateway.getState( ), null );
      }
    },
    SUBNET_ID {
      @Override
      public String apply( final NatGateway natGateway ){
        return natGateway.getSubnetId( );
      }
    },
    VPC_ID {
      @Override
      public String apply( final NatGateway natGateway ){
        return natGateway.getVpcId( );
      }
    },
  }
}

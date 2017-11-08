/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

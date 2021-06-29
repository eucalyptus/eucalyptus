/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

import static com.eucalyptus.compute.common.CloudMetadata.SubnetMetadata;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

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

  <T> T lookupDefault( OwnerFullName ownerFullName,
                       String availabilityZone,
                       Function<? super Subnet, T> transform ) throws VpcMetadataException;


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
          .withStringProperty( "availabilityZone", FilterStringFunctions.AVAILABILITY_ZONE )
          .withIntegerProperty( "available-ip-address-count", FilterIntegerFunctions.AVAILABLE_IP_COUNT )
          .withStringProperty( "cidr", FilterStringFunctions.CIDR )
          .withStringProperty( "cidr-block", FilterStringFunctions.CIDR )
          .withStringProperty( "cidrBlock", FilterStringFunctions.CIDR )
          .withBooleanProperty( "default-for-az", FilterBooleanFunctions.DEFAULT_FOR_AZ )
          .withBooleanProperty( "defaultForAz", FilterBooleanFunctions.DEFAULT_FOR_AZ )
          .withStringProperty( "owner-id", FilterStringFunctions.ACCOUNT_ID )
          .withStringProperty( "state", FilterStringFunctions.STATE )
          .withStringProperty( "subnet-id", CloudMetadatas.toDisplayName( ) )
          .withStringProperty( "vpc-id", FilterStringFunctions.VPC_ID )
          .withStringProperty( "vpcId", FilterStringFunctions.VPC_ID )
          .withPersistenceAlias( "vpc", "vpc" )
          .withPersistenceFilter( "availability-zone", "availabilityZone" )
          .withPersistenceFilter( "availabilityZone" )
          .withPersistenceFilter( "available-ip-address-count", "availableIpAddressCount", Collections.<String>emptySet(), PersistenceFilter.Type.Integer )
          .withPersistenceFilter( "cidr" )
          .withPersistenceFilter( "cidr-block", "cidr" )
          .withPersistenceFilter( "cidrBlock", "cidr" )
          .withPersistenceFilter( "default-for-az", "defaultForAz", Collections.<String>emptySet(), PersistenceFilter.Type.Boolean )
          .withPersistenceFilter( "defaultForAz", "defaultForAz", Collections.<String>emptySet(), PersistenceFilter.Type.Boolean )
          .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
          .withPersistenceFilter( "state", "state", FUtils.valueOfFunction( Subnet.State.class ) )
          .withPersistenceFilter( "subnet-id", "displayName" )
          .withPersistenceFilter( "vpc-id", "vpc.displayName" )
          .withPersistenceFilter( "vpcId", "vpc.displayName" )
          .withUnsupportedProperty("ipv6-cidr-block-association.ipv6-cidr-block")
          .withUnsupportedProperty("ipv6-cidr-block-association.association-id")
          .withUnsupportedProperty("ipv6-cidr-block-association.state")
          .withUnsupportedProperty("outpost-arn")
      );
    }
  }

  public enum FilterStringFunctions implements Function<Subnet,String> {
    ACCOUNT_ID {
      @Override
      public String apply( final Subnet subnet ) {
        return subnet.getOwnerAccountNumber( );
      }
    },
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

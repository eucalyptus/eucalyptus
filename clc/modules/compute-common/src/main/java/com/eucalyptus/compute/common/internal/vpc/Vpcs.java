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

import static com.eucalyptus.compute.common.CloudMetadata.VpcMetadata;

import com.eucalyptus.compute.common.internal.blockstorage.Snapshot;
import com.eucalyptus.compute.common.internal.blockstorage.Snapshots;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ResourceTagSetItemType;
import com.eucalyptus.compute.common.VpcClassicLinkType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.compute.common.internal.tags.Tag;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import io.vavr.collection.Stream;

/**
 *
 */
public interface Vpcs extends Lister<Vpc> {

  String DEFAULT_VPC_CIDR = "172.31.0.0/16";

  ImmutableSet<Cidr> RESERVED_VPC_CIDRS = ImmutableSet.of(
      Cidr.parse( "0.0.0.0/8" ),
      Cidr.parse( "127.0.0.0/8" ),
      Cidr.parse( "169.254.0.0/16" ),
      Cidr.parse( "224.0.0.0/4" )
  );

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super Vpc> filter,
                    Function<? super Vpc,T> transform ) throws VpcMetadataException;

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super Vpc,T> transform ) throws VpcMetadataException;

  <T> T lookupDefault( OwnerFullName ownerFullName,
                       Function<? super Vpc,T> transform ) throws VpcMetadataException;

  boolean delete( VpcMetadata metadata ) throws VpcMetadataException;

  Vpc save( Vpc vpc ) throws VpcMetadataException;

  Vpc updateByExample( Vpc example,
                       OwnerFullName ownerFullName,
                       String key,
                       Callback<Vpc> updateCallback ) throws VpcMetadataException;

  static CompatFunction<Cidr,Boolean> isReservedVpcCidr( @Nonnull List<Cidr> additionalCidrs ) {
    return cidr ->
        Stream.ofAll( RESERVED_VPC_CIDRS )
            .appendAll( additionalCidrs )
            .find( reservedCidr -> reservedCidr.contains( cidr ) )
            .isDefined( );
  }

  @TypeMapper
  public enum VpcToVpcTypeTransform implements Function<Vpc,VpcType> {
    INSTANCE;

    @Nullable
    @Override
    public VpcType apply( @Nullable final Vpc vpc ) {
      return vpc == null ?
          null :
          new VpcType(
              vpc.getDisplayName( ),
              Objects.toString( vpc.getState( ), null ),
              vpc.getCidr( ),
              MoreObjects.firstNonNull( CloudMetadatas.toDisplayName().apply( vpc.getDhcpOptionSet() ), "default" ),
              vpc.getDefaultVpc( ) );
    }
  }

  @TypeMapper
  public enum VpcToVpcClassicLinkTypeTransform implements Function<Vpc, VpcClassicLinkType> {
    INSTANCE;

    @Nullable
    @Override
    public VpcClassicLinkType apply( @Nullable final Vpc vpc ) {
      return vpc == null ?
          null :
          new VpcClassicLinkType(vpc.getDisplayName( ));
    }
  }

  @TypeMapper
  public enum TagToResourceTagSetItemTypeTransform implements Function<Tag,ResourceTagSetItemType> {
    INSTANCE;

    @Nullable
    @Override
    public ResourceTagSetItemType apply( @Nullable final Tag tag ) {
      return tag == null ? null : new ResourceTagSetItemType( tag.getKey( ), tag.getValue( ) );
    }
  }

  public static class VpcFilterSupport extends FilterSupport<Vpc> {
    public VpcFilterSupport(){
      super( builderFor(Vpc.class)
          .withTagFiltering( VpcTag.class, "vpc" )
          .withStringProperty( "cidr", FilterStringFunctions.CIDR )
          .withStringProperty( "cidr-block", FilterStringFunctions.CIDR )
          .withStringProperty( "cidrBlock", FilterStringFunctions.CIDR )
          .withStringProperty( "dhcp-options-id", FilterStringFunctions.DHCP_OPTIONS_ID )
          .withStringProperty( "dhcpOptionsId", FilterStringFunctions.DHCP_OPTIONS_ID )
          .withBooleanProperty( "isDefault", FilterBooleanFunctions.IS_DEFAULT )
          .withStringProperty( "owner-id", FilterStringFunctions.ACCOUNT_ID )
          .withStringProperty( "state", FilterStringFunctions.STATE )
          .withStringProperty( "vpc-id", FilterStringFunctions.VPC_ID )
          .withPersistenceAlias( "dhcpOptionSet", "dhcpOptionSet" )
          .withPersistenceFilter( "cidr" )
          .withPersistenceFilter( "cidrBlock", "cidr" )
          .withPersistenceFilter( "dhcp-options-id", "dhcpOptionSet.displayName", ignoredValueFunction( "default" ) )
          .withPersistenceFilter( "dhcpOptionsId", "dhcpOptionSet.displayName", ignoredValueFunction( "default" ) )
          .withPersistenceFilter( "isDefault", "defaultVpc", Collections.<String>emptySet(), PersistenceFilter.Type.Boolean )
          .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
          .withPersistenceFilter( "state", "state", FUtils.valueOfFunction( Vpc.State.class ) )
          .withPersistenceFilter( "vpc-id", "displayName" )
          .withUnsupportedProperty("cidr-block-association.cidr-block")
          .withUnsupportedProperty("cidr-block-association.association-id")
          .withUnsupportedProperty("cidr-block-association.state")
          .withUnsupportedProperty("ipv6-cidr-block-association.ipv6-cidr-block")
          .withUnsupportedProperty("ipv6-cidr-block-association.ipv6-pool")
          .withUnsupportedProperty("ipv6-cidr-block-association.association-id")
          .withUnsupportedProperty("ipv6-cidr-block-association.state")
      );
    }
  }

  enum FilterStringFunctions implements Function<Vpc,String> {
    ACCOUNT_ID {
      @Override
      public String apply( final Vpc vpc ) {
        return vpc.getOwnerAccountNumber( );
      }
    },
    CIDR {
      @Override
      public String apply( final Vpc vpc ){
        return vpc.getCidr( );
      }
    },
    DHCP_OPTIONS_ID {
      @Override
      public String apply( final Vpc vpc ){
        return MoreObjects.firstNonNull( CloudMetadatas.toDisplayName( ).apply( vpc.getDhcpOptionSet( ) ), "default" );
      }
    },
    STATE {
      @Override
      public String apply( final Vpc vpc ){
        return Objects.toString( vpc.getState(), "" );
      }
    },
    VPC_ID {
      @Override
      public String apply( final Vpc vpc ){
        return vpc.getDisplayName( );
      }
    },
  }

  public enum FilterBooleanFunctions implements Function<Vpc,Boolean> {
    IS_DEFAULT {
      @Override
      public Boolean apply( final Vpc vpc ){
        return vpc.getDefaultVpc( );
      }
    },
  }

  @RestrictedTypes.QuantityMetricFunction( VpcMetadata.class )
  public enum CountVpcs implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( @Nullable final OwnerFullName input ) {
      try ( final TransactionResource tx = Entities.transactionFor( Vpc.class ) ) {
        return Entities.count( Vpc.exampleWithOwner( input ) );
      }
    }
  }
}

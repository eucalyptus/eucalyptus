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

import static com.eucalyptus.compute.common.CloudMetadata.VpcMetadata;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.ResourceTagSetItemType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.compute.common.internal.tags.Tag;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

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

  static boolean isReservedVpcCidr( @Nonnull final Cidr cidr ) {
    return RESERVED_VPC_CIDRS.stream( ).anyMatch( reservedCidr -> reservedCidr.contains( cidr ) );
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
          .withStringProperty( "cidrBlock", FilterStringFunctions.CIDR )
          .withStringProperty( "dhcp-options-id", FilterStringFunctions.DHCP_OPTIONS_ID )
          .withStringProperty( "dhcpOptionsId", FilterStringFunctions.DHCP_OPTIONS_ID )
          .withBooleanProperty( "isDefault", FilterBooleanFunctions.IS_DEFAULT )
          .withStringProperty( "state", FilterStringFunctions.STATE )
          .withStringProperty( "vpc-id", FilterStringFunctions.VPC_ID )
          .withPersistenceAlias( "dhcpOptionSet", "dhcpOptionSet" )
          .withPersistenceFilter( "cidr" )
          .withPersistenceFilter( "cidrBlock", "cidr" )
          .withPersistenceFilter( "dhcp-options-id", "dhcpOptionSet.displayName", ignoredValueFunction( "default" ) )
          .withPersistenceFilter( "dhcpOptionsId", "dhcpOptionSet.displayName", ignoredValueFunction( "default" ) )
          .withPersistenceFilter( "isDefault", "defaultVpc", Collections.<String>emptySet(), PersistenceFilter.Type.Boolean )
          .withPersistenceFilter( "state", "state", FUtils.valueOfFunction( Vpc.State.class ) )
          .withPersistenceFilter( "vpc-id", "displayName" )
      );
    }
  }

  public enum FilterStringFunctions implements Function<Vpc,String> {
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

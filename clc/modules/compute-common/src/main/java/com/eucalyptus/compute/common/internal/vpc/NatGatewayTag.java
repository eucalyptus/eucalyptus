/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal.vpc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.internal.tags.Tag;
import com.eucalyptus.compute.common.internal.tags.TagSupport;
import com.eucalyptus.compute.common.internal.tags.Tags;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_tags_nat_gateways" )
@DiscriminatorValue( "nat-gateway" )
public class NatGatewayTag extends Tag<NatGatewayTag> {
  private static final long serialVersionUID = 1L;

  @JoinColumn( name = "metadata_tag_resource_id", updatable = false, nullable = false )
  @ManyToOne( fetch = FetchType.LAZY )
  private NatGateway natGateway;

  protected NatGatewayTag( ) {
    super( "nat-gateway", ResourceIdFunction.INSTANCE );
  }

  public NatGatewayTag( @Nonnull final NatGateway natGateway,
                        @Nonnull final OwnerFullName ownerFullName,
                        @Nullable final String key,
                        @Nullable final String value ) {
    super( "nat-gateway", ResourceIdFunction.INSTANCE, ownerFullName, key, value );
    setNatGateway( natGateway );
    init();
  }

  public NatGateway getNatGateway( ) {
    return natGateway;
  }

  public void setNatGateway( final NatGateway natGateway ) {
    this.natGateway = natGateway;
  }

  @Nonnull
  public static Tag named( @Nonnull final NatGateway natGateway,
                           @Nonnull final OwnerFullName ownerFullName,
                           @Nullable final String key ) {
    return namedWithValue( natGateway, ownerFullName, key, null );
  }

  @Nonnull
  public static Tag namedWithValue( @Nonnull final NatGateway natGateway,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nullable final String key,
                                    @Nullable final String value ) {
    Preconditions.checkNotNull( natGateway, "natGateway" );
    Preconditions.checkNotNull( ownerFullName, "ownerFullName" );
    return new NatGatewayTag( natGateway, ownerFullName, key, value );
  }

  private enum ResourceIdFunction implements Function<NatGatewayTag,String> {
    INSTANCE {
      @Override
      public String apply( final NatGatewayTag natGatewayTag ) {
        return natGatewayTag.getNatGateway( ).getDisplayName( );
      }
    }
  }

  public static final class NatGatewayTagSupport extends TagSupport {
    public NatGatewayTagSupport() {
      super( NatGateway.class, "nat", "displayName", "natGateway", "InvalidNatGatewayID.NotFound", "The nat gateway '%s' does not exist." );
    }

    @Override
    public Tag createOrUpdate( final CloudMetadata metadata, final OwnerFullName ownerFullName, final String key, final String value ) {
      return Tags.createOrUpdate( new NatGatewayTag( (NatGateway) metadata, ownerFullName, key, value ) );
    }

    @Override
    public Tag example( @Nonnull final CloudMetadata metadata, @Nonnull final OwnerFullName ownerFullName, final String key, final String value ) {
      return NatGatewayTag.namedWithValue( (NatGateway) metadata, ownerFullName, key, value );
    }

    @Override
    public Tag example( @Nonnull final OwnerFullName ownerFullName ) {
      return example( new NatGatewayTag( ), ownerFullName );
    }

    @Override
    public CloudMetadata lookup( final String identifier ) throws TransactionException {
      return Entities.uniqueResult( NatGateway.exampleWithName( null, identifier ) );
    }
  }
}

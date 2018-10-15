/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal.address;

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
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_tags_addresses" )
@DiscriminatorValue( "elastic-ip" )
public class AllocatedAddressTag extends Tag<AllocatedAddressTag> {
  private static final long serialVersionUID = 1L;

  @JoinColumn( name = "metadata_tag_resource_id", updatable = false, nullable = false )
  @ManyToOne( fetch = FetchType.LAZY )
  private AllocatedAddressEntity allocatedAddress;

  protected AllocatedAddressTag( ) {
    super( "elastic-ip", ResourceIdFunction.INSTANCE );
  }

  public AllocatedAddressTag( @Nonnull final AllocatedAddressEntity allocatedAddress,
                              @Nonnull final OwnerFullName ownerFullName,
                              @Nullable final String key,
                              @Nullable final String value ) {
    super( "elastic-ip", ResourceIdFunction.INSTANCE, ownerFullName, key, value );
    setAllocatedAddress( allocatedAddress );
    init();
  }

  public AllocatedAddressEntity getAllocatedAddress( ) {
    return allocatedAddress;
  }

  public void setAllocatedAddress( final AllocatedAddressEntity allocatedAddress ) {
    this.allocatedAddress = allocatedAddress;
  }

  @Nonnull
  public static Tag named( @Nonnull final AllocatedAddressEntity allocatedAddress,
                           @Nonnull final OwnerFullName ownerFullName,
                           @Nullable final String key ) {
    return namedWithValue( allocatedAddress, ownerFullName, key, null );
  }

  @Nonnull
  public static Tag namedWithValue( @Nonnull final AllocatedAddressEntity allocatedAddress,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nullable final String key,
                                    @Nullable final String value ) {
    Preconditions.checkNotNull( allocatedAddress, "allocatedAddress" );
    Preconditions.checkNotNull( ownerFullName, "ownerFullName" );
    return new AllocatedAddressTag( allocatedAddress, ownerFullName, key, value );
  }

  private enum ResourceIdFunction implements Function<AllocatedAddressTag,String> {
    INSTANCE {
      @Override
      public String apply( final AllocatedAddressTag allocatedAddressTag ) {
        return allocatedAddressTag.getAllocatedAddress( ).getAllocationId( );
      }
    }
  }

  public static final class AllocatedAddressTagSupport extends TagSupport {
    public AllocatedAddressTagSupport() {
      super( AllocatedAddressEntity.class,
          "eipalloc",
          "allocationId",
          "allocatedAddress",
          "InvalidAllocationID.NotFound",
          "The address allocation '%s' does not exist." );
    }

    @Override
    public Tag createOrUpdate(
        final CloudMetadata metadata,
        final OwnerFullName ownerFullName,
        final String key,
        final String value ) {
      return Tags.createOrUpdate(
          new AllocatedAddressTag( (AllocatedAddressEntity) metadata, ownerFullName, key, value ) );
    }

    @Override
    public Tag example( @Nonnull final CloudMetadata metadata,
                        @Nonnull final OwnerFullName ownerFullName,
                        final String key,
                        final String value ) {
      return AllocatedAddressTag.namedWithValue( (AllocatedAddressEntity) metadata, ownerFullName, key, value );
    }

    @Override
    public Tag example( @Nonnull final OwnerFullName ownerFullName ) {
      return example( new AllocatedAddressTag( ), ownerFullName );
    }

    @Override
    public CloudMetadata lookup( final String identifier ) {
      return Entities.criteriaQuery( AllocatedAddressEntity.class )
          .whereEqual( AllocatedAddressEntity_.allocationId, identifier )
          .uniqueResult( );
    }
  }
}

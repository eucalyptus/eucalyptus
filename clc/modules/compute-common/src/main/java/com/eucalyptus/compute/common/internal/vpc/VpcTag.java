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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.compute.common.internal.tags.Tag;
import com.eucalyptus.compute.common.internal.tags.TagSupport;
import com.eucalyptus.compute.common.internal.tags.Tags;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_tags_vpcs" )
@DiscriminatorValue( "vpc" )
public class VpcTag extends Tag<VpcTag> {
  private static final long serialVersionUID = 1L;

  @JoinColumn( name = "metadata_tag_resource_id", updatable = false, nullable = false )
  @ManyToOne( fetch = FetchType.LAZY )
  private Vpc vpc;

  protected VpcTag() {
    super( "vpc", ResourceIdFunction.INSTANCE );
  }

  public VpcTag( @Nonnull final Vpc vpc,
                 @Nonnull final OwnerFullName ownerFullName,
                 @Nullable final String key,
                 @Nullable final String value ) {
    super( "vpc", ResourceIdFunction.INSTANCE, ownerFullName, key, value );
    setVpc( vpc );
    init();
  }

  public Vpc getVpc( ) {
    return vpc;
  }

  public void setVpc( final Vpc vpc ) {
    this.vpc = vpc;
  }

  @Nonnull
  public static Tag named( @Nonnull final Vpc vpc,
                           @Nonnull final OwnerFullName ownerFullName,
                           @Nullable final String key ) {
    return namedWithValue( vpc, ownerFullName, key, null );
  }

  @Nonnull
  public static Tag namedWithValue( @Nonnull final Vpc vpc,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nullable final String key,
                                    @Nullable final String value ) {
    Preconditions.checkNotNull( vpc, "vpc" );
    Preconditions.checkNotNull( ownerFullName, "ownerFullName" );
    return new VpcTag( vpc, ownerFullName, key, value );
  }

  private enum ResourceIdFunction implements Function<VpcTag,String> {
    INSTANCE {
      @Override
      public String apply( final VpcTag vpcTag ) {
        return vpcTag.getVpc( ).getDisplayName( );
      }
    }
  }

  public static final class VpcTagSupport extends TagSupport {
    public VpcTagSupport() {
      super( Vpc.class, "vpc", "displayName", "vpc", " InvalidVpcID.NotFound", "The vpc '%s' does not exist." );
    }

    @Override
    public Tag createOrUpdate( final CloudMetadata metadata, final OwnerFullName ownerFullName, final String key, final String value ) {
      return Tags.createOrUpdate( new VpcTag( (Vpc) metadata, ownerFullName, key, value ) );
    }

    @Override
    public Tag example( @Nonnull final CloudMetadata metadata, @Nonnull final OwnerFullName ownerFullName, final String key, final String value ) {
      return VpcTag.namedWithValue( (Vpc) metadata, ownerFullName, key, value );
    }

    @Override
    public Tag example( @Nonnull final OwnerFullName ownerFullName ) {
      return example( new VpcTag( ), ownerFullName );
    }

    @Override
    public CloudMetadata lookup( final String identifier ) throws TransactionException {
      return Entities.uniqueResult( Vpc.exampleWithName( null, identifier ) );
    }
  }
}

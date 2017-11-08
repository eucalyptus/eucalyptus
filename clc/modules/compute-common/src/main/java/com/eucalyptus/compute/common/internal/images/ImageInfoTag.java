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
package com.eucalyptus.compute.common.internal.images;

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
import com.google.common.collect.Sets;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_tags_images" )
@DiscriminatorValue( "image" )
public class ImageInfoTag extends Tag<ImageInfoTag> {
  private static final long serialVersionUID = 1L;

  @JoinColumn( name = "metadata_tag_resource_id", updatable = false, nullable = false )
  @ManyToOne( fetch = FetchType.LAZY )
  private ImageInfo image;

  protected ImageInfoTag() {
    super( "image", ResourceIdFunction.INSTANCE );
  }

  public ImageInfoTag( @Nonnull final ImageInfo image,
                       @Nonnull final OwnerFullName ownerFullName,
                       @Nullable final String key,
                       @Nullable final String value ) {
    super( "image", ResourceIdFunction.INSTANCE, ownerFullName, key, value );
    setImage( image );
    init();
  }

  public ImageInfo getImage() {
    return image;
  }

  public void setImage(final ImageInfo image) {
    this.image = image;
  }

  @Nonnull
  public static Tag named( @Nonnull final ImageInfo image,
                           @Nonnull final OwnerFullName ownerFullName,
                           @Nullable final String key ) {
    return namedWithValue( image, ownerFullName, key, null );
  }

  @Nonnull
  public static Tag namedWithValue( @Nonnull final ImageInfo image,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nullable final String key,
                                    @Nullable final String value ) {
    Preconditions.checkNotNull( image, "image" );
    Preconditions.checkNotNull( ownerFullName, "ownerFullName" );
    return new ImageInfoTag( image, ownerFullName, key, value );
  }
  
  private enum ResourceIdFunction implements Function<ImageInfoTag,String> {
    INSTANCE {
      @Override
      public String apply( final ImageInfoTag imageInfoTag ) {
        return imageInfoTag.getImage().getDisplayName();
      }
    }
  }

  public static final class ImageInfoTagSupport extends TagSupport {
    public ImageInfoTagSupport() {
      super( ImageInfo.class, Sets.newHashSet( "emi", "eri", "eki", "ami", "ari", "aki" ), "displayName", "image", "InvalidAMIID.NotFound", "The image '%s' does not exist" );
    } 
    
    @Override
    public Tag createOrUpdate( final CloudMetadata metadata, final OwnerFullName ownerFullName, final String key, final String value ) {
      return Tags.createOrUpdate(  new ImageInfoTag( (ImageInfo) metadata, ownerFullName, key, value ) );
    }

    @Override
    public Tag example( @Nonnull final OwnerFullName ownerFullName ) {
      return example( new ImageInfoTag(), ownerFullName );
    }

    @Override
    public Tag example( @Nonnull final CloudMetadata metadata, @Nonnull final OwnerFullName ownerFullName, final String key, final String value ) {
      return ImageInfoTag.namedWithValue( (ImageInfo) metadata, ownerFullName, key, value );
    }

    @Override
    public CloudMetadata lookup( final String identifier ) throws TransactionException {
      return Entities.uniqueResult( ImageInfo.named( identifier ) );
    }
  }
}

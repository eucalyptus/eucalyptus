/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.images;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.cloud.CloudMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.tags.Tag;
import com.eucalyptus.tags.TagSupport;
import com.eucalyptus.tags.Tags;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_tags_images" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
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
      super( ImageInfo.class, Sets.newHashSet( "emi", "eri", "eki" ), "displayName", "image", "InvalidAMIID.NotFound", "The image '%s' does not exist" );
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

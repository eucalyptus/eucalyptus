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
package com.eucalyptus.blockstorage;

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

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_tags_snapshots" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( "snapshot" )
public class SnapshotTag extends Tag<SnapshotTag> {
  private static final long serialVersionUID = 1L;

  @JoinColumn( name = "metadata_tag_resource_id", updatable = false, nullable = false )
  @ManyToOne( fetch = FetchType.LAZY )
  private Snapshot snapshot;

  protected SnapshotTag() {
    super( "snapshot", ResourceIdFunction.INSTANCE );
  }

  public SnapshotTag( @Nonnull final Snapshot snapshot,
                      @Nonnull final OwnerFullName ownerFullName,
                      @Nullable final String key,
                      @Nullable final String value ) {
    super( "snapshot", ResourceIdFunction.INSTANCE, ownerFullName, key, value );
    setSnapshot( snapshot );
    init();
  }

  public Snapshot getSnapshot() {
    return snapshot;
  }

  public void setSnapshot( final Snapshot snapshot ) {
    this.snapshot = snapshot;
  }

  @Nonnull
  public static Tag named( @Nonnull final Snapshot snapshot,
                           @Nonnull final OwnerFullName ownerFullName,
                           @Nullable final String key ) {
    return namedWithValue( snapshot, ownerFullName, key, null );
  }

  @Nonnull
  public static Tag namedWithValue( @Nonnull final Snapshot snapshot,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nullable final String key,
                                    @Nullable final String value ) {
    Preconditions.checkNotNull( snapshot, "snapshot" );
    Preconditions.checkNotNull( ownerFullName, "ownerFullName" );
    return new SnapshotTag( snapshot, ownerFullName, key, value );
  }

  private enum ResourceIdFunction implements Function<SnapshotTag,String> {
    INSTANCE {
      @Override
      public String apply( final SnapshotTag snapshotTag ) {
        return snapshotTag.getSnapshot().getDisplayName();
      }
    }
  }

  public static final class SnapshotTagSupport extends TagSupport {
    public SnapshotTagSupport() {
      super( Snapshot.class, "snap", "displayName", "snapshot", "InvalidSnapshot.NotFound", "The snapshot '%s' does not exist." );
    }
    
    @Override
    public Tag createOrUpdate( final CloudMetadata metadata, final OwnerFullName ownerFullName, final String key, final String value ) {
      return Tags.createOrUpdate( new SnapshotTag( (Snapshot) metadata, ownerFullName, key, value ) );
    }

    @Override
    public Tag example( @Nonnull final CloudMetadata metadata, @Nonnull final OwnerFullName ownerFullName, final String key, final String value ) {
      return SnapshotTag.namedWithValue( (Snapshot) metadata, ownerFullName, key, value );
    }

    @Override
    public Tag example( @Nonnull final OwnerFullName ownerFullName ) {
      return example( new SnapshotTag(), ownerFullName );
    }

    @Override
    public CloudMetadata lookup( final String identifier ) throws TransactionException {
      return Entities.uniqueResult( Snapshot.named( null, identifier ) );
    }
  }  
}

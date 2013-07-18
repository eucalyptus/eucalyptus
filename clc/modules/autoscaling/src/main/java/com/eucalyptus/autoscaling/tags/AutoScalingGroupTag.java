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
package com.eucalyptus.autoscaling.tags;

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
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_autoscaling" )
@Table( name = "metadata_tags_groups" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( "auto-scaling-group" )
public class AutoScalingGroupTag extends Tag<AutoScalingGroupTag> {
  private static final long serialVersionUID = 1L;

  @JoinColumn( name = "metadata_tag_resource_id", updatable = false, nullable = false )
  @ManyToOne( fetch = FetchType.LAZY )
  private AutoScalingGroup group;

  protected AutoScalingGroupTag() {
    super( "auto-scaling-group", ResourceIdFunction.INSTANCE );
  }

  public AutoScalingGroupTag( @Nonnull final AutoScalingGroup group,
                              @Nonnull final OwnerFullName ownerFullName,
                              @Nullable final String key,
                              @Nullable final String value,
                              @Nullable final Boolean propagateAtLaunch ) {
    super( "auto-scaling-group", ResourceIdFunction.INSTANCE, ownerFullName, key, value, propagateAtLaunch );
    setGroup( group );
    setResourceId( getResourceId() ); // Set for query by example use
  }

  public AutoScalingGroup getGroup() {
    return group;
  }

  public void setGroup( final AutoScalingGroup group ) {
    this.group = group;
  }

  @Nonnull
  public static Tag named( @Nonnull final AutoScalingGroup group,
                           @Nonnull final OwnerFullName ownerFullName,
                           @Nullable final String key ) {
    return namedWithValue( group, ownerFullName, key, null );
  }

  @Nonnull
  public static Tag namedWithValue( @Nonnull final AutoScalingGroup group,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nullable final String key,
                                    @Nullable final String value ) {
    Preconditions.checkNotNull( group, "auto-scaling-group" );
    Preconditions.checkNotNull( ownerFullName, "ownerFullName" );
    return new AutoScalingGroupTag( group, ownerFullName, key, value,  null );
  }

  @Nonnull
  public static AutoScalingGroupTag createUnassigned() {
    return new AutoScalingGroupTag();
  }

  private enum ResourceIdFunction implements Function<AutoScalingGroupTag,String> {
    INSTANCE {
      @Override
      public String apply( final AutoScalingGroupTag autoScalingGroupTag ) {
        return autoScalingGroupTag.getGroup().getAutoScalingGroupName();
      }
    }
  }

  public static class AutoScalingGroupTagSupport extends TagSupport {
    public AutoScalingGroupTagSupport() {
      super( AutoScalingGroup.class, "auto-scaling-group", "displayName", "group" );
    }

    @Override
    public Tag createOrUpdate( final AutoScalingMetadata metadata,
                               final OwnerFullName ownerFullName,
                               final String key,
                               final String value,
                               final Boolean propagateAtLaunch ) {
      return Tags.createOrUpdate( new AutoScalingGroupTag( (AutoScalingGroup) metadata, ownerFullName, key, value, propagateAtLaunch ) );
    }

    @Override
    public Tag example( @Nonnull final AutoScalingMetadata metadata,
                        @Nonnull final OwnerFullName ownerFullName,
                        final String key,
                        final String value ) {
      return AutoScalingGroupTag.namedWithValue( (AutoScalingGroup) metadata, ownerFullName, key, value );
    }

    @Override
    public Tag example( @Nonnull final OwnerFullName ownerFullName ) {
      return example( new AutoScalingGroupTag(), ownerFullName );
    }

    @Override
    public AutoScalingGroup lookup( final OwnerFullName owner,
                                    final String identifier ) throws TransactionException {
      return Entities.uniqueResult( AutoScalingGroup.named( owner, identifier ) );
    }
  }
}

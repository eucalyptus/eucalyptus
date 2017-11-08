/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.autoscaling.common.internal.tags;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroup;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_autoscaling" )
@Table( name = "metadata_tags_groups" )
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

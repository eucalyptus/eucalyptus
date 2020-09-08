/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal.vm;

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
@Table( name = "metadata_tags_launch_templates" )
@DiscriminatorValue( "launch-template" )
public class LaunchTemplateTag extends Tag<LaunchTemplateTag> {
  private static final long serialVersionUID = 1L;

  @JoinColumn( name = "metadata_tag_resource_id", updatable = false, nullable = false )
  @ManyToOne( fetch = FetchType.LAZY )
  private LaunchTemplate launchTemplate;

  protected LaunchTemplateTag( ) {
    super( "launch-template", ResourceIdFunction.INSTANCE );
  }

  public LaunchTemplateTag( @Nonnull final LaunchTemplate launchTemplate,
                            @Nonnull final OwnerFullName ownerFullName,
                            @Nullable final String key,
                            @Nullable final String value ) {
    super( "launch-template", ResourceIdFunction.INSTANCE, ownerFullName, key, value );
    setLaunchTemplate( launchTemplate );
    init();
  }

  public LaunchTemplate getLaunchTemplate() {
    return launchTemplate;
  }

  public void setLaunchTemplate( final LaunchTemplate launchTemplate ) {
    this.launchTemplate = launchTemplate;
  }

  @Nonnull
  public static Tag named( @Nonnull final LaunchTemplate launchTemplate,
                           @Nonnull final OwnerFullName ownerFullName,
                           @Nullable final String key ) {
    return namedWithValue( launchTemplate, ownerFullName, key, null );
  }

  @Nonnull
  public static Tag namedWithValue( @Nonnull final LaunchTemplate launchTemplate,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nullable final String key,
                                    @Nullable final String value ) {
    Preconditions.checkNotNull( launchTemplate, "launchTemplate" );
    Preconditions.checkNotNull( ownerFullName, "ownerFullName" );
    return new LaunchTemplateTag( launchTemplate, ownerFullName, key, value );
  }

  private enum ResourceIdFunction implements Function<LaunchTemplateTag,String> {
    INSTANCE {
      @Override
      public String apply( final LaunchTemplateTag launchTemplateTag ) {
        return launchTemplateTag.getLaunchTemplate( ).getDisplayName( );
      }
    }
  }

  public static final class LaunchTemplateTagSupport extends TagSupport {
    public LaunchTemplateTagSupport() {
      super( LaunchTemplate.class, "lt", "displayName", "launchTemplate", " InvalidLaunchTemplateId.NotFound", "The launch template '%s' does not exist." );
    }

    @Override
    public Tag createOrUpdate(final CloudMetadata metadata, final OwnerFullName ownerFullName, final String key, final String value ) {
      return Tags.createOrUpdate( new LaunchTemplateTag( (LaunchTemplate) metadata, ownerFullName, key, value ) );
    }

    @Override
    public Tag example( @Nonnull final CloudMetadata metadata, @Nonnull final OwnerFullName ownerFullName, final String key, final String value ) {
      return LaunchTemplateTag.namedWithValue( (LaunchTemplate) metadata, ownerFullName, key, value );
    }

    @Override
    public Tag example( @Nonnull final OwnerFullName ownerFullName ) {
      return example( new LaunchTemplateTag( ), ownerFullName );
    }

    @Override
    public CloudMetadata lookup( final String identifier ) throws TransactionException {
      return Entities.uniqueResult( LaunchTemplate.exampleWithName( null, identifier ) );
    }
  }
}


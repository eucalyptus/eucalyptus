/**
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal.vm;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.persistence.metamodel.SingularAttribute;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.compute.common.CloudMetadata.LaunchTemplateMetadata;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.ComputeMetadataException;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.RestrictedTypes.QuantityMetricFunction;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;

/**
 *
 */
public interface LaunchTemplates {

  <T> List<T> list(OwnerFullName ownerFullName,
                   Criterion criterion,
                   Map<String,String> aliases,
                   Predicate<? super LaunchTemplate> filter,
                   Function<? super LaunchTemplate,T> transform ) throws ComputeMetadataException;

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Predicate<? super LaunchTemplate> filter,
                      Function<? super LaunchTemplate,T> transform ) throws ComputeMetadataException;

  <T> T lookupByExample( final LaunchTemplate example,
                         @Nullable final OwnerFullName ownerFullName,
                         final String key,
                         final Predicate<? super LaunchTemplate> filter,
                         final Function<? super LaunchTemplate,T> transform ) throws ComputeMetadataException;

  boolean delete( final LaunchTemplateMetadata metadata ) throws ComputeMetadataException;

  LaunchTemplate save( LaunchTemplate subnet ) throws ComputeMetadataException;


  static EntityRestriction<LaunchTemplate> restriction(final OwnerFullName owner,
                                                       final String id,
                                                       final String name ) {
    final SingularAttribute<? super LaunchTemplate, String> attribute;
    final String value;
    if (!Strings.isNullOrEmpty(id)) {
      attribute = LaunchTemplate_.displayName;
      value = id;
    } else {
      attribute = LaunchTemplate_.name;
      value = name;
    }
    return Entities.restriction(LaunchTemplate.class)
        .equal(LaunchTemplate_.ownerAccountNumber, owner.getAccountNumber())
        .equal(attribute, value)
        .build();
  }

  @TypeMapper
  enum LaunchTemplateToLaunchTemplateTypeTransform implements Function<LaunchTemplate,com.eucalyptus.compute.common.LaunchTemplate> {
    INSTANCE;

    @Nullable
    @Override
    public com.eucalyptus.compute.common.LaunchTemplate apply(@Nullable final LaunchTemplate launchTemplate) {
      final com.eucalyptus.compute.common.LaunchTemplate launchTemplateType = new com.eucalyptus.compute.common.LaunchTemplate();
      try {
        launchTemplateType.setCreatedBy(Accounts.getAccountArn(launchTemplate.getOwnerAccountNumber()));
      } catch (AuthException ignore) {
      }
      launchTemplateType.setCreateTime(launchTemplate.getCreationTimestamp());
      launchTemplateType.setDefaultVersionNumber(1L);
      launchTemplateType.setLatestVersionNumber(1L);
      launchTemplateType.setLaunchTemplateId(launchTemplate.getDisplayName());
      launchTemplateType.setLaunchTemplateName(launchTemplate.getName());
      return launchTemplateType;
    }
  }

  class LaunchTemplateFilterSupport extends FilterSupport<LaunchTemplate> {
    public LaunchTemplateFilterSupport( ) {
      super( builderFor( LaunchTemplate.class )
          .withTagFiltering( LaunchTemplateTag.class, "launchTemplate" )
          .withInternalStringProperty( "launch-template-id", CloudMetadatas.toDisplayName() )
          .withPersistenceFilter( "launch-template-id", "displayName")
          .withStringProperty( "launch-template-name", LaunchTemplate::getName )
          .withPersistenceFilter( "launch-template-name", "name")
      );
    }
  }

  @QuantityMetricFunction( LaunchTemplateMetadata.class )
  enum CountLaunchTemplates implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( @Nullable final OwnerFullName input ) {
      try ( final TransactionResource tx = Entities.transactionFor( LaunchTemplate.class ) ) {
        return Entities.count( LaunchTemplate.exampleWithOwner( input ) );
      }
    }
  }
}

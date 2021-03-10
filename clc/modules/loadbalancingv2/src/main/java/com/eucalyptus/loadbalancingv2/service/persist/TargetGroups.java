/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancingv2.service.persist.entities.TargetGroup;
import com.eucalyptus.loadbalancingv2.service.persist.views.TargetGroupView;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.msgs.Matcher;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

public interface TargetGroups {

  <T> T lookupByName(
      @Nullable OwnerFullName ownerFullName,
      String name,
      Predicate<? super TargetGroup> filter,
      Function<? super TargetGroup, T> transform
  ) throws Loadbalancingv2MetadataException;

  <T> List<T> listByExample(
      TargetGroup example,
      Predicate<? super TargetGroup> filter,
      Function<? super TargetGroup, T> transform
  ) throws Loadbalancingv2MetadataException;

  <T> T updateByExample(
      TargetGroup example,
      OwnerFullName ownerFullName,
      String key,
      Predicate<? super TargetGroup> filter,
      Function<? super TargetGroup, T> updateTransform
  ) throws Loadbalancingv2MetadataException;

  TargetGroup save(TargetGroup loadBalancer) throws Loadbalancingv2MetadataException;

  boolean delete(Loadbalancingv2Metadata.TargetgroupMetadata metadata) throws Loadbalancingv2MetadataException;

  @TypeMapper
  enum TargetGroupViewToTargetGroupTransform
      implements Function<TargetGroupView, com.eucalyptus.loadbalancingv2.common.msgs.TargetGroup> {
    INSTANCE;

    @Nullable
    @Override
    public com.eucalyptus.loadbalancingv2.common.msgs.TargetGroup apply(@Nullable final TargetGroupView view) {
      com.eucalyptus.loadbalancingv2.common.msgs.TargetGroup targetGroup = null;
      if (view != null) {
        targetGroup = new com.eucalyptus.loadbalancingv2.common.msgs.TargetGroup();

        targetGroup.setTargetGroupName(view.getDisplayName());
        targetGroup.setTargetGroupArn(view.getArn());

        targetGroup.setVpcId(view.getVpcId());
        targetGroup.setPort(view.getPort());
        targetGroup.setProtocol(Objects.toString(view.getProtocol(), null));
        targetGroup.setProtocolVersion(Objects.toString(view.getProtocolVersion(), null));

        final Matcher matcher = new Matcher();
        matcher.setHttpCode(view.getMatcherGrpcCode());
        matcher.setHttpCode(view.getMatcherHttpCode());
        targetGroup.setMatcher(matcher);

        targetGroup.setHealthCheckEnabled(view.getHealthCheckEnabled());
        targetGroup.setHealthCheckIntervalSeconds(view.getHealthCheckIntervalSeconds());
        targetGroup.setHealthCheckTimeoutSeconds(view.getHealthCheckTimeoutSeconds());
        targetGroup.setHealthCheckPort(Objects.toString(view.getHealthCheckPort(), null));
        targetGroup.setHealthCheckPath(view.getHealthCheckPath());
        targetGroup.setHealthCheckProtocol(Objects.toString(view.getHealthCheckProtocol(), null));

        targetGroup.setHealthyThresholdCount(view.getHealthyThresholdCount());
        targetGroup.setUnhealthyThresholdCount(view.getUnhealthyThresholdCount());
      }
      return targetGroup;
    }
  }

  @RestrictedTypes.QuantityMetricFunction(Loadbalancingv2Metadata.TargetgroupMetadata.class)
  enum CountTargetGroups implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply(final OwnerFullName input) {
      try (final TransactionResource db = Entities.transactionFor(TargetGroup.class)) {
        return Entities.count(TargetGroup.named(input, null));
      }
    }
  }
}

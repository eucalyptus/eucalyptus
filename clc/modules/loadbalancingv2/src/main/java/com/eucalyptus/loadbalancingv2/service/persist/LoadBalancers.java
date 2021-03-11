/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.msgs.AvailabilityZone;
import com.eucalyptus.loadbalancingv2.common.msgs.AvailabilityZones;
import com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancerState;
import com.eucalyptus.loadbalancingv2.common.msgs.SecurityGroups;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerView;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import io.vavr.collection.Stream;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;


public interface LoadBalancers {

  <T> T lookupByName(
      @Nullable OwnerFullName ownerFullName,
      String name,
      Predicate<? super LoadBalancer> filter,
      Function<? super LoadBalancer, T> transform
  ) throws Loadbalancingv2MetadataException;

  <T> List<T> listByExample(
      LoadBalancer example,
      Predicate<? super LoadBalancer> filter,
      Function<? super LoadBalancer, T> transform
  ) throws Loadbalancingv2MetadataException;

  <T> T updateByExample(
      LoadBalancer example,
      OwnerFullName ownerFullName,
      String key,
      Predicate<? super LoadBalancer> filter,
      Function<? super LoadBalancer, T> updateTransform
  ) throws Loadbalancingv2MetadataException;

  LoadBalancer save(LoadBalancer loadBalancer) throws Loadbalancingv2MetadataException;

  boolean delete(Loadbalancingv2Metadata.LoadbalancerMetadata metadata) throws Loadbalancingv2MetadataException;

  @TypeMapper
  enum LoadBalancerViewToLoadBalancerTransform
      implements Function<LoadBalancerView, com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancer> {
    INSTANCE;

    @Nullable
    @Override
    public com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancer apply(@Nullable final LoadBalancerView view) {
      com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancer balancer = null;
      if (view != null) {
        balancer = new com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancer();

        balancer.setLoadBalancerName(view.getDisplayName());
        balancer.setLoadBalancerArn(view.getArn());
        balancer.setCreatedTime(view.getCreationTimestamp());
        //balancer.setCanonicalHostedZoneId(); //TODO:STEVE: hosted zone id
        balancer.setDNSName(view.getScheme().schemev1().generate(
            view.getDisplayName(),
            view.getOwnerAccountNumber()));

        final LoadBalancerState state = new LoadBalancerState();
        state.setCode(Objects.toString(view.getState(), null));
        balancer.setState(state);

        balancer.setType(Objects.toString(view.getType(), null));
        balancer.setScheme(Objects.toString(view.getScheme(), null));
        balancer.setIpAddressType(Objects.toString(view.getIpAddressType(), null));

        final SecurityGroups securityGroups = new SecurityGroups();
        securityGroups.getMember().addAll(view.getSecurityGroupIds());
        balancer.setSecurityGroups(securityGroups);

        balancer.setVpcId(view.getVpcId());

        final AvailabilityZones availabilityZones = new AvailabilityZones();
        availabilityZones.getMember().addAll(Stream.ofAll(view.getSubnetIds()).map( subnetId -> {
          AvailabilityZone zone = new AvailabilityZone();
          zone.setSubnetId(subnetId);
          return zone;
        }).toJavaList());
        balancer.setAvailabilityZones(availabilityZones);

      }
      return balancer;
    }
  }

  @RestrictedTypes.QuantityMetricFunction(Loadbalancingv2Metadata.LoadbalancerMetadata.class)
  enum CountLoadBalancers implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply(final OwnerFullName input) {
      try (final TransactionResource db = Entities.transactionFor(LoadBalancer.class)) {
        return Entities.count(LoadBalancer.named(input, null));
      }
    }
  }
}

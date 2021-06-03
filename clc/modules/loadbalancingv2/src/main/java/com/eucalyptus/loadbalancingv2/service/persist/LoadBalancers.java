/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.persist;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.msgs.AvailabilityZone;
import com.eucalyptus.loadbalancingv2.common.msgs.AvailabilityZones;
import com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancerState;
import com.eucalyptus.loadbalancingv2.common.msgs.SecurityGroups;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancingv2.service.persist.views.ImmutableListenerView;
import com.eucalyptus.loadbalancingv2.service.persist.views.ImmutableLoadBalancerListenersView;
import com.eucalyptus.loadbalancingv2.service.persist.views.ImmutableLoadBalancerView;
import com.eucalyptus.loadbalancingv2.service.persist.views.ListenerRuleView;
import com.eucalyptus.loadbalancingv2.service.persist.views.ListenerView;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerListenersView;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerView;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import io.vavr.collection.Stream;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;

public interface LoadBalancers {

  long EXPIRY_AGE = 900_000L;

  CompatFunction<LoadBalancer, LoadBalancerListenersView> LISTENERS_VIEW =
      loadBalancer -> ImmutableLoadBalancerListenersView.builder()
          .loadBalancer(ImmutableLoadBalancerView.copyOf(loadBalancer))
          .listeners(Stream.ofAll(loadBalancer.getListeners()).map(ImmutableListenerView::copyOf))
          .build();

  <T> T lookupByExample(
      LoadBalancer example,
      @Nullable final OwnerFullName ownerFullName,
      String key,
      Predicate<? super LoadBalancer> filter,
      Function<? super LoadBalancer,T> transform
  ) throws Loadbalancingv2MetadataException;

  <T> T lookupByName(
      @Nullable OwnerFullName ownerFullName,
      String name,
      Predicate<? super LoadBalancer> filter,
      Function<? super LoadBalancer, T> transform
  ) throws Loadbalancingv2MetadataException;

  <T> List<T> list(
      @Nullable OwnerFullName ownerFullName,
      Criterion criterion,
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

  default <T> T updateByView(
      final LoadBalancerView view,
      final Predicate<? super LoadBalancer> filter,
      final Function<? super LoadBalancer, T> updateTransform
  ) throws Loadbalancingv2MetadataException {
    final AccountFullName accountFullName = AccountFullName.getInstance(view.getOwnerAccountNumber());
    return updateByExample(
        LoadBalancer.named(accountFullName, view.getDisplayName()),
        accountFullName,
        view.getDisplayName(),
        filter,
        updateTransform
    );
  }

  LoadBalancer save(LoadBalancer loadBalancer) throws Loadbalancingv2MetadataException;

  boolean delete(Loadbalancingv2Metadata.LoadbalancerMetadata metadata) throws Loadbalancingv2MetadataException;

  long countByExample(LoadBalancer example) throws Loadbalancingv2MetadataException;

  default long countByOwnerAndType(
      final OwnerFullName ownerFullName,
      final LoadBalancer.Type type
  ) throws Loadbalancingv2MetadataException {
    final LoadBalancer example = LoadBalancer.named(ownerFullName, null);
    example.setType(type);
    return countByExample(example);
  }

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
        balancer.setCanonicalHostedZoneId(view.getCanonicalHostedZoneId());
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

  @TypeMapper
  enum ListenerViewToListenerTransform
      implements Function<ListenerView, com.eucalyptus.loadbalancingv2.common.msgs.Listener> {
    INSTANCE;

    @Nullable
    @Override
    public com.eucalyptus.loadbalancingv2.common.msgs.Listener apply(@Nullable final ListenerView view) {
      com.eucalyptus.loadbalancingv2.common.msgs.Listener listener = null;
      if (view != null) {
        listener = new com.eucalyptus.loadbalancingv2.common.msgs.Listener();
        listener.setListenerArn(view.getArn());
        listener.setLoadBalancerArn(view.getLoadbalancerArn());
        listener.setPort(view.getPort());
        listener.setProtocol(Objects.toString(view.getProtocol(), null));
        listener.setDefaultActions(view.getListenerDefaultActions());
      }
      return listener;
    }
  }

  @TypeMapper
  enum ListenerRuleViewToRuleTransform
      implements Function<ListenerRuleView, com.eucalyptus.loadbalancingv2.common.msgs.Rule> {
    INSTANCE;

    @Nullable
    @Override
    public com.eucalyptus.loadbalancingv2.common.msgs.Rule apply(@Nullable final ListenerRuleView view) {
      com.eucalyptus.loadbalancingv2.common.msgs.Rule rule = null;
      if (view != null) {
        rule = new com.eucalyptus.loadbalancingv2.common.msgs.Rule();
        rule.setRuleArn(view.getArn());
        rule.setIsDefault(false); //TODO:STEVE: default rule
        rule.setPriority(Objects.toString(view.getPriority(), null)); // or "default"
        rule.setActions(view.getRuleActions());
        rule.setConditions(view.getRuleConditions());
      }
      return rule;
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

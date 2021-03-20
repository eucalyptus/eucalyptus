/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service.servo;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.loadbalancing.LoadBalancerHelper;
import com.eucalyptus.loadbalancing.common.msgs.AccessLog;
import com.eucalyptus.loadbalancing.common.msgs.AvailabilityZones;
import com.eucalyptus.loadbalancing.common.msgs.BackendInstance;
import com.eucalyptus.loadbalancing.common.msgs.BackendInstances;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionDraining;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionSettings;
import com.eucalyptus.loadbalancing.common.msgs.CrossZoneLoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescription;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.PolicyNames;
import com.eucalyptus.loadbalancing.dns.LoadBalancerDomainName;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName;
import com.eucalyptus.loadbalancingv2.common.msgs.Action;
import com.eucalyptus.loadbalancingv2.service.persist.LoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.TargetGroups;
import com.eucalyptus.loadbalancingv2.service.persist.entities.Listener;
import com.eucalyptus.loadbalancingv2.service.persist.entities.ListenerRule;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancingv2.service.persist.entities.PersistenceLoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.entities.PersistenceTargetGroups;
import com.eucalyptus.loadbalancingv2.service.persist.entities.Target;
import com.eucalyptus.loadbalancingv2.service.persist.entities.TargetGroup;
import com.eucalyptus.loadbalancingv2.service.persist.views.ServoView;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.vavr.collection.Stream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.log4j.Logger;

public class Loadbalancingv2ServoMetadataSource implements LoadBalancerHelper.ServoMetadataSource {

  private static final Logger logger = Logger.getLogger(Loadbalancingv2ServoMetadataSource.class);

  private final LoadBalancers loadBalancers = new PersistenceLoadBalancers();
  private final TargetGroups targetGroups = new PersistenceTargetGroups();

  @Override
  public List<String> listPoliciesForLoadBalancer(
      final String accountNumber,
      final String loadBalancerNameOrArn
  ) {
    return Lists.newArrayList();
  }

  @Override
  public PolicyDescription getLoadBalancerPolicy(
      final String accountNumber,
      final String loadBalancerNameOrArn,
      final String policyName
  ) {
    throw new RuntimeException("Policy not found " + policyName);
  }

  @Override
  public Map<String, LoadBalancerServoDescription> getLoadBalancerServoDescriptions(
      final String accountNumber,
      final String loadBalancerArn
  ) {
    final Loadbalancingv2ResourceName arn =
        Loadbalancingv2ResourceName.parse(loadBalancerArn, Loadbalancingv2ResourceName.Type.loadbalancer);
    final AccountFullName account = AccountFullName.getInstance(arn.getNamespace());

    try {
      return loadBalancers.lookupByName(
          account,
          arn.getName(),
          Predicates.alwaysTrue(),
          this::toDescriptions
      );
    } catch (Exception e) {
      throw Exceptions.toUndeclared(e);
    }
  }

  @Override
  public Set<String> resolveIpsForLoadBalancer(
      final String accountNumber,
      final String loadBalancerName
  ) {
    return Stream.ofAll(ServoCache.servosForLoadBalancer(accountNumber, loadBalancerName))
        .map(ServoView::getIpAddress)
        .toJavaSet();
  }

  @Override
  public void notifyServoInstanceFailure(
      final String instanceId
  ) {
    for (final ServoView servoView : ServoCache.servosForInstance(instanceId)) {
      logger.info("Suspect servo instance: " + instanceId + " for balancer " + servoView.getLoadBalancerName());
    }
  }

  private Map<String, LoadBalancerServoDescription> toDescriptions(final LoadBalancer loadBalancer) {
    final Map<String, LoadBalancerServoDescription> descriptions = Maps.newHashMap();

    ServoCache.servosForLoadBalancer(loadBalancer.getNaturalId())
        .forEach(servo -> descriptions.put(servo.getDisplayName(), toDescription(loadBalancer, servo.getAvailabiltyZone())));

    return descriptions;
  }

  private LoadBalancerServoDescription toDescription(
      final LoadBalancer loadBalancer,
      final String availabilityZone
  ) {
    final AccountFullName owner = AccountFullName.getInstance(loadBalancer.getOwnerAccountNumber());

    final LoadBalancerServoDescription description = new LoadBalancerServoDescription();
    description.setCreatedTime(loadBalancer.getCreationTimestamp());
    description.setLoadBalancerName(loadBalancer.getDisplayName());
    description.setDnsName(LoadBalancerDomainName.EXTERNAL.generate(
        loadBalancer.getDisplayName(),
        loadBalancer.getOwnerAccountNumber()));

    final AvailabilityZones availabilityZones = new AvailabilityZones();
    availabilityZones.getMember().add(availabilityZone);
    description.setAvailabilityZones(availabilityZones);

    final BackendInstances backendInstances = new BackendInstances();
    final Consumer<Action> actionConsumer = action -> {
      if ("forward".equals(action.getType()) && action.getTargetGroupArn() != null) {
        try {
          final Loadbalancingv2ResourceName arn =
              Loadbalancingv2ResourceName.parse(action.getTargetGroupArn(),
                  Loadbalancingv2ResourceName.Type.targetgroup);
          final TargetGroup targetGroup = targetGroups.lookupByName(owner, arn.getName(),
              Predicates.alwaysTrue(), Functions.identity());
          final List<Target> targets = targetGroup.getTargets();
          Stream.ofAll(targets)
              .map(target -> {
                final BackendInstance backendInstance = new BackendInstance();
                backendInstance.setInstanceId(target.getTargetId());
                backendInstance.setInstanceIpAddress(target.getIpAddress());
                backendInstance.setReportHealthCheck(true);
                return backendInstance;
              })
              .forEach(backendInstances.getMember()::add);
        } catch (Exception ignore) {
        }
      }
    };
    for (final Listener listener : loadBalancer.getListeners()) {
      Stream.ofAll(listener.getListenerRules())
          .flatMap(rule -> rule.getRuleActions().getMember())
          .forEach(actionConsumer);
      Stream.ofAll(listener.getListenerDefaultActions().getMember())
          .forEach(actionConsumer);
    }
    description.setBackendInstances(backendInstances);

    final HealthCheck healthCheck = new HealthCheck();
    healthCheck.setTarget("TCP:" + loadBalancer.getListeners().get(0).getPort());
    healthCheck.setInterval(30);
    healthCheck.setTimeout(5);
    healthCheck.setHealthyThreshold(3);
    healthCheck.setUnhealthyThreshold(3);
    description.setHealthCheck(healthCheck);

    final ListenerDescriptions listenerDescriptions = new ListenerDescriptions();
    for (final Listener lbListener : loadBalancer.getListeners()) {
      final ListenerDescription listenerDescription = new ListenerDescription();
      final com.eucalyptus.loadbalancing.common.msgs.Listener listener = new com.eucalyptus.loadbalancing.common.msgs.Listener();
      listener.setLoadBalancerPort(lbListener.getPort());
      listener.setProtocol(lbListener.getProtocol().toString());
      listener.setInstancePort(lbListener.getPort()); //TODO:STEVE: target port and protocol
      listenerDescription.setListener(listener);
      final PolicyNames policyNames = new PolicyNames();
      listenerDescription.setPolicyNames(policyNames);
      listenerDescriptions.getMember().add(listenerDescription);
    }
    description.setListenerDescriptions(listenerDescriptions);

    final LoadBalancerAttributes loadBalancerAttributes = new LoadBalancerAttributes();
    final AccessLog accessLog = new AccessLog();
    accessLog.setEnabled(false);
    loadBalancerAttributes.setAccessLog(accessLog);
    final ConnectionDraining connectionDraining = new ConnectionDraining();
    connectionDraining.setEnabled(false);
    loadBalancerAttributes.setConnectionDraining(connectionDraining);
    final ConnectionSettings connectionSettings = new ConnectionSettings();
    connectionSettings.setIdleTimeout(60);
    loadBalancerAttributes.setConnectionSettings(connectionSettings);
    final CrossZoneLoadBalancing crossZoneLoadBalancing = new CrossZoneLoadBalancing();
    crossZoneLoadBalancing.setEnabled(false);
    loadBalancerAttributes.setCrossZoneLoadBalancing(crossZoneLoadBalancing);
    description.setLoadBalancerAttributes(loadBalancerAttributes);

    final PolicyDescriptions policyDescriptions = new PolicyDescriptions();
    description.setPolicyDescriptions(policyDescriptions);

    return description;
  }
}

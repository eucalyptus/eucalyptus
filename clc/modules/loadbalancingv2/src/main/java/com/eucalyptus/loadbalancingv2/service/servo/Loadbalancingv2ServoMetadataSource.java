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
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttributeDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttributeDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.PolicyNames;
import com.eucalyptus.loadbalancing.dns.LoadBalancerDomainName;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName;
import com.eucalyptus.loadbalancingv2.common.msgs.Action;
import com.eucalyptus.loadbalancingv2.service.persist.LoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.TargetGroups;
import com.eucalyptus.loadbalancingv2.service.persist.entities.Listener;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancingv2.service.persist.entities.PersistenceLoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.entities.PersistenceTargetGroups;
import com.eucalyptus.loadbalancingv2.service.persist.entities.Target;
import com.eucalyptus.loadbalancingv2.service.persist.entities.TargetGroup;
import com.eucalyptus.loadbalancingv2.service.persist.views.ServoView;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.Strings;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import io.vavr.collection.Stream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.log4j.Logger;

public class Loadbalancingv2ServoMetadataSource implements LoadBalancerHelper.ServoMetadataSource {

  private static final Logger logger = Logger.getLogger(Loadbalancingv2ServoMetadataSource.class);

  private static final String DEFAULT_SSL_POLICY = "ELBSecurityPolicy-2016-08";
  private static final List<String> DEFAULT_SSL_POLICY_ATTRIBUTES = ImmutableList.<String>builder()
      .add("Protocol-TLSv1")
      .add("Protocol-TLSv1.1")
      .add("Protocol-TLSv1.2")
      .add("Server-Defined-Cipher-Order")
      .add("ECDHE-ECDSA-AES128-GCM-SHA256")
      .add("ECDHE-RSA-AES128-GCM-SHA256")
      .add("ECDHE-ECDSA-AES128-SHA256")
      .add("ECDHE-RSA-AES128-SHA256")
      .add("ECDHE-ECDSA-AES128-SHA")
      .add("ECDHE-RSA-AES128-SHA")
      .add("ECDHE-ECDSA-AES256-GCM-SHA384")
      .add("ECDHE-RSA-AES256-GCM-SHA384")
      .add("ECDHE-ECDSA-AES256-SHA384")
      .add("ECDHE-RSA-AES256-SHA384")
      .add("ECDHE-RSA-AES256-SHA")
      .add("ECDHE-ECDSA-AES256-SHA")
      .add("AES128-GCM-SHA256")
      .add("AES128-SHA256")
      .add("AES128-SHA")
      .add("AES256-GCM-SHA384")
      .add("AES256-SHA256")
      .add("AES256-SHA")
      .build();

  private final LoadBalancers loadBalancers = new PersistenceLoadBalancers();
  private final TargetGroups targetGroups = new PersistenceTargetGroups();

  @Override
  public List<String> listPoliciesForLoadBalancer(
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
          this::toPolicyNames
      );
    } catch (Exception e) {
      throw Exceptions.toUndeclared(e);
    }
  }

  @Override
  public PolicyDescription getLoadBalancerPolicy(
      final String accountNumber,
      final String loadBalancerArn,
      final String policyName
  ) {
    if (DEFAULT_SSL_POLICY.equals(policyName)) {
      final PolicyDescription policy = new PolicyDescription();
      policy.setPolicyName(DEFAULT_SSL_POLICY);
      policy.setPolicyTypeName("SSLNegotiationPolicyType");

      final ArrayList<PolicyAttributeDescription> attrDescs = Lists.newArrayList();
      for (final String attributeName : DEFAULT_SSL_POLICY_ATTRIBUTES) {
        final PolicyAttributeDescription desc = new PolicyAttributeDescription();
        desc.setAttributeName(attributeName);
        desc.setAttributeValue("true");
        attrDescs.add(desc);
      }
      final PolicyAttributeDescriptions descs = new PolicyAttributeDescriptions();
      descs.setMember(attrDescs);
      descs.getMember()
          .sort(Ordering.natural()
              .onResultOf(Strings.nonNull(PolicyAttributeDescription::getAttributeName)));
      policy.setPolicyAttributeDescriptions(descs);
      return policy;
    }
    /* TODO:STEVE: add false attributes?
    {attributeName=Protocol-SSLv3, attributeValue=false},
    {attributeName=DHE-RSA-AES128-SHA, attributeValue=false},
    {attributeName=DHE-DSS-AES128-SHA, attributeValue=false},
    {attributeName=CAMELLIA128-SHA, attributeValue=false},
    {attributeName=EDH-RSA-DES-CBC3-SHA, attributeValue=false},
    {attributeName=DES-CBC3-SHA, attributeValue=false},
    {attributeName=ECDHE-RSA-RC4-SHA, attributeValue=false},
    {attributeName=RC4-SHA, attributeValue=false},
    {attributeName=ECDHE-ECDSA-RC4-SHA, attributeValue=false},
    {attributeName=DHE-DSS-AES256-GCM-SHA384, attributeValue=false},
    {attributeName=DHE-RSA-AES256-GCM-SHA384, attributeValue=false},
    {attributeName=DHE-RSA-AES256-SHA256, attributeValue=false},
    {attributeName=DHE-DSS-AES256-SHA256, attributeValue=false},
    {attributeName=DHE-RSA-AES256-SHA, attributeValue=false},
    {attributeName=DHE-DSS-AES256-SHA, attributeValue=false},
    {attributeName=DHE-RSA-CAMELLIA256-SHA, attributeValue=false},
    {attributeName=DHE-DSS-CAMELLIA256-SHA, attributeValue=false},
    {attributeName=CAMELLIA256-SHA, attributeValue=false},
    {attributeName=EDH-DSS-DES-CBC3-SHA, attributeValue=false},
    {attributeName=DHE-DSS-AES128-GCM-SHA256, attributeValue=false},
    {attributeName=DHE-RSA-AES128-GCM-SHA256, attributeValue=false},
    {attributeName=DHE-RSA-AES128-SHA256, attributeValue=false},
    {attributeName=DHE-DSS-AES128-SHA256, attributeValue=false},
    {attributeName=DHE-RSA-CAMELLIA128-SHA, attributeValue=false},
    {attributeName=DHE-DSS-CAMELLIA128-SHA, attributeValue=false},
    {attributeName=ADH-AES128-GCM-SHA256, attributeValue=false},
    {attributeName=ADH-AES128-SHA, attributeValue=false},
    {attributeName=ADH-AES128-SHA256, attributeValue=false},
    {attributeName=ADH-AES256-GCM-SHA384, attributeValue=false},
    {attributeName=ADH-AES256-SHA, attributeValue=false},
    {attributeName=ADH-AES256-SHA256, attributeValue=false},
    {attributeName=ADH-CAMELLIA128-SHA, attributeValue=false},
    {attributeName=ADH-CAMELLIA256-SHA, attributeValue=false},
    {attributeName=ADH-DES-CBC3-SHA, attributeValue=false},
    {attributeName=ADH-DES-CBC-SHA, attributeValue=false},
    {attributeName=ADH-RC4-MD5, attributeValue=false},
    {attributeName=ADH-SEED-SHA, attributeValue=false},
    {attributeName=DES-CBC-SHA, attributeValue=false},
    {attributeName=DHE-DSS-SEED-SHA, attributeValue=false},
    {attributeName=DHE-RSA-SEED-SHA, attributeValue=false},
    {attributeName=EDH-DSS-DES-CBC-SHA, attributeValue=false},
    {attributeName=EDH-RSA-DES-CBC-SHA, attributeValue=false},
    {attributeName=IDEA-CBC-SHA, attributeValue=false},
    {attributeName=RC4-MD5, attributeValue=false},
    {attributeName=SEED-SHA, attributeValue=false},
    {attributeName=DES-CBC3-MD5, attributeValue=false},
    {attributeName=DES-CBC-MD5, attributeValue=false},
    {attributeName=RC2-CBC-MD5, attributeValue=false},
    {attributeName=PSK-AES256-CBC-SHA, attributeValue=false},
    {attributeName=PSK-3DES-EDE-CBC-SHA, attributeValue=false},
    {attributeName=KRB5-DES-CBC3-SHA, attributeValue=false},
    {attributeName=KRB5-DES-CBC3-MD5, attributeValue=false},
    {attributeName=PSK-AES128-CBC-SHA, attributeValue=false},
    {attributeName=PSK-RC4-SHA, attributeValue=false},
    {attributeName=KRB5-RC4-SHA, attributeValue=false},
    {attributeName=KRB5-RC4-MD5, attributeValue=false},
    {attributeName=KRB5-DES-CBC-SHA, attributeValue=false},
    {attributeName=KRB5-DES-CBC-MD5, attributeValue=false},
    {attributeName=EXP-EDH-RSA-DES-CBC-SHA, attributeValue=false},
    {attributeName=EXP-EDH-DSS-DES-CBC-SHA, attributeValue=false},
    {attributeName=EXP-ADH-DES-CBC-SHA, attributeValue=false},
    {attributeName=EXP-DES-CBC-SHA, attributeValue=false},
    {attributeName=EXP-RC2-CBC-MD5, attributeValue=false},
    {attributeName=EXP-KRB5-RC2-CBC-SHA, attributeValue=false},
    {attributeName=EXP-KRB5-DES-CBC-SHA, attributeValue=false},
    {attributeName=EXP-KRB5-RC2-CBC-MD5, attributeValue=false},
    {attributeName=EXP-KRB5-DES-CBC-MD5, attributeValue=false},
    {attributeName=EXP-ADH-RC4-MD5, attributeValue=false},
    {attributeName=EXP-RC4-MD5, attributeValue=false},
    {attributeName=EXP-KRB5-RC4-SHA, attributeValue=false},
    {attributeName=EXP-KRB5-RC4-MD5, attributeValue=false}
     */

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
  public List<String> getServoInstances(
      final String accountNumber,
      final String loadBalancerArn
  ) {
    final Loadbalancingv2ResourceName arn =
        Loadbalancingv2ResourceName.parse(loadBalancerArn, Loadbalancingv2ResourceName.Type.loadbalancer);
    return Stream.ofAll(ServoCache.servosForLoadBalancer(accountNumber, arn.getName()))
        .map(ServoView::getDisplayName)
        .toJavaList();
  }

  @Override
  public HealthCheck getLoadBalancerHealthCheck(
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
          this::toHealthCheck
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
  public Map<String, String> filterInstanceStatus(
      final String accountNumber,
      final String loadBalancerArn,
      final String servoInstanceId,
      final Map<String, String> statusMap) {
    return statusMap;
  }

  @Override
  public void notifyBackendInstanceStatus(
      final String accountNumber,
      final String loadBalancerArn,
      final Map<String, String> statusMap
  ) {
    final Loadbalancingv2ResourceName arn =
        Loadbalancingv2ResourceName.parse(loadBalancerArn, Loadbalancingv2ResourceName.Type.loadbalancer);
    final AccountFullName account = AccountFullName.getInstance(arn.getNamespace());

    try {
      loadBalancers.updateByExample(
          LoadBalancer.named(account, arn.getName()),
          account,
          arn.getName(),
          Predicates.alwaysTrue(),
          loadBalancer -> {
            for (final TargetGroup targetGroup : loadBalancer.getTargetGroups()) {
              for (final Target target : targetGroup.getTargets()) {
                final String status = statusMap.get(target.getTargetId());
                if ("InService".equals(status)) {
                  Target.State.healthy.apply(target);
                } else {
                  Target.State.unhealthy.apply(target);
                }
              }
            }
            return loadBalancer;
          }
      );
    } catch (Exception e) {
      throw Exceptions.toUndeclared(e);
    }
  }

  @Override
  public void notifyServoInstanceFailure(
      final String instanceId
  ) {
    for (final ServoView servoView : ServoCache.servosForInstance(instanceId)) {
      logger.info("Suspect servo instance: " + instanceId + " for balancer " + servoView.getLoadBalancerName());
    }
  }

  private List<String> toPolicyNames(final LoadBalancer loadBalancer) {
    final Set<String> names = Sets.newTreeSet();
    for (final Listener listener : loadBalancer.getListeners()) {
      if (listener.getDefaultServerCertificateArn() != null) {
        names.add(DEFAULT_SSL_POLICY);
      }
    }
    return Lists.newArrayList(names);
  }

  private Map<String, LoadBalancerServoDescription> toDescriptions(final LoadBalancer loadBalancer) {
    final Map<String, LoadBalancerServoDescription> descriptions = Maps.newHashMap();

    ServoCache.servosForLoadBalancer(loadBalancer.getNaturalId())
        .forEach(servo -> descriptions.put(servo.getDisplayName(), toDescription(loadBalancer, servo.getAvailabiltyZone())));

    return descriptions;
  }

  private HealthCheck toHealthCheck(final LoadBalancer loadBalancer) {
    final AccountFullName owner = AccountFullName.getInstance(loadBalancer.getOwnerAccountNumber());
    final Map<Integer, HealthCheck> healthChecksByListenerPort = Maps.newTreeMap();
    final Map<String, Pair<String,Integer>> listenerToBackendProtocolAndPort = Maps.newHashMap();
    for (final Listener listener : loadBalancer.getListeners()) {
      final Consumer<Action> actionConsumer = action -> {
        if ("forward".equals(action.getType()) && action.getTargetGroupArn() != null) {
          try {
            final Loadbalancingv2ResourceName arn =
                Loadbalancingv2ResourceName.parse(action.getTargetGroupArn(),
                    Loadbalancingv2ResourceName.Type.targetgroup);
            final TargetGroup targetGroup = targetGroups.lookupByName(owner, arn.getName(),
                Predicates.alwaysTrue(), Functions.identity());
            if (targetGroup.getPort() != null && targetGroup.getProtocol() != null) {
              listenerToBackendProtocolAndPort.putIfAbsent(
                  listener.getDisplayName(),
                  Pair.of(targetGroup.getProtocol().name(), targetGroup.getPort()));
            }
            healthChecksByListenerPort.putIfAbsent(listener.getPort(), buildHealthCheck(loadBalancer, targetGroup));
          } catch (Exception ignore) {
          }
        }
      };
      Stream.ofAll(listener.getListenerDefaultActions().getMember()).forEach(actionConsumer);
    }

    final int healthCheckPort = Stream.ofAll(listenerToBackendProtocolAndPort.values())
        .map(CompatFunction.of(Pair.right()))
        .sorted().headOption().getOrElse(() -> loadBalancer.getListeners().get(0).getPort());
    final HealthCheck healthCheck = Stream.ofAll(healthChecksByListenerPort.values())
        .headOption()
        .getOrElse(buildDefaultHealthCheck(loadBalancer, healthCheckPort));
    return healthCheck;
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

    final Map<String, Pair<String,Integer>> listenerToBackendProtocolAndPort = Maps.newHashMap();
    final Set<String> backendIds = Sets.newHashSet();
    final BackendInstances backendInstances = new BackendInstances();
    for (final Listener listener : loadBalancer.getListeners()) {
      final Consumer<Action> actionConsumer = action -> {
        if ("forward".equals(action.getType()) && action.getTargetGroupArn() != null) {
          try {
            final Loadbalancingv2ResourceName arn =
                Loadbalancingv2ResourceName.parse(action.getTargetGroupArn(),
                    Loadbalancingv2ResourceName.Type.targetgroup);
            final TargetGroup targetGroup = targetGroups.lookupByName(owner, arn.getName(),
                Predicates.alwaysTrue(), Functions.identity());
            if (targetGroup.getPort() != null && targetGroup.getProtocol() != null) {
              listenerToBackendProtocolAndPort.putIfAbsent(
                  listener.getDisplayName(),
                  Pair.of(targetGroup.getProtocol().name(), targetGroup.getPort()));
            }
            final List<Target> targets = targetGroup.getTargets();
            Stream.ofAll(targets)
                .map(target -> {
                  final BackendInstance backendInstance = new BackendInstance();
                  backendInstance.setInstanceId(target.getTargetId());
                  backendInstance.setInstanceIpAddress(target.getIpAddress());
                  backendInstance.setReportHealthCheck(true);
                  return backendInstance;
                })
                .filter(backendInstance -> backendIds.add(backendInstance.getInstanceId()))
                .forEach(backendInstances.getMember()::add);
          } catch (Exception ignore) {
          }
        }
      };
      // ignore rules with conditions as back-end does not check them
      //Stream.ofAll(listener.getListenerRules())
      //    .flatMap(rule -> rule.getRuleActions().getMember())
      //    .forEach(actionConsumer);
      Stream.ofAll(listener.getListenerDefaultActions().getMember())
          .forEach(actionConsumer);
      listenerToBackendProtocolAndPort.putIfAbsent(
          listener.getDisplayName(),
          Pair.of(listener.getProtocol().name(), listener.getPort()));
    }
    description.setBackendInstances(backendInstances);
    description.setHealthCheck(toHealthCheck(loadBalancer));

    final ListenerDescriptions listenerDescriptions = new ListenerDescriptions();
    for (final Listener lbListener : loadBalancer.getListeners()) {
      final ListenerDescription listenerDescription = new ListenerDescription();
      final com.eucalyptus.loadbalancing.common.msgs.Listener listener = new com.eucalyptus.loadbalancing.common.msgs.Listener();
      listener.setLoadBalancerPort(lbListener.getPort());
      listener.setProtocol(lbListener.getProtocol().toString());
      listener.setInstancePort(listenerToBackendProtocolAndPort.get(lbListener.getDisplayName()).getRight());
      listener.setInstanceProtocol(listenerToBackendProtocolAndPort.get(lbListener.getDisplayName()).getLeft());
      listener.setSSLCertificateId(lbListener.getDefaultServerCertificateArn());
      listenerDescription.setListener(listener);
      final PolicyNames policyNames = new PolicyNames();
      if (listener.getSSLCertificateId()!=null) {
        policyNames.getMember().add(DEFAULT_SSL_POLICY);
      }
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

  private HealthCheck buildDefaultHealthCheck(
      final LoadBalancer loadBalancer,
      final Integer healthCheckPort
  ) {
    final HealthCheck healthCheck = new HealthCheck();
    if (loadBalancer.getType() == LoadBalancer.Type.application) {
      healthCheck.setTarget("HTTP:" + healthCheckPort + "/");
    } else {
      healthCheck.setTarget("TCP:" + healthCheckPort);
    }
    healthCheck.setInterval(30);
    healthCheck.setTimeout(5);
    healthCheck.setHealthyThreshold(3);
    healthCheck.setUnhealthyThreshold(3);
    return healthCheck;
  }

  private HealthCheck buildHealthCheck(
      final LoadBalancer loadBalancer,
      final TargetGroup targetGroup) {
    final HealthCheck healthCheck = buildDefaultHealthCheck(loadBalancer, targetGroup.getPort());
    final TargetGroup.Protocol defaultProtocol =
        loadBalancer.getType() == LoadBalancer.Type.application ? TargetGroup.Protocol.HTTP : TargetGroup.Protocol.TCP;
    final TargetGroup.Protocol protocol;
    if (targetGroup.getHealthCheckProtocol() != null ||
        targetGroup.getHealthCheckPath() != null ||
        targetGroup.getHealthCheckPort() != null) {
      protocol = MoreObjects.firstNonNull(targetGroup.getHealthCheckProtocol(), defaultProtocol);
      final Integer healthCheckPort =
          MoreObjects.firstNonNull(targetGroup.getHealthCheckPort(), targetGroup.getPort());
      final String healthCheckPath =
          MoreObjects.firstNonNull(targetGroup.getHealthCheckPath(), "/");
      switch (protocol){
        case HTTP:
        case HTTPS:
          healthCheck.setTarget(protocol.name() + ":" + healthCheckPort + healthCheckPath);
          break;
        default:
          healthCheck.setTarget("TCP:" + healthCheckPort);
          break;
      }
    } else {
      protocol = defaultProtocol;
    }

    healthCheck.setInterval(MoreObjects.firstNonNull(
        targetGroup.getHealthCheckIntervalSeconds(),
        MoreObjects.firstNonNull(
            protocol.getDefaultHealthCheckIntervalSeconds(),
            healthCheck.getInterval())));
    healthCheck.setTimeout(MoreObjects.firstNonNull(
        targetGroup.getHealthCheckTimeoutSeconds(),
        healthCheck.getTimeout()));
    healthCheck.setHealthyThreshold(MoreObjects.firstNonNull(
        targetGroup.getHealthyThresholdCount(),
        MoreObjects.firstNonNull(
            protocol.getDefaulHealthyThresholdCount(),
            healthCheck.getHealthyThreshold())));
    healthCheck.setUnhealthyThreshold(MoreObjects.firstNonNull(
        targetGroup.getUnhealthyThresholdCount(),
        MoreObjects.firstNonNull(
            protocol.getDefaulUnhealthyThresholdCount(),
            healthCheck.getUnhealthyThreshold())));

    return healthCheck;
  }

}

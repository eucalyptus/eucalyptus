/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.euare.common.EuareApi;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.ComputeApi;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AbstractPersistent_;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancingHostedZoneManager;
import com.eucalyptus.loadbalancing.LoadBalancingServiceProperties;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadata;
import com.eucalyptus.loadbalancingv2.common.msgs.Action;
import com.eucalyptus.loadbalancingv2.common.msgs.CertificateList;
import com.eucalyptus.loadbalancingv2.common.msgs.ForwardActionConfig;
import com.eucalyptus.loadbalancingv2.common.msgs.Listeners;
import com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancerAttribute;
import com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.loadbalancingv2.common.msgs.Rules;
import com.eucalyptus.loadbalancingv2.common.msgs.SubnetMapping;
import com.eucalyptus.loadbalancingv2.common.msgs.Tag;
import com.eucalyptus.loadbalancingv2.common.msgs.TagDescription;
import com.eucalyptus.loadbalancingv2.common.msgs.TagDescriptions;
import com.eucalyptus.loadbalancingv2.common.msgs.TagList;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetDescription;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetDescriptions;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetGroupAttribute;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetGroupAttributes;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetGroupList;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetGroupTuple;
import com.eucalyptus.loadbalancingv2.service.persist.JsonEncoding;
import com.eucalyptus.loadbalancingv2.service.persist.LoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.LoadBalancingAttribute;
import com.eucalyptus.loadbalancingv2.service.persist.Loadbalancingv2MetadataException;
import com.eucalyptus.loadbalancingv2.service.persist.Loadbalancingv2MetadataNotFoundException;
import com.eucalyptus.loadbalancingv2.service.persist.Taggable;
import com.eucalyptus.loadbalancingv2.service.persist.Tags;
import com.eucalyptus.loadbalancingv2.service.persist.TargetGroups;
import com.eucalyptus.loadbalancingv2.service.persist.entities.Listener;
import com.eucalyptus.loadbalancingv2.service.persist.entities.ListenerRule;
import com.eucalyptus.loadbalancingv2.service.persist.entities.ListenerRule_;
import com.eucalyptus.loadbalancingv2.service.persist.entities.Listener_;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancingv2.service.persist.entities.Target;
import com.eucalyptus.loadbalancingv2.service.persist.entities.TargetGroup;
import com.eucalyptus.loadbalancingv2.service.persist.views.ListenerRuleView;
import com.eucalyptus.loadbalancingv2.service.persist.views.ListenerView;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerView;
import com.eucalyptus.loadbalancingv2.service.persist.views.TagView;
import com.eucalyptus.loadbalancingv2.service.persist.views.TargetGroupView;
import com.eucalyptus.loadbalancingv2.service.persist.views.TargetView;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Metadatas;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName;
import com.eucalyptus.loadbalancingv2.common.msgs.AddListenerCertificatesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.AddListenerCertificatesType;
import com.eucalyptus.loadbalancingv2.common.msgs.AddTagsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.AddTagsType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateListenerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateListenerType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateLoadBalancerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateLoadBalancerType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateRuleResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateRuleType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateTargetGroupResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateTargetGroupType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteListenerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteListenerType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteLoadBalancerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteLoadBalancerType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteRuleResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteRuleType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteTargetGroupResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteTargetGroupType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeregisterTargetsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeregisterTargetsType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeAccountLimitsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeAccountLimitsType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeListenerCertificatesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeListenerCertificatesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeListenersResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeListenersType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeLoadBalancerAttributesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeLoadBalancerAttributesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeLoadBalancersResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeLoadBalancersType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeRulesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeRulesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeSSLPoliciesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeSSLPoliciesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTagsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTagsType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetGroupAttributesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetGroupAttributesType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetGroupsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetGroupsType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetHealthResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.DescribeTargetHealthType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyListenerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyListenerType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyLoadBalancerAttributesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyLoadBalancerAttributesType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyRuleResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyRuleType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyTargetGroupAttributesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyTargetGroupAttributesType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyTargetGroupResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.ModifyTargetGroupType;
import com.eucalyptus.loadbalancingv2.common.msgs.RegisterTargetsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.RegisterTargetsType;
import com.eucalyptus.loadbalancingv2.common.msgs.RemoveListenerCertificatesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.RemoveListenerCertificatesType;
import com.eucalyptus.loadbalancingv2.common.msgs.RemoveTagsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.RemoveTagsType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetIpAddressTypeResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetIpAddressTypeType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetRulePrioritiesResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetRulePrioritiesType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetSecurityGroupsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetSecurityGroupsType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetSubnetsResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.SetSubnetsType;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.CompatSupplier;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncProxy;
import com.google.common.base.Enums;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;

/**
 *
 */
@ComponentNamed
public class Loadbalancingv2Service {

  private static final Logger logger = Logger.getLogger(Loadbalancingv2Service.class);

  private final LoadBalancers loadBalancers;
  private final TargetGroups targetGroups;
  private final Tags tags;

  @Inject
  public Loadbalancingv2Service(
      final LoadBalancers loadBalancers,
      final TargetGroups targetGroups,
      final Tags tags
  ) {
    this.loadBalancers = loadBalancers;
    this.targetGroups = targetGroups;
    this.tags = tags;
  }

  public AddListenerCertificatesResponseType addListenerCertificates(final AddListenerCertificatesType request) {
    return request.getReply();
  }

  public AddTagsResponseType addTags(final AddTagsType request) throws Loadbalancingv2Exception {
    final AddTagsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName().asAccountFullName();
    validateTags(request.getTags());
    try {
      final Set<String> resourceArns = Sets.newHashSet(request.getResourceArns().getMember());
      final List<Loadbalancingv2ResourceName> arns =
          Stream.ofAll(resourceArns).map(Loadbalancingv2ResourceName::parse).toJavaList();
      try (final TransactionResource tx =
               Entities.transactionFor(
                   com.eucalyptus.loadbalancingv2.service.persist.entities.Tag.class)) {
        for (final Loadbalancingv2ResourceName arn : arns) {
          switch(Loadbalancingv2ResourceName.Type.forResourceType(arn.getType()).getOrElseThrow(invalidArn())) {
            case listener:
              addTagsForEntity(listenerNotFound(), Listener.class, arn.getId(), request.getTags());
              break;
            case listener_rule:
              addTagsForEntity(ruleNotFound(), ListenerRule.class, arn.getId(), request.getTags());
              break;
            case loadbalancer:
              addTagsForEntity(loadbalancerNotFound(), LoadBalancer.class, arn.getId(), request.getTags());
              break;
            case targetgroup:
              addTagsForEntity(targetGroupNotFound(), TargetGroup.class, arn.getId(), request.getTags());
              break;
            default:
              throw invalidArn().get();
          }
        }
        tx.commit();
      }
    } catch ( final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
      throw invalidArn().get();
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public CreateListenerResponseType createListener(final CreateListenerType request) throws Loadbalancingv2Exception {
    final CreateListenerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final Loadbalancingv2ResourceName arn;
    try {
      arn = Loadbalancingv2ResourceName.parse(request.getLoadBalancerArn(), Loadbalancingv2ResourceName.Type.loadbalancer);
    } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
      throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid loadbalancer ARN");
    }

    final OwnerFullName ownerFullName =
        ctx.isAdministrator( ) ?
            AccountFullName.getInstance(arn.getNamespace()) :
            ctx.getAccount();

    final Integer port = request.getPort();
    final Listener.Protocol protocol = request.getProtocol()==null ?
        null :
        get(Enums.getIfPresent(Listener.Protocol.class, request.getProtocol()), protocolUnsupported());

    final String certificateArn;
    if (protocol != null && protocol.requiresCertificate()) {
      CertificateList certificateList = request.getCertificates();
      if (certificateList==null ||
          certificateList.getMember().size()!=1 ||
          certificateList.getMember().get(0).getCertificateArn()==null) {
        throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "One certificate ARN required");
      }
      certificateArn = certificateList.getMember().get(0).getCertificateArn();
    } else {
      certificateArn = null;
    }
    validateTags(request.getTags());

    final Set<String> targetGroupArns = Sets.newHashSet();
    for (final Action action : request.getDefaultActions().getMember()) {
      Option.of(action.getTargetGroupArn()).forEach(targetGroupArns::add);
      Option.of(action.getForwardConfig())
          .map(ForwardActionConfig::getTargetGroups)
          .map(TargetGroupList::getMember)
          .map(Stream::ofAll)
          .getOrElse(Stream.empty())
          .map(TargetGroupTuple::getTargetGroupArn)
          .filter(Objects::nonNull)
          .forEach(targetGroupArns::add);
    }
    final Set<String> targetGroupIds = ids(targetGroupArns,
        Loadbalancingv2ResourceName.Type.targetgroup, arn.getNamespace(),
        targetGroupNotFound());

    final com.eucalyptus.loadbalancingv2.common.msgs.Listener resultListener;
    try {
      if (certificateArn != null) {
        final Ern ern = get(() -> Ern.parse(certificateArn), invalidCertificateArn());
        final String certificateAccount = ern.getAccount();
        final String certificateName = Accounts.getNameFromFullName(ern.getResourceName());
        if (!"iam:server-certificate".equals(ern.getResourceType()) || !ctx.getAccountNumber().equals(certificateAccount)) {
          throw invalidCertificateArn().get();
        }
        final EuareApi euareApi = AsyncProxy.client(EuareApi.class);
        get(() -> euareApi.getServerCertificate(certificateName), certificateNotFound());
      }

      resultListener = loadBalancers.updateByExample(
          LoadBalancer.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          loadbalancer -> {
            final List<TargetGroup> targetGroupList;
            try {
              targetGroupList = targetGroups.list(
                  ownerFullName,
                  Restrictions.in("naturalId", targetGroupIds),
                  Loadbalancingv2Metadatas.filterPrivileged(),
                  Functions.identity());
              if (targetGroupList.size() != targetGroupIds.size()) {
                throw targetGroupNotFound().get();
              }
              targetGroupList.forEach(group -> group.getTargets().forEach(target -> {
                if (target.getTargetHealthState() == Target.State.unused) {
                  Target.State.initial.apply(target);
                }
              }));
            } catch (final Exception ex) {
              throw Exceptions.toUndeclared(ex);
            }

            final Listener listener = Listener.create(loadbalancer, port, protocol);
            listener.setDefaultServerCertificateArn(certificateArn);
            listener.setDefaultActions(JsonEncoding.write(request.getDefaultActions()));
            listener.getTargetGroups().addAll(targetGroupList);
            addTags(Entities.persist(listener), request.getTags());
            loadbalancer.getListeners().add(listener);
            loadbalancer.getTargetGroups().addAll(targetGroupList);
            loadbalancer.setUpdateRequired(true);
            return TypeMappers.transform(
                listener, com.eucalyptus.loadbalancingv2.common.msgs.Listener.class);
          }
      );
    } catch (final Loadbalancingv2MetadataNotFoundException ex) {
      throw loadbalancerNotFound().get();
    } catch (final Exception ex) {
      throw handleException(ex);
    }
    final Listeners listeners = new Listeners();
    listeners.getMember().add(resultListener);
    reply.getCreateListenerResult().setListeners(listeners);

    return reply;
  }

  public CreateLoadBalancerResponseType createLoadBalancer(final CreateLoadBalancerType request) throws Loadbalancingv2Exception {
    final CreateLoadBalancerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    if (request.getName().startsWith("internal-")) {
      throw new Loadbalancingv2ClientException("ValidationError", "Name must not start with 'internal-'");
    }

    final LoadBalancer.Type type =
        Enums.getIfPresent(LoadBalancer.Type.class, Objects.toString(request.getType(), "application"))
            .or(LoadBalancer.Type.application);
    final LoadBalancer.Scheme scheme =
        Enums.getIfPresent(LoadBalancer.Scheme.class, Objects.toString(request.getScheme(), "internet-facing").replace('-', '_'))
            .or(LoadBalancer.Scheme.internet_facing);
    final LoadBalancer.IpAddressType ipAddressType =
        Enums.getIfPresent(LoadBalancer.IpAddressType.class, Objects.toString(request.getIpAddressType(), "ipv4"))
            .or(LoadBalancer.IpAddressType.ipv4);
    validateTags(request.getTags());

    final ComputeApi computeApi = AsyncProxy.client(ComputeApi.class);

    final List<SecurityGroupItemType> securityGroupItems =
        request.getSecurityGroups() != null && !request.getSecurityGroups().getMember().isEmpty()?
            computeApi.describeSecurityGroups(request.getSecurityGroups().getMember()).getSecurityGroupInfo() :
            Collections.emptyList();
    if (securityGroupItems.isEmpty()) {
      throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Invalid security groups");
    }

    final Set<String> subnetIds;
    if (request.getSubnets() != null && request.getSubnetMappings() != null) {
      throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Only one of Subnets or SubnetMappings should be present");
    } else if (request.getSubnets() != null) {
      subnetIds = Stream.ofAll(request.getSubnets().getMember())
          .filter(Objects::nonNull).toJavaSet();
    } else if (request.getSubnetMappings() != null) {
      subnetIds = Stream.ofAll(request.getSubnetMappings().getMember())
          .map(SubnetMapping::getSubnetId).filter(Objects::nonNull).toJavaSet();
      //TODO:STEVE: subnet mapping AllocationIdNotFound
      //TODO:STEVE: InvalidConfigurationRequest The specified mapping private ipv4 is not in the subnet.
    } else {
      throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Either Subnets or SubnetMappings is required");
    }

    final List<SubnetType> subnetItems = !subnetIds.isEmpty() ?
        computeApi.describeSubnets(subnetIds).getSubnetSet().getItem() :
        Collections.emptyList();
    if (subnetItems.isEmpty()) {
      throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Invalid subnets");
    }
    for (final SubnetType subnet : subnetItems) {
      subnetIds.remove(subnet.getSubnetId());
      if (subnet.getAvailableIpAddressCount() != null && subnet.getAvailableIpAddressCount() == 0) {
        throw new Loadbalancingv2ClientException("InvalidSubnet",
            "Invalid subnet, no available ips " + subnet.getSubnetId());
      }
    }
    if (!subnetIds.isEmpty()) {
      throw new Loadbalancingv2ClientException("SubnetNotFound",
          "Subnet not found " + subnetIds);
    }

    final Set<String> vpcIds = Stream.ofAll(subnetItems).map(SubnetType::getVpcId).toJavaSet();
    if (vpcIds.size() != 1) {
      throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Inconsistent subnet vpc");
    }

    final Option<Pair<String, String>> hostedZoneNameAndId =
        LoadBalancingHostedZoneManager.getHostedZoneNameAndId();

    final LoadBalancer loadBalancer;
    try {
      @SuppressWarnings({"Guava", "Convert2Lambda"})
      final Supplier<LoadBalancer> allocator = new Supplier<LoadBalancer>(){
        @Override public LoadBalancer get() {
          final LoadBalancer newLoadBalancer = LoadBalancer.create(
              ctx.getUserFullName(),
              request.getName(),
              type,
              scheme
          );

          newLoadBalancer.setIpAddressType(ipAddressType);
          hostedZoneNameAndId.map(Pair::getRight).forEach(newLoadBalancer::setCanonicalHostedZoneId);
          newLoadBalancer.setSecurityGroupIds(
              Stream.ofAll(securityGroupItems).map(SecurityGroupItemType::getGroupId).toJavaList());
          newLoadBalancer.setSubnetIds(
              Stream.ofAll(subnetItems).map(SubnetType::getSubnetId).toJavaList());
          newLoadBalancer.setVpcId(vpcIds.iterator().next());

          try (final TransactionResource tx = Entities.transactionFor(newLoadBalancer)) {
            final LoadBalancer persisted = loadBalancers.save(newLoadBalancer);
            addTags(persisted, request.getTags());
            tx.commit();
            return persisted;
          } catch (Loadbalancingv2MetadataException e) {
            throw Exceptions.toUndeclared(e);
          }
        }
      };

      loadBalancer = Loadbalancingv2Metadatas.allocateUnitlessResource(allocator);
    } catch (Exception ex) {
      throw handleException("TooManyLoadBalancers",
          ex, __ -> new Loadbalancingv2ClientException("DuplicateLoadBalancerName", "Duplicate name") );
    }

    final com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancers loadBalancers =
        new com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancers();
    loadBalancers.getMember().add(TypeMappers.transform(
        loadBalancer,
        com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancer.class));
    reply.getCreateLoadBalancerResult().setLoadBalancers(loadBalancers);

    return reply;
  }

  public CreateRuleResponseType createRule(final CreateRuleType request) throws Loadbalancingv2Exception {
    final CreateRuleResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final Loadbalancingv2ResourceName arn;
    try {
      arn = Loadbalancingv2ResourceName.parse(request.getListenerArn(), Loadbalancingv2ResourceName.Type.listener);
    } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
      throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid loadbalancer ARN");
    }
    validateTags(request.getTags());

    final OwnerFullName ownerFullName =
        ctx.isAdministrator( ) ?
            AccountFullName.getInstance(arn.getNamespace()) :
            ctx.getAccount();
    final String listenerId = arn.getId(Loadbalancingv2ResourceName.Type.listener);

    final Integer priority = request.getPriority();

    final com.eucalyptus.loadbalancingv2.common.msgs.Rule resultRule;
    try {
      resultRule = loadBalancers.updateByExample(
          LoadBalancer.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          loadbalancer -> {
            final Listener listener =
                loadbalancer.findListener(listenerId).getOrElseThrow(runtime(listenerNotFound()));
            final ListenerRule rule = ListenerRule.create(listener, priority);
            rule.setActions(JsonEncoding.write(request.getActions()));
            rule.setConditions(JsonEncoding.write(request.getConditions()));
            addTags(Entities.persist(rule), request.getTags());
            listener.getListenerRules().add(rule);
            listener.updateTimeStamps();
            loadbalancer.updateTimeStamps();
            return TypeMappers.transform(
                rule, com.eucalyptus.loadbalancingv2.common.msgs.Rule.class);
          }
      );
    } catch (final Exception ex) {
      if(Exceptions.isCausedBy(ex, Loadbalancingv2MetadataNotFoundException.class)) {
        throw listenerNotFound().get( );
      }
      throw handleException(ex);
    }
    final Rules rules = new Rules();
    rules.getMember().add(resultRule);
    reply.getCreateRuleResult().setRules(rules);

    return reply;
  }

  public CreateTargetGroupResponseType createTargetGroup(final CreateTargetGroupType request) throws Loadbalancingv2Exception {
    final CreateTargetGroupResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final TargetGroup.TargetType targetType =
        Enums.getIfPresent(TargetGroup.TargetType.class, Objects.toString(request.getTargetType(), "instance"))
            .or(TargetGroup.TargetType.instance);
    final TargetGroup.Protocol protocol = request.getProtocol()==null ?
        null :
        Enums.getIfPresent(TargetGroup.Protocol.class, request.getProtocol()).orNull();
    final TargetGroup.ProtocolVersion protocolVersion = request.getProtocolVersion()==null ?
        null :
        Enums.getIfPresent(TargetGroup.ProtocolVersion.class, request.getProtocolVersion())
            .or(TargetGroup.ProtocolVersion.HTTP1);
    final Integer port = request.getPort();
    validateTags(request.getTags());

    final ComputeApi computeApi = AsyncProxy.client(ComputeApi.class);
    final String vpcId = request.getVpcId();
    if (vpcId != null) {
      final List<VpcType> vpcItems = computeApi.describeVpcs(vpcId).getVpcSet().getItem();
      if (vpcItems.size() != 1) {
        throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Invalid vpc");
      }
    }

    final TargetGroup targetGroup;
    try {
      @SuppressWarnings({"Guava", "Convert2Lambda"})
      final Supplier<TargetGroup> allocator = new Supplier<TargetGroup>(){
        @Override public TargetGroup get() {
          final TargetGroup newTargetGroup = TargetGroup.create(
              ctx.getUserFullName(),
              request.getName(),
              targetType,
              vpcId,
              protocol,
              protocolVersion,
              port
          );

          try (final TransactionResource tx = Entities.transactionFor(newTargetGroup)) {
            final TargetGroup persisted = targetGroups.save(newTargetGroup);
            addTags(persisted, request.getTags());
            tx.commit();
            return persisted;
          } catch (Loadbalancingv2MetadataException e) {
            throw Exceptions.toUndeclared(e);
          }
        }
      };

      targetGroup = Loadbalancingv2Metadatas.allocateUnitlessResource(allocator);
    } catch (Exception ex) {
      throw handleException("TooManyTargetGroups",
          ex, __ -> new Loadbalancingv2ClientException("DuplicateTargetGroupName", "Duplicate name") );
    }

    final com.eucalyptus.loadbalancingv2.common.msgs.TargetGroups targetGroups =
        new com.eucalyptus.loadbalancingv2.common.msgs.TargetGroups();
    targetGroups.getMember().add(TypeMappers.transform(
        targetGroup,
        com.eucalyptus.loadbalancingv2.common.msgs.TargetGroup.class));
    reply.getCreateTargetGroupResult().setTargetGroups(targetGroups);

    return reply;
  }

  public DeleteListenerResponseType deleteListener(final DeleteListenerType request) throws Loadbalancingv2Exception {
    final DeleteListenerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final Loadbalancingv2ResourceName arn;
    try {
      arn = Loadbalancingv2ResourceName.parse(request.getListenerArn(), Loadbalancingv2ResourceName.Type.listener);
    } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
      throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid listener ARN");
    }

    final OwnerFullName ownerFullName =
        ctx.isAdministrator( ) ?
            AccountFullName.getInstance(arn.getNamespace()) :
            ctx.getAccount();
    final String listenerId = arn.getId(Loadbalancingv2ResourceName.Type.listener);

    try {
      loadBalancers.updateByExample(
          LoadBalancer.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          loadbalancer -> {
            final Listener listener =
                loadbalancer.findListener(listenerId).getOrElseThrow(runtime(listenerNotFound()));
            Entities.delete(listener);
            loadbalancer.getListeners().remove(listener);
            loadbalancer.updateTimeStamps();
            return null;
          }
      );
    } catch (final Exception ex) {
      if(Exceptions.isCausedBy(ex, Loadbalancingv2MetadataNotFoundException.class)) {
        throw listenerNotFound().get();
      }
      throw handleException(ex);
    }
    return reply;
  }

  public DeleteLoadBalancerResponseType deleteLoadBalancer(final DeleteLoadBalancerType request) throws Loadbalancingv2Exception {
    final DeleteLoadBalancerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final Loadbalancingv2ResourceName arn;
      try {
        arn = Loadbalancingv2ResourceName.parse(request.getLoadBalancerArn(), Loadbalancingv2ResourceName.Type.loadbalancer);
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid loadbalancer ARN");
      }
      final OwnerFullName ownerFullName =
          ctx.isAdministrator( ) ?
              AccountFullName.getInstance(arn.getNamespace()) :
              ctx.getAccount();

      loadBalancers.updateByExample(
          LoadBalancer.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          loadbalancer -> {
            loadbalancer.setState(LoadBalancer.State.deleted); //TODO:STEVE: need an in-use check?
            return null;
          }
      );
    } catch ( final Loadbalancingv2MetadataNotFoundException e ) {
      throw loadbalancerNotFound().get();
    } catch ( final Exception e ) {
      handleException( e, __ -> new Loadbalancingv2ClientException("ResourceInUse", "Loadbalancer in use") );
    }
    return reply;
  }

  public DeleteRuleResponseType deleteRule(final DeleteRuleType request) throws Loadbalancingv2Exception {
    final DeleteRuleResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final Loadbalancingv2ResourceName arn;
    try {
      arn = Loadbalancingv2ResourceName.parse(request.getRuleArn(), Loadbalancingv2ResourceName.Type.listener_rule);
    } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
      throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid listener rule ARN");
    }

    final OwnerFullName ownerFullName =
        ctx.isAdministrator( ) ?
            AccountFullName.getInstance(arn.getNamespace()) :
            ctx.getAccount();
    final String listenerId = arn.getId(Loadbalancingv2ResourceName.Type.listener);
    final String ruleId = arn.getId(Loadbalancingv2ResourceName.Type.listener_rule);

    try {
      loadBalancers.updateByExample(
          LoadBalancer.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          loadbalancer -> {
            final Listener listener = loadbalancer.findListener(listenerId).getOrElseThrow(runtime(ruleNotFound()));
            final ListenerRule rule = listener.findListenerRule(ruleId).getOrElseThrow(runtime(ruleNotFound()));
            Entities.delete(rule);
            listener.getListenerRules().remove(rule);
            listener.updateTimeStamps();
            loadbalancer.updateTimeStamps();
            return null;
          }
      );
    } catch (final Exception ex) {
      if(Exceptions.isCausedBy(ex, Loadbalancingv2MetadataNotFoundException.class)) {
        throw ruleNotFound().get();
      }
      throw handleException(ex);
    }
    return reply;
  }

  public DeleteTargetGroupResponseType deleteTargetGroup(final DeleteTargetGroupType request) throws Loadbalancingv2Exception {
    final DeleteTargetGroupResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    try {
      final Loadbalancingv2ResourceName arn;
      try {
        arn = Loadbalancingv2ResourceName.parse(request.getTargetGroupArn(), Loadbalancingv2ResourceName.Type.targetgroup);
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid target group ARN");
      }
      final OwnerFullName ownerFullName =
          ctx.isAdministrator( ) ?
              AccountFullName.getInstance(arn.getNamespace()) :
              ctx.getAccount();
      final TargetGroup targetGroup =
          targetGroups.lookupByName(
              ownerFullName,
              arn.getName(),
              Loadbalancingv2Metadatas.filterPrivileged(),
              Functions.identity());
      targetGroups.delete( targetGroup );
    } catch ( final Loadbalancingv2MetadataNotFoundException e ) {
      throw targetGroupNotFound().get();
    } catch ( final Exception e ) {
      handleException( e, __ -> new Loadbalancingv2ClientException("ResourceInUse", "Target group in use") );
    }
    return reply;
  }

  public DeregisterTargetsResponseType deregisterTargets(final DeregisterTargetsType request) throws Loadbalancingv2Exception {
    final DeregisterTargetsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final List<TargetDescription> targetDescriptions = request.getTargets().getMember();
    try {
      final Loadbalancingv2ResourceName arn;
      try {
        arn = Loadbalancingv2ResourceName.parse(request.getTargetGroupArn(), Loadbalancingv2ResourceName.Type.targetgroup);
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid target group ARN");
      }
      final OwnerFullName ownerFullName =
          ctx.isAdministrator( ) ?
              AccountFullName.getInstance(arn.getNamespace()) :
              ctx.getAccount();

      targetGroups.updateByExample(
          TargetGroup.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          targetGroup -> {
            for (final TargetDescription description : targetDescriptions) {
              final Target target = targetGroup.findTarget(description.getId()) //TODO:STEVE: port should be checked here too
                  .getOrElseThrow(runtime(targetInvalid()));
              Entities.delete(target);
              targetGroup.getTargets().remove(target);
            }
            targetGroup.updateTimeStamps();
            return null;
          });
    } catch ( final Loadbalancingv2MetadataNotFoundException e ) {
      throw targetGroupNotFound().get();
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeAccountLimitsResponseType describeAccountLimits(final DescribeAccountLimitsType request) {
    return request.getReply();
  }

  public DescribeListenerCertificatesResponseType describeListenerCertificates(final DescribeListenerCertificatesType request) {
    return request.getReply();
  }

  public DescribeListenersResponseType describeListeners(final DescribeListenersType request) throws Loadbalancingv2Exception {
    final DescribeListenersResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final Loadbalancingv2ResourceName loadbalancerArn;
    if (request.getLoadBalancerArn() != null) {
      try {
        loadbalancerArn = Loadbalancingv2ResourceName.parse(
            request.getLoadBalancerArn(), Loadbalancingv2ResourceName.Type.loadbalancer);
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid loadbalancer ARN");
      }
    } else {
      loadbalancerArn = null;
    }

    final List<Loadbalancingv2ResourceName> listenerArns = Lists.newArrayList();
    if (request.getListenerArns() != null) {
      try {
        for (final String listenerArn : request.getListenerArns().getMember()) {
          listenerArns.add(Loadbalancingv2ResourceName.parse(
              listenerArn, Loadbalancingv2ResourceName.Type.listener));
        }
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid listener ARN");
      }
    }

    final CompatFunction<Loadbalancingv2ResourceName,OwnerFullName> ownerFn = arn ->
        ctx.isAdministrator() ? AccountFullName.getInstance(arn.getNamespace()) : ctx.getAccount();
    try {
      final com.eucalyptus.loadbalancingv2.common.msgs.Listeners resultListeners =
          new com.eucalyptus.loadbalancingv2.common.msgs.Listeners();

      Collection<com.eucalyptus.loadbalancingv2.common.msgs.Listener> listeners =
          Collections.emptyList();
      if (!listenerArns.isEmpty()) {
        final OwnerFullName ownerFullName = ownerFn.apply(listenerArns.get(0));
        final List<String> listenerIds = Stream.ofAll(listenerArns)
            .map(Loadbalancingv2ResourceName.Type.listener.id())
            .toJavaList();
        try (final TransactionResource tx = Entities.transactionFor(Listener.class)) {
          final List<Listener> listenerEntities = Entities.criteriaQuery(Listener.class)
              .whereEqual(Listener_.ownerAccountNumber, ownerFullName.getAccountNumber())
              .whereRestriction(builder -> builder.in(Listener_.naturalId, listenerIds))
              .list();
          listeners = Stream.ofAll(listenerEntities)
              .filter(Loadbalancingv2Metadatas.filterPrivileged())
              .map(TypeMappers.lookupF(ListenerView.class,com.eucalyptus.loadbalancingv2.common.msgs.Listener.class))
              .toJavaList();
        }
      } else if (loadbalancerArn != null) {
        final OwnerFullName ownerFullName =  ownerFn.apply(loadbalancerArn);
        listeners = loadBalancers.lookupByName(
            ownerFullName,
            loadbalancerArn.getName(),
            Predicates.alwaysTrue(),
            loadbalancer -> Stream.ofAll(loadbalancer.getListeners())
                .filter(Loadbalancingv2Metadatas.filterPrivileged())
                .map(TypeMappers.lookupF(ListenerView.class,com.eucalyptus.loadbalancingv2.common.msgs.Listener.class))
                .toJavaList());
      }
      resultListeners.getMember().addAll(listeners);
      reply.getDescribeListenersResult().setListeners(resultListeners);
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeLoadBalancerAttributesResponseType describeLoadBalancerAttributes(
      final DescribeLoadBalancerAttributesType request
  ) throws Loadbalancingv2Exception {
    final DescribeLoadBalancerAttributesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final Loadbalancingv2ResourceName loadbalancerArn;
    try {
      loadbalancerArn = Loadbalancingv2ResourceName.parse(
          request.getLoadBalancerArn(), Loadbalancingv2ResourceName.Type.loadbalancer);
    } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
      throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid loadbalancer ARN");
    }

    final OwnerFullName ownerFullName = ctx.isAdministrator() ?
        AccountFullName.getInstance(loadbalancerArn.getNamespace()) :
        ctx.getAccount();

    try {
      reply.getDescribeLoadBalancerAttributesResult().setAttributes(
          loadBalancers.lookupByName(
              ownerFullName,
              loadbalancerArn.getName(),
              Loadbalancingv2Metadatas.filterPrivileged(),
              TypeMappers.lookup(LoadBalancer.class, LoadBalancerAttributes.class))
      );
    } catch (final Loadbalancingv2MetadataNotFoundException ex) {
      throw loadbalancerNotFound().get();
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeLoadBalancersResponseType describeLoadBalancers(final DescribeLoadBalancersType request) throws Loadbalancingv2Exception {
    final DescribeLoadBalancersResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.getNames()!=null
        && ctx.isAdministrator( )
        && request.getNames().getMember().remove("verbose");
    final OwnerFullName ownerFullName = showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancers resultLoadBalancers =
          new com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancers();
      final Conjunction conjunction = Restrictions.conjunction();
      if (request.getNames() != null) {
        conjunction.add(Restrictions.in("displayName", request.getNames().getMember()));
      }
      if (request.getLoadBalancerArns() != null) {
        conjunction.add(Restrictions.in("naturalId",
            ids(request.getLoadBalancerArns().getMember(),
                Loadbalancingv2ResourceName.Type.loadbalancer)));
      }
      resultLoadBalancers.getMember().addAll( loadBalancers.list(
          ownerFullName,
          conjunction,
          Loadbalancingv2Metadatas.filterPrivileged( ),
          TypeMappers.lookup(
              LoadBalancerView.class,
              com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancer.class ) ) );
      reply.getDescribeLoadBalancersResult().setLoadBalancers(resultLoadBalancers);
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeRulesResponseType describeRules(final DescribeRulesType request) throws Loadbalancingv2Exception {
    final DescribeRulesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final Loadbalancingv2ResourceName listenerArn;
    if (request.getListenerArn() != null) {
      try {
        listenerArn = Loadbalancingv2ResourceName.parse(
            request.getListenerArn(), Loadbalancingv2ResourceName.Type.listener);
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid listener ARN");
      }
    } else {
      listenerArn = null;
    }

    final List<Loadbalancingv2ResourceName> ruleArns = Lists.newArrayList();
    if (request.getRuleArns() != null) {
      try {
        for (final String ruleArn : request.getRuleArns().getMember()) {
          ruleArns.add(Loadbalancingv2ResourceName.parse(
              ruleArn, Loadbalancingv2ResourceName.Type.listener_rule));
        }
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid listener rule ARN");
      }
    }

    final CompatFunction<Loadbalancingv2ResourceName,OwnerFullName> ownerFn = arn ->
        ctx.isAdministrator() ? AccountFullName.getInstance(arn.getNamespace()) : ctx.getAccount();
    try {
      final com.eucalyptus.loadbalancingv2.common.msgs.Rules resultRules =
          new com.eucalyptus.loadbalancingv2.common.msgs.Rules();

      Collection<com.eucalyptus.loadbalancingv2.common.msgs.Rule> rules =
          Collections.emptyList();
      if (!ruleArns.isEmpty()) {
        final OwnerFullName ownerFullName = ownerFn.apply(ruleArns.get(0));
        final List<String> listenerRuleIds = Stream.ofAll(ruleArns)
            .map(Loadbalancingv2ResourceName.Type.listener_rule.id())
            .toJavaList();
        try (final TransactionResource tx = Entities.transactionFor(Listener.class)) {
          final List<ListenerRule> listenerRuleEntities = Entities.criteriaQuery(ListenerRule.class)
              .whereEqual(ListenerRule_.ownerAccountNumber, ownerFullName.getAccountNumber())
              .whereRestriction(builder -> builder.in(ListenerRule_.naturalId, listenerRuleIds))
              .list();
          rules = Stream.ofAll(listenerRuleEntities)
              .filter(Loadbalancingv2Metadatas.filterPrivileged())
              .map(TypeMappers.lookupF(ListenerRuleView.class,com.eucalyptus.loadbalancingv2.common.msgs.Rule.class))
              .toJavaList();
        }
      } else if (listenerArn != null) {
        final OwnerFullName ownerFullName = ownerFn.apply(listenerArn);
        final String listenerId = listenerArn.getId(Loadbalancingv2ResourceName.Type.listener);
        rules = loadBalancers.lookupByName(
            ownerFullName,
            listenerArn.getName(),
            Predicates.alwaysTrue(),
            loadbalancer ->
                Stream.ofAll(loadbalancer.findListener(listenerId).getOrElseThrow(runtime(listenerNotFound())).getListenerRules())
                .filter(Loadbalancingv2Metadatas.filterPrivileged( ))
                .map(TypeMappers.lookupF(ListenerRuleView.class,com.eucalyptus.loadbalancingv2.common.msgs.Rule.class))
                .toJavaList());
      }
      resultRules.getMember().addAll(rules);
      reply.getDescribeRulesResult().setRules(resultRules);
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeSSLPoliciesResponseType describeSSLPolicies(final DescribeSSLPoliciesType request) {
    return request.getReply();
  }

  public DescribeTagsResponseType describeTags(final DescribeTagsType request) throws Loadbalancingv2Exception {
    final DescribeTagsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName().asAccountFullName();
    try {
      final Conjunction conjunction = Restrictions.conjunction();
      if (request.getResourceArns() != null) {
        conjunction.add(Restrictions.in(
            "resourceArn", Sets.newHashSet(request.getResourceArns().getMember())));
      }
      final List<TagDescription> tagDescriptionList = tags.list(
          ownerFullName,
          conjunction,
          Loadbalancingv2Metadatas.filterPrivileged(),
          TypeMappers.lookup(TagView.class, TagDescription.class ));
      final TagDescriptions tagDescriptions = new TagDescriptions();
      tagDescriptions.getMember().addAll(Tags.merge(tagDescriptionList));
      reply.getDescribeTagsResult().setTagDescriptions(tagDescriptions);
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeTargetGroupAttributesResponseType describeTargetGroupAttributes(
      final DescribeTargetGroupAttributesType request
  ) throws Loadbalancingv2Exception {
    final DescribeTargetGroupAttributesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final Loadbalancingv2ResourceName targetGroupArn;
    try {
      targetGroupArn = Loadbalancingv2ResourceName.parse(
          request.getTargetGroupArn(), Loadbalancingv2ResourceName.Type.targetgroup);
    } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
      throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid target group ARN");
    }

    final OwnerFullName ownerFullName = ctx.isAdministrator() ?
        AccountFullName.getInstance(targetGroupArn.getNamespace()) :
        ctx.getAccount();

    try {
      reply.getDescribeTargetGroupAttributesResult().setAttributes(
          targetGroups.lookupByName(
              ownerFullName,
              targetGroupArn.getName(),
              Loadbalancingv2Metadatas.filterPrivileged(),
              TypeMappers.lookup(TargetGroup.class, TargetGroupAttributes.class))
      );
    } catch (final Loadbalancingv2MetadataNotFoundException ex) {
      throw targetGroupNotFound().get();
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeTargetGroupsResponseType describeTargetGroups(final DescribeTargetGroupsType request) throws Loadbalancingv2Exception {
    final DescribeTargetGroupsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final boolean showAll = request.getNames()!=null
        && ctx.isAdministrator( )
        && request.getNames().getMember().remove("verbose");
    final OwnerFullName ownerFullName = showAll ?
        null :
        ctx.getUserFullName( ).asAccountFullName( );

    try {
      final com.eucalyptus.loadbalancingv2.common.msgs.TargetGroups resultTargetGroups =
          new com.eucalyptus.loadbalancingv2.common.msgs.TargetGroups();
      if (request.getLoadBalancerArn() != null) {
        final Loadbalancingv2ResourceName arn;
        try {
          arn = Loadbalancingv2ResourceName.parse(request.getLoadBalancerArn(), Loadbalancingv2ResourceName.Type.loadbalancer);
        } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
          throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid loadbalancer ARN");
        }
        resultTargetGroups.getMember().addAll( loadBalancers.lookupByExample(
            LoadBalancer.named(ownerFullName, arn.getName()),
            ownerFullName,
            arn.getName(),
            Loadbalancingv2Metadatas.filterPrivileged( ),
            loadBalancer ->
                Stream.ofAll(loadBalancer.getTargetGroups())
                    .filter(group ->
                        request.getTargetGroupArns() == null ||
                        request.getTargetGroupArns().getMember().contains(group.getArn()))
                    .map(TypeMappers.lookupF(
                        TargetGroupView.class,
                        com.eucalyptus.loadbalancingv2.common.msgs.TargetGroup.class ))
                    .toJavaList()));
      } else {
        final Conjunction conjunction = Restrictions.conjunction();
        if (request.getNames() != null) {
          conjunction.add(Restrictions.in("displayName", request.getNames().getMember()));
        }
        if (request.getTargetGroupArns() != null) {
          conjunction.add(Restrictions.in("naturalId",
              ids(request.getTargetGroupArns().getMember(),
                  Loadbalancingv2ResourceName.Type.targetgroup)));
        }
        resultTargetGroups.getMember().addAll( targetGroups.list(
            ownerFullName,
            conjunction,
            Loadbalancingv2Metadatas.filterPrivileged( ),
            TypeMappers.lookup(
                TargetGroupView.class,
                com.eucalyptus.loadbalancingv2.common.msgs.TargetGroup.class ) ) );
      }
      reply.getDescribeTargetGroupsResult().setTargetGroups(resultTargetGroups);
    } catch ( Loadbalancingv2MetadataNotFoundException e ) {
      throw loadbalancerNotFound().get(); // Load balancer looked up by arn
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public DescribeTargetHealthResponseType describeTargetHealth(final DescribeTargetHealthType request) throws Loadbalancingv2Exception {
    final DescribeTargetHealthResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );

    final List<TargetDescription> targetDescriptions = request.getTargets()==null ?
        Collections.emptyList() :
        request.getTargets().getMember();
    try {
      final Loadbalancingv2ResourceName arn;
      try {
        arn = Loadbalancingv2ResourceName.parse(request.getTargetGroupArn(), Loadbalancingv2ResourceName.Type.targetgroup);
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid target group ARN");
      }

      final OwnerFullName ownerFullName =
          ctx.isAdministrator( ) ?
              AccountFullName.getInstance(arn.getNamespace()) :
              ctx.getAccount();

      final com.eucalyptus.loadbalancingv2.common.msgs.TargetHealthDescriptions  resultDescriptions =
          new com.eucalyptus.loadbalancingv2.common.msgs.TargetHealthDescriptions();

      final AtomicInteger matchCount = new AtomicInteger(0);
      resultDescriptions.getMember().addAll(targetGroups.lookupByName(
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          targetGroup -> Stream.ofAll(targetGroup.getTargets())
              .filter(targetPredicate(request.getTargets(), matchCount::incrementAndGet))
              .map(TypeMappers.lookupF(TargetView.class,com.eucalyptus.loadbalancingv2.common.msgs.TargetHealthDescription.class))
              .toJavaList()));
      if (request.getTargets() != null && request.getTargets().getMember().size()!=matchCount.get()) {
        throw new Loadbalancingv2ClientException("InvalidTarget", "Invalid target");
      }
      reply.getDescribeTargetHealthResult().setTargetHealthDescriptions(resultDescriptions);
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public ModifyListenerResponseType modifyListener(final ModifyListenerType request) {
    return request.getReply();
  }

  public ModifyLoadBalancerAttributesResponseType modifyLoadBalancerAttributes(
      final ModifyLoadBalancerAttributesType request
  ) throws Loadbalancingv2Exception {
    final ModifyLoadBalancerAttributesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    final Loadbalancingv2ResourceName arn;
    try {
      arn = Loadbalancingv2ResourceName.parse(
          request.getLoadBalancerArn(), Loadbalancingv2ResourceName.Type.loadbalancer);
    } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
      throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid loadbalancer ARN");
    }

    final OwnerFullName ownerFullName =
        ctx.isAdministrator( ) ?
            AccountFullName.getInstance(arn.getNamespace()) :
            ctx.getAccount();

    final Map<String,String> modifiedAttributes = Maps.newHashMap();
    for (final LoadBalancerAttribute attribute : request.getAttributes().getMember()) {
      final String key = attribute.getKey();
      final String value = attribute.getValue();
      if (modifiedAttributes.put(key, value) != null) {
        throw invalidConfiguration().apply("Duplicate attribute key");
      }
    }

    try {
      LoadBalancerAttributes attributes = loadBalancers.updateByExample(
          LoadBalancer.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          loadbalancer -> {
            loadbalancer.getAttributes().putAll(modifiedAttributes);
            LoadBalancingAttribute.validate(
                runtime(invalidConfiguration()),
                Loadbalancingv2Metadata.LoadbalancerMetadata.class,
                loadbalancer.getType(),
                loadbalancer.getAttributes());
            loadbalancer.updateTimeStamps();
            return TypeMappers.transform(loadbalancer, LoadBalancerAttributes.class);
          }
      );
      reply.getModifyLoadBalancerAttributesResult().setAttributes(attributes);
    } catch (final Loadbalancingv2MetadataNotFoundException ex) {
      throw loadbalancerNotFound().get();
    } catch (final Exception ex) {
      throw handleException(ex);
    }
    return reply;
  }

  public ModifyRuleResponseType modifyRule(final ModifyRuleType request) {
    return request.getReply();
  }

  public ModifyTargetGroupResponseType modifyTargetGroup(final ModifyTargetGroupType request) {
    return request.getReply();
  }

  public ModifyTargetGroupAttributesResponseType modifyTargetGroupAttributes(
      final ModifyTargetGroupAttributesType request
  ) throws Loadbalancingv2Exception {
    final ModifyTargetGroupAttributesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();

    final Loadbalancingv2ResourceName arn;
    try {
      arn = Loadbalancingv2ResourceName.parse(
          request.getTargetGroupArn(), Loadbalancingv2ResourceName.Type.targetgroup);
    } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
      throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid target group ARN");
    }

    final OwnerFullName ownerFullName =
        ctx.isAdministrator( ) ?
            AccountFullName.getInstance(arn.getNamespace()) :
            ctx.getAccount();

    final Map<String,String> modifiedAttributes = Maps.newHashMap();
    for (final TargetGroupAttribute attribute : request.getAttributes().getMember()) {
      final String key = attribute.getKey();
      final String value = attribute.getValue();
      if (modifiedAttributes.put(key, value) != null) {
        throw invalidConfiguration().apply("Duplicate attribute key");
      }
    }

    try {
      TargetGroupAttributes attributes = targetGroups.updateByExample(
          TargetGroup.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          targetGroup -> {
            targetGroup.getAttributes().putAll(modifiedAttributes);
            LoadBalancingAttribute.validate(
                runtime(invalidConfiguration()),
                Loadbalancingv2Metadata.TargetgroupMetadata.class,
                null,
                targetGroup.getAttributes());
            targetGroup.updateTimeStamps();
            return TypeMappers.transform(targetGroup, TargetGroupAttributes.class);
          }
      );
      reply.getModifyTargetGroupAttributesResult().setAttributes(attributes);
    } catch (final Loadbalancingv2MetadataNotFoundException ex) {
      throw loadbalancerNotFound().get();
    } catch (final Exception ex) {
      throw handleException(ex);
    }
    return reply;  }

  public RegisterTargetsResponseType registerTargets(final RegisterTargetsType request) throws Loadbalancingv2Exception {
    final RegisterTargetsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final List<TargetDescription> targetDescriptions = request.getTargets().getMember();
    try {
      final Loadbalancingv2ResourceName arn;
      try {
        arn = Loadbalancingv2ResourceName.parse(request.getTargetGroupArn(), Loadbalancingv2ResourceName.Type.targetgroup);
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException ex) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid target group ARN");
      }
      final OwnerFullName ownerFullName =
          ctx.isAdministrator( ) ?
              AccountFullName.getInstance(arn.getNamespace()) :
              ctx.getAccount();

      final Set<String> instanceVpcs = Sets.newHashSet();
      final Map<String,String> ipAddresses = Maps.newHashMap();
      for (final TargetDescription description : targetDescriptions) {
        if (description.getId().startsWith("i-")) {
          // resolve instance ip address
          final ComputeApi ec2 = AsyncProxy.client(ComputeApi.class);
          final DescribeInstancesResponseType response = ec2.describeInstances(
              ComputeApi.filter("instance-id", description.getId())
          );
          if (response.getReservationSet().isEmpty()) {
            throw new Loadbalancingv2ClientException("InvalidTarget", "Instance not found");
          }
          final RunningInstancesItemType instance =
              response.getReservationSet().get(0).getInstancesSet().get(0);
          if (!"running".equals(instance.getStateName())) {
            throw new Loadbalancingv2ClientException("InvalidTarget", "Instance not in running state");
          }
          if (instance.getVpcId()==null || (instanceVpcs.add(instance.getVpcId()) && instanceVpcs.size()!=1)) {
            throw new Loadbalancingv2ClientException("InvalidTarget", "Instance vpc invalid for target");
          }
          ipAddresses.put(
              description.getId(),
              instance.getPrivateIpAddress());
        } else {
          ipAddresses.put(description.getId(), description.getId());
        }
      }

      targetGroups.updateByExample(
          TargetGroup.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          group -> {
            if (!instanceVpcs.isEmpty() && group.getVpcId()!=null && !instanceVpcs.contains(group.getVpcId())) {
              throw Exceptions.toUndeclared(new Loadbalancingv2ClientException("InvalidTarget",
                  "Instance vpc invalid for target"));
            }
            final Target.State state = group.getLoadBalancers().isEmpty() ?
                Target.State.unused :
                Target.State.initial;
            for (final TargetDescription description : targetDescriptions) {
              if (group.findTarget(description.getId()).isEmpty()) {
                final Target target =
                    Target.create(group, description.getId(), ipAddresses.get(description.getId()));
                target.setPort(description.getPort());
                target.setAvailabilityZone(description.getAvailabilityZone());
                state.apply(target);
                group.getTargets().add(target);
              }
            }
            group.updateTimeStamps();
            return null;
          });
    } catch ( final Loadbalancingv2MetadataNotFoundException e ) {
      throw targetGroupNotFound().get();
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public RemoveListenerCertificatesResponseType removeListenerCertificates(final RemoveListenerCertificatesType request) {
    return request.getReply();
  }

  public RemoveTagsResponseType removeTags(final RemoveTagsType request) throws Loadbalancingv2Exception {
    final RemoveTagsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup( );
    final OwnerFullName ownerFullName = ctx.getUserFullName().asAccountFullName();
    try {
      final Set<String> resourceArns = Sets.newHashSet(request.getResourceArns().getMember());
      final Set<String> tagKeys = Sets.newHashSet(request.getTagKeys().getMember());
      try (final TransactionResource tx =
               Entities.transactionFor(com.eucalyptus.loadbalancingv2.service.persist.entities.Tag.class)) {
        final List<com.eucalyptus.loadbalancingv2.service.persist.entities.Tag<?>> tagList = tags.list(
            ownerFullName,
            Restrictions.conjunction(
              Restrictions.in("resourceArn", resourceArns),
              Restrictions.in( "displayName", tagKeys )),
            Loadbalancingv2Metadatas.filterPrivileged(),
            Functions.identity());
        for (final com.eucalyptus.loadbalancingv2.service.persist.entities.Tag<?> tag : tagList) {
          tags.delete(tag);
        }
        tx.commit();
      }
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public SetIpAddressTypeResponseType setIpAddressType(final SetIpAddressTypeType request) {
    return request.getReply();
  }

  public SetRulePrioritiesResponseType setRulePriorities(final SetRulePrioritiesType request) {
    return request.getReply();
  }

  public SetSecurityGroupsResponseType setSecurityGroups(final SetSecurityGroupsType request) {
    return request.getReply();
  }

  public SetSubnetsResponseType setSubnets(final SetSubnetsType request) {
    return request.getReply();
  }

  private static Set<String> ids(
      final Collection<String> arns,
      final Loadbalancingv2ResourceName.Type type
  ) throws Loadbalancingv2ClientException {
    return ids(arns, type, null, null);
  }

  private static Set<String> ids(
      final Collection<String> arns,
      final Loadbalancingv2ResourceName.Type type,
      final String accountNumber,
      final Supplier<Loadbalancingv2ClientException> accountExceptionSupplier
  ) throws Loadbalancingv2ClientException {
    final Set<String> ids = Sets.newLinkedHashSet();
    for ( final String arn : arns ) {
      try {
        final Loadbalancingv2ResourceName resourceName =
            Loadbalancingv2ResourceName.parse(arn, type);
        if (accountNumber != null && !accountNumber.equals(resourceName.getNamespace())) {
          throw accountExceptionSupplier.get();
        }
        ids.add( resourceName.getId(type) );
      } catch (final Loadbalancingv2ResourceName.InvalidResourceNameException e) {
        throw new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid arn");
      }
    }
    return ids;
  }

  private static void validateTags(final TagList tagList) throws Loadbalancingv2ClientException {
    if (tagList != null) {
      validateTags(tagList.getMember(), true,Tag::getKey, Tag::getValue);
    }
  }

  private static void validateTags(final Taggable<?> taggable, boolean checkReserved) throws Loadbalancingv2ClientException {
    validateTags(taggable.getTags(), checkReserved, TagView::getKey, TagView::getValue);
  }

  private static <T> void validateTags(
      final List<T> tagItems,
      final boolean checkReserved,
      final Function<T,String> keyGetter,
      final Function<T,String> valueGetter
  ) throws Loadbalancingv2ClientException {
    final Map<String, String> tags = Maps.newHashMap();
    for (final T tag : tagItems) {
      if (tags.put(keyGetter.apply(tag), Objects.toString(valueGetter.apply(tag), "")) != null) {
        throw new Loadbalancingv2ClientException("DuplicateTagKeys",
            "Duplicate tag key (" + keyGetter.apply(tag) + ")");
      }
    }
    final int reservedTags = Stream.ofAll(tags.keySet())
        .filter(key -> key.startsWith("euca:") || key.startsWith("aws:")).length();
    if (reservedTags > 0 && checkReserved && !Contexts.lookup().isPrivileged()) {
      throw new Loadbalancingv2ClientException("InvalidConfigurationRequest",
          "Invalid tag key (reserved prefix)");
    }
    if ((tags.size() - reservedTags) > LoadBalancingServiceProperties.getMaxTags()) {
      throw Exceptions.toUndeclared(
          new Loadbalancingv2ClientException("TooManyTags", "Tag limit exceeded"));
    }
  }

  private static <RT extends TagView, RTE extends AbstractPersistent & Loadbalancingv2Metadata & Taggable<RT>> void addTagsForEntity(
      final CompatSupplier<Loadbalancingv2ClientException> notFound,
      final Class<RTE> entityClass,
      final String id,
      final TagList tagList) throws Loadbalancingv2Exception {
    try {
      final RTE taggable = Entities.criteriaQuery(entityClass)
          .whereEqual(AbstractPersistent_.naturalId, id).uniqueResult();
      if (!Loadbalancingv2Metadatas.filterPrivileged().test(taggable)) {
        notFound.get();
      }
      final List<RT> tags = taggable.getTags();
      final Map<String,RT> tagMap =
          CollectionUtils.putAll(tags, Maps.newHashMap(), TagView::getKey, Functions.identity());
      for (final Tag tag : tagList.getMember()) {
        if (tagMap.containsKey(tag.getKey())) {
          taggable.updateTag(tagMap.get(tag.getKey()), tag.getValue());
        } else {
          final RT resourceTag = taggable.createTag(tag.getKey(), tag.getValue());
          Entities.persist(resourceTag);
          tags.add(resourceTag);
        }
      }
      validateTags(taggable, false);
    } catch (final NoSuchElementException e) {
      throw notFound.get();
    }
  }

  private static <RT extends TagView> void addTags(final Taggable<RT> resource, final TagList tags) {
    if (tags != null) {
      final List<RT> resourceTags = Lists.newArrayList();
      for (final Tag tag : tags.getMember()) {
        final RT resourceTag =
            resource.createTag(tag.getKey(), Objects.toString(tag.getValue(), ""));
        Entities.persist(resourceTag);
      }
      resource.setTags(resourceTags);
    }
  }

  private static CompatSupplier<Loadbalancingv2ClientException> invalidArn() {
    return () -> new Loadbalancingv2ClientException("InvalidParameterValue", "Invalid ARN");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> loadbalancerNotFound() {
    return () -> new Loadbalancingv2ClientException("LoadBalancerNotFound", "Loadbalancer not found");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> listenerNotFound() {
    return () -> new Loadbalancingv2ClientException("ListenerNotFound", "Listener not found");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> ruleNotFound() {
    return () -> new Loadbalancingv2ClientException("RuleNotFound", "Listener rule not found");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> targetGroupNotFound() {
    return () -> new Loadbalancingv2ClientException("TargetGroupNotFound", "Target group not found");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> targetInvalid() {
    return () -> new Loadbalancingv2ClientException("InvalidTarget", "Target invalid");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> protocolUnsupported() {
    return () -> new Loadbalancingv2ClientException("UnsupportedProtocol", "Protocol not supported");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> certificateNotFound() {
    return () -> new Loadbalancingv2ClientException("CertificateNotFound", "Certificate not found");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> invalidCertificateArn() {
    return () -> new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Invalid certificate ARN");
  }

  private static CompatFunction<String, Loadbalancingv2ClientException> invalidConfiguration() {
    return message -> new Loadbalancingv2ClientException("InvalidConfigurationRequest", message);
  }

  private static CompatFunction<String, RuntimeException> runtime(CompatFunction<String, ? extends Exception> function) {
    return message -> Exceptions.toUndeclared(function.apply(message));
  }
  private static CompatSupplier<RuntimeException> runtime(CompatSupplier<? extends Exception> supplier) {
    return () -> Exceptions.toUndeclared(supplier.get());
  }

  private static <V, T extends Throwable> V get(final Optional<V> optionalValue, Supplier<T> thrown) throws T {
    if (optionalValue == null || !optionalValue.isPresent()) {
      throw thrown.get();
    }
    return optionalValue.get();
  }

  private static <V, T extends Throwable> V get(final Callable<V> callable, Supplier<T> thrown) throws T {
    if (callable == null) {
      throw thrown.get();
    } else {
      try {
        return callable.call();
      } catch (final Exception e) {
        throw thrown.get();
      }
    }
  }

  private static Predicate<Target> targetPredicate(
      @Nullable final TargetDescriptions targetDescriptions,
                final Runnable matchedRunnable
  ) {
    if (targetDescriptions != null) {
      Predicate<Target> resultPredicate = __ -> false;
      for (final TargetDescription targetDescription : targetDescriptions.getMember()) {
        Predicate<Target> predicate = __ -> true;
        final String idMatch = targetDescription.getId();
        final String azMatch = targetDescription.getAvailabilityZone();
        final Integer pMatch = targetDescription.getPort();
        predicate = predicate.and((Target t) -> idMatch.equals(t.getTargetId()));
        if (azMatch != null) {
          predicate = predicate.and((Target t) -> azMatch.equals(t.getAvailabilityZone()));
        } else {
          predicate = predicate.and((Target t) -> t.getAvailabilityZone() == null);
        }
        if (pMatch != null) {
          predicate = predicate.and((Target t) -> pMatch.equals(t.getPort()));
        } else {
          predicate = predicate.and((Target t) -> t.getPort() == null);
        }
        predicate = predicate.and( __ -> { matchedRunnable.run(); return true; });
        resultPredicate = resultPredicate.or(predicate);
      }
      return resultPredicate;
    } else {
      return __ -> true;
    }
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static Loadbalancingv2Exception handleException(
      final String quotaCode,
      final Exception e,
      final NonNullFunction<ConstraintViolationException, Loadbalancingv2Exception> constraintExceptionBuilder
  ) throws Loadbalancingv2Exception {
    final AuthQuotaException quotaCause = Exceptions.findCause( e, AuthQuotaException.class );
    if ( quotaCause != null ) {
      throw new Loadbalancingv2ClientException( quotaCode, "Request would exceed limit" );
    }

    return handleException(e, constraintExceptionBuilder);
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static Loadbalancingv2Exception handleException( final Exception e ) throws Loadbalancingv2Exception {
    return handleException( e,  null );
  }

  /**
   * Method always throws, signature allows use of "throw handleException ..."
   */
  private static Loadbalancingv2Exception handleException(
      final Exception e,
      final NonNullFunction<ConstraintViolationException, Loadbalancingv2Exception> constraintExceptionBuilder
  ) throws Loadbalancingv2Exception {
    final Loadbalancingv2Exception cause = Exceptions.findCause( e, Loadbalancingv2Exception.class );
    if ( cause != null ) {
      throw cause;
    }

    final ConstraintViolationException constraintViolationException =
        Exceptions.findCause( e, ConstraintViolationException.class );
    if ( constraintViolationException != null && constraintExceptionBuilder != null ) {
      throw constraintExceptionBuilder.apply( constraintViolationException );
    }

    logger.error( e, e );

    final Loadbalancingv2ServiceException exception =
        new Loadbalancingv2ServiceException( "InternalFailure", String.valueOf(e.getMessage()) );
    if ( Contexts.lookup( ).hasAdministrativePrivileges() ) {
      exception.initCause( e );
    }
    throw exception;
  }
}

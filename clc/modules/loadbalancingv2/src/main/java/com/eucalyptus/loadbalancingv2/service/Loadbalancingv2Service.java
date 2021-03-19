/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service;

import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.ComputeApi;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancingv2.common.msgs.Listeners;
import com.eucalyptus.loadbalancingv2.common.msgs.Rules;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetDescription;
import com.eucalyptus.loadbalancingv2.service.persist.JsonEncoding;
import com.eucalyptus.loadbalancingv2.service.persist.LoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.Loadbalancingv2MetadataException;
import com.eucalyptus.loadbalancingv2.service.persist.Loadbalancingv2MetadataNotFoundException;
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
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.CompatSupplier;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncProxy;
import com.google.common.base.Enums;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import io.vavr.collection.Stream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;

/**
 *
 */
@ComponentNamed
public class Loadbalancingv2Service {

  private static final Logger logger = Logger.getLogger(Loadbalancingv2Service.class);

  private final LoadBalancers loadBalancers;
  private final TargetGroups targetGroups;

  @Inject
  public Loadbalancingv2Service(
      final LoadBalancers loadBalancers,
      final TargetGroups targetGroups
  ) {
    this.loadBalancers = loadBalancers;
    this.targetGroups = targetGroups;
  }

  public AddListenerCertificatesResponseType addListenerCertificates(final AddListenerCertificatesType request) {
    return request.getReply();
  }

  public AddTagsResponseType addTags(final AddTagsType request) {
    return request.getReply();
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
        Enums.getIfPresent(Listener.Protocol.class, request.getProtocol()).orNull();

    final com.eucalyptus.loadbalancingv2.common.msgs.Listener resultListener;
    try {
      resultListener = loadBalancers.updateByExample(
          LoadBalancer.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          loadbalancer -> {
            final Listener listener = Listener.create(loadbalancer, port, protocol);
            listener.setDefaultActions(JsonEncoding.write(request.getDefaultActions()));
            loadbalancer.getListeners().add(listener);
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

    final LoadBalancer.Type type =
        Enums.getIfPresent(LoadBalancer.Type.class, Objects.toString(request.getType(), "application"))
            .or(LoadBalancer.Type.application);
    final LoadBalancer.Scheme scheme =
        Enums.getIfPresent(LoadBalancer.Scheme.class, Objects.toString(request.getScheme(), "internet-facing").replace('-', '_'))
            .or(LoadBalancer.Scheme.internet_facing);
    final LoadBalancer.IpAddressType ipAddressType =
        Enums.getIfPresent(LoadBalancer.IpAddressType.class, Objects.toString(request.getIpAddressType(), "ipv4"))
            .or(LoadBalancer.IpAddressType.ipv4);

    final ComputeApi computeApi = AsyncProxy.client(ComputeApi.class);

    final List<SecurityGroupItemType> securityGroupItems =
        request.getSecurityGroups() != null && !request.getSecurityGroups().getMember().isEmpty()?
            computeApi.describeSecurityGroups(request.getSecurityGroups().getMember()).getSecurityGroupInfo() :
            Collections.emptyList();
    if (securityGroupItems.isEmpty()) {
      throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Invalid security groups");
    }

    final List<SubnetType> subnetItems =
        request.getSubnets() != null && !request.getSubnets().getMember().isEmpty()?
            computeApi.describeSubnets(request.getSubnets().getMember()).getSubnetSet().getItem() :
            Collections.emptyList();
    if (subnetItems.isEmpty()) {
      throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Invalid subnets");
    }

    final Set<String> vpcIds = Stream.ofAll(subnetItems).map(SubnetType::getVpcId).toJavaSet();
    if (vpcIds.size() != 1) {
      throw new Loadbalancingv2ClientException("InvalidConfigurationRequest", "Inconsistent subnet vpc");
    }

    final LoadBalancer loadBalancer;
    try {
      @SuppressWarnings({"Guava", "Convert2Lambda"})
      final Supplier<LoadBalancer> allocator = new Supplier<LoadBalancer>(){
        @Override public LoadBalancer get() {
          final LoadBalancer newLoadBalancer = LoadBalancer.create(
              ctx.getUserFullName(),
              request.getName(),  //TODO:STEVE: validation
              type,
              scheme
          );

          newLoadBalancer.setIpAddressType(ipAddressType);
          newLoadBalancer.setSecurityGroupIds(
              Stream.ofAll(securityGroupItems).map(SecurityGroupItemType::getGroupId).toJavaList());
          newLoadBalancer.setSubnetIds(
              Stream.ofAll(subnetItems).map(SubnetType::getSubnetId).toJavaList());
          newLoadBalancer.setVpcId(vpcIds.iterator().next());

          try {
            return loadBalancers.save(newLoadBalancer);
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

    final TargetGroup targetGroup;
    try {
      @SuppressWarnings({"Guava", "Convert2Lambda"})
      final Supplier<TargetGroup> allocator = new Supplier<TargetGroup>(){
        @Override public TargetGroup get() {
          final TargetGroup newTargetGroup = TargetGroup.create(
              ctx.getUserFullName(),
              request.getName(),  // maximum of 32 characters, must contain only alphanumeric characters or hyphens, and must not begin or end with a hyphen.
              targetType,
              request.getVpcId(),  //TODO:STEVE: validate vpc etc
              protocol,
              protocolVersion,
              port
          );

          try {
            return targetGroups.save(newTargetGroup);
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
      throw listenerNotFound().get();
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
      throw new Loadbalancingv2ClientException( "TargetGroupNotFound", "Target group not found" );
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
              final Target target = targetGroup.findTarget(description.getId())
                  .getOrElseThrow(runtime(targetInvalid()));
              Entities.delete(target);
              targetGroup.getTargets().remove(target);
            }
            targetGroup.updateTimeStamps();
            return null;
          });
    } catch ( final Loadbalancingv2MetadataNotFoundException e ) {
      throw new Loadbalancingv2ClientException( "TargetGroupNotFound", "Target group not found" );
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

  public DescribeLoadBalancerAttributesResponseType describeLoadBalancerAttributes(final DescribeLoadBalancerAttributesType request) {
    return request.getReply();
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
      resultLoadBalancers.getMember().addAll( loadBalancers.listByExample(
          LoadBalancer.named(ownerFullName, null),
          Loadbalancingv2Metadatas.filterPrivileged( ), //TODO:STEVE: filtering on request properties
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

  public DescribeTagsResponseType describeTags(final DescribeTagsType request) {
    return request.getReply();
  }

  public DescribeTargetGroupAttributesResponseType describeTargetGroupAttributes(final DescribeTargetGroupAttributesType request) {
    return request.getReply();
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
      resultTargetGroups.getMember().addAll( targetGroups.listByExample(
          TargetGroup.named(ownerFullName, null),
          Loadbalancingv2Metadatas.filterPrivileged( ), //TODO:STEVE: filter by target group name/arn and loadBalancerArn
          TypeMappers.lookup(
              TargetGroupView.class,
              com.eucalyptus.loadbalancingv2.common.msgs.TargetGroup.class ) ) );
      reply.getDescribeTargetGroupsResult().setTargetGroups(resultTargetGroups);
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

      resultDescriptions.getMember().addAll(targetGroups.lookupByName(
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          targetGroup -> Stream.ofAll(targetGroup.getTargets())
              // TODO:STEVE: filter based on targetDescriptions
              .map(TypeMappers.lookupF(TargetView.class,com.eucalyptus.loadbalancingv2.common.msgs.TargetHealthDescription.class))
              .toJavaList()));
      reply.getDescribeTargetHealthResult().setTargetHealthDescriptions(resultDescriptions);
    } catch ( Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public ModifyListenerResponseType modifyListener(final ModifyListenerType request) {
    return request.getReply();
  }

  public ModifyLoadBalancerAttributesResponseType modifyLoadBalancerAttributes(final ModifyLoadBalancerAttributesType request) {
    return request.getReply();
  }

  public ModifyRuleResponseType modifyRule(final ModifyRuleType request) {
    return request.getReply();
  }

  public ModifyTargetGroupResponseType modifyTargetGroup(final ModifyTargetGroupType request) {
    return request.getReply();
  }

  public ModifyTargetGroupAttributesResponseType modifyTargetGroupAttributes(final ModifyTargetGroupAttributesType request) {
    return request.getReply();
  }

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

      //TODO:STEVE: instance targets must be running when registered

      targetGroups.updateByExample(
          TargetGroup.named(ownerFullName, arn.getName()),
          ownerFullName,
          arn.getName(),
          Loadbalancingv2Metadatas.filterPrivileged(),
          group -> {
            for (final TargetDescription description : targetDescriptions) {
              if (group.findTarget(description.getId()).isEmpty()) {
                final Target target = Target.create(group, description.getId());
                target.setPort(description.getPort());
                target.setAvailabilityZone(description.getAvailabilityZone());
                group.getTargets().add(target);
              }
            }
            group.updateTimeStamps();
            return null;
          });
    } catch ( final Loadbalancingv2MetadataNotFoundException e ) {
      throw new Loadbalancingv2ClientException( "TargetGroupNotFound", "Target group not found" );
    } catch ( final Exception e ) {
      throw handleException( e );
    }
    return reply;
  }

  public RemoveListenerCertificatesResponseType removeListenerCertificates(final RemoveListenerCertificatesType request) {
    return request.getReply();
  }

  public RemoveTagsResponseType removeTags(final RemoveTagsType request) {
    return request.getReply();
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

  private static CompatSupplier<Loadbalancingv2ClientException> loadbalancerNotFound() {
    return () -> new Loadbalancingv2ClientException("LoadBalancerNotFound", "Loadbalancer not found");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> listenerNotFound() {
    return () -> new Loadbalancingv2ClientException("ListenerNotFound", "Listener not found");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> ruleNotFound() {
    return () -> new Loadbalancingv2ClientException("RuleNotFound", "Listener rule not found");
  }

  private static CompatSupplier<Loadbalancingv2ClientException> targetInvalid() {
    return () -> new Loadbalancingv2ClientException("InvalidTarget", "Target invalid");
  }

  private static CompatSupplier<RuntimeException> runtime(CompatSupplier<? extends Exception> supplier) {
    return () -> Exceptions.toUndeclared(supplier.get());
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

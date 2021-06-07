/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

package com.eucalyptus.loadbalancing.service;

import static com.eucalyptus.loadbalancing.LoadBalancerHelper.findListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;

import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.loadbalancing.*;
import com.eucalyptus.loadbalancing.activities.LoadBalancerVersionException;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.loadbalancing.service.persist.ImmutableLoadBalancingPersistence;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancerSecurityGroups;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancers;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancingMetadataException;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancingMetadataNotFoundException;
import com.eucalyptus.loadbalancing.service.persist.LoadBalancingPersistence;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendInstance;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerPolicyDescription;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerSecurityGroupRef;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerZone;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerZone.STATE;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerBackendInstanceView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerListenerView;
import com.eucalyptus.loadbalancing.service.persist.views.ImmutableLoadBalancerZoneView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerBackendInstanceView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerBackendServerDescriptionFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerListenerFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerListenerView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerListenersView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerPolicyAttributeDescriptionView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerPolicyDescriptionFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerPolicyDescriptionView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerPolicyTypeDescriptionFullView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerSecurityGroupRefView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerSecurityGroupView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerZoneView;
import com.eucalyptus.loadbalancing.service.persist.views.LoadBalancerZonesView;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingWorkflowException;
import com.eucalyptus.system.Threads;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.AuthQuotaException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerBackendServerDescription;
import com.eucalyptus.loadbalancing.LoadBalancerBackendServerHelper;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerListener;
import com.eucalyptus.loadbalancing.service.persist.entities.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerDeploymentVersion;
import com.eucalyptus.loadbalancing.LoadBalancerHelper;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.common.LoadBalancingMetadatas;
import com.eucalyptus.loadbalancing.common.msgs.AddTagsResponseType;
import com.eucalyptus.loadbalancing.common.msgs.AddTagsType;
import com.eucalyptus.loadbalancing.common.msgs.ApplySecurityGroupsToLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.ApplySecurityGroupsToLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.AttachLoadBalancerToSubnetsResponseType;
import com.eucalyptus.loadbalancing.common.msgs.AttachLoadBalancerToSubnetsType;
import com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckResponseType;
import com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckType;
import com.eucalyptus.loadbalancing.common.msgs.CreateAppCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateAppCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLBCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLBCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerListenersResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerListenersType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerListenersResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerListenersType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerAttributesResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerAttributesType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPoliciesResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPoliciesType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPolicyTypesResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPolicyTypesType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeTagsResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeTagsType;
import com.eucalyptus.loadbalancing.common.msgs.DetachLoadBalancerFromSubnetsResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DetachLoadBalancerFromSubnetsType;
import com.eucalyptus.loadbalancing.common.msgs.DisableAvailabilityZonesForLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DisableAvailabilityZonesForLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.EnableAvailabilityZonesForLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.EnableAvailabilityZonesForLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.AccessLog;
import com.eucalyptus.loadbalancing.common.msgs.AppCookieStickinessPolicies;
import com.eucalyptus.loadbalancing.common.msgs.AppCookieStickinessPolicy;
import com.eucalyptus.loadbalancing.common.msgs.AvailabilityZones;
import com.eucalyptus.loadbalancing.common.msgs.BackendServerDescription;
import com.eucalyptus.loadbalancing.common.msgs.BackendServerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckResult;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionSettings;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.CrossZoneLoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthResult;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPoliciesResult;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPolicyTypesResult;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersResult;
import com.eucalyptus.loadbalancing.common.msgs.DisableAvailabilityZonesForLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.EnableAvailabilityZonesForLoadBalancerResult;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.Instance;
import com.eucalyptus.loadbalancing.common.msgs.InstanceState;
import com.eucalyptus.loadbalancing.common.msgs.InstanceStates;
import com.eucalyptus.loadbalancing.common.msgs.Instances;
import com.eucalyptus.loadbalancing.common.msgs.LBCookieStickinessPolicies;
import com.eucalyptus.loadbalancing.common.msgs.LBCookieStickinessPolicy;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.msgs.ModifyLoadBalancerAttributesResponseType;
import com.eucalyptus.loadbalancing.common.msgs.ModifyLoadBalancerAttributesType;
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.RemoveTagsResponseType;
import com.eucalyptus.loadbalancing.common.msgs.RemoveTagsType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerListenerSSLCertificateResponseType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerListenerSSLCertificateType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesForBackendServerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesForBackendServerType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesOfListenerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesOfListenerType;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescription;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerDescription;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.Policies;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttribute;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.PolicyNames;
import com.eucalyptus.loadbalancing.common.msgs.PolicyTypeDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyTypeDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.SecurityGroups;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerListenerSSLCertificateResult;
import com.eucalyptus.loadbalancing.common.msgs.SourceSecurityGroup;
import com.eucalyptus.loadbalancing.common.msgs.Subnets;
import com.eucalyptus.loadbalancing.common.msgs.Tag;
import com.eucalyptus.loadbalancing.common.msgs.TagDescription;
import com.eucalyptus.loadbalancing.common.msgs.TagDescriptions;
import com.eucalyptus.loadbalancing.common.msgs.TagKeyOnly;
import com.eucalyptus.loadbalancing.common.msgs.TagList;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingWorkflows;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.TypeMappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.net.HostSpecifier;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import io.vavr.control.Option;

/**
 * @author Sang-Min Park
 */
@SuppressWarnings("UnusedDeclaration")
@ComponentNamed
public class LoadBalancingService {
  private static Logger LOG = Logger.getLogger(LoadBalancingService.class);
  private static final int MIN_HEALTHCHECK_INTERVAL_SEC = 5;
  public static final int MAX_HEALTHCHECK_INTERVAL_SEC = 120;
  private static final int MIN_HEALTHCHECK_THRESHOLDS = 2;

  private static final Set<String> reservedPrefixes =
      ImmutableSet.<String>builder().add("aws:").add("euca:").build();

  private final LoadBalancingPersistence loadBalancingPersistence;
  private final LoadBalancers loadBalancers;
  private final LoadBalancerSecurityGroups securityGroups;

  @Inject
  public LoadBalancingService(
      final LoadBalancers loadBalancers,
      final LoadBalancerSecurityGroups securityGroups
  ) {
    this.loadBalancers = loadBalancers;
    this.securityGroups = securityGroups;
    this.loadBalancingPersistence = ImmutableLoadBalancingPersistence.builder()
        .balancers(loadBalancers)
        .balancerSecurityGroups(securityGroups)
        .build();
  }

  public CreateLoadBalancerResponseType createLoadBalancer(
      final CreateLoadBalancerType request
  ) throws EucalyptusCloudException {
    final CreateLoadBalancerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final UserFullName ownerFullName = ctx.getUserFullName();
    final String lbName = request.getLoadBalancerName();

    // verify loadbalancer name
    if (lbName == null ||
        !HostSpecifier.isValid(String.format("%s.com", lbName)) ||
        !lbName.matches("[a-zA-Z0-9-]{1,32}")) {
      throw new InvalidConfigurationRequestException(
          "Invalid character found in the loadbalancer name");
    }

    final Map<String, String> tags = Maps.newHashMap();
    if (request.getTags() != null) {
      for (final Tag tag : request.getTags().getMember()) {
        if (tags.put(tag.getKey(), Strings.nullToEmpty(tag.getValue())) != null) {
          throw new LoadBalancingClientException("DuplicateTagKeys",
              "Duplicate tag key (" + tag.getKey() + ")");
        }
      }
      final int userTags =
          Iterables.size(Iterables.filter(tags.keySet(), Predicates.not(isReservedTagPrefix())));
      if (tags.size() - userTags > 0 && !Contexts.lookup().isPrivileged()) {
        throw new InvalidConfigurationRequestException("Invalid tag key (reserved prefix)");
      }
      if (userTags > LoadBalancingServiceProperties.getMaxTags()) {
        throw Exceptions.toUndeclared(
            new LoadBalancingClientException("TooManyTags", "Tag limit exceeded"));
      }
    }

    final List<Listener> listeners = request.getListeners() != null ?
        request.getListeners().getMember() :
        Collections.<Listener>emptyList();

    LoadBalancerHelper.validateListener(listeners);

    // Check SSL Certificate Id before creating LB
    try {
      for (final Listener l : listeners) {
        if ("HTTPS".equals(l.getProtocol().toUpperCase()) || "SSL".equals(
            l.getProtocol().toUpperCase())) {
          final String certArn = l.getSSLCertificateId();
          if (certArn == null || certArn.length() <= 0) {
            throw new InvalidConfigurationRequestException(
                "SSLCertificateId is required for HTTPS or SSL protocol");
          }
          LoadBalancerHelper.checkSSLCertificate(ctx.getAccountNumber(), certArn);
        }
      }
    } catch (Exception ex) {
      if (!(ex instanceof LoadBalancingException)) {
        LOG.error("failed to check SSL certificate Id", ex);
        ex = new InternalFailure400Exception("failed to check SSL certificate Id", ex);
      }
      throw (LoadBalancingException) ex;
    }

    if (request.getAvailabilityZones() != null && !request.getAvailabilityZones()
        .getMember()
        .isEmpty()) {
      final Set<String> validZones = Sets.newHashSet();
      try {
        Iterables.addAll(
            validZones,
            Iterables.transform(
                EucalyptusActivityTasks.getInstance().describeAvailabilityZones(),
                ClusterInfoType.zoneName()));
      } catch (Exception ex) {
        throw new InternalFailure400Exception("Unable to verify the requested zones");
      }
      for (final String zone : request.getAvailabilityZones().getMember()) {
        if (!validZones.contains(zone)) {
          throw new InvalidConfigurationRequestException(
              "No cluster named " + zone + " is available");
        }
      }
    }

    String subnetVpcId = null;
    final Map<String, String> zoneToSubnetIdMap = Maps.newHashMap();
    if (request.getSubnets() != null) {
      final List<SubnetType> subnets =
          EucalyptusActivityTasks.getInstance().describeSubnets(request.getSubnets().getMember());
      if (subnets.size() != request.getSubnets().getMember().size()) {
        throw new LoadBalancingClientException("SubnetNotFound", "Invalid subnet(s)");
      }
      for (final SubnetType subnetType : subnets) {
        if (subnetVpcId == null) {
          subnetVpcId = subnetType.getVpcId();
        } else if (!subnetVpcId.equals(subnetType.getVpcId())) {
          throw new InvalidConfigurationRequestException("Subnets must belong to the same VPC");
        }
        if (zoneToSubnetIdMap.put(subnetType.getAvailabilityZone(), subnetType.getSubnetId())
            != null) {
          throw new InvalidConfigurationRequestException(
              "Multiple subnets for zone (" + subnetType.getAvailabilityZone() + ")");
        }
      }
    }
    final AccountFullName accountFullName = ctx.getAccount();
    boolean defaultVpc = false;
    if (subnetVpcId == null) { // check for a default VPC
      final Optional<VpcType> vpcOptional =
          EucalyptusActivityTasks.getInstance().defaultVpc(accountFullName);
      subnetVpcId = vpcOptional.transform(VpcType.id()).orNull();
      defaultVpc = subnetVpcId != null;
    }
    final String vpcId = subnetVpcId;
    if (defaultVpc && zoneToSubnetIdMap.isEmpty() && request.getAvailabilityZones() != null) {
      final List<SubnetType> subnets = EucalyptusActivityTasks.getInstance().describeSubnetsByZone(
          vpcId, true, request.getAvailabilityZones().getMember());
      CollectionUtils.putAll(subnets, zoneToSubnetIdMap, SubnetType.zone(), SubnetType.id());
      if (request.getAvailabilityZones().getMember().size() != zoneToSubnetIdMap.size()) {
        throw new InvalidConfigurationRequestException("Default subnet not found for zone(s)");
      }
    }

    LoadBalancer.Scheme reqScheme = LoadBalancer.Scheme.fromString(request.getScheme()).orNull();
    if (reqScheme == null && !Strings.isNullOrEmpty(request.getScheme())) {
      throw new InvalidConfigurationRequestException(
          "Invalid scheme (" + request.getScheme() + ")");
    }
    if (reqScheme != null && vpcId == null) {
      if (reqScheme != LoadBalancer.Scheme.InternetFacing) {
        throw new InvalidConfigurationRequestException("Scheme ("
            + request.getScheme()
            + ") should not be specified for EC2-Classic platform");
      } else {
        reqScheme = null; // ignore internet-facing scheme for non-vpc ELB
      }
    }
    final LoadBalancer.Scheme scheme = reqScheme;

    if (vpcId != null && scheme != LoadBalancer.Scheme.Internal) {
      try {
        final List<InternetGatewayType> internetGateways =
            EucalyptusActivityTasks.getInstance()
                .describeInternetGateways(Collections.singleton(vpcId));
        if (internetGateways.isEmpty()) {
          throw new LoadBalancingClientException("InvalidSubnet",
              "VPC " + vpcId + " has no internet gateway");
        }
      } catch (final LoadBalancingException e) {
        throw e;
      } catch (final Exception e) {
        LOG.error("Error checking internet gateway", e);
        throw new InternalFailureException("Unable to verify VPC configuration");
      }
    }

    final Set<String> securityGroupIds = Sets.newHashSet();
    if (request.getSecurityGroups() != null) {
      securityGroupIds.addAll(request.getSecurityGroups().getMember());
    } else if (vpcId != null && request.getSubnets() != null) {
      // for default VPC (where availability-zone is specified), a group is created/discovered later
      final List<SecurityGroupItemType> groups = EucalyptusActivityTasks.getInstance()
          .describeUserSecurityGroupsByName(accountFullName, vpcId, "default");
      if (groups.isEmpty()) {
        throw new InvalidConfigurationRequestException(
            "Default security group not found for VPC " + vpcId);
      }
      securityGroupIds.add(groups.get(0).getGroupId());
    }
    if (!securityGroupIds.isEmpty() && vpcId == null) {
      throw new InvalidConfigurationRequestException(
          "Security groups should not be specified for EC2-Classic platform");
    }
    final List<SecurityGroupItemType> groups = securityGroupIds.isEmpty() ?
        Collections.<SecurityGroupItemType>emptyList() :
        EucalyptusActivityTasks.getInstance()
            .describeUserSecurityGroupsById(accountFullName, vpcId, securityGroupIds);
    if (groups.size() != securityGroupIds.size()) {
      throw new LoadBalancingClientException("InvalidSecurityGroup", "Invalid security group(s)");
    }

    final Collection<String> zones = Lists.<String>newArrayList();
    if (request.getAvailabilityZones() != null) {
      zones.addAll(request.getAvailabilityZones().getMember());
    }
    if (zones.isEmpty()) zones.addAll(zoneToSubnetIdMap.keySet());
    if (!zoneToSubnetIdMap.isEmpty() && !zoneToSubnetIdMap.keySet()
        .equals(Sets.newHashSet(zones))) {
      throw new InvalidConfigurationRequestException(
          "Availability zones and subnets are inconsistent");
    }

    final Supplier<LoadBalancer> allocator = new Supplier<LoadBalancer>() {
      @Override
      public LoadBalancer get() {
        try {
          final Map<String, String> securityGroupIdsToNames = CollectionUtils.putAll(
              groups,
              Maps.<String, String>newHashMap(),
              SecurityGroupItemType.groupId(),
              SecurityGroupItemType.groupName());
          return LoadBalancerHelper.addLoadbalancer(
              loadBalancingPersistence, ownerFullName, lbName, vpcId, scheme,
              securityGroupIdsToNames, tags);
        } catch (final LoadBalancingException e) {
          throw Exceptions.toUndeclared(e);
        }
      }
    };

    final LoadBalancer lb;
    try {
      lb = LoadBalancingMetadatas.allocateUnitlessResource(allocator);
    } catch (Exception e) {
      throw handleException(e);
    }

    Function<String, Boolean> rollback = new Function<String, Boolean>() {
      @Override
      public Boolean apply(String lbName) {
        try {
          LoadBalancerHelper.unsetForeignKeys(loadBalancingPersistence, ownerFullName, lbName);
        } catch (final Exception ex) {
          LOG.warn("unable to unset foreign keys", ex);
        }

        try {
          LoadBalancerHelper.removeZone(loadBalancingPersistence, ownerFullName, lbName, zones);
        } catch (final Exception ex) {
          LOG.error("unable to delete availability zones during rollback", ex);
        }

        try {
          LoadBalancerHelper.deleteLoadbalancer(loadBalancingPersistence, ownerFullName, lbName);
        } catch (LoadBalancingException ex) {
          LOG.error("failed to rollback the loadbalancer: " + lbName, ex);
          return false;
        }
        return true;
      }
    };

    Entities.evictCache(LoadBalancer.class);
    try {
      LoadBalancingWorkflows.createLoadBalancerSync(ctx.getAccountNumber(), lbName,
          Lists.newArrayList(zones));
      if (!listeners.isEmpty()) {
        LoadBalancerHelper.createLoadbalancerListener(loadBalancingPersistence, lbName, ctx,
            Lists.newArrayList(listeners));
        if (!LoadBalancingWorkflows.createListenersSync(ctx.getAccountNumber(), lbName,
            Lists.newArrayList(listeners))) {
          throw new InternalFailure400Exception("Workflow for creating listeners has failed");
        }
      }

      if (!zones.isEmpty()) {
        if (!LoadBalancingWorkflows.enableZonesSync(ctx.getAccountNumber(), lbName,
            Lists.newArrayList(zones), zoneToSubnetIdMap)) {
          throw new InternalFailure400Exception(
              "Workflow for enabling ELB availablity zone has failed");
        }
      }

      LoadBalancingWorkflows.runInstanceStatusPolling(ctx.getAccountNumber(), lbName);
      LoadBalancingWorkflows.runCloudWatchPutMetric(ctx.getAccountNumber(), lbName);
      LoadBalancingWorkflows.runUpdateLoadBalancer(ctx.getAccountNumber(), lbName);
    } catch (final LoadBalancingWorkflowException ex) {
      rollback.apply(lbName);
      final int statusCode = ex.getStatusCode();
      final String reason = ex.getMessage();
      if (statusCode == 400) {
        final String errorMessage = reason != null ? "Failed to create loadbalancer: " + reason
            : "Failed to create loadbalancer: internal error";
        throw new InternalFailure400Exception(errorMessage);
      } else if (reason != null) {
        throw new InternalFailureException("Failed to create loadbalancer: " + reason);
      } else {
        throw new InternalFailureException("Failed to create loadbalancer: internal error");
      }
    } catch (final LoadBalancingException ex) {
      rollback.apply(lbName);
      throw ex;
    } catch (final Exception e) {
      rollback.apply(lbName);
      LOG.error("Error creating the loadbalancer: " + e.getMessage(), e);
      final String reason =
          e.getCause() != null && e.getCause().getMessage() != null ? e.getMessage()
              : "internal error";
      throw new InternalFailure400Exception(
          String.format("Failed to create loadbalancer: %s", reason), e);
    }

    final CreateLoadBalancerResult result = new CreateLoadBalancerResult();
    result.setDnsName(LoadBalancerHelper.getLoadBalancerDnsName(lb));
    reply.setCreateLoadBalancerResult(result);
    reply.set_return(true);
    return reply;
  }

  public DescribeLoadBalancersResponseType describeLoadBalancers(DescribeLoadBalancersType request)
      throws EucalyptusCloudException {
    DescribeLoadBalancersResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccount().getAccountNumber();
    final Set<String> requestedNames = Sets.newHashSet();
    if (request.getLoadBalancerNames() != null) {
      requestedNames.addAll(request.getLoadBalancerNames().getMember());
    }
    final boolean showAll = requestedNames.remove("verbose") && ctx.isAdministrator();

    final Predicate<? super LoadBalancer> requestedAndAccessible =
        LoadBalancingMetadatas.filteringFor(LoadBalancer.class)
            .byId(requestedNames)
            .byPrivileges()
            .buildPredicate();

    final LoadBalancer example = showAll ?
        LoadBalancer.named(null, null) :
        LoadBalancer.namedByAccountId(accountNumber, null);

    final Collection<LoadBalancerFullView> allowedLBs;
    try {
      allowedLBs = loadBalancingPersistence.balancers()
          .listByExample(example, requestedAndAccessible, LoadBalancers.FULL_VIEW);
    } catch (LoadBalancingMetadataException ex) {
      throw Exceptions.toUndeclared(ex);
    }

    final Option<Pair<String, String>> hostedZoneNameAndId =
        LoadBalancingHostedZoneManager.getHostedZoneNameAndId();
    final Set<LoadBalancerDescription> descs = Sets.newHashSet();
    for (final LoadBalancerFullView allowedLB : allowedLBs) {
      final LoadBalancerView lb = allowedLB.getLoadBalancer();
      LoadBalancerDescription desc = new LoadBalancerDescription();
      if (lb == null) // loadbalancer not found
      {
        continue;
      }
      final String lbName = lb.getDisplayName();
      desc.setLoadBalancerName(lbName);
      desc.setCreatedTime(lb.getCreationTimestamp());

      // dns name
      desc.setDnsName(LoadBalancerHelper.getLoadBalancerDnsName(lb));

      // hosted zone
      if (hostedZoneNameAndId.isDefined()) {
        desc.setCanonicalHostedZoneName(hostedZoneNameAndId.get().getLeft());
        desc.setCanonicalHostedZoneNameID(hostedZoneNameAndId.get().getRight());
      }

      // instances
      if (allowedLB.getBackendInstances().size() > 0) {
        desc.setInstances(new Instances());
        desc.getInstances().getMember().addAll(
            Stream.ofAll(allowedLB.getBackendInstances())
                .map(LoadBalancerBackendInstanceView::getInstanceId)
                .map(Instance.instance())
                .toJavaList());
      }

      // availability zones
      final List<String> subnetIds = Lists.newArrayList();
      if (!allowedLB.getZones().isEmpty()) {
        desc.setAvailabilityZones(new AvailabilityZones());
        final List<LoadBalancerZoneView> currentZones = Stream.ofAll(allowedLB.getZones())
            .filter(zone -> LoadBalancerZone.STATE.InService == zone.getState())
            .toJavaList();
        desc.getAvailabilityZones()
            .getMember()
            .addAll(Stream.ofAll(currentZones).map(LoadBalancerZoneView::getName).toJavaList());
        subnetIds.addAll(
            Stream.ofAll(currentZones).flatMap(zone -> Option.of(zone.getSubnetId())).toJavaList());
      }

      // subnets
      if (!subnetIds.isEmpty()) {
        desc.setSubnets(new Subnets(subnetIds));
      }

      desc.setVpcId(lb.getVpcId());

      // listeners
      if (allowedLB.getListeners().size() > 0) {
        desc.setListenerDescriptions(new ListenerDescriptions());
        desc.getListenerDescriptions().setMember(Lists.newArrayList(
            Collections2.transform(allowedLB.getListeners(), fullInput -> {
              final LoadBalancerListenerView input = fullInput.getListener();
              ListenerDescription desc1 = new ListenerDescription();
              Listener listener = new Listener();
              listener.setLoadBalancerPort(input.getLoadbalancerPort());
              listener.setInstancePort(input.getInstancePort());
              if (input.getInstanceProtocol() != PROTOCOL.NONE) {
                listener.setInstanceProtocol(input.getInstanceProtocol().name());
              }
              listener.setProtocol(input.getProtocol().name());
              if (input.getCertificateId() != null) {
                listener.setSSLCertificateId(input.getCertificateId());
              }

              desc1.setListener(listener);
              final PolicyNames pnames = new PolicyNames();
              pnames.setMember(Lists.newArrayList(
                  Stream.ofAll(fullInput.getPolicyDescriptions())
                      .map(LoadBalancerPolicyDescriptionView::getPolicyName)));
              desc1.setPolicyNames(pnames);
              return desc1;
            })));
      }

      /// health check
      if (lb.hasHealthCheckConfig()) {
        int interval = lb.getHealthCheckConfig().getInterval();
        String target = lb.getHealthCheckConfig().getTarget();
        int timeout = lb.getHealthCheckConfig().getTimeout();
        int healthyThresholds = lb.getHealthCheckConfig().getHealthyThreshold();
        int unhealthyThresholds = lb.getHealthCheckConfig().getUnhealthyThreshold();

        final HealthCheck hc = new HealthCheck();
        hc.setInterval(interval);
        hc.setHealthyThreshold(healthyThresholds);
        hc.setTarget(target);
        hc.setTimeout(timeout);
        hc.setUnhealthyThreshold(unhealthyThresholds);
        desc.setHealthCheck(hc);
      }
      /// backend server description
      try {
        final List<LoadBalancerBackendServerDescriptionFullView> backendServers =
            allowedLB.getBackendServers();
        final List<BackendServerDescription> backendDescription = Lists.newArrayList();

        for (final LoadBalancerBackendServerDescriptionFullView server : backendServers) {
          final BackendServerDescription serverDesc = new BackendServerDescription();
          serverDesc.setInstancePort(server.getBackendServer().getInstancePort());
          final PolicyNames polNames = new PolicyNames();
          polNames.setMember(Lists.newArrayList(
              Stream.ofAll(server.getPolicyDescriptions())
                  .map(LoadBalancerPolicyDescriptionView::getPolicyName)));
          serverDesc.setPolicyNames(polNames);
          backendDescription.add(serverDesc);
        }
        final BackendServerDescriptions backendDescs = new BackendServerDescriptions();
        backendDescs.setMember((ArrayList<BackendServerDescription>) backendDescription);
        desc.setBackendServerDescriptions(backendDescs);
      } catch (final Exception ex) {
        LOG.error("Failed to load backend server description", ex);
      }

      /// source security group
      try {
        LoadBalancerSecurityGroupView group = allowedLB.getSecurityGroup();
        if (group != null) {
          desc.setSourceSecurityGroup(
              new SourceSecurityGroup(group.getGroupOwnerAccountId(), group.getName()));
        }
      } catch (Exception ex) {
      }

      if (!allowedLB.getSecurityGroupRefs().isEmpty()) {
        desc.setSecurityGroups(new SecurityGroups(Stream.ofAll(allowedLB.getSecurityGroupRefs())
            .map(LoadBalancerSecurityGroupRefView::getGroupId)
            .toJavaList()));
        if (desc.getSourceSecurityGroup() == null) {
          desc.setSourceSecurityGroup(new SourceSecurityGroup(
              lb.getOwnerAccountNumber(),
              Stream.ofAll(allowedLB.getSecurityGroupRefs()).head().getGroupName()));
        }
      }

      // policies
      try {
        final List<LoadBalancerPolicyDescriptionFullView> lbPolicies = allowedLB.getPolicies();
        final ArrayList<AppCookieStickinessPolicy> appCookiePolicies = Lists.newArrayList();
        final ArrayList<LBCookieStickinessPolicy> lbCookiePolicies = Lists.newArrayList();
        final ArrayList<String> otherPolicies = Lists.newArrayList();
        for (final LoadBalancerPolicyDescriptionFullView policyFull : lbPolicies) {
          final LoadBalancerPolicyDescriptionView policy = policyFull.getPolicyDescription();
          if ("LBCookieStickinessPolicyType".equals(policy.getPolicyTypeName())) {
            final LBCookieStickinessPolicy lbp = new LBCookieStickinessPolicy();
            lbp.setPolicyName(policy.getPolicyName());
            final List<LoadBalancerPolicyAttributeDescriptionView> attrs =
                policyFull.findAttributeDescription("CookieExpirationPeriod");
            if (!attrs.isEmpty()) {
              lbp.setCookieExpirationPeriod(Long.parseLong(attrs.get(0).getAttributeValue()));
            }

            lbCookiePolicies.add(lbp);
          } else if ("AppCookieStickinessPolicyType".equals(policy.getPolicyTypeName())) {
            final AppCookieStickinessPolicy app = new AppCookieStickinessPolicy();
            app.setPolicyName(policy.getPolicyName());
            final List<LoadBalancerPolicyAttributeDescriptionView> attrs =
                policyFull.findAttributeDescription("CookieName");
            if (!attrs.isEmpty()) {
              app.setCookieName(attrs.get(0).getAttributeValue());
            }

            appCookiePolicies.add(app);
          } else {
            otherPolicies.add(policy.getPolicyName());
          }
        }
        final Policies p = new Policies();
        final LBCookieStickinessPolicies lbp = new LBCookieStickinessPolicies();
        lbp.setMember(lbCookiePolicies);
        final AppCookieStickinessPolicies app = new AppCookieStickinessPolicies();
        app.setMember(appCookiePolicies);
        final PolicyNames other = new PolicyNames();
        other.setMember(otherPolicies);
        p.setAppCookieStickinessPolicies(app);
        p.setLbCookieStickinessPolicies(lbp);
        p.setOtherPolicies(other);
        desc.setPolicies(p);

        desc.setScheme(Objects.toString(lb.getScheme(), "internet-facing"));
      } catch (final Exception ex) {
        LOG.error("Failed to retrieve policies", ex);
      }
      descs.add(desc);
    }
    final DescribeLoadBalancersResult descResult = new DescribeLoadBalancersResult();
    final LoadBalancerDescriptions lbDescs = new LoadBalancerDescriptions();
    lbDescs.setMember(new ArrayList<>(descs));
    descResult.setLoadBalancerDescriptions(lbDescs);
    reply.setDescribeLoadBalancersResult(descResult);
    reply.set_return(true);

    return reply;
  }

  public DeleteLoadBalancerResponseType deleteLoadBalancer(DeleteLoadBalancerType request)
      throws EucalyptusCloudException {
    DeleteLoadBalancerResponseType reply = request.getReply();
    final String candidateLB = request.getLoadBalancerName();
    final Context ctx = Contexts.lookup();
    Function<String, LoadBalancerListenersView> findLoadBalancer =
        new Function<String, LoadBalancerListenersView>() {
          @Override
          @Nullable
          public LoadBalancerListenersView apply(@Nullable String lbName) {
            try {
              return LoadBalancerHelper.getLoadbalancer(loadBalancingPersistence,
                  LoadBalancingMetadatas.filterPrivileged(), LoadBalancers.LISTENERS_VIEW,
                  ctx.getAccount(), lbName);
            } catch (NoSuchElementException ex) {
              if (ctx.isAdministrator()) {
                try {
                  return LoadBalancerHelper.getLoadBalancerByDnsName(loadBalancingPersistence,
                      LoadBalancingMetadatas.filterPrivileged(), LoadBalancers.LISTENERS_VIEW,
                      lbName);
                } catch (Exception ex2) {
                  if (ex2 instanceof NoSuchElementException) {
                    throw Exceptions.toUndeclared(new LoadBalancingException(
                        "Unable to find the loadbalancer (use DNS name if you are an administrator)"));
                  }
                  throw Exceptions.toUndeclared(ex2);
                }
              }
              throw ex;
            }
          }
        };

    LoadBalancerListenersView lbFull = null;
    LoadBalancerView lb = null;
    try {
      if (candidateLB != null) {
        String lbToDelete = null;
        try {
          lbFull = findLoadBalancer.apply(candidateLB);
          lb = lbFull.getLoadBalancer();
          lbToDelete = lb.getDisplayName();
        } catch (Exception ex) {
          Exceptions.findAndRethrow(ex, LoadBalancingException.class);
          throw ex;
        }

        final List<LoadBalancerListenerFullView> listeners = lbFull.getListeners();
        final List<Integer> ports = Lists.newArrayList(
            Collections2.transform(listeners, arg0 -> arg0.getListener().getLoadbalancerPort()));

        if (!LoadBalancingWorkflows.deleteListenersSync(lb.getOwnerAccountNumber(), lbToDelete,
            Lists.newArrayList(ports))) {
          throw new Exception("Workflow for deleting listeners has failed");
        } else if (!LoadBalancingWorkflows.deleteLoadBalancerSync(lb.getOwnerAccountNumber(),
            lbToDelete)) {
          throw new Exception("Workflow for deleting loadbalancer has failed");
        } else {
          /// perhaps these workflows should be stopped in the clean-up workflow
          LoadBalancingWorkflows.cancelInstanceStatusPolling(lb.getOwnerAccountNumber(),
              lbToDelete);
          LoadBalancingWorkflows.cancelCloudWatchPutMetric(lb.getOwnerAccountNumber(), lbToDelete);
          LoadBalancingWorkflows.cancelUpdateLoadBalancer(lb.getOwnerAccountNumber(), lbToDelete);
          LoadBalancerHelper.deleteLoadbalancer(
              loadBalancingPersistence,
              AccountFullName.getInstance(lb.getOwnerAccountNumber()),
              lbToDelete);
        }
      }
    } catch (LoadBalancingException e) {
      throw new InternalFailure400Exception(e.getMessage());
    } catch (Exception e) {
      // success if the lb is not found in the system
      if (!Exceptions.isCausedBy(e, NoSuchElementException.class)) {
        LOG.error("Error deleting the loadbalancer: " + e.getMessage(), e);
        final String reason = "internal error";
        throw new InternalFailure400Exception(
            String.format("Failed to delete the loadbalancer: %s", reason), e);
      }
    }

    DeleteLoadBalancerResult result = new DeleteLoadBalancerResult();
    reply.setDeleteLoadBalancerResult(result);
    reply.set_return(true);
    return reply;
  }

  public CreateLoadBalancerListenersResponseType createLoadBalancerListeners(
      CreateLoadBalancerListenersType request) throws EucalyptusCloudException {
    final CreateLoadBalancerListenersResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final List<Listener> listeners = request.getListeners().getMember();

    final LoadBalancerZonesView lb;
    final List<LoadBalancerListenerView> existingListeners;
    try {
      final Tuple2<LoadBalancerZonesView, List<LoadBalancerListenerView>> lbData
          = LoadBalancerHelper.getLoadbalancer(
          loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(),
          lbEntity -> Tuple.of(
              LoadBalancers.ZONES_VIEW.apply(lbEntity),
              Lists.newArrayList(Iterables.transform(
                  lbEntity.getListeners(),
                  ImmutableLoadBalancerListenerView::copyOf))
          ),
          ctx.getAccount(),
          lbName);
      lb = lbData._1();
      existingListeners = lbData._2();
    } catch (NoSuchElementException ex) {
      throw new AccessPointNotFoundException();
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }

    if (listeners != null) {
      LoadBalancerHelper.validateListener(lb.getLoadBalancer(), existingListeners, listeners);
    }

    try {
      for (final Listener l : listeners) {
        if ("HTTPS".equals(l.getProtocol().toUpperCase()) || "SSL".equals(
            l.getProtocol().toUpperCase())) {
          final String certArn = l.getSSLCertificateId();
          if (certArn == null || certArn.length() <= 0) {
            throw new InvalidConfigurationRequestException(
                "SSLCertificateId is required for HTTPS or SSL protocol");
          }
          LoadBalancerHelper.checkSSLCertificate(ctx.getAccountNumber(), certArn);
        }
      }
    } catch (Exception ex) {
      if (!(ex instanceof LoadBalancingException)) {
        LOG.error("failed to check SSL certificate Id", ex);
        ex = new InternalFailure400Exception("failed to check SSL certificate Id", ex);
      }
      throw (LoadBalancingException) ex;
    }

    LoadBalancerHelper.checkWorkerCertificateExpiration(lb);

    try {
      LoadBalancerHelper.createLoadbalancerListener(loadBalancingPersistence, lbName, ctx,
          listeners);
    } catch (final LoadBalancingException ex) {
      throw ex;
    } catch (final Exception e) {
      final String reason =
          e.getCause() != null && e.getCause().getMessage() != null ? e.getMessage()
              : "internal error";
      throw new InternalFailure400Exception(String.format("Failed to create listener: %s", reason),
          e);
    }

    try {
      if (!LoadBalancingWorkflows.createListenersSync(ctx.getAccountNumber(),
          lbName, listeners)) {
        throw new Exception("Workflow for creating listeners failed");
      }
    } catch (final Exception e) {
      try {
        loadBalancingPersistence.balancers().updateByExample(
            LoadBalancer.namedByAccountId(ctx.getAccountNumber(), lbName),
            ctx.getAccount(),
            lbName,
            Predicates.alwaysTrue(),
            update -> {
              for (final Listener addedListener : listeners) {
                final LoadBalancerListener listener =
                    LoadBalancerHelper.findListener(update, addedListener.getLoadBalancerPort());
                if (listener != null) {
                  Entities.delete(listener);
                }
              }
              return update;
            });
      } catch (Exception ex) {
        LOG.warn("Error in cleanup for failure adding load balancer listener(s) " + lbName, ex);
      }
      final String reason =
          e.getCause() != null && e.getCause().getMessage() != null ? e.getMessage()
              : "internal error";
      throw new InternalFailure400Exception(String.format("Failed to create listener: %s", reason),
          e);
    }

    LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    reply.set_return(true);
    return reply;
  }

  public DeleteLoadBalancerListenersResponseType deleteLoadBalancerListeners(
      DeleteLoadBalancerListenersType request) throws EucalyptusCloudException {
    final DeleteLoadBalancerListenersResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final Collection<Integer> listenerPorts;
    try {
      listenerPorts = Collections2.transform(
          request.getLoadBalancerPorts().getMember(), new Function<String, Integer>() {
            @Override
            public Integer apply(final String input) {
              return new Integer(input);
            }
          });
    } catch (Exception ex) {
      throw new InvalidConfigurationRequestException("Invalid port number");
    }

    final LoadBalancerListenersView loadBalancerListeners;
    try {
      loadBalancerListeners = LoadBalancerHelper.getLoadbalancer(
          loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(),
          LoadBalancers.LISTENERS_VIEW,
          ctx.getAccountNumber(),
          lbName);
    } catch (NoSuchElementException e) {
      throw new AccessPointNotFoundException();
    } catch (Exception e) {
      throw handleException(e);
    }

    final List<Integer> toDelete = Stream.ofAll(loadBalancerListeners.getListeners())
        .map(loadBalancerListener -> loadBalancerListener.getListener().getLoadbalancerPort())
        .toJavaList();

    if (!toDelete.isEmpty()) {
      try {
        if (!LoadBalancingWorkflows.deleteListenersSync(ctx.getAccountNumber(),
            lbName, Lists.newArrayList(toDelete))) {
          throw new Exception("Workflow for deleting listeners has failed");
        }
      } catch (final Exception e) {
        final String reason =
            e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage()
                : "internal error";
        throw new InternalFailure400Exception(
            String.format("Failed to delete listener: %s", reason), e);
      }

      try {
        final LoadBalancer example = LoadBalancer.namedByAccountId(ctx.getAccountNumber(), lbName);
        loadBalancingPersistence.balancers().updateByExample(
            example,
            ctx.getAccount(),
            lbName,
            Predicates.alwaysTrue(),
            loadBalancer -> {
              for (final LoadBalancerListener listener : loadBalancer.getListeners()) {
                if (toDelete.contains(listener.getLoadbalancerPort())) {
                  Entities.delete(listener);
                }
              }
              return loadBalancer;
            }
        );
      } catch (final LoadBalancingMetadataNotFoundException e) {
        throw new AccessPointNotFoundException();
      } catch (Exception e) {
        throw handleException(e);
      }
    }

    LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    reply.set_return(true);

    return reply;
  }

  public RegisterInstancesWithLoadBalancerResponseType registerInstancesWithLoadBalancer(
      final RegisterInstancesWithLoadBalancerType request
  ) throws EucalyptusCloudException {
    final RegisterInstancesWithLoadBalancerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccountNumber();
    final UserFullName ownerFullName = ctx.getUserFullName();
    final String lbName = request.getLoadBalancerName();
    final Collection<Instance> instances = request.getInstances().getMember();
    final List<String> requestedInstanceIds =
        Lists.newArrayList(Collections2.transform(instances, Instance::getInstanceId));

    final LoadBalancerFullView lbFull;
    final LoadBalancerView lb;
    try {
      lbFull = LoadBalancerHelper.getLoadbalancer(
          loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(),
          LoadBalancers.FULL_VIEW,
          ctx.getAccountNumber(),
          lbName);
      lb = lbFull.getLoadBalancer();
    } catch (final NoSuchElementException ex) {
      throw new AccessPointNotFoundException();
    } catch (Exception ex) {
      throw handleException(ex);
    }

    final Set<String> backends = Sets.newHashSet(
        Iterables.transform(lbFull.getBackendInstances(),
            LoadBalancerBackendInstanceView::getInstanceId));

    /*********** Verify requests ************/
    if (lb.getVpcId() != null) {
      final List<RunningInstancesItemType> instanceItems =
          EucalyptusActivityTasks.getInstance()
              .describeUserInstances(ctx.getAccountNumber(), requestedInstanceIds);

      for (final RunningInstancesItemType instanceItem : instanceItems) {
        if (!lb.getVpcId().equals(instanceItem.getVpcId())) {
          throw new InvalidConfigurationRequestException("Invalid instance(s) for load balancer.");
        }
      }
    }

    final Collection<LoadBalancerZoneView> enabledZones =
        Collections2.filter(lbFull.getZones(),
            arg0 -> LoadBalancerZone.STATE.InService.equals(arg0.getState()));
    final Set<String> lbZones =
        Sets.newHashSet(Collections2.transform(enabledZones, LoadBalancerZoneView::getName));

    final List<RunningInstancesItemType> eucaInstances;
    try {
      eucaInstances = EucalyptusActivityTasks.getInstance()
          .describeUserInstances(accountNumber, requestedInstanceIds);
    } catch (final Exception ex) {
      throw new InvalidConfigurationRequestException("Failed to look up requested instances");
    }

    for (final RunningInstancesItemType instance : eucaInstances) {
      if (!lbZones.contains(instance.getPlacement())) {
        throw new InvalidConfigurationRequestException("Instance "
            + instance.getInstanceId()
            + "'s availaibility zone is not enabled for the loadbalancer");
      }
    }

    /*********** END Verify requests ************/
    // when there's any new instance in the request
    if (instances.stream().anyMatch(vm -> !backends.contains(vm.getInstanceId()))) {
      final List<RunningInstancesItemType> runningInstances =
          EucalyptusActivityTasks.getInstance()
              .describeUserInstances(accountNumber, requestedInstanceIds);

      try {
        final LoadBalancer example = LoadBalancer.namedByAccountId(accountNumber, lbName);
        loadBalancingPersistence.balancers().updateByExample(
            example,
            ctx.getAccount(),
            lbName,
            Predicates.alwaysTrue(),
            loadBalancer -> {
              for (Instance vm : instances) {
                if (LoadBalancerHelper.findBackendInstance(loadBalancer, vm.getInstanceId())
                    != null) {
                  continue; // the vm instance is already registered
                }

                String partition = null;
                String ipAddress = null;
                for (final RunningInstancesItemType runningInstance : runningInstances) {
                  if (runningInstance.getInstanceId().equals(vm.getInstanceId())
                      && runningInstance.getStateName().equals("running")) {
                    partition = runningInstance.getPlacement();
                    if (loadBalancer.getVpcId() == null) {
                      ipAddress = runningInstance.getIpAddress();
                    } else {
                      ipAddress = runningInstance.getPrivateIpAddress();
                    }
                    break;
                  }
                }

                try {
                  final LoadBalancerZone zone =
                      LoadBalancerHelper.findZone(loadBalancer, partition);
                  final LoadBalancerBackendInstance beInstance =
                      LoadBalancerBackendInstance.newInstance(ownerFullName, loadBalancer, zone,
                          vm.getInstanceId(), ipAddress);
                  final LoadBalancerBackendInstanceStates registration =
                      LoadBalancerBackendInstanceStates.InitialRegistration;
                  beInstance.setState(registration.getState());
                  beInstance.setReasonCode(registration.getReasonCode());
                  beInstance.setDescription(registration.getDescription());
                  Entities.persist(beInstance);
                } catch (final LoadBalancingException ex) {
                  throw Exceptions.toUndeclared(ex);
                }
              }
              return loadBalancer;
            }
        );
      } catch (Exception ex) {
        throw handleException(ex);
      }
      Iterables.addAll(backends, requestedInstanceIds);

      LoadBalancingWorkflows.updateLoadBalancer(accountNumber, lbName);
      Threads.enqueue(LoadBalancing.class, LoadBalancingService.class,
          new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
              try {
                Thread.sleep(2000);
                // delay polling signal by a few sec. Best-effort approach to poll InService instance after registration
              } catch (final Exception ex) {
                ;
              }
              LoadBalancingWorkflows.pollInstanceStatus(accountNumber, lbName);
              return true;
            }
          });
    }
    final Instances returnInstances = new Instances();
    Iterables.addAll(returnInstances.getMember(),
        Iterables.transform(backends, Instance.instance()));
    reply.getRegisterInstancesWithLoadBalancerResult().setInstances(returnInstances);
    return reply;
  }

  public DeregisterInstancesFromLoadBalancerResponseType deregisterInstancesFromLoadBalancer(
      DeregisterInstancesFromLoadBalancerType request) throws EucalyptusCloudException {
    final DeregisterInstancesFromLoadBalancerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccountNumber();
    final String lbName = request.getLoadBalancerName();
    final Collection<Instance> instances = request.getInstances().getMember();
    final List<String> requestedInstanceIds =
        Lists.newArrayList(Collections2.transform(instances, Instance::getInstanceId));

    final LoadBalancerFullView lbFull;
    final LoadBalancerView lb;
    try {
      lbFull = LoadBalancerHelper.getLoadbalancer(
          loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(),
          LoadBalancers.FULL_VIEW,
          ctx.getAccountNumber(),
          lbName);
      lb = lbFull.getLoadBalancer();
    } catch (final NoSuchElementException ex) {
      throw new AccessPointNotFoundException();
    } catch (Exception ex) {
      throw handleException(ex);
    }

    final List<LoadBalancerBackendInstanceView> allInstances =
        Lists.newArrayList(lbFull.getBackendInstances());
    final Collection<LoadBalancerBackendInstanceView> instancesToRemove = Collections2.filter(
        lbFull.getBackendInstances(),
        backendInstance -> requestedInstanceIds.contains(backendInstance.getInstanceId()));
    if (instancesToRemove.isEmpty()) {
      reply.set_return(false);
      return reply;
    }
    final Collection<String> removeInstanceIds =
        Collections2.transform(instancesToRemove, LoadBalancerBackendInstanceView::getInstanceId);

    final Set<String> remainingInstanceIds;
    try {
      final LoadBalancer example = LoadBalancer.namedByAccountId(accountNumber, lbName);
      remainingInstanceIds = loadBalancingPersistence.balancers().updateByExample(
          example,
          ctx.getAccount(),
          lbName,
          Predicates.alwaysTrue(),
          loadBalancer -> {
            final Set<String> backendIdentifiers = Sets.newTreeSet();
            for (final LoadBalancerBackendInstance backendInstance : loadBalancer.getBackendInstances()) {
              if (removeInstanceIds.contains(backendInstance.getInstanceId())) {
                Entities.delete(backendInstance);
              } else {
                backendIdentifiers.add(backendInstance.getInstanceId());
              }
            }
            return backendIdentifiers;
          }
      );
    } catch (final Exception ex) {
      final String reason =
          ex.getCause() != null && ex.getCause().getMessage() != null ? ex.getCause().getMessage()
              : "internal error";
      throw new InternalFailure400Exception(
          String.format("Failed to deregister instances: %s", reason), ex);
    }

    LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);

    final DeregisterInstancesFromLoadBalancerResult result =
        new DeregisterInstancesFromLoadBalancerResult();
    final Instances returnInstances = new Instances();
    returnInstances.setMember(
        Lists.newArrayList(Collections2.transform(remainingInstanceIds, instanceId -> {
          final Instance newInst = new Instance();
          newInst.setInstanceId(instanceId);
          return newInst;
        })));
    result.setInstances(returnInstances);
    reply.setDeregisterInstancesFromLoadBalancerResult(result);
    return reply;
  }

  public EnableAvailabilityZonesForLoadBalancerResponseType enableAvailabilityZonesForLoadBalancer(
      EnableAvailabilityZonesForLoadBalancerType request) throws EucalyptusCloudException {
    final EnableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final Collection<String> requestedZones = request.getAvailabilityZones().getMember();

    final LoadBalancerFullView lbFull;
    try {
      lbFull = LoadBalancerHelper.getLoadbalancer(loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(), LoadBalancers.FULL_VIEW, ctx.getAccount(),
          lbName);
    } catch (final Exception ex) {
      throw new AccessPointNotFoundException();
    }
    final LoadBalancerView lb = lbFull.getLoadBalancer();

    final Set<String> allZones =
        Sets.newHashSet(Iterables.transform(lbFull.getZones(), LoadBalancerZoneView::getName));

    // check for a default VPC
    final Optional<VpcType> vpcOptional =
        EucalyptusActivityTasks.getInstance().defaultVpc(ctx.getAccount());
    final Map<String, String> zoneToSubnetIdMap = Maps.newHashMap();
    if (vpcOptional.isPresent() && Objects.equals(lb.getVpcId(), vpcOptional.get().getVpcId())) {
      final List<SubnetType> subnets = EucalyptusActivityTasks.getInstance()
          .describeSubnetsByZone(lb.getVpcId(), true, requestedZones);
      CollectionUtils.putAll(subnets, zoneToSubnetIdMap, SubnetType.zone(), SubnetType.id());
      if (requestedZones.size() != zoneToSubnetIdMap.size()) {
        throw new InvalidConfigurationRequestException(
            "Cannot enable zone for VPC loadbalancer, default subnet not found.");
      }
    } else if (lb.getVpcId() != null) {
      throw new InvalidConfigurationRequestException("Cannot enable zone for VPC loadbalancer");
    }

    LoadBalancerHelper.checkVersion(lb, LoadBalancerDeploymentVersion.v4_2_0);
    if (lb.getVpcId() != null) {
      LoadBalancerHelper.checkVersion(lb, LoadBalancerDeploymentVersion.v4_3_0);
    }

    // check if requested AZ names are valid
    final List<LoadBalancerZoneView> enabledZones =
        Lists.newArrayList(Collections2.filter(lbFull.getZones(),
            arg0 -> arg0.getState().equals(LoadBalancerZone.STATE.InService)));

    final List<String> enabledZoneNames =
        Lists.newArrayList(Iterables.transform(enabledZones, LoadBalancerZoneView::getName));

    requestedZones.removeAll(enabledZoneNames);

    /// make sure the clusters match the requested zone
    final List<ClusterInfoType> clusters =
        EucalyptusActivityTasks.getInstance().describeAvailabilityZones(false);
    final List<String> foundZones =
        Stream.ofAll(clusters).map(ClusterInfoType::getZoneName).toJavaList();

    for (final String zone : requestedZones) {
      if (!foundZones.contains(zone)) {
        throw new InvalidConfigurationRequestException(
            String.format("The requested zone %s is not valid", zone));
      }
    }

    if (!requestedZones.isEmpty()) {
      try {
        if (!LoadBalancingWorkflows.enableZonesSync(ctx.getAccountNumber(), lbName,
            Lists.newArrayList(requestedZones), zoneToSubnetIdMap)) {
          throw new Exception("Workflow for enabling availability zone has failed");
        }
      } catch (final Exception e) {
        final String reason =
            e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage()
                : "internal error";
        throw new InternalFailure400Exception(String.format("Failed to enable zones: %s", reason),
            e);
      }
    }

    allZones.addAll(requestedZones);

    final EnableAvailabilityZonesForLoadBalancerResult result =
        new EnableAvailabilityZonesForLoadBalancerResult();
    final AvailabilityZones availZones = new AvailabilityZones();
    availZones.getMember().addAll(allZones);
    reply.getEnableAvailabilityZonesForLoadBalancerResult().setAvailabilityZones(availZones);

    return reply;
  }

  public DisableAvailabilityZonesForLoadBalancerResponseType disableAvailabilityZonesForLoadBalancer(
      DisableAvailabilityZonesForLoadBalancerType request) throws EucalyptusCloudException {
    final DisableAvailabilityZonesForLoadBalancerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    Collection<String> zones = request.getAvailabilityZones().getMember();

    LoadBalancerView lb;
    Collection<LoadBalancerZoneView> lbZones;
    try (final TransactionResource tx = Entities.transactionFor(LoadBalancer.class)) {
      final LoadBalancer lbEntity = LoadBalancerHelper.getLoadbalancer(
          loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(),
          Functions.identity(),
          ctx.getAccountNumber(),
          lbName);
      lb = lbEntity;
      lbZones = Stream.ofAll(lbEntity.getZones())
          .<LoadBalancerZoneView>map(ImmutableLoadBalancerZoneView::copyOf)
          .toJavaList();
    } catch (final Exception ex) {
      throw new AccessPointNotFoundException();
    }

    // check for a default VPC
    final Optional<VpcType> vpcOptional =
        EucalyptusActivityTasks.getInstance().defaultVpc(ctx.getAccount());
    final Map<String, String> zoneToSubnetIdMap = Maps.newHashMap();
    if (vpcOptional.isPresent() && Objects.equals(lb.getVpcId(), vpcOptional.get().getVpcId())) {
      final List<SubnetType> subnets =
          EucalyptusActivityTasks.getInstance().describeSubnetsByZone(lb.getVpcId(), true, zones);
      CollectionUtils.putAll(subnets, zoneToSubnetIdMap, SubnetType.zone(), SubnetType.id());
      if (zones.size() != zoneToSubnetIdMap.size()) {
        throw new InvalidConfigurationRequestException(
            "Cannot disable zone for VPC loadbalancer, default subnet not found.");
      }
    } else if (lb.getVpcId() != null) {
      throw new InvalidConfigurationRequestException("Cannot disable zone for VPC loadbalancer");
    }

    LoadBalancerHelper.checkVersion(lb, LoadBalancerDeploymentVersion.v4_2_0);
    if (lb.getVpcId() != null) {
      LoadBalancerHelper.checkVersion(lb, LoadBalancerDeploymentVersion.v4_3_0);
    }

    /// validate the requested zones
    final List<LoadBalancerZoneView> availableZones =
        Lists.newArrayList(
            Collections2.filter(lbZones, arg0 -> arg0.getState().equals(STATE.InService)));

    final Set<String> inServiceZoneNames =
        Sets.newHashSet(Collections2.transform(availableZones, LoadBalancerZoneView::getName));

    zones = Stream.ofAll(zones)
        .filter(inServiceZoneNames::contains)
        .toJavaList();

    if (zones != null && zones.size() > 0) {
      inServiceZoneNames.removeAll(zones);
      if (inServiceZoneNames.size() <= 0) {
        throw new InvalidConfigurationRequestException(
            "There must be at least one availability zone");
      }

      try {
        if (!LoadBalancingWorkflows.disableZonesSync(ctx.getAccountNumber(),
            lbName, Lists.newArrayList(zones))) {
          throw new Exception("Workflow for disabling availability zone has failed");
        }
      } catch (final Exception e) {
        final String reason =
            e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage()
                : "internal error";
        throw new InternalFailure400Exception(String.format("Failed to disable zones: %s", reason),
            e);
      }
    }

    List<String> availableZoneNames = Lists.newArrayList();
    try (final TransactionResource tx = Entities.transactionFor(LoadBalancer.class)) {
      final LoadBalancer updatedLb =
          LoadBalancerHelper.getLoadbalancer(loadBalancingPersistence, ctx.getAccountNumber(),
              lbName);
      availableZoneNames = Stream.ofAll(LoadBalancerHelper.findZonesInService(updatedLb))
          .map(LoadBalancerZoneView::getName)
          .toJavaList();
    } catch (Exception ex) {
    }
    final DisableAvailabilityZonesForLoadBalancerResult result =
        new DisableAvailabilityZonesForLoadBalancerResult();
    final AvailabilityZones availZones = new AvailabilityZones();
    availZones.setMember(Lists.newArrayList(availableZoneNames));
    result.setAvailabilityZones(availZones);
    reply.setDisableAvailabilityZonesForLoadBalancerResult(result);
    reply.set_return(true);
    return reply;
  }

  public ConfigureHealthCheckResponseType configureHealthCheck(ConfigureHealthCheckType request)
      throws EucalyptusCloudException {
    ConfigureHealthCheckResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final HealthCheck hc = request.getHealthCheck();
    final Integer healthyThreshold = hc.getHealthyThreshold();
    if (healthyThreshold == null) {
      throw new InvalidConfigurationRequestException("Healthy tresholds must be specified");
    }
    final Integer interval = hc.getInterval();
    if (interval == null) {
      throw new InvalidConfigurationRequestException("Interval must be specified");
    }
    final String target = hc.getTarget();
    if (target == null) {
      throw new InvalidConfigurationRequestException("Target must be specified");
    }

    final Integer timeout = hc.getTimeout();
    if (timeout == null) {
      throw new InvalidConfigurationRequestException("Timeout must be specified");
    }
    final Integer unhealthyThreshold = hc.getUnhealthyThreshold();
    if (unhealthyThreshold == null) {
      throw new InvalidConfigurationRequestException("Unhealthy tresholds must be specified");
    }

    if (interval < MIN_HEALTHCHECK_INTERVAL_SEC) {
      throw new InvalidConfigurationRequestException(
          String.format("Interval must be longer than %d seconds", MIN_HEALTHCHECK_INTERVAL_SEC));
    }

    if (interval > MAX_HEALTHCHECK_INTERVAL_SEC) {
      throw new InvalidConfigurationRequestException(
          String.format("Interval must be smaller than %d seconds", MAX_HEALTHCHECK_INTERVAL_SEC));
    }

    if (healthyThreshold < MIN_HEALTHCHECK_THRESHOLDS) {
      throw new InvalidConfigurationRequestException(
          String.format("Healthy thresholds must be larger than %d", MIN_HEALTHCHECK_THRESHOLDS));
    }
    if (unhealthyThreshold < MIN_HEALTHCHECK_THRESHOLDS) {
      throw new InvalidConfigurationRequestException(
          String.format("Unhealthy thresholds must be larger than %d", MIN_HEALTHCHECK_THRESHOLDS));
    }

    try (final TransactionResource db = Entities.transactionFor(LoadBalancer.class)) {
      final LoadBalancer update = LoadBalancerHelper.getLoadbalancer(
          loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(),
          Functions.identity(),
          ctx.getAccountNumber(),
          lbName);
      update.setHealthCheck(healthyThreshold, interval, target, timeout, unhealthyThreshold);
      hc.setTarget(update.getHealthCheckTarget());
      db.commit();
    } catch (NoSuchElementException ex) {
      throw new AccessPointNotFoundException();
    } catch (final IllegalArgumentException ex) {
      throw new InvalidConfigurationRequestException(ex.getMessage());
    } catch (final Exception ex) {
      LOG.error("failed to persist health check config", ex);
      throw new InternalFailure400Exception("Failed to persist the health check config", ex);
    }

    LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    ConfigureHealthCheckResult result = new ConfigureHealthCheckResult();
    result.setHealthCheck(hc);
    reply.setConfigureHealthCheckResult(result);
    return reply;
  }

  public DescribeInstanceHealthResponseType describeInstanceHealth(
      DescribeInstanceHealthType request) throws EucalyptusCloudException {
    final DescribeInstanceHealthResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final Instances instances = request.getInstances();

    final List<LoadBalancerBackendInstanceView> lbInstances;
    try (final TransactionResource tx = Entities.transactionFor(LoadBalancer.class)) {
      final LoadBalancer lb =
          lookupAuthorizedByNameOrDnsName(loadBalancingPersistence, ctx.getAccountNumber(), lbName);
      lbInstances = Lists.newArrayList(Iterables.transform(lb.getBackendInstances(),
          ImmutableLoadBalancerBackendInstanceView::copyOf));
    } catch (final LoadBalancingException e) {
      throw e;
    } catch (Exception ex) {
      throw new InternalFailureException("Failed to find the loadbalancer");
    }

    List<LoadBalancerBackendInstanceView> instancesFound;

    if (instances != null && instances.getMember() != null && instances.getMember().size() > 0) {
      instancesFound = Lists.newArrayList();
      for (Instance inst : instances.getMember()) {
        String instId = inst.getInstanceId();
        for (final LoadBalancerBackendInstanceView lbInstance : lbInstances) {
          if (instId.equals(lbInstance.getInstanceId())) {
            instancesFound.add(lbInstance);
            break;
          }
        }
      }
    } else {
      instancesFound = lbInstances;
    }

    final ArrayList<InstanceState> stateList = Lists.newArrayList();
    for (final LoadBalancerBackendInstanceView instance : instancesFound) {
      InstanceState state = new InstanceState();
      state.setInstanceId(instance.getDisplayName());
      state.setState(instance.getState().name());
      if (instance.getReasonCode() != null) {
        state.setReasonCode(instance.getReasonCode());
      }
      if (instance.getDescription() != null) {
        state.setDescription(instance.getDescription());
      }
      stateList.add(state);
    }

    final InstanceStates states = new InstanceStates();
    states.setMember(stateList);
    final DescribeInstanceHealthResult result = new DescribeInstanceHealthResult();
    result.setInstanceStates(states);
    reply.setDescribeInstanceHealthResult(result);
    return reply;
  }

  public SetLoadBalancerListenerSSLCertificateResponseType setLoadBalancerListenerSSLCertificate(
      SetLoadBalancerListenerSSLCertificateType request) throws EucalyptusCloudException {
    final SetLoadBalancerListenerSSLCertificateResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final int lbPort = request.getLoadBalancerPort();
    final String certArn = request.getSSLCertificateId();

    if (lbPort <= 0 || lbPort > 65535) {
      throw new InvalidConfigurationRequestException("Invalid port");
    }

    if (certArn == null || certArn.length() <= 0) {
      throw new InvalidConfigurationRequestException("SSLCertificateId is not specified");
    }

    final LoadBalancerZonesView lb;
    final List<LoadBalancerListenerView> listeners;
    try {
      final Tuple2<LoadBalancerZonesView, List<LoadBalancerListenerView>> lbData
          = LoadBalancerHelper.getLoadbalancer(
          loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(),
          lbEntity -> Tuple.of(
              LoadBalancers.ZONES_VIEW.apply(lbEntity),
              Lists.newArrayList(Iterables.transform(
                  lbEntity.getListeners(),
                  ImmutableLoadBalancerListenerView::copyOf))
          ),
          ctx.getAccount(),
          lbName);
      lb = lbData._1();
      listeners = lbData._2();
    } catch (NoSuchElementException ex) {
      throw new AccessPointNotFoundException();
    } catch (Exception ex) {
      throw new InternalFailure400Exception("Failed to find the loadbalancer");
    }

    LoadBalancerHelper.checkWorkerCertificateExpiration(lb);

    try {
      LoadBalancerHelper.setLoadBalancerListenerSSLCertificate(loadBalancingPersistence,
          lb.getLoadBalancer(), listeners, lbPort, certArn);
      LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    } catch (final LoadBalancingException ex) {
      throw ex;
    } catch (final Exception ex) {
      LOG.error("Failed to set loadbalancer listener SSL certificate", ex);
      throw new InternalFailure400Exception("Failed to set loadbalancer listener SSL certificate",
          ex);
    }
    reply.setSetLoadBalancerListenerSSLCertificateResult(
        new SetLoadBalancerListenerSSLCertificateResult());
    reply.set_return(true);
    return reply;
  }

  public DescribeLoadBalancerPolicyTypesResponseType describeLoadBalancerPolicyTypes(
      DescribeLoadBalancerPolicyTypesType request) throws EucalyptusCloudException {
    final List<PolicyTypeDescription> policyTypes = Lists.newArrayList();
    Set<String> requestedTypeNames = Sets.newHashSet();
    if (request.getPolicyTypeNames() != null &&
        request.getPolicyTypeNames().getMember() != null) {
      requestedTypeNames.addAll(request.getPolicyTypeNames().getMember());
    }
    try {
      final List<LoadBalancerPolicyTypeDescriptionFullView> internalPolicyTypes =
          LoadBalancerPolicyHelper.getLoadBalancerPolicyTypeDescriptions();
      for (final LoadBalancerPolicyTypeDescriptionFullView from : internalPolicyTypes) {
        if (requestedTypeNames.isEmpty() || requestedTypeNames.contains(
            from.getPolicyTypeDescription().getPolicyTypeName())) {
          policyTypes.add(LoadBalancerPolicyHelper.AsPolicyTypeDescription.INSTANCE.apply(from));
        }
      }
    } catch (final Exception ex) {
      LOG.error("Failed to retrieve policy types", ex);
      throw new InternalFailure400Exception("Failed to retrieve policy types", ex);
    }
    final DescribeLoadBalancerPolicyTypesResponseType reply = request.getReply();
    final PolicyTypeDescriptions desc = new PolicyTypeDescriptions();
    desc.setMember((ArrayList<PolicyTypeDescription>) policyTypes);
    final DescribeLoadBalancerPolicyTypesResult result =
        new DescribeLoadBalancerPolicyTypesResult();
    result.setPolicyTypeDescriptions(desc);
    reply.setDescribeLoadBalancerPolicyTypesResult(result);
    reply.set_return(true);
    return reply;
  }

  public DescribeLoadBalancerPoliciesResponseType describeLoadBalancerPolicies(
      DescribeLoadBalancerPoliciesType request) throws EucalyptusCloudException {
    DescribeLoadBalancerPoliciesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final PolicyNames policyNames = request.getPolicyNames();

    if (lbName == null || lbName.isEmpty()) {
      // return sample policies according to ELB API
      final DescribeLoadBalancerPoliciesResult result = new DescribeLoadBalancerPoliciesResult();
      final PolicyDescriptions descs = new PolicyDescriptions();
      final List<PolicyDescription> policies =
          LoadBalancerPolicyHelper.getSamplePolicyDescription();
      descs.setMember((ArrayList<PolicyDescription>) policies);
      result.setPolicyDescriptions(descs);
      reply.setDescribeLoadBalancerPoliciesResult(result);
    } else {
      final List<LoadBalancerPolicyDescriptionFullView> lbPolicies;
      try {
        lbPolicies = lookupAuthorizedByNameOrDnsName(
            loadBalancingPersistence,
            ctx.getAccountNumber(),
            lbName,
            loadBalancer -> {
              final List<LoadBalancerPolicyDescription> policies;
              try {
                if (policyNames != null
                    && policyNames.getMember() != null
                    && policyNames.getMember().size() > 0) {
                  policies = LoadBalancerPolicyHelper.getLoadBalancerPolicyDescription(loadBalancer,
                      policyNames.getMember());
                } else {
                  policies = Lists.newArrayList(loadBalancer.getPolicyDescriptions());
                }
              } catch (final Exception ex) {
                LOG.error("Failed to find policy descriptions", ex);
                throw Exceptions.toUndeclared(
                    new InternalFailure400Exception("Failed to retrieve the policy descriptions",
                        ex));
              }
              return Lists.newArrayList(
                  Iterables.transform(policies, LoadBalancers.POLICY_DESCRIPTION_FULL_VIEW));
            });
      } catch (final Exception ex) {
        throw handleException(ex);
      }

      final DescribeLoadBalancerPoliciesResult result = new DescribeLoadBalancerPoliciesResult();
      final PolicyDescriptions descs = new PolicyDescriptions();
      final List<PolicyDescription> policies = Lists.newArrayList();
      for (final LoadBalancerPolicyDescriptionFullView lbPolicy : lbPolicies) {
        policies.add(LoadBalancerPolicyHelper.AsPolicyDescription.INSTANCE.apply(lbPolicy));
      }
      descs.setMember((ArrayList<PolicyDescription>) policies);
      result.setPolicyDescriptions(descs);
      reply.setDescribeLoadBalancerPoliciesResult(result);
    }

    reply.set_return(true);
    return reply;
  }

  public CreateLoadBalancerPolicyResponseType createLoadBalancerPolicy(
      CreateLoadBalancerPolicyType request) throws EucalyptusCloudException {
    CreateLoadBalancerPolicyResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final String policyName = request.getPolicyName();
    final String policyTypeName = request.getPolicyTypeName();
    if (lbName == null || lbName.isEmpty()) {
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    }
    if (policyName == null || policyName.isEmpty()) {
      throw new InvalidConfigurationRequestException("policy name must be specified");
    }
    if (policyTypeName == null || policyTypeName.isEmpty()) {
      throw new InvalidConfigurationRequestException("policy type name must be specified");
    }

    try {
      loadBalancingPersistence.balancers().updateByExample(
          LoadBalancer.namedByAccountId(ctx.getAccountNumber(), lbName),
          ctx.getUserFullName(),
          lbName,
          LoadBalancingMetadatas.filterPrivileged(),
          lb -> {
            final List<PolicyAttribute> attrs =
                (request.getPolicyAttributes() != null ? request.getPolicyAttributes().getMember()
                    : Lists.<PolicyAttribute>newArrayList());
            try {
              final LoadBalancerPolicyDescription policyDesc =
                  LoadBalancerPolicyHelper.addLoadBalancerPolicy(lb, policyName, policyTypeName,
                      attrs);
              Entities.persist(policyDesc);
            } catch (LoadBalancingException e) {
              throw Exceptions.toUndeclared(e);
            }
            return lb;
          });
    } catch (final LoadBalancingMetadataNotFoundException ex) {
      throw new AccessPointNotFoundException();
    } catch (final Exception ex) {
      throw handleException(ex);
    }

    try {
      LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    } catch (final Exception ex) {
      LOG.error("Failed to add the policy", ex);
      throw new InternalFailure400Exception("Failed to add the policy", ex);
    }
    reply.set_return(true);
    return reply;
  }

  public DeleteLoadBalancerPolicyResponseType deleteLoadBalancerPolicy(
      DeleteLoadBalancerPolicyType request) throws EucalyptusCloudException {
    final DeleteLoadBalancerPolicyResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final String policyName = request.getPolicyName();
    if (lbName == null || lbName.isEmpty()) {
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    }
    if (policyName == null || policyName.isEmpty()) {
      throw new InvalidConfigurationRequestException("policy name must be specified");
    }

    try {
      loadBalancingPersistence.balancers().updateByExample(
          LoadBalancer.namedByAccountId(ctx.getAccountNumber(), lbName),
          ctx.getUserFullName(),
          lbName,
          LoadBalancingMetadatas.filterPrivileged(),
          loadBalancer -> {
            try {
              LoadBalancerPolicyHelper.deleteLoadBalancerPolicy(loadBalancer, policyName);
            } catch (LoadBalancingException e) {
              throw Exceptions.toUndeclared(e);
            }
            return loadBalancer;
          }
      );
    } catch (LoadBalancingMetadataNotFoundException ex) {
      throw new AccessPointNotFoundException();
    } catch (final Exception ex) {
      throw handleException(ex);
    }

    try {
      LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    } catch (final Exception ex) {
      LOG.error("Failed to delete policy", ex);
      throw new InternalFailure400Exception("Failed to delete policy", ex);
    }

    reply.set_return(true);
    return reply;
  }

  public CreateLBCookieStickinessPolicyResponseType createLBCookieStickinessPolicy(
      CreateLBCookieStickinessPolicyType request) throws EucalyptusCloudException {
    CreateLBCookieStickinessPolicyResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final String policyName = request.getPolicyName();
    final Long expiration = request.getCookieExpirationPeriod();

    if (lbName == null || lbName.isEmpty()) {
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    }
    if (policyName == null || policyName.isEmpty()) {
      throw new InvalidConfigurationRequestException("Policy name must be specified");
    }
    if (expiration == null || expiration <= 0) {
      throw new InvalidConfigurationRequestException("Expiration period must be bigger than 0");
    }

    try {
      loadBalancingPersistence.balancers().updateByExample(
          LoadBalancer.namedByAccountId(ctx.getAccountNumber(), lbName),
          ctx.getUserFullName(),
          lbName,
          LoadBalancingMetadatas.filterPrivileged(),
          lb -> {
            final PolicyAttribute attr = new PolicyAttribute();
            attr.setAttributeName("CookieExpirationPeriod");
            attr.setAttributeValue(expiration.toString());
            try {
              final LoadBalancerPolicyDescription policyDesc =
                  LoadBalancerPolicyHelper.addLoadBalancerPolicy(lb, policyName,
                      "LBCookieStickinessPolicyType",
                      Lists.newArrayList(attr));
              Entities.persist(policyDesc);
            } catch (LoadBalancingException e) {
              throw Exceptions.toUndeclared(e);
            }
            return lb;
          });
    } catch (final LoadBalancingMetadataNotFoundException ex) {
      throw new AccessPointNotFoundException();
    } catch (final Exception ex) {
      throw handleException(ex);
    }

    try {
      LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    } catch (final Exception ex) {
      LOG.error("Failed to create policy", ex);
      throw new InternalFailure400Exception("Failed to create policy", ex);
    }

    reply.set_return(true);
    return reply;
  }

  public CreateAppCookieStickinessPolicyResponseType createAppCookieStickinessPolicy(
      CreateAppCookieStickinessPolicyType request) throws EucalyptusCloudException {
    CreateAppCookieStickinessPolicyResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final String policyName = request.getPolicyName();
    final String cookieName = request.getCookieName();

    if (lbName == null || lbName.isEmpty()) {
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    }
    if (policyName == null || policyName.isEmpty()) {
      throw new InvalidConfigurationRequestException("Policy name must be specified");
    }
    if (cookieName == null) {
      throw new InvalidConfigurationRequestException("Cookie name must be specified");
    }

    try {
      loadBalancingPersistence.balancers().updateByExample(
          LoadBalancer.namedByAccountId(ctx.getAccountNumber(), lbName),
          ctx.getUserFullName(),
          lbName,
          LoadBalancingMetadatas.filterPrivileged(),
          lb -> {
            final PolicyAttribute attr = new PolicyAttribute();
            attr.setAttributeName("CookieName");
            attr.setAttributeValue(cookieName);
            try {
              final LoadBalancerPolicyDescription policyDesc =
                  LoadBalancerPolicyHelper.addLoadBalancerPolicy(lb,
                      policyName,
                      "AppCookieStickinessPolicyType",
                      Lists.newArrayList(attr));
              Entities.persist(policyDesc);
            } catch (LoadBalancingException e) {
              throw Exceptions.toUndeclared(e);
            }
            return lb;
          });
    } catch (final LoadBalancingMetadataNotFoundException ex) {
      throw new AccessPointNotFoundException();
    } catch (final Exception ex) {
      throw handleException(ex);
    }

    try {
      LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    } catch (final Exception ex) {
      LOG.error("Failed to create policy", ex);
      throw new InternalFailure400Exception("Failed to create policy", ex);
    }
    reply.set_return(true);
    return reply;
  }

  public SetLoadBalancerPoliciesOfListenerResponseType setLoadBalancerPoliciesOfListener(
      SetLoadBalancerPoliciesOfListenerType request) throws EucalyptusCloudException {
    final SetLoadBalancerPoliciesOfListenerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final int portNum = request.getLoadBalancerPort();
    final PolicyNames pNames = request.getPolicyNames();

    if (lbName == null || lbName.isEmpty()) {
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    }
    if (portNum < 0 || portNum > 65535) {
      throw new InvalidConfigurationRequestException("Invalid port number specified");
    }
    final List<String> policyNames = pNames.getMember();

    try {
      loadBalancingPersistence.balancers().updateByExample(
          LoadBalancer.namedByAccountId(ctx.getAccountNumber(), lbName),
          ctx.getUserFullName(),
          lbName,
          LoadBalancingMetadatas.filterPrivileged(),
          lb -> {
            final LoadBalancerListener listener = findListener(lb, portNum);
            if (listener == null) {
              throw Exceptions.toUndeclared(new ListenerNotFoundException());
            }

            final List<LoadBalancerPolicyDescription> policies = Lists.newArrayList();
            if (policyNames != null) {
              for (final String policyName : policyNames) {
                try {
                  final LoadBalancerPolicyDescription p =
                      LoadBalancerPolicyHelper.getLoadBalancerPolicyDescription(lb, policyName);
                  final String policyType = p.getPolicyTypeName();
                  if (!("SSLNegotiationPolicyType".equals(policyType)
                      || "LBCookieStickinessPolicyType".equals(policyType)
                      || "AppCookieStickinessPolicyType".equals(policyType))) {
                    throw new InvalidConfigurationRequestException(
                        policyType + " cannot be set to listeners");
                  }
                  policies.add(p);
                } catch (final LoadBalancingException ex) {
                  throw Exceptions.toUndeclared(ex);
                } catch (final Exception ex) {
                  throw Exceptions.toUndeclared(new PolicyNotFoundException());
                }
              }
            }
            listener.resetPolicies();
            if (!policies.isEmpty()) {
              try {
                LoadBalancerPolicyHelper.addPoliciesToListener(listener, policies);
              } catch (final LoadBalancingException ex) {
                throw Exceptions.toUndeclared(ex);
              }
            }

            return lb;
          }
      );
    } catch (final LoadBalancingMetadataNotFoundException ex) {
      throw new AccessPointNotFoundException();
    } catch (final Exception ex) {
      throw handleException(ex);
    }

    try {
      LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    } catch (final Exception ex) {
      LOG.error("Failed to set policies to listener", ex);
      throw new InternalFailure400Exception("Failed to set policies to listener", ex);
    }

    return reply;
  }

  public SetLoadBalancerPoliciesForBackendServerResponseType setLoadBalancerPoliciesForBackendServer(
      SetLoadBalancerPoliciesForBackendServerType request) throws EucalyptusCloudException {
    final SetLoadBalancerPoliciesForBackendServerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final int instancePort = request.getInstancePort();
    final PolicyNames pNames = request.getPolicyNames();

    if (lbName == null || lbName.isEmpty()) {
      throw new InvalidConfigurationRequestException("Loadbalancer name must be specified");
    }
    if (instancePort < 0 || instancePort > 65535) {
      throw new InvalidConfigurationRequestException("Invalid port number specified");
    }
    final List<String> policyNames = pNames.getMember();

    try {
      loadBalancingPersistence.balancers().updateByExample(
          LoadBalancer.namedByAccountId(ctx.getAccountNumber(), lbName),
          ctx.getUserFullName(),
          lbName,
          LoadBalancingMetadatas.filterPrivileged(),
          lb -> {
            final Set<String> policyTypes = Sets.newHashSet();
            final List<LoadBalancerPolicyDescription> policiesToAdd = Lists.newArrayList();
            if (policyNames != null) {
              for (final String policyName : policyNames) {
                try {
                  final LoadBalancerPolicyDescription policy =
                      LoadBalancerPolicyHelper.getLoadBalancerPolicyDescription(lb, policyName);
                  if (!"BackendServerAuthenticationPolicyType".equals(policy.getPolicyTypeName())) {
                    throw new InvalidConfigurationRequestException(
                        "Only BackendServerAuthenticationPolicyType can be set to backend server");
                  }
                  policyTypes.add(policy.getPolicyTypeName());
                  policiesToAdd.add(policy);
                } catch (final LoadBalancingException ex) {
                  throw Exceptions.toUndeclared(ex);
                } catch (final Exception ex) {
                  throw Exceptions.toUndeclared(new PolicyNotFoundException());
                }
              }
            }
            boolean listenerFound = false;
            for (final LoadBalancerListener l : lb.getListeners()) {
              if (l.getInstancePort() == instancePort) {
                if (policyTypes.contains("BackendServerAuthenticationPolicyType") &&
                    !(PROTOCOL.HTTPS.equals(l.getInstanceProtocol()) || PROTOCOL.SSL.equals(
                        l.getInstanceProtocol()))) {
                  throw Exceptions.toUndeclared(new InvalidConfigurationRequestException(
                      "Policies of BackendServerAuthenticationPolicyType can be set to only HTTPS/SSL instance protocol"));
                }
                listenerFound = true;
                break;
              }
            }
            if (!listenerFound) {
              throw Exceptions.toUndeclared(new InvalidConfigurationRequestException(
                  "Listener with the specified backend instance port is not found"));
            }

            LoadBalancerBackendServerDescription backend;
            if (!LoadBalancerBackendServerHelper.hasBackendServerDescription(lb, instancePort)) {
              backend = LoadBalancerBackendServerDescription.named(lb, instancePort);
              Entities.persist(backend);
            } else {
              backend =
                  LoadBalancerBackendServerHelper.getBackendServerDescription(lb, instancePort);
            }
            if (backend == null) {
              throw Exceptions.toUndeclared(new InvalidConfigurationRequestException(
                  "Failed to find the backend server description for port " + instancePort));
            }

            try {
              LoadBalancerPolicyHelper.clearPoliciesFromBackendServer(backend);
              if (policiesToAdd.size() > 0) {
                LoadBalancerPolicyHelper.addPoliciesToBackendServer(backend, policiesToAdd);
              }
            } catch (final Exception ex) {
              throw Exceptions.toUndeclared(ex);
            }
            return lb;
          }
      );
    } catch (final LoadBalancingMetadataNotFoundException ex) {
      throw new AccessPointNotFoundException();
    } catch (final Exception ex) {
      throw handleException(ex);
    }

    try {
      LoadBalancingWorkflows.updateLoadBalancer(ctx.getAccountNumber(), lbName);
    } catch (final Exception ex) {
      LOG.error("Failed to set policies to backend server description", ex);
      throw new InternalFailure400Exception("Failed to set policies to backend server description",
          ex);
    }
    return reply;
  }

  public ApplySecurityGroupsToLoadBalancerResponseType applySecurityGroupsToLoadBalancer(
      final ApplySecurityGroupsToLoadBalancerType request
  ) throws EucalyptusCloudException {
    final ApplySecurityGroupsToLoadBalancerResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccountNumber();
    final Set<String> securityGroupIds = Sets.newHashSet(request.getSecurityGroups().getMember());
    final List<SecurityGroupItemType> groups =
        EucalyptusActivityTasks.getInstance()
            .describeUserSecurityGroupsById(ctx.getAccount(), null, securityGroupIds);
    if (groups.size() != securityGroupIds.size()) {
      throw new LoadBalancingClientException("InvalidSecurityGroup", "Invalid security group(s)");
    }

    final Function<String, Map<String, String>> updateSecurityGroups =
        new Function<String, Map<String, String>>() {
          @Override
          public Map<String, String> apply(final String identifier) {
            try {
              final LoadBalancer example = LoadBalancer.namedByAccountId(accountNumber, identifier);
              final LoadBalancer loadBalancer = Entities.uniqueResult(example);
              if (LoadBalancingMetadatas.filterPrivileged().apply(loadBalancer)) {
                if (loadBalancer.getVpcId() == null) {
                  throw Exceptions.toUndeclared(
                      new InvalidConfigurationRequestException("VPC only"));
                }

                LoadBalancerHelper.checkVersion(loadBalancer, LoadBalancerDeploymentVersion.v4_3_0);

                for (final SecurityGroupItemType group : groups) {
                  if (!loadBalancer.getVpcId().equals(group.getVpcId())) {
                    throw Exceptions.toUndeclared(new InvalidConfigurationRequestException(
                        String.format("Security group \"%s\" does not belong to VPC \"%s\"",
                            group.getGroupId(), loadBalancer.getVpcId())));
                  }
                }

                final List<SecurityGroupItemType> sortedGroups =
                    Ordering.natural()
                        .onResultOf(SecurityGroupItemType.groupId())
                        .sortedCopy(groups);
                loadBalancer.setSecurityGroupRefs(Lists.newArrayList(Iterables.transform(
                    sortedGroups,
                    TypeMappers.lookup(SecurityGroupItemType.class,
                        LoadBalancerSecurityGroupRef.class))));

                final Map<String, String> groupIdToNameMap = CollectionUtils.putAll(
                    sortedGroups,
                    Maps.<String, String>newLinkedHashMap(),
                    SecurityGroupItemType.groupId(),
                    SecurityGroupItemType.groupName());
                return groupIdToNameMap;
              } else {
                throw new NoSuchElementException();
              }
            } catch (NoSuchElementException e) {
              throw Exceptions.toUndeclared(new AccessPointNotFoundException());
            } catch (TransactionException e) {
              throw Exceptions.toUndeclared(e);
            } catch (LoadBalancerVersionException e) {
              throw Exceptions.toUndeclared(e);
            }
          }
        };

    final Map<String, String> groupIdToNameMap;
    try {
      groupIdToNameMap = Entities.asTransaction(LoadBalancer.class, updateSecurityGroups)
          .apply(request.getLoadBalancerName());
    } catch (Exception e) {
      throw handleException(e);
    }

    try {
      if (!LoadBalancingWorkflows.applySecurityGroupsSync(accountNumber,
          request.getLoadBalancerName(), groupIdToNameMap)) {
        ;
      }
    } catch (final Exception ex) {
      LOG.error("Workflow for applying security groups failed", ex);
      final String reason =
          Optional.fromNullable(ex.getCause()).transform(Exceptions.message()).or("internal error");
      throw new InternalFailure400Exception(
          String.format("Failed to apply security groups to loadbalancer: %s", reason), ex);
    }

    reply.getApplySecurityGroupsToLoadBalancerResult().setSecurityGroups(
        new SecurityGroups(Collections2.transform(groups, SecurityGroupItemType.groupId()))
    );

    return reply;
  }

  public AttachLoadBalancerToSubnetsResponseType attachLoadBalancerToSubnets(
      final AttachLoadBalancerToSubnetsType request
  ) throws EucalyptusCloudException {
    final AttachLoadBalancerToSubnetsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final Collection<String> requestedSubnetIds = request.getSubnets().getMember();

    final String vpcId;
    final BiMap<String, String> zoneToSubnetIdMap = HashBiMap.create();
    try (final TransactionResource rx = Entities.transactionFor(LoadBalancer.class)) {
      final LoadBalancer lb = LoadBalancerHelper.getLoadbalancer(
          loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(),
          loadBalancer -> loadBalancer,
          ctx.getAccountNumber(),
          lbName);

      vpcId = lb.getVpcId();
      CollectionUtils.putAll(lb.getZones().stream()
              .filter(az -> LoadBalancerZone.STATE.InService.equals(az.getState()))
              .collect(Collectors.toList()), zoneToSubnetIdMap,
          LoadBalancerZone::getName,
          LoadBalancerZone::getSubnetId);
    } catch (final NoSuchElementException ex) {
      throw new AccessPointNotFoundException();
    } catch (final Exception ex) {
      throw handleException(ex);
    }

    if (vpcId == null) {
      throw new InvalidConfigurationRequestException("Invalid subnet for load balancer");
    }

    final List<SubnetType> subnets =
        EucalyptusActivityTasks.getInstance().describeSubnets(requestedSubnetIds);
    if (subnets.size() != requestedSubnetIds.size()) {
      throw new LoadBalancingClientException("SubnetNotFound", "Invalid subnet(s)");
    }
    for (final SubnetType subnetType : subnets) {
      if (!vpcId.equals(subnetType.getVpcId())) {
        throw new InvalidConfigurationRequestException("Invalid subnet for load balancer");
      }
      final String previousSubnetId =
          zoneToSubnetIdMap.put(subnetType.getAvailabilityZone(), subnetType.getSubnetId());
      if (previousSubnetId != null && !previousSubnetId.equals(subnetType.getSubnetId())) {
        throw new InvalidConfigurationRequestException(
            "Multiple subnets for zone (" + subnetType.getAvailabilityZone() + ")");
      }
    }

    final List<String> requestedZones = Lists.newArrayList();
    Iterables.addAll(requestedZones,
        Iterables.transform(requestedSubnetIds, Functions.forMap(zoneToSubnetIdMap.inverse())));
    try {
      LoadBalancingWorkflows.enableZonesSync(ctx.getAccountNumber(), lbName,
          requestedZones, Maps.newHashMap(zoneToSubnetIdMap));
    } catch (final Exception e) {
      LOG.error("Workflow for enabling availablity zones failed", e);
      final String reason =
          e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage()
              : "internal error";
      throw new InternalFailure400Exception(String.format("Failed to enable zones: %s", reason), e);
    }

    final List<String> attachedSubnets = Lists.newArrayList();
    try (final TransactionResource rx = Entities.transactionFor(LoadBalancer.class)) {
      final LoadBalancer lbEntity =
          LoadBalancerHelper.getLoadbalancer(loadBalancingPersistence, ctx.getAccountNumber(),
              lbName);
      attachedSubnets.addAll(
          Collections2.transform(
              Collections2.filter(
                  lbEntity.getZones(),
                  zone -> STATE.InService.equals(zone.getState()) && zoneToSubnetIdMap.containsKey(
                      zone.getName())),
              zone -> zoneToSubnetIdMap.get(zone.getName()))
      );
    } catch (final Exception ex) {
      ;
    }

    final Subnets replySubnets = new Subnets();
    replySubnets.getMember().addAll(attachedSubnets);
    reply.getAttachLoadBalancerToSubnetsResult().setSubnets(replySubnets);
    return reply;
  }

  public DetachLoadBalancerFromSubnetsResponseType detachLoadBalancerFromSubnets(
      final DetachLoadBalancerFromSubnetsType request
  ) throws EucalyptusCloudException {
    final DetachLoadBalancerFromSubnetsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String lbName = request.getLoadBalancerName();
    final Collection<String> requestedSubnetIds = request.getSubnets().getMember();

    final BiMap<String, String> zoneToSubnetIdMap = HashBiMap.create();
    final String vpcId;
    final LoadBalancer lb;
    final Set<String> inServiceZones;
    try {
      final LoadBalancer lbEntity = LoadBalancerHelper.getLoadbalancer(
          loadBalancingPersistence,
          LoadBalancingMetadatas.filterPrivileged(),
          Functions.identity(),
          ctx.getAccountNumber(),
          lbName);
      lb = lbEntity;
      vpcId = lb.getVpcId();
      CollectionUtils.putAll(lbEntity.getZones(),
          zoneToSubnetIdMap,
          LoadBalancerZone::getName,
          LoadBalancerZone::getSubnetId);
      inServiceZones = lbEntity.getZones().stream()
          .filter(az -> LoadBalancerZone.STATE.InService.equals(az.getState()))
          .map(az -> az.getName())
          .collect(Collectors.toSet());
    } catch (final NoSuchElementException ex) {
      throw new AccessPointNotFoundException();
    } catch (final Exception ex) {
      throw handleException(ex);
    }

    if (vpcId == null) {
      throw new InvalidConfigurationRequestException("Invalid subnet for load balancer");
    }

    List<String> zones = Lists.newArrayList();
    Iterables.addAll(zones,
        Iterables.transform(requestedSubnetIds, Functions.forMap(zoneToSubnetIdMap.inverse())));
    zones = zones.stream()
        .filter(az -> inServiceZones.contains(az))
        .collect(Collectors.toList());
    if (!zones.isEmpty()) {
      inServiceZones.removeAll(zones);
      if (inServiceZones.size() <= 0) {
        throw new InvalidConfigurationRequestException(
            "Loadbalancer must be attached to at least one subnet");
      }
      try {
        if (!LoadBalancingWorkflows.disableZonesSync(ctx.getAccountNumber(),
            lbName, zones)) {
          throw new Exception("Workflow for disabling availability zones failed");
        }
      } catch (final Exception e) {
        final String reason =
            e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage()
                : "internal error";
        throw new InternalFailure400Exception(String.format("Failed to disable zones: %s", reason),
            e);
      }
    }

    final List<String> attachedSubnets = Lists.newArrayList();
    try (final TransactionResource tx = Entities.transactionFor(LoadBalancer.class)) {
      final LoadBalancer lbEntity =
          LoadBalancerHelper.getLoadbalancer(loadBalancingPersistence, ctx.getAccountNumber(),
              lbName);
      attachedSubnets.addAll(
          Collections2.transform(
              Collections2.filter(
                  lbEntity.getZones(),
                  zone -> STATE.InService.equals(zone.getState()) && zoneToSubnetIdMap.containsKey(
                      zone.getName())),
              zone -> zoneToSubnetIdMap.get(zone.getName()))
      );
    } catch (final Exception ex) {
      ;
    }

    final Subnets replySubnets = new Subnets();
    replySubnets.getMember().addAll(attachedSubnets);
    reply.getDetachLoadBalancerFromSubnetsResult().setSubnets(replySubnets);
    return reply;
  }

  public AddTagsResponseType addTags(final AddTagsType request) throws EucalyptusCloudException {
    final AddTagsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccount().getAccountNumber();

    final Set<String> requestedNames = Sets.newHashSet();
    if (request.getLoadBalancerNames() != null) {
      requestedNames.addAll(request.getLoadBalancerNames().getMember());
    }
    if (requestedNames.size() > 1) {
      throw new LoadBalancingClientException("InvalidParameterValue", "Too many load balancers");
    }

    final Map<String, String> tags = Maps.newHashMap();
    if (request.getTags() != null) {
      for (final Tag tag : request.getTags().getMember()) {
        if (tags.put(tag.getKey(), Strings.nullToEmpty(tag.getValue())) != null) {
          throw new LoadBalancingClientException("DuplicateTagKeys",
              "Duplicate tag key (" + tag.getKey() + ")");
        }
      }
      final int reservedTags =
          Iterables.size(Iterables.filter(tags.keySet(), isReservedTagPrefix()));
      if (reservedTags > 0 && !Contexts.lookup().isPrivileged()) {
        throw new InvalidConfigurationRequestException("Invalid tag key (reserved prefix)");
      }
    }

    try {
      final String identifer = Iterables.getOnlyElement(requestedNames);
      final LoadBalancer example = LoadBalancer.namedByAccountId(accountNumber, identifer);
      loadBalancingPersistence.balancers().updateByExample(
          example,
          ctx.getAccount(),
          identifer,
          LoadBalancingMetadatas.filterPrivileged(),
          loadBalancer -> {
            final Map<String, String> lbTags = loadBalancer.getTags();
            lbTags.putAll(tags);
            if (Iterables.size(
                Iterables.filter(lbTags.keySet(), Predicates.not(isReservedTagPrefix()))) >
                LoadBalancingServiceProperties.getMaxTags()) {
              throw Exceptions.toUndeclared(
                  new LoadBalancingClientException("TooManyTags", "Tag limit exceeded"));
            }
            return loadBalancer;
          }
      );
    } catch (final LoadBalancingMetadataNotFoundException e) {
      throw new AccessPointNotFoundException();
    } catch (Exception e) {
      throw handleException(e);
    }

    return reply;
  }

  public DescribeTagsResponseType describeTags(
      final DescribeTagsType request
  ) throws EucalyptusCloudException {
    final DescribeTagsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccount().getAccountNumber();
    final Set<String> requestedNames = Sets.newHashSet();
    if (request.getLoadBalancerNames() != null) {
      requestedNames.addAll(request.getLoadBalancerNames().getMember());
    }
    final boolean showAll = requestedNames.remove("verbose") && ctx.isAdministrator();
    final Function<Set<String>, TagDescriptions> lookupAccountLBs =
        new Function<Set<String>, TagDescriptions>() {
          @Override
          public TagDescriptions apply(final Set<String> identifiers) {
            try {
              final Predicate<? super LoadBalancer> requestedAndAccessible =
                  LoadBalancingMetadatas.filteringFor(LoadBalancer.class)
                      .byId(identifiers)
                      .byPrivileges()
                      .buildPredicate();

              final LoadBalancer example = showAll ?
                  LoadBalancer.named(null, null) :
                  LoadBalancer.namedByAccountId(accountNumber, null);
              final List<LoadBalancer> lbs = Entities.query(example, true);
              final TagDescriptions tagDescriptions = new TagDescriptions();
              for (final LoadBalancer loadBalancer : Iterables.filter(lbs,
                  requestedAndAccessible)) {
                final TagDescription tagDescription = new TagDescription();
                tagDescription.setLoadBalancerName(loadBalancer.getName());
                final TagList tagList = new TagList();
                tagDescription.setTags(tagList);
                for (final Map.Entry<String, String> tagKeyAndValue : loadBalancer.getTags()
                    .entrySet()) {
                  final Tag tag = new Tag();
                  tag.setKey(tagKeyAndValue.getKey());
                  tag.setValue(Strings.emptyToNull(tagKeyAndValue.getValue()));
                  tagList.getMember().add(tag);
                }
                tagDescriptions.getMember().add(tagDescription);
              }
              return tagDescriptions;
            } catch (EntityNotFoundException e) {
              Entities.evictCache(LoadBalancer.class);
              throw new OptimisticLockException("Error loading load balancers", e);
            }
          }
        };

    final TagDescriptions tagDescriptions =
        Entities.asTransaction(LoadBalancer.class, lookupAccountLBs).apply(requestedNames);

    reply.getDescribeTagsResult().setTagDescriptions(tagDescriptions);

    return reply;
  }

  public RemoveTagsResponseType removeTags(final RemoveTagsType request)
      throws EucalyptusCloudException {
    final RemoveTagsResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccount().getAccountNumber();

    final Set<String> requestedNames = Sets.newHashSet();
    if (request.getLoadBalancerNames() != null) {
      requestedNames.addAll(request.getLoadBalancerNames().getMember());
    }
    if (requestedNames.size() > 1) {
      throw new LoadBalancingClientException("InvalidParameterValue", "Too many load balancers");
    }

    final Set<String> tags = Sets.newHashSet();
    if (request.getTags() != null) {
      for (final TagKeyOnly tag : request.getTags().getMember()) {
        tags.add(tag.getKey());
      }
      final int reservedTags = Iterables.size(Iterables.filter(tags, isReservedTagPrefix()));
      if (reservedTags > 0 && !Contexts.lookup().isPrivileged()) {
        throw new InvalidConfigurationRequestException("Invalid tag key (reserved prefix)");
      }
    }

    try {
      final String identifer = Iterables.getOnlyElement(requestedNames);
      final LoadBalancer example = LoadBalancer.namedByAccountId(accountNumber, identifer);

      loadBalancingPersistence.balancers().updateByExample(
          example,
          ctx.getAccount(),
          identifer,
          LoadBalancingMetadatas.filterPrivileged(),
          loadBalancer -> {
            final Map<String, String> lbTags = loadBalancer.getTags();
            lbTags.keySet().removeAll(tags);
            return loadBalancer;
          }
      );
    } catch (final LoadBalancingMetadataNotFoundException e) {
      throw new AccessPointNotFoundException();
    } catch (Exception e) {
      throw handleException(e);
    }

    return reply;
  }

  public ModifyLoadBalancerAttributesResponseType modifyLoadBalancerAttributes(
      final ModifyLoadBalancerAttributesType request
  ) throws EucalyptusCloudException {
    final ModifyLoadBalancerAttributesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccount().getAccountNumber();

    try {
      LoadBalancerHelper.getLoadbalancer(loadBalancingPersistence, accountNumber,
          request.getLoadBalancerName());
    } catch (final NoSuchElementException ex) {
      throw new AccessPointNotFoundException();
    } catch (final Exception ex) {
      throw new InternalFailure400Exception(
          "Failed to modify attributes: unable to find the loadbalancer");
    }

    /************ Verify AccessLog attributes ************/
    final AccessLog accessLog = request.getLoadBalancerAttributes().getAccessLog();
    if (accessLog != null) {
      final boolean accessLogEnabled = accessLog.getEnabled();
      if (accessLogEnabled) {
        final String bucketName = accessLog.getS3BucketName();
        if (!EucalyptusActivityTasks.getInstance().bucketExists(ctx.getAccount(), bucketName)) {
          throw new InvalidConfigurationRequestException(
              String.format("S3Bucket: %s does not exist", bucketName));
        }
        final Integer emitInterval =
            MoreObjects.firstNonNull(accessLog.getEmitInterval(), 60);

        if (emitInterval < 5 || emitInterval > 60) {
          throw new InvalidConfigurationRequestException(
              "Access log's emit interval must be between 5 and 60 minutes");
        }
      }

      if (accessLog.getS3BucketPrefix() != null) {
        try {
          String escapedPrefix =
              new ObjectMapper().writeValueAsString(accessLog.getS3BucketPrefix());
          escapedPrefix = escapedPrefix.replaceAll("^\"|\"$", "");
          accessLog.setS3BucketPrefix(escapedPrefix);
        } catch (final Exception ex) {
          throw new InvalidConfigurationRequestException(
              "Invalid characters in bucket prefix text string");
        }
      }
    }
    /************ END Verify AccessLog attributes ************/
    try {
      if (!LoadBalancingWorkflows.modifyLoadBalancerAttributesSync(accountNumber,
          request.getLoadBalancerName(), request.getLoadBalancerAttributes())) {
        throw new Exception("Workflow for modifying attributes has failed");
      }
      LoadBalancingWorkflows.updateLoadBalancer(accountNumber, request.getLoadBalancerName());
    } catch (final Exception e) {
      final String reason =
          e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage()
              : "internal error";
      throw new InternalFailure400Exception(
          String.format("Failed to modify attributes: %s", reason), e);
    }

    final LoadBalancerAttributes attributes;
    try {
      final LoadBalancer example =
          LoadBalancer.namedByAccountId(accountNumber, request.getLoadBalancerName());
      attributes = loadBalancingPersistence.balancers().updateByExample(
          example,
          ctx.getAccount(),
          request.getLoadBalancerName(),
          LoadBalancingMetadatas.filterPrivileged(),
          loadBalancer -> {
            final ConnectionSettings connectionSettings =
                request.getLoadBalancerAttributes().getConnectionSettings();
            if (connectionSettings != null) {
              loadBalancer.setConnectionIdleTimeout(connectionSettings.getIdleTimeout());
            }
            final CrossZoneLoadBalancing crossZoneLb =
                request.getLoadBalancerAttributes().getCrossZoneLoadBalancing();
            if (crossZoneLb != null) {
              loadBalancer.setCrossZoneLoadbalancingEnabled(crossZoneLb.getEnabled());
            }
            return TypeMappers.transform(loadBalancer, LoadBalancerAttributes.class);
          }
      );
    } catch (final Exception e) {
      throw handleException(e);
    }

    reply.getModifyLoadBalancerAttributesResult().setLoadBalancerAttributes(attributes);
    return reply;
  }

  public DescribeLoadBalancerAttributesResponseType describeLoadBalancerAttributes(
      final DescribeLoadBalancerAttributesType request
  ) throws EucalyptusCloudException {
    final DescribeLoadBalancerAttributesResponseType reply = request.getReply();
    final Context ctx = Contexts.lookup();
    final String accountNumber = ctx.getAccount().getAccountNumber();
    final Function<String, LoadBalancerAttributes> lookupAttributes =
        new Function<String, LoadBalancerAttributes>() {
          @Override
          public LoadBalancerAttributes apply(final String identifier) {
            try {
              final LoadBalancer loadBalancer =
                  lookupAuthorizedByNameOrDnsName(loadBalancingPersistence, accountNumber,
                      identifier);
              return TypeMappers.transform(loadBalancer, LoadBalancerAttributes.class);
            } catch (Exception e) {
              throw Exceptions.toUndeclared(e);
            }
          }
        };

    try {
      final LoadBalancerAttributes attributes =
          Entities.asTransaction(LoadBalancer.class, lookupAttributes)
              .apply(request.getLoadBalancerName());

      reply.getDescribeLoadBalancerAttributesResult().setLoadBalancerAttributes(attributes);
    } catch (RuntimeException e) {
      Exceptions.findAndRethrow(e, LoadBalancingException.class);
      throw e;
    }

    return reply;
  }

  /**
   * Lookup by name and verify permissions.
   */
  @Nonnull
  private static LoadBalancer lookupAuthorizedByNameOrDnsName(
      final LoadBalancingPersistence persistence,
      final String accountNumber,
      final String name
  ) throws LoadBalancingException {
    return lookupAuthorizedByNameOrDnsName(persistence, accountNumber, name, Functions.identity());
  }

  /**
   * Lookup by name and verify permissions.
   */
  @Nonnull
  private static <T> T lookupAuthorizedByNameOrDnsName(
      final LoadBalancingPersistence persistence,
      final String accountNumber,
      final String name,
      final Function<LoadBalancer, T> transform
  ) throws LoadBalancingException {
    T loadBalancer;
    try {
      loadBalancer =
          LoadBalancerHelper.getLoadbalancer(persistence, LoadBalancingMetadatas.filterPrivileged(),
              transform, accountNumber, name);
    } catch (final NoSuchElementException e) {
      try {
        loadBalancer = LoadBalancerHelper.getLoadBalancerByDnsName(persistence,
            LoadBalancingMetadatas.filterPrivileged(), transform, name);
      } catch (final NoSuchElementException e2) {
        throw new AccessPointNotFoundException();
      } catch (final Exception e2) {
        LOG.error("Failed to find loadbalancer by DNS name" + accountNumber + ":" + name, e2);
        throw new InternalFailureException("Failed to find loadbalancer " + name);
      }
    } catch (final Exception e) {
      LOG.error("Failed to find loadbalancer " + accountNumber + ":" + name, e);
      throw new InternalFailureException("Failed to find loadbalancer " + name);
    }
    return loadBalancer;
  }

  private static Predicate<String> isReservedTagPrefix() {
    final Collection<Predicate<String>> predicates = Lists.newArrayList();
    for (final String prefix : reservedPrefixes) {
      predicates.add(com.eucalyptus.util.Strings.startsWith(prefix));
    }
    return Predicates.or(predicates);
  }

  private static LoadBalancingException handleException(final Exception e)
      throws LoadBalancingException {
    final LoadBalancingException cause = Exceptions.findCause(e, LoadBalancingException.class);
    if (cause != null) {
      throw cause;
    }

    final AuthQuotaException quotaCause = Exceptions.findCause(e, AuthQuotaException.class);
    if (quotaCause != null) {
      throw new TooManyAccessPointsException();
    }

    final AuthException authCause = Exceptions.findCause(e, AuthException.class);
    if (authCause != null) {
      throw new InternalFailure400Exception(authCause.getMessage());
    }

    LOG.error(e, e);

    final InternalFailureException exception =
        new InternalFailureException(String.valueOf(e.getMessage()));
    if (Contexts.lookup().hasAdministrativePrivileges()) {
      exception.initCause(e);
    }
    throw exception;
  }
}



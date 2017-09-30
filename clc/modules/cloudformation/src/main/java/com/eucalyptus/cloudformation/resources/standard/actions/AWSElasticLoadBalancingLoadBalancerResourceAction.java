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
package com.eucalyptus.cloudformation.resources.standard.actions;


import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSElasticLoadBalancingLoadBalancerResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSElasticLoadBalancingLoadBalancerProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingAccessLoggingPolicy;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingAppCookieStickinessPolicy;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingLBCookieStickinessPolicyType;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingListener;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingPolicyType;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingPolicyTypeAttribute;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.AccessLog;
import com.eucalyptus.loadbalancing.common.msgs.AddTagsResponseType;
import com.eucalyptus.loadbalancing.common.msgs.AddTagsType;
import com.eucalyptus.loadbalancing.common.msgs.AppCookieStickinessPolicy;
import com.eucalyptus.loadbalancing.common.msgs.ApplySecurityGroupsToLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.AttachLoadBalancerToSubnetsType;
import com.eucalyptus.loadbalancing.common.msgs.AvailabilityZones;
import com.eucalyptus.loadbalancing.common.msgs.BackendServerDescription;
import com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckResponseType;
import com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckType;
import com.eucalyptus.loadbalancing.common.msgs.ConnectionSettings;
import com.eucalyptus.loadbalancing.common.msgs.CreateAppCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateAppCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLBCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLBCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerListenersType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.CrossZoneLoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerListenersType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPoliciesResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPoliciesType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeTagsResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeTagsType;
import com.eucalyptus.loadbalancing.common.msgs.DetachLoadBalancerFromSubnetsType;
import com.eucalyptus.loadbalancing.common.msgs.DisableAvailabilityZonesForLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.EnableAvailabilityZonesForLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.Instance;
import com.eucalyptus.loadbalancing.common.msgs.Instances;
import com.eucalyptus.loadbalancing.common.msgs.LBCookieStickinessPolicy;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.msgs.ListenerDescription;
import com.eucalyptus.loadbalancing.common.msgs.Listeners;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerAttributes;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerDescription;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerNames;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerNamesMax20;
import com.eucalyptus.loadbalancing.common.msgs.ModifyLoadBalancerAttributesResponseType;
import com.eucalyptus.loadbalancing.common.msgs.ModifyLoadBalancerAttributesType;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttribute;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttributeDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttributes;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyNames;
import com.eucalyptus.loadbalancing.common.msgs.Ports;
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.RemoveTagsResponseType;
import com.eucalyptus.loadbalancing.common.msgs.RemoveTagsType;
import com.eucalyptus.loadbalancing.common.msgs.SecurityGroups;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerListenerSSLCertificateType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesForBackendServerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesForBackendServerType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesOfListenerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesOfListenerType;
import com.eucalyptus.loadbalancing.common.msgs.Subnets;
import com.eucalyptus.loadbalancing.common.msgs.Tag;
import com.eucalyptus.loadbalancing.common.msgs.TagDescription;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSElasticLoadBalancingLoadBalancerResourceAction extends StepBasedResourceAction {
  public static final Logger LOG = Logger.getLogger(AWSElasticLoadBalancingLoadBalancerResourceAction.class);
  private AWSElasticLoadBalancingLoadBalancerProperties properties = new AWSElasticLoadBalancingLoadBalancerProperties();
  private AWSElasticLoadBalancingLoadBalancerResourceInfo info = new AWSElasticLoadBalancingLoadBalancerResourceInfo();

  public AWSElasticLoadBalancingLoadBalancerResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
    // In this case, update with replacement has a precondition check before essentially the same steps as "create".  We add both.
    Map<String, UpdateStep> updateWithReplacementMap = Maps.newLinkedHashMap();
    updateWithReplacementMap.putAll(fromUpdateEnum(UpdateWithReplacementPreCreateSteps.class));
    updateWithReplacementMap.putAll(createStepsToUpdateWithReplacementSteps(fromEnum(CreateSteps.class)));
    setUpdateSteps(UpdateTypeAndDirection.UPDATE_WITH_REPLACEMENT, updateWithReplacementMap);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSElasticLoadBalancingLoadBalancerResourceAction otherAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
    if (!Objects.equals(properties.getAccessLoggingPolicy(), otherAction.properties.getAccessLoggingPolicy())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getAppCookieStickinessPolicy(), otherAction.properties.getAppCookieStickinessPolicy())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getAvailabilityZones(), otherAction.properties.getAvailabilityZones())) {
      // Replacement if removing all or adding some where none exist
      boolean oldOnesExist = (properties.getAvailabilityZones() != null && !properties.getAvailabilityZones().isEmpty());
      boolean newOnesExist = (otherAction.properties.getAvailabilityZones() != null &&
        !otherAction.properties.getAvailabilityZones().isEmpty());
      if ((oldOnesExist && !newOnesExist) || (!oldOnesExist && newOnesExist)) {
        updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      } else {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    if (!Objects.equals(properties.getConnectionDrainingPolicy(), otherAction.properties.getConnectionDrainingPolicy())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getConnectionSettings(), otherAction.properties.getConnectionSettings())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getCrossZone(), otherAction.properties.getCrossZone())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getHealthCheck(), otherAction.properties.getHealthCheck())) {
      // either removing a health check or adding one from a non-existent case
      if (properties.getHealthCheck() == null || otherAction.properties.getHealthCheck() == null) {
        updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      } else {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    if (!Objects.equals(properties.getInstances(), otherAction.properties.getInstances())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getLbCookieStickinessPolicy(), otherAction.properties.getLbCookieStickinessPolicy())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getListeners(), otherAction.properties.getListeners())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getLoadBalancerName(), otherAction.properties.getLoadBalancerName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getPolicies(), otherAction.properties.getPolicies())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getScheme(), otherAction.properties.getScheme())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getSecurityGroups(), otherAction.properties.getSecurityGroups())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getSubnets(), otherAction.properties.getSubnets())) {
      // Replacement if removing all or adding some where none exist
      boolean oldOnesExist = (properties.getSubnets() != null && !properties.getSubnets().isEmpty());
      boolean newOnesExist = (otherAction.properties.getSubnets() != null &&
        !otherAction.properties.getSubnets().isEmpty());
      if ((oldOnesExist && !newOnesExist) || (!oldOnesExist && newOnesExist)) {
        updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      } else {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        CreateLoadBalancerType createLoadBalancerType = MessageHelper.createMessage(CreateLoadBalancerType.class, action.info.getEffectiveUserId());
        if (action.properties.getLoadBalancerName() == null) {

          // The name here is a little weird.  It needs to be no more than 32 characters
          createLoadBalancerType.setLoadBalancerName(action.getDefaultPhysicalResourceId(32));
        } else {
          createLoadBalancerType.setLoadBalancerName(action.properties.getLoadBalancerName());
        }
        if ( action.properties.getAvailabilityZones( ) != null && !action.properties.getAvailabilityZones( ).isEmpty( ) ) {
          AvailabilityZones availabilityZones = new AvailabilityZones();
          ArrayList<String> member = Lists.newArrayList(action.properties.getAvailabilityZones());
          availabilityZones.setMember(member);
          createLoadBalancerType.setAvailabilityZones(availabilityZones);
        }
        if (action.properties.getListeners() != null) {
          Listeners listeners = new Listeners();
          ArrayList<Listener> member = Lists.newArrayList();
          for (ElasticLoadBalancingListener elasticLoadBalancingListener: action.properties.getListeners()) {
            Listener listener = convertListener(elasticLoadBalancingListener);
            // TO set the policies, look at the next step
            member.add(listener);
          }
          listeners.setMember(member);
          createLoadBalancerType.setListeners(listeners);
        }
        createLoadBalancerType.setScheme(action.properties.getScheme());
        if ( action.properties.getSecurityGroups( ) != null && !action.properties.getSecurityGroups( ).isEmpty( ) ) {
          SecurityGroups securityGroups = new SecurityGroups();
          ArrayList<String> member = Lists.newArrayList(action.properties.getSecurityGroups());
          securityGroups.setMember(member);
          createLoadBalancerType.setSecurityGroups(securityGroups);
        }
        if ( action.properties.getSubnets( ) != null && !action.properties.getSubnets( ).isEmpty( ) ) {
          Subnets subnets = new Subnets();
          ArrayList<String> member = Lists.newArrayList(action.properties.getSubnets());
          subnets.setMember(member);
          createLoadBalancerType.setSubnets(subnets);
        }
        CreateLoadBalancerResponseType createLoadBalancerResponseType = AsyncRequests.<CreateLoadBalancerType,CreateLoadBalancerResponseType> sendSync(configuration, createLoadBalancerType);
        action.info.setPhysicalResourceId(createLoadBalancerType.getLoadBalancerName());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setDnsName(JsonHelper.getStringFromJsonNode(new TextNode(createLoadBalancerResponseType.getCreateLoadBalancerResult().getDnsName())));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ADD_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        // Create 'system' tags as admin user
        String effectiveAdminUserId = action.info.getAccountId( );
        AddTagsType addSystemTagsType = MessageHelper.createPrivilegedMessage(AddTagsType.class, effectiveAdminUserId);
        addSystemTagsType.setLoadBalancerNames(getLoadBalancerNames(action));
        addSystemTagsType.setTags(TagHelper.convertToTagList(TagHelper.getCloudFormationResourceSystemTags(action.info, action.getStackEntity())));
        AsyncRequests.<AddTagsType, AddTagsResponseType>sendSync(configuration, addSystemTagsType);
        // Create non-system tags as regular user
        List<CloudFormationResourceTag> tags = TagHelper.getCloudFormationResourceStackTags(action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedCloudFormationResourceTemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags());
        }
        if (!tags.isEmpty()) {
          AddTagsType addTagsType = MessageHelper.createMessage(AddTagsType.class, action.info.getEffectiveUserId());
          addTagsType.setLoadBalancerNames(getLoadBalancerNames(action));
          addTagsType.setTags(TagHelper.convertToTagList(tags));
          AsyncRequests.<AddTagsType, AddTagsResponseType>sendSync(configuration, addTagsType);
        }
        return action;
      }

    },
    ADD_INSTANCES_TO_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        if ( action.properties.getInstances( ) != null && !action.properties.getInstances( ).isEmpty( ) ) {
          RegisterInstancesWithLoadBalancerType registerInstancesWithLoadBalancerType = MessageHelper.createMessage(RegisterInstancesWithLoadBalancerType.class, action.info.getEffectiveUserId());
          registerInstancesWithLoadBalancerType.setLoadBalancerName(action.info.getPhysicalResourceId());
          Instances instances = new Instances();
          ArrayList<Instance> member = Lists.newArrayList();
          for (String instanceId: action.properties.getInstances()) {
            Instance instance = new Instance();
            instance.setInstanceId(instanceId);
            member.add(instance);
          }
          instances.setMember(member);
          registerInstancesWithLoadBalancerType.setInstances(instances);
          AsyncRequests.<RegisterInstancesWithLoadBalancerType,RegisterInstancesWithLoadBalancerResponseType> sendSync(configuration, registerInstancesWithLoadBalancerType);
        }
        return action;
      }
    },
    ADD_HEALTH_CHECK_TO_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        if (action.properties.getHealthCheck() != null) {
          configureHealthCheck(action, configuration);
        }
        return action;
      }
    },
    ADD_POLICIES_TO_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        checkForDuplicatePolicyNames(action);
        if (action.properties.getPolicies() != null) {
          for (ElasticLoadBalancingPolicyType elasticLoadBalancingPropertyType: action.properties.getPolicies()) {
            createLoadBalancerPolicy(action, configuration, elasticLoadBalancingPropertyType);
          }
        }
        if (action.properties.getAppCookieStickinessPolicy() != null) {
          for (ElasticLoadBalancingAppCookieStickinessPolicy elasticLoadBalancingAppCookieStickinessPolicy: action.properties.getAppCookieStickinessPolicy()) {
            createAppCookieStickinessPolicy(action, configuration, elasticLoadBalancingAppCookieStickinessPolicy);
          }
        }
        if (action.properties.getLbCookieStickinessPolicy() != null) {
          for (ElasticLoadBalancingLBCookieStickinessPolicyType elasticLoadBalancingLbCookieStickinessPolicy: action.properties.getLbCookieStickinessPolicy()) {
            createLBCookieStickinessPolicy(action, configuration, elasticLoadBalancingLbCookieStickinessPolicy);
          }
        }
        return action;
      }
    },
    ADD_LOAD_BALANCER_POLICIES_TO_LISTENERS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        Multimap<Integer, String> listenerPolicyMap = HashMultimap.create();
        Multimap<Integer, String> backendPolicyMap = HashMultimap.create();
        Set<Integer> listenerPorts = Sets.newHashSet();
        // first add policies explicitly listed in the map.
        if (action.properties.getListeners() != null) {
          for (ElasticLoadBalancingListener elasticLoadBalancingListener : action.properties.getListeners()) {
            listenerPorts.add(elasticLoadBalancingListener.getLoadBalancerPort());
            if (elasticLoadBalancingListener.getPolicyNames() != null) {
              listenerPolicyMap.putAll(elasticLoadBalancingListener.getLoadBalancerPort(), elasticLoadBalancingListener.getPolicyNames());
            }
          }
        }
        // now add policies by their load balancer port (assuming a proper listener exists) and backend by their instance ports
        if (action.properties.getPolicies() != null) {
          for (ElasticLoadBalancingPolicyType policyType : action.properties.getPolicies()) {
            if (policyType.getLoadBalancerPorts() != null) {
              for (Integer loadBalancerPort : policyType.getLoadBalancerPorts()) {
                if (!listenerPorts.contains(loadBalancerPort)) {
                  throw new ValidationErrorException("Policy " + policyType.getPolicyName() + " has a load balancer port of " + loadBalancerPort + ", which has no listener defined");
                } else {
                  listenerPolicyMap.put(loadBalancerPort, policyType.getPolicyName());
                }
              }
            }
            if (policyType.getInstancePorts() != null) {
              for (Integer instancePort : policyType.getInstancePorts()) {
                backendPolicyMap.put(instancePort, policyType.getPolicyName());
              }
            }
          }
        }
        for (Integer listenerLBPort: listenerPolicyMap.keySet()) {
          ArrayList<String> policyNamesStr = Lists.newArrayList(listenerPolicyMap.get(listenerLBPort));
          SetLoadBalancerPoliciesOfListenerType setLoadBalancerPoliciesOfListenerType = MessageHelper.createMessage(SetLoadBalancerPoliciesOfListenerType.class, action.info.getEffectiveUserId());
          setLoadBalancerPoliciesOfListenerType.setLoadBalancerName(action.info.getPhysicalResourceId());
          setLoadBalancerPoliciesOfListenerType.setLoadBalancerPort(listenerLBPort);
          PolicyNames policyNames = new PolicyNames();
          policyNames.setMember(policyNamesStr);
          setLoadBalancerPoliciesOfListenerType.setPolicyNames(policyNames);
          AsyncRequests.<SetLoadBalancerPoliciesOfListenerType,SetLoadBalancerPoliciesOfListenerResponseType> sendSync(configuration, setLoadBalancerPoliciesOfListenerType);
        }
        for (Integer backendInstancePort: backendPolicyMap.keySet()) {
          ArrayList<String> policyNamesStr = Lists.newArrayList(backendPolicyMap.get(backendInstancePort));
          SetLoadBalancerPoliciesForBackendServerType setLoadBalancerPoliciesForBackendServerType = MessageHelper.createMessage(SetLoadBalancerPoliciesForBackendServerType.class, action.info.getEffectiveUserId());
          setLoadBalancerPoliciesForBackendServerType.setLoadBalancerName(action.info.getPhysicalResourceId());
          setLoadBalancerPoliciesForBackendServerType.setInstancePort(backendInstancePort);
          PolicyNames policyNames = new PolicyNames();
          policyNames.setMember(policyNamesStr);
          setLoadBalancerPoliciesForBackendServerType.setPolicyNames(policyNames);
          AsyncRequests.<SetLoadBalancerPoliciesForBackendServerType,SetLoadBalancerPoliciesForBackendServerResponseType> sendSync(configuration, setLoadBalancerPoliciesForBackendServerType);
        }
        return action;
      }
    },
    SET_CROSS_ZONE_ATTRIBUTE { // For any configured load balancer attributes
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSElasticLoadBalancingLoadBalancerResourceAction action =
            (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        final ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        final boolean accessLogging = action.properties.getAccessLoggingPolicy( ) != null;
        final boolean crossZone = action.properties.getCrossZone() != null &&
             Boolean.TRUE.equals(action.properties.getCrossZone());
        final boolean idleTimeout = action.properties.getConnectionSettings( ) != null &&
            action.properties.getConnectionSettings( ).getIdleTimeout( ) != null;
        if ( accessLogging || crossZone || idleTimeout ) {
          ModifyLoadBalancerAttributesType modifyLoadBalancerAttributesType = MessageHelper.createMessage(ModifyLoadBalancerAttributesType.class, action.info.getEffectiveUserId());
          modifyLoadBalancerAttributesType.setLoadBalancerName(action.info.getPhysicalResourceId());
          LoadBalancerAttributes loadBalancerAttributes = new LoadBalancerAttributes();
          if ( accessLogging ) {
            final ElasticLoadBalancingAccessLoggingPolicy accessLoggingPolicy = action.properties.getAccessLoggingPolicy( );
            final AccessLog accessLog = new AccessLog( );
            accessLog.setEnabled( accessLoggingPolicy.getEnabled( ) );
            accessLog.setEmitInterval( accessLoggingPolicy.getEmitInterval( ) );
            accessLog.setS3BucketName( accessLoggingPolicy.getS3BucketName( ) );
            accessLog.setS3BucketPrefix( accessLoggingPolicy.getS3BucketPrefix( ) );
            loadBalancerAttributes.setAccessLog( accessLog );
          }
          if ( crossZone ) {
            CrossZoneLoadBalancing crossZoneLoadBalancing = new CrossZoneLoadBalancing( );
            crossZoneLoadBalancing.setEnabled( Boolean.TRUE );
            loadBalancerAttributes.setCrossZoneLoadBalancing( crossZoneLoadBalancing );
          }
          if ( idleTimeout ) {
            ConnectionSettings connectionSettings = new ConnectionSettings( );
            connectionSettings.setIdleTimeout( action.properties.getConnectionSettings( ).getIdleTimeout( ) );
            loadBalancerAttributes.setConnectionSettings( connectionSettings );
          }
          modifyLoadBalancerAttributesType.setLoadBalancerAttributes(loadBalancerAttributes);
          AsyncRequests.<ModifyLoadBalancerAttributesType, ModifyLoadBalancerAttributesResponseType>sendSync(configuration, modifyLoadBalancerAttributesType);
        }
        return action;
      }
    },
    PLACEHOLDER_FOR_OTHER_FIELDS { //// placeholder for ""ConnectionDrainingPolicy"
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        // not currently supported
        return action;
      }
    },
    DESCRIBE_LOAD_BALANCER_TO_GET_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        return describeLoadBalancerToGetAttributes(action, configuration);
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private static ResourceAction describeLoadBalancerToGetAttributes(AWSElasticLoadBalancingLoadBalancerResourceAction action, ServiceConfiguration configuration) throws Exception {
    DescribeLoadBalancersType describeLoadBalancersType = MessageHelper.createMessage(DescribeLoadBalancersType.class, action.info.getEffectiveUserId());
    describeLoadBalancersType.setLoadBalancerNames(getLoadBalancerNames(action));
    DescribeLoadBalancersResponseType describeLoadBalancersResponseType = AsyncRequests.<DescribeLoadBalancersType,DescribeLoadBalancersResponseType> sendSync(configuration, describeLoadBalancersType);
    if (describeLoadBalancersResponseType != null && describeLoadBalancersResponseType.getDescribeLoadBalancersResult() != null
      && describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions() != null &&
      describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember() != null &&
      describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().size() > 0) {
      LoadBalancerDescription loadBalancerDescription = describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().get(0);
      String canonicalHostedZoneName = loadBalancerDescription.getCanonicalHostedZoneName();
      String canonicalHostedZoneNameId = loadBalancerDescription.getCanonicalHostedZoneNameID();
      String sourceSecurityGroupGroupName = loadBalancerDescription.getSourceSecurityGroup().getGroupName();
      String sourceSecurityGroupGroupOwnerAlias = loadBalancerDescription.getSourceSecurityGroup().getOwnerAlias();
      if ("internal".equals(loadBalancerDescription.getScheme())) {
        canonicalHostedZoneName = loadBalancerDescription.getDnsName();
      }
      action.info.setSourceSecurityGroupGroupName(JsonHelper.getStringFromJsonNode(new TextNode(sourceSecurityGroupGroupName)));
      action.info.setSourceSecurityGroupOwnerAlias(JsonHelper.getStringFromJsonNode(new TextNode(sourceSecurityGroupGroupOwnerAlias)));
      action.info.setCanonicalHostedZoneNameID(JsonHelper.getStringFromJsonNode(new TextNode(canonicalHostedZoneNameId)));
      action.info.setCanonicalHostedZoneName(JsonHelper.getStringFromJsonNode(new TextNode(canonicalHostedZoneName)));
    }
    return action;
  }

  private static void createLBCookieStickinessPolicy(AWSElasticLoadBalancingLoadBalancerResourceAction action, ServiceConfiguration configuration, ElasticLoadBalancingLBCookieStickinessPolicyType elasticLoadBalancingLbCookieStickinessPolicy) throws Exception {
    CreateLBCookieStickinessPolicyType createLBCookieStickinessPolicyType = MessageHelper.createMessage(CreateLBCookieStickinessPolicyType.class, action.info.getEffectiveUserId());
    createLBCookieStickinessPolicyType.setPolicyName(elasticLoadBalancingLbCookieStickinessPolicy.getPolicyName());
    createLBCookieStickinessPolicyType.setLoadBalancerName(action.info.getPhysicalResourceId());
    createLBCookieStickinessPolicyType.setCookieExpirationPeriod(elasticLoadBalancingLbCookieStickinessPolicy.getCookieExpirationPeriod());
    AsyncRequests.<CreateLBCookieStickinessPolicyType, CreateLBCookieStickinessPolicyResponseType>sendSync(configuration, createLBCookieStickinessPolicyType);
  }

  private static void createLoadBalancerPolicy(AWSElasticLoadBalancingLoadBalancerResourceAction action, ServiceConfiguration configuration, ElasticLoadBalancingPolicyType elasticLoadBalancingPropertyType) throws Exception {
    CreateLoadBalancerPolicyType createLoadBalancerPolicyType = MessageHelper.createMessage(CreateLoadBalancerPolicyType.class, action.info.getEffectiveUserId());
    createLoadBalancerPolicyType.setLoadBalancerName(action.info.getPhysicalResourceId());
    if (elasticLoadBalancingPropertyType.getAttributes() != null) {
      PolicyAttributes policyAttributes = new PolicyAttributes();
      ArrayList<PolicyAttribute> member = Lists.newArrayList();
      for (ElasticLoadBalancingPolicyTypeAttribute elasticLoadBalancingPolicyTypeAttribute: elasticLoadBalancingPropertyType.getAttributes()) {
        PolicyAttribute policyAttribute = new PolicyAttribute();
        policyAttribute.setAttributeName(elasticLoadBalancingPolicyTypeAttribute.getName());
        policyAttribute.setAttributeValue(elasticLoadBalancingPolicyTypeAttribute.getValue());
        member.add(policyAttribute);
      }
      policyAttributes.setMember(member);
      createLoadBalancerPolicyType.setPolicyAttributes(policyAttributes);
    }
    createLoadBalancerPolicyType.setPolicyName(elasticLoadBalancingPropertyType.getPolicyName());
    createLoadBalancerPolicyType.setPolicyTypeName(elasticLoadBalancingPropertyType.getPolicyType());
    AsyncRequests.<CreateLoadBalancerPolicyType,CreateLoadBalancerPolicyResponseType> sendSync(configuration, createLoadBalancerPolicyType);
  }

  private static void createAppCookieStickinessPolicy(AWSElasticLoadBalancingLoadBalancerResourceAction action, ServiceConfiguration configuration, ElasticLoadBalancingAppCookieStickinessPolicy elasticLoadBalancingAppCookieStickinessPolicy) throws Exception {
    CreateAppCookieStickinessPolicyType createAppCookieStickinessPolicyType = MessageHelper.createMessage(CreateAppCookieStickinessPolicyType.class, action.info.getEffectiveUserId());
    createAppCookieStickinessPolicyType.setPolicyName(elasticLoadBalancingAppCookieStickinessPolicy.getPolicyName());
    createAppCookieStickinessPolicyType.setLoadBalancerName(action.info.getPhysicalResourceId());
    createAppCookieStickinessPolicyType.setCookieName(elasticLoadBalancingAppCookieStickinessPolicy.getCookieName());
    AsyncRequests.<CreateAppCookieStickinessPolicyType, CreateAppCookieStickinessPolicyResponseType>sendSync(configuration, createAppCookieStickinessPolicyType);
  }

  private static Listener convertListener(ElasticLoadBalancingListener elasticLoadBalancingListener) {
    Listener listener = new Listener();
    listener.setInstancePort(elasticLoadBalancingListener.getInstancePort());
    listener.setInstanceProtocol(elasticLoadBalancingListener.getInstanceProtocol());
    listener.setLoadBalancerPort(elasticLoadBalancingListener.getLoadBalancerPort());
    listener.setProtocol(elasticLoadBalancingListener.getProtocol());
    listener.setSSLCertificateId(elasticLoadBalancingListener.getSslCertificateId());
    return listener;
  }

  private static void configureHealthCheck(AWSElasticLoadBalancingLoadBalancerResourceAction action, ServiceConfiguration configuration) throws Exception {
    ConfigureHealthCheckType configureHealthCheckType = MessageHelper.createMessage(ConfigureHealthCheckType.class, action.info.getEffectiveUserId());
    configureHealthCheckType.setLoadBalancerName(action.info.getPhysicalResourceId());
    HealthCheck healthCheck = new HealthCheck();
    healthCheck.setHealthyThreshold(action.properties.getHealthCheck().getHealthyThreshold());
    healthCheck.setInterval(action.properties.getHealthCheck().getInterval());
    healthCheck.setTarget(action.properties.getHealthCheck().getTarget());
    healthCheck.setTimeout(action.properties.getHealthCheck().getTimeout());
    healthCheck.setUnhealthyThreshold(action.properties.getHealthCheck().getUnhealthyThreshold());
    configureHealthCheckType.setHealthCheck(healthCheck);
    AsyncRequests.<ConfigureHealthCheckType,ConfigureHealthCheckResponseType> sendSync(configuration, configureHealthCheckType);
  }

  private static LoadBalancerNames getLoadBalancerNames(AWSElasticLoadBalancingLoadBalancerResourceAction action) {
    LoadBalancerNames loadBalancerNames = new LoadBalancerNames();
    loadBalancerNames.setMember(Lists.newArrayList(action.info.getPhysicalResourceId()));
    return loadBalancerNames;
  }

  private enum DeleteSteps implements Step {
    DELETE_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        DescribeLoadBalancersType describeLoadBalancersType = MessageHelper.createMessage(DescribeLoadBalancersType.class, action.info.getEffectiveUserId());
        LoadBalancerNames loadBalancerNames = new LoadBalancerNames();
        ArrayList<String> member = Lists.newArrayList(action.info.getPhysicalResourceId());
        loadBalancerNames.setMember(member);
        describeLoadBalancersType.setLoadBalancerNames(loadBalancerNames);
        DescribeLoadBalancersResponseType describeLoadBalancersResponseType = AsyncRequests.<DescribeLoadBalancersType,DescribeLoadBalancersResponseType> sendSync(configuration, describeLoadBalancersType);
        if ( describeLoadBalancersResponseType != null && describeLoadBalancersResponseType.getDescribeLoadBalancersResult() != null &&
            describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions() != null &&
            describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember() != null &&
            describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().size() == 1 ) {
          DeleteLoadBalancerType deleteLoadBalancerType = MessageHelper.createMessage(DeleteLoadBalancerType.class, action.info.getEffectiveUserId());
          deleteLoadBalancerType.setLoadBalancerName(action.info.getPhysicalResourceId());
          AsyncRequests.<DeleteLoadBalancerType,DeleteLoadBalancerResponseType> sendSync(configuration, deleteLoadBalancerType);
        }
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction oldAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) oldResourceAction;
        AWSElasticLoadBalancingLoadBalancerResourceAction newAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        checkForDuplicatePolicyNames(newAction);
        // update the items on the main load balance.
        DescribeLoadBalancersType describeLoadBalancersType = MessageHelper.createMessage(DescribeLoadBalancersType.class, newAction.info.getEffectiveUserId());
        describeLoadBalancersType.setLoadBalancerNames(getLoadBalancerNames(newAction));
        DescribeLoadBalancersResponseType describeLoadBalancersResponseType = AsyncRequests.<DescribeLoadBalancersType, DescribeLoadBalancersResponseType>sendSync(configuration, describeLoadBalancersType);
        if (describeLoadBalancersResponseType == null || describeLoadBalancersResponseType.getDescribeLoadBalancersResult() == null ||
          describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions() == null ||
          describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember() == null ||
          describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().size() != 1) {
          throw new ValidationErrorException("Can not find load balancer : " + newAction.info.getPhysicalResourceId());
        }
        LoadBalancerDescription loadBalancerDescription = describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().get(0);
        updateAZs(oldAction, newAction, configuration, loadBalancerDescription);
        updateInstances(oldAction, newAction, configuration, loadBalancerDescription);
        updateSecurityGroups(oldAction, newAction, configuration, loadBalancerDescription);
        updateSubnets(oldAction, newAction, configuration, loadBalancerDescription);
        if (newAction.properties.getHealthCheck() != null) {
          configureHealthCheck(newAction, configuration);
        }
        updateListenersAndPolicies(oldAction, newAction, configuration);
        return newAction;
      }
    },
    UPDATE_LOAD_BALANCER_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction oldAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) oldResourceAction;
        AWSElasticLoadBalancingLoadBalancerResourceAction newAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);

        ModifyLoadBalancerAttributesType modifyLoadBalancerAttributesType = MessageHelper.createMessage(ModifyLoadBalancerAttributesType.class, newAction.info.getEffectiveUserId());
        LoadBalancerAttributes loadBalancerAttributes = new LoadBalancerAttributes();
        AccessLog accessLog = new AccessLog();
        if (newAction.properties.getAccessLoggingPolicy() != null) {
          accessLog.setEnabled( newAction.properties.getAccessLoggingPolicy().getEnabled( ) );
          accessLog.setEmitInterval( newAction.properties.getAccessLoggingPolicy().getEmitInterval( ) );
          accessLog.setS3BucketName( newAction.properties.getAccessLoggingPolicy().getS3BucketName( ) );
          accessLog.setS3BucketPrefix( newAction.properties.getAccessLoggingPolicy().getS3BucketPrefix( ) );
        } else {
          accessLog = new AccessLog();
          accessLog.setEnabled( false );
        }

        ConnectionSettings connectionSettings = new ConnectionSettings();
        if (newAction.properties.getConnectionSettings() != null) {
          connectionSettings.setIdleTimeout(newAction.properties.getConnectionSettings().getIdleTimeout());
        } else {
          connectionSettings.setIdleTimeout(60); // default
        }

        CrossZoneLoadBalancing crossZoneLoadBalancing = new CrossZoneLoadBalancing();
        if (newAction.properties.getCrossZone() != null) {
          crossZoneLoadBalancing.setEnabled(newAction.properties.getCrossZone());
        } else {
          crossZoneLoadBalancing.setEnabled(false);
        }

        loadBalancerAttributes.setAccessLog(accessLog);
        loadBalancerAttributes.setConnectionSettings(connectionSettings);
        loadBalancerAttributes.setCrossZoneLoadBalancing(crossZoneLoadBalancing);
        modifyLoadBalancerAttributesType.setLoadBalancerAttributes(loadBalancerAttributes);
        modifyLoadBalancerAttributesType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
        AsyncRequests.<ModifyLoadBalancerAttributesType, ModifyLoadBalancerAttributesResponseType>sendSync(configuration, modifyLoadBalancerAttributesType);
        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction oldAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) oldResourceAction;
        AWSElasticLoadBalancingLoadBalancerResourceAction newAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        DescribeTagsType describeTagsType = MessageHelper.createMessage(DescribeTagsType.class, newAction.info.getEffectiveUserId());
        LoadBalancerNamesMax20 loadBalancerNames = new LoadBalancerNamesMax20();
        loadBalancerNames.setMember(Lists.newArrayList(newAction.info.getPhysicalResourceId()));
        describeTagsType.setLoadBalancerNames(loadBalancerNames);
        DescribeTagsResponseType describeTagsResponseType = AsyncRequests.sendSync(configuration, describeTagsType);
        if (describeTagsResponseType == null || describeTagsResponseType.getDescribeTagsResult() == null ||
          describeTagsResponseType.getDescribeTagsResult().getTagDescriptions() == null ||
          describeTagsResponseType.getDescribeTagsResult().getTagDescriptions().getMember() == null ||
          describeTagsResponseType.getDescribeTagsResult().getTagDescriptions().getMember().size() != 1) {
          throw new ValidationErrorException("Can not find load balancer : " + newAction.info.getPhysicalResourceId());
        }
        final TagDescription tagDescription = describeTagsResponseType.getDescribeTagsResult().getTagDescriptions().getMember().get(0);
        Set<CloudFormationResourceTag> existingTags = Sets.newLinkedHashSet();
        if (tagDescription != null && tagDescription.getTags() != null && tagDescription.getTags().getMember() != null) {
          for (Tag tag : tagDescription.getTags().getMember()) {
            CloudFormationResourceTag cfTag = new CloudFormationResourceTag();
            cfTag.setKey(tag.getKey());
            cfTag.setValue(tag.getValue());
            existingTags.add(cfTag);
          }
        }
        Set<CloudFormationResourceTag> newTags = Sets.newLinkedHashSet();
        if (newAction.properties.getTags() != null) {
          newTags.addAll(newAction.properties.getTags());
        }
        List<CloudFormationResourceTag> newStackTags = TagHelper.getCloudFormationResourceStackTags(newAction.getStackEntity());
        if (newStackTags != null) {
          newTags.addAll(newStackTags);
        }
        TagHelper.checkReservedCloudFormationResourceTemplateTags(newTags);
        // add only 'new' tags
        Set<CloudFormationResourceTag> onlyNewTags = Sets.difference(newTags, existingTags);
        if (!onlyNewTags.isEmpty()) {
          AddTagsType addTagsType = MessageHelper.createMessage(AddTagsType.class, newAction.info.getEffectiveUserId());
          addTagsType.setLoadBalancerNames(getLoadBalancerNames(newAction));
          addTagsType.setTags(TagHelper.convertToTagList(onlyNewTags));
          AsyncRequests.<AddTagsType, AddTagsResponseType>sendSync(configuration, addTagsType);
        }
        //  Get old tags...
        Set<CloudFormationResourceTag> oldTags = Sets.newLinkedHashSet();
        if (oldAction.properties.getTags() != null) {
          oldTags.addAll(oldAction.properties.getTags());
        }
        List<CloudFormationResourceTag> oldStackTags = TagHelper.getCloudFormationResourceStackTags(oldAction.getStackEntity());
        if (oldStackTags != null) {
          oldTags.addAll(oldStackTags);
        }

        // remove only the old tags that are not new and that exist -- however, since remove tags only goes by tag name, we only look at tag names
        Set<String> oldTagKeys = TagHelper.getTagKeyNames(oldTags);
        Set<String> existingTagKeys = TagHelper.getTagKeyNames(existingTags);
        Set<String> newTagKeys = TagHelper.getTagKeyNames(newTags);
        Set<String> tagKeysToRemove = Sets.intersection(oldTagKeys, Sets.difference(existingTagKeys, newTagKeys));


        if (!tagKeysToRemove.isEmpty()) {
          RemoveTagsType removeTagsType = MessageHelper.createMessage(RemoveTagsType.class, newAction.info.getEffectiveUserId());
          removeTagsType.setLoadBalancerNames(getLoadBalancerNames(newAction));
          removeTagsType.setTags(TagHelper.convertToTagKeyList(tagKeysToRemove));
          AsyncRequests.<RemoveTagsType, RemoveTagsResponseType>sendSync(configuration, removeTagsType);
        }
        return newAction;
      }
    },
    UPDATE_DESCRIBE_LOAD_BALANCER_TO_GET_ATTRIBUTES { // just in case... not sure necessary
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction oldAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) oldResourceAction;
        AWSElasticLoadBalancingLoadBalancerResourceAction newAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        return describeLoadBalancerToGetAttributes(newAction, configuration);
      }
    };
    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private static void updateAZs(AWSElasticLoadBalancingLoadBalancerResourceAction oldAction, AWSElasticLoadBalancingLoadBalancerResourceAction newAction, ServiceConfiguration configuration, LoadBalancerDescription loadBalancerDescription) throws Exception {
    Set<String> existingZones = Sets.newHashSet();
    if (loadBalancerDescription.getAvailabilityZones() != null) {
      addAllIfNotNull(existingZones, loadBalancerDescription.getAvailabilityZones().getMember());
    }

    Set<String> newZones = Sets.newHashSet();
    addAllIfNotNull(newZones, newAction.properties.getAvailabilityZones());

    Set<String> oldZones = Sets.newHashSet();
    addAllIfNotNull(oldZones, oldAction.properties.getAvailabilityZones());

    Set<String> zonesToAdd = Sets.difference(newZones, existingZones);

    if (!zonesToAdd.isEmpty()) {
      EnableAvailabilityZonesForLoadBalancerType enableAvailabilityZonesForLoadBalancerType = MessageHelper.createMessage(EnableAvailabilityZonesForLoadBalancerType.class, newAction.info.getEffectiveUserId());
      enableAvailabilityZonesForLoadBalancerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
      AvailabilityZones availabilityZones = new AvailabilityZones();
      availabilityZones.getMember().addAll(zonesToAdd);
      enableAvailabilityZonesForLoadBalancerType.setAvailabilityZones(availabilityZones);
      AsyncRequests.sendSync(configuration, enableAvailabilityZonesForLoadBalancerType);
    }
    Set<String> zonesToRemove = Sets.difference(Sets.intersection(oldZones, existingZones), newZones);
    if (!zonesToRemove.isEmpty()) {
      DisableAvailabilityZonesForLoadBalancerType disableAvailabilityZonesForLoadBalancerType = MessageHelper.createMessage(DisableAvailabilityZonesForLoadBalancerType.class, newAction.info.getEffectiveUserId());
      disableAvailabilityZonesForLoadBalancerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
      AvailabilityZones availabilityZones = new AvailabilityZones();
      availabilityZones.getMember().addAll(zonesToRemove);
      disableAvailabilityZonesForLoadBalancerType.setAvailabilityZones(availabilityZones);
      AsyncRequests.sendSync(configuration, disableAvailabilityZonesForLoadBalancerType);
    }
  }

  private static void updateInstances(AWSElasticLoadBalancingLoadBalancerResourceAction oldAction, AWSElasticLoadBalancingLoadBalancerResourceAction newAction, ServiceConfiguration configuration, LoadBalancerDescription loadBalancerDescription) throws Exception {
    Set<String> existingInstances = Sets.newHashSet();
    if (loadBalancerDescription.getInstances() != null && loadBalancerDescription.getInstances().getMember() != null) {
      for (Instance instance: loadBalancerDescription.getInstances().getMember()) {
        existingInstances.add(instance.getInstanceId());
      }
    }

    Set<String> newInstances = Sets.newHashSet();
    addAllIfNotNull(newInstances, newAction.properties.getInstances());

    Set<String> oldInstances = Sets.newHashSet();
    addAllIfNotNull(oldInstances, oldAction.properties.getInstances());

    Set<String> instancesToAdd = Sets.difference(newInstances, existingInstances);

    if (!instancesToAdd.isEmpty()) {
      RegisterInstancesWithLoadBalancerType registerInstancesWithLoadBalancerType = MessageHelper.createMessage(RegisterInstancesWithLoadBalancerType.class, newAction.info.getEffectiveUserId());
      registerInstancesWithLoadBalancerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
      Instances instances = new Instances();
      for (String instanceId: instancesToAdd) {
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instances.getMember().add(instance);
      }
      registerInstancesWithLoadBalancerType.setInstances(instances);
      AsyncRequests.sendSync(configuration, registerInstancesWithLoadBalancerType);
    }
    Set<String> instancesToRemove = Sets.difference(Sets.intersection(oldInstances, existingInstances), newInstances);
    if (!instancesToRemove.isEmpty()) {
      DeregisterInstancesFromLoadBalancerType deregisterInstancesFromLoadBalancerType = MessageHelper.createMessage(DeregisterInstancesFromLoadBalancerType.class, newAction.info.getEffectiveUserId());
      deregisterInstancesFromLoadBalancerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
      Instances instances = new Instances();
      for (String instanceId: instancesToRemove) {
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instances.getMember().add(instance);
      }
      deregisterInstancesFromLoadBalancerType.setInstances(instances);
      AsyncRequests.sendSync(configuration, deregisterInstancesFromLoadBalancerType);
    }
  }

  private static void updateSecurityGroups(AWSElasticLoadBalancingLoadBalancerResourceAction oldAction, AWSElasticLoadBalancingLoadBalancerResourceAction newAction, ServiceConfiguration configuration, LoadBalancerDescription loadBalancerDescription) throws Exception {
    if (newAction.properties.getSecurityGroups() != null && !newAction.properties.getSecurityGroups().isEmpty()) {
      if (loadBalancerDescription.getVpcId() == null) throw new ValidationErrorException("Can not set Security Groups in non-vpc mode");
      ApplySecurityGroupsToLoadBalancerType applySecurityGroupsToLoadBalancerType = MessageHelper.createMessage(ApplySecurityGroupsToLoadBalancerType.class, newAction.info.getEffectiveUserId());
      applySecurityGroupsToLoadBalancerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
      SecurityGroups securityGroups = new SecurityGroups();
      securityGroups.getMember().addAll(newAction.properties.getSecurityGroups());
      applySecurityGroupsToLoadBalancerType.setSecurityGroups(securityGroups);
      AsyncRequests.sendSync(configuration, applySecurityGroupsToLoadBalancerType);
    } else if (oldAction.properties.getSecurityGroups() != null && !oldAction.properties.getSecurityGroups().isEmpty()) {
      // reset to default
      ServiceConfiguration ec2Configuration = Topology.lookup(Compute.class);
      if (loadBalancerDescription.getVpcId() == null) throw new ValidationErrorException("Can not set Security Groups in non-vpc mode");
      DescribeVpcsType describeVpcsType = MessageHelper.createMessage(DescribeVpcsType.class, newAction.info.getEffectiveUserId());
      describeVpcsType.setFilterSet(Lists.newArrayList( CloudFilters.filter("vpc-id", loadBalancerDescription.getVpcId())));
      DescribeVpcsResponseType describeVpcsResponseType = AsyncRequests.sendSync(ec2Configuration, describeVpcsType);
      if (describeVpcsResponseType == null || describeVpcsResponseType.getVpcSet() == null || describeVpcsResponseType.getVpcSet().getItem() == null ||
              describeVpcsResponseType.getVpcSet().getItem().isEmpty()) {
        throw new ValidationErrorException("No such vpc " + loadBalancerDescription.getVpcId());
      }
      // special case.  for some reason in the default vpc a elb specific group is created.  We may want to stop this later... (TODO: revisit!!!)
      boolean inDefaultVpcAndFoundElbSpecificGroup = false;
      if (describeVpcsResponseType.getVpcSet().getItem().get(0).getIsDefault()) {
        // value stolen from com.eucalyptus.loadbalancing.activities.EventHandlerChainNew.SecurityGroupSetup.generateDefaultVPCSecurityGroupName
        String defaultVpcSecurityGroupName = String.format("default_elb_%s", UUID.nameUUIDFromBytes(loadBalancerDescription.getVpcId().getBytes(StandardCharsets.UTF_8)).toString());
        DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, newAction.info.getEffectiveUserId());
        describeSecurityGroupsType.getFilterSet().add( CloudFilters.filter("vpc-id", loadBalancerDescription.getVpcId()));
        describeSecurityGroupsType.getFilterSet().add( CloudFilters.filter("group-name", defaultVpcSecurityGroupName));
        DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(ec2Configuration, describeSecurityGroupsType);
        if (describeSecurityGroupsResponseType != null && describeSecurityGroupsResponseType.getSecurityGroupInfo() != null &&
                !describeSecurityGroupsResponseType.getSecurityGroupInfo().isEmpty()) {
          ApplySecurityGroupsToLoadBalancerType applySecurityGroupsToLoadBalancerType = MessageHelper.createMessage(ApplySecurityGroupsToLoadBalancerType.class, newAction.info.getEffectiveUserId());
          applySecurityGroupsToLoadBalancerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
          SecurityGroups securityGroups = new SecurityGroups();
          securityGroups.getMember().add(describeSecurityGroupsResponseType.getSecurityGroupInfo().get(0).getGroupId());
          applySecurityGroupsToLoadBalancerType.setSecurityGroups(securityGroups);
          AsyncRequests.sendSync(configuration, applySecurityGroupsToLoadBalancerType);
          inDefaultVpcAndFoundElbSpecificGroup = true;
        }
      }
      if (!inDefaultVpcAndFoundElbSpecificGroup) {
        DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, newAction.info.getEffectiveUserId());
        describeSecurityGroupsType.getFilterSet().add( CloudFilters.filter("vpc-id", loadBalancerDescription.getVpcId()));
        describeSecurityGroupsType.getFilterSet().add( CloudFilters.filter("group-name", "default"));
        DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(ec2Configuration, describeSecurityGroupsType);
        if (describeSecurityGroupsResponseType != null && describeSecurityGroupsResponseType.getSecurityGroupInfo() != null &&
                !describeSecurityGroupsResponseType.getSecurityGroupInfo().isEmpty()) {
          ApplySecurityGroupsToLoadBalancerType applySecurityGroupsToLoadBalancerType = MessageHelper.createMessage(ApplySecurityGroupsToLoadBalancerType.class, newAction.info.getEffectiveUserId());
          applySecurityGroupsToLoadBalancerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
          SecurityGroups securityGroups = new SecurityGroups();
          securityGroups.getMember().add(describeSecurityGroupsResponseType.getSecurityGroupInfo().get(0).getGroupId());
          applySecurityGroupsToLoadBalancerType.setSecurityGroups(securityGroups);
          AsyncRequests.sendSync(configuration, applySecurityGroupsToLoadBalancerType);
        } else {
          throw new ValidationErrorException("Could not find default group for vpc " + loadBalancerDescription.getVpcId());
        }
      }
    }
  }

  private static void updateSubnets(AWSElasticLoadBalancingLoadBalancerResourceAction oldAction, AWSElasticLoadBalancingLoadBalancerResourceAction newAction, ServiceConfiguration configuration, LoadBalancerDescription loadBalancerDescription) throws Exception {
    Set<String> existingSubnets = Sets.newHashSet();
    if (loadBalancerDescription.getSubnets() != null) {
      addAllIfNotNull(existingSubnets, loadBalancerDescription.getSubnets().getMember());
    }

    Set<String> newSubnets = Sets.newHashSet();
    addAllIfNotNull(newSubnets, newAction.properties.getSubnets());

    Set<String> oldSubnets = Sets.newHashSet();
    addAllIfNotNull(oldSubnets, oldAction.properties.getSubnets());

    Set<String> subnetsToAdd = Sets.difference(newSubnets, existingSubnets);

    if (!subnetsToAdd.isEmpty()) {
      AttachLoadBalancerToSubnetsType attachLoadBalancerToSubnets = MessageHelper.createMessage(AttachLoadBalancerToSubnetsType.class, newAction.info.getEffectiveUserId());
      attachLoadBalancerToSubnets.setLoadBalancerName(newAction.info.getPhysicalResourceId());
      Subnets subnets = new Subnets();
      subnets.getMember().addAll(subnetsToAdd);
      attachLoadBalancerToSubnets.setSubnets(subnets);
      AsyncRequests.sendSync(configuration, attachLoadBalancerToSubnets);
    }
    Set<String> subnetsToRemove = Sets.difference(Sets.intersection(oldSubnets, existingSubnets), newSubnets);
    if (!subnetsToRemove.isEmpty()) {
      DetachLoadBalancerFromSubnetsType detachLoadBalancerFromSubnetsType = MessageHelper.createMessage(DetachLoadBalancerFromSubnetsType.class, newAction.info.getEffectiveUserId());
      detachLoadBalancerFromSubnetsType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
      Subnets subnets = new Subnets();
      subnets.getMember().addAll(subnetsToRemove);
      detachLoadBalancerFromSubnetsType.setSubnets(subnets);
      AsyncRequests.sendSync(configuration, detachLoadBalancerFromSubnetsType);
    }
  }

  private enum UpdateWithReplacementPreCreateSteps implements UpdateStep {
    CHECK_NAME_CHANGED {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction oldAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) oldResourceAction;
        AWSElasticLoadBalancingLoadBalancerResourceAction newAction = (AWSElasticLoadBalancingLoadBalancerResourceAction) newResourceAction;
        if (oldAction.properties.getLoadBalancerName() != null &&  oldAction.properties.getLoadBalancerName().equals(newAction.properties.getLoadBalancerName())) {
          throw new ValidationErrorException("CloudFormation cannot update a stack when a custom-named resource requires " +
            "replacing. Rename " + oldAction.properties.getLoadBalancerName() + " and update the stack again.");
        }
        return newAction;
      }

      @Nullable
      @Override
      public Integer getTimeout() {
        return null;
      }
    }
  }
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSElasticLoadBalancingLoadBalancerProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSElasticLoadBalancingLoadBalancerResourceInfo) resourceInfo;
  }

  private static void checkForDuplicatePolicyNames(AWSElasticLoadBalancingLoadBalancerResourceAction action) throws ValidationErrorException {
    Set<String> policyNames = Sets.newHashSet();
    if (action.properties.getPolicies() != null) {
      for (ElasticLoadBalancingPolicyType elasticLoadBalancingPropertyType: action.properties.getPolicies()) {
        if (!policyNames.contains(elasticLoadBalancingPropertyType.getPolicyName())) {
          policyNames.add(elasticLoadBalancingPropertyType.getPolicyName());
        } else {
          throw new ValidationErrorException("Duplicate policy name: " + elasticLoadBalancingPropertyType.getPolicyName() + " found");
        }
      }
    }
    if (action.properties.getAppCookieStickinessPolicy() != null) {
      for (ElasticLoadBalancingAppCookieStickinessPolicy elasticLoadBalancingAppCookieStickinessPolicy: action.properties.getAppCookieStickinessPolicy()) {
        if (!policyNames.contains(elasticLoadBalancingAppCookieStickinessPolicy.getPolicyName())) {
          policyNames.add(elasticLoadBalancingAppCookieStickinessPolicy.getPolicyName());
        } else {
          throw new ValidationErrorException("Duplicate policy name: " + elasticLoadBalancingAppCookieStickinessPolicy.getPolicyName() + " found");
        }
      }
    }
    if (action.properties.getLbCookieStickinessPolicy() != null) {
      for (ElasticLoadBalancingLBCookieStickinessPolicyType elasticLoadBalancingLbCookieStickinessPolicy: action.properties.getLbCookieStickinessPolicy()) {
        if (!policyNames.contains(elasticLoadBalancingLbCookieStickinessPolicy.getPolicyName())) {
          policyNames.add(elasticLoadBalancingLbCookieStickinessPolicy.getPolicyName());
        } else {
          throw new ValidationErrorException("Duplicate policy name: " + elasticLoadBalancingLbCookieStickinessPolicy.getPolicyName() + " found");
        }
      }
    }
  }
  private static void updateListeners(AWSElasticLoadBalancingLoadBalancerResourceAction oldAction, AWSElasticLoadBalancingLoadBalancerResourceAction newAction, ServiceConfiguration configuration) throws Exception {
    // Get a set of listeners (i.e. load balancer ports) from the previous template
    Set<Integer> oldLoadBalancerPorts = Sets.newHashSet();
    if (oldAction.properties.getListeners() != null) {
      for (ElasticLoadBalancingListener elasticLoadBalancingListener : oldAction.properties.getListeners()) {
        oldLoadBalancerPorts.add(elasticLoadBalancingListener.getLoadBalancerPort());
      }
    }

    // Get a set of listeners (i.e. load balancer ports) from the current template)
    Set<Integer> newLoadBalancerPorts = Sets.newHashSet();
    if (newAction.properties.getListeners() != null) {
      for (ElasticLoadBalancingListener elasticLoadBalancingListener : newAction.properties.getListeners()) {
        newLoadBalancerPorts.add(elasticLoadBalancingListener.getLoadBalancerPort());
      }
    }

    LoadBalancerDescription loadBalancerDescription = getLoadBalancerDescription(newAction, configuration);

    // Get a list of listeners (i.e. load balancer ports) from the loadBalancerDescription (i.e. current actual load balancer values).  While we are at it,
    // we can query the listeners themselves later, so let's build a map of listeners keyed off of the load balancer port.
    // next get a list of load balancer ports from the actual load balancer (and actually keep a map for later)
    Map<Integer, ListenerDescription> existingLoadBalancerPortMap = Maps.newHashMap();
    if (loadBalancerDescription.getListenerDescriptions() != null && loadBalancerDescription.getListenerDescriptions().getMember() != null) {
      for (ListenerDescription listenerDescription : loadBalancerDescription.getListenerDescriptions().getMember()) {
        if (listenerDescription != null && listenerDescription.getListener() != null && listenerDescription.getListener().getLoadBalancerPort() != null) {
          existingLoadBalancerPortMap.put(listenerDescription.getListener().getLoadBalancerPort(), listenerDescription);
        }
      }
    }
    Set<Integer> existingLoadBalancerPorts = existingLoadBalancerPortMap.keySet();

    // We are updating listeners now.  This is what we do.
    // 1) First add any 'new' listeners to the load balancer.  This will contain load balancer ports that are in the
    //   current template but not either in the old template or the load balancer description
    Set<Integer> loadBalancerPortsOfListenersToAdd = Sets.difference(newLoadBalancerPorts, Sets.union(oldLoadBalancerPorts, existingLoadBalancerPorts));
    if (newAction.properties.getListeners() != null) {
      for (ElasticLoadBalancingListener elasticLoadBalancingListener : newAction.properties.getListeners()) {
        if (loadBalancerPortsOfListenersToAdd.contains(elasticLoadBalancingListener.getLoadBalancerPort())) {
          CreateLoadBalancerListenersType createLoadBalancerListenersType = MessageHelper.createMessage(CreateLoadBalancerListenersType.class, newAction.info.getEffectiveUserId());
          createLoadBalancerListenersType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
          Listeners listeners = new Listeners();
          Listener listener = new Listener();
          listener.setSSLCertificateId(elasticLoadBalancingListener.getSslCertificateId());
          listener.setProtocol(elasticLoadBalancingListener.getProtocol());
          listener.setLoadBalancerPort(elasticLoadBalancingListener.getLoadBalancerPort());
          listener.setInstanceProtocol(elasticLoadBalancingListener.getInstanceProtocol());
          listener.setInstancePort(elasticLoadBalancingListener.getInstancePort());
          listeners.getMember().add(listener);
          createLoadBalancerListenersType.setListeners(listeners);
          AsyncRequests.sendSync(configuration, createLoadBalancerListenersType);
        }
      }
    }
    // 2) Next, delete any listeners that are no longer in the new template.  We don't delete listeners that we did not
    //    add ourselves, however, in case the system added some listeners, or a user manually added a listener outside
    //    of Cloudformation.  As such, the listeners we delete contain the following attributes.
    //    i) They must have been added by the original template, so among the list of old listeners.
    //    ii) They must currently exist.  (No point deleting a listener that does not exist.)
    //    iii) They must not be in the new listener set.
    Set<Integer> loadBalancerPortsOfListenersToDelete = Sets.difference(Sets.intersection(oldLoadBalancerPorts, existingLoadBalancerPorts), newLoadBalancerPorts);
    if (oldAction.properties.getListeners() != null) {
      for (ElasticLoadBalancingListener elasticLoadBalancingListener : oldAction.properties.getListeners()) {
        if (loadBalancerPortsOfListenersToDelete.contains(elasticLoadBalancingListener.getLoadBalancerPort())) {
          DeleteLoadBalancerListenersType deleteLoadBalancerListenersType = MessageHelper.createMessage(DeleteLoadBalancerListenersType.class, oldAction.info.getEffectiveUserId());
          deleteLoadBalancerListenersType.setLoadBalancerName(oldAction.info.getPhysicalResourceId());
          Ports loadBalancerPorts = new Ports();
          loadBalancerPorts.getMember().add(elasticLoadBalancingListener.getLoadBalancerPort().toString());
          deleteLoadBalancerListenersType.setLoadBalancerPorts(loadBalancerPorts);
          AsyncRequests.sendSync(configuration, deleteLoadBalancerListenersType);
        }
      }
    }
    // 3) Finally we look at listeners that exist and that are also in the new template.  The state of the listener in
    //   the old template is irrelevant, as the existing value is more recent to compare for changes.  If something has
    //   changed, between the existing listener and the values in the new template, depending on what exactly has changed,
    //   we either have to delete and re-add the listener, or simply update the SSL certificate.  We don't look at policies attached to listeners
    //   for differences, but we do reattach current policies to the listener if they still exist.  New policy attachment is handled later.
    Set<Integer> loadBalancerPortsOfListenersToPossiblyUpdate = Sets.intersection(newLoadBalancerPorts, existingLoadBalancerPorts);
    if (newAction.properties.getListeners() != null) {
      for (ElasticLoadBalancingListener elasticLoadBalancingListener : newAction.properties.getListeners()) {
        if (loadBalancerPortsOfListenersToPossiblyUpdate.contains(elasticLoadBalancingListener.getLoadBalancerPort())) {
          ListenerDescription existingListenerDescription = existingLoadBalancerPortMap.get(elasticLoadBalancingListener.getLoadBalancerPort());
          Listener existingListener = existingListenerDescription.getListener();
          // Here are the cases we need to consider.
          // i) InstancePort, Protocol, LoadBalancerPort, InstanceProtocol have not changed between the existing and new version of the listener.
          //   a) SSL Certificate has also not changed -- Result: do nothing
          //   b) SSL Certificate has changed, but it is null -- Result: odd, but perhaps a different protocol.  Need to delete/recreate the listener.
          //   c) SSL Certificate has changed, but it is not null -- Result: call SetLoadBalancerListenerSSLCertificate()
          // ii) At least one of InstancePort, Protocol, LoadBalancerPort, InstanceProtocol has changed.  We need to delete/recreate the listener.

          // If all fields (InstancePort, Protocol, LoadBalancerPort, InstanceProtocol, and SSLCertificateId) are the same, we need to do nothing

          boolean needToDeleteAndRecreateListener;
          if (Objects.equals(existingListener.getInstancePort(), elasticLoadBalancingListener.getInstancePort()) &&
                  Objects.equals(existingListener.getProtocol(), elasticLoadBalancingListener.getProtocol()) &&
                  Objects.equals(existingListener.getLoadBalancerPort(), elasticLoadBalancingListener.getLoadBalancerPort()) &&
                  Objects.equals(existingListener.getInstanceProtocol(), elasticLoadBalancingListener.getInstanceProtocol())) {
            if (Objects.equals(existingListener.getSSLCertificateId(), elasticLoadBalancingListener.getSslCertificateId())) {
              // case i a
              needToDeleteAndRecreateListener = false;
            } else if (elasticLoadBalancingListener.getSslCertificateId() == null) {
              // case i b
              // can't call SetLoadBalancerListenerSSLCertificateType on a null certificate, replace instead
              needToDeleteAndRecreateListener = true;
            } else {
              // case i c
              // just call SetLoadBalancerListenerSSLCertificateType
              SetLoadBalancerListenerSSLCertificateType setLoadBalancerListenerSSLCertificateType = MessageHelper.createMessage(SetLoadBalancerListenerSSLCertificateType.class, newAction.info.getEffectiveUserId());
              setLoadBalancerListenerSSLCertificateType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
              setLoadBalancerListenerSSLCertificateType.setLoadBalancerPort(elasticLoadBalancingListener.getLoadBalancerPort());
              setLoadBalancerListenerSSLCertificateType.setSSLCertificateId(elasticLoadBalancingListener.getSslCertificateId());
              AsyncRequests.sendSync(configuration, setLoadBalancerListenerSSLCertificateType);
              needToDeleteAndRecreateListener = false;
            }
          } else { // case ii
            needToDeleteAndRecreateListener = true;
          }
          if (needToDeleteAndRecreateListener) {
            // delete the listener
            DeleteLoadBalancerListenersType deleteLoadBalancerListenersType = MessageHelper.createMessage(DeleteLoadBalancerListenersType.class, oldAction.info.getEffectiveUserId());
            deleteLoadBalancerListenersType.setLoadBalancerName(oldAction.info.getPhysicalResourceId());
            Ports loadBalancerPorts = new Ports();
            loadBalancerPorts.getMember().add(elasticLoadBalancingListener.getLoadBalancerPort().toString());
            deleteLoadBalancerListenersType.setLoadBalancerPorts(loadBalancerPorts);
            AsyncRequests.sendSync(configuration, deleteLoadBalancerListenersType);

            // recreate the listener
            CreateLoadBalancerListenersType createLoadBalancerListenersType = MessageHelper.createMessage(CreateLoadBalancerListenersType.class, newAction.info.getEffectiveUserId());
            createLoadBalancerListenersType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
            Listeners listeners = new Listeners();
            Listener listener = new Listener();
            listener.setSSLCertificateId(elasticLoadBalancingListener.getSslCertificateId());
            listener.setProtocol(elasticLoadBalancingListener.getProtocol());
            listener.setLoadBalancerPort(elasticLoadBalancingListener.getLoadBalancerPort());
            listener.setInstanceProtocol(elasticLoadBalancingListener.getInstanceProtocol());
            listener.setInstancePort(elasticLoadBalancingListener.getInstancePort());
            listeners.getMember().add(listener);
            createLoadBalancerListenersType.setListeners(listeners);
            AsyncRequests.sendSync(configuration, createLoadBalancerListenersType);

            // Find the listener and reattach any policies
            SetLoadBalancerPoliciesOfListenerType setLoadBalancerPoliciesOfListenerType = MessageHelper.createMessage(SetLoadBalancerPoliciesOfListenerType.class,
                    newAction.info.getEffectiveUserId());
            setLoadBalancerPoliciesOfListenerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
            setLoadBalancerPoliciesOfListenerType.setLoadBalancerPort(elasticLoadBalancingListener.getLoadBalancerPort());
            PolicyNames policyNames = new PolicyNames();
            if (existingListenerDescription.getPolicyNames() != null && existingListenerDescription.getPolicyNames().getMember() != null) {
              policyNames.getMember().addAll(existingListenerDescription.getPolicyNames().getMember());
            }
            setLoadBalancerPoliciesOfListenerType.setPolicyNames(policyNames);
            AsyncRequests.sendSync(configuration, setLoadBalancerPoliciesOfListenerType);
          }
        }
      }
    }
  }

  private static void updateListenersAndPolicies(AWSElasticLoadBalancingLoadBalancerResourceAction oldAction, AWSElasticLoadBalancingLoadBalancerResourceAction newAction, ServiceConfiguration configuration) throws Exception {
    // First thing we do is detach all policies from listeners and backend service to make add/deleting easier.  We don't remove ALL policies though, just those that were not added by
    // someone externally.  Hence, we just delete the old and new policies.  we also do a new lookup against the load balancer just to make sure we are up to date.
    Set<String> oldPolicyNames = getPolicyNames(oldAction);
    Set<String> newPolicyNames = getPolicyNames(newAction);
    LoadBalancerDescription loadBalancerDescription = getLoadBalancerDescription(newAction, configuration);

    Map<Integer, Collection<String>> listenerPolicyMap = Maps.newHashMap();

    if (loadBalancerDescription.getListenerDescriptions() != null && loadBalancerDescription.getListenerDescriptions().getMember() != null) {
      for (ListenerDescription listenerDescription : loadBalancerDescription.getListenerDescriptions().getMember()) {
        Set<String> policyNames = Sets.newHashSet();
        if (listenerDescription.getPolicyNames() != null && listenerDescription.getPolicyNames().getMember() != null) {
          policyNames.addAll(listenerDescription.getPolicyNames().getMember());
        }
        // remove the policy names we want to remove (old/new)
        policyNames.removeAll(oldPolicyNames);
        policyNames.removeAll(newPolicyNames);

        listenerPolicyMap.put(listenerDescription.getListener().getLoadBalancerPort(), policyNames);
      }
    }

    Map<Integer, Collection<String>> backendPolicyMap = Maps.newHashMap();

    if (loadBalancerDescription.getBackendServerDescriptions() != null && loadBalancerDescription.getBackendServerDescriptions().getMember() != null) {
      for (BackendServerDescription backendServerDescription : loadBalancerDescription.getBackendServerDescriptions().getMember()) {
        Set<String> policyNames = Sets.newHashSet();
        if (backendServerDescription.getPolicyNames() != null && backendServerDescription.getPolicyNames().getMember() != null) {
          policyNames.addAll(backendServerDescription.getPolicyNames().getMember());
        }
        // remove the policy names we want to remove (old/new)
        policyNames.removeAll(oldPolicyNames);
        policyNames.removeAll(newPolicyNames);
        backendPolicyMap.put(backendServerDescription.getInstancePort(), policyNames);
      }
    }

    // now set the values to the 'removed' items
    for (Integer loadBalancerPort: listenerPolicyMap.keySet()) {
      SetLoadBalancerPoliciesOfListenerType setLoadBalancerPoliciesOfListenerType = MessageHelper.createMessage(SetLoadBalancerPoliciesOfListenerType.class, newAction.info.getEffectiveUserId());
      setLoadBalancerPoliciesOfListenerType.setLoadBalancerPort(loadBalancerPort);
      PolicyNames policyNames = new PolicyNames();
      policyNames.getMember().addAll(listenerPolicyMap.get(loadBalancerPort));
      setLoadBalancerPoliciesOfListenerType.setPolicyNames(policyNames);
      setLoadBalancerPoliciesOfListenerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
      AsyncRequests.sendSync(configuration, setLoadBalancerPoliciesOfListenerType);
    }

    for (Integer instancePort: backendPolicyMap.keySet()) {
      SetLoadBalancerPoliciesForBackendServerType setLoadBalancerPoliciesForBackendServerType = MessageHelper.createMessage(SetLoadBalancerPoliciesForBackendServerType.class, newAction.info.getEffectiveUserId());
      setLoadBalancerPoliciesForBackendServerType.setInstancePort(instancePort);
      PolicyNames policyNames = new PolicyNames();
      policyNames.getMember().addAll(backendPolicyMap.get(instancePort));
      setLoadBalancerPoliciesForBackendServerType.setPolicyNames(policyNames);
      setLoadBalancerPoliciesForBackendServerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
      AsyncRequests.sendSync(configuration, setLoadBalancerPoliciesForBackendServerType);
    }


    updateListeners(oldAction, newAction, configuration);
    updatePolicies(oldAction, newAction, configuration);
  }

  private static LoadBalancerDescription getLoadBalancerDescription(AWSElasticLoadBalancingLoadBalancerResourceAction newAction, ServiceConfiguration configuration) throws Exception {
    DescribeLoadBalancersType describeLoadBalancersType = MessageHelper.createMessage(DescribeLoadBalancersType.class, newAction.info.getEffectiveUserId());
    LoadBalancerNames loadBalancerNames = new LoadBalancerNames();
    loadBalancerNames.getMember().add(newAction.info.getPhysicalResourceId());
    describeLoadBalancersType.setLoadBalancerNames(loadBalancerNames);
    DescribeLoadBalancersResponseType describeLoadBalancersResponseType = AsyncRequests.<DescribeLoadBalancersType,DescribeLoadBalancersResponseType> sendSync(configuration, describeLoadBalancersType);
    if (describeLoadBalancersResponseType == null || describeLoadBalancersResponseType.getDescribeLoadBalancersResult() == null
            || describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions() == null ||
            describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember() == null ||
            describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().isEmpty()) {
      throw new ValidationErrorException("Can't find load balancer" + newAction.info.getPhysicalResourceId());
    }
    return describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().get(0);
  }

  private static void updatePolicies(AWSElasticLoadBalancingLoadBalancerResourceAction oldAction, AWSElasticLoadBalancingLoadBalancerResourceAction newAction, ServiceConfiguration configuration) throws Exception {
    // We do policies similar to how we do listeners.  We delete old policies, then check for new policies, then "update" existing policies.
    // For policies to be deleted, they first need to be removed from any listeners or backend servers, then removed.
    // We keep a cache of policy states that we "write through" to allow for policies to be deleted and added.  But to simplify that portion,
    // we remove all old policies from the listeners (and backend) at the beginning, then reattach everything at the end.

    LoadBalancerDescription loadBalancerDescription = getLoadBalancerDescription(newAction, configuration);
    Set<String> existingPolicyNames = getPolicyNames(loadBalancerDescription);
    Map<String, PolicyDescription> existingPolicyDescriptionMap = getPolicyDescriptionMap(newAction, configuration, existingPolicyNames);
    Set<String> oldPolicyNames = getPolicyNames(oldAction);
    Set<String> newPolicyNames = getPolicyNames(newAction);

    // First add all new policies, but also make sure the policy state maps are 'up to date'

    if (newAction.properties.getAppCookieStickinessPolicy() != null) {
      for (ElasticLoadBalancingAppCookieStickinessPolicy policy : newAction.properties.getAppCookieStickinessPolicy()) {
        if (!existingPolicyNames.contains(policy.getPolicyName())) {
          createAppCookieStickinessPolicy(newAction, configuration, policy);
        }
      }
    }
    if (newAction.properties.getLbCookieStickinessPolicy() != null) {
      for (ElasticLoadBalancingLBCookieStickinessPolicyType policy : newAction.properties.getLbCookieStickinessPolicy()) {
        if (!existingPolicyNames.contains(policy.getPolicyName())) {
          createLBCookieStickinessPolicy(newAction, configuration, policy);
        }
      }
    }
    if (newAction.properties.getPolicies() != null) {
      for (ElasticLoadBalancingPolicyType policy : newAction.properties.getPolicies()) {
        if (!existingPolicyNames.contains(policy.getPolicyName())) {
          createLoadBalancerPolicy(newAction, configuration, policy);
        }
      }
    }

    // Now delete all old policies (but only ones that were in the original template, just in case the system added policies
    for (String policyName: oldPolicyNames) {
      if (existingPolicyNames.contains(policyName) && !newPolicyNames.contains(policyName)) {
        deletePolicy(newAction, configuration, policyName);
      }
    }

    // Check whether policy needs to be 'updated'  What does this mean?  Fields have changed.
    // 1) For a new AppStickinessPolicy, we don't change anything if we have an existing AppStickinessPolicy with the
    //    same name and cookie name.  If the cookie name is different or the existing policy type is something else,
    //    we need to replace the policy.
    // 2) For a new LbStickinessPolicy, we don't change anything if we have an existing LbStickinessPolicy
    //    with the same name and cookie expioration period.  If the cookie expiration period is different or
    //    the existing policy is a different type, we need to replace the policy.
    // 3) For a general policy, we don't change anything if the policy has the same type and attribute name/values,
    //    otherwise we need to replace the policy.
    // Finally, to simplify life, we will delete/recreate policies in place,
    if (newAction.properties.getAppCookieStickinessPolicy() != null) {
      for (ElasticLoadBalancingAppCookieStickinessPolicy policy: newAction.properties.getAppCookieStickinessPolicy()) {
        if (existingPolicyNames.contains(policy.getPolicyName())) {
          // we know a policy exists.  See if it is an app cookie stickiness policy
          boolean needsUpdate = true;
          if (loadBalancerDescription.getPolicies() != null && loadBalancerDescription.getPolicies().getAppCookieStickinessPolicies() != null
                  && loadBalancerDescription.getPolicies().getAppCookieStickinessPolicies().getMember() != null) {
            for (AppCookieStickinessPolicy appCookieStickinessPolicy: loadBalancerDescription.getPolicies().getAppCookieStickinessPolicies().getMember()) {
              if (appCookieStickinessPolicy.getPolicyName().equals(policy.getPolicyName()) && Objects.equals(appCookieStickinessPolicy.getCookieName(), policy.getCookieName())) {
                needsUpdate = false;
                break;
              }
            }
          }
          if (needsUpdate) { // delete and recreate...
            deletePolicy(newAction, configuration, policy.getPolicyName());
            createAppCookieStickinessPolicy(newAction, configuration, policy);
          }
        }
      }
    }
    if (newAction.properties.getLbCookieStickinessPolicy() != null) {
      for (ElasticLoadBalancingLBCookieStickinessPolicyType policy: newAction.properties.getLbCookieStickinessPolicy()) {
        if (existingPolicyNames.contains(policy.getPolicyName())) {
          // we know a policy exists.  See if it is an lb cookie stickiness policy
          boolean needsUpdate = true;
          if (loadBalancerDescription.getPolicies() != null && loadBalancerDescription.getPolicies().getLbCookieStickinessPolicies() != null
                  && loadBalancerDescription.getPolicies().getLbCookieStickinessPolicies().getMember() != null) {
            for (LBCookieStickinessPolicy lbCookieStickinessPolicy: loadBalancerDescription.getPolicies().getLbCookieStickinessPolicies().getMember()) {
              if (lbCookieStickinessPolicy.getPolicyName().equals(policy.getPolicyName()) && Objects.equals(lbCookieStickinessPolicy.getCookieExpirationPeriod(), policy.getCookieExpirationPeriod())) {
                needsUpdate = false;
                break;
              }
            }
          }
          if (needsUpdate) { // delete and recreate...
            deletePolicy(newAction, configuration, policy.getPolicyName());
            createLBCookieStickinessPolicy(newAction, configuration, policy);
          }
        }
      }
    }
    if (newAction.properties.getPolicies() != null) {
      for (ElasticLoadBalancingPolicyType policy : newAction.properties.getPolicies()) {
        if (existingPolicyNames.contains(policy.getPolicyName())) {
          PolicyDescription existingPolicyDescription = existingPolicyDescriptionMap.get(policy.getPolicyName());
          boolean needsUpdate = false;
          if (!Objects.equals(existingPolicyDescription.getPolicyTypeName(), policy.getPolicyType())) {
            needsUpdate = true;
          } else {
            // We want to make sure attribute are 'the same'.  This poses a bit of a challenge.  The system may add some attributes.
            // But we can't just make sure the new attributes are a subset of the existing attributes, in case we just removed some old attributes.
            // So we need to check that old and new match, and that the existing contain both.  Otherwise replace,
            // and with 'old' we also need to get the attributes depending on type
            Map<String, String> oldAttributes = Maps.newHashMap();
            if (oldAction.properties.getAppCookieStickinessPolicy() != null) {
              for (ElasticLoadBalancingAppCookieStickinessPolicy oldPolicy : oldAction.properties.getAppCookieStickinessPolicy()) {
                if (oldPolicy.getPolicyName().equals(policy.getPolicyName())) {
                  oldAttributes.put("CookieName", oldPolicy.getCookieName());
                  break;
                }
              }
            }
            if (oldAction.properties.getLbCookieStickinessPolicy() != null) {
              for (ElasticLoadBalancingLBCookieStickinessPolicyType oldPolicy : oldAction.properties.getLbCookieStickinessPolicy()) {
                if (oldPolicy.getPolicyName().equals(policy.getPolicyName())) {
                  oldAttributes.put("CookieExpirationPeriod", String.valueOf(oldPolicy.getCookieExpirationPeriod()));
                  break;
                }
              }
            }
            if (oldAction.properties.getPolicies() != null) {
              for (ElasticLoadBalancingPolicyType oldPolicy : oldAction.properties.getPolicies()) {
                if (oldPolicy.getPolicyName().equals(policy.getPolicyName())) {
                  for (ElasticLoadBalancingPolicyTypeAttribute attribute : oldPolicy.getAttributes()) {
                    oldAttributes.put(oldAttributes.get(attribute.getName()), attribute.getValue());
                  }
                  break;
                }
              }
            }

            Map<String, String> existingAttributes = Maps.newHashMap();
            if (existingPolicyDescription.getPolicyAttributeDescriptions() != null && existingPolicyDescription.getPolicyAttributeDescriptions().getMember() != null) {
              for (PolicyAttributeDescription policyAttributeDescription : existingPolicyDescription.getPolicyAttributeDescriptions().getMember()) {
                existingAttributes.put(policyAttributeDescription.getAttributeName(), policyAttributeDescription.getAttributeValue());
              }
            }
            Map<String, String> newAttributes = Maps.newHashMap();
            if (policy.getAttributes() != null) {
              for (ElasticLoadBalancingPolicyTypeAttribute attribute : policy.getAttributes()) {
                newAttributes.put(newAttributes.get(attribute.getName()), attribute.getValue());
              }
            }

            if (!Objects.equals(oldAttributes, newAttributes)) {
              needsUpdate = true;
            } else {
              for (String attributeName : oldAttributes.keySet()) {
                if (!existingAttributes.containsKey(attributeName) || !Objects.equals(oldAttributes.get(attributeName), existingAttributes.get(attributeName))) {
                  needsUpdate = true;
                  break;
                }
              }
              for (String attributeName : newAttributes.keySet()) {
                if (!existingAttributes.containsKey(attributeName) || !Objects.equals(newAttributes.get(attributeName), existingAttributes.get(attributeName))) {
                  needsUpdate = true;
                  break;
                }
              }
            }
          }
          if (needsUpdate) { // delete and recreate...
            deletePolicy(newAction, configuration, policy.getPolicyName());
            createLoadBalancerPolicy(newAction, configuration, policy);
          }
        }
      }
    }

    // Finally attach policies to listeners and backend servers
    Map<Integer, Collection<String>> newListenerPolicyMap = Maps.newHashMap();
    Map<Integer, Collection<String>> newBackendPolicyMap = Maps.newHashMap();
    if (newAction.properties.getListeners() != null) {
      for (ElasticLoadBalancingListener elasticLoadBalancingListener : newAction.properties.getListeners()) {
        newListenerPolicyMap.put(elasticLoadBalancingListener.getLoadBalancerPort(), Sets.<String>newHashSet());
        if (elasticLoadBalancingListener.getPolicyNames() != null) {
          newListenerPolicyMap.get(elasticLoadBalancingListener.getLoadBalancerPort()).addAll(elasticLoadBalancingListener.getPolicyNames());
        }
      }
    }
    if (newAction.properties.getPolicies() != null) {
      for (ElasticLoadBalancingPolicyType policyType : newAction.properties.getPolicies()) {
        if (policyType.getLoadBalancerPorts() != null) {
          for (Integer loadBalancerPort : policyType.getLoadBalancerPorts()) {
            if (!newListenerPolicyMap.containsKey(loadBalancerPort)) {
              throw new ValidationErrorException("Policy " + policyType.getPolicyName() + " has a load balancer port of " + loadBalancerPort + ", which has no listener defined");
            } else {
              newListenerPolicyMap.get(loadBalancerPort).add(policyType.getPolicyName());
            }
          }
        }
        if (policyType.getInstancePorts() != null) {
          for (Integer instancePort : policyType.getInstancePorts()) {
            if (!newBackendPolicyMap.containsKey(instancePort)) {
              newBackendPolicyMap.put(instancePort, Sets.<String>newHashSet());
            }
            newBackendPolicyMap.get(instancePort).add(policyType.getPolicyName());
          }
        }
      }
    }

    if (loadBalancerDescription.getListenerDescriptions() != null && loadBalancerDescription.getListenerDescriptions().getMember() != null) {
      for (ListenerDescription listenerDescription: loadBalancerDescription.getListenerDescriptions().getMember()) {
        Set<String> policyNamesSet = Sets.newHashSet();
        if (listenerDescription.getPolicyNames() != null && listenerDescription.getPolicyNames().getMember() != null) {
          policyNamesSet.addAll(listenerDescription.getPolicyNames().getMember());
        }
        if (newListenerPolicyMap.containsKey(listenerDescription.getListener().getLoadBalancerPort())) {
          policyNamesSet.addAll(newListenerPolicyMap.get(listenerDescription.getListener().getLoadBalancerPort()));
        }
        SetLoadBalancerPoliciesOfListenerType setLoadBalancerPoliciesOfListenerType = MessageHelper.createMessage(SetLoadBalancerPoliciesOfListenerType.class, newAction.info.getEffectiveUserId());
        setLoadBalancerPoliciesOfListenerType.setLoadBalancerPort(listenerDescription.getListener().getLoadBalancerPort());
        PolicyNames policyNames = new PolicyNames();
        policyNames.getMember().addAll(policyNamesSet);
        setLoadBalancerPoliciesOfListenerType.setPolicyNames(policyNames);
        setLoadBalancerPoliciesOfListenerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
        AsyncRequests.sendSync(configuration, setLoadBalancerPoliciesOfListenerType);
      }
    }

    if (loadBalancerDescription.getBackendServerDescriptions() != null && loadBalancerDescription.getBackendServerDescriptions().getMember() != null) {
      for (BackendServerDescription backendServerDescription: loadBalancerDescription.getBackendServerDescriptions().getMember()) {
        Set<String> policyNamesSet = Sets.newHashSet();
        if (backendServerDescription.getPolicyNames() != null && backendServerDescription.getPolicyNames().getMember() != null) {
          policyNamesSet.addAll(backendServerDescription.getPolicyNames().getMember());
        }
        if (newBackendPolicyMap.containsKey(backendServerDescription.getInstancePort())) {
          policyNamesSet.addAll(newBackendPolicyMap.get(backendServerDescription.getInstancePort()));
        }
        SetLoadBalancerPoliciesForBackendServerType setLoadBalancerPoliciesForBackendServerType = MessageHelper.createMessage(SetLoadBalancerPoliciesForBackendServerType.class, newAction.info.getEffectiveUserId());
        setLoadBalancerPoliciesForBackendServerType.setInstancePort(backendServerDescription.getInstancePort());
        PolicyNames policyNames = new PolicyNames();
        policyNames.getMember().addAll(policyNamesSet);
        setLoadBalancerPoliciesForBackendServerType.setPolicyNames(policyNames);
        setLoadBalancerPoliciesForBackendServerType.setLoadBalancerName(newAction.info.getPhysicalResourceId());
        AsyncRequests.sendSync(configuration, setLoadBalancerPoliciesForBackendServerType);
      }
    }

  }

  private static void deletePolicy(AWSElasticLoadBalancingLoadBalancerResourceAction action, ServiceConfiguration configuration, String policyName) throws Exception {
    DeleteLoadBalancerPolicyType deleteLoadBalancerPolicyType = MessageHelper.createMessage(DeleteLoadBalancerPolicyType.class, action.info.getEffectiveUserId());
    deleteLoadBalancerPolicyType.setPolicyName(policyName);
    deleteLoadBalancerPolicyType.setLoadBalancerName(action.info.getPhysicalResourceId());
    AsyncRequests.sendSync(configuration, deleteLoadBalancerPolicyType);
  }

  private static Map<String, PolicyDescription> getPolicyDescriptionMap(AWSElasticLoadBalancingLoadBalancerResourceAction action, ServiceConfiguration configuration, Set<String> policyNames) throws Exception {
    Map<String, PolicyDescription> policyDescriptionMap = Maps.newHashMap();
    DescribeLoadBalancerPoliciesType describeLoadBalancerPoliciesType = MessageHelper.createMessage(DescribeLoadBalancerPoliciesType.class, action.info.getEffectiveUserId());
    describeLoadBalancerPoliciesType.setLoadBalancerName(action.info.getPhysicalResourceId());
    PolicyNames policyNamesObj = new PolicyNames();
    policyNamesObj.getMember().addAll(policyNames);
    describeLoadBalancerPoliciesType.setPolicyNames(policyNamesObj);
    DescribeLoadBalancerPoliciesResponseType describeLoadBalancerPoliciesResponseType = AsyncRequests.sendSync(configuration, describeLoadBalancerPoliciesType);
    if (describeLoadBalancerPoliciesResponseType !=null && describeLoadBalancerPoliciesResponseType.getDescribeLoadBalancerPoliciesResult() != null
      && describeLoadBalancerPoliciesResponseType.getDescribeLoadBalancerPoliciesResult().getPolicyDescriptions() != null &&
      describeLoadBalancerPoliciesResponseType.getDescribeLoadBalancerPoliciesResult().getPolicyDescriptions().getMember() != null) {
      for (PolicyDescription policyDescription : describeLoadBalancerPoliciesResponseType.getDescribeLoadBalancerPoliciesResult().getPolicyDescriptions().getMember()) {
        policyDescriptionMap.put(policyDescription.getPolicyName(), policyDescription);
      }
    }
    return policyDescriptionMap;
  }

  private static Set<String> getPolicyNames(AWSElasticLoadBalancingLoadBalancerResourceAction action) {
    Set<String> policyNames = Sets.newHashSet();
    if (action.properties.getAppCookieStickinessPolicy() != null) {
      for (ElasticLoadBalancingPolicyType policy: action.properties.getPolicies()) {
        policyNames.add(policy.getPolicyName());
      }
      for (ElasticLoadBalancingAppCookieStickinessPolicy policy: action.properties.getAppCookieStickinessPolicy()) {
        policyNames.add(policy.getPolicyName());
      }
      for (ElasticLoadBalancingLBCookieStickinessPolicyType policy: action.properties.getLbCookieStickinessPolicy()) {
        policyNames.add(policy.getPolicyName());
      }
    }
    return policyNames;
  }

  private static Set<String> getPolicyNames(LoadBalancerDescription loadBalancerDescription) {
    Set<String> policyNames = Sets.newHashSet();
    if (loadBalancerDescription.getPolicies() != null) {
      if (loadBalancerDescription.getPolicies().getAppCookieStickinessPolicies() != null && loadBalancerDescription.getPolicies().getAppCookieStickinessPolicies().getMember() != null) {
        for (AppCookieStickinessPolicy policy: loadBalancerDescription.getPolicies().getAppCookieStickinessPolicies().getMember()) {
          policyNames.add(policy.getPolicyName());
        }
      }
      if (loadBalancerDescription.getPolicies().getLbCookieStickinessPolicies() != null && loadBalancerDescription.getPolicies().getLbCookieStickinessPolicies().getMember() != null) {
        for (LBCookieStickinessPolicy policy: loadBalancerDescription.getPolicies().getLbCookieStickinessPolicies().getMember()) {
          policyNames.add(policy.getPolicyName());
        }
      }
      if (loadBalancerDescription.getPolicies().getOtherPolicies() != null) {
        addAllIfNotNull(policyNames, loadBalancerDescription.getPolicies().getOtherPolicies().getMember());
      }
    }
    return policyNames;
  }


  private static <T> Collection<T> addAllIfNotNull(Collection<T> originalCollection, Collection<T> itemsToAdd) {
    if (itemsToAdd != null) {
      originalCollection.addAll(itemsToAdd);
    }
    return originalCollection;
  }
}

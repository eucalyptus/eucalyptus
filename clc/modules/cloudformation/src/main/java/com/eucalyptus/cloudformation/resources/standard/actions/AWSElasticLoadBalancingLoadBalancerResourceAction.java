/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.cloudformation.resources.standard.actions;


import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSElasticLoadBalancingLoadBalancerResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSElasticLoadBalancingLoadBalancerProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingListener;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingPolicyType;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingPolicyTypeAttribute;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivity;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.AvailabilityZones;
import com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckResponseType;
import com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckType;
import com.eucalyptus.loadbalancing.common.msgs.CreateAppCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateAppCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLBCookieStickinessPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLBCookieStickinessPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerPolicyResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerPolicyType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersType;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.Instance;
import com.eucalyptus.loadbalancing.common.msgs.Instances;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.msgs.Listeners;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerDescription;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerNames;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttribute;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttributes;
import com.eucalyptus.loadbalancing.common.msgs.PolicyNames;
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerType;
import com.eucalyptus.loadbalancing.common.msgs.SecurityGroups;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesOfListenerResponseType;
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesOfListenerType;
import com.eucalyptus.loadbalancing.common.msgs.Subnets;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSElasticLoadBalancingLoadBalancerResourceAction extends ResourceAction {

  private AWSElasticLoadBalancingLoadBalancerProperties properties = new AWSElasticLoadBalancingLoadBalancerProperties();
  private AWSElasticLoadBalancingLoadBalancerResourceInfo info = new AWSElasticLoadBalancingLoadBalancerResourceInfo();

  public AWSElasticLoadBalancingLoadBalancerResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

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
        if (action.properties.getAvailabilityZones() != null) {
          AvailabilityZones availabilityZones = new AvailabilityZones();
          ArrayList<String> member = Lists.newArrayList(action.properties.getAvailabilityZones());
          availabilityZones.setMember(member);
          createLoadBalancerType.setAvailabilityZones(availabilityZones);
        }
        if (action.properties.getListeners() != null) {
          Listeners listeners = new Listeners();
          ArrayList<Listener> member = Lists.newArrayList();
          for (ElasticLoadBalancingListener elasticLoadBalancingListener: action.properties.getListeners()) {
            Listener listener = new Listener();
            listener.setInstancePort(elasticLoadBalancingListener.getInstancePort());
            listener.setInstanceProtocol(elasticLoadBalancingListener.getInstanceProtocol());
            listener.setLoadBalancerPort(elasticLoadBalancingListener.getLoadBalancerPort());
            listener.setProtocol(elasticLoadBalancingListener.getProtocol());
            listener.setSSLCertificateId(elasticLoadBalancingListener.getSslCertificateId());
            // TO set the policies, look at the next step
            member.add(listener);
          }
          listeners.setMember(member);
          createLoadBalancerType.setListeners(listeners);
        }
        createLoadBalancerType.setScheme(action.properties.getScheme());
        if (action.properties.getSecurityGroups() != null) {
          SecurityGroups securityGroups = new SecurityGroups();
          ArrayList<String> member = Lists.newArrayList(action.properties.getSecurityGroups());
          securityGroups.setMember(member);
          createLoadBalancerType.setSecurityGroups(securityGroups);
        }
        if (action.properties.getSubnets() != null) {
          Subnets subnets = new Subnets();
          ArrayList<String> member = Lists.newArrayList(action.properties.getSubnets());
          subnets.setMember(member);
          createLoadBalancerType.setSubnets(subnets);
        }
        CreateLoadBalancerResponseType createLoadBalancerResponseType = AsyncRequests.<CreateLoadBalancerType,CreateLoadBalancerResponseType> sendSync(configuration, createLoadBalancerType);
        action.info.setPhysicalResourceId(createLoadBalancerType.getLoadBalancerName());
        action.info.setDnsName(JsonHelper.getStringFromJsonNode(new TextNode(createLoadBalancerResponseType.getCreateLoadBalancerResult().getDnsName())));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ADD_INSTANCES_TO_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        if (action.properties.getInstances()!= null) {
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
        return action;
      }
    },
    ADD_POLICIES_TO_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        if (action.properties.getPolicies() != null) {
          for (ElasticLoadBalancingPolicyType elasticLoadBalancingPropertyType: action.properties.getPolicies()) {
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
            // NOTE: Cloudformation says that policies have two more fields, "InstanceIds" (which bind to the back end, but which we don't currently support), and
            // "LoadBalancerPorts" which seems redundant since Listeners have PolicyNames associated with them (the docs say load balancer ports are only associated with
            // some policy types).  The first one we don't support and the second we don't know what it means in a non-circular way (TODO: figure that out) so we don't
            // support either currently
            AsyncRequests.<CreateLoadBalancerPolicyType,CreateLoadBalancerPolicyResponseType> sendSync(configuration, createLoadBalancerPolicyType);
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
        if (action.properties.getListeners() != null) {
          for (ElasticLoadBalancingListener elasticLoadBalancingListener: action.properties.getListeners()) {
            if (elasticLoadBalancingListener.getPolicyNames() != null) {
              SetLoadBalancerPoliciesOfListenerType setLoadBalancerPoliciesOfListenerType = MessageHelper.createMessage(SetLoadBalancerPoliciesOfListenerType.class, action.info.getEffectiveUserId());
              setLoadBalancerPoliciesOfListenerType.setLoadBalancerName(action.info.getPhysicalResourceId());
              setLoadBalancerPoliciesOfListenerType.setLoadBalancerPort(elasticLoadBalancingListener.getLoadBalancerPort());
              PolicyNames policyNames = new PolicyNames();
              ArrayList<String> member = Lists.newArrayList(elasticLoadBalancingListener.getPolicyNames());
              policyNames.setMember(member);
              setLoadBalancerPoliciesOfListenerType.setPolicyNames(policyNames);
              AsyncRequests.<SetLoadBalancerPoliciesOfListenerType,SetLoadBalancerPoliciesOfListenerResponseType> sendSync(configuration, setLoadBalancerPoliciesOfListenerType);
            }
          }
        }
        return action;
      }
    },
    ADD_APP_STICKINESS_POLICY_TO_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        if (action.properties.getAppCookieStickinessPolicy() != null) {
          CreateAppCookieStickinessPolicyType createAppCookieStickinessPolicyType = MessageHelper.createMessage(CreateAppCookieStickinessPolicyType.class, action.info.getEffectiveUserId());
          createAppCookieStickinessPolicyType.setPolicyName(action.properties.getAppCookieStickinessPolicy().getPolicyName());
          createAppCookieStickinessPolicyType.setLoadBalancerName(action.info.getPhysicalResourceId());
          createAppCookieStickinessPolicyType.setCookieName(action.properties.getAppCookieStickinessPolicy().getCookieName());
          AsyncRequests.<CreateAppCookieStickinessPolicyType,CreateAppCookieStickinessPolicyResponseType> sendSync(configuration, createAppCookieStickinessPolicyType);
        }
        return action;
      }
    },
    ADD_LB_STICKINESS_POLICY_TO_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        if (action.properties.getLbCookieStickinessPolicy() != null) {
          CreateLBCookieStickinessPolicyType createLBCookieStickinessPolicyType = MessageHelper.createMessage(CreateLBCookieStickinessPolicyType.class, action.info.getEffectiveUserId());
          createLBCookieStickinessPolicyType.setPolicyName(action.properties.getLbCookieStickinessPolicy().getPolicyName());
          createLBCookieStickinessPolicyType.setLoadBalancerName(action.info.getPhysicalResourceId());
          createLBCookieStickinessPolicyType.setCookieExpirationPeriod(action.properties.getLbCookieStickinessPolicy().getCookieExpirationPeriod());
          AsyncRequests.<CreateLBCookieStickinessPolicyType,CreateLBCookieStickinessPolicyResponseType> sendSync(configuration, createLBCookieStickinessPolicyType);
        }
        return action;
      }
    },
    PLACEHOLDER_FOR_OTHER_FIELDS { //// placeholder for "AccessLoggingPolicy", "ConnectionDrainingPolicy", "CrossZone" : Boolean
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
        DescribeLoadBalancersType describeLoadBalancersType = MessageHelper.createMessage(DescribeLoadBalancersType.class, action.info.getEffectiveUserId());
        LoadBalancerNames loadBalancerNames = new LoadBalancerNames();
        ArrayList<String> member = Lists.newArrayList(action.info.getPhysicalResourceId());
        loadBalancerNames.setMember(member);
        describeLoadBalancersType.setLoadBalancerNames(loadBalancerNames);
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
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSElasticLoadBalancingLoadBalancerResourceAction action = (AWSElasticLoadBalancingLoadBalancerResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
        if (action.info.getPhysicalResourceId() == null) return action;
        DescribeLoadBalancersType describeLoadBalancersType = MessageHelper.createMessage(DescribeLoadBalancersType.class, action.info.getEffectiveUserId());
        LoadBalancerNames loadBalancerNames = new LoadBalancerNames();
        ArrayList<String> member = Lists.newArrayList(action.info.getPhysicalResourceId());
        loadBalancerNames.setMember(member);
        describeLoadBalancersType.setLoadBalancerNames(loadBalancerNames);
        DescribeLoadBalancersResponseType describeLoadBalancersResponseType = AsyncRequests.<DescribeLoadBalancersType,DescribeLoadBalancersResponseType> sendSync(configuration, describeLoadBalancersType);
        if (describeLoadBalancersResponseType != null && describeLoadBalancersResponseType.getDescribeLoadBalancersResult() != null
          && describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions() != null &&
          describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember() != null &&
          describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().size() > 0) {
          return action;
        }
        DeleteLoadBalancerType deleteLoadBalancerType = MessageHelper.createMessage(DeleteLoadBalancerType.class, action.info.getEffectiveUserId());
        deleteLoadBalancerType.setLoadBalancerName(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteLoadBalancerType,DeleteLoadBalancerResponseType> sendSync(configuration, deleteLoadBalancerType);
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
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

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivity> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }
}
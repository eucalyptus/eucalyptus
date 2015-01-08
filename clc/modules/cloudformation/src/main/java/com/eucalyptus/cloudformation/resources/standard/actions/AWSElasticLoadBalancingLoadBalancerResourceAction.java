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


import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSElasticLoadBalancingLoadBalancerResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSElasticLoadBalancingLoadBalancerProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingListener;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingPolicyType;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingPolicyTypeAttribute;
import com.eucalyptus.cloudformation.template.JsonHelper;
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

import java.util.ArrayList;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSElasticLoadBalancingLoadBalancerResourceAction extends ResourceAction {

  private AWSElasticLoadBalancingLoadBalancerProperties properties = new AWSElasticLoadBalancingLoadBalancerProperties();
  private AWSElasticLoadBalancingLoadBalancerResourceInfo info = new AWSElasticLoadBalancingLoadBalancerResourceInfo();
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
  public int getNumCreateSteps() {
    return 9; //lots of steps!!!
  }

  @Override
  public void create(int stepNum) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
    switch (stepNum) {
      case 0: // create load balancer
        CreateLoadBalancerType createLoadBalancerType = new CreateLoadBalancerType();
        createLoadBalancerType.setEffectiveUserId(info.getEffectiveUserId());
        if (properties.getLoadBalancerName() == null) {

          // The name here is a little weird.  It needs to be no more than 32 characters
          createLoadBalancerType.setLoadBalancerName(getDefaultPhysicalResourceId(32));
        } else {
          createLoadBalancerType.setLoadBalancerName(properties.getLoadBalancerName());
        }
        if (properties.getAvailabilityZones() != null) {
          AvailabilityZones availabilityZones = new AvailabilityZones();
          ArrayList<String> member = Lists.newArrayList(properties.getAvailabilityZones());
          availabilityZones.setMember(member);
          createLoadBalancerType.setAvailabilityZones(availabilityZones);
        }
        if (properties.getListeners() != null) {
          Listeners listeners = new Listeners();
          ArrayList<Listener> member = Lists.newArrayList();
          for (ElasticLoadBalancingListener elasticLoadBalancingListener: properties.getListeners()) {
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
        createLoadBalancerType.setScheme(properties.getScheme());
        if (properties.getSecurityGroups() != null) {
          SecurityGroups securityGroups = new SecurityGroups();
          ArrayList<String> member = Lists.newArrayList(properties.getSecurityGroups());
          securityGroups.setMember(member);
          createLoadBalancerType.setSecurityGroups(securityGroups);
        }
        if (properties.getSubnets() != null) {
          Subnets subnets = new Subnets();
          ArrayList<String> member = Lists.newArrayList(properties.getSubnets());
          subnets.setMember(member);
          createLoadBalancerType.setSubnets(subnets);
        }
        CreateLoadBalancerResponseType createLoadBalancerResponseType = AsyncRequests.<CreateLoadBalancerType,CreateLoadBalancerResponseType> sendSync(configuration, createLoadBalancerType);
        info.setPhysicalResourceId(createLoadBalancerType.getLoadBalancerName());
        info.setDnsName(JsonHelper.getStringFromJsonNode(new TextNode(createLoadBalancerResponseType.getCreateLoadBalancerResult().getDnsName())));
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      case 1: // add instances to load balancer
        if (properties.getInstances()!= null) {
          RegisterInstancesWithLoadBalancerType registerInstancesWithLoadBalancerType = new RegisterInstancesWithLoadBalancerType();
          registerInstancesWithLoadBalancerType.setLoadBalancerName(info.getPhysicalResourceId());
          Instances instances = new Instances();
          ArrayList<Instance> member = Lists.newArrayList();
          for (String instanceId: properties.getInstances()) {
            Instance instance = new Instance();
            instance.setInstanceId(instanceId);
            member.add(instance);
          }
          instances.setMember(member);
          registerInstancesWithLoadBalancerType.setInstances(instances);
          registerInstancesWithLoadBalancerType.setEffectiveUserId(info.getEffectiveUserId());
          AsyncRequests.<RegisterInstancesWithLoadBalancerType,RegisterInstancesWithLoadBalancerResponseType> sendSync(configuration, registerInstancesWithLoadBalancerType);
        }
        break;
      case 2: // add health check to load balancer
        if (properties.getHealthCheck() != null) {
          ConfigureHealthCheckType configureHealthCheckType = new ConfigureHealthCheckType();
          configureHealthCheckType.setLoadBalancerName(info.getPhysicalResourceId());
          HealthCheck healthCheck = new HealthCheck();
          healthCheck.setHealthyThreshold(properties.getHealthCheck().getHealthyThreshold());
          healthCheck.setInterval(properties.getHealthCheck().getInterval());
          healthCheck.setTarget(properties.getHealthCheck().getTarget());
          healthCheck.setTimeout(properties.getHealthCheck().getTimeout());
          healthCheck.setUnhealthyThreshold(properties.getHealthCheck().getUnhealthyThreshold());
          configureHealthCheckType.setHealthCheck(healthCheck);
          configureHealthCheckType.setEffectiveUserId(info.getEffectiveUserId());
          AsyncRequests.<ConfigureHealthCheckType,ConfigureHealthCheckResponseType> sendSync(configuration, configureHealthCheckType);
        }
        break;
      case 3: // add policies to load balancer
        if (properties.getPolicies() != null) {
          for (ElasticLoadBalancingPolicyType elasticLoadBalancingPropertyType: properties.getPolicies()) {
            CreateLoadBalancerPolicyType createLoadBalancerPolicyType = new CreateLoadBalancerPolicyType();
            createLoadBalancerPolicyType.setLoadBalancerName(info.getPhysicalResourceId());
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
            createLoadBalancerPolicyType.setEffectiveUserId(info.getEffectiveUserId());
            // NOTE: Cloudformation says that policies have two more fields, "InstanceIds" (which bind to the back end, but which we don't currently support), and
            // "LoadBalancerPorts" which seems redundant since Listeners have PolicyNames associated with them (the docs say load balancer ports are only associated with
            // some policy types).  The first one we don't support and the second we don't know what it means in a non-circular way (TODO: figure that out) so we don't
            // support either currently
            AsyncRequests.<CreateLoadBalancerPolicyType,CreateLoadBalancerPolicyResponseType> sendSync(configuration, createLoadBalancerPolicyType);
          }
        }
        break;
      case 4: // add load balancer policies to listeners
        if (properties.getListeners() != null) {
          for (ElasticLoadBalancingListener elasticLoadBalancingListener: properties.getListeners()) {
            if (elasticLoadBalancingListener.getPolicyNames() != null) {
              SetLoadBalancerPoliciesOfListenerType setLoadBalancerPoliciesOfListenerType = new SetLoadBalancerPoliciesOfListenerType();
              setLoadBalancerPoliciesOfListenerType.setLoadBalancerName(info.getPhysicalResourceId());
              setLoadBalancerPoliciesOfListenerType.setLoadBalancerPort(elasticLoadBalancingListener.getLoadBalancerPort());
              PolicyNames policyNames = new PolicyNames();
              ArrayList<String> member = Lists.newArrayList(elasticLoadBalancingListener.getPolicyNames());
              policyNames.setMember(member);
              setLoadBalancerPoliciesOfListenerType.setPolicyNames(policyNames);
              setLoadBalancerPoliciesOfListenerType.setEffectiveUserId(info.getEffectiveUserId());
              AsyncRequests.<SetLoadBalancerPoliciesOfListenerType,SetLoadBalancerPoliciesOfListenerResponseType> sendSync(configuration, setLoadBalancerPoliciesOfListenerType);
            }
          }
        }
        break;
      case 5: // add app stickiness policy load balancer
        if (properties.getAppCookieStickinessPolicy() != null) {
          CreateAppCookieStickinessPolicyType createAppCookieStickinessPolicyType = new CreateAppCookieStickinessPolicyType();
          createAppCookieStickinessPolicyType.setPolicyName(properties.getAppCookieStickinessPolicy().getPolicyName());
          createAppCookieStickinessPolicyType.setLoadBalancerName(info.getPhysicalResourceId());
          createAppCookieStickinessPolicyType.setCookieName(properties.getAppCookieStickinessPolicy().getCookieName());
          createAppCookieStickinessPolicyType.setEffectiveUserId(info.getEffectiveUserId());
          AsyncRequests.<CreateAppCookieStickinessPolicyType,CreateAppCookieStickinessPolicyResponseType> sendSync(configuration, createAppCookieStickinessPolicyType);
        }
        break;
      case 6: // add lb stickiness policy load balancer
        if (properties.getLbCookieStickinessPolicy() != null) {
          CreateLBCookieStickinessPolicyType createLBCookieStickinessPolicyType = new CreateLBCookieStickinessPolicyType();
          createLBCookieStickinessPolicyType.setPolicyName(properties.getLbCookieStickinessPolicy().getPolicyName());
          createLBCookieStickinessPolicyType.setLoadBalancerName(info.getPhysicalResourceId());
          createLBCookieStickinessPolicyType.setCookieExpirationPeriod(properties.getLbCookieStickinessPolicy().getCookieExpirationPeriod());
          createLBCookieStickinessPolicyType.setEffectiveUserId(info.getEffectiveUserId());
          AsyncRequests.<CreateLBCookieStickinessPolicyType,CreateLBCookieStickinessPolicyResponseType> sendSync(configuration, createLBCookieStickinessPolicyType);
        }
        break;
      case 7: // placeholder for "AccessLoggingPolicy", "ConnectionDrainingPolicy", "CrossZone" : Boolean,
        break;
      case 8: // describe load balancer to get attributes
        DescribeLoadBalancersType describeLoadBalancersType = new DescribeLoadBalancersType();
        LoadBalancerNames loadBalancerNames = new LoadBalancerNames();
        ArrayList<String> member = Lists.newArrayList(info.getPhysicalResourceId());
        loadBalancerNames.setMember(member);
        describeLoadBalancersType.setLoadBalancerNames(loadBalancerNames);
        describeLoadBalancersType.setEffectiveUserId(info.getEffectiveUserId());
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
          info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
          info.setSourceSecurityGroupGroupName(JsonHelper.getStringFromJsonNode(new TextNode(sourceSecurityGroupGroupName)));
          info.setSourceSecurityGroupOwnerAlias(JsonHelper.getStringFromJsonNode(new TextNode(sourceSecurityGroupGroupOwnerAlias)));
          info.setCanonicalHostedZoneNameID(JsonHelper.getStringFromJsonNode(new TextNode(canonicalHostedZoneNameId)));
          info.setCanonicalHostedZoneName(JsonHelper.getStringFromJsonNode(new TextNode(canonicalHostedZoneName)));
        }
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }

  @Override
  public void update(int stepNum) throws Exception {
    throw new UnsupportedOperationException();
  }

  public void rollbackUpdate() throws Exception {
    // can't update so rollbackUpdate should be a NOOP
  }

  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    ServiceConfiguration configuration = Topology.lookup(LoadBalancing.class);
    // if lb gone, done
    DescribeLoadBalancersType describeLoadBalancersType = new DescribeLoadBalancersType();
    LoadBalancerNames loadBalancerNames = new LoadBalancerNames();
    ArrayList<String> member = Lists.newArrayList(info.getPhysicalResourceId());
    loadBalancerNames.setMember(member);
    describeLoadBalancersType.setLoadBalancerNames(loadBalancerNames);
    describeLoadBalancersType.setEffectiveUserId(info.getEffectiveUserId());
    DescribeLoadBalancersResponseType describeLoadBalancersResponseType = AsyncRequests.<DescribeLoadBalancersType,DescribeLoadBalancersResponseType> sendSync(configuration, describeLoadBalancersType);
    if (describeLoadBalancersResponseType != null && describeLoadBalancersResponseType.getDescribeLoadBalancersResult() != null
      && describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions() != null &&
      describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember() != null &&
      describeLoadBalancersResponseType.getDescribeLoadBalancersResult().getLoadBalancerDescriptions().getMember().size() > 0) {
      return;
    }
    DeleteLoadBalancerType deleteLoadBalancerType = new DeleteLoadBalancerType();
    deleteLoadBalancerType.setLoadBalancerName(info.getPhysicalResourceId());
    deleteLoadBalancerType.setEffectiveUserId(info.getEffectiveUserId());
    AsyncRequests.<DeleteLoadBalancerType,DeleteLoadBalancerResponseType> sendSync(configuration, deleteLoadBalancerType);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }
}
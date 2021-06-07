/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSElasticLoadBalancingV2TargetGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSElasticLoadBalancingV2TargetGroupProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Api;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateTargetGroupResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateTargetGroupType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteTargetGroupType;
import com.eucalyptus.loadbalancingv2.common.msgs.RegisterTargetsType;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetDescription;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetDescriptions;
import com.eucalyptus.loadbalancingv2.common.msgs.TargetGroup;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableSet;
import io.vavr.collection.Stream;
import java.util.Set;
import java.util.function.Function;
import org.apache.log4j.Logger;

public class AWSElasticLoadBalancingV2TargetGroupResourceAction extends StepBasedResourceAction {

  public static final Logger LOG = Logger.getLogger(AWSElasticLoadBalancingV2TargetGroupResourceAction.class);

  private static final Set<Function<AWSElasticLoadBalancingV2TargetGroupProperties,Object>> PROP_REPLACE =
      ImmutableSet.<Function<AWSElasticLoadBalancingV2TargetGroupProperties,Object>>builder()
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getHealthCheckEnabled)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getHealthCheckIntervalSeconds)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getHealthCheckPath)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getHealthCheckPort)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getHealthCheckProtocol)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getHealthyThresholdCount)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getName)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getPort)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getProtocol)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getProtocolVersion)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getTargetType)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getUnhealthyThresholdCount)
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getVpcId)
          .build();

  private static final Set<Function<AWSElasticLoadBalancingV2TargetGroupProperties,Object>> PROP_SOMEINTER =
      ImmutableSet.<Function<AWSElasticLoadBalancingV2TargetGroupProperties,Object>>builder()
          .build();

  private static final Set<Function<AWSElasticLoadBalancingV2TargetGroupProperties,Object>> PROP_NOINTER =
      ImmutableSet.<Function<AWSElasticLoadBalancingV2TargetGroupProperties,Object>>builder()
          .add(AWSElasticLoadBalancingV2TargetGroupProperties::getTags)
          .build();

  private AWSElasticLoadBalancingV2TargetGroupProperties properties = new AWSElasticLoadBalancingV2TargetGroupProperties();
  private AWSElasticLoadBalancingV2TargetGroupResourceInfo info = new AWSElasticLoadBalancingV2TargetGroupResourceInfo();

  public AWSElasticLoadBalancingV2TargetGroupResourceAction() {
    super(
        fromEnum(CreateSteps.class),
        fromEnum(DeleteSteps.class),
        fromUpdateEnum(UpdateNoInterruptionSteps.class),
        null );
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSElasticLoadBalancingV2TargetGroupProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSElasticLoadBalancingV2TargetGroupResourceInfo) resourceInfo;
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    final AWSElasticLoadBalancingV2TargetGroupResourceAction otherAction =
        (AWSElasticLoadBalancingV2TargetGroupResourceAction) resourceAction;
    return defaultUpdateType(
        stackTagsChanged,
        properties,
        otherAction.properties,
        PROP_REPLACE,
        PROP_SOMEINTER,
        PROP_NOINTER);
  }

  private static String arnToFullName(final String arn) {
    final Loadbalancingv2ResourceName resourceName =
        Loadbalancingv2ResourceName.parse(arn, Loadbalancingv2ResourceName.Type.targetgroup);
    return String.format("targetgroup/%s/%s",
        resourceName.getName(),
        resourceName.getId(Loadbalancingv2ResourceName.Type.targetgroup));
  }

  private enum CreateSteps implements Step {
    CREATE_TARGET_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSElasticLoadBalancingV2TargetGroupResourceAction action =
            (AWSElasticLoadBalancingV2TargetGroupResourceAction) resourceAction;
        final Loadbalancingv2Api elbv2 = AsyncProxy.client(
            Loadbalancingv2Api.class, MessageHelper.userIdentity(action.info.getEffectiveUserId()));
        final CreateTargetGroupType createTargetGroup = new CreateTargetGroupType();
        if (action.properties.getName() == null) {
          createTargetGroup.setName(action.getDefaultPhysicalResourceId(32));
        } else {
          createTargetGroup.setName(action.properties.getName());
        }
        createTargetGroup.setPort(action.properties.getPort());
        createTargetGroup.setProtocol(action.properties.getProtocol());
        createTargetGroup.setTargetType(action.properties.getTargetType());
        createTargetGroup.setVpcId(action.properties.getVpcId());
        final Set<CloudFormationResourceTag> tags =
            AWSElasticLoadBalancingV2LoadBalancerResourceAction.getTags(action, action.properties.getTags());
        if (!tags.isEmpty()) {
          createTargetGroup.setTags(AWSElasticLoadBalancingV2LoadBalancerResourceAction.toTagList(tags));
        }
        final CreateTargetGroupResponseType createResponse =
            elbv2.createTargetGroup(createTargetGroup);
        final TargetGroup targetGroup =
            createResponse.getCreateTargetGroupResult().getTargetGroups().getMember().get(0);
        action.info.setPhysicalResourceId(targetGroup.getTargetGroupArn());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setTargetGroupFullName(JsonHelper.getStringFromJsonNode(new TextNode(arnToFullName(targetGroup.getTargetGroupArn()))));
        action.info.setTargetGroupName(JsonHelper.getStringFromJsonNode(new TextNode(targetGroup.getTargetGroupName())));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ADD_SYSTEM_TAGS {
      @Override
      public ResourceAction perform(ResourceAction action) throws Exception {
        AWSElasticLoadBalancingV2LoadBalancerResourceAction.addSystemTags(action);
        return action;
      }
    },
    REGISTER_TARGETS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSElasticLoadBalancingV2TargetGroupResourceAction action =
            (AWSElasticLoadBalancingV2TargetGroupResourceAction) resourceAction;
        if (action.properties.getTargets().isEmpty()) return action;
        final Loadbalancingv2Api elbv2 = AsyncProxy.client(
            Loadbalancingv2Api.class, MessageHelper.userIdentity(action.info.getEffectiveUserId()));
        final RegisterTargetsType registerTargets = new RegisterTargetsType();
        registerTargets.setTargetGroupArn(action.info.getPhysicalResourceId());
        final TargetDescriptions targetDescriptions = new TargetDescriptions();
        targetDescriptions.getMember().addAll(Stream.ofAll(action.properties.getTargets()).map(elbTargetDescription -> {
          final TargetDescription targetDescription = new TargetDescription();
          targetDescription.setAvailabilityZone(elbTargetDescription.getAvailabilityZone());
          targetDescription.setId(elbTargetDescription.getId());
          targetDescription.setPort(elbTargetDescription.getPort());
          return targetDescription;
        }).toJavaList());
        registerTargets.setTargets(targetDescriptions);
        elbv2.registerTargets(registerTargets);
        return action;
      }
    }
    //TODO:STEVE: where to set ARNS? need to refresh after any listener changes
    //final ArrayNode loadBalancerArnsArrayNode = JsonHelper.createArrayNode();
    //if (loadBalancer.getSecurityGroups() != null) {
    //  loadBalancer.getSecurityGroups().getMember().forEach(securityGroupsArrayNode::add);
    //}
    //action.info.setLoadBalancerArns(JsonHelper.getStringFromJsonNode(loadBalancerArnsArrayNode));
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        final AWSElasticLoadBalancingV2TargetGroupResourceAction newAction =
            (AWSElasticLoadBalancingV2TargetGroupResourceAction) newResourceAction;
        final AWSElasticLoadBalancingV2TargetGroupResourceAction oldAction =
            (AWSElasticLoadBalancingV2TargetGroupResourceAction) oldResourceAction;
        AWSElasticLoadBalancingV2LoadBalancerResourceAction.updateTags(
            oldAction,
            oldAction.properties.getTags(),
            newAction,
            newAction.properties.getTags());
        return newResourceAction;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_TARGET_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSElasticLoadBalancingV2TargetGroupResourceAction action =
            (AWSElasticLoadBalancingV2TargetGroupResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final Loadbalancingv2Api elbv2 = AsyncProxy.client(
            Loadbalancingv2Api.class, MessageHelper.userIdentity(action.info.getEffectiveUserId()));
        final DeleteTargetGroupType deleteTargetGroup = new DeleteTargetGroupType();
        deleteTargetGroup.setTargetGroupArn(action.info.getPhysicalResourceId());
        try {
          elbv2.deleteTargetGroup(deleteTargetGroup);
        } catch (final Exception e) {
          if (!AsyncExceptions.isWebServiceErrorCode(e, "TargetGroupNotFound")) {
            throw e;
          }
        }
        return action;
      }
    }
  }
}

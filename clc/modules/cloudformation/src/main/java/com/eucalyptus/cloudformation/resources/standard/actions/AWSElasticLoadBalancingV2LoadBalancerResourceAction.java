/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSElasticLoadBalancingV2LoadBalancerResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSElasticLoadBalancingV2LoadBalancerProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Api;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2ResourceName;
import com.eucalyptus.loadbalancingv2.common.msgs.AddTagsType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateLoadBalancerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateLoadBalancerType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteLoadBalancerType;
import com.eucalyptus.loadbalancingv2.common.msgs.LoadBalancer;
import com.eucalyptus.loadbalancingv2.common.msgs.RemoveTagsType;
import com.eucalyptus.loadbalancingv2.common.msgs.ResourceArns;
import com.eucalyptus.loadbalancingv2.common.msgs.SecurityGroups;
import com.eucalyptus.loadbalancingv2.common.msgs.Subnets;
import com.eucalyptus.loadbalancingv2.common.msgs.Tag;
import com.eucalyptus.loadbalancingv2.common.msgs.TagKeys;
import com.eucalyptus.loadbalancingv2.common.msgs.TagList;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.vavr.collection.Stream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.apache.log4j.Logger;

public class AWSElasticLoadBalancingV2LoadBalancerResourceAction extends StepBasedResourceAction {

  public static final Logger LOG =
      Logger.getLogger(AWSElasticLoadBalancingV2LoadBalancerResourceAction.class);

  private static final Set<Function<AWSElasticLoadBalancingV2LoadBalancerProperties,Object>> PROP_REPLACE =
      ImmutableSet.<Function<AWSElasticLoadBalancingV2LoadBalancerProperties,Object>>builder()
          .add(AWSElasticLoadBalancingV2LoadBalancerProperties::getIpAddressType)
          .add(AWSElasticLoadBalancingV2LoadBalancerProperties::getName)
          .add(AWSElasticLoadBalancingV2LoadBalancerProperties::getScheme)
          .add(AWSElasticLoadBalancingV2LoadBalancerProperties::getSecurityGroups)
          .add(AWSElasticLoadBalancingV2LoadBalancerProperties::getSubnets)
          .add(AWSElasticLoadBalancingV2LoadBalancerProperties::getType)
          .build();

  private static final Set<Function<AWSElasticLoadBalancingV2LoadBalancerProperties,Object>> PROP_SOMEINTER =
      ImmutableSet.<Function<AWSElasticLoadBalancingV2LoadBalancerProperties,Object>>builder()
          .build();

  private static final Set<Function<AWSElasticLoadBalancingV2LoadBalancerProperties,Object>> PROP_NOINTER =
      ImmutableSet.<Function<AWSElasticLoadBalancingV2LoadBalancerProperties,Object>>builder()
          .add(AWSElasticLoadBalancingV2LoadBalancerProperties::getTags)
          .build();

  private AWSElasticLoadBalancingV2LoadBalancerProperties properties =
      new AWSElasticLoadBalancingV2LoadBalancerProperties();
  private AWSElasticLoadBalancingV2LoadBalancerResourceInfo info =
      new AWSElasticLoadBalancingV2LoadBalancerResourceInfo();

  public AWSElasticLoadBalancingV2LoadBalancerResourceAction() {
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
    properties = (AWSElasticLoadBalancingV2LoadBalancerProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSElasticLoadBalancingV2LoadBalancerResourceInfo) resourceInfo;
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    final AWSElasticLoadBalancingV2LoadBalancerResourceAction otherAction =
        (AWSElasticLoadBalancingV2LoadBalancerResourceAction) resourceAction;
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
        Loadbalancingv2ResourceName.parse(arn, Loadbalancingv2ResourceName.Type.loadbalancer);
    return String.format("%s/%s/%s",
        resourceName.getSubType(Loadbalancingv2ResourceName.Type.loadbalancer),
        resourceName.getName(),
        resourceName.getId(Loadbalancingv2ResourceName.Type.loadbalancer));
  }

  private enum CreateSteps implements Step {
    CREATE_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSElasticLoadBalancingV2LoadBalancerResourceAction action =
            (AWSElasticLoadBalancingV2LoadBalancerResourceAction) resourceAction;
        final Loadbalancingv2Api elbv2 = AsyncProxy.client(
            Loadbalancingv2Api.class, MessageHelper.userIdentity(action.info.getEffectiveUserId()));
        final CreateLoadBalancerType createLoadBalancer = new CreateLoadBalancerType();
        if (action.properties.getName() == null) {
          createLoadBalancer.setName(action.getDefaultPhysicalResourceId(32));
        } else {
          createLoadBalancer.setName(action.properties.getName());
        }
        createLoadBalancer.setType(action.properties.getType());
        createLoadBalancer.setScheme(action.properties.getScheme());
        if (!action.properties.getSecurityGroups().isEmpty()) {
          final SecurityGroups securityGroups = new SecurityGroups();
          securityGroups.setMember(Lists.newArrayList(action.properties.getSecurityGroups()));
          createLoadBalancer.setSecurityGroups(securityGroups);
        }
        if (!action.properties.getSubnets().isEmpty()) {
          final Subnets subnets = new Subnets();
          subnets.setMember(Lists.newArrayList(action.properties.getSubnets()));
          createLoadBalancer.setSubnets(subnets);
        }
        final Set<CloudFormationResourceTag> tags = getTags(action, action.properties.getTags());
        if (!tags.isEmpty()) {
          createLoadBalancer.setTags(toTagList(tags));
        }
        final CreateLoadBalancerResponseType createResponse =
            elbv2.createLoadBalancer(createLoadBalancer);
        final LoadBalancer loadBalancer =
            createResponse.getCreateLoadBalancerResult().getLoadBalancers().getMember().get(0);
        final ArrayNode securityGroupsArrayNode = JsonHelper.createArrayNode();
        if (loadBalancer.getSecurityGroups() != null) {
          loadBalancer.getSecurityGroups().getMember().forEach(securityGroupsArrayNode::add);
        }
        action.info.setPhysicalResourceId(loadBalancer.getLoadBalancerArn());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setCanonicalHostedZoneID(JsonHelper.getStringFromJsonNode(new TextNode(loadBalancer.getCanonicalHostedZoneId())));
        action.info.setDnsName(JsonHelper.getStringFromJsonNode(new TextNode(loadBalancer.getDNSName())));
        action.info.setLoadBalancerFullName(JsonHelper.getStringFromJsonNode(new TextNode(arnToFullName(loadBalancer.getLoadBalancerArn()))));
        action.info.setLoadBalancerName(JsonHelper.getStringFromJsonNode(new TextNode(loadBalancer.getLoadBalancerName())));
        action.info.setSecurityGroups(JsonHelper.getStringFromJsonNode(securityGroupsArrayNode));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    ADD_SYSTEM_TAGS {
      @Override
      public ResourceAction perform(ResourceAction action) throws Exception {
        addSystemTags(action);
        return action;
      }
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        final AWSElasticLoadBalancingV2LoadBalancerResourceAction newAction =
            (AWSElasticLoadBalancingV2LoadBalancerResourceAction) newResourceAction;
        final AWSElasticLoadBalancingV2LoadBalancerResourceAction oldAction =
            (AWSElasticLoadBalancingV2LoadBalancerResourceAction) oldResourceAction;
        updateTags(
            oldAction,
            oldAction.properties.getTags(),
            newAction,
            newAction.properties.getTags());
        return newResourceAction;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_LOAD_BALANCER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSElasticLoadBalancingV2LoadBalancerResourceAction action =
            (AWSElasticLoadBalancingV2LoadBalancerResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final Loadbalancingv2Api elbv2 = AsyncProxy.client(
            Loadbalancingv2Api.class, MessageHelper.userIdentity(action.info.getEffectiveUserId()));
        final DeleteLoadBalancerType deleteLoadBalancer = new DeleteLoadBalancerType();
        deleteLoadBalancer.setLoadBalancerArn(action.info.getPhysicalResourceId());
        try {
          elbv2.deleteLoadBalancer(deleteLoadBalancer);
        } catch (final Exception e) {
          if (!AsyncExceptions.isWebServiceErrorCode(e, "LoadBalancerNotFound")) {
            throw e;
          }
        }
        return action;
      }
    }
  }

  protected static void addSystemTags(final ResourceAction action) {
    final List<CloudFormationResourceTag> tags =
        TagHelper.getCloudFormationResourceSystemTags(action.getResourceInfo(), action.getStackEntity());
    final Loadbalancingv2Api elbv2 = AsyncProxy.client(
        Loadbalancingv2Api.class,
        MessageHelper.privilegedUserIdentity(action.getResourceInfo().getEffectiveUserId()));
    final AddTagsType addTags = new AddTagsType();
    addTags.setResourceArns(toResourceArns(action.getResourceInfo().getPhysicalResourceId()));
    addTags.setTags(toTagList(tags));
    elbv2.addTags(addTags);
  }

  protected static void updateTags(
      final ResourceAction oldAction,
      final ArrayList<CloudFormationResourceTag> oldTagProperty,
      final ResourceAction newAction,
      final ArrayList<CloudFormationResourceTag> newTagProperty
  ) throws Exception {
    final Loadbalancingv2Api elbv2 = AsyncProxy.client(
        Loadbalancingv2Api.class,
        MessageHelper.userIdentity(newAction.getResourceInfo().getEffectiveUserId()));

    final Set<CloudFormationResourceTag> oldTags = getTags(oldAction, oldTagProperty, false);
    final Set<CloudFormationResourceTag> newTags = getTags(newAction, newTagProperty);

    // remove tags
    final Set<String> removedKeys = Sets.newHashSet();
    removedKeys.addAll(Stream.ofAll(oldTags).map(CloudFormationResourceTag::getKey).toJavaList());
    removedKeys.removeAll(Stream.ofAll(newTags).map(CloudFormationResourceTag::getKey).toJavaList());
    if (!removedKeys.isEmpty()) {
      final RemoveTagsType removeTags = new RemoveTagsType();
      removeTags.setResourceArns(toResourceArns(newAction.getResourceInfo().getPhysicalResourceId()));
      removeTags.setTagKeys(toTagKeys(removedKeys));
      elbv2.removeTags(removeTags);
    }

    // add or update tags
    newTags.removeAll(oldTags);
    if (!newTags.isEmpty()) {
      final AddTagsType addTags = new AddTagsType();
      addTags.setResourceArns(toResourceArns(newAction.getResourceInfo().getPhysicalResourceId()));
      addTags.setTags(toTagList(newTags));
      elbv2.addTags(addTags);
    }
  }

  protected static Set<CloudFormationResourceTag> getTags(
      final ResourceAction action,
      final ArrayList<CloudFormationResourceTag> tagProperty
  ) throws Exception {
    return getTags(action, tagProperty, true);
  }

  protected static Set<CloudFormationResourceTag> getTags(
      final ResourceAction action,
      final ArrayList<CloudFormationResourceTag> tagProperty,
      final boolean checkReservedTags
  ) throws Exception {
    final Set<CloudFormationResourceTag> tags = Sets.newLinkedHashSet();
    tags.addAll(TagHelper.getCloudFormationResourceStackTags(action.getStackEntity()));
    if (tagProperty != null && !tagProperty.isEmpty()) {
      if (checkReservedTags) {
        TagHelper.checkReservedCloudFormationResourceTemplateTags(tagProperty);
      }
      tags.addAll(tagProperty);
    }
    return tags;
  }

  protected static ResourceArns toResourceArns(final String resourceArn) {
    final ResourceArns resourceArns = new ResourceArns();
    resourceArns.getMember().add(resourceArn);
    return resourceArns;
  }

  protected static TagKeys toTagKeys(final Set<String> tags) {
    final TagKeys tagKeys = new TagKeys();
    tagKeys.getMember().addAll(tags);
    return tagKeys;
  }

  protected static TagList toTagList(final Iterable<CloudFormationResourceTag> tags) {
    final TagList tagList = new TagList();
    tagList.getMember().addAll(Stream.ofAll(tags).map( cfTag -> {
      Tag tag = new Tag();
      tag.setKey(cfTag.getKey());
      tag.setValue(cfTag.getValue());
      return tag;
    }).toJavaList());
    return tagList;
  }
}

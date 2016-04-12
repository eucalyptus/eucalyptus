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


import com.eucalyptus.auth.Accounts;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupNames;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingNotificationTypes;
import com.eucalyptus.autoscaling.common.msgs.AvailabilityZones;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsResponseType;
import com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsType;
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.autoscaling.common.msgs.LoadBalancerNames;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.TagType;
import com.eucalyptus.autoscaling.common.msgs.Tags;
import com.eucalyptus.autoscaling.common.msgs.TerminationPolicies;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.SignalEntity;
import com.eucalyptus.cloudformation.entity.SignalEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingAutoScalingGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSAutoScalingAutoScalingGroupProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingTag;
import com.eucalyptus.cloudformation.template.CreationPolicy;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.ResourceFailureException;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSAutoScalingAutoScalingGroupResourceAction extends StepBasedResourceAction {

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an autoscaling group to have zero instances during delete")
  public static volatile Integer AUTOSCALING_GROUP_ZERO_INSTANCES_MAX_DELETE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an autoscaling group to be deleted after deletion)")
  public static volatile Integer AUTOSCALING_GROUP_DELETED_MAX_DELETE_RETRY_SECS = 300;

  private AWSAutoScalingAutoScalingGroupProperties properties = new AWSAutoScalingAutoScalingGroupProperties();
  private AWSAutoScalingAutoScalingGroupResourceInfo info = new AWSAutoScalingAutoScalingGroupResourceInfo();

  public AWSAutoScalingAutoScalingGroupResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null);
  }

  private enum CreateSteps implements Step {
    CREATE_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        String autoScalingGroupName = action.getDefaultPhysicalResourceId();
        CreateAutoScalingGroupType createAutoScalingGroupType = MessageHelper.createMessage(CreateAutoScalingGroupType.class, action.info.getEffectiveUserId());
        createAutoScalingGroupType.setAutoScalingGroupName(autoScalingGroupName);
        if (action.properties.getInstanceId() != null) {
          throw new ValidationErrorException("InstanceId not supported");
        }
        if (action.properties.getLaunchConfigurationName() == null) {
          throw new ValidationErrorException("LaunchConfiguration required (as InstanceId not supported)");
        }
        if (action.properties.getAvailabilityZones() != null) {
          createAutoScalingGroupType.setAvailabilityZones(new AvailabilityZones(action.properties.getAvailabilityZones()));
        }
        createAutoScalingGroupType.setDefaultCooldown(action.properties.getCooldown());
        createAutoScalingGroupType.setDesiredCapacity(action.properties.getDesiredCapacity());
        createAutoScalingGroupType.setHealthCheckGracePeriod(action.properties.getHealthCheckGracePeriod());
        createAutoScalingGroupType.setLaunchConfigurationName(action.properties.getLaunchConfigurationName());
        if (action.properties.getLoadBalancerNames() != null) {
          createAutoScalingGroupType.setLoadBalancerNames(new LoadBalancerNames(action.properties.getLoadBalancerNames()));
        }
        createAutoScalingGroupType.setMaxSize(action.properties.getMaxSize());
        createAutoScalingGroupType.setMinSize(action.properties.getMinSize());
        if (action.properties.getTerminationPolicies() != null) {
          createAutoScalingGroupType.setTerminationPolicies(new TerminationPolicies(action.properties.getTerminationPolicies()));
        }
        if (action.properties.getVpcZoneIdentifier() != null) {
          createAutoScalingGroupType.setVpcZoneIdentifier(Strings.emptyToNull(Joiner.on(",").join(action.properties.getVpcZoneIdentifier())));
        }
        AsyncRequests.<CreateAutoScalingGroupType, CreateAutoScalingGroupResponseType>sendSync(configuration, createAutoScalingGroupType);
        action.info.setPhysicalResourceId(autoScalingGroupName);
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        action.info.setEucaCreateStartTime(JsonHelper.getStringFromJsonNode(new TextNode("" + System.currentTimeMillis())));
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        // Create 'system' tags as admin user
        String effectiveAdminUserId = Accounts.lookupPrincipalByAccountNumber(Accounts.lookupPrincipalByUserId(action.info.getEffectiveUserId()).getAccountNumber()).getUserId();
        CreateOrUpdateTagsType createSystemTagsType = MessageHelper.createPrivilegedMessage(CreateOrUpdateTagsType.class, effectiveAdminUserId);
        createSystemTagsType.setTags(convertAutoScalingTagsToCreateOrUpdateTags(action.info.getPhysicalResourceId(), TagHelper.getAutoScalingSystemTags(action.info, action.getStackEntity())));
        AsyncRequests.<CreateOrUpdateTagsType, CreateOrUpdateTagsResponseType>sendSync(configuration, createSystemTagsType);
        // Create non-system tags as regular user
        List<AutoScalingTag> tags = TagHelper.getAutoScalingStackTags(action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedAutoScalingTemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags());
        }
        if (!tags.isEmpty()) {
          CreateOrUpdateTagsType createOrUpdateTagsType = MessageHelper.createMessage(CreateOrUpdateTagsType.class, action.info.getEffectiveUserId());
          createOrUpdateTagsType.setTags(convertAutoScalingTagsToCreateOrUpdateTags(action.info.getPhysicalResourceId(), tags));
          AsyncRequests.<CreateOrUpdateTagsType, CreateOrUpdateTagsResponseType>sendSync(configuration, createOrUpdateTagsType);
        }
        return action;
      }

      private Tags convertAutoScalingTagsToCreateOrUpdateTags(String physicalResourceId, List<AutoScalingTag> autoScalingTags) {
        if (autoScalingTags == null) return null;
        Tags tags = new Tags();
        ArrayList<TagType> member = Lists.newArrayList();
        for (AutoScalingTag autoScalingTag : autoScalingTags) {
          TagType tagType = new TagType();
          tagType.setResourceType("auto-scaling-group");
          tagType.setResourceId(physicalResourceId);
          tagType.setKey(autoScalingTag.getKey());
          tagType.setValue(autoScalingTag.getValue());
          tagType.setPropagateAtLaunch(autoScalingTag.getPropagateAtLaunch());
          member.add(tagType);
        }
        tags.setMember(member);
        return tags;
      }
    },
    ADD_NOTIFICATION_CONFIGURATIONS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (action.properties.getNotificationConfiguration() != null) {
          PutNotificationConfigurationType putNotificationConfigurationType = MessageHelper.createMessage(PutNotificationConfigurationType.class, action.info.getEffectiveUserId());
          putNotificationConfigurationType.setAutoScalingGroupName(action.info.getPhysicalResourceId());
          putNotificationConfigurationType.setTopicARN(action.properties.getNotificationConfiguration().getTopicARN());
          AutoScalingNotificationTypes autoScalingNotificationTypes = new AutoScalingNotificationTypes();
          ArrayList<String> member = Lists.newArrayList(action.properties.getNotificationConfiguration().getNotificationTypes());
          autoScalingNotificationTypes.setMember(member);
          putNotificationConfigurationType.setNotificationTypes(autoScalingNotificationTypes);
          AsyncRequests.<PutNotificationConfigurationType, PutNotificationConfigurationResponseType>sendSync(configuration, putNotificationConfigurationType);
        }
        return action;
      }
    },
    CHECK_SIGNALS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        CreationPolicy creationPolicy = CreationPolicy.parse(action.info.getCreationPolicyJson());
        if (creationPolicy != null && creationPolicy.getResourceSignal() != null) {
          // For some reason AWS completely ignores signals that are not instance ids in the asg.
          Set<String> instanceIds = Sets.newHashSet();
          DescribeAutoScalingGroupsType describeAutoScalingGroupsType = MessageHelper.createMessage(DescribeAutoScalingGroupsType.class, action.info.getEffectiveUserId());
          AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
          autoScalingGroupNames.getMember().add(action.info.getPhysicalResourceId());
          describeAutoScalingGroupsType.setAutoScalingGroupNames(autoScalingGroupNames);
          DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType = AsyncRequests.sendSync(configuration, describeAutoScalingGroupsType);
          if (describeAutoScalingGroupsResponseType != null && describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult() != null &&
            describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups() != null &&
            describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember() != null) {
            for (AutoScalingGroupType autoScalingGroupType: describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember()) {
              if (!Objects.equals(autoScalingGroupType.getAutoScalingGroupName(), action.info.getPhysicalResourceId())) continue;
              if (autoScalingGroupType.getInstances() != null && autoScalingGroupType.getInstances().getMember() != null) {
                for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
                  instanceIds.add(instance.getInstanceId());
                }
              }
            }
          }
          // check for signals
          Collection<SignalEntity> signals = SignalEntityManager.getSignals(action.getStackEntity().getStackId(), action.info.getAccountId(), action.info.getLogicalResourceId(),
            action.getStackEntity().getStackVersion());
          int numSuccessSignals = 0;
          if (signals != null) {
            for (SignalEntity signal : signals) {
              // Honor old signals in case some instance is no longer in the group
              if (signal.getProcessed() && signal.getStatus() == SignalEntity.Status.SUCCESS) {
                numSuccessSignals++;
                continue;
              }
              // Ignore signals with ids not from the list of instance ids.
              if (!instanceIds.contains(signal.getUniqueId())) continue;

              if (signal.getStatus() == SignalEntity.Status.FAILURE) {
                throw new ResourceFailureException("Received FAILURE signal with UniqueId " + signal.getUniqueId());
              }
              if (!signal.getProcessed()) {
                StackEventEntityManager.addSignalStackEvent(signal);
                signal.setProcessed(true);
                SignalEntityManager.updateSignal(signal);
              }
              numSuccessSignals++;
            }
          }
          if (numSuccessSignals < creationPolicy.getResourceSignal().getCount()) {
            long durationMs = System.currentTimeMillis() - Long.valueOf(JsonHelper.getJsonNodeFromString(action.info.getEucaCreateStartTime()).asText());
            if (TimeUnit.MILLISECONDS.toSeconds(durationMs) > creationPolicy.getResourceSignal().getTimeout()) {
              throw new ResourceFailureException("Failed to receive " + creationPolicy.getResourceSignal().getCount() + " resource signal(s) within the specified duration");
            }
            throw new RetryAfterConditionCheckFailedException("Not enough success signals yet");
          }
        }
        return action;
      }

      @Nullable
      @Override
      public Integer getTimeout() {
        return (int) TimeUnit.HOURS.toSeconds(12);
      }
    };
    // no retries on most steps
    @Override
    public Integer getTimeout( ) {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    SET_ZERO_CAPACITY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (groupDoesNotExist(configuration, action)) return action;
        UpdateAutoScalingGroupType updateAutoScalingGroupType = MessageHelper.createMessage(UpdateAutoScalingGroupType.class, action.info.getEffectiveUserId());
        updateAutoScalingGroupType.setMinSize(0);
        updateAutoScalingGroupType.setMaxSize(0);
        updateAutoScalingGroupType.setDesiredCapacity(0);
        updateAutoScalingGroupType.setAutoScalingGroupName(action.info.getPhysicalResourceId());
        updateAutoScalingGroupType.setEffectiveUserId(action.info.getEffectiveUserId());
        AsyncRequests.<UpdateAutoScalingGroupType, UpdateAutoScalingGroupResponseType>sendSync(configuration, updateAutoScalingGroupType);
        return action;
      }
    },

    VERIFY_ZERO_INSTANCES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (groupDoesNotExist(configuration, action)) return action;
        DescribeAutoScalingGroupsType describeAutoScalingGroupsType = MessageHelper.createMessage(DescribeAutoScalingGroupsType.class, action.info.getEffectiveUserId());
        AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
        ArrayList<String> member = Lists.newArrayList(action.info.getPhysicalResourceId());
        autoScalingGroupNames.setMember(member);
        describeAutoScalingGroupsType.setAutoScalingGroupNames(autoScalingGroupNames);
        DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType = AsyncRequests.<DescribeAutoScalingGroupsType,DescribeAutoScalingGroupsResponseType> sendSync(configuration, describeAutoScalingGroupsType);
        if (action.doesGroupNotExist(describeAutoScalingGroupsResponseType)) {
          return action; // group is gone...
        } else {
          AutoScalingGroupType firstGroup = describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().get(0);
          if (firstGroup.getInstances() == null || firstGroup.getInstances().getMember() == null || firstGroup.getInstances().getMember().isEmpty()) {
            return action; // Group has zero instances
          }
        }
        throw new RetryAfterConditionCheckFailedException("Autoscaling group " + action.info.getPhysicalResourceId() + " still has instances");
      }

      @Override
      public Integer getTimeout( ) {
        return AUTOSCALING_GROUP_ZERO_INSTANCES_MAX_DELETE_RETRY_SECS;
      }
    },
    DELETE_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (groupDoesNotExist(configuration, action)) return action;
        DeleteAutoScalingGroupType deleteAutoScalingGroupType = MessageHelper.createMessage(DeleteAutoScalingGroupType.class, action.info.getEffectiveUserId());
        deleteAutoScalingGroupType.setAutoScalingGroupName(action.info.getPhysicalResourceId());
        AsyncRequests.<DeleteAutoScalingGroupType,DeleteAutoScalingGroupResponseType> sendSync(configuration, deleteAutoScalingGroupType);
        return action;
      }
    },
    VERIFY_GROUP_DELETED {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (groupDoesNotExist(configuration, action)) return action;
        throw new RetryAfterConditionCheckFailedException("Autoscaling group " + action.info.getPhysicalResourceId() + " is not yet deleted");
      }

      @Override
      public Integer getTimeout() {
        return AUTOSCALING_GROUP_DELETED_MAX_DELETE_RETRY_SECS;
      }
    };

    private static boolean groupDoesNotExist(ServiceConfiguration configuration, AWSAutoScalingAutoScalingGroupResourceAction action) throws Exception {
      // See if resource was ever populated...
      if (action.info.getCreatedEnoughToDelete() != Boolean.TRUE) return true;
      // See if group still exists
      DescribeAutoScalingGroupsType describeAutoScalingGroupsType = MessageHelper.createMessage(DescribeAutoScalingGroupsType.class, action.info.getEffectiveUserId());
      AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
      ArrayList<String> member = Lists.newArrayList(action.info.getPhysicalResourceId());
      autoScalingGroupNames.setMember(member);
      describeAutoScalingGroupsType.setAutoScalingGroupNames(autoScalingGroupNames);
      DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType = AsyncRequests.<DescribeAutoScalingGroupsType, DescribeAutoScalingGroupsResponseType>sendSync(configuration, describeAutoScalingGroupsType);
      if (action.doesGroupNotExist(describeAutoScalingGroupsResponseType)) {
        return true;
      }
      return false;
    }

    public Integer getTimeout( ) {
      return null;
    }
  }


  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSAutoScalingAutoScalingGroupProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSAutoScalingAutoScalingGroupResourceInfo) resourceInfo;
  }

  private boolean doesGroupNotExist(DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsResponseType) {
    return describeAutoScalingGroupsResponseType == null || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult() == null
      || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups() == null
      || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember() == null
      || describeAutoScalingGroupsResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().isEmpty();
  }



}



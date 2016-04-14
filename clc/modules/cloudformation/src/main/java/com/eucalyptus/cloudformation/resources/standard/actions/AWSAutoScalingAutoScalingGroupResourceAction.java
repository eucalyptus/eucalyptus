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
import com.eucalyptus.autoscaling.common.msgs.DeleteNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.DeleteTagsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DeleteTagsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeNotificationConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeNotificationConfigurationsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeTagsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeTagsType;
import com.eucalyptus.autoscaling.common.msgs.Filter;
import com.eucalyptus.autoscaling.common.msgs.Filters;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.autoscaling.common.msgs.LoadBalancerNames;
import com.eucalyptus.autoscaling.common.msgs.NotificationConfiguration;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.TagDescription;
import com.eucalyptus.autoscaling.common.msgs.TagType;
import com.eucalyptus.autoscaling.common.msgs.Tags;
import com.eucalyptus.autoscaling.common.msgs.TerminationPolicies;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.Values;
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
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingNotificationConfiguration;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.CreationPolicy;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.ResourceFailureException;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSAutoScalingAutoScalingGroupResourceAction otherAction = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
    if (!Objects.equals(properties.getAvailabilityZones(), otherAction.properties.getAvailabilityZones())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getCooldown(), otherAction.properties.getCooldown())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getDesiredCapacity(), otherAction.properties.getDesiredCapacity())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getHealthCheckGracePeriod(), otherAction.properties.getHealthCheckGracePeriod())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getHealthCheckType(), otherAction.properties.getHealthCheckType())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getInstanceId(), otherAction.properties.getInstanceId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getLaunchConfigurationName(), otherAction.properties.getLaunchConfigurationName())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getLoadBalancerNames(), otherAction.properties.getLoadBalancerNames())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getMaxSize(), otherAction.properties.getMaxSize())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getMinSize(), otherAction.properties.getMinSize())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getNotificationConfigurations(), otherAction.properties.getNotificationConfigurations())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getTerminationPolicies(), otherAction.properties.getTerminationPolicies())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getVpcZoneIdentifier(), otherAction.properties.getVpcZoneIdentifier())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION); // docs say some interruption but testing shows none.  May revisit on update policy
    }
    return updateType;
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
        createAutoScalingGroupType.setHealthCheckType(action.properties.getHealthCheckType());
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

    },
    ADD_NOTIFICATION_CONFIGURATIONS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (action.properties.getNotificationConfigurations() != null) {
          for (AutoScalingNotificationConfiguration notificationConfiguration: action.properties.getNotificationConfigurations()) {
            PutNotificationConfigurationType putNotificationConfigurationType = MessageHelper.createMessage(PutNotificationConfigurationType.class, action.info.getEffectiveUserId());
            putNotificationConfigurationType.setAutoScalingGroupName(action.info.getPhysicalResourceId());
            putNotificationConfigurationType.setTopicARN(notificationConfiguration.getTopicARN());
            AutoScalingNotificationTypes autoScalingNotificationTypes = new AutoScalingNotificationTypes();
            ArrayList<String> member = Lists.newArrayList(notificationConfiguration.getNotificationTypes());
            autoScalingNotificationTypes.setMember(member);
            putNotificationConfigurationType.setNotificationTypes(autoScalingNotificationTypes);
            AsyncRequests.<PutNotificationConfigurationType, PutNotificationConfigurationResponseType>sendSync(configuration, putNotificationConfigurationType);
          }
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

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_GROUP {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction oldAction = (AWSAutoScalingAutoScalingGroupResourceAction) oldResourceAction;
        AWSAutoScalingAutoScalingGroupResourceAction newAction = (AWSAutoScalingAutoScalingGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        String autoScalingGroupName = newAction.info.getPhysicalResourceId();
        UpdateAutoScalingGroupType updateAutoScalingGroupType = MessageHelper.createMessage(UpdateAutoScalingGroupType.class, newAction.info.getEffectiveUserId());
        updateAutoScalingGroupType.setAutoScalingGroupName(autoScalingGroupName);
        if (newAction.properties.getInstanceId() != null) {
          throw new ValidationErrorException("InstanceId not supported");
        }
        if (newAction.properties.getLaunchConfigurationName() == null) {
          throw new ValidationErrorException("LaunchConfiguration required (as InstanceId not supported)");
        }
        if (newAction.properties.getAvailabilityZones() != null) {
          updateAutoScalingGroupType.setAvailabilityZones(new AvailabilityZones(newAction.properties.getAvailabilityZones()));
        }
        updateAutoScalingGroupType.setDefaultCooldown(newAction.properties.getCooldown());
        updateAutoScalingGroupType.setDesiredCapacity(newAction.properties.getDesiredCapacity());
        updateAutoScalingGroupType.setHealthCheckGracePeriod(newAction.properties.getHealthCheckGracePeriod());
        updateAutoScalingGroupType.setHealthCheckType(newAction.properties.getHealthCheckType());
        updateAutoScalingGroupType.setLaunchConfigurationName(newAction.properties.getLaunchConfigurationName());
        updateAutoScalingGroupType.setMaxSize(newAction.properties.getMaxSize());
        updateAutoScalingGroupType.setMinSize(newAction.properties.getMinSize());
        if (newAction.properties.getTerminationPolicies() != null) {
          updateAutoScalingGroupType.setTerminationPolicies(new TerminationPolicies(newAction.properties.getTerminationPolicies()));
        }
        if (newAction.properties.getVpcZoneIdentifier() != null) {
          updateAutoScalingGroupType.setVpcZoneIdentifier(Strings.emptyToNull(Joiner.on(",").join(newAction.properties.getVpcZoneIdentifier())));
        }
        AsyncRequests.<UpdateAutoScalingGroupType, UpdateAutoScalingGroupResponseType>sendSync(configuration, updateAutoScalingGroupType);
        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction oldAction = (AWSAutoScalingAutoScalingGroupResourceAction) oldResourceAction;
        AWSAutoScalingAutoScalingGroupResourceAction newAction = (AWSAutoScalingAutoScalingGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        DescribeTagsType describeTagsType = MessageHelper.createMessage(DescribeTagsType.class, newAction.info.getEffectiveUserId());
        Filters filters = new Filters();
        Filter filter = new Filter();
        filter.setName("auto-scaling-group");
        Values values = new Values();
        values.getMember().add(newAction.info.getPhysicalResourceId());
        filter.setValues(values);
        filters.getMember().add(filter);
        describeTagsType.setFilters(filters);
        DescribeTagsResponseType describeTagsResponseType = AsyncRequests.sendSync(configuration, describeTagsType);
        Set<AutoScalingTag> existingTags = Sets.newLinkedHashSet();
        if (describeTagsResponseType != null  & describeTagsResponseType.getDescribeTagsResult() != null && 
          describeTagsResponseType.getDescribeTagsResult().getTags() != null && describeTagsResponseType.getDescribeTagsResult().getTags().getMember() != null) {
          for (TagDescription tagDescription: describeTagsResponseType.getDescribeTagsResult().getTags().getMember()) {
            AutoScalingTag tag = new AutoScalingTag();
            tag.setKey(tagDescription.getKey());
            tag.setValue(tagDescription.getValue());
            tag.setPropagateAtLaunch(tagDescription.getPropagateAtLaunch());
            existingTags.add(tag);
          }
        }
        Set<AutoScalingTag> newTags = Sets.newLinkedHashSet();
        if (newAction.properties.getTags() != null) {
          newTags.addAll(newAction.properties.getTags());
        }
        List<AutoScalingTag> newStackTags = TagHelper.getAutoScalingStackTags(newAction.getStackEntity());
        if (newStackTags != null) {
          newTags.addAll(newStackTags);
        }
        TagHelper.checkReservedAutoScalingTemplateTags(newTags);
        // Note: tag equality includes the 'propegateAtLaunch' field but all fields have to match for delete to work, so we are ok using equals()
        // add only 'new' tags
        Set<AutoScalingTag> onlyNewTags = Sets.difference(newTags, existingTags);
        if (!onlyNewTags.isEmpty()) {
          CreateOrUpdateTagsType createOrUpdateTagsType = MessageHelper.createMessage(CreateOrUpdateTagsType.class, newAction.info.getEffectiveUserId());
          createOrUpdateTagsType.setTags(convertAutoScalingTagsToCreateOrUpdateTags(newAction.info.getPhysicalResourceId(), onlyNewTags));
          AsyncRequests.<CreateOrUpdateTagsType, CreateOrUpdateTagsResponseType>sendSync(configuration, createOrUpdateTagsType);
        }
        //  Get old tags...
        Set<AutoScalingTag> oldTags = Sets.newLinkedHashSet();
        if (oldAction.properties.getTags() != null) {
          oldTags.addAll(oldAction.properties.getTags());
        }
        List<AutoScalingTag> oldStackTags = TagHelper.getAutoScalingStackTags(oldAction.getStackEntity());
        if (oldStackTags != null) {
          oldTags.addAll(oldStackTags);
        }

        // remove only the old tags that are not new and that exist
        Set<AutoScalingTag> tagsToRemove = Sets.intersection(oldTags, Sets.difference(existingTags, newTags));
        if (!tagsToRemove.isEmpty()) {
          DeleteTagsType deleteTagsType = MessageHelper.createMessage(DeleteTagsType.class, newAction.info.getEffectiveUserId());
          deleteTagsType.setTags(convertAutoScalingTagsToCreateOrUpdateTags(newAction.info.getPhysicalResourceId(), tagsToRemove));
          AsyncRequests.<DeleteTagsType, DeleteTagsResponseType>sendSync(configuration, deleteTagsType);
        }
        return newAction;
      }
    },
    UPDATE_NOTIFICATION_CONFIGURATIONS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction oldAction = (AWSAutoScalingAutoScalingGroupResourceAction) oldResourceAction;
        AWSAutoScalingAutoScalingGroupResourceAction newAction = (AWSAutoScalingAutoScalingGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);

        Map<String, Collection<String>> existingNotificationConfigurations = Maps.newHashMap();
        DescribeNotificationConfigurationsType describeNotificationConfigurationsType = MessageHelper.createMessage(DescribeNotificationConfigurationsType.class, newAction.info.getEffectiveUserId());
        AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
        autoScalingGroupNames.getMember().add(newAction.info.getPhysicalResourceId());
        describeNotificationConfigurationsType.setAutoScalingGroupNames(autoScalingGroupNames);
        DescribeNotificationConfigurationsResponseType describeNotificationConfigurationsResponseType = AsyncRequests.sendSync(configuration, describeNotificationConfigurationsType);
        if (describeNotificationConfigurationsResponseType != null && describeNotificationConfigurationsResponseType.getDescribeNotificationConfigurationsResult() != null &&
          describeNotificationConfigurationsResponseType.getDescribeNotificationConfigurationsResult().getNotificationConfigurations() != null &&
          describeNotificationConfigurationsResponseType.getDescribeNotificationConfigurationsResult().getNotificationConfigurations().getMember() != null) {
          for (NotificationConfiguration notificationConfiguration: describeNotificationConfigurationsResponseType.getDescribeNotificationConfigurationsResult().getNotificationConfigurations().getMember()) {
            if (!existingNotificationConfigurations.containsKey(notificationConfiguration.getTopicARN())) {
              existingNotificationConfigurations.put(notificationConfiguration.getTopicARN(), Sets.newHashSet());
            }
            existingNotificationConfigurations.get(notificationConfiguration.getTopicARN()).add(notificationConfiguration.getNotificationType());
          }
        }

        Map<String, Collection<String>> newNotificationConfigurations = Maps.newHashMap();
        if (newAction.properties.getNotificationConfigurations() != null) {
          for (AutoScalingNotificationConfiguration notificationConfiguration: newAction.properties.getNotificationConfigurations()) {
            newNotificationConfigurations.put(notificationConfiguration.getTopicARN(), Sets.newHashSet(notificationConfiguration.getNotificationTypes()));
          }
        }

        Map<String, Collection<String>> oldNotificationConfigurations = Maps.newHashMap();
        if (newAction.properties.getNotificationConfigurations() != null) {
          for (AutoScalingNotificationConfiguration notificationConfiguration: newAction.properties.getNotificationConfigurations()) {
            oldNotificationConfigurations.put(notificationConfiguration.getTopicARN(), Sets.newHashSet(notificationConfiguration.getNotificationTypes()));
          }
        }

        // put all the new ones that are different from the existing ones.
        for (String topicArn: newNotificationConfigurations.keySet()) {
          if (!existingNotificationConfigurations.containsKey(topicArn) || !existingNotificationConfigurations.get(topicArn).equals(newNotificationConfigurations.get(topicArn))) {
            PutNotificationConfigurationType putNotificationConfigurationType = MessageHelper.createMessage(PutNotificationConfigurationType.class, newAction.info.getEffectiveUserId());
            putNotificationConfigurationType.setAutoScalingGroupName(newAction.info.getPhysicalResourceId());
            putNotificationConfigurationType.setTopicARN(topicArn);
            AutoScalingNotificationTypes autoScalingNotificationTypes = new AutoScalingNotificationTypes();
            ArrayList<String> member = Lists.newArrayList(newNotificationConfigurations.get(topicArn));
            autoScalingNotificationTypes.setMember(member);
            putNotificationConfigurationType.setNotificationTypes(autoScalingNotificationTypes);
            AsyncRequests.<PutNotificationConfigurationType, PutNotificationConfigurationResponseType>sendSync(configuration, putNotificationConfigurationType);
          }
        }

        // get rid of all the old ones that are existing and not new
        for (String topicArn: oldNotificationConfigurations.keySet()) {
          if (existingNotificationConfigurations.containsKey(topicArn) && !newNotificationConfigurations.containsKey(topicArn)) {
            DeleteNotificationConfigurationType deleteNotificationConfigurationType = MessageHelper.createMessage(DeleteNotificationConfigurationType.class, newAction.info.getEffectiveUserId());
            deleteNotificationConfigurationType.setAutoScalingGroupName(newAction.info.getPhysicalResourceId());
            deleteNotificationConfigurationType.setTopicARN(topicArn);
            AsyncRequests.sendSync(configuration, deleteNotificationConfigurationType);
          }
        }
        return newAction;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }

  }

  private static Tags convertAutoScalingTagsToCreateOrUpdateTags(String physicalResourceId, Collection<AutoScalingTag> autoScalingTags) {
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



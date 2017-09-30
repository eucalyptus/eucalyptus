/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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


import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupNames;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingNotificationTypes;
import com.eucalyptus.autoscaling.common.msgs.AvailabilityZones;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsType;
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DeleteNotificationConfigurationType;
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
import com.eucalyptus.autoscaling.common.msgs.ProcessNames;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.ResumeProcessesType;
import com.eucalyptus.autoscaling.common.msgs.SuspendProcessesType;
import com.eucalyptus.autoscaling.common.msgs.SuspendedProcessType;
import com.eucalyptus.autoscaling.common.msgs.TagDescription;
import com.eucalyptus.autoscaling.common.msgs.TagType;
import com.eucalyptus.autoscaling.common.msgs.Tags;
import com.eucalyptus.autoscaling.common.msgs.TerminateInstanceInAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.TerminationPolicies;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.Values;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.RollingUpdateStateEntity.ObsoleteInstance.TerminationState;
import com.eucalyptus.cloudformation.entity.RollingUpdateStateEntity.ObsoleteInstance;
import com.eucalyptus.cloudformation.entity.RollingUpdateStateEntity;
import com.eucalyptus.cloudformation.entity.RollingUpdateStateEntityManager;
import com.eucalyptus.cloudformation.entity.SignalEntity;
import com.eucalyptus.cloudformation.entity.SignalEntityManager;
import com.eucalyptus.cloudformation.entity.StackEventEntityManager;
import com.eucalyptus.cloudformation.entity.Status;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingAutoScalingGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSAutoScalingAutoScalingGroupProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingNotificationConfiguration;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingTag;
import com.eucalyptus.cloudformation.template.CreationPolicy;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.template.UpdatePolicy;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.NotAResourceFailureException;
import com.eucalyptus.cloudformation.workflow.ResourceFailureException;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
//  @Override
//  public Integer getMultiUpdateStepTimeoutPollMaximumInterval() {
//    return (int) TimeUnit.SECONDS.toSeconds(10); // this is to allow rolling update to complete sooner
//  }

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an autoscaling group to have zero instances during delete")
  public static volatile Integer AUTOSCALING_GROUP_ZERO_INSTANCES_MAX_DELETE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an autoscaling group to be deleted after deletion)")
  public static volatile Integer AUTOSCALING_GROUP_DELETED_MAX_DELETE_RETRY_SECS = 300;

  private static int MAX_SIGNAL_TIMEOUT = (int) TimeUnit.HOURS.toSeconds(12);

  AWSAutoScalingAutoScalingGroupProperties properties = new AWSAutoScalingAutoScalingGroupProperties();
  AWSAutoScalingAutoScalingGroupResourceInfo info = new AWSAutoScalingAutoScalingGroupResourceInfo();

  public AWSAutoScalingAutoScalingGroupResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  private static Map<String, String> getSubnetMap(Collection<String> instanceIds, String effectiveUserId) throws Exception {
    ServiceConfiguration configuration = Topology.lookup(Compute.class);
    DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, effectiveUserId);
    describeInstancesType.getFilterSet( ).add( CloudFilters.filter("instance-id", instanceIds));
    DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync(configuration, describeInstancesType);
    Map<String, String> subnetMap = Maps.newHashMap();
    if (describeInstancesResponseType.getReservationSet() != null) {
      for (ReservationInfoType reservationInfoType: describeInstancesResponseType.getReservationSet()) {
        if (reservationInfoType.getInstancesSet() != null) {
          for (RunningInstancesItemType runningInstancesItemType: reservationInfoType.getInstancesSet()) {
            subnetMap.put(runningInstancesItemType.getInstanceId(), runningInstancesItemType.getSubnetId());
          }
        }
      }
    }
    return subnetMap;
  }

  private static boolean doesInstanceNeedReplacement(Instance instance, Map<String, String> subnetMap, AutoScalingGroupType autoScalingGroupType) {
    if (!instance.getLaunchConfigurationName().equals(autoScalingGroupType.getLaunchConfigurationName())) {
      return true;
    }
    Splitter commaSplitterAndTrim = Splitter.on(',').omitEmptyStrings().trimResults();
    // check subnet (VPCZoneIdentifier)
    if (autoScalingGroupType.getVpcZoneIdentifier() != null && !autoScalingGroupType.getVpcZoneIdentifier().isEmpty()) {
      // get subnet from instance
      if (!commaSplitterAndTrim.splitToList(autoScalingGroupType.getVpcZoneIdentifier()).contains(subnetMap.get(instance.getInstanceId()))) {
        return true;
      }
    }
    return false;
  }

  private static boolean doesInstanceNeedReplacement(Instance instance, Map<String, String> subnetMap, AWSAutoScalingAutoScalingGroupResourceAction newAction) {
    if (!instance.getLaunchConfigurationName().equals(newAction.properties.getLaunchConfigurationName())) {
      return true;
    }
    if (newAction.properties.getVpcZoneIdentifier() != null && !newAction.properties.getVpcZoneIdentifier().isEmpty()) {
      // get subnet from instance
      if (!newAction.properties.getVpcZoneIdentifier().contains(subnetMap.get(instance.getInstanceId()))) {
        return true;
      }
    }
    return false;
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
        if (action.properties.getAvailabilityZones() != null && !action.properties.getAvailabilityZones().isEmpty()) {
          createAutoScalingGroupType.setAvailabilityZones(new AvailabilityZones(action.properties.getAvailabilityZones()));
        }
        createAutoScalingGroupType.setDefaultCooldown(action.properties.getCooldown());
        createAutoScalingGroupType.setDesiredCapacity(0);
        createAutoScalingGroupType.setHealthCheckGracePeriod(action.properties.getHealthCheckGracePeriod());
        createAutoScalingGroupType.setHealthCheckType(action.properties.getHealthCheckType());
        createAutoScalingGroupType.setLaunchConfigurationName(action.properties.getLaunchConfigurationName());
        if (action.properties.getLoadBalancerNames() != null) {
          createAutoScalingGroupType.setLoadBalancerNames(new LoadBalancerNames(action.properties.getLoadBalancerNames()));
        }
        createAutoScalingGroupType.setMaxSize(0);
        createAutoScalingGroupType.setMinSize(0);
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
        String effectiveAdminUserId = action.info.getAccountId( );
        CreateOrUpdateTagsType createSystemTagsType = MessageHelper.createPrivilegedMessage(CreateOrUpdateTagsType.class, effectiveAdminUserId);
        createSystemTagsType.setTags(convertAutoScalingTagsToCreateOrUpdateTags(action.info.getPhysicalResourceId(), TagHelper.getAutoScalingSystemTags(action.info, action.getStackEntity())));
        sendSyncWithRetryOnScalingEvent(configuration, createSystemTagsType);
        // Create non-system tags as regular user
        List<AutoScalingTag> tags = TagHelper.getAutoScalingStackTags(action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedAutoScalingTemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags());
        }
        if (!tags.isEmpty()) {
          CreateOrUpdateTagsType createOrUpdateTagsType = MessageHelper.createMessage(CreateOrUpdateTagsType.class, action.info.getEffectiveUserId());
          createOrUpdateTagsType.setTags(convertAutoScalingTagsToCreateOrUpdateTags(action.info.getPhysicalResourceId(), tags));
          sendSyncWithRetryOnScalingEvent(configuration, createOrUpdateTagsType);
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
            sendSyncWithRetryOnScalingEvent(configuration, putNotificationConfigurationType);
          }
        }
        return action;
      }
    },
    UPDATE_CAPACITY {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSAutoScalingAutoScalingGroupResourceAction action =
            (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        final ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        final UpdateAutoScalingGroupType updateAutoScalingGroupType =
            MessageHelper.createMessage(UpdateAutoScalingGroupType.class, action.info.getEffectiveUserId());
        updateAutoScalingGroupType.setAutoScalingGroupName(action.info.getPhysicalResourceId());
        updateAutoScalingGroupType.setDesiredCapacity(action.properties.getDesiredCapacity());
        updateAutoScalingGroupType.setMaxSize(action.properties.getMaxSize());
        updateAutoScalingGroupType.setMinSize(action.properties.getMinSize());
        sendSyncWithRetryOnScalingEvent(configuration, updateAutoScalingGroupType);
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
        return (int) MAX_SIGNAL_TIMEOUT;
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
        sendSyncWithRetryOnScalingEvent(configuration, updateAutoScalingGroupType);
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
        sendSyncWithRetryOnScalingEvent(configuration, deleteAutoScalingGroupType);
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
      if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return true;
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

  private static final Logger LOG = Logger.getLogger(AWSAutoScalingAutoScalingGroupResourceAction.class);
  private enum UpdateNoInterruptionSteps implements UpdateStep {
    CHECK_ROLLING_UPDATE_AND_SUSPEND {
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction oldAction = (AWSAutoScalingAutoScalingGroupResourceAction) oldResourceAction;
        AWSAutoScalingAutoScalingGroupResourceAction newAction = (AWSAutoScalingAutoScalingGroupResourceAction) newResourceAction;
        // kill all signals
        SignalEntityManager.deleteSignals(newAction.getStackEntity().getStackId(), newAction.info.getAccountId(), newAction.info.getLogicalResourceId());
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (newAction.info.getUpdatePolicyJson() == null) return newAction;
        UpdatePolicy updatePolicy = UpdatePolicy.parse(newAction.info.getUpdatePolicyJson());
        if (updatePolicy.getAutoScalingRollingUpdate() == null) return newAction;

        RollingUpdateStateEntityManager.deleteRollingUpdateStateEntity(newAction.info.getAccountId(),
          newAction.getStackEntity().getStackId(), newAction.info.getLogicalResourceId());
        RollingUpdateStateEntity rollingUpdateStateEntity = RollingUpdateStateEntityManager.createRollingUpdateStateEntity(newAction.info.getAccountId(),
          newAction.getStackEntity().getStackId(), newAction.info.getLogicalResourceId());


        AutoScalingGroupType autoScalingGroupType = getExistingUniqueAutoscalingGroupType(configuration, newAction);
        // check suspended processes even if we don't terminate instances (AWS does this)

        Set<String> badSuspendedProcesses = Sets.newHashSet();
        Set<String> possibleBadSuspendedProcesses = ImmutableSet.of("Launch", "Terminate");
        Set<String> alreadySuspendedProcesses = Sets.newHashSet();
        if (autoScalingGroupType.getSuspendedProcesses() != null && autoScalingGroupType.getSuspendedProcesses().getMember() != null) {
          for (SuspendedProcessType suspendedProcessType: autoScalingGroupType.getSuspendedProcesses().getMember()) {
            alreadySuspendedProcesses.add(suspendedProcessType.getProcessName());
          }
        }

        for (String possibleBadSuspendedProcess: possibleBadSuspendedProcesses) {
          if (alreadySuspendedProcesses.contains(possibleBadSuspendedProcess) ||
            updatePolicy.getAutoScalingRollingUpdate().getSuspendProcesses().contains(possibleBadSuspendedProcess)) {
            badSuspendedProcesses.add(possibleBadSuspendedProcess);
          }
        }
        if (!badSuspendedProcesses.isEmpty()) {
          throw new ResourceFailureException("Autoscaling rolling updates cannot be performed because the " +
            badSuspendedProcesses + " process(es) have been suspended; please resume these processes and then retry the update.");
        }

        // check to see if rolling updates apply,.(if we have one 'bad' one say)
        Set<String> instanceIds = Sets.newHashSet();
        for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
          instanceIds.add(instance.getInstanceId());
        }
        Map<String, String> subnetMap = getSubnetMap(instanceIds, newAction.info.getEffectiveUserId());
        for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
          if (doesInstanceNeedReplacement(instance, subnetMap, newAction)) {
            rollingUpdateStateEntity.setNeedsRollbackUpdate(true);
            RollingUpdateStateEntityManager.updateRollingUpdateStateEntity(rollingUpdateStateEntity);
            break;
          }
        }

        if (!Boolean.TRUE.equals(rollingUpdateStateEntity.getNeedsRollbackUpdate())) return newAction;

        // otherwise start by suspending processes
        rollingUpdateStateEntity.setAlreadySuspendedProcessNames(Joiner.on(',').join(alreadySuspendedProcesses));
        if (!updatePolicy.getAutoScalingRollingUpdate().getSuspendProcesses().isEmpty()) {
          SuspendProcessesType suspendProcessesType = MessageHelper.createMessage(SuspendProcessesType.class, newAction.info.getEffectiveUserId());
          suspendProcessesType.setAutoScalingGroupName(newAction.info.getPhysicalResourceId());
          ProcessNames processNames = new ProcessNames();
          for (String suspendProcess : updatePolicy.getAutoScalingRollingUpdate().getSuspendProcesses()) {
            if (!alreadySuspendedProcesses.contains(suspendProcess)) {
              processNames.getMember().add(suspendProcess);
            }
          }
          if (!processNames.getMember().isEmpty()) {
            sendSyncWithRetryOnScalingEvent(configuration, suspendProcessesType);
          }
        }

        RollingUpdateStateEntityManager.updateRollingUpdateStateEntity(rollingUpdateStateEntity);
        return newAction;
      }
    },
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
        if (newAction.properties.getAvailabilityZones() != null && !newAction.properties.getAvailabilityZones().isEmpty()) {
          updateAutoScalingGroupType.setAvailabilityZones(new AvailabilityZones(newAction.properties.getAvailabilityZones()));
        }
        if (newAction.info.getUpdatePolicyJson() != null) {
          UpdatePolicy updatePolicy = UpdatePolicy.parse(newAction.info.getUpdatePolicyJson());
          if (updatePolicy != null && updatePolicy.getAutoScalingRollingUpdate() != null &&
            updatePolicy.getAutoScalingRollingUpdate().getMinInstancesInService() >= newAction.properties.getMaxSize() ) {
            throw new ValidationErrorException("MinInstancesInService must be less than the autoscaling group's MaxSize");
          }
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
        sendSyncWithRetryOnScalingEvent(configuration, updateAutoScalingGroupType);
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
        if (describeTagsResponseType != null && describeTagsResponseType.getDescribeTagsResult() != null &&
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
          sendSyncWithRetryOnScalingEvent(configuration, createOrUpdateTagsType);
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
          sendSyncWithRetryOnScalingEvent(configuration, deleteTagsType);
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
            sendSyncWithRetryOnScalingEvent(configuration, putNotificationConfigurationType);
          }
        }

        // get rid of all the old ones that are existing and not new
        for (String topicArn: oldNotificationConfigurations.keySet()) {
          if (existingNotificationConfigurations.containsKey(topicArn) && !newNotificationConfigurations.containsKey(topicArn)) {
            DeleteNotificationConfigurationType deleteNotificationConfigurationType = MessageHelper.createMessage(DeleteNotificationConfigurationType.class, newAction.info.getEffectiveUserId());
            deleteNotificationConfigurationType.setAutoScalingGroupName(newAction.info.getPhysicalResourceId());
            deleteNotificationConfigurationType.setTopicARN(topicArn);
            sendSyncWithRetryOnScalingEvent(configuration, deleteNotificationConfigurationType);
          }
        }
        return newAction;
      }
    },
    ROLLING_UPDATE_EVENT_LOOP {

      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction oldAction = (AWSAutoScalingAutoScalingGroupResourceAction) oldResourceAction;
        AWSAutoScalingAutoScalingGroupResourceAction newAction = (AWSAutoScalingAutoScalingGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        if (newAction.info.getUpdatePolicyJson() == null) return newAction;
        UpdatePolicy updatePolicy = UpdatePolicy.parse(newAction.info.getUpdatePolicyJson());
        if (updatePolicy.getAutoScalingRollingUpdate() == null) return newAction;
        RollingUpdateStateEntity rollingUpdateStateEntity = RollingUpdateStateEntityManager.getRollingUpdateStateEntity(newAction.info.getAccountId(), newAction.getStackEntity().getStackId(), newAction.info.getLogicalResourceId());
        if (!Boolean.TRUE.equals(rollingUpdateStateEntity.getNeedsRollbackUpdate())) return newAction;
        while (rollingUpdateStateEntity.getState() != UpdateRollbackInfo.State.DONE) {
          LOG.info("Evaluating loop action on state " + rollingUpdateStateEntity.getState());
          rollingUpdateStateEntity = (rollingUpdateStateEntity.getState().apply(newAction, configuration, updatePolicy, rollingUpdateStateEntity));
          rollingUpdateStateEntity = RollingUpdateStateEntityManager.updateRollingUpdateStateEntity(rollingUpdateStateEntity);
        }
        return newAction;
      }

      @Nullable
      @Override
      public Integer getTimeout() {
        return MAX_SIGNAL_TIMEOUT;
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


  public static <A extends BaseMessage, B extends BaseMessage> B sendSyncWithRetryOnScalingEvent( ServiceConfiguration config, final A msg ) throws Exception {
    // TODO: library.  configurable.
    int numTriesLeft = 30;
    long sleepTimeMS = 10000L;
    while (true) {
      try {
        return AsyncRequests.sendSync(config, msg);
      } catch (final Exception e) {
        final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
        if (error.isPresent()) switch (Strings.nullToEmpty(error.get().getCode())) {
          case "ScalingActivityInProgress":
            if (--numTriesLeft <= 0) throw e;
            Thread.sleep(sleepTimeMS);
            break;
          default:
            throw e;
        }
        else {
          throw e;
        }
      }
    }
  }

  private static AutoScalingGroupType getExistingUniqueAutoscalingGroupType(ServiceConfiguration configuration, AWSAutoScalingAutoScalingGroupResourceAction action) throws Exception {
    DescribeAutoScalingGroupsType describeAutoScalingGroupsType = MessageHelper.createMessage(DescribeAutoScalingGroupsType.class, action.info.getEffectiveUserId());
    AutoScalingGroupNames autoScalingGroupNames = new AutoScalingGroupNames();
    autoScalingGroupNames.getMember().add(action.info.getPhysicalResourceId());
    describeAutoScalingGroupsType.setAutoScalingGroupNames(autoScalingGroupNames);
    DescribeAutoScalingGroupsResponseType describeAutoScalingGroupResponseType = AsyncRequests.sendSync(configuration, describeAutoScalingGroupsType);
    if (describeAutoScalingGroupResponseType == null || describeAutoScalingGroupResponseType.getDescribeAutoScalingGroupsResult() == null ||
      describeAutoScalingGroupResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups() == null ||
      describeAutoScalingGroupResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember() == null ||
      describeAutoScalingGroupResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().size() != 1) {
      throw new ValidationErrorException(action.info.getPhysicalResourceId() + " refers to either a non-existant or non-unique autoscaling group");
    }
    return describeAutoScalingGroupResponseType.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().get(0);
  }

  public static class UpdateRollbackInfo {
    public enum State {
      NOT_STARTED {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {

          // Find out how many signals we expect.  This is the new desired capacity - # of running, "good" instances
          AutoScalingGroupType autoScalingGroupType = getExistingUniqueAutoscalingGroupType(configuration, newAction);

          int numExpectedSignals = autoScalingGroupType.getDesiredCapacity();
          Set<String> allRunningInstanceIds = Sets.newHashSet();
          if (autoScalingGroupType.getInstances() != null && autoScalingGroupType.getInstances().getMember() != null) {
            Set<String> instanceIds = Sets.newHashSet();
            for (Instance instance : autoScalingGroupType.getInstances().getMember()) {
              instanceIds.add(instance.getInstanceId());
            }
            Map<String, String> subnetMap = getSubnetMap(instanceIds, newAction.info.getEffectiveUserId());
            for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
              if (instance.getLifecycleState().equals("InService")) {
                allRunningInstanceIds.add(instance.getInstanceId());
                if (!doesInstanceNeedReplacement(instance, subnetMap, autoScalingGroupType)) {
                  numExpectedSignals--;
                }
              }
            }
          }
          rollingUpdateStateEntity.setPreviousRunningInstanceIds(Joiner.on(',').join(allRunningInstanceIds));
          rollingUpdateStateEntity.setNumExpectedTotalSignals(numExpectedSignals);
          rollingUpdateStateEntity.setState(State.FIRST_WAIT_TO_SETTLE);
          return rollingUpdateStateEntity;
        }
      },
      FIRST_WAIT_TO_SETTLE {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {
          return commonWaitToSettleLogic(newAction, configuration, updatePolicy, rollingUpdateStateEntity, State.FIRST_WAIT_FOR_SIGNALS, State.DETERMINE_TERMINATE_INFO_AND_RESIZE);
        }
      },
      FIRST_WAIT_FOR_SIGNALS {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {
          return commonWaitForSignalsLogic(newAction, configuration, updatePolicy, rollingUpdateStateEntity, State.DETERMINE_TERMINATE_INFO_AND_RESIZE);
        }
      },
      DETERMINE_TERMINATE_INFO_AND_RESIZE {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {
          AutoScalingGroupType autoScalingGroupType = getExistingUniqueAutoscalingGroupType(configuration, newAction);

          // Record info from group before we change size
          rollingUpdateStateEntity.setMinSize(autoScalingGroupType.getMinSize());
          rollingUpdateStateEntity.setMaxSize(autoScalingGroupType.getMaxSize());
          rollingUpdateStateEntity.setDesiredCapacity(autoScalingGroupType.getDesiredCapacity());

          // figure out which instances are 'bad'
          Collection<ObsoleteInstance> obsoleteInstances = Lists.newArrayList();
          if (autoScalingGroupType.getInstances() != null && autoScalingGroupType.getInstances().getMember() != null) {
            Set<String> instanceIds = Sets.newHashSet();
            for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
              instanceIds.add(instance.getInstanceId());
            }
            Map<String, String> subnetMap = getSubnetMap(instanceIds, newAction.info.getEffectiveUserId());
            for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
              if (doesInstanceNeedReplacement(instance, subnetMap, autoScalingGroupType)) {
                obsoleteInstances.add(new ObsoleteInstance(instance.getInstanceId(), TerminationState.RUNNING));
              }
            }
            rollingUpdateStateEntity.setObsoleteInstancesJson(RollingUpdateStateEntity.ObsoleteInstance.obsoleteInstancesToJson(obsoleteInstances));
            Set<String> previousRunningInstanceIds = Sets.newHashSet();
            for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
              if (instance.getLifecycleState().equals("InService")) {
                previousRunningInstanceIds.add(instance.getInstanceId());
              }
            }
            rollingUpdateStateEntity.setPreviousRunningInstanceIds(Joiner.on(',').join(previousRunningInstanceIds));

          } else {
            rollingUpdateStateEntity.setObsoleteInstancesJson(RollingUpdateStateEntity.ObsoleteInstance.obsoleteInstancesToJson(obsoleteInstances));
            rollingUpdateStateEntity.setPreviousRunningInstanceIds("");
          }

          if (obsoleteInstances.size() == 0) {
            rollingUpdateStateEntity.setState(State.RESUME_PROCESSES);
            return rollingUpdateStateEntity;
          }

          // We try to make the batch size as big as we can, but we have some restrictions.
          // 1) It can't be bigger than the number of obsolete instances
          // 2) It can't be bigger than the max batch size.
          // 3) It would seem as though it couldn't be bigger than desiredCapacity - minRunningInstances, but it can if we increase the size
          //    of the group temporarily.  In that case the value it can't be bigger than is desiredCapacity - minRunningInstances + number of non obsolete instances

          int batchSize = Math.min(
            Math.min(obsoleteInstances.size(), updatePolicy.getAutoScalingRollingUpdate().getMaxBatchSize()),
            rollingUpdateStateEntity.getDesiredCapacity() - updatePolicy.getAutoScalingRollingUpdate().getMinInstancesInService()
              + (rollingUpdateStateEntity.getDesiredCapacity() - obsoleteInstances.size()));

          // Once we set the batch size, we need to make sure the group size is correct.  Either 'desiredCapacity' or (if we need to increase the size, batchSize + minRunningInstances)

          int tempDesiredCapacity = Math.max(batchSize + updatePolicy.getAutoScalingRollingUpdate().getMinInstancesInService(), rollingUpdateStateEntity.getDesiredCapacity());
          rollingUpdateStateEntity.setBatchSize(batchSize);
          rollingUpdateStateEntity.setTempDesiredCapacity(tempDesiredCapacity);

          StringBuilder message = new StringBuilder("Rollling update initiated.  " +
            "Terminating " + obsoleteInstances.size() + " instance(s) in batches of " + batchSize);
          if (updatePolicy.getAutoScalingRollingUpdate().getMinInstancesInService() > 0) {
            message.append(", while keeping at least " + updatePolicy.getAutoScalingRollingUpdate().getMinInstancesInService() + " in service.");
          } else {
            message.append(".");
          }

          if (updatePolicy.getAutoScalingRollingUpdate().isWaitOnResourceSignals()) {
            message.append("  Waiting on resource signals with a timeout of " + updatePolicy.getAutoScalingRollingUpdate().getPauseTime() + " when new instances are added to the autoscaling group.");
          } else if (Duration.parse(updatePolicy.getAutoScalingRollingUpdate().getPauseTime()).getSeconds() > 0) {
            message.append("  Pausing for " + updatePolicy.getAutoScalingRollingUpdate().getPauseTime() + " when new instances are added to the autoscaling group.");
          }
          addStackEventForRollingUpdate(newAction, message.toString());

          // now set the new size of the group (to allow adds or deletes)
          UpdateAutoScalingGroupType updateAutoScalingGroupType = MessageHelper.createMessage(UpdateAutoScalingGroupType.class, newAction.info.getEffectiveUserId());
          updateAutoScalingGroupType.setAutoScalingGroupName(newAction.info.getPhysicalResourceId());
          updateAutoScalingGroupType.setDesiredCapacity(tempDesiredCapacity);
          updateAutoScalingGroupType.setMinSize(tempDesiredCapacity);
          sendSyncWithRetryOnScalingEvent(configuration, updateAutoScalingGroupType);
          addStackEventForRollingUpdate(newAction, "Temporarily setting autoscaling group MinSize and DesiredCapacity to " + tempDesiredCapacity + ".");

          // This size change may result in new instances needing to be spun up
          rollingUpdateStateEntity.setState(State.WAIT_TO_SETTLE);
          return rollingUpdateStateEntity;
        }
      },
      WAIT_TO_SETTLE {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {
          return commonWaitToSettleLogic(newAction, configuration, updatePolicy, rollingUpdateStateEntity, State.WAIT_FOR_SIGNALS, State.TRY_TERMINATE);
        }
      },
      WAIT_FOR_SIGNALS {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {
          return commonWaitForSignalsLogic(newAction, configuration, updatePolicy, rollingUpdateStateEntity, State.TRY_TERMINATE);
        }
      },
      TRY_TERMINATE {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {
          Collection<ObsoleteInstance> obsoleteInstances = ObsoleteInstance.jsonToObsoleteInstances(rollingUpdateStateEntity.getObsoleteInstancesJson());
          Collection<String> obsoleteButStillRunningInstanceIds = Lists.newArrayList();
          for (ObsoleteInstance obsoleteInstance: obsoleteInstances) {
            if (obsoleteInstance.getLastKnownState() == TerminationState.RUNNING) {
              obsoleteButStillRunningInstanceIds.add(obsoleteInstance.getInstanceId());
            }
          }
          AutoScalingGroupType autoScalingGroupType = getExistingUniqueAutoscalingGroupType(configuration, newAction);
          if (obsoleteButStillRunningInstanceIds.isEmpty()) {
            rollingUpdateStateEntity.setState(State.RESUME_PROCESSES);
            return rollingUpdateStateEntity;
          } else {
            boolean isLastRound = (obsoleteButStillRunningInstanceIds.size() <= rollingUpdateStateEntity.getBatchSize());
            List<String> terminatingInstanceIds = Lists.newArrayList();
            for (ObsoleteInstance obsoleteInstance: obsoleteInstances) {
              if (obsoleteInstance.getLastKnownState() == TerminationState.RUNNING) {
                obsoleteInstance.setLastKnownState(TerminationState.TERMINATING);
                terminatingInstanceIds.add(obsoleteInstance.getInstanceId());
                if (terminatingInstanceIds.size() == rollingUpdateStateEntity.getBatchSize()) break;
              }
            }
            rollingUpdateStateEntity.setObsoleteInstancesJson(ObsoleteInstance.obsoleteInstancesToJson(obsoleteInstances));

            Set<String> allRunningInstanceIds = Sets.newHashSet();
            if (autoScalingGroupType.getInstances() != null && autoScalingGroupType.getInstances().getMember() != null) {
              for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
                if (instance.getLifecycleState().equals("InService")) {
                  allRunningInstanceIds.add(instance.getInstanceId());
                }
              }
            }
            // how many will we replace it with?  If we are not in the last round, we replace with as many as we terminate.
            // If we are in the last round we have (# running instances - # terminating instances) " good instances and we want
            // to add as many as we need to to get to the desired capacity... We want to remove X and leave original desired capacity
            // if we already have 'too many' good ones, we don't have any
            int replacementNum;
            if (!isLastRound) {
              replacementNum = terminatingInstanceIds.size();
            } else {
              int numGood = allRunningInstanceIds.size() - terminatingInstanceIds.size();
              replacementNum = numGood > rollingUpdateStateEntity.getDesiredCapacity() ? 0 : rollingUpdateStateEntity.getDesiredCapacity() - numGood;
            }
            addStackEventForRollingUpdate(newAction, "Terminating instance(s) " + terminatingInstanceIds + "; replacing with " + replacementNum + " new instance(s).");
            for (String terminatingInstanceId: terminatingInstanceIds) {
              TerminateInstanceInAutoScalingGroupType terminateInstanceInAutoScalingGroupType = MessageHelper.createMessage(TerminateInstanceInAutoScalingGroupType.class, newAction.info.getEffectiveUserId());
              terminateInstanceInAutoScalingGroupType.setInstanceId(terminatingInstanceId);
              terminateInstanceInAutoScalingGroupType.setShouldDecrementDesiredCapacity(false);
              try {
                sendSyncWithRetryOnScalingEvent(configuration, terminateInstanceInAutoScalingGroupType);
              } catch (final Exception e) {
                final Optional<AsyncExceptions.AsyncWebServiceError> error = AsyncExceptions.asWebServiceError(e);
                if (error.isPresent()) switch (Strings.nullToEmpty(error.get().getCode())) {
                  case "ValidationError":
                    continue; // already terminated
                }
                throw e;
              }
            }
            if (isLastRound) {
              // now set back to original size
              // now set the new size of the group (to allow adds or deletes)
              UpdateAutoScalingGroupType updateAutoScalingGroupType = MessageHelper.createMessage(UpdateAutoScalingGroupType.class, newAction.info.getEffectiveUserId());
              updateAutoScalingGroupType.setAutoScalingGroupName(newAction.info.getPhysicalResourceId());
              updateAutoScalingGroupType.setDesiredCapacity(rollingUpdateStateEntity.getDesiredCapacity());
              updateAutoScalingGroupType.setMinSize(rollingUpdateStateEntity.getMinSize());
              sendSyncWithRetryOnScalingEvent(configuration, updateAutoScalingGroupType);
            }
            rollingUpdateStateEntity.setState(State.WAIT_FOR_TERMINATE);
            return rollingUpdateStateEntity;
          }
        }
      },
      WAIT_FOR_TERMINATE {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {
          Collection<ObsoleteInstance> obsoleteInstances = ObsoleteInstance.jsonToObsoleteInstances(rollingUpdateStateEntity.getObsoleteInstancesJson());
          Set<String> terminatingInstanceIds = Sets.newHashSet();
          for (ObsoleteInstance obsoleteInstance: obsoleteInstances) {
            if (obsoleteInstance.getLastKnownState() == TerminationState.TERMINATING) {
              terminatingInstanceIds.add(obsoleteInstance.getInstanceId());
            }
          }
          if (!isAllTerminated(terminatingInstanceIds, newAction.info.getEffectiveUserId())) {
            throw new NotAResourceFailureException("Still waiting on terminating instances");
          } else {
            // set all terminating instances to terminated
            int numTerminatedInstances = 0;
            for (ObsoleteInstance obsoleteInstance: obsoleteInstances) {
              // move to terminated if was terminating
              if (obsoleteInstance.getLastKnownState() == TerminationState.TERMINATING) {
                obsoleteInstance.setLastKnownState(TerminationState.TERMINATED);
              }
              if (obsoleteInstance.getLastKnownState() == TerminationState.TERMINATED) {
                numTerminatedInstances++;
              }
            }
            int progress = obsoleteInstances.size() == 0 ? 100 : 100 * numTerminatedInstances / obsoleteInstances.size();
            addStackEventForRollingUpdate(newAction, "Successfully terminated instance(s) " + terminatingInstanceIds + " (Progress " + progress + "%).");
            rollingUpdateStateEntity.setObsoleteInstancesJson(ObsoleteInstance.obsoleteInstancesToJson(obsoleteInstances));
            rollingUpdateStateEntity.setState(State.WAIT_TO_SETTLE);
            return rollingUpdateStateEntity;
          }
        }
      },
      RESUME_PROCESSES {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {
          Set<String> alreadySuspendedProcesses = Sets.newHashSet(Splitter.on(',').omitEmptyStrings().trimResults().split(rollingUpdateStateEntity.getAlreadySuspendedProcessNames()));
          if (!updatePolicy.getAutoScalingRollingUpdate().getSuspendProcesses().isEmpty()) {
            ResumeProcessesType resumeProcessesType = MessageHelper.createMessage(ResumeProcessesType.class, newAction.info.getEffectiveUserId());
            resumeProcessesType.setAutoScalingGroupName(newAction.info.getPhysicalResourceId());
            ProcessNames processNames = new ProcessNames();
            for (String suspendProcess : updatePolicy.getAutoScalingRollingUpdate().getSuspendProcesses()) {
              if (!alreadySuspendedProcesses.contains(suspendProcess)) {
                processNames.getMember().add(suspendProcess);
              }
            }
            if (!processNames.getMember().isEmpty()) {
              sendSyncWithRetryOnScalingEvent(configuration, resumeProcessesType);
            }
          }
          rollingUpdateStateEntity.setState(State.DONE);
          return rollingUpdateStateEntity;
        }
      },
      DONE {
        @Override
        public RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception {
          rollingUpdateStateEntity.setState(State.DONE);
          return rollingUpdateStateEntity;
        }
      };
      abstract RollingUpdateStateEntity apply(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity) throws Exception;
    }

    private static RollingUpdateStateEntity commonWaitToSettleLogic(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity, State nextSignalState, State nextNonSignalState) throws Exception {
      if (!settled(configuration, newAction)) {
        throw new NotAResourceFailureException("Autoscaling group is not yet settled, trying again");
      }
      Set<String> previousInstanceIds = Sets.newHashSet(Splitter.on(',').omitEmptyStrings().trimResults().split(rollingUpdateStateEntity.getPreviousRunningInstanceIds()));
      AutoScalingGroupType autoScalingGroupType = getExistingUniqueAutoscalingGroupType(configuration, newAction);
      Set<String> allRunningInstanceIds = Sets.newHashSet();
      if (autoScalingGroupType.getInstances() != null && autoScalingGroupType.getInstances().getMember() != null) {
        for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
          if (instance.getLifecycleState().equals("InService")) {
            allRunningInstanceIds.add(instance.getInstanceId());
          }
        }
      }
      // update previousInstanceIds
      rollingUpdateStateEntity.setPreviousRunningInstanceIds(Joiner.on(',').join(allRunningInstanceIds));
      Set<String> newInstanceIds = Sets.difference(allRunningInstanceIds, previousInstanceIds);
      if (!newInstanceIds.isEmpty()) {
        if (updatePolicy.getAutoScalingRollingUpdate().isWaitOnResourceSignals()) {
          addStackEventForRollingUpdate(newAction, "New instance(s) added to autoscaling group - Waiting on " + newInstanceIds.size() + " resource signal(s) with a timeout of " + updatePolicy.getAutoScalingRollingUpdate().getPauseTime() + ".");
          rollingUpdateStateEntity.setCurrentBatchInstanceIds(Joiner.on(',').join(newInstanceIds));
          rollingUpdateStateEntity.setSignalCutoffTimestamp(new Date(System.currentTimeMillis() +
            TimeUnit.SECONDS.toMillis(Duration.parse(updatePolicy.getAutoScalingRollingUpdate().getPauseTime()).getSeconds())));
        } else {
          if (Duration.parse(updatePolicy.getAutoScalingRollingUpdate().getPauseTime()).getSeconds() > 0) {
            addStackEventForRollingUpdate(newAction, "New instance(s) added to autoscaling group - Pausing for " + updatePolicy.getAutoScalingRollingUpdate().getPauseTime() + ".");
          }
          rollingUpdateStateEntity.setSignalCutoffTimestamp(new Date(System.currentTimeMillis() +
            TimeUnit.SECONDS.toMillis(Duration.parse(updatePolicy.getAutoScalingRollingUpdate().getPauseTime()).getSeconds())));
        }
        rollingUpdateStateEntity.setState(nextSignalState);
      } else {
        rollingUpdateStateEntity.setState(nextNonSignalState);
      }
      return rollingUpdateStateEntity;
    }

    private static RollingUpdateStateEntity commonWaitForSignalsLogic(AWSAutoScalingAutoScalingGroupResourceAction newAction, ServiceConfiguration configuration, UpdatePolicy updatePolicy, RollingUpdateStateEntity rollingUpdateStateEntity, State nextState) throws Exception {
      if (!updatePolicy.getAutoScalingRollingUpdate().isWaitOnResourceSignals()) {
        if (new Date().before(rollingUpdateStateEntity.getSignalCutoffTimestamp())) {
          throw new NotAResourceFailureException("still pausing");
        } else {
          rollingUpdateStateEntity.setState(nextState);
          return rollingUpdateStateEntity;
        }
      }
      // Otherwise we wait for signals?
      Set<String> currentBatchInstanceIds = Sets.newHashSet(Splitter.on(',').omitEmptyStrings().trimResults().split(rollingUpdateStateEntity.getCurrentBatchInstanceIds()));
      Set<String> unsignaledCurrentBatchInstanceIds = Sets.newHashSet(currentBatchInstanceIds);
      Collection<SignalEntity> signals = SignalEntityManager.getSignals(newAction.getStackEntity().getStackId(), newAction.info.getAccountId(), newAction.info.getLogicalResourceId(),
        newAction.getStackEntity().getStackVersion());
      for (SignalEntity signal : signals) {
        if (unsignaledCurrentBatchInstanceIds.contains(signal.getUniqueId())) {
          if (!signal.getProcessed()) {
            StackEventEntityManager.addSignalStackEvent(signal);
            signal.setProcessed(true);
            SignalEntityManager.updateSignal(signal);
          }
          unsignaledCurrentBatchInstanceIds.remove(signal.getUniqueId());
        } else {
          ;
          // Ignore signals with ids not from the list of instance ids.
        }
      }
      if (!unsignaledCurrentBatchInstanceIds.isEmpty()) {
        if (new Date().before(rollingUpdateStateEntity.getSignalCutoffTimestamp())) {
          throw new NotAResourceFailureException("Still waiting for resource signals");
        } else {
          addStackEventForRollingUpdate(newAction, "Failed to receive " + currentBatchInstanceIds.size() + " signals.  Each resource signal timeout is counted as a FAILURE." );
          for (String instanceId: unsignaledCurrentBatchInstanceIds) {
            // add failure signals (but don't log an event, AWS does not log an event)
            SignalEntity signalEntity = new SignalEntity();
            signalEntity.setStackId(newAction.getStackEntity().getStackId());
            signalEntity.setAccountId(newAction.info.getAccountId());
            signalEntity.setLogicalResourceId(newAction.info.getLogicalResourceId());
            signalEntity.setResourceVersion(newAction.getStackEntity().getStackVersion());
            signalEntity.setStatus(SignalEntity.Status.FAILURE);
            signalEntity.setProcessed(true);
            signalEntity.setUniqueId(instanceId);
            SignalEntityManager.addSignal(signalEntity);
          }
        }
      }
      // check failure and success signals (from processed)
      int numSuccessSignals = 0;
      int numFailureSignals = 0;
      signals = SignalEntityManager.getSignals(newAction.getStackEntity().getStackId(), newAction.info.getAccountId(), newAction.info.getLogicalResourceId(),
        newAction.getStackEntity().getStackVersion());
      for (SignalEntity signal : signals) {
        if (!signal.getProcessed()) continue;
        if (signal.getStatus() == SignalEntity.Status.SUCCESS) {
          numSuccessSignals++;
        } else {
          numFailureSignals++;
        }
      }
      double minNumSuccessSignals = updatePolicy.getAutoScalingRollingUpdate().getMinSuccessfulInstancesPercent() / 100.0 * rollingUpdateStateEntity.getNumExpectedTotalSignals();
      double maxNumFailureSignals = rollingUpdateStateEntity.getNumExpectedTotalSignals() - minNumSuccessSignals;
      if (numFailureSignals > maxNumFailureSignals) {
        throw new ResourceFailureException("Received " + numFailureSignals + " FAILURE signal(s) out of " + rollingUpdateStateEntity.getNumExpectedTotalSignals() + ". Unable to satisfy " + updatePolicy.getAutoScalingRollingUpdate().getMinSuccessfulInstancesPercent() + "% MinSuccessfulInstancesPercent requirement");
      }
      // otherwise continue
      rollingUpdateStateEntity.setState(nextState);
      return rollingUpdateStateEntity;
    }

    private static boolean settled(ServiceConfiguration configuration, AWSAutoScalingAutoScalingGroupResourceAction action) throws Exception {
      AutoScalingGroupType autoScalingGroupType = getExistingUniqueAutoscalingGroupType(configuration, action);
      int numInServiceInstances = 0;
      if (autoScalingGroupType.getInstances() != null && autoScalingGroupType.getInstances().getMember() != null) {
        for (Instance instance: autoScalingGroupType.getInstances().getMember()) {
          if (instance.getLifecycleState().equals("InService")) {
            numInServiceInstances++;
          }
        }
      }
      return (numInServiceInstances == autoScalingGroupType.getDesiredCapacity());
    }

    public static void addStackEventForRollingUpdate(AWSAutoScalingAutoScalingGroupResourceAction action, String message) {
      Date timestamp = new Date();
      String eventId = action.info.getLogicalResourceId() + "-" + Status.UPDATE_IN_PROGRESS + "-" + timestamp.getTime();
      StackEventEntityManager.addStackEvent(action.info.getAccountId(), eventId, action.info.getLogicalResourceId(),
        action.info.getPhysicalResourceId(), action.info.getPropertiesJson(),
        Status.UPDATE_IN_PROGRESS, message, action.info.getType(), action.getStackEntity().getStackId(),
        action.getStackEntity().getStackName(), timestamp);
    }

    private static boolean isAllTerminated(Collection<String> instanceIds, String effectiveUserId) throws Exception {
      Map<String, String> stateMap = Maps.newHashMap();
      ServiceConfiguration configuration = Topology.lookup(Compute.class);
      DescribeInstancesType describeInstancesType = MessageHelper.createMessage(DescribeInstancesType.class, effectiveUserId);
      describeInstancesType.getFilterSet( ).add( CloudFilters.filter("instance-id", instanceIds));
      DescribeInstancesResponseType describeInstancesResponseType = AsyncRequests.sendSync(configuration, describeInstancesType);
      if (describeInstancesResponseType.getReservationSet() != null) {
        for (ReservationInfoType reservationInfoType: describeInstancesResponseType.getReservationSet()) {
          if (reservationInfoType.getInstancesSet() != null) {
            for (RunningInstancesItemType runningInstancesItemType: reservationInfoType.getInstancesSet()) {
              stateMap.put(runningInstancesItemType.getInstanceId(), runningInstancesItemType.getStateName());
            }
          }
        }
      }
      for (String instanceId: instanceIds) {
        if (stateMap.containsKey(instanceId) && !"terminated".equals(stateMap.get(instanceId))) {
          return false;
        }
      }
      return true;
    }


  }
}



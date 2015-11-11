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
import com.eucalyptus.autoscaling.common.msgs.LoadBalancerNames;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationResponseType;
import com.eucalyptus.autoscaling.common.msgs.PutNotificationConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.TagType;
import com.eucalyptus.autoscaling.common.msgs.Tags;
import com.eucalyptus.autoscaling.common.msgs.TerminationPolicies;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingAutoScalingGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSAutoScalingAutoScalingGroupProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingTag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.StackActivityClient;
import com.eucalyptus.cloudformation.workflow.ValidationFailedException;
import com.eucalyptus.cloudformation.workflow.steps.CreateMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.DeleteMultiStepPromise;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepTransform;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.netflix.glisten.WorkflowOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSAutoScalingAutoScalingGroupResourceAction extends ResourceAction {

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an autoscaling group to have zero instances during delete")
  public static volatile Integer AUTOSCALING_GROUP_ZERO_INSTANCES_MAX_DELETE_RETRY_SECS = 300;

  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to wait for an autoscaling group to be deleted after deletion)")
  public static volatile Integer AUTOSCALING_GROUP_DELETED_MAX_DELETE_RETRY_SECS = 300;

  private AWSAutoScalingAutoScalingGroupProperties properties = new AWSAutoScalingAutoScalingGroupProperties();
  private AWSAutoScalingAutoScalingGroupResourceInfo info = new AWSAutoScalingAutoScalingGroupResourceInfo();

  public AWSAutoScalingAutoScalingGroupResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

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
        if ( action.properties.getVpcZoneIdentifier() != null ) {
          createAutoScalingGroupType.setVpcZoneIdentifier(Strings.emptyToNull(Joiner.on( "," ).join(action.properties.getVpcZoneIdentifier())));
        }
        AsyncRequests.<CreateAutoScalingGroupType,CreateAutoScalingGroupResponseType> sendSync(configuration, createAutoScalingGroupType);
        action.info.setPhysicalResourceId(autoScalingGroupName);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSAutoScalingAutoScalingGroupResourceAction action = (AWSAutoScalingAutoScalingGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(AutoScaling.class);
        // Create 'system' tags as admin user
        String effectiveAdminUserId = Accounts.lookupPrincipalByAccountNumber( Accounts.lookupPrincipalByUserId(action.info.getEffectiveUserId()).getAccountNumber( ) ).getUserId();
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
        for (AutoScalingTag autoScalingTag: autoScalingTags) {
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
          AsyncRequests.<PutNotificationConfigurationType,PutNotificationConfigurationResponseType> sendSync(configuration, putNotificationConfigurationType);
        }
        return action;
      }
    };

    // no retries on these steps
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
        throw new ValidationFailedException("Autoscaling group " + action.info.getPhysicalResourceId() + " still has instances");
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
        throw new ValidationFailedException("Autoscaling group " + action.info.getPhysicalResourceId() + " is not yet deleted");
      }

      @Override
      public Integer getTimeout() {
        return AUTOSCALING_GROUP_DELETED_MAX_DELETE_RETRY_SECS;
      }
    };

    private static boolean groupDoesNotExist(ServiceConfiguration configuration, AWSAutoScalingAutoScalingGroupResourceAction action) throws Exception {
      // See if resource was ever populated...
      if (action.info.getPhysicalResourceId() == null) return true;
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

  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), StepTransform.INSTANCE);
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(DeleteSteps.values()), StepTransform.INSTANCE);
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}



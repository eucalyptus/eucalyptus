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
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy;
import com.amazonaws.services.simpleworkflow.flow.interceptors.RetryPolicy;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2SecurityGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2SecurityGroupProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2SecurityGroupRule;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.CreateStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.DeleteStackWorkflowImpl;
import com.eucalyptus.cloudformation.workflow.NotAResourceFailureException;
import com.eucalyptus.cloudformation.workflow.create.CreateStep;
import com.eucalyptus.cloudformation.workflow.create.MultiStepWithRetryCreatePromise;
import com.eucalyptus.cloudformation.workflow.ValidationFailedException;
import com.eucalyptus.cloudformation.workflow.delete.DeleteStep;
import com.eucalyptus.cloudformation.workflow.delete.RetrySingleStepDeletePromise;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupEgressResponseType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupEgressType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.RevokeSecurityGroupEgressResponseType;
import com.eucalyptus.compute.common.RevokeSecurityGroupEgressType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressType;
import com.eucalyptus.compute.common.CreateSecurityGroupResponseType;
import com.eucalyptus.compute.common.CreateSecurityGroupType;
import com.eucalyptus.compute.common.DeleteSecurityGroupResponseType;
import com.eucalyptus.compute.common.DeleteSecurityGroupType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2SecurityGroupResourceAction extends ResourceAction {
  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to retry security group deletes (may fail if instances from autoscaling group)")
  public static volatile Integer SECURITY_GROUP_MAX_DELETE_RETRY_SECS = 300;

  private static final Logger LOG = Logger.getLogger(AWSEC2SecurityGroupResourceAction.class);
  private AWSEC2SecurityGroupProperties properties = new AWSEC2SecurityGroupProperties();
  private AWSEC2SecurityGroupResourceInfo info = new AWSEC2SecurityGroupResourceInfo();

  public AWSEC2SecurityGroupResourceAction() {
    for (CreateSteps createStep: CreateSteps.values()) {
      createSteps.put(createStep.name(), createStep);
    }
    for (DeleteSteps deleteStep: DeleteSteps.values()) {
      deleteSteps.put(deleteStep.name(), deleteStep);
    }

  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2SecurityGroupProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2SecurityGroupResourceInfo) resourceInfo;
  }

  private enum CreateSteps implements CreateStep {
    CREATE_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SecurityGroupResourceAction action = (AWSEC2SecurityGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        CreateSecurityGroupType createSecurityGroupType = MessageHelper.createMessage(CreateSecurityGroupType.class, action.info.getEffectiveUserId());
        if (!Strings.isNullOrEmpty(action.properties.getGroupDescription())) {
          createSecurityGroupType.setGroupDescription(action.properties.getGroupDescription());
        }
        if (!Strings.isNullOrEmpty(action.properties.getVpcId())) {
          createSecurityGroupType.setVpcId(action.properties.getVpcId());
        }
        String groupName = action.getDefaultPhysicalResourceId();
        createSecurityGroupType.setGroupName(groupName);
        CreateSecurityGroupResponseType createSecurityGroupResponseType = AsyncRequests.<CreateSecurityGroupType,CreateSecurityGroupResponseType> sendSync(configuration, createSecurityGroupType);
        String groupId = createSecurityGroupResponseType.getGroupId();
        if (!Strings.isNullOrEmpty(action.properties.getVpcId())) {
          action.info.setPhysicalResourceId(groupId);
        } else {
          action.info.setPhysicalResourceId(groupName);
        }
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        action.info.setGroupId(JsonHelper.getStringFromJsonNode(new TextNode(groupId)));
        return action;
      }

    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SecurityGroupResourceAction action = (AWSEC2SecurityGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        List<EC2Tag> tags = TagHelper.getEC2StackTags(action.info, action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedEC2TemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags());
        }
        // due to stack aws: tags
        CreateTagsType createTagsType = MessageHelper.createPrivilegedMessage(CreateTagsType.class, action.info.getEffectiveUserId());
        createTagsType.setResourcesSet(Lists.newArrayList(JsonHelper.getJsonNodeFromString(action.info.getGroupId()).textValue()));
        createTagsType.setTagSet(EC2Helper.createTagSet(tags));
        AsyncRequests.<CreateTagsType,CreateTagsResponseType> sendSync(configuration, createTagsType);
        return action;
      }

    },
    CREATE_INGRESS_RULES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SecurityGroupResourceAction action = (AWSEC2SecurityGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getSecurityGroupIngress() != null && !action.properties.getSecurityGroupIngress().isEmpty()) {
          for (EC2SecurityGroupRule ec2SecurityGroupRule : action.properties.getSecurityGroupIngress()) {
            AuthorizeSecurityGroupIngressType authorizeSecurityGroupIngressType = MessageHelper.createMessage(AuthorizeSecurityGroupIngressType.class, action.info.getEffectiveUserId());
            authorizeSecurityGroupIngressType.setGroupId(JsonHelper.getJsonNodeFromString(action.info.getGroupId()).textValue());

            // Can't specify cidr and source security group
            if (!Strings.isNullOrEmpty(ec2SecurityGroupRule.getCidrIp()) &&
              (!Strings.isNullOrEmpty(ec2SecurityGroupRule.getSourceSecurityGroupId())
                || !Strings.isNullOrEmpty(ec2SecurityGroupRule.getSourceSecurityGroupName())
                || !Strings.isNullOrEmpty(ec2SecurityGroupRule.getSourceSecurityGroupOwnerId()))) {
              throw new ValidationErrorException("Both CidrIp and SourceSecurityGroup cannot be specified in SecurityGroupIngress");
            }
            // Can't specify both source security group name and id
            if (!Strings.isNullOrEmpty(ec2SecurityGroupRule.getSourceSecurityGroupId()) &&
              !Strings.isNullOrEmpty(ec2SecurityGroupRule.getSourceSecurityGroupName())) {
              throw new ValidationErrorException("Both SourceSecurityGroupName and SourceSecurityGroupId cannot be specified in SecurityGroupIngress");
            }
            IpPermissionType ipPermissionType = new IpPermissionType(
              ec2SecurityGroupRule.getIpProtocol(),
              ec2SecurityGroupRule.getFromPort(),
              ec2SecurityGroupRule.getToPort()
            );
            if (!Strings.isNullOrEmpty(ec2SecurityGroupRule.getCidrIp())) {
              ipPermissionType.setCidrIpRanges(Lists.newArrayList(ec2SecurityGroupRule.getCidrIp()));
            }
            if (!Strings.isNullOrEmpty(ec2SecurityGroupRule.getSourceSecurityGroupId())) {
              // Generally no need for SourceSecurityGroupOwnerId if SourceSecurityGroupId is set, but pass it along if set
              ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(ec2SecurityGroupRule.getSourceSecurityGroupOwnerId(), null, ec2SecurityGroupRule.getSourceSecurityGroupId())));
            }
            if (!Strings.isNullOrEmpty(ec2SecurityGroupRule.getSourceSecurityGroupName())) {
              // I think SourceSecurityGroupOwnerId is needed here.  If not provided, use the local account id
              String sourceSecurityGroupOwnerId = ec2SecurityGroupRule.getSourceSecurityGroupOwnerId();
              if (Strings.isNullOrEmpty(sourceSecurityGroupOwnerId)) {
                sourceSecurityGroupOwnerId = action.getStackEntity().getAccountId();
              }
              ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(sourceSecurityGroupOwnerId, ec2SecurityGroupRule.getSourceSecurityGroupName(), null)));
            }
            authorizeSecurityGroupIngressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
            AuthorizeSecurityGroupIngressResponseType authorizeSecurityGroupIngressResponseType = AsyncRequests.<AuthorizeSecurityGroupIngressType, AuthorizeSecurityGroupIngressResponseType> sendSync(configuration, authorizeSecurityGroupIngressType);
          }
        }
        return action;
      }
    },
    CREATE_EGRESS_RULES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SecurityGroupResourceAction action = (AWSEC2SecurityGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        if (action.properties.getSecurityGroupEgress() != null && !action.properties.getSecurityGroupEgress().isEmpty()) {
          // revoke default
          RevokeSecurityGroupEgressType revokeSecurityGroupEgressType = MessageHelper.createMessage(RevokeSecurityGroupEgressType.class, action.info.getEffectiveUserId());
          revokeSecurityGroupEgressType.setGroupId(JsonHelper.getJsonNodeFromString(action.info.getGroupId()).textValue());
          revokeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(DEFAULT_EGRESS_RULE()));
          RevokeSecurityGroupEgressResponseType revokeSecurityGroupEgressResponseType = AsyncRequests.<RevokeSecurityGroupEgressType, RevokeSecurityGroupEgressResponseType> sendSync(configuration, revokeSecurityGroupEgressType);

          for (EC2SecurityGroupRule ec2SecurityGroupRule : action.properties.getSecurityGroupEgress()) {
            AuthorizeSecurityGroupEgressType authorizeSecurityGroupEgressType = MessageHelper.createMessage(AuthorizeSecurityGroupEgressType.class, action.info.getEffectiveUserId());
            authorizeSecurityGroupEgressType.setGroupId(JsonHelper.getJsonNodeFromString(action.info.getGroupId()).textValue());

            // Can't specify cidr and Destination security group
            if (!Strings.isNullOrEmpty(ec2SecurityGroupRule.getCidrIp()) && !Strings.isNullOrEmpty(ec2SecurityGroupRule.getDestinationSecurityGroupId())) {
              throw new ValidationErrorException("Both CidrIp and DestinationSecurityGroup cannot be specified in SecurityGroupEgress");
            }
            IpPermissionType ipPermissionType = new IpPermissionType(
              ec2SecurityGroupRule.getIpProtocol(),
              ec2SecurityGroupRule.getFromPort(),
              ec2SecurityGroupRule.getToPort()
            );
            if (!Strings.isNullOrEmpty(ec2SecurityGroupRule.getCidrIp())) {
              ipPermissionType.setCidrIpRanges(Lists.newArrayList(ec2SecurityGroupRule.getCidrIp()));
            }
            if (!Strings.isNullOrEmpty(ec2SecurityGroupRule.getDestinationSecurityGroupId())) {
              ipPermissionType.setGroups(Lists.newArrayList(new UserIdGroupPairType(null, null, ec2SecurityGroupRule.getDestinationSecurityGroupId())));
            }
            authorizeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
            AuthorizeSecurityGroupEgressResponseType authorizeSecurityGroupEgressResponseType = AsyncRequests.<AuthorizeSecurityGroupEgressType, AuthorizeSecurityGroupEgressResponseType> sendSync(configuration, authorizeSecurityGroupEgressType);
          }
        }
        return action;
      }
    };

    // None of these tasks require retry...

    @Override
    public RetryPolicy getRetryPolicy() {
      return null;
    }

  }

  private enum DeleteSteps implements DeleteStep {
    DELETE_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SecurityGroupResourceAction action = (AWSEC2SecurityGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // See if group was ever populated
        if (action.info.getPhysicalResourceId() == null) return action;
        // See if group exists now
        String groupId = JsonHelper.getJsonNodeFromString(action.info.getGroupId()).textValue();
        DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
        describeSecurityGroupsType.setSecurityGroupIdSet(Lists.newArrayList(groupId));
        DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType =
          AsyncRequests.<DescribeSecurityGroupsType, DescribeSecurityGroupsResponseType>sendSync(configuration, describeSecurityGroupsType);
        ArrayList<SecurityGroupItemType> securityGroupItemTypeArrayList = describeSecurityGroupsResponseType.getSecurityGroupInfo();
        if (securityGroupItemTypeArrayList == null || securityGroupItemTypeArrayList.isEmpty()) {
          return action;
        }
        // Delete the group (may fail)
        DeleteSecurityGroupType deleteSecurityGroupType = MessageHelper.createMessage(DeleteSecurityGroupType.class, action.info.getEffectiveUserId());
        deleteSecurityGroupType.setGroupId(groupId);
        try {
          AsyncRequests.<DeleteSecurityGroupType, DeleteSecurityGroupResponseType>sendSync(configuration, deleteSecurityGroupType);
          return action;
        } catch (Exception ex) {
          Throwable cause = Throwables.getRootCause(ex);
          throw new ValidationFailedException(ex.getMessage());
        }
      }
    }
  }


  private static final IpPermissionType DEFAULT_EGRESS_RULE() {
    IpPermissionType ipPermissionType = new IpPermissionType();
    ipPermissionType.setIpProtocol("-1");
    ipPermissionType.setCidrIpRanges(Lists.newArrayList("0.0.0.0/0"));
    return ipPermissionType;
  }

  @Override
  public Promise<String> getCreatePromise(CreateStackWorkflowImpl createStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.transform(Lists.newArrayList(CreateSteps.values()), CREATE_STEPS_TRANSFORM.INSTANCE);
    return new MultiStepWithRetryCreatePromise(createStackWorkflow, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(DeleteStackWorkflowImpl deleteStackWorkflow, String resourceId, String stackId, String accountId, String effectiveUserId) {
    Collection<Class<? extends Throwable>> exceptionList = Lists.newArrayList();
    exceptionList.add(NotAResourceFailureException.class);
    ExponentialRetryPolicy retryPolicy = new ExponentialRetryPolicy(1L).withExceptionsToRetry(exceptionList);
    if (SECURITY_GROUP_MAX_DELETE_RETRY_SECS != null && SECURITY_GROUP_MAX_DELETE_RETRY_SECS > 0) {
      retryPolicy.setRetryExpirationIntervalSeconds(SECURITY_GROUP_MAX_DELETE_RETRY_SECS);
    }
    return new RetrySingleStepDeletePromise(deleteStackWorkflow, DeleteSteps.DELETE_GROUP.name(), retryPolicy).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  private enum CREATE_STEPS_TRANSFORM implements Function<CreateSteps, String> {
    INSTANCE {
      @Nullable
      @Override
      public String apply(@Nullable CreateSteps createSteps) {
        return createSteps.name();
      }
    }
  }
}



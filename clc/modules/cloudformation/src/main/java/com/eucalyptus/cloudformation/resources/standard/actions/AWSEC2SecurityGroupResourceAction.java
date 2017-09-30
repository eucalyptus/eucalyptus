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
import com.eucalyptus.cloudformation.resources.EC2Helper;
import com.eucalyptus.cloudformation.resources.IpPermissionTypeWithEquals;
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
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupEgressResponseType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupEgressType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateSecurityGroupResponseType;
import com.eucalyptus.compute.common.CreateSecurityGroupType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteSecurityGroupResponseType;
import com.eucalyptus.compute.common.DeleteSecurityGroupType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.RevokeSecurityGroupEgressResponseType;
import com.eucalyptus.compute.common.RevokeSecurityGroupEgressType;
import com.eucalyptus.compute.common.RevokeSecurityGroupIngressResponseType;
import com.eucalyptus.compute.common.RevokeSecurityGroupIngressType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.util.async.AsyncRequests;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSEC2SecurityGroupResourceAction extends StepBasedResourceAction {
  @ConfigurableField(initial = "300", description = "The amount of time (in seconds) to retry security group deletes (may fail if instances from autoscaling group)")
  public static volatile Integer SECURITY_GROUP_MAX_DELETE_RETRY_SECS = 300;

  private static final Logger LOG = Logger.getLogger(AWSEC2SecurityGroupResourceAction.class);
  private AWSEC2SecurityGroupProperties properties = new AWSEC2SecurityGroupProperties();
  private AWSEC2SecurityGroupResourceInfo info = new AWSEC2SecurityGroupResourceInfo();

  public AWSEC2SecurityGroupResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSEC2SecurityGroupResourceAction otherAction = (AWSEC2SecurityGroupResourceAction) resourceAction;
    if (!Objects.equals(properties.getGroupDescription(), otherAction.properties.getGroupDescription())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getSecurityGroupEgress(), otherAction.properties.getSecurityGroupEgress())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getSecurityGroupIngress(), otherAction.properties.getSecurityGroupIngress())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getTags(), otherAction.properties.getTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getVpcId(), otherAction.properties.getVpcId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    return updateType;
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

  private enum CreateSteps implements Step {
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
        action.info.setCreatedEnoughToDelete(true);
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
        // Create 'system' tags as admin user
        String effectiveAdminUserId = action.info.getAccountId( );
        CreateTagsType createSystemTagsType = MessageHelper.createPrivilegedMessage(CreateTagsType.class, effectiveAdminUserId);
        createSystemTagsType.setResourcesSet(Lists.newArrayList(JsonHelper.getJsonNodeFromString(action.info.getGroupId()).asText()));
        createSystemTagsType.setTagSet(EC2Helper.createTagSet(TagHelper.getEC2SystemTags(action.info, action.getStackEntity())));
        AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createSystemTagsType);
        // Create non-system tags as regular user
        List<EC2Tag> tags = TagHelper.getEC2StackTags(action.getStackEntity());
        if (action.properties.getTags() != null && !action.properties.getTags().isEmpty()) {
          TagHelper.checkReservedEC2TemplateTags(action.properties.getTags());
          tags.addAll(action.properties.getTags());
        }
        if (!tags.isEmpty()) {
          CreateTagsType createTagsType = MessageHelper.createMessage(CreateTagsType.class, action.info.getEffectiveUserId());
          createTagsType.setResourcesSet(Lists.newArrayList(JsonHelper.getJsonNodeFromString(action.info.getGroupId()).asText()));
          createTagsType.setTagSet(EC2Helper.createTagSet(tags));
          AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createTagsType);
        }
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
            authorizeSecurityGroupIngressType.setGroupId(JsonHelper.getJsonNodeFromString(action.info.getGroupId()).asText());
            IpPermissionType ipPermissionType = getIpPermissionTypeForIngress(action, ec2SecurityGroupRule);
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
          revokeSecurityGroupEgressType.setGroupId(JsonHelper.getJsonNodeFromString(action.info.getGroupId()).asText());
          revokeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(DEFAULT_EGRESS_RULE()));
          RevokeSecurityGroupEgressResponseType revokeSecurityGroupEgressResponseType = AsyncRequests.<RevokeSecurityGroupEgressType, RevokeSecurityGroupEgressResponseType> sendSync(configuration, revokeSecurityGroupEgressType);

          for (EC2SecurityGroupRule ec2SecurityGroupRule : action.properties.getSecurityGroupEgress()) {
            AuthorizeSecurityGroupEgressType authorizeSecurityGroupEgressType = MessageHelper.createMessage(AuthorizeSecurityGroupEgressType.class, action.info.getEffectiveUserId());
            authorizeSecurityGroupEgressType.setGroupId(JsonHelper.getJsonNodeFromString(action.info.getGroupId()).asText());
            IpPermissionType ipPermissionType = getIpPermissionTypeForEgress(ec2SecurityGroupRule);


            authorizeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(ipPermissionType));
            AuthorizeSecurityGroupEgressResponseType authorizeSecurityGroupEgressResponseType = AsyncRequests.<AuthorizeSecurityGroupEgressType, AuthorizeSecurityGroupEgressResponseType> sendSync(configuration, authorizeSecurityGroupEgressType);
          }
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

  private static IpPermissionType getIpPermissionTypeForEgress(EC2SecurityGroupRule ec2SecurityGroupRule) throws ValidationErrorException {
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
    return ipPermissionType;
  }

  private static IpPermissionType getIpPermissionTypeForIngress(AWSEC2SecurityGroupResourceAction action, EC2SecurityGroupRule ec2SecurityGroupRule) throws ValidationErrorException {
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
    return ipPermissionType;
  }

  private enum DeleteSteps implements Step {
    DELETE_GROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSEC2SecurityGroupResourceAction action = (AWSEC2SecurityGroupResourceAction) resourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // See if group was ever populated
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        // See if group exists now
        String groupId = JsonHelper.getJsonNodeFromString(action.info.getGroupId()).asText();
        DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, action.info.getEffectiveUserId());
        describeSecurityGroupsType.setFilterSet( Lists.newArrayList( CloudFilters.filter( "group-id", groupId ) ) );
        DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);
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
          throw new RetryAfterConditionCheckFailedException(ex.getMessage());
        }
      }

      @Override
      public Integer getTimeout() {
        return SECURITY_GROUP_MAX_DELETE_RETRY_SECS;
      }
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_INGRESS_AND_EGRESS_RULES {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2SecurityGroupResourceAction oldAction = (AWSEC2SecurityGroupResourceAction) oldResourceAction;
        AWSEC2SecurityGroupResourceAction newAction = (AWSEC2SecurityGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        // first get ingress and egress rules...
        String groupId = JsonHelper.getJsonNodeFromString(newAction.info.getGroupId()).asText();
        DescribeSecurityGroupsType describeSecurityGroupsType = MessageHelper.createMessage(DescribeSecurityGroupsType.class, newAction.info.getEffectiveUserId());
        describeSecurityGroupsType.setFilterSet( Lists.newArrayList( CloudFilters.filter( "group-id", groupId ) ) );
        DescribeSecurityGroupsResponseType describeSecurityGroupsResponseType = AsyncRequests.sendSync(configuration, describeSecurityGroupsType);

        Set<IpPermissionTypeWithEquals> existingIngressPermissionTypes = Sets.newLinkedHashSet();
        Set<IpPermissionTypeWithEquals> existingEgressPermissionTypes = Sets.newLinkedHashSet();

        if (describeSecurityGroupsResponseType != null && describeSecurityGroupsResponseType.getSecurityGroupInfo() != null &&
          !describeSecurityGroupsResponseType.getSecurityGroupInfo().isEmpty()) {
          if (describeSecurityGroupsResponseType.getSecurityGroupInfo().get(0).getIpPermissions() != null) {
            existingIngressPermissionTypes.addAll(IpPermissionTypeWithEquals.getNonNullCollection(describeSecurityGroupsResponseType.getSecurityGroupInfo().get(0).getIpPermissions()));
          }
          if (describeSecurityGroupsResponseType.getSecurityGroupInfo().get(0).getIpPermissionsEgress() != null) {
            existingEgressPermissionTypes.addAll(IpPermissionTypeWithEquals.getNonNullCollection(describeSecurityGroupsResponseType.getSecurityGroupInfo().get(0).getIpPermissionsEgress()));
          }
        }

        Set<IpPermissionTypeWithEquals> newIngressPermissionTypes = Sets.newLinkedHashSet();
        Set<IpPermissionTypeWithEquals> newEgressPermissionTypes = Sets.newLinkedHashSet();

        if (newAction.properties.getSecurityGroupIngress() != null && !newAction.properties.getSecurityGroupIngress().isEmpty()) {
          for (EC2SecurityGroupRule ec2SecurityGroupRule : newAction.properties.getSecurityGroupIngress()) {
            IpPermissionType ipPermissionType = getIpPermissionTypeForIngress(newAction, ec2SecurityGroupRule);
            newIngressPermissionTypes.add(new IpPermissionTypeWithEquals(ipPermissionType));
          }
        }

        if (newAction.properties.getSecurityGroupEgress() != null && !newAction.properties.getSecurityGroupEgress().isEmpty()) {
          for (EC2SecurityGroupRule ec2SecurityGroupRule : newAction.properties.getSecurityGroupEgress()) {
            IpPermissionType ipPermissionType = getIpPermissionTypeForEgress(ec2SecurityGroupRule);
            newEgressPermissionTypes.add(new IpPermissionTypeWithEquals(ipPermissionType));
          }
        }

        Set<IpPermissionTypeWithEquals> oldIngressPermissionTypes = Sets.newLinkedHashSet();
        Set<IpPermissionTypeWithEquals> oldEgressPermissionTypes = Sets.newLinkedHashSet();

        if (oldAction.properties.getSecurityGroupIngress() != null && !oldAction.properties.getSecurityGroupIngress().isEmpty()) {
          for (EC2SecurityGroupRule ec2SecurityGroupRule : oldAction.properties.getSecurityGroupIngress()) {
            IpPermissionType ipPermissionType = getIpPermissionTypeForIngress(oldAction, ec2SecurityGroupRule);
            oldIngressPermissionTypes.add(new IpPermissionTypeWithEquals(ipPermissionType));
          }
        }

        if (oldAction.properties.getSecurityGroupEgress() != null && !oldAction.properties.getSecurityGroupEgress().isEmpty()) {
          for (EC2SecurityGroupRule ec2SecurityGroupRule : oldAction.properties.getSecurityGroupEgress()) {
            IpPermissionType ipPermissionType = getIpPermissionTypeForEgress(ec2SecurityGroupRule);
            oldEgressPermissionTypes.add(new IpPermissionTypeWithEquals(ipPermissionType));
          }
        }

        // add all new rules that are not in the existing set
        for (IpPermissionTypeWithEquals ipPermissionTypeWithEquals : Sets.difference(newIngressPermissionTypes, existingIngressPermissionTypes)) {
          AuthorizeSecurityGroupIngressType authorizeSecurityGroupIngressType = MessageHelper.createMessage(AuthorizeSecurityGroupIngressType.class, newAction.info.getEffectiveUserId());
          authorizeSecurityGroupIngressType.setGroupId(groupId);
          authorizeSecurityGroupIngressType.setIpPermissions(Lists.newArrayList(ipPermissionTypeWithEquals.getIpPermissionType()));
          AsyncRequests.<AuthorizeSecurityGroupIngressType, AuthorizeSecurityGroupIngressResponseType> sendSync(configuration, authorizeSecurityGroupIngressType);
        }
        for (IpPermissionTypeWithEquals ipPermissionTypeWithEquals : Sets.difference(newEgressPermissionTypes, existingEgressPermissionTypes)) {
          AuthorizeSecurityGroupEgressType authorizeSecurityGroupEgressType = MessageHelper.createMessage(AuthorizeSecurityGroupEgressType.class, newAction.info.getEffectiveUserId());
          authorizeSecurityGroupEgressType.setGroupId(groupId);
          authorizeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(ipPermissionTypeWithEquals.getIpPermissionType()));
          AsyncRequests.<AuthorizeSecurityGroupEgressType, AuthorizeSecurityGroupEgressResponseType> sendSync(configuration, authorizeSecurityGroupEgressType);
        }

        // revoke all rules from the old set that exist and are not new
        for (IpPermissionTypeWithEquals ipPermissionTypeWithEquals : Sets.intersection(oldIngressPermissionTypes, Sets.difference(existingIngressPermissionTypes, newIngressPermissionTypes))) {
          RevokeSecurityGroupIngressType revokeSecurityGroupIngressType = MessageHelper.createMessage(RevokeSecurityGroupIngressType.class, newAction.info.getEffectiveUserId());
          revokeSecurityGroupIngressType.setGroupId(groupId);
          revokeSecurityGroupIngressType.setIpPermissions(Lists.newArrayList(ipPermissionTypeWithEquals.getIpPermissionType()));
          AsyncRequests.<RevokeSecurityGroupIngressType, RevokeSecurityGroupIngressResponseType> sendSync(configuration, revokeSecurityGroupIngressType);
        }
        for (IpPermissionTypeWithEquals ipPermissionTypeWithEquals : Sets.intersection(oldEgressPermissionTypes, Sets.difference(existingEgressPermissionTypes, newEgressPermissionTypes))) {
          RevokeSecurityGroupEgressType revokeSecurityGroupEgressType = MessageHelper.createMessage(RevokeSecurityGroupEgressType.class, newAction.info.getEffectiveUserId());
          revokeSecurityGroupEgressType.setGroupId(groupId);
          revokeSecurityGroupEgressType.setIpPermissions(Lists.newArrayList(ipPermissionTypeWithEquals.getIpPermissionType()));
          AsyncRequests.<RevokeSecurityGroupEgressType, RevokeSecurityGroupEgressResponseType> sendSync(configuration, revokeSecurityGroupEgressType);
        }

        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSEC2SecurityGroupResourceAction oldAction = (AWSEC2SecurityGroupResourceAction) oldResourceAction;
        AWSEC2SecurityGroupResourceAction newAction = (AWSEC2SecurityGroupResourceAction) newResourceAction;
        ServiceConfiguration configuration = Topology.lookup(Compute.class);
        String groupId = JsonHelper.getJsonNodeFromString(newAction.info.getGroupId()).asText();
        DescribeTagsType describeTagsType = MessageHelper.createMessage(DescribeTagsType.class, newAction.info.getEffectiveUserId());
        describeTagsType.setFilterSet(Lists.newArrayList( CloudFilters.filter("resource-id", groupId)));
        DescribeTagsResponseType describeTagsResponseType = AsyncRequests.sendSync(configuration, describeTagsType);
        Set<EC2Tag> existingTags = Sets.newLinkedHashSet();
        if (describeTagsResponseType != null && describeTagsResponseType.getTagSet() != null) {
          for (TagInfo tagInfo: describeTagsResponseType.getTagSet()) {
            EC2Tag tag = new EC2Tag();
            tag.setKey(tagInfo.getKey());
            tag.setValue(tagInfo.getValue());
            existingTags.add(tag);
          }
        }
        Set<EC2Tag> newTags = Sets.newLinkedHashSet();
        if (newAction.properties.getTags() != null) {
          newTags.addAll(newAction.properties.getTags());
        }
        List<EC2Tag> newStackTags = TagHelper.getEC2StackTags(newAction.getStackEntity());
        if (newStackTags != null) {
          newTags.addAll(newStackTags);
        }
        TagHelper.checkReservedEC2TemplateTags(newTags);
        // add only 'new' tags
        Set<EC2Tag> onlyNewTags = Sets.difference(newTags, existingTags);
        if (!onlyNewTags.isEmpty()) {
          CreateTagsType createTagsType = MessageHelper.createMessage(CreateTagsType.class, newAction.info.getEffectiveUserId());
          createTagsType.setResourcesSet(Lists.newArrayList(groupId));
          createTagsType.setTagSet(EC2Helper.createTagSet(onlyNewTags));
          AsyncRequests.<CreateTagsType, CreateTagsResponseType>sendSync(configuration, createTagsType);
        }
        //  Get old tags...
        Set<EC2Tag> oldTags = Sets.newLinkedHashSet();
        if (oldAction.properties.getTags() != null) {
          oldTags.addAll(oldAction.properties.getTags());
        }
        List<EC2Tag> oldStackTags = TagHelper.getEC2StackTags(oldAction.getStackEntity());
        if (oldStackTags != null) {
          oldTags.addAll(oldStackTags);
        }
        // remove only the old tags that are not new and that exist
        Set<EC2Tag> tagsToRemove = Sets.intersection(oldTags, Sets.difference(existingTags, newTags));
        if (!tagsToRemove.isEmpty()) {
          DeleteTagsType deleteTagsType = MessageHelper.createMessage(DeleteTagsType.class, newAction.info.getEffectiveUserId());
          deleteTagsType.setResourcesSet(Lists.newArrayList(groupId));
          deleteTagsType.setTagSet(EC2Helper.deleteTagSet(tagsToRemove));
          AsyncRequests.<DeleteTagsType, DeleteTagsResponseType>sendSync(configuration, deleteTagsType);
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



  private static final IpPermissionType DEFAULT_EGRESS_RULE() {
    IpPermissionType ipPermissionType = new IpPermissionType();
    ipPermissionType.setIpProtocol("-1");
    ipPermissionType.setCidrIpRanges(Lists.newArrayList("0.0.0.0/0"));
    return ipPermissionType;
  }



}



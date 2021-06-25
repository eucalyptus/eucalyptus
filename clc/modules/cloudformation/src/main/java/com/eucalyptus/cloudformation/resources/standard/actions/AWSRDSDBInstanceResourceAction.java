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
import com.eucalyptus.cloudformation.resources.standard.info.AWSRDSDBInstanceResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSRDSDBInstanceProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.rds.common.RdsApi;
import com.eucalyptus.rds.common.msgs.AddTagsToResourceType;
import com.eucalyptus.rds.common.msgs.CreateDBInstanceResponseType;
import com.eucalyptus.rds.common.msgs.CreateDBInstanceType;
import com.eucalyptus.rds.common.msgs.DBInstance;
import com.eucalyptus.rds.common.msgs.DeleteDBInstanceType;
import com.eucalyptus.rds.common.msgs.DescribeDBInstancesResponseType;
import com.eucalyptus.rds.common.msgs.DescribeDBInstancesType;
import com.eucalyptus.rds.common.msgs.KeyList;
import com.eucalyptus.rds.common.msgs.RemoveTagsFromResourceType;
import com.eucalyptus.rds.common.msgs.Tag;
import com.eucalyptus.rds.common.msgs.TagList;
import com.eucalyptus.rds.common.msgs.VpcSecurityGroupIdList;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import io.vavr.collection.Stream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 *
 */
public class AWSRDSDBInstanceResourceAction extends StepBasedResourceAction {

  public static volatile Integer INSTANCE_CREATE_MAX_CREATE_RETRY_SECS = 600;
  public static volatile Integer INSTANCE_DELETE_MAX_DELETE_RETRY_SECS = 300;


  private AWSRDSDBInstanceProperties properties = new AWSRDSDBInstanceProperties();
  private AWSRDSDBInstanceResourceInfo info = new AWSRDSDBInstanceResourceInfo();

  public AWSRDSDBInstanceResourceAction() {
    super(
        fromEnum(CreateSteps.class),
        fromEnum(DeleteSteps.class),
        fromUpdateEnum(UpdateNoInterruptionSteps.class),
        null);
  }


  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSRDSDBInstanceResourceAction otherAction = (AWSRDSDBInstanceResourceAction) resourceAction;
    //TODO updates ?
    //if (!Objects.equals(properties.getPolicyDocument(), otherAction.properties.getPolicyDocument())) {
    updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    //}
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_DBINSTANCE {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSRDSDBInstanceResourceAction action = (AWSRDSDBInstanceResourceAction) resourceAction;
        final RdsApi rds = AsyncProxy.client( RdsApi.class, Function.identity( ) );
        final CreateDBInstanceType createDBInstance =
            MessageHelper.createMessage(CreateDBInstanceType.class, action.info.getEffectiveUserId());
        createDBInstance.setDBInstanceIdentifier(action.properties.getDbInstanceIdentifier());
        createDBInstance.setDBInstanceClass(action.properties.getDbInstanceClass());
        createDBInstance.setDBName(action.properties.getDbName());
        createDBInstance.setPort(Ints.tryParse(Strings.nullToEmpty(action.properties.getPort())));
        createDBInstance.setDBSubnetGroupName(action.properties.getDbSubnetGroupName());
        createDBInstance.setAllocatedStorage(Ints.tryParse(Strings.nullToEmpty(action.properties.getAllocatedStorage())));
        createDBInstance.setAvailabilityZone(action.properties.getAvailabilityZone());
        createDBInstance.setCopyTagsToSnapshot(action.properties.getCopyTagsToSnapshot());
        createDBInstance.setEngine(action.properties.getEngine());
        createDBInstance.setEngineVersion(action.properties.getEngineVersion());
        createDBInstance.setMasterUsername(action.properties.getMasterUsername());
        createDBInstance.setMasterUserPassword(action.properties.getMasterUserPassword());
        createDBInstance.setPubliclyAccessible(action.properties.getPubliclyAccessible());
        if (action.properties.getVpcSecurityGroups() != null && !action.properties.getVpcSecurityGroups().isEmpty()) {
          final VpcSecurityGroupIdList securityGroupIdList = new VpcSecurityGroupIdList();
          securityGroupIdList.getMember().addAll(action.properties.getVpcSecurityGroups());
          createDBInstance.setVpcSecurityGroupIds(securityGroupIdList);
        }
        final Set<CloudFormationResourceTag> tags = getTags(action, action.properties.getTags());
        if (!tags.isEmpty()) {
          createDBInstance.setTags(toTagList(tags));
        }
        final CreateDBInstanceResponseType response = rds.createDBInstance(createDBInstance);
        action.info.setPhysicalResourceId(action.properties.getDbInstanceIdentifier());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        if (response.getCreateDBInstanceResult() != null &&
            response.getCreateDBInstanceResult().getDBInstance() != null &&
            response.getCreateDBInstanceResult().getDBInstance().getDBInstanceArn() != null) {
          action.info.setArn(response.getCreateDBInstanceResult().getDBInstance().getDBInstanceArn());
        }
        return action;
      }
    },
    ADD_SYSTEM_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) {
        final AWSRDSDBInstanceResourceAction action = (AWSRDSDBInstanceResourceAction) resourceAction;
        addSystemTags(action.info.getArn(), action);
        return action;
      }
    },
    WAIT_UNTIL_AVAILABLE {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSRDSDBInstanceResourceAction action = (AWSRDSDBInstanceResourceAction) resourceAction;
        final RdsApi rds = AsyncProxy.client( RdsApi.class, Function.identity( ) );
        final DescribeDBInstancesType describeDBInstances =
            MessageHelper.createMessage(DescribeDBInstancesType.class, action.info.getEffectiveUserId());
        describeDBInstances.setDBInstanceIdentifier(action.info.getPhysicalResourceId());
        final DescribeDBInstancesResponseType describeInstancesResponse = rds.describeDBInstances(describeDBInstances);
        if (describeInstancesResponse.getDescribeDBInstancesResult().getDBInstances().getMember().size() == 0) {
          throw new RetryAfterConditionCheckFailedException("DB Instance " + action.info.getPhysicalResourceId( ) + " does not yet exist");
        }
        final DBInstance dbInstance = describeInstancesResponse.getDescribeDBInstancesResult().getDBInstances().getMember().get(0);
        if ("available".equals(dbInstance.getDBInstanceStatus())) {
          if (dbInstance.getEndpoint() != null) {
            action.info.setEndpointAddress(JsonHelper.getStringFromJsonNode(new TextNode(dbInstance.getEndpoint().getAddress())));
            action.info.setEndpointPort(JsonHelper.getStringFromJsonNode(new IntNode(dbInstance.getEndpoint().getPort())));
          }
          action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
          return action;
        }
        throw new RetryAfterConditionCheckFailedException(("DB Instance " + action.info.getPhysicalResourceId() + " is not yet available, currently " + dbInstance.getDBInstanceStatus()));
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_CREATE_MAX_CREATE_RETRY_SECS;
      }
    },
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_DBINSTANCE {
      @Override
      public ResourceAction perform(
          final ResourceAction oldResourceAction,
          final ResourceAction newResourceAction
      ) {
        final AWSRDSDBInstanceResourceAction oldAction = (AWSRDSDBInstanceResourceAction) oldResourceAction;
        final AWSRDSDBInstanceResourceAction newAction = (AWSRDSDBInstanceResourceAction) newResourceAction;
        //TODO updates ?
        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        final AWSRDSDBInstanceResourceAction oldAction = (AWSRDSDBInstanceResourceAction) oldResourceAction;
        final AWSRDSDBInstanceResourceAction newAction = (AWSRDSDBInstanceResourceAction) newResourceAction;
        updateTags(
            oldAction.info.getArn(),
            oldAction,
            oldAction.properties.getTags(),
            newAction,
            newAction.properties.getTags());
        return newResourceAction;
      }
    },
  }

  private enum DeleteSteps implements Step {
    DELETE_DBINSTANCE {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSRDSDBInstanceResourceAction action = (AWSRDSDBInstanceResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final RdsApi rds = AsyncProxy.client( RdsApi.class, Function.identity( ) );
        final DeleteDBInstanceType deleteDBInstance =
            MessageHelper.createMessage(DeleteDBInstanceType.class, action.info.getEffectiveUserId());
        deleteDBInstance.setDBInstanceIdentifier(action.info.getPhysicalResourceId());
        try {
          rds.deleteDBInstance(deleteDBInstance);
        } catch ( final RuntimeException ex ) {
          if (AsyncExceptions.isWebServiceErrorCode(ex, "DBInstanceNotFound")) {
            return action;
          }
          throw ex;
        }
        return action;
      }
    },
    VERIFY_DELETED {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSRDSDBInstanceResourceAction action = (AWSRDSDBInstanceResourceAction) resourceAction;
        // See if instance was ever populated
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final RdsApi rds = AsyncProxy.client( RdsApi.class, Function.identity( ) );
        final DescribeDBInstancesType describeDBInstances =
            MessageHelper.createMessage(DescribeDBInstancesType.class, action.info.getEffectiveUserId());
        describeDBInstances.setDBInstanceIdentifier(action.info.getPhysicalResourceId());
        try {
          final DescribeDBInstancesResponseType describeInstancesResponse = rds.describeDBInstances(describeDBInstances);
          if (describeInstancesResponse.getDescribeDBInstancesResult().getDBInstances().getMember().size() == 0) {
            return action; // already deleted
          }
        } catch ( final RuntimeException ex ) {
          if (AsyncExceptions.isWebServiceErrorCode(ex, "DBInstanceNotFound")) {
            return action;
          }
        }
        throw new RetryAfterConditionCheckFailedException(("Instance " + action.info.getPhysicalResourceId() + " is not yet deleted"));
      }

      @Override
      public Integer getTimeout() {
        return INSTANCE_DELETE_MAX_DELETE_RETRY_SECS;
      }
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(final ResourceProperties resourceProperties) {
    properties = (AWSRDSDBInstanceProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(final ResourceInfo resourceInfo) {
    info = (AWSRDSDBInstanceResourceInfo) resourceInfo;
  }


    protected static void addSystemTags(final String arn, final ResourceAction action) {
    final List<CloudFormationResourceTag> tags =
        TagHelper.getCloudFormationResourceSystemTags(action.getResourceInfo(), action.getStackEntity());
    final RdsApi rds = AsyncProxy.client(
        RdsApi.class,
        MessageHelper.privilegedUserIdentity(action.getResourceInfo().getEffectiveUserId()));
    final AddTagsToResourceType addTags = new AddTagsToResourceType();
    addTags.setResourceName(arn);
    addTags.setTags(toTagList(tags));
    rds.addTagsToResource(addTags);
  }

  protected static void updateTags(
      final String arn,
      final ResourceAction oldAction,
      final ArrayList<CloudFormationResourceTag> oldTagProperty,
      final ResourceAction newAction,
      final ArrayList<CloudFormationResourceTag> newTagProperty
  ) throws Exception {
    final RdsApi rds = AsyncProxy.client(
        RdsApi.class,
        MessageHelper.userIdentity(newAction.getResourceInfo().getEffectiveUserId()));

    final Set<CloudFormationResourceTag> oldTags = getTags(oldAction, oldTagProperty, false);
    final Set<CloudFormationResourceTag> newTags = getTags(newAction, newTagProperty);

    // remove tags
    final Set<String> removedKeys = Sets.newHashSet();
    removedKeys.addAll(Stream.ofAll(oldTags).map(CloudFormationResourceTag::getKey).toJavaList());
    removedKeys.removeAll(Stream.ofAll(newTags).map(CloudFormationResourceTag::getKey).toJavaList());
    if (!removedKeys.isEmpty()) {
      final RemoveTagsFromResourceType removeTags = new RemoveTagsFromResourceType();
      removeTags.setResourceName(arn);
      removeTags.setTagKeys(toTagKeys(removedKeys));
      rds.removeTagsFromResource(removeTags);
    }

    // add or update tags
    newTags.removeAll(oldTags);
    if (!newTags.isEmpty()) {
      final AddTagsToResourceType addTags = new AddTagsToResourceType();
      addTags.setResourceName(arn);
      addTags.setTags(toTagList(newTags));
      rds.addTagsToResource(addTags);
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

  protected static KeyList toTagKeys(final Set<String> tags) {
    final KeyList tagKeys = new KeyList();
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
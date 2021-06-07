/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import java.util.function.Function;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSRDSDBInstanceResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSRDSDBInstanceProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.RetryAfterConditionCheckFailedException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.rds.common.RdsApi;
import com.eucalyptus.rds.common.msgs.CreateDBInstanceType;
import com.eucalyptus.rds.common.msgs.DBInstance;
import com.eucalyptus.rds.common.msgs.DeleteDBInstanceType;
import com.eucalyptus.rds.common.msgs.DescribeDBInstancesResponseType;
import com.eucalyptus.rds.common.msgs.DescribeDBInstancesType;
import com.eucalyptus.rds.common.msgs.VpcSecurityGroupIdList;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

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
        // createDBSubnetGroup.setTags();
        rds.createDBInstance(createDBInstance);
        action.info.setPhysicalResourceId(action.properties.getDbInstanceIdentifier());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    }
    ,
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
    }
    // TODO system tags via AddTagsToResource
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


  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_DBINSTANCE {
      @Override
      public ResourceAction perform(
          final ResourceAction oldResourceAction,
          final ResourceAction newResourceAction
      ) throws Exception {
        final AWSRDSDBInstanceResourceAction oldAction = (AWSRDSDBInstanceResourceAction) oldResourceAction;
        final AWSRDSDBInstanceResourceAction newAction = (AWSRDSDBInstanceResourceAction) newResourceAction;
        //TODO updates ?
        return newAction;
      }
    }
  }
}
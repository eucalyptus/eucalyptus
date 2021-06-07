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
import com.eucalyptus.cloudformation.resources.standard.info.AWSRDSDBSubnetGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSRDSDBSubnetGroupProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.rds.common.RdsApi;
import com.eucalyptus.rds.common.msgs.CreateDBSubnetGroupType;
import com.eucalyptus.rds.common.msgs.DeleteDBSubnetGroupType;
import com.eucalyptus.rds.common.msgs.SubnetIdentifierList;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 *
 */
public class AWSRDSDBSubnetGroupResourceAction extends StepBasedResourceAction {

  private AWSRDSDBSubnetGroupProperties properties = new AWSRDSDBSubnetGroupProperties();
  private AWSRDSDBSubnetGroupResourceInfo info = new AWSRDSDBSubnetGroupResourceInfo();

  public AWSRDSDBSubnetGroupResourceAction() {
    super(
        fromEnum(CreateSteps.class),
        fromEnum(DeleteSteps.class),
        fromUpdateEnum(UpdateNoInterruptionSteps.class),
        null);
  }


  @Override
  public UpdateType getUpdateType(final ResourceAction resourceAction, final boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    final AWSRDSDBSubnetGroupResourceAction otherAction = (AWSRDSDBSubnetGroupResourceAction) resourceAction;
    //TODO updates ?
    //if (!Objects.equals(properties.getPolicyDocument(), otherAction.properties.getPolicyDocument())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    //}
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_DBSUBNETGROUP {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSRDSDBSubnetGroupResourceAction action = (AWSRDSDBSubnetGroupResourceAction) resourceAction;
        final RdsApi rds = AsyncProxy.client( RdsApi.class, Function.identity( ) );
        final CreateDBSubnetGroupType createDBSubnetGroup =
            MessageHelper.createMessage(CreateDBSubnetGroupType.class, action.info.getEffectiveUserId());
        createDBSubnetGroup.setDBSubnetGroupName(action.properties.getDBSubnetGroupName());
        createDBSubnetGroup.setDBSubnetGroupDescription(action.properties.getDbSubnetGroupDescription());
        final SubnetIdentifierList subnetIdentifierList = new SubnetIdentifierList();
        subnetIdentifierList.setMember(action.properties.getSubnetIds());
        createDBSubnetGroup.setSubnetIds(subnetIdentifierList);
        // createDBSubnetGroup.setTags();
        rds.createDBSubnetGroup(createDBSubnetGroup);
        action.info.setPhysicalResourceId(action.properties.getDBSubnetGroupName());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    }
    // TODO system tags via AddTagsToResource
  }

  private enum DeleteSteps implements Step {
    REMOVE_DBSUBNETGROUP {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSRDSDBSubnetGroupResourceAction action = (AWSRDSDBSubnetGroupResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final RdsApi rds = AsyncProxy.client( RdsApi.class, Function.identity( ) );
        final DeleteDBSubnetGroupType deleteDBSubnetGroup =
            MessageHelper.createMessage(DeleteDBSubnetGroupType.class, action.info.getEffectiveUserId());
        deleteDBSubnetGroup.setDBSubnetGroupName(action.info.getPhysicalResourceId());
        rds.deleteDBSubnetGroup(deleteDBSubnetGroup);
        return action;
      }
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSRDSDBSubnetGroupProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSRDSDBSubnetGroupResourceInfo) resourceInfo;
  }


  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_DBSUBNETGROUP {
      @Override
      public ResourceAction perform(
          final ResourceAction oldResourceAction,
          final ResourceAction newResourceAction
      ) throws Exception {
        final AWSRDSDBSubnetGroupResourceAction oldAction = (AWSRDSDBSubnetGroupResourceAction) oldResourceAction;
        final AWSRDSDBSubnetGroupResourceAction newAction = (AWSRDSDBSubnetGroupResourceAction) newResourceAction;
        //TODO updates ?
        return newAction;
      }
    }
  }
}
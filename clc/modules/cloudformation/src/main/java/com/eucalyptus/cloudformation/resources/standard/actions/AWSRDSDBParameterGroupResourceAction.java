/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSRDSDBParameterGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSRDSDBParameterGroupProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.rds.common.RdsApi;
import com.eucalyptus.rds.common.msgs.CreateDBParameterGroupResponseType;
import com.eucalyptus.rds.common.msgs.CreateDBParameterGroupType;
import com.eucalyptus.rds.common.msgs.DeleteDBParameterGroupType;
import com.eucalyptus.rds.common.msgs.ModifyDBParameterGroupType;
import com.eucalyptus.rds.common.msgs.Parameter;
import com.eucalyptus.rds.common.msgs.ParametersList;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.vavr.collection.Stream;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.eucalyptus.cloudformation.resources.standard.actions.AWSRDSDBInstanceResourceAction.addSystemTags;
import static com.eucalyptus.cloudformation.resources.standard.actions.AWSRDSDBInstanceResourceAction.getTags;
import static com.eucalyptus.cloudformation.resources.standard.actions.AWSRDSDBInstanceResourceAction.toTagList;
import static com.eucalyptus.cloudformation.resources.standard.actions.AWSRDSDBInstanceResourceAction.updateTags;


public class AWSRDSDBParameterGroupResourceAction extends StepBasedResourceAction {

  private AWSRDSDBParameterGroupProperties properties = new AWSRDSDBParameterGroupProperties();
  private AWSRDSDBParameterGroupResourceInfo info = new AWSRDSDBParameterGroupResourceInfo();

  public AWSRDSDBParameterGroupResourceAction() {
    super(
        fromEnum(CreateSteps.class),
        fromEnum(DeleteSteps.class),
        fromUpdateEnum(UpdateNoInterruptionSteps.class),
        null);
  }


  @Override
  public UpdateType getUpdateType(final ResourceAction resourceAction, final boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ?
        UpdateType.NO_INTERRUPTION :
        UpdateType.NONE;
    final AWSRDSDBParameterGroupResourceAction otherAction = (AWSRDSDBParameterGroupResourceAction) resourceAction;
    //TODO updates ?
    //if (!Objects.equals(properties.getPolicyDocument(), otherAction.properties.getPolicyDocument())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    //}
    return updateType;
  }

  private enum CreateSteps implements Step {
    CREATE_DBPARAMETERGROUP {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSRDSDBParameterGroupResourceAction action = (AWSRDSDBParameterGroupResourceAction) resourceAction;
        String parameterGroupName = action.getDefaultPhysicalResourceId(255).toLowerCase();
        final RdsApi rds = AsyncProxy.client( RdsApi.class, Function.identity( ) );
        final CreateDBParameterGroupType createDBParameterGroup =
            MessageHelper.createMessage(CreateDBParameterGroupType.class, action.info.getEffectiveUserId());
        createDBParameterGroup.setDBParameterGroupName(parameterGroupName);
        createDBParameterGroup.setDBParameterGroupFamily(action.properties.getFamily());
        createDBParameterGroup.setDescription(action.properties.getDescription());
        final Set<CloudFormationResourceTag> tags = getTags(action, action.properties.getTags());
        if (!tags.isEmpty()) {
          createDBParameterGroup.setTags(toTagList(tags));
        }
        final CreateDBParameterGroupResponseType response = rds.createDBParameterGroup(createDBParameterGroup);
        action.info.setPhysicalResourceId(parameterGroupName);
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        if (response.getCreateDBParameterGroupResult() != null &&
            response.getCreateDBParameterGroupResult().getDBParameterGroup() != null &&
            response.getCreateDBParameterGroupResult().getDBParameterGroup().getDBParameterGroupArn() != null) {
          action.info.setArn(response.getCreateDBParameterGroupResult().getDBParameterGroup().getDBParameterGroupArn());
        }
        return action;
      }
    },
    SET_DBPARAMETERS {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSRDSDBParameterGroupResourceAction action =
            (AWSRDSDBParameterGroupResourceAction) resourceAction;
        if (action.properties.getParameters() != null) {
          if (!action.properties.getParameters().isObject()) {
            throw new ValidationErrorException(
                "Invalid Parameters value " + action.properties.getParameters());
          }

          final Map<String, String> parameterMap = Maps.newLinkedHashMap();
          for (final String paramName :
              Lists.newArrayList(action.properties.getParameters().fieldNames())) {
            final JsonNode paramValue = action.properties.getParameters().get(paramName);
            if (!paramValue.isValueNode()) {
              throw new ValidationErrorException("All Parameters must have String values");
            }
            parameterMap.put(paramName, paramValue.asText());
          }

          if (!parameterMap.isEmpty()) {
            final RdsApi rds = AsyncProxy.client(RdsApi.class, Function.identity());
            final ModifyDBParameterGroupType modifyDBParameterGroup =
                MessageHelper.createMessage(ModifyDBParameterGroupType.class,
                    action.info.getEffectiveUserId());
            modifyDBParameterGroup.setDBParameterGroupName(action.info.getPhysicalResourceId());
            final ParametersList parametersList = new ParametersList();
            parametersList.getMember().addAll(Stream.ofAll(parameterMap.entrySet()).map(kvp -> {
              final Parameter parameter = new Parameter();
              parameter.setParameterName(kvp.getKey());
              parameter.setParameterValue(kvp.getValue());
              parameter.setApplyMethod("pending-reboot");
              return parameter;
            }).toJavaList());
            modifyDBParameterGroup.setParameters(parametersList);
            rds.modifyDBParameterGroup(modifyDBParameterGroup);
          }
        }
        return action;
      }
    },
    ADD_SYSTEM_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) {
        final AWSRDSDBParameterGroupResourceAction action = (AWSRDSDBParameterGroupResourceAction) resourceAction;
        addSystemTags(action.info.getArn(), action);
        return action;
      }
    },
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_DBPARAMETERGROUP {
      @Override
      public ResourceAction perform(
          final ResourceAction oldResourceAction,
          final ResourceAction newResourceAction
      ) {
        final AWSRDSDBParameterGroupResourceAction oldAction = (AWSRDSDBParameterGroupResourceAction) oldResourceAction;
        final AWSRDSDBParameterGroupResourceAction newAction = (AWSRDSDBParameterGroupResourceAction) newResourceAction;
        //TODO updates ?
        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        final AWSRDSDBParameterGroupResourceAction oldAction = (AWSRDSDBParameterGroupResourceAction) oldResourceAction;
        final AWSRDSDBParameterGroupResourceAction newAction = (AWSRDSDBParameterGroupResourceAction) newResourceAction;
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
    REMOVE_DBPARAMETERGROUP {
      @Override
      public ResourceAction perform(final ResourceAction resourceAction) throws Exception {
        final AWSRDSDBParameterGroupResourceAction action = (AWSRDSDBParameterGroupResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final RdsApi rds = AsyncProxy.client( RdsApi.class, Function.identity( ) );
        final DeleteDBParameterGroupType deleteDBParameterGroup =
            MessageHelper.createMessage(DeleteDBParameterGroupType.class, action.info.getEffectiveUserId());
        deleteDBParameterGroup.setDBParameterGroupName(action.info.getPhysicalResourceId());
        rds.deleteDBParameterGroup(deleteDBParameterGroup);
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
    properties = (AWSRDSDBParameterGroupProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSRDSDBParameterGroupResourceInfo) resourceInfo;
  }
}
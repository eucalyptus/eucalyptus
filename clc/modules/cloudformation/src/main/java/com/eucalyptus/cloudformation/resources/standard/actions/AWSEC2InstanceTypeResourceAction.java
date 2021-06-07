/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InstanceTypeResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSEC2InstanceTypeProperties;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.compute.common.ComputeApi;
import com.eucalyptus.compute.common.ModifyInstanceTypeAttributeType;
import com.eucalyptus.util.async.AsyncProxy;


/**
 *
 */
public class AWSEC2InstanceTypeResourceAction extends StepBasedResourceAction {

  private AWSEC2InstanceTypeProperties properties = new AWSEC2InstanceTypeProperties();
  private AWSEC2InstanceTypeResourceInfo info = new AWSEC2InstanceTypeResourceInfo();

  public AWSEC2InstanceTypeResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    return UpdateType.NO_INTERRUPTION;
  }

  private enum CreateSteps implements Step {
    CREATE_OR_UPDATE_ATTRIBUTES {
      @Override
      public ResourceAction perform( ResourceAction resourceAction ) {
        final AWSEC2InstanceTypeResourceAction action = (AWSEC2InstanceTypeResourceAction) resourceAction;
        final ComputeApi computeApi = AsyncProxy.client( ComputeApi.class, request -> {request.setEffectiveUserId(action.info.getEffectiveUserId()); return request;} );
        final ModifyInstanceTypeAttributeType modifyInstanceType = new ModifyInstanceTypeAttributeType( );
        modifyInstanceType.setName(action.properties.getName());
        modifyInstanceType.setCpu(action.properties.getCpu());
        modifyInstanceType.setDisk(action.properties.getDisk());
        modifyInstanceType.setDiskCount(action.properties.getDiskCount());
        modifyInstanceType.setEnabled(action.properties.getEnabled());
        modifyInstanceType.setMemory(action.properties.getMemory());
        modifyInstanceType.setNetworkInterfaces(action.properties.getNetworkInterfaces());
        computeApi.modifyInstanceType( modifyInstanceType );
        action.info.setPhysicalResourceId(action.properties.getName());
        action.info.setCreatedEnoughToDelete( true );
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    NOOP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) {
        return resourceAction;
      }
    },
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_ATTRIBUTES {
      @Override
      public ResourceAction perform(
          final ResourceAction oldResourceAction,
          final ResourceAction newResourceAction
      ) {
        final AWSEC2InstanceTypeResourceAction newAction = (AWSEC2InstanceTypeResourceAction) newResourceAction;
        final ComputeApi computeApi = AsyncProxy.client( ComputeApi.class, request -> {request.setEffectiveUserId(newAction.info.getEffectiveUserId()); return request;}  );
        final ModifyInstanceTypeAttributeType modifyInstanceType = new ModifyInstanceTypeAttributeType( );
        modifyInstanceType.setName(newAction.properties.getName( ));
        modifyInstanceType.setCpu(newAction.properties.getCpu());
        modifyInstanceType.setDisk(newAction.properties.getDisk());
        modifyInstanceType.setDiskCount(newAction.properties.getDiskCount());
        modifyInstanceType.setEnabled(newAction.properties.getEnabled());
        modifyInstanceType.setMemory(newAction.properties.getMemory());
        modifyInstanceType.setNetworkInterfaces(newAction.properties.getNetworkInterfaces());
        computeApi.modifyInstanceType( modifyInstanceType );
        return newAction;
      }
    },
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSEC2InstanceTypeProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSEC2InstanceTypeResourceInfo) resourceInfo;
  }
}

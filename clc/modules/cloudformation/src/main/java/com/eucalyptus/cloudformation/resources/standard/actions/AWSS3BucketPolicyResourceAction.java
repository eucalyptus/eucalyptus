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


import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSS3BucketPolicyResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSS3BucketPolicyProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSS3BucketPolicyResourceAction extends StepBasedResourceAction {

  private AWSS3BucketPolicyProperties properties = new AWSS3BucketPolicyProperties();
  private AWSS3BucketPolicyResourceInfo info = new AWSS3BucketPolicyResourceInfo();

  public AWSS3BucketPolicyResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), fromUpdateEnum(UpdateNoInterruptionSteps.class), null);
  }


  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    AWSS3BucketPolicyResourceAction otherAction = (AWSS3BucketPolicyResourceAction) resourceAction;
    if (!Objects.equals(properties.getBucket(), otherAction.properties.getBucket())) {
      // TODO: AWS actually reports the error here as: Bucket property is not updatable .. consider how to do that
      updateType = UpdateType.max(updateType, UpdateType.UNSUPPORTED);
    }
    if (!Objects.equals(properties.getPolicyDocument(), otherAction.properties.getPolicyDocument())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private enum CreateSteps implements Step {
    SET_POLICY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSS3BucketPolicyResourceAction action = (AWSS3BucketPolicyResourceAction) resourceAction;
        User user = Accounts.lookupPrincipalByUserId(action.getResourceInfo().getEffectiveUserId());
        try ( final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client( new SecurityTokenAWSCredentialsProvider( user ) ) ) {
          s3c.setBucketPolicy(action.properties.getBucket(), action.properties.getPolicyDocument().toString());
        }
        action.info.setPhysicalResourceId(action.getDefaultPhysicalResourceId());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    REMOVE_POLICY {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSS3BucketPolicyResourceAction action = (AWSS3BucketPolicyResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        User user = Accounts.lookupPrincipalByUserId(action.getResourceInfo().getEffectiveUserId());
        try ( final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client( new SecurityTokenAWSCredentialsProvider( user ) ) ) {
          s3c.deleteBucketPolicy(action.properties.getBucket());
        } catch (AmazonS3Exception ex) {
          if ("NoSuchBucket".equalsIgnoreCase(ex.getErrorCode())) {
            // do nothing.  (We check existence this way rather than if -> delete due to possible race conditions
          } else {
            throw ex;
          }
        }
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
    properties = (AWSS3BucketPolicyProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSS3BucketPolicyResourceInfo) resourceInfo;
  }


  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_POLICY {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        AWSS3BucketPolicyResourceAction oldAction = (AWSS3BucketPolicyResourceAction) oldResourceAction;
        AWSS3BucketPolicyResourceAction newAction = (AWSS3BucketPolicyResourceAction) newResourceAction;
        User user = Accounts.lookupPrincipalByUserId(newAction.getResourceInfo().getEffectiveUserId());
        try ( final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client( new SecurityTokenAWSCredentialsProvider( user ) ) ) {
          s3c.setBucketPolicy(newAction.properties.getBucket(), newAction.properties.getPolicyDocument().toString());
        }
        return newAction;
      }
    }
  }
}




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


import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationWaitConditionHandleResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationWaitConditionHandleProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.net.URI;
import java.util.Date;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudFormationWaitConditionHandleResourceAction extends ResourceAction {

  private AWSCloudFormationWaitConditionHandleProperties properties = new AWSCloudFormationWaitConditionHandleProperties();
  private AWSCloudFormationWaitConditionHandleResourceInfo info = new AWSCloudFormationWaitConditionHandleResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSCloudFormationWaitConditionHandleProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSCloudFormationWaitConditionHandleResourceInfo) resourceInfo;
  }


  @Override
  public int getNumCreateSteps() {
    return 2;
  }
  private static final int NUM_ATTEMPTS = 1000;
  @Override
  public void create(int stepNum) throws Exception {
    URI serviceURI = ServiceUris.remotePublicify(ObjectStorage.class);
    User user = Accounts.lookupUserById(getResourceInfo().getEffectiveUserId());
    final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(user);
    String bucketName = null;
    ObjectNode objectNode = null;
    switch (stepNum) {
      case 0:
        for (int i=0;i<NUM_ATTEMPTS;i++) {
          bucketName = "cf-waitcondition-" + user.getUserId() + "-" + Crypto.generateAlphanumericId(13, "");
          bucketName = bucketName.toLowerCase();
          if (!s3c.doesBucketExist(bucketName)) {
            break;
          }
        }
        s3c.createBucket(bucketName);
        String keyName = getStackEntity().getStackId() + "/" + info.getLogicalResourceId() + "/WaitHandle";
        ObjectMapper mapper = new ObjectMapper();
        objectNode = mapper.createObjectNode();
        objectNode.put("version","1.0");
        objectNode.put("bucket", bucketName);
        objectNode.put("key", keyName);
        info.setEucaParts(JsonHelper.getStringFromJsonNode(objectNode));
        String url = s3c.generatePresignedUrl(bucketName, keyName, in12Hours(), HttpMethod.PUT).toString();
        info.setPhysicalResourceId(url);
        break;
      case 1:
        objectNode = (ObjectNode) JsonHelper.getJsonNodeFromString(info.getEucaParts());
        if (!"1.0".equals(objectNode.get("version").textValue())) throw new Exception("Invalid version for eucaParts");
        bucketName = objectNode.get("bucket").textValue();
        s3c.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(bucketName, new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)));
        info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
        break;
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
  }

  private Date in12Hours() {
    return new Date(System.currentTimeMillis() + 12 * 60 * 60 * 1000L); // max timeout for handle
  }

  @Override
  public void update(int stepNum) throws Exception {
    throw new UnsupportedOperationException();
  }

  public void rollbackUpdate() throws Exception {
    // can't update so rollbackUpdate should be a NOOP
  }

  @Override
  public void delete() throws Exception {
    if (info.getPhysicalResourceId() == null) return;
    URI serviceURI = ServiceUris.remotePublicify(ObjectStorage.class);
    User user = Accounts.lookupUserById(getResourceInfo().getEffectiveUserId());
    final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(user);
    ObjectNode objectNode = (ObjectNode) JsonHelper.getJsonNodeFromString(info.getEucaParts());
    if (!"1.0".equals(objectNode.get("version").textValue())) throw new Exception("Invalid version for eucaParts");
    String bucketName = objectNode.get("bucket").textValue();
    if (!s3c.doesBucketExist(bucketName)) {
      return;
    }
    VersionListing versionListing = s3c.listVersions(bucketName,"");
    for (S3VersionSummary versionSummary: versionListing.getVersionSummaries()) {
      System.out.println(versionSummary.getBucketName() + " " + versionSummary.getKey() + " " + versionSummary.getVersionId());
      s3c.deleteVersion(versionSummary.getBucketName(), versionSummary.getKey(), versionSummary.getVersionId());
    }
    s3c.deleteBucket(bucketName);
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}



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


import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationWaitConditionHandleResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationWaitConditionResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationWaitConditionProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 2/3/14.
 */
public class AWSCloudFormationWaitConditionResourceAction extends ResourceAction {
  private static final Logger LOG = Logger.getLogger(AWSCloudFormationWaitConditionResourceAction.class);
  private AWSCloudFormationWaitConditionProperties properties = new AWSCloudFormationWaitConditionProperties();
  private AWSCloudFormationWaitConditionResourceInfo info = new AWSCloudFormationWaitConditionResourceInfo();
  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSCloudFormationWaitConditionProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSCloudFormationWaitConditionResourceInfo) resourceInfo;
  }

  @Override
  public void create(int stepNum) throws Exception {
    LOG.trace("Creating wait condition");
    switch (stepNum) {
      case 0:
        int numSignals = properties.getCount() != null && properties.getCount() > 0 ? properties.getCount() : 1;
        LOG.trace("num signals = " + numSignals);
        if (properties.getTimeout() > 43200) {
          throw new Exception("timeout can not be more than 43200");
        }
        LOG.trace("Timeout = " + properties.getTimeout());
        try {
          // TODO: deal with oddball cases...
          LOG.trace("Looking for handle : " + properties.getHandle());
          List<StackResourceEntity> stackResourceEntityList = StackResourceEntityManager.getStackResources(getStackEntity().getStackId(), info.getAccountId());
          StackResourceEntity handleEntity = null;
          for (StackResourceEntity stackResourceEntity: stackResourceEntityList) {
            if (stackResourceEntity.getPhysicalResourceId() != null && stackResourceEntity.getPhysicalResourceId().equals(properties.getHandle())) {
              LOG.trace("found something with the same physical id, type:" + stackResourceEntity.getResourceType());
              if (stackResourceEntity.getResourceType().equals("AWS::CloudFormation::WaitConditionHandle")) {
                handleEntity = stackResourceEntity;
                break;
              }
            }
          }
          if (handleEntity == null) {
            throw new Exception("Handle URL:" + properties.getHandle() + " does not match a WaitConditionHandle from this stack");
          }
          AWSCloudFormationWaitConditionHandleResourceInfo handleResourceInfo = (AWSCloudFormationWaitConditionHandleResourceInfo) StackResourceEntityManager.getResourceInfo(handleEntity);
          ObjectNode objectNode = (ObjectNode) JsonHelper.getJsonNodeFromString(handleResourceInfo.getEucaParts());
          if (!"1.0".equals(objectNode.get("version").textValue())) throw new Exception("Invalid version for eucaParts");
          String bucketName = objectNode.get("bucket").textValue();
          LOG.trace("bucketName=" + bucketName);
          String keyName = objectNode.get("key").textValue();
          LOG.trace("keyName=" + bucketName);
          LOG.trace("Starting to poll");
          URI serviceURI = ServiceUris.remotePublicify(ObjectStorage.class);
          User user = Accounts.lookupUserById(getResourceInfo().getEffectiveUserId());
          final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(user);
          long startTime = System.currentTimeMillis();

          long currentTime = System.currentTimeMillis();

          do {
            boolean foundFailure = false;
            LOG.trace("Handle:" + properties.getHandle());
            VersionListing versionListing = s3c.listVersions(bucketName,keyName);
            LOG.trace("Found " + versionListing.getVersionSummaries() + " versions to check");
            Map<String, String> dataMap = Maps.newHashMap();
            for (S3VersionSummary versionSummary: versionListing.getVersionSummaries()) {
              LOG.trace("Key:" + versionSummary.getKey());
              if (!versionSummary.getKey().equals(keyName)) {
                LOG.trace("Wrong key (probably key is a prefix).  Skipping");
                return;
              }
              LOG.trace("Getting version: " + versionSummary.getVersionId());
              try {
                GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, keyName, versionSummary.getVersionId());
                S3Object s3Object = s3c.getObject(getObjectRequest);
                JsonNode jsonNode = null;
                try (S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent()) {
                  jsonNode = new ObjectMapper().readTree(s3ObjectInputStream);
                }
                if (!jsonNode.isObject()) {
                  LOG.trace("Read object, json but not object..skipping file");
                  continue;
                }
                ObjectNode localObjectNode = (ObjectNode) jsonNode;
                String status = localObjectNode.get("Status").textValue();
                if (status == null) {
                  LOG.trace("Null status, skipping");
                  continue;
                }
                String data = localObjectNode.get("Data").textValue();
                if (data == null) {
                  LOG.trace("Null data, skipping");
                  continue;
                }
                String uniqueId = localObjectNode.get("UniqueId").textValue();
                if (data == null) {
                  LOG.trace("Null uniqueId, skipping");
                  continue;
                }
                if ("FAILURE".equals(status)) {
                  foundFailure = true;
                  LOG.trace("found failure, gonna die");
                  break;
                } else if (!"SUCCESS".equals(status)) {
                  LOG.trace("weird status...skipping");
                  continue;
                } else {
                  LOG.trace("found success, uniqueId="+uniqueId);
                  dataMap.put(uniqueId, data);
                }
              } catch (Exception ex) {
                LOG.error(ex, ex);
                LOG.trace("Exception while going through the objects, will skip this one.");
              }
            }
            if (foundFailure) {
              throw new Exception("Found failure signal");
            }
            LOG.trace("Have " + dataMap.size() + " success signals, need " + numSignals);
            if (dataMap.size() >= numSignals) {
              LOG.trace("Success");
              ObjectNode dataNode = new ObjectMapper().createObjectNode();
              for (String uniqueId: dataMap.keySet()) {
                dataNode.put(uniqueId, dataMap.get(uniqueId));
              }
              info.setData(JsonHelper.getStringFromJsonNode(dataNode));
              info.setPhysicalResourceId(keyName);
              info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(info.getPhysicalResourceId())));
              return;
            } else {
              LOG.trace("Will sleep and try again");
              Thread.sleep(5000L);
            }
            LOG.trace("Time to wait: " + (properties.getTimeout() * 1000L));
            currentTime = System.currentTimeMillis();
            LOG.trace("Time elapsed: " + (currentTime - startTime));
          } while ((currentTime - startTime) <= 1000L * properties.getTimeout());
          throw new Exception("Timeout while waiting for signals");
        } catch (Exception ex) {
          LOG.error(ex, ex);
          throw ex;
        }
      default:
        throw new IllegalStateException("Invalid step " + stepNum);
    }
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
    // nothing to really do...(the handle deletes the bucket for now)
  }

  @Override
  public void rollbackCreate() throws Exception {
    delete();
  }

}



/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.VersionListing;
import com.eucalyptus.cloudformation.bootstrap.CloudFormationAWSCredentialsProvider;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSCloudFormationWaitConditionHandleResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSCloudFormationWaitConditionHandleProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 2/3/14.
 */
@ConfigurableClass( root = "cloudformation", description = "Parameters controlling cloud formation")
public class AWSCloudFormationWaitConditionHandleResourceAction extends StepBasedResourceAction {

  @ConfigurableField(initial = "cf-waitcondition", description = "The prefix of the bucket used for wait condition handles")
  public static volatile String WAIT_CONDITION_BUCKET_PREFIX = "cf-waitcondition";
  private AWSCloudFormationWaitConditionHandleProperties properties = new AWSCloudFormationWaitConditionHandleProperties();
  private AWSCloudFormationWaitConditionHandleResourceInfo info = new AWSCloudFormationWaitConditionHandleResourceInfo();
  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    return UpdateType.UNSUPPORTED;
  }

  public AWSCloudFormationWaitConditionHandleResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null);
  }

  private enum CreateSteps implements Step {
    GET_BUCKET_AND_CREATE_OBJECT {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSCloudFormationWaitConditionHandleResourceAction action = (AWSCloudFormationWaitConditionHandleResourceAction) resourceAction;
        try ( final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client( new CloudFormationAWSCredentialsProvider( (int)TimeUnit.HOURS.toSeconds( 12 ) ) ) ) {
          // look for an existing bucket...
          String bucketName = null;
          ObjectNode objectNode = null;
          List<Bucket> buckets = s3c.listBuckets();
          for ( Bucket bucket : buckets ) {
            if ( bucket.getName().toLowerCase().startsWith( WAIT_CONDITION_BUCKET_PREFIX.toLowerCase() ) ) {
              // TODO: what happens if versioning is off?
              bucketName = bucket.getName();
            }
          }
          if ( bucketName == null ) {
            // TODO: check prefix length
            bucketName = ( WAIT_CONDITION_BUCKET_PREFIX + "-" + Crypto.generateAlphanumericId( 13 ) ).toLowerCase();
          }
          s3c.createBucket( bucketName );
          String keyName = action.getStackEntity().getStackId() + "/" + action.info.getLogicalResourceId() + "/WaitHandle";
          ObjectMapper mapper = new ObjectMapper();
          objectNode = mapper.createObjectNode();
          objectNode.put( "version", "1.0" );
          objectNode.put( "bucket", bucketName );
          objectNode.put( "key", keyName );
          action.info.setEucaParts( JsonHelper.getStringFromJsonNode( objectNode ) );
          s3c.refreshEndpoint( true );
          String url = s3c.generatePresignedUrl( bucketName, keyName, action.in12Hours(), HttpMethod.PUT ).toString();
          action.info.setPhysicalResourceId( url );
          action.info.setCreatedEnoughToDelete(true);
        }
        return action;
      }
    },
    VERSION_BUCKET {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationWaitConditionHandleResourceAction action = (AWSCloudFormationWaitConditionHandleResourceAction) resourceAction;
        try ( final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(new CloudFormationAWSCredentialsProvider()) ) {
          ObjectNode objectNode = (ObjectNode) JsonHelper.getJsonNodeFromString(action.info.getEucaParts());
          if (!"1.0".equals(objectNode.get("version").asText())) throw new Exception("Invalid version for eucaParts");
          String bucketName = objectNode.get("bucket").asText();
          s3c.setBucketVersioningConfiguration(new SetBucketVersioningConfigurationRequest(bucketName, new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED)));
        }
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    };

    @Nullable
    @Override
    public Integer getTimeout() {
      return null;
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_VERSIONS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        AWSCloudFormationWaitConditionHandleResourceAction action = (AWSCloudFormationWaitConditionHandleResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        try ( final EucaS3Client s3c = EucaS3ClientFactory.getEucaS3Client(new CloudFormationAWSCredentialsProvider()) ) {
          ObjectNode objectNode = (ObjectNode) JsonHelper.getJsonNodeFromString(action.info.getEucaParts());
          if (!"1.0".equals(objectNode.get("version").asText())) throw new Exception("Invalid version for eucaParts");
          String bucketName = objectNode.get("bucket").asText();
          String keyName = objectNode.get("key").asText();
          if (!s3c.doesBucketExist(bucketName)) {
            return action;
          }
          VersionListing versionListing = s3c.listVersions(bucketName,"");
          // delete all items under the buckey/key
          for (S3VersionSummary versionSummary: versionListing.getVersionSummaries()) {
            if (versionSummary.getKey().equals(keyName)) {
              s3c.deleteVersion(versionSummary.getBucketName(), versionSummary.getKey(), versionSummary.getVersionId());
            }
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

  private Date in12Hours() {
    return new Date(System.currentTimeMillis() + 12 * 60 * 60 * 1000L); // max timeout for handle
  }



}



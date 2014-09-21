package com.eucalyptus.cloudformation.resources.standard;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.Tag;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.entity.StackEntityHelper;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InternetGatewayResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by ethomas on 9/21/14.
 */
public class TagHelper {

  public static List<CloudFormationResourceTag> getCloudFormationResourceStackTags(ResourceInfo resourceInfo, StackEntity stackEntity) throws CloudFormationException {
    List<CloudFormationResourceTag> tags = Lists.newArrayList();

    CloudFormationResourceTag logicalIdTag = new CloudFormationResourceTag();
    logicalIdTag.setKey("aws:cloudformation:logical-id");
    logicalIdTag.setValue(resourceInfo.getLogicalResourceId());
    tags.add(logicalIdTag);

    CloudFormationResourceTag stackIdTag = new CloudFormationResourceTag();
    stackIdTag.setKey("aws:cloudformation:stack-id");
    stackIdTag.setValue(stackEntity.getStackId());
    tags.add(stackIdTag);

    CloudFormationResourceTag stackNameTag = new CloudFormationResourceTag();
    stackNameTag.setKey("aws:cloudformation:stack-name");
    stackNameTag.setValue(stackEntity.getStackName());
    tags.add(stackNameTag);

    if (stackEntity.getTagsJson() != null) {
      List<Tag> stackTags = StackEntityHelper.jsonToTags(stackEntity.getTagsJson());
      for (Tag stackTag : stackTags) {
        CloudFormationResourceTag cloudFormationResourceTag = new CloudFormationResourceTag();
        cloudFormationResourceTag.setKey(stackTag.getKey());
        cloudFormationResourceTag.setValue(stackTag.getValue());
        tags.add(cloudFormationResourceTag);
      }
    }
    return tags;
  }

  public static List<EC2Tag> getEC2StackTags(ResourceInfo info, StackEntity stackEntity) throws CloudFormationException {
    // TOO many tags
    List<EC2Tag> tags = Lists.newArrayList();
    for (CloudFormationResourceTag otherTag : getCloudFormationResourceStackTags(info, stackEntity)) {
      EC2Tag cloudFormationResourceTag = new EC2Tag();
      cloudFormationResourceTag.setKey(otherTag.getKey());
      cloudFormationResourceTag.setValue(otherTag.getValue());
      tags.add(cloudFormationResourceTag);
    }
    return tags;
  }
}

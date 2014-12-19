/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.resources.standard;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.Tag;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.StackEntity;
import com.eucalyptus.cloudformation.entity.StackEntityHelper;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingAutoScalingGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.info.AWSEC2InternetGatewayResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.util.Strings;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
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
      EC2Tag tag = new EC2Tag();
      tag.setKey(otherTag.getKey());
      tag.setValue(otherTag.getValue());
      tags.add(tag);
    }
    return tags;
  }

  public static List<AutoScalingTag> getAutoScalingStackTags(AWSAutoScalingAutoScalingGroupResourceInfo info, StackEntity stackEntity) throws CloudFormationException {
    // TOO many tags
    List<AutoScalingTag> tags = Lists.newArrayList();
    for (CloudFormationResourceTag otherTag : getCloudFormationResourceStackTags(info, stackEntity)) {
      AutoScalingTag tag = new AutoScalingTag();
      tag.setKey(otherTag.getKey());
      tag.setValue(otherTag.getValue());
      tag.setPropagateAtLaunch(true); // TODO: verify this
      tags.add(tag);
    }
    return tags;
  }

  private static List<String> reservedPrefixes = Lists.newArrayList("euca:","aws:");
  public static void checkReservedAutoScalingTemplateTags(List<AutoScalingTag> tags) throws ValidationErrorException {
    if (tags == null) return;
    List<String> tagNames = Lists.newArrayList();
    for (AutoScalingTag tag: tags) {
      if (Iterables.any(reservedPrefixes, Strings.isPrefixOf(tag.getKey()))) {
        throw new ValidationErrorException("Tag " + tag.getKey() + " uses a reserved prefix " + reservedPrefixes);
      }
    }
  }

  public static void checkReservedCloudFormationResourceTemplateTags(List<CloudFormationResourceTag> tags) throws ValidationErrorException {
    if (tags == null) return;
    List<String> tagNames = Lists.newArrayList();
    for (CloudFormationResourceTag tag: tags) {
      if (Iterables.any(reservedPrefixes, Strings.isPrefixOf(tag.getKey()))) {
        throw new ValidationErrorException("Tag " + tag.getKey() + " uses a reserved prefix " + reservedPrefixes);
      }
    }
  }

  public static void checkReservedEC2TemplateTags(List<EC2Tag> tags) throws ValidationErrorException {
    if (tags == null) return;
    List<String> tagNames = Lists.newArrayList();
    for (EC2Tag tag: tags) {
      if (Iterables.any(reservedPrefixes, Strings.isPrefixOf(tag.getKey()))) {
        throw new ValidationErrorException("Tag " + tag.getKey() + " uses a reserved prefix " + reservedPrefixes);
      }
    }
  }
}

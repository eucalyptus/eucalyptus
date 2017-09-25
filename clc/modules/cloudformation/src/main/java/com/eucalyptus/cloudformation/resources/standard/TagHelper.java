/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation.resources.standard;

import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.common.msgs.Tag;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.entity.StackEntityHelper;
import com.eucalyptus.cloudformation.entity.VersionedStackEntity;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingAutoScalingGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AutoScalingTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.loadbalancing.common.msgs.TagKeyList;
import com.eucalyptus.loadbalancing.common.msgs.TagList;
import com.eucalyptus.util.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ethomas on 9/21/14.
 */
public class TagHelper {

  public static List<CloudFormationResourceTag> getCloudFormationResourceSystemTags(ResourceInfo resourceInfo, VersionedStackEntity stackEntity) throws CloudFormationException {
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
    return tags;
  }

  public static boolean stackTagsEquals(VersionedStackEntity stackEntity1, VersionedStackEntity stackEntity2) throws CloudFormationException {
    if (stackEntity1 == null && stackEntity2 == null) return true;
    if (stackEntity1 != null && stackEntity2 == null) return false;
    if (stackEntity1 == null && stackEntity2 != null) return false;
    Map<String, String> tag1Map = Maps.newHashMap();
    Map<String, String> tag2Map = Maps.newHashMap();
    for (Tag tag: StackEntityHelper.jsonToTags(stackEntity1.getTagsJson())) {
      tag1Map.put(tag.getKey(), tag.getValue());
    }
    for (Tag tag: StackEntityHelper.jsonToTags(stackEntity2.getTagsJson())) {
      tag2Map.put(tag.getKey(), tag.getValue());
    }
    return tag1Map.equals(tag2Map);
  }

  public static List<CloudFormationResourceTag> getCloudFormationResourceStackTags(VersionedStackEntity stackEntity) throws CloudFormationException {
    List<CloudFormationResourceTag> tags = Lists.newArrayList();
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

  public static List<EC2Tag> getEC2StackTags(VersionedStackEntity stackEntity) throws CloudFormationException {
    // TOO many tags
    List<EC2Tag> tags = Lists.newArrayList();
    for (CloudFormationResourceTag otherTag : getCloudFormationResourceStackTags(stackEntity)) {
      EC2Tag tag = new EC2Tag();
      tag.setKey(otherTag.getKey());
      tag.setValue(otherTag.getValue());
      tags.add(tag);
    }
    return tags;
  }

  public static List<EC2Tag> getEC2SystemTags(ResourceInfo info, VersionedStackEntity stackEntity) throws CloudFormationException {
    // TOO many tags
    List<EC2Tag> tags = Lists.newArrayList();
    for (CloudFormationResourceTag otherTag : getCloudFormationResourceSystemTags(info, stackEntity)) {
      EC2Tag tag = new EC2Tag();
      tag.setKey(otherTag.getKey());
      tag.setValue(otherTag.getValue());
      tags.add(tag);
    }
    return tags;
  }

  public static List<AutoScalingTag> getAutoScalingStackTags(VersionedStackEntity stackEntity) throws CloudFormationException {
    // TOO many tags
    List<AutoScalingTag> tags = Lists.newArrayList();
    for (CloudFormationResourceTag otherTag : getCloudFormationResourceStackTags(stackEntity)) {
      AutoScalingTag tag = new AutoScalingTag();
      tag.setKey(otherTag.getKey());
      tag.setValue(otherTag.getValue());
      tag.setPropagateAtLaunch(true); // TODO: verify this
      tags.add(tag);
    }
    return tags;
  }

  public static List<AutoScalingTag> getAutoScalingSystemTags(AWSAutoScalingAutoScalingGroupResourceInfo info, VersionedStackEntity stackEntity) throws CloudFormationException {
    // TOO many tags
    List<AutoScalingTag> tags = Lists.newArrayList();
    for (CloudFormationResourceTag otherTag : getCloudFormationResourceSystemTags(info, stackEntity)) {
      AutoScalingTag tag = new AutoScalingTag();
      tag.setKey(otherTag.getKey());
      tag.setValue(otherTag.getValue());
      tag.setPropagateAtLaunch(true); // TODO: verify this
      tags.add(tag);
    }
    return tags;
  }

  private static List<String> reservedPrefixes = Lists.newArrayList("euca:","aws:");
  public static void checkReservedAutoScalingTemplateTags(Collection<AutoScalingTag> tags) throws ValidationErrorException {
    if (tags == null) return;
    List<String> tagNames = Lists.newArrayList();
    for (AutoScalingTag tag: tags) {
      if (Iterables.any(reservedPrefixes, Strings.isPrefixOf(tag.getKey()))) {
        throw new ValidationErrorException("Tag " + tag.getKey() + " uses a reserved prefix " + reservedPrefixes);
      }
    }
  }

  public static void checkReservedCloudFormationResourceTemplateTags(Collection<CloudFormationResourceTag> tags) throws ValidationErrorException {
    if (tags == null) return;
    List<String> tagNames = Lists.newArrayList();
    for (CloudFormationResourceTag tag: tags) {
      if (Iterables.any(reservedPrefixes, Strings.isPrefixOf(tag.getKey()))) {
        throw new ValidationErrorException("Tag " + tag.getKey() + " uses a reserved prefix " + reservedPrefixes);
      }
    }
  }

  public static void checkReservedEC2TemplateTags(Collection<EC2Tag> tags) throws ValidationErrorException {
    if (tags == null) return;
    List<String> tagNames = Lists.newArrayList();
    for (EC2Tag tag: tags) {
      if (Iterables.any(reservedPrefixes, Strings.isPrefixOf(tag.getKey()))) {
        throw new ValidationErrorException("Tag " + tag.getKey() + " uses a reserved prefix " + reservedPrefixes);
      }
    }
  }

  public static TagList convertToTagList(Collection<CloudFormationResourceTag> cloudFormationResourceTags) {
    TagList tagList = new TagList();
    if (cloudFormationResourceTags != null) {
      for (CloudFormationResourceTag cloudFormationResourceTag: cloudFormationResourceTags) {
        com.eucalyptus.loadbalancing.common.msgs.Tag tag = new com.eucalyptus.loadbalancing.common.msgs.Tag();
        tag.setKey(cloudFormationResourceTag.getKey());
        tag.setValue(cloudFormationResourceTag.getValue());
        tagList.getMember().add(tag);
      }
    }
    return tagList;
  }

  public static TagKeyList convertToTagKeyList(Set<String> tagKeys) {
    TagKeyList tagKeyList = new TagKeyList();
    if (tagKeys != null) {
      for (String tagKey: tagKeys) {
        com.eucalyptus.loadbalancing.common.msgs.TagKeyOnly tagKeyOnly = new com.eucalyptus.loadbalancing.common.msgs.TagKeyOnly();
        tagKeyOnly.setKey(tagKey);
        tagKeyList.getMember().add(tagKeyOnly);
      }
    }
    return tagKeyList;
  }

  public static Set<String> getTagKeyNames(Set<CloudFormationResourceTag> tags) {
    Set<String> tagKeyNames = Sets.newHashSet();
    if (tags != null) {
      for (CloudFormationResourceTag tag: tags) {
        tagKeyNames.add(tag.getKey());
      }
    }
    return tagKeyNames;
  }
}

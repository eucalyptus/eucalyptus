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
package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.cloudformation.entity.StackResourceEntity;
import com.eucalyptus.cloudformation.entity.StackResourceEntityManager;
import com.eucalyptus.cloudformation.entity.VersionedStackEntity;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.compute.common.DeleteResourceTag;
import com.eucalyptus.compute.common.ResourceTag;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by ethomas on 8/30/14.
 */
public class EC2Helper {

  public static ArrayList<ResourceTag> createTagSet(Collection<EC2Tag> tags) {
    ArrayList<ResourceTag> resourceTags = Lists.newArrayList();
    for (EC2Tag tag: tags) {
      ResourceTag resourceTag = new ResourceTag();
      resourceTag.setKey(tag.getKey());
      resourceTag.setValue(tag.getValue());
      resourceTags.add(resourceTag);
    }
    return resourceTags;
  }

  public static void refreshInstanceAttributes(VersionedStackEntity stackEntity, String instanceId, String effectiveUserId, int resourceVersion) throws Exception {
    if (instanceId != null) {
      String stackId = stackEntity.getStackId();
      String accountId = stackEntity.getAccountId();
      StackResourceEntity instanceStackResourceEntity = StackResourceEntityManager.getStackResourceByPhysicalResourceId(stackId, accountId, instanceId, resourceVersion);
      if (instanceStackResourceEntity != null) {
        ResourceInfo instanceResourceInfo = StackResourceEntityManager.getResourceInfo(instanceStackResourceEntity);
        ResourceAction instanceResourceAction = new ResourceResolverManager().resolveResourceAction(instanceResourceInfo.getType());
        instanceResourceAction.setStackEntity(stackEntity);
        instanceResourceInfo.setEffectiveUserId(effectiveUserId);
        instanceResourceAction.setResourceInfo(instanceResourceInfo);
        ResourcePropertyResolver.populateResourceProperties(instanceResourceAction.getResourceProperties(), JsonHelper.getJsonNodeFromString(instanceResourceInfo.getPropertiesJson()), false);
        instanceResourceAction.refreshAttributes();
        instanceStackResourceEntity = StackResourceEntityManager.updateResourceInfo(instanceStackResourceEntity, instanceResourceInfo);
        StackResourceEntityManager.updateStackResource(instanceStackResourceEntity);
      }
    }
  }

  public static ArrayList<DeleteResourceTag> deleteTagSet(Collection<EC2Tag> tagsToRemove) {
    ArrayList<DeleteResourceTag> deleteResourceTags = Lists.newArrayList();
    for (EC2Tag tag: tagsToRemove) {
      DeleteResourceTag resourceTag = new DeleteResourceTag();
      resourceTag.setKey(tag.getKey());
      resourceTag.setValue(tag.getValue());
      deleteResourceTags.add(resourceTag);
    }
    return deleteResourceTags;
  }
}

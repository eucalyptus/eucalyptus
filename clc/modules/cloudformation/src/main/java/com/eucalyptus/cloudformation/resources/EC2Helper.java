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

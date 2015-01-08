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

import com.eucalyptus.cloudformation.resources.standard.propertytypes.EC2Tag;
import com.eucalyptus.compute.common.ResourceTag;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ethomas on 8/30/14.
 */
public class EC2Helper {

  public static ArrayList<ResourceTag> createTagSet(List<EC2Tag> tags) {
    ArrayList<ResourceTag> resourceTags = Lists.newArrayList();
    for (EC2Tag tag: tags) {
      ResourceTag resourceTag = new ResourceTag();
      resourceTag.setKey(tag.getKey());
      resourceTag.setValue(tag.getValue());
      resourceTags.add(resourceTag);
    }
    return resourceTags;
  }


}

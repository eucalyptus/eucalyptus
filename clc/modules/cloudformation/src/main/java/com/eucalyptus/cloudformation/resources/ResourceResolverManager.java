/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
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

import com.eucalyptus.cloudformation.resources.standard.StandardResourceResolver;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;

public class ResourceResolverManager {
  private static final Logger LOG = Logger.getLogger(ResourceResolverManager.class);
  private List<ResourceResolver> resourceResolvers = Lists.newArrayList((ResourceResolver) new StandardResourceResolver());
  public ResourceInfo resolveResourceInfo(String resourceType) {
    for (ResourceResolver resourceResolver: resourceResolvers) {
      ResourceInfo resourceInfo = resourceResolver.resolveResourceInfo(resourceType);
      if (resourceInfo != null) {
        LOG.trace("Resolving resourceInfo " + resourceType + " with " + resourceResolver.getClass().getCanonicalName());
        return resourceInfo;
      }
    }
    return null;
  }

  public ResourceAction resolveResourceAction(String resourceType) {
    for (ResourceResolver resourceResolver: resourceResolvers) {
      ResourceAction resourceAction = resourceResolver.resolveResourceAction(resourceType);
      if (resourceAction != null) {
        LOG.trace("Resolving resourceAction " + resourceType + " with " + resourceResolver.getClass().getCanonicalName());
        return resourceAction;
      }
    }
    return null;
  }
}

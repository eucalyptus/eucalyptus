/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
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

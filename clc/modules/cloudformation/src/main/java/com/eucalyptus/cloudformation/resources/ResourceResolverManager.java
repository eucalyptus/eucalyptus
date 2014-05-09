package com.eucalyptus.cloudformation.resources;

import com.eucalyptus.cloudformation.resources.standard.StandardResourceResolver;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by ethomas on 2/2/14.
 */
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

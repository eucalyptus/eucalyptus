package com.eucalyptus.cloudformation.resources;

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by ethomas on 2/2/14.
 */
public class ResourceResolverManager {
  private static final Logger LOG = Logger.getLogger(ResourceResolverManager.class);
  private List<ResourceResolver> resourceResolvers = Lists.newArrayList((ResourceResolver) new DefaultResourceResolver());
  public Resource resolveResource(String resourceType) {
    for (ResourceResolver resourceResolver: resourceResolvers) {
      Resource resource = resourceResolver.resolveResource(resourceType);
      if (resource != null) {
        LOG.trace("Resolving resource " + resourceType + " with " + resourceResolver.getClass().getCanonicalName());
        return resource;
      }
    }
    return null;
  }
}

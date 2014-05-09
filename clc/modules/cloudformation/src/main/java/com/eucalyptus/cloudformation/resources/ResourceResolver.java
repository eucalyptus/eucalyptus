package com.eucalyptus.cloudformation.resources;

/**
 * Created by ethomas on 2/2/14.
 */
public interface ResourceResolver {

  public ResourceInfo resolveResourceInfo(String resourceType);
  public ResourceAction resolveResourceAction(String resourceType);
}

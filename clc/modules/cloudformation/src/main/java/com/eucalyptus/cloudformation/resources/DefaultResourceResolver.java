package com.eucalyptus.cloudformation.resources;

import org.apache.log4j.Logger;

/**
 * Created by ethomas on 2/2/14.
 */
public class DefaultResourceResolver implements ResourceResolver {
  private static final Logger LOG = Logger.getLogger(DefaultResourceResolver.class);
  @Override
  public Resource resolveResource(String resourceType) {
    String defaultClassLocation = getClass().getPackage().getName() + "." + resourceType.replace(":","");
    try {
      return (Resource) Class.forName(defaultClassLocation).newInstance();
    } catch (ClassNotFoundException ex) {
      LOG.debug("Trying to resolve resource " + resourceType + " could not find class " + defaultClassLocation);
      LOG.debug(ex);
    } catch (InstantiationException | IllegalAccessException ex) {
      LOG.debug("Class " + defaultClassLocation + " does not appear to have a default no-arg constructor so can not be used to resolve resource " + resourceType);
      LOG.debug(ex);
    }
    return null;
  }
}

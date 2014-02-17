package com.eucalyptus.cloudformation.resources.standard;

import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceResolver;
import com.eucalyptus.cloudformation.resources.standard.actions.AWSAutoScalingAutoScalingGroupResourceAction;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingAutoScalingGroupResourceInfo;
import org.apache.log4j.Logger;

/**
 * Created by ethomas on 2/2/14.
 */
public class StandardResourceResolver implements ResourceResolver {
  private static final Logger LOG = Logger.getLogger(StandardResourceResolver.class);
  @Override
  public ResourceInfo resolveResourceInfo(String resourceType) {

    String defaultClassLocation = AWSAutoScalingAutoScalingGroupResourceInfo.class.getPackage().getName() + "." + resourceType.replace(":","") + "ResourceInfo";
    try {
      return (ResourceInfo) Class.forName(defaultClassLocation).newInstance();
    } catch (ClassNotFoundException ex) {
      LOG.debug("Trying to resolve resource info " + resourceType + " could not find class " + defaultClassLocation);
      LOG.debug(ex);
    } catch (InstantiationException | IllegalAccessException ex) {
      LOG.debug("Class " + defaultClassLocation + " does not appear to have a default no-arg constructor so can not be used to resolve resource " + resourceType);
      LOG.debug(ex);
    }
    return null;
  }

  @Override
  public ResourceAction resolveResourceAction(String resourceType) {
    String defaultClassLocation = AWSAutoScalingAutoScalingGroupResourceAction.class.getPackage().getName() + "." + resourceType.replace(":","") + "ResourceAction";
    try {
      return (ResourceAction) Class.forName(defaultClassLocation).newInstance();
    } catch (ClassNotFoundException ex) {
      LOG.debug("Trying to resolve resource info " + resourceType + " could not find class " + defaultClassLocation);
      LOG.debug(ex);
    } catch (InstantiationException | IllegalAccessException ex) {
      LOG.debug("Class " + defaultClassLocation + " does not appear to have a default no-arg constructor so can not be used to resolve resource " + resourceType);
      LOG.debug(ex);
    }
    return null;
  }
}

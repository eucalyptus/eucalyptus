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

package com.eucalyptus.cloudformation.resources.standard;

import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceResolver;
import com.eucalyptus.cloudformation.resources.standard.actions.AWSAutoScalingAutoScalingGroupResourceAction;
import com.eucalyptus.cloudformation.resources.standard.info.AWSAutoScalingAutoScalingGroupResourceInfo;
import org.apache.log4j.Logger;

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

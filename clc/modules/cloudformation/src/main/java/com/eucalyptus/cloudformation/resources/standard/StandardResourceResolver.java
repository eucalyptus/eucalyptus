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

package com.eucalyptus.cloudformation.resources.standard;

import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceResolver;
import com.eucalyptus.cloudformation.resources.standard.actions.AWSEC2SecurityGroupResourceAction;
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
    String defaultClassLocation = AWSEC2SecurityGroupResourceAction.class.getPackage().getName() + "." + resourceType.replace(":","") + "ResourceAction";
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

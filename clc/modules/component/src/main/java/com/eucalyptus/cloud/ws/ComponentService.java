/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cloud.ws;

import org.apache.log4j.Logger;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds; 
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.async.AsyncRequests;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ComponentMessageType;

public class ComponentService {

	private static Logger LOG = Logger.getLogger(ComponentService.class);

	public BaseMessage handle(ComponentMessageType request) throws Exception {
		String component = request.getComponent();
		String host = request.getHost();
		String name = request.getName();
		ServiceConfiguration service = this.lookupService(component,host,name);
    LOG.info("Component: "+service);
    try {
      BaseMessage reply = null;
  		if(service.isVmLocal()) {//send direct to local component using mule registry directly
        reply = ServiceContext.send(service.getComponentId().getLocalEndpointName(),request);
   		} else {//send remote
        reply = AsyncRequests.sendSync(service,request);
   		}
      return reply;
    } catch (Exception e) {
      LOG.error(e);
      throw new EucalyptusCloudException("Unable to dispatch message to: "+service.getName());
    }
	}

  private ServiceConfiguration lookupService(String component,String name,String host) throws EucalyptusCloudException {
    ComponentId destCompId = ComponentIds.lookup(component);
    if(name != null) {
      return ServiceConfigurations.lookupByName(destCompId.getClass(),name);
    } else if (host != null) {
      return ServiceConfigurations.lookupByHost(destCompId.getClass(),name);
    } else {
      throw new EucalyptusCloudException("Unable to dispatch message to: "+component+"@"+host);
    }
  }
}

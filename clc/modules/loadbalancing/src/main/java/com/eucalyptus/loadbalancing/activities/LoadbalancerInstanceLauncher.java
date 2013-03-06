/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.activities;

import java.util.List;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.Exceptions;
import com.google.common.collect.Lists;
/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
@ConfigurableClass(root = "loadbalancing", description = "Parameters controlling loadbalancing")
public class LoadbalancerInstanceLauncher extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String> {
	private static final Logger LOG = Logger.getLogger( LoadbalancerInstanceLauncher.class );
	private List<String> launchedInstances = Lists.newArrayList();
	@ConfigurableField( displayName = "loadbalancer_emi", 
	                    description = "EMI containing haproxy and the controller",
	                    initial = "NULL", 
	                    readonly = false,
	                    type = ConfigurableFieldType.KEYVALUE )
	public static String LOADBALANCER_EMI = "NULL";
	private int numInstancesToLaunch = 1;
	
	@ConfigurableField( displayName = "loadbalancer_instance_type", 
            description = "instance type for loadbalancer instances",
            initial = "NULL", 
            readonly = false,
            type = ConfigurableFieldType.KEYVALUE )
	public static String LOADBALANCER_INSTANCE_TYPE = "NULL";
	
	@ConfigurableField( displayName = "loadbalancer_service_path", 
			description = "service path of the loadbalancer",
			initial = "services/Eucalyptus",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE )
	public static String LOADBALANCER_SERVICE_PATH = "";
	
	LoadbalancerInstanceLauncher(EventHandlerChain<NewLoadbalancerEvent> chain, final int numInstances){
		super(chain);
		this.numInstancesToLaunch = numInstances;
	}
	// Assume the request parameters and the resulting resource allocation is validated in the admission control step
	@Override
	public void apply(NewLoadbalancerEvent evt) throws EventHandlerException {
		final Collection<String> zones = evt.getZones();
		for (String zoneToLaunch : zones){
			InstanceUserDataBuilder userDataBuilder  = null;
			try{
				userDataBuilder=new InstanceUserDataWithCredential(evt.getContext().getUserFullName(), zoneToLaunch);
			}catch(Exception ex){
				throw new EventHandlerException("failed to create service parameters", ex);
			}
			
			List<String> instanceIds = null;
			try{
				instanceIds = 
						EucalyptusActivityTasks.getInstance().launchInstances(zoneToLaunch, 
								LOADBALANCER_EMI, LOADBALANCER_INSTANCE_TYPE, this.numInstancesToLaunch, userDataBuilder.build());
				StringBuilder sb = new StringBuilder();
				for (String id : instanceIds)
					sb.append(id+" ");
				LOG.info("new servo instance launched: "+sb.toString());
			}catch(Exception ex){
				throw new EventHandlerException("failed to launch the servo instance", ex);
			}
			launchedInstances.addAll(instanceIds);
		} //// TODO: SPARK: SAFE TO ASSUME ROLLBACK STEP WILL CLEANUP PARTIALLY COMPLETED REQUEST?
	}

	private class InstanceUserDataWithCredential extends DefaultInstanceUserDataBuilder{
		private UserFullName user = null;
		InstanceUserDataWithCredential(UserFullName requestingUser, String zone){
			super(zone);
			/// credentials
			this.user = requestingUser;
			/// TODO: SPARK: Assuming that the ELB service has an access to database
			try{
				/// ASSUMING LB SERVICE HAS ACCESS TO LOCAL DB
				final User adminUser= Accounts.lookupSystemAdmin( );
				final List<AccessKey> adminKeys = adminUser.getKeys();
				if(adminKeys.size() <= 0)
					throw new EucalyptusActivityException("No access key is found for the admin user");
				final AccessKey adminKey = adminKeys.get(0);
				this.add("access_key", adminKey.getAccessKey());
				this.add("secret_key", adminKey.getSecretKey());
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}
	
	private static class DefaultInstanceUserDataBuilder implements InstanceUserDataBuilder {
		ConcurrentHashMap<String,String> dataDict= null;
		protected DefaultInstanceUserDataBuilder(String zone){
			dataDict = new ConcurrentHashMap<String,String>();
			/// zone
			this.add("availability_zone", zone)	;
			// describe-services to retrieve the info:
			try{
				List<ServiceStatusType> services = 
						EucalyptusActivityTasks.getInstance().describeServices("eucalyptus");
			
				if(services == null || services.size()<=0)
					throw new EucalyptusActivityException("failed to describe eucalyptus services");
				// TODO:SPARK HA?
				ServiceStatusType service = services.get(0); 
				String serviceUrl = service.getServiceId().getUri();
				
				// parse the service Url: e.g., http://192.168.0.1:8773/services/Eucalyptus 
				String tmp = serviceUrl.replace("http://", "").replace("https://", "");
				String host = tmp.substring(0, tmp.indexOf(":"));
				tmp = tmp.replace(host+":", "");
				String port = tmp.substring(0, tmp.indexOf("/"));
				String path = tmp.replace(port+"/", "");
				this.add("eucalyptus_host", host);
				this.add("eucalyptus_port", port);
				this.add("eucalyptus_path", path);
				
				/// TODO: SPARK: ELB service host different from eucalyptus
				this.add("elb_host", host);
				this.add("elb_port", port);	/// elb service path
				this.add("elb_path", LOADBALANCER_SERVICE_PATH);
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
		
		protected void add(String name, String value){
			dataDict.put(name, value);
		}
		
		@Override
		public String build(){
			StringBuilder sb  = new StringBuilder();
			for (String key : dataDict.keySet()){
				String value = dataDict.get(key);
				sb.append(String.format("%s=%s;", key, value));
			}
			return B64.standard.encString(sb.toString());
		}
	}
	interface InstanceUserDataBuilder {
		String build();
	}
	@Override
	public void rollback() throws EventHandlerException {
		// terminate the launched instances
		if(this.launchedInstances!=null && this.launchedInstances.size() >0){
			List<String> terminated = null;
			try{
				terminated = EucalyptusActivityTasks.getInstance().terminateInstances(this.launchedInstances);
				if (terminated.size() != this.launchedInstances.size())
					throw new EventHandlerException("some instances were not terminated");
			}catch(EventHandlerException ex){
				throw ex;
			}catch(Exception ex){
				throw new EventHandlerException("failed to terminate instances", ex);
			}
		}
	}
	@Override
	public List<String> getResult() {
		// TODO Auto-generated method stub
		return  this.launchedInstances == null ? Lists.<String>newArrayList() : this.launchedInstances;
	}
	
}

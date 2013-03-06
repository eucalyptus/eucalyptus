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
import java.util.NoSuchElementException;
import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.google.common.collect.Lists;

/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
@ConfigurableClass(root = "loadbalancing", description = "Parameters controlling loadbalancing")
public class EventHandlerChainNew extends EventHandlerChain<NewLoadbalancerEvent> {
	private static Logger LOG  = Logger.getLogger( EventHandlerChainNew.class );
	@ConfigurableField( displayName = "loadbalancer_num_vm",
			description = "number of VMs per loadbalancer zone",
			initial = "1",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE
			)
	public static String LOADBALANCER_NUM_VM = "1";
	
	@ConfigurableField( displayName = "loadbalancer_loadbalancer_per_user",
			description = "max number of loadbalancers per user",
			initial = "5",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE)
	public static String MAX_LOADBALANCER_PER_USER = "5";
	
	@ConfigurableField ( displayName = "loadbalancer_vm_per_user",
			description = "max number of virtual machines per user",
			initial = "5",
			readonly = false,
			type = ConfigurableFieldType.KEYVALUE)
	public static String MAX_VM_PER_USER = "5";
	
	@Override
	public EventHandlerChain<NewLoadbalancerEvent> build() {
		this.insert(new AdmissionControl(this));
		/// TODO: SPARK: setup security group
	  	this.insert(new DatabaseInsert(this));
	  	int numVm = 1;
	  	try{
	  		numVm = Integer.parseInt(EventHandlerChainNew.LOADBALANCER_NUM_VM);
	  	}catch(NumberFormatException ex){
	  		LOG.warn("unable to parse loadbalancer_num_vm");
	  	}
	  	this.insert(new LoadbalancerInstanceLauncher(this, numVm));
	  	this.insert(new DatabaseUpdate(this));
		
    /// SPARK: TODO: should check the loadbalancer's DNS and map the pub IP of instances to the DNS
		return this;	
	}
	
	private static class AdmissionControl extends AbstractEventHandler<NewLoadbalancerEvent> {
		AdmissionControl(EventHandlerChain<NewLoadbalancerEvent> chain){
			super(chain);
		}
		@Override
		public void apply(NewLoadbalancerEvent evt) throws EventHandlerException {
			// TODO Auto-generated method stub
			
			// check if the requested parameter is valid
			   // loadbalancer
			   // zones
			   // user
			
			//  check if the currently allocated resources + newly requested resources is within the limit
		}

		@Override
		public void rollback() throws EventHandlerException {
			// TODO Auto-generated method stub
			
		}
	}
	
	private class SecurityGroupSetup extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String> {
		SecurityGroupSetup(EventHandlerChain<NewLoadbalancerEvent> chain){
			super(chain);
		}
		
		@Override
		public List<String> getResult() {
			return null;
		}

		@Override
		public void apply(NewLoadbalancerEvent evt)
				throws EventHandlerException {
	
		}

		@Override
		public void rollback() throws EventHandlerException {
			
		}
	}
	
	private class DatabaseInsert extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String>{
		private List<String> uniqueEntries = Lists.newArrayList();
		DatabaseInsert(EventHandlerChain<NewLoadbalancerEvent> chain){
			super(chain);
		}
		@Override
		public void apply(NewLoadbalancerEvent evt)
				throws EventHandlerException {
			
			LoadBalancer lb = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext().getUserFullName(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("can't find the specified loadbalancer user owns", ex);
			}catch(Exception ex){
				throw new EventHandlerException("can't query the loadbalancer due to unknown reason", ex);
			}
			
			final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
			try{
				for (String zoneName : evt.getZones()){
					LoadBalancerZone lbZone = null;
					try{
						lbZone = LoadBalancers.findZone(lb, zoneName);
						// save the servoinstance entry to the DB
					}catch(NoSuchElementException ex){
						throw new EventHandlerException("Can't find the zone in user's loadbalancer ("+zoneName+")", ex);
					}catch(Exception ex){
						throw new EventHandlerException("Can't find the zone due to unknown reason");
					}	
					final LoadBalancerServoInstance instance = LoadBalancerServoInstance.named(lbZone);
					instance.generateUniqueName();
					this.uniqueEntries.add(instance.getUniqueName());
					Entities.persist(instance);
				}
				db.commit();
			}catch(Exception ex){
				db.rollback();
				this.uniqueEntries.clear();
				throw new EventHandlerException("failed to persist the new loadbalancer VMs");
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			final EntityTransaction db = Entities.get(LoadBalancerServoInstance.class);
			for(String unique : this.uniqueEntries){
				try{
					final LoadBalancerServoInstance exist = 
							Entities.uniqueResult(LoadBalancerServoInstance.fromUniqueName(unique));
					Entities.delete(exist);
					db.commit();
				}catch(NoSuchElementException ex){
					LOG.warn("failed to find the loadbalanncer vm to delete");
					db.rollback();
				}catch(Exception ex){
					LOG.error("failed to delete the loadbalancer vm");
					db.rollback();
				}
			}
			this.uniqueEntries.clear();
		}

		@Override
		public List<String> getResult() {
			// TODO Auto-generated method stub
			return this.uniqueEntries;
		}
	}
	
	private class DatabaseUpdate extends AbstractEventHandler<NewLoadbalancerEvent>{
		private StoredResult<String> launcher = null;
		private StoredResult<String> insert = null;
		
		DatabaseUpdate(EventHandlerChain<NewLoadbalancerEvent> chain){
			super(chain);
			launcher = chain.findHandler(LoadbalancerInstanceLauncher.class);
			insert = chain.findHandler(DatabaseInsert.class);
		}
		
		@Override
		public void apply(NewLoadbalancerEvent evt)
				throws EventHandlerException {
			// get the inserted entries
			final List<String> uniqueEntries = insert.getResult();
			// find out new instance ids
			final List<String> newInstances = launcher.getResult();
			// update the database
			if(uniqueEntries.size() != newInstances.size())
				throw new EventHandlerException("Number of launched instances doesn't match with the database record");
			
			final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
			try{
				for(String uniqueId : uniqueEntries){
					String instanceId= newInstances.remove(0);
					final LoadBalancerServoInstance exist = 
							Entities.uniqueResult(LoadBalancerServoInstance.fromUniqueName(uniqueId));
					exist.setInstanceId(instanceId);
					Entities.persist(exist);
				}
				db.commit();
			}catch(NoSuchElementException ex){
				db.rollback();
				throw new EventHandlerException("can't find the loadbalancer VM from the db", ex);
			}catch(Exception ex){
				db.rollback();
				throw new EventHandlerException("failed to update the loadbalancer VM entries", ex);
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
		
	}
}

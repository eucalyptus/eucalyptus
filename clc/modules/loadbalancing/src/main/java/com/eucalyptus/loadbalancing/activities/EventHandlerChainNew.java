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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloud.CloudMetadatas;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.loadbalancing.LoadBalancer;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.LoadBalancing;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;

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
	  	this.insert(new SecurityGroupSetup(this));
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
	
	public static class AdmissionControl extends AbstractEventHandler<NewLoadbalancerEvent> {
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
	
	public static class SecurityGroupSetup extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String>{
		private String createdGroup = null;
		NewLoadbalancerEvent event = null;
		SecurityGroupSetup(EventHandlerChain<NewLoadbalancerEvent> chain){
			super(chain);
		}

		@Override
		public void apply(NewLoadbalancerEvent evt)
				throws EventHandlerException {
			this.event = evt;
			String groupName = String.format("euca-lb-%s-%s", evt.getLoadBalancer(), 
					UUID.randomUUID().toString().substring(0, 5));
			String groupDesc = String.format("group for loadbalancer %s", evt.getLoadBalancer());
			// create a new security group
			try{
				EucalyptusActivityTasks.getInstance().createSecurityGroup(groupName, groupDesc);
				createdGroup = groupName;
			}catch(Exception ex){
				throw new EventHandlerException("Failed to create the security group for loadbalancer", ex);
			}
			
			// set security group with the loadbalancer; update db
			LoadBalancer lb = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext().getUserFullName(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}

			final EntityTransaction db = Entities.get( LoadBalancerSecurityGroup.class );
			try{
				Entities.uniqueResult(LoadBalancerSecurityGroup.named( lb, groupName));
				db.commit();
			}catch(NoSuchElementException ex){
				final LoadBalancerSecurityGroup newGroup = LoadBalancerSecurityGroup.named( lb, groupName);
				LoadBalancerSecurityGroup written = Entities.persist(newGroup);
				Entities.flush(written);
				db.commit();
			}catch(Exception ex){
				db.rollback();
				throw new EventHandlerException("Error while persisting security group", ex);
			}
		}

		@Override
		public void rollback() 
				throws EventHandlerException {
			if(this.createdGroup == null)
				return;
			// set security group with the loadbalancer; update db
			LoadBalancer lb = null;
			try{
				lb = LoadBalancers.getLoadbalancer(this.event.getContext().getUserFullName(), this.event.getLoadBalancer());
			}catch(NoSuchElementException ex){
				return;
			}catch(Exception ex){
				return;
			}
			try{
				EucalyptusActivityTasks.getInstance().deleteSecurityGroup(this.createdGroup);
			}catch(Exception ex){
				throw new EventHandlerException("Failed to delete the security group in rollback", ex);
			}

			final EntityTransaction db = Entities.get( LoadBalancerSecurityGroup.class );
			try{
				final LoadBalancerSecurityGroup sample = LoadBalancerSecurityGroup.named(lb, this.createdGroup);
				final LoadBalancerSecurityGroup toDelete = Entities.uniqueResult(sample);
				Entities.delete(toDelete);
				db.commit();
			}catch(NoSuchElementException ex){
				db.rollback();
			}catch(Exception ex){
				db.rollback();
				throw new EventHandlerException("Error while deleting security group record in rollback", ex);
			}
		}
		

		@Override
		public List<String> getResult() {
			// TODO Auto-generated method stub
			List<String> result = Lists.newArrayList();
			if(this.createdGroup != null)
				result.add(this.createdGroup);
			return result;
		}
	}
	
	public static class DatabaseInsert extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String>{
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
			
			List<LoadBalancerZone> zones = Lists.newArrayList();
		
			for (String zoneName : evt.getZones()){
				LoadBalancerZone lbZone = null;
				try{
					lbZone = LoadBalancers.findZone(lb, zoneName);
					zones.add(lbZone);
					// save the servoinstance entry to the DB
				}catch(NoSuchElementException ex){
					throw new EventHandlerException("Can't find the zone in user's loadbalancer ("+zoneName+")", ex);
				}catch(Exception ex){
					throw new EventHandlerException("Can't find the zone due to unknown reason");
				}
			}
			
			final Function<LoadBalancerZone, LoadBalancerServoInstance> persistInstance = 
					new Function<LoadBalancerZone, LoadBalancerServoInstance>( ) {
		        public LoadBalancerServoInstance apply( final LoadBalancerZone zone ) {
					final LoadBalancerServoInstance instance = LoadBalancerServoInstance.named(zone);
					instance.setState(LoadBalancerServoInstance.STATE.Pending);
					instance.setInstanceId(UUID.randomUUID().toString().substring(0, 6));
					LoadBalancerServoInstance written = Entities.persist(instance);
					return written;
		        }
		    }; 
			for(LoadBalancerZone zone : zones){
				LoadBalancerServoInstance persist = Entities.asTransaction(LoadBalancerServoInstance.class, persistInstance).apply(zone);
				this.uniqueEntries.add(persist.getInstanceId());
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			final EntityTransaction db = Entities.get(LoadBalancerServoInstance.class);
			for(String unique : this.uniqueEntries){
				try{
					final LoadBalancerServoInstance exist = 
							Entities.uniqueResult(LoadBalancerServoInstance.named(unique));
					Entities.delete(exist);
					db.commit();
				}catch(NoSuchElementException ex){
					db.rollback();
					LOG.warn("failed to find the loadbalanncer vm to delete");
				}catch(Exception ex){
					db.rollback();
					LOG.error("failed to delete the loadbalancer vm");
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
	
	public static class DatabaseUpdate extends AbstractEventHandler<NewLoadbalancerEvent>{
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
			
			LoadBalancer lb = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext().getUserFullName(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("can't find the specified loadbalancer user owns", ex);
			}catch(Exception ex){
				throw new EventHandlerException("can't query the loadbalancer due to unknown reason", ex);
			}
			
			String groupName = null;
			LoadBalancerSecurityGroup group= null;
			try{
				StoredResult<String> sgroupResult = this.getChain().findHandler(SecurityGroupSetup.class);
				if(sgroupResult!= null){
					List<String> result = sgroupResult.getResult();
					if(result!=null && result.size()>0)
						groupName = result.get(0);
				}
				if(groupName != null){
					final EntityTransaction db = Entities.get( LoadBalancerSecurityGroup.class );
					try{
						group = Entities.uniqueResult(LoadBalancerSecurityGroup.named(lb, groupName));
						db.commit();
					}catch(Exception ex){
						db.rollback();
						throw ex;
					}
				}
			}catch(Exception ex){
				;
			}
			
			
			// update the database
			if(uniqueEntries.size() != newInstances.size())
				throw new EventHandlerException("Number of launched instances doesn't match with the database record");
			
			final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
			try{
				for(String uniqueId : uniqueEntries){
					String instanceId= newInstances.remove(0);
					final LoadBalancerServoInstance exist = 
							Entities.uniqueResult(LoadBalancerServoInstance.named(uniqueId)); //.fromUniqueName(uniqueId));
					exist.setInstanceId(instanceId);
					if(group!= null)
						exist.setSecurityGroup(group);
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

	public static class ServoInstancePendingToRunningChecker implements EventListener<ClockTick> {
		public static void register( ) {
	      Listeners.register( ClockTick.class, new ServoInstancePendingToRunningChecker() );
	    }


	    @Override
	    public void fireEvent( final ClockTick event ) {
	      if ( Bootstrap.isFinished() &&
	          Topology.isEnabledLocally( LoadBalancing.class ) &&
	          Topology.isEnabled( Eucalyptus.class ) ) {
	    	  /// find all servo instances in PENDING STATE
	    	  final EntityTransaction db = Entities.get( LoadBalancerServoInstance.class );
	    	  final LoadBalancerServoInstance sample = 
	    			  LoadBalancerServoInstance.withState(LoadBalancerServoInstance.STATE.Pending.name());
	    	  List<LoadBalancerServoInstance> instances = null;
	    	  try{
	    		  instances = Entities.query(sample);
	    		  db.commit();
	    	  }catch(NoSuchElementException ex){
	    		  db.rollback();
	    	  }catch(Exception ex){
	    		  db.rollback();
	    		  LOG.warn("Loadbalancer: failed to query servo instances");
	    	  }

	    	  if(instances==null || instances.size()==0)
	    		  return;
	    	  
	    	  /// for each:
	    	  final List<String> param = Lists.newArrayList();
	    	  final Map<String, String> latestState = Maps.newHashMap();
	    	  final Map<String, String> addressMap = Maps.newHashMap();
	    	  for(final LoadBalancerServoInstance instance : instances){
	    		/// 	call describe instance
		    	  String instanceId = instance.getInstanceId();
	    		  if(instanceId == null || !instanceId.startsWith("i-"))
	    			  continue;
	    		  param.clear();
	    		  param.add(instanceId);
	    		  String instanceState = null;
	    		  String address = null;
	    		  try{
	    			  final List<RunningInstancesItemType> result = 
	    					  EucalyptusActivityTasks.getInstance().describeInstances(param);
	    			  if (result.isEmpty())
	    				  throw new Exception("Describe instances returned no result");
	    			  instanceState = result.get(0).getStateName();
	    			  address = result.get(0).getIpAddress();
	    		  }catch(final Exception ex){
	    			  LOG.warn("failed to query instances", ex);
	    			  continue;
	    		  }
	    		  if(instanceState.equals("running")){
	    	    	  ///		if update dns A rec:
	    			  latestState.put(instanceId, LoadBalancerServoInstance.STATE.InService.name());
	    			  if(address!=null)
	    				  addressMap.put(instanceId, address);
	    		  }else if(instanceState.equals("pending")){
	    			  latestState.put(instanceId, LoadBalancerServoInstance.STATE.Pending.name());
	    		  }else{
	    			  /// error condition (shutting-down, terminated, etc...)
	    			  latestState.put(instanceId, LoadBalancerServoInstance.STATE.Error.name());
	    		  }
	    	  }

	    	  List<LoadBalancerServoInstance> newInServiceInstances = Lists.newArrayList();
	    	  for(final String instanceId : latestState.keySet()){
	    		  String nextState = latestState.get(instanceId);
	    		  if(nextState == "pending")
	    			  continue; // no change
	    		  
		    	  final EntityTransaction dbUpdate = Entities.get( LoadBalancerServoInstance.class );
	    		  try{
	    			  final LoadBalancerServoInstance instance =
	    					  Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
	    			  instance.setState(Enum.valueOf(LoadBalancerServoInstance.STATE.class, nextState));
	    			  if(addressMap.containsKey(instanceId))
	    				  instance.setAddress(addressMap.get(instanceId));
	    			  Entities.persist(instance);
	    			  dbUpdate.commit();
	    			  if(nextState.equals(LoadBalancerServoInstance.STATE.InService.name()))
	    				  newInServiceInstances.add(instance);
	    		  }catch(NoSuchElementException ex){
	    			  dbUpdate.rollback();
	    			  LOG.error("could not find the servo instance with id="+instanceId, ex);
	    		  }catch(Exception ex){
	    			  dbUpdate.rollback();
	    			  LOG.error("unknown error occured during the servo instance update ("+instanceId+")", ex);
	    		  }
	    	  }
	    	  
	    	  for(LoadBalancerServoInstance servo : newInServiceInstances){
	    		 final LoadBalancer lb= servo.getAvailabilityZone().getLoadbalancer();
	    		 final LoadBalancerDnsRecord dns = lb.getDns();
	    		 try{
	    			 EucalyptusActivityTasks.getInstance().updateARecord(dns.getZone(), dns.getName(), servo.getAddress());
	    		 }catch(Exception ex){
	    			 LOG.error("failed to update dns A records", ex);
	    			 continue;
	    		 }
		    	 final EntityTransaction db2 = Entities.get(LoadBalancerServoInstance.class);
	    		 try{
	    			 LoadBalancerServoInstance update = Entities.uniqueResult(servo);
	    			 update.setDns(dns);
	    			 Entities.persist(update);
	    			 db2.commit();
	    		 }catch(Exception ex){
	    			 db2.rollback();
	    			 LOG.error("failed to update dns record of the servo instance");
	    		 }
	    	  }
	      }
	    }
	}
}

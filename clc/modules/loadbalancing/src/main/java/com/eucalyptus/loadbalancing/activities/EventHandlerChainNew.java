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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.euare.InstanceProfileType;
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.autoscaling.common.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.AutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsResult;
import com.eucalyptus.autoscaling.common.Instance;
import com.eucalyptus.autoscaling.common.Instances;
import com.eucalyptus.bootstrap.Bootstrap;
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
import com.google.common.collect.Lists;

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
	
	@Override
	public EventHandlerChain<NewLoadbalancerEvent> build() {
		this.insert(new AdmissionControl(this));
		this.insert(new IAMRoleSetup(this));
		this.insert(new InstanceProfileSetup(this));
	  	this.insert(new DNSANameSetup(this));
	  	this.insert(new SecurityGroupSetup(this));
	  	int numVm = 1;
	  	try{
	  		numVm = Integer.parseInt(EventHandlerChainNew.LOADBALANCER_NUM_VM);
	  	}catch(NumberFormatException ex){
	  		LOG.warn("unable to parse loadbalancer_num_vm");
	  	}
	  	this.insert(new LoadBalancerASGroupCreator(this, numVm));
	
	  	return this;	
	}
	
	static class AdmissionControl extends AbstractEventHandler<NewLoadbalancerEvent> {
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
	
	static class IAMRoleSetup extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String>{
		public static final String DEFAULT_ROLE_PATH_PREFIX = "/internal/loadbalancer";
		public static final String DEFAULT_ROLE_NAME = "loadbalancer-vm";
		public static final String DEFAULT_ASSUME_ROLE_POLICY = 
				"{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":[\"ec2.amazonaws.com\"]},\"Action\":[\"sts:AssumeRole\"]}]}" 
				+
				"{\"Statement\":[{\"Action\": \"elasticloadbalancing:*\",\"Effect\": \"Allow\",\"Resource\": \"*\"}]}";
		
		private RoleType role = null;
		protected IAMRoleSetup(EventHandlerChain<NewLoadbalancerEvent> chain) {
			super(chain);
			// TODO Auto-generated constructor stub
		}

		@Override
		public List<String> getResult() {
			return this.role!=null ? Lists.newArrayList(this.role.getRoleName()) : Lists.<String>newArrayList();
		}

		@Override
		public void apply(NewLoadbalancerEvent evt)
				throws EventHandlerException {
			// list-roles.
			try{
				List<RoleType> result = EucalyptusActivityTasks.getInstance().listRoles(DEFAULT_ROLE_PATH_PREFIX);
				if(result != null){
					for(RoleType r : result){
						if(DEFAULT_ROLE_NAME.equals(r.getRoleName())){
							role = r;
							break;
						}	
					}
				}
			}catch(Exception ex){
				throw new EventHandlerException("Failed to list IAM roles", ex);
			}

			// if no role found, create a new role with assume-role policy for elb
			if(role==null){	/// create a new role
				try{
					role = EucalyptusActivityTasks.getInstance().createRole(DEFAULT_ROLE_NAME, DEFAULT_ROLE_PATH_PREFIX, DEFAULT_ASSUME_ROLE_POLICY);
				}catch(Exception ex){
					throw new EventHandlerException("Failed to create the role for ELB Vms");
				}
			}
			
			if(role==null)
				throw new EventHandlerException("No role is found for LoadBalancer Vms");		
		}

		@Override
		public void rollback() throws EventHandlerException {
			; // role and instance profile are system-wide for all loadalancers; no need to delete them
		}	
	}
	

	static class InstanceProfileSetup extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String>{
		public static final String DEFAULT_INSTANCE_PROFILE_PATH_PREFIX="/internal/loadbalancer";
		public static final String DEFAULT_INSTANCE_PROFILE_NAME = "loadbalancer-vm";

		private InstanceProfileType instanceProfile = null;
		protected InstanceProfileSetup(
				EventHandlerChain<NewLoadbalancerEvent> chain) {
			super(chain);
		}

		@Override
		public List<String> getResult() {
			return this.instanceProfile!=null ? Lists.newArrayList(this.instanceProfile.getInstanceProfileName()) : Lists.<String>newArrayList();
		}

		@Override
		public void apply(NewLoadbalancerEvent evt)
				throws EventHandlerException {
			// list instance profiles
			try{
				//   check if the instance profile for ELB VM is found
				List<InstanceProfileType> instanceProfiles =
						EucalyptusActivityTasks.getInstance().listInstanceProfiles(DEFAULT_INSTANCE_PROFILE_PATH_PREFIX);
				for(InstanceProfileType ip : instanceProfiles){
					if(DEFAULT_INSTANCE_PROFILE_NAME.equals(ip.getInstanceProfileName())){
						instanceProfile = ip;
						break;
					}
				}
			}catch(Exception ex){
				throw new EventHandlerException("Failed to list instance profiles", ex);
			}
			
			if(instanceProfile == null){	//   if not create one
				try{
					instanceProfile = 
							EucalyptusActivityTasks.getInstance().createInstanceProfile(DEFAULT_INSTANCE_PROFILE_NAME, DEFAULT_INSTANCE_PROFILE_PATH_PREFIX);
				}catch(Exception ex){
					throw new EventHandlerException("Failed to create instance profile", ex);
				}
			}
			if(instanceProfile == null)
				throw new EventHandlerException("No instance profile for loadbalancer VM is found");
			
			// make sure the role is added to the instance profile; if not add it.
			final List<String> result = this.chain.findHandler(IAMRoleSetup.class).getResult();
			String lbRoleName = null;
			if(result!=null && result.size()>0)
				lbRoleName = result.get(0);
			
			try{
				List<RoleType> roles = instanceProfile.getRoles().getMember();
				boolean roleFound = false;
				for(RoleType role : roles){
					if(role.getRoleName().equals(lbRoleName)){
						roleFound=true;
						break;
					}
				}
				if(!roleFound)
					throw new NoSuchElementException();
			}catch(Exception ex){
				if(lbRoleName == null)
					throw new EventHandlerException("No role name is found for loadbalancer VMs");
				try{
					EucalyptusActivityTasks.getInstance().addRoleToInstanceProfile(this.instanceProfile.getInstanceProfileName(), lbRoleName);
				}catch(Exception ex2){
					throw new EventHandlerException("Failed to add role to the instance profile", ex2);
				}
			}
		}
		
		@Override
		public void rollback() throws EventHandlerException {
			;
		}
	}
	
	static class DNSANameSetup extends AbstractEventHandler<NewLoadbalancerEvent> {
		private String dnsName = null;
		private String dnsZone= null;
		DNSANameSetup(EventHandlerChain<NewLoadbalancerEvent> chain){
			super(chain);
		}
		
		@Override
		public void apply(NewLoadbalancerEvent evt) throws EventHandlerException {	
			LoadBalancer lb = null;
			LoadBalancerDnsRecord dns = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext().getUserFullName(), evt.getLoadBalancer());
				dns = lb.getDns();
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Failed to find the loadbalancer "+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Failed due to query exception", ex);
			}
			if(dns==null)
				throw new EventHandlerException("No dns record is found with the loadbalancer "+evt.getLoadBalancer());
			
			try{
				EucalyptusActivityTasks.getInstance().removeMultiARecord(dns.getZone(), dns.getName());
				EucalyptusActivityTasks.getInstance().createARecord(dns.getZone(), dns.getName());
				this.dnsName = dns.getName();
				this.dnsZone = dns.getZone();
			}catch(Exception ex){
				throw new EventHandlerException("Failed to create new multiA name record");
			}
		}
		

		@Override
		public void rollback() 
				throws EventHandlerException {
			if(this.dnsName!=null && this.dnsZone!= null){
				try{
					EucalyptusActivityTasks.getInstance().removeMultiARecord(this.dnsZone, this.dnsName);
				}catch(Exception ex){
					LOG.warn(String.format("failed to remove the multi A record (%s-%s)", this.dnsZone, this.dnsName), ex);
				}
			}
		}
	}
	
	static class SecurityGroupSetup extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String>{
		private String createdGroup = null;
		NewLoadbalancerEvent event = null;
		SecurityGroupSetup(EventHandlerChain<NewLoadbalancerEvent> chain){
			super(chain);
		}

		@Override
		public void apply(NewLoadbalancerEvent evt)
				throws EventHandlerException {
			this.event = evt;
			// set security group with the loadbalancer; update db
			LoadBalancer lb = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext().getUserFullName(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}

			String groupName = String.format("euca-internal-%s-%s", lb.getOwnerAccountNumber(), lb.getDisplayName());
			String groupDesc = String.format("group for loadbalancer %s", evt.getLoadBalancer());
			
			try{
				EucalyptusActivityTasks.getInstance().deleteSecurityGroup(groupName);
			}catch(Exception ex){
				;
			}
			
			// create a new security group
			try{
				EucalyptusActivityTasks.getInstance().createSecurityGroup(groupName, groupDesc);
				createdGroup = groupName;
			}catch(Exception ex){
				throw new EventHandlerException("Failed to create the security group for loadbalancer", ex);
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
		
	// periodically queries autoscaling group, finds the instances, and update the servo instance records
	// based on the query result
	public static class AutoscalingGroupInstanceChecker implements EventListener<ClockTick> {
		private static int AUTOSCALE_GROUP_CHECK_INTERVAL_SEC = 10;
		
		public static void register(){
			Listeners.register(ClockTick.class, new AutoscalingGroupInstanceChecker() );
		}
		
		@Override
		public void fireEvent(ClockTick event) {
			
			if ( Bootstrap.isFinished() &&
			          Topology.isEnabledLocally( LoadBalancing.class ) &&
			          Topology.isEnabled( Eucalyptus.class ) ) {
				
				// lookup all LoadBalancerAutoScalingGroup records
				EntityTransaction db = Entities.get( LoadBalancerAutoScalingGroup.class );
				List<LoadBalancerAutoScalingGroup> groups = Lists.newArrayList();
				Map<String, LoadBalancerAutoScalingGroup>  allGroupMap = new ConcurrentHashMap<String, LoadBalancerAutoScalingGroup>();
				try{
					groups = Entities.query(LoadBalancerAutoScalingGroup.named(), true);
					db.commit();
					for(LoadBalancerAutoScalingGroup g : groups){
						allGroupMap.put(g.getName(), g);
					}
				}catch(NoSuchElementException ex){
					db.rollback();
				}catch(Exception ex){
					db.rollback();
				}
				
				Map<String, LoadBalancerAutoScalingGroup> groupToQuery = new ConcurrentHashMap<String, LoadBalancerAutoScalingGroup>();
				final Date current = new Date(System.currentTimeMillis());
				
				// find the record eligible to check its status
				for(final LoadBalancerAutoScalingGroup group : groups){
					final Date lastUpdate = group.getLastUpdateTimestamp();
					int elapsedSec = (int)((current.getTime() - lastUpdate.getTime())/1000.0);
					if(elapsedSec > AUTOSCALE_GROUP_CHECK_INTERVAL_SEC){
						db = Entities.get( LoadBalancerAutoScalingGroup.class );
						try{
							LoadBalancerAutoScalingGroup update = Entities.uniqueResult(group);
							update.setLastUpdateTimestamp(current);
							Entities.persist(update);
							db.commit();
						}catch(NoSuchElementException ex){
							db.rollback();
						}catch(Exception ex){
							db.rollback();
						}
						groupToQuery.put(group.getName(), group);
					}
				}
				 
				if(groupToQuery.size() <= 0)
					return;
				
				// describe as group and find the unknown instance Ids
				List<AutoScalingGroupType> queriedGroups = Lists.newArrayList();
				try{
					DescribeAutoScalingGroupsResponseType response = 
							EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(groupToQuery.keySet()));
					DescribeAutoScalingGroupsResult result = response.getDescribeAutoScalingGroupsResult();
					AutoScalingGroupsType asgroups = result.getAutoScalingGroups();
					queriedGroups = asgroups.getMember();
				}catch(Exception ex){
					LOG.error("Failed to describe autoscaling groups", ex);
					return;
				}
				
				/// lookup all servoInstances in the DB
				Map<String, LoadBalancerServoInstance> servoMap = 
						new ConcurrentHashMap<String, LoadBalancerServoInstance>();
				db = Entities.get( LoadBalancerServoInstance.class );
				try{
					final List<LoadBalancerServoInstance> result = Entities.query(LoadBalancerServoInstance.named(), true);
					db.commit();
					for(LoadBalancerServoInstance inst : result){
						servoMap.put(inst.getInstanceId(), inst);
					}
				}catch(NoSuchElementException ex){
					db.rollback();
				}catch(Exception ex){
					db.rollback();
				}
				 
				/// for all found instances that's not in the servo instance DB
				///     create servo record
				final List<LoadBalancerServoInstance> newServos = Lists.newArrayList();
				final Map<String, Instance> foundInstances = new ConcurrentHashMap<String, Instance>();
				for(final AutoScalingGroupType asg : queriedGroups){
					Instances instances = asg.getInstances();
					if(instances!=null && instances.getMember() != null && instances.getMember().size() >0){
						for(final Instance instance : instances.getMember()){
							String instanceId = instance.getInstanceId();
							foundInstances.put(instanceId, instance);
							if(!servoMap.containsKey(instanceId)){ /// new instance found
								try{
									LoadBalancerAutoScalingGroup group= allGroupMap.get(asg.getAutoScalingGroupName());
									if(group==null)
										throw new IllegalArgumentException("The group with name "+ asg.getAutoScalingGroupName()+ " not found in the database");
									
									final LoadBalancer lb = group.getLoadBalancer();
									LoadBalancerZone zone = null;
									for(LoadBalancerZone z : lb.getZones()){
										if(z.getName().equals(instance.getAvailabilityZone())){
											zone = z;
											break;
										}
									}
									if(zone == null)
										throw new Exception("No availability zone with name="+instance.getAvailabilityZone()+" found for loadbalancer "+lb.getDisplayName());
									final List<LoadBalancerSecurityGroup> sgroups = Lists.newArrayList(lb.getGroups());
									
									if(sgroups == null || sgroups.size()<=0)
										throw new Exception("No security group is found for loadbalancer "+lb.getDisplayName());
									final LoadBalancerSecurityGroup sgroup= sgroups.get(0);
									final LoadBalancerDnsRecord dns = lb.getDns();
									final LoadBalancerServoInstance newInstance = LoadBalancerServoInstance.newInstance(zone, sgroup, dns, group, instanceId);
									newServos.add(newInstance); /// persist later
								}catch(Exception ex){
									LOG.error("Failed to construct new servo instance", ex);
									continue;
								}
							}
						}
					}
				}
				
				// CASE 1: NEW INSTANCES WITH THE AS GROUP FOUND
				if(newServos.size()>0){
					db = Entities.get( LoadBalancerServoInstance.class );
					try{
						for(LoadBalancerServoInstance instance : newServos){
							Entities.persist(instance);
						}
						db.commit();
					}catch(Exception ex){
						db.rollback();
						LOG.error("Failed to persist the servo instance record", ex);
					}
				}
				
				List<LoadBalancerServoInstance> servoRecords = Lists.newArrayList();
				for(String groupName : groupToQuery.keySet()){
					final LoadBalancerAutoScalingGroup group = groupToQuery.get(groupName);
					servoRecords.addAll(group.getServos());
				}
				
				List<LoadBalancerServoInstance> registerDnsARec = Lists.newArrayList();
				for(LoadBalancerServoInstance instance : servoRecords){
					/// CASE 2: EXISTING SERVO INSTANCES ARE NOT FOUND IN THE QUERY RESPONSE 
					if(! foundInstances.containsKey(instance.getInstanceId())){
						db = Entities.get( LoadBalancerServoInstance.class );
						try{
							final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
							update.setState(LoadBalancerServoInstance.STATE.Error);
							Entities.persist(update);
							db.commit();
						}catch(NoSuchElementException ex){
							db.rollback();
						}catch(Exception ex){
							db.rollback();
						}
					}else{/// CASE 3: INSTANCE STATE UPDATED
						Instance instanceCurrent = foundInstances.get(instance.getInstanceId());
						final String healthState = instanceCurrent.getHealthStatus();
						final String lifecycleState = instanceCurrent.getLifecycleState();
						LoadBalancerServoInstance.STATE curState = instance.getState();
						LoadBalancerServoInstance.STATE newState = curState;
					
						if(healthState != null && ! healthState.equals("Healthy")){
							newState = LoadBalancerServoInstance.STATE.Error;
						}else if (lifecycleState != null){
							if(lifecycleState.equals("Pending"))
								newState = LoadBalancerServoInstance.STATE.Pending;
							else if(lifecycleState.equals("Quarantined"))
								newState = LoadBalancerServoInstance.STATE.Error;
							else if(lifecycleState.equals("InService"))
								newState = LoadBalancerServoInstance.STATE.InService;
							else if(lifecycleState.equals("Terminating") || lifecycleState.equals("Terminated"))
								newState = LoadBalancerServoInstance.STATE.OutOfService;
						}
						
						if(!curState.equals(newState)){
							if(newState.equals(LoadBalancerServoInstance.STATE.InService))
								registerDnsARec.add(instance);
							db = Entities.get( LoadBalancerServoInstance.class );
							try{
								final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
								update.setState(newState);
								Entities.persist(update);
								db.commit();
							}catch(NoSuchElementException ex){
								db.rollback();
							}catch(Exception ex){
								db.rollback();
							}
						}
					}
				}
				
				/// for new servo instances, find the IP and register it with DNS
				for(LoadBalancerServoInstance instance : registerDnsARec){
					String ipAddr = null;
					String privateIpAddr = null;
					try{
						List<RunningInstancesItemType> result = 
								EucalyptusActivityTasks.getInstance().describeSystemInstances(Lists.newArrayList(instance.getInstanceId()));
						if(result!=null && result.size()>0){
							ipAddr = result.get(0).getIpAddress();
							privateIpAddr = result.get(0).getPrivateIpAddress();
						}
					}catch(Exception ex){
						LOG.warn("failed to run describe-instances", ex);
						continue;
					}
					if(ipAddr == null || ipAddr.length()<=0){
						LOG.warn("no ipaddress found for instance "+instance.getInstanceId());
						continue;
					}
					try{
						String zone = instance.getDns().getZone();
						String name = instance.getDns().getName();
						EucalyptusActivityTasks.getInstance().addARecord(zone, name, ipAddr);
					}catch(Exception ex){
						LOG.warn("failed to register new ipaddress with dns A record", ex);
						continue;
					}

					db = Entities.get( LoadBalancerServoInstance.class );
					try{
						final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
						update.setAddress(ipAddr);
						if(privateIpAddr!=null)
							update.setPrivateIp(privateIpAddr);
						Entities.persist(update);
						db.commit();
					}catch(NoSuchElementException ex){
						db.rollback();
						LOG.warn("failed to find the servo instance named "+instance.getInstanceId(), ex);
					}catch(Exception ex){
						db.rollback();
						LOG.warn("failed to update servo instance's ip address", ex);
					}
				}
			}	
		}
	}
}

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

import com.eucalyptus.auth.euare.GetRolePolicyResult;
import com.eucalyptus.auth.euare.InstanceProfileType;
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResult;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.autoscaling.common.msgs.Instances;
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
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord.LoadBalancerDnsRecordCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerDnsRecord.LoadBalancerDnsRecordEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerZone;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancers;
import com.eucalyptus.loadbalancing.common.LoadBalancingBackend;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceEntityTransform;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;

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
		this.insert(new IAMPolicySetup(this));
	  	this.insert(new DNSANameSetup(this));
	  	this.insert(new SecurityGroupSetup(this));
	  	int numVm = 1;
	  	try{
	  		numVm = Integer.parseInt(EventHandlerChainNew.LOADBALANCER_NUM_VM);
	  	}catch(NumberFormatException ex){
	  		LOG.warn("unable to parse loadbalancer_num_vm");
	  	}
	  	this.insert(new LoadBalancerASGroupCreator(this, numVm));
	  	this.insert(new TagCreator(this));
	
	  	return this;	
	}
	
	static class AdmissionControl extends AbstractEventHandler<NewLoadbalancerEvent> {
		AdmissionControl(EventHandlerChain<NewLoadbalancerEvent> chain){
			super(chain);
		}
		@Override
		public void apply(NewLoadbalancerEvent evt) throws EventHandlerException {
			// is the loadbalancer_emi found?
			final String emi = LoadBalancerASGroupCreator.LOADBALANCER_EMI;
			List<ImageDetails> images = null;
			try{
				images = EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(emi));
				if(images==null || images.size()<=0 ||! images.get(0).getImageId().toLowerCase().equals(emi.toLowerCase()))
					throw new EventHandlerException("No loadbalancer EMI is found");
			}catch(final EventHandlerException ex){
				throw ex;
			}
			catch(final Exception ex){
				throw new EventHandlerException("failed to validate the loadbalancer EMI", ex);
			}
			
			// zones: is the CC found?
			final List<String> requestedZones = Lists.newArrayList(evt.getZones());
			List<ClusterInfoType> clusters = null;
			try{
				clusters = EucalyptusActivityTasks.getInstance().describeAvailabilityZones(true);
				for(final ClusterInfoType cc : clusters){
					requestedZones.remove(cc.getZoneName());
				}
			}catch(final Exception ex){
				throw new EventHandlerException("failed to validate the requested zones", ex);
			}
			if(requestedZones.size()>0){
				throw new EventHandlerException("unknown zone is requested");
			}
			
			// are there enough resources?
			final String instanceType = LoadBalancerASGroupCreator.LOADBALANCER_INSTANCE_TYPE;
			int numVm = 1;
		  	try{
		  		numVm = Integer.parseInt(EventHandlerChainNew.LOADBALANCER_NUM_VM);
		  	}catch(final NumberFormatException ex){
		  		LOG.warn("unable to parse loadbalancer_num_vm");
		  	}
		  	for(final String zone : evt.getZones()){
				final int capacity = findAvailableResources(clusters, zone, instanceType);
				if(numVm>capacity){
					throw new EventHandlerException(String.format("Not enough resources in %s", zone));
				}
			}
			
			// check if the keyname is configured and exists
			final String keyName = LoadBalancerASGroupCreator.LOADBALANCER_VM_KEYNAME;
			if(keyName!=null && keyName.length()>0){
				try{
					final List<DescribeKeyPairsResponseItemType> keypairs = EucalyptusActivityTasks.getInstance().describeKeyPairs(Lists.newArrayList(keyName));
					if(keypairs==null || keypairs.size()<=0 || !keypairs.get(0).getKeyName().equals(keyName))
						throw new Exception();
				}catch(Exception ex){
					throw new EventHandlerException(String.format("The configured keyname is not found"));
				}
			}
		}
		
		private int findAvailableResources(final List<ClusterInfoType> clusters, final String zoneName, final String instanceType){
			// parse euca-describe-availability-zones verbose response
			// WARNING: this is not a standard API!
			
			for(int i =0; i<clusters.size(); i++){
				final ClusterInfoType cc = clusters.get(i);
				if(zoneName.equals(cc.getZoneName())){
					for(int j=i+1; j< clusters.size(); j++){
						final ClusterInfoType candidate = clusters.get(j);
						if(candidate.getZoneName()!=null && candidate.getZoneName().toLowerCase().contains(instanceType.toLowerCase())){
							//<zoneState>0002 / 0002   2    512    10</zoneState>
							final String state = candidate.getZoneState();
							final String[] tokens = state.split("/");
							if(tokens!=null && tokens.length>0){
								try{
									String strNum = tokens[0].trim().replaceFirst("0+", "");
									if(strNum.length()<=0)
										strNum="0";
									
									return Integer.parseInt(strNum);
								}catch(final NumberFormatException ex){
									break;
								}catch(final Exception ex){
									break;
								}
							}
						}
					}
					break;
				}
			}
			return Integer.MAX_VALUE; // when check fails, let's assume its abundant
		}

		@Override
		public void rollback() throws EventHandlerException {
			;
		}
	}
	
	public static class IAMRoleSetup extends AbstractEventHandler<NewLoadbalancerEvent> implements StoredResult<String>{
		public static final String DEFAULT_ROLE_PATH_PREFIX = "/internal/loadbalancer";
		public static final String ROLE_NAME_PREFIX = "loadbalancer-vm";
		public static final String DEFAULT_ASSUME_ROLE_POLICY = 
				"{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":[\"ec2.amazonaws.com\"]},\"Action\":[\"sts:AssumeRole\"]}]}";		
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
	    final String roleName = String.format("%s-%s-%s", ROLE_NAME_PREFIX, evt.getContext().getAccount().getAccountNumber(), evt.getLoadBalancer());
	    // list-roles.
			try{
				List<RoleType> result = EucalyptusActivityTasks.getInstance().listRoles(DEFAULT_ROLE_PATH_PREFIX);
				if(result != null){
					for(RoleType r : result){
						if(roleName.equals(r.getRoleName())){
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
					role = EucalyptusActivityTasks.getInstance().createRole(roleName, DEFAULT_ROLE_PATH_PREFIX, DEFAULT_ASSUME_ROLE_POLICY);
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
		public static final String INSTANCE_PROFILE_NAME_PREFIX = "loadbalancer-vm";

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
		   final String instanceProfileName = 
		       String.format("%s-%s-%s", INSTANCE_PROFILE_NAME_PREFIX, evt.getContext().getAccount().getAccountNumber(), evt.getLoadBalancer());
		   
			// list instance profiles
			try{
				//   check if the instance profile for ELB VM is found
				List<InstanceProfileType> instanceProfiles =
						EucalyptusActivityTasks.getInstance().listInstanceProfiles(DEFAULT_INSTANCE_PROFILE_PATH_PREFIX);
				for(InstanceProfileType ip : instanceProfiles){
					if(instanceProfileName.equals(ip.getInstanceProfileName())){
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
							EucalyptusActivityTasks.getInstance().createInstanceProfile(instanceProfileName, DEFAULT_INSTANCE_PROFILE_PATH_PREFIX);
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
	
	static class IAMPolicySetup extends AbstractEventHandler<NewLoadbalancerEvent> {
		static final String SERVO_ROLE_POLICY_NAME = "euca-internal-loadbalancer-vm-policy";
		private static final String SERVO_ROLE_POLICY_DOCUMENT=
				"{\"Statement\":[{\"Action\": [\"elasticloadbalancing:DescribeLoadBalancersByServo\", \"elasticloadbalancing:PutServoStates\"],\"Effect\": \"Allow\",\"Resource\": \"*\"}]}";
				
			protected IAMPolicySetup(EventHandlerChain<NewLoadbalancerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(NewLoadbalancerEvent evt)
				throws EventHandlerException {
			String roleName = null;
			try{
				StoredResult<String> roleResult = this.chain.findHandler(IAMRoleSetup.class);
				if(roleResult.getResult()!=null && roleResult.getResult().size()>0)
					roleName = roleResult.getResult().get(0);
				if(roleName==null)
					throw new Exception();
			}catch(final Exception ex){
				throw new EventHandlerException("could not find the role name for loadbalancer vm");
			}
			
			GetRolePolicyResult policy  = null;
			/// GetRolePolicy: check if there's an existing policy doc
			try{
				policy = EucalyptusActivityTasks.getInstance().getRolePolicy(roleName, SERVO_ROLE_POLICY_NAME);
			}catch(final Exception ex){
				;
			}
			
			boolean putPolicy = false;
			if(policy == null || policy.getPolicyName() == null || !policy.getPolicyName().equals(SERVO_ROLE_POLICY_NAME)){
				putPolicy=true;
			}else if (!SERVO_ROLE_POLICY_DOCUMENT.toLowerCase().equals(policy.getPolicyDocument().toLowerCase())){
				try{
					EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, SERVO_ROLE_POLICY_NAME);
				}catch(final Exception ex){
					LOG.warn("failed to delete role policy", ex);
				}
				putPolicy = true;
			}else{
				putPolicy = false;
			}
			
			if(putPolicy){
				try{
					EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, SERVO_ROLE_POLICY_NAME, SERVO_ROLE_POLICY_DOCUMENT);
				}catch(final Exception ex){
					throw new EventHandlerException("failed to put role policy for loadbalancer vm");
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
		private NewLoadbalancerEvent event = null;
		DNSANameSetup(EventHandlerChain<NewLoadbalancerEvent> chain){
			super(chain);
		}
		
		@Override
		public void apply(NewLoadbalancerEvent evt) throws EventHandlerException {	
			this.event = evt;
			LoadBalancer lb = null;
			LoadBalancerDnsRecordCoreView dns = null;
			try{
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
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
		private String createdGroupId = null;
		private String groupOwnerAccountId = null;
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
				lb = LoadBalancers.getLoadbalancer(evt.getContext(), evt.getLoadBalancer());
			}catch(NoSuchElementException ex){
				throw new EventHandlerException("Could not find the loadbalancer with name="+evt.getLoadBalancer(), ex);
			}catch(Exception ex){
				throw new EventHandlerException("Error while looking for loadbalancer with name="+evt.getLoadBalancer(), ex);
			}

			String groupName = String.format("euca-internal-%s-%s", lb.getOwnerAccountNumber(), lb.getDisplayName());
			String groupDesc = String.format("group for loadbalancer %s", evt.getLoadBalancer());
			
			EntityTransaction db = Entities.get( LoadBalancerSecurityGroup.class );
			try{
				final LoadBalancerSecurityGroup exist = Entities.uniqueResult(LoadBalancerSecurityGroup.named( lb, this.groupOwnerAccountId, groupName));
				db.commit();
				if(exist!=null)
					throw new EventHandlerException("Cleaning-up resources for the loadbalancer with same name");
			}catch(NoSuchElementException ex){
				db.rollback();
			}catch(Exception ex){
				db.rollback();
			}finally {
				if(db.isActive())
					db.rollback();
			}
			
			// check if there's an existing group with the same name
			boolean groupFound = false;
			try{
				List<SecurityGroupItemType> groups = EucalyptusActivityTasks.getInstance().describeSecurityGroups(Lists.newArrayList(groupName));
				if(groups!=null && groups.size()>0){
					final SecurityGroupItemType current = groups.get(0);
					if(groupName.equals(current.getGroupName())){
						groupFound=true;
						this.createdGroupId = current.getGroupId();
						this.groupOwnerAccountId = current.getAccountId();
					}
				}
			}catch(Exception ex){
				groupFound=false;
			}
			
			// create a new security group
			if(! groupFound){
				try{
					EucalyptusActivityTasks.getInstance().createSecurityGroup(groupName, groupDesc);
					createdGroup = groupName;
					List<SecurityGroupItemType> groups = EucalyptusActivityTasks.getInstance().describeSecurityGroups(Lists.newArrayList(groupName));
					if(groups!=null && groups.size()>0){
						final SecurityGroupItemType current = groups.get(0);
						if(groupName.equals(current.getGroupName())){
							this.groupOwnerAccountId = current.getAccountId();
							this.createdGroupId= current.getGroupId();
						}
					}
				}catch(Exception ex){
					throw new EventHandlerException("Failed to create the security group for loadbalancer", ex);
				}
			}else{
				createdGroup = groupName;
			}
			
			if(this.createdGroup == null || this.groupOwnerAccountId == null)
				throw new EventHandlerException("Failed to create the security group for loadbalancer");
	
			db = Entities.get( LoadBalancerSecurityGroup.class );
			try{
				Entities.uniqueResult(LoadBalancerSecurityGroup.named( lb, this.groupOwnerAccountId, groupName));
				db.commit();
			}catch(NoSuchElementException ex){
				final LoadBalancerSecurityGroup newGroup = LoadBalancerSecurityGroup.named( lb, this.groupOwnerAccountId, groupName);
				LoadBalancerSecurityGroup written = Entities.persist(newGroup);
				db.commit();
			}catch(Exception ex){
				db.rollback();
				throw new EventHandlerException("Error while persisting security group", ex);
			}finally {
				if(db.isActive())
					db.rollback();
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
				lb = LoadBalancers.getLoadbalancer(this.event.getContext(), this.event.getLoadBalancer());
			}catch(NoSuchElementException ex){
				return;
			}catch(Exception ex){
				return;
			}
			
			try{
				EucalyptusActivityTasks.getInstance().deleteSecurityGroup(this.createdGroup);
			}catch(Exception ex){
				; // when there's any servo instance referencing the security group
				        // SecurityGroupCleanup will clean up records
			}
			
			final EntityTransaction db = Entities.get( LoadBalancerSecurityGroup.class );
			try{
				final LoadBalancerSecurityGroup group =
						Entities.uniqueResult(LoadBalancerSecurityGroup.named( lb, this.groupOwnerAccountId, this.createdGroup));
				group.setState(LoadBalancerSecurityGroup.STATE.OutOfService);
				group.setLoadBalancer(null);
				Entities.persist(group);
				db.commit();
			}catch(NoSuchElementException ex){
				db.rollback();
			}catch(Exception ex){
				db.rollback();
				LOG.error("failed to mark the security group OutOfService", ex);
			}finally {
				if(db.isActive())
					db.rollback();
			}
		}

		@Override
		public List<String> getResult() {
			// TODO Auto-generated method stub
			List<String> result = Lists.newArrayList();
			if(this.createdGroup != null)
				result.add(this.createdGroup);
			if(this.createdGroupId != null)
				result.add(this.createdGroupId);
			return result;
		}
	}
	
	public static class TagCreator extends AbstractEventHandler<NewLoadbalancerEvent> {
		public static final String TAG_KEY = "Name";
		public static final String TAG_VALUE = "loadbalancer-resources";
		
		private String sgroup = null;
		protected TagCreator(EventHandlerChain<NewLoadbalancerEvent> chain) {
			super(chain);
		}

		@Override
		public void apply(NewLoadbalancerEvent evt)
				throws EventHandlerException {
			// security group
			try{
				StoredResult<String> result =
						this.chain.findHandler(SecurityGroupSetup.class);
				sgroup = result.getResult().get(1); // get(0) = group name, get(1)= id
			}catch(final Exception ex){
				LOG.warn("could not find the security group for the loadbalancer", ex);
				sgroup = null;
			}
			
			if(sgroup!=null){
				try{
					EucalyptusActivityTasks.getInstance().createTags(TAG_KEY, TAG_VALUE, Lists.newArrayList(sgroup));
				}catch(final Exception ex){
					LOG.warn("could not tag the security group", ex);
				}
			}
		}

		@Override
		public void rollback() throws EventHandlerException {
			if(this.sgroup!=null){
				try{
					EucalyptusActivityTasks.getInstance().deleteTags(TAG_KEY, TAG_VALUE, Lists.newArrayList(sgroup));
				}catch(final Exception ex){
					;
				}
			}
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
			          Topology.isEnabledLocally( LoadBalancingBackend.class ) &&
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
				}finally {
					if(db.isActive())
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
						}finally {
							if(db.isActive())
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
				}finally {
					if(db.isActive())
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
									final LoadBalancerAutoScalingGroup group= allGroupMap.get(asg.getAutoScalingGroupName());
									if(group==null)
										throw new IllegalArgumentException("The group with name "+ asg.getAutoScalingGroupName()+ " not found in the database");
									
									final LoadBalancerCoreView lbView = group.getLoadBalancer();
									LoadBalancer lb = null;
									try{
										lb=LoadBalancerEntityTransform.INSTANCE.apply(lbView);
									}catch(final Exception ex){
										LOG.error("unable to transfrom loadbalancer from the viewer", ex);
										throw ex;
									}
									
									LoadBalancerZoneCoreView zoneView = null;
									for(final LoadBalancerZoneCoreView z : lb.getZones()){
										if(z.getName().equals(instance.getAvailabilityZone())){
											zoneView = z;
											break;
										}
									}
									if(zoneView == null)
										throw new Exception("No availability zone with name="+instance.getAvailabilityZone()+" found for loadbalancer "+lb.getDisplayName());
									final LoadBalancerSecurityGroupCoreView sgroupView = lb.getGroup();
									if(sgroupView == null)
										throw new Exception("No security group is found for loadbalancer "+lb.getDisplayName());
									final LoadBalancerDnsRecordCoreView dnsView = lb.getDns();
									
									LoadBalancerZone zone = null;
									LoadBalancerSecurityGroup sgroup = null;
									LoadBalancerDnsRecord dns = null;
									try{
										zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
										sgroup = LoadBalancerSecurityGroupEntityTransform.INSTANCE.apply(sgroupView);
										dns = LoadBalancerDnsRecordEntityTransform.INSTANCE.apply(dnsView);
									}catch(final Exception ex){
										LOG.error("unable to transform entity", ex);
										throw ex;
									}
									
									final LoadBalancerServoInstance newInstance = 
											LoadBalancerServoInstance.newInstance(zone, sgroup, dns, group, instanceId);
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
					}finally {
						if(db.isActive())
							db.rollback();
					}
				}
				
				List<LoadBalancerServoInstanceCoreView> servoRecords = Lists.newArrayList();
				for(String groupName : groupToQuery.keySet()){
					final LoadBalancerAutoScalingGroup group = groupToQuery.get(groupName);
					servoRecords.addAll(group.getServos());
				}
				
				//final List<LoadBalancerServoInstance> registerDnsARec = Lists.newArrayList();
				for(LoadBalancerServoInstanceCoreView instanceView : servoRecords){
					/// CASE 2: EXISTING SERVO INSTANCES ARE NOT FOUND IN THE QUERY RESPONSE 
					if(! foundInstances.containsKey(instanceView.getInstanceId()) && 
							! instanceView.getState().equals(LoadBalancerServoInstance.STATE.Retired)){
						LoadBalancerServoInstance instance = null;
						try{
							instance = LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(instanceView);
						}catch(final Exception ex){
							LOG.error("unable to transform servo instance from the view", ex);
							continue;
						}
						
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
						}finally {
							if(db.isActive())
								db.rollback();
						}
					}else{/// CASE 3: INSTANCE STATE UPDATED
						Instance instanceCurrent = foundInstances.get(instanceView.getInstanceId());
						final String healthState = instanceCurrent.getHealthStatus();
						final String lifecycleState = instanceCurrent.getLifecycleState();
						LoadBalancerServoInstance.STATE curState = instanceView.getState();
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
						
						if(!curState.equals(LoadBalancerServoInstance.STATE.Retired) && 
								!curState.equals(newState)){
							LoadBalancerServoInstance instance = null;
							try{
								instance = LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(instanceView);
							}catch(final Exception ex){
								LOG.error("unable to transform servo instance from the view", ex);
								continue;
							}	

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
							}finally {
								if(db.isActive())
									db.rollback();
							}
						}
					}
				}
			}	
		}
	}
}

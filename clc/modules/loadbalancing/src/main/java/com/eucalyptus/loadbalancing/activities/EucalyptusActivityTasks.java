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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.autoscaling.activities.DispatchingClient;
import com.eucalyptus.autoscaling.activities.EucalyptusClient;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.AutoScalingGroupNames;
import com.eucalyptus.autoscaling.common.AutoScalingMessage;
import com.eucalyptus.autoscaling.common.AvailabilityZones;
import com.eucalyptus.autoscaling.common.CreateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.CreateLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.DeleteAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.DeleteLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.SecurityGroups;
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration;
import com.eucalyptus.cloudwatch.CloudWatch;
import com.eucalyptus.cloudwatch.CloudWatchMessage;
import com.eucalyptus.cloudwatch.MetricData;
import com.eucalyptus.cloudwatch.PutMetricDataResponseType;
import com.eucalyptus.cloudwatch.PutMetricDataType;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EmpyreanMessage;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.AddMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.AddMultiARecordType;
import edu.ucsb.eucalyptus.msgs.AuthorizeSecurityGroupIngressResponseType;
import edu.ucsb.eucalyptus.msgs.AuthorizeSecurityGroupIngressType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.CreateMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.CreateMultiARecordType;
import edu.ucsb.eucalyptus.msgs.CreateSecurityGroupResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSecurityGroupType;
import edu.ucsb.eucalyptus.msgs.DeleteSecurityGroupResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteSecurityGroupType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.DnsMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.IpPermissionType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiANameResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiANameType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiARecordType;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.RevokeSecurityGroupIngressResponseType;
import edu.ucsb.eucalyptus.msgs.RevokeSecurityGroupIngressType;
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType;
import edu.ucsb.eucalyptus.msgs.TerminateInstancesItemType;
/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
public class EucalyptusActivityTasks {
	private static final Logger LOG = Logger.getLogger( EucalyptusActivityTask.class );
	private EucalyptusActivityTasks() {}
	private static  EucalyptusActivityTasks _instance = new EucalyptusActivityTasks();
	public static EucalyptusActivityTasks getInstance(){
		return _instance;
	}

	private interface ActivityContext<TM extends BaseMessage, TC extends ComponentId> {
	  String getUserId();
	  DispatchingClient<TM, TC> getClient();
	}
	
	private class AutoScalingSystemActivity implements ActivityContext<AutoScalingMessage, AutoScaling>{
		private AutoScalingSystemActivity(){	}

		@Override
		public String getUserId() {
			try{
				return Accounts.lookupSystemAdmin().getUserId();
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}

		@Override
		public DispatchingClient<AutoScalingMessage, AutoScaling> getClient() {
			try{
				final AutoScalingClient client = new AutoScalingClient(this.getUserId());
				client.init();
				return client;
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
		
	}
	
	private class CloudWatchUserActivity implements ActivityContext<CloudWatchMessage, CloudWatch>{
		private String userId = null;
		private CloudWatchUserActivity(final String userId){
			this.userId = userId;
		}
		
		@Override
		public String getUserId() {
			return this.userId;
		}

		@Override
		public DispatchingClient<CloudWatchMessage, CloudWatch> getClient() {
			try{
				final CloudWatchClient client = new CloudWatchClient(this.getUserId());
				client.init();
				return client;
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}
	
	private class DnsSystemActivity implements ActivityContext<DnsMessage, Dns> {

		@Override
		public String getUserId() {
			try{
				return Accounts.lookupSystemAdmin().getUserId();
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}

		@Override
		public DispatchingClient<DnsMessage, Dns> getClient() {
			try{
				final DnsClient client = new DnsClient(this.getUserId());
				client.init();
				return client;
			}catch(Exception e){
				throw Exceptions.toUndeclared(e);
			}
		}
		
	}
	
	private class EmpyreanSystemActivity implements ActivityContext<EmpyreanMessage, Empyrean>{

		@Override
		public String getUserId() {
			try{
				return Accounts.lookupSystemAdmin( ).getUserId();
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}

		@Override
		public DispatchingClient<EmpyreanMessage, Empyrean> getClient() {
			// TODO Auto-generated method stub
			try{
				final EmpyreanClient client = 
						new EmpyreanClient(this.getUserId());
				client.init();
				return client;
			}catch(Exception e){
				throw Exceptions.toUndeclared(e);
			}
		}
	}
	private class EucalyptusSystemActivity implements ActivityContext<EucalyptusMessage, Eucalyptus>{
		@Override
		public String getUserId(){
			// TODO: SPARK: Impersonation?
			try{
				/// ASSUMING LB SERVICE HAS ACCESS TO LOCAL DB
				return Accounts.lookupSystemAdmin( ).getUserId();
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
		
		@Override
		public DispatchingClient<EucalyptusMessage, Eucalyptus> getClient(){
			 try {
			     // final DispatchingClient<BaseMessage, ComponentId> client = 
				 final EucalyptusClient client = 
			    		  new EucalyptusClient( this.getUserId() );
			      client.init();
			      return client;
			    } catch ( Exception e ) {
			      throw Exceptions.toUndeclared( e );
			    }
		}
	}

	private class EucalyptusUserActivity implements ActivityContext<EucalyptusMessage, Eucalyptus>{
		private String userId = null;
		private EucalyptusUserActivity(final String userId){
			this.userId = userId;
		}
		
		@Override
		public String getUserId() {
			// TODO Auto-generated method stub
			return this.userId;
		}

		@Override
		public DispatchingClient<EucalyptusMessage, Eucalyptus> getClient() {
			// TODO Auto-generated method stub
			try{
				EucalyptusClient client = new EucalyptusClient(this.userId);
				client.init();
				return client;
			}catch(Exception e){
				throw Exceptions.toUndeclared(e);
			}
		}
		
	}
	
	public List<String> launchInstances(final String availabilityZone, final String imageId, 
			final String instanceType, final int numInstances){
		return launchInstances(availabilityZone, imageId, instanceType, null,  null, numInstances);
	}
	
	public List<String> launchInstances(final String availabilityZone, final String imageId, 
				final String instanceType, String groupName, final String userData, final int numInstances){
		LOG.info("launching instances at zone="+availabilityZone+", imageId="+imageId+", group="+groupName);
		final EucalyptusLaunchInstanceTask launchTask 
			= new EucalyptusLaunchInstanceTask(availabilityZone, imageId, instanceType, numInstances);
		if(userData!=null)
			launchTask.setUserData(userData);
		if(groupName != null)
			launchTask.setSecurityGroup(groupName);
		final CheckedListenableFuture<Boolean> result = launchTask.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				final List<String> instances = launchTask.getInstanceIds();
				return instances;
			}else
				throw new EucalyptusActivityException("failed to launch the instance");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public List<String> terminateInstances(final List<String> instances){
		LOG.info(String.format("terminating %d instances", instances.size()));
		if(instances.size() <=0)
			return instances;
		
		final EucalyptusTerminateInstanceTask terminateTask = new EucalyptusTerminateInstanceTask(instances);
		final CheckedListenableFuture<Boolean> result = terminateTask.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				final List<String> terminated = terminateTask.getTerminatedInstances();
				return terminated;
			}else
				throw new EucalyptusActivityException("failed to terminate the instances");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	
	public List<RunningInstancesItemType> describeSystemInstances(final List<String> instances){
		if(instances.size() <=0)
			return Lists.newArrayList();
		final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances);
		final CheckedListenableFuture<Boolean> result = describeTask.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				final List<RunningInstancesItemType> describe = describeTask.getResult();
				return describe;
			}else
				throw new EucalyptusActivityException("failed to describe the instances");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public List<RunningInstancesItemType> describeUserInstances(final String userId, final List<String> instances){
		if(instances.size() <=0)
			return Lists.newArrayList();
		final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances);
		final CheckedListenableFuture<Boolean> result = describeTask.dispatch(new EucalyptusUserActivity(userId));

		try{
			if(result.get()){
				final List<RunningInstancesItemType> describe = describeTask.getResult();
				return describe;
			}else
				throw new EucalyptusActivityException("failed to describe the instances");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	
	public List<ServiceStatusType> describeServices(final String componentType){
		//LOG.info("calling describe-services -T "+componentType);
		final EucalyptusDescribeServicesTask serviceTask = new EucalyptusDescribeServicesTask(componentType);
		final CheckedListenableFuture<Boolean> result = serviceTask.dispatch(new EmpyreanSystemActivity());
		try{
			if(result.get()){
				return serviceTask.getServiceDetais();
			}else
				throw new EucalyptusActivityException("failed to describe services");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public List<ClusterInfoType> describeAvailabilityZones(){
		final EucalyptusDescribeAvailabilityZonesTask task = new EucalyptusDescribeAvailabilityZonesTask();
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				final List<ClusterInfoType> describe = task.getAvailabilityZones();
				return describe;
			}else
				throw new EucalyptusActivityException("failed to describe the availability zones");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}

	}
	
	public void createSecurityGroup(String groupName, String groupDesc){
		final EucalyptusCreateGroupTask task = new EucalyptusCreateGroupTask(groupName, groupDesc);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get() && task.getGroupId()!=null){
				return;
			}else
				throw new EucalyptusActivityException("failed to create the group "+groupName);
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void deleteSecurityGroup(String groupName){
		final EucalyptusDeleteGroupTask task = new EucalyptusDeleteGroupTask(groupName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to delete the group "+groupName);
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		} 
	}
	
	public void authorizeSecurityGroup(String groupName, String protocol, int portNum){
		final EucalyptusAuthorizeIngressRuleTask task = new EucalyptusAuthorizeIngressRuleTask(groupName, protocol, portNum);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException(String.format("failed to authorize:%s, %s, %d ", groupName, protocol, portNum));
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		} 
	}
	
	public void revokeSecurityGroup(String groupName, String protocol, int portNum){
		final EucalyptusRevokeIngressRuleTask task = new EucalyptusRevokeIngressRuleTask(groupName, protocol, portNum);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException(String.format("failed to revoke:%s, %s, %d ", groupName, protocol, portNum));
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		} 
	}
	
	
	public void createARecord(String zone, String name){
		final DnsCreateNameRecordTask task = new DnsCreateNameRecordTask(zone, name);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new DnsSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to create multi A record ");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		} 
	}
	
	public void addARecord(String zone, String name, String address){
		final DnsAddARecordTask task = new DnsAddARecordTask(zone, name, address);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new DnsSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to add A record ");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		} 
	}
	
	public void removeARecord(String zone, String name, String address){
		final DnsRemoveARecordTask task = new DnsRemoveARecordTask(zone, name, address);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new DnsSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to remove A record ");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}  
	}
	
	public void removeMultiARecord(String zone, String name){
		final DnsRemoveMultiARecordTask task = new DnsRemoveMultiARecordTask(zone, name);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new DnsSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to remove multi A records ");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}  
	}
	
	public void putCloudWatchMetricData(final String userId, final String namespace, final MetricData data){
		final CloudWatchPutMetricDataTask task = new CloudWatchPutMetricDataTask(namespace, data);

		final CheckedListenableFuture<Boolean> result = task.dispatch(new CloudWatchUserActivity(userId));
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to remove multi A records");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void createLaunchConfiguration(final String imageId, final String instanceType, final String launchConfigName,
			final String securityGroup, final String userData){
		final AutoScalingCreateLaunchConfigTask task = 
				new AutoScalingCreateLaunchConfigTask(imageId, instanceType, launchConfigName, securityGroup, userData);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to create launch configuration");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void createAutoScalingGroup(final String groupName, final List<String> availabilityZones, final int capacity, final String launchConfigName){
		final AutoScalingCreateGroupTask task =
				new AutoScalingCreateGroupTask(groupName, availabilityZones, capacity, launchConfigName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to create autoscaling group");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void deleteLaunchConfiguration(final String launchConfigName){
		final AutoScalingDeleteLaunchConfigTask task =
				new AutoScalingDeleteLaunchConfigTask(launchConfigName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to delete launch configuration");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void deleteAutoScalingGroup(final String groupName, final boolean terminateInstances){
		final AutoScalingDeleteGroupTask task = 
				new AutoScalingDeleteGroupTask(groupName, terminateInstances);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to delete autoscaling group");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups(final List<String> groupNames){
		final AutoScalingDescribeGroupsTask task =
				new AutoScalingDescribeGroupsTask(groupNames);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
		try{
			if(result.get()){
				return task.getResponse();
			}else
				throw new EucalyptusActivityException("failed to describe autoscaling groups");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	private class AutoScalingDescribeGroupsTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private List<String> groupNames = null;
		private DescribeAutoScalingGroupsResponseType response = null;
		private AutoScalingDescribeGroupsTask(final List<String> groupNames){
			this.groupNames = groupNames;
		}
		
		private DescribeAutoScalingGroupsType describeAutoScalingGroup(){
			final DescribeAutoScalingGroupsType req = new DescribeAutoScalingGroupsType();
			final AutoScalingGroupNames names = new AutoScalingGroupNames();
			names.setMember(Lists.<String>newArrayList());
			names.getMember().addAll(this.groupNames);
			req.setAutoScalingGroupNames(names);
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				Checked<AutoScalingMessage> callback) {
			final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
			client.dispatch(describeAutoScalingGroup(), callback);					
		}

		@Override
		void dispatchSuccess(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				AutoScalingMessage response) {
			this.response = (DescribeAutoScalingGroupsResponseType) response;
		}
		
		public DescribeAutoScalingGroupsResponseType getResponse(){
			return this.response;
		}
	}
	
	private class AutoScalingCreateGroupTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String groupName = null;
		private List<String> availabilityZones = null;
		private int capacity = 1;
		private String launchConfigName = null;
		
		private AutoScalingCreateGroupTask(final String groupName, final List<String> zones, final int capacity, final String launchConfig){
			this.groupName = groupName;
			this.availabilityZones = zones;
			this.capacity = capacity;
			this.launchConfigName = launchConfig;
		}
		
		private CreateAutoScalingGroupType createAutoScalingGroup(){
			final CreateAutoScalingGroupType req = new CreateAutoScalingGroupType();
			req.setAutoScalingGroupName(this.groupName);
			AvailabilityZones zones = new AvailabilityZones();
			zones.setMember(Lists.<String>newArrayList());
			zones.getMember().addAll(this.availabilityZones);
			req.setAvailabilityZones(zones);
			req.setDesiredCapacity(this.capacity);
			req.setMaxSize(this.capacity);
			req.setMinSize(this.capacity);
			req.setHealthCheckType("EC2");
			req.setLaunchConfigurationName(this.launchConfigName);
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				Checked<AutoScalingMessage> callback) {
			final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
			client.dispatch(createAutoScalingGroup(), callback);			
		}

		@Override
		void dispatchSuccess(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				AutoScalingMessage response) {
			CreateAutoScalingGroupResponseType resp = (CreateAutoScalingGroupResponseType) response;
		}
	}
	
	private class AutoScalingDeleteGroupTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String groupName = null;
		private boolean terminateInstances = false;
		private AutoScalingDeleteGroupTask(final String groupName, final boolean terminateInstances){
			this.groupName = groupName;
			this.terminateInstances = terminateInstances;
		}
		
		private DeleteAutoScalingGroupType deleteAutoScalingGroup(){
			final DeleteAutoScalingGroupType req = new DeleteAutoScalingGroupType();
			req.setAutoScalingGroupName(this.groupName);
			req.setForceDelete(this.terminateInstances);
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				Checked<AutoScalingMessage> callback) {
			final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
			client.dispatch(deleteAutoScalingGroup(), callback);			
		}

		@Override
		void dispatchSuccess(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				AutoScalingMessage response) {
			final DeleteAutoScalingGroupResponseType resp = (DeleteAutoScalingGroupResponseType) response;
		}
	}
	
	private class AutoScalingDeleteLaunchConfigTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String launchConfigName = null;
		private AutoScalingDeleteLaunchConfigTask(final String launchConfigName){
			this.launchConfigName = launchConfigName;
		}
		
		private DeleteLaunchConfigurationType deleteLaunchConfiguration(){
			final DeleteLaunchConfigurationType req = new DeleteLaunchConfigurationType();
			req.setLaunchConfigurationName(this.launchConfigName);
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				Checked<AutoScalingMessage> callback) {
			final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
			client.dispatch(deleteLaunchConfiguration(), callback);
		}

		@Override
		void dispatchSuccess(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				AutoScalingMessage response) {
			final DeleteLaunchConfigurationResponseType resp = (DeleteLaunchConfigurationResponseType) response;
		}
	}
	private class AutoScalingCreateLaunchConfigTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String imageId=null;
		private String instanceType = null;
		private String launchConfigName = null;
		private String securityGroup = null;
		private String userData = null;
		private AutoScalingCreateLaunchConfigTask(final String imageId, final String instanceType, 
				final String launchConfigName, final String sgroupName, final String userData){
			this.imageId = imageId;
			this.instanceType = instanceType;
			this.launchConfigName = launchConfigName;
			this.securityGroup = sgroupName;
			this.userData = userData;
		}
		
		private CreateLaunchConfigurationType createLaunchConfiguration(){
			final CreateLaunchConfigurationType req = new CreateLaunchConfigurationType();
			req.setImageId(this.imageId);
			req.setInstanceType(this.instanceType);
			req.setLaunchConfigurationName(this.launchConfigName);
			SecurityGroups groups = new SecurityGroups();
			groups.setMember(Lists.<String>newArrayList());
			groups.getMember().add(this.securityGroup);
			req.setSecurityGroups(groups);
			req.setUserData(userData);
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				Checked<AutoScalingMessage> callback) {
			final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
			client.dispatch(createLaunchConfiguration(), callback);
		}

		@Override
		void dispatchSuccess(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				AutoScalingMessage response) {
			final CreateLaunchConfigurationResponseType resp = (CreateLaunchConfigurationResponseType) response;
		}
	}

	private class CloudWatchPutMetricDataTask extends EucalyptusActivityTask<CloudWatchMessage, CloudWatch>{
		private MetricData metricData = null;
		private String namespace = null;
		private CloudWatchPutMetricDataTask( final String namespace, final MetricData data){
			this.namespace = namespace;
			this.metricData = data;
		}
		
		private PutMetricDataType putMetricData(){
			final PutMetricDataType request = new PutMetricDataType();
			request.setNamespace(this.namespace);
			request.setMetricData(this.metricData);
			return request;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<CloudWatchMessage, CloudWatch> context,
				Checked<CloudWatchMessage> callback) {
			final DispatchingClient<CloudWatchMessage, CloudWatch> client = context.getClient();
			client.dispatch(putMetricData(), callback);
			
		}

		@Override
		void dispatchSuccess(
				ActivityContext<CloudWatchMessage, CloudWatch> context,
				CloudWatchMessage response) {
			// TODO Auto-generated method stub
			final PutMetricDataResponseType resp = (PutMetricDataResponseType) response;
		}
		
	}

	private class EucalyptusDescribeAvailabilityZonesTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		private List<ClusterInfoType> zones = null; 
		private EucalyptusDescribeAvailabilityZonesTask(){
		}
		
		private DescribeAvailabilityZonesType describeAvailabilityZones(){
			final DescribeAvailabilityZonesType req = new DescribeAvailabilityZonesType();
		    return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage,Eucalyptus> client = context.getClient();
			client.dispatch(describeAvailabilityZones(), callback);
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context, EucalyptusMessage response) {
			// TODO Auto-generated method stub
			final DescribeAvailabilityZonesResponseType resp = (DescribeAvailabilityZonesResponseType) response;
			zones = resp.getAvailabilityZoneInfo();
		}
		
		public List<ClusterInfoType> getAvailabilityZones(){
			return this.zones;
		}
	}
	
	private class EucalyptusDescribeServicesTask extends EucalyptusActivityTask<EmpyreanMessage, Empyrean> {
		private String componentType = null;
		private List<ServiceStatusType> services = null; 
		private EucalyptusDescribeServicesTask(final String componentType){
			this.componentType = componentType;
		}
		
		private DescribeServicesType describeServices(){
			final DescribeServicesType req = new DescribeServicesType();
		    req.setByServiceType(this.componentType);
		    return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EmpyreanMessage, Empyrean> context,
				Checked<EmpyreanMessage> callback) {
			final DispatchingClient<EmpyreanMessage,Empyrean> client = context.getClient();
			client.dispatch(describeServices(), callback);
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EmpyreanMessage, Empyrean> context, EmpyreanMessage response) {
			// TODO Auto-generated method stub
			final DescribeServicesResponseType resp = (DescribeServicesResponseType) response;
			this.services = resp.getServiceStatuses();
		}
		
		public List<ServiceStatusType> getServiceDetais(){
			return this.services;
		}
	}
		
	/// create new {name - {address1}} mapping
	private class DnsCreateNameRecordTask extends EucalyptusActivityTask<DnsMessage, Dns>{
		private String zone = null;
		private String name = null;

		private DnsCreateNameRecordTask(final String zone, final String name){
			this.zone = zone;
			this.name = name;
		}
		private CreateMultiARecordType createNameRecord(){
			final CreateMultiARecordType req = new CreateMultiARecordType();
			req.setZone(this.zone);
			req.setName(this.name);
			req.setTtl(86400);
			return req;
		}
		
		@Override
		void dispatchInternal(ActivityContext<DnsMessage, Dns> context,
				Checked<DnsMessage> callback) {

			final DispatchingClient<DnsMessage, Dns> client = context.getClient();
			client.dispatch(createNameRecord(), callback);						
		}

		@Override
		void dispatchSuccess(ActivityContext<DnsMessage, Dns> context,
				DnsMessage response) {
			// TODO Auto-generated method stub
			final CreateMultiARecordResponseType resp = (CreateMultiARecordResponseType) response;
		}
	}
	
	/// add {name-address} mapping into an existing {name - {addr1, addr2, etc } } map
	private class DnsAddARecordTask extends EucalyptusActivityTask<DnsMessage, Dns>{
		private String zone = null;
		private String name = null;
		private String address = null;
		private DnsAddARecordTask(final String zone, final String name, final String address){
			this.zone = zone;
			this.name = name;
			this.address = address;
		}
		private AddMultiARecordType addARecord(){
			final AddMultiARecordType req = new AddMultiARecordType();
			req.setZone(this.zone);
			req.setName(this.name);
			req.setAddress(this.address);
			req.setTtl(86400);
			return req;
		}
		
		@Override
		void dispatchInternal(ActivityContext<DnsMessage, Dns> context,
				Checked<DnsMessage> callback) {

			final DispatchingClient<DnsMessage, Dns> client = context.getClient();
			client.dispatch(addARecord(), callback);						
		}

		@Override
		void dispatchSuccess(ActivityContext<DnsMessage, Dns> context,
				DnsMessage response) {
			// TODO Auto-generated method stub
			final AddMultiARecordResponseType resp = (AddMultiARecordResponseType) response;
		}
	}
	
	/// delete one name-address mapping from existing {name - {addr1, addr2, etc } } map
	private class DnsRemoveARecordTask extends EucalyptusActivityTask<DnsMessage, Dns>{
		private String zone = null;
		private String name = null;
		private String address = null;
		private DnsRemoveARecordTask(final String zone, final String name, final String address){
			this.zone = zone;
			this.name = name;
			this.address = address;
		}
		private RemoveMultiARecordType removeARecord(){
			final RemoveMultiARecordType req = new RemoveMultiARecordType();
			req.setZone(this.zone);
			req.setName(this.name);
			req.setAddress(this.address);
			return req;
		}
		
		@Override
		void dispatchInternal(ActivityContext<DnsMessage, Dns> context,
				Checked<DnsMessage> callback) {

			final DispatchingClient<DnsMessage, Dns> client = context.getClient();
			client.dispatch(removeARecord(), callback);						
		}

		@Override
		void dispatchSuccess(ActivityContext<DnsMessage, Dns> context,
				DnsMessage response) {
			// TODO Auto-generated method stub
			final RemoveMultiARecordResponseType resp = (RemoveMultiARecordResponseType) response;
		}
	}
	
	/// delete name - {addr1, addr2, addr3, etc} mapping entirely
	private class DnsRemoveMultiARecordTask extends EucalyptusActivityTask<DnsMessage, Dns>{
		private String zone = null;
		private String name = null;
		private DnsRemoveMultiARecordTask(final String zone, final String name){
			this.zone = zone;
			this.name = name;
		}
		private RemoveMultiANameType removeARecord(){
			final RemoveMultiANameType req = new RemoveMultiANameType();
			req.setZone(this.zone);
			req.setName(this.name);
			return req;
		}
		
		@Override
		void dispatchInternal(ActivityContext<DnsMessage, Dns> context,
				Checked<DnsMessage> callback) {

			final DispatchingClient<DnsMessage, Dns> client = context.getClient();
			client.dispatch(removeARecord(), callback);						
		}

		@Override
		void dispatchSuccess(ActivityContext<DnsMessage, Dns> context,
				DnsMessage response) {
			// TODO Auto-generated method stub
			final RemoveMultiANameResponseType resp = (RemoveMultiANameResponseType) response;
		}
	}
	private class EucalyptusDescribeInstanceTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		private final List<String> instanceIds;
		private final AtomicReference<List<RunningInstancesItemType>> result =
				new AtomicReference<List<RunningInstancesItemType>>();
		private EucalyptusDescribeInstanceTask(final List<String> instanceId){
			this.instanceIds = instanceId;
		}
		private DescribeInstancesType describeInstances(){
			final DescribeInstancesType req = new DescribeInstancesType();
			req.setInstancesSet(Lists.newArrayList(this.instanceIds));
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(describeInstances(), callback);			
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				EucalyptusMessage response) {
			final DescribeInstancesResponseType resp = (DescribeInstancesResponseType) response;
			final List<RunningInstancesItemType> resultInstances = Lists.newArrayList();
			for(final ReservationInfoType res : resp.getReservationSet()){
				resultInstances.addAll(res.getInstancesSet());
			}
			this.result.set(resultInstances);
		}
		
		public List<RunningInstancesItemType> getResult(){
			return this.result.get();
		}
	}
	
	private class EucalyptusTerminateInstanceTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
		private final List<String> instanceIds;
		private final AtomicReference<List<String>> terminatedIds = new AtomicReference<List<String>>();
		private EucalyptusTerminateInstanceTask(final List<String> instanceId){
			this.instanceIds = instanceId;
		}
		private TerminateInstancesType terminateInstances(){
			final TerminateInstancesType req = new TerminateInstancesType();
			req.setInstancesSet(Lists.newArrayList(this.instanceIds));
			return req;
		}
		 
		@Override
		void dispatchInternal( ActivityContext<EucalyptusMessage,Eucalyptus> context, Callback.Checked<EucalyptusMessage> callback){
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(terminateInstances(), callback);
		}
		    
		
		@Override
		void dispatchSuccess( ActivityContext<EucalyptusMessage,Eucalyptus> context, EucalyptusMessage response ){
			TerminateInstancesResponseType resp = (TerminateInstancesResponseType) response;
			this.terminatedIds.set(Lists.transform(resp.getInstancesSet(), 
					new Function<TerminateInstancesItemType, String>(){
						@Override
						public String apply(TerminateInstancesItemType item){
							return item.getInstanceId();
						}
					}));
		}
		
		List<String> getTerminatedInstances(){
			return this.terminatedIds.get();
		}
	}
	
	//SPARK: TODO: SYSTEM, STATIC MODE?
	private class EucalyptusCreateGroupTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
		private String groupName = null;
		private String groupDesc = null;
		private String groupId = null;
		EucalyptusCreateGroupTask(String groupName, String groupDesc){
			this.groupName = groupName;
			this.groupDesc = groupDesc;
		}
		private CreateSecurityGroupType createSecurityGroup(){
			final CreateSecurityGroupType req = new CreateSecurityGroupType();
			req.setGroupName(this.groupName);
			req.setGroupDescription(this.groupDesc);
			return req;
		}
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(createSecurityGroup(), callback);			
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				EucalyptusMessage response) {
			final CreateSecurityGroupResponseType resp = (CreateSecurityGroupResponseType) response;
			this.groupId = resp.getGroupId();
		}
		
		public String getGroupId(){
			return this.groupId;
		}
	}
	
	private class EucalyptusAuthorizeIngressRuleTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		String groupName=null;
		String protocol = null;
		int portNum = 1;
		
		EucalyptusAuthorizeIngressRuleTask(String groupName, String protocol, int portNum){
			this.protocol=protocol;
			this.groupName = groupName;
			this.portNum = portNum;
		}
		private AuthorizeSecurityGroupIngressType authorize(){
			AuthorizeSecurityGroupIngressType req = new AuthorizeSecurityGroupIngressType();
			req.setGroupName(this.groupName);
			IpPermissionType perm = new IpPermissionType();
			perm.setToPort(this.portNum);
			perm.setIpRanges(Lists.newArrayList(Arrays.asList("0.0.0.0/0")));
			perm.setIpProtocol(this.protocol); // udp too?
			req.setIpPermissions(Lists.newArrayList(Arrays.asList(perm)));
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(authorize(), callback);						
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				EucalyptusMessage response) {
			final AuthorizeSecurityGroupIngressResponseType resp = (AuthorizeSecurityGroupIngressResponseType) response;
		}
	}
	private class EucalyptusRevokeIngressRuleTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		String groupName=null;
		String protocol=null;
		int portNum = 1;
		EucalyptusRevokeIngressRuleTask(String groupName, String protocol, int portNum){
			this.groupName = groupName;
			this.protocol = protocol;
			this.portNum = portNum;
		}
		private RevokeSecurityGroupIngressType revoke(){
			RevokeSecurityGroupIngressType req = new RevokeSecurityGroupIngressType();
			req.setGroupName(this.groupName);
			IpPermissionType perm = new IpPermissionType();
			perm.setToPort(this.portNum);
			perm.setIpRanges(Lists.newArrayList(Arrays.asList("0.0.0.0/0")));
			perm.setIpProtocol(this.protocol);
			req.setIpPermissions(Lists.newArrayList(Arrays.asList(perm)));
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(revoke(), callback);						
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				EucalyptusMessage response) {
			final RevokeSecurityGroupIngressResponseType resp = (RevokeSecurityGroupIngressResponseType) response;
		}
	}
	
	private class EucalyptusDeleteGroupTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
		private String groupName = null;
		EucalyptusDeleteGroupTask(String groupName){
			this.groupName = groupName;
		}
		private DeleteSecurityGroupType deleteSecurityGroup(){
			final DeleteSecurityGroupType req = new DeleteSecurityGroupType();
			req.setGroupName(this.groupName);
			return req;
		}
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(deleteSecurityGroup(), callback);			
		}
		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				EucalyptusMessage response) {
			final DeleteSecurityGroupResponseType resp = (DeleteSecurityGroupResponseType) response;
		}
	}
	
	private class EucalyptusLaunchInstanceTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		private final String availabilityZone;
		private final String imageId;
		private final String instanceType;
		private String userData = null;
		private String groupName = null;
		private int numInstances = 1;
		private final AtomicReference<List<String>> instanceIds = new AtomicReference<List<String>>(
		    Collections.<String>emptyList()
	    );

		private EucalyptusLaunchInstanceTask(final String availabilityZone, final String imageId, 
				final String instanceType, int numInstances) {
			this.availabilityZone = availabilityZone;
			this.imageId = imageId;
			this.instanceType = instanceType;
			this.numInstances = numInstances;
		}

	    private RunInstancesType runInstances( 
	    		final String availabilityZone,
	            final int attemptToLaunch ) 
	    {
	    	OwnerFullName systemAcct = AccountFullName.getInstance(Principals.systemAccount( ));
	     	LOG.info("runInstances with zone="+availabilityZone+", account="+systemAcct);
	     		       	
		    final LaunchConfiguration launchConfiguration = 
	    		  LaunchConfiguration.create(systemAcct, "launch_config_loadbalacing", 
	    		  this.imageId, this.instanceType);
		    if(groupName != null){
		    	List<String> groups = Lists.newArrayList();
		    	groups.add(groupName);
		    	launchConfiguration.setSecurityGroups(groups);
		    }
		    final RunInstancesType runInstances = TypeMappers.transform( launchConfiguration, RunInstancesType.class );
		    if(availabilityZone != null)
		    	runInstances.setAvailabilityZone( availabilityZone );
		    
		    if(numInstances>1){
		    	runInstances.setMinCount(numInstances);
		    	runInstances.setMaxCount(numInstances);
		    }
		    if(this.userData!=null)
		    	runInstances.setUserData(this.userData);
		    return runInstances;
	    }
	    
	    @Override
	    void dispatchInternal( final ActivityContext<EucalyptusMessage, Eucalyptus> context,
	                           final Callback.Checked<EucalyptusMessage> callback ) {
	      final DispatchingClient<EucalyptusMessage,Eucalyptus> client = context.getClient();
	      client.dispatch( runInstances( availabilityZone, 1 ), callback );
	    }

	    @Override
	    void dispatchSuccess( final ActivityContext<EucalyptusMessage, Eucalyptus> context,
	                          final EucalyptusMessage response ) {
	      final List<String> instanceIds = Lists.newArrayList();
	      RunInstancesResponseType resp = (RunInstancesResponseType) response;
	      for ( final RunningInstancesItemType item : resp.getRsvInfo().getInstancesSet() ) {
	        instanceIds.add( item.getInstanceId() );
	      }

	      this.instanceIds.set( ImmutableList.copyOf( instanceIds ) );
	    }
	    
	    void setUserData(String userData){
	    	this.userData = userData;
	    }
	    
	    void setSecurityGroup(String groupName){
	    	this.groupName = groupName;
	    }
	    List<String> getInstanceIds() {
	      return instanceIds.get();
	    }
	    
	    
	}
	
	private abstract class EucalyptusActivityTask <TM extends BaseMessage, TC extends ComponentId>{
	    private volatile boolean dispatched = false;
	
	    protected EucalyptusActivityTask(){}
	
	    final CheckedListenableFuture<Boolean> dispatch( final ActivityContext<TM,TC> context ) {
	      try {
	        final CheckedListenableFuture<Boolean> future = Futures.newGenericeFuture();
	        dispatchInternal( context, new Callback.Checked<TM>(){
	          @Override
	          public void fireException( final Throwable throwable ) {
	            try {
	              dispatchFailure( context, throwable );
	            } finally {
	              future.set( false );
	            }
	          }
	
	          @Override
	          public void fire( final TM response ) {
	            try {
	              dispatchSuccess( context, response );
	            } finally {
	              future.set( true );
	            }
	          }
	        } );
	        dispatched = true;
	        return future;
	      } catch ( Exception e ) {
	        LOG.error( e, e );
	      }
	      return Futures.predestinedFuture( false );
	    }
	
	    abstract void dispatchInternal( ActivityContext<TM,TC> context, Callback.Checked<TM> callback );
	
	    void dispatchFailure( ActivityContext<TM,TC> context, Throwable throwable ) {
	      LOG.error( "Loadbalancer activity error", throwable );
	    }
	
	    abstract void dispatchSuccess( ActivityContext<TM,TC> context, TM response );
	}
}

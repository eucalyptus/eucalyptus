/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.imaging;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.AddRoleToInstanceProfileResponseType;
import com.eucalyptus.auth.euare.AddRoleToInstanceProfileType;
import com.eucalyptus.auth.euare.CreateInstanceProfileResponseType;
import com.eucalyptus.auth.euare.CreateInstanceProfileType;
import com.eucalyptus.auth.euare.CreateRoleResponseType;
import com.eucalyptus.auth.euare.CreateRoleType;
import com.eucalyptus.auth.euare.DeleteInstanceProfileResponseType;
import com.eucalyptus.auth.euare.DeleteInstanceProfileType;
import com.eucalyptus.auth.euare.DeleteRolePolicyResponseType;
import com.eucalyptus.auth.euare.DeleteRolePolicyType;
import com.eucalyptus.auth.euare.DeleteRoleResponseType;
import com.eucalyptus.auth.euare.DeleteRoleType;
import com.eucalyptus.auth.euare.DeleteServerCertificateResponseType;
import com.eucalyptus.auth.euare.DeleteServerCertificateType;
import com.eucalyptus.auth.euare.EuareMessage;
import com.eucalyptus.auth.euare.GetRolePolicyResponseType;
import com.eucalyptus.auth.euare.GetRolePolicyResult;
import com.eucalyptus.auth.euare.GetRolePolicyType;
import com.eucalyptus.auth.euare.GetServerCertificateResponseType;
import com.eucalyptus.auth.euare.GetServerCertificateType;
import com.eucalyptus.auth.euare.InstanceProfileType;
import com.eucalyptus.auth.euare.ListInstanceProfilesResponseType;
import com.eucalyptus.auth.euare.ListInstanceProfilesType;
import com.eucalyptus.auth.euare.ListRolesResponseType;
import com.eucalyptus.auth.euare.ListRolesType;
import com.eucalyptus.auth.euare.PutRolePolicyResponseType;
import com.eucalyptus.auth.euare.PutRolePolicyType;
import com.eucalyptus.auth.euare.RemoveRoleFromInstanceProfileResponseType;
import com.eucalyptus.auth.euare.RemoveRoleFromInstanceProfileType;
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.auth.euare.UploadServerCertificateResponseType;
import com.eucalyptus.auth.euare.UploadServerCertificateType;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.AutoScalingGroupNames;
import com.eucalyptus.autoscaling.common.AutoScalingMessage;
import com.eucalyptus.autoscaling.common.AvailabilityZones;
import com.eucalyptus.autoscaling.common.CreateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.CreateLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.CreateOrUpdateTagsResponseType;
import com.eucalyptus.autoscaling.common.CreateOrUpdateTagsType;
import com.eucalyptus.autoscaling.common.DeleteAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.DeleteLaunchConfigurationResponseType;
import com.eucalyptus.autoscaling.common.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.DescribeLaunchConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.DescribeLaunchConfigurationsType;
import com.eucalyptus.autoscaling.common.LaunchConfigurationNames;
import com.eucalyptus.autoscaling.common.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.SecurityGroups;
import com.eucalyptus.autoscaling.common.TagType;
import com.eucalyptus.autoscaling.common.Tags;
import com.eucalyptus.autoscaling.common.UpdateAutoScalingGroupResponseType;
import com.eucalyptus.autoscaling.common.UpdateAutoScalingGroupType;
import com.eucalyptus.cloudwatch.CloudWatch;
import com.eucalyptus.cloudwatch.CloudWatchMessage;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.id.Dns;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EmpyreanMessage;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.eucalyptus.vmtypes.DescribeInstanceTypesResponseType;
import com.eucalyptus.vmtypes.DescribeInstanceTypesType;
import com.eucalyptus.vmtypes.VmTypeDetails;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.AuthorizeSecurityGroupIngressResponseType;
import edu.ucsb.eucalyptus.msgs.AuthorizeSecurityGroupIngressType;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.CreateSecurityGroupResponseType;
import edu.ucsb.eucalyptus.msgs.CreateSecurityGroupType;
import edu.ucsb.eucalyptus.msgs.CreateTagsResponseType;
import edu.ucsb.eucalyptus.msgs.CreateTagsType;
import edu.ucsb.eucalyptus.msgs.DeleteResourceTag;
import edu.ucsb.eucalyptus.msgs.DeleteSecurityGroupResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteSecurityGroupType;
import edu.ucsb.eucalyptus.msgs.DeleteTagsResponseType;
import edu.ucsb.eucalyptus.msgs.DeleteTagsType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeImagesType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeInstancesType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsResponseType;
import edu.ucsb.eucalyptus.msgs.DescribeSecurityGroupsType;
import edu.ucsb.eucalyptus.msgs.DescribeTagsType;
import edu.ucsb.eucalyptus.msgs.DnsMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.IpPermissionType;
import edu.ucsb.eucalyptus.msgs.ReservationInfoType;
import edu.ucsb.eucalyptus.msgs.ResourceTag;
import edu.ucsb.eucalyptus.msgs.RevokeSecurityGroupIngressResponseType;
import edu.ucsb.eucalyptus.msgs.RevokeSecurityGroupIngressType;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
import edu.ucsb.eucalyptus.msgs.Filter;
import edu.ucsb.eucalyptus.msgs.TagInfo;
/**
 * @author Sang-Min Park (spark@eucalyptus.com)
 *
 */
// Copy&paste of Loadbalancing.activites.EucalyptusActivityTasks
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
	
	private class EuareSystemActivity implements ActivityContext<EuareMessage, Euare>{
		private EuareSystemActivity(){}

		@Override
		public String getUserId() {
			try{
				return Accounts.lookupSystemAdmin().getUserId();
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}		
		}

		@Override
		public DispatchingClient<EuareMessage, Euare> getClient() {
			try{
				final DispatchingClient<EuareMessage, Euare> client =
					new DispatchingClient<>( this.getUserId(), Euare.class );
				client.init();
				return client;
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}
	
	private class EuareUserActivity implements ActivityContext<EuareMessage, Euare> {
	  private String userId = null;
	  private EuareUserActivity(final String userId){ this.userId = userId; }

	  @Override
    public String getUserId() {
      return this.userId;
    }

    @Override
    public DispatchingClient<EuareMessage, Euare> getClient() {
      try{
        final DispatchingClient<EuareMessage, Euare> client =
          new DispatchingClient<>(this.getUserId(), Euare.class);
        client.init();
        return client;
      }catch(Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
	  
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
				final DispatchingClient<AutoScalingMessage, AutoScaling> client =
					new DispatchingClient<>( this.getUserId(),AutoScaling.class );
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
				final DispatchingClient<CloudWatchMessage, CloudWatch> client =
					new DispatchingClient<>(this.getUserId(), CloudWatch.class);
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
				final DispatchingClient<DnsMessage, Dns> client
					= new DispatchingClient<>(this.getUserId(), Dns.class);
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
			try{
				final DispatchingClient<EmpyreanMessage, Empyrean> client =
						new DispatchingClient<>(this.getUserId(),Empyrean.class);
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
				// ASSUMING THE SERVICE HAS ACCESS TO LOCAL DB
				return Accounts.lookupSystemAdmin( ).getUserId();
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}

		@Override
		public DispatchingClient<EucalyptusMessage, Eucalyptus> getClient(){
			try {
				final DispatchingClient<EucalyptusMessage, Eucalyptus> client =
			    	  new DispatchingClient<>( this.getUserId( ), Eucalyptus.class );
			    client.init();
			    return client;
			} catch ( Exception e ) {
			    throw Exceptions.toUndeclared( e );
			}
		}
	}

	private class EucalyptusBaseSystemActivity implements ActivityContext<BaseMessage, Eucalyptus>{
		@Override
		public String getUserId(){
			try{
				// ASSUMING THE SERVICE HAS ACCESS TO LOCAL DB
				return Accounts.lookupSystemAdmin( ).getUserId();
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}

		@Override
		public DispatchingClient<BaseMessage, Eucalyptus> getClient(){
			try {
				final DispatchingClient<BaseMessage, Eucalyptus> client =
						new DispatchingClient<>( this.getUserId( ), Eucalyptus.class );
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
				final DispatchingClient<EucalyptusMessage, Eucalyptus> client =
					new DispatchingClient<>( this.getUserId( ), Eucalyptus.class );
				client.init();
				return client;
			}catch(Exception e){
				throw Exceptions.toUndeclared(e);
			}
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
	
	public List<VmTypeDetails> describeVMTypes(){
		final EucalyptusDescribeVMTypesTask task = new EucalyptusDescribeVMTypesTask();
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusBaseSystemActivity());
		try{
			if(result.get()){
				final List<VmTypeDetails> describe = task.getVMTypes();
				return describe;
			}else
				throw new EucalyptusActivityException("failed to describe the vm types");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}

	}

	public List<ClusterInfoType> describeAvailabilityZones(boolean verbose){
		final EucalyptusDescribeAvailabilityZonesTask task = new EucalyptusDescribeAvailabilityZonesTask(verbose);
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
	
	public List<SecurityGroupItemType> describeSecurityGroups(List<String> groupNames){
		final EucalyptusDescribeSecurityGroupTask task = new EucalyptusDescribeSecurityGroupTask(groupNames);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				return task.getResult();
			}else
				throw new EucalyptusActivityException("failed to describe security groups");
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
	
	public void createLaunchConfiguration(final String imageId, final String instanceType, final String instanceProfileName, final String launchConfigName,
			final String securityGroup, final String keyName, final String userData){
		final AutoScalingCreateLaunchConfigTask task = 
				new AutoScalingCreateLaunchConfigTask(imageId, instanceType, instanceProfileName, launchConfigName, securityGroup, keyName, userData);
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

	public LaunchConfigurationType describeLaunchConfiguration(final String launchConfigName){
		final AutoScalingDescribeLaunchConfigsTask task =
				new AutoScalingDescribeLaunchConfigsTask(launchConfigName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
		try{
			if(result.get() && task.getResult()!=null){
				return task.getResult();
			}else
				throw new EucalyptusActivityException("failed to describe launch configuration");
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
	
	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final int capacity){
		updateAutoScalingGroup(groupName, zones, capacity, null);
	}
	
	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final int capacity, final String launchConfigName){
		final AutoScalingUpdateGroupTask task=
				new AutoScalingUpdateGroupTask(groupName, zones, capacity, launchConfigName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to enable zones in autoscaling group");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public List<RoleType> listRoles(final String pathPrefix){
		final EuareListRolesTask task = 
				new EuareListRolesTask(pathPrefix);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return task.getRoles();
			}else
				throw new EucalyptusActivityException("failed to list IAM roles");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public RoleType createRole(final String roleName, final String path, final String assumeRolePolicy){
		final EuareCreateRoleTask task =
				new EuareCreateRoleTask(roleName, path, assumeRolePolicy);

		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return task.getRole();
			}else
				throw new EucalyptusActivityException("failed to create IAM role");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public List<DescribeKeyPairsResponseItemType> describeKeyPairs(final List<String> keyNames){
		final EucaDescribeKeyPairsTask task =
				new EucaDescribeKeyPairsTask(keyNames);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				return task.getResult();
			}else
				throw new EucalyptusActivityException("failed to describe keypairs");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public ServerCertificateType getServerCertificate(final String userId, final String certName){
	  final EuareGetServerCertificateTask task =
	      new EuareGetServerCertificateTask(certName);
	  final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareUserActivity(userId));
	  try{
	    if(result.get()){
	      return task.getServerCertificate();
	    }else
	      throw new EucalyptusActivityException("failed to get server certificate");
	  }catch(Exception ex){
	    throw Exceptions.toUndeclared(ex);
	  }
	}
	
	public void deleteRole(final String roleName){
		final EuareDeleteRoleTask task =
				new EuareDeleteRoleTask(roleName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to delete IAM role");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public List<InstanceProfileType> listInstanceProfiles(String pathPrefix){
		final EuareListInstanceProfilesTask task =
				new EuareListInstanceProfilesTask(pathPrefix);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return task.getInstanceProfiles();
			}else
				throw new EucalyptusActivityException("failed to delete IAM role");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public InstanceProfileType createInstanceProfile(String profileName, String path){
		final EuareCreateInstanceProfileTask task =
				new EuareCreateInstanceProfileTask(profileName, path);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return task.getInstanceProfile();
			}else
				throw new EucalyptusActivityException("failed to create IAM instance profile");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void deleteInstanceProfile(String profileName){
		final EuareDeleteInstanceProfileTask task =
				new EuareDeleteInstanceProfileTask(profileName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to delete IAM instance profile");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void addRoleToInstanceProfile(String instanceProfileName, String roleName){
		final EuareAddRoleToInstanceProfileTask task =
				new EuareAddRoleToInstanceProfileTask(instanceProfileName, roleName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to add role to the instance profile");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void removeRoleFromInstanceProfile(String instanceProfileName, String roleName){
	  final EuareRemoveRoleFromInstanceProfileTask task =
	      new EuareRemoveRoleFromInstanceProfileTask(instanceProfileName, roleName);
	   final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
	    try{
	      if(result.get()){
	        return;
	      }else
	        throw new EucalyptusActivityException("failed to remove role from the instance profile");
	    }catch(Exception ex){
	      throw Exceptions.toUndeclared(ex);
	    } 
	}
	
	public GetRolePolicyResult getRolePolicy(String roleName, String policyName){
		final EuareGetRolePolicyTask task =
				new EuareGetRolePolicyTask(roleName, policyName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return task.getResult();
			}else
				throw new EucalyptusActivityException("failed to get role's policy");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void putRolePolicy(String roleName, String policyName, String policyDocument){
		final EuarePutRolePolicyTask task = 
				new EuarePutRolePolicyTask(roleName, policyName, policyDocument);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to put role's policy");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public void deleteRolePolicy(String roleName, String policyName){
		final EuareDeleteRolePolicyTask task =
				new EuareDeleteRolePolicyTask(roleName, policyName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to delete role's policy");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
	
	public List<ImageDetails> describeImages(final List<String> imageIds){
		final EucaDescribeImagesTask task =
				new EucaDescribeImagesTask(imageIds);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				return task.getResult();
			}else
				throw new EucalyptusActivityException("failed to describe keypairs");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);	
		}
	}
	
	public void createTags(final String tagKey, final String tagValue, final List<String> resources){
		final EucaCreateTagsTask task =
				new EucaCreateTagsTask(tagKey, tagValue, resources);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to create tags");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);	
		}
	}
	
	public void deleteTags(final String tagKey, final String tagValue, final List<String> resources){
		final EucaDeleteTagsTask task = 
				new EucaDeleteTagsTask(tagKey, tagValue, resources);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to delete tags");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);	
		}
	}
	
	public void createOrUpdateAutoscalingTags(final String tagKey, final String tagValue, final String asgName){
		final AutoscalingCreateOrUpdateTagsTask task =
				new AutoscalingCreateOrUpdateTagsTask(tagKey, tagValue, asgName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to create/update autoscaling tags");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);	
		}
	}
	
	public void deleteAutoscalingTags(final String tagKey, final String tagValue, final String asgName){
		final AutoscalingDeleteTagsTask task =
				new AutoscalingDeleteTagsTask(tagKey, tagValue, asgName);
		final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
		try{
			if(result.get()){
				return;
			}else
				throw new EucalyptusActivityException("failed to delete autoscaling tags");
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);	
		}
	}
	
	public void uploadServerCertificate(final String certName, final String certPath, final String certBodyPem,
	    final String pkPem, final String certChainPem){
	  final UploadServerCertificateTask task = 
	      new UploadServerCertificateTask(certName, certPath, certBodyPem, pkPem, certChainPem );
	  final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
	  try{
	    if(result.get()){
	      return;
	    }else
	      throw new EucalyptusActivityException("failed to upload server certificate");
	  }catch(Exception ex){
	    throw Exceptions.toUndeclared(ex);
	  }
	}
	
	public ServerCertificateType getServerCertificate(final String certName){
	  final EuareGetServerCertificateTask task =
	      new EuareGetServerCertificateTask(certName);
	  final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
    try{
      if(result.get()){
        return task.getServerCertificate();
      }else
        throw new EucalyptusActivityException("failed to get server certificate");
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
	}
	
	public void deleteServerCertificate(final String certName){
	  final DeleteServerCertificateTask task =
	      new DeleteServerCertificateTask(certName);
	  final CheckedListenableFuture<Boolean> result = task.dispatch(new EuareSystemActivity());
    try{
      if(result.get()){
        return;
      }else
        throw new EucalyptusActivityException("failed to delete server certificate");
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
	}
	
	public List<TagInfo> describeTags(final List<String> names, final List<String> values){
	  final DescribeTagsTask task =
	      new DescribeTagsTask(names, values);
	  final CheckedListenableFuture<Boolean> result = task.dispatch(new EucalyptusSystemActivity());
	  try{
      if(result.get()){
        return task.getTags();
      }else
        throw new EucalyptusActivityException("failed to describe tags");
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
	}
	
	public List<com.eucalyptus.autoscaling.common.TagDescription> describeAutoScalingTags() {
	  final AutoScalingDescribeTagsTask task =
        new AutoScalingDescribeTagsTask();
    final CheckedListenableFuture<Boolean> result = task.dispatch(new AutoScalingSystemActivity());
    try{
      if(result.get()){
        return task.getTags();
      }else
        throw new EucalyptusActivityException("failed to describe tags");
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
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

	private class DescribeTagsTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
	  private List<String> names = null;
	  private List<String> values = null;
	  private List<TagInfo> tags = null;
	  private DescribeTagsTask(final List<String> names, final List<String> values){
	    this.names = names;
	    this.values = values;
	  }
	  
	  private DescribeTagsType describeTags(){
	    if(names.size() != values.size())
	      throw Exceptions.toUndeclared(new Exception("Names and values don't match"));
	    
	    final DescribeTagsType req = new DescribeTagsType();
	    List<Filter> filterSet = Lists.newArrayList();
	    for(int i=0; i<names.size(); i++){
	      final String name = names.get(i);
	      final String value = values.get(i);
	      final Filter f = new Filter();
	      f.setName(name);
	      f.setValueSet(Lists.newArrayList(value));
	      filterSet.add(f);
	    }
	    req.setFilterSet((ArrayList<Filter>)filterSet);
	    return req;
	  }
	  
    @Override
    void dispatchInternal(
        ActivityContext<EucalyptusMessage, Eucalyptus> context,
        Checked<EucalyptusMessage> callback) {
      final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
      client.dispatch(describeTags(), callback);               
    }

    @Override
    void dispatchSuccess(
        ActivityContext<EucalyptusMessage, Eucalyptus> context,
        EucalyptusMessage response) {
      final edu.ucsb.eucalyptus.msgs.DescribeTagsResponseType resp = 
          (edu.ucsb.eucalyptus.msgs.DescribeTagsResponseType) response;
      
      tags = resp.getTagSet();
    }
    
    public List<TagInfo> getTags(){
      return tags;
    }
	}

	private class DeleteServerCertificateTask extends EucalyptusActivityTask<EuareMessage, Euare> {
    private String certName = null;
    private DeleteServerCertificateTask(final String certName){
      this.certName = certName;
    }
    
    private DeleteServerCertificateType deleteServerCertificate(){
      final DeleteServerCertificateType req = new DeleteServerCertificateType();
      req.setServerCertificateName(this.certName);
      return req;
    }

    @Override
    void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(deleteServerCertificate(), callback);              
    }

    @Override
    void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final DeleteServerCertificateResponseType resp = new DeleteServerCertificateResponseType();
    }
  }
	
	private class UploadServerCertificateTask extends EucalyptusActivityTask<EuareMessage, Euare> {
	  private String certName = null;
	  private String certPath = "/";
	  private String certPem = null;
	  private String pkPem = null;
	  private String certChain = null;
	  
	  private UploadServerCertificateTask(final String certName, final String certPath,
	      final String certPem, final String pkPem, final String certChain ){
	   this.certName = certName;
	   if(certPath!=null)
	     this.certPath = certPath;
	   this.certPem = certPem;
	   this.pkPem = pkPem;
	   this.certChain = certChain;
	  }
	  
	  private UploadServerCertificateType uploadServerCertificate(){
	      final UploadServerCertificateType req = new UploadServerCertificateType();
	      req.setServerCertificateName(this.certName);
	      req.setPath(this.certPath);
	      req.setCertificateBody(this.certPem);
	      req.setPrivateKey(this.pkPem);
	      if(this.certChain!=null)
	        req.setCertificateChain(this.certChain);
	      return req;
	  }
	  
    @Override
    void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(uploadServerCertificate(), callback);        
    }

    @Override
    void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final UploadServerCertificateResponseType resp = (UploadServerCertificateResponseType) response;
    }
	}
	
	private class EucaDescribeImagesTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
		private List<String> imageIds = null;
		private List<ImageDetails> result = null;
		private EucaDescribeImagesTask(final List<String> imageIds){
			this.imageIds = imageIds;
		}
		
		private DescribeImagesType describeImages(){
			final DescribeImagesType req = new DescribeImagesType();
			if(this.imageIds!=null && this.imageIds.size()>0){
				req.setImagesSet(new ArrayList(imageIds));
			}
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(describeImages(), callback);				
		}

		@Override
		void dispatchSuccess(ActivityContext<EucalyptusMessage, Eucalyptus> context, 
				EucalyptusMessage response) {
			final DescribeImagesResponseType resp = (DescribeImagesResponseType) response;
			result = resp.getImagesSet();			
		}
		
		List<ImageDetails> getResult(){
			return this.result;
		}
	}
	
	private class AutoscalingDeleteTagsTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String tagKey = null;
		private String tagValue = null;
		private String asgName = null;
		
		private AutoscalingDeleteTagsTask(final String tagKey, final String tagValue, final String asgName){
			this.tagKey = tagKey;
			this.tagValue = tagValue;
			this.asgName = asgName;
		}
		
		private com.eucalyptus.autoscaling.common.DeleteTagsType deleteTags(){
			final com.eucalyptus.autoscaling.common.DeleteTagsType req = new com.eucalyptus.autoscaling.common.DeleteTagsType();
			final Tags tags = new Tags();
			final TagType tag = new TagType();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			tag.setPropagateAtLaunch(true);
			tag.setResourceType("auto-scaling-group");
			tag.setResourceId(this.asgName);
			tags.setMember(Lists.newArrayList(tag));
			req.setTags(tags);
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				Checked<AutoScalingMessage> callback) {
			final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
			client.dispatch(deleteTags(), callback);	
		}

		@Override
		void dispatchSuccess(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				AutoScalingMessage response) {
			final com.eucalyptus.autoscaling.common.DeleteTagsResponseType resp = (com.eucalyptus.autoscaling.common.DeleteTagsResponseType) response;			
		}	
	}
	
	private class AutoscalingCreateOrUpdateTagsTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String tagKey = null;
		private String tagValue = null;
		private String asgName = null;
		
		private AutoscalingCreateOrUpdateTagsTask(final String tagKey, final String tagValue, final String asgName){
			this.tagKey = tagKey;
			this.tagValue = tagValue;
			this.asgName = asgName;
		}
		
		private CreateOrUpdateTagsType createOrUpdateTags(){
			final CreateOrUpdateTagsType req = new CreateOrUpdateTagsType();
			final Tags tags = new Tags();
			final TagType tag = new TagType();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			tag.setPropagateAtLaunch(true);
			tag.setResourceType("auto-scaling-group");
			tag.setResourceId(this.asgName);
			tags.setMember(Lists.newArrayList(tag));
			req.setTags(tags);
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				Checked<AutoScalingMessage> callback) {
			final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
			client.dispatch(createOrUpdateTags(), callback);	
		}

		@Override
		void dispatchSuccess(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				AutoScalingMessage response) {
			final CreateOrUpdateTagsResponseType resp = (CreateOrUpdateTagsResponseType) response;			
		}
	}
	
	private class EucaDeleteTagsTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
		private String tagKey = null;
		private String tagValue = null;
		private List<String> resources = null;
		
		private EucaDeleteTagsTask(final String tagKey, final String tagValue, final List<String> resources){
			this.tagKey = tagKey;
			this.tagValue = tagValue;
			this.resources = resources;
		}
		
		private DeleteTagsType deleteTags(){
			final DeleteTagsType req = new DeleteTagsType();
			req.setResourcesSet(Lists.newArrayList(this.resources));
			final DeleteResourceTag tag = new DeleteResourceTag();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			req.setTagSet(Lists.newArrayList(tag));
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {

			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(deleteTags(), callback);				
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				EucalyptusMessage response) {
			final DeleteTagsResponseType resp = (DeleteTagsResponseType) response;
		}
	}
	
	private class EucaCreateTagsTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
		private String tagKey = null;
		private String tagValue = null;
		private List<String> resources = null;
		private EucaCreateTagsTask(final String tagKey, final String tagValue, final List<String> resources){
			this.tagKey = tagKey;
			this.tagValue = tagValue;
			this.resources = resources;
		}
		private CreateTagsType createTags(){
			final CreateTagsType req = new CreateTagsType();
			req.setResourcesSet(Lists.newArrayList(this.resources));
			final ResourceTag tag = new ResourceTag();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			req.setTagSet(Lists.newArrayList(tag));
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(createTags(), callback);	
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				EucalyptusMessage response) {
			final CreateTagsResponseType resp = (CreateTagsResponseType) response;
		}
	}
	
	private class EucaDescribeKeyPairsTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
		private List<String> keyNames = null;
		private List<DescribeKeyPairsResponseItemType> result = null;
		private EucaDescribeKeyPairsTask(){}
		private EucaDescribeKeyPairsTask(final String keyName){
			this.keyNames = Lists.newArrayList(keyName);
		}
		private EucaDescribeKeyPairsTask(final List<String> keyNames){
			this.keyNames = keyNames;
		}
		
		private DescribeKeyPairsType describeKeyPairs(){
			final DescribeKeyPairsType req = new DescribeKeyPairsType();
			if(this.keyNames!=null){
				req.setKeySet(new ArrayList<String>(this.keyNames));
			}
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(describeKeyPairs(), callback);	
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				EucalyptusMessage response) {
			final DescribeKeyPairsResponseType resp = (DescribeKeyPairsResponseType) response;
			result = resp.getKeySet();
		}
		
		List<DescribeKeyPairsResponseItemType> getResult(){
			return result;
		}
	}
	
	private class EuareGetServerCertificateTask extends EucalyptusActivityTask<EuareMessage, Euare>{
	  private String certName = null;
	  private ServerCertificateType certificate = null;
	  private EuareGetServerCertificateTask(final String certName){
	    this.certName = certName;
	  }
	  
	  private GetServerCertificateType getRequest(){
	    final GetServerCertificateType req = new GetServerCertificateType();
	    req.setServerCertificateName(this.certName);
	    return req;
	  }

    @Override
    void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
        Checked<EuareMessage> callback) {
      final DispatchingClient<EuareMessage, Euare> client = context.getClient();
      client.dispatch(getRequest(), callback); 
    }

    @Override
    void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
        EuareMessage response) {
      final GetServerCertificateResponseType resp = (GetServerCertificateResponseType) response;
      if(resp.getGetServerCertificateResult()!= null)
        this.certificate = resp.getGetServerCertificateResult().getServerCertificate();
    }
    
    public ServerCertificateType getServerCertificate(){
      return this.certificate;
    }
	}
	
	private class EuareDeleteInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
		private String profileName =null;
		private EuareDeleteInstanceProfileTask(String profileName){
			this.profileName = profileName;
		}
		
		private DeleteInstanceProfileType deleteInstanceProfile(){
			final DeleteInstanceProfileType req = new DeleteInstanceProfileType();
			req.setInstanceProfileName(this.profileName);
			return req;
		}
		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(deleteInstanceProfile(), callback);	
		}

		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {
			final DeleteInstanceProfileResponseType resp = (DeleteInstanceProfileResponseType) response;
		}
	}
	
	private class EuareAddRoleToInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
		private String instanceProfileName = null;
		private String roleName = null;
		
		private EuareAddRoleToInstanceProfileTask(final String instanceProfileName, final String roleName){
			this.instanceProfileName = instanceProfileName;
			this.roleName = roleName;
		}
		
		private AddRoleToInstanceProfileType addRoleToInstanceProfile(){
			final AddRoleToInstanceProfileType req  = new AddRoleToInstanceProfileType();
			req.setRoleName(this.roleName);
			req.setInstanceProfileName(this.instanceProfileName);
			return req;
		}
		
		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(addRoleToInstanceProfile(), callback);	
		}
		
		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {										
			final AddRoleToInstanceProfileResponseType resp = (AddRoleToInstanceProfileResponseType) response;
		}
	}
	
	private class EuareRemoveRoleFromInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
	  private String instanceProfileName = null;
	  private String roleName = null;
	  
	  private EuareRemoveRoleFromInstanceProfileTask(final String instanceProfileName, final String roleName){
	    this.instanceProfileName = instanceProfileName;
	    this.roleName = roleName;
	  }
	  
	  private RemoveRoleFromInstanceProfileType removeRoleFromInstanceProfile(){
	    final RemoveRoleFromInstanceProfileType req = new RemoveRoleFromInstanceProfileType();
	    req.setRoleName(this.roleName);
	    req.setInstanceProfileName(this.instanceProfileName);
	    return req;
	  }
	  
	  @Override
	  void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
	      Checked<EuareMessage> callback){
	    final DispatchingClient<EuareMessage, Euare> client = context.getClient();
	    client.dispatch(removeRoleFromInstanceProfile(), callback);
	  }
	  
	  @Override
	  void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
	      EuareMessage response){
	    final RemoveRoleFromInstanceProfileResponseType resp = (RemoveRoleFromInstanceProfileResponseType) response;
	  }
	}
	
	private class EuareListInstanceProfilesTask extends EucalyptusActivityTask<EuareMessage, Euare>{
		private String pathPrefix = null;
		private List<InstanceProfileType> instanceProfiles = null;
		private EuareListInstanceProfilesTask(final String pathPrefix){
			this.pathPrefix = pathPrefix;
		}
		
		private ListInstanceProfilesType listInstanceProfiles(){
			final ListInstanceProfilesType req = new ListInstanceProfilesType();
			req.setPathPrefix(this.pathPrefix);
			return req;
		}
		
		public List<InstanceProfileType> getInstanceProfiles(){
			return this.instanceProfiles;
		}

		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(listInstanceProfiles(), callback);								
		}

		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {
			ListInstanceProfilesResponseType resp = (ListInstanceProfilesResponseType) response;
			try{
				instanceProfiles = resp.getListInstanceProfilesResult().getInstanceProfiles().getMember();
			}catch(Exception  ex){
				;
			}
		}
	}
	
	private class EuareCreateInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
		private String profileName = null;
		private String path = null;
		private InstanceProfileType instanceProfile = null;
		private EuareCreateInstanceProfileTask(String profileName, String path){
			this.profileName = profileName;
			this.path = path;
		}
		
		private CreateInstanceProfileType createInstanceProfile(){
			final CreateInstanceProfileType req = new CreateInstanceProfileType();
			req.setInstanceProfileName(this.profileName);
			req.setPath(this.path);
			return req;
		}
		
		public InstanceProfileType getInstanceProfile(){
			return this.instanceProfile;
		}
			
		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(createInstanceProfile(), callback);					
		}

		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {
			final CreateInstanceProfileResponseType resp = (CreateInstanceProfileResponseType) response;
			try{
				this.instanceProfile = resp.getCreateInstanceProfileResult().getInstanceProfile();
			}catch(Exception ex){
				;
			}	
		}
	}
	
	private class EuareDeleteRoleTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String roleName = null;
		private EuareDeleteRoleTask(String roleName){
			this.roleName = roleName;
		}
		private DeleteRoleType deleteRole(){
			final DeleteRoleType req = new DeleteRoleType();
			req.setRoleName(this.roleName);
			return req;
		}
		
		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(deleteRole(), callback);			
		}

		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {
			final DeleteRoleResponseType resp = (DeleteRoleResponseType) response;
		}
	}
	
	private class EuareCreateRoleTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		String roleName = null;
		String path = null;
		String assumeRolePolicy = null;
		private RoleType role = null;
		private EuareCreateRoleTask(String roleName, String path, String assumeRolePolicy){
			this.roleName = roleName;
			this.path = path;
			this.assumeRolePolicy = assumeRolePolicy;
		}
		private CreateRoleType createRole(){
			final CreateRoleType req = new CreateRoleType();
			req.setRoleName(this.roleName);
			req.setPath(this.path);
			req.setAssumeRolePolicyDocument(this.assumeRolePolicy);
			return req;
		}
		
		public RoleType getRole(){ 
			return this.role;
		}
		
		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(createRole(), callback);			
		}

		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {
			CreateRoleResponseType resp = (CreateRoleResponseType) response;
			try{
				this.role = resp.getCreateRoleResult().getRole();
			}catch(Exception ex){
				;
			}			
		}
	}
	
	private class EuareListRolesTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String pathPrefix = null;
		private List<RoleType> roles = Lists.newArrayList();
		
		private EuareListRolesTask(String pathPrefix){
			this.pathPrefix = pathPrefix;
		}
		
		private ListRolesType listRoles(){
			final ListRolesType req = new ListRolesType();
			req.setPathPrefix(this.pathPrefix);
			return req;
		}
		
		public List<RoleType> getRoles(){
			return this.roles;
		}
		
		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(listRoles(), callback);
		}

		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {
			ListRolesResponseType resp = (ListRolesResponseType) response;
			try{
				this.roles = resp.getListRolesResult().getRoles().getMember();
			}catch(Exception ex){
				;
			}
		}
	}
	
	private class EuarePutRolePolicyTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String roleName = null;
		private String policyName = null;
		private String policyDocument = null;
		
		private EuarePutRolePolicyTask(String roleName, String policyName, String policyDocument){
			this.roleName = roleName;
			this.policyName = policyName;
			this.policyDocument = policyDocument;
		}
		
		private PutRolePolicyType putRolePolicy(){
			final PutRolePolicyType req = 
					new PutRolePolicyType();
			req.setRoleName(this.roleName);
			req.setPolicyName(this.policyName);
			req.setPolicyDocument(this.policyDocument);
			
			return req;
		}
		
		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(putRolePolicy(), callback);
			
		}
		
		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {
			final PutRolePolicyResponseType resp = (PutRolePolicyResponseType) response;
		}
	}
	
	private class EuareGetRolePolicyTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String roleName = null;
		private String policyName = null;
		private GetRolePolicyResult result = null;
		
		private EuareGetRolePolicyTask(final String roleName, final String policyName){
			this.roleName = roleName;
			this.policyName = policyName;
		}
		
		private GetRolePolicyType getRolePolicy(){
			final GetRolePolicyType req = new GetRolePolicyType();
			req.setRoleName(this.roleName);
			req.setPolicyName(this.policyName);
			return req;
		}
		
		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(getRolePolicy(), callback);
		}

		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {
			final GetRolePolicyResponseType resp = (GetRolePolicyResponseType) response;
			this.result = resp.getGetRolePolicyResult();
		}
		
		GetRolePolicyResult getResult(){
			return this.result;
		}
	}
	
	private class EuareDeleteRolePolicyTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String roleName= null;
		private String policyName = null;
		
		private EuareDeleteRolePolicyTask(final String roleName, final String policyName){
			this.roleName = roleName;
			this.policyName = policyName;
		}
		
		private DeleteRolePolicyType deleteRolePolicy(){
			final DeleteRolePolicyType req = new DeleteRolePolicyType();
			req.setRoleName(this.roleName);
			req.setPolicyName(this.policyName);
			return req;
		}
		
		@Override
		void dispatchInternal(ActivityContext<EuareMessage, Euare> context,
				Checked<EuareMessage> callback) {
			final DispatchingClient<EuareMessage, Euare> client = context.getClient();
			client.dispatch(deleteRolePolicy(), callback);
		}

		@Override
		void dispatchSuccess(ActivityContext<EuareMessage, Euare> context,
				EuareMessage response) {
			final DeleteRolePolicyResponseType resp = (DeleteRolePolicyResponseType) response;
		}
	}
	
	private class AutoScalingDescribeTagsTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
	  private List<com.eucalyptus.autoscaling.common.TagDescription> result = null;
	  private com.eucalyptus.autoscaling.common.DescribeTagsType describeTags(){
	    final com.eucalyptus.autoscaling.common.DescribeTagsType req = new 
	        com.eucalyptus.autoscaling.common.DescribeTagsType();
	    return req;
	  }
	  
    @Override
    void dispatchInternal(
        ActivityContext<AutoScalingMessage, AutoScaling> context,
        Checked<AutoScalingMessage> callback) { 
      final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
      client.dispatch(describeTags(), callback);      
    }

    @Override
    void dispatchSuccess(
        ActivityContext<AutoScalingMessage, AutoScaling> context,
        AutoScalingMessage response) {
      com.eucalyptus.autoscaling.common.DescribeTagsResponseType resp = 
          (com.eucalyptus.autoscaling.common.DescribeTagsResponseType) response;
      if(resp.getDescribeTagsResult()!=null && resp.getDescribeTagsResult().getTags()!=null)
        this.result = resp.getDescribeTagsResult().getTags().getMember();    }
    
    public List<com.eucalyptus.autoscaling.common.TagDescription> getTags(){
      return this.result;
    }
	}
	
	private class AutoScalingUpdateGroupTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String groupName = null;
		private List<String> availabilityZones = null;
		private Integer capacity = null;
		private String launchConfigName = null;
		
		private AutoScalingUpdateGroupTask(final String groupName, final List<String> zones, 
				final Integer capacity, final String launchConfig){
			this.groupName = groupName;
			this.availabilityZones = zones;
			this.capacity = capacity;
			this.launchConfigName = launchConfig;
		}
		
		private UpdateAutoScalingGroupType updateAutoScalingGroup(){
			final UpdateAutoScalingGroupType req = new UpdateAutoScalingGroupType();
			req.setAutoScalingGroupName(this.groupName);
			
			if(this.availabilityZones!=null && this.availabilityZones.size()>0){
				AvailabilityZones zones = new AvailabilityZones();
				zones.setMember(Lists.<String>newArrayList());
				zones.getMember().addAll(this.availabilityZones);
				req.setAvailabilityZones(zones);
			}
			if(this.capacity!=null){
				req.setDesiredCapacity(this.capacity);
				req.setMaxSize(this.capacity);
				req.setMinSize(this.capacity);
			}
			if(this.launchConfigName!=null){
				req.setLaunchConfigurationName(this.launchConfigName);
			}
			return req;
		}
		@Override
		void dispatchInternal(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				Checked<AutoScalingMessage> callback) {
			final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
			client.dispatch(updateAutoScalingGroup(), callback);
		}

		@Override
		void dispatchSuccess(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				AutoScalingMessage response) {
			final UpdateAutoScalingGroupResponseType resp = (UpdateAutoScalingGroupResponseType) response;
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
			if(this.groupNames!=null)
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
    private String tagKey = null;
    private String tagValue = null;
    
    private AutoScalingCreateGroupTask(final String groupName, final List<String> zones, 
        final int capacity, final String launchConfig){
      this.groupName = groupName;
      this.availabilityZones = zones;
      this.capacity = capacity;
      this.launchConfigName = launchConfig;
    }
        
    private AutoScalingCreateGroupTask(final String groupName, final List<String> zones, 
        final int capacity, final String launchConfig, final String tagKey, final String tagValue){
      this.groupName = groupName;
      this.availabilityZones = zones;
      this.capacity = capacity;
      this.launchConfigName = launchConfig;
      this.tagKey = tagKey;
      this.tagValue = tagValue;
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
      if(this.tagKey != null && this.tagValue!=null){
        final Tags tags = new Tags();
        final TagType tag = new TagType();
        tag.setKey(this.tagKey);
        tag.setValue(this.tagValue);
        tag.setPropagateAtLaunch(true);
        tag.setResourceType("auto-scaling-group");
        tag.setResourceId(this.groupName);
        tags.setMember(Lists.newArrayList(tag));
        req.setTags(tags);
      }
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

	private class AutoScalingDescribeLaunchConfigsTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String launchConfigName = null;
		private LaunchConfigurationType result = null;
		private AutoScalingDescribeLaunchConfigsTask(final String launchConfigName){
			this.launchConfigName = launchConfigName;
		}
		
		private DescribeLaunchConfigurationsType describeLaunchConfigurations(){
			final DescribeLaunchConfigurationsType req = new DescribeLaunchConfigurationsType();
			final LaunchConfigurationNames names = new LaunchConfigurationNames();
			names.setMember(Lists.newArrayList(this.launchConfigName));
			req.setLaunchConfigurationNames(names);
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				Checked<AutoScalingMessage> callback) {
			final DispatchingClient<AutoScalingMessage, AutoScaling> client = context.getClient();
			client.dispatch(describeLaunchConfigurations(), callback);
		}

		@Override
		void dispatchSuccess(
				ActivityContext<AutoScalingMessage, AutoScaling> context,
				AutoScalingMessage response) {
			final DescribeLaunchConfigurationsResponseType resp = (DescribeLaunchConfigurationsResponseType) response;
			try{
				this.result = 
						resp.getDescribeLaunchConfigurationsResult().getLaunchConfigurations().getMember().get(0);
			}catch(final Exception ex){
				LOG.error("Launch configuration is not found from the response");
			}
		}
		
		private LaunchConfigurationType getResult(){
			return this.result;
		}
	}
	
	private class AutoScalingCreateLaunchConfigTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String imageId=null;
		private String instanceType = null;
		private String instanceProfileName = null;
		private String launchConfigName = null;
		private String securityGroup = null;
		private String keyName = null;
		private String userData = null;
		private AutoScalingCreateLaunchConfigTask(final String imageId, final String instanceType, String instanceProfileName,
				final String launchConfigName, final String sgroupName, final String keyName, final String userData){
			this.imageId = imageId;
			this.instanceType = instanceType;
			this.instanceProfileName = instanceProfileName;
			this.launchConfigName = launchConfigName;
			this.securityGroup = sgroupName;
			this.keyName = keyName; 
			this.userData = userData;
		}
		
		private CreateLaunchConfigurationType createLaunchConfiguration(){
			final CreateLaunchConfigurationType req = new CreateLaunchConfigurationType();
			req.setImageId(this.imageId);
			req.setInstanceType(this.instanceType);
			if(this.instanceProfileName!=null)
				req.setIamInstanceProfile(this.instanceProfileName);
			if(this.keyName!=null)
				req.setKeyName(this.keyName);
			
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
	
	private class EucalyptusDescribeAvailabilityZonesTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus> {
		private List<ClusterInfoType> zones = null; 
		private boolean verbose = false;
		private EucalyptusDescribeAvailabilityZonesTask(boolean verbose){
			this.verbose = verbose;
		}
		
		private DescribeAvailabilityZonesType describeAvailabilityZones(){
			final DescribeAvailabilityZonesType req = new DescribeAvailabilityZonesType();
			if(this.verbose){
				req.setAvailabilityZoneSet(Lists.newArrayList("verbose"));
			}
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

	private class EucalyptusDescribeVMTypesTask extends EucalyptusActivityTask<BaseMessage, Eucalyptus> {
		private List<VmTypeDetails> types = null;

		private DescribeInstanceTypesType describeVMTypes(){
			final DescribeInstanceTypesType req = new DescribeInstanceTypesType();
		    return req;
		}

		@Override
		void dispatchInternal(
				ActivityContext<BaseMessage, Eucalyptus> context,
				Checked<BaseMessage> callback) {
			final DispatchingClient<BaseMessage, Eucalyptus> client = context.getClient();
			client.dispatch(describeVMTypes(), callback);
		}

		@Override
		void dispatchSuccess(
				ActivityContext<BaseMessage, Eucalyptus> context, BaseMessage response) {
			final DescribeInstanceTypesResponseType resp = (DescribeInstanceTypesResponseType) response;
			this.types = resp.getInstanceTypeDetails();
		}

		public List<VmTypeDetails> getVMTypes(){
			return this.types;
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
			perm.setFromPort(this.portNum);
			perm.setToPort(this.portNum);
			perm.setCidrIpRanges( Lists.newArrayList( Arrays.asList( "0.0.0.0/0" ) ) );
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
			perm.setFromPort(this.portNum);
			perm.setToPort(this.portNum);
			perm.setCidrIpRanges( Lists.newArrayList( Arrays.asList( "0.0.0.0/0" ) ) );
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
	
	private class EucalyptusDescribeSecurityGroupTask extends EucalyptusActivityTask<EucalyptusMessage, Eucalyptus>{
		private List<String> groups = null;
		private List<SecurityGroupItemType> result = null;
		EucalyptusDescribeSecurityGroupTask(final List<String> groups){
			this.groups = groups;
		}
		
		
		private DescribeSecurityGroupsType describeSecurityGroups(){
			final DescribeSecurityGroupsType req = new DescribeSecurityGroupsType();
			req.setSecurityGroupSet(Lists.newArrayList(this.groups));
			return req;
		}
		
		@Override
		void dispatchInternal(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				Checked<EucalyptusMessage> callback) {
			final DispatchingClient<EucalyptusMessage, Eucalyptus> client = context.getClient();
			client.dispatch(describeSecurityGroups(), callback);			
		}

		@Override
		void dispatchSuccess(
				ActivityContext<EucalyptusMessage, Eucalyptus> context,
				EucalyptusMessage response) {
			final DescribeSecurityGroupsResponseType resp = (DescribeSecurityGroupsResponseType) response;
			this.result = resp.getSecurityGroupInfo();
		}
	
		public List<SecurityGroupItemType> getResult(){
			return this.result;
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
	      LOG.error( "Eucalyptus activity error", throwable );
	    }
	
	    abstract void dispatchSuccess( ActivityContext<TM,TC> context, TM response );
	}
}

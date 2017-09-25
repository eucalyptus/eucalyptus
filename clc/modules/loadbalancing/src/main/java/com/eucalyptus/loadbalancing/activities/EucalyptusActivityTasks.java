/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import com.amazonaws.services.s3.model.Bucket;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.compute.common.*;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.common.msgs.AddRoleToInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.CreateInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreateInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.CreateRoleResponseType;
import com.eucalyptus.auth.euare.common.msgs.CreateRoleType;
import com.eucalyptus.auth.euare.common.msgs.DeleteInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.DeleteRoleType;
import com.eucalyptus.auth.euare.common.msgs.EuareMessage;
import com.eucalyptus.auth.euare.common.msgs.GetRolePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.GetRolePolicyResult;
import com.eucalyptus.auth.euare.common.msgs.GetRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.GetServerCertificateResponseType;
import com.eucalyptus.auth.euare.common.msgs.GetServerCertificateType;
import com.eucalyptus.auth.euare.common.msgs.InstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.ListInstanceProfilesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListInstanceProfilesType;
import com.eucalyptus.auth.euare.common.msgs.ListRolePoliciesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListRolePoliciesType;
import com.eucalyptus.auth.euare.common.msgs.ListRolesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListRolesType;
import com.eucalyptus.auth.euare.common.msgs.PutRolePolicyType;
import com.eucalyptus.auth.euare.common.msgs.RemoveRoleFromInstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.RoleType;
import com.eucalyptus.auth.euare.common.msgs.ServerCertificateType;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupNames;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingMessage;
import com.eucalyptus.autoscaling.common.msgs.AvailabilityZones;
import com.eucalyptus.autoscaling.common.msgs.CreateAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.CreateLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.CreateOrUpdateTagsType;
import com.eucalyptus.autoscaling.common.msgs.DeleteAutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DeleteLaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeLaunchConfigurationsType;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationNames;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.SecurityGroups;
import com.eucalyptus.autoscaling.common.msgs.TagType;
import com.eucalyptus.autoscaling.common.msgs.Tags;
import com.eucalyptus.autoscaling.common.msgs.UpdateAutoScalingGroupType;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.cloudwatch.common.msgs.CloudWatchMessage;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.cloudwatch.common.msgs.PutMetricDataType;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ComputeMessage;
import com.eucalyptus.compute.common.DeleteResourceTag;
import com.eucalyptus.compute.common.DescribeImagesResponseType;
import com.eucalyptus.compute.common.DescribeImagesType;
import com.eucalyptus.compute.common.DescribeInstanceTypesResponseType;
import com.eucalyptus.compute.common.DescribeInstanceTypesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.GroupIdSetType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.InternetGatewayIdSetItemType;
import com.eucalyptus.compute.common.InternetGatewayIdSetType;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetIdSetItemType;
import com.eucalyptus.compute.common.SubnetIdSetType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.VmTypeDetails;
import com.eucalyptus.compute.common.VpcType;
import com.eucalyptus.compute.common.backend.AuthorizeSecurityGroupIngressType;
import com.eucalyptus.compute.common.backend.CreateSecurityGroupResponseType;
import com.eucalyptus.compute.common.backend.CreateSecurityGroupType;
import com.eucalyptus.compute.common.backend.DeleteSecurityGroupType;
import com.eucalyptus.compute.common.backend.DescribeAvailabilityZonesResponseType;
import com.eucalyptus.compute.common.backend.DescribeAvailabilityZonesType;
import com.eucalyptus.compute.common.backend.ModifyInstanceAttributeType;
import com.eucalyptus.compute.common.backend.RevokeSecurityGroupIngressType;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EmpyreanMessage;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncExceptions.AsyncWebServiceError;
import com.eucalyptus.util.async.CheckedListenableFuture;
import com.eucalyptus.util.async.Futures;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
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
	  DispatchingClient<TM, TC> getClient();
	}

	private abstract class ActivityContextSupport<TM extends BaseMessage, TC extends ComponentId> implements ActivityContext<TM, TC>{
		private final Class<TC> componentIdClass;
		private ActivityContextSupport( final Class<TC> componentIdClass ) {
			this.componentIdClass = componentIdClass;
		}

		abstract String getUserId( );

		/**
		 * Account to use if user identifier not set, should not be called otherwise.
		 */
		abstract AccountFullName getAccount( );

		@Override
		public DispatchingClient<TM, TC> getClient() {
			try{
				final DispatchingClient<TM, TC> client =
						getUserId( ) != null ?
								new DispatchingClient<TM, TC>( this.getUserId( ), this.componentIdClass ) :
								new DispatchingClient<TM, TC>( this.getAccount(), this.componentIdClass );
				client.init();
				return client;
			}catch(Exception ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
	}

	private abstract class SystemActivityContextSupport<TM extends BaseMessage, TC extends ComponentId> extends ActivityContextSupport<TM, TC>{
	  boolean useELBSystemAccount = true;
	  private SystemActivityContextSupport( final Class<TC> componentIdClass ) {
			super( componentIdClass );
		}

		private SystemActivityContextSupport( final Class<TC> componentIdClass, final boolean useELBSystemAccount ) {
      super( componentIdClass );
      this.useELBSystemAccount = useELBSystemAccount;
		}

		@Override
		final String getUserId() {
			return null;
		}

		@Override
		AccountFullName getAccount() {
			try{
				return AccountFullName.getInstance( Accounts.lookupAccountIdByAlias( this.useELBSystemAccount ?
						AccountIdentifiers.ELB_SYSTEM_ACCOUNT :
						AccountIdentifiers.SYSTEM_ACCOUNT
				) );
			}catch(AuthException ex){
				throw Exceptions.toUndeclared(ex);
			}
		}
  }

	private abstract class UserActivityContextSupport<TM extends BaseMessage, TC extends ComponentId> extends ActivityContextSupport<TM, TC>{
		private final String userId;
		private final AccountFullName accountFullName;

		private UserActivityContextSupport(
				final Class<TC> componentIdClass,
				final String userId
		) {
			super( componentIdClass );
			this.userId = userId;
			this.accountFullName = null;
		}

		private UserActivityContextSupport(
				final Class<TC> componentIdClass,
				final AccountFullName accountFullName
		) {
			super( componentIdClass );
			this.userId = null;
			this.accountFullName = accountFullName;
		}

		public final String getUserId() {
			return userId;
		}

		public final AccountFullName getAccount() {
			return accountFullName;
		}
	}

	private class EuareSystemActivity extends SystemActivityContextSupport<EuareMessage, Euare>{
		private EuareSystemActivity( ){ super( Euare.class ); }
		private EuareSystemActivity( final boolean useELBSystemAccount ){
		  super( Euare.class, useELBSystemAccount );
		}
	}

	private class EuareUserActivity extends UserActivityContextSupport<EuareMessage, Euare> {
	  private EuareUserActivity(final AccountFullName accountFullName){
	    super( Euare.class, accountFullName );
	  }
	}

	private class AutoScalingSystemActivity extends SystemActivityContextSupport<AutoScalingMessage, AutoScaling>{
		private AutoScalingSystemActivity(final boolean useELBSystemAccount){
		  super( AutoScaling.class, useELBSystemAccount );
		}
	}

	private class CloudWatchUserActivity extends UserActivityContextSupport<CloudWatchMessage, CloudWatch>{
		private CloudWatchUserActivity(final String userId){
			super( CloudWatch.class, userId );
		}
	}

	private class EmpyreanSystemActivity extends SystemActivityContextSupport<EmpyreanMessage, Empyrean>{
		private EmpyreanSystemActivity() { super( Empyrean.class, false ); }
	}

	private class ComputeSystemActivity extends SystemActivityContextSupport<ComputeMessage, Compute>{
		private ComputeSystemActivity() { super( Compute.class ); }
		private ComputeSystemActivity(final boolean useELBSystemAccount) { super( Compute.class , useELBSystemAccount); }
    }

	private class ComputeUserActivity extends UserActivityContextSupport<ComputeMessage, Compute>{
		private ComputeUserActivity(final AccountFullName accountFullName){
			super( Compute.class, accountFullName );
		}
	}

	public List<RunningInstancesItemType> describeSystemInstancesWithVerbose(final List<String> instances){
    if(instances.size() <=0)
      return Lists.newArrayList();
    final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances, true);
    return resultOf( describeTask, new ComputeSystemActivity(false), "failed to describe the instances" );
  }

	public List<RunningInstancesItemType> describeSystemInstances(final List<String> instances){
	  return describeSystemInstancesImpl(instances, true);
	}

	public List<RunningInstancesItemType> describeSystemInstances(final List<String> instances, final boolean useELBSystemAccount){
	  return describeSystemInstancesImpl(instances, useELBSystemAccount);
	}
	private List<RunningInstancesItemType> describeSystemInstancesImpl(final List<String> instances, final boolean useELBSystemAccount){
	  if(instances.size() <=0)
	    return Lists.newArrayList();
	  final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances);
	  return resultOf( describeTask, new ComputeSystemActivity(useELBSystemAccount), "failed to describe the instances" );
	}

	public List<RunningInstancesItemType> describeUserInstances(final String accountNumber, final List<String> instances){
		if(instances.size() <=0)
			return Lists.newArrayList();
		final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances);
		return resultOf( describeTask, new ComputeUserActivity( AccountFullName.getInstance( accountNumber )), "failed to describe the instances" );
	}

	public List<ServiceStatusType> describeServices(final String componentType){
		//LOG.info("calling describe-services -T "+componentType);
		final EucalyptusDescribeServicesTask serviceTask = new EucalyptusDescribeServicesTask(componentType);
		return resultOf( serviceTask, new EmpyreanSystemActivity(), "failed to describe services" );
	}

  public List<ClusterInfoType> describeAvailabilityZonesWithVerbose(){
    return resultOf(
        new EucalyptusDescribeAvailabilityZonesTask(true),
        new ComputeSystemActivity(false),
        "failed to describe the availability zones"
    );
  }

  public List<ClusterInfoType> describeAvailabilityZones() {
    return describeAvailabilityZonesImpl(true);
  }
  public List<ClusterInfoType> describeAvailabilityZones(final boolean useELBSystemAccount) {
    return describeAvailabilityZonesImpl(useELBSystemAccount);
  }
	private List<ClusterInfoType> describeAvailabilityZonesImpl(final boolean useELBSystemAccount){
		return resultOf(
				new EucalyptusDescribeAvailabilityZonesTask(false),
				new ComputeSystemActivity(useELBSystemAccount),
				"failed to describe the availability zones"
		);
	}

	public void createSystemSecurityGroup( String groupName, String groupDesc ){
		final EucalyptusCreateGroupTask task = new EucalyptusCreateGroupTask(groupName, groupDesc);
		checkResult( task, new ComputeSystemActivity(), "failed to create the group "+groupName );
	}

	public void deleteSystemSecurityGroup( String groupName ){
	  deleteSystemSecurityGroupImpl(groupName, true);
	}
	public void deleteSystemSecurityGroup( String groupName , boolean useELBSystemAccount){
	  deleteSystemSecurityGroupImpl(groupName, useELBSystemAccount);
	}
	private void deleteSystemSecurityGroupImpl( String groupName, boolean useELBSystemAccount ){
	  final EucalyptusDeleteGroupTask task = new EucalyptusDeleteGroupTask(groupName);
	  checkResult( task, new ComputeSystemActivity(useELBSystemAccount), "failed to delete the group "+groupName );
	}
	public List<SecurityGroupItemType> describeSystemSecurityGroups( List<String> groupNames, boolean useElbSystemAccount ){
    return describeSystemSecurityGroupsImpl(groupNames, null, useElbSystemAccount);
  }
	public List<SecurityGroupItemType> describeSystemSecurityGroups( List<String> groupNames ){
		return describeSystemSecurityGroupsImpl(groupNames, null, true);
	}

	public List<SecurityGroupItemType> describeSystemSecurityGroupsByVpc(final String vpcId) {
		return describeSystemSecurityGroupsImpl(null, vpcId, true);
	}

	private List<SecurityGroupItemType> describeSystemSecurityGroupsImpl( final List<String> groupNames, final String vpcId, boolean useELBSystemAccount ){
	  final EucalyptusDescribeSecurityGroupTask task = new EucalyptusDescribeSecurityGroupTask(null, groupNames, vpcId);
	  return resultOf( task, new ComputeSystemActivity(useELBSystemAccount), "failed to describe security groups" );
	}

	public void authorizeSystemSecurityGroup( String groupNameOrId, String protocol, int portNum ){
	  this.authorizeSystemSecurityGroupImpl( groupNameOrId, protocol, portNum,  new ComputeSystemActivity());
	}

  public void authorizeSystemSecurityGroup( String groupNameOrId, String protocol, int portNum, boolean useELBSystemAccount ){
    this.authorizeSystemSecurityGroupImpl( groupNameOrId, protocol, portNum,  new ComputeSystemActivity(useELBSystemAccount));
  }

  private void authorizeSystemSecurityGroupImpl( String groupNameOrId, String protocol, int portNum, ComputeSystemActivity context){
    final EucalyptusAuthorizeIngressRuleTask task = new EucalyptusAuthorizeIngressRuleTask(groupNameOrId, protocol, portNum);
    checkResult(
        task,
        context,
        String.format("failed to authorize:%s, %s, %d ", groupNameOrId, protocol, portNum)
    );
  }

  public void revokeSystemSecurityGroup( String groupName, String protocol, int portNum) {
    revokeSystemSecurityGroupImpl(groupName, protocol, portNum, true);
  }

  public void revokeSystemSecurityGroup( String groupName, String protocol, int portNum, boolean useELBSystemAccount ){
    revokeSystemSecurityGroupImpl(groupName, protocol, portNum, useELBSystemAccount);
  }

	private void revokeSystemSecurityGroupImpl( String groupName, String protocol, int portNum, boolean useELBSystemAccount ){
		final EucalyptusRevokeIngressRuleTask task = new EucalyptusRevokeIngressRuleTask(groupName, protocol, portNum);
		checkResult(
				task,
				new ComputeSystemActivity(useELBSystemAccount),
				String.format("failed to revoke:%s, %s, %d ", groupName, protocol, portNum)
		);
	}

	public List<SecurityGroupItemType> describeUserSecurityGroupsByName( AccountFullName accountFullName, String vpcId, String groupName ){
		final EucalyptusDescribeSecurityGroupTask task =
				new EucalyptusDescribeSecurityGroupTask( null, Lists.newArrayList( groupName), vpcId );
		return resultOf( task, new ComputeUserActivity( accountFullName ), "failed to describe security groups" );
	}

	public void createUserSecurityGroup( AccountFullName accountFullName, String groupName, String groupDesc ){
		final EucalyptusCreateGroupTask task = new EucalyptusCreateGroupTask( groupName, groupDesc );
		checkResult( task, new ComputeUserActivity( accountFullName ), "failed to create the group "+groupName );
	}

	public void putCloudWatchMetricData(final String userId, final String namespace, final MetricData data){
		final CloudWatchPutMetricDataTask task = new CloudWatchPutMetricDataTask(namespace, data);
		checkResult(
				task,
				new CloudWatchUserActivity(userId),
				"failed to put metric data"
		);
	}

	private final EucaS3Client getS3Client(final AccountFullName account) throws AuthException {
			return EucaS3ClientFactory.getEucaS3Client(
							new SecurityTokenAWSCredentialsProvider( account ) ) ;
	}

	public List<Bucket> listBuckets(final AccountFullName account) {
		try ( final EucaS3Client s3c = getS3Client(account) ) {
			return s3c.listBuckets();
		} catch (final AuthException e) {
			LOG.error("Failed to create s3 client for listing buckets");
			return Lists.newArrayList();
		}
	}

	public boolean bucketExists(final AccountFullName account, final String bucketName) {
		try ( final EucaS3Client s3c = getS3Client(account) ) {
			s3c.getBucketAcl(bucketName);
			return true;
		} catch (final AuthException e) {
			LOG.error("Failed to create s3 client for testing bucket");
			return false;
		} catch (final Exception e) {
			return false;
		}
	}
  public void createLaunchConfiguration(
    final String imageId,
    final String instanceType,
    final String instanceProfileName,
    final String launchConfigName,
    final Collection<String> securityGroupNamesOrIds,
    final String keyName,
    final String userData,
    final Boolean associatePublicIp){
    createLaunchConfigurationImpl(imageId, instanceType, instanceProfileName, launchConfigName, securityGroupNamesOrIds,
        keyName, userData, associatePublicIp, true);
  }


  public void createLaunchConfiguration(
    final String imageId,
    final String instanceType,
    final String instanceProfileName,
    final String launchConfigName,
    final Collection<String> securityGroupNamesOrIds,
    final String keyName,
    final String userData,
    final Boolean associatePublicIp,
    final boolean useELBSystemAccount) {
    createLaunchConfigurationImpl(imageId, instanceType, instanceProfileName, launchConfigName, securityGroupNamesOrIds,
        keyName, userData, associatePublicIp, useELBSystemAccount);
  }

	private void createLaunchConfigurationImpl(
		final String imageId,
		final String instanceType,
		final String instanceProfileName,
		final String launchConfigName,
		final Collection<String> securityGroupNamesOrIds,
		final String keyName,
		final String userData,
		final Boolean associatePublicIp,
		final boolean useELBSystemAccount
	){
		final AutoScalingCreateLaunchConfigTask task =
				new AutoScalingCreateLaunchConfigTask(imageId, instanceType, instanceProfileName, launchConfigName, securityGroupNamesOrIds, keyName, userData, associatePublicIp);
		checkResult(
				task,
				new AutoScalingSystemActivity( useELBSystemAccount ),
				"failed to create launch configuration"
		);
	}

	public void createAutoScalingGroup(final String groupName, final List<String> availabilityZones, final String vpcZoneIdentifier,
	    final int capacity, final String launchConfigName, final String tagKey, final String tagValue){
	  createAutoScalingGroupImpl(groupName, availabilityZones, vpcZoneIdentifier, capacity, launchConfigName, tagKey, tagValue, true);
	}
	public void createAutoScalingGroup(final String groupName, final List<String> availabilityZones, final String vpcZoneIdentifier,
	    final int capacity, final String launchConfigName, final String tagKey, final String tagValue, final boolean useELBSystemAccount){
	  createAutoScalingGroupImpl(groupName, availabilityZones, vpcZoneIdentifier, capacity, launchConfigName, tagKey, tagValue, useELBSystemAccount);
	}
	private void createAutoScalingGroupImpl(final String groupName, final List<String> availabilityZones, final String vpcZoneIdentifier,
	    final int capacity, final String launchConfigName, final String tagKey, final String tagValue, final boolean useELBSystemAccount){
	  final AutoScalingCreateGroupTask task =
	      new AutoScalingCreateGroupTask(groupName, availabilityZones, vpcZoneIdentifier, capacity, launchConfigName, tagKey, tagValue);
	  checkResult(
	      task,
	      new AutoScalingSystemActivity( useELBSystemAccount ),
	      "failed to create autoscaling group"
	      );
	}

	public void createOrUpdateAutoscalingTags(final String tagKey,
      final String tagValue, final String asgName){
	  createOrUpdateAutoscalingTagsImpl(tagKey, tagValue, asgName, true);
	}

	public void createOrUpdateAutoscalingTags(final String tagKey,
      final String tagValue, final String asgName, final boolean useELBSystemAccount){
	  createOrUpdateAutoscalingTagsImpl(tagKey, tagValue, asgName, useELBSystemAccount);
	}

  public void createOrUpdateAutoscalingTagsImpl(final String tagKey,
      final String tagValue, final String asgName, final boolean useELBSystemAccount) {
    final AutoscalingCreateOrUpdateTagsTask task = new AutoscalingCreateOrUpdateTagsTask(
        tagKey, tagValue, asgName);
    checkResult(
        task,
        new AutoScalingSystemActivity(useELBSystemAccount),
        "failed to create/update autoscaling tags"
    );
  }

  public LaunchConfigurationType describeLaunchConfiguration(final String launchConfigName){
    return describeLaunchConfigurationImpl(launchConfigName, true);
  }
  public LaunchConfigurationType describeLaunchConfiguration(final String launchConfigName, boolean useELBSystemAccount){
    return describeLaunchConfigurationImpl(launchConfigName, useELBSystemAccount);
  }
  private LaunchConfigurationType describeLaunchConfigurationImpl(final String launchConfigName, final boolean useELBSystemAccount){
    return resultOf(
        new AutoScalingDescribeLaunchConfigsTask(launchConfigName),
        new AutoScalingSystemActivity(useELBSystemAccount),
        "failed to describe launch configuration"
        );
  }

  public void deleteLaunchConfiguration(final String launchConfigName){
    deleteLaunchConfigurationImpl(launchConfigName, true);
  }
  public void deleteLaunchConfiguration(final String launchConfigName, final boolean useELBSystemAccount){
    deleteLaunchConfigurationImpl(launchConfigName, useELBSystemAccount);
  }
	private void deleteLaunchConfigurationImpl(final String launchConfigName, final boolean useELBSystemAccount){
		checkResult(
				new AutoScalingDeleteLaunchConfigTask(launchConfigName),
				new AutoScalingSystemActivity( useELBSystemAccount ),
				"failed to delete launch configuration"
		);
	}

  public void deleteAutoScalingGroup(final String groupName, final boolean terminateInstances){
    deleteAutoScalingGroupImpl(groupName, terminateInstances, true);
  }
  public void deleteAutoScalingGroup(final String groupName, final boolean terminateInstances, final boolean useELBSystemAccount){
    deleteAutoScalingGroupImpl(groupName, terminateInstances, useELBSystemAccount);
  }
	private void deleteAutoScalingGroupImpl(final String groupName, final boolean terminateInstances, final boolean useELBSystemAccount){
		checkResult(
				new AutoScalingDeleteGroupTask(groupName, terminateInstances),
				new AutoScalingSystemActivity( useELBSystemAccount ),
				"failed to delete autoscaling group"
		);
	}

	public DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsWithVerbose(final List<String> groupNames){
	  final List<String> namesWithVerbose = Lists.newArrayList();
	  namesWithVerbose.addAll(groupNames);
	  namesWithVerbose.add("verbose");
	  return describeAutoScalingGroupsImpl(namesWithVerbose, false);
  }
	public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups(final List<String> groupNames){
	  return describeAutoScalingGroupsImpl(groupNames, true);
	}
	public DescribeAutoScalingGroupsResponseType describeAutoScalingGroups(final List<String> groupNames, final boolean useELBSystemAccount){
	  return describeAutoScalingGroupsImpl(groupNames, useELBSystemAccount);
	}
	private DescribeAutoScalingGroupsResponseType describeAutoScalingGroupsImpl(final List<String> groupNames, final boolean useELBSystemAccount){
		return resultOf(
				new AutoScalingDescribeGroupsTask(groupNames),
				new AutoScalingSystemActivity(useELBSystemAccount),
				"failed to describe autoscaling groups"
		);
	}

	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final int capacity){
	  updateAutoScalingGroup(groupName, zones, capacity, null, true);
	}
	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final int capacity, final boolean useELBSystemAccount){
	  updateAutoScalingGroup(groupName, zones, capacity, null, useELBSystemAccount);
	}

	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final Integer capacity, final String launchConfigName){
	  updateAutoScalingGroupImpl(groupName, zones, capacity, launchConfigName, true);
	}
	public void updateAutoScalingGroup(final String groupName, final List<String> zones, final Integer capacity, final String launchConfigName, final boolean useELBSystemAccount){
	  updateAutoScalingGroupImpl(groupName, zones, capacity, launchConfigName, useELBSystemAccount);
	}
	private void updateAutoScalingGroupImpl(final String groupName, final List<String> zones, final Integer capacity, final String launchConfigName, final boolean useELBSystemAccount){
	  checkResult(
	      new AutoScalingUpdateGroupTask(groupName, zones, capacity, launchConfigName),
	      new AutoScalingSystemActivity( useELBSystemAccount ),
	      "failed to update autoscaling group"
	      );
	}

	public List<RoleType> listRoles(final String pathPrefix){
		return resultOf(
				new EuareListRolesTask(pathPrefix),
				new EuareSystemActivity(),
				"failed to list IAM roles"
		);
	}

	public RoleType createRole(final String roleName, final String path, final String assumeRolePolicy){
		return resultOf(
				new EuareCreateRoleTask(roleName, path, assumeRolePolicy),
				new EuareSystemActivity(),
				"failed to create IAM role"
		);
	}

	public List<SecurityGroupItemType> describeUserSecurityGroupsById(
			final AccountFullName accountFullName,
			final String vpcId,
			final Collection<String> securityGroupIds ){
		return resultOf(
				new EucaDescribeSecurityGroupsTask( vpcId, securityGroupIds ),
				new ComputeUserActivity( accountFullName ),
				"failed to describe security groups"
		);
	}

	public void modifySecurityGroups(
	    final String instanceId,
	    final Collection<String> securityGroupIds
	    ) {
	  modifySecurityGroupsImpl(instanceId, securityGroupIds, true);
	}
	public void modifySecurityGroups(
	    final String instanceId,
	    final Collection<String> securityGroupIds,
	    final boolean useELBSystemAccount
	    ){
	  modifySecurityGroupsImpl(instanceId, securityGroupIds, useELBSystemAccount);
	}
	private void modifySecurityGroupsImpl(
	    final String instanceId,
	    final Collection<String> securityGroupIds,
	    final boolean useELBSystemAccount
	    ) {
	  checkResult(
	      new EucalyptusModifySecurityGroupsTask( instanceId, securityGroupIds ),
	      new ComputeSystemActivity( useELBSystemAccount ),
	      "failed to modify security groups"
	      );
	}

	public Optional<VpcType> defaultVpc( final AccountFullName accountFullName ) {
		return Iterables.tryFind( resultOf(
				new EucaDescribeVpcsTask( true ),
				new ComputeUserActivity( accountFullName ),
				"failed to describe default vpc"
		), Predicates.alwaysTrue() );
	}

	public List<VpcType> describeSystemVpcs(final List<String> vpcIds) {
		return resultOf(
				new EucaDescribeVpcsTask( null, vpcIds ),
				new ComputeSystemActivity(),
				"failed to describe system vpc"
		);
	}

	//// FIXME: The usage from LoadBalancingBackendService is probably wrong.
	public List<SubnetType> describeSubnets(final Collection<String> subnetIds ){
		return resultOf(
				new EucaDescribeSubnetsTask( subnetIds ),
				new ComputeSystemActivity(),
				"failed to describe subnets"
		);
	}

	public List<SubnetType> describeSubnetsByZone(
			final String vpcId,
			final Boolean defaultSubnet,
			final Collection<String> zones ){
		return resultOf(
				new EucaDescribeSubnetsTask( vpcId, defaultSubnet, zones ),
				new ComputeSystemActivity(),
				"failed to describe subnets"
		);
	}

	public List<AddressInfoType> describeSystemAddresses(final boolean vpc) {
		return resultOf(
				new EucaDescribeAddressesTask( vpc? "vpc" : "standard"),
				new ComputeSystemActivity(),
				"failed to describe addresses"
		);
	}

	public List<AddressInfoType> describeSystemAddresses(final boolean vpc, final String publicIp) {
		return resultOf(
				new EucaDescribeAddressesTask( vpc? "vpc" : "standard", publicIp),
				new ComputeSystemActivity(),
				"failed to describe addresses"
		);
	}

	public List<InternetGatewayType> describeInternetGateways(final Collection<String> vpcIds ){
		return resultOf(
				new EucaDescribeInternetGatewaysTask(vpcIds),
				new ComputeSystemActivity(),
				"failed to describe internet gateways"
		);
	}

  public ServerCertificateType getServerCertificate(final String accountNumber, final String certName){
		return resultOf(
				new EuareGetServerCertificateTask(certName),
				new EuareUserActivity( AccountFullName.getInstance( accountNumber ) ),
				"failed to get server certificate"
		);
	}

  public void deleteRole(final String roleName){
    deleteRoleImpl(roleName, true);
  }
  public void deleteRole(final String roleName, boolean useELBSystemAccount){
    deleteRoleImpl(roleName, useELBSystemAccount);
  }
	private void deleteRoleImpl(final String roleName, boolean useELBSystemAccount){
		checkResult(
				new EuareDeleteRoleTask(roleName),
				new EuareSystemActivity(useELBSystemAccount),
				"failed to delete IAM role"
		);
	}

	public List<InstanceProfileType> listInstanceProfiles(String pathPrefix){
		return resultOf(
				new EuareListInstanceProfilesTask(pathPrefix),
				new EuareSystemActivity(),
				"failed to list IAM instance profile"
		);
	}

	public InstanceProfileType createInstanceProfile(String profileName, String path){
		return resultOf(
				new EuareCreateInstanceProfileTask(profileName, path),
				new EuareSystemActivity(),
				"failed to create IAM instance profile"
		);
	}

  public void deleteInstanceProfile(String profileName){
    deleteInstanceProfileImpl(profileName, true);
  }
  public void deleteInstanceProfile(String profileName, boolean useELBSystemAccount){
    deleteInstanceProfileImpl(profileName, useELBSystemAccount);
  }
	private void deleteInstanceProfileImpl(String profileName, boolean useELBSystemAccount){
		checkResult(
				new EuareDeleteInstanceProfileTask(profileName),
				new EuareSystemActivity( useELBSystemAccount ),
				"failed to delete IAM instance profile"
		);
	}

	public void addRoleToInstanceProfile(String instanceProfileName, String roleName){
		checkResult(
				new EuareAddRoleToInstanceProfileTask(instanceProfileName, roleName),
				new EuareSystemActivity( ),
				"failed to add role to the instance profile"
		);
	}

	public void removeRoleFromInstanceProfile(String instanceProfileName, String roleName){
	  removeRoleFromInstanceProfileImpl(instanceProfileName, roleName, true);
	}
	public void removeRoleFromInstanceProfile(String instanceProfileName, String roleName, boolean useELBSystemAccount){
	  removeRoleFromInstanceProfileImpl(instanceProfileName, roleName, useELBSystemAccount);
	}
	private void removeRoleFromInstanceProfileImpl(String instanceProfileName, String roleName, boolean useELBSystemAccount){
	  checkResult(
	      new EuareRemoveRoleFromInstanceProfileTask(instanceProfileName, roleName),
	      new EuareSystemActivity( useELBSystemAccount ),
	      "failed to remove role from the instance profile"
	      );
	}

	public List<String> listRolePolicies(final String roleName){
		return resultOf(
				new EuareListRolePoliciesTask(roleName),
				new EuareSystemActivity(),
				"failed to list role's policies"
		);
	}

	public GetRolePolicyResult getRolePolicy(String roleName, String policyName){
		return resultOf(
				new EuareGetRolePolicyTask(roleName, policyName),
				new EuareSystemActivity(),
				"failed to get role's policy"
		);
	}

	public void putRolePolicy(String roleName, String policyName, String policyDocument){
	  putRolePolicyImpl(roleName, policyName, policyDocument, true);
	}

	public void putRolePolicy(String roleName, String policyName, String policyDocument, boolean useELBSystemAccount){
    putRolePolicyImpl(roleName, policyName, policyDocument, useELBSystemAccount);
	}

	private void putRolePolicyImpl(String roleName, String policyName, String policyDocument, boolean useELBSystemAccount){
		checkResult(
				new EuarePutRolePolicyTask(roleName, policyName, policyDocument),
				new EuareSystemActivity( useELBSystemAccount ),
				"failed to put role's policy"
		);
	}

	public void deleteRolePolicy(String roleName, String policyName){
	  deleteRolePolicyImpl(roleName, policyName, true);
	}
	public void deleteRolePolicy(String roleName, String policyName, boolean useELBSystemAccount){
	  deleteRolePolicyImpl(roleName, policyName, useELBSystemAccount);
	}
	private void deleteRolePolicyImpl(String roleName, String policyName, boolean useELBSystemAccount){
	  checkResult(
	      new EuareDeleteRolePolicyTask(roleName, policyName),
	      new EuareSystemActivity( useELBSystemAccount ),
	      "failed to delete role's policy"
	      );
	}

	public List<ImageDetails> describeImages(final List<String> imageIds){
		return resultOf(
				new EucaDescribeImagesTask(imageIds),
				new ComputeSystemActivity(),
				"failed to describe images"
		);
	}

	public List<VmTypeDetails> describeInstanceTypes(final List<String> instanceTypes) {
	  return resultOf(
	      new EucaDescribeInstanceTypesTask(instanceTypes),
	      new ComputeSystemActivity(),
	      "failed to describe instance types"
	      );
	}

	public List<ImageDetails> describeImagesWithVerbose(final List<String> imageIds){
	  final List<String> idsWithVerbose = Lists.newArrayList(imageIds);
	  idsWithVerbose.add("verbose");
    return resultOf(
        new EucaDescribeImagesTask(idsWithVerbose),
        new ComputeSystemActivity(false),
        "failed to describe images"
    );
  }

	public void createTags(final String tagKey, final String tagValue, final List<String> resources){
		checkResult(
				new EucaCreateTagsTask(tagKey, tagValue, resources),
				new ComputeSystemActivity( ),
				"failed to create tags"
		);
	}

	public void deleteTags(final String tagKey, final String tagValue, final List<String> resources){
		checkResult(
				new EucaDeleteTagsTask(tagKey, tagValue, resources),
				new ComputeSystemActivity( ),
				"failed to delete tags"
		);
	}

	public String createSystemVpc(final String cidrBlock) {
		return resultOf(
				new EucaCreateVpcTask(cidrBlock),
				new ComputeSystemActivity( ),
				"failed to create system VPC"
		);
	}

	public String createSystemInternetGateway() {
		return resultOf(
				new EucaCreateInternetGatewayTask(),
				new ComputeSystemActivity(),
				"failed to create Internet gateway"
		);
	}

	public void attachSystemInternetGateway(final String vpcId, final String gatewayId) {
		checkResult(
				new EucaAttachInternetGatewayTask(vpcId, gatewayId),
				new ComputeSystemActivity(),
				"failed to attach Internet gateway"
		);
	}

	public String createSystemSubnet(final String vpcId, final String availabilityZone, final String cidrBlock) {
		return resultOf(
				new EucaCreateSubnetTask(vpcId, availabilityZone, cidrBlock),
				new ComputeSystemActivity(),
				"failed to create subnet"
		);
	}

	public List<RouteTableType> describeSystemRouteTables() {
		return describeSystemRouteTables(null, null);
	}

	public List<RouteTableType> describeSystemRouteTables(final String routeTableId, final String vpcId) {
		final List<RouteTableType> tables =
				resultOf(
						new EucaDescribeRouteTableTask(routeTableId, vpcId),
						new ComputeSystemActivity(),
						"failed to describe route table"
				);
		return tables;
	}

	public String createSystemRouteTable(final String vpcId) {
		return resultOf(
				new EucaCreateRouteTableTask(vpcId),
				new ComputeSystemActivity(),
				"failed to create custom route table"
		);
	}

	public void deleteSystemRoute(final String routeTableId, final String destCidr) {
		checkResult(
				new EucaDeleteRouteTask(routeTableId, destCidr),
				new ComputeSystemActivity(),
				"failed to delete route"
		);
	}
	public void createSystemRouteToInternetGateway(final String routeTableId, final String destCidr, final String gatewayId ) {
		checkResult(
				new EucaCreateRouteTask(routeTableId, destCidr, gatewayId, null),
				new ComputeSystemActivity(),
				"failed to create a route"
		);
	}

	public void createSystemRouteToNatGateway(final String routeTableId, final String destCidr, final String gatewayId) {
		checkResult(
				new EucaCreateRouteTask(routeTableId, destCidr, null, gatewayId),
				new ComputeSystemActivity(),
				"failed to create a route"
		);
	}

	public void associateSystemRouteTable(final String subnetId, final String routeTableId) {
		checkResult(
				new EucaAssociateRouteTableTask(subnetId, routeTableId),
				new ComputeSystemActivity(),
				"failed to associate a route table with subnet"
		);
	}

	public AllocateAddressResponseType allocateSystemVpcAddress() {
		return resultOf(
				new EucaAllocateAddressTask(true),
				new ComputeSystemActivity(),
				"failed to allocate address"
		);
	}

	public void associateSystemVpcAddress(final String allocationId, final String networkInterfaceId) {
		checkResult(
				new EucaAssociateAddressTask(allocationId, networkInterfaceId),
				new ComputeSystemActivity(),
				"failed to associate EIP address with network interface"
		);
	}

	public void disassociateSystemVpcAddress(final String publicIp) {
		checkResult(
				new EucaDisassociateAddressTask(publicIp),
				new ComputeSystemActivity(),
				"failed to disassociate EIP address"
		);
	}

	public List<NatGatewayType> describeSystemNatGateway(final String subnetId) {
		return resultOf(
				new EucaDescribeNatGatewayTask(subnetId),
				new ComputeSystemActivity(),
				"failed to describe nat gateway"
		);
	}

	public String createSystemNatGateway(final String subnetId, final String elasticIpAllocationId) {
		return resultOf(
				new EucaCreateNatGatewayTask(subnetId, elasticIpAllocationId),
				new ComputeSystemActivity(),
				"failed to create nat gateway"
		);
	}

	public List<NetworkInterfaceType> describeSystemNetworkInterfaces(final String subnetId) {
		return resultOf(
				new EucaDescribeNetworkInterfacesTask(null, subnetId),
				new ComputeSystemActivity(),
				"failed to describe network interfaces"
		);
	}

	public List<NetworkInterfaceType> describeSystemNetworkInterfaces(final List<String> networkInterfaceIds) {
		return resultOf(
				new EucaDescribeNetworkInterfacesTask(networkInterfaceIds, null),
				new ComputeSystemActivity(),
				"failed to describe network interfaces"
		);
	}

	public NetworkInterfaceType createNetworkInterface(final String subnetId, final List<String> securityGroupIds) {
		return resultOf(
				new EucaCreateNetworkInterfaceTask(subnetId, securityGroupIds),
				new ComputeSystemActivity(),
				"failed to create network interface"
		);
	}

	public void attachNetworkInterface(final String instanceId, final String networkInterfaceId, final int deviceIndex) {
		checkResult(new EucaAttachNetworkInterfaceTask(instanceId, networkInterfaceId, deviceIndex),
				new ComputeSystemActivity(),
				String.format("failed to attach network interface %s to %s at index %d",
						networkInterfaceId, instanceId, deviceIndex));
	}

	public void modifyNetworkInterfaceSecurityGroups(final String networkInterfaceId, final List<String> securityGroupIds) {
		checkResult(
				new EucaModifyNetworkInterfaceAttribute(networkInterfaceId, securityGroupIds),
				new ComputeSystemActivity(),
				String.format("failed to modify network interface %s", networkInterfaceId)
		);
	}

	public void modifyNetworkInterfaceDeleteOnTerminate(final String networkInterfaceId,
														final String attachmentId,
														final boolean deleteOnTerminate) {
		checkResult(
				new EucaModifyNetworkInterfaceAttribute(networkInterfaceId, attachmentId, deleteOnTerminate),
				new ComputeSystemActivity(),
				String.format("failed to modify network interface %s", networkInterfaceId)
		);
	}

	public void revokePermissionFromOtherGroup(final String groupId, final String sourceAccountId,
											   final String sourceGroupId, final String protocol) {
		checkResult(
				new EucalyptusRevokeIngressRuleFromOtherGroupTask(groupId, protocol, null, null, sourceAccountId, sourceGroupId),
				new ComputeSystemActivity(),
				"Failed to revoke security group permission"
		);
	}

	public void revokeSystemSecurityGroupEgressRules(final String groupId) {
		checkResult(
				new EucalyptusRevokeEgressRuleTask(groupId, "-1", -1, -1, "0.0.0.0/0"),
				new ComputeSystemActivity(),
				"Failed to revoke egress rule"
		);
	}

	public void authorizeSystemSecurityGroupEgressRule(final String groupId,
														final String protocol,
														int fromPort,
														int toPort,
														final String sourceCidrRange) {
		checkResult(
				new EucalyptusAuthorizeEgressRuleTask(groupId, protocol, fromPort, toPort, sourceCidrRange),
				new ComputeSystemActivity(),
				"Failed to authorize egress rule"
		);
	}

	public void terminateInstances(final List<String> instanceIds) {
		checkResult(
						new EucaTerminateInstancesTask(instanceIds),
						new ComputeSystemActivity(),
						"Failed to terminate VMs"
		);
	}

	private class EucaDescribeImagesTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<ImageDetails>> {
		private List<String> imageIds = null;
		private EucaDescribeImagesTask(final List<String> imageIds){
			this.imageIds = imageIds;
		}

		DescribeImagesType getRequest(){
			final DescribeImagesType req = new DescribeImagesType();
			if(this.imageIds!=null && this.imageIds.size()>0){
				req.setFilterSet( Lists.newArrayList( Filter.filter( "image-id", this.imageIds ) ) );
			}
			return req;
		}

		@Override
		List<ImageDetails> extractResult(ComputeMessage response) {
			final DescribeImagesResponseType resp = (DescribeImagesResponseType) response;
			return resp.getImagesSet();
		}
	}

	private class EucaDescribeInstanceTypesTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<VmTypeDetails>> {
	  private List<String> instanceTypes = Lists.newArrayList();

	  private EucaDescribeInstanceTypesTask() {
	  }

	  private EucaDescribeInstanceTypesTask(final List<String> instanceTypes) {
	    this.instanceTypes.addAll(instanceTypes);
	  }

    @Override
    List<VmTypeDetails> extractResult(ComputeMessage response) {
      final DescribeInstanceTypesResponseType resp = (DescribeInstanceTypesResponseType) response;
      return resp.getInstanceTypeDetails();
    }

    @Override
    DescribeInstanceTypesType getRequest() {
      final DescribeInstanceTypesType req = new DescribeInstanceTypesType();
      req.setInstanceTypes((ArrayList<String>) instanceTypes);
      return req;
    }
	}

	private class EucaTerminateInstancesTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private List<String> instanceIds = Lists.newArrayList();

		private EucaTerminateInstancesTask(final List<String> instanceIds) {
			this.instanceIds.addAll(instanceIds);
		}

		ComputeMessage getRequest() {
			final TerminateInstancesType req = new TerminateInstancesType();
			req.setInstancesSet((ArrayList<String>) this.instanceIds);
			return req;
		}
	}

	private class EucaDeleteTagsTask extends EucalyptusActivityTask<ComputeMessage, Compute>{
		private String tagKey = null;
		private String tagValue = null;
		private List<String> resources = null;

		private EucaDeleteTagsTask(final String tagKey, final String tagValue, final List<String> resources){
			this.tagKey = tagKey;
			this.tagValue = tagValue;
			this.resources = resources;
		}

		DeleteTagsType getRequest(){
			final DeleteTagsType req = new DeleteTagsType();
			req.setResourcesSet(Lists.newArrayList(this.resources));
			final DeleteResourceTag tag = new DeleteResourceTag();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			req.setTagSet(Lists.newArrayList(tag));
			return req;
		}
	}

	private class EucaCreateTagsTask extends EucalyptusActivityTask<ComputeMessage, Compute>{
		private String tagKey = null;
		private String tagValue = null;
		private List<String> resources = null;
		private EucaCreateTagsTask(final String tagKey, final String tagValue, final List<String> resources){
			this.tagKey = tagKey;
			this.tagValue = tagValue;
			this.resources = resources;
		}
		CreateTagsType getRequest(){
			final CreateTagsType req = new CreateTagsType();
			req.setResourcesSet(Lists.newArrayList(this.resources));
			final ResourceTag tag = new ResourceTag();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			req.setTagSet(Lists.newArrayList(tag));
			return req;
		}
	}

	private class EucaDescribeSecurityGroupsTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<SecurityGroupItemType>> {
		private String vpcId = null;
		private Collection<String> securityGroupIds = null;
		private EucaDescribeSecurityGroupsTask( final String vpcId, final Collection<String> securityGroupIds){
			this.vpcId = vpcId;
			this.securityGroupIds = securityGroupIds;
		}

		DescribeSecurityGroupsType getRequest( ){
			final DescribeSecurityGroupsType req = new DescribeSecurityGroupsType( );
			if ( vpcId != null ) {
				req.getFilterSet( ).add( filter( "vpc-id", vpcId ) );
			}
			req.getFilterSet( ).add( filter( "group-id", securityGroupIds ) );
			return req;
		}

		@Override
		List<SecurityGroupItemType> extractResult( ComputeMessage response ) {
			final DescribeSecurityGroupsResponseType resp = (DescribeSecurityGroupsResponseType) response;
			return resp.getSecurityGroupInfo( );
		}
	}

	private class EucaCreateVpcTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, String> {
		private String cidr = null;
		private EucaCreateVpcTask( final String cidr) {
			this.cidr = cidr;
		}

		ComputeMessage getRequest( ) {
			final CreateVpcType req = new CreateVpcType();
			req.setCidrBlock(this.cidr);
			return req;
		}

		@Override
		String extractResult( ComputeMessage resp ) {
			final CreateVpcResponseType response  = (CreateVpcResponseType) resp;
			return response.getVpc().getVpcId();
		}
	}

	private class EucaCreateInternetGatewayTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, String> {
		private EucaCreateInternetGatewayTask() {
		}
		ComputeMessage getRequest( ) {
			final CreateInternetGatewayType req = new CreateInternetGatewayType();
			return req;
		}

		@Override
		String extractResult( ComputeMessage resp ) {
			final CreateInternetGatewayResponseType response = (CreateInternetGatewayResponseType) resp;
			return response.getInternetGateway().getInternetGatewayId();
		}
	}

	private class EucaAttachInternetGatewayTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String vpcId = null;
		private String gatewayId = null;
		private EucaAttachInternetGatewayTask(final String vpcId, final String gatewayId) {
			this.vpcId = vpcId;
			this.gatewayId = gatewayId;
		}

		ComputeMessage getRequest( ) {
			final AttachInternetGatewayType req = new AttachInternetGatewayType();
			req.setVpcId(this.vpcId);
			req.setInternetGatewayId(this.gatewayId);
			return req;
		}
	}

	private class EucaCreateSubnetTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, String> {
		private String vpcId = null;
		private String availabilityZone = null;
		private String cidr = null;

		private EucaCreateSubnetTask(final String vpcId, final String availabilityZone, final String cidr) {
			this.vpcId = vpcId;
			this.availabilityZone = availabilityZone;
			this.cidr = cidr;
		}

		ComputeMessage getRequest( ) {
			final CreateSubnetType req = new CreateSubnetType();
			req.setVpcId( this.vpcId );
			req.setAvailabilityZone( this.availabilityZone );
			req.setCidrBlock( this.cidr );
			return req;
		}

		String extractResult( ComputeMessage resp ) {
			final CreateSubnetResponseType response = (CreateSubnetResponseType) resp;
			return response.getSubnet().getSubnetId();
		}
	}

	private class EucaCreateRouteTableTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, String> {
		private String vpcId = null;
		private EucaCreateRouteTableTask(final String vpcId) {
			this.vpcId = vpcId;
		}

		ComputeMessage getRequest( ) {
			final CreateRouteTableType req = new CreateRouteTableType();
			req.setVpcId( this.vpcId );
			return req;
		}

		String extractResult( ComputeMessage resp) {
			final CreateRouteTableResponseType response = (CreateRouteTableResponseType) resp;
			return response.getRouteTable().getRouteTableId();
		}
	}

	private class EucaDescribeRouteTableTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<RouteTableType>> {
		private String vpcId = null;
		private String routeTableId = null;
		private EucaDescribeRouteTableTask(final String routeTableId, final String vpcId) {
			this.routeTableId = routeTableId;
			this.vpcId = vpcId;
		}
		private EucaDescribeRouteTableTask(final String routeTableId) {
			this.routeTableId = routeTableId;
		}

		ComputeMessage getRequest( ) {
			final DescribeRouteTablesType req = new DescribeRouteTablesType();
			if(this.routeTableId!=null) {
				req.getFilterSet().add( filter( "route-table-id", this.routeTableId) );
			}
			if(this.vpcId != null) {
				req.getFilterSet().add( filter( "vpc-id", this.vpcId) );
			}
			return req;
		}

		List<RouteTableType> extractResult(ComputeMessage resp) {
			final DescribeRouteTablesResponseType response =
					(DescribeRouteTablesResponseType) resp;
			return response.getRouteTableSet().getItem();
		}
	}
	private class EucaAssociateRouteTableTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String subnetId = null;
		private String routeTableId = null;

		private EucaAssociateRouteTableTask(final String subnetId, final String routeTableId) {
			this.subnetId = subnetId;
			this.routeTableId = routeTableId;
		}

		ComputeMessage getRequest( ) {
			final AssociateRouteTableType req = new AssociateRouteTableType();
			req.setSubnetId(this.subnetId);
			req.setRouteTableId(this.routeTableId);
			return req;
		}
	}

	private class EucaAssociateAddressTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String allocationId = null;
		private String networkInterfaceId = null;

		private EucaAssociateAddressTask(final String allocationId, final String networkInterfaceId) {
			this.allocationId = allocationId;
			this.networkInterfaceId = networkInterfaceId;
		}

		ComputeMessage getRequest() {
			final AssociateAddressType req = new AssociateAddressType();
			req.setAllocationId( this.allocationId );
			req.setNetworkInterfaceId( this.networkInterfaceId );
			return req;
		}
	}

	private class EucaDisassociateAddressTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String publicIp = null;

		private EucaDisassociateAddressTask(final String publicIp) {
			this.publicIp = publicIp;
		}

		ComputeMessage getRequest() {
			final DisassociateAddressType req = new DisassociateAddressType();
			req.setPublicIp(this.publicIp);
			return req;
		}
	}

	private class EucaDescribeAddressesTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<AddressInfoType>> {
		private String domain = null;
		private String publicIp = null;
		private EucaDescribeAddressesTask() {}

		private EucaDescribeAddressesTask(final String domain) {
			this.domain = domain;
		}

		private EucaDescribeAddressesTask(final String domain, final String publicIp) {
			this.domain = domain;
			this.publicIp = publicIp;
		}

		ComputeMessage getRequest( ) {
			final DescribeAddressesType req = new DescribeAddressesType();
			if(this.domain!=null) {
				req.getFilterSet().add( filter("domain", domain));
			}
			if(this.publicIp!=null) {
				req.getFilterSet().add( filter("public-ip", this.publicIp));
			}
			return req;
		}

		List<AddressInfoType> extractResult(final ComputeMessage resp) {
			final DescribeAddressesResponseType response = (DescribeAddressesResponseType) resp;
			return response.getAddressesSet();
		}
	}
	private class EucaAllocateAddressTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, AllocateAddressResponseType> {
		private boolean isVpc = false;

		private EucaAllocateAddressTask(final boolean isVpc) {
			this.isVpc = isVpc;
		}

		ComputeMessage getRequest() {
			final AllocateAddressType req = new AllocateAddressType();
			if (this.isVpc)
				req.setDomain("vpc");
			return req;
		}

		@Override
		AllocateAddressResponseType extractResult(final ComputeMessage resp) {
			final AllocateAddressResponseType response =
					(AllocateAddressResponseType) resp;
			return response;
		}
	}

	private class EucaAttachNetworkInterfaceTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String instanceId = null;
		private String interfaceId = null;
		private int deviceIdx = 1;
		private EucaAttachNetworkInterfaceTask(final String instanceId,
											   final String interfaceId,
											   final int deviceIndex) {
			this.instanceId = instanceId;
			this.interfaceId = interfaceId;
			this.deviceIdx = deviceIndex;
		}

		@Override
		ComputeMessage getRequest() {
			final AttachNetworkInterfaceType req = new AttachNetworkInterfaceType();
			req.setInstanceId(this.instanceId);
			req.setNetworkInterfaceId(this.interfaceId);
			req.setDeviceIndex(this.deviceIdx);
			return req;
		}
	}

	private class EucaCreateNetworkInterfaceTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, NetworkInterfaceType> {
		private String subnetId = null;
		private List<String> securityGroupIds = null;
		private EucaCreateNetworkInterfaceTask(final String subnetId) {
			this.subnetId = subnetId;
		}
		private EucaCreateNetworkInterfaceTask(final String subnetId, final List<String> securityGrupIds) {
			this.subnetId = subnetId;
			this.securityGroupIds = securityGrupIds;
		}

		@Override
		ComputeMessage getRequest() {
			final CreateNetworkInterfaceType req = new CreateNetworkInterfaceType();
			req.setSubnetId(this.subnetId);
			if(this.securityGroupIds!=null && ! this.securityGroupIds.isEmpty()) {
				final SecurityGroupIdSetType groupIds = new SecurityGroupIdSetType();
				groupIds.setItem(new ArrayList<>(
						this.securityGroupIds.stream()
						.map(id -> {
							final SecurityGroupIdSetItemType item =
									new SecurityGroupIdSetItemType();
							item.setGroupId(id);
							return item;
						})
						.collect(Collectors.toList())));
				req.setGroupSet( groupIds );
			}
			return req;
		}

		@Override
		NetworkInterfaceType extractResult(ComputeMessage resp) {
			final CreateNetworkInterfaceResponseType response = (CreateNetworkInterfaceResponseType) resp;
			return response.getNetworkInterface();
		}
	}

	private class EucaDescribeNetworkInterfacesTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<NetworkInterfaceType>> {
		private List<String> interfaceIds = null;
		private String subnetId = null;

		private EucaDescribeNetworkInterfacesTask(final List<String> interfaceIds, final String subnetId) {
			this.interfaceIds = interfaceIds;
			this.subnetId = subnetId;
		}

		@Override
		ComputeMessage getRequest() {
			final DescribeNetworkInterfacesType req =
					new DescribeNetworkInterfacesType();
			if(this.interfaceIds!=null) {
				for (final String interfaceId: this.interfaceIds) {
					req.getFilterSet().add(filter("network-interface-id", interfaceId));
				}
			}
			if(this.subnetId!=null) {
				req.getFilterSet().add(filter("subnet-id", this.subnetId));
			}
			return req;
		}

		@Override
		List<NetworkInterfaceType> extractResult(ComputeMessage resp) {
			final DescribeNetworkInterfacesResponseType response = (DescribeNetworkInterfacesResponseType) resp;
			return response.getNetworkInterfaceSet().getItem();
		}
	}

	private class EucaModifyNetworkInterfaceAttribute extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String networkInterfaceId = null;
		private List<String> securityGroupIds = null;

		private String attachmentId = null;
		private Optional<Boolean> deleteOnTerminate =  Optional.absent();
		private EucaModifyNetworkInterfaceAttribute(final String networkInterfaceId, final List<String> securityGroupIds) {
			this.networkInterfaceId = networkInterfaceId;
			this.securityGroupIds = securityGroupIds;
		}

		private EucaModifyNetworkInterfaceAttribute(final String networkInterfaceId, final String attachmentId, final boolean deleteOnTerminate) {
			this.networkInterfaceId = networkInterfaceId;
			this.attachmentId = attachmentId;
			this.deleteOnTerminate = Optional.of(deleteOnTerminate);
		}
		@Override
		ComputeMessage getRequest() {
			final ModifyNetworkInterfaceAttributeType req = new ModifyNetworkInterfaceAttributeType();
			req.setNetworkInterfaceId(this.networkInterfaceId);
			if(this.securityGroupIds!=null) {
				final SecurityGroupIdSetType groupIds = new SecurityGroupIdSetType();
				groupIds.setItem(new ArrayList<>(
						this.securityGroupIds.stream()
								.map(id -> {
									final SecurityGroupIdSetItemType item =
											new SecurityGroupIdSetItemType();
									item.setGroupId(id);
									return item;
								})
								.collect(Collectors.toList())));
				req.setGroupSet(groupIds);
			}
			if(this.attachmentId!=null && this.deleteOnTerminate.isPresent()) {
				final ModifyNetworkInterfaceAttachmentType attachment = new ModifyNetworkInterfaceAttachmentType();
				attachment.setAttachmentId(this.attachmentId);
				attachment.setDeleteOnTermination(this.deleteOnTerminate.get());
				req.setAttachment(attachment);
			}
			return req;
		}
	}

	private class EucaDescribeNatGatewayTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<NatGatewayType>> {
		private String subnetId = null;
		private EucaDescribeNatGatewayTask(final String subnetId) {
			this.subnetId = subnetId;
		}

		@Override
		ComputeMessage getRequest() {
			final DescribeNatGatewaysType req = new DescribeNatGatewaysType();
			if (this.subnetId != null) {
				req.getFilterSet().add(filter("subnet-id", this.subnetId));
			}
			return req;
		}

		@Override
		List<NatGatewayType> extractResult( final ComputeMessage resp ) {
			final DescribeNatGatewaysResponseType response = (DescribeNatGatewaysResponseType) resp;
			return response.getNatGatewaySet().getItem();
		}
	}
	private class EucaCreateNatGatewayTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, String> {
		private String subnetId = null;
		private String elasticIpAllocationId = null;

		private EucaCreateNatGatewayTask(final String subnetId, final String elasticIpAllocationId) {
			this.subnetId = subnetId;
			this.elasticIpAllocationId = elasticIpAllocationId;
		}

		@Override
		ComputeMessage getRequest() {
			final CreateNatGatewayType req = new CreateNatGatewayType();
			req.setSubnetId(this.subnetId);
			req.setAllocationId(this.elasticIpAllocationId);
			return req;
		}

		@Override
		String extractResult(final ComputeMessage resp) {
			final CreateNatGatewayResponseType response = (CreateNatGatewayResponseType) resp;
			return response.getNatGateway().getNatGatewayId();
 		}
	}

	private class EucaDeleteRouteTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String routeTableId = null;
		private String destCidr = null;

		private EucaDeleteRouteTask(final String routeTableId, final String destCidr) {
			this.routeTableId = routeTableId;
			this.destCidr = destCidr;
		}

		ComputeMessage getRequest() {
			final DeleteRouteType req = new DeleteRouteType();
			req.setRouteTableId(this.routeTableId);
			req.setDestinationCidrBlock(this.destCidr);
			return req;
		}
	}

	private class EucaCreateRouteTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String destCidr = null;
		private String internetGateway = null;
		private String natGateway = null;
		private String routeTable = null;

		private EucaCreateRouteTask(final String routeTable, final String destCidr,
									final String internetGateway, final String natGateway) {
			this.routeTable = routeTable;
			this.destCidr = destCidr;

			if(internetGateway!=null && natGateway!=null) {
				throw Exceptions.toUndeclared("Both internet gateway and nat gateway are specified");
			} else if(internetGateway == null && natGateway == null) {
				throw Exceptions.toUndeclared("Internet or nat gateway must be specified");
			}
			this.internetGateway = internetGateway;
			this.natGateway = natGateway;
		}

		ComputeMessage getRequest( ) {
			final CreateRouteType req = new CreateRouteType();
			req.setRouteTableId( this.routeTable );
			req.setDestinationCidrBlock( this.destCidr );
			if (this.internetGateway != null)
				req.setGatewayId( this.internetGateway );
			if (this.natGateway != null)
				req.setNatGatewayId( this.natGateway );
			return req;
		}
	}

	private class EucaDescribeVpcsTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<VpcType>> {
		private Collection<String> vpcIds;
		private final Boolean defaultVpc;
		private EucaDescribeVpcsTask(final Boolean defaultVpc ) {
			this( defaultVpc, null );
		}
		private EucaDescribeVpcsTask(final Collection<String> vpcIds){
			this( null, vpcIds );
		}
		private EucaDescribeVpcsTask(final Boolean defaultVpc, final Collection<String> vpcIds){
			this.defaultVpc = defaultVpc;
			this.vpcIds = vpcIds;
		}

		ComputeMessage getRequest( ){
			final DescribeVpcsType req = new DescribeVpcsType();
			if(this.defaultVpc!=null){
				req.getFilterSet().add( filter( "isDefault", String.valueOf( defaultVpc ) ) );
			}
			if(this.vpcIds!=null){
				final List<VpcIdSetItemType> idItems =
						this.vpcIds.stream().map(s -> {
							final VpcIdSetItemType item = new VpcIdSetItemType();
							item.setVpcId(s);
							return item;
						}).collect(Collectors.toList());
				req.setVpcSet( new VpcIdSetType() );
				req.getVpcSet().setItem(new ArrayList<>(idItems));
			}
			return req;
		}

		@Override
		List<VpcType> extractResult( final ComputeMessage response ) {
			final DescribeVpcsResponseType resp = (DescribeVpcsResponseType) response;
			return resp.getVpcSet( ).getItem();
		}
	}

	private class EucaDescribeSubnetsTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<SubnetType>> {
		private String vpcId = null;
		private Collection<String> subnetIds = null;
		private Collection<String> zones = null;
		private Boolean defaultSubnet = null;
		private EucaDescribeSubnetsTask(final Collection<String> subnetIds){
			this.subnetIds = subnetIds;
		}
		private EucaDescribeSubnetsTask(final String vpcId, final Boolean defaultSubnet, final Collection<String> zones){
			this.vpcId = vpcId;
			this.defaultSubnet = defaultSubnet;
			this.zones = zones;
		}

		ComputeMessage getRequest( ){
			final DescribeSubnetsType req = new DescribeSubnetsType();
			req.setSubnetSet( new SubnetIdSetType(  ) );
			req.getSubnetSet( ).getItem( ).add( new SubnetIdSetItemType() );
			req.getSubnetSet( ).getItem( ).get( 0 ).setSubnetId( "verbose" );
			if(this.vpcId!=null){
				req.getFilterSet( ).add( filter( "vpc-id", vpcId ) );
			}
			if(this.subnetIds!=null){
				req.getFilterSet( ).add( filter( "subnet-id", subnetIds ) );
			}
			if(this.zones!=null){
				req.getFilterSet( ).add( filter( "availability-zone", zones ) );
			}
			if(this.defaultSubnet!=null){
				req.getFilterSet( ).add( filter( "default-for-az", String.valueOf( this.defaultSubnet ) ) );
			}
			return req;
		}

		@Override
		List<SubnetType> extractResult( final ComputeMessage response ) {
			final DescribeSubnetsResponseType resp = (DescribeSubnetsResponseType) response;
			return resp.getSubnetSet( ).getItem( );
		}
	}

	private class EucaDescribeInternetGatewaysTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<InternetGatewayType>> {
		private Collection<String> vpcIds = null;
		private EucaDescribeInternetGatewaysTask(final Collection<String> vpcIds){
			this.vpcIds = vpcIds;
		}

		ComputeMessage getRequest( ){
			final DescribeInternetGatewaysType req = new DescribeInternetGatewaysType();
			req.setInternetGatewayIdSet( new InternetGatewayIdSetType() );
			req.getInternetGatewayIdSet().getItem( ).add( new InternetGatewayIdSetItemType() );
			req.getInternetGatewayIdSet().getItem( ).get( 0 ).setInternetGatewayId( "verbose" );
			if(this.vpcIds!=null){
				req.getFilterSet( ).add( filter( "attachment.vpc-id", this.vpcIds ) );
			}
			return req;
		}

		@Override
		List<InternetGatewayType> extractResult( final ComputeMessage response ) {
			final DescribeInternetGatewaysResponseType resp = (DescribeInternetGatewaysResponseType) response;
			return resp.getInternetGatewaySet( ).getItem( );
		}
	}

	private class EuareGetServerCertificateTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, ServerCertificateType> {
	  private String certName = null;

	  private EuareGetServerCertificateTask(final String certName){
	    this.certName = certName;
	  }

	  GetServerCertificateType getRequest( ){
	    final GetServerCertificateType req = new GetServerCertificateType();
	    req.setServerCertificateName(this.certName);
	    return req;
	  }

    @Override
		ServerCertificateType extractResult( EuareMessage response ) {
			final GetServerCertificateResponseType resp = (GetServerCertificateResponseType) response;
			if(resp.getGetServerCertificateResult()!= null)
				return resp.getGetServerCertificateResult().getServerCertificate();
			return null;
    }
	}

	private class EuareDeleteInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
		private String profileName =null;
		private EuareDeleteInstanceProfileTask(String profileName){
			this.profileName = profileName;
		}

		DeleteInstanceProfileType getRequest(){
			final DeleteInstanceProfileType req = new DeleteInstanceProfileType();
			req.setInstanceProfileName(this.profileName);
			return req;
		}
	}

	private class EuareAddRoleToInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
		private String instanceProfileName = null;
		private String roleName = null;

		private EuareAddRoleToInstanceProfileTask(final String instanceProfileName, final String roleName){
			this.instanceProfileName = instanceProfileName;
			this.roleName = roleName;
		}

		AddRoleToInstanceProfileType getRequest(){
			final AddRoleToInstanceProfileType req  = new AddRoleToInstanceProfileType();
			req.setRoleName(this.roleName);
			req.setInstanceProfileName(this.instanceProfileName);
			return req;
		}
	}

	private class EuareRemoveRoleFromInstanceProfileTask extends EucalyptusActivityTask<EuareMessage, Euare>{
	  private String instanceProfileName = null;
	  private String roleName = null;

	  private EuareRemoveRoleFromInstanceProfileTask(final String instanceProfileName, final String roleName){
	    this.instanceProfileName = instanceProfileName;
	    this.roleName = roleName;
	  }

	  RemoveRoleFromInstanceProfileType getRequest(){
	    final RemoveRoleFromInstanceProfileType req = new RemoveRoleFromInstanceProfileType();
	    req.setRoleName(this.roleName);
	    req.setInstanceProfileName(this.instanceProfileName);
	    return req;
	  }
	}

	private class EuareListInstanceProfilesTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, List<InstanceProfileType>> {
		private String pathPrefix = null;
		private EuareListInstanceProfilesTask(final String pathPrefix){
			this.pathPrefix = pathPrefix;
		}

		ListInstanceProfilesType getRequest(){
			final ListInstanceProfilesType req = new ListInstanceProfilesType();
			req.setPathPrefix(this.pathPrefix);
			return req;
		}

		@Override
		List<InstanceProfileType> extractResult(EuareMessage response) {
			ListInstanceProfilesResponseType resp = (ListInstanceProfilesResponseType) response;
			try{
				return resp.getListInstanceProfilesResult().getInstanceProfiles().getMember();
			}catch(Exception  ex){
				return null;
			}
		}
	}

	private class EuareCreateInstanceProfileTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, InstanceProfileType>{
		private String profileName = null;
		private String path = null;
		private EuareCreateInstanceProfileTask(String profileName, String path){
			this.profileName = profileName;
			this.path = path;
		}

		CreateInstanceProfileType getRequest(){
			final CreateInstanceProfileType req = new CreateInstanceProfileType();
			req.setInstanceProfileName(this.profileName);
			req.setPath(this.path);
			return req;
		}

		@Override
		InstanceProfileType extractResult(EuareMessage response) {
			final CreateInstanceProfileResponseType resp = (CreateInstanceProfileResponseType) response;
			try{
				return resp.getCreateInstanceProfileResult().getInstanceProfile();
			}catch(Exception ex){
				return null;
			}
		}
	}

	private class EuareDeleteRoleTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String roleName = null;
		private EuareDeleteRoleTask(String roleName){
			this.roleName = roleName;
		}
		DeleteRoleType getRequest(){
			final DeleteRoleType req = new DeleteRoleType();
			req.setRoleName(this.roleName);
			return req;
		}
	}

	private class EuareCreateRoleTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, RoleType> {
		String roleName = null;
		String path = null;
		String assumeRolePolicy = null;

		private EuareCreateRoleTask(String roleName, String path, String assumeRolePolicy){
			this.roleName = roleName;
			this.path = path;
			this.assumeRolePolicy = assumeRolePolicy;
		}

		CreateRoleType getRequest(){
			final CreateRoleType req = new CreateRoleType();
			req.setRoleName(this.roleName);
			req.setPath(this.path);
			req.setAssumeRolePolicyDocument(this.assumeRolePolicy);
			return req;
		}

		@Override
		RoleType extractResult( EuareMessage response) {
			CreateRoleResponseType resp = (CreateRoleResponseType) response;
			try{
				return resp.getCreateRoleResult().getRole();
			}catch(Exception ex){
				return null;
			}
		}
	}

	private class EuareListRolesTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, List<RoleType>> {
		private String pathPrefix = null;

		private EuareListRolesTask(String pathPrefix){
			this.pathPrefix = pathPrefix;
		}

		ListRolesType getRequest(){
			final ListRolesType req = new ListRolesType();
			req.setPathPrefix(this.pathPrefix);
			return req;
		}

		@Override
		List<RoleType> extractResult(EuareMessage response) {
			ListRolesResponseType resp = (ListRolesResponseType) response;
			try{
				return resp.getListRolesResult().getRoles().getMember();
			}catch(Exception ex){
				return null;
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

		PutRolePolicyType getRequest(){
			final PutRolePolicyType req =
					new PutRolePolicyType();
			req.setRoleName(this.roleName);
			req.setPolicyName(this.policyName);
			req.setPolicyDocument(this.policyDocument);

			return req;
		}
	}

	private class EuareListRolePoliciesTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, List<String>> {
		private String roleName = null;
		private EuareListRolePoliciesTask(final String roleName){
			this.roleName = roleName;
		}

		ListRolePoliciesType getRequest(){
			final ListRolePoliciesType req = new ListRolePoliciesType();
			req.setRoleName(this.roleName);
			return req;
		}

		@Override
		List<String> extractResult(EuareMessage response) {
			try{
				final ListRolePoliciesResponseType resp = (ListRolePoliciesResponseType) response;
				return resp.getListRolePoliciesResult().getPolicyNames().getMemberList();
			}catch(final Exception ex){
				return Lists.newArrayList();
			}
		}
	}

	private class EuareGetRolePolicyTask extends EucalyptusActivityTaskWithResult<EuareMessage, Euare, GetRolePolicyResult> {
		private String roleName = null;
		private String policyName = null;

		private EuareGetRolePolicyTask(final String roleName, final String policyName){
			this.roleName = roleName;
			this.policyName = policyName;
		}

		GetRolePolicyType getRequest(){
			final GetRolePolicyType req = new GetRolePolicyType();
			req.setRoleName(this.roleName);
			req.setPolicyName(this.policyName);
			return req;
		}

		@Override
		GetRolePolicyResult extractResult( EuareMessage response) {
		  try{
		    final GetRolePolicyResponseType resp = (GetRolePolicyResponseType) response;
		    return resp.getGetRolePolicyResult();
		  }catch(final Exception ex){
		    return null;
		  }
		}
	}

	private class EuareDeleteRolePolicyTask extends EucalyptusActivityTask<EuareMessage, Euare> {
		private String roleName= null;
		private String policyName = null;

		private EuareDeleteRolePolicyTask(final String roleName, final String policyName){
			this.roleName = roleName;
			this.policyName = policyName;
		}

		DeleteRolePolicyType getRequest(){
			final DeleteRolePolicyType req = new DeleteRolePolicyType();
			req.setRoleName(this.roleName);
			req.setPolicyName(this.policyName);
			return req;
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

		UpdateAutoScalingGroupType getRequest(){
			final UpdateAutoScalingGroupType req = new UpdateAutoScalingGroupType();
			req.setAutoScalingGroupName( this.groupName );

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
	}

	private class AutoScalingDescribeGroupsTask extends EucalyptusActivityTaskWithResult<AutoScalingMessage, AutoScaling, DescribeAutoScalingGroupsResponseType> {
		private List<String> groupNames = null;
		private AutoScalingDescribeGroupsTask(final List<String> groupNames){
			this.groupNames = groupNames;
		}

		DescribeAutoScalingGroupsType getRequest( ){
			final DescribeAutoScalingGroupsType req = new DescribeAutoScalingGroupsType();
			final AutoScalingGroupNames names = new AutoScalingGroupNames();
			names.setMember(Lists.<String>newArrayList());
			names.getMember().addAll(this.groupNames);
			req.setAutoScalingGroupNames(names);
			return req;
		}

		@Override
		DescribeAutoScalingGroupsResponseType extractResult( final AutoScalingMessage response ) {
			return (DescribeAutoScalingGroupsResponseType) response;
		}
	}

	private class AutoScalingCreateGroupTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String groupName = null;
		private List<String> availabilityZones = null;
		private String vpcZoneIdentifier;
		private int capacity = 1;
		private String launchConfigName = null;
		private String tagKey = null;
		private String tagValue = null;

		private AutoScalingCreateGroupTask(final String groupName, final List<String> zones, final String vpcZoneIdentifier,
				final int capacity, final String launchConfig, final String tagKey, final String tagValue){
			this.groupName = groupName;
			this.availabilityZones = zones;
			this.vpcZoneIdentifier = vpcZoneIdentifier;
			this.capacity = capacity;
			this.launchConfigName = launchConfig;
			this.tagKey = tagKey;
			this.tagValue = tagValue;
		}

		CreateAutoScalingGroupType getRequest(){
			final CreateAutoScalingGroupType req = new CreateAutoScalingGroupType();
			req.setAutoScalingGroupName(this.groupName);
			req.setAvailabilityZones( new AvailabilityZones( this.availabilityZones ) );
			req.setVpcZoneIdentifier( this.vpcZoneIdentifier );
			req.setDesiredCapacity(this.capacity);
			req.setMaxSize(this.capacity);
			req.setMinSize(this.capacity);
			req.setHealthCheckType("EC2");
			req.setLaunchConfigurationName(this.launchConfigName);
			final Tags tags = new Tags();
			final TagType tag = new TagType();
			tag.setKey(this.tagKey);
			tag.setValue(this.tagValue);
			tag.setPropagateAtLaunch(true);
			tag.setResourceType("auto-scaling-group");
			tag.setResourceId(this.groupName);
			tags.setMember(Lists.newArrayList(tag));
			req.setTags(tags);
			return req;
		}
	}

  private class AutoscalingCreateOrUpdateTagsTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling> {
    private String tagKey = null;
    private String tagValue = null;
    private String asgName = null;

    private AutoscalingCreateOrUpdateTagsTask(final String tagKey, final String tagValue, final String asgName) {
      this.tagKey = tagKey;
      this.tagValue = tagValue;
      this.asgName = asgName;
    }

    @Override
    AutoScalingMessage getRequest() {
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
  }

	private class AutoScalingDeleteGroupTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String groupName = null;
		private boolean terminateInstances = false;
		private AutoScalingDeleteGroupTask(final String groupName, final boolean terminateInstances){
			this.groupName = groupName;
			this.terminateInstances = terminateInstances;
		}

		DeleteAutoScalingGroupType getRequest(){
			final DeleteAutoScalingGroupType req = new DeleteAutoScalingGroupType();
			req.setAutoScalingGroupName(this.groupName);
			req.setForceDelete(this.terminateInstances);
			return req;
		}

		@Override
		boolean dispatchFailure(ActivityContext<AutoScalingMessage, AutoScaling> context, Throwable throwable) {
			if ( !AsyncExceptions.isWebServiceErrorCode( throwable, "ScalingActivityInProgress" ) &&
					!AsyncExceptions.isWebServiceErrorCode( throwable, "ResourceInUse" ) ) {
				return super.dispatchFailure( context, throwable );
			}
			return false;
		}
	}

	private class AutoScalingDeleteLaunchConfigTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private String launchConfigName = null;
		private AutoScalingDeleteLaunchConfigTask(final String launchConfigName){
			this.launchConfigName = launchConfigName;
		}

		DeleteLaunchConfigurationType getRequest(){
			final DeleteLaunchConfigurationType req = new DeleteLaunchConfigurationType();
			req.setLaunchConfigurationName(this.launchConfigName);
			return req;
		}
	}

	private class AutoScalingDescribeLaunchConfigsTask extends EucalyptusActivityTaskWithResult<AutoScalingMessage, AutoScaling, LaunchConfigurationType> {
		private String launchConfigName = null;
		private AutoScalingDescribeLaunchConfigsTask(final String launchConfigName){
			this.launchConfigName = launchConfigName;
		}

		DescribeLaunchConfigurationsType getRequest(){
			final DescribeLaunchConfigurationsType req = new DescribeLaunchConfigurationsType();
			final LaunchConfigurationNames names = new LaunchConfigurationNames();
			names.setMember(Lists.newArrayList(this.launchConfigName));
			req.setLaunchConfigurationNames(names);
			return req;
		}

		@Override
		LaunchConfigurationType extractResult( AutoScalingMessage response) {
			final DescribeLaunchConfigurationsResponseType resp = (DescribeLaunchConfigurationsResponseType) response;
			try{
				return resp.getDescribeLaunchConfigurationsResult().getLaunchConfigurations().getMember().get(0);
			}catch(final Exception ex){
				LOG.error("Launch configuration is not found from the response");
				return null;
			}
		}
	}

	private class AutoScalingCreateLaunchConfigTask extends EucalyptusActivityTask<AutoScalingMessage, AutoScaling>{
		private final String imageId;
		private final String instanceType;
		private final String instanceProfileName;
		private final String launchConfigName;
		private final Collection<String> securityGroupNamesOrIds;
		private final String keyName;
		private final String userData;
		private final Boolean associatePublicIp;
		private AutoScalingCreateLaunchConfigTask(final String imageId, final String instanceType, String instanceProfileName,
				final String launchConfigName, final Collection<String> securityGroupNamesOrIds, final String keyName, final String userData,
				final Boolean associatePublicIp){
			this.imageId = imageId;
			this.instanceType = instanceType;
			this.instanceProfileName = instanceProfileName;
			this.launchConfigName = launchConfigName;
			this.securityGroupNamesOrIds = securityGroupNamesOrIds;
			this.keyName = keyName;
			this.userData = userData;
			this.associatePublicIp = associatePublicIp;
		}

		CreateLaunchConfigurationType getRequest(){
			final CreateLaunchConfigurationType req = new CreateLaunchConfigurationType();
			req.setImageId(this.imageId);
			req.setInstanceType(this.instanceType);
			req.setIamInstanceProfile(this.instanceProfileName);
			if(this.keyName == null || this.keyName.length()<=0)
			  req.setKeyName(null);
			else
			  req.setKeyName(this.keyName);
			req.setLaunchConfigurationName(this.launchConfigName);
			req.setSecurityGroups( new SecurityGroups( this.securityGroupNamesOrIds ) );
			req.setUserData(userData);
			req.setAssociatePublicIpAddress( associatePublicIp );
			return req;
		}
	}

	private class CloudWatchPutMetricDataTask extends EucalyptusActivityTask<CloudWatchMessage, CloudWatch>{
		private MetricData metricData = null;
		private String namespace = null;
		private CloudWatchPutMetricDataTask( final String namespace, final MetricData data){
			this.namespace = namespace;
			this.metricData = data;
		}

		PutMetricDataType getRequest(){
			final PutMetricDataType request = new PutMetricDataType();
			request.setNamespace(this.namespace);
			request.setMetricData(this.metricData);
			return request;
		}
	}


	private class EucalyptusDescribeAvailabilityZonesTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<ClusterInfoType>> {
		private boolean verbose = false;
		private EucalyptusDescribeAvailabilityZonesTask(boolean verbose){
			this.verbose = verbose;
		}

		DescribeAvailabilityZonesType getRequest(){
			final DescribeAvailabilityZonesType req = new DescribeAvailabilityZonesType();
			if(this.verbose){
				req.setAvailabilityZoneSet(Lists.newArrayList("verbose"));
			}
			return req;
		}

		@Override
		List<ClusterInfoType> extractResult(ComputeMessage response) {
			final DescribeAvailabilityZonesResponseType resp = (DescribeAvailabilityZonesResponseType) response;
			return resp.getAvailabilityZoneInfo();
		}
	}

	private class EucalyptusDescribeServicesTask extends EucalyptusActivityTaskWithResult<EmpyreanMessage, Empyrean,List<ServiceStatusType>> {
		private String componentType = null;
		private EucalyptusDescribeServicesTask(final String componentType){
			this.componentType = componentType;
		}

		DescribeServicesType getRequest(){
			final DescribeServicesType req = new DescribeServicesType();
		    req.setByServiceType(this.componentType);
		    return req;
		}

		@Override
		List<ServiceStatusType> extractResult(EmpyreanMessage response) {
			final DescribeServicesResponseType resp = (DescribeServicesResponseType) response;
			return resp.getServiceStatuses();
		}
	}

	private class EucalyptusDescribeInstanceTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute,List<RunningInstancesItemType>> {
		private final List<String> instanceIds;
		private boolean verbose = false;
		private EucalyptusDescribeInstanceTask(final List<String> instanceId){
			this.instanceIds = instanceId;
		}

		private EucalyptusDescribeInstanceTask(final List<String> instanceId, final boolean verbose){
      this.instanceIds = instanceId;
      this.verbose = verbose;
    }

		DescribeInstancesType getRequest(){
			final DescribeInstancesType req = new DescribeInstancesType();
			if( this.verbose ) {
				req.setInstancesSet( Lists.newArrayList( "verbose" ) );
			}
			req.getFilterSet( ).add( filter( "instance-id", this.instanceIds ) );
			return req;
		}

		@Override
		List<RunningInstancesItemType> extractResult( ComputeMessage response) {
			final DescribeInstancesResponseType resp = (DescribeInstancesResponseType) response;
			final List<RunningInstancesItemType> resultInstances = Lists.newArrayList();
			for(final ReservationInfoType res : resp.getReservationSet()){
				resultInstances.addAll(res.getInstancesSet());
			}
			return resultInstances;
		}

		@Override
		List<RunningInstancesItemType> getFailureResult( final String errorCode ) {
			return "InvalidInstanceID.NotFound".equals( errorCode ) ?
					Lists.<RunningInstancesItemType>newArrayList( ) :
					null;
		}
	}

	//SPARK: TODO: SYSTEM, STATIC MODE?
	private class EucalyptusCreateGroupTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, String> {
		private String groupName = null;
		private String groupDesc = null;
		EucalyptusCreateGroupTask(String groupName, String groupDesc){
			this.groupName = groupName;
			this.groupDesc = groupDesc;
		}
		CreateSecurityGroupType getRequest(){
			final CreateSecurityGroupType req = new CreateSecurityGroupType();
			req.setGroupName(this.groupName);
			req.setGroupDescription(this.groupDesc);
			return req;
		}

		@Override
		String extractResult(ComputeMessage response) {
			final CreateSecurityGroupResponseType resp = (CreateSecurityGroupResponseType) response;
			return resp.getGroupId();
		}
	}

	private class EucalyptusAuthorizeIngressRuleTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		String groupNameOrId=null;
		String protocol = null;
		int portNum = 1;

		EucalyptusAuthorizeIngressRuleTask(String groupNameOrId, String protocol, int portNum){
			this.groupNameOrId = groupNameOrId;
			this.protocol=protocol;
			this.portNum = portNum;
		}
		AuthorizeSecurityGroupIngressType getRequest(){
			AuthorizeSecurityGroupIngressType req = new AuthorizeSecurityGroupIngressType( );
			if ( this.groupNameOrId.matches( "sg-[0-9a-fA-F]{8}" ) ) {
				req.setGroupId( this.groupNameOrId );
			} else {
				req.setGroupName( this.groupNameOrId );
			}
			IpPermissionType perm = new IpPermissionType();
			perm.setFromPort(this.portNum);
			perm.setToPort(this.portNum);
			perm.setCidrIpRanges( Collections.singleton( "0.0.0.0/0" ) );
			perm.setIpProtocol(this.protocol); // udp too?
			req.getIpPermissions( ).add( perm );
			return req;
		}
	}
	private class EucalyptusRevokeIngressRuleTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		String groupName=null;
		String protocol=null;
		int portNum = 1;
		EucalyptusRevokeIngressRuleTask(String groupName, String protocol, int portNum){
			this.groupName = groupName;
			this.protocol = protocol;
			this.portNum = portNum;
		}
		RevokeSecurityGroupIngressType getRequest(){
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
	}

	private class EucalyptusRevokeIngressRuleFromOtherGroupTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String groupId = null;
		private String protocol = null;
		private Integer fromPortNum = null;
		private Integer toPortNum = null;
		private String sourceGroupId = null;
		private String sourceAccountId = null;

		EucalyptusRevokeIngressRuleFromOtherGroupTask(final String groupId, final String protocol,
													  final Integer fromPortNum, final Integer toPortNum,
													  final String sourceAccountId, final String sourceGroupId) {
			this.groupId = groupId;
			this.protocol = protocol;
			this.fromPortNum = fromPortNum;
			this.toPortNum = toPortNum;
			this.sourceAccountId = sourceAccountId;
			this.sourceGroupId = sourceGroupId;
		}

		RevokeSecurityGroupIngressType getRequest() {
			final RevokeSecurityGroupIngressType req = new RevokeSecurityGroupIngressType();
			req.setGroupId(this.groupId);
			req.setIpPermissions(new ArrayList<IpPermissionType>());
			final IpPermissionType perm = new IpPermissionType();
			perm.setIpProtocol(this.protocol);
			if(this.sourceAccountId!=null && this.sourceGroupId!=null) {
				perm.setGroups(new ArrayList<UserIdGroupPairType>());
				final UserIdGroupPairType userGroup = new UserIdGroupPairType();
				userGroup.setSourceGroupId( this.sourceGroupId );
				userGroup.setSourceUserId( this.sourceAccountId );
				perm.getGroups().add( userGroup );
			}
			if(this.fromPortNum != null && this.toPortNum != null) {
				perm.setFromPort(this.fromPortNum);
				perm.setToPort(this.toPortNum);
			}
			req.getIpPermissions().add(perm);
			return req;
		}
	}

	private class EucalyptusAuthorizeEgressRuleTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String groupId = null;
		private String protocol = null;
		private int fromPort = -1;
		private int toPort = -1;
		private String cidrSourceRange = null;
		EucalyptusAuthorizeEgressRuleTask(final String groupId, final String protocol,
									   final int fromPort, final int toPort,
									   final String cidrSourceRange) {
			this.groupId = groupId;
			this.protocol = protocol;
			this.fromPort = fromPort;
			this.toPort = toPort;
			this.cidrSourceRange = cidrSourceRange;
		}

		AuthorizeSecurityGroupEgressType getRequest() {
			final AuthorizeSecurityGroupEgressType req = new AuthorizeSecurityGroupEgressType();
			req.setGroupId(this.groupId);
			final IpPermissionType perm = new IpPermissionType();
			perm.setIpProtocol(this.protocol);
			perm.setFromPort(this.fromPort);
			perm.setToPort(this.toPort);
			perm.setCidrIpRanges( Lists.newArrayList( Arrays.asList( this.cidrSourceRange ) ) );
			req.setIpPermissions(Lists.newArrayList(Arrays.asList(perm)));
			return req;
		}
	}

	private class EucalyptusRevokeEgressRuleTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private String groupId = null;
		private String protocol = null;
		private int fromPort = -1;
		private int toPort = -1;
		private String cidrSourceRange = null;
		EucalyptusRevokeEgressRuleTask(final String groupId, final String protocol,
									   final int fromPort, final int toPort,
									   final String cidrSourceRange) {
			this.groupId = groupId;
			this.protocol = protocol;
			this.fromPort = fromPort;
			this.toPort = toPort;
			this.cidrSourceRange = cidrSourceRange;
		}

		RevokeSecurityGroupEgressType getRequest() {
			final RevokeSecurityGroupEgressType req = new RevokeSecurityGroupEgressType();
			req.setGroupId(this.groupId);
			final IpPermissionType perm = new IpPermissionType();
			perm.setIpProtocol(this.protocol);
			perm.setFromPort(this.fromPort);
			perm.setToPort(this.toPort);
			perm.setCidrIpRanges( Lists.newArrayList( Arrays.asList( this.cidrSourceRange ) ) );
			req.setIpPermissions(Lists.newArrayList(Arrays.asList(perm)));
			return req;
		}
	}


	private class EucalyptusDeleteGroupTask extends EucalyptusActivityTask<ComputeMessage, Compute>{
		private String groupName = null;
		EucalyptusDeleteGroupTask(String groupName){
			this.groupName = groupName;
		}
		DeleteSecurityGroupType getRequest(){
			final DeleteSecurityGroupType req = new DeleteSecurityGroupType();
			req.setGroupName(this.groupName);
			return req;
		}

		@Override
		boolean dispatchFailure( final ActivityContext<ComputeMessage, Compute> context, final Throwable throwable ) {
			if ( AsyncExceptions.isWebServiceErrorCode( throwable, "InvalidGroup.InUse" ) ) {
				LOG.warn( "Could not delete in-use security group " + groupName );
				return false;
			} else {
				return super.dispatchFailure( context, throwable );
			}
		}
	}

	private class EucalyptusDescribeSecurityGroupTask extends EucalyptusActivityTaskWithResult<ComputeMessage, Compute, List<SecurityGroupItemType>>{
		@Nullable private List<String> groupIds = null;
		@Nullable private List<String> groupNames = null;
		@Nullable private String vpcId = null;

		EucalyptusDescribeSecurityGroupTask(
				@Nullable final List<String> groupIds,
				@Nullable final List<String> groupNames,
				@Nullable final String vpcId ){
			this.groupIds = groupIds;
			this.groupNames = groupNames;
			this.vpcId = vpcId;
		}

		DescribeSecurityGroupsType getRequest( ) {
			final DescribeSecurityGroupsType req = new DescribeSecurityGroupsType( );
			if ( groupIds != null && !groupIds.isEmpty( ) ) {
				req.getFilterSet().add( filter( "group-id", groupIds ) );
			}
			if ( groupNames != null && !groupNames.isEmpty( ) ) {
				req.getFilterSet().add( filter( "group-name", groupNames ) );
			}
			if ( vpcId != null ) {
				req.getFilterSet().add( filter( "vpc-id", vpcId ) );
			}
			return req;
		}

		@Override
		List<SecurityGroupItemType> extractResult(ComputeMessage response) {
			final DescribeSecurityGroupsResponseType resp = (DescribeSecurityGroupsResponseType) response;
			return resp.getSecurityGroupInfo();
		}
	}

	private class EucalyptusModifySecurityGroupsTask extends EucalyptusActivityTask<ComputeMessage, Compute> {
		private final String instanceId;
		private final Collection<String> securityGroupIds;

		EucalyptusModifySecurityGroupsTask(
				final String instanceId,
				final Collection<String> securityGroupIds
		) {
			this.instanceId = instanceId;
			this.securityGroupIds = securityGroupIds;
		}

		@Override
		ComputeMessage getRequest( ) {
			final ModifyInstanceAttributeType modifyInstanceAttribute = new ModifyInstanceAttributeType( );
			modifyInstanceAttribute.setInstanceId( instanceId );
			modifyInstanceAttribute.setGroupIdSet( new GroupIdSetType( ) );
			for ( final String securityGroupId : securityGroupIds ) {
				final SecurityGroupIdSetItemType id = new SecurityGroupIdSetItemType( );
				id.setGroupId( securityGroupId );
				modifyInstanceAttribute.getGroupIdSet().getItem().add( id );
			}
			return modifyInstanceAttribute;
		}
	}


	private static Filter filter( final String name, String value ) {
		return filter( name, Collections.singleton( value ) );
	}

	private static Filter filter( final String name, final Iterable<String> values ) {
		return Filter.filter( name, values );
	}

	private abstract class EucalyptusActivityTask <TM extends BaseMessage, TC extends ComponentId>{
		protected EucalyptusActivityTask(){}

		final CheckedListenableFuture<Boolean> dispatch( final ActivityContext<TM,TC> context ) {
			try {
				final CheckedListenableFuture<Boolean> future = Futures.newGenericeFuture();
				dispatchInternal( context, new Callback.Checked<TM>(){
					@Override
					public void fireException( final Throwable throwable ) {
						boolean result = false;
						try {
							result = dispatchFailure( context, throwable );
						} finally {
							future.set( result );
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
				return future;
			} catch ( Exception e ) {
				LOG.error( e, e );
			}
			return Futures.predestinedFuture( false );
		}

		/**
		 * Build the request message
		 */
		abstract TM getRequest( );

		final void dispatchInternal( final ActivityContext<TM,TC> context, final Callback.Checked<TM> callback) {
			final DispatchingClient<TM, TC> client = context.getClient( );
			client.dispatch( getRequest( ), callback );
		}

		boolean dispatchFailure( ActivityContext<TM,TC> context, Throwable throwable ) {
			LOG.error( "Loadbalancer activity error", throwable );
			return false;
		}

		void dispatchSuccess( ActivityContext<TM,TC> context, TM response ){ }
	}

	private abstract class EucalyptusActivityTaskWithResult<TM extends BaseMessage, TC extends ComponentId, R>
			extends EucalyptusActivityTask<TM,TC> {
		private final AtomicReference<R> r = new AtomicReference<>( );

		/**
		 * Extract/construct the result from the response message
		 */
		abstract R extractResult( TM response );

		R failureResult( String errorCode ) { return null; }

		final R getResult( ) {
			return r.get( );
		}

		R getFailureResult( final String errorCode ) {
			return null;
		}

		@Override
		void dispatchSuccess( final ActivityContext<TM,TC> context, final TM response) {
			r.set( extractResult( response ) );
		}

		@Override
		boolean dispatchFailure( final ActivityContext<TM, TC> context, final Throwable throwable ) {
			final Optional<AsyncWebServiceError> serviceErrorOptional = AsyncExceptions.asWebServiceError( throwable );
			if ( serviceErrorOptional.isPresent( ) ){
				final R result = getFailureResult( serviceErrorOptional.get( ).getCode( ) );
				if ( result != null ) {
					r.set( result );
					return true;
				} else {
					return super.dispatchFailure( context, throwable );
				}
			} else {
				return super.dispatchFailure( context, throwable );
			}
		}
	}

	private <TM extends BaseMessage, TC extends ComponentId> void checkResult(
			final EucalyptusActivityTask<TM,TC> task,
			final ActivityContext<TM,TC> context,
			final String errorMessage
	) {
		final CheckedListenableFuture<Boolean> result = task.dispatch( context );
		try{
			if ( !result.get( ) ) {
				throw new EucalyptusActivityException( errorMessage );
			}
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}

	private <TM extends BaseMessage, TC extends ComponentId, R> R resultOf(
			final EucalyptusActivityTaskWithResult<TM,TC,R> task,
			final ActivityContext<TM,TC> context,
			final String errorMessage
	) {
		final CheckedListenableFuture<Boolean> result = task.dispatch( context );
		try{
			if (result.get() ){
				return task.getResult();
			}else
				throw new EucalyptusActivityException( errorMessage );
		}catch(Exception ex){
			throw Exceptions.toUndeclared(ex);
		}
	}
}

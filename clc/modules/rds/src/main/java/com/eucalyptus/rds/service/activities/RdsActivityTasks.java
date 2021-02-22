/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.activities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
import com.eucalyptus.cloudformation.common.CloudFormation;
import com.eucalyptus.cloudformation.common.msgs.Capabilities;
import com.eucalyptus.cloudformation.common.msgs.CloudFormationMessage;
import com.eucalyptus.cloudformation.common.msgs.CreateStackResponseType;
import com.eucalyptus.cloudformation.common.msgs.CreateStackType;
import com.eucalyptus.cloudformation.common.msgs.DeleteStackType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksResponseType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksResult;
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksType;
import com.eucalyptus.cloudformation.common.msgs.Parameter;
import com.eucalyptus.cloudformation.common.msgs.Parameters;
import com.eucalyptus.cloudformation.common.msgs.Stack;
import com.eucalyptus.cloudformation.common.msgs.Stacks;
import com.eucalyptus.cloudformation.common.msgs.Tag;
import com.eucalyptus.cloudformation.common.msgs.Tags;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.AllocateAddressResponseType;
import com.eucalyptus.compute.common.AllocateAddressType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.AssociateRouteTableType;
import com.eucalyptus.compute.common.AttachInternetGatewayType;
import com.eucalyptus.compute.common.AttachNetworkInterfaceType;
import com.eucalyptus.compute.common.AuthorizeSecurityGroupEgressType;
import com.eucalyptus.compute.common.CloudFilters;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ComputeMessage;
import com.eucalyptus.compute.common.CreateInternetGatewayResponseType;
import com.eucalyptus.compute.common.CreateInternetGatewayType;
import com.eucalyptus.compute.common.CreateNatGatewayResponseType;
import com.eucalyptus.compute.common.CreateNatGatewayType;
import com.eucalyptus.compute.common.CreateNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.CreateNetworkInterfaceType;
import com.eucalyptus.compute.common.CreateRouteTableResponseType;
import com.eucalyptus.compute.common.CreateRouteTableType;
import com.eucalyptus.compute.common.CreateRouteType;
import com.eucalyptus.compute.common.CreateSubnetResponseType;
import com.eucalyptus.compute.common.CreateSubnetType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.CreateVolumeResponseType;
import com.eucalyptus.compute.common.CreateVolumeType;
import com.eucalyptus.compute.common.CreateVpcResponseType;
import com.eucalyptus.compute.common.CreateVpcType;
import com.eucalyptus.compute.common.DeleteNetworkInterfaceType;
import com.eucalyptus.compute.common.DeleteResourceTag;
import com.eucalyptus.compute.common.DeleteRouteType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DeleteVolumeType;
import com.eucalyptus.compute.common.DescribeAddressesResponseType;
import com.eucalyptus.compute.common.DescribeAddressesType;
import com.eucalyptus.compute.common.DescribeImagesResponseType;
import com.eucalyptus.compute.common.DescribeImagesType;
import com.eucalyptus.compute.common.DescribeInstanceTypesResponseType;
import com.eucalyptus.compute.common.DescribeInstanceTypesType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeInternetGatewaysType;
import com.eucalyptus.compute.common.DescribeNatGatewaysResponseType;
import com.eucalyptus.compute.common.DescribeNatGatewaysType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesResponseType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesType;
import com.eucalyptus.compute.common.DescribeRouteTablesResponseType;
import com.eucalyptus.compute.common.DescribeRouteTablesType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsResponseType;
import com.eucalyptus.compute.common.DescribeSecurityGroupsType;
import com.eucalyptus.compute.common.DescribeSubnetsResponseType;
import com.eucalyptus.compute.common.DescribeSubnetsType;
import com.eucalyptus.compute.common.DescribeVolumesResponseType;
import com.eucalyptus.compute.common.DescribeVolumesType;
import com.eucalyptus.compute.common.DescribeVpcsResponseType;
import com.eucalyptus.compute.common.DescribeVpcsType;
import com.eucalyptus.compute.common.DisassociateAddressType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.GroupIdSetType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.InternetGatewayIdSetItemType;
import com.eucalyptus.compute.common.InternetGatewayIdSetType;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.compute.common.IpPermissionType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttachmentType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeType;
import com.eucalyptus.compute.common.NatGatewayType;
import com.eucalyptus.compute.common.NetworkInterfaceType;
import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.ResourceTagSpecification;
import com.eucalyptus.compute.common.RevokeSecurityGroupEgressType;
import com.eucalyptus.compute.common.RouteTableType;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.SubnetIdSetItemType;
import com.eucalyptus.compute.common.SubnetIdSetType;
import com.eucalyptus.compute.common.SubnetType;
import com.eucalyptus.compute.common.TerminateInstancesType;
import com.eucalyptus.compute.common.UserIdGroupPairType;
import com.eucalyptus.compute.common.VmTypeDetails;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.compute.common.VpcIdSetItemType;
import com.eucalyptus.compute.common.VpcIdSetType;
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
import com.eucalyptus.rds.service.RdsSystemAccountProvider;
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
import io.vavr.control.Option;

/**
 *
 */
@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType", "UnnecessaryLocalVariable", "Guava"})
public class RdsActivityTasks {
  private static final Logger LOG = Logger.getLogger( RdsActivityTask.class );
  private RdsActivityTasks() {}
  private static RdsActivityTasks _instance = new RdsActivityTasks();
  public static RdsActivityTasks getInstance(){
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
                new DispatchingClient<>( this.getUserId( ), this.componentIdClass ) :
                new DispatchingClient<>( this.getAccount(), this.componentIdClass );
        client.init();
        return client;
      }catch(Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }

  private abstract class SystemActivityContextSupport<TM extends BaseMessage, TC extends ComponentId> extends ActivityContextSupport<TM, TC>{
    boolean useServiceAccount = true;
    private SystemActivityContextSupport( final Class<TC> componentIdClass ) {
      super( componentIdClass );
    }

    private SystemActivityContextSupport( final Class<TC> componentIdClass, final boolean useServiceAccount) {
      super( componentIdClass );
      this.useServiceAccount = useServiceAccount;
    }

    @Override
    final String getUserId() {
      return null;
    }

    @Override
    AccountFullName getAccount() {
      try{
        return AccountFullName.getInstance( Accounts.lookupAccountIdByAlias( this.useServiceAccount ?
            RdsSystemAccountProvider.RDS_SYSTEM_ACCOUNT :
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
    private EuareSystemActivity( final boolean useServiceAccount ){
      super( Euare.class, useServiceAccount );
    }
  }

  private class EuareUserActivity extends UserActivityContextSupport<EuareMessage, Euare> {
    private EuareUserActivity(final AccountFullName accountFullName){
      super( Euare.class, accountFullName );
    }
  }

  private class EmpyreanSystemActivity extends SystemActivityContextSupport<EmpyreanMessage, Empyrean>{
    private EmpyreanSystemActivity() { super( Empyrean.class, false ); }
  }

  private class CloudFormationSystemActivity extends SystemActivityContextSupport<CloudFormationMessage, CloudFormation>{
    private CloudFormationSystemActivity() { super( CloudFormation.class ); }
  }

  private class ComputeSystemActivity extends SystemActivityContextSupport<ComputeMessage, Compute>{
    private ComputeSystemActivity() { super( Compute.class ); }
    private ComputeSystemActivity(final boolean useServiceAccount) { super( Compute.class , useServiceAccount); }
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

  public List<RunningInstancesItemType> describeSystemInstances(final List<String> instances, final boolean useServiceAccount){
    return describeSystemInstancesImpl(instances, useServiceAccount);
  }
  private List<RunningInstancesItemType> describeSystemInstancesImpl(final List<String> instances, final boolean useServiceAccount){
    if(instances.size() <=0)
      return Lists.newArrayList();
    final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances);
    return resultOf( describeTask, new ComputeSystemActivity(useServiceAccount), "failed to describe the instances" );
  }

  public List<RunningInstancesItemType> describeUserInstances(final String accountNumber, final List<String> instances){
    if(instances.size() <=0)
      return Lists.newArrayList();
    final EucalyptusDescribeInstanceTask describeTask = new EucalyptusDescribeInstanceTask(instances);
    return resultOf( describeTask, new ComputeUserActivity( AccountFullName.getInstance( accountNumber )), "failed to describe the instances" );
  }

  public List<ServiceStatusType> describeServices(final String componentType){
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
  public List<ClusterInfoType> describeAvailabilityZones(final boolean useServiceAccount) {
    return describeAvailabilityZonesImpl(useServiceAccount);
  }
  private List<ClusterInfoType> describeAvailabilityZonesImpl(final boolean useServiceAccount){
    return resultOf(
        new EucalyptusDescribeAvailabilityZonesTask(false),
        new ComputeSystemActivity(useServiceAccount),
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
  public void deleteSystemSecurityGroup( String groupName , boolean useServiceAccount){
    deleteSystemSecurityGroupImpl(groupName, useServiceAccount);
  }
  private void deleteSystemSecurityGroupImpl( String groupName, boolean useServiceAccount ){
    final EucalyptusDeleteGroupTask task = new EucalyptusDeleteGroupTask(groupName);
    checkResult( task, new ComputeSystemActivity(useServiceAccount), "failed to delete the group "+groupName );
  }
  public List<SecurityGroupItemType> describeSystemSecurityGroups(List<String> groupNames, boolean useServiceAccount ){
    return describeSystemSecurityGroupsImpl(groupNames, null, useServiceAccount);
  }
  public List<SecurityGroupItemType> describeSystemSecurityGroups( List<String> groupNames ){
    return describeSystemSecurityGroupsImpl(groupNames, null, true);
  }

  public List<SecurityGroupItemType> describeSystemSecurityGroupsByVpc(final String vpcId) {
    return describeSystemSecurityGroupsImpl(null, vpcId, true);
  }

  private List<SecurityGroupItemType> describeSystemSecurityGroupsImpl( final List<String> groupNames, final String vpcId, boolean useServiceAccount ){
    final EucalyptusDescribeSecurityGroupTask task = new EucalyptusDescribeSecurityGroupTask(null, groupNames, vpcId);
    return resultOf( task, new ComputeSystemActivity(useServiceAccount), "failed to describe security groups" );
  }

  public void authorizeSystemSecurityGroup( String groupNameOrId, String protocol, int portNum ){
    this.authorizeSystemSecurityGroupImpl( groupNameOrId, protocol, portNum,  new ComputeSystemActivity());
  }

  public void authorizeSystemSecurityGroup( String groupNameOrId, String protocol, int portNum, boolean useServiceAccount ){
    this.authorizeSystemSecurityGroupImpl( groupNameOrId, protocol, portNum,  new ComputeSystemActivity(useServiceAccount));
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

  public void revokeSystemSecurityGroup( String groupName, String protocol, int portNum, boolean useServiceAccount ){
    revokeSystemSecurityGroupImpl(groupName, protocol, portNum, useServiceAccount);
  }

  private void revokeSystemSecurityGroupImpl( String groupName, String protocol, int portNum, boolean useServiceAccount ){
    final EucalyptusRevokeIngressRuleTask task = new EucalyptusRevokeIngressRuleTask(groupName, protocol, portNum);
    checkResult(
        task,
        new ComputeSystemActivity(useServiceAccount),
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
      final boolean useServiceAccount
  ){
    modifySecurityGroupsImpl(instanceId, securityGroupIds, useServiceAccount);
  }
  private void modifySecurityGroupsImpl(
      final String instanceId,
      final Collection<String> securityGroupIds,
      final boolean useServiceAccount
  ) {
    checkResult(
        new EucalyptusModifySecurityGroupsTask( instanceId, securityGroupIds ),
        new ComputeSystemActivity( useServiceAccount ),
        "failed to modify security groups"
    );
  }

  public Optional<VpcType> defaultVpc(final AccountFullName accountFullName ) {
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

  public String createSystemStack(
      final String stackName,
      final String template,
      final Map<String,String> parameters,
      final Map<String,String> tags
  ) {
    return resultOf(
        new CloudFormationCreateStackTask(stackName, template, parameters, tags),
        new CloudFormationSystemActivity(),
        "failed to create stack"
    );
  }

  public List<Stack> describeSystemStacks(final String stackName) {
    return resultOf(
        new CloudFormationDescribeStacksTask(stackName),
        new CloudFormationSystemActivity(),
        "failed to describe stacks"
    );
  }

  public void deleteSystemStack(final String stackName) {
    checkResult(
        new CloudFormationDeleteStackTask(stackName),
        new CloudFormationSystemActivity(),
        "failed to delete stack"
    );
  }

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
  public void deleteRole(final String roleName, boolean useServiceAccount){
    deleteRoleImpl(roleName, useServiceAccount);
  }
  private void deleteRoleImpl(final String roleName, boolean useServiceAccount){
    checkResult(
        new EuareDeleteRoleTask(roleName),
        new EuareSystemActivity(useServiceAccount),
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
  public void deleteInstanceProfile(String profileName, boolean useServiceAccount){
    deleteInstanceProfileImpl(profileName, useServiceAccount);
  }
  private void deleteInstanceProfileImpl(String profileName, boolean useServiceAccount){
    checkResult(
        new EuareDeleteInstanceProfileTask(profileName),
        new EuareSystemActivity( useServiceAccount ),
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
  public void removeRoleFromInstanceProfile(String instanceProfileName, String roleName, boolean useServiceAccount){
    removeRoleFromInstanceProfileImpl(instanceProfileName, roleName, useServiceAccount);
  }
  private void removeRoleFromInstanceProfileImpl(String instanceProfileName, String roleName, boolean useServiceAccount){
    checkResult(
        new EuareRemoveRoleFromInstanceProfileTask(instanceProfileName, roleName),
        new EuareSystemActivity( useServiceAccount ),
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

  public void putRolePolicy(String roleName, String policyName, String policyDocument, boolean useServiceAccount){
    putRolePolicyImpl(roleName, policyName, policyDocument, useServiceAccount);
  }

  private void putRolePolicyImpl(String roleName, String policyName, String policyDocument, boolean useServiceAccount){
    checkResult(
        new EuarePutRolePolicyTask(roleName, policyName, policyDocument),
        new EuareSystemActivity( useServiceAccount ),
        "failed to put role's policy"
    );
  }

  public void deleteRolePolicy(String roleName, String policyName){
    deleteRolePolicyImpl(roleName, policyName, true);
  }
  public void deleteRolePolicy(String roleName, String policyName, boolean useServiceAccount){
    deleteRolePolicyImpl(roleName, policyName, useServiceAccount);
  }
  private void deleteRolePolicyImpl(String roleName, String policyName, boolean useServiceAccount){
    checkResult(
        new EuareDeleteRolePolicyTask(roleName, policyName),
        new EuareSystemActivity( useServiceAccount ),
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

  public List<Volume> describeSystemVolumes(final String volumeId, final String status, final Map<String,String> tags) {
    return resultOf(
        new EucaDescribeVolumesTask(volumeId, status, tags),
        new ComputeSystemActivity(),
        "failed to describe volumes"
    );
  }

  public Volume createSystemVolume(final String availabilityZone, final int size, final Map<String,String> tags) {
    return resultOf(
        new EucaCreateVolumeTask(availabilityZone, size, tags),
        new ComputeSystemActivity(),
        "failed to create volume"
    );
  }

  public boolean deleteSystemVolume(final String volumeId) {
    return resultOf(
        new EucaDeleteVolumeTask(volumeId),
        new ComputeSystemActivity(),
        "failed to delete volume"
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

  public List<NetworkInterfaceType> describeNetworkInterfaces(final AccountFullName accountFullName, final List<String> networkInterfaceIds) {
    return resultOf(
        new EucaDescribeNetworkInterfacesTask(networkInterfaceIds, null),
        new ComputeUserActivity(accountFullName),
        "failed to describe network interfaces"
    );
  }

  public NetworkInterfaceType createNetworkInterface(final AccountFullName accountFullName, final String subnetId, final List<String> securityGroupIds) {
    return resultOf(
        new EucaCreateNetworkInterfaceTask(subnetId, securityGroupIds),
        new ComputeUserActivity(accountFullName),
        "failed to create network interface"
    );
  }

  public void deleteNetworkInterface(final AccountFullName accountFullName, final String networkInterfaceId){
    checkResult(
        new EucaDeleteNetworkInterfaceTask(networkInterfaceId),
        new ComputeUserActivity(accountFullName),
        "failed to create network interface"
    );
  }

  public void attachNetworkInterface(final AccountFullName accountFullName, final String instanceId, final String networkInterfaceId, final int deviceIndex) {
    checkResult(new EucaAttachNetworkInterfaceTask(instanceId, networkInterfaceId, deviceIndex),
        new ComputeUserActivity(accountFullName),
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

  private class EucaDescribeImagesTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<ImageDetails>> {
    private List<String> imageIds;
    private EucaDescribeImagesTask(final List<String> imageIds){
      this.imageIds = imageIds;
    }

    DescribeImagesType getRequest(){
      final DescribeImagesType req = new DescribeImagesType();
      if(this.imageIds!=null && this.imageIds.size()>0){
        req.setFilterSet( Lists.newArrayList( CloudFilters.filter( "image-id", this.imageIds ) ) );
      }
      return req;
    }

    @Override
    List<ImageDetails> extractResult(ComputeMessage response) {
      final DescribeImagesResponseType resp = (DescribeImagesResponseType) response;
      return resp.getImagesSet();
    }
  }

  private class EucaDescribeInstanceTypesTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<VmTypeDetails>> {
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

  private class EucaTerminateInstancesTask extends RdsActivityTask<ComputeMessage, Compute> {
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

  private class EucaDeleteTagsTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String tagKey;
    private String tagValue;
    private List<String> resources;

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

  private class EucaCreateTagsTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String tagKey;
    private String tagValue;
    private List<String> resources;
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

  private class EucaDescribeSecurityGroupsTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<SecurityGroupItemType>> {
    private String vpcId;
    private Collection<String> securityGroupIds;
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

  private class EucaCreateVpcTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, String> {
    private String cidr;
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

  private class EucaCreateInternetGatewayTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, String> {
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

  private class EucaAttachInternetGatewayTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String vpcId;
    private String gatewayId;
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

  private class EucaCreateSubnetTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, String> {
    private String vpcId;
    private String availabilityZone;
    private String cidr;

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

  private class EucaCreateRouteTableTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, String> {
    private String vpcId;
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

  private class EucaDescribeRouteTableTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<RouteTableType>> {
    private String vpcId = null;
    private String routeTableId;
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
  private class EucaAssociateRouteTableTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String subnetId;
    private String routeTableId;

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

  private class EucaAssociateAddressTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String allocationId;
    private String networkInterfaceId;

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

  private class EucaDisassociateAddressTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String publicIp;

    private EucaDisassociateAddressTask(final String publicIp) {
      this.publicIp = publicIp;
    }

    ComputeMessage getRequest() {
      final DisassociateAddressType req = new DisassociateAddressType();
      req.setPublicIp(this.publicIp);
      return req;
    }
  }

  private class EucaDescribeAddressesTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<AddressInfoType>> {
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
  private class EucaAllocateAddressTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, AllocateAddressResponseType> {
    private boolean isVpc;

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

  private class EucaAttachNetworkInterfaceTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String instanceId;
    private String interfaceId;
    private int deviceIdx;
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

  private class EucaDescribeVolumesTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<Volume>> {
    private String volumeId;
    private String status;
    private Map<String,String> tags;

    public EucaDescribeVolumesTask(final String volumeId, final String status, final Map<String,String> tags) {
      this.volumeId = volumeId;
      this.status = status;
      this.tags = tags;
    }

    @Override
    ComputeMessage getRequest() {
      final DescribeVolumesType req = new DescribeVolumesType();
      if ( volumeId != null ) {
        req.getFilterSet().add( filter( "volume-id", volumeId ) );
      }
      if ( status != null ) {
        req.getFilterSet().add( filter( "status", status ) );
      }
      if ( tags != null && !tags.isEmpty() ) {
        req.getFilterSet().addAll(tags.entrySet().stream().map(entry -> filter("tag:"+entry.getKey(), entry.getValue())).collect(Collectors.toList()));
      }
      return req;
    }

    @Override
    List<Volume> extractResult(ComputeMessage resp) {
      final DescribeVolumesResponseType response = (DescribeVolumesResponseType) resp;
      return response.getVolumeSet();
    }
  }

  private class EucaCreateVolumeTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, Volume> {
    private String availabilityZone;
    private int size;
    private Map<String,String> tags;

    public EucaCreateVolumeTask(final String availabilityZone, final int size, final Map<String,String> tags) {
      this.availabilityZone = availabilityZone;
      this.size = size;
      this.tags = tags;
    }

    @Override
    ComputeMessage getRequest() {
      final CreateVolumeType req = new CreateVolumeType();
      req.setAvailabilityZone(availabilityZone);
      req.setSize(String.valueOf(size));
      if (tags !=null && !tags.isEmpty()) {
        final ResourceTagSpecification tagSpecification = new ResourceTagSpecification();
        tagSpecification.setResourceType("volume");
        tagSpecification.getTagSet().addAll(tags.entrySet().stream().map(
            entry -> new ResourceTag(entry.getKey(), entry.getValue())
        ).collect(Collectors.toList()));
        req.setTagSpecification(Lists.newArrayList(tagSpecification));
      }
      return req;
    }

    @Override
    Volume extractResult(ComputeMessage resp) {
      final CreateVolumeResponseType response = (CreateVolumeResponseType) resp;
      return response.getVolume();
    }
  }

  private class EucaDeleteVolumeTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, Boolean> {
    private String volumeId;
    private Boolean deleted = Boolean.FALSE;

    public EucaDeleteVolumeTask(final String volumeId) {
      this.volumeId = volumeId;
    }

    @Override
    ComputeMessage getRequest() {
      final DeleteVolumeType req = new DeleteVolumeType();
      req.setVolumeId(volumeId);
      return req;
    }

    @Override
    Boolean extractResult(final ComputeMessage response) {
      return Boolean.FALSE;
    }

    Boolean getFailureResult( final String errorCode ) {
      return "InvalidVolume.NotFound".equals(errorCode) ? Boolean.TRUE : null;
    }
  }

  private class EucaCreateNetworkInterfaceTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, NetworkInterfaceType> {
    private String subnetId;
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
        groupIds.setItem(
            this.securityGroupIds.stream()
                .map(id -> {
                  final SecurityGroupIdSetItemType item = new SecurityGroupIdSetItemType();
                  item.setGroupId(id);
                  return item;
                })
                .collect(Collectors.toCollection(ArrayList::new)));
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

  private class EucaDeleteNetworkInterfaceTask extends RdsActivityTask<ComputeMessage, Compute> {
    private final String networkInterfaceId;

    public EucaDeleteNetworkInterfaceTask(final String networkInterfaceId) {
      this.networkInterfaceId = networkInterfaceId;
    }

    @Override
    ComputeMessage getRequest() {
      final DeleteNetworkInterfaceType req = new DeleteNetworkInterfaceType();
      req.setNetworkInterfaceId(networkInterfaceId);
      return req;
    }
  }

  private class EucaDescribeNetworkInterfacesTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<NetworkInterfaceType>> {
    private List<String> interfaceIds;
    private String subnetId;

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

  private class EucaModifyNetworkInterfaceAttribute extends RdsActivityTask<ComputeMessage, Compute> {
    private String networkInterfaceId;
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
        groupIds.setItem(
            this.securityGroupIds.stream()
                .map(id -> {
                  final SecurityGroupIdSetItemType item = new SecurityGroupIdSetItemType();
                  item.setGroupId(id);
                  return item;
                })
                .collect(Collectors.toCollection(ArrayList::new)));
        req.setGroupSet(groupIds);
      }
      if(this.attachmentId!=null && this.deleteOnTerminate.isPresent()) {
        final ModifyNetworkInterfaceAttachmentType attachment = new ModifyNetworkInterfaceAttachmentType();
        attachment.setAttachmentId(this.attachmentId);
        attachment.setDeleteOnTermination(this.deleteOnTerminate.or(Boolean.FALSE));
        req.setAttachment(attachment);
      }
      return req;
    }
  }

  private class EucaDescribeNatGatewayTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<NatGatewayType>> {
    private String subnetId;
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
  private class EucaCreateNatGatewayTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, String> {
    private String subnetId;
    private String elasticIpAllocationId;

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

  private class EucaDeleteRouteTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String routeTableId;
    private String destCidr;

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

  private class EucaCreateRouteTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String destCidr;
    private String internetGateway;
    private String natGateway;
    private String routeTable;

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

  private class EucaDescribeVpcsTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<VpcType>> {
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
        final ArrayList<VpcIdSetItemType> idItems =
            this.vpcIds.stream().map(s -> {
              final VpcIdSetItemType item = new VpcIdSetItemType();
              item.setVpcId(s);
              return item;
            }).collect(Collectors.toCollection(ArrayList::new));
        req.setVpcSet( new VpcIdSetType() );
        req.getVpcSet().setItem(idItems);
      }
      return req;
    }

    @Override
    List<VpcType> extractResult( final ComputeMessage response ) {
      final DescribeVpcsResponseType resp = (DescribeVpcsResponseType) response;
      return resp.getVpcSet( ).getItem();
    }
  }

  private class EucaDescribeSubnetsTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<SubnetType>> {
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

  private class EucaDescribeInternetGatewaysTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<InternetGatewayType>> {
    private Collection<String> vpcIds;
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

  private class CloudFormationDescribeStacksTask extends RdsActivityTaskWithResult<CloudFormationMessage, CloudFormation, List<Stack>> {
    private final String stack;

    private CloudFormationDescribeStacksTask(final String stack){
      this.stack = stack;
    }

    CloudFormationMessage getRequest( ){
      final DescribeStacksType req = new DescribeStacksType();
      req.setStackName(stack);
      return req;
    }

    @Override
    List<Stack> extractResult( final CloudFormationMessage response ) {
      final DescribeStacksResponseType resp = (DescribeStacksResponseType) response;
      return Option.of(resp.getDescribeStacksResult( ))
          .map(DescribeStacksResult::getStacks)
          .map(Stacks::getMember)
          .getOrElse(ArrayList::new);
    }
  }

  private class CloudFormationCreateStackTask extends RdsActivityTaskWithResult<CloudFormationMessage, CloudFormation, String> {
    private final String stack;
    private final String template;
    private final Map<String,String> parameters;
    private final Map<String,String> tags;

    private CloudFormationCreateStackTask(
        final String stack,
        final String template,
        final Map<String,String> parameters,
        final Map<String,String> tags
    ){
      this.stack = stack;
      this.template = template;
      this.parameters = parameters;
      this.tags = tags;
    }

    CloudFormationMessage getRequest( ){
      final CreateStackType req = new CreateStackType();
      req.setDisableRollback(true);
      req.setStackName(stack);
      req.setTemplateBody(template);

      final Capabilities capabilities = new Capabilities();
      capabilities.getMember().add("CAPABILITY_IAM");
      req.setCapabilities(capabilities);

      if ( parameters != null && !parameters.isEmpty() ) {
        final Parameters stackParameters = new Parameters();
        stackParameters.getMember().addAll(parameters.entrySet().stream()
            .map(entry -> new Parameter(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList()));
        req.setParameters(stackParameters);
      }

      if ( tags != null && !tags.isEmpty() ) {
        final Tags stackTags = new Tags();
        stackTags.getMember().addAll(tags.entrySet().stream().map(entry -> {
          Tag tag = new Tag();
          tag.setKey(entry.getKey());
          tag.setValue(entry.getValue());
          return tag;
        }).collect(Collectors.toList()));
        req.setTags(stackTags);
      }

      return req;
    }

    @Override
    String extractResult( final CloudFormationMessage response ) {
      final CreateStackResponseType resp = (CreateStackResponseType) response;
      return resp.getCreateStackResult().getStackId();
    }
  }

  private class CloudFormationDeleteStackTask extends RdsActivityTask<CloudFormationMessage, CloudFormation> {
    private final String stack;

    private CloudFormationDeleteStackTask(final String stack){
      this.stack = stack;
    }

    CloudFormationMessage getRequest( ){
      final DeleteStackType req = new DeleteStackType();
      req.setStackName(stack);
      return req;
    }
  }

  private class EuareGetServerCertificateTask extends RdsActivityTaskWithResult<EuareMessage, Euare, ServerCertificateType> {
    private String certName;

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

  private class EuareDeleteInstanceProfileTask extends RdsActivityTask<EuareMessage, Euare> {
    private String profileName;
    private EuareDeleteInstanceProfileTask(String profileName){
      this.profileName = profileName;
    }

    DeleteInstanceProfileType getRequest(){
      final DeleteInstanceProfileType req = new DeleteInstanceProfileType();
      req.setInstanceProfileName(this.profileName);
      return req;
    }
  }

  private class EuareAddRoleToInstanceProfileTask extends RdsActivityTask<EuareMessage, Euare> {
    private String instanceProfileName;
    private String roleName;

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

  private class EuareRemoveRoleFromInstanceProfileTask extends RdsActivityTask<EuareMessage, Euare> {
    private String instanceProfileName;
    private String roleName;

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

  private class EuareListInstanceProfilesTask extends RdsActivityTaskWithResult<EuareMessage, Euare, List<InstanceProfileType>> {
    private String pathPrefix;
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

  private class EuareCreateInstanceProfileTask extends RdsActivityTaskWithResult<EuareMessage, Euare, InstanceProfileType> {
    private String profileName;
    private String path;
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

  private class EuareDeleteRoleTask extends RdsActivityTask<EuareMessage, Euare> {
    private String roleName;
    private EuareDeleteRoleTask(String roleName){
      this.roleName = roleName;
    }
    DeleteRoleType getRequest(){
      final DeleteRoleType req = new DeleteRoleType();
      req.setRoleName(this.roleName);
      return req;
    }
  }

  private class EuareCreateRoleTask extends RdsActivityTaskWithResult<EuareMessage, Euare, RoleType> {
    String roleName;
    String path;
    String assumeRolePolicy;

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

  private class EuareListRolesTask extends RdsActivityTaskWithResult<EuareMessage, Euare, List<RoleType>> {
    private String pathPrefix;

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

  private class EuarePutRolePolicyTask extends RdsActivityTask<EuareMessage, Euare> {
    private String roleName;
    private String policyName;
    private String policyDocument;

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

  private class EuareListRolePoliciesTask extends RdsActivityTaskWithResult<EuareMessage, Euare, List<String>> {
    private String roleName;
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

  private class EuareGetRolePolicyTask extends RdsActivityTaskWithResult<EuareMessage, Euare, GetRolePolicyResult> {
    private String roleName;
    private String policyName;

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

  private class EuareDeleteRolePolicyTask extends RdsActivityTask<EuareMessage, Euare> {
    private String roleName;
    private String policyName;

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

  private class EucalyptusDescribeAvailabilityZonesTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<ClusterInfoType>> {
    private boolean verbose;
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

  private class EucalyptusDescribeServicesTask extends RdsActivityTaskWithResult<EmpyreanMessage, Empyrean,List<ServiceStatusType>> {
    private String componentType;
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

  private class EucalyptusDescribeInstanceTask extends RdsActivityTaskWithResult<ComputeMessage, Compute,List<RunningInstancesItemType>> {
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
          Lists.newArrayList( ) :
          null;
    }
  }

  private class EucalyptusCreateGroupTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, String> {
    private String groupName;
    private String groupDesc;
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

  private class EucalyptusAuthorizeIngressRuleTask extends RdsActivityTask<ComputeMessage, Compute> {
    String groupNameOrId;
    String protocol;
    int portNum;

    EucalyptusAuthorizeIngressRuleTask(String groupNameOrId, String protocol, int portNum){
      this.groupNameOrId = groupNameOrId;
      this.protocol=protocol;
      this.portNum = portNum;
    }
    AuthorizeSecurityGroupIngressType getRequest(){
      AuthorizeSecurityGroupIngressType req = new AuthorizeSecurityGroupIngressType( );
      if ( this.groupNameOrId.matches( "sg-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" ) ) {
        req.setGroupId( this.groupNameOrId );
      } else {
        req.setGroupName( this.groupNameOrId );
      }
      IpPermissionType perm = new IpPermissionType();
      perm.setFromPort(this.portNum);
      perm.setToPort(this.portNum);
      perm.setCidrIpRanges( Collections.singleton( "0.0.0.0/0" ) );
      perm.setIpProtocol(this.protocol);
      req.getIpPermissions( ).add( perm );
      return req;
    }
  }
  private class EucalyptusRevokeIngressRuleTask extends RdsActivityTask<ComputeMessage, Compute> {
    String groupName;
    String protocol;
    int portNum;
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
      perm.setCidrIpRanges( Lists.newArrayList( "0.0.0.0/0" ) );
      perm.setIpProtocol(this.protocol);
      req.setIpPermissions(Lists.newArrayList(perm));
      return req;
    }
  }

  private class EucalyptusRevokeIngressRuleFromOtherGroupTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String groupId;
    private String protocol;
    private Integer fromPortNum;
    private Integer toPortNum;
    private String sourceGroupId;
    private String sourceAccountId;

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
      req.setIpPermissions(new ArrayList<>());
      final IpPermissionType perm = new IpPermissionType();
      perm.setIpProtocol(this.protocol);
      if(this.sourceAccountId!=null && this.sourceGroupId!=null) {
        perm.setGroups(new ArrayList<>());
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

  private class EucalyptusAuthorizeEgressRuleTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String groupId;
    private String protocol;
    private int fromPort;
    private int toPort;
    private String cidrSourceRange;
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
      perm.setCidrIpRanges( Lists.newArrayList( this.cidrSourceRange ) );
      req.setIpPermissions(Lists.newArrayList(perm));
      return req;
    }
  }

  private class EucalyptusRevokeEgressRuleTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String groupId;
    private String protocol;
    private int fromPort;
    private int toPort;
    private String cidrSourceRange;
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
      perm.setCidrIpRanges( Lists.newArrayList(this.cidrSourceRange ) );
      req.setIpPermissions(Lists.newArrayList(perm));
      return req;
    }
  }


  private class EucalyptusDeleteGroupTask extends RdsActivityTask<ComputeMessage, Compute> {
    private String groupName;
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

  private class EucalyptusDescribeSecurityGroupTask extends RdsActivityTaskWithResult<ComputeMessage, Compute, List<SecurityGroupItemType>> {
    @Nullable
    private List<String> groupIds;
    @Nullable private List<String> groupNames;
    @Nullable private String vpcId;

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

  private class EucalyptusModifySecurityGroupsTask extends RdsActivityTask<ComputeMessage, Compute> {
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


  private static Filter filter(final String name, String value ) {
    return filter( name, Collections.singleton( value ) );
  }

  private static Filter filter( final String name, final Iterable<String> values ) {
    return CloudFilters.filter( name, values );
  }

  private abstract class RdsActivityTask<TM extends BaseMessage, TC extends ComponentId>{
    protected RdsActivityTask(){}

    final CheckedListenableFuture<Boolean> dispatch(final ActivityContext<TM,TC> context ) {
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
      LOG.error( "Rds activity error", throwable );
      return false;
    }

    void dispatchSuccess( ActivityContext<TM,TC> context, TM response ){ }
  }

  private abstract class RdsActivityTaskWithResult<TM extends BaseMessage, TC extends ComponentId, R>
      extends RdsActivityTask<TM,TC> {
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
      final RdsActivityTask<TM,TC> task,
      final ActivityContext<TM,TC> context,
      final String errorMessage
  ) {
    final CheckedListenableFuture<Boolean> result = task.dispatch( context );
    try{
      if ( !result.get( ) ) {
        throw new RdsActivityException( errorMessage );
      }
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }

  private <TM extends BaseMessage, TC extends ComponentId, R> R resultOf(
      final RdsActivityTaskWithResult<TM,TC,R> task,
      final ActivityContext<TM,TC> context,
      final String errorMessage
  ) {
    final CheckedListenableFuture<Boolean> result = task.dispatch( context );
    try{
      if (result.get() ){
        return task.getResult();
      }else
        throw new RdsActivityException( errorMessage );
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }
}

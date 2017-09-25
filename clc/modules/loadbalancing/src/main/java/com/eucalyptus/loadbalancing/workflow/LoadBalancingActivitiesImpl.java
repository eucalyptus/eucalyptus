/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.loadbalancing.workflow;

import static com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView.name;
import static com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView.subnetId;
import static com.eucalyptus.loadbalancing.service.LoadBalancingService.MAX_HEALTHCHECK_INTERVAL_SEC;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.tokens.SecurityTokenAWSCredentialsProvider;
import com.eucalyptus.compute.common.*;
import com.eucalyptus.loadbalancing.*;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.loadbalancing.LoadBalancingSystemVpcs;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.eucalyptus.ws.StackConfiguration;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.euare.common.msgs.GetRolePolicyResult;
import com.eucalyptus.auth.euare.common.msgs.InstanceProfileType;
import com.eucalyptus.auth.euare.common.msgs.RoleType;
import com.eucalyptus.auth.euare.common.msgs.ServerCertificateType;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupsType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResult;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.autoscaling.common.msgs.Instances;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancer.LoadBalancerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerBackendInstance.LoadBalancerBackendInstanceEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerListener.LoadBalancerListenerEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerListener.PROTOCOL;
import com.eucalyptus.loadbalancing.LoadBalancerPolicyDescription.LoadBalancerPolicyDescriptionCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerSecurityGroup.LoadBalancerSecurityGroupEntityTransform;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneCoreView;
import com.eucalyptus.loadbalancing.LoadBalancerZone.LoadBalancerZoneEntityTransform;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityException;
import com.eucalyptus.loadbalancing.activities.EucalyptusActivityTasks;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerAutoScalingGroup.LoadBalancerAutoScalingGroupEntityTransform;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceCoreView;
import com.eucalyptus.loadbalancing.activities.LoadBalancerServoInstance.LoadBalancerServoInstanceEntityTransform;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyAttribute;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@ComponentPart(LoadBalancing.class)
public class LoadBalancingActivitiesImpl implements LoadBalancingActivities {
  private static Logger LOG  = Logger.getLogger( LoadBalancingActivitiesImpl.class );

  private static int findAvailableResources(final List<ClusterInfoType> clusters, final String zoneName, final String instanceType){
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
            if( tokens.length > 0 ){
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
  public boolean createLbAdmissionControl(final String accountNumber, final String lbName, final String[] zones) throws LoadBalancingActivityException {
    final String emi = LoadBalancingWorkerProperties.IMAGE;
    List<ImageDetails> images;
    try{
      images = EucalyptusActivityTasks.getInstance().describeImagesWithVerbose(Lists.newArrayList(emi));
      if(images==null || images.size()<=0 ||! images.get(0).getImageId().toLowerCase().equals(emi.toLowerCase()))
        throw new Exception("No loadbalancer EMI is found");
    }catch(final Exception ex){
      throw new LoadBalancingActivityException("failed to validate the loadbalancer EMI", ex);
    }

    // zones: is the CC found?
    final List<String> requestedZones = Lists.newArrayList(zones);
    List<ClusterInfoType> clusters;
    try{
      clusters = EucalyptusActivityTasks.getInstance().describeAvailabilityZonesWithVerbose();
      for(final ClusterInfoType cc : clusters){
        requestedZones.remove(cc.getZoneName());
      }
    }catch(final Exception ex){
      throw new InvalidConfigurationRequestException("failed to validate the requested zones", ex);
    }
    if(requestedZones.size()>0){
      throw new InvalidConfigurationRequestException("unknown zone is requested");
    }

    // are there enough resources?
    final String instanceType = LoadBalancingWorkerProperties.INSTANCE_TYPE;
    int numVm = 1;
      try{
        //// TODO: fix
        numVm = Integer.parseInt(LoadBalancingServiceProperties.VM_PER_ZONE);
      }catch(final NumberFormatException ex){
        LOG.warn("unable to parse loadbalancer_num_vm");
      }
      for(final String zone : zones){
      final int capacity = findAvailableResources(clusters, zone, instanceType);
      if(numVm>capacity){
        throw new NotEnoughResourcesException();
      }
    }

    // check if the keyname is configured and exists, the key name for new ELB's should be from
    // loadbalancing account
    final String keyName = LoadBalancingWorkerProperties.KEYNAME;
    if ( keyName != null && !keyName.isEmpty() ) {
      try {
        Ec2Client.getInstance().describeKeyPairs(Accounts.lookupSystemAccountByAlias(
           AccountIdentifiers.ELB_SYSTEM_ACCOUNT ).getUserId( ), Lists.newArrayList(keyName));
      } catch(Exception ex) {
        throw new LoadBalancingActivityException("The configured keyname is not found."
            + " Do you have keypair " + keyName + " that belongs to "
            + AccountIdentifiers.ELB_SYSTEM_ACCOUNT + " account?");
      }
    }
    return true;
  }


  private static final String DEFAULT_ROLE_PATH_PREFIX = "/internal/loadbalancer";
  public static final String ROLE_NAME_PREFIX = "loadbalancer-vm";
  private static final String DEFAULT_ASSUME_ROLE_POLICY =
      "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":[\"ec2.amazonaws.com\"]},\"Action\":[\"sts:AssumeRole\"]}]}";

  // FIXME: use lambda expression
  public static String getRoleName(final String accountNumber, final String loadbalancer) {
    return String.format("%s-%s-%s", ROLE_NAME_PREFIX, accountNumber, loadbalancer);
  }

  @Override
  public String iamRoleSetup(final String accountNumber, final String lbName) throws LoadBalancingActivityException{
    RoleType role = null;
    final String roleName = getRoleName(accountNumber, lbName);
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
      throw new LoadBalancingActivityException("Failed to list IAM roles", ex);
    }

    // if no role found, create a new role with assume-role policy for elb
    if(role==null){ /// create a new role
      try{
        role = EucalyptusActivityTasks.getInstance().createRole(roleName, DEFAULT_ROLE_PATH_PREFIX, DEFAULT_ASSUME_ROLE_POLICY);
      }catch(Exception ex){
        throw new LoadBalancingActivityException("Failed to create the role for ELB Vms");
      }
    }

    if(role==null)
      throw new LoadBalancingActivityException("No role is found for LoadBalancer Vms");

    return role.getRoleName();
  }

  public static final String DEFAULT_INSTANCE_PROFILE_PATH_PREFIX="/internal/loadbalancer";
  public static final String INSTANCE_PROFILE_NAME_PREFIX = "loadbalancer-vm";
  private static String getInstanceProfileName (final String accountNumber, final String loadbalancer) {
    return String.format("%s-%s-%s", INSTANCE_PROFILE_NAME_PREFIX, accountNumber, loadbalancer);
  }
  @Override
  public String instanceProfileSetup(final String accountNumber, final String lbName,
      String roleName) throws LoadBalancingActivityException{
    InstanceProfileType instanceProfile = null;
    final String instanceProfileName = getInstanceProfileName(accountNumber, lbName);
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
      throw new LoadBalancingActivityException("Failed to list instance profiles", ex);
    }

    if(instanceProfile == null){  //   if not create one
      try{
        instanceProfile =
            EucalyptusActivityTasks.getInstance().createInstanceProfile(instanceProfileName, DEFAULT_INSTANCE_PROFILE_PATH_PREFIX);
      }catch(Exception ex){
        throw new LoadBalancingActivityException("Failed to create instance profile", ex);
      }
    }
    if(instanceProfile == null)
      throw new LoadBalancingActivityException("No instance profile for loadbalancer VM is found");

    try{
      List<RoleType> roles = instanceProfile.getRoles().getMember();
      boolean roleFound = false;
      for(RoleType role : roles){
        if(role.getRoleName().equals(roleName)){
          roleFound=true;
          break;
        }
      }
      if(!roleFound)
        throw new NoSuchElementException();
    }catch(Exception ex){
      if(roleName == null)
        throw new LoadBalancingActivityException("No role name is found for loadbalancer VMs");
      try{
        EucalyptusActivityTasks.getInstance().addRoleToInstanceProfile(instanceProfile.getInstanceProfileName(), roleName);
      }catch(Exception ex2){
        throw new LoadBalancingActivityException("Failed to add role to the instance profile", ex2);
      }
    }
    return instanceProfile.getInstanceProfileName();
  }

  static final String SERVO_ROLE_POLICY_NAME = "euca-internal-loadbalancer-vm-policy";
  private static final String SERVO_ROLE_POLICY_DOCUMENT=
      "{\"Statement\":[{\"Action\": [\"swf:PollForActivityTask\", \"swf:RegisterActivityType\", \"swf:RespondActivityTaskCanceled\", \"swf:RespondActivityTaskCompleted\", \"swf:RespondActivityTaskFailed\", \"swf:RecordActivityTaskHeartbeat\"],\"Effect\": \"Allow\",\"Resource\": \"*\"}]}";
  @Override
  public String iamPolicySetup(final String accountNumber, final String lbName,
      String roleName) throws LoadBalancingActivityException{
    GetRolePolicyResult policy  = null;
    try{
      final List<String> policies = EucalyptusActivityTasks.getInstance().listRolePolicies(roleName);
      if(policies.contains(SERVO_ROLE_POLICY_NAME)){
         policy = EucalyptusActivityTasks.getInstance().getRolePolicy(roleName, SERVO_ROLE_POLICY_NAME);
      }
    }catch(final Exception ex){
    }

    boolean putPolicy;
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
        throw new LoadBalancingActivityException("failed to put role policy for loadbalancer vm");
      }
    }
    return SERVO_ROLE_POLICY_NAME;
  }

  public static String getSecurityGroupName(final String ownerAccountNumber, final String lbName) {
    return String.format( "euca-internal-%s-%s", ownerAccountNumber, lbName );
  }
  private static String generateDefaultVPCSecurityGroupName( final String vpcId ) {
    return String.format( "default_elb_%s", UUID.nameUUIDFromBytes( vpcId.getBytes( StandardCharsets.UTF_8 ) ).toString( ) );
  }

  @Override
  public SecurityGroupSetupActivityResult securityGroupSetup(final String accountNumber, final String lbName)
      throws LoadBalancingActivityException {
    final SecurityGroupSetupActivityResult result = new SecurityGroupSetupActivityResult();
    LoadBalancer lbEntity;
    LoadBalancerCoreView lb;
    try{
      lbEntity = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      lb = lbEntity.getCoreView( );
    }catch(NoSuchElementException ex){
      throw new LoadBalancingActivityException("Could not find the loadbalancer with name="+lbName, ex);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Error while looking for loadbalancer with name="+lbName, ex);
    }

    if ( lb.getVpcId( ) == null ) {
      final String groupName = getSecurityGroupName( lb.getOwnerAccountNumber(), lb.getDisplayName() );
      final String groupDesc = String.format( "group for loadbalancer %s", lbName );

      // check if there's an existing group with the same name
      boolean groupFound = false;
      try {
        List<SecurityGroupItemType> groups = EucalyptusActivityTasks.getInstance().describeSystemSecurityGroups( Lists.newArrayList( groupName ) );
        if ( groups != null ) for ( final SecurityGroupItemType group : groups ) {
          if ( groupName.equals( group.getGroupName() ) && group.getVpcId( ) == null ) {
            groupFound = true;
            result.setGroupName(groupName);
            result.setGroupId(group.getGroupId());
            result.setGroupOwnerAccountId(group.getAccountId());
            break;
          }
        }
      } catch ( Exception ex ) {
        groupFound = false;
      }

      // create a new security group
      if ( !groupFound ) {
        try {
          EucalyptusActivityTasks.getInstance().createSystemSecurityGroup( groupName, groupDesc );
          result.setCreatedGroupName(groupName);
          result.setGroupName(groupName);
          List<SecurityGroupItemType> groups = EucalyptusActivityTasks.getInstance().describeSystemSecurityGroups( Lists.newArrayList( groupName ) );
          if ( groups != null ) for ( final SecurityGroupItemType group : groups ) {
            if ( groupName.equals( group.getGroupName() ) && group.getVpcId( ) == null ) {
              result.setCreatedGroupId(group.getGroupId());
              result.setGroupId(group.getGroupId());
              result.setGroupOwnerAccountId(group.getAccountId());
              break;
            }
          }
        } catch ( Exception ex ) {
          throw new LoadBalancingActivityException( "Failed to create the security group for loadbalancer", ex );
        }
      }

      if(result.getGroupName() == null || result.getGroupOwnerAccountId() == null)
        throw new LoadBalancingActivityException("Failed to create the security group for loadbalancer");

      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerSecurityGroup.class ) ) {
        try {
          Entities.uniqueResult( LoadBalancerSecurityGroup.named( lbEntity,
              result.getGroupOwnerAccountId(), result.getGroupName() ) );
        } catch( NoSuchElementException ex ){
          Entities.persist( LoadBalancerSecurityGroup.create( lbEntity, result.getGroupOwnerAccountId(), result.getGroupName() ) );
        }
        db.commit();
      }catch(Exception ex){
        throw new LoadBalancingActivityException("Error while persisting security group", ex);
      }
      /// END OF NON-VPC CASE
    } else if ( lb.getSecurityGroupIdsToNames( ).isEmpty( ) ) {
      final String groupName = generateDefaultVPCSecurityGroupName( lb.getVpcId( ) );
      final String groupDesc = String.format( "ELB created security group used when no security group is specified during ELB creation - modifications could impact traffic to future ELBs" );
      final AccountFullName accountFullName = AccountFullName.getInstance(accountNumber);
      final List<SecurityGroupItemType> groups = EucalyptusActivityTasks.getInstance()
          .describeUserSecurityGroupsByName( accountFullName, lb.getVpcId( ), groupName );

      final SecurityGroupItemType elbVpcGroup;
      if ( groups.isEmpty( ) ) {
        EucalyptusActivityTasks.getInstance().createUserSecurityGroup( accountFullName, groupName, groupDesc );
        final List<SecurityGroupItemType> createdGroupList = EucalyptusActivityTasks.getInstance( )
            .describeUserSecurityGroupsByName( accountFullName, lb.getVpcId( ), groupName );
        elbVpcGroup = Iterables.getOnlyElement( createdGroupList );
        result.setCreatedGroupId(elbVpcGroup.getGroupId( ));
        result.setCreatedGroupName(elbVpcGroup.getGroupName( ));
      } else {
        elbVpcGroup = Iterables.get( groups, 0 );
      }

      Entities.asDistinctTransaction( LoadBalancer.class, new Predicate<String>( ) {
        @Override
        public boolean apply( @Nullable final String loadBalancerName ) {
          try {
            final LoadBalancer lb =
                Entities.uniqueResult( LoadBalancer.namedByAccountId( accountFullName.getAccountNumber( ), loadBalancerName ) );
            lb.setSecurityGroupRefs( Lists.newArrayList(
              new LoadBalancerSecurityGroupRef( elbVpcGroup.getGroupId( ), elbVpcGroup.getGroupName( ) )
            ) );
          } catch ( TransactionException e ) {
            throw Exceptions.toUndeclared( e );
          }
          return true;
        }
      } ).apply( lb.getDisplayName( ) );

      result.setShouldRollback(false); /// In VPC, security groups are user-owned. So ELB shouldn't delete them during rollback
      result.setGroupId(elbVpcGroup.getGroupId( ));
      result.setGroupName(elbVpcGroup.getGroupName( ));
      result.setGroupOwnerAccountId(elbVpcGroup.getAccountId( ));
    }
    return result;
  }

  @Override
  public CreateTagActivityResult createLbTagCreator(final String accountNumber, final String lbName, String sgroupId) throws LoadBalancingActivityException {
    final String TAG_KEY = "service-type";
    final String TAG_VALUE = "loadbalancing";

    CreateTagActivityResult result = new CreateTagActivityResult();
    // security group
    if(sgroupId!=null){
      final boolean tagGroup;
      try{
        final LoadBalancer lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
        tagGroup = lb.getVpcId( ) == null;
      }catch(NoSuchElementException ex){
        throw new LoadBalancingActivityException("Failed to find the loadbalancer "+ lbName, ex);
      }catch(Exception ex){
        throw new LoadBalancingActivityException("Failed due to query exception", ex);
      }
      if ( tagGroup ) try{
        EucalyptusActivityTasks.getInstance().createTags(TAG_KEY, TAG_VALUE,
            Lists.newArrayList(sgroupId));
        result.setSecurityGroup(sgroupId);
      }catch(final Exception ex){
        LOG.warn("could not tag the security group", ex);
      }
    }
    result.setTagKey(TAG_KEY);
    result.setTagValue(TAG_VALUE);
    return result;
  }


  private static String getLaunchConfigName( final String ownerAccountNumber, final String loadBalancerName, final String availabilityZone ) {
    String newLaunchConfigName =  String.format("lc-euca-internal-elb-%s-%s-%s-%s",
        ownerAccountNumber, loadBalancerName, availabilityZone, UUID.randomUUID().toString().substring(0, 8));
    if(newLaunchConfigName.length()>255)
      newLaunchConfigName = newLaunchConfigName.substring(0, 255);
    return newLaunchConfigName;
  }
  private static String getAutoScalingGroupName( final String ownerAccountNumber, final String loadBalancerName, final String availabilityZone ) {
    String groupName = String.format("euca-internal-elb-%s-%s-%s", ownerAccountNumber, loadBalancerName, availabilityZone );
    if(groupName.length()>255)
      groupName = groupName.substring(0, 255);
    return groupName;
  }

  private static String getCredentialsString() {
    final String credStr = String.format("euca-%s:%s",
        B64.standard.encString("setup-credential"),
        LoadBalancingWorkerProperties.EXPIRATION_DAYS);
    return credStr;
  }

  private static String getLoadBalancerUserData(String initScript, final String ownerAccountNumber) {
    Map<String, String> kvMap = new HashMap<String, String>();

    if (LoadBalancingWorkerProperties.NTP_SERVER != null){
      kvMap.put("ntp_server", LoadBalancingWorkerProperties.NTP_SERVER);
    }
    if(LoadBalancingWorkerProperties.APP_COOKIE_DURATION != null){
      kvMap.put("app-cookie-duration", LoadBalancingWorkerProperties.APP_COOKIE_DURATION);
    }

    kvMap.put("elb_service_url", String.format("loadbalancing.%s",DNSProperties.getDomain()));
    kvMap.put("euare_service_url", String.format("euare.%s", DNSProperties.getDomain()));
    kvMap.put("objectstorage_service_url", String.format("objectstorage.%s", DNSProperties.getDomain()));
    kvMap.put("simpleworkflow_service_url", String.format("simpleworkflow.%s", DNSProperties.getDomain()));
    kvMap.put("webservice_port", String.format("%d", StackConfiguration.PORT));
    if(ownerAccountNumber!=null)
      kvMap.put("loadbalancer_owner_account", ownerAccountNumber);

    try {
      List<ServiceStatusType> services =
          EucalyptusActivityTasks.getInstance().describeServices("eucalyptus");

      if(services == null || services.size()<=0)
        throw new EucalyptusActivityException("failed to describe eucalyptus services");

      ServiceStatusType service = services.get(0);
      String serviceUrl = service.getServiceId().getUri();

      // parse the service Url: e.g., http://192.168.0.1:8773/services/Eucalyptus
      String tmp = serviceUrl.replace("http://", "").replace("https://", "");
      String host = tmp.substring(0, tmp.indexOf(":"));
      tmp = tmp.replace(host+":", "");
      String port = tmp.substring(0, tmp.indexOf("/"));
      String path = tmp.replace(port+"/", "");
      kvMap.put("eucalyptus_host", host);
      kvMap.put("eucalyptus_port", port);
      kvMap.put("eucalyptus_path", path);
    }catch(Exception ex){
      throw Exceptions.toUndeclared(ex);
    }

    final StringBuilder sb = new StringBuilder("#!/bin/bash").append("\n");
    if (initScript != null && initScript.length()>0)
      sb.append(initScript);
    sb.append("\n#System generated Load Balancer Servo config\n");
    sb.append("mkdir -p /etc/load-balancer-servo/\n");
    sb.append("yum -y --disablerepo \\* --enablerepo eucalyptus-service-image install load-balancer-servo\n");
    sb.append("echo \"");
    for (String key : kvMap.keySet()) {
      String value = kvMap.get(key);
      sb.append(String.format("\n%s=%s", key, value));
    }
    sb.append("\" > /etc/load-balancer-servo/servo.conf");
    sb.append("\nchown -R servo:servo /etc/load-balancer-servo");
    sb.append("\nservice load-balancer-servo start");
    return sb.toString();
  }

  private static final String TAG_KEY = "service-type";
  private static final String TAG_VALUE = "loadbalancing";
  @Override
  public AutoscalingGroupSetupActivityResult autoscalingGroupSetup(final String accountNumber,
                                                                   final String lbName, String instanceProfileName,
                                                                   String securityGroupName, List<String> zones, Map<String,String> zoneToSubnetIdMap) throws LoadBalancingActivityException {
    if(LoadBalancingWorkerProperties.IMAGE == null)
      throw new LoadBalancingActivityException("Loadbalancer's EMI is not configured");
    final AutoscalingGroupSetupActivityResult activityResult = new AutoscalingGroupSetupActivityResult();
    final LoadBalancer lbEntity;
    final LoadBalancer.LoadBalancerCoreView lb;
    try{
      lbEntity = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      lb = lbEntity.getCoreView( );
      if ( zoneToSubnetIdMap == null ) {
        zoneToSubnetIdMap = CollectionUtils.putAll(
            Iterables.filter( lbEntity.getZones( ), Predicates.compose( Predicates.notNull( ), subnetId( ) ) ),
            Maps.<String, String>newHashMap( ),
            name( ),
            subnetId( ) );
      }
    }catch(NoSuchElementException ex){
      throw new LoadBalancingActivityException("Failed to find the loadbalancer "+ lbName, ex);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Failed due to query exception", ex);
    }

    if( zones == null)
      return null; // do nothing when zone/groups are not specified

    for (final String availabilityZone : zones) {
      final String groupName = getAutoScalingGroupName( accountNumber, lbName, availabilityZone);
      String launchConfigName = null;

      boolean asgFound = false;
      try{
        final DescribeAutoScalingGroupsResponseType response =
            EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(groupName), lb.useSystemAccount());

        final List<AutoScalingGroupType> groups =
            response.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
        if(groups.size()>0 && groups.get(0).getAutoScalingGroupName().equals(groupName)){
          asgFound =true;
          launchConfigName = groups.get(0).getLaunchConfigurationName();
        }
      }catch(final Exception ex){
        asgFound = false;
      }

      activityResult.setGroupNames(Sets.<String>newHashSet());
      activityResult.setLaunchConfigNames(Sets.<String>newHashSet());
      activityResult.setCreatedGroupNames(Sets.<String>newHashSet());
      activityResult.setCreatedLaunchConfigNames(Sets.<String>newHashSet());
      final List<String> availabilityZones = Lists.newArrayList( availabilityZone );
      String vpcZoneIdentifier = null;
      String systemVpcZoneIdentifier = null;
      if ( !asgFound ) {
        try{
          vpcZoneIdentifier = zoneToSubnetIdMap.isEmpty()
                  ? null : Strings.emptyToNull(Joiner.on(',')
                  .skipNulls().join(Iterables.transform(availabilityZones, Functions.forMap(zoneToSubnetIdMap))));
          if (vpcZoneIdentifier != null)
            systemVpcZoneIdentifier = LoadBalancingSystemVpcs.getSystemVpcSubnetId(vpcZoneIdentifier);
          else
            systemVpcZoneIdentifier = null;
        }catch(final Exception ex) {
          throw new LoadBalancingActivityException("Failed to look up subnet ID", ex);
        }

        try{
          Set<String> securityGroupNamesOrIds = null;
          if (systemVpcZoneIdentifier == null ) {
            securityGroupNamesOrIds = Sets.newHashSet();
            if (!lb.getSecurityGroupIdsToNames().isEmpty()) {
              securityGroupNamesOrIds.addAll(lb.getSecurityGroupIdsToNames().keySet());
            } else {
              if (securityGroupName != null) {
                securityGroupNamesOrIds.add(securityGroupName);
              }
            }
          } else { // if system VPC is used, use it's security group
            securityGroupNamesOrIds = Sets.newHashSet();
            securityGroupNamesOrIds.add(
                    LoadBalancingSystemVpcs.getSecurityGroupId(systemVpcZoneIdentifier)
            );
          }

          final String KEYNAME = LoadBalancingWorkerProperties.KEYNAME;
          final String keyName =
              KEYNAME!=null && KEYNAME.length()>0 ? KEYNAME : null;

          final String userData = B64.standard.encString(String.format("%s\n%s",
              getCredentialsString(),
              getLoadBalancerUserData(LoadBalancingWorkerProperties.INIT_SCRIPT,
                  lb.getOwnerAccountNumber())));

          launchConfigName = getLaunchConfigName (lb.getOwnerAccountNumber(), lb.getDisplayName(), availabilityZone);
          EucalyptusActivityTasks.getInstance().createLaunchConfiguration(LoadBalancingWorkerProperties.IMAGE,
              LoadBalancingWorkerProperties.INSTANCE_TYPE, instanceProfileName,
              launchConfigName, securityGroupNamesOrIds, keyName, userData,
                  zoneToSubnetIdMap.isEmpty( ) ? null : false, lb.useSystemAccount() );
          activityResult.getLaunchConfigNames().add(launchConfigName);
          activityResult.getCreatedLaunchConfigNames().add(launchConfigName);
        }catch(Exception ex){
          throw new LoadBalancingActivityException("Failed to create launch configuration", ex);
        }
      }
      activityResult.getLaunchConfigNames().add(launchConfigName);

      /// FIXME
      Integer capacity = LoadBalancingServiceProperties.getCapacityPerZone();

      if(!asgFound){
        // create autoscaling group with the zone and desired capacity
        try{
          EucalyptusActivityTasks.getInstance().createAutoScalingGroup(groupName, availabilityZones, systemVpcZoneIdentifier,
                  capacity, launchConfigName, TAG_KEY, TAG_VALUE, lb.useSystemAccount());

          activityResult.getGroupNames().add(groupName);
          activityResult.getCreatedGroupNames().add(groupName);
          if (activityResult.getNumVMsPerZone() == null || activityResult.getNumVMsPerZone() == 0) {
            activityResult.setNumVMsPerZone(capacity);
          } else {
            activityResult.setNumVMsPerZone(activityResult.getNumVMsPerZone() + capacity);
          }
        }catch(Exception ex){
          throw new LoadBalancingActivityException("Failed to create autoscaling group", ex);
        }
      }else {
        try{
          EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, availabilityZones, capacity, launchConfigName, lb.useSystemAccount());
        }catch(Exception ex){
          throw new LoadBalancingActivityException("Failed to update the autoscaling group", ex);
        }
      }
      activityResult.getGroupNames().add(groupName);
      if (activityResult.getNumVMsPerZone() == null || activityResult.getNumVMsPerZone() == 0) {
        activityResult.setNumVMsPerZone(capacity);
      } else {
        activityResult.setNumVMsPerZone(activityResult.getNumVMsPerZone() + capacity);
      }
      // commit ASG record to the database
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerAutoScalingGroup.class ) ) {
        try {
          final LoadBalancerAutoScalingGroup group = Entities.uniqueResult( LoadBalancerAutoScalingGroup.named( lbEntity, availabilityZone ) );
          if ( capacity != null ) group.setCapacity( capacity );
        }catch(NoSuchElementException ex){
          final LoadBalancerAutoScalingGroup group =
                  LoadBalancerAutoScalingGroup.newInstance(lbEntity, availabilityZone,
                          vpcZoneIdentifier, systemVpcZoneIdentifier, groupName, launchConfigName);
          if ( capacity != null ) group.setCapacity(capacity);
          Entities.persist(group);
        }
        db.commit();
      }catch(final Exception ex){
        throw new LoadBalancingActivityException("Failed to commit the database", ex);
      }
    } // end of for all zones
    return activityResult;
  }

  @Override
  public void securityGroupSetupRollback(String accountNumber,
      String lbName, SecurityGroupSetupActivityResult result) {
    if(result.getCreatedGroupName() == null || !result.getShouldRollback())
      return;
    // set security group with the loadbalancer; update db
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    } catch(Exception ex){
      return;
    }

    try{
      EucalyptusActivityTasks.getInstance().deleteSystemSecurityGroup( result.getCreatedGroupName() );
    }catch(Exception ex){
      // when there's any servo instance referencing the security group
      // SecurityGroupCleanup will clean up records
    }

    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerSecurityGroup.class ) ) {
      final LoadBalancerSecurityGroup group =
          Entities.uniqueResult(LoadBalancerSecurityGroup.named(lb,
              result.getGroupOwnerAccountId(), result.getCreatedGroupName()));
      group.setState(LoadBalancerSecurityGroup.STATE.OutOfService);
      group.setLoadBalancer(null);
      Entities.persist(group);
      db.commit();
    }catch(NoSuchElementException ex){
    }catch(Exception ex){
      LOG.error("failed to mark the security group OutOfService", ex);
    }
  }

  @Override
  public void createLbTagCreatorRollback(CreateTagActivityResult result) {
    if(result.getSecurityGroup()!=null){
      try{
        EucalyptusActivityTasks.getInstance().deleteTags(result.getTagKey(),
            result.getTagValue(), Lists.newArrayList(result.getSecurityGroup()));
      }catch(final Exception ex){
        ;
      }
    }
  }

  @Override
  public void autoscalingGroupSetupRollback(String accountNumber,
      String lbName, AutoscalingGroupSetupActivityResult result) {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(Exception ex){
      LOG.error("failed to find the loadbalancer: " + lbName);
      return;
    }

    if(result.getCreatedGroupNames()!=null) {
      for(final String asgName : result.getCreatedGroupNames()) {
        // delete autoscaling group
        try{
          // terminate all instances
          EucalyptusActivityTasks.getInstance().deleteAutoScalingGroup(asgName, true,
              lb.useSystemAccount());
        }catch(Exception ex){
          LOG.error("failed to delete autoscaling group - "+ asgName);
        }
      }
    }
    if(result.getCreatedLaunchConfigNames()!=null) {
      for(final String launchConfigName : result.getCreatedLaunchConfigNames()) {
        // delete launch config
        try{
          EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(launchConfigName, lb.useSystemAccount());
        }catch(Exception ex){
          LOG.error("failed to delete launch configuration - "+launchConfigName);
        }
      }
    }
  }

  @Override
  public List<String> enableAvailabilityZonesPersistUpdatedZones(String accountNumber,
      String lbName, List<String> zonesToEnable, Map<String, String> zoneToSubnetIdMap)
          throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      throw new LoadBalancingActivityException("Could not find the loadbalancer with name="+ lbName, ex);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Error while looking for loadbalancer with name="+ lbName, ex);
    }

    final List<String> persistedZones = Lists.newArrayList();
    if(zonesToEnable!= null) {
      for(final String zone : zonesToEnable){
        try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
          try {
            final LoadBalancerZone exist = Entities.uniqueResult( LoadBalancerZone.named( lb, zone ) );
            exist.setState( LoadBalancerZone.STATE.InService );
          }catch(NoSuchElementException ex){
            final String subnetId =
                zoneToSubnetIdMap == null ? null : zoneToSubnetIdMap.get( zone );
            final LoadBalancerZone newZone = LoadBalancerZone.create( lb, zone, subnetId );
            newZone.setState(LoadBalancerZone.STATE.InService);
            Entities.persist(newZone);
          }
          persistedZones.add(zone);
          db.commit();
        }catch(Exception ex){
          throw new LoadBalancingActivityException("Error adding load balancer zone", ex );
        }
      }
    }
    return persistedZones;
  }

  @Override
  public void enableAvailabilityZonesPersistUpdatedZonesRollback(String accountNumber, String lbName, List<String> zonesToRollback) {
    if(zonesToRollback==null || zonesToRollback.size()<=0)
      return;

    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      LOG.warn("Could not find the loadbalancer with name="+ lbName, ex);
      return;
    }catch(Exception ex){
      LOG.warn("Error while looking for loadbalancer with name="+ lbName, ex);
      return;
    }

    for(final LoadBalancerZoneCoreView zoneView : lb.getZones()){
      if(zonesToRollback.contains(zoneView.getName())) {
        try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
          final LoadBalancerZone sample = LoadBalancerZone.named(lb,  zoneView.getName());
          final LoadBalancerZone update = Entities.uniqueResult(sample);
          update.setState( LoadBalancerZone.STATE.OutOfService );
          db.commit();
        }catch(final Exception ex){
          LOG.error("could not mark out of state for the zone", ex);
        }
      }
    }
  }

  @Override
  public void enableAvailabilityZonesPersistBackendInstanceState(
      String accountNumber, String lbName, List<String> enabledZones)
          throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      LOG.warn("Could not find the loadbalancer with name="+ lbName, ex);
      return;
    }catch(Exception ex){
      LOG.warn("Error while looking for loadbalancer with name="+ lbName, ex);
      return;
    }

    try{
      for(final String enabledZone : enabledZones) {
        final LoadBalancerZone zone = LoadBalancers.findZone(lb, enabledZone);
        for(final LoadBalancerBackendInstanceCoreView instance : zone.getBackendInstances()){
          try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
            final LoadBalancerBackendInstance update = Entities.uniqueResult(
                LoadBalancerBackendInstance.named(lb, instance.getInstanceId()));
            update.setReasonCode( "" );
            update.setDescription( "" );
            db.commit();
          }catch(final NoSuchElementException ex){
            LOG.warn("failed to find the backend instance");
          }catch(final Exception ex){
            LOG.warn("failed to query the backend instance", ex);
          }
        }
      }
    }catch(final Exception ex){
      LOG.warn("unable to update backend instances after enabling zone", ex);
    }
  }

  @Override
  public void createListenerCheckSSLCertificateId(String accountNumber,
      String lbName, Listener[] listeners)
          throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("could not find the loadbalancer", ex);
    }

    for(Listener listener : listeners){
      final PROTOCOL protocol = PROTOCOL.valueOf(listener.getProtocol().toUpperCase());
      if(protocol.equals(PROTOCOL.HTTPS) || protocol.equals(PROTOCOL.SSL)) {
        final String certArn = listener.getSSLCertificateId();
        if(certArn == null || certArn.length()<=0)
          throw new LoadBalancingActivityException("No SSLCertificateId is specified");
        final String prefix = String.format("arn:aws:iam::%s:server-certificate", accountNumber);
        if(!certArn.startsWith(prefix))
          throw new LoadBalancingActivityException("SSLCertificateId is not ARN format");
        try{
          final String pathAndName = certArn.replace(prefix, "");
          final String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);

          final ServerCertificateType cert =
              EucalyptusActivityTasks.getInstance().getServerCertificate(accountNumber, certName);
          if(cert==null)
            throw new LoadBalancingActivityException("No SSL certificate is found with the ARN");
          if(!certArn.equals(cert.getServerCertificateMetadata().getArn()))
            throw new LoadBalancingActivityException("Returned certificate's ARN doesn't match the request");
        }catch(final LoadBalancingActivityException ex){
          throw ex;
        }catch(final Exception ex){
            throw new LoadBalancingActivityException("Failed to get SSL server certificate", ex);
        }
      }
    }
  }


  public static final String SERVER_CERT_ROLE_POLICY_NAME_PREFIX = "loadbalancer-iam-policy";
  public static final String ROLE_SERVER_CERT_POLICY_DOCUMENT=
      "{\"Statement\":[{\"Action\": [\"iam:DownloadServerCertificate\"],\"Effect\": \"Allow\",\"Resource\": \"CERT_ARN_PLACEHOLDER\"}]}";
  @Override
  public AuthorizeSSLCertificateActivityResult createListenerAuthorizeSSLCertificate(String accountNumber,
      String lbName, Listener[] listeners)
          throws LoadBalancingActivityException {
    final AuthorizeSSLCertificateActivityResult result =
        new AuthorizeSSLCertificateActivityResult();
    final Set<String> certArns = Sets.newHashSet();
    final List<String> policyNames = Lists.newArrayList();

    for(final Listener listener : listeners){
      final PROTOCOL protocol = PROTOCOL.valueOf(listener.getProtocol().toUpperCase());
      if(protocol.equals(PROTOCOL.HTTPS) || protocol.equals(PROTOCOL.SSL)) {
        certArns.add(listener.getSSLCertificateId());
      }
    }
    if(certArns.size() <= 0)
      return result;

    LoadBalancer lb = null;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("could not find the loadbalancer", ex);
    }

    final String roleName = String.format("%s-%s-%s",
        ROLE_NAME_PREFIX,
        accountNumber, lbName);
    final String prefix =
        String.format("arn:aws:iam::%s:server-certificate",
            accountNumber);

    for (final String arn : certArns){
      if(!arn.startsWith(prefix))
        continue;
      String pathAndName = arn.replace(prefix, "");
      String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);
      String policyName = String.format("%s-%s-%s-%s", SERVER_CERT_ROLE_POLICY_NAME_PREFIX,
          accountNumber, lbName, certName);
      final String rolePolicyDoc = ROLE_SERVER_CERT_POLICY_DOCUMENT.replace("CERT_ARN_PLACEHOLDER", arn);
      try{
        EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, policyName, rolePolicyDoc, lb.useSystemAccount());
        policyNames.add(policyName);
      }catch(final Exception ex){
        throw new LoadBalancingActivityException("failed to authorize server certificate for SSL listener", ex);
      }
    }
    result.setPolicyNames(policyNames);
    result.setRoleName(roleName);
    return result;
  }

  @Override
  public void createListenerAuthorizeSSLCertificateRollback(
      String accountNumber, String lbName, AuthorizeSSLCertificateActivityResult result) {
    final String roleName = result.getRoleName();
    final List<String> policyNames = result.getPolicyNames();

    if(roleName!=null && policyNames!=null && policyNames.size()>0){
      LoadBalancer lb = null;
      try{
        lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      }catch(Exception ex){
        return;
      }

      for(final String policyName : policyNames){
        try{
          EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, policyName, lb.useSystemAccount());
        }catch(final Exception ex){
          LOG.warn("Failed to delete role policy during listener creation rollback", ex);
        }
      }
    }
  }

  @Override
  public AuthorizeIngressRuleActivityResult createListenerAuthorizeIngressRule(String accountNumber,
      String lbName, Listener[] listeners)
          throws LoadBalancingActivityException {
    final AuthorizeIngressRuleActivityResult result = new AuthorizeIngressRuleActivityResult();
    result.setListeners(Lists.<Listener>newArrayList());
    LoadBalancer lb;
    String groupName = null;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      final LoadBalancerSecurityGroupCoreView group = lb.getGroup();
      if(group!=null)
        groupName = group.getName();
    }catch(Exception ex){
      throw new LoadBalancingActivityException("could not find the loadbalancer", ex);
    }

    final Map<String,String> securityGroupIdsToNames =
        lb.getCoreView( ).getSecurityGroupIdsToNames( );
    String protocol = "tcp"; /// Loadbalancer listeners protocols: HTTP, HTTPS, TCP, SSL -> all tcp
    if ( lb.getVpcId( ) == null ) {
      if ( groupName == null )
        throw new LoadBalancingActivityException( "Group name is not found" );

      for ( Listener listener : listeners ) {
        int port = listener.getLoadBalancerPort();
        try {
          EucalyptusActivityTasks.getInstance().authorizeSystemSecurityGroup( groupName, protocol, port, lb.useSystemAccount() );
          result.getListeners().add(listener);
        } catch ( Exception ex ) {
          throw new LoadBalancingActivityException( String.format( "failed to authorize %s, %s, %d", groupName, protocol, port ), ex );
        }
      }
    } else if ( securityGroupIdsToNames.size( ) == 1 ) {
      if (securityGroupIdsToNames.values().contains(generateDefaultVPCSecurityGroupName(lb.getVpcId()))) {
        boolean isRuleEmpty = false;
        try {
          final SecurityGroupItemType defaultGroup =
                  EucalyptusActivityTasks.getInstance()
                          .describeUserSecurityGroupsByName(AccountFullName.getInstance(accountNumber),
                                  lb.getVpcId(), securityGroupIdsToNames.values().stream().findAny().get())
                          .stream().findAny().get();
          if (defaultGroup.getIpPermissions() == null ||
                  defaultGroup.getIpPermissions().isEmpty()) {
            isRuleEmpty = true;
          }
        } catch (final Exception ex) {
          isRuleEmpty = false;
        }

        if (isRuleEmpty) { // the rule is created only for the first time the group is created
          final String groupId = Iterables.getOnlyElement(securityGroupIdsToNames.keySet());
          for (Listener listener : listeners) {
            int port = listener.getLoadBalancerPort();
            try {
              EucalyptusActivityTasks.getInstance().authorizeSystemSecurityGroup(groupId, protocol, port, false);
            } catch (Exception ex) {
              throw new LoadBalancingActivityException(String.format("failed to authorize %s, %s, %d", groupId, protocol, port), ex);
            }
          }
        }
      }
    }
    return result;
  }

  @Override
  public void createListenerAuthorizeIngressRuleRollback(String accountNumber,
      String lbName, AuthorizeIngressRuleActivityResult result) {
    final List<Listener> listeners = result.getListeners();

    if (listeners == null || listeners.size() <= 0)
      return;

    LoadBalancer lb = null;
    String groupName = null;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      final LoadBalancerSecurityGroupCoreView group = lb.getGroup();
      if(group!=null)
        groupName = group.getName();
    }catch(Exception ex){
      ;
    }

    if(groupName == null)
      return;

    for(Listener listener : listeners){
      int port = listener.getLoadBalancerPort();
      String protocol = listener.getProtocol();
      protocol = protocol.toLowerCase();

      try{
        EucalyptusActivityTasks.getInstance().revokeSystemSecurityGroup( groupName, protocol, port, lb.useSystemAccount() );
      }catch(Exception ex){
        ;
      }
    }
  }

  @Override
  public void createListenerUpdateHealthCheckConfig(String accountNumber,
      String lbName, Listener[] listeners)
          throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(final NoSuchElementException ex){
      throw new LoadBalancingActivityException("Could not find the loadbalancer with name="+ lbName, ex);
    }catch(final Exception ex){
      throw new LoadBalancingActivityException("Error while looking for loadbalancer with name="+ lbName, ex);
    }
    final int DEFAULT_HEALTHY_THRESHOLD = 3;
    final int DEFAULT_INTERVAL = 30;
    final int DEFAULT_TIMEOUT = 5;
    final int DEFAULT_UNHEALTHY_THRESHOLD = 3;
    /* default setting in AWS
    "HealthyThreshold": 10,
    "Interval": 30,
    "Target": "TCP:8000",
    "Timeout": 5,
    "UnhealthyThreshold": 2 */
    try{
      lb.getHealthCheckTarget();
      lb.getHealthCheckInterval();
      lb.getHealthCheckTimeout();
      lb.getHealthCheckUnhealthyThreshold();
      lb.getHealthyThreshold();
    }catch(final IllegalStateException ex){ /// only when the health check is not previously configured
      if(listeners==null || listeners.length<=0)
        throw new LoadBalancingActivityException("No listener requested");

      final Listener firstListener = listeners[0];
      final String target = String.format( "TCP:%d", firstListener.getInstancePort() );
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancer.class ) ) {
        final LoadBalancer update = Entities.uniqueResult(lb);
        update.setHealthCheck( DEFAULT_HEALTHY_THRESHOLD, DEFAULT_INTERVAL, target, DEFAULT_TIMEOUT, DEFAULT_UNHEALTHY_THRESHOLD );
        db.commit();
      }catch(final NoSuchElementException exx){
        LOG.warn("Loadbalancer not found in the database");
      }catch(final Exception exx){
        LOG.warn("Unable to query the loadbalancer", ex);
      }
    }
  }

  @Override
  public void createListenerAddDefaultSSLPolicy(String accountNumber,
      String lbName, Listener[] listeners)
          throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      throw new LoadBalancingActivityException("Could not find the loadbalancer with name="+lbName, ex);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Error while looking for loadbalancer with name="+lbName, ex);
    }
    boolean sslListener = false;
    for(final Listener l : listeners) {
      final String protocol = l.getProtocol().toLowerCase();
      if("https".equals(protocol) || "ssl".equals(protocol)) {
        sslListener = true;
        break;
      }
    }
    if(!sslListener)
      return;

    try{
      /// this will load the sample policies into memory
      if(LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME == null) {
        LoadBalancerPolicies.getSamplePolicyDescription();
        if(LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME == null)
          throw new LoadBalancingActivityException("Latest security policy is not found");
      }

      boolean policyCreated = false;
      final Collection<LoadBalancerPolicyDescriptionCoreView> policies = lb.getPolicies();
      if(policies != null) {
        for (final LoadBalancerPolicyDescriptionCoreView view : policies ) {
          if ("SSLNegotiationPolicyType".equals(view.getPolicyTypeName()) &&
              LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME.equals(view.getPolicyName())) {
            policyCreated = true;
            break;
          }
        }
      }
      if(! policyCreated) {
        final PolicyAttribute attr = new PolicyAttribute();
        attr.setAttributeName("Reference-Security-Policy");
        attr.setAttributeValue(LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME);
        LoadBalancerPolicies.addLoadBalancerPolicy(lb, LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME, "SSLNegotiationPolicyType",
            Lists.newArrayList(attr));
        try{ // reload with the newly created policy
          lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
        }catch(NoSuchElementException ex){
          throw new LoadBalancingActivityException("Could not find the loadbalancer with name="+ lbName, ex);
        }catch(Exception ex){
          throw new LoadBalancingActivityException("Error while looking for loadbalancer with name="+ lbName, ex);
        }
      }
    }catch (final Exception ex) {
      LOG.warn("Failed to create default security policy for https/ssl listeners", ex);
      return;
    }

    try{
      final LoadBalancerPolicyDescription policy =
          LoadBalancerPolicies.getLoadBalancerPolicyDescription(lb, LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME);
      if(policy==null)
        throw new LoadBalancingActivityException("No such policy is found: "+LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME);

      final Collection<LoadBalancerListenerCoreView> lbListeners = lb.getListeners();
      for(final Listener l : listeners) {
        final String protocol = l.getProtocol().toLowerCase();
        if("https".equals(protocol) || "ssl".equals(protocol)) {
          LoadBalancerListener listener = null;
          for(final LoadBalancerListenerCoreView view : lbListeners){
            if(view.getLoadbalancerPort() == l.getLoadBalancerPort()){
              listener = LoadBalancerListenerEntityTransform.INSTANCE.apply(view);
              break;
            }
          }
          if(listener == null)
            throw new LoadBalancingActivityException("No such listener is found");
          boolean policyAttached=false;
          final List<LoadBalancerPolicyDescriptionCoreView> listenerPolicies = listener.getPolicies();
          if(listenerPolicies!=null) {
            for(final LoadBalancerPolicyDescriptionCoreView listenerPolicy : listenerPolicies ) {
              if( "SSLNegotiationPolicyType".equals(listenerPolicy.getPolicyTypeName()) &&
                  LoadBalancerPolicies.LATEST_SECURITY_POLICY_NAME.equals(listenerPolicy.getPolicyName()))
              {
                policyAttached = true;
                break;
              }
            }
          }

          if(!policyAttached && listener!=null && policy!=null) {
            LoadBalancerPolicies.addPoliciesToListener(listener, Lists.newArrayList(policy));
          }
        }
      }
    }catch(final Exception ex) {
      LOG.warn("Failed to set default security policy to https/ssl listeners", ex);
    }
  }

  @Override
  public HealthCheck lookupLoadBalancerHealthCheck(final String accountNumber, final String lbName)
          throws LoadBalancingActivityException {
    try {
      final LoadBalancer lb =
              LoadBalancers.getLoadbalancer(accountNumber, lbName);
      final HealthCheck hc = new HealthCheck();
      hc.setHealthyThreshold(lb.getHealthyThreshold());
      hc.setInterval(lb.getHealthCheckInterval());
      hc.setTarget(lb.getHealthCheckTarget());
      hc.setTimeout(lb.getHealthCheckTimeout());
      hc.setUnhealthyThreshold(lb.getHealthCheckUnhealthyThreshold());
      return hc;
    }catch(final Exception ex) {
      throw new LoadBalancingActivityException(String.format("Failed to lookup loadbalancer (%s:%s",
              accountNumber, lbName));
    }
  }

  @Override
  public Map<String, String> filterInstanceStatus(final String accountNumber, final String lbName,
                                    final String servoInstanceId, final String encodedStatus)
          throws LoadBalancingActivityException {
    if (encodedStatus == null) {
      return Maps.newHashMap();
    }

    String monitoringZone = null;
    try {
      final LoadBalancerServoInstance servo =
              LoadBalancers.lookupServoInstance(servoInstanceId);
      monitoringZone = servo.getAvailabilityZone().getName();
    } catch (final Exception ex) {
      throw new LoadBalancingActivityException("Failed to lookup the servo instance with id="+servoInstanceId);
    }

    final Map<String, String> instanceToZone = Maps.newHashMap();
    final Set<String> stoppedInstances = Sets.newHashSet();
    try{
      final LoadBalancer lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      lb.getBackendInstances().stream().forEach ( instance -> instanceToZone.put(instance.getInstanceId(), instance.getPartition()));
      stoppedInstances.addAll(lb.getBackendInstances().stream()
              .filter(v -> LoadBalancerBackendInstanceStates.InstanceStopped.isInstanceState(v))
              .map(v -> v.getInstanceId())
              .collect(Collectors.toList())
      );
    }catch(final Exception ex) {
      throw new LoadBalancingActivityException("Failed to lookup the loadbalancer with name="+lbName);
    }

    final Set<String> validStates = Sets.newHashSet(
            LoadBalancerBackendInstance.STATE.InService.name(),
            LoadBalancerBackendInstance.STATE.OutOfService.name());

    final Map<String, String> instanceToStatus = Maps.newHashMap();
      try{
        final Map<String,String> statusMap = VmWorkflowMarshaller.unmarshalInstances(encodedStatus);
        for(final String instanceId : statusMap.keySet()) {
          final String instanceStatus = statusMap.get(instanceId);
          if (!validStates.contains(instanceStatus))
            continue;
          if (!(instanceToZone.containsKey(instanceId) &&
                  instanceToZone.get(instanceId).equals(monitoringZone)))
            continue;
          if (stoppedInstances.contains(instanceId))
            continue; // EUCA-11859: do not update health check result if instance stopped
          instanceToStatus.put(instanceId, instanceStatus);
        }
      }catch(final Exception ex) {
        throw new LoadBalancingActivityException("Failed unmarshalling instance status message", ex);
      }

    return instanceToStatus;
  }


  //TODO: SCALE
  @Override
  public void updateInstanceStatus(final String accountNumber, final String lbName,
                                   final Map<String,String> statusMap)
      throws LoadBalancingActivityException {
    // for each status, deserialize to Instance type
    // merge the results for each instance
    // update database
    final Set<String> validStatus = Sets.newHashSet(
        LoadBalancerBackendInstance.STATE.InService.name(),
        LoadBalancerBackendInstance.STATE.OutOfService.name());
    final Map<String,String> verifiedStatusMap = statusMap.entrySet().stream()
            .filter( entry -> validStatus.contains(entry.getValue()))
            .collect(Collectors.toMap(p -> p.getKey(), p->p.getValue() ));

    if(verifiedStatusMap.isEmpty())
      return;
    boolean updated = false;
    boolean committed = false;
    final int TRANSACTION_RETRY = 5;
    final LoadBalancer lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    for (int i = 1; i <= TRANSACTION_RETRY; i++) {
      try (final TransactionResource db = Entities.transactionFor(LoadBalancerBackendInstance.class)) {
        for (final String instanceId : verifiedStatusMap.keySet()) {
          final LoadBalancerBackendInstance sample = LoadBalancerBackendInstance.named(lb, instanceId);
          final LoadBalancerBackendInstance update = Entities.uniqueResult(sample);
          final String newStatus = verifiedStatusMap.get(instanceId);
          final LoadBalancerBackendInstance.STATE oldState =
                  update.getBackendState();
          final LoadBalancerBackendInstance.STATE newState =
                  LoadBalancerBackendInstance.STATE.valueOf(newStatus);
          if (!oldState.equals(newState))
            updated = true;
          update.setBackendState(newState);
          final LoadBalancerBackendInstanceStates failure =
                  LoadBalancerBackendInstanceStates.HealthCheckFailure;
          final LoadBalancerBackendInstanceStates success =
                  LoadBalancerBackendInstanceStates.HealthCheckSuccess;
          if (success.getState().equals(newState)) {
            update.setReasonCode(success.getReasonCode());
            update.setDescription(success.getDescription());
          } else if ( failure.getState().equals(newState)) {
            update.setReasonCode(failure.getReasonCode());
            update.setDescription(failure.getDescription());
          }
          update.updateInstanceStateTimestamp();
          Entities.persist(update);
        }
        db.commit();
      } catch (final Exception ex) {
        try {
          Thread.sleep((long) ((Math.random() * 100) * Math.pow(2, i)));
        }catch(final Exception ex2) {
          ;
        }
        continue;
      }
      committed = true;
      break;
    }

    if (!committed) {
      throw new LoadBalancingActivityException("Failed to persist instance status");
    }
    // if changed, updating loadbalancer will cause registering instances in the servo VMs
    if(updated) {
      LoadBalancingWorkflows.updateLoadBalancer(accountNumber, lbName);
    }
  }

  @Override
  public void putCloudWatchInstanceHealth(String accountNumber, String lbName)
      throws LoadBalancingActivityException {
    List<LoadBalancerBackendInstance> backendInstances = Lists.newArrayList();
    LoadBalancer lb = null;

    //TODO: SCALE
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      backendInstances= Lists.transform(Lists.newArrayList(lb.getBackendInstances()),
          LoadBalancerBackendInstanceEntityTransform.INSTANCE);
    }catch(final Exception ex) {
      LOG.error("failed to retrieve loadbalancer's backend instances", ex);
      return;
    }
    /// Update Cloudwatch
    for(final LoadBalancerBackendInstance backend: backendInstances){
      final String zoneName = backend.getAvailabilityZone().getName();
      if(backend.getState().equals(LoadBalancerBackendInstance.STATE.InService)){
        LoadBalancerCwatchMetrics.getInstance().updateHealthy(lb.getCoreView(), zoneName, backend.getInstanceId());
      }else if (backend.getState().equals(LoadBalancerBackendInstance.STATE.OutOfService)){
        LoadBalancerCwatchMetrics.getInstance().updateUnHealthy(lb.getCoreView(), zoneName, backend.getInstanceId());
      }
    }
  }

  @Override
  public void putCloudWatchMetrics(String accountNumber, String lbName,
      Map<String,String> metrics) throws LoadBalancingActivityException {

    if (metrics!= null) {
      for(final String instanceId : metrics.keySet()) {
        final String metric = metrics.get(instanceId);
        if (metric == null)
          continue;
        /// metric data from the servo VM
        final MetricData data =
            VmWorkflowMarshaller.unmarshalMetrics(metric);
        if(data.getMember()== null || data.getMember().size()<=0)
          continue;

        LoadBalancerZone zone = null;
        /// TODO: SCALE
        try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
          final LoadBalancerServoInstance sample =
              LoadBalancerServoInstance.named(instanceId);
          final LoadBalancerServoInstance entity =
              Entities.uniqueResult(sample);
          zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(entity.getAvailabilityZone());
        }catch(final Exception ex) {
          LOG.error("Failed to lookup servo instance named: " + instanceId);;
        }
        if (zone!=null) {
          try{
              LoadBalancerCwatchMetrics.getInstance().addMetric(zone, data);
            }catch(Exception ex){
              LOG.error("Failed to add ELB cloudwatch metric", ex);
            }
        }
      }
    }
  }

  @Override
  public List<String> listLoadBalancerPolicies(final String accountNumber, final String lbName) throws LoadBalancingActivityException {
    try {
      final LoadBalancer lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      final List<LoadBalancerListener> listeners = lb.getListeners().stream()
              .map(view -> LoadBalancerListenerEntityTransform.INSTANCE.apply(view))
              .collect(Collectors.toList());
      final List<LoadBalancerBackendServerDescription> backendServers =
              LoadBalancerBackendServers.getLoadBalancerBackendServerDescription(lb);

      final List<String> listenerPolicies = listeners.stream()
              .map(l -> l.getPolicies())
              .flatMap(p -> p.stream())
              .map(p -> p.getPolicyName())
              .distinct()
              .collect(Collectors.toList());

      final List<String> backendPolicies = backendServers.stream()
              .map(s -> s.getPolicyDescriptions())
              .flatMap(p -> p.stream())
              .map(p -> p.getPolicyName())
              .distinct()
              .collect(Collectors.toList());

      final List<String> publicKeyPolicies = lb.getPolicies().stream()
              .filter(p -> "PublicKeyPolicyType".equals(p.getPolicyTypeName()))
              .map(p -> p.getPolicyName())
              .distinct()
              .collect(Collectors.toList());
      final List<String> policies = Lists.newArrayList(listenerPolicies);
      policies.addAll(backendPolicies);
      policies.addAll(publicKeyPolicies);
      return policies.stream().distinct().collect(Collectors.toList());
    }catch(final Exception ex) {
      throw new LoadBalancingActivityException("Failed to lookup loadbalancer policies", ex);
    }
  }

  @Override
  public PolicyDescription getLoadBalancerPolicy(final String accountNumber, final String lbName, final String policyName)
          throws LoadBalancingActivityException {
    try {
      final LoadBalancer lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      final LoadBalancerPolicyDescription policy =
              LoadBalancerPolicies.getLoadBalancerPolicyDescription(lb, policyName);
      return LoadBalancerPolicies.AsPolicyDescription.INSTANCE.apply(policy);
    } catch(final Exception ex) {
      throw new LoadBalancingActivityException("Failed to lookup loadbalancer policies", ex);
    }
  }

  // TODO: SCALE
  @Override
  public Map<String, LoadBalancerServoDescription> lookupLoadBalancerDescription(final String accountNumber, final String lbName)
      throws LoadBalancingActivityException {
    final Map<String, LoadBalancerServoDescription> result = Maps.newHashMap();
    try{
      final LoadBalancer lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      for(final LoadBalancerZoneCoreView zoneView: lb.getZones()) {
        if (! LoadBalancerZone.STATE.InService.equals(zoneView.getState()))
          continue;

        final LoadBalancerServoDescription desc =
            LoadBalancers.getServoDescription(accountNumber, lbName, zoneView.getName());
        final LoadBalancerZone zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
        for(final LoadBalancerServoInstanceCoreView servoView : zone.getServoInstances()) {
          result.put(servoView.getInstanceId(), desc);
        }
      }
    } catch(final Exception ex) {
      throw new LoadBalancingActivityException("Failed to lookup loadbalancer descriptions", ex);
    }
    return result;
  }

  @Override
  public void deleteListenerRevokeSSLCertificatePolicy(String accountNumber,
      String lbName, List<Integer> portsToDelete) throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("could not find the loadbalancer", ex);
    }

    final Set<String> allArns = Sets.newHashSet();
    final Set<String> arnsToKeep = Sets.newHashSet();
    for(final LoadBalancerListenerCoreView listener : lb.getListeners()){
      final PROTOCOL protocol = listener.getProtocol();
      if(protocol.equals(PROTOCOL.HTTPS) || protocol.equals(PROTOCOL.SSL)) {
        allArns.add(listener.getCertificateId());
        if(! portsToDelete.contains(listener.getLoadbalancerPort())){
          arnsToKeep.add(listener.getCertificateId());
        }
      }
    }

    final Set<String> arnToDelete = Sets.difference(allArns, arnsToKeep);
    if(arnToDelete.size() <= 0)
      return;

    final String roleName = getRoleName(accountNumber, lbName);

    final String prefix =
        String.format("arn:aws:iam::%s:server-certificate", accountNumber);

    for (final String arn : arnToDelete){
      if(!arn.startsWith(prefix))
        continue;
      String pathAndName = arn.replace(prefix, "");
      String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);
      String policyName = String.format("%s-%s-%s-%s",
          SERVER_CERT_ROLE_POLICY_NAME_PREFIX,
          accountNumber,
          lbName,
          certName);
      try{
        EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, policyName, lb.useSystemAccount());
      }catch(final Exception ex){
        LOG.warn(String.format("Failed to delete role (%s) policy (%s)", roleName, policyName), ex);
      }
    }
  }

  @Override
  public void deleteListenerRevokeIngressRule(String accountNumber,
      String lbName, List<Integer> portsToDelete) throws LoadBalancingActivityException {
    LoadBalancer lb;
    String groupName = null;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      final LoadBalancerSecurityGroupCoreView group = lb.getGroup();
      if(group!=null)
        groupName = group.getName();
    }catch(Exception ex){
      throw new LoadBalancingActivityException("could not find the loadbalancer", ex);
    }

    if(groupName == null){
      return;
    }

    String[] protocols = new String[]{"tcp"}; /// Loadbalancer listeners protocols: HTTP, HTTPS, TCP, SSL -> all tcp
    for(String protocol : protocols){
      for(Integer port : portsToDelete){
        try{
          EucalyptusActivityTasks.getInstance().revokeSystemSecurityGroup( groupName, protocol, port, lb.useSystemAccount());
          LOG.debug(String.format("group rule revoked (%s-%d)", groupName, port));
        }catch(Exception ex){
          LOG.warn("Unable to revoke the security group", ex);
        }
      }
    }
  }

  @Override
  public List<String> disableAvailabilityZonesPersistRetiredServoInstances(
      String accountNumber, String lbName, List<String> zonesToDisable)
          throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      throw new LoadBalancingActivityException("Could not find the loadbalancer with name="+lbName, ex);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Error while looking for loadbalancer with name="+lbName, ex);
    }

    List<String> retiredInstances = Lists.newArrayList();
    final List<LoadBalancerZoneCoreView> currentZones = Lists.newArrayList(lb.getZones());
    for(final LoadBalancerZoneCoreView zoneView : currentZones){
      if(zonesToDisable.contains(zoneView.getName())){ // the zone will be disabled
        LoadBalancerZone zone;
        try{
          zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
        }catch(final Exception ex){
          LOG.error("unable to transform zone from the view", ex);
          continue;
        }
        for(final LoadBalancerServoInstanceCoreView instanceView : zone.getServoInstances()){
          LoadBalancerServoInstance instance;
          try{
            instance = LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(instanceView);
          }catch(final Exception ex){
            LOG.error("unable to transfrom servo-instance from the view", ex);
            continue;
          }

          try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
            final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
            update.setState(LoadBalancerServoInstance.STATE.Retired);
            update.setDnsState(LoadBalancerServoInstance.DNS_STATE.Deregistered);
            Entities.persist(update);
            db.commit();
            retiredInstances.add(update.getInstanceId());
          }catch(final NoSuchElementException ex){
            LOG.warn("Failed to update the servo instance's state: no such instance found");
          }catch(final Exception ex){
            LOG.warn("Failed to update the servo instance's state", ex);
          }
        }
      }
    }
    return retiredInstances;
  }


  @Override
  public void disableAvailabilityZonesPersistRetiredServoInstancesRollback(
      String accountNumber, String lbName, List<String> updatedInstanceIds) {
    if(updatedInstanceIds == null || updatedInstanceIds.size()<=0)
      return;

    for(final String instanceId : updatedInstanceIds){
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
        final LoadBalancerServoInstance sample = LoadBalancerServoInstance.named(instanceId);
        final LoadBalancerServoInstance update = Entities.uniqueResult(sample);
        update.setState(LoadBalancerServoInstance.STATE.InService);
        Entities.persist(update);
        db.commit();
      }catch(final NoSuchElementException ex){
        LOG.warn("Failed to update the servo instance's state: no such instance found");
      }catch(final Exception ex){
        LOG.warn("Failed to update the servo instance's state", ex);
      }
    }
  }


  @Override
  public List<String> disableAvailabilityZonesUpdateAutoScalingGroup(
      String accountNumber, String lbName, List<String> zonesToDisable)
          throws LoadBalancingActivityException {
    final List<String> updatedZones = Lists.newArrayList();
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      throw new LoadBalancingActivityException("Could not find the loadbalancer with name="+lbName, ex);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Error while looking for loadbalancer with name="+lbName, ex);
    }

    final Collection<LoadBalancerAutoScalingGroupCoreView> groups = lb.getAutoScaleGroups();
    for(final LoadBalancerAutoScalingGroupCoreView group : groups) {
      if (! zonesToDisable.contains(group.getAvailabilityZone()))
          continue;

      final String groupName = group.getName();
      final int capacity = 0;
      try{
        EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, null, capacity,
            lb.useSystemAccount());
      }catch(final Exception ex) {
        LOG.error("Failed to change the capacity of ELB's autoscaling group", ex);
      }

      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerAutoScalingGroup.class ) ){
        final LoadBalancerAutoScalingGroup update =
            Entities.uniqueResult(LoadBalancerAutoScalingGroup.named(lb, group.getAvailabilityZone()));
        update.setCapacity(capacity);
        Entities.persist(update);
        db.commit();
      }catch(NoSuchElementException ex){
        LOG.error("failed to find the autoscaling group record", ex);
      }catch(Exception ex){
        LOG.error("failed to update the autoscaling group record", ex);
      }
      updatedZones.add(group.getAvailabilityZone());
    }
    return updatedZones;
  }


  @Override
  public void disableAvailabilityZonesUpdateAutoScalingGroupRollback(
      String accountNumber, String lbName, List<String> updatedZones) {
    if(updatedZones == null || updatedZones.size()<=0)
      return;

    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      LOG.error("Could not find the loadbalancer with name="+lbName, ex);
      return;
    }catch(Exception ex){
      LOG.error("Error while looking for loadbalancer with name="+lbName, ex);
      return;
    }

    final Collection<LoadBalancerAutoScalingGroupCoreView> groups = lb.getAutoScaleGroups();
    for(final LoadBalancerAutoScalingGroupCoreView group : groups) {
      if (! updatedZones.contains(group.getAvailabilityZone()))
          continue;

      final String groupName = group.getName();
      final int capacity =  LoadBalancingServiceProperties.getCapacityPerZone();
      try{
        EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, null, capacity,
            lb.useSystemAccount());
      }catch(final Exception ex) {
        LOG.error("Failed to change the capacity of ELB's autoscaling group", ex);
      }

      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerAutoScalingGroup.class ) ){
        final LoadBalancerAutoScalingGroup update =
            Entities.uniqueResult(LoadBalancerAutoScalingGroup.named(lb, group.getAvailabilityZone()));
        update.setCapacity(capacity);
        Entities.persist(update);
        db.commit();
      }catch(NoSuchElementException ex){
        LOG.error("failed to find the autoscaling group record", ex);
      }catch(Exception ex){
        LOG.error("failed to update the autoscaling group record", ex);
      }
    }
  }

  @Override
  public void disableAvailabilityZonesPersistUpdatedZones(String accountNumber,
      String lbName, List<String> zonesToDisable)
          throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      throw new LoadBalancingActivityException("Could not find the loadbalancer with name="+lbName, ex);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Error while looking for loadbalancer with name="+lbName, ex);
    }

    if (zonesToDisable==null)
      return;
    for(final String zone : zonesToDisable) {
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerZone.class ) ) {
        final LoadBalancerZone update = Entities.uniqueResult(LoadBalancerZone.named(lb, zone));
        update.setState( LoadBalancerZone.STATE.OutOfService );
        db.commit();
      }catch(final Exception ex){
        LOG.debug( "Error updating state for load balancer zone", ex );
      }
    }
  }

  @Override
  public void disableAvailabilityZonesPersistBackendInstanceState(
      String accountNumber, String lbName, List<String> zonesToDisable)
          throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      throw new LoadBalancingActivityException("Could not find the loadbalancer with name="+lbName, ex);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Error while looking for loadbalancer with name="+lbName, ex);
    }

    if(zonesToDisable == null || zonesToDisable.size()<=0)
      return;

    for(final String removedZone : zonesToDisable){
      final LoadBalancerZone zone = LoadBalancers.findZone(lb, removedZone);
      for(final LoadBalancerBackendInstanceCoreView instance : zone.getBackendInstances()){
        try ( TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ){
          final LoadBalancerBackendInstance update = Entities.uniqueResult(
              LoadBalancerBackendInstance.named(lb, instance.getInstanceId()));
          final LoadBalancerBackendInstanceStates azDisabled =
                  LoadBalancerBackendInstanceStates.AvailabilityZoneDisabled;
          update.setState(azDisabled.getState());
          update.setReasonCode(azDisabled.getReasonCode());
          update.setDescription(azDisabled.getDescription());
          Entities.persist(update);
          db.commit();
        }catch(final NoSuchElementException ex){
          LOG.warn("failed to find the backend instance");
        }catch(final Exception ex){
          LOG.warn("failed to query the backend instance", ex);
        }
      }
    }
  }

  @Override
  public void deleteLoadBalancerDeactivateDns(String accountNumber,
      String lbName) {
    LoadBalancer lb;
    final List<LoadBalancerServoInstanceCoreView> servos = Lists.newArrayList();
    try{
      lb= LoadBalancers.getLoadbalancer(accountNumber, lbName);
      if(lb.getZones()!=null){
        for(final LoadBalancerZoneCoreView zoneView : lb.getZones()){
          LoadBalancerZone zone;
          try{
            zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
          }catch(final Exception ex){
            continue;
          }
          servos.addAll(zone.getServoInstances());
        }
      }
    }catch(NoSuchElementException ex){
      return;
    }catch(Exception ex){
      LOG.warn("Failed to find the loadbalancer", ex);
      return;
    }

    for(final LoadBalancerServoInstanceCoreView instance: servos){
      final String  address = instance.getAddress();
      if(address==null || address.length()<=0)
        continue;
      try{
        try ( final TransactionResource db =
            Entities.transactionFor(LoadBalancerServoInstance.class)){
          try{
            final LoadBalancerServoInstance entity =
                Entities.uniqueResult(LoadBalancerServoInstance.named(instance.getInstanceId()));
            entity.setDnsState(LoadBalancerServoInstance.DNS_STATE.Deregistered);
            Entities.persist(entity);
            db.commit();
          }catch(final Exception ex){
            LOG.error(String.format("failed to set servo instance(%s)'s dns state to deregistered",
                instance.getInstanceId()), ex);
          }
        }
      }catch(Exception ex){
        LOG.error("Error updating DNS registration state for balancer " + lbName, ex);
      }
    }
  }

  @Override
  public void deleteLoadBalancerDeleteScalingGroup(String accountNumber,
      String lbName) throws LoadBalancingActivityException {
    LoadBalancer lb;
    try{
      lb= LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      return;
    }catch(Exception ex){
      LOG.warn("Failed to find the loadbalancer named " + lbName, ex);
      return;
    }
    final Collection<LoadBalancerAutoScalingGroupCoreView> groups = lb.getAutoScaleGroups();
    if(groups == null || groups.isEmpty()){
      LOG.warn(String.format("Loadbalancer %s had no autoscale group associated with it", lb.getDisplayName()));
      return;
    }

    for (final LoadBalancerAutoScalingGroupCoreView group : groups) {
      final String groupName = group.getName();
      String launchConfigName = null;

      try{
        final DescribeAutoScalingGroupsResponseType resp =
            EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(groupName), lb.useSystemAccount());
        final AutoScalingGroupType asgType = resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().get(0);
        launchConfigName = asgType.getLaunchConfigurationName();
      }catch(final Exception ex){
        LOG.warn(String.format("Unable to find the launch config associated with %s", groupName));
      }

      try{
        EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(groupName, null, 0, lb.useSystemAccount());
      }catch(final Exception ex){
        LOG.warn(String.format("Unable to set desired capacity for %s", groupName), ex);
      }

      boolean error=false;
      final int NUM_DELETE_ASG_RETRY = 4;
      for(int i=0; i<NUM_DELETE_ASG_RETRY; i++){
        try{
          EucalyptusActivityTasks.getInstance().deleteAutoScalingGroup(groupName, true, lb.useSystemAccount());
          error = false;
          // will terminate all instances
        }catch(final Exception ex){
          error = true;
          LOG.warn(String.format("Failed to delete autoscale group (%d'th attempt): %s", (i+1), groupName));
          try{
            long sleepMs = (i+1) * 500;
            Thread.sleep(sleepMs);
          }catch(final Exception ex2){
          }
        }
        if(!error)
          break;
      }

      if(error){
        throw new LoadBalancingActivityException("Failed to delete autoscaling group; retry in a few seconds");
      }

      if(launchConfigName!=null){
        try{
          EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(launchConfigName, lb.useSystemAccount());
        }catch(Exception ex){
          LOG.warn("Failed to delete launch configuration " + launchConfigName, ex);
        }
      }

      LoadBalancerAutoScalingGroup scaleGroup = null;
      try{
        scaleGroup = LoadBalancerAutoScalingGroupEntityTransform.INSTANCE.apply(group);
      }catch(final Exception ex){
        LOG.error("falied to update servo instance record", ex);
      }

      if(scaleGroup==null)
        return;

      try ( TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
        for(final LoadBalancerServoInstanceCoreView instanceView : scaleGroup.getServos()){
          LoadBalancerServoInstance instance;
          try{
            instance=LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(instanceView);
          }catch(final Exception ex){
            continue;
          }
          final LoadBalancerServoInstance found = Entities.uniqueResult(instance);
          found.setAvailabilityZone(null);
          found.setAutoScalingGroup(null);
          // InService --> Retired
          // Pending --> Retired
          // OutOfService --> Retired
          // Error --> Retired
          found.setState(LoadBalancerServoInstance.STATE.Retired);
          Entities.persist(found);
        }
        db.commit();
      }catch(final Exception ex){
        LOG.error("Failed to update servo instance record", ex);
      }
    }
    // AutoScalingGroup record will be deleted later by clean-up workflow
  }

  @Override
  public void deleteLoadBalancerDeleteInstanceProfile(String accountNumber,
      String lbName) {
    final String instanceProfileName =  getInstanceProfileName(accountNumber, lbName);
    final String roleName = getRoleName(accountNumber, lbName);

    LoadBalancer lb = null;
    try{
      lb= LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      return;
    }catch(Exception ex){
      LOG.warn("Failed to find the loadbalancer named " + lbName, ex);
      return;
    }

    try{
       EucalyptusActivityTasks.getInstance().removeRoleFromInstanceProfile(instanceProfileName, roleName, lb.useSystemAccount());
    }catch(final Exception ex){
      LOG.error(String.format("Failed to remove role(%s) from the instance profile(%s)", roleName, instanceProfileName), ex);
    }

    // remove instance profile
    try{
       EucalyptusActivityTasks.getInstance().deleteInstanceProfile(instanceProfileName, lb.useSystemAccount());
    }catch(final Exception ex){
      LOG.error(String.format("Failed to delete instance profile (%s)", instanceProfileName), ex);
    }
  }

  @Override
  public void deleteLoadBalancerDeleteIamRole(String accountNumber,
      String lbName) {
    final String roleName = getRoleName(accountNumber, lbName);
    LoadBalancer lb = null;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(NoSuchElementException ex){
      return;
    }catch(Exception ex){
      LOG.warn("Failed to find the loadbalancer named " + lbName, ex);
      return;
    }
    List<String> rolePolicies = null;
    try{
      rolePolicies = EucalyptusActivityTasks.getInstance().listRolePolicies(roleName);
    }catch(final Exception ex){
      LOG.warn("Failed to list role policies to delete", ex);
    }
    if(rolePolicies != null) {
      for(final String policy : rolePolicies) {
        // delete role policy
        try{
          EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName,
              policy, lb.useSystemAccount());
        }catch(final Exception ex){
          LOG.error("Failed to delete role policy: " + policy, ex);
        }
      }
    }
    // delete role
    try{
      EucalyptusActivityTasks.getInstance().deleteRole(roleName, lb.useSystemAccount());
    }catch(final Exception ex){
      LOG.error("failed to delete role: " + roleName, ex);
    }
  }

  @Override
  public void deleteLoadBalancerDeleteSecurityGroup(String accountNumber,
      String lbName) {
    LoadBalancer lb;
    LoadBalancerSecurityGroupCoreView groupView = null;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
      if(lb.getGroup()!=null){
        groupView = lb.getGroup();
      }
    }catch(NoSuchElementException ex){
      return;
    }catch(Exception ex){
      LOG.error("Error while looking for loadbalancer with name="+lbName, ex);
      return;
    }

    if ( lb.getVpcId( ) == null ) {
      LoadBalancerSecurityGroup group;
      try {
        group = LoadBalancerSecurityGroupEntityTransform.INSTANCE.apply( groupView );
      } catch ( final Exception ex ) {
        LOG.error( "Erorr while looking for loadbalancer group", ex );
        return;
      }

      try ( TransactionResource db = Entities.transactionFor( LoadBalancerSecurityGroup.class ) ) {
        final LoadBalancerSecurityGroup update = Entities.uniqueResult( group );
        update.setLoadBalancer( null );  // this allows the loadbalancer to be deleted
        update.setState( LoadBalancerSecurityGroup.STATE.OutOfService );
        Entities.persist( update );
        db.commit();
      } catch ( Exception ex ) {
        LOG.warn( "Could not disassociate the group from loadbalancer" );
      }

      // the actual security group is delete during the clean-up workflow
    }
  }

  private static EucaS3Client getS3Client (final String roleName) throws AuthException {
    try {
      final Role lbRole = Accounts.lookupRoleByName(
              Accounts.lookupAccountIdByAlias( AccountIdentifiers.ELB_SYSTEM_ACCOUNT),
              roleName);
      final SecurityTokenAWSCredentialsProvider roleCredentialProvider =
              SecurityTokenAWSCredentialsProvider.forUserOrRole(Accounts.lookupPrincipalByRoleId(lbRole.getRoleId()));
      return EucaS3ClientFactory.getEucaS3Client(roleCredentialProvider);
    }catch (AuthException ex) {
      LOG.error("Failed to get credentials for loadbalancing role", ex);
    }catch (Exception ex) {
      LOG.error("Failed to get credentials for loadbalancing role", ex);
    }
    return null;
  }

  final String ACCESSLOG_ROLE_POLICY_NAME = "euca-internal-loadbalancer-vm-policy-accesslog";
  @Override
  public AccessLogPolicyActivityResult modifyLoadBalancerAttributesCreateAccessLogPolicy(
      final String accountNumber, final String lbName, final Boolean accessLogEnabled, final String s3BucketName,
      final String s3BucketPrefix, final Integer emitInterval)
          throws LoadBalancingActivityException {
    final String ACCESSLOG_ROLE_POLICY_DOCUMENT=
        "{\"Statement\":"
        + "[ {"
        + "\"Action\": [\"s3:PutObject\"],"
        + "\"Effect\": \"Allow\","
        + "\"Resource\": [\"arn:aws:s3:::BUCKETNAME_PLACEHOLDER/BUCKETPREFIX_PLACEHOLDER\"]"
        + "}]}";

    AccessLogPolicyActivityResult result = new AccessLogPolicyActivityResult();
    result.setShouldRollback(false);
    if (!accessLogEnabled)
      return result;

    final String bucketName = s3BucketName;
    final String bucketPrefix =
          com.google.common.base.Objects.firstNonNull(s3BucketPrefix, "");

    final String roleName = getRoleName(accountNumber, lbName);
    final String policyName = ACCESSLOG_ROLE_POLICY_NAME;
    try{
      final List<String> policies =
          EucalyptusActivityTasks.getInstance().listRolePolicies(roleName);
      if(policies.contains(policyName)){
        EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, policyName);
      }
    }catch(final Exception ex){
      ;
    }

    String policyDocument = ACCESSLOG_ROLE_POLICY_DOCUMENT.replace("BUCKETNAME_PLACEHOLDER", bucketName);
    if (bucketPrefix.length() > 0) {
      policyDocument = policyDocument.replace("BUCKETPREFIX_PLACEHOLDER", bucketPrefix+"/*");
    }else{
      policyDocument = policyDocument.replace("BUCKETPREFIX_PLACEHOLDER", "*");
    }

    try{
      EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, policyName, policyDocument);
      result.setRoleName(roleName);
      result.setPolicyName(policyName);
      result.setShouldRollback(true);
    }catch(final Exception ex){
      throw new LoadBalancingActivityException("failed to put role policy for loadbalancer vm's access to S3 buckets");
    }

    try {
      final EucaS3Client s3c = getS3Client(roleName);
      final String key = s3BucketPrefix != null && !s3BucketPrefix.isEmpty() ? String.format("%s/AWSLogs/%s/ELBAccessLogTestFile", s3BucketPrefix, accountNumber)
              : String.format("AWSLogs/%s/ELBAccessLogTestFile", accountNumber);
      final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
      final String content = String.format("Enable AccessLog for ELB: %s at %s",
              lbName, df.format(new Date()));
      final PutObjectRequest req = new PutObjectRequest(bucketName, key,
              new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), new ObjectMetadata())
              .withCannedAcl(CannedAccessControlList.BucketOwnerFullControl);
      s3c.putObject(req);
    } catch (final Exception ex) {
      LOG.warn("Failed to put test key to the access log bucket");
    }
    return result;
  }

  @Override
  public void modifyLoadBalancerAttributesCreateAccessLogPolicyRollback(
      final String accountNumber, final String lbName,
      final AccessLogPolicyActivityResult result) {
    if(!result.getShouldRollback())
      return;

    try{
      EucalyptusActivityTasks.getInstance().deleteRolePolicy(result.getRoleName(), result.getPolicyName());
    }catch(final Exception ex) {
      ;
    }
  }

  @Override
  public void modifyLoadBalancerAttributesDeleteAccessLogPolicy(
      final String accountNumber, final String lbName, final Boolean accessLogEnabled, final String s3BucketName,
      final String s3BucketPrefix, final Integer emitInterval)
          throws LoadBalancingActivityException {
    if (accessLogEnabled)
      return;

    final String roleName = getRoleName(accountNumber, lbName);
    final String policyName = ACCESSLOG_ROLE_POLICY_NAME;
    try{
      EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, policyName);
    }catch(final Exception ex) {
      ;
    }
  }

  @Override
  public void modifyLoadBalancerAttributesPersistAttributes(
      final String accountNumber, final String lbName, final Boolean accessLogEnabled, final String s3BucketName,
      final String s3BucketPrefix, Integer emitInterval)
          throws LoadBalancingActivityException {
    String bucketName = null;
    String bucketPrefix = null;
    if(accessLogEnabled) {
      bucketName = s3BucketName;
      bucketPrefix =
          com.google.common.base.Objects.firstNonNull(s3BucketPrefix, "");
      emitInterval =
         com.google.common.base.Objects.firstNonNull(emitInterval, 60);
    } else {
      bucketName = "";
      bucketPrefix = "";
      emitInterval = 60;
    }

    try ( final TransactionResource db = Entities.transactionFor(LoadBalancer.class) ) {
      final LoadBalancer lb = Entities.uniqueResult(
          LoadBalancer.namedByAccountId(accountNumber, lbName));
      lb.setAccessLogEnabled(accessLogEnabled);
      lb.setAccessLogEmitInterval(emitInterval);
      lb.setAccessLogS3BucketName(bucketName);
      lb.setAccessLogS3BucketPrefix(bucketPrefix);
      Entities.persist(lb);
      db.commit();
    }catch(final NoSuchElementException ex) {
      throw new LoadBalancingActivityException("No such loadbalancer is found");
    }catch(final Exception ex) {
      throw new LoadBalancingActivityException("Unknown error occured while saving entity", ex);
    }
  }

  private String lookupSecondaryNetworkInterface(final String instanceId) {
    try{
      final Optional<InstanceNetworkInterfaceSetItemType> optEni =
              LoadBalancingSystemVpcs.getUserVpcInterface(instanceId);
      if(optEni.isPresent()) {
        return optEni.get().getNetworkInterfaceId();
      }
      return null;
    }catch(final Exception ex) {
      LOG.error("Failed to lookup secondary network interface for instance: " + instanceId);
      return null;
    }
  }

  @Override
  public void modifyServicePropertiesValidateRequest(final String emi,
                                                           final String instanceType, final String keyname, final String initScript)
          throws LoadBalancingActivityException {
    if(emi!=null){
      try{
        final List<ImageDetails> images =
                EucalyptusActivityTasks.getInstance().describeImagesWithVerbose(Lists.newArrayList(emi));
        if(images == null || images.size()<=0)
          throw new LoadBalancingActivityException("No such EMI is found in the system");
        if(! images.get(0).getImageId().toLowerCase().equals(emi.toLowerCase()))
          throw new LoadBalancingActivityException("No such EMI is found in the system");
      }catch(final LoadBalancingActivityException ex){
        throw ex;
      }catch(final Exception ex){
        throw new LoadBalancingActivityException("Failed to verify EMI in the system");
      }
    }
    // validate instance type
    if(instanceType!=null){
      try{
        final List<VmTypeDetails> vmTypes =
                EucalyptusActivityTasks.getInstance().describeInstanceTypes(Lists.newArrayList(instanceType));
        if(vmTypes.size()<=0)
          throw new LoadBalancingActivityException("Invalid instance type -- " + instanceType);
      }catch(final LoadBalancingActivityException ex){
        throw ex;
      }catch(final Exception ex) {
        throw new LoadBalancingActivityException("Failed to verify instance type -- " + instanceType);
      }
    }
  }

  @Override
  public void applySecurityGroupUpdateSecurityGroup(final String accountNumber,
                                                    final String lbName, final Map<String, String> groupIdToNames)
          throws LoadBalancingActivityException {
    final LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer( accountNumber, lbName );
    }catch(NoSuchElementException ex){
      throw new LoadBalancingActivityException("Failed to find the loadbalancer " + lbName, ex);
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Unable to access loadbalancer metadata", ex);
    }

    for(final LoadBalancerAutoScalingGroupCoreView group : lb.getAutoScaleGroups()) {
      final String groupName = group.getName();
      final DescribeAutoScalingGroupsResponseType response =
              EucalyptusActivityTasks.getInstance().describeAutoScalingGroups( Lists.newArrayList( groupName ), lb.useSystemAccount() );
      final DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
              response.getDescribeAutoScalingGroupsResult();
      if ( describeAutoScalingGroupsResult != null ) {
        final AutoScalingGroupsType autoScalingGroupsType = describeAutoScalingGroupsResult.getAutoScalingGroups( );
        if ( autoScalingGroupsType != null &&
                autoScalingGroupsType.getMember( ) != null &&
                !autoScalingGroupsType.getMember( ).isEmpty( ) &&
                autoScalingGroupsType.getMember( ).get( 0 ).getInstances( ) != null ) {
          for ( final Instance instance : autoScalingGroupsType.getMember( ).get( 0 ).getInstances( ).getMember( ) ) {
            final String userVpcEni = lookupSecondaryNetworkInterface(instance.getInstanceId());
            if (userVpcEni == null) {
              throw new LoadBalancingActivityException("Failed to lookup user VPC network interface");
            }
            try {
              final List<String> sgroupIds = Lists.newArrayList(groupIdToNames.keySet());
              EucalyptusActivityTasks.getInstance().modifyNetworkInterfaceSecurityGroups(userVpcEni, sgroupIds);
            }catch(final Exception ex) {
              throw new LoadBalancingActivityException("Failed to set security groups to network interface", ex);
            }
          }
        }
      }
    }
  }


  @Override
  public void modifyServicePropertiesUpdateScalingGroup(final String emi,
      final String instanceType, final String keyname, final String initScript)
          throws LoadBalancingActivityException {
    final List<LoadBalancer> lbs = LoadBalancers.listLoadbalancers();
    for(final LoadBalancer lb : lbs){
      final Collection<LoadBalancerAutoScalingGroupCoreView> groups = lb.getAutoScaleGroups();
      for(final LoadBalancerAutoScalingGroupCoreView asg : groups) {
        if(asg==null || asg.getName()==null)
          continue;

        final String asgName = asg.getName();
        try{
          AutoScalingGroupType asgType = null;
          try{
            final DescribeAutoScalingGroupsResponseType resp =
                EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(asgName), lb.useSystemAccount());

            if(resp.getDescribeAutoScalingGroupsResult() != null &&
                resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups()!=null &&
                resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember()!=null &&
                resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().size()>0){
              asgType = resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember().get(0);
            }
          }catch(final Exception ex){
           throw new LoadBalancingActivityException("Failed to find the scaling group: "+asgName);
          }

          if(asgType!=null) {
            final LaunchConfigurationType lc =
                EucalyptusActivityTasks.getInstance().describeLaunchConfiguration(asgType.getLaunchConfigurationName(), lb.useSystemAccount());

            String launchConfigName;
            do{
              launchConfigName =
                  getLaunchConfigName( lb.getOwnerAccountNumber(),
                      lb.getDisplayName(), asg.getAvailabilityZone());
            }while(launchConfigName.equals(asgType.getLaunchConfigurationName()));

            final String newEmi = emi != null? emi : lc.getImageId();
            final String newType = instanceType != null? instanceType : lc.getInstanceType();
            String newKeyname = keyname != null ? keyname : lc.getKeyName();

            final String newUserdata = B64.standard.encString(String.format(
                "%s\n%s",
                getCredentialsString(),
                getLoadBalancerUserData(initScript, lb.getOwnerAccountNumber())));

            try{
              EucalyptusActivityTasks.getInstance().createLaunchConfiguration(newEmi, newType, lc.getIamInstanceProfile(),
                  launchConfigName, lc.getSecurityGroups().getMember(), newKeyname, newUserdata,
                  lc.getAssociatePublicIpAddress( ), lb.useSystemAccount() );
            }catch(final Exception ex){
              throw new LoadBalancingActivityException("Failed to create new launch config", ex);
            }
            try{
              EucalyptusActivityTasks.getInstance().updateAutoScalingGroup(asgName, null,asgType.getDesiredCapacity(), launchConfigName, lb.useSystemAccount());
            }catch(final Exception ex){
              throw new LoadBalancingActivityException("Failed to update the autoscaling group", ex);
            }
            try{
              EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(asgType.getLaunchConfigurationName(), lb.useSystemAccount());
            }catch(final Exception ex){
              LOG.warn("unable to delete the old launch configuration", ex);
            }
            // copy all tags from new image to ASG
            if ( emi != null ) {
              try {
                final List<ImageDetails> images =
                    EucalyptusActivityTasks.getInstance().describeImagesWithVerbose(Lists.newArrayList(emi));
                // image should exist at this point
                for(ResourceTag tag:images.get(0).getTagSet()){
                  EucalyptusActivityTasks.getInstance().createOrUpdateAutoscalingTags(tag.getKey(), tag.getValue(), asgName, lb.useSystemAccount());
                }
              } catch (final Exception ex) {
                LOG.warn("unable to propagate tags from image to ASG", ex);
              }
            }
            LOG.debug(String.format("autoscaling group '%s' was updated", asgName));
          }
        }catch(final Exception ex){
          throw new LoadBalancingActivityException("Failed to apply ELB property changes", ex);
        }
      } // for all autoscaling groups of LB
    } // for all LBs
  }


  private LoadBalancerServoInstance newInstance(final Instance instance, final LoadBalancerAutoScalingGroup group) throws Exception {
    final String instanceId = instance.getInstanceId();
    final LoadBalancerCoreView lbView = group.getLoadBalancer();
    LoadBalancer lb;
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
    if(sgroupView == null && lb.getVpcId()==null)
      throw new Exception("No security group is found for loadbalancer "+lb.getDisplayName());

    LoadBalancerZone zone;
    LoadBalancerSecurityGroup sgroup;
    try{
      zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
      sgroup = sgroupView == null ? null : LoadBalancerSecurityGroupEntityTransform.INSTANCE.apply(sgroupView);
    }catch(final Exception ex){
      LOG.error("Unable to transform entity", ex);
      throw ex;
    }

    // for faster inclusion into DNS response, update status as soon as servo is running
    String ipAddr = null;
    String privateIpAddr = null;
    try{
      List<RunningInstancesItemType> result = null;
      result = EucalyptusActivityTasks.getInstance().describeSystemInstancesWithVerbose(Lists.newArrayList(instance.getInstanceId()));
      if(result!=null && result.size()>0){
        ipAddr = result.get(0).getIpAddress();
        privateIpAddr = result.get(0).getPrivateIpAddress();
      }
    }catch(Exception ex){
      LOG.warn("failed to run describe-instances", ex);
    }

    final LoadBalancerServoInstance newInstance =
        LoadBalancerServoInstance.newInstance(zone, sgroup, group,
                Integer.parseInt(LoadBalancingWorkerProperties.EXPIRATION_DAYS), instanceId);
    if("Healthy".equals(instance.getHealthStatus()) &&
        "InService".equals(instance.getLifecycleState()))
      newInstance.setState(LoadBalancerServoInstance.STATE.InService);
    newInstance.setAddress(ipAddr);
    newInstance.setPrivateIp(privateIpAddr);
    if (!(ipAddr == null && privateIpAddr == null))
      newInstance.setDnsState(LoadBalancerServoInstance.DNS_STATE.Registered);
    return newInstance;
  }

  private void updateIpAddressesInVpc(final String instanceId) {
    final List<Optional<String>> userVpcInterfaceAddresses =
            LoadBalancingSystemVpcs.getUserVpcInterfaceIps(instanceId);
    if(userVpcInterfaceAddresses!=null) {
      final Optional<String> publicIp = userVpcInterfaceAddresses.get(0);
      final Optional<String> privateIp = userVpcInterfaceAddresses.get(1);
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
        final LoadBalancerServoInstance update =
                Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
        if(publicIp.isPresent())
          update.setAddress(publicIp.get());
        if(privateIp.isPresent())
          update.setPrivateIp(privateIp.get());
        Entities.persist(update);
        db.commit();
      }catch(final Exception ex) {
        LOG.error("Failed to update instance's IP addresses", ex);
      }
    }
  }

  @Override
  public void checkServoInstances() throws LoadBalancingActivityException {
    final int NUM_ASGS_TO_DESCRIBE = 8;
    // lookup all LoadBalancerAutoScalingGroup records
    List<LoadBalancerAutoScalingGroup> groups = Lists.newArrayList();
    Map<String, LoadBalancerAutoScalingGroup> allGroupMap = new ConcurrentHashMap<>();
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerAutoScalingGroup.class ) ) {
      groups = Entities.query(LoadBalancerAutoScalingGroup.named(), true);
      for(LoadBalancerAutoScalingGroup g : groups){
        allGroupMap.put(g.getName(), g);
      }
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Failed to query loadbalancer autoscaing groups", ex);
    }

    final Map<String, LoadBalancerAutoScalingGroup> groupToQuery = allGroupMap;
    // describe as group and find the unknown instance Ids
    List<AutoScalingGroupType> queriedGroups = Lists.newArrayList();

    for(final List<String> partition : Iterables.partition(groupToQuery.keySet(), NUM_ASGS_TO_DESCRIBE)) {
      try{
        DescribeAutoScalingGroupsResponseType response =
                EucalyptusActivityTasks.getInstance().describeAutoScalingGroupsWithVerbose(partition);
        DescribeAutoScalingGroupsResult result = response.getDescribeAutoScalingGroupsResult();
        AutoScalingGroupsType asgroups = result.getAutoScalingGroups();
        queriedGroups.addAll(asgroups.getMember());
      }catch(Exception ex){
        throw new LoadBalancingActivityException("Failed to describe autoscaling groups", ex);
      }
    }

    /// lookup all servoInstances in the DB
    Map<String, LoadBalancerServoInstance> servoMap = new ConcurrentHashMap<>();
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
      final List<LoadBalancerServoInstance> result = Entities.query(LoadBalancerServoInstance.named(), true);
      for(LoadBalancerServoInstance inst : result){
        servoMap.put(inst.getInstanceId(), inst);
      }
    }catch(Exception ex){
      throw new LoadBalancingActivityException("Failed to lookup existing servo instances in DB", ex);
    }

    /// for all found instances that's not in the servo instance DB
    ///     create servo record
    final List<LoadBalancerServoInstance> newServos = Lists.newArrayList();
    final Map<String, Instance> foundInstances = new ConcurrentHashMap<>();
    for(final AutoScalingGroupType asg : queriedGroups){
      Instances instances = asg.getInstances();
      if(instances!=null && instances.getMember() != null && instances.getMember().size() >0){
        for(final Instance instance : instances.getMember()){
          final String instanceId = instance.getInstanceId();
          foundInstances.put(instanceId, instance);
          if(!servoMap.containsKey(instanceId)){ /// new instance found
            try{
              final LoadBalancerAutoScalingGroup group= allGroupMap.get(asg.getAutoScalingGroupName());
              if(group==null)
                throw new IllegalArgumentException("The group with name "+ asg.getAutoScalingGroupName()+ " not found in the database");
              final LoadBalancerServoInstance newInstance = newInstance(instance, group);
              newServos.add(newInstance); /// persist later
            }catch(final Exception ex) {
              LOG.error("Failed to construct servo instance entity", ex);
              continue;
            }
          }
        }
      }
    }

    // CASE 1: NEW INSTANCES WITHIN THE AS GROUP FOUND
    if(newServos.size()>0){
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
        for(LoadBalancerServoInstance instance : newServos){
          Entities.persist(instance);
        }
        db.commit();
      }catch(Exception ex){
        LOG.error("Failed to persist the servo instance record", ex);
      }
      if (LoadBalancingSystemVpcs.isCloudVpc().isPresent()
              && LoadBalancingSystemVpcs.isCloudVpc().get()) {
        try {
          newServos.stream()
                  .filter(instance -> LoadBalancerServoInstance.STATE.InService.equals(
                          instance.getState()))
                  .forEach(instance -> LoadBalancingSystemVpcs.setupUserVpcInterface(instance.getInstanceId()));
        } catch (final Exception ex) {
          LOG.error("Failed to attach secondary network interface to ELB instances", ex);
        }
        try { // if servo is in VPC, update ip addresses using the secondary interface's address
          newServos.stream()
                  .filter(instance -> LoadBalancerServoInstance.STATE.InService.equals(
                          instance.getState()))
                  .forEach(instance -> {
                    updateIpAddressesInVpc(instance.getInstanceId());
                  });
        } catch (final Exception ex) {
          LOG.error("Failed to retrieve IP addresses of secondary network interface");
        }
      }
    }

    List<LoadBalancerServoInstanceCoreView> servoRecords = Lists.newArrayList();
    for(String groupName : groupToQuery.keySet()){
      final LoadBalancerAutoScalingGroup group = groupToQuery.get(groupName);
      servoRecords.addAll(group.getServos());
    }

    //final List<LoadBalancerServoInstance> registerDnsARec = Lists.newArrayList();
    for(LoadBalancerServoInstanceCoreView instanceView : servoRecords){
      /// CASE 2: EXISTING SERVO INSTANCES ARE NOT FOUND IN THE ASG QUERY RESPONSE
      if(! foundInstances.containsKey(instanceView.getInstanceId()) &&
              ! instanceView.getState().equals(LoadBalancerServoInstance.STATE.Retired)){
        LoadBalancerServoInstance instance;
        try{
          instance = LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(instanceView);
        }catch(final Exception ex){
          LOG.error("Failed to transform servo instance from the view", ex);
          continue;
        }
        try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
          final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
          update.setState(LoadBalancerServoInstance.STATE.Error);
          Entities.persist(update);
          db.commit();
        }catch(Exception ex){
          LOG.error(String.format("Failed to mark the servo instance's state to ERROR (%s)",
                  instance.getInstanceId()));
        }
      } else if (foundInstances.containsKey(instanceView.getInstanceId())) {
        /// CASE 3: INSTANCE STATE UPDATED
        Instance instanceCurrent = foundInstances.get(instanceView.getInstanceId());
        final String healthState = instanceCurrent.getHealthStatus();
        final String lifecycleState = instanceCurrent.getLifecycleState();
        LoadBalancerServoInstance.STATE curState = instanceView.getState();
        LoadBalancerServoInstance.STATE newState = curState;

        if(healthState != null && ! healthState.equals("Healthy")){
          newState = LoadBalancerServoInstance.STATE.Error;
        }else if (lifecycleState != null){
          switch ( lifecycleState ) {
            case "Pending":
              newState = LoadBalancerServoInstance.STATE.Pending;
              break;
            case "Quarantined":
              newState = LoadBalancerServoInstance.STATE.Error;
              break;
            case "InService":
              newState = LoadBalancerServoInstance.STATE.InService;
              break;
            case "Terminating":
            case "Terminated":
              newState = LoadBalancerServoInstance.STATE.OutOfService;
              break;
          }
        }

        if(!curState.equals(LoadBalancerServoInstance.STATE.Retired) &&
                !curState.equals(newState)){
          LoadBalancerServoInstance instance;
          try{
            instance = LoadBalancerServoInstanceEntityTransform.INSTANCE.apply(instanceView);
          }catch(final Exception ex){
            LOG.error("Failed to transform servo instance from the view", ex);
            continue;
          }
          try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
            final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
            update.setState(newState);
            Entities.persist(update);
            db.commit();
          }catch(Exception ex){
            LOG.error(String.format("Failed to commit the servo instance's state change (%s)",
                    instance.getInstanceId()));
          }
          if (LoadBalancerServoInstance.STATE.InService.equals(newState)) {
            try {
              if(LoadBalancingSystemVpcs.isCloudVpc().isPresent() &&
                      LoadBalancingSystemVpcs.isCloudVpc().get()) {
                LoadBalancingSystemVpcs.setupUserVpcInterface(instance.getInstanceId());
                updateIpAddressesInVpc(instance.getInstanceId());
              }
            }catch(final Exception ex) {
              LOG.error("Failed to attach secondary network interface to ELB instances", ex);
            }
          }
        }
      }
    }
  }

  // make sure  InService servo instance has its IP registered to DNS
  // also make sure Error or OutOfService servo instance has its IP deregistered from DNS
  @Override
  public void checkServoInstanceDns() throws LoadBalancingActivityException {
   /// determine the servo instances to query
    final List<LoadBalancerServoInstance> allInstances = Lists.newArrayList();
    //final List<LoadBalancerServoInstance> stateOutdated = Lists.newArrayList();
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
      allInstances.addAll(
          Entities.query(LoadBalancerServoInstance.named()));
    }catch(final Exception ex){
      throw new LoadBalancingActivityException("Failed to query servo instances in DB");
    }

    final List<LoadBalancerServoInstance> stateOutdated = allInstances;
    for(final LoadBalancerServoInstance instance : stateOutdated){
      if(LoadBalancerServoInstance.STATE.InService.equals(instance.getState())){
        if(!LoadBalancerServoInstance.DNS_STATE.Registered.equals(instance.getDnsState())){
          String ipAddr = null;
          String privateIpAddr = null;
          final Optional<Boolean> vpcTest = LoadBalancingSystemVpcs.isCloudVpc();
          if (vpcTest.isPresent() && vpcTest.get()) { /// in vpc mode
            final List<Optional<String>> userVpcInterfaceAddresses =
                    LoadBalancingSystemVpcs.getUserVpcInterfaceIps(instance.getInstanceId());
            if(userVpcInterfaceAddresses!=null) {
              final Optional<String> optPublicIp = userVpcInterfaceAddresses.get(0);
              final Optional<String> optPrivateIp = userVpcInterfaceAddresses.get(1);
              if(optPublicIp.isPresent())
                ipAddr = optPublicIp.get();
              if(optPrivateIp.isPresent())
                privateIpAddr = optPrivateIp.get();
            }
          }else { /// in classic mode
            if (instance.getAddress() == null) {
              try {
                List<RunningInstancesItemType> result = null;
                result = EucalyptusActivityTasks.getInstance().describeSystemInstancesWithVerbose(Lists.newArrayList(instance.getInstanceId()));
                if (result != null && result.size() > 0) {
                  ipAddr = result.get(0).getIpAddress();
                  privateIpAddr = result.get(0).getPrivateIpAddress();
                }
              } catch (Exception ex) {
                LOG.warn("Failed to run describe-instances", ex);
                continue;
              }
            } else {
              ipAddr = instance.getAddress();
              privateIpAddr = instance.getPrivateIp();
            }
          }

          try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
            final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
            update.setAddress(ipAddr);
            update.setPrivateIp(privateIpAddr);
            if (!(ipAddr == null && privateIpAddr == null) )
              update.setDnsState(LoadBalancerServoInstance.DNS_STATE.Registered);
            db.commit();
          }catch(NoSuchElementException ex){
            LOG.warn("Failed to find the servo instance named "+instance.getInstanceId(), ex);
          }catch(Exception ex){
            LOG.warn("Failed to update servo instance's ip address", ex);
          }
        }
      }else if (LoadBalancerServoInstance.STATE.OutOfService.equals(instance.getState()) ||
              LoadBalancerServoInstance.STATE.Error.equals(instance.getState())){
        if(!LoadBalancerServoInstance.DNS_STATE.Deregistered.equals(instance.getDnsState())){
          try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
            final LoadBalancerServoInstance update = Entities.uniqueResult(instance);
            update.setDnsState(LoadBalancerServoInstance.DNS_STATE.Deregistered);
            db.commit();
          }catch(NoSuchElementException ex){
            LOG.warn("Failed to find the servo instance named "+instance.getInstanceId(), ex);
          }catch(Exception ex){
            LOG.warn("Failed to update servo instance's ip address", ex);
          }
        }
      }
    }
  }

  @Override
  public void checkServoElasticIp() throws LoadBalancingActivityException {
    // EUCA-12956
    if(! LoadBalancingWorkerProperties.useElasticIp()) {
      return;
    }

    final List<LoadBalancerServoInstance> allInstances = Lists.newArrayList();
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
      allInstances.addAll(
              Entities.query(LoadBalancerServoInstance.named()));
    }catch(final Exception ex){
      throw new LoadBalancingActivityException("Failed to query servo instances in DB");
    }

    final Map<String, LoadBalancerServoInstance> inServiceInstances = allInstances.stream()
            .filter( vm -> LoadBalancerServoInstance.STATE.InService.equals(vm.getState()) )
            .filter( vm -> LoadBalancerServoInstance.DNS_STATE.Registered.equals(vm.getDnsState()) )
            .collect( Collectors.toMap( LoadBalancerServoInstance::getInstanceId, vm -> vm) );

    final Optional<Boolean> vpcTest = LoadBalancingSystemVpcs.isCloudVpc();
    for(final String instanceId : inServiceInstances.keySet()) {
      String ipAddr = null;
      String privateIpAddr = null;
      try {
        if (vpcTest.isPresent() && vpcTest.get()) { /// in vpc mode
          final List<Optional<String>> userVpcInterfaceAddresses =
                  LoadBalancingSystemVpcs.getUserVpcInterfaceIps(instanceId);
          if (userVpcInterfaceAddresses != null) {
            final Optional<String> optPublicIp = userVpcInterfaceAddresses.get(0);
            final Optional<String> optPrivateIp = userVpcInterfaceAddresses.get(1);
            if (optPublicIp.isPresent())
              ipAddr = optPublicIp.get();
            if (optPrivateIp.isPresent())
              privateIpAddr = optPrivateIp.get();
          }
        } else { /// in classic mode
          final List<RunningInstancesItemType> result =
                  EucalyptusActivityTasks.getInstance().describeSystemInstancesWithVerbose(
                          Lists.newArrayList(instanceId));
          if (result != null && result.size() > 0) {
            ipAddr = result.get(0).getIpAddress();
            privateIpAddr = result.get(0).getPrivateIpAddress();
          }
        }
      } catch (final Exception ex) {
        LOG.warn("Failed to describe loadbalancer worker instances", ex);
        continue;
      }

      // there's an external change in elastic or private IP
      if((ipAddr != null && !ipAddr.equals(inServiceInstances.get(instanceId).getAddress())) ||
              privateIpAddr!= null && !privateIpAddr.equals(inServiceInstances.get(instanceId).getPrivateIp())) {
        try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
          final LoadBalancerServoInstance update =
                  Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
          if (ipAddr!=null) {
            update.setAddress(ipAddr);
          }
          if (privateIpAddr!=null) {
            update.setPrivateIp(ipAddr);
          }
          db.commit();
        }catch(final NoSuchElementException ex){
          LOG.warn("Failed to find the servo instance named "+instanceId, ex);
        }catch(final Exception ex){
          LOG.warn("Failed to update servo instance's ip address", ex);
        }
      }
    }
  }

  /*
   * Note that the backend instance check does not affect the health check result of the instances.
   * the health check is left to the "ping" mechanism by the servo. the state update here is the mean
   * by which to include only the non-faulty instances in the list delivered to servo.
   */
  @Override
  public void checkBackendInstances() throws LoadBalancingActivityException {
    final int NUM_INSTANCES_TO_DESCRIBE = 8;

    /// determine backend instances to query (an instance can be registered to multiple ELBs)
    final Map<String, List<LoadBalancerBackendInstance>> allInstances = Maps.newHashMap();
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
      final List<LoadBalancerBackendInstance>  instances =
              Entities.query(LoadBalancerBackendInstance.named());
      for (final LoadBalancerBackendInstance instance : instances) {
        if(!allInstances.containsKey(instance.getInstanceId())) {
          allInstances.put(instance.getInstanceId(), Lists.newArrayList());
        }
        allInstances.get(instance.getInstanceId()).add(instance);
      }
    }catch(final Exception ex){
      throw new LoadBalancingActivityException("Failed to query backend instances", ex);
    }

    final List<RunningInstancesItemType> queryResult  = Lists.newArrayList();
    for(final List<String> partition : Iterables.partition(allInstances.keySet(), NUM_INSTANCES_TO_DESCRIBE)) {
      try{
        queryResult.addAll(
            EucalyptusActivityTasks.getInstance().describeSystemInstancesWithVerbose(partition));
      }catch(final Exception ex){
        LOG.warn("Failed to query instances", ex);
        break;
      }
    }

    //EUCA-9919: remove registered instances when terminated
    final Set<String> terminatedInstances =
        Sets.newHashSet();
    final Map<String, LoadBalancerBackendInstanceStates> stateMap =
            new HashMap<>();
    final Map<String, RunningInstancesItemType> runningInstances =
        new HashMap<String, RunningInstancesItemType>();
    for(final RunningInstancesItemType instance : queryResult){
      final String state = instance.getStateName();
      if("pending".equals(state))
        stateMap.put(instance.getInstanceId(), LoadBalancerBackendInstanceStates.InitialRegistration);
      else if("running".equals(state)){
        runningInstances.put(instance.getInstanceId(), instance);
      }else if("shutting-down".equals(state))
        stateMap.put(instance.getInstanceId(), LoadBalancerBackendInstanceStates.InstanceInvalidState);
      else if("terminated".equals(state)) {
        stateMap.put(instance.getInstanceId(), LoadBalancerBackendInstanceStates.InstanceInvalidState);
        terminatedInstances.add(instance.getInstanceId());
      }else if("stopping".equals(state))
        stateMap.put(instance.getInstanceId(), LoadBalancerBackendInstanceStates.InstanceStopped);
      else if("stopped".equals(state))
        stateMap.put(instance.getInstanceId(), LoadBalancerBackendInstanceStates.InstanceStopped);
    }

    final Set<LoadBalancerBackendInstance> backendsToDelete = Sets.newHashSet();
    for(final String instanceId : allInstances.keySet()) {
      for (final LoadBalancerBackendInstance be : allInstances.get(instanceId)) {
        if (terminatedInstances.contains(instanceId)) { // case 1: instance terminated
          backendsToDelete.add(be);
          continue;
        }
        if (stateMap.containsKey(instanceId)) { // case 2: instance not in running state
          try (final TransactionResource db = Entities.transactionFor(LoadBalancerBackendInstance.class)) {
            final LoadBalancerBackendInstanceStates trueState = stateMap.get(be.getInstanceId());
            final LoadBalancerBackendInstance update = Entities.uniqueResult(be);
            update.setBackendState(trueState.getState());
            update.setReasonCode(trueState.getReasonCode());
            update.setDescription(trueState.getDescription());
            Entities.persist(update);
            db.commit();
          } catch (final Exception ex) {
            ;
          }
        } else if (runningInstances.containsKey(instanceId)) { // case 3: instance running
          // case 3.a: check if instance was re-started (EUCA-11859)
          if (LoadBalancerBackendInstanceStates.InstanceStopped.isInstanceState(be)) {
            final LoadBalancerBackendInstanceStates registration = LoadBalancerBackendInstanceStates.InitialRegistration;
            try (final TransactionResource db = Entities.transactionFor(LoadBalancerBackendInstance.class)) {
              final LoadBalancerBackendInstance update = Entities.uniqueResult(be);
              update.setBackendState(registration.getState());
              update.setReasonCode(registration.getReasonCode());
              update.setDescription(registration.getDescription());
              Entities.persist(update);
              db.commit();
            } catch (final Exception ex) {
              ;
            }
          }

          // case 3.b: check instance's IP address change
          String instanceIpAddress = null;
          if (be.getLoadBalancer().getVpcId() == null)
            instanceIpAddress = runningInstances.get(instanceId).getIpAddress();
          else
            instanceIpAddress = runningInstances.get(instanceId).getPrivateIpAddress();
          if (instanceIpAddress == null) {
            LOG.warn(String.format("Failed to determine ELB backend instance's IP address: %s",
                    instanceId));
          } else if (!instanceIpAddress.equals(be.getIpAddress())) {
            try (final TransactionResource db = Entities.transactionFor(LoadBalancerBackendInstance.class)) {
              final LoadBalancerBackendInstance update = Entities.uniqueResult(be);
              update.setIpAddress(instanceIpAddress);
              update.setPartition(runningInstances.get(instanceId).getPlacement());
              Entities.persist(update);
              db.commit();
            } catch (final Exception ex) {
              ;
            }
          }
        }
      }
    }

    for(final LoadBalancerBackendInstance be : backendsToDelete) {
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerBackendInstance.class ) ) {
        final LoadBalancerBackendInstance entity = Entities.uniqueResult(be);
        Entities.delete(entity);
        LOG.info("Instance "+be.getInstanceId()+" is terminated and removed from ELB");
        db.commit();
      }catch(final Exception ex) {
        ;
      }
    }

    /// mark outdated instances as Error
    final int HealthUpdateTimeoutSec = 3 * MAX_HEALTHCHECK_INTERVAL_SEC; /// 6 minutes
    final Predicate<LoadBalancerBackendInstance> unreachableLoadbalancer =
            (instance) -> {
              if (LoadBalancerBackendInstanceStates.UnrechableLoadBalancer.isInstanceState(instance))
                return false;
              if (LoadBalancerBackendInstanceStates.InstanceStopped.isInstanceState(instance))
                return false;
              final long currentTime = System.currentTimeMillis();
              Date lastUpdated = instance.instanceStateLastUpdated();
              if (lastUpdated == null)
                lastUpdated = instance.getCreationTimestamp();
              final int diffSec = (int) ((currentTime - lastUpdated.getTime()) / 1000.0);
              return diffSec > HealthUpdateTimeoutSec;
            };

    final Set<LoadBalancerBackendInstance> outdatedInstances =
            allInstances.values().stream()
                    .flatMap(Collection::stream)
                    .filter(v -> !backendsToDelete.contains(v)) // DB records deleted already
                    .filter(v -> unreachableLoadbalancer.apply(v))
                    .collect(Collectors.toSet());

    if (! outdatedInstances.isEmpty()) {
      final LoadBalancerBackendInstanceStates unreachable = LoadBalancerBackendInstanceStates.UnrechableLoadBalancer;
      try (TransactionResource db = Entities.transactionFor(LoadBalancerBackendInstance.class)) {
        for (final LoadBalancerBackendInstance instance : outdatedInstances) {
          final LoadBalancerBackendInstance update = Entities.uniqueResult(instance);
          update.setState(unreachable.getState());
          update.setReasonCode(unreachable.getReasonCode());
          update.setDescription(unreachable.getDescription());
          Entities.persist(update);
        }
        db.commit();
      } catch (final Exception ex) {
        ;
      }
    }
  }

  @Override
  public void cleanupSecurityGroups() throws LoadBalancingActivityException {
  /// find all security group whose member instances are empty
    List<LoadBalancerSecurityGroup> allGroups = null;
    try ( TransactionResource db = Entities.transactionFor( LoadBalancerSecurityGroup.class ) ) {
      allGroups = Entities.query(LoadBalancerSecurityGroup.withState(LoadBalancerSecurityGroup.STATE.OutOfService));
    }catch(Exception ex){ /* retry later */ }
    if(allGroups==null || allGroups.size()<=0)
      return;
    final List<LoadBalancerSecurityGroup> toDelete = Lists.newArrayList();
    for(LoadBalancerSecurityGroup group : allGroups){
      Collection<LoadBalancerServoInstanceCoreView> instances = group.getServoInstances();
      if(instances == null || instances.size()<=0)
        toDelete.add(group);
    }

    /// delete them from euca
    for(final LoadBalancerSecurityGroup group : toDelete){
      boolean deleted = false;
      try{
        final List<SecurityGroupItemType> existingGroups =
            EucalyptusActivityTasks.getInstance().describeSystemSecurityGroups( Lists.newArrayList(group.getName()), true);
        if(existingGroups == null || existingGroups.size()<=0)
          deleted =true;
        else {
          EucalyptusActivityTasks.getInstance().deleteSystemSecurityGroup( group.getName(), true);
          LOG.info("Deleted security group: "+group.getName());
          deleted = true;
        }
      }catch(final Exception ex){
        try{
          final List<SecurityGroupItemType> existingGroups =
              EucalyptusActivityTasks.getInstance().describeSystemSecurityGroups( Lists.newArrayList(group.getName()), false);
          if(existingGroups == null || existingGroups.size()<=0)
            deleted =true;
          else{
            EucalyptusActivityTasks.getInstance().deleteSystemSecurityGroup( group.getName(), false);
            LOG.info("Deleted security group: "+group.getName());
            deleted = true;
          }
        }catch(final Exception ex2) {
          LOG.warn("Failed to delete the security group from eucalyptus",ex2);
        }
      }
      if (deleted) {
        try ( final TransactionResource db = Entities.transactionFor( LoadBalancerSecurityGroup.class ) ) {
          final LoadBalancerSecurityGroup g =  Entities.uniqueResult(group);
          Entities.delete(g);
          db.commit();
        }catch(NoSuchElementException ex){
          ;
        }catch(Exception ex){
          LOG.warn("Failed to delete the securty group records from database", ex);
        }
      }
    }
  }

  @Override
  public void cleanupServoInstances() throws LoadBalancingActivityException {
 // find all OutOfService instances
    List<LoadBalancerServoInstance> retired=null;
    try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
      LoadBalancerServoInstance sample =
          LoadBalancerServoInstance.withState(LoadBalancerServoInstance.STATE.Retired.name());
      retired = Entities.query(sample);
      sample =  LoadBalancerServoInstance.withState(LoadBalancerServoInstance.STATE.Error.name());
      retired.addAll(Entities.query(sample));
    }catch(Exception ex){
      LOG.warn("Failed to query loadbalancer servo instance", ex);
    }

    if(retired == null || retired.size()<=0)
      return;
    /// for each:
    // describe instances
    final List<String> param = Lists.newArrayList();
    final Map<String, String> latestState = Maps.newHashMap();
    for(final LoadBalancerServoInstance instance : retired){
      ///   call describe instance
      String instanceId = instance.getInstanceId();
      if(instanceId == null)
        continue;
      param.clear();
      param.add(instanceId);
      String instanceState;
      try{
        List<RunningInstancesItemType> result =null;
        result = EucalyptusActivityTasks.getInstance().describeSystemInstancesWithVerbose(param);
        if (result.isEmpty())
          instanceState= "terminated";
        else
          instanceState = result.get(0).getStateName();
      }catch(final Exception ex){
        LOG.warn("Failed to query instances", ex);
        continue;
      }
      latestState.put(instanceId, instanceState);
    }

    // if state==terminated or describe instances return no result,
    //    delete the database record
    for(String instanceId : latestState.keySet()){
      String state = latestState.get(instanceId);
      if(state.equals("terminated")){
        try ( final TransactionResource db2 = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
          LoadBalancerServoInstance toDelete = Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
          Entities.delete(toDelete);
          db2.commit();
        }catch(Exception ex){
          LOG.warn( "Unable to delete load balancer servo instance: ", ex );
        }
      }
    }
  }

  @Override
  public   void recycleFailedServoInstances() throws LoadBalancingActivityException {
    /// when SWF activity fails on the VM more than the threshold (property), the VM is terminated
    /// and the autoscaling group replaces it with the new VM.
    /// The terminated Vms will be cleaned up later by checkServoInstances() and cleanupServoInstances()
    final int failureThreshold = Integer.parseInt(LoadBalancingWorkerProperties.FAILURE_THRESHOLD_FOR_RECYCLE);
    if (failureThreshold <= 0) {
      return;
    }
    try{
      final List<LoadBalancerServoInstance> inServiceInstances = Lists.newArrayList();
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
        LoadBalancerServoInstance sample =
                LoadBalancerServoInstance.withState(LoadBalancerServoInstance.STATE.InService.name());
        inServiceInstances.addAll(Entities.query(sample));
      }
      final Date oneHourAgo = new Date(System.currentTimeMillis() - (1 * 60 * 60 * 1000));
      final List<String> unhealthyInstances = inServiceInstances.stream()
              .filter(vm ->
                      vm.getActivityFailureCount() >= failureThreshold)
              .map(vm -> vm.getInstanceId())
              .collect(Collectors.toList());
      final List<String> newInstances = inServiceInstances.stream()
              .filter(vm -> vm.getActivityFailureUpdateTime() == null)
              .map(vm -> vm.getInstanceId())
              .collect(Collectors.toList());
      final List<String> temporallyFailedInstances = inServiceInstances.stream()
              .filter(vm ->
                      vm.getActivityFailureCount() < failureThreshold && vm.getActivityFailureCount() > 0
                      && (vm.getActivityFailureUpdateTime()!=null && vm.getActivityFailureUpdateTime().before(oneHourAgo)))
              .map(vm -> vm.getInstanceId())
              .collect(Collectors.toList());

      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
        for (final String instanceId : newInstances) {
          final LoadBalancerServoInstance update =
                  Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
          update.setActivityFailureUpdateTime(new Date(System.currentTimeMillis()));
          Entities.persist(update);
        }
        for (final String instanceId : temporallyFailedInstances) {
          final LoadBalancerServoInstance update =
                  Entities.uniqueResult(LoadBalancerServoInstance.named(instanceId));
          update.setActivityFailureCount(0);
          update.setActivityFailureUpdateTime(new Date(System.currentTimeMillis()));
          Entities.persist(update);
        }
        db.commit();
      }

      for (final String instanceId : unhealthyInstances) {
       EucalyptusActivityTasks.getInstance().terminateInstances(Lists.newArrayList(instanceId));
        LOG.debug(String.format("Unhealthy loadbalancer VM is detected and terminated (%s)", instanceId));
      }
    } catch(final Exception ex) {
      LOG.error("Failed to recycle unhealthy worker VMs", ex);
    }
  }

  @Override
  public void runContinousWorkflows() throws LoadBalancingActivityException {
    List<LoadBalancer> loadbalancers = null;
    try{
      loadbalancers = LoadBalancers.listLoadbalancers();
    }catch(final Exception ex) {
      LOG.error("Failed to list all loadbalancers", ex);
      return;
    }
    for(final LoadBalancer lb : loadbalancers) {
      final String accountId = lb.getOwnerAccountNumber();
      final String lbName = lb.getDisplayName();
      try{
        LoadBalancingWorkflows.runUpdateLoadBalancer(accountId, lbName);
        LoadBalancingWorkflows.runInstanceStatusPolling(accountId, lbName);
        LoadBalancingWorkflows.runCloudWatchPutMetric(accountId, lbName);
      }catch(final Exception ex) {
        LOG.error("Failed to run continous workflows for loadbalancers", ex);
      }
    }
  }

  @Override
  public List<String> lookupServoInstances(final String accountNumber,
      final String lbName) throws LoadBalancingActivityException {
    final LoadBalancer lb;
    try{
      lb = LoadBalancers.getLoadbalancer(accountNumber, lbName);
    }catch(final Exception ex) {
      throw new LoadBalancingActivityException("Failed to lookup loadbalancer");
    }

    final List<String> instances = Lists.newArrayList();
    try{
      for(final LoadBalancerZoneCoreView zoneView : lb.getZones()) {
        if(LoadBalancerZone.STATE.OutOfService.equals(zoneView.getState()))
          continue;

        final LoadBalancerZone zone = LoadBalancerZoneEntityTransform.INSTANCE.apply(zoneView);
        instances.addAll(Collections2.transform(zone.getServoInstances(),
            new Function<LoadBalancerServoInstanceCoreView, String>(){
          @Override
          public String apply(LoadBalancerServoInstanceCoreView arg0) {
            return arg0.getInstanceId();
          }
        }));
      }
    }catch(final Exception ex) {
      throw new LoadBalancingActivityException("Failed to lookup servo instance records", ex);
    }

    return instances;
  }


  // to make sure that all ELB VMs have the right role policy
  @Override
  public void upgrade4_4() throws LoadBalancingActivityException {
    final List<LoadBalancer> oldLBs =
            LoadBalancers.listLoadbalancers().stream()
                    .filter( lb -> !LoadBalancers.v4_4_0.apply(lb) )
                    .collect(Collectors.toList());

    for (final LoadBalancer lb : oldLBs) {
      final String accountNumber = lb.getOwnerAccountNumber();
      final String lbName = lb.getDisplayName();
      final String roleName = getRoleName(accountNumber, lbName);

      try {
        GetRolePolicyResult policy = null;
        final List<String> policies = EucalyptusActivityTasks.getInstance().listRolePolicies(roleName);
        if (policies.contains(SERVO_ROLE_POLICY_NAME)) {
          policy = EucalyptusActivityTasks.getInstance().getRolePolicy(roleName, SERVO_ROLE_POLICY_NAME);
        }

        final boolean policyAllowsSwf = true ? policy != null && SERVO_ROLE_POLICY_DOCUMENT.toLowerCase().equals(policy.getPolicyDocument().toLowerCase()) : false;
        if (!policyAllowsSwf) {
          if (policy != null) {
            EucalyptusActivityTasks.getInstance().deleteRolePolicy(roleName, policy.getPolicyName());
          }
          EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, SERVO_ROLE_POLICY_NAME, SERVO_ROLE_POLICY_DOCUMENT);
          LOG.info(String.format("IAM role policy was updated for loadbalancer (%s-%s)", accountNumber, lbName));
        }
      } catch (final Exception ex) {
        LOG.warn(String.format("Failed to upgrade old loadbalancer (%s-%s) to 4.4", accountNumber, lbName), ex);
      }
    }
  }

  @Override
  public void recordInstanceTaskFailure(final String instanceId) throws LoadBalancingActivityException {
    try {
      try ( final TransactionResource db = Entities.transactionFor( LoadBalancerServoInstance.class ) ) {
        final LoadBalancerServoInstance sample =
                LoadBalancerServoInstance.named(instanceId);
        final LoadBalancerServoInstance entity =
                Entities.uniqueResult(sample);
        entity.setActivityFailureCount(entity.getActivityFailureCount() + 1);
        Entities.persist(entity);
        db.commit();
      }
    }catch(final Exception ex) {
       LOG.warn(String.format("Failed to mark the VM (%s) as failed", instanceId), ex);
    }
  }
}
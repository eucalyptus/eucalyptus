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
package com.eucalyptus.imaging.worker;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.euare.GetRolePolicyResult;
import com.eucalyptus.auth.euare.InstanceProfileType;
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.TagDescription;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.imaging.Imaging;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.ucsb.eucalyptus.msgs.ClusterInfoType;
import edu.ucsb.eucalyptus.msgs.DescribeKeyPairsResponseItemType;
import edu.ucsb.eucalyptus.msgs.ImageDetails;
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType;
import edu.ucsb.eucalyptus.msgs.SecurityGroupItemType;
import edu.ucsb.eucalyptus.msgs.TagInfo;

/**
 * @author Sang-Min Park
 *
 */
public class ImagingServiceActions {

    public static String CREDENTIALS_STR = "euca-"+B64.standard.encString("setup-credential");
    private static Logger  LOG = Logger.getLogger( ImagingServiceActions.class );

    // make sure the cloud is ready to launch imaging service instances
    // e.g., lack of resources will keep the launcher from creating resources
    public static class AdmissionControl extends AbstractAction {
      public AdmissionControl(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup,
          String groupId) {
        super(lookup, groupId);
      }

      @Override
      public boolean apply() throws ImagingServiceActionException{
        // check if there is an ASG for the imaging service
        boolean asgFound = false;
        try{
          final DescribeAutoScalingGroupsResponseType resp = 
              EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(null);
          final List<AutoScalingGroupType> groups =
              resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
          for(final AutoScalingGroupType asg : groups){
            if(asg.getAutoScalingGroupName().contains(this.getGroupId())){
              asgFound = true;
              break;
            } 
          }
        }catch(final Exception ex){
          asgFound = false;
        }
        
        if(asgFound)
          return false; // this will stop the whole instance launch chain
        
        final String emi = ImagingServiceProperties.IMAGING_WORKER_EMI;
        List<ImageDetails> images = null;
        try{
          images = EucalyptusActivityTasks.getInstance().describeImages(Lists.newArrayList(emi), false);
          if(images==null || images.size()<=0 ||! images.get(0).getImageId().toLowerCase().equals(emi.toLowerCase()))
            throw new ImagingServiceActionException("No imaging service EMI is found");
        }catch(final ImagingServiceActionException ex){
          throw ex;
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to validate the imaging service EMI", ex);
        }
        
        List<ClusterInfoType> clusters = null;
        try{
          clusters = EucalyptusActivityTasks.getInstance().describeAvailabilityZones(true);
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to validate the zones", ex);
        }
      
        // are there enough resources?
        final String instanceType = ImagingServiceProperties.IMAGING_WORKER_INSTANCE_TYPE;
        int numVm = 1;
        boolean resourceAvailable= false;
        for(final ClusterInfoType cluster : clusters){
          final int capacity = findAvailableResources(clusters,  cluster.getZoneName(), instanceType);
          if(numVm<=capacity){
            resourceAvailable = true;
            break;
          }
        }
        
        if(!resourceAvailable)
          throw new ImagingServiceActionException("not enough resource in the cloud");
        
        // check if the keyname is configured and exists
        final String keyName = ImagingServiceProperties.IMAGING_WORKER_KEYNAME;
        if(keyName!=null && keyName.length()>0){
          try{
            final List<DescribeKeyPairsResponseItemType> keypairs = 
                EucalyptusActivityTasks.getInstance().describeKeyPairs(Lists.newArrayList(keyName));
            if(keypairs==null || keypairs.size()<=0 || !keypairs.get(0).getKeyName().equals(keyName))
              throw new Exception();
          }catch(Exception ex){
            throw new ImagingServiceActionException(String.format("The configured keyname %s is not found", 
                ImagingServiceProperties.IMAGING_WORKER_KEYNAME));
          }
        }
        
        return true;
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
      public void rollback() {
        ;
      }

      @Override
      public String getResult() {
        return null;
      }
    }
    
    public static class SecurityGroupSetup extends AbstractAction {
      private String createdGroup = null;
      private String createdGroupId = null;
      private String groupOwnerAccountId = null;
    
      public SecurityGroupSetup(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId) {
        super(lookup, groupId);
      }

      @Override
      public boolean apply() throws ImagingServiceActionException {
        final String groupName = String.format("euca-internal-imaging-%s", this.getGroupId());
        final String groupDesc = String.format("group for imaging service workers");
        
        // check if there's an existing group with the same name
        boolean groupFound = false;
        try{
          List<SecurityGroupItemType> groups = EucalyptusActivityTasks.getInstance().describeSecurityGroups(Lists.newArrayList(groupName));
          if(groups!=null && groups.size()>0){
            final SecurityGroupItemType current = groups.get(0);
            if(groupName.equals(current.getGroupName())){
              groupFound=true;
              this.createdGroup = groupName;
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
            throw new ImagingServiceActionException("Failed to create the security group for imaging service", ex);
          }
        }else{
          createdGroup = groupName;
        }
        
        if(this.createdGroup == null || this.groupOwnerAccountId == null)
          throw new ImagingServiceActionException("Failed to create the security group for loadbalancer");
       
        return true;
      }
      
      @Override
      public void rollback() throws ImagingServiceActionException {
        if(this.createdGroup == null)
          return;
        try{
          EucalyptusActivityTasks.getInstance().deleteSecurityGroup(this.createdGroup);
          this.createdGroup = null;
          this.createdGroupId = null;
          this.groupOwnerAccountId = null;
        }catch(Exception ex){
          /// this will fail if the ASG already launched instances and the instances are not terminated
          ; 
        }
      }

      @Override
      public String getResult() {
        return this.createdGroup;
      }
    }
    
    
    public static class IamRoleSetup extends AbstractAction {
      private static final String DEFAULT_ROLE_PATH_PREFIX = "/internal/imaging";
      private static final String DEFAULT_ROLE_NAME = "imaging";
      private static final String DEFAULT_ASSUME_ROLE_POLICY =
          "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":[\"ec2.amazonaws.com\"]},\"Action\":[\"sts:AssumeRole\"]}]}";
      private RoleType role = null;

      public IamRoleSetup(Function<Class<? extends AbstractAction>, AbstractAction> lookup,  final String groupId){
        super(lookup, groupId);
      }

      @Override
      public boolean apply() throws ImagingServiceActionException{
        final String rolePath = DEFAULT_ROLE_PATH_PREFIX;
        final String roleName = String.format("%s-%s",DEFAULT_ROLE_NAME, this.getGroupId());
        final String assumeRolePolicy = DEFAULT_ASSUME_ROLE_POLICY;
                
        // list-roles.
        try{
          List<RoleType> result = EucalyptusActivityTasks.getInstance().listRoles(rolePath);
          if(result != null){
            for(RoleType r : result){
              if(roleName.equals(r.getRoleName())){
                role = r;
                break;
              } 
            }
          }
        }catch(Exception ex){
          throw new ImagingServiceActionException("Failed to list IAM roles", ex);
        }

        // if no role found, create a new role with assume-role policy for elb
        if(role==null){ /// create a new role
          try{
            role = EucalyptusActivityTasks.getInstance().createRole(roleName, rolePath, assumeRolePolicy);
          }catch(Exception ex){
            throw new ImagingServiceActionException("Failed to create the role for Imaging Service Vms");
          }
        }
        
        if(role==null)
          throw new ImagingServiceActionException("No role is found for Imaging Service Vms");   
  
        return true;
      }

      @Override
      public void rollback() throws ImagingServiceActionException{
        if(this.role!=null){
          try{
            EucalyptusActivityTasks.getInstance().deleteRole(this.role.getRoleName());
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to delete the created role");
          }
        }
      }

      @Override
      public String getResult() {
        if(this.role!=null)
          return this.role.getRoleName();
        else
          return null;
      }
    }
    
    public static class IamInstanceProfileSetup extends AbstractAction {
      public static final String DEFAULT_INSTANCE_PROFILE_PATH_PREFIX="/internal/imaging";
      public static final String DEFAULT_INSTANCE_PROFILE_NAME_PREFIX = "imaging";
      private InstanceProfileType instanceProfile = null;
      private String attachedRole = null;

      public IamInstanceProfileSetup(Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId){
        super(lookup, groupId);
      }
      
      @Override
      public boolean apply() throws ImagingServiceActionException {
        final String profilePath = DEFAULT_INSTANCE_PROFILE_PATH_PREFIX;
        final String profileName = String.format("%s-%s", DEFAULT_INSTANCE_PROFILE_NAME_PREFIX, this.getGroupId());
        final String roleName = this.getResult(IamRoleSetup.class);
        
       // list instance profiles
       try{
         //   check if the instance profile for ELB VM is found
         List<InstanceProfileType> instanceProfiles =
             EucalyptusActivityTasks.getInstance().listInstanceProfiles(profilePath);
         for(InstanceProfileType ip : instanceProfiles){
           if(profileName.equals(ip.getInstanceProfileName())){
             instanceProfile = ip;
             break;
           }
         }
       }catch(Exception ex){
         throw new ImagingServiceActionException("Failed to list instance profiles", ex);
       }
       
       if(instanceProfile == null){  //   if not create one
         try{
           instanceProfile = 
               EucalyptusActivityTasks.getInstance().createInstanceProfile(profileName, profilePath);
         }catch(Exception ex){
           throw new ImagingServiceActionException("Failed to create instance profile", ex);
         }
       }
       if(instanceProfile == null)
         throw new ImagingServiceActionException("No instance profile for imaging service VM is found");
       
       try{
         List<RoleType> roles = instanceProfile.getRoles().getMember();
         boolean roleFound = false;
         for(RoleType role : roles){
           if(role.getRoleName().equals(roleName)){
             roleFound=true;
             this.attachedRole = role.getRoleName();
             break;
           }
         }
         if(!roleFound)
           throw new NoSuchElementException();
       }catch(Exception ex){
         if(roleName == null)
           throw new ImagingServiceActionException("No role name is found for imaging service VMs");
        
         try{
           EucalyptusActivityTasks.getInstance()
             .addRoleToInstanceProfile(this.instanceProfile.getInstanceProfileName(), roleName);
           this.attachedRole = roleName;
         }catch(Exception ex2){
           throw new ImagingServiceActionException("Failed to add role to the instance profile", ex2);
         }
       }
      
       return true;
      }

      @Override
      public void rollback() throws ImagingServiceActionException {
        if(this.attachedRole!=null && this.instanceProfile !=null){
          try{
            EucalyptusActivityTasks.getInstance()
              .removeRoleFromInstanceProfile(this.instanceProfile.getInstanceProfileName(), this.attachedRole);
          }catch(final Exception ex){
            ;
          }
        }
        if(this.instanceProfile!=null){
          try{
            EucalyptusActivityTasks.getInstance()
            .deleteInstanceProfile(this.instanceProfile.getInstanceProfileName());
          }catch(final Exception ex){
            throw new ImagingServiceActionException("Failed to delete instance profile", ex);
          }
        }
      }
      
      @Override
      public String getResult() {
        if(this.instanceProfile!=null)
          return this.instanceProfile.getInstanceProfileName();
        else
          return null;
      }
    }
    
    public static class UserDataSetup extends AbstractAction {
      public UserDataSetup(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup,
          String groupId) {
        super(lookup, groupId);
      }
      
      @Override
      public boolean apply() throws ImagingServiceActionException{
        return true;
      }

      @Override
      public void rollback() {
        ;
      }

      @Override
      public String getResult() {
        final String userData = B64.standard.encString(String.format("%s\n%s",
            CREDENTIALS_STR,
            getUserDataMap(ImagingServiceProperties.IMAGING_WORKER_NTP_SERVER,
                ImagingServiceProperties.IMAGING_WORKER_LOG_SERVER,
                ImagingServiceProperties.IMAGING_WORKER_LOG_SERVER_PORT)));
        return userData;
      }
    }
    
    public static String getUserDataMap(String ntpServer, String logServer, String logServerPort) {
      Map<String,String> kvMap = new HashMap<String,String>();
      if(ntpServer != null)
        kvMap.put("ntp_server", ntpServer);
      if(logServer != null)
        kvMap.put("log_server", logServer);
      if(logServerPort != null)
        kvMap.put("log_server_port", logServerPort);

      ServiceConfiguration service = Topology.lookup( Eucalyptus.class );
      kvMap.put("eucalyptus_port", Integer.toString( service.getPort() ) );
      kvMap.put("ec2_path", service.getServicePath());
      service = Topology.lookup( Imaging.class );
      kvMap.put("imaging_path", service.getServicePath());

      final StringBuilder sb = new StringBuilder();
      for (String key : kvMap.keySet()){
        String value = kvMap.get(key);
        sb.append(String.format("%s=%s;", key, value));
      }
      return sb.toString();
    }

    public static class CreateLaunchConfiguration extends AbstractAction {
      private String emi = null;
      private String instanceType = null;
      private String keyName = null;
      
      private String createdLaunchConfig = null;
      public CreateLaunchConfiguration(Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId,
          final String emi, final String instanceType) {
        super(lookup, groupId);
        this.emi = emi;
        this.instanceType = instanceType;
      }
      
      public CreateLaunchConfiguration(Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId,
          final String emi, final String instanceType, final String keyName) {
        this(lookup, groupId, emi, instanceType);
        this.keyName = keyName;
      }

      @Override
      public boolean apply() throws ImagingServiceActionException {
        final String launchConfigName = String.format("lc-euca-internal-imaging-%s",this.getGroupId());
        try{
          final LaunchConfigurationType lcFound = 
              EucalyptusActivityTasks.getInstance().describeLaunchConfiguration(launchConfigName);
          this.createdLaunchConfig = launchConfigName;
        }catch(final Exception ex){
          ;
        }
        
        if(this.createdLaunchConfig==null){
        // create launch config based on the parameters

          String instanceProfileName = null;
          try{
            instanceProfileName = this.getResult(IamInstanceProfileSetup.class);
          }catch(Exception ex){
            throw new ImagingServiceActionException("failed to get the instance profile name", ex);
          }
        
          String sgName = null;
          try{
            sgName = this.getResult(SecurityGroupSetup.class);
          }catch(Exception ex){
            throw new ImagingServiceActionException("failed to get the security group name", ex);
          }
          
          String userData = null;
          try{
            userData = this.getResult(UserDataSetup.class);
          }catch(Exception ex){
            throw new ImagingServiceActionException("failed to get the user-data field", ex);
          }
          
          try{
            EucalyptusActivityTasks.getInstance().createLaunchConfiguration(this.emi, 
                this.instanceType, instanceProfileName,
                launchConfigName, sgName, this.keyName, userData);
            this.createdLaunchConfig = launchConfigName;
          }catch(Exception ex){
            throw new ImagingServiceActionException("Failed to create launch configuration", ex);
          }
        }
        return true;
      }

      @Override
      public void rollback() {
        if(this.createdLaunchConfig!=null){
          try{
            EucalyptusActivityTasks.getInstance().deleteLaunchConfiguration(this.createdLaunchConfig);
          }catch(final Exception ex){
            ;
          }
        }
      }

      @Override
      public String getResult() {
        if(this.createdLaunchConfig!=null)
          return this.createdLaunchConfig ;
        else
          return null;
      }
    }
    
    public static class CreateAutoScalingGroup extends AbstractAction {
      private String createdAutoScalingGroup = null;
      public static final int NUM_INSTANCES = 1;
      
      public CreateAutoScalingGroup(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId) {
        super(lookup, groupId);
      }

      @Override
      public boolean apply() throws ImagingServiceActionException {
        final String asgName = String.format("asg-euca-internal-imaging-%s", this.getGroupId());
        boolean asgFound = false;
        try{
          final DescribeAutoScalingGroupsResponseType response = 
              EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(asgName));
          final List<AutoScalingGroupType> groups =
              response.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
          if(groups.size()>0 && groups.get(0).getAutoScalingGroupName().equals(asgName)){
            asgFound =true;
          }
        }catch(final Exception ex){
          asgFound = false;
        }
        
        if(asgFound){
          createdAutoScalingGroup = asgName;
          return true;
        }
        
        String launchConfigName = null;
        try{
          launchConfigName = this.getResult(CreateLaunchConfiguration.class);
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to find the launch configuration name", ex);
        }
        
        List<String> availabilityZones = Lists.newArrayList();
        if(ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES!= null &&
            ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES.length()>0){
          if(ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES.contains(",")){
            final String[] tokens = ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES.split(",");
            for(final String zone : tokens)
              availabilityZones.add(zone);
          }else{
            availabilityZones.add(ImagingServiceProperties.IMAGING_WORKER_AVAILABILITY_ZONES);
          }
        }else{
          try{
            final List<ClusterInfoType> clusters = 
                EucalyptusActivityTasks.getInstance().describeAvailabilityZones(false);
            for(final ClusterInfoType c : clusters)
              availabilityZones.add(c.getZoneName());
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to lookup availability zones", ex);
          }
        }
        final int capacity = NUM_INSTANCES;
        try{
          EucalyptusActivityTasks.getInstance().createAutoScalingGroup(asgName, availabilityZones, 
              capacity, launchConfigName);
          this.createdAutoScalingGroup = asgName;
        }catch(Exception ex){
          throw new ImagingServiceActionException("Failed to create autoscaling group", ex);
        }
        
        return true;
      }

      @Override
      public void rollback() throws ImagingServiceActionException {
        if(this.createdAutoScalingGroup!=null){
          final Set<String> instances = Sets.newHashSet();
          try{
            final DescribeAutoScalingGroupsResponseType resp = 
                EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(this.createdAutoScalingGroup));
            for(final AutoScalingGroupType asg :
              resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember()){
              if(asg.getInstances()!=null){
                for(final Instance instance : asg.getInstances().getMember()){
                  instances.add(instance.getInstanceId());
                }
              }
            }
          }catch(final Exception ex){
            ;
          }
          
          try{
            EucalyptusActivityTasks.getInstance().deleteAutoScalingGroup(this.createdAutoScalingGroup, true);
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to delete autoscaling group", ex);
          }
          
          // poll the state of instances
          do{
            final List<String> terminated = Lists.newArrayList();
            for(final String instanceId : instances){
              try{
                final List<RunningInstancesItemType> runningInstances =
                    EucalyptusActivityTasks.getInstance().describeSystemInstances(Lists.newArrayList(instanceId));
                final String state = runningInstances.get(0).getStateName();
                if("terminated".equals(state))
                  terminated.add(instanceId);
                // pending | running | shutting-down | terminated | stopping | stopped
              }catch(final Exception ex){
                // assuming no such instance is found
                terminated.add(instanceId);
              }
            }
            
            for(final String terminatedId : terminated){
              instances.remove(terminatedId);
            }
            
            try{
              Thread.sleep(1000);
            }catch(final Exception ex){
              ;
            }
          }while(instances.size()>0);
        }
      }

      @Override
      public String getResult() {
        if(this.createdAutoScalingGroup!=null)
          return this.createdAutoScalingGroup;
        else
          return null;
      } 
    }
    
    public static class UploadServerCertificate extends AbstractAction {
      /// EUARE will not list and delete certificates under this path
      private static final String DEFAULT_SERVER_CERT_PATH = "/euca-internal";
      private String createdServerCert = null;
      private String certificateName = null;
      public UploadServerCertificate(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId, final String certName) {
        super(lookup, groupId);
        this.certificateName = certName;
      }

      @Override
      public boolean apply() throws ImagingServiceActionException{
        final String certPath = DEFAULT_SERVER_CERT_PATH;
        
        try{
          final ServerCertificateType cert = 
              EucalyptusActivityTasks.getInstance().getServerCertificate(this.certificateName);
          if(cert!=null && cert.getServerCertificateMetadata()!=null)
            this.createdServerCert = cert.getServerCertificateMetadata().getServerCertificateName();
        }catch(final Exception ex){
          this.createdServerCert = null;
        }
        
        if(this.createdServerCert == null){
          String certPem = null;
          String pkPem = null;
          try{
            final KeyPair kp = Certs.generateKeyPair();
            final X509Certificate kpCert = Certs.generateCertificate(kp, 
                String.format("Certificate-for-imaging-workers(%s)", this.getGroupId()));
          
            certPem = new String(PEMFiles.getBytes(kpCert));
            pkPem = new String(PEMFiles.getBytes(kp));
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed generating server cert", ex);
          }
          
          try{
            EucalyptusActivityTasks.getInstance().uploadServerCertificate(this.certificateName, certPath, certPem, pkPem, null);
            this.createdServerCert = this.certificateName;
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to upload server cert", ex);
          }
        }
        return true;
      }

      @Override
      public void rollback() throws ImagingServiceActionException {
        if(this.createdServerCert != null){
          try{
            EucalyptusActivityTasks.getInstance().deleteServerCertificate(this.createdServerCert);
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to delete server certificate");
          }
        }
      }

      @Override
      public String getResult() {
        return this.createdServerCert;
      }
    }
    
    public static class AuthorizeServerCertificate extends AbstractAction {
      public static final String SERVER_CERT_ROLE_POLICY_NAME_PREFIX = "imaging-iam-policy-servercert";
      public static final String ROLE_SERVER_CERT_POLICY_DOCUMENT=
        "{\"Statement\":[{\"Action\":[\"iam:DownloadServerCertificate\"],\"Effect\": \"Allow\",\"Resource\": \"CERT_ARN_PLACEHOLDER\"}]}";

      private String roleName = null;
      private String createdPolicyName = null;
      public AuthorizeServerCertificate(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId) {
        super(lookup, groupId);
      }

      @Override
      public boolean apply() throws ImagingServiceActionException{
        String certName = null;
        String certArn = null;
        try{
          roleName = this.getResult(IamRoleSetup.class);
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to find the created role name", ex);
        }
        
        try{
          certName = this.getResult(UploadServerCertificate.class);
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to find the uploaded cert name", ex);
        }
      
        try{
          final ServerCertificateType cert = EucalyptusActivityTasks.getInstance().getServerCertificate(certName);
          certArn = cert.getServerCertificateMetadata().getArn();
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to lookup server certificate", ex);
        }
      
        final String policyName = String.format("%s-%s", SERVER_CERT_ROLE_POLICY_NAME_PREFIX, this.getGroupId());
        final String rolePolicyDoc = ROLE_SERVER_CERT_POLICY_DOCUMENT.replace("CERT_ARN_PLACEHOLDER", certArn);
        try{
          final GetRolePolicyResult rolePolicy = EucalyptusActivityTasks.getInstance().getRolePolicy(roleName, policyName);
          if(rolePolicy!=null && policyName.equals(rolePolicy.getPolicyName()))
            this.createdPolicyName = policyName;
        }catch(final Exception ex){
          ;
        }
       
        if(this.createdPolicyName==null){
          try{
            EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, policyName, rolePolicyDoc);
            createdPolicyName = policyName;
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to authorize server certificate", ex);
          }
        }
        return true;
      }

      @Override
      public void rollback() throws ImagingServiceActionException{
        if(this.createdPolicyName!=null){
          try{
            EucalyptusActivityTasks.getInstance().deleteRolePolicy(this.roleName, this.createdPolicyName);
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to delete role policy for server certificate", ex);
          }
        }
      }

      @Override
      public String getResult() {
        return this.createdPolicyName;
      }
    }
    
    public static class AuthorizeVolumeOperations extends AbstractAction {
      public static final String VOLUME_OPS_ROLE_POLICY_NAME_PREFIX = "imaging-iam-policy-volumes";
      public static final String ROLE_VOLUME_OPS_POLICY_DOCUMENT=
        "{\"Statement\":[{\"Action\":[\"ec2:AttachVolume\",\"ec2:DetachVolume\",\"ec2:DescribeVolumes\"],\"Effect\": \"Allow\",\"Resource\": \"*\"}]}";

      private String roleName = null;
      private String createdPolicyName = null;
      public AuthorizeVolumeOperations(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId) {
        super(lookup, groupId);
      }

      @Override
      public boolean apply() throws ImagingServiceActionException{
        try{
          roleName = this.getResult(IamRoleSetup.class);
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to find the created role name", ex);
        }
      
        final String policyName = String.format("%s-%s", VOLUME_OPS_ROLE_POLICY_NAME_PREFIX, this.getGroupId());
        final String rolePolicyDoc = ROLE_VOLUME_OPS_POLICY_DOCUMENT;
        try{
          final GetRolePolicyResult rolePolicy = EucalyptusActivityTasks.getInstance().getRolePolicy(roleName, policyName);
          if(rolePolicy!=null && policyName.equals(rolePolicy.getPolicyName()))
            this.createdPolicyName = policyName;
        }catch(final Exception ex){
          ;
        }
       
        if(this.createdPolicyName==null){
          try{
            EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, policyName, rolePolicyDoc);
            createdPolicyName = policyName;
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to authorize volume operations", ex);
          }
        }
        return true;
      }

      @Override
      public void rollback() throws ImagingServiceActionException{
        if(this.createdPolicyName!=null){
          try{
            EucalyptusActivityTasks.getInstance().deleteRolePolicy(this.roleName, this.createdPolicyName);
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to delete role policy for volume operations", ex);
          }
        }
      }

      @Override
      public String getResult() {
        return this.createdPolicyName;
      }
    }
    
    public static class AuthorizeS3Operations extends AbstractAction {
      public static final String S3_OPS_ROLE_POLICY_NAME_PREFIX = "imaging-iam-policy-s3";
      public static final String ROLE_S3_OPS_POLICY_DOCUMENT=
        "{\"Statement\":[{\"Action\":[\"s3:*\"],\"Effect\": \"Allow\",\"Resource\": \"*\"}]}";

      private String roleName = null;
      private String createdPolicyName = null;
      public AuthorizeS3Operations(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup, final String groupId) {
        super(lookup, groupId);
      }

      @Override
      public boolean apply() throws ImagingServiceActionException{
        try{
          roleName = this.getResult(IamRoleSetup.class);
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to find the created role name", ex);
        }
      
        final String policyName = String.format("%s-%s", S3_OPS_ROLE_POLICY_NAME_PREFIX, this.getGroupId());
        final String rolePolicyDoc = ROLE_S3_OPS_POLICY_DOCUMENT;
        try{
          final GetRolePolicyResult rolePolicy = 
              EucalyptusActivityTasks.getInstance().getRolePolicy(roleName, policyName);
          if(rolePolicy!=null && policyName.equals(rolePolicy.getPolicyName()))
            this.createdPolicyName = policyName;
        }catch(final Exception ex){
          ;
        }
       
        if(this.createdPolicyName==null){
          try{
            EucalyptusActivityTasks.getInstance().putRolePolicy(roleName, policyName, rolePolicyDoc);
            createdPolicyName = policyName;
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to authorize S3 operations", ex);
          }
        }
        return true;
      }

      @Override
      public void rollback() throws ImagingServiceActionException{
        if(this.createdPolicyName!=null){
          try{
            EucalyptusActivityTasks.getInstance().deleteRolePolicy(this.roleName, this.createdPolicyName);
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to delete role policy for S3 operations", ex);
          }
        }
      }

      @Override
      public String getResult() {
        return this.createdPolicyName;
      }
    }
    
    public static class CreateTags extends AbstractAction {
      private String tagValue = null;
      private String tagKey = "Name";
      private String taggedSgroupId = null;
      private String taggedAsgName = null;
      
      public CreateTags(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup,
          String groupId) {
        super(lookup, groupId);
      }
      public CreateTags(
          Function<Class<? extends AbstractAction>, AbstractAction> lookup,
          String groupId, String tag) {
        super(lookup, groupId);
        tagValue = tag;
      }


      @Override
      public boolean apply() throws ImagingServiceActionException{
        // security group
        String asgName = null;
        try{
          asgName = this.getResult(CreateAutoScalingGroup.class);
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to get autoscaling group name", ex);
        }
        
        try{
          final DescribeAutoScalingGroupsResponseType resp =
              EucalyptusActivityTasks.getInstance().describeAutoScalingGroups(Lists.newArrayList(asgName));
          for(AutoScalingGroupType asg :
            resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember()){
            if(asg.getTags()!=null){
              for(final TagDescription tag : asg.getTags().getMember()){
                if(this.tagValue.equals(tag.getValue())){
                  this.taggedAsgName = asgName;
                  break;
                }
              } 
            }
          }
        }catch(final Exception ex){
          ;
        }
        
        if(this.taggedAsgName==null){
          try{
            EucalyptusActivityTasks.getInstance().createOrUpdateAutoscalingTags(tagKey, this.tagValue, asgName);
            this.taggedAsgName = asgName;
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to tag autoscaling group", ex);
          }
        }
        
        String sgroupName = null;
        String sgroupId = null;
        try{
          sgroupName = this.getResult(SecurityGroupSetup.class);
          List<SecurityGroupItemType> groups = 
              EucalyptusActivityTasks.getInstance().describeSecurityGroups(Lists.newArrayList(sgroupName));
          if(groups!=null && groups.size()>0){
            final SecurityGroupItemType current = groups.get(0);
            if(sgroupName.equals(current.getGroupName())){
                sgroupId= current.getGroupId();
            }
          }
          if(sgroupId==null)
            throw new Exception(String.format("No security group named %s is found", sgroupName));
        }catch(final Exception ex){
          throw new ImagingServiceActionException("failed to get security group name", ex);
        }
        
        try{
          final List<TagInfo> tags = EucalyptusActivityTasks.getInstance().describeTags(Lists.newArrayList("value"), 
            Lists.newArrayList(this.tagValue));
          if(tags !=null){
            for(final TagInfo tag : tags){
              if(sgroupId.equals(tag.getResourceId())){
                this.taggedSgroupId = sgroupId;
                break;
              }
            }
          }
        }catch(final Exception ex){
          ;
        }
        
        if(this.taggedSgroupId==null){
          try{
            EucalyptusActivityTasks.getInstance().createTags(tagKey, tagValue, Lists.newArrayList(sgroupId));
            taggedSgroupId = sgroupId;
          }catch(final Exception ex){
            throw new ImagingServiceActionException("failed to tag security group", ex);
          }
        }
   
        return true;
      }

      @Override
      public void rollback() throws ImagingServiceActionException {
        if(this.tagKey != null && this.tagValue !=null){
          if(this.taggedSgroupId!=null){
            try{
              EucalyptusActivityTasks.getInstance()
                .deleteTags(this.tagKey, this.tagValue, Lists.newArrayList(this.taggedSgroupId));
            }catch(final Exception ex){
              ;
            }
          }
          
          if(this.taggedAsgName!=null){
            try{
              EucalyptusActivityTasks.getInstance()
                .deleteAutoscalingTags(this.tagKey, this.tagValue, this.taggedAsgName);
            }catch(final Exception ex){
              ;
            }
          }
        }
      }

      @Override
      public String getResult() {
        return this.tagValue;
      }
    }
}

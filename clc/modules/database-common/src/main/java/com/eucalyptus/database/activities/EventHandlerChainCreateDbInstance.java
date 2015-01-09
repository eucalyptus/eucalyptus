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
package com.eucalyptus.database.activities;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.GetRolePolicyResult;
import com.eucalyptus.auth.euare.InstanceProfileType;
import com.eucalyptus.auth.euare.RoleType;
import com.eucalyptus.auth.euare.ServerCertificateType;
import com.eucalyptus.autoscaling.common.msgs.AutoScalingGroupType;
import com.eucalyptus.autoscaling.common.msgs.DescribeAutoScalingGroupsResponseType;
import com.eucalyptus.autoscaling.common.msgs.Instance;
import com.eucalyptus.autoscaling.common.msgs.LaunchConfigurationType;
import com.eucalyptus.autoscaling.common.msgs.TagDescription;
import com.eucalyptus.compute.common.ClusterInfoType;
import com.eucalyptus.compute.common.DescribeKeyPairsResponseItemType;
import com.eucalyptus.compute.common.ImageDetails;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupItemType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.compute.common.Volume;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.resources.AbstractEventHandler;
import com.eucalyptus.resources.EventHandlerChain;
import com.eucalyptus.resources.EventHandlerException;
import com.eucalyptus.resources.StoredResult;
import com.eucalyptus.resources.client.AutoScalingClient;
import com.eucalyptus.resources.client.Ec2Client;
import com.eucalyptus.resources.client.EuareClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sang-Min Park
 *
 */
public class EventHandlerChainCreateDbInstance extends
    EventHandlerChain<NewDBInstanceEvent> {
  private static Logger  LOG = Logger.getLogger( EventHandlerChainCreateDbInstance.class );
  @Override
  public EventHandlerChain<NewDBInstanceEvent> build() {
    this.append(new AdmissionControl(this));
    this.append(new SecurityGroupSetup(this));
    this.append(new AuthorizePort(this));
    this.append(new IamRoleSetup(this));
    this.append(new IamInstanceProfileSetup(this));
    this.append(new UploadServerCertificate(this));
    this.append(new AuthorizeServerCertificate(this));
    this.append(new AuthorizeVolumeOperations(this));
    this.append(new UserDataSetup(this));
    this.append(new CreateLaunchConfiguration(this, DatabaseServerProperties.IMAGE, DatabaseServerProperties.INSTANCE_TYPE, 
          ( DatabaseServerProperties.KEYNAME != null && DatabaseServerProperties.KEYNAME.length()>0) ? DatabaseServerProperties.KEYNAME : null));
    this.append(new CreateAutoScalingGroup(this));
    this.append(new CreateTags(this));
    return this;
  }
  
  private static String getSystemUserId(){ 
    try{
      return Accounts.lookupSystemAdmin().getUserId();
    }catch(final Exception ex){
      return null;
    }
  }
  
  // make sure the cloud is ready to launch database server instances
  // e.g., lack of resources will keep the launcher from creating resources
  public static class AdmissionControl extends AbstractEventHandler<NewDBInstanceEvent> {
    public AdmissionControl(EventHandlerChain<NewDBInstanceEvent> chain) {
      super(chain);
    }

    @Override
    public void apply(NewDBInstanceEvent evt) throws EventHandlerException {
      // check if there is an ASG for the db server
      boolean asgFound = false;
      final String userId = evt.getUserId();
      String systemUserId = getSystemUserId();
      String acctNumber = null;
      try{
        acctNumber =  Accounts.lookupUserById(userId).getAccountNumber();
      }catch(final AuthException ex){
        throw new EventHandlerException("Failed to lookup system user", ex);
      }
      
      final String asgName = CreateAutoScalingGroup.getAutoscalingGroupName(acctNumber, evt.getDbInstanceIdentifier());
      try{
        final DescribeAutoScalingGroupsResponseType resp = 
            AutoScalingClient.getInstance().describeAutoScalingGroups(null, Lists.newArrayList(asgName));
        final List<AutoScalingGroupType> groups =
            resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember();
        for(final AutoScalingGroupType asg : groups){
          if(asg.getAutoScalingGroupName().contains(asgName)){
            asgFound = true;
            break;
          } 
        }
      }catch(final Exception ex){
        asgFound = false;
      }
      
      if(asgFound)
        throw new EventHandlerException("Existing autoscaling group ("+asgName+") found");
     
      // this will stop the whole instance launch chain
      final String emi = DatabaseServerProperties.IMAGE;
      List<ImageDetails> images = null;
      try{
        images = Ec2Client.getInstance().describeImages(systemUserId, Lists.newArrayList(emi));
        if(images==null || images.size()<=0 ||! images.get(0).getImageId().toLowerCase().equals(emi.toLowerCase()))
          throw new EventHandlerException("No such EMI is found: "+emi);
      }catch(final EventHandlerException ex){
        throw ex;
      }catch(final Exception ex){
        throw new EventHandlerException("failed to validate the db server EMI", ex);
      }
      
      final String volumeId = DatabaseServerProperties.VOLUME;
      List<Volume> volumes = null;
      try{
        volumes = Ec2Client.getInstance().describeVolumes(systemUserId, Lists.newArrayList(volumeId));
        if(volumes==null || volumes.size()<=0 || ! volumes.get(0).getVolumeId().toLowerCase().equals(volumeId.toLowerCase()))
          throw new EventHandlerException("No such volume id is found: " + volumeId);
        
        if(! "available".equals(volumes.get(0).getStatus()))
          throw new EventHandlerException("Volume is not available");
      }catch(final EventHandlerException ex) {
        throw ex;
      }catch(final Exception ex) {
        throw new EventHandlerException("failed to validate the db server volume ID", ex);
      }
      
      List<ClusterInfoType> clusters = null;
      try{
        clusters = Ec2Client.getInstance().describeAvailabilityZones(null, true);
      }catch(final Exception ex){
        throw new EventHandlerException("failed to validate the zones", ex);
      }
    
      // are there enough resources?
      final String instanceType = DatabaseServerProperties.INSTANCE_TYPE;
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
        throw new EventHandlerException("not enough resource in the cloud");
      
      // check if the keyname is configured and exists
      final String keyName = DatabaseServerProperties.KEYNAME;
      if(keyName!=null && keyName.length()>0){
        try{
          final List<DescribeKeyPairsResponseItemType> keypairs = 
              Ec2Client.getInstance().describeKeyPairs(systemUserId, Lists.newArrayList(keyName));
          if(keypairs==null || keypairs.size()<=0 || !keypairs.get(0).getKeyName().equals(keyName))
            throw new Exception();
        }catch(Exception ex){
          throw new EventHandlerException(String.format("The configured keyname %s is not found", 
              DatabaseServerProperties.KEYNAME));
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
    public void rollback() {
      ;
    }
  }
  
  public static class SecurityGroupSetup extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<String> {
    private String createdGroup = null;
    private String createdGroupId = null;
    private String groupOwnerAccountId = null;
    NewDBInstanceEvent event = null;
    SecurityGroupSetup(EventHandlerChain<NewDBInstanceEvent> chain){
      super(chain);
    }

    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      this.event = evt;
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      String acctNumber = null;
      try{
        acctNumber =  Accounts.lookupUserById(userId).getAccountNumber();
      }catch(final AuthException ex){
        throw new EventHandlerException("Failed to lookup system user", ex);
      }

      String groupName = String.format("euca-internal-db-%s-%s", acctNumber, evt.getDbInstanceIdentifier());
      String groupDesc = String.format("group for db server %s", evt.getDbInstanceIdentifier());
      
      // check if there's an existing group with the same name
      boolean groupFound = false;
      try{
        List<SecurityGroupItemType> groups = 
            Ec2Client.getInstance().describeSecurityGroups(systemUserId, Lists.newArrayList(groupName));
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
          Ec2Client.getInstance().createSecurityGroup(systemUserId, groupName, groupDesc);
          createdGroup = groupName;
          List<SecurityGroupItemType> groups = 
              Ec2Client.getInstance().describeSecurityGroups(systemUserId, Lists.newArrayList(groupName));
          if(groups!=null && groups.size()>0){
            final SecurityGroupItemType current = groups.get(0);
            if(groupName.equals(current.getGroupName())){
              this.groupOwnerAccountId = current.getAccountId();
              this.createdGroupId= current.getGroupId();
            }
          }
        }catch(Exception ex){
          throw new EventHandlerException("Failed to create the security group for db server", ex);
        }
      }else{
        createdGroup = groupName;
      }
      
      if(this.createdGroup == null || this.groupOwnerAccountId == null)
        throw new EventHandlerException("Failed to create the security group for db server");
    }

    @Override
    public void rollback() 
        throws EventHandlerException {
      if(this.createdGroup == null)
        return;
      try{
        Ec2Client.getInstance().deleteSecurityGroup(getSystemUserId(), this.createdGroup);
      }catch(Exception ex){
        ; // when there's any servo instance referencing the security group
                // SecurityGroupCleanup will clean up records
      }
    }

    @Override
    public List<String> getResult() {
      List<String> result = Lists.newArrayList();
      if(this.createdGroup != null)
        result.add(this.createdGroup);
      if(this.createdGroupId != null)
        result.add(this.createdGroupId);
      return result;
    }
  }
  
  public static class AuthorizePort extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<Integer> {
    private String sgroupName = null;
    private int authorizedPort = -1;
    protected AuthorizePort(EventHandlerChain<NewDBInstanceEvent> chain) {
      super(chain);
    }
    
    @Override
    public void apply(NewDBInstanceEvent evt) throws EventHandlerException {
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      try{
        sgroupName = this.getChain().findHandler(SecurityGroupSetup.class).getResult().get(0);
      }catch(final Exception ex){
        throw new EventHandlerException("Cannot find the created security group name", ex);
      }
      
      try{
        Ec2Client.getInstance().authorizeSecurityGroup(systemUserId, sgroupName, "tcp", evt.getPort());
        this.authorizedPort = evt.getPort();
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to authorize the port", ex);
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      if (sgroupName!=null && authorizedPort > 0 ) {
        try{
          Ec2Client.getInstance().revokeSecurityGroup(getSystemUserId(), sgroupName, "tcp", authorizedPort);
        }catch(final Exception ex) {
          throw new EventHandlerException("Failed to revoke the port", ex);
        }
      }
    }

    @Override
    public List<Integer> getResult() {
      if(this.authorizedPort > 0 ) {
        return Lists.newArrayList(this.authorizedPort);
      }else
        return Lists.newArrayList();
    }
  }
  
  
  public static class IamRoleSetup extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<String> {
    private static final String DEFAULT_ROLE_PATH_PREFIX = "/internal/db";
    private static final String DEFAULT_ROLE_NAME = "db";
    private static final String DEFAULT_ASSUME_ROLE_POLICY =
        "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":[\"ec2.amazonaws.com\"]},\"Action\":[\"sts:AssumeRole\"]}]}";
    private RoleType role = null;

    public IamRoleSetup(EventHandlerChain<NewDBInstanceEvent> chain){
      super(chain);
    }

    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      String acctNumber = null;
      try{
        acctNumber =  Accounts.lookupUserById(userId).getAccountNumber();
      }catch(final AuthException ex){
        throw new EventHandlerException("Failed to lookup account number", ex);
      }
      
      final String rolePath = DEFAULT_ROLE_PATH_PREFIX;
      final String roleName = String.format("%s-%s-%s",DEFAULT_ROLE_NAME, acctNumber, evt.getDbInstanceIdentifier());
      final String assumeRolePolicy = DEFAULT_ASSUME_ROLE_POLICY;
      // list-roles.
      try{
        List<RoleType> result = EuareClient.getInstance().listRoles(systemUserId, rolePath);
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
      if(role==null){ /// create a new role
        try{
          role = EuareClient.getInstance().createRole(systemUserId, roleName, rolePath, assumeRolePolicy);
        }catch(Exception ex){
          throw new EventHandlerException("Failed to create the role for db server Vms");
        }
      }
      
      if(role==null)
        throw new EventHandlerException("No role is found for db server Vms");   
    }

    @Override
    public void rollback() throws EventHandlerException{
      if(this.role!=null){
        try{
          EuareClient.getInstance().deleteRole(getSystemUserId(), this.role.getRoleName());
        }catch(final Exception ex){
          throw new EventHandlerException("failed to delete the created role");
        }
      }
    }

    @Override
    public List<String> getResult() {
      if(this.role!=null)
        return Lists.newArrayList(this.role.getRoleName());
      else
        return Lists.newArrayList();
    }
  }
  
  public static class IamInstanceProfileSetup extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<String> {
    public static final String DEFAULT_INSTANCE_PROFILE_PATH_PREFIX="/internal/db";
    public static final String DEFAULT_INSTANCE_PROFILE_NAME_PREFIX = "db";
    private InstanceProfileType instanceProfile = null;
    private String attachedRole = null;

    public IamInstanceProfileSetup(EventHandlerChain<NewDBInstanceEvent> chain){
      super(chain);
    }
    
    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      String acctNumber = null;
      try{
        acctNumber =  Accounts.lookupUserById(userId).getAccountNumber();
      }catch(final AuthException ex){
        throw new EventHandlerException("Failed to lookup account number", ex);
      }
      final String profilePath = DEFAULT_INSTANCE_PROFILE_PATH_PREFIX;
      final String profileName = String.format("%s-%s-%s", DEFAULT_INSTANCE_PROFILE_NAME_PREFIX, acctNumber, evt.getDbInstanceIdentifier());
      String roleName = null;
      try{
        roleName=this.getChain().findHandler(IamRoleSetup.class).getResult().get(0);
      }catch(final Exception ex){
        throw new EventHandlerException("Failed to lookup the created role name");
      }
      
     // list instance profiles
     try{
       //   check if the instance profile for db VM is found
       List<InstanceProfileType> instanceProfiles =
           EuareClient.getInstance().listInstanceProfiles(systemUserId, profilePath);
       for(InstanceProfileType ip : instanceProfiles){
         if(profileName.equals(ip.getInstanceProfileName())){
           instanceProfile = ip;
           break;
         }
       }
     }catch(Exception ex){
       throw new EventHandlerException("Failed to list instance profiles", ex);
     }
     
     if(instanceProfile == null){  //   if not create one
       try{
         instanceProfile = 
             EuareClient.getInstance().createInstanceProfile(systemUserId, profileName, profilePath);
       }catch(Exception ex){
         throw new EventHandlerException("Failed to create instance profile", ex);
       }
     }
     if(instanceProfile == null)
       throw new EventHandlerException("No instance profile for db server VM is found");
     
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
         throw new EventHandlerException("No role name is found for db service VMs");
      
       try{
         EuareClient.getInstance()
           .addRoleToInstanceProfile(systemUserId, this.instanceProfile.getInstanceProfileName(), roleName);
         this.attachedRole = roleName;
       }catch(Exception ex2){
         throw new EventHandlerException("Failed to add role to the instance profile", ex2);
       }
     }
    }

    @Override
    public void rollback() throws EventHandlerException{
      if(this.attachedRole!=null && this.instanceProfile !=null){
        try{
          EuareClient.getInstance()
            .removeRoleFromInstanceProfile(getSystemUserId(), this.instanceProfile.getInstanceProfileName(), this.attachedRole);
        }catch(final Exception ex){
          ;
        }
      }
      if(this.instanceProfile!=null){
        try{
          EuareClient.getInstance()
          .deleteInstanceProfile(getSystemUserId(), this.instanceProfile.getInstanceProfileName());
        }catch(final Exception ex){
          throw new EventHandlerException("Failed to delete instance profile", ex);
        }
      }
    }
    
    @Override
    public List<String> getResult() {
      if(this.instanceProfile!=null)
        return Lists.newArrayList(this.instanceProfile.getInstanceProfileName());
      else
        return Lists.newArrayList();
    }
  }
  
  public static class UploadServerCertificate extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<String> {
    /// EUARE will not list and delete certificates under this path
    private static final String DEFAULT_SERVER_CERT_PATH = "/euca-internal";
    private static final String SERVER_CERT_NAME_PREFIX = "euca-internal-db";
    private String createdServerCert = null;
    private String certificate = null;
    public UploadServerCertificate(EventHandlerChain<NewDBInstanceEvent> chain){
      super(chain);
    }
    
    // make it static for now; later should be stored in db
    public static String getCertificateName(final String acctNumber, final String dbIdentifier) {
      return String.format("%s-%s-%s", SERVER_CERT_NAME_PREFIX, acctNumber, dbIdentifier);
    }
    
    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      String acctNumber = null;
      try{
        acctNumber =  Accounts.lookupUserById(userId).getAccountNumber();
      }catch(final AuthException ex){
        throw new EventHandlerException("Failed to lookup account number", ex);
      }
      final String certPath = DEFAULT_SERVER_CERT_PATH;
      final String certName = getCertificateName(acctNumber, evt.getDbInstanceIdentifier());
      try{
        final ServerCertificateType cert = 
            EuareClient.getInstance().getServerCertificate(systemUserId, certName);
        if(cert!=null && cert.getServerCertificateMetadata()!=null) {
          // delete the existing server cert
          try{
            EuareClient.getInstance().deleteServerCertificate(systemUserId, cert.getServerCertificateMetadata().getServerCertificateName());
            this.createdServerCert=null;
          }catch(final Exception ex){
            throw new EventHandlerException("failed to delete server certificate");
          }
        }
      }catch(final EventHandlerException ex) {
        throw ex;
      }catch(final Exception ex){
        this.createdServerCert = null;
      }
      
      if(this.createdServerCert == null){
        String pkPem = null;
        String certPem = null;
        try{
          final KeyPair kp = Certs.generateKeyPair();
          final X509Certificate kpCert = Certs.generateCertificate(kp, 
              String.format("Certificate-for-db-(%s-%s)", acctNumber, evt.getDbInstanceIdentifier()));
        
          certPem = new String(PEMFiles.getBytes(kpCert));
          pkPem = new String(PEMFiles.getBytes(kp));
          certificate = PEMFiles.fromCertificate(kpCert);
        }catch(final Exception ex){
          throw new EventHandlerException("failed generating server cert", ex);
        }
        
        try{
          EuareClient.getInstance().uploadServerCertificate(systemUserId, certName, certPath, certPem, pkPem, null);
          this.createdServerCert = certName;
        }catch(final Exception ex){
          throw new EventHandlerException("failed to upload server cert", ex);
        }
      }
    }

    @Override
    public void rollback() throws EventHandlerException{
      if(this.createdServerCert != null){
        try{
          EuareClient.getInstance().deleteServerCertificate(getSystemUserId(), this.createdServerCert);
        }catch(final Exception ex){
          throw new EventHandlerException("failed to delete server certificate");
        }
      }
    }

    @Override
    public List<String> getResult() {
      if(this.createdServerCert!=null)
        return Lists.newArrayList(this.createdServerCert, this.certificate);
      else
        return Lists.newArrayList();
    }
  }
  
  public static class AuthorizeServerCertificate extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<String> {
    public static final String SERVER_CERT_ROLE_POLICY_NAME_PREFIX = "db-iam-policy-servercert";
    public static final String ROLE_SERVER_CERT_POLICY_DOCUMENT=
      "{\"Statement\":[{\"Action\":[\"iam:DownloadServerCertificate\"],\"Effect\": \"Allow\",\"Resource\": \"CERT_ARN_PLACEHOLDER\"}]}";

    private String roleName = null;
    private String createdPolicyName = null;
    public AuthorizeServerCertificate(EventHandlerChain<NewDBInstanceEvent> chain){
      super(chain);
    }
    
    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      String acctNumber = null;
      try{
        acctNumber =  Accounts.lookupUserById(userId).getAccountNumber();
      }catch(final AuthException ex){
        throw new EventHandlerException("Failed to lookup account number", ex);
      }
      
      String certName = null;
      String certArn = null;
      try{
        roleName = this.getChain().findHandler(IamRoleSetup.class).getResult().get(0);
      }catch(final Exception ex){
        throw new EventHandlerException("failed to find the created role name", ex);
      }
      
      try{
        certName = this.getChain().findHandler(UploadServerCertificate.class).getResult().get(0);
      }catch(final Exception ex){
        throw new EventHandlerException("failed to find the uploaded cert name", ex);
      }
    
      try{
        final ServerCertificateType cert = EuareClient.getInstance().getServerCertificate(systemUserId, certName);
        certArn = cert.getServerCertificateMetadata().getArn();
      }catch(final Exception ex){
        throw new EventHandlerException("failed to lookup server certificate", ex);
      }
    
      final String policyName = String.format("%s-%s-%s", SERVER_CERT_ROLE_POLICY_NAME_PREFIX, acctNumber, evt.getDbInstanceIdentifier());
      final String rolePolicyDoc = ROLE_SERVER_CERT_POLICY_DOCUMENT.replace("CERT_ARN_PLACEHOLDER", certArn);
      try{
        final GetRolePolicyResult rolePolicy = EuareClient.getInstance().getRolePolicy(systemUserId, roleName, policyName);
        if(rolePolicy!=null && policyName.equals(rolePolicy.getPolicyName()))
          this.createdPolicyName = policyName;
      }catch(final Exception ex){
        ;
      }
     
      if(this.createdPolicyName==null){
        try{
          EuareClient.getInstance().putRolePolicy(systemUserId, roleName, policyName, rolePolicyDoc);
          createdPolicyName = policyName;
        }catch(final Exception ex){
          throw new EventHandlerException("failed to authorize server certificate", ex);
        }
      }
    }

    @Override
    public void rollback() throws EventHandlerException{
      if(this.createdPolicyName!=null){
        try{
          EuareClient.getInstance().deleteRolePolicy(getSystemUserId(), this.roleName, this.createdPolicyName);
        }catch(final Exception ex){
          throw new EventHandlerException("failed to delete role policy for server certificate", ex);
        }
      }
    }

    @Override
    public List<String> getResult() {
      if(this.createdPolicyName!=null)
        return Lists.newArrayList(this.createdPolicyName);
      else
        return Lists.newArrayList();
    }
  }
  
  public static class AuthorizeVolumeOperations extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<String> {
    public static final String VOLUME_OPS_ROLE_POLICY_NAME_PREFIX = "db-iam-policy-volumes";
    public static final String ROLE_VOLUME_OPS_POLICY_DOCUMENT=
      "{\"Statement\":[{\"Action\":[\"ec2:CreateVolume\",\"ec2:AttachVolume\",\"ec2:DetachVolume\",\"ec2:DescribeVolumes\"],\"Effect\": \"Allow\",\"Resource\": \"*\"}]}";

    private String roleName = null;
    private String createdPolicyName = null;
    public AuthorizeVolumeOperations(EventHandlerChain<NewDBInstanceEvent> chain){
      super(chain);
    }
    
    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      String acctNumber = null;
      try{
        acctNumber =  Accounts.lookupUserById(userId).getAccountNumber();
      }catch(final AuthException ex){
        throw new EventHandlerException("Failed to lookup account number", ex);
      }
      
      try{
        roleName = this.getChain().findHandler(IamRoleSetup.class).getResult().get(0);
      }catch(final Exception ex){
        throw new EventHandlerException("failed to find the created role name", ex);
      }
      final String policyName = String.format("%s-%s-%s", VOLUME_OPS_ROLE_POLICY_NAME_PREFIX, acctNumber, evt.getDbInstanceIdentifier());
      final String rolePolicyDoc = ROLE_VOLUME_OPS_POLICY_DOCUMENT;
      try{
        final GetRolePolicyResult rolePolicy = 
            EuareClient.getInstance().getRolePolicy(systemUserId, roleName, policyName);
        if(rolePolicy!=null && policyName.equals(rolePolicy.getPolicyName()))
          this.createdPolicyName = policyName;
      }catch(final Exception ex){
        ;
      }
     
      if(this.createdPolicyName==null){
        try{
          EuareClient.getInstance().putRolePolicy(systemUserId, roleName, policyName, rolePolicyDoc);
          createdPolicyName = policyName;
        }catch(final Exception ex){
          throw new EventHandlerException("failed to authorize volume operations", ex);
        }
      }
    }

    @Override
    public void rollback() throws EventHandlerException{
      if(this.createdPolicyName!=null){
        try{
          EuareClient.getInstance().deleteRolePolicy(getSystemUserId(), this.roleName, this.createdPolicyName);
        }catch(final Exception ex){
          throw new EventHandlerException("failed to delete role policy for volume operations", ex);
        }
      }
    }

    @Override
    public List<String> getResult() {
      if(this.createdPolicyName!=null)
        return Lists.newArrayList(this.createdPolicyName);
      else
        return Lists.newArrayList();
    }
  }
  
  public static class UserDataSetup extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<String> {
    private String encryptedPassword = null;
    private String serverCertArn = null;
    public UserDataSetup(EventHandlerChain<NewDBInstanceEvent> chain){
      super(chain);
    }
    
    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      String certPem = null;
      X509Certificate serverCert = null;
      String serverCertName = null;
      try{
        List<String> result = this.getChain().findHandler(UploadServerCertificate.class).getResult();
        serverCertName = result.get(0); 
        certPem = result.get(1);
        serverCert = PEMFiles.toCertificate(certPem);
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to find the server certificate");
      }
      
      try{
        final Cipher cipher = Ciphers.RSA_PKCS1.get();
        cipher.init(Cipher.ENCRYPT_MODE, serverCert.getPublicKey(), Crypto.getSecureRandomSupplier( ).get( ));
        byte[] bencPassword = cipher.doFinal(evt.getMasterUserPassword().getBytes());
        encryptedPassword = new String(Base64.encode(bencPassword));
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to encrypt the password");
      }
      
      try{
        final ServerCertificateType cert = EuareClient.getInstance().getServerCertificate(getSystemUserId(), serverCertName);
        serverCertArn = cert.getServerCertificateMetadata().getArn();
      }catch(final Exception ex) {
        throw new EventHandlerException("Failed to get server certificate named "+serverCertName, ex);
      }
    }

    @Override
    public void rollback() {
      ;
    }
    

    @Override
    public List<String> getResult() {
      final String userData = B64.standard.encString(String.format("%s\n%s",
          DatabaseServerProperties.getCredentialsString(),
          DatabaseServerProperties.getServerUserData(DatabaseServerProperties.VOLUME, 
              DatabaseServerProperties.NTP_SERVER,
              DatabaseServerProperties.INIT_SCRIPT,
              this.encryptedPassword,
              this.serverCertArn)));
      return Lists.newArrayList(userData);
    }
  }
  
  public static class CreateLaunchConfiguration extends AbstractEventHandler<NewDBInstanceEvent> 
    implements StoredResult<String> {
    private String emi = null;
    private String instanceType = null;
    private String keyName = null;
    
    private String createdLaunchConfig = null;
    public CreateLaunchConfiguration(EventHandlerChain<NewDBInstanceEvent> chain, 
        final String emi, final String instanceType) {
      super(chain);
      this.emi = emi;
      this.instanceType = instanceType;
    }
    
    public CreateLaunchConfiguration(EventHandlerChain<NewDBInstanceEvent> chain, 
        final String emi, final String instanceType, final String keyName) {
      this(chain, emi, instanceType);
      this.keyName = keyName;
    }

    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      String acctNumber = null;
      try{
        acctNumber =  Accounts.lookupUserById(userId).getAccountNumber();
      }catch(final AuthException ex){
        throw new EventHandlerException("Failed to lookup account number", ex);
      }
      
      final String launchConfigName = String.format("lc-euca-internal-db-%s-%s", acctNumber, evt.getDbInstanceIdentifier());
      try{
        final LaunchConfigurationType lcFound = 
            AutoScalingClient.getInstance().describeLaunchConfiguration(systemUserId, launchConfigName);
        if(lcFound!=null) {
          try{
            AutoScalingClient.getInstance().deleteLaunchConfiguration(systemUserId, launchConfigName);
            LOG.debug("Deleted the existing launch config: "+launchConfigName);
            this.createdLaunchConfig=null;
          }catch(final Exception ex){
            // this shouldn't happen
            this.createdLaunchConfig = launchConfigName;
          }
        }
      }catch(final Exception ex){
        ;
      }
      
      if(this.createdLaunchConfig==null){
      // create launch config based on the parameters

        String instanceProfileName = null;
        try{
          instanceProfileName = this.getChain().findHandler(IamInstanceProfileSetup.class).getResult().get(0);
        }catch(Exception ex){
          throw new EventHandlerException("failed to get the instance profile name", ex);
        }
      
        String sgName = null;
        try{
          sgName = this.getChain().findHandler(SecurityGroupSetup.class).getResult().get(0);
        }catch(Exception ex){
          throw new EventHandlerException("failed to get the security group name", ex);
        }
        
        String userData = null;
        try{
          userData =  this.getChain().findHandler(UserDataSetup.class).getResult().get(0);
        }catch(Exception ex){
          throw new EventHandlerException("failed to get the user-data field", ex);
        }
        
        try{
          AutoScalingClient.getInstance().createLaunchConfiguration(systemUserId, this.emi, 
              this.instanceType, instanceProfileName,
              launchConfigName, sgName, this.keyName, userData);
          this.createdLaunchConfig = launchConfigName;
        }catch(Exception ex){
          throw new EventHandlerException("Failed to create launch configuration", ex);
        }
      }
    }

    @Override
    public void rollback() {
      if(this.createdLaunchConfig!=null){
        try{
          AutoScalingClient.getInstance().deleteLaunchConfiguration(getSystemUserId(), this.createdLaunchConfig);
        }catch(final Exception ex){
          ;
        }
      }
    }

    @Override
    public List<String> getResult() {
      if(this.createdLaunchConfig!=null)
        return Lists.newArrayList(this.createdLaunchConfig );
      else
        return Lists.newArrayList();
    }
  }
  
  public static class CreateAutoScalingGroup extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<String>{
    private String createdAutoScalingGroup = null;    
    public CreateAutoScalingGroup(EventHandlerChain<NewDBInstanceEvent> chain){
      super(chain);
    }
    
    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      final String userId = evt.getUserId();
      final String systemUserId = getSystemUserId();
      String acctNumber = null;
      try{
        acctNumber =  Accounts.lookupUserById(userId).getAccountNumber();
      }catch(final AuthException ex){
        throw new EventHandlerException("Failed to lookup account number", ex);
      }
      final String asgName = getAutoscalingGroupName(acctNumber, evt.getDbInstanceIdentifier());
      boolean asgFound = false;
      try{
        final DescribeAutoScalingGroupsResponseType response = 
            AutoScalingClient.getInstance().describeAutoScalingGroups(systemUserId, Lists.newArrayList(asgName));
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
        return;
      }
      
      String launchConfigName = null;
      try{
        launchConfigName = this.getChain().findHandler(CreateLaunchConfiguration.class).getResult().get(0);
      }catch(final Exception ex){
        throw new EventHandlerException("failed to find the launch configuration name", ex);
      }
      
      List<String> availabilityZones = null;
      try{
        availabilityZones = DatabaseServerProperties.listConfiguredZones();
      }catch(final Exception ex){
        throw new EventHandlerException("Failed to lookup configured availability zones for db servers", ex);
      }
      if(availabilityZones.size()<=0)
        throw new EventHandlerException("No availability zone is found for deploying db servers");
      try{
        AutoScalingClient.getInstance().createAutoScalingGroup(systemUserId, asgName, availabilityZones, 
            1, launchConfigName, null, null);
        this.createdAutoScalingGroup = asgName;
      }catch(Exception ex){
        throw new EventHandlerException("Failed to create autoscaling group", ex);
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      if(this.createdAutoScalingGroup!=null){
        final Set<String> instances = Sets.newHashSet();
        final String systemUserId = getSystemUserId();
        try{
          final DescribeAutoScalingGroupsResponseType resp = 
              AutoScalingClient.getInstance().describeAutoScalingGroups(systemUserId, 
                  Lists.newArrayList(this.createdAutoScalingGroup));
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
          AutoScalingClient.getInstance().updateAutoScalingGroup(systemUserId, this.createdAutoScalingGroup, null, 0);
        }catch(final Exception ex){
          LOG.warn(String.format("Unable to set desired capacity for %s", this.createdAutoScalingGroup), ex);
        }
        
        boolean error = false;
        final int NUM_DELETE_ASG_RETRY = 4;
        for(int i=0; i<NUM_DELETE_ASG_RETRY; i++){
          try{
            AutoScalingClient.getInstance().deleteAutoScalingGroup(systemUserId, this.createdAutoScalingGroup, true);
            boolean asgFound=false;
            try{
              final DescribeAutoScalingGroupsResponseType resp = 
                  AutoScalingClient.getInstance().describeAutoScalingGroups(systemUserId, Lists.newArrayList(this.createdAutoScalingGroup));
              for(final AutoScalingGroupType asg :
                resp.getDescribeAutoScalingGroupsResult().getAutoScalingGroups().getMember()){
                if(this.createdAutoScalingGroup.equals(asg.getAutoScalingGroupName()))
                  asgFound=true; 
              }
            }catch(final Exception ex){
              asgFound=false;
            }
            if(asgFound)
              throw new Exception("Autoscaling group was not deleted");
            error = false;
            // willl terminate all instances
          }catch(final Exception ex){
            error = true;
            LOG.warn(String.format("Failed to delete autoscale group (%d'th attempt): %s", (i+1), this.createdAutoScalingGroup));
            try{
              long sleepMs = (i+1) * 500;
              Thread.sleep(sleepMs);
            }catch(final Exception ex2){
              ;
            }
          }
          if(!error)
            break;
        }
        
        if(error)
          throw new EventHandlerException("Failed to delete autoscaling group");
        
        // poll the state of instances
        do{
          final List<String> terminated = Lists.newArrayList();
          for(final String instanceId : instances){
            try{
              final List<RunningInstancesItemType> runningInstances =
                  Ec2Client.getInstance().describeInstances(systemUserId, Lists.newArrayList(instanceId));
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
    public List<String> getResult() {
      if(this.createdAutoScalingGroup!=null)
        return Lists.newArrayList(this.createdAutoScalingGroup);
      else
        return Lists.newArrayList();
    } 

    public static String getAutoscalingGroupName(final String acctNumber, final String dbName){
      return String.format("asg-euca-internal-db-%s-%s", acctNumber, dbName); 
    }
  }
  
  public static class CreateTags extends AbstractEventHandler<NewDBInstanceEvent> implements StoredResult<String>{
    private String tagValue = null;
    private String tagKey = "Name";
    private String taggedSgroupId = null;
    private String taggedAsgName = null;
    
    public CreateTags(EventHandlerChain<NewDBInstanceEvent> chain){
      super(chain);
      this.tagValue = DatabaseServerProperties.DEFAULT_LAUNCHER_TAG;
    }
    
    @Override
    public void apply(NewDBInstanceEvent evt)  throws EventHandlerException {
      // security group
      String asgName = null;
      try{
        asgName = this.getChain().findHandler(CreateAutoScalingGroup.class).getResult().get(0);
      }catch(final Exception ex){
        throw new EventHandlerException("failed to get autoscaling group name", ex);
      }
      final String systemUserId = getSystemUserId();
      
      try{
        final DescribeAutoScalingGroupsResponseType resp =
            AutoScalingClient.getInstance().describeAutoScalingGroups(systemUserId, Lists.newArrayList(asgName));
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
          AutoScalingClient.getInstance().createOrUpdateAutoscalingTags(systemUserId, tagKey, this.tagValue, asgName);
          this.taggedAsgName = asgName;
        }catch(final Exception ex){
          throw new EventHandlerException("failed to tag autoscaling group", ex);
        }
      }
      
      String sgroupName = null;
      String sgroupId = null;
      try{
        sgroupName = this.getChain().findHandler(SecurityGroupSetup.class).getResult().get(0);
        List<SecurityGroupItemType> groups = 
            Ec2Client.getInstance().describeSecurityGroups(systemUserId, Lists.newArrayList(sgroupName));
        if(groups!=null && groups.size()>0){
          final SecurityGroupItemType current = groups.get(0);
          if(sgroupName.equals(current.getGroupName())){
              sgroupId= current.getGroupId();
          }
        }
        if(sgroupId==null)
          throw new Exception(String.format("No security group named %s is found", sgroupName));
      }catch(final Exception ex){
        throw new EventHandlerException("failed to get security group name", ex);
      }
      
      try{
        final List<TagInfo> tags = Ec2Client.getInstance().describeTags(systemUserId, Lists.newArrayList("value"), 
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
          Ec2Client.getInstance().createTags(systemUserId, tagKey, tagValue, Lists.newArrayList(sgroupId));
          taggedSgroupId = sgroupId;
        }catch(final Exception ex){
          throw new EventHandlerException("failed to tag security group", ex);
        }
      }
    }

    @Override
    public void rollback() throws EventHandlerException {
      if(this.tagKey != null && this.tagValue !=null){
        final String systemUserId = getSystemUserId();
        if(this.taggedSgroupId!=null){
          try{
            Ec2Client.getInstance()
              .deleteTags(systemUserId, this.tagKey, this.tagValue, Lists.newArrayList(this.taggedSgroupId));
          }catch(final Exception ex){
            ;
          }
        }
        
        if(this.taggedAsgName!=null){
          try{
            AutoScalingClient.getInstance()
              .deleteAutoscalingTags(systemUserId, this.tagKey, this.tagValue, this.taggedAsgName);
          }catch(final Exception ex){
            ;
          }
        }
      }
    }

    @Override
    public List<String> getResult() {
      return Lists.newArrayList(this.tagValue);
    }
  }
}

/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.loadbalancing.workflow;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.ActivityRegistrationOptions;
import com.amazonaws.services.simpleworkflow.flow.common.FlowConstants;
import com.eucalyptus.loadbalancing.common.msgs.HealthCheck;
import com.eucalyptus.loadbalancing.common.msgs.Listener;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerServoDescription;
import com.eucalyptus.loadbalancing.common.msgs.PolicyDescription;

/**
 * @author Sang-Min Park (sangmin.park@hpe.com)
 *
 */
@Activities(version="1.0")
@ActivityRegistrationOptions(
    defaultTaskHeartbeatTimeoutSeconds = FlowConstants.NONE,
    defaultTaskScheduleToCloseTimeoutSeconds = 180,
    defaultTaskScheduleToStartTimeoutSeconds = 120,
    defaultTaskStartToCloseTimeoutSeconds = 60)
public interface LoadBalancingActivities {
  /***** CreateLoadBalancer activities  *****/
  boolean createLbAdmissionControl(String accountNumber, String lbName, String[] zones) throws LoadBalancingActivityException;
  String iamRoleSetup(String accountNumber, String lbName) throws LoadBalancingActivityException;
  String instanceProfileSetup(String accountNumber, String lbName, String roleName) throws LoadBalancingActivityException;
  String iamPolicySetup(String accountNumber, String lbName, String roleName) throws LoadBalancingActivityException;
  SecurityGroupSetupActivityResult securityGroupSetup(String accountNumber, String lbName) throws LoadBalancingActivityException;
  void securityGroupSetupRollback(String accountNumber, String lbName, SecurityGroupSetupActivityResult result);
  CreateTagActivityResult createLbTagCreator(String accountNumber, String lbName, String sgroupId) throws LoadBalancingActivityException;
  void createLbTagCreatorRollback(CreateTagActivityResult result);
  AutoscalingGroupSetupActivityResult autoscalingGroupSetup(String accountNumber, String lbName, String instanceProfileName, String securityGroupName, List<String> zones, Map<String,String> zoneToSubnetIdMap) 
      throws LoadBalancingActivityException;
  void autoscalingGroupSetupRollback(String accountNumber, String lbName, AutoscalingGroupSetupActivityResult result );
  /***** END of CreateLoadBalancer activities  *****/

  
  /***** EnableZone activities *****/
  /// returned list contains the availability zones that's persisted
  List<String> enableAvailabilityZonesPersistUpdatedZones(String accountNumber, String lbName, List<String> zonesToEnable, Map<String,String> zoneToSubnetIdMap)
      throws LoadBalancingActivityException;
  void enableAvailabilityZonesPersistUpdatedZonesRollback(String accountNumber, String lbName, List<String> zonesToRollback);
  void enableAvailabilityZonesPersistBackendInstanceState(String accountNumber, String lbName, List<String> enabledZones)
      throws LoadBalancingActivityException;
  /***** END of EnableZone activities *****/
  
  /***** DisableZone activities *****/
  /// returned list contains the instance IDs that's updated
  List<String> disableAvailabilityZonesPersistRetiredServoInstances(String accountNumber, String lbName, List<String> zonesToDisable) 
      throws LoadBalancingActivityException;
  void disableAvailabilityZonesPersistRetiredServoInstancesRollback(String accountNumber, String lbName, List<String> updatedInstanceIds) ;
  /// returned list contains the AZ whose corresponding autoscaling group is updated
  List<String> disableAvailabilityZonesUpdateAutoScalingGroup(String accountNumber, String lbName, List<String> zonesToDisable)
      throws LoadBalancingActivityException;
  void disableAvailabilityZonesUpdateAutoScalingGroupRollback(String accountNumber, String lbName, List<String> updatedZones);
  void disableAvailabilityZonesPersistUpdatedZones(String accountNumber, String lbName, List<String> zonesToDisable)
      throws LoadBalancingActivityException;
  void disableAvailabilityZonesPersistBackendInstanceState(String accountNumber, String lbName, List<String> zonesToDisable)
      throws LoadBalancingActivityException;
  /***** END of DisableZone activities *****/
  
  /***** CreateListener activities *****/
  void createListenerCheckSSLCertificateId(String accountNumber, String lbName, Listener[] listeners)
      throws LoadBalancingActivityException;
  /// returned list contains the list of policy names added
  AuthorizeSSLCertificateActivityResult createListenerAuthorizeSSLCertificate(String accountNumber, String lbName, Listener[] listeners)
      throws LoadBalancingActivityException;
  void createListenerAuthorizeSSLCertificateRollback(String accountNumber, String lbName, AuthorizeSSLCertificateActivityResult result);
  AuthorizeIngressRuleActivityResult createListenerAuthorizeIngressRule(String accountNumber, String lbName, Listener[] listeners)
      throws LoadBalancingActivityException; 
  void createListenerAuthorizeIngressRuleRollback(String accountNumber, String lbName, AuthorizeIngressRuleActivityResult result);
  void createListenerUpdateHealthCheckConfig(String accountNumber, String lbName, Listener[] listeners)
      throws LoadBalancingActivityException;
  void createListenerAddDefaultSSLPolicy(String accountNumber, String lbName, Listener[] listeners)
      throws LoadBalancingActivityException;
  /***** END of CreateListener activities *****/

  /***** DeleteListener activities *****/
  void deleteListenerRevokeSSLCertificatePolicy(String accountNumber, String lbName, 
      List<Integer> portsToDelete)
      throws LoadBalancingActivityException;
  void deleteListenerRevokeIngressRule(String accountNumber, String lbName, 
      List<Integer> portsToDelete)
      throws LoadBalancingActivityException;
  /***** END of DeleteListener activities *****/

  /***** DeleteLoadBalancer activities *****/
  void deleteLoadBalancerDeactivateDns(String accountNumber, String lbName);
  void deleteLoadBalancerDeleteScalingGroup(String accountNumber, String lbName) 
      throws LoadBalancingActivityException;
  void deleteLoadBalancerDeleteInstanceProfile(String accountNumber, String lbName);
  void deleteLoadBalancerDeleteIamRole(String accountNumber, String lbName);
  void deleteLoadBalancerDeleteSecurityGroup(String accountNumber, String lbName);
  /***** END of DeleteLoadBalancer activities *****/
  
  /***** ModifyAttributes activities *****/
  AccessLogPolicyActivityResult modifyLoadBalancerAttributesCreateAccessLogPolicy(String accountNumber, String lbName,
                                                                                  Boolean accessLogEnabled, String s3BucketName,
                                                                                  String s3BucketPrefix, Integer emitInterval) throws LoadBalancingActivityException;
  void modifyLoadBalancerAttributesCreateAccessLogPolicyRollback(String accountNumber, String lbName, AccessLogPolicyActivityResult result);
  void modifyLoadBalancerAttributesDeleteAccessLogPolicy(String accountNumber, String lbName,
                                                         Boolean accessLogEnabled, String s3BucketName,
                                                         String s3BucketPrefix, Integer emitInterval)
      throws LoadBalancingActivityException;
  void modifyLoadBalancerAttributesPersistAttributes(String accountNumber, String lbName,
                                                     Boolean accessLogEnabled, String s3BucketName,
                                                     String s3BucketPrefix, Integer emitInterval)
      throws LoadBalancingActivityException;
  /***** END ModifyAttributes activities *****/
  
  /***** ApplySecurityGroups activities *****/
  void applySecurityGroupUpdateSecurityGroup(String accountNumber, String lbName, Map<String,String> groupIdToNames)
          throws LoadBalancingActivityException;
  /***** END ApplySecurityGroups activities ****/
  
  /***** ModifyProperties activities *****/
  void modifyServicePropertiesValidateRequest(String emi, String instanceType,
      String keyname, String initScript) throws LoadBalancingActivityException;
  void modifyServicePropertiesUpdateScalingGroup(String emi, String instanceType,
      String keyname, String initScript) throws LoadBalancingActivityException;
  
  /***** Activities for ELB VMs *****/
  /// to update servo VMs with the latest ELB
  List<String> lookupServoInstances(String accountNumber, String lbName) throws LoadBalancingActivityException;
  List<String> listLoadBalancerPolicies(String accountNumber, String lbName) throws LoadBalancingActivityException;
  PolicyDescription getLoadBalancerPolicy(String accountNumber, String lbName, String policyName) throws LoadBalancingActivityException;
  Map<String, LoadBalancerServoDescription> lookupLoadBalancerDescription(String accountNumber, String lbName)
      throws LoadBalancingActivityException;

  /// backend instance status update
  /// Because servo VM is not aware of avaiability zones of the registered instances (in cross-zone lb),
  /// it monitors and reports the status info. about all registered instances.
  /// This activity is to filter out reports for which the servo VM is not responsible for reporting the status
  /// In other words, servo VM in AZ A should only report the status about instances in AZ A.
  HealthCheck lookupLoadBalancerHealthCheck(String accountNumber, String lbName)
          throws LoadBalancingActivityException;
  Map<String, String> filterInstanceStatus(final String accountNumber, final String lbName,
                            final String servoInstanceId, final String status)
          throws LoadBalancingActivityException;
  void updateInstanceStatus(String accountNumber, String lbName, Map<String,String> statusList)
      throws LoadBalancingActivityException;
  /// cloudwatch put metrics
  void putCloudWatchInstanceHealth(String accountNumber, String lbName)
      throws LoadBalancingActivityException;
  public void putCloudWatchMetrics(String accountNumber, String lbName,
      Map<String,String> metric) throws LoadBalancingActivityException;
  /***** END of Activities for ELB VMs *****/

  
  /***** Activities for monitoring all ELBs in the system *****/
    // discover new servos, update the state of existing ones, or delete the terminated servo VMs
  void checkServoInstances() throws LoadBalancingActivityException;
  void checkServoInstanceDns() throws LoadBalancingActivityException;
  void checkBackendInstances() throws LoadBalancingActivityException;
  void cleanupSecurityGroups() throws LoadBalancingActivityException;
  void cleanupServoInstances() throws LoadBalancingActivityException;
  void runContinousWorkflows() throws LoadBalancingActivityException;
  void recycleFailedServoInstances() throws LoadBalancingActivityException;
       // for each LB, there are workflows that must continue to run
       // if for any reason the workflows are terminated, this actvity should re-run it
  /**** END Activities for monitoring all ELBs in the system *****/

  void recordInstanceTaskFailure(String instanceId) throws LoadBalancingActivityException;
  /***** Upgrade activities  *****/
  void upgrade4_4() throws LoadBalancingActivityException; // to make sure that all ELB VMs have the right role policy
  /***** END Upgrade activities  *****/
}
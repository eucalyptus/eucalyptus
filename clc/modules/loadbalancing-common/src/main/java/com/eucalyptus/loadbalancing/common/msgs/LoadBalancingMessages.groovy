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
@GroovyAddClassUUID
package com.eucalyptus.loadbalancing.common.msgs

import com.eucalyptus.loadbalancing.common.LoadBalancing
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

import com.eucalyptus.cloudwatch.common.msgs.MetricData;
import java.lang.reflect.Field
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID


public class LoadBalancingServoMessage extends LoadBalancingMessage {
  String sourceIp
}

public class DescribeLoadBalancersByServoType extends LoadBalancingServoMessage {
	String instanceId;
	public DescribeLoadBalancersByServoType() {  }
}

public class DescribeLoadBalancersByServoResponseType extends LoadBalancingServoMessage {
	public DescribeLoadBalancersResponseType() {  }
	DescribeLoadBalancersResult describeLoadBalancersResult = new DescribeLoadBalancersResult();
	ResponseMetadata responseMetadata = new ResponseMetadata();
}

public class PutServoStatesType extends LoadBalancingServoMessage {
	String instanceId;
	
	@HttpEmbedded
	Instances instances;
	
	@HttpEmbedded
	MetricData metricData;
	public PutServoceStatesType(){}
}

public class PutServoStatesResponseType extends LoadBalancingServoMessage {
	public PutServoStatesResponseType() { }
	PutServoStatesResult putServoStatesResult = new PutServoStatesResult();
	ResponseMetadata responseMetadata = new ResponseMetadata();
}

public class PutServoStatesResult extends EucalyptusData {
	public PutServoStatesResult() { }
}

public class CreateLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  Listeners listeners;
  @HttpEmbedded
  AvailabilityZones availabilityZones;
  @HttpEmbedded
  Subnets subnets;
  @HttpEmbedded
  SecurityGroups securityGroups;
  String scheme;
  public CreateLoadBalancerType() {  }
}

@ComponentMessage(LoadBalancing.class)
public class LoadBalancingMessage extends BaseMessage {
	@Override
	def <TYPE extends BaseMessage> TYPE getReply() {
	  TYPE type = super.getReply()
	  try {
		Field responseMetadataField = type.class.getDeclaredField("responseMetadata")
		responseMetadataField.setAccessible( true )
		((ResponseMetadata) responseMetadataField.get( type )).requestId = getCorrelationId()
	  } catch ( Exception e ) {
	  }
	  return type
	}
}
public class CreateLoadBalancerResponseType extends LoadBalancingMessage {
  public CreateLoadBalancerResponseType() {  }
  CreateLoadBalancerResult createLoadBalancerResult = new CreateLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class CreateLoadBalancerPolicyResult extends EucalyptusData {
  public CreateLoadBalancerPolicyResult() {  }
}
public class DeregisterInstancesFromLoadBalancerResponseType extends LoadBalancingMessage {
  public DeregisterInstancesFromLoadBalancerResponseType() {  }
  DeregisterInstancesFromLoadBalancerResult deregisterInstancesFromLoadBalancerResult = new DeregisterInstancesFromLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class CreateAppCookieStickinessPolicyResult extends EucalyptusData {
  public CreateAppCookieStickinessPolicyResult() {  }
}
public class Listeners extends EucalyptusData {
  public Listeners() {  }
  
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Listener> member = new ArrayList<Listener>();
}
public class DeleteLoadBalancerListenersResult extends EucalyptusData {
  public DeleteLoadBalancerListenersResult() {  }
}
public class DescribeLoadBalancerPolicyTypesType extends LoadBalancingMessage {
  @HttpEmbedded
  PolicyTypeNames policyTypeNames;
  public DescribeLoadBalancerPolicyTypesType() {  }
}
public class CreateLoadBalancerPolicyResponseType extends LoadBalancingMessage {
  public CreateLoadBalancerPolicyResponseType() {  }
  CreateLoadBalancerPolicyResult createLoadBalancerPolicyResult = new CreateLoadBalancerPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class CreateLoadBalancerListenersResponseType extends LoadBalancingMessage {
  public CreateLoadBalancerListenersResponseType() {  }
  CreateLoadBalancerListenersResult createLoadBalancerListenersResult = new CreateLoadBalancerListenersResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class SetLoadBalancerPoliciesForBackendServerResponseType extends LoadBalancingMessage {
  public SetLoadBalancerPoliciesForBackendServerResponseType() {  }
  SetLoadBalancerPoliciesForBackendServerResult setLoadBalancerPoliciesForBackendServerResult = new SetLoadBalancerPoliciesForBackendServerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteLoadBalancerResult extends EucalyptusData {
  public DeleteLoadBalancerResult() {  }
}
public class DeregisterInstancesFromLoadBalancerResult extends EucalyptusData {
  @HttpEmbedded
  Instances instances;
  public DeregisterInstancesFromLoadBalancerResult() {  }
}
public class RegisterInstancesWithLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded 
  Instances instances;
  public RegisterInstancesWithLoadBalancerType() {  }
  public RegisterInstancesWithLoadBalancerType( String loadBalancerName,
                                                Collection<String> instanceIds ) {
    this.loadBalancerName = loadBalancerName
    this.instances = new Instances( member: instanceIds.collect{ String instanceId -> new Instance( instanceId: instanceId ) } as ArrayList<Instance> )
  }
}
public class AttachLoadBalancerToSubnetsResponseType extends LoadBalancingMessage {
  public AttachLoadBalancerToSubnetsResponseType() {  }
  AttachLoadBalancerToSubnetsResult attachLoadBalancerToSubnetsResult = new AttachLoadBalancerToSubnetsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class LoadBalancerDescription extends EucalyptusData {
  String loadBalancerName;
  String dnsName;
  String canonicalHostedZoneName;
  String canonicalHostedZoneNameID;
  @HttpEmbedded
  ListenerDescriptions listenerDescriptions;
  @HttpEmbedded
  Policies policies;
  @HttpEmbedded
  BackendServerDescriptions backendServerDescriptions;
  @HttpEmbedded
  AvailabilityZones availabilityZones;
  @HttpEmbedded
  Subnets subnets;
  String vpcId;
  @HttpEmbedded
  Instances instances;
  @HttpEmbedded
  HealthCheck healthCheck;
  @HttpEmbedded
  SourceSecurityGroup sourceSecurityGroup;
  @HttpEmbedded
  SecurityGroups securityGroups;
  
  Date createdTime;
  String scheme;
  public LoadBalancerDescription() {  }
}
public class LBCookieStickinessPolicies extends EucalyptusData {
  public LBCookieStickinessPolicies() {  }
  
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<LBCookieStickinessPolicy> member = new ArrayList<LBCookieStickinessPolicy>();
}
public class PolicyTypeDescriptions extends EucalyptusData {
  public PolicyTypeDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyTypeDescription> member = new ArrayList<PolicyTypeDescription>();
}
public class Ports extends EucalyptusData {
  public Ports() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class CreateAppCookieStickinessPolicyResponseType extends LoadBalancingMessage {
  public CreateAppCookieStickinessPolicyResponseType() {  }
  CreateAppCookieStickinessPolicyResult createAppCookieStickinessPolicyResult = new CreateAppCookieStickinessPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class EnableAvailabilityZonesForLoadBalancerResponseType extends LoadBalancingMessage {
  public EnableAvailabilityZonesForLoadBalancerResponseType() {  }
  EnableAvailabilityZonesForLoadBalancerResult enableAvailabilityZonesForLoadBalancerResult = new EnableAvailabilityZonesForLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Subnets extends EucalyptusData {
  public Subnets() {  }  
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class ApplySecurityGroupsToLoadBalancerResult extends EucalyptusData {
  @HttpEmbedded
  SecurityGroups securityGroups;
  public ApplySecurityGroupsToLoadBalancerResult() {  }
}
public class InstanceStates extends EucalyptusData {
  public InstanceStates() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<InstanceState> member = new ArrayList<InstanceState>();
}
public class DescribeInstanceHealthResponseType extends LoadBalancingMessage {
  public DescribeInstanceHealthResponseType() {  }
  DescribeInstanceHealthResult describeInstanceHealthResult = new DescribeInstanceHealthResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Error extends EucalyptusData {
  String type;
  String code;
  String message;
  public Error() {  }  
  @HttpEmbedded
  ErrorDetail detail = new ErrorDetail();
}
public class DeleteLoadBalancerPolicyResult extends EucalyptusData {
  public DeleteLoadBalancerPolicyResult() {  }
}
public class DescribeLoadBalancerPolicyTypesResponseType extends LoadBalancingMessage {
  public DescribeLoadBalancerPolicyTypesResponseType() {  }
  DescribeLoadBalancerPolicyTypesResult describeLoadBalancerPolicyTypesResult = new DescribeLoadBalancerPolicyTypesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class CreateLBCookieStickinessPolicyResult extends EucalyptusData {
  public CreateLBCookieStickinessPolicyResult() {  }
}
public class DescribeLoadBalancerPoliciesType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  PolicyNames policyNames;
  public DescribeLoadBalancerPoliciesType() {  }
}
public class PolicyAttributeDescriptions extends EucalyptusData {
  public PolicyAttributeDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyAttributeDescription> member = new ArrayList<PolicyAttributeDescription>();
}
public class DescribeLoadBalancerPoliciesResponseType extends LoadBalancingMessage {
  public DescribeLoadBalancerPoliciesResponseType() {  }
  DescribeLoadBalancerPoliciesResult describeLoadBalancerPoliciesResult = new DescribeLoadBalancerPoliciesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  public DeleteLoadBalancerType() {  }
}
public class Instance extends EucalyptusData {
  String instanceId;
  public Instance() {  }
}
public class DisableAvailabilityZonesForLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  AvailabilityZones availabilityZones;
  public DisableAvailabilityZonesForLoadBalancerType() {  }
}
public class RegisterInstancesWithLoadBalancerResponseType extends LoadBalancingMessage {
  public RegisterInstancesWithLoadBalancerResponseType() {  }
  RegisterInstancesWithLoadBalancerResult registerInstancesWithLoadBalancerResult = new RegisterInstancesWithLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Listener extends EucalyptusData {
  String protocol;
  Integer loadBalancerPort;
  String instanceProtocol;
  Integer instancePort;
  String SSLCertificateId;
  public Listener() {  }
}
public class SetLoadBalancerPoliciesForBackendServerType extends LoadBalancingMessage {
  String loadBalancerName;
  Integer instancePort;
  @HttpEmbedded
  PolicyNames policyNames;
  public SetLoadBalancerPoliciesForBackendServerType() {  }
}
public class AttachLoadBalancerToSubnetsResult extends EucalyptusData {
  @HttpEmbedded
  Subnets subnets;
  public AttachLoadBalancerToSubnetsResult() {  }
}
public class SetLoadBalancerPoliciesOfListenerResponseType extends LoadBalancingMessage {
  public SetLoadBalancerPoliciesOfListenerResponseType() {  }
  SetLoadBalancerPoliciesOfListenerResult setLoadBalancerPoliciesOfListenerResult = new SetLoadBalancerPoliciesOfListenerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class HealthCheck extends EucalyptusData {
  String target;
  Integer interval;
  Integer timeout;
  Integer unhealthyThreshold;
  Integer healthyThreshold;
  public HealthCheck() {  }
}
public class DescribeLoadBalancerPolicyTypesResult extends EucalyptusData {
  @HttpEmbedded
  PolicyTypeDescriptions policyTypeDescriptions;
  public DescribeLoadBalancerPolicyTypesResult() {  }
}
public class PolicyTypeDescription extends EucalyptusData {
  String policyTypeName;
  String description;
  @HttpEmbedded
  PolicyAttributeTypeDescriptions policyAttributeTypeDescriptions;
  public PolicyTypeDescription() {  }
}
public class DisableAvailabilityZonesForLoadBalancerResponseType extends LoadBalancingMessage {
  public DisableAvailabilityZonesForLoadBalancerResponseType() {  }
  DisableAvailabilityZonesForLoadBalancerResult disableAvailabilityZonesForLoadBalancerResult = new DisableAvailabilityZonesForLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeLoadBalancersResponseType extends LoadBalancingMessage {
  public DescribeLoadBalancersResponseType() {  }
  DescribeLoadBalancersResult describeLoadBalancersResult = new DescribeLoadBalancersResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class AppCookieStickinessPolicy extends EucalyptusData {
  String policyName;
  String cookieName;
  public AppCookieStickinessPolicy() {  }
}
public class SetLoadBalancerListenerSSLCertificateType extends LoadBalancingMessage {
  String loadBalancerName;
  Integer loadBalancerPort;
  String SSLCertificateId;
  public SetLoadBalancerListenerSSLCertificateType() {  }
}
public class PolicyTypeNames extends EucalyptusData {
  public PolicyTypeNames() {  }

  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class ListenerDescription extends EucalyptusData {
  @HttpEmbedded	
  Listener listener;
  @HttpEmbedded
  PolicyNames policyNames;
  public ListenerDescription() {  }
}
public class LoadBalancerNames extends EucalyptusData {
  public LoadBalancerNames() {  }
  
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class SetLoadBalancerPoliciesOfListenerType extends LoadBalancingMessage {
  String loadBalancerName;
  Integer loadBalancerPort;
  @HttpEmbedded
  PolicyNames policyNames;
  public SetLoadBalancerPoliciesOfListenerType() {  }
}
public class DetachLoadBalancerFromSubnetsResult extends EucalyptusData {
  @HttpEmbedded
  Subnets subnets;
  public DetachLoadBalancerFromSubnetsResult() {  }
}
public class ListenerDescriptions extends EucalyptusData {
  public ListenerDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<ListenerDescription> member = new ArrayList<ListenerDescription>();
}
public class DeleteLoadBalancerListenersType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  Ports loadBalancerPorts;
  public DeleteLoadBalancerListenersType() {  }
}
public class PolicyNames extends EucalyptusData {
  public PolicyNames() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class EnableAvailabilityZonesForLoadBalancerResult extends EucalyptusData {
  @HttpEmbedded
  AvailabilityZones availabilityZones;
  public EnableAvailabilityZonesForLoadBalancerResult() {  }
}
public class DetachLoadBalancerFromSubnetsType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  Subnets subnets;
  public DetachLoadBalancerFromSubnetsType() {  }
}
public class PolicyAttributeTypeDescriptions extends EucalyptusData {
  public PolicyAttributeTypeDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyAttributeTypeDescription> member = new ArrayList<PolicyAttributeTypeDescription>();
}
public class CreateLoadBalancerListenersResult extends EucalyptusData {
  public CreateLoadBalancerListenersResult() {  }
}
public class DeleteLoadBalancerResponseType extends LoadBalancingMessage {
  public DeleteLoadBalancerResponseType() {  }
  DeleteLoadBalancerResult deleteLoadBalancerResult = new DeleteLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DeleteLoadBalancerPolicyType extends LoadBalancingMessage {
  String loadBalancerName;
  String policyName;
  public DeleteLoadBalancerPolicyType() {  }
}
public class CreateLoadBalancerPolicyType extends LoadBalancingMessage {
  String loadBalancerName;
  String policyName;
  String policyTypeName;
  @HttpEmbedded
  PolicyAttributes policyAttributes;
  public CreateLoadBalancerPolicyType() {  }
}
public class ConfigureHealthCheckType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  HealthCheck healthCheck;
  public ConfigureHealthCheckType() {  }
}
public class CreateAppCookieStickinessPolicyType extends LoadBalancingMessage {
  String loadBalancerName;
  String policyName;
  String cookieName;
  public CreateAppCookieStickinessPolicyType() {  }
}
public class ConfigureHealthCheckResult extends EucalyptusData {
  @HttpEmbedded
  HealthCheck healthCheck;
  public ConfigureHealthCheckResult() {  }
}
public class Instances extends EucalyptusData {
  public Instances() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Instance> member = new ArrayList<Instance>();
}
public class AvailabilityZones extends EucalyptusData {
  public AvailabilityZones() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class SourceSecurityGroup extends EucalyptusData {
  String ownerAlias;
  String groupName;
  public SourceSecurityGroup() {  }
}
public class DetachLoadBalancerFromSubnetsResponseType extends LoadBalancingMessage {
  public DetachLoadBalancerFromSubnetsResponseType() {  }
  DetachLoadBalancerFromSubnetsResult detachLoadBalancerFromSubnetsResult = new DetachLoadBalancerFromSubnetsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class ErrorDetail extends EucalyptusData {
  public ErrorDetail() {  }
}
public class DescribeLoadBalancersResult extends EucalyptusData {
  @HttpEmbedded
  LoadBalancerDescriptions loadBalancerDescriptions;
  String nextMarker;
  public DescribeLoadBalancersResult() {  }
}
public class DeleteLoadBalancerListenersResponseType extends LoadBalancingMessage {
  public DeleteLoadBalancerListenersResponseType() {  }
  DeleteLoadBalancerListenersResult deleteLoadBalancerListenersResult = new DeleteLoadBalancerListenersResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class LoadBalancerDescriptions extends EucalyptusData {
  public LoadBalancerDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<LoadBalancerDescription> member = new ArrayList<LoadBalancerDescription>();
}
public class BackendServerDescription extends EucalyptusData {
  Integer instancePort;
  @HttpEmbedded
  PolicyNames policyNames;
  public BackendServerDescription() {  }
}
public class DescribeLoadBalancerPoliciesResult extends EucalyptusData {
  @HttpEmbedded
  PolicyDescriptions policyDescriptions;
  public DescribeLoadBalancerPoliciesResult() {  }
}
public class RegisterInstancesWithLoadBalancerResult extends EucalyptusData {
  @HttpEmbedded
  Instances instances;
  public RegisterInstancesWithLoadBalancerResult() {  }
}
public class PolicyAttribute extends EucalyptusData {
  String attributeName;
  String attributeValue;
  public PolicyAttribute() {  }
}
public class CreateLoadBalancerListenersType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  Listeners listeners;
  public CreateLoadBalancerListenersType() {  }
}
public class ConfigureHealthCheckResponseType extends LoadBalancingMessage {
  public ConfigureHealthCheckResponseType() {  }
  ConfigureHealthCheckResult configureHealthCheckResult = new ConfigureHealthCheckResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DisableAvailabilityZonesForLoadBalancerResult extends EucalyptusData {
  @HttpEmbedded
  AvailabilityZones availabilityZones;
  public DisableAvailabilityZonesForLoadBalancerResult() {  }
}
public class SetLoadBalancerListenerSSLCertificateResponseType extends LoadBalancingMessage {
  public SetLoadBalancerListenerSSLCertificateResponseType() {  }
  SetLoadBalancerListenerSSLCertificateResult setLoadBalancerListenerSSLCertificateResult = new SetLoadBalancerListenerSSLCertificateResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Policies extends EucalyptusData {
  @HttpEmbedded
  AppCookieStickinessPolicies appCookieStickinessPolicies;
  @HttpEmbedded
  LBCookieStickinessPolicies lbCookieStickinessPolicies;
  @HttpEmbedded
  PolicyNames otherPolicies;
  public Policies() {  }
}
public class CreateLoadBalancerResult extends EucalyptusData {
  String dnsName;
  public CreateLoadBalancerResult() {  }
}
public class SecurityGroups extends EucalyptusData {
  public SecurityGroups() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
public class CreateLBCookieStickinessPolicyResponseType extends LoadBalancingMessage {
  public CreateLBCookieStickinessPolicyResponseType() {  }
  CreateLBCookieStickinessPolicyResult createLBCookieStickinessPolicyResult = new CreateLBCookieStickinessPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class LBCookieStickinessPolicy extends EucalyptusData {
  String policyName;
  Long cookieExpirationPeriod;
  public LBCookieStickinessPolicy() {  }
}
public class SetLoadBalancerPoliciesForBackendServerResult extends EucalyptusData {
  public SetLoadBalancerPoliciesForBackendServerResult() {  }
}
public class EnableAvailabilityZonesForLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  AvailabilityZones availabilityZones;
  public EnableAvailabilityZonesForLoadBalancerType() {  }
}
public class DeleteLoadBalancerPolicyResponseType extends LoadBalancingMessage {
  public DeleteLoadBalancerPolicyResponseType() {  }
  DeleteLoadBalancerPolicyResult deleteLoadBalancerPolicyResult = new DeleteLoadBalancerPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DescribeInstanceHealthType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  Instances instances;
  public DescribeInstanceHealthType() {  }
  public DescribeInstanceHealthType( String loadBalancerName,
                                     Collection<String> instanceIds ) {
    this.loadBalancerName = loadBalancerName
    this.instances = new Instances( member: instanceIds.collect{ String instanceId -> new Instance( instanceId: instanceId ) } as ArrayList<Instance> )
  }
}
public class CreateLBCookieStickinessPolicyType extends LoadBalancingMessage {
  String loadBalancerName;
  String policyName;
  Long cookieExpirationPeriod;
  public CreateLBCookieStickinessPolicyType() {  }
}
public class PolicyDescription extends EucalyptusData {
  String policyName;
  String policyTypeName;
  @HttpEmbedded
  PolicyAttributeDescriptions policyAttributeDescriptions;
  public PolicyDescription() {  }
}
public class InstanceState extends EucalyptusData {
  String instanceId;
  String state;
  String reasonCode;
  String description;
  public InstanceState() {  }
}
public class DescribeLoadBalancersType extends LoadBalancingMessage {
  @HttpEmbedded
  LoadBalancerNames loadBalancerNames;
  String marker;
  public DescribeLoadBalancersType() {  }
}
public class DeregisterInstancesFromLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  Instances instances;
  public DeregisterInstancesFromLoadBalancerType() {  }
  public DeregisterInstancesFromLoadBalancerType( String loadBalancerName,
                                                  Collection<String> instanceIds ) {
    this.loadBalancerName = loadBalancerName
    this.instances = new Instances( member: instanceIds.collect{ String instanceId -> new Instance( instanceId: instanceId ) } as ArrayList<Instance> )
  }
}
public class AttachLoadBalancerToSubnetsType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  Subnets subnets;
  public AttachLoadBalancerToSubnetsType() {  }
}
public class ApplySecurityGroupsToLoadBalancerResponseType extends LoadBalancingMessage {
  public ApplySecurityGroupsToLoadBalancerResponseType() {  }
  ApplySecurityGroupsToLoadBalancerResult applySecurityGroupsToLoadBalancerResult = new ApplySecurityGroupsToLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class AppCookieStickinessPolicies extends EucalyptusData {
  public AppCookieStickinessPolicies() {  }
  ArrayList<AppCookieStickinessPolicy> member = new ArrayList<AppCookieStickinessPolicy>();
}
public class ResponseMetadata extends EucalyptusData {
  String requestId;
  public ResponseMetadata() {  }
}
public class BackendServerDescriptions extends EucalyptusData {
  public BackendServerDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<BackendServerDescription> member = new ArrayList<BackendServerDescription>();
}
public class PolicyAttributes extends EucalyptusData {
  public PolicyAttributes() {  }
  
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyAttribute> member = new ArrayList<PolicyAttribute>();
}
public class ErrorResponse extends LoadBalancingMessage { // EucalyptusData {
  String requestId;
  public ErrorResponse() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="error")
  ArrayList<Error> error = new ArrayList<Error>();
}
public class ApplySecurityGroupsToLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  @HttpEmbedded
  SecurityGroups securityGroups;
  public ApplySecurityGroupsToLoadBalancerType() {  }
}
public class SetLoadBalancerPoliciesOfListenerResult extends EucalyptusData {
  public SetLoadBalancerPoliciesOfListenerResult() {  }
}
public class DescribeInstanceHealthResult extends EucalyptusData {
  @HttpEmbedded
  InstanceStates instanceStates;
  public DescribeInstanceHealthResult() {  }
}
public class PolicyAttributeTypeDescription extends EucalyptusData {
  String attributeName;
  String attributeType;
  String description;
  String defaultValue;
  String cardinality;
  public PolicyAttributeTypeDescription() {  }
}
public class PolicyDescriptions extends EucalyptusData {
  public PolicyDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyDescription> member = new ArrayList<PolicyDescription>();
}
public class PolicyAttributeDescription extends EucalyptusData {
  String attributeName;
  String attributeValue;
  public PolicyAttributeDescription() {  }
}
public class SetLoadBalancerListenerSSLCertificateResult extends EucalyptusData {
  public SetLoadBalancerListenerSSLCertificateResult() {  }
}

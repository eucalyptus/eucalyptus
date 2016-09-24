/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
import com.eucalyptus.util.MessageValidation
import com.eucalyptus.ws.WebServiceError
import com.google.common.base.Function
import com.google.common.collect.Maps
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

import com.eucalyptus.cloudwatch.common.msgs.MetricData

import javax.annotation.Nonnull
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

import static com.eucalyptus.loadbalancing.common.msgs.LoadBalancingMessageValidation.*
import static com.eucalyptus.util.MessageValidation.validateRecursively

class LoadBalancerServoDescriptions extends EucalyptusData {
	ArrayList<LoadBalancerServoDescription> member = new ArrayList<LoadBalancerServoDescription>()
}

class LoadBalancerServoDescription extends EucalyptusData {
	String loadBalancerName
	String dnsName
	String canonicalHostedZoneName
	String canonicalHostedZoneNameID
	ListenerDescriptions listenerDescriptions
  PolicyDescriptions policyDescriptions
	BackendServerDescriptions backendServerDescriptions
	AvailabilityZones availabilityZones
	Subnets subnets
	String vpcId
	BackendInstances backendInstances
	HealthCheck healthCheck
	SourceSecurityGroup sourceSecurityGroup
	SecurityGroups securityGroups
	Date createdTime
	String scheme
	LoadBalancerAttributes loadBalancerAttributes
}

/* EUCA-specific extension to describe backend-instances passed to LB VM */
class BackendInstances extends EucalyptusData {
  BackendInstances() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  @FieldRange( min = 1l )
  ArrayList<BackendInstance> member = new ArrayList<BackendInstance>();
}

class BackendInstance extends EucalyptusData {
  @Nonnull
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_INSTANCE_ID_OPTIONAL_STATUS )
  String instanceId;
  
  @Nonnull
  String instanceIpAddress;
  
  /* true if instance is in the same zone as servo
   * false otherwise
   */
  Boolean reportHealthCheck;

  BackendInstance() {  }

  static Function<BackendInstance,String> instanceId( ) {
    return { BackendInstance instance -> instance.instanceId } as Function<BackendInstance,String>
  }

  static Function<String,BackendInstance> instance( ) {
    return { String instanceId -> new BackendInstance( instanceId: instanceId ) } as Function<String,BackendInstance>
  }
}

class CreateLoadBalancerType extends LoadBalancingMessage {
  @Nonnull
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_NAME )
  String loadBalancerName;
  @Nonnull
  Listeners listeners;
  AvailabilityZones availabilityZones;
  Subnets subnets;
  SecurityGroups securityGroups;
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_SCHEME )
  String scheme;
  TagList tags
  CreateLoadBalancerType() {  }

}

@ComponentMessage(LoadBalancing.class)
class LoadBalancingMessage extends BaseMessage implements MessageValidation.ValidatableMessage {
  @Override
  def <TYPE extends BaseMessage> TYPE getReply() {
    TYPE type = super.getReply()
    try {
      ((ResponseMetadata)((GroovyObject)type).getProperty( "responseMetadata" )).requestId = getCorrelationId( )
    } catch ( Exception e ) {
    }
    return type
  }

  Map<String,String> validate( ) {
    validateRecursively(
        Maps.<String,String>newTreeMap( ),
        new LoadBalancingMessageValidationAssistant( ),
        "",
        this )
  }
}
class CreateLoadBalancerResponseType extends LoadBalancingMessage {
  CreateLoadBalancerResponseType() {  }
  CreateLoadBalancerResult createLoadBalancerResult = new CreateLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class CreateLoadBalancerPolicyResult extends EucalyptusData {
  CreateLoadBalancerPolicyResult() {  }
}
class DeregisterInstancesFromLoadBalancerResponseType extends LoadBalancingMessage {
  DeregisterInstancesFromLoadBalancerResponseType() {  }
  DeregisterInstancesFromLoadBalancerResult deregisterInstancesFromLoadBalancerResult = new DeregisterInstancesFromLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class CreateAppCookieStickinessPolicyResult extends EucalyptusData {
  CreateAppCookieStickinessPolicyResult() {  }
}
class Listeners extends EucalyptusData {
  Listeners() {  }
  
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Listener> member = new ArrayList<Listener>();
}
class DeleteLoadBalancerListenersResult extends EucalyptusData {
  DeleteLoadBalancerListenersResult() {  }
}
class DescribeLoadBalancerPolicyTypesType extends LoadBalancingMessage {
  PolicyTypeNames policyTypeNames;
  DescribeLoadBalancerPolicyTypesType() {  }
}
class CreateLoadBalancerPolicyResponseType extends LoadBalancingMessage {
  CreateLoadBalancerPolicyResponseType() {  }
  CreateLoadBalancerPolicyResult createLoadBalancerPolicyResult = new CreateLoadBalancerPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class CreateLoadBalancerListenersResponseType extends LoadBalancingMessage {
  CreateLoadBalancerListenersResponseType() {  }
  CreateLoadBalancerListenersResult createLoadBalancerListenersResult = new CreateLoadBalancerListenersResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class SetLoadBalancerPoliciesForBackendServerResponseType extends LoadBalancingMessage {
  SetLoadBalancerPoliciesForBackendServerResponseType() {  }
  SetLoadBalancerPoliciesForBackendServerResult setLoadBalancerPoliciesForBackendServerResult = new SetLoadBalancerPoliciesForBackendServerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class DeleteLoadBalancerResult extends EucalyptusData {
  DeleteLoadBalancerResult() {  }
}
class DeregisterInstancesFromLoadBalancerResult extends EucalyptusData {
  Instances instances;
  DeregisterInstancesFromLoadBalancerResult() {  }
}
class RegisterInstancesWithLoadBalancerType extends LoadBalancingMessage {
  @Nonnull
  String loadBalancerName;
  @Nonnull
  Instances instances;
  RegisterInstancesWithLoadBalancerType() {  }
  RegisterInstancesWithLoadBalancerType( String loadBalancerName,
                                                Collection<String> instanceIds ) {
    this.loadBalancerName = loadBalancerName
    this.instances = new Instances( member: instanceIds.collect{ String instanceId -> new Instance( instanceId: instanceId ) } as ArrayList<Instance> )
  }
}
class AttachLoadBalancerToSubnetsResponseType extends LoadBalancingMessage {
  AttachLoadBalancerToSubnetsResponseType() {  }
  AttachLoadBalancerToSubnetsResult attachLoadBalancerToSubnetsResult = new AttachLoadBalancerToSubnetsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class LoadBalancerDescription extends EucalyptusData {
  String loadBalancerName;
  String dnsName;
  String canonicalHostedZoneName;
  String canonicalHostedZoneNameID;
  ListenerDescriptions listenerDescriptions;
  Policies policies;
  BackendServerDescriptions backendServerDescriptions;
  AvailabilityZones availabilityZones;
  Subnets subnets;
  String vpcId;
  Instances instances;
  HealthCheck healthCheck;
  SourceSecurityGroup sourceSecurityGroup;
  SecurityGroups securityGroups;
  Date createdTime;
  String scheme;
}
class LBCookieStickinessPolicies extends EucalyptusData {
  LBCookieStickinessPolicies() {  }
  
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<LBCookieStickinessPolicy> member = new ArrayList<LBCookieStickinessPolicy>();
}
class PolicyTypeDescriptions extends EucalyptusData {
  PolicyTypeDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyTypeDescription> member = new ArrayList<PolicyTypeDescription>();
}
class Ports extends EucalyptusData {
  Ports() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
class CreateAppCookieStickinessPolicyResponseType extends LoadBalancingMessage {
  CreateAppCookieStickinessPolicyResponseType() {  }
  CreateAppCookieStickinessPolicyResult createAppCookieStickinessPolicyResult = new CreateAppCookieStickinessPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class EnableAvailabilityZonesForLoadBalancerResponseType extends LoadBalancingMessage {
  EnableAvailabilityZonesForLoadBalancerResponseType() {  }
  EnableAvailabilityZonesForLoadBalancerResult enableAvailabilityZonesForLoadBalancerResult = new EnableAvailabilityZonesForLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class Subnets extends EucalyptusData {
  Subnets() {  }
  Subnets( Collection<String> subnetId ) { member.addAll( subnetId?:[] ) }
  @HttpParameterMapping(parameter="member")
  @Nonnull
  @FieldRange( min = 1l )
  @FieldRegex( FieldRegexValue.EC2_SUBNET_ID )
  ArrayList<String> member = new ArrayList<String>();
}
class ApplySecurityGroupsToLoadBalancerResult extends EucalyptusData {
  SecurityGroups securityGroups;
  ApplySecurityGroupsToLoadBalancerResult() {  }
}
class InstanceStates extends EucalyptusData {
  InstanceStates() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<InstanceState> member = new ArrayList<InstanceState>();
}
class DescribeInstanceHealthResponseType extends LoadBalancingMessage {
  DescribeInstanceHealthResponseType() {  }
  DescribeInstanceHealthResult describeInstanceHealthResult = new DescribeInstanceHealthResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class Error extends EucalyptusData {
  String type;
  String code;
  String message;
  Error() {  }  
  ErrorDetail detail = new ErrorDetail();
}
class DeleteLoadBalancerPolicyResult extends EucalyptusData {
  DeleteLoadBalancerPolicyResult() {  }
}
class DescribeLoadBalancerPolicyTypesResponseType extends LoadBalancingMessage {
  DescribeLoadBalancerPolicyTypesResponseType() {  }
  DescribeLoadBalancerPolicyTypesResult describeLoadBalancerPolicyTypesResult = new DescribeLoadBalancerPolicyTypesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class CreateLBCookieStickinessPolicyResult extends EucalyptusData {
  CreateLBCookieStickinessPolicyResult() {  }
}
class DescribeLoadBalancerPoliciesType extends LoadBalancingMessage {
  String loadBalancerName;
  PolicyNames policyNames;
  DescribeLoadBalancerPoliciesType() {  }
}
class PolicyAttributeDescriptions extends EucalyptusData {
  PolicyAttributeDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyAttributeDescription> member = new ArrayList<PolicyAttributeDescription>();
}
class DescribeLoadBalancerPoliciesResponseType extends LoadBalancingMessage {
  DescribeLoadBalancerPoliciesResponseType() {  }
  DescribeLoadBalancerPoliciesResult describeLoadBalancerPoliciesResult = new DescribeLoadBalancerPoliciesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class DeleteLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  DeleteLoadBalancerType() {  }
}
class Instance extends EucalyptusData {
  @Nonnull
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_INSTANCE_ID_OPTIONAL_STATUS )
  String instanceId;

  Instance() {  }

  static Function<Instance,String> instanceId( ) {
    return { Instance instance -> instance.instanceId } as Function<Instance,String>
  }

  static Function<String,Instance> instance( ) {
    return { String instanceId -> new Instance( instanceId: instanceId ) } as Function<String,Instance>
  }
}
class DisableAvailabilityZonesForLoadBalancerType extends LoadBalancingMessage {
  @Nonnull
  String loadBalancerName;
  @Nonnull
  AvailabilityZones availabilityZones;
  DisableAvailabilityZonesForLoadBalancerType() {  }
}
class RegisterInstancesWithLoadBalancerResponseType extends LoadBalancingMessage {
  RegisterInstancesWithLoadBalancerResponseType() {  }
  RegisterInstancesWithLoadBalancerResult registerInstancesWithLoadBalancerResult = new RegisterInstancesWithLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class Listener extends EucalyptusData {
  String protocol;
  Integer loadBalancerPort;
  String instanceProtocol;
  Integer instancePort;
  String SSLCertificateId;
  Listener() {  }
}
class SetLoadBalancerPoliciesForBackendServerType extends LoadBalancingMessage {
  String loadBalancerName;
  Integer instancePort;
  PolicyNames policyNames;
  SetLoadBalancerPoliciesForBackendServerType() {  }
}
class AttachLoadBalancerToSubnetsResult extends EucalyptusData {
  Subnets subnets;
  AttachLoadBalancerToSubnetsResult() {  }
}
class SetLoadBalancerPoliciesOfListenerResponseType extends LoadBalancingMessage {
  SetLoadBalancerPoliciesOfListenerResponseType() {  }
  SetLoadBalancerPoliciesOfListenerResult setLoadBalancerPoliciesOfListenerResult = new SetLoadBalancerPoliciesOfListenerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class HealthCheck extends EucalyptusData {
  String target;
  Integer interval;
  Integer timeout;
  Integer unhealthyThreshold;
  Integer healthyThreshold;
  HealthCheck() {  }
}
class DescribeLoadBalancerPolicyTypesResult extends EucalyptusData {
  PolicyTypeDescriptions policyTypeDescriptions;
  DescribeLoadBalancerPolicyTypesResult() {  }
}
class PolicyTypeDescription extends EucalyptusData {
  String policyTypeName;
  String description;
  PolicyAttributeTypeDescriptions policyAttributeTypeDescriptions;
  PolicyTypeDescription() {  }
}
class DisableAvailabilityZonesForLoadBalancerResponseType extends LoadBalancingMessage {
  DisableAvailabilityZonesForLoadBalancerResponseType() {  }
  DisableAvailabilityZonesForLoadBalancerResult disableAvailabilityZonesForLoadBalancerResult = new DisableAvailabilityZonesForLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class DescribeLoadBalancersResponseType extends LoadBalancingMessage {
  DescribeLoadBalancersResponseType() {  }
  DescribeLoadBalancersResult describeLoadBalancersResult = new DescribeLoadBalancersResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class AppCookieStickinessPolicy extends EucalyptusData {
  String policyName;
  String cookieName;
  AppCookieStickinessPolicy() {  }
}
class SetLoadBalancerListenerSSLCertificateType extends LoadBalancingMessage {
  String loadBalancerName;
  Integer loadBalancerPort;
  String SSLCertificateId;
  SetLoadBalancerListenerSSLCertificateType() {  }
}
class PolicyTypeNames extends EucalyptusData {
  PolicyTypeNames() {  }

  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
class ListenerDescription extends EucalyptusData {
  Listener listener;
  PolicyNames policyNames;
  ListenerDescription() {  }
}
class LoadBalancerNames extends EucalyptusData {
  LoadBalancerNames() {  }
  
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
class SetLoadBalancerPoliciesOfListenerType extends LoadBalancingMessage {
  String loadBalancerName;
  Integer loadBalancerPort;
  PolicyNames policyNames;
  SetLoadBalancerPoliciesOfListenerType() {  }
}
class DetachLoadBalancerFromSubnetsResult extends EucalyptusData {
  Subnets subnets;
  DetachLoadBalancerFromSubnetsResult() {  }
}
class ListenerDescriptions extends EucalyptusData {
  ListenerDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<ListenerDescription> member = new ArrayList<ListenerDescription>();
}
class DeleteLoadBalancerListenersType extends LoadBalancingMessage {
  String loadBalancerName;
  Ports loadBalancerPorts;
  DeleteLoadBalancerListenersType() {  }
}
class PolicyNames extends EucalyptusData {
  PolicyNames() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
class EnableAvailabilityZonesForLoadBalancerResult extends EucalyptusData {
  AvailabilityZones availabilityZones;
  EnableAvailabilityZonesForLoadBalancerResult() {  }
}
class DetachLoadBalancerFromSubnetsType extends LoadBalancingMessage {
  @Nonnull
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_NAME )
  String loadBalancerName;
  @Nonnull
  Subnets subnets;
  DetachLoadBalancerFromSubnetsType() {  }
}
class PolicyAttributeTypeDescriptions extends EucalyptusData {
  PolicyAttributeTypeDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyAttributeTypeDescription> member = new ArrayList<PolicyAttributeTypeDescription>();
}
class CreateLoadBalancerListenersResult extends EucalyptusData {
  CreateLoadBalancerListenersResult() {  }
}
class DeleteLoadBalancerResponseType extends LoadBalancingMessage {
  DeleteLoadBalancerResponseType() {  }
  DeleteLoadBalancerResult deleteLoadBalancerResult = new DeleteLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class DeleteLoadBalancerPolicyType extends LoadBalancingMessage {
  String loadBalancerName;
  String policyName;
  DeleteLoadBalancerPolicyType() {  }
}
class CreateLoadBalancerPolicyType extends LoadBalancingMessage {
  String loadBalancerName;
  String policyName;
  String policyTypeName;
  PolicyAttributes policyAttributes;
  CreateLoadBalancerPolicyType() {  }
}
class ConfigureHealthCheckType extends LoadBalancingMessage {
  String loadBalancerName;
  HealthCheck healthCheck;
  ConfigureHealthCheckType() {  }
}
class CreateAppCookieStickinessPolicyType extends LoadBalancingMessage {
  String loadBalancerName;
  String policyName;
  String cookieName;
  CreateAppCookieStickinessPolicyType() {  }
}
class ConfigureHealthCheckResult extends EucalyptusData {
  HealthCheck healthCheck;
  ConfigureHealthCheckResult() {  }
}
class Instances extends EucalyptusData {
  Instances() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  @FieldRange( min = 1l )
  ArrayList<Instance> member = new ArrayList<Instance>();
}
class AvailabilityZones extends EucalyptusData {
  AvailabilityZones() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
class SourceSecurityGroup extends EucalyptusData {
  String ownerAlias
  String groupName
  SourceSecurityGroup() {  }
  SourceSecurityGroup( String ownerAlias, String groupName ) {
    this.ownerAlias = ownerAlias
    this.groupName = groupName
  }
}
class DetachLoadBalancerFromSubnetsResponseType extends LoadBalancingMessage {
  DetachLoadBalancerFromSubnetsResponseType() {  }
  DetachLoadBalancerFromSubnetsResult detachLoadBalancerFromSubnetsResult = new DetachLoadBalancerFromSubnetsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class ErrorDetail extends EucalyptusData {
  ErrorDetail() {  }
}
class DescribeLoadBalancersResult extends EucalyptusData {
  LoadBalancerDescriptions loadBalancerDescriptions;
  String nextMarker;
  DescribeLoadBalancersResult() {  }
}
class DeleteLoadBalancerListenersResponseType extends LoadBalancingMessage {
  DeleteLoadBalancerListenersResponseType() {  }
  DeleteLoadBalancerListenersResult deleteLoadBalancerListenersResult = new DeleteLoadBalancerListenersResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class LoadBalancerDescriptions extends EucalyptusData {
  LoadBalancerDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<LoadBalancerDescription> member = new ArrayList<LoadBalancerDescription>();
}
class BackendServerDescription extends EucalyptusData {
  Integer instancePort;
  PolicyNames policyNames;
  BackendServerDescription() {  }
}
class DescribeLoadBalancerPoliciesResult extends EucalyptusData {
  PolicyDescriptions policyDescriptions;
  DescribeLoadBalancerPoliciesResult() {  }
}
class RegisterInstancesWithLoadBalancerResult extends EucalyptusData {
  Instances instances;
  RegisterInstancesWithLoadBalancerResult() {  }
}
class PolicyAttribute extends EucalyptusData {
  String attributeName;
  String attributeValue;
  PolicyAttribute() {  }
}
class CreateLoadBalancerListenersType extends LoadBalancingMessage {
  String loadBalancerName;
  Listeners listeners;
  CreateLoadBalancerListenersType() {  }
}
class ConfigureHealthCheckResponseType extends LoadBalancingMessage {
  ConfigureHealthCheckResponseType() {  }
  ConfigureHealthCheckResult configureHealthCheckResult = new ConfigureHealthCheckResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class DisableAvailabilityZonesForLoadBalancerResult extends EucalyptusData {
  AvailabilityZones availabilityZones;
  DisableAvailabilityZonesForLoadBalancerResult() {  }
}
class SetLoadBalancerListenerSSLCertificateResponseType extends LoadBalancingMessage {
  SetLoadBalancerListenerSSLCertificateResponseType() {  }
  SetLoadBalancerListenerSSLCertificateResult setLoadBalancerListenerSSLCertificateResult = new SetLoadBalancerListenerSSLCertificateResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class Policies extends EucalyptusData {
  AppCookieStickinessPolicies appCookieStickinessPolicies;
  LBCookieStickinessPolicies lbCookieStickinessPolicies;
  PolicyNames otherPolicies;
  Policies() {  }
}
class CreateLoadBalancerResult extends EucalyptusData {
  String dnsName;
  CreateLoadBalancerResult() {  }
}
class SecurityGroups extends EucalyptusData {
  SecurityGroups() {  }
  SecurityGroups( final Collection<String> securityGroupIds ) { member.addAll( securityGroupIds?:[] )  }
  @HttpParameterMapping(parameter="member")
  @Nonnull
  @FieldRange( min = 1l, max = 5l )
  @FieldRegex( FieldRegexValue.EC2_SECURITY_GROUP_ID )
  ArrayList<String> member = new ArrayList<String>();
}
class CreateLBCookieStickinessPolicyResponseType extends LoadBalancingMessage {
  CreateLBCookieStickinessPolicyResponseType() {  }
  CreateLBCookieStickinessPolicyResult createLBCookieStickinessPolicyResult = new CreateLBCookieStickinessPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class LBCookieStickinessPolicy extends EucalyptusData {
  String policyName;
  Long cookieExpirationPeriod;
  LBCookieStickinessPolicy() {  }
}
class SetLoadBalancerPoliciesForBackendServerResult extends EucalyptusData {
  SetLoadBalancerPoliciesForBackendServerResult() {  }
}
class EnableAvailabilityZonesForLoadBalancerType extends LoadBalancingMessage {
  @Nonnull
  String loadBalancerName;
  @Nonnull
  AvailabilityZones availabilityZones;
  EnableAvailabilityZonesForLoadBalancerType() {  }
}
class DeleteLoadBalancerPolicyResponseType extends LoadBalancingMessage {
  DeleteLoadBalancerPolicyResponseType() {  }
  DeleteLoadBalancerPolicyResult deleteLoadBalancerPolicyResult = new DeleteLoadBalancerPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class DescribeInstanceHealthType extends LoadBalancingMessage {
  String loadBalancerName;
  Instances instances;
  DescribeInstanceHealthType() {  }
  DescribeInstanceHealthType( String loadBalancerName,
                                     Collection<String> instanceIds ) {
    this.loadBalancerName = loadBalancerName
    this.instances = instanceIds ?
        new Instances( member: instanceIds.collect{ String instanceId -> new Instance( instanceId: instanceId ) } as ArrayList<Instance> ) :
        null
  }
}
class CreateLBCookieStickinessPolicyType extends LoadBalancingMessage {
  String loadBalancerName;
  String policyName;
  Long cookieExpirationPeriod;
  CreateLBCookieStickinessPolicyType() {  }
}
class PolicyDescription extends EucalyptusData {
  String policyName;
  String policyTypeName;
  PolicyAttributeDescriptions policyAttributeDescriptions;
  PolicyDescription() {  }
}
class InstanceState extends EucalyptusData {
  String instanceId;
  String state;
  String reasonCode;
  String description;
  InstanceState() {  }
}
class DescribeLoadBalancersType extends LoadBalancingMessage {
  LoadBalancerNames loadBalancerNames;
  String marker;
  DescribeLoadBalancersType() {  }
}
class DeregisterInstancesFromLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  Instances instances;
  DeregisterInstancesFromLoadBalancerType() {  }
  DeregisterInstancesFromLoadBalancerType( String loadBalancerName,
                                                  Collection<String> instanceIds ) {
    this.loadBalancerName = loadBalancerName
    this.instances = new Instances( member: instanceIds.collect{ String instanceId -> new Instance( instanceId: instanceId ) } as ArrayList<Instance> )
  }
}
class AttachLoadBalancerToSubnetsType extends LoadBalancingMessage {
  @Nonnull
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_NAME )
  String loadBalancerName;
  @Nonnull
  Subnets subnets;
  AttachLoadBalancerToSubnetsType() {  }
}
class ApplySecurityGroupsToLoadBalancerResponseType extends LoadBalancingMessage {
  ApplySecurityGroupsToLoadBalancerResponseType() {  }
  ApplySecurityGroupsToLoadBalancerResult applySecurityGroupsToLoadBalancerResult = new ApplySecurityGroupsToLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class AppCookieStickinessPolicies extends EucalyptusData {
  AppCookieStickinessPolicies() {  }
  ArrayList<AppCookieStickinessPolicy> member = new ArrayList<AppCookieStickinessPolicy>();
}
class ResponseMetadata extends EucalyptusData {
  String requestId;
  ResponseMetadata() {  }
}
class BackendServerDescriptions extends EucalyptusData {
  BackendServerDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<BackendServerDescription> member = new ArrayList<BackendServerDescription>();
}
class PolicyAttributes extends EucalyptusData {
  PolicyAttributes() {  }
  
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyAttribute> member = new ArrayList<PolicyAttribute>();
}
class ErrorResponse extends LoadBalancingMessage implements WebServiceError {
  String requestId;
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="error")
  ArrayList<Error> error = new ArrayList<Error>( );

  ErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.getAt(0)?.type} error (${webServiceErrorCode}): ${webServiceErrorMessage}"
  }

  @Override
  String getWebServiceErrorCode( ) {
    error?.getAt(0)?.code
  }

  @Override
  String getWebServiceErrorMessage( ) {
    error?.getAt(0)?.message
  }
}
class ApplySecurityGroupsToLoadBalancerType extends LoadBalancingMessage {
  @Nonnull
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_NAME )
  String loadBalancerName;
  @Nonnull
  SecurityGroups securityGroups;
  ApplySecurityGroupsToLoadBalancerType() {  }
}
class SetLoadBalancerPoliciesOfListenerResult extends EucalyptusData {
  SetLoadBalancerPoliciesOfListenerResult() {  }
}
class DescribeInstanceHealthResult extends EucalyptusData {
  InstanceStates instanceStates;
  DescribeInstanceHealthResult() {  }
}
class PolicyAttributeTypeDescription extends EucalyptusData {
  String attributeName;
  String attributeType;
  String description;
  String defaultValue;
  String cardinality;
  PolicyAttributeTypeDescription() {  }
}
class PolicyDescriptions extends EucalyptusData {
  PolicyDescriptions() {  }
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<PolicyDescription> member = new ArrayList<PolicyDescription>();
}
class PolicyAttributeDescription extends EucalyptusData {
  String attributeName;
  String attributeValue;
  PolicyAttributeDescription() {  }
}
class SetLoadBalancerListenerSSLCertificateResult extends EucalyptusData {
  SetLoadBalancerListenerSSLCertificateResult() {  }
}
class TagList extends EucalyptusData {
  @Nonnull
  @FieldRange( min = 1l, max = 10l )
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<Tag> member = new ArrayList<Tag>();
}
class TagDescription extends EucalyptusData {
  String loadBalancerName;
  TagList tags;
}
class TagDescriptions extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  ArrayList<TagDescription> member = new ArrayList<TagDescription>();
}
class AddTagsType extends LoadBalancingMessage {
  @Nonnull
  LoadBalancerNames loadBalancerNames;
  @Nonnull
  TagList tags;
}
class DescribeTagsResult extends EucalyptusData {
  TagDescriptions tagDescriptions;
}
class DescribeTagsResponseType extends LoadBalancingMessage {
  DescribeTagsResult describeTagsResult = new DescribeTagsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class RemoveTagsResult extends EucalyptusData {
}
class AccessLog extends EucalyptusData {
  @Nonnull
  Boolean enabled;
  String s3BucketName;
  Integer emitInterval;
  String s3BucketPrefix;
}
class TagKeyList extends EucalyptusData {
  @HttpEmbedded(multiple=true)
  @HttpParameterMapping(parameter="member")
  @FieldRange( min = 1l )
  ArrayList<TagKeyOnly> member = new ArrayList<TagKeyOnly>();
}
class AddTagsResponseType extends LoadBalancingMessage {
  AddTagsResult addTagsResult = new AddTagsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class LoadBalancerNamesMax20 extends EucalyptusData {
  LoadBalancerNamesMax20() {  }
  @HttpParameterMapping(parameter="member")
  ArrayList<String> member = new ArrayList<String>();
}
class DescribeTagsType extends LoadBalancingMessage {
  LoadBalancerNamesMax20 loadBalancerNames;
}
class Tag extends EucalyptusData {
  @Nonnull
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_TAG_KEY )
  String key;
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_TAG_VALUE )
  String value;
}
class AddTagsResult extends EucalyptusData {
}
class RemoveTagsType extends LoadBalancingMessage {
  @Nonnull
  LoadBalancerNames loadBalancerNames;
  @Nonnull
  TagKeyList tags;
}
class TagKeyOnly extends EucalyptusData {
  @Nonnull
  @FieldRegex( FieldRegexValue.LOAD_BALANCER_TAG_KEY )
  String key;
}
class RemoveTagsResponseType extends LoadBalancingMessage {
  RemoveTagsResult removeTagsResult = new RemoveTagsResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class CrossZoneLoadBalancing extends EucalyptusData {
  @Nonnull
  Boolean enabled;
  CrossZoneLoadBalancing() {  }
}
class ConnectionDraining extends EucalyptusData {
  @Nonnull
  Boolean enabled;
  Integer timeout;
  ConnectionDraining() {  }
}
class ConnectionSettings extends EucalyptusData {
  @Nonnull
  @FieldRange( min=1l, max=3600l )
  Integer idleTimeout;
}
class LoadBalancerAttributes extends EucalyptusData {
  CrossZoneLoadBalancing crossZoneLoadBalancing;
  AccessLog accessLog;
  ConnectionDraining connectionDraining;
  ConnectionSettings connectionSettings;
}
class ModifyLoadBalancerAttributesType extends LoadBalancingMessage {
  @Nonnull
  String loadBalancerName;
  @Nonnull
  LoadBalancerAttributes loadBalancerAttributes;
}
class ModifyLoadBalancerAttributesResponseType extends LoadBalancingMessage {
  ModifyLoadBalancerAttributesResult modifyLoadBalancerAttributesResult = new ModifyLoadBalancerAttributesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class ModifyLoadBalancerAttributesResult extends EucalyptusData {
  String loadBalancerName;
  LoadBalancerAttributes loadBalancerAttributes;
}
class DescribeLoadBalancerAttributesType extends LoadBalancingMessage {
  String loadBalancerName;
}
class DescribeLoadBalancerAttributesResponseType extends LoadBalancingMessage {
  DescribeLoadBalancerAttributesResult describeLoadBalancerAttributesResult = new DescribeLoadBalancerAttributesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
class DescribeLoadBalancerAttributesResult extends EucalyptusData {
  LoadBalancerAttributes loadBalancerAttributes;
}


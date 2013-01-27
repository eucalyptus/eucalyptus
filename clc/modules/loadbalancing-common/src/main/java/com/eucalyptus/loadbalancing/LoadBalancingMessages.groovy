
package com.eucalyptus.loadbalancing;
import java.util.Date;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import java.math.BigInteger;
import java.util.ArrayList;

import com.eucalyptus.autoscaling.AutoScaling;
import com.eucalyptus.component.ComponentId;



public class CreateLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  Listeners listeners;
  AvailabilityZones availabilityZones;
  Subnets subnets;
  SecurityGroups securityGroups;
  String scheme;
  public CreateLoadBalancerType() {  }
}

@ComponentId.ComponentMessage(LoadBalancing.class)
public class LoadBalancingMessage extends BaseMessage {
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
public class Listeners extends LoadBalancingMessage {
  public Listeners() {  }
  ArrayList<Listener> member = new ArrayList<Listener>();
}
public class DeleteLoadBalancerListenersResult extends EucalyptusData {
  public DeleteLoadBalancerListenersResult() {  }
}
public class DescribeLoadBalancerPolicyTypesType extends LoadBalancingMessage {
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
  Instances instances;
  public DeregisterInstancesFromLoadBalancerResult() {  }
}
public class RegisterInstancesWithLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  Instances instances;
  public RegisterInstancesWithLoadBalancerType() {  }
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
  public LoadBalancerDescription() {  }
}
public class LBCookieStickinessPolicies extends EucalyptusData {
  public LBCookieStickinessPolicies() {  }
  ArrayList<LBCookieStickinessPolicy> member = new ArrayList<LBCookieStickinessPolicy>();
}
public class PolicyTypeDescriptions extends EucalyptusData {
  public PolicyTypeDescriptions() {  }
  ArrayList<PolicyTypeDescription> member = new ArrayList<PolicyTypeDescription>();
}
public class Ports extends EucalyptusData {
  public Ports() {  }
  ArrayList<BigInteger> member = new ArrayList<BigInteger>();
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
public class Subnets extends LoadBalancingMessage {
  public Subnets() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class ApplySecurityGroupsToLoadBalancerResult extends EucalyptusData {
  SecurityGroups securityGroups;
  public ApplySecurityGroupsToLoadBalancerResult() {  }
}
public class InstanceStates extends EucalyptusData {
  public InstanceStates() {  }
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
  PolicyNames policyNames;
  public DescribeLoadBalancerPoliciesType() {  }
}
public class PolicyAttributeDescriptions extends EucalyptusData {
  public PolicyAttributeDescriptions() {  }
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
  AvailabilityZones availabilityZones;
  public DisableAvailabilityZonesForLoadBalancerType() {  }
}
public class RegisterInstancesWithLoadBalancerResponseType extends LoadBalancingMessage {
  public RegisterInstancesWithLoadBalancerResponseType() {  }
  RegisterInstancesWithLoadBalancerResult registerInstancesWithLoadBalancerResult = new RegisterInstancesWithLoadBalancerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Listener extends LoadBalancingMessage {
  String protocol;
  BigInteger loadBalancerPort;
  String instanceProtocol;
  BigInteger instancePort;
  String sslCertificateId;
  public Listener() {  }
}
public class SetLoadBalancerPoliciesForBackendServerType extends LoadBalancingMessage {
  String loadBalancerName;
  BigInteger instancePort;
  PolicyNames policyNames;
  public SetLoadBalancerPoliciesForBackendServerType() {  }
}
public class AttachLoadBalancerToSubnetsResult extends EucalyptusData {
  Subnets subnets;
  public AttachLoadBalancerToSubnetsResult() {  }
}
public class SetLoadBalancerPoliciesOfListenerResponseType extends LoadBalancingMessage {
  public SetLoadBalancerPoliciesOfListenerResponseType() {  }
  SetLoadBalancerPoliciesOfListenerResult setLoadBalancerPoliciesOfListenerResult = new SetLoadBalancerPoliciesOfListenerResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class HealthCheck extends LoadBalancingMessage {
  String target;
  BigInteger interval;
  BigInteger timeout;
  BigInteger unhealthyThreshold;
  BigInteger healthyThreshold;
  public HealthCheck() {  }
}
public class DescribeLoadBalancerPolicyTypesResult extends EucalyptusData {
  PolicyTypeDescriptions policyTypeDescriptions;
  public DescribeLoadBalancerPolicyTypesResult() {  }
}
public class PolicyTypeDescription extends EucalyptusData {
  String policyTypeName;
  String description;
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
public class AppCookieStickinessPolicy extends LoadBalancingMessage {
  String policyName;
  String cookieName;
  public AppCookieStickinessPolicy() {  }
}
public class SetLoadBalancerListenerSSLCertificateType extends LoadBalancingMessage {
  String loadBalancerName;
  BigInteger loadBalancerPort;
  String sslCertificateId;
  public SetLoadBalancerListenerSSLCertificateType() {  }
}
public class PolicyTypeNames extends EucalyptusData {
  public PolicyTypeNames() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class ListenerDescription extends EucalyptusData {
  Listener listener;
  PolicyNames policyNames;
  public ListenerDescription() {  }
}
public class LoadBalancerNames extends EucalyptusData {
  public LoadBalancerNames() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class SetLoadBalancerPoliciesOfListenerType extends LoadBalancingMessage {
  String loadBalancerName;
  BigInteger loadBalancerPort;
  PolicyNames policyNames;
  public SetLoadBalancerPoliciesOfListenerType() {  }
}
public class DetachLoadBalancerFromSubnetsResult extends EucalyptusData {
  Subnets subnets;
  public DetachLoadBalancerFromSubnetsResult() {  }
}
public class ListenerDescriptions extends EucalyptusData {
  public ListenerDescriptions() {  }
  ArrayList<ListenerDescription> member = new ArrayList<ListenerDescription>();
}
public class DeleteLoadBalancerListenersType extends LoadBalancingMessage {
  String loadBalancerName;
  Ports loadBalancerPorts;
  public DeleteLoadBalancerListenersType() {  }
}
public class PolicyNames extends EucalyptusData {
  public PolicyNames() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class EnableAvailabilityZonesForLoadBalancerResult extends EucalyptusData {
  AvailabilityZones availabilityZones;
  public EnableAvailabilityZonesForLoadBalancerResult() {  }
}
public class DetachLoadBalancerFromSubnetsType extends LoadBalancingMessage {
  String loadBalancerName;
  Subnets subnets;
  public DetachLoadBalancerFromSubnetsType() {  }
}
public class PolicyAttributeTypeDescriptions extends EucalyptusData {
  public PolicyAttributeTypeDescriptions() {  }
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
  PolicyAttributes policyAttributes;
  public CreateLoadBalancerPolicyType() {  }
}
public class ConfigureHealthCheckType extends LoadBalancingMessage {
  String loadBalancerName;
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
  HealthCheck healthCheck;
  public ConfigureHealthCheckResult() {  }
}
public class Instances extends EucalyptusData {
  public Instances() {  }
  ArrayList<Instance> member = new ArrayList<Instance>();
}
public class AvailabilityZones extends EucalyptusData {
  public AvailabilityZones() {  }
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
  ArrayList<LoadBalancerDescription> member = new ArrayList<LoadBalancerDescription>();
}
public class BackendServerDescription extends EucalyptusData {
  BigInteger instancePort;
  PolicyNames policyNames;
  public BackendServerDescription() {  }
}
public class DescribeLoadBalancerPoliciesResult extends EucalyptusData {
  PolicyDescriptions policyDescriptions;
  public DescribeLoadBalancerPoliciesResult() {  }
}
public class RegisterInstancesWithLoadBalancerResult extends EucalyptusData {
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
  Listeners listeners;
  public CreateLoadBalancerListenersType() {  }
}
public class ConfigureHealthCheckResponseType extends LoadBalancingMessage {
  public ConfigureHealthCheckResponseType() {  }
  ConfigureHealthCheckResult configureHealthCheckResult = new ConfigureHealthCheckResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class DisableAvailabilityZonesForLoadBalancerResult extends EucalyptusData {
  AvailabilityZones availabilityZones;
  public DisableAvailabilityZonesForLoadBalancerResult() {  }
}
public class SetLoadBalancerListenerSSLCertificateResponseType extends LoadBalancingMessage {
  public SetLoadBalancerListenerSSLCertificateResponseType() {  }
  SetLoadBalancerListenerSSLCertificateResult setLoadBalancerListenerSSLCertificateResult = new SetLoadBalancerListenerSSLCertificateResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class Policies extends LoadBalancingMessage {
  AppCookieStickinessPolicies appCookieStickinessPolicies;
  LBCookieStickinessPolicies lbCookieStickinessPolicies;
  PolicyNames otherPolicies;
  public Policies() {  }
}
public class CreateLoadBalancerResult extends EucalyptusData {
  String dnsName;
  public CreateLoadBalancerResult() {  }
}
public class SecurityGroups extends EucalyptusData {
  public SecurityGroups() {  }
  ArrayList<String> member = new ArrayList<String>();
}
public class CreateLBCookieStickinessPolicyResponseType extends LoadBalancingMessage {
  public CreateLBCookieStickinessPolicyResponseType() {  }
  CreateLBCookieStickinessPolicyResult createLBCookieStickinessPolicyResult = new CreateLBCookieStickinessPolicyResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}
public class LBCookieStickinessPolicy extends LoadBalancingMessage {
  String policyName;
  Long cookieExpirationPeriod;
  public LBCookieStickinessPolicy() {  }
}
public class SetLoadBalancerPoliciesForBackendServerResult extends EucalyptusData {
  public SetLoadBalancerPoliciesForBackendServerResult() {  }
}
public class EnableAvailabilityZonesForLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
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
  Instances instances;
  public DescribeInstanceHealthType() {  }
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
  LoadBalancerNames loadBalancerNames;
  String marker;
  public DescribeLoadBalancersType() {  }
}
public class DeregisterInstancesFromLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  Instances instances;
  public DeregisterInstancesFromLoadBalancerType() {  }
}
public class AttachLoadBalancerToSubnetsType extends LoadBalancingMessage {
  String loadBalancerName;
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
  ArrayList<BackendServerDescription> member = new ArrayList<BackendServerDescription>();
}
public class PolicyAttributes extends EucalyptusData {
  public PolicyAttributes() {  }
  ArrayList<PolicyAttribute> member = new ArrayList<PolicyAttribute>();
}
public class ErrorResponse extends LoadBalancingMessage { // EucalyptusData {
  String requestId;
  public ErrorResponse() {  }
  ArrayList<Error> error = new ArrayList<Error>();
}
public class ApplySecurityGroupsToLoadBalancerType extends LoadBalancingMessage {
  String loadBalancerName;
  SecurityGroups securityGroups;
  public ApplySecurityGroupsToLoadBalancerType() {  }
}
public class SetLoadBalancerPoliciesOfListenerResult extends EucalyptusData {
  public SetLoadBalancerPoliciesOfListenerResult() {  }
}
public class DescribeInstanceHealthResult extends EucalyptusData {
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

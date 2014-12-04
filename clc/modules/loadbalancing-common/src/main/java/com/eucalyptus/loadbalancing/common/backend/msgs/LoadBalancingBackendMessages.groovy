/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 *
 * You should have received a copy of the GNU General License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
@GroovyAddClassUUID
package com.eucalyptus.loadbalancing.common.backend.msgs

import com.eucalyptus.loadbalancing.common.LoadBalancingBackend
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersByServoResult
import com.eucalyptus.loadbalancing.common.msgs.Instances
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancingMessage
import com.eucalyptus.loadbalancing.common.msgs.PutServoStatesResult
import com.eucalyptus.loadbalancing.common.msgs.ResponseMetadata

import com.eucalyptus.cloudwatch.common.msgs.MetricData
import edu.ucsb.eucalyptus.msgs.BaseMessageMarker
import edu.ucsb.eucalyptus.msgs.EucalyptusData

import com.eucalyptus.component.annotation.ComponentMessage;
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

class LoadBalancingServoBackendMessage extends LoadBalancingMessage implements LoadBalancingBackendMessage {
  String sourceIp
}

class DescribeLoadBalancersByServoType extends LoadBalancingServoBackendMessage {
  String instanceId;
  DescribeLoadBalancersByServoType() {  }
}

class DescribeLoadBalancersByServoResponseType extends LoadBalancingServoBackendMessage {
  DescribeLoadBalancersByServoResult describeLoadBalancersResult = new DescribeLoadBalancersByServoResult( );
  ResponseMetadata responseMetadata = new ResponseMetadata();
}

class PutServoStatesType extends LoadBalancingServoBackendMessage {
  String instanceId;

  Instances instances;

  MetricData metricData;
}

class PutServoStatesResponseType extends LoadBalancingServoBackendMessage {
  PutServoStatesResponseType() { }
  PutServoStatesResult putServoStatesResult = new PutServoStatesResult();
  ResponseMetadata responseMetadata = new ResponseMetadata();
}

class Error extends EucalyptusData {
  String type;
  String code;
  String message;
  Error() {  }
  ErrorDetail detail = new ErrorDetail();
}
class ErrorDetail extends EucalyptusData {
  ErrorDetail() {  }
}

@ComponentMessage(LoadBalancingBackend.class)
interface LoadBalancingBackendMessage extends BaseMessageMarker {
}


class AddTagsResponseType extends com.eucalyptus.loadbalancing.common.msgs.AddTagsResponseType implements LoadBalancingBackendMessage { }
class AddTagsType extends com.eucalyptus.loadbalancing.common.msgs.AddTagsType implements LoadBalancingBackendMessage { }
class ApplySecurityGroupsToLoadBalancerResponseType extends com.eucalyptus.loadbalancing.common.msgs.ApplySecurityGroupsToLoadBalancerResponseType implements LoadBalancingBackendMessage { }
class ApplySecurityGroupsToLoadBalancerType extends com.eucalyptus.loadbalancing.common.msgs.ApplySecurityGroupsToLoadBalancerType implements LoadBalancingBackendMessage { }
class AttachLoadBalancerToSubnetsResponseType extends com.eucalyptus.loadbalancing.common.msgs.AttachLoadBalancerToSubnetsResponseType implements LoadBalancingBackendMessage { }
class AttachLoadBalancerToSubnetsType extends com.eucalyptus.loadbalancing.common.msgs.AttachLoadBalancerToSubnetsType implements LoadBalancingBackendMessage { }
class ConfigureHealthCheckResponseType extends com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckResponseType implements LoadBalancingBackendMessage { }
class ConfigureHealthCheckType extends com.eucalyptus.loadbalancing.common.msgs.ConfigureHealthCheckType implements LoadBalancingBackendMessage { }
class CreateAppCookieStickinessPolicyResponseType extends com.eucalyptus.loadbalancing.common.msgs.CreateAppCookieStickinessPolicyResponseType implements LoadBalancingBackendMessage { }
class CreateAppCookieStickinessPolicyType extends com.eucalyptus.loadbalancing.common.msgs.CreateAppCookieStickinessPolicyType implements LoadBalancingBackendMessage { }
class CreateLBCookieStickinessPolicyResponseType extends com.eucalyptus.loadbalancing.common.msgs.CreateLBCookieStickinessPolicyResponseType implements LoadBalancingBackendMessage { }
class CreateLBCookieStickinessPolicyType extends com.eucalyptus.loadbalancing.common.msgs.CreateLBCookieStickinessPolicyType implements LoadBalancingBackendMessage { }
class CreateLoadBalancerListenersResponseType extends com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerListenersResponseType implements LoadBalancingBackendMessage { }
class CreateLoadBalancerListenersType extends com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerListenersType implements LoadBalancingBackendMessage { }
class CreateLoadBalancerPolicyResponseType extends com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerPolicyResponseType implements LoadBalancingBackendMessage { }
class CreateLoadBalancerPolicyType extends com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerPolicyType implements LoadBalancingBackendMessage { }
class CreateLoadBalancerResponseType extends com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerResponseType implements LoadBalancingBackendMessage { }
class CreateLoadBalancerType extends com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerType implements LoadBalancingBackendMessage { }
class DeleteLoadBalancerListenersResponseType extends com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerListenersResponseType implements LoadBalancingBackendMessage { }
class DeleteLoadBalancerListenersType extends com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerListenersType implements LoadBalancingBackendMessage { }
class DeleteLoadBalancerPolicyResponseType extends com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerPolicyResponseType implements LoadBalancingBackendMessage { }
class DeleteLoadBalancerPolicyType extends com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerPolicyType implements LoadBalancingBackendMessage { }
class DeleteLoadBalancerResponseType extends com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerResponseType implements LoadBalancingBackendMessage { }
class DeleteLoadBalancerType extends com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerType implements LoadBalancingBackendMessage { }
class DeregisterInstancesFromLoadBalancerResponseType extends com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerResponseType implements LoadBalancingBackendMessage { }
class DeregisterInstancesFromLoadBalancerType extends com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerType implements LoadBalancingBackendMessage { }
class DescribeInstanceHealthResponseType extends com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthResponseType implements LoadBalancingBackendMessage { }
class DescribeInstanceHealthType extends com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthType implements LoadBalancingBackendMessage { }
class DescribeLoadBalancerAttributesResponseType extends com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerAttributesResponseType implements LoadBalancingBackendMessage { }
class DescribeLoadBalancerAttributesType extends com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerAttributesType implements LoadBalancingBackendMessage { }
class DescribeLoadBalancerPoliciesResponseType extends com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPoliciesResponseType implements LoadBalancingBackendMessage { }
class DescribeLoadBalancerPoliciesType extends com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPoliciesType implements LoadBalancingBackendMessage { }
class DescribeLoadBalancerPolicyTypesResponseType extends com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPolicyTypesResponseType implements LoadBalancingBackendMessage { }
class DescribeLoadBalancerPolicyTypesType extends com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancerPolicyTypesType implements LoadBalancingBackendMessage { }
class DescribeLoadBalancersResponseType extends com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersResponseType implements LoadBalancingBackendMessage { }
class DescribeLoadBalancersType extends com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersType implements LoadBalancingBackendMessage { }
class DescribeTagsResponseType extends com.eucalyptus.loadbalancing.common.msgs.DescribeTagsResponseType implements LoadBalancingBackendMessage { }
class DescribeTagsType extends com.eucalyptus.loadbalancing.common.msgs.DescribeTagsType implements LoadBalancingBackendMessage { }
class DetachLoadBalancerFromSubnetsResponseType extends com.eucalyptus.loadbalancing.common.msgs.DetachLoadBalancerFromSubnetsResponseType implements LoadBalancingBackendMessage { }
class DetachLoadBalancerFromSubnetsType extends com.eucalyptus.loadbalancing.common.msgs.DetachLoadBalancerFromSubnetsType implements LoadBalancingBackendMessage { }
class DisableAvailabilityZonesForLoadBalancerResponseType extends com.eucalyptus.loadbalancing.common.msgs.DisableAvailabilityZonesForLoadBalancerResponseType implements LoadBalancingBackendMessage { }
class DisableAvailabilityZonesForLoadBalancerType extends com.eucalyptus.loadbalancing.common.msgs.DisableAvailabilityZonesForLoadBalancerType implements LoadBalancingBackendMessage { }
class EnableAvailabilityZonesForLoadBalancerResponseType extends com.eucalyptus.loadbalancing.common.msgs.EnableAvailabilityZonesForLoadBalancerResponseType implements LoadBalancingBackendMessage { }
class EnableAvailabilityZonesForLoadBalancerType extends com.eucalyptus.loadbalancing.common.msgs.EnableAvailabilityZonesForLoadBalancerType implements LoadBalancingBackendMessage { }
class ModifyLoadBalancerAttributesResponseType extends com.eucalyptus.loadbalancing.common.msgs.ModifyLoadBalancerAttributesResponseType implements LoadBalancingBackendMessage { }
class ModifyLoadBalancerAttributesType extends com.eucalyptus.loadbalancing.common.msgs.ModifyLoadBalancerAttributesType implements LoadBalancingBackendMessage { }
class RegisterInstancesWithLoadBalancerResponseType extends com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerResponseType implements LoadBalancingBackendMessage { }
class RegisterInstancesWithLoadBalancerType extends com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerType implements LoadBalancingBackendMessage { }
class RemoveTagsResponseType extends com.eucalyptus.loadbalancing.common.msgs.RemoveTagsResponseType implements LoadBalancingBackendMessage { }
class RemoveTagsType extends com.eucalyptus.loadbalancing.common.msgs.RemoveTagsType implements LoadBalancingBackendMessage { }
class SetLoadBalancerListenerSSLCertificateResponseType extends com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerListenerSSLCertificateResponseType implements LoadBalancingBackendMessage { }
class SetLoadBalancerListenerSSLCertificateType extends com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerListenerSSLCertificateType implements LoadBalancingBackendMessage { }
class SetLoadBalancerPoliciesForBackendServerResponseType extends com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesForBackendServerResponseType implements LoadBalancingBackendMessage { }
class SetLoadBalancerPoliciesForBackendServerType extends com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesForBackendServerType implements LoadBalancingBackendMessage { }
class SetLoadBalancerPoliciesOfListenerResponseType extends com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesOfListenerResponseType implements LoadBalancingBackendMessage { }
class SetLoadBalancerPoliciesOfListenerType extends com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesOfListenerType implements LoadBalancingBackendMessage { }



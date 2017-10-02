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
package com.eucalyptus.loadbalancing.ws

import com.eucalyptus.loadbalancing.common.msgs.AvailabilityZones
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerListenersType
import com.eucalyptus.loadbalancing.common.msgs.CreateLoadBalancerType
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerListenersType
import com.eucalyptus.loadbalancing.common.msgs.DeleteLoadBalancerType
import com.eucalyptus.loadbalancing.common.msgs.DeregisterInstancesFromLoadBalancerType
import com.eucalyptus.loadbalancing.common.msgs.DescribeInstanceHealthType
import com.eucalyptus.loadbalancing.common.msgs.DescribeLoadBalancersType
import com.eucalyptus.loadbalancing.common.msgs.Instance
import com.eucalyptus.loadbalancing.common.msgs.Instances
import com.eucalyptus.loadbalancing.common.msgs.Listener
import com.eucalyptus.loadbalancing.common.msgs.Listeners
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancerNames
import com.eucalyptus.loadbalancing.common.msgs.PolicyNames
import com.eucalyptus.loadbalancing.common.msgs.Ports
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerType
import com.eucalyptus.loadbalancing.common.msgs.SetLoadBalancerPoliciesOfListenerType
import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.junit.Test

/**
 *
 */
class LoadBalancingQueryBindingTest extends QueryBindingTestSupport {
  @Test
  void testValidBinding() {
    URL resource = LoadBalancingQueryBindingTest.class.getResource( '/loadbalancing-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = LoadBalancingQueryBindingTest.class.getResource( '/loadbalancing-binding.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testInternalRoundTrip() {
    URL resource = LoadBalancingQueryBindingTest.getResource('/loadbalancing-binding.xml')
    assertValidInternalRoundTrip( resource )
  }

  @Test
  void testMessageQueryBindings() {
    URL resource = LoadBalancingQueryBindingTest.class.getResource( '/loadbalancing-binding.xml' )
    LoadBalancingQueryBinding lbb = new LoadBalancingQueryBinding() {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass( final String operationName ) {
        createTestBindingFromXml( resource, operationName )
      }

      @Override
      protected void validateBinding( final com.eucalyptus.binding.Binding currentBinding,
                                      final String operationName,
                                      final Map<String, String> params,
                                      final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    };

    // CreateLoadBalancer
    bindAndAssertObject( lbb, CreateLoadBalancerType.class, "CreateLoadBalancer", new CreateLoadBalancerType(
        loadBalancerName: 'Name',
        availabilityZones: new AvailabilityZones( member: [ 'Zone1', 'Zone2' ] ),
        listeners : new Listeners (member: [
            new Listener(protocol:'http', loadBalancerPort: 80, instanceProtocol:'http'),
            new Listener(protocol:'tcp', loadBalancerPort: 22, instanceProtocol:'tcp')
        ])),
        9 );
    // DeleteLoadBalancer
    bindAndAssertObject(lbb, DeleteLoadBalancerType.class, "DeleteLoadBalancer", new DeleteLoadBalancerType(
        loadBalancerName: 'Name'
    ), 1);

    // DescribeLoadBalancers
    bindAndAssertObject( lbb, DescribeLoadBalancersType.class, "DescribeLoadBalancers", new DescribeLoadBalancersType(
        loadBalancerNames: new LoadBalancerNames( member: ['Name1', 'Name2'])
    ), 2);


    //RegisterInstancesWithLoadBalancer
    bindAndAssertObject( lbb, RegisterInstancesWithLoadBalancerType.class, "RegisterInstancesWithLoadBalancer", new RegisterInstancesWithLoadBalancerType(
        loadBalancerName : 'Name',
        instances : new Instances( member: [new Instance(instanceId: 'i-abcdefgh'), new Instance(instanceId:'i-12345678')])
    ), 3);

    //DeRegisterInstancesFromLoadBalancer
    bindAndAssertObject(lbb, DeregisterInstancesFromLoadBalancerType.class, "DeregisterInstancesFromLoadBalancer", new DeregisterInstancesFromLoadBalancerType(
        loadBalancerName : 'Name',
        instances : new Instances( member: [new Instance(instanceId: 'i-abcdefgh'), new Instance(instanceId:'i-12345678')])
    ), 3);

    // CreateLoadBalancerListeners
    bindAndAssertObject( lbb, CreateLoadBalancerListenersType.class, "CreateLoadBalancerListeners", new CreateLoadBalancerListenersType(
        loadBalancerName: 'Name',
        listeners : new Listeners (member: [
            new Listener(protocol:'http', loadBalancerPort: 80, instanceProtocol:'http'),
            new Listener(protocol:'tcp', loadBalancerPort: 22, instanceProtocol:'tcp')
        ])),
        7);

    // DeleteLoadBalancerListeners
    bindAndAssertObject( lbb, DeleteLoadBalancerListenersType.class, "DeleteLoadBalancerListeners", new DeleteLoadBalancerListenersType(
        loadBalancerName: 'Name',
        loadBalancerPorts : new Ports (member: ['80', '22'])),
        3);

    // ConfigureHealthCheck: TODO: Fix BigInt type in the message
    /*bindAndAssertObject( lbb, ConfigureHealthCheckType.class, "ConfigureHealthCheck", new ConfigureHealthCheckType(
      loadBalancerName: 'Name',
      healthCheck : new HealthCheck ( target: "target", interval: 100, timeout:1000, unhealthyThreshold:10, healthyThreshold:10)
      ), 6);
      */
    // DescribeInstanceHealth
    bindAndAssertObject( lbb, DescribeInstanceHealthType.class, "DescribeInstanceHealth", new DescribeInstanceHealthType(
        loadBalancerName: 'Name',
        instances : new Instances( member: [new Instance(instanceId: 'i-abcdefgh'), new Instance(instanceId:'i-12345678')])
    ), 3);

    // SetLoadBalancerPoliciesOfListener
    bindAndAssertParameters( lbb, SetLoadBalancerPoliciesOfListenerType.class, "SetLoadBalancerPoliciesOfListener", new SetLoadBalancerPoliciesOfListenerType(
        loadBalancerName: 'Name',
        loadBalancerPort: 80,
        policyNames: new PolicyNames( )
    ), [
        'LoadBalancerName': 'Name',
        'LoadBalancerPort': '80',
        'PolicyNames': ''
    ]);
  }
}

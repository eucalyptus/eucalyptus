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
package com.eucalyptus.loadbalancing.service.ws

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
import com.eucalyptus.loadbalancing.common.msgs.Ports
import com.eucalyptus.loadbalancing.common.msgs.RegisterInstancesWithLoadBalancerType
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


  }
}

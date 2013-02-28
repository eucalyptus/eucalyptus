package com.eucalyptus.loadbalancing

import java.util.Map;
import java.math.BigInteger;
import org.junit.Test

import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import com.eucalyptus.loadbalancing.ws.LoadBalancingQueryBinding

import edu.ucsb.eucalyptus.msgs.BaseMessage;

class LoadBalancingBindingTest extends QueryBindingTestSupport {
	@Test
	void testValidBinding() {
	  URL resource = LoadBalancingBindingTest.class.getResource( '/loadbalancing-binding.xml' )
	  assertValidBindingXml( resource )
	}
	
	@Test
	void testValidQueryBinding() {
	  URL resource = LoadBalancingBindingTest.class.getResource( '/loadbalancing-binding.xml' )
	  assertValidQueryBinding( resource )
	}
	
	@Test
	void testMessageQueryBindings() {
		URL resource = LoadBalancingBindingTest.class.getResource( '/loadbalancing-binding.xml' )
		LoadBalancingQueryBinding lbb = new LoadBalancingQueryBinding() {
			@Override
			protected Binding getBindingWithElementClass( final String operationName ) {
			  createTestBindingFromXml( resource, operationName )
			}
	  
			@Override
			protected void validateBinding( final Binding currentBinding,
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

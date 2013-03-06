package com.eucalyptus.autoscaling.activities

import com.eucalyptus.auth.Accounts
import com.eucalyptus.auth.api.AccountProvider
import com.eucalyptus.auth.principal.AccessKey
import com.eucalyptus.auth.principal.Account
import com.eucalyptus.auth.principal.Certificate
import com.eucalyptus.auth.principal.Group
import com.eucalyptus.auth.principal.Principals
import com.eucalyptus.auth.principal.User
import com.eucalyptus.autoscaling.configurations.LaunchConfiguration
import com.eucalyptus.autoscaling.configurations.LaunchConfigurations
import com.eucalyptus.autoscaling.groups.AutoScalingGroup
import com.eucalyptus.autoscaling.groups.AutoScalingGroups
import com.eucalyptus.autoscaling.instances.AutoScalingInstance
import com.eucalyptus.autoscaling.instances.AutoScalingInstances
import com.eucalyptus.autoscaling.instances.HealthStatus
import com.eucalyptus.autoscaling.instances.LifecycleState
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException
import com.eucalyptus.crypto.util.Timestamps
import com.eucalyptus.util.Callback
import com.eucalyptus.util.OwnerFullName
import com.eucalyptus.util.TypeMappers
import com.google.common.base.Predicate
import com.google.common.base.Supplier
import com.google.common.base.Suppliers
import edu.ucsb.eucalyptus.cloud.NotImplementedException
import edu.ucsb.eucalyptus.msgs.CreateTagsType
import edu.ucsb.eucalyptus.msgs.ReservationInfoType
import edu.ucsb.eucalyptus.msgs.RunInstancesResponseType
import edu.ucsb.eucalyptus.msgs.RunInstancesType
import edu.ucsb.eucalyptus.msgs.RunningInstancesItemType
import edu.ucsb.eucalyptus.msgs.TerminateInstancesType
import static org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

import java.security.cert.X509Certificate
import com.eucalyptus.autoscaling.tags.Tag
import com.eucalyptus.autoscaling.instances.ConfigurationState
import com.eucalyptus.loadbalancing.DeregisterInstancesFromLoadBalancerType
import com.eucalyptus.loadbalancing.RegisterInstancesWithLoadBalancerType
import com.eucalyptus.loadbalancing.RegisterInstancesWithLoadBalancerResponseType
import com.eucalyptus.loadbalancing.RegisterInstancesWithLoadBalancerResult
import com.eucalyptus.loadbalancing.Instances
import com.eucalyptus.loadbalancing.DeregisterInstancesFromLoadBalancerResponseType
import com.eucalyptus.loadbalancing.DeregisterInstancesFromLoadBalancerResult

/**
 * 
 */
@SuppressWarnings("GroovyAccessibility")
class ActivityManagerTest {
  
  @BeforeClass
  static void before() {
    TypeMappers.TypeMapperDiscovery discovery = new TypeMappers.TypeMapperDiscovery()
    discovery.processClass( LaunchConfigurations.LaunchConfigurationToRunInstances.class )
  }
  
  @Test
  void testLaunchInstances() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 2,
        capacity:  0,
        minSize: 1,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
    )    
    List<AutoScalingInstance> instances = [];
    List<ScalingActivity> scalingActivities = [];    
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 0, invoke( Integer.class, group, "getCapacity") )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 2, invoke( Integer.class, group, "getCapacity") )
    assertFalse( "Group scaling required", invoke( Boolean.class, group, "getScalingRequired") )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", invoke( String.class, instances.get(0), "getInstanceId" ) )
    assertEquals( "Instances 1 az", "Zone1", invoke( String.class, instances.get(0), "getAvailabilityZone" ) )
    assertEquals( "Instances 2 id", "i-00000002", invoke( String.class, instances.get(1), "getInstanceId" ) )
    assertEquals( "Instances 2 az", "Zone1", invoke( String.class, instances.get(1), "getAvailabilityZone" ) )
    assertEquals( "Scaling activity count", 2, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(0), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 1 has end date", invoke( Date.class, scalingActivities.get(0), "getEndTime" ) )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(1), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 2 has end date", invoke( Date.class, scalingActivities.get(1), "getEndTime" ) )
  }

  @Test
  void testRegisterInstances() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        loadBalancerNames: [ "LoadBalancer1", "LoadBalancer2" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: false,
        desiredCapacity: 2,
        capacity:  2,
        minSize: 1,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "2",
            naturalId: "1",
            uniqueName: "1",
            displayName: "i-00000001",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Instantiated,
            registrationAttempts: 0,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            displayName: "i-00000002",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Instantiated,
            registrationAttempts: 0,
        ),
    ];
    List<ScalingActivity> scalingActivities = [];
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 2, invoke( Integer.class, group, "getCapacity") )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 2, invoke( Integer.class, group, "getCapacity") )
    assertFalse( "Group scaling required", invoke( Boolean.class, group, "getScalingRequired") )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", invoke( String.class, instances.get(0), "getInstanceId" ) )
    assertEquals( "Instances 1 az", "Zone1", invoke( String.class, instances.get(0), "getAvailabilityZone" ) )
    assertEquals( "Instances 1 config state", ConfigurationState.Registered, invoke( ConfigurationState.class, instances.get(0), "getConfigurationState" ) )
    assertEquals( "Instances 2 id", "i-00000002", invoke( String.class, instances.get(1), "getInstanceId" ) )
    assertEquals( "Instances 2 az", "Zone1", invoke( String.class, instances.get(1), "getAvailabilityZone" ) )
    assertEquals( "Instances 2 config state", ConfigurationState.Registered, invoke( ConfigurationState.class, instances.get(1), "getConfigurationState" ) )
    assertEquals( "Scaling activity count", 2, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(0), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 1 has end date", invoke( Date.class, scalingActivities.get(0), "getEndTime" ) )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(1), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 2 has end date", invoke( Date.class, scalingActivities.get(1), "getEndTime" ) )
  }

  @Test
  void testTerminateInstances() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 0,
        capacity:  2,
        minSize: 0,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "1",
            naturalId: "1",
            uniqueName: "1",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,            
            configurationState: ConfigurationState.Registered,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
    ];
    List<ScalingActivity> scalingActivities = [];
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 2, invoke( Integer.class, group, "getCapacity") )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 0, invoke( Integer.class, group, "getCapacity") )
    assertFalse( "Group scaling required", invoke( Boolean.class, group, "getScalingRequired") )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 2, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(0), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 1 has end date", invoke( Date.class, scalingActivities.get(0), "getEndTime" ) )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(1), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 2 has end date", invoke( Date.class, scalingActivities.get(1), "getEndTime" ) )
  }

  @Test
  void testTerminateRegisteredInstances() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        loadBalancerNames: [ "LoadBalancer1", "LoadBalancer2" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 0,
        capacity:  2,
        minSize: 0,
        maxSize: 2,
        ownerAccountNumber: "000000000000",
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "1",
            naturalId: "1",
            uniqueName: "1",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            ownerAccountNumber: "000000000000",
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
            configurationState: ConfigurationState.Registered,
        ),
    ];
    List<ScalingActivity> scalingActivities = [];
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 2, invoke( Integer.class, group, "getCapacity") )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 0, invoke( Integer.class, group, "getCapacity") )
    assertFalse( "Group scaling required", invoke( Boolean.class, group, "getScalingRequired") )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 4, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(0), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 1 has end date", invoke( Date.class, scalingActivities.get(0), "getEndTime" ) )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(1), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 2 has end date", invoke( Date.class, scalingActivities.get(1), "getEndTime" ) )
    assertEquals( "Scaling activity 3 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(2), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 3 has end date", invoke( Date.class, scalingActivities.get(2), "getEndTime" ) )
    assertEquals( "Scaling activity 4 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(3), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 4 has end date", invoke( Date.class, scalingActivities.get(3), "getEndTime" ) )
  }

  @Test
  void testLaunchInstancesMultipleAvailabilityZones() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1", "Zone2", "Zone3", "Zone4" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 8,
        capacity:  0,
        minSize: 8,
        maxSize: 8,
        ownerAccountNumber: "000000000000",
    )
    List<AutoScalingInstance> instances = [];
    List<ScalingActivity> scalingActivities = [];
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 0, invoke( Integer.class, group, "getCapacity") )
    assertEquals( "Instance count", 0, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 8, invoke( Integer.class, group, "getCapacity") )
    assertFalse( "Group scaling required", invoke( Boolean.class, group, "getScalingRequired") )
    assertEquals( "Instance count", 8, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", invoke( String.class, instances.get(0), "getInstanceId" ) )
    assertEquals( "Instances 1 az", "Zone1", invoke( String.class, instances.get(0), "getAvailabilityZone" ) )
    assertEquals( "Instances 2 id", "i-00000002", invoke( String.class, instances.get(1), "getInstanceId" ) )
    assertEquals( "Instances 2 az", "Zone2", invoke( String.class, instances.get(1), "getAvailabilityZone" ) )
    assertEquals( "Instances 3 id", "i-00000003", invoke( String.class, instances.get(2), "getInstanceId" ) )
    assertEquals( "Instances 3 az", "Zone3", invoke( String.class, instances.get(2), "getAvailabilityZone" ) )
    assertEquals( "Instances 4 id", "i-00000004", invoke( String.class, instances.get(3), "getInstanceId" ) )
    assertEquals( "Instances 4 az", "Zone4", invoke( String.class, instances.get(3), "getAvailabilityZone" ) )
    assertEquals( "Instances 5 id", "i-00000005", invoke( String.class, instances.get(4), "getInstanceId" ) )
    assertEquals( "Instances 5 az", "Zone1", invoke( String.class, instances.get(4), "getAvailabilityZone" ) )
    assertEquals( "Instances 6 id", "i-00000006", invoke( String.class, instances.get(5), "getInstanceId" ) )
    assertEquals( "Instances 6 az", "Zone2", invoke( String.class, instances.get(5), "getAvailabilityZone" ) )
    assertEquals( "Instances 7 id", "i-00000007", invoke( String.class, instances.get(6), "getInstanceId" ) )
    assertEquals( "Instances 7 az", "Zone3", invoke( String.class, instances.get(6), "getAvailabilityZone" ) )
    assertEquals( "Instances 8 id", "i-00000008", invoke( String.class, instances.get(7), "getInstanceId" ) )
    assertEquals( "Instances 8 az", "Zone4", invoke( String.class, instances.get(7), "getAvailabilityZone" ) )
    assertEquals( "Scaling activity count", 8, scalingActivities.size() )
    for ( int i=0; i<8; i++ ) {
      assertEquals( "Scaling activity "+(i+1)+" status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(i), "getActivityStatusCode" ) )
      assertNotNull( "Scaling activity "+(i+1)+" has end date", invoke( Date.class, scalingActivities.get(i), "getEndTime" ) )
    }
  }
  
  @Test
  void testTerminateInstancesMultipleAvailabilityZones() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1", "Zone2", "Zone3" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 3,
        capacity:  5,
        minSize: 0,
        maxSize: 6,
        ownerAccountNumber: "000000000000",
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "1",
            naturalId: "1",
            uniqueName: "1",
            displayName: "i-00000001",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:01:00.000Z"),
            availabilityZone: "Zone3",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            displayName: "i-00000002",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:02:00.000Z"),
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
        ),
        new AutoScalingInstance(
            id: "3",
            naturalId: "3",
            uniqueName: "3",
            displayName: "i-00000003",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:03:00.000Z"),
            availabilityZone: "Zone3",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
        ),
        new AutoScalingInstance(
            id: "4",
            naturalId: "4",
            uniqueName: "4",
            displayName: "i-00000004",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:04:00.000Z"),
            availabilityZone: "Zone2",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
        ),
        new AutoScalingInstance(
            id: "5",
            naturalId: "5",
            uniqueName: "5",
            displayName: "i-00000005",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:05:00.000Z"),
            availabilityZone: "Zone2",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
        ),
    ];
    List<ScalingActivity> scalingActivities = [];
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 5, invoke( Integer.class, group, "getCapacity") )
    assertEquals( "Instance count", 5, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 3, invoke( Integer.class, group, "getCapacity") )
    assertFalse( "Group scaling required", invoke( Boolean.class, group, "getScalingRequired") )
    assertEquals( "Instance count", 3, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", invoke( String.class, instances.get(0), "getInstanceId" ) )
    assertEquals( "Instances 1 az", "Zone3", invoke( String.class, instances.get(0), "getAvailabilityZone" ) )
    assertEquals( "Instances 2 id", "i-00000002", invoke( String.class, instances.get(1), "getInstanceId" ) )
    assertEquals( "Instances 2 az", "Zone1", invoke( String.class, instances.get(1), "getAvailabilityZone" ) )
    assertEquals( "Instances 3 id", "i-00000004", invoke( String.class, instances.get(2), "getInstanceId" ) )
    assertEquals( "Instances 3 az", "Zone2", invoke( String.class, instances.get(2), "getAvailabilityZone" ) )
    assertEquals( "Scaling activity count", 2, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(0), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 1 has end date", invoke( Date.class, scalingActivities.get(0), "getEndTime" ) )
    assertEquals( "Scaling activity 2 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(1), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 2 has end date", invoke( Date.class, scalingActivities.get(1), "getEndTime" ) )    
  }

  @Test
  void testTerminateFromUnwantedAvailabilityZones() {
    Accounts.setAccountProvider( accountProvider() )

    AutoScalingGroup group = new AutoScalingGroup(
        id: "1",
        naturalId: "1",
        availabilityZones: [ "Zone1" ],
        displayName: "Group1",
        launchConfiguration: new LaunchConfiguration(
            id: "1",
            naturalId: "1",
            ownerAccountNumber: "000000000000",
            displayName: "Config1",
            imageId: "emi-00000001",
            instanceType: "m1.small",
        ),
        scalingRequired: true,
        desiredCapacity: 2,
        capacity:  3,
        minSize: 0,
        maxSize: 3,
        ownerAccountNumber: "000000000000",
    )
    List<AutoScalingInstance> instances = [
        new AutoScalingInstance(
            id: "1",
            naturalId: "1",
            uniqueName: "1",
            displayName: "i-00000001",            
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:00:00.000Z"),
            availabilityZone: "Zone1",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
        ),
        new AutoScalingInstance(
            id: "2",
            naturalId: "2",
            uniqueName: "2",
            displayName: "i-00000002",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T11:50:00.000Z"),
            availabilityZone: "Zone2",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
        ),
        new AutoScalingInstance(
            id: "3",
            naturalId: "3",
            uniqueName: "3",
            displayName: "i-00000003",
            ownerAccountNumber: "000000000000",
            creationTimestamp: timestamp("2012-02-10T12:00:00.000Z"),
            availabilityZone: "Zone2",
            healthStatus: HealthStatus.Healthy,
            autoScalingGroup: group,
            autoScalingGroupName: invoke( String.class, group, "getAutoScalingGroupName" ),
            launchConfigurationName: "Config1",
            lifecycleState: LifecycleState.InService,
        ),
    ];
    List<ScalingActivity> scalingActivities = [];
    ActivityManager manager = activityManager( group, scalingActivities, instances )

    assertEquals( "Group capacity", 3, invoke( Integer.class, group, "getCapacity") )
    assertEquals( "Instance count", 3, instances.size() )
    assertEquals( "Scaling activity count", 0, scalingActivities.size() )

    manager.doScaling()

    assertEquals( "Group capacity", 2, invoke( Integer.class, group, "getCapacity") )
    assertFalse( "Group scaling required", invoke( Boolean.class, group, "getScalingRequired") )
    assertEquals( "Instance count", 2, instances.size() )
    assertEquals( "Instances 1 id", "i-00000001", invoke( String.class, instances.get(0), "getInstanceId" ) )
    assertEquals( "Instances 1 az", "Zone1", invoke( String.class, instances.get(0), "getAvailabilityZone" ) )
    assertEquals( "Instances 2 id", "i-00000003", invoke( String.class, instances.get(1), "getInstanceId" ) )
    assertEquals( "Instances 2 az", "Zone2", invoke( String.class, instances.get(1), "getAvailabilityZone" ) )
    assertEquals( "Scaling activity count", 1, scalingActivities.size() )
    assertEquals( "Scaling activity 1 status", ActivityStatusCode.Successful, invoke( ActivityStatusCode.class, scalingActivities.get(0), "getActivityStatusCode" ) )
    assertNotNull( "Scaling activity 1 has end date", invoke( Date.class, scalingActivities.get(0), "getEndTime" ) )
  }  

  Date timestamp( String text ) {
    Timestamps.parseIso8601Timestamp( text )
  }
  
  def <T> T invoke( Class<T> resultClass, Object object, String method, Class[] parameterClasses, Object[] parameters = [] ) {
    // A groovy metaclass issue or class path issue prevents some method
    // invocations from succeeding without a bit of voodoo
    Object result = object.getClass().getMethod( method, parameterClasses ).invoke( object, parameters )
    result == null ? null : resultClass.cast( result )  
  }

  private ActivityManager activityManager( AutoScalingGroup group,
                                           List<ScalingActivity> scalingActivities,
                                           List<AutoScalingInstance> instances) {
    ActivityManager manager = new ActivityManager(
        autoScalingActivitiesStore(scalingActivities),
        autoScalingGroupStore([group]),
        autoScalingInstanceStore(instances)
    ) {
      int instanceCount = 0
      BackoffRunner runner = new BackoffRunner();

      @Override
      void runTask(ActivityManager.ScalingProcessTask task) {
        runner.runTask( task );
      }

      @Override
      boolean taskInProgress(String groupArn) {
        return false;
      }

      @Override
      EucalyptusClient createEucalyptusClientForUser(String userId) {
        new TestClients.TestEucalyptusClient( userId, { request ->
          if (request instanceof RunInstancesType) {
                new RunInstancesResponseType(
                    rsvInfo: new ReservationInfoType(
                        instancesSet: [
                            new RunningInstancesItemType(
                                instanceId: "i-0000000" + (++instanceCount),
                                placement: ((RunInstancesType) request).availabilityZone,
                            )
                        ]
                    )
                )
          } else if ( request instanceof CreateTagsType ||
              request instanceof TerminateInstancesType ) {
                request.reply
          } else {
            throw new RuntimeException("Unknown request type: " + request.getClass())
          }
        } as TestClients.RequestHandler )
      }

      @Override
      ElbClient createElbClientForUser(final String userId) {
        new TestClients.TestElbClient( userId, { request ->
          if (request instanceof RegisterInstancesWithLoadBalancerType ) {
            new RegisterInstancesWithLoadBalancerResponseType(
                registerInstancesWithLoadBalancerResult: new RegisterInstancesWithLoadBalancerResult(
                    instances: new Instances(
                        member: request.instances.member
                    )
                )
            )
          } else if ( request instanceof DeregisterInstancesFromLoadBalancerType ) {
            new DeregisterInstancesFromLoadBalancerResponseType(
                deregisterInstancesFromLoadBalancerResult: new DeregisterInstancesFromLoadBalancerResult(
                    instances: new Instances(
                        member: request.instances.member
                    )
                )
            )
          } else {
            throw new RuntimeException("Unknown request type: " + request.getClass())
          }
        } as TestClients.RequestHandler )
      }

      @Override
      Supplier<String> userIdSupplier(String accountNumber) {
        Suppliers.ofInstance(accountNumber)
      }

      @Override
      List<Tag> getTags(AutoScalingGroup autoScalingGroup) {
        []
      }
    }
    manager
  }

  AccountProvider accountProvider() {
    new AccountProvider() {
      @Override
      Account lookupAccountByName(final String accountName) {
        throw new NotImplementedException()
      }

      @Override
      Account lookupAccountById(final String accountId) {
        Principals.systemAccount( )
      }

      @Override
      Account addAccount(final String accountName) {
        throw new NotImplementedException()
      }

      @Override
      void deleteAccount(final String accountName, final boolean forceDeleteSystem, final boolean recursive) {
        throw new NotImplementedException()
      }

      @Override
      List<Account> listAllAccounts() {
        throw new NotImplementedException()
      }

      @Override
      Set<String> resolveAccountNumbersForName(final String accountNAmeLike) {
        [] as Set
      }

      @Override
      List<User> listAllUsers() {
        throw new NotImplementedException()
      }

      @Override
      boolean shareSameAccount(final String userId1, final String userId2) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserById(final String userId) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserByAccessKeyId(final String keyId) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserByCertificate(final X509Certificate cert) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserByConfirmationCode(final String code) {
        throw new NotImplementedException()
      }

      @Override
      Group lookupGroupById(final String groupId) {
        throw new NotImplementedException()
      }

      @Override
      Certificate lookupCertificate(final X509Certificate cert) {
        throw new NotImplementedException()
      }

      @Override
      AccessKey lookupAccessKeyById(final String keyId) {
        throw new NotImplementedException()
      }

      @Override
      User lookupUserByName(final String userName) {
        throw new NotImplementedException()
      }
    }
  }  
  
  ScalingActivities autoScalingActivitiesStore( List<ScalingActivity> activities = [] ) {
    new ScalingActivities() {
      @Override
      List<ScalingActivity> list(OwnerFullName ownerFullName) {
        activities.findAll { activity -> invoke( String.class, activity, "getOwnerAccountNumber").equals( ownerFullName.accountNumber ) }
      }

      @Override
      List<ScalingActivity> list(OwnerFullName ownerFullName, 
                                 Predicate<? super ScalingActivity> filter) {
        list( ownerFullName ).findAll { activity -> filter.apply( activity ) } as List
      }

      @Override
      ScalingActivity lookup(OwnerFullName ownerFullName, 
                             String activityId) {
        ScalingActivity activity = activities.find{ activity -> invoke( String.class, activity, "getActivityId").equals( activityId ) }
        if ( activity == null ) {
          throw new AutoScalingMetadataNotFoundException("Scaling activity not found: " + activityId)
        }
        activity
      }

      @Override
      ScalingActivity update(OwnerFullName ownerFullName, 
                             String activityId, 
                             Callback<ScalingActivity> activityUpdateCallback) {
        ScalingActivity activity = lookup( ownerFullName, activityId )
        activityUpdateCallback.fire( activity )
        activity
      }

      @Override
      boolean delete(ScalingActivity scalingActivity) {
        activities.remove(scalingActivity)
      }

      @Override
      ScalingActivity save(ScalingActivity scalingActivity) {
        scalingActivity.setId( "1" )
        scalingActivity.setNaturalId( UUID.randomUUID( ).toString( ) )
        scalingActivity.setCreationTimestamp( new Date() )
        scalingActivity.setLastUpdateTimestamp( new Date() )
        activities.add( scalingActivity )
        scalingActivity
      }
    }    
  }
  
  AutoScalingGroups autoScalingGroupStore( List<AutoScalingGroup> groups = [] ) {
    new AutoScalingGroups() {
      @Override
      List<AutoScalingGroup> list(OwnerFullName ownerFullName) {
        groups.findAll { group -> invoke( String.class, group, "getOwnerAccountNumber").equals( ownerFullName.accountNumber ) }
      }

      @Override
      List<AutoScalingGroup> list(OwnerFullName ownerFullName, Predicate<? super AutoScalingGroup> filter) {
        list( ownerFullName ).findAll { group -> filter.apply( group ) } as List
      }

      @Override
      List<AutoScalingGroup> listRequiringScaling() {
        groups.findAll { group -> Boolean.TRUE.equals( invoke( Boolean.class, group, "getScalingRequired") ) }
      }

      @Override
      List<AutoScalingGroup> listRequiringInstanceReplacement() {
        []
      }

      @Override
      List<AutoScalingGroup> listRequiringMonitoring(long interval) {
        []
      }

      @Override
      AutoScalingGroup lookup(OwnerFullName ownerFullName, String autoScalingGroupName) {
        AutoScalingGroup group = groups.find { AutoScalingGroup group ->
          invoke( String.class, group, "getArn" ).equals( autoScalingGroupName ) ||
              ( invoke( String.class, group, "getDisplayName" ).equals( autoScalingGroupName ) && // work around some groovy metaclass issue
                  invoke( String.class, group, "getOwnerAccountNumber").equals( ownerFullName.accountNumber ) )
        }
        if ( group == null ) {
          throw new AutoScalingMetadataNotFoundException("Group not found: " + autoScalingGroupName)
        }
        group
      }

      @Override
      AutoScalingGroup update(OwnerFullName ownerFullName,
                              String autoScalingGroupName,
                              Callback<AutoScalingGroup> groupUpdateCallback) {
        AutoScalingGroup group = lookup( ownerFullName, autoScalingGroupName )
        groupUpdateCallback.fire( group )
        invoke( Void.class, group, "setScalingRequired", [ Boolean.class ] as Class[], [ 
            invoke( Integer.class, group, "getCapacity" ) !=
            invoke( Integer.class, group, "getDesiredCapacity" ) 
        ] as Object[] )
        group
      }

      @Override
      boolean delete(AutoScalingGroup autoScalingGroup) {
        groups.remove( autoScalingGroup )
      }

      @Override
      AutoScalingGroup save(AutoScalingGroup autoScalingGroup) {
        autoScalingGroup.setId( "1" )
        autoScalingGroup.setNaturalId( UUID.randomUUID( ).toString( ) )
        groups.add( autoScalingGroup )
        autoScalingGroup
      }
    }
  }

  AutoScalingInstances autoScalingInstanceStore( List<AutoScalingInstance> instances = [] ) {
    new AutoScalingInstances(){
      @Override
      List<AutoScalingInstance> list(OwnerFullName ownerFullName) {
        ownerFullName == null ?
          instances :
          instances.findAll { instance -> invoke( String.class, instance, "getOwnerAccountNumber" ).equals( ownerFullName.accountNumber ) }
      }

      @Override
      List<AutoScalingInstance> list(OwnerFullName ownerFullName, Predicate<? super AutoScalingInstance> filter) {
        list( ownerFullName ).findAll { instance -> filter.apply( instance ) } as List
      }

      @Override
      List<AutoScalingInstance> listByGroup(OwnerFullName ownerFullName, String groupName) {
        list( ownerFullName, { AutoScalingInstance instance -> 
          groupName.equals( invoke( String.class, instance, "getAutoScalingGroupName" ) ) 
        } as Predicate )
      }

      @Override
      List<AutoScalingInstance> listByGroup(AutoScalingGroup group) {
        group == null ?
          list( null ) :
          listByGroup(
            invoke( OwnerFullName.class, group, "getOwner" ),
            invoke( String.class, group, "getAutoScalingGroupName" ) )
      }

      @Override
      List<AutoScalingInstance> listByState(LifecycleState lifecycleState,
                                            ConfigurationState configurationState) {
        instances.findAll { instance -> lifecycleState.apply( instance ) && configurationState.apply( instance ) }
      }

      @Override
      List<AutoScalingInstance> listUnhealthyByGroup( AutoScalingGroup group ) {
        []
      }

      @Override
      AutoScalingInstance lookup(OwnerFullName ownerFullName, String instanceId) {
        list( ownerFullName ).find { instance ->
          invoke( String.class, instance, "getInstanceId").equals( instanceId )
        }
      }

      @Override
      AutoScalingInstance update(OwnerFullName ownerFullName,
                                 String instanceId,
                                 Callback<AutoScalingInstance> instanceUpdateCallback) {
        AutoScalingInstance instance = lookup( ownerFullName, instanceId )
        instanceUpdateCallback.fire( instance )
        instance
      }

      @Override
      void markMissingInstancesUnhealthy(AutoScalingGroup group,
                                         Collection<String> instanceIds) {
      }

      @Override
      Set<String> verifyInstanceIds(String accountNumber,
                                    Collection<String> instanceIds) {
        return [] as Set
      }

      @Override
      void transitionState(AutoScalingGroup group,
                           LifecycleState from,
                           LifecycleState to,
                           Collection<String> instanceIds) {
      }

      @Override
      void transitionConfigurationState(AutoScalingGroup group,
                                        ConfigurationState from,
                                        ConfigurationState to,
                                        Collection<String> instanceIds) {
        instances.each { instance ->
          instanceIds.contains( invoke( String.class, instance, "getInstanceId") ) &&
              from.transitionTo(to).apply( instance )  }
      }

      @Override
      int registrationFailure(AutoScalingGroup group,
                              Collection<String> instanceIds) {
        0
      }

      @Override
      boolean delete(AutoScalingInstance autoScalingInstance) {
        instances.remove( autoScalingInstance )
      }

      @Override
      boolean deleteByGroup(AutoScalingGroup group) {
        instances.removeAll( instances.findAll { instance -> group.autoScalingGroupName.equals( instance.autoScalingGroupName ) } )
      }

      @Override
      AutoScalingInstance save(AutoScalingInstance autoScalingInstance) {
        autoScalingInstance.setId( "1" )
        autoScalingInstance.setNaturalId( UUID.randomUUID( ).toString( ) )
        instances.add( autoScalingInstance )
        autoScalingInstance
      }
    }
  }
}

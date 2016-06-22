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
package com.eucalyptus.compute.service.ws

import com.eucalyptus.compute.common.AttributeBooleanValueType
import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressType
import com.eucalyptus.compute.common.CreateVolumePermissionItemType
import com.eucalyptus.compute.common.DescribeSnapshotAttributeType
import com.eucalyptus.compute.common.DiskImage
import com.eucalyptus.compute.common.DiskImageDetail
import com.eucalyptus.compute.common.DiskImageVolume
import com.eucalyptus.compute.common.ImportInstanceLaunchSpecification
import com.eucalyptus.compute.common.ImportInstanceType
import com.eucalyptus.compute.common.InstancePlacement
import com.eucalyptus.compute.common.IpPermissionType
import com.eucalyptus.compute.common.LaunchPermissionItemType
import com.eucalyptus.compute.common.LaunchPermissionOperationType
import com.eucalyptus.compute.common.ModifyImageAttributeType
import com.eucalyptus.compute.common.ModifySnapshotAttributeType
import com.eucalyptus.compute.common.ModifyVpcAttributeType
import com.eucalyptus.compute.common.MonitoringInstance
import com.eucalyptus.compute.common.ResetSnapshotAttributeType
import com.eucalyptus.compute.common.UserData
import com.eucalyptus.compute.common.UserIdGroupPairType
import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import com.google.common.base.Splitter
import edu.ucsb.eucalyptus.msgs.BaseMessage
import groovy.transform.CompileStatic
import org.junit.Test
import static org.junit.Assert.*

/**
 *
 */
@CompileStatic
class ComputeQueryBindingTest extends QueryBindingTestSupport {

    @Test
    void testValidBinding2006_06_26() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2006-06-26-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2006_10_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2006-10-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2007_01_03() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2007-01-03-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2007_01_19() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2007-01-19-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2007_03_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2007-03-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2007_08_29() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2007-08-29-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2008_02_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2008-02-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2008_05_05() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2008-05-05-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2008_08_08() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2008-08-08-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2008_12_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2008-12-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2009_03_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2009-03-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2009_04_04() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2009-04-04-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2009_07_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2009-07-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2009_08_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2009-08-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2009_10_31() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2009-10-31-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2009_11_30() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2009-11-30-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2010_06_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2010-06-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2010_08_31() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2010-08-31-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2010_11_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2010-11-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2011_01_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2011-01-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2011_02_28() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2011-02-28-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2011_05_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2011-05-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2011_07_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2011-07-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2011_11_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2011-11-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2011_12_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2011-12-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2011_12_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2011-12-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_03_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-03-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_04_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-04-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_05_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-05-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_06_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-06-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_06_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-06-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_07_20() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-07-20-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_08_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-08-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_10_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-10-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_11_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-11-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2012_12_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2012-12-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2013_02_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2013-02-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2013_06_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2013-06-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2013_07_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2013-07-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2013_08_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2013-08-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2013_10_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2013-10-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2013_10_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2013-10-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2014_02_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2014-02-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2014_05_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2014-05-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2014_06_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2014-06-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2014_09_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2014-09-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2014_10_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2014-10-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2015_04_15() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2015-04-15-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testValidBinding2015_10_01() {
        URL resource = ComputeQueryBindingTest.getResource('/ec2-2015-10-01-binding.xml')
        assertValidBindingXml(resource)
    }

    @Test
    void testBindingsForAllActions(){
      // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_Operations.html
      String actionsCopiedAndPastedFromAWSEC2Docs = '''
    AcceptVpcPeeringConnection

    AllocateAddress

    AssignPrivateIpAddresses

    AssociateAddress

    AssociateDhcpOptions

    AssociateRouteTable

    AttachClassicLinkVpc

    AttachInternetGateway

    AttachNetworkInterface

    AttachVolume

    AttachVpnGateway

    AuthorizeSecurityGroupEgress

    AuthorizeSecurityGroupIngress

    BundleInstance

    CancelBundleTask

    CancelConversionTask

    CancelExportTask

    CancelImportTask

    CancelReservedInstancesListing

    CancelSpotFleetRequests

    CancelSpotInstanceRequests

    ConfirmProductInstance

    CopyImage

    CopySnapshot

    CreateCustomerGateway

    CreateDhcpOptions

    CreateImage

    CreateInstanceExportTask

    CreateInternetGateway

    CreateKeyPair

    CreateNetworkAcl

    CreateNetworkAclEntry

    CreateNetworkInterface

    CreatePlacementGroup

    CreateReservedInstancesListing

    CreateRoute

    CreateRouteTable

    CreateSecurityGroup

    CreateSnapshot

    CreateSpotDatafeedSubscription

    CreateSubnet

    CreateTags

    CreateVolume

    CreateVpc

    CreateVpcEndpoint

    CreateVpcPeeringConnection

    CreateVpnConnection

    CreateVpnConnectionRoute

    CreateVpnGateway

    DeleteCustomerGateway

    DeleteDhcpOptions

    DeleteInternetGateway

    DeleteKeyPair

    DeleteNetworkAcl

    DeleteNetworkAclEntry

    DeleteNetworkInterface

    DeletePlacementGroup

    DeleteRoute

    DeleteRouteTable

    DeleteSecurityGroup

    DeleteSnapshot

    DeleteSpotDatafeedSubscription

    DeleteSubnet

    DeleteTags

    DeleteVolume

    DeleteVpc

    DeleteVpcEndpoints

    DeleteVpcPeeringConnection

    DeleteVpnConnection

    DeleteVpnConnectionRoute

    DeleteVpnGateway

    DeregisterImage

    DescribeAccountAttributes

    DescribeAddresses

    DescribeAvailabilityZones

    DescribeBundleTasks

    DescribeClassicLinkInstances

    DescribeConversionTasks

    DescribeCustomerGateways

    DescribeDhcpOptions

    DescribeExportTasks

    DescribeImageAttribute

    DescribeImages

    DescribeImportImageTasks

    DescribeImportSnapshotTasks

    DescribeInstanceAttribute

    DescribeInstanceStatus

    DescribeInstances

    DescribeInternetGateways

    DescribeKeyPairs

    DescribeMovingAddresses

    DescribeNetworkAcls

    DescribeNetworkInterfaceAttribute

    DescribeNetworkInterfaces

    DescribePlacementGroups

    DescribePrefixLists

    DescribeRegions

    DescribeReservedInstances

    DescribeReservedInstancesListings

    DescribeReservedInstancesModifications

    DescribeReservedInstancesOfferings

    DescribeRouteTables

    DescribeSecurityGroups

    DescribeSnapshotAttribute

    DescribeSnapshots

    DescribeSpotDatafeedSubscription

    DescribeSpotFleetInstances

    DescribeSpotFleetRequestHistory

    DescribeSpotFleetRequests

    DescribeSpotInstanceRequests

    DescribeSpotPriceHistory

    DescribeSubnets

    DescribeTags

    DescribeVolumeAttribute

    DescribeVolumeStatus

    DescribeVolumes

    DescribeVpcAttribute

    DescribeVpcClassicLink

    DescribeVpcEndpointServices

    DescribeVpcEndpoints

    DescribeVpcPeeringConnections

    DescribeVpcs

    DescribeVpnConnections

    DescribeVpnGateways

    DetachClassicLinkVpc

    DetachInternetGateway

    DetachNetworkInterface

    DetachVolume

    DetachVpnGateway

    DisableVgwRoutePropagation

    DisableVpcClassicLink

    DisassociateAddress

    DisassociateRouteTable

    EnableVgwRoutePropagation

    EnableVolumeIO

    EnableVpcClassicLink

    GetConsoleOutput

    GetPasswordData

    ImportImage

    ImportInstance

    ImportKeyPair

    ImportSnapshot

    ImportVolume

    ModifyImageAttribute

    ModifyInstanceAttribute

    ModifyNetworkInterfaceAttribute

    ModifyReservedInstances

    ModifySnapshotAttribute

    ModifySubnetAttribute

    ModifyVolumeAttribute

    ModifyVpcAttribute

    ModifyVpcEndpoint

    MonitorInstances

    MoveAddressToVpc

    PurchaseReservedInstancesOffering

    RebootInstances

    RegisterImage

    RejectVpcPeeringConnection

    ReleaseAddress

    ReplaceNetworkAclAssociation

    ReplaceNetworkAclEntry

    ReplaceRoute

    ReplaceRouteTableAssociation

    ReportInstanceStatus

    RequestSpotFleet

    RequestSpotInstances

    ResetImageAttribute

    ResetInstanceAttribute

    ResetNetworkInterfaceAttribute

    ResetSnapshotAttribute

    RestoreAddressToClassic

    RevokeSecurityGroupEgress

    RevokeSecurityGroupIngress

    RunInstances

    StartInstances

    StopInstances

    TerminateInstances

    UnassignPrivateIpAddresses

    UnmonitorInstances
    '''

      List<String> whitelist = [
      ]
      Splitter.on(' ').trimResults( ).omitEmptyStrings( ).split( actionsCopiedAndPastedFromAWSEC2Docs ).each { String action ->
        try {
          Class.forName( "com.eucalyptus.compute.common.${action}Type" )
        } catch ( Exception e ) {
          if ( !whitelist.contains(action) ) fail( "Message not found for ${action}" )
        }
      }
    }

    @Test
    void testSecurityMessageQueryBindings() {
        URL resource = ComputeQueryBindingTest.class.getResource('/ec2-security-11-01-01.xml')

        String version = "2009-11-30"
        ComputeQueryBinding eb = new ComputeQueryBinding() {
            @Override
            protected com.eucalyptus.binding.Binding getBindingWithElementClass(final String operationName) {
                createTestBindingFromXml(resource, operationName)
            }

            @Override
            String getNamespace() {
                return getNamespaceForVersion(version);
            }

            @Override
            protected void validateBinding(final com.eucalyptus.binding.Binding currentBinding,
                                           final String operationName,
                                           final Map<String, String> params,
                                           final BaseMessage eucaMsg) {
                // Validation requires compiled bindings
            }
        }

        version = "2010-06-15"
        bindAndAssertObject(eb, AuthorizeSecurityGroupIngressType.class, "AuthorizeSecurityGroupIngress", new AuthorizeSecurityGroupIngressType(
                groupId: 'sg-00000000',
                ipPermissions: [
                        new IpPermissionType(
                                ipProtocol: "TCP",
                                fromPort: 7,
                                toPort: 12,
                                groups: [
                                        new UserIdGroupPairType(sourceGroupId: 'sg-00000001')
                                ] as ArrayList<UserIdGroupPairType>,
                                cidrIpRanges: [
                                        "0.0.0.0/0"
                                ]
                        )
                ] as ArrayList<IpPermissionType>,

        ), 6)

        // AuthorizeSecurityGroupIngress - 2009/11/30  (should be 2009/10/31 according to API documents)
        version = "2009-11-30"
        bindAndAssertParameters(eb, AuthorizeSecurityGroupIngressType.class, "AuthorizeSecurityGroupIngress", new AuthorizeSecurityGroupIngressType(
                groupUserId: 'AAAAAAAAAAAAAAAAAAAAA',
                groupName: 'default',
                ipProtocol: 'tcp',
                fromPort: 7,
                toPort: 12,
                sourceSecurityGroupOwnerId: 'BBBBBBBBBBBBBBBBBBBB',
                sourceSecurityGroupName: 'userb-group',
                cidrIp: '0.0.0.0/0'
        ), [
                UserId                    : 'AAAAAAAAAAAAAAAAAAAAA',
                GroupName                 : 'default',
                IpProtocol                : 'tcp',
                FromPort                  : '7',
                ToPort                    : '12',
                SourceSecurityGroupOwnerId: 'BBBBBBBBBBBBBBBBBBBB',
                SourceSecurityGroupName   : 'userb-group',
                CidrIp                    : '0.0.0.0/0',
        ])

        // AuthorizeSecurityGroupIngress - 2010/06/15
        version = "2010-06-15"
        bindAndAssertParameters(eb, AuthorizeSecurityGroupIngressType.class, "AuthorizeSecurityGroupIngress", new AuthorizeSecurityGroupIngressType(
                groupName: 'default',
                ipPermissions: [
                        new IpPermissionType(
                                ipProtocol: 'tcp',
                                fromPort: 7,
                                toPort: 12,
                                groups: [
                                        new UserIdGroupPairType(
                                                sourceUserId: 'BBBBBBBBBBBBBBBBBBBB',
                                                sourceGroupName: 'userb-group'
                                        )
                                ] as ArrayList<UserIdGroupPairType>,
                                cidrIpRanges: [
                                        '0.0.0.0/0'
                                ]
                        )
                ] as ArrayList<IpPermissionType>,

        ), [
                GroupName                           : 'default',
                'IpPermissions.1.IpProtocol'        : 'tcp',
                'IpPermissions.1.FromPort'          : '7',
                'IpPermissions.1.ToPort'            : '12',
                'IpPermissions.1.Groups.1.UserId'   : 'BBBBBBBBBBBBBBBBBBBB',
                'IpPermissions.1.Groups.1.GroupName': 'userb-group',
                'IpPermissions.1.IpRanges.1.CidrIp' : '0.0.0.0/0',
        ])

        // AuthorizeSecurityGroupIngress - 2011/01/01
        version = "2011-01-01"
        bindAndAssertParameters(eb, AuthorizeSecurityGroupIngressType.class, "AuthorizeSecurityGroupIngress", new AuthorizeSecurityGroupIngressType(
                groupName: 'default',
                groupId: 'sg-00000001',
                ipPermissions: [
                        new IpPermissionType(
                                ipProtocol: 'tcp',
                                fromPort: 7,
                                toPort: 12,
                                groups: [
                                        new UserIdGroupPairType(
                                                sourceUserId: 'BBBBBBBBBBBBBBBBBBBB',
                                                sourceGroupName: 'userb-group',
                                                sourceGroupId: 'sg-00000002'
                                        )
                                ] as ArrayList<UserIdGroupPairType>,
                                cidrIpRanges: [
                                        '0.0.0.0/0'
                                ]
                        )
                ] as ArrayList<IpPermissionType>,

        ), [
                GroupName                           : 'default',
                GroupId                             : 'sg-00000001',
                'IpPermissions.1.IpProtocol'        : 'tcp',
                'IpPermissions.1.FromPort'          : '7',
                'IpPermissions.1.ToPort'            : '12',
                'IpPermissions.1.Groups.1.UserId'   : 'BBBBBBBBBBBBBBBBBBBB',
                'IpPermissions.1.Groups.1.GroupName': 'userb-group',
                'IpPermissions.1.Groups.1.GroupId'  : 'sg-00000002',
                'IpPermissions.1.IpRanges.1.CidrIp' : '0.0.0.0/0',
        ])
    }

    @Test
    void testImageAttrMessageQueryBindings() {
        URL resource = ComputeQueryBindingTest.class.getResource('/ec2-image-attr.xml')

        String version = "2009-11-30"
        ComputeQueryBinding eb = new ComputeQueryBinding() {
            @Override
            protected com.eucalyptus.binding.Binding getBindingWithElementClass(final String operationName) {
                createTestBindingFromXml(resource, operationName)
            }

            @Override
            String getNamespace() {
                return getNamespaceForVersion(version);
            }

            @Override
            protected void validateBinding(final com.eucalyptus.binding.Binding currentBinding,
                                           final String operationName,
                                           final Map<String, String> params,
                                           final BaseMessage eucaMsg) {
                // Validation requires compiled bindings
            }
        }

        // ModifyImageAttribute - 2010-06-15
        version = "2010-06-15"
        bindAndAssertParameters(eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
                imageId: 'emi-0000001',
                queryUserId: ['111111111111', '222222222222'] as ArrayList<String>,
                queryUserGroup: ['G1', 'G2'] as ArrayList<String>,
                productCodes: ['Code1', 'Code2'] as ArrayList<String>,
                attribute: 'launchPermission',
                operationType: 'add',
        ), [
                ImageId        : 'emi-0000001',
                'UserId.1'     : '111111111111',
                'UserId.2'     : '222222222222',
                'Group.1'      : 'G1',
                'Group.2'      : 'G2',
                'ProductCode.1': 'Code1',
                'ProductCode.2': 'Code2',
                'Attribute'    : 'launchPermission',
                'OperationType': 'add',
        ]).with {
            assertEquals('Attribute', ModifyImageAttributeType.ImageAttribute.LaunchPermission, imageAttribute())
            assertTrue('Is add operation', add())
            assertEquals('UserIds', ['111111111111', '222222222222'], userIds())
            assertFalse('Group all', groupAll())
            assertEquals('Launch permissions', [new LaunchPermissionItemType(userId: '111111111111'), new LaunchPermissionItemType(userId: '222222222222')] as List<LaunchPermissionItemType>, asAddLaunchPermissionsItemTypes())
        }

        // ModifyImageAttribute - 2010-06-15 - Incorrect 'UserGroup' parameter (backwards compatible)
        version = "2010-06-15"
        bindAndAssertParameters(eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
                imageId: 'emi-0000001',
                queryUserId: ['111111111111', '222222222222'] as ArrayList<String>,
                queryUserGroup: ['G1', 'G2'] as ArrayList<String>,
                productCodes: ['Code1', 'Code2'] as ArrayList<String>,
                attribute: 'launchPermission',
                operationType: 'add',
        ), [
                ImageId        : 'emi-0000001',
                'UserId.1'     : '111111111111',
                'UserId.2'     : '222222222222',
                'UserGroup.1'  : 'G1',
                'UserGroup.2'  : 'G2',
                'ProductCode.1': 'Code1',
                'ProductCode.2': 'Code2',
                'Attribute'    : 'launchPermission',
                'OperationType': 'add',
        ])

        // ModifyImageAttribute - 2010-06-15 / Non standard group parameter
        version = "2010-06-15"
        bindAndAssertParameters(eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
                imageId: 'emi-0000001',
                queryUserId: ['111111111111', '222222222222'] as ArrayList<String>,
                queryUserGroup: ['G1', 'G2'] as ArrayList<String>,
                productCodes: ['Code1', 'Code2'] as ArrayList<String>,
                attribute: 'launchPermission',
                operationType: 'add',
        ), [
                ImageId        : 'emi-0000001',
                'UserId.1'     : '111111111111',
                'UserId.2'     : '222222222222',
                'UserGroup.1'  : 'G1',
                'UserGroup.2'  : 'G2',
                'ProductCode.1': 'Code1',
                'ProductCode.2': 'Code2',
                'Attribute'    : 'launchPermission',
                'OperationType': 'add',
        ])

        // ModifyImageAttribute - 2010-08-31
        version = "2010-08-31"
        bindAndAssertParameters(eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
                imageId: 'emi-0000001',
                launchPermission: new LaunchPermissionOperationType(
                        add: [
                                new LaunchPermissionItemType(userId: '111111111111'),
                                new LaunchPermissionItemType(userId: '222222222222'),
                        ] as ArrayList<LaunchPermissionItemType>,
                        remove: [
                                new LaunchPermissionItemType(userId: '333333333333'),
                                new LaunchPermissionItemType(userId: '444444444444'),
                        ] as ArrayList<LaunchPermissionItemType>,
                ),
                productCodes: ['Code1', 'Code2'] as ArrayList<String>,
                description: 'An image',
        ), [
                ImageId                           : 'emi-0000001',
                'LaunchPermission.Add.1.UserId'   : '111111111111',
                'LaunchPermission.Add.2.UserId'   : '222222222222',
                'LaunchPermission.Remove.1.UserId': '333333333333',
                'LaunchPermission.Remove.2.UserId': '444444444444',
                'ProductCode.1'                   : 'Code1',
                'ProductCode.2'                   : 'Code2',
                'Description.Value'               : 'An image',
        ]).with { bound ->
            assertEquals('Attribute', ModifyImageAttributeType.ImageAttribute.LaunchPermission, imageAttribute())
            assertTrue('Is add operation', add())
            assertEquals('UserIds', ['111111111111', '222222222222'], userIds())
            assertFalse('Group all', groupAll())
            assertEquals('Launch permissions', [new LaunchPermissionItemType(userId: '111111111111'), new LaunchPermissionItemType(userId: '222222222222')] as List<LaunchPermissionItemType>, asAddLaunchPermissionsItemTypes())
        }

        bindAndAssertParameters(eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
                imageId: 'emi-0000001',
                launchPermission: new LaunchPermissionOperationType(
                        add: [
                                new LaunchPermissionItemType(group: 'G1'),
                                new LaunchPermissionItemType(group: 'G2'),
                        ] as ArrayList<LaunchPermissionItemType>,
                        remove: [
                                new LaunchPermissionItemType(group: 'G3'),
                                new LaunchPermissionItemType(group: 'G4'),
                        ] as ArrayList<LaunchPermissionItemType>,
                ),
                productCodes: ['Code1', 'Code2'] as ArrayList<String>,
                description: 'An image',
        ), [
                ImageId                          : 'emi-0000001',
                'LaunchPermission.Add.1.Group'   : 'G1',
                'LaunchPermission.Add.2.Group'   : 'G2',
                'LaunchPermission.Remove.1.Group': 'G3',
                'LaunchPermission.Remove.2.Group': 'G4',
                'ProductCode.1'                  : 'Code1',
                'ProductCode.2'                  : 'Code2',
                'Description.Value'              : 'An image',
        ])
    }

    @Test
    void testSnapshotAttrMessageQueryBindings() {
        URL resource = ComputeQueryBindingTest.class.getResource('/ec2-ebs-snapshot-attr.xml')

        String version = "2014-02-01"
        ComputeQueryBinding eb = new ComputeQueryBinding() {
            @Override
            protected com.eucalyptus.binding.Binding getBindingWithElementClass(final String operationName) {
                createTestBindingFromXml(resource, operationName)
            }

            @Override
            String getNamespace() {
                return getNamespaceForVersion(version);
            }

            @Override
            protected void validateBinding(final com.eucalyptus.binding.Binding currentBinding,
                                           final String operationName,
                                           final Map<String, String> params,
                                           final BaseMessage eucaMsg) {
                // Validation requires compiled bindings
            }
        }

        List<String> supportedVersions = [
                "2012-03-01",
                "2012-04-01",
                "2012-05-01",
                "2012-06-01",
                "2012-06-15",
                "2012-07-20",
                "2012-08-15",
                "2012-10-01",
                "2012-11-15",
                "2012-12-01",
                "2013-02-01",
                "2013-06-15",
                "2013-07-15",
                "2013-08-15",
                "2013-10-01",
                "2013-10-15",
                "2014-02-01" ]

        supportedVersions.each { v ->
            version = v
            bindAndAssertParameters(eb, ModifySnapshotAttributeType.class, "ModifySnapshotAttribute", new ModifySnapshotAttributeType(
                    snapshotId: 'snap-00000001',
                    queryUserId: ['111111111111', '222222222222'] as ArrayList<String>,
                    queryUserGroup: ['G1', 'G2'] as ArrayList<String>,
                    productCodes: ['Code1', 'Code2'] as ArrayList<String>,
                    attribute: 'createVolumePermission',
                    operationType: 'add',
            ), [
                    SnapshotId     : 'snap-00000001',
                    'UserId.1'     : '111111111111',
                    'UserId.2'     : '222222222222',
                    'Group.1'      : 'G1',
                    'Group.2'      : 'G2',
                    'ProductCode.1': 'Code1',
                    'ProductCode.2': 'Code2',
                    'Attribute'    : 'createVolumePermission',
                    'OperationType': 'add',
            ]).with {
                assertEquals('Attribute', ModifySnapshotAttributeType.SnapshotAttribute.CreateVolumePermission, snapshotAttribute())
                assertTrue('Is add operation', add())
                assertEquals('UserIds', ['111111111111', '222222222222'], addUserIds())
                assertFalse('Group all', addGroupAll())
                assertEquals('Create Volume permissions', [new CreateVolumePermissionItemType(userId: '111111111111'), new CreateVolumePermissionItemType(userId: '222222222222')] as List<CreateVolumePermissionItemType>, asAddCreateVolumePermissionsItemTypes())
            }

            bindAndAssertParameters(eb, ModifySnapshotAttributeType.class, "ModifySnapshotAttribute", new ModifySnapshotAttributeType(
                    snapshotId: 'snap-0000001',
                    queryUserId: ['111111111111', '222222222222'] as ArrayList<String>,
                    queryUserGroup: ['all'] as ArrayList<String>,
                    productCodes: ['Code1', 'Code2'] as ArrayList<String>,
                    attribute: 'createVolumePermission',
                    operationType: 'add',
            ), [
                    SnapshotId     : 'snap-0000001',
                    'UserId.1'     : '111111111111',
                    'UserId.2'     : '222222222222',
                    'Group'        : 'all',
                    'ProductCode.1': 'Code1',
                    'ProductCode.2': 'Code2',
                    'Attribute'    : 'createVolumePermission',
                    'OperationType': 'add',
            ])

            bindAndAssertParameters(eb, ModifySnapshotAttributeType.class, "ModifySnapshotAttribute", new ModifySnapshotAttributeType(
                    snapshotId: 'snap-0000001',
                    queryUserId: ['111111111111', '222222222222', '333333333333'] as ArrayList<String>,
                    queryUserGroup: ['all'] as ArrayList<String>,
                    productCodes: [] as ArrayList<String>,
                    attribute: 'createVolumePermission',
                    operationType: 'add',
            ), [
                    SnapshotId     : 'snap-0000001',
                    'UserId.1'     : '111111111111',
                    'UserId.2'     : '222222222222',
                    'UserId.3'     : '333333333333',
                    'Group.1'      : 'all',
                    'Attribute'    : 'createVolumePermission',
                    'OperationType': 'add',
            ])

            bindAndAssertParameters(eb, ModifySnapshotAttributeType.class, "ModifySnapshotAttribute", new ModifySnapshotAttributeType(
                    snapshotId: 'snap-0000001',
                    queryUserId: ['111111111111', '222222222222', '333333333333'] as ArrayList<String>,
                    queryUserGroup: ['all'] as ArrayList<String>,
                    productCodes: [] as ArrayList<String>,
                    attribute: 'createVolumePermission',
                    operationType: 'add',
            ), [
                    SnapshotId     : 'snap-0000001',
                    'UserId.1'     : '111111111111',
                    'UserId.2'     : '222222222222',
                    'UserId.3'     : '333333333333',
                    'Group.1'      : 'all',
                    'Attribute'    : 'createVolumePermission',
                    'OperationType': 'add',
            ])

            //Describe create volume permission
            bindAndAssertParameters(eb, DescribeSnapshotAttributeType.class, "DescribeSnapshotAttribute", new DescribeSnapshotAttributeType(
                    snapshotId: 'snap-0000001',
                    createVolumePermission: 'hi',
                    attribute: 'createVolumePermission',
                    productCodes: 'hi'

            ), [
                    SnapshotId : 'snap-0000001',
                    'Attribute': 'createVolumePermission',
            ])

            //Describe product codes
            bindAndAssertParameters(eb, DescribeSnapshotAttributeType.class, "DescribeSnapshotAttribute", new DescribeSnapshotAttributeType(
                    snapshotId: 'snap-0000001',
                    createVolumePermission: 'hi',
                    attribute: 'productCodes',
                    productCodes: 'hi'

            ), [
                    SnapshotId : 'snap-0000001',
                    'Attribute': 'productCodes'
            ])

            //Reset
            bindAndAssertParameters(eb, ResetSnapshotAttributeType.class, "ResetSnapshotAttribute", new ResetSnapshotAttributeType(
                    snapshotId: 'snap-0000001',
                    createVolumePermission: 'createVolumePermission',
            ), [
                    SnapshotId : 'snap-0000001',
                    'Attribute': 'createVolumePermission',
            ])
        }
    }

  @Test
  void testModifyVpcAttrMessageQueryBindings() {
    URL resource = ComputeQueryBindingTest.class.getResource('/ec2-vpc-10-06-15.xml')

    String version = "2010-06-15"
    ComputeQueryBinding eb = new ComputeQueryBinding() {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass(final String operationName) {
        createTestBindingFromXml(resource, operationName)
      }

      @Override
      String getNamespace() {
        return getNamespaceForVersion(version);
      }

      @Override
      protected void validateBinding(final com.eucalyptus.binding.Binding currentBinding,
                                     final String operationName,
                                     final Map<String, String> params,
                                     final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    }

    // ModifyImageAttribute - 2010-06-15
    bindAndAssertParameters(eb, ModifyVpcAttributeType.class, "ModifyVpcAttribute", new ModifyVpcAttributeType(
        vpcId: 'vpc-0000001',
        enableDnsHostnames: new AttributeBooleanValueType( value: true ),
    ), [
        VpcId                     : 'vpc-0000001',
        'EnableDnsHostnames.Value': 'true',
    ]).with {
      assertNull( 'Expected null dns support attribute', enableDnsSupport )
      assertNotNull( 'Expected non-null dns hostnames attribute', enableDnsHostnames )
      assertEquals( 'EnableDnsHostnames value', true, enableDnsHostnames.value )
    }
  }

  @Test
  void testImportInstanceMessageQueryBindings() {
    URL resource = ComputeQueryBindingTest.class.getResource('/ec2-import-13-10-15.xml')

    String version = "2013-10-15"
    ComputeQueryBinding eb = new ComputeQueryBinding() {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass(final String operationName) {
        createTestBindingFromXml(resource, operationName)
      }

      @Override
      String getNamespace() {
        return getNamespaceForVersion(version);
      }

      @Override
      protected void validateBinding(final com.eucalyptus.binding.Binding currentBinding,
                                     final String operationName,
                                     final Map<String, String> params,
                                     final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    }

    // ImportInstance \w  LaunchSpecification.UserData
    bindAndAssertParameters(eb, ImportInstanceType.class, "ImportInstance", new ImportInstanceType(
        diskImageSet: [
            new DiskImage(
                image: new DiskImageDetail(
                    bytes: 1231323l,
                    format: 'raw',
                    importManifestUrl: 'http://foo/bar'
                ),
                volume: new DiskImageVolume(
                    size: 4
                ),
            )
        ] as ArrayList<DiskImage>,
        launchSpecification: new ImportInstanceLaunchSpecification(
            architecture: 'x86_64',
            groupName: ['import_instance_test_group'] as ArrayList<String>,
            instanceType: 'c1.medium',
            keyName: 'ImportInstanceTests_key_1466551088.49',
            monitoring: new MonitoringInstance(enabled: false),
            placement: new InstancePlacement(availabilityZone: 'one'),
            userData: new UserData(data: 'I2Nsb3VkLWNvbmZpZwpkaXNhYmxlX3Jvb3Q6IGZhbHNl')
        ),
        platform: 'Linux'
    ), [
        'DiskImage.1.Image.Bytes'                       : '1231323',
        'DiskImage.1.Image.Format'                      : 'raw',
        'DiskImage.1.Image.ImportManifestUrl'           : 'http://foo/bar',
        'DiskImage.1.Volume.Size'                       : '4',
        'LaunchSpecification.Architecture'              : 'x86_64',
        'LaunchSpecification.GroupName.1'               : 'import_instance_test_group',
        'LaunchSpecification.InstanceType'              : 'c1.medium',
        'LaunchSpecification.KeyName'                   : 'ImportInstanceTests_key_1466551088.49',
        'LaunchSpecification.Monitoring.Enabled'        : 'False',
        'LaunchSpecification.Placement.AvailabilityZone': 'one',
        'LaunchSpecification.UserData'                  : 'I2Nsb3VkLWNvbmZpZwpkaXNhYmxlX3Jvb3Q6IGZhbHNl',
        'Platform'                                      : 'Linux',
    ])

    // ImportInstance \w  LaunchSpecification.UserData.Data
    bindAndAssertParameters(eb, ImportInstanceType.class, "ImportInstance", new ImportInstanceType(
        diskImageSet: [
            new DiskImage(
                image: new DiskImageDetail(
                    bytes: 1231323l,
                    format: 'raw',
                    importManifestUrl: 'http://foo/bar'
                ),
                volume: new DiskImageVolume(
                    size: 4
                ),
            )
        ] as ArrayList<DiskImage>,
        launchSpecification: new ImportInstanceLaunchSpecification(
            architecture: 'x86_64',
            groupName: ['import_instance_test_group'] as ArrayList<String>,
            instanceType: 'c1.medium',
            keyName: 'ImportInstanceTests_key_1466551088.49',
            monitoring: new MonitoringInstance(enabled: false),
            placement: new InstancePlacement(availabilityZone: 'one'),
            userData: new UserData(data: 'I2Nsb3VkLWNvbmZpZwpkaXNhYmxlX3Jvb3Q6IGZhbHNl')
        ),
        platform: 'Linux'
    ), [
        'DiskImage.1.Image.Bytes'                       : '1231323',
        'DiskImage.1.Image.Format'                      : 'raw',
        'DiskImage.1.Image.ImportManifestUrl'           : 'http://foo/bar',
        'DiskImage.1.Volume.Size'                       : '4',
        'LaunchSpecification.Architecture'              : 'x86_64',
        'LaunchSpecification.GroupName.1'               : 'import_instance_test_group',
        'LaunchSpecification.InstanceType'              : 'c1.medium',
        'LaunchSpecification.KeyName'                   : 'ImportInstanceTests_key_1466551088.49',
        'LaunchSpecification.Monitoring.Enabled'        : 'False',
        'LaunchSpecification.Placement.AvailabilityZone': 'one',
        'LaunchSpecification.UserData.Data'             : 'I2Nsb3VkLWNvbmZpZwpkaXNhYmxlX3Jvb3Q6IGZhbHNl',
        'Platform'                                      : 'Linux',
    ])
  }
}

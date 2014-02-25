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

import com.eucalyptus.compute.common.AuthorizeSecurityGroupIngressType
import com.eucalyptus.compute.common.IpPermissionType
import com.eucalyptus.compute.common.LaunchPermissionItemType
import com.eucalyptus.compute.common.LaunchPermissionOperationType
import com.eucalyptus.compute.common.ModifyImageAttributeType
import com.eucalyptus.compute.common.UserIdGroupPairType
import com.eucalyptus.ws.protocol.QueryBindingTestSupport
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
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2006-06-26-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2006_10_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2006-10-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2007_01_03() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2007-01-03-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2007_01_19() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2007-01-19-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2007_03_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2007-03-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2007_08_29() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2007-08-29-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2008_02_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2008-02-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2008_05_05() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2008-05-05-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2008_08_08() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2008-08-08-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2008_12_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2008-12-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2009_03_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2009-03-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2009_04_04() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2009-04-04-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2009_07_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2009-07-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2009_08_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2009-08-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2009_10_31() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2009-10-31-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2009_11_30() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2009-11-30-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2010_06_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2010-06-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2010_08_31() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2010-08-31-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2010_11_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2010-11-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2011_01_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2011-01-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2011_02_28() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2011-02-28-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2011_05_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2011-05-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2011_07_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2011-07-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2011_11_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2011-11-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2011_12_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2011-12-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2011_12_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2011-12-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_03_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-03-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_04_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-04-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_05_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-05-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_06_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-06-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_06_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-06-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_07_20() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-07-20-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_08_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-08-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_10_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-10-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_11_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-11-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_12_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2012-12-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2013_02_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2013-02-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2013_06_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2013-06-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2013_07_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2013-07-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2013_08_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2013-08-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2013_10_01() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2013-10-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2013_10_15() {
    URL resource = ComputeQueryBindingTest.getResource( '/ec2-2013-10-15-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testSecurityMessageQueryBindings() {
    URL resource = ComputeQueryBindingTest.class.getResource( '/ec2-security-11-01-01.xml' )

    String version = "2009-11-30"
    ComputeQueryBinding eb = new ComputeQueryBinding() {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass( final String operationName ) {
        createTestBindingFromXml( resource, operationName )
      }

      @Override
      String getNamespace() {
        return getNamespaceForVersion( version );
      }

      @Override
      protected void validateBinding( final com.eucalyptus.binding.Binding currentBinding,
                                      final String operationName,
                                      final Map<String, String> params,
                                      final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    }

    version = "2010-06-15"
    bindAndAssertObject( eb, AuthorizeSecurityGroupIngressType.class, "AuthorizeSecurityGroupIngress", new AuthorizeSecurityGroupIngressType(
        groupId: 'sg-00000000',
        ipPermissions: [
            new IpPermissionType(
                ipProtocol: "TCP",
                fromPort: 7,
                toPort: 12,
                groups: [
                    new UserIdGroupPairType( sourceGroupId: 'sg-00000001' )
                ] as ArrayList<UserIdGroupPairType>,
                cidrIpRanges: [
                    "0.0.0.0/0"
                ]
            )
        ] as ArrayList<IpPermissionType>,

    ), 6 )

    // AuthorizeSecurityGroupIngress - 2009/11/30  (should be 2009/10/31 according to API documents)
    version = "2009-11-30"
    bindAndAssertParameters( eb, AuthorizeSecurityGroupIngressType.class, "AuthorizeSecurityGroupIngress", new AuthorizeSecurityGroupIngressType(
        groupUserId: 'AAAAAAAAAAAAAAAAAAAAA',
        groupName: 'default',
        ipProtocol: 'tcp',
        fromPort: 7,
        toPort: 12,
        sourceSecurityGroupOwnerId: 'BBBBBBBBBBBBBBBBBBBB',
        sourceSecurityGroupName: 'userb-group',
        cidrIp: '0.0.0.0/0'
    ), [
        UserId: 'AAAAAAAAAAAAAAAAAAAAA',
        GroupName: 'default',
        IpProtocol: 'tcp',
        FromPort: '7',
        ToPort: '12',
        SourceSecurityGroupOwnerId: 'BBBBBBBBBBBBBBBBBBBB',
        SourceSecurityGroupName: 'userb-group',
        CidrIp: '0.0.0.0/0',
    ] )

    // AuthorizeSecurityGroupIngress - 2010/06/15
    version = "2010-06-15"
    bindAndAssertParameters( eb, AuthorizeSecurityGroupIngressType.class, "AuthorizeSecurityGroupIngress", new AuthorizeSecurityGroupIngressType(
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
        GroupName: 'default',
        'IpPermissions.1.IpProtocol': 'tcp',
        'IpPermissions.1.FromPort': '7',
        'IpPermissions.1.ToPort': '12',
        'IpPermissions.1.Groups.1.UserId': 'BBBBBBBBBBBBBBBBBBBB',
        'IpPermissions.1.Groups.1.GroupName': 'userb-group',
        'IpPermissions.1.IpRanges.1.CidrIp': '0.0.0.0/0',
    ] )

    // AuthorizeSecurityGroupIngress - 2011/01/01
    version = "2011-01-01"
    bindAndAssertParameters( eb, AuthorizeSecurityGroupIngressType.class, "AuthorizeSecurityGroupIngress", new AuthorizeSecurityGroupIngressType(
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
        GroupName: 'default',
        GroupId: 'sg-00000001',
        'IpPermissions.1.IpProtocol': 'tcp',
        'IpPermissions.1.FromPort': '7',
        'IpPermissions.1.ToPort': '12',
        'IpPermissions.1.Groups.1.UserId': 'BBBBBBBBBBBBBBBBBBBB',
        'IpPermissions.1.Groups.1.GroupName': 'userb-group',
        'IpPermissions.1.Groups.1.GroupId': 'sg-00000002',
        'IpPermissions.1.IpRanges.1.CidrIp': '0.0.0.0/0',
    ] )
  }

  @Test
  void testImageAttrMessageQueryBindings() {
    URL resource = ComputeQueryBindingTest.class.getResource( '/ec2-image-attr.xml' )

    String version = "2009-11-30"
    ComputeQueryBinding eb = new ComputeQueryBinding() {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass( final String operationName ) {
        createTestBindingFromXml( resource, operationName )
      }

      @Override
      String getNamespace() {
        return getNamespaceForVersion( version );
      }

      @Override
      protected void validateBinding( final com.eucalyptus.binding.Binding currentBinding,
                                      final String operationName,
                                      final Map<String, String> params,
                                      final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    }

    // ModifyImageAttribute - 2010-06-15
    version = "2010-06-15"
    bindAndAssertParameters( eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
        imageId: 'emi-0000001',
        queryUserId: [ '111111111111', '222222222222' ] as ArrayList<String>,
        queryUserGroup: [ 'G1', 'G2' ] as ArrayList<String>,
        productCodes: [ 'Code1', 'Code2' ] as ArrayList<String>,
        attribute: 'launchPermission',
        operationType: 'add',
    ), [
        ImageId: 'emi-0000001',
        'UserId.1': '111111111111',
        'UserId.2': '222222222222',
        'Group.1': 'G1',
        'Group.2': 'G2',
        'ProductCode.1': 'Code1',
        'ProductCode.2': 'Code2',
        'Attribute': 'launchPermission',
        'OperationType': 'add',
    ] ).with {
      assertEquals( 'Attribute', ModifyImageAttributeType.ImageAttribute.LaunchPermission, imageAttribute( ) )
      assertTrue( 'Is add operation', add() )
      assertEquals( 'UserIds', [ '111111111111', '222222222222' ], userIds( ) )
      assertFalse( 'Group all', groupAll( ) )
      assertEquals( 'Launch permissions', [ new LaunchPermissionItemType( userId: '111111111111' ), new LaunchPermissionItemType( userId: '222222222222' ) ] as List<LaunchPermissionItemType>, asAddLaunchPermissionsItemTypes( ) )
    }

    // ModifyImageAttribute - 2010-06-15 - Incorrect 'UserGroup' parameter (backwards compatible)
    version = "2010-06-15"
    bindAndAssertParameters( eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
        imageId: 'emi-0000001',
        queryUserId: [ '111111111111', '222222222222' ] as ArrayList<String>,
        queryUserGroup: [ 'G1', 'G2' ] as ArrayList<String>,
        productCodes: [ 'Code1', 'Code2' ] as ArrayList<String>,
        attribute: 'launchPermission',
        operationType: 'add',
    ), [
        ImageId: 'emi-0000001',
        'UserId.1': '111111111111',
        'UserId.2': '222222222222',
        'UserGroup.1': 'G1',
        'UserGroup.2': 'G2',
        'ProductCode.1': 'Code1',
        'ProductCode.2': 'Code2',
        'Attribute': 'launchPermission',
        'OperationType': 'add',
    ] )

    // ModifyImageAttribute - 2010-06-15 / Non standard group parameter
    version = "2010-06-15"
    bindAndAssertParameters( eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
        imageId: 'emi-0000001',
        queryUserId: [ '111111111111', '222222222222' ] as ArrayList<String>,
        queryUserGroup: [ 'G1', 'G2' ] as ArrayList<String>,
        productCodes: [ 'Code1', 'Code2' ] as ArrayList<String>,
        attribute: 'launchPermission',
        operationType: 'add',
    ), [
        ImageId: 'emi-0000001',
        'UserId.1': '111111111111',
        'UserId.2': '222222222222',
        'UserGroup.1': 'G1',
        'UserGroup.2': 'G2',
        'ProductCode.1': 'Code1',
        'ProductCode.2': 'Code2',
        'Attribute': 'launchPermission',
        'OperationType': 'add',
    ] )

    // ModifyImageAttribute - 2010-08-31
    version = "2010-08-31"
    bindAndAssertParameters( eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
        imageId: 'emi-0000001',
        launchPermission: new LaunchPermissionOperationType(
            add: [
                new LaunchPermissionItemType( userId: '111111111111' ),
                new LaunchPermissionItemType( userId: '222222222222' ),
            ] as ArrayList<LaunchPermissionItemType>,
            remove: [
                new LaunchPermissionItemType( userId: '333333333333' ),
                new LaunchPermissionItemType( userId: '444444444444' ),
            ] as ArrayList<LaunchPermissionItemType>,
        ),
        productCodes: [ 'Code1', 'Code2' ] as ArrayList<String>,
        description: 'An image',
    ), [
        ImageId: 'emi-0000001',
        'LaunchPermission.Add.1.UserId': '111111111111',
        'LaunchPermission.Add.2.UserId': '222222222222',
        'LaunchPermission.Remove.1.UserId': '333333333333',
        'LaunchPermission.Remove.2.UserId': '444444444444',
        'ProductCode.1': 'Code1',
        'ProductCode.2': 'Code2',
        'Description.Value': 'An image',
    ] ).with{ bound ->
      assertEquals( 'Attribute', ModifyImageAttributeType.ImageAttribute.LaunchPermission, imageAttribute( ) )
      assertTrue( 'Is add operation', add() )
      assertEquals( 'UserIds', [ '111111111111', '222222222222' ], userIds( ) )
      assertFalse( 'Group all', groupAll( ) )
      assertEquals( 'Launch permissions', [ new LaunchPermissionItemType( userId: '111111111111' ), new LaunchPermissionItemType( userId: '222222222222' ) ] as List<LaunchPermissionItemType>, asAddLaunchPermissionsItemTypes( ) )
    }

    bindAndAssertParameters( eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
        imageId: 'emi-0000001',
        launchPermission: new LaunchPermissionOperationType(
            add: [
                new LaunchPermissionItemType( group: 'G1' ),
                new LaunchPermissionItemType( group: 'G2' ),
            ] as ArrayList<LaunchPermissionItemType>,
            remove: [
                new LaunchPermissionItemType( group: 'G3' ),
                new LaunchPermissionItemType( group: 'G4' ),
            ] as ArrayList<LaunchPermissionItemType>,
        ),
        productCodes: [ 'Code1', 'Code2' ] as ArrayList<String>,
        description: 'An image',
    ), [
        ImageId: 'emi-0000001',
        'LaunchPermission.Add.1.Group': 'G1',
        'LaunchPermission.Add.2.Group': 'G2',
        'LaunchPermission.Remove.1.Group': 'G3',
        'LaunchPermission.Remove.2.Group': 'G4',
        'ProductCode.1': 'Code1',
        'ProductCode.2': 'Code2',
        'Description.Value': 'An image',
    ] )
  }
}

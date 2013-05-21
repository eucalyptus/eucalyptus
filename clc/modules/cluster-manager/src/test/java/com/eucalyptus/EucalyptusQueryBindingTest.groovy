/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus

import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import static org.junit.Assert.*
import org.junit.Test
import com.eucalyptus.binding.Binding
import edu.ucsb.eucalyptus.msgs.BaseMessage
import com.eucalyptus.ws.handlers.EucalyptusQueryBinding
import edu.ucsb.eucalyptus.msgs.AuthorizeSecurityGroupIngressType
import edu.ucsb.eucalyptus.msgs.IpPermissionType
import edu.ucsb.eucalyptus.msgs.UserIdGroupPairType
import edu.ucsb.eucalyptus.msgs.ModifyImageAttributeType
import edu.ucsb.eucalyptus.msgs.LaunchPermissionOperationType
import edu.ucsb.eucalyptus.msgs.LaunchPermissionItemType

/**
 * 
 */
class EucalyptusQueryBindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding2009_10_31() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/2009-10-31-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2010_08_31() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/2010-08-31-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2011_11_01() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/2011-11-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2012_06_01() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/2012-06-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidBinding2013_02_01() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/2013-02-01-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBindingSecurity() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-security.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testValidQueryBindingSecurity2010_08_31() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-security-10-08-31.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testValidQueryBindingSecurity2011_01_01() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-security-11-01-01.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testValidQueryBindingInstances() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-instances.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testValidQueryBindingInstances2009_10_31() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-instances-09-10-31.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testValidQueryBindingInstances2010_08_31() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-instances-10-08-31.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testValidQueryBindingInstances2011_11_01() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-instances-11-11-01.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testValidQueryBindingInstances2012_06_01() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-instances-12-06-01.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testSecurityMessageQueryBindings() {
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-security-11-01-01.xml' )

    String version = "2009-11-30"
    EucalyptusQueryBinding eb = new EucalyptusQueryBinding() {
      @Override
      protected Binding getBindingWithElementClass( final String operationName ) {
        createTestBindingFromXml( resource, operationName )
      }

      @Override
      String getNamespace() {
        return getNamespaceForVersion( version );
      }

      @Override
      protected void validateBinding( final Binding currentBinding,
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
              ],
              cidrIpRanges: [
                  "0.0.0.0/0"
              ]
          )
        ],

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
                ],
                cidrIpRanges: [
                    '0.0.0.0/0'
                ]
            )
        ],

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
                ],
                cidrIpRanges: [
                    '0.0.0.0/0'
                ]
            )
        ],

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
    URL resource = EucalyptusQueryBindingTest.class.getResource( '/aws-image-attr.xml' )

    String version = "2009-11-30"
    EucalyptusQueryBinding eb = new EucalyptusQueryBinding() {
      @Override
      protected Binding getBindingWithElementClass( final String operationName ) {
        createTestBindingFromXml( resource, operationName )
      }

      @Override
      String getNamespace() {
        return getNamespaceForVersion( version );
      }

      @Override
      protected void validateBinding( final Binding currentBinding,
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
        queryUserId: [ '111111111111', '222222222222' ],
        queryUserGroup: [ 'G1', 'G2' ],
        productCodes: [ 'Code1', 'Code2' ],
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
    ] ).with { bound ->
      assertEquals( 'Attribute', ModifyImageAttributeType.ImageAttribute.LaunchPermission, bound.getImageAttribute( ) )
      assertTrue( 'Is add operation', bound.isAdd() )
      assertEquals( 'UserIds', [ '111111111111', '222222222222' ], bound.getUserIds( ) )
      assertFalse( 'Group all', bound.isGroupAll( ) )
      assertEquals( 'Launch permissions', [ new LaunchPermissionItemType( userId: '111111111111' ), new LaunchPermissionItemType( userId: '222222222222' ) ] as List<LaunchPermissionItemType>, bound.getAdd( ) )
    }

    // ModifyImageAttribute - 2010-06-15 - Incorrect 'UserGroup' parameter (backwards compatible)
    version = "2010-06-15"
    bindAndAssertParameters( eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
        imageId: 'emi-0000001',
        queryUserId: [ '111111111111', '222222222222' ],
        queryUserGroup: [ 'G1', 'G2' ],
        productCodes: [ 'Code1', 'Code2' ],
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
        queryUserId: [ '111111111111', '222222222222' ],
        queryUserGroup: [ 'G1', 'G2' ],
        productCodes: [ 'Code1', 'Code2' ],
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
            ],
            remove: [
                new LaunchPermissionItemType( userId: '333333333333' ),
                new LaunchPermissionItemType( userId: '444444444444' ),
            ],
        ),
        productCodes: [ 'Code1', 'Code2' ],
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
      assertEquals( 'Attribute', ModifyImageAttributeType.ImageAttribute.LaunchPermission, bound.getImageAttribute( ) )
      assertTrue( 'Is add operation', bound.isAdd() )
      assertEquals( 'UserIds', [ '111111111111', '222222222222' ], bound.getUserIds( ) )
      assertFalse( 'Group all', bound.isGroupAll( ) )
      assertEquals( 'Launch permissions', [ new LaunchPermissionItemType( userId: '111111111111' ), new LaunchPermissionItemType( userId: '222222222222' ) ] as List<LaunchPermissionItemType>, bound.getAdd( ) )
    }

    bindAndAssertParameters( eb, ModifyImageAttributeType.class, "ModifyImageAttribute", new ModifyImageAttributeType(
        imageId: 'emi-0000001',
        launchPermission: new LaunchPermissionOperationType(
            add: [
                new LaunchPermissionItemType( group: 'G1' ),
                new LaunchPermissionItemType( group: 'G2' ),
            ],
            remove: [
                new LaunchPermissionItemType( group: 'G3' ),
                new LaunchPermissionItemType( group: 'G4' ),
            ],
        ),
        productCodes: [ 'Code1', 'Code2' ],
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

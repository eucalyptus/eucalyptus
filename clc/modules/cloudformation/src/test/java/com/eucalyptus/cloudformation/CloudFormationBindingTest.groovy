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
package com.eucalyptus.cloudformation

import static org.junit.Assert.*
import org.junit.Test

import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.cloudformation.ws.CloudFormationQueryBinding;
import com.eucalyptus.ws.protocol.QueryBindingTestSupport

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * 
 */
class CloudFormationBindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding() {
    URL resource = CloudFormationBindingTest.class.getResource( '/cloudformation-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = CloudFormationBindingTest.class.getResource( '/cloudformation-binding.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testMessageQueryBindings() {
    URL resource = CloudFormationBindingTest.class.getResource( '/cloudformation-binding.xml' )
    CloudFormationQueryBinding asb = new CloudFormationQueryBinding() {
      @Override
      protected Binding getBindingWithElementClass(String operationName)
          throws BindingException {
            createTestBindingFromXml( resource, operationName )
     }

      @Override
      protected void validateBinding(Binding currentBinding,
          String operationName, Map<String, String> params, BaseMessage eucaMsg)
          throws BindingException {
          // Validation requires compiled bindings
      } 
    }

        // CancelUpdateStack
    bindAndAssertObject( asb, CancelUpdateStackType.class, "CancelUpdateStack", new CancelUpdateStackType(
      stackName: 'Stack-Name'
    ), 1)

    // CreateStack
    bindAndAssertObject( asb, CreateStackType.class, "CreateStack", new CreateStackType(
      capabilities: new ResourceList (
        member: [ 'capability1', 'capability2' ],
      ),
      disableRollback: true,
      notificationARNs: new ResourceList (
        member: [ 'notificationARN1', 'notificationARN2', 'notificationARN3' ],
      ),
      onFailure: 'fail',
      parameters: new Parameters (
        member: [
          new Parameter(
            parameterKey: 'Parameter-Key-1',
            parameterValue: 'Parameter-Value-1'
          ),
          new Parameter(
            parameterKey: 'Parameter-Key-2',
            parameterValue: 'Parameter-Value-2'
          )
        ]
      ),
      stackName: 'Stack-Name',
      stackPolicyBody: 'Stack-Policy-Body',
      stackPolicyURL: 'Stack-Policy-URL',
      tags: new Tags (
        member: [
          new Tag(
            key: 'Key-1',
            value: 'Value-1'
          )
        ]
      ),
      templateBody: 'Template-Body',
      templateURL: 'Template-URL',
      timeoutInMinutes: 10
    ), 19)
    // DeleteStack
    bindAndAssertObject( asb, DeleteStackType.class, "DeleteStack", new DeleteStackType(
      stackName: 'Stack-Name'
    ), 1)
    // DeleteStack
    bindAndAssertObject( asb, DeleteStackType.class, "DeleteStack", new DeleteStackType(
      stackName: 'Stack-Name'
    ), 1)
    // DescribeStackEvents
    bindAndAssertObject( asb, DescribeStackEventsType.class, "DescribeStackEvents", new DescribeStackEventsType(
      nextToken: 'Next-Token',
      stackName: 'Stack-Name'
    ), 2)
    // DescribeStackResource
    bindAndAssertObject( asb, DescribeStackResourceType.class, "DescribeStackResource", new DescribeStackResourceType(
      logicalResourceId: 'Logical-Resource-Id',
      stackName: 'Stack-Name'
    ), 2)
    // DescribeStackResources
    bindAndAssertObject( asb, DescribeStackResourcesType.class, "DescribeStackResources", new DescribeStackResourcesType(
      logicalResourceId: 'Logical-Resource-Id',
      physicalResourceId: 'Physical-Resource-Id',
      stackName: 'Stack-Name'
    ), 3)
    // DescribeStacks
    bindAndAssertObject( asb, DescribeStacksType.class, "DescribeStacks", new DescribeStacksType(
      nextToken: 'Next-Token',
      stackName: 'Stack-Name'
    ), 2)
    // EstimateTemplateCost
    bindAndAssertObject( asb, EstimateTemplateCostType.class, "EstimateTemplateCost", new EstimateTemplateCostType(
      parameters: new Parameters (
        member: [
          new Parameter(
            parameterKey: 'Parameter-Key-1',
            parameterValue: 'Parameter-Value-1'
          ),
          new Parameter(
            parameterKey: 'Parameter-Key-2',
            parameterValue: 'Parameter-Value-2'
          )
        ]
      ),
      templateBody: 'Template-Body',
      templateURL: 'Template-URL'
    ), 6)
    // GetStackPolicy
    bindAndAssertObject( asb, GetStackPolicyType.class, "GetStackPolicy", new GetStackPolicyType(
      stackName: 'Stack-Name'
    ), 1)
    // GetTemplate
    bindAndAssertObject( asb, GetTemplateType.class, "GetTemplate", new GetTemplateType(
      stackName: 'Stack-Name'
    ), 1)
    // ListStackResources
    bindAndAssertObject( asb, ListStackResourcesType.class, "ListStackResources", new ListStackResourcesType(
      nextToken: 'Next-Token',
      stackName: 'Stack-Name'
    ), 2)
    // ListStacks
    bindAndAssertObject( asb, ListStacksType.class, "ListStacks", new ListStacksType(
      nextToken: 'Next-Token',
      stackStatusFilter: new ResourceList(
        member: ['Stack-Status-Filter-1', 'Stack-Status-Filter-2']
      )
    ), 3)
    // SetStackPolicy
    bindAndAssertObject( asb, SetStackPolicyType.class, "SetStackPolicy", new SetStackPolicyType(
      stackName: 'Stack-Name',
      stackPolicyBody: 'Stack-Policy-Body',
      stackPolicyURL: 'Stack-Policy-URL'
    ), 3)
    // UpdateStack
    bindAndAssertObject( asb, UpdateStackType.class, "UpdateStack", new UpdateStackType(
      capabilities: new ResourceList (
        member: [ 'capability1', 'capability2' ],
      ),
      parameters: new Parameters (
        member: [
          new Parameter(
            parameterKey: 'Parameter-Key-1',
            parameterValue: 'Parameter-Value-1'
          ),
          new Parameter(
            parameterKey: 'Parameter-Key-2',
            parameterValue: 'Parameter-Value-2'
          )
        ]
      ),
      stackName: 'Stack-Name',
      stackPolicyBody : 'Stack-Policy-Body',
      stackPolicyDuringUpdateBody: 'Stack-Policy-During-Update-Body',
      stackPolicyDuringUpdateURL: 'Stack-Policy-During-Update-URL',
      stackPolicyURL: 'Stack-Policy-URL',
      templateBody: 'Template-Body',
      templateURL: 'Template-URL'
    ), 13 ) 
    // ValidateTemplate
    bindAndAssertObject( asb, ValidateTemplateType.class, "ValidateTemplate", new ValidateTemplateType(
      templateBody: 'Template-Body',
      templateURL: 'Template-URL'
    ), 2 )
  }

  @Test
  void testDateFormat( ) {
    assertEquals(
        'JSON output with date', '{"Date":0.001}',
        CloudFormationQueryBinding.jsonWriter( ).writeValueAsString( new TestBean( ) ) )
  }

  static class TestBean {
    Date date = new Date( 1 )
  }
}


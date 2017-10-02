/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.cloudformation

import com.eucalyptus.binding.Binding
import com.eucalyptus.binding.BindingException
import com.eucalyptus.cloudformation.common.msgs.CancelUpdateStackType
import com.eucalyptus.cloudformation.common.msgs.CloudFormationErrorResponse
import com.eucalyptus.cloudformation.common.msgs.CreateStackType
import com.eucalyptus.cloudformation.common.msgs.DeleteStackType
import com.eucalyptus.cloudformation.common.msgs.DescribeStackEventsType
import com.eucalyptus.cloudformation.common.msgs.DescribeStackResourceType
import com.eucalyptus.cloudformation.common.msgs.DescribeStackResourcesType
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksType
import com.eucalyptus.cloudformation.common.msgs.Error
import com.eucalyptus.cloudformation.common.msgs.EstimateTemplateCostType
import com.eucalyptus.cloudformation.common.msgs.GetStackPolicyType
import com.eucalyptus.cloudformation.common.msgs.GetTemplateType
import com.eucalyptus.cloudformation.common.msgs.ListStackResourcesType
import com.eucalyptus.cloudformation.common.msgs.ListStacksType
import com.eucalyptus.cloudformation.common.msgs.Parameter
import com.eucalyptus.cloudformation.common.msgs.Parameters
import com.eucalyptus.cloudformation.common.msgs.ResourceList
import com.eucalyptus.cloudformation.common.msgs.SetStackPolicyType
import com.eucalyptus.cloudformation.common.msgs.Tag
import com.eucalyptus.cloudformation.common.msgs.Tags
import com.eucalyptus.cloudformation.common.msgs.UpdateStackType
import com.eucalyptus.cloudformation.common.msgs.ValidateTemplateType
import com.eucalyptus.cloudformation.ws.CloudFormationQueryBinding
import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.junit.Test

import static org.junit.Assert.assertEquals;

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
  void testInternalRoundTrip() {
    URL resource = CloudFormationBindingTest.class.getResource( '/cloudformation-binding.xml' )
    assertValidInternalRoundTrip( resource )
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

  @Test
  void testErrorFormat( ) {
    assertEquals(
        'JSON error response',
        '''\
        {
          "RequestId" : "edbe8968-c437-11e4-bac0-25d29c2758a9",
          "Error" : {
            "Type" : "Sender",
            "Code" : "ValidationError",
            "Message" : "Template file referenced by https://cf-template-429942273585-us-west-1.s3.amazonaws.com/cfn-as-launchconfig-paravirt-instanceprofile.json?Signature=W9mgzWRlMAqZe1n0%2BM9gmmFLIF8%3D&Expires=1425670813&AWSAccessKeyId=ASIAIPHT3XGEH2GINPHQ&x-amz-security-token=AQoDYXdzEN3//////////wEa8AHGhON5Y9xtxXv79F9WCUnNWHAo4We9n4GIQGsrOJ9HmW/Niksj/m7mliigTqw0GTQS48R/v8bVuRo5B8BYWMVBqPvJ%2BIEKozO%2B48grz17fjy8gflb%2BGbwv/N4Fb0KORiX51GCm0xosId5dMBcrbJH5pn2fvwItEz2CvEnYTG3WN/abDuJD6qJeqvRGqIgG%2BFtjP/voQDWeIw5%2BFSwhFXyd29tu3NX12E8oy1DsO/Nox6UO92%2Ba8T0%2B0zcwn3JPZsLlDHutpSgNEjV85xolGEAQQt1FYihnTn/lkTL2UXcISgNWUKbsmZ%2BJQZqQR0ocY04gm4DopwU%3D does not exist."
          }
        }'''.stripIndent( ),
        CloudFormationQueryBinding.jsonWriter( ).withDefaultPrettyPrinter().writeValueAsString( new CloudFormationErrorResponse(
            effectiveUserId: '',
            statusMessage: '',
            _epoch: 42,
            requestId: 'edbe8968-c437-11e4-bac0-25d29c2758a9',
            error: new Error(
                code: 'ValidationError',
                message: 'Template file referenced by https://cf-template-429942273585-us-west-1.s3.amazonaws.com/cfn-as-launchconfig-paravirt-instanceprofile.json?Signature=W9mgzWRlMAqZe1n0%2BM9gmmFLIF8%3D&Expires=1425670813&AWSAccessKeyId=ASIAIPHT3XGEH2GINPHQ&x-amz-security-token=AQoDYXdzEN3//////////wEa8AHGhON5Y9xtxXv79F9WCUnNWHAo4We9n4GIQGsrOJ9HmW/Niksj/m7mliigTqw0GTQS48R/v8bVuRo5B8BYWMVBqPvJ%2BIEKozO%2B48grz17fjy8gflb%2BGbwv/N4Fb0KORiX51GCm0xosId5dMBcrbJH5pn2fvwItEz2CvEnYTG3WN/abDuJD6qJeqvRGqIgG%2BFtjP/voQDWeIw5%2BFSwhFXyd29tu3NX12E8oy1DsO/Nox6UO92%2Ba8T0%2B0zcwn3JPZsLlDHutpSgNEjV85xolGEAQQt1FYihnTn/lkTL2UXcISgNWUKbsmZ%2BJQZqQR0ocY04gm4DopwU%3D does not exist.',
                type: 'Sender'
            )
        ) ) )
  }

  static class TestBean {
    Date date = new Date( 1 )
  }
}


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

package com.eucalyptus.simpleworkflow.common.model

import org.junit.Test
import static org.junit.Assert.*

/**
 *
 */
class SimpleWorkflowMessageValidationTest {

  @Test
  void testValid() {
    RegisterDomainRequest registerDomainRequest = new RegisterDomainRequest(
        name: 'Name',
        workflowExecutionRetentionPeriodInDays: 'NONE'
    )

    assertEquals( "Errors", [:], registerDomainRequest.validate() )
  }

  @Test
  void testBasicValidation() {
    RegisterDomainRequest registerDomainRequest = new RegisterDomainRequest(
        name: ' ',
        workflowExecutionRetentionPeriodInDays: '-1'
    )

    Map<String,String> result = registerDomainRequest.validate( )
    assertEquals( "Name error", "' ' for parameter Name is invalid", result["Name"] )
    assertEquals( "Retention period error", "'-1' for parameter WorkflowExecutionRetentionPeriodInDays is invalid", result["WorkflowExecutionRetentionPeriodInDays"] )
    assertEquals( "Error count", 2, result.size() )
  }

  @Test
  void testEmbeddedValidation() {
    RegisterWorkflowTypeRequest registerWorkflowTypeRequest = new RegisterWorkflowTypeRequest(
        domain: 'domain',
        name: 'name',
        version: 'version',
        defaultTaskList: new TaskList()
    )

    assertEquals( "Embedded validation result", ["DefaultTaskList.Name": "DefaultTaskList.Name is required"], registerWorkflowTypeRequest.validate() );
  }
}

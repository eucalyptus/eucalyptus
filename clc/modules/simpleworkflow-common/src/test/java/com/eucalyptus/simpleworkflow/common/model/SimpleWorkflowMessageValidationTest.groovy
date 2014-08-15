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

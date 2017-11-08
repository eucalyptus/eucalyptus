/*************************************************************************
 * Copyright 2014 Ent. Services Development Corporation LP
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2015 Amazon.com, Inc. or its affiliates.
 *   All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is
 *   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 *   ANY KIND, either express or implied. See the License for the specific
 *   language governing permissions and limitations under the License.
 ************************************************************************/
package com.eucalyptus.simpleworkflow.common.model;

/**
 * Continue As New Workflow Execution Failed Cause
 */
public enum ContinueAsNewWorkflowExecutionFailedCause {
    
    UNHANDLED_DECISION("UNHANDLED_DECISION"),
    WORKFLOW_TYPE_DEPRECATED("WORKFLOW_TYPE_DEPRECATED"),
    WORKFLOW_TYPE_DOES_NOT_EXIST("WORKFLOW_TYPE_DOES_NOT_EXIST"),
    DEFAULT_EXECUTION_START_TO_CLOSE_TIMEOUT_UNDEFINED("DEFAULT_EXECUTION_START_TO_CLOSE_TIMEOUT_UNDEFINED"),
    DEFAULT_TASK_START_TO_CLOSE_TIMEOUT_UNDEFINED("DEFAULT_TASK_START_TO_CLOSE_TIMEOUT_UNDEFINED"),
    DEFAULT_TASK_LIST_UNDEFINED("DEFAULT_TASK_LIST_UNDEFINED"),
    DEFAULT_CHILD_POLICY_UNDEFINED("DEFAULT_CHILD_POLICY_UNDEFINED"),
    CONTINUE_AS_NEW_WORKFLOW_EXECUTION_RATE_EXCEEDED("CONTINUE_AS_NEW_WORKFLOW_EXECUTION_RATE_EXCEEDED"),
    OPERATION_NOT_PERMITTED("OPERATION_NOT_PERMITTED");

    private String value;

    private ContinueAsNewWorkflowExecutionFailedCause(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Use this in place of valueOf.
     *
     * @param value
     *            real value
     * @return ContinueAsNewWorkflowExecutionFailedCause corresponding to the value
     */
    public static ContinueAsNewWorkflowExecutionFailedCause fromValue(String value) {
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        
        } else if ("UNHANDLED_DECISION".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.UNHANDLED_DECISION;
        } else if ("WORKFLOW_TYPE_DEPRECATED".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.WORKFLOW_TYPE_DEPRECATED;
        } else if ("WORKFLOW_TYPE_DOES_NOT_EXIST".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.WORKFLOW_TYPE_DOES_NOT_EXIST;
        } else if ("DEFAULT_EXECUTION_START_TO_CLOSE_TIMEOUT_UNDEFINED".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.DEFAULT_EXECUTION_START_TO_CLOSE_TIMEOUT_UNDEFINED;
        } else if ("DEFAULT_TASK_START_TO_CLOSE_TIMEOUT_UNDEFINED".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.DEFAULT_TASK_START_TO_CLOSE_TIMEOUT_UNDEFINED;
        } else if ("DEFAULT_TASK_LIST_UNDEFINED".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.DEFAULT_TASK_LIST_UNDEFINED;
        } else if ("DEFAULT_CHILD_POLICY_UNDEFINED".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.DEFAULT_CHILD_POLICY_UNDEFINED;
        } else if ("CONTINUE_AS_NEW_WORKFLOW_EXECUTION_RATE_EXCEEDED".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.CONTINUE_AS_NEW_WORKFLOW_EXECUTION_RATE_EXCEEDED;
        } else if ("OPERATION_NOT_PERMITTED".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.OPERATION_NOT_PERMITTED;
        } else {
            throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
        }
    }
}
    
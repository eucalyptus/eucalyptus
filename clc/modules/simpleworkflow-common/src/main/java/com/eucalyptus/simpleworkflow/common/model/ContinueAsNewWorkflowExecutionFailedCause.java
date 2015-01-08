/*************************************************************************
 * Copyright 2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you
 * need additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights
 *   Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *    http://aws.amazon.com/apache2.0
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
        } else if ("OPERATION_NOT_PERMITTED".equals(value)) {
            return ContinueAsNewWorkflowExecutionFailedCause.OPERATION_NOT_PERMITTED;
        } else {
            throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
        }
    }
}
    
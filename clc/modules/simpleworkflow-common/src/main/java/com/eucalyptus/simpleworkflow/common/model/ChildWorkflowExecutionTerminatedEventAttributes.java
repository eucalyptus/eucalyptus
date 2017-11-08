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

import java.io.Serializable;

/**
 * <p>
 * Provides details of the <code>ChildWorkflowExecutionTerminated</code>
 * event.
 * </p>
 */
public class ChildWorkflowExecutionTerminatedEventAttributes implements Serializable {

    /**
     * The child workflow execution that was terminated.
     */
    private WorkflowExecution workflowExecution;

    /**
     * The type of the child workflow execution.
     */
    private WorkflowType workflowType;

    /**
     * The ID of the <code>StartChildWorkflowExecutionInitiated</code> event
     * corresponding to the <code>StartChildWorkflowExecution</code> decision
     * to start this child workflow execution. This information can be useful
     * for diagnosing problems by tracing back the chain of events leading up
     * to this event.
     */
    private Long initiatedEventId;

    /**
     * The ID of the <code>ChildWorkflowExecutionStarted</code> event
     * recorded when this child workflow execution was started. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event.
     */
    private Long startedEventId;

    /**
     * The child workflow execution that was terminated.
     *
     * @return The child workflow execution that was terminated.
     */
    public WorkflowExecution getWorkflowExecution() {
        return workflowExecution;
    }
    
    /**
     * The child workflow execution that was terminated.
     *
     * @param workflowExecution The child workflow execution that was terminated.
     */
    public void setWorkflowExecution(WorkflowExecution workflowExecution) {
        this.workflowExecution = workflowExecution;
    }
    
    /**
     * The child workflow execution that was terminated.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param workflowExecution The child workflow execution that was terminated.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ChildWorkflowExecutionTerminatedEventAttributes withWorkflowExecution(WorkflowExecution workflowExecution) {
        this.workflowExecution = workflowExecution;
        return this;
    }

    /**
     * The type of the child workflow execution.
     *
     * @return The type of the child workflow execution.
     */
    public WorkflowType getWorkflowType() {
        return workflowType;
    }
    
    /**
     * The type of the child workflow execution.
     *
     * @param workflowType The type of the child workflow execution.
     */
    public void setWorkflowType(WorkflowType workflowType) {
        this.workflowType = workflowType;
    }
    
    /**
     * The type of the child workflow execution.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param workflowType The type of the child workflow execution.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ChildWorkflowExecutionTerminatedEventAttributes withWorkflowType(WorkflowType workflowType) {
        this.workflowType = workflowType;
        return this;
    }

    /**
     * The ID of the <code>StartChildWorkflowExecutionInitiated</code> event
     * corresponding to the <code>StartChildWorkflowExecution</code> decision
     * to start this child workflow execution. This information can be useful
     * for diagnosing problems by tracing back the chain of events leading up
     * to this event.
     *
     * @return The ID of the <code>StartChildWorkflowExecutionInitiated</code> event
     *         corresponding to the <code>StartChildWorkflowExecution</code> decision
     *         to start this child workflow execution. This information can be useful
     *         for diagnosing problems by tracing back the chain of events leading up
     *         to this event.
     */
    public Long getInitiatedEventId() {
        return initiatedEventId;
    }
    
    /**
     * The ID of the <code>StartChildWorkflowExecutionInitiated</code> event
     * corresponding to the <code>StartChildWorkflowExecution</code> decision
     * to start this child workflow execution. This information can be useful
     * for diagnosing problems by tracing back the chain of events leading up
     * to this event.
     *
     * @param initiatedEventId The ID of the <code>StartChildWorkflowExecutionInitiated</code> event
     *         corresponding to the <code>StartChildWorkflowExecution</code> decision
     *         to start this child workflow execution. This information can be useful
     *         for diagnosing problems by tracing back the chain of events leading up
     *         to this event.
     */
    public void setInitiatedEventId(Long initiatedEventId) {
        this.initiatedEventId = initiatedEventId;
    }
    
    /**
     * The ID of the <code>StartChildWorkflowExecutionInitiated</code> event
     * corresponding to the <code>StartChildWorkflowExecution</code> decision
     * to start this child workflow execution. This information can be useful
     * for diagnosing problems by tracing back the chain of events leading up
     * to this event.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param initiatedEventId The ID of the <code>StartChildWorkflowExecutionInitiated</code> event
     *         corresponding to the <code>StartChildWorkflowExecution</code> decision
     *         to start this child workflow execution. This information can be useful
     *         for diagnosing problems by tracing back the chain of events leading up
     *         to this event.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ChildWorkflowExecutionTerminatedEventAttributes withInitiatedEventId(Long initiatedEventId) {
        this.initiatedEventId = initiatedEventId;
        return this;
    }

    /**
     * The ID of the <code>ChildWorkflowExecutionStarted</code> event
     * recorded when this child workflow execution was started. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event.
     *
     * @return The ID of the <code>ChildWorkflowExecutionStarted</code> event
     *         recorded when this child workflow execution was started. This
     *         information can be useful for diagnosing problems by tracing back the
     *         chain of events leading up to this event.
     */
    public Long getStartedEventId() {
        return startedEventId;
    }
    
    /**
     * The ID of the <code>ChildWorkflowExecutionStarted</code> event
     * recorded when this child workflow execution was started. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event.
     *
     * @param startedEventId The ID of the <code>ChildWorkflowExecutionStarted</code> event
     *         recorded when this child workflow execution was started. This
     *         information can be useful for diagnosing problems by tracing back the
     *         chain of events leading up to this event.
     */
    public void setStartedEventId(Long startedEventId) {
        this.startedEventId = startedEventId;
    }
    
    /**
     * The ID of the <code>ChildWorkflowExecutionStarted</code> event
     * recorded when this child workflow execution was started. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param startedEventId The ID of the <code>ChildWorkflowExecutionStarted</code> event
     *         recorded when this child workflow execution was started. This
     *         information can be useful for diagnosing problems by tracing back the
     *         chain of events leading up to this event.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ChildWorkflowExecutionTerminatedEventAttributes withStartedEventId(Long startedEventId) {
        this.startedEventId = startedEventId;
        return this;
    }

    /**
     * Returns a string representation of this object; useful for testing and
     * debugging.
     *
     * @return A string representation of this object.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getWorkflowExecution() != null) sb.append("WorkflowExecution: " + getWorkflowExecution() + ",");
        if (getWorkflowType() != null) sb.append("WorkflowType: " + getWorkflowType() + ",");
        if (getInitiatedEventId() != null) sb.append("InitiatedEventId: " + getInitiatedEventId() + ",");
        if (getStartedEventId() != null) sb.append("StartedEventId: " + getStartedEventId() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getWorkflowExecution() == null) ? 0 : getWorkflowExecution().hashCode()); 
        hashCode = prime * hashCode + ((getWorkflowType() == null) ? 0 : getWorkflowType().hashCode()); 
        hashCode = prime * hashCode + ((getInitiatedEventId() == null) ? 0 : getInitiatedEventId().hashCode()); 
        hashCode = prime * hashCode + ((getStartedEventId() == null) ? 0 : getStartedEventId().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof ChildWorkflowExecutionTerminatedEventAttributes == false) return false;
        ChildWorkflowExecutionTerminatedEventAttributes other = (ChildWorkflowExecutionTerminatedEventAttributes)obj;
        
        if (other.getWorkflowExecution() == null ^ this.getWorkflowExecution() == null) return false;
        if (other.getWorkflowExecution() != null && other.getWorkflowExecution().equals(this.getWorkflowExecution()) == false) return false; 
        if (other.getWorkflowType() == null ^ this.getWorkflowType() == null) return false;
        if (other.getWorkflowType() != null && other.getWorkflowType().equals(this.getWorkflowType()) == false) return false; 
        if (other.getInitiatedEventId() == null ^ this.getInitiatedEventId() == null) return false;
        if (other.getInitiatedEventId() != null && other.getInitiatedEventId().equals(this.getInitiatedEventId()) == false) return false; 
        if (other.getStartedEventId() == null ^ this.getStartedEventId() == null) return false;
        if (other.getStartedEventId() != null && other.getStartedEventId().equals(this.getStartedEventId()) == false) return false; 
        return true;
    }
}
    
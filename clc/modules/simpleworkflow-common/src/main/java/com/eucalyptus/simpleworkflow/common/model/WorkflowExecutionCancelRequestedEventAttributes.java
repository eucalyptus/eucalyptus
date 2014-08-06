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

import java.io.Serializable;

/**
 * <p>
 * Provides details of the <code>WorkflowExecutionCancelRequested</code>
 * event.
 * </p>
 */
public class WorkflowExecutionCancelRequestedEventAttributes implements WorkflowEventAttributes {

    /**
     * The external workflow execution for which the cancellation was
     * requested.
     */
    private WorkflowExecution externalWorkflowExecution;

    /**
     * The id of the
     * <code>RequestCancelExternalWorkflowExecutionInitiated</code> event
     * corresponding to the
     * <code>RequestCancelExternalWorkflowExecution</code> decision to cancel
     * this workflow execution.The source event with this Id can be found in
     * the history of the source workflow execution. This information can be
     * useful for diagnosing problems by tracing back the chain of events
     * leading up to this event.
     */
    private Long externalInitiatedEventId;

    /**
     * If set, indicates that the request to cancel the workflow execution
     * was automatically generated, and specifies the cause. This happens if
     * the parent workflow execution times out or is terminated, and the
     * child policy is set to cancel child executions.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>CHILD_POLICY_APPLIED
     */
    private String cause;

    /**
     * The external workflow execution for which the cancellation was
     * requested.
     *
     * @return The external workflow execution for which the cancellation was
     *         requested.
     */
    public WorkflowExecution getExternalWorkflowExecution() {
        return externalWorkflowExecution;
    }
    
    /**
     * The external workflow execution for which the cancellation was
     * requested.
     *
     * @param externalWorkflowExecution The external workflow execution for which the cancellation was
     *         requested.
     */
    public void setExternalWorkflowExecution(WorkflowExecution externalWorkflowExecution) {
        this.externalWorkflowExecution = externalWorkflowExecution;
    }
    
    /**
     * The external workflow execution for which the cancellation was
     * requested.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param externalWorkflowExecution The external workflow execution for which the cancellation was
     *         requested.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowExecutionCancelRequestedEventAttributes withExternalWorkflowExecution(WorkflowExecution externalWorkflowExecution) {
        this.externalWorkflowExecution = externalWorkflowExecution;
        return this;
    }

    /**
     * The id of the
     * <code>RequestCancelExternalWorkflowExecutionInitiated</code> event
     * corresponding to the
     * <code>RequestCancelExternalWorkflowExecution</code> decision to cancel
     * this workflow execution.The source event with this Id can be found in
     * the history of the source workflow execution. This information can be
     * useful for diagnosing problems by tracing back the chain of events
     * leading up to this event.
     *
     * @return The id of the
     *         <code>RequestCancelExternalWorkflowExecutionInitiated</code> event
     *         corresponding to the
     *         <code>RequestCancelExternalWorkflowExecution</code> decision to cancel
     *         this workflow execution.The source event with this Id can be found in
     *         the history of the source workflow execution. This information can be
     *         useful for diagnosing problems by tracing back the chain of events
     *         leading up to this event.
     */
    public Long getExternalInitiatedEventId() {
        return externalInitiatedEventId;
    }
    
    /**
     * The id of the
     * <code>RequestCancelExternalWorkflowExecutionInitiated</code> event
     * corresponding to the
     * <code>RequestCancelExternalWorkflowExecution</code> decision to cancel
     * this workflow execution.The source event with this Id can be found in
     * the history of the source workflow execution. This information can be
     * useful for diagnosing problems by tracing back the chain of events
     * leading up to this event.
     *
     * @param externalInitiatedEventId The id of the
     *         <code>RequestCancelExternalWorkflowExecutionInitiated</code> event
     *         corresponding to the
     *         <code>RequestCancelExternalWorkflowExecution</code> decision to cancel
     *         this workflow execution.The source event with this Id can be found in
     *         the history of the source workflow execution. This information can be
     *         useful for diagnosing problems by tracing back the chain of events
     *         leading up to this event.
     */
    public void setExternalInitiatedEventId(Long externalInitiatedEventId) {
        this.externalInitiatedEventId = externalInitiatedEventId;
    }
    
    /**
     * The id of the
     * <code>RequestCancelExternalWorkflowExecutionInitiated</code> event
     * corresponding to the
     * <code>RequestCancelExternalWorkflowExecution</code> decision to cancel
     * this workflow execution.The source event with this Id can be found in
     * the history of the source workflow execution. This information can be
     * useful for diagnosing problems by tracing back the chain of events
     * leading up to this event.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param externalInitiatedEventId The id of the
     *         <code>RequestCancelExternalWorkflowExecutionInitiated</code> event
     *         corresponding to the
     *         <code>RequestCancelExternalWorkflowExecution</code> decision to cancel
     *         this workflow execution.The source event with this Id can be found in
     *         the history of the source workflow execution. This information can be
     *         useful for diagnosing problems by tracing back the chain of events
     *         leading up to this event.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowExecutionCancelRequestedEventAttributes withExternalInitiatedEventId(Long externalInitiatedEventId) {
        this.externalInitiatedEventId = externalInitiatedEventId;
        return this;
    }

    /**
     * If set, indicates that the request to cancel the workflow execution
     * was automatically generated, and specifies the cause. This happens if
     * the parent workflow execution times out or is terminated, and the
     * child policy is set to cancel child executions.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>CHILD_POLICY_APPLIED
     *
     * @return If set, indicates that the request to cancel the workflow execution
     *         was automatically generated, and specifies the cause. This happens if
     *         the parent workflow execution times out or is terminated, and the
     *         child policy is set to cancel child executions.
     *
     * @see WorkflowExecutionCancelRequestedCause
     */
    public String getCause() {
        return cause;
    }
    
    /**
     * If set, indicates that the request to cancel the workflow execution
     * was automatically generated, and specifies the cause. This happens if
     * the parent workflow execution times out or is terminated, and the
     * child policy is set to cancel child executions.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>CHILD_POLICY_APPLIED
     *
     * @param cause If set, indicates that the request to cancel the workflow execution
     *         was automatically generated, and specifies the cause. This happens if
     *         the parent workflow execution times out or is terminated, and the
     *         child policy is set to cancel child executions.
     *
     * @see WorkflowExecutionCancelRequestedCause
     */
    public void setCause(String cause) {
        this.cause = cause;
    }
    
    /**
     * If set, indicates that the request to cancel the workflow execution
     * was automatically generated, and specifies the cause. This happens if
     * the parent workflow execution times out or is terminated, and the
     * child policy is set to cancel child executions.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>CHILD_POLICY_APPLIED
     *
     * @param cause If set, indicates that the request to cancel the workflow execution
     *         was automatically generated, and specifies the cause. This happens if
     *         the parent workflow execution times out or is terminated, and the
     *         child policy is set to cancel child executions.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     *
     * @see WorkflowExecutionCancelRequestedCause
     */
    public WorkflowExecutionCancelRequestedEventAttributes withCause(String cause) {
        this.cause = cause;
        return this;
    }

    /**
     * If set, indicates that the request to cancel the workflow execution
     * was automatically generated, and specifies the cause. This happens if
     * the parent workflow execution times out or is terminated, and the
     * child policy is set to cancel child executions.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>CHILD_POLICY_APPLIED
     *
     * @param cause If set, indicates that the request to cancel the workflow execution
     *         was automatically generated, and specifies the cause. This happens if
     *         the parent workflow execution times out or is terminated, and the
     *         child policy is set to cancel child executions.
     *
     * @see WorkflowExecutionCancelRequestedCause
     */
    public void setCause(WorkflowExecutionCancelRequestedCause cause) {
        this.cause = cause.toString();
    }
    
    /**
     * If set, indicates that the request to cancel the workflow execution
     * was automatically generated, and specifies the cause. This happens if
     * the parent workflow execution times out or is terminated, and the
     * child policy is set to cancel child executions.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>CHILD_POLICY_APPLIED
     *
     * @param cause If set, indicates that the request to cancel the workflow execution
     *         was automatically generated, and specifies the cause. This happens if
     *         the parent workflow execution times out or is terminated, and the
     *         child policy is set to cancel child executions.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     *
     * @see WorkflowExecutionCancelRequestedCause
     */
    public WorkflowExecutionCancelRequestedEventAttributes withCause(WorkflowExecutionCancelRequestedCause cause) {
        this.cause = cause.toString();
        return this;
    }

    @Override
    public void attach( final HistoryEvent historyEvent ) {
        historyEvent.setWorkflowExecutionCancelRequestedEventAttributes( this );
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
        if (getExternalWorkflowExecution() != null) sb.append("ExternalWorkflowExecution: " + getExternalWorkflowExecution() + ",");
        if (getExternalInitiatedEventId() != null) sb.append("ExternalInitiatedEventId: " + getExternalInitiatedEventId() + ",");
        if (getCause() != null) sb.append("Cause: " + getCause() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getExternalWorkflowExecution() == null) ? 0 : getExternalWorkflowExecution().hashCode()); 
        hashCode = prime * hashCode + ((getExternalInitiatedEventId() == null) ? 0 : getExternalInitiatedEventId().hashCode()); 
        hashCode = prime * hashCode + ((getCause() == null) ? 0 : getCause().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof WorkflowExecutionCancelRequestedEventAttributes == false) return false;
        WorkflowExecutionCancelRequestedEventAttributes other = (WorkflowExecutionCancelRequestedEventAttributes)obj;
        
        if (other.getExternalWorkflowExecution() == null ^ this.getExternalWorkflowExecution() == null) return false;
        if (other.getExternalWorkflowExecution() != null && other.getExternalWorkflowExecution().equals(this.getExternalWorkflowExecution()) == false) return false; 
        if (other.getExternalInitiatedEventId() == null ^ this.getExternalInitiatedEventId() == null) return false;
        if (other.getExternalInitiatedEventId() != null && other.getExternalInitiatedEventId().equals(this.getExternalInitiatedEventId()) == false) return false; 
        if (other.getCause() == null ^ this.getCause() == null) return false;
        if (other.getCause() != null && other.getCause().equals(this.getCause()) == false) return false; 
        return true;
    }
    
}
    
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
 * Provides details of the
 * <code>ExternalWorkflowExecutionSignaled</code> event.
 * </p>
 */
public class ExternalWorkflowExecutionSignaledEventAttributes implements Serializable {

    /**
     * The external workflow execution that the signal was delivered to.
     */
    private WorkflowExecution workflowExecution;

    /**
     * The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the
     * <code>SignalExternalWorkflowExecution</code> decision to request this
     * signal. This information can be useful for diagnosing problems by
     * tracing back the chain of events leading up to this event.
     */
    private Long initiatedEventId;

    /**
     * The external workflow execution that the signal was delivered to.
     *
     * @return The external workflow execution that the signal was delivered to.
     */
    public WorkflowExecution getWorkflowExecution() {
        return workflowExecution;
    }
    
    /**
     * The external workflow execution that the signal was delivered to.
     *
     * @param workflowExecution The external workflow execution that the signal was delivered to.
     */
    public void setWorkflowExecution(WorkflowExecution workflowExecution) {
        this.workflowExecution = workflowExecution;
    }
    
    /**
     * The external workflow execution that the signal was delivered to.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param workflowExecution The external workflow execution that the signal was delivered to.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public ExternalWorkflowExecutionSignaledEventAttributes withWorkflowExecution(WorkflowExecution workflowExecution) {
        this.workflowExecution = workflowExecution;
        return this;
    }

    /**
     * The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the
     * <code>SignalExternalWorkflowExecution</code> decision to request this
     * signal. This information can be useful for diagnosing problems by
     * tracing back the chain of events leading up to this event.
     *
     * @return The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     *         event corresponding to the
     *         <code>SignalExternalWorkflowExecution</code> decision to request this
     *         signal. This information can be useful for diagnosing problems by
     *         tracing back the chain of events leading up to this event.
     */
    public Long getInitiatedEventId() {
        return initiatedEventId;
    }
    
    /**
     * The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the
     * <code>SignalExternalWorkflowExecution</code> decision to request this
     * signal. This information can be useful for diagnosing problems by
     * tracing back the chain of events leading up to this event.
     *
     * @param initiatedEventId The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     *         event corresponding to the
     *         <code>SignalExternalWorkflowExecution</code> decision to request this
     *         signal. This information can be useful for diagnosing problems by
     *         tracing back the chain of events leading up to this event.
     */
    public void setInitiatedEventId(Long initiatedEventId) {
        this.initiatedEventId = initiatedEventId;
    }
    
    /**
     * The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the
     * <code>SignalExternalWorkflowExecution</code> decision to request this
     * signal. This information can be useful for diagnosing problems by
     * tracing back the chain of events leading up to this event.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param initiatedEventId The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     *         event corresponding to the
     *         <code>SignalExternalWorkflowExecution</code> decision to request this
     *         signal. This information can be useful for diagnosing problems by
     *         tracing back the chain of events leading up to this event.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public ExternalWorkflowExecutionSignaledEventAttributes withInitiatedEventId(Long initiatedEventId) {
        this.initiatedEventId = initiatedEventId;
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
        if (getInitiatedEventId() != null) sb.append("InitiatedEventId: " + getInitiatedEventId() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getWorkflowExecution() == null) ? 0 : getWorkflowExecution().hashCode()); 
        hashCode = prime * hashCode + ((getInitiatedEventId() == null) ? 0 : getInitiatedEventId().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof ExternalWorkflowExecutionSignaledEventAttributes == false) return false;
        ExternalWorkflowExecutionSignaledEventAttributes other = (ExternalWorkflowExecutionSignaledEventAttributes)obj;
        
        if (other.getWorkflowExecution() == null ^ this.getWorkflowExecution() == null) return false;
        if (other.getWorkflowExecution() != null && other.getWorkflowExecution().equals(this.getWorkflowExecution()) == false) return false; 
        if (other.getInitiatedEventId() == null ^ this.getInitiatedEventId() == null) return false;
        if (other.getInitiatedEventId() != null && other.getInitiatedEventId().equals(this.getInitiatedEventId()) == false) return false; 
        return true;
    }
    
}
    
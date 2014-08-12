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
 * Provides details of the <code>WorkflowExecutionSignaled</code> event.
 * </p>
 */
public class WorkflowExecutionSignaledEventAttributes implements WorkflowEventAttributes {

    /**
     * The name of the signal received. The decider can use the signal name
     * and inputs to determine how to the process the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    private String signalName;

    /**
     * Inputs provided with the signal (if any). The decider can use the
     * signal name and inputs to determine how to process the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     */
    private String input;

    /**
     * The workflow execution that sent the signal. This is set only of the
     * signal was sent by another workflow execution.
     */
    private WorkflowExecution externalWorkflowExecution;

    /**
     * The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the <code>SignalExternalWorkflow</code>
     * decision to signal this workflow execution.The source event with this
     * Id can be found in the history of the source workflow execution. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event. This field is set only if
     * the signal was initiated by another workflow execution.
     */
    private Long externalInitiatedEventId;

    /**
     * The name of the signal received. The decider can use the signal name
     * and inputs to determine how to the process the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return The name of the signal received. The decider can use the signal name
     *         and inputs to determine how to the process the signal.
     */
    public String getSignalName() {
        return signalName;
    }
    
    /**
     * The name of the signal received. The decider can use the signal name
     * and inputs to determine how to the process the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param signalName The name of the signal received. The decider can use the signal name
     *         and inputs to determine how to the process the signal.
     */
    public void setSignalName(String signalName) {
        this.signalName = signalName;
    }
    
    /**
     * The name of the signal received. The decider can use the signal name
     * and inputs to determine how to the process the signal.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param signalName The name of the signal received. The decider can use the signal name
     *         and inputs to determine how to the process the signal.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowExecutionSignaledEventAttributes withSignalName(String signalName) {
        this.signalName = signalName;
        return this;
    }

    /**
     * Inputs provided with the signal (if any). The decider can use the
     * signal name and inputs to determine how to process the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @return Inputs provided with the signal (if any). The decider can use the
     *         signal name and inputs to determine how to process the signal.
     */
    public String getInput() {
        return input;
    }
    
    /**
     * Inputs provided with the signal (if any). The decider can use the
     * signal name and inputs to determine how to process the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @param input Inputs provided with the signal (if any). The decider can use the
     *         signal name and inputs to determine how to process the signal.
     */
    public void setInput(String input) {
        this.input = input;
    }
    
    /**
     * Inputs provided with the signal (if any). The decider can use the
     * signal name and inputs to determine how to process the signal.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @param input Inputs provided with the signal (if any). The decider can use the
     *         signal name and inputs to determine how to process the signal.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowExecutionSignaledEventAttributes withInput(String input) {
        this.input = input;
        return this;
    }

    /**
     * The workflow execution that sent the signal. This is set only of the
     * signal was sent by another workflow execution.
     *
     * @return The workflow execution that sent the signal. This is set only of the
     *         signal was sent by another workflow execution.
     */
    public WorkflowExecution getExternalWorkflowExecution() {
        return externalWorkflowExecution;
    }
    
    /**
     * The workflow execution that sent the signal. This is set only of the
     * signal was sent by another workflow execution.
     *
     * @param externalWorkflowExecution The workflow execution that sent the signal. This is set only of the
     *         signal was sent by another workflow execution.
     */
    public void setExternalWorkflowExecution(WorkflowExecution externalWorkflowExecution) {
        this.externalWorkflowExecution = externalWorkflowExecution;
    }
    
    /**
     * The workflow execution that sent the signal. This is set only of the
     * signal was sent by another workflow execution.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param externalWorkflowExecution The workflow execution that sent the signal. This is set only of the
     *         signal was sent by another workflow execution.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowExecutionSignaledEventAttributes withExternalWorkflowExecution(WorkflowExecution externalWorkflowExecution) {
        this.externalWorkflowExecution = externalWorkflowExecution;
        return this;
    }

    /**
     * The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the <code>SignalExternalWorkflow</code>
     * decision to signal this workflow execution.The source event with this
     * Id can be found in the history of the source workflow execution. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event. This field is set only if
     * the signal was initiated by another workflow execution.
     *
     * @return The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     *         event corresponding to the <code>SignalExternalWorkflow</code>
     *         decision to signal this workflow execution.The source event with this
     *         Id can be found in the history of the source workflow execution. This
     *         information can be useful for diagnosing problems by tracing back the
     *         chain of events leading up to this event. This field is set only if
     *         the signal was initiated by another workflow execution.
     */
    public Long getExternalInitiatedEventId() {
        return externalInitiatedEventId;
    }
    
    /**
     * The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the <code>SignalExternalWorkflow</code>
     * decision to signal this workflow execution.The source event with this
     * Id can be found in the history of the source workflow execution. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event. This field is set only if
     * the signal was initiated by another workflow execution.
     *
     * @param externalInitiatedEventId The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     *         event corresponding to the <code>SignalExternalWorkflow</code>
     *         decision to signal this workflow execution.The source event with this
     *         Id can be found in the history of the source workflow execution. This
     *         information can be useful for diagnosing problems by tracing back the
     *         chain of events leading up to this event. This field is set only if
     *         the signal was initiated by another workflow execution.
     */
    public void setExternalInitiatedEventId(Long externalInitiatedEventId) {
        this.externalInitiatedEventId = externalInitiatedEventId;
    }
    
    /**
     * The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the <code>SignalExternalWorkflow</code>
     * decision to signal this workflow execution.The source event with this
     * Id can be found in the history of the source workflow execution. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event. This field is set only if
     * the signal was initiated by another workflow execution.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param externalInitiatedEventId The id of the <code>SignalExternalWorkflowExecutionInitiated</code>
     *         event corresponding to the <code>SignalExternalWorkflow</code>
     *         decision to signal this workflow execution.The source event with this
     *         Id can be found in the history of the source workflow execution. This
     *         information can be useful for diagnosing problems by tracing back the
     *         chain of events leading up to this event. This field is set only if
     *         the signal was initiated by another workflow execution.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowExecutionSignaledEventAttributes withExternalInitiatedEventId(Long externalInitiatedEventId) {
        this.externalInitiatedEventId = externalInitiatedEventId;
        return this;
    }

    @Override
    public void attach( final HistoryEvent historyEvent ) {
        historyEvent.setWorkflowExecutionSignaledEventAttributes( this );
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
        if (getSignalName() != null) sb.append("SignalName: " + getSignalName() + ",");
        if (getInput() != null) sb.append("Input: " + getInput() + ",");
        if (getExternalWorkflowExecution() != null) sb.append("ExternalWorkflowExecution: " + getExternalWorkflowExecution() + ",");
        if (getExternalInitiatedEventId() != null) sb.append("ExternalInitiatedEventId: " + getExternalInitiatedEventId() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getSignalName() == null) ? 0 : getSignalName().hashCode()); 
        hashCode = prime * hashCode + ((getInput() == null) ? 0 : getInput().hashCode()); 
        hashCode = prime * hashCode + ((getExternalWorkflowExecution() == null) ? 0 : getExternalWorkflowExecution().hashCode()); 
        hashCode = prime * hashCode + ((getExternalInitiatedEventId() == null) ? 0 : getExternalInitiatedEventId().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof WorkflowExecutionSignaledEventAttributes == false) return false;
        WorkflowExecutionSignaledEventAttributes other = (WorkflowExecutionSignaledEventAttributes)obj;
        
        if (other.getSignalName() == null ^ this.getSignalName() == null) return false;
        if (other.getSignalName() != null && other.getSignalName().equals(this.getSignalName()) == false) return false; 
        if (other.getInput() == null ^ this.getInput() == null) return false;
        if (other.getInput() != null && other.getInput().equals(this.getInput()) == false) return false; 
        if (other.getExternalWorkflowExecution() == null ^ this.getExternalWorkflowExecution() == null) return false;
        if (other.getExternalWorkflowExecution() != null && other.getExternalWorkflowExecution().equals(this.getExternalWorkflowExecution()) == false) return false; 
        if (other.getExternalInitiatedEventId() == null ^ this.getExternalInitiatedEventId() == null) return false;
        if (other.getExternalInitiatedEventId() != null && other.getExternalInitiatedEventId().equals(this.getExternalInitiatedEventId()) == false) return false; 
        return true;
    }
    
}
    
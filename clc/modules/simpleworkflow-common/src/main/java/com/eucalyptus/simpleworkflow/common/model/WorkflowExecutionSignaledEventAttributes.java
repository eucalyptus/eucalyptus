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
 * Provides details of the <code>WorkflowExecutionSignaled</code> event.
 * </p>
 */
public class WorkflowExecutionSignaledEventAttributes implements Serializable, WorkflowEventAttributes {

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
     * The ID of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the <code>SignalExternalWorkflow</code>
     * decision to signal this workflow execution.The source event with this
     * ID can be found in the history of the source workflow execution. This
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
     * The ID of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the <code>SignalExternalWorkflow</code>
     * decision to signal this workflow execution.The source event with this
     * ID can be found in the history of the source workflow execution. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event. This field is set only if
     * the signal was initiated by another workflow execution.
     *
     * @return The ID of the <code>SignalExternalWorkflowExecutionInitiated</code>
     *         event corresponding to the <code>SignalExternalWorkflow</code>
     *         decision to signal this workflow execution.The source event with this
     *         ID can be found in the history of the source workflow execution. This
     *         information can be useful for diagnosing problems by tracing back the
     *         chain of events leading up to this event. This field is set only if
     *         the signal was initiated by another workflow execution.
     */
    public Long getExternalInitiatedEventId() {
        return externalInitiatedEventId;
    }
    
    /**
     * The ID of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the <code>SignalExternalWorkflow</code>
     * decision to signal this workflow execution.The source event with this
     * ID can be found in the history of the source workflow execution. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event. This field is set only if
     * the signal was initiated by another workflow execution.
     *
     * @param externalInitiatedEventId The ID of the <code>SignalExternalWorkflowExecutionInitiated</code>
     *         event corresponding to the <code>SignalExternalWorkflow</code>
     *         decision to signal this workflow execution.The source event with this
     *         ID can be found in the history of the source workflow execution. This
     *         information can be useful for diagnosing problems by tracing back the
     *         chain of events leading up to this event. This field is set only if
     *         the signal was initiated by another workflow execution.
     */
    public void setExternalInitiatedEventId(Long externalInitiatedEventId) {
        this.externalInitiatedEventId = externalInitiatedEventId;
    }
    
    /**
     * The ID of the <code>SignalExternalWorkflowExecutionInitiated</code>
     * event corresponding to the <code>SignalExternalWorkflow</code>
     * decision to signal this workflow execution.The source event with this
     * ID can be found in the history of the source workflow execution. This
     * information can be useful for diagnosing problems by tracing back the
     * chain of events leading up to this event. This field is set only if
     * the signal was initiated by another workflow execution.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param externalInitiatedEventId The ID of the <code>SignalExternalWorkflowExecutionInitiated</code>
     *         event corresponding to the <code>SignalExternalWorkflow</code>
     *         decision to signal this workflow execution.The source event with this
     *         ID can be found in the history of the source workflow execution. This
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

    @Override
    public void attach(HistoryEvent historyEvent) {
      historyEvent.setWorkflowExecutionSignaledEventAttributes(this);
    }
}
    
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

import static com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage.FieldRegex;
import static com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage.FieldRegexValue;
import java.io.Serializable;
import javax.annotation.Nonnull;

/**
 * <p>
 * Provides details of the <code>SignalExternalWorkflowExecution</code>
 * decision.
 * </p>
 * <p>
 * <b>Access Control</b>
 * </p>
 * <p>
 * You can use IAM policies to control this decision's access to Amazon
 * SWF resources as follows:
 * </p>
 * 
 * <ul>
 * <li>Use a <code>Resource</code> element with the domain name to limit
 * the action to only specified domains.</li>
 * <li>Use an <code>Action</code> element to allow or deny permission to
 * call this action.</li>
 * <li>You cannot use an IAM policy to constrain this action's
 * parameters.</li>
 * 
 * </ul>
 * <p>
 * If the caller does not have sufficient permissions to invoke the
 * action, or the parameter values fall outside the specified
 * constraints, the action fails. The associated event attribute's
 * <b>cause</b> parameter will be set to OPERATION_NOT_PERMITTED. For
 * details and example IAM policies, see
 * <a href="http://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dev-iam.html"> Using IAM to Manage Access to Amazon SWF Workflows </a>
 * .
 * </p>
 */
public class SignalExternalWorkflowExecutionDecisionAttributes implements Serializable {

    /**
     * <b>Required.</b> The <code>workflowId</code> of the workflow execution
     * to be signaled.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String workflowId;

    /**
     * The <code>runId</code> of the workflow execution to be signaled.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 64<br/>
     */
    @FieldRegex( FieldRegexValue.NAME_64 )
    private String runId;

    /**
     * <b>Required.</b> The name of the signal.The target workflow execution
     * will use the signal name and input to process the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.STRING_256 )
    private String signalName;

    /**
     * <i>Optional.</i> Input data to be provided with the signal. The target
     * workflow execution will use the signal name and input data to process
     * the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     */
    @FieldRegex( FieldRegexValue.OPT_STRING_32768 )
    private String input;

    /**
     * <i>Optional.</i> Data attached to the event that can be used by the
     * decider in subsequent decision tasks.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     */
    @FieldRegex( FieldRegexValue.OPT_STRING_32768 )
    private String control;

    /**
     * <b>Required.</b> The <code>workflowId</code> of the workflow execution
     * to be signaled.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return <b>Required.</b> The <code>workflowId</code> of the workflow execution
     *         to be signaled.
     */
    public String getWorkflowId() {
        return workflowId;
    }
    
    /**
     * <b>Required.</b> The <code>workflowId</code> of the workflow execution
     * to be signaled.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param workflowId <b>Required.</b> The <code>workflowId</code> of the workflow execution
     *         to be signaled.
     */
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
    
    /**
     * <b>Required.</b> The <code>workflowId</code> of the workflow execution
     * to be signaled.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param workflowId <b>Required.</b> The <code>workflowId</code> of the workflow execution
     *         to be signaled.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public SignalExternalWorkflowExecutionDecisionAttributes withWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    /**
     * The <code>runId</code> of the workflow execution to be signaled.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 64<br/>
     *
     * @return The <code>runId</code> of the workflow execution to be signaled.
     */
    public String getRunId() {
        return runId;
    }
    
    /**
     * The <code>runId</code> of the workflow execution to be signaled.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 64<br/>
     *
     * @param runId The <code>runId</code> of the workflow execution to be signaled.
     */
    public void setRunId(String runId) {
        this.runId = runId;
    }
    
    /**
     * The <code>runId</code> of the workflow execution to be signaled.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 64<br/>
     *
     * @param runId The <code>runId</code> of the workflow execution to be signaled.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public SignalExternalWorkflowExecutionDecisionAttributes withRunId(String runId) {
        this.runId = runId;
        return this;
    }

    /**
     * <b>Required.</b> The name of the signal.The target workflow execution
     * will use the signal name and input to process the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return <b>Required.</b> The name of the signal.The target workflow execution
     *         will use the signal name and input to process the signal.
     */
    public String getSignalName() {
        return signalName;
    }
    
    /**
     * <b>Required.</b> The name of the signal.The target workflow execution
     * will use the signal name and input to process the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param signalName <b>Required.</b> The name of the signal.The target workflow execution
     *         will use the signal name and input to process the signal.
     */
    public void setSignalName(String signalName) {
        this.signalName = signalName;
    }
    
    /**
     * <b>Required.</b> The name of the signal.The target workflow execution
     * will use the signal name and input to process the signal.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param signalName <b>Required.</b> The name of the signal.The target workflow execution
     *         will use the signal name and input to process the signal.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public SignalExternalWorkflowExecutionDecisionAttributes withSignalName(String signalName) {
        this.signalName = signalName;
        return this;
    }

    /**
     * <i>Optional.</i> Input data to be provided with the signal. The target
     * workflow execution will use the signal name and input data to process
     * the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @return <i>Optional.</i> Input data to be provided with the signal. The target
     *         workflow execution will use the signal name and input data to process
     *         the signal.
     */
    public String getInput() {
        return input;
    }
    
    /**
     * <i>Optional.</i> Input data to be provided with the signal. The target
     * workflow execution will use the signal name and input data to process
     * the signal.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @param input <i>Optional.</i> Input data to be provided with the signal. The target
     *         workflow execution will use the signal name and input data to process
     *         the signal.
     */
    public void setInput(String input) {
        this.input = input;
    }
    
    /**
     * <i>Optional.</i> Input data to be provided with the signal. The target
     * workflow execution will use the signal name and input data to process
     * the signal.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @param input <i>Optional.</i> Input data to be provided with the signal. The target
     *         workflow execution will use the signal name and input data to process
     *         the signal.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public SignalExternalWorkflowExecutionDecisionAttributes withInput(String input) {
        this.input = input;
        return this;
    }

    /**
     * <i>Optional.</i> Data attached to the event that can be used by the
     * decider in subsequent decision tasks.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @return <i>Optional.</i> Data attached to the event that can be used by the
     *         decider in subsequent decision tasks.
     */
    public String getControl() {
        return control;
    }
    
    /**
     * <i>Optional.</i> Data attached to the event that can be used by the
     * decider in subsequent decision tasks.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @param control <i>Optional.</i> Data attached to the event that can be used by the
     *         decider in subsequent decision tasks.
     */
    public void setControl(String control) {
        this.control = control;
    }
    
    /**
     * <i>Optional.</i> Data attached to the event that can be used by the
     * decider in subsequent decision tasks.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 32768<br/>
     *
     * @param control <i>Optional.</i> Data attached to the event that can be used by the
     *         decider in subsequent decision tasks.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public SignalExternalWorkflowExecutionDecisionAttributes withControl(String control) {
        this.control = control;
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
        if (getWorkflowId() != null) sb.append("WorkflowId: " + getWorkflowId() + ",");
        if (getRunId() != null) sb.append("RunId: " + getRunId() + ",");
        if (getSignalName() != null) sb.append("SignalName: " + getSignalName() + ",");
        if (getInput() != null) sb.append("Input: " + getInput() + ",");
        if (getControl() != null) sb.append("Control: " + getControl() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getWorkflowId() == null) ? 0 : getWorkflowId().hashCode()); 
        hashCode = prime * hashCode + ((getRunId() == null) ? 0 : getRunId().hashCode()); 
        hashCode = prime * hashCode + ((getSignalName() == null) ? 0 : getSignalName().hashCode()); 
        hashCode = prime * hashCode + ((getInput() == null) ? 0 : getInput().hashCode()); 
        hashCode = prime * hashCode + ((getControl() == null) ? 0 : getControl().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof SignalExternalWorkflowExecutionDecisionAttributes == false) return false;
        SignalExternalWorkflowExecutionDecisionAttributes other = (SignalExternalWorkflowExecutionDecisionAttributes)obj;
        
        if (other.getWorkflowId() == null ^ this.getWorkflowId() == null) return false;
        if (other.getWorkflowId() != null && other.getWorkflowId().equals(this.getWorkflowId()) == false) return false; 
        if (other.getRunId() == null ^ this.getRunId() == null) return false;
        if (other.getRunId() != null && other.getRunId().equals(this.getRunId()) == false) return false; 
        if (other.getSignalName() == null ^ this.getSignalName() == null) return false;
        if (other.getSignalName() != null && other.getSignalName().equals(this.getSignalName()) == false) return false; 
        if (other.getInput() == null ^ this.getInput() == null) return false;
        if (other.getInput() != null && other.getInput().equals(this.getInput()) == false) return false; 
        if (other.getControl() == null ^ this.getControl() == null) return false;
        if (other.getControl() != null && other.getControl().equals(this.getControl()) == false) return false; 
        return true;
    }
    
}
    
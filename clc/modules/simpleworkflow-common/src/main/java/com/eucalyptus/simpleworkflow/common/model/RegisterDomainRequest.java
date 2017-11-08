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
import javax.annotation.Nonnull;
import com.eucalyptus.auth.policy.annotation.PolicyAction;

/**
 * Container for the parameters to the {@link com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#registerDomain(RegisterDomainRequest) RegisterDomain operation}.
 * <p>
 * Registers a new domain.
 * </p>
 * <p>
 * <b>Access Control</b>
 * </p>
 * <p>
 * You can use IAM policies to control this action's access to Amazon SWF
 * resources as follows:
 * </p>
 * 
 * <ul>
 * <li>You cannot use an IAM policy to control domain access for this
 * action. The name of the domain being registered is available as the
 * resource of this action.</li>
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
 *
 * @see com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#registerDomain(RegisterDomainRequest)
 */
@PolicyAction( vendor = "swf", action = "registerdomain" )
public class RegisterDomainRequest extends SimpleWorkflowMessage implements Serializable {

    /**
     * Name of the domain to register. The name must be unique in the region
     * that the domain is registered in. <p>The specified string must not
     * start or end with whitespace. It must not contain a <code>:</code>
     * (colon), <code>/</code> (slash), <code>|</code> (vertical bar), or any
     * control characters (\u0000-\u001f | \u007f - \u009f). Also, it must
     * not contain the literal string "arn".
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String name;

    /**
     * A text description of the domain.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 1024<br/>
     */
    @FieldRegex( FieldRegexValue.OPT_STRING_1024 )
    private String description;

    /**
     * The duration (in days) that records and histories of workflow
     * executions on the domain should be kept by the service. After the
     * retention period, the workflow execution is not available in the
     * results of visibility calls. <p>If you pass the value
     * <code>NONE</code> or <code>0</code> (zero), then the workflow
     * execution history will not be retained. As soon as the workflow
     * execution completes, the execution record and its history are deleted.
     * <p>The maximum workflow execution retention period is 90 days. For
     * more information about Amazon SWF service limits, see: <a
     * href="http://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dg-limits.html">Amazon
     * SWF Service Limits</a> in the <i>Amazon SWF Developer Guide</i>.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 8<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.DURATION_8_NONE )
    private String workflowExecutionRetentionPeriodInDays;

    /**
     * Name of the domain to register. The name must be unique in the region
     * that the domain is registered in. <p>The specified string must not
     * start or end with whitespace. It must not contain a <code>:</code>
     * (colon), <code>/</code> (slash), <code>|</code> (vertical bar), or any
     * control characters (\u0000-\u001f | \u007f - \u009f). Also, it must
     * not contain the literal string "arn".
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return Name of the domain to register. The name must be unique in the region
     *         that the domain is registered in. <p>The specified string must not
     *         start or end with whitespace. It must not contain a <code>:</code>
     *         (colon), <code>/</code> (slash), <code>|</code> (vertical bar), or any
     *         control characters (\u0000-\u001f | \u007f - \u009f). Also, it must
     *         not contain the literal string "arn".
     */
    public String getName() {
        return name;
    }
    
    /**
     * Name of the domain to register. The name must be unique in the region
     * that the domain is registered in. <p>The specified string must not
     * start or end with whitespace. It must not contain a <code>:</code>
     * (colon), <code>/</code> (slash), <code>|</code> (vertical bar), or any
     * control characters (\u0000-\u001f | \u007f - \u009f). Also, it must
     * not contain the literal string "arn".
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param name Name of the domain to register. The name must be unique in the region
     *         that the domain is registered in. <p>The specified string must not
     *         start or end with whitespace. It must not contain a <code>:</code>
     *         (colon), <code>/</code> (slash), <code>|</code> (vertical bar), or any
     *         control characters (\u0000-\u001f | \u007f - \u009f). Also, it must
     *         not contain the literal string "arn".
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Name of the domain to register. The name must be unique in the region
     * that the domain is registered in. <p>The specified string must not
     * start or end with whitespace. It must not contain a <code>:</code>
     * (colon), <code>/</code> (slash), <code>|</code> (vertical bar), or any
     * control characters (\u0000-\u001f | \u007f - \u009f). Also, it must
     * not contain the literal string "arn".
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param name Name of the domain to register. The name must be unique in the region
     *         that the domain is registered in. <p>The specified string must not
     *         start or end with whitespace. It must not contain a <code>:</code>
     *         (colon), <code>/</code> (slash), <code>|</code> (vertical bar), or any
     *         control characters (\u0000-\u001f | \u007f - \u009f). Also, it must
     *         not contain the literal string "arn".
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public RegisterDomainRequest withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * A text description of the domain.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 1024<br/>
     *
     * @return A text description of the domain.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * A text description of the domain.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 1024<br/>
     *
     * @param description A text description of the domain.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * A text description of the domain.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 1024<br/>
     *
     * @param description A text description of the domain.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public RegisterDomainRequest withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * The duration (in days) that records and histories of workflow
     * executions on the domain should be kept by the service. After the
     * retention period, the workflow execution is not available in the
     * results of visibility calls. <p>If you pass the value
     * <code>NONE</code> or <code>0</code> (zero), then the workflow
     * execution history will not be retained. As soon as the workflow
     * execution completes, the execution record and its history are deleted.
     * <p>The maximum workflow execution retention period is 90 days. For
     * more information about Amazon SWF service limits, see: <a
     * href="http://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dg-limits.html">Amazon
     * SWF Service Limits</a> in the <i>Amazon SWF Developer Guide</i>.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 8<br/>
     *
     * @return The duration (in days) that records and histories of workflow
     *         executions on the domain should be kept by the service. After the
     *         retention period, the workflow execution is not available in the
     *         results of visibility calls. <p>If you pass the value
     *         <code>NONE</code> or <code>0</code> (zero), then the workflow
     *         execution history will not be retained. As soon as the workflow
     *         execution completes, the execution record and its history are deleted.
     *         <p>The maximum workflow execution retention period is 90 days. For
     *         more information about Amazon SWF service limits, see: <a
     *         href="http://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dg-limits.html">Amazon
     *         SWF Service Limits</a> in the <i>Amazon SWF Developer Guide</i>.
     */
    public String getWorkflowExecutionRetentionPeriodInDays() {
        return workflowExecutionRetentionPeriodInDays;
    }
    
    /**
     * The duration (in days) that records and histories of workflow
     * executions on the domain should be kept by the service. After the
     * retention period, the workflow execution is not available in the
     * results of visibility calls. <p>If you pass the value
     * <code>NONE</code> or <code>0</code> (zero), then the workflow
     * execution history will not be retained. As soon as the workflow
     * execution completes, the execution record and its history are deleted.
     * <p>The maximum workflow execution retention period is 90 days. For
     * more information about Amazon SWF service limits, see: <a
     * href="http://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dg-limits.html">Amazon
     * SWF Service Limits</a> in the <i>Amazon SWF Developer Guide</i>.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 8<br/>
     *
     * @param workflowExecutionRetentionPeriodInDays The duration (in days) that records and histories of workflow
     *         executions on the domain should be kept by the service. After the
     *         retention period, the workflow execution is not available in the
     *         results of visibility calls. <p>If you pass the value
     *         <code>NONE</code> or <code>0</code> (zero), then the workflow
     *         execution history will not be retained. As soon as the workflow
     *         execution completes, the execution record and its history are deleted.
     *         <p>The maximum workflow execution retention period is 90 days. For
     *         more information about Amazon SWF service limits, see: <a
     *         href="http://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dg-limits.html">Amazon
     *         SWF Service Limits</a> in the <i>Amazon SWF Developer Guide</i>.
     */
    public void setWorkflowExecutionRetentionPeriodInDays(String workflowExecutionRetentionPeriodInDays) {
        this.workflowExecutionRetentionPeriodInDays = workflowExecutionRetentionPeriodInDays;
    }
    
    /**
     * The duration (in days) that records and histories of workflow
     * executions on the domain should be kept by the service. After the
     * retention period, the workflow execution is not available in the
     * results of visibility calls. <p>If you pass the value
     * <code>NONE</code> or <code>0</code> (zero), then the workflow
     * execution history will not be retained. As soon as the workflow
     * execution completes, the execution record and its history are deleted.
     * <p>The maximum workflow execution retention period is 90 days. For
     * more information about Amazon SWF service limits, see: <a
     * href="http://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dg-limits.html">Amazon
     * SWF Service Limits</a> in the <i>Amazon SWF Developer Guide</i>.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 8<br/>
     *
     * @param workflowExecutionRetentionPeriodInDays The duration (in days) that records and histories of workflow
     *         executions on the domain should be kept by the service. After the
     *         retention period, the workflow execution is not available in the
     *         results of visibility calls. <p>If you pass the value
     *         <code>NONE</code> or <code>0</code> (zero), then the workflow
     *         execution history will not be retained. As soon as the workflow
     *         execution completes, the execution record and its history are deleted.
     *         <p>The maximum workflow execution retention period is 90 days. For
     *         more information about Amazon SWF service limits, see: <a
     *         href="http://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dg-limits.html">Amazon
     *         SWF Service Limits</a> in the <i>Amazon SWF Developer Guide</i>.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public RegisterDomainRequest withWorkflowExecutionRetentionPeriodInDays(String workflowExecutionRetentionPeriodInDays) {
        this.workflowExecutionRetentionPeriodInDays = workflowExecutionRetentionPeriodInDays;
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
        if (getName() != null) sb.append("Name: " + getName() + ",");
        if (getDescription() != null) sb.append("Description: " + getDescription() + ",");
        if (getWorkflowExecutionRetentionPeriodInDays() != null) sb.append("WorkflowExecutionRetentionPeriodInDays: " + getWorkflowExecutionRetentionPeriodInDays() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getName() == null) ? 0 : getName().hashCode()); 
        hashCode = prime * hashCode + ((getDescription() == null) ? 0 : getDescription().hashCode()); 
        hashCode = prime * hashCode + ((getWorkflowExecutionRetentionPeriodInDays() == null) ? 0 : getWorkflowExecutionRetentionPeriodInDays().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof RegisterDomainRequest == false) return false;
        RegisterDomainRequest other = (RegisterDomainRequest)obj;
        
        if (other.getName() == null ^ this.getName() == null) return false;
        if (other.getName() != null && other.getName().equals(this.getName()) == false) return false; 
        if (other.getDescription() == null ^ this.getDescription() == null) return false;
        if (other.getDescription() != null && other.getDescription().equals(this.getDescription()) == false) return false; 
        if (other.getWorkflowExecutionRetentionPeriodInDays() == null ^ this.getWorkflowExecutionRetentionPeriodInDays() == null) return false;
        if (other.getWorkflowExecutionRetentionPeriodInDays() != null && other.getWorkflowExecutionRetentionPeriodInDays().equals(this.getWorkflowExecutionRetentionPeriodInDays()) == false) return false; 
        return true;
    }

}
    
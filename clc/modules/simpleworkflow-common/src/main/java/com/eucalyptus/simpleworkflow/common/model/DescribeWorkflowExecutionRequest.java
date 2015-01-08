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
import javax.annotation.Nonnull;
import com.eucalyptus.auth.policy.PolicyAction;


/**
 * Container for the parameters to the {@link com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#describeWorkflowExecution(DescribeWorkflowExecutionRequest) DescribeWorkflowExecution operation}.
 * <p>
 * Returns information about the specified workflow execution including
 * its type and some statistics.
 * </p>
 * <p>
 * <b>NOTE:</b> This operation is eventually consistent. The results are
 * best effort and may not exactly reflect recent updates and changes.
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
 * constraints, the action fails by throwing
 * <code>OperationNotPermitted</code> . For details and example IAM
 * policies, see
 * <a href="http://docs.aws.amazon.com/amazonswf/latest/developerguide/swf-dev-iam.html"> Using IAM to Manage Access to Amazon SWF Workflows </a>
 * .
 * </p>
 *
 * @see com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#describeWorkflowExecution(DescribeWorkflowExecutionRequest)
 */
@PolicyAction( vendor = "swf", action = "describeworkflowexecution" )
public class DescribeWorkflowExecutionRequest extends SimpleWorkflowMessage implements Serializable {

    /**
     * The name of the domain containing the workflow execution.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String domain;

    /**
     * The workflow execution to describe.
     */
    @Nonnull
    private WorkflowExecution execution;

    /**
     * The name of the domain containing the workflow execution.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return The name of the domain containing the workflow execution.
     */
    public String getDomain() {
        return domain;
    }
    
    /**
     * The name of the domain containing the workflow execution.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param domain The name of the domain containing the workflow execution.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    /**
     * The name of the domain containing the workflow execution.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param domain The name of the domain containing the workflow execution.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public DescribeWorkflowExecutionRequest withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * The workflow execution to describe.
     *
     * @return The workflow execution to describe.
     */
    public WorkflowExecution getExecution() {
        return execution;
    }
    
    /**
     * The workflow execution to describe.
     *
     * @param execution The workflow execution to describe.
     */
    public void setExecution(WorkflowExecution execution) {
        this.execution = execution;
    }
    
    /**
     * The workflow execution to describe.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param execution The workflow execution to describe.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public DescribeWorkflowExecutionRequest withExecution(WorkflowExecution execution) {
        this.execution = execution;
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
        if (getDomain() != null) sb.append("Domain: " + getDomain() + ",");
        if (getExecution() != null) sb.append("Execution: " + getExecution() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getDomain() == null) ? 0 : getDomain().hashCode()); 
        hashCode = prime * hashCode + ((getExecution() == null) ? 0 : getExecution().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof DescribeWorkflowExecutionRequest == false) return false;
        DescribeWorkflowExecutionRequest other = (DescribeWorkflowExecutionRequest)obj;
        
        if (other.getDomain() == null ^ this.getDomain() == null) return false;
        if (other.getDomain() != null && other.getDomain().equals(this.getDomain()) == false) return false; 
        if (other.getExecution() == null ^ this.getExecution() == null) return false;
        if (other.getExecution() != null && other.getExecution().equals(this.getExecution()) == false) return false; 
        return true;
    }
    
}
    
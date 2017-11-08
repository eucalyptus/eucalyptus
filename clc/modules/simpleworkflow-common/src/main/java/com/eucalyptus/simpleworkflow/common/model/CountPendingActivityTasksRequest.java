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
 * Container for the parameters to the {@link com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#countPendingActivityTasks(CountPendingActivityTasksRequest) CountPendingActivityTasks operation}.
 * <p>
 * Returns the estimated number of activity tasks in the specified task
 * list. The count returned is an approximation and is not guaranteed to
 * be exact. If you specify a task list that no activity task was ever
 * scheduled in then 0 will be returned.
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
 * <li>Constrain the <code>taskList.name</code> parameter by using a
 * <b>Condition</b> element with the <code>swf:taskList.name</code> key
 * to allow the action to access only certain task lists.</li>
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
 * @see com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#countPendingActivityTasks(CountPendingActivityTasksRequest)
 */
@PolicyAction( vendor = "swf", action = "countpendingactivitytasks" )
public class CountPendingActivityTasksRequest extends SimpleWorkflowMessage implements Serializable {

    /**
     * The name of the domain that contains the task list.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String domain;

    /**
     * The name of the task list.
     */
    @Nonnull
    private TaskList taskList;

    /**
     * The name of the domain that contains the task list.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return The name of the domain that contains the task list.
     */
    public String getDomain() {
        return domain;
    }
    
    /**
     * The name of the domain that contains the task list.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param domain The name of the domain that contains the task list.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    /**
     * The name of the domain that contains the task list.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param domain The name of the domain that contains the task list.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public CountPendingActivityTasksRequest withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * The name of the task list.
     *
     * @return The name of the task list.
     */
    public TaskList getTaskList() {
        return taskList;
    }
    
    /**
     * The name of the task list.
     *
     * @param taskList The name of the task list.
     */
    public void setTaskList(TaskList taskList) {
        this.taskList = taskList;
    }
    
    /**
     * The name of the task list.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param taskList The name of the task list.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public CountPendingActivityTasksRequest withTaskList(TaskList taskList) {
        this.taskList = taskList;
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
        if (getTaskList() != null) sb.append("TaskList: " + getTaskList() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getDomain() == null) ? 0 : getDomain().hashCode()); 
        hashCode = prime * hashCode + ((getTaskList() == null) ? 0 : getTaskList().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof CountPendingActivityTasksRequest == false) return false;
        CountPendingActivityTasksRequest other = (CountPendingActivityTasksRequest)obj;
        
        if (other.getDomain() == null ^ this.getDomain() == null) return false;
        if (other.getDomain() != null && other.getDomain().equals(this.getDomain()) == false) return false; 
        if (other.getTaskList() == null ^ this.getTaskList() == null) return false;
        if (other.getTaskList() != null && other.getTaskList().equals(this.getTaskList()) == false) return false; 
        return true;
    }
}
    
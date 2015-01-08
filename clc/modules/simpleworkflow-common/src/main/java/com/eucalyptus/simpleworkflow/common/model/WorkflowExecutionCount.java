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
 * Contains the count of workflow executions returned from
 * CountOpenWorkflowExecutions or CountClosedWorkflowExecutions
 * </p>
 */
public class WorkflowExecutionCount extends SimpleWorkflowMessage {

    /**
     * The number of workflow executions.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Range: </b>0 - <br/>
     */
    private Integer count;

    /**
     * If set to true, indicates that the actual count was more than the
     * maximum supported by this API and the count returned is the truncated
     * value.
     */
    private Boolean truncated;

    /**
     * The number of workflow executions.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Range: </b>0 - <br/>
     *
     * @return The number of workflow executions.
     */
    public Integer getCount() {
        return count;
    }
    
    /**
     * The number of workflow executions.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Range: </b>0 - <br/>
     *
     * @param count The number of workflow executions.
     */
    public void setCount(Integer count) {
        this.count = count;
    }
    
    /**
     * The number of workflow executions.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Range: </b>0 - <br/>
     *
     * @param count The number of workflow executions.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowExecutionCount withCount(Integer count) {
        this.count = count;
        return this;
    }

    /**
     * If set to true, indicates that the actual count was more than the
     * maximum supported by this API and the count returned is the truncated
     * value.
     *
     * @return If set to true, indicates that the actual count was more than the
     *         maximum supported by this API and the count returned is the truncated
     *         value.
     */
    public Boolean isTruncated() {
        return truncated;
    }
    
    /**
     * If set to true, indicates that the actual count was more than the
     * maximum supported by this API and the count returned is the truncated
     * value.
     *
     * @param truncated If set to true, indicates that the actual count was more than the
     *         maximum supported by this API and the count returned is the truncated
     *         value.
     */
    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }
    
    /**
     * If set to true, indicates that the actual count was more than the
     * maximum supported by this API and the count returned is the truncated
     * value.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param truncated If set to true, indicates that the actual count was more than the
     *         maximum supported by this API and the count returned is the truncated
     *         value.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowExecutionCount withTruncated(Boolean truncated) {
        this.truncated = truncated;
        return this;
    }

    /**
     * If set to true, indicates that the actual count was more than the
     * maximum supported by this API and the count returned is the truncated
     * value.
     *
     * @return If set to true, indicates that the actual count was more than the
     *         maximum supported by this API and the count returned is the truncated
     *         value.
     */
    public Boolean getTruncated() {
        return truncated;
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
        if (getCount() != null) sb.append("Count: " + getCount() + ",");
        if (isTruncated() != null) sb.append("Truncated: " + isTruncated() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getCount() == null) ? 0 : getCount().hashCode()); 
        hashCode = prime * hashCode + ((isTruncated() == null) ? 0 : isTruncated().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof WorkflowExecutionCount == false) return false;
        WorkflowExecutionCount other = (WorkflowExecutionCount)obj;
        
        if (other.getCount() == null ^ this.getCount() == null) return false;
        if (other.getCount() != null && other.getCount().equals(this.getCount()) == false) return false; 
        if (other.isTruncated() == null ^ this.isTruncated() == null) return false;
        if (other.isTruncated() != null && other.isTruncated().equals(this.isTruncated()) == false) return false; 
        return true;
    }
    
}
    
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
 * Contains details about a workflow type.
 * </p>
 */
public class WorkflowTypeDetail extends SimpleWorkflowMessage {

    /**
     * General information about the workflow type. <p> The status of the
     * workflow type (returned in the WorkflowTypeInfo structure) can be one
     * of the following. <ul> <li> <b>REGISTERED</b>: The type is registered
     * and available. Workers supporting this type should be running. </li>
     * <li> <b>DEPRECATED</b>: The type was deprecated using
     * <a>DeprecateWorkflowType</a>, but is still in use. You should keep
     * workers supporting this type running. You cannot create new workflow
     * executions of this type. </li> </ul>
     */
    private WorkflowTypeInfo typeInfo;

    /**
     * Configuration settings of the workflow type registered through
     * <a>RegisterWorkflowType</a>
     */
    private WorkflowTypeConfiguration configuration;

    /**
     * General information about the workflow type. <p> The status of the
     * workflow type (returned in the WorkflowTypeInfo structure) can be one
     * of the following. <ul> <li> <b>REGISTERED</b>: The type is registered
     * and available. Workers supporting this type should be running. </li>
     * <li> <b>DEPRECATED</b>: The type was deprecated using
     * <a>DeprecateWorkflowType</a>, but is still in use. You should keep
     * workers supporting this type running. You cannot create new workflow
     * executions of this type. </li> </ul>
     *
     * @return General information about the workflow type. <p> The status of the
     *         workflow type (returned in the WorkflowTypeInfo structure) can be one
     *         of the following. <ul> <li> <b>REGISTERED</b>: The type is registered
     *         and available. Workers supporting this type should be running. </li>
     *         <li> <b>DEPRECATED</b>: The type was deprecated using
     *         <a>DeprecateWorkflowType</a>, but is still in use. You should keep
     *         workers supporting this type running. You cannot create new workflow
     *         executions of this type. </li> </ul>
     */
    public WorkflowTypeInfo getTypeInfo() {
        return typeInfo;
    }
    
    /**
     * General information about the workflow type. <p> The status of the
     * workflow type (returned in the WorkflowTypeInfo structure) can be one
     * of the following. <ul> <li> <b>REGISTERED</b>: The type is registered
     * and available. Workers supporting this type should be running. </li>
     * <li> <b>DEPRECATED</b>: The type was deprecated using
     * <a>DeprecateWorkflowType</a>, but is still in use. You should keep
     * workers supporting this type running. You cannot create new workflow
     * executions of this type. </li> </ul>
     *
     * @param typeInfo General information about the workflow type. <p> The status of the
     *         workflow type (returned in the WorkflowTypeInfo structure) can be one
     *         of the following. <ul> <li> <b>REGISTERED</b>: The type is registered
     *         and available. Workers supporting this type should be running. </li>
     *         <li> <b>DEPRECATED</b>: The type was deprecated using
     *         <a>DeprecateWorkflowType</a>, but is still in use. You should keep
     *         workers supporting this type running. You cannot create new workflow
     *         executions of this type. </li> </ul>
     */
    public void setTypeInfo(WorkflowTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }
    
    /**
     * General information about the workflow type. <p> The status of the
     * workflow type (returned in the WorkflowTypeInfo structure) can be one
     * of the following. <ul> <li> <b>REGISTERED</b>: The type is registered
     * and available. Workers supporting this type should be running. </li>
     * <li> <b>DEPRECATED</b>: The type was deprecated using
     * <a>DeprecateWorkflowType</a>, but is still in use. You should keep
     * workers supporting this type running. You cannot create new workflow
     * executions of this type. </li> </ul>
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param typeInfo General information about the workflow type. <p> The status of the
     *         workflow type (returned in the WorkflowTypeInfo structure) can be one
     *         of the following. <ul> <li> <b>REGISTERED</b>: The type is registered
     *         and available. Workers supporting this type should be running. </li>
     *         <li> <b>DEPRECATED</b>: The type was deprecated using
     *         <a>DeprecateWorkflowType</a>, but is still in use. You should keep
     *         workers supporting this type running. You cannot create new workflow
     *         executions of this type. </li> </ul>
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowTypeDetail withTypeInfo(WorkflowTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
        return this;
    }

    /**
     * Configuration settings of the workflow type registered through
     * <a>RegisterWorkflowType</a>
     *
     * @return Configuration settings of the workflow type registered through
     *         <a>RegisterWorkflowType</a>
     */
    public WorkflowTypeConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Configuration settings of the workflow type registered through
     * <a>RegisterWorkflowType</a>
     *
     * @param configuration Configuration settings of the workflow type registered through
     *         <a>RegisterWorkflowType</a>
     */
    public void setConfiguration(WorkflowTypeConfiguration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Configuration settings of the workflow type registered through
     * <a>RegisterWorkflowType</a>
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param configuration Configuration settings of the workflow type registered through
     *         <a>RegisterWorkflowType</a>
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public WorkflowTypeDetail withConfiguration(WorkflowTypeConfiguration configuration) {
        this.configuration = configuration;
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
        if (getTypeInfo() != null) sb.append("TypeInfo: " + getTypeInfo() + ",");
        if (getConfiguration() != null) sb.append("Configuration: " + getConfiguration() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getTypeInfo() == null) ? 0 : getTypeInfo().hashCode()); 
        hashCode = prime * hashCode + ((getConfiguration() == null) ? 0 : getConfiguration().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof WorkflowTypeDetail == false) return false;
        WorkflowTypeDetail other = (WorkflowTypeDetail)obj;
        
        if (other.getTypeInfo() == null ^ this.getTypeInfo() == null) return false;
        if (other.getTypeInfo() != null && other.getTypeInfo().equals(this.getTypeInfo()) == false) return false; 
        if (other.getConfiguration() == null ^ this.getConfiguration() == null) return false;
        if (other.getConfiguration() != null && other.getConfiguration().equals(this.getConfiguration()) == false) return false; 
        return true;
    }
    
}
    
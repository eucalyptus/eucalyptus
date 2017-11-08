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
 * Used to filter workflow execution query results by type. Each
 * parameter, if specified, defines a rule that must be satisfied by each
 * returned result.
 * </p>
 */
public class WorkflowTypeFilter implements Serializable {

    /**
     * <b>Required.</b> Name of the workflow type.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String name;

    /**
     * Version of the workflow type.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 64<br/>
     */
    @FieldRegex( FieldRegexValue.NAME_64 )
    private String version;

    /**
     * <b>Required.</b> Name of the workflow type.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return <b>Required.</b> Name of the workflow type.
     */
    public String getName() {
        return name;
    }
    
    /**
     * <b>Required.</b> Name of the workflow type.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param name <b>Required.</b> Name of the workflow type.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * <b>Required.</b> Name of the workflow type.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param name <b>Required.</b> Name of the workflow type.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public WorkflowTypeFilter withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Version of the workflow type.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 64<br/>
     *
     * @return Version of the workflow type.
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Version of the workflow type.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 64<br/>
     *
     * @param version Version of the workflow type.
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * Version of the workflow type.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 64<br/>
     *
     * @param version Version of the workflow type.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public WorkflowTypeFilter withVersion(String version) {
        this.version = version;
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
        if (getVersion() != null) sb.append("Version: " + getVersion() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getName() == null) ? 0 : getName().hashCode()); 
        hashCode = prime * hashCode + ((getVersion() == null) ? 0 : getVersion().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof WorkflowTypeFilter == false) return false;
        WorkflowTypeFilter other = (WorkflowTypeFilter)obj;
        
        if (other.getName() == null ^ this.getName() == null) return false;
        if (other.getName() != null && other.getName().equals(this.getName()) == false) return false; 
        if (other.getVersion() == null ^ this.getVersion() == null) return false;
        if (other.getVersion() != null && other.getVersion().equals(this.getVersion()) == false) return false; 
        return true;
    }

}
    
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
 * Used to filter the closed workflow executions in visibility APIs by
 * their close status.
 * </p>
 */
public class CloseStatusFilter implements Serializable {

    /**
     * <b>Required.</b> The close status that must match the close status of
     * an execution for it to meet the criteria of this filter.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>COMPLETED, FAILED, CANCELED, TERMINATED, CONTINUED_AS_NEW, TIMED_OUT
     */   
     @Nonnull
     @FieldRegex( FieldRegexValue.CLOSE_STATUS )
     private String status;

    /**
     * <b>Required.</b> The close status that must match the close status of
     * an execution for it to meet the criteria of this filter.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>COMPLETED, FAILED, CANCELED, TERMINATED, CONTINUED_AS_NEW, TIMED_OUT
     *
     * @return <b>Required.</b> The close status that must match the close status of
     *         an execution for it to meet the criteria of this filter.
     *
     * @see CloseStatus
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * <b>Required.</b> The close status that must match the close status of
     * an execution for it to meet the criteria of this filter.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>COMPLETED, FAILED, CANCELED, TERMINATED, CONTINUED_AS_NEW, TIMED_OUT
     *
     * @param status <b>Required.</b> The close status that must match the close status of
     *         an execution for it to meet the criteria of this filter.
     *
     * @see CloseStatus
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * <b>Required.</b> The close status that must match the close status of
     * an execution for it to meet the criteria of this filter.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>COMPLETED, FAILED, CANCELED, TERMINATED, CONTINUED_AS_NEW, TIMED_OUT
     *
     * @param status <b>Required.</b> The close status that must match the close status of
     *         an execution for it to meet the criteria of this filter.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     *
     * @see CloseStatus
     */
    public CloseStatusFilter withStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * <b>Required.</b> The close status that must match the close status of
     * an execution for it to meet the criteria of this filter.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>COMPLETED, FAILED, CANCELED, TERMINATED, CONTINUED_AS_NEW, TIMED_OUT
     *
     * @param status <b>Required.</b> The close status that must match the close status of
     *         an execution for it to meet the criteria of this filter.
     *
     * @see CloseStatus
     */
    public void setStatus(CloseStatus status) {
        this.status = status.toString();
    }
    
    /**
     * <b>Required.</b> The close status that must match the close status of
     * an execution for it to meet the criteria of this filter.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>COMPLETED, FAILED, CANCELED, TERMINATED, CONTINUED_AS_NEW, TIMED_OUT
     *
     * @param status <b>Required.</b> The close status that must match the close status of
     *         an execution for it to meet the criteria of this filter.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     *
     * @see CloseStatus
     */
    public CloseStatusFilter withStatus(CloseStatus status) {
        this.status = status.toString();
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
        if (getStatus() != null) sb.append("Status: " + getStatus() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getStatus() == null) ? 0 : getStatus().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof CloseStatusFilter == false) return false;
        CloseStatusFilter other = (CloseStatusFilter)obj;
        
        if (other.getStatus() == null ^ this.getStatus() == null) return false;
        if (other.getStatus() != null && other.getStatus().equals(this.getStatus()) == false) return false; 
        return true;
    }
}
    
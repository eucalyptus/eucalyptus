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

/**
 * <p>
 * Used to filter the workflow executions in visibility APIs by various
 * time-based rules. Each parameter, if specified, defines a rule that
 * must be satisfied by each returned query result. The parameter values
 * are in the
 * <a href="https://en.wikipedia.org/wiki/Unix_time"> Unix Time format </a>
 * . For example: <code>"oldestDate": 1325376070.</code>
 * </p>
 */
public class ExecutionTimeFilter implements Serializable {

    /**
     * Specifies the oldest start or close date and time to return.
     */
    @Nonnull
    private java.util.Date oldestDate;

    /**
     * Specifies the latest start or close date and time to return.
     */
    private java.util.Date latestDate;

    /**
     * Specifies the oldest start or close date and time to return.
     *
     * @return Specifies the oldest start or close date and time to return.
     */
    public java.util.Date getOldestDate() {
        return oldestDate;
    }
    
    /**
     * Specifies the oldest start or close date and time to return.
     *
     * @param oldestDate Specifies the oldest start or close date and time to return.
     */
    public void setOldestDate(java.util.Date oldestDate) {
        this.oldestDate = oldestDate;
    }
    
    /**
     * Specifies the oldest start or close date and time to return.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param oldestDate Specifies the oldest start or close date and time to return.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ExecutionTimeFilter withOldestDate(java.util.Date oldestDate) {
        this.oldestDate = oldestDate;
        return this;
    }

    /**
     * Specifies the latest start or close date and time to return.
     *
     * @return Specifies the latest start or close date and time to return.
     */
    public java.util.Date getLatestDate() {
        return latestDate;
    }
    
    /**
     * Specifies the latest start or close date and time to return.
     *
     * @param latestDate Specifies the latest start or close date and time to return.
     */
    public void setLatestDate(java.util.Date latestDate) {
        this.latestDate = latestDate;
    }
    
    /**
     * Specifies the latest start or close date and time to return.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param latestDate Specifies the latest start or close date and time to return.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ExecutionTimeFilter withLatestDate(java.util.Date latestDate) {
        this.latestDate = latestDate;
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
        if (getOldestDate() != null) sb.append("OldestDate: " + getOldestDate() + ",");
        if (getLatestDate() != null) sb.append("LatestDate: " + getLatestDate() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getOldestDate() == null) ? 0 : getOldestDate().hashCode()); 
        hashCode = prime * hashCode + ((getLatestDate() == null) ? 0 : getLatestDate().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof ExecutionTimeFilter == false) return false;
        ExecutionTimeFilter other = (ExecutionTimeFilter)obj;
        
        if (other.getOldestDate() == null ^ this.getOldestDate() == null) return false;
        if (other.getOldestDate() != null && other.getOldestDate().equals(this.getOldestDate()) == false) return false; 
        if (other.getLatestDate() == null ^ this.getLatestDate() == null) return false;
        if (other.getLatestDate() != null && other.getLatestDate().equals(this.getLatestDate()) == false) return false; 
        return true;
    }
    
}
    
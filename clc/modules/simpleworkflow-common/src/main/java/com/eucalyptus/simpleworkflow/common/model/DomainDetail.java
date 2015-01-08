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
 * Contains details of a domain.
 * </p>
 */
public class DomainDetail extends SimpleWorkflowMessage {

    /**
     * Contains general information about a domain.
     */
    private DomainInfo domainInfo;

    /**
     * Contains the configuration settings of a domain.
     */
    private DomainConfiguration configuration;

    /**
     * Contains general information about a domain.
     *
     * @return Contains general information about a domain.
     */
    public DomainInfo getDomainInfo() {
        return domainInfo;
    }
    
    /**
     * Contains general information about a domain.
     *
     * @param domainInfo Contains general information about a domain.
     */
    public void setDomainInfo(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
    }
    
    /**
     * Contains general information about a domain.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param domainInfo Contains general information about a domain.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public DomainDetail withDomainInfo(DomainInfo domainInfo) {
        this.domainInfo = domainInfo;
        return this;
    }

    /**
     * Contains the configuration settings of a domain.
     *
     * @return Contains the configuration settings of a domain.
     */
    public DomainConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Contains the configuration settings of a domain.
     *
     * @param configuration Contains the configuration settings of a domain.
     */
    public void setConfiguration(DomainConfiguration configuration) {
        this.configuration = configuration;
    }
    
    /**
     * Contains the configuration settings of a domain.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param configuration Contains the configuration settings of a domain.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public DomainDetail withConfiguration(DomainConfiguration configuration) {
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
        if (getDomainInfo() != null) sb.append("DomainInfo: " + getDomainInfo() + ",");
        if (getConfiguration() != null) sb.append("Configuration: " + getConfiguration() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getDomainInfo() == null) ? 0 : getDomainInfo().hashCode()); 
        hashCode = prime * hashCode + ((getConfiguration() == null) ? 0 : getConfiguration().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof DomainDetail == false) return false;
        DomainDetail other = (DomainDetail)obj;
        
        if (other.getDomainInfo() == null ^ this.getDomainInfo() == null) return false;
        if (other.getDomainInfo() != null && other.getDomainInfo().equals(this.getDomainInfo()) == false) return false; 
        if (other.getConfiguration() == null ^ this.getConfiguration() == null) return false;
        if (other.getConfiguration() != null && other.getConfiguration().equals(this.getConfiguration()) == false) return false; 
        return true;
    }
    
}
    
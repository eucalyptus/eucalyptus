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

/**
 * <p>
 * Contains a paginated collection of DomainInfo structures.
 * </p>
 */
public class DomainInfos extends SimpleWorkflowMessage implements Serializable {
    /**
     * A list of DomainInfo structures.
     */
    private com.amazonaws.internal.ListWithAutoConstructFlag<DomainInfo> domainInfos;

    /**
     * If a <code>NextPageToken</code> was returned by a previous call, there
     * are more results available. To retrieve the next page of results, make
     * the call again using the returned token in <code>nextPageToken</code>.
     * Keep all other arguments unchanged. <p>The configured
     * <code>maximumPageSize</code> determines how many results can be
     * returned in a single call.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     */
    private String nextPageToken;

    /**
     * A list of DomainInfo structures.
     *
     * @return A list of DomainInfo structures.
     */
    public java.util.List<DomainInfo> getDomainInfos() {
      if (domainInfos == null) {
        domainInfos = new com.amazonaws.internal.ListWithAutoConstructFlag<DomainInfo>();
        domainInfos.setAutoConstruct(true);
      }
      return domainInfos;
    }
    
    /**
     * A list of DomainInfo structures.
     *
     * @param domainInfos A list of DomainInfo structures.
     */
    public void setDomainInfos(java.util.Collection<DomainInfo> domainInfos) {
        if (domainInfos == null) {
            this.domainInfos = null;
            return;
        }
        com.amazonaws.internal.ListWithAutoConstructFlag<DomainInfo> domainInfosCopy = new com.amazonaws.internal.ListWithAutoConstructFlag<DomainInfo>(domainInfos.size());
        domainInfosCopy.addAll(domainInfos);
        this.domainInfos = domainInfosCopy;
    }
    
    /**
     * A list of DomainInfo structures.
     * <p>
     * <b>NOTE:</b> This method appends the values to the existing list (if
     * any). Use {@link #setDomainInfos(java.util.Collection)} or {@link
     * #withDomainInfos(java.util.Collection)} if you want to override the
     * existing values.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param domainInfos A list of DomainInfo structures.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DomainInfos withDomainInfos(DomainInfo... domainInfos) {
        if (getDomainInfos() == null) setDomainInfos(new java.util.ArrayList<DomainInfo>(domainInfos.length));
        for (DomainInfo value : domainInfos) {
            getDomainInfos().add(value);
        }
        return this;
    }
    
    /**
     * A list of DomainInfo structures.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param domainInfos A list of DomainInfo structures.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DomainInfos withDomainInfos(java.util.Collection<DomainInfo> domainInfos) {
        if (domainInfos == null) {
            this.domainInfos = null;
        } else {
            com.amazonaws.internal.ListWithAutoConstructFlag<DomainInfo> domainInfosCopy = new com.amazonaws.internal.ListWithAutoConstructFlag<DomainInfo>(domainInfos.size());
            domainInfosCopy.addAll(domainInfos);
            this.domainInfos = domainInfosCopy;
        }

        return this;
    }

    /**
     * If a <code>NextPageToken</code> was returned by a previous call, there
     * are more results available. To retrieve the next page of results, make
     * the call again using the returned token in <code>nextPageToken</code>.
     * Keep all other arguments unchanged. <p>The configured
     * <code>maximumPageSize</code> determines how many results can be
     * returned in a single call.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     *
     * @return If a <code>NextPageToken</code> was returned by a previous call, there
     *         are more results available. To retrieve the next page of results, make
     *         the call again using the returned token in <code>nextPageToken</code>.
     *         Keep all other arguments unchanged. <p>The configured
     *         <code>maximumPageSize</code> determines how many results can be
     *         returned in a single call.
     */
    public String getNextPageToken() {
        return nextPageToken;
    }
    
    /**
     * If a <code>NextPageToken</code> was returned by a previous call, there
     * are more results available. To retrieve the next page of results, make
     * the call again using the returned token in <code>nextPageToken</code>.
     * Keep all other arguments unchanged. <p>The configured
     * <code>maximumPageSize</code> determines how many results can be
     * returned in a single call.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     *
     * @param nextPageToken If a <code>NextPageToken</code> was returned by a previous call, there
     *         are more results available. To retrieve the next page of results, make
     *         the call again using the returned token in <code>nextPageToken</code>.
     *         Keep all other arguments unchanged. <p>The configured
     *         <code>maximumPageSize</code> determines how many results can be
     *         returned in a single call.
     */
    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
    
    /**
     * If a <code>NextPageToken</code> was returned by a previous call, there
     * are more results available. To retrieve the next page of results, make
     * the call again using the returned token in <code>nextPageToken</code>.
     * Keep all other arguments unchanged. <p>The configured
     * <code>maximumPageSize</code> determines how many results can be
     * returned in a single call.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     *
     * @param nextPageToken If a <code>NextPageToken</code> was returned by a previous call, there
     *         are more results available. To retrieve the next page of results, make
     *         the call again using the returned token in <code>nextPageToken</code>.
     *         Keep all other arguments unchanged. <p>The configured
     *         <code>maximumPageSize</code> determines how many results can be
     *         returned in a single call.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DomainInfos withNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
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
        if (getDomainInfos() != null) sb.append("DomainInfos: " + getDomainInfos() + ",");
        if (getNextPageToken() != null) sb.append("NextPageToken: " + getNextPageToken() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getDomainInfos() == null) ? 0 : getDomainInfos().hashCode()); 
        hashCode = prime * hashCode + ((getNextPageToken() == null) ? 0 : getNextPageToken().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof DomainInfos == false) return false;
        DomainInfos other = (DomainInfos)obj;
        
        if (other.getDomainInfos() == null ^ this.getDomainInfos() == null) return false;
        if (other.getDomainInfos() != null && other.getDomainInfos().equals(this.getDomainInfos()) == false) return false; 
        if (other.getNextPageToken() == null ^ this.getNextPageToken() == null) return false;
        if (other.getNextPageToken() != null && other.getNextPageToken().equals(this.getNextPageToken()) == false) return false; 
        return true;
    }

}
    
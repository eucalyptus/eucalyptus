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
 * Container for the parameters to the {@link com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#listWorkflowTypes(ListWorkflowTypesRequest) ListWorkflowTypes operation}.
 * <p>
 * Returns information about workflow types in the specified domain. The
 * results may be split into multiple pages that can be retrieved by
 * making the call repeatedly.
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
 * @see com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#listWorkflowTypes(ListWorkflowTypesRequest)
 */
@PolicyAction( vendor = "swf", action = "listworkflowtypes" )
public class ListWorkflowTypesRequest extends SimpleWorkflowMessage implements Serializable {

    /**
     * The name of the domain in which the workflow types have been
     * registered.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String domain;

    /**
     * If specified, lists the workflow type with this name.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String name;

    /**
     * Specifies the registration status of the workflow types to list.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>REGISTERED, DEPRECATED
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.REGISTRATION_STATUS )
    private String registrationStatus;

    /**
     * If on a previous call to this method a <code>NextPageToken</code> was
     * returned, the results are being paginated. To get the next page of
     * results, repeat the call with the returned token and all other
     * arguments unchanged.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     */
    @FieldRegex( FieldRegexValue.OPT_STRING_2048 )
    private String nextPageToken;

    /**
     * The maximum number of results returned in each page. The default is
     * 100, but the caller can override this value to a page size
     * <i>smaller</i> than the default. You cannot specify a page size
     * greater than 100. Note that the number of types may be less than the
     * maxiumum page size, in which case, the returned page will have fewer
     * results than the maximumPageSize specified.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Range: </b>0 - 1000<br/>
     */
    @FieldRange( max = 1000 )
    private Integer maximumPageSize;

    /**
     * When set to <code>true</code>, returns the results in reverse order.
     * By default the results are returned in ascending alphabetical order of
     * the <code>name</code> of the workflow types.
     */
    private Boolean reverseOrder;

    /**
     * The name of the domain in which the workflow types have been
     * registered.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return The name of the domain in which the workflow types have been
     *         registered.
     */
    public String getDomain() {
        return domain;
    }
    
    /**
     * The name of the domain in which the workflow types have been
     * registered.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param domain The name of the domain in which the workflow types have been
     *         registered.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    /**
     * The name of the domain in which the workflow types have been
     * registered.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param domain The name of the domain in which the workflow types have been
     *         registered.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public ListWorkflowTypesRequest withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * If specified, lists the workflow type with this name.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return If specified, lists the workflow type with this name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * If specified, lists the workflow type with this name.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param name If specified, lists the workflow type with this name.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * If specified, lists the workflow type with this name.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param name If specified, lists the workflow type with this name.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public ListWorkflowTypesRequest withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Specifies the registration status of the workflow types to list.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>REGISTERED, DEPRECATED
     *
     * @return Specifies the registration status of the workflow types to list.
     *
     * @see RegistrationStatus
     */
    public String getRegistrationStatus() {
        return registrationStatus;
    }
    
    /**
     * Specifies the registration status of the workflow types to list.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>REGISTERED, DEPRECATED
     *
     * @param registrationStatus Specifies the registration status of the workflow types to list.
     *
     * @see RegistrationStatus
     */
    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }
    
    /**
     * Specifies the registration status of the workflow types to list.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>REGISTERED, DEPRECATED
     *
     * @param registrationStatus Specifies the registration status of the workflow types to list.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     *
     * @see RegistrationStatus
     */
    public ListWorkflowTypesRequest withRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
        return this;
    }

    /**
     * Specifies the registration status of the workflow types to list.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>REGISTERED, DEPRECATED
     *
     * @param registrationStatus Specifies the registration status of the workflow types to list.
     *
     * @see RegistrationStatus
     */
    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus.toString();
    }
    
    /**
     * Specifies the registration status of the workflow types to list.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Allowed Values: </b>REGISTERED, DEPRECATED
     *
     * @param registrationStatus Specifies the registration status of the workflow types to list.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     *
     * @see RegistrationStatus
     */
    public ListWorkflowTypesRequest withRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus.toString();
        return this;
    }

    /**
     * If on a previous call to this method a <code>NextPageToken</code> was
     * returned, the results are being paginated. To get the next page of
     * results, repeat the call with the returned token and all other
     * arguments unchanged.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     *
     * @return If on a previous call to this method a <code>NextPageToken</code> was
     *         returned, the results are being paginated. To get the next page of
     *         results, repeat the call with the returned token and all other
     *         arguments unchanged.
     */
    public String getNextPageToken() {
        return nextPageToken;
    }
    
    /**
     * If on a previous call to this method a <code>NextPageToken</code> was
     * returned, the results are being paginated. To get the next page of
     * results, repeat the call with the returned token and all other
     * arguments unchanged.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     *
     * @param nextPageToken If on a previous call to this method a <code>NextPageToken</code> was
     *         returned, the results are being paginated. To get the next page of
     *         results, repeat the call with the returned token and all other
     *         arguments unchanged.
     */
    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
    
    /**
     * If on a previous call to this method a <code>NextPageToken</code> was
     * returned, the results are being paginated. To get the next page of
     * results, repeat the call with the returned token and all other
     * arguments unchanged.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 2048<br/>
     *
     * @param nextPageToken If on a previous call to this method a <code>NextPageToken</code> was
     *         returned, the results are being paginated. To get the next page of
     *         results, repeat the call with the returned token and all other
     *         arguments unchanged.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public ListWorkflowTypesRequest withNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
        return this;
    }

    /**
     * The maximum number of results returned in each page. The default is
     * 100, but the caller can override this value to a page size
     * <i>smaller</i> than the default. You cannot specify a page size
     * greater than 100. Note that the number of types may be less than the
     * maxiumum page size, in which case, the returned page will have fewer
     * results than the maximumPageSize specified.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Range: </b>0 - 1000<br/>
     *
     * @return The maximum number of results returned in each page. The default is
     *         100, but the caller can override this value to a page size
     *         <i>smaller</i> than the default. You cannot specify a page size
     *         greater than 100. Note that the number of types may be less than the
     *         maxiumum page size, in which case, the returned page will have fewer
     *         results than the maximumPageSize specified.
     */
    public Integer getMaximumPageSize() {
        return maximumPageSize;
    }
    
    /**
     * The maximum number of results returned in each page. The default is
     * 100, but the caller can override this value to a page size
     * <i>smaller</i> than the default. You cannot specify a page size
     * greater than 100. Note that the number of types may be less than the
     * maxiumum page size, in which case, the returned page will have fewer
     * results than the maximumPageSize specified.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Range: </b>0 - 1000<br/>
     *
     * @param maximumPageSize The maximum number of results returned in each page. The default is
     *         100, but the caller can override this value to a page size
     *         <i>smaller</i> than the default. You cannot specify a page size
     *         greater than 100. Note that the number of types may be less than the
     *         maxiumum page size, in which case, the returned page will have fewer
     *         results than the maximumPageSize specified.
     */
    public void setMaximumPageSize(Integer maximumPageSize) {
        this.maximumPageSize = maximumPageSize;
    }
    
    /**
     * The maximum number of results returned in each page. The default is
     * 100, but the caller can override this value to a page size
     * <i>smaller</i> than the default. You cannot specify a page size
     * greater than 100. Note that the number of types may be less than the
     * maxiumum page size, in which case, the returned page will have fewer
     * results than the maximumPageSize specified.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Range: </b>0 - 1000<br/>
     *
     * @param maximumPageSize The maximum number of results returned in each page. The default is
     *         100, but the caller can override this value to a page size
     *         <i>smaller</i> than the default. You cannot specify a page size
     *         greater than 100. Note that the number of types may be less than the
     *         maxiumum page size, in which case, the returned page will have fewer
     *         results than the maximumPageSize specified.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public ListWorkflowTypesRequest withMaximumPageSize(Integer maximumPageSize) {
        this.maximumPageSize = maximumPageSize;
        return this;
    }

    /**
     * When set to <code>true</code>, returns the results in reverse order.
     * By default the results are returned in ascending alphabetical order of
     * the <code>name</code> of the workflow types.
     *
     * @return When set to <code>true</code>, returns the results in reverse order.
     *         By default the results are returned in ascending alphabetical order of
     *         the <code>name</code> of the workflow types.
     */
    public Boolean isReverseOrder() {
        return reverseOrder;
    }
    
    /**
     * When set to <code>true</code>, returns the results in reverse order.
     * By default the results are returned in ascending alphabetical order of
     * the <code>name</code> of the workflow types.
     *
     * @param reverseOrder When set to <code>true</code>, returns the results in reverse order.
     *         By default the results are returned in ascending alphabetical order of
     *         the <code>name</code> of the workflow types.
     */
    public void setReverseOrder(Boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
    }
    
    /**
     * When set to <code>true</code>, returns the results in reverse order.
     * By default the results are returned in ascending alphabetical order of
     * the <code>name</code> of the workflow types.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param reverseOrder When set to <code>true</code>, returns the results in reverse order.
     *         By default the results are returned in ascending alphabetical order of
     *         the <code>name</code> of the workflow types.
     *
     * @return A reference to this updated object so that method calls can be chained 
     *         together.
     */
    public ListWorkflowTypesRequest withReverseOrder(Boolean reverseOrder) {
        this.reverseOrder = reverseOrder;
        return this;
    }

    /**
     * When set to <code>true</code>, returns the results in reverse order.
     * By default the results are returned in ascending alphabetical order of
     * the <code>name</code> of the workflow types.
     *
     * @return When set to <code>true</code>, returns the results in reverse order.
     *         By default the results are returned in ascending alphabetical order of
     *         the <code>name</code> of the workflow types.
     */
    public Boolean getReverseOrder() {
        return reverseOrder;
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
        if (getName() != null) sb.append("Name: " + getName() + ",");
        if (getRegistrationStatus() != null) sb.append("RegistrationStatus: " + getRegistrationStatus() + ",");
        if (getNextPageToken() != null) sb.append("NextPageToken: " + getNextPageToken() + ",");
        if (getMaximumPageSize() != null) sb.append("MaximumPageSize: " + getMaximumPageSize() + ",");
        if (isReverseOrder() != null) sb.append("ReverseOrder: " + isReverseOrder() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getDomain() == null) ? 0 : getDomain().hashCode()); 
        hashCode = prime * hashCode + ((getName() == null) ? 0 : getName().hashCode()); 
        hashCode = prime * hashCode + ((getRegistrationStatus() == null) ? 0 : getRegistrationStatus().hashCode()); 
        hashCode = prime * hashCode + ((getNextPageToken() == null) ? 0 : getNextPageToken().hashCode()); 
        hashCode = prime * hashCode + ((getMaximumPageSize() == null) ? 0 : getMaximumPageSize().hashCode()); 
        hashCode = prime * hashCode + ((isReverseOrder() == null) ? 0 : isReverseOrder().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof ListWorkflowTypesRequest == false) return false;
        ListWorkflowTypesRequest other = (ListWorkflowTypesRequest)obj;
        
        if (other.getDomain() == null ^ this.getDomain() == null) return false;
        if (other.getDomain() != null && other.getDomain().equals(this.getDomain()) == false) return false; 
        if (other.getName() == null ^ this.getName() == null) return false;
        if (other.getName() != null && other.getName().equals(this.getName()) == false) return false; 
        if (other.getRegistrationStatus() == null ^ this.getRegistrationStatus() == null) return false;
        if (other.getRegistrationStatus() != null && other.getRegistrationStatus().equals(this.getRegistrationStatus()) == false) return false; 
        if (other.getNextPageToken() == null ^ this.getNextPageToken() == null) return false;
        if (other.getNextPageToken() != null && other.getNextPageToken().equals(this.getNextPageToken()) == false) return false; 
        if (other.getMaximumPageSize() == null ^ this.getMaximumPageSize() == null) return false;
        if (other.getMaximumPageSize() != null && other.getMaximumPageSize().equals(this.getMaximumPageSize()) == false) return false; 
        if (other.isReverseOrder() == null ^ this.isReverseOrder() == null) return false;
        if (other.isReverseOrder() != null && other.isReverseOrder().equals(this.isReverseOrder()) == false) return false; 
        return true;
    }
    
}
    
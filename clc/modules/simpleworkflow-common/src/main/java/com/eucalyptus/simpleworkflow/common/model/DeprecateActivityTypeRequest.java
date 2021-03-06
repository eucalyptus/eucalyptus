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
 * Container for the parameters to the {@link com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#deprecateActivityType(DeprecateActivityTypeRequest) DeprecateActivityType operation}.
 * <p>
 * Deprecates the specified <i>activity type</i> . After an activity type
 * has been deprecated, you cannot create new tasks of that activity
 * type. Tasks of this type that were scheduled before the type was
 * deprecated will continue to run.
 * </p>
 * <p>
 * <b>NOTE:</b>This operation is eventually consistent. The results are
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
 * <li>Constrain the following parameters by using a
 * <code>Condition</code> element with the appropriate keys.
 * <ul>
 * <li> <code>activityType.name</code> : String constraint. The key is
 * <code>swf:activityType.name</code> .</li>
 * <li> <code>activityType.version</code> : String constraint. The key
 * is <code>swf:activityType.version</code> .</li>
 * 
 * </ul>
 * </li>
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
 * @see com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow#deprecateActivityType(DeprecateActivityTypeRequest)
 */
@PolicyAction( vendor = "swf", action = "deprecateactivitytype" )
public class DeprecateActivityTypeRequest extends SimpleWorkflowMessage implements Serializable {

    /**
     * The name of the domain in which the activity type is registered.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String domain;

    /**
     * The activity type to deprecate.
     */
    @Nonnull
    private ActivityType activityType;

    /**
     * The name of the domain in which the activity type is registered.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return The name of the domain in which the activity type is registered.
     */
    public String getDomain() {
        return domain;
    }
    
    /**
     * The name of the domain in which the activity type is registered.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param domain The name of the domain in which the activity type is registered.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
    /**
     * The name of the domain in which the activity type is registered.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param domain The name of the domain in which the activity type is registered.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DeprecateActivityTypeRequest withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * The activity type to deprecate.
     *
     * @return The activity type to deprecate.
     */
    public ActivityType getActivityType() {
        return activityType;
    }
    
    /**
     * The activity type to deprecate.
     *
     * @param activityType The activity type to deprecate.
     */
    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }
    
    /**
     * The activity type to deprecate.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param activityType The activity type to deprecate.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public DeprecateActivityTypeRequest withActivityType(ActivityType activityType) {
        this.activityType = activityType;
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
        if (getActivityType() != null) sb.append("ActivityType: " + getActivityType() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getDomain() == null) ? 0 : getDomain().hashCode()); 
        hashCode = prime * hashCode + ((getActivityType() == null) ? 0 : getActivityType().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof DeprecateActivityTypeRequest == false) return false;
        DeprecateActivityTypeRequest other = (DeprecateActivityTypeRequest)obj;
        
        if (other.getDomain() == null ^ this.getDomain() == null) return false;
        if (other.getDomain() != null && other.getDomain().equals(this.getDomain()) == false) return false; 
        if (other.getActivityType() == null ^ this.getActivityType() == null) return false;
        if (other.getActivityType() != null && other.getActivityType().equals(this.getActivityType()) == false) return false; 
        return true;
    }
}
    
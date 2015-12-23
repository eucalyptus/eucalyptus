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

 * Copyright 2010-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * 
 *  http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.eucalyptus.simpleworkflow.common.model;

import static com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage.FieldRegex;
import static com.eucalyptus.simpleworkflow.common.model.SimpleWorkflowMessage.FieldRegexValue;
import java.io.Serializable;
import javax.annotation.Nonnull;
/**
 * <p>
 * Provides details for the <code>LambdaFunctionScheduled</code> event.
 * </p>
 */
public class LambdaFunctionScheduledEventAttributes implements Serializable {

    /**
     * The unique Amazon SWF ID for the AWS Lambda task.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String id;

    /**
     * The name of the scheduled AWS Lambda function.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 64<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_64 )
    private String name;

    /**
     * Input provided to the AWS Lambda function.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 32768<br/>
     */
    @FieldRegex( FieldRegexValue.OPT_STRING_32768 )
    private String input;

    /**
     * The maximum time, in seconds, that the AWS Lambda function can take to
     * execute from start to close before it is marked as failed.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 8<br/>
     */
    @FieldRegex( FieldRegexValue.DURATION_8 )
    private String startToCloseTimeout;

    /**
     * The ID of the <code>DecisionTaskCompleted</code> event for the
     * decision that resulted in the scheduling of this AWS Lambda function.
     * This information can be useful for diagnosing problems by tracing back
     * the chain of events leading up to this event.
     */
    @Nonnull
    private Long decisionTaskCompletedEventId;

    /**
     * The unique Amazon SWF ID for the AWS Lambda task.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return The unique Amazon SWF ID for the AWS Lambda task.
     */
    public String getId() {
        return id;
    }
    
    /**
     * The unique Amazon SWF ID for the AWS Lambda task.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param id The unique Amazon SWF ID for the AWS Lambda task.
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * The unique Amazon SWF ID for the AWS Lambda task.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param id The unique Amazon SWF ID for the AWS Lambda task.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public LambdaFunctionScheduledEventAttributes withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * The name of the scheduled AWS Lambda function.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 64<br/>
     *
     * @return The name of the scheduled AWS Lambda function.
     */
    public String getName() {
        return name;
    }
    
    /**
     * The name of the scheduled AWS Lambda function.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 64<br/>
     *
     * @param name The name of the scheduled AWS Lambda function.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * The name of the scheduled AWS Lambda function.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 64<br/>
     *
     * @param name The name of the scheduled AWS Lambda function.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public LambdaFunctionScheduledEventAttributes withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Input provided to the AWS Lambda function.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 32768<br/>
     *
     * @return Input provided to the AWS Lambda function.
     */
    public String getInput() {
        return input;
    }
    
    /**
     * Input provided to the AWS Lambda function.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 32768<br/>
     *
     * @param input Input provided to the AWS Lambda function.
     */
    public void setInput(String input) {
        this.input = input;
    }
    
    /**
     * Input provided to the AWS Lambda function.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 32768<br/>
     *
     * @param input Input provided to the AWS Lambda function.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public LambdaFunctionScheduledEventAttributes withInput(String input) {
        this.input = input;
        return this;
    }

    /**
     * The maximum time, in seconds, that the AWS Lambda function can take to
     * execute from start to close before it is marked as failed.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 8<br/>
     *
     * @return The maximum time, in seconds, that the AWS Lambda function can take to
     *         execute from start to close before it is marked as failed.
     */
    public String getStartToCloseTimeout() {
        return startToCloseTimeout;
    }
    
    /**
     * The maximum time, in seconds, that the AWS Lambda function can take to
     * execute from start to close before it is marked as failed.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 8<br/>
     *
     * @param startToCloseTimeout The maximum time, in seconds, that the AWS Lambda function can take to
     *         execute from start to close before it is marked as failed.
     */
    public void setStartToCloseTimeout(String startToCloseTimeout) {
        this.startToCloseTimeout = startToCloseTimeout;
    }
    
    /**
     * The maximum time, in seconds, that the AWS Lambda function can take to
     * execute from start to close before it is marked as failed.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 8<br/>
     *
     * @param startToCloseTimeout The maximum time, in seconds, that the AWS Lambda function can take to
     *         execute from start to close before it is marked as failed.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public LambdaFunctionScheduledEventAttributes withStartToCloseTimeout(String startToCloseTimeout) {
        this.startToCloseTimeout = startToCloseTimeout;
        return this;
    }

    /**
     * The ID of the <code>DecisionTaskCompleted</code> event for the
     * decision that resulted in the scheduling of this AWS Lambda function.
     * This information can be useful for diagnosing problems by tracing back
     * the chain of events leading up to this event.
     *
     * @return The ID of the <code>DecisionTaskCompleted</code> event for the
     *         decision that resulted in the scheduling of this AWS Lambda function.
     *         This information can be useful for diagnosing problems by tracing back
     *         the chain of events leading up to this event.
     */
    public Long getDecisionTaskCompletedEventId() {
        return decisionTaskCompletedEventId;
    }
    
    /**
     * The ID of the <code>DecisionTaskCompleted</code> event for the
     * decision that resulted in the scheduling of this AWS Lambda function.
     * This information can be useful for diagnosing problems by tracing back
     * the chain of events leading up to this event.
     *
     * @param decisionTaskCompletedEventId The ID of the <code>DecisionTaskCompleted</code> event for the
     *         decision that resulted in the scheduling of this AWS Lambda function.
     *         This information can be useful for diagnosing problems by tracing back
     *         the chain of events leading up to this event.
     */
    public void setDecisionTaskCompletedEventId(Long decisionTaskCompletedEventId) {
        this.decisionTaskCompletedEventId = decisionTaskCompletedEventId;
    }
    
    /**
     * The ID of the <code>DecisionTaskCompleted</code> event for the
     * decision that resulted in the scheduling of this AWS Lambda function.
     * This information can be useful for diagnosing problems by tracing back
     * the chain of events leading up to this event.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     *
     * @param decisionTaskCompletedEventId The ID of the <code>DecisionTaskCompleted</code> event for the
     *         decision that resulted in the scheduling of this AWS Lambda function.
     *         This information can be useful for diagnosing problems by tracing back
     *         the chain of events leading up to this event.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public LambdaFunctionScheduledEventAttributes withDecisionTaskCompletedEventId(Long decisionTaskCompletedEventId) {
        this.decisionTaskCompletedEventId = decisionTaskCompletedEventId;
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
        if (getId() != null) sb.append("Id: " + getId() + ",");
        if (getName() != null) sb.append("Name: " + getName() + ",");
        if (getInput() != null) sb.append("Input: " + getInput() + ",");
        if (getStartToCloseTimeout() != null) sb.append("StartToCloseTimeout: " + getStartToCloseTimeout() + ",");
        if (getDecisionTaskCompletedEventId() != null) sb.append("DecisionTaskCompletedEventId: " + getDecisionTaskCompletedEventId() );
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        
        hashCode = prime * hashCode + ((getId() == null) ? 0 : getId().hashCode()); 
        hashCode = prime * hashCode + ((getName() == null) ? 0 : getName().hashCode()); 
        hashCode = prime * hashCode + ((getInput() == null) ? 0 : getInput().hashCode()); 
        hashCode = prime * hashCode + ((getStartToCloseTimeout() == null) ? 0 : getStartToCloseTimeout().hashCode()); 
        hashCode = prime * hashCode + ((getDecisionTaskCompletedEventId() == null) ? 0 : getDecisionTaskCompletedEventId().hashCode()); 
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof LambdaFunctionScheduledEventAttributes == false) return false;
        LambdaFunctionScheduledEventAttributes other = (LambdaFunctionScheduledEventAttributes)obj;
        
        if (other.getId() == null ^ this.getId() == null) return false;
        if (other.getId() != null && other.getId().equals(this.getId()) == false) return false; 
        if (other.getName() == null ^ this.getName() == null) return false;
        if (other.getName() != null && other.getName().equals(this.getName()) == false) return false; 
        if (other.getInput() == null ^ this.getInput() == null) return false;
        if (other.getInput() != null && other.getInput().equals(this.getInput()) == false) return false; 
        if (other.getStartToCloseTimeout() == null ^ this.getStartToCloseTimeout() == null) return false;
        if (other.getStartToCloseTimeout() != null && other.getStartToCloseTimeout().equals(this.getStartToCloseTimeout()) == false) return false; 
        if (other.getDecisionTaskCompletedEventId() == null ^ this.getDecisionTaskCompletedEventId() == null) return false;
        if (other.getDecisionTaskCompletedEventId() != null && other.getDecisionTaskCompletedEventId().equals(this.getDecisionTaskCompletedEventId()) == false) return false; 
        return true;
    }
    
}
    
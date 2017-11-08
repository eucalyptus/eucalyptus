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
 * Provides details of the <code>ScheduleLambdaFunction</code> decision.
 * </p>
 * <p>
 * <b>Access Control</b>
 * </p>
 * <p>
 * You can use IAM policies to control this decision's access to Amazon
 * SWF resources as follows:
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
 * <li> <code>taskList</code> : String constraint. The key is
 * <code>swf:taskList.name</code> .</li>
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
 */
public class ScheduleLambdaFunctionDecisionAttributes implements Serializable {

    /**
     * <b>Required.</b> The SWF <code>id</code> of the AWS Lambda task.
     * <p>The specified string must not start or end with whitespace. It must
     * not contain a <code>:</code> (colon), <code>/</code> (slash),
     * <code>|</code> (vertical bar), or any control characters
     * (\u0000-\u001f | \u007f - \u009f). Also, it must not contain the
     * literal string "arn".
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_256 )
    private String id;

    /**
     * <b>Required.</b> The name of the AWS Lambda function to invoke.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 64<br/>
     */
    @Nonnull
    @FieldRegex( FieldRegexValue.NAME_64 )
    private String name;

    /**
     * The input provided to the AWS Lambda function.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 32768<br/>
     */
    @FieldRegex( FieldRegexValue.OPT_STRING_32768 )
    private String input;

    /**
     * If set, specifies the maximum duration the function may take to
     * execute.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 8<br/>
     */
    @FieldRegex( FieldRegexValue.DURATION_8 )
    private String startToCloseTimeout;

    /**
     * <b>Required.</b> The SWF <code>id</code> of the AWS Lambda task.
     * <p>The specified string must not start or end with whitespace. It must
     * not contain a <code>:</code> (colon), <code>/</code> (slash),
     * <code>|</code> (vertical bar), or any control characters
     * (\u0000-\u001f | \u007f - \u009f). Also, it must not contain the
     * literal string "arn".
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @return <b>Required.</b> The SWF <code>id</code> of the AWS Lambda task.
     *         <p>The specified string must not start or end with whitespace. It must
     *         not contain a <code>:</code> (colon), <code>/</code> (slash),
     *         <code>|</code> (vertical bar), or any control characters
     *         (\u0000-\u001f | \u007f - \u009f). Also, it must not contain the
     *         literal string "arn".
     */
    public String getId() {
        return id;
    }
    
    /**
     * <b>Required.</b> The SWF <code>id</code> of the AWS Lambda task.
     * <p>The specified string must not start or end with whitespace. It must
     * not contain a <code>:</code> (colon), <code>/</code> (slash),
     * <code>|</code> (vertical bar), or any control characters
     * (\u0000-\u001f | \u007f - \u009f). Also, it must not contain the
     * literal string "arn".
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param id <b>Required.</b> The SWF <code>id</code> of the AWS Lambda task.
     *         <p>The specified string must not start or end with whitespace. It must
     *         not contain a <code>:</code> (colon), <code>/</code> (slash),
     *         <code>|</code> (vertical bar), or any control characters
     *         (\u0000-\u001f | \u007f - \u009f). Also, it must not contain the
     *         literal string "arn".
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * <b>Required.</b> The SWF <code>id</code> of the AWS Lambda task.
     * <p>The specified string must not start or end with whitespace. It must
     * not contain a <code>:</code> (colon), <code>/</code> (slash),
     * <code>|</code> (vertical bar), or any control characters
     * (\u0000-\u001f | \u007f - \u009f). Also, it must not contain the
     * literal string "arn".
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 256<br/>
     *
     * @param id <b>Required.</b> The SWF <code>id</code> of the AWS Lambda task.
     *         <p>The specified string must not start or end with whitespace. It must
     *         not contain a <code>:</code> (colon), <code>/</code> (slash),
     *         <code>|</code> (vertical bar), or any control characters
     *         (\u0000-\u001f | \u007f - \u009f). Also, it must not contain the
     *         literal string "arn".
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ScheduleLambdaFunctionDecisionAttributes withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * <b>Required.</b> The name of the AWS Lambda function to invoke.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 64<br/>
     *
     * @return <b>Required.</b> The name of the AWS Lambda function to invoke.
     */
    public String getName() {
        return name;
    }
    
    /**
     * <b>Required.</b> The name of the AWS Lambda function to invoke.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 64<br/>
     *
     * @param name <b>Required.</b> The name of the AWS Lambda function to invoke.
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * <b>Required.</b> The name of the AWS Lambda function to invoke.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 64<br/>
     *
     * @param name <b>Required.</b> The name of the AWS Lambda function to invoke.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ScheduleLambdaFunctionDecisionAttributes withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * The input provided to the AWS Lambda function.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 32768<br/>
     *
     * @return The input provided to the AWS Lambda function.
     */
    public String getInput() {
        return input;
    }
    
    /**
     * The input provided to the AWS Lambda function.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 32768<br/>
     *
     * @param input The input provided to the AWS Lambda function.
     */
    public void setInput(String input) {
        this.input = input;
    }
    
    /**
     * The input provided to the AWS Lambda function.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>1 - 32768<br/>
     *
     * @param input The input provided to the AWS Lambda function.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ScheduleLambdaFunctionDecisionAttributes withInput(String input) {
        this.input = input;
        return this;
    }

    /**
     * If set, specifies the maximum duration the function may take to
     * execute.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 8<br/>
     *
     * @return If set, specifies the maximum duration the function may take to
     *         execute.
     */
    public String getStartToCloseTimeout() {
        return startToCloseTimeout;
    }
    
    /**
     * If set, specifies the maximum duration the function may take to
     * execute.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 8<br/>
     *
     * @param startToCloseTimeout If set, specifies the maximum duration the function may take to
     *         execute.
     */
    public void setStartToCloseTimeout(String startToCloseTimeout) {
        this.startToCloseTimeout = startToCloseTimeout;
    }
    
    /**
     * If set, specifies the maximum duration the function may take to
     * execute.
     * <p>
     * Returns a reference to this object so that method calls can be chained together.
     * <p>
     * <b>Constraints:</b><br/>
     * <b>Length: </b>0 - 8<br/>
     *
     * @param startToCloseTimeout If set, specifies the maximum duration the function may take to
     *         execute.
     *
     * @return A reference to this updated object so that method calls can be chained
     *         together.
     */
    public ScheduleLambdaFunctionDecisionAttributes withStartToCloseTimeout(String startToCloseTimeout) {
        this.startToCloseTimeout = startToCloseTimeout;
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
        if (getStartToCloseTimeout() != null) sb.append("StartToCloseTimeout: " + getStartToCloseTimeout() );
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
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        if (obj instanceof ScheduleLambdaFunctionDecisionAttributes == false) return false;
        ScheduleLambdaFunctionDecisionAttributes other = (ScheduleLambdaFunctionDecisionAttributes)obj;
        
        if (other.getId() == null ^ this.getId() == null) return false;
        if (other.getId() != null && other.getId().equals(this.getId()) == false) return false; 
        if (other.getName() == null ^ this.getName() == null) return false;
        if (other.getName() != null && other.getName().equals(this.getName()) == false) return false; 
        if (other.getInput() == null ^ this.getInput() == null) return false;
        if (other.getInput() != null && other.getInput().equals(this.getInput()) == false) return false; 
        if (other.getStartToCloseTimeout() == null ^ this.getStartToCloseTimeout() == null) return false;
        if (other.getStartToCloseTimeout() != null && other.getStartToCloseTimeout().equals(this.getStartToCloseTimeout()) == false) return false; 
        return true;
    }

}
    
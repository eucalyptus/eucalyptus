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

/**
 * Schedule Lambda Function Failed Cause
 */
public enum ScheduleLambdaFunctionFailedCause {
    
    ID_ALREADY_IN_USE("ID_ALREADY_IN_USE"),
    OPEN_LAMBDA_FUNCTIONS_LIMIT_EXCEEDED("OPEN_LAMBDA_FUNCTIONS_LIMIT_EXCEEDED"),
    LAMBDA_FUNCTION_CREATION_RATE_EXCEEDED("LAMBDA_FUNCTION_CREATION_RATE_EXCEEDED"),
    LAMBDA_SERVICE_NOT_AVAILABLE_IN_REGION("LAMBDA_SERVICE_NOT_AVAILABLE_IN_REGION");

    private String value;

    private ScheduleLambdaFunctionFailedCause(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Use this in place of valueOf.
     *
     * @param value
     *            real value
     * @return ScheduleLambdaFunctionFailedCause corresponding to the value
     */
    public static ScheduleLambdaFunctionFailedCause fromValue(String value) {
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        
        } else if ("ID_ALREADY_IN_USE".equals(value)) {
            return ScheduleLambdaFunctionFailedCause.ID_ALREADY_IN_USE;
        } else if ("OPEN_LAMBDA_FUNCTIONS_LIMIT_EXCEEDED".equals(value)) {
            return ScheduleLambdaFunctionFailedCause.OPEN_LAMBDA_FUNCTIONS_LIMIT_EXCEEDED;
        } else if ("LAMBDA_FUNCTION_CREATION_RATE_EXCEEDED".equals(value)) {
            return ScheduleLambdaFunctionFailedCause.LAMBDA_FUNCTION_CREATION_RATE_EXCEEDED;
        } else if ("LAMBDA_SERVICE_NOT_AVAILABLE_IN_REGION".equals(value)) {
            return ScheduleLambdaFunctionFailedCause.LAMBDA_SERVICE_NOT_AVAILABLE_IN_REGION;
        } else {
            throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
        }
    }
}
    
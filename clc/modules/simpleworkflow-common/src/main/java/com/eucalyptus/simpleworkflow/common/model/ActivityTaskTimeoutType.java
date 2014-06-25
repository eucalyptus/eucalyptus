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

/**
 * Activity Task Timeout Type
 */
public enum ActivityTaskTimeoutType {
    
    START_TO_CLOSE("START_TO_CLOSE"),
    SCHEDULE_TO_START("SCHEDULE_TO_START"),
    SCHEDULE_TO_CLOSE("SCHEDULE_TO_CLOSE"),
    HEARTBEAT("HEARTBEAT");

    private String value;

    private ActivityTaskTimeoutType(String value) {
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
     * @return ActivityTaskTimeoutType corresponding to the value
     */
    public static ActivityTaskTimeoutType fromValue(String value) {
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        
        } else if ("START_TO_CLOSE".equals(value)) {
            return ActivityTaskTimeoutType.START_TO_CLOSE;
        } else if ("SCHEDULE_TO_START".equals(value)) {
            return ActivityTaskTimeoutType.SCHEDULE_TO_START;
        } else if ("SCHEDULE_TO_CLOSE".equals(value)) {
            return ActivityTaskTimeoutType.SCHEDULE_TO_CLOSE;
        } else if ("HEARTBEAT".equals(value)) {
            return ActivityTaskTimeoutType.HEARTBEAT;
        } else {
            throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
        }
    }
}
    
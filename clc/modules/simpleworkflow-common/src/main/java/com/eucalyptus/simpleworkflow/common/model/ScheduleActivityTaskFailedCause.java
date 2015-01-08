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
 * Schedule Activity Task Failed Cause
 */
public enum ScheduleActivityTaskFailedCause {
    
    ACTIVITY_TYPE_DEPRECATED("ACTIVITY_TYPE_DEPRECATED"),
    ACTIVITY_TYPE_DOES_NOT_EXIST("ACTIVITY_TYPE_DOES_NOT_EXIST"),
    ACTIVITY_ID_ALREADY_IN_USE("ACTIVITY_ID_ALREADY_IN_USE"),
    OPEN_ACTIVITIES_LIMIT_EXCEEDED("OPEN_ACTIVITIES_LIMIT_EXCEEDED"),
    ACTIVITY_CREATION_RATE_EXCEEDED("ACTIVITY_CREATION_RATE_EXCEEDED"),
    DEFAULT_SCHEDULE_TO_CLOSE_TIMEOUT_UNDEFINED("DEFAULT_SCHEDULE_TO_CLOSE_TIMEOUT_UNDEFINED"),
    DEFAULT_TASK_LIST_UNDEFINED("DEFAULT_TASK_LIST_UNDEFINED"),
    DEFAULT_SCHEDULE_TO_START_TIMEOUT_UNDEFINED("DEFAULT_SCHEDULE_TO_START_TIMEOUT_UNDEFINED"),
    DEFAULT_START_TO_CLOSE_TIMEOUT_UNDEFINED("DEFAULT_START_TO_CLOSE_TIMEOUT_UNDEFINED"),
    DEFAULT_HEARTBEAT_TIMEOUT_UNDEFINED("DEFAULT_HEARTBEAT_TIMEOUT_UNDEFINED"),
    OPERATION_NOT_PERMITTED("OPERATION_NOT_PERMITTED");

    private String value;

    private ScheduleActivityTaskFailedCause(String value) {
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
     * @return ScheduleActivityTaskFailedCause corresponding to the value
     */
    public static ScheduleActivityTaskFailedCause fromValue(String value) {
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Value cannot be null or empty!");
        
        } else if ("ACTIVITY_TYPE_DEPRECATED".equals(value)) {
            return ScheduleActivityTaskFailedCause.ACTIVITY_TYPE_DEPRECATED;
        } else if ("ACTIVITY_TYPE_DOES_NOT_EXIST".equals(value)) {
            return ScheduleActivityTaskFailedCause.ACTIVITY_TYPE_DOES_NOT_EXIST;
        } else if ("ACTIVITY_ID_ALREADY_IN_USE".equals(value)) {
            return ScheduleActivityTaskFailedCause.ACTIVITY_ID_ALREADY_IN_USE;
        } else if ("OPEN_ACTIVITIES_LIMIT_EXCEEDED".equals(value)) {
            return ScheduleActivityTaskFailedCause.OPEN_ACTIVITIES_LIMIT_EXCEEDED;
        } else if ("ACTIVITY_CREATION_RATE_EXCEEDED".equals(value)) {
            return ScheduleActivityTaskFailedCause.ACTIVITY_CREATION_RATE_EXCEEDED;
        } else if ("DEFAULT_SCHEDULE_TO_CLOSE_TIMEOUT_UNDEFINED".equals(value)) {
            return ScheduleActivityTaskFailedCause.DEFAULT_SCHEDULE_TO_CLOSE_TIMEOUT_UNDEFINED;
        } else if ("DEFAULT_TASK_LIST_UNDEFINED".equals(value)) {
            return ScheduleActivityTaskFailedCause.DEFAULT_TASK_LIST_UNDEFINED;
        } else if ("DEFAULT_SCHEDULE_TO_START_TIMEOUT_UNDEFINED".equals(value)) {
            return ScheduleActivityTaskFailedCause.DEFAULT_SCHEDULE_TO_START_TIMEOUT_UNDEFINED;
        } else if ("DEFAULT_START_TO_CLOSE_TIMEOUT_UNDEFINED".equals(value)) {
            return ScheduleActivityTaskFailedCause.DEFAULT_START_TO_CLOSE_TIMEOUT_UNDEFINED;
        } else if ("DEFAULT_HEARTBEAT_TIMEOUT_UNDEFINED".equals(value)) {
            return ScheduleActivityTaskFailedCause.DEFAULT_HEARTBEAT_TIMEOUT_UNDEFINED;
        } else if ("OPERATION_NOT_PERMITTED".equals(value)) {
            return ScheduleActivityTaskFailedCause.OPERATION_NOT_PERMITTED;
        } else {
            throw new IllegalArgumentException("Cannot create enum from " + value + " value!");
        }
    }
}
    
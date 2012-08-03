// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

#ifndef INCLUDE_SENSOR_H
#define INCLUDE_SENSOR_H
#define MAX_SENSOR_NAME 64

typedef struct {
    long long timestampMs; // in milliseconds
    double value;          // measurement
    char available;        // if '1' then value is valid, otherwise it is not
} sensorValue;

typedef struct {
    char dimensionName [MAX_SENSOR_NAME]; // e.g. "default", "root", "vol-123ABC"
    sensorValue ** values;                // array of pointers
    int valuesLen;                        // size of the array
} sensorDimension;

typedef struct {
    enum { SENSOR_SUMMATION, SENSOR_AVERAGE } type;
    long long collectionIntervalMs; // the spacing of values, based on sensor's configuration
    long long sequenceNum;          // starts with 0 when sensor is reset and monotonically increases
    sensorDimension ** dimensions;  // array of pointers
    int dimensionsLen;              // size of the array
} sensorCounter;

typedef struct {
    char metricName [MAX_SENSOR_NAME]; // e.g. "CPUUtilization"
    sensorCounter ** counters;         // array of pointers
    int countersLen;                   // size of the array
} sensorMetric;

typedef struct {
    char resourceName [MAX_SENSOR_NAME]; // e.g. "i-1234567"
    char resourceType [MAX_SENSOR_NAME]; // e.g. "instance"
    sensorMetric ** metrics;             // array of pointers
    int metricsLen;                      // size of the array
} sensorResource;

void sensor_free_metric (sensorMetric * m);

#endif

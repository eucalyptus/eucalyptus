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

#define MAX_SENSOR_NAME_LEN    64
#define MAX_SENSOR_VALUES      32 // by default 20
#define MAX_SENSOR_DIMENSIONS  32 // root, ephemeral[0-1], vol-XYZ
#define MAX_SENSOR_COUNTERS    2  // we only have two types of counters (summation|average) for now
#define MAX_SENSOR_METRICS     16 // currently 9 are implemented

#define DEFAULT_SENSOR_SLEEP_DURATION_USEC 5000000L
#define MIN_COLLECTION_INTERVAL_MS 1000L // below 1 second is too frequent
#define MAX_COLLECTION_INTERVAL_MS 86400000L // above 24 hours is too infrequent

typedef struct {
    long long timestampMs; // in milliseconds
    double value;          // measurement
    char available;        // if '1' then value is valid, otherwise it is not
} sensorValue;

typedef struct {
    char dimensionName [MAX_SENSOR_NAME_LEN]; // e.g. "default", "root", "vol-123ABC"
    sensorValue values [MAX_SENSOR_VALUES];   // array of values (not pointers, to simplify shared-memory region use)
    int valuesLen;                            // size of the array
} sensorDimension;

static char * sensorCounterTypeName [] = {
    "summation",
    "average"
};

typedef struct {
    enum { SENSOR_SUMMATION=0, SENSOR_AVERAGE } type;
    long long collectionIntervalMs;                     // the spacing of values, based on sensor's configuration
    long long sequenceNum;                              // starts with 0 when sensor is reset and monotonically increases
    sensorDimension dimensions [MAX_SENSOR_DIMENSIONS]; // array of values (not pointers, to simplify shared-memory region use)
    int dimensionsLen;                                  // size of the array
} sensorCounter;

typedef struct {
    char metricName [MAX_SENSOR_NAME_LEN];        // e.g. "CPUUtilization"
    sensorCounter counters [MAX_SENSOR_COUNTERS]; // array of values (not pointers, to simplify shared-memory region use)
    int countersLen;                              // size of the array
} sensorMetric;

typedef struct {
    char resourceName [MAX_SENSOR_NAME_LEN];   // e.g. "i-1234567"
    char resourceType [MAX_SENSOR_NAME_LEN];   // e.g. "instance"
    sensorMetric metrics [MAX_SENSOR_METRICS]; // array of values (not pointers, to simplify shared-memory region use)
    int metricsLen;                            // size of the array
} sensorResource;

int sensor_init (void);
int sensor_config (int new_history_size, long long new_collection_interval_time_ms);
int sensor_str2type (const char * counterType);
const char * sensor_type2str (int type);
int sensor_res2str (char * buf, int bufLen, const sensorResource **res, int resLen);
int sensor_set_instance_data (const char * instanceId, const char ** sensorIds, int sensorIdsLen, sensorResource * sr);

#endif

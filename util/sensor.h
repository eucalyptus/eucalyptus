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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

#ifndef _INCLUDE_SENSOR_H_
#define _INCLUDE_SENSOR_H_

//!
//! @file util/sensor.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include "ipc.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifndef _UNIT_TEST
#define MAX_SENSOR_NAME_LEN                      64
#define MAX_SENSOR_VALUES                        15 //!< by default 10 on CLC
#define MAX_SENSOR_DIMENSIONS                    (5 + EUCA_MAX_VOLUMES) //!< root, ephemeral[0-1], vol-XYZ
#define MAX_SENSOR_COUNTERS                      2  //!< we only have two types of counters (summation|average) for now
#define MAX_SENSOR_METRICS                       12 //!< currently 11 are implemented
#else /* ! _UNIT_TEST */
#define MAX_SENSOR_NAME_LEN                      64
#define MAX_SENSOR_VALUES                         5 // smaller sizes, for easier testing of limits
#define MAX_SENSOR_DIMENSIONS                     3
#define MAX_SENSOR_COUNTERS                       1
#define MAX_SENSOR_METRICS                        2
#endif /* ! _UNIT_TEST */

#define DEFAULT_SENSOR_SLEEP_DURATION_USEC       15000000L
#define MIN_COLLECTION_INTERVAL_MS                   1000L  //!< below 1 second is too frequent
#define MAX_COLLECTION_INTERVAL_MS               86400000L  //!< above 24 hours is too infrequent
#define MAX_SENSOR_RESOURCES_HARD                10000000L  //!< 10 mil resources max, for sanity checking

//! Sensor resources that have not been updated in this multiple of the
//! upstream polling interval will be expired from the cache.
#define CACHE_EXPIRY_MULTIPLE_OF_POLLING_INTERVAL 3

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Sensor counter type enumeration
typedef enum {
    SENSOR_UNUSED = 0,
    SENSOR_SUMMATION,
    SENSOR_AVERAGE
} sensorCounterType;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Sensor value structure
typedef struct {
    long long timestampMs;             //!< in milliseconds
    double value;                      //!< measurement
    char available;                    //!< if '1' then value is valid, otherwise it is not
} sensorValue;

//! Sensor dimension structure
typedef struct {
    char dimensionName[MAX_SENSOR_NAME_LEN];    //!< e.g. "default", "root", "vol-123ABC"
    char dimensionAlias[MAX_SENSOR_NAME_LEN];   //!< e.g. "sda1", "vda", "sdc"
    long long sequenceNum;             //!< num of first value in values[], starts with 0 when sensor is reset
    sensorValue values[MAX_SENSOR_VALUES];  //!< array of values (not pointers, to simplify shared-memory region use)
    int valuesLen;                     //!< size of the array
    int firstValueIndex;               //!< index into values[] of the first value (one that matches sequenceNum)
    double shift_value;                // amount that should be added to all values at this dimension
} sensorDimension;

//! Sensor counter structure
typedef struct {
    sensorCounterType type;
    long long collectionIntervalMs;    //!< the spacing of values, based on sensor's configuration
    sensorDimension dimensions[MAX_SENSOR_DIMENSIONS];  //!< array of values (not pointers, to simplify shared-memory region use)
    int dimensionsLen;                 //!< size of the array
} sensorCounter;

//! Sensor metric structure
typedef struct {
    char metricName[MAX_SENSOR_NAME_LEN];   //!< e.g. "CPUUtilization"
    sensorCounter counters[MAX_SENSOR_COUNTERS];    //!< array of values (not pointers, to simplify shared-memory region use)
    int countersLen;                   //!< size of the array
} sensorMetric;

//! Sensor resource structure
typedef struct {
    char resourceName[MAX_SENSOR_NAME_LEN]; //!< e.g. "i-1234567"
    char resourceAlias[MAX_SENSOR_NAME_LEN];    //!< e.g. "123.45.67.89" (its private IP address)
    char resourceType[10];             //!< e.g. "instance"
    char resourceUuid[64];             //!< e.g. "550e8400-e29b-41d4-a716-446655443210"
    sensorMetric metrics[MAX_SENSOR_METRICS];   //!< array of values (not pointers, to simplify shared-memory region use)
    int metricsLen;                    //!< size of the array
    int timestamp;                     // timestamp for last receipt of metrics
} sensorResource;

//! Sensor resource cache structure
typedef struct {
    long long collection_interval_time_ms;
    int history_size;
    boolean initialized;
    boolean suspend_polling;
    int max_resources;
    int used_resources;
    time_t last_polled;
    time_t interval_polled;
    sensorResource resources[1];       //!< if struct should be allocated with extra space after it for additional cache elements
} sensorResourceCache;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Sensor counter type names matching the enum
extern const char *sensorCounterTypeName[];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int sensor_init(sem * sem, sensorResourceCache * resources, int resources_size, boolean run_bottom_half, int (*update_euca_config_function) (void));
int sensor_suspend_polling(void);
int sensor_resume_polling(void);
int sensor_config(int new_history_size, long long new_collection_interval_time_ms);
int sensor_set_hyp_sem(sem * sem);
int sensor_get_config(int *history_size, long long *collection_interval_time_ms);
int sensor_get_num_resources(void);
sensorCounterType sensor_str2type(const char *counterType);
const char *sensor_type2str(sensorCounterType type);
int sensor_res2str(char *buf, int bufLen, sensorResource ** srs, int srsLen);
int sensor_get_dummy_instance_data(long long sn, const char *instanceId, const char **sensorIds, int sensorIdsLen, sensorResource ** srs, int srsLen);
int sensor_merge_records(sensorResource * srs[], int srsLen, boolean fail_on_oom);
int sensor_add_value(const char *instanceId, const char *metricName, const int counterType, const char *dimensionName, const long long sequenceNum,
                     const long long timestampMs, const boolean available, const double value);
int sensor_get_value(const char *instanceId, const char *metricName, const int counterType, const char *dimensionName, long long *sequenceNum,
                     long long *timestampMs, boolean * available, double *value, long long *intervalMs, int *valLen);
int sensor_get_instance_data(const char *instanceId, char **sensorIds, int sensorIdsLen, sensorResource ** sr_out, int srLen);
int sensor_add_resource(const char *resourceName, const char *resourceType, const char *resourceUuid);
int sensor_set_resource_alias(const char *resourceName, const char *resourceAlias);
int sensor_remove_resource(const char *resourceName);
int sensor_shift_metric(const char *resourceName, const char *metricName);
int sensor_set_dimension_alias(const char *resourceName, const char *metricName, const int counterType, const char *dimensionName, const char *dimensionAlias);
int sensor_set_volume(const char *instanceId, const char *volumeId, const char *guestDev);
int sensor_refresh_resources(char resourceNames[][MAX_SENSOR_NAME_LEN], char resourceAliases[][MAX_SENSOR_NAME_LEN], int size);
int sensor_validate_resources(sensorResource ** srs, int srsLen);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_SENSOR_H_ */

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

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h> // strlen, strcpy
#include <ctype.h> // isspace
#include <assert.h>
#include <stdarg.h>
#include <unistd.h> // usleep
#include <pthread.h> 
#include <assert.h>

#include "eucalyptus.h"
#include "misc.h"
#include "sensor.h"
#include "ipc.h"

#define MAX_SENSOR_RESOURCES MAXINSTANCES

static useconds_t next_sleep_duration_usec = DEFAULT_SENSOR_SLEEP_DURATION_USEC;
static sensorResourceCache * sensor_state = NULL;
static sem * state_sem = NULL;

// Never-returning function that performs polling of sensors and updates 
// their 'resources' while holding the 'sem'. This may be called from 
// sensor_init() directly or via a thread
static void sensor_bottom_half (void)
{        
    assert(sensor_state!=NULL && state_sem!=NULL);

    for (;;) {
        usleep (next_sleep_duration_usec);

        boolean skip = FALSE;
        sem_p (state_sem);
        if (sensor_state->collection_interval_time_ms == 0 ||
            sensor_state->history_size == 0) {
            skip = TRUE;
        }
        sem_v (state_sem);
        
        if (skip)
            continue;

        logprintfl (EUCADEBUG, "sensor bottom half polling sensors...\n");

    }
    
}

static void * sensor_thread (void *arg) 
{
    logprintfl (EUCADEBUG, "spawning sensor thread\n");
    sensor_bottom_half ();
    return NULL;
}

static void init_state (int resources_size)
{
    logprintfl (EUCADEBUG, "initializing sensor shared memory (%d KB)...\n",
                (sizeof(sensorResourceCache)+sizeof(sensorResource)*(resources_size-1))/1024);
    sensor_state->max_resources = resources_size;
    sensor_state->collection_interval_time_ms = 0;
    sensor_state->history_size = 0;
    for (int i; i<resources_size; i++) {
        sensorResource * sr = sensor_state->resources + i;
        bzero (sr, sizeof (sensorResource));
    }
    sensor_state->initialized = TRUE; // inter-process init done
    logprintfl (EUCADEBUG, "initialized sensor shared memory\n");
}

// Sensor subsystem initialization routine, which must be called before 
// all state-full sensor_* functions. If 'sem' and 'resources' are set,
// this function will use them. Otherwise, this function will allocate 
// memory and will perform data collection in a thread. If run_bottom_half
// is TRUE, the logic normally running in a background thread will be
// executed synchronously, causing sensor_init() to never return.
int sensor_init (sem * sem, sensorResourceCache * resources, int resources_size, boolean run_bottom_half)
{
    int use_resources_size = MAX_SENSOR_RESOURCES;

    if (sem || resources) { // we will use an externally allocated semaphore and memory region
        if (sem==NULL || resources==NULL || resources_size<1) { // all must be set
            return ERROR;
        }

        if (sensor_state != NULL) { // already invoked this in this process
            if (sensor_state != resources || state_sem != sem) { // but with different params?!
                return ERROR;
            } else {
                return OK;
            }
        } else { // first invocation in this process, so set the static pointers
            sensor_state = resources;
            state_sem = sem;            
        }

        // if this process is the first to get to global state, initialize it
        sem_p (state_sem);
        if (!sensor_state->initialized) {
            init_state (resources_size);
        } 
        sem_v (state_sem);

        if (! run_bottom_half)
            return OK;

        sensor_bottom_half (); // never to return

    } else { // we will allocate a memory region and a semaphore in this process
        if (resources_size>0) {
            use_resources_size = resources_size;
        }

        if (sensor_state != NULL || state_sem != NULL) // already initialized
            return OK;
    
        state_sem = sem_alloc (1, "mutex");
        if (state_sem==NULL) {
            logprintfl (EUCAFATAL, "failed to allocate semaphore for sensor\n");
            return ERROR_FATAL;
        }
        
        sensor_state = malloc (sizeof(sensorResourceCache)+sizeof(sensorResource)*(use_resources_size-1));
        if (sensor_state==NULL) {
            logprintfl (EUCAFATAL, "failed to allocate memory for sensor data\n");
            sem_free (state_sem);
            return ERROR_FATAL;
        }

        init_state (use_resources_size);

        { // start the sensor thread
            pthread_t tcb;
            if (pthread_create (&tcb, NULL, sensor_thread, NULL)) {
                logprintfl (EUCAFATAL, "failed to spawn a sensor thread\n");
                return ERROR_FATAL;
            }
            if (pthread_detach (tcb)) {
                logprintfl (EUCAFATAL, "failed to detach the sensor thread\n");
                return ERROR_FATAL;
            }
        }
    }
    
    return OK;
}

int sensor_config (int new_history_size, long long new_collection_interval_time_ms)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;
    if (new_history_size < 0) return 2; // nonsense value
    if (new_history_size > MAX_SENSOR_VALUES) return 3; // static data struct too small
    if (new_collection_interval_time_ms < MIN_COLLECTION_INTERVAL_MS) return 4;
    if (new_collection_interval_time_ms > MAX_COLLECTION_INTERVAL_MS) return 5;

    sem_p (state_sem);
    if (sensor_state->history_size != new_history_size)
        logprintfl (EUCAINFO, "setting sensor history size to %d\n", new_history_size);
    if (sensor_state->collection_interval_time_ms != new_collection_interval_time_ms)
        logprintfl (EUCAINFO, "setting sensor collection interval time to %lld\n", new_collection_interval_time_ms);
    sensor_state->history_size = new_history_size;
    sensor_state->collection_interval_time_ms = new_collection_interval_time_ms;
    sem_v (state_sem);
    
    return 0;
}

int sensor_str2type (const char * counterType)
{
    for (int i=0; i<(sizeof (sensorCounterTypeName) / sizeof (char *)); i++) {
        if (strcmp (sensorCounterTypeName[i], counterType) == 0)
            return i;
    }
    logprintfl (EUCAERROR, "internal error (sensor counter type out of range)\n");
    return -1;
}

const char * sensor_type2str (int type)
{
    if (type>=0 && type<(sizeof (sensorCounterTypeName) / sizeof (char *)))
        return sensorCounterTypeName[type];
    else
        return "[invalid]";
}

int sensor_res2str (char * buf, int bufLen, sensorResource **res, int resLen)
{
    char * s = buf;
    int left = bufLen-1;
    int printed;

    for (int r=0; r<resLen; r++) {
        const sensorResource * sr = res [r];
        printed = snprintf (s, left, "resource: %s type: %s metrics: %d\n", sr->resourceName, sr->resourceType, sr->metricsLen);
#define MAYBE_BAIL s = s + printed; left = left - printed; if (left < 1) return (bufLen - left);
        MAYBE_BAIL
        for (int m=0; m<sr->metricsLen; m++) {
            const sensorMetric * sm = sr->metrics + m;
            printed = snprintf (s, left, "\tmetric: %s counters: %d\n", sm->metricName, sm->countersLen); 
            MAYBE_BAIL
            for (int c=0; c<sm->countersLen; c++) {
                const sensorCounter * sc = sm->counters + c;
                printed = snprintf (s, left, "\t\tcounter: %s interval: %lld seq: %lld dimensions: %d\n", 
                                    sensor_type2str(sc->type), sc->collectionIntervalMs, sc->sequenceNum, sc->dimensionsLen);
                MAYBE_BAIL
                for (int d=0; d<sc->dimensionsLen; d++) {
                    const sensorDimension * sd = sc->dimensions + d;
                    printed = snprintf (s, left, "\t\t\tdimension: %s values: %d\n", sd->dimensionName, sd->valuesLen);
                    MAYBE_BAIL
                    for (int v=0; v<sd->valuesLen; v++) {
                        const sensorValue * sv = sd->values + v;
                        printed = snprintf (s, left, "\t\t\t\t%lld %s %f\n", sv->timestampMs, sv->available?"YES":" NO", sv->available?sv->value:-1);
                        MAYBE_BAIL
                    }
                }
            }
        }
    }
    * s = '\0';

    return 0;
}

int sensor_set_instance_data (const char * instanceId, const char ** sensorIds, int sensorIdsLen, sensorResource * sr) // TODO3.2: actually implement the function
{
    sensorResource example = { 
        .resourceName = "i-23456",
        .resourceType = "instance",
        .metricsLen = 2,
        .metrics = { 
            {
                .metricName = "CPUUtilization",
                .countersLen = 1,
                .counters = {
                    {
                        .type = SENSOR_AVERAGE,
                        .collectionIntervalMs = 20000,
                        .sequenceNum = 0,
                        .dimensionsLen = 1,
                        .dimensions = {
                            {
                                .dimensionName = "default",
                                .valuesLen = 5,
                                .values = {
                                    { .timestampMs = 1344056910424, .value = 33.3, .available = 1 },
                                    { .timestampMs = 1344056930424, .value = 34.7, .available = 1 },
                                    { .timestampMs = 1344056950424, .value = 31.1, .available = 1 },
                                    { .timestampMs = 1344056970424, .value = -999, .available = 0 },
                                    { .timestampMs = 1344056990424, .value = 39.9, .available = 1 },
                                }
                            }
                        }
                    }
                }
            },
            {
                .metricName = "DiskReadOps",
                .countersLen = 1,
                .counters = {
                    {
                        .type = SENSOR_SUMMATION,
                        .collectionIntervalMs = 20000,
                        .sequenceNum = 0,
                        .dimensionsLen = 3,
                        .dimensions = {
                            {
                                .dimensionName = "root",
                                .valuesLen = 3,
                                .values = {
                                    { .timestampMs = 1344056910424, .value = 0.0, .available = 1 },
                                    { .timestampMs = 1344056930424, .value = 111.0, .available = 1 },
                                    { .timestampMs = 1344056950424, .value = 2222222.0, .available = 1 },
                                }
                            },
                            {
                                .dimensionName = "ephemeral0",
                                .valuesLen = 3,
                                .values = {
                                    { .timestampMs = 1344056910424, .value = 0.0, .available = 1 },
                                    { .timestampMs = 1344056930424, .value = 0.0, .available = 1 },
                                    { .timestampMs = 1344056950424, .value = 3333333.0, .available = 1 },
                                }
                            },
                            {
                                .dimensionName = "vol-34567",
                                .valuesLen = 3,
                                .values = {
                                    { .timestampMs = 1344056910424, .value = 0.0, .available = 1 },
                                    { .timestampMs = 1344056930424, .value = 44444.0, .available = 1 },
                                    { .timestampMs = 1344056950424, .value = 55555555.0, .available = 1 },
                                }
                            }
                        }
                    }
                }
            } 
        }
    };
    memcpy (sr, &example, sizeof(sensorResource));
    strncpy (sr->resourceName, instanceId, sizeof(sr->resourceName));

    return 0;
}

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

//!
//! @file util/sensor.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h>                    // strlen, strcpy
#include <ctype.h>                     // isspace
#include <assert.h>
#include <stdarg.h>
#include <unistd.h>                    // usleep
#include <pthread.h>
#include <assert.h>
#include <errno.h>

#include "eucalyptus.h"
#include "misc.h"
#include "sensor.h"
#include "ipc.h"
#include "euca_string.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAX_SENSOR_RESOURCES                     MAXINSTANCES_PER_CC    //!< used for resource name cache
#define SENSOR_SYSTEM_POLL_INTERVAL_MINIMUM_USEC 5000000    //!< never poll system more often than this

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! an internal struct for temporary storage of stats
typedef struct getstat_t {
    char instanceId[100];
    long long timestamp;
    char metricName[100];
    int counterType;
    char dimensionName[100];
    double value;
    struct getstat_t *next;
} getstat;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef _UNIT_TEST
const char *euca_this_component_name = "ignore";
const char *euca_client_component_name = "ignore";
#endif /* _UNIT_TEST */

//! Sensor counter type names matching the enum
const char *sensorCounterTypeName[] = {
    "[unused]",
    "summation",
    "average",
    "latest"
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static useconds_t next_sleep_duration_usec = DEFAULT_SENSOR_SLEEP_DURATION_USEC;
static sensorResourceCache *sensor_state = NULL;
static sem *state_sem = NULL;
static sem *hyp_sem = NULL;
static int (*sensor_update_euca_config) (void) = NULL;
static long long seq_num = 0L;

#ifdef _UNIT_TEST
static long long ts = 0;
static void *competitor_function_writer(void *ptr);
static void *competitor_function_reader(void *ptr);
static long long _sn = -1;
static double val = 0.0;
#endif /* _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void getstat_free(getstat ** stats);
static getstat *getstat_find(getstat ** stats, const char *instanceId);
static int getstat_ninstances(getstat ** stats);
static int getstat_generate(getstat *** pstats);
static void sensor_bottom_half(void);
static void *sensor_thread(void *arg);
static void init_state(int resources_size);
static __inline__ boolean is_empty_sr(const sensorResource * sr);
static int sensor_expire_cache_entries(void);
#ifdef _UNIT_TEST
static void log_sensor_resources(const char *name, sensorResource ** srs, int srsLen);
#endif /* _UNIT_TEST */
static sensorResource *find_or_alloc_sr(const boolean do_alloc, const char *resourceName, const char *resourceType, const char *resourceUuid);
static sensorMetric *find_or_alloc_sm(const boolean do_alloc, sensorResource * sr, const char *metricName);
static sensorCounter *find_or_alloc_sc(const boolean do_alloc, sensorMetric * sm, const sensorCounterType counterType);
static sensorDimension *find_or_alloc_sd(const boolean do_alloc, sensorCounter * sc, const char *dimensionName);

#ifdef _UNIT_TEST
static void dump_sensor_cache(void);
static void clear_srs(sensorResource ** srs, int srsLen);
static void *competitor_function_reader(void *ptr);
static void *competitor_function_writer(void *ptr);
#endif /* _UNIT_TEST */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Frees the 'stats' array and the linked lists (of stat values) that
//! array entries point to.
//!
//! @param[in] stats
//!
static void getstat_free(getstat ** stats)
{
    if (stats == NULL)
        return;

    getstat *gs;
    for (int i = 0; (gs = stats[i]) != NULL; i++) {
        getstat *gs_next;
        for (; gs != NULL; gs = gs_next) {
            gs_next = gs->next;
            EUCA_FREE(gs);
        }
    }
    EUCA_FREE(stats);
}

//!
//! Looks for a resource in the stats[] array and returns a pointer to it
//!
//! @param[in] stats array of pointers to getstat results in a linked list
//! @param[in] resource name of the resource to find in the stats[] array
//!
//! @return a pointer to the stats structure
//!
static getstat *getstat_find(getstat ** stats, const char *resource)
{
    getstat *gs = NULL;

    if (stats) {
        for (int i = 0; (gs = stats[i]) != NULL; i++) {
            if (resource == NULL)      // special case, for testing, return first thing in the list
                break;
            if (strcmp(gs->instanceId, resource) == 0)
                break;
        }
    }

    return gs;
}

//!
//!
//!
//! @param[in] stats
//!
//! @return number of instances
//!
static int getstat_ninstances(getstat ** stats)
{
    int ninstances = 0;

    if (stats) {
        for (int i = 0; stats[i] != NULL; i++) {
            ninstances++;
        }
    }

    return ninstances;
}

//!
//! Adds all values in a linked list to sensor memory
//!
//! @param[in] name
//! @param[in] head of a linked list of getstat values
//!
//! @return number of values added
//!
static int getstat_add_values(const char *name, getstat * head)
{
    int nvalues = 0;

    for (getstat * s = head; s != NULL; s = s->next) {
        sensor_add_value(name, s->metricName, s->counterType, s->dimensionName, seq_num, s->timestamp, TRUE, s->value);
        nvalues++;
    }

    return nvalues;
}

//!
//! obtain stats from the getstats script
//!
//! @param[in,out] pstats
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
static int getstat_generate(getstat *** pstats)
{
    assert(sensor_state != NULL && state_sem != NULL);

    errno = 0;
    char *output = NULL;
    if (!strcmp(euca_this_component_name, "cc")) {
        char *instroot = NULL;
        char getstats_cmd[EUCA_MAX_PATH] = "";

        if (euca_sanitize_path(getenv(EUCALYPTUS_ENV_VAR_NAME)) == EUCA_OK) {
            instroot = strdup(getenv(EUCALYPTUS_ENV_VAR_NAME));
            snprintf(getstats_cmd, EUCA_MAX_PATH, EUCALYPTUS_LIBEXEC_DIR "/euca_rootwrap " EUCALYPTUS_DATA_DIR "/getstats_net.pl", instroot, instroot);
            EUCA_FREE(instroot);
        } else {
            snprintf(getstats_cmd, EUCA_MAX_PATH, EUCALYPTUS_LIBEXEC_DIR "/euca_rootwrap " EUCALYPTUS_DATA_DIR "/getstats_net.pl", "", "");
        }

        output = system_output(getstats_cmd);   // invoke th Perl script
        LOGTRACE("getstats_net.pl output:\n%s\n", output);
    } else if (!strcmp(euca_this_component_name, "nc")) {
        // Right now !CC means the NC.
        output = system_output("euca_rootwrap getstats.pl");    // invoke th Perl script
        LOGTRACE("getstats.pl output:\n%s\n", output);
    } else {
        // output will be NULL, so we'll return with an error
        errno = EBADSLT;               // using an obscure errno to mean internal error
    }

    int ret = EUCA_ERROR;
    if (output) {                      // output is a string with one line per measurement, with tab-delimited fields
        char *token, *subtoken;
        char *saveptr1, *saveptr2;
        char *str1 = output;
        getstat **gss = NULL;
        int ninst = 0;

        for (int i = 1;; i++, str1 = NULL) {    // iterate over lines in output
            token = strtok_r(str1, "\n", &saveptr1);    // token points to a whole line
            if (token == NULL)
                break;
            getstat *gs = EUCA_ZALLOC(1, sizeof(getstat));  // new lines means new data record
            if (gs == NULL)
                goto bail;

            char *str2 = token;
            for (int j = 1;; j++, str2 = NULL) {    // iterate over tab-separated entries in the line
                subtoken = strtok_r(str2, "\t", &saveptr2);
                if (subtoken == NULL) {
                    if (j == 1)
                        EUCA_FREE(gs);
                    break;
                }
                // e.g. line: i-760B43A1      1347407243789   NetworkIn       summation       total   2112765752
                switch (j) {
                case 1:{              // first entry is instance ID
                        getstat *gsp = getstat_find(*pstats, subtoken);
                        if (gsp == NULL) {  // first record for this instance => expand pointer array
                            ninst++;
                            gss = EUCA_REALLOC(gss, (ninst + 1), sizeof(getstat *));
                            gss[ninst - 1] = gs;
                            gss[ninst] = NULL;  // NULL-terminate the array
                            *pstats = gss;
                        } else {       // not first record
                            for (; gsp->next != NULL; gsp = gsp->next) ;    // walk the linked list to the end
                            gsp->next = gs; // add the new record
                        }
                        euca_strncpy(gs->instanceId, subtoken, sizeof(gs->instanceId));
                        break;
                    }
                case 2:{
                        char *endptr;
                        errno = 0;
                        gs->timestamp = strtoll(subtoken, &endptr, 10);
                        if (errno != 0 && *endptr != '\0') {
                            LOGERROR("unexpected input from getstats.pl (could not convert timestamp with strtoll())\n");
                            goto bail;
                        }
                        break;
                    }
                case 3:
                    euca_strncpy(gs->metricName, subtoken, sizeof(gs->metricName));
                    break;
                case 4:
                    gs->counterType = sensor_str2type(subtoken);
                    break;
                case 5:
                    euca_strncpy(gs->dimensionName, subtoken, sizeof(gs->dimensionName));
                    break;
                case 6:{
                        char *endptr;
                        errno = 0;
                        gs->value = strtod(subtoken, &endptr);
                        if (errno != 0 && *endptr != '\0') {
                            LOGERROR("unexpected input from getstats.pl (could not convert value with strtod())\n");
                            goto bail;
                        }
                        break;
                    }
                default:
                    LOGERROR("unexpected input from getstats.pl (too many fields)\n");
                    goto bail;
                }
            }
        }
        ret = EUCA_OK;
        goto done;

bail:
        getstat_free(*pstats);

done:
        EUCA_FREE(output);
    } else {
        LOGWARN("failed to invoke getstats for sensor data (%s)\n", strerror(errno));
    }

    return ret;
}

//!
//! Never-returning function that performs polling of sensors and updates
//! their 'resources' while holding the 'sem'. This may be called from
//! sensor_init() directly or via a thread
//!
static void sensor_bottom_half(void)
{
    assert(sensor_state != NULL && state_sem != NULL);

    char resourceNames[MAX_SENSOR_RESOURCES][MAX_SENSOR_NAME_LEN];
    char resourceAliases[MAX_SENSOR_RESOURCES][MAX_SENSOR_NAME_LEN];
    for (int i = 0; i < MAX_SENSOR_RESOURCES; i++) {
        resourceNames[i][0] = '\0';
        resourceAliases[i][0] = '\0';
    }

    for (;;) {
        usleep(next_sleep_duration_usec);

        if (sensor_update_euca_config) {
            LOGTRACE("calling sensor_update_euca_config() after sleeping %u usec\n", next_sleep_duration_usec);
            sensor_update_euca_config();
        } else {
            LOGTRACE("NOT calling sensor_update_euca_config() after sleeping %u usec\n", next_sleep_duration_usec);
        }
        boolean skip = FALSE;
        sem_p(state_sem);
        if (sensor_state->collection_interval_time_ms == 0 || sensor_state->history_size == 0 || sensor_state->suspend_polling) {
            skip = TRUE;
        } else {
            next_sleep_duration_usec = sensor_state->collection_interval_time_ms * 1000;
        }
        sem_v(state_sem);

        if (skip)
            continue;

        // obtain the list of current resources and their aliases from the cache
        // (they had to have been added explicitly with sensor_add_resource)
        // and only query the OS for those resources/instances
        useconds_t start_usec = time_usec();
        sem_p(state_sem);
        for (int i = 0; i < sensor_state->max_resources && i < MAX_SENSOR_RESOURCES; i++) {
            euca_strncpy(resourceNames[i], sensor_state->resources[i].resourceName, MAX_SENSOR_NAME_LEN);
            euca_strncpy(resourceAliases[i], sensor_state->resources[i].resourceAlias, MAX_SENSOR_NAME_LEN);
            if (strlen(resourceNames[i]) && strlen(resourceAliases[i])) {
                LOGTRACE("Found alias '%s' for resource '%s'\n", resourceAliases[i], resourceNames[i]);
            }
        }

        sem_v(state_sem);

        // serialize invocation of sensor_refresh_resources with other hypervisor calls
        if (hyp_sem)
            sem_p(hyp_sem);
        sensor_refresh_resources(resourceNames, resourceAliases, MAX_SENSOR_RESOURCES);
        if (hyp_sem)
            sem_v(hyp_sem);

        useconds_t stop_usec = time_usec();

        // adjust the next sleep time to account for how long sensor refresh took
        next_sleep_duration_usec = next_sleep_duration_usec - (stop_usec - start_usec);
        if (next_sleep_duration_usec < SENSOR_SYSTEM_POLL_INTERVAL_MINIMUM_USEC)
            next_sleep_duration_usec = SENSOR_SYSTEM_POLL_INTERVAL_MINIMUM_USEC;
    }
}

//!
//!
//!
//! @param[in] arg
//!
//! @return Always return NULL
//!
static void *sensor_thread(void *arg)
{
    LOGDEBUG("spawning sensor thread\n");
    sensor_bottom_half();
    return NULL;
}

//!
//!
//!
//! @param[in] resources_size
//!
static void init_state(int resources_size)
{
    LOGDEBUG("initializing sensor shared memory (%lu KB)...\n", (sizeof(sensorResourceCache) + sizeof(sensorResource) * (resources_size - 1)) / 1024);
    sensor_state->max_resources = resources_size;
    sensor_state->collection_interval_time_ms = 0;
    sensor_state->history_size = 0;
    sensor_state->last_polled = 0;
    sensor_state->interval_polled = 0;
    for (int i = 0; i < resources_size; i++) {
        sensorResource *sr = sensor_state->resources + i;
        bzero(sr, sizeof(sensorResource));
    }
    sensor_state->initialized = TRUE;  // inter-process init done
    LOGINFO("initialized sensor shared memory\n");
}

//!
//! Checks wether or not a sensor resource is in use
//!
//! @param[in] sr pointer to the sensor resource to evaluate
//!
//! @return TRUE if the given sensor is not created/initialized or FALSE otherwise
//!
static __inline__ boolean is_empty_sr(const sensorResource * sr)
{
    return (sr == NULL || sr->resourceName[0] == '\0');
}

//!
//! This must be called from within a state_sem lock--it doesn't do its
//! own locking.
//!
//! @return the number of sensor that has their cache timeout expired
//!
static int sensor_expire_cache_entries(void)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return 1;

    LOGDEBUG("invoked\n");

    int ret = 0;                       // returns the number of cache entries expired.
    time_t t = time(NULL);

    for (int r = 0; r < sensor_state->max_resources; r++) {
        sensorResource *sr = sensor_state->resources + r;
        if (is_empty_sr(sr))
            continue;
        if (!sr->timestamp) {
            LOGDEBUG("resource %s does not yet have an update timestamp, skipping expiration...\n", sr->resourceName);
            continue;
        }
        time_t timestamp_age = t - sr->timestamp;   // time, in sec, elapsed since an update (to any sensor) for the resource
        time_t cache_timeout = sensor_state->collection_interval_time_ms / 1000 // expected time, in sec, between updates
            + sensor_state->interval_polled * CACHE_EXPIRY_MULTIPLE_OF_POLLING_INTERVAL;    // extra time for upstream to pick up last values before expiration

        LOGTRACE("resource %ss, timestamp %ds, poll interval %lds, timeout %lds, age %lds\n", sr->resourceName, sr->timestamp,
                 sensor_state->interval_polled, cache_timeout, timestamp_age);

        if (cache_timeout && (timestamp_age > cache_timeout)) {
            LOGINFO("expiring resource %s from sensor cache, no update in %ld seconds, timeout is %ld seconds\n", sr->resourceName, timestamp_age, cache_timeout);
            sr->resourceName[0] = '\0'; // marks the slot as empty
            ret++;
        }
    }
    return ret;
}

//!
//! Sensor subsystem initialization routine, which must be called before
//! all state-full sensor_* functions. If 'sem' and 'resources' are set,
//! this function will use them. Otherwise, this function will allocate
//! memory and will perform data collection in a thread. If run_bottom_half
//! is TRUE, the logic normally running in a background thread will be
//! executed synchronously, causing sensor_init() to never return.
//!
//! @param[in] sem
//! @param[in] resources
//! @param[in] resources_size
//! @param[in] run_bottom_half
//! @param[in] update_euca_config_function
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_THREAD_ERROR and EUCA_MEMORY_ERROR.
//!
int sensor_init(sem * sem, sensorResourceCache * resources, int resources_size, boolean run_bottom_half, int (*update_euca_config_function) (void))
{
    int use_resources_size = MAX_SENSOR_RESOURCES;

    if (sem || resources) {            // we will use an externally allocated semaphore and memory region
        if (sem == NULL || resources == NULL || resources_size < 1) {   // all must be set
            return (EUCA_ERROR);
        }

        if (sensor_state != NULL) {    // already invoked this in this process
            if (sensor_state != resources || state_sem != sem) {    // but with different params?!
                return (EUCA_ERROR);
            } else {
                return (EUCA_OK);
            }
        } else {                       // first invocation in this process, so set the static pointers
            sensor_state = resources;
            state_sem = sem;
        }

        // if this process is the first to get to global state, initialize it
        sem_p(state_sem);
        if (!sensor_state->initialized) {
            init_state(resources_size);
        }
        LOGDEBUG("setting sensor_update_euca_config: %s\n", update_euca_config_function ? "TRUE" : "NULL");
        sensor_update_euca_config = update_euca_config_function;
        sem_v(state_sem);

        if (!run_bottom_half)
            return (EUCA_OK);

        sensor_bottom_half();          // never to return

    } else {                           // we will allocate a memory region and a semaphore in this process
        if (resources_size > 0) {
            use_resources_size = resources_size;
        }

        if (sensor_state != NULL || state_sem != NULL)  // already initialized
            return (EUCA_OK);

        state_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
        if (state_sem == NULL) {
            LOGFATAL("failed to allocate semaphore for sensor\n");
            return (EUCA_MEMORY_ERROR);
        }

        sensor_state = EUCA_ZALLOC(sizeof(sensorResourceCache) + sizeof(sensorResource), (use_resources_size - 1));
        if (sensor_state == NULL) {
            LOGFATAL("failed to allocate memory for sensor data\n");
            SEM_FREE(state_sem);
            return (EUCA_MEMORY_ERROR);
        }

        init_state(use_resources_size);

        {                              // start the sensor thread
            pthread_t tcb;
            if (pthread_create(&tcb, NULL, sensor_thread, NULL)) {
                LOGFATAL("failed to spawn a sensor thread\n");
                return (EUCA_THREAD_ERROR);
            }
            if (pthread_detach(tcb)) {
                LOGFATAL("failed to detach the sensor thread\n");
                return (EUCA_THREAD_ERROR);
            }
        }
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] new_history_size
//! @param[in] new_collection_interval_time_ms
//!
//! @return EUCA_OK on success
//!
int sensor_config(int new_history_size, long long new_collection_interval_time_ms)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return 1;
    if (new_history_size < 0)
        return 2;                      // nonsense value
    if (new_history_size > MAX_SENSOR_VALUES)
        return 3;                      // static data struct too small
    if (new_collection_interval_time_ms < MIN_COLLECTION_INTERVAL_MS)
        return 4;
    if (new_collection_interval_time_ms > MAX_COLLECTION_INTERVAL_MS)
        return 5;

    sem_p(state_sem);
    if (sensor_state->history_size != new_history_size)
        LOGINFO("setting sensor history size to %d\n", new_history_size);
    if (sensor_state->collection_interval_time_ms != new_collection_interval_time_ms)
        LOGINFO("setting sensor collection interval time to %lld milliseconds\n", new_collection_interval_time_ms);
    sensor_state->history_size = new_history_size;
    sensor_state->collection_interval_time_ms = new_collection_interval_time_ms;
    sem_v(state_sem);

    return (EUCA_OK);
}

//!
//!
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int sensor_suspend_polling(void)
{
    if ((sensor_state == NULL) || (sensor_state->initialized == FALSE))
        return (EUCA_ERROR);

    sem_p(state_sem);
    {
        sensor_state->suspend_polling = TRUE;
    }
    sem_v(state_sem);

    LOGDEBUG("sensor polling suspended\n");
    return (EUCA_OK);
}

//!
//!
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int sensor_resume_polling(void)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    sem_p(state_sem);
    {
        sensor_state->suspend_polling = FALSE;
    }
    sem_v(state_sem);

    LOGDEBUG("sensor polling resumed\n");
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] sem
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int sensor_set_hyp_sem(sem * sem)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    sem_p(state_sem);
    hyp_sem = sem;
    sem_v(state_sem);

    return (EUCA_OK);
}

//!
//!
//!
//! @param[out] history_size
//! @param[out] collection_interval_time_ms
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int sensor_get_config(int *history_size, long long *collection_interval_time_ms)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    sem_p(state_sem);
    *history_size = sensor_state->history_size;
    *collection_interval_time_ms = sensor_state->collection_interval_time_ms;
    sem_v(state_sem);

    return (EUCA_OK);
}

//!
//! Retrieves the number of used resources
//!
//! @return the  number of used resources
//!
int sensor_get_num_resources(void)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return -1;

    int num_resources;
    sem_p(state_sem);
    num_resources = sensor_state->used_resources;
    sem_v(state_sem);

    return num_resources;
}

//!
//! Converts a readable string to a sensor counter type
//!
//! @param[in] counterType the string representation of a counter type value
//!
//! @return The matching counter type value or -1 if the parameter is invalid
//!
sensorCounterType sensor_str2type(const char *counterType)
{
    for (u_int i = 0; i < (sizeof(sensorCounterTypeName) / sizeof(char *)); i++) {
        if (strcmp(sensorCounterTypeName[i], counterType) == 0)
            return i;
    }
    LOGERROR("internal error (sensor counter type out of range)\n");
    return -1;
}

//!
//! Converts a sensor counter type to readable string
//!
//! @param[in] type the sensor type
//!
//! @return a string matching the sensor type
//!
const char *sensor_type2str(sensorCounterType type)
{
    if ((((signed)type) >= 0) && (type < (sizeof(sensorCounterTypeName) / sizeof(char *))))
        return (sensorCounterTypeName[type]);
    return ("[invalid]");
}

//!
//!
//!
//! @param[in] buf
//! @param[in] bufLen
//! @param[in] srs
//! @param[in] srsLen
//!
//! @return EUCA_OK on success
//!
int sensor_res2str(char *buf, int bufLen, sensorResource ** srs, int srsLen)
{
    char *s = buf;
    int left = bufLen - 1;
    int printed;

    for (int r = 0; r < srsLen; r++) {
        const sensorResource *sr = srs[r];
        if (is_empty_sr(sr))
            continue;
        printed = snprintf(s, left, "resource: %s uuid: %s type: %s metrics: %d\n", sr->resourceName, sr->resourceUuid, sr->resourceType, sr->metricsLen);
#define MAYBE_BAIL s = s + printed; left = left - printed; if (left < 1) return (bufLen - left);
        MAYBE_BAIL for (int m = 0; m < sr->metricsLen; m++) {
            const sensorMetric *sm = sr->metrics + m;
            printed = snprintf(s, left, "\tmetric: %s counters: %d\n", sm->metricName, sm->countersLen);
            MAYBE_BAIL for (int c = 0; c < sm->countersLen; c++) {
                const sensorCounter *sc = sm->counters + c;
                printed = snprintf(s, left, "\t\tcounter: %s interval: %lld dimensions: %d\n", sensor_type2str(sc->type), sc->collectionIntervalMs, sc->dimensionsLen);
                MAYBE_BAIL for (int d = 0; d < sc->dimensionsLen; d++) {
                    const sensorDimension *sd = sc->dimensions + d;
                    printed =
                        snprintf(s, left, "\t\t\tdimension: %s values: %d seq: %lld firstValueIndex: %d\n", sd->dimensionName, sd->valuesLen, sd->sequenceNum, sd->firstValueIndex);
                    MAYBE_BAIL for (int v = 0; v < sd->valuesLen; v++) {
                        const int i = (sd->firstValueIndex + v) % MAX_SENSOR_VALUES;
                        const sensorValue *sv = sd->values + i;
                        const long long sn = sd->sequenceNum + v;
                        printed = snprintf(s, left, "\t\t\t\t[%02d] %05lld %014lld %s %f\n", i, sn, sv->timestampMs, sv->available ? "YES" : " NO", sv->available ? sv->value : -1);
                    MAYBE_BAIL}
                }
            }
        }
    }
    *s = '\0';

    return EUCA_OK;
}

#ifdef _UNIT_TEST
//!
//!
//!
//! @param[in] name
//! @param[in] srs
//! @param[in] srsLen
//!
static void log_sensor_resources(const char *name, sensorResource ** srs, int srsLen)
{
    char buf[1024 * 1024];
    if (sensor_res2str(buf, sizeof(buf), srs, srsLen) != 0) {
        LOGERROR("failed to print sensor resources (%s)\n", name);
    } else {
        LOGDEBUG("sensor resources (%s) BEGIN\n%ssensor resources END\n", name, buf);
    }
}
#endif /* _UNIT_TEST */

//!
//!
//!
//! @param[in] sn
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] sensorIds
//! @param[in] sensorIdsLen
//! @param[in] srs
//! @param[in] srsLen
//!
//! @return EUCA_OK on success
//!
int sensor_get_dummy_instance_data(long long sn, const char *instanceId, const char **sensorIds, int sensorIdsLen, sensorResource ** srs, int srsLen)   //! @TODO 3.2: move this into _UNIT_TEST
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
                                   .dimensionsLen = 1,
                                   .dimensions = {
                                                  {
                                                   .dimensionName = "default",
                                                   .sequenceNum = 0,
                                                   .valuesLen = 5,
                                                   .values = {
                                                              {.timestampMs = 1344056910424,.value = 33.3,.available = 1},
                                                              {.timestampMs = 1344056930424,.value = 34.7,.available = 1},
                                                              {.timestampMs = 1344056950424,.value = 31.1,.available = 1},
                                                              {.timestampMs = 1344056970424,.value = 666,.available = 1},
                                                              {.timestampMs = 1344056990424,.value = 39.9,.available = 1},
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
                                   .dimensionsLen = 3,
                                   .dimensions = {
                                                  {
                                                   .dimensionName = "root",
                                                   .sequenceNum = sn,
                                                   .valuesLen = 3,
                                                   .values = {
                                                              {.timestampMs = 1344056910424 + sn,.value = 111.0 + sn,.available = 1},
                                                              {.timestampMs = 1344056910425 + sn,.value = 112.0 + sn,.available = 1},
                                                              {.timestampMs = 1344056910426 + sn,.value = 113.0 + sn,.available = 1},
                                                              }
                                                   },
                                                  {
                                                   .dimensionName = "ephemeral0",
                                                   .sequenceNum = sn,
                                                   .valuesLen = 3,
                                                   .values = {
                                                              {.timestampMs = 1344056910424 + sn,.value = 1111.0 + sn,.available = 1},
                                                              {.timestampMs = 1344056910425 + sn,.value = 1112.0 + sn,.available = 1},
                                                              {.timestampMs = 1344056910426 + sn,.value = 1113.0 + sn,.available = 1},
                                                              }
                                                   },
                                                  {
                                                   .dimensionName = "vol-34567",
                                                   .sequenceNum = sn,
                                                   .valuesLen = 3,
                                                   .values = {
                                                              {.timestampMs = 1344056910424 + sn,.value = 11111.0 + sn,.available = 1},
                                                              {.timestampMs = 1344056910425 + sn,.value = 11112.0 + sn,.available = 1},
                                                              {.timestampMs = 1344056910426 + sn,.value = 11113.0 + sn,.available = 1},
                                                              }
                                                   }
                                                  }
                                   }
                                  }
                     }
                    }
    };
    assert(srsLen > 0);
    sensorResource *sr = srs[0];
    memcpy(sr, &example, sizeof(sensorResource));
    euca_strncpy(sr->resourceName, instanceId, sizeof(sr->resourceName));

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] do_alloc
//! @param[in] resourceName
//! @param[in] resourceType
//! @param[in] resourceUuid
//!
//! @return a pointer to the sensor resource or NULL on failure
//!
static sensorResource *find_or_alloc_sr(const boolean do_alloc, const char *resourceName, const char *resourceType, const char *resourceUuid)
{
    // sanity check
    if (sensor_state->max_resources < 0 || sensor_state->max_resources > MAX_SENSOR_RESOURCES_HARD) {
        LOGERROR("inconsistency in sensor database (max_resources=%d for %s)\n", sensor_state->max_resources, resourceName);
        return NULL;
    }

    sensorResource *unused_sr = NULL;
    for (int r = 0; r < sensor_state->max_resources; r++) {
        sensorResource *sr = sensor_state->resources + r;

        if (is_empty_sr(sr)) {         // unused slot
            // remember the first unused slot in case we do not find this resource
            if (unused_sr == NULL) {
                unused_sr = sr;
            }
            continue;
        }
        // we have a match
        if ((strcmp(sr->resourceName, resourceName) == 0) || (strcmp(sr->resourceAlias, resourceName) == 0)) {
            if (resourceType) {
                if (strcmp(sr->resourceType, resourceType) == 0) {
                    return sr;
                }
            }
            return sr;
        }
    }

    if (!do_alloc)
        return NULL;
    if (resourceType == NULL)          // must be set for allocation
        return NULL;

    // fill out the new slot
    if (unused_sr != NULL) {
        bzero(unused_sr, sizeof(sensorResource));
        euca_strncpy(unused_sr->resourceName, resourceName, sizeof(unused_sr->resourceName));
        if (resourceType)
            euca_strncpy(unused_sr->resourceType, resourceType, sizeof(unused_sr->resourceType));
        if (resourceUuid)
            euca_strncpy(unused_sr->resourceUuid, resourceUuid, sizeof(unused_sr->resourceUuid));
        unused_sr->timestamp = time(NULL);
        sensor_state->used_resources++;
        LOGINFO("allocated new sensor resource %s\n", resourceName);
    }

    return unused_sr;
}

//!
//!
//!
//! @param[in] do_alloc
//! @param[in] sr
//! @param[in] metricName
//!
//! @return a pointer to the sensor metric or NULL on failure
//!
static sensorMetric *find_or_alloc_sm(const boolean do_alloc, sensorResource * sr, const char *metricName)
{
    // sanity check
    if (sr->metricsLen < 0 || sr->metricsLen > MAX_SENSOR_METRICS) {
        LOGWARN("inconsistency in sensor database (metricsLen=%d for %s)\n", sr->metricsLen, sr->resourceName);
        char trace[8172] = "";         // print stack trace to see which invocation led to this erroneous condition
        log_dump_trace(trace, sizeof(trace));
        LOGTRACE("%s", trace);
        return NULL;
    }

    for (int m = 0; m < sr->metricsLen; m++) {
        sensorMetric *sm = sr->metrics + m;
        if (strcmp(sm->metricName, metricName) == 0) {
            return sm;
        }
    }
    if (!do_alloc                      // did not find it
        || sr->metricsLen == MAX_SENSOR_METRICS)    // out of room
        return NULL;

    // fill out the new slot
    sensorMetric *sm = sr->metrics + sr->metricsLen;
    bzero(sm, sizeof(sensorMetric));
    euca_strncpy(sm->metricName, metricName, sizeof(sm->metricName));
    sr->metricsLen++;
    LOGDEBUG("allocated new sensor metric %s:%s\n", sr->resourceName, sm->metricName);

    return sm;
}

//!
//!
//!
//! @param[in] do_alloc
//! @param[in] sm
//! @param[in] counterType
//!
//! @return a pointer to the sensor counter or NULL on failure
//!
static sensorCounter *find_or_alloc_sc(const boolean do_alloc, sensorMetric * sm, const sensorCounterType counterType)
{
    // sanity check
    if (sm->countersLen < 0 || sm->countersLen > MAX_SENSOR_COUNTERS) {
        LOGWARN("inconsistency in sensor database (countersLen=%d for %s)\n", sm->countersLen, sm->metricName);
        return NULL;
    }

    for (int c = 0; c < sm->countersLen; c++) {
        sensorCounter *sc = sm->counters + c;
        if (sc->type == counterType) {
            return sc;
        }
    }

    if (!do_alloc                      // did not find it
        || sm->countersLen == MAX_SENSOR_COUNTERS)  // out of room
        return NULL;

    // fill out the new slot
    sensorCounter *sc = sm->counters + sm->countersLen;
    bzero(sc, sizeof(sensorCounter));
    sc->type = counterType;
    sm->countersLen++;
    LOGDEBUG("allocated new sensor counter %s:%s\n", sm->metricName, sensor_type2str(sc->type));

    return sc;
}

//!
//!
//!
//! @param[in] do_alloc
//! @param[in] sc
//! @param[in] dimensionName
//!
//! @return a pointer to the sensor dimension structure or NULL on failure.
//!
static sensorDimension *find_or_alloc_sd(const boolean do_alloc, sensorCounter * sc, const char *dimensionName)
{
    // sanity check
    if (sc->dimensionsLen < 0 || sc->dimensionsLen > MAX_SENSOR_DIMENSIONS) {
        LOGWARN("inconsistency in sensor database (dimensionsLen=%d for %s)\n", sc->dimensionsLen, sensor_type2str(sc->type));
        return NULL;
    }

    for (int d = 0; d < sc->dimensionsLen; d++) {
        sensorDimension *sd = sc->dimensions + d;
        if ((strcmp(sd->dimensionName, dimensionName) == 0) || (strcmp(sd->dimensionAlias, dimensionName) == 0)) {
            return sd;
        }
    }
    if (!do_alloc                      // did not find it
        || sc->dimensionsLen == MAX_SENSOR_DIMENSIONS)  // out of room
        return NULL;

    // fill out the new slot
    sensorDimension *sd = sc->dimensions + sc->dimensionsLen;
    bzero(sd, sizeof(sensorDimension));
    euca_strncpy(sd->dimensionName, dimensionName, sizeof(sd->dimensionName));
    sc->dimensionsLen++;
    LOGDEBUG("allocated new sensor dimension %s:%s\n", sensor_type2str(sc->type), sd->dimensionName);

    return sd;
}

//!
//! Merges records in srs[] array of pointers (of length srsLen)
//! into records in the in-memory sensor values cache.  The merge
//! adds new entries at all levels, if necessary (i.e., if the sensor
//! is new, if the metric is new, etc.) and skips over values that
//! are already in the cache. So it is safe to call it many times
//! with the same data - all but the first invocations will have no
//! effect.
//!
//! @param[in] srs
//! @param[in] srsLen
//! @param[in] fail_on_oom
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int sensor_merge_records(sensorResource * srs[], int srsLen, boolean fail_on_oom)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    LOGTRACE("invoked with srsLen=%d fail_on_oom=%d\n", srsLen, fail_on_oom);

    int ret = EUCA_ERROR;
    int num_merged = 0;
    sem_p(state_sem);
    for (int r = 0; r < srsLen; r++) {
        const sensorResource *sr = srs[r];
        LOGTRACE("merging results for resource %s [%d]\n", sr->resourceName, r);
        if (is_empty_sr(sr))
            continue;
        sensorResource *cache_sr = find_or_alloc_sr(TRUE, sr->resourceName, sr->resourceType, sr->resourceUuid);
        if (cache_sr == NULL) {
            LOGWARN("failed to find space in sensor cache for resource %s\n", sr->resourceName);
            if (fail_on_oom)
                goto bail;
            continue;
        }

        for (int m = 0; m < sr->metricsLen; m++) {
            const sensorMetric *sm = sr->metrics + m;
            sensorMetric *cache_sm = find_or_alloc_sm(TRUE, cache_sr, sm->metricName);
            if (cache_sm == NULL) {
                LOGWARN("failed to find space in sensor cache for metric %s:%s\n", sr->resourceName, sm->metricName);
                if (fail_on_oom)
                    goto bail;
                continue;
            }

            for (int c = 0; c < sm->countersLen; c++) {
                const sensorCounter *sc = sm->counters + c;
                sensorCounter *cache_sc = find_or_alloc_sc(TRUE, cache_sm, sc->type);
                if (cache_sc == NULL) {
                    LOGWARN("failed to find space in sensor cache for counter %s:%s:%s\n", sr->resourceName, sm->metricName, sensor_type2str(sc->type));
                    if (fail_on_oom)
                        goto bail;
                    continue;
                }
                // update the collection interval
                if (sc->collectionIntervalMs > 0) {
                    cache_sc->collectionIntervalMs = sc->collectionIntervalMs;
                }
                // run through dimensions merging in their values separately
                for (int d = 0; d < sc->dimensionsLen; d++) {
                    const sensorDimension *sd = sc->dimensions + d;
                    sensorDimension *cache_sd = find_or_alloc_sd(TRUE, cache_sc, sd->dimensionName);
                    if (cache_sd == NULL) {
                        LOGWARN("failed to find space in sensor cache for dimension %s:%s:%s:%s\n", sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
                        if (fail_on_oom)
                            goto bail;
                        continue;
                    }

                    if (cache_sd->valuesLen < 0 || cache_sd->valuesLen > MAX_SENSOR_VALUES) {   // sanity check
                        LOGWARN("inconsistency in sensor database (valuesLen=%d for %s:%s:%s:%s)\n",
                                cache_sd->valuesLen, cache_sr->resourceName, cache_sm->metricName, sensor_type2str(cache_sc->type), cache_sd->dimensionName);
                        goto bail;
                    }

                    if (sd->valuesLen < 1)  // no values in this dimension at all
                        continue;

                    // correlate new values with values already in the cache:
                    // phase 1: go backwards through sequence numbers of new and old

                    int inv_start = -1; // input start logical index for copying of new values
                    int iov = cache_sd->valuesLen - 1;  // logical index for old values, starting with the latest
                    int iov_start = iov + 1;    // cache start logical index for receiving new values
                    for (int inv = sd->valuesLen - 1; inv >= 0; inv--) {    // logical index for new values, starting with the latest
                        long long sov = cache_sd->sequenceNum + iov;    // seq for old values
                        long long snv = sd->sequenceNum + inv;  // seq for new values

                        if (snv < sov) {    // the last new seq number is behind the last old seq number
                            // this can happen when sensor resets; if, additionally,
                            // network outage prevented delivery for a while, there
                            // may also be a gap in numbers, rather than a reset to 0
                            LOGINFO("reset in sensor values detected, clearing history for %s:%s:%s:%s\n", sr->resourceName,
                                    sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
                            LOGDEBUG("cached valuesLen=%d seq=%lld+%d vs new valuesLen=%d seq=%lld+%d\n", cache_sd->valuesLen,
                                     cache_sd->sequenceNum, iov, sd->valuesLen, sd->sequenceNum, inv);
                            inv_start = 0;  // copy all new values
                            iov_start = 0;  // overwrite what is in cache
                            break;
                        }

                        if (snv > sov) {    // new data, so include it in the list to copy
                            inv_start = inv;
                            continue;
                        }

                        if (iov < 0)   // no more old, cached data to compare against
                            continue;

                        // the rest of this is for internal checking - the old and new values must match
                        int vn_adj = (inv + sd->firstValueIndex) % MAX_SENSOR_VALUES;   // values adjusted for firstValueIndex
                        int vo_adj = (iov + cache_sd->firstValueIndex) % MAX_SENSOR_VALUES;
                        if ((sd->values[vn_adj].timestampMs != cache_sd->values[vo_adj].timestampMs)
                            || (sd->values[vn_adj].available != cache_sd->values[vo_adj].available)
                            || (sd->values[vn_adj].value != cache_sd->values[vo_adj].value)) {
                            LOGWARN("mismatch in sensor data being merged into in-memory cache, clearing history for %s:%s:%s:%s\n",
                                    sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
                            inv_start = 0;
                            iov_start = 0;
                            break;
                        }

                        iov--;
                    }

                    // step 2: if there is new data, copy it into the right place

                    if (inv_start >= 0) {   // there is new data to copy
                        int iov = iov_start;
                        int copied = 0;
                        for (int inv = inv_start; inv < sd->valuesLen; inv++, iov++) {
                            int vn_adj = (inv + sd->firstValueIndex) % MAX_SENSOR_VALUES;   // values adjusted for firstValueIndex
                            int vo_adj = (iov + cache_sd->firstValueIndex) % MAX_SENSOR_VALUES;
                            cache_sd->values[vo_adj].timestampMs = sd->values[vn_adj].timestampMs;
                            cache_sd->values[vo_adj].available = sd->values[vn_adj].available;
                            cache_sd->values[vo_adj].value = sd->values[vn_adj].value;

                            // if this is the first value for a SUMMATION-type counter (seq num is zero),
                            // set the shift to the negative of the value so that values go back to zero, too
                            // (this is easier than maintaining shift_value, which is also used to compensate
                            // for value resets due to instance rebooting, across component restarts)
                            if ((sd->sequenceNum + iov) == 0 && copied == 0 && sc->type == SENSOR_SUMMATION) {
                                if (sd->values[vn_adj].value != 0) {
                                    cache_sd->shift_value = -sd->values[vn_adj].value;  // TODO: deal with the case when available is FALSE?
                                    LOGTRACE("at seq 0, setting shift for %s:%s:%s:%s to %f\n",
                                             sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName, cache_sd->shift_value);
                                }
                            } else {
                                sensorValue *sv = cache_sd->values + vo_adj;
                                LOGTRACE("merging sensor value %s:%s:%s:%s %05lld %014lld %s %f\n",
                                         sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName, sd->sequenceNum + inv,
                                         sv->timestampMs, sv->available ? "YES" : " NO", sv->available ? sv->value : -1);
                            }
                            num_merged++;
                            copied++;
                        }
                        // adjust the length capping it at array size
                        cache_sd->valuesLen = iov_start + copied;
                        if (cache_sd->valuesLen > MAX_SENSOR_VALUES) {
                            cache_sd->valuesLen = MAX_SENSOR_VALUES;
                        }
                        // shift the first entry's index up if the values wrapped
                        cache_sd->firstValueIndex = (cache_sd->firstValueIndex + (iov_start + copied) - cache_sd->valuesLen) % MAX_SENSOR_VALUES;

                        // set the sequence number by counting back from the seq num of the last value copied in
                        cache_sd->sequenceNum = (sd->sequenceNum + sd->valuesLen) - cache_sd->valuesLen;

                        //! update the interval now (@TODO should we base it on the delta between the last two values?)
                        cache_sc->collectionIntervalMs = sensor_state->collection_interval_time_ms;
                    }
                }
            }
        }
        cache_sr->timestamp = time(NULL);
        LOGTRACE("updated %s cache timestamp to %d\n", cache_sr->resourceName, cache_sr->timestamp);
    }
    ret = EUCA_OK;

bail:

    sem_v(state_sem);
    LOGTRACE("completed: merged %d values, ret=%d\n", num_merged, ret);

    return (ret);
}

//!
//! Adds a single value into the in-memory sensor cache. This is
//! implemented by constructing a whole big sensorResource record
//! just for one value and merging it. The advantage of this
//! approach is that all additions are done by the merging code.
//!
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] metricName
//! @param[in] counterType
//! @param[in] dimensionName
//! @param[in] sequenceNum
//! @param[in] timestampMs
//! @param[in] available
//! @param[in] value
//!
//! @return the result of the sensor_merge_records() call
//!
//! @see sensor_merge_records()
//!
int sensor_add_value(const char *instanceId, const char *metricName, const int counterType, const char *dimensionName, const long long sequenceNum, const long long timestampMs,
                     const boolean available, const double value)
{
    // this data structure is a carrier for the value
    sensorResource sr = {
        .resourceType = "instance",
        .metricsLen = 1,
        .metrics = {
                    {
                     .countersLen = 1,
                     .counters = {
                                  {
                                   .collectionIntervalMs = 0,
                                   .dimensionsLen = 1,
                                   .dimensions = {
                                                  {
                                                   .sequenceNum = sequenceNum,
                                                   .valuesLen = 1}
                                                  }
                                   }
                                  }
                     }
                    }
    };
    euca_strncpy(sr.resourceName, instanceId, sizeof(sr.resourceName));
    sensorMetric *sm = sr.metrics;     // use array entry [0]
    euca_strncpy(sm->metricName, metricName, sizeof(sm->metricName));
    sensorCounter *sc = sm->counters;  // use array entry [0]
    sc->type = counterType;
    sensorDimension *sd = sc->dimensions;   // use array entry [0]
    euca_strncpy(sd->dimensionName, dimensionName, sizeof(sd->dimensionName));
    sensorValue *sv = sd->values;      // use array entry [0]
    sv->timestampMs = timestampMs;
    sv->value = value;
    sv->available = available;

    sensorResource *srs[1] = { &sr };

    LOGTRACE("adding sensor value %s:%s:%s:%s %05lld %014lld %s %f\n",
             sr.resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName, sequenceNum, sv->timestampMs,
             sv->available ? "YES" : " NO", sv->available ? sv->value : -1);
    return sensor_merge_records(srs, 1, TRUE);
}

//!
//! A function for getting the latest value for a particular
//! (resource x metric x counter x dimension) value, along with
//! various related values, such as intervalMs and total number
//! of values in the cache. Given that most users will prefer
//! the bulk retrieval funcion sensor_get_instance_data(), this
//! one is more likely of use only for debugging.
//!
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] metricName
//! @param[in] counterType
//! @param[in] dimensionName
//! @param[in] sequenceNum
//! @param[in] timestampMs
//! @param[in] available
//! @param[in] value
//! @param[in] intervalMs
//! @param[in] valLen
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int sensor_get_value(const char *instanceId, const char *metricName, const int counterType, const char *dimensionName, long long *sequenceNum, long long *timestampMs,
                     boolean * available, double *value, long long *intervalMs, int *valLen)
{
    int ret = EUCA_ERROR;
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    sem_p(state_sem);
    sensorResource *cache_sr = find_or_alloc_sr(FALSE, instanceId, "instance", NULL);
    if (cache_sr == NULL)
        goto bail;

    sensorMetric *cache_sm = find_or_alloc_sm(FALSE, cache_sr, metricName);
    if (cache_sm == NULL)
        goto bail;

    sensorCounter *cache_sc = find_or_alloc_sc(FALSE, cache_sm, counterType);
    if (cache_sc == NULL)
        goto bail;

    sensorDimension *cache_sd = find_or_alloc_sd(FALSE, cache_sc, dimensionName);
    if (cache_sd == NULL)
        goto bail;

    if (cache_sd->valuesLen < 1)       // no values in this dimension at all
        goto bail;

    *sequenceNum = cache_sd->sequenceNum + cache_sd->valuesLen - 1;
    *intervalMs = cache_sc->collectionIntervalMs;
    *valLen = cache_sd->valuesLen;

    sensorValue *sv = cache_sd->values + ((cache_sd->firstValueIndex + cache_sd->valuesLen - 1) % MAX_SENSOR_VALUES);
    *timestampMs = sv->timestampMs;
    *available = sv->available;
    *value = sv->value;
    ret = EUCA_OK;

bail:

    sem_v(state_sem);
    return ret;
}

//!
//!
//!
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] sensorIds
//! @param[in] sensorIdsLen
//! @param[in] sr_out
//! @param[in] srLen
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int sensor_get_instance_data(const char *instanceId, char **sensorIds, int sensorIdsLen, sensorResource ** sr_out, int srLen)
{
    int ret = EUCA_ERROR;
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    LOGTRACE("sensor_get_instance_data() called for instance %s\n", instanceId == NULL ? "NULL" : instanceId);

    sem_p(state_sem);
    time_t this_interval = 0;          // For determining polling interval.
    int sri = 0;                       // index into output array sr_out[]
    for (int r = 0; r < sensor_state->max_resources; r++) {
        sensorResource *sr = sensor_state->resources + r;

        if (is_empty_sr(sr))           // unused slot in cache, skip it
            continue;

        if ((instanceId != NULL)       // we are looking for a specific instance (rather than all)
            && (strcmp(sr->resourceName, instanceId) != 0)) // and this is not the one
            continue;

        if (sensorIdsLen > 0)          //! @todo implement support for sensorIds[]
            goto bail;

        if (sri >= srLen)              // out of room in output
            goto bail;                 //! @fixme Log something here?

        memcpy(sr_out[sri], sr, sizeof(sensorResource));    //! @todo run through the data, do not just copy
        sri++;

        if (instanceId != NULL)        // only one instance to copy
            break;
    }
    if (sri > 0)                       // we have at least one result
        ret = EUCA_OK;

bail:

    if (sensor_state->last_polled) {   // Ensure this isn't the first one.
        time_t t = time(NULL);
        this_interval = t - sensor_state->last_polled;
        // The interval since the last poll must exceed a minimum
        // threshold to be updated. If it does not exceed this
        // threshold, the most likely reason is that it was one of a
        // series of "clumped" queries in a single polling cycle.  The
        // threshold has been set, somewhat arbitrarily (FIXME?), to 5
        // seconds, or 1 below the current minimum NC_POLLING_FREQUENCY
        // value (which is a period rather than a frequency).
        if (this_interval <= 5) {
            LOGTRACE("NOT adjusting measured upstream polling interval from %ld to %ld (which is below threshold)\n", sensor_state->interval_polled, this_interval);
            sensor_state->last_polled = t;
        } else {
            if (this_interval == sensor_state->interval_polled) {
                LOGTRACE("maintaining measured upstream polling interval of %ld\n", sensor_state->interval_polled);
            } else {
                if (sensor_state->interval_polled) {
                    LOGTRACE("adjusting measured upstream polling interval from %ld to %ld\n", sensor_state->interval_polled, this_interval);
                } else {
                    LOGTRACE("setting measured upstream polling interval to %ld\n", this_interval);
                }
                sensor_state->interval_polled = this_interval;
            }
            sensor_state->last_polled = t;
        }
    } else {
        LOGTRACE("first poll--setting baseline for measuring upstream polling interval\n");
        sensor_state->last_polled = time(NULL);
    }
    if (this_interval > 5) {
        // Only do this if at least the minimum interval has
        // passed--prevents trying to expire the cache several times in
        // one polling cycle when we get clumped requests.
        int num_expired = sensor_expire_cache_entries();
        if (num_expired) {
            LOGINFO("%d resource entries expired from sensor cache\n", num_expired);
        }
    }

    sem_v(state_sem);
    return ret;
}

//!
//!
//!
//! @param[in] resourceName
//! @param[in] resourceType
//! @param[in] resourceUuid
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int sensor_add_resource(const char *resourceName, const char *resourceType, const char *resourceUuid)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    int ret = (EUCA_ERROR);
    sem_p(state_sem);
    if (find_or_alloc_sr(TRUE, resourceName, resourceType, resourceUuid) != NULL) {
        ret = EUCA_OK;
    }
    sem_v(state_sem);

    return ret;
}

//!
//!
//!
//! @param[in] resourceName
//! @param[in] resourceAlias
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int sensor_set_resource_alias(const char *resourceName, const char *resourceAlias)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    int ret = EUCA_ERROR;
    sem_p(state_sem);
    sensorResource *sr = find_or_alloc_sr(FALSE, resourceName, NULL, NULL);
    if (sr != NULL) {
        if (resourceAlias) {
            if (strcmp(sr->resourceAlias, resourceAlias) != 0) {
                euca_strncpy(sr->resourceAlias, resourceAlias, sizeof(sr->resourceAlias));
                LOGDEBUG("set alias for sensor resource %s to %s\n", resourceName, resourceAlias);
            }
        } else {
            LOGTRACE("clearing alias for resource '%s'\n", resourceName);
            sr->resourceAlias[0] = '\0';    // clears the alias
        }
        ret = EUCA_OK;
    }
    sem_v(state_sem);

    return ret;
}

//!
//!
//!
//! @param[in] resourceName
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int sensor_remove_resource(const char *resourceName)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    int ret = EUCA_ERROR;
    sem_p(state_sem);
    sensorResource *sr = find_or_alloc_sr(FALSE, resourceName, NULL, NULL);
    if (sr != NULL) {
        sr->resourceName[0] = '\0';    // marks the slot as empty
        ret = EUCA_OK;
    }
    sem_v(state_sem);

    return ret;
}

//!
//!
//!
//! @param[in] resourceName
//! @param[in] metricName
//!
//! @return
//!
int sensor_shift_metric(const char *resourceName, const char *metricName)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    int ret = EUCA_ERROR;
    sem_p(state_sem);

    sensorResource *sr = find_or_alloc_sr(FALSE, resourceName, NULL, NULL);
    if (sr == NULL)
        goto bail;

    sensorMetric *sm = find_or_alloc_sm(FALSE, sr, metricName);
    if (sm == NULL)
        goto bail;

    if (sm->countersLen < 0 || sm->countersLen > MAX_SENSOR_COUNTERS) {
        LOGERROR("invalid resource array: counterLen out of bounds (countersLen=%d for %s:%s)\n", sm->countersLen, sr->resourceName, sm->metricName);
        goto bail;
    }

    for (int c = 0; c < sm->countersLen; c++) {
        const sensorCounter *sc = sm->counters + c;
        if (sc->dimensionsLen < 0 || sc->dimensionsLen > MAX_SENSOR_DIMENSIONS) {
            LOGERROR("invalid resource array: [%d] sensorCounter out of bounds (dimensionsLen=%d for %s:%s:%s)\n", c,
                     sc->dimensionsLen, sr->resourceName, sm->metricName, sensor_type2str(sc->type));
            goto bail;
        }

        if (sc->type != SENSOR_SUMMATION)   // shifting numbers only makes sense for summation counters
            continue;

        for (int d = 0; d < sc->dimensionsLen; d++) {
            sensorDimension *sd = ((sensorDimension *) (sc->dimensions + d));

            if (sd->valuesLen < 0 || sd->valuesLen > MAX_SENSOR_VALUES) {   // sanity check
                LOGERROR("inconsistency in sensor database (valuesLen=%d for %s:%s:%s:%s)\n",
                         sd->valuesLen, sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
                goto bail;
            }

            if (sd->valuesLen < 1)     // no values in this dimension at all
                continue;

            // find the latest value in the history (TODO: use the latest available, not just latest value?)
            int i_last_logical = sd->valuesLen - 1; // logical index for the latest value
            int i_last_actual = (i_last_logical + sd->firstValueIndex) % MAX_SENSOR_VALUES; // actual index, adjusted for offset and wrap
            double offset = sd->values[i_last_actual].value;

            // increment the shift by the latest value: this way the next measurement can reset to zero,
            // while DescribeSensors() can continue reporting a strictly growing set of numbers
            sd->shift_value += offset;
            LOGTRACE("increasing shift for %s:%s:%s:%s by %f to %f\n", sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName, offset, sd->shift_value);

            // adjust the history to reflect the shift so that these pre-shift values
            // continue being reported correctly after the shift
            for (int i = 0; i < sd->valuesLen; i++) {
                int i_actual = (i + sd->firstValueIndex) % MAX_SENSOR_VALUES;
                if (sd->values[i_actual].available) {
                    sd->values[i_actual].value -= offset;

                    // sanity check
                    if (sd->values[i_actual].value > 0) {
                        LOGERROR("inconsistency in sensor database (positive history value after shift: %f for %s:%s:%s:%s)\n",
                                 sd->values[i_actual].value, sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
                    }
                }
            }
        }
    }

    ret = EUCA_OK;

bail:
    sem_v(state_sem);
    return (ret);
}

//!
//!
//!
//! @param[in] resourceName
//! @param[in] metricName
//! @param[in] counterType
//! @param[in] dimensionName
//! @param[in] dimensionAlias
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int sensor_set_dimension_alias(const char *resourceName, const char *metricName, const int counterType, const char *dimensionName, const char *dimensionAlias)
{
    int ret = EUCA_ERROR;
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    if (resourceName == NULL || strlen(resourceName) < 1 || strlen(resourceName) > MAX_SENSOR_NAME_LEN) {
        LOGWARN("sensor_set_dimension_alias() invoked with invalid resourceName (%s)\n", resourceName);
        return 1;
    }

    sem_p(state_sem);

    // do not allocate resource structure here
    // (it should be done prior to calling this function,
    // by somebody who knows resource type and uuid)
    sensorResource *sr = find_or_alloc_sr(FALSE, resourceName, NULL, NULL);
    if (sr == NULL)
        goto bail;

    sensorMetric *sm = find_or_alloc_sm(TRUE, sr, metricName);  // allocate metric if necessary
    if (sm == NULL)
        goto bail;

    sensorCounter *sc = find_or_alloc_sc(TRUE, sm, counterType);    // allocate counter if necessary
    if (sc == NULL)
        goto bail;

    sensorDimension *sd = find_or_alloc_sd(TRUE, sc, dimensionName);    // allocate dimension if necessary
    if (sd == NULL)
        goto bail;

    boolean changed = FALSE;
    if (dimensionAlias) {
        if (strcmp(sd->dimensionAlias, dimensionAlias) != 0) {
            euca_strncpy(sd->dimensionAlias, dimensionAlias, sizeof(sd->dimensionAlias));
            changed = TRUE;
        }
    } else {
        if (strlen(sd->dimensionAlias) > 0) {
            sd->dimensionAlias[0] = '\0';   // clear the alias
            changed = TRUE;
        }
    }
    if (changed) {
        LOGDEBUG("set alias for sensor dimension %s:%s:%s:%s to '%s'\n", resourceName, metricName, sensor_type2str(counterType), dimensionName, sd->dimensionAlias);
    }

    ret = EUCA_OK;

bail:

    sem_v(state_sem);
    return ret;
}

//!
//!
//!
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] guestDev
//!
//! @return 0 or the number of sensor that we fail to update
//!
int sensor_set_volume(const char *instanceId, const char *volumeId, const char *guestDev)
{
    int ret = 0;

    ret += sensor_set_dimension_alias(instanceId, "DiskReadOps", SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias(instanceId, "DiskWriteOps", SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias(instanceId, "DiskReadBytes", SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias(instanceId, "DiskWriteBytes", SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias(instanceId, "VolumeTotalReadTime", SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias(instanceId, "VolumeTotalWriteTime", SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias(instanceId, "VolumeQueueLength", SENSOR_SUMMATION, volumeId, guestDev);   // VB uses 'summation'
    ret += sensor_set_dimension_alias(instanceId, "VolumeQueueLength", SENSOR_LATEST, volumeId, guestDev);  // NC uses 'latest'

    return ret;
}

//!
//! request to explicitly refresh sensor values for a
//! particular resource (useful for getting data between
//! poll events in bottom_half(), which may be spaced far
//! apart)
//!
//! NOTE: it is recommended to hold hyp_sem (hypervisor
//!       semaphore) while invoking this so that concurrent
//!       calls to libvirt do not destabilize libvirtd
//!
//! @param[in] resourceNames
//! @param[in] resourceAliases
//! @param[in] size
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int sensor_refresh_resources(char resourceNames[][MAX_SENSOR_NAME_LEN], char resourceAliases[][MAX_SENSOR_NAME_LEN], int size)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE)
        return (EUCA_ERROR);

    LOGTRACE("invoked size=%d\n", size);
    getstat **stats = NULL;
    if (getstat_generate(&stats) != EUCA_OK) {
        LOGWARN("failed to invoke getstats for sensor data\n");
        return (EUCA_ERROR);
    } else {
        LOGDEBUG("polled statistics for %d instance(s)\n", getstat_ninstances(stats));
    }

    int nvalues = 0;
    for (int i = 0; i < size; i++) {
        int nvalues_resource = 0;
        char *name = (char *)resourceNames[i];
        char *alias = (char *)resourceAliases[i];
        if (name[0] == '\0')           // empty entry in the array
            continue;
        getstat *vals = NULL;
        if ((vals = getstat_find(stats, name)) != NULL)
            nvalues_resource += getstat_add_values(name, vals);
        if ((alias[0] != '\0') && (vals = getstat_find(stats, alias))) {
            nvalues_resource += getstat_add_values(name, vals);
        }
        if (nvalues_resource > 0) {
            nvalues += nvalues_resource;
            continue;
        }
        // can't find this resource by name or by alias
        LOGDEBUG("unable to get metrics for resource %s (OK if it was terminated---should soon expire from the cache)\n", name);
        //! @TODO 3.2: decide what to do when some metrics for an instance aren't available.
        //! One possibility is that the CLC isn't actively polling us, which
        //! means we've not cleaned up the sensor cache recently...and
        //! stale/terminated resources have accumulated in it. So force a
        //! cache-expiration run.
        sem_p(state_sem);              // Must set semaphore for sensor_expire_cache_entries() call.
        time_t t = time(NULL);
        time_t this_interval = t - sensor_state->last_polled;
        if (this_interval > 5) {
            // Only do this if at least the minimum interval has
            // passed--prevents trying to expire the cache several times
            // in one polling cycle when we get clumped requests.
            int num_expired = sensor_expire_cache_entries();
            if (num_expired) {
                LOGINFO("%d resource entries expired from sensor cache\n", num_expired);
            }
        }
        sem_v(state_sem);
    }
    getstat_free(stats);
    if (nvalues > 0)
        seq_num++;
    LOGTRACE("done nvalues=%d seq_num=%lld\n", nvalues, seq_num);

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] srs
//! @param[in] srsLen
//!
//! @return EUCA_OK on success or the number of errors on failure
//!
int sensor_validate_resources(sensorResource ** srs, int srsLen)
{
    int errors = EUCA_OK;

    for (int i = 0; i < srsLen; i++) {
        sensorResource *sr = srs[i];
        if (sr == NULL) {
            LOGERROR("invalid resource array: [%d] empty slot\n", i);
            errors++;
            continue;
        }
        if (sr->metricsLen < 0 || sr->metricsLen > MAX_SENSOR_METRICS) {
            LOGERROR("invalid resource array: [%d] metricsLen out of bounds (metricsLen=%d for %s)\n", i, sr->metricsLen, sr->resourceName);
            errors++;
            continue;
        }
        for (int m = 0; m < sr->metricsLen; m++) {
            const sensorMetric *sm = sr->metrics + m;
            if (sm->countersLen < 0 || sm->countersLen > MAX_SENSOR_COUNTERS) {
                LOGERROR("invalid resource array: [%d:%d] counterLen out of bounds (countersLen=%d for %s:%s)\n", i, m, sm->countersLen, sr->resourceName, sm->metricName);
                errors++;
                goto next_resource;
            }
            for (int c = 0; c < sm->countersLen; c++) {
                const sensorCounter *sc = sm->counters + c;
                if (sc->dimensionsLen < 0 || sc->dimensionsLen > MAX_SENSOR_DIMENSIONS) {
                    LOGERROR("invalid resource array: [%d:%d:%d] sensorCounter out of bounds (dimensionsLen=%d for %s:%s:%s)\n", i, m, c,
                             sc->dimensionsLen, sr->resourceName, sm->metricName, sensor_type2str(sc->type));
                    errors++;
                    goto next_resource;
                }
                for (int d = 0; d < sc->dimensionsLen; d++) {
                    const sensorDimension *sd = sc->dimensions + d;
                    if (sd->valuesLen < 0 || sd->valuesLen > MAX_SENSOR_VALUES) {
                        LOGERROR("invalid resource array: [%d:%d:%d:%d] valuesLen out of bounds (valuesLen=%d for %s:%s:%s:%s)\n", i, m,
                                 c, d, sd->valuesLen, sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
                        errors++;
                        goto next_resource;
                    }
                    for (int v = 0; v < sd->valuesLen; v++) {
                        //! @TODO anything to verify in values?
                    }
                }
            }
        }
next_resource:
        continue;                      // label so we can bail out of loops, continue to keep gcc happy
    }

    return errors;
}

#ifdef _UNIT_TEST
//!
//!
//!
static void dump_sensor_cache(void)
{
    sensorResource **srs = EUCA_ZALLOC(sensor_state->max_resources, sizeof(sensorResource *));
    for (int i = 0; i < sensor_state->max_resources; i++) {
        srs[i] = &(sensor_state->resources[i]);
    }
    log_sensor_resources("whole cache", srs, sensor_state->max_resources);
    EUCA_FREE(srs);
}

//!
//!
//!
//! @param[out] srs
//! @param[in]  srsLen
//!
static void clear_srs(sensorResource ** srs, int srsLen)
{
    for (int i = 0; i < srsLen; i++) {
        bzero(srs[i], sizeof(sensorResource));
    }
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
    ts = time_usec() / 1000;
    logfile(NULL, EUCA_LOG_TRACE, 4);
    log_prefix_set("%T %L %t9 %m-24 %F-33 |");
    LOGDEBUG("testing sensor.c with cache of size 2 and MAX_SENSOR_VALUES=%d\n", MAX_SENSOR_VALUES);

    long long intervalMs = 50000;
    assert(sensor_init(NULL, NULL, 2, FALSE, NULL) == 0);
    assert(sensor_state->max_resources > 0);
    assert(sensor_state->used_resources == 0);
    assert(0 != sensor_config(-1, 5000));
    assert(0 != sensor_config(MAX_SENSOR_VALUES + 1, 5000));
    assert(0 != sensor_config(1, 50));
    assert(0 != sensor_config(1, 50000000000));
    assert(0 == sensor_config(0, 50000));
    assert(0 == sensor_config(1, 50000));
    assert(0 == sensor_config(3, intervalMs));

#define GETSTAT_ITERS 10
    // test the getstat function
    getstat **stats = NULL;
    for (int i = 1; i <= GETSTAT_ITERS; i++) {
        if (i % 2) {
            euca_this_component_name = "nc";
        } else {
            euca_this_component_name = "cc";
        }
        assert(getstat_generate(&stats) == EUCA_OK);
        getstat *gs = getstat_find(stats, NULL);
        if (gs != NULL) {
            char id[1][MAX_SENSOR_NAME_LEN] = { "" };
            char res[1][MAX_SENSOR_NAME_LEN] = { "" };
            euca_strncpy(id[0], gs->instanceId, sizeof(id[0]));
            assert(sensor_refresh_resources(id, res, 1) == EUCA_OK);
        }
        if (i % 101 == 0 || i % 102 == 0) {
            LOGDEBUG("getstat_refresh() iteration %d/%d found %d instances\n", i, GETSTAT_ITERS, getstat_ninstances(stats));
        }
    }
    char *anInstanceId = NULL;
    if (stats) {
        anInstanceId = stats[0]->instanceId;
    }
    if (anInstanceId != NULL)
        assert(getstat_find(stats, anInstanceId) != NULL);

    euca_this_component_name = "ignore";    // set component name so that getstats won't get involved and won't put real sensor readings into sensor state
    getstat_free(stats);
    init_state(2);                     // clear out sensor state after previous experiments
    assert(0 == sensor_config(3, intervalMs));

    // test sensor_add_value and sensor_get_value
    double val = 11.0;
    for (int j = 0; j < 50; j++) {
        long long sn = -1L;
        int valLen = 0;
        for (int i = 0; i < ((MAX_SENSOR_VALUES / 2) * (j + 1)); i++) {
            sn += 1;
            ts += intervalMs;
            val += 1;
            assert(0 == sensor_add_value("i-555", "CPUUtilization", SENSOR_AVERAGE, "default", sn, ts, (sn % 2) ? TRUE : FALSE, val));
            assert(0 == sensor_add_value("i-555", "CPUUtilization", SENSOR_AVERAGE, "default", sn, ts, (sn % 2) ? TRUE : FALSE, val));  // should be a no-op
            valLen++;
            if (valLen > MAX_SENSOR_VALUES)
                valLen = MAX_SENSOR_VALUES;

            {
                long long last_sn;
                long long last_ts;
                boolean last_available;
                double last_val;
                long long last_intervalMs;
                int last_valLen;

                assert(0 == sensor_get_value("i-555", "CPUUtilization", SENSOR_AVERAGE, "default", &last_sn, &last_ts, &last_available, &last_val, &last_intervalMs, &last_valLen));
                assert(last_sn == sn);
                assert(last_ts == ts);
                if (!(last_intervalMs == intervalMs)) {
                    LOGERROR("bad\n");
                }
                assert(last_available == (sn % 2) ? TRUE : FALSE);
                assert(last_val == val);
                assert(last_valLen == valLen);
            }
        }
        intervalMs += 50000;
        assert(0 == sensor_config(3, intervalMs));
    }

    // add the "dummy" struct as a second resource
    sensorResource **srs = EUCA_ZALLOC(sensor_state->max_resources, sizeof(sensorResource *));
    assert(srs);
    for (int i = 0; i < sensor_state->max_resources; i++) {
        srs[i] = EUCA_ZALLOC(1, sizeof(sensorResource));
        assert(srs[i]);
    }
    assert(0 == sensor_get_dummy_instance_data(0L, "i-666", NULL, 0, srs, sensor_state->max_resources));
    assert(0 == sensor_merge_records(srs, sensor_state->max_resources, TRUE));
    assert(0 == sensor_merge_records(srs, sensor_state->max_resources, TRUE));  // should be a no-op

    assert(0 == sensor_get_dummy_instance_data(1L, "i-666", NULL, 0, srs, sensor_state->max_resources));    // will merge in last value
    assert(0 == sensor_merge_records(srs, sensor_state->max_resources, TRUE));
    assert(0 == sensor_merge_records(srs, sensor_state->max_resources, TRUE));  // should be a no-op

    assert(0 == sensor_get_dummy_instance_data(3L, "i-666", NULL, 0, srs, sensor_state->max_resources));    // will merge in two last values
    assert(0 == sensor_merge_records(srs, sensor_state->max_resources, TRUE));
    assert(0 == sensor_merge_records(srs, sensor_state->max_resources, TRUE));  // should be a no-op

    assert(0 == sensor_get_dummy_instance_data(6L, "i-666", NULL, 0, srs, sensor_state->max_resources));    // will append three values
    assert(0 == sensor_merge_records(srs, sensor_state->max_resources, TRUE));
    assert(0 == sensor_merge_records(srs, sensor_state->max_resources, TRUE));  // should be a no-op

    {                                  // verify the last value
        long long last_sn;
        long long last_ts;
        boolean last_available;
        double last_val;
        long long last_intervalMs;
        int last_valLen;

        assert(0 == sensor_get_value("i-666", "DiskReadOps", SENSOR_SUMMATION, "root", &last_sn, &last_ts, &last_available, &last_val, &last_intervalMs, &last_valLen));
        assert(last_sn == 8L);
        assert(last_available == TRUE);
    }

    // go to the limits of the sensorResource(Cache) structs
    assert(0 != sensor_add_value("i-777", "CPUUtilization", SENSOR_AVERAGE, "default", 0, 0, TRUE, 0)); // too many resources (only requested 2)
    assert(0 != sensor_add_value("i-666", "MadeUpMetric", SENSOR_SUMMATION, "root", 0, 0, TRUE, 0));    // exceeding MAX_SENSOR_METRICS (2)
    assert(0 != sensor_add_value("i-666", "DiskReadOps", SENSOR_AVERAGE, "root", 0, 0, TRUE, 0));   // exceeding MAX_SENSOR_COUNTERS (1)
    assert(0 != sensor_add_value("i-666", "DiskReadOps", SENSOR_SUMMATION, "another", 0, 0, TRUE, 0));  // exceeding MAX_SENSOR_DIMENSIONS (3)

    dump_sensor_cache();

    int srsLen = sensor_state->max_resources;
    LOGDEBUG("testing sensor_get_instance_data() function\n");
    clear_srs(srs, srsLen);            // clear out the array of structs to use it to retrieve data
    assert(0 == sensor_get_instance_data(NULL, NULL, 0, srs, srsLen));
    assert(0 == sensor_get_instance_data(NULL, NULL, 0, srs, srsLen));  // same
    clear_srs(srs, srsLen);            // clear out the array of structs to use it to retrieve data
    assert(0 == sensor_get_instance_data(NULL, NULL, 0, srs, srsLen));
    clear_srs(srs, srsLen);            // clear out the array of structs to use it to retrieve data
    assert(0 != sensor_get_instance_data("i-777", NULL, 0, srs, srsLen));

    {
        char *sensorId = "foo";
        assert(0 != sensor_get_instance_data("i-555", &sensorId, 1, srs, srsLen));
    }
    assert(0 == sensor_get_instance_data("i-555", NULL, 0, srs, srsLen));
    assert(0 == sensor_get_instance_data("i-555", NULL, 0, srs, srsLen));   // same
    log_sensor_resources("values read from cache", srs, srsLen);

    for (int i = 0; i < sensor_state->max_resources; i++) {
        EUCA_FREE(srs[i]);
    }
    EUCA_FREE(srs);

    dump_sensor_cache();
    LOGDEBUG("********************************\n");
    LOGDEBUG("testing with competitive threads\n");
    LOGDEBUG("********************************\n");
#define COMPETITIVE_PARTICIPANTS 5
#define COMPETITIVE_ITERATIONS 66666
    LOGINFO("spawning %d competing threads\n", COMPETITIVE_PARTICIPANTS);
    pthread_t threads[COMPETITIVE_PARTICIPANTS];
    long long thread_par[COMPETITIVE_PARTICIPANTS];
    int thread_par_sum = 0;
    for (int j = 0; j < COMPETITIVE_PARTICIPANTS; j++) {
        thread_par[j] = 0;             // pass param to thread, if any
        pthread_create(&threads[j], NULL, (j % 2 == 0) ? competitor_function_writer : competitor_function_reader, (void *)&thread_par[j]);
    }
    for (int j = 0; j < COMPETITIVE_PARTICIPANTS; j++) {
        pthread_join(threads[j], NULL);
        thread_par_sum += (int)thread_par[j];
    }
    LOGINFO("waited for all competing threads (returned sum=%d)\n", thread_par_sum);
    dump_sensor_cache();
    assert(thread_par_sum == 0);

    return 0;
}

//!
//!
//!
//! @param[in] ptr
//!
//! @return Always return NULL
//!
static void *competitor_function_reader(void *ptr)
{
    long long param = *(long long *)ptr;
    int errors = 0;

    LOGDEBUG("competitor reader running with param=%lld\n", param);

    sensorResource **srs = EUCA_ZALLOC(sensor_state->max_resources, sizeof(sensorResource *));
    int srsLen = sensor_state->max_resources;
    assert(srs);
    for (int i = 0; i < sensor_state->max_resources; i++) {
        srs[i] = EUCA_ZALLOC(1, sizeof(sensorResource));
        assert(srs[i]);
    }

    for (int j = 0; j < COMPETITIVE_ITERATIONS; j++) {
        assert(0 == sensor_get_instance_data(NULL, NULL, 0, srs, srsLen));
        // verify the output
        assert(0 == sensor_validate_resources(srs, srsLen));
    }

    for (int i = 0; i < sensor_state->max_resources; i++) {
        EUCA_FREE(srs[i]);
    }
    EUCA_FREE(srs);

    *(long long *)ptr = errors;
    return NULL;
}

//!
//!
//!
//! @param[in] ptr
//!
//! @return Always return NULL
//!
static void *competitor_function_writer(void *ptr)
{
    long long param = *(long long *)ptr;
    int errors = 0;

    LOGDEBUG("competitor writer running with param=%lld\n", param);

    // add the "dummy" struct as a second resource
    sensorResource **srs = EUCA_ZALLOC(sensor_state->max_resources, sizeof(sensorResource *));
    assert(srs);
    for (int i = 0; i < sensor_state->max_resources; i++) {
        srs[i] = EUCA_ZALLOC(1, sizeof(sensorResource));
        assert(srs[i]);
    }

    for (int j = 0; j < COMPETITIVE_ITERATIONS; j++) {
        _sn += 1;
        ts += 500;
        val += 1;
        char *r = (j % 2 == 0) ? "i-555" : "i-666";
        int err = 0;
        if (j % 3 == 0) {
            //  err = (0==sensor_add_value ("i-777", "CPUUtilization", SENSOR_AVERAGE, "default", _sn, ts, TRUE, val));
        } else {
            err = sensor_add_value(r, "CPUUtilization", SENSOR_AVERAGE, "default", _sn, ts, TRUE, val);
        }
        if (err) {
            LOGERROR("sensor_add_value failed err=%d\n", err);
            errors++;
        }
    }

    *(long long *)ptr = errors;
    return NULL;
}
#endif /* _UNIT_TEST */

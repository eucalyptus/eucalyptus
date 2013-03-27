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

// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

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
#include <errno.h>

#include "eucalyptus.h"
#include "misc.h"
#include "sensor.h"
#include "ipc.h"

#define MAX_SENSOR_RESOURCES MAXINSTANCES_PER_CC // used for resource name cache

static useconds_t next_sleep_duration_usec = DEFAULT_SENSOR_SLEEP_DURATION_USEC;
static sensorResourceCache * sensor_state = NULL;
static sem * state_sem = NULL;
static sem * hyp_sem = NULL;
static long long sn = 0L;
static int (*sensor_update_euca_config) (void) = NULL;

typedef struct getstat_t { // an internal struct for temporary storage of stats
    char instanceId [100];
    long long timestamp;
    char metricName [100];
    int counterType;
    char dimensionName [100];
    double value;
    struct getstat_t * next;
} getstat;

static void getstat_free (getstat ** stats)
{
  if (stats==NULL)
    return;

  getstat * gs;
  for (int i = 0; (gs = stats[i]) != NULL; i++) {
    getstat * gs_next;
    for ( ; gs != NULL ; gs = gs_next) {
      gs_next = gs->next;
      free (gs);
    }
  }
  free (stats);
}

static getstat * getstat_find (getstat ** stats, const char * instanceId)
{
  getstat * gs = NULL;

  if (stats) {
    for (int i = 0; (gs = stats[i]) != NULL; i++) {
      if (instanceId==NULL) // special case, for testing, return first thing in the list
	break;
      if (strcmp (gs->instanceId, instanceId) == 0)
	break;
    }
  }

  return gs;
}

static int getstat_ninstances (getstat ** stats)
{
  int ninstances = 0;

  if (stats) {
    for (int i = 0; stats[i] != NULL; i++) {
      ninstances++;
    }
  }

  return ninstances;
}

// obtain stats from the getstats script
static int getstat_generate (getstat ***pstats)
{
  assert(sensor_state!=NULL && state_sem!=NULL);

  errno = 0;
  if (hyp_sem) sem_p (hyp_sem);
  char * output = NULL;
  if (!strcmp (euca_this_component_name, "cc")) {
    char getstats_cmd[MAX_PATH];
    char *instroot = getenv (EUCALYPTUS_ENV_VAR_NAME);

    if (!instroot) {
      snprintf (getstats_cmd, MAX_PATH, EUCALYPTUS_LIBEXEC_DIR "/euca_rootwrap " EUCALYPTUS_DATA_DIR "/getstats_net.pl", "", "");
    } else {
      snprintf (getstats_cmd, MAX_PATH, EUCALYPTUS_LIBEXEC_DIR "/euca_rootwrap " EUCALYPTUS_DATA_DIR "/getstats_net.pl", instroot, instroot);
    }
    output = system_output (getstats_cmd); // invoke th Perl script
    logprintfl (EUCATRACE, "getstats_net.pl output:\n%s\n", output);
  } else if (!strcmp (euca_this_component_name, "nc")) {
    // Right now !CC means the NC.
    output = system_output ("euca_rootwrap getstats.pl"); // invoke th Perl script
    logprintfl (EUCATRACE, "getstats.pl output:\n%s\n", output);
  } else {
    // output will be NULL, so we'll return with an error
    errno = EBADSLT; // using an obscure errno to mean internal error
  }
  if (hyp_sem) sem_v (hyp_sem);

  int ret = ERROR;
  if (output) { // output is a string with one line per measurement, with tab-delimited fields
    char * token, * subtoken;
    char * saveptr1, * saveptr2;
    char * str1 = output;
    getstat ** gss = NULL;
    int ninst = 0;

    for (int i = 1; ; i++, str1 = NULL) { // iterate over lines in output
      token = strtok_r(str1, "\n", &saveptr1); // token points to a whole line
      if (token == NULL)
	break;
      getstat * gs = calloc (1, sizeof (getstat)); // new lines means new data record
      if (gs == NULL)
	goto bail;

      char * str2 = token;
      for (int j = 1; ; j++, str2 = NULL) { // iterate over tab-separated entries in the line
	subtoken = strtok_r(str2, "\t", &saveptr2);
	if (subtoken == NULL)
	  break;

	// e.g. line: i-760B43A1      1347407243789   NetworkIn       summation       total   2112765752
	switch (j) {
	case 1: { // first entry is instance ID
	  getstat * gsp = getstat_find (*pstats, subtoken);
	  if (gsp == NULL) { // first record for this instance => expand pointer array
	    ninst++;
	    gss = realloc (gss, (ninst + 1) * sizeof (getstat *));
	    gss [ninst-1] = gs;
	    gss [ninst] = NULL; // NULL-terminate the array
	    *pstats = gss;
	  } else { // not first record
	    for ( ; gsp->next != NULL; gsp = gsp->next); // walk the linked list to the end
	    gsp->next = gs; // add the new record
	  }
	  strncpy (gs->instanceId, subtoken, sizeof (gs->instanceId));
	  break;
	}
	case 2: {
	  char * endptr;
	  errno = 0;
	  gs->timestamp = strtoll (subtoken, &endptr, 10);
	  if (errno != 0 && *endptr != '\0') {
	    logprintfl (EUCAERROR, "unexpected input from getstats.pl (could not convert timestamp with strtoll())\n");
	    goto bail;
	  }
	  break;
	}
	case 3:
	  strncpy (gs->metricName, subtoken, sizeof (gs->metricName));
	  break;
	case 4:
	  gs->counterType = sensor_str2type (subtoken);
	  break;
	case 5:
	  strncpy (gs->dimensionName, subtoken, sizeof (gs->dimensionName));
	  break;
	case 6: {
	  char * endptr;
	  errno = 0;
	  gs->value = strtod (subtoken, &endptr);
	  if (errno != 0 && *endptr != '\0') {
	    logprintfl (EUCAERROR, "unexpected input from getstats.pl (could not convert value with strtod())\n");
	    goto bail;
	  }
	  break;
	}
	default:
	  logprintfl (EUCAERROR, "unexpected input from getstats.pl (too many fields)\n");
	  goto bail;
	}
      }
    }
    ret = OK;
    goto done;

  bail:
    getstat_free(*pstats);
  done:
    free (output);
  } else {
    logprintfl (EUCAWARN, "failed to invoke getstats for sensor data (%s)\n", strerror (errno));
  }

  return ret;
}

// Never-returning function that performs polling of sensors and updates
// their 'resources' while holding the 'sem'. This may be called from
// sensor_init() directly or via a thread
static void sensor_bottom_half (void)
{
    assert(sensor_state!=NULL && state_sem!=NULL);

    char resourceNames [MAX_SENSOR_RESOURCES][MAX_SENSOR_NAME_LEN];
    char resourceAliases [MAX_SENSOR_RESOURCES][MAX_SENSOR_NAME_LEN];
    for (int i=0; i<MAX_SENSOR_RESOURCES; i++) {
        resourceNames [i][0]='\0';
        resourceAliases [i][0]='\0';
    }

    for (;;) {
        usleep (next_sleep_duration_usec);

        if (sensor_update_euca_config) {
            logprintfl (EUCATRACE, "calling sensor_update_euca_config()...\n");
            sensor_update_euca_config ();
        } else {
            logprintfl (EUCATRACE, "NOT calling sensor_update_euca_config()...\n");
        }
        boolean skip = FALSE;
        sem_p (state_sem);
        if (sensor_state->collection_interval_time_ms == 0 ||
            sensor_state->history_size == 0) {
            skip = TRUE;
        }
        sem_v (state_sem);

        if (skip)
            continue;

        // refresh local copy of resource names & aliases
        sem_p (state_sem);
        for (int i=0; i<sensor_state->max_resources && i<MAX_SENSOR_RESOURCES; i++) {
            strncpy (resourceNames[i], sensor_state->resources[i].resourceName, MAX_SENSOR_NAME_LEN);
            strncpy (resourceAliases[i], sensor_state->resources[i].resourceAlias, MAX_SENSOR_NAME_LEN);
            if (strlen (resourceNames[i]) && strlen(resourceAliases[i])) {
                logprintfl (EUCATRACE, "Found alias '%s' for resource '%s'\n",
                            resourceAliases[i], resourceNames[i]);
            }
        }

        sem_v (state_sem);

        sensor_refresh_resources (resourceNames, resourceAliases,
                                  MAX_SENSOR_RESOURCES);
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
    sensor_state->last_polled = 0;
    sensor_state->interval_polled = 0;
    for (int i=0; i<resources_size; i++) {
        sensorResource * sr = sensor_state->resources + i;
        bzero (sr, sizeof (sensorResource));
    }
    sensor_state->initialized = TRUE; // inter-process init done
    logprintfl (EUCADEBUG, "initialized sensor shared memory\n");
}

__inline__ static boolean is_empty_sr (const sensorResource * sr)
{
    return (sr == NULL || sr->resourceName[0] == '\0');
}

// This must be called from within a state_sem lock--it doesn't do its
// own locking.
static int sensor_expire_cache_entries (void)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    logprintfl(EUCADEBUG, "invoked\n");

    int ret = 0; // returns the number of cache entries expired.
    time_t t = time(NULL);

    for (int r = 0; r < sensor_state->max_resources; r++) {
	sensorResource *sr = sensor_state->resources + r;

	if (is_empty_sr(sr))
	    continue;

	if (! sr->timestamp) {
	    logprintfl(EUCATRACE, "resource %s does not yet have an update timestamp, skipping...\n", sr->resourceName);
	    continue;
	}
	time_t timestamp_age = t - sr->timestamp;
	time_t cache_timeout = sensor_state->interval_polled * CACHE_EXPIRY_MULTIPLE_OF_POLLING_INTERVAL;

	logprintfl(EUCATRACE, "resource %ss, timestamp %ds, timeout (%ds * %ds), age %ds\n", sr->resourceName, sr->timestamp, sensor_state->interval_polled, CACHE_EXPIRY_MULTIPLE_OF_POLLING_INTERVAL, timestamp_age);

	if (timestamp_age > cache_timeout) {
	    logprintfl(EUCAINFO, "expiring resource %s from sensor cache, no update in %d seconds, timeout is %d seconds\n", sr->resourceName, timestamp_age, cache_timeout);
	    sr->resourceName[0] = '\0'; // marks the slot as empty
	    ret++;
	}
    }
    return ret;
}

// Sensor subsystem initialization routine, which must be called before
// all state-full sensor_* functions. If 'sem' and 'resources' are set,
// this function will use them. Otherwise, this function will allocate
// memory and will perform data collection in a thread. If run_bottom_half
// is TRUE, the logic normally running in a background thread will be
// executed synchronously, causing sensor_init() to never return.
int sensor_init (sem * sem, sensorResourceCache * resources, int resources_size, boolean run_bottom_half, int (*update_euca_config_function)(void))
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
        logprintfl (EUCADEBUG, "setting sensor_update_euca_config: %s\n", update_euca_config_function ? "TRUE" : "NULL");
        sensor_update_euca_config = update_euca_config_function;
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

        sensor_state = calloc (sizeof(sensorResourceCache)+sizeof(sensorResource), (use_resources_size-1));
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

int sensor_set_hyp_sem (sem * sem)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    sem_p (state_sem);
    hyp_sem = sem;
    sem_v (state_sem);

    return 0;
}

int sensor_get_config (int *history_size, long long * collection_interval_time_ms)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    sem_p (state_sem);
    * history_size = sensor_state->history_size;
    * collection_interval_time_ms = sensor_state->collection_interval_time_ms;
    sem_v (state_sem);

    return 0;
}

int sensor_get_num_resources (void)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return -1;

    int num_resources;
    sem_p (state_sem);
    num_resources = sensor_state->used_resources;
    sem_v (state_sem);

    return num_resources;
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

int sensor_res2str (char * buf, int bufLen, sensorResource **srs, int srsLen)
{
    char * s = buf;
    int left = bufLen-1;
    int printed;

    for (int r=0; r<srsLen; r++) {
        const sensorResource * sr = srs [r];
        if (is_empty_sr (sr))
            continue;
        printed = snprintf (s, left, "resource: %s uuid: %s type: %s metrics: %d\n", sr->resourceName, sr->resourceUuid, sr->resourceType, sr->metricsLen);
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
                    printed = snprintf (s, left, "\t\t\tdimension: %s values: %d firstValueIndex: %d\n", sd->dimensionName, sd->valuesLen, sd->firstValueIndex);
                    MAYBE_BAIL
                    for (int v=0; v<sd->valuesLen; v++) {
                        const int i = (sd->firstValueIndex + v) % MAX_SENSOR_VALUES;
                        const sensorValue * sv = sd->values + i;
                        const long long sn = sc->sequenceNum + v;
                        printed = snprintf (s, left, "\t\t\t\t[%02d] %05lld %014lld %s %f\n", i, sn, sv->timestampMs, sv->available?"YES":" NO", sv->available?sv->value:-1);
                        MAYBE_BAIL
                    }
                }
            }
        }
    }
    * s = '\0';

    return 0;
}

static void log_sensor_resources (const char * name, const sensorResource ** srs, int srsLen)
{
    char buf [1024*1024];
    if (sensor_res2str (buf, sizeof (buf), srs, srsLen) != 0) {
        logprintfl (EUCAERROR, "failed to print sensor resources (%s)\n", name);
    } else {
        logprintfl (EUCAINFO, "sensor resources (%s) BEGIN\n%ssensor resources END\n", name, buf);
    }
}

int sensor_get_dummy_instance_data (long long sn, const char * instanceId, const char ** sensorIds, int sensorIdsLen, sensorResource ** srs, int srsLen) // TODO3.2: move this into _UNIT_TEST
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
                                    { .timestampMs = 1344056970424, .value = 666, .available = 1 },
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
                        .sequenceNum = sn,
                        .dimensionsLen = 3,
                        .dimensions = {
                            {
                                .dimensionName = "root",
                                .valuesLen = 3,
                                .values = {
                                    { .timestampMs = 1344056910424 + sn, .value = 111.0 + sn, .available = 1 },
                                    { .timestampMs = 1344056910425 + sn, .value = 112.0 + sn, .available = 1 },
                                    { .timestampMs = 1344056910426 + sn, .value = 113.0 + sn, .available = 1 },
                                }
                            },
                            {
                                .dimensionName = "ephemeral0",
                                .valuesLen = 3,
                                .values = {
                                    { .timestampMs = 1344056910424 + sn, .value = 1111.0 + sn, .available = 1 },
                                    { .timestampMs = 1344056910425 + sn, .value = 1112.0 + sn, .available = 1 },
                                    { .timestampMs = 1344056910426 + sn, .value = 1113.0 + sn, .available = 1 },
                                }
                            },
                            {
                                .dimensionName = "vol-34567",
                                .valuesLen = 3,
                                .values = {
                                    { .timestampMs = 1344056910424 + sn, .value = 11111.0 + sn, .available = 1 },
                                    { .timestampMs = 1344056910425 + sn, .value = 11112.0 + sn, .available = 1 },
                                    { .timestampMs = 1344056910426 + sn, .value = 11113.0 + sn, .available = 1 },
                                }
                            }
                        }
                    }
                }
            }
        }
    };
    assert (srsLen>0);
    sensorResource * sr = srs[0];
    memcpy (sr, &example, sizeof(sensorResource));
    strncpy (sr->resourceName, instanceId, sizeof(sr->resourceName));

    return 0;
}

static sensorResource * find_or_alloc_sr (const boolean do_alloc, const char * resourceName, const char * resourceType, const char * resourceUuid)
{
    // sanity check
    if (sensor_state->max_resources < 0 || sensor_state->max_resources > MAX_SENSOR_RESOURCES_HARD) {
        logprintfl (EUCAERROR, "inconsistency in sensor database (max_resources=%d for %s)\n",
                    sensor_state->max_resources, resourceName);
        return NULL;
    }

    sensorResource * unused_sr = NULL;
    for (int r=0; r<sensor_state->max_resources; r++) {
        sensorResource * sr = sensor_state->resources + r;

        if (is_empty_sr (sr)) { // unused slot
            // remember the first unused slot in case we do not find this resource
            if (unused_sr == NULL) {
                unused_sr = sr;
            }
            continue;
        }

        // we have a match
        if ((strcmp (sr->resourceName,  resourceName) == 0) ||
            (strcmp (sr->resourceAlias, resourceName) == 0)) {
            if (resourceType) {
                if (strcmp (sr->resourceType, resourceType) == 0) {
                    return sr;
                }
            }
            return sr;
        }
    }

    if (! do_alloc)
        return NULL;
    if (resourceType==NULL) // must be set for allocation
        return NULL;

    // fill out the new slot
    if (unused_sr != NULL) {
        bzero (unused_sr, sizeof (sensorResource));
        strncpy (unused_sr->resourceName, resourceName, sizeof (unused_sr->resourceName));
        if (resourceType)
            strncpy (unused_sr->resourceType, resourceType, sizeof (unused_sr->resourceType));
        if (resourceUuid)
            strncpy (unused_sr->resourceUuid, resourceUuid, sizeof (unused_sr->resourceUuid));
        sensor_state->used_resources++;
        logprintfl (EUCADEBUG, "allocated new sensor resource %s\n", resourceName);
    }

    return unused_sr;
}

static sensorMetric * find_or_alloc_sm (const boolean do_alloc, sensorResource * sr, const char * metricName)
{
    // sanity check
    if (sr->metricsLen<0 || sr->metricsLen>MAX_SENSOR_METRICS) {
        logprintfl (EUCAERROR, "inconsistency in sensor database (metricsLen=%d for %s)\n",
                    sr->metricsLen, sr->resourceName);
        return NULL;
    }

    for (int m=0; m < sr->metricsLen; m++) {
        sensorMetric * sm = sr->metrics + m;
        if (strcmp (sm->metricName, metricName) == 0) {
            return sm;
        }
    }
    if (! do_alloc // did not find it
        || sr->metricsLen==MAX_SENSOR_METRICS) // out of room
        return NULL;

    // fill out the new slot
    sensorMetric * sm = sr->metrics + sr->metricsLen;
    bzero (sm, sizeof (sensorMetric));
    strncpy (sm->metricName, metricName, sizeof (sm->metricName));
    sr->metricsLen++;
    logprintfl (EUCADEBUG, "allocated new sensor metric %s:%s\n", sr->resourceName, sm->metricName);

    return sm;
}

static sensorCounter * find_or_alloc_sc (const boolean do_alloc, sensorMetric * sm, const int counterType)
{
    // sanity check
    if (sm->countersLen<0 || sm->countersLen>MAX_SENSOR_COUNTERS) {
        logprintfl (EUCAERROR, "inconsistency in sensor database (countersLen=%d for %s)\n",
                    sm->countersLen, sm->metricName);
        return NULL;
    }

    for (int c=0; c < sm->countersLen; c++) {
        sensorCounter * sc = sm->counters + c;
        if (sc->type == counterType) {
            return sc;
        }
    }

    if (! do_alloc // did not find it
        || sm->countersLen==MAX_SENSOR_COUNTERS) // out of room
        return NULL;

    // fill out the new slot
    sensorCounter * sc = sm->counters + sm->countersLen;
    bzero (sc, sizeof (sensorCounter));
    sc->type = counterType;
    sm->countersLen++;
    logprintfl (EUCADEBUG, "allocated new sensor counter %s:%s\n", sm->metricName, sensor_type2str(sc->type));

    return sc;
}

static sensorDimension * find_or_alloc_sd (const boolean do_alloc, sensorCounter * sc, const char * dimensionName)
{
    // sanity check
    if (sc->dimensionsLen<0 || sc->dimensionsLen>MAX_SENSOR_DIMENSIONS) {
        logprintfl (EUCAERROR, "inconsistency in sensor database (dimensionsLen=%d for %s)\n",
                    sc->dimensionsLen, sensor_type2str(sc->type));
        return NULL;
    }

    for (int d=0; d < sc->dimensionsLen; d++) {
        sensorDimension * sd = sc->dimensions + d;
        if ((strcmp (sd->dimensionName,  dimensionName) == 0) ||
            (strcmp (sd->dimensionAlias, dimensionName) == 0)) {
            return sd;
        }
    }
    if (! do_alloc // did not find it
        || sc->dimensionsLen==MAX_SENSOR_DIMENSIONS) // out of room
        return NULL;

    // fill out the new slot
    sensorDimension * sd = sc->dimensions + sc->dimensionsLen;
    bzero (sd, sizeof (sensorDimension));
    strncpy (sd->dimensionName, dimensionName, sizeof (sd->dimensionName));
    sc->dimensionsLen++;
    logprintfl (EUCADEBUG, "allocated new sensor dimension %s:%s\n", sensor_type2str(sc->type), sd->dimensionName);

    return sd;
}

// Merges records in srs[] array of pointers (of length srsLen)
// into records in the in-memory sensor values cache.  The merge
// adds new entries at all levels, if necessary (i.e., if the sensor
// is new, if the metric is new, etc.) and skips over values that
// are already in the cache. So it is safe to call it many times
// with the same data - all but the first invocations will have no
// effect.
int sensor_merge_records (const sensorResource * srs[], int srsLen, boolean fail_on_oom)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    // log_sensor_resources ("sensor_merge_records", srs, srsLen);

    logprintfl(EUCADEBUG, "invoked\n");

    int ret = 1;
    sem_p (state_sem);
    for (int r=0; r<srsLen; r++) {
        const sensorResource * sr = srs [r];
        if (is_empty_sr (sr))
            continue;
        sensorResource * cache_sr = find_or_alloc_sr (TRUE, sr->resourceName, sr->resourceType, sr->resourceUuid);
        if (cache_sr == NULL) {
            logprintfl (EUCAWARN, "failed to find space in sensor cache for resource %s\n",
                        sr->resourceName);
            if (fail_on_oom)
                goto bail;
            continue;
        }

        for (int m=0; m<sr->metricsLen; m++) {
            const sensorMetric * sm = sr->metrics + m;
            sensorMetric * cache_sm = find_or_alloc_sm (TRUE, cache_sr, sm->metricName);
            if (cache_sm == NULL) {
                logprintfl (EUCAWARN, "failed to find space in sensor cache for metric %s:%s\n",
                            sr->resourceName, sm->metricName);
                if (fail_on_oom)
                    goto bail;
                continue;
            }

            for (int c=0; c<sm->countersLen; c++) {
                const sensorCounter * sc = sm->counters + c;
                sensorCounter * cache_sc = find_or_alloc_sc (TRUE, cache_sm, sc->type);
                if (cache_sc == NULL) {
                    logprintfl (EUCAWARN, "failed to find space in sensor cache for counter %s:%s:%s\n",
                                sr->resourceName, sm->metricName, sensor_type2str(sc->type));
                    if (fail_on_oom)
                        goto bail;
                    continue;
                }

                // update the collection interval
                if (sc->collectionIntervalMs > 0) {
                    cache_sc->collectionIntervalMs = sc->collectionIntervalMs;
                }

                // run through dimensions merging in their values separately
                long long dimension_seq [MAX_SENSOR_DIMENSIONS] = { 0 };
                long long largest_seq = -1L;
                for (int d=0; d<sc->dimensionsLen; d++) {
                    const sensorDimension * sd = sc->dimensions + d;
                    sensorDimension * cache_sd = find_or_alloc_sd (TRUE, cache_sc, sd->dimensionName);
                    if (cache_sd == NULL) {
                        logprintfl (EUCAWARN, "failed to find space in sensor cache for dimension %s:%s:%s:%s\n",
                                    sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
                        if (fail_on_oom)
                            goto bail;
                        continue;
                    }

                    if (sd->valuesLen < 1) // no values in this dimension at all
                        continue;

                    if (cache_sd->valuesLen<0 || cache_sd->valuesLen>MAX_SENSOR_VALUES) { // sanity check
                        logprintfl (EUCAERROR, "inconsistency in sensor database (valuesLen=%d for %s)\n",
                                    cache_sd->valuesLen, cache_sr->resourceName, cache_sm->metricName, sensor_type2str(cache_sc->type), cache_sd->dimensionName);
                        goto bail;
                    }

                    // correlate new values with values already in the cache:
                    // phase 1: go backwards through sequence numbers of new and old

                    int inv_start = -1; // input start logical index for copying of new values
                    int iov = cache_sd->valuesLen - 1; // logical index for old values, starting with the latest
                    int iov_start = iov + 1; // cache start logical index for receiving new values
                    for (int inv = sd->valuesLen - 1; inv >= 0; inv--) { // logical index for new values, starting with the latest
                        long long sov = cache_sc->sequenceNum + iov; // seq for old values
                        long long snv = sc->sequenceNum       + inv; // seq for new values

                        if (snv < sov) { // the last new seq number is behind the last old seq number
                            // this can happen when sensor resets; if, additionally,
                            // network outage prevented delivery for a while, there
                            // may also be a gap in numbers, rather than a reset to 0
                            logprintfl (EUCAINFO, "reset in sensor values detected, clearing history for %s:%s:%s:%s\n",
                                        sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
			    logprintfl (EUCADEBUG, "cached valuesLen=%d seq=%lld+%d vs new valuesLen=%d seq=%lld+%d\n",
					cache_sd->valuesLen, cache_sc->sequenceNum, iov, sd->valuesLen, sc->sequenceNum, inv);
                            inv_start = 0;                           // copy all new values
                            iov_start = 0;                           // overwrite what is in cache
                            cache_sc->sequenceNum = sc->sequenceNum; // reset the seq number to the new value
                            break;
                        }

                        if (snv > sov) { // new data, so include it in the list to copy
                            inv_start = inv;
                            continue;
                        }

                        if (iov<0) // no more old, cached data to compare against
                            continue;

                        // the rest of this is for internal checking - the old and new values must match
                        int vn_adj = (inv + sd->firstValueIndex) % MAX_SENSOR_VALUES; // values adjusted for firstValueIndex
                        int vo_adj = (iov + cache_sd->firstValueIndex) % MAX_SENSOR_VALUES;
                        if ((sd->values[vn_adj].timestampMs != cache_sd->values[vo_adj].timestampMs)
                            || (sd->values[vn_adj].available != cache_sd->values[vo_adj].available)
                            || (sd->values[vn_adj].value != cache_sd->values[vo_adj].value)) {
                            logprintfl (EUCAWARN, "mismatch in sensor data being merged into in-memory cache, clearing history for %s:%s:%s:%s\n",
                                        sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
                            inv_start = 0;
                            iov_start = 0;
                            break;
                        }

                        iov--;
                    }

                    // step 2: if there is new data, copy it into the right place

                    long long seq = cache_sc->sequenceNum; // current value
                    if (inv_start >= 0) { // there is new data to copy
                        int iov = iov_start;
                        int copied = 0;
                        for (int inv = inv_start; inv < sd->valuesLen; inv++, iov++) {
                            int vn_adj = (inv + sd->firstValueIndex) % MAX_SENSOR_VALUES; // values adjusted for firstValueIndex
                            int vo_adj = (iov + cache_sd->firstValueIndex) % MAX_SENSOR_VALUES;
                            cache_sd->values[vo_adj].timestampMs = sd->values[vn_adj].timestampMs;
                            cache_sd->values[vo_adj].available   = sd->values[vn_adj].available;
                            cache_sd->values[vo_adj].value       = sd->values[vn_adj].value;
                            copied++;
                        }
                        // adjust the length capping it at array size
                        cache_sd->valuesLen = iov_start + copied;
                        if (cache_sd->valuesLen > MAX_SENSOR_VALUES) {
                            cache_sd->valuesLen = MAX_SENSOR_VALUES;
                        }
                        // shift the first entry's seq number up if the values wrapped
                        seq = seq + (iov_start + copied) - cache_sd->valuesLen;

                        // shift the first entry's index up if the values wrapped
                        cache_sd->firstValueIndex = (cache_sd->firstValueIndex + (iov_start + copied) - cache_sd->valuesLen) % MAX_SENSOR_VALUES;

                        // update the interval now (TODO: should we base it on the delta between the last two values?)
                        cache_sc->collectionIntervalMs = sensor_state->collection_interval_time_ms;
                    }

                    // record sequence number for possible later adjustment across dimensions
                    dimension_seq [d] = seq;
                    if (largest_seq < seq) {
                        largest_seq = seq;
                    }
                }

                if (largest_seq > cache_sc->sequenceNum) { // will have to change the sequence number
                    cache_sc->sequenceNum = largest_seq;
                    for (int d=0; d<sc->dimensionsLen; d++) {
                        if (dimension_seq [d] < largest_seq) {
                            sensorDimension * peer_sd = cache_sc->dimensions + d;
                            int delta = largest_seq - dimension_seq [d];
                            peer_sd->valuesLen -= delta;
                            if (peer_sd->valuesLen < 0)
                                peer_sd->valuesLen = 0;
                            peer_sd->firstValueIndex = (peer_sd->firstValueIndex + delta) % MAX_SENSOR_VALUES;
                        }
                    }
                }
            }
        }
        cache_sr->timestamp = time(NULL);
        logprintfl(EUCATRACE, "updated %s cache timestamp to %d\n", cache_sr->resourceName, cache_sr->timestamp);
    }
    ret = 0;

 bail:

    sem_v (state_sem);

    return ret;
}

// Adds a single value into the in-memory sensor cache. This is
// implemented by constructing a whole big sensorResource record
// just for one value and merging it. The advantage of this
// approach is that all additions are done by the merging code.
int sensor_add_value (const char * instanceId,
                      const char * metricName,
                      const int counterType,
                      const char * dimensionName,
                      const long long sequenceNum,
                      const long long timestampMs,
                      const boolean available,
                      const double value)
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
                        .sequenceNum = sequenceNum,
                        .dimensionsLen = 1,
                        .dimensions = {
                            {
                                .valuesLen = 1
                            }
                        }
                    }
                }
            }
        }
    };
    strncpy (sr.resourceName, instanceId, sizeof (sr.resourceName));
    sensorMetric * sm = sr.metrics; // use array entry [0]
    strncpy (sm->metricName, metricName, sizeof (sm->metricName));
    sensorCounter * sc = sm->counters; // use array entry [0]
    sc->type = counterType;
    sensorDimension * sd = sc->dimensions; // use array entry [0]
    strncpy (sd->dimensionName, dimensionName, sizeof (sd->dimensionName));
    sensorValue * sv = sd->values; // use array entry [0]
    sv->timestampMs = timestampMs;
    sv->value = value;
    sv->available = available;

    sensorResource * srs [1] = { &sr };

    logprintfl (EUCADEBUG, "adding %s:%s:%s:%s %05lld %014lld %s %f\n",
		sr.resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName,
		sequenceNum, sv->timestampMs, sv->available?"YES":" NO", sv->available?sv->value:-1);
    return sensor_merge_records (srs, 1, TRUE);
}

// A function for getting the latest value for a particular
// (resource x metric x counter x dimension) value, along with
// various related values, such as intervalMs and total number
// of values in the cache. Given that most users will prefer
// the bulk retrieval funcion sensor_get_instance_data(), this
// one is more likely of use only for debugging.
int sensor_get_value (const char * instanceId,
                      const char * metricName,
                      const int counterType,
                      const char * dimensionName,
                      long long * sequenceNum,
                      long long * timestampMs,
                      boolean * available,
                      double * value,
                      long long * intervalMs,
                      int * valLen)
{
    int ret = 1;
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    sem_p (state_sem);
    sensorResource * cache_sr = find_or_alloc_sr (FALSE, instanceId, "instance", NULL);
    if (cache_sr == NULL)
        goto bail;

    sensorMetric * cache_sm = find_or_alloc_sm (FALSE, cache_sr, metricName);
    if (cache_sm == NULL)
        goto bail;

    sensorCounter * cache_sc = find_or_alloc_sc (FALSE, cache_sm, counterType);
    if (cache_sc == NULL)
        goto bail;

    sensorDimension * cache_sd = find_or_alloc_sd (FALSE, cache_sc, dimensionName);
    if (cache_sd == NULL)
        goto bail;

    if (cache_sd->valuesLen < 1) // no values in this dimension at all
        goto bail;

    * sequenceNum = cache_sc->sequenceNum + cache_sd->valuesLen - 1;
    * intervalMs = cache_sc->collectionIntervalMs;
    * valLen = cache_sd->valuesLen;

    sensorValue * sv = cache_sd->values + ((cache_sd->firstValueIndex + cache_sd->valuesLen - 1) % MAX_SENSOR_VALUES);
    * timestampMs = sv->timestampMs;
    * available = sv->available;
    * value = sv->value;
    ret = 0;

 bail:

    sem_v (state_sem);
    return ret;
}

int sensor_get_instance_data (const char * instanceId, const char ** sensorIds, int sensorIdsLen, sensorResource ** sr_out, int srLen)
{
    int ret = 1;
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    logprintfl (EUCATRACE,
		"sensor_get_instance_data() called for instance %s\n",
		instanceId == NULL ? "NULL" : instanceId);

    sem_p (state_sem);
    time_t this_interval = 0; // For determining polling interval.
    int sri = 0; // index into output array sr_out[]
    for (int r=0; r<sensor_state->max_resources; r++) {
        sensorResource * sr = sensor_state->resources + r;

        if (is_empty_sr (sr)) // unused slot in cache, skip it
            continue;

        if ((instanceId != NULL) // we are looking for a specific instance (rather than all)
            && (strcmp (sr->resourceName, instanceId) != 0)) // and this is not the one
            continue;

        if (sensorIdsLen>0) // TODO: implement support for sensorIds[]
            goto bail;

        if (sri>=srLen) // out of room in output
            goto bail;  // FIXME: Log something here?

        memcpy (sr_out[sri], sr, sizeof (sensorResource)); // TODO: run through the data, do not just copy
        sri++;

        if (instanceId != NULL) // only one instance to copy
            break;
    }
    if (sri>0) // we have at least one result
        ret = 0;

 bail:

    if (sensor_state->last_polled) { // Ensure this isn't the first one.
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
	    logprintfl(EUCATRACE, "NOT adjusting measured upstream polling interval from %d to %d (which is below threshold)\n", sensor_state->interval_polled, this_interval);
	    sensor_state->last_polled = t;
	} else {
	    if (this_interval == sensor_state->interval_polled) {
		logprintfl(EUCATRACE, "maintaining measured upstream polling interval of %d\n", sensor_state->interval_polled);
	    } else {
		if (sensor_state->interval_polled) {
		    logprintfl(EUCATRACE, "adjusting measured upstream polling interval from %d to %d\n", sensor_state->interval_polled, this_interval);
		} else {
		    logprintfl(EUCATRACE, "setting measured upstream polling interval to %d\n", this_interval);
		}
		sensor_state->interval_polled = this_interval;
	    }
	    sensor_state->last_polled = t;
	}
    } else {
	logprintfl(EUCATRACE, "first poll--setting baseline for measuring upstream polling interval\n");
	sensor_state->last_polled = time(NULL);
    }
    if (this_interval > 5) {
	// Only do this if at least the minimum interval has
	// passed--prevents trying to expire the cache several times in
	// one polling cycle when we get clumped requests.
	int num_expired = sensor_expire_cache_entries ();
	if (num_expired) {
	    logprintfl(EUCAINFO, "%d resource entries expired from sensor cache\n", num_expired);
	}
    }

    sem_v (state_sem);
    return ret;
}

int sensor_add_resource (const char * resourceName, const char * resourceType, const char * resourceUuid)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    int ret = 1;
    sem_p (state_sem);
    if (find_or_alloc_sr (TRUE, resourceName, resourceType, resourceUuid) != NULL) {
        ret = 0;
    }
    sem_v (state_sem);

    return ret;
}

int sensor_set_resource_alias (const char * resourceName, const char * resourceAlias)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    int ret = 1;
    sem_p (state_sem);
    sensorResource * sr = find_or_alloc_sr (FALSE, resourceName, NULL, NULL);
    if (sr != NULL) {
        if (resourceAlias) {
            if (strcmp (sr->resourceAlias, resourceAlias) != 0) {
                safe_strncpy (sr->resourceAlias, resourceAlias, sizeof (sr->resourceAlias));
                logprintfl (EUCADEBUG, "set alias for sensor resource %s to %s\n", resourceName, resourceAlias);
            }
        } else {
            logprintfl (EUCATRACE, "clearing alias for resource '%s'\n", resourceName);
            sr->resourceAlias [0] = '\0'; // clears the alias
        }
        ret = 0;
    }
    sem_v (state_sem);

    return ret;
}

int sensor_remove_resource (const char * resourceName)
{
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    int ret = 1;
    sem_p (state_sem);
    sensorResource * sr = find_or_alloc_sr (FALSE, resourceName, NULL, NULL);
    if (sr != NULL) {
        sr->resourceName [0] = '\0'; // marks the slot as empty
        ret = 0;
    }
    sem_v (state_sem);

    return ret;
}

int sensor_set_dimension_alias (const char * resourceName,
                                const char * metricName,
                                const int counterType,
                                const char * dimensionName,
                                const char * dimensionAlias)
{
    int ret = 1;
    if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

    sem_p (state_sem);

    // do not allocate resource structure here
    // (it should be done prior to calling this function,
    // by somebody who knows resource type and uuid)
    sensorResource * sr = find_or_alloc_sr (FALSE, resourceName, NULL, NULL);
    if (sr == NULL)
        goto bail;

    sensorMetric * sm = find_or_alloc_sm (TRUE, sr, metricName); // allocate metric if necessary
    if (sm == NULL)
        goto bail;

    sensorCounter * sc = find_or_alloc_sc (TRUE, sm, counterType); // allocate counter if necessary
    if (sc == NULL)
        goto bail;

    sensorDimension * sd = find_or_alloc_sd (TRUE, sc, dimensionName); // allocate dimension if necessary
    if (sd == NULL)
        goto bail;

    boolean changed = FALSE;
    if (dimensionAlias) {
        if (strcmp (sd->dimensionAlias, dimensionAlias) != 0) {
            safe_strncpy (sd->dimensionAlias, dimensionAlias, sizeof (sd->dimensionAlias));
            changed = TRUE;
        }
    } else {
        if (strlen (sd->dimensionAlias) > 0) {
            sd->dimensionAlias [0] = '\0'; // clear the alias
            changed = TRUE;
        }
    }
    if (changed) {
        logprintfl (EUCADEBUG, "set alias for sensor dimension %s:%s:%s:%s to '%s'\n",
                    resourceName, metricName, sensor_type2str(counterType), dimensionName, sd->dimensionAlias);
    }

    ret = 0;

 bail:

    sem_v (state_sem);
    return ret;
}

int sensor_set_volume (const char * instanceId, const char * volumeId, const char * guestDev)
{
    int ret = 0;

    ret += sensor_set_dimension_alias (instanceId, "DiskReadOps",          SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias (instanceId, "DiskWriteOps",         SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias (instanceId, "DiskReadBytes",        SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias (instanceId, "DiskWriteBytes",       SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias (instanceId, "VolumeTotalReadTime",  SENSOR_SUMMATION, volumeId, guestDev);
    ret += sensor_set_dimension_alias (instanceId, "VolumeTotalWriteTime", SENSOR_SUMMATION, volumeId, guestDev);

    return ret;
}

// request to explicitly refresh sensor values for a
// particular resource (useful for getting data between
// poll events in bottom_half(), which may be spaced far
// apart)
int sensor_refresh_resources (const char resourceNames [][MAX_SENSOR_NAME_LEN], const char resourceAliases [][MAX_SENSOR_NAME_LEN], int size)
{
  if (sensor_state == NULL || sensor_state->initialized == FALSE) return 1;

  getstat ** stats = NULL;
  if (getstat_generate (&stats) != OK) {
    logprintfl (EUCAWARN, "failed to invoke getstats for sensor data\n");
    return 1;
  } else {
    logprintfl (EUCADEBUG, "polled statistics for %d instance(s)\n", getstat_ninstances(stats));
  }

  boolean found_values = FALSE;
  for (int i=0; i<size; i++) {
    char * name = (char *)resourceNames [i];
    char * alias = (char *)resourceAliases [i];
    if (name [0] == '\0')
      continue;
    getstat * head = getstat_find (stats, name);
    if (head == NULL && alias [0] != '\0') {
      // Check for aliased resource.
      head = getstat_find (stats, alias);
    }
    for (getstat * s = head; s != NULL; s = s->next) {
      sensor_add_value (name, s->metricName, s->counterType, s->dimensionName, sn, s->timestamp, TRUE, s->value);
      found_values = TRUE;
    }
    if (head == NULL) {
      // OK, can't find this thing anywhere.
      logprintfl (EUCADEBUG, "unable to get metrics for instance %s (which is OK if it was terminated--the instance should soon expire from the cache)\n", name);
      // TODO3.2: decide what to do when some metrics for an instance
      // aren't available
      //
      // On possibility is that the CLC isn't actively polling us, which
      // means we've not cleaned up the sensor cache recently...and
      // stale/terminated resources have accumulated in it. So force a
      // cache-expiration run.
      sem_p(state_sem); // Must set semaphore for sensor_expire_cache_entries() call.
      time_t t = time(NULL);
      time_t this_interval = t - sensor_state->last_polled;
      if (this_interval > 5) {
	  // Only do this if at least the minimum interval has
	  // passed--prevents trying to expire the cache several times
	  // in one polling cycle when we get clumped requests.
	  int num_expired = sensor_expire_cache_entries ();
	  if (num_expired) {
	      logprintfl(EUCAINFO, "%d resource entries expired from sensor cache\n", num_expired);
	  }
      }
      sem_v(state_sem);

    }
  }
  getstat_free (stats);
  if (found_values)
    sn++;

  return(0);
}

int sensor_validate_resources (sensorResource ** srs, int srsLen)
{
  for (int i=0; i<srsLen; i++) {
    sensorResource * sr = srs [i];
    if (sr == NULL) {
      logprintfl (EUCAERROR, "invalid resource array: empty slot in position %d\n", i);
      return 1;
    }
    if (sr->metricsLen<0 || sr->metricsLen>MAX_SENSOR_METRICS) {
      logprintfl (EUCAERROR, "invalid resource array: metricsLen out of bounds (metricsLen=%d for %s)\n",
		  sr->metricsLen, sr->resourceName);
      return 1;
    }
    for (int m=0; m<sr->metricsLen; m++) {
      const sensorMetric * sm = sr->metrics + m;
      if (sm->countersLen<0 || sm->countersLen>MAX_SENSOR_COUNTERS) {
	logprintfl (EUCAERROR, "invalid resource array: counterLen out of bounds (countersLen=%d for %s:%s)\n",
		    sm->countersLen, sr->resourceName, sm->metricName);
	return 1;
      }
      for (int c=0; c<sm->countersLen; c++) {
	const sensorCounter * sc = sm->counters + c;
	if (sc->dimensionsLen<0 || sc->dimensionsLen>MAX_SENSOR_DIMENSIONS) {
	  logprintfl (EUCAERROR, "invalid resource array: sensorCounter out of bounds (dimensionsLen=%d for %s:%s:%s)\n",
		      sc->dimensionsLen, sr->resourceName, sm->metricName, sensor_type2str(sc->type));
	  return 1;
	}
	for (int d=0; d<sc->dimensionsLen; d++) {
	  const sensorDimension * sd = sc->dimensions + d;
	  if (sd->valuesLen<0 || sd->valuesLen>MAX_SENSOR_VALUES) {
	    logprintfl (EUCAERROR, "invalid resource array: valuesLen out of bounds (valuesLen=%d for %s:%s:%s:%s)\n",
			sd->valuesLen, sr->resourceName, sm->metricName, sensor_type2str(sc->type), sd->dimensionName);
	    return 1;
	  }
	  for (int v=0; v<sd->valuesLen; v++) {
	    // TODO: anything to verify in values?
	  }
	}
      }
    }
  }

  return 0;
}

/////////////////////////////////////////////// unit testing code ///////////////////////////////////////////////////

#ifdef _UNIT_TEST

static void dump_sensor_cache (void)
{
    sensorResource ** srs = calloc (sensor_state->max_resources, sizeof (sensorResource *));
    for (int i=0; i<sensor_state->max_resources; i++) {
        srs [i] = & (sensor_state->resources [i]);
    }
    log_sensor_resources ("whole cache", srs, sensor_state->max_resources);
    free (srs);
}

static void clear_srs (sensorResource ** srs, int srsLen)
{
    for (int i=0; i<srsLen; i++) {
        bzero (srs [i], sizeof (sensorResource));
    }
}

const char * euca_this_component_name   = "ignore";
const char * euca_client_component_name = "ignore";
static long long ts = 0;
static void * competitor_function_writer (void * ptr);
static void * competitor_function_reader (void * ptr);

int main (int argc, char ** argv)
{
    int errors = 0;

    ts = time_usec() / 1000;
    logfile (NULL, EUCATRACE, 4);
    log_prefix_set ("%T %L %t9 %m-24 %F-33 |");
    logprintfl (EUCADEBUG, "testing sensor.c with cache of size 2 and MAX_SENSOR_VALUES=%d\n", MAX_SENSOR_VALUES);

    long long intervalMs = 50000;
    assert (sensor_init (NULL, NULL, 2, FALSE, NULL) == 0);
    assert (sensor_state->max_resources > 0);
    assert (sensor_state->used_resources == 0);
    assert (0 != sensor_config (-1, 5000));
    assert (0 != sensor_config (MAX_SENSOR_VALUES+1, 5000));
    assert (0 != sensor_config (1, 50));
    assert (0 != sensor_config (1, 50000000000));
    assert (0 == sensor_config (0, 50000));
    assert (0 == sensor_config (1, 50000));
    assert (0 == sensor_config (3, intervalMs));

#define GETSTAT_ITERS 10
    // test the getstat function
    getstat ** stats = NULL;
    for (int i=1; i<=GETSTAT_ITERS; i++) {
        if (i%2) {
            euca_this_component_name = "nc";
        } else {
            euca_this_component_name = "cc";
        }
        assert (getstat_generate(&stats)==OK);
        getstat * gs = getstat_find (stats, NULL);
        if (gs != NULL) {
	  char id [MAX_SENSOR_NAME_LEN];
	  strncpy (id, gs->instanceId, sizeof (id));
	  assert (sensor_refresh_resources (id, "", 1) == OK);
        }
        if (i%101 == 0 || i%102 == 0) {
	  logprintfl (EUCADEBUG, "getstat_refresh() iteration %d/%d found %d instances\n", i, GETSTAT_ITERS, getstat_ninstances(stats));
        }
    }
    char * anInstanceId = NULL;
    if (stats) {
      anInstanceId = stats[0]->instanceId;
    }
    if (anInstanceId != NULL)
      assert (getstat_find (stats, anInstanceId) != NULL);

    euca_this_component_name = "ignore"; // set component name so that getstats won't get involved and won't put real sensor readings into sensor state
    getstat_free (stats);
    init_state(2); // clear out sensor state after previous experiments
    assert (0 == sensor_config (3, intervalMs));

    // test sensor_add_value and sensor_get_value
    double val = 11.0;
    for (int j=0; j<50; j++) {
        long long sn = -1L;
        int valLen = 0;
        for (int i=0; i<((MAX_SENSOR_VALUES/2)*(j+1)); i++) {
            sn+=1;
            ts+=intervalMs;
            val+=1;
            assert (0 == sensor_add_value ("i-555", "CPUUtilization", SENSOR_AVERAGE, "default", sn, ts, (sn%2)?TRUE:FALSE, val));
            assert (0 == sensor_add_value ("i-555", "CPUUtilization", SENSOR_AVERAGE, "default", sn, ts, (sn%2)?TRUE:FALSE, val)); // should be a no-op
            valLen++;
            if (valLen>MAX_SENSOR_VALUES)
                valLen=MAX_SENSOR_VALUES;

            {
                long long last_sn;
                long long last_ts;
                boolean last_available;
                double last_val;
                long long last_intervalMs;
                int last_valLen;

                assert (0 == sensor_get_value ("i-555", "CPUUtilization", SENSOR_AVERAGE, "default",
                                               &last_sn, &last_ts, &last_available, &last_val, &last_intervalMs, &last_valLen));
                assert (last_sn == sn);
                assert (last_ts == ts);
                if (! (last_intervalMs == intervalMs)) {
		  logprintfl (EUCAERROR, "bad\n");
		}
                assert (last_available == (sn%2)?TRUE:FALSE);
                assert (last_val == val);
                assert (last_valLen == valLen);
            }
        }
        intervalMs += 50000;
        assert (0 == sensor_config (3, intervalMs));
    }

    // add the "dummy" struct as a second resource
    sensorResource ** srs = calloc (sensor_state->max_resources, sizeof (sensorResource *));
    assert (srs);
    for (int i=0; i<sensor_state->max_resources; i++) {
        srs [i] = calloc (1, sizeof (sensorResource));
        assert (srs [i]);
    }
    assert (0 == sensor_get_dummy_instance_data (0L, "i-666", NULL, 0, srs, sensor_state->max_resources));
    assert (0 == sensor_merge_records (srs, sensor_state->max_resources, TRUE));
    assert (0 == sensor_merge_records (srs, sensor_state->max_resources, TRUE)); // should be a no-op

    assert (0 == sensor_get_dummy_instance_data (1L, "i-666", NULL, 0, srs, sensor_state->max_resources)); // will merge in last value
    assert (0 == sensor_merge_records (srs, sensor_state->max_resources, TRUE));
    assert (0 == sensor_merge_records (srs, sensor_state->max_resources, TRUE)); // should be a no-op

    assert (0 == sensor_get_dummy_instance_data (3L, "i-666", NULL, 0, srs, sensor_state->max_resources)); // will merge in two last values
    assert (0 == sensor_merge_records (srs, sensor_state->max_resources, TRUE));
    assert (0 == sensor_merge_records (srs, sensor_state->max_resources, TRUE)); // should be a no-op

    assert (0 == sensor_get_dummy_instance_data (6L, "i-666", NULL, 0, srs, sensor_state->max_resources)); // will append three values
    assert (0 == sensor_merge_records (srs, sensor_state->max_resources, TRUE));
    assert (0 == sensor_merge_records (srs, sensor_state->max_resources, TRUE)); // should be a no-op

    { // verify the last value
        long long last_sn;
        long long last_ts;
        boolean last_available;
        double last_val;
        long long last_intervalMs;
        int last_valLen;

        assert (0 == sensor_get_value ("i-666", "DiskReadOps", SENSOR_SUMMATION, "root",
        &last_sn, &last_ts, &last_available, &last_val, &last_intervalMs, &last_valLen));
        assert (last_sn == 8L);
        assert (last_available == TRUE);
    }

    // go to the limits of the sensorResource(Cache) structs
    assert (0 != sensor_add_value ("i-777", "CPUUtilization", SENSOR_AVERAGE, "default", 0, 0, TRUE, 0)); // too many resources (only requested 2)
    assert (0 != sensor_add_value ("i-666", "MadeUpMetric", SENSOR_SUMMATION, "root", 0, 0, TRUE, 0)); // exceeding MAX_SENSOR_METRICS (2)
    assert (0 != sensor_add_value ("i-666", "DiskReadOps", SENSOR_AVERAGE, "root", 0, 0, TRUE, 0)); // exceeding MAX_SENSOR_COUNTERS (1)
    assert (0 != sensor_add_value ("i-666", "DiskReadOps", SENSOR_SUMMATION, "another", 0, 0, TRUE, 0)); // exceeding MAX_SENSOR_DIMENSIONS (3)

    dump_sensor_cache();

    int srsLen = sensor_state->max_resources;
    logprintfl (EUCADEBUG, "testing sensor_get_instance_data() function\n");
    clear_srs (srs, srsLen); // clear out the array of structs to use it to retrieve data
    assert (0 == sensor_get_instance_data (NULL, NULL, 0, srs, srsLen));
    assert (0 == sensor_get_instance_data (NULL, NULL, 0, srs, srsLen)); // same
    clear_srs (srs, srsLen); // clear out the array of structs to use it to retrieve data
    assert (0 == sensor_get_instance_data (NULL, NULL, 0, srs, srsLen));
    clear_srs (srs, srsLen); // clear out the array of structs to use it to retrieve data
    assert (0 != sensor_get_instance_data ("i-777", NULL, 0, srs, srsLen));
    assert (0 != sensor_get_instance_data ("i-555", "foo", 1, srs, srsLen));
    assert (0 == sensor_get_instance_data ("i-555", NULL, 0, srs, srsLen));
    assert (0 == sensor_get_instance_data ("i-555", NULL, 0, srs, srsLen)); // same
    log_sensor_resources ("values read from cache", srs, srsLen);

    for (int i=0; i<sensor_state->max_resources; i++) {
        free (srs [i]);
    }
    free (srs);

    dump_sensor_cache();
    logprintfl (EUCADEBUG, "********************************\n");
    logprintfl (EUCADEBUG, "testing with competitive threads\n");
    logprintfl (EUCADEBUG, "********************************\n");
#define COMPETITIVE_PARTICIPANTS 5
#define COMPETITIVE_ITERATIONS 66666
    logprintfl (EUCAINFO, "spawning %d competing threads\n", COMPETITIVE_PARTICIPANTS);
    pthread_t threads [COMPETITIVE_PARTICIPANTS];
    long long thread_par [COMPETITIVE_PARTICIPANTS];
    int thread_par_sum = 0;
    for (int j=0; j<COMPETITIVE_PARTICIPANTS; j++) {
      thread_par [j] = 0; // pass param to thread, if any
      pthread_create (&threads[j],
		      NULL,
		      (j%2==0) ? competitor_function_writer : competitor_function_reader,
		      (void *)&thread_par[j]);
    }
    for (int j=0; j<COMPETITIVE_PARTICIPANTS; j++) {
      pthread_join (threads[j], NULL);
      thread_par_sum += (int)thread_par [j];
    }
    logprintfl (EUCAINFO, "waited for all competing threads (returned sum=%d)\n", thread_par_sum);
    dump_sensor_cache();
    assert (thread_par_sum == 0);

    return 0;
}

static void * competitor_function_reader (void * ptr)
{
    long long param = * (long long *) ptr;
    int errors = 0;

    logprintfl (EUCADEBUG, "competitor reader running with param=%lld\n", param);

    sensorResource ** srs = calloc (sensor_state->max_resources, sizeof (sensorResource *));
    int srsLen = sensor_state->max_resources;
    assert (srs);
    for (int i=0; i<sensor_state->max_resources; i++) {
        srs [i] = calloc (1, sizeof (sensorResource));
        assert (srs [i]);
    }

    for (int j=0; j<COMPETITIVE_ITERATIONS; j++) {
      assert (0 == sensor_get_instance_data (NULL, NULL, 0, srs, srsLen));
      // verify the output
      assert (0 == sensor_validate_resources (srs, srsLen));
    }

    for (int i=0; i<sensor_state->max_resources; i++) {
        free (srs [i]);
    }
    free (srs);

    * (long long *) ptr = errors;
    return NULL;
}

static long long _sn = -1;
static double val = 0.0;

static void * competitor_function_writer (void * ptr)
{
    long long param = * (long long *) ptr;
    int errors = 0;

    logprintfl (EUCADEBUG, "competitor writer running with param=%lld\n", param);

    // add the "dummy" struct as a second resource
    sensorResource ** srs = calloc (sensor_state->max_resources, sizeof (sensorResource *));
    assert (srs);
    for (int i=0; i<sensor_state->max_resources; i++) {
      srs [i] = calloc (1, sizeof (sensorResource));
      assert (srs [i]);
    }

    for (int j=0; j<COMPETITIVE_ITERATIONS; j++) {
      _sn+=1;
      ts+=500;
      val+=1;
      char * r = (j%2==0) ? "i-555" : "i-666";
      int err = 0;
      if (j%3==0) {
	//	err = (0==sensor_add_value ("i-777", "CPUUtilization", SENSOR_AVERAGE, "default", _sn, ts, TRUE, val));
      } else {
	err = sensor_add_value (r, "CPUUtilization", SENSOR_AVERAGE, "default", _sn, ts, TRUE, val);
      }
      if (err) {
	logprintfl (EUCAERROR, "sensor_add_value failed err=%d\n", err);
	errors++;
      }
    }

    * (long long *) ptr = errors;
    return NULL;
}
#endif

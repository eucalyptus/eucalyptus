// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//!
//! @file util/stats/message_stats.c
//! Implementation for tracking message statistics on a per-message-type basis
//! This is not the sensor, just the state tracking implementation
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#include "message_stats.h"
#include <eucalyptus.h>
#include <string.h>
#include <log.h>
#include <ipc.h>
#include <json-c/json.h>
#include <math.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*
Stats map is json mapping. Each message is named and has an associated nested map containing the
useful stats. The stats tracked are: count, success_count, failed_count, mean, min, max all in milliseconds of duration. 
Example:
{
  "describeResources": { "min": 10, "max"; 20, "mean":12.3},
  "describeSensors": { "min": 10, "max"; 40, "mean":24.3},
  "runInstance": { "min": 15, "max"; 20, "mean":33.3},
  "attachVolume": { "min": 35, "max"; 100, "mean":76.3}
}
*/

/*
Uses a map as input to the update and fetch systems. This is entirely due to
the memory model of the CC and how it must share memory between processes.

The stats system assumes that the actual stats state is shared memory managed
externally (or not, as in the case of the NC), and is provided directly to these
methods as a json string.
 */

#ifdef _UNIT_TEST
json_object *message_stats_map;
#endif

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/
static double calculate_moving_mean(double current_mean, int current_count, long new_value);

//! Constructs a new json object for the message statistics, but not for any specific message, no name used
static json_object *initialize_new_message_map();

//! Zero the stats for a single message entry.
static void reset_message_stat(json_object *msg_entry);

#ifdef _UNIT_TEST
static int test_moving_average();
static int test_init_message_map();
static int test_update_message_stats();
#endif

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

//! Calculate a cumulative moving averaged using the standard method
static double calculate_moving_mean(double current_mean, int current_count, long new_value) {
    return current_mean + ( ((double)new_value - current_mean ) / (current_count + 1) );
}

//! Returns a new message map for the named message type
//! If cached_map != NULL, reads in that value and initializes the structure with that state.
//! This is necessary for the CC's memory model.
static json_object *initialize_new_message_map() {
    json_object *default_fields = json_object_new_object();
    json_object_object_add(default_fields, MSG_COUNT_KEY, json_object_new_int(MSG_COUNT_INIT));
    json_object_object_add(default_fields, MSG_OK_COUNT_KEY, json_object_new_int(MSG_COUNT_INIT));
    json_object_object_add(default_fields, MSG_FAIL_COUNT_KEY, json_object_new_int(MSG_COUNT_INIT));
    json_object_object_add(default_fields, MSG_MEAN_KEY, json_object_new_double(MSG_MEAN_INIT));
    json_object_object_add(default_fields, MSG_MIN_KEY, json_object_new_int(MSG_MIN_INIT)); //Use -1 since it is invalid measure
    json_object_object_add(default_fields, MSG_MAX_KEY, json_object_new_int(MSG_MAX_INIT));
    return default_fields;
}

//! Resets the counters for the given message entry. Expects that the json object passed
//! is the map of values for a single message entry. Modifies the entry in-place
static void reset_message_stat(json_object *msg_entry) {
    if(msg_entry == NULL || json_object_is_type(msg_entry, json_type_object)) {
        return;
    }

    json_object_object_del(msg_entry, MSG_MEAN_KEY);
    json_object_object_del(msg_entry, MSG_MIN_KEY);
    json_object_object_del(msg_entry, MSG_MAX_KEY);
    json_object_object_del(msg_entry, MSG_COUNT_KEY);
    json_object_object_del(msg_entry, MSG_OK_COUNT_KEY);
    json_object_object_del(msg_entry, MSG_FAIL_COUNT_KEY);

    json_object_object_add(msg_entry, MSG_COUNT_KEY, json_object_new_int(MSG_COUNT_INIT));
    json_object_object_add(msg_entry, MSG_OK_COUNT_KEY, json_object_new_int(MSG_COUNT_INIT));
    json_object_object_add(msg_entry, MSG_FAIL_COUNT_KEY, json_object_new_int(MSG_COUNT_INIT));
    json_object_object_add(msg_entry, MSG_MEAN_KEY, json_object_new_double(MSG_MEAN_INIT));
    json_object_object_add(msg_entry, MSG_MIN_KEY, json_object_new_int(MSG_MIN_INIT)); //Use -1 since it is invalid measure
    json_object_object_add(msg_entry, MSG_MAX_KEY, json_object_new_int(MSG_MAX_INIT));

}

//! Idempotently enable stats
void enable_stats(json_object *stats_state) {
    json_object *enabled_bool = NULL;
    json_object_object_get_ex(stats_state, STATS_ENABLED_KEY, &enabled_bool);
    if(enabled_bool == NULL || 
       ( !json_object_is_type(enabled_bool, json_type_boolean) || 
         json_object_get_boolean(enabled_bool) != TRUE) ) {

        if(enabled_bool != NULL) {
            json_object_object_del(stats_state, STATS_ENABLED_KEY);
        }

        json_object_object_add(stats_state, STATS_ENABLED_KEY, json_object_new_boolean(TRUE));
    }    
}

int is_enabled(json_object *stats_state) {
    json_object *enabled_bool = NULL;
    json_object_object_get_ex(stats_state, STATS_ENABLED_KEY, &enabled_bool);
    if(enabled_bool != NULL && json_object_is_type(enabled_bool, json_type_boolean)) {
        return json_object_get_boolean(enabled_bool);
    } else {
        return FALSE;
    }
}

//! Idempotently disable stats
void disable_stats(json_object *stats_state) {
    json_object *enabled_bool = NULL;
    json_object_object_get_ex(stats_state, STATS_ENABLED_KEY, &enabled_bool);
    if(enabled_bool == NULL || 
       (!json_object_is_type(enabled_bool, json_type_boolean) || json_object_get_boolean(enabled_bool) == TRUE)) {

        if(enabled_bool != NULL) {
            json_object_object_del(stats_state, STATS_ENABLED_KEY);
        }

        json_object_object_add(stats_state, STATS_ENABLED_KEY, json_object_new_boolean(FALSE));
    }        
}

//! Must ensure that this is called serially the first time. Once the mutex is initialized it is protected by locks
//! @param stats_state - the current state, if any. Will be reset and set enabled. Does not empty memory, just resets counters
int initialize_message_stats(json_object **stats_state) {
    if(stats_state == NULL) {
        LOGFATAL("Cannot initialize a NULL address pointer for stats\n");
        return EUCA_INVALID_ERROR;
    }

    return reset_message_stats(stats_state);
}

//! Add a new data point to the message's stats
int update_message_stats(json_object *stats_state, const char *message_name, int timing_ms, int failed) {
    if(message_name == NULL) {
        return EUCA_ERROR;
    }

    if(stats_state == NULL || !is_enabled(stats_state) ) {
        //Stats are disabled, return ok
        return EUCA_OK;
    }

    json_object *msg = NULL;
    json_object_object_get_ex(stats_state, message_name, &msg);
    json_object *min_obj, *max_obj, *mean_obj, *count_obj, *ok_obj, *fail_obj;
    int min_val, max_val, count_val, ok_count, fail_count;
    double mean_val;

    if(msg == NULL) {
        //Not found. Add it.
        msg = initialize_new_message_map();
        if(msg == NULL) {
            LOGERROR("Failed to add message type %s to the message stats map", message_name);
            return EUCA_ERROR;
        }  else {
            json_object_object_add(stats_state, message_name, msg);
        }
    }

    //Update the data
    json_object_object_get_ex(msg, MSG_MEAN_KEY, &mean_obj);
    if(mean_obj == NULL) {
        LOGERROR("Null in stats object found, cannot continue update.\n");
        return EUCA_ERROR;
    }
    mean_val = json_object_get_int(mean_obj);

    json_object_object_get_ex(msg, MSG_MIN_KEY, &min_obj);
    if(mean_obj == NULL) {
        LOGERROR("Null in stats object found, cannot continue update.\n");
        return EUCA_ERROR;
    }
    min_val = json_object_get_int(min_obj);

    json_object_object_get_ex(msg, MSG_MAX_KEY, &max_obj);
    if(max_obj == NULL) {
        LOGERROR("Null in stats object found, cannot continue update.\n");
        return EUCA_ERROR;
    }
    max_val = json_object_get_int(max_obj);

    json_object_object_get_ex(msg, MSG_COUNT_KEY, &count_obj);
    if(count_obj == NULL) {
        LOGERROR("Null in stats object found, cannot continue update.\n");
        return EUCA_ERROR;
    }
    count_val = json_object_get_int(count_obj);

    json_object_object_get_ex(msg, MSG_OK_COUNT_KEY, &ok_obj);
    if(ok_obj == NULL) {
        LOGERROR("Null in stats object found, cannot continue update.\n");
        return EUCA_ERROR;
    }
    ok_count = json_object_get_int(ok_obj);

    json_object_object_get_ex(msg, MSG_COUNT_KEY, &fail_obj);
    if(fail_obj == NULL) {
        LOGERROR("Null in stats object found, cannot continue update.\n");
        return EUCA_ERROR;
    }
    fail_count = json_object_get_int(fail_obj);

    mean_val = (int)trunc(calculate_moving_mean((double)mean_val, count_val, timing_ms));
    json_object_object_del(msg, MSG_MEAN_KEY);

    json_object_object_add(msg, MSG_MEAN_KEY, json_object_new_int(mean_val));
    
    if(min_val == MSG_MIN_INIT || timing_ms < min_val) {
        json_object_object_del(msg, MSG_MIN_KEY);
        json_object_object_add(msg, MSG_MIN_KEY, json_object_new_int(timing_ms));
    }
    
    if(max_val == MSG_MAX_INIT || timing_ms > max_val) {
        json_object_object_del(msg, MSG_MAX_KEY);
        json_object_object_add(msg, MSG_MAX_KEY, json_object_new_int(timing_ms));
    }
    
    json_object_object_del(msg, MSG_COUNT_KEY);
    json_object_object_add(msg, MSG_COUNT_KEY, json_object_new_int(++count_val));

    if(failed == 0) {
        json_object_object_del(msg, MSG_OK_COUNT_KEY);
        json_object_object_add(msg, MSG_OK_COUNT_KEY, json_object_new_int(++ok_count));
    } else {
        json_object_object_del(msg, MSG_FAIL_COUNT_KEY);
        json_object_object_add(msg, MSG_FAIL_COUNT_KEY, json_object_new_int(++fail_count));
    }

    return EUCA_OK;
}

//! Iterate through and reset all message metrics for next interval
//! Removes all current data and stats from memory
int reset_message_stats(json_object **stats_state) {
    if(stats_state == NULL) {
        LOGERROR("Cannot reset message stats on null pointer\n");
        return EUCA_INVALID_ERROR;
    }

    if(*stats_state != NULL) {
        //In future versions of json-c (0.12 for sure), this returns an int to determine if count decremented.
        //The CentOS version is old (0.10), but when available use the return value to ensure all refs are released
        json_object_object_foreach(*stats_state, key, value) {
            if(strcmp(key, STATS_ENABLED_KEY) != 0 && json_object_is_type(value, json_type_object)) {
                reset_message_stat(value);
            }
        }
    } else {
        LOGTRACE("Reseting message stats to a new json object\n");
        *stats_state = json_object_new_object();
        enable_stats(*stats_state);
    }
    return EUCA_OK;
}

//! Entry point for the message stats sensor. Gets the stats data from
//! the message maps and sends it to the emitter.
json_object *get_message_stats_json(const char *message_stats_string) {
    //Stringify and re-parse to create completely separate copy
    if(message_stats_string != NULL) {
        return json_tokener_parse(message_stats_string);
    } else {
        return NULL;
    }
}


#ifdef _UNIT_TEST
static int test_moving_average() {
    LOGINFO("Testing moving average calculation\n");
    double cma = 0.0;
    int count = 0;
    double result;
    result = calculate_moving_mean(cma, count, 1); 
    if(result != 1.0) {
        LOGERROR("Wrong answer %f\n", result);
        return 1;
    }
    
    cma = 1.0;
    count = 1;    
    result = calculate_moving_mean(cma, count, 2);
    if(result != 1.5) {
        LOGERROR("Wrong answer %f\n", result);
        return 1;
    }

    return 0;
}

static int test_init_message_map() {
    json_object *map = initialize_new_message_map();
    if(map != NULL) {
        LOGINFO("Built: %s\n", json_object_to_json_string_ext(map, JSON_C_TO_STRING_PRETTY));
        return 0;
    }
    return 1;
}

static int test_update_message_stats() {
    LOGINFO("Testing update message stats\n");
    if(initialize_message_stats(&message_stats_map) != 0) {
        LOGERROR("Failed to initialize the structures\n");
        return 1;
    }
    
    if(update_message_stats(message_stats_map, "runInstance", 100, 0) != 0) {
        LOGERROR("Error updating stats\n");
        return 1;
    }

    if(update_message_stats(message_stats_map, "runInstance", 125, 0) != 0) {
        LOGERROR("Error updating stats\n");
        return 1;
    }

    if(update_message_stats(message_stats_map, "runInstance", 75, 0) != 0) {
        LOGERROR("Error updating stats\n");
        return 1;
    }

    if(update_message_stats(message_stats_map, "runInstance", 50, 0) != 0) {
        LOGERROR("Error updating stats\n");
        return 1;
    }

    if(update_message_stats(message_stats_map, "describeInstances", 50, 0) != 0) {
        LOGERROR("Error updating stats\n");
        return 1;
    }

    if(update_message_stats(message_stats_map, "terminateInstance", 50, 0) != 0) {
        LOGERROR("Error updating stats\n");
        return 1;
    }
    
    //Verify
    json_object * inst = NULL;
    json_object_object_get_ex(message_stats_map, "runInstance", &inst);
    LOGINFO("Message stats: \n%s\n", json_object_to_json_string_ext(message_stats_map, JSON_C_TO_STRING_PRETTY));
    json_object *msg_count, *msg_max, *msg_min, *msg_mean;
    msg_count = NULL;
    msg_max = NULL;
    msg_min = NULL;
    msg_mean = NULL;
    json_object_object_get_ex(inst, MSG_COUNT_KEY, &msg_count);
    json_object_object_get_ex(inst, MSG_MAX_KEY, &msg_max);
    json_object_object_get_ex(inst, MSG_MIN_KEY, &msg_min);
    json_object_object_get_ex(inst, MSG_MEAN_KEY, &msg_mean);
    if(inst != NULL && 
       json_object_get_int(msg_count) == 4 &&
       json_object_get_int(msg_max) == 125 &&
       json_object_get_double(msg_mean) == ((100.0+125.0+75.0+50.0)/4.0) && 
       json_object_get_int(msg_min) == 50) {
        LOGINFO("test passes\n");
        return 0;
    } else {
        LOGERROR("failure on verification of results\n");
        return 1;
    }
}

static int test_get_message_stats_json() {
    LOGINFO("Testing update message stats\n");
    if(initialize_message_stats(&message_stats_map) != 0) {
        LOGERROR("Failed to initialize the structures\n");
        return 1;
    }
    
    if(update_message_stats(message_stats_map, "runInstance", 100, 0) != 0) {
        LOGERROR("Error updating stats\n");
        return 1;
    }
    
    LOGINFO("Got intermediate result: %s\n", json_object_to_json_string_ext(message_stats_map, JSON_C_TO_STRING_PRETTY));

    if(update_message_stats(message_stats_map, "runInstance", 125, 0) != 0) {
        LOGERROR("Error updating stats\n");
        return 1;
    }

    LOGINFO("Post-update result: %s\n", json_object_to_json_string_ext(message_stats_map, JSON_C_TO_STRING_PRETTY));

    LOGINFO("Internal state result: %s\n", json_object_to_json_string_ext(message_stats_map, JSON_C_TO_STRING_PRETTY));
    
    return 0;
}

int main(int argc, char** argv) {
    int count, success, failure;
    count = 0;
    success = 0;
    failure = 0;

    if(test_moving_average() == 0) {
        LOGINFO("Success!\n");
        success++;
    } else {
        LOGINFO("Failed\n");
        failure++;
    }
    count++;

    if(test_init_message_map() == 0) {
        LOGINFO("Success!\n");
        success++;
    } else {
        LOGINFO("Failed\n");
        failure++;
    }
    count++;

    if(test_update_message_stats() == 0) {
        LOGINFO("Success!\n");
        success++;
    } else {
        LOGINFO("Failed\n");
        failure++;
    }
    count++;

    if(test_get_message_stats_json() == 0) {
        LOGINFO("Success!\n");
        success++;
    } else {
        LOGINFO("Failed\n");
        failure++;
    }
    count++;


    LOGINFO("Tests: %d, Success: %d, Failure: %d\n", count, success, failure);
    return 0;
}
#endif

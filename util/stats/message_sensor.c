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
#include "message_sensor.h"
#include "message_stats.h"
#include "sensor_common.h"
#include "euca_string.h"
#include <string.h>
#include "log.h"
#include "ipc.h"

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
static json_object *default_tags;
static int sensor_data_ttl;
static char component_name[EUCA_MAX_PATH];
static json_object **(*message_stats_get_fn)(); //Pointer to function to get the stats
static void (*message_stats_set_fn)(); //Pointer to function to set the stats. Called after the sensor runs to update the stats to the reset values

#ifdef _UNIT_TEST
static json_object *test_stats_state;
#endif

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/
//! the message maps and prepares it for the emitter in json format
//! The service_name is expected to be a string
static json_object *msg_stats_sensor_call();
static void toggle_stats(int enabled);

#ifdef _UNIT_TEST
static int test_msg_stats_sensor_call();
static json_object **get_stats_state();
static void set_stats_state();
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


//! Entry point for the message stats sensor. Gets the stats data from
//! the message maps and sends it to the emitter.
//! The arg is a string for the service name to use in output.
static json_object *msg_stats_sensor_call() {
    json_object **msg_data;
    json_object *event_json;
    if(message_stats_get_fn == NULL) {
        LOGERROR("Cannot complete message stats sensor operation, no stats found available\n");
        return NULL;
    }
    
    msg_data = message_stats_get_fn();    
    if(msg_data == NULL || *msg_data == NULL) {
        LOGTRACE("Cannot output results because no result found\n");
        //Make an empty one for clean output, but no values
        return NULL;
    }

    event_json = build_sensor_output(message_sensor.sensor_name, MESSAGE_STATS_SENSOR_DESCRIPTION, time(NULL), sensor_data_ttl, default_tags, *msg_data);
    
    if(event_json == NULL) {
        LOGERROR("Failed in message stats output generation.\n");
        return NULL;
    }
    
    LOGTRACE("Resetting message stats\n");
    reset_message_stats(msg_data);
    
    //Update the stats memory
    message_stats_set_fn();
    
    return event_json;
}

//! Enable/Disable stats collection in coordination with the sensor itself
static void toggle_stats(int enabled)
{
    json_object **stats_data = message_stats_get_fn();
    if(stats_data == NULL || *stats_data == NULL) {
        LOGWARN("Cannot toggle message stats enabled/disabled status, null found\n");
        return;
    }

    if(enabled) {
        LOGTRACE("Setting message stats enabled\n");
        enable_stats(*stats_data);
    } else {
        LOGTRACE("Setting message stats disabled\n");
        disable_stats(*stats_data);
    }

    LOGTRACE("Setting changes in the cache\n");
    message_stats_set_fn();
    return;
}

//! Idempotently initialize the message sensor structures. Not threadsafe.
//! The function pointer is a supplier for the json state of the message stats system at run-time
//! This is for CC & NC memory models. For CC, this can be the cache copy and lock, while for NC it is just a ref return
int initialize_message_sensor(const char *current_component_name, int interval, int ttl, json_object **(*stats_state_get_fn)(), void (*stats_state_set_fn)())
{   
    json_object **stats_state = NULL;
    int ret = 0;
    LOGINFO("Initializing internal message sensor for component %s\n", current_component_name);
    if(current_component_name == NULL ||
       interval < 1 ||
       ttl < 0 ||
       stats_state_get_fn == NULL ||
       stats_state_set_fn == NULL) {
        LOGERROR("Invalid message sensor initialization values. Cannot initialize\n");
        return EUCA_INVALID_ERROR;
    }
    
    message_stats_get_fn = stats_state_get_fn;
    message_stats_set_fn = stats_state_set_fn;    

    stats_state = message_stats_get_fn();
    if(stats_state == NULL) {
        LOGERROR("Cannot initialize internal message stats structures due to null pointer\n");
        return EUCA_INVALID_ERROR;
    }

    ret = initialize_message_stats(stats_state);
    if(ret != EUCA_OK) {
        LOGERROR("Error intializing internal message stats structure: %d\n", ret);
        return ret;
    } else {
        const char *tmp_json = json_object_to_json_string(*stats_state);
        LOGDEBUG("Initialized message stats structure: %s\n", tmp_json);
    }

    message_stats_set_fn();
    
    euca_strncpy(component_name, current_component_name, EUCA_MAX_PATH);
    euca_strncpy(message_sensor.config_name, MESSAGE_STATS_SENSOR_CONFIG_NAME, SENSOR_NAME_MAX);
    snprintf(message_sensor.sensor_name, SENSOR_NAME_MAX, MESSAGE_STATS_SENSOR_NAME_FORMAT, current_component_name);
    message_sensor.enabled = 0;
    message_sensor.sensor_function = msg_stats_sensor_call;
    message_sensor.state_toggle_callback = toggle_stats;
    
    char interval_tag[SENSOR_NAME_MAX];
    snprintf(interval_tag, SENSOR_NAME_MAX, SENSOR_INTERVAL_PERIOD_TAG_FORMAT, interval);
    default_tags = build_tag_set(1, interval_tag);
    sensor_data_ttl = ttl;
    return EUCA_OK;
}

int teardown_message_sensor() {
    //Allow the map to be freed
    if(message_stats_get_fn != NULL) {
        json_object **stats_json = message_stats_get_fn();
        reset_message_stats(stats_json);
    } else {
        LOGDEBUG("No stats get function defined, cannot reset stats during teardown\n");
    }

    if(message_stats_set_fn != NULL) {
        message_stats_set_fn();
    } else {
        LOGDEBUG("No stats set function defined, cannot reset stats during teardown\n");
    }
    return EUCA_OK;
}

#ifdef _UNIT_TEST
static json_object **get_stats_state() 
{
    return &test_stats_state;
}

static void set_stats_state()
{
    //No op
    return;
}

static int test_msg_stats_sensor_call() {
    LOGINFO("\nRunning test %s\n", __func__);
    int test_interval, test_ttl;
    test_interval = 60;
    test_ttl = 30;
    initialize_message_sensor("testservice", test_interval, test_ttl, get_stats_state, set_stats_state);
    update_message_stats(test_stats_state, "runInstance", 55, 0);
    update_message_stats(test_stats_state, "terminateInstance", 15, 0);
    update_message_stats(test_stats_state, "describeInstances", 15, 0);
    
    json_object *output_map = msg_stats_sensor_call();
    if(output_map == NULL) {
        return 1;
    }
    LOGINFO("Result map: %s\n", json_object_to_json_string_ext(output_map, JSON_C_TO_STRING_PRETTY));
    return 0;
}

int main(int argc, char** argv) {
    int count, success, failure;
    count = 0;
    success = 0;
    failure = 0;

    if(test_msg_stats_sensor_call() == 0) {
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

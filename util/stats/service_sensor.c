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
//! @file util/stats/service_sensor.c
//! Service state sensors and functions
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#include "service_sensor.h"
#include "sensor_common.h"
#include <eucalyptus.h>
#include <euca_string.h>
#include <string.h>
#include <log.h>

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
static service_state_sensor_t internal_state_sensor;
static char interval_tag[SENSOR_TAG_MAX];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef _UNIT_TEST
static int test_service_state_sensor();

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
json_object *service_state_sensor_call() {
    json_object *msg_data;
    json_object *event_json;
    json_object *tags;

    char *service_state = internal_state_sensor.state_callback();
    char *check_result = internal_state_sensor.check_callback();
    
    msg_data = json_object_new_object();
    json_object_object_add(msg_data, SERVICE_STATE_KEY, json_object_new_string(service_state));
    json_object_object_add(msg_data, SERVICE_CHECK_KEY, json_object_new_string(check_result));

    tags = build_tag_set(1, interval_tag);
    event_json = build_sensor_output(service_state_sensor.sensor_name, SERVICE_STATE_SENSOR_DESCRIPTION, time(NULL), internal_state_sensor.event_ttl, tags, msg_data);

    if(event_json == NULL) {
        json_object_put(msg_data);
        LOGERROR("Failed in message stats output generation.");
        return NULL;
    }    

    return event_json;
}

//! Idempotently initialize the message sensor structures. Not threadsafe.
int initialize_service_state_sensor(const char *service_name, int interval, int event_ttl, char *(*state_call)(), char *(*check_call)()) {
    if(service_name == NULL ||
       event_ttl < 0 ||
       state_call == NULL ||
       check_call == NULL) {
        LOGERROR("Invalid initialization values for service sensor. Cannot initialize\n");
        return EUCA_ERROR;
    }

    LOGINFO("Initializing service state sensor for component %s\n", service_name);
    euca_strncpy(service_state_sensor.config_name,SERVICE_STATE_SENSOR_NAME, SENSOR_NAME_MAX);
    snprintf(service_state_sensor.sensor_name, SENSOR_NAME_MAX, SERVICE_STATE_SENSOR_NAME_FORMAT, service_name);
    service_state_sensor.enabled = 0;
    service_state_sensor.sensor_function = service_state_sensor_call;
    service_state_sensor.state_toggle_callback = NULL;

    euca_strncpy(internal_state_sensor.service_name, service_name, SENSOR_NAME_MAX);
    internal_state_sensor.check_callback = check_call;
    internal_state_sensor.state_callback = state_call;
    internal_state_sensor.event_ttl = event_ttl;
    snprintf(interval_tag, SENSOR_TAG_MAX, SENSOR_INTERVAL_PERIOD_TAG_FORMAT, interval);

    return EUCA_OK;
}

int teardown_service_state_sensor() {
    bzero(internal_state_sensor.service_name, SENSOR_NAME_MAX);
    internal_state_sensor.state_callback = NULL;
    internal_state_sensor.check_callback = NULL;
    return EUCA_OK;
}

#ifdef _UNIT_TEST

char *state_test() {
    return "ENABLED";
}

char *check_test() {
    return "OK";
}

int test_service_state_sensor() {
    int test_ttl = 60;
    initialize_service_state_sensor("nc", test_ttl, test_ttl, state_test, check_test);
    json_object *event = service_state_sensor_call("nc");
    LOGINFO("Result map: %s\n", json_object_to_json_string_ext(event, JSON_C_TO_STRING_PRETTY));
    return 0;
}

int main(int argc, char** argv) {
    int count, success, failure;
    count = 0;
    success = 0;
    failure = 0;

    if(test_service_state_sensor() == 0) {
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

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
#include "sensor_common.h"
#include <eucalyptus.h>
#include <log.h>
#include <json-c/json.h>
#include <stdarg.h>

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef _UNIT_TEST
static int test_build_sensor_output();
static int test_build_tag_set();
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

//! Builds a basic sensor output json object. Returns the newly allocated json
json_object *build_sensor_output(const char *sensor_name, const char *description, time_t timestamp, int ttl, json_object *tags, json_object *values) {
    json_object *event = json_object_new_object();
    if(sensor_name != NULL) {
        json_object_object_add(event, SENSOR_NAME_KEY, json_object_new_string(sensor_name));
    }
    if(description != NULL) {
        json_object_object_add(event, SENSOR_DESCRIPTION_KEY, json_object_new_string(description));
    }
    json_object_object_add(event, SENSOR_TIMESTAMP_KEY, json_object_new_int64((long)timestamp));
    json_object_object_add(event, SENSOR_TTL_KEY, json_object_new_int(ttl));
    json_object_object_add(event, SENSOR_TAGS_KEY, tags);
    //copy the values
    const char *tmp_data = json_object_to_json_string(values);
    LOGTRACE("Building sensor output from %s\n", tmp_data);
    json_object *sensor_data = json_tokener_parse(tmp_data);
    json_object_object_add(event, SENSOR_DATA_KEY, sensor_data);
    return event;
}

json_object *build_tag_set(int argc, ...) {
    json_object *default_tags = json_object_new_array();
    char *c;
    int i;
    va_list arg_list;
    va_start(arg_list, argc);
    for(i = 0; i < argc; i++) {
        c = va_arg(arg_list, char *);
        json_object_array_add(default_tags, json_object_new_string(c));
    }
    va_end(arg_list);
    return default_tags;   
}

#ifdef _UNIT_TEST
static int test_build_tag_set() {
    json_object *t = build_tag_set(1, "tag1");
    if(t == NULL) return 1;
    LOGINFO("tag set %s\n", json_object_to_json_string_ext(t, JSON_C_TO_STRING_PRETTY));

    t = build_tag_set(1, "tag1","tag2","tag3");
    if(t == NULL) return 1;
    LOGINFO("tag set2 %s\n", json_object_to_json_string_ext(t, JSON_C_TO_STRING_PRETTY));

    return 0;
}

static int test_build_sensor_output() {
    json_object *tags_json, *values_json;
    int ttl = 10;    
    tags_json = build_tag_set(1, "tag1","tag2","tag3");

    values_json = json_object_new_object();
    json_object_object_add(values_json, "mean", json_object_new_int(100));
    json_object_object_add(values_json, "min", json_object_new_int(0));
    json_object_object_add(values_json, "max", json_object_new_int(200));

    json_object *test = build_sensor_output("testsensor.mysensor.test", "test description sensor", time(NULL), ttl, tags_json, values_json);
    if(test == NULL) {
        LOGERROR("failed, got null sensor output");
        return 1;
    }

    LOGINFO("Test output: %s\n", json_object_to_json_string_ext(test, JSON_C_TO_STRING_PRETTY));
    return 0;
}

int main(int argc, char** argv) {
    int count, success, failure;
    count = 0;
    success = 0;
    failure = 0;

    if(test_build_sensor_output() == 0) {
        LOGINFO("Success!\n");
        success++;
    } else {
        LOGINFO("Failed\n");
        failure++;
    }    
    count++;

    if(test_build_tag_set() == 0) {
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

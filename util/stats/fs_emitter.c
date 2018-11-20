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
//! @file util/stats/fs_emitter.c
//! Implementation of event emitter the writes json to the filesystem with
//!  each sensor event in a unique file identified by the sensor name
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include "fs_emitter.h"
#include "sensor_common.h"
#include <eucalyptus.h>
#include <euca_file.h>
#include <euca_string.h>
#include <diskutil.h>
#include <log.h>
#include <stdio.h>
#include <json-c/json.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <grp.h>
#include <string.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#ifdef _UNIT_TEST
#define MAX_JSON_LENGTH_TEST 256
#endif
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
static struct group *euca_stats_group;
const static int file_flags = O_CREAT | O_WRONLY;
static char euca_stats_path[EUCA_MAX_PATH];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/
static const char *get_sensor_name(json_object *event);
static char *get_temp_output_name(const char *sensor_name);
static char *get_output_name(const char *sensor_name);
static int write_event_to_file(json_object *event);
static char *expand_data_path(const char *path);
static int set_stats_output_path(const char *euca_home);
static char *get_stats_output_path();
static void euca_chrreplace(char *haystack, char target, char replacement);

#ifdef _UNIT_TEST
static char *test_home; //home dir for tests
static int test_get_sensor_name();
static int test_get_temp_output_name();
static int test_get_output_name();
static int test_write_event_to_file();
static int test_get_set_stats_path();
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

int init_emitter(const char *euca_home) {
    //check for proper group
    LOGDEBUG("Initializing fs emitter\n");
    LOGINFO("Verifying %s group is present\n", DATA_OUTPUT_GROUP);
    euca_stats_group = getgrnam(DATA_OUTPUT_GROUP);

    if(euca_stats_group == NULL) {
        LOGERROR("Cannot init fs emitter. User group not found %s\n", DATA_OUTPUT_GROUP);
        return EUCA_ERROR;
    }
    // only diskutil_ch is used by emitter, the diskutil_ch requires first three dist utils helpers
    if(diskutil_init(3) != EUCA_OK) {
        LOGERROR("Diskutil init failed. Cannot initialize fs emitter\n");
        return EUCA_ERROR;
    }

    //Initialize the output path
    if(set_stats_output_path(euca_home) != EUCA_OK) {
        LOGERROR("Error initializing output directory for fs emitter with home %s\n", euca_home);
        return EUCA_ERROR;
    } else {
        char *path = get_stats_output_path();
        if(path == NULL || ensure_directories_exist(path, 0, DATA_OUTPUT_USER, DATA_OUTPUT_GROUP, DATA_DIR_PERM) < 0) {
            LOGERROR("Cannot find output directory for fs emitter as expected: %s\n", path);
            return EUCA_ERROR;
        } else {
            LOGINFO("Verified fs emitter stats directory: %s\n", path);
        }
    }

    LOGINFO("FS emitter initialization complete\n");
    return EUCA_OK;
}

//!
//! Synchronously offer an event to the emitter. Emits the event at this time, no batching/queues
//! 
//! @param json document to emit
//! @returns 0 on success, error code != 0 on failure
int emitter_offer_event(json_object *event) {
    if(event == NULL) {
        return EUCA_ERROR;
    }
    return write_event_to_file(event);
}

//! Replace the 'replace' char with the 'find' char in the string. Simple
static void euca_chrreplace(char *haystack, char target, char replacement) {
    if(haystack == NULL) {
        return;
    }
    int i = 0;
    for(i = 0 ; i < strlen(haystack); i++) {
        if(haystack[i] == target) {
            haystack[i] = replacement;
        }
    }    
}


//! Returns the sensor name from the given json object or NULL if none found
//! Looks for a string element at the top level that has key = <SENSOR_NAME_KEY>
static const char *get_sensor_name(json_object *event) {
    if(event == NULL) {
        return NULL;
    }
    json_object *name_obj;
    if(json_object_object_get_ex(event, SENSOR_NAME_KEY, &name_obj) != FALSE && name_obj != NULL) {
        return json_object_get_string(name_obj);
    } else {
        return NULL;
    }
}

//! return a new string with name delims replaced with path delims
//! and a suffix to ensure uniquenes
static char *get_temp_output_name(const char *sensor_name) {
    if(sensor_name == NULL) {
        return NULL;
    }
    char *copy = get_output_name(sensor_name);
    if(copy == NULL) {
        return NULL;
    }
    return euca_strdupcat(copy, DATA_FILENAME_TEMP_SUFFIX);
}

//! Get the final output file name from a given sensor name
//! Replaces the '.' with '/' to convert to a path
static char *get_output_name(const char *sensor_name) {
    if(sensor_name == NULL) {
        return NULL;
    }
    char *copy = euca_strdup(sensor_name);
    if(copy == NULL) {
        LOGERROR("Error converting sensor name %s to a path\n", sensor_name);
        return NULL;
    } else {
        euca_chrreplace(copy, SENSOR_NAME_SEPARATOR, SENSOR_NAME_PATH_SEPARATOR);
        char *result = expand_data_path(copy);
        EUCA_FREE(copy);
        return result;
    }
}

//! Returns the string that is the current output path
static char *get_stats_output_path() {
    return euca_stats_path;
}

//! Sets a string that is the constructed path for the stats output directory based
//!  on environment variables
static int set_stats_output_path(const char *euca_home) {
    if(euca_home == NULL) {
        LOGERROR("Cannot initialize stats output path: No EUCALYPTUS value found in the environment.\n");
        return EUCA_ERROR;
    }

    if(snprintf(euca_stats_path, EUCA_MAX_PATH, EUCALYPTUS_STATS_OUTPUT_DIR, euca_home) < 0) {
        LOGERROR("Failed stats output path initialization\n");
        return EUCA_ERROR;
    }

    dedup_path(euca_stats_path);

    return EUCA_OK;
}

//! Get the output path with proper root location
static char *expand_data_path(const char *path) {
    char *full_path = NULL;
    const char *euca_root_path;

    if(path == NULL) {
        return NULL;
    }
    
    if (euca_sanitize_path(path) != EUCA_OK) {
        LOGERROR("Invalid characters in the requested path %s\n", path);
        return NULL;
    }

    euca_root_path = get_stats_output_path();
    if(euca_root_path == NULL) {
        LOGERROR("Could not get output path for stats data.\n");
        return NULL;
    }    
    
    full_path = EUCA_ALLOC(EUCA_MAX_PATH, sizeof(char));
    if(full_path != NULL && snprintf(full_path, EUCA_MAX_PATH, "%s/%s", euca_root_path, path) < 0) {
        full_path = NULL;
    }
    return full_path;
}

//! Returns 0 on success, error code != 0 otherwise
static int write_event_to_file(json_object *event) {
    LOGTRACE("Writing event to output file\n");
    char *file_name = NULL;
    char *tmp_file_name = NULL;
    int result = EUCA_OK;
    int rc = 0;

    if(event == NULL) {
        LOGDEBUG("Cannot emit a null event\n");
        return EUCA_INVALID_ERROR;
    }

    //Get the name
    const char *sensor_name = get_sensor_name(event);
    if(sensor_name == NULL) {
        LOGERROR("Could not get sensorname from sensor event\n");
        return EUCA_ERROR;
    }
    
    //Get the whole output string
    const char *json_string = json_object_to_json_string_ext(event, JSON_C_TO_STRING_PRETTY);
    if(json_string == NULL) {
        LOGERROR("Error getting json string for sensor event\n");
        return EUCA_ERROR;
    }
    
    tmp_file_name = get_temp_output_name(sensor_name);
    file_name = get_output_name(sensor_name);
    if(tmp_file_name == NULL || file_name == NULL) {
        LOGERROR("Could not get filename from sensor event %s\n", json_string);
        if(tmp_file_name != NULL ) EUCA_FREE(tmp_file_name);
        if(file_name != NULL ) EUCA_FREE(file_name);            
        return result;
    } else {
        if(ensure_directories_exist(tmp_file_name, 1, DATA_OUTPUT_USER, DATA_OUTPUT_GROUP, DATA_DIR_PERM) < 0) {
            LOGERROR("Could not create full directory path for file %s\n", tmp_file_name);
            result = EUCA_ERROR;
        } else {
            //Create the file with the proper restricted permissions.
            if(str2file(json_string, tmp_file_name, file_flags, OUTPUT_DATA_PERM, FALSE) != EUCA_OK) {
                LOGERROR("Error writing event data: %s to output file %s\n", json_string, tmp_file_name);
                result = EUCA_IO_ERROR;
            } else {
                //Set ownership to allow the proper group access
                rc = diskutil_ch(tmp_file_name, DATA_OUTPUT_USER, DATA_OUTPUT_GROUP, OUTPUT_DATA_PERM);
                if(rc != EUCA_OK) {
                    LOGERROR("Error setting ownership info on sensor data file %s\n", tmp_file_name);
                    result = rc; 
                } else {                                       
                    if(rename(tmp_file_name, file_name) == -1) {
                        LOGERROR("Could not rename new data file %s to %s. Cleaning up\n", tmp_file_name, file_name);
                        result = EUCA_IO_ERROR;
                    }
                }
            }            
            
            //Cleanup any partial file
            if(result != EUCA_OK && remove(tmp_file_name) != 0) {
                //Delete failed, probably no file to delete. continue
                LOGWARN("Deletion of temp file %s after failed sensor output failed. File probably did not exist\n", tmp_file_name);
            }
        }

        EUCA_FREE(tmp_file_name);
        EUCA_FREE(file_name);
        
    }
    return result;
}
    
#ifdef _UNIT_TEST
static int test_get_set_stats_path() {
    LOGINFO("\n------------Testing get/set stats path------------------\n");
    
    if(set_stats_output_path(test_home) != EUCA_OK) {
        LOGERROR("Unexpected failure for init of stats path. Check the environment\n");
        return EUCA_ERROR;
    }

    char *path = get_stats_output_path();
    if(path == NULL || strlen(path) == 0) {
        LOGERROR("Stats path is null or zero-length. Check the environment var EUCALYPTUS\n");
        return EUCA_ERROR;
    } else {
        LOGINFO("Found path: %s\n", path);
    }

    return EUCA_OK;
}
static int test_get_sensor_name() {
    LOGINFO("\n------------Testing get_sensor_name()------------------\n");
    json_object *obj = json_tokener_parse("{\"sensor\":\"mysensor.mytest\"}");
    if(obj == NULL) {
        LOGERROR("Got null json\n");
        return EUCA_ERROR;
    }
    const char *name = get_sensor_name(obj);
    if(name == NULL) {
        LOGERROR("Got null sensor name\n");
        return EUCA_ERROR;
    }

    if(strcmp(name, "mysensor.mytest") != 0) {
        LOGERROR("Got unexpected sensor name: %s\n", name);
        return EUCA_ERROR;
    }

    LOGINFO("Got name: %s\n", name);

    //type 2
    json_object *obj2 = json_tokener_parse("{\"sensor\":\"mysensormytest\"}");
    if(obj2 == NULL) {
        LOGERROR("Got null json\n");
        return EUCA_ERROR;
    }
    const char *name2 = get_sensor_name(obj2);
    if(name2 == NULL) {
        LOGERROR("Got null sensor name\n");
        return EUCA_ERROR;
    }

    if(strcmp(name2, "mysensormytest") != 0) {
        LOGERROR("Got unexpected sensor name: %s\n", name);
        return EUCA_ERROR;
    }

    LOGINFO("Got name: %s\n", name);
    return EUCA_OK;
}

static int test_get_temp_output_name() {
    LOGINFO("\n-------------Testing get_temp_output_name()-----------\n");
    const char * test = "my.sensor.name";
    const char * test2 = "my-sensor.name";
    const char * test3 = "mysensorname";
    char * result = get_temp_output_name(test);
    if(result == NULL) return EUCA_ERROR;
    LOGINFO("Tmp1 Output name got: %s\n", result);

    char *expected = expand_data_path("my/sensor/name.new");
    if(strcmp(result, expected) != 0) return EUCA_ERROR;

    char * result2 = get_temp_output_name(test2);
    if(result2 == NULL) return EUCA_ERROR;
    LOGINFO("Tmp2 Output name got: %s\n", result2);

    char *expected2 = expand_data_path("my-sensor/name.new");
    if(strcmp(result2, expected2) != 0) return EUCA_ERROR;


    char * result3 = get_temp_output_name(test3);
    if(result3 == NULL) return EUCA_ERROR;
    LOGINFO("Tmp3 Output name got: %s\n", result3);

    char *expected3 = expand_data_path("mysensorname.new");
    if(strcmp(result3, expected3) != 0) return EUCA_ERROR;
    return EUCA_OK;
}

static int test_get_output_name() {
    LOGINFO("\n-------------Testing get_output_name()-----------------\n");
    const char * test = "mysensorname";
    char *expected = expand_data_path("mysensorname");
    LOGINFO("Expecting: %s\n", expected);
    char * result = get_output_name(test);
    if(result == NULL) return EUCA_ERROR;
    LOGINFO("Output name got: %s\n", result);
    if(strcmp(result, expected) != 0) {
        LOGERROR("Sensor name doesn't match expected\n");
        return 1;
    }

    const char * test2 = "my.sensor.name";
    char *expected2 = expand_data_path("my/sensor/name");
    LOGINFO("Expecting: %s\n", expected2);
    char * result2 = get_output_name(test2);
    if(result2 == NULL) return EUCA_ERROR;
    LOGINFO("Output name got: %s\n", result2);
    if(strcmp(result2, expected2) != 0) {
        LOGINFO("Sensor name doesn't match expected\n");
        return 1;
    }

    return EUCA_OK;
}
 
static int test_write_event_to_file() {
    LOGINFO("\n-------------Testing write_event_to_file----------------\n");
    const char * test_json = "{\"sensor\":\"mysensor.name\",\"test\":\"value\", \"timestamp\": 123456 }";
    json_object *test_event = json_tokener_parse(test_json);
    if(test_event == NULL) {
        LOGERROR("Got null json\n");
        return EUCA_ERROR;
    }
    int result = write_event_to_file(test_event);
    if(result == EUCA_OK) {
        LOGINFO("Success!\n");
        return EUCA_OK;
    } else {
        LOGINFO("Failed writing to file! Got error: %d\n", result);
        return result;
    }
}

static int test_write_event_to_file_highload() {    
    LOGINFO("\n-------------Testing write_event_to_file with high load----------------\n");
    char test_json0[MAX_JSON_LENGTH_TEST];
    char test_json1[MAX_JSON_LENGTH_TEST];
    char test_json2[MAX_JSON_LENGTH_TEST];
    const char * test_template = "{\"sensor\":\"%s\",\"test\":\"value\", \"timestamp\": %d }";
    snprintf(test_json0, MAX_JSON_LENGTH_TEST, test_template, "euca.components.nc.msgs.stats", (int)time(NULL));
    snprintf(test_json1, MAX_JSON_LENGTH_TEST, test_template, "euca.components.cc.msgs.count", (int)time(NULL));
    snprintf(test_json2, MAX_JSON_LENGTH_TEST, test_template, "euca.components.nc.resources.types", (int)time(NULL));
    json_object *test_event0 = json_tokener_parse(test_json0);
    json_object *test_event1 = json_tokener_parse(test_json1);
    json_object *test_event2 = json_tokener_parse(test_json2);
    if(test_event0 == NULL || test_event1 == NULL || test_event2 == NULL) {
        LOGERROR("Got null json\n");
        return EUCA_ERROR;
    }
    LOGINFO("json0: %s\n",json_object_to_json_string_ext(test_event0, JSON_C_TO_STRING_PRETTY));
    LOGINFO("json1: %s\n",json_object_to_json_string_ext(test_event1,  JSON_C_TO_STRING_PRETTY));
    LOGINFO("json2: %s\n",json_object_to_json_string_ext(test_event2, JSON_C_TO_STRING_PRETTY));
    int i = 0;
    int j = 0;
    int set_count = 100;
    int count_per_event = 5;
    int result = 0;
    int successes = 0;
    int failures = 0;
    for(i = 0; i < set_count ; i++) {
        for(j = 0 ; j < count_per_event ; j++) {
            result = write_event_to_file(test_event0);
            result == EUCA_OK ? successes++ : failures++;

            result = write_event_to_file(test_event1);
            result == EUCA_OK ? successes++ : failures++;

            result = write_event_to_file(test_event2);
            result == EUCA_OK ? successes++ : failures++;
        }
    }
    if(failures > 0 ) {
        return failures;
    } else {
        return EUCA_OK;
    }
}

const char * test_output_path = "unit_test_output";

int main(int argc, char** argv) {
    //get log params
    int current_level, roll_out;
    long bytes_out;
    log_params_get(&current_level, &roll_out, &bytes_out);
    log_params_set(log_level_int("TRACE"), roll_out, bytes_out);

    LOGINFO("Environment macros: \nEUCALYPTUS_RUN_DIR = %s\nLOCALSTATEDIR=%s\nDATA_DIR=%s\nSTAT_DIR=%s\n", EUCALYPTUS_RUN_DIR, LOCALSTATEDIR, DATADIR, EUCALYPTUS_STATS_OUTPUT_DIR);
    LOGINFO("Running unit tests with Environment var: EUCALYPTUS=%s\n", getenv(EUCALYPTUS_ENV_VAR_NAME));
    int success_count = 0;
    int failure_count = 0;
    int test_count = 0;
    test_home = getenv(EUCALYPTUS_ENV_VAR_NAME);
    init_emitter(test_home);
    ++test_count && (test_get_sensor_name() == EUCA_OK) ? success_count++ : failure_count++;
    LOGINFO("Unit tests completed: %d total, %d success, %d failures\n", test_count, success_count, failure_count);

    ++test_count && (test_get_set_stats_path() == EUCA_OK) ? success_count++ : failure_count++;
    LOGINFO("Unit tests completed: %d total, %d success, %d failures\n", test_count, success_count, failure_count);


    ++test_count && (test_get_output_name() == EUCA_OK) ? success_count++ : failure_count++;
    LOGINFO("Unit tests completed: %d total, %d success, %d failures\n", test_count, success_count, failure_count);

    ++test_count && (test_get_temp_output_name() == EUCA_OK) ? success_count++ : failure_count++;
    LOGINFO("Unit tests completed: %d total, %d success, %d failures\n", test_count, success_count, failure_count);
    
    ++test_count && (test_write_event_to_file() == EUCA_OK) ? success_count++ : failure_count++;    
    LOGINFO("Unit tests completed: %d total, %d success, %d failures\n", test_count, success_count, failure_count);

    //Test performance and lots of data
    //++test_count && (test_write_event_to_file_highload() == EUCA_OK) ? success_count++ : failure_count++;    
    LOGINFO("Unit tests completed: %d total, %d success, %d failures\n", test_count, success_count, failure_count);

    return 0;
}

#endif


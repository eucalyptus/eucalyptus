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
//! @file util/stats/stats.c
//! Implementation for basic statistics/state tracking of c-based services
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#include <string.h>
#include <stdio.h>

#include "eucalyptus.h"
#include "euca_file.h"
#include "euca_string.h"
#include "log.h"
#include "misc.h"
#include "config.h"
#include "stats.h"
#include "message_sensor.h"
#include "message_stats.h"
#include "service_sensor.h"
#include "fs_emitter.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#define _GNU_SOURCE

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
extern struct internal_sensor message_sensor; //from message_sensor.h
extern struct internal_sensor service_state_sensor; //from service_sensor.h

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/
#ifdef _UNIT_TEST
json_object *test_msg_stats; //Stats obj for testing
configEntry configEntryKeysRestart[] = { { "placeholderkey", "placeholderdefault" } }; //Not used but must have something here, cannot be zero
configEntry configEntryKeysNoRestart[] = { {SENSOR_LIST_CONF_PARAM_NAME, SENSOR_LIST_CONF_PARAM_DEFAULT} };
#endif

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/
static int is_initialized = FALSE;
static char component_name[EUCA_MAX_PATH];
static void (*get_lock_fn)();
static void (*release_lock_fn)();

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void toggle_disabled_all();
static int register_sensor_set();
static int register_sensor(struct internal_sensor *sensor_to_register);
static int is_registered_sensor_name(const char *name);
static int get_new_config_status(const char *sensor_name, const char **enabled_sensors);

#ifdef _UNIT_TEST
static int test_sensor_registration();
static int test_stats_run(const char *config_file);
static char *testing_service_state_call();
static char *testing_service_state_call();
static json_object **test_get_msg_stats();
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

//! Returns only if requested by run_once = TRUE
//! Pointer to get updated config
int run_stats(int run_once, int execution_interval_sec, int (*update_config_fn)(void))
{
    LOGDEBUG("Starting stats loop with execution interval %d sec\n", execution_interval_sec);
    useconds_t start_time_us = 0;
    useconds_t execution_interval_us = execution_interval_sec * 1000 * 1000;
    useconds_t sleep_time_us = execution_interval_us;
    do {
        LOGTRACE("Sleeping for %d usec for next pass\n", sleep_time_us);
        usleep(sleep_time_us);
        start_time_us = time_usec();

        //update config
        if(update_config_fn) {
            LOGTRACE("Getting updated config\n");
            update_config_fn();
            LOGTRACE("Done getting updated config\n");
        }

        //Run internal stats sensor updates
        LOGTRACE("Executing internal stats sensor pass...not quite\n");
        //Don't return immediately on failure, run all internal sensors
        if(internal_sensor_pass(FALSE) != EUCA_OK) {
            LOGERROR("Error encountered during internal stats sensor run.\n");
        } else {
            LOGTRACE("Internal stats sensor pass completed successfully\n");
        }
        
        sleep_time_us = execution_interval_us - (time_usec() - start_time_us);
    } while(!run_once);

    LOGDEBUG("Returning from run_stats()\n");
    return EUCA_OK;
}

//! A single sensor pass. Runs each sensor and emits the result using
//! the configured emitter.
//! 
int internal_sensor_pass(int return_on_fail) {
    LOGTRACE("Executing internal sensor pass\n");
    json_object *result;
    int i = 0;
    int is_enabled = FALSE;
    int ret = EUCA_OK;
    //Get the sensor list from the config
    char **names = NULL;
    char *tmpstr = configFileValue(SENSOR_LIST_CONF_PARAM_NAME);
    if (tmpstr == NULL) {
        LOGDEBUG("%s parameter is missing from config file\n", SENSOR_LIST_CONF_PARAM_NAME);
    } else {
        names = from_var_to_char_list(tmpstr);
        EUCA_FREE(tmpstr);
    }

    char **sensors = { NULL }; // if no config value or an empty list
    if (names != NULL) {
        sensors = names;
    }

    //Enter the execution loop
    if(get_lock_fn != NULL) {
        get_lock_fn();
    } else {
        LOGERROR("Cannot run internal sensor pass because no lock provided to protect data.\n");

        //Free up the sensor listing
        if (names) {
            for (int i=0; names[i]; i++) {
                EUCA_FREE(names[i]);
            }
            EUCA_FREE(names);
        }

        return EUCA_ERROR;
    }

    LOGTRACE("Running %d sensors\n", sensor_registry.sensor_count);
    for(i = 0; i < sensor_registry.sensor_count; i++) {
        LOGTRACE("Checking sensor %s is enabled\n", sensor_registry.sensors[i]->sensor_name);
        is_enabled = get_new_config_status(sensor_registry.sensors[i]->config_name, (const char**)sensors);
        if(is_enabled && sensor_registry.sensors[i]->sensor_function != NULL) {
            result = sensor_registry.sensors[i]->sensor_function(); 
            if(result == NULL) {
                LOGERROR("Error encountered getting internal sensor output from sensor %s\n", sensor_registry.sensors[i]->sensor_name);
                ret = EUCA_ERROR;
            } else {
                LOGTRACE("Offering event for emitter: %s\n", json_object_to_json_string(result));
                if(emitter_offer_event(result) != EUCA_OK) {
                    LOGERROR("Error emitting event from internal sensor %s\n", sensor_registry.sensors[i]->sensor_name);
                    ret = EUCA_ERROR;
                } else {
                    LOGTRACE("Event offered to emitter successfully\n");
                }
            }
        } else {
            LOGTRACE("Sensor %s not enabled, skipping execution\n", sensor_registry.sensors[i]->sensor_name);
        }
        
        //Update the state of the sensor as needed.
        if(sensor_registry.sensors[i]->state_toggle_callback != NULL) {
            //Must be idempotent
            LOGTRACE("Calling toggle callback on sensor %s with enabled=%d\n", sensor_registry.sensors[i]->sensor_name, is_enabled);
            sensor_registry.sensors[i]->state_toggle_callback(is_enabled);
        }

        if(return_on_fail && ret != EUCA_OK) {
            goto cleanup;
        }
    }
    
 cleanup:
    if(release_lock_fn != NULL) {
        release_lock_fn();
        LOGTRACE("Released lock for stats during sensor pass\n");
    } else {
        LOGERROR("No lock release function found, this could result in deadlock or leaks. Continuing to exit sensor pass function\n");       
    }

    //Free up the sensor listing
    if (names) {
        for (int i=0; names[i]; i++) {
            EUCA_FREE(names[i]);
        }
        EUCA_FREE(names);
    }
    return ret;
}

//! Disables all registered sensors
static void toggle_disabled_all()
{
    for (int i=0; i < sensor_registry.sensor_count; i++) {
        if (sensor_registry.sensors[i]->config_name != NULL) {
            sensor_registry.sensors[i]->enabled = 0;
        }
    }
}

//! Register the common internal sensors
static int register_sensor_set() {
    int result = 0;

    LOGDEBUG("Registering message stats sensor\n");
    if(result += register_sensor(&message_sensor) != 0) {
        LOGERROR("Error registering message stats sensor\n");
    }
    
    LOGDEBUG("Registering service state sensor\n");
    if(result += register_sensor(&service_state_sensor) > 0) {
        LOGERROR("Error registering service state sensor\n");
    }

    return result;
}

//! Add the specified sensor to the registry
static int register_sensor(struct internal_sensor *sensor_to_register) {
    int result_code = EUCA_ERROR;
    if(sensor_to_register == NULL) {
        LOGERROR("Cannot register null as a sensor\n");
        return EUCA_ERROR;
    }

    if(sensor_to_register->config_name == NULL || strlen(sensor_to_register->config_name) == 0) {
        LOGERROR("Sensor must have a config name to be registerable. None found\n");
        return EUCA_ERROR;
    }

    if(sensor_to_register->sensor_name == NULL || strlen(sensor_to_register->sensor_name) == 0) {
        LOGERROR("Sensor must have a sensor name to be registerable. None found\n");
        return EUCA_ERROR;
    }

    if(sensor_registry.sensor_count < MAX_SENSOR_COUNT) {
        sensor_registry.sensors[sensor_registry.sensor_count++] = sensor_to_register;
        result_code = EUCA_OK;
    } else {
        result_code = EUCA_ERROR;
    }
    return result_code;
}

//! Checks the sensor registry for presence of a sensor with the given name
//! Returns 0 if not found, 1 if found
//! Always returns 0 if name == NULL
static int is_registered_sensor_name(const char *name) {
    int i = 0;
    if(name == NULL) {
        return FALSE;
    }

    for(i = 0; i < sensor_registry.sensor_count; i++) {
        LOGTRACE("Checking sensor config name %s against config name: %s and sensor name: %s\n", name, sensor_registry.sensors[i]->config_name, sensor_registry.sensors[i]->sensor_name);
        if(strncmp(sensor_registry.sensors[i]->config_name, name, SENSOR_NAME_MAX) == 0) {
            return TRUE;
        }
    }

    return FALSE;
}

//! Not thread-safe. Initalizes semaphores etc. Must be guarded externally
int init_stats(const char *euca_home, const char *current_component_name, void (*lock_fn)(), void (*unlock_fn)()) {
    int result = EUCA_OK;

    if(euca_home == NULL ||
       current_component_name == NULL ||
       lock_fn == NULL ||
       unlock_fn == NULL) {
        LOGERROR("Cannot initialize stats subsystem due to invalid config parameters\n");
        return EUCA_INVALID_ERROR;
    }

    if(is_initialized) {
        LOGDEBUG("Stats already initialized.\n");
        return EUCA_OK;
    }

    LOGINFO("Initializing internal stats subsystem for component %s with euca home %s\n", current_component_name, euca_home);

    if(euca_strncpy(component_name, current_component_name, EUCA_MAX_PATH) <= 0) {
        LOGERROR("Failed setting stats component name to %s\n", current_component_name);
        result = EUCA_FATAL_ERROR;
        goto cleanup;
    }

    get_lock_fn = lock_fn;
    release_lock_fn = unlock_fn;

    LOGDEBUG("Registering sensors\n");
    //Register the sensors (map the function pointers, etc).
    result = register_sensor_set(component_name);
    if(result != EUCA_OK) {
        LOGERROR("Error registering internal sensor set: %d\n", result);
        goto cleanup;
    }

    LOGDEBUG("Initializing event emitter\n");
    //Initialize the event emitter
    result = init_emitter(euca_home);
    if(result != EUCA_OK) {
        LOGERROR("Error initializing emitter: %d\n", result);
        goto cleanup;
    }
    
 cleanup:
    if(result == EUCA_OK) { 
        is_initialized = TRUE;
        LOGINFO("Internal stats initialization complete\n");
    } else {
        LOGERROR("Initialization of stats system failed due to error: %d\n", result);
    }

    return result;
}

//! Return if the given sensor is in the list of enabled_sensors. Not terribly efficient
//! But the list is usually really short (eg. <10)
static int get_new_config_status(const char *sensor_name, const char **enabled_sensors) 
{
    if(enabled_sensors == NULL) {
        LOGTRACE("Cannot determine config status of sensor %s due to null list of sensors to enable\n", sensor_name);
        return FALSE;
    }

    for (int i=0; enabled_sensors[i] != NULL; i++) {
        const char *enabled_sensor_name = enabled_sensors[i];
        LOGTRACE("Checking config status of sensor %s from %s\n", sensor_name, enabled_sensor_name);
        if(strncmp(sensor_name, enabled_sensor_name, SENSOR_NAME_MAX) == 0) {
            return TRUE;
        }
    }
    return FALSE;
}

//! Enables and disables sensors based on a NULL-terminated array of names.
//! All named sensors are enabled, all others are disabled. This can be
//! invoked any time the set of enabled sensors changes, such as when 
//! eucalyptus.conf parameter for sensors changes.
//!
//! @param enabled_sensors a NULL-terminated array of sensor names (allocation and freeing is managed by the caller)

int conf_stats(const char **enabled_sensors)
{
    int result = EUCA_OK;

    if (enabled_sensors == NULL) {
        LOGDEBUG("conf_stats() called when sensors are not enabled\n");
        return EUCA_ERROR;
    }
    
    if(get_lock_fn != NULL) {
        get_lock_fn();
    }

    {
        // enable ones listed
        int sensor_should_enable = FALSE;
        int toggle_count = 0;
        for(int i = 0; i < sensor_registry.sensor_count; i++) {
            sensor_should_enable = get_new_config_status(sensor_registry.sensors[i]->config_name, enabled_sensors);
            LOGTRACE("Should enable sensor %s = %d .Current enable = %d\n", sensor_registry.sensors[i]->sensor_name, sensor_should_enable, sensor_registry.sensors[i]->enabled);
            if(sensor_registry.sensors[i]->enabled != sensor_should_enable) {
                //Toggle.
                sensor_registry.sensors[i]->enabled = sensor_should_enable;
                if(sensor_registry.sensors[i]->state_toggle_callback != NULL) {
                    sensor_registry.sensors[i]->state_toggle_callback(sensor_should_enable);
                }
                toggle_count++;
            }
        }
        LOGDEBUG("Changed state of %d sensors\n", toggle_count);
    }

    if(release_lock_fn != NULL) {
        release_lock_fn();
    }
    
    return result;
}

//! Reads the list of enabled sensors from Eucalyptus configuration
//! files and ensures that only those are enabled on the system.
int update_sensors_list()
{

    int ret = EUCA_OK;
    char **names = NULL;

    char *tmpstr = configFileValue(SENSOR_LIST_CONF_PARAM_NAME);
    if (tmpstr == NULL) {
        LOGDEBUG("%s parameter is missing from config file\n", SENSOR_LIST_CONF_PARAM_NAME);
    } else {
        names = from_var_to_char_list(tmpstr);
        EUCA_FREE(tmpstr);
    }

    char **sensors = { NULL }; // if no config value or an empty list
    if (names != NULL) {
        sensors = names;
    }

    ret = conf_stats((const char **)sensors);

    if (names) {
        for (int i=0; names[i]; i++) {
            EUCA_FREE(names[i]);
        }
        EUCA_FREE(names);
    }

    return ret;
}

//! Zeroes the configuration and sets the sensor count to zero.
//! This does a hard reset of the config while protected in the mutex
int flush_sensor_registry() {

    //Handle the null case for full idempotency
    if(get_lock_fn != NULL) {
        get_lock_fn();
    }
    
    {
        //Zero the registry
        sensor_registry.sensor_count = 0;
        bzero(sensor_registry.sensors, MAX_SENSOR_COUNT * sizeof(struct internal_sensor*));
        is_initialized = FALSE;
    }

    if(release_lock_fn != NULL) {
        release_lock_fn();
    }
    
    return EUCA_OK;
}

//! ***********UNIT TESTS ****************
#ifdef _UNIT_TEST
static json_object **test_get_msg_stats() {
    return &test_msg_stats;
}

static void test_set_msg_stats() {
    return;
}

void print_header(const char* name) {
    LOGINFO("\n\n***** Running test %s *****\n", name);
}

static char *testing_service_check_call() {
    return "check-ok";
}

static char *testing_service_state_call() {
    return "state-ok";
}

static void test_lock()
{
    return;
}

static void test_unlock()
{
    return;
}

//! Do a full init and execution run. Tests end-to-end
static int test_stats_run(const char *config_file_path) {
    print_header(__func__);
    char *(*check_call)() = testing_service_check_call;
    char *(*state_call)() = testing_service_state_call;
    const char *test_home = getenv(EUCALYPTUS_ENV_VAR_NAME);

    //Init the config file stuff
    LOGDEBUG("Setting up config files and values for init test\n");
    char configFiles[1][EUCA_MAX_PATH];
    bzero(configFiles[0], EUCA_MAX_PATH);
    euca_strncpy(configFiles[0], config_file_path, EUCA_MAX_PATH);
    configInitValues(configEntryKeysRestart, configEntryKeysNoRestart);
    readConfigFile(configFiles, 1);
    LOGDEBUG("Getting sensor list to validate config works\n");
    char *test_value = configFileValue(SENSOR_LIST_CONF_PARAM_NAME);
    if(!test_value) {
        LOGERROR("Config setup didn't work. Null value found\n");
        return EUCA_ERROR;
    } else {
        LOGINFO("Config file has enabled stats: %s\n", test_value);
    }
    LOGDEBUG("Done with config file checks\n");

    flush_sensor_registry(); //just to be sure from other tests
    initialize_message_sensor("testservice", 60, 60, test_get_msg_stats, test_set_msg_stats);
    initialize_service_state_sensor("testservice", 60, 60, state_call, check_call);

    if(init_stats(test_home, "testservice", test_lock, test_unlock) != EUCA_OK) {
        LOGERROR("Error initialing stats\n");
        flush_sensor_registry();
        return EUCA_ERROR;
    }

    LOGINFO("Setting some message stats and doing an internal run\n");
    //populate some stats for the message stats
    update_message_stats(test_msg_stats, "fakemessage", 500, 0);
    update_message_stats(test_msg_stats, "fakemessageDescribe", 250, 0);
    update_message_stats(test_msg_stats, "fakemessageRun", 200, 0);

    int ret = internal_sensor_pass(TRUE);
    flush_sensor_registry();
    return ret;
}

static int test_sensor_registration() {    
    print_header(__func__);
    const char *name1 = "sensor1";
    const char *name2 = "sensor2";
    const char *name3 = "sensor1.sensor2";

    struct internal_sensor sensor1;
    strcpy(sensor1.config_name, name1);
    strcpy(sensor1.sensor_name, name1);
    sensor1.enabled = 0;
    sensor1.sensor_function = NULL;

    struct internal_sensor sensor2;
    strcpy(sensor2.config_name, name2);
    strcpy(sensor2.sensor_name, name2);
    sensor2.enabled = 0;
    sensor2.sensor_function = NULL;

    struct internal_sensor sensor3;
    strcpy(sensor3.config_name, name3);
    strcpy(sensor3.sensor_name, name3);
    sensor3.enabled = 0;
    sensor3.sensor_function = NULL;

    if(is_registered_sensor_name(name1)) {
        LOGERROR("Should not be registered name %s\n", name1);
        return EUCA_ERROR;
    }

    if(is_registered_sensor_name(name2)) {
        LOGERROR("Should not be registered name %s\n", name2);
        return EUCA_ERROR;
    }

    if(is_registered_sensor_name(name3)) {
        LOGERROR("Should not be registered name %s\n", name3);
        return EUCA_ERROR;
    }

    register_sensor(&sensor1);

    if(!is_registered_sensor_name(name1)) {
        LOGERROR("Should be registered name %s\n", name1);
        return EUCA_ERROR;
    }

    if(is_registered_sensor_name(name2)) {
        LOGERROR("Should not be registered name %s\n", name2);
        return EUCA_ERROR;
    }

    if(is_registered_sensor_name(name3)) {
        LOGERROR("Should not be registered name %s\n", name3);
        return EUCA_ERROR;
    }

    register_sensor(&sensor2);

    if(!is_registered_sensor_name(name1)) {
        LOGERROR("Should be registered name %s\n", name1);
        return EUCA_ERROR;
    }

    if(!is_registered_sensor_name(name2)) {
        LOGERROR("Should be registered name %s\n", name2);
        return EUCA_ERROR;
    }

    if(is_registered_sensor_name(name3)) {
        LOGERROR("Should not be registered name %s\n", name3);
        return EUCA_ERROR;
    }

    register_sensor(&sensor3);

    if(!is_registered_sensor_name(name1)) {
        LOGERROR("Should be registered name %s\n", name1);
        return EUCA_ERROR;
    }

    if(!is_registered_sensor_name(name2)) {
        LOGERROR("Should be registered name %s\n", name2);
        return EUCA_ERROR;
    }

    if(!is_registered_sensor_name(name3)) {
        LOGERROR("Should be registered name %s\n", name3);
        return EUCA_ERROR;
    }

    if(is_registered_sensor_name("somename")) {
        LOGERROR("Should not be registered name %s\n", name1);
        return EUCA_ERROR;
    }
    
    flush_sensor_registry();
    return EUCA_OK;
}

int main(int argc, char** argv) {
    int success, failed;
    success = failed = 0;
    if(argc < 2) {
        fprintf(stderr, "Requires 1 argument: config file absolute path\n");
        exit(1);
    }

    //get log params
    int current_level, roll_out;
    long bytes_out;
    log_params_get(&current_level, &roll_out, &bytes_out);
    log_params_set(log_level_int("TRACE"), roll_out, bytes_out);

    if(test_sensor_registration() != EUCA_OK) {
        LOGERROR("Failed sensor registration test\n");
        failed++;
    } else {
        LOGINFO("Sensor registration check passed\n");
        success++;
    }

    char *config_path = argv[1];
    if(test_stats_run(config_path) != EUCA_OK) {
        LOGERROR("Failed init stats test\n");
        failed++;
    } else {
        LOGINFO("Init stats test passed\n");
        success++;
    }

    LOGINFO("Results: %d passed, %d failed\n", success, failed);
        
    return failed;
}

#endif

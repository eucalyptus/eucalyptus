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
//! @file net/eucanetd_vpc.c
//! Implementation of the MIDONET VPC Network Driver Interface. This Network Driver
//! for EUCANETD.
//!
//! Coding Standard:
//! Every function that has multiple words must follow the word1_word2_word3() naming
//! convention and variables must follow the 'word1Word2Word3()' convention were no
//! underscore is used and every word, except for the first one, starts with a capitalized
//! letter. Whenever possible, prefixing a variable name with one or more of the following
//! qualifier would help reading code:
//!     - p - indicates a variable is a pointer (example: int *pAnIntegerPointer)
//!     - s - indicates a string variable (examples: char sThisString[10], char *psAnotherString). When 's' is used on its own, this mean a static string.
//!     - a - indicates an array of objects (example: int aAnArrayOfInteger[10])
//!     - g - indicates a variable with global scope to the file or application (example: static eucanetdConfig gConfig)
//!
//! The network driver APIs must implement the following function:
//!     - network_driver_init()
//!     - network_driver_cleanup()
//!     - network_driver_system_flush()
//!     - network_driver_system_scrub()
//!     - network_driver_implement_network()
//!     - network_driver_implement_sg()
//!     - network_driver_implement_addressing()
//!
//! Any other function implemented within the scope of this network driver must have its name
//! start with the mode name followed by an underscore and the rest of the function name. For example,
//! if the more name is "TEMPLATE", a non-driver API function would be named like: template_create_dhcp_configuration().
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <pwd.h>
#include <dirent.h>
#include <errno.h>
#include <netdb.h>
#include <net/if.h>
#include <net/ethernet.h>
#include <netinet/if_ether.h>
#include <netinet/ip.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <euca_network.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>
#include <config.h>
#include <sequence_executor.h>
#include <atomic_file.h>
#include <signal.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "eucanetd_config.h"
#include "euca_gni.h"
#include "euca_lni.h"
#include "eucanetd.h"
#include "eucanetd_util.h"
#include "euca-to-mido.h"

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
extern int midonet_api_system_changed;
extern int midocache_invalid;

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

//! Set to TRUE when driver is initialized
static boolean gInitialized = FALSE;
static boolean gTunnelZoneOk = FALSE;

//! Midonet pluggin specific configuration
mido_config *pMidoConfig = NULL;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name MIDONET VPC Mode Network Driver APIs
static int network_driver_init(eucanetdConfig *pConfig);
static int network_driver_cleanup(globalNetworkInfo *pGni, boolean forceFlush);
static int network_driver_system_flush(globalNetworkInfo *pGni);
static int network_driver_system_maint(globalNetworkInfo *pGni, lni_t *pLni);
static u32 network_driver_system_scrub(globalNetworkInfo *pGni,
        globalNetworkInfo *pGniApplied, lni_t *pLni);
static int network_driver_handle_signal(globalNetworkInfo *pGni, int signal);
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to see if the driver has been initialized
#define IS_INITIALIZED()                         ((gInitialized == TRUE) ? TRUE : FALSE)

//! Macro to get the driver name
#define DRIVER_NAME()                            midoVpcDriverHandler.name

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              CALLBACK STRUCTURE                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! TEMPLATE driver operation handlers
struct driver_handler_t midoVpcDriverHandler = {
    .name = NETMODE_VPCMIDO,
    .init = network_driver_init,
    .cleanup = network_driver_cleanup,
    .system_flush = network_driver_system_flush,
    .system_maint = network_driver_system_maint,
    .system_scrub = network_driver_system_scrub,
    .implement_network = NULL,
    .implement_sg = NULL,
    .implement_addressing = NULL,
    .handle_signal = network_driver_handle_signal,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Initialize the network driver.
//!
//! @param[in] pConfig a pointer to our application configuration
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre
//!     - The core application configuration must be completed prior calling
//!     - The driver should not be already initialized (if its the case, a no-op will occur)
//!     - The pConfig parameter must not be NULL
//!
//! @post
//!     On success the driver is properly configured. On failure, the state of
//!     the driver is non-deterministic. If the driver was previously initialized,
//!     this will result into a no-op.
//!
//! @note
//!
static int network_driver_init(eucanetdConfig * pConfig)
{
    int rc = 0;

    LOGINFO("Initializing '%s' network driver.\n", DRIVER_NAME());

    // Make sure our given pointer is valid
    if (!pConfig) {
        LOGERROR("Failure to initialize '%s' networking mode. Invalid configuration parameter provided.\n", DRIVER_NAME());
        return (1);
    }
    // Are we already initialized?
    if (IS_INITIALIZED()) {
        LOGERROR("Networking '%s' mode already initialized. Skipping!\n", DRIVER_NAME());
        return (0);
    }

    pMidoConfig = EUCA_ZALLOC_C(1, sizeof (mido_config));
    pMidoConfig->config = pConfig;
    //rc = initialize_mido(pMidoConfig, pConfig->eucahome, pConfig->flushmode, pConfig->disable_l2_isolation, pConfig->midoeucanetdhost, pConfig->midogwhosts,
    //        pConfig->midopubnw, pConfig->midopubgwip, "169.254.0.0", "17");
    rc = initialize_mido(pMidoConfig, pConfig, "169.254.0.0", "17");
    if (rc) {
        LOGERROR("could not initialize mido: please ensure that all required config options for VPCMIDO mode are set\n");
        EUCA_FREE(pMidoConfig);
        return (1);
    }
    
    // We are now initialized
    gInitialized = TRUE;

    return (0);
}

//!
//! Cleans up the network driver. This will work even if the initial initialization
//! fail for any reasons. This will reset anything that could have been half-way or
//! fully configured. If forceFlush is set, then a network flush will be performed.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] forceFlush set to TRUE if a network flush needs to be performed
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre
//!     The driver should have been initialized already
//!
//! @post
//|     On success, the network driver has been cleaned up and the system flushed
//!     if forceFlush was set. On failure, the system state will be non-deterministic.
//!
//! @note
//!
static int network_driver_cleanup(globalNetworkInfo *pGni, boolean forceFlush)
{
    int ret = 0;

    if (forceFlush) {
        if (network_driver_system_flush(pGni)) {
            LOGERROR("Fail to flush network artifacts during network driver cleanup. See above log errors for details.\n");
            ret = 1;
        }
    }
    midonet_api_cleanup();
    gInitialized = FALSE;
    return (ret);
}

//!
//! Responsible for flushing any networking artifacts implemented by this
//! network driver.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre
//!     The driver must be initialized already
//!
//! @post
//!     On success, all networking mode artifacts will be flushed from the system. If any
//!     failure occurred. The system is left in a non-deterministic state and a subsequent
//!     call to this API may resolve the remaining issues.
//!
static int network_driver_system_flush(globalNetworkInfo *pGni)
{
    int rc = 0;
    int ret = 0;

    LOGINFO("Flushing '%s' network driver artifacts.\n", DRIVER_NAME());

    // Is our driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to flush the networking artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }

    if (pMidoConfig->config->flushmode) {
        switch(pMidoConfig->config->flushmode) {
            case FLUSH_DYNAMIC:
                LOGINFO("Flushing objects in MidoNet (keep the core intact)\n");
                rc = do_midonet_teardown(pMidoConfig);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_ALL:
                LOGINFO("Flush all objects in MidoNet\n");
                rc = do_midonet_teardown(pMidoConfig);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_MIDO_CHECKDUPS:
                LOGINFO("Check for duplicate objects in MidoNet\n");
                rc = do_midonet_delete_dups(pMidoConfig, TRUE);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_MIDO_DUPS:
                LOGINFO("Flush duplicate objects in MidoNet\n");
                rc = do_midonet_delete_dups(pMidoConfig, FALSE);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_MIDO_CHECKUNCONNECTED:
                LOGINFO("Check for unconnected objects in MidoNet\n");
                rc = do_midonet_delete_vpc_object(pMidoConfig, "unconnected", TRUE);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_MIDO_UNCONNECTED:
                LOGINFO("Flush unconnected objects in MidoNet\n");
                rc = do_midonet_delete_vpc_object(pMidoConfig, "unconnected", FALSE);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_MIDO_CHECKVPC:
                LOGINFO("Check %s health in MidoNet\n", pMidoConfig->config->flushmodearg);
                rc = do_midonet_delete_vpc_object(pMidoConfig, pMidoConfig->config->flushmodearg, TRUE);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_MIDO_VPC:
                rc = do_midonet_delete_vpc_object(pMidoConfig, pMidoConfig->config->flushmodearg, FALSE);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_MIDO_LISTVPC:
                rc = do_midonet_delete_vpc_object(pMidoConfig, "list", TRUE);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_MIDO_TEST:
                do_midonet_delete_vpc_object(pMidoConfig, "test", TRUE);
                break;
            case FLUSH_NONE:
            default:
                LOGERROR("check for eucanetd bug: should never reach this point.\n");
                break;                
        }
        free_mido_config(pMidoConfig);
        EUCA_FREE(pMidoConfig);
        pMidoConfig = NULL;
        gInitialized = FALSE;
    }

    return (ret);
}

//!
//! Maintenance activities to be executed when eucanetd is idle between polls.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] pLni a pointer to the Local Network Information structure
//!
//! @return 0 on success, 1 otherwise.
//!
//! @see
//!
//! @pre
//!     - pGni must not be NULL. pLni is ignored.
//!     - The driver must be initialized prior to calling this API.
//!
//! @post
//!
//! @note
//!
static int network_driver_system_maint(globalNetworkInfo *pGni, lni_t *pLni)
{
    int rc = 0;
    struct timeval tv;
    
    LOGTRACE("Running maintenance for '%s' network driver.\n", DRIVER_NAME());
    eucanetd_timer(&tv);

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to run maintenance activities. Driver '%s' not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Need a valid global network view
    if (!pGni) {
        LOGERROR("Failed to run maintenance for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    rc = do_midonet_maint(pMidoConfig);
    return (rc);
}

//!
//! This API is invoked when eucanetd catches an USR1 or USR2 signal.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] signal received signal
//!
//! @return 0 on success, 1 otherwise.
//!
//! @see
//!
//! @pre
//!     - pGni must not be NULL
//!     - The driver must be initialized prior to calling this API.
//!
//! @post
//!
//! @note
//!
static int network_driver_handle_signal(globalNetworkInfo *pGni, int signal) {
    int rc = 0;
    LOGTRACE("Handling singal %d for '%s' network driver.\n", signal, DRIVER_NAME());

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to handle signal. Driver '%s' not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Is the global network view structure NULL?
    if (!pGni) {
        LOGERROR("Failed to handle signal for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    switch (signal) {
        case SIGUSR1:
            mido_info_http_count_total();
            mido_info_midocache();
            break;
        case SIGUSR2:
            LOGINFO("Going to invalidate midocache\n");
            rc = midonet_api_cache_refresh_v_threads(MIDO_CACHE_REFRESH_ALL);
            if (rc) {
                LOGERROR("failed to retrieve objects from MidoNet.\n");
                midocache_invalid = 1;
                break;
            }

            rc = do_midonet_populate(pMidoConfig);
            if (rc) {
                LOGERROR("failed to populate euca VPC models\n");
                midocache_invalid = 1;
            }
            break;
        default:
            break;
    }
    return (0);
}

//!
//! For MIDONET VPC mode, all is done in this driver API.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] pLni a pointer to the Local Network Information structure
//!
//! @return EUCANETD_RUN_NO_API or EUCANETD_RUN_ERROR_API
//!
//! @see
//!
//! @pre
//!     - Both pGni and pLni must not be NULL
//!     - The driver must be initialized prior to calling this API.
//!
//! @post
//!
//! @note
//!
static u32 network_driver_system_scrub(globalNetworkInfo *pGni, globalNetworkInfo *pGniApplied, lni_t *pLni)
{
    int rc = 0;
    u32 ret = EUCANETD_RUN_NO_API;
    char versionFile[EUCA_MAX_PATH];
    int check_tz_attempts = 30;
    struct timeval tv;

    eucanetd_timer(&tv);
    // Make sure midoname buffer is available
    midonet_api_cache_midos_init();

    if (!gTunnelZoneOk) {
        LOGDEBUG("Checking MidoNet tunnel-zone.\n");
        rc = 1;
    }
    while (!gTunnelZoneOk) {
        // Check tunnel-zone
        rc = check_mido_tunnelzone();
        if (rc) {
            if ((--check_tz_attempts) > 0) {
                sleep(3);
            } else {
                LOGERROR("Cannot proceed without a valid tunnel-zone.\n");
                return (EUCANETD_RUN_ERROR_API);
            }
        } else {
            gTunnelZoneOk = TRUE;
        }
    }

    bzero(versionFile, EUCA_MAX_PATH);

    // Need a valid global network view
    if (!pGni) {
        LOGERROR("Failed to scrub the system for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (ret);
    }

    if (!IS_INITIALIZED() || (pGni && pGniApplied && cmp_gni_vpcmido_config(pGni, pGniApplied))) {
        LOGINFO("(re)initializing %s driver.\n", DRIVER_NAME());
        if (pMidoConfig) {
            eucanetdConfig *configbak = pMidoConfig->config;
            free_mido_config(pMidoConfig);
            pMidoConfig->config = configbak;
        }
        //rc = initialize_mido(pMidoConfig, config->eucahome, config->flushmode,
        //        config->disable_l2_isolation, config->midoeucanetdhost, config->midogwhosts,
        //        config->midopubnw, config->midopubgwip, "169.254.0.0", "17");
        rc = initialize_mido(pMidoConfig, pMidoConfig->config, "169.254.0.0", "17");
        if (rc) {
            LOGERROR("failed to (re)initialize config options for VPCMIDO mode are set\n");
            return (EUCANETD_RUN_ERROR_API);
        }
        pGniApplied = NULL;
    }
    LOGTRACE("euca VPCMIDO system state: %s\n", midonet_api_system_changed == 0 ? "CLEAN" : "DIRTY");
    rc = do_midonet_update(pGni, pGniApplied, pMidoConfig);

    if (rc != 0) {
        LOGERROR("failed to update midonet: check log for details\n");
        if (rc < 0) {
            // Accept errors in instances/interface implementation.
            ret = EUCANETD_VPCMIDO_IFERROR;
        } else {
            ret = EUCANETD_RUN_ERROR_API;
        }
    } else {
        LOGTRACE("Networking state sync: updated successfully in %.2f ms\n", eucanetd_timer_usec(&tv) / 1000.0);
    }

    return (ret);
}

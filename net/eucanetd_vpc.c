// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
//! The network driver APIs must implement the following functions:
//!     - network_driver_init()
//!     - network_driver_cleanup()
//!     - network_driver_system_flush()
//!     - network_driver_system_scrub()
//!     Optional functions:
//!     - network_driver_upgrade()
//!     - network_driver_implement_network()
//!     - network_driver_implement_sg()
//!     - network_driver_implement_addressing()
//!     - network_driver_system_maint()
//!     - network_driver_handle_signal()
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
#include "euca_gni.h"
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
static int network_driver_upgrade(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_cleanup(eucanetdConfig *pConfig, globalNetworkInfo *pGni, boolean forceFlush);
static int network_driver_system_flush(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_system_maint(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static u32 network_driver_system_scrub(eucanetdConfig *pConfig, globalNetworkInfo *pGni, globalNetworkInfo *pGniApplied);
//static int network_driver_implement_network(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
//static int network_driver_implement_sg(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
//static int network_driver_implement_addressing(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_handle_signal(eucanetdConfig *pConfig, globalNetworkInfo *pGni, int signal);
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
    .upgrade = network_driver_upgrade,
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

/**
 * Initialize this network driver.
 * - The core application configuration must be completed prior calling
 * - The driver should not be already initialized (if its the case, a no-op will occur)
 * - The pConfig parameter must not be NULL
 *
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_init(eucanetdConfig *pConfig) {
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

    if (!pMidoConfig) {
        pMidoConfig = EUCA_ZALLOC_C(1, sizeof (mido_config));
    }
    pMidoConfig->config = pConfig;
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

/**
 * Cleans up the network driver. This will work even if the initial initialization
 * fail for any reasons. This will reset anything that could have been half-way or
 * fully configured. If forceFlush is set, then a network flush will be performed.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @param forceFlush [in] set to TRUE if a network flush needs to be performed
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_cleanup(eucanetdConfig *pConfig, globalNetworkInfo *pGni, boolean forceFlush) {
    int ret = 0;

    LOGINFO("Cleaning up '%s' network driver.\n", DRIVER_NAME());

    if (forceFlush) {
        if (network_driver_system_flush(pConfig, pGni)) {
            LOGERROR("Fail to flush network artifacts during network driver cleanup. See above log errors for details.\n");
            ret = 1;
        }
    }
    midonet_api_cleanup();
    gInitialized = FALSE;
    return (ret);
}

/**
 * Perform network driver upgrade. This function should be invoked once when eucanetd
 * starts.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_upgrade(eucanetdConfig *pConfig, globalNetworkInfo *pGni) {
    int ret = 0;

    // Skip upgrade in flush mode
    if (pMidoConfig && pMidoConfig->config && (pMidoConfig->config->flushmode != FLUSH_NONE)) {
        LOGTRACE("\tflush mode selected. Skipping upgrade\n");
        return (0);
    }

    LOGINFO("Upgrade '%s' network driver.\n", DRIVER_NAME());
    if (!pConfig || !pGni) {
        LOGERROR("Invalid argument: cannot process upgrade with NULL config.\n");
        return (1);
    }

    // Make sure midoname buffer is available
    midonet_api_cache_midos_init();

    u32 mido_euca_version = 0;
    char *mido_euca_version_str = NULL;
    midoname **ipgs = NULL;
    int max_ipgs = 0;
    midoname **ips = NULL;
    int max_ips = 0;
    int rc = mido_get_ipaddrgroups(VPCMIDO_TENANT, &ipgs, &max_ipgs);
    if (!rc && max_ipgs) {
        for (int i = 0; i < max_ipgs; i++) {
            if (!strcmp(ipgs[i]->name, "euca_version")) {
                rc = mido_get_ipaddrgroup_ips(ipgs[i], &ips, &max_ips);
                if (!rc && ips && max_ips) {
                    mido_euca_version = euca_version_dot2hex(ips[0]->ipagip->ip);
                    mido_euca_version_str = hex2dot(mido_euca_version);
                    LOGTRACE("\tFound %s artifacts\n", mido_euca_version_str);
                }
                EUCA_FREE(ips);
            }
        }
    }
    EUCA_FREE(ipgs);

    EUCA_FREE(mido_euca_version_str);
    return (ret);
}

/**
 * Responsible for flushing any networking artifacts implemented by this
 * network driver.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_system_flush(eucanetdConfig *pConfig, globalNetworkInfo *pGni) {
    int rc = 0;
    int ret = 0;

    // Is our driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to flush the networking artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }

    if (pMidoConfig->config->flushmode) {
        switch(pMidoConfig->config->flushmode) {
            case FLUSH_MIDO_DYNAMIC:
                LOGINFO("Flushing objects in MidoNet (keep the core intact)\n");
                rc = do_midonet_teardown(pMidoConfig);
                if (rc) {
                    ret = 1;
                }
                break;
            case FLUSH_MIDO_ALL:
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

/**
 * This API is invoked when eucanetd will potentially be idle. For example, after
 * populating the global network state, eucanetd detects that no action needs to
 * be taken. Good for pre-populating cache, or flushing dirty cache - so these
 * actions are not necessary in the regular iteration.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_system_maint(eucanetdConfig *pConfig, globalNetworkInfo *pGni) {
    int rc = 0;
    struct timeval tv;
    
    LOGTRACE("Running maintenance for '%s' network driver.\n", DRIVER_NAME());
    eucanetd_timer(&tv);

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to run maintenance activities. Driver '%s' not initialized.\n", DRIVER_NAME());
        return (1);
    }

    // Make sure midoname buffer is available
    midonet_api_cache_midos_init();

    pMidoConfig->config = pConfig;
    rc = do_midonet_maint(pMidoConfig);
    return (rc);
}

/**
 * This API checks the new GNI against the system view to decide what really
 * needs to be done.
 * For MIDONET VPC mode, all is done in this driver API.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @param pGniApplied [in] a pointer to the previously successfully implemented GNI
 * @return A bitmask indicating what needs to be done. The following bits are
 *         the ones to look for: EUCANETD_RUN_NETWORK_API, EUCANETD_RUN_SECURITY_GROUP_API
 *         and EUCANETD_RUN_ADDRESSING_API.
 */
static u32 network_driver_system_scrub(eucanetdConfig *pConfig, globalNetworkInfo *pGni, globalNetworkInfo *pGniApplied) {
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
    if (!pConfig || !pGni) {
        LOGERROR("Failed to scrub the system for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (ret);
    }

    if (!IS_INITIALIZED() || (pGni && pGniApplied && cmp_gni_vpcmido_config(pGni, pGniApplied))) {
        LOGINFO("(re)initializing %s driver.\n", DRIVER_NAME());
        if (pMidoConfig) {
            free_mido_config(pMidoConfig);
        } else {
            LOGERROR("failed to (re)initialize config options: VPCMIDO driver not initialized\n");
            return (EUCANETD_RUN_ERROR_API);
        }
        rc = network_driver_init(pConfig);
        if (rc) {
            LOGERROR("failed to (re)initialize config options\n");
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

/**
 * This API is invoked when eucanetd catches an USR1 or USR2 signal.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @param signal [in] received signal
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_handle_signal(eucanetdConfig *pConfig, globalNetworkInfo *pGni, int signal) {
    LOGTRACE("Handling singal %d for '%s' network driver.\n", signal, DRIVER_NAME());

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to handle signal. Driver '%s' not initialized.\n", DRIVER_NAME());
        return (1);
    }

    if (!pConfig) {
        LOGERROR("Invalid argument: cannot handle signal with NULL config.\n");
        return (1);
    }
    pMidoConfig->config = pConfig;
    switch (signal) {
        case SIGUSR1:
            mido_info_midonetapi();
            mido_info_http_count_total();
            mido_info_midocache();
            char *bgprecovery = NULL;
            bgprecovery = discover_mido_bgps(pMidoConfig);
            if (bgprecovery && strlen(bgprecovery)) {
                LOGINFO("\nmido BGP configuration (for manual recovery):\n%s\n", bgprecovery);
            }
            EUCA_FREE(bgprecovery);
            break;
        case SIGUSR2:
            LOGINFO("Going to invalidate midocache\n");
            midocache_invalid = 1;
            break;
        default:
            break;
    }
    return (0);
}


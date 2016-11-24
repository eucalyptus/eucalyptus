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
//! @file net/eucanetd_template.c
//! Template file containing the necessary information to create a new network driver
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

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "euca_gni.h"
#include "eucanetd.h"
#include "eucanetd_util.h"

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

//! Set to TRUE when driver is initialized
static boolean gInitialized = FALSE;

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
//! @name TEMPLATE Mode Network Driver APIs
static int network_driver_init(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_upgrade(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_cleanup(eucanetdConfig *pConfig, globalNetworkInfo *pGni, boolean forceFlush);
static int network_driver_system_flush(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_system_maint(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static u32 network_driver_system_scrub(eucanetdConfig *pConfig, globalNetworkInfo *pGni, globalNetworkInfo *pGniApplied);
static int network_driver_implement_network(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_implement_sg(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_implement_addressing(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
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
#define DRIVER_NAME()                            templateDriverHandler.name

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              CALLBACK STRUCTURE                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! TEMPLATE driver operation handlers
struct driver_handler_t templateDriverHandler = {
    .name = "TEMPLATE",
    .init = network_driver_init,
    .upgrade = network_driver_upgrade,
    .cleanup = network_driver_cleanup,
    .system_flush = network_driver_system_flush,
    .system_maint = network_driver_system_maint,
    .system_scrub = network_driver_system_scrub,
    .implement_network = network_driver_implement_network,
    .implement_sg = network_driver_implement_sg,
    .implement_addressing = network_driver_implement_addressing,
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
 * @param pGni [in] a pointer to the Global Network Information structure
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_init(eucanetdConfig *pConfig, globalNetworkInfo *pGni) {
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

    if (PEER_IS_NC(eucanetdPeer)) {

    } else if (PEER_IS_CC(eucanetdPeer)) {

    }
    // We are now initialize
    gInitialized = TRUE;
    return (0);
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

    LOGINFO("Upgrade '%s' network driver.\n", DRIVER_NAME());
    if (!pConfig || !pGni) {
        LOGERROR("Failed to run upgrade for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    gInitialized = FALSE;
    return (ret);
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
    gInitialized = FALSE;
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
    LOGINFO("Flushing '%s' network driver artifacts.\n", DRIVER_NAME());

    // Is our driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to flush the networking artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    if (!pConfig || !pGni) {
        LOGERROR("Failed to run maintenance activities for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    if (PEER_IS_NC(eucanetdPeer)) {

    } else if (PEER_IS_CC(eucanetdPeer)) {

    }

    return (0);
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
    LOGTRACE("Maintenance activities for '%s' network driver.\n", DRIVER_NAME());

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to run maintenance. Driver '%s' not initialized.\n", DRIVER_NAME());
        return (1);
    }
    if (!pConfig || !pGni) {
        LOGERROR("Failed to run maintenance activities for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    return (0);
}

/**
 * This API checks the new GNI against the system view to decide what really
 * needs to be done.
 * In practice, network drivers will execute all that needs to be done instead
 * of just checking. In many cases, implement artifacts while detecting GNI and
 * system view differences is more efficient.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @param pGniApplied [in] a pointer to the previously successfully implemented GNI
 * @return A bitmask indicating what needs to be done. The following bits are
 *         the ones to look for: EUCANETD_RUN_NETWORK_API, EUCANETD_RUN_SECURITY_GROUP_API
 *         and EUCANETD_RUN_ADDRESSING_API.
 */
static u32 network_driver_system_scrub(eucanetdConfig *pConfig, globalNetworkInfo *pGni, globalNetworkInfo *pGniApplied) {
    u32 ret = EUCANETD_RUN_NO_API;

    LOGTRACE("Scrubbing for '%s' network driver.\n", DRIVER_NAME());

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to scrub the system for network artifacts. Driver '%s' not initialized.\n", DRIVER_NAME());
        return (EUCANETD_RUN_NO_API);
    }
    // Are the global and local network view structures NULL?
    if (!pGni || !pConfig) {
        LOGERROR("Failed to scrub '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (EUCANETD_RUN_NO_API);
    }

    if (PEER_IS_NC(eucanetdPeer)) {

    } else if (PEER_IS_CC(eucanetdPeer)) {

    }

    return (ret);
}

/**
 * This takes care of implementing the network artifacts necessary. This will add or
 * remove devices, tunnels, etc. as necessary.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_implement_network(eucanetdConfig *pConfig, globalNetworkInfo *pGni) {
    LOGTRACE("Implementing network artifacts for 'TEMPLATE' network driver.\n");

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to implement network artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Are the global and local network view structures NULL?
    if (!pGni || !pConfig) {
        LOGERROR("Failed to implement network artifacts for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    if (PEER_IS_NC(eucanetdPeer)) {

    } else if (PEER_IS_CC(eucanetdPeer)) {

    }

    return (0);
}

/**
 * This takes care of implementing the security-group artifacts necessary. This will add or
 * remove networking rules pertaining to the groups and their membership.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_implement_sg(eucanetdConfig *pConfig, globalNetworkInfo *pGni) {
    LOGTRACE("Implementing security-group artifacts for '%s' network driver.\n", DRIVER_NAME());

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to implement security-group artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Are the global and local network view structures NULL?
    if (!pGni || !pConfig) {
        LOGERROR("Failed to implement security-group artifacts for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    if (PEER_IS_NC(eucanetdPeer)) {

    } else if (PEER_IS_CC(eucanetdPeer)) {

    }

    return (0);
}

/**
 * This takes care of implementing the addressing artifacts necessary. This will add or
 * remove IP addresses and elastic IPs for each instances.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_implement_addressing(eucanetdConfig *pConfig, globalNetworkInfo *pGni) {
    LOGTRACE("Implementing addressing artifacts for '%s' network driver.\n", DRIVER_NAME());

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to implement addressing artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Are the global and local network view structures NULL?
    if (!pGni || !pConfig) {
        LOGERROR("Failed to implement addressing artifacts for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    if (PEER_IS_NC(eucanetdPeer)) {

    } else if (PEER_IS_CC(eucanetdPeer)) {

    }

    return (0);
}

/**
 * This API is invoked when eucanetd catches an USR1 or USR2 signal.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @param signal [in] received signal
 * @return 0 on success. Integer number on failure.
 */
static int network_driver_handle_signal(eucanetdConfig *pConfig, globalNetworkInfo *pGni, int signal) {
    LOGDEBUG("Handling singal %d for '%s' network driver.\n", signal, DRIVER_NAME());

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to handle signal. Driver '%s' not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Is the global network view structure NULL?
    if (!pGni || !pConfig) {
        LOGERROR("Failed to handle signal for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    return (0);
}


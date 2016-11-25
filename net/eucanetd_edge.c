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
//! @file net/eucanetd_edge.c
//! Implementation of the EDGE Network Driver Interface. This Network Driver
//! Interface implements the driver_handler_t APIs.
//!
//! Coding Standard:
//! Every function that has multiple words must follow the word1_word2_word3() naming
//! convention and variables must follow the 'word1Word2Word3()' convention were no
//! underscore is used and every word, except for the first one, starts with a capitalized
//! letter. Whenever possible (not mendatory but strongly encouraged), prefixing a variable
//! name with one or more of the following qualifier would help reading code:
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
//! if the mode name is "TEMPLATE", a non-driver API function would be named like: template_create_dhcp_configuration().
//!
//! @todo
//!  - Implement the scrub API
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

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <log.h>
#include <hash.h>
#include <math.h>
#include <http.h>
#include <config.h>
#include <atomic_file.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "eucalyptus-config.h"
#include "euca_gni.h"
#include "eucanetd.h"
#include "eucanetd_util.h"
#include "eucanetd_edge.h"
#include "euca_arp.h"

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

static edge_config *edgeConfig_a = NULL;
static edge_config *edgeConfig_b = NULL;
static edge_config *edgeConfig = NULL;
static edge_config *edgeConfigApplied = NULL;

static edge_netmeter *netmeter = NULL;

static int edgeMaintCount = 0;

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
 //! @name EDGE Mode Network Driver APIs
static int network_driver_init(eucanetdConfig *pEucanetdConfig, globalNetworkInfo *pGni);
static int network_driver_upgrade(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_cleanup(eucanetdConfig *pConfig, globalNetworkInfo *pGni, boolean forceFlush);
static int network_driver_system_flush(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static int network_driver_system_maint(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
static u32 network_driver_system_scrub(eucanetdConfig *pConfig, globalNetworkInfo *pGni, globalNetworkInfo *pGniApplied);
//static int network_driver_implement_network(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
//static int network_driver_implement_sg(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
//static int network_driver_implement_addressing(eucanetdConfig *pConfig, globalNetworkInfo *pGni);
//static int network_driver_handle_signal(eucanetdConfig *pConfig, globalNetworkInfo *pGni, int signal);
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to see if the driver has been initialized
#define IS_INITIALIZED()                         ((gInitialized == TRUE) ? TRUE : FALSE)

//! Macro to get the driver name
#define DRIVER_NAME()                            edgeDriverHandler.name

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              CALLBACK STRUCTURE                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! EDGE driver operation handlers
struct driver_handler_t edgeDriverHandler = {
    .name = NETMODE_EDGE,
    .init = network_driver_init,
    .cleanup = network_driver_cleanup,
    .upgrade = network_driver_upgrade,
    .system_flush = network_driver_system_flush,
    .system_maint = network_driver_system_maint,
    .system_scrub = network_driver_system_scrub,
    .implement_network = NULL,
    .implement_sg = NULL,
    .implement_addressing = NULL,
    .handle_signal = NULL,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/**
 * Initialize EDGE network driver.
 * @param pEucanetdConfig [in] a pointer to eucanetd configuration structure
 * @param pGni [in] a pointer to the Global Network Information structure
 * @return 0 on success or 1 on any failure
 * 
 * @pre \li The core application configuration must be completed prior calling
 *      \li The driver must not be already initialized (if its the case, a no-op will occur)
 *      \li The pEucanetdConfig parameter must not be NULL
 *
 * @post On success the driver is properly configured. On failure, the state of
 *       the driver is non-deterministic. If the driver was previously initialized,
 *       this will result into a no-op.
 */
static int network_driver_init(eucanetdConfig *pEucanetdConfig, globalNetworkInfo *pGni) {
    int rc = 0;
    int ret = 0;
    LOGINFO("Initializing '%s' network driver.\n", DRIVER_NAME());

    // Make sure our given pointer is valid
    if (!pEucanetdConfig) {
        LOGERROR("Failure to initialize '%s' networking mode. Invalid configuration parameter provided.\n", DRIVER_NAME());
        return (1);
    }
    // Are we already initialized?
    if (IS_INITIALIZED()) {
        LOGERROR("Networking '%s' mode already initialized. Skipping!\n", DRIVER_NAME());
        return (0);
    }

    if (!pEucanetdConfig->ipt) {
        pEucanetdConfig->ipt = EUCA_ZALLOC_C(1, sizeof (ipt_handler));
        rc = ipt_handler_init(pEucanetdConfig->ipt, pEucanetdConfig->cmdprefix, NULL);
        if (rc) {
            LOGERROR("could not initialize ipt_handler: check above log errors for details\n");
            ret = 1;
        } else {
            ipt_handler_free(pEucanetdConfig->ipt);
        }
    }
    if (!pEucanetdConfig->ips) {
        pEucanetdConfig->ips = EUCA_ZALLOC_C(1, sizeof (ips_handler));
        rc = ips_handler_init(pEucanetdConfig->ips, pEucanetdConfig->cmdprefix);
        if (rc) {
            LOGERROR("could not initialize ips_handler: check above log errors for details\n");
            ret = 1;
        } else {
            ips_handler_free(pEucanetdConfig->ips);
        }
    }
    if (!pEucanetdConfig->ebt) {
        pEucanetdConfig->ebt = EUCA_ZALLOC_C(1, sizeof (ebt_handler));
        rc = ebt_handler_init(pEucanetdConfig->ebt, pEucanetdConfig->cmdprefix);
        if (rc) {
            LOGERROR("could not initialize ebt_handler: check above log errors for details\n");
            ret = 1;
        } else {
            ebt_handler_free(pEucanetdConfig->ebt);
        }
    }
    if (ret) {
        EUCA_FREE(pEucanetdConfig->ipt);
        EUCA_FREE(pEucanetdConfig->ips);
        EUCA_FREE(pEucanetdConfig->ebt);
        return (1);
    }
    netmeter = EUCA_ZALLOC_C(1, sizeof (edge_netmeter));
    edgeConfig_a = EUCA_ZALLOC_C(1, sizeof (edge_config));
    edgeConfig_a->config = pEucanetdConfig;
    edgeConfig_a->nmeter = netmeter;
    edgeConfig_b = EUCA_ZALLOC_C(1, sizeof (edge_config));
    edgeConfig_b->config = pEucanetdConfig;
    edgeConfig_b->nmeter = netmeter;
    edgeConfig = edgeConfigApplied = NULL;

    // We are now initialize
    gInitialized = TRUE;
    return (0);
}

/**
 * Upgrades 4.3 EDGE constructs to 4.4 EDGE constructs.
 * iptables chain names and ipset names are changed from EU_hash to sg-xxxxxxxx
 * naming style. Pre 4.3 iptables chains and ipsets are cleared - necessary iptables
 * chains and ipsets are created in the first successful eucanetd iteration.
 * 
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * 
 * @post
 *     - On success all EU_hash iptables chains and ipsets are flushed
 *     - On failure, the system is left in an undetermined state
 *
 * @TODO:
 *     This will not be needed for 4.4+ (if upgrade path from 4.3 is not supported)
 */
static int network_driver_upgrade(eucanetdConfig *pConfig, globalNetworkInfo *pGni) {
    int rc = 0;
    int ret = 0;

    LOGINFO("Upgrade 4.3 '%s' network driver artifacts.\n", DRIVER_NAME());

    if (!pConfig) {
        LOGWARN("Invalid argument: cannot upgrade with NULL config\n");
        return (1);
    }
    // this only applies to NC components
    if (!PEER_IS_NC(eucanetdPeer)) {
        // no-op
        return (0);
    }
    // Is our driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Upgrade 4.3 '%s' network driver artifacts failed. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // iptables
    ipt_handler_repopulate(pConfig->ipt);
    // delete legacy (pre 4.4) chains
    ipt_table_deletechainmatch(pConfig->ipt, "filter", "EU_");
    ipt_chain_flush(pConfig->ipt, "filter", "EUCA_FILTER_FWD");
    ipt_table_add_chain(pConfig->ipt, "filter", "EUCA_FILTER_FWD_DROPPED", "-", "[0:0]");
    ipt_chain_add_rule(pConfig->ipt, "filter", "EUCA_FILTER_FWD", "-A EUCA_FILTER_FWD -j EUCA_FILTER_FWD_DROPPED");
    ipt_handler_print(pConfig->ipt);
    rc = ipt_handler_deploy(pConfig->ipt);
    if (rc) {
        LOGERROR("Failed to upgrade 4.3 %s IP Tables artifacts.\n", DRIVER_NAME());
        ret = 1;
    }
    // ipsets
    ips_handler_repopulate(pConfig->ips);
    // delete legacy (pre 4.4) ipsets
    ips_handler_deletesetmatch(pConfig->ips, "EU_");
    ips_handler_print(pConfig->ips);
    rc = ips_handler_deploy(pConfig->ips, 1);
    if (rc) {
        LOGERROR("Failed to upgrade 4.3 %s ipset artifacts.\n", DRIVER_NAME());
        ret = 1;
    }
    // ebtables
    // no upgrade operation required

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
    free_edge_netmeter(netmeter);
    free_edge_config(edgeConfig_a);
    free_edge_config(edgeConfig_b);
    EUCA_FREE(netmeter);
    EUCA_FREE(edgeConfig_a);
    EUCA_FREE(edgeConfig_b);
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
    int rc = 0;
    int ret = 0;

    LOGINFO("Flushing '%s' network driver artifacts.\n", DRIVER_NAME());

    // this only applies to NC components
    if (!PEER_IS_NC(eucanetdPeer)) {
        // no-op
        return (0);
    }
    // Is our driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to flush the networking artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    if (!pConfig) {
        LOGWARN("Invalid argument: cannot flush %s with NULL config\n", DRIVER_NAME());
        return (1);
    }
    // iptables
    ipt_handler_repopulate(pConfig->ipt);
    ipt_chain_flush(pConfig->ipt, "raw", "EUCA_COUNTERS_IN");
    ipt_chain_flush(pConfig->ipt, "raw", "EUCA_COUNTERS_OUT");
    ipt_chain_flush(pConfig->ipt, "raw", "EUCA_RAW_PRE");
    ipt_chain_flush(pConfig->ipt, "filter", "EUCA_FILTER_FWD");
    ipt_chain_flush(pConfig->ipt, "filter", "EUCA_FILTER_FWD_DROPPED");
    ipt_chain_flush(pConfig->ipt, "nat", "EUCA_NAT_PRE");
    ipt_chain_flush(pConfig->ipt, "nat", "EUCA_NAT_POST");
    ipt_chain_flush(pConfig->ipt, "nat", "EUCA_NAT_OUT");

    ipt_table_deletechainmatch(pConfig->ipt, "filter", "sg-");

    // Flush core artifacts
    if (pConfig->flushmode == FLUSH_ALL) {
        ipt_table_deletechainmatch(pConfig->ipt, "raw", "EUCA_");
        ipt_table_deletechainmatch(pConfig->ipt, "filter", "EUCA_");
        ipt_table_deletechainmatch(pConfig->ipt, "nat", "EUCA_");
        ipt_chain_flush_rule(pConfig->ipt, "raw", "PREROUTING", "-A PREROUTING -j EUCA_RAW_PRE");
        ipt_chain_flush_rule(pConfig->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD_PREUSERHOOK");
        ipt_chain_flush_rule(pConfig->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD");
        ipt_chain_flush_rule(pConfig->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD_POSTUSERHOOK");
        ipt_chain_flush_rule(pConfig->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE_PREUSERHOOK");
        ipt_chain_flush_rule(pConfig->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE");
        ipt_chain_flush_rule(pConfig->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE_POSTUSERHOOK");
        ipt_chain_flush_rule(pConfig->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST_PREUSERHOOK");
        ipt_chain_flush_rule(pConfig->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST");
        ipt_chain_flush_rule(pConfig->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST_POSTUSERHOOK");
        ipt_chain_flush_rule(pConfig->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT_PREUSERHOOK");
        ipt_chain_flush_rule(pConfig->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT");
        ipt_chain_flush_rule(pConfig->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT_POSTUSERHOOK");
    }
    ipt_handler_print(pConfig->ipt);
    rc = ipt_handler_deploy(pConfig->ipt);
    if (rc) {
        LOGERROR("Failed to flush the IP Tables artifact in '%s' networking mode.\n", DRIVER_NAME());
        ret = 1;
    }

    // ipsets
    ips_handler_repopulate(pConfig->ips);

    ips_handler_deletesetmatch(pConfig->ips, "sg-");
    ips_handler_deletesetmatch(pConfig->ips, "EUCA_");
    if (pConfig->flushmode != FLUSH_ALL) {
        u32 euca_version = euca_version_dot2hex(EUCA_VERSION);
        char *strptra = hex2dot(euca_version);
        ips_handler_add_set(pConfig->ips, "EUCA_VERSION");
        ips_set_flush(pConfig->ips, "EUCA_VERSION");
        ips_set_add_ip(pConfig->ips, "EUCA_VERSION", strptra);
        EUCA_FREE(strptra);
    }
    ips_handler_print(pConfig->ips);
    rc = ips_handler_deploy(pConfig->ips, 1);
    if (rc) {
        LOGERROR("Failed to flush the IP Sets artifact in '%s' networking mode.\n", DRIVER_NAME());
        ret = 1;
    }

    // ebtables
    ebt_handler_repopulate(pConfig->ebt);
    ebt_chain_flush(pConfig->ebt, "filter", "EUCA_EBT_FWD");
    ebt_chain_flush(pConfig->ebt, "nat", "EUCA_EBT_NAT_PRE");
    ebt_chain_flush(pConfig->ebt, "nat", "EUCA_EBT_NAT_POST");
    // Flush core artifacts
    if (pConfig->flushmode == FLUSH_ALL) {
        ebt_table_deletechainmatch(pConfig->ebt, "filter", "EUCA_");
        ebt_table_deletechainmatch(pConfig->ebt, "nat", "EUCA_");
        ebt_chain_flush_rule(pConfig->ebt, "filter", "FORWARD", "-j EUCA_EBT_FWD");
        ebt_chain_flush_rule(pConfig->ebt, "nat", "PREROUTING", "-j EUCA_EBT_NAT_PRE");
        ebt_chain_flush_rule(pConfig->ebt, "nat", "POSTROUTING", "-j EUCA_EBT_NAT_POST");
    }
    rc = ebt_handler_deploy(pConfig->ebt);
    if (rc) {
        LOGERROR("Failed to flush the EB Tables artifact in '%s' networking mode.\n", DRIVER_NAME());
        ret = 1;
    }

    // Clear public IPs that have been mapped
    u32 *ips = NULL, *nms = NULL;
    int max_nets = 0;
    rc = 0;
    int i = 0;
    int j = 0;
    char cmd[EUCA_MAX_PATH] = "";
    char *strptra = NULL;

    if (getdevinfo(pConfig->pubInterface, &ips, &nms, &max_nets)) {
        // could not get interface info - skip public IP flush
        max_nets = 0;
        ips = NULL;
        nms = NULL;
        ret = 1;
        LOGERROR("Failed to flush public IP address(es) in '%s' networking mode.\n", DRIVER_NAME());
    } else {
        for (i = 0; i < pGni->max_public_ips; i++) {
            for (j = 0; j < max_nets; j++) {
                if (ips[j] == pGni->public_ips[i]) {
                    // this global public IP is assigned to the public interface
                    strptra = hex2dot(pGni->public_ips[i]);
                    snprintf(cmd, EUCA_MAX_PATH, "%s/32", strptra);
                    EUCA_FREE(strptra);
                    euca_execlp_redirect(&rc, NULL, "/dev/null", FALSE, "/dev/null", FALSE, pConfig->cmdprefix, "ip", "addr", "del", cmd,  "dev", pConfig->pubInterface, NULL);
                    rc = rc >> 8;
                    if(!(rc == 0 || rc == 2)){
                        LOGERROR("Failed to run ip addr del %s/32 dev %s", strptra, pConfig->pubInterface);
                        ret = 1;
                    }
                }
            }
        }
        EUCA_FREE(ips);
        EUCA_FREE(nms);
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

    if ((edgeMaintCount % 10) == 0) {
        if (pGni == edgeConfig_a->gni) {
            edgeConfig_a->config = pConfig;
            do_edge_update_netmeter(edgeConfig_a);
        }
        if (pGni == edgeConfig_b->gni) {
            edgeConfig_b->config = pConfig;
            do_edge_update_netmeter(edgeConfig_b);
        }
    }
    edgeMaintCount++;
    return (rc);
}

/**
 * This API checks the new GNI against the system view to decide what really
 * needs to be done.
 * EDGE system scrub. Detect instances and security groups relevant to local NC.
 * Then, process security groups, elastic/public IPs, and DHCP.
 * @param pConfig [in] a pointer to eucanetd system-wide configuration
 * @param pGni [in] a pointer to the Global Network Information structure
 * @param pGniApplied [in] a pointer to the previously successfully implemented GNI
 * @return A bitmask indicating what needs to be done. The following bits are
 * the ones to look for: EUCANETD_RUN_NO_API (GNI successfully applied), or
 * EUCANETD_RUN_ERROR_API (failed to apply GNI).
 */
static u32 network_driver_system_scrub(eucanetdConfig *pConfig, globalNetworkInfo *pGni, globalNetworkInfo *pGniApplied) {
    int rc = 0;
    u32 ret = EUCANETD_RUN_NO_API;

    struct timeval tv;

    eucanetd_timer(&tv);
    LOGTRACE("Scrubbing for '%s' network driver.\n", DRIVER_NAME());

    // this only applies to NC components
    if (!PEER_IS_NC(eucanetdPeer)) {
        // no-op
        return (EUCANETD_RUN_NO_API);
    }
    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Unable to execute system scrub. Driver not initialized.\n");
        return (EUCANETD_RUN_ERROR_API);
    }
    // Are the global and local network view structures NULL?
    if (!pGni || !pConfig) {
        LOGERROR("Unable to execute system scrub. Invalid parameters provided.\n");
        return (EUCANETD_RUN_ERROR_API);
    }

    if (edgeConfig == edgeConfig_a) {
        edgeConfig = edgeConfig_b;
        edgeConfigApplied = edgeConfig_a;
    } else {
        edgeConfig = edgeConfig_a;
        edgeConfigApplied = edgeConfig_b;
    }
    free_edge_config(edgeConfig);
    free_edge_config(edgeConfigApplied);
    
    edgeConfig->gni = pGni;
    edgeConfig->config = pConfig;
    
    rc = extract_edge_config_from_gni(edgeConfig);
    if (rc) {
        LOGDEBUG("failed to populate edgeConfig\n");
        return (EUCANETD_RUN_ERROR_API);
    }

    boolean do_edge_update = TRUE;
    int do_instances = 1;
    int do_sgs = 1;
    int do_allprivate = 1;
    if (pGniApplied && (pGni != pGniApplied)) {
        edgeConfigApplied->gni = pGniApplied;
        edgeConfigApplied->config = pConfig;

        rc = extract_edge_config_from_gni(edgeConfigApplied);
        if (!rc) {
            if (!cmp_edge_config(edgeConfig, edgeConfigApplied, &do_instances,
                    &do_sgs, &do_allprivate)) {
                do_edge_update = FALSE;
                LOGINFO("\tSystem is already up-to-date\n");
            }
        }
    }

    if (do_edge_update) {
        if (do_allprivate) {
            rc += do_edge_update_allprivate(edgeConfig);
        }
        if (do_sgs) {
            rc += do_edge_update_sgs(edgeConfig);
        }
        if (do_instances) {
            rc += do_edge_update_eips(edgeConfig);
            rc += do_edge_update_l2(edgeConfig);
            rc += do_edge_update_ips(edgeConfig);
        }
    }
    rc += do_edge_update_netmeter(edgeConfig);

    if (rc) {
        ret = EUCANETD_RUN_ERROR_API;
    }
    return (ret);
}

/**
 * Updates the list of IP addresses in EUCA_ALLPRIVATE ipset.
 * @param edge [in] pointer to EDGE configuration structure
 * @return 0 on success. Positive integer on any error during processing.
 */
int do_edge_update_allprivate(edge_config *edge) {
    int rc = 0;
    int slashnet = 0;
    char *strptra = NULL;
    char *vmgwip = NULL;
    struct timeval tv = { 0 };

    eucanetd_timer(&tv);
    LOGTRACE("Updating EUCA_ALLPRIVATE ipset.\n");

    // Is EDGE configuration NULL?
    if (!edge || !edge->config || !edge->gni) {
        LOGERROR("Invalid argument: cannot update core ipset with NULL configuration.\n");
        return (1);
    }

    // pull in latest IPS state
    rc |= ips_handler_repopulate(edge->config->ips);

    if (rc) {
        LOGERROR("Failed to load ipset state\n");
        return (1);
    }

    u32 euca_version = euca_version_dot2hex(EUCA_VERSION);
    strptra = hex2dot(euca_version);
    ips_handler_add_set(edge->config->ips, "EUCA_VERSION");
    ips_set_flush(edge->config->ips, "EUCA_VERSION");
    ips_set_add_ip(edge->config->ips, "EUCA_VERSION", strptra);
    EUCA_FREE(strptra);

    // reset and create ipset for allprivate
    ips_handler_add_set(edge->config->ips, "EUCA_ALLPRIVATE");
    ips_set_flush(edge->config->ips, "EUCA_ALLPRIVATE");

    // Populate ipset with all private IPs
    for (int i = 0; i < edge->gni->max_instances; i++) {
        gni_instance *inst = edge->gni->instances[i];
        if (inst->privateIp) {
            strptra = hex2dot(inst->privateIp);
            ips_set_add_ip(edge->config->ips, "EUCA_ALLPRIVATE", strptra);
            EUCA_FREE(strptra);
        }
    }
    // add additional private non-euca subnets to EUCA_ALLPRIVATE
    for (int i = 0; i < edge->gni->max_subnets; i++) {
        strptra = hex2dot(edge->gni->subnets[i].subnet);
        slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - edge->gni->subnets[i].netmask) + 1))));
        ips_set_add_net(edge->config->ips, "EUCA_ALLPRIVATE", strptra, slashnet);
        EUCA_FREE(strptra);
    }

    // VM gateway IP
    vmgwip = hex2dot(edge->config->vmGatewayIP);
    ips_set_add_ip(edge->config->ips, "EUCA_ALLPRIVATE", vmgwip);
    EUCA_FREE(vmgwip);

    // Deploy our IP sets
    rc = ips_handler_deploy(edge->config->ips, 0);
    if (rc) {
        LOGERROR("could not apply ipsets: check above log errors for details\n");
        return (1);
    }
    LOGINFO("\tcore ipsets processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (0);
}

/**
 * Implements security-group artifacts. This will add or remove remove networking
 * rules pertaining to the groups and their membership. Entails installing the
 * iptables rules for the groups and the ipset for the sg members.
 * @param edge [in] pointer to EDGE configuration structure
 * @return 0 on success. Positive integer on any error during processing.
 */
int do_edge_update_sgs(edge_config *edge) {
#define MAX_RULE_LEN              1024

    int i = 0;
    int j = 0;
    int k = 0;
    int rc = 0;
    int ret = 0;
    int ipcount = 0;
    char *strptra = NULL;
    char *vmgwip = NULL;
    char *chainname = NULL;
    char *refchainname = NULL;
    char rule[MAX_RULE_LEN] = "";
    gni_instance *instances;
    int max_instances = 0;
    gni_secgroup *secgroup = NULL;
    gni_secgroup *refsecgroup = NULL;
    u32 cidrnm = 0xffffffff;
    struct timeval tv = { 0 };

    eucanetd_timer(&tv);
    LOGTRACE("Implementing security-group artifacts.\n");

    // Is EDGE configuration NULL?
    if (!edge || !edge->config || !edge->gni) {
        LOGERROR("Invalid argument: cannot update SGs from NULL configuration.\n");
        return (1);
    }

    // pull in latest IPT state
    rc |= ipt_handler_repopulate(edge->config->ipt);
    // pull in latest IPS state
    rc |= ips_handler_repopulate(edge->config->ips);

    if (rc) {
        LOGERROR("Failed to load iptables and/or ipset state\n");
        return (1);
    }

    // make sure euca chains are in place
    ipt_table_add_chain(edge->config->ipt, "filter", "EUCA_FILTER_FWD_PREUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "filter", "EUCA_FILTER_FWD", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "filter", "EUCA_FILTER_FWD_DROPPED", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "filter", "EUCA_FILTER_FWD_POSTUSERHOOK", "-", "[0:0]");
    ipt_chain_add_rule(edge->config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD_PREUSERHOOK");
    ipt_chain_add_rule(edge->config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD");
    ipt_chain_add_rule(edge->config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD_POSTUSERHOOK");

    // clear all chains that we're about to (re)populate with latest network view
    ipt_chain_flush(edge->config->ipt, "filter", "EUCA_FILTER_FWD");
    ipt_chain_flush(edge->config->ipt, "filter", "EUCA_FILTER_FWD_DROPPED");
    ipt_table_deletechainmatch(edge->config->ipt, "filter", "sg-");
    ipt_chain_add_rule(edge->config->ipt, "filter", "EUCA_FILTER_FWD",
            "-A EUCA_FILTER_FWD -m conntrack --ctstate ESTABLISHED -j ACCEPT");

    // reset and create ipsets for allprivate, ncprivate and noneuca subnet sets
    ips_handler_deletesetmatch(edge->config->ips, "sg-");

    ips_handler_add_set(edge->config->ips, "EUCA_NCPRIVATE");
    ips_set_flush(edge->config->ips, "EUCA_NCPRIVATE");
    
    // Populate ipset with local private IPs
    ipcount = 0;
    for (i = 0; i < edge->max_my_instances; i++) {
        gni_instance *inst = &(edge->my_instances[i]);
        if (inst->privateIp) {
            strptra = hex2dot(inst->privateIp);
            ips_set_add_ip(edge->config->ips, "EUCA_NCPRIVATE", strptra);
            ipcount++;
            EUCA_FREE(strptra);
        }
    }
    if (!ipcount) {
        ips_set_add_net(edge->config->ips, "EUCA_NCPRIVATE", "127.0.0.1", 32);
    }

    // Forward packets generated by instances hosted by this NC and not destined
    // to instances hosted by this NC (this should go out of this NC). Packets
    // destined to instances hosted by this NC are subject to SG chains
    snprintf(rule, MAX_RULE_LEN, "-A EUCA_FILTER_FWD -m physdev --physdev-in vn_i+ " 
            "-m set ! --match-set EUCA_NCPRIVATE dst -j ACCEPT");
    ipt_chain_add_rule(edge->config->ipt, "filter", "EUCA_FILTER_FWD", rule);
    
    // add referenced SG ipsets
    for (i = 0; i < edge->max_ref_sgs; i++) {
        secgroup = edge->ref_sgs[i];
        chainname = strdup(secgroup->name);

        ips_handler_add_set(edge->config->ips, chainname);
        ips_set_flush(edge->config->ips, chainname);
        ips_set_add_ip(edge->config->ips, chainname, vmgwip);

        max_instances = 0;
        gni_secgroup_get_instances(edge->gni, secgroup, NULL, 0, NULL, 0, &instances, &max_instances);

        for (j = 0; j < max_instances; j++) {
            if (instances[j].privateIp) {
                strptra = hex2dot(instances[j].privateIp);
                ips_set_add_ip(edge->config->ips, chainname, strptra);
                EUCA_FREE(strptra);
            }
            if (instances[j].publicIp) {
                strptra = hex2dot(instances[j].publicIp);
                ips_set_add_ip(edge->config->ips, chainname, strptra);
                EUCA_FREE(strptra);
            }
        }

        EUCA_FREE(instances);
        EUCA_FREE(chainname);
    }

    // add SGs of VMs hosted by this NC
    for (i = 0; i < edge->max_my_sgs; i++) {
        secgroup = edge->my_sgs[i];
        chainname = strdup(secgroup->name);
        rule[0] = '\0';

        ips_handler_add_set(edge->config->ips, chainname);
        ips_set_flush(edge->config->ips, chainname);
        ips_set_add_ip(edge->config->ips, chainname, vmgwip);

        max_instances = 0;
        gni_secgroup_get_instances(edge->gni, secgroup, NULL, 0, NULL, 0, &instances, &max_instances);

        for (j = 0; j < max_instances; j++) {
            if (instances[j].privateIp) {
                strptra = hex2dot(instances[j].privateIp);
                ips_set_add_ip(edge->config->ips, chainname, strptra);
                EUCA_FREE(strptra);
            }
            if (instances[j].publicIp) {
                strptra = hex2dot(instances[j].publicIp);
                ips_set_add_ip(edge->config->ips, chainname, strptra);
                EUCA_FREE(strptra);
            }
        }

        // add forward chain
        ipt_table_add_chain(edge->config->ipt, "filter", chainname, "-", "[0:0]");
        ipt_chain_flush(edge->config->ipt, "filter", chainname);

        // add jump rule
        snprintf(rule, MAX_RULE_LEN, "-A EUCA_FILTER_FWD -m set --match-set %s dst -j %s", chainname, chainname);
        ipt_chain_add_rule(edge->config->ipt, "filter", "EUCA_FILTER_FWD", rule);

        // populate forward chain

        // this one needs to be first
        snprintf(rule, MAX_RULE_LEN, "-A %s -m set --match-set %s src -j ACCEPT", chainname, chainname);
        ipt_chain_add_rule(edge->config->ipt, "filter", chainname, rule);

        // then put all the group specific IPT rules (temporary one here)
        if (secgroup->max_ingress_rules) {
            for (j = 0; j < secgroup->max_ingress_rules; j++) {
                // If this rule is in reference to another group, lets add this IP set here
                if (strlen(secgroup->ingress_rules[j].groupId) != 0) {
                    refsecgroup = gni_get_secgroup(edge->gni, secgroup->ingress_rules[j].groupId, NULL);
                    if (refsecgroup == NULL) {
                        LOGWARN("Could not find referenced security group %s. Skipping ingress rule.\n", secgroup->ingress_rules[j].groupId);
                    } else {
                        LOGDEBUG("Found referenced security group %s owner %s\n", refsecgroup->name, refsecgroup->accountId);
                        refchainname = NULL;
                        refchainname = strdup(refsecgroup->name);
                        ingress_gni_to_iptables_rule(NULL, &(secgroup->ingress_rules[j]), rule, 0);
                        strptra = strdup(rule);
                        snprintf(rule, MAX_RULE_LEN, "-A %s -m set --match-set %s src %s -j ACCEPT", chainname, refchainname, strptra);
                        ipt_chain_add_rule(edge->config->ipt, "filter", chainname, rule);
                        EUCA_FREE(strptra);
                        EUCA_FREE(refchainname);
                    }
                } else {
                    ingress_gni_to_iptables_rule(NULL, &(secgroup->ingress_rules[j]), rule, 0);
                    strptra = strdup(rule);
                    snprintf(rule, MAX_RULE_LEN, "-A %s %s -j ACCEPT", chainname, strptra);
                    ipt_chain_add_rule(edge->config->ipt, "filter", chainname, rule);
                    EUCA_FREE(strptra);

                    // Check if this rule refers to a public IP that this NC is responsible for
                    if (strlen(secgroup->ingress_rules[j].cidr)) {
                        // Ignoring potential shift by 32 on a u32. If cidrsn is 0, the rule will not be processed (it allows all, so the rule above suffices)
                        cidrnm = (u32) 0xffffffff << (32 - secgroup->ingress_rules[j].cidrSlashnet);
                        if (secgroup->ingress_rules[j].cidrSlashnet != 0) {
                            // Search for public IPs that this NC is responsible
                            for (k = 0; k < edge->max_my_instances; k++) {
                                if (((edge->my_instances[k].publicIp & cidrnm) == (secgroup->ingress_rules[j].cidrNetaddr & cidrnm)) &&
                                        ((edge->my_instances[k].privateIp & cidrnm) != (secgroup->ingress_rules[j].cidrNetaddr & cidrnm))) {
                                    strptra = hex2dot(edge->my_instances[k].privateIp);
                                    LOGDEBUG("Found instance private IP (%s) local to this NC affected by another rule.\n", strptra);
                                    ingress_gni_to_iptables_rule(strptra, &(secgroup->ingress_rules[j]), rule, 1);
                                    LOGDEBUG("Created new iptables rule: %s\n", rule);
                                    EUCA_FREE(strptra);
                                    strptra = strdup(rule);
                                    snprintf(rule, MAX_RULE_LEN, "-A %s %s -j ACCEPT", chainname, strptra);
                                    ipt_chain_add_rule(edge->config->ipt, "filter", chainname, rule);
                                    EUCA_FREE(strptra);
                                }
                            }
                        }
                    }
                }
            }
        }

        EUCA_FREE(instances);
        EUCA_FREE(chainname);
    }
    EUCA_FREE(vmgwip);

    // counter rules for dropped packets
    ipt_chain_add_rule(edge->config->ipt, "filter", "EUCA_FILTER_FWD", "-A EUCA_FILTER_FWD -j EUCA_FILTER_FWD_DROPPED");
    // Place rules to count dropped packets
    for (i = 0; i < edge->max_my_instances; i++) {
        strptra = hex2dot(edge->my_instances[i].privateIp);
        // dropped private traffic
        snprintf(rule, MAX_RULE_LEN, "-A EUCA_FILTER_FWD_DROPPED -d %s/32 -m set --match-set EUCA_ALLPRIVATE src", strptra);
        ipt_chain_add_rule(edge->config->ipt, "filter", "EUCA_FILTER_FWD_DROPPED", rule);
        // dropped public traffic
        snprintf(rule, MAX_RULE_LEN, "-A EUCA_FILTER_FWD_DROPPED -d %s/32 -m set ! --match-set EUCA_ALLPRIVATE src", strptra);
        ipt_chain_add_rule(edge->config->ipt, "filter", "EUCA_FILTER_FWD_DROPPED", rule);
        EUCA_FREE(strptra);
    }

    // DROP if no accepts have made it past the FWD chains, and the dst IP is in the ALLPRIVATE ipset
    snprintf(rule, MAX_RULE_LEN, "-A EUCA_FILTER_FWD -m set --match-set EUCA_ALLPRIVATE dst -j DROP");
    ipt_chain_add_rule(edge->config->ipt, "filter", "EUCA_FILTER_FWD", rule);

    // Deploy our IP sets
    if (1 || !ret) {
        ips_handler_print(edge->config->ips);
        rc = ips_handler_deploy(edge->config->ips, 0);
        if (rc) {
            LOGERROR("could not apply ipsets: check above log errors for details\n");
            ret = 1;
        }
    }
    // Deploy our IP Table rules
    if (1 || !ret) {
        ipt_handler_print(edge->config->ipt);
        rc = ipt_handler_deploy(edge->config->ipt);
        if (rc) {
            LOGERROR("could not apply new rules: check above log errors for details\n");
            ret = 1;
        }
    }

    if (1 || !ret) {
        ips_handler_print(edge->config->ips);
        rc = ips_handler_deploy(edge->config->ips, 1);
        if (rc) {
            LOGERROR("could not apply ipsets: check above log errors for details\n");
            ret = 1;
        }
    }

    if (!ret) {
        LOGINFO("\tsgs processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    }
    return (ret);
#undef MAX_RULE_LEN
}


/**
 * Update the elastic IP artifacts. This will install the NAT rules for each one
 * of them.
 * @param edge [in] pointer to EDGE configuration structure
 * @return 0 on success. Positive integer on any error during processing.
 */
int do_edge_update_eips(edge_config *edge) {
#define MAX_RULE_LEN               1024

    int slashnet = 0;
    int ret = 0;
    int rc = 0;
    int i = 0;
    int j = 0;
    int found = 0;
    u32 nw = 0;
    u32 nm = 0;
    char cmd[EUCA_MAX_PATH] = "";
    char rule[MAX_RULE_LEN] = "";
    char *strptra = NULL;
    char *strptrb = NULL;
    struct timeval tv = { 0 };

    eucanetd_timer_usec(&tv);
    LOGDEBUG("Updating public IP to private IP mappings.\n");

    if (!edge || !edge->config || !edge->gni || !edge->my_cluster) {
        LOGERROR("Invalid argument: cannot update EIPs from NULL configuration.\n");
        return (1);
    }

    nw = edge->my_cluster->private_subnet.subnet;
    nm = edge->my_cluster->private_subnet.netmask;

    slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - nm) + 1))));

    // install EL IP addrs and NAT rules
    rc = ipt_handler_repopulate(edge->config->ipt);
    if (rc) {
        LOGERROR("unable to reinitialize iptables handler\n");
        return (1);
    }

    ipt_handler_add_table(edge->config->ipt, "raw");
    ipt_table_add_chain(edge->config->ipt, "raw", "EUCA_RAW_PRE", "-", "[0:0]");
    ipt_chain_add_rule(edge->config->ipt, "raw", "PREROUTING", "-A PREROUTING -j EUCA_RAW_PRE");

    ipt_table_add_chain(edge->config->ipt, "nat", "EUCA_NAT_PRE_PREUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "nat", "EUCA_NAT_PRE", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "nat", "EUCA_NAT_PRE_POSTUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "nat", "EUCA_NAT_POST_PREUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "nat", "EUCA_NAT_POST", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "nat", "EUCA_NAT_POST_POSTUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "nat", "EUCA_NAT_OUT_PREUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "nat", "EUCA_NAT_OUT", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "nat", "EUCA_NAT_OUT_POSTUSERHOOK", "-", "[0:0]");

    ipt_chain_add_rule(edge->config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE_PREUSERHOOK");
    ipt_chain_add_rule(edge->config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE");
    ipt_chain_add_rule(edge->config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE_POSTUSERHOOK");
    ipt_chain_add_rule(edge->config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST_PREUSERHOOK");
    ipt_chain_add_rule(edge->config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST");
    ipt_chain_add_rule(edge->config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST_POSTUSERHOOK");
    ipt_chain_add_rule(edge->config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT_PREUSERHOOK");
    ipt_chain_add_rule(edge->config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT");
    ipt_chain_add_rule(edge->config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT_POSTUSERHOOK");

    //ipt_handler_print(edge->config->ipt);
    rc = ipt_handler_deploy(edge->config->ipt);
    if (rc) {
        LOGERROR("could not deploy iptables rules: check above log errors for details\n");
        ret = 1;
    }

    rc = ipt_handler_repopulate(edge->config->ipt);

    ipt_table_add_chain(edge->config->ipt, "raw", "EUCA_COUNTERS_IN", "-", "[0:0]");
    ipt_table_add_chain(edge->config->ipt, "raw", "EUCA_COUNTERS_OUT", "-", "[0:0]");
    ipt_chain_flush(edge->config->ipt, "nat", "EUCA_NAT_PRE");
    ipt_chain_flush(edge->config->ipt, "nat", "EUCA_NAT_POST");
    ipt_chain_flush(edge->config->ipt, "nat", "EUCA_NAT_OUT");
    ipt_chain_flush(edge->config->ipt, "raw", "EUCA_COUNTERS_IN");
    ipt_chain_flush(edge->config->ipt, "raw", "EUCA_COUNTERS_OUT");
    ipt_chain_flush(edge->config->ipt, "raw", "EUCA_RAW_PRE");

    strptra = hex2dot(nw);

    if (edge->config->metadata_use_vm_private) {
        // set a mark so that VM to metadata service requests are not SNATed
        snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_PRE -s %s/%d -d 169.254.169.254/32 -j MARK --set-xmark 0x2a/0xffffffff", strptra, slashnet);
        ipt_chain_add_rule(edge->config->ipt, "nat", "EUCA_NAT_PRE", rule);
    }

    snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_PRE -s %s/%d -m set --match-set EUCA_ALLPRIVATE dst -j MARK --set-xmark 0x2a/0xffffffff", strptra, slashnet);
    ipt_chain_add_rule(edge->config->ipt, "nat", "EUCA_NAT_PRE", rule);

    ipt_chain_add_rule(edge->config->ipt, "raw", "EUCA_RAW_PRE", "-A EUCA_RAW_PRE -j EUCA_COUNTERS_IN");
    ipt_chain_add_rule(edge->config->ipt, "raw", "EUCA_RAW_PRE", "-A EUCA_RAW_PRE -j EUCA_COUNTERS_OUT");

    snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/%d", strptra, slashnet);
    ipt_chain_add_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_IN", rule);

    snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/%d", strptra, slashnet);
    ipt_chain_add_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_OUT", rule);

    EUCA_FREE(strptra);

    int max_instances = edge->max_my_instances;
    gni_instance *instances = edge->my_instances;
    for (i = 0; i < max_instances; i++) {
        strptra = hex2dot(instances[i].publicIp);
        strptrb = hex2dot(instances[i].privateIp);
        LOGTRACE("instance pub/priv: %s: %s/%s\n", instances[i].name, strptra, strptrb);
        if ((instances[i].publicIp && instances[i].privateIp) && (instances[i].publicIp != instances[i].privateIp)) {
            // run some commands
            snprintf(cmd, EUCA_MAX_PATH, "%s/32", strptra);
            euca_execlp_redirect(&rc, NULL, "/dev/null", FALSE, "/dev/null", FALSE, edge->config->cmdprefix, "ip", "addr", "add", cmd, "dev", edge->config->pubInterface, NULL);
            rc = rc >> 8;
            if (!(rc == 0 || rc == 2)) {
                LOGERROR("could not execute: adding ips\n");
                ret = 1;
            } else {
                // try arping up to 3 times
                rc = EUCA_TIMEOUT_ERROR;
                for (j = 1; j < 4 && rc != EUCA_OK; j++) {
                    rc = euca_exec_wait(j, edge->config->cmdprefix, "arping", "-c", "1", "-U", "-I", edge->config->pubInterface, strptra, NULL);
                }
            }

            snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_PRE -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
            rc = ipt_chain_add_rule(edge->config->ipt, "nat", "EUCA_NAT_PRE", rule);

            snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_OUT -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
            rc = ipt_chain_add_rule(edge->config->ipt, "nat", "EUCA_NAT_OUT", rule);

            snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_POST -s %s/32 -m mark ! --mark 0x2a/0x2a -j SNAT --to-source %s", strptrb, strptra);
            rc = ipt_chain_add_rule(edge->config->ipt, "nat", "EUCA_NAT_POST", rule);

            // public in counter (will potentially count dropped packets)
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/32", strptra);
            rc = ipt_chain_add_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_IN", rule);

            // private in counter (will potentially count dropped packets)
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/32", strptrb);
            rc = ipt_chain_add_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_IN", rule);

            // public out counter
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/32 -m set ! --match-set EUCA_ALLPRIVATE dst", strptrb);
            rc = ipt_chain_add_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_OUT", rule);
            
            // private out counter            
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/32 -m set --match-set EUCA_ALLPRIVATE dst", strptrb);
            rc = ipt_chain_add_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_OUT", rule);

        }

        EUCA_FREE(strptra);
        EUCA_FREE(strptrb);
    }

    // Install the masquerade rules
    if (edge->config->nc_proxy) {
        strptra = hex2dot(edge->my_cluster->private_subnet.subnet);
        slashnet = NETMASK_TO_SLASHNET(edge->my_cluster->private_subnet.netmask);

        snprintf(rule, 1024, "-A EUCA_NAT_POST -s %s/%u -m mark ! --mark 0x2a -j MASQUERADE", strptra, slashnet);
        ipt_chain_add_rule(edge->config->ipt, "nat", "EUCA_NAT_POST", rule);
        EUCA_FREE(strptra);
    }

    // lastly, install metadata redirect rule
    if (edge->config->metadata_ip) {
        strptra = hex2dot(edge->config->clcMetadataIP);
    } else {
        strptra = hex2dot(edge->gni->enabledCLCIp);
    }
    snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_PRE -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", strptra);
    rc = ipt_chain_add_rule(edge->config->ipt, "nat", "EUCA_NAT_PRE", rule);
    EUCA_FREE(strptra);

    //  rc = ipt_handler_print(edge->config->ipt);
    rc = ipt_handler_deploy(edge->config->ipt);
    if (rc) {
        LOGERROR("could not apply new ipt handler rules: check above log errors for details\n");
        ret = 1;
    }

    // if all has gone well, now clear any public IPs that have not been mapped to private IPs
    if (!ret) {
        u32 *ips=NULL, *nms=NULL;
        int max_nets;
        
        if (getdevinfo(edge->config->pubInterface, &ips, &nms, &max_nets)) {
            // could not get interface info - only check below against instances
            max_nets = 0;
            ips = NULL;
            nms = NULL;
        }

        for (i = 0; i < edge->gni->max_public_ips; i++) {
            found = 0;
            // only clear IPs that are not assigned to instances running on this node
            if (!found && max_instances > 0) {
                for (j = 0; j < max_instances && !found; j++) {
                    if (instances[j].publicIp == edge->gni->public_ips[i]) {
                        // this global public IP is assigned to an instance on this node, do not delete
                        found = 1;
                    }
                }
            }

            // only clear IPs that are assigned on the public interface already
            if (!found && max_nets > 0) {
                found = 1;
                for (j = 0; j < max_nets && found; j++) {
                    if (ips[j] == edge->gni->public_ips[i]) {
                        // this global public IP is assigned to the public interface currently (but not to an instance) - do the delete
                        found = 0;
                    }
                }
            }

            if (!found) {
                strptra = hex2dot(edge->gni->public_ips[i]);
                snprintf(cmd, EUCA_MAX_PATH, "%s/32", strptra);
                EUCA_FREE(strptra);
                if (euca_execlp_redirect(NULL, NULL, "/dev/null", FALSE, "/dev/null", FALSE, edge->config->cmdprefix,
                                         "ip", "addr", "del", cmd, "dev", edge->config->pubInterface, NULL) != EUCA_OK) {
                    LOGERROR("could not execute: revoking no longer in use ips\n");
                    ret = 1;
                }
            }
        }
        EUCA_FREE(ips);
        EUCA_FREE(nms);
    }
    LOGINFO("\tpublic/elastic IPs processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (ret);

#undef MAX_RULE_LEN
}

/**
 * Application of EBT rules to only allow unique and known IP<->MAC pairings to
 * send traffic through the bridge
 * @param edge [in] pointer to EDGE configuration structure
 * @return 0 on success. Positive integer on any error during processing.
 */
int do_edge_update_l2(edge_config *edge) {
    int i = 0;
    int rc = 0;
    int ret = 0;
    int max_instances = 0;
    char cmd[EUCA_MAX_PATH] = "";
    char *strptra = NULL;
    char *strptrb = NULL;
    char vnetinterface[64];
    char *gwip = NULL;
    char *brmac = NULL;
    gni_instance *instances = NULL;
    struct timeval tv = { 0 };

    eucanetd_timer_usec(&tv);
    LOGDEBUG("Updating IP/MAC pairing rules.\n");

    // Make sure our given parameter is valid
    if (!edge || !edge->config || !edge->gni || !edge->my_cluster) {
        LOGERROR("Invalid argument: cannot update EIPs from NULL configuration.\n");
        return (1);
    }

    if (edge->config->nc_proxy) {
        // Now update our host information by sending Gratuitous ARP as necessary
        update_host_arp(edge);
    }

    ebt_handler_repopulate(edge->config->ebt);

    ebt_table_add_chain(edge->config->ebt, "filter", "EUCA_EBT_FWD", "ACCEPT", "");
    ebt_chain_add_rule(edge->config->ebt, "filter", "FORWARD", "-j EUCA_EBT_FWD");
    ebt_chain_flush(edge->config->ebt, "filter", "EUCA_EBT_FWD");

    ebt_table_add_chain(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", "ACCEPT", "");
    ebt_chain_add_rule(edge->config->ebt, "nat", "PREROUTING", "-j EUCA_EBT_NAT_PRE");
    ebt_chain_flush(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE");

    ebt_table_add_chain(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", "ACCEPT", "");
    ebt_chain_add_rule(edge->config->ebt, "nat", "POSTROUTING", "-j EUCA_EBT_NAT_POST");
    ebt_chain_flush(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST");

    // add these for DHCP to pass
    //    ebt_chain_add_rule(edge->config->ebt, "filter", "EUCA_EBT_FWD", "-p IPv4 -d Broadcast --ip-proto udp --ip-dport 67:68 -j ACCEPT");
    //    ebt_chain_add_rule(edge->config->ebt, "filter", "EUCA_EBT_FWD", "-p IPv4 -d Broadcast --ip-proto udp --ip-sport 67:68 -j ACCEPT");

    gwip = hex2dot(edge->config->vmGatewayIP);
    brmac = INTFC2MAC(edge->config->bridgeDev);

    if (gwip && brmac) {
        // Add this one for DHCP to pass since windows may be requesting broadcast responses
        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -o vn_i+ -s %s -d Broadcast --ip-proto udp --ip-dport 67:68 -j ACCEPT", brmac);
        rc = ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

        instances = edge->my_instances;
        max_instances = edge->max_my_instances;

        if (!edge->config->nc_proxy) {
            // If we're using the "fake" router option and have some instance running,
            // we need to respond for out of network ARP request.
            if (edge->config->nc_router && !edge->config->nc_router_ip && (max_instances > 0)) {
                snprintf(cmd, EUCA_MAX_PATH, "-i vn_i+ -p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", gwip, brmac);
                rc = ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
            }
        } else {
            // Only allow ARP Reply to our bridge MAC
            snprintf(cmd, EUCA_MAX_PATH, "-p ARP -i vn_i+ -d ! %s --arp-op Reply -j DROP ", brmac);
            rc = ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

            // Ensures only our Bridge can send ARP request to our VMs
            snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o vn_i+ --arp-mac-src ! %s -j DROP", brmac);
            rc = ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);
        }

        for (i = 0; i < max_instances; i++) {
            if (instances[i].privateIp && maczero(instances[i].macAddress)) {
                strptra = strptrb = NULL;
                strptra = hex2dot(instances[i].privateIp);
                hex2mac(instances[i].macAddress, &strptrb);

                snprintf(vnetinterface, 63, "vn_%s", instances[i].name);

                if (strptra && strptrb) {
                    if (!edge->config->disable_l2_isolation) {
                        //NOTE: much of this ruleset is a translation of libvirt FW example at http://libvirt.org/firewall.html

                        // PRE Routing

                        // basic MAC check
                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -s ! %s -j DROP", vnetinterface, strptrb);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        // IPv4
                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -i %s -s %s --ip-proto udp --ip-dport 67:68 -j ACCEPT", vnetinterface, strptrb);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -i %s --ip-src ! %s -j DROP", vnetinterface, strptra);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -i %s -j ACCEPT", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        if (edge->config->nc_proxy) {
                            snprintf(cmd, EUCA_MAX_PATH, "-p ARP -i %s --arp-ip-dst %s -j DROP", vnetinterface, strptra);
                            ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                            snprintf(cmd, EUCA_MAX_PATH, "-p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", strptra, brmac);
                            ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                            // Forces all ARP from VM to get replied with our Bridge MAC (Force forward to the bridge)
                            snprintf(cmd, EUCA_MAX_PATH, "-p ARP -i %s -j arpreply --arpreply-mac %s", vnetinterface, brmac);
                            ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
                        }

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-mac-src ! %s -j DROP", vnetinterface, strptrb);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-ip-src ! %s -j DROP", vnetinterface, strptra);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-op Request -j ACCEPT", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-op Reply -j ACCEPT", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP -j DROP", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        // RARP
                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p 0x8035 -s %s -d Broadcast --arp-op Request_Reverse --arp-ip-src 0.0.0.0 --arp-ip-dst 0.0.0.0 --arp-mac-src %s "
                                 "--arp-mac-dst %s -j ACCEPT", vnetinterface, strptrb, strptrb, strptrb);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p 0x8035 -j DROP", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        // pass KVM migration weird packet
                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p 0x835 -j ACCEPT", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        // POST routing

                        // IPv4
                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -o %s -d ! %s --ip-proto udp --ip-dport 67:68 -j DROP", vnetinterface, strptrb);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -o %s -j ACCEPT", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        // ARP
                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Reply --arp-mac-dst ! %s -j DROP", vnetinterface, strptrb);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-ip-dst ! %s -j DROP", vnetinterface, strptra);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Request -j ACCEPT", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Request -j ACCEPT", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Reply -j ACCEPT", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s -j DROP", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        // RARP
                        snprintf(cmd, EUCA_MAX_PATH, "-p 0x8035 -o %s -d Broadcast --arp-op Request_Reverse --arp-ip-src 0.0.0.0 --arp-ip-dst 0.0.0.0 --arp-mac-src %s "
                                 "--arp-mac-dst %s -j ACCEPT", vnetinterface, strptrb, strptrb);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p 0x8035 -o %s -j DROP", vnetinterface);
                        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    } else {
                        if (edge->config->nc_router && !edge->config->nc_router_ip) {
                            snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", vnetinterface, gwip, brmac);
                            ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
                        }
                    }
                } else {
                    LOGWARN("could not retrieve one of: vmip (%s), vminterface (%s), vmmac (%s), gwip (%s), brmac (%s): skipping but will retry\n", SP(strptra), SP(vnetinterface),
                            SP(strptrb), SP(gwip), SP(brmac));
                    ret = 1;
                }
                EUCA_FREE(strptra);
                EUCA_FREE(strptrb);
            }
        }
    } else {
        LOGWARN("could not retrieve one of: gwip (%s), brmac (%s): skipping but will retry\n", SP(gwip), SP(brmac));
        ret = 1;
    }

    if (!edge->config->disable_l2_isolation) {
        // DROP everything from the instance by default
        snprintf(cmd, EUCA_MAX_PATH, "-i vn_i+ -j DROP");
        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
        
        // DROP everything to the instance by default
        snprintf(cmd, EUCA_MAX_PATH, "-o vn_i+ -j DROP");
        ebt_chain_add_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);
    }
    EUCA_FREE(brmac);
    EUCA_FREE(gwip);

    ebt_handler_print(edge->config->ebt);
    rc = ebt_handler_deploy(edge->config->ebt);
    if (rc) {
        LOGERROR("could not install ebtables rules: check above log errors for details\n");
        ret = 1;
    }

    LOGINFO("\tL2 addressing processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    return (ret);
}

/**
 * Update the private IP addressing. This will ensure a DHCP configuration file
 * is generated and the server restarted upon success.
 * @param edge [in] pointer to EDGE configuration structure
 * @return 0 on success. Positive integer on any error during processing.
 */
int do_edge_update_ips(edge_config *edge) {
    struct timeval tv = { 0 };

    eucanetd_timer_usec(&tv);
    LOGDEBUG("Updating private IP and DHCPD handling.\n");

    // Make sure our given parameter is valid
    if (!edge || !edge->config || !edge->gni) {
        LOGERROR("Invalid argument: cannot update ips from NULL configuration.\n");
        return (1);
    }

    if (edge->max_my_instances == 0) {
        LOGDEBUG("\tstopping dhcpd\n");
        eucanetd_stop_dhcpd_server(edge->config);
    } else {
        // Generate the DHCP configuration so instances can get their network config
        if ((generate_dhcpd_config(edge)) != 0) {
            LOGERROR("unable to generate new dhcp configuration file: check above log errors for details\n");
            return (1);
        }
        // Restart the DHCP server so it can pick up the new configuration
        if ((eucanetd_kick_dhcpd_server(edge->config)) != 0) {
            LOGERROR("unable to (re)configure local dhcpd server: check above log errors for details\n");
            return (1);
        }
    }
    LOGINFO("\tdhcp config processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (0);
}

/**
 * Update netmeter. Go through each instance's public and private IP iptables counter
 * rules and extract the updated counts.
 * @param edge [in] pointer to EDGE configuration structure
 * @return 0 on success. Positive integer on any error during processing.
 */
int do_edge_update_netmeter(edge_config *edge) {
#define MAX_RULE_LEN              1024
    int rc = 0;
    struct timeval tv = { 0 };

    eucanetd_timer_usec(&tv);
    LOGDEBUG("Updating netmeter.\n");

    // Make sure our given parameter is valid
    if (!edge || !edge->config || !edge->gni || !edge->nmeter) {
        LOGERROR("Invalid argument: cannot update netmeter from NULL configuration.\n");
        return (1);
    }

    clear_edge_netmeter_tag(edge->nmeter);
    rc = ipt_handler_repopulate(edge->config->ipt);
    if (rc) {
        LOGWARN("Unable to update netmeter data.\n");
        return (rc);
    }

    char *pubip = NULL;
    char *privip = NULL;
    gni_instance *vm = NULL;
    edge_netmeter_instance *vmnm = NULL;
    ipt_rule *iptrule = NULL;
    char rule[MAX_RULE_LEN];
    for (int i = 0; i < edge->max_my_instances; i++) {
        vm = &(edge->my_instances[i]);
        if (vm->privateIp) {
            privip = hex2dot(vm->privateIp);
        } else {
            continue;
        }
        if (vm->publicIp) {
            pubip = hex2dot(vm->publicIp);
            vmnm = find_edge_netmeter_instance(&(edge->nmeter->pub_ips), &(edge->nmeter->max_pub_ips),
                    vm->name, pubip, TRUE);
            vmnm->iptype = EDGE_IPV4_PUBLIC;
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/32", pubip);
            iptrule = ipt_chain_find_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_IN", rule);
            if (iptrule) {
                sscanf(iptrule->counterstr, "[%ld:%ld]", &(vmnm->pkts_in), &(vmnm->bytes_in));
                vmnm->updated = TRUE;
            }
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/32 -m set ! --match-set EUCA_ALLPRIVATE dst", privip);
            iptrule = ipt_chain_find_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_OUT", rule);
            if (iptrule) {
                sscanf(iptrule->counterstr, "[%ld:%ld]", &(vmnm->pkts_out), &(vmnm->bytes_out));
                vmnm->updated = TRUE;
            }
            
            // Subtract dropped packets
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_FILTER_FWD_DROPPED -d %s/32 -m set ! --match-set EUCA_ALLPRIVATE src", privip);
            iptrule = ipt_chain_find_rule(edge->config->ipt, "filter", "EUCA_FILTER_FWD_DROPPED", rule);
            if (iptrule) {
                long a = 0; long b = 0;
                sscanf(iptrule->counterstr, "[%ld:%ld]", &a, &b);
                vmnm->pkts_out = vmnm->pkts_out - a;
                vmnm->bytes_out = vmnm->bytes_out - b;
            }
        }
        
        {
            vmnm = find_edge_netmeter_instance(&(edge->nmeter->priv_ips), &(edge->nmeter->max_priv_ips),
                    vm->name, privip, TRUE);
            vmnm->iptype = EDGE_IPV4_PRIVATE;
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/32", privip);
            iptrule = ipt_chain_find_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_IN", rule);
            if (iptrule) {
                sscanf(iptrule->counterstr, "[%ld:%ld]", &(vmnm->pkts_in), &(vmnm->bytes_in));
                vmnm->updated = TRUE;
            }
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/32 -m set --match-set EUCA_ALLPRIVATE dst", privip);
            iptrule = ipt_chain_find_rule(edge->config->ipt, "raw", "EUCA_COUNTERS_OUT", rule);
            if (iptrule) {
                sscanf(iptrule->counterstr, "[%ld:%ld]", &(vmnm->pkts_out), &(vmnm->bytes_out));
                vmnm->updated = TRUE;
            }

            // Subtract dropped packets
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_FILTER_FWD_DROPPED -d %s/32 -m set --match-set EUCA_ALLPRIVATE src", privip);
            iptrule = ipt_chain_find_rule(edge->config->ipt, "filter", "EUCA_FILTER_FWD_DROPPED", rule);
            if (iptrule) {
                long a = 0; long b = 0;
                sscanf(iptrule->counterstr, "[%ld:%ld]", &a, &b);
                vmnm->pkts_out = vmnm->pkts_out - a;
                vmnm->bytes_out = vmnm->bytes_out - b;
            }
        }
        EUCA_FREE(pubip);
        EUCA_FREE(privip);
    }
    edge_dump_netmeter(edge);
    LOGDEBUG("\tnetmeter updated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (0);
#undef MAX_RULE_LEN
}

/**
 * Generates the DHCP server configuration so the instances can get their
 * networking configuration information.
 * @param edge [in] pointer to EDGE configuration structure
 * @return 0 on success. Positive integer on any error during processing.
 */
int generate_dhcpd_config(edge_config *edge) {
    int i = 0;
    int ret = 0;
    int max_instances = 0;
    u32 nw = 0;
    u32 nm = 0;
    char *mac = NULL;
    char *ip = NULL;
    char *network = NULL;
    char *netmask = NULL;
    char *broadcast = NULL;
    char *router = NULL;
    char *strptra = NULL;
    char dhcpd_config_path[EUCA_MAX_PATH] = "";
    FILE *OFH = NULL;
    gni_instance *instances = NULL;

    // Make sure our given parameter is valid
    if (!edge || !edge->config || !edge->gni || !edge->my_cluster) {
        LOGERROR("Invalid argument: cannot update dhcp from NULL configuration.\n");
        return (1);
    }

    nw = edge->my_cluster->private_subnet.subnet;
    nm = edge->my_cluster->private_subnet.netmask;
    
    instances = edge->my_instances;
    max_instances = edge->max_my_instances;

    // Open the DHCP configuration file
    snprintf(dhcpd_config_path, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.conf", edge->config->eucahome);
    OFH = fopen(dhcpd_config_path, "w");
    if (!OFH) {
        LOGERROR("cannot open dhcpd server config file for write '%s': check permissions\n", dhcpd_config_path);
        ret = 1;
    } else {
        fprintf(OFH, "# automatically generated config file for DHCP server\ndefault-lease-time 86400;\nmax-lease-time 86400;\nddns-update-style none;\n\n");
        fprintf(OFH, "shared-network euca {\n");

        network = hex2dot(nw);
        netmask = hex2dot(nm);
        broadcast = hex2dot(nw | ~nm);
        router = hex2dot(edge->config->vmGatewayIP);  // this is set by configuration

        fprintf(OFH, "subnet %s netmask %s {\n  option subnet-mask %s;\n  option broadcast-address %s;\n", network, netmask, netmask, broadcast);
        if (strlen(edge->gni->instanceDNSDomain)) {
            fprintf(OFH, "  option domain-name \"%s\";\n", edge->gni->instanceDNSDomain);
        }

        if (edge->gni->max_instanceDNSServers) {
            strptra = hex2dot(edge->gni->instanceDNSServers[0]);
            fprintf(OFH, "  option domain-name-servers %s", SP(strptra));
            EUCA_FREE(strptra);
            for (i = 1; i < edge->gni->max_instanceDNSServers; i++) {
                strptra = hex2dot(edge->gni->instanceDNSServers[i]);
                fprintf(OFH, ", %s", SP(strptra));
                EUCA_FREE(strptra);
            }
            fprintf(OFH, ";\n");
        } else {
            fprintf(OFH, "  option domain-name-servers 8.8.8.8;\n");
        }
        fprintf(OFH, "  option routers %s;\n}\n", router);

        EUCA_FREE(network);
        EUCA_FREE(netmask);
        EUCA_FREE(broadcast);
        EUCA_FREE(router);

        for (i = 0; i < max_instances; i++) {
            hex2mac(instances[i].macAddress, &mac);
            ip = hex2dot(instances[i].privateIp);
            fprintf(OFH, "\nhost node-%s {\n  hardware ethernet %s;\n  fixed-address %s;\n}\n", ip, mac, ip);
            EUCA_FREE(mac);
            EUCA_FREE(ip);
        }

        fprintf(OFH, "}\n");
        fclose(OFH);
    }

    return (ret);
}

/**
 * Go through the list of instances that we are managing on this node and
 * send a gratuitous ARP for the newly created instances
 * @param edge [in] pointer to EDGE configuration structure
 * @return 0 on success. 1 on failure.
 */
int update_host_arp(edge_config *edge) {
    int i = 0;
    int rc = 0;
    int ret = 0;
    u8 aHexOut[ENET_BUF_SIZE] = { 0 };
    int bridgeMacLen = 0;
    int max_instances = 0;
    char *psTrimMac = NULL;
    char *psBridgeMac = NULL;
    char *psPrivateIp = NULL;
    char sRule[EUCA_MAX_PATH] = "";
    char sCommand[EUCA_MAX_PATH] = "";
    gni_instance *instances = NULL;
    struct timeval tv = { 0 };

    eucanetd_timer(&tv);
    LOGDEBUG("updating ARP entries for peers\n");

    if (!edge || !edge->config || !edge->gni || !edge->my_instances) {
        return (0);
    }

    instances = edge->my_instances;
    max_instances = edge->max_my_instances;
    rc = ebt_handler_repopulate(edge->config->ebt);
    if ((psBridgeMac = INTFC2MAC(edge->config->bridgeDev)) != NULL) {
        if ((bridgeMacLen = strlen(psBridgeMac)) > 0) {
            // Convert the MAC to a trimmed down version for EB table comparison
            if (mac2hex(psBridgeMac, aHexOut)) {
                euca_hex2mac(aHexOut, &psTrimMac, TRUE);

                // Now send gratuitous ARP requests for each instance with a private IP
                for (i = 0; i < max_instances; i++) {
                    if (instances[i].privateIp && maczero(instances[i].macAddress)) {
                        psPrivateIp = hex2dot(instances[i].privateIp);
                        if (edge->config->nc_proxy) {
                            snprintf(sRule, EUCA_MAX_PATH, "-p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", psPrivateIp, psTrimMac);
                            if (ebt_chain_find_rule(edge->config->ebt, "nat", "EUCA_EBT_NAT_PRE", sRule) == NULL) {
                                LOGDEBUG("Sending gratuitous ARP for instance %s IP %s using MAC %s on %s\n", instances[i].name, psPrivateIp, psBridgeMac, edge->config->bridgeDev);
                                snprintf(sCommand, EUCA_MAX_PATH, "/usr/libexec/eucalyptus/announce-arp %s %s %s", edge->config->bridgeDev, psPrivateIp, psBridgeMac);
                                rc = euca_execlp(&rc, edge->config->cmdprefix, "/usr/libexec/eucalyptus/announce-arp", edge->config->bridgeDev, psPrivateIp, psBridgeMac, NULL);
                                if (rc) {
                                    LOGDEBUG("error executing announce-arp\n");
                                }
                            }
                        }
                        EUCA_FREE(psPrivateIp);
                    }
                }
                // Done with the MAC
                EUCA_FREE(psTrimMac);
            }
        }
        EUCA_FREE(psBridgeMac);
    }

    LOGINFO("\tGARP processed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (ret);
}

/**
 * Releases resources allocated for the given edge_config structure.
 * @param edge [in] edge_config structure of interest
 * @return 0 on success. 1 on any failure.
 */
int free_edge_config(edge_config *edge) {
    if (edge == NULL) {
        return (0);
    }
    if (edge->my_instances) {
        EUCA_FREE(edge->my_instances);
    }
    edge->max_my_instances = 0;
    if (edge->my_sgs) {
        EUCA_FREE(edge->my_sgs);
    }
    edge->max_my_sgs = 0;
    if (edge->ref_sgs) {
        EUCA_FREE(edge->ref_sgs);
    }
    edge->max_ref_sgs = 0;
    return (0);
}

/**
 * Releases resources allocated for the given edge_netmeter_instance structure.
 * @param nm [in] edge_netmeter_instance structure of interest
 * @return 0 on success. 1 on any failure.
 */
int free_edge_netmeter_instance(edge_netmeter_instance *nm) {
    if (nm == NULL) {
        return (0);
    }
    if (nm->instance_id) {
        EUCA_FREE(nm->instance_id);
    }
    if (nm->ipaddr) {
        EUCA_FREE(nm->ipaddr);
    }
    memset(nm, 0, sizeof(edge_netmeter_instance));
    return (0);
}

/**
 * Releases resources allocated for the given array of edge_netmeter_instance
 * structure pointers. Each entry in an array should be pointing to an independently
 * allocated edge_netmeter_instance structure memory.
 * @param nms [in] pointer to an array of edge_netmeter_instance structure pointers of interest
 * @param max_nms [in] number of entries in the array
 * @return 0 on success. 1 on any failure.
 */
int free_edge_netmeter_instances(edge_netmeter_instance **nms, int max_nms) {
    if (!nms || !max_nms) {
        return (0);
    }
    for (int i = 0; i < max_nms; i++) {
        free_edge_netmeter_instance(nms[i]);
        EUCA_FREE(nms[i]);
        nms[i] = NULL;
    }
    return (0);
}

/**
 * Releases resources allocated for the given edge_netmeter structure.
 * @param nm [in] edge_netmeter structure of interest
 * @return 0 on success. 1 on any failure.
 */
int free_edge_netmeter(edge_netmeter *nm) {
    if (nm == NULL) {
        return (0);
    }
    free_edge_netmeter_instances(nm->pub_ips, nm->max_pub_ips);
    free_edge_netmeter_instances(nm->priv_ips, nm->max_priv_ips);
    EUCA_FREE(nm->pub_ips);
    EUCA_FREE(nm->priv_ips);
    memset(nm, 0, sizeof(edge_netmeter));
    return (0);
}

/**
 * Extracts state relevant to this NC from gni and stores in edge. edge datastructure
 * is assumed to have the pointer to its GNI pre-populated.
 * @param edge [out] edge_config data structure to store the extracted information
 * @return 0 on success. 1 on any error.
 */
int extract_edge_config_from_gni(edge_config *edge) {
    int rc = 0;
    if (!edge || !edge->gni || !edge->config) {
        LOGWARN("Invalid argument: cannot extract gni information to/from NULL\n");
        return (1);
    }

    rc = gni_find_self_cluster(edge->gni, &(edge->my_cluster));
    if (rc) {
        LOGERROR("unable to find cluster in global network view: check network config\n");
        return (1);
    }

    rc = gni_find_self_node(edge->gni, &(edge->my_node));
    if (rc) {
        LOGERROR("unable to find node in global network view: check network config\n");
        return (1);
    }

    rc = gni_node_get_instances(edge->gni, edge->my_node, NULL, 0, NULL, 0,
            &(edge->my_instances), &(edge->max_my_instances));
    if (rc) {
        LOGWARN("unable to find instances hosted by this NC.\n");
        return (1);
    }
    
    rc = gni_get_secgroups_from_instances(edge->gni, edge->my_instances, edge->max_my_instances,
            &(edge->my_sgs), &(edge->max_my_sgs));
    if (rc) {
        LOGWARN("unable to find security groups of instances hosted by this NC.\n");
        return (1);
    }

    rc = gni_get_referenced_secgroups(edge->gni, edge->my_sgs, edge->max_my_sgs,
            &(edge->ref_sgs), &(edge->max_ref_sgs));
    if (rc) {
        LOGWARN("unable to find referenced security groups.\n");
        return (1);
    }
    return (0);
}

/**
 * Searches an array of edge_netmeter_instance pointers for the entry specified in the
 * argument.
 * @param nms [i/o] pointer to an array of edge_netmeter_instance pointers
 * @param max_nms [i/o] pointer to the number of entries in the array
 * @param instance_id [in] instance ID of interest
 * @param ipaddr [in] IP address of the instance of interest
 * @param force [in] create an entry if the instance in the argument is not found.
 * @return pointer to the edge_netmeter_instance structure of interest
 */
edge_netmeter_instance *find_edge_netmeter_instance(edge_netmeter_instance ***nms,
        int *max_nms, char *instance_id, char *ipaddr, boolean force) {
    if (!nms || !max_nms || !instance_id || !ipaddr) {
        return (NULL);
    }
    edge_netmeter_instance **nms_updated = *nms;
    edge_netmeter_instance *ret = NULL;
    boolean found = FALSE;
    for (int i = 0; i < *max_nms && !found; i++) {
        if (nms_updated[i]) {
            if (!strcmp(nms_updated[i]->instance_id, instance_id) &&
                    !strcmp(nms_updated[i]->ipaddr, ipaddr)) {
                ret = nms_updated[i];
                found = TRUE;
            }
        }
    }
    if (!found && force) {
        ret = EUCA_ZALLOC_C(1, sizeof(edge_netmeter_instance));
        ret->instance_id = strdup(instance_id);
        ret->ipaddr = strdup(ipaddr);
        nms_updated = EUCA_APPEND_PTRARR(nms_updated, max_nms, ret);
    }
    *nms = nms_updated;
    return (ret);
}

/**
 * Removes entries that were not updated from the array of edge_netmeter_instance
 * pointers. Memory allocated for the removed entries are freed.
 * @param nms [i/o] pointer to an array of edge_netmeter_instance pointers
 * @param max_nms [i/o] pointer to the number of entries in the array
 * @return 0 on success; 1 on any failure.
 */
int clean_edge_netmeter_instances(edge_netmeter_instance ***nms, int *max_nms) {
    int i = 0;
    int r = 0;
    
    edge_netmeter_instance **result = NULL;
    edge_netmeter_instance **n = *nms;
    for (i = 0; i < *max_nms; i++) {
        if (!(n[i]->updated)) {
            free_edge_netmeter_instance(n[i]);
            EUCA_FREE(n[i]);
            n[i] = NULL;
        }
    }
    result = EUCA_ZALLOC_C(*max_nms, sizeof (edge_netmeter_instance *));
    for (i = 0; i < *max_nms; i++) {
        if (n[i]) {
            result[r] = n[i];
            r++;
        }
    }
    EUCA_FREE(n);
    *nms = result;
    *max_nms = r;
    return (0);
}

/**
 * Clears the "updated" flag of all netmeter entries.
 * @param nm [in] edge_netmeter structure of interest.
 * @return always 0.
 */
int clear_edge_netmeter_tag(edge_netmeter *nm) {
    if (!nm) {
        return (0);
    }
    for (int i = 0; i < nm->max_pub_ips; i++) {
        if (nm->pub_ips[i]) {
            nm->pub_ips[i]->updated = FALSE;
        }
    }
    for (int i = 0; i < nm->max_priv_ips; i++) {
        if (nm->priv_ips[i]) {
            nm->priv_ips[i]->updated = FALSE;
        }
    }
    return (0);
}

/**
 * Dump EDGE netmeter to file(s).
 * Each line represents an instance: time,i-ID,pub pkts in,pub bytes in,pub pkts out,pub bytes out,
 * priv pkts in,priv bytes in,priv pkts out,priv pkts in.
 * Successful execution of do_edge_update_netmeter() is required.
 * @param  edge_config structure of interest
 * @return always 0.
 */
int edge_dump_netmeter(edge_config *edge) {
    int ret = 0;
    struct timeval tv = { 0 };
    FILE *SRFH;
    FILE *NMFH;
    FILE *DFH;
    char sensorfname[EUCA_MAX_PATH];
    char nmfname[EUCA_MAX_PATH];
    char dfname[EUCA_MAX_PATH];
    char dfname1[EUCA_MAX_PATH];
    boolean dfname_exists = FALSE;

    eucanetd_timer_usec(&tv);
    LOGDEBUG("Dump netmeter to file(s).\n");

    // Make sure our given parameter is valid
    if (!edge || !edge->config || !edge->gni || !edge->nmeter) {
        LOGERROR("Invalid argument: cannot dump netmeter from NULL configuration.\n");
        return (1);
    }

    char ts[32] = { 0 };
    time_t t = time(NULL);
    struct tm tm = { 0 };
    localtime_r(&t, &tm);
    strftime(ts, 32, "%F_%T", &tm);

    char *eucahome = NULL;
    if (strcmp(edge->config->eucahome, "/")) {
        eucahome = strdup(edge->config->eucahome);
    } else {
        eucahome = strdup("");
    }
    snprintf(sensorfname, EUCA_MAX_PATH, EDGE_NETMETER_FILE_SENSOR, eucahome);
    snprintf(nmfname, EUCA_MAX_PATH, EDGE_NETMETER_FILE_NEW, eucahome);
    snprintf(dfname, EUCA_MAX_PATH, EDGE_NETMETER_FILE_DONE, eucahome);
    EUCA_FREE(eucahome);

    long int timestamp = eucanetd_get_timestamp();
    
    unlink(sensorfname);
    SRFH = fopen(sensorfname, "w");

    unlink(nmfname);
    NMFH = fopen(nmfname, "w");

    if (access(dfname, F_OK) != -1) {
        dfname_exists = TRUE;

        struct stat statbuf = { 0 };
        
        if (stat(dfname, &statbuf) == 0) {
            long dfsize = (long) statbuf.st_size;
            if (dfsize > EDGE_NETMETER_FILE_MAX_SIZE) {
                snprintf(dfname1, EUCA_MAX_PATH, "%s.1", dfname);
                if (rename(dfname, dfname1)) {
                    LOGDEBUG("Failed to rename %s\n", dfname);
                }
                dfname_exists = FALSE;
            }
        }
    }
    DFH = fopen(dfname, "a");

    if (!NMFH || !DFH || !SRFH) {
        LOGWARN("failed to open netmeter file(s)\n");
        ret = 1;
    } else {

        fprintf(NMFH, "time,instance,ipaddr,type,pktin,bytesin,pktout,bytesout\n");
        if (!dfname_exists) {
            fprintf(DFH, "time,instance,ipaddr,type,pktin,bytesin,pktout,bytesout\n");
        }

        for (int i = 0; i < edge->nmeter->max_pub_ips; i++) {
            edge_netmeter_instance *nm = edge->nmeter->pub_ips[i];
            if (nm->updated) {
                fprintf(NMFH, "%s,%s,%s,pub,%ld,%ld,%ld,%ld\n", ts, nm->instance_id, nm->ipaddr,
                        nm->pkts_in, nm->bytes_in, nm->pkts_out, nm->bytes_out);
                fprintf(SRFH, "%s\t%ld\tNetworkInExternal\tsummation\tdefault\t%ld\n",
                        nm->instance_id, timestamp, nm->bytes_in);
                fprintf(SRFH, "%s\t%ld\tNetworkOutExternal\tsummation\tdefault\t%ld\n",
                        nm->instance_id, timestamp, nm->bytes_out);
/*
                fprintf(SRFH, "%s\t%ld\tNetworkPacketsInExternal\tsummation\tdefault\t%ld\n",
                        nm->instance_id, timestamp, nm->pkts_in);
                fprintf(SRFH, "%s\t%ld\tNetworkPacketsOutExternal\tsummation\tdefault\t%ld\n",
                        nm->instance_id, timestamp, nm->pkts_out);
*/
            } else {
                fprintf(DFH, "%s,%s,%s,pub,%ld,%ld,%ld,%ld\n", ts, nm->instance_id, nm->ipaddr,
                        nm->pkts_in, nm->bytes_in, nm->pkts_out, nm->bytes_out);
            }
        }
        for (int i = 0; i < edge->nmeter->max_priv_ips; i++) {
            edge_netmeter_instance *nm = edge->nmeter->priv_ips[i];
            if (nm->updated) {
                fprintf(NMFH, "%s,%s,%s,priv,%ld,%ld,%ld,%ld\n", ts, nm->instance_id, nm->ipaddr,
                        nm->pkts_in, nm->bytes_in, nm->pkts_out, nm->bytes_out);
            } else {
                fprintf(DFH, "%s,%s,%s,priv,%ld,%ld,%ld,%ld\n", ts, nm->instance_id, nm->ipaddr,
                        nm->pkts_in, nm->bytes_in, nm->pkts_out, nm->bytes_out);
            }
        }
    }

    if (SRFH) fclose(SRFH);
    if (NMFH) fclose(NMFH);
    if (DFH) fclose(DFH);
    
    clean_edge_netmeter_instances(&(edge->nmeter->pub_ips), &(edge->nmeter->max_pub_ips));
    clean_edge_netmeter_instances(&(edge->nmeter->priv_ips), &(edge->nmeter->max_priv_ips));
    LOGTRACE("\tdump netmeter to file(s) in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (ret);
}

/**
 * Tests if the given ip is local (as specified by the edge_config structure in
 * the argument)
 * @param edge [in] edge_config structure of interest
 * @param ip [in] IP address of interest
 * @return TRUE if ip is found in edge. FALSE if ip is not found.
 */
boolean is_my_ip(edge_config *edge, u32 ip) {
    for (int i = 0; i < edge->max_my_instances; i++) {
        if (ip == edge->my_instances[i].privateIp) return (TRUE);
        if (ip == edge->my_instances[i].publicIp) return (TRUE);
    }
    return (FALSE);
}

/**
 * Compares edge_config data structures a and b.
 * @param a [in] edge_config data structure of interest
 * @param b [in] edge_config data structure of interest
 * @param instances_diff [out] optionally set to 1 iff instances local to NC in a and b differ
 * @param sgs_diff [out] optionally set to 1 iff security groups in a an b differ
 * @param instances_diff [out] optionally set to 1 iff instances in a and b differ
 * @return 0 if properties of a and b matches. Properties gni and config are
 * not taken into account. Non-zero if properties that differ are found.
 */
int cmp_edge_config(edge_config *a, edge_config *b, int *my_instances_diff,
        int *sgs_diff, int *instances_diff) {
    int abmatch = 1;

    if (my_instances_diff) {
        *my_instances_diff = 0;
    }
    if (sgs_diff) {
        *sgs_diff = 0;
    }
    if (instances_diff) {
        *instances_diff = 0;
    }

    if (a == b) {
        return (0);
    }
    if ((a == NULL) || (b == NULL)) {
        abmatch = 0;
    }
    
    // Only compare the name of cluster (should not differ in normal use)
    if (abmatch && a->my_cluster && b->my_cluster) {
        if (strcmp(a->my_cluster->name, b->my_cluster->name)) {
            abmatch = 0;
        }
    } else {
        abmatch = 0;
    }
    
    // Only compare the name of node (should not differ in normal use)
    if (abmatch && a->my_node && b->my_node) {
        if (strcmp(a->my_node->name, b->my_node->name)) {
            abmatch = 0;
        }
    } else {
        abmatch = 0;
    }
    
    // Compare instances
    if (abmatch && a->my_instances && b->my_instances) {
        if (a->max_my_instances != b->max_my_instances) {
            abmatch = 0;
        } else {
            for (int i = 0; i < a->max_my_instances && abmatch; i++) {
                if (cmp_gni_instance(&(a->my_instances[i]), &(b->my_instances[i]))) {
                    abmatch = 0;
                }
            }
        }
    } else {
        abmatch = 0;
    }
    if (!abmatch && my_instances_diff) {
        *my_instances_diff = 1;
    }
    
    // Compare security groups
    if (abmatch && a->my_sgs && b->my_sgs) {
        if (a->max_my_sgs != b->max_my_sgs) {
            abmatch = 0;
        } else {
            for (int i = 0; i < a->max_my_sgs && abmatch; i++) {
                if (cmp_gni_secgroup(a->my_sgs[i], b->my_sgs[i], NULL, NULL, NULL)) {
                    abmatch = 0;
                }
            }
        }
    } else {
        abmatch = 0;
    }
    if (!abmatch && sgs_diff) {
        *sgs_diff = 1;
    }

    // Compare all instances
    if (abmatch && a->gni && b->gni) {
        if (a->gni->max_instances != b->gni->max_instances) {
            abmatch = 0;
        } else {
            for (int i = 0; i < a->gni->max_instances && abmatch; i++) {
                if (cmp_gni_instance(a->gni->instances[i], b->gni->instances[i])) {
                    abmatch = 0;
                }
            }
        }
    } else {
        abmatch = 0;
    }
    if (!abmatch && instances_diff) {
        *instances_diff = 1;
    }

    if (abmatch) {
        return (0);
    }
    return (1);
}
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
#include <sequence_executor.h>
#include <atomic_file.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "eucanetd_config.h"
#include "euca_gni.h"
#include "euca_lni.h"
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
 //! @name EDGE Mode Network Driver APIs
static int network_driver_init(eucanetdConfig * pEucanetdConfig);
static int network_driver_cleanup(globalNetworkInfo * pGni, boolean forceFlush);
static int network_driver_system_flush(globalNetworkInfo * pGni);
//static u32 network_driver_system_scrub(globalNetworkInfo * pGni, lni_t * pLni);
//static int network_driver_implement_network(globalNetworkInfo * pGni, lni_t * pLni);
static int network_driver_implement_sg(globalNetworkInfo * pGni, lni_t * pLni);
static int network_driver_implement_addressing(globalNetworkInfo * pGni, lni_t * pLni);
//! @}

static int generate_dhcpd_config(globalNetworkInfo * pGni);

static int update_private_ips(globalNetworkInfo * pGni);
static int update_elastic_ips(globalNetworkInfo * pGni);
static int update_l2_addressing(globalNetworkInfo * pGni);

#ifdef USE_IP_ROUTE_HANDLER
static int install_public_routes(globalNetworkInfo * pGni);
static int install_private_routes(globalNetworkInfo * pGni);
#endif /* USE_IP_ROUTE_HANDLER */
static int update_host_arp(void);

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
    .system_flush = network_driver_system_flush,
    .system_maint = NULL,
    //.system_scrub = network_driver_system_scrub,
    .system_scrub = NULL,
    //.implement_network = network_driver_implement_network,
    .implement_network = NULL,
    .implement_sg = network_driver_implement_sg,
    .implement_addressing = network_driver_implement_addressing,
    .handle_signal = NULL,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Initialize the network driver.
//!
//! @param[in] pEucanetdConfig a pointer to our application configuration
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre \li The core application configuration must be completed prior calling
//!      \li The driver must not be already initialized (if its the case, a no-op will occur)
//!      \li The pEucanetdConfig parameter must not be NULL
//!
//! @post On success the driver is properly configured. On failure, the state of
//!       the driver is non-deterministic. If the driver was previously initialized,
//!       this will result into a no-op.
//!
//! @note
//!
static int network_driver_init(eucanetdConfig * pEucanetdConfig)
{
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
    // We are now initialize
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
//!     The driver should have been initialized by now
//!
//! @post
//!     On success, the network driver has been cleaned up and the system flushed
//!     if forceFlush was set. On failure, the system state will be non-deterministic.
//!
//! @note
//!
static int network_driver_cleanup(globalNetworkInfo * pGni, boolean forceFlush)
{
    int ret = 0;

    LOGINFO("Cleaning up '%s' network driver.\n", DRIVER_NAME());
    if (forceFlush) {
        if (network_driver_system_flush(pGni)) {
            LOGERROR("Fail to flush network artifacts during network driver cleanup. See above log errors for details.\n");
            ret = 1;
        }
    }
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
//! @pre The driver must be initialized already
//!
//! @post On success, all networking mode artifacts will be flushed from the system. If any
//!       failure occurred. The system is left in a non-deterministic state and a subsequent
//!       call to this API may resolve the remaining issues.
//!
//! @note
//!
static int network_driver_system_flush(globalNetworkInfo * pGni)
{
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
    // iptables
    rc |= ipt_handler_init(config->ipt, config->cmdprefix, NULL);
    rc |= ipt_handler_repopulate(config->ipt);
    rc |= ipt_chain_flush(config->ipt, "filter", "EUCA_FILTER_FWD");
    rc |= ipt_chain_flush(config->ipt, "filter", "EUCA_COUNTERS_IN");
    rc |= ipt_chain_flush(config->ipt, "filter", "EUCA_COUNTERS_OUT");
    rc |= ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_PRE");
    rc |= ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_POST");
    rc |= ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_OUT");
    rc |= ipt_table_deletechainmatch(config->ipt, "filter", "EU_");
    // Flush core artifacts
    if (config->flushmode == FLUSH_ALL) {
        rc |= ipt_table_deletechainmatch(config->ipt, "filter", "EUCA_");
        rc |= ipt_table_deletechainmatch(config->ipt, "nat", "EUCA_");
        rc |= ipt_chain_flush_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD_PREUSERHOOK");
        rc |= ipt_chain_flush_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD");
        rc |= ipt_chain_flush_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD_POSTUSERHOOK");
        rc |= ipt_chain_flush_rule(config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE_PREUSERHOOK");
        rc |= ipt_chain_flush_rule(config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE");
        rc |= ipt_chain_flush_rule(config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE_POSTUSERHOOK");
        rc |= ipt_chain_flush_rule(config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST_PREUSERHOOK");
        rc |= ipt_chain_flush_rule(config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST");
        rc |= ipt_chain_flush_rule(config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST_POSTUSERHOOK");
        rc |= ipt_chain_flush_rule(config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT_PREUSERHOOK");
        rc |= ipt_chain_flush_rule(config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT");
        rc |= ipt_chain_flush_rule(config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT_POSTUSERHOOK");
    }
    rc |= ipt_handler_print(config->ipt);
    rc |= ipt_handler_deploy(config->ipt);
    if (rc) {
        LOGERROR("Failed to flush the IP Tables artifact in '%s' networking mode.\n", DRIVER_NAME());
        ret = 1;
    }
    // ipsets
    rc = 0;
    rc |= ips_handler_init(config->ips, config->cmdprefix);
    rc |= ips_handler_repopulate(config->ips);
    rc |= ips_handler_deletesetmatch(config->ips, "EU_");
    rc |= ips_handler_deletesetmatch(config->ips, "EUCA_");
    rc |= ips_handler_print(config->ips);
    rc |= ips_handler_deploy(config->ips, 1);
    if (rc) {
        LOGERROR("Failed to flush the IP Sets artifact in '%s' networking mode.\n", DRIVER_NAME());
        ret = 1;
    }
    // ebtables
    rc = 0;
    rc |= ebt_handler_init(config->ebt, config->cmdprefix);
    rc |= ebt_handler_repopulate(config->ebt);
    rc |= ebt_chain_flush(config->ebt, "filter", "EUCA_EBT_FWD");
    rc |= ebt_chain_flush(config->ebt, "nat", "EUCA_EBT_NAT_PRE");
    rc |= ebt_chain_flush(config->ebt, "nat", "EUCA_EBT_NAT_POST");
    // Flush core artifacts
    if (config->flushmode == FLUSH_ALL) {
        rc |= ebt_table_deletechainmatch(config->ebt, "filter", "EUCA_");
        rc |= ebt_table_deletechainmatch(config->ebt, "nat", "EUCA_");
        rc |= ebt_chain_flush_rule(config->ebt, "filter", "FORWARD", "-j EUCA_EBT_FWD");
        rc |= ebt_chain_flush_rule(config->ebt, "nat", "PREROUTING", "-j EUCA_EBT_NAT_PRE");
        rc |= ebt_chain_flush_rule(config->ebt, "nat", "POSTROUTING", "-j EUCA_EBT_NAT_POST");
    }
    rc |= ebt_handler_deploy(config->ebt);
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
    sequence_executor cmds = { {0} };
    char *strptra = NULL;

    if (getdevinfo(config->pubInterface, &ips, &nms, &max_nets)) {
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
                    se_init(&cmds, config->cmdprefix, 2, 1);

                    strptra = hex2dot(pGni->public_ips[i]);
                    snprintf(cmd, EUCA_MAX_PATH, "ip addr del %s/%d dev %s >/dev/null 2>&1", strptra, 32, config->pubInterface);
                    EUCA_FREE(strptra);

                    rc = se_add(&cmds, cmd, NULL, ignore_exit2);
                    se_print(&cmds);
                    rc = se_execute(&cmds);
                    if (rc) {
                        LOGERROR("could not execute command sequence (check above log errors for details): flushing public ips\n");
                        ret = 1;
                    }
                    se_free(&cmds);
                }
            }
        }
        EUCA_FREE(ips);
        EUCA_FREE(nms);
    }
    return (ret);
}

//!
//! This API checks the new GNI against the system view to decide what really
//! needs to be done.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] pLni a pointer to the Local Network Information structure
//!
//! @return A bitmask indicating what needs to be done. The following bits are
//!         the ones to look for: EUCANETD_RUN_NETWORK_API, EUCANETD_RUN_SECURITY_GROUP_API
//!         and EUCANETD_RUN_ADDRESSING_API.
//!
//! @see
//!
//! @pre \li Both pGni and pLni must not be NULL
//!      \li The driver must be initialized prior to calling this API.
//!
//! @post
//!
//! @note
//!
/*
static u32 network_driver_system_scrub(globalNetworkInfo * pGni, lni_t * pLni)
{
    LOGINFO("Scrubbing for '%s' network driver.\n", DRIVER_NAME());
    return (EUCANETD_RUN_ALL_API);
}
*/

//!
//! This takes care of implementing the network artifacts necessary. This will add or
//! remove devices, tunnels, etc. as necessary.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] pLni a pointer to the Local Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre Both pGni and pLni must not be NULL
//!
//! @post On success, the networking artifacts should be implemented. On failure, the
//!       current state of the system may be left in a non-deterministic state. A
//!       subsequent call to this API may resolve the remaining issues.
//!
//! @note For EDGE mode, we have no networking artifacts that we own.
//!
/*
static int network_driver_implement_network(globalNetworkInfo * pGni, lni_t * pLni)
{
    LOGINFO("Implementing network artifacts for '%s' network driver.\n", DRIVER_NAME());

    // this only applies to NC components
    if (!PEER_IS_NC(eucanetdPeer)) {
        // no-op
        return (0);
    }
    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to implement network artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Are the global and local network view structures NULL?
    if (!pGni || !pLni) {
        LOGERROR("Failed to implement network artifacts for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    return (0);
}
*/

//!
//! This takes care of implementing the security-group artifacts necessary. This will add or
//! remove networking rules pertaining to the groups and their membership.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] pLni a pointer to the Local Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre \li Both pGni and pLni must not be NULL
//!      \li the driver must have been initialized
//!
//! @post On success, the networking artifacts should be implemented. On failure, the
//!       current state of the system may be left in a non-deterministic state. A
//!       subsequent call to this API may resolve the left over issues.
//!
//! @note For EDGE mode, this means installing the IP table rules for the groups and the
//!       IP sets for the groups' members
//!
static int network_driver_implement_sg(globalNetworkInfo * pGni, lni_t * pLni)
{
#define MAX_RULE_LEN              1024

    int i = 0;
    int j = 0;
    int k = 0;
    int rc = 0;
    int ret = 0;
    int slashnet = 0;
    int max_instances = 0;
    char *strptra = NULL;
    char *chainname = NULL;
    char *refchainname = NULL;
    char rule[MAX_RULE_LEN] = "";
    gni_cluster *mycluster = NULL;
    gni_secgroup *secgroup = NULL;
    gni_secgroup *refsecgroup = NULL;
    gni_instance *instances = NULL;
    int max_myinstances = 0;
    gni_instance *myinstances = NULL;
    gni_node *myself = NULL;
    u32 cidrnm = 0xffffffff;

    LOGTRACE("Implementing security-group artifacts for '%s' network driver.\n", DRIVER_NAME());

    // this only applies to NC components
    if (!PEER_IS_NC(eucanetdPeer)) {
        // no-op
        return (0);
    }
    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to implement security-group artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Are the global and local network view structures NULL?
    if (!pGni || !pLni) {
        LOGERROR("Failed to implement security-group artifacts for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    rc = gni_find_self_cluster(pGni, &mycluster);
    if (rc) {
        LOGERROR("cannot find cluster to which local node belongs, in global network view: check network config settings\n");
        return (1);
    }
    // pull in latest IPT state
    rc = ipt_handler_repopulate(config->ipt);
    if (rc) {
        LOGERROR("cannot read current IPT rules: check above log errors for details\n");
        return (1);
    }
    // pull in latest IPS state
    rc = ips_handler_repopulate(config->ips);
    if (rc) {
        LOGERROR("cannot read current IPS sets: check above log errors for details\n");
        return (1);
    }
    // make sure euca chains are in place
    rc = ipt_table_add_chain(config->ipt, "filter", "EUCA_FILTER_FWD_PREUSERHOOK", "-", "[0:0]");
    rc = ipt_table_add_chain(config->ipt, "filter", "EUCA_FILTER_FWD", "-", "[0:0]");
    rc = ipt_table_add_chain(config->ipt, "filter", "EUCA_FILTER_FWD_POSTUSERHOOK", "-", "[0:0]");
    rc = ipt_table_add_chain(config->ipt, "filter", "EUCA_COUNTERS_IN", "-", "[0:0]");
    rc = ipt_table_add_chain(config->ipt, "filter", "EUCA_COUNTERS_OUT", "-", "[0:0]");
    rc = ipt_chain_add_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD_PREUSERHOOK");
    rc = ipt_chain_add_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD");
    rc = ipt_chain_add_rule(config->ipt, "filter", "FORWARD", "-A FORWARD -j EUCA_FILTER_FWD_POSTUSERHOOK");

    // clear all chains that we're about to (re)populate with latest network metadata
    rc = ipt_table_deletechainmatch(config->ipt, "filter", "EU_");
    rc = ipt_chain_flush(config->ipt, "filter", "EUCA_FILTER_FWD");
    rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", "-A EUCA_FILTER_FWD -j EUCA_COUNTERS_IN");
    rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", "-A EUCA_FILTER_FWD -j EUCA_COUNTERS_OUT");
    rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", "-A EUCA_FILTER_FWD -m conntrack --ctstate ESTABLISHED -j ACCEPT");
    rc = ipt_chain_flush(config->ipt, "filter", "EUCA_COUNTERS_IN");
    rc = ipt_chain_flush(config->ipt, "filter", "EUCA_COUNTERS_OUT");

    // reset and create ipsets for allprivate and noneuca subnet sets
    rc = ips_handler_deletesetmatch(config->ips, "EU_");

    ips_handler_add_set(config->ips, "EUCA_ALLPRIVATE");
    ips_set_flush(config->ips, "EUCA_ALLPRIVATE");
    ips_set_add_net(config->ips, "EUCA_ALLPRIVATE", "127.0.0.1", 32);

    ips_handler_add_set(config->ips, "EUCA_ALLNONEUCA");
    ips_set_flush(config->ips, "EUCA_ALLNONEUCA");
    ips_set_add_net(config->ips, "EUCA_ALLNONEUCA", "127.0.0.1", 32);

    // add addition of private non-euca subnets to EUCA_ALLPRIVATE, here
    for (i = 0; i < pGni->max_subnets; i++) {
        strptra = hex2dot(pGni->subnets[i].subnet);
        slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - pGni->subnets[i].netmask) + 1))));
        ips_set_add_net(config->ips, "EUCA_ALLNONEUCA", strptra, slashnet);
        EUCA_FREE(strptra);
    }

    // find all instances that are local to this NC
    rc = gni_find_self_node(pGni, &myself);
    if (!rc) {
        rc = gni_node_get_instances(pGni, myself, NULL, 0, NULL, 0, &myinstances, &max_myinstances);
    } else {
        LOGWARN("Failed to retrieve list of local instances.\n");
        max_myinstances = 0;
    }

    // add chains/rules
    for (i = 0; i < pGni->max_secgroups; i++) {
        chainname = NULL;
        secgroup = NULL;
        instances = NULL;
        max_instances = 0;

        secgroup = &(pGni->secgroups[i]);
        rule[0] = '\0';
        rc = gni_secgroup_get_chainname(pGni, secgroup, &chainname);
        if (rc) {
            LOGERROR("cannot get chain name from security group: check above log errors for details\n");
            ret = 1;
        } else {

            ips_handler_add_set(config->ips, chainname);
            ips_set_flush(config->ips, chainname);

            strptra = hex2dot(config->vmGatewayIP);
            ips_set_add_ip(config->ips, chainname, strptra);
            ips_set_add_ip(config->ips, "EUCA_ALLNONEUCA", strptra);
            EUCA_FREE(strptra);

            rc = gni_secgroup_get_instances(pGni, secgroup, NULL, 0, NULL, 0, &instances, &max_instances);

            for (j = 0; j < max_instances; j++) {
                if (instances[j].privateIp) {
                    strptra = hex2dot(instances[j].privateIp);
                    ips_set_add_ip(config->ips, chainname, strptra);
                    ips_set_add_ip(config->ips, "EUCA_ALLPRIVATE", strptra);
                    EUCA_FREE(strptra);
                }
                if (instances[j].publicIp) {
                    strptra = hex2dot(instances[j].publicIp);
                    ips_set_add_ip(config->ips, chainname, strptra);
                    EUCA_FREE(strptra);
                }
            }

            EUCA_FREE(instances);

            // add forward chain
            ipt_table_add_chain(config->ipt, "filter", chainname, "-", "[0:0]");
            ipt_chain_flush(config->ipt, "filter", chainname);

            // add jump rule
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_FILTER_FWD -m set --match-set %s dst -j %s", chainname, chainname);
            ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", rule);

            // populate forward chain

            // this one needs to be first
            snprintf(rule, MAX_RULE_LEN, "-A %s -m set --match-set %s src -j ACCEPT", chainname, chainname);
            ipt_chain_add_rule(config->ipt, "filter", chainname, rule);
            // make sure conntrack rule is in place
            //snprintf(rule, MAX_RULE_LEN, "-A %s -m conntrack --ctstate ESTABLISHED -j ACCEPT", chainname);
            //ipt_chain_add_rule(config->ipt, "filter", chainname, rule);

            // then put all the group specific IPT rules (temporary one here)
            if (secgroup->max_ingress_rules) {
                for (j = 0; j < secgroup->max_ingress_rules; j++) {
                    // If this rule is in reference to another group, lets add this IP set here
                    if (strlen(secgroup->ingress_rules[j].groupId) != 0) {
                        // Create the IP set first and add localhost as a holder
                        //ips_handler_add_set(config->ips, secgroup->ingress_rules[j].groupId);
                        //ips_set_add_ip(config->ips, secgroup->ingress_rules[j].groupId, "127.0.0.1");
                        rc = gni_find_secgroup(pGni, secgroup->ingress_rules[j].groupId, &refsecgroup);
                        if (0 != rc) {
                            LOGWARN("Could not find referenced security group %s. Skipping ingress rule.\n", secgroup->ingress_rules[j].groupId);
                        } else {
                            LOGDEBUG("Found referenced security group %s owner %s\n", refsecgroup->name, refsecgroup->accountId);
                            refchainname = NULL;
                            rc = gni_secgroup_get_chainname(pGni, refsecgroup, &refchainname);
                            if (rc) {
                                LOGERROR("cannot get chain name from security group: check above log errors for details\n");
                                ret = 1;
                            } else {
                                ips_handler_add_set(config->ips, refchainname);
                                ips_set_add_ip(config->ips, refchainname, "127.0.0.1");
                                // Next add the rule
                                //snprintf(rule, MAX_RULE_LEN, "-A %s -m set --set %s src %s -j ACCEPT", chainname, secgroup->ingress_rules[j].groupId, secgroup->grouprules[j].name);
                                //ipt_chain_add_rule(config->ipt, "filter", chainname, rule);
                                ingress_gni_to_iptables_rule(NULL, &(secgroup->ingress_rules[j]), rule, 0);
                                strptra = strdup(rule);
                                snprintf(rule, MAX_RULE_LEN, "-A %s -m set --set %s src %s -j ACCEPT", chainname, refchainname, strptra);
                                ipt_chain_add_rule(config->ipt, "filter", chainname, rule);
                                EUCA_FREE(strptra);
                            }
                            EUCA_FREE(refchainname);
                        }

                    } else {
                        ingress_gni_to_iptables_rule(NULL, &(secgroup->ingress_rules[j]), rule, 0);
                        strptra = strdup(rule);
                        snprintf(rule, MAX_RULE_LEN, "-A %s %s -j ACCEPT", chainname, strptra);
                        ipt_chain_add_rule(config->ipt, "filter", chainname, rule);
                        EUCA_FREE(strptra);

                        // Check if this rule refers to a public IP that this NC is responsible for
                        if (secgroup->ingress_rules[j].cidr) {
                            // Ignoring potential shift by 32 on a u32. If cidrsn is 0, the rule will not be processed (it allows all)
                            cidrnm = (u32) 0xffffffff << (32 - secgroup->ingress_rules[j].cidrSlashnet);
                            if (secgroup->ingress_rules[j].cidrSlashnet != 0) {
                                // Search for public IPs that this NC is responsible
                                for (k = 0; k < max_myinstances; k++) {
                                    if (((myinstances[k].publicIp & cidrnm) == (secgroup->ingress_rules[j].cidrNetaddr & cidrnm)) &&
                                            ((myinstances[k].privateIp & cidrnm) != (secgroup->ingress_rules[j].cidrNetaddr & cidrnm))) {
                                        strptra = hex2dot(myinstances[k].privateIp);
                                        LOGDEBUG("Found instance private IP (%s) local to this NC affected by rule (%s).\n", strptra, secgroup->grouprules[j].name);
                                        ingress_gni_to_iptables_rule(strptra, &(secgroup->ingress_rules[j]), rule, 1);
                                        LOGDEBUG("Created new iptables rule: %s\n", rule);
                                        EUCA_FREE(strptra);
                                        strptra = strdup(rule);
                                        snprintf(rule, MAX_RULE_LEN, "-A %s %s -j ACCEPT", chainname, strptra);
                                        ipt_chain_add_rule(config->ipt, "filter", chainname, rule);
                                        EUCA_FREE(strptra);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            EUCA_FREE(chainname);
        }
    }
    EUCA_FREE(myinstances);

    // last rule in place is to DROP if no accepts have made it past the FWD chains, and the dst IP is in the ALLPRIVATE ipset
    snprintf(rule, MAX_RULE_LEN, "-A EUCA_FILTER_FWD -m set --match-set EUCA_ALLPRIVATE dst -j DROP");
    ipt_chain_add_rule(config->ipt, "filter", "EUCA_FILTER_FWD", rule);

    // Deploy our IP sets
    if (1 || !ret) {
        ips_handler_print(config->ips);
        rc = ips_handler_deploy(config->ips, 0);
        if (rc) {
            LOGERROR("could not apply ipsets: check above log errors for details\n");
            ret = 1;
        }
    }
    // Deploy our IP Table rules
    if (1 || !ret) {
        ipt_handler_print(config->ipt);
        rc = ipt_handler_deploy(config->ipt);
        if (rc) {
            LOGERROR("could not apply new rules: check above log errors for details\n");
            ret = 1;
        }
    }

    if (1 || !ret) {
        ips_handler_print(config->ips);
        rc = ips_handler_deploy(config->ips, 1);
        if (rc) {
            LOGERROR("could not apply ipsets: check above log errors for details\n");
            ret = 1;
        }
    }

    return (ret);

#undef MAX_RULE_LEN
}

//!
//! This takes care of implementing the addressing artifacts necessary. This will add or
//! remove IP addresses and elastic IPs for each instances.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] pLni a pointer to the Local Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see update_private_ips(), update_elastic_ips(), update_l2_addressing()
//!
//! @pre \li Both pGni and pLni must not be NULL
//!      \li the driver must have been initialized
//!
//! @post On success, the networking artifacts should be implemented. On failure, the
//!       current state of the system may be left in a non-deterministic state. A
//!       subsequent call to this API may resolve the left over issues.
//!
//! @note For EDGE mode, this means creating the DHCP configuration for private addresses,
//!       installing the NAT rules for elastic IPs and the L2 addressing rules.
//!
static int network_driver_implement_addressing(globalNetworkInfo * pGni, lni_t * pLni)
{
    int rc = 0;
    int ret = 0;

    LOGTRACE("Implementing addressing artifacts for '%s' network driver.\n", DRIVER_NAME());

    // this only applies to NC components
    if (!PEER_IS_NC(eucanetdPeer)) {
        // no-op
        return (0);
    }
    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to implement addressing artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Are the global and local network view structures NULL?
    if (!pGni || !pLni) {
        LOGERROR("Failed to implement addressing artifacts for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (1);
    }

    // Install the elastic IPs artifacts for instances
    rc = update_elastic_ips(pGni);
    if (rc) {
        LOGERROR("could not complete update of public IPs: check above log errors for details\n");
        ret = 1;
    }

    // Install the L2 addressing artifacts for instances
    rc = update_l2_addressing(pGni);
    if (rc) {
        LOGERROR("could not complete update of public IPs: check above log errors for details\n");
        ret = 1;
    }

    // Install the private IPs artifacts for instances
    rc = update_private_ips(pGni);
    if (rc) {
        LOGERROR("could not complete update of private IPs: check above log errors for details\n");
        ret = 1;
    }
    return (ret);
}

//!
//! Generates the DHCP server configuration so the instances can get their
//! networking configuration information.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see
//!
//! @pre The pGni parameter MUST not be NULL
//!
//! @post On success the DHCP server configuration is created. On failure, the
//!       DHCP configuration file should not exists.
//!
//! @note
//!
static int generate_dhcpd_config(globalNetworkInfo * pGni)
{
    int i = 0;
    int rc = 0;
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
    gni_node *myself = NULL;
    gni_cluster *mycluster = NULL;
    gni_instance *instances = NULL;

    // Make sure the given pointer is valid
    if (!pGni) {
        LOGERROR("Cannot configure DHCP server. Invalid parameter provided.\n");
        return (1);
    }
    // Find our associated cluster
    rc = gni_find_self_cluster(pGni, &mycluster);
    if (rc) {
        LOGERROR("cannot find the cluster to which the local node belongs: check network config settings\n");
        return (1);
    }
    // Find ourself as a node
    rc = gni_find_self_node(pGni, &myself);
    if (rc) {
        LOGERROR("cannot find local node in global network state: check network config settings\n");
        return (1);
    }
    // Get our instance list
    rc = gni_node_get_instances(pGni, myself, NULL, 0, NULL, 0, &instances, &max_instances);
    if (rc) {
        LOGERROR("cannot find instances belonging to this node: check network config settings\n");
        return (1);
    }

    nw = mycluster->private_subnet.subnet;
    nm = mycluster->private_subnet.netmask;
    //    rt = mycluster->private_subnet.gateway;

    // Open the DHCP configuration file
    snprintf(dhcpd_config_path, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.conf", config->eucahome);
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
        router = hex2dot(config->vmGatewayIP);  // this is set by configuration

        fprintf(OFH, "subnet %s netmask %s {\n  option subnet-mask %s;\n  option broadcast-address %s;\n", network, netmask, netmask, broadcast);
        if (strlen(pGni->instanceDNSDomain)) {
            fprintf(OFH, "  option domain-name \"%s\";\n", pGni->instanceDNSDomain);
        }

        if (pGni->max_instanceDNSServers) {
            strptra = hex2dot(pGni->instanceDNSServers[0]);
            fprintf(OFH, "  option domain-name-servers %s", SP(strptra));
            EUCA_FREE(strptra);
            for (i = 1; i < pGni->max_instanceDNSServers; i++) {
                strptra = hex2dot(pGni->instanceDNSServers[i]);
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

    // Free our instance list
    EUCA_FREE(instances);
    return (ret);
}

//!
//! Update the private IP addressing. This will ensure a DHCP configuration file
//! is generated and the server restarted upon success.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see generate_dhcpd_config(), eucanetd_kick_dhcpd_server()
//!
//! @pre The pGni parameter must not be NULL
//!
//! @post On success, the DHCP server configuration is updated and the server
//!       restarted
//!
//! @note
//!
static int update_private_ips(globalNetworkInfo * pGni)
{
    int rc = 0;
    struct timeval tv = { 0 };

    eucanetd_timer_usec(&tv);
    LOGDEBUG("Updating private IP and DHCPD handling.\n");

    // Make sure our given parameter is valid
    if (!pGni) {
        LOGERROR("Failed to update private IP addressing. Invalid parameters provided.\n");
        return (1);
    }
#ifdef USE_IP_ROUTE_HANDLER
    // Make sure we can install our private and public subnet routes if NC_PROXY is enabled
    if (config->nc_proxy) {
        if ((rc = install_private_routes(pGni)) != 0) {
            LOGERROR("unable to generate private IP routes: check above log errors for details\n");
            return (1);
        }

        if ((rc = install_public_routes(pGni)) != 0) {
            LOGERROR("unable to generate private IP routes: check above log errors for details\n");
            return (1);
        }
    }
#endif /* USE_IP_ROUTE_HANDLER */
    // Generate the DHCP configuration so instances can get their network config
    if ((rc = generate_dhcpd_config(pGni)) != 0) {
        LOGERROR("unable to generate new dhcp configuration file: check above log errors for details\n");
        return (1);
    }
    // Restart the DHCP server so it can pick up the new configuration
    if ((rc = eucanetd_kick_dhcpd_server(config)) != 0) {
        LOGERROR("unable to (re)configure local dhcpd server: check above log errors for details\n");
        return (1);
    }
    LOGINFO("update_private_ips executed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (0);
}

//!
//! Update the elastic IP artifacts. This will install the NAT rules for each one
//! of them.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre The pGni parameter must not be NULL
//!
//! @post On success, the networking artifacts should be implemented. On failure, the
//!       current state of the system may be left in a non-deterministic state. A
//!       subsequent call to this API may resolve the left over issues.
//!
//! @note
//!
static int update_elastic_ips(globalNetworkInfo * pGni)
{
#define MAX_RULE_LEN               1024

    int slashnet = 0;
    int ret = 0;
    int rc = 0;
    int i = 0;
    int j = 0;
    int found = 0;
    int max_instances = 0;
    u32 nw = 0;
    u32 nm = 0;
    char cmd[EUCA_MAX_PATH] = "";
    char rule[MAX_RULE_LEN] = "";
    char *strptra = NULL;
    char *strptrb = NULL;
    sequence_executor cmds = { {0} };
    gni_cluster *mycluster = NULL;
    gni_node *myself = NULL;
    gni_instance *instances = NULL;
    struct timeval tv = { 0 };

    eucanetd_timer_usec(&tv);
    LOGDEBUG("Updating public IP to private IP mappings.\n");

    // Make sure our given parameter is valid
    if (!pGni) {
        LOGERROR("Failed to update public IP to private IP mappings. Invalid parameters provided.\n");
        return (1);
    }
    // Find our associated CC
    rc = gni_find_self_cluster(pGni, &mycluster);
    if (rc) {
        LOGERROR("cannot locate cluster to which local node belongs, in global network view: check network config settings\n");
        return (1);
    }

    nw = mycluster->private_subnet.subnet;
    nm = mycluster->private_subnet.netmask;

    //    slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetpConfig->networks[0].nm) + 1))));
    slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - nm) + 1))));

    // install EL IP addrs and NAT rules
    rc = ipt_handler_repopulate(config->ipt);
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_PRE_PREUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_PRE", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_PRE_POSTUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_POST_PREUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_POST", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_POST_POSTUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_OUT_PREUSERHOOK", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_OUT", "-", "[0:0]");
    ipt_table_add_chain(config->ipt, "nat", "EUCA_NAT_OUT_POSTUSERHOOK", "-", "[0:0]");

    ipt_chain_add_rule(config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE_PREUSERHOOK");
    ipt_chain_add_rule(config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE");
    ipt_chain_add_rule(config->ipt, "nat", "PREROUTING", "-A PREROUTING -j EUCA_NAT_PRE_POSTUSERHOOK");
    ipt_chain_add_rule(config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST_PREUSERHOOK");
    ipt_chain_add_rule(config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST");
    ipt_chain_add_rule(config->ipt, "nat", "POSTROUTING", "-A POSTROUTING -j EUCA_NAT_POST_POSTUSERHOOK");
    ipt_chain_add_rule(config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT_PREUSERHOOK");
    ipt_chain_add_rule(config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT");
    ipt_chain_add_rule(config->ipt, "nat", "OUTPUT", "-A OUTPUT -j EUCA_NAT_OUT_POSTUSERHOOK");

    //  rc = ipt_handler_print(config->ipt);
    rc = ipt_handler_deploy(config->ipt);
    if (rc) {
        LOGERROR("could not add euca net chains: check above log errors for details\n");
        ret = 1;
    }

    rc = ipt_handler_repopulate(config->ipt);
    rc = se_init(&cmds, config->cmdprefix, 2, 1);

    ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_PRE");
    ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_POST");
    ipt_chain_flush(config->ipt, "nat", "EUCA_NAT_OUT");

    //    strptra = hex2dot(vnetpConfig->networks[0].nw);
    strptra = hex2dot(nw);

    if (config->metadata_use_vm_private) {
        // set a mark so that VM to metadata service requests are not SNATed
        snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_PRE -s %s/%d -d 169.254.169.254/32 -j MARK --set-xmark 0x2a/0xffffffff", strptra, slashnet);
        ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_PRE", rule);
    }

    snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_PRE -s %s/%d -m set --match-set EUCA_ALLPRIVATE dst -j MARK --set-xmark 0x2a/0xffffffff", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_PRE", rule);

    snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_PRE -s %s/%d -m set --match-set EUCA_ALLNONEUCA dst -j MARK --set-xmark 0x2a/0xffffffff", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_PRE", rule);

    snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/%d", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_IN", rule);

    snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/%d", strptra, slashnet);
    ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_OUT", rule);

    EUCA_FREE(strptra);

    rc = gni_find_self_node(pGni, &myself);
    if (!rc) {
        rc = gni_node_get_instances(pGni, myself, NULL, 0, NULL, 0, &instances, &max_instances);
    }

    for (i = 0; i < max_instances; i++) {
        strptra = hex2dot(instances[i].publicIp);
        strptrb = hex2dot(instances[i].privateIp);
        LOGTRACE("instance pub/priv: %s: %s/%s\n", instances[i].name, strptra, strptrb);
        if ((instances[i].publicIp && instances[i].privateIp) && (instances[i].publicIp != instances[i].privateIp)) {
            // run some commands
            rc = se_init(&cmds, config->cmdprefix, 2, 1);

            snprintf(cmd, EUCA_MAX_PATH, "ip addr add %s/%d dev %s >/dev/null 2>&1", strptra, 32, config->pubInterface);
            rc = se_add(&cmds, cmd, NULL, ignore_exit2);

            snprintf(cmd, EUCA_MAX_PATH, "arping -c 5 -w 1 -U -I %s %s >/dev/null 2>&1 &", config->pubInterface, strptra);
            rc = se_add(&cmds, cmd, NULL, ignore_exit);

            se_print(&cmds);
            rc = se_execute(&cmds);
            if (rc) {
                LOGERROR("could not execute command sequence (check above log errors for details): adding ips, sending arpings\n");
                ret = 1;
            }
            se_free(&cmds);

            snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_PRE -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
            rc = ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_PRE", rule);

            snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_OUT -d %s/32 -j DNAT --to-destination %s", strptra, strptrb);
            rc = ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_OUT", rule);

            snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_POST -s %s/32 -m mark ! --mark 0x2a -j SNAT --to-source %s", strptrb, strptra);
            rc = ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_POST", rule);

            //snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -m conntrack --ctstate DNAT --ctorigdst %s/32", strptra);
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/32", strptrb);
            rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_IN", rule);

            //snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -m conntrack --ctstate SNAT --ctrepldst %s/32", strptra);
            snprintf(rule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/32", strptrb);
            rc = ipt_chain_add_rule(config->ipt, "filter", "EUCA_COUNTERS_OUT", rule);

        }

        EUCA_FREE(strptra);
        EUCA_FREE(strptrb);
    }

    // Install the masquerade rules
    if (config->nc_proxy) {
        strptra = hex2dot(mycluster->private_subnet.subnet);
        slashnet = NETMASK_TO_SLASHNET(mycluster->private_subnet.netmask);

        snprintf(rule, 1024, "-A EUCA_NAT_POST -s %s/%u -m mark ! --mark 0x2a -j MASQUERADE", strptra, slashnet);
        ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_POST", rule);
        EUCA_FREE(strptra);
    }

    // lastly, install metadata redirect rule
    if (config->metadata_ip) {
        strptra = hex2dot(config->clcMetadataIP);
    } else {
        strptra = hex2dot(pGni->enabledCLCIp);
    }
    snprintf(rule, MAX_RULE_LEN, "-A EUCA_NAT_PRE -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", strptra);
    rc = ipt_chain_add_rule(config->ipt, "nat", "EUCA_NAT_PRE", rule);
    EUCA_FREE(strptra);

    //  rc = ipt_handler_print(config->ipt);
    rc = ipt_handler_deploy(config->ipt);
    if (rc) {
        LOGERROR("could not apply new ipt handler rules: check above log errors for details\n");
        ret = 1;
    }

    // if all has gone well, now clear any public IPs that have not been mapped to private IPs
    if (!ret) {
        u32 *ips=NULL, *nms=NULL;
        int max_nets;
        
        if (getdevinfo(config->pubInterface, &ips, &nms, &max_nets)) {
            // could not get interface info - only check below against instances
            max_nets = 0;
            ips = NULL;
            nms = NULL;
        }

        for (i = 0; i < pGni->max_public_ips; i++) {
            found = 0;
            // only clear IPs that are not assigned to instances running on this node
            if (!found && max_instances > 0) {
                for (j = 0; j < max_instances && !found; j++) {
                    if (instances[j].publicIp == pGni->public_ips[i]) {
                        // this global public IP is assigned to an instance on this node, do not delete
                        found = 1;
                    }
                }
            }

            // only clear IPs that are assigned on the public interface already
            if (!found && max_nets > 0) {
                found = 1;
                for (j = 0; j < max_nets && found; j++) {
                    if (ips[j] == pGni->public_ips[i]) {
                        // this global public IP is assigned to the public interface currently (but not to an instance) - do the delete
                        found = 0;
                    }
                }
            }

            if (!found) {
                se_init(&cmds, config->cmdprefix, 2, 1);

                strptra = hex2dot(pGni->public_ips[i]);
                snprintf(cmd, EUCA_MAX_PATH, "ip addr del %s/%d dev %s >/dev/null 2>&1", strptra, 32, config->pubInterface);
                EUCA_FREE(strptra);

                rc = se_add(&cmds, cmd, NULL, ignore_exit2);
                se_print(&cmds);
                rc = se_execute(&cmds);
                if (rc) {
                    LOGERROR("could not execute command sequence (check above log errors for details): revoking no longer in use ips\n");
                    ret = 1;
                }
                se_free(&cmds);
            }
        }
        EUCA_FREE(ips);
        EUCA_FREE(nms);
    }
    EUCA_FREE(instances);
    LOGINFO("update_elastic_ips exected in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (ret);

#undef MAX_RULE_LEN
}

//!
//! Application of EBT rules to only allow unique and known IP<->MAC pairings to
//! send traffic through the bridge
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if a failure occurred
//!
//! @see
//!
//! @pre The pGni parameter must not be NULL
//!
//! @post On success, the networking L2 artifacts should be implemented. On failure, the
//!       current state of the system may be left in a non-deterministic state. A
//!       subsequent call to this API may resolve the left over issues.
//!
//! @note
//!
static int update_l2_addressing(globalNetworkInfo * pGni)
{
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
    gni_node *myself = NULL;
    gni_cluster *mycluster = NULL;
    gni_instance *instances = NULL;
    struct timeval tv = { 0 };

    eucanetd_timer_usec(&tv);
    LOGDEBUG("Updating IP/MAC pairing rules.\n");

    // Make sure our given parameter is valid
    if (!pGni) {
        LOGERROR("Failed to update IP/MAC pairing. Invalid parameters provided.\n");
        return (1);
    }

    if (config->nc_proxy) {
#ifdef USE_IP_ROUTE_HANDLER
        // Populate our IP rules
        ipr_handler_repopulate(config->ipr);

        // Flush what we have, we'll re-add if necessary
        ipr_handler_flush(config->ipr);
#endif /* USE_IP_ROUTE_HANDLER */

        // Now update our host information by sending Gratuitous ARP as necessary
        update_host_arp();
    }

    // Find our associated cluster
    rc = gni_find_self_cluster(pGni, &mycluster);
    if (rc) {
        LOGERROR("cannot find cluster to which local node belongs, in global network view: check network config settings\n");
        return (1);
    }

    rc = ebt_handler_repopulate(config->ebt);

    rc = ebt_table_add_chain(config->ebt, "filter", "EUCA_EBT_FWD", "ACCEPT", "");
    rc = ebt_chain_add_rule(config->ebt, "filter", "FORWARD", "-j EUCA_EBT_FWD");
    rc = ebt_chain_flush(config->ebt, "filter", "EUCA_EBT_FWD");

    rc = ebt_table_add_chain(config->ebt, "nat", "EUCA_EBT_NAT_PRE", "ACCEPT", "");
    rc = ebt_chain_add_rule(config->ebt, "nat", "PREROUTING", "-j EUCA_EBT_NAT_PRE");
    rc = ebt_chain_flush(config->ebt, "nat", "EUCA_EBT_NAT_PRE");

    rc = ebt_table_add_chain(config->ebt, "nat", "EUCA_EBT_NAT_POST", "ACCEPT", "");
    rc = ebt_chain_add_rule(config->ebt, "nat", "POSTROUTING", "-j EUCA_EBT_NAT_POST");
    rc = ebt_chain_flush(config->ebt, "nat", "EUCA_EBT_NAT_POST");

    // add these for DHCP to pass
    //    rc = ebt_chain_add_rule(config->ebt, "filter", "EUCA_EBT_FWD", "-p IPv4 -d Broadcast --ip-proto udp --ip-dport 67:68 -j ACCEPT");
    //    rc = ebt_chain_add_rule(config->ebt, "filter", "EUCA_EBT_FWD", "-p IPv4 -d Broadcast --ip-proto udp --ip-sport 67:68 -j ACCEPT");

    rc = gni_find_self_node(pGni, &myself);
    if (!rc) {
        rc = gni_node_get_instances(pGni, myself, NULL, 0, NULL, 0, &instances, &max_instances);
    }

    gwip = hex2dot(config->vmGatewayIP);
    brmac = INTFC2MAC(config->bridgeDev);

    if (gwip && brmac) {
        // Add this one for DHCP to pass since windows may be requesting broadcast responses
        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -o vn_i+ -s %s -d Broadcast --ip-proto udp --ip-dport 67:68 -j ACCEPT", brmac);
        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

        if (!config->nc_proxy) {
            // If we're using the "fake" router option and have some instance running,
            // we need to respond for out of network ARP request.
            if (config->nc_router && !config->nc_router_ip && (max_instances > 0)) {
                snprintf(cmd, EUCA_MAX_PATH, "-i vn_i+ -p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", gwip, brmac);
                rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
            }
        } else {
            // Only allow ARP Reply to our bridge MAC
            snprintf(cmd, EUCA_MAX_PATH, "-p ARP -i vn_i+ -d ! %s --arp-op Reply -j DROP ", brmac);
            rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

            // Ensures only our Bridge can send ARP request to our VMs
            snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o vn_i+ --arp-mac-src ! %s -j DROP", brmac);
            rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);
        }


        for (i = 0; i < max_instances; i++) {
            if (instances[i].privateIp && maczero(instances[i].macAddress)) {
                strptra = strptrb = NULL;
                strptra = hex2dot(instances[i].privateIp);
                hex2mac(instances[i].macAddress, &strptrb);

                // this one is a special case, which only gets identified once the VM is actually running on the hypervisor - need to give it some time to appear
                //                vnetinterface = MAC2INTFC(strptrb);
                snprintf(vnetinterface, 63, "vn_%s", instances[i].name);

                if (strptra && strptrb) {
                    if (!config->disable_l2_isolation) {
                        //NOTE: much of this ruleset is a translation of libvirt FW example at http://libvirt.org/firewall.html

                        // PRE Routing

                        // basic MAC check
                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -s ! %s -j DROP", vnetinterface, strptrb);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        // IPv4
                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -i %s -s %s --ip-proto udp --ip-dport 67:68 -j ACCEPT", vnetinterface, strptrb);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -i %s --ip-src ! %s -j DROP", vnetinterface, strptra);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -i %s -j ACCEPT", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        if (config->nc_proxy) {
                            snprintf(cmd, EUCA_MAX_PATH, "-p ARP -i %s --arp-ip-dst %s -j DROP", vnetinterface, strptra);
                            rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                            snprintf(cmd, EUCA_MAX_PATH, "-p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", strptra, brmac);
                            rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                            // Forces all ARP from VM to get replied with our Bridge MAC (Force forward to the bridge)
                            snprintf(cmd, EUCA_MAX_PATH, "-p ARP -i %s -j arpreply --arpreply-mac %s", vnetinterface, brmac);
                            rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

#ifdef USE_IP_ROUTE_HANDLER
                            // If I don't have a public IP, confine the traffic to the private network routes
                            if (!instances[i].publicIp) {
                                snprintf(cmd, EUCA_MAX_PATH, "from %s lookup euca_private", strptra);
                            } else {
                                snprintf(cmd, EUCA_MAX_PATH, "from %s lookup euca_public", strptra);
                            }
                            rc = ipr_handler_add_rule(config->ipr, cmd);
#endif /* USE_IP_ROUTE_HANDLER */
                        }

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-mac-src ! %s -j DROP", vnetinterface, strptrb);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-ip-src ! %s -j DROP", vnetinterface, strptra);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-op Request -j ACCEPT", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-op Reply -j ACCEPT", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP -j DROP", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        // RARP
                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p 0x8035 -s %s -d Broadcast --arp-op Request_Reverse --arp-ip-src 0.0.0.0 --arp-ip-dst 0.0.0.0 --arp-mac-src %s "
                                 "--arp-mac-dst %s -j ACCEPT", vnetinterface, strptrb, strptrb, strptrb);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p 0x8035 -j DROP", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        // pass KVM migration weird packet
                        snprintf(cmd, EUCA_MAX_PATH, "-i %s -p 0x835 -j ACCEPT", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);

                        // POST routing

                        // IPv4
                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -o %s -d ! %s --ip-proto udp --ip-dport 67:68 -j DROP", vnetinterface, strptrb);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p IPv4 -o %s -j ACCEPT", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        // ARP
                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Reply --arp-mac-dst ! %s -j DROP", vnetinterface, strptrb);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-ip-dst ! %s -j DROP", vnetinterface, strptra);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Request -j ACCEPT", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Request -j ACCEPT", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s --arp-op Reply -j ACCEPT", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p ARP -o %s -j DROP", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        // RARP
                        snprintf(cmd, EUCA_MAX_PATH, "-p 0x8035 -o %s -d Broadcast --arp-op Request_Reverse --arp-ip-src 0.0.0.0 --arp-ip-dst 0.0.0.0 --arp-mac-src %s "
                                 "--arp-mac-dst %s -j ACCEPT", vnetinterface, strptrb, strptrb);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                        snprintf(cmd, EUCA_MAX_PATH, "-p 0x8035 -o %s -j DROP", vnetinterface);
                        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);

                    } else {
                        if (config->nc_router && !config->nc_router_ip) {
                            snprintf(cmd, EUCA_MAX_PATH, "-i %s -p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", vnetinterface, gwip, brmac);
                            rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
                        }
                    }
                } else {
                    LOGWARN("could not retrieve one of: vmip (%s), vminterface (%s), vmmac (%s), gwip (%s), brmac (%s): skipping but will retry\n", SP(strptra), SP(vnetinterface),
                            SP(strptrb), SP(gwip), SP(brmac));
                    ret = 1;
                }
                //                EUCA_FREE(vnetinterface);
                EUCA_FREE(strptra);
                EUCA_FREE(strptrb);
            }
        }
    } else {
        LOGWARN("could not retrieve one of: gwip (%s), brmac (%s): skipping but will retry\n", SP(gwip), SP(brmac));
        ret = 1;
    }

    if (!config->disable_l2_isolation) {
        // DROP everything from the instance by default
        snprintf(cmd, EUCA_MAX_PATH, "-i vn_i+ -j DROP");
        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", cmd);
        
        // DROP everything to the instance by default
        snprintf(cmd, EUCA_MAX_PATH, "-o vn_i+ -j DROP");
        rc = ebt_chain_add_rule(config->ebt, "nat", "EUCA_EBT_NAT_POST", cmd);
    }
    EUCA_FREE(brmac);
    EUCA_FREE(gwip);
    EUCA_FREE(instances);

    rc = ebt_handler_print(config->ebt);
    rc = ebt_handler_deploy(config->ebt);
    if (rc) {
        LOGERROR("could not install ebtables rules: check above log errors for details\n");
        ret = 1;
    }

#ifdef USE_IP_ROUTE_HANDLER
    if (config->nc_proxy) {
        rc = ipr_handler_print(config->ipr);
        rc = ipr_handler_deploy(config->ipr);
        if (rc) {
            LOGERROR("could not install ip rules: check above log errors for details\n");
            ret = 1;
        }
    }
#endif /* USE_IP_ROUTE_HANDLER */

    LOGINFO("update_l2_addressing executed in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);

    return (ret);
}

#ifdef USE_IP_ROUTE_HANDLER
//!
//! Checks whether or not we need to install our public routes. We only update this
//! if the routes have changed.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see
//!
//! @pre
//!     - The pGni parameter MUST not be NULL
//!
//! @post
//!     - On success the public routes have been updated if changed
//!     - If the routes have not changed, then the routes remain
//!     - On failure the results are non-deterministic
//!
//! @note
//!
static int install_public_routes(globalNetworkInfo * pGni)
{
    int i = 0;
    int fd = 0;
    int rc = 0;
    int ret = 0;
    u32 slashnet = 0;
    char *psTmpStr = NULL;
    char *psFileContent = NULL;
    char *psPublicGateway = NULL;
    char *psPrivateSubnet = NULL;
    char *psPrivateGateway = NULL;
    char sCommand[EUCA_MAX_PATH] = "";
    char sPublicRouteFile[EUCA_MAX_PATH] = "";
    boolean found = TRUE;
    gni_subnet *pSubnet = NULL;
    gni_subnet *pMySubnet = NULL;
    gni_cluster *pCluster = NULL;
    gni_cluster *pMyCluster = NULL;
    sequence_executor routeExecutor = { {0} };

    // Make sure the given pointer is valid
    if (!pGni) {
        LOGERROR("Cannot configure public routes. Invalid parameter provided.\n");
        return (1);
    }
    // Find our associated cluster
    if ((rc = gni_find_self_cluster(pGni, &pMyCluster)) != 0) {
        LOGERROR("cannot find the cluster to which the local node belongs: check network config settings\n");
        return (1);
    }
    // Retrieve our subnet
    pMySubnet = &pMyCluster->private_subnet;

    // Create our temporary file to dump the rules when we work them
    snprintf(sPublicRouteFile, EUCA_MAX_PATH, "/tmp/euca_public_route-XXXXXX");
    if ((fd = safe_mkstemp(sPublicRouteFile)) < 0) {
        LOGERROR("cannot create tmpfile '%s': check permissions\n", sPublicRouteFile);
        return (1);
    }
    // Change the permissions on that temporary file
    if (chmod(sPublicRouteFile, 0600)) {
        LOGWARN("chmod failed: was able to create tmpfile '%s', but could not change file permissions\n", sPublicRouteFile);
    }
    // Now close this file descriptor
    close(fd);

    // Now save our route table for analysis
    snprintf(sCommand, EUCA_MAX_PATH, "%s ip route list table euca_public > %s", config->cmdprefix, sPublicRouteFile);
    rc = system(sCommand);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("ip public route save failed '%s'\n", sCommand);
        unlink(sPublicRouteFile);
        return (1);
    }

    // Now load the file content
    if ((psFileContent = file2str(sPublicRouteFile)) == NULL) {
        LOGERROR("Loading public route file '%s' failed.\n", sPublicRouteFile);
        unlink(sPublicRouteFile);
        return (1);
    }

    // We're done with the file now
    unlink(sPublicRouteFile);

    // Convert our public gateway
    if ((psPublicGateway = hex2dot(pGni->publicGateway)) == NULL) {
        LOGERROR("Failed to converd public gateway 0x%08x to string.\n", pGni->publicGateway);
        return (1);
    }

    // Convert our private subnet
    if ((psPrivateSubnet = hex2dot(pMySubnet->subnet)) == NULL) {
        LOGERROR("Failed to converd private subnet 0x%08x to string.\n", pMySubnet->subnet);
        EUCA_FREE(psPublicGateway);
        return (1);
    }

    // Convert our private gateway
    if ((psPrivateGateway = hex2dot(pMySubnet->gateway)) == NULL) {
        LOGERROR("Failed to converd private gateway 0x%08x to string.\n", pMySubnet->gateway);
        EUCA_FREE(psPublicGateway);
        EUCA_FREE(psPrivateSubnet);
        return (1);
    }

    // First check if we have our default gateway
    if (strstr(psFileContent, psPublicGateway) != NULL) {
        // Now check if we have all of the cluster's subnet present
        for (i = 0, pCluster = pGni->clusters; ((i < pGni->max_clusters) && found); i++, pCluster++) {
            if ((psTmpStr = hex2dot(pCluster->private_subnet.subnet)) != NULL) {
                if (strstr(psFileContent, psTmpStr) == NULL) {
                    found = FALSE;
                }

                EUCA_FREE(psTmpStr);
            }
        }

        // Now check for the global subnet's presence if all other cluster subnets were present
        if (found) {
            for (i = 0, pSubnet = pGni->subnets; ((i < pGni->max_subnets) && found); i++, pSubnet++) {
                if ((psTmpStr = hex2dot(pSubnet->subnet)) != NULL) {
                    if (strstr(psFileContent, psTmpStr) == NULL) {
                        found = FALSE;
                    }

                    EUCA_FREE(psTmpStr);
                }
            }
        }
    } else {
        found = FALSE;
    }

    // We're now done with our old routes
    EUCA_FREE(psFileContent);

    // If we did not find any of the subnet's gateway, flush and rebuild
    if (!found) {
        // Initialize our sequence
        se_init(&routeExecutor, config->cmdprefix, 2, 1);

        // First, flush our routing table
        snprintf(sCommand, EUCA_MAX_PATH, "%s ip route flush table euca_public", config->cmdprefix);
        se_add(&routeExecutor, sCommand, NULL, ignore_exit2);

        // Add our default gateway
        snprintf(sCommand, EUCA_MAX_PATH, "%s ip route add default via %s dev %s table euca_public", config->cmdprefix, psPublicGateway, config->pubInterface);
        se_add(&routeExecutor, sCommand, NULL, ignore_exit2);

        // Take care of our own subnet
        slashnet = NETMASK_TO_SLASHNET(pMySubnet->netmask);
        snprintf(sCommand, EUCA_MAX_PATH, "%s ip route add %s/%u dev %s table euca_public", config->cmdprefix, psPrivateSubnet, slashnet, config->privInterface);
        se_add(&routeExecutor, sCommand, NULL, ignore_exit2);

        // Then add our known subnets
        for (i = 0, pSubnet = pGni->subnets; i < pGni->max_subnets; i++, pSubnet++) {
            // Turn the subnet to string
            if ((psTmpStr = hex2dot(pSubnet->subnet)) != NULL) {
                // Retrieve our slashnet
                slashnet = NETMASK_TO_SLASHNET(pSubnet->netmask);

                // Now add the rule
                snprintf(sCommand, EUCA_MAX_PATH, "%s ip route add %s/%u via %s dev %s table euca_public", config->cmdprefix, psTmpStr, slashnet, psPrivateGateway, config->privInterface);
                se_add(&routeExecutor, sCommand, NULL, ignore_exit2);
                EUCA_FREE(psTmpStr);
            } else {
                LOGWARN("Fail to convert subnet '0x%08x'!", pSubnet->subnet);
                ret = 1;
            }
        }

        // Finally add our clusters subnet
        for (i = 0, pCluster = pGni->clusters; i < pGni->max_clusters; i++, pCluster++) {
            // We already took care of ourselves
            if (pCluster == pMyCluster) {
                continue;
            }

            // Turn the subnet to string
            if ((psTmpStr = hex2dot(pCluster->private_subnet.subnet)) != NULL) {
                // Retrieve our slashnet
                slashnet = NETMASK_TO_SLASHNET(pCluster->private_subnet.netmask);

                // Now add the rule
                snprintf(sCommand, EUCA_MAX_PATH, "%s ip route add %s/%u via %s dev %s table euca_public", config->cmdprefix, psTmpStr, slashnet, psPrivateGateway, config->privInterface);
                se_add(&routeExecutor, sCommand, NULL, ignore_exit2);
                EUCA_FREE(psTmpStr);
            } else {
                LOGWARN("Fail to convert subnet '0x%08x'!", pSubnet->subnet);
                ret = 1;
            }
        }

        // Now try pushing what we have
        se_print(&routeExecutor);
        if ((rc = se_execute(&routeExecutor)) != 0) {
            LOGERROR("could not execute command sequence (check above log errors for details): ip public route.\n");
            ret = 1;
        }
        se_free(&routeExecutor);

    }

    EUCA_FREE(psPublicGateway);
    EUCA_FREE(psPrivateSubnet);
    EUCA_FREE(psPrivateGateway);
    return (ret);
}

//!
//! Checks whether or not we need to install our private routes. We only update this
//! if the routes have changed.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see
//!
//! @pre
//!     - The pGni parameter MUST not be NULL
//!
//! @post
//!     - On success the public routes have been updated if changed
//!     - If the routes have not changed, then the routes remain
//!     - On failure the results are non-deterministic
//!
//! @note
//!
static int install_private_routes(globalNetworkInfo * pGni)
{
    int fd = 0;
    int rc = 0;
    int ret = 0;
    u32 slashnet = 0;
    char *psSubnet = NULL;
    char *psGateway = NULL;
    char *psFileContent = NULL;
    char sCommand[EUCA_MAX_PATH] = "";
    char sPrivateRouteFile[EUCA_MAX_PATH] = "";
    boolean found = TRUE;
    gni_subnet *pSubnet = NULL;
    gni_cluster *pCluster = NULL;
    sequence_executor routeExecutor = { {0} };

    // Make sure the given pointer is valid
    if (!pGni) {
        LOGERROR("Cannot configure private routes. Invalid parameter provided.\n");
        return (1);
    }
    // Find our associated cluster
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("cannot find the cluster to which the local node belongs: check network config settings\n");
        return (1);
    }
    // Retrieve our subnet
    pSubnet = &pCluster->private_subnet;

    // Create our temporary file to dump the rules when we work them
    snprintf(sPrivateRouteFile, EUCA_MAX_PATH, "/tmp/euca_private_route-XXXXXX");
    if ((fd = safe_mkstemp(sPrivateRouteFile)) < 0) {
        LOGERROR("cannot create tmpfile '%s': check permissions\n", sPrivateRouteFile);
        return (1);
    }
    // Change the permissions on that temporary file
    if (chmod(sPrivateRouteFile, 0600)) {
        LOGWARN("chmod failed: was able to create tmpfile '%s', but could not change file permissions\n", sPrivateRouteFile);
    }
    // Now close this file descriptor
    close(fd);

    // Now save our route table for analysis
    snprintf(sCommand, EUCA_MAX_PATH, "%s ip route list table euca_private > %s", config->cmdprefix, sPrivateRouteFile);
    rc = system(sCommand);
    rc = rc >> 8;
    if (rc) {
        LOGERROR("ip private route save failed '%s'\n", sCommand);
        unlink(sPrivateRouteFile);
        return (1);
    }

    // Now load the file content
    if ((psFileContent = file2str(sPrivateRouteFile)) == NULL) {
        LOGERROR("Loading private route file '%s' failed.\n", sPrivateRouteFile);
        unlink(sPrivateRouteFile);
        return (1);
    }

    // We're done with the file now
    unlink(sPrivateRouteFile);

    // Convert our gateway IP address to string representation
    if ((psGateway = hex2dot(pSubnet->gateway)) == NULL) {
        LOGERROR("invalid gateway IP '%u' for cluster '%s'.\n", pSubnet->gateway, pCluster->name);
        return (1);
    }

    // Convert our subnet IP address to string representation
    if ((psSubnet = hex2dot(pSubnet->subnet)) == NULL) {
        LOGERROR("invalid subnet '%u' for cluster '%s'.\n", pSubnet->subnet, pCluster->name);
        EUCA_FREE(psGateway);
        return (1);
    }

    // First check if we can find our default private gateway and then check for our private subnet declaration
    if (strstr(psFileContent, psGateway) == NULL) {
        found = FALSE;
    } else if (strstr(psFileContent, psSubnet) == NULL) {
        found = FALSE;
    }

    // We're now done with our old routes
    EUCA_FREE(psFileContent);

    // If we didn't find it, then flush and add this default gateway
    if (!found) {
        // Retrieve our slashnet
        slashnet = NETMASK_TO_SLASHNET(pSubnet->netmask);

        // Initialize our sequence
        se_init(&routeExecutor, config->cmdprefix, 2, 1);

        // flush our routing table first
        snprintf(sCommand, EUCA_MAX_PATH, "%s ip route flush table euca_private", config->cmdprefix);
        se_add(&routeExecutor, sCommand, NULL, ignore_exit2);

        // Add our default gateway
        snprintf(sCommand, EUCA_MAX_PATH, "%s ip route add default via %s dev %s table euca_private", config->cmdprefix, psGateway, config->privInterface);
        se_add(&routeExecutor, sCommand, NULL, ignore_exit2);

        // finally add our subnet to the mix so we can ping from one VM to another on the same bridge
        snprintf(sCommand, EUCA_MAX_PATH, "%s ip route add %s/%u dev %s table euca_private", config->cmdprefix, psSubnet, slashnet, config->privInterface);
        se_add(&routeExecutor, sCommand, NULL, ignore_exit2);

        // Now try pushing what we have
        se_print(&routeExecutor);
        if ((rc = se_execute(&routeExecutor)) != 0) {
            LOGERROR("could not execute command sequence (check above log errors for details): ip private route.\n");
            ret = 1;
        }
        se_free(&routeExecutor);
    }

    EUCA_FREE(psSubnet);
    EUCA_FREE(psGateway);
    return (ret);
}
#endif /* USE_IP_ROUTE_HANDLER */

//!
//! Go through the list of instances that we are managing on this node and
//! send a gratuitous ARP for the newly created instances only
//!
//! @return 0 on success or 1 if a failure occurred
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static int update_host_arp(void)
{
#ifdef USE_EUCA_ARP
    int i = 0;
    int rc = 0;
    u8 aHexOut[ENET_BUF_SIZE] = { 0 };
    int bridgeMacLen = 0;
    int maxInstances = 0;
    char *psTrimMac = NULL;
    char *psBridgeMac = NULL;
    char *psPrivateIp = NULL;
    char sRule[EUCA_MAX_PATH] = "";
    char sCommand[EUCA_MAX_PATH] = "";
    gni_node *pNode = NULL;
    gni_instance *pInstances = NULL;
    sequence_executor arpExecutor = { {0} };

    LOGDEBUG("updating ARP rules for peers\n");

    rc = ebt_handler_repopulate(config->ebt);
    if ((rc = gni_find_self_node(pGni, &pNode)) == 0) {
        if ((rc = se_init(&arpExecutor, config->cmdprefix, 2, 1)) != 0) {
            LOGERROR("Failed to initialize our sequence executor!\n");
            return (1);
        }

        if ((rc = gni_node_get_instances(pGni, pNode, NULL, 0, NULL, 0, &pInstances, &maxInstances)) == 0) {
            if ((psBridgeMac = INTFC2MAC(config->bridgeDev)) != NULL) {
                if ((bridgeMacLen = strlen(psBridgeMac)) > 0) {
                    // Conver the MAC to a trimmed down version for EB table comparison
                    if (mac2hex(psBridgeMac, aHexOut)) {
                        euca_hex2mac(aHexOut, &psTrimMac, TRUE);

                        // Not sent the gratuitous ARP for each instance with a private IP
                        for (i = 0; i < maxInstances; i++) {
                            if (pInstances[i].privateIp && maczero(pInstances[i].macAddress)) {
                                psPrivateIp = hex2dot(pInstances[i].privateIp);
                                snprintf(sRule, EUCA_MAX_PATH, "-p ARP --arp-ip-dst %s -j arpreply --arpreply-mac %s", psPrivateIp, psTrimMac);
                                if (ebt_chain_find_rule(config->ebt, "nat", "EUCA_EBT_NAT_PRE", sRule) == NULL) {
                                    LOGDEBUG("Sending gratuitous ARP for instance %s IP %s using MAC %s on %s\n", pInstances[i].name, psPrivateIp, psBridgeMac, config->bridgeDev);
                                    snprintf(sCommand, EUCA_MAX_PATH, "/usr/libexec/eucalyptus/announce-arp %s %s %s", config->bridgeDev, psPrivateIp, psBridgeMac);
                                    if ((rc = se_add(&arpExecutor, sCommand, NULL, ignore_exit2)) != 0) {
                                        LOGWARN("Fail to schedule gratuitous ARP for host '%s' using mac '%s'. rc=%d\n", psPrivateIp, psBridgeMac, rc);
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
        }

        // Free our instances
        EUCA_FREE(pInstances);

        // Now try to push what we have
        se_print(&arpExecutor);
        if ((rc = se_execute(&arpExecutor)) != 0) {
            LOGERROR("could not execute command sequence (check above log errors for details): gratuitous ARP.\n");
        }
        se_free(&arpExecutor);

        return (0);
    }

    LOGERROR("cannot find local node in global network view: check network config settings\n");
    return (1);
#else /* USE_EUCA_ARP */
    return (0);
#endif /* USE_EUCA_ARP */
}

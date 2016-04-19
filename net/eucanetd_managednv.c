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
//! @file net/eucanetd_managednv.c
//! Implementation of the MANAGED-NOVLAN Network Driver Interface. This Network Driver
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
#include "eucanetd_config.h"
#include "euca_gni.h"
#include "euca_lni.h"
#include "eucanetd.h"
#include "eucanetd_util.h"
#include "eucanetd_managed.h"

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
//! @name MANAGED-NOVLAN Mode Network Driver APIs
static int network_driver_init(eucanetdConfig * pConfig);
static int network_driver_cleanup(globalNetworkInfo * pGni, boolean forceFlush);
static int network_driver_system_flush(globalNetworkInfo * pGni);
static u32 network_driver_system_scrub(globalNetworkInfo * pGni,
        globalNetworkInfo * pGniApplied, lni_t * pLni);
static int network_driver_implement_network(globalNetworkInfo * pGni, lni_t * pLni);
static int network_driver_implement_sg(globalNetworkInfo * pGni, lni_t * pLni);
static int network_driver_implement_addressing(globalNetworkInfo * pGni, lni_t * pLni);
//! @}

//! @{
//! @name Methods to check wether or not some of the APIs needs to be called
static boolean managednv_has_network_changed(globalNetworkInfo * pGni, lni_t * pLni);
// @}

//! @{
//! @name APIs to work with tunnel network devices
static int managednv_initialize_tunnels(eucanetdConfig * pConfig);
static int managednv_attach_tunnels(globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups);
static int managednv_detach_tunnels(globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups, dev_entry * pTunnels, int nbTunnels);
//! @}

//! @{
//! @name APIs to install/removes elastic IPs as well as subnet gateways on our public/private interface
static int managednv_update_gateway_ips(globalNetworkInfo * pGni);
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to see if the driver has been initialized
#define IS_INITIALIZED()                         ((gInitialized == TRUE) ? TRUE : FALSE)

//! Macro to get the driver name
#define DRIVER_NAME()                            managedNoVlanDriverHandler.name

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              CALLBACK STRUCTURE                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! MANAGED-NOVLAN driver operation handlers
struct driver_handler_t managedNoVlanDriverHandler = {
    .name = NETMODE_MANAGED_NOVLAN,
    .init = network_driver_init,
    .cleanup = network_driver_cleanup,
    .upgrade = NULL,
    .system_flush = network_driver_system_flush,
    .system_maint = NULL,
    .system_scrub = network_driver_system_scrub,
    .implement_network = network_driver_implement_network,
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
//! @param[in] pConfig a pointer to our application configuration
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre
//!     - The core application configuration must be completed prior calling
//!     - The driver must not be already initialized (if its the case, a no-op will occur)
//!     - The pConfig parameter must not be NULL
//!
//! @post
//!     On success the driver is properly configured. On failure, the state of
//!     the driver is non-deterministic. If the driver was previously initialized,
//!     this will result into a no-op.
//!
//! @note
//!     For MANAGED-NOVLAN, this means the following will occur:
//!     - For the NC, we need to execute the following:
//!         - Validate we were provided with a valid bridge interface
//!
//!     - For the CC, we need to execute the following:
//!         - Validate we have a valid DHCP daemon installed
//!         - Validate both Public and Private interfaces were provided and valid
//!         - Setup the IP Table Preload file to use
//!
static int network_driver_init(eucanetdConfig * pConfig)
{
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
    // Setup our tunnel attachment function pointers
    managed_attach_tunnels_fn = managednv_attach_tunnels;
    managed_detach_tunnels_fn = managednv_detach_tunnels;

    if (PEER_IS_NC(eucanetdPeer)) {
        // We need to ensure we were supplied with a valid bridge device
        if (!dev_is_bridge(config->bridgeDev)) {
            LOGERROR("cannot verify bridge device '%s', please check parameters and bridge device.\n", config->bridgeDev);
            return (1);
        }
    } else if (PEER_IS_CC(eucanetdPeer)) {
        // Make sure we can work with the given DHCP Daemon
        if (check_file(config->dhcpDaemon)) {
            LOGERROR("cannot verify VNET_DHCPDAEMON (%s), please check parameter and location\n", config->dhcpDaemon);
            return (1);
        }
        // Ensure our public interface is valid
        if (!dev_exist(config->pubInterface)) {
            LOGERROR("cannot verify VNET_PUBINTERFACE (%s), please check parameter and device name\n", config->pubInterface);
            return (1);
        }
        // Ensure our private interface is valid
        if (!dev_exist(config->privInterface)) {
            LOGERROR("cannot verify VNET_PRIVINTERFACE (%s), please check parameter and device name\n", config->privInterface);
            return (1);
        }
        // setup the IP table preload file path
        snprintf(pConfig->sIptPreload, EUCA_MAX_PATH, EUCALYPTUS_CONF_DIR "/%s", pConfig->eucahome, "iptables-preload");

        // Initialize tunneling as necessary
        if (managednv_initialize_tunnels(pConfig)) {
            LOGERROR("Fail to initialize tunnels. Check vtun packages are installed. Look at above log errors for more details.\n");
            return (1);
        }
    }

    LOGINFO("Network Driver Initialized: networkDriver=%s,\n", DRIVER_NAME());
    LOGINFO("                            servicePeer=%s,\n", PEER2STR(eucanetdPeer));
    LOGINFO("                            dhcpDaemon=%s,\n", config->dhcpDaemon);
    LOGINFO("                            dhcpUser=%s,\n", config->dhcpUser);
    LOGINFO("                            pubInterface=%s,\n", config->pubInterface);
    LOGINFO("                            privInterface=%s,\n", config->privInterface);
    LOGINFO("                            bridgeDev=%s,\n", config->bridgeDev);

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
//! @see network_driver_system_flush()
//!
//! @pre
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
        // Flush our network artifacts
        if (network_driver_system_flush(pGni)) {
            LOGERROR("Fail to flush network artifacts during network driver cleanup. See above log errors for details.\n");
            ret = 1;
        }
        // cleanup our tunnels
        if (managed_cleanup_tunnels(pGni)) {
            LOGERROR("Fail to flush network tunnel artifacts during network driver cleanup. See above log errors for details.\n");
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
//! @pre
//!     - The driver must be initialized already
//!     - The driver must be initialized prior to calling this API.
//!
//! @post
//!     On success, all networking mode artifacts will be flushed from the system. If any
//!     failure occurred. The system is left in a non-deterministic state and a subsequent
//!     call to this API may resolve the remaining issues.
//!
//! @note
//!     For MANAGED-NOVLAN, this means the following will occur on the CC only
//!     - Flushing of all the filter and nat tables chains
//!     - Removal of the elastic IPs from the public interface
//!     - Removal of security-group subnets from the private interface
//!     - Tearing down of tunnels
//!
static int network_driver_system_flush(globalNetworkInfo * pGni)
{
    int i = 0;
    int rc = 0;
    int ret = 0;
    int nbIps = 0;
    in_addr_t ipmask = 0;
    in_addr_t netmask = 0;
    gni_cluster *pCluster = NULL;
    in_addr_entry *pIps = NULL;

    LOGINFO("Flushing '%s' network driver artifacts.\n", DRIVER_NAME());

    // Is our driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to flush the networking artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // What component are we running on?
    if (PEER_IS_CC(eucanetdPeer)) {
        //
        // First, flush all of our IP tables stuff
        //
        rc |= ipt_handler_repopulate(config->ipt);
        rc |= ipt_table_deletechainmatch(config->ipt, IPT_TABLE_FILTER, MANAGED_SG_CHAINNAME_PREFIX);
        rc |= ipt_table_set_chain_policy(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, IPT_CHAIN_POLICY_DEFAULT);
        rc |= ipt_chain_flush(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD);
        rc |= ipt_chain_flush(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_IN);
        rc |= ipt_chain_flush(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_OUT);
        rc |= ipt_chain_flush(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_OUTPUT);
        rc |= ipt_chain_flush(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_PREROUTING);
        rc |= ipt_chain_flush(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_POSTROUTING);
        rc |= ipt_handler_print(config->ipt);
        rc |= ipt_handler_deploy(config->ipt);
        if (rc) {
            LOGERROR("Failed to flush the IP Tables artifact in '%s' networking mode.\n", DRIVER_NAME());
            ret = 1;
        }
        //
        // Then clear our public network of all addresses
        //
        for (i = 0; i < pGni->max_public_ips; i++) {
            dev_remove_ip(config->pubInterface, pGni->public_ips[i], 0xFFFFFFFF);
        }

        // We need to retrieve the cluster configuration for deleting private IPs and tearing down tunnels
        if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
            LOGERROR("Cannot find cluster for our network in global network view: check network configuration settings\n");
            return (1);
        }
        //
        // Then clear our private network of all addresses
        //
        if (dev_get_ips(config->privInterface, &pIps, &nbIps) == 0) {
            netmask = pGni->managedSubnet->netmask;
            ipmask = (pGni->managedSubnet->subnet & netmask);
            for (i = 0; i < nbIps; i++) {
                if ((pIps[i].address & netmask) == ipmask) {
                    dev_remove_ip(config->privInterface, pIps[i].address, pIps[i].netmask);
                }
            }
            dev_free_ips(&pIps);
        }
        //
        // Flush our tunnels
        //
        if ((rc = managed_unset_tunnels(pGni)) != 0) {
            LOGERROR("Fail to flush our tunnels. Look at above log errors for mode details.\n");
            ret = 1;
        }
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
//! @see managednv_has_network_changed(), managed_has_sg_changed(), managed_has_addressing_changed()
//!
//! @pre
//!     - Both pGni and pLni must not be NULL
//!     - The driver must be initialized prior to calling this API.
//!
//! @post
//!
//! @note
//!
static u32 network_driver_system_scrub(globalNetworkInfo * pGni, globalNetworkInfo * pGniApplied, lni_t * pLni)
{
    int i = 0;
    int rc = 0;
    u32 ret = EUCANETD_RUN_NO_API;
    boolean done = FALSE;
    gni_cluster *pCluster = NULL;

    LOGINFO("Scrubbing for '%s' network driver.\n", DRIVER_NAME());

    // Is the driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to scub the system for network artifacts. Driver '%s' not initialized.\n", DRIVER_NAME());
        return (EUCANETD_RUN_NO_API);
    }
    // Are the global and local network view structures NULL?
    if (!pGni || !pLni) {
        LOGERROR("Failed to implement security-group artifacts for '%s' network driver. Invalid parameters provided.\n", DRIVER_NAME());
        return (EUCANETD_RUN_NO_API);
    }
    // We only have something to do on the CC
    if (!PEER_IS_CC(eucanetdPeer)) {
        return (EUCANETD_RUN_NO_API);
    }
    // Initialize our subnet if necessary
    INITIALIZE_SUBNETS(pGni);

    // Check for CC specific stuff
    if (PEER_IS_CC(eucanetdPeer)) {
        // Set our previous cluster ID
        previousClusterId = ((clusterIdChangedSuccessful) ? currentClusterId : previousClusterId);

        // Get the cluster configuration
        if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
            // For any failure force a change
            LOGERROR("Cannot find cluster for our network in global network view: check network configuration settings\n");
            currentClusterId = -1;
            return (EUCANETD_RUN_ALL_API);
        }
        // Set the Cluster ID to use later
        for (i = 0, done = FALSE; ((i < pGni->max_clusters) && !done); i++) {
            if (pCluster->enabledCCIp == pGni->clusters[i].enabledCCIp) {
                // Did our position in the list changed?
                if (currentClusterId != i) {
                    // Yes, remember our old ID, set the new ID and adjust all network config
                    LOGERROR("Cluster ID change detected. New ID=%d, Old ID=%d.\n", i, currentClusterId);
                    if (!clusterIdChangedSuccessful)
                        previousClusterId = currentClusterId;

                    currentClusterId = i;
                    clusterIdChangedSuccessful = TRUE;
                    return (EUCANETD_RUN_ALL_API);
                }
                done = TRUE;
            }
        }
    }
    // Check for any network changes
    if (managednv_has_network_changed(pGni, pLni)) {
        LOGDEBUG("Network artifacts changes detected!\n");
        ret |= EUCANETD_RUN_NETWORK_API;
    }
    // Check for any security-group changes
    if (managed_has_sg_changed(pGni, pLni)) {
        LOGDEBUG("Security-Groups artifacts changes detected!\n");
        ret |= EUCANETD_RUN_SECURITY_GROUP_API;
    }
    // Check for any network addressing changes
    if (managed_has_addressing_changed(pGni, pLni)) {
        LOGDEBUG("Network addressing artifacts changes detected!\n");
        ret |= EUCANETD_RUN_ADDRESSING_API;
    }

    return (ret);
}

//!
//! This takes care of implementing the network artifacts necessary. This will add or
//! remove devices, tunnels, etc. as necessary.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] pLni a pointer to the Local Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see managednv_update_gateway_ips(), managed_setup_metadata_ip()
//!
//! @pre
//!     - Both pGni and pLni must not be NULL
//!     - The driver must have been initialized
//!
//! @post
//!     On success, the networking artifacts should be implemented. On failure, the
//!     current state of the system may be left in a non-deterministic state. A
//!     subsequent call to this API may resolve the remaining issues.
//!
//! @note
//!     For MANAGED-NOVLAN on CC only, this means executing the following:
//!     - Setup our security-groups private subnet gateway addresses on the private interface
//!     - Setup the metadata IP address on the private interface based on the knowledge of the active CLC IP address
//!
static int network_driver_implement_network(globalNetworkInfo * pGni, lni_t * pLni)
{
    int rc = 0;

    LOGINFO("Implementing network artifacts for '%s' network driver.\n", DRIVER_NAME());

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

    if (PEER_IS_CC(eucanetdPeer)) {
        // Install our SG gateway IPs on
        if ((rc = managednv_update_gateway_ips(pGni)) != 0) {
            LOGERROR("Failed to update gateway IP addresses. Check above log errors for details.\n");
            return (1);
        }
        // Then setup our tunnels as necessary
        if ((rc = managed_setup_tunnels(pGni)) != 0) {
            LOGERROR("Failed to update tunneling. Check above log errors for more details\n");
            return (1);
        }
        // Install or remove the metadata IP on our private interface
        if ((rc = managed_setup_metadata_ip(pGni, config->privInterface)) != 0) {
            LOGERROR("Failed to setup metadata IP address on device '%s'. Check above log errors for details.\n", config->privInterface);
            return (1);
        }
    }

    return (0);
}

//!
//! This takes care of implementing the security-group artifacts necessary. This will add or
//! remove networking rules pertaining to the groups and their membership.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] pLni a pointer to the Local Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see managed_setup_sg_filters()
//!
//! @pre
//!     - Both pGni and pLni must not be NULL
//!     - The driver must have been initialized
//!
//! @post
//!     On success, the networking artifacts should be implemented. On failure, the
//!     current state of the system may be left in a non-deterministic state. A
//!     subsequent call to this API may resolve the left over issues.
//!
//! @note
//!     For MANAGED-NOVLAN on the CC only, this means the following needs to occur:
//!     - Setup of the security-groups filters
//!
static int network_driver_implement_sg(globalNetworkInfo * pGni, lni_t * pLni)
{
    LOGINFO("Implementing security-group artifacts for '%s' network driver.\n", DRIVER_NAME());

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
    // Install our security-group filters
    if (managed_setup_sg_filters(pGni)) {
        LOGERROR("Failed to implement security-group filters for '%s' network driver. See above log errors for details.\n", DRIVER_NAME());
        return (1);
    }
    return (0);
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
//! @see managed_generate_dhcpd_config(), eucanetd_kick_dhcpd_server(), managed_setup_addressing(), managed_setup_elastic_ips()
//!
//! @pre
//!     - Both pGni and pLni must not be NULL
//!     - The driver must have been initialized
//!
//! @post
//!     On success, the networking artifacts should be implemented. On failure, the
//!     current state of the system may be left in a non-deterministic state. A
//!     subsequent call to this API may resolve the left over issues.
//!
//! @note
//!     For MANAGED-NOVLAN on the CC only, this means the following needs to occur:
//!     - Setup the DHCP configuration file with the instance's private IP / MAC addresses
//!     - Restart of the DHCP server to pick up the new configuration
//!     - Setup of the instance's private to public IP mapping as necessary
//!     - Setup of the elastic IPs on the public interface
//!
static int network_driver_implement_addressing(globalNetworkInfo * pGni, lni_t * pLni)
{
    int rc = 0;

    LOGINFO("Implementing addressing artifacts for '%s' network driver.\n", DRIVER_NAME());

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
    // This only works on CC
    if (!PEER_IS_CC(eucanetdPeer)) {
        return (0);
    }
    // Generate the DHCP configuration so instances can get their network config
    LOGDEBUG("Generating DHCP configuration.\n");
    if ((rc = managed_generate_dhcpd_config(pGni)) != 0) {
        LOGERROR("unable to generate new dhcp configuration file: check above log errors for details\n");
        return (1);
    }
    // Restart the DHCP server so it can pick up the new configuration
    LOGDEBUG("Restarting DHCP service.\n");
    if ((rc = eucanetd_kick_dhcpd_server(config)) != 0) {
        LOGERROR("unable to (re)configure local dhcpd server: check above log errors for details\n");
        return (1);
    }
    // Setup our private IP to public IP mapping
    LOGDEBUG("Setting up private to public IP mapping.\n");
    if ((rc = managed_setup_addressing(pGni)) != 0) {
        LOGERROR("Could not setup private IP to public IP mapping. Check above log errors for details.\n");
        return (1);
    }
    // Setup our elastic IPs
    LOGDEBUG("Setting up elastic IPs.\n");
    if ((rc = managed_setup_elastic_ips(pGni)) != 0) {
        LOGERROR("Could not update elastic IPs. Check above log errors for details.\n");
        return (1);
    }

    return (0);
}

//!
//! Checks wether or not the new GNI configuration includes some network artifacts changes. This
//! will compare the new GNI structure with our current view of the local system.
//!
//! @param[in] pGni a pointer to the global network view
//! @param[in] pLni a pointer to the local network view
//!
//! @return TRUE if there are any changes to apply otherwise, FALSE is returned
//!
//! @see network_driver_system_scrub()
//!
//! @pre
//!     - By not, pGni and pLni should have been validated by the caller
//!     - caller should have validated that the driver has been initialized
//!     - Both pGni and pLni are the most recent network view
//!
//! @post
//!
//! @note
//!
static boolean managednv_has_network_changed(globalNetworkInfo * pGni, lni_t * pLni)
{
    int i = 0;
    int j = 0;
    int k = 0;
    int rc = 0;
    int nbGroups = 0;
    int subnetIdx = 0;
    int nbInstances = 0;
    boolean found = FALSE;
    boolean hasMetadata = FALSE;
    gni_cluster *pCluster = NULL;
    gni_secgroup *pSecGroup = NULL;
    gni_secgroup *pSecGroups = NULL;
    gni_instance *pInstances = NULL;
    in_addr_entry *pNetwork = NULL;
    managed_subnet *pSubnet = NULL;

    LOGTRACE("Scrubbing network for changes.\n");

    // Network artifacts only applies to CC
    if (!PEER_IS_CC(eucanetdPeer))
        return (FALSE);

    // Get the cluster configuration
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("Cannot find cluster for our network in global network view: check network configuration settings\n");
        return (TRUE);
    }
    // Retrieve our security groups for this cluster only
    if ((rc = gni_cluster_get_secgroup(pGni, pCluster, NULL, 0, NULL, 0, &pSecGroups, &nbGroups)) != 0) {
        LOGERROR("Cannot find security groups for cluster %s in global network view: check network configuration settings\n", pCluster->name);
        return (TRUE);
    }
    // Check for Tunnel device changes
    if (managed_has_tunnel_changed(pGni, pSecGroups, nbGroups)) {
        LOGTRACE("Network change detected! Tunnel mapping change detected.\n");
        EUCA_FREE(pSecGroups);
        return (TRUE);
    }
    //
    // Now check if our gateways IPs are all in sync. We will look through the installed
    // gateways IPs and make sure they have an associated security group and we will then
    // look through our security group and make sure they all have gateways installed.
    //
    for (i = 0, pNetwork = pLni->pNetworks; i < pLni->numberOfNetworks; i++, pNetwork++) {
        // If this network is part of the subnet we care, than look for SG
        if ((subnetIdx = managed_find_subnet_idx_from_gateway(pNetwork->address)) != -1) {
            pSubnet = &gaManagedSubnets[subnetIdx];

            for (j = 0, pSecGroup = pSecGroups, found = FALSE; ((j < nbGroups) && !found); j++, pSecGroup++) {
                // Retrieve the instances for this security group
                if ((rc = gni_secgroup_get_instances(pGni, pSecGroup, NULL, 0, NULL, 0, &pInstances, &nbInstances)) != 0) {
                    // Warn and return TRUE... A failure should constiture a difference.
                    LOGWARN("Failed to retrieve instances for security group '%s'. Check above error for more details.\n", pSecGroup->name);
                    EUCA_FREE(pSecGroups);
                    return (TRUE);
                }
                // Do we have a matching
                for (k = 0; ((k < nbInstances) && !found); k++) {
                    if (subnetIdx == managed_find_subnet_idx(pInstances[k].privateIp)) {
                        found = TRUE;
                    }
                }

                // Free our instances
                EUCA_FREE(pInstances);
            }

            // If we did not find one, we need to get this one removed
            if (!found) {
                LOGTRACE("Network change detected! Did not find associated security-group for subnet %s\n", pNetwork->sHost);
                EUCA_FREE(pSecGroups);
                return (TRUE);
            }
        }
    }

    // Now go through the SGs and see if we have all subnets installed
    for (i = 0, pSecGroup = pSecGroups; i < nbGroups; i++, pSecGroup++) {
        // Retrieve the instances for this security group
        if ((rc = gni_secgroup_get_instances(pGni, pSecGroup, NULL, 0, NULL, 0, &pInstances, &nbInstances)) != 0) {
            // Warn and return TRUE... A failure should constiture a difference.
            LOGWARN("Failed to retrieve instances for security group '%s'. Check above error for more details.\n", pSecGroup->name);
            EUCA_FREE(pSecGroups);
            return (TRUE);
        }
        // Check to make sure this group has its subnet installed
        for (k = 0, found = FALSE; ((k < nbInstances) && !found); k++) {
            if ((subnetIdx = managed_find_subnet_idx(pInstances[k].privateIp)) != -1) {
                pSubnet = &gaManagedSubnets[subnetIdx];
                //
                // Ok, we got one. Now lets find out if we have the security group gateway
                // IP properly installed on the private interface
                //
                for (j = 0, pNetwork = pLni->pNetworks, found = FALSE; ((j < pLni->numberOfNetworks) && !found); j++, pNetwork++) {
                    if (pSubnet->gateway == pNetwork->address) {
                        found = TRUE;
                    }
                }

                if (!found) {
                    LOGTRACE("Network change detected! Did not find assigned subnet for security-group %s\n", pSecGroup->name);
                    EUCA_FREE(pInstances);
                    EUCA_FREE(pSecGroups);
                    return (TRUE);
                }
            }
        }
        EUCA_FREE(pInstances);
    }

    // We're done with security groups
    EUCA_FREE(pSecGroups);

    //
    // Now check for the CLC/Metadata IP on the private interface
    //
    for (i = 0, pNetwork = pLni->pNetworks, hasMetadata = FALSE; ((i < pLni->numberOfNetworks) && !hasMetadata); i++, pNetwork++) {
        if ((pNetwork->address == INADDR_METADATA) && !strcmp(pNetwork->sDevName, config->privInterface)) {
            hasMetadata = TRUE;
        }
    }
    // Did something change?
    if (pGni->enabledCLCIp && !hasMetadata) {
        LOGERROR("Network change detected! New CLC IP learned %s\n", euca_ntoa(pGni->enabledCLCIp));
        return (TRUE);
    }

    if (!pGni->enabledCLCIp && hasMetadata) {
        LOGERROR("Network change detected! Lost CLC IP address.\n");
        return (TRUE);
    }
    // Ok, I guess nothing changed
    return (FALSE);
}

//!
//! This initialize tunneling as necessary. If tunneling is enabled, this function will
//! execure the following tasks:
//!
//!     -# Ensures our configured private interface is a valid bridge device
//!     -# Calls managed_initialize_tunnels() API
//!
//! @param[in] pConfig a pointer to the global configuration structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see managed_initialize_tunnels(), managed_cleanup_tunnels(), managed_setup_tunnels(), managed_unset_tunnels(), managed_attach_tunnel(), managed_detach_tunnel()
//!
//! @pre
//!     - The pConfig parameter must not be NULL
//!     - The configured private interface must be a bridge device
//!     - All pre-conditions from managed_initialize_tunnels() must be met
//!
//! @post
//!     On success, tunneling is initialized properly. On Failure, nothing has changed
//!     on the system.
//!
//! @note
//!
static int managednv_initialize_tunnels(eucanetdConfig * pConfig)
{
    LOGINFO("Initializing tunneling.\n");

    // Make sure our parameters are valid
    if (!pConfig) {
        LOGERROR("Fail to initialize tunneling. Invalid parameters provided.\n");
        return (1);
    }
    // Are we using tunneling???
    if (!pConfig->disableTunnel) {
        if (!dev_is_bridge(pConfig->privInterface)) {
            LOGERROR("Fail to initialize tunneling. Private interface '%s' is n't a valid bridge device. Check your configuration.\n", pConfig->privInterface);
            return (1);
        }

        return (managed_initialize_tunnels(pConfig));
    }

    LOGDEBUG("Tunneling disabled.\n");
    return (0);
}

//!
//! This will attach the tunnels for all active security-groups.
//!
//! @param[in] pGni a pointer to our global network view structure
//! @param[in] pCluster a pointer to our cluster structure
//! @param[in] pSecGroups a pointer this cluster's list of active security groups
//! @param[in] nbGroups number of security groups in the pSecGroups list
//!
//! @return 0 on success or 1 on failure.
//!
//! @see managednv_initialize_tunnels(), managed_initialize_tunnels(), managed_cleanup_tunnels()
//! @see managed_setup_tunnels(), managed_unset_tunnels()
//! @see managed_attach_tunnel()
//! @see managednv_detach_tunnels(), managed_detach_tunnel()
//!
//! @pre
//!     - All of our pointer must not be NULL
//!     - nbGroups should be greater than 0 (or it'll be a no-op)
//!     - Tunneling should be enabled (if not, we're good)
//!
//! @post
//!     On success, every active security-groups will have their tunnel attached. On failure,
//!     none or some of the tunnels will be attached but not all.
//!
//! @note
//!
static int managednv_attach_tunnels(globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups)
{
    int i = 0;
    int j = 0;
    int ret = 0;
    int localId = -1;
    int nbBridges = 0;
    dev_entry *pBridge = NULL;
    gni_secgroup *pSecGroup = NULL;
    managed_subnet *pSubnet = NULL;

    LOGTRACE("Attaching tunnels.\n");

    // Do we have tunneling enabled?
    if (config->disableTunnel == TRUE)
        return (0);

    // Do we have anything to attach?
    if (nbGroups == 0)
        return (0);

    // Make sure our pointers are valid
    if (!pGni || !pCluster || !pSecGroups) {
        LOGERROR("Fail to attach tunnels. Invalid parameters provided.\n");
        return (1);
    }
    // Retrieve our private interface device. It MUST be a bridge device...
    if (dev_get_bridges(config->privInterface, &pBridge, &nbBridges)) {
        LOGWARN("Fail to attach tunnels. Could not retrieve associated bridge device '%s'.\n", config->privInterface);
        return (1);
    } else if (nbBridges != 1) {
        // This should NEVER happen
        LOGERROR("Fail to attach tunnels. Fail to find unique bridge device '%s'.\n", config->privInterface);
        dev_free_list(&pBridge, nbBridges);
        return (1);
    }
    // Retrieve the local tunnel ID from the GNI
    localId = managed_get_new_tunnel_id(pGni, pCluster);

    // Install tunnel for each security group but only if they have instances
    for (i = 0, pSecGroup = pSecGroups; i < nbGroups; i++, pSecGroup++) {
        // Do we have any instances for this security-group?
        if (pSecGroup->max_instances == 0)
            continue;

        // Make sure we find the matching subnet and bridge device
        if ((pSubnet = managed_find_subnet(pGni, pSecGroup)) == NULL) {
            LOGWARN("Fail to attach tunnel for security-group '%s'. No valid subnet found.\n", pSecGroup->name);
            ret = 1;
        } else {
            // Go and attach the tunnel for each clusters
            for (j = 0; j < pGni->max_clusters; j++) {
                if (j != localId) {
                    if (managed_attach_tunnel(pBridge, pSubnet, localId, j)) {
                        LOGERROR("Fail to attach tunnel for security-group '%s'. Check above log errors for more details.\n", pSecGroup->name);
                        ret = 1;
                    }
                }
            }
        }
    }

    dev_free_list(&pBridge, nbBridges);
    return (ret);
}

//!
//! This will detach the tunnels for all inactive security-groups.
//!
//! @param[in] pGni a pointer to our global network view structure
//! @param[in] pCluster a pointer to our cluster structure
//! @param[in] pSecGroups a pointer this cluster's list of active security groups
//! @param[in] nbGroups number of security groups in the pSecGroups list
//! @param[in] pTunnels a pointer to the list of tunnel devices
//! @param[in] nbTunnels the number of devices in the list
//!
//! @return 0 on success or 1 on failure.
//!
//! @see managednv_initialize_tunnels(), managed_initialize_tunnels(), managed_cleanup_tunnels()
//! @see managed_setup_tunnels(), managed_unset_tunnels()
//! @see managednv_attach_tunnels(), managed_attach_tunnel()
//! @see managed_detach_tunnels()
//!
//! @pre
//!     - The pBridge parameter MUST not be null
//!     - If pSubnet is NULL, this mean get rid of all VLAN interface on this bridge
//!     - The tunnels should be setup for this security-group on this bridge device (if not we're good).
//!
//! @post
//!     On success, the tunnels are detached properly on this bridge. On failure, the
//!     tunnels are not detached.
//!
//! @note
//!
static int managednv_detach_tunnels(globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups, dev_entry * pTunnels, int nbTunnels)
{
    int i = 0;
    int j = 0;
    int ret = 0;
    int vlanId = 0;
    int localId = -1;
    int nbBridges = 0;
    boolean done = FALSE;
    boolean found = FALSE;
    boolean remove = FALSE;
    dev_entry *pBridge = NULL;
    managed_subnet *pSubnet = NULL;

    LOGTRACE("Detaching tunnels.\n");

    // Make sure our pointers are valid
    if (!pGni || !pCluster) {
        LOGERROR("Fail to detach tunnels. Invalid parameters provided.\n");
        return (1);
    }
    // Do we have any tunnels to detach?
    if (nbTunnels == 0) {
        return (0);
    }
    // Make sure our security-group are valid
    if ((nbGroups > 0) && !pSecGroups) {
        LOGERROR("Fail to detach tunnels. Invalid security-groups list parameters provided.\n");
        return (1);
    }
    // Make sure our tunnel devices are valid
    if ((nbTunnels > 0) && !pTunnels) {
        LOGERROR("Fail to detach tunnels. Invalid tunnel list parameters provided.\n");
        return (1);
    }
    // Retrieve our private interface device. It MUST be a bridge device...
    if (dev_get_bridges(config->privInterface, &pBridge, &nbBridges)) {
        LOGWARN("Fail to detach tunnels. Could not retrieve associated bridge device '%s'.\n", config->privInterface);
        return (1);
    } else if (nbBridges != 1) {
        // This should NEVER happen
        LOGERROR("Fail to detach tunnels. Fail to find unique bridge device '%s'.\n", config->privInterface);
        dev_free_list(&pBridge, nbBridges);
        return (1);
    }
    // Retrieve the local tunnel ID from the GNI
    localId = managed_get_new_tunnel_id(pGni, pCluster);

    // Are we detaching all tunnels or do we need to pick and choose?
    if (nbGroups == 0) {
        for (i = 0; i < nbTunnels; i++) {
            // Is this a subnet tunnel (i.e. its tap-[id]-[id].[vlan] format rather than just tap-[id]-[id])?
            if (strstr(pTunnels[i].sDevName, ".") != NULL) {
                if (managed_detach_tunnel(pBridge, &pTunnels[i]) != 0) {
                    LOGERROR("Failed to detach tunnel device '%s' from bridge device '%s'. Look at above log errors for more details.\n", pTunnels[i].sDevName, pBridge->sDevName);
                    ret = 1;
                }

                LOGDEBUG("Removing tunnel device '%s' from system.\n", pTunnels[i].sDevName);
                if (dev_remove_vlan_interface(pTunnels[i].sDevName) != 0) {
                    LOGERROR("Failed to remove tunnel device '%s' from the system. Look at above log errors for more details.\n", pTunnels[i].sDevName);
                    ret = 1;
                }
            }
        }
    } else {
        // Only detach tunnels that do not have a matching security-group or if the security-group has 0 instances
        for (i = 0; i < nbTunnels; i++) {
            // Is this a subnet tunnel (i.e. its tap-[id]-[id].[vlan] format rather than just tap-[id]-[id])?
            if ((vlanId = dev_get_vlan_id(pTunnels[i].sDevName)) != -1) {
                // We remove them from the system by default unless we find a valid security-group
                remove = TRUE;

                // Retrieve our subnet
                if ((pSubnet = GET_SUBNET(vlanId)) != NULL) {
                    // Now see if we have a security-group that has 1 or more instances associated with this bridge
                    for (j = 0, found = FALSE, done = FALSE; ((j < nbGroups) && !done); j++) {
                        if (pSubnet == managed_find_subnet(pGni, &pSecGroups[j])) {
                            done = TRUE;
                            if (pSecGroups[j].max_instances > 0) {
                                found = TRUE;
                                remove = FALSE;
                            }
                        }
                    }

                    // If not found, then detach...
                    if (!found) {
                        if (managed_detach_tunnel(pBridge, &pTunnels[i]) != 0) {
                            LOGERROR("Failed to detach tunnel device '%s' from bridge device '%s'. Look at above log errors for more details.\n",
                                     pTunnels[i].sDevName, pBridge->sDevName);
                            ret = 1;
                        }
                    }
                }
                // do we need to remove this one from the system as well?
                if (remove) {
                    LOGDEBUG("Removing tunnel device '%s' from system.\n", pTunnels[i].sDevName);
                    if (dev_remove_vlan_interface(pTunnels[i].sDevName) != 0) {
                        LOGERROR("Failed to remove tunnel device '%s' from the system. Look at above log errors for more details.\n", pTunnels[i].sDevName);
                        ret = 1;
                    }
                }
            }
        }
    }

    dev_free_list(&pBridge, nbBridges);
    return (ret);
}

//!
//! This API updates the private subnets gateway IP address on the private network interface. It will
//! remove any gateway that are no longer in use and adds the one that are still being used. The following
//! tasks will be executed:
//!     -# Remove any unused security-group managed subnet from the private interface
//!     -# Install any active security-group managed subnet on the private interface
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static int managednv_update_gateway_ips(globalNetworkInfo * pGni)
{
    int i = 0;
    int j = 0;
    int k = 0;
    int rc = 0;
    int ret = 0;
    int nbGroups = 0;
    int subnetIdx = 0;
    int nbNetworks = 0;
    int nbInstances = 0;
    boolean found = FALSE;
    gni_cluster *pCluster = NULL;
    gni_instance *pInstances = NULL;
    gni_secgroup *pSecGroup = NULL;
    gni_secgroup *pSecGroups = NULL;
    in_addr_entry *pNetwork = NULL;
    in_addr_entry *pNetworks = NULL;
    managed_subnet *pSubnet = NULL;

    LOGTRACE("Updating network gateway IPs.\n");

    // Are the global and local network view structures NULL?
    if (!pGni) {
        LOGERROR("Failed to setup gateway IPs. Invalid parameters provided.\n");
        return (1);
    }
    // Retrieve this cluster instance
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("Cannot find cluster for our network in global network view: check network configuration settings\n");
        return (1);
    }
    // First remove our gateways we no longer need. Lets start by getting the list of IPs on our private interface
    if (dev_get_ips(config->privInterface, &pNetworks, &nbNetworks)) {
        LOGERROR("Failed to retrieve networks installed on '%s' network device.\n", config->privInterface);
        return (1);
    }
    // Did we get a Cluster ID change?
    if (currentClusterId == previousClusterId) {
        //
        // We didn't so scan our current list of IPs for the ones that are no longer
        // needed for SGs
        //
        for (j = 0, pNetwork = pNetworks; j < nbNetworks; j++, pNetwork++) {
            //
            // We're only interested in addresses that matches our private subnets (there may be
            // others like public IPs or system IPs). These should be our private subnets gateway IPs
            //
            if ((subnetIdx = managed_find_subnet_idx_from_gateway(pNetwork->address)) != -1) {
                pSubnet = &gaManagedSubnets[subnetIdx];

                //
                // Now scan our security group for any instances that match this subnet. If we cannot
                // find any, this means we need to remove this gateway from the private network device
                //
                for (i = 0, found = FALSE; ((i < pGni->max_secgroups) && !found); i++) {
                    // Retrieve the instances for this security group
                    if ((rc = gni_secgroup_get_instances(pGni, &pGni->secgroups[i], NULL, 0, NULL, 0, &pInstances, &nbInstances)) != 0) {
                        //
                        // Warn and keep going... Just in case this was the group, we'll mark this as
                        // the group and set found to TRUE. This will result in leaving the IP on the
                        // private interface longer if we no longer have instances and if we still have
                        // instances, this will.
                        //
                        LOGWARN("Failed to retrieve instances for security group '%s'. Check above error for more details.\n", pGni->secgroups[i].name);
                        found = TRUE;
                        ret = 1;
                    } else {
                        for (k = 0; ((k < nbInstances) && !found); k++) {
                            if (subnetIdx == managed_find_subnet_idx(pInstances[k].privateIp)) {
                                found = TRUE;
                            }
                        }
                    }
                    EUCA_FREE(pInstances);
                }

                //
                // Now that we have scanned all our groups and instances. If none of the existing instances
                // match this subnet, we will remove the gateway.
                //
                if (!found) {
                    if ((rc = dev_remove_ip(config->privInterface, pSubnet->gateway, pSubnet->netmask)) != 0) {
                        LOGERROR("Failed to remove gateway IP %s/%d from network device %s.\n", pSubnet->sGateway, pSubnet->slashNet, config->privInterface);
                        ret = 1;
                    }
                }
            }
        }
    } else {
        //
        // We didn't so scan our current list of IPs for the ones that are no longer
        // needed for SGs
        //
        for (j = 0, pNetwork = pNetworks; j < nbNetworks; j++, pNetwork++) {
            //
            // We're only interested in addresses that matches our private subnets (there may be
            // others like public IPs or system IPs). These should be our private subnets gateway IPs
            //
            if ((subnetIdx = managed_find_subnet_idx_from_gateway(pNetwork->address)) != -1) {
                pSubnet = &gaManagedSubnets[subnetIdx];
                if ((rc = dev_remove_ip(config->privInterface, (pSubnet->gateway + previousClusterId), pSubnet->netmask)) != 0) {
                    LOGERROR("Failed to remove gateway IP %s/%d from network device %s. Cluster ID change.\n", euca_ntoa(pSubnet->gateway + previousClusterId), pSubnet->slashNet,
                             config->privInterface);
                    clusterIdChangedSuccessful = FALSE;
                    ret = 1;
                }
            }
        }
    }

    // Now that we removed addresses, free the list and scan the device again to refresh and work on adding the new gateways
    dev_free_ips(&pNetworks);
    if (dev_get_ips(config->privInterface, &pNetworks, &nbNetworks)) {
        // It worked the first time so we shouldn't get here unless we have some system issues
        LOGERROR("Failed to retrieve networks installed on '%s' network device.\n", config->privInterface);
        return (1);
    }
    // Get the security groups for this cluster only
    if ((rc = gni_cluster_get_secgroup(pGni, pCluster, NULL, 0, NULL, 0, &pSecGroups, &nbGroups)) != 0) {
        LOGERROR("Cannot find security-groups for cluster '%s' in global network view: check network configuration settings\n", pCluster->name);
        dev_free_ips(&pNetworks);
        return (1);
    }
    //
    // Scan the our SGs for the ones we need to install the GW IP on
    // the private interface
    //
    for (i = 0, pSecGroup = pSecGroups; i < nbGroups; i++, pSecGroup++) {
        // Retrieve the instances for this security group
        if ((rc = gni_secgroup_get_instances(pGni, pSecGroup, NULL, 0, NULL, 0, &pInstances, &nbInstances)) != 0) {
            //
            // Warn and keep going... Just in case this was the group, we'll mark this as
            // the group and set found to TRUE. This will result in leaving the IP on the
            // private interface longer if we no longer have instances and if we still have
            // instances, this will.
            //
            LOGWARN("Failed to retrieve instances for security group '%s'. Check above error for more details.\n", pGni->secgroups[i].name);
            ret = 1;
        } else {
            // Scan our instances until we get one with a valid private IP assigned
            for (k = 0, found = FALSE; ((k < nbInstances) && !found); k++) {
                if ((subnetIdx = managed_find_subnet_idx(pInstances[k].privateIp)) != -1) {
                    pSubnet = &gaManagedSubnets[subnetIdx];
                    //
                    // Ok, we got one. Now lets find out if we have the security group gateway
                    // IP properly installed on the private interface
                    //
                    for (j = 0, pNetwork = pNetworks, found = FALSE; ((j < nbNetworks) && !found); j++, pNetwork++) {
                        if (subnetIdx == managed_find_subnet_idx_from_gateway(pNetwork->address)) {
                            found = TRUE;
                        }
                    }

                    if (!found) {
                        //
                        // If the gateway wasn't found on the private interface, we need to add
                        // it so traffic can get through.
                        //
                        if ((rc = dev_move_ip(config->privInterface, (pSubnet->gateway + currentClusterId), pSubnet->netmask, pSubnet->broadcast, SCOPE_GLOBAL)) != 0) {
                            LOGERROR("Failed to install gateway IP %s/%d broadcast %s on network device %s.\n", euca_ntoa(pSubnet->gateway + currentClusterId), pSubnet->slashNet,
                                     pSubnet->sBroadcast, config->privInterface);
                            ret = 1;
                        }

                        if ((rc = dev_up(config->privInterface)) != 0) {
                            LOGERROR("Failed to enable network device %s.\n", config->privInterface)
                                ret = 1;
                        }
                        // Force found to TRUE so we can get our of this loop
                        found = TRUE;
                    }
                }
            }
        }
        EUCA_FREE(pInstances);
    }

    // Free our IPs and security groups
    EUCA_FREE(pSecGroups);
    dev_free_ips(&pNetworks);
    return (ret);
}

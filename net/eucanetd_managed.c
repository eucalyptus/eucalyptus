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
//! @file net/eucanetd_managed.c
//! Implementation of the MANAGED Network Driver Interface. This Network Driver
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
//!  - Implement the network scrub
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

//! @{
//! @name Various VTUND path constant

#define VTUND_APPLICATION                        "vtund"    //!< VTUND application name
#define VTUND_PATH                               "/usr/sbin/" VTUND_APPLICATION //!< VTUND application binaries path
#define VTUND_CONFIG_PATH                        EUCALYPTUS_KEYS_DIR "/vtunall.conf"    //!< Configuration file path
#define VTUND_TEMPLATE_CONFIG_PATH               EUCALYPTUS_DATA_DIR "/vtunall.conf.template"   //!< Template configuration file path
#define VTUND_PASSWORD_PATH                      EUCALYPTUS_KEYS_DIR "/vtunpass"    //!< Path to the VTUND password file
#define VTUND_SERVER_PID_PATH                    EUCALYPTUS_RUN_DIR "/vtund-server.pid"
#define VTUND_CLIENT_PID_PATH                    EUCALYPTUS_RUN_DIR
#define VTUND_CLIENT_PID_FILE_FORMAT             VTUND_CLIENT_PID_PATH "/vtund-client-%d-%d.pid"
#define VTUND_TUNNEL_ID_FILE_FORMAT              EUCALYPTUS_RUN_DIR "/vtund-tunnel-id.conf"

//! @}

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

//! Array containing information about each one of our private subnets
managed_subnet gaManagedSubnets[NB_VLAN_802_1Q] = {
    {0}
};

//! Attach tunnel function pointer so the proper Managed or Managed-NoVlan API equivalent is called
int (*managed_attach_tunnels_fn) (globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups) = NULL;

//! Detach tunnel function pointer so the proper Managed or Managed-NoVlan API equivalent is called
int (*managed_detach_tunnels_fn) (globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups, dev_entry * pTunnels, int nbTunnels) = NULL;

//! Our stored previous cluster ID
char previousClusterId = -1;

//! The current cluster ID in the list of clusters
char currentClusterId = -1;

//! Boolean to indicate if we fail during the cluster ID change. We assume success until someone indicates a failure
boolean clusterIdChangedSuccessful = TRUE;

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
//! @name MANAGED Mode Network Driver APIs
static int network_driver_init(eucanetdConfig * pConfig);
static int network_driver_cleanup(globalNetworkInfo * pGni, boolean forceFlush);
static int network_driver_upgrade(globalNetworkInfo * pGni); // TODO: Needed for 4.2.0 and remove in 4.3.0
static int network_driver_system_flush(globalNetworkInfo * pGni);
static u32 network_driver_system_scrub(globalNetworkInfo * pGni,
        globalNetworkInfo * pGniApplied, lni_t * pLni);
static int network_driver_implement_network(globalNetworkInfo * pGni, lni_t * pLni);
static int network_driver_implement_sg(globalNetworkInfo * pGni, lni_t * pLni);
static int network_driver_implement_addressing(globalNetworkInfo * pGni, lni_t * pLni);
//! @}

//! @{
//! @name Methods to check whether or not some of the APIs needs to be called
static boolean managed_has_network_changed(globalNetworkInfo * pGni, lni_t * pLni);
// @}

//! @{
//! @name Managed mode specific tunnel APIs
static int managed_create_tunnel(gni_cluster * pCluster, const char *psPidFile, const char *psConfigPath, int localId, int remoteId);
static int managed_attach_tunnels(globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups);
static int managed_detach_tunnels(globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups, dev_entry * pTunnels, int nbTunnels);
//! @}

//! @{
//! @name APIs to install/removes subnet bridges/gateways on our private interface
static boolean managed_is_bridge_setup(dev_entry * pBridge);
static int managed_remove_bridge(dev_entry * pBridge);
static int managed_create_bridge(const char *psBridgeName, managed_subnet * pSubnet);
static int managed_setup_bridges(globalNetworkInfo * pGni);
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to see if the driver has been initialized
#define IS_INITIALIZED()                         ((gInitialized == TRUE) ? TRUE : FALSE)

//! Macro to get the driver name
#define DRIVER_NAME()                            managedDriverHandler.name

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              CALLBACK STRUCTURE                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! MANAGED-NOVLAN driver operation handlers
struct driver_handler_t managedDriverHandler = {
    .name = NETMODE_MANAGED,
    .init = network_driver_init,
    .cleanup = network_driver_cleanup,
    .upgrade = network_driver_upgrade,
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
//!     For MANAGED, this means the following will occur:
//!     - For the NC, we need to execute the following:
//!         - Validate we were provided with a valid bridge interface
//!
//!     - For the CC, we need to execute the following:
//!         - Validate we have a valid DHCP daemon installed
//!         - Validate both Public and Private interfaces were provided and valid
//!         - Setup the IP Table Preload file to use
//!

static int network_driver_init(eucanetdConfig * pConfig) {
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
    managed_attach_tunnels_fn = managed_attach_tunnels;
    managed_detach_tunnels_fn = managed_detach_tunnels;

    if (PEER_IS_NC(eucanetdPeer)) {
        // We need to ensure we were supplied with a valid bridge device
        if (!dev_exist(pConfig->bridgeDev)) {
            LOGERROR("cannot verify bridge device '%s', please check parameters and bridge device.\n", pConfig->bridgeDev);
            return (1);
        }
    } else if (PEER_IS_CC(eucanetdPeer)) {
        // Make sure we can work with the given DHCP Daemon
        if (check_file(pConfig->dhcpDaemon)) {
            LOGERROR("cannot verify VNET_DHCPDAEMON (%s), please check parameter and location\n", pConfig->dhcpDaemon);
            return (1);
        }
        // Ensure our public interface is valid
        if (!dev_exist(pConfig->pubInterface)) {
            LOGERROR("cannot verify VNET_PUBINTERFACE (%s), please check parameter and device name\n", pConfig->pubInterface);
            return (1);
        }
        // Ensure our private interface is valid
        if (!dev_exist(pConfig->privInterface)) {
            LOGERROR("cannot verify VNET_PRIVINTERFACE (%s), please check parameter and device name\n", pConfig->privInterface);
            return (1);
        }
        // setup the IP table preload file path
        snprintf(pConfig->sIptPreload, EUCA_MAX_PATH, EUCALYPTUS_CONF_DIR "/%s", pConfig->eucahome, "iptables-preload");

        // Initialize tunneling as necessary
        if (managed_initialize_tunnels(pConfig)) {
            LOGERROR("Fail to initialize tunnels. Check vtun packages are installed. Look at above log errors for more details.\n");
            return (1);
        }
    }

    LOGINFO("Network Driver Initialized: networkDriver=%s,\n", DRIVER_NAME());
    LOGINFO("                            networkMode=%s\n", pConfig->netMode);
    LOGINFO("                            servicePeer=%s,\n", PEER2STR(eucanetdPeer));
    LOGINFO("                            dhcpDaemon=%s,\n", pConfig->dhcpDaemon);
    LOGINFO("                            dhcpUser=%s,\n", pConfig->dhcpUser);
    LOGINFO("                            pubInterface=%s,\n", pConfig->pubInterface);
    LOGINFO("                            privInterface=%s,\n", pConfig->privInterface);
    LOGINFO("                            bridgeDev=%s,\n", pConfig->bridgeDev);

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

static int network_driver_cleanup(globalNetworkInfo * pGni, boolean forceFlush) {
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
//! Upgrades a 4.1.2 system to 4.2.0. We need to move the vn_i* network devices from
//! the old bridge (eucabrVLANID) to the new bridge naming style (sg-xxxxxxxx).
//!
//! @param[in] pGni a pointer to our global network information structure
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see
//!
//! @pre
//!     - The driver must be initialized already
//!     - pGni must not be NULL
//!
//! @post
//!     - On success all eucabrXXX device have been renamed with the proper sg-XXXXXXXX name
//!     - On failure, the system is left in an undetermined state
//!
//! @note
//!
//! @TODO:
//!     Remove this code for 4.2.0+ releases
//!

static int network_driver_upgrade(globalNetworkInfo * pGni) {
#define OLD_BRIDGE_NAME_PREFIX         "eucabr"

    int i = 0;
    int j = 0;
    int nbInstances = 0;
    int nbVnetDevices = 0;
    int nbBridgeDevices = 0;
    char sMac[ENET_ADDR_LEN] = "";
    char *psBridgeName = NULL;
    char *psInstanceId = NULL;
    char *psInstanceMac = NULL;
    boolean found = FALSE;
    gni_node *pNode = NULL;
    dev_entry *pVnetDevices = NULL;
    dev_entry *pBridgeDevices = NULL;
    gni_instance *pInstances = NULL;

    // Is our driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to upgrade the system for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // Is the given pGni NULL?
    if (!pGni) {
        LOGERROR("Failed to upgrade the system for '%s' network driver. GNI not provided.\n", DRIVER_NAME());
        return (1);
    }

    LOGINFO("Upgrading network system for %s network driver.\n", DRIVER_NAME());

    // Upgrade for NC need to search for instances interfaces
    if (PEER_IS_NC(eucanetdPeer)) {
        // Retrieve our node information
        if (gni_find_self_node(pGni, &pNode) != 0) {
            LOGERROR("Upgrade fail: Cannot find node: check network configuration settings\n");
            return (1);
        }
        if (dev_get_list("vn_*", &pVnetDevices, &nbVnetDevices) == 0) {
            // retrieve our instances and process our known instances
            if (gni_node_get_instances(pGni, pNode, NULL, 0, NULL, 0, &pInstances, &nbInstances) == 0) {
                for (i = 0; i < nbInstances; i++) {
                    // Convert the mac address to string
                    snprintf(sMac, ENET_ADDR_LEN, "%s", euca_etoa(pInstances[i].macAddress));

                    // Shortcut to the instance ID and MAC. For the MAC skip the first 2 bytes (e.g. d0:0d:)
                    psInstanceId = pInstances[i].name;
                    psInstanceMac = euca_strtolower(sMac + 6);

                    // Find the matching VNET device
                    for (j = 0, found = FALSE; ((j < nbVnetDevices) && !found); j++) {
                        // Do we have a MAC address match?
                        if (strstr(pVnetDevices[j].sMacAddress, psInstanceMac)) {
                            // Now try to get its associated bridge device and change its name
                            if ((psBridgeName = dev_get_interface_bridge(pVnetDevices[j].sDevName)) != NULL) {
                                // Does the associated bridge have the old name style?
                                if (strstr(psBridgeName, OLD_BRIDGE_NAME_PREFIX)) {
                                    // Then fix it but only if we have a security-group (i.e. check for NULL just in case)
                                    if (pInstances[i].max_secgroup_names > 0) {
                                        LOGDEBUG("Renaming network bridge device '%s' to '%s'.\n", psBridgeName, pInstances[i].secgroup_names[0].name);
                                        if (dev_rename(psBridgeName, pInstances[i].secgroup_names[0].name) != 0) {
                                            LOGWARN("Failed to rename device '%s' to '%s'.\n", psBridgeName, pInstances[i].secgroup_names[0].name);
                                        }
                                    } else {
                                        LOGWARN("Failed to rename bridge device '%s'. No security-group found for instance '%s'\n", psBridgeName, psInstanceId)
                                    }
                                }
                                EUCA_FREE(psBridgeName);
                            }
                            found = TRUE;
                        }
                    }
                }

                // We're done with the instances...
                EUCA_FREE(pInstances);
            } else {
                LOGWARN("Failed to update network for instances. Cannot retrieve instances from the view!\n");
            }
            // Release the memory
            dev_free_list(&pVnetDevices, nbVnetDevices);
        }
        //
        // At this point, we should no longer have bridge devices starting with "eucabr". But just in case
        // do another scan and delete the ones that we have still left behind as long as they no longer
        // have associated devices
        //
        if (dev_get_bridges(OLD_BRIDGE_NAME_PREFIX "*", &pBridgeDevices, &nbBridgeDevices) == 0) {
            for (i = 0; i < nbBridgeDevices; i++) {
                // Do we have any assigned network interface?
                if (!dev_has_bridge_interfaces(pBridgeDevices[i].sDevName)) {
                    // Then we don't need this bridge interface anymore (or at least for now)
                    LOGDEBUG("Removing unused network bridge device '%s'\n", pBridgeDevices[i].sDevName);
                    if (dev_remove_bridge(pBridgeDevices[i].sDevName, TRUE) != 0) {
                        // Log the error and go on
                        LOGERROR("Failed to remove unused bridge device '%s' during upgrade procedure.\n", pBridgeDevices[i].sDevName);
                    }
                    // Done with this. Go on to the next one
                    continue;
                }
            }

            // Release the memory
            dev_free_list(&pBridgeDevices, nbBridgeDevices);
        }
    } else if (PEER_IS_CC(eucanetdPeer)) {
        if (dev_get_bridges(OLD_BRIDGE_NAME_PREFIX "*", &pBridgeDevices, &nbBridgeDevices) == 0) {
            for (i = 0; i < nbBridgeDevices; i++) {
                LOGDEBUG("Removing network bridge device '%s'\n", pBridgeDevices[i].sDevName);
                if (dev_remove_bridge(pBridgeDevices[i].sDevName, TRUE) != 0) {
                    // Log the error and go on
                    LOGERROR("Failed to remove unused bridge device '%s' during upgrade procedure.\n", pBridgeDevices[i].sDevName);
                }
                // Done with this. Go on to the next one
                continue;
            }
        }

        // Release the memory
        dev_free_list(&pBridgeDevices, nbBridgeDevices);
    } else {
        LOGERROR("Running network upgrade for '%s' network driver on unknown component!\n", DRIVER_NAME());
        return (1);
    }

    return (0);

#undef OLD_BRIDGE_NAME_PREFIX
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
//! @note
//!     For MANAGED, we will flush the filter and nat tables chains and remove the security-group
//!     chains on the CC only. On both CC and NC, we will also remove all the bridge devices associated
//!     with any of the security-groups. These bridge devices are identified by their name starting with
//!     "sg-". Finally, all tunnels will be teared down.
//!

static int network_driver_system_flush(globalNetworkInfo * pGni) {
    int i = 0;
    int rc = 0;
    int ret = 0;
    int nbBridges = 0;
    dev_entry *pBridge = NULL;
    dev_entry *pBridges = NULL;

    LOGINFO("Flushing '%s' network driver artifacts.\n", DRIVER_NAME());

    // Is our driver initialized?
    if (!IS_INITIALIZED()) {
        LOGERROR("Failed to flush the networking artifacts for '%s' network driver. Driver not initialized.\n", DRIVER_NAME());
        return (1);
    }
    // For CC we need to flush the IP tables and Public IPs
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

        //
        // Flush our tunnels
        //
        if ((rc = managed_unset_tunnels(pGni)) != 0) {
            LOGERROR("Fail to flush our tunnels. Look at above log errors for mode details.\n");
            ret = 1;
        }
    }
    //
    // For both CC and NC, we will clear the bridge devices as well. For this, we will
    // retrieve our bridge devices. They should all start with "sg-".
    //
    if (dev_get_bridges("sg-*", &pBridges, &nbBridges) == 0) {
        for (i = 0, pBridge = pBridges; i < nbBridges; i++, pBridge++) {
            LOGDEBUG("Flushing bridge device '%s'.\n", pBridge->sDevName);
            if (managed_remove_bridge(pBridge)) {
                LOGWARN("Failed to flush bridge device '%s'.\n", pBridge->sDevName);
                ret = 1;
            }
        }

        // We're done with the bridge device list
        dev_free_list(&pBridges, nbBridges);
    } else {
        LOGERROR("Failed to flush bridged network devices.\n");
        ret = 1;
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
//! @see managed_has_network_changed(), managed_has_sg_changed(), managed_has_addressing_changed()
//!
//! @pre
//!     - Both pGni and pLni must not be NULL
//!     - The driver must be initialized prior to calling this API.
//!
//! @post
//!
//! @note
//!

static u32 network_driver_system_scrub(globalNetworkInfo * pGni, globalNetworkInfo * pGniApplied, lni_t * pLni) {
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
    // Initialize our subnet if necessary
    INITIALIZE_SUBNETS(pGni);

    // Check for CC specific stuff
    if (PEER_IS_CC(eucanetdPeer)) {
        // Set our previous cluster ID
        previousClusterId = currentClusterId;

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
                    currentClusterId = i;
                    return (EUCANETD_RUN_ALL_API);
                }
                done = TRUE;
            }
        }
    }
    // Check for any network changes
    if (managed_has_network_changed(pGni, pLni)) {
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
    // TODO: Why do we always set the network bit?
    return (ret | EUCANETD_RUN_NETWORK_API);
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
//! @see managed_setup_bridges(), managed_setup_metadata_ip()
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
//!     For MANAGED, the following will be executed:
//!     - On CC only
//!         - Setup of the security-groups tunnel devices as needed
//!         - Setup of the security-groups bridges devices
//!         - Installation of the security-groups private subnet gateway on the associated bridge device
//!         - Setup of the metadata IP on the private interface
//!
//!     - On NC only
//!         - Setup of the security-groups bridges devices
//!

static int network_driver_implement_network(globalNetworkInfo * pGni, lni_t * pLni) {
    int rc = 0;

    LOGTRACE("Implementing network artifacts for '%s' network driver.\n", DRIVER_NAME());

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
        // Setup our bridge devices first
        if ((rc = managed_setup_bridges(pGni)) != 0) {
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
    } else if (PEER_IS_NC(eucanetdPeer)) {
        // setup our bridge devices
        if ((rc = managed_setup_bridges(pGni)) != 0) {
            LOGERROR("Failed to update gateway IP addresses. Check above log errors for details.\n");
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
//!     For MANAGED on the CC only, this means the following needs to occur:
//!     - Setup of the security-groups filters
//!

static int network_driver_implement_sg(globalNetworkInfo * pGni, lni_t * pLni) {
    LOGTRACE("Implementing security-group artifacts for '%s' network driver.\n", DRIVER_NAME());

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
//!     For MANAGED on the CC only, this means the following needs to occur:
//!     - Setup the DHCP configuration file with the instance's private IP / MAC addresses
//!     - Restart of the DHCP server to pick up the new configuration
//!     - Setup of the instance's private to public IP mapping as necessary
//!     - Setup of the elastic IPs on the public interface
//!

static int network_driver_implement_addressing(globalNetworkInfo * pGni, lni_t * pLni) {
    int rc = 0;

    LOGTRACE("Implementing addressing artifacts for '%s' network driver.\n", DRIVER_NAME());

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
    if ((rc = managed_setup_addressing(pGni)) != 0) {
        LOGERROR("Could not setup private IP to public IP mapping. Check above log errors for details.\n");
        return (1);
    }
    // Setup our elastic IPs
    if ((rc = managed_setup_elastic_ips(pGni)) != 0) {
        LOGERROR("Could not update elastic IPs. Check above log errors for details.\n");
        return (1);
    }

    return (0);
}

//!
//! Checks wether or not the new GNI configuration includes some network changes. This
//! will compare the new GNI structure with our current view of the system.
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
//!     - Caller should have validated that the driver has been initialized
//!     - Both pGni and pLni are the most recent network view
//!
//! @post
//!
//! @note TODO: Implement
//!

static boolean managed_has_network_changed(globalNetworkInfo * pGni, lni_t * pLni) {
    LOGTRACE("Scrubbing network for changes.\n");
    return (TRUE);
}

//!
//! Checks wether or not the new GNI configuration includes some security-group artifacts changes. This
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
//!     - Caller should have validated that the driver has been initialized
//!     - Both pGni and pLni are the most recent network view
//!
//! @post
//!
//! @note
//!

boolean managed_has_sg_changed(globalNetworkInfo * pGni, lni_t * pLni) {
#define MAX_RULE_LEN      1024

    int i = 0;
    int j = 0;
    int k = 0;
    int l = 0;
    int rc = 0;
    int ret = 0;
    int nbGroups = 0;
    int subnetIdx = 0;
    int nbInstances = 0;
    char sRule[MAX_RULE_LEN] = "";
    char *pStra = NULL;
    boolean found = FALSE;
    ipt_table *pTable = NULL;
    gni_cluster *pCluster = NULL;
    gni_secgroup *pSecGroup = NULL;
    gni_secgroup *pPeerGroup = NULL;
    gni_secgroup *pSecGroups = NULL;
    gni_instance *pInstances = NULL;
    managed_subnet *pPeerSubnet = NULL;
    u32 cidrnm = 0;

    LOGTRACE("Scrubbing security-groups for changes.\n");

    // This only applies to CC
    if (!PEER_IS_CC(eucanetdPeer))
        return (FALSE);

    // Get the cluster configuration
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        // For any failure force a change
        LOGERROR("Cannot find cluster for our network in global network view: check network configuration settings\n");
        return (TRUE);
    }
    // Retrieve our security groups for this cluster only
    if ((rc = gni_cluster_get_secgroup(pGni, pCluster, NULL, 0, NULL, 0, &pSecGroups, &nbGroups)) != 0) {
        LOGERROR("Cannot find security groups for cluster %s in global network view: check network configuration settings\n", pCluster->name);
        return (TRUE);
    }
    // Now go through all our SGs and check to make sure our rules are installed properly
    for (i = 0, pSecGroup = pSecGroups; i < nbGroups; i++, pSecGroup++) {
        if ((rc = gni_secgroup_get_instances(pGni, pSecGroup, NULL, 0, NULL, 0, &pInstances, &nbInstances)) != 0) {
            LOGERROR("Cannot retrieve instances for security-group '%s'\n", pSecGroup->name);
            return (TRUE);
        }
        // For an instance in this SG with a valid private IP, find the subnet index
        for (j = 0, subnetIdx = -1; ((j < nbInstances) && (subnetIdx == -1)); j++) {
            if (pInstances[j].privateIp) {
                subnetIdx = managed_find_subnet_idx(pInstances[j].privateIp);
            }
        }

        // Now, did we find a network for this group?
        if (subnetIdx >= 0) {
            // Check the jump rule is there
            snprintf(sRule, MAX_RULE_LEN, "-A FORWARD -j %s", pSecGroup->name);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, sRule) == NULL) ? 1 : 0);

            // Check the in-private subnet forwarding rule
            snprintf(sRule, MAX_RULE_LEN, "-A FORWARD -s %s/%u -d %s/%u -j ACCEPT", gaManagedSubnets[subnetIdx].sSubnet,
                    gaManagedSubnets[subnetIdx].slashNet, gaManagedSubnets[subnetIdx].sSubnet, gaManagedSubnets[subnetIdx].slashNet);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, sRule) == NULL) ? 1 : 0);

            // then check the group specific IPT rules (temporary one here)
            if (pSecGroup->max_grouprules) {
                for (j = 0; j < pSecGroup->max_grouprules; j++) {
                    // are we authorizing 1 group into another?
                    if (strlen(pSecGroup->ingress_rules[j].groupId) == 0) {
                        // This is CIDR-based rule
                        cidrnm = (u32) 0xffffffff << (32 - pSecGroup->ingress_rules[j].cidrSlashnet);
                        if (pSecGroup->ingress_rules[j].cidrSlashnet != 0) {
                            // Search for public IPs affected by EUCA-11476
                            for (k = 0; k < pGni->max_instances; k++) {
                                for (l = 0, found = 0; (l < nbInstances) && !found; l++) {
                                    // Skip instances in this security group
                                    if (pInstances[l].publicIp == pGni->instances[k]->publicIp) {
                                        found = 1;
                                    }
                                }
                                if (!found && ((pGni->instances[k]->publicIp & cidrnm) == (pSecGroup->ingress_rules[j].cidrNetaddr & cidrnm)) &&
                                        ((pGni->instances[k]->privateIp & cidrnm) != (pSecGroup->ingress_rules[j].cidrNetaddr & cidrnm))) {
                                    pStra = hex2dot(pGni->instances[k]->privateIp);
                                    LOGTRACE("Found instance private IP (%s) affected by EUCA-11476.\n", pStra);
                                    ingress_gni_to_iptables_rule(pStra, &(pSecGroup->ingress_rules[j]), sRule, 2);
                                    EUCA_FREE(pStra);
                                    pStra = strdup(sRule);
                                    snprintf(sRule, MAX_RULE_LEN, "-A %s -d %s/%u %s -j ACCEPT", pSecGroup->name, gaManagedSubnets[subnetIdx].sSubnet,
                                            gaManagedSubnets[subnetIdx].slashNet, pStra);
                                    LOGTRACE("Checking iptables rule: %s\n", sRule);
                                    ret |= ((ipt_chain_find_rule(config->ipt, IPT_TABLE_FILTER, pSecGroup->name, sRule) == NULL) ? 1 : 0);
                                    EUCA_FREE(pStra);
                                }
                            }
                            ingress_gni_to_iptables_rule(NULL, &(pSecGroup->ingress_rules[j]), sRule, 4);
                            pStra = strdup(sRule);
                            snprintf(sRule, MAX_RULE_LEN, "-A %s -d %s/%u %s -j ACCEPT", pSecGroup->name, gaManagedSubnets[subnetIdx].sSubnet,
                                    gaManagedSubnets[subnetIdx].slashNet, pStra);
                            EUCA_FREE(pStra);
                        } else {
                            ingress_gni_to_iptables_rule(NULL, &(pSecGroup->ingress_rules[j]), sRule, 0);
                            pStra = strdup(sRule);
                            snprintf(sRule, MAX_RULE_LEN, "-A %s -d %s/%u %s -j ACCEPT", pSecGroup->name, gaManagedSubnets[subnetIdx].sSubnet,
                                    gaManagedSubnets[subnetIdx].slashNet, pStra);
                            EUCA_FREE(pStra);
                        }
                        ret |= ((ipt_chain_find_rule(config->ipt, IPT_TABLE_FILTER, pSecGroup->name, sRule) == NULL) ? 1 : 0);
                    } else if (gni_find_secgroup(pGni, pSecGroup->ingress_rules[j].groupId, &pPeerGroup) == 0) {
                        // Now find the subnet for this security group
                        if ((pPeerSubnet = managed_find_subnet(pGni, pPeerGroup)) != NULL) {
                            // Private IPs of this security group are authorized, so EUCA-11476 does not manifest
                            ingress_gni_to_iptables_rule(NULL, &(pSecGroup->ingress_rules[j]), sRule, 0);
                            pStra = strdup(sRule);
                            snprintf(sRule, MAX_RULE_LEN, "-A %s -s %s/%u -d %s/%u %s -j ACCEPT", pSecGroup->name, pPeerSubnet->sSubnet, pPeerSubnet->slashNet,
                                    gaManagedSubnets[subnetIdx].sSubnet, gaManagedSubnets[subnetIdx].slashNet, pStra);
                            EUCA_FREE(pStra);
                            /*
                            snprintf(sRule, MAX_RULE_LEN, "-A %s -s %s/%u -d %s/%u %s -j ACCEPT", pSecGroup->name, pPeerSubnet->sSubnet, pPeerSubnet->slashNet,
                                    gaManagedSubnets[subnetIdx].sSubnet, gaManagedSubnets[subnetIdx].slashNet, pSecGroup->grouprules[j].name);
                             */
                            ret |= ((ipt_chain_find_rule(config->ipt, IPT_TABLE_FILTER, pSecGroup->name, sRule) == NULL) ? 1 : 0);
                        }
                    }
                }
            }
            // Do we have any missing rules?
            if (ret) {
                LOGTRACE("Security-Group change detected!. Missing IPT rules for group '%s'.\n", pSecGroup->name);
                EUCA_FREE(pSecGroups);
                EUCA_FREE(pInstances);
                return (TRUE);
            }
        }
        EUCA_FREE(pInstances);
    }

    // Get the filter table
    if ((pTable = ipt_handler_find_table(pLni->pIpTables, IPT_TABLE_FILTER)) == NULL) {
        LOGERROR("Cannot find filter table...\n");
        return (TRUE);
    }
    // Now scan all our IPT chains starting with 'sg-' and make sure we have a matching group
    for (i = 0; i < pTable->max_chains; i++) {
        // is this an SG table?
        if (!strncmp(pTable->chains[i].name, MANAGED_SG_CHAINNAME_PREFIX, 3)) {
            for (j = 0, pSecGroup = pSecGroups, found = FALSE; ((j < nbGroups) && !found); j++, pSecGroup++) {
                if (!strcmp(pSecGroup->name, pTable->chains[i].name))
                    found = TRUE;
            }

            if (!found) {
                LOGTRACE("Security-Group change detected!. Cannot find security-group for table '%s'.\n", pLni->pIpTables->tables[i].name);
                EUCA_FREE(pSecGroups);
                return (TRUE);
            }
        }
    }
    EUCA_FREE(pSecGroups);
    return (FALSE);

#undef MAX_RULE_LEN
}

//!
//! Checks wether or not the new GNI configuration includes some network addressing artifacts changes. This
//! will compare the new GNI structure with our current view of the local system.
//!
//! @param[in] pGni a pointer to the global network view
//! @param[in] pLni a pointer to the local network view
//!
//! @return TRUE if there are any changes to apply otherwise, FALSE is returned
//!
//! @see network_driver_system_scrub()
//!
//! @pre -
//!     - By not, pGni and pLni should have been validated by the caller
//!     - Caller should have validated that the driver has been initialized
//!     - Both pGni and pLni are the most recent network view
//!
//! @post
//!
//! @note
//!

boolean managed_has_addressing_changed(globalNetworkInfo * pGni, lni_t * pLni) {
#define DHCP_ENTRY_LEN    1024
#define MAX_RULE_LEN      1024

    int i = 0;
    int j = 0;
    int rc = 0;
    int ret = 0;
    int slashnet = 0;
    int subnetIdx = 0;
    int nbInstances = 0;
    char *pStr = NULL;
    char *psSubnetIp = NULL;
    char *psPublicIp = NULL;
    char *psPrivateIp = NULL;
    char *psDhcpConfig = NULL;
    char sRule[MAX_RULE_LEN] = "";
    char sPath[EUCA_MAX_PATH] = "";
    char sDhcp[DHCP_ENTRY_LEN] = "";
    boolean found = FALSE;
    gni_cluster *pCluster = NULL;
    gni_instance *pInstances = NULL;
    in_addr_entry *pNetwork = NULL;
    managed_subnet *pSubnet = NULL;

    LOGTRACE("Scrubbing addressing for changes.\n");

    // This only applies to CC
    if (!PEER_IS_CC(eucanetdPeer))
        return (FALSE);

    // Get the cluster configuration
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        // For any failure force a change
        LOGERROR("Cannot find cluster for our network in global network view: check network configuration settings\n");
        return (TRUE);
    }
    // Load our DHCP configuration
    snprintf(sPath, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.conf", config->eucahome);
    if ((psDhcpConfig = file2str(sPath)) == NULL) {
        // If we cannot load the DHCP configuration file, consider it that something has changed
        LOGERROR("Fail to load DHCP configuration file '%s'\n", sPath);
        return (TRUE);
    }
    // Get our associated instances
    if ((rc = gni_cluster_get_instances(pGni, pCluster, NULL, 0, NULL, 0, &pInstances, &nbInstances)) != 0) {
        // For any failure force a change
        LOGERROR("Cannot retrieve instances for our cluster '%s'.\n", pCluster->name);
        EUCA_FREE(psDhcpConfig);
        return (TRUE);
    }
    //
    // Check for DHCP configuration for each instances and each security-group
    //
    for (i = 0; i < nbInstances; i++) {
        snprintf(sDhcp, DHCP_ENTRY_LEN, "\n  host node-%s {\n    hardware ethernet %s;\n    fixed-address %s;\n  }\n",
                euca_ntoa(pInstances[i].privateIp), euca_etoa(pInstances[i].macAddress), euca_ntoa(pInstances[i].privateIp));

        if (strstr(psDhcpConfig, sDhcp) == NULL) {
            LOGTRACE("Network addressing change detected!. Missing DHCP configuration for host %s/%s.\n", euca_ntoa(pInstances[i].privateIp), euca_etoa(pInstances[i].macAddress));
            EUCA_FREE(psDhcpConfig);
            EUCA_FREE(pInstances);
            return (TRUE);
        }
        // now check that we have a group configuration for this instance
        if ((subnetIdx = managed_find_subnet_idx(pInstances[i].privateIp)) != -1) {
            pSubnet = &gaManagedSubnets[subnetIdx];

            pStr = sDhcp;
            pStr += snprintf(pStr, (DHCP_ENTRY_LEN - (pStr - sDhcp)), "  subnet %s netmask %s {\n    option subnet-mask %s;\n    option broadcast-address %s;\n",
                    pSubnet->sSubnet, pSubnet->sNetmask, pSubnet->sNetmask, pSubnet->sBroadcast);

            if (strlen(pGni->instanceDNSDomain)) {
                pStr += snprintf(pStr, (DHCP_ENTRY_LEN - (pStr - sDhcp)), "    option domain-name \"%s\";\n", pGni->instanceDNSDomain);
            }

            if (pGni->max_instanceDNSServers) {
                pStr += snprintf(pStr, (DHCP_ENTRY_LEN - (pStr - sDhcp)), "    option domain-name-servers %s", euca_ntoa(pGni->instanceDNSServers[0]));
                for (j = 1; j < pGni->max_instanceDNSServers; j++) {
                    pStr += snprintf(pStr, (DHCP_ENTRY_LEN - (pStr - sDhcp)), ", %s", euca_ntoa(pGni->instanceDNSServers[j]));
                }
                pStr += snprintf(pStr, (DHCP_ENTRY_LEN - (pStr - sDhcp)), ";\n");
            } else {
                pStr += snprintf(pStr, (DHCP_ENTRY_LEN - (pStr - sDhcp)), "    option domain-name-servers 8.8.8.8;\n");
            }
            pStr += snprintf(pStr, (DHCP_ENTRY_LEN - (pStr - sDhcp)), "    option routers %s;\n  }\n", pSubnet->sGateway);

            // Is the subnet config present?
            if (strstr(psDhcpConfig, sDhcp) == NULL) {
                LOGTRACE("Network addressing change detected!. Missing DHCP subnet declaration for subnet %s/%d.\n", pSubnet->sSubnet, pSubnet->slashNet);
                EUCA_FREE(psDhcpConfig);
                EUCA_FREE(pInstances);
                return (TRUE);
            }
        }
    }

    // Done with DHCP configuration
    EUCA_FREE(psDhcpConfig);

    //
    // Check if there are any changes to the METADATA/CLCIP redirect rule
    //
    if (pGni->enabledCLCIp) {
        snprintf(sRule, MAX_RULE_LEN, "-A PREROUTING -d %s/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", METADATA_IP_STRING, euca_ntoa(pGni->enabledCLCIp));
        if (ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_NAT, IPT_CHAIN_PREROUTING, sRule) == NULL) {
            LOGTRACE("Network addressing change detected! CLC IP changed to '%s'!\n", euca_ntoa(pGni->enabledCLCIp));
            EUCA_FREE(pInstances);
            return (TRUE);
        }
    }
    //
    // Check for NAT rules to be installed for each elastic IPs
    //
    psSubnetIp = hex2dot(pGni->managedSubnet->subnet);
    slashnet = NETMASK_TO_SLASHNET(pGni->managedSubnet->netmask);
    for (i = 0; i < nbInstances; i++) {
        ret = 0;
        psPublicIp = hex2dot(pInstances[i].publicIp);
        psPrivateIp = hex2dot(pInstances[i].privateIp);

        // Only install NAT rules if we have a valid public IP
        if (pInstances[i].publicIp) {
            // Mark packets that requires SNAT
            snprintf(sRule, MAX_RULE_LEN, "-A PREROUTING -s %s/%d -d %s/32 -j MARK --set-xmark 0x15/0xffffffff",
                    psSubnetIp, slashnet, psPublicIp);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_NAT, IPT_CHAIN_PREROUTING, sRule) == NULL) ? 1 : 0);
            snprintf(sRule, MAX_RULE_LEN, "-A OUTPUT -s %s/%d -d %s/32 -j MARK --set-xmark 0x15/0xffffffff",
                    psSubnetIp, slashnet, psPublicIp);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_NAT, IPT_CHAIN_OUTPUT, sRule) == NULL) ? 1 : 0);

            // DNAT public to private on the pre-routing chain
            snprintf(sRule, MAX_RULE_LEN, "-A PREROUTING -d %s -j DNAT --to-destination %s", psPublicIp, psPrivateIp);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_NAT, IPT_CHAIN_PREROUTING, sRule) == NULL) ? 1 : 0);

            // DNAT public to private on the output chain
            snprintf(sRule, MAX_RULE_LEN, "-A OUTPUT -d %s -j DNAT --to-destination %s", psPublicIp, psPrivateIp);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_NAT, IPT_CHAIN_OUTPUT, sRule) == NULL) ? 1 : 0);

            // SNAT private to public on the post-routing chain
            snprintf(sRule, MAX_RULE_LEN, "-A POSTROUTING -s %s/32 ! -d %s/%d -j SNAT --to-source %s", psPrivateIp, psSubnetIp, slashnet, psPublicIp);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_NAT, IPT_CHAIN_POSTROUTING, sRule) == NULL) ? 1 : 0);
            snprintf(sRule, MAX_RULE_LEN, "-A POSTROUTING -s %s/32 -m mark --mark 0x15 -j SNAT --to-source %s",
                    psPrivateIp, psPublicIp);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_NAT, IPT_CHAIN_POSTROUTING, sRule) == NULL) ? 1 : 0);

            // SNAT for the instance itself
            snprintf(sRule, MAX_RULE_LEN, "-A POSTROUTING -s %s/32 -d %s/32 -j SNAT --to-source %s", psPrivateIp, psPrivateIp, psPublicIp);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_NAT, IPT_CHAIN_POSTROUTING, sRule) == NULL) ? 1 : 0);
        }
        // Only install COUNTERs rules if we have a valid private IP
        if (pInstances[i].privateIp) {
            // Add to our IN counters
            snprintf(sRule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/32", psPrivateIp);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_IN, sRule) == NULL) ? 1 : 0);

            // Add to our OUT counters
            snprintf(sRule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/32", psPrivateIp);
            ret |= ((ipt_chain_find_rule(pLni->pIpTables, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_OUT, sRule) == NULL) ? 1 : 0);
        }

        EUCA_FREE(psPublicIp);
        EUCA_FREE(psPrivateIp);

        // Do we have any missing rules?
        if (ret) {
            LOGTRACE("Network addressing change detected!. Missing IPT rules for instance '%s'.\n", pInstances[i].name);
            EUCA_FREE(pInstances);
            EUCA_FREE(psSubnetIp);
            return (TRUE);
        }
    }

    // Done with subnet IP
    EUCA_FREE(psSubnetIp);

    //
    // Check to ensure all elastic IPs are properly installed on the public interface
    //
    for (i = 0, pNetwork = pLni->pNetworks; i < pLni->numberOfNetworks; i++, pNetwork++) {
        // make sure this is on our public interface
        if (strcmp(pNetwork->sDevName, config->pubInterface)) {
            if ((subnetIdx = managed_find_subnet_idx_from_gateway(pNetwork->address)) == -1)
                continue;

            for (j = 0, found = FALSE; ((j < nbInstances) && !found); j++) {
                if (pNetwork->address == pInstances[j].publicIp)
                    found = TRUE;
            }

            if (!found) {
                LOGTRACE("Network addressing change detected!. No instance with public IP %s.\n", pNetwork->sHost);
                EUCA_FREE(pInstances);
                return (TRUE);
            }
        }
    }

    for (i = 0; i < nbInstances; i++) {
        for (j = 0, found = FALSE, pNetwork = pLni->pNetworks; ((j < pLni->numberOfNetworks) && !found); j++, pNetwork++) {
            if (pNetwork->address == pInstances[i].publicIp)
                found = TRUE;
        }

        if (!found) {
            LOGTRACE("Network addressing change detected!. No IP %s not found on public interface.\n", euca_ntoa(pInstances[i].publicIp));
            EUCA_FREE(pInstances);
            return (TRUE);
        }
    }

    EUCA_FREE(pInstances);
    return (FALSE);

#undef DHCP_ENTRY_LEN
#undef MAX_RULE_LEN
}

//!
//! This will assign or unassign the metadata IP address to the given device based on the
//! knowledge of the CLC IP address within the GNI configuration.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] psDevName a constant string pointer to the device name we need to work with
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see network_driver_implement_network()
//!
//! @pre
//!     Both pGni and psDevName must not be NULL
//!
//! @post
//!     On success, the metadata IP is either added or removed from the device. On failure,
//!     nothing has changed on this system.
//!
//! @note
//!

int managed_setup_metadata_ip(globalNetworkInfo * pGni, const char *psDevName) {
    int rc = 0;

    // Are our pointers valid?
    if (!pGni || !psDevName) {
        LOGERROR("Failed to set/unset metadata on device '%s'. Invalid parameters provided!\n", SP(psDevName));
        return (1);
    }
    //
    // Install or remove the metadata IP on our private interface. If the device does not exist,
    // the APIs below will take care of it.
    //
    if (pGni->enabledCLCIp) {
        // We know the CLC IP address... We have to install the metadata IP on our private interface
        LOGTRACE("Setting up metadata IP %s/32 on device %s.\n", METADATA_IP_STRING, psDevName);
        if ((rc = dev_move_ip(psDevName, INADDR_METADATA, IN_HOST_NET, INADDR_ANY, SCOPE_LINK)) != 0) {
            LOGERROR("Failed to install metadata IP %s/32 on network device '%s'.\n", METADATA_IP_STRING, psDevName);
            return (1);
        }
    } else {
        // CLC IP isn't known. Remove the metadata IP from the private interface
        LOGTRACE("Removing metadata IP %s/32 from device %s.\n", METADATA_IP_STRING, psDevName);
        if ((rc = dev_remove_ip(psDevName, INADDR_METADATA, IN_HOST_NET)) != 0) {
            LOGERROR("Failed to remove metadata IP %s/32 from network device '%s'.\n", METADATA_IP_STRING, psDevName);
            return (1);
        }
    }
    return (0);
}

//!
//! This takes care of implementing the security-group filters on the IP tables. The following
//! steps will be executed:
//!     -# Populate the IP table
//!     -# Set the default FORWARD filter policy to DROP
//!     -# Install the IN/OUT Counter filter chains
//!     -# Delete any chains starting with "sg-" (this is the security group chains)
//!     -# Flush the entire FORWARD and IN/OUT Counters chains under the filter table
//!     -# Add the global subnet FORWARDING and IN/OUT Counter rules
//!     -# Add the security group chains ("sg-xxxxxxxx") and associated rules
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see network_driver_implement_sg()
//!
//! @pre
//!     Both pGni and pLni must not be NULL
//!
//! @post
//!     On success, the networking artifacts should be implemented. On failure, the
//!     current state of the system may be left in a non-deterministic state. A
//!     subsequent call to this API may resolve the left over issues.
//!
//! @note
//!

int managed_setup_sg_filters(globalNetworkInfo * pGni) {
#define MAX_RULE_LEN      1024

    int i = 0;
    int j = 0;
    int k = 0;
    int l = 0;
    int found = 0;
    int rc = 0;
    int ret = 0;
    int slashnet = 0;
    int nbGroups = 0;
    int networkIdx = 0;
    int nbInstances = 0;
    char *psSubnetIp = NULL;
    char sRule[MAX_RULE_LEN] = "";
    gni_cluster *pCluster = NULL;
    gni_secgroup *pSecGroup = NULL;
    gni_secgroup *pPeerGroup = NULL;
    gni_secgroup *pSecGroups = NULL;
    gni_instance *pInstances = NULL;
    managed_subnet *pPeerSubnet = NULL;
    u32 cidrnm = 0;
    char *pStra = NULL;

    LOGTRACE("Implementing security-group artifacts\n");

    // Are the global and local network view structures NULL?
    if (!pGni) {
        LOGERROR("Failed to implement security-group artifacts. Invalid parameters provided.\n");
        return (1);
    }
    // We only have something to do on the CC
    if (!PEER_IS_CC(eucanetdPeer)) {
        return (0);
    }
    // Get the cluster configuration
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("cannot find cluster for our network in global network view: check network configuration settings\n");
        return (1);
    }
    // pull in latest IPT state
    if ((rc = ipt_handler_repopulate(config->ipt)) != 0) {
        LOGERROR("cannot read current IPT rules: check above log errors for details\n");
        return (1);
    }
    // Get the security groups for this cluster only
    if ((rc = gni_cluster_get_secgroup(pGni, pCluster, NULL, 0, NULL, 0, &pSecGroups, &nbGroups)) != 0) {
        LOGERROR("Cannot find security-groups for cluster '%s' in global network view: check network configuration settings\n", pCluster->name);
        return (1);
    }
    // Retrieve our subnet information
    psSubnetIp = hex2dot(pGni->managedSubnet->subnet);
    slashnet = NETMASK_TO_SLASHNET(pGni->managedSubnet->netmask);

    // Make sure our default FORWARD policy is set to DROP
    rc = ipt_table_set_chain_policy(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, IPT_CHAIN_POLICY_DROP);

    // make sure our EUCA counter chains are in place
    rc = ipt_table_add_chain(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_IN, "-", "[0:0]");
    rc = ipt_table_add_chain(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_OUT, "-", "[0:0]");

    // clear all chains and IP sets that we're about to (re)populate with latest network metadata
    rc = ipt_table_deletechainmatch(config->ipt, IPT_TABLE_FILTER, MANAGED_SG_CHAINNAME_PREFIX);
    rc = ipt_chain_flush(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD);
    rc = ipt_chain_flush(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_IN);
    rc = ipt_chain_flush(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_OUT);

    // add our counters and the connection tracking state to the FORWARD filter chain
    rc = ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, "-A FORWARD -j EUCA_COUNTERS_IN");
    rc = ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, "-A FORWARD -j EUCA_COUNTERS_OUT");
    rc = ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, "-A FORWARD -m conntrack --ctstate ESTABLISHED -j ACCEPT");

    // Add global forwarding rule for non-private subnet traffic
    snprintf(sRule, MAX_RULE_LEN, "-A FORWARD ! -d %s/%u -j ACCEPT", psSubnetIp, slashnet);
    rc = ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, sRule);

    // add our entire private subnet to the EUCA counters IN
    snprintf(sRule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/%d", psSubnetIp, slashnet);
    ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_IN, sRule);

    // add our entire private subnet to the EUCA counters OUT
    snprintf(sRule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/%d", psSubnetIp, slashnet);
    ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_OUT, sRule);

    // Free our subnet string
    EUCA_FREE(psSubnetIp);

    // add security group chains/rules
    for (i = 0, pSecGroup = pSecGroups; i < nbGroups; i++, pSecGroup++) {
        if ((rc = gni_secgroup_get_instances(pGni, pSecGroup, NULL, 0, NULL, 0, &pInstances, &nbInstances)) != 0) {
            LOGERROR("Fail to get instances for security group '%s'. Look at above log errors for more details.\n", ((pSecGroup == NULL) ? "NULL" : SP(pSecGroup->name)));
            continue;
        }
        // For an instance in this SG with a valid private IP, find its subnet index
        for (j = 0, networkIdx = -1; ((j < nbInstances) && (networkIdx == -1)); j++) {
            if (pInstances[j].privateIp) {
                networkIdx = managed_find_subnet_idx(pInstances[j].privateIp);
            }
        }

        // Now, did we find a network for this group?
        if (networkIdx >= 0) {
            // add the security group forward chain
            ipt_table_add_chain(config->ipt, IPT_TABLE_FILTER, pSecGroup->name, "-", "[0:0]");
            ipt_chain_flush(config->ipt, IPT_TABLE_FILTER, pSecGroup->name);

            // add jump rule to the security group chain
            snprintf(sRule, MAX_RULE_LEN, "-A FORWARD -j %s", pSecGroup->name);
            ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, sRule);

            // add the in-private subnet forwarding rule
            snprintf(sRule, MAX_RULE_LEN, "-A FORWARD -s %s/%u -d %s/%u -j ACCEPT", gaManagedSubnets[networkIdx].sSubnet,
                    gaManagedSubnets[networkIdx].slashNet, gaManagedSubnets[networkIdx].sSubnet, gaManagedSubnets[networkIdx].slashNet);
            ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_FORWARD, sRule);

            // then put all the group specific IPT rules (temporary one here)
            if (pSecGroup->max_grouprules) {
                for (j = 0; j < pSecGroup->max_grouprules; j++) {
                    // are we authorizing 1 group into another?
                    if (strlen(pSecGroup->ingress_rules[j].groupId) == 0) {
                        // This is CIDR-based rule
                        cidrnm = (u32) 0xffffffff << (32 - pSecGroup->ingress_rules[j].cidrSlashnet);
                        if (pSecGroup->ingress_rules[j].cidrSlashnet != 0) {
                            // Search for public IPs affected by EUCA-11476
                            for (k = 0; k < pGni->max_instances; k++) {
                                for (l = 0, found = 0; (l < nbInstances) && !found; l++) {
                                    // Skip instances in this security group
                                    if (pInstances[l].publicIp == pGni->instances[k]->publicIp) {
                                        found = 1;
                                    }
                                }
                                if (!found && ((pGni->instances[k]->publicIp & cidrnm) == (pSecGroup->ingress_rules[j].cidrNetaddr & cidrnm)) &&
                                        ((pGni->instances[k]->privateIp & cidrnm) != (pSecGroup->ingress_rules[j].cidrNetaddr & cidrnm))) {
                                    pStra = hex2dot(pGni->instances[k]->privateIp);
                                    LOGTRACE("Found instance private IP (%s) affected by EUCA-11476.\n", pStra);
                                    ingress_gni_to_iptables_rule(pStra, &(pSecGroup->ingress_rules[j]), sRule, 2);
                                    EUCA_FREE(pStra);
                                    pStra = strdup(sRule);
                                    snprintf(sRule, MAX_RULE_LEN, "-A %s -d %s/%u %s -j ACCEPT", pSecGroup->name, gaManagedSubnets[networkIdx].sSubnet,
                                            gaManagedSubnets[networkIdx].slashNet, pStra);
                                    LOGTRACE("Created new iptables rule: %s\n", sRule);
                                    ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, pSecGroup->name, sRule);
                                    EUCA_FREE(pStra);
                                }
                            }
                            ingress_gni_to_iptables_rule(NULL, &(pSecGroup->ingress_rules[j]), sRule, 4);
                            pStra = strdup(sRule);
                            snprintf(sRule, MAX_RULE_LEN, "-A %s -d %s/%u %s -j ACCEPT", pSecGroup->name, gaManagedSubnets[networkIdx].sSubnet,
                                    gaManagedSubnets[networkIdx].slashNet, pStra);
                            EUCA_FREE(pStra);
                        } else {
                            ingress_gni_to_iptables_rule(NULL, &(pSecGroup->ingress_rules[j]), sRule, 0);
                            pStra = strdup(sRule);
                            snprintf(sRule, MAX_RULE_LEN, "-A %s -d %s/%u %s -j ACCEPT", pSecGroup->name, gaManagedSubnets[networkIdx].sSubnet,
                                    gaManagedSubnets[networkIdx].slashNet, pStra);
                            EUCA_FREE(pStra);
                        }
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, pSecGroup->name, sRule);
                    } else if (gni_find_secgroup(pGni, pSecGroup->ingress_rules[j].groupId, &pPeerGroup) == 0) {
                        // Now find the subnet for this security group
                        if ((pPeerSubnet = managed_find_subnet(pGni, pPeerGroup)) != NULL) {
                            // Private IPs of this security group are authorized, so EUCA-11476 does not manifest
                            ingress_gni_to_iptables_rule(NULL, &(pSecGroup->ingress_rules[j]), sRule, 0);
                            pStra = strdup(sRule);
                            snprintf(sRule, MAX_RULE_LEN, "-A %s -s %s/%u -d %s/%u %s -j ACCEPT", pSecGroup->name, pPeerSubnet->sSubnet, pPeerSubnet->slashNet,
                                    gaManagedSubnets[networkIdx].sSubnet, gaManagedSubnets[networkIdx].slashNet, pStra);
                            EUCA_FREE(pStra);
                            /*
                                                        snprintf(sRule, MAX_RULE_LEN, "-A %s -s %s/%u -d %s/%u %s -j ACCEPT", pSecGroup->name, pPeerSubnet->sSubnet, pPeerSubnet->slashNet,
                                                                 gaManagedSubnets[networkIdx].sSubnet, gaManagedSubnets[networkIdx].slashNet, pSecGroup->grouprules[j].name);
                             */
                            ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, pSecGroup->name, sRule);
                        }
                    }
                }
            }
        } else if (ipt_table_find_chain(config->ipt, IPT_TABLE_FILTER, pSecGroup->name)) {
            ipt_table_deletechainmatch(config->ipt, IPT_TABLE_FILTER, pSecGroup->name);
        }
        // We're done with the instances
        EUCA_FREE(pInstances);
    }

    EUCA_FREE(pSecGroups);

    if (1 || !ret) {
        ipt_handler_print(config->ipt);
        if ((rc = ipt_handler_deploy(config->ipt)) != 0) {
            LOGERROR("could not apply new rules: check above log errors for details\n");
            ret = 1;
        }
    }

    return (ret);

#undef MAX_RULE_LEN
}

//!
//! This takes care of implementing the addressing artifacts necessary. This will add or
//! remove IP addresses and elastic IPs for each instances. This will execute the following
//! tasks:
//!     -# Populate the IP tables structure
//!     -# Flush the following chains from the NAT table: OUTPUT, PREROUTING and POSTROUTING
//!     -# Install the metadata redirect rule on the NAT table PREROUTING chain
//!     -# Install the network masquerade rule on the POSTROUTING Chain
//!     -# Add the proper DNAT, SNAT and IN/OUT Counter rules for each instances
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see network_driver_implement_addressing()
//!
//! @pre
//!     Both pGni and pLni must not be NULL
//!
//! @post
//!     On success, the networking artifacts should be implemented. On failure, the
//!     current state of the system may be left in a non-deterministic state. A
//!     subsequent call to this API may resolve the left over issues.
//!
//! @note
//!

int managed_setup_addressing(globalNetworkInfo * pGni) {
#define MAX_RULE_LEN      1024

    int i = 0;
    int j = 0;
    int rc = 0;
    int ret = 0;
    int slashnet = 0;
    int nbNodes = 0;
    int nbInstances = 0;
    u32 network = 0;
    u32 netmask = 0;
    char *psClcIp = NULL;
    char *psSubnetIp = NULL;
    char *psPublicIp = NULL;
    char *psPrivateIp = NULL;
    char sRule[MAX_RULE_LEN] = "";
    gni_cluster *pCluster = NULL;
    gni_node *pNodes = NULL;
    gni_instance *pInstances = NULL;

    LOGDEBUG("Updating public IP to private IP mappings.\n");

    // Are the global and local network view structures NULL?
    if (!pGni) {
        LOGERROR("Failed to update public IP to private IP mapping. Invalid parameters provided.\n");
        return (1);
    }
    // This only works on CC
    if (!PEER_IS_CC(eucanetdPeer)) {
        return (0);
    }
    // Retrieve our configuration information
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("Cannot locate cluster to which local node belongs, in global network view: check network configuration settings!\n");
        return (1);
    }
    // pull in latest IPT state
    if ((rc = ipt_handler_repopulate(config->ipt)) != 0) {
        LOGERROR("Cannot read current IPT rules: check above log errors for details\n");
        return (1);
    }
    // clear all chains and IP sets that we're about to (re)populate with latest network metadata
    rc = ipt_chain_flush(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_OUTPUT);
    rc = ipt_chain_flush(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_PREROUTING);
    rc = ipt_chain_flush(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_POSTROUTING);

    // Retrieve our subnet information
    network = pGni->managedSubnet->subnet;
    netmask = pGni->managedSubnet->netmask;
    slashnet = NETMASK_TO_SLASHNET(netmask);

    // Add our pre-routing metadata rules if we know which CLC is active
    if (pGni->enabledCLCIp) {
        psClcIp = hex2dot(pGni->enabledCLCIp);
        snprintf(sRule, MAX_RULE_LEN, "-A PREROUTING -d %s/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", METADATA_IP_STRING, psClcIp);
        ipt_chain_add_rule(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_PREROUTING, sRule);
        EUCA_FREE(psClcIp);
    }
    psSubnetIp = hex2dot(network);

    if ((rc = gni_cluster_get_nodes(pGni, pCluster, NULL, 0, NULL, 0, &pNodes, &nbNodes)) == 0) {
        for (i = 0; i < nbNodes; i++) {
            if ((rc = gni_node_get_instances(pGni, &pNodes[i], NULL, 0, NULL, 0, &pInstances, &nbInstances)) == 0) {
                for (j = 0; j < nbInstances; j++) {
                    psPublicIp = hex2dot(pInstances[j].publicIp);
                    psPrivateIp = hex2dot(pInstances[j].privateIp);

                    // Only install NAT rules if we have a valid public IP
                    if (pInstances[j].publicIp) {
                        // Mark packets that requires SNAT
                        snprintf(sRule, MAX_RULE_LEN, "-A PREROUTING -s %s/%d -d %s/32 -j MARK --set-xmark 0x15/0xffffffff",
                                psSubnetIp, slashnet, psPublicIp);
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_PREROUTING, sRule);
                        snprintf(sRule, MAX_RULE_LEN, "-A OUTPUT -s %s/%d -d %s/32 -j MARK --set-xmark 0x15/0xffffffff",
                                psSubnetIp, slashnet, psPublicIp);
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_OUTPUT, sRule);

                        // DNAT public to private on the pre-routing chain
                        snprintf(sRule, MAX_RULE_LEN, "-A PREROUTING -d %s -j DNAT --to-destination %s", psPublicIp, psPrivateIp);
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_PREROUTING, sRule);

                        // DNAT public to private on the output chain
                        snprintf(sRule, MAX_RULE_LEN, "-A OUTPUT -d %s -j DNAT --to-destination %s", psPublicIp, psPrivateIp);
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_OUTPUT, sRule);

                        // SNAT private to public on the post-routing chain
                        snprintf(sRule, MAX_RULE_LEN, "-A POSTROUTING -s %s/32 ! -d %s/%d -j SNAT --to-source %s", psPrivateIp, psSubnetIp, slashnet, psPublicIp);
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_POSTROUTING, sRule);
                        snprintf(sRule, MAX_RULE_LEN, "-A POSTROUTING -s %s/32 -m mark --mark 0x15 -j SNAT --to-source %s",
                                psPrivateIp, psPublicIp);
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_POSTROUTING, sRule);

                        // SNAT for the instance itself
                        snprintf(sRule, MAX_RULE_LEN, "-A POSTROUTING -s %s/32 -d %s/32 -j SNAT --to-source %s", psPrivateIp, psPrivateIp, psPublicIp);
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_POSTROUTING, sRule);
                    }
                    // Only install COUNTERs rules if we have a valid private IP
                    if (pInstances[j].privateIp) {
                        // Add to our IN counters
                        snprintf(sRule, MAX_RULE_LEN, "-A EUCA_COUNTERS_IN -d %s/32", psPrivateIp);
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_IN, sRule);

                        // Add to our OUT counters
                        snprintf(sRule, MAX_RULE_LEN, "-A EUCA_COUNTERS_OUT -s %s/32", psPrivateIp);
                        ipt_chain_add_rule(config->ipt, IPT_TABLE_FILTER, IPT_CHAIN_EUCA_COUNTERS_OUT, sRule);
                    }

                    EUCA_FREE(psPublicIp);
                    EUCA_FREE(psPrivateIp);
                }
            }
            EUCA_FREE(pInstances);
        }
    }
    // Add our post-routing masquerade rules
    snprintf(sRule, MAX_RULE_LEN, "-A POSTROUTING ! -d %s/%u -s %s/%u -j MASQUERADE", psSubnetIp, slashnet, psSubnetIp, slashnet);
    ipt_chain_add_rule(config->ipt, IPT_TABLE_NAT, IPT_CHAIN_POSTROUTING, sRule);
    EUCA_FREE(psSubnetIp);
    EUCA_FREE(pNodes);

    ipt_handler_print(config->ipt);
    if ((rc = ipt_handler_deploy(config->ipt)) != 0) {
        LOGERROR("could not apply new ipt handler rules: check above log errors for details\n");
        ret = 1;
    }

    return (ret);

#undef MAX_RULE_LEN
}

//!
//! Updates the elastic IP addresses on the public network device. It will remove any IPs
//! that are no longer in service and add the ones that are being put in service.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see network_driver_implement_addressing()
//!
//! @pre
//!
//! @post
//!
//! @note
//!

int managed_setup_elastic_ips(globalNetworkInfo * pGni) {
    int i = 0;
    int j = 0;
    int k = 0;
    int rc = 0;
    int ret = 0;
    int nbNodes = 0;
    int nbInstances = 0;
    boolean found = FALSE;
    gni_cluster *pCluster = NULL;
    gni_node *pNodes = NULL;
    gni_instance *pInstances = NULL;

    LOGTRACE("Updating elastic IPs.\n");

    // Retrieve our configuration information
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("Cannot locate cluster in global network view: check network configuration settings\n");
        return (1);
    }
    // Retrieve the nodes associated with our cluster so we can apply the instances elastic IPs
    if ((rc = gni_cluster_get_nodes(pGni, pCluster, NULL, 0, NULL, 0, &pNodes, &nbNodes)) != 0) {
        LOGERROR("Cannot retrieve the nodes associated with this cluster in global network view: check network configuration settings\n");
        return (1);
    }
    //
    // Remove the elastic IPs that are no longer in use
    //
    for (k = 0; k < pGni->max_public_ips; k++) {
        for (i = 0, found = FALSE; ((i < nbNodes) && !found); i++) {
            // Get the instances associated with this node
            if ((rc = gni_node_get_instances(pGni, &pNodes[i], NULL, 0, NULL, 0, &pInstances, &nbInstances)) == 0) {
                for (j = 0; ((j < nbInstances) && !found); j++) {
                    // Only install elastic IPs if we have them to our public interface
                    if (pInstances[j].publicIp == pGni->public_ips[k]) {
                        found = TRUE;
                    }
                }
            }
            EUCA_FREE(pInstances);
        }

        if (!found) {
            if ((rc = dev_remove_ip(config->pubInterface, pGni->public_ips[k], 0xFFFFFFFF)) != 0) {
                LOGERROR("Failed to remove elastic IP %s/32 on network device '%s'.\n", euca_ntoa(pGni->public_ips[k]), config->pubInterface);
                ret = 1;
            }
        }
    }

    //
    // Now lets add the elastic IPs for the instances that uses them
    //
    for (i = 0; i < nbNodes; i++) {
        // Get the instances associated with this node
        if ((rc = gni_node_get_instances(pGni, &pNodes[i], NULL, 0, NULL, 0, &pInstances, &nbInstances)) == 0) {
            for (j = 0; j < nbInstances; j++) {
                // Only install elastic IPs if we have them to our public interface
                if (pInstances[j].publicIp) {
                    if ((rc = dev_move_ip(config->pubInterface, pInstances[j].publicIp, 0xFFFFFFFF, 0x00000000, SCOPE_GLOBAL)) == 0) {
                        // Make sure the device is up
                        if ((rc = dev_up(config->pubInterface)) != 0) {
                            LOGERROR("Failed to enable network device '%s'.\n", config->pubInterface)
                            ret = 1;
                        }
                    } else {
                        LOGERROR("Failed to install elastic IP %s/32 on network device '%s'.\n", euca_ntoa(pInstances[j].publicIp), config->pubInterface);
                        ret = 1;
                    }
                }
            }
        }
        EUCA_FREE(pInstances);
    }
    EUCA_FREE(pNodes);
    return (ret);
}

//!
//! Splits our private subnet in smaller "sub-subnets" based on the configured
//! VNET_ADDRSPERNET parameter. Each security groups have their own smaller
//! subnets. Building this structure helps pre-planning for instance to
//! security group management.
//!
//! @param[in] pGni a pointer to our Global Network Information configuration structure.
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see managed_find_subnet_idx(), managed_find_subnet_idx_from_gateway(), managed_find_subnet()
//!
//! @pre
//!     The pGni parameter must not be NULL
//!
//! @post
//!     The managed subnet structure is initialized
//!
//! @note
//!

int managed_initialize_private_subnets(globalNetworkInfo * pGni) {
    int rc = 0;
    int netIdx = 0;
    u32 numNetworks = 0;
    u32 broadcastOffset = 0;
    in_addr_t newSubnet = 0;
    in_addr_t newNetmask = 0;
    gni_cluster *pCluster = NULL;

    LOGTRACE("Initializing managed subnets.\n");

    // Make sure our given parameter is valid
    if (!pGni) {
        LOGERROR("Failed to initialize private managed subnets. Invalid parameters provided.\n");
        return (1);
    }
    // Find the cluster we're associated with to retrieve the private subnet address and netmask
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("cannot find cluster for our network in global network view: check network configuration settings\n");
        return (1);
    }
    // Reset our structure
    bzero(gaManagedSubnets, (NB_VLAN_802_1Q * sizeof (managed_subnet)));

    // What are the size of our sub-networks? It must not exceed what we can handle maximum
    numNetworks = (((0xFFFFFFFF - pGni->managedSubnet->netmask) + 1) / pGni->managedSubnet->segmentSize);
    numNetworks = MIN(numNetworks, NB_VLAN_802_1Q);

    LOGTRACE("Initializing %u subnets for %s network with %u addresses per network.\n", numNetworks, euca_ntoa(pGni->managedSubnet->subnet), pGni->managedSubnet->segmentSize);

    // Compute all of our sub-subnets
    newSubnet = pGni->managedSubnet->subnet;
    newNetmask = 0xFFFFFFFF - (pGni->managedSubnet->segmentSize - 1);
    broadcastOffset = pGni->managedSubnet->segmentSize - 1;
    for (netIdx = MIN_VLAN_EUCA; netIdx < numNetworks; netIdx++) {
        gaManagedSubnets[netIdx].vlanId = netIdx;
        gaManagedSubnets[netIdx].subnet = newSubnet;
        gaManagedSubnets[netIdx].netmask = newNetmask;
        gaManagedSubnets[netIdx].slashNet = NETMASK_TO_SLASHNET(newNetmask);
        gaManagedSubnets[netIdx].gateway = (newSubnet + 1);
        gaManagedSubnets[netIdx].broadcast = (newSubnet + broadcastOffset);

        euca_strncpy(gaManagedSubnets[netIdx].sSubnet, euca_ntoa(gaManagedSubnets[netIdx].subnet), sizeof (gaManagedSubnets[netIdx].sSubnet));
        euca_strncpy(gaManagedSubnets[netIdx].sNetmask, euca_ntoa(gaManagedSubnets[netIdx].netmask), sizeof (gaManagedSubnets[netIdx].sNetmask));
        euca_strncpy(gaManagedSubnets[netIdx].sGateway, euca_ntoa(gaManagedSubnets[netIdx].gateway), sizeof (gaManagedSubnets[netIdx].sGateway));
        euca_strncpy(gaManagedSubnets[netIdx].sBroadcast, euca_ntoa(gaManagedSubnets[netIdx].broadcast), sizeof (gaManagedSubnets[netIdx].sBroadcast));

        newSubnet += pGni->managedSubnet->segmentSize;

        LOGEXTREME("subnet %s netmask %s (%u) broadcast %s gateway %s\n", gaManagedSubnets[netIdx].sSubnet, gaManagedSubnets[netIdx].sNetmask,
                gaManagedSubnets[netIdx].slashNet, gaManagedSubnets[netIdx].sGateway, gaManagedSubnets[netIdx].sBroadcast)
    }
    return (0);
}

//!
//! Find the proper private subnet index within our managed subnet structure that fits a
//! given instance IP.
//!
//! @param[in] instanceAddress the IP address of the instance we're looking up a subnet for
//!
//! @return The managed subnet index or -1 on failure
//!
//! @see initialize_private_subnets(), managed_find_subnet_idx_from_gateway(), managed_find_subnet()
//!
//! @pre
//!     The managed subnets must have been initialized prior calling this API
//!
//! @post
//!
//! @note
//!

int managed_find_subnet_idx(in_addr_t instanceAddress) {
    int networkIdx = 0;

    // Scan our subnet structure to find which one will fit this address within its netmask
    for (networkIdx = MIN_VLAN_EUCA; networkIdx < MAX_VLAN_EUCA; networkIdx++) {
        if (gaManagedSubnets[networkIdx].netmask > 0) {
            if ((instanceAddress & gaManagedSubnets[networkIdx].netmask) == (gaManagedSubnets[networkIdx].subnet)) {
                return (networkIdx);
            }
        }
    }
    return (-1);
}

//!
//! Find the proper private subnet index within our managed subnet structure that fits a
//! given subnet gateway IP.
//!
//! @param[in] gatewayAddress the gateway IP address of the private subnet
//!
//! @return The managed subnet index or -1 on failure
//!
//! @see initialize_private_subnets(), managed_find_subnet_idx(), managed_find_subnet()
//!
//! @pre
//!     The managed subnets must have been initialized prior calling this API
//!
//! @post
//!
//! @note
//!

int managed_find_subnet_idx_from_gateway(in_addr_t gatewayAddress) {
    int networkIdx = 0;

    // Scan our subnet structure to find which one will fit this address within its netmask
    for (networkIdx = MIN_VLAN_EUCA; networkIdx < MAX_VLAN_EUCA; networkIdx++) {
        if (gaManagedSubnets[networkIdx].gateway == gatewayAddress) {
            return (networkIdx);
        }
    }
    return (-1);
}

//!
//! Find the proper private subnet index within our managed subnet structure that fits a
//! given security group.
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//! @param[in] pSecGroup a pointer to the security group
//!
//! @return A pointer to the managed subnet if found otherwise FALSE is returned.
//!
//! @see initialize_private_subnets(), managed_find_subnet_idx(), managed_find_subnet_idx_from_gateway()
//!
//! @pre
//!     - Both our parameters must not be NULL.
//!     - The security group must have some instances associated
//!
//! @post
//!
//! @note
//!

managed_subnet *managed_find_subnet(globalNetworkInfo * pGni, gni_secgroup * pSecGroup) {
    int k = 0;
    int rc = 0;
    u32 subnetIdx = 0;
    int nbInstances = 0;
    gni_instance *pInstances = NULL;
    managed_subnet *pSubnet = NULL;

    // Make sure our GNI and security group aren't NULL
    if (!pGni || !pSecGroup)
        return (NULL);
    // Retrieve the instances for this security group
    if ((rc = gni_secgroup_get_instances(pGni, pSecGroup, NULL, 0, NULL, 0, &pInstances, &nbInstances)) != 0) {
        LOGWARN("Failed to retrieve instances for security group '%s'. Check above error for more details.\n", pSecGroup->name);
        return (NULL);
    }
    // If we don't have any instances, continue with the next group
    if (nbInstances == 0) {
        EUCA_FREE(pInstances);
        return (NULL);
    }
    // Scan our instances until we get one with a valid private IP assigned
    for (k = 0, pSubnet = NULL; ((k < nbInstances) && !pSubnet); k++) {
        // Ok, now we should be able to find our subnet
        if ((subnetIdx = managed_find_subnet_idx(pInstances[k].privateIp)) != -1) {
            pSubnet = &gaManagedSubnets[subnetIdx];
        }
    }
    EUCA_FREE(pInstances);
    return (pSubnet);
}

//!
//! This initialize tunneling on startup. This requires that we know our system
//! local IP address through the VNET_LOCALIP configuration parameter in eucalyptus.conf.
//! This function will execute the following tasks:
//!
//!     -# Make sure tuneling is enabled. If its disabled, than we'll cleanup in case we were previously enabled
//!     -# Make sure VTUN packages are installed
//!     -# Make sure we know our local IP
//!     -# Make sure the tunnel password file exists (vtunpass) under the keys directory
//!     -# Update the vtunall.conf file with proper configuration
//!
//! @param[in] pConfig a pointer to eucanetd configuration structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see managed_cleanup_tunnels()
//! @see managed_setup_tunnels(), managed_unset_tunnels()
//! @see managed_attach_tunnels(), managed_attach_tunnel()
//! @see managed_detach_tunnels(), managed_detach_tunnel()
//!
//! @pre
//!     - The pConfig parameter must not be NULL
//!     - The VTUN packages must be installed
//!     - A local IP address must have been provided in VNET_LOCALIP
//!     - Tunneling should be enabled. If not, we will cleanup and return the success/failure of this action
//!     - The 'vtunpass' file must be present in the Eucalyptus keys directory
//!     - The 'vtunall.conf.template' configuration template file must exists in the Eucalyptus data directory
//!
//! @post
//!     On success, tunneling is initialized properly. This means the 'vtunall.conf' configuration file is
//!     present under the Eucalyptus keys directory. On Failure, nothing has changed on the system.
//!
//! @note
//!     By now, we assume any caller would have validated we are running on a CC component...
//!

int managed_initialize_tunnels(eucanetdConfig * pConfig) {
    int rc = 0;
    char *pChar = NULL;
    char *psPassword = NULL;
    char *psTemplate = NULL;
    char *psTunConfig = NULL;
    char sFileName[EUCA_MAX_PATH] = "";

    LOGINFO("Initializing tunneling.\n");

    // Make sure our parameters are valid
    if (!pConfig) {
        LOGERROR("Fail to initialize tunneling. Invalid parameters provided.\n");
        return (1);
    }
    // Are we using tunneling???
    if (pConfig->disableTunnel) {
        LOGDEBUG("Tunneling disabled.\n");
        return (0);
    }
    // Is vtund present?
    if (check_path(VTUND_PATH)) {
        LOGERROR("Fail to initialize tunneling. Tunneling application '%s' not found. Check installed packages.\n", VTUND_PATH);
        return (1);
    }
    // Do we know our local IP address?
    if (pConfig->localIp == 0) {
        LOGERROR("Fail to initialize tunneling. Unknown local IP address. Check configuration for VNET_LOCALIP.\n");
        return (1);
    }
    // Check that the tunnel password is present
    snprintf(sFileName, EUCA_MAX_PATH, VTUND_PASSWORD_PATH, pConfig->eucahome);
    if (check_file(sFileName)) {
        LOGERROR("Fail to initialize tunneling. Cannot locate tunnel password file '%s'.\n", sFileName);
        return (1);
    }
    // Can we get the password?
    if ((psPassword = file2str(sFileName)) == NULL) {
        LOGERROR("Fail to initialize tunneling. Cannot retrieve tunnel password from '%s' file.\n", sFileName);
        return (1);
    }
    // Remove the new line character if present
    if ((pChar = strchr(psPassword, '\n')) != NULL)
        (*pChar) = '\0';

    snprintf(sFileName, EUCA_MAX_PATH, VTUND_TEMPLATE_CONFIG_PATH, pConfig->eucahome);
    if ((psTemplate = file2str(sFileName)) == NULL) {
        LOGERROR("Fail to initialize tunneling. Cannot retrieve tunnel template configuration from '%s'.\n", sFileName);
        EUCA_FREE(psPassword);
        return (1);
    }
    // Do the substitution
    if ((psTunConfig = euca_strreplace(&psTemplate, "VPASS", psPassword)) == NULL) {
        LOGERROR("Fail to initialize tunneling. Cannot configure password in '%s'.\n", sFileName);
        EUCA_FREE(psTemplate);
        EUCA_FREE(psPassword);
    }
    // We're now done with the password
    EUCA_FREE(psPassword);

    // Now save the template into the real configuration file
    snprintf(sFileName, EUCA_MAX_PATH, VTUND_CONFIG_PATH, pConfig->eucahome);
    if ((rc = write2file(sFileName, psTemplate)) != 0) {
        LOGERROR("Fail to initialize tunneling. Cannot write vtun config file '%s'.\n", sFileName);
        EUCA_FREE(psTemplate);
        return (1);
    }

    EUCA_FREE(psTemplate);
    return (0);
}

//!
//! This unitialize the tuneling functionality on this system. This will essentially unset the
//! tunnels and delete the VTUND configuration file.
//!
//! @param[in] pGni a pointer to our GNI structure
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see managed_initialize_tunnels()
//! @see managed_setup_tunnels(), managed_unset_tunnels()
//! @see managed_attach_tunnels(), managed_attach_tunnel()
//! @see managed_detach_tunnels(), managed_detach_tunnel()
//!
//! @pre
//!     The given pointer not be NULL
//!
//! @post
//!     On success the tunnel devices are remove from the system and tunneling is
//!     unconfigured. On failure, the state of the system may be left undeterministic.
//!     A subsequent call to cleanup may solve the problem.
//!
//! @note
//!     Once this function is called and successful, we won't be able to setup any
//!     tunnel unless the "initialize()" function is called again. This is better
//!     called when the system is terminating and we want to cleanup the system.
//!

int managed_cleanup_tunnels(globalNetworkInfo * pGni) {
    LOGINFO("Cleaning up tunneling.\n");

    // Make sure our parameter isn't NULL
    if (!pGni) {
        LOGERROR("Fail to cleanup tunneling. Invalid parameters provided!\n");
        return (1);
    }
    // Teardown our tunnels if necessary
    if (managed_unset_tunnels(pGni)) {
        LOGERROR("Fail to cleanup tunneling. Fail to teardown tunnels. Check above log errors for more details.\n");
        return (1);
    }
    // Remove the configuration file
    unlink(VTUND_CONFIG_PATH);
    return (0);
}

//!
//! Checks wether or not we need to make any modification to our network tunnel interface
//! artifacts.
//!
//! @param[in] pGni a pointer to our GNI structure
//! @param[in] pSecGroups pointer to a list of security-groups
//! @param[in] nbGroups number of security-groups in the list
//!
//! @return TRUE if we have any change to apply otherwise FALSE is returned
//!
//! @see
//!
//! @pre
//!     The given pointers not be NULL
//!
//! @post
//!
//! @note
//!

boolean managed_has_tunnel_changed(globalNetworkInfo * pGni, gni_secgroup * pSecGroups, int nbGroups) {
    int i = 0;
    int nbTunnels = 0;
    int nbTunnelCalc = 0;
    int nbActiveGroups = 0;
    dev_entry *pTunnels = NULL;

    // Retrieve our tunnel devices, If we fail here, its an automatique change
    if (dev_get_tunnels(NULL, &pTunnels, &nbTunnels)) {
        LOGERROR("Fail to retrieve our tunnel devices. Look at above log errors for more details.\n");
        return (TRUE);
    }
    // If we only have one cluster, then we should have 0 tunnels established. Otherwise, we need to compute
    if (pGni->max_clusters <= 1) {
        // We don't need the devices anymore
        dev_free_list(&pTunnels, nbTunnels);

        // So, do we have any tunnels set?
        if (nbTunnels > 0) {
            return (TRUE);
        }
        return (FALSE);
    }
    // Figure out how many instances with 1 instance or more do we have
    for (i = 0; i < nbGroups; i++) {
        if (pSecGroups[i].max_instances > 0)
            nbActiveGroups++;
    }

    //
    // The number of tunnel devices we have should be equal to the following:
    //   nbTunnelAct = ((number of CC - 1) * 2) + (((number of CC - 1) * 2) * number of active SG)
    //
    nbTunnelCalc = ((pGni->max_clusters - 1) * 2);
    nbTunnelCalc += (nbTunnelCalc * nbActiveGroups);

    // Do we have enough tunnels?
    if (nbTunnels != nbTunnelCalc) {
        // We don't need the devices anymore
        dev_free_list(&pTunnels, nbTunnels);
        return (TRUE);
    }
    // Make sure each tunnel is associated with a bridge device
    for (i = 0; i < nbTunnels; i++) {
        if (!dev_is_bridge_interface(pTunnels[i].sDevName, NULL)) {
            // We don't need the devices anymore
            dev_free_list(&pTunnels, nbTunnels);
            return (TRUE);
        }
    }

    // We don't need the devices anymore
    dev_free_list(&pTunnels, nbTunnels);
    return (FALSE);
}

//!
//! This function retrieves the current cluster ID saved on the system. If the
//! tunnel index is -1, we will remove the tunnel ID configuration file from
//! the system.
//!
//! @param tunnelId the tunnel index to save
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see managed_get_current_tunnel_id()
//!
//! @pre
//!     - If the tunnel index is -1 and the tunnel index configuration file exists, we should be able to unlink it
//!     - If the tunnel index is valid, we should be able to write and overwrite the configuration file
//!
//! @post
//!     On success, the configuration file is created or deleted based on the given valid input. On
//!     failure, status-quo is observed.
//!
//! @note
//!

int managed_save_tunnel_id(int tunnelId) {
    char sTunnelConfFile[EUCA_MAX_PATH] = "";
    FILE *pFh = NULL;

    // Setup the tunnel configuration file
    snprintf(sTunnelConfFile, EUCA_MAX_PATH, VTUND_TUNNEL_ID_FILE_FORMAT, config->eucahome);

    // Is the tunnel index valid?
    if (tunnelId < -1) {
        return (1);
    }
    // If the tunnel ID is -1 then remove the config file
    if (tunnelId == -1) {
        unlink(sTunnelConfFile);
        return (0);
    }
    // Save / overwrite the tunnel ID in the file
    if ((pFh = fopen(sTunnelConfFile, "w")) != NULL) {
        fprintf(pFh, "%d", tunnelId);
        fclose(pFh);
        return (0);
    }

    return (1);
}

//!
//! This function retrieves the current cluster ID saved on the system
//!
//! @return The index of the cluster in the cluster list or -1 if any error (not found) occurred.
//!
//! @see managed_save_tunnel_id()
//!
//! @pre
//!     - if the tunnel ID configuration file exists, it should be readable and only contain the tunnel index
//!
//! @post
//!
//! @note
//!

int managed_get_current_tunnel_id(void) {
    int tunnelId = -1;
    char *psTunnelId = NULL;
    char sTunnelConfFile[EUCA_MAX_PATH] = "";

    // Setup the tunnel configuration file
    snprintf(sTunnelConfFile, EUCA_MAX_PATH, VTUND_TUNNEL_ID_FILE_FORMAT, config->eucahome);

    // Is our tunnel id configuration file present?
    if (check_file(sTunnelConfFile) == 0) {
        // Retrieve the configured tunnel ID
        if ((psTunnelId = file2str(sTunnelConfFile)) != NULL) {
            tunnelId = atoi(psTunnelId);
            EUCA_FREE(psTunnelId);
            return (tunnelId);
        }
    }

    return (-1);
}

//!
//! This function retrieves the configured cluster ID of the local cluster. This is the ordered
//! position in the GNI cluster list.
//!
//! @param[in] pGni a pointer to the global network view which contains the AZs information
//! @param[in] pCluster a pointer to the local cluster structure
//!
//! @return The index of the cluster in the cluster list or -1 if any error (not found) occurred.
//!
//! @see
//!
//! @pre
//!     The pGni and pCluster pointer must not be NULL
//!
//! @post
//!
//! @note
//!

int managed_get_new_tunnel_id(globalNetworkInfo * pGni, gni_cluster * pCluster) {
    int i = 0;

    // Make sure our pointers aren't NULL
    if (pGni && pCluster) {
        // Scan the cluster list until we find the position that matches ours
        for (i = 0; i < pGni->max_clusters; i++) {
            if (pCluster == &pGni->clusters[i])
                return (i);
        }
    }
    return (-1);
}

//!
//! Creates a VTUND tunnel. This will execute the following tasks:
//!
//!     -# If the PID file referred by psPidFile is present, check if the process is running and for the proper endpoint (if so we're good)
//!     -# If this process is not for the same endpoint as original, terminate the VTUND client process
//!     -# Run the VTUND client process for this endpoint
//!
//! @param[in] pCluster a pointer to the local cluster structure
//! @param[in] psPidFile a constant string pointer to the PID file path to contain the VTUND client PID
//! @param[in] psConfigPath a constant string pointer to the VTUND configuration file path
//! @param[in] localId tunnel local endpoint index
//! @param[in] remoteId tunnel remote endpoint index
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see
//!
//! @pre
//!     - pCluster and psConfigPath must not be 0
//!     - psConfigPath should not be 0-length
//!
//! @post
//!     On success, the VTUND client process for this endpoint is executed and its PID is saved in the
//!     given PID file. On failure, the client is not executed.
//!
//! @note
//!

static int managed_create_tunnel(gni_cluster * pCluster, const char *psPidFile, const char *psConfigPath, int localId, int remoteId) {
#define SESSION_STRING_LEN        16

    int rc = 0;
    char *psPidId = NULL;
    char sTunName[IF_NAME_LEN] = "";
    char sCommand[EUCA_MAX_PATH] = "";
    char sRemoteIp[INET_ADDR_LEN] = "";
    char sCmdLinePath[EUCA_MAX_PATH] = "";
    char sSessionId[SESSION_STRING_LEN] = "";
    FILE *pFh = NULL;

    // Make sure our pointers aren't NULL and our configuration file path string is valid
    if (!pCluster || !psConfigPath || (strlen(psConfigPath) == 0)) {
        LOGERROR("Failed to create tunnel. Invalid parameters provided.\n");
        return (1);
    }
    // Convert the cluster IP address so its readable
    snprintf(sRemoteIp, INET_ADDR_LEN, "%s", euca_ntoa(pCluster->enabledCCIp));

    // Build the tunnel device name
    snprintf(sTunName, IF_NAME_LEN, "%s%d-%d", TUNNEL_NAME_PREFIX, localId, remoteId);

    // Build our session ID string
    snprintf(sSessionId, SESSION_STRING_LEN, "tun-%d-%d", localId, remoteId);

    LOGTRACE("Creating tunnel session '%s' for endpoint '%s'\n", sSessionId, sRemoteIp);

    // Check if we already have a pid file
    if ((rc = check_file(psPidFile)) == 0) {
        //
        // read and make sure the command matches. If it does not match, we will need to restart.
        //
        if ((psPidId = file2str(psPidFile)) != NULL) {
            snprintf(sCmdLinePath, EUCA_MAX_PATH, "/proc/%s/cmdline", psPidId);

            // Check if the process is running
            if (check_file(sCmdLinePath) == 0) {
                //
                // Open and read the process' command line content to make sure its VTUND and its
                // for the same endpoint. If its not for the same endpoint, restart
                //
                if ((pFh = fopen(sCmdLinePath, "r")) != NULL) {
                    if (fgets(sCommand, EUCA_MAX_PATH, pFh)) {
                        if (strstr(sCommand, VTUND_APPLICATION)) {
                            if (strstr(sCommand, sSessionId)) {
                                // The process is running and its for the same endpoint, we won't kick it
                                LOGTRACE("Tunnel session '%s' running properly. Nothing to do.\n", sSessionId);
                                EUCA_FREE(psPidId);
                                fclose(pFh);
                                return (0);
                            } else {
                                //
                                // Ok, something's not lining up. Stop this tunnel and reset it. Perhaps someone
                                // deregistered a CC causing a shuffling
                                //
                                LOGINFO("Detected tunnel for session '%s' but maybe not for endpoint '%s'. Restarting session with proper endpoint.\n", sSessionId, sRemoteIp);
                                if (eucanetd_kill_program(atoi(psPidId), VTUND_APPLICATION, config->cmdprefix) != EUCA_OK) {
                                    LOGERROR("Failed to stop tunnel session '%s' for endpoint '%s'.\n", sSessionId, sRemoteIp);
                                    EUCA_FREE(psPidId);
                                    fclose(pFh);
                                    return (1);
                                }

                                unlink(psPidFile);
                            }
                        }
                    }
                    fclose(pFh);
                    pFh = NULL;
                }
            } else {
                // pidfile passed in but process is not running
                unlink(psPidFile);
            }

            EUCA_FREE(psPidId);
        }
    }
    // Build the command string for debugging purpose
    snprintf(sCommand, EUCA_MAX_PATH, "%s %s -n -f %s -p %s %s", config->cmdprefix, VTUND_APPLICATION, psConfigPath, sSessionId, sRemoteIp);

    //
    // Starting up the tunnel. We are passing the "-n" option to prevent VTUND from daemonizing itself. We need to manage
    // the process (i.e. being able to kill and restart it as necessary) and, since VTUND does not create its own pid file
    // and since we will loose track of the PID in this case, we need to make sure it does not daemonize and we will control
    // our own precess managing it.
    //
    if ((rc = eucanetd_run_program(psPidFile, config->cmdprefix, FALSE, config->cmdprefix, VTUND_APPLICATION, "-n", "-f", psConfigPath, "-p", sSessionId, sRemoteIp, NULL)) != 0) {
        LOGERROR("Cannot create tunnel session '%s' for endpoint '%s' command '%s'. Look at above error logs for more details.\n", sSessionId, sRemoteIp, sCommand);
        return (1);
    }

    return (0);

#undef SESSION_STRING_LEN
}

//!
//! This setups and establishes the tunnels between the different registered AZs (CC). This
//! function will execute the following tasks:
//!
//!     -# If we have more than one AZ
//!         -# If we have tunneling enabled
//!             -# Start the vtund server if not started already
//!             -# Configures the tunnels between the AZs if we have more than one AZs
//!             -# If we have any active security-groups
//!                 -# Detach any tunnels associated with inactive security-groups
//!                 -# Attach all tunnels for active security-groups
//!             -# If we no longer have active security-groups
//!                 -# Detach any remaining security groups
//!         -# If tunneling is disabled
//!             -# Call managed_unset_tunnels()
//!     -# If we have 0 or 1 AZ
//!         -# Call managed_unset_tunnels()
//!
//! @param[in] pGni a pointer to the global network view which contains the AZs information
//!
//! @return 0 on success or 1 on failure.
//!
//! @see managed_initialize_tunnels(), managed_cleanup_tunnels()
//! @see managed_unset_tunnels()
//! @see managed_attach_tunnels(), managed_attach_tunnel()
//! @see managed_detach_tunnels(), managed_detach_tunnel()
//!
//! @pre
//!     - The pGni pointer must not be NULL
//!     - Tunneling should be enabled (if not, we're good)
//!     - We should have a minimum of 2 AZs in order to establish tunnels (if not, we're good)
//!
//! @post
//!     On success, the tunnels are setup properly between the AZs. On failure, the system
//!     should be left unchanged.
//!
//! @note
//!

int managed_setup_tunnels(globalNetworkInfo * pGni) {
#define SESSION_STRING_LEN        16

    int i = 0;
    int rc = 0;
    int ret = 0;
    int nbGroups = 0;
    int nbTunnels = 0;
    int oldLocalId = -1;
    int newLocalId = -1;
    char *psPid = NULL;
    char sPidFile[EUCA_MAX_PATH] = "";
    char sConfigPath[EUCA_MAX_PATH] = "";
    boolean done = FALSE;
    dev_entry *pTunnels = NULL;
    gni_cluster *pCluster = NULL;
    gni_cluster *pClusters = NULL;
    gni_secgroup *pSecGroups = NULL;

    LOGTRACE("Setting up tunnels.\n");

    // Do we have tunneling disabled or do we have less than 2 clusters??
    if ((config->disableTunnel == TRUE) || (pGni->max_clusters < 2)) {
        return (managed_unset_tunnels(pGni));
    }
    // Get the cluster configuration
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("Cannot find cluster for our network in global network view: check network configuration settings\n");
        return (1);
    }
    // Retrieve our security groups for this cluster only
    if ((rc = gni_cluster_get_secgroup(pGni, pCluster, NULL, 0, NULL, 0, &pSecGroups, &nbGroups)) != 0) {
        LOGERROR("Cannot find security groups for cluster %s in global network view: check network configuration settings.\n", pCluster->name);
        return (1);
    }
    // Retrieve our tunnel devices
    if ((rc = dev_get_tunnels(NULL, &pTunnels, &nbTunnels)) != 0) {
        LOGERROR("Fail to retrieve our tunnel devices. Look at above log errors for more details.\n");
        EUCA_FREE(pSecGroups);
        return (1);
    }
    // Retrieve our currently used tunnel ID on the system
    oldLocalId = managed_get_current_tunnel_id();

    // Retrieve the new tunnel ID from the GNI
    newLocalId = managed_get_new_tunnel_id(pGni, pCluster);

    // Check if we have a local ID change
    if (oldLocalId != newLocalId) {
        //
        // In this case, we need to shutdown all the tunnels and re-configure everything. This
        // typically occurs when someone unregister a CC which triggered a re-ordering of the
        // CC components
        //
        LOGWARN("Tunnel shuffle detected. Resetting tunnels. Old ID:%d, New ID:%d\n", oldLocalId, newLocalId);
        if (managed_unset_tunnels(pGni)) {
            LOGERROR("Fail to reset tunnels after CC shuffling detected. Look at above log errors for more details.\n");
            EUCA_FREE(pSecGroups);
            dev_free_list(&pTunnels, nbTunnels);
            return (1);
        }
        // save our new tunnel ID
        if (managed_save_tunnel_id(newLocalId)) {
            LOGERROR("Failed to save our tunnel index.\n");
            EUCA_FREE(pSecGroups);
            dev_free_list(&pTunnels, nbTunnels);
            return (1);
        }
    }
    //
    // Now setup the VTUND server.
    //
    snprintf(sPidFile, EUCA_MAX_PATH, VTUND_SERVER_PID_PATH, config->eucahome);

    //
    // Starting up the tunnel server. We are passing the "-n" option to prevent VTUND from daemonizing itself. We need to manage
    // the process (i.e. being able to kill and restart it as necessary) and, since VTUND does not create its own pid file
    // and since we will loose track of the PID in this case, we need to make sure it does not daemonize and we will control
    // our own precess managing it.
    //
    snprintf(sConfigPath, EUCA_MAX_PATH, VTUND_CONFIG_PATH, config->eucahome);
    if ((rc = eucanetd_run_program(sPidFile, config->cmdprefix, FALSE, config->cmdprefix, VTUND_APPLICATION, "-s", "-n", "-f", sConfigPath, NULL)) != 0) {
        LOGERROR("Cannot run tunnel server\n");
        EUCA_FREE(pSecGroups);
        dev_free_list(&pTunnels, nbTunnels);
        return (1);
    }
    //
    // Create our point-to point tunnels
    //
    for (i = 0, pClusters = pGni->clusters; i < pGni->max_clusters; i++) {
        // Skip our own cluster
        if (newLocalId != i) {
            snprintf(sPidFile, EUCA_MAX_PATH, VTUND_CLIENT_PID_FILE_FORMAT, config->eucahome, newLocalId, i);

            if ((rc = managed_create_tunnel(&pClusters[i], sPidFile, sConfigPath, newLocalId, i)) != 0) {
                // Log it and go to the next one
                LOGERROR("Cannot create tunnel session 'tun-%d-%d' for endpoint '%s'. Look at above error logs for more details.\n", newLocalId, i,
                        euca_ntoa(pClusters[i].enabledCCIp));
            } else {
                LOGTRACE("Created tunnel session 'tun-%d-%d' for endpoint '%s'.\n", newLocalId, i, euca_ntoa(pClusters[i].enabledCCIp));
            }
        }
    }

    //
    // Keep going with the tunnels in case we need to stop a few
    //
    done = FALSE;
    while (!done) {
        snprintf(sPidFile, EUCA_MAX_PATH, VTUND_CLIENT_PID_FILE_FORMAT, config->eucahome, newLocalId, i);

        // Do we have a valid PID file for this extra tunnel?
        if (check_file(sPidFile) == 0) {
            // Can we read the content?
            if ((psPid = file2str(sPidFile)) != NULL) {
                // Now kill this sucker
                if (eucanetd_kill_program(atoi(psPid), VTUND_APPLICATION, config->cmdprefix) != EUCA_OK) {
                    LOGERROR("Failed to stop tunnel session 'tun-%d-%d'.\n", newLocalId, i);
                } else {
                    // Ok, we're done with this tunnel, remove the PID file
                    unlink(sPidFile);
                }

                EUCA_FREE(psPid);
            } else {
                // No PID in file, remove the file
                unlink(sPidFile);
            }

            i++;
        } else {
            // PID file not found for this tunnel, we are good now...
            done = TRUE;
        }
    }

    //
    // Now detach any of our unused tunnels
    //
    if ((rc = managed_detach_tunnels_fn(pGni, pCluster, pSecGroups, nbGroups, pTunnels, nbTunnels)) != 0) {
        //
        // We will warn and continue through. We can live with some extra tunnels and hopefully
        // the next call will clear this
        //
        LOGWARN("Fail to detach inactive tunnels. Look at above log errors for more details.\n");
        ret = 1;
    }
    //
    // Now attach any of our active tunnels
    //
    if ((rc = managed_attach_tunnels_fn(pGni, pCluster, pSecGroups, nbGroups)) != 0) {
        LOGERROR("Fail to attach tunnels for active security-groups. Look at above log errors for more details.\n");
        ret = 1;
    }

    EUCA_FREE(pSecGroups);
    dev_free_list(&pTunnels, nbTunnels);
    return (ret);

#undef SESSION_STRING_LEN
}

//!
//! This will teardown the tunnels established between all of the AZs. This function will
//! execute the following tasks:
//!
//!     -# Detach any configured tunnels
//!     -# Stop the main VTUND server if active
//!     -# Stop all the VTUND client application (1 per tunnel), if any
//!
//! @param[in] pGni a pointer to the global network view which contains the AZs information
//!
//! @return 0 on success or 1 on failure.
//!
//! @see managed_initialize_tunnels(), managed_cleanup_tunnels()
//! @see managed_setup_tunnels()
//! @see managed_attach_tunnels(), managed_attach_tunnel()
//! @see managed_detach_tunnels(), managed_detach_tunnel()
//!
//! @pre
//!     We should have some tunnels established (hopefully)
//!
//! @post
//!     On success, the tunnels are all teardown and removed from the system. On failure, the
//!     system is left in a non-deterministic state.
//!
//! @note
//!

int managed_unset_tunnels(globalNetworkInfo * pGni) {
    int i = 0;
    int rc = 0;
    int ret = 0;
    int nbTunnels = 0;
    int oldLocalId = -1;
    int newLocalId = -1;
    char *psPid = NULL;
    char sPidPath[EUCA_MAX_PATH] = "";
    boolean done = FALSE;
    dev_entry *pTunnels = NULL;
    gni_cluster *pCluster = NULL;

    LOGTRACE("Unsetting tunnels.\n");

    // Get the cluster configuration
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("Cannot find cluster for our network in global network view: check network configuration settings\n");
        return (1);
    }
    // Retrieve our tunnel devices
    if ((rc = dev_get_tunnels(NULL, &pTunnels, &nbTunnels)) != 0) {
        LOGERROR("Fail to retrieve our tunnel devices. Look at above log errors for more details.\n");
        return (1);
    }
    // Retrieve our currently used tunnel ID on the system
    oldLocalId = managed_get_current_tunnel_id();

    // Retrieve the new tunnel ID from the GNI
    newLocalId = managed_get_new_tunnel_id(pGni, pCluster);

    //
    // Now detach all of our tunnels
    //
    if ((rc = managed_detach_tunnels_fn(pGni, pCluster, NULL, 0, pTunnels, nbTunnels)) != 0) {
        // We will warn and continue through
        LOGWARN("Fail to detach inactive tunnels. Look at above log errors for more details.\n");
        ret = 1;
    }
    // Now we're done with the tunnels
    dev_free_list(&pTunnels, nbTunnels);

    // Now stop the vtund server
    snprintf(sPidPath, EUCA_MAX_PATH, VTUND_SERVER_PID_PATH, config->eucahome);

    // Make sure we have the server running
    if (check_file(sPidPath) == 0) {
        // then kill it
        if ((psPid = file2str(sPidPath)) != NULL) {
            if ((rc = eucanetd_kill_program(atoi(psPid), VTUND_APPLICATION, config->cmdprefix)) != 0) {
                LOGERROR("Failed to stop tunnel server.\n");
                ret = 1;
            }
            EUCA_FREE(psPid);
        }
    }
    // Ok, try to stop any vtund client using the current ID
    if (oldLocalId != -1) {
        for (i = 0, done = FALSE; !done; i++) {
            // Skip our own id
            if (oldLocalId != i) {
                snprintf(sPidPath, EUCA_MAX_PATH, VTUND_CLIENT_PID_FILE_FORMAT, config->eucahome, oldLocalId, i);

                // Do we have a PID file?
                if (check_file(sPidPath) == 0) {
                    if ((psPid = file2str(sPidPath)) != NULL) {
                        if ((rc = eucanetd_kill_program(atoi(psPid), VTUND_APPLICATION, config->cmdprefix)) != 0) {
                            LOGERROR("Fail to stop tunnel session 'tun-%d-%d'.\n", oldLocalId, i);
                            ret = 1;
                        }
                        EUCA_FREE(psPid);
                    }
                } else {
                    done = TRUE;
                }
            }
        }
    }
    // In case we had a switch of ids, try to stop any other clients that uses the new ID
    if ((oldLocalId != newLocalId) && (newLocalId != -1)) {
        for (i = 0, done = FALSE; !done; i++) {
            // Skip our own id
            if (newLocalId != i) {
                snprintf(sPidPath, EUCA_MAX_PATH, VTUND_CLIENT_PID_FILE_FORMAT, config->eucahome, newLocalId, i);

                // Do we have a PID file?
                if (check_file(sPidPath) == 0) {
                    if ((psPid = file2str(sPidPath)) != NULL) {
                        if ((rc = eucanetd_kill_program(atoi(psPid), VTUND_APPLICATION, config->cmdprefix)) != 0) {
                            LOGERROR("Fail to stop tunnel session 'tun-%d-%d'.\n", newLocalId, i);
                            ret = 1;
                        }
                        EUCA_FREE(psPid);
                    }
                } else {
                    done = TRUE;
                }
            }
        }
    }
    return (ret);
}

//!
//! This will create a tunnel for a security group and associate it with the given bridge
//! device. The function execute the following tasks:
//!
//!     -# Ensures the tunnels isn't already created (if so, we're good)
//!     -# Ensures STP is enabled on the bridge device
//!     -# Create the VLAN on each AZ tunnel interface
//!     -# Associate each VLAN interface with the given bridge device
//!     -# Enable the VLAN interfaces
//!
//! @param[in] pBridge a pointer to our bridge device
//! @param[in] pSubnet a pointer to our subnet structure
//! @param[in] localId the local tunnel identifier
//! @param[in] remoteId the remote tunnel identifier
//!
//! @return 0 on success or 1 on failure.
//!
//! @see managed_initialize_tunnels(), managed_cleanup_tunnels()
//! @see managed_setup_tunnels(), managed_unset_tunnels()
//! @see managed_attach_tunnels()
//! @see managed_detach_tunnels(), managed_detach_tunnel()
//!
//! @pre
//!     - Both pointer must not be NULL
//!     - Tunneling should be enabled (if not, we're good)
//!     - The given pBridge device must be a valid network bridge device
//!     - Tunneling should not be already configured (if so, we're good)
//!
//! @post
//!     On success, the tunnels are attached properly on this bridge. On failure, the
//!     tunnels are not attached.
//!
//! @note
//!

int managed_attach_tunnel(dev_entry * pBridge, managed_subnet * pSubnet, int localId, int remoteId) {
    int i = 0;
    int j = 0;
    int left = 0;
    int right = 0;
    int nbDevices = 0;
    char sTunName[IF_NAME_LEN] = "";
    char sTapName[IF_NAME_LEN] = "";
    boolean found = FALSE;
    dev_entry *pDevices = NULL;
    dev_entry *pTapDevice = NULL;

    // Make sure our pointers are valid
    if (!pBridge || !pSubnet) {
        LOGERROR("Fail to attach tunnel. Invalid parameters provided.\n");
        return (1);
    }
    // Make sure local and remote IDs are valie (i.e. not -1)
    if ((localId == -1) || (remoteId == -1)) {
        LOGERROR("Fail to attach tunnel to bridge device '%s'. Invalid IDs provided. localId=%d, remoteID=%d\n", pBridge->sDevName, localId, remoteId);
        return (1);
    }
    // Make sure this is a bridge device
    if (!dev_is_bridge(pBridge->sDevName)) {
        LOGERROR("Fail to attach tunnel. Invalid bridge device '%s' provided.\n", pBridge->sDevName);
        return (1);
    }
    // Make sure we enable spanning tree... Should have been done already on creation
    if (dev_set_bridge_stp(pBridge->sDevName, BRIDGE_STP_ON) != 0) {
        LOGERROR("Fail to attach tunnel. Invalid bridge device '%s' provided.\n", pBridge->sDevName);
        return (1);
    }
    // Setup the local tunnel and then swap to do the remote tunnel
    for (j = 0, left = localId, right = remoteId; j < 2; j++, left = remoteId, right = localId) {
        snprintf(sTunName, IF_NAME_LEN, "%s%d-%d", TUNNEL_NAME_PREFIX, left, right);
        snprintf(sTapName, IF_NAME_LEN, "%s.%d", sTunName, pSubnet->vlanId);

        LOGTRACE("Attaching tunnel device '%s' to bridge device '%s'.\n", sTapName, pBridge->sDevName);

        // Does the main tunnel exists?
        if (!dev_exist(sTunName)) {
            LOGERROR("Fail to attach tunnel device '%s' to bridge device '%s'. Tunnel device '%s' does not exists.\n", sTapName, pBridge->sDevName, sTunName);
            return (1);
        }
        // Does the tunnel VLAN device exists already?
        if (!dev_exist(sTapName)) {
            // No, try to create it and we will attach it to the bridge
            if ((pTapDevice = dev_create_vlan(sTunName, pSubnet->vlanId)) == NULL) {
                LOGERROR("Fail to attach tunnel device '%s' to bridge device '%s'. Could not create VLAN.\n", sTapName, pBridge->sDevName);
                return (1);
            }

            EUCA_FREE(pTapDevice);
        } else {
            // Retrieve our assigned interfaces so we can analyze
            if (dev_get_bridge_interfaces(pBridge->sDevName, &pDevices, &nbDevices)) {
                LOGERROR("Fail to attach tunnel device '%s' to bridge device '%s'. Failed to retrieve interfaces assigned to bridge device '%s'.\n",
                        sTapName, pBridge->sDevName, pBridge->sDevName);
                return (1);
            }
            // Check if we have this interface in our bridge interfaces already
            for (i = 0, found = FALSE; ((i < nbDevices) && !found); i++) {
                if (strcmp(pDevices[i].sDevName, sTapName)) {
                    found = TRUE;
                }
            }

            // Done with the devices
            dev_free_list(&pDevices, nbDevices);

            // Was it found?
            if (found)
                continue;
        }

        //
        // Ok, the device exists and is not already associated with our bridge device. We will
        // associate them both together
        //
        if (dev_bridge_assign_interface(pBridge->sDevName, sTapName) != 0) {
            LOGERROR("Fail to associate tunnel device '%s' with bridge device '%s'. Look at above error logs for more details.\n", sTapName, pBridge->sDevName);
            return (1);
        }
        // Now enable this device
        if (dev_up(sTapName) != 0) {
            LOGERROR("Tunnel device '%s' associated with bridge device '%s' but remained 'down'. Look at above error logs for more details.\n", sTapName, pBridge->sDevName);
            return (1);
        }
    }

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
//! @see managed_initialize_tunnels(), managed_cleanup_tunnels()
//! @see managed_setup_tunnels(), managed_unset_tunnels()
//! @see managed_attach_tunnel()
//! @see managed_detach_tunnels(), managed_detach_tunnel()
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

static int managed_attach_tunnels(globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups) {
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
        } else if (dev_get_bridges(pSecGroup->name, &pBridge, &nbBridges)) {
            LOGWARN("Fail to attach tunnel for security-group '%s'. Could not retrieve associated bridge device.\n", pSecGroup->name);
            ret = 1;
        } else if (nbBridges != 1) {
            // This should never happen because only one bridge device should match this group but just in case
            LOGWARN("Fail to attach tunnel for security-group '%s'. Too many bridge devices found (%d).\n", pSecGroup->name, nbBridges);
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
        dev_free_list(&pBridge, nbBridges);
    }

    return (ret);
}

//!
//! This will detach a tunnel from a given bridge device. This function will execute
//! the following tasks:
//!
//!     -# Make sure each tunnel VLAN interface are associated with the given bridge device
//!     -# Disable each VLAN interface associated with this security group on the tunnel devices
//!     -# Unassociate each VLAN interface from the bridge device
//!     -# Remove the tunnel VLAN interfaces from the system
//!
//! @param[in] pBridge a pointer to our bridge device
//! @param[in] pTunnel a pointer to our tunnel device to detach from the bridge
//!
//! @return 0 on success or 1 on failure.
//!
//! @see managed_initialize_tunnels(), managed_cleanup_tunnels()
//! @see managed_setup_tunnels(), managed_unset_tunnels()
//! @see managed_attach_tunnels(), managed_attach_tunnel()
//! @see managed_detach_tunnel()
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

int managed_detach_tunnel(dev_entry * pBridge, dev_entry * pTunnel) {
    // Make sure our pointers are valid
    if (!pBridge || !pTunnel) {
        LOGERROR("Fail to detach tunnel. Invalid parameters provided.\n");
        return (1);
    }
    // Make sure this is a bridge device
    if (!dev_is_bridge(pBridge->sDevName)) {
        LOGERROR("Fail to detach tunnel. Invalid bridge device '%s' provided.\n", pBridge->sDevName);
        return (1);
    }
    // Make sure our tunnel device is a valid tunnel device
    if (!dev_is_tunnel(pTunnel->sDevName)) {
        LOGERROR("Fail to attach tunnel. Invalid bridge device '%s' provided.\n", pBridge->sDevName);
        return (1);
    }

    LOGTRACE("Detaching tunnel device '%s' from bridge device '%s'.\n", pBridge->sDevName, pTunnel->sDevName);

    // Disassociate them...
    if (dev_bridge_delete_interface(pBridge->sDevName, pTunnel->sDevName) != 0) {
        LOGERROR("Fail to disassociate tunnel device '%s' with bridge device '%s'. Look at above error logs for more details.\n", pTunnel->sDevName, pBridge->sDevName);
        return (1);
    }
    return (0);
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
//! @see managed_initialize_tunnels(), managed_cleanup_tunnels()
//! @see managed_setup_tunnels(), managed_unset_tunnels()
//! @see managed_attach_tunnels(), managed_attach_tunnel()
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

static int managed_detach_tunnels(globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups, dev_entry * pTunnels, int nbTunnels) {
    int i = 0;
    int j = 0;
    int ret = 0;
    //int localId = -1;
    int nbBridges = 0;
    char *psBridgeName = NULL;
    boolean done = FALSE;
    boolean found = FALSE;
    boolean remove = FALSE;
    dev_entry *pBridges = NULL;

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
    // Retrieve the local tunnel ID from the GNI
    //localId = managed_get_new_tunnel_id(pGni, pCluster);

    // Are we detaching all tunnels or do we need to pick and choose?
    if (nbGroups == 0) {
        for (i = 0; i < nbTunnels; i++) {
            // Is this a subnet tunnel (i.e. its tap-[id]-[id].[vlan] format rather than just tap-[id]-[id])?
            if (strstr(pTunnels[i].sDevName, ".") != NULL) {
                // If its attached, we should be able to get the associated bridge device
                if ((psBridgeName = dev_get_interface_bridge(pTunnels[i].sDevName)) != NULL) {
                    // Retrieve the bridge device from its name
                    if (dev_get_bridges(psBridgeName, &pBridges, &nbBridges) == 0) {
                        LOGDEBUG("Detaching tunnel device '%s' from bridge device '%s'.\n", pTunnels[i].sDevName, pBridges[0].sDevName);
                        if (managed_detach_tunnel(&pBridges[0], &pTunnels[i]) != 0) {
                            LOGERROR("Failed to detach tunnel device '%s' from bridge device '%s'. Look at above log errors for more details.\n",
                                    pTunnels[i].sDevName, pBridges[0].sDevName);
                            ret = 1;
                        }

                        dev_free_list(&pBridges, nbBridges);
                    } else {
                        LOGERROR("Failed to detach tunnel device '%s' from bridge device '%s'. Fail to lookup bridge device.\n", pTunnels[i].sDevName, pBridges[0].sDevName);
                        ret = 1;
                    }
                    EUCA_FREE(psBridgeName);
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
            if (strstr(pTunnels[i].sDevName, ".") != NULL) {
                // We remove them by default unless we find a valid security group
                remove = TRUE;

                // If its attached, we should be able to get the associated bridge device
                if ((psBridgeName = dev_get_interface_bridge(pTunnels[i].sDevName)) != NULL) {
                    // Now see if we have a security-group that has 1 or more instances associated with this bridge
                    for (j = 0, found = FALSE, done = FALSE; ((j < nbGroups) && !done); j++) {
                        if (!strcmp(pSecGroups[j].name, psBridgeName)) {
                            done = TRUE;
                            if (pSecGroups[j].max_instances > 0) {
                                found = TRUE;
                                remove = FALSE;
                            }
                        }
                    }

                    // If not found, then detach...
                    if (!found) {
                        if (dev_get_bridges(psBridgeName, &pBridges, &nbBridges) == 0) {
                            LOGDEBUG("Detaching tunnel device '%s' from bridge device '%s'.\n", pTunnels[i].sDevName, pBridges[0].sDevName);
                            if (managed_detach_tunnel(&pBridges[0], &pTunnels[i]) != 0) {
                                LOGERROR("Failed to detach tunnel device '%s' from bridge device '%s'. Look at above log errors for more details.\n",
                                        pTunnels[i].sDevName, pBridges[0].sDevName);
                                ret = 1;
                            }

                            dev_free_list(&pBridges, nbBridges);
                        } else {
                            LOGERROR("Failed to detach tunnel device '%s' from bridge device '%s'. Fail to lookup bridge device.\n", pTunnels[i].sDevName, pBridges[0].sDevName);
                            ret = 1;
                        }
                    }

                    EUCA_FREE(psBridgeName);
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

    return (ret);
}

//!
//! Checks wether or not if a given bridge device is configured appropriately.
//!
//! @param[in] pBridge a pointer to the bridge device to validate
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see
//!
//! @pre
//!     The pBridge parameter must not be NULL
//!
//! @post
//!
//! @note
//!

static boolean managed_is_bridge_setup(dev_entry * pBridge) {
    int i = 0;
    int nbDevices = 0;
    dev_entry *pDevice = NULL;
    dev_entry *pDevices = NULL;

    // First make sure the pointer isn't NULL
    if (!pBridge) {
        LOGERROR("Failed to validate unknown bridge device.\n");
        return (FALSE);
    }
    // Now, does the bridge device exist?
    if (!dev_exist(pBridge->sDevName))
        return (FALSE);

    // Retrieve our assigned interfaces so we can analyze
    if (dev_get_bridge_interfaces(pBridge->sDevName, &pDevices, &nbDevices)) {
        LOGERROR("Failed to retrieve interfaces assigned to bridge device '%s'.\n", pBridge->sDevName);
        return (FALSE);
    }
    // If we don't have any interfaces assigned, than its not setup
    if (nbDevices == 0) {
        dev_free_list(&pDevices, nbDevices);
        return (FALSE);
    }

    if (PEER_IS_NC(eucanetdPeer)) {
        // A bridge is only valid here if we have at least the private interface assigned
        for (i = 0, pDevice = pDevices; i < nbDevices; i++, pDevice++) {
            if (!strncmp(config->privInterface, pDevice->sDevName, strlen(config->privInterface))) {
                dev_free_list(&pDevices, nbDevices);
                return (TRUE);
            }
        }
    } else {
        // A bridge is only valid here if we have at least the private interface assigned
        for (i = 0, pDevice = pDevices; i < nbDevices; i++, pDevice++) {
            if (!strncmp(config->privInterface, pDevice->sDevName, strlen(config->privInterface))) {
                // We're good
                dev_free_list(&pDevices, nbDevices);
                return (TRUE);
            }
        }
    }

    dev_free_list(&pDevices, nbDevices);
    return (FALSE);
}

//!
//! Unconfigures and removes a bridge device from the system. The following tasks
//! will be executed:
//!     -# Cleanup the tunnels associated with this bridge as necessary
//!     -# Disable and unassociate each associated interfaces. If this is a VLAN interface, its removed from the private interface as well.
//!     -# Disable the bridge device
//!     -# Remove the bridge device from the system.
//!
//! @param[in] pBridge a pointer to our bridge device information to remove
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see
//!
//! @pre
//!     The pBridge parameter must not be NULL
//!
//! @post
//!     On success the device is removed from the system. On failure, the state
//!     of the system (in regards to this device) may not be deterministic
//!
//! @note
//!

static int managed_remove_bridge(dev_entry * pBridge) {
    int i = 0;
    int ret = 0;
    int nbDevices = 0;
    boolean forced = FALSE;
    dev_entry *pDevice = NULL;
    dev_entry *pDevices = NULL;

    // Make sure our inputs are valid
    if (!pBridge) {
        LOGERROR("Failed to remove bridge device '%s'. Invalid input provided!.\n", ((pBridge) ? pBridge->sDevName : "UNKNOWN"));
        return (1);
    }
    // Get the list of assigned devices
    if (dev_get_bridge_interfaces(pBridge->sDevName, &pDevices, &nbDevices)) {
        LOGERROR("Failed to retrieve interfaces assigned to bridge device '%s'.\n", pBridge->sDevName);
        return (1);
    }
    // Cleanup all assigned interfaces
    for (i = 0, pDevice = pDevices; i < nbDevices; i++, pDevice++) {
        // Disable the interface
        if (dev_down(pDevice->sDevName)) {
            // Just warn, we may still be able to execute the rest
            LOGWARN("Failed to disable network device '%s' assigned to bridge device '%s'.\n", pDevice->sDevName, pBridge->sDevName);
        }
        // Unassign the device from the bridge
        if (dev_bridge_delete_interface(pBridge->sDevName, pDevice->sDevName)) {
            // definitely an issue but still try to go on as far as we can and mark failure
            LOGERROR("Failed to unassign network device '%s' from bridge device '%s'.\n", pDevice->sDevName, pBridge->sDevName);
            ret = 1;
        } else {
            // If the device name starts with our private interface name, its a VLAN interface, remove it
            if (!strncmp(config->privInterface, pDevice->sDevName, strlen(config->privInterface))) {
                if (dev_remove_vlan_interface(pDevice->sDevName)) {
                    //
                    // Warn but this does not constitute a failure. The device is down and useless at this
                    // point. It may be use again for another network later.
                    //
                    LOGWARN("Failed to remove VLAN device '%s' from system.\n", pDevice->sDevName)
                }
            }
        }
    }

    // We're done with our device list
    dev_free_list(&pDevices, nbDevices);

    // Now take the bridge device down
    if (dev_down(pBridge->sDevName)) {
        LOGWARN("Failed to disable bridge device '%s'.\n", pBridge->sDevName);
        ret = 1;
    }
    // If we have a failure so far, try a hail mary... If not, do it gently ;)
    if (ret) {
        // Give it a change
        forced = TRUE;
    }
    // Now remove it from the system
    if ((ret = dev_remove_bridge(pBridge->sDevName, forced)) != 0) {
        LOGERROR("Failed to remove bridge device '%s' from the system. (forced=%s)\n", pBridge->sDevName, ((forced) ? "TRUE" : "FALSE"));
    }

    return (ret);
}

//!
//! Creates a bridge device for a given subnet. Once the bridge device is created,
//! the VLAN device will be created and added to the bridge. If we're running on a
//! CC component, the subnet gateway will be added on the bridge device and we will
//! also set the proper tunnel if tunneling is enabled. The Bridge device should not
//! exist by now. The following tasks will be executed:
//!     -# Create the Bridge Device
//!     -# If on CC, then assign the security-group private subnet gateway IP with proper netmask
//!     -# Add the security-group associated VLAN on the private interface
//!     -# Assign the VLAN interface to the bridge device
//!     -# Enable the Bridge device
//!     -# Enable the VLAN interface
//!     -# Setup the tunnels for the security-group private subnet as necessary
//!
//! @param[in] psBridgeName a constant string pointer to the bridge device name to create.
//! @param[in] pSubnet a pointer to the security-group managed subnet structure
//!
//! @return 0 on success or 1 if any failure occurred.
//!
//! @see
//!
//! @pre
//!     - Both psBridgeName and pSubnet MUST not be NULL and psBridgeName cannot be empty.
//!     - The bridge device must not exists.
//!
//! @post
//!     On success the bridge device is created and configured appropriately. On failure,
//!     the bridge device will not exists.
//!
//! @note
//!

static int managed_create_bridge(const char *psBridgeName, managed_subnet * pSubnet) {
    const char *psStpState = BRIDGE_STP_OFF;
    dev_entry *pBridge = NULL;
    dev_entry *pVlanDev = NULL;

    // Make sure our inputs are valid
    if (!psBridgeName || (strlen(psBridgeName) == 0) || !pSubnet) {
        LOGERROR("Failed to create bridge device '%s'. Invalid input provided!\n", SP(psBridgeName));
        return (1);
    }
    // Check if the device already exists
    if (dev_exist(psBridgeName)) {
        LOGERROR("Failed to create bridge device '%s'. Device already exists!", psBridgeName);
        return (1);
    }
    // Do we need to enable STP?
    if (config->disableTunnel == FALSE)
        psStpState = BRIDGE_STP_ON;

    // Create our bridge device first
    if ((pBridge = dev_create_bridge(psBridgeName, psStpState)) == NULL) {
        LOGERROR("Failed to create bridge device '%s'.\n", psBridgeName);
        return (1);
    }
    // Now assign the gateway IP for that subnet on the bridge device if we're on CC only
    if (PEER_IS_CC(eucanetdPeer)) {
        if (dev_install_ip(psBridgeName, (pSubnet->gateway + currentClusterId), pSubnet->netmask, pSubnet->broadcast, SCOPE_GLOBAL)) {
            LOGERROR("Failed to install gateway %s/%u on bridge device %s.\n", pSubnet->sGateway, pSubnet->slashNet, psBridgeName);
            if (dev_remove_bridge(psBridgeName, TRUE)) {
                LOGWARN("Failed to cleanup bridge device '%s' from system after failure.\n", psBridgeName);
            }
            EUCA_FREE(pBridge);
            return (1);
        }
    }
    // Now create the VLAN interface for this bridge
    if ((pVlanDev = dev_create_vlan(config->privInterface, pSubnet->vlanId)) == NULL) {
        LOGERROR("Failed to create VLAN device '%s' for bridge device '%s'.\n", dev_get_vlan_name(config->privInterface, pSubnet->vlanId), psBridgeName);
        if (managed_remove_bridge(pBridge)) {
            LOGWARN("Failed to cleanup bridge device '%s' from system after failure.\n", psBridgeName);
        }
        EUCA_FREE(pBridge);
        return (1);
    }
    // Now associate our bridge with the VLAN device
    if (dev_bridge_assign_interface(pBridge->sDevName, pVlanDev->sDevName)) {
        LOGERROR("Failed to assign VLAN device '%s' to bridge device '%s'.\n", dev_get_vlan_name(config->privInterface, pSubnet->vlanId), psBridgeName);
        if (managed_remove_bridge(pBridge)) {
            LOGWARN("Failed to cleanup bridge device '%s' from system after failure.\n", psBridgeName);
        }
        EUCA_FREE(pBridge);
        EUCA_FREE(pVlanDev);
        return (1);
    }
    // Enable the bridge device
    if (dev_up(pBridge->sDevName)) {
        LOGERROR("Failed to enable bridge device '%s'. Failed to enable bridge device.\n", psBridgeName);
        if (managed_remove_bridge(pBridge)) {
            LOGWARN("Failed to cleanup bridge device '%s' from system after failure.\n", psBridgeName);
        }
        EUCA_FREE(pBridge);
        EUCA_FREE(pVlanDev);
        return (1);
    }
    // Enable the VLAN device
    if (dev_up(pVlanDev->sDevName)) {
        LOGERROR("Failed to enable VLAN device '%s'.\n", dev_get_vlan_name(config->privInterface, pSubnet->vlanId));
        if (managed_remove_bridge(pBridge)) {
            LOGWARN("Failed to cleanup bridge device '%s' from system after failure.\n", psBridgeName);
        }
        EUCA_FREE(pBridge);
        EUCA_FREE(pVlanDev);
        return (1);
    }
    // We're all good
    EUCA_FREE(pBridge);
    EUCA_FREE(pVlanDev);
    return (0);
}

//!
//! This API sets up the bridge devices necessary on CC component. The following
//! tasks will be executed:
//!     -# Remove any non-needed or mis-configured bridge devices
//!     -# Create and setup a bridge device for each active security-group
//!
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

static int managed_setup_bridges(globalNetworkInfo * pGni) {
    int i = 0;
    int j = 0;
    int rc = 0;
    int ret = 0;
    int nbSecGroups = 0;
    int nbBridges = 0;
    boolean found = FALSE;
    dev_entry *pBridges = NULL;
    gni_node *pNode = NULL;
    gni_cluster *pCluster = NULL;
    gni_secgroup *pSecGroups = NULL;
    managed_subnet *pSubnet = NULL;

    LOGTRACE("Setting bridge devices and network gateways.\n");

    // Are the global and local network view structures NULL?
    if (!pGni) {
        LOGERROR("Failed to implement network artifacts for '%s' network driver. Invalid parameters provided.\n", NETMODE_MANAGED_NOVLAN);
        return (1);
    }
    //Retrieve our security group lists since our bridge devices are tied to this information
    if (PEER_IS_CC(eucanetdPeer)) {
        // Get our cluster structure from the config
        if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
            LOGERROR("Cannot find cluster for our network in global network view: check network configuration settings\n");
            return (1);
        }
        // Retrieve this cluster security group list
        if ((rc = gni_cluster_get_secgroup(pGni, pCluster, NULL, 0, NULL, 0, &pSecGroups, &nbSecGroups)) != 0) {
            LOGERROR("Failed to retrieve security groups for cluster '%s'.\n", pCluster->name);
            return (1);
        }
    } else if (PEER_IS_NC(eucanetdPeer)) {
        // Get our node structure from the config
        if ((rc = gni_find_self_node(pGni, &pNode)) != 0) {
            LOGERROR("Cannot find node for our network in global network view: check network configuration settings\n");
            return (1);
        }
        // Retrieve this node security group list
        if ((rc = gni_node_get_secgroup(pGni, pNode, NULL, 0, NULL, 0, &pSecGroups, &nbSecGroups)) != 0) {
            LOGERROR("Failed to retrieve security groups for node '%s'.\n", pNode->name);
            return (1);
        }
    } else {
        LOGERROR("System Error!!! Peer=%d\n", eucanetdPeer);
        return (1);
    }
    // Retrieve our bridge devices. They should all start with "sg-".
    if (dev_get_bridges((MANAGED_SG_CHAINNAME_PREFIX "*"), &pBridges, &nbBridges)) {
        LOGERROR("Failed to retrieve bridged network devices.\n");
        EUCA_FREE(pSecGroups);
        return (1);
    }
    //
    // First, delete any bridge that we no longer need. We will also take the time to make sure any
    // current bridge devices are setup properly. If they are not, we will remove them and we will
    // re-add them in the next step.
    //
    if (nbBridges) {
        // Did we get a Cluster ID change?
        if (currentClusterId == previousClusterId) {
            // No cluster ID change, go through all of our bridge devices to see what we need to remove
            for (i = 0; i < nbBridges; i++) {
                // Do we have a matching security group?
                for (j = 0, found = FALSE; ((j < nbSecGroups) && !found); j++) {
                    if (!strcmp(pBridges[i].sDevName, pSecGroups[j].name))
                        found = TRUE;
                }

                // Did we find a matching security group?
                if (found) {
                    // Are we setup properly?
                    if (managed_is_bridge_setup(&pBridges[i])) {
                        continue;
                    }
                }
                // If we get here, we need to delete. Either its no longer needed or not setup properly
                LOGDEBUG("Removing bridge device '%s' for security-group '%s'\n", pBridges[i].sDevName, pBridges[i].sDevName);
                if (managed_remove_bridge(&pBridges[i])) {
                    LOGWARN("Failed to remove bridge device '%s'.\n", pBridges[i].sDevName);
                    ret = 1;
                }
            }
        } else {
            // We have a cluster ID change, remove all bridges and re-set them
            for (i = 0; i < nbBridges; i++) {
                LOGDEBUG("Removing bridge device '%s' for security-group '%s'. Cluster ID changed.\n", pBridges[i].sDevName, pBridges[i].sDevName);
                if (managed_remove_bridge(&pBridges[i])) {
                    LOGWARN("Failed to remove bridge device '%s'.\n", pBridges[i].sDevName);
                    ret = 1;
                }
            }
        }
    }
    // We're done with the bridge device list
    dev_free_list(&pBridges, nbBridges);

    //
    // Now lets add what's missing
    //
    for (i = 0; i < nbSecGroups; i++) {
        // If we find a subnet, add the bridge device
        if ((pSubnet = managed_find_subnet(pGni, &pSecGroups[i])) != NULL) {
            //
            // if the bridge device does not exists, than create it. By now, all bridge devices on
            // the system have been scrubbed to be valid. All non-used bridge have been removed as
            // well as the non-complient bridge devices
            //
            if (!dev_exist(pSecGroups[i].name)) {
                LOGDEBUG("Creating bridge device '%s' for security-group '%s' on subnet %s/%u\n", pSecGroups[i].name, pSecGroups[i].name, pSubnet->sSubnet, pSubnet->slashNet);

                if (managed_create_bridge(pSecGroups[i].name, pSubnet)) {
                    LOGERROR("Failed to create bridge device for subnet for security-group %s\n", pSecGroups[i].name);
                    ret = 1;
                }
            }
        }
    }

    EUCA_FREE(pSecGroups);
    return (ret);
}

//!
//! Generates the DHCP server configuration so the instances can get their
//! networking configuration information. The following tasks will be executed:
//!     -# Write the standard DHCP header in the configuration file
//!     -# Write the security-groups private subnet declarations
//!     -# Write each instance private IP/MAC declaration
//!
//! @param[in] pGni a pointer to the Global Network Information structure
//!
//! @return 0 on success or 1 if any failure occurred
//!
//! @see
//!
//! @pre
//!     The pGni parameter MUST not be NULL
//!
//! @post
//!     On success the DHCP server configuration is created. On failure, the
//!     DHCP configuration file should not exists.
//!
//! @note
//!

int managed_generate_dhcpd_config(globalNetworkInfo * pGni) {
    int i = 0;
    int j = 0;
    int k = 0;
    int rc = 0;
    int ret = 0;
    int subnetIdx = 0;
    int nbSgInstances = 0;
    int nbClusterInstances = 0;
    char sPath[EUCA_MAX_PATH] = "";
    FILE *pFh = NULL;
    boolean found = FALSE;
    gni_cluster *pCluster = NULL;
    gni_instance *pSgInstances = NULL;
    gni_instance *pClusterInstances = NULL;
    managed_subnet *pSubnet = NULL;

    // Make sure the given pointer is valid
    if (!pGni) {
        LOGERROR("Cannot configure DHCP server. Invalid parameter provided.\n");
        return (1);
    }
    // Find our associated cluster
    if ((rc = gni_find_self_cluster(pGni, &pCluster)) != 0) {
        LOGERROR("cannot find the cluster to which the local node belongs: check network config settings\n");
        return (1);
    }

    if ((rc = gni_cluster_get_instances(pGni, pCluster, NULL, 0, NULL, 0, &pClusterInstances, &nbClusterInstances)) != 0) {
        LOGERROR("Cannot retrieve the instances associated with this cluster in global network view: check network configuration settings\n");
        return (1);
    }
    // Open the DHCP configuration file
    snprintf(sPath, EUCA_MAX_PATH, NC_NET_PATH_DEFAULT "/euca-dhcp.conf", config->eucahome);
    if ((pFh = fopen(sPath, "w")) == NULL) {
        LOGERROR("Cannot open dhcpd server config file for write '%s': check permissions\n", sPath);
        EUCA_FREE(pClusterInstances);
        return (1);
    }
    //
    // write the header of the file with our defaults
    //
    fprintf(pFh, "# automatically generated config file for DHCP server\ndefault-lease-time 86400;\nmax-lease-time 86400;\nddns-update-style none;\n\n");

    // Do we have any instances to care about?
    if (nbClusterInstances > 0) {
        // Create the shared-network configuration ;)
        fprintf(pFh, "shared-network euca {\n");

        //
        // At this point we will declare all of our active subnets
        //
        for (i = 0; i < pGni->max_secgroups; i++) {
            // Retrieve the instances for this security group
            if ((rc = gni_secgroup_get_instances(pGni, &pGni->secgroups[i], NULL, 0, NULL, 0, &pSgInstances, &nbSgInstances)) != 0) {
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
                for (j = 0, found = FALSE; ((j < nbSgInstances) && !found); j++) {
                    if ((subnetIdx = managed_find_subnet_idx(pSgInstances[j].privateIp)) != -1) {
                        // We have one instance in this group. Create the subnet configuration
                        found = TRUE;
                        pSubnet = &gaManagedSubnets[subnetIdx];

                        fprintf(pFh, "  subnet %s netmask %s {\n    option subnet-mask %s;\n    option broadcast-address %s;\n",
                                pSubnet->sSubnet, pSubnet->sNetmask, pSubnet->sNetmask, pSubnet->sBroadcast);

                        if (strlen(pGni->instanceDNSDomain)) {
                            fprintf(pFh, "    option domain-name \"%s\";\n", pGni->instanceDNSDomain);
                        }

                        if (pGni->max_instanceDNSServers) {
                            fprintf(pFh, "    option domain-name-servers %s", euca_ntoa(pGni->instanceDNSServers[0]));
                            for (k = 1; k < pGni->max_instanceDNSServers; k++) {
                                fprintf(pFh, ", %s", euca_ntoa(pGni->instanceDNSServers[k]));
                            }
                            fprintf(pFh, ";\n");
                        } else {
                            fprintf(pFh, "    option domain-name-servers 8.8.8.8;\n");
                        }

                        fprintf(pFh, "    option routers %s;\n  }\n", euca_ntoa(pSubnet->gateway + currentClusterId));

                    }
                }
            }
            EUCA_FREE(pSgInstances);
        }

        //
        // Now its time to include the configuration of each one of our instances
        //
        for (i = 0; i < nbClusterInstances; i++) {
            if (pClusterInstances[i].privateIp) {
                fprintf(pFh, "\n  host node-%s {\n    hardware ethernet %s;\n    fixed-address %s;\n  }\n",
                        euca_ntoa(pClusterInstances[i].privateIp), euca_etoa(pClusterInstances[i].macAddress), euca_ntoa(pClusterInstances[i].privateIp));
            }
        }

        //
        // Now close the content
        //
        fprintf(pFh, "}\n");
    }
    EUCA_FREE(pClusterInstances);
    fclose(pFh);
    return (ret);
}

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

#ifndef _INCLUDE_DEV_HANDLER_H_
#define _INCLUDE_DEV_HANDLER_H_

//!
//! @file net/dev_handler.h
//! Define a network device handling API. Anything that relates to network devices
//! IP addresses, Ethernet addresses, VLANs, etc. should be covered in this API. All
//! function names MUST start with the 'dev_' string and be properly documented.
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
//! Any other function implemented must have its name start with "dev" followed by an underscore
//! and the rest of the function name with every words separated with an underscore character. For
//! example: dev_this_is_a_good_function_name().
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <netdb.h>
#include <net/if.h>
#include <net/ethernet.h>
#include <netinet/if_ether.h>
#include <netinet/ip.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <ifaddrs.h>
#include <linux/if_link.h>

#include <euca_network.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name Various IP scopes to use for when installing IPs on a network device

#define SCOPE_GLOBAL                          "global"  //!< Indicates the installed IP is valid everywhere
#define SCOPE_SITE                            "site"    //!< Indicates the installed IP is only valid within this site (IPV6)
#define SCOPE_LINK                            "link"    //!< Indicates the installed IP is only valid on this network device
#define SCOPE_HOST                            "host"    //!< Indicates the installed IP is only valid inside this host (machine)

// @}

//! @{
//! @name Bridge device STP state

#define BRIDGE_STP_ON                         "on"  //!< STP is enabled on bridge device
#define BRIDGE_STP_OFF                        "off" //!< STP is disabled on bridge device

//! @}

#define TUNNEL_NAME_PREFIX                    "tap-"    //!< Tunnel device name prefix identifier

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

//! Enumeration defining the various type of network devices
typedef enum dev_type_t {
    DEV_TYPE_UNKNOWN = 0,              //!< For error detection
    DEV_TYPE_INTERFACE = 1,            //!< Standard network devices that aren't tunnels or bridge
    DEV_TYPE_BRIDGE = 2,               //!< Bridge network devices
    DEV_TYPE_TUNNEL = 3,               //!< Tunnel network devices
    DEV_TYPE_ANY = 4,                  //!< Any type of network devices (for searching without limiting)
} dev_type;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! A structure containing the pertinent information about a network device
typedef struct dev_entry_t {
    char sDevName[IF_NAME_LEN];        //!< Name of the device
    char sMacAddress[ENET_ADDR_LEN];   //!< Mac address string associated with this interface
    boolean isBridge;                  //!< Indicates if a device is a bridge device (TRUE) or not (FALSE)
} dev_entry;

//! A structure containing the pertinent information about a networking addresses
typedef struct in_addr_entry_t {
    char sDevName[IF_NAME_LEN];        //!< The device name for which this IP entry apply
    in_addr_t address;                 //!< The IP address associated with this device
    in_addr_t netmask;                 //!< The netmask for this device
    in_addr_t broascast;               //!< The network broadcast address
    u32 slashnet;                      //!< The bitmask for the network
    char sHost[NETWORK_ADDR_LEN];      //!< The host entry for this IP in the form of AAA.BBB.CCC.DDD/XX
} in_addr_entry;

typedef struct dev_handler_t {
    dev_entry *pDevices;               //!< Pointer to a list of devices on the system
    int numberOfDevices;               //!< The number of devices in the pDevices list
    in_addr_entry *pNetworks;          //!< Pointer to a list of networks on the system
    int numberOfNetworks;              //!< The number of networks in the pNetworks list
    int init;
    char cmdprefix[EUCA_MAX_PATH];
} dev_handler;


/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Array of string mapping to the device types enumeration
extern const char *asDevTypeNames[];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name APIs to retrieve information about the system network devices
int dev_handler_init(dev_handler *devh, const char *cmdprefix);
int dev_handler_free(dev_handler *devh);
int dev_handler_close(dev_handler *devh);
int dev_handler_repopulate(dev_handler *devh);

int dev_get(dev_handler *devh, const char *cpsSearch, dev_entry **pDevices, int *pNbDevices, dev_type deviceType);
boolean dev_exist(const char *psDeviceName);
boolean dev_is_up(const char *psDeviceName);
//! @}

//! @{
//! @name APIs to enable/disable and rename a given device
int dev_up(dev_handler *devh, const char *psDeviceName);
int dev_down(dev_handler *devh, const char *psDeviceName);
int dev_rename(dev_handler *devh, const char *psDeviceName, const char *psNewDevName);
//! @}

//! @{
//! @name APIs to work with VLAN on devices
const char *dev_get_vlan_name(const char *psDeviceName, u16 vlan);
int dev_get_vlan_id(const char *psDeviceName);
boolean dev_has_vlan(const char *psDeviceName, u16 vlan);
dev_entry *dev_create_vlan(dev_handler *devh, const char *psDeviceName, u16 vlan);
int dev_remove_vlan(dev_handler *devh, const char *psDeviceName, u16 vlan);
int dev_remove_vlan_interface(dev_handler *devh, const char *psVlanInterfaceName);
//! @}

//! @{
//! @name APIs to work with bridge devices
boolean dev_is_bridge(const char *psDeviceName);
boolean dev_is_bridge_interface(const char *psDeviceName, const char *psBridgeName);
boolean dev_has_bridge_interfaces(dev_handler *devh, const char *psBridgeName);
char *dev_get_interface_bridge(dev_handler *devh, const char *psDeviceName);
int dev_get_bridge_interfaces(dev_handler *devh, const char *psBridgeName, dev_entry ** pOutDevices, int *pOutNbDevices);
int dev_set_bridge_stp(dev_handler *devh, const char *psBridgeName, const char *psStpState);
dev_entry *dev_create_bridge(dev_handler *devh, const char *psBridgeName, const char *psStpState);
int dev_remove_bridge(dev_handler *devh, const char *psBridgeName, boolean forced);
int dev_bridge_assign_interface(dev_handler *devh, const char *psBridgeName, const char *psDeviceName);
int dev_bridge_delete_interface(dev_handler *devh, const char *psBridgeName, const char *psDeviceName);
//! @}

//! @{
//! @name APIs to work with tunnel devices
boolean dev_is_tunnel(const char *psDeviceName);
//! @}

//! @{
//! @name APIs to work with L2 addressing
char *dev_get_mac(const char *psDeviceName);
//! @}

//! @{
//! @name APIs to work with L3 addressing
int dev_get_ips(const char *psDeviceName, in_addr_entry **pOutIps, int *pNumberOfIps);
boolean dev_has_ip(const char *psDeviceName, in_addr_t ip);
boolean dev_has_host(const char *psDeviceName, in_addr_t ip, in_addr_t netmask);
int dev_flush_ips(dev_handler *devh, const char *psDeviceName);
int dev_install_ip(dev_handler *devh, const char *psDeviceName, in_addr_t address, in_addr_t netmask, in_addr_t broadcast, const char *scope);
int dev_install_ips(dev_handler *devh, in_addr_entry  *pIps, int nbIps, const char *scope);
int dev_move_ip(dev_handler *devh, const char *psDeviceName, in_addr_t address, in_addr_t netmask, in_addr_t broadcast, const char *scope);
int dev_move_ips(dev_handler *devh, in_addr_entry  *pIps, int nbIps, const char *scope);
int dev_remove_ip(dev_handler *devh, const char *psDeviceName, in_addr_t address, in_addr_t netmask);
int dev_remove_ips(dev_handler *devh, in_addr_entry * pIps, int nbIps);
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/**
 * Free a list of device names.
 *
 * @param[in,out] pList a pointer to a list of string that will contain the device names
 * @param[in]     nbItems number of items to free in the list
 *
 * @pre
 *     psDevNames SHOULD not be NULL
 *
 * @post
 *     The memory is freed and pList is set to NULL
 */
static inline void dev_free_list(dev_entry **pList, int nbItems) {
    if (pList != NULL) {
        EUCA_FREE((*pList));
    }
}

/**
 * Free a list of ips.
 *
 * @param pList [in,out] a pointer to a list of in_addr_entry
 *
 * @pre
 *     psDevNames SHOULD not be NULL
 *
 * @post
 *     The memory is freed and pList is set to NULL
 */
static inline void dev_free_ips(in_addr_entry **pList) {
    if (pList != NULL) {
        EUCA_FREE((*pList));
    }
}

/**
 * Initialize an in_addr_entry structure with the given information.
 *
 * @param pEntry [in,out] a pointer to the structure to initialize
 * @param psDeviceName [in] a string pointer to the device name associated with this IP entry
 * @param address [in] the IP address
 * @param netmask [in] the netmask address
 *
 * @pre
 *     pEntry SHOULD not be NULL
 *
 * @post
 *     The structure is initialized
 */
static inline void dev_in_addr_entry(in_addr_entry *pEntry, const char *psDeviceName, in_addr_t address, in_addr_t netmask) {
    char *sAddress = NULL;
    if (pEntry) {
        snprintf(pEntry->sDevName, IF_NAME_LEN, "%s", psDeviceName);
        pEntry->address = address;
        pEntry->netmask = netmask;
        pEntry->slashnet = NETMASK_TO_SLASHNET(netmask);
        pEntry->broascast = (address | ~netmask);
        sAddress = hex2dot(address);
        snprintf(pEntry->sHost, NETWORK_ADDR_LEN, "%s/%u", sAddress, pEntry->slashnet);
        EUCA_FREE(sAddress);
    }
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name Macros to retrieve different type of devices based on device type

#define dev_get_list(_pDh, _cpsSearch, _pDevices, _pNbDevices)      dev_get((_pDh), (_cpsSearch), (_pDevices), (_pNbDevices), DEV_TYPE_ANY)
#define dev_get_intfc(_pDh, _cpsSearch, _pDevices, _pNbDevices)     dev_get((_pDh), (_cpsSearch), (_pDevices), (_pNbDevices), DEV_TYPE_INTERFACE)
#define dev_get_bridges(_pDh, _cpsSearch, _pDevices, _pNbDevices)   dev_get((_pDh), (_cpsSearch), (_pDevices), (_pNbDevices), DEV_TYPE_BRIDGE)
#define dev_get_tunnels(_pDh, _cpsSearch, _pDevices, _pNbDevices)   dev_get((_pDh), (_cpsSearch), (_pDevices), (_pNbDevices), DEV_TYPE_TUNNEL)

//! @}

//! Macro to convert a device type to a human readable string
#define DEVTYPE2STR(_devType)                    (((_devType) > DEV_TYPE_ANY) ? asDevTypeNames[0] : asDevTypeNames[(_devType)])

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_DEV_HANDLER_H_ */

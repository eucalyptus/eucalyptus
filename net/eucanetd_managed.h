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

#ifndef _INCLUDE_EUCANETD_MANAGED_H_
#define _INCLUDE_EUCANETD_MANAGED_H_

//!
//! @file net/eucanetd_managed.h
//! This defines the various APIs shared between both managed networking modes:
//! MANAGED and MANAGED-NOVLAN.
//!
//! Every APIs defined under this header file are implemented in the
//! net/eucanetd_managed.c source file and follow the rules set forth under this
//! file description.
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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MANAGED_SG_CHAINNAME_PREFIX              "sg-"

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

//! defines a subnet entry for managed-novlan functionality
typedef struct managed_subnet_t {
    u16 vlanId;                        //!< VLAN identifier for this subnet
    in_addr_t subnet;                  //!< Binary subnet address
    char sSubnet[INET_ADDR_LEN];       //!< Human readable subnet address
    in_addr_t netmask;                 //!< Binary subnet netmask address
    u32 slashNet;                      //!< Number of bits for the bitmask
    char sNetmask[INET_ADDR_LEN];      //!< Human readable subnet netmask address
    in_addr_t gateway;                 //!< Binary subnet gateway address
    char sGateway[INET_ADDR_LEN];      //!< Human readable subnet gateway address
    in_addr_t broadcast;               //!< Binary subnet broadcast address
    char sBroadcast[INET_ADDR_LEN];    //!< Human readable subnet broadcast address
} managed_subnet;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Array containing information about each one of our private subnets
extern managed_subnet gaManagedSubnets[NB_VLAN_802_1Q];

//! Attach tunnel function pointer so the proper Managed or Managed-NoVlan API equivalent is called
extern int (*managed_attach_tunnels_fn) (globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups);

//! Detach tunnel function pointer so the proper Managed or Managed-NoVlan API equivalent is called
extern int (*managed_detach_tunnels_fn) (globalNetworkInfo * pGni, gni_cluster * pCluster, gni_secgroup * pSecGroups, int nbGroups, dev_entry * pTunnels, int nbTunnels);

//! Our stored previous cluster ID
extern char previousClusterId;

//! The current cluster ID in the list of clusters
extern char currentClusterId;

//! Boolean to indicate if we fail during the cluster ID change. We assume success until someone indicates a failure
extern boolean clusterIdChangedSuccessful;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name APIs to initialize our private subnets and find the subnet matching a private instance IP or subnet gateway
int managed_initialize_private_subnets(globalNetworkInfo * pGni);
int managed_find_subnet_idx(in_addr_t instanceAddress);
int managed_find_subnet_idx_from_gateway(in_addr_t gatewayAddress);
managed_subnet *managed_find_subnet(globalNetworkInfo * pGni, gni_secgroup * pSecGroup);
//! @}

//! @{
//! @name Various APIs shared between MANAGED and MANAGED-NOVLAN modes
boolean managed_has_sg_changed(globalNetworkInfo * pGni, lni_t * pLni);
boolean managed_has_addressing_changed(globalNetworkInfo * pGni, lni_t * pLni);
int managed_setup_metadata_ip(globalNetworkInfo * pGni, const char *psDevName);
int managed_setup_sg_filters(globalNetworkInfo * pGni);
int managed_setup_addressing(globalNetworkInfo * pGni);
int managed_setup_elastic_ips(globalNetworkInfo * pGni);
//! @}

//! @{
//! @name APIs to work with tunnel network devices
int managed_initialize_tunnels(eucanetdConfig * pConfig);
int managed_cleanup_tunnels(globalNetworkInfo * pGni);
boolean managed_has_tunnel_changed(globalNetworkInfo * pGni, gni_secgroup * pSecGroups, int nbGroups);
int managed_save_tunnel_id(int tunnelId);
int managed_get_current_tunnel_id(void);
int managed_get_new_tunnel_id(globalNetworkInfo * pGni, gni_cluster * pCluster);
int managed_setup_tunnels(globalNetworkInfo * pGni);
int managed_unset_tunnels(globalNetworkInfo * pGni);
int managed_attach_tunnel(dev_entry * pBridge, managed_subnet * pSubnet, int localId, int remoteId);
int managed_detach_tunnel(dev_entry * pBridge, dev_entry * pTunnel);
//! @}

//! @{
//! @name API to generate a DHCP configuration file for managed modes
int managed_generate_dhcpd_config(globalNetworkInfo * pGni);
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to initialize our subnet structure
#define INITIALIZE_SUBNETS(_pGni)                managed_initialize_private_subnets((_pGni))

//! Macro to retrieve a managed subnet structure based on its network index
#define GET_SUBNET(_idx)                        ((((_idx) < MIN_VLAN_EUCA) || ((_idx) > MAX_VLAN_EUCA)) ? NULL : &gaManagedSubnets[(_idx)])

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCANETD_MANAGED_H_ */

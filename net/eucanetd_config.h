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

#ifndef _INCLUDE_EUCANETD_CONFIG_H_
#define _INCLUDE_EUCANETD_CONFIG_H_

//!
//! @file net/eucanetd_config.h
//!
//! @todo - move this content into eucanetd.h... Do not see why we need a separate
//!         file for configuration unless we have a .c
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <config.h>
#include <data.h>
#include <ipt_handler.h>
#include <ips_handler.h>
#include <ebt_handler.h>
#include <atomic_file.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define NUM_EUCANETD_CONFIG                      1

/* Defines the bitmask for the flush mode */
#define EUCANETD_FLUSH_AND_RUN_MASK              0x01  //!< Will only flush and continue running
#define EUCANETD_FLUSH_ONLY_MASK                 0x02  //!< Will flush and stop running the daemon
#define EUCANETD_FLUSH_MASK                      0xFF  //!< Mask to see if we need to flush

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

// Enumeration of configuration constants found in eucalyptus.conf
enum {
    EUCANETD_CVAL_PUBINTERFACE,
    EUCANETD_CVAL_PRIVINTERFACE,
    EUCANETD_CVAL_BRIDGE,
    EUCANETD_CVAL_EUCAHOME,
    EUCANETD_CVAL_MODE,
    EUCANETD_CVAL_DHCPDAEMON,
    EUCANETD_CVAL_DHCPUSER,
    EUCANETD_CVAL_POLLING_FREQUENCY,
    EUCANETD_CVAL_DISABLE_L2_ISOLATION,
    EUCANETD_CVAL_NC_PROXY,
    EUCANETD_CVAL_NC_ROUTER,
    EUCANETD_CVAL_NC_ROUTER_IP,
    EUCANETD_CVAL_METADATA_USE_VM_PRIVATE,
    EUCANETD_CVAL_METADATA_IP,
    EUCANETD_CVAL_DISABLE_TUNNELING,
    EUCANETD_CVAL_ADDRSPERNET,
    EUCANETD_CVAL_EUCA_USER,
    EUCANETD_CVAL_LOGLEVEL,
    EUCANETD_CVAL_LOGROLLNUMBER,
    EUCANETD_CVAL_LOGMAXSIZE,
    EUCANETD_CVAL_MIDOEUCANETDHOST,
    EUCANETD_CVAL_MIDOGWHOSTS,
    EUCANETD_CVAL_MIDOPUBNW,
    EUCANETD_CVAL_MIDOPUBGWIP,
    EUCANETD_CVAL_LOCALIP,
    EUCANETD_CVAL_LAST,
};

enum {
    FLUSH_NONE,
    FLUSH_ALL,
    FLUSH_DYNAMIC,
    FLUSH_MIDO_ALL,
    FLUSH_MIDO_DYNAMIC,
    FLUSH_MIDO_CHECKDUPS,
    FLUSH_MIDO_DUPS,
    FLUSH_MIDO_CHECKVPC,
    FLUSH_MIDO_CHECKUNCONNECTED,
    FLUSH_MIDO_UNCONNECTED,
    FLUSH_MIDO_VPC,
    FLUSH_MIDO_LISTVPC,
    FLUSH_MIDO_TEST,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Structure defining the core EUCANETD configuration
typedef struct eucanetdConfig_t {
    ipt_handler *ipt;                  //!< Pointer to the IP Tables Handler
    ips_handler *ips;                  //!< Pointer to the IP Sets Handler
    ebt_handler *ebt;                  //!< Pointer to the EB Tables Handler

    char netMode[NETMODE_LEN];         //!< Network mode name string
    euca_netmode nmCode;               //!< Network mode integer code
    char *eucahome;                    //!< Pointer to the string containing the eucalyptus area home path
    char *eucauser;                    //!< Pointer to the string containing the eucalyptus system user name
    char cmdprefix[EUCA_MAX_PATH];
    char sIptPreload[EUCA_MAX_PATH];
    char configFiles[NUM_EUCANETD_CONFIG][EUCA_MAX_PATH];
    u32 vmGatewayIP;
    u32 clcMetadataIP;
    char ncRouterIP[INET_ADDR_LEN];
    char metadataIP[INET_ADDR_LEN];

    char pubInterface[IF_NAME_LEN];    //!< The configured public interface device to use for networking (VNET_PUBINTERFACE)
    char privInterface[IF_NAME_LEN];   //!< The configured private interface device to use for networking (VNET_PRIVINTERFACE)
    char bridgeDev[IF_NAME_LEN];       //!< The configured bridge device to use for networking (VNET_BRIDGE)

    char dhcpUser[32];                 //!< The user name as which the DHCP daemon runs on the distribution. (VNET_DHCPUSER)
    char dhcpDaemon[EUCA_MAX_PATH];    //!< The path to the ISC DHCP server executable to use. (VNET_DHCPDAEMON)

    char midoeucanetdhost[HOSTNAME_LEN];
    char midogwhosts[HOSTNAME_LEN*3*33];
    char midopubnw[HOSTNAME_LEN];
    char midopubgwip[HOSTNAME_LEN];

    atomic_file global_network_info_file;
    char lastAppliedVersion[32];

    // these are flags that can be set by values in eucalyptus.conf
    int polling_frequency;
    int disable_l2_isolation;
    int nc_router_ip;
    int nc_router;
    int metadata_use_vm_private;
    int metadata_ip;

    in_addr_t localIp;                 //!< Local address to use for this system
    boolean disableTunnel;             //!< Set to FALSE if we need to make use of L2 tunnels (DISABLE_TUNNELING).

    boolean nc_proxy;                //!< Set to TRUE to indicate we're using the NC proxy feature

    int debug;
    int flushmode;
    char *flushmodearg;
    boolean multieucanetd_safe;
    int udpsock;
    boolean init;
} eucanetdConfig;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Global EUCANETD configuration structure
extern eucanetdConfig *config;

//! @{
//! @name Configuration Keys from eucalyptus.conf
extern configEntry configKeysRestartEUCANETD[];
extern configEntry configKeysNoRestartEUCANETD[];
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCANETD_CONFIG_H_ */

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

#ifndef _INCLUDE_EUCANETD_H_
#define _INCLUDE_EUCANETD_H_

//!
//! @file net/eucanetd.h
//! Definition of the service management layer
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <data.h>
#include <atomic_file.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name The bitmask indicating which of the API needs to be executed after a successful system scrup

#define EUCANETD_RUN_NO_API                      0x00000000 //!< Bassically says don't do anything
#define EUCANETD_RUN_NETWORK_API                 0x00000001 //!< If set, this will trigger the core to run the implement_network() driver API
#define EUCANETD_RUN_SECURITY_GROUP_API          0x00000002 //!< If set, this will trigger the core to run the implement_sg() driver API
#define EUCANETD_RUN_ADDRESSING_API              0x00000004 //!< If set, this will trigger the core to run the implement_addressing() driver API
#define EUCANETD_RUN_ALL_API                     (EUCANETD_RUN_NETWORK_API | EUCANETD_RUN_SECURITY_GROUP_API | EUCANETD_RUN_ADDRESSING_API)
#define EUCANETD_RUN_ERROR_API                   0xFFFFFFFF //!< This is to indicate an error case

//! @}

//! @{
//! @name Various known network IP and usual bitmask

#define INADDR_METADATA                          0xA9FEA9FE //!< Metadata IP address
#define IN_HOST_NET                              0xFFFFFFFF //!< 32 bit host network (not defined in in.h)

//! @}

#define EUCANETD_DUMMY_UDP_PORT                  63822

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

//! Enumeration of the peer component type running alongside this eucanetd service
typedef enum eucanetd_peer_t {
    PEER_INVALID = 0,                  //!< This is an invalid peer, this is used to detect initialization failures (forget to initialize)
    PEER_CLC = 1,                      //!< This indicates we are currently working with a CLC component
    PEER_CC = 2,                       //!< This indicates we are currently working with a CC component
    PEER_NC = 3,                       //!< This indicates we are currently working with an NC component
    PEER_NONE = 4,                     //!< This indicates we are currently running on a non-euca host
    PEER_MAX = 5,                      //!< This is an invalid role use to detect initialization errors (couldn't set the role)
} eucanetd_peer;

enum eucanetd_debug_level_t {
    EUCANETD_DEBUG_NONE = 0,
    EUCANETD_DEBUG_TRACE = 1,
    EUCANETD_DEBUG_DEBUG = 2,
    EUCANETD_DEBUG_INFO = 3,
    EUCANETD_DEBUG_WARN = 4,
    EUCANETD_DEBUG_ERROR = 5,
    EUCANETD_DEBUG_FATAL = 6,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Network Driver API
typedef struct driver_handler_t {
    char name[CHAR_BUFFER_SIZE];                                            //!< The name of the given network driver (e.g. EDGE, MANAGED, etc.)
    int (*init) (eucanetdConfig *pConfig);                                  //!< The driver initialization interface
    int (*cleanup) (globalNetworkInfo *pGni, boolean doFlush);              //!< The driver cleanup interface
    int (*upgrade) (globalNetworkInfo *pGni);                               //!< This is optional when upgrade tasks are required.
    int (*system_flush) (globalNetworkInfo *pGni);                          //!< Responsible for the flushing of all euca networking artifacts
    int (*system_maint) (globalNetworkInfo *pGni, lni_t *pLni);             //!< Maintenance actions when eucanetd is idle (e.g., no GNI changes)
    u32 (*system_scrub) (globalNetworkInfo *pGni,
            globalNetworkInfo *pGniApplied, lni_t *pLni);                   //!< Works on detecting what is changing
    int (*implement_network) (globalNetworkInfo *pGni, lni_t *pLni);        //!< Takes care of network devices, tunnels, etc.
    int (*implement_sg) (globalNetworkInfo *pGni, lni_t *pLni);             //!< Takes care of security group implementations and membership
    int (*implement_addressing) (globalNetworkInfo *pGni, lni_t *pLni);     //!< Takes care of IP addressing, Elastic IPs, etc.
    int (*handle_signal) (globalNetworkInfo *pGni, int signal);             //!< Forward signals (USR1 and USR2) to driver
} driver_handler;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name Network Driver Interfaces (NDIs)
extern struct driver_handler_t edgeDriverHandler;           //!< EDGE network driver callback instance
extern struct driver_handler_t managedDriverHandler;        //!< MANAGED network driver callback instance
extern struct driver_handler_t managedNoVlanDriverHandler;  //!< MANAGED-NOVLAN network driver callback instance
extern struct driver_handler_t midoVpcDriverHandler;        //!< MIDONET VPC network driver callback instance
extern struct driver_handler_t systemDriverHandler;         //!< SYSTEM network driver callback instance
extern struct driver_handler_t staticDriverHandler;         //!< STATIC network driver callback instance
//! @}

//! Global Network Information structure pointer.
//extern globalNetworkInfo *globalnetworkinfo;

//! Role of the component running alongside this eucanetd service
extern eucanetd_peer eucanetdPeer;

//! Array of peer type strings
extern const char *asPeerRoleName[];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/
int eucanetd_dummy_udpsock(void);
int eucanetd_dummy_udpsock_close(void);

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

//! Macro to check wether or not a given peer value is valid
#define PEER_IS_VALID(_peer)             (((_peer) > PEER_INVALID) && ((_peer) < PEER_MAX))

//! Macro to determine if we are on a CLC
#define PEER_IS_CLC(_peer)               ((_peer) == PEER_CLC)

//! Macro to determine if we are on a CC
#define PEER_IS_CC(_peer)                ((_peer) == PEER_CC)

//! Macro to determine if we are on a NC
#define PEER_IS_NC(_peer)                ((_peer) == PEER_NC)

//! Macro to determine if we are not either NC, CC, and CLC
#define PEER_IS_NONE(_peer)                ((_peer) == PEER_NONE)

//! Macro to convert a peer enumeration to a string representation
#define PEER2STR(_peer)                  ((((unsigned)(_peer)) > PEER_MAX) ? asPeerRoleName[PEER_MAX] : asPeerRoleName[(_peer)])

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCANETD_H_ */

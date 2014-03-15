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
//! This file needs a description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <data.h>
#include <ipt_handler.h>
#include <atomic_file.h>
#include <globalnetwork.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define NUM_EUCANETD_CONFIG                     1

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

typedef struct eucanetdConfig_t {
    ipt_handler *ipt;
    ips_handler *ips;
    ebt_handler *ebt;
    char *eucahome, *eucauser;
    char cmdprefix[EUCA_MAX_PATH];
    char configFiles[NUM_EUCANETD_CONFIG][EUCA_MAX_PATH];
    char bridgeDev[32];
    char pubInterface[32];
    char privInterface[32];
    char dhcpDaemon[EUCA_MAX_PATH];
    //    u32 all_public_ips[NUMBER_OF_PUBLIC_IPS * MAXINSTANCES_PER_CC];
    //    int max_all_public_ips;

    //    atomic_file cc_configfile, cc_networktopofile, nc_localnetfile, global_network_info_file;
    atomic_file global_network_info_file;

    int cc_polling_frequency, cc_cmdline_override, disable_l2_isolation, fake_router;
    int debug, flushmode;

    // u32 defaultgw;

    //    char *clcIp, *ccIp;

    //    gni_secgroup *security_groups;
    //    int max_security_groups;

    int init;
} eucanetdConfig;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int temporary(void);

int daemonize(void);

int eucanetdInit(void);
int logInit(void);

int read_config_bootstrap(void);
int read_config(void);
int read_latest_network(void);

int fetch_latest_network(int *update_globalnet);
int fetch_latest_localconfig(void);
int fetch_latest_serviceIps(int *);
int fetch_latest_euca_network(int *update_globalnet);

int parse_network_topology(char *);
int parse_pubprivmap(char *pubprivmap_file);
int parse_ccpubprivmap(char *cc_configfile);

//int ruleconvert(char *rulebuf, char *outrule);
//int check_for_network_update(int *, int *);

int update_private_ips(void);
int update_public_ips(void);
int update_sec_groups(void);
int update_metadata_redirect(void);
int update_isolation_rules(void);

int flush_all(void);

void sec_groups_print(gni_secgroup * newgroups, int max_newgroups);
gni_secgroup *find_sec_group_bypriv(gni_secgroup * groups, int max_groups, u32 privip, int *foundidx);
gni_secgroup *find_sec_group_bypub(gni_secgroup * groups, int max_groups, u32 pubip, int *foundidx);

int check_stderr_already_exists(int rc, char *o, char *e);

char *mac2interface(char *mac);
char *interface2mac(char *dev);

int kick_dhcpd_server(void);
int generate_dhcpd_config(void);

// XML parsing stuff

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

#endif /* ! _INCLUDE_EUCANETD_H_ */

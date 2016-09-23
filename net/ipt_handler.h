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

#ifndef _INCLUDE_IPT_HANDLER_H_
#define _INCLUDE_IPT_HANDLER_H_

//!
//! @file net/ipt_handler.h
//! This file needs a description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <unistd.h>
#include <errno.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name Standard IP Table Names

#define IPT_TABLE_FILTER                         "filter"
#define IPT_TABLE_NAT                            "nat"

//! @}

//! @{
//! @name IP Chain Names

#define IPT_CHAIN_INPUT                          "INPUT"
#define IPT_CHAIN_FORWARD                        "FORWARD"
#define IPT_CHAIN_OUTPUT                         "OUTPUT"
#define IPT_CHAIN_EUCA_COUNTERS_IN               "EUCA_COUNTERS_IN"
#define IPT_CHAIN_EUCA_COUNTERS_OUT              "EUCA_COUNTERS_OUT"
#define IPT_CHAIN_PREROUTING                     "PREROUTING"
#define IPT_CHAIN_POSTROUTING                    "POSTROUTING"

//! @}

//! @{
//! @name IP Chain Policy Names

#define IPT_CHAIN_POLICY_DROP                    "DROP"
#define IPT_CHAIN_POLICY_ACCEPT                  "ACCEPT"
#define IPT_CHAIN_POLICY_DEFAULT                 IPT_CHAIN_POLICY_ACCEPT

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

enum { IPT_NO_ORDER, IPT_ORDER };

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct ipt_rule_t {
    char iptrule[1024];
    //  long long int countersa, countersb;
    char counterstr[256];
    int flushed;
    int order;
} ipt_rule;

typedef struct ipt_chain_t {
    char name[64];
    char policyname[64];
    char counters[64];
    ipt_rule *rules;
    int max_rules;
    int ruleorder;
    int ref_count;
    int flushed;
} ipt_chain;

typedef struct ipt_table_t {
    char name[64];
    ipt_chain *chains;
    int max_chains;
} ipt_table;

typedef struct ipt_handler_t {
    ipt_table *tables;
    int max_tables;
    int init;
    char ipt_file[EUCA_MAX_PATH];
    char cmdprefix[EUCA_MAX_PATH];
    char preloadPath[EUCA_MAX_PATH];
} ipt_handler;

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

//! @{
//! @name IP tables API
int ipt_handler_init(ipt_handler *ipth, const char *cmdprefix, const char *preload);
int ipt_handler_free(ipt_handler *ipth);
int ipt_handler_close(ipt_handler *ipth);

int ipt_system_save(ipt_handler *ipth);
int ipt_system_restore(ipt_handler *ipth);

int ipt_handler_repopulate(ipt_handler *ipth);
int ipt_handler_deploy(ipt_handler *ipth);
int ipt_handler_update_refcounts(ipt_handler *ipth);

int ipt_handler_add_table(ipt_handler *ipth, char *tablename);
ipt_table *ipt_handler_find_table(ipt_handler *ipth, const char *findtable);

int ipt_table_add_chain(ipt_handler *ipth, char *tablename, char *chainname, char *policyname, char *counters);
ipt_chain *ipt_table_find_chain(ipt_handler *ipth, const char *tablename, const char *findchain);
int ipt_table_set_chain_policy(ipt_handler *ipth, const char *tablename, const char *chainname, const char *policyname);

int ipt_chain_add_rule(ipt_handler *ipth, char *tablename, char *chainname, char *newrule);
//int ipt_chain_add_rule_with_counters(ipt_handler *ipth, char *tablename, char *chainname, char *newrule, long long int countersa, long long int countersb);
int ipt_chain_add_rule_with_counters(ipt_handler *ipth, char *tablename, char *chainname, char *newrule, char *counterstr);
//int ipt_chain_insert_rule(ipt_handler *ipth, char *tablename, char *chainname, char *newrule, long long int countersa, long long int countersb, int order);
int ipt_chain_insert_rule(ipt_handler *ipth, char *tablename, char *chainname, char *newrule, char *counterstr, int order);
ipt_rule *ipt_chain_find_rule(ipt_handler *ipth, char *tablename, char *chainname, char *findrule);

int ipt_chain_flush(ipt_handler *ipth, char *tablename, char *chainname);
int ipt_chain_flush_rule(ipt_handler *ipth, char *tablename, char *chainname, char *findrule);

int ipt_table_deletechainmatch(ipt_handler *ipth, char *tablename, char *chainmatch);
int ipt_table_deletechainempty(ipt_handler *ipth, char *tablename);

int ipt_handler_print(ipt_handler *ipth);

int ipt_ruleordercmp(const void *p1, const void *p2);
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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_IPT_HANDLER_H_ */

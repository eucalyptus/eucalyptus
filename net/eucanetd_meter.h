// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

#ifndef _INCLUDE_EUCANETD_METER_H_
#define _INCLUDE_EUCANETD_METER_H_

//!
//! @file net/eucanetd_meter.h
//! Definition for eucanetd meter
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <data.h>
#include <config.h>
#include <atomic_file.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define NUM_ENM_CONFIG                      1

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
    ENM_CVAL_EUCAHOME,
    ENM_CVAL_MODE,
    ENM_CVAL_EUCA_USER,
    ENM_CVAL_LOGLEVEL,
    ENM_CVAL_LOGROLLNUMBER,
    ENM_CVAL_LOGMAXSIZE,
    ENM_CVAL_LAST,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct enmCounter_t {
    long long id;
    long long bytes_in;
    long long bytes_out;
    long long pkts_in;
    long long pkts_out;
    struct in_addr **match_netaddr;
    struct in_addr **match_netmask;
    int max_match;
    struct in_addr **inv_match_netaddr;
    struct in_addr **inv_match_netmask;
    int max_inv_match;
    char name[SMALL_CHAR_BUFFER_SIZE];
} enmCounter;

typedef struct enmInterface_t {
    long long id;
    char name[INTERFACE_ID_LEN];
    struct in_addr **local_ips;
    int max_local_ips;
    enmCounter **counters;
    int max_counters;
    enmCounter *vpc;
    enmCounter *vpcsubnet;
    enmCounter *external;
    enmCounter *metadata;
    enmCounter *clc;
} enmInterface;

//! Structure defining eucanetd_meter configuration
typedef struct enmConfig_t {
    char netMode[NETMODE_LEN];         /** Network mode name string */
    euca_netmode nmCode;               /** Network mode integer code */
    char *eucahome;                    /** Pointer to the string containing the eucalyptus area home path */
    char *eucauser;                    /** Pointer to the string containing the eucalyptus system user name */
    char cmdprefix[EUCA_MAX_PATH];
    char configFiles[NUM_ENM_CONFIG][EUCA_MAX_PATH];

    u32 euca_version;
    char *euca_version_str;
    
    atomic_file gni_atomic_file;
    
    char *device;                      /** name of the device to meter */
    char *lips;                        /** comma separated Local IPv4 addresses */
    char *lsn;                         /** Local subnet IPv4 CIDR block */
    int count;
    int promiscuous_mode;
    int daemonize;
    int snaplen;
    
    enmInterface eni;
} enmConfig;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

extern configEntry configKeysRestartEnm[];
extern configEntry configKeysNoRestartEnm[];

extern int enm_sig_rcvd;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

u32 euca_id_strtol(char *id);
int enmInterface_free(enmInterface *eni);
int enmCounter_free(enmCounter *counter);

enmCounter *newCounterEmpty(char *name);
enmCounter *newCounter(char *name, char *cidr);
enmCounter *findCounter(char *name, enmCounter **counters, int max_counters);

int counterAddMatch(enmCounter *counter, boolean inv, char *cidr);

char *enmPrintCounters(enmConfig *config);

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

#endif /* ! _INCLUDE_EUCANETD_METER_H_ */

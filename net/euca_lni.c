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
//! @file net/euca_lni.c
//! Implements the Local Network View of the system. This library retrieves networking
//! information from the system and stores it into a data structure that the EUCANETD
//! network drivers can use to take some decisions.
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
//! Any other function implemented must have its name start with "lni" followed by an underscore
//! and the rest of the function name with every words separated with an underscore character. For
//! example: lni_this_is_a_good_function_name().
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
#include <math.h>
#include <config.h>
#include <dirent.h>
#include <errno.h>
#include <netdb.h>
#include <net/if.h>
#include <net/ethernet.h>
#include <netinet/if_ether.h>
#include <netinet/ip.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <ifaddrs.h>
#include <linux/if_link.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <euca_network.h>
#include <log.h>
#include <atomic_file.h>

#include "ipt_handler.h"
#include "ips_handler.h"
#include "ebt_handler.h"
#include "dev_handler.h"
#include "euca_gni.h"
#include "euca_lni.h"
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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Allocates and initialize memory for a new Local Network Information structure.
//!
//! @return A pointer to the allocated and initialized memory on success or NULL on failure
//!
//! @see lni_free(), LNI_FREE()
//!
//! @pre
//!
//! @post
//!
//! @note
//!     Memory for the structure is allocated dynamically. The caller is responsible
//!     for freeing the memory using the LNI_FREE() macro or lni_free() API call.
//!
lni_t *lni_init(const char *psCmdPrefix, const char *psIptPreload)
{
    int rc = 0;
    lni_t *pLni = NULL;

    // Allocate clean memory for our structure
    if ((pLni = EUCA_ZALLOC(1, sizeof(lni_t))) == NULL) {
        LOGFATAL("out of memory!\n");
        return (NULL);
    }
    // Allocate memory for the IP Table handler
    if ((pLni->pIpTables = EUCA_ZALLOC(1, sizeof(ipt_handler))) == NULL) {
        LOGFATAL("out of memory!\n");
        LNI_FREE(pLni);
        return (NULL);
    }
    // Iniitalize the IP table handler
    if ((rc = ipt_handler_init(pLni->pIpTables, psCmdPrefix, psIptPreload)) != 0) {
        LOGERROR("could not initialize ipt_handler: check above log errors for details\n");
        LNI_FREE(pLni);
        return (NULL);
    }
    // Allocate clean memory for our IP set handler
    if ((pLni->pIpSet = EUCA_ZALLOC(1, sizeof(ips_handler))) == NULL) {
        LOGFATAL("out of memory!\n");
        LNI_FREE(pLni);
        return (NULL);
    }
    // Initialize the IP set handler
    if ((rc = ips_handler_init(pLni->pIpSet, psCmdPrefix)) != 0) {
        LOGERROR("could not initialize ips_handler: check above log errors for details\n");
        LNI_FREE(pLni);
        return (NULL);
    }
    // Allocate clean memory for the EB table structure
    if ((pLni->pEbTables = EUCA_ZALLOC(1, sizeof(ebt_handler))) == NULL) {
        LOGFATAL("out of memory!\n");
        LNI_FREE(pLni);
        return (NULL);
    }
    // Initialize the EB table handler
    if ((rc = ebt_handler_init(pLni->pEbTables, psCmdPrefix)) != 0) {
        LOGERROR("could not initialize ebt_handler: check above log errors for details\n");
        LNI_FREE(pLni);
        return (NULL);
    }

    return (pLni);
}

//!
//! Re-initialize the given lni_t structure. This should be performed between
//! lni_populate() calls to prevent memory leaks
//!
//! @param[in] pLni a pointer to the structure to re-initialize
//!
//! @see lni_populate(), LNI_RESET()
//!
//! @pre
//!     pLni should not be NULL
//!
//! @post
//!
//! @note
//!
void lni_reinit(lni_t * pLni)
{
    if (pLni) {
        ipt_handler_free(pLni->pIpTables);
        ips_handler_free(pLni->pIpSet);
        ebt_handler_free(pLni->pEbTables);
        EUCA_FREE(pLni->pDevices);
        EUCA_FREE(pLni->pNetworks);
    }
}

//!
//! Frees an allocated lni_t structure. A safer call is to use the
//! LNI_FREE() macro.
//!
//! @param[in] pLni a pointer to the structure to free
//!
//! @see LNI_FREE()
//!
//! @pre
//!     The pLni parameter should not be NULL
//!
//! @post
//!
//! @note
//!
void lni_free(lni_t * pLni)
{
    if (pLni) {
        lni_reinit(pLni);

        // lni_reinit will recreate temporary files on disk. They should be removed.
        unlink_handler_file(pLni->pIpTables->ipt_file);
        unlink_handler_file(pLni->pIpSet->ips_file);
        unlink_handler_file(pLni->pEbTables->ebt_filter_file);
        unlink_handler_file(pLni->pEbTables->ebt_nat_file);
        unlink_handler_file(pLni->pEbTables->ebt_asc_file);

        EUCA_FREE(pLni->pIpTables);
        EUCA_FREE(pLni->pIpSet);
        EUCA_FREE(pLni->pEbTables);
        EUCA_FREE(pLni);
    }
}

//!
//! Populates the Local Network Information structure with local network
//! data retrieved from the system. This will later be used by the network
//! drivers to evaluate what really changed between the current network
//! information and the new GNI configuration.
//!
//! @param[in] pLni a pointer to the structure to populate
//!
//! @return 0 on success or 1 on failure
//!
//! @see lni_reinit()
//!
//! @pre
//!     The pLni parameter MUST not be NULL
//!
//! @post
//!     On success the structure is populated. If any error occured, the
//!     content of the structure is non-deterministic
//!
//! @note
//!     To avoid memory leaks, it is mandatory to call lni_reinit() between
//!      calls to lni_populate.
//!
int lni_populate(lni_t * pLni)
{
    int rc = 0;

    // Make sure the structure pointer isn't NULL
    if (pLni == NULL) {
        LOGERROR("Cannot populate Local Network View. Invalid parameters provided\n");
        return (1);
    }
    // pull in latest IPT state
    if ((rc = ipt_handler_repopulate(pLni->pIpTables)) != 0) {
        LOGERROR("Cannot read current IPT rules: check above log errors for details\n");
        LNI_RESET(pLni);
        return (1);
    }
    // pull in latest IPS state
    if ((rc = ips_handler_repopulate(pLni->pIpSet)) != 0) {
        LOGERROR("Cannot read current IPS sets: check above log errors for details\n");
        LNI_RESET(pLni);
        return (1);
    }
    // pull in latest EBT state
    if ((rc = ebt_handler_repopulate(pLni->pEbTables)) != 0) {
        LOGERROR("Cannot read current EBT rules: check above log errors for details\n");
        LNI_RESET(pLni);
        return (1);
    }
    // Retrieve our system network device information
    if ((rc = dev_get_list(NULL, &pLni->pDevices, &pLni->numberOfDevices)) != 0) {
        LOGERROR("Cannot retrieve system network device information.\n");
        LNI_RESET(pLni);
        return (1);
    }
    // Retrieve our system network device information
    if ((rc = dev_get_ips(NULL, &pLni->pNetworks, &pLni->numberOfNetworks)) != 0) {
        LOGERROR("Cannot retrieve system network information.\n");
        LNI_RESET(pLni);
        return (1);
    }

    return (0);
}

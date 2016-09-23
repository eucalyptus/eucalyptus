// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/

#ifndef _INCLUDE_IPS_HANDLER_H_
#define _INCLUDE_IPS_HANDLER_H_

//!
//! @file net/ips_handler.h
//! This file needs a description
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

typedef struct ips_set_t {
    char name[64];
    u32 *member_ips;
    int *member_nms;
    int max_member_ips;
    int ref_count;
} ips_set;

typedef struct ips_handler_t {
    ips_set *sets;
    int max_sets;
    char ips_file[EUCA_MAX_PATH];
    char cmdprefix[EUCA_MAX_PATH];
    int init;
} ips_handler;

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
//! @name IP set APIs
int ips_handler_init(ips_handler *ipsh, const char *cmdprefix);

int ips_system_save(ips_handler *ipsh);
int ips_system_restore(ips_handler *ipsh);

int ips_handler_repopulate(ips_handler *ipsh);
int ips_handler_deploy(ips_handler *ipsh, int dodelete);

int ips_handler_add_set(ips_handler *ipsh, char *setname);
ips_set *ips_handler_find_set(ips_handler *ipsh, char *findset);

int ips_set_add_net(ips_handler *ipsh, char *setname, char *ip, int nm);
u32 *ips_set_find_net(ips_handler *ipsh, char *setname, char *findip, int findnm);

int ips_set_add_ip(ips_handler *ipsh, char *setname, char *ip);
u32 *ips_set_find_ip(ips_handler *ipsh, char *setname, char *findip);

int ips_set_flush(ips_handler *ipsh, char *setname);
int ips_handler_deletesetmatch(ips_handler *ipsh, char *match);

int ips_handler_free(ips_handler *ipsh);
int ips_handler_close(ips_handler *ipsh);

int ips_handler_print(ips_handler *ipsh);
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

#endif /* ! _INCLUDE_IPS_HANDLER_H_ */

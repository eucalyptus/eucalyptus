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

#ifndef _INCLUDE_EBT_HANDLER_H_
#define _INCLUDE_EBT_HANDLER_H_

//!
//! @file net/ebt_handler.h
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

typedef struct ebt_rule_t {
    char ebtrule[1024];
} ebt_rule;

typedef struct ebt_chain_t {
    char name[64];
    char policyname[64];
    char counters[64];
    ebt_rule *rules;
    int max_rules;
    int ref_count;
} ebt_chain;

typedef struct ebt_table_t {
    char name[64];
    ebt_chain *chains;
    int max_chains;
} ebt_table;

typedef struct ebt_handler_t {
    ebt_table *tables;
    int max_tables;
    int init;
    char ebt_filter_file[EUCA_MAX_PATH];
    char ebt_nat_file[EUCA_MAX_PATH];
    char ebt_asc_file[EUCA_MAX_PATH];
    char cmdprefix[EUCA_MAX_PATH];
} ebt_handler;

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
//! @name EB Tables API
int ebt_handler_init(ebt_handler *ebth, const char *cmdprefix);
int ebt_handler_free(ebt_handler *ebth);
int ebt_handler_close(ebt_handler *ebth);

int ebt_system_save(ebt_handler *ebth);
int ebt_system_restore(ebt_handler *ebth);

int ebt_handler_repopulate(ebt_handler *ebth);
int ebt_handler_deploy(ebt_handler *ebth);
int ebt_handler_update_refcounts(ebt_handler *ebth);

int ebt_handler_add_table(ebt_handler *ebth, char *tablename);
ebt_table *ebt_handler_find_table(ebt_handler *ebth, char *findtable);

int ebt_table_add_chain(ebt_handler *ebth, char *tablename, char *chainname, char *policyname, char *counters);
ebt_chain *ebt_table_find_chain(ebt_handler *ebth, char *tablename, char *findchain);

int ebt_chain_add_rule(ebt_handler *ebth, char *tablename, char *chainname, char *newrule);
ebt_rule *ebt_chain_find_rule(ebt_handler *ebth, char *tablename, char *chainname, char *findrule);

int ebt_chain_flush(ebt_handler *ebth, char *tablename, char *chainname);
int ebt_chain_flush_rule(ebt_handler *ebth, char *tablename, char *chainname, char *findrule);

int ebt_table_deletechainmatch(ebt_handler *ebth, char *tablename, char *chainmatch);
int ebt_table_deletechainempty(ebt_handler *ebth, char *tablename);

int ebt_handler_print(ebt_handler *ebth);
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

#endif /* ! _INCLUDE_EBT_HANDLER_H_ */

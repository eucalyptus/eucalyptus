// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2014 Eucalyptus Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 ************************************************************************/

#ifndef _INCLUDE_IMAGER_H_
#define _INCLUDE_IMAGER_H_

//!
//! @file storage/imager/imager.h
//!
//! imager header file
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <sys/stat.h>                  // mode_t
#include <misc.h>
#include <map.h>
#include <vbr.h>

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

typedef struct _imager_param {
    char *key;
    char *val;
} imager_param;

typedef struct _imager_request {
    struct _imager_command *cmd;
    imager_param *params;
    int index;                         //!< of this command in a sequence
    void *internal;
} imager_request;

typedef struct _imager_command {
    char *name;
    const char **(*parameters) ();     //!< returns valid parameter names and info for each
    int (*validate) (imager_request *); //!< verifies parameters, returning 0 if all is well
    artifact *(*requirements) (imager_request *, artifact * prev_art) _attribute_wur_;  //!< checks on input, records output
    int (*cleanup) (imager_request *, boolean);
} imager_command;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

extern boolean vddk_available;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

void err(const char *format, ...) _attribute_format_(1, 2) _attribute_noreturn_;

void print_req(imager_request * req);
char *parse_loginpassword(const char *s) _attribute_wur_;
s64 parse_bytes(const char *s);
int verify_readability(const char *path);
char *get_euca_home(void);
map *get_artifacts_map(void);
int ensure_path_exists(const char *path, mode_t mode);
int ensure_dir_exists(const char *path, mode_t mode);
artifact *skip_sentinels(artifact * root);

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

#endif /* ! _INCLUDE_IMAGER_H_ */

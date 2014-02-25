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

#ifndef _INCLUDE_IMG_H_
#define _INCLUDE_IMG_H_

//!
//! @file storage/imager/img.h
//!
//! Need Description
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

#define SIZE                                     512

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

typedef struct _img_creds {
    enum { NONE, PASSWORD, X509CREDS, SSHKEY } type;
    char login[SIZE];
    char password[SIZE];
    char pk_path[SIZE];
    char cert_path[SIZE];
    char ssh_key_path[SIZE];
} img_creds;

typedef struct _img_loc {
    enum { PATH, HTTP, HTTPS, VSPHERE, OBJECTSTORAGE, SFTP } type;
    char url[SIZE];
    char path[SIZE];                   //!< dir/file
    char dir[SIZE];
    char file[SIZE];
    char host[SIZE];
    char params[SIZE];
    int port;
    char vsphere_dc[SIZE];
    char vsphere_ds[SIZE];
    char vsphere_vmx_ds[SIZE];
    char vsphere_vmx_path[SIZE];
    img_creds creds;
} img_loc;

typedef struct _img_spec {
    char id[SIZE];
    img_loc location;
    int size;
} img_spec;

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

int parse_img_spec(img_loc * loc, const char *str);
int img_init_spec(img_spec * spec, const char *id, const char *loc, const img_creds * creds);
void img_cleanup(void);
void print_img_spec(const char *name, const img_spec * spec);

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

#endif /* ! _INCLUDE_IMG_H_ */

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

#ifndef _INCLUDE_CMD_H_
#define _INCLUDE_CMD_H_

//!
//! @file storage/imager/cmd.h
//! header that defines signatures of all commands
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

#define EUCA_NB_IMAGER_CMD                       6

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
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

extern imager_command known_cmds[EUCA_NB_IMAGER_CMD];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name FSCK commands handlers
const char **fsck_parameters(void);
artifact *fsck_requirements(imager_request * req, artifact * prev_art) _attribute_wur_;
int fsck_validate(imager_request * req);
//! @}

//! @{
//! @name Prepare commands handlers
const char **prepare_parameters(void);
int prepare_validate(imager_request * req);
artifact *prepare_requirements(imager_request * req, artifact * prev_art) _attribute_wur_;
int prepare_cleanup(imager_request * req, boolean last);
//! @}

//! @{
//! @name Convert commands handlers
const char **convert_parameters();
int convert_validate(imager_request * req);
artifact *convert_requirements(imager_request * req, artifact * prev_art) _attribute_wur_;
int convert_cleanup(imager_request * req, boolean last);
//! @}

//! @{
//! @name Upload commands handlers
const char **upload_parameters(void);
int upload_validate(imager_request * req);
artifact *upload_requirements(imager_request * req, artifact * prev_art) _attribute_wur_;
int upload_cleanup(imager_request * req, boolean last);
//! @}

//! @{
//! @name Bundle commands handlers
const char **bundle_parameters(void);
int bundle_validate(imager_request * req);
artifact *bundle_requirements(imager_request * req, artifact * prev_art) _attribute_wur_;
int bundle_cleanup(imager_request * req, boolean last);
//! @}

//! @{
//! @name Extract commands handlers
const char **extract_parameters(void);
int extract_validate(imager_request * req);
artifact *extract_requirements(imager_request * req, artifact * prev_art) _attribute_wur_;
int extract_cleanup(imager_request * req, boolean last);
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

#endif /* ! _INCLUDE_CMD_H_ */

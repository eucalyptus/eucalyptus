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

//!
//! @file storage/imager/cmd_fsck.c
//! Need Description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <eucalyptus.h>
#include <diskutil.h>
#include <assert.h>
#include "imager.h"
#include "vmdk.h"

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

static const char *params[] = {
    NULL,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

const char **fsck_parameters(void);
int fsck_validate(imager_request * req);

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
//!
//!
//! @return The list of parameters for the fsck commands
//!
const char **fsck_parameters(void)
{
    return params;
}

//!
//!
//!
//! @param[in] req
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int fsck_validate(imager_request * req)
{
    print_req(req);

    if (diskutil_init(TRUE)) {         // imager may need GRUB
        LOGERROR("failed to initialize diskutil\n");
        return EUCA_ERROR;
    }

    return EUCA_OK;
}

//!
//!
//!
//! @param[in] req
//! @param[in] prev_art
//!
//! @return A pointer to the newly created artifact
//!
artifact *fsck_requirements(imager_request * req, artifact * prev_art)
{
    artifact *this_art = NULL;

    assert(req);
    prev_art = skip_sentinels(prev_art);

    this_art = art_alloc("fsck",       // pick non-existing file system path (URL should do)
                         "",           // doesn't matter for fsck
                         -1LL,         // ditto
                         FALSE,        // ditto
                         FALSE,        // ditto
                         FALSE,        // ditto
                         NULL, NULL); // not using the VBR
    if (this_art == NULL)
        err("out of memory");

    this_art->id_is_path = FALSE;      // we want work blobstore to be opened
    this_art->may_be_cached = TRUE;    // we want cache blobstore to be opened
    art_add_dep(this_art, prev_art);
    return this_art;
}

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
//! @file storage/imager/cmd_extract.c
//! Need Description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>                    // strcmp
#include <unistd.h>                    // access
#include <assert.h>                    // duh
#include <fcntl.h>

#include "eucalyptus.h"
#include "misc.h"
#include "imager.h"
#include "diskutil.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _M                                       "* "   // mandatory
#define _IN                                      "in"
#define _OUT                                     "out"

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

typedef struct _extract_params {
    char *in;
    char *out;
} extract_params;

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
    _IN, "local name for the input object",
    _OUT, "local name for the output object",
    NULL,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int extract_creator(artifact * a);

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
//! @return The list of parameters for the extract commands
//!
const char **extract_parameters(void)
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
int extract_validate(imager_request * req)
{
    extract_params *state = NULL;
    imager_param *p = NULL;

    print_req(req);

    if ((state = calloc(1, sizeof(extract_params))) == NULL)
        err("out of memory");

    // record in 'state' all specified parameters
    for (p = req->params; p != NULL && p->key != NULL; p++) {
        if (strcmp(p->key, _IN) == 0) {
            state->in = p->val;
        } else if (strcmp(p->key, _OUT) == 0) {
            state->out = p->val;
        } else {
            err("invalid parameter '%s' for command 'extract'", p->key);
        }
    }

    if (state->in == NULL) {
        err("parameter '" _IN "' must be specified");
    }

    if (state->out == NULL) {
        err("parameter '" _OUT "' must be specified");
    }

    int fd = open(state->out, O_RDONLY);
    if (fd >= 0) {
        close(fd);
        err("file '%s' already exists", state->out);
    }

    req->internal = ((void *)state);   // save pointer to find it later
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] req
//! @param[in] prev_art
//!
//! @return A pointer to a newly created artifact
//!
artifact *extract_requirements(imager_request * req, artifact * prev_art)
{
    extract_params *state = NULL;
    artifact *this_art = NULL;

    assert(req);
    assert(req->internal);
    state = ((extract_params *) req->internal);

    // _validate should have enforced that
    assert(strlen(state->in) > 0);
    assert(strlen(state->out) > 0);

    this_art = art_alloc(state->out,   // pick non-existing file system path
                         "",           // doesn't matter for upload
                         1LL,          // ditto
                         FALSE,        // ditto
                         FALSE,        // ditto
                         FALSE,        // ditto
                         extract_creator, NULL);    // not using the VBR
    if (this_art == NULL)
        err("out of memory");

    if (diskutil_init(FALSE)) {
        err("failed to initialize diskutil library");
    }

    this_art->internal = ((void *)state);
    this_art->id_is_path = TRUE;       // say that this is a file and pick non-existing path
    art_add_dep(this_art, prev_art);
    return this_art;
}

//!
//! Creator callback for the extract operation
//!
//! @param[in] a
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure or the result of
//!         vmdk_extract_local() or vmdk_extract_remote().
//!
//! @see vmdk_extract_local(), vmdk_extract_remote()
//!
static int extract_creator(artifact * a)
{
    int ret = EUCA_ERROR;
    extract_params *state = NULL;
    s64 last = 0;
    s64 first = 0;

    assert(a);
    assert(a->internal);
    state = ((extract_params *) a->internal);
    art_set_instanceId("extract");     // for logging

    // do the conversion to create the artifact
    LOGINFO("extracting partition to '%s'\n", state->out);

    if (diskutil_sectors(state->in, 1, ((long long *)&first), ((long long *)&last)) == EUCA_OK) {
        LOGINFO("partition 1: [%ld-%ld] sectors\n", first, last);
    } else {
        LOGERROR("can't find root partition information\n");
        return ret;
    }

    ret = diskutil_dd2(state->in, state->out, 512, last - first + 1, 0, first);

    if (ret != EUCA_OK) {
        LOGERROR("failed to extract root partition from '%s' to '%s'\n", state->in, state->out);
    }

    return ret;
}

//!
//!
//!
//! @param[in] req
//! @param[in] last
//!
//! @return Always return EUCA_OK for now.
//!
int extract_cleanup(imager_request * req, boolean last)
{
    extract_params *state = ((extract_params *) req->internal);

    LOGINFO("cleaning up for '%s'...\n", req->cmd->name);
    EUCA_FREE(state);
    return (EUCA_OK);
}

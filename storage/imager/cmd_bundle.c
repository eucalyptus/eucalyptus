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
//! @file storage/imager/cmd_bundle.c
//! Need Description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h>                    // strcmp
#include <unistd.h>                    // access, exec
#include <sys/wait.h>                  // waitpid
#include <eucalyptus.h>
#include <misc.h>
#include <objectstorage.h>
#include <map.h>
#include <vmdk.h>
#include "imager.h"
#include "cache.h"
#include "img.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _M                                       "* "   // mandatory
#define _IN                                      "in"
#define _BUKT                                    "bucket"
#define _CLEAN                                   "clean"
#define _S3P                                     "s3-policy"
#define _S3PS                                    "s3-policy-sig"
#define _KID                                     "kernel"
#define _RID                                     "ramdisk"

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

typedef struct _bundle_params {
    char *bucket;
    char *in;
    boolean clean;
    char *s3policy;
    char *s3policysig;
    char *kernelid;
    char *ramdiskid;
} bundle_params;

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

static char check_cmd[1024] = "";
static char upload_cmd[1024] = "";
static char delete_cmd[1024] = "";

static const char *params[] = {
    _M _BUKT, "name of the bucket to upload the bundle into",
    _IN, "local name for the object to bundle",
    _CLEAN, "instead of bundling, clean up results of bundling",
    _S3P, "S3 policy to use when bundling",
    _S3PS, "S3 policy's signature",
    _KID, "ID of the kernel image to associate with the machine bundle",
    _RID, "ID of the ramdisk image to associate with the machine bundle",
    NULL,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int bundle_clean(bundle_params * state);
static int bundle_creator(artifact * a);

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
//! Retrieves the list of bundle command parameters
//!
//! @return The list of bundle command parameters
//!
const char **bundle_parameters(void)
{
    return params;
}

//!
//!
//!
//! @param[in] req
//!
//! @return Always return EUCA_OK for now
//!
int bundle_validate(imager_request * req)
{
    imager_param *p = NULL;
    bundle_params *state = NULL;
    static char *helpers_name[3] = { NULL };

    print_req(req);

    if ((state = EUCA_ZALLOC(1, sizeof(bundle_params))) == NULL)
        err("out of memory");

    // defaults
    state->clean = FALSE;

    // record in 'state' all specified parameters
    for (p = req->params; ((p != NULL) && (p->key != NULL)); p++) {
        if (strcmp(p->key, _IN) == 0) {
            state->in = p->val;
        } else if (strcmp(p->key, _BUKT) == 0) {
            state->bucket = p->val;
        } else if (strcmp(p->key, _CLEAN) == 0) {
            state->clean = parse_boolean(p->val);
        } else if (strcmp(p->key, _S3P) == 0) {
            state->s3policy = parse_loginpassword(p->val);
        } else if (strcmp(p->key, _S3PS) == 0) {
            state->s3policysig = parse_loginpassword(p->val);
        } else if (strcmp(p->key, _KID) == 0) {
            state->kernelid = p->val;
        } else if (strcmp(p->key, _RID) == 0) {
            state->ramdiskid = p->val;
        } else {
            err("invalid parameter '%s' for command 'bundle'", p->key);
        }
    }

    // ensure mandatory params are present
    if (state->bucket == NULL)
        err("missing mandatory parameter '" _BUKT "'");

    if (state->in == NULL) {
        if (req->index < 1)
            err("parameter '" _IN "' must be specified for first command in a sequence");
        else
            state->in = "";            // so it is set to something
    }

    if (state->ramdiskid == NULL) {
        state->ramdiskid = "";
    }

    if (state->kernelid == NULL) {
        state->kernelid = "";
    }
    // verify diskutil to which we shell out
    snprintf(check_cmd, 1024, "euca-check-bucket");
    snprintf(upload_cmd, 1024, "euca-bundle-upload");
    snprintf(delete_cmd, 1024, "euca-delete-bundle");

    helpers_name[0] = check_cmd;
    helpers_name[1] = upload_cmd;
    helpers_name[2] = delete_cmd;

    if (verify_helpers(helpers_name, NULL, 3) > 0) {
        err("failed to find required euca2ools\n");
    }
    // save pointer to find it later
    req->internal = ((void *)state);
    return EUCA_OK;
}

//!
//! Function description.
//!
//! @param[in] req
//! @param[in] prev_art
//!
//! @return A pointer to a newly allocated artifact
//!
//! @pre The req and req->internal values must not be NULL.
//!
artifact *bundle_requirements(imager_request * req, artifact * prev_art)
{
    s64 size_bytes = 0;
    artifact *this_art = NULL;
    bundle_params *state = NULL;

    assert(req);
    assert(req->internal);
    prev_art = skip_sentinels(prev_art);

    state = ((bundle_params *) req->internal);

    // _validate should have enforced that
    assert(((strlen(state->in) > 0) || (prev_art != NULL)));

    // calculate output size by assuming it will be the same as input size
    if (prev_art) {
        size_bytes = prev_art->size_bytes;
    } else {
        size_bytes = file_size(state->in);
    }

    if (size_bytes < 0) {
        LOGINFO("failed to locate required input '%s'\n", state->in);
        return NULL;
    }

    this_art = art_alloc(state->bucket, // pick non-existing file system path (bucket should do)
                         "",           // sig doesn't matter for bundling
                         -1LL,         // ditto
                         FALSE,        // ditto
                         FALSE,        // ditto
                         FALSE,        // ditto
                         bundle_creator, NULL); // not using the VBR

    if (this_art == NULL)
        err("out of memory");

    this_art->internal = ((void *)state);
    this_art->id_is_path = TRUE;       // say that this is a file and pick non-existing path
    art_add_dep(this_art, prev_art);
    return this_art;
}

//!
//!
//!
//! @param[in] state
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @see euca_execlp()
//!
//! @note Used only when clean=yes is specified
//!
static int bundle_clean(bundle_params * state)
{
    int rc = 0;
    char stdoutstr[1024] = "";
    char stderrstr[1024] = "";

    LOGDEBUG("invoking %s -b %s -p %s --euca-auth\n", delete_cmd, state->bucket, state->in);
    if ((rc = euca_execlp(NULL, delete_cmd, stdoutstr, stderrstr, 1024, 2, "-b", state->bucket, "-p", state->in, "--euca-auth", NULL)) != EUCA_OK) {
        LOGERROR("failed to clean up bundling with '%s -b %s -p %s --euca-auth'=%d\n", delete_cmd, state->bucket, state->in, rc);
        return EUCA_ERROR;
    }

    LOGINFO("bundle deleted\n");
    return EUCA_OK;
}

//!
//! Creator callback for the upload operation
//!
//! @param[in] a
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure or the result of bundle_clean()
//!
//! @see bundle_clean()
//!
static int bundle_creator(artifact * a)
{
    int ret = EUCA_ERROR;
    const char *in_path = NULL;
    bundle_params *state = NULL;

    assert(a);
    assert(a->internal);

    state = ((bundle_params *) a->internal);
    if (state->clean)
        return bundle_clean(state);

    if (a->deps[0]) {
        if (a->deps[0]->id_is_path) {
            in_path = a->deps[0]->id;
        } else {
            in_path = blockblob_get_file(a->deps[0]->bb);
        }
    } else {
        assert(strlen(state->in));
        in_path = state->in;
    }

    if (check_file(in_path)) {
        LOGERROR("error: input file '%s' not found\n", in_path);
        return EUCA_ERROR;
    }

    LOGINFO("bundling to '%s'\n", state->bucket);
    if ((strlen(state->kernelid) > 0) && (strlen(state->ramdiskid) > 0)) {
        ret = euca_execlp(NULL, upload_cmd, "-i", in_path, "-d", get_work_dir(), "-b", state->bucket, "-c", state->s3policy, "--policysignature", state->s3policysig, "--kernel",
                          state->kernelid, "--ramdisk", state->ramdiskid, "--euca-auth", NULL);
    } else {
        ret = euca_execlp(NULL, upload_cmd, "-i", in_path, "-d", get_work_dir(), "-b", state->bucket, "-c", state->s3policy, "--policysignature", state->s3policysig,
                          "--euca-auth", NULL);
    }

    if (ret != EUCA_OK) {
        LOGERROR("failed to bundle '%s' and upload to '%s'\n", in_path, state->bucket);
    }

    return ret;
}

//!
//!
//!
//! @param[in] req
//! @param[in] last
//!
//! @return Always return EUCA_OK
//!
int bundle_cleanup(imager_request * req, boolean last)
{
    bundle_params *state = ((bundle_params *) req->internal);

    LOGINFO("cleaning up for '%s'...\n", req->cmd->name);
    EUCA_FREE(state);
    return (EUCA_OK);
}

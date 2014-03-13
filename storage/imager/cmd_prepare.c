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
//! @file storage/imager/cmd_prepare.c
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
#include <unistd.h>                    // access
#include <eucalyptus.h>
#include <misc.h>
#include <objectstorage.h>
#include <euca_string.h>
#include <map.h>
#include "imager.h"
#include "cache.h"
#include "img.h"
#include "vmdk.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define BOOT_VBR_FORMAT                          "boot:none:104857600:ext3:sda%d:none"

#define _M                                       "* "   // mandatory
#define _BOOT                                    "boot"
#define _CACHE                                   "cache"
#define _ID                                      "id"
#define _KEY                                     "key"
#define _OUT                                     "out"
#define _VBR                                     "vbr"
#define _WORK                                    "work"
#define _ACTION                                  "action"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define ACTION_DOWNLOAD                          00001
#define ACTION_CONVERT                           00002

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

typedef struct _prepare_params {
    boolean bootable;
    boolean cache;
    char *id;
    char *sshkey;
    char *out;
    char *vbrs[EUCA_MAX_VBRS];
    int total_vbrs;
    virtualMachine vm;
    boolean work;
    int action;
} prepare_params;

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
    _BOOT, "make bootable disk? (default=no)",
    _CACHE, "cache the object when possible? (default=true)",
    _ID, "ID of the instance (for better logging)",
    _KEY, "SSH key (or path to one) to inject into the instance",
    _OUT, "local name of the output object",
    _M _VBR, "one or more virtual boot records:\n"
        "\t\t\t[type:id:size:format:guestDeviceName:resourceLocation]\n"
        "\t\t\t      where type = {machine|kernel|ramdisk|ephemeral|ebs}\n"
        "\t\t\t              id = {none|emi-...|eki-...|eri-...|vol-...}\n"
        "\t\t\t            size = {-1|NNNNNN} in bytes (required for 'ephemeral')\n"
        "\t\t\t          format = {none|swap|ext2|ext3} (requierd for 'ephemeral')\n" "\t\t\t guestDeviceName = x?[vhsf]d[a-z]?[1-9]*\n" "\t\t\tresourceLocation = http://...",
    _WORK, "work copy of results is required (default=false)",
    _ACTION, "action: {download|convert} (default=download+convert)",
    NULL,
};

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
//! @return The list of parameters for the prepare commands
//!
const char **prepare_parameters(void)
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
int prepare_validate(imager_request * req)
{
    int i = 0;
    int num_sda_parts = 0;
    imager_param *p = NULL;
    prepare_params *state = NULL;

    print_req(req);

    if ((state = EUCA_ZALLOC(1, sizeof(prepare_params))) == NULL)
        err("out of memory");

    // set defaults
    state->bootable = FALSE;
    state->cache = TRUE;
    state->work = FALSE;
    state->action = ACTION_DOWNLOAD | ACTION_CONVERT;

    // record in 'state' all specified parameters
    for (p = req->params; p != NULL && p->key != NULL; p++) {
        if (strcmp(p->key, _CACHE) == 0) {
            state->cache = parse_boolean(p->val);
        } else if (strcmp(p->key, _BOOT) == 0) {
            state->bootable = parse_boolean(p->val);
        } else if (strcmp(p->key, _ID) == 0) {
            state->id = p->val;
        } else if (strcmp(p->key, _KEY) == 0) {
            state->sshkey = parse_loginpassword(p->val);
        } else if (strcmp(p->key, _OUT) == 0) {
            state->out = p->val;
        } else if (strcmp(p->key, _WORK) == 0) {
            state->work = parse_boolean(p->val);
        } else if (strcmp(p->key, _VBR) == 0) {
            if (state->total_vbrs == (EUCA_MAX_VBRS - 1))
                err("too many vbr= parameters");
            state->vbrs[state->total_vbrs++] = p->val;
            if (strstr(p->val, ":sda1:") != NULL || strstr(p->val, ":sda2:") != NULL || strstr(p->val, ":sda3:") != NULL) {
                num_sda_parts++;
            }
        } else if (strcmp(p->key, _ACTION) == 0) {
            if (strcmp(p->val, "download") == 0) {
                state->action = ACTION_DOWNLOAD;
            } else if (strcmp(p->val, "convert") == 0) {
                state->action = ACTION_CONVERT;
            } else {
                err("unknown action parameter '%s' for command 'prepare'", p->val);
            }
        } else {
            err("invalid parameter '%s' for command 'prepare'", p->key);
        }
    }

    // ensure mandatory params are present
    if (state->total_vbrs < 1)
        err("not a single VBR was specified");

    LOGINFO("actions: download=%s convert=%s\n",
            (state->action & ACTION_DOWNLOAD) ? ("yes") : ("no"),
            (state->action & ACTION_CONVERT) ? ("yes") : ("no"))

    // if a bootable disk is requested and the expected number of partitions is present,
    // then add the boot VBR entry so an extra, 4th, boot partition will get created
    if (state->bootable && num_sda_parts > 0) {
        char buf[1024];
        snprintf(buf, sizeof(buf), BOOT_VBR_FORMAT, num_sda_parts+1);
        state->vbrs[state->total_vbrs++] = strdup(buf);
    }

    for (i = 0; i < state->total_vbrs; i++) {
        if (vbr_add_ascii(state->vbrs[i], &(state->vm)))
            err("failed to add VBR record '%s'", state->vbrs[i]);
    }

    if (vbr_parse(&(state->vm), NULL) != EUCA_OK)
        err("failed to validate VBR records");

    // if given an empty key file, just set the pointer to NULL
    if (state->sshkey && strlen(state->sshkey) == 0) {
        EUCA_FREE(state->sshkey);
    }
    // save pointer to find it later
    req->internal = ((void *)state);
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] req
//! @param[in] prev_art
//!
//! @return A pointer to the newly created artifact
//!
artifact *prepare_requirements(imager_request * req, artifact * prev_art)
{
    artifact *result = NULL;
    artifact *sentinel = NULL;
    prepare_params *state = NULL;

    assert(req);
    assert(req->internal);
    assert(prev_art == NULL);
    state = (prepare_params *) req->internal;

    // compute tree of dependencies
    sentinel = vbr_alloc_tree(&(state->vm), // the struct containing the VBR
                              state->bootable,  // TRUE when hypervisors can't take a kernel/ramdisk
                              state->work,  // TRUE when disk will be used by hypervisor on this host
                              ! (state->action & ACTION_DOWNLOAD),   // migration destination => do not bother with download
                              state->sshkey,    // the SSH key
                              state->id);   // ID is for logging
    if (sentinel == NULL)
        err("failed to prepare image %s", state->id);

    assert(sentinel->deps[0]);
    result = sentinel->deps[0];        // result should be disk, not the dummy sentinel
    EUCA_FREE(sentinel);

    // for disk, do_not_download means don't bother constructing it
    // so, if 'convert' action wasn't requested, we'll set do_not_download
    result->do_not_download = ! (state->action & ACTION_CONVERT); 

    if (state->out) {
        // specified ID trumps generated one
        euca_strncpy(result->id, state->out, sizeof(result->id));
    }
    return result;
}

//!
//!
//!
//! @param[in] req
//! @param[in] last
//!
//! @return Always return EUCA_OK for now.
//!
int prepare_cleanup(imager_request * req, boolean last)
{
    prepare_params *state = ((prepare_params *) req->internal);

    LOGINFO("cleaning up for '%s'...\n", req->cmd->name);
    EUCA_FREE(state);
    return (EUCA_OK);
}

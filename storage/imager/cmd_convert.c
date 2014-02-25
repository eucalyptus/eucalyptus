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
//! @file storage/imager/cmd_convert.c
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
#include <errno.h>

#include "eucalyptus.h"
#include "misc.h"
#include "objectstorage.h"
#include "euca_string.h"
#include "imager.h"
#include "img.h"
#include "vmdk.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _M                                       "* "   // mandatory
#define _IN                                      "in"
#define _ITYPE                                   "in-type"
#define _IRANGE                                  "in-range"
#define _CACHE                                   "cache"
#define _LOGIN                                   "login"
#define _OUT                                     "out"
#define _OTYPE                                   "out-type"
#define _PASS                                    "password"
#define _VDC                                     "vsphere-datacenter"
#define _VMX                                     "vsphere-vmx"
#define _VMDK                                    "vsphere-vmdk"

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

typedef struct _convert_params {
    char *in;
    boolean cache;
    enum content_type {
        GUESS = 0,
        DISK,
        VMDK,
    } in_type;
    long long start_sector;
    long long end_sector;
    char *login;
    char *out;
    char *password;
    enum content_type out_type;
    char *vdc;
    char *vmdk;
    char *vmx;
    img_spec *remote;
} convert_params;

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
    _IN, "local name or vSphere URL for the input object",
    _ITYPE, "input object type: {disk|vmdk} (default=guess)",
    _IRANGE, "range of blocks in input: START-END (default: all)",
    _CACHE, "cache the object? (default=yes)",
    _LOGIN, "vSphere login or name of the file containing it",
    _OUT, "local name for the output object",
    _OTYPE, "output object type: {disk|vmdk} (default=guess)",
    _PASS, "vSphere password or name of the file containing it",
    _VDC, "vSphere datacenter name",
    _VMDK, "vSphere path to the disk, including the datastore",
    _VMX, "vSphere path to the VMX, including the datastore",
    NULL,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static enum content_type parse_content_type_enum(const char *s);
static int convert_creator(artifact * a);

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
//! @param[in] s
//!
//! @return The matching enumeration value
//!
static enum content_type parse_content_type_enum(const char *s)
{
    char *lc = euca_strduptolower(s);
    enum content_type val = GUESS;

    if (strcmp(lc, "guess") == 0)
        val = GUESS;
    else if (strcmp(lc, "disk") == 0)
        val = DISK;
    else if (strcmp(lc, "vmdk") == 0)
        val = VMDK;
    else
        err("failed to parse '%s' as content type", lc);

    EUCA_FREE(lc);
    return val;
}

//!
//!
//!
//! @return The list of parameters for the convert commands
//!
const char **convert_parameters(void)
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
int convert_validate(imager_request * req)
{
    img_loc *loc = NULL;
    img_spec *spec = NULL;
    imager_param *p = NULL;
    convert_params *state = NULL;

    print_req(req);

    if (vddk_available == FALSE) {
        LOGERROR("failed to initialize VMware's VDDK (is LD_LIBRARY_PATH set?)\n");
        return EUCA_ERROR;
    }

    if ((state = calloc(1, sizeof(convert_params))) == NULL)
        err("out of memory");

    // default values
    state->cache = TRUE;

    // record in 'state' all specified parameters
    for (p = req->params; p != NULL && p->key != NULL; p++) {
        if (strcmp(p->key, _IN) == 0) {
            state->in = p->val;
        } else if (strcmp(p->key, _ITYPE) == 0) {
            state->in_type = parse_content_type_enum(p->val);
        } else if (strcmp(p->key, _IRANGE) == 0) {
            char * start = strtok(p->val, "-");
            char * end = strtok(NULL, "-");
            if (start == NULL || end == NULL)
                err("failed to parse range parameter " _IRANGE);
            char * endptr;
            errno = 0;
            state->start_sector = strtoll(start, &endptr, 10);
            if (errno != 0 && *endptr != '\0')
                err("failed to parse range parameter " _IRANGE);
            state->end_sector = strtoll(end, &endptr, 10);
            if (errno != 0 && *endptr != '\0')
                err("failed to parse range parameter " _IRANGE);
            if (state->start_sector > state->end_sector)
                err("start sector is greater than end sector in " _IRANGE);
        } else if (strcmp(p->key, _CACHE) == 0) {
            state->cache = parse_boolean(p->val);
        } else if (strcmp(p->key, _LOGIN) == 0) {
            state->login = parse_loginpassword(p->val);
        } else if (strcmp(p->key, _OUT) == 0) {
            state->out = p->val;
        } else if (strcmp(p->key, _OTYPE) == 0) {
            state->out_type = parse_content_type_enum(p->val);
        } else if (strcmp(p->key, _PASS) == 0) {
            state->password = parse_loginpassword(p->val);
        } else if (strcmp(p->key, _VDC) == 0) {
            state->vdc = p->val;
        } else if (strcmp(p->key, _VMDK) == 0) {
            state->vmdk = p->val;
        } else if (strcmp(p->key, _VMX) == 0) {
            state->vmx = p->val;
        } else {
            err("invalid parameter '%s' for command 'convert'", p->key);
        }
    }

    if (state->in == NULL) {
        if (req->index < 1)
            err("parameter '" _IN "' must be specified for first command in a sequence");
        else
            state->in = "";            // so it is set to something
    }
    // ensure paired parameters are both present
    if ((state->login != NULL && state->password == NULL) || (state->login == NULL && state->password != NULL))
        err("both login and password must be specified");

    // figure out what kind of conversion this is
    char * url = NULL;
    if (strncmp("http", state->in, 4) == 0) {   // looks like a URL, so a remote conversion
        if (state->out == NULL)
            err("with a remote conversion '" _OUT "' must be specified");
        state->in_type = VMDK;
        url = state->in;
    }
    if (state->out!=NULL && strncmp("http", state->out, 4) == 0) { // output looks like a URL, so a remote conversion
        if (url != NULL)
            err("conversion from remote to remote VMDK not supported");
        state->out_type = VMDK;
        url = state->out;
    }
    if (url != NULL) { // local conversion
        if (state->in_type == GUESS) {
            if (strstr(".vmdk", state->in) != NULL) {
                state->in_type = VMDK;
            } else {
                state->in_type = DISK;
            }
        }
    }

    if (state->out_type == GUESS) {
        if (state->in_type == VMDK)
            state->out_type = DISK;
        else if (state->in_type == DISK)
            state->out_type = VMDK;
    }
    if (state->in_type == GUESS) {
        if (state->out_type == VMDK)
            state->in_type = DISK;
        else if (state->out_type == DISK)
            state->in_type = VMDK;
    }
    if (state->in_type==state->out_type)
        err("identical input and output types requested");

    if (url != NULL) { // remote conversion
        if (strstr(url, "?dcPath=") != NULL) {    // looks like a vSphere URL
            if (state->vdc) {
                err("with " _VDC " option the URL must not contain a path");
            }
            if (state->vmdk) {
                err("with " _VMDK " option the URL must not contain a path");
            }
        }
        // parse vSphere URL and maybe path
        if ((spec = calloc(1, sizeof(img_spec))) == NULL)
            err("out of memory for img_spec in convert_validate");

        loc = &(spec->location);
        if (parse_img_spec(loc, url) != EUCA_OK)
            err("failed to parse input string '%s'", state->in);

        if (state->vdc) {
            euca_strncpy(loc->vsphere_dc, state->vdc, sizeof(loc->vsphere_dc));
        }

        if (state->vmdk) {
            if (parse_vsphere_path(state->vmdk, loc->vsphere_ds, sizeof(loc->vsphere_ds), loc->path, sizeof(loc->path)) != EUCA_OK)
                err("failed to parse VMDK path");
        }

        if (state->vmx) {
            if (parse_vsphere_path(state->vmx, loc->vsphere_vmx_ds, sizeof(loc->vsphere_vmx_ds), loc->vsphere_vmx_path, sizeof(loc->vsphere_vmx_ds))
                != EUCA_OK)
                err("failed to parse VMX path");
        }
        // add login/password to the spec if we have them
        if (state->login) {
            euca_strncpy(spec->location.creds.login, state->login, sizeof(spec->location.creds.login));
            euca_strncpy(spec->location.creds.password, state->password, sizeof(spec->location.creds.password));
        }

        state->remote = spec;
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
artifact *convert_requirements(imager_request * req, artifact * prev_art)
{
    char *id = NULL;
    char *sig = NULL;
    char *i = NULL;
    char *o = NULL;
    s64 size_bytes = 0;
    char sig_buf[MAX_ARTIFACT_SIG] = "";
    char id_buf[EUCA_MAX_PATH] = "";
    char prev_id_buf[EUCA_MAX_PATH] = "";
    artifact *this_art = NULL;
    convert_params *state = NULL;

    assert(req);
    assert(req->internal);
    prev_art = skip_sentinels(prev_art);
    state = ((convert_params *) req->internal);

    // _validate should have enforced that
    assert(strlen(state->in) > 0 || prev_art != NULL);

    // calculate output size by assuming it will be the same as input size
    if (state->remote && state->in_type == VMDK) { // remote input
        size_bytes = vmdk_get_size(state->remote);
    } else {                           // local input
        if (prev_art) {
            size_bytes = prev_art->size_bytes;
        } else if (strcmp(state->in, "-") == 0) { // standard input
            size_bytes = 0;
        } else {
            size_bytes = file_size(state->in);
        }
    }

    if (size_bytes < 0) {
        LOGINFO("failed to locate required input '%s'\n", state->in);
        return NULL;
    }

    if (prev_art) {
        id = prev_art->id;
        sig = prev_art->sig;
    } else {
        // covert intput files into blobstore IDs by replacing slashes with hyphens
        i = state->in;
        o = prev_id_buf;

        do {
            if (*i == '/') {
                if (i != state->in)
                    *o++ = '-';
            } else
                *o++ = *i;
        } while (*i++ && ((o - prev_id_buf) < sizeof(prev_id_buf)));
        id = prev_id_buf;
        sig = "";                      //! @TODO calculate proper sig for the file
    }

    if (state->out) {
        id = state->out;               // specified ID trumps generated one
    } else {
        snprintf(id_buf, sizeof(id_buf), "%s.%s", id, (state->out_type == VMDK) ? "vmdk" : "disk");  // append dest type
        id = id_buf;
    }

    // append new info to the signature
    snprintf(sig_buf, sizeof(sig_buf), "%s\n\nconverted from=%s to=%s size=%ld\n", sig, state->in, (state->out==NULL)?("(next stage)"):(state->out), size_bytes);
    sig = sig_buf;

    this_art = art_alloc(id, sig, size_bytes, !state->remote,   // do not cache remote conversions
                         TRUE,         // must be a file
                         FALSE,        // should not be hollow
                         convert_creator, NULL);    // not using the VBR
    if (this_art == NULL)
        err("out of memory");

    this_art->internal = ((void *)state);
    this_art->id_is_path = (state->out != NULL);
    art_add_dep(this_art, prev_art);
    return this_art;
}

//!
//! Creator callback for the convert operation
//!
//! @param[in] a
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure or the result of
//!         vmdk_convert_local() or vmdk_convert_remote().
//!
//! @see vmdk_convert_local(), vmdk_convert_remote()
//!
static int convert_creator(artifact * a)
{
    int ret = EUCA_ERROR;
    const char *in_path = NULL;
    const char *out_path = NULL;
    convert_params *state = NULL;

    assert(a);
    assert(a->internal);
    state = ((convert_params *) a->internal);
    art_set_instanceId("convert");     // for logging

    // determine paths for input of output
    if (a->deps[0]) { // there is a dependent artifact upstream, use it
        in_path = blockblob_get_dev(a->deps[0]->bb);
    } else { // no dependents, assume that input is the path itself (or '-' for stdin)
        assert(strlen(state->in));
        in_path = state->in;
    }
    if (a->id_is_path) {
        out_path = a->id;
    } else {
        assert(a->bb);
        out_path = blockblob_get_file(a->bb);
    }
    assert(out_path);

    // do the conversion to create the artifact
    LOGINFO("converting from '%s' to '%s'\n", in_path, out_path);
    if (state->remote) {
        switch(state->in_type) {
        case DISK:
            ret = vmdk_convert_to_remote(in_path, state->remote, state->start_sector, state->end_sector);
            break;
        case VMDK:
            ret = vmdk_convert_from_remote(state->remote, out_path, state->start_sector, state->end_sector);
            break;
        default:
            LOGERROR("unexpected input type (%d)\n", state->in_type);
            break;
        }
    } else {
        assert(state->in_type==DISK); // currently, only DISK->VMDK conversion is supported
        ret = vmdk_convert_local(in_path, out_path, TRUE);
    }

    if (ret != EUCA_OK) {
        LOGERROR("failed to convert '%s' to '%s'\n", in_path, out_path);
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
int convert_cleanup(imager_request * req, boolean last)
{
    convert_params *state = ((convert_params *) req->internal);

    LOGINFO("cleaning up for '%s'...\n", req->cmd->name);
    EUCA_FREE(state);
    return (EUCA_OK);
}

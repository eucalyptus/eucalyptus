// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdio.h>
#include <stdlib.h>
#include <assert.h> 
#include <string.h> // strcmp
#include <unistd.h> // access
#include "eucalyptus.h"
#include "misc.h"
#include "walrus.h"
#include "imager.h"
#include "cache.h"
#include "map.h"
#include "img.h"
#include "vmdk.h"

#define _M     "* " // mandatory
#define _IN    "in"
#define _ITYPE "in-type"
#define _LOGIN "login"
#define _OUT   "out"
#define _PASS  "password"
#define _VDC   "vsphere-datacenter"
#define _VMX   "vsphere-vmx"
#define _VMDK  "vsphere-vmdk"

static char * params [] = {
    _IN,        "local name for the object to upload",
    _ITYPE,     "input object type: {any|vmdk} (default=guess)",
    _LOGIN,     "vSphere login or name of the file containing it",
    _M _OUT,    "URL to upload to (vSphere or not)",
    _PASS,      "vSphere password or name of the file containing it",
    _VDC,       "vSphere datacenter name",
    _VMDK,      "vSphere path to the disk, including the datastore",
    _VMX,       "vSphere path to the VMX, including the datastore",
    NULL
};

typedef struct _upload_params {
    char * in;
    enum content_type {
        _GUESS_CONT=0,
        _ANY_CONT,
        _VMDK_CONT
    } in_type;
    char * login;
    char * out;
    char * password;
    char * vdc;
    char * vmdk;
    char * vmx;
    img_spec * remote;
    enum dest_type {
        _WEB = 0,
        _VSPHERE
    } out_type;
} upload_params;


static enum content_type parse_content_type_enum (const char * s)
{
    char * lc = strduplc (s);
    enum content_type val;

    if (strcmp (lc, "guess")==0) val = _GUESS_CONT;
    else if (strcmp (lc, "any")==0) val = _ANY_CONT;
    else if (strcmp (lc, "vmdk")==0) val = _VMDK_CONT;
    else err ("failed to parse '%s' as content type", lc);
    free (lc);

    return val;
}

char ** upload_parameters ()
{
    return params;
}

int upload_validate (imager_request * req)
{
    print_req (req);

    upload_params * state = calloc (sizeof (upload_params), 1);
    if (state==NULL)
        err ("out of memory");

    // record in 'state' all specified parameters
    for (imager_param * p = req->params; p!=NULL && p->key!=NULL; p++) {
        if (strcmp (p->key, _IN)==0)         {state->in       = p->val; }
        else if (strcmp (p->key, _ITYPE)==0) {state->in_type  = parse_content_type_enum (p->val); }
        else if (strcmp (p->key, _LOGIN)==0) {state->login    = parse_loginpassword (p->val); }
        else if (strcmp (p->key, _OUT)==0)   {state->out      = p->val; }
        else if (strcmp (p->key, _PASS)==0)  {state->password = parse_loginpassword (p->val); }
        else if (strcmp (p->key, _VDC)==0)   {state->vdc      = p->val; }
        else if (strcmp (p->key, _VMDK)==0)  {state->vmdk     = p->val; }
        else if (strcmp (p->key, _VMX)==0)   {state->vmx      = p->val; }
        else
            err ("invalid parameter '%s' for command 'upload'", p->key);
    }

    // ensure mandatory params are present
    if (state->out==NULL) err ("missing mandatory parameter '" _OUT "'");
    if (state->in==NULL) {
        if (req->index<1) err ("parameter '" _IN "' must be specified for first command in a sequence");
        else state->in = ""; // so it is set to something
    }

    // ensure paired parameters are both present
    if ((state->login!=NULL && state->password==NULL) ||
        (state->login==NULL && state->password!=NULL))
        err ("both login and password must be specified");

    // parse the URL
    img_spec * spec = calloc (1, sizeof (img_spec));
    if (spec==NULL)
        err ("out of memory for img_spec in upload_validate");
    img_loc * loc = &(spec->location);
    if (parse_img_spec (loc, state->out)!=OK)
        err ("failed to parse output string '%s'", state->out);

    if (state->in_type == _GUESS_CONT) {
        if (strstr (state->in, ".vmdk")!=NULL) {
            state->in_type = _VMDK_CONT;
        } else {
            state->in_type = _ANY_CONT;
        }
    }

    if (loc->type==VSPHERE) {
        if (state->vdc) {
            err ("with " _VDC " option the URL must not contain a path");
        }
        if (state->vmdk) {
            err ("with " _VMDK " option the URL must not contain a path");
        }
        state->out_type = _VSPHERE;
    } else if (loc->type==HTTP || loc->type==HTTPS) {
        if (state->vdc || state->vmdk) {
            state->out_type = _VSPHERE;
        } else {
            state->out_type = _WEB;
        }
    } else {
        err ("output format not recognized/supported");
    }

    if (state->vdc) {
        safe_strncpy (loc->vsphere_dc, state->vdc, sizeof (loc->vsphere_dc));
    }
    if (state->vmdk)
        if (parse_vsphere_path (state->vmdk, loc->vsphere_ds, sizeof (loc->vsphere_ds), loc->path, sizeof (loc->path))!=OK)
            err ("failed to parse VMDK path");
    if (state->vmx)
        if (parse_vsphere_path (state->vmx, loc->vsphere_vmx_ds, sizeof (loc->vsphere_vmx_ds), loc->vsphere_vmx_path, sizeof (loc->vsphere_vmx_ds))!=OK)
            err ("failed to parse VMX path");

    // add login/password to the spec if we have them
    if (state->login) {
        safe_strncpy (spec->location.creds.login, state->login, sizeof (spec->location.creds.login));
        safe_strncpy (spec->location.creds.password, state->password, sizeof (spec->location.creds.password));
    }

    state->remote = spec;
    req->internal = (void *) state; // save pointer to find it later

    return 0;
}

static int upload_creator (artifact * a);

artifact * upload_requirements (imager_request * req, artifact * prev_art)
{
    assert (req);
    assert (req->internal);
    prev_art = skip_sentinels (prev_art);

    upload_params * state = (upload_params *) req->internal;
    long long size_bytes;
    char sig_buf [MAX_ARTIFACT_SIG];
    char id_buf [EUCA_MAX_PATH];

    // _validate should have enforced that
    assert (strlen(state->in)>0 || prev_art!=NULL); 
    
    artifact * this_art = art_alloc (state->out, // pick non-existing file system path (URL should do)
                                     "", // doesn't matter for upload
                                     -1LL, // ditto
                                     FALSE, // ditto
                                     FALSE, // ditto
                                     FALSE, // ditto
                                     upload_creator, 
                                     NULL); // not using the VBR
    if (this_art==NULL) 
        err ("out of memory");
    this_art->internal = (void *) state;
    this_art->id_is_path = TRUE; // say that this is a file and pick non-existing path
    art_add_dep (this_art, prev_art);

    return this_art;
}

// creator callback for the upload operation
static int upload_creator (artifact * a)
{
    assert (a);
    assert (a->internal);
    upload_params * state = (upload_params *) a->internal;
    art_set_instanceId ("upload"); // for logging
    int ret = ERROR;
    
    const char * in_path;
    if (a->deps[0]) {
        if (a->deps[0]->id_is_path) {
            in_path = a->deps[0]->id;
        } else {
            in_path = blockblob_get_file (a->deps[0]->bb);
        }
    } else {
        assert (strlen (state->in));
        in_path = state->in;
    }
    
    if (check_file (in_path)) {
        logprintfl (EUCAERROR, "error: input file '%s' not found\n", in_path);
        return ERROR;
    }
    
    logprintfl (EUCAINFO, "uploading to '%s'\n", state->out);
    switch (state->out_type) {
    case _WEB:
        err ("upload to arbitrary URLs not supported, sorry");
        break;
    case _VSPHERE: {
        if (state->in_type==_VMDK_CONT) {
            ret = vmdk_clone (in_path, state->remote);
        } else {
            ret = vmdk_upload (in_path, state->remote);
        }
        break;
    }
    default:
        err ("internal error (unexpected output type)");
    }

    if (ret!=OK) {
        logprintfl (EUCAERROR, "error: failed to upload '%s' to '%s'\n", in_path, state->out);
    }

    return ret;
}

int upload_cleanup (imager_request * req, boolean last)
{
    upload_params * state = (upload_params *) req->internal;
    logprintfl (EUCAINFO, "cleaning up for '%s'...\n", req->cmd->name);
    free (state);

    return 0;
}

// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdio.h>
#include <stdlib.h>
#include <string.h> // strcmp
#include <unistd.h> // access
#include <assert.h> // duh
#include "eucalyptus.h"
#include "misc.h"
#include "walrus.h"
#include "imager.h"
#include "img.h"
#include "vmdk.h"

#define _M     "* " // mandatory
#define _IN    "in"
#define _ITYPE "in-type"
#define _CACHE "cache"
#define _LOGIN "login"
#define _OUT   "out"
#define _OTYPE "out-type"
#define _PASS  "password"
#define _VDC   "vsphere-datacenter"
#define _VMX   "vsphere-vmx"
#define _VMDK  "vsphere-vmdk"

static char * params [] = {
    _IN,        "local name or vSphere URL for the input object",
    _ITYPE,     "input object type: {disk|vmdk} (default=guess)",
    _CACHE,     "cache the object? (default=yes)",
    _LOGIN,     "vSphere login or name of the file containing it",
    _OUT,       "local name for the output object",
    _OTYPE,     "output object type: {disk|vmdk} (default=guess)",
    _PASS,      "vSphere password or name of the file containing it",
    _VDC,       "vSphere datacenter name",
    _VMDK,      "vSphere path to the disk, including the datastore",
    _VMX,       "vSphere path to the VMX, including the datastore",
    NULL
};

typedef struct _convert_params {
    char * in;
    boolean cache;
    enum content_type {
        GUESS=0,
        DISK,
        VMDK
    } in_type;
    char * login;
    char * out;
    char * password;
    enum content_type out_type;
    char * vdc;
    char * vmdk;
    char * vmx;
    img_spec * remote;
} convert_params;

static enum content_type parse_content_type_enum (const char * s)
{
    char * lc = strduplc (s);
    enum content_type val;

    if (strcmp (lc, "guess")==0) val = GUESS;
    else if (strcmp (lc, "disk")==0) val = DISK;
    else if (strcmp (lc, "vmdk")==0) val = VMDK;
    else err ("failed to parse '%s' as content type", lc);
    free (lc);

    return val;
}

char ** convert_parameters ()
{
    return params;
}

int convert_validate (imager_request * req)
{
    print_req (req);

    convert_params * state = calloc (sizeof (convert_params), 1);
    if (state==NULL)
        err ("out of memory");

    // default values
    state->cache = TRUE;

    // record in 'state' all specified parameters
    for (imager_param * p = req->params; p!=NULL && p->key!=NULL; p++) {
        if (strcmp (p->key, _IN)==0)         {state->in       = p->val; }
        else if (strcmp (p->key, _ITYPE)==0) {state->in_type  = parse_content_type_enum (p->val); }
        else if (strcmp (p->key, _CACHE)==0) {state->cache    = parse_boolean (p->val); }
        else if (strcmp (p->key, _LOGIN)==0) {state->login    = parse_loginpassword (p->val); }
        else if (strcmp (p->key, _OUT)==0)   {state->out      = p->val; }
        else if (strcmp (p->key, _OTYPE)==0) {state->out_type = parse_content_type_enum (p->val); }
        else if (strcmp (p->key, _PASS)==0)  {state->password = parse_loginpassword (p->val); }
        else if (strcmp (p->key, _VDC)==0)   {state->vdc      = p->val; }
        else if (strcmp (p->key, _VMDK)==0)  {state->vmdk     = p->val; }
        else if (strcmp (p->key, _VMX)==0)   {state->vmx      = p->val; }
        else
            err ("invalid parameter '%s' for command 'convert'", p->key);
    }

    if (state->in==NULL) {
        if (req->index<1) err ("parameter '" _IN "' must be specified for first command in a sequence");
        else state->in = ""; // so it is set to something
    }
    
    // ensure paired parameters are both present
    if ((state->login!=NULL && state->password==NULL) ||
        (state->login==NULL && state->password!=NULL))
        err ("both login and password must be specified");
    
    // figure out what kind of convert this is
    if (strncmp ("http", state->in, 4)==0) { // looks like a URL, see if it is for vSphere
        if (state->out==NULL)
            err ("with a remote conversion '" _OUT "' must be specified");

        if (state->in_type != VMDK && state->in_type != GUESS)
            err ("can only convert from vSphere URLs pointing to a VMDK");

        state->in_type = VMDK;
        if (strstr (state->in, "?dcPath=")!=NULL) { // looks like a vSphere URL
            if (state->vdc) {
                err ("with " _VDC " option the URL must not contain a path");
            }
            if (state->vmdk) {
                err ("with " _VMDK " option the URL must not contain a path");
            }
        }
        
        // parse vSphere URL and maybe path
        img_spec * spec = calloc (1, sizeof (img_spec));
        if (spec==NULL)
            err ("out of memory for img_spec in convert_validate");
        img_loc * loc = &(spec->location);
        if (parse_img_spec (loc, state->in)!=OK)
            err ("failed to parse input string '%s'", state->in);
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

    } else { // must be a file
        if (state->in_type == GUESS) {
            if (strstr (".vmdk", state->in)!=NULL) {
                state->in_type = VMDK;
            } else {
                state->in_type = DISK;
            }
        }
    }

    if (state->out_type == GUESS) {
        if (state->in_type == VMDK) state->out_type = DISK;
        if (state->in_type == DISK) state->out_type = VMDK;
    } else {
        if (state->in_type == DISK && state->out_type == DISK)
            err ("can only convert from disk to a VMDK");
        if (state->in_type == VMDK && state->out_type == VMDK)
            err ("can only convert from a VMDK to a disk");
    }

    req->internal = (void *) state; // save pointer to find it later

    return 0;
}

static int convert_creator (artifact * a);

artifact * convert_requirements (imager_request * req, artifact * prev_art)
{
    assert (req);
    assert (req->internal);
    prev_art = skip_sentinels (prev_art);

    convert_params * state = (convert_params *) req->internal;
    char * from, * to, * id, * sig;
    long long size_bytes;
    char sig_buf [MAX_ARTIFACT_SIG];
    char id_buf [EUCA_MAX_PATH];
    
    // _validate should have enforced that
    assert (strlen(state->in)>0 || prev_art!=NULL); 

    // calculate output size by assuming it will be the same as input size
    if (state->remote) { // remote input
        size_bytes = vmdk_get_size (state->remote);
        from = "vmdk";
        to = "disk";
    } else { // local input
        if (prev_art) {
            size_bytes = prev_art->size_bytes;
        } else {
            size_bytes = file_size (state->in); 
        }
        from = "disk";
        to = "vmdk";
    }
    if (size_bytes < 0) {
        logprintfl (EUCAINFO, "failed to locate required input '%s'\n", state->in);
        return NULL;
    }

    char prev_id_buf [EUCA_MAX_PATH];
    if (prev_art) {
        id = prev_art->id;
        sig = prev_art->sig;
    } else {
        // covert intput files into blobstore IDs by replacing slashes with hyphens
        char * i = state->in;
        char * o = prev_id_buf;
        do {
            if (* i == '/') {
                if (i != state->in) 
                    * o++ = '-';
            } else 
                * o++ = * i;
        } while (* i++ && ((o - prev_id_buf) < sizeof (prev_id_buf)));
        id = prev_id_buf;
        sig = ""; // TODO: calculate proper sig for the file
    }

    if (state->out) {
        id = state->out; // specified ID trumps generated one
    } else {
        snprintf (id_buf, sizeof (id_buf), "%s.%s", id, to); // append dest type
        id = id_buf;
    }

    // append new info to the signature
    snprintf (sig_buf, sizeof (sig_buf), "%s\n\nconverted from=%s to=%s size=%lld\n", sig, from, to, size_bytes);
    sig = sig_buf;
    
    artifact * this_art = art_alloc (id, sig, 
                                     size_bytes,
                                     ! state->remote, // do not cache remote conversions
                                     TRUE, // must be a file
                                     FALSE, // should not be hollow
                                     convert_creator, 
                                     NULL); // not using the VBR
    if (this_art==NULL) 
        err ("out of memory");
    this_art->internal = (void *) state;
    this_art->id_is_path = (state->out!=NULL);
    art_add_dep (this_art, prev_art);

    return this_art;
}

// creator callback for the convert operation
static int convert_creator (artifact * a)
{
    assert (a);
    assert (a->internal);
    convert_params * state = (convert_params *) a->internal;
    art_set_instanceId ("convert"); // for logging
    int ret = ERROR;

    const char * out_path;
    if (a->id_is_path) {
        out_path = a->id;
    } else {
        assert (a->bb);
        out_path = blockblob_get_file (a->bb);
    }
    assert (out_path);

    // do the conversion to create the artifact
    logprintfl (EUCAINFO, "converting to '%s'\n", a->id);
    switch (state->in_type) {
    case DISK: {
        const char * in_path;
        if (a->deps[0]) {
            in_path = blockblob_get_dev (a->deps[0]->bb);
        } else {
            assert (strlen (state->in));
            in_path = state->in;
        }
        ret = vmdk_convert_local (in_path, out_path, TRUE);
        break;
    }
    case VMDK:
        assert (state->remote);
        ret = vmdk_convert_remote (state->remote, out_path);
        break;
    default:
        logprintfl (EUCAERROR, "internal error: unexpected input type\n");
    }

    if (ret!=OK) {
        logprintfl (EUCAERROR, "error: failed to convert '%s' to '%s'\n", a->id, out_path);
    }

    return ret;
}

int convert_cleanup (imager_request * req, boolean last)
{
    convert_params * state = (convert_params *) req->internal;
    logprintfl (EUCAINFO, "cleaning up for '%s'...\n", req->cmd->name);

    // TODO: remove artifact if not cacheable

    free (state);

    return 0;
}

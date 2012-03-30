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
#define _BOOT  "boot"
#define _CACHE "cache"
#define _ID    "id"
#define _KEY   "key"
#define _OUT   "out"
#define _VBR   "vbr"
#define _WORK  "work"

static char * params [] = {
    _BOOT,    "make bootable disk? (default=no)",
    _CACHE,   "cache the object when possible? (default=true)",
    _ID,      "ID of the instance (for better logging)",
    _KEY,     "SSH key (or path to one) to inject into the instance",
    _OUT,     "local name of the output object",
    _M _VBR,  "one or more virtual boot records:\n"
    "\t\t\t[type:id:size:format:guestDeviceName:resourceLocation]\n"
    "\t\t\t      where type = {machine|kernel|ramdisk|ephemeral|ebs}\n"
    "\t\t\t              id = {none|emi-...|eki-...|eri-...|vol-...}\n"
    "\t\t\t            size = {-1|NNNNNN} in bytes (required for 'ephemeral')\n"
    "\t\t\t          format = {none|swap|ext2|ext3} (requierd for 'ephemeral')\n"
    "\t\t\t guestDeviceName = x?[vhsf]d[a-z]?[1-9]*\n"
    "\t\t\tresourceLocation = http://...",
    _WORK,    "work copy of results is required (default=false)",
    NULL
};

typedef struct _prepare_params {
    boolean bootable;
    boolean cache;
    char * id;
    char * sshkey;
    char * out;
    char * vbrs [EUCA_MAX_VBRS];
    int total_vbrs;
    virtualMachine vm;
    boolean work;
} prepare_params;

char ** prepare_parameters ()
{
    return params;
}

int prepare_validate (imager_request * req)
{
    print_req (req);

    prepare_params * state = calloc (sizeof (prepare_params), 1);
    if (state==NULL)
        err ("out of memory");

    // set defaults
    state->bootable = FALSE;
    state->cache = TRUE;
    state->work = FALSE;

    // record in 'state' all specified parameters
    for (imager_param * p = req->params; p!=NULL && p->key!=NULL; p++) {
        if (strcmp (p->key, _CACHE)==0)      {state->cache    = parse_boolean (p->val); }
        else if (strcmp (p->key, _BOOT)==0)  {state->bootable = parse_boolean (p->val); }
        else if (strcmp (p->key, _ID)==0)    {state->id       = p->val; }
        else if (strcmp (p->key, _KEY)==0)   {state->sshkey   = parse_loginpassword (p->val); }
        else if (strcmp (p->key, _OUT)==0)   {state->out      = p->val; }
        else if (strcmp (p->key, _WORK)==0)  {state->work     = parse_boolean (p->val); }
        else if (strcmp (p->key, _VBR)==0)   {
            if (state->total_vbrs==(EUCA_MAX_VBRS-1))
                err ("too many vbr= parameters");
            state->vbrs [state->total_vbrs++] = p->val;
        } else  {
            err ("invalid parameter '%s' for command 'prepare'", p->key);
        }
    }
    
    // ensure mandatory params are present
    if (state->total_vbrs<1)
        err ("not a single VBR was specified");

    for (int i=0; i<state->total_vbrs; i++)
        if (vbr_add_ascii (state->vbrs [i], &(state->vm))) 
            err ("failed to add VBR record '%s'", state->vbrs [i]);
    
    if (vbr_parse (&(state->vm), NULL)!=OK)
        err ("failed to validate VBR records");
    
    // if given an empty key file, just set the pointer to NULL
    if (state->sshkey && strlen (state->sshkey)==0) {
        free (state->sshkey);
        state->sshkey = NULL;
    }

    req->internal = (void *) state; // save pointer to find it later

    return 0;
}

artifact * prepare_requirements (imager_request * req, artifact * prev_art)
{
    assert (req);
    assert (req->internal);
    assert (prev_art==NULL);

    prepare_params * state = (prepare_params *) req->internal;

    // compute tree of dependencies
    artifact * sentinel = vbr_alloc_tree (&(state->vm), // the struct containing the VBR
                                          state->bootable, // TRUE when hypervisors can't take a kernel/ramdisk
                                          state->work, // TRUE when disk will be used by hypervisor on this host
                                          state->sshkey, // the SSH key
                                          state->id); // ID is for logging
    if (sentinel==NULL)
        err ("failed to prepare image %s", state->id);

    assert (sentinel->deps[0]);
    artifact * result = sentinel->deps[0]; // result should be disk, not the dummy sentinel
    free (sentinel);

    if (state->out) { // specified ID trumps generated one
        safe_strncpy (result->id, state->out, sizeof (result->id));
    }

    return result;
}

int prepare_cleanup (imager_request * req, boolean last)
{
    prepare_params * state = (prepare_params *) req->internal;
    logprintfl (EUCAINFO, "cleaning up for '%s'...\n", req->cmd->name);
    free (state);

    return 0;
}

// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <string.h> // strcmp
#include <unistd.h> // access, exec
#include <sys/wait.h> // waitpid
#include "eucalyptus.h"
#include "misc.h"
#include "walrus.h"
#include "imager.h"
#include "cache.h"
#include "map.h"
#include "img.h"
#include "vmdk.h"

static char check_cmd  [1024];
static char upload_cmd [1024];
static char delete_cmd [1024];

#define _M     "* " // mandatory
#define _IN    "in"
#define _BUKT  "bucket"
#define _CLEAN "clean"
#define _S3P   "s3-policy"
#define _S3PS  "s3-policy-sig"

static char * params [] = {
    _M _BUKT,   "name of the bucket to upload the bundle into",
    _IN,        "local name for the object to bundle",
    _CLEAN,     "instead of bundling, clean up results of bundling",
    _S3P,       "S3 policy to use when bundling",
    _S3PS,      "S3 policy's signature",
    NULL
};

typedef struct _bundle_params {
    char * bucket;
    char * in;
    boolean clean;
    char * s3policy;
    char * s3policysig;
} bundle_params;

char ** bundle_parameters ()
{
    return params;
}

int bundle_validate (imager_request * req)
{
    print_req (req);

    bundle_params * state = calloc (sizeof (bundle_params), 1);
    if (state==NULL)
        err ("out of memory");

    // defaults
    state->clean = FALSE;

    // record in 'state' all specified parameters
    for (imager_param * p = req->params; p!=NULL && p->key!=NULL; p++) {
        if (strcmp (p->key, _IN)==0)         {state->in          = p->val; }
        else if (strcmp (p->key, _BUKT)==0)  {state->bucket      = p->val; }
        else if (strcmp (p->key, _CLEAN)==0) {state->clean       = parse_boolean (p->val); }
        else if (strcmp (p->key, _S3P)==0)   {state->s3policy    = parse_loginpassword (p->val); }
        else if (strcmp (p->key, _S3PS)==0)  {state->s3policysig = parse_loginpassword (p->val); }
        else
            err ("invalid parameter '%s' for command 'bundle'", p->key);
    }

    // ensure mandatory params are present
    if (state->bucket==NULL) err ("missing mandatory parameter '" _BUKT "'");
    if (state->in==NULL) {
        if (req->index<1) err ("parameter '" _IN "' must be specified for first command in a sequence");
        else state->in = ""; // so it is set to something
    }

    // verify diskutil to which we shell out
    snprintf(check_cmd, 1024, "euca-check-bucket");
    snprintf(upload_cmd, 1024, "euca-bundle-upload");
    snprintf(delete_cmd, 1024, "euca-delete-bundle");
    static char * helpers_name [3] = { 
           check_cmd, upload_cmd, delete_cmd
    };
    
    if (verify_helpers (helpers_name, NULL, 3) > 0) {
        err("failed to find required euca2ools\n");
    }

    req->internal = (void *) state; // save pointer to find it later

    return 0;
}

static int bundle_creator (artifact * a);

artifact * bundle_requirements (imager_request * req, artifact * prev_art)
{
    assert (req);
    assert (req->internal);
    prev_art = skip_sentinels (prev_art);

    bundle_params * state = (bundle_params *) req->internal;
    long long size_bytes;

   // if (state->clean) return OK;

    // _validate should have enforced that
    assert (strlen(state->in)>0 || prev_art!=NULL); 

    // calculate output size by assuming it will be the same as input size
    if (prev_art) {
        size_bytes = prev_art->size_bytes;
    } else {
        size_bytes = file_size (state->in); 
    }
    if (size_bytes < 0) {
        logprintfl (EUCAINFO, "failed to locate required input '%s'\n", state->in);
        return NULL;
    }
    artifact * this_art = art_alloc (state->bucket, // pick non-existing file system path (bucket should do)
                                     "", // sig doesn't matter for bundling
                                     -1LL, // ditto
                                     FALSE, // ditto
                                     FALSE, // ditto
                                     FALSE, // ditto
                                     bundle_creator, 
                                     NULL); // not using the VBR

    if (this_art==NULL) 
        err ("out of memory");
    this_art->internal = (void *) state;
    this_art->id_is_path = TRUE; // say that this is a file and pick non-existing path
    art_add_dep (this_art, prev_art);
    
    return this_art;
}

// used only when clean=yes is specified
static int bundle_clean (bundle_params * state)
{
    char cmd [1024];
    snprintf (cmd, sizeof(cmd), "%s -b %s -p %s --euca-auth ", delete_cmd, state->bucket, state->in);
    logprintfl (EUCADEBUG, "invoking %s\n", cmd);
    int rc = system(cmd);
    rc = rc>>8;
    if (rc) {
        logprintfl (EUCAERROR, "failed to clean up bundling with '%s'=%d\n", cmd, rc);
        return ERROR;
    } else {
        logprintfl (EUCAINFO, "bundle deleted\n");
        return OK;
    }
}

// creator callback for the upload operation
static int bundle_creator (artifact * a)
{
    assert (a);
    assert (a->internal);
    bundle_params * state = (bundle_params *) a->internal;
    int ret = ERROR;

    if (state->clean) return bundle_clean (state);

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

    logprintfl (EUCAINFO, "bundling to '%s'\n", state->bucket);
    int status;
    int pid = fork();
    if (!pid) {
        exit (execlp (upload_cmd, upload_cmd,
                     "-i", in_path,
                     "-d", get_work_dir(),
                     "-b", state->bucket,
                     "-c", state->s3policy,
                     "--policysignature", state->s3policysig,
                     "--euca-auth", NULL));
    } else {
        ret = waitpid(pid, &status, 0);
        if (WIFEXITED(status)) {
            ret = WEXITSTATUS(status);
        } else {
            ret = ERROR;
        }
    }

    if (ret!=OK) {
        logprintfl (EUCAERROR, "error: failed to bundle '%s' and upload to '%s'\n", in_path, state->bucket);
    }

    return ret;
}

int bundle_cleanup (imager_request * req, boolean last)
{
    bundle_params * state = (bundle_params *) req->internal;

    logprintfl (EUCAINFO, "cleaning up for '%s'...\n", req->cmd->name);

    // TODO: remove artifact if not cacheable

    free (state);

    return 0;
}

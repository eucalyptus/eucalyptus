// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdio.h>
#include <stdlib.h>
#include <string.h> // strcmp
#include <unistd.h> // access
#include <errno.h> // duh
#include "eucalyptus.h"
#include "misc.h"
#include "imager.h"
#include "cache.h"
#include "map.h"
#include "diskutil.h"
#include "diskfile.h"

#define _M       "* " // mandatory
#define _CACHE   "cache"
#define _IN      "in"
#define _INF     "in-file"
#define _KERNEL  "kernel"
#define _MOD     "modify"
#define _OUT     "out"
#define _OUTF    "out-file"
#define _PART    "part"
#define _RAMDISK "ramdisk"
#define _TUNE    "tune"
#define _WORK    "work"

static char * params [] = {
    _CACHE,     "cache the output object? (default=no)",
    _M _IN,     "local name of the input object",
    _M _INF,    "path to file to inject into input object", // TODO: make in-file optional when kernel/ramdisk are given?
    _KERNEL,    "add this kernel and a grub entry for it",
    _M _OUT,    "local name for the output object",
    _M _OUTF,   "path on the file system in the output object",
    _PART,      "mount partition of a disk file",
    _RAMDISK,   "add this initrd and a grub entry for it",
    _TUNE,      "invoke tune2fs on the file system? (default=no)",
    _WORK,      "create a work copy? (default=yes)",
    NULL
};

typedef struct _inject_params {
    boolean cache;
    char * in;
    char * infile;
    char * kernel;
    char * out;
    char * outfile;
    int part;
    char * ramdisk;
    boolean tune;
    boolean work;
    int num_ext_inputs;
} inject_params;

char ** inject_parameters ()
{
    return params;
}

int inject_validate (imager_request * req)
{
    print_req (req);

    inject_params * state = calloc (sizeof (inject_params), 1);
    if (state==NULL)
        err ("out of memory");

    // default values
    state->cache = FALSE;
    state->tune = FALSE;
    state->work = TRUE;
    state->part = -1; // means the whole input object is a partition, not a disk

    // record in 'state' all specified parameters
    for (imager_param * p = req->params; p!=NULL && p->key!=NULL; p++) {
        if      (strcmp (p->key, _CACHE)==0)   {state->cache    = parse_boolean (p->val); }
        else if (strcmp (p->key, _IN)==0)      {state->in       = p->val; }
        else if (strcmp (p->key, _INF)==0)     {state->infile   = p->val; }
        else if (strcmp (p->key, _KERNEL)==0)  {state->kernel   = p->val; }
        else if (strcmp (p->key, _OUT)==0)     {state->out      = p->val; }
        else if (strcmp (p->key, _OUTF)==0)    {state->outfile  = p->val; }
        else if (strcmp (p->key, _PART)==0)    {state->part     = atoi(p->val); }
        else if (strcmp (p->key, _RAMDISK)==0) {state->ramdisk  = p->val; }
        else if (strcmp (p->key, _TUNE)==0)    {state->tune     = parse_boolean (p->val); }
        else if (strcmp (p->key, _WORK)==0)    {state->work     = parse_boolean (p->val); }
        else
            err ("invalid parameter '%s' for command 'inject'", p->key);
    }

    // ensure mandatory params are present
    if (state->in==NULL) err ("missing mandatory parameter '" _IN "'");
    if (state->infile==NULL) err ("missing mandatory parameter '" _INF "'");
    if (state->out==NULL) err ("missing mandatory parameter '" _OUT "'");
    if (state->outfile==NULL) err ("missing mandatory parameter '" _OUTF "'");

    // ensure paired arguments are both present
    if (state->ramdisk!=NULL && state->kernel==NULL)
        err ("'ramdisk' must be specified with 'kernel'");
    if (state->kernel!=NULL && state->part==-1)
        err ("'kernel' can only be injected in a partition (need 'part')");

    // make sure the input file is there
    if (check_file (state->infile)!=OK) {
        logprintfl (EUCAERROR, "input file '%s' not found\n", state->infile);
        free (state);
        return ERROR;
    }

    req->internal = (void *) state; // save pointer to find it later

    return 0;
}

int inject_requirements (imager_request * req)
{
    inject_params * state = (inject_params *) req->internal;
    artifacts_spec * input_spec = NULL;
    long long input_size;
    state->num_ext_inputs = 0;

    input_spec = map_get (get_artifacts_map(), state->in);
    if (input_spec==NULL) { // no artifact from an earlier stage
        input_size = file_size (state->in); // file size won't change in conversion
    } else {
        input_size = input_spec->size; // file size won't change in conversion
    }
    if (input_size<0) {
        logprintfl (EUCAINFO, "failed to locate required input '%s'\n", state->in);
        return ERROR;
    }

    artifacts_spec * kernel_spec = NULL;
    artifacts_spec * ramdisk_spec = NULL;
    if (state->kernel) {
        long long size;

        kernel_spec = map_get (get_artifacts_map(), state->kernel);
        if (kernel_spec==NULL) { // no artifact from an earlier stage
            size = file_size (state->kernel); // file size won't change in conversion
        } else {
            size = kernel_spec->size; // file size won't change in conversion
        }
        if (size<0) {
            logprintfl (EUCAINFO, "failed to locate required input '%s'\n", state->kernel);
            return ERROR;
        }

        if (state->ramdisk) {
            ramdisk_spec = map_get (get_artifacts_map(), state->ramdisk);
            if (ramdisk_spec==NULL) { // no artifact from an earlier stage
                size = file_size (state->kernel); // file size won't change in conversion
            } else {
                size = ramdisk_spec->size; // file size won't change in conversion
            }
            if (size<0) {
                logprintfl (EUCAINFO, "failed to locate required input '%s'\n", state->ramdisk);
                return ERROR;
            }
        }
    }

    char size_str [30];
    snprintf (size_str, sizeof (size_str), "%lld", input_size);
    char * attrs [] = { // TODO: ideally use more than size to characterize an object
        "size", size_str,
        "updated", state->outfile,
        NULL
    };

    artifacts_spec * output_spec = alloc_artifacts_spec (req, attrs);
    if (output_spec==NULL) {
        logprintfl (EUCAERROR, "error: out of memory for artifacts\n");
        return ERROR;
    }
    if (input_spec)   output_spec->deps[state->num_ext_inputs++] = input_spec; // point back to inputs we depend on, if any
    if (kernel_spec)  output_spec->deps[state->num_ext_inputs++] = kernel_spec;
    if (ramdisk_spec) output_spec->deps[state->num_ext_inputs++] = ramdisk_spec;

    // NOTE: if state->in == state->out, this may replace the previous
    // stage in the map with this one, but we'll still have a pointer
    // to the previous stage via deps[0], so we can invoke it if needed
    map_set (get_artifacts_map(), state->out, (void *)output_spec);

    return OK;
}

int inject_execute (imager_request * req)
{
    inject_params * state = (inject_params *) req->internal;
    char * id = state->out;
    boolean success = TRUE;
    int ret = OK;

    // look up the artifacts from the preceding requirements() invocation
    artifacts_spec * spec = map_get (get_artifacts_map(), id);
    artifacts_spec * prev_spec = map_get (get_artifacts_map(), state->in);
    if (spec==NULL) {
        logprintfl (EUCAERROR, "error: execute() called before requirements() for 'inject'\n");
        return ERROR;
    }

    output_file * o = preprocess_output_path (id, spec, state->work, state->cache, prev_spec);
    if (o==NULL) {
        free (spec); // TODO: free previous stage?
        return ERROR;
    }

    if (strlen (o->path)) { // valid output file does not exist
        if (state->num_ext_inputs > 0) {
            logprintfl (EUCAINFO, "stage '%s' has %d previous stage(s)\n", req->cmd->name, state->num_ext_inputs);
            for (int i=0; i<state->num_ext_inputs; i++) {

                // run the dependent stage(s) to bring in the input
                logprintfl (EUCAINFO, "stage '%s' invoking execution of stage '%s'...\n", req->cmd->name, spec->deps[i]->req->cmd->name);
                ret = spec->deps[i]->req->cmd->execute(spec->deps[i]->req);
                if (ret!=OK)
                    goto cleanup;
            }
        }

        // if not in-place injection, make a copy
        if (strcmp (state->in, state->out)!=0) {
            char path [EUCA_MAX_PATH];
            snprintf (path, sizeof (path), "%s/%s", get_work_dir(), state->in); // TODO: fix this to work with work and cache
            logprintfl (EUCAINFO, "copying '%s' to '%s'\n", path, o->path);
            if (copy_file (path, o->path)!=OK) {
                logprintfl (EUCAERROR, "failed to copy '%s' to '%s'\n", path, o->path);
                ret = ERROR;
                goto cleanup;
            }
        }

        diskfile * df = df_open (o->path);
        if (df==NULL) {
            logprintfl (EUCAERROR, "failed to open '%s'\n", o->path);
            ret = ERROR;
            goto cleanup;
        }
        if (df->nparts<1) {
            logprintfl (EUCAERROR, "no partitions found in disk '%s'\n", o->path);
            ret = ERROR;
            goto cleanup;
        }

        // mount the partition
        char mnt_pt [EUCA_MAX_PATH];
        snprintf (mnt_pt, EUCA_MAX_PATH, "%s/euca-mount-XXXXXX", get_work_dir());
        if (mkdtemp (mnt_pt)==NULL) {
            logprintfl (EUCAINFO, "error: mkdtemp() failed: %s\n", strerror(errno));
            ret = ERROR;
            goto cleanup;
        }
        char mnt_pt_outfile    [EUCA_MAX_PATH];
        char mnt_pt_outfiledir [EUCA_MAX_PATH];
        snprintf (mnt_pt_outfile,    EUCA_MAX_PATH, "%s/%s", mnt_pt, state->outfile);
        snprintf (mnt_pt_outfiledir, EUCA_MAX_PATH, "%s/%s", mnt_pt, state->outfile);
        for (int i=strlen(mnt_pt_outfiledir)-1; i>0; i--) { // remove file name from path
            if (mnt_pt_outfiledir[i]=='/') {
                mnt_pt_outfiledir[i] = '\0';
                break;
            }
        }
        if (df_mount (df, state->part, mnt_pt)!=OK) {
            logprintfl (EUCAINFO, "error: failed to mount '%s' on '%s'\n", df->path, mnt_pt);
            ret = ERROR;
            goto cleanup;
        }

        // do the single-file injection
        logprintfl (EUCAINFO, "injecting '%s' to '%s' on '%s'\n", state->infile, state->outfile, id);
        if (diskutil_mkdir (mnt_pt_outfiledir)!=OK) {
            logprintfl (EUCAINFO, "error: failed to create subdirectories for '%s'\n", mnt_pt_outfiledir);
            ret = ERROR;
            goto unmount;
        }

        if (diskutil_cp (state->infile, mnt_pt_outfile)!=OK) {
            logprintfl (EUCAINFO, "error: failed to copy '%s' to '%s'\n", state->infile, mnt_pt_outfile);
            ret = ERROR;
            goto unmount;
        }

        if (diskutil_ch (mnt_pt_outfile, "root", 0700)!=OK) { // TODO: don't hardcode perms and user
            logprintfl (EUCAINFO, "error: failed to change user and/or permissions for '%s'\n", mnt_pt_outfile);
            ret = ERROR;
            goto unmount;
        }

        // do the bootification, if requested
        if (state->kernel) {
            if (df->mbr == MBR_NONE) {
                logprintfl (EUCAERROR, "error: cannot make partition bootable\n");
                ret = ERROR;
                goto unmount;
            }
            char kpath [EUCA_MAX_PATH];
            if (state->kernel [0] == '/') {
                strncpy (kpath, state->kernel, sizeof (kpath));
            } else {
                snprintf (kpath, sizeof (kpath), "%s/%s", get_work_dir(), state->kernel); // TODO: fix this to work with work and cache
            }
            char rpath [EUCA_MAX_PATH];
            if (state->ramdisk [0] == '/') {
                strncpy (rpath, state->in, sizeof (rpath));
            } else {
                snprintf (rpath, sizeof (rpath), "%s/%s", get_work_dir(), state->ramdisk); // TODO: fix this to work with work and cache
            }
            logprintfl (EUCAINFO, "making partition %d bootable with kernel %s and ramdisk %s\n", state->part, state->kernel, state->ramdisk);
            if (diskutil_grub_files (mnt_pt, state->part, kpath, rpath)!=OK) {
                logprintfl (EUCAERROR, "error: failed to make partition %d of '%s' bootable\n", state->part, state->out);
                ret = ERROR;
                goto unmount;
            }
        }

    unmount:

        // unmount
        if (df_umount (df, state->part, TRUE)!=OK) {
            logprintfl (EUCAINFO, "error: failed to unmount %s (there may be a resource leak!)\n", mnt_pt_outfile);
            ret = ERROR;
        }
        if (rmdir (mnt_pt)!=0) {
            logprintfl (EUCAINFO, "error: failed to remove %s (there may be a resource leak!): %s\n", mnt_pt, strerror(errno));
            ret = ERROR;
        }

        // run grub over MBR
        if (ret == OK) {
            if (diskutil_grub_mbr (o->path, state->part)!=OK) {
                logprintfl (EUCAINFO, "error: failed to remove %s: %s\n", mnt_pt, strerror(errno));
                ret = ERROR;
            }
        }

    }

 cleanup:

    if (ret!=OK) {
        logprintfl (EUCAERROR, "error: failed to inject into '%s'\n", id);
        success = FALSE;
    }
    postprocess_output_path (o, success);
    // TODO: free spec

    return ret;
}

int inject_cleanup (imager_request * req, boolean last)
{
    inject_params * state = (inject_params *) req->internal;

    logprintfl (EUCAINFO, "cleaning up for '%s'...\n", req->cmd->name);
    if (!last)
        rm_workfile (state->out);
    free (state);

    return 0;
}

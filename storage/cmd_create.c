// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdio.h>
#include <stdlib.h>
#include <string.h> // strcmp, strstr
#include <unistd.h> // access
#include <assert.h> // duh
#include "eucalyptus.h"
#include "misc.h"
#include "imager.h"
#include "cache.h"
#include "map.h"
#include "img.h"
#include "diskfile.h"
#include "errno.h"

#define _M     "* " // mandatory
#define _CACHE "cache"
#define _FMT   "format"
#define _MBR   "mbr"
#define _OUT   "out"
#define _PCONT "p#-content"
#define _PFMT  "p#-format"
#define _PSIZE "p#-size"
#define _SIZE  "size"
#define _WORK  "work"

#define _MAX_PARTS 3 // maximum number of partitions supported by 'create'

static char * params [] = {
    _CACHE,     "cache the object? (default=no)",
    _FMT,       "format object as {ext3|swap} partition",
    _MBR,       "format object as disk with MBR of type {msdos}",
    _M _OUT,    "local name for the output object",
    _PCONT,     "copy this object to disk partition #",
    _PFMT,      "format disk partition # as {ext3|swap}",
    _PSIZE,     "size, in bytes, of disk partition #",
    _SIZE,      "size, in bytes of the object (i.e., disk/partition)",
    _WORK,      "create a work copy? (default=yes)",
    NULL
};

typedef struct _create_params {
    boolean cache;
    enum pformat_t format;
    enum mbr_t mbr;
    char * out;
    struct _part_type {
        char * content;
        enum pformat_t format;
        long long size;
    } parts [_MAX_PARTS];
    long long size;
    boolean work;
    boolean disk;
    int nparts;
    int num_ext_inputs;
} create_params;

#define PFORMAT_DEFAULT PFORMAT_EXT3 // when format isn't specified for content

static int verify_part (struct _part_type * p, const int n)
{
    if (p->content) { // partition with content to be copied into
        if (p->size > 0)
            err ("in partition %d, 'size' is not needed with 'content'", n);
        if (p->format == PFORMAT_UNKNOWN) {
            logprintfl (EUCAWARN, "warning: assuming '%s' as format for partition %d\n", enum_format_as_string (PFORMAT_DEFAULT), n);
            p->format = PFORMAT_DEFAULT;
        }
    } else if (p->format != PFORMAT_UNKNOWN) { // this partition is to be formatted
        if (p->size == 0)
            err ("in partition %d, 'size' is needed with 'format'", n);
        long long min = get_min_size (pformat_to_diskpart_t (p->format)); // argh, is type checking of enums too much to ask for?
        if (p->size > 0 && p->size < min)
            err ("in partition %d, of type '%s', 'size' must be at least %lld bytes", n, enum_format_as_string (p->format), min);
    } else if (p->size > 0) { // only have size, not format
        err ("in partition %d, 'format' is needed with 'size'", n);
    } else {
        return 0;
    }
    return 1;
}

char ** create_parameters ()
{
    return params;
}

// e.g., key="p3-content" subpar="-content"
static int parse_part (char * key, char * subpar)
{
    if (key [0] != 'p') return -1;
    char * hyphen = strstr (key, subpar);
    if (hyphen == NULL) return -1;
    int len = hyphen - key - 1;
    char buf [10];
    if (len < 1 || len > (sizeof(buf)-1)) return -1;
    memcpy (buf, key + 1, len);
    buf [len] = '\0';
    int part = -1;
    part = (int) strtol(buf, (char **)NULL, 10);
    if (part>=_MAX_PARTS || part == -1)
        err ("partition %d is outside of the range of supported values: [0-%d]", part, _MAX_PARTS-1);
    return part;
}

int create_validate (imager_request * req)
{
    print_req (req);

    create_params * state = calloc (sizeof (create_params), 1);
    if (state==NULL)
        err ("out of memory");

    // default values
    state->cache = FALSE;
    state->work = TRUE;

    // record in 'state' all specified parameters
    boolean have_partitions = FALSE;
    for (imager_param * p = req->params; p!=NULL && p->key!=NULL; p++) {
        if      (strcmp (p->key, _CACHE)==0) {state->cache    = parse_boolean (p->val); }
        else if (strcmp (p->key, _FMT)==0)   {state->format   = parse_pformat_t_enum (p->val); }
        else if (strcmp (p->key, _MBR)==0)   {state->mbr      = parse_mbr_t_enum (p->val); }
        else if (strcmp (p->key, _OUT)==0)   {state->out      = p->val; }
        else if (strcmp (p->key, _SIZE)==0)  {state->size     = parse_bytes (p->val); }
        else if (strcmp (p->key, _WORK)==0)  {state->work     = parse_boolean (p->val); }
        else {
            int part;

            if ((part = parse_part (p->key, "-content"))>=0) {
                state->parts [part].content = p->val;
                have_partitions = TRUE;
            } else if ((part = parse_part (p->key, "-format"))>=0) {
                state->parts [part].format = parse_pformat_t_enum (p->val);
                have_partitions = TRUE;
            } else if ((part = parse_part (p->key, "-size"))>=0) {
                long long size = parse_bytes (p->val);
                if (size<0) { // a negative size means "as big as will fit into overall size"
                    size = -1;
                } else if (size<5) { // TODO: what's a reasonable minimum for object?
                    err ("in partition %d, invalid size spec (%lld)", part, size);
                }
                state->parts [part].size = size;
                have_partitions = TRUE;
            } else {
                err ("invalid parameter '%s' for command 'create'", p->key);
            }
        }
    }

    // ensure mandatory params are present
    if (state->out==NULL)
        err ("missing mandatory parameter '" _OUT "'");
    if (state->format==PFORMAT_UNKNOWN && state->mbr==MBR_NONE)
        err ("either 'format' (for partition) or 'mbr' (for disk) must be specified");

    // if creating a partition
    if (state->format!=PFORMAT_UNKNOWN) {
        if (state->mbr!=MBR_NONE)
            err ("'format' (for partition) and 'mbr' (for disk) are mutually exclusive");
        if (have_partitions)
            err ("p#-* parameters are for use with disks, not with partitions");
        if (state->size<1)
            err ("'size' must be specified for the partition");

    } else { // if creating a disk
        if (have_partitions) {
            boolean unsized_part = FALSE;
            boolean no_prev = FALSE;
            for (int i=0; i<_MAX_PARTS; i++) { // verify all partition specs
                if (verify_part (&(state->parts [i]), i)) {
                    if (no_prev == TRUE)  // ensure they are contiguous
                        err ("partition %d follows unspecified partition %d", i, i-1);
                    if (state->parts[i].size < 0) {
                        if (unsized_part)
                            err ("only one unsized partition (size==-1) is allowed");
                        unsized_part = TRUE;
                        if (state->size < 1)
                            err ("overall disk size must be specified with an unsized partition");
                    }
                    state->nparts++;
                } else {
                    no_prev = TRUE;
                }
            }
        } else {
            if (state->size<1)
                err ("either give overall disk 'size' or the individual partitions");
        }
        state->disk = TRUE;
    }
    req->internal = (void *) state; // save pointer to find it later

    return 0;
}

int create_requirements (imager_request * req)
{
    create_params * state = (create_params *) req->internal;
    artifacts_spec * input_specs [_MAX_PARTS];
    artifacts_spec * output_spec;
    int num_ext_inputs = 0;
    long long size = 0;
    char hash [32];

    char * attrs [_MAX_PARTS*2+8+1];
    if (attrs==NULL) {
        logprintfl (EUCAERROR, "error: out of memory for attributes\n");
        return ERROR;
    }
#define _SET_ATTR(_s1,_s2) attrs [n++] = _s1; attrs [n++] = _s2
    int n = 0;
    _SET_ATTR("id", state->out);

    if (state->disk) { // creating a disk, which may have external inputs
        _SET_ATTR("type", "disk");
        long long known_size = 0L;
        int unsized_part = -1;

        // run through specified partitions, if any
        for (int i=0; i<state->nparts; i++) {
            struct _part_type * p = &(state->parts [i]);
            char * format;
            long long part_size;

            if (p->content) { // the partition will consist of external copied content
                format = p->content;
                artifacts_spec * input_spec = map_get (get_artifacts_map(), p->content);
                if (input_spec==NULL) { // no artifact from an earlier stage
                    part_size = file_size (p->content); // assume that file size won't change in conversion
                } else {
                    input_specs [num_ext_inputs++] = input_spec;
                    part_size = input_spec->size; // assume that file size won't change in conversion
                }
                if (part_size<0) {
                    logprintfl (EUCAINFO, "failed to locate required input '%s'\n", p->content);
                    return ERROR;
                }
                p->size = part_size; // record the size of the input file
            } else { // partition will be formatted
                format = enum_format_as_string (p->format);
                part_size = p->size;
            }
            _SET_ATTR("includes-partition", format);
            if (part_size > 0) {
                known_size += round_up_sec (part_size);
            } else {
                if (unsized_part>=0)
                    err ("error: internal: multiple unsized partitions in create_requirements()\n");
                unsized_part = i;
            }
        }

        if (unsized_part>=0) {
            if (state->size < 1)
                err ("error: internal: unsized partition and no overall size in create_requirements()\n");
            long long min = get_min_size (pformat_to_diskpart_t (state->parts [unsized_part].format));
            size = round_down_sec (state->size);
            if ((size - known_size) < min) {
                logprintfl (EUCAERROR, "error: insufficient space for partition %d given disk limit %lld and sum of other partitions %lld\n", unsized_part, size, known_size);
                return ERROR;
            }
            state->parts [unsized_part].size = size - known_size;
            logprintfl (EUCAINFO, "calculated size %lld for partition %d\n", state->parts [unsized_part].size, unsized_part);
        } else {
            size = known_size;
            if (state->size > 0) {
                long long req_size = round_down_sec (state->size);
                if (size == 0) {
                    size = req_size; // no partitions, use disk size
                } else {
                    if (req_size != size) {
                        logprintfl (EUCAWARN, "warning: ignoring requested disk size (%lld) in favor of the sum of partitions (%lld)\n", req_size, size);
                    }
                }
            }
        }

    } else { // creating a partition doesn't take external inputs
        _SET_ATTR("type", "partition");
        _SET_ATTR("format", enum_format_as_string (state->format));
        size = round_up_sec (state->size);
    }

    // increase the size required to accommodate MBR, etc
    size += mbr_size_bytes ();

    char size_str [30];
    snprintf (size_str, sizeof (size_str), "%lld", size);
    _SET_ATTR("size", size_str);
    //_SET_ATTR("hash", hash); // TODO: hash
    assert (sizeof(attrs)/sizeof(char *)>(n-1));
    attrs [n++] = NULL;

    output_spec = alloc_artifacts_spec (req, attrs);
    if (output_spec==NULL) {
        logprintfl (EUCAERROR, "error: out of memory for artifacts\n");
        return ERROR;
    }

    for (int i=0; i<num_ext_inputs; i++) {
        output_spec->deps [i] = input_specs [i]; // point back to inputs we depend on
    }

    state->num_ext_inputs = num_ext_inputs;
    output_spec->size = round_up_sec (size);
    map_set (get_artifacts_map(), state->out, (void *)output_spec);

    return OK;
}

int create_execute (imager_request * req)
{
    create_params * state = (create_params *) req->internal;
    char * id = state->out;
    boolean success = TRUE;
    int ret = OK;

    // look up the artifacts from the preceding requirements() invocation
    artifacts_spec * spec = map_get (get_artifacts_map(), id);
    if (spec==NULL) {
        logprintfl (EUCAERROR, "error: execute() called before requirements() for 'create'\n");
        return ERROR;
    }

    output_file * o = preprocess_output_path (id, spec, state->work, state->cache, NULL);
    if (o==NULL) {
        free_artifacts_spec (spec);
        return ERROR;
    }

    if (strlen (o->path)) { // valid output file does not exist

        // if expecting local input files
        if (state->num_ext_inputs > 0) {
            for (int i=0; i<state->num_ext_inputs; i++) {

                // run the dependent stage(s) to bring in the input
                logprintfl (EUCAINFO, "stage '%s' executing previous %d stage(s)...\n", req->cmd->name, state->num_ext_inputs);
                ret = spec->deps[i]->req->cmd->execute(spec->deps[i]->req);
                if (ret!=OK)
                    goto cleanup;
            }
        }

        // create the output file
        logprintfl (EUCAINFO, "creating '%s'...\n", id);
        diskfile * df = df_create (o->path, spec->size, FALSE);
        if (df==NULL) {
            ret = ERROR;
            goto close;
        }

        if (state->disk) {
            // build up a list of partitions
            diskpart * dp = calloc (state->nparts + 1, sizeof (diskpart));
            if (dp==NULL) {
                logprintfl (EUCAERROR, "out of memory in execute()\n");
                ret = ERROR;
                goto close;
            }
            for (int i=0; i<state->nparts; i++) {
                struct _part_type * p = &(state->parts [i]);
                dp [i].size_bytes = p->size;
                dp [i].type = pformat_to_diskpart_t (p->format);
            }

#define     _CALL_CHECK(f) if (f!=OK) {         \
                ret = ERROR;                    \
                goto close;                     \
            }

            // create the partitions
            _CALL_CHECK(df_partition (df, MBR_MSDOS, dp));

            // either format or populate the partition
            for (int i=0; i<state->nparts; i++) {
                struct _part_type * p = &(state->parts [i]);
                if (p->content) {
                    char path [EUCA_MAX_PATH];
                    if (p->content [0] == '/') {
                        strncpy (path, p->content, sizeof (path));
                    } else {
                        snprintf (path, sizeof (path), "%s/%s", get_work_dir(), p->content); // TODO: fix this to work with work and cache
                    }
                    _CALL_CHECK(df_dd (df, i, path));
                } else {
                    _CALL_CHECK(df_format (df, i, p->format));
                }
            }
        } else { // a partition => format it
            _CALL_CHECK(df_format (df, 0, state->format));
        }

    close:

        df_close (df);
    }

 cleanup:

    if (ret!=OK) {
        logprintfl (EUCAERROR, "error: failed to create '%s'\n", id);
        success = FALSE;
    }
    postprocess_output_path (o, success);
    // TODO: free spec

    return ret;
}

int create_cleanup (imager_request * req, boolean last)
{
    create_params * state = (create_params *) req->internal;

    logprintfl (EUCAINFO, "cleaning up for '%s'...\n", req->cmd->name);
    if (!last)
        rm_workfile (state->out);
    free (state);

    return 0;
}

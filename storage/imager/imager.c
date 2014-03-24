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
//! @file storage/imager/imager.c
//! Need Description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>
#include <unistd.h>                    // getopt
#include <fcntl.h>                     // open
#include <ctype.h>                     // tolower
#include <errno.h>                     // errno
#include <sys/vfs.h>                   // statfs

#include <eucalyptus.h>
#include <euca_auth.h>
#include <misc.h>
#include <map.h>

#include "vmdk.h" // vmdk_init()
#include "imager.h"
#include "cmd.h"
#include "cache.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAX_REQS                                 32
#define MAX_PARAMS                               32

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

boolean vddk_available = FALSE;

imager_command known_cmds[EUCA_NB_IMAGER_CMD] = {
    {"fsck", fsck_parameters, fsck_validate, fsck_requirements, NULL},
    {"prepare", prepare_parameters, prepare_validate, prepare_requirements, prepare_cleanup},
    {"convert", convert_parameters, convert_validate, convert_requirements, convert_cleanup},
    {"upload", upload_parameters, upload_validate, upload_requirements, upload_cleanup},
    {"bundle", bundle_parameters, bundle_validate, bundle_requirements, bundle_cleanup},
    {"extract", extract_parameters, extract_validate, extract_requirements, extract_cleanup},
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static imager_request reqs[MAX_REQS] = { {0} };

static char *euca_home = NULL;
static map *artifacts_map = NULL;
static boolean print_debug = FALSE;
static boolean print_argv = FALSE;
static boolean purge_cache = FALSE; // whether to clean out cache after work

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void bs_errors(const char *msg);
static void set_debug(boolean yes);
static void usage(const char *msg) __attribute__ ((__noreturn__));
static imager_command *find_struct(char *name);
static imager_command *validate_cmd(int index, char *this_cmd, imager_param * params, char *next_cmd);
static void set_global_parameter(char *key, char *val);
static int stale_blob_examiner(const blockblob * bb);
static int stat_blobstore(const char *path, blobstore * bs);

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
//! @param[in] msg
//!
//! @pre
//!
//! @post
//!
static void bs_errors(const char *msg)
{
    // we normally do not care to print all messages from blobstore as many are errors that we can handle
    LOGEXTREME("blobstore: %s", msg);
}

//!
//!
//!
//! @param[in] yes
//!
static void set_debug(boolean yes)
{
    // so euca libs will log to stdout
    if (yes == TRUE) {
        logfile(NULL, EUCA_LOG_DEBUG, 0);
    } else {
        logfile(NULL, EUCA_LOG_WARN, 0);
    }
}

//!
//!
//!
//! @param[in] msg
//!
//! @return the return value description
//!
//! @see euca_strncpy(), euca_strdupcat()
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note Important note
//!
static void usage(const char *msg)
{
    if (msg != NULL)
        fprintf(stderr, "error: %s\n\n", msg);

    fprintf(stderr, "Usage: euca-imager [command param=value param2=value ...] [command2 ...]\n");

    if (msg == NULL)
        fprintf(stderr, "Try 'euca-imager help' for list of commands\n");

    exit(1);
}

//!
//!
//!
//! @param[in] format
//! @param[in] ...
//!
//! @pre
//!
//! @post
//!
void err(const char *format, ...)
{
    char buf[1024] = "";
    va_list ap = { {0} };

    va_start(ap, format);
    vsnprintf(buf, sizeof(buf), format, ap);
    va_end(ap);

    LOGERROR("%s\n", buf);
    LOGINFO("imager done (exit code=1)\n");
    exit(1);
}

//!
//! Finds a command by name among known_cmds[] defined in cmd.h
//!
//! @param[in] name
//!
//! @return A pointer to the matching command structure if found. Otherwise
//!         NULL is returned.
//!
//! @pre
//!
//! @post
//!
static imager_command *find_struct(char *name)
{
    int i = 0;

    for (i = 0; i < (sizeof(known_cmds) / sizeof(imager_command)); i++) {
        if (strcmp(name, known_cmds[i].name) == 0) {
            return &known_cmds[i];
        }
    }
    return NULL;
}

//!
//! Either prints help messages or runs command-specific validator
//!
//! @param[in] index
//! @param[in] this_cmd
//! @param[in] params
//! @param[in] next_cmd
//!
//! @return A pointer to the command structure if valid otherwise NULL is returned
//!
//! @pre
//!
//! @post
//!
static imager_command *validate_cmd(int index, char *this_cmd, imager_param * params, char *next_cmd)
{
    int i = 0;
    char help = '\0';
    char *cmd = NULL;
    const char **p = NULL;
    imager_command *cmd_struct = NULL;
    imager_request *req = NULL;

    if (this_cmd == NULL)
        return NULL;

    cmd = this_cmd;

    // see if it is a help request
    if (strcmp(cmd, "help") == 0) {
        help = 1;
        if (next_cmd == NULL) {        // not specific
            fprintf(stderr, "supported commands:\n");
            for (i = 0; i < (sizeof(known_cmds) / sizeof(imager_command)); i++) {
                fprintf(stderr, "\t%s\n", known_cmds[i].name);
            }
            exit(0);
        } else {                       // help for a specific command
            cmd = next_cmd;
        }
    }
    // find the function pointers for the command
    if ((cmd_struct = find_struct(cmd)) == NULL)
        err("command '%s' not found", cmd);

    // print command-specific help
    if (help) {
        p = cmd_struct->parameters();
        if (p == NULL || *p == NULL) {
            fprintf(stderr, "command '%s' has no parameters\n", cmd_struct->name);
        } else {
            fprintf(stderr, "parameters for '%s' (* - mandatory):\n", cmd_struct->name);
            while (*p && *(p + 1)) {
                fprintf(stderr, "%18s - %s\n", *p, *(p + 1));
                p += 2;
            }
        }
        exit(0);
    }
    // fill out the request struct and pass to the validator
    req = &reqs[index];
    req->cmd = cmd_struct;
    req->params = params;
    req->index = index;
    req->internal = NULL;
    if (cmd_struct->validate(req))
        err("incorrect parameters or missing dependencies for command '%s'", cmd_struct->name);

    return cmd_struct;
}

//!
//!
//!
//! @param[in] key
//! @param[in] val
//!
//! @pre
//!
//! @post
//!
static void set_global_parameter(char *key, char *val)
{
    if (strcmp(key, "debug") == 0) {
        print_debug = parse_boolean(val);
        set_debug(print_debug);
    } else if (strcmp(key, "argv") == 0) {
        print_argv = parse_boolean(val);
    } else if (strcmp(key, "work") == 0) {
        set_work_dir(val);
    } else if (strcmp(key, "work_size") == 0) {
        set_work_limit(parse_bytes(val));
    } else if (strcmp(key, "cache") == 0) {
        set_cache_dir(val);
    } else if (strcmp(key, "cache_size") == 0) {
        set_cache_limit(parse_bytes(val));
    } else if (strcmp(key, "purge_cache") == 0) {
        purge_cache = parse_boolean(val);
    } else {
        err("unknown global parameter '%s'", key);
    }
    LOGINFO("GLOBAL: %s=%s\n", key, val);
}

//!
//!
//!
//! @param[in] bb
//!
//! @return Always returns 1 for now
//!
static int stale_blob_examiner(const blockblob * bb)
{
    return 1;                          // delete all work blobs during fsck
}

//!
//!
//!
//! @param[in] path
//! @param[in] bs
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
static int stat_blobstore(const char *path, blobstore * bs)
{
    s64 size_mb = 0;
    s64 allocated_mb = 0;
    s64 reserved_mb = 0;
    s64 locked_mb = 0;
    s64 fs_size_mb = 0;
    struct statfs fs = { 0 };
    blobstore_meta meta = { {0} };

    blobstore_stat(bs, &meta);
    size_mb = meta.blocks_limit ? (meta.blocks_limit / 2048) : (-1L);   // convert sectors->MB
    allocated_mb = meta.blocks_limit ? (meta.blocks_allocated / 2048) : 0;
    reserved_mb = meta.blocks_limit ? ((meta.blocks_locked + meta.blocks_unlocked) / 2048) : 0;
    locked_mb = meta.blocks_limit ? (meta.blocks_locked / 2048) : (-1L);

    if (statfs(path, &fs) == -1) {
        LOGERROR("failed to stat %s: %s\n", path, strerror(errno));
        return EUCA_ERROR;
    }

    fs_size_mb = ((s64) fs.f_bsize) * ((s64) (fs.f_blocks / MEGABYTE));

    LOGINFO("disk space under %s\n", path);
    LOGINFO("                 %06ldMB limit (%.1f%% of the file system)\n", size_mb, ((double)size_mb / (double)fs_size_mb) * 100.0);
    LOGINFO("                 %06ldMB reserved for use (%.1f%% of limit)\n", reserved_mb, ((double)reserved_mb / (double)size_mb) * 100.0);
    LOGINFO("                 %06ldMB locked for use (%.1f%% of limit)\n", locked_mb, ((double)locked_mb / (double)size_mb) * 100.0);
    LOGINFO("                 %06ldMB allocated for use (%.1f%% of limit, %.1f%% of the file system)\n",
            allocated_mb, ((double)allocated_mb / (double)size_mb) * 100.0, ((double)allocated_mb / (double)fs_size_mb) * 100.0);
    return EUCA_OK;
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char *argv[])
{
    int i = 0;
    int ret = EUCA_OK;
    int nparams = 0;
    int ncmds = 0;
    char *eq = NULL;
    char *key = NULL;
    char *val = NULL;
    char euca_root[] = "";
    char argv_str[4096] = "";
    char *cmd_name = NULL;
    char pid_file[EUCA_MAX_PATH] = "";
    FILE *fp = NULL;
    pid_t pid = 0;
    artifact *root = NULL;
    blobstore *work_bs = NULL;
    blobstore *cache_bs = NULL;
    imager_param *cmd_params = NULL;

    log_fp_set(stderr); // imager logs to stderr so image data can be piped to stdout
    set_debug(print_debug);

    // initialize globals
    artifacts_map = map_create(10);

    // use $EUCALYPTUS env var if available
    euca_home = getenv(EUCALYPTUS_ENV_VAR_NAME);
    if (!euca_home) {
        euca_home = euca_root;
    }
    // save the command line into a buffer so it's easier to rerun it by hand
    argv_str[0] = '\0';
    for (i = 0; i < argc; i++) {
        strncat(argv_str, "\"", sizeof(argv_str) - strlen(argv_str) - 1);
        strncat(argv_str, argv[i], sizeof(argv_str) - strlen(argv_str) - 1);
        strncat(argv_str, "\" ", sizeof(argv_str) - strlen(argv_str) - 1);
    }

    // initialize dependencies
    if (vmdk_init() == EUCA_OK) {
        vddk_available = TRUE;
    }

    // parse command-line parameters
    while (*(++argv)) {
        eq = strstr(*argv, "=");       // all params have '='s
        if (eq == NULL) {              // it's a command
            // process previous command, if any
            if (validate_cmd(ncmds, cmd_name, cmd_params, *argv) != NULL)
                ncmds++;               // increment only if there was a previous command

            if (ncmds + 1 > MAX_REQS)
                err("too many commands (max is %d)", MAX_REQS);

            cmd_name = *argv;
            cmd_params = NULL;
            nparams = 0;
        } else {                       // this is a parameter
            if (strlen(eq) == 1)
                usage("parameters must have non-empty values");
            *eq = '\0';                // split key from value
            if (strlen(*argv) == 1)
                usage("parameters must have non-empty names");

            key = *argv;
            val = eq + 1;
            if (key == NULL || val == NULL)
                usage("syntax error in parameters");

            if (key[0] == '-')
                key++;                 // skip '-' if any

            if (key[0] == '-')
                key++;                 // skip second '-' if any

            if (cmd_name == NULL) {    // without a preceding command => global parameter
                set_global_parameter(key, val);
                continue;
            }

            if (cmd_params == NULL) {
                cmd_params = calloc(MAX_PARAMS + 1, sizeof(imager_param));  // +1 for terminating NULL
                if (!cmd_params)
                    err("calloc failed");
            }

            if (nparams + 1 > MAX_PARAMS)
                err("too many parameters (max is %d)", MAX_PARAMS);
            cmd_params[nparams].key = key;
            cmd_params[nparams].val = val;
            nparams++;
        }
    }

    if (validate_cmd(ncmds, cmd_name, cmd_params, *argv) != NULL)   // validate last command
        ncmds++;

    LOGINFO("verified all parameters for %d command(s)\n", ncmds);
    if (print_argv) {
        LOGDEBUG("argv[]: %s\n", argv_str);
    }
    // record PID, which may be used by VB to kill the imager process (e.g., in cancelBundling)
    pid = getpid();
    sprintf(pid_file, "%s/imager.pid", get_work_dir());
    if ((fp = fopen(pid_file, "w")) == NULL) {
        err("could not create pid file");
    } else {
        fprintf(fp, "%d", pid);
        fclose(fp);
    }

    // invoke the requirements checkers in the same order as on command line,
    // constructing the artifact tree originating at 'root'
    for (i = 0; i < ncmds; i++) {
        if (reqs[i].cmd->requirements != NULL) {
            art_set_instanceId(reqs[i].cmd->name);  // for logging
            if ((root = reqs[i].cmd->requirements(&reqs[i], root)) == NULL) // pass results of earlier checkers to later checkers
                err("failed while verifying requirements");
        }
    }

    // it is OK for root to be NULL at this point

    // see if work blobstore will be needed at any stage
    // and open or create the work blobstore
    if (root && tree_uses_blobstore(root)) {
        // set the function that will catch blobstore errors
        blobstore_set_error_function(&bs_errors);

        if (ensure_directories_exist(get_work_dir(), 0, NULL, NULL, BLOBSTORE_DIRECTORY_PERM) == -1)
            err("failed to open or create work directory %s", get_work_dir());

        work_bs = blobstore_open(get_work_dir(), get_work_limit() / 512, BLOBSTORE_FLAG_CREAT, BLOBSTORE_FORMAT_FILES, BLOBSTORE_REVOCATION_NONE, BLOBSTORE_SNAPSHOT_ANY);
        if (work_bs == NULL) {
            err("failed to open work blobstore: %s", blobstore_get_error_str(blobstore_get_error()));
        }
        // no point in fscking the work blobstore as it was just created
    }
    // see if cache blobstore will be needed at any stage
    if (root && tree_uses_cache(root)) {
        if (ensure_directories_exist(get_cache_dir(), 0, NULL, NULL, BLOBSTORE_DIRECTORY_PERM) == -1)
            err("failed to open or create cache directory %s", get_cache_dir());
        cache_bs = blobstore_open(get_cache_dir(), get_cache_limit() / 512, BLOBSTORE_FLAG_CREAT, BLOBSTORE_FORMAT_DIRECTORY, BLOBSTORE_REVOCATION_LRU, BLOBSTORE_SNAPSHOT_ANY);
        if (cache_bs == NULL) {
            blobstore_close(work_bs);
            err("failed to open cache blobstore: %s\n", blobstore_get_error_str(blobstore_get_error()));
        }

        if (blobstore_fsck(cache_bs, NULL)) //! @TODO: verify checksums?
            err("cache blobstore failed integrity check: %s", blobstore_get_error_str(blobstore_get_error()));

        if (stat_blobstore(get_cache_dir(), cache_bs))
            err("blobstore is unreadable");
    }
    // implement the artifact tree
    ret = EUCA_OK;
    if (root) {
        art_set_instanceId("imager");  // for logging
        ret = art_implement_tree(root, work_bs, cache_bs, NULL, INSTANCE_PREP_TIMEOUT_USEC);    // do all the work!
    }
    // invoke the cleaners for each command to tidy up disk space and memory allocations
    for (i = 0; i < ncmds; i++) {
        if (reqs[i].cmd->cleanup != NULL) {
            art_set_instanceId(reqs[i].cmd->name);  // for logging
            reqs[i].cmd->cleanup(&reqs[i], (i == (ncmds - 1)) ? (TRUE) : (FALSE));
        }
    }

    // free the artifact tree
    if (root) {
        if (tree_uses_blobstore(root)) {
            if (blobstore_fsck(work_bs, stale_blob_examiner)) { // will remove all blobs
                LOGWARN("failed to clean up work space: %s\n", blobstore_get_error_str(blobstore_get_error()));
            }
        }
        art_free(root);
    }

    LOGINFO("cleaning the work directory...\n");
    if (clean_work_dir(work_bs) != EUCA_OK) {
        LOGWARN("failed to clean up work blobstore\n");
    }
    if (purge_cache) {
        LOGINFO("purging the cache...\n");
        if (clean_cache_dir(cache_bs) != EUCA_OK) {
            LOGWARN("failed to purge cache blobstore\n");
        }
    }

    // indicate completion
    LOGINFO("imager done (exit code=%d)\n", ret);

    exit(ret);
}

//!
//! Common functions used by commands
//!
//! @param[in] req
//!
//! @pre
//!
//! @post
//!
void print_req(imager_request * req)
{
    imager_param *p = NULL;

    LOGINFO("command: %s\n", req->cmd->name);
    for (p = req->params; ((p != NULL) && (p->key != NULL)); p++) {
        LOGINFO("\t%s=%s\n", p->key, p->val);
    }
}

//!
//! Read in login or password from command line or from a file
//!
//! @param[in] s
//!
//! @return
//!
//! @pre
//!
//! @post
//!
//! @note Caller is responsible to free the returned value
//!
char *parse_loginpassword(const char *s)
{
    char *val = strdup(s);
    FILE *fp = NULL;

    if ((fp = fopen(s, "r")) != NULL) {
        EUCA_FREE(val);

        if ((val = fp2str(fp)) == NULL) {
            err("failed to read file '%s'", s);
        } else {
            LOGINFO("read in contents from '%s'\n", s);
        }
        fclose(fp);
    }

    return val;
}

//!
//! Parse a string into bytes, a la 'dd':
//!
//! If the number ends with a ``b'', ``k'', ``m'', ``g'',
//! or ``w'', the number is multiplied by 512, 1024 (1K), 1048576 (1M),
//! 1073741824 (1G) or the number of bytes in an integer, respectively.
//!
//! @param[in] s
//!
//! @return
//!
//! @pre
//!
//! @post
//!
s64 parse_bytes(const char *s)
{
    s64 result = 0;
    s64 multiplier = 1L;
    char *suffix = NULL;

    errno = 0;
    result = strtoull(s, &suffix, 0);
    if (errno != 0) {
        err("empty or invalid size specification '%s'\n", s);
    }

    if (*suffix != '\0') {
        switch (toupper(*suffix)) {
        case 'B':
            multiplier = 512L;
            break;                     // blocks
        case 'K':
            multiplier = 1024L;
            break;                     // kilo
        case 'M':
            multiplier = 1048576L;
            break;                     // mega
        case 'G':
            multiplier = 1073741824L;
            break;                     // giga
        case 'W':
            multiplier = sizeof(int);
            break;                     // word
        default:
            err("unrecognized suffix '%s' in size specification\n", suffix);
            break;
        }

        if (*(suffix + 1) != '\0') {
            err("suffix '%s' in size specification is longer than one letter\n", suffix);
        }
    }
    return result * multiplier;
}

//!
//! Make sure the path exists and is readable
//!
//! @param[in] path
//!
//! @return Always returns EUCA_OK for now. On failure the call to err() will
//!         terminate the application.
//!
//! @see err()
//!
//! @pre
//!
//! @post
//!
int verify_readability(const char *path)
{
    if (fopen(path, "r") == NULL)
        err("unable to read '%s'", path);
    return EUCA_OK;
}

//!
//! Return eucalyptus root
//!
//! @return The eucalyptus root
//!
char *get_euca_home(void)
{
    return euca_home;
}

//!
//! Return global artifacts map
//!
//! @return The global artifacts map
//!
map *get_artifacts_map(void)
{
    return artifacts_map;
}

//!
//! If path=A/B/C but only A exists, this will try to create B and C
//!
//! @param[in] path
//! @param[in] mode
//!
//! @return 0 on success or errno value on failure
//!
//! @pre
//!
//! @post
//!
int ensure_path_exists(const char *path, mode_t mode)
{
    int i = 0;
    int len = strlen(path);
    boolean try_it = FALSE;
    char *path_copy = strdup(path);
    struct stat buf = { 0 };

    if (path_copy == NULL)
        return errno;

    for (i = 0; i < len; i++) {
        try_it = FALSE;
        if (path[i] == '/' && i > 0) {
            path_copy[i] = '\0';
            try_it = TRUE;
        } else if (path[i] != '/' && i + 1 == len) {    // last one
            try_it = TRUE;
        }

        if (try_it) {
            if (stat(path_copy, &buf) == -1) {
                LOGINFO("creating path %s\n", path_copy);

                if (mkdir(path_copy, mode) == -1) {
                    LOGERROR("failed to create path %s: %s\n", path_copy, strerror(errno));
                    EUCA_FREE(path_copy);
                    return errno;
                }
            }
            path_copy[i] = '/';        // restore the slash
        }
    }

    EUCA_FREE(path_copy);
    return (0);
}

//!
//! If path=A/B/C but only A exists, this will try to create B, but not C
//!
//! @param[in] path
//! @param[in] mode
//!
//! @return 0 on success or errno value on failure
//!
//! @pre
//!
//! @post
//!
int ensure_dir_exists(const char *path, mode_t mode)
{
    int i = 0;
    int err = 0;
    int len = strlen(path);
    char *path_copy = strdup(path);

    if (path_copy == NULL)
        return errno;

    for (i = len - 1; i > 0; i--) {
        if (path[i] == '/') {
            path_copy[i] = '\0';
            err = ensure_path_exists(path_copy, mode);
            break;
        }
    }

    EUCA_FREE(path_copy);
    return err;
}

//!
//! Function for bypassing sentinel artifacts in a tree
//!
//! @param[in] root
//!
//! @return
//!
//! @pre
//!
//! @post
//!
artifact *skip_sentinels(artifact * root)
{
    artifact *ret = root;
    artifact *next_ret = NULL;

    while (ret) {
        if (ret->creator != NULL)
            break;
        // has a creator => not a sentinel
        if (ret->deps[1] != NULL)
            break;                     // has multiple children => do not skip

        next_ret = ret->deps[0];
        EUCA_FREE(ret);
        ret = next_ret;
    }
    return ret;
}

// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>
#include <unistd.h> // getopt
#include <fcntl.h> // open
#include <ctype.h> // tolower
#include <errno.h> // errno
#include <sys/vfs.h> // statfs

#include "euca_auth.h"
#include "eucalyptus.h"
#include "misc.h"
#include "imager.h"
#include "cmd.h"
#include "map.h"
#include "cache.h"

extern char ** environ;

#define MAX_REQS 32
#define MAX_PARAMS 32

static imager_request reqs [MAX_REQS];
static char * euca_home = NULL;
static map * artifacts_map;
static boolean print_debug = FALSE;
static boolean print_argv = FALSE;

static void bs_errors (const char * msg) { 
    // we normally do not care to print all messages from blobstore as many are errors that we can handle
    logprintfl (EUCADEBUG2, "{%u} blobstore: %s", (unsigned int)pthread_self(), msg);
} 

static void set_debug (boolean yes)
{
    // so euca libs will log to stdout
    if (yes==TRUE) {
        logfile (NULL, EUCADEBUG, 4);
    } else {
        logfile (NULL, EUCAWARN, 4);
    }
}

static void usage (const char * msg)
{
    if (msg!=NULL)
        fprintf (stderr, "error: %s\n\n", msg);

    fprintf (stderr, "Usage: euca_imager [command param=value param2=value ...] [command2 ...]\n");

    if (msg==NULL)
        fprintf (stderr, "Try 'euca_imager help' for list of commands\n");

    exit (1);
}

void err (const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    char buf [1024];
    vsnprintf (buf, sizeof (buf), format, ap);
    va_end(ap);

    logprintfl (EUCAERROR, "%s\n", buf);
    logprintfl (EUCAINFO,  "imager done (exit code=1)\n");
    exit (1);
}

// finds a command by name among known_cmds[] defined in cmd.h
static imager_command * find_struct (char * name)
{
    for (int i=0; i<(sizeof(known_cmds)/sizeof(imager_command)); i++) {
        if (strcmp(name, known_cmds[i].name)==0) {
            return &known_cmds[i];
        }
    }
    return NULL;
}

// either prints help messages or runs command-specific validator
static imager_command * validate_cmd (int index, char *this_cmd, imager_param *params, char *next_cmd)
{
    if (this_cmd==NULL)
        return NULL;

    char * cmd = this_cmd;
    char help = 0;

    // see if it is a help request
    if (strcmp(cmd, "help")==0) {
        help = 1;
        if (next_cmd==NULL) { // not specific
            fprintf (stderr, "supported commands:\n");
            for (int i=0; i<(sizeof(known_cmds)/sizeof(imager_command)); i++) {
                fprintf (stderr, "\t%s\n", known_cmds[i].name);
            }
            exit (0);
        } else { // help for a specific command
            cmd = next_cmd;
        }
    }

    // find the function pointers for the command
    imager_command *cmd_struct = find_struct (cmd);
    if (cmd_struct==NULL)
        err ("command '%s' not found", cmd);

    // print command-specific help
    if (help) {
        char ** p = cmd_struct->parameters();
        if (p==NULL || *p==NULL) {
            fprintf (stderr, "command '%s' has no parameters\n", cmd_struct->name);
        } else {
            fprintf (stderr, "parameters for '%s' (* - mandatory):\n", cmd_struct->name);
            while (*p && *(p+1)) {
                fprintf (stderr, "%18s - %s\n", *p, *(p+1));
                p+=2;
            }
        }
        exit (0);
    }

    // fill out the request struct and pass to the validator
    imager_request * req = & reqs [index];
    req->cmd = cmd_struct;
    req->params = params;
    req->index = index;
    req->internal = NULL;
    if (cmd_struct->validate (req))
        err ("incorrect parameters for command '%s'", cmd_struct->name);

    return cmd_struct;
}

static void set_global_parameter (char * key, char * val)
{
    if (strcmp (key, "debug")==0) {
        print_debug = parse_boolean (val);
        set_debug (print_debug);
    } else if (strcmp (key, "argv")==0) {
        print_argv = parse_boolean (val);
    } else if (strcmp (key, "work")==0) {
        set_work_dir (val);
    } else if (strcmp (key, "work_size")==0) {
        set_work_limit (parse_bytes (val));
    } else if (strcmp (key, "cache")==0) {
        set_cache_dir (val);
    } else if (strcmp (key, "cache_size")==0) {
        set_cache_limit (parse_bytes (val));
    } else {
        err ("unknown global parameter '%s'", key);
    }
    logprintfl (EUCAINFO, "GLOBAL: %s=%s\n", key, val);
}

static int stale_blob_examiner (const blockblob * bb) 
{
    return 1; // delete all work blobs during fsck
}

static int stat_blobstore (const char * path, blobstore * bs) 
{
    blobstore_meta meta;
    blobstore_stat (bs, &meta);
    long long size_mb       = meta.blocks_limit ? (meta.blocks_limit / 2048) : (-1L); // convert sectors->MB
    long long allocated_mb  = meta.blocks_limit ? (meta.blocks_allocated / 2048) : 0;
    long long reserved_mb   = meta.blocks_limit ? ((meta.blocks_locked + meta.blocks_unlocked) / 2048) : 0;
    long long locked_mb     = meta.blocks_limit ? (meta.blocks_locked / 2048) : (-1L);

    struct statfs fs;
    if (statfs (path, &fs) == -1) { 
        logprintfl (EUCAERROR, "error: failed to stat %s: %s\n", path, strerror(errno));
        return 1;
    }
    long long fs_avail_mb = (long long)fs.f_bsize * (long long)(fs.f_bavail/MEGABYTE);
    long long fs_size_mb =  (long long)fs.f_bsize * (long long)(fs.f_blocks/MEGABYTE); 

    logprintfl (EUCAINFO, "disk space under %s\n", path);
    logprintfl (EUCAINFO, "                 %06lldMB limit (%.1f%% of the file system)\n",
                size_mb, 
                ((double)size_mb/(double)fs_size_mb)*100.0);
    logprintfl (EUCAINFO, "                 %06lldMB reserved for use (%.1f%% of limit)\n", 
                reserved_mb, 
                ((double)reserved_mb/(double)size_mb)*100.0 );
    logprintfl (EUCAINFO, "                 %06lldMB locked for use (%.1f%% of limit)\n", 
                locked_mb, 
                ((double)locked_mb/(double)size_mb)*100.0 );
    logprintfl (EUCAINFO, "                 %06lldMB allocated for use (%.1f%% of limit, %.1f%% of the file system)\n", 
                allocated_mb, 
                ((double)allocated_mb/(double)size_mb)*100.0,
                ((double)allocated_mb/(double)fs_size_mb)*100.0 );
    return 0;
}

int main (int argc, char * argv[])
{
    set_debug (print_debug);

    // initialize globals
    artifacts_map = map_create (10);

    // use $EUCALYPTUS env var if available
    char euca_root [] = "";
    euca_home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!euca_home) {
        euca_home = euca_root;
    }

    // save the command line into a buffer so it's easier to rerun it by hand
    char argv_str [4096];
    argv_str[0] = '\0';
    for (int i=0; i<argc; i++) {
        strncat (argv_str, "\"", sizeof (argv_str) - strlen(argv_str) - 1);
        strncat (argv_str, argv[i], sizeof (argv_str) - strlen(argv_str) - 1);
        strncat (argv_str, "\" ", sizeof (argv_str) - strlen(argv_str) - 1);
    }

    // parse command-line parameters
    char * cmd_name = NULL;
    imager_param * cmd_params = NULL;
    int nparams = 0;
    int ncmds = 0;
    while ( *(++argv) ) {
        char * eq = strstr(*argv, "="); // all params have '='s

        if (eq==NULL) { // it's a command
            // process previous command, if any
            if (validate_cmd (ncmds, cmd_name, cmd_params, *argv)!=NULL)
                ncmds++; // increment only if there was a previous command

            if (ncmds+1>MAX_REQS)
                err ("too many commands (max is %d)", MAX_REQS);

            cmd_name = * argv;
            cmd_params = NULL;
            nparams = 0;

        } else { // this is a parameter
            if (strlen (eq) == 1)
                usage ("parameters must have non-empty values");
            * eq = '\0'; // split key from value
            if (strlen (* argv) == 1)
                usage ("parameters must have non-empty names");
            char * key = * argv;
            char * val = eq + 1;
            if (key==NULL || val==NULL)
                usage ("syntax error in parameters");
            if (key[0]=='-') key++; // skip '-' if any
            if (key[0]=='-') key++; // skip second '-' if any

            if (cmd_name==NULL) { // without a preceding command => global parameter
                set_global_parameter (key, val);
                continue;
            }

            if (cmd_params==NULL) {
                cmd_params = calloc (MAX_PARAMS+1, sizeof(imager_param)); // +1 for terminating NULL
                if(!cmd_params)
                    err ("calloc failed");
            }
            if (nparams+1>MAX_PARAMS)
                err ("too many parameters (max is %d)", MAX_PARAMS);
            cmd_params[nparams].key = key;
            cmd_params[nparams].val = val;
            nparams++;
        }
    }
    if (validate_cmd (ncmds, cmd_name, cmd_params, *argv)!=NULL) // validate last command
        ncmds++;

    logprintfl (EUCAINFO, "verified all parameters for %d command(s)\n", ncmds);
    if (print_argv) {
        logprintfl (EUCADEBUG, "argv[]: %s\n", argv_str);
    }

    // record PID, which may be used by VB to kill the imager process (e.g., in cancelBundling)
    pid_t pid = getpid();
    char pid_file [EUCA_MAX_PATH];
    sprintf (pid_file, "%s/imager.pid", get_work_dir());
    FILE *fp = fopen (pid_file, "w");
    if (fp==NULL) {
        err ("could not create pid file");
    } else {
        fprintf (fp, "%d", pid);
        fclose (fp);
    }

    // invoke the requirements checkers in the same order as on command line,
    // constructing the artifact tree originating at 'root'
    artifact * root = NULL;
    for (int i=0; i<ncmds; i++) {
        if (reqs[i].cmd->requirements!=NULL) {
            art_set_instanceId (reqs[i].cmd->name); // for logging
            if ((root = reqs[i].cmd->requirements (&reqs[i], root))==NULL) // pass results of earlier checkers to later checkers
                err ("failed while verifying requirements");
        }
    }
    // it is OK for root to be NULL at this point
    
    // see if work blobstore will be needed at any stage
    // and open or create the work blobstore
    blobstore * work_bs = NULL;

    if (root && tree_uses_blobstore (root)) {
        // set the function that will catch blobstore errors
        blobstore_set_error_function ( &bs_errors ); 

        if (ensure_directories_exist (get_work_dir(), 0, NULL, NULL, BLOBSTORE_DIRECTORY_PERM) == -1)
            err ("failed to open or create work directory %s", get_work_dir());
        work_bs = blobstore_open (get_work_dir(), get_work_limit()/512, BLOBSTORE_FLAG_CREAT, BLOBSTORE_FORMAT_FILES, BLOBSTORE_REVOCATION_NONE, BLOBSTORE_SNAPSHOT_ANY);
        if (work_bs==NULL) {
            err ("failed to open work blobstore: %s", blobstore_get_error_str(blobstore_get_error()));
        }
        // no point in fscking the work blobstore as it was just created
    }
   
    // see if cache blobstore will be needed at any stage
    blobstore * cache_bs = NULL;
    if (root && tree_uses_cache (root)) {
        if (ensure_directories_exist (get_cache_dir(), 0, NULL, NULL, BLOBSTORE_DIRECTORY_PERM) == -1)
            err ("failed to open or create cache directory %s", get_cache_dir());
        cache_bs = blobstore_open (get_cache_dir(), get_cache_limit()/512, BLOBSTORE_FLAG_CREAT, BLOBSTORE_FORMAT_DIRECTORY, BLOBSTORE_REVOCATION_LRU, BLOBSTORE_SNAPSHOT_ANY);
        if (cache_bs==NULL) {
            blobstore_close (work_bs);            
            err ("failed to open cache blobstore: %s\n", blobstore_get_error_str(blobstore_get_error()));
        }
        if (blobstore_fsck (cache_bs, NULL)) // TODO: verify checksums?
            err ("cache blobstore failed integrity check: %s", blobstore_get_error_str(blobstore_get_error()));

        if (stat_blobstore (get_cache_dir(), cache_bs))
            err ("blobstore is unreadable");
    }

    // implement the artifact tree
    int ret = OK;
    if (root) {
        art_set_instanceId ("imager"); // for logging
        ret = art_implement_tree (root, work_bs, cache_bs, NULL, INSTANCE_PREP_TIMEOUT_USEC); // do all the work!
    }

    // invoke the cleaners for each command to tidy up disk space and memory allocations
    for (int i=0; i<ncmds; i++) {
        if (reqs[i].cmd->cleanup!=NULL) {
            art_set_instanceId (reqs[i].cmd->name); // for logging
            reqs[i].cmd->cleanup (&reqs[i], (i==(ncmds-1))?(TRUE):(FALSE));
        }
    }

    // free the artifact tree
    if (root) {
        if (tree_uses_blobstore (root)) {
            if (blobstore_fsck (work_bs, stale_blob_examiner)) { // will remove all blobs
                logprintfl (EUCAWARN, "WARNING: failed to clean up work space: %s\n", blobstore_get_error_str(blobstore_get_error()));
            }
        }
        art_free (root);
    }
    clean_work_dir (work_bs);

    // indicate completion
    logprintfl (EUCAINFO, "imager done (exit code=%d)\n", ret);

    exit (ret);
}

// common functions used by commands

void print_req (imager_request * req)
{
    logprintfl (EUCAINFO, "command: %s\n", req->cmd->name);
    for (imager_param * p = req->params; p!=NULL && p->key!=NULL; p++) {
        logprintfl (EUCAINFO, "\t%s=%s\n", p->key, p->val);
    }
}

// read in login or password from command line or from a file

char * parse_loginpassword (const char * s)
{
    char * val = strdup (s);
    FILE * fp;
    if ((fp = fopen (s, "r"))!=NULL) {
        if(val) 
            free(val);
        val = fp2str (fp);
        if (val==NULL) {
            err ("failed to read file '%s'", s);
        } else {
            logprintfl (EUCAINFO, "read in contents from '%s'\n", s);
        }
        fclose(fp);
    }

    return val;
}

// parse a string into bytes, a la 'dd':
//
// If the number ends with a ``b'', ``k'', ``m'', ``g'',
// or ``w'', the number is multiplied by 512, 1024 (1K), 1048576 (1M),
// 1073741824 (1G) or the number of bytes in an integer, respectively.
long long parse_bytes (const char * s)
{
    char * suffix;
    errno = 0;
    long long result = strtoull (s, &suffix, 0);
    if ( errno != 0 ) {
        err ("empty or invalid size specification '%s'\n", s);
    }
    long long multiplier = 1L;
    if ( * suffix!='\0' ) {
        switch ( toupper (* suffix) ) {
        case 'B': multiplier = 512L; break; // blocks
        case 'K': multiplier = 1024L; break; // kilo
        case 'M': multiplier = 1048576L; break; // mega
        case 'G': multiplier = 1073741824L; break; // giga
        case 'W': multiplier = sizeof (int); break; // word
        default:
            err ( "unrecognized suffix '%s' in size specification\n", suffix);
        }
        if ( *(suffix+1)!='\0' ) {
            err ( "suffix '%s' in size specification is longer than one letter\n", suffix);
        }
    }
    return result * multiplier;
}

// make sure the path exists and is readable

int verify_readability (const char * path)
{
    if (fopen (path, "r")==NULL)
        err ("unable to read '%s'", path);
    return 0;
}

// return eucalyptus root

char * get_euca_home (void)
{
    return euca_home;
}

// return global artifacts map

map * get_artifacts_map (void)
{
    return artifacts_map;
}

// if path=A/B/C but only A exists, this will try to create B and C
int ensure_path_exists (const char * path, mode_t mode)
{
    int len = strlen (path);
    char * path_copy = strdup (path);
    int i;

    if (path_copy==NULL)
        return errno;

    for (i=0; i<len; i++) {
        struct stat buf;
        char try_it = 0;

        if (path[i]=='/' && i>0) {
            path_copy[i] = '\0';
            try_it = 1;
        } else if (path[i]!='/' && i+1==len) { // last one
            try_it = 1;
        }

        if ( try_it ) {
            if ( stat (path_copy, &buf) == -1 ) {
                logprintfl (EUCAINFO, "creating path %s\n", path_copy);

                if ( mkdir (path_copy, mode) == -1) {
                    logprintfl (EUCAERROR, "error: failed to create path %s: %s\n", path_copy, strerror (errno));

                    if (path_copy)
                        free (path_copy);
                    return errno;
                }
            }
            path_copy[i] = '/'; // restore the slash
        }
    }

    free (path_copy);
    return 0;
}

// if path=A/B/C but only A exists, this will try to create B, but not C
int ensure_dir_exists (const char * path, mode_t mode)
{
    int len = strlen (path);
    char * path_copy = strdup (path);
    int i, err = 0;

    if (path_copy==NULL)
        return errno;

    for (i=len-1; i>0; i--) {
        if (path[i]=='/') {
            path_copy[i] = '\0';
            err = ensure_path_exists (path_copy, mode);
            break;
        }
    }

    free (path_copy);
    return err;
}

// function for bypassing sentinel artifacts in a tree
artifact * skip_sentinels (artifact * root)
{
    artifact * ret = root;
    while (ret) {
        if (ret->creator != NULL) break; // has a creator => not a sentinel
        if (ret->deps[1] != NULL) break; // has multiple children => do not skip
        artifact * next_ret = ret->deps[0];
        free (ret);
        ret = next_ret;
    }
    return ret;
}

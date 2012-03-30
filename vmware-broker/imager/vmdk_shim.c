// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
 * shim surrounding vmdk.c that allows all of the methods therein 
 * to be invoked from a different process, thus avoiding library conflicts
 */

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/ipc.h> // ftok
#include <sys/shm.h> // shmget et al
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <fcntl.h>
#include <time.h>
#include <errno.h>
#include <string.h>
#include <assert.h>

#include "eucalyptus.h"
#include "vmdk.h"

#define VMDK_CMD                 "euca_vmdk"
#define VMDK_SHMEM_MAGICK        0xbeefcafe
#define VMDK_GEN_CALL            "gen_vmdk"
#define VMDK_GET_SIZE_CALL       "vmdk_get_size"
#define VMDK_CONVERT_REMOTE_CALL "vmdk_convert_remote"
#define VMDK_CLONE_CALL          "vmdk_clone"
#define VMDK_CONVERT_LOCAL_CALL  "vmdk_convert_local"

struct vmdk_shmem {
    int magick;
    int shmid;
    key_t key; 
    char fname [64];
    char path1 [MAX_PATH];
    char path2 [MAX_PATH];
    img_spec spec;
    boolean flag;
    int ret_int;
    long long ret_long_long;
};

/*
 * front-end half of the vmdk.c shim
 */

#ifdef VMDK_CALLER

#include "http.h"

static char my_path   [MAX_PATH] = "";
static char vmdk_path [MAX_PATH] = "";

static int set_paths ()
{
    if (strlen (my_path) < 1 || strlen (vmdk_path) < 1) {
        // determine the executable's path in a Linux-specific way
        if (readlink("/proc/self/exe", my_path, sizeof (my_path)) == -1) {
            logprintfl (EUCAERROR, "failed to determine path for executable\n", strerror(errno));
            return 1;
        }
        
        // assume that _euca_vmdk is in the same directory
        safe_strncpy (vmdk_path, my_path, sizeof (vmdk_path));
        char * lslash = strrchr (vmdk_path, '/');
        if (lslash == NULL) {
            logprintfl (EUCAERROR, "unexpected path for executable (%s)\n", vmdk_path);
            return 1;
        }
        lslash++; // go past the slash
        snprintf (lslash, sizeof (vmdk_path) - (lslash - vmdk_path) - 2, VMDK_CMD); // append different command name
        if (strstr (vmdk_path, VMDK_CMD) == NULL) {
            logprintfl (EUCAERROR, "unable to construct valid executable path (%s)\n", vmdk_path);
            return 1;
        }
    }
    
    return 0;
}

static struct vmdk_shmem * alloc_shmem ()
{
    set_paths();
    key_t key = ftok (my_path, (int)getpid()); // generate a shared-memory region key
    if (key==-1) {
        logprintfl (EUCAERROR, "failed to generate a System V IPC key: %s\n", strerror(errno));
        return NULL;
    }
    logprintfl (EUCADEBUG, "generated shared memory segment key [%u]\n", key);
    int shmid = shmget (key, sizeof (struct vmdk_shmem), IPC_CREAT | IPC_EXCL | 0600);
    if (shmid==-1) {
        logprintfl (EUCAERROR, "failed to allocate shared memory segment: %s\n", strerror(errno));
        return NULL;
    }
    void * mem = shmat (shmid, NULL, 0);
    if (mem==(void *)-1) {
        logprintfl (EUCAERROR, "failed to attach shared memory segment: %s\n", strerror(errno));
        shmctl(shmid, IPC_RMID, NULL);
        return NULL;
    }
    bzero (mem, sizeof (struct vmdk_shmem));
    struct vmdk_shmem * shm = (struct vmdk_shmem *) mem;
    shm->magick = VMDK_SHMEM_MAGICK;
    shm->shmid = shmid;
    shm->key = key;
    
    return shm;
}

static void free_shmem (struct vmdk_shmem * shm)
{
    int shmid = shm->shmid;
    if (shmdt ((void *)shm) == -1) {
        logprintfl (EUCAERROR, "failed to detach shared memory segment: %s\n", strerror(errno));
    }
    if (shmctl (shmid, IPC_RMID, NULL) == -1) {
        logprintfl (EUCAERROR, "failed to remove shared memory segment: %s\n", strerror(errno));
    }
}

static int do_call (const char * fname, struct vmdk_shmem * shm)
{
    strncpy (shm->fname, fname, sizeof (shm->fname));
    char cmd [1025];
    snprintf (cmd, sizeof (cmd), "%s %u", vmdk_path, shm->key);
    logprintfl (EUCADEBUG, "executing [%s]\n", cmd);
    return WEXITSTATUS (system (cmd));
}

int vmdk_init (void)
{
    set_paths();
    if (check_file (vmdk_path)) {
        logprintfl (EUCAERROR, "failed to locate %s", VMDK_CMD);
        return ERROR;
    }
    return OK;
}

// generate a VMDK metadata file
int gen_vmdk (const char * disk_path, const char * vmdk_path)
{
    int ret = ERROR;

    struct vmdk_shmem * shm = alloc_shmem ();
    if (shm == NULL)
        return ret;

    strncpy (shm->path1, disk_path, sizeof (shm->path1));
    strncpy (shm->path2, vmdk_path, sizeof (shm->path2));
    if (do_call (VMDK_GEN_CALL, shm) == OK)
        ret = shm->ret_int;

    free_shmem (shm);
    
    return ret;
}

// uploading a file to vSphere endpoint
int vmdk_upload (const char * file_path, const img_spec * dest)
{
    const img_loc * loc = &(dest->location);
    char url [1024];
    int ret = OK;

    char * en_path = strdup (loc->path); // TODO: how to encode paths so slashes are maintained?
    char * en_dc   = url_encode (loc->vsphere_dc);
    char * en_ds   = url_encode (loc->vsphere_ds);
    if (en_path == NULL
        || en_dc == NULL
        || en_ds == NULL) {
        logprintfl (EUCAERROR, "out of memory in vmdk_upload()\n");
        ret = ERROR;
        goto cleanup;
    }
    // EXAMPLE: https://192.168.7.236/folder/i-4DD50852?dcPath=ha-datacenter&dsName=S1
    snprintf (url, sizeof(url), "https://%s/folder/%s?dcPath=%s&dsName=%s", loc->host, en_path, en_dc, en_ds);
    if (http_put (file_path, url, loc->creds.login, loc->creds.password) != OK) {
        logprintfl (EUCAFATAL, "upload of file '%s' to '%s' failed\n", file_path, url);
        ret = ERROR;
    }

 cleanup:
    if (en_path) free (en_path);
    if (en_dc) free (en_dc);
    if (en_ds) free (en_ds);

    return ret;
}

// get the size of the VMDK file, in bytes
long long vmdk_get_size (const img_spec * spec)
{
    long long ret = -1;
    
    struct vmdk_shmem * shm = alloc_shmem ();
    if (shm == NULL)
        return ret;
    
    memcpy (&(shm->spec), spec, sizeof (img_spec));
    if (do_call (VMDK_GET_SIZE_CALL, shm) == OK)
        ret = shm->ret_long_long;
    
    free_shmem (shm);
    
    return ret;
}

// converts a remote VMDK to a local plain disk file
int vmdk_convert_remote (const img_spec * spec, const char * disk_path)
{
    int ret = ERROR;
    
    struct vmdk_shmem * shm = alloc_shmem ();
    if (shm == NULL)
        return ret;
    
    strncpy (shm->path1,   disk_path, sizeof (shm->path1));
    memcpy  (&(shm->spec), spec,      sizeof (img_spec));
    if (do_call (VMDK_CONVERT_REMOTE_CALL, shm) == OK)
        ret = shm->ret_int;
    
    free_shmem (shm);
    
    return ret;
}

// clone a local VMDK to a remote datastore
int vmdk_clone (const char * vmdk_path, const img_spec * spec)
{
    int ret = ERROR;
    
    struct vmdk_shmem * shm = alloc_shmem ();
    if (shm == NULL)
        return ret;
    
    strncpy (shm->path1,   vmdk_path, sizeof (shm->path1));
    memcpy  (&(shm->spec), spec,      sizeof (img_spec));
    if (do_call (VMDK_CLONE_CALL, shm) == OK)
        ret = shm->ret_int;
    
    free_shmem (shm);
    
    return ret;
}

// convert a local disk file to a local VMDK
int vmdk_convert_local (const char * disk_path, const char * vmdk_path, boolean overwrite)
{
    int ret = ERROR;
    
    struct vmdk_shmem * shm = alloc_shmem ();
    if (shm == NULL)
        return ret;
    
    strncpy (shm->path1, disk_path, sizeof (shm->path1));
    strncpy (shm->path2, vmdk_path, sizeof (shm->path2));
    shm->flag = overwrite;
    if (do_call (VMDK_CONVERT_LOCAL_CALL, shm) == OK)
        ret = shm->ret_int;
    
    free_shmem (shm);
    
    return ret;
}

// helper used by vSphere-related commands to break up path into vds (datacenter) and vpath (path on datacenter)
// TODO: this code is duplicated in vmdk.c - figure out a way to unify the two
int parse_vsphere_path (const char * path, char * vds, const int vds_size, char * vpath, const int vpath_size)
{
    char * br1 = strstr (path, "[");
    char * br2 = strstr (path, "]");
    
    if (br1==NULL || br2==NULL || br1>=br2 || (br2-path+2)>strlen(path)) return ERROR;
    
    int len = br2-br1-1;
    if (len>vds_size) len = vds_size;
    strncpy (vds, br1+1, len);
    
    if (*(br2+1)==' ') br2++; // skip the space after [datastore], if any
    strncpy (vpath, br2+1, vpath_size);
    
    return OK;
}

#ifdef _UNIT_TEST

/*
 * main() for a partial unit test of vmdk_shim
 * (it is partial in that all calls are tested,
 * but only with invalid parameters)
 */
int main (int argc, char * argv[])
{
    img_spec spec;
    bzero (&spec, sizeof (img_spec));

    // make real invocations with fake parameters and expect them all to fail
    assert (gen_vmdk ("/foo/bar", "/foo/baz"));
    assert (vmdk_get_size (&spec) == -1L);
    assert (vmdk_convert_remote (&spec, "/foo/bar"));
    assert (vmdk_clone ("/foo/bar", &spec));
    assert (vmdk_convert_local ("/foo/bar", "/foo/baz", 0));

    // make a fake invocation
    struct vmdk_shmem * shm = alloc_shmem ();
    assert (shm != NULL);
    assert (do_call ("invalid-function-call", shm) != OK);
    free_shmem (shm);
}

#endif // _UNIT_TEST

/*
 * back-end half of the vmdk.c shim
 */

#elif VMDK_CALLEE

static struct vmdk_shmem * find_shmem (key_t key)
{
    int shmid = shmget (key, sizeof (struct vmdk_shmem), 0);
    if (shmid==-1) {
        logprintfl (EUCAERROR, "failed to find shared memory segment: %s\n", strerror(errno));
        return NULL;
    }
    void * mem = shmat (shmid, NULL, 0);
    if (mem==(void *)-1) {
        logprintfl (EUCAERROR, "failed to attach shared memory segment: %s\n", strerror(errno));
        return NULL;
    }
    struct vmdk_shmem * shm = (struct vmdk_shmem *) mem;
    if (shm->magick != VMDK_SHMEM_MAGICK) {
        logprintfl (EUCAERROR, "failed to verify magick number (%u!=%u)\n", shm->magick, VMDK_SHMEM_MAGICK);
        shmdt (mem);
        return NULL;
    }

    return shm;
}

/*
 * main() for the back-end half of the shim, which
 * finds the shared memory segment created by the parent,
 * locates desired function name in it, unmarshalls the
 * parameters, makes the call, and stores the result back
 * into the shared memory region
 */
int main (int argc, char * argv[])
{
    logfile (NULL, EUCADEBUG, 4);
    if (argc != 2) {
        logprintfl (EUCAERROR, "%s: unexpected number of arguments (%d)\n", argv[0], argc);
    }

    // find, using the key, and attach the shared memory segment created by the parent process
    char * endptr;
    errno = 0;
    key_t key = (key_t) strtoull (argv[1], &endptr, 0);
    if (errno != 0) {
        logprintfl (EUCAERROR, "%s: failed to parse shmem region key number on command line (%s)\n", argv[0], argv[1]);
        exit (1);
    }
    struct vmdk_shmem * shm = find_shmem (key);
    if (shm == NULL)
        exit (1);

    // initialize the vmdk wrapper
    if (vmdk_init() != OK) {
        logprintfl (EUCAERROR, "%s: failed to initialize the VDDK wrapper\n", argv[0]);
        shmdt ((void *)shm);
        exit (1);
    }

    // find the desired function name in shm->fname and call it,
    // having extracted the parameters from other fields in shm, 
    // and store the result back into shm (either ret_int or ret_long_long)
    int ret = 0; // 0 = sucessfully was able to invoke one of the vmdk_* funcitons (not their result)
    if (strcmp (shm->fname, VMDK_GEN_CALL) == 0) {
        shm->ret_int = gen_vmdk (shm->path1, shm->path2);
    } else if (strcmp (shm->fname, VMDK_GET_SIZE_CALL) == 0) {
        shm->ret_long_long = vmdk_get_size (&shm->spec);
    } else if (strcmp (shm->fname, VMDK_CONVERT_REMOTE_CALL) == 0) {
        shm->ret_int = vmdk_convert_remote (&shm->spec, shm->path1);
    } else if (strcmp (shm->fname, VMDK_CLONE_CALL) == 0) {
        shm->ret_int = vmdk_clone (shm->path1, &shm->spec);
    } else if (strcmp (shm->fname, VMDK_CONVERT_LOCAL_CALL) == 0) {
        shm->ret_int = vmdk_convert_local (shm->path1, shm->path2, shm->flag);
    } else {
        logprintfl (EUCAERROR, "%s: unknown function call %s\n", argv[0], shm->fname);
        ret = 1;
    }
    shmdt ((void *)shm);
    
    return ret;
}

#endif

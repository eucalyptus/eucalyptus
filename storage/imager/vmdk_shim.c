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
//! @file storage/imager/vmdk_shim.c
//!
//! Shim surrounding vmdk.c that allows all of the methods therein
//! to be invoked from a different process, thus avoiding library conflicts
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/ipc.h>                   // ftok
#include <sys/shm.h>                   // shmget et al
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <fcntl.h>
#include <time.h>
#include <errno.h>
#include <string.h>
#include <assert.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <http.h>
#include "vmdk.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define VMDK_CMD                                 "euca-vmdk-wrapper"
#define VMDK_SHMEM_MAGICK                        0xbeefcafe
#define VMDK_GEN_CALL                            "gen_vmdk"
#define VMDK_GET_SIZE_CALL                       "vmdk_get_size"
#define VMDK_CONVERT_TO_REMOTE_CALL              "vmdk_convert_to_remote"
#define VMDK_CONVERT_FROM_REMOTE_CALL            "vmdk_convert_from_remote"
#define VMDK_CLONE_CALL                          "vmdk_clone"
#define VMDK_CONVERT_LOCAL_CALL                  "vmdk_convert_local"

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

struct vmdk_shmem {
    int magick;
    int shmid;
    key_t key;
    char fname[64];
    char path1[EUCA_MAX_PATH];
    char path2[EUCA_MAX_PATH];
    img_spec spec;
    boolean flag;
    long long start_sector;
    long long end_sector;
    int ret_int;
    s64 ret_long_long;
};

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

#ifdef VMDK_CALLER
static char my_path[EUCA_MAX_PATH] = "";
static char vmdk_path[EUCA_MAX_PATH] = "";
#endif /* VMDK_CALLER */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef VMDK_CALLER
static int set_paths(void);
static struct vmdk_shmem *alloc_shmem(void);
static void free_shmem(struct vmdk_shmem *shm);
static int do_call(const char *fname, struct vmdk_shmem *shm);
#endif /* VMDK_CALLER */

#ifdef VMDK_CALLEE
static struct vmdk_shmem *find_shmem(key_t key);
#endif /* VMDK_CALLEE */

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

#ifdef VMDK_CALLER
//!
//!
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @pre
//!
//! @post
//!
static int set_paths(void)
{
    int count = 0;
    char *lslash = NULL;

    if (strlen(my_path) < 1 || strlen(vmdk_path) < 1) {
        // determine the executable's path in a Linux-specific way
        if ((count = readlink("/proc/self/exe", my_path, sizeof(my_path))) == -1) {
            LOGERROR("failed to determine path for executable. errno=%d(%s)\n", errno, strerror(errno));
            return 1;
        }
        // Adds a NULL caracter at the end of our string since readlink() does not do it.
        count = ((count < sizeof(my_path)) ? count : (sizeof(my_path) - 1));
        my_path[count] = '\0';

        // assume that $VMDK_CMD is in the same directory
        euca_strncpy(vmdk_path, my_path, sizeof(vmdk_path));
        if ((lslash = strrchr(vmdk_path, '/')) == NULL) {
            LOGERROR("unexpected path for executable (%s)\n", vmdk_path);
            return EUCA_ERROR;
        }

        lslash++;                      // go past the slash
        snprintf(lslash, sizeof(vmdk_path) - (lslash - vmdk_path) - 2, VMDK_CMD);   // append different command name
        if (strstr(vmdk_path, VMDK_CMD) == NULL) {
            LOGERROR("unable to construct valid executable path (%s)\n", vmdk_path);
            return EUCA_ERROR;
        }
    }

    return EUCA_OK;
}

//!
//!
//!
//! @return A pointer to the shared memory or NULL if any failure occured.
//!
//! @pre
//!
//! @post
//!
static struct vmdk_shmem *alloc_shmem(void)
{
    int shmid = -1;
    key_t key = -1;
    void *mem = ((void *)-1);
    struct vmdk_shmem *shm = NULL;

    set_paths();

    // generate a shared-memory region key
    if ((key = ftok(my_path, (int)getpid())) == -1) {
        LOGERROR("failed to generate a System V IPC key: %s\n", strerror(errno));
        return NULL;
    }

    LOGDEBUG("generated shared memory segment key [%u]\n", key);
    if ((shmid = shmget(key, sizeof(struct vmdk_shmem), IPC_CREAT | IPC_EXCL | 0600)) == -1) {
        LOGERROR("failed to allocate shared memory segment: %s\n", strerror(errno));
        return NULL;
    }

    if ((mem = shmat(shmid, NULL, 0)) == ((void *)-1)) {
        LOGERROR("failed to attach shared memory segment: %s\n", strerror(errno));
        shmctl(shmid, IPC_RMID, NULL);
        return NULL;
    }

    bzero(mem, sizeof(struct vmdk_shmem));

    shm = ((struct vmdk_shmem *)mem);
    shm->magick = VMDK_SHMEM_MAGICK;
    shm->shmid = shmid;
    shm->key = key;
    return shm;
}

//!
//!
//!
//! @param[in] shm
//!
//! @pre
//!
//! @post
//!
static void free_shmem(struct vmdk_shmem *shm)
{
    int shmid = shm->shmid;

    if (shmdt(((void *)shm)) == -1) {
        LOGERROR("failed to detach shared memory segment: %s\n", strerror(errno));
    }

    if (shmctl(shmid, IPC_RMID, NULL) == -1) {
        LOGERROR("failed to remove shared memory segment: %s\n", strerror(errno));
    }
}

//!
//!
//!
//! @param[in] fname
//! @param[in] shm
//!
//! @return The result of euca_execlp()
//!
//! @pre
//!
//! @post
//!
//! @see euca_execlp()
//!
static int do_call(const char *fname, struct vmdk_shmem *shm)
{
    char keyStr[32] = "";

    euca_strncpy(shm->fname, fname, sizeof(shm->fname));
    snprintf(keyStr, 32, "%d", shm->key);

    LOGDEBUG("executing [%s %s]\n", vmdk_path, keyStr);
    return (euca_execlp(NULL, vmdk_path, keyStr, NULL));
}

//!
//!
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int vmdk_init(void)
{
    set_paths();
    if (check_file(vmdk_path)) {
        LOGDEBUG("failed to locate %s\n", VMDK_CMD);
        return EUCA_ERROR;
    }
    return EUCA_OK;
}

//!
//! Generate a VMDK metadata file
//!
//! @param[in] disk_path
//! @param[in] vmdk_path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int gen_vmdk(const char *disk_path, const char *vmdk_path)
{
    int ret = EUCA_ERROR;
    struct vmdk_shmem *shm = NULL;

    if ((shm = alloc_shmem()) == NULL)
        return ret;

    euca_strncpy(shm->path1, disk_path, sizeof(shm->path1));
    euca_strncpy(shm->path2, vmdk_path, sizeof(shm->path2));
    if (do_call(VMDK_GEN_CALL, shm) == EUCA_OK)
        ret = shm->ret_int;

    free_shmem(shm);
    return ret;
}

//!
//! Uploading a file to vSphere endpoint
//!
//! @param[in] file_path
//! @param[in] dest
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int vmdk_upload(const char *file_path, const img_spec * dest)
{
    int ret = EUCA_OK;
    char *en_path = NULL;
    char *en_dc = NULL;
    char *en_ds = NULL;
    char url[1024] = "";
    const img_loc *loc = &(dest->location);

    en_path = strdup(loc->path);       //! @TODO how to encode paths so slashes are maintained?
    en_dc = url_encode(loc->vsphere_dc);
    en_ds = url_encode(loc->vsphere_ds);
    if (en_path == NULL || en_dc == NULL || en_ds == NULL) {
        LOGERROR("out of memory\n");
        ret = EUCA_ERROR;
        goto cleanup;
    }
    // EXAMPLE: https://192.168.7.236/folder/i-4DD50852?dcPath=ha-datacenter&dsName=S1
    snprintf(url, sizeof(url), "https://%s/folder/%s?dcPath=%s&dsName=%s", loc->host, en_path, en_dc, en_ds);
    if (http_put(file_path, url, loc->creds.login, loc->creds.password) != EUCA_OK) {
        LOGFATAL("upload of file '%s' to '%s' failed\n", file_path, url);
        ret = EUCA_ERROR;
    }

cleanup:
    EUCA_FREE(en_path);
    EUCA_FREE(en_dc);
    EUCA_FREE(en_ds);
    return ret;
}

//!
//! Get the size of the VMDK file, in bytes
//!
//! @param[in] spec
//!
//! @return The size of the VMDK file in bytes on success or -1 on failure
//!
//! @pre
//!
//! @post
//!
s64 vmdk_get_size(const img_spec * spec)
{
    s64 ret = -1;
    struct vmdk_shmem *shm = NULL;

    if ((shm = alloc_shmem()) == NULL)
        return ret;

    memcpy(&(shm->spec), spec, sizeof(img_spec));
    if (do_call(VMDK_GET_SIZE_CALL, shm) == EUCA_OK)
        ret = shm->ret_long_long;

    free_shmem(shm);
    return ret;
}

//!
//! Converts a local raw disk file or stream into a remote VMDK
//!
//! @param[in] spec
//! @param[in] disk_path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int vmdk_convert_to_remote(const char *disk_path, const img_spec * spec, long long start_sector, long long end_sector)
{
    int ret = EUCA_ERROR;
    struct vmdk_shmem *shm = NULL;

    if ((shm = alloc_shmem()) == NULL)
        return ret;

    euca_strncpy(shm->path1, disk_path, sizeof(shm->path1));
    memcpy(&(shm->spec), spec, sizeof(img_spec));
    shm->start_sector = start_sector;
    shm->end_sector = end_sector;
    if (do_call(VMDK_CONVERT_TO_REMOTE_CALL, shm) == EUCA_OK)
        ret = shm->ret_int;

    free_shmem(shm);
    return ret;
}

//!
//! Converts a remote VMDK to a local plain disk file or stream
//!
//! @param[in] spec
//! @param[in] disk_path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int vmdk_convert_from_remote(const img_spec * spec, const char *disk_path, long long start_sector, long long end_sector)
{
    int ret = EUCA_ERROR;
    struct vmdk_shmem *shm = NULL;

    if ((shm = alloc_shmem()) == NULL)
        return ret;

    euca_strncpy(shm->path1, disk_path, sizeof(shm->path1));
    memcpy(&(shm->spec), spec, sizeof(img_spec));
    shm->start_sector = start_sector;
    shm->end_sector = end_sector;
    if (do_call(VMDK_CONVERT_FROM_REMOTE_CALL, shm) == EUCA_OK)
        ret = shm->ret_int;

    free_shmem(shm);
    return ret;
}

//!
//! Clone a local VMDK to a remote datastore
//!
//! @param[in] vmdk_path
//! @param[in] spec
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int vmdk_clone(const char *vmdk_path, const img_spec * spec)
{
    int ret = EUCA_ERROR;
    struct vmdk_shmem *shm = NULL;

    if ((shm = alloc_shmem()) == NULL)
        return ret;

    euca_strncpy(shm->path1, vmdk_path, sizeof(shm->path1));
    memcpy(&(shm->spec), spec, sizeof(img_spec));
    if (do_call(VMDK_CLONE_CALL, shm) == EUCA_OK)
        ret = shm->ret_int;

    free_shmem(shm);
    return ret;
}

//!
//! Convert a local disk file to a local VMDK
//!
//! @param[in] disk_path
//! @param[in] vmdk_path
//! @param[in] overwrite
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int vmdk_convert_local(const char *disk_path, const char *vmdk_path, boolean overwrite)
{
    int ret = EUCA_ERROR;
    struct vmdk_shmem *shm = NULL;

    if ((shm = alloc_shmem()) == NULL)
        return ret;

    euca_strncpy(shm->path1, disk_path, sizeof(shm->path1));
    euca_strncpy(shm->path2, vmdk_path, sizeof(shm->path2));
    shm->flag = overwrite;
    if (do_call(VMDK_CONVERT_LOCAL_CALL, shm) == EUCA_OK)
        ret = shm->ret_int;

    free_shmem(shm);
    return ret;
}

//!
//! Helper used by vSphere-related commands to break up path into vds (datacenter)
//! and vpath (path on datacenter)
//!
//! @param[in] path
//! @param[in] vds
//! @param[in] vds_size
//! @param[in] vpath
//! @param[in] vpath_size
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
//! @todo this code is duplicated in vmdk.c - figure out a way to unify the two
//!
int parse_vsphere_path(const char *path, char *vds, const int vds_size, char *vpath, const int vpath_size)
{
    int len = 0;
    char *br1 = strstr(path, "[");
    char *br2 = strstr(path, "]");

    if (br1 == NULL || br2 == NULL || br1 >= br2 || (br2 - path + 2) > strlen(path))
        return EUCA_ERROR;

    len = br2 - br1 - 1;
    if (len > vds_size)
        len = vds_size;

    strncpy(vds, br1 + 1, len);

    if (*(br2 + 1) == ' ') {
        // skip the space after [datastore], if any
        br2++;
    }

    euca_strncpy(vpath, br2 + 1, vpath_size);
    return EUCA_OK;
}

#ifdef _UNIT_TEST
//!
//! main() for a partial unit test of vmdk_shim (it is partial in that all calls are tested,
//! but only with invalid parameters)
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char *argv[])
{
    img_spec spec = { {0} };
    struct vmdk_shmem *shm = NULL;

    bzero(&spec, sizeof(img_spec));

    // make real invocations with fake parameters and expect them all to fail
    assert(gen_vmdk("/foo/bar", "/foo/baz"));
    assert(vmdk_get_size(&spec) == -1L);
    assert(vmdk_convert_to_remote("/foo/bar", &spec, 0LL, 0LL));
    assert(vmdk_convert_from_remote(&spec, "/foo/bar", 0LL, 0LL));
    assert(vmdk_clone("/foo/bar", &spec));
    assert(vmdk_convert_local("/foo/bar", "/foo/baz", 0));

    // make a fake invocation
    shm = alloc_shmem();
    assert(shm != NULL);
    assert(do_call("invalid-function-call", shm) != EUCA_OK);
    free_shmem(shm);
}
#endif // _UNIT_TEST
#elif VMDK_CALLEE
//!
//!
//!
//! @param[in] key
//!
//! @return A pointer to the shared memory related to the given key
//!
//! @pre
//!
//! @post
//!
static struct vmdk_shmem *find_shmem(key_t key)
{
    int shmid = -1;
    void *mem = ((void *)-1);
    struct vmdk_shmem *shm = NULL;

    if ((shmid = shmget(key, sizeof(struct vmdk_shmem), 0)) == -1) {
        LOGERROR("failed to find shared memory segment: %s\n", strerror(errno));
        return NULL;
    }

    if ((mem = shmat(shmid, NULL, 0)) == ((void *)-1)) {
        LOGERROR("failed to attach shared memory segment: %s\n", strerror(errno));
        return NULL;
    }

    shm = ((struct vmdk_shmem *)mem);
    if (shm->magick != VMDK_SHMEM_MAGICK) {
        LOGERROR("failed to verify magick number (%u!=%u)\n", shm->magick, VMDK_SHMEM_MAGICK);
        shmdt(mem);
        return NULL;
    }

    return shm;
}

//!
//! main() for the back-end half of the shim, which finds the shared memory segment created by the
//! parent, locates desired function name in it, unmarshalls the parameters, makes the call, and stores
//! the result back into the shared memory region
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char *argv[])
{
    int ret = EUCA_OK;
    char *endptr = NULL;
    key_t key = 0;
    struct vmdk_shmem *shm = NULL;

    log_fp_set(stderr); // imager logs to stderr so image data can be piped to stdout
    logfile(NULL, EUCA_LOG_DEBUG, 4);
    if (argc != 2) {
        LOGERROR("%s: unexpected number of arguments (%d)\n", argv[0], argc);
        exit(1);
    }
    // find, using the key, and attach the shared memory segment created by the parent process
    errno = 0;
    key = (key_t) strtoull(argv[1], &endptr, 10);
    if (errno != 0) {
        LOGERROR("%s: failed to parse shmem region key number on command line (%s)\n", argv[0], argv[1]);
        exit(1);
    }

    if ((shm = find_shmem(key)) == NULL)
        exit(1);

    // initialize the vmdk wrapper
    if (vmdk_init() != EUCA_OK) {
        LOGERROR("%s: failed to initialize the VDDK wrapper\n", argv[0]);
        shmdt(((void *)shm));
        exit(1);
    }
    // find the desired function name in shm->fname and call it,
    // having extracted the parameters from other fields in shm,
    // and store the result back into shm (either ret_int or ret_long_long)
    ret = EUCA_OK;                     // EUCA_OK = sucessfully was able to invoke one of the vmdk_* funcitons (not their result)
    if (strcmp(shm->fname, VMDK_GEN_CALL) == 0) {
        shm->ret_int = gen_vmdk(shm->path1, shm->path2);
    } else if (strcmp(shm->fname, VMDK_GET_SIZE_CALL) == 0) {
        shm->ret_long_long = vmdk_get_size(&shm->spec);
    } else if (strcmp(shm->fname, VMDK_CONVERT_TO_REMOTE_CALL) == 0) {
        shm->ret_int = vmdk_convert_to_remote(shm->path1, &shm->spec, shm->start_sector, shm->end_sector);
    } else if (strcmp(shm->fname, VMDK_CONVERT_FROM_REMOTE_CALL) == 0) {
        shm->ret_int = vmdk_convert_from_remote(&shm->spec, shm->path1, shm->start_sector, shm->end_sector);
    } else if (strcmp(shm->fname, VMDK_CLONE_CALL) == 0) {
        shm->ret_int = vmdk_clone(shm->path1, &shm->spec);
    } else if (strcmp(shm->fname, VMDK_CONVERT_LOCAL_CALL) == 0) {
        shm->ret_int = vmdk_convert_local(shm->path1, shm->path2, shm->flag);
    } else {
        LOGERROR("%s: unknown function call %s\n", argv[0], shm->fname);
        ret = EUCA_ERROR;
    }

    shmdt(((void *)shm));
    return ret;
}
#endif /* VMDK_CALLER */

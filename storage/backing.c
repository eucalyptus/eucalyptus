// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//!
//! @file storage/backing.c
//! Implements the backing store
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <fcntl.h>
#include <errno.h>
#include <limits.h>
#include <assert.h>
#include <dirent.h>

#include <eucalyptus.h>
#include <misc.h>                      // logprintfl, ensure_...
#include <data.h>                      // ncInstance
#include <handlers.h>                  // nc_state
#include <ipc.h>                       // sem
#include <euca_string.h>

#include "diskutil.h"
#include "blobstore.h"
#include "walrus.h"
#include "storage-windows.h"
#include "backing.h"
#include "iscsi.h"
#include "vbr.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define CACHE_TIMEOUT_USEC                       (1000000LL * 60 * 60 * 2)
#define STORE_TIMEOUT_USEC                       (1000000LL * 60 * 2)
#define DELETE_TIMEOUT_USEC                      (1000000LL * 10)
#define FIND_TIMEOUT_USEC                        (50000LL)  //! @TODO use 1000LL or less to induce rare timeouts

#define INSTANCE_FILE_NAME                       "instance.xml"
#define INSTANCE_LIBVIRT_FILE_NAME               "instance-libvirt.xml"
#define INSTANCE_CONSOLE_FILE_NAME               "console.log"

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

extern struct nc_state_t nc_state;

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

static char instances_path[MAX_PATH] = "";
static blobstore *cache_bs = NULL;
static blobstore *work_bs = NULL;
static sem *disk_sem = NULL;

static bunchOfInstances **instances = NULL;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int check_backing_store(bunchOfInstances ** global_instances);
int stat_backing_store(const char *conf_instances_path, blobstore_meta * work_meta, blobstore_meta * cache_meta);
int init_backing_store(const char *conf_instances_path, unsigned int conf_work_size_mb, unsigned int conf_cache_size_mb);
int save_instance_struct(const ncInstance * instance);
ncInstance *load_instance_struct(const char *instanceId);
int create_instance_backing(ncInstance * instance);
int clone_bundling_backing(ncInstance * instance, const char *filePrefix, char *blockPath);
int destroy_instance_backing(ncInstance * instance, boolean do_destroy_files);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static void bs_errors(const char *msg);
static int stat_blobstore(const char *conf_instances_path, const char *name, blobstore_meta * meta);
static void set_id(const ncInstance * instance, virtualBootRecord * vbr, char *id, unsigned int id_size);
static void set_id2(const ncInstance * instance, const char *suffix, char *id, unsigned int id_size);
static void set_path(char *path, unsigned int path_size, const ncInstance * instance, const char *filename);
static int stale_blob_examiner(const blockblob * bb);

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
//! Logs a blobstore error message at "EUCATRACE" level.
//!
//! @param[in] msg the error message to log
//!
//! @see logprintfl
//!
//! @note We normally do not care to print all messages from blobstore as many are errors that we can handle.
//!
static void bs_errors(const char *msg)
{
    // we normally do not care to print all messages from blobstore as many are errors that we can handle
    LOGTRACE("blobstore: %s", msg);
}

//!
//! Stats the blobstore (name) created under the given path.
//!
//! @param[in]  conf_instances_path path to where the instances information are stored
//! @param[in]  name name of the blobstore
//! @param[out] meta the blobstore metadata to retrieve
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ACCESS_ERROR if we fail to access the requested blobstore
//!
//! @pre The conf_instance_path, name and meta fields must not be NULL.
//!
//! @post On success the meta fields have been updated
//!
static int stat_blobstore(const char *conf_instances_path, const char *name, blobstore_meta * meta)
{
    char path[MAX_PATH] = { 0 };
    blobstore *bs = NULL;

    bzero(meta, sizeof(blobstore_meta));
    snprintf(path, sizeof(path), "%s/%s", conf_instances_path, name);

    // stat the file system and return those numbers even if blobstore does not exist
    if (statfs_path(path, &(meta->fs_bytes_size), &(meta->fs_bytes_available), &(meta->fs_id)) != EUCA_OK) {
        return (EUCA_ACCESS_ERROR);
    }
    // get the size and params of the blobstore, if it exists (do not create)
    if ((bs = blobstore_open(path, 0, 0, BLOBSTORE_FORMAT_ANY, BLOBSTORE_REVOCATION_ANY, BLOBSTORE_SNAPSHOT_ANY)) == NULL)
        return (EUCA_OK);

    // stat and close our blobstore
    blobstore_stat(bs, meta);
    BLOBSTORE_CLOSE(bs);
    return (EUCA_OK);
}

//!
//! Sets our global instances list and checks the integrity of our work and
//! cache blobstore.
//!
//! @param[in] global_instances a pointer to our list of instances.
//!
//! @return EUCA_OK on success or EUCA_ERROR if any error occured.
//!
//! @see blobstore_fsck()
//!
//! @pre  The global_instances should not be NULL.
//!
//! @post The global instances variable will be set to global_instances regardless of the
//!       outcome of this function. If any error occured while checking the integrity of
//!       the work blobstore, the cache blobstore will be closed.
//!
int check_backing_store(bunchOfInstances ** global_instances)
{
    instances = global_instances;

    if (work_bs) {
        if (blobstore_fsck(work_bs, stale_blob_examiner)) {
            LOGERROR("work directory failed integrity check: %s\n", blobstore_get_error_str(blobstore_get_error()));
            //! @todo CHUCK -> Ok to close cache_bs and not set to NULL???
            BLOBSTORE_CLOSE(cache_bs);
            return (EUCA_ERROR);
        }
    }

    if (cache_bs) {
        if (blobstore_fsck(cache_bs, NULL)) {
            //! @TODO verify checksums?
            LOGERROR("cache failed integrity check: %s\n", blobstore_get_error_str(blobstore_get_error()));
            return (EUCA_ERROR);
        }
    }
    return (EUCA_OK);
}

//!
//! Stats the backing blobstores (work and cache) created under the given path.
//!
//! @param[in]  conf_instances_path path to where the instances information are stored
//! @param[out] work_meta pointer to the work blobstore metadata
//! @param[out] cache_meta pointer to the cache blobstore metadata
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @pre The work_meta and cache_meta fiels should now be NULL.
//!
//! @post On success, both work_meta and cache_meta have been updated
//!
int stat_backing_store(const char *conf_instances_path, blobstore_meta * work_meta, blobstore_meta * cache_meta)
{
    const char *path = conf_instances_path;

    // If path wasn't provided, use the one we were configured with
    if (path == NULL) {
        if (strlen(instances_path) < 1) {
            return (EUCA_ERROR);
        }
        path = instances_path;
    }

    return (stat_blobstore(path, "work", work_meta) || stat_blobstore(path, "cache", cache_meta) ? EUCA_ERROR : EUCA_OK);
}

//!
//! Initialize the backing store. Called during initialization of node controller.
//!
//! @param[in] conf_instances_path path to where the instances information are stored
//! @param[in] conf_work_size_mb the work blobstore size limit in MB (if 0 then unlimitted)
//! @param[in] conf_cache_size_mb the cache blobstore size limit in MB (if 0 then cache isn't used)
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_ACCESS_ERROR: if we fail to access our cache and work directories
//!         \li EUCA_PERMISSION_ERROR: if we fail to create the cache or work stores.
//!
//! @pre The conf_instances_path field must not be NULL
//!
//! @post On success, the backing store module is initialized and the following happened:
//!       \li our global instance_path variable is set with the given conf_instance_path
//!       \li the work blobstore is created and our global work_bs variable is set
//!       \li the cache blobstore is created if necessary and the cache_bs variable is set
//!       \li the disk semaphore is created if necessary
//!
int init_backing_store(const char *conf_instances_path, unsigned int conf_work_size_mb, unsigned int conf_cache_size_mb)
{
    char cache_path[MAX_PATH] = { 0 };
    char work_path[MAX_PATH] = { 0 };
    unsigned long long cache_limit_blocks = 0;
    unsigned long long work_limit_blocks = 0;
    blobstore_snapshot_t snapshot_policy = BLOBSTORE_SNAPSHOT_ANY;

    LOGINFO("initializing backing store...\n");

    // Make sure we have a valid intance path passed to us
    if (conf_instances_path == NULL) {
        LOGERROR("INSTANCE_PATH not specified\n");
        return (EUCA_INVALID_ERROR);
    }
    // Set our global instance_path variable with the content of conf_instance_path
    euca_strncpy(instances_path, conf_instances_path, sizeof(instances_path));
    if (check_directory(instances_path)) {
        LOGERROR("INSTANCE_PATH (%s) does not exist!\n", instances_path);
        return (EUCA_ACCESS_ERROR);
    }
    // Check if our cache path exist. If not it should get crated
    snprintf(cache_path, sizeof(cache_path), "%s/cache", instances_path);
    if (ensure_directories_exist(cache_path, 0, NULL, NULL, BACKING_DIRECTORY_PERM) == -1)
        return (EUCA_ACCESS_ERROR);

    // Check if our work path exist. If not it should get crated
    snprintf(work_path, sizeof(work_path), "%s/work", instances_path);
    if (ensure_directories_exist(work_path, 0, NULL, NULL, BACKING_DIRECTORY_PERM) == -1)
        return (EUCA_ACCESS_ERROR);

    // convert MB to blocks
    cache_limit_blocks = conf_cache_size_mb * 2048;
    work_limit_blocks = conf_work_size_mb * 2048;

    // we take 0 as unlimited
    if (work_limit_blocks == 0) {
        work_limit_blocks = ULLONG_MAX;
    }
    // by default we let blobstore pick the snapshot policy, which
    // will use device mapper if available, which is faster than copying
    snapshot_policy = BLOBSTORE_SNAPSHOT_ANY;
    if (nc_state.disable_snapshots) {
        LOGINFO("if allocating storage, will avoid using snapshots\n");
        snapshot_policy = BLOBSTORE_SNAPSHOT_NONE;
    }
    // Set the backing store error callback function
    blobstore_set_error_function(&bs_errors);

    // Do we need to create a cache blobstore
    if (cache_limit_blocks) {
        cache_bs = blobstore_open(cache_path, cache_limit_blocks, BLOBSTORE_FLAG_CREAT, BLOBSTORE_FORMAT_DIRECTORY, BLOBSTORE_REVOCATION_LRU, snapshot_policy);
        if (cache_bs == NULL) {
            LOGERROR("failed to open/create cache blobstore: %s\n", blobstore_get_error_str(blobstore_get_error()));
            return (EUCA_PERMISSION_ERROR);
        }
    }
    // Lets open the work blobstore
    work_bs = blobstore_open(work_path, work_limit_blocks, BLOBSTORE_FLAG_CREAT, BLOBSTORE_FORMAT_FILES, BLOBSTORE_REVOCATION_NONE, snapshot_policy);
    if (work_bs == NULL) {
        LOGERROR("failed to open/create work blobstore: %s\n", blobstore_get_error_str(blobstore_get_error()));
        LOGERROR("%s\n", blobstore_get_last_trace());
        BLOBSTORE_CLOSE(cache_bs);
        return (EUCA_PERMISSION_ERROR);
    }
    // set the initial value of the semaphore to the number of 
    // disk-intensive operations that can run in parallel on this node
    if (nc_state.concurrent_disk_ops && ((disk_sem = sem_alloc(nc_state.concurrent_disk_ops, "mutex")) == NULL)) {
        LOGERROR("failed to create and initialize disk semaphore\n");
        return (EUCA_PERMISSION_ERROR);
    }

    return (EUCA_OK);
}

//!
//! sets id to:
//! \li the blockblob ID of an instance-directory blob (if vbr!=NULL): userId/instanceId/blob-....
//! \li the work prefix within work blobstore for an instance: userId/instanceId
//!
//! @param[in]  instance pointer to the instance to retrieve the information from
//! @param[in]  vbr pointer to the virtual boot record
//! @param[out] id the identifier string to build
//! @param[in]  id_size the size of the given id string
//!
//! @pre \li The instance and id fields must not be NULL.
//!      \li The instance's userId and instanceId fields length must not be set to 0.
//!      \li If vbr is not NULL, then vbr's typeName field length must not be 0
//!
//! @post The id variable is set or the application will ABORT if anything went wrong
//!
//! @todo Remove this
//!
static void set_id(const ncInstance * instance, virtualBootRecord * vbr, char *id, unsigned int id_size)
{
    char suffix[1024] = { 0 };

    assert(id);
    assert(instance);
    assert(strlen(instance->userId));
    assert(strlen(instance->instanceId));

    if (vbr) {
        assert(strlen(vbr->typeName));
        snprintf(id, id_size, "/blob-%s-%s", vbr->typeName, (vbr->type == NC_RESOURCE_KERNEL || vbr->type == NC_RESOURCE_RAMDISK) ? (vbr->id) : (vbr->guestDeviceName));
    }
    snprintf(id, id_size, "%s/%s%s", instance->userId, instance->instanceId, suffix);
}

//!
//! sets id to:
//! \li the work prefix within work blobstore for an instance: userId/instanceId(suffix)
//!
//! @param[in]  instance pointer to the instance to retrieve the information from
//! @param[in]  suffix the optional suffix string
//! @param[out] id the identifier string to build
//! @param[in]  id_size the size of the given id string
//!
//! @pre  \li The instance and id fields must not be NULL.
//!       \li The instance's userId and instanceId fields length must not be 0.
//!
//! @post The id variable is set or the application will ABORT if anything went wrong
//!
static void set_id2(const ncInstance * instance, const char *suffix, char *id, unsigned int id_size)
{
    assert(id);
    assert(instance);
    assert(strlen(instance->userId));
    assert(strlen(instance->instanceId));
    snprintf(id, id_size, "%s/%s%s", instance->userId, instance->instanceId, (suffix) ? (suffix) : (""));
}

//!
//! sets path to
//! \li the path of a file in an instance directory (if filename!=NULL)
//! \li the path of the instance directory (if instance!=NULL)
//! \li the path where all instance directories are kept
//!
//! @param[out] path the path we're trying to build
//! @param[in]  path_size the size of the given path string
//! @param[in]  instance pointer to the instance to retrieve information from
//! @param[in]  filename the optional filename to append to the path
//!
//! @see set_id()
//!
//! @pre \li The length of the global instance_path field must not be 0.
//!      \li if instance is not NULL, the length of its userId and instanceId fields must not be 0.
//!
//! @post The path value is set appropriately depending on the given instance and filename value
//!       or the application will ABORT if anything went wrong
//!
//! @note This function must be kept consistent with set_id().
//!
static void set_path(char *path, unsigned int path_size, const ncInstance * instance, const char *filename)
{
    char buf[1024] = { 0 };

    assert(strlen(instances_path));
    if (instance) {
        assert(strlen(instance->userId));
        assert(strlen(instance->instanceId));
        set_id(instance, NULL, buf, sizeof(buf));
        if (filename) {
            snprintf(path, path_size, "%s/work/%s/%s", instances_path, buf, filename);
        } else {
            snprintf(path, path_size, "%s/work/%s", instances_path, buf);
        }
    } else {
        snprintf(path, path_size, "%s/work", instances_path);
    }
}

//!
//! Callback used when checking for the integrity of the work blobstore.
//!
//! @param[in] bb pointer to the blockblob to examine
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @see check_backing_store()
//! @see blobstore_fsck()
//!
//! @pre The bb field must not be NULL.
//!
static int stale_blob_examiner(const blockblob * bb)
{
#define DEL_FILE(_filename)                                                                           \
{                                                                                                     \
	snprintf(path, sizeof(path), "%s/work/%s/%s/%s", instances_path, user_id, inst_id, (_filename));  \
	unlink(path);                                                                                     \
}

    char *s = NULL;
    char *user_id = NULL;
    char *inst_id = NULL;
    char *file = NULL;
    char path[MAX_PATH] = { 0 };
    char work_path[MAX_PATH] = { 0 };
    int work_path_len = 0;
    ncInstance *instance = NULL;

    set_path(work_path, sizeof(work_path), NULL, NULL);
    work_path_len = strlen(work_path);
    assert(work_path_len > 0);

    s = strstr(bb->blocks_path, work_path);
    if ((s == NULL) || (s != bb->blocks_path)) {
        // blob not under work blobstore path
        return (EUCA_OK);
    }
    // parse the path past the work directory base
    euca_strncpy(work_path, bb->blocks_path, sizeof(work_path));
    s = work_path + work_path_len + 1;
    user_id = strtok(s, "/");
    inst_id = strtok(NULL, "/");
    file = strtok(NULL, "/");

    if ((instance = find_instance(instances, inst_id)) == NULL) {
        // not found among running instances => stale
        // while we're here, try to delete extra files that aren't managed by the blobstore
        //! @TODO ensure we catch any other files - perhaps by performing this cleanup after all blobs are deleted
        DEL_FILE(INSTANCE_FILE_NAME);
        DEL_FILE(INSTANCE_LIBVIRT_FILE_NAME);
        DEL_FILE(INSTANCE_CONSOLE_FILE_NAME);
        DEL_FILE("instance.checkpoint");
        return (EUCA_ERROR);
    }

    return (EUCA_OK);

#undef DEL_FILE
}

//!
//! Save the instance structure data in the instance.checkpoint file under the instance's
//! work blobstore path.
//!
//! @param[in] instance pointer to the instance to save
//!
//! @return EUCA_OK on success of the following error codes:
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_PERMISSION_ERROR: if we fail to open/create the checkpoint file
//!         \li EUCA_IO_ERROR: if we fail to save the instance in the checkpoint file
//!
//! @pre The instance variable must not be NULL.
//!
//! @post On success, the checkpoint file is created and contains the instance information
//!
int save_instance_struct(const ncInstance * instance)
{
    int fd = 0;
    char checkpoint_path[MAX_PATH] = { 0 };

    // Make sure the given instance is valid
    if (instance == NULL) {
        LOGERROR("internal error (NULL instance in save_instance_struct)\n");
        return (EUCA_INVALID_ERROR);
    }
    // Figure out our path to the checkpoint file
    set_path(checkpoint_path, sizeof(checkpoint_path), instance, "instance.checkpoint");

    // Create and open our checkpoint file
    if ((fd = open(checkpoint_path, O_CREAT | O_WRONLY, BACKING_FILE_PERM)) < 0) {
        LOGDEBUG("[%s] save_instance_struct: failed to create instance checkpoint at %s\n", instance->instanceId, checkpoint_path);
        return (EUCA_PERMISSION_ERROR);
    }
    // Store our instance in the file entirely.
    if (write(fd, ((char *)instance), sizeof(struct ncInstance_t)) != sizeof(struct ncInstance_t)) {
        LOGDEBUG("[%s] save_instance_struct: failed to write instance checkpoint at %s\n", instance->instanceId, checkpoint_path);
        close(fd);
        //! @TODO: unlink the file here?
        return (EUCA_IO_ERROR);
    }

    close(fd);
    return (EUCA_OK);
}

//!
//! Loads an instance structure data from the instance.checkpoint file under the instance's
//! work blobstore path.
//!
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return A pointer to the instance structure if successful or otherwise NULL.
//!
//! @pre The instanceId parameter must not be NULL.
//!
//! @post On success, a newly allocated pointer to the instance is returned where the stateCode
//!       is set to NO_STATE.
//!
ncInstance *load_instance_struct(const char *instanceId)
{
    int fd;
    DIR *insts_dir = NULL;
    char tmp_path[MAX_PATH] = { 0 };
    char user_paths[MAX_PATH] = { 0 };
    char checkpoint_path[MAX_PATH] = { 0 };
    ncInstance *instance = NULL;
    struct dirent *dir_entry = NULL;
    struct stat mystat = { 0 };
    const int meta_size = sizeof(struct ncInstance_t);

    // Allocate memory for our instance
    if ((instance = EUCA_ZALLOC(1, meta_size)) == NULL) {
        LOGERROR("out of memory (for instance struct)\n");
        return (NULL);
    }
    // We know the instance indentifier
    euca_strncpy(instance->instanceId, instanceId, sizeof(instance->instanceId));

    // we don't know userId, so we'll look for instanceId in every user's
    // directory (we're assuming that instanceIds are unique in the system)
    set_path(user_paths, sizeof(user_paths), NULL, NULL);
    if ((insts_dir = opendir(user_paths)) == NULL) {
        LOGERROR("failed to open %s\n", user_paths);
        goto free;
    }
    // Scan every path under the user path for one that conaints our instance
    while ((dir_entry = readdir(insts_dir)) != NULL) {
        snprintf(tmp_path, sizeof(tmp_path), "%s/%s/%s", user_paths, dir_entry->d_name, instance->instanceId);
        if (stat(tmp_path, &mystat) == 0) {
            // found it. Now save our user identifier
            euca_strncpy(instance->userId, dir_entry->d_name, sizeof(instance->userId));
            break;
        }
    }

    // Done with the directory
    closedir(insts_dir);
    insts_dir = NULL;

    // Did we really find one?
    if (strlen(instance->userId) < 1) {
        LOGERROR("didn't find instance %s\n", instance->instanceId);
        goto free;
    }
    // Now open our checkpoint file and load it up
    set_path(checkpoint_path, sizeof(checkpoint_path), instance, "instance.checkpoint");
    if (((fd = open(checkpoint_path, O_RDONLY)) < 0) || (read(fd, instance, meta_size) < meta_size)) {
        LOGERROR("failed to load metadata for %s from %s: %s\n", instance->instanceId, checkpoint_path, strerror(errno));
        if (fd >= 0)
            close(fd);
        goto free;
    }
    // Done with the file
    close(fd);

    // Reset some fields for safety since they would now be wrong
    instance->stateCode = NO_STATE;
    instance->params.root = NULL;
    instance->params.kernel = NULL;
    instance->params.ramdisk = NULL;
    instance->params.swap = NULL;
    instance->params.ephemeral0 = NULL;

    // fix up the pointers
    vbr_parse(&(instance->params), NULL);
    return (instance);

free:
    EUCA_FREE(instance);
    return (NULL);
}

//!
//! Implement the backing store for a given instance
//!
//! @param[in] instance pointer to the instance
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre The instance parameter must not be NULL.
//!
//! @post
//!
int create_instance_backing(ncInstance * instance)
{
    int rc = 0;
    int ret = EUCA_ERROR;
    virtualMachine *vm = &(instance->params);
    artifact *sentinel = NULL;
    char work_prefix[1024] = { 0 };    // {userId}/{instanceId}

    // ensure instance directory exists
    set_path(instance->instancePath, sizeof(instance->instancePath), instance, NULL);
    if (ensure_directories_exist(instance->instancePath, 0, NULL, "root", BACKING_DIRECTORY_PERM) == -1)
        goto out;

    // set various instance-directory-relative paths in the instance struct
    set_path(instance->xmlFilePath, sizeof(instance->xmlFilePath), instance, INSTANCE_FILE_NAME);
    set_path(instance->libvirtFilePath, sizeof(instance->libvirtFilePath), instance, INSTANCE_LIBVIRT_FILE_NAME);
    set_path(instance->consoleFilePath, sizeof(instance->consoleFilePath), instance, INSTANCE_CONSOLE_FILE_NAME);
    if (strstr(instance->platform, "windows")) {
        // generate the floppy file for windows instances
        if (makeWindowsFloppy(nc_state.home, instance->instancePath, instance->keyName, instance->instanceId)) {
            LOGERROR("[%s] could not create windows bootup script floppy\n", instance->instanceId);
            goto out;
        } else {
            set_path(instance->floppyFilePath, sizeof(instance->floppyFilePath), instance, "floppy");
        }
    }

    set_id(instance, NULL, work_prefix, sizeof(work_prefix));

    // compute tree of dependencies
    sentinel = vbr_alloc_tree(vm,      // the struct containing the VBR
                              FALSE,   // for Xen and KVM we do not need to make disk bootable
                              TRUE,    // make working copy of runtime-modifiable files
                              (instance->do_inject_key) ? (instance->keyName) : (NULL), // the SSH key
                              instance->instanceId);    // ID is for logging
    if (sentinel == NULL) {
        LOGERROR("[%s] failed to prepare backing for instance\n", instance->instanceId);
        goto out;
    }

    sem_p(disk_sem);
    {
        // download/create/combine the dependencies
        rc = art_implement_tree(sentinel, work_bs, cache_bs, work_prefix, INSTANCE_PREP_TIMEOUT_USEC);
    }
    sem_v(disk_sem);

    if (rc != EUCA_OK) {
        LOGERROR("[%s] failed to implement backing for instance\n", instance->instanceId);
        goto out;
    }

    if (save_instance_struct(instance)) // update instance checkpoint now that the struct got updated
        goto out;

    ret = EUCA_OK;

out:
    if (sentinel)
        art_free(sentinel);
    return (ret);
}

//!
//! Implement the backing store for a the target of migration
//!
//! @param[in] instance pointer to the instance
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre The instance parameter must not be NULL.
//!
//! @post
//!
int create_migration_backing(ncInstance * instance)
{
    int rc = 0;
    int ret = EUCA_ERROR;
    virtualMachine *vm = &(instance->params);
    artifact *sentinel = NULL;
    char work_prefix[1024] = { 0 }; // {userId}/{instanceId}

    // ensure instance directory exists
    set_path(instance->instancePath, sizeof(instance->instancePath), instance, NULL);
    if (ensure_directories_exist(instance->instancePath, 0, NULL, "root", BACKING_DIRECTORY_PERM) == -1)
        goto out;

    // set various instance-directory-relative paths in the instance struct
    set_path(instance->xmlFilePath, sizeof(instance->xmlFilePath), instance, INSTANCE_FILE_NAME);
    set_path(instance->libvirtFilePath, sizeof(instance->libvirtFilePath), instance, INSTANCE_LIBVIRT_FILE_NAME);
    set_path(instance->consoleFilePath, sizeof(instance->consoleFilePath), instance, INSTANCE_CONSOLE_FILE_NAME);
    if (strstr(instance->platform, "windows")) {
        // generate the floppy file for windows instances
        if (makeWindowsFloppy(nc_state.home, instance->instancePath, instance->keyName, instance->instanceId)) {
            logprintfl(EUCAERROR, "[%s] could not create windows bootup script floppy\n", instance->instanceId);
            goto out;
        } else {
            set_path(instance->floppyFilePath, sizeof(instance->floppyFilePath), instance, "floppy");
        }
    }

    set_id(instance, NULL, work_prefix, sizeof(work_prefix));

    // compute tree of dependencies
    sentinel = vbr_alloc_tree(vm,   // the struct containing the VBR
                              FALSE,    // for Xen and KVM we do not need to make disk bootable
                              TRUE, // make working copy of runtime-modifiable files
                              (instance->do_inject_key) ? (instance->keyName) : (NULL), // the SSH key
                              instance->instanceId);    // ID is for logging
    if (sentinel == NULL) {
        logprintfl(EUCAERROR, "[%s] failed to prepare backing for instance\n", instance->instanceId);
        goto out;
    }

    // convert top-level artifacts into simple blobs and prune children
    for (int i = 0; i < MAX_ARTIFACT_DEPS && sentinel->deps[i]; i++) {
        // TODO:....
    }

    sem_p(disk_sem);
    {
        // download/create/combine the dependencies
        rc = art_implement_tree(sentinel, work_bs, cache_bs, work_prefix, INSTANCE_PREP_TIMEOUT_USEC);
    }
    sem_v(disk_sem);

    if (rc != EUCA_OK) {
        logprintfl(EUCAERROR, "[%s] failed to implement migration backing for instance\n", instance->instanceId);
        goto out;
    }

    if (save_instance_struct(instance)) // update instance checkpoint now that the struct got updated
        goto out;

    ret = EUCA_OK;

out:
    if (sentinel)
        art_free(sentinel);
    return (ret);
}

//!
//! Do a clone of an instance backing store to another location.
//!
//! @param[in] instance pointer to the instance
//! @param[in] filePrefix the
//! @param[in] blockPath the path where we need to clone
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_IO_ERROR: if we fail to copy the data.
//!         \li EUCA_NOT_FOUND_ERROR: if we cannot find the blobstore to clone.
//!         \li EUCA_PERMISSION_ERROR: if we fail to create/open the destination blobstore
//!
//! @pre \li The instance parameter must not be NULL.
//!      \li The filePrefix and blockPath parameters must not be NULL and should be valid strings.
//!
//! @note
//!
int clone_bundling_backing(ncInstance * instance, const char *filePrefix, char *blockPath)
{
    int ret = EUCA_OK;
    int found = -1;
    char path[MAX_PATH] = { 0 };
    char work_regex[1024] = { 0 };
    char id[BLOBSTORE_MAX_PATH] = { 0 };
    char workPath[BLOBSTORE_MAX_PATH] = { 0 };
    blockblob *src_blob = NULL;
    blockblob *dest_blob = NULL;
    blockblob *bb = NULL;
    blockblob_meta *bm = NULL;
    blockblob_meta *next = NULL;
    blockblob_meta *matches = NULL;

    set_path(path, sizeof(path), instance, NULL);
    set_id2(instance, "/.*", work_regex, sizeof(work_regex));

    if ((found = blobstore_search(work_bs, work_regex, &matches) <= 0)) {
        LOGERROR("[%s] failed to find blob in %s %d\n", instance->instanceId, path, found);
        return (EUCA_NOT_FOUND_ERROR);
    }

    for (bm = matches; bm; bm = bm->next) {
        bb = blockblob_open(work_bs, bm->id, 0, 0, NULL, FIND_TIMEOUT_USEC);
        if ((bb != NULL) && (bb->snapshot_type == BLOBSTORE_SNAPSHOT_DM) && (strstr(bb->blocks_path, "emi-") != NULL)) {
            // root image contains substr 'emi-'
            src_blob = bb;
            break;
        } else if (bb != NULL) {
            blockblob_close(bb);
        }
    }

    if (!src_blob) {
        LOGERROR("[%s] couldn't find the blob to clone from", instance->instanceId);
        ret = EUCA_NOT_FOUND_ERROR;
        goto error;
    }

    set_id(instance, NULL, workPath, sizeof(workPath));
    snprintf(id, sizeof(id), "%s/%s", workPath, filePrefix);

    // open destination blob 
    dest_blob = blockblob_open(work_bs, id, src_blob->size_bytes, BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL, NULL, FIND_TIMEOUT_USEC);
    if (!dest_blob) {
        LOGERROR("[%s] couldn't create the destination blob for bundling (%s)", instance->instanceId, id);
        ret = EUCA_PERMISSION_ERROR;
        goto error;
    }

    if (strlen(dest_blob->blocks_path) > 0)
        snprintf(blockPath, MAX_PATH, "%s", dest_blob->blocks_path);

    // copy blob (will 'dd' eventually)
    if (blockblob_copy(src_blob, 0, dest_blob, 0, src_blob->size_bytes) != EUCA_OK) {
        LOGERROR("[%s] couldn't copy block blob for bundling (%s)", instance->instanceId, id);
        ret = EUCA_IO_ERROR;
        goto error;
    }

error:
    // free the search results
    for (bm = matches; bm; bm = next) {
        next = bm->next;
        EUCA_FREE(bm);
    }

    // Close our blockblobs
    BLOCKBLOB_CLOSE(src_blob);
    BLOCKBLOB_CLOSE(dest_blob);
    return (ret);
}

//!
//! Destroy the backing store for a given instance
//!
//! @param[in] instance the instance identifier for which we will destroy the backing store
//! @param[in] do_destroy_files set to TRUE if we need to destroy the content otherwise set to FALSE.
//!
//! @return EUCA_OK on success
//!
//! @pre The instance parameter must not be NULL and do_destroy_files must be a valid boolean.
//!
//! @note
//!
int destroy_instance_backing(ncInstance * instance, boolean do_destroy_files)
{
    int i = 0;
    int n = 0;
    int ret = EUCA_OK;
    char toDelete[MAX_PATH] = { 0 };
    char path[MAX_PATH] = { 0 };
    char work_regex[1024] = { 0 };     // {userId}/{instanceId}/.*
    struct dirent *entry = NULL;
    struct dirent **files = NULL;
    ncVolume *volume = NULL;
    virtualMachine *vm = &(instance->params);
    virtualBootRecord *vbr = NULL;

    // find and detach iSCSI targets, if any
    for (i = 0; ((i < EUCA_MAX_VBRS) && (i < vm->virtualBootRecordLen)); i++) {
        vbr = &(vm->virtualBootRecord[i]);
        if (vbr->locationType == NC_LOCATION_IQN) {
            if (disconnect_iscsi_target(vbr->resourceLocation)) {
                LOGERROR("[%s] failed to disconnect iSCSI target attached to %s\n", instance->instanceId, vbr->backingPath);
            }
        }
    }

    // see if instance directory is there (sometimes startup fails before it is created)
    set_path(path, sizeof(path), instance, NULL);
    if (check_path(path))
        return (ret);

    // to ensure that we are able to delete all blobs, we chown files back to 'eucalyptus'
    // (e.g., libvirt on KVM on Maverick chowns them to libvirt-qemu while
    // VM is running and then chowns them to root after termination)
    set_path(path, sizeof(path), instance, "*");
    if (diskutil_ch(path, EUCALYPTUS_ADMIN, NULL, BACKING_FILE_PERM)) {
        LOGWARN("[%s] failed to chown files before cleanup\n", instance->instanceId);
    }

    if (do_destroy_files) {
        set_id2(instance, "/.*", work_regex, sizeof(work_regex));

        if (blobstore_delete_regex(work_bs, work_regex) == -1) {
            LOGERROR("[%s] failed to remove some artifacts in %s\n", instance->instanceId, path);
        }
        // remove the known leftover files
        unlink(instance->xmlFilePath);
        unlink(instance->libvirtFilePath);
        unlink(instance->consoleFilePath);
        if (strlen(instance->floppyFilePath)) {
            unlink(instance->floppyFilePath);
        }

        set_path(path, sizeof(path), instance, "instance.checkpoint");
        unlink(path);
        for (i = 0; i < EUCA_MAX_VOLUMES; ++i) {
            volume = &instance->volumes[i];
            snprintf(path, sizeof(path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volume->volumeId);
            unlink(path);
        }

        // bundle instance will leave additional files let's delete every file in the directory
        if ((n = scandir(instance->instancePath, &files, 0, alphasort)) > 0) {
            while (n--) {
                entry = files[n];
                if ((entry != NULL) && (strncmp(entry->d_name, ".", 1) != 0) && (strncmp(entry->d_name, "..", 2) != 0)) {
                    snprintf(toDelete, MAX_PATH, "%s/%s", instance->instancePath, entry->d_name);
                    unlink(toDelete);
                }
                EUCA_FREE(entry);
            }
            EUCA_FREE(files);
        }
    }
    // Finally try to remove the directory. If either the user or our code introduced
    // any new files, this last step will fail.
    set_path(path, sizeof(path), instance, NULL);
    if (rmdir(path) && do_destroy_files) {
        LOGWARN("[%s] failed to remove backing directory %s\n", instance->instanceId, path);
    }

    return (ret);
}

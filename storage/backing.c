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
#include "instance33.h"                // ncInstance as of 3.3.*, for upgrade
#include <handlers.h>                  // nc_state
#include <ipc.h>                       // sem
#include <euca_string.h>

#include "diskutil.h"
#include "blobstore.h"
#include "objectstorage.h"
#include "storage-windows.h"
#include "backing.h"
#include "vbr.h"
#include <ebs_utils.h>
#include "xml.h"

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

static char instances_path[EUCA_MAX_PATH] = "";
static blobstore *cache_bs = NULL;
static blobstore *work_bs = NULL;
static sem *disk_sem = NULL;

static bunchOfInstances **instances = NULL;

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
    char path[EUCA_MAX_PATH] = "";
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
    char cache_path[EUCA_MAX_PATH] = "";
    char work_path[EUCA_MAX_PATH] = "";
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
    cache_limit_blocks = (unsigned long long)conf_cache_size_mb *2048;
    work_limit_blocks = (unsigned long long)conf_work_size_mb *2048;

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
    if (nc_state.concurrent_disk_ops && ((disk_sem = sem_alloc(nc_state.concurrent_disk_ops, IPC_MUTEX_SEMAPHORE)) == NULL)) {
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
//! Set various instance-directory-relative paths in the instance struct
//!
//! @param[in] instance pointer to the instance struct to modify
//!
inline static void set_instance_paths(ncInstance * instance)
{
    set_path(instance->instancePath, sizeof(instance->instancePath), instance, NULL);
    set_path(instance->xmlFilePath, sizeof(instance->xmlFilePath), instance, INSTANCE_FILE_NAME);
    set_path(instance->libvirtFilePath, sizeof(instance->libvirtFilePath), instance, INSTANCE_LIBVIRT_FILE_NAME);
    set_path(instance->consoleFilePath, sizeof(instance->consoleFilePath), instance, INSTANCE_CONSOLE_FILE_NAME);
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
    char *s = NULL;
    char *user_id = NULL;
    char *inst_id = NULL;
    char *file = NULL;
    char path[EUCA_MAX_PATH] = "";
    char work_path[EUCA_MAX_PATH] = "";
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

    if (((instance = find_instance(instances, inst_id)) == NULL)    // not found among instances => stale
        || instance->state == TEARDOWN) {   // found among instances, but is already marked as terminated

        // if this instance is not among those we know about,
        // load it into memory and report it in Teardown state
        //! @TODO technically, disk state for this instance is not gone,
        //! but it soon will be, once this examiner function returns error
        if ((instance == NULL) && ((instance = load_instance_struct(inst_id)) != NULL)) {
            LOGINFO("marking non-running instance %s as terminated\n", inst_id);
            instance->terminationTime = time(NULL); // set time to now, so record won't get expired immediately
            change_state(instance, TEARDOWN);
            int err = add_instance(instances, instance);    // we are not using locks because we assume the caller does
            if (err) {
                free_instance(&instance);
            }
        }
        // while we're here, try to delete extra files that aren't managed by the blobstore
        snprintf(path, sizeof(path), "%s/work/%s/%s", instances_path, user_id, inst_id);
        blobstore_delete_nonblobs(bb->store, path);
        return (EUCA_ERROR);
    }

    return (EUCA_OK);
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
    return gen_instance_xml(instance);
}

//!
//! Loads an instance structure data from the instance.xml file under the instance's
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
    DIR *insts_dir = NULL;
    char tmp_path[EUCA_MAX_PATH] = "";
    char user_paths[EUCA_MAX_PATH] = "";
    char checkpoint_path[EUCA_MAX_PATH] = "";
    ncInstance *instance = NULL;
    struct dirent *dir_entry = NULL;
    struct stat mystat = { 0 };

    // Allocate memory for our instance
    if ((instance = EUCA_ZALLOC(1, sizeof(ncInstance))) == NULL) {
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
    // set various instance-directory-relative paths in the instance struct
    set_instance_paths(instance);

    // Check if there is a binary checkpoint file, used by versions up to 3.3,
    // and load metadata from it (as part of a "warm" upgrade from 3.3.0 and 3.3.1).
    set_path(checkpoint_path, sizeof(checkpoint_path), instance, "instance.checkpoint");
    set_path(instance->xmlFilePath, sizeof(instance->xmlFilePath), instance, INSTANCE_FILE_NAME);
    if (check_file(checkpoint_path) == 0) {
        ncInstance33 instance33;
        {                              // read in the checkpoint
            int fd = open(checkpoint_path, O_RDONLY);
            if (fd < 0) {
                LOGERROR("failed to load metadata for %s from %s: %s\n", instance->instanceId, checkpoint_path, strerror(errno));
                goto free;
            }

            size_t meta_size = (size_t) sizeof(ncInstance33);
            assert(meta_size <= SSIZE_MAX); // beyond that read() behavior is unspecified
            ssize_t bytes_read = read(fd, &instance33, meta_size);
            close(fd);
            if (bytes_read < meta_size) {
                LOGERROR("metadata checkpoint for %s (%ld bytes) in %s is too small (< %ld)\n", instance->instanceId, bytes_read, checkpoint_path, meta_size);
                goto free;
            }
        }
        // Convert the 3.3 struct into the current struct.
        // Currently, a copy is sufficient, but if ncInstance
        // ever changes so that its beginning differs from ncInstanc33,
        // we may have to write something more elaborate or to break
        // the ability to upgrade from 3.3. We attempt to detect such a
        // change with the following if-statement, which compares offsets
        // in the structs of the last member in the 3.3 version.
        if (((unsigned long)&(instance->last_stat) - (unsigned long)instance)
            != ((unsigned long)&(instance33.last_stat) - (unsigned long)&instance33)) {
            LOGERROR("BUG: upgrade from v3.3 is not possible due to changes to instance struct\n");
            goto free;
        }
        memcpy(instance, &instance33, sizeof(ncInstance33));
        LOGINFO("[%s] upgraded instance checkpoint from v3.3\n", instance->instanceId);
    } else {                           // no binary checkpoint, so we expect an XML-formatted checkpoint
        if (read_instance_xml(instance->xmlFilePath, instance) != EUCA_OK) {
            LOGERROR("failed to read instance XML\n");
            goto free;
        }
    }

    // Reset some fields for safety since they would now be wrong
    instance->stateCode = NO_STATE;
    instance->params.root = NULL;
    instance->params.kernel = NULL;
    instance->params.ramdisk = NULL;
    instance->params.swap = NULL;
    instance->params.ephemeral0 = NULL;

    // fix up the pointers
    vbr_parse(&(instance->params), NULL);

    // perform any upgrade-related manipulations to bring the struct up to date

    // save the struct back to disk after the upgrade routine had a chance to modify it
    if (gen_instance_xml(instance) != EUCA_OK) {
        LOGERROR("failed to create instance XML in %s\n", instance->xmlFilePath);
        goto free;
    }
    // remove the binary checkpoint because it is no longer needed and not used past 3.3
    unlink(checkpoint_path);

    return (instance);

free:
    EUCA_FREE(instance);
    return (NULL);
}

//!
//! Implement the backing store for a given instance
//!
//! @param[in] instance pointer to the instance
//! @param[in] is_migration_dest
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre The instance parameter must not be NULL.
//!
//! @post
//!
int create_instance_backing(ncInstance * instance, boolean is_migration_dest)
{
    int rc = 0;
    int ret = EUCA_ERROR;
    virtualMachine *vm = &(instance->params);
    artifact *sentinel = NULL;
    char work_prefix[1024] = { 0 };    // {userId}/{instanceId}

    // set various instance-directory-relative paths in the instance struct
    set_instance_paths(instance);

    // ensure instance directory exists
    if (ensure_directories_exist(instance->instancePath, 0, NULL, "root", BACKING_DIRECTORY_PERM) == -1)
        goto out;

    if (strstr(instance->platform, "windows")) {
        // generate the floppy file for windows instances
        if (makeWindowsFloppy(nc_state.home, instance->instancePath, instance->keyName, instance->instanceId)) {
            LOGERROR("[%s] could not create windows bootup script floppy\n", instance->instanceId);
            goto out;
        } else {
            set_path(instance->floppyFilePath, sizeof(instance->floppyFilePath), instance, "floppy");
        }
    } else if (strlen(instance->instancePk) > 0) {  // TODO: credential floppy is limited to Linux instances ATM
        LOGDEBUG("[%s] creating floppy for instance credential\n", instance->instanceId);
        if (make_credential_floppy(nc_state.home, instance)) {
            LOGERROR("[%s] could not create credential floppy\n", instance->instanceId);
            goto out;
        } else {
            set_path(instance->floppyFilePath, sizeof(instance->floppyFilePath), instance, "floppy");
        }
    }

    set_id(instance, NULL, work_prefix, sizeof(work_prefix));

    // if this looks like a partition m1.small image, make it a bootable disk
    virtualMachine *vm2 = NULL;
    LOGDEBUG("vm->virtualBootRecordLen=%d\n", vm->virtualBootRecordLen);
    if (vm->virtualBootRecordLen == 5) {    // TODO: make this check more robust

        // as an experiment, construct a new VBR, without swap and ephemeral
        virtualMachine vm_copy;
        vm2 = &vm_copy;
        memcpy(vm2, vm, sizeof(virtualMachine));
        bzero(vm2->virtualBootRecord, EUCA_MAX_VBRS * sizeof(virtualBootRecord));
        vm2->virtualBootRecordLen = 0;

        virtualBootRecord *emi_vbr = NULL;
        for (int i = 0; i < EUCA_MAX_VBRS && i < vm->virtualBootRecordLen; i++) {
            virtualBootRecord *vbr = &(vm->virtualBootRecord[i]);
            if (vbr->type != NC_RESOURCE_KERNEL && vbr->type != NC_RESOURCE_RAMDISK && vbr->type != NC_RESOURCE_IMAGE)
                continue;
            if (vbr->type == NC_RESOURCE_IMAGE)
                emi_vbr = vbr;
            memcpy(vm2->virtualBootRecord + (vm2->virtualBootRecordLen++), vbr, sizeof(virtualBootRecord));
        }

        if (emi_vbr == NULL) {
            LOGERROR("[%s] failed to find EMI among VBR entries\n", instance->instanceId);
            goto out;
        }

        if (vbr_add_ascii("boot:none:104857600:ext3:sda2:none", vm2) != EUCA_OK) {
            LOGERROR("[%s] could not add a boot partition VBR entry\n", instance->instanceId);
            goto out;
        }
        if (vbr_parse(vm2, NULL) != EUCA_OK) {
            LOGERROR("[%s] could not parse the boot partition VBR entry\n", instance->instanceId);
            goto out;
        }
        // compute tree of dependencies
        sentinel = vbr_alloc_tree(vm2, // the struct containing the VBR
                                  TRUE, // we always make the disk bootable, for consistency
                                  TRUE, // make working copy of runtime-modifiable files
                                  is_migration_dest,    // tree of an instance on the migration destination
                                  (instance->do_inject_key) ? (instance->keyName) : (NULL), // the SSH key
                                  instance->instanceId);    // ID is for logging
        if (sentinel == NULL) {
            LOGERROR("[%s] failed to prepare backing for instance\n", instance->instanceId);
            goto out;
        }

        LOGDEBUG("disk size prior to tree implementation is = %lld\n", sentinel->deps[0]->size_bytes);
        long long right_disk_size = sentinel->deps[0]->size_bytes;

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

        LOGDEBUG("[%s] created the initial bootable disk\n", instance->instanceId);

        /* option A starts */
        assert(emi_vbr);
        assert(sentinel->deps[0]);
        strcpy(emi_vbr->guestDeviceName, "sda");    // switch 'sda1' to 'sda' now that we've built the disk
        //emi_vbr->sizeBytes = sentinel->deps[0]->size_bytes; // update the size to match the disk
        emi_vbr->sizeBytes = right_disk_size;   // this is bad...
        LOGDEBUG("at boot disk creation time emi_vbr->sizeBytes = %lld\n", emi_vbr->sizeBytes);
        euca_strncpy(emi_vbr->id, sentinel->deps[0]->id, SMALL_CHAR_BUFFER_SIZE);   // change to the ID of the disk
        if (vbr_parse(vm, NULL) != EUCA_OK) {
            LOGERROR("[%s] could not parse the boot partition VBR entry\n", instance->instanceId);
            goto out;
        }
        emi_vbr->locationType = NC_LOCATION_NONE;   // i.e., it should already exist

        art_free(sentinel);
        /* option A end */

        /* option B starts *
           memcpy(vm, vm2, sizeof(virtualMachine));
           if (save_instance_struct(instance)) // update instance checkpoint now that the struct got updated
           goto out;
           ret = EUCA_OK;
           goto out;
           * option B ends */
    }
    // compute tree of dependencies
    sentinel = vbr_alloc_tree(vm,      // the struct containing the VBR
                              FALSE,   // if image had to be made bootable, that was done above
                              TRUE,    // make working copy of runtime-modifiable files
                              is_migration_dest,    // tree of an instance on the migration destination
                              (instance->do_inject_key) ? (instance->keyName) : (NULL), // the SSH key
                              instance->instanceId);    // ID is for logging
    if (sentinel == NULL) {
        LOGERROR("[%s] failed to prepare extended backing for instance\n", instance->instanceId);
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
    char path[EUCA_MAX_PATH] = "";
    char work_regex[1024] = "";
    char id[BLOBSTORE_MAX_PATH] = "";
    char workPath[BLOBSTORE_MAX_PATH] = "";
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
    snprintf(id, sizeof(id), "%s/bundle/%s", workPath, filePrefix);

    // open destination blob
    dest_blob = blockblob_open(work_bs, id, src_blob->size_bytes, BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL, NULL, FIND_TIMEOUT_USEC);
    if (!dest_blob) {
        LOGERROR("[%s] couldn't create the destination blob for bundling (%s)", instance->instanceId, id);
        ret = EUCA_PERMISSION_ERROR;
        goto error;
    }

    if (strlen(dest_blob->blocks_path) > 0)
        snprintf(blockPath, EUCA_MAX_PATH, "%s", dest_blob->blocks_path);

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
    int ret = EUCA_OK;
    char path[EUCA_MAX_PATH] = "";
    char work_regex[1024] = "";        // {userId}/{instanceId}/.*
    char scURL[512] = "";
    ncVolume *volume = NULL;
    virtualMachine *vm = &(instance->params);
    virtualBootRecord *vbr = NULL;

    if (get_localhost_sc_url(scURL) != EUCA_OK || strlen(scURL) == 0) {
        LOGWARN("[%s] could not obtain SC URL (is SC enabled?)\n", instance->instanceId);
        scURL[0] = '\0';
    }
    // find and detach iSCSI targets, if any
    for (i = 0; ((i < EUCA_MAX_VBRS) && (i < vm->virtualBootRecordLen)); i++) {
        vbr = &(vm->virtualBootRecord[i]);
        if (vbr->locationType == NC_LOCATION_SC) {
            if (disconnect_ebs_volume
                (scURL, localhost_config.use_ws_sec, localhost_config.ws_sec_policy_file, vbr->resourceLocation, vbr->preparedResourceLocation, localhost_config.ip,
                 localhost_config.iqn) != 0) {
                LOGERROR("[%s] failed to disconnect volume attached to '%s'\n", instance->instanceId, vbr->backingPath);
            }
        }
    }
    // there may be iSCSI targets for volumes if instance disappeared or was migrated
    for (i = 0; i < EUCA_MAX_VOLUMES; ++i) {
        ncVolume *volume = &instance->volumes[i];
        if (!is_volume_used(volume))
            continue;

        if (disconnect_ebs_volume
            (scURL, localhost_config.use_ws_sec, localhost_config.ws_sec_policy_file, volume->attachmentToken, volume->connectionString, localhost_config.ip,
             localhost_config.iqn) != 0) {
            LOGERROR("[%s][%s] failed to disconnect volume\n", instance->instanceId, volume->volumeId);
        }
    }

    // see if instance directory is there (sometimes startup fails before it is created)
    set_path(path, sizeof(path), instance, NULL);
    if (check_path(path))
        return (ret);

    // to ensure that we are able to delete all blobs, we chown files back to 'eucalyptus'
    // (e.g., libvirt on KVM on Maverick chowns them to libvirt-qemu while
    // VM is running and then chowns them to root after termination)
    {
        DIR *dir = NULL;
        if ((dir = opendir(path)) == NULL) {
            return (-1);
        }

        struct dirent *dir_entry;
        while ((dir_entry = readdir(dir)) != NULL) {
            char *entry_name = dir_entry->d_name;

            if (!strcmp(".", entry_name) || !strcmp("..", entry_name))
                continue;

            // get the path of the directory item
            char entry_path[BLOBSTORE_MAX_PATH];
            snprintf(entry_path, sizeof(entry_path), "%s/%s", path, entry_name);

            if (diskutil_ch(entry_path, EUCALYPTUS_ADMIN, NULL, BACKING_FILE_PERM)) {
                LOGWARN("[%s] failed to chown files before cleanup\n", instance->instanceId);
            }
        }
        closedir(dir);
    }

    if (do_destroy_files) {
        set_id2(instance, "/.*", work_regex, sizeof(work_regex));

        if (blobstore_delete_regex(work_bs, work_regex) == -1) {
            LOGERROR("[%s] failed to remove some artifacts in %s\n", instance->instanceId, path);
        }

        for (i = 0; i < EUCA_MAX_VOLUMES; ++i) {
            volume = &instance->volumes[i];
            snprintf(path, sizeof(path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volume->volumeId);
            unlink(path);
        }

        // delete all non-blob files in the directory
        if (blobstore_delete_nonblobs(work_bs, instance->instancePath) < 0) {
            LOGWARN("[%s] failed to delete some non-blob files under %s\n", instance->instanceId, instance->instancePath);
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

//!
//!
//!
//! @param[in] euca_home
//! @param[in] rundir_path
//! @param[in] instName
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_ERROR,
//!         EUCA_IO_ERROR and EUCA_MEMORY_ERROR.
//!
int make_credential_floppy(char *euca_home, ncInstance * instance)
{
    int fd = 0;
    int rc = 0;
    int rbytes = 0;
    int count = 0;
    int ret = EUCA_ERROR;
    char dest_path[1024] = "";
    char source_path[1024] = "";
    char *ptr = NULL;
    char *buf = NULL;
    char *tmp = NULL;

    char *rundir_path = instance->instancePath;

    if (!euca_home || !rundir_path || !strlen(euca_home) || !strlen(rundir_path)) {
        return (EUCA_ERROR);
    }

    snprintf(source_path, 1024, EUCALYPTUS_HELPER_DIR "/floppy", euca_home);
    snprintf(dest_path, 1024, "%s/floppy", rundir_path);

    if ((buf = EUCA_ALLOC(1024 * 2048, sizeof(char))) == NULL) {
        ret = EUCA_MEMORY_ERROR;
        goto cleanup;
    }

    if ((fd = open(source_path, O_RDONLY)) < 0) {
        ret = EUCA_IO_ERROR;
        goto cleanup;
    }

    rbytes = read(fd, buf, 1024 * 2048);
    close(fd);
    if (rbytes < 0) {
        ret = EUCA_IO_ERROR;
        goto cleanup;
    }

    tmp = EUCA_ZALLOC(KEY_STRING_SIZE, sizeof(char));
    if (!tmp) {
        ret = EUCA_MEMORY_ERROR;
        goto cleanup;
    }

    ptr = buf;
    count = 0;
    while (count < rbytes) {
        memcpy(tmp, ptr, strlen("MAGICEUCALYPTUSINSTPUBKEYPLACEHOLDER"));
        if (!strcmp(tmp, "MAGICEUCALYPTUSINSTPUBKEYPLACEHOLDER")) {
            memcpy(ptr, instance->instancePubkey, strlen(instance->instancePubkey));
        } else if (!strcmp(tmp, "MAGICEUCALYPTUSAUTHPUBKEYPLACEHOLDER")) {
            memcpy(ptr, instance->euareKey, strlen(instance->euareKey));
        } else if (!strcmp(tmp, "MAGICEUCALYPTUSAUTHSIGNATPLACEHOLDER")) {
            memcpy(ptr, instance->instanceToken, strlen(instance->instanceToken));
        } else if (!strcmp(tmp, "MAGICEUCALYPTUSINSTPRIKEYPLACEHOLDER")) {
            memcpy(ptr, instance->instancePk, strlen(instance->instancePk));
        }

        ptr++;
        count++;
    }

    if ((fd = open(dest_path, O_CREAT | O_TRUNC | O_RDWR, 0700)) < 0) {
        ret = EUCA_IO_ERROR;
        goto cleanup;
    }

    rc = write(fd, buf, rbytes);
    close(fd);

    if (rc != rbytes) {
        ret = EUCA_IO_ERROR;
        goto cleanup;
    }

    ret = EUCA_OK;

cleanup:
    if (buf != NULL)
        EUCA_FREE(buf);
    if (tmp != NULL)
        EUCA_FREE(tmp);
    return ret;
}

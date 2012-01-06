// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
  Copyright (c) 2009  Eucalyptus Systems, Inc.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, only version 3 of the License.

  This file is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  for more details.

  You should have received a copy of the GNU General Public License along
  with this program.  If not, see <http://www.gnu.org/licenses/>.

  Please contact Eucalyptus Systems, Inc., 130 Castilian
  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
  if you need additional information or have any questions.

  This file may incorporate work covered under the following copyright and
  permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California


  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

  Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

  Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/

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
#include "misc.h" // logprintfl, ensure_...
#include "data.h" // ncInstance
#include "diskutil.h"
#include "eucalyptus.h"
#include "blobstore.h"
#include "walrus.h"
#include "storage-windows.h"
#include "handlers.h" // nc_state
#include "backing.h"
#include "iscsi.h"
#include "vbr.h"
#include "ipc.h" // sem

#define CACHE_TIMEOUT_USEC  1000000LL*60*60*2 
#define STORE_TIMEOUT_USEC  1000000LL*60*2
#define DELETE_TIMEOUT_USEC 1000000LL*10
#define FIND_TIMEOUT_USEC   50000LL // TODO: use 1000LL or less to induce rare timeouts

static char instances_path [MAX_PATH];
static blobstore * cache_bs = NULL;
static blobstore * work_bs = NULL;
static sem * disk_sem = NULL;

extern struct nc_state_t nc_state;

static void bs_errors (const char * msg) { 
    // we normally do not care to print all messages from blobstore as many are errors that we can handle
    logprintfl (EUCADEBUG2, "{%u} blobstore: %s", (unsigned int)pthread_self(), msg);
} 

static void stat_blobstore (const char * conf_instances_path, const char * name, blobstore_meta * meta)
{
    bzero (meta, sizeof (blobstore_meta));
    char path [MAX_PATH]; 
    snprintf (path, sizeof (path), "%s/%s", conf_instances_path, name);
    blobstore * bs = blobstore_open (path, 
                                     0, // any size
                                     0, // no flags = do not create it
                                     BLOBSTORE_FORMAT_ANY, 
                                     BLOBSTORE_REVOCATION_ANY, 
                                     BLOBSTORE_SNAPSHOT_ANY);
    if (bs == NULL)
        return;
    blobstore_stat (bs, meta);
    blobstore_close (bs);
}

static int stale_blob_examiner (const blockblob * bb);
static bunchOfInstances ** instances = NULL;

int check_backing_store (bunchOfInstances ** global_instances)
{
    instances = global_instances;

    if (work_bs) {
        if (blobstore_fsck (work_bs, stale_blob_examiner)) {
            logprintfl (EUCAERROR, "ERROR: work directory failed integrity check: %s\n", blobstore_get_error_str(blobstore_get_error()));
            blobstore_close (cache_bs);
            return ERROR;
        }
    }
    if (cache_bs) {
        if (blobstore_fsck (cache_bs, NULL)) { // TODO: verify checksums?
            logprintfl (EUCAERROR, "ERROR: cache failed integrity check: %s\n", blobstore_get_error_str(blobstore_get_error()));
            return ERROR;
        }
    }
    return OK;
}

void stat_backing_store (const char * conf_instances_path, blobstore_meta * work_meta, blobstore_meta * cache_meta)
{
    assert (conf_instances_path);
    stat_blobstore (conf_instances_path, "work",  work_meta);
    stat_blobstore (conf_instances_path, "cache", cache_meta);
}

int init_backing_store (const char * conf_instances_path, unsigned int conf_work_size_mb, unsigned int conf_cache_size_mb)
{
    logprintfl (EUCAINFO, "initializing backing store...\n");

    if (conf_instances_path == NULL) {
        logprintfl (EUCAERROR, "error: INSTANCE_PATH not specified\n");
        return ERROR;
    }
    safe_strncpy (instances_path, conf_instances_path, sizeof (instances_path));
    if (check_directory (instances_path)) {
	    logprintfl (EUCAERROR, "error: INSTANCE_PATH (%s) does not exist!\n", instances_path);
        return ERROR;
    }
    char cache_path [MAX_PATH]; snprintf (cache_path, sizeof (cache_path), "%s/cache", instances_path);
    if (ensure_directories_exist (cache_path, 0, NULL, NULL, BACKING_DIRECTORY_PERM) == -1) return ERROR;
    char work_path [MAX_PATH];  snprintf (work_path,  sizeof (work_path),  "%s/work", instances_path);
    if (ensure_directories_exist (work_path, 0, NULL, NULL, BACKING_DIRECTORY_PERM) == -1) return ERROR;
    unsigned long long cache_limit_blocks = conf_cache_size_mb * 2048; // convert MB to blocks
    unsigned long long work_limit_blocks  = conf_work_size_mb * 2048;
    if (work_limit_blocks==0) { // we take 0 as unlimited
        work_limit_blocks = ULLONG_MAX;
    }

    blobstore_set_error_function ( &bs_errors );
    if (cache_limit_blocks) {
        cache_bs = blobstore_open (cache_path, cache_limit_blocks, BLOBSTORE_FLAG_CREAT, BLOBSTORE_FORMAT_DIRECTORY, BLOBSTORE_REVOCATION_LRU, BLOBSTORE_SNAPSHOT_ANY);
        if (cache_bs==NULL) {
            logprintfl (EUCAERROR, "ERROR: failed to open/create cache blobstore: %s\n", blobstore_get_error_str(blobstore_get_error()));
            return ERROR;
        }
    }
    work_bs = blobstore_open (work_path, work_limit_blocks, BLOBSTORE_FLAG_CREAT, BLOBSTORE_FORMAT_FILES, BLOBSTORE_REVOCATION_NONE, BLOBSTORE_SNAPSHOT_ANY);
    if (work_bs==NULL) {
        logprintfl (EUCAERROR, "ERROR: failed to open/create work blobstore: %s\n", blobstore_get_error_str(blobstore_get_error()));
        logprintfl (EUCAERROR, "ERROR: %s\n", blobstore_get_last_trace());
        blobstore_close (cache_bs);
        return ERROR;
    }

    // set the initial value of the semaphore to the number of 
    // disk-intensive operations that can run in parallel on this node
    if (nc_state.concurrent_disk_ops && (disk_sem = sem_alloc (nc_state.concurrent_disk_ops, "mutex")) == NULL) {
        logprintfl (EUCAERROR, "failed to create and initialize disk semaphore\n");
        return ERROR;
    }

    return OK;
}

// sets id to:
// - the blockblob ID of an instance-directory blob (if vbr!=NULL): userId/instanceId/blob-....
// - the work prefix within work blobstore for an instance: userId/instanceId
static void set_id (const ncInstance * instance, virtualBootRecord * vbr, char * id, unsigned int id_size) // TODO: remove this
{
    assert (id);
    assert (instance);
    assert (strlen (instance->userId));
    assert (strlen (instance->instanceId));
    
    char suffix [1024] = "";
    if (vbr) {
        assert (vbr);
        assert (strlen (vbr->typeName));
    
        snprintf (id, id_size, "/blob-%s-%s",
                  vbr->typeName, 
                  (vbr->type==NC_RESOURCE_KERNEL||vbr->type==NC_RESOURCE_RAMDISK)?(vbr->id):(vbr->guestDeviceName));
    }
    snprintf (id, id_size, "%s/%s%s", instance->userId, instance->instanceId, suffix);
}

// sets id to:
// - the work prefix within work blobstore for an instance: userId/instanceId(suffix)
static void set_id2 (const ncInstance * instance, const char * suffix, char * id, unsigned int id_size)
{
    assert (id);
    assert (instance);
    assert (strlen (instance->userId));
    assert (strlen (instance->instanceId));
    snprintf (id, id_size, "%s/%s%s", instance->userId, instance->instanceId, (suffix)?(suffix):(""));
}

// sets path to 
// - the path of a file in an instance directory (if filename!=NULL)
// - the path of the instance directory (if instance!=NULL)
// - the path where all instance directories are kept
// this function must be kept consistent with set_id() below
static void set_path (char * path, unsigned int path_size, const ncInstance * instance, const char * filename)
{
    assert (strlen (instances_path));
    if (instance) {
        assert (strlen (instance->userId));
        assert (strlen (instance->instanceId));
        char buf [1024];
        set_id (instance, NULL, buf, sizeof (buf));
        if (filename) {
            snprintf (path, path_size, "%s/work/%s/%s", instances_path, buf, filename);
        } else {
            snprintf (path, path_size, "%s/work/%s", instances_path, buf);
        } 
    } else {
        snprintf     (path, path_size, "%s/work", instances_path);
    }
}

static int stale_blob_examiner (const blockblob * bb)
{
    char work_path [MAX_PATH];
    
    set_path (work_path, sizeof (work_path), NULL, NULL);
    int work_path_len = strlen (work_path);
    assert (work_path_len > 0);

    char * s = strstr(bb->blocks_path, work_path);
    if (s==NULL || s!=bb->blocks_path)
        return 0; // blob not under work blobstore path

    // parse the path past the work directory base
    safe_strncpy (work_path, bb->blocks_path, sizeof (work_path));
    s = work_path + work_path_len + 1;
    char * user_id = strtok (s, "/");
    char * inst_id = strtok (NULL, "/"); 
    char * file    = strtok (NULL, "/");

    ncInstance * instance = find_instance (instances, inst_id);
    if (instance == NULL) { // not found among running instances => stale
        // while we're here, try to delete extra files that aren't managed by the blobstore
        // TODO: ensure we catch any other files - perhaps by performing this cleanup after all blobs are deleted
        char path [MAX_PATH];
#define del_file(filename) snprintf (path, sizeof (path), "%s/work/%s/%s/%s", instances_path, user_id, inst_id, filename); unlink (path);
        del_file("instance.xml");
        del_file("libvirt.xml");
        del_file("console.log");
        del_file("instance.checkpoint");
        return 1;
    }

    return 0;
}

int save_instance_struct (const ncInstance * instance)
{
    if (instance==NULL) {
	    logprintfl(EUCADEBUG, "save_instance_struct: NULL instance!\n");
        return ERROR;
    }

    char checkpoint_path [MAX_PATH];
    set_path (checkpoint_path, sizeof (checkpoint_path), instance, "instance.checkpoint");

    int fd;
    if ((fd = open (checkpoint_path, O_CREAT | O_WRONLY, BACKING_FILE_PERM)) < 0) {
	    logprintfl(EUCADEBUG, "[%s] save_instance_struct: failed to create instance checkpoint at %s\n", instance->instanceId, checkpoint_path);
        return ERROR;
    }

    if (write (fd, (char *)instance, sizeof(struct ncInstance_t)) != sizeof (struct ncInstance_t)) {
	    logprintfl(EUCADEBUG, "[%s] save_instance_struct: failed to write instance checkpoint at %s\n", instance->instanceId, checkpoint_path);
        close (fd);
        return ERROR;
    }
    close (fd);
    
    return OK;
}

ncInstance * load_instance_struct (const char * instanceId)
{
    const int meta_size = sizeof (struct ncInstance_t);
    ncInstance * instance = calloc (1, meta_size);    
    if (instance==NULL) {
	    logprintfl (EUCADEBUG, "load_instance_struct: out of memory for instance struct\n");
	    return NULL;
    }
    safe_strncpy (instance->instanceId, instanceId, sizeof (instance->instanceId));

    // we don't know userId, so we'll look for instanceId in every user's
    // directory (we're assuming that instanceIds are unique in the system)
    char user_paths [MAX_PATH];
    set_path (user_paths, sizeof (user_paths), NULL, NULL);
    DIR * insts_dir = opendir(user_paths);
    if (insts_dir == NULL) {
	    logprintfl (EUCADEBUG, "load_instance_struct: failed to open %s\n", user_paths);
        goto free;
    }
    
    struct dirent * dir_entry;
    while ((dir_entry = readdir (insts_dir)) != NULL) {
        char tmp_path [MAX_PATH];
        struct stat mystat;
        
        snprintf(tmp_path, sizeof (tmp_path), "%s/%s/%s", user_paths, dir_entry->d_name, instance->instanceId);
        if (stat(tmp_path, &mystat)==0) {
            safe_strncpy (instance->userId, dir_entry->d_name, sizeof (instance->userId));
            break; // found it
        }
    }
    closedir (insts_dir);

    if (strlen(instance->userId)<1) {
	    logprintfl (EUCADEBUG, "load_instance_struct: didn't find instance %s\n", instance->instanceId);
        goto free;
    }

    int fd;
    char checkpoint_path [MAX_PATH];
    set_path (checkpoint_path, sizeof (checkpoint_path), instance, "instance.checkpoint");
    if ((fd = open(checkpoint_path, O_RDONLY)) < 0 
        || read (fd, instance, meta_size) < meta_size) {
        logprintfl(EUCADEBUG, "load_instance_struct: failed to load metadata for %s from %s: %s\n", instance->instanceId, checkpoint_path, strerror (errno));
        if(fd >= 0)
            close (fd);
        goto free;
    }
    close (fd);
    instance->stateCode = NO_STATE;
    // clear out pointers, since they are now wrong
    instance->params.root       = NULL;
    instance->params.kernel     = NULL;
    instance->params.ramdisk    = NULL;
    instance->params.swap       = NULL;
    instance->params.ephemeral0 = NULL;
    vbr_parse (&(instance->params), NULL); // fix up the pointers
    return instance;
    
 free:
    if (instance) free (instance);
    return NULL;
}

int create_instance_backing (ncInstance * instance)
{
    int ret = ERROR;
    virtualMachine * vm = &(instance->params);

    // ensure instance directory exists
    set_path (instance->instancePath,    sizeof (instance->instancePath),    instance, NULL);
    if (ensure_directories_exist (instance->instancePath, 0, NULL, "root", BACKING_DIRECTORY_PERM) == -1)
        goto out;

    // set various instance-directory-relative paths in the instance struct
    set_path (instance->xmlFilePath,     sizeof (instance->xmlFilePath),     instance, "instance.xml");
    set_path (instance->libvirtFilePath, sizeof (instance->libvirtFilePath), instance, "libvirt.xml");
    set_path (instance->consoleFilePath, sizeof (instance->consoleFilePath), instance, "console.log");
    if (strstr (instance->platform, "windows")) {
        // generate the floppy file for windows instances
        if (makeWindowsFloppy (nc_state.home, instance->instancePath, instance->keyName, instance->instanceId)) {
            logprintfl (EUCAERROR, "[%s] error: could not create windows bootup script floppy\n", instance->instanceId);
            goto out;
        } else {
            set_path (instance->floppyFilePath, sizeof (instance->floppyFilePath), instance, "floppy");
        }
    }
    
    char work_prefix [1024]; // {userId}/{instanceId}
    set_id (instance, NULL, work_prefix, sizeof (work_prefix));
    
    // compute tree of dependencies
    artifact * sentinel = vbr_alloc_tree (vm, // the struct containing the VBR
                                          FALSE, // for Xen and KVM we do not need to make disk bootable
                                          TRUE, // make working copy of runtime-modifiable files
                                          (instance->do_inject_key)?(instance->keyName):(NULL), // the SSH key
                                          instance->instanceId); // ID is for logging
    if (sentinel == NULL) {
        logprintfl (EUCAERROR, "[%s] error: failed to prepare backing for instance\n", instance->instanceId);
        goto out;
    }

    sem_p (disk_sem);
    // download/create/combine the dependencies
    int rc = art_implement_tree (sentinel, work_bs, cache_bs, work_prefix, INSTANCE_PREP_TIMEOUT_USEC);
    sem_v (disk_sem);

    if (rc != OK) {
        logprintfl (EUCAERROR, "[%s] error: failed to implement backing for instance\n", instance->instanceId);
        goto out;
    }

    if (save_instance_struct (instance)) // update instance checkpoint now that the struct got updated
        goto out;

    ret = OK;
 out:
    if (sentinel)
        art_free (sentinel);
    return ret;
}

int clone_bundling_backing (ncInstance *instance, const char* filePrefix, char* blockPath)
{
    char path[MAX_PATH];
    char work_regex [1024];
    char id [BLOBSTORE_MAX_PATH];
    char workPath [BLOBSTORE_MAX_PATH];
    int ret = OK;
    int found=-1;
    blockblob *src_blob = NULL, *dest_blob = NULL;
    blockblob_meta *matches = NULL;
    
    set_path (path, sizeof (path), instance, NULL);
    set_id2 (instance, "/.*", work_regex, sizeof (work_regex));
    
    if( (found=blobstore_search (work_bs, work_regex, &matches) <= 0 ) ) {
        logprintfl (EUCAERROR, "[%s] error: failed to find blob in %s %d\n", instance->instanceId, path, found);
        return ERROR;
    }
    
    for (blockblob_meta * bm = matches; bm; bm=bm->next) {
        blockblob * bb = blockblob_open (work_bs, bm->id, 0, 0, NULL, FIND_TIMEOUT_USEC);
        if (bb!=NULL && bb->snapshot_type == BLOBSTORE_SNAPSHOT_DM && strstr(bb->blocks_path,"emi-") != NULL) { // root image contains substr 'emi-'
            src_blob = bb;
            break;
        } else if (bb!=NULL) {
            blockblob_close(bb);
        }
    } 
    if (!src_blob) {
        logprintfl (EUCAERROR, "[%s] couldn't find the blob to clone from", instance->instanceId);
        goto error;
    }
    set_id (instance, NULL, workPath, sizeof (workPath));
    snprintf (id, sizeof(id), "%s/%s", workPath, filePrefix);
    
    // open destination blob 
    dest_blob = blockblob_open (work_bs, id, src_blob->size_bytes, BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL, NULL, FIND_TIMEOUT_USEC); 
    if (!dest_blob) {
        logprintfl (EUCAERROR, "[%s] couldn't create the destination blob for bundling (%s)", instance->instanceId, id);
        goto error;
    }
    
    if (strlen (dest_blob->blocks_path) > 0)
        snprintf (blockPath, MAX_PATH, "%s", dest_blob->blocks_path);
    
    // copy blob (will 'dd' eventually)
    if (blockblob_copy (src_blob, 0, dest_blob, 0, src_blob->size_bytes) != OK) {
        logprintfl (EUCAERROR, "[%s] couldn't copy block blob for bundling (%s)", instance->instanceId, id);
        goto error;
    }
    
    goto free;
 error: 
    ret = ERROR; 
 free:
    // free the search results
    for (blockblob_meta * bm = matches; bm;) {
        blockblob_meta * next = bm->next;
        free (bm);
        bm = next;
    } 
    
    if(src_blob)
        blockblob_close(src_blob);
    if(dest_blob)
        blockblob_close(dest_blob);
    return ret;
}

int destroy_instance_backing (ncInstance * instance, int do_destroy_files)
{
    int ret = OK;
    int total_prereqs = 0;
    char path [MAX_PATH];
    virtualMachine * vm = &(instance->params);
    
    // find and detach iSCSI targets, if any
    for (int i=0; i<EUCA_MAX_VBRS && i<vm->virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(vm->virtualBootRecord[i]);
        if (vbr->locationType==NC_LOCATION_IQN) {
            if (disconnect_iscsi_target (vbr->resourceLocation)) {
                logprintfl(EUCAERROR, "[%s] error: failed to disconnect iSCSI target attached to %s\n", instance->instanceId, vbr->backingPath);
            } 
        }
    }

    // see if instance directory is there (sometimes startup fails before it is created)
    set_path (path, sizeof (path), instance, NULL);
    if (check_path (path))
        return ret;

    // to ensure that we are able to delete all blobs, we chown files back to 'eucalyptus'
    // (e.g., libvirt on KVM on Maverick chowns them to libvirt-qemu while
    // VM is running and then chowns them to root after termination)
    set_path (path, sizeof (path), instance, "*");
    if (diskutil_ch (path, EUCALYPTUS_ADMIN, NULL, BACKING_FILE_PERM)) {
        logprintfl (EUCAWARN, "[%s] error: failed to chown files before cleanup\n", instance->instanceId);
    }

    if (do_destroy_files) {
        char work_regex [1024]; // {userId}/{instanceId}/.*
        set_id2 (instance, "/.*", work_regex, sizeof (work_regex));

        if (blobstore_delete_regex (work_bs, work_regex) == -1) {
            logprintfl (EUCAERROR, "[%s] error: failed to remove some artifacts in %s\n", instance->instanceId, path);
        }

        // remove the known leftover files
        unlink (instance->xmlFilePath);
        unlink (instance->libvirtFilePath);
        unlink (instance->consoleFilePath);
        if (strlen (instance->floppyFilePath)) {
            unlink (instance->floppyFilePath);
        }
        set_path (path, sizeof (path), instance, "instance.checkpoint");
        unlink (path);
        for (int i=0; i < EUCA_MAX_VOLUMES; ++i) {
            ncVolume * volume = &instance->volumes[i];
            snprintf (path, sizeof (path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volume->volumeId);
            unlink (path);
        }
        // bundle instance will leave additional files
        // let's delete every file in the directory
        struct dirent **files;
        int n = scandir(instance->instancePath, &files, 0, alphasort);
        char toDelete[MAX_PATH];
        if (n>0){
            while (n--) {
               struct dirent *entry = files[n];
               if( entry !=NULL && strncmp(entry->d_name, ".",1)!=0 && strncmp(entry->d_name, "..", 2)!=0){
                    snprintf(toDelete, MAX_PATH, "%s/%s", instance->instancePath, entry->d_name);
                    unlink(toDelete);
                    free(entry);
               }
            }
            free(files);
        }
    }
   
    // Finally try to remove the directory.
    // If either the user or our code introduced
    // any new files, this last step will fail.
    set_path (path, sizeof (path), instance, NULL);
    if (rmdir (path) && do_destroy_files) {
        logprintfl (EUCAWARN, "[%s] warning: failed to remove backing directory %s\n", instance->instanceId, path);
    }
    
    return ret;
}

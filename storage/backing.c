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
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
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
#include "backing.h"
#include "handlers.h" // connect_iscsi*
#include "vbr.h"

#define CACHE_TIMEOUT_USEC 1000000LL*60*60*2 
#define STORE_TIMEOUT_USEC 1000000LL*60*2
#define DELETE_TIMEOUT_USEC 1000000LL*10

static char instances_path [MAX_PATH];
static blobstore * cache_bs = NULL;
static blobstore * work_bs;

static void bs_errors (const char * msg) { 
    // we normally do not care to print all messages from blobstore as many are errors that we can handle
    logprintfl (EUCADEBUG2, "{%u} blobstore: %s", (unsigned int)pthread_self(), msg);
} 

int init_backing_store (const char * conf_instances_path, unsigned int conf_work_size_mb, unsigned int conf_cache_size_mb)
{
    logprintfl (EUCAINFO, "initializing backing store...\n");

    if (conf_instances_path == NULL) {
        logprintfl (EUCAERROR, "error: INSTANCE_PATH not specified\n");
        return ERROR;
    }
    strncpy (instances_path, conf_instances_path, sizeof (instances_path));
    if (check_directory (instances_path)) {
	    logprintfl (EUCAERROR, "error: INSTANCE_PATH (%s) does not exist!\n", instances_path);
        return ERROR;
    }
    char cache_path [MAX_PATH]; snprintf (cache_path, sizeof (cache_path), "%s/cache", instances_path);
    if (ensure_directories_exist (cache_path, 0, BACKING_DIRECTORY_PERM) == -1) return ERROR;
    char work_path [MAX_PATH];  snprintf (work_path,  sizeof (work_path),  "%s/work", instances_path);
    if (ensure_directories_exist (work_path, 0, BACKING_DIRECTORY_PERM) == -1) return ERROR;
    unsigned long long cache_limit_blocks = conf_cache_size_mb * 2048; // convert MB to blocks
    unsigned long long work_limit_blocks  = conf_work_size_mb * 2048;
    if (work_limit_blocks==0) { // we take 0 as unlimited
        work_limit_blocks = ULLONG_MAX;
    }

    blobstore_set_error_function ( &bs_errors );
    if (cache_limit_blocks) {
        cache_bs = blobstore_open (cache_path, cache_limit_blocks, BLOBSTORE_FORMAT_DIRECTORY, BLOBSTORE_REVOCATION_LRU, BLOBSTORE_SNAPSHOT_ANY);
        if (cache_bs==NULL) {
            logprintfl (EUCAERROR, "ERROR: %s\n", blobstore_get_error_str(blobstore_get_error()));
            return ERROR;
        }
        // TODO: run through cache and verify checksums?
        // TODO: run through cache and work and clean up unused stuff?
    }
    work_bs = blobstore_open (work_path, work_limit_blocks, BLOBSTORE_FORMAT_FILES, BLOBSTORE_REVOCATION_NONE, BLOBSTORE_SNAPSHOT_ANY);
    if (work_bs==NULL) {
        logprintfl (EUCAERROR, "ERROR: %s\n", blobstore_get_error_str(blobstore_get_error()));
        blobstore_close (cache_bs);
        return ERROR;
    }

    return OK;
}

static void update_vbr_with_backing_info (virtualBootRecord * vbr, blockblob * bb, int allow_block_dev)
{
    if (allow_block_dev && strlen (blockblob_get_dev (bb))) {
        strncpy (vbr->backingPath, blockblob_get_dev (bb), sizeof (vbr->backingPath));
        vbr->backingType = SOURCE_TYPE_BLOCK;
    } else {
        strncpy (vbr->backingPath, blockblob_get_file (bb), sizeof (vbr->backingPath));
        vbr->backingType = SOURCE_TYPE_FILE;
    }
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

static int create_vbr_backing (ncInstance * instance, virtualBootRecord * vbr, int allow_block_dev) // TODO: remove this obsolete function
{
    logprintfl (EUCAINFO, "[%s] preparing backing of type %s (pulled from '%s')...\n", instance->instanceId, vbr->typeName, vbr->resourceLocation);
    int ret = ERROR;

    // construct the blob IDs for this resource
    char * cache_id = vbr->id;
    char work_id [EUCA_MAX_PATH];
    set_id (instance, vbr, work_id, sizeof (work_id));

    // download data or prepare remote device or create local data
    switch (vbr->locationType) {
    case NC_LOCATION_URL:
    case NC_LOCATION_CLC: 
    case NC_LOCATION_SC:
        logprintfl (EUCAERROR, "[%s] error: backing of type %s is NOT IMPLEMENTED\n", instance->instanceId, vbr->typeName);
        // TODO: implement backing location type URL, CLC, SC
        break;

    case NC_LOCATION_WALRUS: {

        // get the digest
        char * blob_sig = walrus_get_digest (vbr->preparedResourceLocation);
        if (blob_sig==NULL) goto w_error;
        long long bb_size_bytes = str2longlong (blob_sig, "<size>", "</size>"); // pull size from the digest
        if (bb_size_bytes < 1) goto w_error;
        vbr->size = bb_size_bytes; // record size now that we know it

        // get a reference to a cached blob, if possible
        blockblob * cache_bb = NULL;
        int in_cache = 0;
        if (cache_bs) { // we have a cache store
            int flags = 0; // first we'll try opening as a reader
            while ((cache_bb = blockblob_open (cache_bs, cache_id, bb_size_bytes, flags, blob_sig, CACHE_TIMEOUT_USEC)) == NULL) {
                int err = blobstore_get_error();

                if (err==BLOBSTORE_ERROR_NOENT) { // cache entry does not exist
                    flags = BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL; // try creating

                } else if (err==BLOBSTORE_ERROR_SIGNATURE) { // wrong signature or length
                    // open with any signature and delete the old one
                    cache_bb = blockblob_open (cache_bs, cache_id, 0, 0, NULL, 0); // TODO: should there be a non-zero timeout?
                    if (cache_bb && blockblob_delete (cache_bb, 0) == 0) {
                        flags = BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL; // try creating
                        cache_bb = NULL; // (delete frees the handle)
                    } else {
                        break;
                    }
                } else if (err==BLOBSTORE_ERROR_NOSPC) { // no space in the cache
                    break; // give up

                } else if (err==BLOBSTORE_ERROR_AGAIN) { // timed out waiting
                    logprintfl (EUCAWARN, "[%s] timed out waiting for blob cache\n", instance->instanceId);
                    logprintfl (EUCADEBUG, "[%s] blobstore: %s\n%s", instance->instanceId, blobstore_get_last_msg(), blobstore_get_last_trace());
                    break; // give up

                } else {
                    logprintfl (EUCAWARN, "[%s] unkown error while preparing cache entry, skipping cache\n", instance->instanceId);
                    break;
                }
            }
            if (cache_bb) {
                if (!flags) {
                    in_cache = 1; // we have a valid entry in the cache
                    logprintfl (EUCAINFO, "[%s] blob cache has a valid entry\n", instance->instanceId);
                }
            } else {
                logprintfl (EUCAINFO, "[%s] blob cache will not be used\n", instance->instanceId);
            }
        }
        
        logprintfl (EUCAINFO, "[%s] allocating work blob %s of size %lld bytes\n", instance->instanceId,  work_id, bb_size_bytes);

        // allocate the work entry
        blockblob * work_bb = blockblob_open (work_bs, work_id, bb_size_bytes, BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL, blob_sig, STORE_TIMEOUT_USEC); // TODO: figure out the timeout
        if (work_bb==NULL)
            goto w_error;

        if (!in_cache) {
            const char * dest_path; // decide where the download will go
            if (cache_bb) {
                dest_path = blockblob_get_file (cache_bb);
            } else {
                dest_path = blockblob_get_file (work_bb);
            }
            if (walrus_image_by_manifest_url (vbr->preparedResourceLocation, dest_path, 1) != OK) {
                logprintfl (EUCAERROR, "[%s] error: failed to download component %s\n", instance->instanceId, vbr->preparedResourceLocation);
                goto w_error;
            }
        }

        if (cache_bb) {
            if (allow_block_dev) {
                blockmap map [] = {
                    {BLOBSTORE_SNAPSHOT, BLOBSTORE_BLOCKBLOB, {blob:cache_bb}, 0, 0, round_up_sec (bb_size_bytes) / 512}
                };
                if (blockblob_clone (work_bb, map, 1)==-1) {
                    logprintfl (EUCAERROR, "[%s] error: failed to clone cached blob %s to work blob %s\n", instance->instanceId, cache_bb->id, work_bb->id);
                    goto w_error;
                }
            } else {
                if (blockblob_copy (cache_bb, 0L, work_bb, 0L, 0L)==-1) {
                    logprintfl (EUCAERROR, "[%s] error: failed to copy cached blob %s to work blob %s\n", instance->instanceId, cache_bb->id, work_bb->id);
                    goto w_error;
                }
            }
        }

        if (instance->params.image == vbr && vbr->partitionNumber > 0) { // this is a linux root partition image 
            const char * dev = blockblob_get_dev (work_bb);

            // tune file system, which is needed to boot EMIs fscked long ago
            logprintfl (EUCAINFO, "[%s] tuning root file system on disk %d partition %d\n", instance->instanceId, vbr->diskNumber, vbr->partitionNumber);
            if (diskutil_tune (dev) == ERROR) {
                logprintfl (EUCAERROR, "[%s] error: failed to tune root file system\n", instance->instanceId);
                goto w_error;
            }

            logprintfl (EUCAINFO, "[%s] injecting the ssh key\n", instance->instanceId);

            // mount the partition
            char mnt_pt [EUCA_MAX_PATH];
            set_path (mnt_pt, sizeof (mnt_pt), instance, "euca-mount-XXXXXX");
            if (mkdtemp (mnt_pt)==NULL) {
                logprintfl (EUCAINFO, "[%s] error: mkdtemp() failed: %s\n", instance->instanceId,  strerror (errno));
                goto w_error;
            }
            if (diskutil_mount (dev, mnt_pt) != OK) {
                logprintfl (EUCAINFO, "[%s] error: failed to mount '%s' on '%s'\n", instance->instanceId, dev, mnt_pt);
                goto w_error;
            }

            // save the SSH key, with the right permissions
            int injection_failed = 0;
            char path [EUCA_MAX_PATH];
            snprintf (path, sizeof (path), "%s/root/.ssh", mnt_pt);
            if (diskutil_mkdir (path) == -1) {
                logprintfl (EUCAINFO, "[%s] error: failed to create path '%s'\n", instance->instanceId, path);
                injection_failed = 1;
                goto unmount;
            }
            if (diskutil_ch (path, "root", 0700) != OK) {
                logprintfl (EUCAINFO, "[%s] error: failed to change user and/or permissions for '%s'\n", instance->instanceId, path);
                injection_failed = 1;
                goto unmount;
            }
            snprintf (path, sizeof (path), "%s/root/.ssh/authorized_keys", mnt_pt);
            if (diskutil_write2file (path, instance->keyName) != OK) { // TODO: maybe append the key instead of overwriting?
                logprintfl (EUCAINFO, "[%s] error: failed to save key in '%s'\n", instance->instanceId, path);
                injection_failed = 1;
                goto unmount;
            }
            if (diskutil_ch (path, "root", 0600) != OK) {
                logprintfl (EUCAINFO, "[%s] error: failed to change user and/or permissions for '%s'\n", instance->instanceId, path);
                injection_failed = 1;
                goto unmount;
            }

        unmount:

            // unmount partition and delete the mount point
            if (diskutil_umount (mnt_pt) != OK) {
                logprintfl (EUCAINFO, "[%s] error: failed to unmount %s (there may be a resource leak)\n", instance->instanceId, mnt_pt);
                injection_failed = 1;
            }
            if (rmdir (mnt_pt) != 0) {
                logprintfl (EUCAINFO, "[%s] error: failed to remove %s (there may be a resource leak): %s\n", instance->instanceId, mnt_pt, strerror(errno));
                injection_failed = 1;
            }

            if (injection_failed)
                goto w_error;
        }        

        update_vbr_with_backing_info (vbr, work_bb, allow_block_dev);
        ret = OK;

        w_error:
        
        if (work_bb) {
            if (blockblob_close (work_bb) != 0) {
                logprintfl (EUCAERROR, "[%s] error: failed to close work blob %s\n", work_id);
                ret = ERROR;
            }
        }
        if (cache_bb) {
            if (blockblob_close (cache_bb) != 0) {
                logprintfl (EUCAERROR, "[%s] error: failed to close cache blob %s\n", cache_id);
                ret = ERROR;
            }
        }
        if (blob_sig) 
            free (blob_sig);
        break;
    }
        
    case NC_LOCATION_IQN: {
        char * dev = connect_iscsi_target(vbr->resourceLocation);
		if (!dev || !strstr(dev, "/dev")) {
            logprintfl(EUCAERROR, "[%s] error: failed to connect to iSCSI target\n", instance->instanceId);
            goto i_error;
		} 
        // update VBR with device location
        strncpy (vbr->backingPath, dev, sizeof (vbr->backingPath));
        vbr->backingType = SOURCE_TYPE_BLOCK;
        ret = OK;

    i_error:
        break;
    }

    case NC_LOCATION_AOE:
        logprintfl (EUCAERROR, "[%s] error: backing of type %s is NOT IMPLEMENTED\n", instance->instanceId, vbr->typeName);
        // TODO:
        break;

    case NC_LOCATION_NONE: {
        // allocate the work entry
        logprintfl (EUCAINFO, "[%s] allocating work blob %s of size %lld bytes\n", instance->instanceId, work_id, vbr->size);

        blockblob * work_bb = blockblob_open (work_bs, work_id, vbr->size, BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL, NULL, STORE_TIMEOUT_USEC); // TODO: figure out the timeout
        if (work_bb==NULL)
            break;

        int format = ERROR;
        switch (vbr->format) {
        case NC_FORMAT_NONE:
            format = OK;
            break;
        case NC_FORMAT_EXT2: // TODO: distinguish ext2 and ext3!
        case NC_FORMAT_EXT3:
            logprintfl (EUCAINFO, "[%s] formatting blob %s as ext3\n", instance->instanceId, work_id);
            format = diskutil_mkfs (blockblob_get_dev (work_bb), vbr->size);
            break;
        case NC_FORMAT_SWAP:
            logprintfl (EUCAINFO, "[%s] formatting blob %s as swap\n", instance->instanceId, work_id);
            format = diskutil_mkswap (blockblob_get_dev (work_bb), vbr->size);
            break;
        default:
            logprintfl (EUCAERROR, "[%s] error: format of type %s is NOT IMPLEMENTED\n", instance->instanceId, vbr->formatName);
        }

        if (format == OK) {
            update_vbr_with_backing_info (vbr, work_bb, allow_block_dev);
            logprintfl (EUCAINFO, "[%s] work blob allocated\n", instance->instanceId);
            ret = OK;
        }

        if (work_bb) blockblob_close (work_bb);
        break;
    }

    default:
        logprintfl (EUCAERROR, "[%s] error: unrecognized locationType %d\n", instance->instanceId, vbr->locationType);
    }

    return ret;
}

static blockblob * open_blob (ncInstance * instance, virtualBootRecord * vbr)
{
    char id [EUCA_MAX_PATH];
    set_id (instance, vbr, id, sizeof (id));
    logprintfl (EUCADEBUG, "[%s] opening blob %s\n", instance->instanceId, id);
    return blockblob_open (work_bs, id, vbr->size, BLOBSTORE_FLAG_EXCL, NULL, STORE_TIMEOUT_USEC); // TODO: figure out the timeout
}

static int destroy_blob (ncInstance * instance, virtualBootRecord * vbr)
{
    blockblob * bb = open_blob (instance, vbr); // open just so we can close it
    if (bb==NULL) {
        return ERROR;
    }
    int ret = (blockblob_delete (bb, DELETE_TIMEOUT_USEC)==0)?(OK):(ERROR); // TODO: figure out the timeout
    if (ret!=OK) {
        blockblob_close (bb); // at least try to close it
    }
    return ret;
}

// sets vbr->guestDeviceName based on other entries in the struct
// (guestDevice{Type|Bus}, {disk|partition}Number}
static void set_disk_dev (virtualBootRecord * vbr)
{
    char type [3] = "\0\0\0";
    if (vbr->guestDeviceType==DEV_TYPE_FLOPPY) {
        type [0] = 'f';
    } else { // a disk 
        switch (vbr->guestDeviceBus) {
        case BUS_TYPE_IDE:    type [0] = 'h'; break;
        case BUS_TYPE_SCSI:   type [0] = 's'; break;
        case BUS_TYPE_VIRTIO: type [0] = 'v'; break;
        case BUS_TYPE_XEN:    type [0] = 'x'; type [1] = 'v'; break;
        case BUS_TYPES_TOTAL:
        default:
            type [0] = '?'; // error
        }
    }
    
    char disk;
    if (vbr->guestDeviceType==DEV_TYPE_FLOPPY) {
        assert (vbr->diskNumber >=0 && vbr->diskNumber <= 9);
        disk = '0' + vbr->diskNumber;
    } else { // a disk 
        assert (vbr->diskNumber >=0 && vbr->diskNumber <= 26);
        disk = 'a' + vbr->diskNumber;
    }
    
    char part [3] = "\0";
    if (vbr->partitionNumber) {
        snprintf (part, sizeof(part), "%d", vbr->partitionNumber);
    }
    
    snprintf (vbr->guestDeviceName, sizeof (vbr->guestDeviceName), "%sd%c%s", type, disk, part);
}

static int create_disk (ncInstance * instance, virtualBootRecord * disk, virtualBootRecord ** parts, int partitions) // remove this obsolete function
{
    logprintfl (EUCAINFO, "[%s] composing a disk from supplied partitions...\n", instance->instanceId);

    int ret = ERROR;
#define MBR_BLOCKS (62 + 4)
    disk->size = 512 * MBR_BLOCKS; 
    blockblob * pbbs [EUCA_MAX_PARTITIONS];
    blockmap map [EUCA_MAX_PARTITIONS] = { {BLOBSTORE_SNAPSHOT, BLOBSTORE_ZERO, {blob:NULL}, 0, 0, MBR_BLOCKS} }; // initially only MBR is in the map

    bzero(pbbs, sizeof(blockblob *) * EUCA_MAX_PARTITIONS);
    // run through partitions, add their sizes, populate the map
    for (int i=0; i<partitions; i++) {
        virtualBootRecord * p = * (parts + i);
        
        if (p->size < 1) {
            logprintfl (EUCAERROR, "[%s] error: unknown size for partition %d\n", instance->instanceId, i);
            goto cleanup;
        }

        if (p->size % 512) {
            logprintfl (EUCAERROR, "[%s] error: size for partition %d is not a multiple of 512\n", instance->instanceId, i);
            goto cleanup;
        }
        
        pbbs [i] = open_blob (instance, p);
        if (pbbs [i] == NULL) {
            logprintfl (EUCAERROR, "[%s] error: failed to open blob for partition %d\n", instance->instanceId, i);
            goto cleanup;
        }

        int m = i + 1; // first map entry is for MBR
        map [m].relation_type = BLOBSTORE_MAP;
        map [m].source_type = BLOBSTORE_BLOCKBLOB;
        map [m].source.blob = pbbs [i];
        map [m].first_block_src = 0;
        map [m].first_block_dst = (disk->size / 512);
        map [m].len_blocks = (p->size / 512);
        disk->size += p->size;
    }

    // set *some* of the fields in vbr:
    // - ones needed for set_id() below to work
    // - ones needed for xml.c:gen_instance_xml() to generate correct disk entries
    virtualBootRecord * p1 = * parts; // first partition is representative of others
    strncpy (disk->typeName, "root", sizeof (disk->typeName)); // for id
    disk->type = NC_RESOURCE_IMAGE; // for id
    disk->guestDeviceType = p1->guestDeviceType; // for xml
    disk->guestDeviceBus = p1->guestDeviceBus; // for xml
    disk->diskNumber = p1->diskNumber;
    set_disk_dev (disk);

    // generate the id and create the blob
    char disk_id [EUCA_MAX_PATH];
    set_id (instance, disk, disk_id, sizeof (disk_id));
    blockblob * dbb = blockblob_open (work_bs, disk_id, disk->size, BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL, NULL, STORE_TIMEOUT_USEC); // TODO: figure out the timeout
    if (dbb == NULL) {
        goto cleanup;
    }

    // map the partitions to the disk
    if (blockblob_clone (dbb, map, partitions + 1)==-1) {
        logprintfl (EUCAERROR, "[%s] error: failed to clone partitions to created disk\n", instance->instanceId);
        goto cleanup;
    }
    update_vbr_with_backing_info (disk, dbb, TRUE);

    // create MBR
    logprintfl (EUCAINFO, "[%s] creating MBR\n", instance->instanceId);
    if (diskutil_mbr (blockblob_get_dev (dbb), "msdos") == ERROR) { // issues `parted mklabel`
        logprintfl (EUCAERROR, "[%s] error: failed to add MBR to disk\n", instance->instanceId);
        goto cleanup;
    }

    for (int i=0; i<partitions; i++) {
        int m = i + 1; // first map entry is for MBR
        logprintfl (EUCAINFO, "[%s] adding partition %d to partition table\n", instance->instanceId, i);
        if (diskutil_part (blockblob_get_dev (dbb),  // issues `parted mkpart`
                           "primary", // TODO: make this work with more than 4 partitions
                           NULL, // do not create file system
                           map [m].first_block_dst, // first sector
                           map [m].first_block_dst + map [m].len_blocks - 1) == ERROR) {
            logprintfl (EUCAERROR, "[%s] error: failed to add partition %d to disk\n", instance->instanceId, i);
            goto cleanup;
        }

        /*
        virtualBootRecord * p = * (parts + i);
        blockblob * pbb = pbbs [i];
        if (instance->params.image == p) {
        */
        
    }

    // TODO: make disk bootable if kernel/ramdisk are present
    
    ret = OK;
 cleanup:
    
    if (dbb)
        blockblob_close (dbb);

    for (int i=0; i<partitions; i++) {
        if (pbbs [i]) {
            blockblob_close (pbbs [i]);
        }
    }

    return ret;
}

int create_instance_backing1 (ncInstance * instance) // remove this obsolete function
{
    int ret = ERROR;
    int total_prereqs = 0;
    virtualMachine * vm = &(instance->params);

    char instance_path [MAX_PATH];
    set_path (instance_path, sizeof (instance_path), instance, NULL);
    ensure_directories_exist (instance_path, 0, BACKING_DIRECTORY_PERM);

    // sort vbrs into prereqs[] and parts[] so they can be approached in the right order
    // (first the prereqs, then disks and partitions, in increasing order)
    virtualBootRecord * prereq [EUCA_MAX_VBRS];
    virtualBootRecord * parts  [BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS];
    bzero (parts, sizeof (parts));
    for (int i=0; i<EUCA_MAX_VBRS && i<vm->virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(vm->virtualBootRecord[i]);
        if (vbr->type==NC_RESOURCE_KERNEL || vbr->type==NC_RESOURCE_RAMDISK) {
            prereq [total_prereqs++] = vbr;
        } else {
            parts [vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;
        }
    }

    // first download the prerequisites
    for (int i=0; i<total_prereqs; i++) {
        virtualBootRecord * vbr = prereq [i];
        if (create_vbr_backing (instance, vbr, FALSE)) { // libvirt wants a file not a block device for kernel and ramdisk
            logprintfl (EUCAERROR, "[%s] error: failed to obtain prerequisites needed by instance\n", instance->instanceId);
            goto out;
        }
    }

    // then create disks and partitions
    for (int i=0; i<BUS_TYPES_TOTAL; i++) { 
        for (int j=0; j<EUCA_MAX_DISKS; j++) {
            int partitions = 0;
            for (int k=0; k<EUCA_MAX_PARTITIONS; k++) {
                virtualBootRecord * vbr = parts [i][j][k];
                if (vbr) {
                    if (create_vbr_backing (instance, vbr, TRUE)) { // libvirt can use either a file or block device for disks
                        logprintfl (EUCAERROR, "[%s] error: failed to create backing (bus %d, disk %d, part %d)\n", instance->instanceId, i, j, k);
                        goto out;
                    }
                    if (k>0)
                        partitions++; 
                    
                } else if (partitions) { // there were partitions and we saw them all
                    if (vm->virtualBootRecordLen==EUCA_MAX_VBRS) {
                        logprintfl (EUCAERROR, "[%s] error: out of room in the virtual boot record while adding disk %d on bus %d\n", instance->instanceId, j, i);
                        goto out;
                    }
                    if (create_disk (instance, &(vm->virtualBootRecord [vm->virtualBootRecordLen++]), &(parts [i][j][1]), partitions)) {
                        logprintfl (EUCAERROR, "[%s] error: failed to create disk %d on bus %d from %d partitions\n", instance->instanceId, j, i, partitions);
                        vm->virtualBootRecordLen--;
                        goto out;
                    }
                    partitions = 0;
                }
            }
        }
    }

    set_path (instance->instancePath,    sizeof (instance->instancePath),    instance, NULL);
    set_path (instance->xmlFilePath,     sizeof (instance->xmlFilePath),     instance, "instance.xml");
    set_path (instance->libvirtFilePath, sizeof (instance->libvirtFilePath), instance, "libvirt.xml");
    set_path (instance->consoleFilePath, sizeof (instance->consoleFilePath), instance, "console.log");

    if (save_instance_struct (instance)) // update instance checkpoint now that the struct got updated
        goto out;

    ret = OK;
 out:
    return ret;
}

int destroy_instance_backing1 (ncInstance * instance, int destroy_files) // TODO: remove this obsolete function
{
    int ret = OK;
    int total_prereqs = 0;
    char path [MAX_PATH];
    virtualMachine * vm = &(instance->params);

    // to ensure that we are able to delete all blobs, we chown files back to 'eucalyptus'
    // (e.g., libvirt on KVM on Maverick chowns them to libvirt-qemu while
    // VM is running and then chowns them to root after termination)
    set_path (path, sizeof (path), instance, "*");
    if (diskutil_ch (path, EUCALYPTUS_ADMIN, BACKING_FILE_PERM)) {
        logprintfl (EUCAWARN, "[%s] error: failed to chown files before cleanup\n", instance->instanceId);
    }
    
    // sort vbrs into prereqs[] and parts[] so they can be approached differently
    virtualBootRecord * prereq [EUCA_MAX_VBRS];
    virtualBootRecord * parts  [BUS_TYPES_TOTAL][EUCA_MAX_DISKS][EUCA_MAX_PARTITIONS];
    bzero (parts, sizeof (parts));
    for (int i=0; i<EUCA_MAX_VBRS && i<vm->virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(vm->virtualBootRecord[i]);
        if (vbr->type==NC_RESOURCE_KERNEL || vbr->type==NC_RESOURCE_RAMDISK) {
            prereq [total_prereqs++] = vbr;
        } else {
            parts [vbr->guestDeviceBus][vbr->diskNumber][vbr->partitionNumber] = vbr;
        }
    }

    // destroy disks and partitions
    for (int i=0; i<BUS_TYPES_TOTAL; i++) { 
        for (int j=0; j<EUCA_MAX_DISKS; j++) {
            for (int k=0; k<EUCA_MAX_PARTITIONS; k++) {
                virtualBootRecord * vbr = parts [i][j][k];
                if (vbr) {  
                    if (vbr->locationType==NC_LOCATION_IQN) {
                        if (disconnect_iscsi_target (vbr->resourceLocation)) {
                            logprintfl(EUCAERROR, "[%s] error: failed to disconnect iSCSI target attached to %s\n", instance->instanceId, vbr->backingPath);
                        } 
                    } else {
                        // if k==0, we will destroy a disk blob, 
                        // which must be destroyed before child
                        // partition blobs, if any (k>0)
                        if (destroy_files && destroy_blob (instance, vbr)) {
                            logprintfl (EUCAWARN, "[%s] error: failed to destroy backing (bus %d, disk %d, part %d) for instance\n", instance->instanceId, i, j, k);
                        }
                    }
                }
            }
        }
    }

    // destroy the prerequisites
    for (int i=0; i<total_prereqs; i++) {
        virtualBootRecord * vbr = prereq [i];
        if (destroy_files && destroy_blob (instance, vbr)) {
            logprintfl (EUCAWARN, "[%s] error: failed to destroy prerequisite for instance\n", instance->instanceId);
        }
    }

    if (destroy_files) {
        // remove the known leftover files
        unlink (instance->instancePath);
        unlink (instance->xmlFilePath);
        unlink (instance->libvirtFilePath);
        unlink (instance->consoleFilePath);
        set_path (path, sizeof (path), instance, "instance.checkpoint");
        unlink (path);
    }

    // finally try to remove the directory. now,
    // if either the user or our code introduced
    // any new files, this last step will fail
    set_path (path, sizeof (path), instance, NULL);
    if (rmdir (path) && destroy_files) {
        logprintfl (EUCAWARN, "[%s] warning: failed to remove backing directory %s\n", instance->instanceId, path);
    }
    
    return ret;
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
    strncpy (instance->instanceId, instanceId, sizeof (instance->instanceId));

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
            strncpy (instance->userId, dir_entry->d_name, sizeof (instance->userId));
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
    return instance;
    
 free:
    if (instance) free (instance);
    return NULL;
}

int create_instance_backing (ncInstance * instance)
{
    int ret = ERROR;
    virtualMachine * vm = &(instance->params);

    { // ensure instance directory exists
        char instance_path [MAX_PATH];
        set_path (instance_path, sizeof (instance_path), instance, NULL);
        ensure_directories_exist (instance_path, 0, BACKING_DIRECTORY_PERM);
    }
    
    char work_prefix [1024]; // {userId}/{instanceId}
    set_id (instance, NULL, work_prefix, sizeof (work_prefix));

    // compute tree of dependencies
    artifact * sentinel = vbr_alloc_tree (vm, // the struct containing the VBR
                                          FALSE, // for Xen and KVM we do not need to make disk bootable
                                          TRUE, // make working copy of runtime-modifiable files
                                          instance->keyName, // the SSH key
                                          instance->instanceId); // ID is for logging
    if (sentinel == NULL ||
        art_implement_tree (sentinel, work_bs, cache_bs, work_prefix, INSTANCE_PREP_TIMEOUT_USEC) != OK) { // download/create/combine the dependencies
        logprintfl (EUCAERROR, "[%s] error: failed to prepare backing for instance\n", instance->instanceId);
        goto out;
    }
    
    set_path (instance->instancePath,    sizeof (instance->instancePath),    instance, NULL);
    set_path (instance->xmlFilePath,     sizeof (instance->xmlFilePath),     instance, "instance.xml");
    set_path (instance->libvirtFilePath, sizeof (instance->libvirtFilePath), instance, "libvirt.xml");
    set_path (instance->consoleFilePath, sizeof (instance->consoleFilePath), instance, "console.log");

    if (save_instance_struct (instance)) // update instance checkpoint now that the struct got updated
        goto out;

    ret = OK;
 out:
    if (sentinel)
        art_free (sentinel);
    return ret;
}

int destroy_instance_backing (ncInstance * instance, int destroy_files)
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
    if (diskutil_ch (path, EUCALYPTUS_ADMIN, BACKING_FILE_PERM)) {
        logprintfl (EUCAWARN, "[%s] error: failed to chown files before cleanup\n", instance->instanceId);
    }

    if (destroy_files) {
        char work_regex [1024]; // {userId}/{instanceId}/.*
        set_id2 (instance, "/.*", work_regex, sizeof (work_regex));

        if (blobstore_delete_regex (work_bs, work_regex) == -1) {
            logprintfl (EUCAERROR, "[%s] error: failed to remove some artifacts in %s\n", path);
        }

        // remove the known leftover files
        unlink (instance->instancePath);
        unlink (instance->xmlFilePath);
        unlink (instance->libvirtFilePath);
        unlink (instance->consoleFilePath);
        set_path (path, sizeof (path), instance, "instance.checkpoint");
        unlink (path);
    }

    // Finally try to remove the directory.
    // If either the user or our code introduced
    // any new files, this last step will fail.
    set_path (path, sizeof (path), instance, NULL);
    if (rmdir (path) && destroy_files) {
        logprintfl (EUCAWARN, "[%s] warning: failed to remove backing directory %s\n", instance->instanceId, path);
    }
    
    return ret;
}

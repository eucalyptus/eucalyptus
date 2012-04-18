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

/*
 * blobstore.h
 */

#ifndef _BLOBSTORE_H
#define _BLOBSTORE_H

#define BLOBSTORE_MAX_PATH 1024
#define MAX_BLOCKMAP_SIZE 32
#define MAX_DM_NAME 128                // e.g. euca-819312998196-i-4336096F-prt-00512swap-ac8d5670
#define MAX_DM_PATH (MAX_DM_NAME+12)   // e.g. /dev/mapper/euca-819312998196-i-4336096F-prt-00512swap-ac8d5670 
#define MAX_DM_LINE (MAX_DM_PATH*2+40) // e.g. 0 1048576 snapshot $DM1 $DM2 p 16

// default permissions for blosbstore content
#define BLOBSTORE_DIRECTORY_PERM 0771 // the '1' is there so libvirt/KVM on Maverick do not stumble on permissions
#define BLOBSTORE_FILE_PERM 0660

// flags for *_open() calls
#define BLOBSTORE_FLAG_RDWR     00001
#define BLOBSTORE_FLAG_RDONLY   00002
#define BLOBSTORE_FLAG_CREAT    00004
#define BLOBSTORE_FLAG_EXCL     00010
#define BLOBSTORE_FLAG_STRICT   01000
#define BLOBSTORE_FLAG_HOLLOW   02000 // means the blob being created should be viewed as not occupying space

// in-use flags for blockblobs
#define BLOCKBLOB_STATUS_OPENED 00002 // currently opened by someone (read or write)
#define BLOCKBLOB_STATUS_LOCKED 00004 // locked by a process (TODO: remove this UNUSED status?)
#define BLOCKBLOB_STATUS_MAPPED 00010 // loopback device dm-mapped by one or more other blobs
#define BLOCKBLOB_STATUS_BACKED 00020 // loopback device used as a backing by device mapper

typedef enum { // make sure these match up with _blobstore_error_strings[] entries below
    BLOBSTORE_ERROR_OK = 0,
    BLOBSTORE_ERROR_GENERAL, // here for compatibility with 'ERROR' elsewhere in Eucalyptus

    // system errno equivalents
    BLOBSTORE_ERROR_NOENT,
    BLOBSTORE_ERROR_BADF,
    BLOBSTORE_ERROR_NOMEM,
    BLOBSTORE_ERROR_ACCES,
    BLOBSTORE_ERROR_EXIST,
    BLOBSTORE_ERROR_INVAL,
    BLOBSTORE_ERROR_NOSPC,
    BLOBSTORE_ERROR_AGAIN,
    BLOBSTORE_ERROR_MFILE,

    // blobstore-specific errors
    BLOBSTORE_ERROR_SIGNATURE,
    BLOBSTORE_ERROR_UNKNOWN,
} blobstore_error_t;

static char * _blobstore_error_strings [] = { // make sure these match up with blobstore_error_t enums above
    "success",
    "general error",

    // system errno equivalents
    "no such entity",
    "bad file descriptor",
    "out of memory",
    "permission denied",
    "already exists",
    "invalid parameters",
    "no space left",
    "timeout",
    "too many files open",

    // blobstore-specific errors
    "wrong signature",
    "unknown error"
};

typedef enum {
    BLOBSTORE_REVOCATION_ANY, // on create, defaults to NONE; on open, allows for whatever policy is in effect
    BLOBSTORE_REVOCATION_NONE, // on create, error will be returned when blobstore is full
    BLOBSTORE_REVOCATION_LRU, // on create, unlocked items are purged in priority+LRU order, when possible
} blobstore_revocation_t;

typedef enum {
    BLOBSTORE_SNAPSHOT_ANY, // on create, pick DM if possible; on open, allows for whatever policy is in effect
    BLOBSTORE_SNAPSHOT_NONE, // snapshots are not used, disk copies are used for cloning
    BLOBSTORE_SNAPSHOT_DM, // device mapper snapshots are used for cloning
} blobstore_snapshot_t;

typedef enum {
    BLOBSTORE_FORMAT_ANY, // on create, defaults to FILES_VISIBLE; on open, allows for whatever policy is in effect
    BLOBSTORE_FORMAT_FILES, // blob content/backing and metadata are stored under blobstore path in individual files
    BLOBSTORE_FORMAT_DIRECTORY, // all blob data are stored in a separate subdirectory under blobstore path
} blobstore_format_t;

typedef struct _blobstore {
    char id [BLOBSTORE_MAX_PATH]; // ID of the blobstore, to handle directory moving
    char path [BLOBSTORE_MAX_PATH]; // full path to blobstore directory
    unsigned long long limit_blocks; // maximum space, in 512-byte blocks, that all blobs in store may use together
    blobstore_revocation_t revocation_policy; 
    blobstore_snapshot_t snapshot_policy;
    blobstore_format_t format;
    int fd; // file descriptor of the blobstore metadata file
} blobstore;

typedef struct _blockblob {
    blobstore * store; // pointer to the store for this blob
    char id [BLOBSTORE_MAX_PATH]; // ID of the blob (used as part of file/directory name)
    char blocks_path [BLOBSTORE_MAX_PATH]; // full path of the content or snapshot backing file
    char device_path [BLOBSTORE_MAX_PATH]; // full path of a block device on which blob can be accessed
    char dm_name [MAX_DM_NAME]; // name of the main device mapper device if this is a clone
    unsigned long long size_bytes; // size of the blob in bytes
    unsigned long long blocks_allocated; // actual number of blocks on disk taken by the blob
    blobstore_snapshot_t snapshot_type; // ANY = not initialized/known, NONE = not a snapshot, DM = DM-based snapshot
    unsigned int in_use; // flags showing how the blockblob is being used (OPENED, LOCKED, LINKED)
    unsigned char is_hollow; // blockblob is 'hollow' - its size doesn't count toward the limit
    time_t last_accessed; // timestamp of last access
    time_t last_modified; // timestamp of last modification
    double priority; // priority, for assisting LRU
    int fd_lock; // file descriptor of the blockblob lock file
    int fd_blocks; // file descriptor of the blockblob content file

    // LL pointers
    struct _blockblob * next;
    struct _blockblob * prev;
} blockblob;

typedef struct _blockmap {
    enum { BLOBSTORE_COPY, BLOBSTORE_MAP, BLOBSTORE_SNAPSHOT } relation_type;
    enum { BLOBSTORE_DEVICE, BLOBSTORE_BLOCKBLOB, BLOBSTORE_ZERO } source_type;
    union {
        char device_path [BLOBSTORE_MAX_PATH];
        blockblob * blob;
    } source;
    unsigned long long first_block_src;
    unsigned long long first_block_dst;
    unsigned long long len_blocks;
} blockmap;

static char * blobstore_relation_type_name [] = {
    "copy", "map", "snapshot"
};

typedef struct _blockblob_meta {
    char id [BLOBSTORE_MAX_PATH]; // ID of the blob (used as part of file/directory name)
    unsigned long long size_bytes; // size of the blob in bytes
    unsigned int in_use; // flags showing how the blockblob is being used (OPENED, LOCKED, LINKED)
    unsigned char is_hollow; // blockblob is 'hollow' - its size doesn't count toward the limit
    time_t last_accessed; // timestamp of last access
    time_t last_modified; // timestamp of last modification
    blobstore * bs; // pointer to blobstore, if one is open

    struct _blockblob_meta * next;
    struct _blockblob_meta * prev;
} blockblob_meta;

typedef struct _blobstore_meta {
    char id [BLOBSTORE_MAX_PATH]; // ID of the blobstore, to handle directory moving
    unsigned long long blocks_limit; // max size of the blobstore, in blocks
    unsigned long long blocks_unlocked;  // number of blocks in blobstore allocated to blobs that are not in use and is not mapped
    unsigned long long blocks_locked;    // number of blocks in blobstore allocated to blobs that are in use or is mapped (a dependency)
    unsigned long long blocks_allocated; // number of blocks in blobstore that have been allocated on disk
    unsigned int num_blobs; // count of blobs in the blobstore
    blobstore_revocation_t revocation_policy; 
    blobstore_snapshot_t snapshot_policy;
    blobstore_format_t format;    
} blobstore_meta;

// blobstore operations

blobstore * blobstore_open ( const char * path, 
                             unsigned long long limit_blocks, // on create: 0 is not valid; on open: 0 = any size
                             unsigned int flags,
                             blobstore_format_t format,
                             blobstore_revocation_t revocation_policy,
                             blobstore_snapshot_t snapshot_policy);
int blobstore_stat (blobstore * bs, blobstore_meta * meta);
int blobstore_fsck (blobstore * bs, int (* examiner) (const blockblob * bb));
int blobstore_search ( blobstore * bs, const char * regex, blockblob_meta ** results ); // returns a list of blockblobs matching an expression
int blobstore_delete_regex (blobstore * bs, const char * regex); // delete all blobs in blobstore that match regex, return number deleted or -1 if error
int blobstore_close ( blobstore * bs ); // releases a reference, allowing others to change some parameters (revocation policy) or delete the store, and frees the blobstore handle
int blobstore_delete ( blobstore * bs ); // if no outside references to store or blobs exist, and no blobs are protected, deletes the blobs, the store metadata, and frees the blobstore handle
int blobstore_get_error ( void ); // returns code of the last error 
const char * blobstore_get_last_msg (); // returns last message logged inside blobstore
const char * blobstore_get_last_trace(); // returns last error stack trace logged inside blobstore
const char * blobstore_get_error_str ( blobstore_error_t error ); // description of the error
void blobstore_set_error_function ( void (* fn) (const char * msg)); // sets the function that will be handed error messages (instead of sending them to stdout)

int blobstore_init (void);
int blobstore_cleanup (void);

// blockblob operations

blockblob * blockblob_open ( blobstore * bs,
                             const char * id, // can be NULL if creating, in which case blobstore will pick a random ID
                             unsigned long long size_bytes, // on create: reserve this size; on open: verify the size, unless set to 0
                             unsigned int flags, // BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL - same semantcs as for open() flags
                             const char * sig, // if non-NULL, on create sig is recorded, on open it is verified
                             unsigned long long timeout ); // maximum wait, in milliseconds, for a lock (0 = no blocking)
int blockblob_close ( blockblob * bb ); // releases the blob locks, allowing others to open() it, and frees the blockblob handle
int blockblob_delete ( blockblob * bb, long long timeout_usec, char do_force ); // deletes the blob, its metadata, and frees the blockblob handle, but only
                                                                                // if no outside references to the blob exist and blob is not protected (unless forced)
int blockblob_copy ( blockblob * src_bb, // source blob to copy data from
                     unsigned long long src_offset_bytes, // start offset in source
                     blockblob * dst_bb, // destination blob to copy data to
                     unsigned long long dst_offset_bytes, // start offset in destination
                     unsigned long long len_bytes); // length of region to copy, 0 = copy until EOF of source
int blockblob_clone ( blockblob * bb, // destination blob
                      const blockmap * map, // map of blocks from other blobs to be copied/snapshotted (NOTE: all blocks in map must have sizes that are multiples of 512)
                      unsigned int map_size ); // length of the map []
const char * blockblob_get_dev ( blockblob * bb ); // returns a block device pointing to the blob
const char * blockblob_get_file ( blockblob * bb ); // returns a path to the file containg the blob, but only if snapshot_type={ANY|NONE}
unsigned long long blockblob_get_size_blocks ( blockblob * bb); // size of blob in blocks
unsigned long long blockblob_get_size_bytes ( blockblob * bb); // size of blob in bytes

#endif // _BLOBSTORE_H

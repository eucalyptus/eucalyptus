// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
 * blobstore.h
 */

#ifndef _BLOBSTORE_H
#define _BLOBSTORE_H

#define BLOBSTORE_MAX_PATH 1024
#define MAX_BLOCKMAP_SIZE 32
#define MAX_DM_NAME 32
#define MAX_DM_PATH (MAX_DM_NAME+12)
#define MAX_DM_LINE (MAX_DM_PATH*2+40)

// flags for *_open() calls
#define BLOBSTORE_FLAG_RDWR     00001
#define BLOBSTORE_FLAG_RDONLY   00002
#define BLOBSTORE_FLAG_CREAT    00004
#define BLOBSTORE_FLAG_EXCL     00010

// in-use flags for blockblobs
#define BLOCKBLOB_STATUS_OPENED 00002 // currently opened by someone (read or write)
#define BLOCKBLOB_STATUS_LOCKED 00004 // locked by a process (TODO: remove this UNUSED status?)
#define BLOCKBLOB_STATUS_MAPPED 00010 // loopback device dm-mapped by one or more other blobs
#define BLOCKBLOB_STATUS_BACKED 00020 // loopback device used as a backing by device mapper

typedef enum {
    BLOBSTORE_ERROR_OK = 0,

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

static char * _blobstore_error_strings [] = {
    "success",

    "no such entity",
    "bad file descriptor",
    "out of memory",
    "permission denied",
    "already exists",
    "invalid parameters",
    "no space left",
    "timeout",
    "too many files open",

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
    unsigned long long size_blocks; // size of the blob, in 512-byte blocks
    blobstore_snapshot_t snapshot_type; // ANY = not initialized/known, NONE = not a snapshot, DM = DM-based snapshot
    unsigned int in_use; // flags showing how the blockblob is being used (OPENED, LOCKED, LINKED)
    time_t last_accessed; // timestamp of last access
    time_t last_modified; // timestamp of last modification
    double priority; // priority, for assisting LRU
    int fd; // file descriptor of the blockblob metadata file

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

// blockstore operations

blobstore * blobstore_open ( const char * path, 
                             unsigned long long limit_blocks, // on create: 0 is not valid; on open: 0 = any size
                             blobstore_format_t format,
                             blobstore_revocation_t revocation_policy,
                             blobstore_snapshot_t snapshot_policy);
int blobstore_close ( blobstore * bs ); // releases a reference, allowing others to change some parameters (revocation policy) or delete the store, and frees the blobstore handle
int blobstore_delete ( blobstore * bs ); // if no outside references to store or blobs exist, and no blobs are protected, deletes the blobs, the store metadata, and frees the blobstore handle
int blobstore_get_error ( void ); // returns code of the last error 
const char * blobstore_get_error_str ( blobstore_error_t error ); // description of the error

// blockblob operations

blockblob * blockblob_open ( blobstore * bs,
                             const char * id, // can be NULL if creating, in which case blobstore will pick a random ID
                             unsigned long long size_blocks, // on create: reserve this size; on open: verify the size, unless set to 0
                             unsigned int flags, // BLOBSTORE_FLAG_CREAT | BLOBSTORE_FLAG_EXCL - same semantcs as for open() flags
                             const char * sig, // if non-NULL, on create sig is recorded, on open it is verified
                             unsigned long long timeout ); // maximum wait, in milliseconds, for a lock (0 = no blocking)
int blockblob_close ( blockblob * bb ); // releases the blob locks, allowing others to open() it, and frees the blockblob handle
int blockblob_delete ( blockblob * bb, long long timeout ); // if no outside references to the blob exist, and blob is not protected, deletes the blob, its metadata, and frees the blockblob handle
int blockblob_clone ( blockblob * bb, // destination blob
                      const blockmap * map, // map of blocks from other blobs to be copied/snapshotted
                      unsigned int map_size ); // length of the map []
const char * blockblob_get_dev ( blockblob * bb ); // returns a block device pointing to the blob
const char * blockblob_get_file ( blockblob * bb ); // returns a path to the file containg the blob, but only if snapshot_type={ANY|NONE}
unsigned long long blockblob_get_size ( blockblob * bb); // size of blob in blocks

#endif // _BLOBSTORE_H

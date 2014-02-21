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
//! @file storage/imager/diskfile.c
//! Need Description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdlib.h>                    // NULL
#include <stdio.h>
#include <string.h>                    // bzero, memcpy
#include <unistd.h>                    // getopt, access
#include <ctype.h>                     // tolower
#include <stdarg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <diskutil.h>

#include "diskfile.h"
#include "imager.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define SECTOR_SIZE                               512   //!< small enough and yet not too small
#define MIN_DF_BYTES                             (512 * 8)  //!< big enough for swap, any better ideas?

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static boolean diskutil_initialized = FALSE;

static const char *_pformats[sizeof(enum pformat_t)] = {
    "unknown",
    "ext3",
    "swap",
    "ntfs",
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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
//! @return The MBR size
//!
//! @todo figure out why extra couple sectors seem needed
//!
s64 mbr_size_bytes(void)
{
    return (SECTOR_SIZE * (62 + 4));
}

//!
//! Allocates a diskfile object and creates an empty files of size at most limit_bytes
//! either filled with zeros (written out) or not (seeked to the end of the file)
//!
//! @param[in] path
//! @param[in] limit_bytes
//! @param[in] zero_fill
//!
//! @return A pointer to the newly created diskfile structure
//!
//! @pre
//!
//! @post
//!
diskfile *df_create(const char *path, const s64 limit_bytes, boolean zero_fill)
{
    diskfile *df = NULL;

    if (!diskutil_initialized) {
        if (diskutil_init(FALSE)) {    // will not use GRUB functions
            LOGERROR("failed to initialize diskutil\n");
            return NULL;
        }
        diskutil_initialized = TRUE;
    }

    if ((df = EUCA_ZALLOC(1, sizeof(diskfile))) == NULL) {
        LOGERROR("out of memory\n");
        return NULL;
    }

    if (limit_bytes < MIN_DF_BYTES) {
        LOGERROR("disk file must be at least %d bytes\n", MIN_DF_BYTES);
        EUCA_FREE(df);
        return NULL;
    }

    euca_strncpy(df->path, path, sizeof(df->path));
    df->limit_bytes = round_down_sec(limit_bytes);  // ensure we do not exceed the limit
    df->size_sectors = df->limit_bytes / SECTOR_SIZE;

    LOGINFO("creating disk file %s sectors [0-%ld]\n", path, (df->size_sectors - 1));
    if (diskutil_ddzero(path, df->size_sectors, zero_fill) != EUCA_OK) {
        EUCA_FREE(df);
        df = NULL;
    }

    return df;
}

//!
//! Opens an existing diskfile
//!
//! @param[in] path
//!
//! @return A pointer to an existing diskfile.
//!
//! @pre
//!
//! @post
//!
//! @note The memory for the pointer is allocated so the caller is responsible
//!       to free this memory
//!
diskfile *df_open(const char *path)
{
    int p = 0;
    s64 last = 0;
    s64 first = 0;
    diskfile *df = NULL;

    if (!diskutil_initialized) {
        if (diskutil_init(FALSE)) {    // will not use GRUB functions
            LOGERROR("failed to initialize diskutil\n");
            return NULL;
        }
        diskutil_initialized = TRUE;
    }

    if ((df = EUCA_ZALLOC(1, sizeof(diskfile))) == NULL) {
        LOGERROR("out of memory\n");
        return NULL;
    }

    df->limit_bytes = round_down_sec(file_size(path));
    if (df->limit_bytes < MIN_DF_BYTES) {
        LOGERROR("disk file must be at least %d bytes\n", MIN_DF_BYTES);
        EUCA_FREE(df);
        return NULL;
    }

    euca_strncpy(df->path, path, sizeof(df->path));
    df->size_sectors = df->limit_bytes / SECTOR_SIZE;

    // discover the partitions, if any
    for (p = 0; p < MAX_PARTS; p++) {
        first = 0;
        last = 0;
        if (diskutil_sectors(df->path, p, ((long long *)&first), ((long long *)&last)) == EUCA_OK) {
            LOGINFO("partition %d: [%ld-%ld]\n", p, first, last);
            df->parts[p].first_sector = first;
            df->parts[p].last_sector = last;
            df->parts[p].size_bytes = (last - first + 1) * SECTOR_SIZE;
            df->nparts++;
        } else {
            if (df->nparts > 0) {      // some partitions found, so this is a disk
                df->mbr = MBR_MSDOS;   //! @TODO verify that it's 'msdos'
            } else {                   // no partitions found, so record sector information in part[0]
                df->parts[0].first_sector = 0;
                df->parts[0].last_sector = df->size_sectors - 1;
                df->parts[0].size_bytes = df->size_sectors * SECTOR_SIZE;
            }
            break;
        }
    }

    return df;
}

//!
//! Creates MBR of type 'mbr' and the partition table on diskfile 'df'
//! according to a NULL-terminated array of 'parts' specs (where only
//! 'size_bytes' and 'type' must be specified)
//!
//! @param[in] df
//! @param[in] type
//! @param[in] parts
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @see euca_strncpy(), euca_strdupcat()
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note Important note
//!
int df_partition(diskfile * df, enum mbr_t type, diskpart parts[])
{
    int i = 0;
    s64 current_sector = 0;
    diskpart *p = NULL;
    diskpart *q = NULL;

    if (type != MBR_MSDOS) {
        LOGERROR("only partition type 'msdos' is supported\n");
        return EUCA_ERROR;
    }
    df->mbr = type;

    df->nparts = 0;                    // ignore previous partitioning
    current_sector = 63;               // the starting sector for 'msdos' MBRs
    for (p = parts; ((p->size_bytes > 0) && (df->nparts < MAX_PARTS)); p++) {
        q = &(df->parts[df->nparts]);
        q->size_bytes = p->size_bytes;
        q->type = p->type;
        q->first_sector = current_sector;
        q->last_sector = current_sector + round_up_sec(p->size_bytes) / SECTOR_SIZE;    // round up to make sure all data fits
        if (q->last_sector >= df->size_sectors) {
            LOGERROR("out of space in disk for partition %d (sectors: last=%ld max=%ld)\n", df->nparts, q->last_sector, df->size_sectors);
            goto error;
        }
        current_sector = q->last_sector + 1;
        df->nparts++;
    }

    if (p->size_bytes > 0) {
        LOGERROR("maximum number of partitions reached (%d)\n", MAX_PARTS);
        goto error;
    }
    // now, actually do it on disk
    LOGINFO("adding MBR to disk file %s in sectors [0-62]\n", df->path);
    if (diskutil_mbr(df->path, "msdos") != EUCA_OK)
        goto error;

    for (i = 0; i < df->nparts; i++) {
        q = &(df->parts[i]);
        LOGINFO("adding partition %d to disk %s in sectors [%ld-%ld]\n", i, df->path, q->first_sector, q->last_sector);
        if (diskutil_part(df->path, "primary", enum_diskpart_t_string(q->type), q->first_sector, q->last_sector) != EUCA_OK)
            goto error;
    }

    return EUCA_OK;

error:
    df->nparts = 0;                    // leave it unpartitioned
    return EUCA_ERROR;
}

//!
//! Formats partition 'part' of diskfile 'df' using 'format'
//!
//! @param[in] df
//! @param[in] part
//! @param[in] format
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int df_format(diskfile * df, const int part, const enum diskpart_t format)
{
    int ret = EUCA_OK;
    int use_part = 0;
    s64 size_bytes = 0;
    char part_str[50] = "partition";
    diskpart *p = NULL;

    if (part < 0 || (df->mbr != MBR_NONE && part >= df->nparts)) {
        LOGERROR("invalid args part=%d nparts=%d\n", part, df->nparts);
        return EUCA_ERROR;
    }

    use_part = part;
    if (df->mbr == MBR_NONE) {         // if partition
        use_part = 0;                  // use part[0] (to store 'format')
    } else {
        snprintf(part_str, sizeof(part_str), "partition %d", use_part);
    }

    p = &(df->parts[use_part]);
    p->format = format;
    if (df->mbr == MBR_NONE) {         // if partition
        p->first_sector = 0;           // should be 0 already
        p->last_sector = df->size_sectors - 1;
    }

    size_bytes = (p->last_sector - p->first_sector + 1) * SECTOR_SIZE;
    if (diskutil_loop(df->path, p->first_sector * SECTOR_SIZE, p->lodev, sizeof(p->lodev)) != EUCA_OK) {
        LOGERROR("failed to mount partition on loopback\n");
        return EUCA_ERROR;
    }

    LOGINFO("formating %s as '%s' on '%s'\n", part_str, enum_format_as_string(format), df->path);
    switch (format) {
    case PFORMAT_SWAP:
        if (diskutil_mkswap(p->lodev, size_bytes) != EUCA_OK) {
            LOGERROR("failed to make swap space\n");
            ret = EUCA_ERROR;
            goto unloop;
        }
        break;

    case PFORMAT_EXT3:
        if (diskutil_mkfs(p->lodev, size_bytes) != EUCA_OK) {
            LOGERROR("failed to make swap space\n");
            ret = EUCA_ERROR;
            goto unloop;
        }
        break;

    default:
        err("called with invalid format");
        break;
    }

unloop:
    if (diskutil_unloop(p->lodev) != EUCA_OK) {
        LOGERROR("failed to detach loopback device (there may be a leak!)\n");
        ret = EUCA_ERROR;
    }

    return ret;
}

//!
//! Copies data in file at 'path' into partition 'part' of diskfile 'df'
//!
//! @param[in] df
//! @param[in] part
//! @param[in] path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int df_dd(diskfile * df, const int part, const char *path)
{
    int ret = EUCA_OK;
    int use_part = 0;
    diskpart *p = NULL;

    if ((part < 0) || (part >= df->nparts)) {
        LOGERROR("invalid args part=%d nparts=%d\n", part, df->nparts);
        return EUCA_ERROR;
    }

    use_part = part;
    if (df->mbr == MBR_NONE) {         // if partition
        use_part = 0;                  // use part[0]
    }

    p = &(df->parts[use_part]);
    if (df->mbr == MBR_NONE) {
        // if partition
        p->first_sector = 0;           // should be 0 already
        p->last_sector = df->size_sectors - 1;
    }

    if (diskutil_loop(df->path, p->first_sector * SECTOR_SIZE, p->lodev, sizeof(p->lodev)) != EUCA_OK) {
        LOGERROR("failed to mount partition on loopback\n");
        return EUCA_ERROR;
    }
    //! @TODO: experiment with block sizes?
    if (diskutil_dd(path, p->lodev, SECTOR_SIZE, (p->last_sector - p->first_sector + 1)) != EUCA_OK) {
        LOGERROR("failed to copy file '%s' to '%s'\n", path, p->lodev);
        ret = EUCA_ERROR;
    }

    if (diskutil_unloop(p->lodev) != EUCA_OK) {
        LOGERROR("failed to detach loopback device (there may be a leak!)\n");
        ret = EUCA_ERROR;
    }

    return ret;
}

//!
//! Mounts partition 'part' of diskfile 'df' on path 'path',
//! which it will attempt to create, if necessary
//!
//! @param[in] df
//! @param[in] part
//! @param[in] path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int df_mount(diskfile * df, const int part, const char *path)
{
    int ret = EUCA_OK;
    int use_part = 0;
    diskpart *p = NULL;

    if (part < 0 || part >= df->nparts) {
        LOGERROR("invalid args part=%d nparts=%d\n", part, df->nparts);
        return EUCA_ERROR;
    }

    use_part = part;
    if (df->mbr == MBR_NONE) {
        // if partition
        use_part = 0;                  // use part[0]
    }

    p = &(df->parts[use_part]);
    if (df->mbr == MBR_NONE) {
        // if partition
        p->first_sector = 0;           // should be 0 already
        p->last_sector = df->size_sectors - 1;
    }

    if (diskutil_loop(df->path, p->first_sector * SECTOR_SIZE, p->lodev, sizeof(p->lodev)) != EUCA_OK) {
        LOGERROR("failed to mount partition on loopback\n");
        return EUCA_ERROR;
    }

    euca_strncpy(p->mntpt, path, sizeof(p->mntpt));
    if (diskutil_mount(p->lodev, p->mntpt) != EUCA_OK) {
        LOGERROR("failed to mount '%s' on '%s'\n", p->lodev, path);
        ret = EUCA_ERROR;
    }

    return ret;
}

//!
//! Unmounts previously mounted partition 'part', optionally
//! performing 'tune2fs' operation (which resets mount count and time)
//!
//! @param[in] df
//! @param[in] part
//! @param[in] tune2fs
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int df_umount(diskfile * df, const int part, boolean tune2fs)
{
    int ret = EUCA_OK;
    int use_part = 0;
    diskpart *p = NULL;

    if (part < 0 || part >= df->nparts) {
        LOGERROR("invalid args part=%d nparts=%d\n", part, df->nparts);
        return EUCA_ERROR;
    }

    use_part = part;
    if (df->mbr == MBR_NONE) {
        // if partition
        use_part = 0;                  // use part[0]
    }

    p = &(df->parts[use_part]);
    if (df->mbr == MBR_NONE) {
        // if partition
        p->first_sector = 0;           // should be 0 already
        p->last_sector = df->size_sectors - 1;
    }

    if (diskutil_umount(p->mntpt) != EUCA_OK) {
        LOGERROR("failed to umount '%s' (there may be a leak!)\n", p->mntpt);
        ret = EUCA_ERROR;
    }

    if (diskutil_unloop(p->lodev) != EUCA_OK) {
        LOGERROR("failed to detach loopback device '%s' (there may be a leak!)\n", p->lodev);
        ret = EUCA_ERROR;
    }

    return ret;
}

//!
//! Installs grub on diskfile 'df', pointing root to partition 'part'
//!
//! @param[in] df
//! @param[in] part
//!
//! @return Always returns EUCA_OK for now.
//!
int df_grub(diskfile * df, const int part)
{
    return EUCA_OK;
}

//!
//! Closes all open handles and frees memory associated with a diskfile
//!
//! @param[in] df
//!
//! @return Always returns EUCA_OK for now.
//!
//! @pre
//!
//! @post
//!
//! @note Important note
//!
int df_close(diskfile * df)
{
    EUCA_FREE(df);
    return EUCA_OK;
}

//!
//!
//!
//! @param[in] s
//!
//! @return The enumeration value matching s
//!
//! @pre
//!
//! @post
//!
enum mbr_t parse_mbr_t_enum(const char *s)
{
    char *lc = euca_strduptolower(s);
    enum mbr_t val = MBR_NONE;

    if (strcmp(lc, "none") == 0)
        val = MBR_NONE;
    else if (strcmp(lc, "msdos") == 0)
        val = MBR_MSDOS;
    else
        err("failed to parse '%s' as mbr type", lc);
    EUCA_FREE(lc);

    return val;
}

//!
//!
//!
//! @param[in] t
//!
//! @return The string value matching t
//!
//! @pre
//!
//! @post
//!
const char *enum_diskpart_t_string(enum diskpart_t t)
{
    static const char *_diskparts[sizeof(enum diskpart_t)] = {
        "unknown",
        "linux-swap",                  // this is the value parted expects
        "ext2",
        "ntfs"
    };

    return _diskparts[t];
}

//!
//!
//!
//! @param[in] t
//!
//! @return The partition minimum size matching the partition type
//!
//! @pre
//!
//! @post
//!
s64 get_min_size(enum diskpart_t t)
{
    static const s64 _diskparts[sizeof(enum diskpart_t)] = {
        0,                             // unknown
        80,                            // linux-swap: based on empirical evidence
        439,                           // ext*: based on empirical evidence
        0                              //! ntfs: not implemented (@TODO)
    };

    return _diskparts[t] * SECTOR_SIZE;
}

//!
//!
//!
//! @param[in] fmt
//!
//! @return The disk partition format
//!
//! @pre
//!
//! @post
//!
enum diskpart_t pformat_to_diskpart_t(enum pformat_t fmt)
{
    switch (fmt) {
    case PFORMAT_UNKNOWN:
        return DISKPART_UNKNOWN;
    case PFORMAT_EXT3:
        return DISKPART_EXT234;
    case PFORMAT_SWAP:
        return DISKPART_SWAP;
    case PFORMAT_NTFS:
        return DISKPART_WINDOWS;
    default:
        err("invalid conversion");
        break;
    }
    return (0);
}

//!
//!
//!
//! @param[in] t
//!
//! @return The matching string for the partition format
//!
//! @pre
//!
//! @post
//!
const char *enum_format_as_string(enum pformat_t t)
{
    return _pformats[t];
}

//!
//!
//!
//! @param[in] s
//!
//! @return The matching enumeration value for the given string
//!
//! @pre
//!
//! @post
//!
enum pformat_t parse_pformat_t_enum(const char *s)
{
    int i = 0;
    enum pformat_t val = 0;
    boolean found = FALSE;
    char *lc = euca_strduptolower(s);

    for (i = 0; i < (sizeof(_pformats) / sizeof(char *)); i++) {
        if (strcmp(lc, _pformats[i]) == 0) {
            val = (enum pformat_t)i;
            found = TRUE;
            break;
        }
    }

    if (!found)
        err("failed to parse '%s' as format type", lc);

    EUCA_FREE(lc);
    return val;
}

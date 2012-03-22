// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#include <stdlib.h> // NULL
#include <stdio.h>
#include <string.h> // bzero, memcpy
#include <unistd.h> // getopt, access
#include <ctype.h> // tolower
#include <stdarg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include "diskfile.h"
#include "misc.h"
#include "diskutil.h"
#include "eucalyptus.h"
#include "imager.h"

#define SECTOR_SIZE 512 // small enough and yet not too small
#define MIN_DF_BYTES 512 * 8 // big enough for swap, any better ideas?

static boolean diskutil_initialized = FALSE;

long long mbr_size_bytes (void) { return SECTOR_SIZE * (62 + 4); } // TODO: figure out why extra couple sectors seem needed

// allocates a diskfile object and creates an empty files of size at most limit_bytes
// either filled with zeros (written out) or not (seeked to the end of the file)
diskfile * df_create (const char * path, const long long limit_bytes, boolean zero_fill)
{
    if (!diskutil_initialized) {
        if (diskutil_init(FALSE)) { // will not use GRUB functions
            logprintfl (EUCAERROR, "error: failed to initialize diskutil\n");
            return NULL;
        }
        diskutil_initialized = TRUE;
    }

    diskfile * df = calloc (1, sizeof (diskfile));
    if (df==NULL) {
        logprintfl (EUCAERROR, "error: out of memory in df_create()\n");
        return NULL;
    }

    if (limit_bytes < MIN_DF_BYTES) {
        logprintfl (EUCAERROR, "error: disk file must be at least %d bytes\n", MIN_DF_BYTES);
        free(df);
        return NULL;
    }

    safe_strncpy (df->path, path, sizeof (df->path));
    df->limit_bytes = round_down_sec (limit_bytes); // ensure we do not exceed the limit
    df->size_sectors = df->limit_bytes/SECTOR_SIZE;

    logprintfl (EUCAINFO, "creating disk file %s sectors [0-%lld]\n", path, (df->size_sectors-1));
    if (diskutil_ddzero (path, df->size_sectors, zero_fill)!=OK) {
        free (df);
        df = NULL;
    }

    return df;
}

// opens an existing diskfile
diskfile * df_open (const char * path)
{
    if (!diskutil_initialized) {
        if (diskutil_init(FALSE)) { // will not use GRUB functions
            logprintfl (EUCAERROR, "error: failed to initialize diskutil\n");
            return NULL;
        }
        diskutil_initialized = TRUE;
    }

    diskfile * df = calloc (1, sizeof (diskfile));
    if (df==NULL) {
        logprintfl (EUCAERROR, "error: out of memory in df_create()\n");
        return NULL;
    }

    df->limit_bytes = round_down_sec (file_size (path));
    if (df->limit_bytes < MIN_DF_BYTES) {
        logprintfl (EUCAERROR, "error: disk file must be at least %d bytes\n", MIN_DF_BYTES);
        free(df);
        return NULL;
    }

    safe_strncpy (df->path, path, sizeof (df->path));
    df->size_sectors = df->limit_bytes/SECTOR_SIZE;

    // discover the partitions, if any
    for (int p = 0; p < MAX_PARTS; p++) {
        long long first, last;
        if (diskutil_sectors (df->path, p, &first, &last)==OK) {
            logprintfl (EUCAINFO, "partition %d: [%lld-%lld]\n", p, first, last);
            df->parts[p].first_sector = first;
            df->parts[p].last_sector = last;
            df->parts[p].size_bytes = (last-first+1) * SECTOR_SIZE;
            df->nparts++;
        } else {
            if (df->nparts>0) { // some partitions found, so this is a disk
                df->mbr = MBR_MSDOS; // TODO: verify that it's 'msdos'
            } else { // no partitions found, so record sector information in part[0]
                df->parts[0].first_sector = 0;
                df->parts[0].last_sector = df->size_sectors-1;
                df->parts[0].size_bytes = df->size_sectors * SECTOR_SIZE;
            }
            break;
        }
    }

    return df;
}

// creates MBR of type 'mbr' and the partition table on diskfile 'df'
// according to a NULL-terminated array of 'parts' specs (where only
// 'size_bytes' and 'type' must be specified)
int df_partition (diskfile * df, enum mbr_t type, diskpart parts[])
{
    if (type!=MBR_MSDOS) {
        logprintfl (EUCAERROR, "error: only partition type 'msdos' is supported\n");
        return ERROR;
    }
    df->mbr = type;

    df->nparts = 0; // ignore previous partitioning
    long long current_sector = 63; // the starting sector for 'msdos' MBRs
    diskpart * p = parts;
    for (; p->size_bytes>0 && df->nparts<MAX_PARTS; p++) {
        diskpart * q = &(df->parts [df->nparts]);
        q->size_bytes = p->size_bytes;
        q->type = p->type;
        q->first_sector = current_sector;
        q->last_sector = current_sector + round_up_sec (p->size_bytes)/SECTOR_SIZE; // round up to make sure all data fits
        if (q->last_sector >= df->size_sectors) {
            logprintfl (EUCAERROR, "error: out of space in disk for partition %d (sectors: last=%lld max=%lld)\n", df->nparts, q->last_sector, df->size_sectors);
            goto error;
        }
        current_sector = q->last_sector + 1;
        df->nparts++;
    }
    if (p->size_bytes>0) {
        logprintfl (EUCAERROR, "error: maximum number of partitions reached (%d)\n", MAX_PARTS);
        goto error;
    }

    // now, actually do it on disk
    logprintfl (EUCAINFO, "adding MBR to disk file %s in sectors [0-62]\n", df->path);
    if (diskutil_mbr (df->path, "msdos")!=OK) goto error;
    for (int i=0; i<df->nparts; i++) {
        diskpart * q = & (df->parts [i]);
        logprintfl (EUCAINFO, "adding partition %d to disk %s in sectors [%lld-%lld]\n", i, df->path, q->first_sector, q->last_sector);
        if (diskutil_part (df->path, "primary", enum_diskpart_t_string (q->type), q->first_sector, q->last_sector)!=OK) goto error;
    }

    return OK;

 error:
    df->nparts = 0; // leave it unpartitioned
    return ERROR;
}

// formats partition 'part' of diskfile 'df' using 'format'
int df_format (diskfile * df, const int part, const enum diskpart_t format)
{
    int ret = OK;
    char part_str [50] = "partition";

    if (part<0 || (df->mbr!=MBR_NONE && part>=df->nparts)) {
        logprintfl (EUCAERROR, "error: internal error: invalid args to df_format(): part=%d nparts=%d\n", part, df->nparts);
        return ERROR;
    }

    int use_part = part;
    if (df->mbr==MBR_NONE) { // if partition
        use_part = 0; // use part[0] (to store 'format')
    } else {
        snprintf (part_str, sizeof (part_str), "partition %d", use_part);
    }
    diskpart * p = & (df->parts [use_part]);
    p->format = format;
    if (df->mbr==MBR_NONE) { // if partition
        p->first_sector = 0; // should be 0 already
        p->last_sector = df->size_sectors-1;
    }
    long long size_bytes = (p->last_sector - p->first_sector + 1) * SECTOR_SIZE;
    if (diskutil_loop (df->path,
                   p->first_sector * SECTOR_SIZE,
                   p->lodev,
                   sizeof (p->lodev))!=OK) {
        logprintfl (EUCAERROR, "error: failed to mount partition on loopback\n");
        return ERROR;
    }

    logprintfl (EUCAINFO, "formating %s as '%s' on '%s'\n", part_str, enum_format_as_string (format), df->path);
    switch (format) {
    case PFORMAT_SWAP:
        if (diskutil_mkswap (p->lodev, size_bytes)!=OK) {
            logprintfl (EUCAERROR, "error: failed to make swap space\n");
            ret = ERROR;
            goto unloop;
        }
        break;

    case PFORMAT_EXT3:
        if (diskutil_mkfs (p->lodev, size_bytes)!=OK) {
            logprintfl (EUCAERROR, "error: failed to make swap space\n");
            ret = ERROR;
            goto unloop;
        }
        break;

    default:
        err ("error: df_format() called with invalid format");
    }

 unloop:

    if (diskutil_unloop (p->lodev)!=OK) {
        logprintfl (EUCAERROR, "error: failed to detach loopback device (there may be a leak!)\n");
        ret = ERROR;
    }

    return ret;
}

// copies data in file at 'path' into partition 'part' of diskfile 'df'
int df_dd (diskfile * df, const int part, const char * path)
{
    int ret = OK;

    if (part<0 || part>=df->nparts) {
        logprintfl (EUCAERROR, "error: internal error: invalid args to df_dd(): part=%d nparts=%d\n", part, df->nparts);
        return ERROR;
    }

    int use_part = part;
    if (df->mbr==MBR_NONE) { // if partition
        use_part = 0; // use part[0]
    }
    diskpart * p = & (df->parts [use_part]);
    if (df->mbr==MBR_NONE) { // if partition
        p->first_sector = 0; // should be 0 already
        p->last_sector = df->size_sectors-1;
    }

    if (diskutil_loop (df->path,
                   p->first_sector * SECTOR_SIZE,
                   p->lodev,
                   sizeof (p->lodev))!=OK) {
        logprintfl (EUCAERROR, "error: failed to mount partition on loopback\n");
        return ERROR;
    }

    if (diskutil_dd (path, p->lodev, SECTOR_SIZE, (p->last_sector - p->first_sector + 1))!=OK) { // TODO: experiment with block sizes?
        logprintfl (EUCAERROR, "error: failed to copy file '%s' to '%s'\n", path, p->lodev);
        ret = ERROR;
    }

    if (diskutil_unloop (p->lodev)!=OK) {
        logprintfl (EUCAERROR, "error: failed to detach loopback device (there may be a leak!)\n");
        ret = ERROR;
    }

    return ret;
}

// mounts partition 'part' of diskfile 'df' on path 'path',
// which it will attempt to create, if necessary
int df_mount (diskfile * df, const int part, const char * path)
{
    int ret = OK;

    if (part<0 || part>=df->nparts) {
        logprintfl (EUCAERROR, "error: internal error: invalid args to df_mount(): part=%d nparts=%d\n", part, df->nparts);
        return ERROR;
    }

    int use_part = part;
    if (df->mbr==MBR_NONE) { // if partition
        use_part = 0; // use part[0]
    }
    diskpart * p = & (df->parts [use_part]);
    if (df->mbr==MBR_NONE) { // if partition
        p->first_sector = 0; // should be 0 already
        p->last_sector = df->size_sectors-1;
    }

    if (diskutil_loop (df->path,
                   p->first_sector * SECTOR_SIZE,
                   p->lodev,
                   sizeof (p->lodev))!=OK) {
        logprintfl (EUCAERROR, "error: failed to mount partition on loopback\n");
        return ERROR;
    }

    safe_strncpy (p->mntpt, path, sizeof (p->mntpt));
    if (diskutil_mount (p->lodev, p->mntpt)!=OK) {
        logprintfl (EUCAERROR, "error: failed to mount '%s' on '%s'\n", p->lodev, path);
        ret = ERROR;
    }

    return ret;
}

// unmounts previously mounted partition 'part', optionally
// performing 'tune2fs' operation (which resets mount count and time)
int df_umount (diskfile * df, const int part, boolean tune2fs)
{
    int ret = OK;

    if (part<0 || part>=df->nparts) {
        logprintfl (EUCAERROR, "error: internal error: invalid args to df_umount(): part=%d nparts=%d\n", part, df->nparts);
        return ERROR;
    }

    int use_part = part;
    if (df->mbr==MBR_NONE) { // if partition
        use_part = 0; // use part[0]
    }
    diskpart * p = & (df->parts [use_part]);
    if (df->mbr==MBR_NONE) { // if partition
        p->first_sector = 0; // should be 0 already
        p->last_sector = df->size_sectors-1;
    }

    if (diskutil_umount (p->mntpt)!=OK) {
        logprintfl (EUCAERROR, "error: failed to umount '%s' (there may be a leak!)\n", p->mntpt);
        ret = ERROR;
    }

    if (diskutil_unloop (p->lodev)!=OK) {
        logprintfl (EUCAERROR, "error: failed to detach loopback device '%s' (there may be a leak!)\n", p->lodev);
        ret = ERROR;
    }

    return ret;
}

// installs grub on diskfile 'df', pointing root to partition 'part'
int df_grub (diskfile * df, const int part)
{
    return OK;
}

// closes all open handles and frees memory associated with a diskfile
int df_close (diskfile * df)
{
    free (df);
    return OK;
}

// diskutil

enum mbr_t parse_mbr_t_enum (const char * s)
{
    char * lc = strduplc (s);
    enum mbr_t val;

    if (strcmp (lc, "none")==0) val = MBR_NONE;
    else if (strcmp (lc, "msdos")==0) val = MBR_MSDOS;
    else err ("failed to parse '%s' as mbr type", lc);
    free (lc);

    return val;
}

char * enum_diskpart_t_string (enum diskpart_t t)
{
    static char * _diskparts [sizeof (enum diskpart_t)] = {
        "unknown",
        "linux-swap", // this is the value parted expects
        "ext2",
        "ntfs"
    };
    return _diskparts [t];
}

long long get_min_size (enum diskpart_t t)
{
    static long long _diskparts [sizeof (enum diskpart_t)] = {
        0,  // unknown
        80, // linux-swap: based on empirical evidence
        439, // ext*: based on empirical evidence
        0 // ntfs: not implemented (TODO)
    };
    return _diskparts [t] * SECTOR_SIZE;
}

enum diskpart_t pformat_to_diskpart_t (enum pformat_t fmt)
{
    switch (fmt) {
    case PFORMAT_UNKNOWN: return DISKPART_UNKNOWN;
    case PFORMAT_EXT3:    return DISKPART_EXT234;
    case PFORMAT_SWAP:    return DISKPART_SWAP;
    case PFORMAT_NTFS:    return DISKPART_WINDOWS;
    default:
        err ("invalid pformat_to_diskpart_t conversion");
    }
    return 0;
}

static char * _pformats [sizeof (enum pformat_t)] = {
    "unknown",
    "ext3",
    "swap",
    "ntfs"
};

char * enum_format_as_string (enum pformat_t t)
{
    return _pformats [t];
}

enum pformat_t parse_pformat_t_enum (const char * s)
{
    enum pformat_t val;
    boolean found = FALSE;

    char * lc = strduplc (s);
    for (int i=0; i<(sizeof(_pformats)/sizeof(char *)); i++) {
        if (strcmp (lc, _pformats [i])==0) {
            val = (enum pformat_t)i;
            found = TRUE;
            break;
        }
    }

    if (!found)
        err ("failed to parse '%s' as format type", lc);
    free (lc);

    return val;
}


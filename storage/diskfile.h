// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#ifndef DISKFILE_H
#define DISKFILE_H

#include "misc.h" // boolean

#define MAX_PATH 4096
#define MAX_PARTS 20

typedef struct _diskpart {
    // public fields
    long long size_bytes;
    enum diskpart_t { // partition ids (as known by MBR)
        DISKPART_UNKNOWN=0,
        DISKPART_SWAP,
        DISKPART_EXT234,
        DISKPART_WINDOWS // not supported yet
    } type;
    // private fields
    enum pformat_t { // partition formats (swap, file system types)
        PFORMAT_UNKNOWN=0,
        PFORMAT_EXT3,
        PFORMAT_SWAP,
        PFORMAT_NTFS
    } format;
    long long first_sector;
    long long last_sector;
    char lodev [MAX_PATH];
    char mntpt [MAX_PATH];
} diskpart;

typedef struct _diskfile {
    // private fields
    char path [MAX_PATH];
    long long limit_bytes;
    long long size_sectors;
    enum mbr_t {
        MBR_NONE=0,
        MBR_MSDOS
    } mbr;
    diskpart parts [MAX_PARTS];
    int nparts;
} diskfile;

diskfile * df_create (const char * path, const long long limit_bytes, boolean zero_fill);
diskfile * df_open (const char * path);
int df_partition (diskfile * df, enum mbr_t type, diskpart parts[]);
int df_format (diskfile * df, const int part, enum diskpart_t format);
int df_dd (diskfile * df, const int part, const char * path);
int df_mount (diskfile * df, const int part, const char * path);
int df_umount (diskfile * df, const int part, boolean tune2fs);
int df_grub (diskfile * df, const int part);
int df_close (diskfile * df);

// diskutil
enum mbr_t parse_mbr_t_enum (const char * s);
char * enum_diskpart_t_string (enum diskpart_t t);
long long round_up_sec   (long long bytes);
long long round_down_sec (long long bytes);
long long mbr_size_bytes (void);
enum diskpart_t pformat_to_diskpart_t (enum pformat_t fmt);
char * enum_format_as_string (enum pformat_t t);
enum pformat_t parse_pformat_t_enum (const char * s);
long long get_min_size (enum diskpart_t t);
#endif

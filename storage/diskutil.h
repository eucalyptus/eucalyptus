// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#ifndef HELPERS_H
#define HELPERS_H

#include "misc.h" // bolean

int diskutil_init (void);
int diskutil_cleanup (void);
int diskutil_ddzero (const char * path, const long long sectors, boolean zero_fill);
int diskutil_dd (const char * in, const char * out, const int bs, const long long count);
int diskutil_dd2 (const char * in, const char * out, const int bs, const long long count, const long long seek, const long long skip);
int diskutil_mbr (const char * path, const char * type);
int diskutil_part (const char * path, char * part_type, const char * fs_type, const long long first_sector, const long long last_sector);
int diskutil_loop (const char * path, const long long offset, char * lodev, int lodev_size);
int diskutil_unloop (const char * lodev);
int diskutil_mkswap (const char * lodev, const long long size_bytes);
int diskutil_mkfs (const char * lodev, const long long size_bytes);
int diskutil_sectors (const char * path, const int part, long long * first, long long * last);
int diskutil_mount (const char * dev, const char * mnt_pt);
int diskutil_umount (const char * dev);
int diskutil_grub_files (const char * mnt_pt, const int part, const char * kernel, const char * ramdisk);
int diskutil_grub_mbr (const char * path, const int part);
int diskutil_ch (const char * path, const char * user, const int perms);
int diskutil_mkdir (const char * path);
int diskutil_cp (const char * from, const char * to);

#endif

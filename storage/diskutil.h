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
 ************************************************************************/

#ifndef HELPERS_H
#define HELPERS_H

#include "misc.h" // bolean
#include "ipc.h" // sem

#define MBR_BLOCKS 63 // the size of "DOS-compatibility region" partially used by 'grub'
#define SECTOR_SIZE 512

int diskutil_init (int require_grub);
sem * diskutil_get_loop_sem (void);
int diskutil_cleanup (void);
int diskutil_ddzero (const char * path, const long long sectors, boolean zero_fill);
int diskutil_dd (const char * in, const char * out, const int bs, const long long count);
int diskutil_dd2 (const char * in, const char * out, const int bs, const long long count, const long long seek, const long long skip);
int diskutil_mbr (const char * path, const char * type);
int diskutil_part (const char * path, char * part_type, const char * fs_type, const long long first_sector, const long long last_sector);
int diskutil_loop (const char * path, const long long offset, char * lodev, int lodev_size);
int diskutil_loop_check (const char * path, const char * lodev);
int diskutil_loop_clean (const char * path);
int diskutil_unloop (const char * lodev);
int diskutil_mkswap (const char * lodev, const long long size_bytes);
int diskutil_mkfs (const char * lodev, const long long size_bytes);
int diskutil_tune (const char * lodev);
int diskutil_sectors (const char * path, const int part, long long * first, long long * last);
int diskutil_mount (const char * dev, const char * mnt_pt);
int diskutil_umount (const char * dev);
int diskutil_write2file (const char * file, const char * str);
int diskutil_grub (const char * path, const char * mnt_pt, const int part, const char * kernel, const char * ramdisk);
int diskutil_grub_files (const char * mnt_pt, const int part, const char * kernel, const char * ramdisk);
int diskutil_grub_mbr (const char * path, const int part);
int diskutil_grub2_mbr (const char * path, const int part, const char * mnt_pt);
int diskutil_ch (const char * path, const char * user, const char * group, const int perms);
int diskutil_mkdir (const char * path);
int diskutil_cp (const char * from, const char * to);
long long round_up_sec   (long long bytes);
long long round_down_sec (long long bytes);
#endif

// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

#ifndef _INCLUDE_DISKUTIL_H_
#define _INCLUDE_DISKUTIL_H_

//!
//! @file storage/diskutil.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include "misc.h"                      // bolean
#include "ipc.h"                       // sem

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MBR_BLOCKS                                   63 //!< the size of "DOS-compatibility region" (32K) partially used by 'grub'
#define BOOT_BLOCKS                              204800 //!< the size of the boot partition (100MB)
#define SECTOR_SIZE                                 512 //!< the size of each sector in Bytes

#define MBR_BLOCKS                                63    //!< the size of "DOS-compatibility region" partially used by 'grub'
#define SECTOR_SIZE                              512

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

struct partition_table_entry {
    long long start_sector;
    long long end_sector;
    char type[32];
    char filesystem[32];
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int imaging_init(const char *new_euca_home_path, const char *new_cloud_cert_path, const char *new_service_key_path);
int imaging_image_by_manifest_url(const char *instanceId, const char *url, const char *dest_path, long long size_bytes);

int diskutil_init(int check_first);
int diskutil_cleanup(void);
int diskutil_ddzero(const char *path, const long long sectors, boolean zero_fill);
int diskutil_dd(const char *in, const char *out, const int bs, const long long count);
int diskutil_dd2(const char *in, const char *out, const int bs, const long long count, const long long seek, const long long skip);
int diskutil_mbr(const char *path, const char *type);
int diskutil_part(const char *path, char *part_type, const char *fs_type, const long long first_sector, const long long last_sector);
int diskutil_get_parts(const char *path, struct partition_table_entry entries[], int num_entries);
sem *diskutil_get_loop_sem(void);
int diskutil_loop_check(const char *path, const char *lodev);
int diskutil_loop(const char *path, const long long offset, char *lodev, int lodev_size);
int diskutil_unloop(const char *lodev);
int diskutil_mkswap(const char *lodev, const long long size_bytes);
int diskutil_mkfs(const char *lodev, const long long size_bytes);
int diskutil_tune(const char *lodev);
int diskutil_sectors(const char *path, const int part, long long *first, long long *last);
int diskutil_write2file(const char *file, const char *str);
int diskutil_ch(const char *path, const char *user, const char *group, const int perms);
int diskutil_mkdir(const char *path);
int diskutil_cp(const char *from, const char *to);

long long round_up_sec(long long bytes);
long long round_down_sec(long long bytes);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to round up to sector size
#define ROUND_UP_SECTOR(_bytes)        (((_bytes) % SECTOR_SIZE) ? ((((_bytes) / SECTOR_SIZE) + 1) * SECTOR_SIZE) : (_bytes));

//! Macro to round down to sector size
#define ROUND_DOWN_SECTOR(_bytes)      (((_bytes) % SECTOR_SIZE) ? ((((_bytes) / SECTOR_SIZE)) * SECTOR_SIZE) : (_bytes));

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_DISKUTIL_H_ */

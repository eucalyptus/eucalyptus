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

#ifndef _INCLUDE_DISKFILE_H_
#define _INCLUDE_DISKFILE_H_

//!
//! @file storage/imager/diskfile.h
//! Need Description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <misc.h>                      // boolean

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAX_PARTS                                  20

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

typedef struct _diskpart {
    // public fields
    s64 size_bytes;
    enum diskpart_t {                  //!< partition ids (as known by MBR)
        DISKPART_UNKNOWN = 0,
        DISKPART_SWAP,
        DISKPART_EXT234,
        DISKPART_WINDOWS               //!< not supported yet
    } type;
    // private fields
    enum pformat_t {                   //!< partition formats (swap, file system types)
        PFORMAT_UNKNOWN = 0,
        PFORMAT_EXT3,
        PFORMAT_SWAP,
        PFORMAT_NTFS
    } format;
    s64 first_sector;
    s64 last_sector;
    char lodev[EUCA_MAX_PATH];
    char mntpt[EUCA_MAX_PATH];
} diskpart;

typedef struct _diskfile {
    // private fields
    char path[EUCA_MAX_PATH];
    s64 limit_bytes;
    s64 size_sectors;
    enum mbr_t {
        MBR_NONE = 0,
        MBR_MSDOS
    } mbr;
    diskpart parts[MAX_PARTS];
    int nparts;
} diskfile;

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

s64 mbr_size_bytes(void);

diskfile *df_create(const char *path, const s64 limit_bytes, boolean zero_fill) _attribute_wur_;
diskfile *df_open(const char *path) _attribute_wur_;

int df_partition(diskfile * df, enum mbr_t type, diskpart parts[]);
int df_format(diskfile * df, const int part, const enum diskpart_t format);
int df_dd(diskfile * df, const int part, const char *path);
int df_mount(diskfile * df, const int part, const char *path);
int df_umount(diskfile * df, const int part, boolean tune2fs);
int df_grub(diskfile * df, const int part);
int df_close(diskfile * df);

enum mbr_t parse_mbr_t_enum(const char *s);
const char *enum_diskpart_t_string(enum diskpart_t t);
s64 get_min_size(enum diskpart_t t);
enum diskpart_t pformat_to_diskpart_t(enum pformat_t fmt);
const char *enum_format_as_string(enum pformat_t t);
enum pformat_t parse_pformat_t_enum(const char *s);

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

#define DF_CLOSE(_df) \
{                     \
    df_close((_df));  \
    (_df) = NULL;     \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_DISKFILE_H_ */

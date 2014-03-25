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

#ifndef _INCLUDE_CACHE_H_
#define _INCLUDE_CACHE_H_

//!
//! @file storage/imager/cache.h
//! Need Description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <eucalyptus.h>                // EUCA_MAX_PATH
#include <misc.h>                      // boolean
#include "imager.h"                    // imager_request

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAX_DEPS                                 16

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

typedef struct _disk_item {
    char id[EUCA_MAX_PATH];
    char base[EUCA_MAX_PATH];
    char path[EUCA_MAX_PATH];
    char summ[EUCA_MAX_PATH];
    boolean cache_item;
    s64 content_size;
    s64 total_size;
    struct _disk_item *next;
    struct _disk_item *prev;
} disk_item;

typedef struct _artifacts_spec {
    struct _artifacts_spec *deps[MAX_DEPS];
    imager_request *req;               //!< the request associated with this stage
    imager_param *attrs;               //!< attributes uniquely describing artifacts of this stage
    s64 size;                          //!< total expected size of artifacts produced by this stage
} artifacts_spec;

typedef struct _output_file {
    char id[EUCA_MAX_PATH];
    char path[EUCA_MAX_PATH];

    boolean predecessor;

    boolean do_work;
    boolean in_work;

    boolean do_cache;
    boolean in_cache;

    boolean to_cache;

    disk_item *cache_copy;
    disk_item *work_copy;
} output_file;

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

//! @{
//! @name functions for managing the cache directory
void lock_cache(void);
void unlock_cache(void);

int set_cache_limit(s64 size);
s64 get_cache_limit(void);
int set_cache_dir(const char *path);
char *get_cache_dir(void);
int clean_cache_dir(blobstore * cache_bs);
int reserve_cache_slot(disk_item * di);
void return_cache_path(s64 size);
s64 scan_cache(void);
//! @}

//! @{
//! @name functions for managing the work directory
int set_work_limit(s64 size);
s64 get_work_limit(void);
int set_work_dir(const char *path);
char *get_work_dir(void);
int clean_work_dir(blobstore * work_bs);
int reserve_work_path(s64 size);
void return_work_path(s64 size);
//! @}

char *alloc_tmp_file(const char *name_base, s64 size) _attribute_wur_;
void free_tmp_file(char *path, s64 size);

//! @{
//! @name disk item methods
int lock_disk_item(disk_item * di);
int unlock_disk_item(disk_item * di);
disk_item *alloc_disk_item(const char *id, const s64 content_size, const s64 total_size, boolean cache_item) _attribute_wur_;
int free_disk_item(disk_item * di);
int reserve_disk_item(disk_item * di);
int add_summ_disk_item(disk_item * di, artifacts_spec * spec);
int delete_disk_item(disk_item * di);
disk_item *find_disk_item(const char *id, const boolean cache_item) _attribute_wur_;
int verify_disk_item(const disk_item * di, artifacts_spec * spec);
int copy_disk_item(disk_item * src, disk_item * dst);
char *get_disk_item_path(disk_item * di);
//! @}

//! @{
//! @name artifacts spec methods
artifacts_spec *alloc_artifacts_spec(imager_request * req, char **attrs) _attribute_wur_;
void free_artifacts_spec(artifacts_spec * spec);
//! @}

//! @{
//! @name postprocess/preprocess methods
output_file *preprocess_output_path(const char *id, artifacts_spec * spec, boolean use_work, boolean use_cache, artifacts_spec * prev_spec) _attribute_wur_;
void postprocess_output_path(output_file * o, boolean success);
//! @}

void rm_workfile(const char *filename);

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

//! Macro to delete a previously created temporary file. Ensures path is set to NULL.
#define FREE_TMP_FILE(_path, _size)   \
{                                     \
    free_tmp_file((_path), (_size));  \
    (_path) = NULL;                   \
}

//! Macro to free a previously allocated disk item. Ensures di is set to NULL.
#define FREE_DISK_ITEM(_di)  \
{                            \
    free_disk_item((_di));   \
    (_di) = NULL;            \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_CACHE_H_ */

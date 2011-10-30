// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

#ifndef _CACHE_H
#define _CACHE_H

#include "eucalyptus.h" // EUCA_MAX_PATH
#include "misc.h" // boolean
#include "imager.h" // imager_request

typedef struct _disk_item {
    char id [EUCA_MAX_PATH];
    char base [EUCA_MAX_PATH];
    char path [EUCA_MAX_PATH];
    char summ [EUCA_MAX_PATH];
    boolean cache_item;
    long long content_size;
    long long total_size;
    struct _disk_item * next;
    struct _disk_item * prev;
} disk_item;

#define MAX_DEPS 16

typedef struct _artifacts_spec {
    struct _artifacts_spec * deps [MAX_DEPS];
    imager_request * req; // the request associated with this stage
    imager_param * attrs; // attributes uniquely describing artifacts of this stage
    long long size; // total expected size of artifacts produced by this stage
} artifacts_spec;

typedef struct _output_file {
    char id [EUCA_MAX_PATH];
    char path [EUCA_MAX_PATH];

    boolean predecessor;

    boolean do_work;
    boolean in_work;

    boolean do_cache;
    boolean in_cache;

    boolean to_cache;

    disk_item * cache_copy;
    disk_item * work_copy;
} output_file;

void lock_cache (void);
void unlock_cache (void);

int lock_disk_item (disk_item * di);
int unlock_disk_item (disk_item * di);

long long scan_cache (void);

int set_cache_limit (long long size);
long long get_cache_limit (void);
int set_cache_dir (const char * path);
char * get_cache_dir (void);

int set_work_limit (long long size);
long long get_work_limit (void);
int set_work_dir (const char * path);
int clean_work_dir (blobstore * work_bs);
char * get_work_dir (void);

disk_item * alloc_disk_item (const char * id, const long long content_size, const long long total_size, boolean cache_item);
int free_disk_item (disk_item * di);
int reserve_disk_item (disk_item * di);
int add_summ_disk_item (disk_item * di, artifacts_spec * spec);
int delete_disk_item (disk_item * di);

disk_item * find_disk_item (const char * id, const boolean cache_item);
int verify_disk_item (const disk_item * di, artifacts_spec * spec);
int copy_disk_item (disk_item * src, disk_item * dst);
char * get_disk_item_path (disk_item * di);

char * alloc_tmp_file (const char * name_base, long long size);
void free_tmp_file (char * name, long long size);

artifacts_spec * alloc_artifacts_spec (imager_request * req, char ** attrs);
void free_artifacts_spec (artifacts_spec * spec);

output_file * preprocess_output_path (const char * id, artifacts_spec * spec, boolean use_work, boolean use_cache, artifacts_spec * prev_spec);
void postprocess_output_path (output_file * o, boolean success);

void rm_workfile (const char * filename);

#endif // _CACHE_H

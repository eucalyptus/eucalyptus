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
//! @file storage/imager/cache.c
//! Need Description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>                    // close
#include <time.h>                      // time
#include <errno.h>                     // errno
#include <sys/types.h>                 // *dir, etc
#include <limits.h>
#include <dirent.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include "cache.h"
#include "imager.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define EUCA_SIZE_UNLIMITED                      LLONG_MAX

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

static disk_item *cache_head = NULL;
static s64 cache_used = 0L;
static s64 cache_limit = EUCA_SIZE_UNLIMITED;   //!< size limit for cache space
static char cache_path[EUCA_MAX_PATH] = "./euca-imager-cache.d";  //!< default cache dir

static s64 work_used = 0L;
static s64 work_limit = EUCA_SIZE_UNLIMITED;    //!< size limit for work space
static char work_path[EUCA_MAX_PATH] = "."; //!< cwd is default work dir
static boolean work_was_created = FALSE;
static boolean cache_was_created = FALSE;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static disk_item *find_in_cache(const char *path) _attribute_wur_;
static void add_to_cache(disk_item * di);
static void print_cache(void);

static size_t catprintfn(char *buf, const size_t len, const char *format, ...) _attribute_format_(3, 4);

static char *gen_summary(artifacts_spec * spec) _attribute_wur_;

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
//! @see unlock_caches()
//!
void lock_cache(void)
{

}

//!
//!
//!
//! @see lock_cache()
//!
void unlock_cache(void)
{

}

//!
//!
//!
//! @param[in] size the new cache limit size
//!
//! @return Always returns 0 for now
//!
//! @see get_cache_limit()
//!
int set_cache_limit(s64 size)
{
    cache_limit = EUCA_SIZE_UNLIMITED;
    if (size >= 0)
        cache_limit = size;
    return 0;
}

//!
//! Retrieves the size of the cache limit
//!
//! @return The cache limit size
//!
//! @see set_cache_limit()
//!
s64 get_cache_limit(void)
{
    return cache_limit;
}

//!
//!
//!
//! @param[in] path
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_ERROR if any error occured
//!         \li EUCA_INVALID_ERROR if any parameter does not meet the pre-conditions
//!
//! @see get_cache_dir()
//!
//! @pre The path field must not be NULL.
//!
//! @post Uppon success, the cache_path global field is set with the given path.
//!
int set_cache_dir(const char *path)
{
    struct stat mystat = { 0 };

    if (path == NULL) {
        LOGERROR("failed to set cache directory to NULL path\n");
        return EUCA_INVALID_ERROR;
    }

    if (stat(path, &mystat)) {
        // remember that cache directory had to be created
        cache_was_created = TRUE;
    }

    if (ensure_path_exists(path, 0700)) {
        LOGERROR("failed to set cache directory to '%s'\n", path);
        return EUCA_ERROR;
    }

    euca_strncpy(cache_path, path, EUCA_MAX_PATH);
    return EUCA_OK;
}

//!
//! Retrieves the current cache path
//!
//! @return The cache path
//!
//! @see set_cache_dir()
//!
char *get_cache_dir(void)
{
    return cache_path;
}

//!
//! Purges blobs from the cache directory
//!
//! @param[in] cache_bs
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_SYSTEM_ERROR if call to rmdir() failed
//!
//! @pre The cache_bs field must not be NULL
//!
//! @post The working directory has been cleaned and deleted.
//!
int clean_cache_dir(blobstore * cache_bs)
{
    if (cache_bs) {
        if (blobstore_delete(cache_bs) == EUCA_OK) {
            if (cache_was_created) {
                return ((rmdir(get_cache_dir()) == 0) ? EUCA_OK : EUCA_SYSTEM_ERROR);
            }
        } else {
            return EUCA_SYSTEM_ERROR;
        }
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] path
//!
//! @return
//!
//! @pre The path field must not be NULL.
//!
static disk_item *find_in_cache(const char *path)
{
    disk_item *p = NULL;

    if (path != NULL) {
        for (p = cache_head; p != NULL; p = p->next) {
            if (strncmp(p->path, path, EUCA_MAX_PATH) == 0)
                break;
        }
    }
    return p;
}

//!
//!
//!
//! @param[in] di
//!
//! @pre The di field must not be NULL
//!
//! @post Uppon success, the disk item is added to the cache list
//!
static void add_to_cache(disk_item * di)
{
    disk_item *p = NULL;
    disk_item **pp = NULL;

    if (di == NULL) {
        LOGFATAL("NULL disk item in add_to_cache()\n");
    } else {
        di->next = NULL;
        di->prev = NULL;

        // run through the LL of cache entries
        for (pp = &cache_head; *pp != NULL; pp = &((*pp)->next)) {
            p = *pp;

            // if entry is alredy there, splice in the new struct into LL
            if (strncmp(p->base, di->base, EUCA_MAX_PATH) == 0) {
                LOGWARN("refreshing cache entry (%s)\n", di->base);
                di->prev = p->prev;
                di->next = p->next;

                if (di->prev)
                    di->prev->next = di;

                if (di->next)
                    di->next->prev = di;

                cache_used -= p->total_size;
                FREE_DISK_ITEM(p);
                pp = NULL;
                break;
            }
        }

        // if entry wasn't in the LL, add it at the end
        if (p) {
            // if second or later entry
            di->prev = p;
            p->next = di;
        }
        // if first entry
        if (pp) {
            *pp = di;
        }
        cache_used += di->total_size;
    }
}

//!
//! Prints the content of the cache in the logs
//!
static void print_cache(void)
{
    disk_item *e = NULL;
    struct stat mystat = { 0 };

    if (cache_limit == EUCA_SIZE_UNLIMITED) {
        LOGINFO("cached objects used %ldMB of UNLIMITED cache\n", (cache_used / MEGABYTE));
    } else {
        LOGINFO("cached images used %ld of %ldMB\n", (cache_used / MEGABYTE), (cache_limit / MEGABYTE));
    }

    for (e = cache_head; e; e = e->next) {
        bzero(&mystat, sizeof(mystat));
        if (stat(e->path, &mystat) == 0) {
            LOGINFO("- %s total_size=%ldMB ts=%8ldsec base=%s \n", e->id, (e->total_size / MEGABYTE), mystat.st_mtime, e->base);
        }
    }
}

//!
//!
//!
//! @param[in] di
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR on failure.
//!         \li EUCA_INVALID_ERROR if any parameter does not meet the pre-conditions
//!
//! @pre The di field must not be NULL.
//!
//! @post
//!
int reserve_cache_slot(disk_item * di)
{
    off_t oldest_size = 0;
    time_t oldest_mtime = 0;
    disk_item *e = NULL;
    disk_item *oldest_entry = NULL;
    struct stat mystat = { 0 };

    if (di == NULL)
        return EUCA_INVALID_ERROR;

    //! @TODO check if there is enough disk space?
    if (cache_limit == EUCA_SIZE_UNLIMITED) {   // cache is always big enough, we never purge
        cache_used += di->total_size;
        goto create;
    }

    if (di->total_size > cache_limit)  // cache isn't big enough
        return EUCA_ERROR;

    while (di->total_size > (cache_limit - cache_used)) {
        oldest_mtime = time(NULL) + 1;
        oldest_size = 0;
        oldest_entry = NULL;

        // run through cache and find the LRU entry
        for (e = cache_head; e; e = e->next) {
            if (stat(e->path, &mystat) < 0) {
                LOGERROR("can't stat %s\n", e->path);
                return EUCA_ERROR;
            }

            if (mystat.st_mtime < oldest_mtime) {
                oldest_mtime = mystat.st_mtime;
                oldest_size = e->total_size;    // (mystat.st_size doesn't include digest)
                oldest_entry = e;
            } else {
                if (mystat.st_mtime == oldest_mtime) {  // with same age, smaller ones get purged first
                    if (oldest_size > e->total_size) {
                        oldest_size = e->total_size;
                        oldest_entry = e;
                    }
                }
            }
        }

        if (oldest_entry) {            // remove it
            LOGINFO("purging from cache entry %s\n", oldest_entry->base);
            if (oldest_entry->next) {
                oldest_entry->next->prev = oldest_entry->prev;
            }

            if (oldest_entry->prev) {
                oldest_entry->prev->next = oldest_entry->next;
            } else {
                cache_head = oldest_entry->next;
            }

            delete_disk_item(oldest_entry);
            cache_used -= oldest_entry->total_size;
            FREE_DISK_ITEM(oldest_entry);
        } else {
            LOGERROR("cannot find oldest entry in cache\n");
            return EUCA_ERROR;
        }
    }

create:
    if (ensure_path_exists(di->base, 0700)) {
        LOGERROR("failed to create cache entry '%s'\n", di->base);
        return EUCA_ERROR;
    }

    add_to_cache(di);
    return EUCA_OK;
}

//!
//!
//!
//! @param[in] size
//!
void return_cache_path(s64 size)
{
    cache_used -= size;
}

//!
//!
//!
//! @return The size of the cache or -1L if any error occured
//!
s64 scan_cache(void)
{
    s64 total_size = 0;
    s64 image_size = 0;
    s64 content_size = 0;
    int image_files = 0;
    DIR *cache_dir = NULL;
    DIR *image_dir = NULL;
    char *image_name = NULL;
    char name[EUCA_MAX_PATH] = "";
    char filepath[EUCA_MAX_PATH] = "";
    char image_path[EUCA_MAX_PATH] = "";
    boolean found_content = FALSE;
    boolean found_summary = FALSE;
    disk_item *d = NULL;
    disk_item *di = NULL;
    disk_item *prev_d = NULL;
    struct stat mystat = { 0 };
    struct dirent *cache_dir_entry = NULL;
    struct dirent *image_dir_entry = NULL;

    // remove old LL
    while ((d = cache_head) != NULL) {
        prev_d = d;
        d = d->next;
        prev_d->next = NULL;
        prev_d->prev = NULL;
        FREE_DISK_ITEM(prev_d);
    }
    cache_head = NULL;

    LOGINFO("scanning the cache directory (%s)\n", cache_path);

    if (strlen(cache_path) == 0) {
        LOGINFO("no cache directory yet\n");
        return total_size;
    }

    if (stat(cache_path, &mystat) < 0) {
        LOGWARN("could not stat %s\n", cache_path);
        return -1L;
    }
    total_size += mystat.st_size;

    if ((cache_dir = opendir(cache_path)) == NULL) {
        LOGFATAL("could not open cache directory %s\n", cache_path);
        return -1L;
    }
    // iterate over all directories in cache directory
    while ((cache_dir_entry = readdir(cache_dir)) != NULL) {
        image_name = cache_dir_entry->d_name;
        image_size = 0;
        content_size = 0;
        image_files = 0;

        if (!strcmp(".", image_name) || !strcmp("..", image_name))
            continue;

        snprintf(image_path, EUCA_MAX_PATH, "%s/%s", cache_path, image_name);
        if ((image_dir = opendir(image_path)) == NULL) {
            LOGWARN("unopeneable directory %s\n", image_path);
            continue;
        }

        if (stat(image_path, &mystat) < 0) {
            LOGWARN("could not stat %s\n", image_path);
            closedir(image_dir);
            continue;
        }
        image_size += mystat.st_size;

        // add up sizes of all files in a directory
        found_content = FALSE;
        found_summary = FALSE;
        while ((image_dir_entry = readdir(image_dir)) != NULL) {
            strncpy(name, image_dir_entry->d_name, EUCA_MAX_PATH);
            if (!strcmp(".", name) || !strcmp("..", name))
                continue;

            image_files++;

            snprintf(filepath, EUCA_MAX_PATH, "%s/%s", image_path, name);
            if (stat(filepath, &mystat) < 0) {
                LOGERROR("could not stat file %s\n", filepath);
                break;
            }

            if (mystat.st_size < 1) {
                LOGERROR("empty file among cached images in '%s'\n", filepath);
                break;
            }

            if (!strcmp("content", name)) {
                content_size = mystat.st_size;
                found_content = TRUE;
            }

            if (!strcmp("summary", name))
                found_summary = TRUE;

            image_size += mystat.st_size;
        }
        closedir(image_dir);

        // report on what was found in each cache directory
        if (image_files > 0) {
            // ignore empty directories
            if (image_size > 0) {
                LOGINFO("- cached image '%s': size=%ldMB, files=%d\n", image_name, (image_size / MEGABYTE), image_files);
                total_size += image_size;

                // allocate a disk item object
                di = alloc_disk_item(image_name, content_size, image_size, TRUE);
                add_to_cache(di);
            } else {
                LOGWARN("empty cached image directory %s\n", image_path);
            }
        }
    }
    closedir(cache_dir);
    print_cache();
    return total_size;
}

//!
//!
//!
//! @param[in] size
//!
//! @return Always return 0 for now.
//!
//! @see get_work_limit()
//!
int set_work_limit(s64 size)
{
    work_limit = EUCA_SIZE_UNLIMITED;
    if (size >= 0)
        work_limit = size;
    return 0;
}

//!
//!
//!
//! @return the work limit size
//!
//! @see set_work_limit()
//!
s64 get_work_limit(void)
{
    return work_limit;
}

//!
//! Sets the working directory path
//!
//! @param[in] path the working directory path string
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR if any error occured
//!         \li EUCA_INVALID_ERROR if any parameter does not meet the pre-condition.
//!
//! @see get_work_dir()
//!
//! @pre The path field must not be NULL
//!
//! @post The work_path global variable is set
//!
int set_work_dir(const char *path)
{
    struct stat mystat = { 0 };

    if (path == NULL)
        return (EUCA_INVALID_ERROR);

    if (stat(path, &mystat)) {
        // remember that work directory had to be created
        work_was_created = TRUE;
    }

    if (ensure_path_exists(path, 0700)) {
        LOGERROR("failed to set work directory to '%s'\n", path);
        return EUCA_ERROR;
    }

    euca_strncpy(work_path, path, EUCA_MAX_PATH);
    return EUCA_OK;
}

//!
//! Retrieves the working directory path
//!
//! @return The working directory path
//!
//! @see set_work_dir()
//!
char *get_work_dir(void)
{
    return work_path;
}

//!
//!
//!
//! @param[in] work_bs
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_SYSTEM_ERROR if call to rmdir() failed
//!
//! @pre The work_bs field must not be NULL
//!
//! @post The working directory has been cleaned and deleted.
//!
int clean_work_dir(blobstore * work_bs)
{
    char path[EUCA_MAX_PATH] = "";

    sprintf(path, "%s/imager.pid", get_work_dir());
    unlink(path);

    if (work_bs)
        blobstore_delete(work_bs);     // not fully implemented, but will delete artifacts of an empty blobstore

    if (work_was_created)
        return ((rmdir(get_work_dir()) == 0) ? EUCA_OK : EUCA_SYSTEM_ERROR);
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] size
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR if any failure occured.
//!
int reserve_work_path(s64 size)
{
    //! @TODO check if there is enough disk space?
    work_used += size;
    if ((work_limit != EUCA_SIZE_UNLIMITED) && (work_used > work_limit)) {
        work_used -= size;
        return (EUCA_ERROR);
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] size
//!
void return_work_path(s64 size)
{
    work_used -= size;
}

//!
//! Allocates a temporary file
//!
//! @param[in] name_base the base name of the file
//! @param[in] size the size of data to reserve under the work path
//!
//! @return The full path name to the temporary file on success or NULL on error.
//!
//! @see free_tmp_file()
//!
//! @pre \li The name_base field must not be NULL.
//!      \li There should be enough space on the work path to reserve size bytes.
//!
//! @post Uppon success the temporary file is created and the memory has been reserved
//!       for the workspace. Uppon failure, the temporary file has not been created and
//!       no memory has been reserved.
//!
char *alloc_tmp_file(const char *name_base, s64 size)
{
    int tmp_fd = 0;
    char *path = NULL;

    if (!name_base) {
        LOGERROR("Invalid name_base parameter provided.\n");
        return (NULL);
    }

    if (reserve_work_path(size) != EUCA_OK) {
        LOGERROR("out of work disk space for a temporary file\n");
        return NULL;
    }

    if ((path = EUCA_ALLOC(EUCA_MAX_PATH, sizeof(char))) == NULL) {
        LOGERROR("out of memory\n");
        return_work_path(size);
        return NULL;
    }

    snprintf(path, EUCA_MAX_PATH, "%s/%s-XXXXXX", get_work_dir(), name_base);
    if ((tmp_fd = safe_mkstemp(path)) < 0) {
        LOGERROR("failed to create a temporary file under %s\n", path);
        EUCA_FREE(path);
        return_work_path(size);
        return NULL;
    }

    close(tmp_fd);
    return path;
}

//!
//! Deletes a previously created temporary file.
//!
//! @param[in] path the path to the temporaty file to delete
//! @param[in] size the reserved size for this temporary file
//!
//! @see alloc_tmp_file()
//!
//! @pre The path field must not be NULL.
//!
//! @post The temporary file has been removed from disk and the reserved
//!       memory in the work path has been reclaimed. The path field memory
//!       will be freed.
//!
void free_tmp_file(char *path, s64 size)
{
    if (path) {
        unlink(path);
        return_work_path(size);
        EUCA_FREE(path);
    }
}

//!
//!
//!
//! @param[in] di a pointer to the disk item structure to gather a lock from
//!
//! @return Always return 0 for now.
//!
//! @see unlock_disk_item()
//!
int lock_disk_item(disk_item * di)
{
    return 0;
}

//!
//!
//!
//! @param[in] di a pointer to teh disk item structure to release a lock from
//!
//! @return Always return 0 for now.
//!
//! @see lock_disk_item()
//!
int unlock_disk_item(disk_item * di)
{
    return 0;
}

//!
//! Allocates disc item struct in memory, but does nothing on disk
//!
//! @param[in] id
//! @param[in] content_size
//! @param[in] total_size
//! @param[in] cache_item
//!
//! @return A pointer to the allocated disc item memory or NULL on failure.
//!
//! @see free_disk_item()
//!
disk_item *alloc_disk_item(const char *id, const s64 content_size, const s64 total_size, boolean cache_item)
{
    disk_item *di = NULL;

    if ((di = EUCA_ZALLOC(1, sizeof(disk_item))) == NULL) {
        LOGERROR("out of memory\n");
        return NULL;
    }

    euca_strncpy(di->id, id, EUCA_MAX_PATH);
    if (cache_item) {
        snprintf(di->base, EUCA_MAX_PATH, "%s/%s", get_cache_dir(), di->id);
        snprintf(di->path, EUCA_MAX_PATH, "%s/content", di->base);
        snprintf(di->summ, EUCA_MAX_PATH, "%s/summary", di->base);
    } else {
        snprintf(di->base, EUCA_MAX_PATH, "%s", get_work_dir());
        snprintf(di->path, EUCA_MAX_PATH, "%s/%s", di->base, di->id);
        snprintf(di->summ, EUCA_MAX_PATH, "%s/%s-summary", di->base, di->id);
    }

    di->cache_item = cache_item;
    di->total_size = total_size;
    di->content_size = content_size;

    return di;
}

//!
//! Frees the disk item struct from memory, but does nothing on disk
//!
//! @param[in] di a pointer to the disk item structure to free
//!
//! @return Always return 0 for now
//!
//! @see alloc_disk_item()
//!
int free_disk_item(disk_item * di)
{
    EUCA_FREE(di);
    return (0);
}

//!
//! Ensures there is space for the item, purging cache if necessary
//!
//! @param[in] di
//!
//! @return The result of reserve_cache_slot() if di->cache_item is set or reserve_work_path()
//!         otherwise.
//!
//! @see reserve_cache_slot(), reserve_work_path()
//!
int reserve_disk_item(disk_item * di)
{
    LOGINFO("reserving room for item '%s' in %s space\n", di->id, di->cache_item ? "cache" : "work");
    if (di->cache_item) {
        return reserve_cache_slot(di);
    }
    return reserve_work_path(di->total_size);
}

//!
//! Like printfn, but concatenates to the buffer rather than overwrites it
//! hence, the string in buf must be null-terminated
//!
//! @param[in] buf
//! @param[in] len
//! @param[in] format
//! @param[in] ...
//!
//! @return The amount of characters inside the given buffer (including the offset).
//!
static size_t catprintfn(char *buf, const size_t len, const char *format, ...)
{
    int printed = 0;
    size_t i = 0;
    size_t offset = len + 1;
    va_list ap = { {0} };

    // avoid strlen() in case string in buf is not terminated
    for (i = 0; i < len; i++) {
        if (buf[i] == '\0') {
            offset = i;
            break;
        }
    }

    if (offset >= len)
        return offset;

    va_start(ap, format);
    printed = vsnprintf((buf + offset), (len - offset), format, ap);
    va_end(ap);

    if (printed > 0)
        return (offset + printed);
    return (offset);
}

//!
//! Generates, in memory, the summary string for a stage, including previous stages
//!
//! @param[in] spec
//!
//! @return The summary string
//!
//! @note The caller must free the return memory
//!
static char *gen_summary(artifacts_spec * spec)
{
    char buf[4096] = "";
    char *dep_summary = NULL;
    imager_param *p = NULL;
    artifacts_spec **s = NULL;

    catprintfn(buf, sizeof(buf), "operation: %s\n", spec->req->cmd->name);
    for (p = spec->attrs; p->key; p++) {
        catprintfn(buf, sizeof(buf), "\t%s: %s\n", p->key, p->val);
    }

    for (s = spec->deps; ((*s) && ((s - spec->deps) < MAX_DEPS)); s++) {
        dep_summary = gen_summary(*s); // yay recursion!
        catprintfn(buf, sizeof(buf), "%s", dep_summary);
        EUCA_FREE(dep_summary);
    }

    return (strdup(buf));
}

//!
//! Saves the summary of disk item to disk
//!
//! @param[in] di
//! @param[in] spec
//!
//! @return EUCA_ERROR on failure or the result of write2file().
//!
//! @see write2file()
//!
int add_summ_disk_item(disk_item * di, artifacts_spec * spec)
{
    int ret = EUCA_ERROR;
    char *summary = NULL;

    if ((summary = gen_summary(spec)) != NULL) {
        ret = write2file(di->summ, summary);
        EUCA_FREE(summary);
    }

    return ret;
}

//!
//! Deletes on disk state for the item, but does nothing in memory
//!
//! @param[in] di
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int delete_disk_item(disk_item * di)
{
    int ret = EUCA_OK;

    if (unlink(di->path)) {
        LOGERROR("failed to delete '%s'\n", di->path);
        ret = EUCA_ERROR;
    }

    if (unlink(di->summ)) {
        LOGWARN("failed to delete summary file '%s'\n", di->summ);
    }

    if (di->cache_item) {
        if (rmdir(di->base)) {
            LOGERROR("failed to delete '%s'\n", di->base);
            ret = EUCA_ERROR;
        }
    }

    return ret;
}

//!
//! Finds disc item on disk (in work or cache), allocates and fills out memory struct for it
//!
//! @param[in] id
//! @param[in] cache_item
//!
//! @return A pointer to the disk item if found or a newly created disk item if not found.
//!
//! @see alloc_disk_item(), find_in_cache()
//!
disk_item *find_disk_item(const char *id, const boolean cache_item)
{
    char path[EUCA_MAX_PATH] = "";
    struct stat mystat = { 0 };

    if (cache_item) {
        snprintf(path, EUCA_MAX_PATH, "%s/%s/content", get_cache_dir(), id);
    } else {
        snprintf(path, EUCA_MAX_PATH, "%s/%s", get_work_dir(), id);
    }

    if (cache_item) {
        scan_cache();                  // rebuilds cache state in memory based on disk
        return find_in_cache(path);
    }

    if (stat(path, &mystat) < 0) {
        if (errno != ENOENT) {         // file not found
            LOGERROR("could not stat '%s'\n", path);
        }
        return NULL;
    }

    return alloc_disk_item(id, mystat.st_size, mystat.st_size, cache_item);
}

//!
//! Checks whether disk state of the item matches the spec (when it does not, function returns 1)
//!
//! @param[in] di
//! @param[in] spec
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int verify_disk_item(const disk_item * di, artifacts_spec * spec)
{
    int ret = EUCA_ERROR;
    char *file_summary = NULL;
    char *spec_summary = NULL;
    struct stat mystat = { 0 };

    if (stat(di->path, &mystat) < 0)
        return EUCA_ERROR;

    if (di->content_size != mystat.st_size)
        return EUCA_ERROR;

    //! @TODO check the checksum?
    file_summary = file2str(di->summ);
    spec_summary = gen_summary(spec);
    if (file_summary == NULL) {
        LOGWARN("failed to read summary file '%s'\n", di->summ);
        EUCA_FREE(spec_summary);
        ret = EUCA_OK;
    } else {
        if (spec_summary != NULL) {
            if (strcmp(file_summary, spec_summary) == 0)
                ret = EUCA_OK;
            EUCA_FREE(spec_summary);
        }
        EUCA_FREE(file_summary);
    }

    return ret;
}

//!
//! Copies the item disk state from one item to another
//!
//! @param[in] src
//! @param[in] dst
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int copy_disk_item(disk_item * src, disk_item * dst)
{
    if (copy_file(src->path, dst->path))
        return EUCA_ERROR;

    if (copy_file(src->summ, dst->summ))
        return EUCA_ERROR;
    return EUCA_OK;
}

//!
//! Returns the path of the item contents on disk
//!
//! @param[in] di
//!
//! @return The path of the item contents on disk
//!
char *get_disk_item_path(disk_item * di)
{
    return di->path;
}

//!
//! Allocates artifacts struct and copies attributes into it
//!
//! @param[in] req
//! @param[in] attrs
//!
//! @return the return value description
//!
//! @see free_artifacts_spec()
//!
artifacts_spec *alloc_artifacts_spec(imager_request * req, char **attrs)
{
    int count = 0;
    char **a = NULL;
    artifacts_spec *spec = NULL;

    // create an input spec with 0s for dependencies
    if ((spec = EUCA_ZALLOC(1, sizeof(artifacts_spec))) != NULL) {
        spec->req = req;

        if ((attrs != NULL) && (*attrs != NULL)) {
            count = 0;

            // determine the number of attributes
            for (a = attrs; ((*a) && (*(a + 1))); a += 2)
                count++;

            // allocate the key-val pair array
            spec->attrs = EUCA_ZALLOC(count + 1, sizeof(imager_param)); // will be terminated with two NULL pointers
            if (spec->attrs == NULL) {
                EUCA_FREE(spec);
                return NULL;
            }
            // copy the attributes
            for (count = 0, a = attrs; *a && *(a + 1); a += 2) {
                spec->attrs[count].key = strdup(*a);
                spec->attrs[count].val = strdup(*(a + 1));
                count++;
            }
        }
    }

    return spec;
}

//
//!
//! Frees artifacts struct and the attributes therein
//!
//! @param[in] spec
//!
//! @see alloc_artifacts_spec()
//!
void free_artifacts_spec(artifacts_spec * spec)
{
    int i = 0;
    for (i = 0; spec->attrs && spec->attrs[i].key; i++) {
        EUCA_FREE(spec->attrs[i].key);
        EUCA_FREE(spec->attrs[i].val);
    }

    EUCA_FREE(spec->attrs);
    EUCA_FREE(spec);
}

//!
//! Performs tasks needed before an item can be procssed by execute():
//! checks work and cache for existing results, validates or purges them,
//! reserves space if necessary, locks the cache entry (must be released with postprocess_output_path)
//!
//! @param[in] id
//! @param[in] spec
//! @param[in] use_work
//! @param[in] use_cache
//! @param[in] prev_spec
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
output_file *preprocess_output_path(const char *id, artifacts_spec * spec, boolean use_work, boolean use_cache, artifacts_spec * prev_spec)
{
    int ret = EUCA_OK;
    char *result_path = NULL;
    output_file *o = NULL;

    // all pointers are NULL, all booleans are FALSE
    if ((o = EUCA_ZALLOC(1, sizeof(output_file))) == NULL) {
        LOGERROR("out of memory\n");
        return NULL;
    }

    o->do_work = use_work;
    o->do_cache = use_cache;
    euca_strncpy(o->id, id, sizeof(o->id));

    // is there a valid work copy already?
    o->work_copy = find_disk_item(id, FALSE);
    if (o->work_copy != NULL) {        // there is a copy
        if (verify_disk_item(o->work_copy, spec)) { // does it match the spec?
            if (prev_spec != NULL && verify_disk_item(o->work_copy, prev_spec) == EUCA_OK) {    // does it match previous stage's spec?
                LOGINFO("found previous stage's work copy of image '%s'\n", id);
                if (add_summ_disk_item(o->work_copy, spec) != EUCA_OK) {    // update the spec
                    LOGERROR("failed to update summary of work copy of '%s'\n", id);
                    ret = EUCA_ERROR;
                    goto cleanup;
                } else {
                    o->predecessor = TRUE;
                    o->in_work = TRUE;
                }
            } else {
                delete_disk_item(o->work_copy); // no => delete it
                FREE_DISK_ITEM(o->work_copy);
                LOGINFO("purged a stale work copy of image '%s'\n", id);
            }
        } else {
            o->in_work = TRUE;
            LOGINFO("found a valid work copy of image '%s'\n", id);
        }
    }

    if (o->do_work && !o->in_work) {   // no valid work copy => reserve space
        o->work_copy = alloc_disk_item(id, 0, spec->size, FALSE);
        if (o->work_copy == NULL) {
            ret = EUCA_ERROR;
            goto cleanup;
        }

        if (reserve_disk_item(o->work_copy) != EUCA_OK) {   // reserve room for the object
            LOGERROR("failed to allocate work space for '%s'\n", id);
            ret = EUCA_ERROR;
            goto cleanup;
        }

        if (add_summ_disk_item(o->work_copy, spec) != EUCA_OK) {
            LOGERROR("failed to add summary to work copy of '%s'\n", id);
            ret = EUCA_ERROR;
            goto cleanup;
        }
    }
    // if caching, is there a valid cached copy?
    if (o->do_cache) {
        lock_cache();

        o->cache_copy = find_disk_item(id, TRUE);
        if (o->cache_copy != NULL) {   // there is a copy with this id in the cache?
            lock_disk_item(o->cache_copy);  // NOTE: may be unlocked by the _delete
            if (verify_disk_item(o->cache_copy, spec)) {    // does it match the spec?
                if (prev_spec != NULL && o->work_copy != NULL && verify_disk_item(o->work_copy, prev_spec) == EUCA_OK) {    // does it match previous stage's spec?
                    LOGINFO("found previous stage's cache copy of image '%s'\n", id);
                    if (add_summ_disk_item(o->cache_copy, spec) != EUCA_OK) {   // update the spec
                        LOGERROR("failed to update summary of cache copy of '%s'\n", id);
                        delete_disk_item(o->cache_copy);
                        FREE_DISK_ITEM(o->cache_copy);
                    } else {
                        o->predecessor = TRUE;
                        o->in_cache = TRUE;
                    }
                } else {
                    delete_disk_item(o->cache_copy);    // no => purge it
                    FREE_DISK_ITEM(o->cache_copy);
                    LOGINFO("purged a stale cache copy of image '%s'\n", id);
                }
            } else {
                o->in_cache = TRUE;
                LOGINFO("found a valid cache copy of image '%s'\n", id);
            }
        }

        if (!o->in_cache) {            // not in cache => reserve space, purging LRU items if necessary
            o->cache_copy = alloc_disk_item(id, 0, spec->size, TRUE);
            if (o->cache_copy != NULL) {
                lock_disk_item(o->cache_copy);
                if (reserve_disk_item(o->cache_copy) == EUCA_OK) {  // there is room in the cache
                    if (add_summ_disk_item(o->cache_copy, spec) != EUCA_OK) {
                        LOGERROR("failed to add summary to cache copy of '%s'\n", id);
                        delete_disk_item(o->cache_copy);
                        FREE_DISK_ITEM(o->cache_copy);
                    } else {
                        o->to_cache = TRUE;
                    }
                } else {
                    LOGERROR("no room in cache for image '%s'\n", id);
                }
            }
        }
        unlock_cache();
    }

    if (!(o->in_work || o->in_cache) || o->predecessor) {
        // no local copy, so execute() will have to create it
        // => decide where the result should go: work or cache
        if (o->do_work) {
            result_path = get_disk_item_path(o->work_copy);
            o->in_work = TRUE;
        } else if (o->to_cache) {
            result_path = get_disk_item_path(o->cache_copy);
            o->in_cache = TRUE;
        } else {
            LOGERROR("no work or cache space for downloading '%s'\n", id);
            ret = EUCA_ERROR;
        }
    }

    if (result_path) {
        euca_strncpy(o->path, result_path, sizeof(o->path));
    }

cleanup:
    // in case of error: delete disk state, unlock cache item
    //! @TODO be more selective? invalidate cache?
    if (ret != EUCA_OK) {
        if (o->work_copy != NULL) {
            delete_disk_item(o->work_copy);
            FREE_DISK_ITEM(o->work_copy);
        }

        if (o->do_cache && o->cache_copy != NULL) {
            unlock_disk_item(o->cache_copy);
        }

        EUCA_FREE(o);
    }

    return o;
}

//!
//! Performs tasks after an item has been processed the execute():
//! copies the result to/from cache, unlocks the cache entry, frees memory
//!
//! @param[in] o
//! @param[in] success
//!
void postprocess_output_path(output_file * o, boolean success)
{
    if (success) {
        // copy to or from cache directory, as needed
        if (o->do_work && o->in_cache && !o->in_work) {
            LOGINFO("copying image '%s' from cache to work space\n", o->id);
            copy_disk_item(o->cache_copy, o->work_copy);
        } else if (o->in_work && o->to_cache) {
            LOGINFO("copying image '%s' from work to cache space\n", o->id);
            copy_disk_item(o->work_copy, o->cache_copy);
        }
    } else {
        //! @TODO delete the partial results?
    }

    // unlock the cache entry
    if (o->do_cache && o->cache_copy != NULL) {
        unlock_disk_item(o->cache_copy);
    }

    if (o->work_copy != NULL) {
        FREE_DISK_ITEM(o->work_copy);  // NOTE: we do not free cache_copy because it's in a LL managed by the cache
    }

    EUCA_FREE(o);
}

//
//!
//! Remove file and its summary in work directory
//!
//! @param[in] filename
//!
void rm_workfile(const char *filename)
{
    char file_path[EUCA_MAX_PATH] = "";
    char summ_path[EUCA_MAX_PATH] = "";

    snprintf(file_path, EUCA_MAX_PATH, "%s/%s", get_work_dir(), filename);
    snprintf(summ_path, EUCA_MAX_PATH, "%s/%s-summary", get_work_dir(), filename);

    unlink(file_path);                 // ignore errors
    unlink(summ_path);
}

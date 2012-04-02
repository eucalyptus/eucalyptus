// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
 * cache.c
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h> // close
#include <time.h> // time
#include <errno.h> // errno
#include <sys/types.h> // *dir, etc
#include <limits.h>
#include <dirent.h>
#include "cache.h"
#include "eucalyptus.h"
#include "imager.h"
#include "misc.h"

#define EUCA_SIZE_UNLIMITED LLONG_MAX

/////////// TODO: locks

void lock_cache (void)
{

}

void unlock_cache (void)
{

}

int lock_disk_item (disk_item * di)
{
    return 0;
}

int unlock_disk_item (disk_item * di)
{
    return 0;
}

/////////// functions for managing the cache directory

static disk_item * cache_head = NULL;
static long long cache_used = 0L;
static long long cache_limit = EUCA_SIZE_UNLIMITED; // size limit for cache space
static char cache_path [EUCA_MAX_PATH] = "./euca_imager_cache"; // default cache dir

int set_cache_limit (long long size)
{
    if (size<0)
        cache_limit = EUCA_SIZE_UNLIMITED;
    else
        cache_limit = size;
    return 0;
}

long long get_cache_limit (void)
{
    return cache_limit;
}

int set_cache_dir (const char * path)
{
    if (ensure_path_exists(path, 0700)) {
        logprintfl (EUCAERROR, "failed to set cache directory to '%s'\n", path);
        return 1;
    }
    safe_strncpy (cache_path, path, EUCA_MAX_PATH);
    return 0;
}

char * get_cache_dir (void)
{
    return cache_path;
}

static disk_item * find_in_cache (const char * path)
{
    disk_item * p;

    for ( p = cache_head; p != NULL; p = p->next ) {
        if (strncmp (p->path, path, EUCA_MAX_PATH)==0)
            break;
    }

    return p;
}

static void add_to_cache (disk_item * di)
{
    if (di==NULL) {
        logprintfl (EUCAFATAL, "error: NULL disk item in add_to_cache()\n");
        return;
    }

    di->next = NULL;
    di->prev = NULL;

    // run through the LL of cache entries
    disk_item ** pp;
    disk_item * p = NULL;
    for ( pp = & cache_head; * pp != NULL; pp = & ((* pp)->next)) {
        p = * pp;

        // if entry is alredy there, splice in the new struct into LL
        if (strncmp(p->base, di->base, EUCA_MAX_PATH)==0) {
            logprintfl (EUCAWARN, "warning: refreshing cache entry (%s)\n", di->base);
            di->prev = p->prev;
            di->next = p->next;
            if (di->prev)
                di->prev->next = di;
            if (di->next)
                di->next->prev = di;
            cache_used -= p->total_size;
            free_disk_item (p);
            pp = NULL;
            p = NULL;
            break;
        }
    }

    // if entry wasn't in the LL, add it at the end
    if ( p ) { // if second or later entry
        di->prev = p;
        p->next = di;
    }
    if ( pp ) { // if first entry
        * pp = di;
    }
    cache_used += di->total_size;
}

static void print_cache (void)
{
    struct stat mystat;
    disk_item * e;

    if (cache_limit == EUCA_SIZE_UNLIMITED) {
        logprintfl (EUCAINFO, "cached objects used %lldMB of UNLIMITED cache\n", cache_used/MEGABYTE);
    } else {
        logprintfl (EUCAINFO, "cached images used %lld of %lldMB\n", cache_used/MEGABYTE, cache_limit/MEGABYTE);
    }
    for ( e = cache_head; e; e=e->next) {
        bzero (&mystat, sizeof (mystat));
        if (stat (e->path, &mystat) == 0) {
            logprintfl (EUCAINFO, "- %s total_size=%dMB ts=%8dsec base=%s \n", e->id, e->total_size/MEGABYTE, mystat.st_mtime, e->base);
        }
    }
}

int reserve_cache_slot (disk_item * di)
{
    // TODO: check if there is enough disk space?

    if (cache_limit==EUCA_SIZE_UNLIMITED) { // cache is always big enough, we never purge
        cache_used += di->total_size;
        goto create;
    }

    if (di->total_size > cache_limit) // cache isn't big enough
        return ERROR;

    while (di->total_size > (cache_limit-cache_used) ) {
        time_t oldest_mtime = time (NULL) + 1;
        off_t  oldest_size = 0;
        disk_item * oldest_entry = NULL;
        struct stat mystat;
        disk_item * e;

        // run through cache and find the LRU entry
        for ( e = cache_head; e; e=e->next) {
            if (stat (e->path, &mystat)<0) {
                logprintfl (EUCAERROR, "error: ok_to_cache() can't stat %s\n", e->path);
                return ERROR;
            }
            if (mystat.st_mtime<oldest_mtime) {
                oldest_mtime = mystat.st_mtime;
                oldest_size = e->total_size; // (mystat.st_size doesn't include digest)
                oldest_entry = e;
            } else {
                if (mystat.st_mtime==oldest_mtime) { // with same age, smaller ones get purged first
                    if (oldest_size > e->total_size) {
                        oldest_size = e->total_size;
                        oldest_entry = e;
                    }
                }
            }
        }

        if ( oldest_entry ) { // remove it
            logprintfl (EUCAINFO, "purging from cache entry %s\n", oldest_entry->base);
            if ( oldest_entry->next ) {
                oldest_entry->next->prev = oldest_entry->prev;
            }
            if ( oldest_entry->prev ) {
                oldest_entry->prev->next = oldest_entry->next;
            } else {
                cache_head = oldest_entry->next;
            }
            delete_disk_item (oldest_entry);
            cache_used -= oldest_entry->total_size;
            free_disk_item (oldest_entry);
        } else {
            logprintfl (EUCAERROR, "error: cannot find oldest entry in cache\n");
            return 1;
        }
    }

 create:

    if (ensure_path_exists (di->base, 0700)) {
        logprintfl (EUCAERROR, "error: failed to create cache entry '%d'\n", di->base);
        return ERROR;
    }

    add_to_cache (di);

    return OK;
}

void return_cache_path (long long size)
{
    cache_used -= size;
}

long long scan_cache (void)
{
    long long total_size = 0;

    // remove old LL
    disk_item * d = cache_head;
    while ( d != NULL ) {
        disk_item * prev_d = d;
        d = d->next;
        prev_d->next = NULL;
        prev_d->prev = NULL;
        free_disk_item (prev_d);
    }
    cache_head = NULL;

    logprintfl (EUCAINFO, "scanning the cache directory (%s)\n", cache_path);

    if (strlen(cache_path) == 0) {
        logprintfl (EUCAINFO, "no cache directory yet\n");
        return total_size;
    }

    struct stat mystat;
    if (stat (cache_path, &mystat) < 0) {
        logprintfl (EUCAWARN, "warning: could not stat %s\n", cache_path);
        return -1L;
    }
    total_size += mystat.st_size;

    DIR * cache_dir;
    if ((cache_dir=opendir(cache_path))==NULL) {
        logprintfl (EUCAFATAL, "errror: could not open cache directory %s\n", cache_path);
        return -1L;
    }

    // iterate over all directories in cache directory
    struct dirent * cache_dir_entry;
    while ((cache_dir_entry=readdir(cache_dir))!=NULL) {
        char * image_name = cache_dir_entry->d_name;
        char image_path [EUCA_MAX_PATH];
        long long image_size = 0;
        long long content_size = 0;
        int image_files = 0;


        if (!strcmp(".", image_name) ||
            !strcmp("..", image_name))
            continue;

        DIR * image_dir;
        snprintf (image_path, EUCA_MAX_PATH, "%s/%s", cache_path, image_name);
        if ((image_dir=opendir(image_path))==NULL) {
            logprintfl (EUCAWARN, "warning: unopeneable directory %s\n", image_path);
            continue;
        }

        if (stat (image_path, &mystat) < 0) {
            logprintfl (EUCAWARN, "warning: could not stat %s\n", image_path);
            closedir(image_dir);
            continue;
        }
        image_size += mystat.st_size;

        // add up sizes of all files in a directory
        struct dirent * image_dir_entry;
        boolean found_content = FALSE;
        boolean found_summary = FALSE;
        while ((image_dir_entry=readdir(image_dir))!=NULL) {
            char name [EUCA_MAX_PATH];
            strncpy (name, image_dir_entry->d_name, EUCA_MAX_PATH);

            if (!strcmp(".", name) ||
                !strcmp("..", name))
                continue;

            image_files++;

            char filepath [EUCA_MAX_PATH];
            snprintf (filepath, EUCA_MAX_PATH, "%s/%s", image_path, name);
            if (stat (filepath, &mystat) < 0 ) {
                logprintfl (EUCAERROR, "error: could not stat file %s\n", filepath);
                break;
            }
            if (mystat.st_size < 1) {
                logprintfl (EUCAERROR, "error: empty file among cached images in '%s'\n", filepath);
                break;
            }

            if (!strcmp ("content", name)) {
                content_size = mystat.st_size;
                found_content = TRUE;
            }
            if (!strcmp ("summary", name))
                found_summary = TRUE;

            image_size += mystat.st_size;
        }
        closedir(image_dir);

        // report on what was found in each cache directory
        if (image_files > 0) { // ignore empty directories
            if (image_size>0) {
                logprintfl (EUCAINFO, "- cached image '%s': size=%dMB, files=%d\n", image_name, image_size/MEGABYTE, image_files);
                total_size += image_size;

                //              if (!found_content || !found_summary)
                //                  logprintfl (EUCAINFO, "(warning: incomplete cache entry for image '%s')\n", image_name); // TODO: do this if cache entry is not locked

                // allocate a disk item object
                disk_item * di = alloc_disk_item (image_name, content_size, image_size, TRUE);
                add_to_cache (di);
            } else {
                logprintfl (EUCAWARN, "warning: empty cached image directory %s\n", image_path);
            }
        }
    }
    closedir (cache_dir);

    print_cache ();

    return total_size;
}

/////////// functions for managing the work directory

static long long work_used = 0L;
static long long work_limit = EUCA_SIZE_UNLIMITED; // size limit for work space
static char work_path [EUCA_MAX_PATH] = "."; // cwd is default work dir
static boolean work_was_created = FALSE;

int set_work_limit (long long size)
{
    if (size<0)
        work_limit = EUCA_SIZE_UNLIMITED;
    else
        work_limit = size;
    return 0;
}

long long get_work_limit (void)
{
    return work_limit;
}

int set_work_dir (const char * path)
{
    struct stat mystat;

    if (stat (path, &mystat)) {
        work_was_created = TRUE; // remember that work directory had to be created
    }
    if (ensure_path_exists(path, 0700)) {
        logprintfl (EUCAERROR, "failed to set work directory to '%s'\n", path);
        return 1;
    }
    safe_strncpy (work_path, path, EUCA_MAX_PATH);

    return 0;
}

char * get_work_dir (void)
{
    return work_path;
}

int clean_work_dir (blobstore * work_bs)
{
    char path [EUCA_MAX_PATH];
    sprintf (path, "%s/imager.pid", get_work_dir());
    unlink (path);
    
    if (work_bs)
        blobstore_delete (work_bs); // not fully implemented, but will delete artifacts of an empty blobstore
    
    if (work_was_created)
        return rmdir (work_path);
    
    return 0;
}

int reserve_work_path (long long size)
{
    // TODO: check if there is enough disk space?
    work_used += size;
    if (work_limit!=EUCA_SIZE_UNLIMITED &&
        work_used > work_limit) {
        work_used -= size;
        return 1;
    }
    return 0;
}

void return_work_path (long long size)
{
    work_used -= size;
}

char * alloc_tmp_file (const char * name_base, long long size)
{
    if (reserve_work_path(size)) {
        logprintfl (EUCAERROR, "out of work disk space for a temporary file\n");
        return NULL;
    }
    char * path = malloc (EUCA_MAX_PATH);
    if (path==NULL) {
        logprintfl (EUCAERROR, "out of memory in alloc_tmp_file()\n");
        return NULL;
    }

    snprintf (path, EUCA_MAX_PATH, "%s/%s-XXXXXX", get_work_dir(), name_base);
    int tmp_fd = safe_mkstemp (path);
    if (tmp_fd<0) {
        logprintfl (EUCAERROR, "error: failed to create a temporary file under %s\n", path);
        free(path);
        return NULL;
    }
    close (tmp_fd);

    return path;
}

void free_tmp_file (char * path, long long size)
{
    unlink (path);
    return_work_path (size);
    free (path);
}

/////////// disk item methods

// allocates disc item struct in memory, but does nothing on disk
disk_item * alloc_disk_item (const char * id, const long long content_size, const long long total_size, boolean cache_item)
{
    disk_item * di = calloc (1, sizeof(disk_item));
    if (di==NULL) {
        logprintfl (EUCAERROR, "error: out of memory in alloc_disk_item()\n");
        return di;
    }
    safe_strncpy (di->id, id, EUCA_MAX_PATH);
    if (cache_item) {
        snprintf (di->base, EUCA_MAX_PATH, "%s/%s",      get_cache_dir(), di->id);
        snprintf (di->path, EUCA_MAX_PATH, "%s/content", di->base);
        snprintf (di->summ, EUCA_MAX_PATH, "%s/summary", di->base);
    } else {
        snprintf (di->base, EUCA_MAX_PATH, "%s",            get_work_dir());
        snprintf (di->path, EUCA_MAX_PATH, "%s/%s",         di->base, di->id);
        snprintf (di->summ, EUCA_MAX_PATH, "%s/%s-summary", di->base, di->id);
    }
    di->cache_item = cache_item;
    di->total_size = total_size;
    di->content_size = content_size;

    return di;
}

// frees the disk item struct from memory, but does nothing on disk
int free_disk_item (disk_item * di)
{
    free (di);
    return 0;
}

// ensures there is space for the item, purging cache if necessary
int reserve_disk_item (disk_item * di)
{
    logprintfl (EUCAINFO, "reserving room for item '%s' in %s space\n", di->id, di->cache_item ? "cache" : "work");
    if (di->cache_item) {
        return reserve_cache_slot (di);
    } else {
        return reserve_work_path (di->total_size);
    }
}

// like printfn, but concatenates to the buffer rather than overwrites it
// hence, the string in buf must be '\0'-terminated
static int catprintfn (char * buf, const int len, const char * format, ...)
{
    // avoid strlen() in case string in buf is not terminated
    int offset = -1;
    for (int i=0; i<len; i++) {
        if (buf[i]=='\0') {
            offset = i;
            break;
        }
    }
    if (offset < 0 || offset >= len) return offset;

    va_list ap;
    va_start (ap, format);
    int printed = vsnprintf (buf + offset, len - offset, format, ap);
    va_end (ap);

    return offset + printed;
}

// generates, in memory, the summary string for a stage, including previous stages
static char * gen_summary (artifacts_spec * spec)
{
    char buf [4096] = "";

    catprintfn (buf, sizeof (buf), "operation: %s\n", spec->req->cmd->name);
    for (imager_param * p = spec->attrs; p->key; p++) {
        catprintfn (buf, sizeof (buf), "\t%s: %s\n", p->key, p->val);
    }
    for (artifacts_spec ** s = spec->deps; * s && (s-spec->deps)<MAX_DEPS; s++) {
        char * dep_summary = gen_summary (* s); // yay recursion!
        catprintfn (buf, sizeof (buf), "%s", dep_summary);
        free (dep_summary);
    }

    return strdup (buf);
}

// saves the summary of disk item to disk
int add_summ_disk_item (disk_item * di, artifacts_spec * spec)
{
    int ret = ERROR;

    char * summary = gen_summary (spec);
    if (summary != NULL) {
        ret = write2file (di->summ, summary);
        free (summary);
    }

    return ret;
}

// deletes on disk state for the item, but does nothing in memory
int delete_disk_item (disk_item * di)
{
    int ret = 0;

    if (unlink (di->path)) { logprintfl (EUCAERROR, "error: failed to delete '%s'\n", di->path); ret = 1; }
    if (unlink (di->summ)) { logprintfl (EUCAWARN, "warning: failed to delete summary file '%s'\n", di->summ); }
    if (di->cache_item) {
        if (rmdir (di->base)) { logprintfl (EUCAERROR, "error: failed to delete '%s'\n", di->base); ret = 1; }
    }

    return ret;
}

// finds disc item on disk (in work or cache), allocates and fills out memory struct for it
disk_item * find_disk_item (const char * id, const boolean cache_item)
{
    char path [EUCA_MAX_PATH];

    if (cache_item) {
        snprintf (path, EUCA_MAX_PATH, "%s/%s/content", get_cache_dir(), id);
    } else {
        snprintf (path, EUCA_MAX_PATH, "%s/%s", get_work_dir(), id);
    }

    if (cache_item) {
        scan_cache (); // rebuilds cache state in memory based on disk
        return find_in_cache (path);
    }

    struct stat mystat;
    if (stat (path, &mystat)<0) {
        if (errno!=ENOENT) { // file not found
            logprintfl (EUCAERROR, "error: could not stat '%s'\n", path);
        }
        return NULL;
    }

    return alloc_disk_item (id, mystat.st_size, mystat.st_size, cache_item);
}

// checks whether disk state of the item matches the spec (when it does not, function returns 1)
int verify_disk_item (const disk_item * di, artifacts_spec * spec)
{
    int ret = ERROR;
    struct stat mystat;
    if (stat (di->path, &mystat)<0) return ERROR;
    if (di->content_size != mystat.st_size) return ERROR;

    // TODO: check the checksum?
    char * file_summary = file2str (di->summ);
    char * spec_summary = gen_summary (spec);
    if (file_summary==NULL) {
        logprintfl (EUCAWARN, "warning: failed to read summary file '%s'\n", di->summ);
        if(spec_summary != NULL)
            free(spec_summary);
        ret = OK;
    } else {
        if (spec_summary!=NULL) {
            if (strcmp (file_summary, spec_summary)==0)
                ret = OK;
            free (spec_summary);
        }
        free (file_summary);
    }

    return ret;
}

// copies the item disk state from one item to another
int copy_disk_item (disk_item * src, disk_item * dst)
{
    if (copy_file (src->path, dst->path)) return ERROR;
    if (copy_file (src->summ, dst->summ)) return ERROR;
    return OK;
}

// returns the path of the item contents on disk
char * get_disk_item_path (disk_item * di)
{
    return di->path;
}

/////////// artifacts spec methods

// allocates artifacts struct and copies attributes into it
artifacts_spec * alloc_artifacts_spec (imager_request * req, char ** attrs)
{
    // create an input spec with 0s for dependencies
    artifacts_spec * spec = calloc (1, sizeof (artifacts_spec));
    if (spec!=NULL) {
        spec->req = req;

        if (attrs!=NULL && *attrs!=NULL) {
            char ** a;
            int count = 0;

            // determine the number of attributes
            for (a = attrs; *a && *(a+1); a+=2)
                count++;

            // allocate the key-val pair array
            spec->attrs = calloc (count+1, sizeof (imager_param)); // will be terminated with two NULL pointers
            if (spec->attrs==NULL) {
                free (spec);
                return NULL;
            }

            // copy the attributes
            for (count = 0, a = attrs; *a && *(a+1); a+=2) {
                spec->attrs[count].key = strdup (*a);
                spec->attrs[count].val = strdup (*(a+1));
                count++;
            }
        }
    }

    return spec;
}

// frees artifacts struct and the attributes therein
void free_artifacts_spec (artifacts_spec * spec)
{
    for (int i=0; spec->attrs && spec->attrs[i].key; i++) {
        free (spec->attrs[i].key);
        free (spec->attrs[i].val);
    }
    if (spec->attrs) free (spec->attrs);
    free (spec);
}

/////////// postprocess/preprocess methods

// performs tasks needed before an item can be procssed by execute():
// checks work and cache for existing results, validates or purges them,
// reserves space if necessary, locks the cache entry (must be released with postprocess_output_path)
output_file * preprocess_output_path (const char * id, artifacts_spec * spec, boolean use_work, boolean use_cache, artifacts_spec * prev_spec)
{
    int ret = OK;

    output_file * o = calloc (1, sizeof (output_file)); // all pointers are NULL, all booleans are FALSE
    if (o==NULL) {
        logprintfl (EUCAERROR, "out of memory in preprocess_output_path()\n");
        return NULL;
    }
    o->do_work = use_work;
    o->do_cache = use_cache;
    safe_strncpy (o->id, id, sizeof (o->id));

    // is there a valid work copy already?
    o->work_copy = find_disk_item (id, FALSE);
    if (o->work_copy!=NULL) { // there is a copy
        if (verify_disk_item (o->work_copy, spec)) { // does it match the spec?
            if (prev_spec!=NULL && verify_disk_item (o->work_copy, prev_spec)==OK) { // does it match previous stage's spec?
                logprintfl (EUCAINFO, "found previous stage's work copy of image '%s'\n", id);
                if (add_summ_disk_item (o->work_copy, spec)!=OK) { // update the spec
                    logprintfl (EUCAERROR, "error: failed to update summary of work copy of '%s'\n", id);
                    ret = ERROR;
                    goto cleanup;
                } else {
                    o->predecessor = TRUE;
                    o->in_work = TRUE;
                }
            } else {
                delete_disk_item (o->work_copy); // no => delete it
                free_disk_item (o->work_copy);
                o->work_copy = NULL;
                logprintfl (EUCAINFO, "purged a stale work copy of image '%s'\n", id);
            }
        } else {
            o->in_work = TRUE;
            logprintfl (EUCAINFO, "found a valid work copy of image '%s'\n", id);
        }
    }
    if (o->do_work && !o->in_work) { // no valid work copy => reserve space
        o->work_copy = alloc_disk_item (id, 0, spec->size, FALSE);
        if (o->work_copy==NULL) {
            ret = ERROR;
            goto cleanup;
        }
        if (reserve_disk_item (o->work_copy)!=OK) { // reserve room for the object
            logprintfl (EUCAERROR, "error: failed to allocate work space for '%s'\n", id);
            ret = ERROR;
            goto cleanup;
        }
        if (add_summ_disk_item (o->work_copy, spec)!=OK) {
            logprintfl (EUCAERROR, "error: failed to add summary to work copy of '%s'\n", id);
            ret = ERROR;
            goto cleanup;
        }
    }

    // if caching, is there a valid cached copy?
    if (o->do_cache) {
        lock_cache ();

        o->cache_copy = find_disk_item (id, TRUE);
        if (o->cache_copy!=NULL) { // there is a copy with this id in the cache?
            lock_disk_item (o->cache_copy); // NOTE: may be unlocked by the _delete
            if (verify_disk_item (o->cache_copy, spec)) { // does it match the spec?
                if (prev_spec!=NULL && o->work_copy != NULL && verify_disk_item (o->work_copy, prev_spec)==OK) { // does it match previous stage's spec?
                    logprintfl (EUCAINFO, "found previous stage's cache copy of image '%s'\n", id);
                    if (add_summ_disk_item (o->cache_copy, spec)!=OK) { // update the spec
                        logprintfl (EUCAERROR, "error: failed to update summary of cache copy of '%s'\n", id);
                        delete_disk_item (o->cache_copy);
                        free_disk_item (o->cache_copy);
                        o->cache_copy = NULL;
                    } else {
                        o->predecessor = TRUE;
                        o->in_cache = TRUE;
                    }
                } else {
                    delete_disk_item (o->cache_copy); // no => purge it
                    free_disk_item (o->cache_copy);
                    o->cache_copy = NULL;
                    logprintfl (EUCAINFO, "purged a stale cache copy of image '%s'\n", id);
                }
            } else {
                o->in_cache = TRUE;
                logprintfl (EUCAINFO, "found a valid cache copy of image '%s'\n", id);
            }
        }
        if (!o->in_cache) { // not in cache => reserve space, purging LRU items if necessary
            o->cache_copy = alloc_disk_item (id, 0, spec->size, TRUE);
            if (o->cache_copy!=NULL) {
                lock_disk_item (o->cache_copy);
                if (reserve_disk_item (o->cache_copy)==OK) { // there is room in the cache
                    if (add_summ_disk_item (o->cache_copy, spec)!=OK) {
                        logprintfl (EUCAERROR, "error: failed to add summary to cache copy of '%s'\n", id);
                        delete_disk_item (o->cache_copy);
                        free_disk_item (o->cache_copy);
                        o->cache_copy = NULL;
                    } else {
                        o->to_cache = TRUE;
                    }
                } else {
                    logprintfl (EUCAERROR, "warning: no room in cache for image '%s'\n", id);
                }
            }
        }
        unlock_cache ();
    }

    char * result_path = NULL;
    if (! (o->in_work || o->in_cache) || o->predecessor) {
        // no local copy, so execute() will have to create it
        // => decide where the result should go: work or cache
        if (o->do_work) {
            result_path = get_disk_item_path (o->work_copy);
            o->in_work = TRUE;
        } else if (o->to_cache) {
            result_path = get_disk_item_path (o->cache_copy);
            o->in_cache = TRUE;
        } else {
            logprintfl (EUCAERROR, "error: no work or cache space for downloading '%s'\n", id);
            ret = ERROR;
        }
    }

    if (result_path) {
        safe_strncpy (o->path, result_path, sizeof (o->path));
    }

 cleanup:

    // in case of error: delete disk state, unlock cache item
    // TODO: be more selective? invalidate cache?
    if (ret==ERROR) {
        if (o->work_copy!=NULL) {
            delete_disk_item (o->work_copy);
            free_disk_item (o->work_copy);
        }
        if (o->do_cache && o->cache_copy!=NULL) {
            unlock_disk_item (o->cache_copy);
        }
        free (o);
        o = NULL;
    }

    return o;
}

// performs tasks after an item has been processed the execute():
// copies the result to/from cache, unlocks the cache entry, frees memory
void postprocess_output_path (output_file * o, boolean success)
{
    if (success) {
        // copy to or from cache directory, as needed
        if (o->do_work && o->in_cache && !o->in_work) {
            logprintfl (EUCAINFO, "copying image '%s' from cache to work space\n", o->id);
            copy_disk_item (o->cache_copy, o->work_copy);

        } else if (o->in_work && o->to_cache) {
            logprintfl (EUCAINFO, "copying image '%s' from work to cache space\n", o->id);
            copy_disk_item (o->work_copy, o->cache_copy);
        }
    } else {
        // TODO: delete the partial results?
    }

    // unlock the cache entry
    if (o->do_cache && o->cache_copy!=NULL) {
        unlock_disk_item (o->cache_copy);
    }

    if (o->work_copy!=NULL) {
        free_disk_item (o->work_copy); // NOTE: we do not free cache_copy because it's in a LL managed by the cache
    }

    free (o);
}

// remove file and its summary in work directory
void rm_workfile (const char * filename)
{
    char file_path [EUCA_MAX_PATH];
    char summ_path [EUCA_MAX_PATH];

    snprintf (file_path, EUCA_MAX_PATH, "%s/%s", get_work_dir(), filename);
    snprintf (summ_path, EUCA_MAX_PATH, "%s/%s-summary", get_work_dir(), filename);

    unlink (file_path); // ignore errors
    unlink (summ_path);
}

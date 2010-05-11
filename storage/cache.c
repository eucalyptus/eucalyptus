
/*
 * Walrus-to-node caching logic.  If used concurrently, the caller must ensure mutual exclusion.
 */

#include <stdlib.h> // NULL
#include <string.h> // bzero, memcpy
#include <time.h> // time
#include <dirent.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>

#include "eucalyptus.h"  // MEGABYTE
#include "misc.h" // logprintfl
#include "cache.h"

#define SIZE 512

typedef struct cache_entry_t {
    char path [SIZE];
    long long size_mb;
    struct cache_entry_t * next;
    struct cache_entry_t * prev;
} cache_entry;

static cache_entry * cache_head = NULL;
static long long cache_size_mb = 9999; /* in MB */
static long long cache_free_mb = 9999;

long long init_cache (const char * cache_path)
{
    long long total_size = 0;
    
    logprintfl (EUCAINFO, "checking the integrity of the cache directory (%s)\n", cache_path);
    
    if (cache_path==NULL) {
        logprintfl (EUCAINFO, "no cache directory yet\n");
        return total_size;
    }

    struct stat mystat;
    if (stat (cache_path, &mystat) < 0) {
        logprintfl (EUCAFATAL, "error: could not stat %s\n", cache_path);
        return -1;
    }
    total_size += mystat.st_size;
   
    DIR * cache_dir;
    if ((cache_dir=opendir(cache_path))==NULL) {
        logprintfl (EUCAFATAL, "errror: could not open cache directory %s\n", cache_path);
        return -1;
    }

    struct dirent * cache_dir_entry;
    while ((cache_dir_entry=readdir(cache_dir))!=NULL) {
        char * image_name = cache_dir_entry->d_name;
        char image_path [SIZE];
        int image_size = 0;
        int image_files = 0;

        if (!strcmp(".", image_name) || 
            !strcmp("..", image_name))
            continue;
        
        DIR * image_dir;
        snprintf (image_path, SIZE, "%s/%s", cache_path, image_name);
        if ((image_dir=opendir(image_path))==NULL) {
            logprintfl (EUCAWARN, "warning: unopeneable directory %s\n", image_path);
            continue;
        }

        if (stat (image_path, &mystat) < 0) {
            logprintfl (EUCAWARN, "warning: could not stat %s\n", image_path);
            continue;
        }
        image_size += mystat.st_size;
        
        /* make sure that image directory contains only two files: one
         * named X and another X-digest, also add up their sizes */
        char X        [SIZE] = "";
        char X_digest [SIZE] = "";
        struct dirent * image_dir_entry;
        while ((image_dir_entry=readdir(image_dir))!=NULL) {
            char name [SIZE];
            strncpy (name, image_dir_entry->d_name, SIZE);
            
            if (!strcmp(".", name) ||
                !strcmp("..", name))
                continue;

            image_files++;
            
            char filepath [SIZE];
            snprintf (filepath, SIZE, "%s/%s", image_path, name);
            if (stat (filepath, &mystat) < 0 ) {
                logprintfl (EUCAERROR, "error: could not stat file %s\n", filepath);
                break;
            }
            if (mystat.st_size < 1) {
                logprintfl (EUCAERROR, "error: empty file among cached images in %s\n", filepath);
                break;
            }
            image_size += mystat.st_size;
            
            char * suffix;
            if ((suffix=strstr (name, "-digest"))==NULL) {
                if (strlen (X)) 
                    break; /* already saw X => fail */
                strncpy (X, name, SIZE);
            } else {
                if (strlen (X_digest))
                    break; /* already saw X-digest => fail */
                * suffix = '\0';
                strncpy (X_digest, name, SIZE);
            }
        }

        if (image_files > 0) { /* ignore empty directories */
            if (image_files != 2 || strncmp (X, X_digest, SIZE) != 0 ) {
                logprintfl (EUCAERROR, "error: inconsistent state of cached image %s, deleting it\n", image_name);
                if (vrun ("rm -rf %s", image_path)) {            
                    logprintfl (EUCAWARN, "warning: failed to remove %s\n", image_path);
                }
            } else {
                char filepath [SIZE];
                snprintf (filepath, SIZE, "%s/%s", image_path, X);
                if (image_size>0) {
                    logprintfl (EUCAINFO, "- cached image %s directory, size=%d\n", image_name, image_size);
                    total_size += image_size;
                    add_to_cache (filepath, image_size);
                } else {
                    logprintfl (EUCAWARN, "warning: empty cached image directory %s\n", image_path);
                }
            }
        }
    }
    closedir (cache_dir);

    return total_size;
}

void add_to_cache (const char * cached_path, const long long file_size_bytes)
{
    long long file_size_mb = file_size_bytes/MEGABYTE;

    cache_entry * e = malloc (sizeof(cache_entry));
    if (e==NULL) {
        logprintfl (EUCAFATAL, "error: out of memory in add_to_cache()\n");
        return;
    }

    strncpy (e->path, cached_path, SIZE);
    e->size_mb = file_size_mb;
    e->next = NULL;
    e->prev = NULL;

    // add at the end
    cache_entry ** pp;
    cache_entry * p = NULL;
    for ( pp = & cache_head; * pp != NULL; pp = & ((* pp)->next)) p = * pp;
    if ( p ) {
        e->prev = p;
    }
    * pp = e;

    cache_free_mb -= file_size_mb;
}

void print_cache (void)
{
    struct stat mystat;
    cache_entry * e;
    if (cache_head) {
        logprintfl (EUCAINFO, "cached images (free=%lld of %lldMB):\n", cache_free_mb, cache_size_mb);
    } else {
        logprintfl (EUCAINFO, "cached images (free=%lld of %lldMB): none\n", cache_free_mb, cache_size_mb);
    }
    for ( e = cache_head; e; e=e->next) {
        bzero (&mystat, sizeof (mystat));
        if (!stat (e->path, &mystat)) {
	  logprintfl (EUCAINFO, "\t%5dMB %8dsec %s\n", e->size_mb, mystat.st_mtime, e->path);
	}
    }
}

/* Returns 1 if there is space in the cache for the image,
 * purging the cache in LRU fashion, if necessary.
 * The function assumes the image is not in the cache already */
int ok_to_cache (const char * cached_path, const long long file_size_bytes)
{
    long long file_size_mb = file_size_bytes/MEGABYTE;

    if (file_size_mb > cache_size_mb) return 0;

    while (file_size_mb > cache_free_mb) {
        time_t oldest_mtime = time (NULL) + 1;
        off_t  oldest_size = 0;
        cache_entry * oldest_entry = NULL;
        struct stat mystat;
        cache_entry * e;
        for ( e = cache_head; e; e=e->next) {
            if (stat (e->path, &mystat)<0) {
                logprintfl (EUCAERROR, "error: ok_to_cache() can't stat %s\n", cached_path);
                return 0;
            }
            if (mystat.st_mtime<oldest_mtime) {
                oldest_mtime = mystat.st_mtime;
                oldest_size = e->size_mb; /* (mystat.st_size doesn't include digest) */
                oldest_entry = e;
            } else {
                if (mystat.st_mtime==oldest_mtime) {
                    /* smaller ones get purged first */
                    if (oldest_size > e->size_mb) {
                        oldest_size = e->size_mb;
                        oldest_entry = e;
                    }
                }
            }
        }

        if ( oldest_entry ) { // remove it
            logprintfl (EUCAINFO, "purging from cache image %s\n", oldest_entry->path);
            if ( oldest_entry->next ) {
                oldest_entry->next->prev = oldest_entry->prev;
            }
            if ( oldest_entry->prev ) {
                oldest_entry->prev->next = oldest_entry->next;
            } else {
                cache_head = oldest_entry->next;
            }
            if ( unlink (oldest_entry->path) != 0 ) { // should allow open descriptors to complete
                logprintfl (EUCAERROR, "error: failed to unlink file %s (%s)\n", oldest_entry->path, strerror (errno));
            }
            char digest_path [SIZE];
            snprintf (digest_path, SIZE, "%s-digest", oldest_entry->path);
            unlink (digest_path);
            cache_free_mb += oldest_entry->size_mb;
            free (oldest_entry);
        } else {
            logprintfl (EUCAERROR, "error: cannot find oldest entry in cache\n");
            return 0;
        }
    }
    add_to_cache (cached_path, file_size_bytes);
    return 1;
}

// unit tests

#define F1 "/tmp/improbable-cache-file-1"
#define F2 "/tmp/improbable-cache-file-2"
#define F3 "/tmp/improbable-cache-file-3"
#define F4 "/tmp/improbable-cache-file-4"
#define F5 "/tmp/improbable-cache-file-5"
#define RM_CMD "rm -rf /tmp/improbable-cache-file-?"

int test_cache (void)
{
    int error = 0;

    /* save the current values */
    long long saved_size = cache_size_mb;
    long long saved_free = cache_free_mb;
    cache_entry * saved_head = cache_head;

    cache_size_mb = 10;
    cache_free_mb = 10;
    cache_head = NULL;

    touch (F1); 
    if (ok_to_cache (F1, 3*MEGABYTE)!=1) { error= 1; goto out; }
    print_cache();
    sleep (1);

    touch (F2);
    add_to_cache (F2, 3*MEGABYTE);
    print_cache();
    sleep (1);

    touch (F3);
    if (ok_to_cache (F3, 11*MEGABYTE)!=0) { error = 2; goto out; }
    if (ok_to_cache (F3, 7*MEGABYTE)!=1) { error = 3; goto out; }
    print_cache();

    touch (F4);
    if (ok_to_cache (F4, 4*MEGABYTE)!=1) { error = 4; goto out; }
    touch (F5);
    if (ok_to_cache (F5, 6*MEGABYTE)!=1) { error = 5; goto out; }
    print_cache();

    touch (F3);
    add_to_cache (F3, 3*MEGABYTE);
    touch (F2);
    add_to_cache (F2, 5*MEGABYTE);
    print_cache();

    touch (F1);
    if (ok_to_cache (F1, 1*MEGABYTE)!=1) { error = 6; goto out; }
    print_cache();
    
out:
    cache_size_mb = saved_size;
    cache_free_mb = saved_free;
    cache_head = saved_head;
    system (RM_CMD);
    return error;
}

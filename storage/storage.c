/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU /* strnlen */
#include <string.h>
#include <sys/types.h>
#define _FILE_OFFSET_BITS 64
#include <sys/stat.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h> /* open|read|close dir */
#include <time.h> /* time() */

#include "eucalyptus.h"
#include "ipc.h"
#include "walrus.h"
#include "euca_auth.h"
#include <data.h>
#include <misc.h>
#include <storage.h>
#include <vnetwork.h>

#define BUFSIZE 512 /* random buffer size used all over the place */

/* default paths(may be overriden from config file) */
static char add_key_command_path [BUFSIZE] = "";
static long long swap_size_mb = DEFAULT_SWAP_SIZE; /* default swap in MB, if not specified in config file */
static long long cache_size_mb = DEFAULT_NC_CACHE_SIZE; /* in MB */
static long long cache_free_mb = DEFAULT_NC_CACHE_SIZE;
static long long work_size_mb = DEFAULT_NC_WORK_SIZE;
static long long work_free_mb = DEFAULT_NC_WORK_SIZE;

static char *sc_instance_path = "";
static char disk_convert_command_path [BUFSIZE] = "";
static int scConfigInit=0;
static sem * sc_sem;
static sem * disk_sem;

int scInitConfig (void)
{
    struct stat mystat;
    char configFiles[2][MAX_PATH];
    char * s;
    int concurrent_disk_ops;

    if (scConfigInit) {
      return 0;
    }
    
    if ((sc_sem = sem_alloc (1, "mutex")) == NULL) { /* TODO: use this semaphore to fix the race */
        logprintfl (EUCAERROR, "failed to create and initialize storage semaphore\n");
        return 1;
    }
    
    /* read in configuration */
    char *home, *tmp;
    tmp = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (tmp) {
        home = strdup(tmp);
    } else {
        home = strdup(""); /* root by default */
    }
    if (!home) {
       logprintfl (EUCAERROR, "out of memory\n");
       return 1;
    }
   
    snprintf(configFiles[0], BUFSIZE, EUCALYPTUS_CONF_OVERRIDE_LOCATION, home);
    snprintf(configFiles[1], BUFSIZE, EUCALYPTUS_CONF_LOCATION, home);
    if (stat(configFiles[1], &mystat)==0 || stat(configFiles[0], &mystat) == 0) {
      logprintfl (EUCAINFO, "SC is looking for configuration in files (%s,%s)\n", configFiles[1], configFiles[0]);
        s = getConfString(configFiles, 2, INSTANCE_PATH);
        if (s) {
	  sc_instance_path = s;
        }

        s = getConfString(configFiles, 2, CONFIG_NC_CACHE_SIZE);
        if (s) {
	  cache_size_mb = atoll (s); 
	  cache_free_mb = cache_size_mb;
	  free (s); 
        }

        s = getConfString(configFiles, 2, CONFIG_NC_WORK_SIZE);
        if (s) {
			work_size_mb = atoll (s); 
			work_free_mb = work_size_mb;
			free (s); 
        }


	s = getConfString(configFiles, 2, CONFIG_NC_SWAP_SIZE);
        if (s){ 
	  swap_size_mb = atoll (s); 
	  free (s); 
        }

	concurrent_disk_ops = 1;
	s = getConfString(configFiles, 2, CONFIG_CONCURRENT_DISK_OPS);
	if (s) {
	  concurrent_disk_ops = atoi(s);
	  free(s);
	}
	/* set the initial value of semaphore to number of 'disk intensive' operations that can run in parallel on this node */
	if ((disk_sem = sem_alloc (concurrent_disk_ops, "mutex")) == NULL) {
	  logprintfl (EUCAERROR, "failed to create and initialize disk semaphore\n");
	  if (home) free(home);
	  return(1);
	}
    }
    snprintf(add_key_command_path, BUFSIZE, EUCALYPTUS_ADD_KEY, home, home, home);
    
    /* we need to have valid path */
    if (check_directory(sc_instance_path)) {
	    logprintfl (EUCAERROR, "ERROR: INSTANCE_PATH (%s) does not exist!\n", sc_instance_path);
	    if (home) free(home);
	    return(1);
    }

    if (euca_init_cert ()) {
        logprintfl (EUCAFATAL, "failed to find cryptographic certificates\n");
        if (home) free(home);
        return 1;
    }

    snprintf (disk_convert_command_path, BUFSIZE, EUCALYPTUS_DISK_CONVERT, home, home);
    if (home) free(home);

    scConfigInit=1;
    return(0);
}

void scSaveInstanceInfo (const ncInstance * instance)
{
    char file_path [BUFSIZE];
    int fd;

    if (instance==NULL) return;
    snprintf(file_path, BUFSIZE, "%s/%s/%s/instance-checkpoint", sc_instance_path, instance->userId, instance->instanceId);
    if ((fd=open(file_path, O_CREAT | O_WRONLY, 0600))<0) return;
    write (fd, (char *)instance, sizeof(struct ncInstance_t));
    close (fd);
}

ncInstance * scRecoverInstanceInfo (const char *instanceId)
{
    const int file_size = sizeof(struct ncInstance_t);
    ncInstance * instance = malloc(file_size);
    char file_path [BUFSIZE];
    struct dirent * dir_entry;
    DIR * insts_dir;
    char * userId = NULL;
    int fd;

    if (instance==NULL) {
	    logprintfl(EUCADEBUG, "scRecoverInstanceInfo: NULL instance!\n");
	    return NULL;
    }

    /* we don't know userId, so we'll look for instanceId in every user's
     * directory (we're assuming that instanceIds are unique in the system) */
    if ((insts_dir=opendir(sc_instance_path))==NULL) {
	    logprintfl(EUCADEBUG, "scRecoverInstanceInfo: failed to open %s!\n", sc_instance_path);
	    free(instance);
	    return NULL;
    }
    while ((dir_entry=readdir(insts_dir))!=NULL) {
        char tmp_path [BUFSIZE];
        struct stat mystat;

        snprintf(tmp_path, BUFSIZE, "%s/%s/%s", sc_instance_path, dir_entry->d_name, instanceId);
        if (stat(tmp_path, &mystat)==0) {
            userId = strdup (dir_entry->d_name);
            break; /* we got it! */
        }
    }
    closedir(insts_dir);
    if (userId==NULL) {
	    logprintfl(EUCADEBUG, "scRecoverInstanceInfo: didn't find instance %s!\n", instanceId);
	    free(instance);
	    return NULL;
    }

    snprintf(file_path, BUFSIZE, "%s/%s/%s/instance-checkpoint", sc_instance_path, userId, instanceId);
	free(userId);
    if ((fd=open(file_path, O_RDONLY))<0 ||
        read(fd, instance, file_size)<file_size) {
        perror(file_path);
        free (instance);
	logprintfl(EUCADEBUG, "scRecoverInstanceInfo: fail to read recover file for %s!\n", instanceId);
        return NULL;
    }
    close (fd);
    instance->stateCode = NO_STATE;
    return instance;
}

typedef struct cache_entry_t {
    char path [BUFSIZE];
    long long size_mb;
    struct cache_entry_t * next;
    struct cache_entry_t * prev;
} cache_entry;

static cache_entry * cache_head = NULL;

static void add_to_cache (const char * cached_path, const long long file_size_bytes)
{
    long long file_size_mb = file_size_bytes/MEGABYTE;

    cache_entry * e = malloc (sizeof(cache_entry));
    if (e==NULL) {
        logprintfl (EUCAFATAL, "error: out of memory in add_to_cache()\n");
        return;
    }

    strncpy (e->path, cached_path, BUFSIZE);
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

void LogprintfCache (void)
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
        if (stat (e->path, &mystat) == 0) {
           logprintfl (EUCAINFO, "\t%5dMB %8dsec %s\n", e->size_mb, mystat.st_mtime, e->path);
	}
    }
}

/* Returns 1 if there is space in the cache for the image,
 * purging the cache in LRU fashion, if necessary.
 * The function assumes the image is not in the cache already */
static int ok_to_cache (const char * cached_path, const long long file_size_bytes)
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
            char digest_path [BUFSIZE];
            snprintf (digest_path, BUFSIZE, "%s-digest", oldest_entry->path);
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

static long long init_cache (const char * cache_path)
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
        char image_path [BUFSIZE];
        int image_size = 0;
        int image_files = 0;

        if (!strcmp(".", image_name) || 
            !strcmp("..", image_name))
            continue;
        
        DIR * image_dir;
        snprintf (image_path, BUFSIZE, "%s/%s", cache_path, image_name);
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
        
        /* make sure that image directory contains only two files: one
         * named X and another X-digest, also add up their sizes */
        char X        [BUFSIZE] = "";
        char X_digest [BUFSIZE] = "";
        struct dirent * image_dir_entry;
        while ((image_dir_entry=readdir(image_dir))!=NULL) {
            char name [BUFSIZE];
            strncpy (name, image_dir_entry->d_name, BUFSIZE);
            
            if (!strcmp(".", name) ||
                !strcmp("..", name))
                continue;

            image_files++;
            
            char filepath [BUFSIZE];
            snprintf (filepath, BUFSIZE, "%s/%s", image_path, name);
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
                strncpy (X, name, BUFSIZE);
            } else {
                if (strlen (X_digest))
                    break; /* already saw X-digest => fail */
                * suffix = '\0';
                strncpy (X_digest, name, BUFSIZE);
            }
        }
	closedir(image_dir);

        if (image_files > 0) { /* ignore empty directories */
            if (image_files != 2 || strncmp (X, X_digest, BUFSIZE) != 0 ) {
                logprintfl (EUCAERROR, "error: inconsistent state of cached image %s, deleting it\n", image_name);
                if (vrun ("rm -rf %s", image_path)) {            
                    logprintfl (EUCAWARN, "warning: failed to remove %s\n", image_path);
                }
            } else {
                char filepath [BUFSIZE];
                snprintf (filepath, BUFSIZE, "%s/%s", image_path, X);
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
    LogprintfCache();
    sleep (1);

    touch (F2);
    add_to_cache (F2, 3*MEGABYTE);
    LogprintfCache();
    sleep (1);

    touch (F3);
    if (ok_to_cache (F3, 11*MEGABYTE)!=0) { error = 2; goto out; }
    if (ok_to_cache (F3, 7*MEGABYTE)!=1) { error = 3; goto out; }
    LogprintfCache();

    touch (F4);
    if (ok_to_cache (F4, 4*MEGABYTE)!=1) { error = 4; goto out; }
    touch (F5);
    if (ok_to_cache (F5, 6*MEGABYTE)!=1) { error = 5; goto out; }
    LogprintfCache();

    touch (F3);
    add_to_cache (F3, 3*MEGABYTE);
    touch (F2);
    add_to_cache (F2, 5*MEGABYTE);
    LogprintfCache();

    touch (F1);
    if (ok_to_cache (F1, 1*MEGABYTE)!=1) { error = 6; goto out; }
    LogprintfCache();
    
out:
    cache_size_mb = saved_size;
    cache_free_mb = saved_free;
    cache_head = saved_head;
    system (RM_CMD);
    return error;
}

/* perform integrity check on instances directory, including the cache:
 * remove any files from non-running Eucalyptus instances, delete files
 * from cache that are not complete, return the amount of bytes used up by
 * everything
 */
long long scFSCK (bunchOfInstances ** instances)
{
    long long total_size = 0;
    struct stat mystat;

    if (instances==NULL) return -1;
    
    logprintfl (EUCAINFO, "checking the integrity of instances directory (%s)\n", sc_instance_path);

    /* let us not 'rm -rf /' accidentally */
    if (strlen(sc_instance_path)<2 || 
        sc_instance_path[0]!='/' ) {
        logprintfl (EUCAFATAL, "error: instances directory cannot be /, sorry\n");
        return -1; 
    }

    if (stat (sc_instance_path, &mystat) < 0) {
        logprintfl (EUCAFATAL, "error: could not stat %s\n", sc_instance_path);
        return -1;
    }
    total_size += mystat.st_size;

    DIR * insts_dir;
    if ((insts_dir=opendir(sc_instance_path))==NULL) {
        logprintfl (EUCAFATAL, "error: could not open instances directory %s\n", sc_instance_path);
        return -1;
    }

    /*** run through all users ***/

    char * cache_path = NULL;
    char * work_path = NULL;
    struct dirent * inst_dir_entry;
    while ((inst_dir_entry=readdir(insts_dir))!=NULL) {
        char * uname = inst_dir_entry->d_name;
        char user_path [BUFSIZE];
        struct dirent * user_dir_entry;
        DIR * user_dir;

        if (!strcmp(".", uname) || 
            !strcmp("..", uname))
            continue;

        snprintf (user_path, BUFSIZE, "%s/%s", sc_instance_path, uname);
        if ((user_dir=opendir(user_path))==NULL) {
            logprintfl (EUCAWARN, "warning: unopeneable directory %s\n", user_path);
            continue;
        }

        /*** run through all instances of a user ***/
        while ((user_dir_entry=readdir(user_dir))!=NULL) {
            char * iname = user_dir_entry->d_name;
            
            if (!strcmp(".", iname) ||
                !strcmp("..", iname))
                continue;
            
            char instance_path [BUFSIZE];
            snprintf (instance_path, BUFSIZE, "%s/%s", user_path, iname);

            if (!strcmp("cache", iname) &&
				!strcmp(EUCALYPTUS_ADMIN, uname)) { /* cache is in admin's dir */
				if (cache_path) {
                    logprintfl (EUCADEBUG, "Found a second cache_path?\n");
					free(cache_path);
				}
                cache_path = strdup (instance_path);
                continue;
            }

            if (!strcmp("work", iname) &&
				!strcmp(EUCALYPTUS_ADMIN, uname)) { /* work is in admin's dir */
				if (work_path) {
                    logprintfl (EUCADEBUG, "Found a second work_path?\n");
					free(work_path);
				}
                work_path = strdup (instance_path);
                continue;
            }

            /* spare directories of running instances, but count their usage */
            if (find_instance (instances, iname)) {
                long long bytes = dir_size (instance_path);
                if (bytes>0) {
                    logprintfl (EUCAINFO, "- running instance %s directory, size=%d\n", iname, bytes);
                    total_size += bytes;
                } else if (bytes==0) {
                    logprintfl (EUCAWARN, "warning: empty instance directory %s\n", instance_path);
                } else {
                    logprintfl (EUCAWARN, "warning: non-standard instance directory %s\n", instance_path);
                } 
                continue;
            }

            /* looks good - destroy it */
            if (vrun ("rm -rf %s", instance_path)) {
                logprintfl (EUCAWARN, "warning: failed to remove %s\n", instance_path);
            }
        }
        closedir (user_dir);
    }
    closedir (insts_dir);

    /*** scan the cache ***/
    long long cache_bytes = init_cache (cache_path);
    free (cache_path);
    if (cache_bytes < 0) {
      if (work_path) free(work_path);
        return -1;
    }
    
	// clean up work directory
	if (work_path) {
		if (vrun ("rm -rf %s", work_path)) {
			logprintfl (EUCAWARN, "warning: failed to clean work directory %s\n", work_path);
		}
		free (work_path);
	}

    return total_size + cache_bytes;
}

const char * scGetInstancePath(void)
{
    return sc_instance_path;
}

int scSetInstancePath(char *path) 
{
    sc_instance_path = strdup(path);
    return(0);
}

int scCleanupInstanceImage (char *user, char *instId) 
{
    return vrun ("rm -rf %s/%s/%s/", sc_instance_path, user, instId);
}

/* if path=A/B/C but only A exists, this will try to create B and C */
int ensure_path_exists (const char * path)
{
    mode_t mode = 0777;
    int len = strlen(path);
    char * path_copy = strdup(path);
    int i;

    if (path_copy==NULL) return errno;

    for (i=0; i<len; i++) {
        struct stat buf;
        char try_it = 0;

        if (path[i]=='/' && i>0) {
            path_copy[i] = '\0';
            try_it = 1;
        } else if (path[i]!='/' && i+1==len) { /* last one */
            try_it = 1;
        }
        
        if ( try_it ) {
            if ( stat (path_copy, &buf) == -1 ) {
                printf ("trying to create path %s\n", path_copy);
                if ( mkdir (path_copy, mode) == -1) {
                    printf ("error: failed to create path %s\n", path_copy);
		    if (path_copy) free(path_copy);
                    return errno;
                }
            }
            path_copy[i] = '/'; /* restore the slash */
        }
    }

	free (path_copy);
    return 0;
}

/* if path=A/B/C/D but only A exists, this will try to create B and C, but not D */
int ensure_subdirectory_exists (const char * path)
{
    int len = strlen(path);
    char * path_copy = strdup(path);
    int i;

    if (path_copy==NULL) return errno;

    for (i=len-1; i>0; i--) {
		if (path[i]=='/') {
			path_copy[i] = '\0';
			ensure_path_exists (path_copy);
			break;
		}
	}
	
	free (path_copy);
	return 0;	
}

/* wait for file 'appear' to appear or for file 'disappear' to disappear */
static int wait_for_file (const char * appear, const char * disappear, const int iterations, const char * name)
{
    int done, i;
    if (!appear && !disappear) return 1;

    for ( i=0, done=0; i<iterations && !done; i++ ) {
		struct stat mystat;
        sem_p (sc_sem);
        int check = ( (appear    && (stat (appear,    &mystat)==0)) ||
                      (disappear && (stat (disappear, &mystat)!=0)) );
        sem_v (sc_sem);
        if (check) {
            done++;
        } else {
	  		if (i==0) {
	    		logprintfl (EUCAINFO, "waiting for %s to become ready...\n", name);
	  		}
            sleep (10);
        }
    }

    if (!done) {
        logprintfl (EUCAERROR, "ERROR: timed out waiting for %s to become ready\n", name);
        return 1;
    }
    return 0;
}

/* returns size of the file in bytes if OK, otherwise a negative error */
static long long get_cached_file (const char * user_id, const char * url, const char * file_id, const char * instance_id, const char * file_name, char * file_path, sem * s, int convert_to_disk, long long limit_mb) 
{
    char tmp_digest_path [BUFSIZE];
	char cached_dir      [BUFSIZE]; 
	char cached_path     [BUFSIZE];
	char staging_path    [BUFSIZE];
	char digest_path     [BUFSIZE];

	snprintf (file_path,       BUFSIZE, "%s/%s/%s/%s",    sc_instance_path, user_id, instance_id, file_name);
	snprintf (tmp_digest_path, BUFSIZE, "%s-digest",      file_path);
	snprintf (cached_dir,      BUFSIZE, "%s/%s/cache/%s", sc_instance_path, EUCALYPTUS_ADMIN, file_id); /* cache is in admin's directory */
	snprintf (cached_path,     BUFSIZE, "%s/%s",          cached_dir, file_name);
	snprintf (staging_path,    BUFSIZE, "%s-staging",     cached_path);
	snprintf (digest_path,     BUFSIZE, "%s-digest",      cached_path);

retry:

    /* under a lock, figure out the state of the file */
    sem_p (sc_sem); /***** acquire lock *****/
    ensure_subdirectory_exists (file_path); /* creates missing directories */

	struct stat mystat;
    int cached_exists  = ! stat (cached_path, &mystat);
    int staging_exists = ! stat (staging_path, &mystat);

    int e = ERROR;
    int action;
    enum { ABORT, VERIFY, WAIT, STAGE };
    if ( staging_exists ) {
        action = WAIT;
    } else {
        if ( cached_exists ) {
            action = VERIFY;
        } else {
            action = STAGE;
        }
    }

    /* we return the sum of these */
    long long file_size_b = 0;
    long long digest_size_b = 0;
   
    /* while still under lock, decide whether to cache */
    int should_cache = 0;
    if (action==STAGE) { 
        e = walrus_object_by_url (url, tmp_digest_path, 0); /* get the digest to see how big the file is */
        if (e==OK && stat (tmp_digest_path, &mystat)) {
            digest_size_b = (long long)mystat.st_size;
        }
        if (e==OK) {
            /* pull the size out of the digest */
            char * xml_file = file2str (tmp_digest_path);
            if (xml_file) {
                file_size_b = str2longlong (xml_file, "<size>", "</size>");
                free (xml_file);
            }
            if (file_size_b > 0) {
                long long full_size_b = file_size_b+digest_size_b;
                if (convert_to_disk) {
                    full_size_b += swap_size_mb*MEGABYTE + MEGABYTE; /* TODO: take into account extra padding required for disks (over partitions) */
                }
                if ( full_size_b/MEGABYTE + 1 > limit_mb ) {
                    logprintfl (EUCAFATAL, "error: insufficient disk capacity remaining (%lldMB) in VM Type of instance %s for component %s\n", limit_mb, instance_id, file_name);
                    action = ABORT;
                    
                } else if ( ok_to_cache (cached_path, full_size_b) ) { /* will invalidate the cache, if needed */
                    ensure_path_exists (cached_dir); /* creates missing directories */
                    should_cache = 1;
                    if ( touch (staging_path) ) { /* indicate that we'll be caching it */
                        logprintfl (EUCAERROR, "error: failed to create staging file %s\n", staging_path);
                        action = ABORT;
                    }
                }
            } else {
                logprintfl (EUCAERROR, "error: failed to obtain file size from digest %s\n", url);
                action = ABORT;
            }
        } else {
            logprintfl (EUCAERROR, "error: failed to obtain digest from %s\n", url);
            action = ABORT;
        }
    }
    sem_v (sc_sem); /***** release lock *****/
    
    switch (action) {
    case STAGE:
        logprintfl (EUCAINFO, "downloading and preparing image into %s...\n", file_path);		
        e = walrus_image_by_manifest_url (url, file_path, 1);

        /* for KVM, convert partition into disk */
        if (e==OK && convert_to_disk) { 
            sem_p (s);
            sem_p (disk_sem);
            /* for the cached disk swap==0 and ephemeral==0 as we'll append them below */
            if ((e=vrun("%s %s %d %d", disk_convert_command_path, file_path, 0, 0))!=0) {
                logprintfl (EUCAERROR, "error: partition-to-disk image conversion command failed\n");
            }
	    sem_v(disk_sem);
            sem_v (s);
            
            /* recalculate file size now that it was converted */
            if ( stat (file_path, &mystat ) != 0 ) {
                logprintfl (EUCAERROR, "error: file %s not found\n", file_path);
            } else if (mystat.st_size < 1) {
                logprintfl (EUCAERROR, "error: file %s has the size of 0\n", file_path);
            } else {
                file_size_b = (long long)mystat.st_size;
            }
        }

	sem_p (disk_sem);
        /* cache the partition or disk, if possible */
        if ( e==OK && should_cache ) {
            if ( (e=vrun ("cp -a %s %s", file_path, cached_path)) != 0) {
                logprintfl (EUCAERROR, "failed to copy file %s into cache at %s\n", file_path, cached_path);
            }
            if ( e==OK && (e=vrun ("cp -a %s %s", tmp_digest_path, digest_path)) != 0) {
                logprintfl (EUCAERROR, "failed to copy digest file %s into cache at %s\n", tmp_digest_path, digest_path);
            }
        }
	sem_v (disk_sem);
        
        sem_p (sc_sem);
        if (should_cache) {
            unlink (staging_path);            
        }
        if ( e ) {
            logprintfl (EUCAERROR, "error: failed to download or prepare into %s\n", file_path);
            unlink (file_path);
            unlink (tmp_digest_path);
            if (should_cache) {
                unlink (cached_path);
                unlink (digest_path);
                if ( rmdir(cached_dir) ) {
                    logprintfl (EUCAWARN, "warning: failed to remove cache directory %s\n", cached_dir);
                }
            }
        }
        sem_v (sc_sem);
        break;
        
    case WAIT:
        logprintfl (EUCAINFO, "waiting for disapperance of %s...\n", staging_path);
        /* wait for staging_path to disappear, which means both either the
         * download succeeded or it failed */
        if ( (e=wait_for_file (NULL, staging_path, 180, "cached image")) ) 
            return 0L;        
        /* yes, it is OK to fall through */
        
    case VERIFY:
        logprintfl (EUCAINFO, "verifying cached file in %s...\n", cached_path);
        sem_p (sc_sem); /***** acquire lock *****/
        e = ERROR;
        if ( stat (cached_path, &mystat ) != 0 ) {
            logprintfl (EUCAERROR, "error: file %s not found\n", cached_path);
        } else if (mystat.st_size < 1) {
            logprintfl (EUCAERROR, "error: file %s has the size of 0\n", cached_path);
        } else if ((e=walrus_verify_digest (url, digest_path))<0) {
            /* negative status => digest changed */
            unlink (cached_path);
            unlink (staging_path); /* TODO: needed? */
            unlink (digest_path);
            if ( rmdir (cached_dir) ) {
                logprintfl (EUCAWARN, "warning: failed to remove cache directory %s\n", cached_dir);
            } else {
                logprintfl (EUCAINFO, "due to failure, removed cache directory %s\n", cached_dir);
            }
        } else {
            file_size_b = mystat.st_size;

            /* touch the digest so cache can use mtime for invalidation */
            if ( touch (digest_path) ) {
                logprintfl (EUCAERROR, "error: failed to touch digest file %s\n", digest_path);
            } else if ( stat (digest_path, &mystat) ) {
                logprintfl (EUCAERROR, "error: digest file %s not found\n", digest_path);
            } else {
                digest_size_b = (long long)mystat.st_size;
            }
        }
        sem_v (sc_sem); /***** release lock *****/
        
        if (e<0) { /* digest changed */
            if (action==VERIFY) { /* i.e. we did not download/waited for this file */
                /* try downloading anew */
                goto retry;
            } else {
                logprintfl (EUCAERROR, "error: digest mismatch, giving up\n");
                return 0L;
            }
        } else if (e>0) { /* problem with file or digest */
            return 0L;
            
        } else { /* all good - copy it, finally */
            ensure_subdirectory_exists (file_path); /* creates missing directories */            
	    sem_p (disk_sem);
            if ( (e=vrun ("cp -a %s %s", cached_path, file_path)) != 0) {
  	        logprintfl (EUCAERROR, "failed to copy file %s from cache at %s\n", file_path, cached_path);
		sem_v (disk_sem);
                return 0L;
            }
	    sem_v (disk_sem);
        }
        break;
        
    case ABORT:
        logprintfl (EUCAERROR, "get_cached_file() failed (errno=%d)\n", e);
        e = ERROR;
    }

    if (e==OK && file_size_b > 0 && convert_to_disk ) { // if all went well above
        long long ephemeral_mb = limit_mb - swap_size_mb - (file_size_b+digest_size_b)/MEGABYTE;
        if ( swap_size_mb>0L || ephemeral_mb>0L ) {
            sem_p (s);
            sem_p (disk_sem);
            if ((e=vrun("%s %s %lld %lld", disk_convert_command_path, file_path, swap_size_mb, ephemeral_mb))!=0) {
                logprintfl (EUCAERROR, "error: failed to add swap or ephemeral to the disk image\n");
            }
            sem_v (disk_sem);
            sem_v (s);

            /* recalculate file size (again!) now that it was converted */
            if ( stat (file_path, &mystat ) != 0 ) {
                logprintfl (EUCAERROR, "error: file %s not found\n", file_path);
            } else if (mystat.st_size < 1) {
                logprintfl (EUCAERROR, "error: file %s has the size of 0\n", file_path);
            } else {
                file_size_b = (long long)mystat.st_size;
            }
        }
    }

    if (e==OK && action!=ABORT)
        return file_size_b + digest_size_b;
    return 0L;
}

char * get_disk_path (
	const char * instanceId, 
	const char * userId)
{
	char file_path [MAX_PATH_SIZE];
	struct stat mystat;

	snprintf (file_path, MAX_PATH_SIZE, "%s/%s/%s/disk", sc_instance_path, userId, instanceId);
	if (stat (file_path, &mystat)!=0) {
        	snprintf (file_path, MAX_PATH_SIZE, "%s/%s/%s/root", sc_instance_path, userId, instanceId);
                if (stat (file_path, &mystat) !=0) {
		  logprintfl (EUCAERROR, "failed to stat disk %s\n", file_path);
		  return NULL;
                }
	}
	return strdup (file_path);
}

long long get_bundling_size (
	const char * instanceId, 
	const char * userId)
{
	char file_path [MAX_PATH_SIZE];
	struct stat mystat;

	snprintf (file_path, MAX_PATH_SIZE, "%s/%s/%s/disk", sc_instance_path, userId, instanceId);
	if (stat (file_path, &mystat)!=0) {
        	snprintf (file_path, MAX_PATH_SIZE, "%s/%s/%s/root", sc_instance_path, userId, instanceId);
                if (stat (file_path, &mystat) !=0) {
		  logprintfl (EUCAERROR, "failed to stat disk %s\n", file_path);
		  return -1L;
                }
	}

	return ((long long)mystat.st_size)*2L; // bundling requires twice the size of disk
}

char * alloc_work_path (
	const char * instanceId, // IN: id of instance that needs work space
	const char * userId, // IN: id of owner of the instance
	const long long sizeMb) // IN: size needed under work path
{
	char file_path [MAX_PATH_SIZE];
	if (sizeMb < 0L)
		return NULL;

	long long left = work_free_mb - sizeMb;
	if (left>0) {
		sem_p (sc_sem);
		work_free_mb -= sizeMb;
		sem_v (sc_sem);
		if (snprintf (file_path, MAX_PATH_SIZE, "%s/%s/work/%s", sc_instance_path, EUCALYPTUS_ADMIN, instanceId)<1) { // work is in admin's directory
			return NULL;
		}
	} else {
		logprintfl (EUCAERROR, "work disk space limit exceeded (free=%lld size=%lld)\n", work_free_mb, sizeMb);
		return NULL;
	}
	ensure_path_exists (file_path);

	return strdup (file_path);
}

int free_work_path (
	const char * instanceId, // IN: id of instance giving up work space
	const char * userId, // IN: id of owner of the instance
	const long long sizeMb) // IN: size needed under work path
{
	if (sizeMb < 0L) 
		return ERROR;

	char workPath [MAX_PATH_SIZE];
	if (snprintf (workPath, MAX_PATH_SIZE, "%s/%s/work/%s", sc_instance_path, EUCALYPTUS_ADMIN, instanceId)<1) { // work is in admin's directory
		return ERROR;
	}
	if (vrun ("rm -rf %s", workPath)) {
		logprintfl (EUCAWARN, "warning: failed to clean work directory %s\n", workPath);
	} else {
		sem_p (sc_sem);
		work_free_mb += sizeMb;
		sem_v (sc_sem);
	}
	return OK;
}

int scMakeInstanceImage (char *euca_home, char *userId, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *instanceId, char *keyName, char **instance_path, sem * s, int convert_to_disk, long long total_disk_limit_mb) 
{
    char image_path   [BUFSIZE]; long long image_size_b = 0L;
    char kernel_path  [BUFSIZE]; long long kernel_size_b = 0L;
    char ramdisk_path [BUFSIZE]; long long ramdisk_size_b = 0L;
    char config_path  [BUFSIZE];
    char rundir_path  [BUFSIZE];
    int e = ERROR;
    
    logprintfl (EUCAINFO, "retrieving images for instance %s (disk limit=%lldMB)...\n", instanceId, total_disk_limit_mb);
    
    /* get the necessary files from Walrus, caching them if possible */
    char * image_name;
    int mount_offset = 0;
    long long limit_mb = total_disk_limit_mb;
    if (convert_to_disk) {
        image_name = "disk";
        mount_offset = 32256; /* 1st partition offset in the disk image */
    } else {
        image_name = "root";
        limit_mb -= swap_size_mb; /* account for swap, which will be a separate file */
    } 

#define CHECK_LIMIT(WHAT) \
    if (limit_mb < 1L) { \
        logprintfl (EUCAFATAL, "error: insufficient disk capacity remaining (%lldMB) in VM Type of instance %s for component %s\n", limit_mb, instanceId, WHAT); \
        return e; \
    }
    CHECK_LIMIT("swap");
    
    /* do kernel & ramdisk first, since either the disk or the ephemeral partition will take up the rest */
    if (kernelId && strnlen(kernelId, CHAR_BUFFER_SIZE) ) {
      if ((kernel_size_b=get_cached_file (userId, kernelURL, kernelId, instanceId, "kernel", kernel_path, s, 0, limit_mb))<1L) return e;
      limit_mb -= kernel_size_b/MEGABYTE;
      CHECK_LIMIT("kernel");
    }
    if (ramdiskId && strnlen (ramdiskId, CHAR_BUFFER_SIZE) ) {
      if ((ramdisk_size_b=get_cached_file (userId, ramdiskURL, ramdiskId, instanceId, "ramdisk", ramdisk_path, s, 0, limit_mb))<1L) return e;
      limit_mb -= ramdisk_size_b/MEGABYTE;
      CHECK_LIMIT("ramdisk")
    }

    if ((image_size_b=get_cached_file (userId, imageURL, imageId, instanceId, image_name, image_path, s, convert_to_disk, limit_mb))<1L) return e;
    limit_mb -= image_size_b/MEGABYTE;

    snprintf (rundir_path, BUFSIZE, "%s/%s/%s", sc_instance_path, userId, instanceId);
   
    logprintfl (EUCAINFO, "preparing images for instance %s...\n", instanceId);
    
    /* embed the key, which is contained in keyName */
    char *key_template = NULL;
    if (keyName && strlen(keyName)) {
      int key_len = strlen(keyName);
      int fd = -1;
      int ret;
      
      key_template = strdup("/tmp/sckey.XXXXXX");
      
      if (((fd = mkstemp(key_template)) < 0)) {
	logprintfl (EUCAERROR, "failed to create a temporary key file\n"); 
      } else if ((ret = write (fd, keyName, key_len))<key_len) {
	logprintfl (EUCAERROR, "failed to write to key file %s write()=%d\n", key_template, ret);
      } else {
	close (fd);
	logprintfl (EUCAINFO, "adding key%s to the root file system at %s using (%s)\n", key_template, image_path, add_key_command_path);
      }
    } else { /* if no key was given, add_key just does tune2fs to up the filesystem mount date */
      key_template = "";
      logprintfl (EUCAINFO, "running tune2fs on the root file system at %s using (%s)\n", key_template, image_path, add_key_command_path);
    }
    
    /* do the key injection and/or tune2fs */
    sem_p (s);
    if (vrun("%s %d %s %s", add_key_command_path, mount_offset, image_path, key_template)!=0) {
      logprintfl (EUCAERROR, "ERROR: key injection / tune2fs command failed\n");
      /* we proceed despite the failure since maybe user embedded the key
       * into the image; also tune2fs may fail on uncrecognized but valid
       * filesystems */
    }
    sem_v (s);
    
    if (strlen(key_template)) {
      if (unlink(key_template) != 0) {
	logprintfl (EUCAWARN, "WARNING: failed to remove temporary key file %s\n", key_template);
      }
      free (key_template);
    }
    
    /* if the image is a root partition... */
    if (!convert_to_disk) {
      /* create swap partition */
      if (swap_size_mb>0) { 
	sem_p (disk_sem);
	if ((e=vrun ("dd bs=1M count=%lld if=/dev/zero of=%s/swap 2>/dev/null", swap_size_mb, rundir_path)) != 0) { 
	  logprintfl (EUCAINFO, "creation of swap (dd) at %s/swap failed\n", rundir_path);
	  sem_v (disk_sem);
	  return e;
	}
	if ((e=vrun ("mkswap %s/swap >/dev/null", rundir_path)) != 0) {
	  logprintfl (EUCAINFO, "initialization of swap (mkswap) at %s/swap failed\n", rundir_path);
	  sem_v (disk_sem);
	  return e;		
	}
	sem_v (disk_sem);
      }
      /* create ephemeral partition */
      if (limit_mb>0) {
	sem_p (disk_sem);
	if ((e=vrun ("dd bs=1M count=%lld if=/dev/zero of=%s/ephemeral 2>/dev/null", limit_mb, rundir_path )) != 0) {
	  logprintfl (EUCAINFO, "creation of ephemeral disk (dd) at %s/ephemeral failed\n", rundir_path);
	  sem_v (disk_sem);
	  return e;
	}
	if ((e=vrun ("mkfs.ext3 -F %s/ephemeral >/dev/null 2>&1", rundir_path)) != 0) {
	  logprintfl (EUCAINFO, "initialization of ephemeral disk (mkfs.ext3) at %s/ephemeral failed\n", rundir_path);
	  sem_v (disk_sem);
	  return e;		
	}
	sem_v (disk_sem);
      }
    }
    
    * instance_path = strdup (rundir_path);
    if (*instance_path==NULL) return errno;
    return 0;
}

int scStoreStringToInstanceFile (const char *userId, const char *instanceId, const char * file, const char * data)
{
    FILE * fp;
    int ret = ERROR;
	char path [BUFSIZE];
	snprintf (path, BUFSIZE, "%s/%s/%s/%s", sc_instance_path, userId, instanceId, file);
    if ( (fp = fopen (path, "w")) != NULL ) {
        if ( fputs (data, fp) != EOF ) {
           ret = OK;
	}
        fclose (fp);
    }
    return ret;
}

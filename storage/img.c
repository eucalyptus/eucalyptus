#include <stdlib.h> // NULL
#include <stdio.h>
#include <string.h> // bzero, memcpy
#include <unistd.h> // getopt, access
#include <ctype.h> // tolower
#include <stdarg.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>
#include <fcntl.h>
#include <time.h>
#include <regex.h>

#include "eucalyptus.h" // EUCALYPTUS_ENV_VAR_NAME
#include "ami2vmx.h"
#include "walrus.h"
#include "misc.h" // logprintfl
#include "cache.h"
#include "img.h"
#include "ipc.h"
#include "http.h" // http_put

static img_env g_env = { initialized: 0 };
static sem * img_sem;
static char add_key_command_path [SIZE] = "";
static char disk_convert_command_path [SIZE] = "";
static char * euca_home = NULL;

static void max_to_str (char * str, const int size, int limit_mb) 
{
    if (limit_mb<0) {
        snprintf (str, size, "unlimited");
    } else if (limit_mb==0) {
        snprintf (str, size, "blocked");
    } else {
        snprintf (str, size, "%dMB", limit_mb);
    }
}

img_env * img_init (char * wdir, int wdir_max_mb, char * cdir, int cdir_max_mb) 
{
    if (g_env.initialized==0) {
        // this clause only execute once
        bzero (&g_env, sizeof(g_env));
        
        if ((img_sem = sem_realloc (1, "eucalyptus-img-semaphore", 0)) == NULL) {
            logprintfl (EUCAERROR, "failed to create and initialize img semaphore\n");
            return NULL;
        }
        
        g_env.initialized = 1;
    }
    
    // the rest is OK to redo (so img_init users can pick up new files or change params)

    g_env.wloc.type = PATH; // only PATH for now    
    if (wdir!=NULL && strlen(wdir)>0) {
        if (check_directory (wdir)) {
    	    logprintfl (EUCAERROR, "ERROR: working directory (%s) does not exist!\n", wdir);
            return NULL;
        }
        strncpy (g_env.wloc.path, wdir, sizeof (g_env.wloc.path));
    } else {
        if (getcwd (g_env.wloc.path, sizeof (g_env.wloc.path))==NULL) {
            logprintfl (EUCAERROR, "ERROR: failed to obtain current working directory!\n");
            return NULL;   
        }
    }
    g_env.wloc_max_mb = wdir_max_mb;
    char s [SIZE];
    max_to_str (s, SIZE, g_env.wloc_max_mb); // pretty-print it
    logprintfl (EUCAINFO, "img_init: working directory=%s limit=%s\n", g_env.wloc.path, s);
    
    if (cdir!=NULL && strlen(cdir)>0 && cdir_max_mb!=0) {
        g_env.cloc.type = PATH; // only PATH for now
        if (check_directory (cdir)) {
    	    logprintfl (EUCAERROR, "ERROR: cache directory (%s) does not exist!\n", cdir);
    	    return NULL;
        }
        strncpy (g_env.cloc.path, cdir, sizeof(g_env.cloc.path));
        g_env.cloc_max_mb = cdir_max_mb;
        max_to_str (s, SIZE, g_env.cloc_max_mb); // pretty-print it
        logprintfl (EUCAINFO, "img_init: cache directory=%s limit=%s\n", g_env.cloc.path, s);
        long long size_b = init_cache (g_env.cloc.path);
        if (size_b<0) {
    	    logprintfl (EUCAERROR, "ERROR: failed to initialize or verify cache directory (%s)\n", cdir);
    	    return NULL;           
        }
        int size_mb = size_b / MEGABYTE;
        if (g_env.cloc_max_mb>0 && size_mb > g_env.cloc_max_mb) {
            logprintfl (EUCAWARN, "img_init: cache size (%dMB) exceeds the limit (%dMB)!\n", size_mb, g_env.cloc_max_mb);
        }
    }
    
    // try looking for stuff relative to $EUCALYPTUS env var
    char root [] = "";
	euca_home = getenv(EUCALYPTUS_ENV_VAR_NAME);
    if (!euca_home) {
        euca_home = root;
    }
    snprintf (add_key_command_path,      SIZE, EUCALYPTUS_ADD_KEY, euca_home, euca_home, euca_home);
    snprintf (disk_convert_command_path, SIZE, EUCALYPTUS_DISK_CONVERT, euca_home, euca_home);

    // try to find default Walrus creds    
    img_creds * w = &(g_env.default_walrus_creds);
    snprintf (w->cert_path, sizeof (w->cert_path), "%s/var/lib/eucalyptus/keys/node-cert.pem", euca_home);
    snprintf (w->pk_path, sizeof (w->pk_path), "%s/var/lib/eucalyptus/keys/node-pk.pem", euca_home);
    if (access (w->cert_path, R_OK) || access (w->pk_path, R_OK)) {
        w->type=NONE; // files weren't there or weren't readable
        logprintfl (EUCAWARN, "img_init: missing or unreadable Walrus x509 cert in %s\n", w->cert_path);
    } else {
        w->type=X509CREDS;
        logprintfl (EUCAINFO, "img_init: found Walrus x509 cert in %s\n", w->cert_path);
    }
    
    // node creds are same as Walrus creds for now
    img_creds * n = &(g_env.default_node_creds);
    snprintf (n->cert_path, sizeof (n->cert_path), "%s/var/lib/eucalyptus/keys/node-cert.pem", euca_home);
    snprintf (n->pk_path, sizeof (n->pk_path), "%s/var/lib/eucalyptus/keys/node-pk.pem", euca_home);
    if (access (n->cert_path, R_OK) || access (n->pk_path, R_OK)) {
        n->type=NONE; // files weren't there or weren't readable
        logprintfl (EUCAWARN, "img_init: missing or unreadable node x509 cert in %s\n", n->cert_path);
    } else {
        n->type=X509CREDS;
        logprintfl (EUCAINFO, "img_init: found node x509 cert in %s\n", n->cert_path);
    }
    
    return &g_env;
} 

static void strnsub (char * dest, const char *src, const unsigned int sindex, const unsigned int eindex, const unsigned int size)
{
    strncpy (dest, src+sindex, ((eindex-sindex)>size)?size:eindex-sindex);
}

static int parse_img_spec (img_loc * img, const char * str)
{
    char low [SIZE];
    int i;
    
    for (i=0; i<SIZE && i<strlen(str); i++) {
        low [i] = tolower (str[i]);
    }

    if (!strncmp (low, "http://", 7) || !strncmp (low, "https://", 8)) {
        if (strstr (str, "services/Walrus")) {
            img->type=WALRUS;
            strncpy (img->url, str, sizeof(img->url));
       
        } else if (strstr (str, "dcPath=")) {            
            // EXAMPLE: https://192.168.7.236/folder/i-4DD50852?dcPath=ha-datacenter&dsName=S1
            char * ex = "^[Hh][Tt][Tt][Pp][Ss]?://([^/]+)/([^\\?]+)\\?dcPath=([^&]+)&dsName=(.*)$";
            regmatch_t pmatch[5];
            regex_t re;
            int status;
            
            if (regcomp (&re, ex, REG_EXTENDED) != 0) {
                logprintfl (EUCAERROR, "parse_img_spec: failed to compile regular expression for vSphere URL: %s\n", ex);
                return 1;
            }
            status = regexec (&re, str, (size_t)5, pmatch, 0);
            regfree (&re);
            if (status != 0) {
                logprintfl (EUCAERROR, "parse_img_spec: failed to match the syntax of vSphere URL (%s)\nwith regular expression %s\n", str, ex);
                return 1;
            }
            if (re.re_nsub!=4) {
                logprintfl (EUCAERROR, "parse_img_spec: unexpected number of matched elements in %s\n", ex);
                return 1;
            }

            img->type=VSPHERE;
            strncpy (img->url,            str, sizeof (img->url));
            strnsub (img->vsphere_host,   str, pmatch[1].rm_so, pmatch[1].rm_eo, sizeof (img->vsphere_host));
            strnsub (img->path,           str, pmatch[2].rm_so, pmatch[2].rm_eo, sizeof (img->path));
            strnsub (img->vsphere_dcPath, str, pmatch[3].rm_so, pmatch[3].rm_eo, sizeof (img->vsphere_dcPath));
            strnsub (img->vsphere_dsName, str, pmatch[4].rm_so, pmatch[4].rm_eo, sizeof (img->vsphere_dsName));

            //logprintfl (EUCADEBUG, "parse_img_spec: re_nsub=%d host=%s path=%s dc=%s ds=%s\n", re.re_nsub, img->vsphere_host, img->path, img->vsphere_dcPath, img->vsphere_dsName);

        } else {
            img->type=HTTP;
            strncpy (img->url, str, sizeof(img->url));
        }

    } else if (!strncmp (low, "sftp://", 7)) {
        img->type=SFTP;
        strncpy (img->url, str, sizeof(img->url));

    } else {
        img->type=PATH;
        strncpy (img->path, str, sizeof(img->path));
    }
    
    return 0;
}

int img_init_spec (img_spec * spec, const char * id, const char * loc, const img_creds * creds)
{
    bzero (spec, sizeof(spec));
    
    if (id && strlen(id)) 
        strncpy (spec->id, id, sizeof(spec->id));
    else
        return 0; // if no ID is given, this function just zeroes out the struct
        
    if (parse_img_spec (&(spec->location), loc)) return 1;
    if (creds) 
        memcpy (&(spec->location.creds), creds, sizeof(img_creds));
    else 
        spec->location.creds.type=NONE;
    
    return 0;
}

void img_cleanup (void)
{
    sem_free (img_sem);
}

static char * img_spec_type_str (const int type)
{
    switch (type) {
        case PATH: return "path";
        case HTTP: return "http";
        case VSPHERE: return "vsphere";
        case WALRUS: return "walrus";
        case SFTP: return "sftp";
        default: return "unknown";
    }
}

static char * img_creds_type_str (const int type)
{
    switch (type) {
        case NONE: return "none";
        case PASSWORD: return "password";
        case X509CREDS: return "x509";
        case SSHKEY: return "sshkey";
        default: return "unknown";
    }
}

static const char * img_creds_location_str (const img_loc * loc)
{
    switch (loc->type) {
    case PATH: return loc->path;
    case HTTP: return loc->url;
    case VSPHERE: return loc->url;
    case WALRUS: return loc->url;
    case SFTP: return loc->url;
    default: return "";
    }
}

static void print_img_spec (const char * name, const img_spec * spec)
{
    if (name==NULL || spec==NULL || strlen(name)==0 || strlen(spec->id)==0) return;
    logprintfl (EUCADEBUG, "\t%s: id=%-12ss type=%-7s creds=%-8s %s\n", 
                name, 
                spec->id,
                img_spec_type_str (spec->location.type),
                img_creds_type_str (spec->location.creds.type), 
                img_creds_location_str (&(spec->location)));
}

/* wait for file 'appear' to appear or for file 'disappear' to disappear */
static int wait_for_file (const char * appear, const char * disappear, const int iterations, const char * name)
{
    int done, i;
    if (!appear && !disappear) return 1;

    for ( i=0, done=0; i<iterations && !done; i++ ) {
		struct stat mystat;
        sem_p (img_sem);
        int check = ( (appear    && (stat (appear,    &mystat)==0)) ||
                      (disappear && (stat (disappear, &mystat)!=0)) );
        sem_v (img_sem);
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

/* if path=A/B/C but only A exists, this will try to create B and C */
static int ensure_path_exists (const char * path)
{
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
                logprintfl (EUCADEBUG, "trying to create path %s\n", path_copy);
                if ((mkdir (path_copy, (mode_t)0) == -1) || (chmod (path_copy, (mode_t)0774) == -1)) {
                    printf ("error: failed to create path %s\n", path_copy);
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
static int ensure_subdirectory_exists (const char * path)
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

/* returns size of the file in bytes if OK, otherwise a negative error */
static long long get_cached_file (
        const char * src_url, 
        const char * dst_path, 
        const char * cache_key, 
        sem * s, 
        int convert_to_disk, 
        long long limit_mb, 
        long long swap_size_mb,
        long long ephemeral_size_mb) 
{
    char tmp_digest_path [SIZE];
	char cached_dir      [SIZE]; 
	char cached_path     [SIZE];
	char staging_path    [SIZE];
	char digest_path     [SIZE];

	snprintf (tmp_digest_path, SIZE, "%s-digest",      dst_path);
	snprintf (cached_dir,      SIZE, "%s/%s",          g_env.cloc.path, cache_key);
	snprintf (cached_path,     SIZE, "%s/content",     cached_dir);
	snprintf (staging_path,    SIZE, "%s-staging",     cached_path);
	snprintf (digest_path,     SIZE, "%s-digest",      cached_path);

retry:

    /* under a lock, figure out the state of the file */
    sem_p (img_sem); /***** acquire lock *****/
    ensure_subdirectory_exists (dst_path); /* creates missing directories */

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
        e = walrus_object_by_url (src_url, tmp_digest_path, 0); /* get the digest to see how big the file is */
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
                if ( limit_mb > 0 && full_size_b/MEGABYTE + 1 > limit_mb ) {
                    logprintfl (EUCAFATAL, "error: insufficient disk capacity remaining (%lldMB) in VM Type for component %s\n", limit_mb, dst_path);
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
                logprintfl (EUCAERROR, "error: failed to obtain file size from digest %s\n", src_url);
                action = ABORT;
            }
        } else {
            logprintfl (EUCAERROR, "error: failed to obtain digest from %s\n", src_url);
            action = ABORT;
        }
    }
    sem_v (img_sem); /***** release lock *****/
    
    switch (action) {
    case STAGE:
        logprintfl (EUCAINFO, "downloding image into %s...\n", dst_path);		
        e = walrus_image_by_manifest_url (src_url, dst_path, 1);

        /* for KVM, convert partition into disk */
        if (e==OK && convert_to_disk) { 
            sem_p (s);
            /* for the cached disk swap==0 and ephemeral==0 as we'll append them below */
            if ((e=vrun("%s %s %d %d", disk_convert_command_path, dst_path, 0, 0))!=0) {
                logprintfl (EUCAERROR, "error: partition-to-disk image conversion command failed\n");
            }
            sem_v (s);
            
            /* recalculate file size now that it was converted */
            if ( stat (dst_path, &mystat ) != 0 ) {
                logprintfl (EUCAERROR, "error: file %s not found\n", dst_path);
            } else if (mystat.st_size < 1) {
                logprintfl (EUCAERROR, "error: file %s has the size of 0\n", dst_path);
            } else {
                file_size_b = (long long)mystat.st_size;
            }
        }

        /* cache the partition or disk, if possible */
        if ( e==OK && should_cache ) {
            if ( (e=vrun ("cp -a %s %s", dst_path, cached_path)) != 0) {
                logprintfl (EUCAERROR, "failed to copy file %s into cache at %s\n", dst_path, cached_path);
            }
            if ( e==OK && (e=vrun ("cp -a %s %s", tmp_digest_path, digest_path)) != 0) {
                logprintfl (EUCAERROR, "failed to copy digest file %s into cache at %s\n", tmp_digest_path, digest_path);
            }
        }
        
        sem_p (img_sem);
        if (should_cache) {
            unlink (staging_path);            
        }
        if ( e ) {
            logprintfl (EUCAERROR, "error: failed to download file from Walrus into %s\n", dst_path);
            unlink (dst_path);
            unlink (tmp_digest_path);
            if (should_cache) {
                unlink (cached_path);
                unlink (digest_path);
                if ( rmdir(cached_dir) ) {
                    logprintfl (EUCAWARN, "warning: failed to remove cache directory %s\n", cached_dir);
                }
            }
        }
        sem_v (img_sem);
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
        sem_p (img_sem); /***** acquire lock *****/
        e = ERROR;
        if ( stat (cached_path, &mystat ) != 0 ) {
            logprintfl (EUCAERROR, "error: file %s not found\n", cached_path);
        } else if (mystat.st_size < 1) {
            logprintfl (EUCAERROR, "error: file %s has the size of 0\n", cached_path);
        } else if ((e=walrus_verify_digest (src_url, digest_path))<0) {
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
        sem_v (img_sem); /***** release lock *****/
        
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
            ensure_subdirectory_exists (dst_path); /* creates missing directories */            
            if ( (e=vrun ("cp -a %s %s", cached_path, dst_path)) != 0) {
                logprintfl (EUCAERROR, "failed to copy file %s from cache at %s\n", dst_path, cached_path);
                return 0L;
            }
        }
        break;
        
    case ABORT:
        logprintfl (EUCAERROR, "get_cached_file() failed (errno=%d)\n", e);
        e = ERROR;
    }

    if (e==OK && file_size_b > 0 && convert_to_disk ) { // if all went well above
        if ( swap_size_mb>0L || ephemeral_size_mb>0L ) {
                if (ephemeral_size_mb<0) {
                        ephemeral_size_mb=0;
                }
            sem_p (s);
            if ((e=vrun("%s %s %lld %lld", disk_convert_command_path, dst_path, swap_size_mb, ephemeral_size_mb))!=0) {
                logprintfl (EUCAERROR, "error: failed to add swap or ephemeral to the disk image\n");
            }
            sem_v (s);

            /* recalculate file size (again!) now that it was converted */
            if ( stat (dst_path, &mystat ) != 0 ) {
                logprintfl (EUCAERROR, "error: file %s not found\n", dst_path);
            } else if (mystat.st_size < 1) {
                logprintfl (EUCAERROR, "error: file %s has the size of 0\n", dst_path);
            } else {
                file_size_b = (long long)mystat.st_size;
            }
        }
    }

    if (e==OK && action!=ABORT)
        return file_size_b + digest_size_b;
    return 0L;
}

int build_disk_image (
        char *userId, 
        char *imageId, char *imageURL, char *image_path,
        char *kernelId, char *kernelURL, char *kernel_path,
        char *ramdiskId, char *ramdiskURL, char *ramdisk_path,
        const char *keyName, 
        long long root_limit_mb, 
        long long swap_size_mb,
        long long ephemeral_size_mb) 
{
    long long image_size_b = 0L;
    long long kernel_size_b = 0L;
    long long ramdisk_size_b = 0L;
    
    int e = ERROR;
    
    logprintfl (EUCAINFO, "retrieving images (root limit=%lldMB)...\n", root_limit_mb);
    
    /* get the necessary files from Walrus, caching them if possible */
    char * image_name;
    int mount_offset = 0;
    long long limit_mb = root_limit_mb; // OK if total is negative (unlimited)
    /*
    if (convert_to_disk) {
        image_name = "disk";
        mount_offset = 32256; // 1st partition offset in the disk image
    } else {
        image_name = "root";
        limit_mb -= swap_size_mb; // account for swap, which will be a separate file
    } 
    */

#define CHECK_LIMIT(WHAT) \
    if (root_limit_mb>0L && limit_mb < 1L) { \
        logprintfl (EUCAFATAL, "error: insufficient disk capacity remaining (%lldMB) in VM Type for component %s\n", limit_mb, WHAT); \
        return e; \
    }

//    static long long get_cached_file (const char * src_url, const char * dst_path, const char * cache_key, sem * s, int convert_to_disk, long long limit_mb, long long swap_size_mb) 

    /* do kernel & ramdisk first, since either the disk or the ephemeral partition will take up the rest */
    if ((kernel_size_b=get_cached_file (kernelURL, kernel_path, kernelId, img_sem, 0, limit_mb, swap_size_mb, ephemeral_size_mb))<1L) return e;
    limit_mb -= kernel_size_b/MEGABYTE;
    CHECK_LIMIT("kernel")
    if (ramdiskId && strlen (ramdiskId) ) {
        if ((ramdisk_size_b=get_cached_file (ramdiskURL, ramdisk_path, ramdiskId, img_sem, 0, limit_mb, swap_size_mb, ephemeral_size_mb))<1L) return e;
        limit_mb -= ramdisk_size_b/MEGABYTE;
        CHECK_LIMIT("ramdisk")
    }
    if ((image_size_b=get_cached_file (imageURL, image_path, imageId, img_sem, 0, limit_mb, swap_size_mb, ephemeral_size_mb))<1L) return e;
    limit_mb -= image_size_b/MEGABYTE;

    logprintfl (EUCAINFO, "preparing images...\n");
    
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
            logprintfl (EUCAINFO, "adding key %s to the root file system at %s using (%s)\n", key_template, image_path, add_key_command_path);
        }
    } else { /* if no key was given, add_key just does tune2fs to up the filesystem mount date */
        key_template = "";
        logprintfl (EUCAINFO, "running tune2fs on the root file system at %s using (%s)\n", image_path, add_key_command_path);
    }

    /* do the key injection and/or tune2fs */
    sem_p (img_sem);
    if (vrun("%s %d %s %s", add_key_command_path, mount_offset, image_path, key_template)!=0) {
        logprintfl (EUCAERROR, "ERROR: key injection / tune2fs command failed\n");
        /* we proceed despite the failure since maybe user embedded the key
         * into the image; also tune2fs may fail on uncrecognized but valid
         * filesystems */
    }
    sem_v (img_sem);
    
    if (strlen(key_template)) {
        if (unlink(key_template) != 0) {
            logprintfl (EUCAWARN, "WARNING: failed to remove temporary key file %s\n", key_template);
        }
        free (key_template);
    }
    
    return 0;
}

static int file_size (const char * file_path)
{
    struct stat mystat;
    int err = stat (file_path, &mystat);
    if (err<0) return err;
    return (int)(mystat.st_size);
}

static void gen_hash (char * hash, const int size, 
    const char * id1, const char *id2, const char *id3, 
    const char *key, const int size1, const int size2, const int size3) 
{
    char buffer [1024*5];
    snprintf (buffer, sizeof(buffer), "%s/%s/%s/%s/%d/%d/%d", id1, id2, id3, key, size1, size2, size3);
    unsigned int code = hash_code (buffer);
    snprintf (hash, size, "%x", code);
}

static int gen_vmdk (const char * disk_path, const char * vmdk_path)
{
    struct stat mystat;
    if (stat (disk_path, &mystat) < 0 ) {
        logprintfl (EUCAERROR, "failed to stat file %s\n", disk_path);
        return 1;
    }
    int total_size = (int)(mystat.st_size/512); // file size in blocks (TODO: do we need to round up?)
    
    static const char desc_template[] =
        "# Disk DescriptorFile\n"
        "version=1\n"
        "CID=%x\n"
        "parentCID=ffffffff\n"
        "createType=\"%s\"\n"
        "\n"
        "# Extent description\n"
        "RW %d %s \"%s\"\n"
        "\n"
        "# The Disk Data Base \n"
        "#DDB\n"
        "\n"
        "ddb.virtualHWVersion = \"%d\"\n"
        "ddb.geometry.cylinders = \"%d\"\n"
        "ddb.geometry.heads = \"16\"\n"
        "ddb.geometry.sectors = \"63\"\n"
        "ddb.adapterType = \"%s\"\n";
    char desc[1024];
    int fd = open (vmdk_path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd<0) {
        logprintfl (EUCAFATAL, "failed to create %s\n", vmdk_path);
        return 1;        
    }
    snprintf(desc, sizeof(desc), desc_template, 
            (unsigned int)time(NULL),
            "vmfs", // can also be "monolithicSparse"
             total_size, // in blocks
             "VMFS", // can also be SPARSE
             "disk-flat.vmdk", // backing store file's name
             4, // qemu-img can also produce 6, vmkfstools seems to be on 7 now
             (int)(total_size / (int64_t)(63 * 16)),
             "lsilogic" // can also be "ide" and "buslogic"
             );
    write(fd, desc, strlen(desc));
    close(fd);
    
    return 0;
}

static int upload_vmdk (const char * disk_path, const char * vmdk_path, const img_spec * dest)
{
    const img_loc * loc = &(dest->location);
    char url[1024];

    logprintfl (EUCAINFO, "uploading disk backing files to destination\n");    
    
    // EXAMPLE: https://192.168.7.236/folder/i-4DD50852?dcPath=ha-datacenter&dsName=S1
    snprintf (url, sizeof(url), "https://%s/%s/%s/%s?dcPath=%s&dsName=%s", loc->vsphere_host, loc->path, dest->id, "disk-flat.vmdk", loc->vsphere_dcPath, loc->vsphere_dsName);
    int rc = http_put (disk_path, url, loc->creds.login, loc->creds.password);
    if (rc) {
        logprintfl (EUCAFATAL, "upload of disk file failed\n");
        return 1;        
    }
    
    snprintf (url, sizeof(url), "https://%s/%s/%s/%s?dcPath=%s&dsName=%s", loc->vsphere_host, loc->path, dest->id, "disk.vmdk", loc->vsphere_dcPath, loc->vsphere_dsName);
    rc = http_put (vmdk_path, url, loc->creds.login, loc->creds.password);
    if (rc) {
        logprintfl (EUCAFATAL, "upload of disk file metadata failed\n");
        return 1;        
    }
    return 0;
}

int img_convert (
        img_spec * root, 
        img_spec * kernel, 
        img_spec * ramdisk, 
        img_spec * dest, 
        const char * key, 
        int rlimit_mb, 
        int ssize_mb, 
        int esize_mb)
{
    int rc, force = 0;
    char path [512];

    snprintf (path, 512, "%s/usr/lib/eucalyptus", euca_home);
    rc = verify_ami2vmx_helpers(force, path);
    if (rc) {
        logprintfl (EUCAFATAL, "failed to verify helpers\n");
        return 1;
    }
    
    // print diagnostic information
    logprintfl (EUCADEBUG, "img_convert:\n");
    print_img_spec ("EMI", root);
    print_img_spec ("EKI", kernel);
    print_img_spec ("RMI", ramdisk);
    print_img_spec ("dst", dest);
    
    // see if we have a saved copy of this exact disk image
    char hash [SIZE]; 
    gen_hash (hash, sizeof(hash), root->id, kernel->id, ramdisk->id, key, rlimit_mb, ssize_mb, esize_mb);
    char cached_path      [SIZE]; snprintf (cached_path,      SIZE, "%s/%s-disk",      g_env.wloc.path, hash);
    char cached_path_vmdk [SIZE]; snprintf (cached_path_vmdk, SIZE, "%s/%s-disk.vmdk", g_env.wloc.path, hash);
    
    if (file_size (cached_path)>0 && file_size (cached_path_vmdk)>0) {
        logprintfl (EUCAINFO, "found a cached copy of the file in %s\n", cached_path);
        rc = upload_vmdk (cached_path, cached_path_vmdk, dest);

    } else {
        char unique_path [SIZE]; 
        snprintf (unique_path,  SIZE, "%s/%s", g_env.wloc.path, dest->id);
        char image_path   [SIZE]; snprintf (image_path,   SIZE, "%s/%s",           unique_path, root->id);
        char kernel_path  [SIZE]; snprintf (kernel_path,  SIZE, "%s/%s",           unique_path, kernel->id);
        char ramdisk_path [SIZE]; snprintf (ramdisk_path, SIZE, "%s/%s",           unique_path, ramdisk->id);
        char disk_path    [SIZE]; snprintf (disk_path,    SIZE, "%s/%s-disk",      unique_path, root->id);
        char vmdk_path    [SIZE]; snprintf (vmdk_path,    SIZE, "%s/%s-disk.vmdk", unique_path, root->id);

        rc = build_disk_image ("admin", 
            root->id, root->location.url, image_path,
            kernel->id, kernel->location.url, kernel_path,
            ramdisk->id, ramdisk->location.url, ramdisk_path,
            key, rlimit_mb, ssize_mb, esize_mb);
        if (rc) {
            logprintfl (EUCAFATAL, "failed to download the necessary components\n");
            goto cleanup;
        }
        
        rc = verify_input (image_path, kernel_path, ramdisk_path, NULL, 0);
        if (rc) {
            logprintfl (EUCAFATAL, "input to ami2vmx not correct\n");
            goto cleanup;
        }

	// if ephemeral is specified as -1, then we set it to the space remaining in the root partition
	if (esize_mb<0) {
	  esize_mb = rlimit_mb - (file_size (image_path) + file_size (kernel_path) + file_size (ramdisk_path))/MEGABYTE;
	  // if root limit is -1 (unspecified) or if there is no room left, then there won't be ephemeral disk
	  if (esize_mb<0) {
	    esize_mb = 0;
	  }
	}
        sem_p (img_sem); // kinda heavy-handed
        rc = do_convert (image_path, disk_path, kernel_path, ramdisk_path, NULL, ssize_mb, esize_mb, 64, 0, force);
        sem_v (img_sem);
        if (rc) {
            logprintfl (EUCAFATAL, "conversion to disk failed\n");
            goto cleanup;        
        }
        
        if (gen_vmdk (disk_path, vmdk_path) || 
            upload_vmdk (disk_path, vmdk_path, dest)) {
            rc = 1;
            goto cleanup;
        }
        rc = 0;
            
        sem_p (img_sem);
        if (file_size (cached_path)<0 && file_size (cached_path_vmdk)<0) {
            rename (disk_path, cached_path);
            rename (vmdk_path, cached_path_vmdk);
        }
        sem_v (img_sem);
        
    cleanup:
        unlink (disk_path); // will silently fail if they've been renamed
        unlink (vmdk_path);
        
        unlink (image_path);
        unlink (kernel_path);
        unlink (ramdisk_path);
        rmdir (unique_path);        
    }

    return rc;
}

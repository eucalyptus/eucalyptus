// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

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
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#define _FILE_OFFSET_BITS 64 // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h> // strlen, strcpy
#include <ctype.h> // isspace
#include <assert.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <time.h>
#include <math.h> // powf 
#include <fcntl.h> // open
#include <utime.h> // utime 
#include <sys/wait.h>
#include <sys/types.h>
#include <dirent.h> // opendir, etc
#include <errno.h> // errno
#include <sys/time.h> // gettimeofday
#include <limits.h>
#include <sys/mman.h> // mmap
#include <pthread.h>

#include "misc.h"
#include "euca_auth.h"
#include "diskutil.h"
#include "vnetwork.h"

// Given an array of pointers to command names (e.g., "ls", "dd", etc.),
// as well as either an array of NULL pointers or pointers to full paths,
// either the full paths are verified or system $PATH is searched for 
// the names and full paths are pointed to by helpers_path[] entries. 
// Number of missing entries is returned or -1 for error.
int verify_helpers (char **helpers, char **helpers_path, int num_helpers) 
{
    int missing_helpers = 0;
    char **tmp_helpers_path = helpers_path;

    if(helpers_path==NULL)
        tmp_helpers_path = (char**) calloc (num_helpers, sizeof(char*));
    
    for (int i=0; i<num_helpers; i++) {
        struct stat statbuf;
        int done = 0;

        // full path was given, so it just needs to be verified
        if (helpers_path != NULL && helpers_path[i]!=NULL) {
            int rc = stat (helpers_path[i], &statbuf);
            if (!rc && S_ISREG(statbuf.st_mode)) {
                done++;
            }

        } else { // no full path was given, so search $PATH
            char *tok, *toka, *path, *helper, *save, *savea;

            tok = getenv("PATH");
            if (!tok) return -1;

            path = strdup(tok);
            if (!path) {
                missing_helpers = -1;
                goto cleanup;
            }

            { // append some Eucalyptus-specific locations to $PATH
                char * euca = getenv ("EUCALYPTUS");
                if (euca == NULL) {
                    euca = "";
                }
                char * locations [] = {
                    "/usr/lib/eucalyptus",
                    "/usr/share/eucalyptus",
                    "/usr/sbin",
                    NULL
                };
                for (int i = 0; locations [i]; i++) {
                    char lpath [MAX_PATH];
                    snprintf (lpath, sizeof (lpath), ":%s%s", euca, locations [i]);
                    char * newpath = strdupcat (path, lpath);
                    if (newpath == NULL) {
                        missing_helpers = -1;
                        goto cleanup;
                    }
                    path = newpath;
                }
            }
            
            tok = strtok_r(path, ":", &save);
            done=0;
            while (tok && !done) {
                helper = strdup(helpers[i]);
                toka = strtok_r(helper, ",", &savea);
                while (toka && !done) {
                    char file[MAX_PATH];

                    snprintf(file, MAX_PATH, "%s/%s", tok, toka);
                    int rc = stat(file, &statbuf);
                    if (!rc) {
                        if (S_ISREG(statbuf.st_mode)) {
                            tmp_helpers_path[i] = strdup(file);
                            done++;
                        }
                    }
                    toka = strtok_r(NULL, ":", &savea);
                }
                tok = strtok_r(NULL, ":", &save);
                if (helper) free(helper);
            }
            free(path);
        }

        if (!done) {
            missing_helpers++;
            logprintfl (EUCADEBUG2, "did not find '%s' in path\n", helpers[i]);
        } else {
            logprintfl (EUCADEBUG2, "found '%s' at '%s'\n", helpers[i], tmp_helpers_path[i]);
        }
    }

 cleanup:
    
    if(helpers_path == NULL) { 
        for(int i = 0; i < num_helpers; i++) 
            if(tmp_helpers_path[i])
                free(tmp_helpers_path[i]);
        free(tmp_helpers_path);
    }

    return missing_helpers;
}

int timeread(int fd, void *buf, size_t bytes, int timeout) {
  int rc;
  fd_set rfds;
  struct timeval tv;

  if (timeout <= 0) timeout = 1;

  FD_ZERO(&rfds);
  FD_SET(fd, &rfds);
  
  tv.tv_sec = timeout;
  tv.tv_usec = 0;
  
  rc = select(fd+1, &rfds, NULL, NULL, &tv);
  if (rc <= 0) {
    // timeout
    logprintfl(EUCAERROR, "timeread(): select() timed out for read: timeout=%d\n", timeout);
    return(-1);
  }
  rc = read(fd, buf, bytes);
  return(rc);
}

int add_euca_to_path (const char * euca_home_supplied) 
{
    const char * euca_home;
    if (euca_home_supplied && strlen (euca_home_supplied)) {
        euca_home = euca_home_supplied;
    } else if (getenv(EUCALYPTUS_ENV_VAR_NAME) && strlen (getenv(EUCALYPTUS_ENV_VAR_NAME))) {
        euca_home = getenv(EUCALYPTUS_ENV_VAR_NAME);
    } else { // we'll assume root
        euca_home = "";
    }

    char * old_path = getenv ("PATH");
    if (old_path==NULL) old_path = "";

    char new_path [4098];
    snprintf (new_path, sizeof (new_path), 
              "%s/usr/share/eucalyptus:" // (connect|disconnect iscsi, get_xen_info, getstats, get_sys_info)
              "%s/usr/sbin:" // (eucalyptus-cloud, euca_conf, euca_sync_key, euca-* admin commands)
              "%s/usr/lib/eucalyptus:" // (rootwrap, mountwrap)
              "%s",
              euca_home, euca_home, euca_home,
              old_path);
    return setenv ("PATH", new_path, TRUE);
}

// Wrapper around waitpid() that retries waiting until
// a timeout, specified in seconds, occurs. Return value 
// is that of waitpid():
//
//  -1 means there was an error
//   0 means there was a timeout
//   N is the PID of the process 
// 
// When a positive value is returned, status is set
// to the exit status of the process

pid_t timewait (pid_t pid, int *status, int timeout_sec) 
{    
    if (timeout_sec < 0) // do not allow negative timeouts
        timeout_sec = 0;
    
    *status = 1; // TODO: remove this once we know that no callers rely on status to detect timeout
    
    int rc = waitpid (pid, status, WNOHANG);
    time_t elapsed_usec = 0;
    while (rc == 0 && elapsed_usec < (timeout_sec * 1000000)) {
        usleep (10000);
        elapsed_usec += 10000;
        rc = waitpid(pid, status, WNOHANG);
    }
    if (rc == 0) {
        logprintfl(EUCAERROR, "waitpid() timed out: pid=%d\n", pid);
    }
    return (rc);
}

int timelog=0; /* change to 1 for TIMELOG entries */

int logging=0;
int loglevel=EUCADEBUG;
int logrollnumber=4;
FILE *LOGFH=NULL;
char logFile[MAX_PATH];

int param_check(char *func, ...) {
  int fail;
  va_list al;

  if (!func) {
    return(1);
  }
  
  va_start(al, func);
  fail=0;
  if (!strcmp(func, "vnetGenerateDHCP") || !strcmp(func, "vnetKickDHCP")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    if (!a) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetAddPublicIP") || !strcmp(func, "vnetAddDev")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    if (!a || !b) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetAddHost")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    int d = va_arg(al, int);
    if (!a || !b || (d < 0) || (d > NUMBER_OF_VLANS-1)) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetGetNextHost")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    int d = va_arg(al, int);
    if (!a || !b || !c || d < 0 || d > NUMBER_OF_VLANS-1) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetDelHost") || !strcmp(func, "vnetEnableHost") || !strcmp(func, "vnetDisableHost")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    int d = va_arg(al, int);
    if (!a || (!b && !c) || d < 0 || d > NUMBER_OF_VLANS-1) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetDeleteChain") || !strcmp(func, "vnetCreateChain")) { 
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    if (!a || !b || !c) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetTableRule")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    char *d = va_arg(al, char *);
    char *e = va_arg(al, char *);
    char *f = va_arg(al, char *);
    char *g = va_arg(al, char *);
    if (!a || !b || !c || !d || (!e && !f && !g)) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetSetVlan")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    int b = va_arg(al, int);
    char *c = va_arg(al, char *);
    char *d = va_arg(al, char *);
    if (!a || b < 0 || b >= NUMBER_OF_VLANS || !c || !d) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetDelVlan")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    int b = va_arg(al, int);
    if (!a || b < 0 || b >= NUMBER_OF_VLANS) {
      fail=1;
    }
  } else if (!strcmp(func, "vnetInit")) {
    vnetConfig *a = va_arg(al, vnetConfig *);
    char *b = va_arg(al, char *);
    char *c = va_arg(al, char *);
    char *d = va_arg(al, char *);
    int e = va_arg(al, int);
    if (!a || !b || !c || d<0) {
      fail=1;
    }
  }

  va_end(al);

  if (fail) {
    logprintfl (EUCAERROR, "INTERNAL ERROR: incorrect input parameters to function %s\n", func);
    return(1);
  }
  return(0);
}

int check_process(pid_t pid, char *search) {
  char file[MAX_PATH], buf[1024];
  FILE *FH=NULL;
  int rc, ret=0;

  snprintf(file, MAX_PATH, "/proc/%d/cmdline", pid);
  rc = check_file(file);
  if (!rc) {
    // cmdline exists
    ret = 1;
    if (search) {
      // check if cmdline contains 'search'
      FH = fopen(file, "r");
      if (FH) {
	bzero(buf, 1024);
	while(fgets(buf, 1024, FH)) {
	  char *p;
	  while((p = memchr(buf, '\0', 1024))) {
	    *p = 'X';
	  }
	  buf[1023] = '\0';
	  if (strstr(buf, search)) {
	    ret = 0;
	  }
	}
	fclose(FH);
      }
    } else {
      ret = 0;
    }
  } else {
    ret = 1;
  }
  return(ret);
}

// make sure 'dir' is a directory or a soft-link to one
// and that it is readable by the current user (1 on error)
int check_directory (const char *dir) 
{    
    if (!dir) {
        return (1);
    }
    
    char checked_dir [MAX_PATH];
    snprintf (checked_dir, sizeof (checked_dir), "%s", dir);
    
    struct stat mystat;
    int rc = lstat (checked_dir, &mystat);
    if (rc < 0)
        return 1;
    
    // if a soft link, append '/' and try lstat() again
    if (!S_ISDIR(mystat.st_mode) && S_ISLNK(mystat.st_mode)) {
        snprintf (checked_dir, sizeof (checked_dir), "%s/", dir);
        rc = lstat (checked_dir, &mystat);
        if (rc < 0)
            return 1;
    } 
    
    if (!S_ISDIR (mystat.st_mode)) 
        return 1;
    
    DIR * d = opendir (checked_dir);
    if (d==NULL)
        return 1;

    closedir (d);
    return 0;
}

int check_file_newer_than(char *file, time_t mtime) {
  struct stat mystat;
  int rc;

  if (!file) {
    return(1);
  } else if (mtime <= 0) {
    return(0);
  }
  
  bzero(&mystat, sizeof(struct stat));
  rc = stat(file, &mystat);
  if (rc) {
    return(1);
  }
  if (mystat.st_mtime > mtime) {
    return(0);
  }
  return(1);
}

int check_block (const char *file)
{
  int rc;
  struct stat mystat;

  if (!file) {
    return(1);
  }

  rc = lstat(file, &mystat);
  if (rc < 0 || !S_ISBLK(mystat.st_mode)) {
    return(1);
  }
  return(0);
}

// returns 0 if file is a readable regular file, 1 otherwise
int check_file (const char *file) 
{
    int rc;
    struct stat mystat;
    
    if (!file) {
        return(1);
    }
    
    rc = lstat(file, &mystat);
    if (rc < 0 || !S_ISREG(mystat.st_mode)) {
        return(1);
    }
    return(0);
}

// return 0 if path exists
int check_path (const char *path) 
{
    int rc;
    struct stat mystat;
    
    if (!path) {
        return(1);
    }
    
    rc = lstat(path, &mystat);
    if (rc < 0) {
        return(1);
    }
    return(0);
}

/* given string *stringp, replace occurences of <source> with <destination>
 * and return the new string in stringp */
char * replace_string (char ** stringp, char * source, char * destination )
{
    char *start=NULL, *substart=NULL, *tok=NULL, * new_string=NULL;
    if (source==NULL || destination==NULL) return NULL;
    char * buf;
    int maxlen = 32768*2;
    
    buf = malloc(sizeof(char) * maxlen);
    new_string = malloc(sizeof(char) * maxlen); /* TODO: this has to be dynamic */
    if (!buf || !new_string) {
        fprintf(stderr, "replace_string: out of memory\n");
	if (buf) free(buf);
	if (new_string) free(new_string);
	return NULL;
    }
    bzero(new_string, maxlen);
    
    start = * stringp;
    substart = start;
    tok = strstr(start, source);
    while(tok != NULL) {
        *tok = '\0';
        snprintf (buf, maxlen, "%s%s%s", new_string, substart, destination);
        strncpy (new_string, buf, maxlen);
        tok+=strlen(source);
        substart = tok;
        tok = strstr(substart, source);
    }
    snprintf (buf, maxlen, "%s%s", new_string, substart);
    strncpy (new_string, buf, maxlen);
    if (buf) free(buf);

    free (* stringp);
    * stringp = new_string;

    
    return new_string;
}

/* do sscanf() on each line in lines[], returning upon first match */
/* returns 1 if there was match and 0 otherwise */
int sscanf_lines (char * lines, char * format, void * varp) 
{
    char * copy;
    char * start, * end;
    int found = 0;

    if (!lines) return found;
    copy = strdup(lines);
    if (!copy) return found;

    for (start = copy; start && *start!='\0' && !found; start = end+1 ) {
        int newline = 0;

        for (end = start+1; *end!='\n' && *end!='\0'; end++);
        if (*end=='\n') {
            *end='\0';
            newline = 1;
        }

        if (sscanf (start, format, varp)==1) 
            found=1;

        if (!newline) {
            end--; /* so that start=='\0' */
        }
    }
    free(copy);
        
    return found;
}

char * fp2str (FILE * fp)
{
#   define INCREMENT 512
    int buf_max = INCREMENT;
    int buf_current = 0;
    char * last_read;
    char * buf = NULL;
    
    if (fp==NULL) return NULL;
    do {
        // create/enlarge the buffer
        void * new_buf;
        if ((new_buf = realloc (buf, buf_max)) == NULL) {
            if ( buf != NULL ) { // previous realloc()s worked
                free (buf); // free partial buffer
            }
            return NULL;
        }
        memset(new_buf+buf_current, 0, INCREMENT * sizeof(char));

        buf = new_buf;
        logprintfl (EUCADEBUG3, "fp2str: enlarged buf to %d\n", buf_max);
        
        do { // read in until EOF or buffer is full
            last_read = fgets (buf+buf_current, buf_max-buf_current, fp);
            if ( last_read != NULL ) {
                buf_current = strlen(buf);
            } else {
                if (! feof(fp)) {
                    logprintfl (EUCAERROR, "fp2str: failed while reading from file handle\n");
                    free (buf);
                    return NULL;
                }
            }
            logprintfl (EUCADEBUG3, "fp2str: read %d characters so far (max=%d, last=%s)\n", buf_current, buf_max, last_read?"no":"yes");
        } while ( last_read && buf_max > buf_current+1 ); // +1 is needed for fgets() to put \0
        
        buf_max += INCREMENT; // in case it is full
    } while (last_read);
    
    return buf;
}

/* execute system(shell_command) and return stdout in new string
 * pointed to by *stringp */
char * system_output (char * shell_command )
{
  char * buf = NULL;
  FILE * fp;

  /* forks off command (this doesn't fail if command doesn't exist */
  logprintfl (EUCADEBUG2, "system_output(): [%s]\n", shell_command);
  if ( (fp=popen(shell_command, "r")) == NULL) 
    return NULL; /* caller can check errno */
  buf = fp2str (fp);

  pclose(fp);

  if (buf && (strlen(buf) == 0)) {
      free(buf);
      buf = NULL;
  }
  return buf;
}


char *getConfString(char configFiles[][MAX_PATH], int numFiles, char *key) {
  int rc, i, done;
  char *tmpstr=NULL;

  done=0;
  for (i=0; i<numFiles && !done; i++) {
    rc = get_conf_var(configFiles[i], key, &tmpstr);
    if (rc == 1) {
      done++;
    }
  }
  if (tmpstr && strlen(tmpstr)) {
    char *tmpptr;
    tmpptr = tmpstr + (strlen(tmpstr)-1);
    while(*tmpptr == ' ') {
      *tmpptr = '\0';
      tmpptr = tmpstr + (strlen(tmpstr)-1);
    }
  }
  return(tmpstr);
}


/* search for variable #name# in file #path# and return whatever is after
 * = in value (which will need to be freed).
 *
 * Example of what we are able to parse:
 * TEST="test"
 * TEST=test
 *     TEST   = test
 *
 * Return 1 on success, 0 on variable not found and -1 on error (parse or
 * file not found)
 */
int
get_conf_var(	const char *path, 
		const char *name,
		char **value) {
	FILE *f;
	char *buf, *ptr, *ret;
	int len;

	/* sanity check */
	if (path == NULL || path[0] == '\0' || name == NULL  
			|| name[0] == '\0'|| value == NULL) {
		return -1;
	}
	*value = NULL;

	f = fopen(path, "r");
	if (f == NULL) {
		return -1;
	}

	len = strlen(name);
	buf = malloc(sizeof(char) * 32768);
	while (fgets(buf, 32768, f)) {
		/* the process here is fairly simple: spaces are not
		 * considered (unless between "") so we remove them
		 * before every step. We look for the variable *name*
		 * first, then for an = then for the value */
		for (ptr = buf; (*ptr != '\0') && isspace((int)*ptr); ptr++)
			; 
		if (strncmp(ptr, name, len) != 0) {
			continue;
		}
		for (ptr += len; (*ptr != '\0') && isspace((int)*ptr); ptr++)
			;
		if (*ptr != '=') {
			continue;
		}
		/* we are in business */
		for (ptr++; (*ptr != '\0') && isspace((int)*ptr); ptr++)
			;
		if (*ptr == '"') {
			/* we have a quote, we need the companion */
			ret = ++ptr;
			while ((*ptr != '"')) {
				if (*ptr == '\0') {
					/* something wrong happened */
				  fclose(f);
				  free(buf);
				  return -1;
				}
				ptr++;
			}
		} else {
			/* well we get the single word right after the = */
			ret = ptr;
			while (!isspace((int)*ptr) && *ptr != '#' 
					&& *ptr != '\0') {
				ptr++;
			}
		}
		*ptr = '\0';
		*value = strdup(ret);
		if (*value == NULL) {
		  fclose(f);
		  free(buf);
		  return -1;
		}
		fclose(f);
		free(buf);
		return 1;
	}
	fclose(f);
	free(buf);
	return 0;
}

void
free_char_list(char **value) {
	int i;

	if (value == NULL || *value == NULL) {
		return;
	}

	for (i = 0; value[i] != NULL; i++) {
		free(value[i]);
	}
	free(value);
}


char **
from_var_to_char_list(	const char *v) {
	char *value, **tmp, *ptr, *w, a;
	int i;

	/* sanity check */
	if (v == NULL || v[0] == '\0') {
		return NULL;
	}
	tmp = malloc(sizeof(char*));
	if (tmp == NULL) {
		return NULL;
	}
	value = strdup(v);
	if (value == NULL) {
		free(tmp);
		return NULL;
	}
	tmp[0] = NULL;

	i = 0;
	ptr = value;
	for (i = 0, ptr = value; *ptr != '\0'; ptr++) {
		/* let's look for the beginning of the word */
		for (; *ptr != '\0' && isspace((int)*ptr); ptr++)
			;
		if (*ptr == '\0') {
			/* end of string with no starting word: we are
			 * done here */
			break;
		}
		w = ptr;		/* beginning of word */
		for (ptr++; *ptr != '\0' && !isspace((int)*ptr); ptr++)
			;

		/* found the end of word */
		a = *ptr;
		*ptr = '\0';

		tmp = realloc(tmp, sizeof(char *)*(i+2));
		if (tmp == NULL) {
			free(value);
			return NULL;
		}
		tmp[i] = strdup(w);
		if (tmp[i] == NULL) {
			free_char_list(tmp);
			free(value);
			return NULL;
		}
		tmp[++i] = NULL;

		/* now we need to check if we were at the end of the
		 * string */
		if (a == '\0') {
			break;
		}
	}
	free(value);

	return tmp;
}

int logfile(char *file, int in_loglevel, int in_logrollnumber) {
  logging = 0;
  if (in_loglevel >= EUCADEBUG2 && in_loglevel <= EUCAFATAL) {
    loglevel = in_loglevel;
  } else {
    loglevel = EUCADEBUG;
  }
  if (file == NULL) {
    LOGFH = NULL;
  } else {
    if (LOGFH != NULL) {
      fclose(LOGFH);
    }
    
    snprintf(logFile, MAX_PATH, "%s", file);
    LOGFH = fopen(file, "a");
    if (LOGFH) {
      logging=1;
    }
  }
  if (in_logrollnumber > 0 && in_logrollnumber < 100) {
      logrollnumber = in_logrollnumber;
  }
  return(1-logging);
}

void eventlog(char *hostTag, char *userTag, char *cid, char *eventTag, char *other) {
  double ts;
  struct timeval tv;
  char hostTagFull[256];
  char hostName [256];
  FILE *PH;

  if (!timelog) return;

  hostTagFull[0] = '\0';
  PH = popen("hostname", "r");
  if(PH) {
      fscanf(PH, "%256s", hostName);
      pclose(PH);
  
      snprintf (hostTagFull, 256, "%s/%s", hostName, hostTag);
  
      gettimeofday(&tv, NULL);
      ts = (double)tv.tv_sec + ((double)tv.tv_usec / 1000000.0);

      logprintf("TIMELOG %s:%s:%s:%s:%f:%s\n", hostTagFull, userTag, cid, eventTag, ts, other);
  }
}

int logprintf(const char *format, ...) {
  va_list ap;
  int rc;
  char buf[27], *eol;
  time_t t;
  FILE *file;
  
  rc = 1;
  va_start(ap, format);
  
  if (logging) {
    file = LOGFH;
  } else {
    file = stdout;
  }
  
  t = time(NULL);
  if (ctime_r(&t, buf)) {
    eol = strchr(buf, '\n');
    if (eol) {
      *eol = '\0';
    }
    fprintf(file, "[%s] ", buf);
  }
  rc = vfprintf(file, format, ap);
  fflush(file);
  
  va_end(ap);
  return(rc);
}

int logprintfl(int level, const char *format, ...) {
  va_list ap;
  int rc, fd;
  char buf[27], *eol;
  time_t t;
  struct stat statbuf;
  FILE *file;
  
  if (level < loglevel) {
    return(0);
  }
  
  rc = 1;
  va_start(ap, format);
  
  if (logging) {
    file = LOGFH;
    fd = fileno(file);
    if (fd > 0) {
      rc = fstat(fd, &statbuf);
      if (!rc && ((int)statbuf.st_size > MAXLOGFILESIZE)) {
	int i;
	char oldFile[MAX_PATH], newFile[MAX_PATH];
	
	rc = stat(logFile, &statbuf);
	if (!rc && ((int)statbuf.st_size > MAXLOGFILESIZE)) {
	  for (i=logrollnumber; i>=0; i--) {
	    snprintf(oldFile, MAX_PATH, "%s.%d", logFile, i);
	    snprintf(newFile, MAX_PATH, "%s.%d", logFile, i+1);
	    rename(oldFile, newFile);
	  }
	  snprintf(oldFile, MAX_PATH, "%s", logFile);
	  snprintf(newFile, MAX_PATH, "%s.%d", logFile, 0);
	  rename(oldFile, newFile);
	}
	fclose(LOGFH);
	LOGFH = fopen(logFile, "a");
	if (LOGFH) {
	  file = LOGFH;
	} else {
	  file = stdout;
	}
      }
    }
  } else {
    file = stderr;
  }

  
  t = time(NULL);
  if (ctime_r(&t, buf)) {
    eol = strchr(buf, '\n');
    if (eol) {
      *eol = '\0';
    }
    fprintf(file, "[%s]", buf);
  }

  fprintf(file, "[%06d]", getpid());
  if (level == EUCADEBUG2) {fprintf(file, "[%-10s] ", "EUCADEBUG2");}
  else if (level == EUCADEBUG) {fprintf(file, "[%-10s] ", "EUCADEBUG");}
  else if (level == EUCAINFO) {fprintf(file, "[%-10s] ", "EUCAINFO");}
  else if (level == EUCAWARN) {fprintf(file, "[%-10s] ", "EUCAWARN");}
  else if (level == EUCAERROR) {fprintf(file, "[%-10s] ", "EUCAERROR");}
  else if (level == EUCAFATAL) {fprintf(file, "[%-10s] ", "EUCAFATAL");}
  else {fprintf(file, "[%-10s] ", "EUCADEBUG");}
  rc = vfprintf(file, format, ap);
  fflush(file);
  
  va_end(ap);
  return(rc);
}

/* implements Java's String.hashCode() */
int hash_code (const char * s)
{
	int code = 0;
	int i;
	
	if (!s) return code;
	
	int len = strlen((char *)s);
	for (i = 0; i<len; i++) {
		code = 31 * code + (unsigned char) s[i];
	}
	
	return code;
}

/* given a string, returns 3 relevant statistics as a static string */
#define OUTSIZE 50
char * get_string_stats (const char * s)
{
	static char out [OUTSIZE]; // TODO: malloc this?
	int len = (int)strlen(s);
	snprintf (out, OUTSIZE, "length=%d buf[n-1]=%i hash=%d", len, (int)((signed char)s[len-1]), hash_code(s));
	return out;
}

#define BUFSIZE 1024

/* daemonize and store pid in pidfile. if pidfile exists and contained
   pid is daemon already running, do nothing.  force option will first
   kill and then re-daemonize */
int daemonmaintain(char *cmd, char *procname, char *pidfile, int force, char *rootwrap) {
  int rc, found, ret;
  char cmdstr[MAX_PATH], file[MAX_PATH];
  FILE *FH;
  
  if (!cmd || !procname) {
    return(1);
  }
  
  if (pidfile) {
    found=0;
    rc = check_file(pidfile);
    if (!rc) {
      char *pidstr=NULL;
      // pidfile exists
      pidstr = file2str(pidfile);
      if (pidstr) {
	snprintf(file, MAX_PATH, "/proc/%s/cmdline", pidstr);
	if (!check_file(file)) {
	  FH = fopen(file, "r");
	  if (FH) {
	    if (fgets(cmdstr, MAX_PATH, FH)) {
	      if (strstr(cmdstr, procname)) {
		// process is running, and is indeed procname
		found=1;
	      }
	    }
	    fclose(FH);
	  }
	}
	free(pidstr);
      }
    }
    
    if (found) {
      // pidfile passed in and process is already running
      if (force) {
	// kill process and remove pidfile
	rc = safekillfile(pidfile, procname, 9, rootwrap);
      } else {
	// nothing to do
	return(0);
      }
    } else {
      // pidfile passed in but process is not running
      if (!check_file(pidfile)) {
	unlink(pidfile);
      }
    }
  }
  
  rc = daemonrun(cmd, pidfile);
  if (!rc) {
    // success
    //    if (pidfile) {
    //      char pidstr[32];
    //      snprintf(pidstr, 32, "%d", *dpid);
    //      rc = write2file(pidfile, pidstr);
    //    }
    ret = 0;
  } else {
    ret = 1;
  }
  return(ret);
}

/* daemonize and run 'cmd', returning pid of the daemonized process */
int daemonrun(char *incmd, char *pidfile) {
  int pid, sid, i, status, rc;
  char **argv=NULL, *cmd;
  
  if (!incmd) {
    return(1);
  }
  
  pid = fork();
  if (pid < 0) {
    return(1);
  }
  if (!pid) {
    char *tok=NULL, *ptr=NULL;
    int idx, rc;
    struct sigaction newsigact;

    newsigact.sa_handler = SIG_DFL;
    newsigact.sa_flags = 0;
    sigemptyset(&newsigact.sa_mask);
    sigprocmask(SIG_SETMASK, &newsigact.sa_mask, NULL);
    sigaction(SIGTERM, &newsigact, NULL);
    
    rc = daemon(0,0);
    // become parent of session
    sid = setsid();
    
    // construct argv
    cmd = strdup(incmd);
    idx=0;
    argv = realloc(NULL, sizeof(char *));
    tok = strtok_r(cmd, " ", &ptr);
    while(tok) {
      fflush(stdout);
      argv[idx] = strdup(tok);
      idx++;
      tok = strtok_r(NULL, " ", &ptr);
      argv = realloc(argv, sizeof(char *) * (idx+1));
    }
    argv[idx] = NULL;
    free(cmd);
    
    // close all fds
    for (i=0; i<sysconf(_SC_OPEN_MAX); i++) {
      close(i);
    }

    if (pidfile) {
      char pidstr[32];
      snprintf(pidstr, 32, "%d", getpid());
      rc = write2file(pidfile, pidstr);
    }
    // run
    exit(execvp(*argv, argv));
  } else {
    rc = waitpid(pid, &status, 0);
  }
  
  return(0);
}

/* given printf-style arguments, run the resulting string in the shell */
int vrun (const char * fmt, ...)
{
	char buf [MAX_PATH];
	int e;
	
	va_list ap;
    va_start (ap, fmt);
	vsnprintf (buf, MAX_PATH, fmt, ap);
	va_end (ap);
	
    logprintfl (EUCAINFO, "vrun(): [%s]\n", buf);
	if ((e = system (buf)) != 0) {
		logprintfl (EUCAERROR, "system(%s) failed with %d\n", buf, e); /* TODO: remove? */
	}
	return e;
}

/* given a file path, prints it to stdout */
int cat (const char * file_name)
{
	int got;
	int put = 0;
	char buf [BUFSIZE];
	
	int fd = open (file_name, O_RDONLY);
	if (fd == -1) {
		// we should print some error
		return put;
	}
	while ( ( got = read (fd, buf, BUFSIZE)) > 0) {
		put += write (1, buf, got);
	}
	close (fd);
	return put;
}

/* prints contents of a file with logprintf */
int logcat (int debug_level, const char * file_name)
{
	int got = 0;
	char buf [BUFSIZE];
	
	FILE *fp = fopen (file_name, "r");
	if (!fp) return got;
    while ( fgets (buf, BUFSIZE, fp) ) {
        int l = strlen (buf);
        if ( l<0 ) 
            break;
        if ( l+1<BUFSIZE && buf[l-1]!='\n' ) {
            buf [l++] = '\n';
            buf [l] = '\0';
        }
        logprintfl (debug_level, buf);
        got += l;
	}
    fclose (fp);
	return got;
}

/* "touch" a file, creating if necessary */
int touch (const char * path)
{
    int ret = 0;
    int fd;
    
    if ( (fd = open (path, O_WRONLY | O_CREAT | O_NONBLOCK, 0644)) >= 0 ) {
        close (fd);
        if (utime (path, NULL)!=0) {
            logprintfl (EUCAERROR, "error: touch(): failed to adjust time for %s (%s)\n", path, strerror (errno));
            ret = 1;
        }
    } else {
        logprintfl (EUCAERROR, "error: touch(): failed to create/open file %s (%s)\n", path, strerror (errno));
        ret = 1;
    }
    return ret;
}

/* diffs two files: 0=same, -N=different, N=error */
int diff (const char * path1, const char * path2)
{
    int fd1, fd2;
    char buf1 [BUFSIZE], buf2 [BUFSIZE];

    if ( (fd1 = open (path1, O_RDONLY)) < 0 ) {
        logprintfl (EUCAERROR, "error: diff(): failed to open %s\n", path1);
    } else if ( (fd2 = open (path2, O_RDONLY)) < 0 ) {
        logprintfl (EUCAERROR, "error: diff(): failed to open %s\n", path2);
    } else {
        int read1, read2;
        do {
            read1 = read (fd1, buf1, BUFSIZE);
            read2 = read (fd2, buf2, BUFSIZE);
            if (read1!=read2) break;
            if (read1 && memcmp (buf1, buf2, read1)) break;
        } while (read1);
        close (fd1);
        close (fd2);
        return (-(read1 + read2)); /* both should be 0s if files are equal */
    }
    return ERROR;
}

/* sums up sizes of files in the directory, as well as the size of the
 * directory itself; no subdirectories are allowed - if there are any, this
 * returns -1 */
long long dir_size (const char * path)
{
    struct stat mystat;
    DIR * dir;
    long long size = 0;

    if ((dir=opendir(path))==NULL) {
        logprintfl (EUCAWARN, "warning: unopeneable directory %s\n", path);
        return -1;
    }
    if (stat (path, &mystat) < 0) {
        logprintfl (EUCAWARN, "warning: could not stat %s\n", path);
	closedir(dir);
        return -1;
    }
    size += (long long)mystat.st_size;

    struct dirent * dir_entry;
    while ((dir_entry=readdir(dir))!=NULL) {
        char * name = dir_entry->d_name;
        unsigned char type = dir_entry->d_type;

        if (!strcmp(".", name) ||
            !strcmp("..", name))
            continue;

        if (DT_REG!=type) {
            logprintfl (EUCAWARN, "warning: non-regular (type=%d) file %s/%s\n", type, path, name);
            size = -1;
            break;
        }
        
        char filepath [MAX_PATH];
        snprintf (filepath, MAX_PATH, "%s/%s", path, name);
        if (stat (filepath, &mystat) < 0 ) {
            logprintfl (EUCAWARN, "warning: could not stat file %s\n", filepath);
            size = -1;
            break;
        }
        
        size += (long long)mystat.st_size;
    }

    closedir (dir);
    return size;
}

int write2file(const char *path, char *str) {
  FILE *FH=NULL;
  
  FH = fopen(path, "w");
  if (FH) {
    fprintf(FH, "%s", str);
    fclose(FH);
  } else {
    return(1);
  }
  return(0);
}

char * file2strn (const char * path, const ssize_t limit) 
{
  struct stat mystat;
  if (stat (path, &mystat) < 0) {
    logprintfl (EUCAERROR, "error: file2strn() could not stat file %s\n", path);
    return NULL;
  }
  if (mystat.st_size>limit) {
    logprintfl (EUCAERROR, "error: file %s exceeds the limit (%d) in file2strn()\n", path, limit);
    return NULL;
  }
  return file2str (path);
}

/* read file 'path' into a new string */
char * file2str (const char * path)
{
    char * content = NULL;
    int file_size;

    struct stat mystat;
    if (stat (path, &mystat) < 0) {
        logprintfl (EUCAERROR, "error: file2str() could not stat file %s\n", path);
        return content;
    }
    file_size = mystat.st_size;

    if ( (content = malloc (file_size+1)) == NULL ) {
        logprintfl (EUCAERROR, "error: file2str() out of memory reading file %s\n", path);
        return content;
    }

    int fp;
    if ( ( fp = open (path, O_RDONLY) ) < 1 ) {
        logprintfl (EUCAERROR, "error: file2str() failed to open file %s\n", path);
        free (content);
        content = NULL;
        return content;
    }

    int bytes;
    int bytes_total = 0;
    int to_read = ((SSIZE_MAX)<file_size)?(SSIZE_MAX):file_size;
    char * p = content;
    while ( (bytes = read (fp, p, to_read)) > 0) {
        bytes_total += bytes;
        p += bytes;
        if (to_read > (file_size-bytes_total)) {
            to_read = file_size-bytes_total;
        }
    }
    close(fp);

    if ( bytes < 0 ) {
        logprintfl (EUCAERROR, "error: file2str() failed to read file %s\n", path);
        free (content);
        content = NULL;
        return content;
    }

    * p = '\0';
    return content;
}

char *file2str_seek(char *file, size_t size, int mode) {
  int rc, fd;
  struct stat statbuf;
  char *ret=NULL;

  if (!file || size <= 0) {
    logprintfl(EUCAERROR, "file2str_seek(): bad input parameters\n");
    return(NULL);
  }

  ret = malloc(size);
  if (!ret) {
    logprintfl(EUCAERROR, "file2str_seek(): out of memory!\n");
    return(NULL);
  }

  rc = stat(file, &statbuf);
  if (rc >= 0) {
    fd = open(file, O_RDONLY);
    if (fd >= 0) {

      if (mode == 1) {
	rc = lseek(fd, (off_t)(-1 * size), SEEK_END);
	if (rc < 0) {
	  rc = lseek(fd, (off_t)0, SEEK_SET);
	  if (rc < 0) {
	    logprintfl(EUCAERROR, "file2str_seek(): cannot seek\n");
	    if (ret) free(ret);
	    return(NULL);
	  }
	}
      }

      bzero(ret, size);
      rc = read(fd, ret, (size)-1);
      close(fd);
    } else {
      logprintfl(EUCAERROR, "file2str_seek(): cannot open '%s' read-only\n", file);
      if (ret) free(ret);
      return(NULL);
    }
  } else {
    logprintfl(EUCAERROR, "file2str_seek(): cannot stat console_output file '%s'\n", file);
    if (ret) free(ret);
    return(NULL);
  }
  
  return(ret);
}

// extract string from str bound by 'begin' and 'end'
char * str2str (const char * str, const char * begin, const char * end)
{
  char * buf = NULL;

    if ( str==NULL || begin==NULL || end==NULL || strlen (str)<3 || strlen (begin)<1 || strlen (end)<1 ) {
        logprintfl (EUCAERROR, "error: str2str() called with bad parameters\n");
        return buf;
    }

    char * b = strstr ( str, begin );
    if ( b==NULL ) {
        logprintfl (EUCAERROR, "error: str2str() beginning string '%s' not found\n", begin);
        return buf;
    }

    char * e = strstr ( str, end );
    if ( e==NULL ) {
        logprintfl (EUCAERROR, "error: str2str() end string '%s' not found\n", end);
        return buf;
    }

    b += strlen (begin); // b now points at the supposed content
    int len = e-b;
    if ( len < 0 ) {
        logprintfl (EUCAERROR, "error: str2str() there is nothing between '%s' and '%s'\n", begin, end);
        return buf;
    }

    if ( len > BUFSIZE-1 ) {
        logprintfl (EUCAERROR, "error: str2str() string between '%s' and '%s' is too long\n", begin, end);
        return buf;
    }

    buf = malloc (len+1);
    if (buf!=NULL) {
      strncpy (buf, b, len);
      buf [len] = '\0';
    }

    return buf;
}

/* extract integer from str bound by 'begin' and 'end' */
long long str2longlong (const char * str, const char * begin, const char * end)
{
  long long val = -1L;
  char * buf = str2str (str, begin, end);
  if (buf!=NULL) {
    val = atoll (buf);
    free (buf);
  }

  return val;
}

int uint32compar(const void *ina, const void *inb) {
  uint32_t a, b;
  a = *(uint32_t *)ina;
  b = *(uint32_t *)inb;
  if (a < b) {
    return(-1);
  } else if (a > b) {
    return(1);
  }
  return(0);
}

int safekillfile(char *pidfile, char *procname, int sig, char *rootwrap) {
  int rc;
  char *pidstr;
  
  if (!pidfile || !procname || sig < 0 || check_file(pidfile)) {
    return(1);
  }

  rc = 1;
  pidstr = file2str(pidfile);
  if (pidstr) {
    logprintfl(EUCADEBUG, "calling safekill with pid %d\n", atoi(pidstr));
    rc = safekill(atoi(pidstr), procname, sig, rootwrap);
    free(pidstr);
  }
  unlink(pidfile);
  return(rc);
}

int safekill(pid_t pid, char *procname, int sig, char *rootwrap) {
  char cmdstr[MAX_PATH], file[MAX_PATH], cmd[MAX_PATH];
  FILE *FH;
  int ret;

  if (pid < 2 || !procname) {
    return(1);
  }
  
  snprintf(file, MAX_PATH, "/proc/%d/cmdline", pid);
  if (check_file(file)) {
    return(1);
  }

  FH = fopen(file, "r");
  if (FH) {
    if (!fgets(cmdstr, MAX_PATH, FH)) {
      fclose(FH);
      return(1);
    }
    fclose(FH);
  } else {
    return(1);
  }
  
  ret=1;
  
  // found running process
  if (strstr(cmdstr, procname)) {
    // passed in cmd matches running cmd
    if (rootwrap) {
      snprintf(cmd, MAX_PATH, "%s kill -%d %d", rootwrap, sig, pid);
      ret = system(cmd)>>8;
    } else {
      ret = kill(pid, sig);
    }
  }
  
  return(ret);
}

int maxint(int a, int b) {
  return(a>b?a:b);
}

int minint(int a, int b) {
  return(a<b?a:b);
}

// copies contents of src to dst, possibly overwriting whatever is in dst
int copy_file (const char * src, const char * dst)
{
	struct stat mystat;

	if (stat (src, &mystat) < 0) {
		logprintfl (EUCAERROR, "error: cannot stat '%s'\n", src);
		return ERROR;
	}

	int ifp = open (src, O_RDONLY);
	if (ifp<0) {
		logprintfl (EUCAERROR, "failed to open the input file '%s'\n", src);
		return ERROR;
	}

	int ofp = open (dst, O_WRONLY | O_CREAT, 0600);
	if (ofp<0) {
		logprintfl (EUCAERROR, "failed to create the ouput file '%s'\n", dst);
		close (ifp);
		return ERROR;
	}

#   define _BUFSIZE 16384
	char buf [_BUFSIZE];
	ssize_t bytes;
	int ret = OK;

	while ((bytes = read (ifp, buf, _BUFSIZE)) > 0) {
		if (write (ofp, buf, bytes) < 1) {
			logprintfl (EUCAERROR, "failed while writing to '%s'\n", dst);
			ret = ERROR;
			break;
		}
	}
	if (bytes<0) {
		logprintfl (EUCAERROR, "failed while writing to '%s'\n", dst);
		ret = ERROR;
	}

	close (ifp);
	close (ofp);

	return ret;
}

long long file_size (const char * file_path)
{
    struct stat mystat;
    int err = stat (file_path, &mystat);
    if (err<0) return (long long)err;
    return (long long)(mystat.st_size);
}

// tolower for strings (result must be freed by the caller)

char * strduplc (const char * s)
{
	char * lc = strdup (s);
	for (int i=0; i<strlen(s); i++) {
		lc [i] = tolower(lc [i]);
	}
	return lc;
}

// some XML parsing routines

// given a '\0'-terminated string in 'xml', finds the next complete <tag...>
// and returns its name in a newly allocated string (which must be freed by
// the caller) or returns NULL if no tag can be found or if the XML is not 
// well formed; when not NULL, the following parameters are also returned:
// 
//     start   = index of the '<' character
//     end     = index of the '>' character
//     single  = 1 if this is a <..../> tag
//     closing = 1 if this is a </....> tag

static char * next_tag (const char * xml, int * start, int * end, int * single, int * closing)
{
    char * ret = NULL;

    int name_start = -1;
    int name_end = -1;
    int tag_start = -1;

    for (const char *p = xml; *p; p++) {
        
        if ( *p == '<' ) { // found a new tag
            tag_start = (p - xml); // record the char so its offset can be returned

            * closing = 0;
            if (*(p+1) == '/' || *(p+1) == '?') {
                if (*(p+1) == '/') // if followed by '/' then it is a "closing" tag
                    * closing = 1;
                name_start = (p - xml + 2); 
                p++;
            } else {
                name_start = (p - xml + 1);
            }
            continue;
        }

        if ( *p == ' ' && name_start != -1 && name_end == -1 ) { // a name may be terminated by a space
            name_end = (p - 1 - xml);
            continue;
        }

        if ( *p == '>' ) {
            if (name_start == -1) // never saw '<', error
                break;
            if (p < xml + 2) // tag is too short, error
                break;
            const char * last_ch = p-1;
            if ( * last_ch == '/' || * last_ch == '?' ) {
                * single = 1; // preceded by '/' then it is a "single" tag
                last_ch--;
            } else {
                * single = 0;
            }

            if (name_start != -1 && name_end == -1) { // a name may be terminated by '/' or '>' or '?'
                name_end = (last_ch - xml);
            }

            if ((name_end-name_start)>=0) { // we have a name rather than '<>'
                ret = calloc (name_end-name_start+2, sizeof (char));
                if (ret==NULL) break;
                strncpy (ret, xml + name_start, name_end - name_start + 1);
                * start = tag_start;
                * end = p - xml;
            }
            break;
        }
    }

    return ret;
}

// given a '\0'-terminated string in 'xml' and an 'xpath' of an XML
// element, returns the "content" of that element in a newly allocated
// string, which must be freed by the caller; for example, with XML:
// 
//   "<a><b>foo</b><c><d>bar</d><e>baz</e></c></a>"
// 
// the content returned for xpath "a/c/e" is "baz"

static char * find_cont (const char * xml, char * xpath) 
{
    char * ret = NULL;

    int tag_start;
    int tag_end = -1;
    int single;
    int closing;
    char * name;
    #define _STK_SIZE 64
    char       * n_stk [_STK_SIZE];
    const char * c_stk [_STK_SIZE];
    int stk_p = -1;
    
    // iterate over tags until the matching xpath is reached or
    // until no more tags are found in the 'xml'
    bzero(n_stk, sizeof(char *) * _STK_SIZE);

    for (int xml_offset=0; 
         (name = next_tag (xml + xml_offset, &tag_start, &tag_end, &single, &closing))!=NULL; 
         xml_offset += tag_end + 1)
    {
        if (single) {
            // not interested in singles because we are looking for content
            
        } else if (!closing) { // opening a tag
            // put name and pointer to content onto the stack
            stk_p++;   
            if (stk_p==_STK_SIZE) // exceeding stack size, error
                goto cleanup;
            n_stk [stk_p] = strduplc (name); // put a lower-case-only copy onto stack
            c_stk [stk_p] = xml + xml_offset + tag_end + 1;
            
        } else { // closing tag
            // get the name in all lower-case, for consistency with xpath
            char * name_lc = strduplc (name);
            free (name);
            name = name_lc;

            // name doesn't match last seen opening tag, error
	    if (stk_p >= 0) {
	      if (!n_stk[stk_p] || (strcmp(n_stk[stk_p], name)!=0)) {
                goto cleanup;
	      }
	    }


            // construct the xpath of the closing tag based on stack contents
            char xpath_cur [MAX_PATH] = "";
            for (int i=0; i<=stk_p; i++) {
                if (i>0) strncat (xpath_cur, "/", MAX_PATH);
                strncat (xpath_cur, n_stk[i], MAX_PATH);
            }
              
            // pop the stack whether we have a match or not
            if (stk_p<0) // past the bottom of the stack, error
                goto cleanup; 
            const char * contp = c_stk [stk_p];
            int cont_len = xml + xml_offset + tag_start - contp;
            free (n_stk [stk_p]);
            stk_p--;

            // see if current xpath matches the requested one
            if (strcmp (xpath, xpath_cur)==0) {
                char * cont = calloc (cont_len + 1, sizeof (char));
                if (cont==NULL)
                    goto cleanup;
                strncpy (cont, contp, cont_len);
                ret = cont;
                break;
            }
        }
        free (name);
        name = NULL;
    }

cleanup:
    if (name) free (name); // for exceptions
    for (int i=0; i<=stk_p; i++)
        free (n_stk [i]); // free everything on the stack
    return ret;
}

// given a '\0'-terminated string in 'xml' and an 'xpath' of an XML
// element, returns the "content" of that element in a newly allocated
// string, which must be freed by the caller; for example, with XML:
// 
//   "<a><b>foo</b><c><d>bar</d><e>baz</e></c></a>"
// 
// the content returned for xpath "a/c/e" is "baz"

char * xpath_content (const char * xml, const char * xpath)
{
    char * ret=NULL;

    if (xml==NULL || xpath==NULL) return NULL;
    char * xpath_l = strduplc (xpath); // lower-case copy of requested xpath
    if (xpath_l != NULL) {
        ret = find_cont (xml, xpath_l);
        free (xpath_l);
    }

    return ret;
}

int construct_uri(char *uri, char *uriType, char *host, int port, char *path) {
  
  if (!uri || !uriType || !host || !strlen(uriType) || !strlen(host)) {
    return(1);
  }
  
  uri[0] = '\0';
  strncat(uri, uriType, strlen(uriType));
  strncat(uri, "://", 3);
  
  strncat(uri, host, strlen(host));
  
  if (port > 0) {
    char tmp[32];
    snprintf(tmp, 32, ":%d", port);
    strncat(uri, tmp, strlen(tmp));
  }
  strncat(uri, "/", 1);

  if (path && strlen(path)) {
    strncat(uri, path, strlen(path));
  }
  
  return(0);
}

int tokenize_uri(char *uri, char *uriType, char *host, int *port, char *path) {
  char *tok, *start;

  uriType[0] = host[0] = path[0] = '\0';
  *port = 0;

  start = uri;

  // must have a type
  tok = strsep(&start, "://");
  if (!start) {
    return(1);
  }
  snprintf(uriType, strlen(tok)+1, "%s", tok);
  start += 2;
  //  printf("HERE: %s %s\n", start, tok);

  tok = strsep(&start, ":");
  if (!start) {
    // no port
    start = tok;
    tok = strsep(&start, "/");
    if (!start) {
      // no path
      if (tok) {
	snprintf(host, strlen(tok)+1, "%s", tok);
      } else {
	// no host
	// must have a host
	return(1);
      }
    } else {
      // path present
      snprintf(host, strlen(tok)+1, "%s", tok);
      snprintf(path, strlen(start)+1, "%s", start);
    }
  } else {
    // port present
    snprintf(host, strlen(tok)+1, "%s", tok);
    tok = strsep(&start, "/");
    if (!start) {
      // no path present
      if (tok) {
	*port = atoi(tok);
      }
    } else {
      // path present
      *port = atoi(tok);
      snprintf(path, strlen(start)+1, "%s", start);
    }
  }
  return(0);
}

// returns a new string in which 'new' is appended to 'original'
// and frees 'original'
char * strdupcat (char * original, char * new)
{
    int len = 0;
    int olen = 0;

    if (original) {
        olen = strlen (original);
        len += olen;
    }
        
    if (new) {
        len += strlen (new);
    }
        
    char * ret = calloc (len + 1, sizeof (char));
    if ( ret ) {
        if (original) {
            strncat (ret, original, len);
            free (original);
        }

        if (new) {
            strncat (ret, new, len-olen);
        }
    }
        
    return ret;
}

// given path=A/B/C and only A existing, create A/B and, unless
// is_file_path==1, also create A/B/C directory
// returns: 0 = path already existed, 1 = created OK, -1 = error
int ensure_directories_exist (const char * path, int is_file_path, const char * user, const char * group, mode_t mode)
{
    int len = strlen (path);
    char * path_copy = NULL;
    int ret = 0;
    int i;

    if (len>0)
        path_copy = strdup (path);

    if (path_copy==NULL)
        return -1;

    for (i=0; i<len; i++) {
        struct stat buf;
        int try_dir = 0;

        if (path[i]=='/' && i>0) { // dir path, not root
            path_copy[i] = '\0';
            try_dir = 1;

        } else if (path[i]!='/' && i+1==len) { // last one
            if (!is_file_path)
                try_dir = 1;
        }

        if ( try_dir ) {
            if ( stat (path_copy, &buf) == -1 ) {
                logprintfl (EUCAINFO, "{%u} creating path %s\n", (unsigned int)pthread_self(), path_copy);

                if ( mkdir (path_copy, mode) == -1) {
                    logprintfl (EUCAERROR, "error: failed to create path %s: %s\n", path_copy, strerror (errno));

                    free (path_copy);
                    return -1;
                }
                ret = 1; // we created a directory

                if(diskutil_ch(path_copy, user, group, mode) != OK) {
                    logprintfl (EUCAERROR, "error: failed to change perms on path %s\n", path_copy);
                    free (path_copy);
                    return -1;
                }
                //                chmod (path_copy, mode); // ensure perms are right despite mask
            }
            path_copy[i] = '/'; // restore the slash
        }
    }

    free (path_copy);
    return ret;
}

// time since 1970 in microseconds
long long time_usec (void)
{
    struct timeval tv;
    gettimeofday (&tv, NULL);
    return (long long)tv.tv_sec * 1000000 + tv.tv_usec;
}

// ensure the temp file is only readable by the user
char *safe_mkdtemp (char *template)
{
    mode_t u;
    char *ret=NULL;
    u=umask(0077);
    ret = mkdtemp(template);
    umask(u);
    return(ret);
}

// ensure the temp file is only readable by the user
int safe_mkstemp (char *template)
{
    mode_t u;
    int ret;
    u=umask(0077);
    ret = mkstemp(template);
    umask(u);
    return(ret);
}

// ensure the string is aways 0-terminated
char* safe_strncpy (char *s1, const char *s2, size_t len) 
{
    char* ret = strncpy(s1, s2, len);
    s1[len-1] = '\0';
    return ret;
}

// try to get UUID of the block device, returning 0 on success and 1 otherwise (TODO: use blkidlib maybe)
int get_blkid (const char * dev_path, char * uuid, unsigned int uuid_size) 
{
    char cmd [1024];
    snprintf (cmd, sizeof (cmd), "blkid %s", dev_path); // option '-u filesystem' did not exist on Centos
    char * blkid_output = system_output (cmd);
    if (blkid_output==NULL) 
        return 1;
    int ret = 1;
    char * first_char = strstr (blkid_output, "UUID=\"");
    if (first_char) {
        first_char += 6;
        char * last_char = strchr (first_char, '"');
        if (last_char && ((last_char-first_char)>0)) {
            * last_char = '\0';
            safe_strncpy (uuid, first_char, uuid_size);
            assert (0 == strcmp (uuid, first_char));
            ret = 0;
        }
    }
    free (blkid_output);

    return ret;
}

// turn a string into a boolean (returned as a char)
char parse_boolean (const char * s)
{
    char * lc = strduplc (s);
    char val = 0;

    if (strcmp (lc, "y")==0 ||
        strcmp (lc, "yes")==0 ||
        strcmp (lc, "t")==0 ||
        strcmp (lc, "true")==0) { val = 1; }
    else if (strcmp (lc, "n")==0 ||
             strcmp (lc, "no")==0 ||
             strcmp (lc, "f")==0 ||
             strcmp (lc, "false")==0) { val = 0; }
    else
        logprintfl (EUCAERROR, "error: failed to parse value '%s' as boolean", lc);
    free (lc);

    return val;
}

/////////////////////////////////////////////// unit testing code ///////////////////////////////////////////////////

#ifdef _UNIT_TEST

#define _STR "a lovely string"

int main (int argc, char ** argv)
{
    int errors = 0;
    char cwd [1024];
    char *s;
    getcwd (cwd, sizeof (cwd));
    srandom (time(NULL));

    printf("testing system_output() in misc.c\n");
    s = system_output("echo Hello");
    assert(s);
    assert (strlen (s) != 0);
    printf("echo Hello == |%s|\n", s);
    free (s);

    s = system_output("echo -n");
    assert(!s);

    printf ("testing fp2str in misc.c\n");
    FILE * fp = tmpfile ();
    assert (fp);
    s = fp2str (fp);
    assert (s);
    assert (strlen (s) == 0);
    free (s);
    rewind (fp);
    fputs (_STR, fp);
    rewind (fp);
    s = fp2str (fp);
    assert (s);
    assert (strlen (s) == strlen (_STR));
    free (s);
    fclose (fp);

    printf ("testing get_blkid in misc.c\n");
    char * devs [] = { "hda", "hdb", "hdc", "hdd", "sda", "sdb", "sdc", "sdd", NULL };
    for (char ** d = devs; * d != NULL; d++) {
        for (int p=1; p<4; p++) {
            char dev_path [32];
            char uuid [64] = "";
            snprintf (dev_path, sizeof (dev_path), "/dev/%s%d", *d, p);
            int ret = get_blkid (dev_path, uuid, sizeof (uuid));
            printf ("\t%s: %s\n", dev_path, ret==0?uuid:"UUID not found");
        }
    }

    return 0;
}

#endif // _UNIT_TEST

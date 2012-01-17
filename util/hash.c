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
#include <openssl/md5.h>
#include <sys/mman.h> // mmap
#include <pthread.h>

#include "misc.h"
#include "hash.h"
#include "euca_auth.h" // base64_enc
#include "vnetwork.h" // OK / ERROR

int hash_b64enc_string(const char *in, char **out) {
  unsigned char *md5ret=NULL;
  unsigned char hash[17];

  if (!in || !out) {
    return(1);
  }
  *out = NULL;
  logprintfl(EUCADEBUG, "hash_b64enc_string(): in=%s\n", in);
  bzero(hash, 17);
  md5ret = MD5((const unsigned char *)in, strlen(in), hash);
  if (md5ret) {
    *out = base64_enc(hash, 16);
    if (*out == NULL) {
      return(1);
    }
  }

  return(0);
}

// calculates an md5 hash of 'str' and places it into 'buf' in hex
int str2md5str (char * buf, unsigned int buf_size, const char * str)
{
        if (buf_size < (MD5_DIGEST_LENGTH * 2 + 1)) 
                return ERROR;

        unsigned char md5digest [MD5_DIGEST_LENGTH];
        if (MD5 ((const unsigned char *)str, strlen (str), md5digest)==NULL)
                return ERROR;
        
        char * p = buf;
        for (int i=0; i<MD5_DIGEST_LENGTH; i++) {
                sprintf (p, "%02x", md5digest [i]);
                p += 2;
        }

        return OK;
}

// returns a new string with a hex value of an MD5 hash of a file (same as `md5sum`)
// or NULL if there was an error; the string must be freed by the caller
char * file2md5str (const char * path)
{
    char * md5string = NULL;

    int fd = open (path, O_RDONLY);
    if (fd<0) return NULL;

    struct stat mystat;
    if (fstat(fd, &mystat) < 0) goto cleanup;

    char * buf = mmap(0, mystat.st_size, PROT_READ, MAP_SHARED, fd, 0);
    if (buf==MAP_FAILED) goto cleanup;

    unsigned char md5digest [MD5_DIGEST_LENGTH];
    if (MD5((unsigned char*) buf, mystat.st_size, md5digest)==NULL) goto cleanup;

    md5string = calloc (MD5_DIGEST_LENGTH * 2 + 1, sizeof (char));
    if (md5string==NULL) goto cleanup;

    char * p = md5string;
    for (int i=0; i<MD5_DIGEST_LENGTH; i++) {
        sprintf (p, "%02x", md5digest [i]);
        p += 2;
    }

 cleanup:

    close (fd);
    return md5string;
}

// Jenkins hash function (from http://en.wikipedia.org/wiki/Jenkins_hash_function)
uint32_t jenkins (const char * key, size_t len)
{
        uint32_t hash, i;
        for (hash = i = 0; i < len; ++i) {
                hash += key[i];
                hash += (hash << 10);
                hash ^= (hash >> 6);
        }
        hash += (hash << 3);
        hash ^= (hash >> 11);
        hash += (hash << 15);
        
        return hash;
}

// calculates a Jenkins hash of 'str' and places it into 'buf' in hex
int hexjenkins (char * buf, unsigned int buf_size, const char * str)
{
        snprintf (buf, buf_size, "%08x", jenkins (str, strlen (str)));
        return OK;
}

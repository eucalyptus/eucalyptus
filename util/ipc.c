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
#include <sys/types.h>
#include <semaphore.h>
#define _FILE_OFFSET_BITS 64
#include <sys/stat.h>
#include <sys/ipc.h>
#include <sys/sem.h>
#include <fcntl.h> /* For O_* */
#include <string.h>
#include <strings.h>

#include "misc.h" /* logprintfl */
#include "ipc.h"

/* these are wrappers support BOTH SYS V semaphores and the POSIX named
   semaphores depending on whether name was passed to sem_alloc() */

#define DECLARE_ARG union semun { int val; struct semid_ds *buf; ushort *array; } arg

sem * sem_alloc (const int val, const char * name)
{
    return sem_realloc (val, name, O_EXCL);
}

sem * sem_realloc (const int val, const char * name, int flags) 
{
    DECLARE_ARG;

    sem * s = malloc (sizeof (sem));
    if (s==NULL) return NULL;
    bzero (s, sizeof (sem));
    s->sysv = -1;
    s->flags = flags;
    
    if (name && !strcmp(name, "mutex")) { /* use pthread mutex */
      s->usemutex = 1;
      s->mutcount = val;
      s->mutwaiters = 0;
      pthread_mutex_init(&(s->mutex), NULL);
      pthread_cond_init(&(s->cond), NULL);
    } else if (name) { /* named semaphores */
        if (s->flags & O_EXCL) {
            if ( sem_unlink (name) == 0) { /* clean up in case previous sem holder crashed */
                logprintfl (EUCAINFO, "sem_alloc(): cleaning up old semaphore %s\n", name);
            }
        }
        if ((s->posix = sem_open (name, O_CREAT | flags, 0644, val))==SEM_FAILED) {
            free (s);
            return NULL;
        }
        s->name = strdup (name);

    } else { /* SYS V IPC semaphores */
        s->sysv = semget (IPC_PRIVATE, /* private to process & children */
                        1, /* only need one */
                        IPC_CREAT | IPC_EXCL | S_IRUSR | S_IWUSR /* user-only */);
        if (s->sysv<0) {
            free (s);
            return NULL;
        }
        
        /* set the value */
        arg.val = val;
        if (semctl(s->sysv, 0, SETVAL, arg) == -1) {
            free (s);
            return NULL;
        }
    }
    
    return s;
}

int sem_p (sem * s)
{
    int rc;
    if (s && s->usemutex) {
        rc = pthread_mutex_lock(&(s->mutex));
	s->mutwaiters++;
	while(s->mutcount == 0) {
	  pthread_cond_wait(&(s->cond), &(s->mutex));
	}
	s->mutwaiters--;
	s->mutcount--;
	rc = pthread_mutex_unlock(&(s->mutex));
	return(rc);
    }

    if (s && s->posix) {
        return sem_wait (s->posix);
    }

    if (s && s->sysv > 0) {
        struct sembuf sb = {0, -1, 0};
        return semop (s->sysv, &sb, 1);
    }

    return -1;
}

int sem_v (sem * s)
{
    int rc;
    if (s && s->usemutex) {
        rc = pthread_mutex_lock(&(s->mutex));
        if (s->mutwaiters > 0) {
	  rc = pthread_cond_signal(&(s->cond));
	}
	s->mutcount++;
        rc = pthread_mutex_unlock(&(s->mutex));
	return(rc);
    }

    if (s && s->posix) {
        return sem_post (s->posix);
    }

    if (s && s->sysv > 0) {
        struct sembuf sb = {0, 1, 0};
        return semop (s->sysv, &sb, 1);
    }
    
    return -1;
}

void sem_free (sem * s)
{
    DECLARE_ARG;
    
    if (s && s->posix) {
        sem_close (s->posix);
        if (s->flags & O_EXCL) {
            sem_unlink (s->name);            
        }
        free (s->name);
    }
    
    if (s && s->sysv > 0) {
        semctl (s->sysv, 0, IPC_RMID, arg); /* TODO: check return */
    }
    
    free (s);
}

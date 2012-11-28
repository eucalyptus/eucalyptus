// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//!
//! @file util/ipc.c
//! Provides wrappers that support BOTH SYS V semaphores and the POSIX named
//! semaphores depending on whether name was passed to sem_alloc().
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64    // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <semaphore.h>
#include <sys/stat.h>
#include <sys/ipc.h>
#include <sys/sem.h>
#include <fcntl.h>              /* For O_* */
#include <string.h>
#include <strings.h>
#include <assert.h>

#include "misc.h"               /* logprintfl */
#include "ipc.h"
#include "eucalyptus.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

sem *sem_alloc(const int val, const char *name);
sem *sem_realloc(const int val, const char *name, int flags);
sem *sem_alloc_posix(sem_t * external_lock);
int sem_prolaag(sem * s, boolean do_log);
int sem_p(sem * s);
int sem_verhogen(sem * s, boolean do_log);
int sem_v(sem * s);
void sem_free(sem * s);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define DECLARE_ARG               union semun { int val; struct semid_ds *buf; ushort *array; } arg

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Allocate a new semaphore with the given name and mutex starting value.
//!
//! @param[in] val  the starting mutex count
//! @param[in] name the type of semaphore ('mutex' = pthread mutex; 'semaphore' = semaphore) if any other
//!                 name, then SYS V IPC semaphore is implied.
//!
//! @return a pointer to the newly allocated semaphore or NULL if a failure occured
//!
//! @see sem_realloc()
//! @see sem_free()
//!
//! @note Caller is responsible to free allocated memory for this semaphore. Call to sem_free() is
//!       prefered for this operation
//!
sem *sem_alloc(const int val, const char *name)
{
    return (sem_realloc(val, name, O_EXCL));
}

//!
//! Allocate a new semaphore with the given name and mutex starting value.
//!
//! @param[in] val   the starting mutex count
//! @param[in] name  the type of semaphore ('mutex' = pthread mutex; 'semaphore' = semaphore) if any other
//!                  name, then SYS V IPC semaphore is implied.
//! @param[in] flags Kernel encoding of open mode
//!
//! @return a pointer to the newly allocated semaphore or NULL if a failure occured
//!
//! @see sem_free()
//!
//! @pre The name field must not be NULL.
//!
//! @post On success, a semaphore structure is allocated and initialized
//!
//! @note Caller is responsible to free allocated memory for this semaphore. Call to sem_free() is
//!       prefered for this operation
//!
sem *sem_realloc(const int val, const char *name, int flags)
{
    sem *s = NULL;
    DECLARE_ARG;

    assert(name);
    if ((s = EUCA_ZALLOC(1, sizeof(sem))) == NULL)
        return (NULL);

    s->sysv = -1;
    s->flags = flags;

    if (name && !strcmp(name, "mutex")) {
        /* use pthread mutex */
        s->usemutex = 1;
        s->mutcount = val;
        s->mutwaiters = 0;
        pthread_mutex_init(&(s->mutex), NULL);
        pthread_cond_init(&(s->cond), NULL);
    } else if (name) {
        /* named semaphores */
        if (s->flags & O_EXCL) {
            if (sem_unlink(name) == 0) {
                /* clean up in case previous sem holder crashed */
                logprintfl(EUCAINFO, "cleaning up old semaphore %s\n", name);
            }
        }

        if ((s->posix = sem_open(name, O_CREAT | flags, 0644, val)) == SEM_FAILED) {
            EUCA_FREE(s);
            return (NULL);
        }

        s->name = strdup(name);
    } else {
        /* SYS V IPC semaphores */
        s->sysv = semget(IPC_PRIVATE,   /* private to process & children */
                         1,     /* only need one */
                         (IPC_CREAT | IPC_EXCL | S_IRUSR | S_IWUSR) /* user-only */ );
        if (s->sysv < 0) {
            EUCA_FREE(s);
            return (NULL);
        }

        /* set the value */
        arg.val = val;
        if (semctl(s->sysv, 0, SETVAL, arg) == -1) {
            EUCA_FREE(s);
            return (NULL);
        }
    }

    return (s);
}

//!
//! Allocates sem structure from an already allocated Posix named semaphore; sem_p/v can
//! be used with it; freeing the sem struct also closes the Posix semaphore that was passed
//! in.
//!
//! @param[in] external_lock a pointer to the posix semaphore structure to use.
//!
//! @return a pointer to the newly created semaphore or NULL if any failure occured.
//!
//! @pre The external_lock parameter must not be NULL
//!
//! @post On success a new sem structure is allocated and initialized with the external_lock.
//!
sem *sem_alloc_posix(sem_t * external_lock)
{
    sem *s = NULL;

    if (external_lock != NULL) {
        if ((s = EUCA_ZALLOC(1, sizeof(sem))) == NULL)
            return (NULL);
        s->posix = external_lock;
        s->name = strdup("unknown");
        return (s);
    }

    return (NULL);
}

//!
//! Semaphore increment (aka lock acquisition) function. The second parameter
//! tells it not to log the event (useful for avoiding infinite recursion when
//! locking from the logging code).
//!
//! @param[in] s      a pointer to the semaphore to acquire
//! @param[in] do_log set to TRUE if we need to log this acquisition. Otherwise set to FALSE.
//!
//! @return 0 on success or any other values on failure
//!
int sem_prolaag(sem * s, boolean do_log)
{
    int rc = 0;
    char addr[24] = { 0 };
    struct sembuf sb = { 0, -1, 0 };

    if (s) {
        if (do_log) {
            snprintf(addr, sizeof(addr), "%lx", ((unsigned long) s));
            logprintfl(EUCAEXTREME, "%s locking\n", ((s->name) ? (s->name) : (addr)));
        }

        if (s->usemutex) {
            rc = pthread_mutex_lock(&(s->mutex));
            s->mutwaiters++;
            while (s->mutcount == 0) {
                pthread_cond_wait(&(s->cond), &(s->mutex));
            }

            s->mutwaiters--;
            s->mutcount--;
            rc = pthread_mutex_unlock(&(s->mutex));
            return (rc);
        }

        if (s->posix) {
            return (sem_wait(s->posix));
        }

        if (s->sysv > 0) {
            return (semop(s->sysv, &sb, 1));
        }
    }

    return (-1);
}

//
//!
//! The semaphore increment (aka lock acquisition) function used throughout.
//!
//! @param[in] s a pointer to the semaphore to acquire
//!
//! @return the result from sem_prolaag()
//!
//! @see sem_prolaag()
//!
int sem_p(sem * s)
{
    return (sem_prolaag(s, TRUE));
}

//!
//! Semaphore decrement (aka lock release) function. The second parameter tells it not
//! to log the event (useful for avoiding infinite recursion when unlocking from the
//! logging code)
//!
//! @param[in] s      pointer to the semaphore to release
//! @param[in] do_log set to TRUE if we need to log this acquisition. Otherwise set to FALSE.
//!
//! @return 0 on success or any other values on failure
//!
int sem_verhogen(sem * s, boolean do_log)
{
    int rc = 0;
    char addr[24] = { 0 };
    struct sembuf sb = { 0, 1, 0 };

    if (s) {
        if (do_log) {
            snprintf(addr, sizeof(addr), "%lx", ((unsigned long) s));
            logprintfl(EUCAEXTREME, "%s unlocking\n", ((s->name) ? (s->name) : (addr)));
        }

        if (s->usemutex) {
            rc = pthread_mutex_lock(&(s->mutex));
            if (s->mutwaiters > 0) {
                rc = pthread_cond_signal(&(s->cond));
            }

            s->mutcount++;
            rc = pthread_mutex_unlock(&(s->mutex));
            return (rc);
        }

        if (s->posix) {
            return (sem_post(s->posix));
        }

        if (s->sysv > 0) {
            return (semop(s->sysv, &sb, 1));
        }
    }

    return (-1);
}

//!
//! The semaphore decrement (aka lock release) function used throughout.
//!
//! @param[in] s a pointer to the semaphore to release
//!
//! @return the result from sem_verhogen()
//!
//! @see sem_verhogen()
//!
int sem_v(sem * s)
{
    return (sem_verhogen(s, TRUE));
}

//!
//! Frees a semaphore
//!
//! @param[in] s a pointer to the semaphore to free if not NULL
//!
void sem_free(sem * s)
{
    DECLARE_ARG;

    if (s) {
        if (s->posix) {
            sem_close(s->posix);
            if (s->flags & O_EXCL) {
                sem_unlink(s->name);
            }
            EUCA_FREE(s->name);
        }

        if (s->sysv > 0) {
            /* @todo check return */
            semctl(s->sysv, 0, IPC_RMID, arg);
        }

        EUCA_FREE(s);
    }
}

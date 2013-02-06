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

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <semaphore.h>
#include <sys/stat.h>
#include <sys/ipc.h>
#include <sys/sem.h>
#include <fcntl.h>                     /* For O_* */
#include <string.h>
#include <strings.h>
#include <assert.h>

#include "eucalyptus.h"
#include "misc.h"                      /* logprintfl */
#include "ipc.h"

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
 |                              GLOBAL VARIABLES                              |
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
sem *sem_realloc(const int val, const char *name, u32 flags);
sem *sem_alloc_posix(sem_t * pExternalLock);
void sem_free(sem * pSem);

int sem_prolaag(sem * pSem, boolean doLog);
int sem_p(sem * pSem);

int sem_verhogen(sem * pSem, boolean doLog);
int sem_v(sem * pSem);

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
//! @pre The name field must not be NULL.
//!
//! @post On success, a new semaphore will be allocated and initialized. If name happened to be NULL, a
//!       SIGABRT signal will be thrown by the application.
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
//! @param[in] name  the type of semaphore. If 'mutex' then pthread mutexis used if any other
//!                  name then posix named semaphore is used if an empty name then SYS V IPC
//!                  semaphore is implied.
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
sem *sem_realloc(const int val, const char *name, u32 flags)
{
    sem *pSem = NULL;
    char addr[24] = "";
    DECLARE_ARG;

    // Validate the name parameter
    assert(name);

    // Check if we can allocate memory for our zemaphore
    if ((pSem = EUCA_ZALLOC(1, sizeof(sem))) == NULL)
        return (NULL);

    // Initialize our type independent fields
    pSem->sysv = -1;
    pSem->flags = flags;

    // save the address of this semaphore for future use
    snprintf(addr, sizeof(addr), "%p", pSem);

    //
    // Initialize our semphore base on the requested type
    //
    if (!strcmp(name, "mutex")) {
        // use pthread mutex
        pSem->usemutex = 1;
        pSem->mutcount = val;
        pSem->mutwaiters = 0;
        pthread_mutex_init(&(pSem->mutex), NULL);
        pthread_cond_init(&(pSem->cond), NULL);

        // In this case we'll use the address of the semaphore rather than the name
        pSem->name = strdup(addr);
    } else if (strlen(name) > 0) {
        // named semaphores
        if (pSem->flags & O_EXCL) {
            // clean up in case previous sem holder crashed
            if (sem_unlink(name) == 0) {
                LOGINFO("cleaning up old semaphore %s\n", name);
            }
        }
        // Create a new semaphore with this name.
        if ((pSem->posix = sem_open(name, O_CREAT | flags, 0644, val)) == SEM_FAILED) {
            EUCA_FREE(pSem);
            return (NULL);
        }

        pSem->name = strdup(name);
    } else {
        // SYS V IPC semaphores
        if ((pSem->sysv = semget(IPC_PRIVATE, 1, (IPC_CREAT | IPC_EXCL | S_IRUSR | S_IWUSR))) < 0) {
            EUCA_FREE(pSem);
            return (NULL);
        }
        // Set the value
        arg.val = val;
        if (semctl(pSem->sysv, 0, SETVAL, arg) == -1) {
            EUCA_FREE(pSem);
            return (NULL);
        }
        // In this case we'll use the address of the semaphore rather than the name
        pSem->name = strdup(addr);
    }

    return (pSem);
}

//!
//! Allocates sem structure from an already allocated Posix named semaphore; sem_p/v can
//! be used with it; freeing the sem struct also closes the Posix semaphore that was passed
//! in.
//!
//! @param[in] pExternalLock a pointer to the posix semaphore structure to use.
//!
//! @return a pointer to the newly created semaphore or NULL if any failure occured.
//!
//! @pre The external_lock parameter must not be NULL
//!
//! @post On success a new sem structure is allocated and initialized with the external_lock.
//!
sem *sem_alloc_posix(sem_t * pExternalLock)
{
    sem *pSem = NULL;

    // Name sure or parameter is valid
    if (pExternalLock != NULL) {
        // Allocate memory for our duplicate semaphore
        if ((pSem = EUCA_ZALLOC(1, sizeof(sem))) == NULL)
            return (NULL);

        // Set the dependency
        pSem->posix = pExternalLock;
        pSem->name = strdup("unknown");
        return (pSem);
    }

    return (NULL);
}

//!
//! Frees a semaphore.
//!
//! @param[in] pSem a pointer to the semaphore to free if not NULL
//!
//! @see SEM_FREE()
//!
//! @pre The pSem field should not be NULL
//!
//! @post If pSem isn't NULL, the semaphore will be uninitialized and the memory will be freed
//!
//! @note It is recommended to use the SEM_FREE() macro as this will ensured that pSem will be
//!       explicitedly set to NULL uppon returning from this call.
//!
void sem_free(sem * pSem)
{
    DECLARE_ARG;

    // Make sure we were provided with a valid semaphore
    if (pSem) {
        // Is this a posix semaphore?
        if (pSem->posix) {
            // Close the posix semaphore
            sem_close(pSem->posix);

            // If we had the exclusive flag set, then unlink
            if (pSem->flags & O_EXCL) {
                sem_unlink(pSem->name);
            }

        }
        // If this is a SYS V semaphore
        if (pSem->sysv > 0) {
            semctl(pSem->sysv, 0, IPC_RMID, arg);
        }
        // If we use mutex, we need to destroy it.
        if (pSem->usemutex) {
            pthread_mutex_destroy(&(pSem->mutex));
        }
        // Free the memory for the name before freeing the semaphore structure memory
        EUCA_FREE(pSem->name);
        EUCA_FREE(pSem);
    }
}

//!
//! Semaphore increment (aka lock acquisition) function. The second parameter
//! tells it not to log the event (useful for avoiding infinite recursion when
//! locking from the logging code).
//!
//! @param[in] pSem  a pointer to the semaphore to acquire
//! @param[in] doLog set to TRUE if we need to log this acquisition. Otherwise set to FALSE.
//!
//! @return 0 on success or any other values on failure
//!
//! @pre The pSem field must not be NULL.
//!
//! @post On success, the sempahore is acquired
//!
int sem_prolaag(sem * pSem, boolean doLog)
{
    int rc = 0;
    struct sembuf sb = { 0, -1, 0 };

    // Make sure our given semaphore is valid
    if (pSem) {
        // Check if we need to log this operation now
        if (doLog) {
            LOGEXTREME("%s locking\n", pSem->name);
        }
        // For mutex semaphore
        if (pSem->usemutex) {
            rc = pthread_mutex_lock(&(pSem->mutex));
            pSem->mutwaiters++;
            while (pSem->mutcount == 0) {
                pthread_cond_wait(&(pSem->cond), &(pSem->mutex));
            }

            pSem->mutwaiters--;
            pSem->mutcount--;
            rc = pthread_mutex_unlock(&(pSem->mutex));
            return (rc);
        }
        // For Posix semaphore
        if (pSem->posix) {
            return (sem_wait(pSem->posix));
        }
        // For SYS V semaphore
        if (pSem->sysv > 0) {
            return (semop(pSem->sysv, &sb, 1));
        }
    }

    return (-1);
}

//!
//! The semaphore increment (aka lock acquisition) function used throughout.
//!
//! @param[in] pSem a pointer to the semaphore to acquire
//!
//! @return the result from sem_prolaag()
//!
//! @see sem_prolaag()
//!
int sem_p(sem * pSem)
{
    return (sem_prolaag(pSem, TRUE));
}

//!
//! Semaphore decrement (aka lock release) function. The second parameter tells it not
//! to log the event (useful for avoiding infinite recursion when unlocking from the
//! logging code)
//!
//! @param[in] pSem  pointer to the semaphore to release
//! @param[in] doLog set to TRUE if we need to log this acquisition. Otherwise set to FALSE.
//!
//! @return 0 on success or any other values on failure
//!
//! @pre The pSem field must not be NULL
//!
//! @post On success, the semaphore has successfully been released.
//!
int sem_verhogen(sem * pSem, boolean doLog)
{
    int rc = 0;
    struct sembuf sb = { 0, 1, 0 };

    // Make sure our given semaphore is valid
    if (pSem) {
        // Check if we need to log this operation now
        if (doLog) {
            LOGEXTREME("%s unlocking\n", pSem->name);
        }
        // For mutex semaphore
        if (pSem->usemutex) {
            rc = pthread_mutex_lock(&(pSem->mutex));
            if (pSem->mutwaiters > 0) {
                rc = pthread_cond_signal(&(pSem->cond));
            }

            pSem->mutcount++;
            rc = pthread_mutex_unlock(&(pSem->mutex));
            return (rc);
        }
        // For Posix semaphore
        if (pSem->posix) {
            return (sem_post(pSem->posix));
        }
        // For SYS V semaphore
        if (pSem->sysv > 0) {
            return (semop(pSem->sysv, &sb, 1));
        }
    }

    return (-1);
}

//!
//! The semaphore decrement (aka lock release) function used throughout.
//!
//! @param[in] pSem a pointer to the semaphore to release
//!
//! @return the result from sem_verhogen()
//!
//! @see sem_verhogen()
//!
int sem_v(sem * pSem)
{
    return (sem_verhogen(pSem, TRUE));
}

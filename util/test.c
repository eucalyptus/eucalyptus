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
//! @file util/test.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>                    /* usleep */
#include <string.h>
#include <pthread.h>

#include "eucalyptus.h"
#include "ipc.h"
#include "euca_string.h"
#include "misc.h"
#include "data.h"
#include <diskutil.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define ITER                                     (160 * 4)

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

const char *euca_this_component_name = "test";

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

 /*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef EUCA_DEPRECATED_API
static void test_volumes(void);
#endif /* EUCA_DEPRECATED_API */

 /*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define EXIT()                                       \
{                                                    \
	fprintf(stderr, "error on line %d\n", __LINE__); \
	exit(1);                                         \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//!
//!
void test_sem_fork(void)
{
    int i = 0;
    char c = '\0';
    sem *s = sem_alloc(1, "eucalyptus-util-test");

    printf("---> testing semaphores between processes\n");

    if (fork()) {
        c = 'A';
        sleep(1);
        printf("A trying sem...\n");
        sem_p(s);
        {
            printf("A got sem!\n");
            sleep(1);
            printf("A releasing sem...\n");
        }
        sem_v(s);
        sleep(1);

        for (i = 0; i < ITER; i++) {
            sem_p(s);
            {
                if (i % 16 == 0)
                    printf("\n");
                if (write(1, &c, 1) <= 0)
                    printf("Failed to write to stdout\n");
                usleep(100);
                if (write(1, &c, 1) <= 0)
                    printf("Failed to write to stdout\n");
            }
            sem_v(s);
        }
    } else {
        c = 'B';

        printf("B trying sem...\n");
        sem_p(s);
        {
            printf("B got sem!\n");
            sleep(2);
            printf("B releasing sem...\n");
        }
        sem_v(s);
        sleep(2);

        for (i = 0; i < ITER; i++) {
            sem_p(s);
            {
                if (write(1, &c, 1) <= 0)
                    printf("Failed to write to stdout\n");
                usleep(100);
                if (write(1, &c, 1) <= 0)
                    printf("Failed to write to stdout\n");
            }
            sem_v(s);
        }
        exit(0);                       // child quits
    }

    SEM_FREE(s);
    printf("\n");
}

//!
//!
//!
//! @param[in] arg tranparent pointer to thread arguments
//!
//! @return Always return NULL
//!
void *thread_a(void *arg)
{
    int i = 0;
    char c = 'T';
    sem *s = ((sem *) arg);

    sleep(1);
    printf("T trying sem...\n");
    sem_p(s);
    {
        printf("T got sem!\n");
        sleep(1);
        printf("T releasing sem...\n");
    }
    sem_v(s);
    sleep(1);

    for (i = 0; i < ITER; i++) {
        sem_p(s);
        {
            if (i % 16 == 0)
                printf("\n");
            if (write(1, &c, 1) <= 0)
                printf("Failed to write to stdout\n");
            usleep(100);
            if (write(1, &c, 1) <= 0)
                printf("Failed to write to stdout\n");
        }
        sem_v(s);
    }
    return (NULL);
}

//!
//!
//!
//! @param[in] arg tranparent pointer to thread arguments
//!
//! @return Always return NULL
//!
void *thread_b(void *arg)
{
    int i = 0;
    char c = 'U';
    sem *s = ((sem *) arg);

    printf("U trying sem...\n");
    sem_p(s);
    {
        printf("U got sem!\n");
        sleep(2);
        printf("U releasing sem...\n");
    }
    sem_v(s);
    sleep(2);

    for (i = 0; i < ITER; i++) {
        sem_p(s);
        {
            if (write(1, &c, 1) <= 0)
                printf("Failed to write to stdout\n");
            usleep(100);
            if (write(1, &c, 1) <= 0)
                printf("Failed to write to stdout\n");
        }
        sem_v(s);
    }
    return (NULL);
}

//!
//!
//!
void test_sem_pthreads(void)
{
    void *status = NULL;
    pthread_t ta = { 0 };
    pthread_t tb = { 0 };
    sem *s = sem_alloc(1, "eucalyptus-util-test2");

    printf("---> testing semaphores between threads\n");
    pthread_create(&ta, NULL, thread_a, s);
    pthread_create(&tb, NULL, thread_b, s);
    pthread_join(ta, &status);
    pthread_join(tb, &status);
    printf("\n");

    SEM_FREE(s);
}

#ifdef EUCA_DEPRECATED_API
//!
//!
//!
static void test_volumes(void)
{
    int i = 0;
    int j = 0;
    int pivot = 0;
    char id[100] = "";
    ncInstance inst = { {0} };
    ncVolume *vols[EUCA_MAX_VOLUMES + 1] = { NULL };
    ncVolume *v = NULL;
    ncVolume *v2 = NULL;

    for (j = 0; j < 10; j++) {
        pivot = random() % EUCA_MAX_VOLUMES;
        printf("testing volumes iteration=%d pivot=%d\n", j, pivot);
        bzero(&inst, sizeof(ncInstance));
        for (i = 0; i < EUCA_MAX_VOLUMES; i++) {
            snprintf(id, 100, "v%06d", i);
            vols[i] = add_volume(&inst, id, "remote", "local");
            if (vols[i] == NULL) {
                fprintf(stderr, "error on add iteration %i-%i\n", i, j);
                EXIT();
            }

            if (inst.volumesSize != i + 1) {
                fprintf(stderr, "error on add iteration %i-%i\n", i, j);
                EXIT();
            }
        }

        snprintf(id, 100, "v%06d", i);
        vols[i] = add_volume(&inst, id, "remote", "local");
        if (vols[i] != NULL)
            EXIT();

        if (inst.volumesSize != EUCA_MAX_VOLUMES)
            EXIT();

        v = vols[pivot];
        strncpy(id, v->volumeId, 100);
        v2 = free_volume(&inst, id, "remote", "local");
        if (v2 != v)
            EXIT();

        if (inst.volumesSize != EUCA_MAX_VOLUMES - 1)
            EXIT();

        v = add_volume(&inst, id, "remote", "local");
        if (v == NULL)
            EXIT();

        if (inst.volumesSize != EUCA_MAX_VOLUMES)
            EXIT();

        v2 = free_volume(&inst, id, "remote", "local");
        if (v2 != v)
            EXIT();

        if (inst.volumesSize != EUCA_MAX_VOLUMES - 1)
            EXIT();

        v = add_volume(&inst, id, "remote", "local");
        if (v == NULL)
            EXIT();

        if (inst.volumesSize != EUCA_MAX_VOLUMES)
            EXIT();

        for (i = 0; i < EUCA_MAX_VOLUMES; i++) {
            snprintf(id, 100, "v%06d", i);
            v = free_volume(&inst, id, "remote", "local");
            if (v == NULL) {
                fprintf(stderr, "error on free iteration %i-%i\n", i, j);
                EXIT();
            }

            if (inst.volumesSize != EUCA_MAX_VOLUMES - i - 1) {
                fprintf(stderr, "error on free iteration %i-%i\n", i, j);
                EXIT();
            }
        }
    }
}
#endif /* EUCA_DEPRECATED_API */

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK
//!
int main(int argc, char *argv[])
{
    char *s = strdup("jolly old jolly old time...");
    char **sp = &s;

    if (diff("/etc/motd", "/etc/motd") != 0)
        EXIT();

    if (diff("/etc/passwd", "/etc/motd") == 0)
        EXIT();

    if (strcmp(euca_strreplace(sp, "old", "new"), "jolly new jolly new time..."))
        EXIT();

    if (euca_execlp(NULL, "ls", "/", "/etc", NULL) != EUCA_OK)
        EXIT();

    if (euca_execlp(NULL, "sh", "-c", "ls / /etc > /dev/null", NULL) != EUCA_OK)
        EXIT();

#if EUCA_DEPRECATED_API
    test_volumes();
#endif /* EUCA_DEPRECATED_API */

    printf("all tests passed!\n");
    if (argc == 1)
        return (EUCA_OK);

    /* "visual" testing of the semaphores */
    test_sem_fork();
    test_sem_pthreads();

    return (EUCA_OK);
}

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

// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

//!
//! @file node/libvirt_tortura.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64    // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU
#include <string.h>             /* strlen, strcpy */
#include <time.h>
#include <limits.h>             /* INT_MAX */
#include <sys/types.h>          /* fork */
#include <sys/wait.h>           /* waitpid */
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <errno.h>
#include <sys/stat.h>
#include <pthread.h>
#include <sys/vfs.h>            /* statfs */
#include <signal.h>             /* SIGINT */
#include <linux/limits.h>
#include <pwd.h>                /* getpwuid_r */
#include <libvirt/libvirt.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAXDOMS                                   100
#define ITERS                                     300
#define SITERS                                     20
#define IITERS                                   1000
#define THREADS                                    11

#define DUMMY_DISK_FILE                          "libvirt_tortura-dummy-disk"
#define DUMMY_DISK_SIZE_BYTES                    10000000

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

virConnectPtr conn = NULL;
char uri[] = "qemu:///system";

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static pthread_mutex_t check_mutex = PTHREAD_MUTEX_INITIALIZER;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int main(int argc, char **argv);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static virConnectPtr *check_hypervisor_conn(void);
static void *checker_thread(void *ptr);
static void *tortura_thread(void *ptr);
static void *startup_thread(void *ptr);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//!
//!
//! @return a pointer to the LIBVIRT connection structure if successful or NULL otherwize
//!
//! @pre
//!
//! @note
//!
static virConnectPtr *check_hypervisor_conn(void)
{
    int rc = 0;

    pthread_mutex_lock(&check_mutex);
    {
        if (conn) {
            if ((rc = virConnectClose(conn)) != 0) {
                printf("refcount on close was non-zero: %d\n", rc);
            }

            conn = NULL;
        }

        if ((conn = virConnectOpen(uri)) == NULL) {
            printf("failed to connect to %s\n", uri);
        }
    }
    pthread_mutex_unlock(&check_mutex);
    return (&conn);
}

//!
//!
//!
//! @param[in] ptr
//!
//! @return Always returns NULL
//!
static void *checker_thread(void *ptr)
{
    int iter = 0;

    for (iter = 0; iter < IITERS; iter++) {
        printf("checker thread starting iteration %d\n", iter);
        check_hypervisor_conn();
    }

    return (NULL);
}

//!
//!
//!
//! @param[in] ptr
//!
//! @return Always returns NULL
//!
static void *tortura_thread(void *ptr)
{
    int i = 0;
    int iter = 0;
    int error = 0;
    int num_doms = 0;
    int dom_ids[MAXDOMS] = { 0 };
    long long tid = (*(long long *)ptr);
    virDomainPtr dom[MAXDOMS] = { NULL };
    virDomainInfo info[MAXDOMS] = { 0 };

    check_hypervisor_conn();
    for (iter = 0; iter < ITERS; iter++) {
        printf("thread %lld starting iteration %d\n", tid, iter);

        pthread_mutex_lock(&check_mutex);
        {
            num_doms = virConnectListDomains(conn, dom_ids, MAXDOMS);
        }
        pthread_mutex_unlock(&check_mutex);

        for (i = 0; i < num_doms; i++) {
            pthread_mutex_lock(&check_mutex);
            {
                dom[i] = virDomainLookupByID(conn, dom_ids[i]);
            }
            pthread_mutex_unlock(&check_mutex);

            if (!dom[i]) {
                printf("failed to look up domain %d\n", dom_ids[i]);
                continue;
            }
        }

        for (i = 0; i < num_doms; i++) {
            pthread_mutex_lock(&check_mutex);
            {
                error = virDomainGetInfo(dom[i], &info[i]);
            }
            pthread_mutex_unlock(&check_mutex);

            if (error < 0) {
                printf("failed to get info on domain %d\n", dom_ids[i]);
                continue;
            }
        }

        for (i = 0; i < num_doms; i++) {
            pthread_mutex_lock(&check_mutex);
            {
                error = virDomainFree(dom[i]);
            }
            pthread_mutex_unlock(&check_mutex);

            if (error < 0) {
                printf("failed to close domain %d\n", dom_ids[i]);
                continue;
            }
        }
    }

    return (NULL);
}

//!
//!
//!
//! @param[in] arg
//!
//! @return Always returns NULL
//!
static void *startup_thread(void *arg)
{
    int fd = 0;
    int iter = 0;
    long long tid = (*(long long *)arg);
    virDomainPtr dom = NULL;
    char file_name[1024] = { 0 };
    char xml[4096] = { 0 };
    char *xml_template =
        "<?xml version='1.0' encoding='UTF-8'?>"
        "<domain type='kvm'>"
        "  <name>tortura-%04lld</name>"
        "  <description>Eucalyptus instance i-XXX</description>"
        "  <os>"
        "    <type>hvm</type>"
        "  </os>"
        "  <features>"
        "    <acpi/>"
        "  </features>"
        "  <clock offset='localtime'/>"
        "  <on_poweroff>destroy</on_poweroff>"
        "  <on_reboot>restart</on_reboot>"
        "  <on_crash>destroy</on_crash>"
        "  <vcpu>1</vcpu>"
        "  <memory>52428</memory>"
        "  <devices>" "    <disk device='disk'>" "      <source file='%s'/>" "      <target bus='virtio' dev='vda'/>" "    </disk>" "  </devices>"
        "</domain>";

    snprintf(file_name, sizeof(file_name), "%s/%s-%lld", get_current_dir_name(), DUMMY_DISK_FILE, tid);

    umask(0000);
    if ((fd = open(file_name, O_RDWR | O_CREAT | O_TRUNC, 0666)) < 0) {
        printf("failed to create %s\n", DUMMY_DISK_FILE);
        perror("libvirt_tortura");
        exit(1);
    }

    if (lseek(fd, DUMMY_DISK_SIZE_BYTES, SEEK_SET) == ((off_t) - 1)) {
        printf("failed to seek in %s\n", DUMMY_DISK_FILE);
        perror("libvirt_tortura");
        exit(1);
    }

    if (write(fd, "x", 1) != 1) {
        printf("failed to write to %s\n", DUMMY_DISK_FILE);
        perror("libvirt_tortura");
        exit(1);
    }

    close(fd);

    sync();

    check_hypervisor_conn();
    for (iter = 0; iter < SITERS; iter++) {
        printf("startup thread %lld starting iteration %d\n", tid, iter);
        snprintf(xml, sizeof(xml), xml_template, tid, file_name);

        pthread_mutex_lock(&check_mutex);
        {
            dom = virDomainCreateLinux(conn, xml, 0);
        }
        pthread_mutex_unlock(&check_mutex);

        sleep(3);

        if (dom == NULL) {
            printf("ERROR: failed to start domain\n");
            continue;
        }

        pthread_mutex_lock(&check_mutex);
        {
            virDomainDestroy(dom);
            virDomainFree(dom);
            dom = NULL;
        }
        pthread_mutex_unlock(&check_mutex);
    }

    unlink(file_name);

    return (NULL);
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
int main(int argc, char **argv)
{
    int j = 0;
    int thread_par_sum = 0;
    long long thread_par[THREADS] = { 0 };
    pthread_t threads[THREADS] = { 0 };

    printf("spawning %d competing threads\n", THREADS);

    for (j = 0; j < THREADS; j++) {
        thread_par[j] = j;
        if (j == 0) {
            pthread_create(&threads[j], NULL, checker_thread, (void *)&thread_par[j]);
        } else {
            if ((j % 2) == 0) {
                pthread_create(&threads[j], NULL, tortura_thread, (void *)&thread_par[j]);
            } else {
                pthread_create(&threads[j], NULL, startup_thread, (void *)&thread_par[j]);
            }
        }
    }

    for (j = 0; j < THREADS; j++) {
        pthread_join(threads[j], NULL);
        thread_par_sum += (int)thread_par[j];
    }

    printf("waited for all competing threads\n");
    return (0);
}

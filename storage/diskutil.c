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
//! @file storage/diskutil.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <unistd.h>
#include <getopt.h>
#include <string.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <errno.h>

#include <eucalyptus.h>
#include <misc.h>                      // logprintfl
#include <ipc.h>                       // sem
#include <euca_string.h>

#include "diskutil.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define LOOP_RETRIES                             9
#define OUTPUT_ALLOC_CHUNK 1024
#define MAX_OUTPUT_BYTES 1024*1024

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

enum {
    CHMOD = 0,
    CHOWN,
    CP,
    DD,
    FILECMD,
    GRUB,
    GRUB_SETUP,
    GRUB_INSTALL,
    LOSETUP,
    MKDIR,
    MKEXT3,
    MKSWAP,
    MOUNT,
    PARTED,
    TUNE2FS,
    UMOUNT,
    ROOTWRAP,
    MOUNTWRAP,
    LASTHELPER
};

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

static char *helpers[LASTHELPER] = {
    "chmod",
    "chown",
    "cp",
    "dd",
    "file",
    "grub",
    "grub-setup",
    "grub-install",
    "losetup",
    "mkdir",
    "mkfs.ext3",
    "mkswap",
    "mount",
    "parted",
    "tune2fs",
    "umount",
    "euca_rootwrap",
    "euca_mountwrap",
};

static char *helpers_path[LASTHELPER] = { NULL };

static char stage_files_dir[EUCA_MAX_PATH] = "";
static int initialized = 0;
static sem *loop_sem = NULL;           //!< semaphore held while attaching/detaching loopback devices
static unsigned char grub_version = 0;
static char euca_home[EUCA_MAX_PATH] = "";

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int try_stage_dir(const char *dir);
static char *pruntf(boolean log_error, char *format, ...)
_attribute_wur_ _attribute_format_(2, 3);
static char *execlp_output(boolean log_error, ...);

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
//! Looks for file 'stage1' in dir.
//!
//! @param[in] dir the path where we are looking for the stage1 file
//!
//! @return EUCA_OK if the file is found or the following error code:
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if the 'stage1' file isn't found in the directory
//!
//! @pre The dir parameter must not be NULL
//!
//! @post If the 'stage1' file is found, the stage_file_dir variable is set.
//!
//! @note
//!
static int try_stage_dir(const char *dir)
{
    char stage_file_path[EUCA_MAX_PATH] = { 0 };
    if (dir) {
        snprintf(stage_file_path, sizeof(stage_file_path), "%s/stage1", dir);
        if (check_file(stage_file_path))
            return (EUCA_NOT_FOUND_ERROR);
        euca_strncpy(stage_files_dir, dir, sizeof(stage_files_dir));
        return (EUCA_OK);
    }
    return (EUCA_INVALID_ERROR);
}

int imaging_init(const char *euca_home_path)
{
    assert(euca_home_path);
    euca_strncpy(euca_home, euca_home_path, sizeof(euca_home));
    return EUCA_OK;
}

int imaging_image_by_manifest_url(const char *instanceId, const char *url, const char *dest_path, long long size_bytes)
{
    LOGDEBUG("[%s] getting download manifest from %s\n", instanceId, url);

    char cmd[1024];
    snprintf(cmd, sizeof(cmd),
             "%s/usr/libexec/eucalyptus/euca-run-workflow down-bundle/write-raw"
             " --image-manifest-url '%s'"
             " --output-path '%s'"
             " --cloud-cert-path '%s/var/lib/eucalyptus/keys/cloud-cert.pem'"
             " --decryption-key-path '%s/var/lib/eucalyptus/keys/node-pk.pem' >> /tmp/euca_nc_unbundle.log 2>&1", euca_home, url, dest_path, euca_home, euca_home);
    LOGDEBUG("%s\n", cmd);
    if (system(cmd) == 0) {
        LOGDEBUG("[%s] downloaded and unbundled %s\n", instanceId, url);
        return EUCA_OK;
    } else {
        LOGERROR("[%s] failed on download and unbundle with command %s\n", instanceId, cmd);
        return EUCA_ERROR;
    }
    return EUCA_ERROR;
}

//!
//!
//!
//! @param[in] require_grub FALSE = not required, TRUE = required
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
int diskutil_init(boolean require_grub)
{
    int i = 0;
    int ret = EUCA_OK;
    int missing_handlers = 0;

    // Just in case
    if (require_grub > FALSE)
        require_grub = TRUE;

    // if init was called without grub requirement, it will run again if grub is needed now
    if (initialized < (1 + require_grub)) {
        bzero(helpers_path, sizeof(helpers_path));  //! @fixme: chuck - this does not reset the whole array
        missing_handlers = verify_helpers(helpers, helpers_path, LASTHELPER);
        if (helpers_path[GRUB])
            grub_version = 1;
        else
            missing_handlers--;

        if (helpers_path[GRUB_SETUP]) {
            // don't need it, but grub-setup only exists on v2
            if (grub_version != 1) {
                // prefer 1 until 2 is implemented
                grub_version = 2;
            }
        } else
            missing_handlers--;

        if (require_grub && grub_version == 0) {
            LOGERROR("cannot find either grub 1 or grub 2\n");
            ret = EUCA_ERROR;
        } else if (grub_version == 1) {
            // grub 1 commands seem present, check for stage files, which we will be copying
            if (!try_stage_dir("/usr/lib/grub/x86_64-pc") || !try_stage_dir("/usr/lib/grub/i386-pc") || !try_stage_dir("/usr/lib/grub")
                || !try_stage_dir("/boot/grub")) {
                LOGINFO("found grub 1 stage files in %s\n", stage_files_dir);
            } else if (require_grub) {
                LOGERROR("failed to find grub 1 stage files (in /boot/grub et al)\n");
                ret = EUCA_ERROR;
            }
        } else if (grub_version == 2) {
            LOGINFO("detected grub 2\n");
        }
        // flag missing handlers
        if (missing_handlers) {
            for (i = 0; i < LASTHELPER; i++) {
                if (helpers_path[i] == NULL && i != GRUB && i != GRUB_SETUP && i != GRUB_INSTALL) {
                    LOGERROR("missing a required handler: %s\n", helpers[i]);
                    ret = EUCA_ERROR;
                }
            }
        }

        if ((initialized < 1) && (loop_sem == NULL))
            loop_sem = sem_alloc(1, IPC_MUTEX_SEMAPHORE);
        initialized = 1 + require_grub;
    }

    return (ret);
}

//!
//!
//!
//! @return EUCA_OK
//!
int diskutil_cleanup(void)
{
    int i = 0;
    for (i = 0; i < LASTHELPER; i++) {
        EUCA_FREE(helpers_path[i]);
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] path
//! @param[in] sectors
//! @param[in] zero_fill
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to create the disk file
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre The path parameter must not be NULL.
//!
//! @post On success, the disk file is created.
//!
int diskutil_ddzero(const char *path, const long long sectors, boolean zero_fill)
{
    char *output = NULL;
    long long count = 1;
    long long seek = sectors - 1;

    if (path) {
        if (zero_fill) {
            count = sectors;
            seek = 0;
        }

        char of_str[EUCA_MAX_PATH] = "";
        snprintf(of_str, sizeof(of_str), "of=%s", path);
        char seek_str[64];
        snprintf(seek_str, sizeof(seek_str), "seek=%lld", seek);
        char count_str[64];
        snprintf(count_str, sizeof(count_str), "count=%lld", count);
        output = execlp_output(TRUE, helpers_path[ROOTWRAP], helpers_path[DD], "bs=512", "if=/dev/zero", of_str, seek_str, count_str, NULL);
        if (!output) {
            LOGERROR("cannot create disk file %s\n", path);
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGWARN("bad params: path=%s\n", SP(path));
    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] in
//! @param[in] out
//! @param[in] bs
//! @param[in] count
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to copy the disk file
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre Both in and out paramters must not be NULL.
//!
//! @post On success the data from 'in' has been copied in 'out'.
//!
int diskutil_dd(const char *in, const char *out, const int bs, const long long count)
{
    char *output = NULL;

    if (in && out) {
        LOGINFO("copying data from '%s'\n", in);
        LOGINFO("               to '%s' (blocks=%lld)\n", out, count);

        char if_str[EUCA_MAX_PATH] = "";
        snprintf(if_str, sizeof(if_str), "if=%s", in);
        char of_str[EUCA_MAX_PATH] = "";
        snprintf(of_str, sizeof(of_str), "of=%s", out);
        char bs_str[64];
        snprintf(bs_str, sizeof(bs_str), "bs=%d", bs);
        char count_str[64];
        snprintf(count_str, sizeof(count_str), "count=%lld", count);
        output = execlp_output(TRUE, helpers_path[ROOTWRAP], helpers_path[DD], if_str, of_str, bs_str, count_str, NULL);
        if (!output) {
            LOGERROR("cannot copy '%s'\n", in);
            LOGERROR("                to '%s'\n", out);
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGWARN("bad params: in=%s, out=%s\n", SP(in), SP(out));
    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] in
//! @param[in] out
//! @param[in] bs
//! @param[in] count
//! @param[in] seek
//! @param[in] skip
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to copy the disk file
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre Both in and out paramters must not be NULL.
//!
//! @post On success the data from 'in' has been copied in 'out'.
//!
int diskutil_dd2(const char *in, const char *out, const int bs, const long long count, const long long seek, const long long skip)
{
    char *output = NULL;

    if (in && out) {
        LOGINFO("copying data from '%s'\n", in);
        LOGINFO("               to '%s'\n", out);
        LOGINFO("               of %lld blocks (bs=%d), seeking %lld, skipping %lld\n", count, bs, seek, skip);

        char if_str[EUCA_MAX_PATH] = "";
        snprintf(if_str, sizeof(if_str), "if=%s", in);
        char of_str[EUCA_MAX_PATH] = "";
        snprintf(of_str, sizeof(of_str), "of=%s", out);
        char bs_str[64];
        snprintf(bs_str, sizeof(bs_str), "bs=%d", bs);
        char count_str[64];
        snprintf(count_str, sizeof(count_str), "count=%lld", count);
        char seek_str[64];
        snprintf(seek_str, sizeof(seek_str), "seek=%lld", seek);
        char skip_str[64];
        snprintf(skip_str, sizeof(skip_str), "skip=%lld", skip);
        output = execlp_output(TRUE, helpers_path[ROOTWRAP], helpers_path[DD], if_str, of_str, bs_str, count_str, seek_str, skip_str, "conv=notrunc,fsync", NULL);
        if (!output) {
            LOGERROR("cannot copy '%s'\n", in);
            LOGERROR("                to '%s'\n", out);
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGWARN("bad params: in=%s, out=%s\n", SP(in), SP(out));
    return (EUCA_INVALID_ERROR);
}

//!
//! Creates a Master Boot Record (MBR) of the given type at the given path
//!
//! @param[in] path path where we need to create the MBR
//! @param[in] type type of MBR to create
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to create the MBR
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre Both path and type parameters must not be NULL.
//!
//! @post On success, the MBR is created
//!
int diskutil_mbr(const char *path, const char *type)
{
    char *output = NULL;

    if (path && type) {
        output = pruntf(TRUE, "LD_PRELOAD='' %s %s --script %s mklabel %s", helpers_path[ROOTWRAP], helpers_path[PARTED], path, type);
        if (!output) {
            LOGERROR("cannot create an MBR\n");
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGWARN("bad params: path=%s, type=%s\n", SP(path), SP(type));
    return (EUCA_INVALID_ERROR);
}

//!
//! Creates a new partition of the given part_type at the given path.
//!
//! @param[in] path path where to create the partition
//! @param[in] part_type the type of partition
//! @param[in] fs_type the type of file system (if NULL, "" is assumed)
//! @param[in] first_sector the first sector of the partition
//! @param[in] last_sector the last sector of this partition
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to create the partition
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre Both path and part_type parameters must not be NULL.
//!
//! @post On success, the partition is created
//!
int diskutil_part(const char *path, char *part_type, const char *fs_type, const long long first_sector, const long long last_sector)
{
    char *output = NULL;

    if (path && part_type) {
        output = pruntf(TRUE, "LD_PRELOAD='' %s %s --script %s mkpart %s %s %llds %llds",
                        helpers_path[ROOTWRAP], helpers_path[PARTED], path, part_type, ((fs_type != NULL) ? (fs_type) : ("")), first_sector, last_sector);
        if (!output) {
            LOGERROR("cannot add a partition\n");
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGWARN("bad params: path=%s, part_type=%s\n", SP(path), SP(part_type));
    return (EUCA_INVALID_ERROR);
}

//!
//! Expose the loop semaphore so others (e.g., instance startup code)
//! can avoid races with 'losetup' that we've seen on Xen
//!
//! @return a pointer to the loop semaphore
//!
sem *diskutil_get_loop_sem(void)
{
    return (loop_sem);
}

//!
//!
//!
//! @param[in] path
//! @param[in] lodev
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any error occured
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre Both path and lodev parameters must not be NULL.
//!
//! @todo since 'losetup' truncates paths in its output, this check is not perfect. It
//!       may approve loopback devices that are actually pointing at a different path.
//!
int diskutil_loop_check(const char *path, const char *lodev)
{
    int ret = EUCA_OK;
    char *output = NULL;
    char *oparen = NULL;
    char *cparen = NULL;

    if (path && lodev) {
        output = pruntf(TRUE, "%s %s %s", helpers_path[ROOTWRAP], helpers_path[LOSETUP], lodev);
        if (output == NULL)
            return (EUCA_ERROR);

        // output is expected to look like: /dev/loop4: [0801]:5509589 (/var/lib/eucalyptus/volumes/v*)
        oparen = strchr(output, '(');
        cparen = strchr(output, ')');
        if ((oparen == NULL) || (cparen == NULL)) {
            // no parenthesis => unexpected `losetup` output
            ret = EUCA_ERROR;
        } else if ((cparen - oparen) < 3) {
            // strange paren arrangement => unexpected
            ret = EUCA_ERROR;
        } else {
            // extract just the path, possibly truncated, from inside the parens
            oparen++;
            cparen--;
            if (*cparen == '*') {
                // handle truncated paths, identified with an asterisk
                cparen--;
            }
            // truncate ')' or '*)'
            *cparen = '\0';

            // see if path is in the blobstore
            if (strstr(path, oparen) == NULL) {
                ret = EUCA_ERROR;
            }
        }

        EUCA_FREE(output);
        return (ret);
    }

    LOGWARN("bad params: path=%s, lodev=%s\n", SP(path), SP(lodev));
    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] path
//! @param[in] offset
//! @param[in] lodev name of the loop device
//! @param[in] lodev_size
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any error occured.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!
//! @pre Both path and lodev parameters must not be NULL.
//!
//! @post On success, the loop device is attached.
//!
int diskutil_loop(const char *path, const long long offset, char *lodev, int lodev_size)
{
    int i = 0;
    int ret = EUCA_OK;
    char *ptr = NULL;
    char *output = NULL;
    boolean done = FALSE;
    boolean found = FALSE;
    boolean do_log = FALSE;

    if (path && lodev) {
        // we retry because we cannot atomically obtain a free loopback device on all distros (some
        // versions of 'losetup' allow a file argument with '-f' options, but some do not)
        for (i = 0, done = FALSE, found = FALSE; i < LOOP_RETRIES; i++) {
            sem_p(loop_sem);
            {
                output = pruntf(TRUE, "%s %s -f", helpers_path[ROOTWRAP], helpers_path[LOSETUP]);
            }
            sem_v(loop_sem);

            if (output == NULL) {
                // there was a problem
                break;
            }

            if (strstr(output, "/dev/loop")) {
                strncpy(lodev, output, lodev_size);
                if ((ptr = strrchr(lodev, '\n')) != NULL) {
                    *ptr = '\0';
                    found = TRUE;
                }
            }

            EUCA_FREE(output);

            if (found) {
                do_log = ((i + 1) == LOOP_RETRIES); // log error on last try only
                LOGDEBUG("attaching file %s\n", path);
                LOGDEBUG("            to %s at offset %lld\n", lodev, offset);
                sem_p(loop_sem);
                {
                    char str_offset[64];
                    snprintf(str_offset, sizeof(str_offset), "%lld", offset);
                    output = execlp_output(do_log, helpers_path[ROOTWRAP], helpers_path[LOSETUP], "-o", str_offset, lodev, path, NULL);
                }
                sem_v(loop_sem);

                if (output == NULL) {
                    LOGDEBUG("cannot attach to loop device %s (will retry)\n", lodev);
                } else {
                    EUCA_FREE(output);
                    done = TRUE;
                    break;
                }
            }

            sleep(1);
            found = FALSE;
        }

        if (!done) {
            LOGERROR("cannot find free loop device or attach to one\n");
            ret = EUCA_ERROR;
        }

        return (ret);
    }

    LOGWARN("cannot attach to loop device. path=%s, lodev=%s\n", SP(path), SP(lodev));
    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] lodev name of the loop device
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any error occured.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!
//! @pre The lodev parameters must not be NULL.
//!
//! @post On success, the loop device is detached.
//!
int diskutil_unloop(const char *lodev)
{
    int i = 0;
    int ret = EUCA_OK;
    int retried = 0;
    char *output = NULL;
    boolean do_log = FALSE;

    if (lodev) {
        LOGDEBUG("detaching from loop device %s\n", lodev);

        // we retry because we have seen spurious errors from 'losetup -d' on Xen:
        //     ioctl: LOOP_CLR_FD: Device or resource bus
        for (i = 0; i < LOOP_RETRIES; i++) {
            do_log = ((i + 1) == LOOP_RETRIES); // log error on last try only
            sem_p(loop_sem);
            {
                output = execlp_output(do_log, helpers_path[ROOTWRAP], helpers_path[LOSETUP], "-d", lodev, NULL);
            }
            sem_v(loop_sem);

            if (!output) {
                ret = EUCA_ERROR;
            } else {
                ret = EUCA_OK;
                EUCA_FREE(output);
                break;
            }

            LOGDEBUG("cannot detach loop device %s (will retry)\n", lodev);
            retried++;
            sleep(1);
        }

        if (ret == EUCA_ERROR) {
            LOGWARN("cannot detach loop device\n");
        } else if (retried) {
            LOGINFO("succeeded to detach %s after %d retries\n", lodev, retried);
        }

        return (ret);
    }

    LOGWARN("cannot detach loop device. lodev=%s\n", SP(lodev));
    return (EUCA_INVALID_ERROR);
}

//!
//! Format a partition as a swap.
//!
//! @param[in] lodev
//! @param[in] size_bytes
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any error occured.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!
//! @pre The lodev parameters must not be NULL.
//!
//! @post On success, the partition is formatted as swap.
//!
int diskutil_mkswap(const char *lodev, const long long size_bytes)
{
    char *output = NULL;

    if (lodev) {
        output = pruntf(TRUE, "%s %s %s %lld", helpers_path[ROOTWRAP], helpers_path[MKSWAP], lodev, size_bytes / 1024);
        if (!output) {
            LOGERROR("cannot format partition on '%s' as swap\n", lodev);
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGWARN("cannot format partition as swap. lodev=%s\n", SP(lodev));
    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] lodev
//! @param[in] size_bytes
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any error occured.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!
//! @pre The lodev parameters must not be NULL.
//!
//! @post On success, the loop device is attached.
//!
int diskutil_mkfs(const char *lodev, const long long size_bytes)
{
    int block_size = 4096;
    char *output = NULL;

    if (lodev) {
        output = pruntf(TRUE, "%s %s -b %d %s %lld", helpers_path[ROOTWRAP], helpers_path[MKEXT3], block_size, lodev, size_bytes / block_size);
        if (!output) {
            LOGERROR("cannot format partition on '%s' as ext3\n", lodev);
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGWARN("cannot format partition as ext3. lodev=%s\n", SP(lodev));
    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] lodev
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any error occured.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!
//! @pre The lodev parameters must not be NULL.
//!
//! @post On success, the loop device is attached.
//!
int diskutil_tune(const char *lodev)
{
    char *output = NULL;

    if (lodev) {
        sem_p(loop_sem);
        {
            output = pruntf(TRUE, "%s %s %s -c 0 -i 0", helpers_path[ROOTWRAP], helpers_path[TUNE2FS], lodev);
        }
        sem_v(loop_sem);

        if (!output) {
            LOGERROR("cannot tune file system on '%s'\n", lodev);
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGWARN("cannot tune file system. lodev=%s\n", SP(lodev));
    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in]  path
//! @param[in]  part
//! @param[out] first
//! @param[out] last
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any issue occured.
//!         \li EUCA_INVALID_ERROR: if any paramter does not meet the preconditions.
//!
//! @pre The path, first and last parameters must not be NULL.
//!
//! @post On success, the partition information has been extracted and contained in first and last.
//!
int diskutil_sectors(const char *path, const int part, long long *first, long long *last)
{
    int p = 0;
    int ret = EUCA_ERROR;
    char *ss = NULL;
    char *end = NULL;
    char *comma = NULL;
    char *output = NULL;
    char *section = NULL;
    boolean found = FALSE;

    if (path && first && last) {
        *first = 0L;
        *last = 0L;

        if ((output = pruntf(TRUE, "%s %s", helpers_path[FILECMD], path)) == NULL) {
            LOGERROR("failed to extract partition information for '%s'\n", path);
        } else {
            // parse the output, such as:
            // NAME: x86 boot sector;
            // partition 1: ID=0x83, starthead 1, startsector 63, 32769 sectors;
            // partition 2: ID=0x83, starthead 2, startsector 32832, 32769 sectors;
            // partition 3: ID=0x82, starthead 2, startsector 65601, 81 sectors
            found = FALSE;
            section = strtok(output, ";");  // split by semicolon
            for (p = 0; section != NULL; p++) {
                section = strtok(NULL, ";");
                if (section && (p == part)) {
                    found = TRUE;
                    break;
                }
            }

            if (found) {
                if ((ss = strstr(section, "startsector")) != NULL) {
                    ss += strlen("startsector ");
                    if ((comma = strstr(ss, ", ")) != NULL) {
                        *comma = '\0';
                        comma += strlen(", ");
                        if ((end = strstr(comma, " sectors")) != NULL) {
                            *end = '\0';
                            *first = atoll(ss);
                            *last = *first + atoll(comma) - 1L;
                        }
                    }
                }
            }

            EUCA_FREE(output);
        }

        if (*last > 0)
            ret = EUCA_OK;

        return (ret);
    }

    LOGERROR("failed to extract partition information for '%s'\n", SP(path));
    return (EUCA_INVALID_ERROR);
}

//!
//! Mounts a given device at a given location.
//!
//! @param[in] dev
//! @param[in] mnt_pt
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any issue occured.
//!         \li EUCA_INVALID_ERROR: if any paramter does not meet the preconditions.
//!
//! @pre Both dev and mnt_pt parameters must not be NULL.
//!
//! @post On success, the device is successfully mounted at its mounting point.
//!
int diskutil_mount(const char *dev, const char *mnt_pt)
{
    char *output = NULL;

    if (dev && mnt_pt) {
        sem_p(loop_sem);
        {
            output = pruntf(TRUE, "%s %s mount %s %s", helpers_path[ROOTWRAP], helpers_path[MOUNTWRAP], dev, mnt_pt);
        }
        sem_v(loop_sem);

        if (!output) {
            LOGERROR("cannot mount device '%s' on '%s'\n", dev, mnt_pt);
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGERROR("cannot mount device '%s' on '%s'\n", SP(dev), SP(mnt_pt));
    return (EUCA_INVALID_ERROR);
}

//!
//! Unmount a given device
//!
//! @param[in] dev
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any issue occured.
//!         \li EUCA_INVALID_ERROR: if any paramter does not meet the preconditions.
//!
//! @pre \li The dev paramter must not be NULL.
//!      \li The given device must already be mounted.
//!
//! @post On success, the device is unmounted from the file system.
//!
int diskutil_umount(const char *dev)
{
    char *output = NULL;

    if (dev) {
        sem_p(loop_sem);
        {
            output = pruntf(TRUE, "%s %s umount %s", helpers_path[ROOTWRAP], helpers_path[MOUNTWRAP], dev);
        }
        sem_v(loop_sem);

        if (!output) {
            LOGERROR("cannot unmount device '%s'\n", dev);
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    LOGERROR("cannot unmount device '%s'\n", SP(dev));
    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] file
//! @param[in] str
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ACCESS_ERROR: if we fail to write the given data.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!         \li EUCA_PERMISSION_ERROR: if we fail to create the temp or destination files.
//!
//! @pre Both file and str parameters must not be NULL.
//!
//! @post On success, the file is created and contains the str data.
//!
int diskutil_write2file(const char *file, const char *str)
{
    int fd = -1;
    int ret = EUCA_OK;
    int size = 0;
    char tmpfile[] = "/tmp/euca-temp-XXXXXX";

    if (file && str) {
        if ((fd = safe_mkstemp(tmpfile)) < 0) {
            LOGERROR("failed to create temporary directory\n");
            unlink(tmpfile);
            return (EUCA_PERMISSION_ERROR);
        }

        size = strlen(str);
        if (write(fd, str, size) != size) {
            LOGERROR("failed to write to temporary directory\n");
            ret = EUCA_ACCESS_ERROR;
        } else {
            if (diskutil_cp(tmpfile, file) != EUCA_OK) {
                LOGERROR("failed to copy temp file (%s)\n", file);
                ret = EUCA_PERMISSION_ERROR;
            }
        }

        close(fd);
        unlink(tmpfile);
        return (ret);
    }

    LOGERROR("failed to write to file. Bad params: file=%p, str=%p\n", file, str);
    return (EUCA_INVALID_ERROR);
}

//!
//! combines functionalities of diskutil_grub_files() and diskutil_grub2_mbr(),
//! performing them one after another
//!
//! @param[in] path
//! @param[in] mnt_pt
//! @param[in] part
//! @param[in] kernel
//! @param[in] ramdisk
//!
//! @return The result of either diskutil_grub_files() or diskutil_grub2_mbr().
//!
//! @see diskutil_grub2_mbr()
//! @see diskutil_grub_files()
//!
//! @pre The path, mnt_pt and kernel parameters should not be NULL;
//!
//! @post
//!
int diskutil_grub(const char *path, const char *mnt_pt, const int part, const char *kernel, const char *ramdisk)
{
    int ret = EUCA_ERROR;

    if ((ret = diskutil_grub_files(mnt_pt, part, kernel, ramdisk)) != EUCA_OK)
        return (ret);

    ret = diskutil_grub2_mbr(path, part, mnt_pt);
    return (ret);
}

//!
//!
//!
//! @param[in] mnt_pt
//! @param[in] part
//! @param[in] kernel
//! @param[in] ramdisk
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any error occured.
//!         \li EUCA_INVALID_ERROR: if any paramter does not meet the preconditions
//!
//! @pre Both mnt_pt and kernel parameters must not be NULL.
//!
//! @post
//!
int diskutil_grub_files(const char *mnt_pt, const int part, const char *kernel, const char *ramdisk)
{
    char *output = NULL;
    char *kfile = "euca-vmlinuz";
    char *rfile = "euca-initrd";
    char buf[1024] = { 0 };
    char buf2[1024] = { 0 };
    char initrd[1024] = { 0 };
    char grub_conf_path[EUCA_MAX_PATH] = { 0 };
    char menu_lst_path[EUCA_MAX_PATH] = { 0 };

    if (mnt_pt && kernel) {
        LOGINFO("installing kernel and ramdisk\n");
        output = pruntf(TRUE, "%s %s -p %s/boot/grub/", helpers_path[ROOTWRAP], helpers_path[MKDIR], mnt_pt);
        if (!output) {
            LOGERROR("failed to create grub directory\n");
            return (EUCA_ERROR);
        }
        EUCA_FREE(output);

        if (grub_version == 1) {
            output = pruntf(TRUE, "%s %s %s/*stage* %s/boot/grub", helpers_path[ROOTWRAP], helpers_path[CP], stage_files_dir, mnt_pt);
            if (!output) {
                LOGERROR("failed to copy stage files into grub directory\n");
                return (EUCA_ERROR);
            }
            EUCA_FREE(output);
        }

        output = pruntf(TRUE, "%s %s %s %s/boot/%s", helpers_path[ROOTWRAP], helpers_path[CP], kernel, mnt_pt, kfile);
        if (!output) {
            LOGERROR("failed to copy the kernel to boot directory\n");
            return (EUCA_ERROR);
        }
        EUCA_FREE(output);

        if (ramdisk) {
            output = pruntf(TRUE, "%s %s %s %s/boot/%s", helpers_path[ROOTWRAP], helpers_path[CP], ramdisk, mnt_pt, rfile);
            if (!output) {
                LOGERROR("failed to copy the ramdisk to boot directory\n");
                return (EUCA_ERROR);
            }
            EUCA_FREE(output);
        }

        if (grub_version == 1) {
            snprintf(menu_lst_path, sizeof(menu_lst_path), "%s/boot/grub/menu.lst", mnt_pt);
            snprintf(grub_conf_path, sizeof(grub_conf_path), "%s/boot/grub/grub.conf", mnt_pt);

            snprintf(buf, sizeof(buf), "default=0\n" "timeout=2\n\n" "title TheOS\n" "root (hd0,%d)\n" "kernel /boot/%s root=/dev/sda1 ro\n", part, kfile); // grub 1 expects 0 for first partition
            if (ramdisk) {
                snprintf(buf2, sizeof(buf2), "initrd /boot/%s\n", rfile);
                strncat(buf, buf2, sizeof(buf) - 1);
            }

            if (diskutil_write2file(menu_lst_path, buf) != EUCA_OK) {
                return (EUCA_ERROR);
            }

        } else if (grub_version == 2) {
            snprintf(grub_conf_path, sizeof(grub_conf_path), "%s/boot/grub/grub.cfg", mnt_pt);
            if (ramdisk) {
                snprintf(initrd, sizeof(initrd), "  initrd /boot/%s\n", rfile);
            }
            snprintf(buf, sizeof(buf), "set default=0\n" "set timeout=2\n" "insmod part_msdos\n" "insmod ext2\n" "set root='(hd0,%d)'\n" "menuentry 'TheOS' --class os {\n" "  linux /boot/%s root=/dev/sda1 ro\n" "%s" "}\n", part + 1, kfile, initrd);    // grub 2 expects 1 for first partition
        }

        if (diskutil_write2file(grub_conf_path, buf) != EUCA_OK) {
            return (EUCA_ERROR);
        }

        return (EUCA_OK);
    }

    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] path
//! @param[in] part
//!
//! @return EUCA_ERROR if grub_version is not 1 or the result of the diskutil_grub2_mbr() call.
//!
//! @pre The path parameter should not be NULL.
//!
//! @post
//!
int diskutil_grub_mbr(const char *path, const int part)
{
    if (grub_version != 1) {
        LOGERROR("grub 2 is not supported\n");
        return (EUCA_ERROR);
    }
    return diskutil_grub2_mbr(path, part, NULL);
}

//!
//!
//!
//! @param[in] path
//! @param[in] part
//! @param[in] mnt_pt
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any error occured.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!         \li EUCA_PERMISSION_ERROR: if we fail to create the temporary file.
//!
//! @pre \li The path parameter must not be NULL.
//!      \li If we use GRUB version 2, then the mnt_pt parameter must not be NULL.
//!      \li GRUB must be installed on the system/
//!
//! @note
//!
int diskutil_grub2_mbr(const char *path, const int part, const char *mnt_pt)
{
#define _PR()                         fprintf (fp, "%s", s);    // logprintfl (EUCADEBUG, "\t%s", s)

    int rc = 1;
    int tfd = -1;
    int bytes_read = -1;
    int read_bytes = -1;
    FILE *fp = NULL;
    char *output = NULL;
    char s[EUCA_MAX_PATH] = { 0 };
    char buf[1024] = { 0 };
    char cmd[1024] = { 0 };
    char part_path[EUCA_MAX_PATH] = { 0 };
    char device_map_path[EUCA_MAX_PATH] = { 0 };
    char device_map_buf[512] = { 0 };
    char tmp_file[EUCA_MAX_PATH] = "/tmp/euca-temp-XXXXXX";
    boolean saw_done = FALSE;
    boolean created_partition_softlink = FALSE;

    if (path == NULL) {
        LOGERROR("invocation of diskutil_grub2_mbr unknown path\n");
        return (EUCA_INVALID_ERROR);
    }

    if ((grub_version != 1) && (grub_version != 2)) {
        LOGERROR("invocation of diskutil_grub2_mbr without grub found\n");
        return (EUCA_INVALID_ERROR);
    }

    if ((mnt_pt == NULL) && (grub_version != 1)) {
        LOGERROR("invocation of diskutil_grub2_mbr with grub 1 params\n");
        return (EUCA_INVALID_ERROR);
    }

    LOGINFO("installing grub in MBR\n");
    if (grub_version == 1) {
        if ((tfd = safe_mkstemp(tmp_file)) < 0) {
            LOGERROR("mkstemp() failed: %s\n", strerror(errno));
            return (EUCA_PERMISSION_ERROR);
        }
        // create a soft link of the first partition's device mapper entry in the
        // form that grub is looking for (not DISKp1 but just DISK1)
        created_partition_softlink = FALSE;
        snprintf(part_path, sizeof(EUCA_MAX_PATH), "%s1", path);
        if (check_path(part_path) != 0) {
            if ((output = pruntf(TRUE, "%s /bin/ln -s %sp1 %s", helpers_path[ROOTWRAP], path, part_path)) == NULL) {
                LOGINFO("warning: failed to create partition device soft-link (%s)\n", part_path);
            } else {
                created_partition_softlink = TRUE;
                EUCA_FREE(output);
            }
        }
        // we now invoke grub through euca_rootwrap because it may need to operate on
        // devices that are owned by root (e.g. /dev/mapper/euca-dsk-7E4E131B-fca1d769p1)
        snprintf(cmd, sizeof(cmd), "%s %s --batch >%s 2>&1", helpers_path[ROOTWRAP], helpers_path[GRUB], tmp_file);
        LOGDEBUG("running %s\n", cmd);
        errno = 0;
        if ((fp = popen(cmd, "w")) != NULL) {
            snprintf(s, sizeof(s), "device (hd0) %s\n", path);
            _PR();
            snprintf(s, sizeof(s), "root (hd0,%d)\n", part);
            _PR();
            snprintf(s, sizeof(s), "setup (hd0)\n");
            _PR();
            snprintf(s, sizeof(s), "quit\n");
            _PR();
            rc = pclose(fp);           // base success on exit code of grub
        }

        if (rc) {
            LOGERROR("failed to run grub 1 on disk '%s': %s\n", path, strerror(errno));
        } else {
            bzero(buf, sizeof(buf));
            saw_done = FALSE;
            do {
                // read in a line
                bytes_read = 0;
                while ((sizeof(buf) - 2 - bytes_read) > 0   // there is space in buffer for \n and \0
                       && ((read_bytes = read(tfd, buf + bytes_read, 1)) > 0))
                    if (buf[bytes_read++] == '\n')
                        break;
                if (read_bytes < 0)    // possibly truncated output, ensure there is newline
                    buf[bytes_read++] = '\n';
                buf[bytes_read] = '\0';
                LOGDEBUG("\t%s", buf); // log grub 1 prompts and our inputs
                if (strstr(buf, "Done."))   // this indicates that grub 1 succeeded (the message has been there since 2000)
                    saw_done = TRUE;
            } while (read_bytes > 0);
            close(tfd);

            if (saw_done == FALSE) {
                LOGERROR("failed to run grub 1 on disk '%s'\n", path);
                rc = 1;
            } else {
                rc = 0;
            }
        }

        // try to remove the partition device soft link that may have been created above
        if (created_partition_softlink) {
            if ((output = pruntf(TRUE, "%s /bin/rm %s", helpers_path[ROOTWRAP], part_path)) == NULL) {
                LOGINFO("warning: failed to remove partition device soft-link\n");
            } else {
                EUCA_FREE(output);
            }
        }

    } else if (grub_version == 2) {
        // create device.map file
        snprintf(device_map_path, sizeof(device_map_path), "%s/boot/grub/device.map", mnt_pt);
        snprintf(device_map_buf, sizeof(device_map_buf), "(hd0) %s\n", path);
        if (diskutil_write2file(device_map_path, device_map_buf) != EUCA_OK) {
            LOGWARN("failed to create device.map file\n");
        } else {
            LOGINFO("wrote to '%s':\n", device_map_path);
            LOGINFO("%s", device_map_buf);
        }

        output = pruntf(TRUE, "%s %s --modules='part_msdos ext2' --root-directory=%s '(hd0)'", helpers_path[ROOTWRAP], helpers_path[GRUB_INSTALL], mnt_pt);
        if (!output) {
            LOGERROR("failed to install grub 2 on disk '%s' mounted on '%s'\n", path, mnt_pt);
        } else {
            EUCA_FREE(output);
            rc = 0;
        }
    }

    if (rc == 0)
        return (EUCA_OK);
    return (EUCA_ERROR);

#undef _PR
}

//!
//!
//!
//! @param[in] path
//! @param[in] user
//! @param[in] group
//! @param[in] perms
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: If any issue occured
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!
//! @pre The path parameter must not be NULL.
//!
//! @post On success, the user, group and permission of a given path are modified based on the
//!       given parameters.
//!
int diskutil_ch(const char *path, const char *user, const char *group, const int perms)
{
    char *output = NULL;

    LOGDEBUG("ch(own|mod) '%s' %s.%s %o\n", SP(path), ((user != NULL) ? user : "*"), ((group != NULL) ? group : "*"), perms);

    if (path) {
        if (user) {
            output = execlp_output(TRUE, helpers_path[ROOTWRAP], helpers_path[CHOWN], user, path, NULL);
            if (!output) {
                return (EUCA_ERROR);
            }
            EUCA_FREE(output);
        }

        if (group) {
            char group_str[128];
            snprintf(group_str, sizeof(group_str), ":%s", group);
            output = execlp_output(TRUE, helpers_path[ROOTWRAP], helpers_path[CHOWN], group_str, path, NULL);
            if (!output) {
                return (EUCA_ERROR);
            }
            EUCA_FREE(output);
        }

        if (perms > 0) {
            char perms_str[32];
            snprintf(perms_str, sizeof(perms_str), "0%o", perms);
            output = execlp_output(TRUE, helpers_path[ROOTWRAP], helpers_path[CHMOD], perms_str, path, NULL);
            if (!output) {
                return (EUCA_ERROR);
            }
            EUCA_FREE(output);
        }

        return (EUCA_OK);
    }

    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] path
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to create the directory
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!
//! @pre The path parameter must not be NULL.
//!
//! @post On success, the directory is created.
//!
int diskutil_mkdir(const char *path)
{
    char *output = NULL;

    if (path) {
        if ((output = pruntf(TRUE, "%s %s -p %s", helpers_path[ROOTWRAP], helpers_path[MKDIR], path)) == NULL) {
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] from
//! @param[in] to
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to copy the item.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre Both from and to parameters must not be NULL.
//!
//! @post On success the file has been copied.
//!
int diskutil_cp(const char *from, const char *to)
{
    char *output = NULL;

    if (from && to) {
        if ((output = pruntf(TRUE, "%s %s %s %s", helpers_path[ROOTWRAP], helpers_path[CP], from, to)) == NULL) {
            return (EUCA_ERROR);
        }

        EUCA_FREE(output);
        return (EUCA_OK);
    }

    return (EUCA_INVALID_ERROR);
}

//!
//!
//!
//! @param[in] log_error
//! @param[in] format
//! @param[in] ...
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static char *pruntf(boolean log_error, char *format, ...)
{
    va_list ap;
    FILE *IF = NULL;
    int rc = -1;
    int outsize = 1025;
    char cmd[1024] = { 0 };
    size_t bytes = 0;
    char *output = NULL;

    va_start(ap, format);
    vsnprintf(cmd, 1024, format, ap);

    strncat(cmd, " 2>&1", 1024 - 1);
    output = NULL;

    IF = popen(cmd, "r");
    if (!IF) {
        LOGERROR("cannot popen() cmd '%s' for read\n", cmd);
        va_end(ap);
        return (NULL);
    }

    output = EUCA_ALLOC(outsize, sizeof(char));
    if (output) {
        output[0] = '\0';              // make sure we return an empty string if there is no output
    }

    while ((output != NULL) && (bytes = fread(output + (outsize - 1025), 1, 1024, IF)) > 0) {
        output[(outsize - 1025) + bytes] = '\0';
        outsize += 1024;
        output = EUCA_REALLOC(output, outsize, sizeof(char));
    }

    if (output == NULL) {
        LOGERROR("failed to allocate mem for output\n");
        va_end(ap);
        pclose(IF);
        return (NULL);
    }

    rc = pclose(IF);
    if (rc) {
        //! @TODO improve this hacky special case: failure to find or detach non-existing loop device is not a failure
        if (strstr(cmd, "losetup") && strstr(output, ": No such device or address")) {
            rc = 0;
        } else {
            if (log_error) {
                LOGERROR("bad return code from cmd '%s'\n", cmd);
                LOGDEBUG("%s\n", output);
            }
            EUCA_FREE(output);
        }
    }
    va_end(ap);

    return (output);
}

//!
//!
//!
//! @param[in] log_error
//! @param[in] format
//! @param[in] ...
//!
//! @return
//!
//! @pre
//!
//! @note
//!
static char *execlp_output(boolean log_error, ...)
{
    va_list ap;
    int ntokens = 0;
    char cmd[256] = "";                // for logging, OK if command gets truncated

    // run through arguments once to count them
    va_start(ap, log_error);
    {
        char *s;
        while ((s = va_arg(ap, char *)) != NULL) {
            ntokens++;
        }
    }
    va_end(ap);
    if (ntokens < 1) {
        LOGERROR("internal error: too few arguments to %s\n", __func__);
        return NULL;
    }
    // allocate an array and run through arguments again, copying them into the array
    char **argv = EUCA_ZALLOC(ntokens + 1, sizeof(char *)); // one extra for the terminating NULL
    va_start(ap, log_error);
    {
        for (int i = 0; i < ntokens; i++) {
            argv[i] = strdup(va_arg(ap, char *));

            // append tokens to 'cmd', strictly for logging purposes
            int left_in_cmd = sizeof(cmd) - strlen(cmd);
            if (left_in_cmd > 1)       // has room for at least one character and '\0'
                snprintf(cmd + strlen(cmd), left_in_cmd, "%s%s%s%s", (i > 0) ? (" ") : (""),    // add space in front all but the first argument
                         (i == 0 || argv[i][0] == '-') ? ("") : ("'"),  // add quoates around non-flags
                         argv[i], (i == 0 || argv[i][0] == '-') ? ("") : ("'"));
        }
    }
    va_end(ap);

    char *output = NULL;

    // set up a pipe for getting stdout and stderror from child process
    int filedes[2];
    if (pipe(filedes)) {
        LOGERROR("failed to create a pipe\n");
        goto free;
    }
    LOGTRACE("executing: %s\n", cmd);

    pid_t cpid = fork();
    int rc = -1;
    if (cpid == -1) {
        LOGERROR("failed to fork\n");
        close(filedes[0]);
        close(filedes[1]);
        goto free;
    } else if (cpid == 0) {            // child
        close(filedes[0]);
        if (dup2(filedes[1], STDOUT_FILENO) == -1) {
            LOGERROR("failed to dup2\n");
            exit(-1);
        }
        if (dup2(filedes[1], STDERR_FILENO) == -1) {
            LOGERROR("failed to dup2\n");
            exit(-1);
        }
        exit(execvp(argv[0], argv));
    }
    // parent reads stdout and stdin from child into a string
    close(filedes[1]);

    int outsize = OUTPUT_ALLOC_CHUNK;  // allocate in chunks of this size
    int nextchar = 0;                  // offset of the next usable char
    int bytesread;
    output = EUCA_ALLOC(outsize, sizeof(char));
    if (output) {
        output[0] = '\0';              // return an empty string if there is no output
    }

    while ((output != NULL) && (bytesread = read(filedes[0], output + nextchar, outsize - nextchar - 1)) > 0) {
        nextchar += bytesread;
        output[nextchar] = '\0';
        if (nextchar + 1 == outsize) {
            outsize += OUTPUT_ALLOC_CHUNK;
            if (outsize > MAX_OUTPUT_BYTES) {
                LOGERROR("internal error: output from command is too long\n");
                EUCA_FREE(output);
                break;
            }
            output = EUCA_REALLOC(output, outsize, sizeof(char));
        }
    }
    if (output == NULL) {
        LOGERROR("failed to allocate mem for output\n");
    }
    close(filedes[0]);

    {                                  // wait for the child to reap status
        int status;
        rc = waitpid(cpid, &status, 0);
        if (rc == -1) {
            LOGERROR("failed to wait for child process\n");
        } else if (WIFEXITED(status)) {
            rc = WEXITSTATUS(status);
            if (rc) {
                LOGERROR("child return non-zero status (%d)\n", rc);
            }
        } else {
            LOGERROR("child process did not terminate normally\n");
            rc = -1;
        }
    }

    if (rc) {
        // there were problems above
        if ((output != NULL) && strstr(cmd, "losetup") && strstr(output, ": No such device or address")) {
            rc = 0;
        } else {
            if (log_error) {
                LOGERROR("bad return code from cmd %s\n", cmd);
                LOGDEBUG("%s\n", output);
            }
            EUCA_FREE(output);         // will be set to NULL
        }
    }

free:
    for (int i = 0; i < ntokens; i++) {
        EUCA_FREE(argv[i]);
    }
    EUCA_FREE(argv);

    return output;
}

//!
//! Round up to sector size
//!
//! @param[in] bytes
//!
//! @return
//!
//! @pre
//!
//! @note
//!
//! @todo CHUCK turn this into MACRO?
//!
long long round_up_sec(long long bytes)
{
    return ((bytes % SECTOR_SIZE) ? (((bytes / SECTOR_SIZE) + 1) * SECTOR_SIZE) : bytes);
}

//!
//! Round down to sector size
//!
//! @param[in] bytes
//!
//! @return
//!
//! @pre
//!
//! @note
//!
//! @todo CHUCK turn this into MACRO?
//!
long long round_down_sec(long long bytes)
{
    return ((bytes % SECTOR_SIZE) ? (((bytes / SECTOR_SIZE)) * SECTOR_SIZE) : bytes);
}

#ifdef _UNIT_TEST
int main(int argc, char *argv[])
{
    char *output;

    logfile(NULL, EUCA_LOG_TRACE, 4);
    log_prefix_set("%T %L %t9 |");
    printf("%s: starting\n", argv[0]);
    output = execlp_output(TRUE, "/bin/echo", "hi", NULL);
    assert(output);
    printf("%s\n", output);
    free(output);
    output = execlp_output(TRUE, "echo", "there", NULL);
    assert(output);
    printf("%s\n", output);
    free(output);
    output = execlp_output(TRUE, "ls", "-l", "/", NULL);
    assert(output);
    printf("%s\n", output);
    free(output);
    output = execlp_output(TRUE, "foo", "bar", "baz", NULL);
    assert(output == NULL);
    output = execlp_output(TRUE, "sleep", "3", NULL);
    assert(output);
    assert(strlen(output) == 0);
    free(output);
    output = execlp_output(TRUE, "ls", "a-ridiculously-long-name-that-does-not-exist", NULL);
    assert(output == NULL);
    printf("%s: completed\n", argv[0]);
}

#endif // _UNIT_TEST

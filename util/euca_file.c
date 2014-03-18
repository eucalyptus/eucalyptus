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
//! @file util/euca_file.h
//! This file implements the various file and directory utility functions
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define _GNU_SOURCE
#include <string.h>                    // strlen, strcpy
#include <ctype.h>                     // isspace
#include <assert.h>
#include <stdarg.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/vfs.h>
#include <unistd.h>
#include <time.h>
#include <math.h>                      // powf
#include <fcntl.h>                     // open
#include <utime.h>                     // utime
#include <sys/wait.h>
#include <pwd.h>
#include <dirent.h>                    // opendir, etc
#include <sys/errno.h>                 // errno
#include <sys/time.h>                  // gettimeofday
#include <limits.h>
#include <sys/mman.h>                  // mmap
#include <pthread.h>

#include "eucalyptus.h"

#include <diskutil.h>
#include <vnetwork.h>

#include "misc.h"
#include "euca_string.h"
#include "euca_file.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define BUFSIZE                                  1024

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
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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
//! Remove a directory given by the psPath string pointer. If the 'force' parameter
//! is set to TRUE, the directory will be emptied and deleted. If the 'force'
//! parameter is set to FALSE then this is the equivalent of an 'rmdir()' system
//! calls which will not delete a non-empty directory.
//!
//! @param[in] psPath a constant pointer to the string containing the path to the directory to remove.
//! @param[in] force set to TRUE, will force remove a non-empty directory
//!
//! @return EUCA_OK on success or the following error code:
//!         EUCA_INVALID_ERROR if any of our pre-conditions are not met
//!         EUCA_SYSTEM_ERROR if the system fails to remove a file or directory in the process
//!         EUCA_MEMORY_ERROR if we fail to allocate memory during the process
//!
//! @see
//!
//! @pre \li psPath must not be NULL
//!      \li psPath must be a valid path to a directory
//!      \li if force is set to FALSE, the directory should be empty
//!
//! @post \li on success, the directory will be removed
//!       \li on failure, the directory may remain with some content
//!
//! @note
//!
int euca_rmdir(const char *psPath, boolean force)
{
    int result = EUCA_OK;
    char *psBuf = NULL;
    DIR *pDir = NULL;
    size_t len = 0;
    size_t pathLen = 0;
    struct dirent *pDirEnt = NULL;
    struct stat statbuf = { 0 };

    // Make sure we have a path
    if (psPath == NULL) {
        return (EUCA_INVALID_ERROR);
    }
    // If force was set, read the directory and empty it
    if (force) {
        // Retrieve the length of our directory path
        pathLen = strlen(psPath);

        // Open the directory and start scanning for items
        if ((pDir = opendir(psPath)) != NULL) {
            while ((result == EUCA_OK) && ((pDirEnt = readdir(pDir)) != NULL)) {
                // Skip the names "." and ".." as we don't want to recurse on them.
                if (!strcmp(pDirEnt->d_name, ".") || !strcmp(pDirEnt->d_name, "..")) {
                    continue;
                }

                len = pathLen + strlen(pDirEnt->d_name) + 2;
                if ((psBuf = EUCA_ALLOC(len, sizeof(char))) != NULL) {

                    snprintf(psBuf, len, "%s/%s", psPath, pDirEnt->d_name);

                    if (stat(psBuf, &statbuf) == 0) {
                        if (S_ISDIR(statbuf.st_mode)) {
                            result = euca_rmdir(psBuf, TRUE);
                        } else {
                            if (unlink(psBuf) != 0) {
                                result = EUCA_SYSTEM_ERROR;
                            }
                        }
                    }

                    EUCA_FREE(psBuf);
                } else {
                    // Memory failure
                    result = EUCA_MEMORY_ERROR;
                }
            }

            closedir(pDir);
        } else {
            // return the proper error
            if (errno == ENOTDIR)
                return (EUCA_INVALID_ERROR);
            return (EUCA_SYSTEM_ERROR);
        }
    }
    // If we were successful so far, remove the directory
    if (result == EUCA_OK) {
        if (rmdir(psPath) != 0) {
            // Set the proper return error
            if (errno == ENOTDIR)
                return (EUCA_INVALID_ERROR);
            return (EUCA_SYSTEM_ERROR);
        }
    }

    return (result);
}

//!
//! Sanitize a path string and make sure it does not contains any illegal characters
//!
//! @param[in] psPath a pointer to a string containing the path to sanitize
//!
//! @return EUCA_OK if the path is a valid string and EUCA_ERROR if its not. If NULL is passed
//!         this function will return EUCA_INVALID_ERROR.
//!
//! @note a valid path should not contain any of the following characters: "!@#$%^&*()+={}[]\|;?<>,`~'
//!
int euca_sanitize_path(const char *psPath)
{
    static char sOkChar[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890/._-:";

    // Check if we have a path provided
    if (psPath) {
        // Does the path contains only legal characters?
        if (strlen(psPath) != strspn(psPath, sOkChar))
            return (EUCA_ERROR);
        return (EUCA_OK);
    }
    return (EUCA_INVALID_ERROR);
}

//!
//! make sure 'dir' is a directory or a soft-link to one
//! and that it is readable by the current user (1 on error)
//!
//! @param[in] dir the directory name to validate
//!
//! @return 0 if dir is a directory or 1 if not
//!
//! @pre The dir field must not be NULL
//!
int check_directory(const char *dir)
{
    int rc = 0;
    DIR *d = NULL;
    char checked_dir[EUCA_MAX_PATH] = "";
    struct stat mystat = { 0 };

    if (!dir) {
        return (1);
    }

    snprintf(checked_dir, sizeof(checked_dir), "%s", dir);

    if ((rc = lstat(checked_dir, &mystat)) < 0)
        return (1);

    // if a soft link, append '/' and try lstat() again
    if (!S_ISDIR(mystat.st_mode) && S_ISLNK(mystat.st_mode)) {
        snprintf(checked_dir, sizeof(checked_dir), "%s/", dir);
        if ((rc = lstat(checked_dir, &mystat)) < 0)
            return (1);
    }

    if (!S_ISDIR(mystat.st_mode))
        return (1);

    if ((d = opendir(checked_dir)) == NULL)
        return (1);

    closedir(d);
    return (0);
}

//!
//! Check if a file is newer than the given timestamp
//!
//! @param[in] file the file name string
//! @param[in] mtime the timestamp to compare with
//!
//! @return 0 if the file is newer than timestamp or 1 if not
//!
int check_file_newer_than(const char *file, time_t mtime)
{
    int rc = 0;
    struct stat mystat = { 0 };

    if (!file) {
        return (1);
    } else if (mtime <= 0) {
        return (0);
    }

    bzero(&mystat, sizeof(struct stat));
    if ((rc = stat(file, &mystat)) != 0) {
        return (1);
    }

    if (mystat.st_mtime > mtime) {
        return (0);
    }

    return (1);
}

//!
//!
//!
//! @param[in] file
//!
//! @return 0 on success or 1 on failure
//!
int check_block(const char *file)
{
    int rc = 0;
    char *rpath = NULL;
    struct stat mystat = { 0 };

    if (!file) {
        LOGERROR("Invalid file name");
        return (1);
    }

    if ((rpath = realpath(file, NULL)) == NULL) {
        LOGERROR("No canonical file found for %s", file);
        return (1);
    }

    rc = lstat(rpath, &mystat);
    EUCA_FREE(rpath);
    if ((rc < 0) || !S_ISBLK(mystat.st_mode)) {
        LOGERROR("No stat information found for %s", rpath);
        return (1);
    }

    return (0);
}

//!
//! Checks if a file is a readable regular file.
//!
//! @param[in] file
//!
//! @return 0 if the file is readable, 1 otherwise
//!
//! @pre The file field must not be NULL.
//!
int check_file(const char *file)
{
    int rc = 0;
    struct stat mystat = { 0 };

    if (!file) {
        return (1);
    }

    rc = lstat(file, &mystat);
    if ((rc < 0) || !S_ISREG(mystat.st_mode)) {
        return (1);
    }

    return (0);
}

//!
//! Check if a path exists
//!
//! @param[in] path
//!
//! @return 0 if the path exists, othewise 1 is returned.
//!
int check_path(const char *path)
{
    int rc = 0;
    struct stat mystat = { 0 };

    if (!path) {
        return (1);
    }

    if ((rc = lstat(path, &mystat)) < 0) {
        return (1);
    }

    return (0);
}

//!
//! obtains size & available bytes of a file system that 'path' resides on
//! path may be a symbolic link, which will get resolved.
//!
//! @param[in]  path
//! @param[out] fs_bytes_size
//! @param[out] fs_bytes_available
//! @param[out] fs_id
//!
//! @return EUCA_OK on success or the following error code.
//!         \li EUCA_INVALID_ERROR
//!         \li EUCA_IO_ERROR
//!
//! @pre \li The path, fs_bytes_size, fs_bytes_available and fs_id fields must not be NULL.
//!      \li The path field must be a valid path.
//!
//! @post The fs_id, fs_bytes_size and fs_bytes_available fields are set appropriately.
//!
int statfs_path(const char *path, unsigned long long *fs_bytes_size, unsigned long long *fs_bytes_available, int *fs_id)
{
    char cpath[PATH_MAX] = { 0 };
    struct statfs fs = { 0 };

    if ((path == NULL) || (fs_bytes_size == NULL) || (fs_bytes_available == NULL) || (fs_id == NULL))
        return (EUCA_INVALID_ERROR);

    errno = 0;

    // will convert a path with symbolic links and '..' into canonical form
    if (realpath(path, cpath) == NULL) {
        LOGERROR("failed to resolve %s (%s)\n", path, strerror(errno));
        return (EUCA_IO_ERROR);
    }
    // obtain the size and ID info from the file system of the canonical path
    if (statfs(cpath, &fs) == -1) {
        LOGERROR("failed to stat %s (%s)\n", cpath, strerror(errno));
        return (EUCA_IO_ERROR);
    }

    *fs_id = hash_code_bin((char *)&fs.f_fsid, sizeof(fsid_t));
    *fs_bytes_size = (long long)fs.f_bsize * (long long)(fs.f_blocks);
    *fs_bytes_available = (long long)fs.f_bsize * (long long)(fs.f_bavail);

    LOGDEBUG("path '%s' resolved\n", path);
    LOGDEBUG("  to '%s' with ID %0x\n", cpath, *fs_id);
    LOGDEBUG("  of size %llu bytes with available %llu bytes\n", *fs_bytes_size, *fs_bytes_available);

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] fp
//!
//! @return a pointer to the file output string
//!
//! @pre \li The fp field MUST not be NULL.
//!      \li The file handles must have previously been opened.
//!
//! @post The file remains open regardless of the result.
//!
//! @note caller is responsible to free the returned memory
//!
char *fp2str(FILE * fp)
{
#define INCREMENT              512

    int buf_max = INCREMENT;
    int buf_current = 0;
    void *new_buf = NULL;
    char *last_read = NULL;
    char *buf = NULL;

    if (fp == NULL)
        return (NULL);

    do {
        // create/enlarge the buffer
        if ((new_buf = EUCA_REALLOC(buf, buf_max, sizeof(char))) == NULL) {
            // free partial buffer
            EUCA_FREE(buf);
            return (NULL);
        }

        memset((new_buf + buf_current), 0, (INCREMENT * sizeof(char)));

        buf = new_buf;
        LOGEXTREME("enlarged buf to %d\n", buf_max);

        do {                           // read in until EOF or buffer is full
            last_read = fgets(buf + buf_current, buf_max - buf_current, fp);
            if (last_read != NULL) {
                buf_current = strlen(buf);
            } else if (!feof(fp)) {
                LOGERROR("failed while reading from file handle\n");
                EUCA_FREE(buf);
                return (NULL);
            }

            LOGEXTREME("read %d characters so far (max=%d, last=%s)\n", buf_current, buf_max, last_read ? "no" : "yes");
        } while (last_read && (buf_max > (buf_current + 1)));   // +1 is needed for fgets() to put \0

        // in case it is full
        buf_max += INCREMENT;
    } while (last_read);

    return (buf);

#undef INCREMENT
}

//!
//! given a file path, prints it to stdout
//!
//! @param[in] file_name
//!
//! @return the number of bytes written to stdout
//!
int cat(const char *file_name)
{
    int fd = 0;
    int got = 0;
    int put = 0;
    char buf[BUFSIZE] = "";

    if ((fd = open(file_name, O_RDONLY)) == -1) {
        // we should print some error
        return (put);
    }

    while ((got = read(fd, buf, BUFSIZE)) > 0) {
        put += write(1, buf, got);
    }

    close(fd);
    return (put);
}

//!
//! "touch" a file, creating if necessary
//!
//! @param[in] path
//!
//! @return EUCA_OK on success or EUCA_IO_ERROR on failure
//!
int touch(const char *path)
{
    int ret = EUCA_OK;
    int fd = -1;

    if ((fd = open(path, O_WRONLY | O_CREAT | O_NONBLOCK, 0644)) >= 0) {
        close(fd);
        if (utime(path, NULL) != 0) {
            LOGERROR("failed to adjust time for %s (%s)\n", path, strerror(errno));
            ret = EUCA_IO_ERROR;
        }
    } else {
        LOGERROR("failed to create/open file %s (%s)\n", path, strerror(errno));
        ret = EUCA_IO_ERROR;
    }
    return (ret);
}

//!
//! diffs two files: 0=same, -N=different, N=error
//!
//! @param[in] path1
//! @param[in] path2
//!
//! @return 0=same, -N=different, N=error
//!
int diff(const char *path1, const char *path2)
{
    int fd1 = 0;
    int fd2 = 0;
    int read1 = 0;
    int read2 = 0;
    char buf1[BUFSIZE] = "";
    char buf2[BUFSIZE] = "";

    if ((fd1 = open(path1, O_RDONLY)) < 0) {
        LOGERROR("failed to open %s\n", path1);
    } else if ((fd2 = open(path2, O_RDONLY)) < 0) {
        LOGERROR("failed to open %s\n", path2);
        close(fd1);
    } else {
        do {
            read1 = read(fd1, buf1, BUFSIZE);
            read2 = read(fd2, buf2, BUFSIZE);
            if (read1 != read2)
                break;

            if (read1 && memcmp(buf1, buf2, read1))
                break;
        } while (read1);

        close(fd1);
        close(fd2);
        return (-(read1 + read2));     // both should be 0s if files are equal
    }
    return EUCA_ERROR;
}

//!
//! sums up sizes of files in the directory, as well as the size of the
//! directory itself; no subdirectories are allowed - if there are any, this
//! returns -1
//!
//! @param[in] path
//!
//! @return the sommation of all file size in a directory
//!
long long dir_size(const char *path)
{
    DIR *dir = NULL;
    char *name = NULL;
    char filepath[EUCA_MAX_PATH] = "";
    unsigned char type = '\0';
    long long size = 0;
    struct stat mystat = { 0 };
    struct dirent *dir_entry = NULL;

    if ((dir = opendir(path)) == NULL) {
        LOGWARN("unopeneable directory %s\n", path);
        return (-1);
    }

    if (stat(path, &mystat) < 0) {
        LOGWARN("could not stat %s\n", path);
        closedir(dir);
        return (-1);
    }

    size += ((long long)mystat.st_size);

    while ((dir_entry = readdir(dir)) != NULL) {
        name = dir_entry->d_name;
        type = dir_entry->d_type;

        if (!strcmp(".", name) || !strcmp("..", name))
            continue;

        if (DT_REG != type) {
            LOGWARN("non-regular (type=%d) file %s/%s\n", type, path, name);
            size = -1;
            break;
        }

        snprintf(filepath, EUCA_MAX_PATH, "%s/%s", path, name);
        if (stat(filepath, &mystat) < 0) {
            LOGWARN("could not stat file %s\n", filepath);
            size = -1;
            break;
        }

        size += ((long long)mystat.st_size);
    }

    closedir(dir);
    return (size);
}

//!
//!
//!
//! @param[in] path
//! @param[in] str
//!
//! @return EUCA_OK on success or EUCA_IO_ERROR on failure
//!
int write2file(const char *path, char *str)
{
    FILE *FH = NULL;

    if ((FH = fopen(path, "w")) != NULL) {
        fprintf(FH, "%s", str);
        fclose(FH);
        return (EUCA_OK);
    }

    return (EUCA_IO_ERROR);
}

//!
//!
//!
//! @param[in] path
//! @param[in] limit
//!
//! @return the result of teh file2str() call
//!
//! @see file2str()
//!
//! @note the caller must free the memory when done.
//!
char *file2strn(const char *path, const ssize_t limit)
{
    struct stat mystat = { 0 };

    if (stat(path, &mystat) < 0) {
        LOGERROR("could not stat file %s\n", path);
        return (NULL);
    }

    if (mystat.st_size > limit) {
        LOGERROR("file %s exceeds the limit (%lu) in file2strn()\n", path, limit);
        return (NULL);
    }

    return (file2str(path));
}

//!
//! read file 'path' into a new string
//!
//! @param[in] path
//!
//! @return the string content of the given file
//!
//! @note the caller must free the memory when done.
//!
char *file2str(const char *path)
{
    int fp = 0;
    int bytes = 0;
    int bytes_total = 0;
    int to_read = 0;
    char *p = NULL;
    char *content = NULL;
    off_t file_size = 0;
    struct stat mystat = { 0 };

    if (stat(path, &mystat) < 0) {
        LOGERROR("could not stat file %s\n", path);
        return (content);
    }

    file_size = mystat.st_size;

    if ((content = EUCA_ALLOC((file_size + 1), sizeof(char))) == NULL) {
        LOGERROR("out of memory reading file %s\n", path);
        return (content);
    }

    if ((fp = open(path, O_RDONLY)) < 0) {
        LOGERROR("failed to open file %s\n", path);
        EUCA_FREE(content);
        return (content);
    }

    p = content;
    to_read = (((SSIZE_MAX) < file_size) ? (SSIZE_MAX) : file_size);
    while ((bytes = read(fp, p, to_read)) > 0) {
        bytes_total += bytes;
        p += bytes;
        if (to_read > (file_size - bytes_total)) {
            to_read = file_size - bytes_total;
        }
    }

    close(fp);

    if (bytes < 0) {
        LOGERROR("failed to read file %s\n", path);
        EUCA_FREE(content);
        return (content);
    }

    *p = '\0';
    return (content);
}

//!
//!
//!
//! @param[in] file
//! @param[in] size
//! @param[in] mode
//!
//! @return the newly allocated string or NULL if any error occured
//!
//! @note caller is responsible to free the allocated memory
//!
char *file2str_seek(char *file, size_t size, int mode)
{
    int rc = 0;
    int fd = 0;
    char *ret = NULL;
    struct stat statbuf = { 0 };

    if (!file || size <= 0) {
        LOGERROR("bad input parameters\n");
        return (NULL);
    }

    if ((ret = EUCA_ZALLOC(size, sizeof(char))) == NULL) {
        LOGERROR("out of memory!\n");
        return (NULL);
    }

    if ((rc = stat(file, &statbuf)) >= 0) {
        if ((fd = open(file, O_RDONLY)) >= 0) {
            if (mode == 1) {
                if ((rc = lseek(fd, (off_t) (-1 * size), SEEK_END)) < 0) {
                    if ((rc = lseek(fd, (off_t) 0, SEEK_SET)) < 0) {
                        LOGERROR("cannot seek\n");
                        EUCA_FREE(ret);
                        close(fd);
                        return (NULL);
                    }
                }
            }

            rc = read(fd, ret, (size) - 1);
            close(fd);
        } else {
            LOGERROR("cannot open '%s' read-only\n", file);
            EUCA_FREE(ret);
            return (NULL);
        }
    } else {
        LOGERROR("cannot stat console_output file '%s'\n", file);
        EUCA_FREE(ret);
        return (NULL);
    }

    return (ret);
}

//!
//! Write a NULL-terminated string to a file according to a file
//! specification. If 'mktemp' is TRUE, 'path' is expected to be
//! not a file path, but a template for a temporary file name,
//! according to the mkstemp() specification, with six X's in it.
//! Otherwise, 'path' is the path of the file to create and write
//! the string to.
//!
//! @param[in] str String to write to a file.
//! @param[in] path Path of the file to create or mktemp spec.
//! @param[in] flags Same flags as accepted by open() call. Ignored when mktemp is TRUE.
//! @param[in] mode Permissions of the file to create.
//! @param[in] mktemp Flag requesting a temporary file.
//!
//! @return EUCA_OK on success and -1 on failure.
//!
int str2file(const char *str, char *path, int flags, mode_t mode, boolean mktemp)
{
    if (path == NULL)
        return 1;

    int fd;

    // if temporary file was requested, assume that the path is actually
    // a template for mkstemp(), with 6 X's in it, and that it is to be
    // overwritten with the actual file path
    if (mktemp) {
        fd = safe_mkstemp(path);
        if (fd < 0) {
            LOGERROR("cannot create temporary file '%s': %s\n", path, strerror(errno));
            return (-1);
        }
        if (fchmod(fd, mode)) {
            LOGERROR("failed to change permissions on '%s': %s\n", path, strerror(errno));
            close(fd);
            return (-1);
        }
    } else {
        fd = open(path, flags, mode);
        if (fd == -1) {
            LOGERROR("failed to create file '%s': %s\n", path, strerror(errno));
            return (-1);
        }
    }

    if (str) {
        int to_write = strlen(str);
        int offset = 0;
        while (to_write > 0) {
            int wrote = write(fd, str + offset, to_write);
            if (wrote == -1) {
                LOGERROR("failed to write to file '%s': %s\n", path, strerror(errno));
                close(fd);
                return (-1);
            }
            to_write -= wrote;
            offset += wrote;
        }
    }
    close(fd);

    return (EUCA_OK);
}

//!
//! copies contents of src to dst, possibly overwriting whatever is in dst
//!
//! @param[in] src
//! @param[in] dst
//!
//! @return EUCA_OK on success or EUCA_IO_ERROR on failure
//!
int copy_file(const char *src, const char *dst)
{
#define _BUFSIZE          16384

    int ret = EUCA_OK;
    int ifp = 0;
    int ofp = 0;
    char buf[_BUFSIZE] = "";
    ssize_t bytes = 0;
    struct stat mystat = { 0 };

    if (stat(src, &mystat) < 0) {
        LOGERROR("cannot stat '%s'\n", src);
        return (EUCA_IO_ERROR);
    }

    if ((ifp = open(src, O_RDONLY)) < 0) {
        LOGERROR("failed to open the input file '%s'\n", src);
        return (EUCA_IO_ERROR);
    }

    if ((ofp = open(dst, O_WRONLY | O_CREAT | O_TRUNC, 0600)) < 0) {
        LOGERROR("failed to create the ouput file '%s'\n", dst);
        close(ifp);
        return (EUCA_IO_ERROR);
    }

    while ((bytes = read(ifp, buf, _BUFSIZE)) > 0) {
        if (write(ofp, buf, bytes) < 1) {
            LOGERROR("failed while writing to '%s'\n", dst);
            ret = EUCA_IO_ERROR;
            break;
        }
    }

    if (bytes < 0) {
        LOGERROR("failed while writing to '%s'\n", dst);
        ret = EUCA_IO_ERROR;
    }

    close(ifp);
    close(ofp);

    return (ret);

#undef _BUFSIZE
}

//!
//!
//!
//! @param[in] file_path
//!
//! @return the size of the file
//!
long long file_size(const char *file_path)
{
    int err = 0;
    struct stat mystat = { 0 };

    if ((err = stat(file_path, &mystat)) < 0)
        return ((long long)err);
    return ((long long)mystat.st_size);
}

//!
//! given path=A/B/C and only A existing, create A/B and, unless
//! is_file_path==1, also create A/B/C directory
//!
//! @param[in] path
//! @param[in] is_file_path
//! @param[in] user
//! @param[in] group
//! @param[in] mode
//!
//! @return 0 = path already existed, 1 = created OK, -1 = error
//!
int ensure_directories_exist(const char *path, int is_file_path, const char *user, const char *group, mode_t mode)
{
    int ret = 0;
    int i = 0;
    int len = strlen(path);
    int try_dir = 0;
    char *path_copy = NULL;
    struct stat buf = { 0 };

    if (len > 0)
        path_copy = strdup(path);

    if (path_copy == NULL)
        return (-1);

    for (i = 0; i < len; i++) {
        try_dir = 0;

        if ((path[i] == '/') && (i > 0)) {
            // dir path, not root
            path_copy[i] = '\0';
            try_dir = 1;

        } else if ((path[i] != '/') && ((i + 1) == len)) {
            // last one
            if (!is_file_path)
                try_dir = 1;
        }

        if (try_dir) {
            if (stat(path_copy, &buf) == -1) {
                LOGINFO("creating path %s\n", path_copy);

                if (mkdir(path_copy, mode) == -1) {
                    LOGERROR("failed to create path %s: %s\n", path_copy, strerror(errno));

                    EUCA_FREE(path_copy);
                    return (-1);
                }

                ret = 1;               // we created a directory

                if (diskutil_ch(path_copy, user, group, mode) != EUCA_OK) {
                    LOGERROR("failed to change perms on path %s\n", path_copy);
                    EUCA_FREE(path_copy);
                    return (-1);
                }
            }

            path_copy[i] = '/';        // restore the slash
        }
    }

    EUCA_FREE(path_copy);
    return (ret);
}

//!
//! ensure the temp file is only readable by the user
//!
//! @param[in] template
//!
//! @return -1 on failure or the corresponding files descriptor for the temp file
//!
char *safe_mkdtemp(char *template)
{
    mode_t u = 0;
    char *ret = NULL;

    u = umask(0077);
    ret = mkdtemp(template);
    umask(u);
    return (ret);
}

//!
//! ensure the temp file is only readable by the user
//!
//! @param[in] template
//!
//! @return -1 on failure or the corresponding files descriptor for the temp file
//!
int safe_mkstemp(char *template)
{
    mode_t u = 0;
    int ret = 0;

    u = umask(0077);
    ret = mkstemp(template);
    umask(u);
    return (ret);
}

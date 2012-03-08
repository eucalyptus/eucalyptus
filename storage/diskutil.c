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

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <unistd.h>
#include <getopt.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <errno.h>
#include "misc.h" // logprintfl
#include "ipc.h" // sem
#include "diskutil.h"
#include "eucalyptus.h"
#include "pthread.h"

enum {
    CHMOD=0,
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

static char * helpers [LASTHELPER] = {
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
    "euca_mountwrap"
};

static char * helpers_path [LASTHELPER];
static char stage_files_dir [EUCA_MAX_PATH] = "";
static char * pruntf (boolean log_error, char *format, ...);
static int initialized = 0;
static sem * loop_sem = NULL; // semaphore held while attaching/detaching loopback devices
static unsigned char grub_version = 0;

// looks for file 'stage1' in dir and, if found,
// sets stage_files_dir and returns 1, else returns 0
static int try_stage_dir (const char * dir)
{
    char stage_file_path [EUCA_MAX_PATH];
    snprintf (stage_file_path, sizeof (stage_file_path), "%s/stage1", dir);
    if (check_file (stage_file_path))
        return 0;
    safe_strncpy (stage_files_dir, dir, sizeof (stage_files_dir));
    return 1;
}

int diskutil_init (int require_grub) // 0 = not required, 1 = required
{
    int ret = 0;

    if (require_grub > 0) require_grub = 1;
    if (initialized < 1+require_grub) { // if init was called without grub requirement, it will run again if grub is needed now
        bzero (helpers_path, sizeof (helpers_path));
        int missing_handlers = verify_helpers (helpers, helpers_path, LASTHELPER);
        if (helpers_path [GRUB])
            grub_version = 1;
        else
            missing_handlers--;

        if (helpers_path [GRUB_SETUP]) // don't need it, but grub-setup only exists on v2
            if (grub_version != 1)
                grub_version = 2; // prefer 1 until 2 is implemented
        else 
            missing_handlers--;

        if (require_grub && grub_version == 0) {
            logprintfl (EUCAERROR, "ERROR: cannot find either grub 1 or grub 2\n");
            ret = 1;   
        } else if (grub_version == 1) { 
            // grub 1 commands seem present, check for stage files, which we will be copying
            if (try_stage_dir ("/usr/lib/grub/x86_64-pc") ||
                try_stage_dir ("/usr/lib/grub/i386-pc") ||
                try_stage_dir ("/usr/lib/grub") ||
                try_stage_dir ("/boot/grub")) {
                logprintfl (EUCAINFO, "found grub 1 stage files in %s\n", stage_files_dir);
            } else if (require_grub) {
                logprintfl (EUCAERROR, "ERROR: failed to find grub 1 stage files (in /boot/grub et al)\n");
                ret = 1;
            }
        } else if (grub_version == 2) {
            logprintfl (EUCAINFO, "detected grub 2\n");
        }
        
        // flag missing handlers
        if (missing_handlers) {
            for (int i=0; i<LASTHELPER; i++) {
                if (helpers_path [i] == NULL && i!=GRUB && i!=GRUB_SETUP) {
                    logprintfl (EUCAERROR, "ERROR: missing a required handler: %s\n", helpers[i]);
                    ret = 1;
                }
            }
        }
        
        if (initialized < 1)
            loop_sem = sem_alloc (1, "mutex");
        initialized = 1 + require_grub;
    }
    
    return ret;
}

int diskutil_cleanup (void)
{
    for (int i=0; i<LASTHELPER; i++) {
        free (helpers_path [i]);
    }
    return 0;
}

int diskutil_ddzero (const char * path, const long long sectors, boolean zero_fill)
{
    int ret = OK;
    char * output;

    long long count = 1;
    long long seek = sectors - 1;
    if (zero_fill) {
        count = sectors;
        seek = 0;
    }

    output = pruntf (TRUE, "%s %s if=/dev/zero of=%s bs=512 seek=%lld count=%lld", helpers_path[ROOTWRAP], helpers_path[DD], path, seek, count);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: cannot create disk file %s\n", path);
        ret = ERROR;
    } else {
        free (output);
    }

    return ret;
}

int diskutil_dd (const char * in, const char * out, const int bs, const long long count)
{
    int ret = OK;
    char * output;

    logprintfl (EUCAINFO, "{%u} copying data from '%s'\n", (unsigned int)pthread_self(), in);
    logprintfl (EUCAINFO, "{%u}                to '%s' (blocks=%lld)\n", (unsigned int)pthread_self(), out, count);
    output = pruntf (TRUE, "%s %s if=%s of=%s bs=%d count=%lld", helpers_path[ROOTWRAP], helpers_path[DD], in, out, bs, count);
    if (!output) {
        logprintfl (EUCAERROR, "{%u} error: cannot copy '%s'\n", (unsigned int)pthread_self(), in);
        logprintfl (EUCAERROR, "{%u}                 to '%s'\n", (unsigned int)pthread_self(), out);
    } else {
        free (output);
    }

    return ret;
}

int diskutil_dd2 (const char * in, const char * out, const int bs, const long long count, const long long seek, const long long skip)
{
    int ret = OK;
    char * output;

    logprintfl (EUCAINFO, "{%u} copying data from '%s'\n", (unsigned int)pthread_self(), in);
    logprintfl (EUCAINFO, "{%u}                to '%s'\n", (unsigned int)pthread_self(), out);
    logprintfl (EUCAINFO, "{%u}                of %lld blocks (bs=%d), seeking %lld, skipping %lld\n", (unsigned int)pthread_self(), count, bs, seek, skip);
    output = pruntf (TRUE, "%s %s if=%s of=%s bs=%d count=%lld seek=%lld skip=%lld conv=notrunc,fsync", helpers_path[ROOTWRAP], helpers_path[DD], in, out, bs, count, seek, skip);
    if (!output) {
        logprintfl (EUCAERROR, "{%u} error: cannot copy '%s'\n", (unsigned int)pthread_self(), in);
        logprintfl (EUCAERROR, "{%u}                 to '%s'\n", (unsigned int)pthread_self(), out);
        ret = ERROR;
    } else {
        free (output);
    }

    return ret;
}

int diskutil_mbr (const char * path, const char * type)
{
    int ret = OK;
    char * output;

    output = pruntf (TRUE, "LD_PRELOAD='' %s %s --script %s mklabel %s", helpers_path[ROOTWRAP], helpers_path[PARTED], path, type);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: cannot create an MBR\n");
        ret = ERROR;
    } else {
        free (output);
    }

    return ret;
}

int diskutil_part (const char * path, char * part_type, const char * fs_type, const long long first_sector, const long long last_sector)
{
    int ret = OK;
    char * output;

    output = pruntf (TRUE, "LD_PRELOAD='' %s %s --script %s mkpart %s %s %llds %llds", helpers_path[ROOTWRAP], helpers_path[PARTED], path, part_type, (fs_type)?(fs_type):(""), first_sector, last_sector);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: cannot add a partition\n");
        ret = ERROR;
    } else {
        free (output);
    }

    return ret;
}

// expose the loop semaphore so others (e.g., instance startup code) 
// can avoid races with 'losetup' that we've seen on Xen
sem * diskutil_get_loop_sem (void)
{
    return loop_sem;
}

#define LOOP_RETRIES 9

// TODO: since 'losetup' truncates paths in its output, this
// check is not perfect. It may approve loopback devices
// that are actually pointing at a different path.
int diskutil_loop_check (const char * path, const char * lodev)
{
    int ret = 0;

    char * output = pruntf (TRUE, "%s %s %s", helpers_path[ROOTWRAP], helpers_path[LOSETUP], lodev);
    if (output==NULL)
        return 1;
    
    // output is expected to look like:
    // /dev/loop4: [0801]:5509589 (/var/lib/eucalyptus/volumes/v*)
    char * oparen = strchr (output, '(');
    char * cparen = strchr (output, ')');
    if (oparen==NULL || cparen==NULL) { // no parenthesis => unexpected `losetup` output
        ret = 1;
    } else if ((cparen - oparen) < 3) { // strange paren arrangement => unexpected
        ret = 1;
    } else { // extract just the path, possibly truncated, from inside the parens
        oparen++;
        cparen--;
        if (* cparen == '*') // handle truncated paths, identified with an asterisk
            cparen--;
        * cparen = '\0'; // truncate ')' or '*)'
        if (strstr (path, oparen) == NULL) { // see if path is in the blobstore
            ret = 1;
        }
    }
    free (output);
    
    return ret;
}

int diskutil_loop (const char * path, const long long offset, char * lodev, int lodev_size)
{
    int found = 0;
    int done = 0;
    int ret = OK;
    char * output;

    // we retry because we cannot atomically obtain a free loopback
    // device on all distros (some versions of 'losetup' allow a file
    // argument with '-f' options, but some do not)
    for (int i=0; i<LOOP_RETRIES; i++) {
        sem_p (loop_sem);
        output = pruntf (TRUE, "%s %s -f", helpers_path[ROOTWRAP], helpers_path[LOSETUP]);
        sem_v (loop_sem);
        if (output==NULL) // there was a problem
            break;
        if (strstr (output, "/dev/loop")) {
            strncpy (lodev, output, lodev_size);
            char * ptr = strrchr (lodev, '\n');
            if (ptr) {
                *ptr = '\0';
                found = 1;
            }
        }
        free (output);

        if (found) {
            boolean do_log = ((i+1)==LOOP_RETRIES); // log error on last try only
            logprintfl (EUCADEBUG, "{%u} attaching file %s\n", (unsigned int)pthread_self(), path);
            logprintfl (EUCADEBUG, "{%u}             to %s at offset %lld\n", (unsigned int)pthread_self(), lodev, offset);
            sem_p (loop_sem);
            output = pruntf (do_log, "%s %s -o %lld %s %s", helpers_path[ROOTWRAP], helpers_path[LOSETUP], offset, lodev, path);
            sem_v (loop_sem);
            if (output==NULL) {
                logprintfl (EUCADEBUG, "{%u} cannot attach to loop device %s (will retry)\n", (unsigned int)pthread_self(), lodev);
            } else {
                free (output);
                done = 1;
                break;
            }
        }
        
        sleep (1);
        found = 0;
    }
    if (!done) {
        logprintfl (EUCAINFO, "{%u} error: cannot find free loop device or attach to one\n", (unsigned int)pthread_self());
        ret = ERROR;
    }
    
    return ret;
}

int diskutil_unloop (const char * lodev)
{
    int ret = OK;
    char * output;
    int retried = 0;

    logprintfl (EUCADEBUG, "{%u} detaching from loop device %s\n", (unsigned int)pthread_self(), lodev);

    // we retry because we have seen spurious errors from 'losetup -d' on Xen:
    //     ioctl: LOOP_CLR_FD: Device or resource bus
    for (int i=0; i<LOOP_RETRIES; i++) {
        boolean do_log = ((i+1)==LOOP_RETRIES); // log error on last try only
        sem_p (loop_sem);
        output = pruntf (do_log, "%s %s -d %s", helpers_path[ROOTWRAP], helpers_path[LOSETUP], lodev);
        sem_v (loop_sem);
        if (!output) {           
            ret = ERROR;
        } else {
            ret = OK;
            free (output);
            break;
        }
        logprintfl (EUCADEBUG, "{%u} cannot detach loop device %s (will retry)\n", (unsigned int)pthread_self(), lodev);
        retried++;
        sleep (1);
    }
    if (ret == ERROR) {
        logprintfl (EUCAWARN, "{%u} error: cannot detach loop device\n", (unsigned int)pthread_self());
    } else if (retried) {
        logprintfl (EUCAINFO, "{%u} succeeded to detach %s after %d retries\n", (unsigned int)pthread_self(), lodev, retried);
    }

    return ret;
}

int diskutil_mkswap (const char * lodev, const long long size_bytes)
{
    int ret = OK;
    char * output;

    output = pruntf (TRUE, "%s %s %s %lld", helpers_path[ROOTWRAP], helpers_path[MKSWAP], lodev, size_bytes/1024);
    if (!output) {
        logprintfl (EUCAINFO, "{%u} error: cannot format partition on '%s' as swap\n", (unsigned int)pthread_self(), lodev);
        ret = ERROR;
    } else {
        free (output);
    }

    return ret;
}

int diskutil_mkfs (const char * lodev, const long long size_bytes)
{
    int ret = OK;
    char * output;
    int block_size = 4096;

    output = pruntf (TRUE, "%s %s -b %d %s %lld", helpers_path[ROOTWRAP], helpers_path[MKEXT3], block_size, lodev, size_bytes/block_size);
    if (!output) {
        logprintfl (EUCAINFO, "{%u} error: cannot format partition on '%s' as ext3\n", (unsigned int)pthread_self(), lodev);
        ret = ERROR;
    } else {
        free (output);
    }

    return ret;
}

int diskutil_tune (const char * lodev)
{
    int ret = OK;
    char * output;

    sem_p (loop_sem);
    output = pruntf (TRUE, "%s %s %s -c 0 -i 0", helpers_path[ROOTWRAP], helpers_path[TUNE2FS], lodev);
    sem_v (loop_sem);
    if (!output) {
        logprintfl (EUCAINFO, "{%u} error: cannot tune file system on '%s'\n", (unsigned int)pthread_self(), lodev);
        ret = ERROR;
    } else {
        free (output);
    }

    return ret;
}

int diskutil_sectors (const char * path, const int part, long long * first, long long * last)
{
    int ret = ERROR;
    char * output;
    * first = 0L;
    * last = 0L;

    output = pruntf (TRUE, "%s %s", helpers_path[FILECMD], path);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: failed to extract partition information for '%s'\n", path);
    } else {
        // parse the output, such as:
        // NAME: x86 boot sector;
        // partition 1: ID=0x83, starthead 1, startsector 63, 32769 sectors;
        // partition 2: ID=0x83, starthead 2, startsector 32832, 32769 sectors;
        // partition 3: ID=0x82, starthead 2, startsector 65601, 81 sectors
        boolean found = FALSE;
        char * section = strtok (output, ";"); // split by semicolon
        for (int p = 0; section != NULL; p++) {
            section = strtok (NULL, ";");
            if (section && p == part) {
                found = TRUE;
                break;
            }
        }
        if (found) {
            char * ss = strstr (section, "startsector");
            if (ss) {
                ss += strlen ("startsector ");
                char * comma = strstr (ss, ", ");
                if (comma) {
                    * comma = '\0';
                    comma += strlen (", ");
                    char * end = strstr (comma, " sectors");
                    if (end) {
                        * end = '\0';
                        * first = atoll (ss);
                        * last = * first + atoll (comma) - 1L;
                    }
                }
            }
        }

        free (output);
    }

    if ( * last > 0 )
        ret = OK;

    return ret;
}

int diskutil_mount (const char * dev, const char * mnt_pt)
{
    int ret = OK;
    char * output;

    sem_p (loop_sem);
    output = pruntf (TRUE, "%s %s mount %s %s", helpers_path[ROOTWRAP], helpers_path[MOUNTWRAP], dev, mnt_pt);
    sem_v (loop_sem);
    if (!output) {
        logprintfl (EUCAINFO, "{%u} error: cannot mount device '%s' on '%s'\n", (unsigned int)pthread_self(), dev, mnt_pt);
        ret = ERROR;
    } else {
        free (output);
    }

    return ret;
}

int diskutil_umount (const char * dev)
{
    int ret = OK;
    char * output;

    sem_p (loop_sem);
    output = pruntf (TRUE, "%s %s umount %s", helpers_path[ROOTWRAP], helpers_path[MOUNTWRAP], dev);
    sem_v (loop_sem);
    if (!output) {
        logprintfl (EUCAINFO, "{%u} error: cannot unmount device '%s'\n", (unsigned int)pthread_self(), dev);
        ret = ERROR;
    } else {
        free (output);
    }

    return ret;
}

int diskutil_write2file (const char * file, const char * str)
{
    int ret = OK;
    char tmpfile [] = "/tmp/euca-temp-XXXXXX";
    int fd = safe_mkstemp (tmpfile);
    if (fd<0) {
        logprintfl (EUCAERROR, "{%u} error: failed to create temporary directory\n", (unsigned int)pthread_self());
        unlink(tmpfile);
        return ERROR;
    }
    int size = strlen (str);
    if (write (fd, str, size) != size) {
        logprintfl (EUCAERROR, "{%u} error: failed to create temporary directory\n", (unsigned int)pthread_self());
        ret = ERROR;
    } else {
        if (diskutil_cp (tmpfile, file) != OK) {
            logprintfl (EUCAERROR, "{%u} error: failed to copy temp file to destination (%s)\n", (unsigned int)pthread_self(), file);
            ret = ERROR;
        }
    }
    close (fd);

    unlink(tmpfile);
    return ret;
}

// diskutil_grub combines functionalities of diskutil_grub_files and diskutil_grub_mbr,
// performing them one after another
int diskutil_grub (const char * path, const char * mnt_pt, const int part, const char * kernel, const char * ramdisk)
{
    int ret = diskutil_grub_files (mnt_pt, part, kernel, ramdisk);
    if (ret!=OK) return ret;
    ret = diskutil_grub_mbr (path, part);
    return ret;
}

int diskutil_grub_files (const char * mnt_pt, const int part, const char * kernel, const char * ramdisk)
{
    int ret = OK;
    char * output = NULL;
    char * kfile = "euca-vmlinuz";
    char * rfile = "euca-initrd";

    logprintfl (EUCAINFO, "{%u} installing kernel and ramdisk\n", (unsigned int)pthread_self());
    output = pruntf (TRUE, "%s %s -p %s/boot/grub/", helpers_path[ROOTWRAP], helpers_path[MKDIR], mnt_pt);
    if (!output) {
        logprintfl (EUCAINFO, "{%u} error: failed to create grub directory\n", (unsigned int)pthread_self());
        return ERROR;
    }
    free (output);

    if (grub_version==1) {
        output = pruntf (TRUE, "%s %s %s/*stage* %s/boot/grub", helpers_path[ROOTWRAP], helpers_path[CP], stage_files_dir, mnt_pt);
        if (!output) {
            logprintfl (EUCAINFO, "{%u} error: failed to copy stage files into grub directory\n", (unsigned int)pthread_self());
            return ERROR;
        }
        free (output);
    }

    output = pruntf (TRUE, "%s %s %s %s/boot/%s", helpers_path[ROOTWRAP], helpers_path[CP], kernel, mnt_pt, kfile);
    if (!output) {
        logprintfl (EUCAINFO, "{%u} error: failed to copy the kernel to boot directory\n", (unsigned int)pthread_self());
        ret = ERROR;
        goto cleanup;
    }
    free (output);

    if (ramdisk) {
        output = pruntf (TRUE, "%s %s %s %s/boot/%s", helpers_path[ROOTWRAP], helpers_path[CP], ramdisk, mnt_pt, rfile);
        if (!output) {
            logprintfl (EUCAINFO, "{%u} error: failed to copy the ramdisk to boot directory\n", (unsigned int)pthread_self());
            ret = ERROR;
            goto cleanup;
        }
        free (output);
    }

    char buf [1024];
    char grub_conf_path [EUCA_MAX_PATH];
    if (grub_version==1) {
        char menu_lst_path [EUCA_MAX_PATH];
        snprintf (menu_lst_path, sizeof (menu_lst_path),   "%s/boot/grub/menu.lst", mnt_pt);
        snprintf (grub_conf_path, sizeof (grub_conf_path), "%s/boot/grub/grub.conf", mnt_pt);
    
        snprintf (buf, sizeof (buf), "default=0\n"
                  "timeout=2\n\n"
                  "title TheOS\n"
                  "root (hd0,%d)\n"
                  "kernel /boot/%s root=/dev/sda1 ro\n", part, kfile);
        if (ramdisk) {
            char buf2 [1024];
            snprintf (buf2, sizeof (buf2), "initrd /boot/%s\n", rfile);
            strncat (buf, buf2, sizeof (buf) - 1);
        }    
        if (diskutil_write2file (menu_lst_path, buf)!=OK) {
            ret = ERROR;
            goto cleanup;
        }
        
    } else if (grub_version==2) {
        snprintf (grub_conf_path, sizeof (grub_conf_path), "%s/boot/grub/grub.cfg", mnt_pt);
        char initrd [1024] = "";
        if (ramdisk) {
            snprintf (initrd, sizeof (initrd), "  initrd /boot/%s\n", rfile);
        }    
        snprintf (buf, sizeof (buf), "set default=0\n"
                  "set timeout=2\n"
                  "insmod part_msdos\n"
                  "insmod ext2\n"
                  "set root='(hd0,msdos%d)'\n"
                  "menuentry 'TheOS' --class os {\n"
                  "  linux /boot/%s root=/dev/sda1 ro\n"
                  "%s"
                  "}\n", part, kfile, initrd);
    }
    if (diskutil_write2file (grub_conf_path, buf)!=OK) {
        ret = ERROR;
        goto cleanup;
    }
    
 cleanup:        
    return ret;
}

int diskutil_grub_mbr (const char * path, const int part)
{
    if (grub_version != 1) {
        logprintfl (EUCAERROR, "{%u} grub 2 is not supported\n", (unsigned int)pthread_self());
        return ERROR;
    }
    return diskutil_grub2_mbr (path, part, NULL);
}

int diskutil_grub2_mbr (const char * path, const int part, const char * mnt_pt)
{
    char cmd [1024];
    int rc = 1;

    if (grub_version!=1 && grub_version!=2) {
        logprintfl (EUCAERROR, "{%u} internal error: invocation of diskutil_grub2_mbr without grub found\n", (unsigned int)pthread_self());
        return ERROR;
    } else if (mnt_pt==NULL && grub_version!=1) {
        logprintfl (EUCAERROR, "{%u} internal error: invocation of diskutil_grub2_mbr with grub 1\n", (unsigned int)pthread_self());
        return ERROR;
    }
    
    logprintfl (EUCAINFO, "{%u} installing grub in MBR\n", (unsigned int)pthread_self());
    if (grub_version==1) {
        char tmp_file [EUCA_MAX_PATH] = "/tmp/euca-temp-XXXXXX";
        int tfd = safe_mkstemp (tmp_file);
        if (tfd < 0) {
            logprintfl (EUCAINFO, "{%u} error: mkstemp() failed: %s\n", (unsigned int)pthread_self(), strerror (errno));
            return ERROR;
        }

        // create a soft link of the first partition's device mapper entry in the
        // form that grub is looking for (not DISKp1 but just DISK1)
        char *output = pruntf (TRUE, "%s /bin/ln -s %sp1 %s1", helpers_path[ROOTWRAP], path, path);
        if(!output) {
            logprintfl (EUCAINFO, "{%u} warning: failed to create partition device soft-link", (unsigned int)pthread_self());
        } else {
            free(output);
        }

        // we now invoke grub through euca_rootwrap because it may need to operate on
        // devices that are owned by root (e.g. /dev/mapper/euca-dsk-7E4E131B-fca1d769p1)
        snprintf(cmd, sizeof (cmd), "%s %s --batch >%s 2>&1", helpers_path[ROOTWRAP], helpers_path[GRUB], tmp_file);
        logprintfl (EUCADEBUG, "{%u} running %s\n", (unsigned int)pthread_self(), cmd);
        errno = 0;
        FILE * fp = popen (cmd, "w");
        if (fp!=NULL) {
            char s [EUCA_MAX_PATH];
#define _PR fprintf (fp, "%s", s); // logprintfl (EUCADEBUG, "\t%s", s)
            snprintf (s, sizeof (s), "device (hd0) %s\n", path); _PR;
            snprintf (s, sizeof (s), "root (hd0,%d)\n", part);   _PR;
            snprintf (s, sizeof (s), "setup (hd0)\n");           _PR;
            snprintf (s, sizeof (s), "quit\n");                  _PR;
            rc = pclose (fp); // base success on exit code of grub
        }
        if (rc) {
            logprintfl (EUCAERROR, "{%u} error: failed to run grub 1 on disk '%s': %s\n", (unsigned int)pthread_self(), path, strerror (errno));
        } else {
            int read_bytes;
            char buf [1024];
            bzero (buf, sizeof (buf));
            boolean saw_done = FALSE;
            do {
                // read in a line
                int bytes_read = 0;
                while ((sizeof (buf) - 2 - bytes_read)>0 // there is space in buffer for \n and \0
                       && ((read_bytes = read (tfd, buf + bytes_read, 1)) > 0))
                    if (buf [bytes_read++] == '\n')
                        break;
                if (read_bytes < 0) // possibly truncated output, ensure there is newline
                    buf [bytes_read++] = '\n';
                buf [bytes_read] = '\0';
                logprintfl (EUCADEBUG, "\t%s", buf); // log grub 1 prompts and our inputs
                if (strstr (buf, "Done.")) // this indicates that grub 1 succeeded (the message has been there since 2000)
                    saw_done = TRUE;
            } while (read_bytes>0);
            close (tfd);

            if (saw_done==FALSE) {
                logprintfl (EUCAERROR, "{%u} error: failed to run grub 1 on disk '%s'\n", (unsigned int)pthread_self(), path);
                rc = 1;
            } else {
                rc = 0;
            }
        }
        // try to remove the partition device soft link created above
        output = pruntf (TRUE, "%s /bin/rm %s1", helpers_path[ROOTWRAP], path);
        if(!output) {
            logprintfl (EUCAINFO, "{%u} warning: failed to remove partition device soft-link", (unsigned int)pthread_self());
        } else {
            free(output);
        }

    } else if (grub_version==2) {
        char * output = pruntf (TRUE, "%s %s --modules='part_msdos ext2' --root-directory=%s %s", helpers_path[ROOTWRAP], helpers_path[GRUB_INSTALL], mnt_pt, path);
        if (!output) {
            logprintfl (EUCAINFO, "{%u} error: failed to install grub 2 on disk '%s' mounted on '%s'\n", (unsigned int)pthread_self(), path, mnt_pt);
        } else {
            free (output);
            rc = 0;
        }
    }
    
    if (rc==0) 
        return OK;    
    else
        return ERROR;
}

int diskutil_ch (const char * path, const char * user, const char * group, const int perms)
{
    int ret = OK;
    char * output;

    logprintfl (EUCAINFO, "{%u} ch(own|mod) '%s' %s.%s %o\n", 
                (unsigned int)pthread_self(), 
                path,
                user?user:"*", 
                group?group:"*", 
                perms);
    if (user) {
        output = pruntf (TRUE, "%s %s %s %s", helpers_path[ROOTWRAP], helpers_path[CHOWN], user, path);
        if (!output) {
            return ERROR;
        }
        free (output);
    }

    if (group) {
        output = pruntf (TRUE, "%s %s :%s %s", helpers_path[ROOTWRAP], helpers_path[CHOWN], group, path);
        if (!output) {
            return ERROR;
        }
        free (output);
    }

    if (perms>0) {
        output = pruntf (TRUE, "%s %s 0%o %s", helpers_path[ROOTWRAP], helpers_path[CHMOD], perms, path);
        if (!output) {
            return ERROR;
        }
        free (output);
    }

    return OK;
}

int diskutil_mkdir (const char * path)
{
    char * output;

    output = pruntf (TRUE, "%s %s -p %s", helpers_path[ROOTWRAP], helpers_path[MKDIR], path);
    if (!output) {
        return ERROR;
    }
    free (output);

    return OK;
}

int diskutil_cp (const char * from, const char * to)
{
    char * output;

    output = pruntf (TRUE, "%s %s %s %s", helpers_path[ROOTWRAP], helpers_path[CP], from, to);
    if (!output) {
        return ERROR;
    }
    free (output);

    return OK;
}

static char * pruntf (boolean log_error, char *format, ...)
{
    va_list ap;
    FILE *IF=NULL;
    char cmd[1024], *ptr;
    size_t bytes=0;
    int outsize=1025, rc;
    char *output=NULL;

    va_start(ap, format);
    vsnprintf(cmd, 1024, format, ap);

    strncat(cmd, " 2>&1", 1024 - 1);
    output = NULL;

    IF=popen(cmd, "r");
    if (!IF) {
      logprintfl (EUCAERROR, "{%u} error: cannot popen() cmd '%s' for read\n", (unsigned int)pthread_self(), cmd);
      va_end(ap);
      return(NULL);
    }

    output = malloc(sizeof(char) * outsize);
    if(output) {
        output[0]='\0'; // make sure we return an empty string if there is no output
    }

    while(output != NULL && (bytes = fread(output+(outsize-1025), 1, 1024, IF)) > 0) {
        output[(outsize-1025)+bytes] = '\0';
        outsize += 1024;
        output = realloc(output, outsize);
    }

    if (output == NULL) {
        logprintfl (EUCAERROR, "error: failed to allocate mem for output\n");
        va_end(ap);
        pclose(IF);
        return(NULL);
    }

    rc = pclose(IF);
    if (rc) {
        // TODO: improve this hacky special case: failure to find or detach non-existing loop device is not a failure
        if (strstr (cmd, "losetup") && strstr (output, ": No such device or address")) {
            rc = 0;
        } else {
            if (log_error) {
                logprintfl (EUCAERROR, "{%u} error: bad return code from cmd '%s'\n", (unsigned int)pthread_self(), cmd);
                logprintfl (EUCADEBUG, "%s\n", output);
            }
            if (output) free (output);
            output = NULL;
        }
    }
    va_end(ap);

    return (output);
}

// round up or down to sector size
long long round_up_sec   (long long bytes) { return ((bytes % SECTOR_SIZE) ? (((bytes / SECTOR_SIZE) + 1) * SECTOR_SIZE) : bytes); }
long long round_down_sec (long long bytes) { return ((bytes % SECTOR_SIZE) ? (((bytes / SECTOR_SIZE))     * SECTOR_SIZE) : bytes); }

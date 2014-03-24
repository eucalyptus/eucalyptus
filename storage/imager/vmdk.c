// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2014 Eucalyptus Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 ************************************************************************/

//!
//! @file storage/imager/vmdk.c
//!
//! Support for VMware disks and for uploading/cloning them to vSphere/ESX hosts
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
#include <sys/stat.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>
#include <string.h>                    // bzero, memcpy, strlen
#include <ctype.h>                     // toupper
#include <errno.h>
#include <sys/ioctl.h>
#include <linux/fs.h>                  // BLKGETSIZE64
#include <vixDiskLib.h>

#include <eucalyptus.h>
#include <diskutil.h>
#include <misc.h>
#include <http.h>
#include "vmdk.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define VIXDISKLIB_VERSION_MAJOR                    1
#define VIXDISKLIB_VERSION_MINOR                    0
#define BUFSIZE                                  1024
#define STDIN                                       0 // @TODO what's the standard constant?
#define STDOUT                                      1 // @TODO what's the standard constant?
#define MIN_VDDK_SIZE_BYTES                   1049600 // 1MB is minimum, apparently

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

typedef struct _vix_session {
    VixDiskLibConnection cnxRemote;
    VixDiskLibConnection cnxLocal;
    VixDiskLibHandle diskHandle;
    VixDiskLibHandle diskHandleLocal;
} vix_session;

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

boolean initialized = FALSE;
VixError vixError = VIX_OK;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int completed = -1;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static char clone_progress_func(void *data, int percentCompleted);
static void log_func(const char *fmt, va_list args);
static char *make_vmx_spec(const img_loc * loc, char *buf, const size_t buf_size);
static char *make_vmdk_spec(const img_loc * loc, char *buf, const int buf_size);
static int open_connection(vix_session * s, const img_spec * spec);
static int open_disk(vix_session * s, const img_spec * spec);
static int open_disk_local(vix_session * s, const char *path);
static void cleanup(vix_session * s);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define CHECK_ERROR()                                          \
{                                                              \
    if (VIX_FAILED((vixError))) {                              \
        char *__msg = VixDiskLib_GetErrorText(vixError, NULL); \
        LOGFATAL("%s:%d: %s\n", __FILE__, __LINE__, __msg);    \
        VixDiskLib_FreeErrorText(__msg);                       \
        goto out;                                              \
    }                                                          \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//!
//!
//! @param[in] data
//! @param[in] percentCompleted
//!
//! @return Always return true
//!
//! @see euca_strncpy(), euca_strdupcat()
//!
//! @pre List of pre-conditions
//!
//! @post List of post conditions
//!
//! @note Important note
//!
static char clone_progress_func(void *data, int percentCompleted)
{
    if (percentCompleted != completed) {
        LOGINFO("cloning %d percent completed\n", percentCompleted);
        if (percentCompleted == 100) {
            LOGINFO("stay tuned for zero-filling...\n");
        }
        completed = percentCompleted;
    }
    return TRUE;
}

//!
//!
//!
//! @param[in] fmt
//! @param[in] args
//!
//! @pre
//!
//! @post
//!
static void log_func(const char *fmt, va_list args)
{
#define _SIZE      160

    int i = 0;
    char msg[_SIZE] = "";

    vsnprintf(msg, _SIZE, fmt, args);

    // add newline if it is not there (thanks, vddk)
    i = strlen(msg) - 1;
    if (msg[i] != '\n') {
        if (i < (_SIZE - 2)) {
            i++;
            msg[i + 1] = '\0';
        }
        msg[i] = '\n';
    }

    LOGINFO("VDDK: %s", msg);

#undef _SIZE
}

//!
//!
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @pre
//!
//! @post
//!
int vmdk_init(void)
{
    int ret = EUCA_ERROR;
    VixDiskLibConnection cnxLocal = NULL;
    VixDiskLibConnectParams cnxLocalParams = { 0 };

    // init the lib once
    if (!initialized) {
        // log, warn, panic (can be NULL)
        vixError = VixDiskLib_Init(VIXDISKLIB_VERSION_MAJOR, VIXDISKLIB_VERSION_MINOR, log_func, log_func, log_func, NULL);
        CHECK_ERROR();

        // connect to local whatever it is
        vixError = VixDiskLib_Connect(&cnxLocalParams, &cnxLocal);
        CHECK_ERROR();

        if (diskutil_init(TRUE)) {     // imager may need GRUB
            LOGERROR("failed to initialize helpers\n");
            return EUCA_ERROR;
        }

        initialized = TRUE;
        ret = EUCA_OK;
    }

out:
    return ret;
}

//!
//! UNUSED: generating a VMDK metadata file
//!
//! @param[in] disk_path
//! @param[in] vmdk_path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int gen_vmdk(const char *disk_path, const char *vmdk_path)
{
    int total_size = 0;
    int fd = 0;
    char desc[1024] = "";
    struct stat mystat = { 0 };
    static const char desc_template[] =
        "# Disk DescriptorFile\n"
        "version=1\n"
        "CID=%x\n"
        "parentCID=ffffffff\n"
        "createType=\"%s\"\n"
        "\n"
        "# Extent description\n"
        "RW %d %s \"%s\"\n"
        "\n"
        "# The Disk Data Base \n"
        "#DDB\n" "\n" "ddb.virtualHWVersion = \"%d\"\n" "ddb.geometry.cylinders = \"%d\"\n" "ddb.geometry.heads = \"16\"\n"
        "ddb.geometry.sectors = \"63\"\n" "ddb.adapterType = \"%s\"\n";

    if (stat(disk_path, &mystat) < 0) {
        LOGERROR("failed to stat file %s\n", disk_path);
        return EUCA_ERROR;
    }

    total_size = (int)(mystat.st_size / 512);   //! file size in blocks (@TODO do we need to round up?)

    if ((fd = open(vmdk_path, O_WRONLY | O_CREAT | O_TRUNC, 0644)) < 0) {
        LOGFATAL("failed to create %s\n", vmdk_path);
        return EUCA_ERROR;
    }

    snprintf(desc, sizeof(desc), desc_template, (unsigned int)time(NULL), "vmfs",   // can also be "monolithicSparse"
             total_size,               // in blocks
             "VMFS",                   // can also be SPARSE
             "disk-flat.vmdk",         // backing store file's name
             4,                        // qemu-img can also produce 6, vmkfstools seems to be on 7 now
             (int)(total_size / (int64_t) (63 * 16)), "lsilogic"    // can also be "ide" and "buslogic"
        );

    if (write(fd, desc, strlen(desc)) != strlen(desc)) {
        close(fd);
        return (1);
    }
    close(fd);
    return 0;
}

//!
//!
//!
//! @param[in] loc
//! @param[in] buf
//! @param[in] buf_size
//!
//! @return An alias pointer to buf on success or NULL on failure
//!
//! @pre
//!
//! @post
//!
static char *make_vmx_spec(const img_loc * loc, char *buf, const size_t buf_size)
{
    if (strlen(loc->vsphere_vmx_ds) < 1 || strlen(loc->vsphere_vmx_path) < 1)
        return NULL;

    if (strlen(loc->vsphere_dc) > 0) {
        snprintf(buf, buf_size, "%s?dcPath=%s&dsName=%s", loc->vsphere_vmx_path, loc->vsphere_dc, loc->vsphere_vmx_ds); // unencoded spaces are OK
    } else {
        snprintf(buf, buf_size, "[%s] %s", loc->vsphere_vmx_ds, loc->vsphere_vmx_path);
    }

    return buf;
}

//!
//!
//!
//! @param[in] loc
//! @param[in] buf
//! @param[in] buf_size
//!
//! @return An alias pointer to buf on success or NULL on failure
//!
//! @pre
//!
//! @post
//!
static char *make_vmdk_spec(const img_loc * loc, char *buf, const int buf_size)
{
    if (strlen(loc->path) < 1 || strlen(loc->vsphere_ds) < 1)
        return NULL;

    snprintf(buf, buf_size, "[%s] %s", loc->vsphere_ds, loc->path);
    return buf;
}

//!
//!
//!
//! @param[in] s
//! @param[in] spec
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
static int open_connection(vix_session * s, const img_spec * spec)
{
    int ret = EUCA_ERROR;
    char vmxSpecBuf[512] = "";
    const img_loc *loc = NULL;
    const img_creds *creds = NULL;
    VixDiskLibConnectParams cnxRemoteParams = { 0 };
    VixDiskLibConnectParams cnxLocalParams = { 0 };

    if (spec) {
        loc = &(spec->location);
        creds = &(loc->creds);
    }

    bzero(s, sizeof(vix_session));
    bzero(&cnxRemoteParams, sizeof(cnxRemoteParams));

    // init the lib once
    if (!initialized) {
        vixError = VixDiskLib_Init(VIXDISKLIB_VERSION_MAJOR, VIXDISKLIB_VERSION_MINOR, log_func, log_func, log_func, NULL);
        CHECK_ERROR();
        initialized = TRUE;
    }
    // connect to remote vSphere endpoint
    if (spec) {
        cnxRemoteParams.vmxSpec = make_vmx_spec(loc, vmxSpecBuf, sizeof(vmxSpecBuf));
        cnxRemoteParams.serverName = strdup(loc->host); // strdup() for const
        cnxRemoteParams.credType = VIXDISKLIB_CRED_UID;
        cnxRemoteParams.creds.uid.userName = strdup(creds->login);  // strdup() for const
        cnxRemoteParams.creds.uid.password = strdup(creds->password);   // strdup() for const
        cnxRemoteParams.port = loc->port ? loc->port : 902; // 443? 902? 903? 8333?
        vixError = VixDiskLib_Connect(&cnxRemoteParams, &(s->cnxRemote));
        CHECK_ERROR();
    }
    // connect to local whatever it is
    vixError = VixDiskLib_Connect(&cnxLocalParams, &(s->cnxLocal));
    CHECK_ERROR();

    ret = EUCA_OK;

out:
    EUCA_FREE(cnxRemoteParams.serverName);
    EUCA_FREE(cnxRemoteParams.creds.uid.userName);
    EUCA_FREE(cnxRemoteParams.creds.uid.password);
    return ret;
}

//!
//!
//!
//! @param[in] s
//! @param[in] spec
//!
//! @return EUCA_OK on success or EUCA_ERROR on error.
//!
//! @pre
//!
//! @post
//!
static int open_disk(vix_session * s, const img_spec * spec)
{
    char vmdkPathBuf[512] = "";
    const img_loc *loc = &(spec->location);

    if (s->cnxRemote == NULL)
        return EUCA_ERROR;

    // open the disk
    if (make_vmdk_spec(loc, vmdkPathBuf, sizeof(vmdkPathBuf)) == NULL) {
        LOGERROR("insufficient information for the VMDK\n");
        return EUCA_ERROR;
    }

    vixError = VixDiskLib_Open(s->cnxRemote, vmdkPathBuf, 0, &(s->diskHandle));
    CHECK_ERROR();
    return EUCA_OK;

out:
    return EUCA_ERROR;
}

//!
//!
//!
//! @param[in] s
//! @param[in] path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
static int open_disk_local(vix_session * s, const char *path)
{
    if (s->cnxLocal == NULL)
        return EUCA_ERROR;

    vixError = VixDiskLib_Open(s->cnxLocal, path, 0, &(s->diskHandleLocal));
    CHECK_ERROR();
    return EUCA_OK;

out:
    return EUCA_ERROR;
}

//!
//!
//!
//! @param[in] s
//!
//! @pre
//!
//! @post
//!
static void cleanup(vix_session * s)
{
    if (s->diskHandle != NULL)
        VixDiskLib_Close(s->diskHandle);

    if (s->diskHandleLocal != NULL)
        VixDiskLib_Close(s->diskHandleLocal);

    if (s->cnxRemote != NULL)
        VixDiskLib_Disconnect(s->cnxRemote);

    if (s->cnxLocal != NULL)
        VixDiskLib_Disconnect(s->cnxLocal);
}

//!
//! Returns file size, in bytes, of a remote VMDK
//!
//! @param[in] spec
//!
//! @return The file size, in bytes, of a remote VMDK
//!
//! @pre
//!
//! @post
//!
s64 vmdk_get_size(const img_spec * spec)
{
    s64 ret = -1L;
    vix_session s = { 0 };
    VixError vixError = VIX_OK;
    VixDiskLibInfo *info = NULL;

    if (open_connection(&s, spec) != EUCA_OK)
        goto out;

    if (open_disk(&s, spec) != EUCA_OK)
        goto out;

    // find out the size of the disk
    vixError = VixDiskLib_GetInfo(s.diskHandle, &info);
    CHECK_ERROR();

    ret = (s64) info->capacity * VIXDISKLIB_SECTOR_SIZE;
    VixDiskLib_FreeInfo(info);

out:
    cleanup(&s);
    return ret;
}

//!
//! Converts a local raw disk file or stream into remove VMDK
//!
//! @param[in] disk_path
//! @param[in] spec
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int vmdk_convert_to_remote(const char *disk_path, const img_spec * spec, long long start_sector, long long end_sector)
{
    int ret = EUCA_ERROR;
    s64 vddk_blocks = 0;
    s64 vddk_bytes = 0;
    s64 true_bytes = 0;
    s64 zero_sectors = 0;
    s64 bytes_written = 0;
    int fp = -1;
    VixError vixError = VIX_OK;
    vix_session s = { 0 };
    VixDiskLibInfo *info = NULL;

    LOGINFO("intput file %s:\n", disk_path);
    if (strcmp(disk_path, "-") == 0) {
        fp = STDIN;
    } else {
        s64 fs_bytes = 0;
        s64 fs_blocks = 0;
        int fs_blocksize = 0;
        struct stat mystat = { 0 };

        if ((fp = open(disk_path, 0)) < 0) {
            LOGERROR("failed to open input path: %s\n", strerror(errno));
            return ret;
        }
        if (S_ISBLK(mystat.st_mode)) {
            if (ioctl(fp, BLKGETSIZE64, &true_bytes) != 0) {
                LOGERROR("input: failed to ioctl() device %s\n", disk_path);
                close(fp);
                return ret;
            }
            fs_bytes = true_bytes;
            fs_blocks = true_bytes / (s64) mystat.st_blksize;
            fs_blocksize = (int)mystat.st_blksize;
        } else if (S_ISREG(mystat.st_mode)) {
            true_bytes = (s64) mystat.st_size;
            fs_bytes = (s64) (mystat.st_blocks * mystat.st_blksize);
            fs_blocks = (s64) mystat.st_blocks;
            fs_blocksize = (int)mystat.st_blksize;
        } else {
            LOGERROR("input: invalid path (neither regular file nor block device): %s\n", disk_path);
            close(fp);
            return ret;
        }
        LOGINFO("\ttrue bytes=%-12ld\n", true_bytes);
        LOGINFO("\t  fs bytes=%-12ld blocks=%-9ld (blksize=%d)\n", fs_bytes, fs_blocks, fs_blocksize);
    }

    // in case VDDK sectors are bigger than disk units
    vddk_blocks = (true_bytes / VIXDISKLIB_SECTOR_SIZE) + ((true_bytes % VIXDISKLIB_SECTOR_SIZE > 0) ? 1 : 0);
    vddk_bytes = vddk_blocks * VIXDISKLIB_SECTOR_SIZE;
    LOGINFO("\tvddk bytes=%-12ld blocks=%-9ld (blksize=%d)\n", vddk_bytes, vddk_blocks, (int)VIXDISKLIB_SECTOR_SIZE);

    if (vddk_blocks > 0 && vddk_blocks < (MIN_VDDK_SIZE_BYTES / VIXDISKLIB_SECTOR_SIZE)) {
        LOGERROR("input file is too small to be converted into VMDK (%d bytes minimum)\n", MIN_VDDK_SIZE_BYTES);
        close(fp);
        return ret;
    }

    if (open_connection(&s, spec) != EUCA_OK)
        goto out;

    if (open_disk(&s, spec) != EUCA_OK)
        goto out;

    // find out the size of the disk
    vixError = VixDiskLib_GetInfo(s.diskHandle, &info);
    CHECK_ERROR();

  if (vddk_blocks > 0) { // i.e., we know the size of input
    if (vddk_blocks > info->capacity) {
	LOGDEBUG("capacity of the disk on vsphere (%ld) is smaller than input (%ld)\n", (s64)info->capacity, vddk_blocks);
    }
    if (start_sector < 0 || start_sector > (vddk_blocks-1)) {
        LOGERROR("start sector '%lld' is out of range of disk '%s', giving up", start_sector, disk_path);
        goto out;
    }
    if (end_sector < 0 || end_sector > (vddk_blocks-1)) {
        LOGERROR("end sector '%lld' is out of range of disk '%s', giving up", end_sector, disk_path);
        goto out;
    }
    if (start_sector > end_sector) {
        LOGERROR("start sector '%lld' is greater than end sector '%lld', giving up", start_sector, end_sector);
        goto out;
    }
    if (end_sector == 0) { // 0 is special value meaning the whole disk
        end_sector = vddk_blocks - 1;
    }
  }
    int bytes_read = 1;
    time_t before = time(NULL);
    for (long seen_partial = FALSE, zero_sectors = 0, sector = start_sector; 
	bytes_read > 0 // have not hit an error or end of file
	&& (end_sector == 0 || sector <= end_sector); // are within limits requested
        sector++) {
        u8 buf[VIXDISKLIB_SECTOR_SIZE] = { 0 };
        if ((bytes_read = read(fp, buf, VIXDISKLIB_SECTOR_SIZE)) != VIXDISKLIB_SECTOR_SIZE) {
            if (bytes_read > 0) {      // partially written sector
                if (seen_partial) {
                    LOGERROR("second partially written sector, giving up\n");
                } else {
                    LOGWARN("partially written sector in input file\n");
                    seen_partial = TRUE;
                }

                // fill with zeros
                for (int i = bytes_read; i < VIXDISKLIB_SECTOR_SIZE; i++) {
                    buf[i] = 0;
                }
            } else if (bytes_read == 0) {
                if (vddk_bytes == 0) { // input size is unknown
                    LOGINFO("encountered EOF on input\n");
                }
                break;
            } else {
                LOGERROR("failed to read input file\n");
                break;
            }
        }
        // scan for all zeros
        boolean all_zeros = TRUE;
        for (int i = 0; i < VIXDISKLIB_SECTOR_SIZE; i++) {
            if (buf[i] != 0) {
                all_zeros = FALSE;
                break;
            }
        }

	if ((bytes_written / VIXDISKLIB_SECTOR_SIZE) % 1000 == 0) {
        LOGDEBUG("writing sector %ld, zero_sectors=%ld, start=%lld, end=%lld, bytes_written=%ld\n",
                 sector, zero_sectors, start_sector, end_sector, bytes_written);
        }
        if (!all_zeros) {
            vixError = VixDiskLib_Write(s.diskHandle, sector, 1, buf);
            CHECK_ERROR();
        } else {
            zero_sectors++;
        }
        bytes_written += VIXDISKLIB_SECTOR_SIZE;
    }

    time_t after = time(NULL);
    LOGINFO("copy of %ldMB-disk took %d seconds (zero sectors=%ld/%ld)\n", bytes_written / 1000000, (int)(after - before), zero_sectors, bytes_written / VIXDISKLIB_SECTOR_SIZE);

    ret = EUCA_OK;


out:
    close(fp);
    cleanup(&s);
    return ret;
}

//!
//! Converts a remote VMDK to a local plain disk file
//!
//! @param[in] spec
//! @param[in] disk_path
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int vmdk_convert_from_remote(const img_spec * spec, const char *disk_path, long long start_sector, long long end_sector)
{
    u8 buf[VIXDISKLIB_SECTOR_SIZE] = { 0 };
    int i = 0;
    int fp = -1;
    int ret = EUCA_ERROR;
    int percent = 0;
    s64 zero_sectors = 0;
    s64 vddk_blocks = 0;
    s64 vddk_bytes = 0;
    s64 bytes_scanned = 0;
    s64 bytes_zero = 0;
    off_t bytes = 0;
    time_t now = 0;
    time_t after = 0;
    time_t before = 0;
    time_t timestamp = 0;
    boolean all_zeros = TRUE;
    struct stat mystat = { 0 };
    VixError vixError = VIX_OK;
    vix_session s = { 0 };
    VixDiskLibInfo *info = NULL;
    VixDiskLibSectorType sector = 0;

    if (strcmp(disk_path, "-") == 0) {
        fp = STDOUT; // stdout
    } else {
        if (stat(disk_path, &mystat) == 0) {
            LOGERROR("output file '%s' exists", disk_path);
            return EUCA_ERROR;
        }
        //! @TODO are these perms ok?
        if ((fp = open(disk_path, O_CREAT | O_WRONLY, 0600)) < 0) {
            LOGERROR("failed to create the output file '%s'", disk_path);
            return EUCA_ERROR;
        }
    }

    if (open_connection(&s, spec) != EUCA_OK)
        goto out;

    if (open_disk(&s, spec) != EUCA_OK)
        goto out;

    // find out the size of the disk
    vixError = VixDiskLib_GetInfo(s.diskHandle, &info);
    CHECK_ERROR();

    // read the disk, sector at a time
    before = time(NULL);
    zero_sectors = 0;
    vddk_blocks = info->capacity;
    vddk_bytes = vddk_blocks * VIXDISKLIB_SECTOR_SIZE;
    bytes_scanned = 0;
    bytes_zero = 0;
    VixDiskLib_FreeInfo(info);

    if (start_sector < 0 || start_sector > (vddk_blocks-1)) {
        LOGERROR("start sector '%lld' is out of range of disk '%s', giving up", start_sector, disk_path);
        goto out;
    }
    if (end_sector < 0 || end_sector > (vddk_blocks-1)) {
        LOGERROR("end sector '%lld' is out of range of disk '%s', giving up", end_sector, disk_path);
        goto out;
    }
    if (start_sector > end_sector) {
        LOGERROR("start sector '%lld' is greater than end sector '%lld', giving up", start_sector, end_sector);
        goto out;
    }
    if (end_sector == 0) { // 0 is special value meaning the whole disk
        end_sector = vddk_blocks - 1;
    }

    for (sector = start_sector; sector <= end_sector; sector++) {
        vixError = VixDiskLib_Read(s.diskHandle, sector, 1, buf);
        CHECK_ERROR();

        // scan for all zeros unless this is the last sector
        if (sector == vddk_blocks - 1) {
            all_zeros = FALSE;
        } else {
            for (all_zeros = TRUE, i = 0; i < VIXDISKLIB_SECTOR_SIZE; i++) {
                if (buf[i] != 0) {
                    all_zeros = FALSE;
                    break;
                }
            }
        }

        bytes_scanned += VIXDISKLIB_SECTOR_SIZE;

        if (all_zeros && (fp != STDOUT)) {
            if (lseek(fp, VIXDISKLIB_SECTOR_SIZE, SEEK_CUR) < 0) {
                LOGERROR("failed to seek by a sector in '%s', giving up", disk_path);
                goto out;
            }

            bytes_zero += VIXDISKLIB_SECTOR_SIZE;
            zero_sectors++;
        } else {
            if ((bytes = write(fp, buf, VIXDISKLIB_SECTOR_SIZE)) != VIXDISKLIB_SECTOR_SIZE) {
                LOGERROR("failed to write a sector in '%s', giving up", disk_path);
                goto out;
            }
        }

        // progress printer
        now = time(NULL);
        if ((now - timestamp) > 10) {
            timestamp = now;
            percent = (int)((bytes_scanned * 100) / vddk_bytes);
            LOGDEBUG("transfer progress %ld/%ld bytes (%d%%) zeros=%ld\n", bytes_scanned, vddk_bytes, percent, bytes_zero);
        }
    }

    after = time(NULL);
    ret = EUCA_OK;

    LOGINFO("download of %ldMB-disk took %d seconds (zero sectors=%ld/%ld)\n", vddk_bytes / 1000000, (int)(after - before), zero_sectors, vddk_blocks);

out:
    close(fp);
    cleanup(&s);
    return ret;
}

//!
//! Clone a local VMDK to a remote datastore
//!
//! @param[in] vmdk_path
//! @param[in] spec
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @pre
//!
//! @post
//!
int vmdk_clone(const char *vmdk_path, const img_spec * spec)
{
    int ret = EUCA_ERROR;
    char vmdkPathBuf[512] = "";
    time_t after = 0;
    time_t before = 0;
    vix_session s = { 0 };
    const img_loc *loc = &(spec->location);
    VixDiskLibCreateParams cloneParams = { 0 };

    if (open_connection(&s, spec) != EUCA_OK)
        goto out;

    if (make_vmdk_spec(loc, vmdkPathBuf, sizeof(vmdkPathBuf)) == NULL) {
        LOGERROR("insufficient information for the VMDK\n");
        goto out;
    }

    LOGINFO("cloning local disk %s to %s\n", vmdk_path, vmdkPathBuf);
    cloneParams.adapterType = VIXDISKLIB_ADAPTER_SCSI_BUSLOGIC;
    cloneParams.diskType = VIXDISKLIB_DISK_STREAM_OPTIMIZED;
    cloneParams.hwVersion = VIXDISKLIB_HWVERSION_WORKSTATION_5;

    before = time(NULL);
    vixError = VixDiskLib_Clone(s.cnxRemote, vmdkPathBuf, s.cnxLocal, vmdk_path, &cloneParams, clone_progress_func, NULL, TRUE);
    CHECK_ERROR();

    after = time(NULL);
    LOGINFO("cloning of disk took %d seconds\n", (int)(after - before));
    ret = EUCA_OK;

out:
    cleanup(&s);
    return ret;
}

//!
//! Convert a local disk file to a local VMDK
//!
//! @param[in] disk_path
//! @param[in] vmdk_path
//! @param[in] overwrite
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @pre
//!
//! @post
//!
int vmdk_convert_local(const char *disk_path, const char *vmdk_path, boolean overwrite)
{
    u8 buf[VIXDISKLIB_SECTOR_SIZE] = { 0 };
    int i = 0;
    int fp = -1;
    int ret = EUCA_ERROR;
    int bytes_read = 0;
    int fs_blocksize = 0;
    s64 true_bytes = 0;
    s64 fs_bytes = 0;
    s64 fs_blocks = 0;
    s64 vddk_blocks = 0;
    s64 vddk_bytes = 0;
    s64 zero_sectors = 0;
    time_t after = 0;
    time_t before = 0;
    boolean all_zeros = TRUE;
    boolean seen_partial = FALSE;
    vix_session s = { 0 };
    struct stat mystat = { 0 };
    VixDiskLibSectorType sector = 0;
    VixDiskLibCreateParams createParams = { 0 };

    if (stat(vmdk_path, &mystat) == 0) {
        if (overwrite) {
            unlink(vmdk_path);         //! @TODO is this OK to do with blobstore 'blocks' files?
        } else {
            LOGERROR("output: file exists\n");
            return ret;
        }
    }

    if (stat(disk_path, &mystat) != 0) {
        LOGERROR("input: path does not exist: %s\n", disk_path);
        return ret;
    }
    // Although we try to ensure that blobs created by Eucalyptus are owned by 'eucalyptus',
    // sometimes ownership changes to 'root' as a result of operations that we perform.
    // Here we try to ensure we can open the file by forcing the ownership right before use.
    // This is a conservative measure, in case sleep(1) in vbr.c is not long enough.
    if (diskutil_ch(disk_path, EUCALYPTUS_ADMIN, NULL, 0) != EUCA_OK) {
        LOGINFO("failed to change user for '%s' to '%s'\n", disk_path, EUCALYPTUS_ADMIN);
        return ret;
    }

    if ((fp = open(disk_path, 0)) < 0) {
        LOGERROR("failed to open input path: %s\n", strerror(errno));
        return ret;
    }

    if (S_ISBLK(mystat.st_mode)) {
        if (ioctl(fp, BLKGETSIZE64, &true_bytes) != 0) {
            LOGERROR("input: failed to ioctl() device %s\n", disk_path);
            close(fp);
            return ret;
        }

        fs_bytes = true_bytes;
        fs_blocks = true_bytes / (s64) mystat.st_blksize;
        fs_blocksize = (int)mystat.st_blksize;
    } else if (S_ISREG(mystat.st_mode)) {
        true_bytes = (s64) mystat.st_size;
        fs_bytes = (s64) (mystat.st_blocks * mystat.st_blksize);
        fs_blocks = (s64) mystat.st_blocks;
        fs_blocksize = (int)mystat.st_blksize;
    } else {
        LOGERROR("input: invalid path (neither regular file nor block device): %s\n", disk_path);
        close(fp);
        return ret;
    }

    // in case VDDK sectors are bigger than disk units
    vddk_blocks = (true_bytes / VIXDISKLIB_SECTOR_SIZE) + ((true_bytes % VIXDISKLIB_SECTOR_SIZE > 0) ? 1 : 0);
    vddk_bytes = vddk_blocks * VIXDISKLIB_SECTOR_SIZE;

    LOGINFO("intput file %s:\n", disk_path);
    LOGINFO("\ttrue bytes=%-12ld\n", true_bytes);
    LOGINFO("\t  fs bytes=%-12ld blocks=%-9ld (blksize=%d)\n", fs_bytes, fs_blocks, fs_blocksize);
    LOGINFO("\tvddk bytes=%-12ld blocks=%-9ld (blksize=%d)\n", vddk_bytes, vddk_blocks, (int)VIXDISKLIB_SECTOR_SIZE);

    if (vddk_blocks < 2050) {
        LOGERROR("input file is too small to be converted into VMDK (1049600 bytes minimum)\n");
        close(fp);
        return ret;
    }

    if (open_connection(&s, NULL) != EUCA_OK)
        goto out;

    createParams.adapterType = VIXDISKLIB_ADAPTER_SCSI_BUSLOGIC;
    createParams.capacity = vddk_blocks;
    createParams.diskType = VIXDISKLIB_DISK_MONOLITHIC_SPARSE;
    createParams.hwVersion = VIXDISKLIB_HWVERSION_WORKSTATION_5;

    vixError = VixDiskLib_Create(s.cnxLocal, vmdk_path, &createParams, NULL, NULL);
    CHECK_ERROR();

    LOGINFO("created disk %s, copying data to it\n", vmdk_path);
    if (open_disk_local(&s, vmdk_path) != EUCA_OK)
        goto out;

    before = time(NULL);
    for (seen_partial = FALSE, zero_sectors = 0, sector = 0; sector < vddk_blocks; sector++) {
        if ((bytes_read = read(fp, buf, VIXDISKLIB_SECTOR_SIZE)) != VIXDISKLIB_SECTOR_SIZE) {
            if (bytes_read > 0) {      // partially written sector
                if (seen_partial) {
                    LOGERROR("second partially written sector, giving up\n");
                } else {
                    LOGWARN("partially written sector in input file\n");
                    seen_partial = TRUE;
                }

                // fill with zeros
                for (i = bytes_read; i < VIXDISKLIB_SECTOR_SIZE; i++) {
                    buf[i] = 0;
                }
            } else {
                LOGERROR("failed to read input file\n");
            }
        }
        // scan for all zeros
        for (all_zeros = TRUE, i = 0; i < VIXDISKLIB_SECTOR_SIZE; i++) {
            if (buf[i] != 0) {
                all_zeros = FALSE;
                break;
            }
        }

        if (!all_zeros) {
            vixError = VixDiskLib_Write(s.diskHandleLocal, sector, 1, buf);
            CHECK_ERROR();
        } else {
            zero_sectors++;
        }
    }

    after = time(NULL);
    LOGINFO("copy of %ldMB-disk took %d seconds (zero sectors=%ld/%ld)\n", vddk_bytes / 1000000, (int)(after - before), zero_sectors, vddk_blocks);

    ret = EUCA_OK;

out:
    close(fp);
    cleanup(&s);
    return ret;
}

//!
//! Helper used by vSphere-related commands to break up path into vds (datacenter)
//! and vpath (path on datacenter)
//!
//! @param[in] path
//! @param[in] vds
//! @param[in] vds_size
//! @param[in] vpath
//! @param[in] vpath_size
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure
//!
//! @pre
//!
//! @post
//!
int parse_vsphere_path(const char *path, char *vds, const int vds_size, char *vpath, const int vpath_size)
{
    int len = 0;
    char *br1 = strstr(path, "[");
    char *br2 = strstr(path, "]");

    if (br1 == NULL || br2 == NULL || br1 >= br2 || (br2 - path + 2) > strlen(path))
        return EUCA_ERROR;

    len = br2 - br1 - 1;
    if (len > vds_size)
        len = vds_size;

    strncpy(vds, br1 + 1, len);

    if (*(br2 + 1) == ' ') {
        // skip the space after [datastore], if any
        br2++;
    }

    strncpy(vpath, br2 + 1, vpath_size);
    return EUCA_OK;
}

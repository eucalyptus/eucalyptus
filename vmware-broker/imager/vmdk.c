// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
 * support for VMware disks and for uploading/cloning them to vSphere/ESX hosts
 */

#define _FILE_OFFSET_BITS 64 // so large-file support works on 32-bit systems

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <time.h>
#include <string.h> // bzero, memcpy, strlen
#include <ctype.h> // toupper
#include <errno.h>
#include <sys/ioctl.h>
#include <linux/fs.h> // BLKGETSIZE64
#include <vixDiskLib.h>

#include "eucalyptus.h"
#include "diskutil.h"
#include "misc.h"
#include "http.h"
#include "vmdk.h"

#define VIXDISKLIB_VERSION_MAJOR 1
#define VIXDISKLIB_VERSION_MINOR 0
#define BUFSIZE 1024

typedef struct _vix_session {
    VixDiskLibConnection cnxRemote;
    VixDiskLibConnection cnxLocal;
    VixDiskLibHandle diskHandle;
    VixDiskLibHandle diskHandleLocal;
} vix_session;

boolean initialized = 0;

VixError vixError;
#define CHECK_ERROR                                                     \
    if (VIX_FAILED((vixError))) {                                       \
        char * msg = VixDiskLib_GetErrorText (vixError, NULL);          \
        logprintfl (EUCAFATAL, "%s:%d: %s\n", __FILE__, __LINE__, msg); \
        VixDiskLib_FreeErrorText (msg);                                 \
        goto out;                                                       \
    }

static int completed = -1;

static char clone_progress_func(void * data, int percentCompleted)
{
    if (percentCompleted!=completed) {
        logprintfl (EUCAINFO, "cloning %d percent completed\n", percentCompleted);
        if (percentCompleted==100) {
            logprintfl (EUCAINFO, "stay tuned for zero-filling...\n");
        }
        completed = percentCompleted;
    }
    return TRUE;
}

static void log_func (const char *fmt, va_list args)
{
#   define _SIZE 160
    char msg [_SIZE];
    vsnprintf (msg, _SIZE, fmt, args);

    // add newline if it is not there (thanks, vddk)
    int i = strlen (msg) - 1;
    if (msg [i]!='\n') {
        if (i<(_SIZE-2)) {
            i++;
            msg [i+1] = '\0';
        }
        msg [i] = '\n';
    }

    logprintfl (EUCAINFO, "VDDK: %s", msg);
}

int vmdk_init (void)
{
    int ret = ERROR;

    if (!initialized) { // init the lib once
        vixError = VixDiskLib_Init (VIXDISKLIB_VERSION_MAJOR,
                                    VIXDISKLIB_VERSION_MINOR,
                                    log_func, log_func, log_func, // log, warn, panic (can be NULL)
                                    NULL); // libdir
        CHECK_ERROR;

        // connect to local whatever it is
        VixDiskLibConnection cnxLocal;
        VixDiskLibConnectParams cnxLocalParams = { 0 };
        vixError = VixDiskLib_Connect (&cnxLocalParams, &cnxLocal);
        CHECK_ERROR;

        if (diskutil_init(TRUE)) { // euca_imager may need GRUB
            logprintfl (EUCAERROR, "failed to initialize helpers\n");
            return ERROR;
        }

        initialized = 1;
        ret = OK;
    }
 out:
    return ret;
}

// UNUSED: generating a VMDK metadata file
int gen_vmdk (const char * disk_path, const char * vmdk_path)
{
    struct stat mystat;
    if (stat (disk_path, &mystat) < 0 ) {
        logprintfl (EUCAERROR, "failed to stat file %s\n", disk_path);
        return 1;
    }
    int total_size = (int)(mystat.st_size/512); // file size in blocks (TODO: do we need to round up?)

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
        "#DDB\n"
        "\n"
        "ddb.virtualHWVersion = \"%d\"\n"
        "ddb.geometry.cylinders = \"%d\"\n"
        "ddb.geometry.heads = \"16\"\n"
        "ddb.geometry.sectors = \"63\"\n"
        "ddb.adapterType = \"%s\"\n";
    char desc[1024];
    int fd = open (vmdk_path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd<0) {
        logprintfl (EUCAFATAL, "failed to create %s\n", vmdk_path);
        return 1;
    }
    snprintf(desc, sizeof(desc), desc_template,
             (unsigned int)time(NULL),
             "vmfs", // can also be "monolithicSparse"
             total_size, // in blocks
             "VMFS", // can also be SPARSE
             "disk-flat.vmdk", // backing store file's name
             4, // qemu-img can also produce 6, vmkfstools seems to be on 7 now
             (int)(total_size / (int64_t)(63 * 16)),
             "lsilogic" // can also be "ide" and "buslogic"
             );
    write(fd, desc, strlen(desc));
    close(fd);

    return 0;
}

static char * make_vmx_spec (const img_loc * loc, char * buf, const int buf_size)
{
    if (strlen(loc->vsphere_vmx_ds)<1 || strlen(loc->vsphere_vmx_path)<1)
        return NULL;
    if (strlen(loc->vsphere_dc)>0) {
        snprintf (buf, buf_size, "%s?dcPath=%s&dsName=%s", loc->vsphere_vmx_path, loc->vsphere_dc, loc->vsphere_vmx_ds); // unencoded spaces are OK
    } else {
        snprintf (buf, buf_size, "[%s] %s", loc->vsphere_vmx_ds, loc->vsphere_vmx_path);
    }

    return buf;
}

static char * make_vmdk_spec (const img_loc * loc, char * buf, const int buf_size)
{
    if (strlen(loc->path)<1 || strlen(loc->vsphere_ds)<1)
        return NULL;
    snprintf (buf, buf_size, "[%s] %s", loc->vsphere_ds, loc->path);

    return buf;
}

static int open_connection (vix_session * s, const img_spec * spec)
{
    const img_loc * loc = NULL;
    const img_creds * creds = NULL;
    VixDiskLibConnectParams cnxRemoteParams;
    int ret = ERROR;

    if (spec) {
        loc = &(spec->location);
        creds = &(loc->creds);
    }
    bzero (s, sizeof (vix_session));
    bzero (&cnxRemoteParams, sizeof (cnxRemoteParams));

    if (!initialized) { // init the lib once
        vixError = VixDiskLib_Init (VIXDISKLIB_VERSION_MAJOR,
                                    VIXDISKLIB_VERSION_MINOR,
                                    log_func, log_func, log_func, // log, warn, panic (can be NULL)
                                    NULL); // libdir
        CHECK_ERROR;
        initialized = 1;
    }

    // connect to remote vSphere endpoint
    if (spec) {
        char vmxSpecBuf [512];
        cnxRemoteParams.vmxSpec = make_vmx_spec (loc, vmxSpecBuf, sizeof (vmxSpecBuf));
        cnxRemoteParams.serverName = strdup(loc->host); // strdup() for const
        cnxRemoteParams.credType = VIXDISKLIB_CRED_UID;
        cnxRemoteParams.creds.uid.userName = strdup(creds->login); // strdup() for const
        cnxRemoteParams.creds.uid.password = strdup(creds->password); // strdup() for const
        cnxRemoteParams.port = loc->port ? loc->port : 902; // 443? 902? 903? 8333?
        vixError = VixDiskLib_Connect (&cnxRemoteParams, &(s->cnxRemote));
        CHECK_ERROR;
    }

    // connect to local whatever it is
    VixDiskLibConnectParams cnxLocalParams = { 0 };
    vixError = VixDiskLib_Connect (&cnxLocalParams, &(s->cnxLocal));
    CHECK_ERROR;

    ret = OK;

 out:
    if (cnxRemoteParams.serverName!=NULL)         free (cnxRemoteParams.serverName);
    if (cnxRemoteParams.creds.uid.userName!=NULL) free (cnxRemoteParams.creds.uid.userName);
    if (cnxRemoteParams.creds.uid.password!=NULL) free (cnxRemoteParams.creds.uid.password);

    return ret;
}

static int open_disk (vix_session * s, const img_spec * spec)
{
    const img_loc * loc = &(spec->location);

    if (s->cnxRemote==NULL)
        return ERROR;

    // open the disk
    char vmdkPathBuf [512];
    if (make_vmdk_spec (loc, vmdkPathBuf, sizeof(vmdkPathBuf))==NULL) {
        logprintfl (EUCAERROR, "insufficient information for the VMDK\n");
        goto out;
    }
    vixError = VixDiskLib_Open(s->cnxRemote,
                               vmdkPathBuf,
                               0,
                               &(s->diskHandle));
    CHECK_ERROR;

    return OK;
 out:
    return ERROR;
}

static int open_disk_local (vix_session * s, const char * path)
{
    if (s->cnxLocal==NULL)
        return ERROR;

    vixError = VixDiskLib_Open(s->cnxLocal,
                               path,
                               0,
                               &(s->diskHandleLocal));
    CHECK_ERROR;

    return OK;
 out:
    return ERROR;
}

static void cleanup (vix_session * s)
{
    if (s->diskHandle!=NULL) VixDiskLib_Close (s->diskHandle);
    if (s->diskHandleLocal!=NULL) VixDiskLib_Close (s->diskHandleLocal);
    if (s->cnxRemote!=NULL) VixDiskLib_Disconnect(s->cnxRemote);
    if (s->cnxLocal!=NULL) VixDiskLib_Disconnect(s->cnxLocal);
}

// returns file size, in bytes, of a remote VMDK

long long vmdk_get_size (const img_spec * spec)
{
    const img_loc * loc = &(spec->location);
    long long ret = -1L;
    vix_session s;

    if (open_connection (&s, spec)!=OK)
        goto out;

    if (open_disk (&s, spec)!=OK)
        goto out;

    // find out the size of the disk
    VixDiskLibInfo * info;
    VixError vixError = VixDiskLib_GetInfo (s.diskHandle, &info);
    CHECK_ERROR;

    ret = (long long)info->capacity * VIXDISKLIB_SECTOR_SIZE;
    VixDiskLib_FreeInfo (info);

 out:
    cleanup (&s);

    return ret;
}

// converts a remote VMDK to a local plain disk file

int vmdk_convert_remote (const img_spec * spec, const char * disk_path)
{
    int ret = ERROR;

    struct stat mystat;
    if (stat (disk_path, &mystat) == 0) {
        logprintfl (EUCAERROR, "output file '%s' exists", disk_path);
        return ERROR;
    }

    int fp = open (disk_path, O_CREAT | O_WRONLY, 0600); // TODO: are these perms ok?
    if (fp<0) {
        logprintfl (EUCAERROR, "failed to create the output file '%s'", disk_path);
        return ERROR;
    }

    vix_session s;

    if (open_connection (&s, spec)!=OK)
        goto out;

    if (open_disk (&s, spec)!=OK)
        goto out;

    // find out the size of the disk
    VixDiskLibInfo * info;
    VixError vixError = VixDiskLib_GetInfo (s.diskHandle, &info);
    CHECK_ERROR;

    // read the disk, sector at a time
    time_t before = time (NULL);
    uint8 buf [VIXDISKLIB_SECTOR_SIZE];
    long long zero_sectors = 0;
    long long vddk_blocks = info->capacity;
    long long vddk_bytes = vddk_blocks * VIXDISKLIB_SECTOR_SIZE;
    long long bytes_scanned = 0;
    long long bytes_zero = 0;
    VixDiskLib_FreeInfo (info);

    time_t timestamp = 0;
    for (VixDiskLibSectorType sector = 0; sector < vddk_blocks; sector++) {
        vixError = VixDiskLib_Read (s.diskHandle, sector, 1, buf);
        CHECK_ERROR;

        // scan for all zeros
        char all_zeros = 1;
        for (int i=0; i<VIXDISKLIB_SECTOR_SIZE; i++) {
            if ( buf[i] != 0 ) {
                all_zeros = 0;
                break;
            }
        }
        bytes_scanned += VIXDISKLIB_SECTOR_SIZE;

        if (all_zeros) {
            if (lseek (fp, VIXDISKLIB_SECTOR_SIZE, SEEK_CUR)<0) {
                logprintfl (EUCAERROR, "failed to seek by a sector in '%s', giving up", disk_path);
                goto out;
            }
            bytes_zero += VIXDISKLIB_SECTOR_SIZE;
            zero_sectors++;

        } else {
            off_t bytes = write (fp, buf, VIXDISKLIB_SECTOR_SIZE);
            if (bytes!=VIXDISKLIB_SECTOR_SIZE) {
                logprintfl (EUCAERROR, "failed to write a sector in '%s', giving up", disk_path);
                goto out;
            }
        }

        // progress printer
        time_t now = time (NULL);
        if ((now-timestamp)>10) {
            timestamp = now;
            int percent = (int)((bytes_scanned*100)/vddk_bytes);
            logprintfl (EUCADEBUG, "transfer progress %ld/%ld bytes (%d%%) zeros=%ld\n", bytes_scanned, vddk_bytes, percent, bytes_zero);
        }

    }
    time_t after = time (NULL);
    ret = OK;

    logprintfl (EUCAINFO, "download of %lldMB-disk took %d seconds (zero sectors=%lld/%lld)\n", vddk_bytes/1000000, (int)(after-before), zero_sectors, vddk_blocks);

 out:
    close(fp);
    cleanup (&s);

    return ret;
}

// clone a local VMDK to a remote datastore

int vmdk_clone (const char * vmdk_path, const img_spec * spec)
{
    const img_loc * loc = &(spec->location);
    int ret = ERROR;
    vix_session s;

    if (open_connection (&s, spec)!=OK)
        goto out;

    char vmdkPathBuf [512];
    if (make_vmdk_spec (loc, vmdkPathBuf, sizeof(vmdkPathBuf))==NULL) {
        logprintfl (EUCAERROR, "insufficient information for the VMDK\n");
        goto out;
    }

    logprintfl (EUCAINFO, "cloning local disk %s to %s\n", vmdk_path, vmdkPathBuf);
    VixDiskLibCreateParams cloneParams;
    cloneParams.adapterType = VIXDISKLIB_ADAPTER_SCSI_BUSLOGIC;
    //cloneParams.capacity = sizeMb * 2048; // seems irrelevant
    cloneParams.diskType = VIXDISKLIB_DISK_STREAM_OPTIMIZED;
    cloneParams.hwVersion = VIXDISKLIB_HWVERSION_WORKSTATION_5;
    time_t before = time (NULL);
    vixError = VixDiskLib_Clone(s.cnxRemote,
                                vmdkPathBuf,
                                s.cnxLocal,
                                vmdk_path,
                                &cloneParams,
                                clone_progress_func,
                                NULL,   // clientData
                                TRUE);  // doOverWrite
    CHECK_ERROR;
    time_t after = time (NULL);
    logprintfl (EUCAINFO, "cloning of disk took %d seconds\n", (int)(after-before));

    ret = OK;

 out:
    cleanup (&s);

    return ret;
}

// convert a local disk file to a local VMDK

int vmdk_convert_local (const char * disk_path, const char * vmdk_path, boolean overwrite)
{
    int ret = ERROR;

    struct stat mystat;
    if (stat (vmdk_path, &mystat) == 0) {
        if (overwrite) {
            unlink (vmdk_path); // TODO: is this OK to do with blobstore 'blocks' files?
        } else {
            logprintfl (EUCAERROR, "output: vmdk_convert_local: file exists\n");
            return ret;
        }
    }
    if (stat (disk_path, &mystat) != 0) {
        logprintfl (EUCAERROR, "input: vmdk_convert_local: path does not exist: %s\n", disk_path);
        return ret;
    }
    // Although we try to ensure that blobs created by Eucalyptus are owned by 'eucalyptus', 
    // sometimes ownership changes to 'root' as a result of operations that we perform.
    // Here we try to ensure we can open the file by forcing the ownership right before use.
    // This is a conservative measure, in case sleep(1) in vbr.c is not long enough.
    if (diskutil_ch (disk_path, EUCALYPTUS_ADMIN, NULL, 0) != OK) {
        logprintfl (EUCAINFO, "error: failed to change user for '%s' to '%s'\n", disk_path, EUCALYPTUS_ADMIN);
        return ret;
    }
    int fp = open (disk_path, 0);
    if (fp<0) {
        logprintfl (EUCAERROR, "error: vmdk_convert_local: failed to open input path: %s\n", strerror(errno));
        return ret;
    }

    long long true_bytes;
    long long fs_bytes;
    long long fs_blocks;
    int  fs_blocksize;
    if (S_ISBLK(mystat.st_mode)) {
        if (ioctl (fp, BLKGETSIZE64, &true_bytes)!=0) {
            logprintfl (EUCAERROR, "input: vmdk_convert_local: failed to ioctl() device %s\n", disk_path);
            close (fp);
            return ret;
        }
        fs_bytes = true_bytes;
        fs_blocks = true_bytes / (long long) mystat.st_blksize;
        fs_blocksize = (int) mystat.st_blksize;

    } else if (S_ISREG(mystat.st_mode)) {
        true_bytes = (long long) mystat.st_size;
        fs_bytes = (long long) (mystat.st_blocks*mystat.st_blksize);
        fs_blocks = (long long) mystat.st_blocks;
        fs_blocksize = (int) mystat.st_blksize;
    } else {
        logprintfl (EUCAERROR, "input: vmdk_convert_local: invalid path (neither regular file nor block device): %s\n", disk_path);
        close (fp);
        return ret;
    }

    long long vddk_blocks = ( true_bytes / VIXDISKLIB_SECTOR_SIZE )
        + ( ( true_bytes % VIXDISKLIB_SECTOR_SIZE > 0 ) ? 1 : 0 ); // in case VDDK sectors are bigger than disk units
    long long vddk_bytes = vddk_blocks * VIXDISKLIB_SECTOR_SIZE;

    logprintfl (EUCAINFO, "intput file %s:\n", disk_path);
    logprintfl (EUCAINFO, "\ttrue bytes=%-12lld\n", true_bytes);
    logprintfl (EUCAINFO,  "\t  fs bytes=%-12lld blocks=%-9lld (blksize=%d)\n",
                fs_bytes,
                fs_blocks,
                fs_blocksize);
    logprintfl (EUCAINFO,  "\tvddk bytes=%-12lld blocks=%-9lld (blksize=%d)\n",
                vddk_bytes,
                vddk_blocks,
                (int) VIXDISKLIB_SECTOR_SIZE);

    if (vddk_blocks<2050) {
        logprintfl (EUCAERROR, "input file is too small to be converted into VMDK (1049600 bytes minimum)\n");
        close (fp);
        return ret;
    }

    vix_session s;
    if (open_connection (&s, NULL)!=OK)
        goto out;

    VixDiskLibCreateParams createParams;
    createParams.adapterType = VIXDISKLIB_ADAPTER_SCSI_BUSLOGIC;
    createParams.capacity = vddk_blocks;
    createParams.diskType = VIXDISKLIB_DISK_MONOLITHIC_SPARSE;
    createParams.hwVersion = VIXDISKLIB_HWVERSION_WORKSTATION_5;
    vixError = VixDiskLib_Create(s.cnxLocal,
                                 vmdk_path,
                                 &createParams,
                                 NULL,
                                 NULL);
    CHECK_ERROR;
    logprintfl (EUCAINFO, "created disk %s, copying data to it\n", vmdk_path);

    if (open_disk_local (&s, vmdk_path)!=OK)
        goto out;

    time_t before = time (NULL);
    uint8 buf [VIXDISKLIB_SECTOR_SIZE];
    char seen_partial = 0;
    long long zero_sectors = 0;

    for (VixDiskLibSectorType sector = 0; sector < vddk_blocks; sector++) {
        int bytes_read = read (fp, buf, VIXDISKLIB_SECTOR_SIZE);

        if (bytes_read!=VIXDISKLIB_SECTOR_SIZE) {
            if (bytes_read>0) { // partially written sector
                if (seen_partial) {
                    logprintfl (EUCAERROR, "second partially written sector, giving up\n");
                } else {
                    logprintfl (EUCAWARN, "warning: partially written sector in input file\n");
                    seen_partial = 1;
                }
                for (int i=bytes_read; i<VIXDISKLIB_SECTOR_SIZE; i++) { // fill with zeros
                    buf [i] = 0;
                }
            } else {
                logprintfl (EUCAERROR, "failed to read input file\n");
            }
        }

        // scan for all zeros
        char all_zeros = 1;
        for (int i=0; i<VIXDISKLIB_SECTOR_SIZE; i++) {
            if ( buf[i] != 0 ) {
                all_zeros = 0;
                break;
            }
        }

        if (!all_zeros) {
            vixError = VixDiskLib_Write(s.diskHandleLocal, sector, 1, buf);
            CHECK_ERROR;
        } else {
            zero_sectors++;
        }
    }
    time_t after = time (NULL);
    logprintfl (EUCAINFO, "copy of %lldMB-disk took %d seconds (zero sectors=%lld/%lld)\n", vddk_bytes/1000000, (int)(after-before), zero_sectors, vddk_blocks);

    ret = OK;
 out:
    close (fp);
    cleanup (&s);

    return ret;
}

// helper used by vSphere-related commands to break up path into vds (datacenter) and vpath (path on datacenter)
int parse_vsphere_path (const char * path, char * vds, const int vds_size, char * vpath, const int vpath_size)
{
    char * br1 = strstr (path, "[");
    char * br2 = strstr (path, "]");

    if (br1==NULL || br2==NULL || br1>=br2 || (br2-path+2)>strlen(path)) return ERROR;

    int len = br2-br1-1;
    if (len>vds_size) len = vds_size;
    strncpy (vds, br1+1, len);

    if (*(br2+1)==' ') br2++; // skip the space after [datastore], if any
    strncpy (vpath, br2+1, vpath_size);

    return OK;
}

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <getopt.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <ami2vmx.h>
#include <string.h>
#include <errno.h>
#include "misc.h" // logprintfl

extern char *helpers[LASTHELPER];
extern char *helpers_path[LASTHELPER];

int do_convert(
        char *infile, 
        char *outfile, 
        char *kernel, 
        char *ramdisk, 
        char *modules, 
        int swap, 
        int ephemeral, 
        int bits, 
        int dopause, 
        int force) 
{
    int i, rc, done, first_sector, last_sector;
    off_t disksize=0;
    struct stat mystat;
    char *output, *loopdev, *loopdevp, *ptr;
    char file[1024], cmd[1024];
    char *tmpdir=NULL, *kfile=NULL, *rfile=NULL;
    char bail = 0;
    FILE *PH;

    rc = stat(infile, &mystat);
    disksize = (mystat.st_size)/1000000 + swap + ephemeral + 1;
  
    logprintfl (EUCAINFO, "Creating intermediate disk file...\n");
    output = pruntf("%s if=/dev/zero of=%s-disk bs=1M seek=%d count=1", helpers_path[DD], infile, disksize);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: cannot create intermediate disk file\n");
        return(1);
    }

    logprintfl (EUCAINFO, "Initializing intermediate disk file...\n");
    output = pruntf("%s --script %s-disk mklabel msdos", helpers_path[PARTED], infile);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: cannot initialize disk file\n");
        return(1);
    }

    logprintfl (EUCAINFO, "Setting up primary partition...\n");
    first_sector = 63;
    last_sector = first_sector + (mystat.st_size / 512);
    output = pruntf("%s --script %s-disk mkpart primary ext2 %ds %ds", helpers_path[PARTED], infile, first_sector, last_sector);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: cannot setup primary partition in disk file\n");
        return(1);
    }

    {
        int done=0;

        logprintfl (EUCAINFO, "Looking for loop devices...\n");
        for (i=0; i<30 && !done; i++) {
            output = pruntf("%s %s -f", helpers_path[ROOTWRAP], helpers_path[LOSETUP]);
            if (strstr(output, "/dev/loop")) {
                loopdev = strdup(output);
                ptr = strrchr(loopdev, '\n');
                if (ptr) *ptr = '\0';
                done++;
            }
        }
        if (done) {
            logprintfl (EUCAINFO, "Attaching disk to loop device...\n");
            output = pruntf("%s %s -o 32256 %s %s-disk", helpers_path[ROOTWRAP], helpers_path[LOSETUP], loopdev, infile);
            if (!output) {
                logprintfl (EUCAINFO, "ERROR: cannot attach %s-disk to loop device %s\n", infile, loopdev);
                return(1);
            }
        } else {
            logprintfl (EUCAINFO, "ERROR: cannot find free loop device\n");
            return(1);
        }
  
        logprintfl (EUCAINFO, "Copying infile data to intermediate disk file...\n");
        output = pruntf("%s %s if=%s of=%s bs=512k", helpers_path[ROOTWRAP], helpers_path[DD], infile, loopdev);
        if (!output) {
            logprintfl (EUCAINFO, "ERROR: cannot copy infile to intermediate loop device\n");
            bail = 1;
        }

        output = pruntf("%s %s", helpers_path[ROOTWRAP], helpers_path[SYNC]);
        output = pruntf("%s %s -d %s", helpers_path[ROOTWRAP], helpers_path[LOSETUP], loopdev);
        if (!output) {
            logprintfl (EUCAINFO, "ERROR: cannot detach loop device\n");
            return(1);
        }
        if (bail) return 1;
    }

    logprintfl (EUCAINFO, "Setting up swap and ephemeral partitions...\n");
    first_sector = last_sector+1;
    last_sector = first_sector + (ephemeral * 2000);
    output = pruntf("%s %s --script %s-disk mkpartfs primary ext2 %ds %ds", helpers_path[ROOTWRAP], helpers_path[PARTED], infile, first_sector, last_sector);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: cannot set up ephemeral partition\n");
        return(1);
    }

    first_sector = last_sector+1;
    output = pruntf("%s %s --script %s-disk mkpartfs primary linux-swap %ds 100%%", helpers_path[ROOTWRAP], helpers_path[PARTED], infile, first_sector);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: cannot set up swap partition\n");
        return(1);
    }
  
    // now we have a disk image
    
    char dtemplate[] = "/tmp/euca.XXXXXX";
    tmpdir = mkdtemp(dtemplate);
    if (tmpdir==NULL) {
            logprintfl (EUCAINFO, "ERROR: mkdtemp() failed: %s\n", strerror(errno));
            return(1);    
    }
    
    {
        int done=0;

        for (i=0; i<30 && !done; i++) {
            output = pruntf("%s %s -f", helpers_path[ROOTWRAP], helpers_path[LOSETUP]);
            if (strstr(output, "/dev/loop")) {
                loopdev = strdup(output);
                ptr = strrchr(loopdev, '\n');
                if (ptr) *ptr = '\0';
                done++;
            }
        }
        if (done) {
            output = pruntf("%s %s -o 32256 %s %s-disk", helpers_path[ROOTWRAP], helpers_path[LOSETUP], loopdev, infile);
            if (!output) {
                logprintfl (EUCAINFO, "ERROR: cannot attach %s-disk to loop device %s\n", infile, loopdev);
                return(1);
            }
        } else {
            logprintfl (EUCAINFO, "ERROR: cannot find free loop device\n");
            return(1);
        }
      
        logprintfl (EUCAINFO, "Mounting volume...\n");
        output = pruntf("%s %s mount %s %s", helpers_path[ROOTWRAP], helpers_path[MOUNTWRAP], loopdev, tmpdir);
        if (!output) {
            logprintfl (EUCAINFO, "ERROR: failed to mount %s on %s\n", loopdev, tmpdir);
            bail = 1;
            goto clean_up;
        }
      
        logprintfl (EUCAINFO, "Installing boot loader...\n");
        output = pruntf("%s %s -p %s/boot/grub/", helpers_path[ROOTWRAP], helpers_path[MKDIR], tmpdir);
        if (!output) {
            logprintfl (EUCAINFO, "ERROR: failed to create grub directory\n");
            bail = 1;
            goto clean_up;
        }
      
        output = pruntf("%s %s /boot/grub/*stage* %s/boot/grub", helpers_path[ROOTWRAP], helpers_path[CP], tmpdir);
        if (!output) {
            logprintfl (EUCAINFO, "ERROR: failed to copy stage files into grub directory\n");
            bail = 1;
            goto clean_up;
        }

        ptr = strrchr(kernel, '/');
        if (ptr) {
            kfile = strdup(ptr+1);
        } else {
            kfile = strdup(kernel);
        }
      
        if (ramdisk) {
            ptr = strrchr(ramdisk, '/');
            if (ptr) {
                rfile = strdup(ptr+1);
            } else {
                rfile = strdup(ramdisk);
            }
        }
      
        logprintfl (EUCAINFO, "Installing kernel, ramdisk and modules...\n");
        output = pruntf("%s %s %s %s/boot/%s", helpers_path[ROOTWRAP], helpers_path[CP], kernel, tmpdir, kfile);
        if (!output) {
            logprintfl (EUCAINFO, "ERROR: failed to copy the kernel to boot directory\n");
            bail = 1;
            goto clean_up;
        }
      
        if (ramdisk) {
            output = pruntf("%s %s %s %s/boot/%s", helpers_path[ROOTWRAP], helpers_path[CP], ramdisk, tmpdir, rfile);
            if (!output) {
                logprintfl (EUCAINFO, "ERROR: failed to copy the ramdisk to boot directory\n");
                bail = 1;
                goto clean_up;
            }
        }
      
        if (modules) { // dmitrii made modules optional
            while(strlen(modules) && modules[strlen(modules)-1] == '/') {
                modules[strlen(modules)-1] = '\0';
            }
            output = pruntf("%s %s -az %s %s/lib/modules/", helpers_path[ROOTWRAP], helpers_path[RSYNC], modules, tmpdir);
            if (!output) {
                logprintfl (EUCAINFO, "ERROR: failed to rsync the modules\n");
                bail = 1;
                goto clean_up;
            }
        }
      
    }

    {
        char ftemplate[] = "/tmp/euca.XXXXXX";
        FILE * FH = fdopen(mkstemp (ftemplate), "w");

        if (FH==NULL) {
            logprintfl (EUCAINFO, "ERROR: failed to create a temporary file\n");
            bail = 1;
            goto clean_up;
        }
        if (ramdisk) {
            fprintf(FH, "default=0\ntimeout=5\n\ntitle TheOS\nroot (hd0,0)\nkernel /boot/%s root=/dev/sda1 ro\ninitrd /boot/%s\n", kfile, rfile);
        } else {
            fprintf(FH, "default=0\ntimeout=5\n\ntitle TheOS\nroot (hd0,0)\nkernel /boot/%s root=/dev/sda1 ro\n", kfile);
        }
        fflush(FH);
        output = pruntf("%s %s %s %s/boot/grub/menu.lst", helpers_path[ROOTWRAP], helpers_path[CP], ftemplate, tmpdir);
        fclose(FH);
        unlink (ftemplate);
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: failed to copy %s to %s/boot/grub/menu.lst\n", ftemplate, tmpdir);
            bail = 1;
            goto clean_up;
        }

        char ftemplate2[] = "/tmp/euca.XXXXXX";
        FH = fdopen(mkstemp (ftemplate2), "w");
        if (FH==NULL) {
            logprintfl (EUCAINFO, "ERROR: failed to create a temporary file\n");
            bail = 1;
            goto clean_up;
        }
        if (ramdisk) {
            fprintf(FH, "default=0\ntimeout=5\n\ntitle TheOS\n\troot (hd0,0)\n\tkernel /boot/%s root=/dev/sda1 ro\n\tinitrd /boot/%s\n", kfile, rfile);
        } else {
            fprintf(FH, "default=0\ntimeout=5\n\ntitle TheOS\n\troot (hd0,0)\n\tkernel /boot/%s root=/dev/sda1 ro\n", kfile);
        }
        fflush(FH);
        output = pruntf("%s %s %s %s/boot/grub/grub.conf", helpers_path[ROOTWRAP], helpers_path[CP], ftemplate2, tmpdir);
        fclose(FH);
        unlink (ftemplate);
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: failed to copy %s to %s/boot/grub/grub.conf\n", ftemplate2, tmpdir);
            bail = 1;
            goto clean_up;
        }
    }
  
    if (dopause) {
        printf("Pausing while image is mounted on '%s', hit enter to continue.", tmpdir);
        fgetc(stdin);
    }

clean_up:

    output = pruntf("%s %s umount %s", helpers_path[ROOTWRAP], helpers_path[MOUNTWRAP], tmpdir);
    if (!output) {
        printf("WARNING: failed to umount\n");
    }
  
    output = pruntf("%s %s -d %s", helpers_path[ROOTWRAP], helpers_path[LOSETUP], loopdev);
    if (!output) {
        printf("WARNING: failed to losetup -d\n");
    }

    if (bail)
        return 1;
  
    snprintf(cmd, 1024, "%s --device-map=/dev/null --batch >/dev/null 2>&1", helpers_path[GRUB]);
    PH = popen(cmd, "w");
  
    fprintf(PH, "device (hd0) %s-disk\n", infile);
    fprintf(PH, "root (hd0,0)\n");
    fprintf(PH, "setup (hd0)\n");
    fprintf(PH, "quit\n");
  
    rc = pclose(PH);
  
    return 0; // dmitrii stops here
    
    logprintfl (EUCAINFO, "Converting intermediate volume to VMWare image...\n");
    output = pruntf("%s %s convert -f raw %s-disk -O vmdk %s", helpers_path[ROOTWRAP], helpers_path[KVMIMG], infile, outfile);
    if (!output) {
        logprintfl (EUCAINFO, "ERROR: VMX conversion failed\n");
        return(1);
    }

    snprintf(file, 1024, "%s-disk", infile);
    unlink(file);

    {
        logprintfl (EUCAINFO, "Creating VMX configuration file...\n");
        snprintf(file, 1024, "%s.vmx", outfile);
        FILE * FH = fopen(file, "w");
        if (bits == 32) {
            fprintf(FH, "#!/usr/bin/vmware\nconfig.version = \"8\"\nvirtualHW.version = \"4\"\nguestOS=\"otherlinux\"\nscsi0:0.present = \"TRUE\"\nscsi0:0.fileName = \"%s\"\nscsi0:0.writeThrough = \"TRUE\"\nscsi0.virtualDev = \"lsilogic\"\nmemsize = \"512\"\nvmi.enabled=\"TRUE\"\nEthernet0.present = \"TRUE\"\nEthernet0.virtualDev = \"e1000\"\n", outfile);
        } else if (bits == 64) {
            fprintf(FH, "#!/usr/bin/vmware\nconfig.version = \"8\"\nvirtualHW.version = \"4\"\nguestOS=\"otherlinux-64\"\nscsi0:0.present = \"TRUE\"\nscsi0:0.fileName = \"%s\"\nscsi0:0.writeThrough = \"TRUE\"\nscsi0.virtualDev = \"lsilogic\"\nmemsize = \"512\"\nvmi.enabled=\"TRUE\"\nEthernet0.present = \"TRUE\"\nEthernet0.virtualDev = \"e1000\"\n", outfile);
        }
        fclose(FH);
    }
    
 
    logprintfl (EUCAINFO, "Conversion complete.\n");
    logprintfl (EUCAINFO, "image=%s\n", outfile);
    logprintfl (EUCAINFO, "config=%s.vmx\n", outfile);
  
    return(0);
}

  

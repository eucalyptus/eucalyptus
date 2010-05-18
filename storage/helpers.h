#ifndef HELPERS_H
#define HELPERS_H

#include "misc.h" // bolean

enum { MKSWAP=0, MKEXT3, FILECMD, LOSETUP, MOUNT, GRUB, PARTED, MV, DD, SYNC, MKDIR, CP, RSYNC, UMOUNT, CAT, CHOWN, CHMOD, ROOTWRAP, MOUNTWRAP, LASTHELPER };

int help_init (void);
int help_ddzero (const char * path, const long long sectors, boolean zero_fill);
int help_dd (const char * in, const char * out, const int bs, const long long count);
int help_mbr (const char * path, const char * type);
int help_part (const char * path, char * part_type, const char * fs_type, const long long first_sector, const long long last_sector);
int help_loop (const char * path, const long long offset, const long long len, char * lodev, int lodev_size);
int help_unloop (const char * lodev);
int help_mkswap (const char * lodev);
int help_mkfs (const char * lodev);
int help_sectors (const char * path, const int part, long long * first, long long * last);
int help_mount (const char * dev, const char * mnt_pt);
int help_umount (const char * dev);
int help_grub_files (const char * mnt_pt, const int part, const char * kernel, const char * ramdisk);
int help_grub_mbr (const char * path, const int part);
int help_ch (const char * path, const char * user, const int perms);
int help_mkdir (const char * path);
int help_cp (const char * from, const char * to);

#endif

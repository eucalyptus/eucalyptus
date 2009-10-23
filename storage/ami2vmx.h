#ifndef INCLUDE_AMI2VMX_H
#define INCLUDE_AMI2VMX_H

enum {LOSETUP, MOUNT, KVMIMG, GRUB, PARTED, MV, DD, SYNC, MKDIR, CP, RSYNC, UMOUNT, CAT, ROOTWRAP, MOUNTWRAP, LASTHELPER};

int verify_ami2vmx_helpers(int, char *);
int verify_input(char *, char *, char *, char *, int);
int do_convert(char *, char *, char *, char *, char *, int, int, int, int, int);
char *pruntf(char *, ...);
void usage(void);

#endif

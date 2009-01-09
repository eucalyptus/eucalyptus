#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mount.h>

int main(int argc, char **argv) {
  int rc;
  if (argc < 2) {
    exit(1);
  }
  if (!strcmp("mount", argv[1])) {
    if (argc < 4) {
      exit(1);
    }

    rc = mount(argv[2], argv[3], "ext2", MS_MGC_VAL, NULL);
    if (rc) {
      perror("");
      exit(1);
    }
  } else if (!strcmp("umount", argv[1])) {
    if (argc < 3) {
      exit(1);
    }

    rc = umount(argv[2]);
    if (rc) {
      perror("");
      exit(1);
    }
  }
  exit(0);
}

#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU /* strnlen */
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <dirent.h> /* open|read|close dir */
#include <time.h> /* time() */

#include <storage-windows.h>

int makeWindowsFloppy(char *euca_home, char *rundir_path, char *keyName, char *password) {
  int fd, rc, rbytes, count;
  char *buf, *ptr, *tmp, *newpass, dest_path[1024], source_path[1024], fname[1024];

  /*
    char *password, *rundir_path, *euca_home;
    password = strdup("HALLOTHAR");
    snprintf(source_path, 1024, "flop");
    snprintf(dest_path, 1024, "flop.new");
  */

  // generate the pem
  snprintf(fname, 1024, "%s/key.pub", rundir_path);
  rc = write2file(fname, keyName);
  if (rc) {
    //logprintfl(EUCAERROR, "makeWindowsFloppy(): failed to write public key to file %s\n", fname);
    return(1);
  } else {
    rc = vrun("ssh-keygen -e -f %s > %s.pem", fname, fname);
    if (rc) {
      //      logprintfl(EUCAERROR, "makeWindowsFloppy(): failed to convert to PEM\n");
      return(1);
    }
  }
  
  snprintf(source_path, 1024, "%s/usr/share/eucalyptus/floppy", euca_home);
  snprintf(dest_path, 1024, "%s/floppy", rundir_path);

  buf = malloc(1024 * 2048);
  if (!buf) {
    return(1);
  }

  fd = open(source_path, O_RDONLY);
  if (fd < 0) {
    if (buf) free(buf);
    return(1);
  }

  rbytes = read(fd, buf, 1024 * 2048);
  close(fd);

  ptr = buf;
  count=0;
  tmp = malloc(sizeof(char) * strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER")+1);
  newpass = malloc(sizeof(char) * strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER")+1);

  bzero(tmp, strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER")+1);
  bzero(newpass, strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER")+1);
  snprintf(newpass, strlen(password)+1, "%s", password);

  while(count < rbytes) {
    memcpy(tmp, ptr, strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER"));
    if (!strcmp(tmp, "MAGICEUCALYPTUSPASSWORDPLACEHOLDER")) {
      memcpy(ptr, newpass, strlen("MAGICEUCALYPTUSPASSWORDPLACEHOLDER"));
    }
    ptr++;
    count++;
  }

  fd = open(dest_path, O_CREAT | O_TRUNC | O_RDWR);
  if (fd < 0) {
    if (buf) free(buf);
    return(1);
  }
  rc = write(fd, buf, rbytes);
  if (rc != rbytes) {
    if (buf) free(buf);
    return(1);
  }
  close(fd);
  if (buf) free(buf);
  return(0);
}

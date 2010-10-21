#define _FILE_OFFSET_BITS 64 // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU
#include <string.h> /* strlen, strcpy */
#include <time.h>
#include <limits.h> /* INT_MAX */
#include <sys/types.h> /* fork */
#include <sys/wait.h> /* waitpid */
#include <unistd.h>
#include <fcntl.h>
#include <assert.h>
#include <errno.h>
#include <sys/stat.h>
#include <pthread.h>
#include <sys/vfs.h> /* statfs */
#include <signal.h> /* SIGINT */
#include <linux/limits.h>
#ifndef MAX_PATH
#define MAX_PATH 4096
#endif

#include <data.h>
#include <windows-bundle.h>

int allocate_bundleTask(bundleTask *out, char *instanceId, char *state) {
  if (out != NULL) {
    bzero(out, sizeof(bundleTask));
    if (instanceId) snprintf(out->instanceId, CHAR_BUFFER_SIZE, "%s", instanceId);
    if (state) snprintf(out->state, CHAR_BUFFER_SIZE, "%s", state);
  }
  return(0);
}


#ifndef INCLUDE_WINDOWS_BUNDLE_H
#define INCLUDE_WINDOWS_BUNDLE_H

#include <eucalyptus.h>
#include <data.h>

#define MAXBUNDLES MAXINSTANCES

typedef struct bundleTask_t {
  char instanceId[CHAR_BUFFER_SIZE];
  char state[CHAR_BUFFER_SIZE];
} bundleTask;

int allocate_bundleTask(bundleTask *out, char *instanceId, char *state);

#endif

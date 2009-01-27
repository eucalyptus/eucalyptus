#ifndef INCLUDE_STORAGE_H
#define INCLUDE_STORAGE_H

#include "data.h"
#include "ipc.h"
#include <vnetwork.h>

int scInitConfig (void);
int scSetInstancePath(char *path);
const char * scGetInstancePath(void);
void scSaveInstanceInfo (const ncInstance * instance);
ncInstance * scRecoverInstanceInfo (const char *instanceId);
void LogprintfCache (void);
long long scFSCK (bunchOfInstances ** instances);
int scGetConfigXML(char *user, char *amiId, char **out);
int scMakeInstanceImage(char *user, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *instId, char *keyName, char **instance_path, sem * s, int convert_to_disk);
int scCleanupInstanceImage(char *user, char *instId);
int scStoreStringToInstanceFile (const char *userId, const char *instanceId, const char * file, const char * data);

/* utility function (exported for now so it can be tested by test.c) */
int ensure_path_exists (const char * path);
int test_cache (void);

#endif

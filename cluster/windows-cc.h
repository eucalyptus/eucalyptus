#ifndef INCLUDE_WINDOWS_CC_H
#define INCLUDE_WINDOWS_CC_H

#include <windows-bundle.h>

enum {BUNDLEINVALID, BUNDLEVALID};

typedef struct ccBundleCache_t {
  bundleTask bundles[MAXBUNDLES];
  int lastseen[MAXBUNDLES];
  int cacheState[MAXBUNDLES];
  int numBundles;
  int lastBundleUpdate;
  int bundleCacheUpdate;
} ccBundleCache;

int refresh_bundleTasks(ncMetadata *ccMeta, int timeout, int dolock);
int add_bundleCache(char *instanceId, bundleTask *in);
int refresh_bundleCache(char *instanceId, bundleTask *in);
int del_bundleCacheId(char *instanceId);
int find_bundleCacheId(char *instanceId, bundleTask **out);
void print_bundleCache(void);
void invalidate_bundleCache(void);

#endif

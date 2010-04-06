#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <signal.h>

#include "axis2_skel_EucalyptusCC.h"

#include <server-marshal.h>
#include <handlers.h>
#include <storage.h>
#include <vnetwork.h>
#include <euca_auth.h>
#include <misc.h>
#include <ipc.h>

#include "data.h"
#include "client-marshal.h"
#include "handlers.h"
#include "windows-cc.h"

extern ccConfig *config;

extern ccInstanceCache *instanceCache;

extern ccResourceCache *resourceCache;

extern vnetConfig *vnetconfig;

//extern ccBundleCache *bundleCache;

extern sem_t *locks[ENDLOCK];
extern int mylocks[ENDLOCK];

// function called by the CC during polling
int refresh_bundleTasks(ncMetadata *ccMeta, int timeout, int dolock) {
  bundleTask *myBundle=NULL;
  int i, k, rc, pid, numBundles, found=0;
  time_t op_start, op_timer, op_pernode;
  
  bundleTask **outBundleTasks=NULL;
  int outBundleTasksLen=0;
  ncStub *ncs;
  
  ccResourceCache resourceCacheLocal;

  op_start = time(NULL);
  op_timer = timeout;
  logprintfl(EUCAINFO,"refresh_bundleTasks(): called\n");

  // critical NC call section
  sem_mywait(NCCALL);  

  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);

  //  invalidate_bundleCache();

  for (i=0; i<resourceCacheLocal.numResources; i++) {
    if (resourceCacheLocal.resources[i].state == RESUP) {
      int status, ret=0;
      int filedes[2];
      int len, j;
      
      rc = pipe(filedes);
      pid = fork();
      if (pid == 0) {
	ret=0;
	close(filedes[0]);
	ncs = ncStubCreate(resourceCacheLocal.resources[i].ncURL, NULL, NULL);
	if (config->use_wssec) {
	  rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
	}
        outBundleTasksLen=0;

	rc = ncDescribeBundleTasksStub(ncs, ccMeta, NULL, 0, &outBundleTasks, &outBundleTasksLen);

	if (!rc) {
	  len = outBundleTasksLen;
	  rc = write(filedes[1], &len, sizeof(int));
	  for (j=0; j<len; j++) {
	    myBundle = outBundleTasks[j];
	    rc = write(filedes[1], myBundle, sizeof(bundleTask));
	  }
	  ret = 0;
	} else {
	  len = 0;
	  rc = write(filedes[1], &len, sizeof(int));
	  ret = 1;
	}
	close(filedes[1]);
	fflush(stdout);
	
	exit(ret);
      } else {
	int len,rbytes,j;
	close(filedes[1]);
	
	op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	op_pernode = (op_timer/(resourceCacheLocal.numResources-i)) > OP_TIMEOUT_PERNODE ? (op_timer/(resourceCacheLocal.numResources-i)) : OP_TIMEOUT_PERNODE;
	logprintfl(EUCADEBUG, "refresh_bundleTasks(): timeout(%d/%d) len\n", op_pernode, OP_TIMEOUT);
	rbytes = timeread(filedes[0], &len, sizeof(int), op_pernode);
	if (rbytes <= 0) {
	  // read went badly
	  kill(pid, SIGKILL);
	  wait(&status);
	  rc = -1;
	} else {
	  if (rbytes < sizeof(int)) {
	    len = 0;
	    outBundleTasks = NULL;
	    outBundleTasksLen = 0;
	  } else {
	    outBundleTasks = malloc(sizeof(bundleTask *) * len);
	    if (!outBundleTasks) {
	      logprintfl(EUCAFATAL, "refresh_bundleTasks(): out of memory!\n");
	      unlock_exit(1);
	    }
	    outBundleTasksLen = len;
	    for (j=0; j<len; j++) {
	      myBundle = malloc(sizeof(bundleTask));
	      if (!myBundle) {
		logprintfl(EUCAFATAL, "refresh_bundleTasks(): out of memory!\n");
		unlock_exit(1);
	      }
	      op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	      op_pernode = (op_timer/(resourceCacheLocal.numResources-i)) > OP_TIMEOUT_PERNODE ? (op_timer/(resourceCacheLocal.numResources-i)) : OP_TIMEOUT_PERNODE;
	      logprintfl(EUCADEBUG, "refresh_bundleTasks(): timeout(%d/%d) inst\n", op_pernode, OP_TIMEOUT);
	      rbytes = timeread(filedes[0], myBundle, sizeof(bundleTask), op_pernode);
	      outBundleTasks[j] = myBundle;
	      myBundle = NULL;
	    }
	  }
	  wait(&status);
	  rc = WEXITSTATUS(status);
	}
	close(filedes[0]);
      }
      
      if (rc != 0) {
	logprintfl(EUCAERROR,"refresh_bundleTasks(): ncDescribeBundleTasksStub(%s): returned fail: (%d/%d)\n", resourceCacheLocal.resources[i].ncURL, pid, rc);
      } else {
	for (j=0; j<outBundleTasksLen; j++) {
	  found=1;
	  if (found) {
	    // add it
	    logprintfl(EUCADEBUG,"refresh_bundleTasks(): describing bundleTask %s, %s, %d\n", outBundleTasks[j]->instanceId, outBundleTasks[j]->state, j);
	    numBundles++;
	    
	    // grab instance from cache, if available.  otherwise, start from scratch
	    find_bundleCacheId(outBundleTasks[j]->instanceId, &myBundle);
	    if (!myBundle) {
	      myBundle = malloc(sizeof(bundleTask));
	      if (!myBundle) {
		logprintfl(EUCAFATAL, "refresh_bundleTasks(): out of memory!\n");
		unlock_exit(1);
	      }
	      bzero(myBundle, sizeof(bundleTask));
	    }

	    // update CC instance with instance state from NC 
	    snprintf(myBundle->instanceId, CHAR_BUFFER_SIZE, "%s", outBundleTasks[j]->instanceId);
	    snprintf(myBundle->state, CHAR_BUFFER_SIZE, "%s", outBundleTasks[j]->state);
	    //	    rc = ccInstance_to_ncInstance(myInstance, ncOutInsts[j]);

	    refresh_bundleCache(myBundle->instanceId, myBundle);

	    logprintfl(EUCADEBUG, "refresh_bundleTasks(): storing bundle state: %s/%s\n", myBundle->instanceId, myBundle->state);
	    //	    print_ccInstance("refresh_instances(): ", myIn);
	    free(myBundle);
	  }
	}
      }

      if (outBundleTasks) {
        for (j=0; j<outBundleTasksLen; j++) {
          free(outBundleTasks[j]);
        }
        free(outBundleTasks);
        outBundleTasks = NULL;
	outBundleTasksLen=0;
      }
    }
  }

  sem_mypost(NCCALL);

  logprintfl(EUCADEBUG,"refresh_bundleTasks(): done\n");  
  return(0);
}

void print_bundleCache(void) {
  int i;

  sem_mywait(BUNDLECACHE);
  for (i=0; i<MAXBUNDLES; i++) {
    if ( bundleCache->cacheState[i] == BUNDLEVALID ) {
      logprintfl(EUCADEBUG,"\tcache: %d/%d %s %s\n", i, bundleCache->numBundles, bundleCache->bundles[i].instanceId, bundleCache->bundles[i].state);
    }
  }
  sem_mypost(BUNDLECACHE);
}

void invalidate_bundleCache(void) {
  int i;
  
  sem_mywait(BUNDLECACHE);
  for (i=0; i<MAXBUNDLES; i++) {
    if ( (bundleCache->cacheState[i] == BUNDLEVALID) && ((time(NULL) - bundleCache->lastseen[i]) > 300)) {
      logprintfl(EUCADEBUG, "invalidate_bundleCache(): invalidating bundle '%s' (last seen %d seconds ago)\n", bundleCache->bundles[i].instanceId, (time(NULL) - bundleCache->lastseen[i]));
      bzero(&(bundleCache->bundles[i]), sizeof(bundleTask));
      bundleCache->lastseen[i] = 0;
      bundleCache->cacheState[i] = BUNDLEINVALID;
      bundleCache->numBundles--;
    }
  }
  sem_mypost(BUNDLECACHE);
}

int refresh_bundleCache(char *instanceId, bundleTask *in){
  int i, done, rc;
  
  if (!instanceId || !in) {
    return(1);
  }
  
  sem_mywait(BUNDLECACHE);
  done=0;
  for (i=0; i<MAXBUNDLES && !done; i++) {
    if (!strcmp(bundleCache->bundles[i].instanceId, instanceId)) {
      // in cache
      memcpy(&(bundleCache->bundles[i]), in, sizeof(bundleTask));
      bundleCache->lastseen[i] = time(NULL);
      sem_mypost(BUNDLECACHE);
      return(0);
    }
  }
  sem_mypost(BUNDLECACHE);

  add_bundleCache(instanceId, in);

  return(0);
}

int add_bundleCache(char *instanceId, bundleTask *in){
  int i, done, firstNull=0;

  if (!instanceId || !in) {
    return(1);
  }
  
  sem_mywait(BUNDLECACHE);
  done=0;
  for (i=0; i<MAXBUNDLES && !done; i++) {
    if ( (bundleCache->cacheState[i] == BUNDLEVALID ) && (!strcmp(bundleCache->bundles[i].instanceId, instanceId))) {
      // already in cache
      logprintfl(EUCADEBUG, "add_bundleCache(): '%s' already in cache\n", instanceId);
      bundleCache->lastseen[i] = time(NULL);
      sem_mypost(BUNDLECACHE);
      return(0);
    } else if ( bundleCache->cacheState[i] == BUNDLEINVALID ) {
      firstNull = i;
      done++;
    }
  }
  logprintfl(EUCADEBUG, "add_bundleCache(): adding '%s' to cache\n", instanceId);
  allocate_bundleTask(&(bundleCache->bundles[firstNull]), in->instanceId, in->state);
  bundleCache->numBundles++;
  bundleCache->lastseen[firstNull] = time(NULL);
  bundleCache->cacheState[firstNull] = BUNDLEVALID;

  sem_mypost(BUNDLECACHE);
  return(0);
}

int del_bundleCacheId(char *instanceId) {
  int i;

  sem_mywait(BUNDLECACHE);
  for (i=0; i<MAXBUNDLES; i++) {
    if ( (bundleCache->cacheState[i] == BUNDLEVALID) && (!strcmp(bundleCache->bundles[i].instanceId, instanceId))) {
      // del from cache
      bzero(&(bundleCache->bundles[i]), sizeof(bundleTask));
      bundleCache->lastseen[i] = 0;
      bundleCache->cacheState[i] = BUNDLEINVALID;
      bundleCache->numBundles--;
      sem_mypost(BUNDLECACHE);
      return(0);
    }
  }
  sem_mypost(BUNDLECACHE);
  return(0);
}

int find_bundleCacheId(char *instanceId, bundleTask **out) {
  int i, done;
  
  if (!instanceId || !out) {
    return(1);
  }
  
  sem_mywait(BUNDLECACHE);
  *out = NULL;
  done=0;
  for (i=0; i<MAXBUNDLES && !done; i++) {
    if (!strcmp(bundleCache->bundles[i].instanceId, instanceId)) {
      // found it
      *out = malloc(sizeof(bundleTask));
      if (!*out) {
	logprintfl(EUCAFATAL, "find_bundleCacheId(): out of memory!\n");
	unlock_exit(1);
      }

      allocate_bundleTask(*out, bundleCache->bundles[i].instanceId,bundleCache->bundles[i].state);
      logprintfl(EUCADEBUG, "find_bundleCache(): found instance in cache '%s'\n", bundleCache->bundles[i].instanceId);
      done++;
    }
  }
  sem_mypost(BUNDLECACHE);
  if (done) {
    return(0);
  }
  return(1);
}

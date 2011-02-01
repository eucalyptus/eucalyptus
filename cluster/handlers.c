/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
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
#include <misc.h>
#include <ipc.h>
#include <walrus.h>

#include <euca_axis.h>
#include "data.h"
#include "client-marshal.h"

#include <storage-windows.h>
#include <euca_auth.h>

#include <handlers-state.h>

#define SUPERUSER "eucalyptus"

// local globals
int config_init=0;
int local_init=0;
int thread_init=0;
int init=0;

// to be stored in shared memory
ccConfig *config=NULL;

ccInstanceCache *instanceCache=NULL;

ccResourceCache *resourceCache=NULL;
ccResourceCache *resourceCacheStage=NULL;

vnetConfig *vnetconfig=NULL;

//sem_t *locks[ENDLOCK] = {NULL, NULL, NULL, NULL, NULL, NULL};
//int mylocks[ENDLOCK] = {0,0,0,0,0,0};
sem_t *locks[ENDLOCK];
int mylocks[ENDLOCK];

//ccBundleCache *bundleCache=NULL;

int doBundleInstance(ncMetadata *ccMeta, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL, char *userPublicKey, char *S3Policy, char *S3PolicySig) {
  int i, j, rc, start = 0, stop = 0, ret=0, timeout, done;
  ccInstance *myInstance;
  ncStub *ncs;
  time_t op_start;
  ccResourceCache resourceCacheLocal;

  i = j = 0;
  myInstance = NULL;
  op_start = time(NULL);
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO, "BundleInstance(): called\n");
  logprintfl(EUCADEBUG, "BundleInstance(): params: userId=%s, instanceId=%s, bucketName=%s, filePrefix=%s, walrusURL=%s, userPublicKey=%s, S3Policy=%s, S3PolicySig=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(instanceId), SP(bucketName), SP(filePrefix), SP(walrusURL), SP(userPublicKey), SP(S3Policy), SP(S3PolicySig));
  if (!instanceId) {
    logprintfl(EUCAERROR, "BundleInstance(): bad input params\n");
    return(1);
  }

  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
  
  rc = find_instanceCacheId(instanceId, &myInstance);
  if (!rc) {
    // found the instance in the cache
    if (myInstance) {
      start = myInstance->ncHostIdx;
      stop = start+1;
      free(myInstance);
    }
  } else {
    start = 0;
    stop = resourceCacheLocal.numResources;
  }
  
  done=0;
  for (j=start; j<stop && !done; j++) {
    timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop-start, j);
    rc = ncClientCall(ccMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncBundleInstance", instanceId, bucketName, filePrefix, walrusURL, userPublicKey, S3Policy, S3PolicySig);
    if (rc) {
      ret = 1;
    } else {
      ret = 0;
      done++;
    }
  }

  logprintfl(EUCADEBUG,"BundleInstance(): done.\n");
  
  shawn();
  
  return(ret);
}

int doCancelBundleTask(ncMetadata *ccMeta, char *instanceId) {
  int i, j, rc, start = 0, stop = 0, ret=0, done, timeout;
  ccInstance *myInstance;
  ncStub *ncs;
  time_t op_start;
  ccResourceCache resourceCacheLocal;

  i = j = 0;
  myInstance = NULL;
  op_start = time(NULL);
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO, "CancelBundleTask(): called\n");
  logprintfl(EUCADEBUG, "CancelBundleTask(): params: userId=%s, instanceId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(instanceId));
  if (!instanceId) {
    logprintfl(EUCAERROR, "CancelBundleTask(): bad input params\n");
    return(1);
  }
  
  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
    
  rc = find_instanceCacheId(instanceId, &myInstance);
  if (!rc) {
    // found the instance in the cache
    if (myInstance) {
      start = myInstance->ncHostIdx;
      stop = start+1;
      free(myInstance);
    }
  } else {
    start = 0;
    stop = resourceCacheLocal.numResources;
  }
  
  done=0;
  for (j=start; j<stop && !done; j++) {
    timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop-start, j);
    rc = ncClientCall(ccMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncCancelBundleTask", instanceId);
    if (rc) {
      ret = 1;
    } else {
      ret = 0;
      done++;
    }
  }

  logprintfl(EUCADEBUG,"CancelBundleTask(): done.\n");
  
  shawn();
  
  return(ret);
}

/*
int doDescribeBundleTasks(ncMetadata *ccMeta, char **instIds, int instIdsLen, bundleTask **outBundleTasks, int *outBundleTasksLen) {
  int i, j, k, rc, start = 0, stop = 0, ret=0, count=0;
  ccInstance *myInstance;
  ncStub *ncs;
  time_t op_start, op_timer;
  char *instanceId;
  
  op_start = time(NULL);
  op_timer = OP_TIMEOUT;
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }

  logprintfl(EUCAINFO, "DescribeBundleTasks(): called\n");
  logprintfl(EUCADEBUG, "DescribeBundleTasks(): params: userId=%s, instIdsLen=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), instIdsLen);

  *outBundleTasks = NULL;
  *outBundleTasksLen = 0;

  sem_mywait(INSTCACHE);
  count=0;
  if(instanceCache->numInsts) {
    *outBundleTasks = malloc(sizeof(bundleTask) * instanceCache->numInsts);
    if (!*outBundleTasks) {
      logprintfl(EUCAFATAL, "doDescribeBundleTasks(): out of memory!\n");
      unlock_exit(1);
    }
    
    for (i=0; i<MAXINSTANCES; i++) {
      if (instanceCache->cacheState[i] == INSTVALID) {
	if (count >= instanceCache->numInsts) {
	  logprintfl(EUCAWARN, "doDescribeBundleTasks(): found more bundles than reported by numBundles, will only report a subset of bundles\n");
	  count=0;
	}
	snprintf((*outBundleTasks)[count].instanceId, CHAR_BUFFER_SIZE, "%s", instanceCache->instances[i].instanceId);
	snprintf((*outBundleTasks)[count].state, 64, "%s", instanceCache->instances[i].bundleTaskStateName);
	(*outBundleTasksLen)++;
	count++;
      }
    }
  }
  sem_mypost(INSTCACHE);

  for (i=0; i< (*outBundleTasksLen) ; i++) {
    logprintfl(EUCADEBUG, "DescribeBundleTasks(): returning: instanceId=%s, state=%s\n", (*outBundleTasks)[i].instanceId, (*outBundleTasks)[i].state);
  }

  logprintfl(EUCADEBUG,"DescribeBundleTasks(): done.\n");
  
  shawn();
  
  return(ret);
}
*/

int ncClientCall(ncMetadata *meta, int timeout, int ncLock, char *ncURL, char *ncOp, ...) {
  va_list al;
  int pid, rc=0, ret=0, status=0, opFail=0, len, rbytes, i;
  int filedes[2];

  logprintfl(EUCADEBUG, "ncClientCall(%s): called ncURL=%s timeout=%d\n", ncOp, ncURL, timeout);
  
  rc = pipe(filedes);
  if (rc) {
    logprintfl(EUCAERROR, "ncClientCall(%s): cannot create pipe\n", ncOp);
    return(1);
  }

  va_start(al, ncOp);

  // grab the lock
  sem_mywait(ncLock);

  pid = fork();
  if (!pid) {
    ncStub *ncs;

    close(filedes[0]);
    ncs = ncStubCreate(ncURL, NULL, NULL);
    if (config->use_wssec) {
      rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
    }
              
    logprintfl(EUCADEBUG, "\tncClientCall(%s): ppid=%d client calling '%s'\n", ncOp, getppid(), ncOp);
    if (!strcmp(ncOp, "ncGetConsoleOutput")) {
      // args: char *instId
      char *instId = va_arg(al, char *);
      char **consoleOutput=va_arg(al, char **);

      rc = ncGetConsoleOutputStub(ncs, meta, instId, consoleOutput);
      if (timeout && consoleOutput) {
	if (!rc && *consoleOutput) {
	  len = strlen(*consoleOutput) + 1;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = write(filedes[1], *consoleOutput, sizeof(char) * len);
	  rc = 0;
	} else {
	  len = 0;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = 1;
	}
      }
    } else if (!strcmp(ncOp, "ncAttachVolume")) {
      char *instanceId = va_arg(al, char *);
      char *volumeId = va_arg(al, char *);      
      char *remoteDev = va_arg(al, char *);      
      char *localDev = va_arg(al, char *);      

      rc = ncAttachVolumeStub(ncs, meta, instanceId, volumeId, remoteDev, localDev);
    } else if (!strcmp(ncOp, "ncDetachVolume")) {
      char *instanceId = va_arg(al, char *);
      char *volumeId = va_arg(al, char *);      
      char *remoteDev = va_arg(al, char *);      
      char *localDev = va_arg(al, char *);      
      int force = va_arg(al, int);

      rc = ncDetachVolumeStub(ncs, meta, instanceId, volumeId, remoteDev, localDev, force);
    } else if (!strcmp(ncOp, "ncCreateImage")) {
      char *instanceId = va_arg(al, char *);
      char *volumeId = va_arg(al, char *);      
      char *remoteDev = va_arg(al, char *);      

      rc = ncCreateImageStub(ncs, meta, instanceId, volumeId, remoteDev);
    } else if (!strcmp(ncOp, "ncPowerDown")) {
      rc = ncPowerDownStub(ncs, meta);
    } else if (!strcmp(ncOp, "ncAssignAddress")) {
      char *instanceId = va_arg(al, char *);
      char *publicIp = va_arg(al, char *);

      rc = ncAssignAddressStub(ncs, meta, instanceId, publicIp);
      //rc = 0;
    } else if (!strcmp(ncOp, "ncRebootInstance")) {
      char *instId = va_arg(al, char *);

      rc = ncRebootInstanceStub(ncs, meta, instId);
    } else if (!strcmp(ncOp, "ncTerminateInstance")) {
      char *instId = va_arg(al, char *);
      int *shutdownState = va_arg(al, int *);
      int *previousState = va_arg(al, int *);
      
      rc = ncTerminateInstanceStub(ncs, meta, instId, shutdownState, previousState);
      if (timeout) {
	if (!rc) {
	  len = 2;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = write(filedes[1], shutdownState, sizeof(int));
	  rc = write(filedes[1], previousState, sizeof(int));
	  rc = 0;
	} else {
	  len = 0;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = 1;
	}
      }
    } else if (!strcmp(ncOp, "ncStartNetwork")) {
      char *uuid = va_arg(al, char *);
      char **peers = va_arg(al, char **);
      int peersLen = va_arg(al, int);
      int port = va_arg(al, int);
      int vlan = va_arg(al, int);
      char **outStatus = va_arg(al, char **);
      
      rc = ncStartNetworkStub(ncs, meta, uuid, peers, peersLen, port, vlan, outStatus);
      if (timeout && outStatus) {
	if (!rc && *outStatus) {
	  len = strlen(*outStatus) + 1;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = write(filedes[1], *outStatus, sizeof(char) * len);
	  rc = 0;
	} else {
	  len = 0;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = 1;
	}      
      }
    } else if (!strcmp(ncOp, "ncRunInstance")) {
      char *uuid = va_arg(al, char *);
      char *instId = va_arg(al, char *);
      char *reservationId = va_arg(al, char *);
      virtualMachine *ncvm = va_arg(al, virtualMachine *);
      char *imageId = va_arg(al, char *);
      char *imageURL = va_arg(al, char *);
      char *kernelId = va_arg(al, char *);
      char *kernelURL = va_arg(al, char *);
      char *ramdiskId = va_arg(al, char *);
      char *ramdiskURL = va_arg(al, char *);
      char *keyName = va_arg(al, char *);
      netConfig *ncnet = va_arg(al, netConfig *);
      char *userData = va_arg(al, char *);
      char *launchIndex = va_arg(al, char *);
      char *platform = va_arg(al, char *);
      int expiryTime = va_arg(al, int);
      char **netNames = va_arg(al, char **);
      int netNamesLen = va_arg(al, int);
      ncInstance **outInst = va_arg(al, ncInstance **);
      
      rc = ncRunInstanceStub(ncs, meta, uuid, instId, reservationId, ncvm, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, ncnet, userData, launchIndex, platform, expiryTime, netNames, netNamesLen, outInst);
      if (timeout && outInst) {
	if (!rc && *outInst) {
	  len = sizeof(ncInstance);
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = write(filedes[1], *outInst, sizeof(ncInstance));
	  rc = 0;
	} else {
	  len = 0;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = 1;
	}
      }
    } else if (!strcmp(ncOp, "ncDescribeInstances")) {
      char **instIds = va_arg(al, char **);
      int instIdsLen = va_arg(al, int);
      ncInstance ***ncOutInsts=va_arg(al, ncInstance ***);
      int *ncOutInstsLen= va_arg(al, int *);

      rc = ncDescribeInstancesStub(ncs, meta, instIds, instIdsLen, ncOutInsts, ncOutInstsLen);
      if (timeout && ncOutInsts && ncOutInstsLen) {
	if (!rc) {
	  len = *ncOutInstsLen;
	  rc = write(filedes[1], &len, sizeof(int));
	  for (i=0; i<len; i++) {
	    ncInstance *inst;
	    inst = (*ncOutInsts)[i];
	    rc = write(filedes[1], inst, sizeof(ncInstance));
	  }
	  rc = 0;
	} else {
	  len = 0;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = 1;
	}      
      }
    } else if (!strcmp(ncOp, "ncDescribeResource")) {
      char *resourceType = va_arg(al, char *);
      ncResource **outRes=va_arg(al, ncResource **);

      rc = ncDescribeResourceStub(ncs, meta, resourceType, outRes);
      if (timeout && outRes) {
	if (!rc && *outRes) {
	  len = sizeof(ncResource);
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = write(filedes[1], *outRes, sizeof(ncResource));
	  rc = 0;
	} else {
	  len = 0;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = 1;
	}      
      }
    } else if (!strcmp(ncOp, "ncBundleInstance")) {
      char *instanceId = va_arg(al, char *);
      char *bucketName = va_arg(al, char *);      
      char *filePrefix = va_arg(al, char *);      
      char *walrusURL = va_arg(al, char *);
      char *userPublicKey = va_arg(al, char *);
      char *S3Policy = va_arg(al, char *);
      char *S3PolicySig = va_arg(al, char *);      

      rc = ncBundleInstanceStub(ncs, meta, instanceId, bucketName, filePrefix, walrusURL, userPublicKey, S3Policy, S3PolicySig);
    } else if (!strcmp(ncOp, "ncCancelBundleTask")) {
      char *instanceId = va_arg(al, char *);

      rc = ncCancelBundleTaskStub(ncs, meta, instanceId);
    } else {
      logprintfl(EUCAWARN, "\tncClientCall(%s): ppid=%d operation '%s' not found\n", ncOp, getppid(), ncOp);
      rc = 1;
    }
    logprintfl(EUCADEBUG, "\tncClientCall(%s): ppid=%d done calling '%s' with exit code '%d'\n", ncOp, getppid(), ncOp, rc);
    if (rc) {
      ret = 1;
    } else {
      ret = 0;
    }
    close(filedes[1]);
    exit(ret);
  } else {
    // returns for each client call
    close(filedes[1]);

    if (!strcmp(ncOp, "ncGetConsoleOutput")) {
      char *instId = va_arg(al, char *);
      char **outConsoleOutput = va_arg(al, char **);
      if (outConsoleOutput) {
	*outConsoleOutput = NULL;
      }
      if (timeout && outConsoleOutput) {
	rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
	if (rbytes <= 0) {
	  kill(pid, SIGKILL);
	  opFail=1;
	} else {
	  *outConsoleOutput = malloc(sizeof(char) * len);
	  if (!*outConsoleOutput) {
	    logprintfl(EUCAFATAL, "ncClientCall(%s): out of memory!\n", ncOp);
	    unlock_exit(1);
	  }
	  rbytes = timeread(filedes[0], *outConsoleOutput, len, timeout);
	  if (rbytes <= 0) {
	    kill(pid, SIGKILL);
	    opFail=1;
	  }
	}
      }
    } else if (!strcmp(ncOp, "ncTerminateInstance")) {
      char *instId = va_arg(al, char *);
      int *shutdownState = va_arg(al, int *);
      int *previousState = va_arg(al, int *);
      if (shutdownState && previousState) {
	*shutdownState = *previousState = 0;
      }
      if (timeout && shutdownState && previousState) {
	rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
	if (rbytes <= 0) {
	  kill(pid, SIGKILL);
	  opFail=1;
	} else {
	  rbytes = timeread(filedes[0], shutdownState, sizeof(int), timeout);
	  if (rbytes <= 0) {
	    kill(pid, SIGKILL);
	    opFail=1;
	  }
	  rbytes = timeread(filedes[0], previousState, sizeof(int), timeout);
	  if (rbytes <= 0) {
	    kill(pid, SIGKILL);
	    opFail=1;
	  }
	}
      }
    } else if (!strcmp(ncOp, "ncStartNetwork")) {
      char *uuid = va_arg(al, char *);
      char **peers = va_arg(al, char **);
      int peersLen = va_arg(al, int);
      int port = va_arg(al, int);
      int vlan = va_arg(al, int);
      char **outStatus = va_arg(al, char **);
      if (outStatus) {
	*outStatus = NULL;
      }
      if (timeout && outStatus) {
	*outStatus = NULL;
	rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
	if (rbytes <= 0) {
	  kill(pid, SIGKILL);
	  opFail=1;
	} else {
	  *outStatus = malloc(sizeof(char) * len);
	  if (!*outStatus) {
	    logprintfl(EUCAFATAL, "ncClientCall(%s): out of memory!\n", ncOp);
	    unlock_exit(1);
	  }
	  rbytes = timeread(filedes[0], *outStatus, len, timeout);
	  if (rbytes <= 0) {
	    kill(pid, SIGKILL);
	    opFail=1;
	  }
	}
      }
    } else if (!strcmp(ncOp, "ncRunInstance")) {
      char *uuid = va_arg(al, char *);
      char *instId = va_arg(al, char *);
      char *reservationId = va_arg(al, char *);
      virtualMachine *ncvm = va_arg(al, virtualMachine *);
      char *imageId = va_arg(al, char *);
      char *imageURL = va_arg(al, char *);
      char *kernelId = va_arg(al, char *);
      char *kernelURL = va_arg(al, char *);
      char *ramdiskId = va_arg(al, char *);
      char *ramdiskURL = va_arg(al, char *);
      char *keyName = va_arg(al, char *);
      netConfig *ncnet = va_arg(al, netConfig *);
      char *userData = va_arg(al, char *);
      char *launchIndex = va_arg(al, char *);
      char *platform = va_arg(al, char *);
      int expiryTime = va_arg(al, int);
      char **netNames = va_arg(al, char **);
      int netNamesLen = va_arg(al, int);
      ncInstance **outInst = va_arg(al, ncInstance **);
      if (outInst) {
	*outInst = NULL;
      }
      if (timeout && outInst) {
	rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
	if (rbytes <= 0) {
	  kill(pid, SIGKILL);
	  opFail=1;
	} else {
	  *outInst = malloc(sizeof(ncInstance));
	  if (!*outInst) {
	    logprintfl(EUCAFATAL, "ncClientCall(%s): out of memory!\n", ncOp);
	    unlock_exit(1);
	  }
	  rbytes = timeread(filedes[0], *outInst, sizeof(ncInstance), timeout);
	  if (rbytes <= 0) {
	    kill(pid, SIGKILL);
	    opFail=1;
	  }
	}
      }
    } else if (!strcmp(ncOp, "ncDescribeInstances")) {
      char **instIds = va_arg(al, char **);
      int instIdsLen = va_arg(al, int);
      ncInstance ***ncOutInsts=va_arg(al, ncInstance ***);
      int *ncOutInstsLen=va_arg(al, int *);
      if (ncOutInstsLen && ncOutInsts) {
	*ncOutInstsLen = 0;
	*ncOutInsts = NULL;
      }
      if (timeout && ncOutInsts && ncOutInstsLen) {
	rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
	if (rbytes <= 0) {
	  kill(pid, SIGKILL);
	  opFail=1;
	} else {
	  *ncOutInsts = malloc(sizeof(ncInstance *) * len);
	  if (!*ncOutInsts) {
	    logprintfl(EUCAFATAL, "ncClientCall(%s): out of memory!\n", ncOp);
	    unlock_exit(1);
	  }
	  *ncOutInstsLen = len;
	  for (i=0; i<len; i++) {
	    ncInstance *inst;
	    inst = malloc(sizeof(ncInstance));
	    if (!inst) {
	      logprintfl(EUCAFATAL, "ncClientCall(%s): out of memory!\n", ncOp);
	      unlock_exit(1);
	    }
	    rbytes = timeread(filedes[0], inst, sizeof(ncInstance), timeout);
	    (*ncOutInsts)[i] = inst;
	  }
	}
      }
    } else if (!strcmp(ncOp, "ncDescribeResource")) {
      char *resourceType = va_arg(al, char *);
      ncResource **outRes=va_arg(al, ncResource **);
      if (outRes) {
	*outRes = NULL;
      }
      if (timeout && outRes) {
	rbytes = timeread(filedes[0], &len, sizeof(int), timeout);
	if (rbytes <= 0) {
	  kill(pid, SIGKILL);
	  opFail=1;
	} else {
	  *outRes = malloc(sizeof(ncResource));
	  if (!outRes) {
	    logprintfl(EUCAFATAL, "ncClientCall(%s): out of memory!\n", ncOp);
	    unlock_exit(1);
	  }
	  rbytes = timeread(filedes[0], *outRes, sizeof(ncResource), timeout);
	  if (rbytes <= 0) {
	    kill(pid, SIGKILL);
	    opFail=1;
	  }
	}
      }
    } else {
      // nothing to do in default case (succ/fail encoded in exit code)
    }
    
    close(filedes[0]);
    if (timeout) {
      rc = timewait(pid, &status, timeout);
      rc = WEXITSTATUS(status);
    } else {
      rc = 0;
    }
  }

  logprintfl(EUCADEBUG, "ncClientCall(%s): done clientrc=%d opFail=%d\n", ncOp, rc, opFail);
  if (rc || opFail) {
    ret = 1;
  } else {
    ret = 0;
  }
  
  // release the lock
  sem_mypost(ncLock);
  
  va_end(al);
  
  return(ret);
}

// calculate nc call timeout, based on when operation was started (op_start), the total number of calls to make (numCalls), and the current progress (idx)
int ncGetTimeout(time_t op_start, time_t op_max, int numCalls, int idx) {
  time_t op_timer, op_pernode;
  int numLeft;

  numLeft = numCalls - idx;
  if ( numLeft <= 0 ) {
    numLeft = 1;
  }
 
  op_timer = op_max - (time(NULL) - op_start);
  op_pernode = op_timer / numLeft;

  return(maxint(minint(op_pernode, OP_TIMEOUT_PERNODE), OP_TIMEOUT_MIN));
}

int doAttachVolume(ncMetadata *ccMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev) {
  int i, j, rc, start = 0, stop = 0, ret=0, done=0, timeout;
  ccInstance *myInstance;
  ncStub *ncs;
  time_t op_start;
  ccResourceCache resourceCacheLocal;
  
  i = j = 0;
  myInstance = NULL;
  op_start = time(NULL);
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "AttachVolume(): called\n");
  logprintfl(EUCADEBUG, "AttachVolume(): params: userId=%s, volumeId=%s, instanceId=%s, remoteDev=%s, localDev=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(volumeId), SP(instanceId), SP(remoteDev), SP(localDev));
  if (!volumeId || !instanceId || !remoteDev || !localDev) {
    logprintfl(EUCAERROR, "AttachVolume(): bad input params\n");
    return(1);
  }
  
  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
  
  rc = find_instanceCacheId(instanceId, &myInstance);
  if (!rc) {
    // found the instance in the cache
    if (myInstance) {
      start = myInstance->ncHostIdx;
      stop = start+1;
      free(myInstance);
    }
  } else {
    start = 0;
    stop = resourceCacheLocal.numResources;
  }
  
  done=0;
  for (j=start; j<stop && !done; j++) {
    timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop-start, j);
    rc = ncClientCall(ccMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncAttachVolume", instanceId, volumeId, remoteDev, localDev);
    if (rc) {
      ret = 1;
    } else {
      ret = 0;
      done++;
    }
  }
  
  logprintfl(EUCADEBUG,"AttachVolume(): done.\n");
  
  shawn();

  return(ret);
}

int doDetachVolume(ncMetadata *ccMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev, int force) {
  int i, j, rc, start = 0, stop = 0, ret=0, done=0, timeout;
  ccInstance *myInstance;
  ncStub *ncs;
  time_t op_start;
  ccResourceCache resourceCacheLocal;
  i = j = 0;
  myInstance = NULL;
  op_start = time(NULL);
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO, "DetachVolume(): called\n");
  logprintfl(EUCADEBUG, "DetachVolume(): params: userId=%s, volumeId=%s, instanceId=%s, remoteDev=%s, localDev=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(volumeId), SP(instanceId), SP(remoteDev), SP(localDev));
  if (!volumeId || !instanceId || !remoteDev || !localDev) {
    logprintfl(EUCAERROR, "DetachVolume(): bad input params\n");
    return(1);
  }

  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
  
  rc = find_instanceCacheId(instanceId, &myInstance);
  if (!rc) {
    // found the instance in the cache
    if (myInstance) {
      start = myInstance->ncHostIdx;
      stop = start+1;
      free(myInstance);
    }
  } else {
    start = 0;
    stop = resourceCacheLocal.numResources;
  }
  
  for (j=start; j<stop; j++) {
    timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop-start, j);
    rc = ncClientCall(ccMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncDetachVolume", instanceId, volumeId, remoteDev, localDev);
    if (rc) {
      ret = 1;
    } else {
      ret = 0;
      done++;
    }
  }

  logprintfl(EUCADEBUG,"DetachVolume(): done.\n");
  
  shawn();
  
  return(ret);
}

int doConfigureNetwork(ncMetadata *ccMeta, char *type, int namedLen, char **sourceNames, char **userNames, int netLen, char **sourceNets, char *destName, char *destUserName, char *protocol, int minPort, int maxPort) {
  int rc, i, fail;

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "ConfigureNetwork(): called\n");
  logprintfl(EUCADEBUG, "ConfigureNetwork(): params: userId=%s, type=%s, namedLen=%d, netLen=%d, destName=%s, destUserName=%s, protocol=%s, minPort=%d, maxPort=%d\n", ccMeta ? SP(ccMeta->userId) : "UNSET", SP(type), namedLen, netLen, SP(destName), SP(destUserName), SP(protocol), minPort, maxPort);
  
  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC")) {
    fail = 0;
  } else {
    
    if ( destUserName == NULL ) {
      if ( (ccMeta && ccMeta->userId) ) {
	destUserName = ccMeta->userId;
      } else {
	// destUserName is not set, return fail
	logprintfl(EUCAERROR, "ConfigureNetwork(): cannot set destUserName from ccMeta or input\n");
	return(1);
      }
    }
    
    sem_mywait(VNET);
    
    fail=0;
    for (i=0; i<namedLen; i++) {
      if (sourceNames && userNames) {
	rc = vnetTableRule(vnetconfig, type, destUserName, destName, userNames[i], NULL, sourceNames[i], protocol, minPort, maxPort);
      }
      if (rc) {
	logprintfl(EUCAERROR,"ERROR: vnetTableRule() returned error\n");
	fail=1;
      }
    }
    for (i=0; i<netLen; i++) {
      if (sourceNets) {
	rc = vnetTableRule(vnetconfig, type, destUserName, destName, NULL, sourceNets[i], NULL, protocol, minPort, maxPort);
      }
      if (rc) {
	logprintfl(EUCAERROR,"ERROR: vnetTableRule() returned error\n");
	fail=1;
      }
    }
    sem_mypost(VNET);
  }
  
  logprintfl(EUCADEBUG,"ConfigureNetwork(): done\n");
  
  shawn();
  
  if (fail) {
    return(1);
  }
  return(0);
}

int doFlushNetwork(ncMetadata *ccMeta, char *destName) {
  int rc;

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
    return(0);
  }

  sem_mywait(VNET);
  rc = vnetFlushTable(vnetconfig, ccMeta->userId, destName);
  sem_mypost(VNET);
  return(rc);
}

int doAssignAddress(ncMetadata *ccMeta, char *uuid, char *src, char *dst) {
  int rc, allocated, addrdevno, ret;
  char cmd[MAX_PATH];
  ccInstance *myInstance=NULL;

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO,"AssignAddress(): called\n");
  logprintfl(EUCADEBUG,"AssignAddress(): params: src=%s, dst=%s\n", SP(src), SP(dst));

  if (!src || !dst || !strcmp(src, "0.0.0.0")) {
    logprintfl(EUCADEBUG, "AssignAddress(): bad input params\n");
    return(1);
  }
  
  ret = 0;
  
  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
    ret = 0;
  } else {
    
    sem_mywait(VNET);

    ret = vnetReassignAddress(vnetconfig, uuid, src, dst);
    if (ret) {
      logprintfl(EUCAERROR, "doAssignAddress(): vnetReassignAddress() failed\n");
      ret = 1;
    }

    /*
    rc = vnetGetPublicIP(vnetconfig, src, NULL, &allocated, &addrdevno);
    if (rc) {
      logprintfl(EUCAERROR,"AssignAddress(): failed to retrieve publicip record %s\n", src);
      ret = 1;
    } else {
      if (!allocated) {
	snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr add %s/32 dev %s", config->eucahome, src, vnetconfig->pubInterface);
	logprintfl(EUCADEBUG,"running cmd %s\n", cmd);
	rc = system(cmd);
	rc = rc>>8;
	if (rc && (rc != 2)) {
	  logprintfl(EUCAERROR,"AssignAddress(): cmd '%s' failed\n", cmd);
	  ret = 1;
	} else {
	  rc = vnetAssignAddress(vnetconfig, src, dst);
	  if (rc) {
	    logprintfl(EUCAERROR,"AssignAddress(): vnetAssignAddress() failed\n");
	    ret = 1;
	  } else {
	    rc = vnetAllocatePublicIP(vnetconfig, uuid, src, dst);
	    if (rc) {
	      logprintfl(EUCAERROR,"AssignAddress(): vnetAllocatePublicIP() failed\n");
	      ret = 1;
	    }
	  }
	}
      } else {
	logprintfl(EUCAWARN,"AssignAddress(): ip %s is already assigned, ignoring\n", src);
	ret = 0;
      }
    }
    */
    sem_mypost(VNET);
  }
  
  if (!ret && strcmp(dst, "0.0.0.0")) {
    // everything worked, update instance cache

    rc = map_instanceCache(privIpCmp, dst, pubIpSet, src);
    if (rc) {
      logprintfl(EUCAERROR, "AssignAddress(): map_instanceCache() failed to assign %s->%s\n", dst, src);
    }
  }
  
  logprintfl(EUCADEBUG,"AssignAddress(): done\n");  
  
  shawn();

  return(ret);
}

int doDescribePublicAddresses(ncMetadata *ccMeta, publicip **outAddresses, int *outAddressesLen) {
  int rc, ret;
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "DescribePublicAddresses(): called\n");
  logprintfl(EUCADEBUG, "DescribePublicAddresses(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  ret=0;
  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    *outAddresses = vnetconfig->publicips;
    *outAddressesLen = NUMBER_OF_PUBLIC_IPS;
  } else {
    *outAddresses = NULL;
    *outAddressesLen = 0;
    ret=2;
  }
  
  logprintfl(EUCADEBUG, "DescribePublicAddresses(): done\n");

  shawn();

  return(ret);
}

int doUnassignAddress(ncMetadata *ccMeta, char *src, char *dst) {
  int rc, allocated, addrdevno, ret;
  char cmd[MAX_PATH];
  ccInstance *myInstance=NULL;

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO,"UnassignAddress(): called\n");
  logprintfl(EUCADEBUG,"UnassignAddress(): params: userId=%s, src=%s, dst=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(src), SP(dst));  
  
  if (!src || !dst || !strcmp(src, "0.0.0.0")) {
    logprintfl(EUCADEBUG, "UnassignAddress(): bad input params\n");
    return(1);
  }

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
    ret = 0;
  } else {
    
    sem_mywait(VNET);

    ret = vnetReassignAddress(vnetconfig, "UNSET", src, "0.0.0.0");
    if (ret) {
      logprintfl(EUCAERROR, "UnassignAddress(): vnetReassignAddress() failed\n");
      ret = 1;
    }
    /*
    ret=0;
    rc = vnetGetPublicIP(vnetconfig, src, NULL, &allocated, &addrdevno);
    if (rc) {
      logprintfl(EUCAERROR,"UnassignAddress(): failed to find publicip to unassign (%s)\n", src);
      ret=1;
    } else {
      if (allocated && dst) {
	rc = vnetUnassignAddress(vnetconfig, src, dst); 
	if (rc) {
	  logprintfl(EUCAWARN,"vnetUnassignAddress() failed %d: %s/%s\n", rc, src, dst);
	}
	
	rc = vnetDeallocatePublicIP(vnetconfig, NULL, src, dst);
	if (rc) {
	  logprintfl(EUCAWARN,"vnetDeallocatePublicIP() failed %d: %s\n", rc, src);
	}
      }
      

      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del %s/32 dev %s", config->eucahome, src, vnetconfig->pubInterface);
      logprintfl(EUCADEBUG, "UnassignAddress(): running cmd '%s'\n", cmd);
      rc = system(cmd);
      if (rc) {
      	logprintfl(EUCAWARN,"UnassignAddress(): cmd failed '%s'\n", cmd);
      }
    }
    */
    sem_mypost(VNET);
  }

  if (!ret) {
    // refresh instance cache
    rc = map_instanceCache(pubIpCmp, src, pubIpSet, "0.0.0.0");
    if (rc) {
      logprintfl(EUCAERROR, "UnassignAddress(): map_instanceCache() failed to assign %s->%s\n", dst, src);
    }
  }
  
  logprintfl(EUCADEBUG,"UnassignAddress(): done\n");  
  
  shawn();

  return(ret);
}

int doStopNetwork(ncMetadata *ccMeta, char *netName, int vlan) {
  int rc, ret;
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "StopNetwork(): called\n");
  logprintfl(EUCADEBUG, "StopNetwork(): params: userId=%s, netName=%s, vlan=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(netName), vlan);
  if (!ccMeta || !netName || vlan < 0) {
    logprintfl(EUCAERROR, "StopNetwork(): bad input params\n");
  }

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
    ret = 0;
  } else {
    
    sem_mywait(VNET);
    if(ccMeta != NULL) {
      rc = vnetStopNetwork(vnetconfig, vlan, ccMeta->userId, netName);
    }
    ret = rc;
    sem_mypost(VNET);
  }
  
  logprintfl(EUCADEBUG,"StopNetwork(): done\n");

  shawn();
  
  return(ret);
}

int doDescribeNetworks(ncMetadata *ccMeta, char *nameserver, char **ccs, int ccsLen, vnetConfig *outvnetConfig) {
  int rc, i, j;
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }

  logprintfl(EUCAINFO, "DescribeNetworks(): called\n");
  logprintfl(EUCADEBUG, "DescribeNetworks(): params: userId=%s, nameserver=%s, ccsLen=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(nameserver), ccsLen);
  
  sem_mywait(VNET);
  if (nameserver) {
    vnetconfig->euca_ns = dot2hex(nameserver);
  }
  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    rc = vnetSetCCS(vnetconfig, ccs, ccsLen);
    rc = vnetSetupTunnels(vnetconfig);
  }
  memcpy(outvnetConfig, vnetconfig, sizeof(vnetConfig));

  sem_mypost(VNET);
  logprintfl(EUCADEBUG, "DescribeNetworks(): done\n");
  
  shawn();

  return(0);
}

int doStartNetwork(ncMetadata *ccMeta, char *uuid, char *netName, int vlan, char *nameserver, char **ccs, int ccsLen) {
  int rc, ret;
  time_t op_start;
  char *brname;
  
  op_start = time(NULL);

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "StartNetwork(): called\n");
  logprintfl(EUCADEBUG, "StartNetwork(): params: userId=%s, netName=%s, vlan=%d, nameserver=%s, ccsLen=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(netName), vlan, SP(nameserver), ccsLen);

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
    ret = 0;
  } else {
    sem_mywait(VNET);
    if (nameserver) {
      vnetconfig->euca_ns = dot2hex(nameserver);
    }
    
    rc = vnetSetCCS(vnetconfig, ccs, ccsLen);
    rc = vnetSetupTunnels(vnetconfig);

    brname = NULL;
    rc = vnetStartNetwork(vnetconfig, vlan, uuid, ccMeta->userId, netName, &brname);
    if (brname) free(brname);

    sem_mypost(VNET);
    
    if (rc) {
      logprintfl(EUCAERROR,"StartNetwork(): vnetStartNetwork() failed (%d)\n", rc);
      ret = 1;
    } else {
      ret = 0;
    }
    
  }
  
  logprintfl(EUCADEBUG,"StartNetwork(): done\n");
  
  shawn();  

  return(ret);
}

int doDescribeResources(ncMetadata *ccMeta, virtualMachine **ccvms, int vmLen, int **outTypesMax, int **outTypesAvail, int *outTypesLen, ccResource **outNodes, int *outNodesLen) {
  int i;
  int rc, diskpool, mempool, corepool;
  int j;
  ccResource *res;
  time_t op_start;
  ccResourceCache resourceCacheLocal;
  char strbuf[4096];

  logprintfl(EUCAINFO,"DescribeResources(): called\n");
  logprintfl(EUCADEBUG,"DescribeResources(): params: userId=%s, vmLen=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), vmLen);
  op_start = time(NULL);

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  if (outTypesMax == NULL || outTypesAvail == NULL || outTypesLen == NULL || outNodes == NULL || outNodesLen == NULL) {
    // input error
    return(1);
  }
  
  *outTypesMax = NULL;
  *outTypesAvail = NULL;
  
  *outTypesMax = malloc(sizeof(int) * vmLen);
  *outTypesAvail = malloc(sizeof(int) * vmLen);
  if (*outTypesMax == NULL || *outTypesAvail == NULL) {
      logprintfl(EUCAERROR,"DescribeResources(): out of memory\n");
      unlock_exit(1);
  }
  bzero(*outTypesMax, sizeof(int) * vmLen);
  bzero(*outTypesAvail, sizeof(int) * vmLen);

  *outTypesLen = vmLen;

  for (i=0; i<vmLen; i++) {
    if ((*ccvms)[i].mem <= 0 || (*ccvms)[i].cores <= 0 || (*ccvms)[i].disk <= 0) {
      logprintfl(EUCAERROR,"DescribeResources(): input error\n");
      if (*outTypesAvail) free(*outTypesAvail);
      if (*outTypesMax) free(*outTypesMax);
      *outTypesLen = 0;
      return(1);
    }
  }

  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
  {
    *outNodes = malloc(sizeof(ccResource) * resourceCacheLocal.numResources);
    bzero(*outNodes, sizeof(ccResource) * resourceCacheLocal.numResources);
    if (*outNodes == NULL) {
      logprintfl(EUCAFATAL,"DescribeResources(): out of memory!\n");
      unlock_exit(1);
    } else {
      memcpy(*outNodes, resourceCacheLocal.resources, sizeof(ccResource) * resourceCacheLocal.numResources);
      *outNodesLen = resourceCacheLocal.numResources;
    }

    for (i=0; i<resourceCacheLocal.numResources; i++) {
      res = &(resourceCacheLocal.resources[i]);
      
      for (j=0; j<vmLen; j++) {
	mempool = res->availMemory;
	diskpool = res->availDisk;
	corepool = res->availCores;
	
	mempool -= (*ccvms)[j].mem;
	diskpool -= (*ccvms)[j].disk;
	corepool -= (*ccvms)[j].cores;
	while (mempool >= 0 && diskpool >= 0 && corepool >= 0) {
	  (*outTypesAvail)[j]++;
	  mempool -= (*ccvms)[j].mem;
	  diskpool -= (*ccvms)[j].disk;
	  corepool -= (*ccvms)[j].cores;
	}
	
	mempool = res->maxMemory;
	diskpool = res->maxDisk;
	corepool = res->maxCores;
	
	mempool -= (*ccvms)[j].mem;
	diskpool -= (*ccvms)[j].disk;
	corepool -= (*ccvms)[j].cores;
	while (mempool >= 0 && diskpool >= 0 && corepool >= 0) {
	  (*outTypesMax)[j]++;
	  mempool -= (*ccvms)[j].mem;
	  diskpool -= (*ccvms)[j].disk;
	  corepool -= (*ccvms)[j].cores;
	}
      }
    }
  }
  
  if (vmLen >= 5) {
    logprintfl(EUCAINFO,"DescribeResources(): resource response summary (name{avail/max}): %s{%d/%d} %s{%d/%d} %s{%d/%d} %s{%d/%d} %s{%d/%d}\n", (*ccvms)[0].name, (*outTypesAvail)[0], (*outTypesMax)[0], (*ccvms)[1].name, (*outTypesAvail)[1], (*outTypesMax)[1], (*ccvms)[2].name, (*outTypesAvail)[2], (*outTypesMax)[2], (*ccvms)[3].name, (*outTypesAvail)[3], (*outTypesMax)[3], (*ccvms)[4].name, (*outTypesAvail)[4], (*outTypesMax)[4]);
  }

  logprintfl(EUCADEBUG,"DescribeResources(): done\n");
  
  shawn();

  return(0);
}

int changeState(ccResource *in, int newstate) {
  if (in == NULL) return(1);
  if (in->state == newstate) return(0);
  
  in->lastState = in->state;
  in->state = newstate;
  in->stateChange = time(NULL);
  in->idleStart = 0;
  
  return(0);
}

int refresh_resources(ncMetadata *ccMeta, int timeout, int dolock) {
  int i, rc, nctimeout, pid, *pids=NULL;
  int status, ret=0;
  time_t op_start;
  ncStub *ncs;
  ncResource *ncResDst=NULL;

  if (timeout <= 0) timeout = 1;
  
  op_start = time(NULL);
  logprintfl(EUCAINFO,"refresh_resources(): called\n");

  rc = update_config();
  if (rc) {
    logprintfl(EUCAWARN, "refresh_resources(): bad return from update_config(), check your config file\n");
  }
  
  // critical NC call section
  sem_mywait(RESCACHE);
  //  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  memcpy(resourceCacheStage, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);

  sem_close(locks[REFRESHLOCK]);
  locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
  
  pids = malloc(sizeof(int) * resourceCacheStage->numResources);
  if (!pids) {
    logprintfl(EUCAFATAL, "refresh_resources(): out of memory!\n");
    unlock_exit(1);
  }
  
  for (i=0; i<resourceCacheStage->numResources; i++) {

    sem_mywait(REFRESHLOCK);

    pid = fork();
    if (!pid) {
      ncResDst=NULL;
      if (resourceCacheStage->resources[i].state != RESASLEEP && resourceCacheStage->resources[i].running == 0) {
//	nctimeout = ncGetTimeout(op_start, timeout, resourceCacheStage->numResources, i);
	nctimeout = ncGetTimeout(op_start, timeout, 1, 1);
	rc = ncClientCall(ccMeta, nctimeout, resourceCacheStage->resources[i].lockidx, resourceCacheStage->resources[i].ncURL, "ncDescribeResource", NULL, &ncResDst);
	if (rc != 0) {
	  powerUp(&(resourceCacheStage->resources[i]));
	  
	  if (resourceCacheStage->resources[i].state == RESWAKING && ((time(NULL) - resourceCacheStage->resources[i].stateChange) < config->wakeThresh)) {
	    logprintfl(EUCADEBUG, "refresh_resources(): resource still waking up (%d more seconds until marked as down)\n", config->wakeThresh - (time(NULL) - resourceCacheStage->resources[i].stateChange));
	  } else{
	    logprintfl(EUCAERROR,"refresh_resources(): bad return from ncDescribeResource(%s) (%d)\n", resourceCacheStage->resources[i].hostname, rc);
	    resourceCacheStage->resources[i].maxMemory = 0;
	    resourceCacheStage->resources[i].availMemory = 0;
	    resourceCacheStage->resources[i].maxDisk = 0;
	    resourceCacheStage->resources[i].availDisk = 0;
	    resourceCacheStage->resources[i].maxCores = 0;
	    resourceCacheStage->resources[i].availCores = 0;    
	    changeState(&(resourceCacheStage->resources[i]), RESDOWN);
	  }
	} else {
	  logprintfl(EUCADEBUG,"refresh_resources(): received data from node=%s mem=%d/%d disk=%d/%d cores=%d/%d\n", resourceCacheStage->resources[i].hostname, ncResDst->memorySizeMax, ncResDst->memorySizeAvailable, ncResDst->diskSizeMax,  ncResDst->diskSizeAvailable, ncResDst->numberOfCoresMax, ncResDst->numberOfCoresAvailable);
	  resourceCacheStage->resources[i].maxMemory = ncResDst->memorySizeMax;
	  resourceCacheStage->resources[i].availMemory = ncResDst->memorySizeAvailable;
	  resourceCacheStage->resources[i].maxDisk = ncResDst->diskSizeMax;
	  resourceCacheStage->resources[i].availDisk = ncResDst->diskSizeAvailable;
	  resourceCacheStage->resources[i].maxCores = ncResDst->numberOfCoresMax;
	  resourceCacheStage->resources[i].availCores = ncResDst->numberOfCoresAvailable;    

	  // set iqn, if set
	  if (strlen(ncResDst->iqn)) {
	    snprintf(resourceCacheStage->resources[i].iqn, 128, "%s", ncResDst->iqn);
	  }
      
	  changeState(&(resourceCacheStage->resources[i]), RESUP);
	}
      } else {
	logprintfl(EUCADEBUG, "refresh_resources(): resource asleep/running instances (%d), skipping resource update\n", resourceCacheStage->resources[i].running);
      }
      
      // try to discover the mac address of the resource
      if (resourceCacheStage->resources[i].mac[0] == '\0' && resourceCacheStage->resources[i].ip[0] != '\0') {
	char *mac;
	rc = ip2mac(vnetconfig, resourceCacheStage->resources[i].ip, &mac);
	if (!rc) {
	  strncpy(resourceCacheStage->resources[i].mac, mac, 24);
	  free(mac);
	  logprintfl(EUCADEBUG, "refresh_resources(): discovered MAC '%s' for host %s(%s)\n", resourceCacheStage->resources[i].mac, resourceCacheStage->resources[i].hostname, resourceCacheStage->resources[i].ip);
	}
      }
      
      if (ncResDst) free(ncResDst);
      sem_mypost(REFRESHLOCK);
      exit(0);
    } else {
      pids[i] = pid;
    }
  }

  for (i=0; i<resourceCacheStage->numResources; i++) {
    rc = timewait(pids[i], &status, 120);
    if (!rc) {
      // timed out, really bad failure (reset REFRESHLOCK semaphore)
      sem_close(locks[REFRESHLOCK]);
      locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
      rc = 1;
    } else if (rc > 0) {
      // process exited, and wait picked it up.
      if (WIFEXITED(status)) {
	rc = WEXITSTATUS(status);
      } else {
	rc = 1;
      }
    } else {
      // process no longer exists, and someone else reaped it
      rc = 0;
    }
    if (rc) {
      logprintfl(EUCAWARN, "refresh_resources(): error waiting for child pid '%d', exit code '%d'\n", pids[i], rc);
    }
  }
  
  sem_mywait(RESCACHE);
  //  memcpy(resourceCache, &resourceCacheLocal, sizeof(ccResourceCache));
  memcpy(resourceCache, resourceCacheStage, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);  

  if (pids) free(pids);

  logprintfl(EUCADEBUG,"refresh_resources(): done\n");
  return(0);
}

int refresh_instances(ncMetadata *ccMeta, int timeout, int dolock) {
  ccInstance *myInstance=NULL;
  int i, k, numInsts = 0, found, ncOutInstsLen, rc, pid, nctimeout, *pids=NULL, status;
  time_t op_start;

  ncInstance **ncOutInsts=NULL;
  ncStub *ncs;

  //  ccResourceCache resourceCacheLocal;

  op_start = time(NULL);

  logprintfl(EUCAINFO,"refresh_instances(): called\n");

  // critical NC call section
  sem_mywait(RESCACHE);
  memcpy(resourceCacheStage, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);

  sem_close(locks[REFRESHLOCK]);
  locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);

  pids = malloc(sizeof(int) * resourceCacheStage->numResources);
  if (!pids) {
    logprintfl(EUCAFATAL, "refresh_instances(): out of memory!\n");
    unlock_exit(1);
  }

  invalidate_instanceCache();

  for (i=0; i<resourceCacheStage->numResources; i++) {
    sem_mywait(REFRESHLOCK);
    pid = fork();
    if (!pid) {
      if (resourceCacheStage->resources[i].state == RESUP) {
	int j;
	
//	nctimeout = ncGetTimeout(op_start, timeout, resourceCacheStage->numResources, i);
	nctimeout = ncGetTimeout(op_start, timeout, 1, 1);
	rc = ncClientCall(ccMeta, nctimeout, resourceCacheStage->resources[i].lockidx, resourceCacheStage->resources[i].ncURL, "ncDescribeInstances", NULL, 0, &ncOutInsts, &ncOutInstsLen);
	if (!rc) {
	  
	  // if idle, power down
	  if (ncOutInstsLen == 0) {
	    logprintfl(EUCADEBUG, "refresh_instances(): node %s idle since %d: (%d/%d) seconds\n", resourceCacheStage->resources[i].hostname, resourceCacheStage->resources[i].idleStart, time(NULL) - resourceCacheStage->resources[i].idleStart, config->idleThresh); 
	    if (!resourceCacheStage->resources[i].idleStart) {
	      resourceCacheStage->resources[i].idleStart = time(NULL);
	    } else if ((time(NULL) - resourceCacheStage->resources[i].idleStart) > config->idleThresh) {
	      // call powerdown
	      
	      if (powerDown(ccMeta, &(resourceCacheStage->resources[i]))) {
		logprintfl(EUCAWARN, "refresh_instances(): powerDown for %s failed\n", resourceCacheStage->resources[i].hostname);
	      }
	    }
	  } else {
	    resourceCacheStage->resources[i].idleStart = 0;
	  }
	  
	  // populate instanceCache
	  for (j=0; j<ncOutInstsLen; j++) {
	    found=1;
	    if (found) {
	      myInstance = NULL;
	      // add it
	      logprintfl(EUCADEBUG,"refresh_instances(): describing instance %s, %s, %d\n", ncOutInsts[j]->instanceId, ncOutInsts[j]->stateName, j);
	      numInsts++;
	      
	      // grab instance from cache, if available.  otherwise, start from scratch
	      rc = find_instanceCacheId(ncOutInsts[j]->instanceId, &myInstance);
	      if (rc || !myInstance) {
		myInstance = malloc(sizeof(ccInstance));
		if (!myInstance) {
		  logprintfl(EUCAFATAL, "refresh_instances(): out of memory!\n");
		  unlock_exit(1);
		}
		bzero(myInstance, sizeof(ccInstance));
	      }
	      
	      // update CC instance with instance state from NC 
	      rc = ccInstance_to_ncInstance(myInstance, ncOutInsts[j]);
	      
	      // instance info that the CC maintains
	      myInstance->ncHostIdx = i;
	      strncpy(myInstance->serviceTag, resourceCacheStage->resources[i].ncURL, 64);
	      {
		char *ip=NULL;
		if (!strcmp(myInstance->ccnet.publicIp, "0.0.0.0")) {
		  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
		    rc = mac2ip(vnetconfig, myInstance->ccnet.privateMac, &ip);
		    if (!rc) {
		      strncpy(myInstance->ccnet.publicIp, ip, 24);
		    }
		  }
		}
		
		if (ip) free(ip);
		ip=NULL;
		
		if (!strcmp(myInstance->ccnet.privateIp, "0.0.0.0")) {
		  rc = mac2ip(vnetconfig, myInstance->ccnet.privateMac, &ip);
		  if (!rc) {
		    strncpy(myInstance->ccnet.privateIp, ip, 24);
		  }
		}
		
		if (ip) free(ip);
	      }

	      // check for network instance IP inconsistency
	      if ( strcmp(myInstance->ccnet.publicIp, ncOutInsts[j]->ncnet.publicIp) ) {
		logprintfl(EUCADEBUG, "refresh_instances(): instId=%s, publicIP reported by NC (%s) differs from publicIp assigned at CC (%s), updating.\n", myInstance->instanceId, ncOutInsts[j]->ncnet.publicIp, myInstance->ccnet.publicIp);
		rc = ncClientCall(ccMeta, nctimeout, resourceCacheStage->resources[i].lockidx, resourceCacheStage->resources[i].ncURL, "ncAssignAddress", myInstance->instanceId, myInstance->ccnet.publicIp);
		if (rc) {
		  logprintfl(EUCAERROR, "refresh_instance(): could not update publicIP (%s) of instance (%s) at NC (%s)\n", myInstance->ccnet.publicIp, myInstance->instanceId, resourceCacheStage->resources[i].ncURL);
		}
	      }
	      
	      refresh_instanceCache(myInstance->instanceId, myInstance);
	      if (!strcmp(myInstance->state, "Extant")) {
	       if (myInstance->ccnet.vlan < 0) {
	          vnetEnableHost(vnetconfig, myInstance->ccnet.privateMac, myInstance->ccnet.privateIp, 0);
	       } else {
	          vnetEnableHost(vnetconfig, myInstance->ccnet.privateMac, myInstance->ccnet.privateIp, myInstance->ccnet.vlan);
	       }
              }
	      logprintfl(EUCADEBUG, "refresh_instances(): storing instance state: %s/%s/%s/%s\n", myInstance->instanceId, myInstance->state, myInstance->ccnet.publicIp, myInstance->ccnet.privateIp);
	      print_ccInstance("refresh_instances(): ", myInstance);
	      
	      if (myInstance) free(myInstance);
	    }

	  }
	}
	if (ncOutInsts) {
	  for (j=0; j<ncOutInstsLen; j++) {
	    free_instance(&(ncOutInsts[j]));
	  }
	  free(ncOutInsts);
	  ncOutInsts = NULL;
	}
      }
      sem_mypost(REFRESHLOCK);
      exit(0);
    } else {
      pids[i] = pid;
    }
  }
  
  for (i=0; i<resourceCacheStage->numResources; i++) {
    rc = timewait(pids[i], &status, 120);
    if (!rc) {
      // timed out, really bad failure (reset REFRESHLOCK semaphore)
      sem_close(locks[REFRESHLOCK]);
      locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
      rc = 1;
    } else if (rc > 0) {
      // process exited, and wait picked it up.
      if (WIFEXITED(status)) {
        rc = WEXITSTATUS(status);
      } else {
	rc = 1;
      }
    } else {
      // process no longer exists, and someone else reaped it
      rc = 0;
    }
    if (rc) {
      logprintfl(EUCAWARN, "refresh_instances(): error waiting for child pid '%d', exit code '%d'\n", pids[i], rc);
    }
  }
	    
  sem_mywait(RESCACHE);
  memcpy(resourceCache, resourceCacheStage, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
  
  if (pids) free(pids);

  logprintfl(EUCADEBUG,"refresh_instances(): done\n");  
  return(0);
}

int doDescribeInstances(ncMetadata *ccMeta, char **instIds, int instIdsLen, ccInstance **outInsts, int *outInstsLen) {
  ccInstance *myInstance=NULL, *out=NULL, *cacheInstance=NULL;
  int i, k, numInsts, found, ncOutInstsLen, rc, pid, count;
  virtualMachine ccvm;
  time_t op_start;

  ncInstance **ncOutInsts=NULL;
  ncStub *ncs;

  logprintfl(EUCAINFO,"DescribeInstances(): called\n");
  logprintfl(EUCADEBUG,"DescribeInstances(): params: userId=%s, instIdsLen=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), instIdsLen);
  
  op_start = time(NULL);

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }

  *outInsts = NULL;
  *outInstsLen = 0;

  sem_mywait(INSTCACHE);
  count=0;
  if (instanceCache->numInsts) {
    *outInsts = malloc(sizeof(ccInstance) * instanceCache->numInsts);
    if (!*outInsts) {
      logprintfl(EUCAFATAL, "doDescribeInstances(): out of memory!\n");
      unlock_exit(1);
    }

    for (i=0; i<MAXINSTANCES; i++) {
      if (instanceCache->cacheState[i] == INSTVALID) {
	if (count >= instanceCache->numInsts) {
	  logprintfl(EUCAWARN, "doDescribeInstances(): found more instances than reported by numInsts, will only report a subset of instances\n");
	  count=0;
	}
	memcpy( &((*outInsts)[count]), &(instanceCache->instances[i]), sizeof(ccInstance));
	count++;
      }
    }
    
    *outInstsLen = instanceCache->numInsts;
  }
  sem_mypost(INSTCACHE);

  for (i=0; i< (*outInstsLen) ; i++) {
    logprintfl(EUCAINFO, "DescribeInstances(): instance response summary: instanceId=%s, state=%s, publicIp=%s, privateIp=%s\n", (*outInsts)[i].instanceId, (*outInsts)[i].state, (*outInsts)[i].ccnet.publicIp, (*outInsts)[i].ccnet.privateIp);
  }

  logprintfl(EUCADEBUG,"DescribeInstances(): done\n");

  shawn();
      
  return(0);
}

int powerUp(ccResource *res) {
  int rc,ret,len, i;
  char cmd[MAX_PATH], *bc=NULL;
  uint32_t *ips=NULL, *nms=NULL;
  
  if (config->schedPolicy != SCHEDPOWERSAVE) {
    return(0);
  }

  rc = getdevinfo(vnetconfig->privInterface, &ips, &nms, &len);
  if (rc) {

    ips = malloc(sizeof(uint32_t));
    if (!ips) {
      logprintfl(EUCAFATAL, "powerUp(): out of memory!\n");
      unlock_exit(1);
    }
    
    nms = malloc(sizeof(uint32_t));
    if (!nms) {
      logprintfl(EUCAFATAL, "powerUp(): out of memory!\n");
      unlock_exit(1);
    }

    ips[0] = 0xFFFFFFFF;
    nms[0] = 0xFFFFFFFF;
    len = 1;
  }
  
  for (i=0; i<len; i++) {
    logprintfl(EUCADEBUG, "powerUp(): attempting to wake up resource %s(%s/%s)\n", res->hostname, res->ip, res->mac);
    // try to wake up res

    // broadcast
    bc = hex2dot((0xFFFFFFFF - nms[i]) | (ips[i] & nms[i]));

    rc = 0;
    ret = 0;
    if (strcmp(res->mac, "00:00:00:00:00:00")) {
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap powerwake -b %s %s", vnetconfig->eucahome, bc, res->mac);
    } else if (strcmp(res->ip, "0.0.0.0")) {
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap powerwake -b %s %s", vnetconfig->eucahome, bc, res->ip);
    } else {
      ret = rc = 1;
    }
    if (bc) free(bc);
    if (!rc) {
      logprintfl(EUCAINFO, "powerUp(): waking up powered off host %s(%s/%s): %s\n", res->hostname, res->ip, res->mac, cmd);
      rc = system(cmd);
      rc = rc>>8;
      if (rc) {
	logprintfl(EUCAERROR, "powerUp(): cmd failed: %d\n", rc);
	ret = 1;
      } else {
	logprintfl(EUCAERROR, "powerUp(): cmd success: %d\n", rc);
	changeState(res, RESWAKING);
	ret = 0;
      }
    }
  }
  if (ips) free(ips);
  if (nms) free(nms);
  return(ret);
}

int powerDown(ncMetadata *ccMeta, ccResource *node) {
  int pid, rc, status, timeout;
  ncStub *ncs=NULL;
  time_t op_start;
  
  if (config->schedPolicy != SCHEDPOWERSAVE) {
    node->idleStart = 0;
    return(0);
  }

  op_start = time(NULL);
  
  logprintfl(EUCAINFO, "powerDown(): sending powerdown to node: %s, %s\n", node->hostname, node->ncURL);
  
  timeout = ncGetTimeout(op_start, OP_TIMEOUT, 1, 1);
  rc = ncClientCall(ccMeta, timeout, node->lockidx, node->ncURL, "ncPowerDown");
  
  if (rc == 0) {
    changeState(node, RESASLEEP);
  }
  return(rc);
}

int ccInstance_to_ncInstance(ccInstance *dst, ncInstance *src) {
  int i;
  
  strncpy(dst->uuid, src->uuid, 48);
  strncpy(dst->instanceId, src->instanceId, 16);
  strncpy(dst->reservationId, src->reservationId, 16);
  strncpy(dst->ownerId, src->userId, 16);
  strncpy(dst->amiId, src->imageId, 16);
  strncpy(dst->kernelId, src->kernelId, 16);
  strncpy(dst->ramdiskId, src->ramdiskId, 16);
  strncpy(dst->keyName, src->keyName, 1024);
  strncpy(dst->launchIndex, src->launchIndex, 64);
  strncpy(dst->platform, src->platform, 64);
  strncpy(dst->bundleTaskStateName, src->bundleTaskStateName, 64);
  strncpy(dst->createImageTaskStateName, src->createImageTaskStateName, 64);
  strncpy(dst->userData, src->userData, 4096);
  strncpy(dst->state, src->stateName, 16);
  dst->ts = src->launchTime;

  dst->ccnet.vlan = src->ncnet.vlan;
  dst->ccnet.networkIndex = src->ncnet.networkIndex;
  strncpy(dst->ccnet.privateMac, src->ncnet.privateMac, 24);
  if (strcmp(src->ncnet.publicIp, "0.0.0.0") || dst->ccnet.publicIp[0] == '\0') strncpy(dst->ccnet.publicIp, src->ncnet.publicIp, 24);
  if (strcmp(src->ncnet.privateIp, "0.0.0.0") || dst->ccnet.privateIp[0] == '\0') strncpy(dst->ccnet.privateIp, src->ncnet.privateIp, 24);

  for (i=0; i < src->groupNamesSize && i < 64; i++) {
    snprintf(dst->groupNames[i], 32, "%s", src->groupNames[i]);
  }

  memcpy(dst->volumes, src->volumes, sizeof(ncVolume) * EUCA_MAX_VOLUMES);
  dst->volumesSize = src->volumesSize;

  memcpy(&(dst->ccvm), &(src->params), sizeof(virtualMachine));

  dst->blkbytes = src->blkbytes;
  dst->netbytes = src->netbytes;

  return(0);
}

int schedule_instance(virtualMachine *vm, char *targetNode, int *outresid) {
  int ret;
  ncMetadata ccMeta;

  if (targetNode != NULL) {
    ret = schedule_instance_explicit(vm, targetNode, outresid);
  } else if (config->schedPolicy == SCHEDGREEDY) {
    ret = schedule_instance_greedy(vm, outresid);
  } else if (config->schedPolicy == SCHEDROUNDROBIN) {
    ret = schedule_instance_roundrobin(vm, outresid);
  } else if (config->schedPolicy == SCHEDPOWERSAVE) {
    ret = schedule_instance_greedy(vm, outresid);
  } else {
    ret = schedule_instance_greedy(vm, outresid);
  }

  return(ret);
}

int schedule_instance_roundrobin(virtualMachine *vm, int *outresid) {
  int i, done, start, found, resid=0;
  ccResource *res;

  *outresid = 0;

  logprintfl(EUCADEBUG, "schedule(): scheduler using ROUNDROBIN policy to find next resource\n");
  // find the best 'resource' on which to run the instance
  done=found=0;
  start = config->schedState;
  i = start;
  
  logprintfl(EUCADEBUG, "schedule(): scheduler state starting at resource %d\n", config->schedState);
  while(!done) {
    int mem, disk, cores;
    
    res = &(resourceCache->resources[i]);
    if (res->state != RESDOWN) {
      mem = res->availMemory - vm->mem;
      disk = res->availDisk - vm->disk;
      cores = res->availCores - vm->cores;
      
      if (mem >= 0 && disk >= 0 && cores >= 0) {
	resid = i;
	found=1;
	done++;
      }
    }
    i++;
    if (i >= resourceCache->numResources) {
      i = 0;
    }
    if (i == start) {
      done++;
    }
  }

  if (!found) {
    // didn't find a resource
    return(1);
  }

  *outresid = resid;
  config->schedState = i;

  logprintfl(EUCADEBUG, "schedule(): scheduler state finishing at resource %d\n", config->schedState);

  return(0);
}

int schedule_instance_explicit(virtualMachine *vm, char *targetNode, int *outresid) {
  int i, rc, done, resid, sleepresid;
  ccResource *res;
  
  *outresid = 0;

  logprintfl(EUCADEBUG, "schedule(): scheduler using EXPLICIT policy to run VM on target node '%s'\n", targetNode);

  // find the best 'resource' on which to run the instance
  resid = sleepresid = -1;
  done=0;
  for (i=0; i<resourceCache->numResources && !done; i++) {
    int mem, disk, cores;
    
    res = &(resourceCache->resources[i]);
    if (!strcmp(res->hostname, targetNode)) {
      done++;
      if (res->state == RESUP) {
	mem = res->availMemory - vm->mem;
	disk = res->availDisk - vm->disk;
	cores = res->availCores - vm->cores;
	
	if (mem >= 0 && disk >= 0 && cores >= 0) {
	  resid = i;
	}
      } else if (res->state == RESASLEEP) {
	mem = res->availMemory - vm->mem;
	disk = res->availDisk - vm->disk;
	cores = res->availCores - vm->cores;
	
	if (mem >= 0 && disk >= 0 && cores >= 0) {
	  sleepresid = i;
	}
      }
    }
  }
  
  if (resid == -1 && sleepresid == -1) {
    // target resource is unavailable
    return(1);
  }
  
  if (resid != -1) {
    res = &(resourceCache->resources[resid]);
    *outresid = resid;
  } else if (sleepresid != -1) {
    res = &(resourceCache->resources[sleepresid]);
    *outresid = sleepresid;
  }
  if (res->state == RESASLEEP) {
    rc = powerUp(res);
  }

  return(0);
}

int schedule_instance_greedy(virtualMachine *vm, int *outresid) {
  int i, rc, done, resid, sleepresid;
  ccResource *res;
  
  *outresid = 0;

  if (config->schedPolicy == SCHEDGREEDY) {
    logprintfl(EUCADEBUG, "schedule(): scheduler using GREEDY policy to find next resource\n");
  } else if (config->schedPolicy == SCHEDPOWERSAVE) {
    logprintfl(EUCADEBUG, "schedule(): scheduler using POWERSAVE policy to find next resource\n");
  }

  // find the best 'resource' on which to run the instance
  resid = sleepresid = -1;
  done=0;
  for (i=0; i<resourceCache->numResources && !done; i++) {
    int mem, disk, cores;
    
    res = &(resourceCache->resources[i]);
    if ((res->state == RESUP || res->state == RESWAKING) && resid == -1) {
      mem = res->availMemory - vm->mem;
      disk = res->availDisk - vm->disk;
      cores = res->availCores - vm->cores;
      
      if (mem >= 0 && disk >= 0 && cores >= 0) {
	resid = i;
	done++;
      }
    } else if (res->state == RESASLEEP && sleepresid == -1) {
      mem = res->availMemory - vm->mem;
      disk = res->availDisk - vm->disk;
      cores = res->availCores - vm->cores;
      
      if (mem >= 0 && disk >= 0 && cores >= 0) {
	sleepresid = i;
      }
    }
  }
  
  if (resid == -1 && sleepresid == -1) {
    // didn't find a resource
    return(1);
  }
  
  if (resid != -1) {
    res = &(resourceCache->resources[resid]);
    *outresid = resid;
  } else if (sleepresid != -1) {
    res = &(resourceCache->resources[sleepresid]);
    *outresid = sleepresid;
  }
  if (res->state == RESASLEEP) {
    rc = powerUp(res);
  }

  return(0);
}


int doRunInstances(ncMetadata *ccMeta, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char **instIds, int instIdsLen, char **netNames, int netNamesLen, char **macAddrs, int macAddrsLen, int *networkIndexList, int networkIndexListLen, char **uuids, int uuidsLen, int minCount, int maxCount, char *ownerId, char *reservationId, virtualMachine *ccvm, char *keyName, int vlan, char *userData, char *launchIndex, char *platform, int expiryTime, char *targetNode, ccInstance **outInsts, int *outInstsLen) {
  int rc=0, i=0, done=0, runCount=0, resid=0, foundnet=0, error=0, networkIdx=0, nidx=0, thenidx=0;
  ccInstance *myInstance=NULL, 
    *retInsts=NULL;
  char instId[16], uuid[48];
  time_t op_start=0;
  ccResource *res=NULL;
  char mac[32], privip[32], pubip[32];
  
  ncInstance *outInst=NULL;
  virtualMachine ncvm;
  netConfig ncnet;
  ncStub *ncs=NULL;
  
  char emiURLCached[1024], kernelURLCached[1024], ramdiskURLCached[1024];

  op_start = time(NULL);
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO,"RunInstances(): called\n");
  logprintfl(EUCADEBUG,"RunInstances(): params: userId=%s, emiId=%s, kernelId=%s, ramdiskId=%s, emiURL=%s, kernelURL=%s, ramdiskURL=%s, instIdsLen=%d, netNamesLen=%d, macAddrsLen=%d, networkIndexListLen=%d, minCount=%d, maxCount=%d, ownerId=%s, reservationId=%s, keyName=%s, vlan=%d, userData=%s, launchIndex=%s, platform=%s, targetNode=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(amiId), SP(kernelId), SP(ramdiskId), SP(amiURL), SP(kernelURL), SP(ramdiskURL), instIdsLen, netNamesLen, macAddrsLen, networkIndexListLen, minCount, maxCount, SP(ownerId), SP(reservationId), SP(keyName), vlan, SP(userData), SP(launchIndex), SP(platform), SP(targetNode));
  
  if (config->use_proxy) {
    rc = image_cache(amiId, amiURL);
    if (rc) {
      logprintfl(EUCAWARN, "RunInstances(): could not cache image (%s/%s)\n", SP(amiId), SP(amiURL));
    } else {
      // re-write URLs to point at CC image proxy
      snprintf(emiURLCached, 1024, "http://%s:8776/%s", config->proxyIp, amiId);
      amiURL = emiURLCached;
    }
    rc = image_cache(kernelId, kernelURL);
    if (rc) {
      logprintfl(EUCAWARN, "RunInstances(): could not cache image (%s/%s)\n", SP(kernelId), SP(kernelURL));
    } else {
      // re-write URLs to point at CC image proxy
      snprintf(kernelURLCached, 1024, "http://%s:8776/%s", config->proxyIp, kernelId);
      kernelURL = kernelURLCached;
    }
    rc = image_cache(ramdiskId, ramdiskURL);
    if (rc) {
      logprintfl(EUCAWARN, "RunInstances(): could not cache image (%s/%s)\n", SP(ramdiskId), SP(ramdiskURL));
    } else {
      // re-write URLs to point at CC image proxy
      snprintf(ramdiskURLCached, 1024, "http://%s:8776/%s", config->proxyIp, ramdiskId);
      ramdiskURL = ramdiskURLCached;
    }
  }

  *outInstsLen = 0;
  
  if (!ccvm) {
    logprintfl(EUCAERROR,"RunInstances(): invalid ccvm\n");
    return(-1);
  }
  if (minCount <= 0 || maxCount <= 0 || instIdsLen < maxCount) {
    logprintfl(EUCAERROR,"RunInstances(): bad min or max count, or not enough instIds (%d, %d, %d)\n", minCount, maxCount, instIdsLen);
    return(-1);
  }

  // check health of the networkIndexList
  if ( (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC")) || networkIndexList == NULL) {
    // disabled
    nidx=-1;
  } else {
    if ( (networkIndexListLen < minCount) || (networkIndexListLen > maxCount) ) {
      logprintfl(EUCAERROR, "RunInstances(): network index length (%d) is out of bounds for min/max instances (%d-%d)\n", networkIndexListLen, minCount, maxCount);
      return(1);
    }
    for (i=0; i<networkIndexListLen; i++) {
      if ( (networkIndexList[i] < 0) || (networkIndexList[i] > (vnetconfig->numaddrs-1)) ) {
	logprintfl(EUCAERROR, "RunInstances(): network index (%d) out of bounds (0-%d)\n", networkIndexList[i], vnetconfig->numaddrs-1);
	return(1);
      }
    }

    // all checked out
    nidx=0;
  }
  
  retInsts = malloc(sizeof(ccInstance) * maxCount);  
  if (!retInsts) {
    logprintfl(EUCAFATAL, "RunInstances(): out of memory!\n");
    unlock_exit(1);
  }

  runCount=0;
  
  // get updated resource information

  done=0;
  for (i=0; i<maxCount && !done; i++) {
    snprintf(instId, 16, "%s", instIds[i]);
    if (uuidsLen > i) {
      snprintf(uuid, 48, "%s", uuids[i]);
    } else {
      snprintf(uuid, 48, "UNSET");
    }

    logprintfl(EUCADEBUG,"RunInstances(): running instance %s with emiId %s...\n", instId, amiId);
    
    // generate new mac
    bzero(mac, 32);
    bzero(pubip, 32);
    bzero(privip, 32);
    
    strncpy(pubip, "0.0.0.0", 32);
    strncpy(privip, "0.0.0.0", 32);
    if (macAddrsLen >= maxCount) {
      strncpy(mac, macAddrs[i], 32);
    }      

    sem_mywait(VNET);
    if (nidx == -1) {
      rc = vnetGenerateNetworkParams(vnetconfig, instId, vlan, -1, mac, pubip, privip);
      thenidx = -1;
    } else {
      rc = vnetGenerateNetworkParams(vnetconfig, instId, vlan, networkIndexList[nidx], mac, pubip, privip);
      thenidx=nidx;
      nidx++;
    }
    if (rc) {
      foundnet = 0;
    } else {
      foundnet = 1;
    }
    sem_mypost(VNET);
    
    if (thenidx != -1) {
      logprintfl(EUCADEBUG,"RunInstances(): assigning MAC/IP: %s/%s/%s/%d\n", mac, pubip, privip, networkIndexList[thenidx]);
    } else {
      logprintfl(EUCADEBUG,"RunInstances(): assigning MAC/IP: %s/%s/%s/%d\n", mac, pubip, privip, thenidx);
    }
    
    if (mac[0] == '\0' || !foundnet) {
      logprintfl(EUCAERROR,"RunInstances(): could not find/initialize any free network address, failing doRunInstances()\n");
    } else {
      // "run" the instance
      memcpy (&ncvm, ccvm, sizeof(virtualMachine));
      
      ncnet.vlan = vlan;
      if (thenidx >= 0) {
	ncnet.networkIndex = networkIndexList[thenidx];
      } else {
	ncnet.networkIndex = -1;
      }
      snprintf(ncnet.privateMac, 24, "%s", mac);
      snprintf(ncnet.privateIp, 24, "%s", privip);
      snprintf(ncnet.publicIp, 24, "%s", pubip);
      
      sem_mywait(RESCACHE);

      resid = 0;
      
      sem_mywait(CONFIG);
      rc = schedule_instance(ccvm, targetNode, &resid);
      sem_mypost(CONFIG);

      res = &(resourceCache->resources[resid]);
      if (rc) {
	// could not find resource
	logprintfl(EUCAERROR, "RunInstances(): scheduler could not find resource to run the instance on\n");
	// couldn't run this VM, remove networking information from system
	free_instanceNetwork(mac, vlan, 1, 1);
      } else {
	int pid, status, ret, rbytes;
	
	// try to run the instance on the chosen resource
	logprintfl(EUCADEBUG, "RunInstances(): scheduler decided to run instance '%s' on resource '%s', running count '%d'\n", instId, res->ncURL, res->running);
	
	outInst=NULL;
	
	pid = fork();
	if (pid == 0) {
	  time_t startRun;

	  sem_mywait(RESCACHE);
	  if (res->running > 0) {
	    res->running++;
	  }
	  sem_mypost(RESCACHE);

	  ret=0;
	  logprintfl(EUCAINFO,"RunInstances(): sending run instance: node=%s instanceId=%s emiId=%s mac=%s privIp=%s pubIp=%s vlan=%d networkIdx=%d key=%.32s... mem=%d disk=%d cores=%d\n", res->ncURL, instId, SP(amiId), ncnet.privateMac, ncnet.privateIp, ncnet.publicIp, ncnet.vlan, ncnet.networkIndex, SP(keyName), ncvm.mem, ncvm.disk, ncvm.cores);
	  rc = 1;
	  startRun = time(NULL);
	  while(rc && ((time(NULL) - startRun) < config->wakeThresh)){
            int clientpid;

	    // if we're running windows, and are an NC, create the pw/floppy locally
	    if (strstr(platform, "windows") && !strstr(res->ncURL, "EucalyptusNC")) {
	      //if (strstr(platform, "windows")) {
	      char cdir[MAX_PATH];
	      
	      snprintf(cdir, MAX_PATH, "%s/var/lib/eucalyptus/windows/", config->eucahome);
	      if (check_directory(cdir)) mkdir(cdir, 0700);
	      snprintf(cdir, MAX_PATH, "%s/var/lib/eucalyptus/windows/%s/", config->eucahome, instId);
	      if (check_directory(cdir)) mkdir(cdir, 0700);
	      if (check_directory(cdir)) {
		logprintfl(EUCAERROR, "RunInstances(): could not create console/floppy cache directory '%s'\n", cdir);
	      } else {
		// drop encrypted windows password and floppy on filesystem
		rc = makeWindowsFloppy(config->eucahome, cdir, keyName, instId);
		if (rc) {
		  logprintfl(EUCAERROR, "RunInstances(): could not create console/floppy cache\n");
		}
	      }
	    }
	    
            // call StartNetwork client
	    rc = ncClientCall(ccMeta, OP_TIMEOUT_PERNODE, res->lockidx, res->ncURL, "ncStartNetwork", uuid, NULL, 0, 0, vlan, NULL);

	    rc = ncClientCall(ccMeta, OP_TIMEOUT_PERNODE, res->lockidx, res->ncURL, "ncRunInstance", uuid, instId, reservationId, &ncvm, amiId, amiURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, &ncnet, userData, launchIndex, platform, expiryTime, netNames, netNamesLen, &outInst);

	    if (rc) {
	      sleep(1);
	    }
	  }
	  if (!rc) {
	    ret = 0;
	  } else {
	    ret = 1;
	  }
	  
	  sem_mywait(RESCACHE);
	  if (res->running > 0) {
	    res->running--;
	  }
	  sem_mypost(RESCACHE);

	  exit(ret);
	} else {
	  rc = 0;
	  logprintfl(EUCADEBUG,"RunInstances(): call complete (pid/rc): %d/%d\n", pid, rc);
	}
	if (rc != 0) {
	  // problem
	  logprintfl(EUCAERROR, "RunInstances(): tried to run the VM, but runInstance() failed; marking resource '%s' as down\n", res->ncURL);
	  res->state = RESDOWN;
	  i--;
	  // couldn't run this VM, remove networking information from system
	  free_instanceNetwork(mac, vlan, 1, 1);
	} else {
	  res->availMemory -= ccvm->mem;
	  res->availDisk -= ccvm->disk;
	  res->availCores -= ccvm->cores;

	  logprintfl(EUCADEBUG, "RunInstances(): resource information after schedule/run: %d/%d, %d/%d, %d/%d\n", res->availMemory, res->maxMemory, res->availCores, res->maxCores, res->availDisk, res->maxDisk);

	  myInstance = &(retInsts[runCount]);
	  bzero(myInstance, sizeof(ccInstance));
	  
	  allocate_ccInstance(myInstance, instId, amiId, kernelId, ramdiskId, amiURL, kernelURL, ramdiskURL, ownerId, "Pending", time(NULL), reservationId, &ncnet, ccvm, resid, keyName, resourceCache->resources[resid].ncURL, userData, launchIndex, platform, myInstance->bundleTaskStateName, myInstance->groupNames, myInstance->volumes, myInstance->volumesSize);

	  // start up DHCP
	  sem_mywait(CONFIG);
	  config->kick_dhcp = 1;
	  sem_mypost(CONFIG);

	  // add the instance to the cache, and continue on
	  add_instanceCache(myInstance->instanceId, myInstance);
	  print_ccInstance("RunInstances(): ", myInstance);
	  
	  runCount++;
	}
      }

      sem_mypost(RESCACHE);

    }
    
  }
  *outInstsLen = runCount;
  *outInsts = retInsts;
  
  logprintfl(EUCADEBUG,"RunInstances(): done\n");
  
  shawn();

  if (error) {
    return(1);
  }
  return(0);
}

int doGetConsoleOutput(ncMetadata *ccMeta, char *instId, char **outConsoleOutput) {
  int i, j, rc, numInsts, start, stop, done, ret, rbytes, timeout=0;
  ccInstance *myInstance;
  ncStub *ncs;
  char *consoleOutput;
  time_t op_start;
  ccResourceCache resourceCacheLocal;

  i = j = numInsts = 0;
  op_start = time(NULL);

  consoleOutput = NULL;
  myInstance = NULL;
  
  *outConsoleOutput = NULL;

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }

  logprintfl(EUCAINFO,"GetConsoleOutput(): called\n");
  logprintfl(EUCADEBUG,"GetConsoleOutput(): params: userId=%s, instId=%s\n", SP(ccMeta->userId), SP(instId));

  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);

  rc = find_instanceCacheId(instId, &myInstance);
  if (!rc) {
    // found the instance in the cache
    start = myInstance->ncHostIdx;
    stop = start+1;      
    free(myInstance);
  } else {
    start = 0;
    stop = resourceCacheLocal.numResources;
  }

  done=0;
  for (j=start; j<stop && !done; j++) {
    if (*outConsoleOutput) free(*outConsoleOutput);

    if (!strstr(resourceCacheLocal.resources[j].ncURL, "EucalyptusNC")) {
      //if (1) {
      char pwfile[MAX_PATH];
      char *rawconsole=NULL;
      *outConsoleOutput = NULL;
      snprintf(pwfile, MAX_PATH, "%s/var/lib/eucalyptus/windows/%s/console.append.log", config->eucahome, instId);
      if (!check_file(pwfile)) {
	rawconsole = file2str(pwfile);
	if (rawconsole) {
	  *outConsoleOutput = base64_enc((unsigned char *)rawconsole, strlen(rawconsole));
	}
      }
      if (!*outConsoleOutput) {
	rc = 1;
      } else {
	rc = 0;
      }
    } else {
      timeout = ncGetTimeout(op_start, timeout, (stop - start), j);
      rc = ncClientCall(ccMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncGetConsoleOutput", instId, outConsoleOutput);
    }

    if (rc) {
      ret = 1;
    } else {
      ret = 0;
      done++;
    }
  }

  logprintfl(EUCADEBUG,"GetConsoleOutput(): done.\n");
  
  shawn();

  return(ret);
}

int doRebootInstances(ncMetadata *ccMeta, char **instIds, int instIdsLen) {
  int i, j, rc, numInsts, start, stop, done, timeout=0, ret=0;
  char *instId;
  ccInstance *myInstance;
  ncStub *ncs;
  time_t op_start;
  ccResourceCache resourceCacheLocal;

  i = j = numInsts = 0;
  instId = NULL;
  myInstance = NULL;
  op_start = time(NULL);

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO,"RebootInstances(): called\n");
  logprintfl(EUCADEBUG,"RebootInstances(): params: userId=%s, instIdsLen=%d\n", SP(ccMeta->userId), instIdsLen);
  
  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);

  for (i=0; i<instIdsLen; i++) {
    instId = instIds[i];
    rc = find_instanceCacheId(instId, &myInstance);
    if (!rc) {
      // found the instance in the cache
      start = myInstance->ncHostIdx;
      stop = start+1;      
      free(myInstance);
    } else {
      start = 0;
      stop = resourceCacheLocal.numResources;
    }
    
    done=0;
    for (j=start; j<stop && !done; j++) {
      timeout = ncGetTimeout(op_start, OP_TIMEOUT, (stop - start), j);
      rc = ncClientCall(ccMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncRebootInstance", instId);
      if (rc) {
	ret = 1;
      } else {
	ret = 0;
	done++;
      }      
    }
  }
  
  logprintfl(EUCADEBUG,"RebootInstances(): done.\n");

  shawn();

  return(0);
}

int doTerminateInstances(ncMetadata *ccMeta, char **instIds, int instIdsLen, int **outStatus) {
  int i, j, shutdownState, previousState, rc, start, stop, done=0, timeout, ret=0;
  char *instId;
  ccInstance *myInstance=NULL;
  ncStub *ncs;
  time_t op_start;
  ccResourceCache resourceCacheLocal;

  i = j = 0;
  instId = NULL;
  myInstance = NULL;
  op_start = time(NULL);
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO,"TerminateInstances(): called\n");
  logprintfl(EUCADEBUG,"TerminateInstances(): params: userId=%s, instIdsLen=%d, firstInstId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), instIdsLen, SP(instIdsLen ? instIds[0] : "UNSET"));
  
  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
  

  for (i=0; i<instIdsLen; i++) {
    instId = instIds[i];
    rc = find_instanceCacheId(instId, &myInstance);
    if (!rc) {
      // found the instance in the cache
      if (!strcmp(myInstance->state, "Pending") || !strcmp(myInstance->state, "Extant") || !strcmp(myInstance->state, "Unknown")) {
	start = myInstance->ncHostIdx;
	stop = start+1;
      } else {
	// instance is not in a terminatable state
	start = 0;
	stop = 0;
	(*outStatus)[i] = 0;
      }
      
      rc = free_instanceNetwork(myInstance->ccnet.privateMac, myInstance->ccnet.vlan, 1, 1);

      free(myInstance);
    } else {
      // instance is not in cache, try all resources
      start = 0;
      stop = 0;
      (*outStatus)[i] = 0;      
    }
    
    
    done=0;
    for (j=start; j<stop && !done; j++) {
      if (resourceCacheLocal.resources[j].state == RESUP) {

	if (!strstr(resourceCacheLocal.resources[j].ncURL, "EucalyptusNC")) {
	  char cdir[MAX_PATH];
	  char cfile[MAX_PATH];
	  snprintf(cdir, MAX_PATH, "%s/var/lib/eucalyptus/windows/%s/", config->eucahome, instId);
	  if (!check_directory(cdir)) {
	    snprintf(cfile, MAX_PATH, "%s/floppy", cdir);
	    if (!check_file(cfile)) unlink(cfile);
	    snprintf(cfile, MAX_PATH, "%s/console.append.log", cdir);
	    if (!check_file(cfile)) unlink(cfile);
	    rmdir(cdir);
	  }
	}

	rc = ncClientCall(ccMeta, 0, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncTerminateInstance", instId, &shutdownState, &previousState);
	if (rc) {
	  (*outStatus)[i] = 1;
	  logprintfl(EUCAWARN, "TerminateInstances(): failed to terminate '%s': instance may not exist any longer\n", instId);
	  ret = 1;
	} else {
	  (*outStatus)[i] = 0;
	  ret = 0;
	  done++;
	}
      }
    }
  }
  
  logprintfl(EUCADEBUG,"TerminateInstances(): done.\n");
  
  shawn();

  return(0);
}

int doCreateImage(ncMetadata *ccMeta, char *instanceId, char *volumeId, char *remoteDev) {
  int i, j, rc, start = 0, stop = 0, ret=0, done=0, timeout;
  ccInstance *myInstance;
  ncStub *ncs;
  time_t op_start;
  ccResourceCache resourceCacheLocal;
  
  i = j = 0;
  myInstance = NULL;
  op_start = time(NULL);
  
  rc = initialize(ccMeta);
  if (rc) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "CreateImage(): called\n");
  logprintfl(EUCADEBUG, "CreateImage(): params: userId=%s, volumeId=%s, instanceId=%s, remoteDev=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(volumeId), SP(instanceId), SP(remoteDev));
  if (!volumeId || !instanceId || !remoteDev) {
    logprintfl(EUCAERROR, "CreateImage(): bad input params\n");
    return(1);
  }
  
  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
  
  rc = find_instanceCacheId(instanceId, &myInstance);
  if (!rc) {
    // found the instance in the cache
    if (myInstance) {
      start = myInstance->ncHostIdx;
      stop = start+1;
      free(myInstance);
    }
  } else {
    start = 0;
    stop = resourceCacheLocal.numResources;
  }
  
  done=0;
  for (j=start; j<stop && !done; j++) {
    timeout = ncGetTimeout(op_start, OP_TIMEOUT, stop-start, j);
    //    rc = ncClientCall(ccMeta, timeout, NCCALL, resourceCacheLocal.resources[j].ncURL, "ncCreateImage", instanceId, volumeId, remoteDev);
    rc = ncClientCall(ccMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncCreateImage", instanceId, volumeId, remoteDev);
    if (rc) {
      ret = 1;
    } else {
      ret = 0;
      done++;
    }
  }
  
  logprintfl(EUCADEBUG,"CreateImage(): done.\n");
  
  shawn();

  return(ret);
}

int setup_shared_buffer(void **buf, char *bufname, size_t bytes, sem_t **lock, char *lockname, int mode) {
  int shd, rc, ret;
  
  // create a lock and grab it
  *lock = sem_open(lockname, O_CREAT, 0644, 1);    
  sem_wait(*lock);
  ret=0;

  if (mode == SHARED_MEM) {
    // set up shared memory segment for config
    shd = shm_open(bufname, O_CREAT | O_RDWR | O_EXCL, 0644);
    if (shd >= 0) {
      // if this is the first process to create the config, init to 0
      rc = ftruncate(shd, bytes);
    } else {
      shd = shm_open(bufname, O_CREAT | O_RDWR, 0644);
    }
    if (shd < 0) {
      fprintf(stderr, "cannot initialize shared memory segment\n");
      sem_post(*lock);
      sem_close(*lock);
      return(1);
    }
    *buf = mmap(0, bytes, PROT_READ | PROT_WRITE, MAP_SHARED, shd, 0);
  } else if (mode == SHARED_FILE) {
    char *tmpstr, path[MAX_PATH];
    struct stat mystat;
    int fd;
    
    tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
    if (!tmpstr) {
      snprintf(path, MAX_PATH, "/var/lib/eucalyptus/CC/%s", bufname);
    } else {
      snprintf(path, MAX_PATH, "%s/var/lib/eucalyptus/CC/%s", tmpstr, bufname);
    }
    fd = open(path, O_RDWR | O_CREAT, 0600);
    if (fd<0) {
      fprintf(stderr, "ERROR: cannot open/create '%s' to set up mmapped buffer\n", path);
      ret = 1;
    } else {
      mystat.st_size = 0;
      rc = fstat(fd, &mystat);
      // this is the check to make sure we're dealing with a valid prior config
      if (mystat.st_size != bytes) {
	rc = ftruncate(fd, bytes);
      }
      *buf = mmap(NULL, bytes, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0);
      if (*buf == NULL) {
	fprintf(stderr, "ERROR: cannot mmap fd\n");
	ret = 1;
      }
      close(fd);
    }
  }
  sem_post(*lock);
  return(ret);
}

int initialize(ncMetadata *ccMeta) {
  int rc, ret;

  ret=0;
  rc = init_thread();
  if (rc) {
    ret=1;
    logprintfl(EUCAERROR, "initialize(): cannot initialize thread\n");
  }

  rc = init_localstate();
  if (rc) {
    ret = 1;
    logprintfl(EUCAERROR, "initialize(): cannot initialize local state\n");
  }

  rc = init_config();
  if (rc) {
    ret=1;
    logprintfl(EUCAERROR, "initialize(): cannot initialize from configuration file\n");
  }
  
  if (config->use_tunnels) {
    rc = vnetInitTunnels(vnetconfig);
    if (rc) {
      logprintfl(EUCAERROR, "initialize(): cannot initialize tunnels\n");
    }
  }

  rc = init_pthreads();
  if (rc) {
    logprintfl(EUCAERROR, "initialize(): cannot initialize background threads\n");
    ret = 1;
  }

  if (!ret) {
    // store information from CLC that needs to be kept up-to-date in the CC
    if (ccMeta != NULL) {
      sem_mywait(CONFIG);
      memcpy(config->services, ccMeta->services, sizeof(serviceInfoType) * 16);
      sem_mypost(CONFIG);
    }
    
    // initialization went well, this thread is now initialized
    init=1;
  }
  
  return(ret);
}

int ccIsEnabled() {
  // initialized, but ccState is disabled (refuse to service operations)

  if (!config || config->ccState != ENABLED) {
    return(1);
  }
  return(0);
}

int ccIsDisabled() {
  // initialized, but ccState is disabled (refuse to service operations)

  if (!config || config->ccState != DISABLED) {
    return(1);
  }
  return(0);
}

int ccChangeState(int newstate) {
  if (config) {
    char localState[32];
    config->ccLastState = config->ccState;
    config->ccState = newstate;
    ccGetStateString(localState, 32);
    snprintf(config->ccStatus.localState, 32, "%s", localState);
    return(0);
  }
  return(1);
}

int ccGetStateString(char *statestr, int n) {
  if (config->ccState == ENABLED) {
    snprintf(statestr, n, "ENABLED");
  } else if (config->ccState == DISABLED) {
    snprintf(statestr, n, "DISABLED");
  } else if (config->ccState == STOPPED) {
    snprintf(statestr, n, "STOPPED");
  } else if (config->ccState == LOADED) {
    snprintf(statestr, n, "LOADED");
  } else if (config->ccState == INITIALIZED) {
    snprintf(statestr, n, "INITIALIZED");
  } else if (config->ccState == PRIMORDIAL) {
    snprintf(statestr, n, "PRIMORDIAL");
  }
  return(0);
}

int ccCheckState() {
  char localDetails[1024];
  int ret=0;

  if (!config) {
    return(1);
  }
  // check local configuration
  
  // configuration
  {
    char cmd[MAX_PATH];
    snprintf(cmd, MAX_PATH, "%s", config->eucahome);
    if (check_directory(cmd)) {
      logprintfl(EUCAERROR, "ccCheckState(): cannot find directory '%s'\n", cmd);
      ret++;
    }
  }
  
  // shellouts
  {
    char cmd[MAX_PATH];
    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", config->eucahome);
    if (check_file(cmd)) {
      logprintfl(EUCAERROR, "ccCheckState(): cannot find shellout '%s'\n", cmd);
      ret++;
    }

    snprintf(cmd, MAX_PATH, "%s/usr/share/eucalyptus/dynserv.pl", config->eucahome);
    if (check_file(cmd)) {
      logprintfl(EUCAERROR, "ccCheckState(): cannot find shellout '%s'\n", cmd);
      ret++;
    }
    
    snprintf(cmd, MAX_PATH, "ip addr show");
    if (system(cmd)) {
      logprintfl(EUCAERROR, "ccCheckState(): cannot run shellout '%s'\n", cmd);
      ret++;
    }    
  }
  // filesystem
  
  // network

  snprintf(localDetails, 1023, "ERRORS=%d", ret);
  sem_mywait(CONFIG);
  snprintf(config->ccStatus.details, 1023, "%s", localDetails);
  sem_mypost(CONFIG);
  return(ret);
}

/* 
   As of 1.6.2, the CC will start a background thread to poll its
   collection of nodes.  This thread populates an in-memory cache of
   instance and resource information that can be accessed via the
   regular describeInstances and describeResources calls to the CC.
   The purpose of this separation is to allow for a more scalable
   framework where describe operations do not block on access to node
   controllers.
*/
void *monitor_thread(void *in) {
  int rc;
  ncMetadata ccMeta;
  char pidfile[MAX_PATH], *pidstr=NULL;

  bzero(&ccMeta, sizeof(ncMetadata));
  ccMeta.correlationId = strdup("monitor");
  ccMeta.userId = strdup("eucalyptus");
  if (!ccMeta.correlationId || !ccMeta.userId) {
    logprintfl(EUCAFATAL, "monitor_thread(): out of memory!\n");
    unlock_exit(1);
  }

  while(1) {
    // set up default signal handler for this child process (for SIGTERM)
    struct sigaction newsigact;
    newsigact.sa_handler = SIG_DFL;
    newsigact.sa_flags = 0;
    sigemptyset(&newsigact.sa_mask);
    sigprocmask(SIG_SETMASK, &newsigact.sa_mask, NULL);
    sigaction(SIGTERM, &newsigact, NULL);

    logprintfl(EUCADEBUG, "monitor_thread(): running\n");
    
    if (config->ccState == ENABLED) {
      rc = refresh_resources(&ccMeta, 60, 1);
      if (rc) {
	logprintfl(EUCAWARN, "monitor_thread(): call to refresh_resources() failed in monitor thread\n");
      }
      
      rc = refresh_instances(&ccMeta, 60, 1);
      if (rc) {
	logprintfl(EUCAWARN, "monitor_thread(): call to refresh_instances() failed in monitor thread\n");
      }

      sem_mywait(CONFIG);

      if (config->kick_network) {
	logprintfl(EUCADEBUG, "monitor_thread(): refreshing network cache\n");
	rc = map_instanceCache(validCmp, NULL, instNetParamsSet, NULL);
	if (rc) {
	  logprintfl(EUCAERROR, "monitor_thread(): man_instanceCache() failed to reset networkparams from instanceCache\n");
	} else {
	  rc = restoreNetworkState();
	  if (rc) {
	  // failed to restore network state, continue 
	  logprintfl(EUCAWARN, "monitor_thread(): restoreNetworkState returned false (may be already restored)\n");
	  } else {
	    config->kick_network = 0;
	  }
	}
      }
    
      snprintf(pidfile, MAX_PATH, "%s/var/run/eucalyptus/net/euca-dhcp.pid", config->eucahome);
      if (!check_file(pidfile)) {
	pidstr = file2str(pidfile);
      } else {
	pidstr = NULL;
      }
      if (config->kick_dhcp || !pidstr || check_process(atoi(pidstr), "euca-dhcp.pid")) {
	rc = vnetKickDHCP(vnetconfig);
	if (rc) {
	  logprintfl(EUCAERROR, "monitor_thread(): cannot start DHCP daemon\n");
	} else {
	  config->kick_dhcp = 0;
	}
      }

      if (config->kick_enabled) {
	ccChangeState(ENABLED);
      }

      sem_mypost(CONFIG);
      
      if (config->use_proxy) {
	rc = image_cache_invalidate();
	if (rc) {
	  logprintfl(EUCAERROR, "monitor_thread(): cannot invalidate image cache\n");
	}
	
	snprintf(pidfile, MAX_PATH, "%s/var/run/eucalyptus/httpd-dynserv.pid", config->eucahome);
	pidstr = file2str(pidfile);
	if (pidstr) {
	  if (check_process(atoi(pidstr), "dynserv-httpd.conf")) {
	    rc = image_cache_proxykick(resourceCache->resources, &(resourceCache->numResources));
	    if (rc) {
	      logprintfl(EUCAERROR, "monitor_thread(): could not start proxy cache\n");
	    }
	  }
	  free(pidstr);
	} else {
	  rc = image_cache_proxykick(resourceCache->resources, &(resourceCache->numResources));
	  if (rc) {
	    logprintfl(EUCAERROR, "monitor_thread(): could not start proxy cache\n");
	  }
	}
      }

      rc = maintainNetworkState();
      if (rc) {
	logprintfl(EUCAERROR, "monitor_thread(): network state maintainance failed\n");
      }
      
    } else {
      // this CC is not enabled, ensure that local network state is disabled
      rc = clean_network_state();
      if (rc) {
	logprintfl(EUCAERROR, "monitor_thread(): could not cleanup network state\n");
      }
    }

    if (ccCheckState()) {
      logprintfl(EUCAERROR, "monitor_thread(): ccCheckState() failed\n");
    }

    shawn();
    
    logprintfl(EUCADEBUG, "monitor_thread(): done\n");
    sleep(config->ncPollingFrequency);
  }
  return(NULL);
}

int init_pthreads() {
  // start any background threads
  if (!config_init) {
    return(1);
  }
  sem_mywait(CONFIG);
  if (config->threads[MONITOR] == 0 || check_process(config->threads[MONITOR], "httpd-cc.conf")) {
    int pid;
    pid = fork();
    if (!pid) {
      // set up default signal handler for this child process (for SIGTERM)
      struct sigaction newsigact;
      newsigact.sa_handler = SIG_DFL;
      newsigact.sa_flags = 0;
      sigemptyset(&newsigact.sa_mask);
      sigprocmask(SIG_SETMASK, &newsigact.sa_mask, NULL);
      sigaction(SIGTERM, &newsigact, NULL);
      config->kick_dhcp = 1;
      config->kick_network = 1;
      monitor_thread(NULL);
      exit(0);
    } else {
      config->threads[MONITOR] = pid;
    }
  }

  sem_mypost(CONFIG);

  return(0);
}

int init_localstate(void) {
  int rc, loglevel, ret;
  char *tmpstr=NULL, logFile[MAX_PATH], configFiles[2][MAX_PATH], home[MAX_PATH], vfile[MAX_PATH];

  ret=0;
  if (local_init) {
  } else {
    // thread is not initialized, run first time local state setup
    bzero(logFile, MAX_PATH);
    bzero(home, MAX_PATH);
    bzero(configFiles[0], MAX_PATH);
    bzero(configFiles[1], MAX_PATH);
    
    tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
    if (!tmpstr) {
      snprintf(home, MAX_PATH, "/");
    } else {
      snprintf(home, MAX_PATH, "%s", tmpstr);
    }
    
    snprintf(configFiles[1], MAX_PATH, EUCALYPTUS_CONF_LOCATION, home);
    snprintf(configFiles[0], MAX_PATH, EUCALYPTUS_CONF_OVERRIDE_LOCATION, home);
    snprintf(logFile, MAX_PATH, "%s/var/log/eucalyptus/cc.log", home);  
    
    tmpstr = getConfString(configFiles, 2, "LOGLEVEL");
    if (!tmpstr) {
      loglevel = EUCADEBUG;
    } else {
      if (!strcmp(tmpstr,"DEBUG")) {loglevel=EUCADEBUG;}
      else if (!strcmp(tmpstr,"INFO")) {loglevel=EUCAINFO;}
      else if (!strcmp(tmpstr,"WARN")) {loglevel=EUCAWARN;}
      else if (!strcmp(tmpstr,"ERROR")) {loglevel=EUCAERROR;}
      else if (!strcmp(tmpstr,"FATAL")) {loglevel=EUCAFATAL;}
      else {loglevel=EUCADEBUG;}
    }
    if (tmpstr) free(tmpstr);
    // set up logfile
    logfile(logFile, loglevel);
    
    local_init=1;
  }

  return(ret);
}

int init_thread(void) {
  int rc, i;
  
  logprintfl(EUCADEBUG, "init_thread(): init=%d %08X %08X %08X %08X\n", init, config, vnetconfig, instanceCache, resourceCache);
  if (thread_init) {
    // thread has already been initialized
  } else {
    // this thread has not been initialized, set up shared memory segments
    srand(time(NULL));
    
    bzero(locks, sizeof(sem_t *) * ENDLOCK);
    bzero(mylocks, sizeof(int) * ENDLOCK);

    locks[INIT] = sem_open("/eucalyptusCCinitLock", O_CREAT, 0644, 1);
    sem_mywait(INIT);

    for (i=NCCALL0; i<=NCCALL31; i++) {
      char lockname[MAX_PATH];
      snprintf(lockname, MAX_PATH, "/eucalyptusCCncCallLock%d", i);
      locks[i] = sem_open(lockname, O_CREAT, 0644, 1);
    }
    
    if (config == NULL) {
      rc = setup_shared_buffer((void **)&config, "/eucalyptusCCConfig", sizeof(ccConfig), &(locks[CONFIG]), "/eucalyptusCCConfigLock", SHARED_FILE);
      if (rc != 0) {
	fprintf(stderr, "init_thread(): Cannot set up shared memory region for ccConfig, exiting...\n");
	sem_mypost(INIT);
	exit(1);
      }
    }
    
    if (instanceCache == NULL) {
      rc = setup_shared_buffer((void **)&instanceCache, "/eucalyptusCCInstanceCache", sizeof(ccInstanceCache), &(locks[INSTCACHE]), "/eucalyptusCCInstanceCacheLock", SHARED_FILE);
      if (rc != 0) {
	fprintf(stderr, "init_thread(): Cannot set up shared memory region for ccInstanceCache, exiting...\n");
	sem_mypost(INIT);
	exit(1);
      }
    }

    if (resourceCache == NULL) {
      rc = setup_shared_buffer((void **)&resourceCache, "/eucalyptusCCResourceCache", sizeof(ccResourceCache), &(locks[RESCACHE]), "/eucalyptusCCResourceCacheLock", SHARED_FILE);
      if (rc != 0) {
	fprintf(stderr, "init_thread(): Cannot set up shared memory region for ccResourceCache, exiting...\n");
	sem_mypost(INIT);
	exit(1);
      }
    }

    if (resourceCacheStage == NULL) {
      rc = setup_shared_buffer((void **)&resourceCacheStage, "/eucalyptusCCResourceCacheStage", sizeof(ccResourceCache), &(locks[RESCACHESTAGE]), "/eucalyptusCCResourceCacheStatgeLock", SHARED_FILE);
      if (rc != 0) {
	fprintf(stderr, "init_thread(): Cannot set up shared memory region for ccResourceCacheStage, exiting...\n");
	sem_mypost(INIT);
	exit(1);
      }
    }
    
    if (vnetconfig == NULL) {
      rc = setup_shared_buffer((void **)&vnetconfig, "/eucalyptusCCVNETConfig", sizeof(vnetConfig), &(locks[VNET]), "/eucalyptusCCVNETConfigLock", SHARED_FILE);
      if (rc != 0) {
	fprintf(stderr, "init_thread(): Cannot set up shared memory region for ccVNETConfig, exiting...\n");
	sem_mypost(INIT);
	exit(1);
      }
    }

    sem_mypost(INIT);
    thread_init=1;
  }
  return(0);
}

int update_config(void) {
  char home[MAX_PATH], *tmpstr=NULL;
  ccResource *res=NULL;
  int rc, numHosts, ret;
  time_t configMtime;
  struct stat statbuf;
  int i;

  ret = 0;

  
  configMtime = 0;
  sem_mywait(CONFIG);
  for (i=0; i<2; i++) {
    // stat the config file, update modification time
    rc = stat(config->configFiles[i], &statbuf);
    if (!rc) {
      if (statbuf.st_mtime > configMtime) {
	configMtime = statbuf.st_mtime;
      }
    }
  }
  if (configMtime == 0) {
    logprintfl(EUCAERROR, "update_config(): could not stat config files (%s,%s)\n", config->configFiles[0], config->configFiles[1]);
    sem_mypost(CONFIG);
    return(1);
  }
  
  // check to see if the configfile has changed
  if (config->configMtime != configMtime) {
    // something has changed
    logprintfl(EUCAINFO, "update_config(): config file has been modified, refreshing node list\n");
    res = NULL;
    rc = refreshNodes(config, &res, &numHosts);
    if (rc) {
      logprintfl(EUCAERROR, "update_config(): cannot read list of nodes, check your config file\n");
      sem_mywait(RESCACHE);
      resourceCache->numResources = 0;
      config->schedState = 0;
      bzero(resourceCache->resources, sizeof(ccResource) * MAXNODES);
      sem_mypost(RESCACHE);
      ret = 1;
    } else {
      sem_mywait(RESCACHE);
      if (numHosts > MAXNODES) {
	logprintfl(EUCAWARN, "update_config(): the list of nodes specified exceeds the maximum number of nodes that a single CC can support (%d).  Truncating list to %d nodes.\n", MAXNODES, MAXNODES);
	numHosts = MAXNODES;
      }
      resourceCache->numResources = numHosts;
      config->schedState = 0;
      memcpy(resourceCache->resources, res, sizeof(ccResource) * numHosts);
      sem_mypost(RESCACHE);
    }
    if (res) free(res);
  }
  
  config->configMtime = configMtime;
  sem_mypost(CONFIG);
  
  return(ret);
}

int init_config(void) {
  ccResource *res=NULL;
  char *tmpstr=NULL, *proxyIp=NULL;
  int rc, numHosts, use_wssec, use_tunnels, use_proxy, proxy_max_cache_size, schedPolicy, idleThresh, wakeThresh, ret, i;
  
  char configFiles[2][MAX_PATH], netPath[MAX_PATH], eucahome[MAX_PATH], policyFile[MAX_PATH], home[MAX_PATH], proxyPath[MAX_PATH];
  
  time_t configMtime, instanceTimeout, ncPollingFrequency, ncFanout;
  struct stat statbuf;
  
  // read in base config information
  tmpstr = getenv(EUCALYPTUS_ENV_VAR_NAME);
  if (!tmpstr) {
    snprintf(home, MAX_PATH, "/");
  } else {
    snprintf(home, MAX_PATH, "%s", tmpstr);
  }
  
  bzero(configFiles[0], MAX_PATH);
  bzero(configFiles[1], MAX_PATH);
  bzero(netPath, MAX_PATH);
  bzero(policyFile, MAX_PATH);
  
  snprintf(configFiles[1], MAX_PATH, EUCALYPTUS_CONF_LOCATION, home);
  snprintf(configFiles[0], MAX_PATH, EUCALYPTUS_CONF_OVERRIDE_LOCATION, home);
  snprintf(netPath, MAX_PATH, CC_NET_PATH_DEFAULT, home);
  snprintf(policyFile, MAX_PATH, "%s/var/lib/eucalyptus/keys/nc-client-policy.xml", home);
  snprintf(eucahome, MAX_PATH, "%s/", home);

  if (config_init && config->initialized) {
    // this means that this thread has already been initialized
    ret = 0;
    return(ret);
  }
  
  if (config->initialized) {
    config_init = 1;
    return(0);
  }
  
  logprintfl(EUCADEBUG,"init_config(): initializing CC configuration\n");  
  
  // DHCP configuration section
  {
    char *daemon=NULL,
      *dhcpuser=NULL,
      *numaddrs=NULL,
      *pubmode=NULL,
      *pubmacmap=NULL,
      *pubips=NULL,
      *pubInterface=NULL,
      *privInterface=NULL,
      *pubSubnet=NULL,
      *pubSubnetMask=NULL,
      *pubBroadcastAddress=NULL,
      *pubRouter=NULL,
      *pubDomainname=NULL,
      *pubDNS=NULL,
      *localIp=NULL,
      *cloudIp=NULL;
    uint32_t *ips, *nms;
    int initFail=0, len;
    
    // DHCP Daemon Configuration Params
    daemon = getConfString(configFiles, 2, "VNET_DHCPDAEMON");
    if (!daemon) {
      logprintfl(EUCAWARN,"init_config(): no VNET_DHCPDAEMON defined in config, using default\n");
    }
    
    dhcpuser = getConfString(configFiles, 2, "VNET_DHCPUSER");
    if (!dhcpuser) {
      dhcpuser = strdup("root");
      if (!dhcpuser) {
         logprintfl(EUCAFATAL,"init_config(): Out of memory\n");
	 unlock_exit(1);
      }
    }
    
    pubmode = getConfString(configFiles, 2, "VNET_MODE");
    if (!pubmode) {
      logprintfl(EUCAWARN,"init_config(): VNET_MODE is not defined, defaulting to 'SYSTEM'\n");
      pubmode = strdup("SYSTEM");
      if (!pubmode) {
         logprintfl(EUCAFATAL,"init_config(): Out of memory\n");
	 unlock_exit(1);
      }
    }
    
    {
      int usednew=0;
      
      pubInterface = getConfString(configFiles, 2, "VNET_PUBINTERFACE");
      if (!pubInterface) {
	logprintfl(EUCAWARN,"init_config(): VNET_PUBINTERFACE is not defined, defaulting to 'eth0'\n");
	pubInterface = strdup("eth0");
	if (!pubInterface) {
	  logprintfl(EUCAFATAL, "init_config(): out of memory!\n");
	  unlock_exit(1);
	}
      } else {
	usednew=1;
      }
      
      privInterface = NULL;
      privInterface = getConfString(configFiles, 2, "VNET_PRIVINTERFACE");
      if (!privInterface) {
	logprintfl(EUCAWARN,"init_config(): VNET_PRIVINTERFACE is not defined, defaulting to 'eth0'\n");
	privInterface = strdup("eth0");
	if (!privInterface) {
	  logprintfl(EUCAFATAL, "init_config(): out of memory!\n");
	  unlock_exit(1);
	}
	usednew = 0;
      }
      
      if (!usednew) {
	tmpstr = NULL;
	tmpstr = getConfString(configFiles, 2, "VNET_INTERFACE");
	if (tmpstr) {
	  logprintfl(EUCAWARN, "init_config(): VNET_INTERFACE is deprecated, please use VNET_PUBINTERFACE and VNET_PRIVINTERFACE instead.  Will set both to value of VNET_INTERFACE (%s) for now.\n", tmpstr);
	  if (pubInterface) free(pubInterface);
	  pubInterface = strdup(tmpstr);
	  if (!pubInterface) {
	    logprintfl(EUCAFATAL, "init_config(): out of memory!\n");
	    unlock_exit(1);
	  }

	  if (privInterface) free(privInterface);
	  privInterface = strdup(tmpstr);
	  if (!privInterface) {
	    logprintfl(EUCAFATAL, "init_config(): out of memory!\n");
	    unlock_exit(1);
	  }
	}
	if (tmpstr) free(tmpstr);
      }
    }

    if (pubmode && (!strcmp(pubmode, "STATIC") || !strcmp(pubmode, "STATIC-DYNMAC"))) {
      pubSubnet = getConfString(configFiles, 2, "VNET_SUBNET");
      pubSubnetMask = getConfString(configFiles, 2, "VNET_NETMASK");
      pubBroadcastAddress = getConfString(configFiles, 2, "VNET_BROADCAST");
      pubRouter = getConfString(configFiles, 2, "VNET_ROUTER");
      pubDNS = getConfString(configFiles, 2, "VNET_DNS");
      pubDomainname = getConfString(configFiles, 2, "VNET_DOMAINNAME");
      pubmacmap = getConfString(configFiles, 2, "VNET_MACMAP");
      pubips = getConfString(configFiles, 2, "VNET_PUBLICIPS");

      if (!pubSubnet || !pubSubnetMask || !pubBroadcastAddress || !pubRouter || !pubDNS || (!pubmacmap && !pubips)) {
	logprintfl(EUCAFATAL,"init_config(): in 'STATIC' network modes, you must specify values for 'VNET_SUBNET, VNET_NETMASK, VNET_BROADCAST, VNET_ROUTER, VNET_DNS, and (VNET_MACMAP or VNET_PUBLICIPS)'\n");
	initFail = 1;
      }
    } else if (pubmode && (!strcmp(pubmode, "MANAGED") || !strcmp(pubmode, "MANAGED-NOVLAN"))) {
      numaddrs = getConfString(configFiles, 2, "VNET_ADDRSPERNET");
      pubSubnet = getConfString(configFiles, 2, "VNET_SUBNET");
      pubSubnetMask = getConfString(configFiles, 2, "VNET_NETMASK");
      pubDNS = getConfString(configFiles, 2, "VNET_DNS");
      pubDomainname = getConfString(configFiles, 2, "VNET_DOMAINNAME");
      pubips = getConfString(configFiles, 2, "VNET_PUBLICIPS");
      localIp = getConfString(configFiles, 2, "VNET_LOCALIP");
      if (!localIp) {
	logprintfl(EUCAWARN, "init_config(): VNET_LOCALIP not defined, will attempt to auto-discover (consider setting this explicitly if tunnelling does not function properly.)\n");
      }
      cloudIp = getConfString(configFiles, 2, "VNET_CLOUDIP");

      if (!pubSubnet || !pubSubnetMask || !pubDNS || !numaddrs) {
	logprintfl(EUCAFATAL,"init_config(): in 'MANAGED' or 'MANAGED-NOVLAN' network mode, you must specify values for 'VNET_SUBNET, VNET_NETMASK, VNET_ADDRSPERNET, and VNET_DNS'\n");
	initFail = 1;
      }
    }
    
    if (initFail) {
      logprintfl(EUCAFATAL, "init_config(): bad network parameters, must fix before system will work\n");
      if (cloudIp) free(cloudIp);
      if (pubSubnet) free(pubSubnet);
      if (pubSubnetMask) free(pubSubnetMask);
      if (pubBroadcastAddress) free(pubBroadcastAddress);
      if (pubRouter) free(pubRouter);
      if (pubDomainname) free(pubDomainname);
      if (pubDNS) free(pubDNS);
      if (pubmacmap) free(pubmacmap);
      if (numaddrs) free(numaddrs);
      if (pubips) free(pubips);
      if (localIp) free(localIp);
      if (pubInterface) free(pubInterface);
      if (privInterface) free(privInterface);
      if (dhcpuser) free(dhcpuser);
      if (daemon) free(daemon);
      if (pubmode) free(pubmode);
      return(1);
    }
    
    sem_mywait(VNET);
    
    vnetInit(vnetconfig, pubmode, eucahome, netPath, CLC, pubInterface, privInterface, numaddrs, pubSubnet, pubSubnetMask, pubBroadcastAddress, pubDNS, pubDomainname, pubRouter, daemon, dhcpuser, NULL, localIp, cloudIp);
    if (cloudIp) free(cloudIp);
    if (pubSubnet) free(pubSubnet);
    if (pubSubnetMask) free(pubSubnetMask);
    if (pubBroadcastAddress) free(pubBroadcastAddress);
    if (pubDomainname) free(pubDomainname);
    if (pubDNS) free(pubDNS);
    if (pubRouter) free(pubRouter);
    if (numaddrs) free(numaddrs);
    if (pubmode) free(pubmode);
    if (dhcpuser) free(dhcpuser);
    if (daemon) free(daemon);
    if (privInterface) free(privInterface);
    if (pubInterface) free(pubInterface);
    
    vnetAddDev(vnetconfig, vnetconfig->privInterface);

    if (pubmacmap) {
      char *mac=NULL, *ip=NULL, *ptra=NULL, *toka=NULL, *ptrb=NULL;
      toka = strtok_r(pubmacmap, " ", &ptra);
      while(toka) {
	mac = ip = NULL;
	mac = strtok_r(toka, "=", &ptrb);
	ip = strtok_r(NULL, "=", &ptrb);
	if (mac && ip) {
	  vnetAddHost(vnetconfig, mac, ip, 0, -1);
	}
	toka = strtok_r(NULL, " ", &ptra);
      }
      vnetKickDHCP(vnetconfig);
      free(pubmacmap);
    } else if (pubips) {
      char *ip, *ptra, *toka;
      toka = strtok_r(pubips, " ", &ptra);
      while(toka) {
	ip = toka;
	if (ip) {
	  rc = vnetAddPublicIP(vnetconfig, ip);
	  if (rc) {
	    logprintfl(EUCAERROR, "init_config(): could not add public IP '%s'\n", ip);
	  }
	}
	toka = strtok_r(NULL, " ", &ptra);
      }

      // detect and populate ips
      if (vnetCountLocalIP(vnetconfig) <= 0) {
	ips = nms = NULL;
	rc = getdevinfo("all", &ips, &nms, &len);
	if (!rc) {
	  for (i=0; i<len; i++) {
	    char *theip=NULL;
	    theip = hex2dot(ips[i]);
	    if (vnetCheckPublicIP(vnetconfig, theip)) {
	      vnetAddLocalIP(vnetconfig, ips[i]);
	    }
	    if (theip) free(theip);
	  }
	}
	if (ips) free(ips);
	if (nms) free(nms);
      }
      free(pubips);
    }
    
    sem_mypost(VNET);
  }
  
  tmpstr = getConfString(configFiles, 2, "SCHEDPOLICY");
  if (!tmpstr) {
    // error
    logprintfl(EUCAWARN,"init_config(): parsing config file (%s) for SCHEDPOLICY, defaulting to GREEDY\n", configFiles[0]);
    schedPolicy = SCHEDGREEDY;
    tmpstr = NULL;
  } else {
    if (!strcmp(tmpstr, "GREEDY")) schedPolicy = SCHEDGREEDY;
    else if (!strcmp(tmpstr, "ROUNDROBIN")) schedPolicy = SCHEDROUNDROBIN;
    else if (!strcmp(tmpstr, "POWERSAVE")) schedPolicy = SCHEDPOWERSAVE;
    else schedPolicy = SCHEDGREEDY;
  }
  if (tmpstr) free(tmpstr);

  // powersave options
  tmpstr = getConfString(configFiles, 2, "POWER_IDLETHRESH");
  if (!tmpstr) {
    logprintfl(EUCAWARN,"init_config(): parsing config file (%s) for POWER_IDLETHRESH, defaulting to 300 seconds\n", configFiles[0]);
    idleThresh = 300;
    tmpstr = NULL;
  } else {
    idleThresh = atoi(tmpstr);
    if (idleThresh < 300) {
      logprintfl(EUCAWARN, "init_config(): POWER_IDLETHRESH set too low (%d seconds), resetting to minimum (300 seconds)\n", idleThresh);
      idleThresh = 300;
    }
  }
  if (tmpstr) free(tmpstr);

  tmpstr = getConfString(configFiles, 2, "POWER_WAKETHRESH");
  if (!tmpstr) {
    logprintfl(EUCAWARN,"init_config(): parsing config file (%s) for POWER_WAKETHRESH, defaulting to 300 seconds\n", configFiles[0]);
    wakeThresh = 300;
    tmpstr = NULL;
  } else {
    wakeThresh = atoi(tmpstr);
    if (wakeThresh < 300) {
      logprintfl(EUCAWARN, "init_config(): POWER_WAKETHRESH set too low (%d seconds), resetting to minimum (300 seconds)\n", wakeThresh);
      wakeThresh = 300;
    }
  }
  if (tmpstr) free(tmpstr);

  // some administrative options
  tmpstr = getConfString(configFiles, 2, "NC_POLLING_FREQUENCY");
  if (!tmpstr) {
    ncPollingFrequency = 6;
    tmpstr = NULL;
  } else {
    ncPollingFrequency = atoi(tmpstr);
    if (ncPollingFrequency < 6) {
      logprintfl(EUCAWARN, "init_config(): NC_POLLING_FREQUENCY set too low (%d seconds), resetting to minimum (6 seconds)\n", ncPollingFrequency);
      ncPollingFrequency = 6;
    }
  }
  if (tmpstr) free(tmpstr);

  tmpstr = getConfString(configFiles, 2, "NC_FANOUT");
  if (!tmpstr) {
    ncFanout = 1;
    tmpstr = NULL;
  } else {
    ncFanout = atoi(tmpstr);
    if (ncFanout < 1 || ncFanout > 32) {
      logprintfl(EUCAWARN, "init_config(): NC_FANOUT set out of bounds (min=%d max=%d) (current=%d), resetting to default (1 NC)\n", 1, 32, ncFanout);
      ncFanout = 1;
    }
  }
  if (tmpstr) free(tmpstr);

  tmpstr = getConfString(configFiles, 2, "INSTANCE_TIMEOUT");
  if (!tmpstr) {
    instanceTimeout = 300;
    tmpstr = NULL;
  } else {
    instanceTimeout = atoi(tmpstr);
    if (instanceTimeout < 30) {
      logprintfl(EUCAWARN, "init_config(): INSTANCE_TIMEOUT set too low (%d seconds), resetting to minimum (30 seconds)\n", instanceTimeout);
      instanceTimeout = 30;
    }
  }
  if (tmpstr) free(tmpstr);

  // WS-Security
  use_wssec = 0;
  tmpstr = getConfString(configFiles, 2, "ENABLE_WS_SECURITY");
  if (!tmpstr) {
    // error
    logprintfl(EUCAFATAL,"init_config(): parsing config file (%s) for ENABLE_WS_SECURITY\n", configFiles[0]);
    return(1);
  } else {
    if (!strcmp(tmpstr, "Y")) {
      use_wssec = 1;
    }
  }
  if (tmpstr) free(tmpstr);

  // Multi-cluster tunneling
  use_tunnels = 1;
  tmpstr = getConfString(configFiles, 2, "DISABLE_TUNNELING");
  if (tmpstr) {
    if (!strcmp(tmpstr, "Y")) {
      use_tunnels = 0;
    }
  }
  if (tmpstr) free(tmpstr);

  // CC Image Caching
  proxyIp = NULL;
  use_proxy=0;
  tmpstr = getConfString(configFiles, 2, "CC_IMAGE_PROXY");
  if (tmpstr) {
    proxyIp = strdup(tmpstr);
    use_proxy=1;
  }
  if (tmpstr) free(tmpstr);

  proxy_max_cache_size=32768;
  tmpstr = getConfString(configFiles, 2, "CC_IMAGE_PROXY_CACHE_SIZE");
  if (tmpstr) {
    proxy_max_cache_size = atoi(tmpstr);
  }
  if (tmpstr) free(tmpstr);

  tmpstr = getConfString(configFiles, 2, "CC_IMAGE_PROXY_PATH");
  if (tmpstr) {
    snprintf(proxyPath, MAX_PATH, "%s", tmpstr);
  } else {
    snprintf(proxyPath, MAX_PATH, "%s/var/lib/eucalyptus/dynserv", eucahome);
  }

  sem_mywait(CONFIG);
  // set up the current config   
  strncpy(config->eucahome, eucahome, MAX_PATH);
  strncpy(config->policyFile, policyFile, MAX_PATH);
  //  snprintf(config->proxyPath, MAX_PATH, "%s/var/lib/eucalyptus/dynserv/data", config->eucahome);
  snprintf(config->proxyPath, MAX_PATH, "%s", proxyPath);
  config->use_proxy = use_proxy;
  config->proxy_max_cache_size = proxy_max_cache_size;
  if (use_proxy) {
    snprintf(config->proxyIp, 32, "%s", proxyIp);
  }
  config->use_wssec = use_wssec;
  config->use_tunnels = use_tunnels;
  config->schedPolicy = schedPolicy;
  config->idleThresh = idleThresh;
  config->wakeThresh = wakeThresh;
  //  config->configMtime = configMtime;
  config->instanceTimeout = instanceTimeout;
  config->ncPollingFrequency = ncPollingFrequency;
  config->ncFanout = ncFanout;
  locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
  config->initialized = 1;
  ccChangeState(ENABLED);
  snprintf(config->configFiles[0], MAX_PATH, "%s", configFiles[0]);
  snprintf(config->configFiles[1], MAX_PATH, "%s", configFiles[1]);
  
  logprintfl(EUCAINFO, "init_config(): CC Configuration: eucahome=%s, policyfile=%s, ws-security=%s, schedulerPolicy=%s, idleThreshold=%d, wakeThreshold=%d\n", SP(config->eucahome), SP(config->policyFile), use_wssec ? "ENABLED" : "DISABLED", SP(SCHEDPOLICIES[config->schedPolicy]), config->idleThresh, config->wakeThresh);

  sem_mypost(CONFIG);

  res = NULL;
  rc = refreshNodes(config, &res, &numHosts);
  if (rc) {
    logprintfl(EUCAERROR, "init_config(): cannot read list of nodes, check your config file\n");
    return(1);
  }
      
  // update resourceCache
  sem_mywait(RESCACHE);
  resourceCache->numResources = numHosts;
  if (numHosts) {
    memcpy(resourceCache->resources, res, sizeof(ccResource) * numHosts);
  }
  if (res) free(res);
  resourceCache->lastResourceUpdate = 0;
  sem_mypost(RESCACHE);
  
  config_init=1;
  logprintfl(EUCADEBUG,"init_config(): done\n");
  
  return(0);
}

int maintainNetworkState() {
  int rc, i, ret=0;
  time_t startTime, startTimeA;
  uint32_t cloudIp;


  /*  rc = reconfigureNetworkFromCLC();
  if (rc) {
    logprintfl(EUCAWARN, "maintainNetworkState(): cannot get network ground truth from CLC\n");
  }
  */

  // find current CLC IP
  cloudIp = vnetconfig->cloudIp;
  for (i=0; i<16; i++) {
    int j;
    logprintfl(EUCADEBUG, "maintainNetworkState(): internal serviceInfos type=%s name=%s urisLen=%d\n", config->services[i].type, config->services[i].name, config->services[i].urisLen);
    for (j=0; j<8; j++) {
      if (strlen(config->services[i].uris[j])) {
	logprintfl(EUCADEBUG, "maintainNetworkState(): internal serviceInfos\t uri[%d]:%s\n", j, config->services[i].uris[j]);
      }
    }
  }
  
  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    sem_mywait(VNET);
    
    // check to see if cloudIp has changed
    if (cloudIp && (cloudIp != vnetconfig->cloudIp)) {
      vnetconfig->cloudIp = cloudIp;
      rc = vnetSetMetadataRedirect(vnetconfig);
      if (rc) {
	logprintfl(EUCAWARN, "maintainNetworkState(): failed to set metadata redirect\n");
      }
    }

    rc = vnetSetupTunnels(vnetconfig);
    if (rc) {
      logprintfl(EUCAERROR, "maintainNetworkState(): failed to setup tunnels during maintainNetworkState()\n");
      ret = 1;
    }
    
    for (i=2; i<NUMBER_OF_VLANS; i++) {
      if (vnetconfig->networks[i].active) {
	char brname[32];
	if (!strcmp(vnetconfig->mode, "MANAGED")) {
	  snprintf(brname, 32, "eucabr%d", i);
	} else {
	  snprintf(brname, 32, "%s", vnetconfig->privInterface);
	}
	startTime=time(NULL);
	rc = vnetAttachTunnels(vnetconfig, i, brname);
	if (rc) {
	  logprintfl(EUCADEBUG, "maintainNetworkState(): failed to attach tunnels for vlan %d during maintainNetworkState()\n", i);
	  ret = 1;
	}
      }
    }
    sem_mypost(VNET);
  }
  
  return(ret);
}

int restoreNetworkState() {
  int rc, ret=0, i;
  char cmd[MAX_PATH];

  /* this function should query both internal and external information sources and restore the CC to correct networking state
     1.) restore from internal instance state
         - local IPs (instance and cloud)
         - networks (bridges)
     2.) query CLC for sec. group rules and apply (and/or apply from in-memory iptables?)
     3.) (re)start local network processes (dhcpd)
  */

  logprintfl(EUCADEBUG, "restoreNetworkState(): restoring network state\n");
  sem_mywait(VNET);

  // restore iptables state                                                                                    
  logprintfl(EUCADEBUG, "restoreNetworkState(): restarting iptables\n");
  rc = vnetRestoreTablesFromMemory(vnetconfig);
  if (rc) {
    logprintfl(EUCAERROR, "restoreNetworkState(): cannot restore iptables state\n");
    ret = 1;
  }
  
  // restore ip addresses                                                                                      
  logprintfl(EUCADEBUG, "restoreNetworkState(): restarting ips\n");
  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr add 169.254.169.254/32 scope link dev %s", config->eucahome, vnetconfig->privInterface);
    logprintfl(EUCADEBUG,"restoreNetworkState(): running cmd %s\n", cmd);
    rc = system(cmd);
    if (rc) {
      logprintfl(EUCAWARN, "restoreNetworkState(): cannot add ip 169.254.169.254\n");
    }
  }
  for (i=1; i<NUMBER_OF_PUBLIC_IPS; i++) {
    if (vnetconfig->publicips[i].allocated) {
      char *tmp;

      tmp = hex2dot(vnetconfig->publicips[i].ip);
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr add %s/32 dev %s", config->eucahome, tmp, vnetconfig->pubInterface);
      logprintfl(EUCADEBUG,"restoreNetworkState(): running cmd %s\n", cmd);
      rc = system(cmd);
      if (rc) {
        logprintfl(EUCAWARN, "restoreNetworkState(): cannot add ip %s\n", tmp);
      }
      free(tmp);
    }
  }

  // re-create all active networks
  logprintfl(EUCADEBUG, "restoreNetworkState(): restarting networks\n");
  for (i=2; i<NUMBER_OF_VLANS; i++) {
    if (vnetconfig->networks[i].active) {
      char *brname=NULL;
      logprintfl(EUCADEBUG, "restoreNetworkState(): found active network: %d\n", i);
      rc = vnetStartNetwork(vnetconfig, i, NULL, vnetconfig->users[i].userName, vnetconfig->users[i].netName, &brname);
      if (rc) {
        logprintfl(EUCADEBUG, "restoreNetworkState(): failed to reactivate network: %d", i);
      }
      if (brname) free(brname);
    }
  }
  // get DHCPD back up and running
  logprintfl(EUCADEBUG, "restoreNetworkState(): restarting DHCPD\n");
  rc = vnetKickDHCP(vnetconfig);
  if (rc) {
    logprintfl(EUCAERROR, "restoreNetworkState(): cannot start DHCP daemon, please check your network settings\n");
    ret = 1;
  }

  sem_mypost(VNET);

  //  rc = reconfigureNetworkFromCLC();
  //  if (rc) {
  //    logprintfl(EUCAWARN, "restoreNetworkState(): cannot get network ground truth from CLC\n");
  //  }

  logprintfl(EUCADEBUG, "restoreNetworkState(): done restoring network state\n");

  return(ret);
}

int reconfigureNetworkFromCLC() {
  FILE *FH;
  char buf[1024], *tok=NULL, *start=NULL, *save=NULL, *type=NULL, *dgroup=NULL, *linetok=NULL, *linesave=NULL, *dname=NULL, *duser=NULL, tmpfile[MAX_PATH];
  char **suser, **sgroup, **snet, *protocol=NULL;
  char snettok[1024], range[1024];
  int minport=0, maxport=0, slashnet=0, snetset=0, rc, snetLen=0, susergroupLen=0, fd=0;

  snettok[0] = '\0';
  range[0] = '\0';

  // TODO: grab CLC ip from serviceInfo / ccConfig
  snprintf(tmpfile, MAX_PATH, "/tmp/euca-clcnet-XXXXXX");
  
  fd = mkstemp(tmpfile);
  if (fd < 0) {
    logprintfl(EUCAERROR, "reconfigureNetworkFromCLC(): cannot open tmpfile '%s'\n", tmpfile);
    return(1);
  }
  chmod(tmpfile, 0644);
  close(fd);

  rc = http_get("http://localhost:8773/latest/network-topology", tmpfile);
  if (rc) {
    logprintfl(EUCAWARN, "reconfigureNetworkFromCLC(): cannot get latest network topology from cloud controller\n");
    unlink(tmpfile);
    return(1);
  }

  FH = fopen(tmpfile, "r");
  if (!FH) {
    logprintfl(EUCAWARN, "reconfigureNetworkFromCLC(): cannot open tmpfile /tmp/mehfoo\n");
    unlink(tmpfile);
    return(1);
  }

  while(fgets(buf, 1024, FH)) {
    start = buf;
    logprintfl(EUCADEBUG, "LINE: %s\n", buf);
    tok = strchr(buf, '\n');
    if (tok) {
      *tok='\0';
      tok=NULL;
    }

    snetset = minport = maxport = snetLen = susergroupLen = 0;
    suser = sgroup = snet = NULL;
    dname = duser = NULL;
    protocol = NULL;
    
    type = strtok_r(start, " ", &save);
    if (type && !strcmp(type, "RULE")) {
      dgroup = strtok_r(NULL, " ", &save);
      if (dgroup) {
	while((tok = strtok_r(NULL, " ", &save))) {
	  logprintfl(EUCADEBUG, "\tTOK: %s\n", tok);
	  if (tok && !strcmp(tok, "-P")) {
	    tok = strtok_r(NULL, " ", &save);
	    if (tok) {
	      logprintfl(EUCADEBUG, "PROTOCOL: %s\n", tok);
	      protocol = strdup(tok);
	    }
	  } else if (tok && !strcmp(tok, "-p")) {
	    tok = strtok_r(NULL, " ", &save);
	    if (tok) {
	      logprintfl(EUCADEBUG, "PORTRANGE: %s\n", tok);
	      snprintf(range, 1024, "%s", tok);
	    }
	  } else if (tok && !strcmp(tok, "-t")) {
	    tok = strtok_r(NULL, " ", &save);
	    if (tok) {
	      logprintfl(EUCADEBUG, "TYPERANGE: %s\n", tok);
	      snprintf(range, 1024, "%s", tok);
	    }
	  } else if (tok && !strcmp(tok, "-o")) {
	    tok = strtok_r(NULL, " ", &save);
	    if (tok) {
	      logprintfl(EUCADEBUG, "DGROUP: %s\n", tok);
	      sgroup = malloc(sizeof(char *));
	      sgroup[0] = strdup(tok);
	    }
	  } else if (tok && !strcmp(tok, "-u")) {
	    tok = strtok_r(NULL, " ", &save);
	    if (tok) {
	      logprintfl(EUCADEBUG, "DUSER: %s\n", tok);
	      suser = malloc(sizeof(char *));
	      suser[0] = strdup(tok);
	    }
	  } else if (tok && !strcmp(tok, "-s")) {
	    tok = strtok_r(NULL, " ", &save);
	    if (tok) {
	      logprintfl(EUCADEBUG, "SNET: %s\n", tok);
	      snet = malloc(sizeof(char *));
	      snet[0] = strdup(tok);
	      snetset=1;
	    }
	  }
	  start = NULL;
	}

	if (!strcmp(protocol, "tcp") || !strcmp(protocol, "udp")) {
	  sscanf(range, "%d-%d", &minport, &maxport);
	} else if (!strcmp(protocol, "icmp")) {
	  sscanf(range, "%d:%d", &minport, &maxport);
	}


	if (snetset) {
	  /*
	  int a, b, c, d;
	  sscanf(snettok, "%d.%d.%d.%d/%d", &a, &b, &c, &d, &slashnet);
	  snet = malloc(sizeof(char *));
	  snet[0] = malloc(sizeof(char) * 16);
	  snprintf(snet[0], 16, "%d.%d.%d.%d", a, b, c, d);
	  */
	  logprintfl(EUCADEBUG, "protocol=%s minport=%d maxport=%d snet=%s\n", protocol, minport, maxport, snet[0]);
	  snetLen = 1;
	} else {
	  logprintfl(EUCADEBUG, "protocol=%s minport=%d maxport=%d suser=%s sgroup=%s\n", protocol, minport, maxport, suser[0], sgroup[0]);
	  susergroupLen = 1;
	}
	
	// call doConfigureNetwork here
	dname = dgroup;
	duser = strchr(dgroup, '-');
	*duser = '\0';
	duser++;
	rc = doConfigureNetwork(NULL, "firewall-open", susergroupLen, sgroup, suser, snetLen, snet, duser, dname, protocol, minport, maxport);
	
	if (protocol) free(protocol);
	if (suser) {
	  if (suser[0]) free(suser[0]);
	  free(suser);
	}
	if (sgroup) {
	  if (sgroup[0]) free(sgroup[0]);
	  free(sgroup);
	}
	if (snet) {
	  if (snet[0]) free(snet[0]);
	  free(snet);
	}

      }
    }
  }
  fclose(FH);
  unlink(tmpfile);

  return(0);
}

int refreshNodes(ccConfig *config, ccResource **res, int *numHosts) {
  int rc, i, lockmod;
  char *tmpstr, *ipbuf;
  char ncservice[512];
  int ncport;
  char **hosts;

  *numHosts = 0;
  *res = NULL;

  tmpstr = getConfString(config->configFiles, 2, CONFIG_NC_SERVICE);
  if (!tmpstr) {
    // error
    logprintfl(EUCAFATAL,"refreshNodes(): parsing config files (%s,%s) for NC_SERVICE\n", config->configFiles[1], config->configFiles[0]);
    return(1);
  } else {
    if(tmpstr) {
      snprintf(ncservice, 512, "%s", tmpstr);
    }

  }
  if (tmpstr) free(tmpstr);

  tmpstr = getConfString(config->configFiles, 2, CONFIG_NC_PORT);
  if (!tmpstr) {
    // error
    logprintfl(EUCAFATAL,"refreshNodes(): parsing config files (%s,%s) for NC_PORT\n", config->configFiles[1], config->configFiles[0]);
    return(1);
  } else {
    if(tmpstr)
      ncport = atoi(tmpstr);
  }
  if (tmpstr) free(tmpstr);

  tmpstr = getConfString(config->configFiles, 2, CONFIG_NODES);
  if (!tmpstr) {
    // error
    logprintfl(EUCAWARN,"refreshNodes(): NODES parameter is missing from config files(%s,%s)\n", config->configFiles[1], config->configFiles[0]);
    return(0);
  } else {
    hosts = from_var_to_char_list(tmpstr);
    if (hosts == NULL) {
      logprintfl(EUCAWARN,"refreshNodes(): NODES list is empty in config files(%s,%s)\n", config->configFiles[1], config->configFiles[0]);
      if (tmpstr) free(tmpstr);
      return(0);
    }

    *numHosts = 0;
    lockmod = 0;
    i = 0;
    while(hosts[i] != NULL) {
      (*numHosts)++;
      *res = realloc(*res, sizeof(ccResource) * *numHosts);
      bzero(&((*res)[*numHosts-1]), sizeof(ccResource));
      snprintf((*res)[*numHosts-1].hostname, 128, "%s", hosts[i]);

      ipbuf = host2ip(hosts[i]);
      if (ipbuf) {
	snprintf((*res)[*numHosts-1].ip, 24, "%s", ipbuf);
      }
      if (ipbuf) free(ipbuf);

      (*res)[*numHosts-1].ncPort = ncport;
      snprintf((*res)[*numHosts-1].ncService, 128, "%s", ncservice);
      snprintf((*res)[*numHosts-1].ncURL, 128, "http://%s:%d/%s", hosts[i], ncport, ncservice);	
      (*res)[*numHosts-1].state = RESDOWN;
      (*res)[*numHosts-1].lastState = RESDOWN;
      (*res)[*numHosts-1].lockidx = NCCALL0 + lockmod;
      lockmod = (lockmod + 1) % 32;
      free(hosts[i]);
      i++;
    }
  }

  rc = image_cache_proxykick(*res, numHosts);
  if (rc) {
    logprintfl(EUCAERROR, "refreshNodes(): could not restart the image proxy\n");
  }

  if (hosts) free(hosts);
  if (tmpstr) free(tmpstr);


  return(0);
}

void shawn() {
  int p=1, status, rc;

  // clean up any orphaned child processes
  while(p > 0) {
    p = waitpid(-1, &status, WNOHANG);
  }
  
  if (instanceCache) msync(instanceCache, sizeof(ccInstanceCache), MS_ASYNC);
  if (resourceCache) msync(resourceCache, sizeof(ccResourceCache), MS_ASYNC);
  if (config) msync(config, sizeof(ccConfig), MS_ASYNC);
  if (vnetconfig) msync(vnetconfig, sizeof(vnetConfig), MS_ASYNC);

}

int timeread(int fd, void *buf, size_t bytes, int timeout) {
  int rc;
  fd_set rfds;
  struct timeval tv;

  if (timeout <= 0) timeout = 1;

  FD_ZERO(&rfds);
  FD_SET(fd, &rfds);
  
  tv.tv_sec = timeout;
  tv.tv_usec = 0;
  
  rc = select(fd+1, &rfds, NULL, NULL, &tv);
  if (rc <= 0) {
    // timeout
    logprintfl(EUCAERROR, "timeread(): select() timed out for read: timeout=%d\n", timeout);
    return(-1);
  }
  rc = read(fd, buf, bytes);
  return(rc);
}

int allocate_ccResource(ccResource *out, char *ncURL, char *ncService, int ncPort, char *hostname, char *mac, char *ip, int maxMemory, int availMemory, int maxDisk, int availDisk, int maxCores, int availCores, int state, int laststate, time_t stateChange, time_t idleStart) {

  if (out != NULL) {
    if (ncURL) strncpy(out->ncURL, ncURL, 128);
    if (ncService) strncpy(out->ncService, ncService, 128);
    if (hostname) strncpy(out->hostname, hostname, 128);
    if (mac) strncpy(out->mac, mac, 24);
    if (ip) strncpy(out->ip, ip, 24);
    
    out->ncPort = ncPort;
    out->maxMemory = maxMemory;
    out->availMemory = availMemory;
    out->maxDisk = maxDisk;
    out->availDisk = availDisk;
    out->maxCores = maxCores;
    out->availCores = availCores;
    out->state = state;
    out->lastState = laststate;
    out->stateChange = stateChange;
    out->idleStart = idleStart;
  }

  return(0);
}

int free_instanceNetwork(char *mac, int vlan, int force, int dolock) {
  int inuse, i;
  unsigned char hexmac[6];
  mac2hex(mac, hexmac);
  if (!maczero(hexmac)) {
    return(0);
  }

  if (dolock) {
    sem_mywait(INSTCACHE);
  }

  inuse=0;
  if (!force) {
    // check to make sure the mac isn't in use elsewhere
    for (i=0; i<MAXINSTANCES && !inuse; i++) {
      if (!strcmp(instanceCache->instances[i].ccnet.privateMac, mac) && strcmp(instanceCache->instances[i].state, "Teardown")) {
	inuse++;
      }
    }
  }

  if (dolock) {
    sem_mypost(INSTCACHE);
  }

  if (!inuse) {
    // remove private network info from system                                                                                                                                                                
    sem_mywait(VNET);
    vnetDisableHost(vnetconfig, mac, NULL, 0);
    if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
      vnetDelHost(vnetconfig, mac, NULL, vlan);
    }
    sem_mypost(VNET);
  }
  return(0);
}

int allocate_ccInstance(ccInstance *out, char *id, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char *ownerId, char *state, time_t ts, char *reservationId, netConfig *ccnet, virtualMachine *ccvm, int ncHostIdx, char *keyName, char *serviceTag, char *userData, char *launchIndex, char *platform, char *bundleTaskStateName, char groupNames[][32], ncVolume *volumes, int volumesSize) {
  if (out != NULL) {
    bzero(out, sizeof(ccInstance));
    if (id) strncpy(out->instanceId, id, 16);
    if (amiId) strncpy(out->amiId, amiId, 16);
    if (kernelId) strncpy(out->kernelId, kernelId, 16);
    if (ramdiskId) strncpy(out->ramdiskId, ramdiskId, 16);
    
    if (amiURL) strncpy(out->amiURL, amiURL, 512);
    if (kernelURL) strncpy(out->kernelURL, kernelURL, 512);
    if (ramdiskURL) strncpy(out->ramdiskURL, ramdiskURL, 512);
    
    if (state) strncpy(out->state, state, 16);
    if (ownerId) strncpy(out->ownerId, ownerId, 16);
    if (reservationId) strncpy(out->reservationId, reservationId, 16);
    if (keyName) strncpy(out->keyName, keyName, 1024);
    out->ts = ts;
    out->ncHostIdx = ncHostIdx;
    if (serviceTag) strncpy(out->serviceTag, serviceTag, 64);
    if (userData) strncpy(out->userData, userData, 4096);
    if (launchIndex) strncpy(out->launchIndex, launchIndex, 64);
    if (platform) strncpy(out->platform, platform, 64);
    if (bundleTaskStateName) strncpy(out->bundleTaskStateName, bundleTaskStateName, 64);
    if (groupNames) {
      int i;
      for (i=0; i<64; i++) {
	if (groupNames[i]) {
	  strncpy(out->groupNames[i], groupNames[i], 32);
	}
      }
    }

    if (volumes) {
      memcpy(out->volumes, volumes, sizeof(ncVolume) * EUCA_MAX_VOLUMES);
    }
    out->volumesSize = volumesSize;
    //    if (networkIndex) out->networkIndex = networkIndex;

    if (ccnet) allocate_netConfig(&(out->ccnet), ccnet->privateMac, ccnet->privateIp, ccnet->publicIp, ccnet->vlan, ccnet->networkIndex);
    if (ccvm) allocate_virtualMachine(&(out->ccvm), ccvm);
  }
  return(0);
}

int pubIpCmp(ccInstance *inst, void *ip) {
  if (!ip || !inst) {
    return(1);
  }
  
  if (!strcmp((char *)ip, inst->ccnet.publicIp)) {
    return(0);
  }
  return(1);
}

int privIpCmp(ccInstance *inst, void *ip) {
  if (!ip || !inst) {
    return(1);
  }
  
  if (!strcmp((char *)ip, inst->ccnet.privateIp)) {
    return(0);
  }
  return(1);
}

int privIpSet(ccInstance *inst, void *ip) {
  if (!ip || !inst) {
    return(1);
  }
  
  logprintfl(EUCADEBUG, "privIpSet(): set: %s/%s\n", inst->ccnet.privateIp, (char *)ip);
  snprintf(inst->ccnet.privateIp, 24, "%s", (char *)ip);
  return(0);
}

int pubIpSet(ccInstance *inst, void *ip) {
  if (!ip || !inst) {
    return(1);
  }
  
  logprintfl(EUCADEBUG, "pubIpSet(): set: %s/%s\n", inst->ccnet.publicIp, (char *)ip);
  snprintf(inst->ccnet.publicIp, 24, "%s", (char *)ip);
  return(0);
}

int map_instanceCache(int (*match)(ccInstance *, void *), void *matchParam, int (*operate)(ccInstance *, void *), void *operateParam) {
  int i, ret=0;
  
  sem_mywait(INSTCACHE);

  for (i=0; i<MAXINSTANCES; i++) {
    if (!match(&(instanceCache->instances[i]), matchParam)) {
      if (operate(&(instanceCache->instances[i]), operateParam)) {
	logprintfl(EUCAWARN, "map_instanceCache(): failed to operate at index %d\n", i);
	ret++;
      }
    }
  }
  
  sem_mypost(INSTCACHE);
  return(ret);
}

void print_instanceCache(void) {
  int i;

  sem_mywait(INSTCACHE);
  for (i=0; i<MAXINSTANCES; i++) {
    if ( instanceCache->cacheState[i] == INSTVALID ) {
      logprintfl(EUCADEBUG,"\tcache: %d/%d %s %s %s %s\n", i, instanceCache->numInsts, instanceCache->instances[i].instanceId, instanceCache->instances[i].ccnet.publicIp, instanceCache->instances[i].ccnet.privateIp, instanceCache->instances[i].state);
    }
  }
  sem_mypost(INSTCACHE);
}

void print_ccInstance(char *tag, ccInstance *in) {
  char *volbuf, *groupbuf;
  int i;

  volbuf = malloc(sizeof(ncVolume)*EUCA_MAX_VOLUMES*2);
  bzero(volbuf, sizeof(ncVolume)*EUCA_MAX_VOLUMES*2);

  groupbuf = malloc(64*32*2);
  bzero(groupbuf, 64*32*2);
  
  for (i=0; i<64; i++) {
    if (in->groupNames[i][0] != '\0') {
      strncat(groupbuf, in->groupNames[i], 32);
      strncat(groupbuf, " ", 1);
    }
  }
  
  for (i=0; i<EUCA_MAX_VOLUMES; i++) {
    if (in->volumes[i].volumeId[0] != '\0') {
      strncat(volbuf, in->volumes[i].volumeId, CHAR_BUFFER_SIZE);
      strncat(volbuf, ",", 1);
      strncat(volbuf, in->volumes[i].remoteDev, CHAR_BUFFER_SIZE);
      strncat(volbuf, ",", 1);
      strncat(volbuf, in->volumes[i].localDev, CHAR_BUFFER_SIZE);
      strncat(volbuf, ",", 1);
      strncat(volbuf, in->volumes[i].stateName, CHAR_BUFFER_SIZE);
      strncat(volbuf, " ", 1);
    }
  }
  
  logprintfl(EUCADEBUG, "print_ccInstance(): %s instanceId=%s reservationId=%s emiId=%s kernelId=%s ramdiskId=%s emiURL=%s kernelURL=%s ramdiskURL=%s state=%s ts=%d ownerId=%s keyName=%s ccnet={privateIp=%s publicIp=%s privateMac=%s vlan=%d networkIndex=%d} ccvm={cores=%d mem=%d disk=%d} ncHostIdx=%d serviceTag=%s userData=%s launchIndex=%s platform=%s bundleTaskStateName=%s, volumesSize=%d volumes={%s} groupNames={%s}\n", tag, in->instanceId, in->reservationId, in->amiId, in->kernelId, in->ramdiskId, in->amiURL, in->kernelURL, in->ramdiskURL, in->state, in->ts, in->ownerId, in->keyName, in->ccnet.privateIp, in->ccnet.publicIp, in->ccnet.privateMac, in->ccnet.vlan, in->ccnet.networkIndex, in->ccvm.cores, in->ccvm.mem, in->ccvm.disk, in->ncHostIdx, in->serviceTag, in->userData, in->launchIndex, in->platform, in->bundleTaskStateName, in->volumesSize, volbuf, groupbuf);

  free(volbuf);
  free(groupbuf);
}

void invalidate_instanceCache(void) {
  int i;
  
  sem_mywait(INSTCACHE);
  for (i=0; i<MAXINSTANCES; i++) {
    // if instance is in teardown, free up network information
    if ( !strcmp(instanceCache->instances[i].state, "Teardown") ) {
      free_instanceNetwork(instanceCache->instances[i].ccnet.privateMac, instanceCache->instances[i].ccnet.vlan, 0, 0);
    }
    if ( (instanceCache->cacheState[i] == INSTVALID) && ((time(NULL) - instanceCache->lastseen[i]) > config->instanceTimeout)) {
      logprintfl(EUCADEBUG, "invalidate_instanceCache(): invalidating instance '%s' (last seen %d seconds ago)\n", instanceCache->instances[i].instanceId, (time(NULL) - instanceCache->lastseen[i]));
      bzero(&(instanceCache->instances[i]), sizeof(ccInstance));
      instanceCache->lastseen[i] = 0;
      instanceCache->cacheState[i] = INSTINVALID;
      instanceCache->numInsts--;
    }
  }
  sem_mypost(INSTCACHE);
}

int refresh_instanceCache(char *instanceId, ccInstance *in){
  int i, done, rc;
  
  if (!instanceId || !in) {
    return(1);
  }
  
  sem_mywait(INSTCACHE);
  done=0;
  for (i=0; i<MAXINSTANCES && !done; i++) {
    if (!strcmp(instanceCache->instances[i].instanceId, instanceId)) {
      // in cache
      memcpy(&(instanceCache->instances[i]), in, sizeof(ccInstance));
      instanceCache->lastseen[i] = time(NULL);
      sem_mypost(INSTCACHE);
      return(0);
    }
  }
  sem_mypost(INSTCACHE);

  add_instanceCache(instanceId, in);

  return(0);
}

int add_instanceCache(char *instanceId, ccInstance *in){
  int i, done, firstNull=0;

  if (!instanceId || !in) {
    return(1);
  }
  
  sem_mywait(INSTCACHE);
  done=0;
  for (i=0; i<MAXINSTANCES && !done; i++) {
    if ( (instanceCache->cacheState[i] == INSTVALID ) && (!strcmp(instanceCache->instances[i].instanceId, instanceId))) {
      // already in cache
      logprintfl(EUCADEBUG, "add_instanceCache(): '%s/%s/%s' already in cache\n", instanceId, in->ccnet.publicIp, in->ccnet.privateIp);
      instanceCache->lastseen[i] = time(NULL);
      sem_mypost(INSTCACHE);
      return(0);
    } else if ( instanceCache->cacheState[i] == INSTINVALID ) {
      firstNull = i;
      done++;
    }
  }
  logprintfl(EUCADEBUG, "add_instanceCache(): adding '%s/%s/%s/%d' to cache\n", instanceId, in->ccnet.publicIp, in->ccnet.privateIp, in->volumesSize);
  allocate_ccInstance(&(instanceCache->instances[firstNull]), in->instanceId, in->amiId, in->kernelId, in->ramdiskId, in->amiURL, in->kernelURL, in->ramdiskURL, in->ownerId, in->state, in->ts, in->reservationId, &(in->ccnet), &(in->ccvm), in->ncHostIdx, in->keyName, in->serviceTag, in->userData, in->launchIndex, in->platform, in->bundleTaskStateName, in->groupNames, in->volumes, in->volumesSize);
  instanceCache->numInsts++;
  instanceCache->lastseen[firstNull] = time(NULL);
  instanceCache->cacheState[firstNull] = INSTVALID;

  sem_mypost(INSTCACHE);
  return(0);
}

int del_instanceCacheId(char *instanceId) {
  int i;

  sem_mywait(INSTCACHE);
  for (i=0; i<MAXINSTANCES; i++) {
    if ( (instanceCache->cacheState[i] == INSTVALID) && (!strcmp(instanceCache->instances[i].instanceId, instanceId))) {
      // del from cache
      bzero(&(instanceCache->instances[i]), sizeof(ccInstance));
      instanceCache->lastseen[i] = 0;
      instanceCache->cacheState[i] = INSTINVALID;
      instanceCache->numInsts--;
      sem_mypost(INSTCACHE);
      return(0);
    }
  }
  sem_mypost(INSTCACHE);
  return(0);
}

int find_instanceCacheId(char *instanceId, ccInstance **out) {
  int i, done;
  
  if (!instanceId || !out) {
    return(1);
  }
  
  sem_mywait(INSTCACHE);
  *out = NULL;
  done=0;
  for (i=0; i<MAXINSTANCES && !done; i++) {
    if (!strcmp(instanceCache->instances[i].instanceId, instanceId)) {
      // found it
      *out = malloc(sizeof(ccInstance));
      if (!*out) {
	logprintfl(EUCAFATAL, "find_instanceCacheId(): out of memory!\n");
	unlock_exit(1);
      }

      allocate_ccInstance(*out, instanceCache->instances[i].instanceId,instanceCache->instances[i].amiId, instanceCache->instances[i].kernelId, instanceCache->instances[i].ramdiskId, instanceCache->instances[i].amiURL, instanceCache->instances[i].kernelURL, instanceCache->instances[i].ramdiskURL, instanceCache->instances[i].ownerId, instanceCache->instances[i].state,instanceCache->instances[i].ts, instanceCache->instances[i].reservationId, &(instanceCache->instances[i].ccnet), &(instanceCache->instances[i].ccvm), instanceCache->instances[i].ncHostIdx, instanceCache->instances[i].keyName, instanceCache->instances[i].serviceTag, instanceCache->instances[i].userData, instanceCache->instances[i].launchIndex, instanceCache->instances[i].platform, instanceCache->instances[i].bundleTaskStateName, instanceCache->instances[i].groupNames, instanceCache->instances[i].volumes, instanceCache->instances[i].volumesSize);
      logprintfl(EUCADEBUG, "find_instanceCache(): found instance in cache '%s/%s/%s'\n", instanceCache->instances[i].instanceId, instanceCache->instances[i].ccnet.publicIp, instanceCache->instances[i].ccnet.privateIp);
      done++;
    }
  }
  sem_mypost(INSTCACHE);
  if (done) {
    return(0);
  }
  return(1);
}

int find_instanceCacheIP(char *ip, ccInstance **out) {
  int i, done;
  
  if (!ip || !out) {
    return(1);
  }
  
  sem_mywait(INSTCACHE);
  *out = NULL;
  done=0;
  for (i=0; i<MAXINSTANCES && !done; i++) {
    if ((instanceCache->instances[i].ccnet.publicIp[0] != '\0' || instanceCache->instances[i].ccnet.privateIp[0] != '\0')) {
      if (!strcmp(instanceCache->instances[i].ccnet.publicIp, ip) || !strcmp(instanceCache->instances[i].ccnet.privateIp, ip)) {
	// found it
	*out = malloc(sizeof(ccInstance));
	if (!*out) {
	  logprintfl(EUCAFATAL, "find_instanceCacheIP(): out of memory!\n");
	  unlock_exit(1);
	}
	
	allocate_ccInstance(*out, instanceCache->instances[i].instanceId,instanceCache->instances[i].amiId, instanceCache->instances[i].kernelId, instanceCache->instances[i].ramdiskId, instanceCache->instances[i].amiURL, instanceCache->instances[i].kernelURL, instanceCache->instances[i].ramdiskURL, instanceCache->instances[i].ownerId, instanceCache->instances[i].state,instanceCache->instances[i].ts, instanceCache->instances[i].reservationId, &(instanceCache->instances[i].ccnet), &(instanceCache->instances[i].ccvm), instanceCache->instances[i].ncHostIdx, instanceCache->instances[i].keyName, instanceCache->instances[i].serviceTag, instanceCache->instances[i].userData, instanceCache->instances[i].launchIndex, instanceCache->instances[i].platform, instanceCache->instances[i].bundleTaskStateName, instanceCache->instances[i].groupNames, instanceCache->instances[i].volumes, instanceCache->instances[i].volumesSize);
	done++;
      }
    }
  }

  sem_mypost(INSTCACHE);
  if (done) {
    return(0);
  }
  return(1);
}


void print_resourceCache(void) {
  int i;

  sem_mywait(RESCACHE);
  for (i=0; i<MAXNODES; i++) {
    if (resourceCache->cacheState[i] == RESVALID) {
      logprintfl(EUCADEBUG,"\tcache: %s %s %s %s/%s state=%d\n", resourceCache->resources[i].hostname, resourceCache->resources[i].ncURL, resourceCache->resources[i].ncService, resourceCache->resources[i].mac, resourceCache->resources[i].ip, resourceCache->resources[i].state);
    }
  }
  sem_mypost(RESCACHE);
}

void invalidate_resourceCache(void) {
  int i;
  
  sem_mywait(RESCACHE);

  bzero(resourceCache->cacheState, sizeof(int)*MAXNODES);
  resourceCache->numResources = 0;
  resourceCache->resourceCacheUpdate = 0;

  sem_mypost(RESCACHE);
  
}

int refresh_resourceCache(char *host, ccResource *in){
  int i, done, rc;

  if (!host || !in) {
    return(1);
  }
  
  sem_mywait(RESCACHE);
  done=0;
  for (i=0; i<MAXNODES && !done; i++) {
    if (resourceCache->cacheState[i] == RESVALID) {
      if (!strcmp(resourceCache->resources[i].hostname, host)) {
	// in cache
	memcpy(&(resourceCache->resources[i]), in, sizeof(ccResource));
	sem_mypost(RESCACHE);
	return(0);
      }
    }
  }
  sem_mypost(RESCACHE);

  add_resourceCache(host, in);

  return(0);
}

int add_resourceCache(char *host, ccResource *in){
  int i, done, firstNull=0;

  if (!host || !in) {
    return(1);
  }
  
  sem_mywait(RESCACHE);
  done=0;
  for (i=0; i<MAXNODES && !done; i++) {
    if (resourceCache->cacheState[i] == RESVALID) {
      if (!strcmp(resourceCache->resources[i].hostname, host)) {
	// already in cache
	sem_mypost(RESCACHE);
	return(0);
      }
    } else {
      firstNull = i;
      done++;
    }
  }
  resourceCache->cacheState[firstNull] = RESVALID;
  allocate_ccResource(&(resourceCache->resources[firstNull]), in->ncURL, in->ncService, in->ncPort, in->hostname, in->mac, in->ip, in->maxMemory, in->availMemory, in->maxDisk, in->availDisk, in->maxCores, in->availCores, in->state, in->lastState, in->stateChange, in->idleStart);

  resourceCache->numResources++;
  sem_mypost(RESCACHE);
  return(0);
}

int del_resourceCacheId(char *host) {
  int i;

  sem_mywait(RESCACHE);
  for (i=0; i<MAXNODES; i++) {
    if (resourceCache->cacheState[i] == RESVALID) {
      if (!strcmp(resourceCache->resources[i].hostname, host)) {
	// del from cache
	bzero(&(resourceCache->resources[i]), sizeof(ccResource));
	resourceCache->cacheState[i] = RESINVALID;
	resourceCache->numResources--;
	sem_mypost(RESCACHE);
	return(0);
      }
    }
  }
  sem_mypost(RESCACHE);
  return(0);
}

int find_resourceCacheId(char *host, ccResource **out) {
  int i, done;
  
  if (!host || !out) {
    return(1);
  }
  
  sem_mywait(RESCACHE);
  *out = NULL;
  done=0;
  for (i=0; i<MAXNODES && !done; i++) {
    if (resourceCache->cacheState[i] == RESVALID) {
      if (!strcmp(resourceCache->resources[i].hostname, host)) {
	// found it
	*out = malloc(sizeof(ccResource));
	if (!*out) {
	  logprintfl(EUCAFATAL, "find_resourceCacheId(): out of memory!\n");
	  unlock_exit(1);
	}
	allocate_ccResource(*out, resourceCache->resources[i].ncURL, resourceCache->resources[i].ncService, resourceCache->resources[i].ncPort, resourceCache->resources[i].hostname, resourceCache->resources[i].mac, resourceCache->resources[i].ip, resourceCache->resources[i].maxMemory, resourceCache->resources[i].availMemory, resourceCache->resources[i].maxDisk, resourceCache->resources[i].availDisk, resourceCache->resources[i].maxCores, resourceCache->resources[i].availCores, resourceCache->resources[i].state, resourceCache->resources[i].lastState, resourceCache->resources[i].stateChange, resourceCache->resources[i].idleStart);
	done++;
      }
    }
  }

  sem_mypost(RESCACHE);
  if (done) {
    return(0);
  }
  return(1);
}

void unlock_exit(int code) {
  int i;
  
  logprintfl(EUCADEBUG, "unlock_exit(): params: code=%d\n", code);
  
  for (i=0; i<ENDLOCK; i++) {
    if (mylocks[i]) {
      logprintfl(EUCAWARN, "unlock_exit(): unlocking index '%d'\n", i);
      sem_post(locks[i]);
    }
  }
  exit(code);
}

int sem_mywait(int lockno) {
  int rc;
  rc = sem_wait(locks[lockno]);
  mylocks[lockno] = 1;
  return(rc);
}
int sem_mypost(int lockno) {
  mylocks[lockno] = 0;
  return(sem_post(locks[lockno]));
}

int image_cache(char *id, char *url) {
  int rc;
  int pid;
  char path[MAX_PATH], finalpath[MAX_PATH];

  if (url && id) {
    pid = fork();
    if (!pid) {
      snprintf(finalpath, MAX_PATH, "%s/data/%s.manifest.xml", config->proxyPath, id);
      snprintf(path, MAX_PATH, "%s/data/%s.manifest.xml.staging", config->proxyPath, id);
      if (check_file(path) && check_file(finalpath)) {
	rc = walrus_object_by_url(url, path, 0);
	if (rc) {
	  logprintfl(EUCAERROR, "image_cache(): could not cache image manifest (%s/%s)\n", id, url);
	  unlink(path);
	  exit(1);
	}
	rename(path, finalpath);
      }
      snprintf(path, MAX_PATH, "%s/data/%s.staging", config->proxyPath, id);
      snprintf(finalpath, MAX_PATH, "%s/data/%s", config->proxyPath, id);
      if (check_file(path) && check_file(finalpath)) {
	rc = walrus_image_by_manifest_url(url, path, 1);
	if (rc) {
	  logprintfl(EUCAERROR, "image_cache(): could not cache image (%s/%s)\n", id, url);
	  unlink(path);
	  exit(1);
	}
	rename(path, finalpath);
      }
      exit(0);
    }
  }
  //  int walrus_image_by_manifest_url (const char * url, const char * outfile, const int do_compress);
  return(0);
}

int image_cache_invalidate() {
  time_t oldest;
  char proxyPath[MAX_PATH], path[MAX_PATH], oldestpath[MAX_PATH], oldestmanifestpath[MAX_PATH];
  DIR *DH=NULL;
  struct dirent dent, *result=NULL;
  struct stat mystat;
  int rc, total_megs=0;
  //  snprintf(path, MAX_PATH, "%s/%s.manifest.xml.staging", config->proxyPath, id);  

  if (config->use_proxy) {

    oldest = time(NULL);
    snprintf(proxyPath, MAX_PATH, "%s/data", config->proxyPath);
    DH=opendir(proxyPath);
    if (!DH) {
      logprintfl(EUCAERROR, "image_cache_invalidate(): could not open dir '%s'\n", proxyPath);
      return(1);
    }
    
    rc = readdir_r(DH, &dent, &result);
    while(!rc && result) {
      if (strcmp(dent.d_name, ".") && strcmp(dent.d_name, "..") && !strstr(dent.d_name, "manifest.xml")) {
	snprintf(path, MAX_PATH, "%s/%s", proxyPath, dent.d_name);
	rc = stat(path, &mystat);
	if (!rc) {
	  logprintfl(EUCADEBUG, "image_cache_invalidate(): evaluating file: name=%s size=%d atime=%d'\n", dent.d_name, mystat.st_size/1048576, mystat.st_atime);
	  if (mystat.st_atime < oldest) {
	    oldest = mystat.st_atime;
	    snprintf(oldestpath, MAX_PATH, "%s", path);
	    snprintf(oldestmanifestpath, MAX_PATH, "%s.manifest.xml", path);
	  }
	  total_megs += mystat.st_size/1048576;
	}
      }
      rc = readdir_r(DH, &dent, &result);
    }
    closedir(DH);
    logprintfl(EUCADEBUG, "image_cache_invalidate(): summary: totalMBs=%d oldestAtime=%d oldestFile=%s\n", total_megs, oldest, oldestpath);
    if (total_megs > config->proxy_max_cache_size) {
      // start slowly deleting
      logprintfl(EUCAINFO, "image_cache_invalidate(): invalidating cached image: name=%s\n", oldestpath);
      unlink(oldestpath);
      unlink(oldestmanifestpath);
    }
  }
  
  return(0);
}


int image_cache_proxykick(ccResource *res, int *numHosts) {
  char cmd[MAX_PATH];
  char *nodestr=NULL;
  int i, rc;

  nodestr = malloc(( (*numHosts) * 128) + (*numHosts) + 1);
  
  bzero(nodestr, ( (*numHosts) * 128 ) + (*numHosts) + 1);
  for (i=0; i<(*numHosts); i++) {
    strcat(nodestr, res[i].hostname);
    strcat(nodestr, " ");
  }
  
  snprintf(cmd, MAX_PATH, "%s/usr/share/eucalyptus/dynserv.pl %s %s", config->eucahome, config->proxyPath, nodestr);
  logprintfl(EUCADEBUG, "image_cache_proxykick(): running cmd '%s'\n", cmd);
  rc = system(cmd);
  
  if (nodestr) free(nodestr);
  return(rc);
}

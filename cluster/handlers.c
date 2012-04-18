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
#include <math.h>

#include "axis2_skel_EucalyptusCC.h"

#include <server-marshal.h>
#include <handlers.h>
#include <vnetwork.h>
#include <misc.h>
#include <ipc.h>
#include <walrus.h>
#include <http.h>

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

sem_t *locks[ENDLOCK];
int mylocks[ENDLOCK];

void doInitCC(void) {
  initialize(NULL);
}

int doBundleInstance(ncMetadata *ccMeta, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL, char *userPublicKey, char *S3Policy, char *S3PolicySig) {
  int i, j, rc, start = 0, stop = 0, ret=0, timeout, done;
  char internalWalrusURL[MAX_PATH], theWalrusURL[MAX_PATH];
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
  logprintfl(EUCAINFO, "BundleInstance(): called \n");
  logprintfl(EUCADEBUG, "BundleInstance(): params: userId=%s, instanceId=%s, bucketName=%s, filePrefix=%s, walrusURL=%s, userPublicKey=%s, S3Policy=%s, S3PolicySig=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(instanceId), SP(bucketName), SP(filePrefix), SP(walrusURL), SP(userPublicKey), SP(S3Policy), SP(S3PolicySig));
  if (!instanceId) {
    logprintfl(EUCAERROR, "BundleInstance(): bad input params\n");
    return(1);
  }

  // get internal walrus IP
  done=0;
  internalWalrusURL[0] = '\0';
  for (i=0; i<16 && !done; i++) {
    if (!strcmp(config->services[i].type, "walrus")) {
      snprintf(internalWalrusURL, MAX_PATH, "%s", config->services[i].uris[0]);
      done++;
    }
  }
  if (done) {
    snprintf(theWalrusURL, MAX_PATH, "%s", internalWalrusURL);
  } else {
    strncpy(theWalrusURL, walrusURL, strlen(walrusURL)+1);
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
    rc = ncClientCall(ccMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncBundleInstance", instanceId, bucketName, filePrefix, theWalrusURL, userPublicKey, S3Policy, S3PolicySig);
    if (rc) {
      ret = 1;
    } else {
      ret = 0;
      done++;
    }
  }

  logprintfl(EUCADEBUG,"BundleInstance(): done. \n");
  
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
  logprintfl(EUCAINFO, "CancelBundleTask(): called \n");
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

  logprintfl(EUCADEBUG,"CancelBundleTask(): done. \n");
  
  shawn();
  
  return(ret);
}

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
    ncMetadata *localmeta=NULL;
    
    localmeta = malloc(sizeof(ncMetadata));
    if (!localmeta) {
      logprintfl(EUCAFATAL, "ncClientCall(%s): out of memory!\n", ncOp);
      unlock_exit(1);
    }
    memcpy(localmeta, meta, sizeof(ncMetadata));
    if (meta->correlationId) {
      localmeta->correlationId = strdup(meta->correlationId);
    } else {
      localmeta->correlationId = strdup("unset");
    }
    if (meta->userId) {
      localmeta->userId = strdup(meta->userId);
    } else {
      localmeta->userId = strdup("eucalyptus");
    }
    
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

      rc = ncGetConsoleOutputStub(ncs, localmeta, instId, consoleOutput);
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

      rc = ncAttachVolumeStub(ncs, localmeta, instanceId, volumeId, remoteDev, localDev);
    } else if (!strcmp(ncOp, "ncDetachVolume")) {
      char *instanceId = va_arg(al, char *);
      char *volumeId = va_arg(al, char *);      
      char *remoteDev = va_arg(al, char *);      
      char *localDev = va_arg(al, char *);      
      int force = va_arg(al, int);

      rc = ncDetachVolumeStub(ncs, localmeta, instanceId, volumeId, remoteDev, localDev, force);
    } else if (!strcmp(ncOp, "ncCreateImage")) {
      char *instanceId = va_arg(al, char *);
      char *volumeId = va_arg(al, char *);      
      char *remoteDev = va_arg(al, char *);      

      rc = ncCreateImageStub(ncs, localmeta, instanceId, volumeId, remoteDev);
    } else if (!strcmp(ncOp, "ncPowerDown")) {
      rc = ncPowerDownStub(ncs, localmeta);
    } else if (!strcmp(ncOp, "ncAssignAddress")) {
      char *instanceId = va_arg(al, char *);
      char *publicIp = va_arg(al, char *);

      rc = ncAssignAddressStub(ncs, localmeta, instanceId, publicIp);
    } else if (!strcmp(ncOp, "ncRebootInstance")) {
      char *instId = va_arg(al, char *);

      rc = ncRebootInstanceStub(ncs, localmeta, instId);
    } else if (!strcmp(ncOp, "ncTerminateInstance")) {
      char *instId = va_arg(al, char *);
      int force = va_arg(al, int);
      int *shutdownState = va_arg(al, int *);
      int *previousState = va_arg(al, int *);

      rc = ncTerminateInstanceStub(ncs, localmeta, instId, force, shutdownState, previousState);

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
      
      rc = ncStartNetworkStub(ncs, localmeta, uuid, peers, peersLen, port, vlan, outStatus);
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
      char *ownerId = va_arg(al, char *);
      char *accountId = va_arg(al, char *);
      char *keyName = va_arg(al, char *);
      netConfig *ncnet = va_arg(al, netConfig *);
      char *userData = va_arg(al, char *);
      char *launchIndex = va_arg(al, char *);
      char *platform = va_arg(al, char *);
      int expiryTime = va_arg(al, int);
      char **netNames = va_arg(al, char **);
      int netNamesLen = va_arg(al, int);
      ncInstance **outInst = va_arg(al, ncInstance **);
      
      rc = ncRunInstanceStub(ncs, localmeta, uuid, instId, reservationId, ncvm, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, ownerId, accountId, keyName, ncnet, userData, launchIndex, platform, expiryTime, netNames, netNamesLen, outInst);
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

      rc = ncDescribeInstancesStub(ncs, localmeta, instIds, instIdsLen, ncOutInsts, ncOutInstsLen);
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

      rc = ncDescribeResourceStub(ncs, localmeta, resourceType, outRes);
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

      rc = ncBundleInstanceStub(ncs, localmeta, instanceId, bucketName, filePrefix, walrusURL, userPublicKey, S3Policy, S3PolicySig);
    } else if (!strcmp(ncOp, "ncCancelBundleTask")) {
      char *instanceId = va_arg(al, char *);

      rc = ncCancelBundleTaskStub(ncs, localmeta, instanceId);
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
    if (localmeta) free(localmeta);
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
      int force = va_arg(al, int);
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
      char *ownerId = va_arg(al, char *);
      char *accountId = va_arg(al, char *);
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
	  if (*outRes == NULL) {
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
  
  logprintfl(EUCAINFO, "AttachVolume(): called \n");
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
  
  logprintfl(EUCADEBUG,"AttachVolume(): done. \n");
  
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
  logprintfl(EUCAINFO, "DetachVolume(): called \n");
  logprintfl(EUCADEBUG, "DetachVolume(): params: userId=%s, volumeId=%s, instanceId=%s, remoteDev=%s, localDev=%s, force=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(volumeId), SP(instanceId), SP(remoteDev), SP(localDev), force);
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
    rc = ncClientCall(ccMeta, timeout, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncDetachVolume", instanceId, volumeId, remoteDev, localDev, force);
    if (rc) {
      ret = 1;
    } else {
      ret = 0;
      done++;
    }
  }

  logprintfl(EUCADEBUG,"DetachVolume(): done. \n");
  
  shawn();
  
  return(ret);
}

int doConfigureNetwork(ncMetadata *ccMeta, char *accountId, char *type, int namedLen, char **sourceNames, char **userNames, int netLen, char **sourceNets, char *destName, char *destUserName, char *protocol, int minPort, int maxPort) {
  int rc, i, fail;

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "ConfigureNetwork(): called \n");
  logprintfl(EUCADEBUG, "ConfigureNetwork(): params: userId=%s, accountId=%s, type=%s, namedLen=%d, netLen=%d, destName=%s, destUserName=%s, protocol=%s, minPort=%d, maxPort=%d\n", ccMeta ? SP(ccMeta->userId) : "UNSET", SP(accountId), SP(type), namedLen, netLen, SP(destName), SP(destUserName), SP(protocol), minPort, maxPort);
  
  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC")) {
    fail = 0;
  } else {
    
    if ( destUserName == NULL ) {
      if ( accountId ) {
	destUserName = accountId;
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
  
  logprintfl(EUCADEBUG,"ConfigureNetwork(): done. \n");
  
  shawn();
  
  if (fail) {
    return(1);
  }
  return(0);
}

int doFlushNetwork(ncMetadata *ccMeta, char *accountId, char *destName) {
  int rc;

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
    return(0);
  }

  sem_mywait(VNET);
  rc = vnetFlushTable(vnetconfig, accountId, destName);
  sem_mypost(VNET);
  return(rc);
}

int doAssignAddress(ncMetadata *ccMeta, char *uuid, char *src, char *dst) {
  int rc, allocated, addrdevno, ret, i;
  char cmd[MAX_PATH];
  ccInstance *myInstance=NULL;
  ccResourceCache resourceCacheLocal;

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO,"doAssignAddress(): called \n");
  logprintfl(EUCADEBUG,"doAssignAddress(): params: src=%s, dst=%s\n", SP(src), SP(dst));

  if (!src || !dst || !strcmp(src, "0.0.0.0")) {
    logprintfl(EUCADEBUG, "doAssignAddress(): bad input params\n");
    return(1);
  }
  set_dirty_instanceCache();

  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);

  ret = 1;  
  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
    ret = 0;
  } else {

    rc = find_instanceCacheIP(dst, &myInstance);
    if (!rc) {
      if (myInstance) {
	logprintfl(EUCADEBUG, "doAssignAddress(): found local instance, applying %s->%s mapping\n", src, dst);

	sem_mywait(VNET);	
	rc = vnetReassignAddress(vnetconfig, uuid, src, dst);
	if (rc) {
	  logprintfl(EUCAERROR, "doAssignAddress(): vnetReassignAddress() failed\n");
	  ret = 1;
	} else {
	  ret = 0;
	}
	sem_mypost(VNET);

	if (myInstance) free(myInstance);
      }
    } else {
      logprintfl(EUCADEBUG, "doAssignAddress(): skipping %s->%s mapping, as this clusters does not own the instance (%s)\n", src, dst, dst); 
    }
  }
  
  if (!ret && strcmp(dst, "0.0.0.0")) {
    // everything worked, update instance cache

    rc = map_instanceCache(privIpCmp, dst, pubIpSet, src);
    if (rc) {
      logprintfl(EUCAERROR, "doAssignAddress(): map_instanceCache() failed to assign %s->%s\n", dst, src);
    } else {
      rc = find_instanceCacheIP(src, &myInstance);
      if (!rc) {
	logprintfl(EUCADEBUG, "doAssignAddress(): found instance (%s) in cache with IP (%s)\n", myInstance->instanceId, myInstance->ccnet.publicIp);
	// found the instance in the cache
	if (myInstance) {
	  //timeout = ncGetTimeout(op_start, OP_TIMEOUT, 1, myInstance->ncHostIdx);
	  rc = ncClientCall(ccMeta, OP_TIMEOUT, resourceCacheLocal.resources[myInstance->ncHostIdx].lockidx, resourceCacheLocal.resources[myInstance->ncHostIdx].ncURL, "ncAssignAddress", myInstance->instanceId, myInstance->ccnet.publicIp);
	  if (rc) {
	    logprintfl(EUCAERROR, "doAssignAddress(): could not sync public IP %s with NC\n", src);
	    ret = 1;
	  } else {
	    ret = 0;
	  }
	  if (myInstance) free(myInstance);
	}
      }
    }
  }
  
  logprintfl(EUCADEBUG,"doAssignAddress(): done. \n");
  
  shawn();

  return(ret);
}

int doDescribePublicAddresses(ncMetadata *ccMeta, publicip **outAddresses, int *outAddressesLen) {
  int rc, ret;
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "DescribePublicAddresses(): called \n");
  logprintfl(EUCADEBUG, "DescribePublicAddresses(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  ret=0;
  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    sem_mywait(VNET);
    *outAddresses = vnetconfig->publicips;
    *outAddressesLen = NUMBER_OF_PUBLIC_IPS;
    sem_mypost(VNET);
  } else {
    *outAddresses = NULL;
    *outAddressesLen = 0;
    ret=0;
  }
  
  logprintfl(EUCADEBUG, "DescribePublicAddresses(): done. \n");

  shawn();

  return(ret);
}

int doUnassignAddress(ncMetadata *ccMeta, char *src, char *dst) {
  int rc, allocated, addrdevno, ret;
  char cmd[MAX_PATH];
  ccInstance *myInstance=NULL;
  ccResourceCache resourceCacheLocal;

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  logprintfl(EUCAINFO,"UnassignAddress(): called \n");

  logprintfl(EUCADEBUG,"UnassignAddress(): params: userId=%s, src=%s, dst=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(src), SP(dst));  
  
  if (!src || !dst || !strcmp(src, "0.0.0.0")) {
    logprintfl(EUCADEBUG, "UnassignAddress(): bad input params\n");
    return(1);
  }
  set_dirty_instanceCache();

  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
  
  ret=0;

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
    ret = 0;
  } else {
    
    sem_mywait(VNET);

    ret = vnetReassignAddress(vnetconfig, "UNSET", src, "0.0.0.0");
    if (ret) {
      logprintfl(EUCAERROR, "UnassignAddress(): vnetReassignAddress() failed\n");
      ret = 1;
    }

    sem_mypost(VNET);
  }

  if (!ret) {

    rc = find_instanceCacheIP(src, &myInstance);
    if (!rc) {
      logprintfl(EUCADEBUG, "UnassignAddress(): found instance %s in cache with IP %s\n", myInstance->instanceId, myInstance->ccnet.publicIp);
      // found the instance in the cache
      if (myInstance) {
	//timeout = ncGetTimeout(op_start, OP_TIMEOUT, 1, myInstance->ncHostIdx);
	rc = ncClientCall(ccMeta, OP_TIMEOUT, resourceCacheLocal.resources[myInstance->ncHostIdx].lockidx, resourceCacheLocal.resources[myInstance->ncHostIdx].ncURL, "ncAssignAddress", myInstance->instanceId, "0.0.0.0");
	if (rc) {
	  logprintfl(EUCAERROR, "UnassignAddress(): could not sync IP with NC\n");
	  ret = 1;
	} else {
	  ret = 0;
	}
	if (myInstance) free(myInstance);
      }
    }

    // refresh instance cache
    rc = map_instanceCache(pubIpCmp, src, pubIpSet, "0.0.0.0");
    if (rc) {
      logprintfl(EUCAERROR, "UnassignAddress(): map_instanceCache() failed to assign %s->%s\n", dst, src);
    }
  }
  
  logprintfl(EUCADEBUG,"UnassignAddress(): done. \n");
  
  shawn();

  return(ret);
}

int doStopNetwork(ncMetadata *ccMeta, char *accountId, char *netName, int vlan) {
  int rc, ret;
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "StopNetwork(): called \n");
  logprintfl(EUCADEBUG, "StopNetwork(): params: userId=%s, accountId=%s, netName=%s, vlan=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(accountId), SP(netName), vlan);
  if (!ccMeta || !netName || vlan < 0) {
    logprintfl(EUCAERROR, "StopNetwork(): bad input params\n");
  }

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
    ret = 0;
  } else {
    
    sem_mywait(VNET);
    if(ccMeta != NULL) {
      rc = vnetStopNetwork(vnetconfig, vlan, accountId, netName);
    }
    ret = rc;
    sem_mypost(VNET);
  }
  
  logprintfl(EUCADEBUG,"StopNetwork(): done. \n");

  shawn();
  
  return(ret);
}

int doDescribeNetworks(ncMetadata *ccMeta, char *nameserver, char **ccs, int ccsLen, vnetConfig *outvnetConfig) {
  int rc, i, j;
  
  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }

  logprintfl(EUCAINFO, "DescribeNetworks(): called \n");
  logprintfl(EUCADEBUG, "DescribeNetworks(): params: userId=%s, nameserver=%s, ccsLen=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(nameserver), ccsLen);
  
  // ensure that we have the latest network state from the CC (based on instance cache) before responding to CLC
  rc = checkActiveNetworks();
  if (rc) {
    logprintfl(EUCAWARN, "DescribeNetowrks(): checkActiveNetworks() failed, will attempt to re-sync\n");
  }
  
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
  logprintfl(EUCADEBUG, "DescribeNetworks(): done. \n");
  
  shawn();

  return(0);
}

int doStartNetwork(ncMetadata *ccMeta, char *accountId, char *uuid, char *netName, int vlan, char *nameserver, char **ccs, int ccsLen) {
  int rc, ret;
  time_t op_start;
  char *brname;
  
  op_start = time(NULL);

  rc = initialize(ccMeta);
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "StartNetwork(): called \n");
  logprintfl(EUCADEBUG, "StartNetwork(): params: userId=%s, accountId=%s, netName=%s, vlan=%d, nameserver=%s, ccsLen=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(accountId), SP(netName), vlan, SP(nameserver), ccsLen);

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
    rc = vnetStartNetwork(vnetconfig, vlan, uuid, accountId, netName, &brname);
    if (brname) free(brname);

    sem_mypost(VNET);
    
    if (rc) {
      logprintfl(EUCAERROR,"StartNetwork(): vnetStartNetwork() failed (%d)\n", rc);
      ret = 1;
    } else {
      ret = 0;
    }
    
  }
  
  logprintfl(EUCADEBUG,"StartNetwork(): done \n");
  
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

  logprintfl(EUCAINFO,"DescribeResources(): called \n");
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

    if (*outNodes == NULL) {
      logprintfl(EUCAFATAL,"DescribeResources(): out of memory!\n");
      unlock_exit(1);
    } else {
       bzero(*outNodes, sizeof(ccResource) * resourceCacheLocal.numResources);
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

  logprintfl(EUCADEBUG,"DescribeResources(): done \n");
  
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

  // critical NC call section
  sem_mywait(RESCACHE);
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
	  safe_strncpy(resourceCacheStage->resources[i].mac, mac, 24);
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
  netConfig origNetConfig;


  op_start = time(NULL);

  logprintfl(EUCAINFO,"refresh_instances(): called\n");
  
  set_clean_instanceCache();

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
	      safe_strncpy(myInstance->serviceTag, resourceCacheStage->resources[i].ncURL, 64);
	      {
		char *ip=NULL;
		if (!strcmp(myInstance->ccnet.publicIp, "0.0.0.0")) {
		  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC") || !strcmp(vnetconfig->mode, "STATIC-DYNMAC") ) {
		    rc = mac2ip(vnetconfig, myInstance->ccnet.privateMac, &ip);
		    if (!rc) {
		      safe_strncpy(myInstance->ccnet.publicIp, ip, 24);
		    }
		  }
		}
		
		if (ip) free(ip);
		ip=NULL;
		
		if (!strcmp(myInstance->ccnet.privateIp, "0.0.0.0")) {
		  rc = mac2ip(vnetconfig, myInstance->ccnet.privateMac, &ip);
		  if (!rc) {
		    safe_strncpy(myInstance->ccnet.privateIp, ip, 24);
		  }
		}
		
		if (ip) free(ip);
	      }

	      //#if 0
	      if ((myInstance->ccnet.publicIp[0] != '\0' && strcmp(myInstance->ccnet.publicIp, "0.0.0.0")) && (myInstance->ncnet.publicIp[0] == '\0' || !strcmp(myInstance->ncnet.publicIp, "0.0.0.0"))) {
		// CC has network info, NC does not
		logprintfl(EUCADEBUG, "refresh_instances(): sending ncAssignAddress to sync NC\n");
		rc = ncClientCall(ccMeta, nctimeout, resourceCacheStage->resources[i].lockidx, resourceCacheStage->resources[i].ncURL, "ncAssignAddress", myInstance->instanceId, myInstance->ccnet.publicIp);
		if (rc) {
		  // problem, but will retry next time
		  logprintfl(EUCAWARN, "refresh_instances(): could not send AssignAddress to NC\n");
		}
	      }
	      //#endif
	      
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

  logprintfl(EUCAINFO,"DescribeInstances(): called \n");
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

  logprintfl(EUCADEBUG,"DescribeInstances(): done \n");

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

void print_netConfig(char *prestr, netConfig *in) {
  logprintfl(EUCADEBUG, "print_netConfig(): %s: vlan:%d networkIndex:%d privateMac:%s publicIp:%s privateIp:%s\n", prestr, in->vlan, in->networkIndex, in->privateMac, in->publicIp, in->privateIp);
}

int ccInstance_to_ncInstance(ccInstance *dst, ncInstance *src) {
  int i;
  
  safe_strncpy(dst->uuid, src->uuid, 48);
  safe_strncpy(dst->instanceId, src->instanceId, 16);
  safe_strncpy(dst->reservationId, src->reservationId, 16);
  safe_strncpy(dst->accountId, src->accountId, 48);
  safe_strncpy(dst->ownerId, src->ownerId, 48);
  safe_strncpy(dst->amiId, src->imageId, 16);
  safe_strncpy(dst->kernelId, src->kernelId, 16);
  safe_strncpy(dst->ramdiskId, src->ramdiskId, 16);
  safe_strncpy(dst->keyName, src->keyName, 1024);
  safe_strncpy(dst->launchIndex, src->launchIndex, 64);
  safe_strncpy(dst->platform, src->platform, 64);
  safe_strncpy(dst->bundleTaskStateName, src->bundleTaskStateName, 64);
  safe_strncpy(dst->createImageTaskStateName, src->createImageTaskStateName, 64);
  safe_strncpy(dst->userData, src->userData, 16384);
  safe_strncpy(dst->state, src->stateName, 16);
  dst->ts = src->launchTime;

  memcpy(&(dst->ncnet), &(src->ncnet), sizeof(netConfig));

  for (i=0; i < src->groupNamesSize && i < 64; i++) {
    snprintf(dst->groupNames[i], 64, "%s", src->groupNames[i]);
  }

  memcpy(dst->volumes, src->volumes, sizeof(ncVolume) * EUCA_MAX_VOLUMES);
  dst->volumesSize = 0;
  for (i=0; i < EUCA_MAX_VOLUMES; i++) {
          if (strlen (dst->volumes[i].volumeId) == 0)
                  break;
          dst->volumesSize++;
  }

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


int doRunInstances(ncMetadata *ccMeta, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char **instIds, int instIdsLen, char **netNames, int netNamesLen, char **macAddrs, int macAddrsLen, int *networkIndexList, int networkIndexListLen, char **uuids, int uuidsLen, int minCount, int maxCount, char *accountId, char *ownerId, char *reservationId, virtualMachine *ccvm, char *keyName, int vlan, char *userData, char *launchIndex, char *platform, int expiryTime, char *targetNode, ccInstance **outInsts, int *outInstsLen) {
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
  logprintfl(EUCAINFO,"RunInstances(): called \n");
  logprintfl(EUCADEBUG,"RunInstances(): params: userId=%s, emiId=%s, kernelId=%s, ramdiskId=%s, emiURL=%s, kernelURL=%s, ramdiskURL=%s, instIdsLen=%d, netNamesLen=%d, macAddrsLen=%d, networkIndexListLen=%d, minCount=%d, maxCount=%d, accountId=%s, ownerId=%s, reservationId=%s, keyName=%s, vlan=%d, userData=%s, launchIndex=%s, platform=%s, targetNode=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"), SP(amiId), SP(kernelId), SP(ramdiskId), SP(amiURL), SP(kernelURL), SP(ramdiskURL), instIdsLen, netNamesLen, macAddrsLen, networkIndexListLen, minCount, maxCount, SP(accountId), SP(ownerId), SP(reservationId), SP(keyName), vlan, SP(userData), SP(launchIndex), SP(platform), SP(targetNode));
  
  if (config->use_proxy) {
    char walrusURL[MAX_PATH], *strptr=NULL, newURL[MAX_PATH];
    
    // get walrus IP
    done=0;
    for (i=0; i<16 && !done; i++) {
      if (!strcmp(config->services[i].type, "walrus")) {
	snprintf(walrusURL, MAX_PATH, "%s", config->services[i].uris[0]);
	done++;
      }
    }
    
    if (done) {
      // cache and reset endpoint
      for (i=0; i<ccvm->virtualBootRecordLen; i++) {
	newURL[0] = '\0';
	if (!strcmp(ccvm->virtualBootRecord[i].typeName, "machine") || !strcmp(ccvm->virtualBootRecord[i].typeName, "kernel") || !strcmp(ccvm->virtualBootRecord[i].typeName, "ramdisk")) {
	  strptr = strstr(ccvm->virtualBootRecord[i].resourceLocation, "walrus://");
	  if (strptr) {
	    strptr += strlen("walrus://");
	    snprintf(newURL, MAX_PATH, "%s/%s", walrusURL, strptr);
	    logprintfl(EUCADEBUG, "RunInstances(): constructed cacheable URL: %s\n", newURL);
	    rc = image_cache(ccvm->virtualBootRecord[i].id, newURL);
	    if (!rc) {
	      snprintf(ccvm->virtualBootRecord[i].resourceLocation, CHAR_BUFFER_SIZE, "http://%s:8776/%s", config->proxyIp, ccvm->virtualBootRecord[i].id);
	    } else {
	      logprintfl(EUCAWARN, "RunInstances(): could not cache image %s/%s\n", ccvm->virtualBootRecord[i].id, newURL);
	    }
	  }
	}
      }
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

    logprintfl(EUCADEBUG,"RunInstances(): running instance %s\n", instId);
    
    // generate new mac
    bzero(mac, 32);
    bzero(pubip, 32);
    bzero(privip, 32);
    
    strncpy(pubip, "0.0.0.0", 32);
    strncpy(privip, "0.0.0.0", 32);

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
	  time_t startRun, ncRunTimeout;

	  sem_mywait(RESCACHE);
	  if (res->running > 0) {
	    res->running++;
	  }
	  sem_mypost(RESCACHE);

	  ret=0;
	  logprintfl(EUCAINFO,"RunInstances(): sending run instance: node=%s instanceId=%s emiId=%s mac=%s privIp=%s pubIp=%s vlan=%d networkIdx=%d key=%.32s... mem=%d disk=%d cores=%d\n", res->ncURL, instId, SP(amiId), ncnet.privateMac, ncnet.privateIp, ncnet.publicIp, ncnet.vlan, ncnet.networkIndex, SP(keyName), ncvm.mem, ncvm.disk, ncvm.cores);

	  rc = 1;
	  startRun = time(NULL);
          if (config->schedPolicy == SCHEDPOWERSAVE) {
            ncRunTimeout = config->wakeThresh;
          } else {
            ncRunTimeout = 15;
          }

          while(rc && ((time(NULL) - startRun) < ncRunTimeout)) {
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
	    logprintfl(EUCADEBUG, "RunInstances(): sent network start request for network idx '%d' on resource '%s' uuid '%s': result '%s'\n", vlan, res->ncURL, uuid, rc ? "FAIL" : "SUCCESS");
	    rc = ncClientCall(ccMeta, OP_TIMEOUT_PERNODE, res->lockidx, res->ncURL, "ncRunInstance", uuid, instId, reservationId, &ncvm, amiId, amiURL, kernelId, kernelURL, ramdiskId, ramdiskURL, ownerId, accountId, keyName, &ncnet, userData, launchIndex, platform, expiryTime, netNames, netNamesLen, &outInst);
	    logprintfl(EUCADEBUG, "RunInstances(): sent run request for instance '%s' on resource '%s': result '%s' uuis '%s'\n", instId, res->ncURL, uuid, rc ? "FAIL" : "SUCCESS");
	    if (rc) {
	      // make sure we get the latest topology information before trying again
	      sem_mywait(CONFIG);
	      memcpy(ccMeta->services, config->services, sizeof(serviceInfoType) * 16);
	      memcpy(ccMeta->disabledServices, config->disabledServices, sizeof(serviceInfoType) * 16);
	      memcpy(ccMeta->notreadyServices, config->notreadyServices, sizeof(serviceInfoType) * 16);
	      sem_mypost(CONFIG);
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
	  
	  allocate_ccInstance(myInstance, instId, amiId, kernelId, ramdiskId, amiURL, kernelURL, ramdiskURL, ownerId, accountId, "Pending", "", time(NULL), reservationId, &ncnet, &ncnet, ccvm, resid, keyName, resourceCache->resources[resid].ncURL, userData, launchIndex, platform, myInstance->bundleTaskStateName, myInstance->groupNames, myInstance->volumes, myInstance->volumesSize);

	  // start up DHCP
	  sem_mywait(CONFIG);
	  config->kick_dhcp = 1;
	  sem_mypost(CONFIG);

	  // add the instance to the cache, and continue on
	  //	  add_instanceCache(myInstance->instanceId, myInstance);
	  refresh_instanceCache(myInstance->instanceId, myInstance);
	  print_ccInstance("RunInstances(): ", myInstance);
	  
	  runCount++;
	}
      }

      sem_mypost(RESCACHE);

    }
    
  }
  *outInstsLen = runCount;
  *outInsts = retInsts;
  
  logprintfl(EUCADEBUG,"RunInstances(): done. \n");
  
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

  logprintfl(EUCAINFO,"GetConsoleOutput(): called \n");
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
    if (*outConsoleOutput) {
      free(*outConsoleOutput);
      *outConsoleOutput = NULL;
    }

    // if not talking to Eucalyptus NC (but, e.g., a Broker)
    if (!strstr(resourceCacheLocal.resources[j].ncURL, "EucalyptusNC")) {
            char pwfile[MAX_PATH];
            *outConsoleOutput = NULL;
            snprintf(pwfile, MAX_PATH, "%s/var/lib/eucalyptus/windows/%s/console.append.log", config->eucahome, instId);

            char *rawconsole=NULL;
            if (!check_file(pwfile)) { // the console log file should exist for a Windows guest (with encrypted password in it)
                    rawconsole = file2str(pwfile);
            } else { // the console log file will not exist for a Linux guest
                    rawconsole = strdup ("not implemented");
            }
            if (rawconsole) {
                    *outConsoleOutput = base64_enc((unsigned char *)rawconsole, strlen(rawconsole));
                    free (rawconsole);
            }
            // set the return code accordingly
            if (!*outConsoleOutput) {
                    rc = 1;
            } else {
                    rc = 0;
            }
            done++; // quit on the first host, since they are not queried remotely 

    } else { // otherwise, we *are* talking to a Eucalyptus NC, so make the remote call
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

  logprintfl(EUCADEBUG,"GetConsoleOutput(): done. \n");
  
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
  logprintfl(EUCAINFO,"RebootInstances(): called \n");
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
  
  logprintfl(EUCADEBUG,"RebootInstances(): done. \n");

  shawn();

  return(0);
}

int doTerminateInstances(ncMetadata *ccMeta, char **instIds, int instIdsLen, int force, int **outStatus) {
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
  set_dirty_instanceCache();

  logprintfl(EUCAINFO,"TerminateInstances(): called \n");
  logprintfl(EUCADEBUG,"TerminateInstances(): params: userId=%s, instIdsLen=%d, firstInstId=%s, force=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), instIdsLen, SP(instIdsLen ? instIds[0] : "UNSET"), force);
  
  sem_mywait(RESCACHE);
  memcpy(&resourceCacheLocal, resourceCache, sizeof(ccResourceCache));
  sem_mypost(RESCACHE);
  

  for (i=0; i<instIdsLen; i++) {
    instId = instIds[i];
    rc = find_instanceCacheId(instId, &myInstance);
    if (!rc) {
      // found the instance in the cache
       if (myInstance != NULL && (!strcmp(myInstance->state, "Pending") || !strcmp(myInstance->state, "Extant") || !strcmp(myInstance->state, "Unknown"))) {
	start = myInstance->ncHostIdx;
	stop = start+1;
      } else {
	// instance is not in a terminatable state
	start = 0;
	stop = 0;
	(*outStatus)[i] = 0;
      }
      if (myInstance) free(myInstance);
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

	rc = ncClientCall(ccMeta, 0, resourceCacheLocal.resources[j].lockidx, resourceCacheLocal.resources[j].ncURL, "ncTerminateInstance", instId, force, &shutdownState, &previousState);
	if (rc) {
	  (*outStatus)[i] = 1;
	  logprintfl(EUCAWARN, "TerminateInstances(): failed to terminate '%s': instance may not exist any longer\n", instId);
	  ret = 1;
	} else {
	  (*outStatus)[i] = 0;
	  ret = 0;
	  done++;
	}
	rc = ncClientCall(ccMeta, 0, resourceCacheStage->resources[j].lockidx, resourceCacheStage->resources[j].ncURL, "ncAssignAddress", instId, "0.0.0.0");
	if (rc) {
	  // problem, but will retry next time
	  logprintfl(EUCAWARN, "TerminateInstances(): could not send AssignAddress to NC\n");
	}
      }
    }
  }
  
  logprintfl(EUCADEBUG,"TerminateInstances(): done. \n");
  
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
  if (rc || ccIsEnabled()) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "CreateImage(): called \n");
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
  
  logprintfl(EUCADEBUG,"CreateImage(): done. \n");
  
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

  if (ccMeta != NULL) {
    logprintfl(EUCADEBUG, "initialize(): ccMeta: userId=%s correlationId=%s\n", ccMeta->userId, ccMeta->correlationId);
  }

  if (!ret) {
    // store information from CLC that needs to be kept up-to-date in the CC
    if (ccMeta != NULL) {
      int i;
      sem_mywait(CONFIG);
      memcpy(config->services, ccMeta->services, sizeof(serviceInfoType) * 16);
      memcpy(config->disabledServices, ccMeta->disabledServices, sizeof(serviceInfoType) * 16);
      memcpy(config->notreadyServices, ccMeta->notreadyServices, sizeof(serviceInfoType) * 16);
      
      for (i=0; i<16; i++) {
	int j;
		if (strlen(config->services[i].type)) {
	  // search for this CCs serviceInfoType
		  /*  if (!strcmp(config->services[i].type, "cluster")) {
	    char uri[MAX_PATH], uriType[32], host[MAX_PATH], path[MAX_PATH];
	    int port, done;
	    snprintf(uri, MAX_PATH, "%s", config->services[i].uris[0]);
	    rc = tokenize_uri(uri, uriType, host, &port, path);
	    if (strlen(host)) {
	      done=0;
	      for (j=0; j<32 && !done; j++) {
		uint32_t hostip;
		hostip = dot2hex(host);
		if (hostip == vnetconfig->localIps[j]) {
		  // found a match, update local serviceInfoType
		  memcpy(&(config->ccStatus.serviceId), &(config->services[i]), sizeof(serviceInfoType));
		  done++;
		}
	      }
	    }
	    } else */
	  if (!strcmp(config->services[i].type, "eucalyptus")) {
	    char uri[MAX_PATH], uriType[32], host[MAX_PATH], path[MAX_PATH];
	    int port, done;
	    // this is the cloud controller serviceInfo
	    snprintf(uri, MAX_PATH, "%s", config->services[i].uris[0]);
	    rc = tokenize_uri(uri, uriType, host, &port, path);
	    if (strlen(host)) {
	      config->cloudIp = dot2hex(host);
	    }
	  }
	}
      }
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
    if (config->ccState == SHUTDOWNCC) {
      // CC is to be shut down, there is no transition out of this state
      return(0);
    }
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
  } else if (config->ccState == NOTREADY || config->ccState == SHUTDOWNCC) {
    snprintf(statestr, n, "NOTREADY");
  }
  return(0);
}

int ccCheckState(int clcTimer) {
  char localDetails[1024];
  int ret=0;
  char cmd[MAX_PATH];
  char buri[MAX_PATH], uriType[32], bhost[MAX_PATH], path[MAX_PATH], curi[MAX_PATH], chost[MAX_PATH];
  int port, done=0, i, j, rc;

  if (!config) {
    return(1);
  }
  // check local configuration
  if (config->ccState == SHUTDOWNCC) {
    logprintfl(EUCAINFO, "ccCheckState(): this cluster controller marked as shut down\n");
    ret++;
  }

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
  // arbitrators
  if (clcTimer == 1 && strlen(config->arbitrators)) {
    char *tok, buf[256], *host;
    uint32_t hostint;
    int count=0;
    int arbitratorFails=0;
    snprintf(buf, 255, "%s", config->arbitrators);
    tok = strtok(buf, " ");
    while(tok && count<3) {
      hostint = dot2hex(tok);
      host = hex2dot(hostint);
      if (host) {
	logprintfl(EUCADEBUG, "ccCheckState(): checking health of arbitrator (%s)\n", tok);
	snprintf(cmd, 255, "ping -c 1 %s", host);
	rc = system(cmd);
	if (rc) {
	  logprintfl(EUCADEBUG, "ccCheckState(): cannot ping arbitrator %s (ping rc=%d)\n", host, rc);
	  arbitratorFails++;
	}
	free(host);
      }
      tok = strtok(NULL, " ");
      count++;
    }
    if (arbitratorFails) {
      config->arbitratorFails++;
    } else {
      config->arbitratorFails=0;
    }

    if (config->arbitratorFails > 10) {
      logprintfl(EUCADEBUG, "ccCheckState(): more than 10 arbitrator ping fails in a row (%d), failing check\n", config->arbitratorFails);
      ret++;
    }
  }

  // broker pairing algo
  for (i=0; i<16; i++) {
    int j;
    if (strlen(config->notreadyServices[i].type)) {
      if (!strcmp(config->notreadyServices[i].type, "vmwarebroker")) {
	for (j=0; j<8; j++) {
	  if (strlen(config->notreadyServices[i].uris[j])) {
	    logprintfl(EUCADEBUG, "ccCheckState(): found broker - %s\n", config->notreadyServices[i].uris[j]);
	    
	    snprintf(buri, MAX_PATH, "%s", config->notreadyServices[i].uris[j]);
	    bzero(bhost, sizeof(char) * MAX_PATH);
	    rc = tokenize_uri(buri, uriType, bhost, &port, path);
	    
	    snprintf(curi, MAX_PATH, "%s", config->ccStatus.serviceId.uris[0]);
	    bzero(chost, sizeof(char) * MAX_PATH);
	    rc = tokenize_uri(curi, uriType, chost, &port, path);
	    logprintfl(EUCADEBUG, "ccCheckState(): comparing found not ready broker host (%s) with local CC host (%s)\n", bhost, chost);
	    if (!strcmp(chost, bhost)) {
	      logprintfl(EUCAWARN, "ccCheckState(): detected local broker (%s) matching local CC (%s) in NOTREADY state\n", bhost, chost);
	      ret++;
	    }
	  }
	}
      }
    }
  }

  snprintf(localDetails, 1023, "ERRORS=%d", ret);
  snprintf(config->ccStatus.details, 1023, "%s", localDetails);
  
  return(ret);
}

/* 
   The CC will start a background thread to poll its collection of
   nodes.  This thread populates an in-memory cache of instance and
   resource information that can be accessed via the regular
   describeInstances and describeResources calls to the CC.  The
   purpose of this separation is to allow for a more scalable
   framework where describe operations do not block on access to node
   controllers.  
*/
void *monitor_thread(void *in) {
  int rc, ncTimer, clcTimer, ncRefresh = 0, clcRefresh = 0;
  ncMetadata ccMeta;
  char pidfile[MAX_PATH], *pidstr=NULL;

  bzero(&ccMeta, sizeof(ncMetadata));
  ccMeta.correlationId = strdup("monitor");
  ccMeta.userId = strdup("eucalyptus");
  if (!ccMeta.correlationId || !ccMeta.userId) {
    logprintfl(EUCAFATAL, "monitor_thread(): out of memory!\n");
    unlock_exit(1);
  }

  // set up default signal handler for this child process (for SIGTERM)
  struct sigaction newsigact;
  newsigact.sa_handler = SIG_DFL;
  newsigact.sa_flags = 0;
  sigemptyset(&newsigact.sa_mask);
  sigprocmask(SIG_SETMASK, &newsigact.sa_mask, NULL);
  sigaction(SIGTERM, &newsigact, NULL);

  ncTimer = config->ncPollingFrequency+1;
  clcTimer = config->clcPollingFrequency+1;
  
  while(1) {
    logprintfl(EUCADEBUG, "monitor_thread(): running\n");
    
    if (config->kick_enabled) {
      ccChangeState(ENABLED);
      config->kick_enabled = 0;
    }

    rc = update_config();
    if (rc) {
      logprintfl(EUCAWARN, "refresh_resources(): bad return from update_config(), check your config file\n");
    }

    if (config->ccState == ENABLED) {

      // NC Polling operations
      if (ncTimer >= config->ncPollingFrequency) {
	ncTimer=0;
	ncRefresh=1;
      }
      ncTimer++;

      // CLC Polling operations
      if (clcTimer >= config->clcPollingFrequency) {
	clcTimer=0;
	clcRefresh=1;
      }
      clcTimer++;

      if (ncRefresh) {
	rc = refresh_resources(&ccMeta, 60, 1);
	if (rc) {
	  logprintfl(EUCAWARN, "monitor_thread(): call to refresh_resources() failed in monitor thread\n");
	}
	
	rc = refresh_instances(&ccMeta, 60, 1);
	if (rc) {
	  logprintfl(EUCAWARN, "monitor_thread(): call to refresh_instances() failed in monitor thread\n");
	}
      }
      
      if (ncRefresh) {
	if (is_clean_instanceCache()) {
	// Network state operations
	//	sem_mywait(RESCACHE);
	
	  logprintfl(EUCADEBUG, "monitor_thread(): syncing network state\n");
	  rc = syncNetworkState();
	  if (rc) {
	    logprintfl(EUCADEBUG, "monitor_thread(): syncNetworkState() triggering network restore\n");
	    config->kick_network = 1;
	  }
	  //	sem_mypost(RESCACHE);
	  
	  if (config->kick_network) {
	    logprintfl(EUCADEBUG, "monitor_thread(): restoring network state\n");
	    rc = restoreNetworkState();
	    if (rc) {
	      // failed to restore network state, continue 
	      logprintfl(EUCAWARN, "monitor_thread(): restoreNetworkState returned false (may be already restored)\n");
	    } else {
	      sem_mywait(CONFIG);
	      config->kick_network = 0;
	      sem_mypost(CONFIG);
	    }
	  }
	} else {
	  logprintfl(EUCADEBUG, "monitor_thread(): instanceCache is dirty, skipping network update\n");
	}
      }


      if (clcRefresh) {
	logprintfl(EUCADEBUG, "monitor_thread(): syncing CLC network rules ground truth with local state\n");
	rc = reconfigureNetworkFromCLC();
	if (rc) {
	  logprintfl(EUCAWARN, "monitor_thread(): cannot get network ground truth from CLC\n");
	}
      }

      if (ncRefresh) {
	logprintfl(EUCADEBUG, "monitor_thread(): maintaining network state\n");
	rc = maintainNetworkState();
	if (rc) {
	  logprintfl(EUCAERROR, "monitor_thread(): network state maintainance failed\n");
	}
      }
      
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
      config->kick_monitor_running = 1;
    } else {
      // this CC is not enabled, ensure that local network state is disabled
      rc = clean_network_state();
      if (rc) {
	logprintfl(EUCAERROR, "monitor_thread(): could not cleanup network state\n");
      }
    }

    // do state checks under CONFIG lock
    sem_mywait(CONFIG);
    if (ccCheckState(clcTimer)) {
      logprintfl(EUCAERROR, "monitor_thread(): ccCheckState() returned failures\n");
      config->kick_enabled = 0;
      ccChangeState(NOTREADY);
    } else if (config->ccState == NOTREADY) {
      ccChangeState(DISABLED);
    }
    sem_mypost(CONFIG);
    shawn();
    
    logprintfl(EUCADEBUG, "monitor_thread(localState=%s): done\n", config->ccStatus.localState);
    //sleep(config->ncPollingFrequency);
    ncRefresh = clcRefresh = 0;
    sleep(1);
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
  int rc, loglevel, logrollnumber, ret;
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

    tmpstr = getConfString(configFiles, 2, "LOGROLLNUMBER");
    if (!tmpstr) {
      logrollnumber = 4;
    } else {
      logrollnumber = atoi(tmpstr);
    }
    if (tmpstr) free(tmpstr);

    // set up logfile
    logfile(logFile, loglevel, logrollnumber);
    
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
      if (statbuf.st_mtime > 0 || statbuf.st_ctime > 0) {
	if (statbuf.st_ctime > statbuf.st_mtime) {
	  configMtime = statbuf.st_ctime;
	} else {
	  configMtime = statbuf.st_mtime;
	}
      }
    }
  }
  if (configMtime == 0) {
    logprintfl(EUCAERROR, "update_config(): could not stat config files (%s,%s)\n", config->configFiles[0], config->configFiles[1]);
    sem_mypost(CONFIG);
    return(1);
  }
  
  // check to see if the configfile has changed
  logprintfl(EUCADEBUG, "update_config(): current mtime=%d, stored mtime=%d\n", configMtime, config->configMtime);
  if (config->configMtime != configMtime) {
    rc = readConfigFile(config->configFiles, 2);
    if (rc) {
      // something has changed that can be read in
      logprintfl(EUCAINFO, "update_config(): ingressing new options.\n");

      // NODES
      logprintfl(EUCAINFO, "update_config(): refreshing node list.\n");
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

      // CC Arbitrators
      tmpstr = configFileValue("CC_ARBITRATORS");
      if (tmpstr) {
	snprintf(config->arbitrators, 255, "%s", tmpstr);
	free(tmpstr);
      } else {
	bzero(config->arbitrators, 256);
      }

      // polling frequencies

      // CLC
      tmpstr = configFileValue("CLC_POLLING_FREQUENCY");
      if (tmpstr) {
	if (atoi(tmpstr) > 0) {
	  config->clcPollingFrequency = atoi(tmpstr);
	} else {
	  config->clcPollingFrequency = 6;
	}
	free(tmpstr);
      } else {
	config->clcPollingFrequency = 6;
      }

      // NC
      tmpstr = configFileValue("NC_POLLING_FREQUENCY");
      if (tmpstr) {
	if (atoi(tmpstr) > 6) {
	  config->ncPollingFrequency = atoi(tmpstr);
	} else {
	  config->ncPollingFrequency = 6;	  
	}
	free(tmpstr);
      } else {
	config->ncPollingFrequency = 6;
      }

    }
  }
  
  config->configMtime = configMtime;
  sem_mypost(CONFIG);
  
  return(ret);
}

int init_config(void) {
  ccResource *res=NULL;
  char *tmpstr=NULL, *proxyIp=NULL;
  int rc, numHosts, use_wssec, use_tunnels, use_proxy, proxy_max_cache_size, schedPolicy, idleThresh, wakeThresh, ret, i;
  
  char configFiles[2][MAX_PATH], netPath[MAX_PATH], eucahome[MAX_PATH], policyFile[MAX_PATH], home[MAX_PATH], proxyPath[MAX_PATH], arbitrators[256];
  
  time_t configMtime, instanceTimeout, ncPollingFrequency, clcPollingFrequency, ncFanout;
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

  sem_mywait(INIT);

  if (config_init && config->initialized) {
    // this means that this thread has already been initialized
    sem_mypost(INIT);
    return(0);
  }
  
  if (config->initialized) {
    config_init = 1;
    sem_mypost(INIT);
    return(0);
  }
  
  logprintfl(EUCADEBUG, "init_config(): called.\n");
  logprintfl(EUCADEBUG, "init_config(): initializing CC configuration\n");  

  readConfigFile(configFiles, 2);
  
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
      *macPrefix=NULL;

    uint32_t *ips, *nms;
    int initFail=0, len, usednew=0;;
    
    // DHCP Daemon Configuration Params
    daemon = configFileValue("VNET_DHCPDAEMON");
    if (!daemon) {
      logprintfl(EUCAWARN,"init_config(): no VNET_DHCPDAEMON defined in config, using default\n");
    }
    
    dhcpuser = configFileValue("VNET_DHCPUSER");
    if (!dhcpuser) {
      dhcpuser = strdup("root");
      if (!dhcpuser) {
         logprintfl(EUCAFATAL,"init_config(): Out of memory\n");
	 unlock_exit(1);
      }
    }
    
    pubmode = configFileValue("VNET_MODE");
    if (!pubmode) {
      logprintfl(EUCAWARN,"init_config(): VNET_MODE is not defined, defaulting to 'SYSTEM'\n");
      pubmode = strdup("SYSTEM");
      if (!pubmode) {
         logprintfl(EUCAFATAL,"init_config(): Out of memory\n");
	 unlock_exit(1);
      }
    }
    
    macPrefix = configFileValue("VNET_MACPREFIX");
    if (!macPrefix) {
      logprintfl(EUCAWARN, "init_config(): VNET_MACPREFIX is not defined, defaulting to 'd0:0d'\n");
      macPrefix = strdup("d0:0d");
      if (!macPrefix) {
	logprintfl(EUCAFATAL, "init_config(): Out of memory!\n");
	unlock_exit(1);
      }
    } else {
      unsigned int a=0, b=0;
      if (sscanf(macPrefix, "%02X:%02X", &a,&b) != 2 || (a > 0xFF || b > 0xFF)) {
	logprintfl(EUCAWARN, "init_config(): VNET_MACPREFIX is not defined, defaulting to 'd0:0d'\n");
	if(macPrefix) free(macPrefix);
	macPrefix = strdup("d0:0d");
      }
    }
      
    pubInterface = configFileValue("VNET_PUBINTERFACE");
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
    privInterface = configFileValue("VNET_PRIVINTERFACE");
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
      tmpstr = configFileValue("VNET_INTERFACE");
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
    
    if (pubmode && (!strcmp(pubmode, "STATIC") || !strcmp(pubmode, "STATIC-DYNMAC"))) {
      pubSubnet = configFileValue("VNET_SUBNET");
      pubSubnetMask = configFileValue("VNET_NETMASK");
      pubBroadcastAddress = configFileValue("VNET_BROADCAST");
      pubRouter = configFileValue("VNET_ROUTER");
      pubDNS = configFileValue("VNET_DNS");
      pubDomainname = configFileValue("VNET_DOMAINNAME");
      pubmacmap = configFileValue("VNET_MACMAP");
      pubips = configFileValue("VNET_PUBLICIPS");

      if (!pubSubnet || !pubSubnetMask || !pubBroadcastAddress || !pubRouter || !pubDNS 
	  || (!strcmp(pubmode, "STATIC") && !pubmacmap) || (!strcmp(pubmode, "STATIC-DYNMAC") && !pubips)) {
	logprintfl(EUCAFATAL,"init_config(): in '%s' network mode, you must specify values for 'VNET_SUBNET, VNET_NETMASK, VNET_BROADCAST, VNET_ROUTER, VNET_DNS and %s'\n", 
		   pubmode, (!strcmp(pubmode, "STATIC")) ? "VNET_MACMAP" : "VNET_PUBLICIPS");
	initFail = 1;
      }

    } else if (pubmode && (!strcmp(pubmode, "MANAGED") || !strcmp(pubmode, "MANAGED-NOVLAN"))) {
      numaddrs = configFileValue("VNET_ADDRSPERNET");
      pubSubnet = configFileValue("VNET_SUBNET");
      pubSubnetMask = configFileValue("VNET_NETMASK");
      pubDNS = configFileValue("VNET_DNS");
      pubDomainname = configFileValue("VNET_DOMAINNAME");
      pubips = configFileValue("VNET_PUBLICIPS");
      localIp = configFileValue("VNET_LOCALIP");
      if (!localIp) {
	logprintfl(EUCAWARN, "init_config(): VNET_LOCALIP not defined, will attempt to auto-discover (consider setting this explicitly if tunnelling does not function properly.)\n");
      }

      if (!pubSubnet || !pubSubnetMask || !pubDNS || !numaddrs) {
	logprintfl(EUCAFATAL,"init_config(): in 'MANAGED' or 'MANAGED-NOVLAN' network mode, you must specify values for 'VNET_SUBNET, VNET_NETMASK, VNET_ADDRSPERNET, and VNET_DNS'\n");
	initFail = 1;
      }
    }
    
    if (initFail) {
      logprintfl(EUCAFATAL, "init_config(): bad network parameters, must fix before system will work\n");
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
      if (macPrefix) free(macPrefix);
      sem_mypost(INIT);
      return(1);
    }
    
    sem_mywait(VNET);
    
    int ret = vnetInit(vnetconfig, pubmode, eucahome, netPath, CLC, pubInterface, privInterface, numaddrs, pubSubnet, pubSubnetMask, pubBroadcastAddress, pubDNS, pubDomainname, pubRouter, daemon, dhcpuser, NULL, localIp, macPrefix);
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
    if (macPrefix) free(macPrefix);
    if (localIp) free(localIp);

    if(ret > 0) {
      sem_mypost(VNET);
      sem_mypost(INIT);
      return(1);
    }
    
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
      //free(pubips);
    }
    
    if(pubips) free(pubips);
    sem_mypost(VNET);
  }
  
  tmpstr = configFileValue("SCHEDPOLICY");
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
  tmpstr = configFileValue("POWER_IDLETHRESH");
  if (!tmpstr) {
    if (SCHEDPOWERSAVE == schedPolicy)
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

  tmpstr = configFileValue("POWER_WAKETHRESH");
  if (!tmpstr) {
    if (SCHEDPOWERSAVE == schedPolicy)
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
  tmpstr = configFileValue("NC_POLLING_FREQUENCY");
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

  tmpstr = configFileValue("CLC_POLLING_FREQUENCY");
  if (!tmpstr) {
    clcPollingFrequency = 6;
    tmpstr = NULL;
  } else {
    clcPollingFrequency = atoi(tmpstr);
    if (clcPollingFrequency < 1) {
      logprintfl(EUCAWARN, "init_config(): CLC_POLLING_FREQUENCY set too low (%d seconds), resetting to default (6 seconds)\n", clcPollingFrequency);
      clcPollingFrequency = 6;
    }
  }
  if (tmpstr) free(tmpstr);

  // CC Arbitrators
  tmpstr = configFileValue("CC_ARBITRATORS");
  if (tmpstr) {
    snprintf(arbitrators, 255, "%s", tmpstr);
    free(tmpstr);
  } else {
    bzero(arbitrators, 256);
  }

  tmpstr = configFileValue("NC_FANOUT");
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

  tmpstr = configFileValue("INSTANCE_TIMEOUT");
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
  tmpstr = configFileValue("ENABLE_WS_SECURITY");
  if (!tmpstr) {
    // error
    logprintfl(EUCAFATAL,"init_config(): parsing config file (%s) for ENABLE_WS_SECURITY\n", configFiles[0]);
    sem_mypost(INIT);
    return(1);
  } else {
    if (!strcmp(tmpstr, "Y")) {
      use_wssec = 1;
    }
  }
  if (tmpstr) free(tmpstr);

  // Multi-cluster tunneling
  use_tunnels = 1;
  tmpstr = configFileValue("DISABLE_TUNNELING");
  if (tmpstr) {
    if (!strcmp(tmpstr, "Y")) {
      use_tunnels = 0;
    }
  }
  if (tmpstr) free(tmpstr);

  // CC Image Caching
  proxyIp = NULL;
  use_proxy=0;
  tmpstr = configFileValue("CC_IMAGE_PROXY");
  if (tmpstr) {
    proxyIp = strdup(tmpstr);
    if (!proxyIp) {
      logprintfl(EUCAFATAL, "init_config(): out of memory!\n");
      unlock_exit(1);
    }
    use_proxy=1;
  }
  if (tmpstr) free(tmpstr);

  proxy_max_cache_size=32768;
  tmpstr = configFileValue("CC_IMAGE_PROXY_CACHE_SIZE");
  if (tmpstr) {
    proxy_max_cache_size = atoi(tmpstr);
  }
  if (tmpstr) free(tmpstr);

  tmpstr = configFileValue("CC_IMAGE_PROXY_PATH");
  if (tmpstr) tmpstr = replace_string(&tmpstr, "$EUCALYPTUS", eucahome);
  if (tmpstr) {
    snprintf(proxyPath, MAX_PATH, "%s", tmpstr);
    free(tmpstr);
  } else {
    snprintf(proxyPath, MAX_PATH, "%s/var/lib/eucalyptus/dynserv", eucahome);
  }

  sem_mywait(CONFIG);
  // set up the current config   
  safe_strncpy(config->eucahome, eucahome, MAX_PATH);
  safe_strncpy(config->policyFile, policyFile, MAX_PATH);
  //  snprintf(config->proxyPath, MAX_PATH, "%s/var/lib/eucalyptus/dynserv/data", config->eucahome);
  snprintf(config->proxyPath, MAX_PATH, "%s", proxyPath);
  config->use_proxy = use_proxy;
  config->proxy_max_cache_size = proxy_max_cache_size;
  if (use_proxy) {
    snprintf(config->proxyIp, 32, "%s", proxyIp);
  }
  if(proxyIp) free(proxyIp);

  config->use_wssec = use_wssec;
  config->use_tunnels = use_tunnels;
  config->schedPolicy = schedPolicy;
  config->idleThresh = idleThresh;
  config->wakeThresh = wakeThresh;
  //  config->configMtime = configMtime;
  config->instanceTimeout = instanceTimeout;
  config->ncPollingFrequency = ncPollingFrequency;
  config->clcPollingFrequency = clcPollingFrequency;
    config->ncFanout = ncFanout;
  locks[REFRESHLOCK] = sem_open("/eucalyptusCCrefreshLock", O_CREAT, 0644, config->ncFanout);
  config->initialized = 1;
  ccChangeState(LOADED);
  config->ccStatus.localEpoch = 0;
  snprintf(config->arbitrators, 255, "%s", arbitrators);
  snprintf(config->ccStatus.details, 1024, "ERRORS=0");
  snprintf(config->ccStatus.serviceId.type, 32, "cluster");
  snprintf(config->ccStatus.serviceId.name, 32, "self");
  config->ccStatus.serviceId.urisLen=0;
  for (i=0; i<32 && config->ccStatus.serviceId.urisLen < 8; i++) {
    if (vnetconfig->localIps[i]) {
      char *host;
      host = hex2dot(vnetconfig->localIps[i]);
      if (host) {
	snprintf(config->ccStatus.serviceId.uris[config->ccStatus.serviceId.urisLen], 512, "http://%s:8774/axis2/services/EucalyptusCC", host);
	config->ccStatus.serviceId.urisLen++;
	free(host);
      } 
    }
  }
  snprintf(config->configFiles[0], MAX_PATH, "%s", configFiles[0]);
  snprintf(config->configFiles[1], MAX_PATH, "%s", configFiles[1]);
  
  logprintfl(EUCAINFO, "init_config(): CC Configuration: eucahome=%s, policyfile=%s, ws-security=%s, schedulerPolicy=%s, idleThreshold=%d, wakeThreshold=%d\n", SP(config->eucahome), SP(config->policyFile), use_wssec ? "ENABLED" : "DISABLED", SP(SCHEDPOLICIES[config->schedPolicy]), config->idleThresh, config->wakeThresh);
  sem_mypost(CONFIG);

  res = NULL;
  rc = refreshNodes(config, &res, &numHosts);
  if (rc) {
    logprintfl(EUCAERROR, "init_config(): cannot read list of nodes, check your config file\n");
    sem_mypost(INIT);
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

  sem_mypost(INIT);
  return(0);
}

int syncNetworkState() {
  int rc, ret=0;

  logprintfl(EUCADEBUG, "syncNetworkState(): syncing public/private IP mapping ground truth with local state\n");
  rc = map_instanceCache(validCmp, NULL, instIpSync, NULL);
  if (rc) {
    logprintfl(EUCAWARN, "syncNetworkState(): network sync implies network restore is necessary\n");
    ret++;
  }


  return(ret);
}

int checkActiveNetworks() {
  int i, rc;
  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    int activeNetworks[NUMBER_OF_VLANS];
    bzero(activeNetworks, sizeof(int) * NUMBER_OF_VLANS);
    
    logprintfl(EUCADEBUG, "checkActiveNetworks(): maintaining active networks\n");
    for (i=0; i<MAXINSTANCES; i++) {
      if ( instanceCache->cacheState[i] != INSTINVALID ) {
	if ( strcmp(instanceCache->instances[i].state, "Teardown") ) {
	  int vlan = instanceCache->instances[i].ccnet.vlan;
	  activeNetworks[vlan] = 1;
	  if ( ! vnetconfig->networks[vlan].active ) {
	    logprintfl(EUCAWARN, "checkActiveNetworks(): instance running in network that is currently inactive (%s, %s, %d)\n", vnetconfig->users[vlan].userName, vnetconfig->users[vlan].netName, vlan);
	  }
	}
      }
    }
    
    for (i=0; i<NUMBER_OF_VLANS; i++) {
      sem_mywait(VNET);
      if ( !activeNetworks[i] && vnetconfig->networks[i].active ) {
	logprintfl(EUCAWARN, "checkActiveNetworks(): network active but no running instances (%s, %s, %d)\n", vnetconfig->users[i].userName, vnetconfig->users[i].netName, i);
	rc = vnetStopNetwork(vnetconfig, i, vnetconfig->users[i].userName, vnetconfig->users[i].netName);
	if (rc) {
	  logprintfl(EUCAERROR, "checkActiveNetworks(): failed to stop network (%s, %s, %d), will re-try\n", vnetconfig->users[i].userName, vnetconfig->users[i].netName, i);
	}
      }
      sem_mypost(VNET);

      /*
      if ( activeNetworks[i] ) {
	// make sure all active network indexes are used by an instance
	for (j=0; j<NUMBER_OF_HOSTS_PER_VLAN; j++) {
	  if (vnetconfig->networks[i].addrs[j].active && (vnetconfig->networks[i].addrs[j].ip != 0) ) {
	    // dan
	    char *ip=NULL;
	    ccInstance *myInstance=NULL;
	    
	    ip = hex2dot(vnetconfig->networks[i].addrs[j].ip);
	    rc = find_instanceCacheIP(ip, &myInstance);
	    if (rc) {
	      // network index marked as used, but no instance in cache with that index/ip
	      logprintfl(EUCAWARN, "checkActiveNetworks(): address active but no instances using addr (%s, %d, %d\n", ip, i, j);
	    } else {
	      logprintfl(EUCADEBUG, "checkActiveNetworks(): address active and found for instance (%s, %s, %d, %d\n", myInstance->instanceId, ip, i, j);
	    }
	    if (myInstance) free(myInstance);
	    if (ip) free(ip);
	  }
	}
      }
      */
    }
  }
  return(0);
}

int maintainNetworkState() {
  int rc, i, j, ret=0, done=0;
  char pidfile[MAX_PATH], *pidstr=NULL;
  
  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    //    rc = checkActiveNetworks();
    //    if (rc) {
    //      logprintfl(EUCAWARN, "maintainNetworkState(): checkActiveNetworks() failed, attempting to re-sync\n");
    //    }
    
    logprintfl(EUCADEBUG, "maintainNetworkState(): maintaining metadata redirect and tunnel health\n");
    sem_mywait(VNET);
    
    // check to see if cloudIp has changed
    char *cloudIp1 = hex2dot(config->cloudIp);
    char *cloudIp2 = hex2dot(vnetconfig->cloudIp);
    logprintfl(EUCADEBUG, "maintainNetworkState(): CCcloudIp=%s VNETcloudIp=%s\n", cloudIp1, cloudIp2);
    free(cloudIp1);
    free(cloudIp2);
    
    if (config->cloudIp && (config->cloudIp != vnetconfig->cloudIp)) {
      rc = vnetUnsetMetadataRedirect(vnetconfig);
      if (rc) {
	logprintfl(EUCAWARN, "maintainNetworkState(): failed to unset old metadata redirect\n");
      }
      vnetconfig->cloudIp = config->cloudIp;
      rc = vnetSetMetadataRedirect(vnetconfig);
      if (rc) {
	logprintfl(EUCAWARN, "maintainNetworkState(): failed to set new metadata redirect\n");
      }
    }

    // check to see if this CCs localIpId has changed
    if (vnetconfig->tunnels.localIpId != vnetconfig->tunnels.localIpIdLast) {
      logprintfl(EUCADEBUG, "maintainNetworkState(): local CC index has changed (%d -> %d): re-assigning gateway IPs and tunnel connections.\n", vnetconfig->tunnels.localIpId , vnetconfig->tunnels.localIpIdLast);
      
      for (i=2; i<NUMBER_OF_VLANS; i++) {
	if (vnetconfig->networks[i].active) {
	  char brname[32];
	  if (!strcmp(vnetconfig->mode, "MANAGED")) {
	    snprintf(brname, 32, "eucabr%d", i);
	  } else {
	    snprintf(brname, 32, "%s", vnetconfig->privInterface);
	  }

	  if (vnetconfig->tunnels.localIpIdLast >= 0) {
	    vnetDelGatewayIP(vnetconfig, i, brname, vnetconfig->tunnels.localIpIdLast);
	  }
	  if (vnetconfig->tunnels.localIpId >= 0) {
	    vnetAddGatewayIP(vnetconfig, i, brname, vnetconfig->tunnels.localIpId);
	  }
	}
      }
      rc = vnetTeardownTunnels(vnetconfig);
      if (rc) {
	logprintfl(EUCAERROR, "maintainNetworkState(): failed to tear down tunnels\n");
	ret = 1;
      }
      
      config->kick_dhcp = 1;
      vnetconfig->tunnels.localIpIdLast = vnetconfig->tunnels.localIpId;
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
	rc = vnetAttachTunnels(vnetconfig, i, brname);
	if (rc) {
	  logprintfl(EUCADEBUG, "maintainNetworkState(): failed to attach tunnels for vlan %d during maintainNetworkState()\n", i);
	  ret = 1;
	}
      }
    }
   
    //    rc = vnetApplyArpTableRules(vnetconfig);
    //    if (rc) {
    //      logprintfl(EUCAWARN, "maintainNetworkState(): failed to maintain arp tables\n");
    //    }
    
    sem_mypost(VNET);
  }

  sem_mywait(CONFIG);
  snprintf(pidfile, MAX_PATH, "%s/var/run/eucalyptus/net/euca-dhcp.pid", config->eucahome);
  if (!check_file(pidfile)) {
    pidstr = file2str(pidfile);
  } else {
    pidstr = NULL;
  }
  if (config->kick_dhcp || !pidstr || check_process(atoi(pidstr), "euca-dhcp.pid")) {
    rc = vnetKickDHCP(vnetconfig);
    if (rc) {
      logprintfl(EUCAERROR, "maintainNetworkState(): cannot start DHCP daemon\n");
      ret=1;
    } else {
      config->kick_dhcp = 0;
    }
  }
  sem_mypost(CONFIG);

  if(pidstr) 
     free(pidstr);

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

  // sync up internal network state with information from instances
  logprintfl(EUCADEBUG, "restoreNetworkState(): syncing internal network state with current instance state\n");
  rc = map_instanceCache(validCmp, NULL, instNetParamsSet, NULL);
  if (rc) {
    logprintfl(EUCAERROR, "restoreNetworkState(): could not sync internal network state with current instance state\n");
    ret = 1;
  }

  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    // restore iptables state, if internal iptables state exists
    logprintfl(EUCADEBUG, "restoreNetworkState(): restarting iptables\n");
    rc = vnetRestoreTablesFromMemory(vnetconfig);
    if (rc) {
      logprintfl(EUCAERROR, "restoreNetworkState(): cannot restore iptables state\n");
      ret = 1;
    }
    
    // re-create all active networks (bridges, vlan<->bridge mappings)
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
    
    rc = map_instanceCache(validCmp, NULL, instNetReassignAddrs, NULL);
    if (rc) {
      logprintfl(EUCAERROR, "restoreNetworkState(): could not (re)assign public/private IP mappings\n");
      ret = 1;
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

  logprintfl(EUCADEBUG, "restoreNetworkState(): done restoring network state\n");

  return(ret);
}

int reconfigureNetworkFromCLC() {
  char clcnetfile[MAX_PATH], chainmapfile[MAX_PATH], url[MAX_PATH], cmd[MAX_PATH];
  char *cloudIp=NULL, **users=NULL, **nets=NULL;
  int fd=0, i=0, rc=0, ret=0, usernetlen=0;
  FILE *FH=NULL;

  if (strcmp(vnetconfig->mode, "MANAGED") && strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    return(0);
  }
  
  // get the latest cloud controller IP address
  if (vnetconfig->cloudIp) {
    cloudIp = hex2dot(vnetconfig->cloudIp);
  } else {
    cloudIp = strdup("localhost");
    if (!cloudIp) {
      logprintfl(EUCAFATAL, "init_config(): out of memory!\n");
      unlock_exit(1);
    }
  }

  // create and populate network state files
  snprintf(clcnetfile, MAX_PATH, "/tmp/euca-clcnet-XXXXXX");
  snprintf(chainmapfile, MAX_PATH, "/tmp/euca-chainmap-XXXXXX");
  
  fd = safe_mkstemp(clcnetfile);
  if (fd < 0) {
    logprintfl(EUCAERROR, "reconfigureNetworkFromCLC(): cannot open clcnetfile '%s'\n", clcnetfile);
    if(cloudIp)
       free(cloudIp);
    return(1);
  }
  chmod(clcnetfile, 0644);
  close(fd);

  fd = safe_mkstemp(chainmapfile);
  if (fd < 0) {
    logprintfl(EUCAERROR, "reconfigureNetworkFromCLC(): cannot open chainmapfile '%s'\n", chainmapfile);
    if (cloudIp) free(cloudIp);
    unlink(clcnetfile);
    return(1);
  }
  chmod(chainmapfile, 0644);
  close(fd);

  // clcnet populate
  snprintf(url, MAX_PATH, "http://%s:8773/latest/network-topology", cloudIp);
  rc = http_get_timeout(url, clcnetfile, 0, 0, 10, 15);
  if (cloudIp) free(cloudIp);
  if (rc) {
    logprintfl(EUCAWARN, "reconfigureNetworkFromCLC(): cannot get latest network topology from cloud controller\n");
    unlink(clcnetfile);
    unlink(chainmapfile);
    return(1);
  }

  // chainmap populate
  FH = fopen(chainmapfile, "w");
  if (!FH) {
    logprintfl(EUCAERROR, "reconfigureNetworkFromCLC(): cannot write chain/net map to chainmap file '%s'\n", chainmapfile);
    unlink(clcnetfile);
    unlink(chainmapfile);
    return(1);
  }

  sem_mywait(VNET);
  rc = vnetGetAllVlans(vnetconfig, &users, &nets, &usernetlen);
  if (rc) {
  } else {
    for (i=0; i<usernetlen; i++) {
      fprintf(FH, "%s %s\n", users[i], nets[i]);
      free(users[i]);
      free(nets[i]);
    }
  }
  fclose(FH);
  
  if(users) free(users);
  if(nets) free(nets);

  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/euca_ipt filter %s %s", vnetconfig->eucahome, vnetconfig->eucahome, clcnetfile, chainmapfile);
  rc = system(cmd);
  if (rc) {
    logprintfl(EUCAERROR, "reconfigureNetworkFromCLC(): cannot run command '%s'\n", cmd);
    ret = 1;
  }
  sem_mypost(VNET);

  unlink(clcnetfile);
  unlink(chainmapfile);

  return(ret);
}

int refreshNodes(ccConfig *config, ccResource **res, int *numHosts) {
  int rc, i, lockmod;
  char *tmpstr, *ipbuf;
  char ncservice[512];
  int ncport;
  char **hosts;

  *numHosts = 0;
  *res = NULL;

  tmpstr = configFileValue(CONFIG_NC_SERVICE);
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

  tmpstr = configFileValue(CONFIG_NC_PORT);
  if (!tmpstr) {
    // error
    logprintfl(EUCAFATAL,"refreshNodes(): parsing config files (%s,%s) for NC_PORT\n", config->configFiles[1], config->configFiles[0]);
    return(1);
  } else {
    if(tmpstr)
      ncport = atoi(tmpstr);
  }
  if (tmpstr) free(tmpstr);

  tmpstr = configFileValue(CONFIG_NODES);
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

  if (config->use_proxy) {
    rc = image_cache_proxykick(*res, numHosts);
    if (rc) {
      logprintfl(EUCAERROR, "refreshNodes(): could not restart the image proxy\n");
    }
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

int allocate_ccResource(ccResource *out, char *ncURL, char *ncService, int ncPort, char *hostname, char *mac, char *ip, int maxMemory, int availMemory, int maxDisk, int availDisk, int maxCores, int availCores, int state, int laststate, time_t stateChange, time_t idleStart) {

  if (out != NULL) {
    if (ncURL) safe_strncpy(out->ncURL, ncURL, 128);
    if (ncService) safe_strncpy(out->ncService, ncService, 128);
    if (hostname) safe_strncpy(out->hostname, hostname, 128);
    if (mac) safe_strncpy(out->mac, mac, 24);
    if (ip) safe_strncpy(out->ip, ip, 24);
    
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

int allocate_ccInstance(ccInstance *out, char *id, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char *ownerId, char *accountId, char *state, char *ccState, time_t ts, char *reservationId, netConfig *ccnet, netConfig *ncnet, virtualMachine *ccvm, int ncHostIdx, char *keyName, char *serviceTag, char *userData, char *launchIndex, char *platform, char *bundleTaskStateName, char groupNames[][64], ncVolume *volumes, int volumesSize) {
  if (out != NULL) {
    bzero(out, sizeof(ccInstance));
    if (id) safe_strncpy(out->instanceId, id, 16);
    if (amiId) safe_strncpy(out->amiId, amiId, 16);
    if (kernelId) safe_strncpy(out->kernelId, kernelId, 16);
    if (ramdiskId) safe_strncpy(out->ramdiskId, ramdiskId, 16);
    
    if (amiURL) safe_strncpy(out->amiURL, amiURL, 512);
    if (kernelURL) safe_strncpy(out->kernelURL, kernelURL, 512);
    if (ramdiskURL) safe_strncpy(out->ramdiskURL, ramdiskURL, 512);
    
    if (state) safe_strncpy(out->state, state, 16);
    if (state) safe_strncpy(out->ccState, ccState, 16);
    if (ownerId) safe_strncpy(out->ownerId, ownerId, 48);
    if (accountId) safe_strncpy(out->accountId, accountId, 48);
    if (reservationId) safe_strncpy(out->reservationId, reservationId, 16);
    if (keyName) safe_strncpy(out->keyName, keyName, 1024);
    out->ts = ts;
    out->ncHostIdx = ncHostIdx;
    if (serviceTag) safe_strncpy(out->serviceTag, serviceTag, 64);
    if (userData) safe_strncpy(out->userData, userData, 16384);
    if (launchIndex) safe_strncpy(out->launchIndex, launchIndex, 64);
    if (platform) safe_strncpy(out->platform, platform, 64);
    if (bundleTaskStateName) safe_strncpy(out->bundleTaskStateName, bundleTaskStateName, 64);
    if (groupNames) {
      int i;
      for (i=0; i<64; i++) {
	if (groupNames[i]) {
	  safe_strncpy(out->groupNames[i], groupNames[i], 64);
	}
      }
    }

    if (volumes) {
      memcpy(out->volumes, volumes, sizeof(ncVolume) * EUCA_MAX_VOLUMES);
    }
    out->volumesSize = volumesSize;

    if (ccnet) allocate_netConfig(&(out->ccnet), ccnet->privateMac, ccnet->privateIp, ccnet->publicIp, ccnet->vlan, ccnet->networkIndex);
    if (ncnet) allocate_netConfig(&(out->ncnet), ncnet->privateMac, ncnet->privateIp, ncnet->publicIp, ncnet->vlan, ncnet->networkIndex);
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
  
  if ( (strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant")) ) {
    snprintf(inst->ccnet.privateIp, 24, "0.0.0.0");
    return(0);
  }

  logprintfl(EUCADEBUG, "privIpSet(): set: %s/%s\n", inst->ccnet.privateIp, (char *)ip);
  snprintf(inst->ccnet.privateIp, 24, "%s", (char *)ip);
  return(0);
}

int pubIpSet(ccInstance *inst, void *ip) {
  if (!ip || !inst) {
    return(1);
  }

  if ( (strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant")) ) {
    snprintf(inst->ccnet.publicIp, 24, "0.0.0.0");
    return(0);
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
  if (!volbuf) {
    logprintfl(EUCAFATAL, "print_ccInstance(): out of memory!\n");
    unlock_exit(1);
  }
  bzero(volbuf, sizeof(ncVolume)*EUCA_MAX_VOLUMES*2);

  groupbuf = malloc(64*32*2);
  if (!groupbuf) {
    logprintfl(EUCAFATAL, "print_ccInstance(): out of memory!\n");
    unlock_exit(1);
  }
  bzero(groupbuf, 64*32*2);
  
  for (i=0; i<64; i++) {
    if (in->groupNames[i][0] != '\0') {
      strncat(groupbuf, in->groupNames[i], 64);
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
  
  logprintfl(EUCADEBUG, "print_ccInstance(): %s instanceId=%s reservationId=%s state=%s accountId=%s ownerId=%s ts=%d keyName=%s ccnet={privateIp=%s publicIp=%s privateMac=%s vlan=%d networkIndex=%d} ccvm={cores=%d mem=%d disk=%d} ncHostIdx=%d serviceTag=%s userData=%s launchIndex=%s platform=%s bundleTaskStateName=%s, volumesSize=%d volumes={%s} groupNames={%s}\n", tag, in->instanceId, in->reservationId, in->state, in->accountId, in->ownerId, in->ts, in->keyName, in->ccnet.privateIp, in->ccnet.publicIp, in->ccnet.privateMac, in->ccnet.vlan, in->ccnet.networkIndex, in->ccvm.cores, in->ccvm.mem, in->ccvm.disk, in->ncHostIdx, in->serviceTag, in->userData, in->launchIndex, in->platform, in->bundleTaskStateName, in->volumesSize, volbuf, groupbuf);

  free(volbuf);
  free(groupbuf);
}

void set_clean_instanceCache(void) {
  sem_mywait(INSTCACHE);
  instanceCache->dirty = 0;
  sem_mypost(INSTCACHE);
}
void set_dirty_instanceCache(void) {
  sem_mywait(INSTCACHE);
  instanceCache->dirty = 1;
  sem_mypost(INSTCACHE);
}

int is_clean_instanceCache(void) {
  int ret=1;
  sem_mywait(INSTCACHE);
  if (instanceCache->dirty) {
    ret = 0;
  } else {
    ret = 1;
  }
  sem_mypost(INSTCACHE);
  return(ret);
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
      // give precedence to instances that are in Extant/Pending over expired instances, when info comes from two different nodes
      if (strcmp(in->serviceTag, instanceCache->instances[i].serviceTag) && strcmp(in->state, instanceCache->instances[i].state) && !strcmp(in->state, "Teardown")) {
	// skip
	logprintfl(EUCADEBUG, "refresh_instanceCache(): skipping cache refresh with instance in Teardown (instance with non-Teardown from different node already cached)\n");
      } else {
	// update cached instance info
	memcpy(&(instanceCache->instances[i]), in, sizeof(ccInstance));
	instanceCache->lastseen[i] = time(NULL);
      }
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
  allocate_ccInstance(&(instanceCache->instances[firstNull]), in->instanceId, in->amiId, in->kernelId, in->ramdiskId, in->amiURL, in->kernelURL, in->ramdiskURL, in->ownerId, in->accountId, in->state, in->ccState, in->ts, in->reservationId, &(in->ccnet), &(in->ncnet), &(in->ccvm), in->ncHostIdx, in->keyName, in->serviceTag, in->userData, in->launchIndex, in->platform, in->bundleTaskStateName, in->groupNames, in->volumes, in->volumesSize);
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

      allocate_ccInstance(*out, instanceCache->instances[i].instanceId,instanceCache->instances[i].amiId, instanceCache->instances[i].kernelId, instanceCache->instances[i].ramdiskId, instanceCache->instances[i].amiURL, instanceCache->instances[i].kernelURL, instanceCache->instances[i].ramdiskURL, instanceCache->instances[i].ownerId, instanceCache->instances[i].accountId, instanceCache->instances[i].state, instanceCache->instances[i].ccState, instanceCache->instances[i].ts, instanceCache->instances[i].reservationId, &(instanceCache->instances[i].ccnet), &(instanceCache->instances[i].ncnet), &(instanceCache->instances[i].ccvm), instanceCache->instances[i].ncHostIdx, instanceCache->instances[i].keyName, instanceCache->instances[i].serviceTag, instanceCache->instances[i].userData, instanceCache->instances[i].launchIndex, instanceCache->instances[i].platform, instanceCache->instances[i].bundleTaskStateName, instanceCache->instances[i].groupNames, instanceCache->instances[i].volumes, instanceCache->instances[i].volumesSize);
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
	
	allocate_ccInstance(*out, instanceCache->instances[i].instanceId,instanceCache->instances[i].amiId, instanceCache->instances[i].kernelId, instanceCache->instances[i].ramdiskId, instanceCache->instances[i].amiURL, instanceCache->instances[i].kernelURL, instanceCache->instances[i].ramdiskURL, instanceCache->instances[i].ownerId, instanceCache->instances[i].accountId, instanceCache->instances[i].state, instanceCache->instances[i].ccState, instanceCache->instances[i].ts, instanceCache->instances[i].reservationId, &(instanceCache->instances[i].ccnet), &(instanceCache->instances[i].ncnet), &(instanceCache->instances[i].ccvm), instanceCache->instances[i].ncHostIdx, instanceCache->instances[i].keyName, instanceCache->instances[i].serviceTag, instanceCache->instances[i].userData, instanceCache->instances[i].launchIndex, instanceCache->instances[i].platform, instanceCache->instances[i].bundleTaskStateName, instanceCache->instances[i].groupNames, instanceCache->instances[i].volumes, instanceCache->instances[i].volumesSize);
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
	chmod(finalpath, 0600);
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
	chmod(finalpath, 0600);
      }
      exit(0);
    }
  }

  return(0);
}

int image_cache_invalidate() {
  time_t oldest;
  char proxyPath[MAX_PATH], path[MAX_PATH], oldestpath[MAX_PATH], oldestmanifestpath[MAX_PATH];
  DIR *DH=NULL;
  struct dirent dent, *result=NULL;
  struct stat mystat;
  int rc, total_megs=0;

  if (config->use_proxy) {
    proxyPath[0] = '\0';
    path[0] = '\0';
    oldestpath[0] = '\0';
    oldestmanifestpath[0] = '\0';

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
  if (!nodestr) {
    logprintfl(EUCAFATAL, "image_cache_proxykick(): out of memory!\n");
    unlock_exit(1);
  }
  
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

char *configFileValue(char *key) {
  int i;
  for (i=0; i<configRestartLen; i++) {
    if (configKeysRestart[i].key) {
      if (!strcmp(configKeysRestart[i].key, key)) {
	return(configValuesRestart[i] ? strdup(configValuesRestart[i]) : (configKeysRestart[i].defaultValue ? strdup(configKeysRestart[i].defaultValue) : NULL));
      }
    }
  }
  for (i=0; i<configNoRestartLen; i++) {
    if (configKeysNoRestart[i].key) {
      if (!strcmp(configKeysNoRestart[i].key, key)) {
	return(configValuesNoRestart[i] ? strdup(configValuesNoRestart[i]) : (configKeysNoRestart[i].defaultValue ? strdup(configKeysNoRestart[i].defaultValue) : NULL));
      }
    }
  }
  return(NULL);
}

int readConfigFile(char configFiles[][MAX_PATH], int numFiles) {
  int i, ret=0;
  char *old=NULL, *new=NULL;

  for (i=0; configKeysRestart[i].key; i++) {
    old = configValuesRestart[i];
    new = getConfString(configFiles, numFiles, configKeysRestart[i].key);
    if (configRestartLen) {
      if ( (!old && new) || (old && !new) || ( (old && new) && strcmp(old, new) ) ) {
	logprintfl(EUCAWARN, "readConfigFile(): configuration file changed (KEY=%s, ORIGVALUE=%s, NEWVALUE=%s): clean restart is required before this change will take effect!\n", configKeysRestart[i].key, SP(old), SP(new));
      }
      if (new) free(new);
    } else {
      logprintfl(EUCAINFO, "readConfigFile(): read (%s=%s, default=%s)\n", configKeysRestart[i].key, SP(new), SP(configKeysRestart[i].defaultValue));
      if (configValuesRestart[i]) free(configValuesRestart[i]);
      configValuesRestart[i] = new;
      ret++;
    }
  }
  configRestartLen = i;
  
  for (i=0; configKeysNoRestart[i].key; i++) {
    old = configValuesNoRestart[i];
    new = getConfString(configFiles, numFiles, configKeysNoRestart[i].key);
    
    if (configNoRestartLen) {
      if ( (!old && new) || (old && !new) || ( (old && new) && strcmp(old, new) ) ) {
	logprintfl(EUCAINFO, "readConfigFile(): configuration file changed (KEY=%s, ORIGVALUE=%s, NEWVALUE=%s): change will take effect immediately.\n", configKeysNoRestart[i].key, SP(old), SP(new));
	ret++;
	if (configValuesNoRestart[i]) free(configValuesNoRestart[i]);
	configValuesNoRestart[i] = new;
      } else {
	if (new) free(new);
      }
    } else {
      logprintfl(EUCAINFO, "readConfigFile(): read (%s=%s, default=%s)\n", configKeysNoRestart[i].key, SP(new), SP(configKeysNoRestart[i].defaultValue));
      if (configValuesNoRestart[i]) free(configValuesNoRestart[i]);
      configValuesNoRestart[i] = new;
      ret++;
    }
  }
  configNoRestartLen = i;

  return(ret);
}


#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <semaphore.h>

#include "axis2_skel_EucalyptusCC.h"

#include <server-marshal.h>
#include <handlers.h>
#include <storage.h>
#include <vnetwork.h>
#include <euca_auth.h>
#include <misc.h>

#include "data.h"
#include "client-marshal.h"

#define SUPERUSER "eucalyptus"

int init=0;
sem_t *initLock=NULL;

// to be stored in shared memory
ccConfig *config=NULL;
sem_t *configLock=NULL;

ccInstance *instanceCache=NULL;
sem_t *instanceCacheLock=NULL;

vnetConfig *vnetconfig=NULL;
sem_t *vnetConfigLock=NULL;

int doAttachVolume(ncMetadata *ccMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev) {
  int i, j, rc, start, stop, k, done, ret=0;
  ccInstance *myInstance, *out;
  ncStub *ncs;
  time_t op_start, op_timer;
  
  i = j = 0;
  myInstance = NULL;
  op_start = time(NULL);
  op_timer = OP_TIMEOUT;
  
  rc = init_config();
  if (rc) {
    return(1);
  }
  logprintfl(EUCADEBUG,"AttachVolume(): called\n");
  if (!volumeId || !instanceId || !remoteDev || !localDev) {
    logprintfl(EUCAERROR, "bad input params to AttachVolume()\n");
    return(1);
  }

  rc = find_instanceCacheId(instanceId, &myInstance);
  if (!rc) {
    // found the instance in the cache
    start = myInstance->ncHostIdx;
    stop = start+1;
    if (myInstance) free(myInstance);
  } else {
    start = 0;
    stop = config->numResources;
  }
  
  sem_wait(configLock);
  for (j=start; j<stop; j++) {
    // read the instance ids
    logprintfl(EUCAINFO,"AttachVolume(): calling attach volume (%s) on (%s)\n", instanceId, config->resourcePool[j].hostname);
    if (1) {
      int pid, status;
      pid = fork();
      if (pid == 0) {
	ncs = ncStubCreate(config->resourcePool[j].ncURL, NULL, NULL);
	if (config->use_wssec) {
	  rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
	}
	logprintfl(EUCADEBUG, "calling attachVol on NC: %s\n",  config->resourcePool[j].hostname);
	rc = 0;
	// here
	rc = ncAttachVolumeStub(ncs, ccMeta, instanceId, volumeId, remoteDev, localDev);
	if (!rc) {
	  ret = 0;
	} else {
	  ret = 1;
	}
	exit(ret);
      } else {
	op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	rc = timewait(pid, &status, op_timer / ((stop-start) - (j - start)));
	rc = WEXITSTATUS(status);
	logprintfl(EUCADEBUG,"\tcall complete (pid/rc): %d/%d\n", pid, rc);
      }
    }
    sem_post(configLock);
    
    if (!rc) {
      ret = 0;
    } else {
      logprintfl(EUCAERROR, "failed to attach volume '%s'\n", instanceId);
      ret = 1;
    }
  }
  
  //rc = refresh_resources(ccMeta, OP_TIMEOUT - (time(NULL) - op_start));
  
  logprintfl(EUCADEBUG,"AttachVolume(): done.\n");
  
  shawn();
  
  return(ret);
}

int doDetachVolume(ncMetadata *ccMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev, int force) {
  int i, j, rc, start, stop, k, done, ret=0;
  ccInstance *myInstance, *out;
  ncStub *ncs;
  time_t op_start, op_timer;
  
  i = j = 0;
  myInstance = NULL;
  op_start = time(NULL);
  op_timer = OP_TIMEOUT;
  
  rc = init_config();
  if (rc) {
    return(1);
  }
  logprintfl(EUCADEBUG,"DetachVolume(): called\n");
  if (!volumeId || !instanceId || !remoteDev || !localDev) {
    logprintfl(EUCAERROR, "bad input params to DetachVolume()\n");
    return(1);
  }
  
  rc = find_instanceCacheId(instanceId, &myInstance);
  if (!rc) {
    // found the instance in the cache
    start = myInstance->ncHostIdx;
    stop = start+1;
    if (myInstance) free(myInstance);
  } else {
    start = 0;
    stop = config->numResources;
  }
  
  sem_wait(configLock);
  for (j=start; j<stop; j++) {
    // read the instance ids
    logprintfl(EUCAINFO,"DetachVolume(): calling dettach volume (%s) on (%s)\n", instanceId, config->resourcePool[j].hostname);
    if (1) {
      int pid, status;
      pid = fork();
      if (pid == 0) {
	ncs = ncStubCreate(config->resourcePool[j].ncURL, NULL, NULL);
	if (config->use_wssec) {
	  rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
	}
	logprintfl(EUCADEBUG, "calling detachVol on NC: %s\n",  config->resourcePool[j].hostname);
	rc = 0;
	rc = ncDetachVolumeStub(ncs, ccMeta, instanceId, volumeId, remoteDev, localDev, force);
	if (!rc) {
	  ret = 0;
	} else {
	  ret = 1;
	}
	exit(ret);
      } else {
	op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	rc = timewait(pid, &status, op_timer / ((stop-start) - (j - start)));
	rc = WEXITSTATUS(status);
	logprintfl(EUCADEBUG,"\tcall complete (pid/rc): %d/%d\n", pid, rc);
      }
    }
    sem_post(configLock);
    
    if (!rc) {
      ret = 0;
    } else {
      logprintfl(EUCAERROR, "failed to dettach volume '%s'\n", instanceId);
      ret = 1;
    }
  }
  
  //rc = refresh_resources(ccMeta, OP_TIMEOUT - (time(NULL) - op_start));
  
  logprintfl(EUCADEBUG,"DetachVolume(): done.\n");
  
  shawn();
  
  return(ret);
}

int doConfigureNetwork(ncMetadata *meta, char *type, int namedLen, char **sourceNames, char **userNames, int netLen, char **sourceNets, char *destName, char *protocol, int minPort, int maxPort) {
  int rc, i, destVlan, slashnet, fail;
  char *destUserName;

  rc = init_config();
  if (rc) {
    return(1);
  }
  
  logprintfl(EUCADEBUG, "ConfigureNetwork(): called\n");
  
  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
    fail = 0;
  } else {
    
    destUserName = meta->userId;
    
    sem_wait(vnetConfigLock);
    
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
    sem_post(vnetConfigLock);
  }
  
  logprintfl(EUCADEBUG,"ConfigureNetwork(): done\n");
  
  if (fail) {
    return(1);
  }
  return(0);
}

int doFlushNetwork(ncMetadata *ccMeta, char *destName) {
  int rc;

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
    return(0);
  }

  sem_wait(vnetConfigLock);
  rc = vnetFlushTable(vnetconfig, ccMeta->userId, destName);
  sem_post(vnetConfigLock);
  return(rc);
}

int doAssignAddress(ncMetadata *ccMeta, char *src, char *dst) {
  int rc, allocated, addrdevno, ret;
  char cmd[256];
  ccInstance *myInstance=NULL;

  rc = init_config();
  if (rc) {
    return(1);
  }
  logprintfl(EUCADEBUG,"AssignAddress(): called\n");

  if (!src || !dst || !strcmp(src, "0.0.0.0") || !strcmp(dst, "0.0.0.0")) {
    logprintfl(EUCADEBUG, "AssignAddress(): bad input params\n");
    return(1);
  }
  
  ret = 0;

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
    ret = 0;
  } else {
    
    sem_wait(vnetConfigLock);
    rc = vnetGetPublicIP(vnetconfig, src, NULL, &allocated, &addrdevno);
    if (rc) {
      logprintfl(EUCAERROR,"failed to get publicip record %s\n", src);
      ret = 1;
    } else {
      if (!allocated) {
	snprintf(cmd, 255, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr add %s/32 dev %s", config->eucahome, src, vnetconfig->pubInterface);
	logprintfl(EUCAINFO,"running cmd %s\n", cmd);
	rc = system(cmd);
	if (rc) {
	  logprintfl(EUCAERROR,"cmd '%s' failed\n", cmd);
	  ret = 1;
	} else {
	  rc = vnetAssignAddress(vnetconfig, src, dst);
	  if (rc) {
	    logprintfl(EUCAERROR,"could not assign address\n");
	    ret = 1;
	  } else {
	    rc = vnetAllocatePublicIP(vnetconfig, src, dst);
	    if (rc) {
	      logprintfl(EUCAERROR,"could not allocate public IP\n");
	      ret = 1;
	    }
	  }
	}
      } else {
	logprintfl(EUCAWARN,"ip %s is allready assigned, ignoring\n", src);
	ret = 0;
      }
    }
    sem_post(vnetConfigLock);
  }
  
  if (!ret) {
    // everything worked, update instance cache
    rc = find_instanceCacheIP(dst, &myInstance);
    if (!rc) {
      snprintf(myInstance->ccnet.publicIp, 24, "%s", src);
      rc = refresh_instanceCache(myInstance->instanceId, myInstance);
      free(myInstance);
    }
  }
  logprintfl(EUCADEBUG,"AssignAddress(): done\n");  
  return(ret);
}

int doDescribePublicAddresses(ncMetadata *ccMeta, publicip **outAddresses, int *outAddressesLen) {
  int i, rc, count;
  
  rc = init_config();
  if (rc) {
    return(1);
  }
  
  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
    *outAddresses = vnetconfig->publicips;
    *outAddressesLen = NUMBER_OF_PUBLIC_IPS;
  } else {
    *outAddresses = NULL;
    *outAddressesLen = 0;
    return(2);
  }
  
  return(0);
}

int doUnassignAddress(ncMetadata *ccMeta, char *src, char *dst) {
  int rc, allocated, addrdevno, ret, count;
  char cmd[256];
  ccInstance *myInstance=NULL;

  rc = init_config();
  if (rc) {
    return(1);
  }
  logprintfl(EUCADEBUG,"UnassignAddress(): called\n");  
  
  if (!src || !dst || !strcmp(src, "0.0.0.0") || !strcmp(dst, "0.0.0.0")) {
    logprintfl(EUCADEBUG, "UnassignAddress(): bad input params\n");
    return(1);
  }

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
    ret = 0;
  } else {
    
    sem_wait(vnetConfigLock);
    ret=0;
    rc = vnetGetPublicIP(vnetconfig, src, NULL, &allocated, &addrdevno);
    if (rc) {
      logprintfl(EUCAERROR,"failed to find publicip to unassign (%s)\n", src);
      ret=1;
    } else {
      if (allocated && dst) {
	rc = vnetUnassignAddress(vnetconfig, src, dst); 
	if (rc) {
	  logprintfl(EUCAWARN,"vnetUnassignAddress() failed %d: %s/%s\n", rc, src, dst);
	}
	
	rc = vnetDeallocatePublicIP(vnetconfig, src, dst);
	if (rc) {
	  logprintfl(EUCAWARN,"vnetDeallocatePublicIP() failed %d: %s\n", rc, src);
	}
      }
      

      snprintf(cmd, 256, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del %s/32 dev %s", config->eucahome, src, vnetconfig->pubInterface);
      logprintfl(EUCADEBUG, "running cmd '%s'\n", cmd);
      rc = system(cmd);
      if (rc) {
      	logprintfl(EUCAWARN,"cmd failed '%s'\n", cmd);
      }
    }
    sem_post(vnetConfigLock);
  }

  if (!ret) {
    // refresh instance cache
    rc = find_instanceCacheIP(src, &myInstance);
    if (!rc) {
      snprintf(myInstance->ccnet.publicIp, 24, "0.0.0.0");
      rc = refresh_instanceCache(myInstance->instanceId, myInstance);
      free(myInstance);
    }
  }
  
  logprintfl(EUCADEBUG,"UnassignAddress(): done\n");  
  return(ret);
}

int doStopNetwork(ncMetadata *ccMeta, char *netName, int vlan) {
  int rc, ret;
  
  rc = init_config();
  if (rc) {
    return(1);
  }
  
  logprintfl(EUCADEBUG,"StopNetwork(): called\n");
  logprintfl(EUCADEBUG, "\t vlan:%d\n", vlan);

  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
    ret = 0;
  } else {
    
    sem_wait(vnetConfigLock);
    rc = vnetStopNetwork(vnetconfig, vlan, ccMeta->userId, netName);
    ret = rc;
    sem_post(vnetConfigLock);
  }
  
  logprintfl(EUCADEBUG,"StopNetwork(): done\n");
  
  return(ret);
}

int doStartNetwork(ncMetadata *ccMeta, char *netName, int vlan) {
  int rc, ret, i, status;
  time_t op_start, op_timer;
  char *brname;
  
  op_start = time(NULL);
  op_timer = OP_TIMEOUT;

  rc = init_config();
  if (rc) {
    return(1);
  }
  
  logprintfl(EUCADEBUG, "StartNetwork(): called\n");
  logprintfl(EUCADEBUG, "\t vlan:%d\n", vlan);
  if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
    ret = 0;
  } else {
    sem_wait(vnetConfigLock);
    brname = NULL;
    rc = vnetStartNetwork(vnetconfig, vlan, ccMeta->userId, netName, &brname);
    //    if (brname) {
    //      vnetAddDev(vnetconfig, brname);
    //    }
    
    sem_post(vnetConfigLock);
    
    if (rc) {
      logprintfl(EUCAERROR,"StartNetwork(): ERROR return from vnetStartNetwork %d\n", rc);
      ret = 1;
    } else {
      logprintfl(EUCAINFO,"StartNetwork(): SUCCESS return from vnetStartNetwork %d\n", rc);
      ret = 0;
    }
    
    sem_wait(configLock);
    
    for (i=0; i<config->numResources; i++) {
      int pid, j, numHosts, done, k;
      ncStub *ncs=NULL;
      char *statusString=NULL, **hosts;
      
      hosts = malloc(sizeof(char *));
      numHosts = 1;
      hosts[0] = strdup("localhost");
      
      pid = fork();
      if (pid == 0) {
	ncs = ncStubCreate(config->resourcePool[i].ncURL, NULL, NULL);
	if (config->use_wssec) {
	  rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
	}
	rc = ncStartNetworkStub(ncs, ccMeta, hosts, numHosts, 1234, vlan, &statusString);
	exit(rc);
      }
      op_timer = OP_TIMEOUT - (time(NULL) - op_start);
      rc = timewait(pid, &status, op_timer / (config->numResources - i));
      
      if (hosts != NULL) {
	for (j=0; j<numHosts; j++) {
	  if (hosts[j]) free(hosts[j]);
	}
	if (hosts) free(hosts);
      }
      
    }
    
    sem_post(configLock);
  }
  
  logprintfl(EUCADEBUG,"StartNetwork(): done\n");
  
  shawn();
  
  return(ret);
}

int doDescribeResources(ncMetadata *ccMeta, virtualMachine **ccvms, int vmLen, int **outTypesMax, int **outTypesAvail, int *outTypesLen, char ***outServiceTags, int *outServiceTagsLen) {
  int i;
  ncResource *ncRes;
  int rc, diskpool, mempool, corepool;
  int *numberOfTypes, j;
  resource *res;
  ncStub *ncs;
  axis2_svc_client_t *svc_client;  
  char *ptr;
  time_t op_start, op_timer;

  op_start = time(NULL);
  op_timer = OP_TIMEOUT;

  rc = init_config();
  if (rc) {
    return(1);
  }
  logprintfl(EUCADEBUG,"DescribeResources(): called %d\n", vmLen);
  
  if (outTypesMax == NULL || outTypesAvail == NULL || outTypesLen == NULL || outServiceTags == NULL || outServiceTagsLen == NULL) {
    // input error
    return(1);
  }
  
  print_instanceCache();

  *outServiceTags = malloc(sizeof(char *) * config->numResources);
  *outServiceTagsLen = config->numResources;
  for (i=0; i<config->numResources; i++) {
    (*outServiceTags)[i] = strdup(config->resourcePool[i].ncURL);
  }
  
  *outTypesMax = NULL;
  *outTypesAvail = NULL;
  
  *outTypesMax = malloc(sizeof(int) * vmLen);
  bzero(*outTypesMax, sizeof(int) * vmLen);

  *outTypesAvail = malloc(sizeof(int) * vmLen);
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
  
  rc = refresh_resources(ccMeta, OP_TIMEOUT - (time(NULL) - op_start));
  if (rc) {
    logprintfl(EUCAERROR,"calling refresh_resources\n");
  }

  sem_wait(configLock);
  {
    for (i=0; i<config->numResources; i++) {
      res = &(config->resourcePool[i]);
      
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
    sem_post(configLock);
  }

  logprintfl(EUCADEBUG,"DescribeResources(): done\n");
  
  shawn();

  return(0);
}

int refresh_resources(ncMetadata *ccMeta, int timeout) {
  int i, rc;
  int pid, status, ret;
  int filedes[2];  
  time_t op_start, op_timer;
  ncStub *ncs;
  ncResource *ncRes;

  if (timeout <= 0) timeout = 1;

  op_start = time(NULL);
  op_timer = timeout;
  logprintfl(EUCADEBUG,"refresh_resources(): called\n");

  sem_wait(configLock);
  for (i=0; i<config->numResources; i++) {
    rc = pipe(filedes);

    logprintfl(EUCADEBUG, "calling %s\n", config->resourcePool[i].ncURL);
    pid = fork();
    if (pid == 0) {
      close(filedes[0]);
      ncs = ncStubCreate(config->resourcePool[i].ncURL, NULL, NULL);
      if (config->use_wssec) {
	rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
      }
      rc = ncDescribeResourceStub(ncs, ccMeta, NULL, &ncRes);
      
      if (!rc) {
	rc = write(filedes[1], ncRes, sizeof(ncResource));
	ret = 0;
      } else {
	ret = 1;
      }
      close(filedes[1]);	  
      exit(ret);
    } else {
      fd_set rfds;
      struct timeval tv;
      
      close(filedes[1]);
      ncRes = malloc(sizeof(ncResource));
      bzero(ncRes, sizeof(ncResource));
      op_timer = timeout - (time(NULL) - op_start);
      logprintfl(EUCADEBUG, "\ttime left for next op: %d\n", op_timer);
      rc = timeread(filedes[0], ncRes, sizeof(ncResource), op_timer / (config->numResources - i));
      close(filedes[0]);
      if (rc <= 0) {
	// timeout or read went badly
	kill(pid, SIGKILL);
	wait(&status);
      } else {
	wait(&status);
	rc = WEXITSTATUS(status);
      }
    }
    
    config->lastResourceUpdate = time(NULL);
    if (rc != 0) {
      logprintfl(EUCAERROR,"bad return from ncDescribeResource(%s) (%d/%d)\n", config->resourcePool[i].hostname, pid, rc);
      config->resourcePool[i].maxMemory = 0;
      config->resourcePool[i].availMemory = 0;
      config->resourcePool[i].maxDisk = 0;
      config->resourcePool[i].availDisk = 0;
      config->resourcePool[i].maxCores = 0;
      config->resourcePool[i].availCores = 0;    
      config->resourcePool[i].isup = 0;
    } else {
      logprintfl(EUCAINFO,"\tnode=%s mem=%d/%d disk=%d/%d cores=%d/%d\n", config->resourcePool[i].hostname, ncRes->memorySizeMax, ncRes->memorySizeAvailable, ncRes->diskSizeMax,  ncRes->diskSizeAvailable, ncRes->numberOfCoresMax, ncRes->numberOfCoresAvailable);
      config->resourcePool[i].maxMemory = ncRes->memorySizeMax;
      config->resourcePool[i].availMemory = ncRes->memorySizeAvailable;
      config->resourcePool[i].maxDisk = ncRes->diskSizeMax;
      config->resourcePool[i].availDisk = ncRes->diskSizeAvailable;
      config->resourcePool[i].maxCores = ncRes->numberOfCoresMax;
      config->resourcePool[i].availCores = ncRes->numberOfCoresAvailable;    
      config->resourcePool[i].isup = 1;
      if (ncRes) free(ncRes);
    }
  }
  sem_post(configLock);

  logprintfl(EUCADEBUG,"refresh_resources(): done\n");
  return(0);
}

int doDescribeInstances(ncMetadata *ccMeta, char **instIds, int instIdsLen, ccInstance **outInsts, int *outInstsLen) {
  ccInstance *myInstance=NULL, *out=NULL, *cacheInstance=NULL;
  int i, j, k, numInsts, found, ncOutInstsLen, rc, pid;
  virtualMachine ccvm;
  time_t op_start, op_timer;

  ncInstance **ncOutInsts=NULL;
  ncStub *ncs;

  op_start = time(NULL);
  op_timer = OP_TIMEOUT;

  rc = init_config();
  if (rc) {
    return(1);
  }
  logprintfl(EUCADEBUG, "printing instance cache in describeInstances()\n");
  print_instanceCache();

  logprintfl(EUCADEBUG,"DescribeInstances(): called\n");

  *outInsts = NULL;
  out = *outInsts;

  *outInstsLen = 0;
  numInsts=0;

  sem_wait(configLock);  
  for (i=0; i<config->numResources; i++) {
    if (1) {
      int status, ret;
      int filedes[2];
      int len, j;
      
      rc = pipe(filedes);
      pid = fork();
      if (pid == 0) {
	close(filedes[0]);
	ncs = ncStubCreate(config->resourcePool[i].ncURL, NULL, NULL);
	if (config->use_wssec) {
	  rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
	}
	rc = ncDescribeInstancesStub(ncs, ccMeta, instIds, instIdsLen, &ncOutInsts, &ncOutInstsLen);
	
    	if (!rc) {
	  len = ncOutInstsLen;
	  rc = write(filedes[1], &len, sizeof(int));
	  for (j=0; j<len; j++) {
	    ncInstance *inst;
	    inst = ncOutInsts[j];
	    rc = write(filedes[1], inst, sizeof(ncInstance));
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
	ncInstance *inst;
	close(filedes[1]);
	
	op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	rbytes = timeread(filedes[0], &len, sizeof(int), op_timer / (config->numResources - i));
	if (rbytes <= 0) {
	  // read went badly
	  kill(pid, SIGKILL);
	  wait(&status);
	  rc = -1;
	} else {
	  if (rbytes < sizeof(int)) {
	    len = 0;
	    ncOutInsts = NULL;
	    ncOutInstsLen = 0;
	  } else {
	    ncOutInsts = malloc(sizeof(ncInstance *) * len);
	    ncOutInstsLen = len;
	    for (j=0; j<len; j++) {
	      inst = malloc(sizeof(ncInstance));
	      op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	      rbytes = timeread(filedes[0], inst, sizeof(ncInstance), op_timer / (config->numResources - i));
	      ncOutInsts[j] = inst;
	    }
	  }
	  wait(&status);
	  rc = WEXITSTATUS(status);
	}
	close(filedes[0]);
      }
    }
    
    if (rc != 0) {
      logprintfl(EUCAERROR,"ncDescribeInstancesStub(%s): returned fail: (%d/%d)\n", config->resourcePool[i].ncURL, pid, rc);
    } else {
      for (j=0; j<ncOutInstsLen; j++) {
	found=0;
	for (k=0; k<instIdsLen; k++) {
	  if (!strcmp(ncOutInsts[j]->instanceId, instIds[k]) && (!strcmp(ncOutInsts[j]->userId, ccMeta->userId) || !strcmp(ccMeta->userId, SUPERUSER))) {
	    found=1;
	    k=instIdsLen;
	  }
	}
	if (found || instIdsLen == 0) {
	  // add it
	  logprintfl(EUCAINFO,"DescribeInstances(): describing instance %s, %d\n", ncOutInsts[j]->instanceId, j);
	  numInsts++;
	  
	  *outInsts = realloc(*outInsts, sizeof(ccInstance) * numInsts);
	  out = *outInsts;
	  
	  // ccvm.name = TODO
	  bzero(ccvm.name, 64);
	  ccvm.mem = ncOutInsts[j]->params.memorySize;
	  ccvm.disk = ncOutInsts[j]->params.diskSize;
	  ccvm.cores = ncOutInsts[j]->params.numberOfCores;
	  
	  myInstance = &(out[numInsts-1]);
	  bzero(myInstance, sizeof(ccInstance));
	  //	  strncpy(myInstance->instanceId, ncOutInsts[j]->instanceId, 16);
	  cacheInstance=NULL;
	  find_instanceCacheId(ncOutInsts[j]->instanceId, &cacheInstance);
	  if (cacheInstance) {
	    logprintfl(EUCADEBUG, "\t%s in cache\n", ncOutInsts[j]->instanceId);
	    memcpy(myInstance, cacheInstance, sizeof(ccInstance));
	  }
	  
	  rc = ccInstance_to_ncInstance(myInstance, ncOutInsts[j]);

	  /*
	  strncpy(myInstance->keyName, ncOutInsts[j]->keyName, 1024);
	  strncpy(myInstance->ownerId, ncOutInsts[j]->userId, 16);
	  strncpy(myInstance->reservationId, ncOutInsts[j]->reservationId, 16);  
	  strncpy(myInstance->amiId, ncOutInsts[j]->imageId, 16);
	  strncpy(myInstance->kernelId, ncOutInsts[j]->kernelId, 16);
	  strncpy(myInstance->ramdiskId, ncOutInsts[j]->ramdiskId, 16);
	  strncpy(myInstance->state, ncOutInsts[j]->stateName, 16);
	  myInstance->ts = ncOutInsts[j]->launchTime;
	  
	  myInstance->ccnet.vlan = ncOutInsts[j]->ncnet.vlan;
	  strncpy(myInstance->ccnet.publicIp, ncOutInsts[j]->ncnet.publicIp, 24);
	  strncpy(myInstance->ccnet.privateIp, ncOutInsts[j]->ncnet.privateIp, 24);
	  strncpy(myInstance->ccnet.publicMac, ncOutInsts[j]->ncnet.publicMac, 24);
	  strncpy(myInstance->ccnet.privateMac, ncOutInsts[j]->ncnet.privateMac, 24);
	  */

	  // instance info that the CC maintains
	  myInstance->ncHostIdx = i;
	  strncpy(myInstance->serviceTag, config->resourcePool[i].ncURL, 64);
	  memcpy(&(myInstance->ccvm), &ccvm, sizeof(virtualMachine));
	  
	  /*
	    if (cacheInstance) {
	    // see if I remember the IPs
	    memcpy(&(myInstance->ccvm), &(cacheInstance->ccvm), sizeof(virtualMachine));
	    
	    if (!strcmp(myInstance->ccnet.publicIp, "0.0.0.0") && strcmp(cacheInstance->ccnet.publicIp, "0.0.0.0")) {
	      // found cached publicIp
	      strncpy(myInstance->ccnet.publicIp, cacheInstance->ccnet.publicIp, 24);
	    }
	    if (!strcmp(myInstance->ccnet.privateIp, "0.0.0.0") && strcmp(cacheInstance->ccnet.privateIp, "0.0.0.0")) {
	      // found cached publicIp
	      strncpy(myInstance->ccnet.privateIp, cacheInstance->ccnet.privateIp, 24);
	    }
	  }
	  */
	  
	  {
	    char *ip;
	    
	    if (!strcmp(myInstance->ccnet.publicIp, "0.0.0.0")) {
	      if (!strcmp(vnetconfig->mode, "SYSTEM") || !strcmp(vnetconfig->mode, "STATIC")) {
		rc = discover_mac(vnetconfig, myInstance->ccnet.publicMac, &ip);
		if (!rc) {
		  strncpy(myInstance->ccnet.publicIp, ip, 24);
		}
	      }
	    }
	    if (!strcmp(myInstance->ccnet.privateIp, "0.0.0.0")) {
	      rc = discover_mac(vnetconfig, myInstance->ccnet.privateMac, &ip);
	      if (!rc) {
		strncpy(myInstance->ccnet.privateIp, ip, 24);
	      }
	    }
	  }
	  if (cacheInstance) free(cacheInstance);
	  
	  refresh_instanceCache(myInstance->instanceId, myInstance);
	}
      }
      for (j=0; j<ncOutInstsLen; j++) {
	free_instance(&(ncOutInsts[j]));
      }
      if (ncOutInsts) free(ncOutInsts);
    }
  }
  sem_post(configLock);
  
  *outInstsLen = numInsts;
  logprintfl(EUCADEBUG,"DescribeInstances(): done\n");

  shawn();
      
  return(0);
}

int ccInstance_to_ncInstance(ccInstance *dst, ncInstance *src) {
  int i;
  
  strncpy(dst->instanceId, src->instanceId, 16);
  strncpy(dst->reservationId, src->reservationId, 16);
  strncpy(dst->ownerId, src->userId, 16);
  strncpy(dst->amiId, src->imageId, 16);
  strncpy(dst->kernelId, src->kernelId, 16);
  strncpy(dst->ramdiskId, src->ramdiskId, 16);
  strncpy(dst->launchIndex, src->launchIndex, 64);
  strncpy(dst->userData, src->userData, 64);
  for (i=0; i<src->groupNamesSize || i >= 64; i++) {
    snprintf(dst->groupNames[i], 32, "%s", src->groupNames[i]);
  }
  strncpy(dst->state, src->stateName, 16);
  dst->ccnet.vlan = src->ncnet.vlan;
  strncpy(dst->ccnet.publicMac, src->ncnet.publicMac, 24);
  strncpy(dst->ccnet.privateMac, src->ncnet.privateMac, 24);
  if (strcmp(src->ncnet.publicIp, "0.0.0.0") || dst->ccnet.publicIp[0] == '\0') strncpy(dst->ccnet.publicIp, src->ncnet.publicIp, 16);
  if (strcmp(src->ncnet.privateIp, "0.0.0.0") || dst->ccnet.privateIp[0] == '\0') strncpy(dst->ccnet.privateIp, src->ncnet.privateIp, 16);

  memcpy(dst->volumes, src->volumes, sizeof(ncVolume) * EUCA_MAX_VOLUMES);
  dst->volumesSize = src->volumesSize;

  return(0);
}

int schedule_instance(virtualMachine *vm, int *outresid) {
  
  if (config->schedPolicy == SCHEDGREEDY) {
    return(schedule_instance_greedy(vm, outresid));
  } else if (config->schedPolicy == SCHEDROUNDROBIN) {
    return(schedule_instance_roundrobin(vm, outresid));
  }
  
  return(schedule_instance_greedy(vm, outresid));
}

int schedule_instance_roundrobin(virtualMachine *vm, int *outresid) {
  int i, rc, done, start, found, resid=0;
  resource *res;

  *outresid = 0;

  logprintfl(EUCAINFO, "scheduler using ROUNDROBIN policy to find next resource\n");

  // find the best 'resource' on which to run the instance
  done=found=0;
  start = config->schedState;
  i = start;
  
  logprintfl(EUCADEBUG, "scheduler state starting at resource %d\n", config->schedState);
  while(!done) {
    int mem, disk, cores;
    
    res = &(config->resourcePool[i]);
    if (res->isup) {
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
    if (i >= config->numResources) {
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
  logprintfl(EUCADEBUG, "scheduler state finishing at resource %d\n", config->schedState);

  return(0);
}

int schedule_instance_greedy(virtualMachine *vm, int *outresid) {
  int i, rc, done, resid=0;
  resource *res;

  *outresid = 0;

  logprintfl(EUCAINFO, "scheduler using GREEDY policy to find next resource\n");

  // find the best 'resource' on which to run the instance
  done=0;
  for (i=0; i<config->numResources && !done; i++) {
    int mem, disk, cores;
    
    // new fashion way
    res = &(config->resourcePool[i]);
    if (res->isup) {
      mem = res->availMemory - vm->mem;
      disk = res->availDisk - vm->disk;
      cores = res->availCores - vm->cores;
      
      if (mem >= 0 && disk >= 0 && cores >= 0) {
	resid = i;
	done++;
      }
    }
  }
  
  if (!done) {
    // didn't find a resource
    return(1);
  }
  *outresid = resid;
  return(0);
}

int doRunInstances(ncMetadata *ccMeta, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char **instIds, int instIdsLen, char **netNames, int netNamesLen, char **macAddrs, int macAddrsLen, int minCount, int maxCount, char *ownerId, char *reservationId, virtualMachine *ccvm, char *keyName, int vlan, char *userData, char *launchIndex, ccInstance **outInsts, int *outInstsLen) {
  int rc, i, j, done, runCount, resid, foundnet=0, error=0;
  ccInstance *myInstance=NULL, 
    *retInsts=NULL;
  char *instId=NULL, 
    *brname=NULL;
  time_t op_start, op_timer;
  resource *res;
  char mac[32], privip[32], pubip[32];

  ncInstance *outInst=NULL;
  ncInstParams ncvm;
  ncStub *ncs=NULL;

  op_start = time(NULL);
  op_timer = OP_TIMEOUT;
  
  rc = init_config();
  if (rc) {
    return(1);
  }
  logprintfl(EUCADEBUG,"RunInstances(): called\n");
  
  *outInstsLen = 0;
  
  if (minCount <= 0 || maxCount <= 0 || instIdsLen < maxCount) {
    logprintfl(EUCAERROR,"RunInstances(): bad min or max count, or not enough instIds (%d, %d, %d)\n", minCount, maxCount, instIdsLen);
    return(-1);
  }
  
  retInsts = malloc(sizeof(ccInstance) * maxCount);  
  runCount=0;
  
  // get updated resource information
  rc = refresh_resources(ccMeta, OP_TIMEOUT - (time(NULL) - op_start));
  
  done=0;
  for (i=0; i<maxCount && !done; i++) {
    logprintfl(EUCAINFO,"\trunning instance %d with emiId %s...\n", i, amiId);
    
    // generate new mac
    bzero(mac, 32);
    bzero(pubip, 32);
    bzero(privip, 32);
    
    strncpy(pubip, "0.0.0.0", 32);
    strncpy(privip, "0.0.0.0", 32);
    strncpy(mac, macAddrs[i], 32);
    
    sem_wait(vnetConfigLock);
    
    // define/get next mac and allocate IP
    foundnet = 0;
    if (!strcmp(vnetconfig->mode, "STATIC")) {
      // get the next valid mac/ip pairing for this vlan
      bzero(mac, 32);
      rc = vnetGetNextHost(vnetconfig, mac, privip, 0);
      if (!rc) {
	snprintf(pubip, 32, "%s", privip);
	foundnet = 1;
      }
    } else if (!strcmp(vnetconfig->mode, "SYSTEM")) {
      foundnet = 1;
    } else if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
      
      // add the mac address to the virtual network
      rc = vnetAddHost(vnetconfig, mac, NULL, vlan);
      if (!rc) {
	// get the next valid mac/ip pairing for this vlan
	rc = vnetGetNextHost(vnetconfig, mac, privip, vlan);
	if (!rc) {
	  foundnet = 1;
	}
      }
    }
    sem_post(vnetConfigLock);
    
    logprintfl(EUCAINFO,"\tassigning MAC/IP: %s/%s/%s\n", mac, pubip, privip);

    if (mac[0] == '\0' || !foundnet) {
      logprintfl(EUCAERROR,"could not find/initialize any free network address, failing doRunInstances()\n");
    } else {
      // "run" the instance
      instId = strdup(instIds[i]);
      ncvm.memorySize = ccvm->mem;
      ncvm.diskSize = ccvm->disk;
      ncvm.numberOfCores = ccvm->cores;
      
      sem_wait(configLock);
      
      resid = 0;
      rc = schedule_instance(ccvm, &resid);
      res = &(config->resourcePool[resid]);
      if (rc) {
	// could not find resource
	logprintfl(EUCAERROR, "scheduler could not find resource to run the instance on\n");
	// couldn't run this VM, remove networking information from system
	sem_wait(vnetConfigLock);
	
	vnetDisableHost(vnetconfig, mac, NULL, 0);
	if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
	  vnetDelHost(vnetconfig, mac, NULL, vlan);
	}
	
	sem_post(vnetConfigLock);
      } else {
	int pid, status, ret, rbytes;
	int filedes[2];
	
	// try to run the instance on the chosen resource
	logprintfl(EUCAINFO, "\tscheduler decided to run instance '%s' on resource '%s'\n", instId, res->ncURL);
	outInst=NULL;
	
	rc = pipe(filedes);
	pid = fork();
	if (pid == 0) {
	  close(filedes[0]);
	  ncs = ncStubCreate(res->ncURL, NULL, NULL);
	  if (config->use_wssec) {
	    rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
	  }
	  logprintfl(EUCAINFO,"\tclient (%s) running instance: %s %s %s %s %d %s\n", res->ncURL, instId, amiId, mac, mac, vlan, keyName);
	  logprintfl(EUCAINFO,"\tasking for virtual hardware (mem/disk/cores): %d/%d/%d\n", ncvm.memorySize, ncvm.diskSize, ncvm.numberOfCores);
	  rc = ncRunInstanceStub(ncs, ccMeta, instId, reservationId, &ncvm, amiId, amiURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, mac, mac, vlan, userData, launchIndex, netNames, netNamesLen, &outInst);
	  if (!rc) {
	    rc = write(filedes[1], outInst, sizeof(ncInstance));
	    ret = 0;
	  } else {
	    ret = 1;
	  }
	  close(filedes[1]);	  
	  exit(ret);
	} else {
	  close(filedes[1]);
	  outInst = malloc(sizeof(ncInstance));
	  op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	  logprintfl(EUCADEBUG, "\ttime left for op: %d\n", op_timer / (maxCount - i));
	  rbytes = timeread(filedes[0], outInst, sizeof(ncInstance), op_timer / (maxCount - i));
	  close(filedes[0]);
	  if (rbytes <= 0) {
	    // read went badly
	    kill(pid, SIGKILL);
	    wait(&status);
	    rc = -1;
	  } else {
	    wait(&status);
	    rc = WEXITSTATUS(status);
	  }
	  logprintfl(EUCAINFO,"\tcall complete (pid/rc): %d/%d\n", pid, rc);
	}
	if (rc != 0) {
	  // problem
	  logprintfl(EUCAERROR, "tried to run the VM, but runInstance() failed; marking resource '%s' as down\n", res->ncURL);
	  res->isup = 0;
	  i--;
	  // couldn't run this VM, remove networking information from system
	  sem_wait(vnetConfigLock);
	  vnetDisableHost(vnetconfig, mac, NULL, 0);
	  if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
	    vnetDelHost(vnetconfig, mac, NULL, vlan);
	  }
	  sem_post(vnetConfigLock);
	} else {
	  res->availMemory -= ccvm->mem;
	  res->availDisk -= ccvm->disk;
	  res->availCores -= ccvm->cores;
	  
	  myInstance = &(retInsts[runCount]);
	  
	  bzero(myInstance, sizeof(ccInstance));
	  
	  // stuff from NC
	  rc = ccInstance_to_ncInstance(myInstance, outInst);
	  
	  // instance info that CC has
	  myInstance->ts = time(NULL);
	  if (strcmp(pubip, "0.0.0.0")) {
	    strncpy(myInstance->ccnet.publicIp, pubip, 16);
	  }
	  if (strcmp(privip, "0.0.0.0")) {
	    strncpy(myInstance->ccnet.privateIp, privip, 16);
	  }
	  myInstance->ncHostIdx = resid;
	  if (ccvm) memcpy(&(myInstance->ccvm), ccvm, sizeof(virtualMachine));
	  if (config->resourcePool[resid].ncURL) strncpy(myInstance->serviceTag, config->resourcePool[resid].ncURL, 64);
	  
	  // start up DHCP
	  rc = vnetKickDHCP(vnetconfig);
	  if (rc) {
	    logprintfl(EUCAERROR, "cannot start DHCP daemon, for instance %s please check your network settings\n", myInstance->instanceId);
	  }
	  
	  // add the instance to the cache, and continue on
	  add_instanceCache(myInstance->instanceId, myInstance);
	  free_instance(&outInst);
	  runCount++;
	}
      }
      sem_post(configLock);
    }
  }
  *outInstsLen = runCount;
  *outInsts = retInsts;
  
  logprintfl(EUCADEBUG,"RunInstances(): done\n");
  
  shawn();
  if (instId) free(instId);
  if (error) {
    return(1);
  }
  return(0);
}

int doGetConsoleOutput(ncMetadata *meta, char *instId, char **outConsoleOutput) {
  int i, j, rc, numInsts, start, stop, k, done, ret;
  ccInstance *myInstance;
  ncStub *ncs;
  char *consoleOutput;
  time_t op_start, op_timer;

  i = j = numInsts = 0;
  op_start = time(NULL);
  op_timer = OP_TIMEOUT;

  myInstance = NULL;
  
  *outConsoleOutput = NULL;

  rc = init_config();
  if (rc) {
    return(1);
  }

  logprintfl(EUCADEBUG,"GetConsoleOutput(): called\n");
  
  rc = find_instanceCacheId(instId, &myInstance);
  if (!rc) {
    // found the instance in the cache
    start = myInstance->ncHostIdx;
    stop = start+1;      
    free(myInstance);
  } else {
    start = 0;
    stop = config->numResources;
  }
  
  sem_wait(configLock);
  done=0;
  for (j=start; j<stop && !done; j++) {
    // read the instance ids
    logprintfl(EUCAINFO,"getConsoleOutput(): calling GetConsoleOutput for instance (%s) on (%s)\n", instId, config->resourcePool[j].hostname);
    if (1) {
      int pid, status, ret, rbytes, len;
      int filedes[2];
      rc = pipe(filedes);
      pid = fork();
      if (pid == 0) {
	close(filedes[0]);
	ncs = ncStubCreate(config->resourcePool[j].ncURL, NULL, NULL);
	if (config->use_wssec) {
	  rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
	}

	rc = ncGetConsoleOutputStub(ncs, meta, instId, &consoleOutput);
	if (!rc && consoleOutput) {
	  len = strlen(consoleOutput) + 1;
	  rc = write(filedes[1], &len, sizeof(int));
	  rc = write(filedes[1], consoleOutput, sizeof(char) * len);
	  ret = 0;
	} else {
	  len = 0;
	  rc = write(filedes[1], &len, sizeof(int));
	  ret = 1;
	}
	close(filedes[1]);	  
	exit(ret);
      } else {
	close(filedes[1]);
	op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	rbytes = timeread(filedes[0], &len, sizeof(int), op_timer / ((stop-start) - (j - start)));
	if (rbytes <= 0) {
	  // read went badly
	  kill(pid, SIGKILL);
	  wait(&status);
	  rc = -1;
	} else {
	  consoleOutput = malloc(sizeof(char) * len);
	  op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	  rbytes = timeread(filedes[0], consoleOutput, len, op_timer / ((stop-start) - (j-start)));
	  if (rbytes <= 0) {
	    // read went badly
	    kill(pid, SIGKILL);
	    wait(&status);
	    rc = -1;
	  } else {
	    wait(&status);
	    rc = WEXITSTATUS(status);
	  }
	}
	close(filedes[0]);
	
	logprintfl(EUCAINFO,"\tcall complete (pid/rc): %d/%d\n", pid, rc);
	if (!rc) {
	  done++;
	} else {
	  if (consoleOutput) {
	    free(consoleOutput);
	    consoleOutput = NULL;
	  }
	}
      }
    }
  }
  sem_post(configLock);
  
  logprintfl(EUCADEBUG,"GetConsoleOutput(): done.\n");
  
  shawn();
  
  if (consoleOutput) {
    *outConsoleOutput = strdup(consoleOutput);
    ret = 0;
  } else {
    *outConsoleOutput = NULL;
    ret = 1;
  }
  if (consoleOutput) free(consoleOutput);
  return(ret);
}

int doRebootInstances(ncMetadata *meta, char **instIds, int instIdsLen) {
  int i, j, rc, numInsts, start, stop, k, done, ret;
  char *instId;
  ccInstance *myInstance, *out;
  ncStub *ncs;
  time_t op_start, op_timer;

  i = j = numInsts = 0;
  instId = NULL;
  myInstance = NULL;
  op_start = time(NULL);
  op_timer = OP_TIMEOUT;

  rc = init_config();
  if (rc) {
    return(1);
  }
  logprintfl(EUCADEBUG,"RebootInstances(): called\n");
  
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
      stop = config->numResources;
    }
    
    sem_wait(configLock);
    done=0;
    for (j=start; j<stop && !done; j++) {
      // read the instance ids
      logprintfl(EUCAINFO,"RebootInstances(): calling reboot instance (%s) on (%s)\n", instId, config->resourcePool[j].hostname);
      if (1) {
	int pid, status, ret, rbytes;
	pid = fork();
	if (pid == 0) {
	  ncs = ncStubCreate(config->resourcePool[j].ncURL, NULL, NULL);
	  if (config->use_wssec) {
	    rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
	  }
	  
	  rc = 0;
	  rc = ncRebootInstanceStub(ncs, meta, instId);
	  
	  if (!rc) {
	    ret = 0;
	  } else {
	    ret = 1;
	  }
	  exit(ret);
	} else {
	  op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	  rc = timewait(pid, &status, op_timer / ((stop-start) - (j-start)));
	  rc = WEXITSTATUS(status);
	  logprintfl(EUCAINFO,"\tcall complete (pid/rc): %d/%d\n", pid, rc);
	}
      }
      sem_post(configLock);
      
      if (!rc) {
	done++;
      }
    }
  }
  
  logprintfl(EUCADEBUG,"RebootInstances(): done.\n");

  shawn();

  return(0);
}

int doTerminateInstances(ncMetadata *ccMeta, char **instIds, int instIdsLen, int **outStatus) {
  int i, j, shutdownState, previousState, rc, start, stop, k, done;
  char *instId;
  ccInstance *myInstance, *out;
  ncStub *ncs;
  time_t op_start, op_timer;

  i = j = 0;
  instId = NULL;
  myInstance = NULL;
  op_start = time(NULL);
  op_timer = OP_TIMEOUT;

  rc = init_config();
  if (rc) {
    return(1);
  }
  logprintfl(EUCADEBUG,"TerminateInstances(): called\n");
  
  for (i=0; i<instIdsLen; i++) {
    instId = instIds[i];
    rc = find_instanceCacheId(instId, &myInstance);
    if (!rc) {
      // found the instance in the cache
      start = myInstance->ncHostIdx;
      stop = start+1;
      
      // remove private network info from system
      sem_wait(vnetConfigLock);
      
      vnetDisableHost(vnetconfig, myInstance->ccnet.privateMac, NULL, 0);
      if (!strcmp(vnetconfig->mode, "MANAGED") || !strcmp(vnetconfig->mode, "MANAGED-NOVLAN")) {
	vnetDelHost(vnetconfig, myInstance->ccnet.privateMac, NULL, myInstance->ccnet.vlan);
      }
      
      sem_post(vnetConfigLock);
      
      if (myInstance) free(myInstance);
    } else {
      start = 0;
      stop = config->numResources;
    }
    
    sem_wait(configLock);
    for (j=start; j<stop; j++) {
      // read the instance ids

      logprintfl(EUCAINFO,"TerminateInstances(): calling terminate instance (%s) on (%s)\n", instId, config->resourcePool[j].hostname);
      if (1) {
	int pid, status, ret, rbytes;
	int filedes[2];
	rc = pipe(filedes);
	pid = fork();
	if (pid == 0) {
	  close(filedes[0]);
	  ncs = ncStubCreate(config->resourcePool[j].ncURL, NULL, NULL);
	  if (config->use_wssec) {
	    rc = InitWSSEC(ncs->env, ncs->stub, config->policyFile);
	  }
	  rc = ncTerminateInstanceStub(ncs, ccMeta, instId, &shutdownState, &previousState);
	  
	  if (!rc) {
	    ret = 0;
	  } else {
	    ret = 1;
	  }
	  close(filedes[1]);	  
	  exit(ret);
	} else {
	  close(filedes[1]);
	  close(filedes[0]);
	  
	  op_timer = OP_TIMEOUT - (time(NULL) - op_start);
	  rc = timewait(pid, &status, op_timer / ((stop-start) - (j - start)));
	  rc = WEXITSTATUS(status);
	  logprintfl(EUCADEBUG,"\tcall complete (pid/rc): %d/%d\n", pid, rc);
	}
      }
      sem_post(configLock);

      if (!rc) {
	del_instanceCacheId(instId);
	(*outStatus)[i] = 1;
	logprintfl(EUCAWARN, "failed to terminate '%s': instance may not exist any longer\n", instId);
      } else {
	(*outStatus)[i] = 0;
      }
    }
  }
  
  rc = refresh_resources(ccMeta, OP_TIMEOUT - (time(NULL) - op_start));
  
  logprintfl(EUCADEBUG,"TerminateInstances(): done.\n");
  
  shawn();

  return(0);
}

int setup_shared_buffer(void **buf, char *bufname, size_t bytes, sem_t **lock, char *lockname) {
  int shd, rc;
  sem_t *thelock;
  
  // create a lock and grab it
  *lock = sem_open(lockname, O_CREAT, 0644, 1);    
  sem_wait(*lock);
  
  // set up shared memory segment for config
  shd = shm_open(bufname, O_CREAT | O_RDWR | O_EXCL, 0644);
  if (shd >= 0) {
    // if this is the first process to create the config, init to 0
    rc = ftruncate(shd, bytes);
  } else {
    shd = shm_open(bufname, O_CREAT | O_RDWR, 0644);
  }
  if (shd < 0) {
    printf("cannot initialize shared memory segment\n");
    sem_post(*lock);
    sem_close(*lock);
    return(1);
  }
  *buf = mmap(0, bytes, PROT_READ | PROT_WRITE, MAP_SHARED, shd, 0);
  sem_post(*lock);
  return(0);
}

int sem_timepost(sem_t *sem) {
  int rc;
  rc = sem_post(sem);
  if (rc == 0) {
    //    sem_getvalue(sem, &rc);
    //    logprintfl(EUCADEBUG, "dropped sem %d %d %08X\n", getpid(), rc, sem);
  }
  return(rc);
}

int sem_timewait(sem_t *sem, time_t seconds) {
  int rc;
  struct timespec to;

  to.tv_sec = time(NULL) + seconds + 1;
  to.tv_nsec = 0;
  
  rc = sem_timedwait(sem, &to);
  if (rc < 0) {
    perror("SEM");
    logprintfl(EUCAERROR, "timeout waiting for semaphore\n");
  } else {
  }
  return(rc);
}

int init_config(void) {
  resource *res=NULL;
  char *tmpstr=NULL, **hosts=NULL, *hostname=NULL, *ncservice=NULL, *dhcp_deamon;
  int ncport, rd, shd, val, rc, i, numHosts, tcount, use_wssec, loglevel, schedPolicy;
  
  char configFile[1024], netPath[1024], logFile[1024], eucahome[1024], policyFile[1024], buf[1024], *home=NULL, cmd[256];
  
  axutil_env_t *env = NULL;
  FILE *FH=NULL;
  time_t configMtime;
  struct stat statbuf;

  // read in base config information
  home = strdup(getenv(EUCALYPTUS_ENV_VAR_NAME));
  if (!home) {
    home = strdup("");
  }
  
  bzero(configFile, 1024);
  bzero(netPath, 1024);
  bzero(logFile, 1024);
  bzero(policyFile, 1024);

  snprintf(configFile, 1024, EUCALYPTUS_CONF_LOCATION, home);
  snprintf(netPath, 1024, CC_NET_PATH_DEFAULT, home);
  snprintf(logFile, 1024, "%s/var/log/eucalyptus/cc.log", home);
  snprintf(policyFile, 1024, "%s/var/lib/eucalyptus/keys/nc-client-policy.xml", home);
  snprintf(eucahome, 1024, "%s/", home);
  free(home);

  if (init) {
    // this means that this thread has already been initialized
    // check to see if the configfile has changed
    rc = stat(configFile, &statbuf);
    if (rc) {
      logprintfl(EUCAERROR, "cannot stat configfile '%s'\n", configFile);
      return(1);
    } 
    configMtime = statbuf.st_mtime;
    
    if (config->configMtime != configMtime) {
      // something has changed
      config->configMtime = configMtime;
      
      logprintfl(EUCAINFO, "config file has been modified, refreshing node list\n");
      res = NULL;
      rc = refreshNodes(config, configFile, &res, &numHosts);
      if (rc) {
	logprintfl(EUCAERROR, "cannot read list of nodes, check your config file\n");
	sem_wait(configLock);
	config->numResources = 0;
	bzero(config->resourcePool, sizeof(resource) * MAXNODES);
	sem_post(configLock);

	return(1);
      }
      
      sem_wait(configLock);
      config->numResources = numHosts;
      memcpy(config->resourcePool, res, sizeof(resource) * numHosts);
      if (res) free(res);
      sem_post(configLock);
    }

    return(0);
  }
  
  // this thread has not been initialized, set up shared memory segments
  initLock = sem_open("/eucalyptusCCinitLock", O_CREAT, 0644, 1);    
  sem_wait(initLock);

  if (config == NULL) {
    rc = setup_shared_buffer((void **)&config, "/eucalyptusCCConfig", sizeof(ccConfig), &configLock, "/eucalyptusCCConfigLock");
    if (rc != 0) {
      fprintf(stderr, "Cannot set up shared memory region for ccConfig, exiting...\n");
      sem_post(initLock);
      exit(1);
    }
  }
  
  if (instanceCache == NULL) {
    rc = setup_shared_buffer((void **)&instanceCache, "/eucalyptusCCInstanceCache", sizeof(ccInstance) * MAXINSTANCES, &instanceCacheLock, "/eucalyptusCCInstanceCacheLock");
    if (rc != 0) {
      fprintf(stderr, "Cannot set up shared memory region for ccInstanceCache, exiting...\n");
      sem_post(initLock);
      exit(1);
    }
  }
  
  if (vnetconfig == NULL) {
    rc = setup_shared_buffer((void **)&vnetconfig, "/eucalyptusCCVNETConfig", sizeof(vnetConfig), &vnetConfigLock, "/eucalyptusCCVNETConfigLock");
    if (rc != 0) {
      fprintf(stderr, "Cannot set up shared memory region for ccVNETConfig, exiting...\n");
      sem_post(initLock);
      exit(1);
    }
  }
  sem_post(initLock);
  srand(time(NULL));

  rc = stat(configFile, &statbuf);
  if (rc) {
    fprintf(stderr, "ERROR: cannot stat configfile '%s'\n", configFile);
    exit(1);
  }
  configMtime = statbuf.st_mtime;
  
  // now start reading the config file
  rc = get_conf_var(configFile, "LOGLEVEL", &tmpstr);
  if (rc != 1) {
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
  
  if (config->initialized) {
    // some other thread has already initialized the configuration
    logprintfl(EUCAINFO, "init(): another thread has already set up config\n");
    logprintfl(EUCADEBUG, "printing instance cache in init_config()\n");
    print_instanceCache();
    rc = restoreNetworkState();
    if (rc) {
      // error
    }

    init = 1;
    return(0);
  }
  
  logprintfl(EUCADEBUG,"init_config(): initializing CC configutation\n");  
  
  // DHCP configuration section
  {
    char *daemon=NULL,
      *dhcpuser=NULL,
      *numaddrs=NULL,
      *pubmode=NULL,
      *pubmacmap=NULL,
      *pubips=NULL,
      *pubInterface=NULL,
      *pubSubnet=NULL,
      *pubSubnetMask=NULL,
      *pubBroadcastAddress=NULL,
      *pubRouter=NULL,
      *pubDNS=NULL,
      *pubRangeMin=NULL,
      *pubRangeMax=NULL,
      *privSubnet=NULL,
      *privSubnetMask=NULL,
      *privBroadcastAddress=NULL;
    int initFail=0;
    
    // DHCP Daemon Configuration Params
    daemon = getConfString(configFile, "VNET_DHCPDAEMON");
    if (!daemon) {
      logprintfl(EUCAWARN,"no VNET_DHCPDAEMON defined in config, using default\n");
      daemon = NULL;
    }

    dhcpuser = getConfString(configFile, "VNET_DHCPUSER");
    if (!dhcpuser) {
      dhcpuser = strdup("root");
    }

    pubmode = getConfString(configFile, "VNET_MODE");
    if (!pubmode) {
      logprintfl(EUCAWARN,"VNET_MODE is not defined, defaulting to 'SYSTEM'\n");
      pubmode = strdup("SYSTEM");
    }
    
    pubInterface = getConfString(configFile, "VNET_INTERFACE");
    if (!pubInterface) {
      logprintfl(EUCAWARN,"VNET_INTERFACE is not defined, defaulting to 'eth0'\n");
      pubInterface = strdup("eth0");
    }
    
    if (!strcmp(pubmode, "STATIC")) {
      pubSubnet = getConfString(configFile, "VNET_SUBNET");
      pubSubnetMask = getConfString(configFile, "VNET_NETMASK");
      pubBroadcastAddress = getConfString(configFile, "VNET_BROADCAST");
      pubRouter = getConfString(configFile, "VNET_ROUTER");
      pubDNS = getConfString(configFile, "VNET_DNS");
      pubmacmap = getConfString(configFile, "VNET_MACMAP");

      if (!pubSubnet || !pubSubnetMask || !pubBroadcastAddress || !pubRouter || !pubDNS || !pubmacmap) {
	logprintfl(EUCAFATAL,"in 'STATIC' network mode, you must specify values for 'VNET_SUBNET, VNET_NETMASK, VNET_BROADCAST, VNET_ROUTER, VNET_DNS, and VNET_MACMAP'\n");
	initFail = 1;
      } else {
      }
    } else if (!strcmp(pubmode, "MANAGED") || !strcmp(pubmode, "MANAGED-NOVLAN")) {
      numaddrs = getConfString(configFile, "VNET_ADDRSPERNET");
      pubSubnet = getConfString(configFile, "VNET_SUBNET");
      pubSubnetMask = getConfString(configFile, "VNET_NETMASK");
      pubDNS = getConfString(configFile, "VNET_DNS");
      pubips = getConfString(configFile, "VNET_PUBLICIPS");
      if (!pubSubnet || !pubSubnetMask || !pubDNS || !numaddrs) {
	logprintfl(EUCAFATAL,"in 'MANAGED' or 'MANAGED-NOVLAN' network mode, you must specify values for 'VNET_SUBNET, VNET_NETMASK, VNET_ADDRSPERNET, and VNET_DNS'\n");
	initFail = 1;
      }
    }
    
    if (initFail) {
      logprintfl(EUCAFATAL, "bad network parameters, must fix before system will work\n");
      return(1);
    }
    
    sem_wait(vnetConfigLock);
    
    vnetInit(vnetconfig, pubmode, eucahome, netPath, CLC, pubInterface, numaddrs, pubSubnet, pubSubnetMask, pubBroadcastAddress, pubDNS, pubRouter, daemon, dhcpuser, NULL);

    vnetAddDev(vnetconfig, vnetconfig->pubInterface);

    if (pubmacmap) {
      char *mac=NULL, *ip=NULL, *ptra=NULL, *toka=NULL, *ptrb=NULL, *tokb=NULL;
      toka = strtok_r(pubmacmap, " ", &ptra);
      while(toka) {
	mac = ip = NULL;
	mac = strtok_r(toka, "=", &ptrb);
	ip = strtok_r(NULL, "=", &ptrb);
	if (mac && ip) {
	  vnetAddHost(vnetconfig, mac, ip, 0);
	}
	toka = strtok_r(NULL, " ", &ptra);
      }
      vnetKickDHCP(vnetconfig);
    } else if (pubips) {
      char *ip, *ptra, *toka;
      toka = strtok_r(pubips, " ", &ptra);
      while(toka) {
	ip = toka;
	if (ip) {
	  rc = vnetAddPublicIP(vnetconfig, ip);
	  if (rc) {
	    logprintfl(EUCAERROR, "could not add public IP '%s'\n", ip);
	  }
	}
	toka = strtok_r(NULL, " ", &ptra);
      }
    }
    
    //    vnetPrintNets(vnetconfig);
    sem_post(vnetConfigLock);
  }
  
  rc = get_conf_var(configFile, "SCHEDPOLICY", &tmpstr);
  if (rc != 1) {
    // error
    logprintfl(EUCAWARN,"parsing config file (%s) for SCHEDPOLICY, defaulting to GREEDY\n", configFile);
    schedPolicy = SCHEDGREEDY;
  } else {
    if (!strcmp(tmpstr, "GREEDY")) schedPolicy = SCHEDGREEDY;
    else if (!strcmp(tmpstr, "ROUNDROBIN")) schedPolicy = SCHEDROUNDROBIN;
    else schedPolicy = SCHEDGREEDY;
  }
  if (tmpstr) free(tmpstr);

  rc = get_conf_var(configFile, "ENABLE_WS_SECURITY", &tmpstr);
  if (rc != 1) {
    // error
    logprintfl(EUCAFATAL,"parsing config file (%s) for ENABLE_WS_SECURITY\n", configFile);
    return(1);
  } else {
    if (!strcmp(tmpstr, "Y")) {
      use_wssec = 1;
    } else {
      use_wssec = 0;
    }
  }
  if (tmpstr) free(tmpstr);

  res = NULL;
  rc = refreshNodes(config, configFile, &res, &numHosts);
  if (rc) {
    logprintfl(EUCAERROR, "cannot read list of nodes, check your config file\n");
    return(1);
  }
  
  sem_wait(configLock);
  // set up the current config   
  strncpy(config->eucahome, eucahome, 1024);
  strncpy(config->policyFile, policyFile, 1024);
  config->use_wssec = use_wssec;
  config->schedPolicy = schedPolicy;
  config->numResources = numHosts;
  memcpy(config->resourcePool, res, sizeof(resource) * numHosts);
  if (res) free(res);
  config->lastResourceUpdate = 0;
  config->instanceCacheUpdate = time(NULL);
  config->configMtime = configMtime;
  config->initialized = 1;
  sem_post(configLock);
  
  logprintfl(EUCADEBUG,"init_config(): done\n");
  init=1;
  
  return(0);
}

int restoreNetworkState() {
  int rc, ret=0;
  
  logprintfl(EUCAINFO, "restoring network state from memory\n");
  // get DHCPD back up and running
  sem_wait(vnetConfigLock);
  
  rc = vnetKickDHCP(vnetconfig);
  if (rc) {
    logprintfl(EUCAERROR, "cannot start DHCP daemon, please check your network settings\n");
    ret = 1;
  }
  
  // restore iptables state
  rc = vnetRestoreTablesFromMemory(vnetconfig);
  if (rc) {
    logprintfl(EUCAERROR, "cannot restore iptables state from memory\n");
    ret = 1;
  }
  
  sem_post(vnetConfigLock);
  
  return(ret);
}

int refreshNodes(ccConfig *config, char *configFile, resource **res, int *numHosts) {
  int rc, i;
  char *tmpstr;
  char *ncservice;
  int ncport;
  char **hosts;

  rc = get_conf_var(configFile, CONFIG_NC_SERVICE, &tmpstr);
  if (rc != 1) {
    // error
    logprintfl(EUCAFATAL,"parsing config file (%s) for NC_SERVICE\n", configFile);
    return(1);
  } else {
    ncservice = strdup(tmpstr);
  }
  if (tmpstr) free(tmpstr);

  rc = get_conf_var(configFile, CONFIG_NC_PORT, &tmpstr);
  if (rc != 1) {
    // error
    logprintfl(EUCAFATAL,"parsing config file (%s) for NC_PORT\n", configFile);
    return(1);
  } else {
    ncport = atoi(tmpstr);
  }
  if (tmpstr) free(tmpstr);

  rc = get_conf_var(configFile, CONFIG_NODES, &tmpstr);
  if (rc != 1) {
    // error
    logprintfl(EUCAWARN,"parsing config file (%s) for NODES\n", configFile);
    return(1);
  } else {
    hosts = from_var_to_char_list(tmpstr);
    if (hosts == NULL) {
      logprintfl(EUCAFATAL,"parsing config file (%s) for NODES from substring (%s)\n", configFile, tmpstr);
      if (tmpstr) free(tmpstr);
      return(1);
    }

    *numHosts = 0;
    i = 0;
    while(hosts[i] != NULL) {
      (*numHosts)++;
      *res = realloc(*res, sizeof(resource) * *numHosts);
      snprintf((*res)[*numHosts-1].hostname, 128, "%s", hosts[i]);
      (*res)[*numHosts-1].ncPort = ncport;
      snprintf((*res)[*numHosts-1].ncService, 128, "%s", ncservice);
      snprintf((*res)[*numHosts-1].ncURL, 128, "http://%s:%d/%s", hosts[i], ncport, ncservice);	
      free(hosts[i]);
      i++;
    }
  }
  if (hosts) free(hosts);
  if (tmpstr) free(tmpstr);
  return(0);
}

void shawn() {
  int p=1, status;

  // clean up any orphaned child processes
  while(p > 0) {
    p = waitpid(-1, &status, WNOHANG);
  }
  if (time(NULL) - config->instanceCacheUpdate > 86400) {
    config->instanceCacheUpdate = time(NULL);
  }
  
  //  deadlock detection
  //  rc = sem_getvalue(configLock,&status);
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
    logprintfl(EUCAERROR, "select() timed out for read: timeout=%d\n", timeout);
    return(-1);
  }
  rc = read(fd, buf, bytes);
  return(rc);
}

pid_t timewait(pid_t pid, int *status, int timeout) {
  time_t timer=0;
  int rc;

  if (timeout <= 0) timeout = 1;

  *status = 1;
  rc = waitpid(pid, status, WNOHANG);
  while(rc <= 0 && timer < (timeout * 1000000)) {
    usleep(50000);
    timer += 50000;
    rc = waitpid(pid, status, WNOHANG);
  }
  if (rc < 0) {
    logprintfl(EUCAERROR, "waitpid() timed out: pid=%d\n", pid);
  }
  return(rc);
}

int allocate_ccInstance(ccInstance *out, char *id, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char *ownerId, char *state, time_t ts, char *reservationId, netConfig *ccnet, virtualMachine *ccvm, int ncHostIdx, char *keyName, char *serviceTag, char *userData, char *launchIndex, char groupNames[][32], ncVolume *volumes, int volumesSize) {
  if (out != NULL) {
    bzero(out, sizeof(ccInstance));
    if (id) strncpy(out->instanceId, id, 16);
    if (amiId) strncpy(out->amiId, amiId, 16);
    if (kernelId) strncpy(out->kernelId, kernelId, 16);
    if (ramdiskId) strncpy(out->ramdiskId, ramdiskId, 16);
    
    if (amiURL) strncpy(out->amiURL, amiURL, 64);
    if (kernelURL) strncpy(out->kernelURL, kernelURL, 64);
    if (ramdiskURL) strncpy(out->ramdiskURL, ramdiskURL, 64);
    
    if (state) strncpy(out->state, state, 16);
    if (ownerId) strncpy(out->ownerId, ownerId, 16);
    if (reservationId) strncpy(out->reservationId, reservationId, 16);
    if (keyName) strncpy(out->keyName, keyName, 1024);
    out->ts = ts;
    out->ncHostIdx = ncHostIdx;
    if (serviceTag) strncpy(out->serviceTag, serviceTag, 64);
    if (userData) strncpy(out->userData, userData, 64);
    if (launchIndex) strncpy(out->launchIndex, launchIndex, 64);
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

    if (ccnet) allocate_netConfig(&(out->ccnet), ccnet->privateMac, ccnet->publicMac, ccnet->privateIp, ccnet->publicIp, ccnet->vlan);
    if (ccvm) allocate_virtualMachine(&(out->ccvm), ccvm->mem, ccvm->disk, ccvm->cores, ccvm->name);    
  }
  return(0);
}

int allocate_netConfig(netConfig *out, char *pvMac, char *pbMac, char *pvIp, char *pbIp, int vlan) {
  if (out != NULL) {
    if (pvMac) strncpy(out->privateMac,pvMac,24);
    if (pbMac) strncpy(out->publicMac,pbMac,24);
    if (pvIp) strncpy(out->privateIp,pvIp,24);
    if (pbIp) strncpy(out->publicIp,pbIp,24);
    out->vlan = vlan;
  }
  return(0);
}

int allocate_virtualMachine(virtualMachine *out, int mem, int disk, int cores, char *name) {
  if (out != NULL) {
    out->mem = mem;
    out->disk = disk;
    out->cores = cores;
    snprintf(out->name, 64, "%s", name);
  }
  return(0);
}

void print_instanceCache(void) {
  int i;
  for (i=0; i<MAXINSTANCES; i++) {
    if (instanceCache[i].instanceId[0] != '\0') {
      logprintfl(EUCADEBUG,"\tcache: %s %s %s\n", instanceCache[i].instanceId, instanceCache[i].ccnet.publicIp, instanceCache[i].ccnet.privateIp);
    }
  }
}

void invalidate_instanceCache(void) {
  int i;
  for (i=0; i<MAXINSTANCES; i++) {
    if (instanceCache[i].instanceId[0] != '\0') {
      // del from cache
      bzero(&(instanceCache[i]), sizeof(ccInstance));
    }
  }
}

int refresh_instanceCache(char *instanceId, ccInstance *in){
  int i, done, firstNull;

  if (!instanceId || !in) {
    return(1);
  }
  
  done=0;
  for (i=0; i<MAXINSTANCES && !done; i++) {
    if (instanceCache[i].instanceId[0] != '\0') {
      if (!strcmp(instanceCache[i].instanceId, instanceId)) {
	// in cache
	logprintfl(EUCADEBUG, "refreshing instance '%s'\n", instanceId);
	memcpy(&(instanceCache[i]), in, sizeof(ccInstance));
	return(0);
      }
    }
  }
  return(0);
}

int add_instanceCache(char *instanceId, ccInstance *in){
  int i, done, firstNull=0;

  if (!instanceId || !in) {
    return(1);
  }
  
  done=0;
  for (i=0; i<MAXINSTANCES && !done; i++) {
    if (instanceCache[i].instanceId[0] != '\0') {
      if (!strcmp(instanceCache[i].instanceId, instanceId)) {
	// already in cache
	return(0);
      }
    } else {
      firstNull = i;
      done++;
    }
  }
  if (!done) {
  }
  allocate_ccInstance(&(instanceCache[firstNull]), in->instanceId, in->amiId, in->kernelId, in->ramdiskId, in->amiURL, in->kernelURL, in->ramdiskURL, in->ownerId, in->state, in->ts, in->reservationId, &(in->ccnet), &(in->ccvm), in->ncHostIdx, in->keyName, in->serviceTag, in->userData, in->launchIndex, in->groupNames, in->volumes, in->volumesSize);

  return(0);
}

int del_instanceCacheId(char *instanceId) {
  int i;

  for (i=0; i<MAXINSTANCES; i++) {
    if (instanceCache[i].instanceId[0] != '\0') {
      if (!strcmp(instanceCache[i].instanceId, instanceId)) {
	// del from cache
	bzero(&(instanceCache[i]), sizeof(ccInstance));
	return(0);
      }
    }
  }
  return(0);
}

int find_instanceCacheId(char *instanceId, ccInstance **out) {
  int i, done;
  
  if (!instanceId || !out) {
    return(1);
  }
  
  *out = NULL;
  done=0;
  for (i=0; i<MAXINSTANCES && !done; i++) {
    if (instanceCache[i].instanceId[0] != '\0') {
      if (!strcmp(instanceCache[i].instanceId, instanceId)) {
	// found it
	*out = malloc(sizeof(ccInstance));
	allocate_ccInstance(*out, instanceCache[i].instanceId,instanceCache[i].amiId, instanceCache[i].kernelId, instanceCache[i].ramdiskId, instanceCache[i].amiURL, instanceCache[i].kernelURL, instanceCache[i].ramdiskURL, instanceCache[i].ownerId, instanceCache[i].state,instanceCache[i].ts, instanceCache[i].reservationId, &(instanceCache[i].ccnet), &(instanceCache[i].ccvm), instanceCache[i].ncHostIdx, instanceCache[i].keyName, instanceCache[i].serviceTag, instanceCache[i].userData, instanceCache[i].launchIndex, instanceCache[i].groupNames, instanceCache[i].volumes, instanceCache[i].volumesSize);
	done++;
      }
    }
  }

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
  
  *out = NULL;
  done=0;
  for (i=0; i<MAXINSTANCES && !done; i++) {
    if (instanceCache[i].ccnet.publicIp[0] != '\0' || instanceCache[i].ccnet.privateIp[0] != '\0') {
      if (!strcmp(instanceCache[i].ccnet.publicIp, ip) || !strcmp(instanceCache[i].ccnet.privateIp, ip)) {
	// found it
	*out = malloc(sizeof(ccInstance));
	allocate_ccInstance(*out, instanceCache[i].instanceId,instanceCache[i].amiId, instanceCache[i].kernelId, instanceCache[i].ramdiskId, instanceCache[i].amiURL, instanceCache[i].kernelURL, instanceCache[i].ramdiskURL, instanceCache[i].ownerId, instanceCache[i].state,instanceCache[i].ts, instanceCache[i].reservationId, &(instanceCache[i].ccnet), &(instanceCache[i].ccvm), instanceCache[i].ncHostIdx, instanceCache[i].keyName, instanceCache[i].serviceTag, instanceCache[i].userData, instanceCache[i].launchIndex, instanceCache[i].groupNames, instanceCache[i].volumes, instanceCache[i].volumesSize);
	done++;
      }
    }
  }

  if (done) {
    return(0);
  }
  return(1);
}


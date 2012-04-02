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
#include <string.h>

#define HANDLERS_FANOUT
#include "handlers.h"
#include "client-marshal.h"

#include <fcntl.h>           /* For O_* constants */
#include <sys/stat.h>        /* For mode constants */
#include <semaphore.h>
#include <sys/mman.h>
#include <sys/stat.h>        /* For mode constants */
#include <fcntl.h>           /* For O_* constants */


enum {SHARED_MEM, SHARED_FILE};
#define MAX_FAKE_INSTANCES 4096

typedef struct fakeconfig_t {
  ncInstance global_instances[MAX_FAKE_INSTANCES];
  ncResource res;
  time_t current, last;
} fakeconfig;
fakeconfig *myconfig;
sem_t *fakelock;

void saveNcStuff() {
  int fd, i, done=0;
  
  sem_post(fakelock);
}

int setup_shared_buffer_fake(void **buf, char *bufname, size_t bytes, sem_t **lock, char *lockname, int mode) {
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

void loadNcStuff() {
  int fd, i, count=0, done=0, rc, j;
  struct stat mystat;

  rc = setup_shared_buffer_fake((void **)&myconfig, "/eucalyptusCCfakeconfig", sizeof(fakeconfig), &fakelock, "/eucalyptusCCfakelock", SHARED_FILE);
  if (rc) {
    logprintfl(EUCADEBUG, "fakeNC:  error setting up shared mem\n");
  }  
  sem_wait(fakelock);

  done=0;
  for (i=0; i<MAX_FAKE_INSTANCES && !done; i++) {
    if (!strlen(myconfig->global_instances[i].instanceId)) {
      count = i;
      done++;
    }
  }
  
  if (myconfig->last == 0) {
    myconfig->last = time(NULL);
    myconfig->current = time(NULL);
  } else {
    myconfig->current = time(NULL);
  }
  logprintfl(EUCADEBUG, "fakeNC: setup(): last=%d current=%d\n", myconfig->last, myconfig->current);
  if ( (myconfig->current - myconfig->last) > 30 ) {
    // do a refresh
    myconfig->last = time(NULL);
    myconfig->current = time(NULL);
    for (i=0; i<MAX_FAKE_INSTANCES; i++) {
      if (strlen(myconfig->global_instances[i].instanceId)) {

	if (!strcmp(myconfig->global_instances[i].stateName, "Teardown") && ((time(NULL) - myconfig->global_instances[i].launchTime) > 300) ) {
	  logprintfl(EUCADEBUG, "fakeNC: setup(): invalidating instance %s\n", myconfig->global_instances[i].instanceId);
	  bzero(&(myconfig->global_instances[i]), sizeof(ncInstance));
	} else {
	  for (j=0; j<EUCA_MAX_VOLUMES; j++) {
	    if (strlen(myconfig->global_instances[i].volumes[j].volumeId) && strcmp(myconfig->global_instances[i].volumes[j].stateName, "attached")) {
	      logprintfl(EUCADEBUG, "fakeNC: setup(): invalidating volume %s\n", myconfig->global_instances[i].volumes[j].volumeId);	    
	      bzero(&(myconfig->global_instances[i].volumes[j]), sizeof(ncVolume));
	    }
	  }
	}
      }
    }
  }
}

ncStub * ncStubCreate (char *endpoint_uri, char *logfile, char *homedir) 
{
    axutil_env_t * env = NULL;
    axis2_char_t * client_home;
    axis2_stub_t * stub;
    ncStub * st = NULL;

    if ( logfile ) {
      env =  axutil_env_create_all (logfile, AXIS2_LOG_LEVEL_TRACE);
    } else {
      env =  axutil_env_create_all (NULL, 0);
    }
    if ( homedir ) {
        client_home = (axis2_char_t *)homedir;
    } else {
        client_home = AXIS2_GETENV("AXIS2C_HOME");
    }

    if (client_home == NULL) {
        logprintfl (EUCAERROR, "fakeNC: ERROR: cannot get AXIS2C_HOME");
	return NULL;
    }
    if (endpoint_uri == NULL) {
        logprintfl (EUCAERROR, "fakeNC: ERROR: empty endpoint_url");
	return NULL;
    }

    char * uri = endpoint_uri;

    // extract node name from the endpoint
    char * p = strstr (uri, "://"); // find "http[s]://..."
    if (p==NULL) {
      logprintfl (EUCAERROR, "fakeNC: ncStubCreate received invalid URI %s\n", uri);
      return NULL;
    }
    char * node_name = strdup (p+3); // copy without the protocol prefix
    if (node_name==NULL) {
      logprintfl (EUCAERROR, "fakeNC: ncStubCreate is out of memory\n");
      return NULL;
    }
    if ((p = strchr (node_name, ':')) != NULL) *p = '\0'; // cut off the port
    if ((p = strchr (node_name, '/')) != NULL) *p = '\0'; // if there is no port

    logprintfl (EUCADEBUG, "fakeNC: DEBUG: requested URI %s\n", uri);

    // see if we should redirect to a local broker
    if (strstr (uri, "EucalyptusBroker")) {
        uri = "http://localhost:8773/services/EucalyptusBroker";
        logprintfl (EUCADEBUG, "fakeNC: DEBUG: redirecting request to %s\n", uri);
    }

    // TODO: what if endpoint_uri, home, or env are NULL?
    stub = axis2_stub_create_EucalyptusNC(env, client_home, (axis2_char_t *)uri);

    if (stub && (st = malloc (sizeof(ncStub)))) {
        st->env=env;
        st->client_home=strdup((char *)client_home);
        st->endpoint_uri=(axis2_char_t *)strdup(endpoint_uri);
	st->node_name=(axis2_char_t *)strdup(node_name);
        st->stub=stub;
	if (st->client_home == NULL || st->endpoint_uri == NULL) {
            logprintfl (EUCAWARN, "fakeNC: WARNING: out of memory");
	}
    } else {
        logprintfl (EUCAWARN, "fakeNC: WARNING: out of memory");
    } 
    
    free (node_name);
    return st;
}

int ncStubDestroy (ncStub * st)
{
    free (st);
    return (0);
}

/************************** stubs **************************/

int ncRunInstanceStub (ncStub *st, ncMetadata *meta, char *uuid, char *instanceId, char *reservationId, virtualMachine *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId, char *accountId, char *keyName, netConfig *netparams, char *userData, char *launchIndex, char *platform, int expiryTime, char **groupNames, int groupNamesSize, ncInstance **outInstPtr)
{
  ncInstance *instance;
  int i, j, foundidx=-1;

  logprintfl(EUCADEBUG, "fakeNC: runInstance(): params: uuid=%s instanceId=%s reservationId=%s ownerId=%s accountId=%s platform=%s\n", SP(uuid), SP(instanceId), SP(reservationId), SP(ownerId), SP(accountId), SP(platform));

  if (!uuid || !instanceId || !reservationId || !ownerId || !accountId || !platform || !meta || !netparams) {
    logprintfl(EUCAERROR, "fakeNC: runInstance(): bad input params\n");
    return(0);
  }

  loadNcStuff();

  instance = allocate_instance (uuid,
				instanceId, 
				reservationId,
				params, 
				instance_state_names[PENDING], 
				PENDING, 
				meta->userId, 
				ownerId, accountId,
				netparams, keyName,
				userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize);
  if (instance) {
    instance->launchTime = time (NULL);
    foundidx=-1;
    for (i=0; i<MAX_FAKE_INSTANCES && (foundidx < 0); i++) {
      if (!strlen(myconfig->global_instances[i].instanceId)) {
	foundidx = i;
      }
    }
    memcpy( &(myconfig->global_instances[foundidx]),instance, sizeof(ncInstance));
    logprintfl(EUCADEBUG, "fakeNC: runInstance(): decrementing resource by %d/%d/%d\n", params->cores, params->mem, params->disk);
    myconfig->res.memorySizeAvailable -= params->mem;
    myconfig->res.numberOfCoresAvailable -= params->cores;
    myconfig->res.diskSizeAvailable -= params->disk;
    
    *outInstPtr = instance;
    logprintfl(EUCADEBUG, "fakeNC: runInstance(): allocated and stored instance\n");
  } else {
    logprintfl(EUCAERROR, "fakeNC: runInstance(): failed to allocate instance\n");
  }
  
  saveNcStuff();
  return(0);
}

int ncTerminateInstanceStub (ncStub *st, ncMetadata *meta, char *instanceId, int force, int *shutdownState, int *previousState)
{
  int i, done=0;
  
  logprintfl(EUCADEBUG, "fakeNC: terminateInstance(): params: instanceId=%s force=%d\n", SP(instanceId), force);
  
  if (!instanceId) {
    logprintfl(EUCAERROR, "fakeNC: termianteInstance(): bad input params\n");
    return(0);
  }
  
  loadNcStuff();

  for (i=0; i<MAX_FAKE_INSTANCES && !done; i++) {
    if (!strcmp(myconfig->global_instances[i].instanceId, instanceId)) {
      logprintfl(EUCADEBUG, "fakeNC: terminateInstance():\tsetting stateName for instance %s at idx %d\n", instanceId, i);
      snprintf(myconfig->global_instances[i].stateName, 10, "Teardown");
      myconfig->res.memorySizeAvailable += myconfig->global_instances[i].params.mem;
      myconfig->res.numberOfCoresAvailable += myconfig->global_instances[i].params.cores;
      myconfig->res.diskSizeAvailable += myconfig->global_instances[i].params.disk;
      done++;
    }
  }
  
  if (shutdownState && previousState) {
    *shutdownState = *previousState = 0;
  }

  saveNcStuff();
  return(0);
}

int ncAssignAddressStub (ncStub *st, ncMetadata *meta, char *instanceId, char *publicIp){
  int i, done=0;

  logprintfl(EUCADEBUG, "fakeNC: assignAddress(): params: instanceId=%s publicIp=%s\n", SP(instanceId), SP(publicIp));
  if (!instanceId || !publicIp) {
    logprintfl(EUCADEBUG, "fakeNC: assignAddress(): bad input params\n");
    return(0);
  }

  loadNcStuff();

  for (i=0; i<MAX_FAKE_INSTANCES && !done; i++) {
    if (!strcmp(myconfig->global_instances[i].instanceId, instanceId)) {
      logprintfl(EUCADEBUG, "fakeNC: assignAddress()\tsetting publicIp at idx %d\n", i);
      snprintf(myconfig->global_instances[i].ncnet.publicIp, 24, "%s", publicIp);
      done++;
    }
  }
  
  saveNcStuff();
  return(0);
}

int ncPowerDownStub (ncStub *st, ncMetadata *meta){
  return(0);
}

int ncDescribeInstancesStub (ncStub *st, ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
  int i, numinsts=0;
  logprintfl(EUCADEBUG, "fakeNC: describeInstances(): params: instIdsLen=%d\n", instIdsLen);
  
  if (instIdsLen < 0) {
    logprintfl(EUCAERROR, "fakeNC: describeInstances(): bad input params\n");
    return(0);
  }
  
  loadNcStuff();

  //  *outInstsLen = myconfig->instanceidx+1;
  *outInsts = malloc (sizeof(ncInstance *) * MAX_FAKE_INSTANCES);
  for (i=0; i<MAX_FAKE_INSTANCES; i++) {
    if (strlen(myconfig->global_instances[i].instanceId)) {
      ncInstance *newinst;
      newinst = malloc(sizeof(ncInstance));
      if (!strcmp(myconfig->global_instances[i].stateName, "Pending")) {
	snprintf(myconfig->global_instances[i].stateName, 8, "Extant");
      }
      memcpy(newinst, &(myconfig->global_instances[i]), sizeof(ncInstance));
      (* outInsts)[numinsts] = newinst;
      logprintfl(EUCADEBUG, "fakeNC: describeInstances(): idx=%d numinsts=%d instanceId=%s stateName=%s\n", i, numinsts, newinst->instanceId, newinst->stateName);
      numinsts++;
    }
  }
  *outInstsLen = numinsts;
  
  saveNcStuff();
  return(0);
}

int ncBundleInstanceStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL, char *userPublicKey, char *S3Policy, char *S3PolicySig) {
  return(ncTerminateInstanceStub(stub, meta, instanceId, 0, NULL, NULL));
}

int ncCancelBundleTaskStub (ncStub *stub, ncMetadata *meta, char *instanceId) {
  return(0);
}

int ncDescribeBundleTasksStub (ncStub *stub, ncMetadata *meta, char **instIds, int instIdsLen, bundleTask ***outBundleTasks, int *outBundleTasksLen) {
  return(0);
}

int ncDescribeResourceStub (ncStub *st, ncMetadata *meta, char *resourceType, ncResource **outRes)
{
  int ret=0;
  ncResource *res;

  loadNcStuff();
  
  if (myconfig->res.memorySizeMax <= 0) {
    // not initialized?
    res = allocate_resource ("OK", "iqn.1993-08.org.debian:01:736a4e92c588", 1024000, 1024000, 30000000, 30000000, 4096, 4096, "none");
    if (!res) {
      logprintfl(EUCAERROR, "fakeNC: describeResource(): failed to allocate fake resource\n");
      ret=1;
    } else {
      memcpy(&(myconfig->res), res, sizeof(ncResource));
      free(res);
    }
  }
  
  if (!ret) {
    res = malloc(sizeof(ncResource));
    memcpy(res, &(myconfig->res), sizeof(ncResource));
    *outRes = res;
  } else {
    *outRes = NULL;
  }
  
  saveNcStuff();
  return(ret);
}

int ncAttachVolumeStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
  int i, j, done=0, vdone=0, foundidx=-1;
  
  logprintfl(EUCADEBUG, "fakeNC:  attachVolume(): params: instanceId=%s volumeId=%s remoteDev=%s localDev=%s\n", SP(instanceId), SP(volumeId), SP(remoteDev), SP(localDev));
  if (!instanceId || !volumeId || !remoteDev || !localDev) {
    logprintfl(EUCADEBUG, "fakeNC:  attachVolume(): bad input params\n");
    return(0);
  }

  loadNcStuff();

  for (i=0; i<MAX_FAKE_INSTANCES && !done; i++) {
    if (!strcmp(myconfig->global_instances[i].instanceId, instanceId)) {
      logprintfl(EUCADEBUG, "fakeNC: \tsetting volume info at idx %d\n", i);
      vdone=0;
      for (j=0; j<EUCA_MAX_VOLUMES; j++) {
	if (!strlen(myconfig->global_instances[i].volumes[j].volumeId)) {
	  if (foundidx < 0) {
	    foundidx=j;
	  }
	} else if (!strcmp(myconfig->global_instances[i].volumes[j].volumeId, volumeId)) {
	  vdone++;
	}
      }
      if (!vdone && foundidx >= 0) {
	logprintfl(EUCADEBUG, "fakeNC: \tfake attaching volume at idx %d\n", foundidx);
	snprintf(myconfig->global_instances[i].volumes[foundidx].volumeId, CHAR_BUFFER_SIZE, "%s", volumeId);
	snprintf(myconfig->global_instances[i].volumes[foundidx].remoteDev, CHAR_BUFFER_SIZE, "%s", remoteDev);
	snprintf(myconfig->global_instances[i].volumes[foundidx].localDev, CHAR_BUFFER_SIZE, "%s", localDev);
	snprintf(myconfig->global_instances[i].volumes[foundidx].localDevReal, CHAR_BUFFER_SIZE, "%s", localDev);
	snprintf(myconfig->global_instances[i].volumes[foundidx].stateName, CHAR_BUFFER_SIZE, "%s", "attached");
      }
      done++;
    }
  }

  saveNcStuff();
  return (0);
}

int ncDetachVolumeStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
  int i, j, done=0, vdone=0, foundidx=-1;

  logprintfl(EUCADEBUG, "fakeNC:  detachVolume(): params: instanceId=%s volumeId=%s remoteDev=%s localDev=%s\n", SP(instanceId), SP(volumeId), SP(remoteDev), SP(localDev));
  if (!instanceId || !volumeId || !remoteDev || !localDev) {
    logprintfl(EUCADEBUG, "fakeNC:  detachVolume(): bad input params\n");
    return(0);
  }

  loadNcStuff();

  for (i=0; i<MAX_FAKE_INSTANCES && !done; i++) {
    if (!strcmp(myconfig->global_instances[i].instanceId, instanceId)) {
      logprintfl(EUCADEBUG, "fakeNC: \tsetting volume info at idx %d\n", i);
      vdone=0;
      for (j=0; j<EUCA_MAX_VOLUMES; j++) {
	if (!strcmp(myconfig->global_instances[i].volumes[j].volumeId, volumeId)) {
	  foundidx=j;
	}
      }
      if (foundidx >= 0) {
	logprintfl(EUCADEBUG, "fakeNC: \tfake detaching volume at idx %d\n", foundidx);
	snprintf(myconfig->global_instances[i].volumes[foundidx].stateName, CHAR_BUFFER_SIZE, "%s", "detached");
      }
      done++;
    }
  }

  saveNcStuff();
  return (0);
}

int ncCreateImageStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev)
{
  return(ncTerminateInstanceStub(stub, meta, instanceId, 0, NULL, NULL));
}

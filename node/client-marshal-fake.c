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
  int instanceidx;
} fakeconfig;
fakeconfig *myconfig;
sem_t *fakelock;

void saveNcStuff() {
  int fd, i, done=0;

  sem_post(fakelock);
  /*
  fd = open("/tmp/global_instances", O_WRONLY | O_CREAT, 0644);
  if (fd) {
    write(fd, global_instances, sizeof(ncInstance) * MAX_FAKE_INSTANCES);
    close(fd);
  }
  for (i=0; i<MAX_FAKE_INSTANCES && !done; i++) {
    if (strlen(global_instances[i].instanceId)) {
      logprintfl(EUCADEBUG, "HI:\tinstance %d: %s/%s\n", i, global_instances[i].instanceId, global_instances[i].stateName);
    } else {
      done++;
    }
  }
  logprintfl(EUCADEBUG, "HI: saved global_instances\n");
  */
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
  int fd, i, count=0, done=0, rc;
  struct stat mystat;

  /*
  if (stat("/tmp/global_instances", &mystat)) {
    logprintfl(EUCADEBUG, "HI: initializeing global_instances\n");
    bzero(global_instances, sizeof(ncInstance) * MAX_FAKE_INSTANCES);
    lock = sem_open("eucalyptusCCfakesem", O_CREAT, 0644, 1);    
    sem_wait(lock);
    saveNcStuff();
  }
  */
  
  rc = setup_shared_buffer_fake((void **)&myconfig, "/eucalyptusCCfakeconfig", sizeof(fakeconfig), &fakelock, "/eucalyptusCCfakelock", SHARED_FILE);
  if (rc) {
    logprintfl(EUCADEBUG, "HI: error setting up shared mem\n");
  }  
  sem_wait(fakelock);

  /*
  fd = open("/tmp/global_instances", O_RDONLY);
  if (fd) {
    int rbytes;
    logprintfl(EUCADEBUG, "HI: reading global_intances\n");
    rbytes = read(fd, global_instances, sizeof(ncInstance) * MAX_FAKE_INSTANCES);
    logprintfl(EUCADEBUG, "HI: read %d bytes\n", rbytes);
    close(fd);
  }
  */
  done=0;
  for (i=0; i<MAX_FAKE_INSTANCES && !done; i++) {
    if (!strlen(myconfig->global_instances[i].instanceId)) {
      count = i;
      done++;
    }
  }
  myconfig->instanceidx = count-1;

  logprintfl(EUCADEBUG, "HI: instanceidx=%d after load\n", myconfig->instanceidx);
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
        logprintfl (EUCAERROR, "ERROR: cannot get AXIS2C_HOME");
	return NULL;
    }
    if (endpoint_uri == NULL) {
        logprintfl (EUCAERROR, "ERROR: empty endpoint_url");
	return NULL;
    }

    char * uri = endpoint_uri;

    // extract node name from the endpoint
    char * p = strstr (uri, "://"); // find "http[s]://..."
    if (p==NULL) {
      logprintfl (EUCAERROR, "ncStubCreate received invalid URI %s\n", uri);
      return NULL;
    }
    char * node_name = strdup (p+3); // copy without the protocol prefix
    if (node_name==NULL) {
      logprintfl (EUCAERROR, "ncStubCreate is out of memory\n");
      return NULL;
    }
    if ((p = strchr (node_name, ':')) != NULL) *p = '\0'; // cut off the port
    if ((p = strchr (node_name, '/')) != NULL) *p = '\0'; // if there is no port

    logprintfl (EUCADEBUG, "DEBUG: requested URI %s\n", uri);

    // see if we should redirect to the VMware broker
    if (strstr (uri, "VMwareBroker")) {
        uri = "http://localhost:8773/services/VMwareBroker";
        logprintfl (EUCADEBUG, "DEBUG: redirecting request to %s\n", uri);
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
            logprintfl (EUCAWARN, "WARNING: out of memory");
	}
    } else {
        logprintfl (EUCAWARN, "WARNING: out of memory");
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

int ncRunInstanceStub (ncStub *st, ncMetadata *meta, char *uuid, char *instanceId, char *reservationId, virtualMachine *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, netConfig *netparams, char *userData, char *launchIndex, char *platform, int expiryTime, char **groupNames, int groupNamesSize, ncInstance **outInstPtr)
{
  ncInstance *instance;
  loadNcStuff();

  instance = allocate_instance (uuid,
				instanceId, 
				reservationId,
				params, 
				instance_state_names[PENDING], 
				PENDING, 
				meta->userId, 
				netparams, keyName,
				userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize);
  myconfig->instanceidx++;
  memcpy( &(myconfig->global_instances[myconfig->instanceidx]),instance, sizeof(ncInstance));

  *outInstPtr = instance;

  saveNcStuff();
  return(0);
  //  return doRunInstance (meta, uuid, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, netparams, userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize, outInstPtr);
}

int ncTerminateInstanceStub (ncStub *st, ncMetadata *meta, char *instanceId, int force, int *shutdownState, int *previousState)
{
  int i, done=0;
  loadNcStuff();

  logprintfl(EUCADEBUG, "HI: Terminate for instance %s\n", instanceId);
  for (i=0; i<MAX_FAKE_INSTANCES && !done; i++) {
    if (!strlen(myconfig->global_instances[i].instanceId)) {
      logprintfl(EUCADEBUG, "HI:\tfinishing terminate loop at idx %d\n", i);
      done++;
    } else if (!strcmp(myconfig->global_instances[i].instanceId, instanceId)) {
      logprintfl(EUCADEBUG, "HI:\tsetting stateName at idx %d\n", i);
      snprintf(myconfig->global_instances[i].stateName, 10, "Teardown");
    }
  }
  
  *shutdownState = *previousState = 0;

  saveNcStuff();
  return(0);
  //  return doTerminateInstance (meta, instanceId, force, shutdownState, previousState);
}
int ncAssignAddressStub (ncStub *st, ncMetadata *meta, char *instanceId, char *publicIp){
  return(0);
}
int ncPowerDownStub (ncStub *st, ncMetadata *meta){
  return(0);
}

int ncDescribeInstancesStub (ncStub *st, ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
  int i;
  loadNcStuff();

  *outInstsLen = myconfig->instanceidx+1;
  * outInsts = malloc (sizeof(ncInstance *) * *outInstsLen);
  for (i=0; i<*outInstsLen; i++) {
    ncInstance *newinst;
    newinst = malloc(sizeof(ncInstance));
    if (!strcmp(myconfig->global_instances[i].stateName, "Pending")) {
      snprintf(myconfig->global_instances[i].stateName, 8, "Extant");
    }
    memcpy(newinst, &(myconfig->global_instances[i]), sizeof(ncInstance));
    (* outInsts)[i] = newinst;
    logprintfl(EUCADEBUG, "HI: %s/%s\n", newinst->instanceId, newinst->stateName);
  }

  saveNcStuff();
  return(0);

  //    return doDescribeInstances (meta, instIds, instIdsLen, outInsts, outInstsLen);
}

int ncBundleInstanceStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL, char *userPublicKey, char *S3Policy, char *S3PolicySig) {
  return(0);
}

int ncCancelBundleTaskStub (ncStub *stub, ncMetadata *meta, char *instanceId) {
  return(0);
}

int ncDescribeBundleTasksStub (ncStub *stub, ncMetadata *meta, char **instIds, int instIdsLen, bundleTask ***outBundleTasks, int *outBundleTasksLen) {
  return(0);
}

int ncDescribeResourceStub (ncStub *st, ncMetadata *meta, char *resourceType, ncResource **outRes)
{
  ncResource *res;
  loadNcStuff();

  res = allocate_resource ("OK", "iqn.1993-08.org.debian:01:736a4e92c588", 1024000, 1024000, 30000000, 30000000, 4096, 4096, "none");
  *outRes = res;

  saveNcStuff();
  return(0);
  //    return doDescribeResource (meta, resourceType, outRes);
}

int ncAttachVolumeStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
    return doAttachVolume (meta, instanceId, volumeId, remoteDev, localDev);
}

int ncDetachVolumeStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
    return doDetachVolume (meta, instanceId, volumeId, remoteDev, localDev, force, 1);
}

int ncCreateImageStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev)
{
    return doCreateImage (meta, instanceId, volumeId, remoteDev);
}

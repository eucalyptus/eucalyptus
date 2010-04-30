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
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU /* strnlen */
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

#include "ipc.h"
#include "misc.h"
#include <handlers.h>
#include <storage.h>
#include <eucalyptus.h>
#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>
#include <vnetwork.h>
#include <euca_auth.h>

#include <windows-bundle.h>

/* coming from handlers.c */
extern sem * hyp_sem;
extern sem * inst_sem;
extern bunchOfInstances * global_instances;

static int
doInitialize (struct nc_state_t *nc) 
{
	return OK;
}

static int
doRunInstance (	struct nc_state_t *nc, ncMetadata *meta, char *instanceId,
		char *reservationId, virtualMachine *params, 
		char *imageId, char *imageURL, 
		char *kernelId, char *kernelURL, 
		char *ramdiskId, char *ramdiskURL, 
		char *keyName, 
		netConfig *netparams,
		char *userData, char *launchIndex, char *platform,
		char **groupNames, int groupNamesSize, ncInstance **outInst)
{
	logprintfl(EUCAERROR, "no default for doRunInstance!\n");
	return ERROR_FATAL;
}

static int
doRebootInstance(struct nc_state_t *nc, ncMetadata *meta, char *instanceId) 
{    
	logprintfl(EUCAERROR, "no default for doRebootInstance!\n");
	return ERROR_FATAL;
}

static int
doGetConsoleOutput(	struct nc_state_t *nc, 
			ncMetadata *meta,
			char *instanceId,
			char **consoleOutput)
{
	logprintfl(EUCAERROR, "no default for doGetConsoleOutput!\n");
	return ERROR_FATAL;
}

// finds instance by ID and destroys it on the hypervisor
// NOTE: this must be called with inst_sem semaphore held
static int 
find_and_destroy_instance ( 
	char *instanceId, 
	ncInstance **instance_p )
{
	ncInstance *instance;
	virConnectPtr *conn;
	int err;

	instance = find_instance(&global_instances, instanceId);
	if (instance == NULL) 
		return NOT_FOUND;
	* instance_p = instance;

	/* try stopping the domain */
	conn = check_hypervisor_conn();
	if (conn) {
	        sem_p(hyp_sem);
	        virDomainPtr dom = virDomainLookupByName(*conn, instanceId);
		sem_v(hyp_sem);
		if (dom) {
			/* also protect 'destroy' commands, just in case */
			sem_p (hyp_sem);
			err = virDomainDestroy (dom);
			sem_v (hyp_sem);
			if (err==0) {
				logprintfl (EUCAINFO, "destroyed domain for instance %s\n", instanceId);
			}
			sem_p(hyp_sem);
			virDomainFree(dom); /* necessary? */
			sem_v(hyp_sem);
		} else {
			if (instance->state != BOOTING && instance->state != STAGING && instance->state != TEARDOWN)
				logprintfl (EUCAWARN, "warning: domain %s to be terminated not running on hypervisor\n", instanceId);
		}
	} 
	return OK;
}

static int
doTerminateInstance(	struct nc_state_t *nc,
			ncMetadata *meta,
			char *instanceId,
			int *shutdownState,
			int *previousState)
{
	ncInstance *instance;
	int err;

	sem_p (inst_sem);
	err = find_and_destroy_instance (instanceId, &instance);
	if (err!=OK) {
		sem_v(inst_sem);
		return err;
	}

	// change the state and let the monitoring_thread clean up state
	if (instance->state!=TEARDOWN) { // do not leave TEARDOWN
		if (instance->state==BOOTING || instance->state==STAGING) {
			change_state (instance, CANCELED);
		} else {
			change_state (instance, SHUTOFF);
		}
	}
    sem_v (inst_sem);

	*previousState = instance->stateCode;
	*shutdownState = instance->stateCode;

	return OK;
}

static int
doDescribeInstances(	struct nc_state_t *nc,
			ncMetadata *meta,
			char **instIds,
			int instIdsLen,
			ncInstance ***outInsts,
			int *outInstsLen)
{
	ncInstance *instance;
	int total, i, j, k;

	*outInstsLen = 0;
	*outInsts = NULL;

	sem_p (inst_sem);
	if (instIdsLen == 0) /* describe all instances */
		total = total_instances (&global_instances);
	else 
		total = instIdsLen;

	*outInsts = malloc(sizeof(ncInstance *)*total);
	if ((*outInsts) == NULL) {
		sem_v (inst_sem);
		return OUT_OF_MEMORY;
	}

	k = 0;
	for (i=0; (instance = get_instance(&global_instances)) != NULL; i++) {
		/* only pick ones the user (or admin)  is allowed to see */
		if (strcmp(meta->userId, nc->admin_user_id) 
				&& strcmp(meta->userId, instance->userId))
			continue;

		if (instIdsLen > 0) {
			for (j=0; j < instIdsLen; j++)
				if (!strcmp(instance->instanceId, instIds[j]))
					break;

			if (j >= instIdsLen)
				/* instance of not relavance right now */
				continue;
		}
		(* outInsts)[k++] = instance;
	}
	*outInstsLen = k;
	sem_v (inst_sem);

	return OK;
}

static int
doDescribeResource(	struct nc_state_t *nc,
			ncMetadata *meta,
			char *resourceType,
			ncResource **outRes)
{
    ncResource * res;
    ncInstance * inst;

    /* stats to re-calculate now */
    long long mem_free;
    long long disk_free;
    int cores_free;

    /* intermediate sums */
    long long sum_mem = 0;  /* for known domains: sum of requested memory */
    long long sum_disk = 0; /* for known domains: sum of requested disk sizes */
    int sum_cores = 0;      /* for known domains: sum of requested cores */


    *outRes = NULL;
    sem_p (inst_sem); 
    while ((inst=get_instance(&global_instances))!=NULL) {
        if (inst->state == TEARDOWN) continue; /* they don't take up resources */
        sum_mem += inst->params.mem;
        sum_disk += (inst->params.disk + SWAP_SIZE);
        sum_cores += inst->params.cores;
    }
    sem_v (inst_sem);
    
    disk_free = nc->disk_max - sum_disk;
    if ( disk_free < 0 ) disk_free = 0; /* should not happen */
    
    mem_free = nc->mem_max - sum_mem;
    if ( mem_free < 0 ) mem_free = 0; /* should not happen */

    cores_free = nc->cores_max - sum_cores; /* TODO: should we -1 for dom0? */
    if ( cores_free < 0 ) cores_free = 0; /* due to timesharing */

    /* check for potential overflow - should not happen */
    if (nc->mem_max > INT_MAX ||
        mem_free > INT_MAX ||
        nc->disk_max > INT_MAX ||
        disk_free > INT_MAX) {
        logprintfl (EUCAERROR, "stats integer overflow error (bump up the units?)\n");
        logprintfl (EUCAERROR, "   memory: max=%-10lld free=%-10lld\n", nc->mem_max, mem_free);
        logprintfl (EUCAERROR, "     disk: max=%-10lld free=%-10lld\n", nc->disk_max, disk_free);
        logprintfl (EUCAERROR, "    cores: max=%-10d free=%-10d\n", nc->cores_max, cores_free);
        logprintfl (EUCAERROR, "       INT_MAX=%-10d\n", INT_MAX);
        return 10;
    }
    
    res = allocate_resource ("OK", nc->mem_max, mem_free, nc->disk_max, disk_free, nc->cores_max, cores_free, "none");
    if (res == NULL) {
        logprintfl (EUCAERROR, "Out of memory\n");
        return 1;
    }
    *outRes = res;

    return OK;
}

static int
doPowerDown(	struct nc_state_t *nc,
		ncMetadata *ccMeta)
{
	char cmd[MAX_PATH];
	int rc;

	snprintf(cmd, MAX_PATH, "%s /usr/sbin/powernap-now", nc->rootwrap_cmd_path);
	logprintfl(EUCADEBUG, "saving power: %s\n", cmd);
	rc = system(cmd);
	rc = rc>>8;
	if (rc)
		logprintfl(EUCAERROR, "cmd failed: %d\n", rc);
  
	return OK;
}

static int
doStartNetwork(	struct nc_state_t *nc,
		ncMetadata *ccMeta, 
		char **remoteHosts, 
		int remoteHostsLen, 
		int port, 
		int vlan) {
	int rc, ret, i, status;
	char *brname;

	rc = vnetStartNetwork(nc->vnetconfig, vlan, NULL, NULL, &brname);
	if (rc) {
		ret = 1;
		logprintfl (EUCAERROR, "StartNetwork(): ERROR return from vnetStartNetwork %d\n", rc);
	} else {
		ret = 0;
		logprintfl (EUCAINFO, "StartNetwork(): SUCCESS return from vnetStartNetwork %d\n", rc);
		if (brname) free(brname);
	}
	logprintfl (EUCAINFO, "StartNetwork(): done\n");

	return (ret);
}

static int
doAttachVolume(	struct nc_state_t *nc,
		ncMetadata *meta,
		char *instanceId,
		char *volumeId,
		char *remoteDev,
		char *localDev)
{
	logprintfl(EUCAERROR, "no default for doAttachVolume!\n");
	return ERROR_FATAL;
}

static int
doDetachVolume(	struct nc_state_t *nc,
		ncMetadata *meta,
		char *instanceId,
		char *volumeId,
		char *remoteDev,
		char *localDev,
		int force)
{
	logprintfl(EUCAERROR, "no default for doDetachVolume!\n");
	return ERROR_FATAL;
}

struct bundling_params_t {
	ncInstance * instance;
	char * bucketName;
	char * filePrefix;
	char * walrusURL;
	char * userPublicKey;
	char * workPath; // work directory path
	char * diskPath; // disk file path
	char * eucalyptusHomePath; 
	long long sizeMb; // diskPath size
	char * ncBundleUploadCmd;
};

// helper for changing bundling task state and stateName together
static void change_bundling_state (ncInstance * instance, bundling_progress state)
{
	instance->bundleTaskState = state;
	strncpy (instance->bundleTaskStateName, bundling_progress_names [state], CHAR_BUFFER_SIZE);
}

// helper for cleaning up 
static int cleanup_bundling_task (ncInstance * instance, struct bundling_params_t * params, instance_states state, bundling_progress result)
{
	logprintfl (EUCAINFO, "cleanup_bundling_task: instance %s bundling task result=%s\n", instance->instanceId, bundling_progress_names [result]);
	sem_p (inst_sem);
	change_bundling_state (instance, result);
	if (state!=NO_STATE) // do not touch instance state (these are early failures, before we destroyed the domain)
		change_state (instance, state);
	sem_v (inst_sem);

	if (params) {
		if (params->workPath) {
			free_work_path (instance->instanceId, instance->userId, params->sizeMb);
			free (params->workPath);
		}
		if (params->bucketName) free (params->bucketName);
		if (params->filePrefix) free (params->filePrefix);
		if (params->walrusURL) free (params->walrusURL);
		if (params->userPublicKey) free (params->userPublicKey);
		if (params->diskPath) free (params->diskPath);
		if (params->eucalyptusHomePath) free (params->eucalyptusHomePath);
		if (params->ncBundleUploadCmd) free (params->ncBundleUploadCmd);
		free (params);
	}

	return (result==BUNDLING_SUCCESS)?OK:ERROR;
}

static void * bundling_thread (void *arg) 
{
	struct bundling_params_t * params = (struct bundling_params_t *)arg;
	ncInstance * instance = params->instance;

	logprintfl (EUCAINFO, "bundling_thread: started bundling instance %s\n", instance->instanceId);

	char dstDiskPath[MAX_PATH];
	snprintf(dstDiskPath, MAX_PATH, "%s/%s/%s/%s", scGetInstancePath(), params->instance->userId, params->instance->instanceId, params->filePrefix);

	int rc = rename(params->diskPath, dstDiskPath); // rename disk file to match input 'filePrefix'
	if (rc) {
		logprintfl(EUCAERROR, "bundling_thread: could not rename '%s' to specified filePrefix '%s'\n", params->diskPath, dstDiskPath);
	} else {
		// USAGE: euca-nc-bundle-upload -i <image_path> -d <working dir> -b <bucket>
	        int pid, status;
		char cmd[MAX_PATH];
		snprintf(cmd, MAX_PATH, 
				 "EC2_CERT=%s/var/lib/eucalyptus/keys/node-cert.pem "
				 "EC2_SECRET_KEY=HALOTHAR "
				 "EUCALYPTUS_CERT=%s/var/lib/eucalyptus/keys/cloud-cert.pem "
				 "S3_URL=%s "
				 "EC2_ACCESS_KEY=%s "
				 "EC2_USER_ID=%s "
				 "EUCA_CERT=%s/var/lib/eucalyptus/keys/node-cert.pem "
				 "EUCA_PRIVATE_KEY=%s/var/lib/eucalyptus/keys/node-pk.pem "
				 "%s -i %s -d %s -b %s", 
				 params->eucalyptusHomePath, 
				 params->eucalyptusHomePath, 
				 params->walrusURL, 
				 params->userPublicKey, 
				 "123456789012", 
				 params->eucalyptusHomePath, 
				 params->eucalyptusHomePath, 
				 params->ncBundleUploadCmd, dstDiskPath, params->workPath, params->bucketName);
		logprintfl(EUCADEBUG, "bundling_thread: running cmd '%s'\n", cmd);
		pid = fork();
		if (!pid) {
		  char buf[1024];

		  snprintf(buf, 1024, "%s/var/lib/eucalyptus/keys/node-cert.pem", params->eucalyptusHomePath);
		  setenv("EC2_CERT", buf, 1);

		  snprintf(buf, 1024, "IGNORED");
		  setenv("EC2_SECRET_KEY", buf, 1);

		  snprintf(buf, 1024, "%s/var/lib/eucalyptus/keys/cloud-cert.pem", params->eucalyptusHomePath);
		  setenv("EUCALYPTUS_CERT", buf, 1);

		  snprintf(buf, 1024, "%s", params->walrusURL);
		  setenv("S3_URL", buf, 1);
		  
		  snprintf(buf, 1024, "%s", params->userPublicKey);
		  setenv("EC2_ACCESS_KEY", buf, 1);

		  snprintf(buf, 1024, "123456789012");
		  setenv("EC2_USER_ID", buf, 1);

		  snprintf(buf, 1024, "%s/var/lib/eucalyptus/keys/node-cert.pem", params->eucalyptusHomePath);
		  setenv("EUCA_CERT", buf, 1);

		  snprintf(buf, 1024, "%s/var/lib/eucalyptus/keys/node-pk.pem", params->eucalyptusHomePath);
		  setenv("EUCA_PRIVATE_KEY", buf, 1);

		  exit(execl(params->ncBundleUploadCmd, params->ncBundleUploadCmd, "-i", dstDiskPath, "-d", params->workPath, "-b", params->bucketName, NULL));
		  //		  rc = system(cmd);
		  //		  rc = rc>>8;
		  //		  exit (rc);
		} else {
		  instance->bundlePid = pid;
		  rc = waitpid(pid, &status, 0);
		  if (WIFEXITED(status)) {
		    rc = WEXITSTATUS(status);
		  } else {
		    rc = -1;
		  }
		}

		if (rc==0) {
			cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_SUCCESS);
			logprintfl (EUCAINFO, "bundling_thread: finished bundling instance %s\n", instance->instanceId);
		} else if (rc == -1) {
		        // bundler child was cancelled (killed)
		        cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_CANCELLED);
			logprintfl (EUCAINFO, "bundling_thread: cancelled while bundling instance %s (rc=%d)\n", instance->instanceId, rc);
		} else {
			cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_FAILED);
			logprintfl (EUCAINFO, "bundling_thread: failed while bundling instance %s (rc=%d)\n", instance->instanceId, rc);
		}
	}

	return NULL;
}

static int
doBundleInstance(
	struct nc_state_t *nc,
	ncMetadata *meta,
	char *instanceId,
	char *bucketName,
	char *filePrefix,
	char *walrusURL,
	char *userPublicKey)
{
	// sanity checking
	if (instanceId==NULL
		|| bucketName==NULL
		|| filePrefix==NULL
		|| walrusURL==NULL
		|| userPublicKey==NULL) {
		logprintfl (EUCAERROR, "doBundleInstance: bundling instance called with invalid parameters\n");
		return ERROR;
	}

	// find the instance
	ncInstance * instance = find_instance(&global_instances, instanceId);
	if (instance==NULL) {
		logprintfl (EUCAERROR, "doBundleInstance: instance %s not found\n", instanceId);
		return ERROR;
	}

	// "marshall" thread parameters
	struct bundling_params_t * params = malloc (sizeof (struct bundling_params_t));
	if (params==NULL) 
		return cleanup_bundling_task (instance, params, NO_STATE, BUNDLING_FAILED);

	bzero (params, sizeof (struct bundling_params_t));
	params->instance = instance;
	params->bucketName = strdup (bucketName);
	params->filePrefix = strdup (filePrefix);
	params->walrusURL = strdup (walrusURL);
	params->userPublicKey = strdup (userPublicKey);
	params->eucalyptusHomePath = strdup (nc->home);
	params->ncBundleUploadCmd = strdup (nc->ncBundleUploadCmd);

	params->sizeMb = get_bundling_size (instanceId, instance->userId) / MEGABYTE;
	if (params->sizeMb<1)
		return cleanup_bundling_task (instance, params, NO_STATE, BUNDLING_FAILED);
	params->workPath = alloc_work_path (instanceId, instance->userId, params->sizeMb); // reserve work disk space for bundling
	if (params->workPath==NULL)
		return cleanup_bundling_task (instance, params, NO_STATE, BUNDLING_FAILED);
	params->diskPath = get_disk_path (instanceId, instance->userId); // path of the disk to bundle
	if (params->diskPath==NULL)
		return cleanup_bundling_task (instance, params, NO_STATE, BUNDLING_FAILED);

	// terminate the instance
    sem_p (inst_sem);
	int err = find_and_destroy_instance (instanceId, &instance);
	if (err!=OK) {
		sem_v (inst_sem);
		return err;
	}

	// while still under lock, update its state
	instance->bundlingTime = time (NULL);
	change_state (instance, BUNDLING);
	change_bundling_state (instance, BUNDLING_IN_PROGRESS);
    sem_v (inst_sem);

	// do the rest in a thread
	pthread_attr_t tattr;
	pthread_t tid;
	pthread_attr_init (&tattr);
	pthread_attr_setdetachstate (&tattr, PTHREAD_CREATE_DETACHED);
	if (pthread_create (&tid, &tattr, bundling_thread, (void *)params)!=0) {
		logprintfl (EUCAERROR, "doBundleInstance: failed to start VM budling thread\n");
		return cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_FAILED);
	}

	return OK;
}

static int
doCancelBundleTask(
	struct nc_state_t *nc,
	ncMetadata *meta,
	char *instanceId)
{
  ncInstance * instance = find_instance(&global_instances, instanceId);
  if (instance==NULL) {
    logprintfl (EUCAERROR, "doCancelBundleTask: instance %s not found\n", instanceId);
    return ERROR;
  } 
  //  logprintfl(EUCADEBUG, "WTF: pid=%d check=%d\n", instance->bundlePid, check_process(instance->bundlePid, "euca-nc-bundle-upload"));
  if (instance->bundlePid > 0 && !check_process(instance->bundlePid, "euca-nc-bundle-upload")) {
    logprintfl(EUCADEBUG, "doCancelBundleTask: found bundlePid '%d', sending kill signal...\n", instance->bundlePid);
    kill(instance->bundlePid, 9);
  }
  return(OK);
}

static int
doDescribeBundleTasks(
	struct nc_state_t *nc,
	ncMetadata *meta,
	char **instIds,
	int instIdsLen,
	bundleTask ***outBundleTasks,
	int *outBundleTasksLen)
{	
	if (instIdsLen < 1 || instIds == NULL) {
		logprintfl(EUCADEBUG, "doDescribeBundleTasks: input instIds empty\n");
		return ERROR;
	}
	
    *outBundleTasks = malloc(sizeof(bundleTask *) * instIdsLen); // maximum size
	if ((*outBundleTasks) == NULL) {
		return OUT_OF_MEMORY;
	}
    *outBundleTasksLen = 0; // we may return fewer than instIdsLen
	
	int i, j;
	for (i=0, j=0; i<instIdsLen; i++) {
		bundleTask * bundle = NULL;

		sem_p (inst_sem);		
		ncInstance * instance = find_instance(&global_instances, instIds[i]);
		if (instance != NULL) {
			bundle = malloc(sizeof(bundleTask));
			if (bundle == NULL) {
				logprintfl (EUCAERROR, "out of memory\n");
				return OUT_OF_MEMORY;
			}
			allocate_bundleTask (bundle, instIds[i], instance->bundleTaskStateName);
		}
		sem_v (inst_sem);
		
		if (bundle) {
			(*outBundleTasks)[j++] = bundle;
			(*outBundleTasksLen)++;
		}
	}
	
	return OK;
}

struct handlers default_libvirt_handlers = {
    .name = "default",
    .doInitialize        = doInitialize,
    .doDescribeInstances = doDescribeInstances,
    .doRunInstance       = doRunInstance,
    .doTerminateInstance = doTerminateInstance,
    .doRebootInstance    = doRebootInstance,
    .doGetConsoleOutput  = doGetConsoleOutput,
    .doDescribeResource  = doDescribeResource,
    .doStartNetwork      = doStartNetwork,
    .doPowerDown         = doPowerDown,
    .doAttachVolume      = doAttachVolume,
    .doDetachVolume      = doDetachVolume,
    .doBundleInstance    = doBundleInstance,
    .doCancelBundleTask  = doCancelBundleTask,
    .doDescribeBundleTasks    = doDescribeBundleTasks
};


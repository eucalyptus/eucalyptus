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
#define _FILE_OFFSET_BITS 64
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
		char *userData, char *launchIndex,
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
find_and_terminate_instance ( 
	char *instanceId, 
	ncInstance **instance_p,
	char destroy)
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
			if (destroy)
			  err = virDomainDestroy (dom);
			else 
			  err = virDomainShutdown (dom);
			sem_v (hyp_sem);
			if (err==0) {
                if (destroy)
                    logprintfl (EUCAINFO, "destroyed domain for instance %s\n", instanceId);
                else
                    logprintfl (EUCAINFO, "shutting down domain for instance %s\n", instanceId);
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
	err = find_and_terminate_instance (instanceId, &instance, 1);
	if (err!=OK) {
		sem_v(inst_sem);
		return err;
	}

	// change the state and let the monitoring_thread clean up state
	if (instance->state!=TEARDOWN) { // do not leave TEARDOWN
	  if (instance->state==STAGING) {
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
	ncInstance *instance, *tmp;
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
		//(* outInsts)[k++] = instance;
		tmp = (ncInstance *)malloc(sizeof(ncInstance));
    memcpy(tmp, instance, sizeof(ncInstance));
    (* outInsts)[k++] = tmp;
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
};


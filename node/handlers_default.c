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
#define _FILE_OFFSET_BITS 64 // so large-file support works on 32-bit systems
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
prep_location (virtualBootRecord * vbr, ncMetadata * meta, const char * typeName)
{
    int i;
    
    for (i=0; i<meta->servicesLen; i++) {
        serviceInfoType * service = &(meta->services[i]);
        if (strncmp(service->type, typeName, strlen(typeName)-3)==0 && service->urisLen>0) {
            char * l = vbr->resourceLocation + (strlen (typeName) + 3); // +3 for "://", so 'l' points past, e.g., "walrus:"
            snprintf (vbr->preparedResourceLocation, sizeof(vbr->preparedResourceLocation), "%s%s", service->uris[0], l); // TODO: for now we just pick the first one
            return OK;
        }
    }
    logprintfl (EUCAERROR, "failed to find service '%s' in eucalyptusMessage\n", typeName);
    return ERROR;
}

static int
doRunInstance(	struct nc_state_t *nc,
                ncMetadata *meta,
		char *uuid,
                char *instanceId,
                char *reservationId,
                virtualMachine *params, 
                char *imageId, char *imageURL,  // ignored
                char *kernelId, char *kernelURL, // ignored
                char *ramdiskId, char *ramdiskURL, // ignored
                char *keyName, 
                netConfig *netparams,
                char *userData, char *launchIndex, int expiryTime,
                char **groupNames, int groupNamesSize,
                ncInstance **outInst)
{
    ncInstance * instance = NULL;
    * outInst = NULL;
    pid_t pid;
    netConfig ncnet;
    int error = OK;

    memcpy(&ncnet, netparams, sizeof(netConfig));

    /* check as much as possible before forking off and returning */
    sem_p (inst_sem);
    instance = find_instance (&global_instances, instanceId);
    sem_v (inst_sem);
    if (instance) {
        logprintfl (EUCAFATAL, "Error: instance %s already running\n", instanceId);
        return 1; /* TODO: return meaningful error codes? */
    }
    if (!(instance = allocate_instance (uuid,
					instanceId, 
                                        reservationId,
                                        params, 
                                        instance_state_names[PENDING], 
                                        PENDING, 
                                        meta->userId, 
                                        &ncnet, keyName,
                                        userData, launchIndex, expiryTime, groupNames, groupNamesSize))) {
        logprintfl (EUCAFATAL, "Error: could not allocate instance struct\n");
        return ERROR;
    }
    instance->launchTime = time (NULL);

    // parse and sanity-check the virtual boot record
    int i, j;
    char parts [6][EUCA_MAX_VBRS]; // record partitions seen
    for (i=0, j=0; i<EUCA_MAX_VBRS && i<instance->params.virtualBootRecordLen; i++) {
        virtualBootRecord * vbr = &(instance->params.virtualBootRecord[i]);
        // get the type (the only mandatory field)
        if (strstr (vbr->typeName, "machine") == vbr->typeName) { 
            vbr->type = NC_RESOURCE_IMAGE; 
            instance->params.image = vbr;
        } else if (strstr (vbr->typeName, "kernel") == vbr->typeName) { 
            vbr->type = NC_RESOURCE_KERNEL; 
            instance->params.kernel = vbr;
        } else if (strstr (vbr->typeName, "ramdisk") == vbr->typeName) { 
            vbr->type = NC_RESOURCE_RAMDISK; 
            instance->params.ramdisk = vbr;
        } else if (strstr (vbr->typeName, "ephemeral") == vbr->typeName) { 
            vbr->type = NC_RESOURCE_EPHEMERAL; 
            if (strstr (vbr->typeName, "ephemeral0") == vbr->typeName) {
                instance->params.ephemeral0 = vbr;
            }
        } else if (strstr (vbr->typeName, "swap") == vbr->typeName) { 
            vbr->type = NC_RESOURCE_SWAP; 
            instance->params.swap = vbr;
        } else if (strstr (vbr->typeName, "ebs") == vbr->typeName) { 
            vbr->type = NC_RESOURCE_EBS;
        } else {
            logprintfl (EUCAERROR, "Error: failed to parse resource type '%s'\n", vbr->typeName);
	    goto error;
        }
        
        // identify the type of resource location from location string
        if (strcasestr (vbr->resourceLocation, "http://") == vbr->resourceLocation) { 
            vbr->locationType = NC_LOCATION_URL;
            strncpy (vbr->preparedResourceLocation, vbr->resourceLocation, sizeof(vbr->preparedResourceLocation));
        } else if (strcasestr (vbr->resourceLocation, "iqn://") == vbr->resourceLocation) {
            vbr->locationType = NC_LOCATION_IQN;
            // TODO: prep iqn location?
        } else if (strcasestr (vbr->resourceLocation, "aoe://") == vbr->resourceLocation) {
            vbr->locationType = NC_LOCATION_AOE;
            // TODO: prep aoe location?
        } else if (strcasestr (vbr->resourceLocation, "walrus://") == vbr->resourceLocation) {
            vbr->locationType = NC_LOCATION_WALRUS;
            error = prep_location (vbr, meta, "walrus");
        } else if (strcasestr (vbr->resourceLocation, "cloud://") == vbr->resourceLocation) {
            vbr->locationType = NC_LOCATION_CLC;
            error = prep_location (vbr, meta, "cloud");
        } else if (strcasestr (vbr->resourceLocation, "sc://") == vbr->resourceLocation) {//'sc' should be 'storage'
            vbr->locationType = NC_LOCATION_SC;
            error = prep_location (vbr, meta, "sc");
        } else if (strcasestr (vbr->resourceLocation, "none") == vbr->resourceLocation) { 
            if (vbr->type!=NC_RESOURCE_EPHEMERAL && vbr->type!=NC_RESOURCE_SWAP) {
                logprintfl (EUCAERROR, "Error: resourceLocation not specified for non-ephemeral resource '%s'\n", vbr->resourceLocation);
                goto error;
            }            
            vbr->locationType = NC_LOCATION_NONE;
        } else {
            logprintfl (EUCAERROR, "Error: failed to parse resource location '%s'\n", vbr->resourceLocation);
	    goto error;
        }
        if (error!=OK) {
            logprintfl (EUCAERROR, "Error: URL for resourceLocation '%s' is not in the message\n", vbr->resourceLocation);
            goto error;
        }

        // device can be 'none' only for kernel and ramdisk types
        if (!strcmp (vbr->guestDeviceName, "none")) {
            if (vbr->type!=NC_RESOURCE_KERNEL &&
                vbr->type!=NC_RESOURCE_RAMDISK) {
                logprintfl (EUCAERROR, "Error: guestDeviceName not specified for resource '%s'\n", vbr->resourceLocation);
                goto error;
            }

        } else { // should be a valid device
            // trim off "/dev/" prefix, if present, and verify the rest
            if (strstr (vbr->guestDeviceName, "/dev/") == vbr->guestDeviceName) {
                logprintfl (EUCAWARN, "Warning: trimming off invalid prefix '/dev/' from guestDeviceName '%s'\n", vbr->guestDeviceName);
                char buf [10];
                strncpy (buf, vbr->guestDeviceName + 5, sizeof (buf));
                strncpy (vbr->guestDeviceName, buf, sizeof (vbr->guestDeviceName));
            }
            if (strlen (vbr->guestDeviceName)<3) {
                logprintfl (EUCAERROR, "Error: invalid guestDeviceName '%s'\n", vbr->guestDeviceName);
                goto error;
            }
            {
                char t = vbr->guestDeviceName [0];
                char d = vbr->guestDeviceName [1];
                char n = vbr->guestDeviceName [2];
                long long int p = 0;
                if (strlen (vbr->guestDeviceName)>3) {
                    errno = 0;
                    p = strtoll (vbr->guestDeviceName + 3, NULL, 10);
                    if (errno!=0) { 
                        logprintfl (EUCAERROR, "Error: failed to parse partition number in guestDeviceName '%s'\n", vbr->guestDeviceName);
                        goto error; 
                    } 
                    if (p<1 || p>99) {
                        logprintfl (EUCAERROR, "Error: unexpected partition number '%d' in guestDeviceName '%s'\n", p, vbr->guestDeviceName);
                        goto error;
                    }
                }
                if (t!='h' && t!='s' && t!='f' && t!='v') {
                    logprintfl (EUCAERROR, "Error: failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                    goto error; 
                }
                if (d!='d') {
                    logprintfl (EUCAERROR, "Error: failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                    goto error; 
                }
                if (!(n>='a' && n<='z')) {
                    logprintfl (EUCAERROR, "Error: failed to parse disk type guestDeviceName '%s'\n", vbr->guestDeviceName);
                    goto error; 
                }
                snprintf (parts[j++], 6, "%c%c%c%0lld", t, d, n, p);
            }
        }

        // parse ID
        if (strlen (vbr->id)<4) {
            logprintfl (EUCAERROR, "Error: failed to parse VBR resource ID '%s' (use 'none' when no ID)\n", vbr->id);
            goto error;
        }

        // parse disk formatting instructions (none = do not format)
        if (strstr (vbr->formatName, "none") == vbr->formatName) { vbr->format = NC_FORMAT_NONE;
        } else if (strstr (vbr->formatName, "ext2") == vbr->formatName) { vbr->format = NC_FORMAT_EXT2;
        } else if (strstr (vbr->formatName, "ext3") == vbr->formatName) { vbr->format = NC_FORMAT_EXT3;
        } else if (strstr (vbr->formatName, "ntfs") == vbr->formatName) { vbr->format = NC_FORMAT_NTFS;
        } else if (strstr (vbr->formatName, "swap") == vbr->formatName) { vbr->format = NC_FORMAT_SWAP;
        } else {
            logprintfl (EUCAERROR, "Error: failed to parse resource format '%s'\n", vbr->formatName);
            goto error;
        }
        if (vbr->type==NC_RESOURCE_EPHEMERAL || vbr->type==NC_RESOURCE_SWAP) { // TODO: should we allow ephemeral/swap that reside remotely?
            if (vbr->size<1) {
                logprintfl (EUCAERROR, "Error: invalid size '%d' for ephemeral resource '%s'\n", vbr->size, vbr->resourceLocation);
                goto error;
            }
        } else {
	    //            if (vbr->size!=1 || vbr->format!=NC_FORMAT_NONE) { // TODO: dan check with dmitrii
	    if (vbr->format!=NC_FORMAT_NONE) {
                logprintfl (EUCAERROR, "Error: invalid size '%d' or format '%s' for non-ephemeral resource '%s'\n", vbr->size, vbr->formatName, vbr->resourceLocation);
                goto error;
            }
        }
    }
    // run through partitions seen and look for gaps
    qsort (parts, j, 6, (int(*)(const void *, const void *))strcmp);
    int k;
    for (k=0; k<j; k++) {
        logprintfl (EUCADEBUG, "Found partition %s\n", parts [k]); // TODO: verify no gaps in partitions
    }

    /*
    // TODO: dan ask dmitrii
    for (i=0; i<EUCA_MAX_VBRS && i < params->virtualBootRecordLen; i++) {
      virtualBootRecord * vbr = &(params->virtualBootRecord[i]);
      logprintfl(EUCADEBUG, "VBR(%d): %s %s %s\n", i, vbr->resourceLocation, vbr->formatName, vbr->typeName);
      if (vbr->type == NC_RESOURCE_KERNEL && vbr->locationType == NC_LOCATION_URL) {
	instance->params.kernel = vbr;
	logprintfl(EUCADEBUG, "DAN: kernel info: %s %s\n", instance->params.kernel->resourceLocation, instance->params.kernel->preparedResourceLocation);
      } else if (vbr->type == NC_RESOURCE_RAMDISK && vbr->locationType == NC_LOCATION_URL) {
	instance->params.ramdisk = vbr;
	logprintfl(EUCADEBUG, "DAN: ramdisk info: %s %s\n", instance->params.ramdisk->resourceLocation, instance->params.ramdisk->preparedResourceLocation);
      } else if (vbr->type == NC_RESOURCE_IMAGE && vbr->locationType == NC_LOCATION_URL) {
	instance->params.image = vbr;
	logprintfl(EUCADEBUG, "DAN: image info: %s %s\n", instance->params.image->resourceLocation, instance->params.image->preparedResourceLocation);
      }
    }
    */

    change_state(instance, STAGING);

    sem_p (inst_sem); 
    error = add_instance (&global_instances, instance);
    sem_v (inst_sem);
    if ( error ) {
        logprintfl (EUCAFATAL, "Error: could not save instance struct\n");
        goto error;
    }

    // do the potentially long tasks in a thread
    pthread_attr_t* attr = (pthread_attr_t*) malloc(sizeof(pthread_attr_t));
    if (!attr) { 
        logprintfl (EUCAFATAL, "Error: out of memory\n");
        goto error;
    }
    pthread_attr_init(attr);
    pthread_attr_setdetachstate(attr, PTHREAD_CREATE_DETACHED);
    
    if ( pthread_create (&(instance->tcb), attr, startup_thread, (void *)instance) ) {
        pthread_attr_destroy(attr);
        logprintfl (EUCAFATAL, "failed to spawn a VM startup thread\n");
        sem_p (inst_sem);
        remove_instance (&global_instances, instance);
        sem_v (inst_sem);
	if (attr) free(attr);
        goto error;
    }
    pthread_attr_destroy(attr);
    if (attr) free(attr);

    * outInst = instance;
    return 0;

 error:
    free_instance (&instance);
    return ERROR;
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
        struct nc_state_t *nc_state,
        ncMetadata *meta,
	char *instanceId, 
	ncInstance **instance_p,
	char destroy)
{
	ncInstance *instance;
	virConnectPtr *conn;
	int err;
	int i;

	instance = find_instance(&global_instances, instanceId);
	if (instance == NULL) 
		return NOT_FOUND;
	* instance_p = instance;

        /* detach all attached volumes */
        for (i=0 ; i < instance->volumesSize; ++i) {
	int ret = OK;
        ncVolume *volume = &instance->volumes[i];
	logprintfl (EUCAINFO, "Detaching volume on terminate: %s\n", volume->volumeId);
	if (nc_state->H->doDetachVolume) 
		ret = nc_state->H->doDetachVolume(nc_state, meta, instanceId, volume->volumeId, volume->remoteDev, volume->localDevReal, 0, 0);
	else
		ret = nc_state->D->doDetachVolume(nc_state, meta, instanceId, volume->volumeId, volume->remoteDev, volume->localDevReal, 0, 0);
	if(ret != OK)
		return ret;
	}

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
	err = find_and_terminate_instance (nc, meta, instanceId, &instance, 1);
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

	logprintfl(EUCADEBUG, "doDescribeInstances: excerpt: userId=%s correlationId=%s epoch=%d services[0].name=%s services[0].type=%s services[0].uris[0]=%s\n", SP(meta->userId), SP(meta->correlationId), meta->epoch, SP(meta->services[0].name), SP(meta->services[0].type), SP(meta->services[0].uris[0])); 

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
doAssignAddress(struct nc_state_t *nc,
		ncMetadata *ccMeta,
		char *instanceId,
		char *publicIp)
{
  int ret = OK;
  ncInstance *instance=NULL;

  if (instanceId == NULL || publicIp == NULL) {
    logprintfl(EUCAERROR, "doAssignAddress(): bad input params\n");
    return(ERROR);
  }

  sem_p (inst_sem); 
  instance = find_instance(&global_instances, instanceId);
  if ( instance ) {
    snprintf(instance->ncnet.publicIp, 24, "%s", publicIp);  
  }
  sem_v (inst_sem);
  
  return ret;
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
		char *uuid,
		char **remoteHosts, 
		int remoteHostsLen, 
		int port, 
		int vlan) {
	int rc, ret, i, status;
	char *brname;

	rc = vnetStartNetwork(nc->vnetconfig, vlan, NULL, NULL, NULL, &brname);
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

// helper for changing bundling task state and stateName together                                                              
static void change_createImage_state (ncInstance * instance, createImage_progress state)
{
  instance->createImageTaskState = state;
  strncpy (instance->createImageTaskStateName, createImage_progress_names [state], CHAR_BUFFER_SIZE);
}

// helper for cleaning up 
static int cleanup_createImage_task (ncInstance * instance, struct createImage_params_t * params, instance_states state, createImage_progress result)
{
        char cmd[MAX_PATH];
	char buf[MAX_PATH];
	int rc;
	logprintfl (EUCAINFO, "cleanup_createImage_task: instance %s createImage task result=%s\n", instance->instanceId, createImage_progress_names [result]);
	sem_p (inst_sem);
	change_createImage_state (instance, result);
	if (state!=NO_STATE) // do not touch instance state (these are early failures, before we destroyed the domain)
		change_state (instance, state);
	sem_v (inst_sem);

	if (params) {
	        // if the result was failed or cancelled, clean up walrus state
	        if (result == CREATEIMAGE_FAILED || result == CREATEIMAGE_CANCELLED) {
		}
		if (params->workPath) {
			free_work_path (instance->instanceId, instance->userId, params->sizeMb);
			free (params->workPath);
		}
		if (params->volumeId) free (params->volumeId);
		if (params->remoteDev) free (params->remoteDev);
		if (params->diskPath) free (params->diskPath);
		if (params->eucalyptusHomePath) free (params->eucalyptusHomePath);
		free (params);
	}

	return (result==CREATEIMAGE_SUCCESS)?OK:ERROR;
}

static void * createImage_thread (void *arg) 
{
	struct createImage_params_t * params = (struct createImage_params_t *)arg;
	ncInstance * instance = params->instance;
	char cmd[MAX_PATH];
	char buf[MAX_PATH];
	int rc;

	logprintfl (EUCAINFO, "createImage_thread: waiting for instance %s to shut down\n", instance->instanceId);
	// wait until monitor thread changes the state of the instance instance 
	if (wait_state_transition (instance, CREATEIMAGE_SHUTDOWN, CREATEIMAGE_SHUTOFF)) { 
	  if (instance->createImageCanceled) { // cancel request came in while the instance was shutting down
	    logprintfl (EUCAINFO, "createImage_thread: cancelled while createImage instance %s\n", instance->instanceId);
	    cleanup_createImage_task (instance, params, SHUTOFF, CREATEIMAGE_CANCELLED);
	  } else {
	    logprintfl (EUCAINFO, "createImage_thread: failed while createImage instance %s\n", instance->instanceId);
	    cleanup_createImage_task (instance, params, SHUTOFF, CREATEIMAGE_FAILED);
	  }
	  return NULL;
	}

	logprintfl (EUCAINFO, "createImage_thread: started createImage instance %s\n", instance->instanceId);
	{
	  rc = 0;
	  if (rc==0) {
	    cleanup_createImage_task (instance, params, SHUTOFF, CREATEIMAGE_SUCCESS);
	    logprintfl (EUCAINFO, "createImage_thread: finished createImage instance %s\n", instance->instanceId);
	  } else if (rc == -1) {
	    // bundler child was cancelled (killed)
	    cleanup_createImage_task (instance, params, SHUTOFF, CREATEIMAGE_CANCELLED);
	    logprintfl (EUCAINFO, "createImage_thread: cancelled while createImage instance %s (rc=%d)\n", instance->instanceId, rc);
	  } else {
	    cleanup_createImage_task (instance, params, SHUTOFF, CREATEIMAGE_FAILED);
	    logprintfl (EUCAINFO, "createImage_thread: failed while createImage instance %s (rc=%d)\n", instance->instanceId, rc);
	  }
	}
	
	return NULL;
}

static int
doCreateImage(	struct nc_state_t *nc,
		ncMetadata *meta,
		char *instanceId,
		char *volumeId,
		char *remoteDev)
{
	logprintfl (EUCAINFO, "CreateImage(): invoked\n");

	// sanity checking
	if (instanceId==NULL
	    || remoteDev==NULL) {
	  logprintfl (EUCAERROR, "CreateImage: called with invalid parameters\n");
	  return ERROR;
	}

	// find the instance
	ncInstance * instance = find_instance(&global_instances, instanceId);
	if (instance==NULL) {
		logprintfl (EUCAERROR, "CreateImage: instance %s not found\n", instanceId);
		return ERROR;
	}

	// "marshall" thread parameters
	struct createImage_params_t * params = malloc (sizeof (struct createImage_params_t));
	if (params==NULL) 
		return cleanup_createImage_task (instance, params, NO_STATE, CREATEIMAGE_FAILED);

	bzero (params, sizeof (struct createImage_params_t));
	params->instance = instance;
	params->volumeId = strdup (volumeId);
	params->remoteDev = strdup (remoteDev);

	params->sizeMb = get_bundling_size (instanceId, instance->userId) / MEGABYTE;
	if (params->sizeMb<1)
		return cleanup_createImage_task (instance, params, NO_STATE, CREATEIMAGE_FAILED);
	params->workPath = alloc_work_path (instanceId, instance->userId, params->sizeMb); // reserve work disk space for bundling
	if (params->workPath==NULL)
		return cleanup_createImage_task (instance, params, NO_STATE, CREATEIMAGE_FAILED);
	params->diskPath = get_disk_path (instanceId, instance->userId); // path of the disk to bundle
	if (params->diskPath==NULL)
		return cleanup_createImage_task (instance, params, NO_STATE, CREATEIMAGE_FAILED);

	// terminate the instance
	sem_p (inst_sem);
	instance->createImageTime = time (NULL);
	change_state (instance, CREATEIMAGE_SHUTDOWN);
	change_createImage_state (instance, CREATEIMAGE_IN_PROGRESS);
	
	int err = find_and_terminate_instance (nc, meta, instanceId, &instance, 1);
	if (err!=OK) {
	  sem_v (inst_sem);
	  if (params) free(params);
	  return err;
	}
	sem_v (inst_sem);
	
	// do the rest in a thread
	pthread_attr_t tattr;
	pthread_t tid;
	pthread_attr_init (&tattr);
	pthread_attr_setdetachstate (&tattr, PTHREAD_CREATE_DETACHED);
	if (pthread_create (&tid, &tattr, createImage_thread, (void *)params)!=0) {
		logprintfl (EUCAERROR, "CreateImage: failed to start VM createImage thread\n");
		return cleanup_createImage_task (instance, params, SHUTOFF, CREATEIMAGE_FAILED);
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
    .doAssignAddress     = doAssignAddress,
    .doPowerDown         = doPowerDown,
    .doAttachVolume      = doAttachVolume,
    .doDetachVolume      = doDetachVolume,
    .doCreateImage       = doCreateImage
};


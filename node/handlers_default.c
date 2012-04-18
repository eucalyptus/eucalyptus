// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

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
#define _FILE_OFFSET_BITS 64 // so large-file support works on 32-bit systems
#define __USE_GNU /* strnlen */
#include <stdio.h>
#include <stdlib.h>
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
#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>

#include "ipc.h"
#include "misc.h"
#include "handlers.h"
#include "backing.h"
#include "eucalyptus.h"
#include "vnetwork.h"
#include "euca_auth.h"
#include "vbr.h"
#include "iscsi.h"
#include "xml.h"
#include "hooks.h"

#include "windows-bundle.h"

// coming from handlers.c
extern sem * hyp_sem;
extern sem * inst_sem;
extern bunchOfInstances * global_instances;

static int
doInitialize (struct nc_state_t *nc) 
{
	return OK;
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
                char *ownerId, char *accountId,
                char *keyName, 
                netConfig *netparams,
                char *userData, char *launchIndex, char *platform, int expiryTime,
                char **groupNames, int groupNamesSize,
                ncInstance **outInst)
{
    ncInstance * instance = NULL;
    * outInst = NULL;
    pid_t pid;
    netConfig ncnet;

    memcpy(&ncnet, netparams, sizeof(netConfig));

    // check as much as possible before forking off and returning
    sem_p (inst_sem);
    instance = find_instance (&global_instances, instanceId);
    sem_v (inst_sem);
    if (instance) {
        if (instance->state==TEARDOWN) { // fully cleaned up, so OK to revive it, e.g., with euca-start-instance
            remove_instance (&global_instances, instance);
            free_instance (&instance);
        } else {
            logprintfl (EUCAERROR, "[%s] error: instance already running\n", instanceId);
            return 1; /* TODO: return meaningful error codes? */
        }
    }
    if (!(instance = allocate_instance (uuid,
                                        instanceId, 
                                        reservationId,
                                        params, 
                                        instance_state_names[PENDING], 
                                        PENDING, 
                                        meta->userId, 
                                        ownerId, accountId,
                                        &ncnet, keyName,
                                        userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize))) {
        logprintfl (EUCAERROR, "[%s] error: could not allocate instance struct\n", instanceId);
        return ERROR;
    }
    instance->launchTime = time (NULL);

    // parse and sanity-check the virtual boot record
    if (vbr_parse (&(instance->params), meta) != OK)
            goto error;

    change_state(instance, STAGING);

    sem_p (inst_sem); 
    int error = add_instance (&global_instances, instance);
    sem_v (inst_sem);
    if ( error ) {
        logprintfl (EUCAERROR, "[%s] error: could not save instance struct\n", instanceId);
        goto error;
    }

    // do the potentially long tasks in a thread
    pthread_attr_t* attr = (pthread_attr_t*) malloc(sizeof(pthread_attr_t));
    if (!attr) { 
        logprintfl (EUCAERROR, "[%s] error: out of memory\n", instanceId);
        goto error;
    }
    pthread_attr_init(attr);
    pthread_attr_setdetachstate(attr, PTHREAD_CREATE_DETACHED);
    
    if ( pthread_create (&(instance->tcb), attr, startup_thread, (void *)instance) ) {
        pthread_attr_destroy(attr);
        logprintfl (EUCAERROR, "[%s] failed to spawn a VM startup thread\n", instanceId);
        sem_p (inst_sem);
        remove_instance (&global_instances, instance);
        sem_v (inst_sem);
	if (attr) free(attr);
        goto error;
    }
    pthread_attr_destroy(attr);
    if (attr) free(attr);

    * outInst = instance;
    return OK;

 error:
    free_instance (&instance);
    return ERROR;
}

static int
doRebootInstance(struct nc_state_t *nc, ncMetadata *meta, char *instanceId) 
{    
	logprintfl(EUCAERROR, "[%s] internal error: no default for doRebootInstance!\n", instanceId);
	return ERROR_FATAL;
}

static int
doGetConsoleOutput(	struct nc_state_t *nc, 
                    ncMetadata *meta,
                    char *instanceId,
                    char **consoleOutput)
{
	logprintfl(EUCAERROR, "[%s] internal error: no default for doGetConsoleOutput!\n", instanceId);
	return ERROR_FATAL;
}

// finds instance by ID and destroys it on the hypervisor
// NOTE: this must be called with inst_sem semaphore held
int 
find_and_terminate_instance ( 
                             struct nc_state_t *nc_state,
                             ncMetadata *meta,
                             char *instanceId, 
                             int force,
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
    
    // detach all attached volumes
    for (i=0; i < EUCA_MAX_VOLUMES; ++i) {
        ncVolume * volume = &instance->volumes[i];
        if (! is_volume_used (volume))
            continue;

        int ret;
        logprintfl (EUCAINFO, "[%s] detaching volume %s, force=%d on termination\n", instanceId, volume->volumeId, force);
        if (nc_state->H->doDetachVolume) {
            ret = nc_state->H->doDetachVolume(nc_state, meta, instanceId, volume->volumeId, volume->remoteDev, volume->localDevReal, 0, 0);
        } else {
            ret = nc_state->D->doDetachVolume(nc_state, meta, instanceId, volume->volumeId, volume->remoteDev, volume->localDevReal, 0, 0);
        }

        // do our best to detach, then proceed
        if ((ret != OK)) {
            if (nc_state->H->doDetachVolume) {
                ret = nc_state->H->doDetachVolume(nc_state, meta, instanceId, volume->volumeId, volume->remoteDev, volume->localDevReal, 1, 0);
            } else {
                ret = nc_state->D->doDetachVolume(nc_state, meta, instanceId, volume->volumeId, volume->remoteDev, volume->localDevReal, 1, 0);
            }
        }
        
        if ((ret != OK) && (force == 0)) {
            logprintfl(EUCAWARN, "[%s] detaching of volume on terminate failed\n", instanceId);
            //            return ret;
        }
	}

	// try stopping the domain
	conn = check_hypervisor_conn();
	if (conn) {
        sem_p(hyp_sem);
        virDomainPtr dom = virDomainLookupByName(*conn, instanceId);
		sem_v(hyp_sem);
		if (dom) {
			// protect 'destroy' commands as we do with 'create', just in case
			sem_p (hyp_sem);
			if (destroy)
                err = virDomainDestroy (dom);
			else 
                err = virDomainShutdown (dom);
			sem_v (hyp_sem);
			if (err==0) {
                if (destroy)
                    logprintfl (EUCAINFO, "[%s] destroying instance\n", instanceId);
                else
                    logprintfl (EUCAINFO, "[%s] shutting down instance\n", instanceId);
			}
			sem_p(hyp_sem);
			virDomainFree(dom); // TODO: necessary?
			sem_v(hyp_sem);
		} else {
			if (instance->state != BOOTING && instance->state != STAGING && instance->state != TEARDOWN)
				logprintfl (EUCAWARN, "[%s] warning: instance to be terminated not running on hypervisor\n", instanceId);
		}
	} 
	return OK;
}

static int
doTerminateInstance( struct nc_state_t *nc,
                     ncMetadata *meta,
                     char *instanceId,
                     int force,
                     int *shutdownState,
                     int *previousState)
{
	ncInstance *instance;
	int err;
    
	sem_p (inst_sem);
	err = find_and_terminate_instance (nc, meta, instanceId, force, &instance, 1);
	if (err!=OK) {
		sem_v(inst_sem);
		return err;
	}
    
	// change the state and let the monitoring_thread clean up state
	if (instance->state!=TEARDOWN && instance->state!=CANCELED) { // do not leave TEARDOWN (cleaned up) or CANCELED (already trying to terminate)
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
doDescribeInstances( struct nc_state_t *nc,
                     ncMetadata *meta,
                     char **instIds,
                     int instIdsLen,
                     ncInstance ***outInsts,
                     int *outInstsLen)
{
	ncInstance *instance, *tmp;
	int total, i, j, k;
    
	logprintfl(EUCADEBUG, "doDescribeInstances: userId=%s correlationId=%s epoch=%d services[0].name=%s services[0].type=%s services[0].uris[0]=%s\n", 
               SP(meta->userId), 
               SP(meta->correlationId), 
               meta->epoch, 
               SP(meta->services[0].name), 
               SP(meta->services[0].type), 
               SP(meta->services[0].uris[0])); 
    
	*outInstsLen = 0;
	*outInsts = NULL;
    
	sem_p (inst_sem);
	if (instIdsLen == 0) // describe all instances
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
		// only pick ones the user (or admin) is allowed to see
		if (strcmp(meta->userId, nc->admin_user_id) 
            && strcmp(meta->userId, instance->userId))
			continue;
        
		if (instIdsLen > 0) {
			for (j=0; j < instIdsLen; j++)
				if (!strcmp(instance->instanceId, instIds[j]))
					break;
            
			if (j >= instIdsLen)
				// instance of no relevance right now
				continue;
		}
		// (* outInsts)[k++] = instance;
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
    
    // stats to re-calculate now
    long long mem_free;
    long long disk_free;
    int cores_free;

    // intermediate sums 
    long long sum_mem = 0;  // for known domains: sum of requested memory
    long long sum_disk = 0; // for known domains: sum of requested disk sizes
    int sum_cores = 0;      // for known domains: sum of requested cores
    
    *outRes = NULL;
    sem_p (inst_sem); 
    while ((inst=get_instance(&global_instances))!=NULL) {
        if (inst->state == TEARDOWN) continue; // they don't take up resources
        sum_mem += inst->params.mem;
        sum_disk += (inst->params.disk);
        sum_cores += inst->params.cores;
    }
    sem_v (inst_sem);
    
    disk_free = nc->disk_max - sum_disk;
    if ( disk_free < 0 ) disk_free = 0; // should not happen
    
    mem_free = nc->mem_max - sum_mem;
    if ( mem_free < 0 ) mem_free = 0; // should not happen

    cores_free = nc->cores_max - sum_cores; // TODO: should we -1 for dom0?
    if ( cores_free < 0 ) cores_free = 0; // due to timesharing 
    
    // check for potential overflow - should not happen
    if (nc->mem_max > INT_MAX ||
        mem_free > INT_MAX ||
        nc->disk_max > INT_MAX ||
        disk_free > INT_MAX) {
        logprintfl (EUCAERROR, "stats integer overflow error (bump up the units?)\n");
        logprintfl (EUCAERROR, "   memory: max=%-10lld free=%-10lld\n", nc->mem_max, mem_free);
        logprintfl (EUCAERROR, "     disk: max=%-10lld free=%-10lld\n", nc->disk_max, disk_free);
        logprintfl (EUCAERROR, "    cores: max=%-10lld free=%-10d\n", nc->cores_max, cores_free);
        logprintfl (EUCAERROR, "       INT_MAX=%-10d\n", INT_MAX);
        return 10;
    }
    
    res = allocate_resource ("OK", nc->iqn, nc->mem_max, mem_free, nc->disk_max, disk_free, nc->cores_max, cores_free, "none");
    if (res == NULL) {
        logprintfl (EUCAERROR, "error: doDescribeResouce: out of memory\n");
        return 1;
    }
    *outRes = res;
	logprintfl(EUCADEBUG, "doDescribeResource: cores=%d/%lld mem=%lld/%lld disk=%lld/%lld iqn=%s\n", 
               cores_free, nc->cores_max,
               mem_free, nc->mem_max,
               disk_free, nc->disk_max,
               nc->iqn);

    return OK;
}

static int
doAssignAddress( struct nc_state_t *nc,
                 ncMetadata *ccMeta,
                 char *instanceId,
                 char *publicIp)
{
    int ret = OK;
    ncInstance *instance=NULL;
    
    if (instanceId == NULL || publicIp == NULL) {
        logprintfl(EUCAERROR, "[%s] error: doAssignAddress: bad input params\n", instanceId);
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
doPowerDown( struct nc_state_t *nc,
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
                int vlan) 
{
	int rc, ret, i, status;
	char *brname;
    
	rc = vnetStartNetwork(nc->vnetconfig, vlan, NULL, NULL, NULL, &brname);
	if (rc) {
		ret = 1;
		logprintfl (EUCAERROR, "StartNetwork: ERROR return from vnetStartNetwork return=%d\n", rc);
	} else {
		ret = 0;
		logprintfl (EUCAINFO, "StartNetwork: SUCCESS return from vnetStartNetwork\n");
		if (brname) free(brname);
	}
    
	return (ret);
}

// because libvirt detach has a bug in some version (which one?)
// this helper detaches volumes using 'virsh', bypassing libvirt
static int xen_detach_helper (struct nc_state_t *nc, char *instanceId, char *localDevReal, char *xml) 
{
    int err, rc;
    
    pid_t pid = fork();
    if (!pid) {
        char tmpfile[MAX_PATH];
        snprintf(tmpfile, 32, "/tmp/detachxml.XXXXXX");
        int fd = safe_mkstemp(tmpfile);

	char devReal[32];
	char *tmp = strstr(xml, "<target");
	if(tmp==NULL){
	    logprintfl(EUCAERROR, "'<target' not found in the device xml\n"); 
	    return -1;
        }
	tmp = strstr(tmp, "dev=\"");
        if(tmp==NULL){
            logprintfl(EUCAERROR, "'<target dev' not found in the device xml\n");     
            return -1;
        }
        snprintf(devReal, 32, "%s", tmp+strlen("dev=\""));
        for(int i=0;i<32; i++){
	     if(devReal[i]=='\"'){
                for(;i<32; i++)
		      devReal[i] = '\0';
	     }
        }	

        if (fd > 0) {
            write(fd, xml, strlen(xml));
            close(fd);

            char cmd[MAX_PATH];
            snprintf(cmd, MAX_PATH, "%s %s `which virsh` %s %s %s", 
                     nc->detach_cmd_path, // TODO: does this work?
                     nc->rootwrap_cmd_path, 
                     instanceId, 
                     devReal, 
                     tmpfile);
            logprintfl(EUCAINFO, "%s\n", cmd);
            rc = system(cmd);
            rc = rc>>8;
            unlink(tmpfile);
        } else {
            logprintfl(EUCAERROR, "xen_detach_helper(): could not write to tmpfile for detach XML: %s\n", tmpfile);
            rc = 1;
        } 
        exit(rc);

    } else { // parent or failed to fork
        int status;
        rc = timewait(pid, &status, 15);
        if (WEXITSTATUS(status)) {
            logprintfl(EUCAERROR, "xen_detach_helper(): failed to sucessfully run detach helper\n");
            err = 1;
        } else {
            err = 0;
        }
    }
    return(err);
}

static int
doAttachVolume (	struct nc_state_t *nc,
 			ncMetadata *meta,
 			char *instanceId,
 			char *volumeId,
 			char *remoteDev,
 			char *localDev)
 {
     int ret = OK;
     int is_iscsi_target = 0;
     int have_remote_device = 0;
     
     char * tagBuf;
     char * localDevName;
     char localDevReal[32], localDevTag[256], remoteDevReal[32];
     if (!strcmp (nc->H->name, "xen")) {
         tagBuf = NULL;
         localDevName = localDevReal;
     } else if (!strcmp (nc->H->name, "kvm")) {
         tagBuf = localDevTag;
         localDevName = localDevTag;
     } else {
         logprintfl (EUCAERROR, "AttachVolume(): unknown hypervisor type '%s'\n", nc->H->name);
         return ERROR;
     }
     
     // sets localDevReal to the file name from the device path 
     // and, for KVM, sets localDevTag to the "unknown" string
     ret = convert_dev_names (localDev, localDevReal, tagBuf);
     if (ret)
         return ret;
     
     // find the instance record
     sem_p (inst_sem); 
     ncInstance *instance = find_instance (&global_instances, instanceId);
     sem_v (inst_sem);
     if ( instance == NULL ) 
         return NOT_FOUND;
     
     // try attaching to hypervisor
     virConnectPtr *conn = check_hypervisor_conn();
     if (conn==NULL) {
         logprintfl(EUCAERROR, "AttachVolume(): cannot get connection to hypervisor\n");
         return ERROR;
     }
     
     // find domain on hypervisor
     sem_p (hyp_sem);
     virDomainPtr dom = virDomainLookupByName (*conn, instanceId);
     sem_v (hyp_sem);
     if (dom==NULL) {
         if (instance->state != BOOTING && instance->state != STAGING) {
             logprintfl (EUCAWARN, "AttachVolume(): domain %s not running on hypervisor, cannot attach device\n", instanceId);
         }
         return ERROR;
     }
     
     // mark volume as 'attaching'
     ncVolume * volume;
     sem_p (inst_sem);
     volume = save_volume (instance, volumeId, remoteDev, localDevName, localDevReal, VOL_STATE_ATTACHING);
     save_instance_struct (instance);
     sem_v (inst_sem);
     if (!volume) {
         logprintfl (EUCAERROR, "AttachVolume(): failed to update the volume record, aborting volume attachment\n");
         return ERROR;
     }
     
     // do iscsi connect shellout if remoteDev is an iSCSI target
     if (check_iscsi (remoteDev)) {
         char *remoteDevStr=NULL;
         is_iscsi_target = 1;
         
         // get credentials, decrypt them, login into target
         remoteDevStr = connect_iscsi_target(remoteDev);
         if (!remoteDevStr || !strstr(remoteDevStr, "/dev")) {
             logprintfl(EUCAERROR, "AttachVolume(): failed to connect to iscsi target\n");
             remoteDevReal[0] = '\0';
         } else { 
             logprintfl(EUCADEBUG, "[%s] attached iSCSI target of host device '%s'\n", instanceId, remoteDevStr);
             snprintf(remoteDevReal, 32, "%s", remoteDevStr);
             have_remote_device = 1;
         }
         if (remoteDevStr) 
             free(remoteDevStr);
     } else {
         snprintf(remoteDevReal, 32, "%s", remoteDev);
         have_remote_device = 1;
     }
     
     // something went wrong above, abort
     if (!have_remote_device) {
         ret = ERROR;
         goto release;
     }
     
     // make sure there is a block device
     if (check_block (remoteDevReal)) {
         logprintfl(EUCAERROR, "AttachVolume(): cannot verify that host device '%s' is available for hypervisor attach\n", remoteDevReal);
         ret = ERROR;
         goto release;
     } 
     
     // generate XML for libvirt attachment request
     char xml [1024];
     if (gen_libvirt_attach_xml (volumeId, instance, localDevReal, remoteDevReal, xml, sizeof(xml))) {
         logprintfl(EUCAERROR, "AttachVolume(): could not produce attach device xml\n");
         ret = ERROR;
         goto release;
     }

     // invoke hooks
     char path [MAX_PATH];
     snprintf (path, sizeof (path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volumeId);
     if (call_hooks (NC_EVENT_PRE_ATTACH, path)) {
         logprintfl (EUCAERROR, "[%s] cancelled volume attachment via hooks\n", instance->instanceId);
         ret = ERROR;
         goto release;
     }
     
     // protect libvirt calls, just in case
     sem_p (hyp_sem);
     int err = virDomainAttachDevice (dom, xml);
     sem_v (hyp_sem);
     if (err) {
         logprintfl (EUCAERROR, "[%s] failed to attach host device '%s' to guest device '%s'\n", instanceId, remoteDevReal, localDevReal);
         logprintfl (EUCAERROR, "AttachVolume(): virDomainAttachDevice() failed (err=%d) XML='%s'\n", err, xml);
         ret = ERROR;
     }
     
 release:
     
     sem_p(hyp_sem);
     virDomainFree (dom); // release libvirt resource
     sem_v(hyp_sem);

     // record volume state in memory and on disk
     char * next_vol_state;
     if (ret==OK) {
         next_vol_state = VOL_STATE_ATTACHED;
     } else {
         next_vol_state = VOL_STATE_ATTACHING_FAILED;
     }
     sem_p (inst_sem);
     volume = save_volume (instance, volumeId, NULL, NULL, NULL, next_vol_state); // now we can record remoteDevReal
     save_instance_struct (instance);
     sem_v (inst_sem);
     if (volume==NULL) {
         logprintfl (EUCAERROR, "AttachVolume(): failed to save the volume record, aborting volume attachment (detaching)\n");
         sem_p (hyp_sem);
         err = virDomainDetachDevice (dom, xml);
         sem_v (hyp_sem);
         if (err) {
             logprintfl (EUCAERROR, "AttachVolume(): virDomainDetachDevice() failed (err=%d) XML='%s'\n", err, xml);
         }
         ret = ERROR;
     }

     // if iSCSI and there were problems, try to disconnect the target
     if (ret != OK && is_iscsi_target && have_remote_device) {
         logprintfl(EUCADEBUG, "AttachVolume(): attempting to disconnect iscsi target due to attachment failure\n");
         if (disconnect_iscsi_target(remoteDev) != 0) {
             logprintfl (EUCAERROR, "AttachVolume(): disconnect_iscsi_target failed for %s\n", remoteDev);
         }
     }

     if (ret==OK)
         logprintfl (EUCAINFO, "[%s] attached '%s' as host device '%s' to guest device '%s'\n", instanceId, volumeId, remoteDevReal, localDevReal);
          
     return ret;
 }

static int
doDetachVolume (	struct nc_state_t *nc,
                    ncMetadata *meta,
                    char *instanceId,
                    char *volumeId,
                    char *remoteDev,
                    char *localDev,
                    int force,
                    int grab_inst_sem)
{
    int ret = OK;
    int is_iscsi_target = 0;
    int have_remote_device = 0;
    
    char * tagBuf;
    char * localDevName;
    char localDevReal[32], localDevTag[256], remoteDevReal[32];
    if (!strcmp (nc->H->name, "xen")) {
        tagBuf = NULL;
        localDevName = localDevReal;
    } else if (!strcmp (nc->H->name, "kvm")) {
        tagBuf = localDevTag;
        localDevName = localDevTag;
    } else {
        logprintfl (EUCAERROR, "DetachVolume(): unknown hypervisor type '%s'\n", nc->H->name);
        return ERROR;
    }
    
    // get the file name from the device path and, for KVM, the "unknown" string
    ret = convert_dev_names (localDev, localDevReal, tagBuf);
    if (ret)
        return ret;
    
    // find the instance record
    if (grab_inst_sem) sem_p (inst_sem); 
    ncInstance * instance = find_instance (&global_instances, instanceId);
    if (grab_inst_sem) sem_v (inst_sem);
    if (instance == NULL)
        return NOT_FOUND;
    
    // try attaching to hypervisor
    virConnectPtr *conn = check_hypervisor_conn();
    if (conn==NULL) {
        logprintfl(EUCAERROR, "DetachVolume(): cannot get connection to hypervisor\n");
        return ERROR;
    }
    
    // find domain on hypervisor
    sem_p (hyp_sem);
    virDomainPtr dom = virDomainLookupByName (*conn, instanceId);
    sem_v (hyp_sem);
    if (dom==NULL) {
        if (instance->state != BOOTING && instance->state != STAGING) {
            logprintfl (EUCAWARN, "DetachVolume(): domain %s not running on hypervisor, cannot attach device\n", instanceId);
        }
        return ERROR;
    }
    
    // mark volume as 'detaching'
    ncVolume * volume;
    if (grab_inst_sem) sem_p (inst_sem);
    volume = save_volume (instance, volumeId, remoteDev, localDevName, localDevReal, VOL_STATE_DETACHING);
    save_instance_struct (instance);
    if (grab_inst_sem) sem_v (inst_sem);
    if (!volume) {
        logprintfl (EUCAERROR, "DetachVolume(): failed to update the volume record, aborting volume attachment\n");
        return ERROR;
    }

    // do iscsi connect shellout if remoteDev is an iSCSI target
    if (check_iscsi(remoteDev)) {
        char *remoteDevStr=NULL;
        is_iscsi_target = 1;
        
        // get credentials, decrypt them
        remoteDevStr = get_iscsi_target (remoteDev);
        if (!remoteDevStr || !strstr(remoteDevStr, "/dev")) {
            logprintfl(EUCAERROR, "DetachVolume(): failed to get local name of host iscsi device\n");
            remoteDevReal[0] = '\0';
        } else { 
            snprintf(remoteDevReal, 32, "%s", remoteDevStr);
            have_remote_device = 1;
        }
        if (remoteDevStr) 
            free(remoteDevStr);
    } else {
        snprintf(remoteDevReal, 32, "%s", remoteDev);
        have_remote_device = 1;
    }
    
    // something went wrong above, abort
    if (!have_remote_device) {
        ret = ERROR;
        goto release;
    }
    
    // make sure there is a block device
    if (check_block (remoteDevReal)) {
        logprintfl(EUCAERROR, "DetachVolume(): cannot verify that host device '%s' is available for hypervisor detach\n", remoteDevReal);
        if (!force) 
            ret = ERROR;
        goto release;
    } 

    // generate XML for libvirt detachment request
    char xml [1024];
    if (gen_libvirt_attach_xml (volumeId, instance, localDevReal, remoteDevReal, xml, sizeof(xml))) {
        logprintfl(EUCAERROR, "DetachVolume(): could not produce detach device xml\n");
        ret = ERROR;
        goto release;
    }
    
    // protect libvirt calls, just in case
    sem_p (hyp_sem);
    int err = virDomainDetachDevice (dom, xml);
    if (!strcmp (nc->H->name, "xen")) {
        err = xen_detach_helper (nc, instanceId, localDevReal, xml);
    }
    sem_v (hyp_sem);
    
    if (err) {
        logprintfl (EUCAERROR, "[%s] failed to detach host device '%s' from guest device '%s'\n", instanceId, remoteDevReal, localDevReal);
        logprintfl (EUCAERROR, "DetachVolume(): virDomainDetachDevice() or 'virsh detach' failed (err=%d) XML='%s'\n", err, xml);
        if (!force) 
            ret = ERROR;
    } else {
        char path [MAX_PATH];
        snprintf (path, sizeof (path), EUCALYPTUS_VOLUME_XML_PATH_FORMAT, instance->instancePath, volumeId);
        call_hooks (NC_EVENT_POST_DETACH, path); // invoke hooks, but do not do anything if they return error
        unlink (path); // remove vol-XXXX.xml file
    }
    
 release:
    
    sem_p (hyp_sem);
    virDomainFree (dom); // release libvirt resource
    sem_v (hyp_sem);
    // record volume state in memory and on disk
    char * next_vol_state;
    if (ret==OK) {
        next_vol_state = VOL_STATE_DETACHED;
    } else {
        next_vol_state = VOL_STATE_DETACHING_FAILED;
    }
    if (grab_inst_sem) sem_p (inst_sem);
    volume = save_volume (instance, volumeId, NULL, NULL, NULL, next_vol_state);
    save_instance_struct (instance);
    if (grab_inst_sem) sem_v (inst_sem);
    if (volume==NULL) {
        logprintfl (EUCAWARN, "DetachVolume(): failed to save the volume record\n");
        ret=ERROR;
    }
    
    // if iSCSI, try to disconnect the target
    if (is_iscsi_target && have_remote_device) {
        logprintfl(EUCADEBUG, "DetachVolume(): attempting to disconnect iscsi target\n");
        if (disconnect_iscsi_target(remoteDev) != 0) {
            logprintfl (EUCAERROR, "DetachVolume(): disconnect_iscsi_target failed for %s\n", remoteDev);
            if (!force) 
                ret = ERROR;
        }
    }
    
    if (ret==OK)
        logprintfl (EUCAINFO, "[%s] detached '%s' as host device '%s' and guest device '%s'\n", instanceId, volumeId, remoteDevReal, localDevReal);
    
    if (force) {
        return(OK);
    }
    return ret;
}

// helper for changing bundling task state and stateName together                                                              
static void change_createImage_state (ncInstance * instance, createImage_progress state)
{
    instance->createImageTaskState = state;
    safe_strncpy (instance->createImageTaskStateName, createImage_progress_names [state], CHAR_BUFFER_SIZE);
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
            /***
                free_work_path (instance->instanceId, instance->userId, params->sizeMb);
            ***/
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
    
    /***
        params->sizeMb = get_bundling_size (instanceId, instance->userId) / MEGABYTE;
        if (params->sizeMb<1)
		return cleanup_createImage_task (instance, params, NO_STATE, CREATEIMAGE_FAILED);
        params->workPath = alloc_work_path (instanceId, instance->userId, params->sizeMb); // reserve work disk space for bundling
        if (params->workPath==NULL)
		return cleanup_createImage_task (instance, params, NO_STATE, CREATEIMAGE_FAILED);
        params->diskPath = get_disk_path (instanceId, instance->userId); // path of the disk to bundle
        if (params->diskPath==NULL)
		return cleanup_createImage_task (instance, params, NO_STATE, CREATEIMAGE_FAILED);
    ***/
    
	// terminate the instance
	sem_p (inst_sem);
	instance->createImageTime = time (NULL);
	change_state (instance, CREATEIMAGE_SHUTDOWN);
	change_createImage_state (instance, CREATEIMAGE_IN_PROGRESS);
	
	int err = find_and_terminate_instance (nc, meta, instanceId, 0, &instance, 1);
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

// helper for changing bundling task state and stateName together
static void change_bundling_state (ncInstance * instance, bundling_progress state)
{
	instance->bundleTaskState = state;
	safe_strncpy (instance->bundleTaskStateName, bundling_progress_names [state], CHAR_BUFFER_SIZE);
}

/*
static void set_bundling_env(struct bundling_params_t *params) {
  char buf[MAX_PATH];

  // set up environment for euca2ools
  snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-cert.pem", params->eucalyptusHomePath);
  setenv("EC2_CERT", buf, 1);
  
  snprintf(buf, MAX_PATH, "IGNORED");
  setenv("EC2_SECRET_KEY", buf, 1);
  
  snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/cloud-cert.pem", params->eucalyptusHomePath);
  setenv("EUCALYPTUS_CERT", buf, 1);
  
  snprintf(buf, MAX_PATH, "%s", params->walrusURL);
  setenv("S3_URL", buf, 1);
  
  snprintf(buf, MAX_PATH, "%s", params->userPublicKey);
  setenv("EC2_ACCESS_KEY", buf, 1);
  
  snprintf(buf, MAX_PATH, "123456789012");
  setenv("EC2_USER_ID", buf, 1);
  
  snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-cert.pem", params->eucalyptusHomePath);
  setenv("EUCA_CERT", buf, 1);
  
  snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-pk.pem", params->eucalyptusHomePath);
  setenv("EUCA_PRIVATE_KEY", buf, 1);
}

static void unset_bundling_env(void) {
  // unset up environment for euca2ools
  unsetenv("EC2_CERT");
  unsetenv("EC2_SECRET_KEY");
  unsetenv("EUCALYPTUS_CERT");
  unsetenv("S3_URL");
  unsetenv("EC2_ACCESS_KEY");
  unsetenv("EC2_USER_ID");
  unsetenv("EUCA_CERT");  
  unsetenv("EUCA_PRIVATE_KEY");
}
*/

// helper for cleaning up 
static int cleanup_bundling_task (ncInstance * instance, struct bundling_params_t * params, instance_states state, bundling_progress result)
{
        char cmd[MAX_PATH];
	char buf[MAX_PATH];
	int rc;
	logprintfl (EUCAINFO, "cleanup_bundling_task: instance %s bundling task result=%s\n", instance->instanceId, bundling_progress_names [result]);
	sem_p (inst_sem);
	change_bundling_state (instance, result);
	if (state!=NO_STATE) // do not touch instance state (these are early failures, before we destroyed the domain)
		change_state (instance, state);
	sem_v (inst_sem);

	if (params) {
	        // if the result was failed or cancelled, clean up walrus state
	        if (result == BUNDLING_FAILED || result == BUNDLING_CANCELLED) {
		  if (!instance->bundleBucketExists) {
		    snprintf(cmd, MAX_PATH, "%s -b %s -p %s --euca-auth", params->ncDeleteBundleCmd, params->bucketName, params->filePrefix);
		  } else {
		    snprintf(cmd, MAX_PATH, "%s -b %s -p %s --euca-auth --clear", params->ncDeleteBundleCmd, params->bucketName, params->filePrefix);
		  }
		  // set up environment for euca2ools
		  snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-cert.pem", params->eucalyptusHomePath);
		  setenv("EC2_CERT", buf, 1);
		  
		  snprintf(buf, MAX_PATH, "IGNORED");
		  setenv("EC2_SECRET_KEY", buf, 1);
		  
		  snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/cloud-cert.pem", params->eucalyptusHomePath);
		  setenv("EUCALYPTUS_CERT", buf, 1);
		  
		  snprintf(buf, MAX_PATH, "%s", params->walrusURL);
		  setenv("S3_URL", buf, 1);
		  
		  snprintf(buf, MAX_PATH, "%s", params->userPublicKey);
		  setenv("EC2_ACCESS_KEY", buf, 1);
		  
		  snprintf(buf, MAX_PATH, "123456789012");
		  setenv("EC2_USER_ID", buf, 1);
		  
		  snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-cert.pem", params->eucalyptusHomePath);
		  setenv("EUCA_CERT", buf, 1);
		  
		  snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-pk.pem", params->eucalyptusHomePath);
		  setenv("EUCA_PRIVATE_KEY", buf, 1);
		  logprintfl(EUCADEBUG, "cleanup_bundling_task: running cmd '%s'\n", cmd);
		  rc = system(cmd);
		  rc = rc>>8;
		  if (rc) {
		    logprintfl(EUCAWARN, "cleanup_bundling_task: bucket cleanup cmd '%s' failed with rc '%d'\n", cmd, rc);
		  }
		}
		if (params->workPath) {
                        /***
			free_work_path (instance->instanceId, instance->userId, params->sizeMb);
                        ***/
			free (params->workPath);
		}
		if (params->bucketName) free (params->bucketName);
		if (params->filePrefix) free (params->filePrefix);
		if (params->walrusURL) free (params->walrusURL);
		if (params->userPublicKey) free (params->userPublicKey);
		if (params->diskPath) free (params->diskPath);
		if (params->eucalyptusHomePath) free (params->eucalyptusHomePath);
		if (params->ncBundleUploadCmd) free (params->ncBundleUploadCmd);
		if (params->ncCheckBucketCmd) free (params->ncCheckBucketCmd);
		if (params->ncDeleteBundleCmd) free (params->ncDeleteBundleCmd);
		free (params);
	}

	return (result==BUNDLING_SUCCESS)?OK:ERROR;
}

static void * bundling_thread (void *arg) 
{
	struct bundling_params_t * params = (struct bundling_params_t *)arg;
	ncInstance * instance = params->instance;
	char cmd[MAX_PATH];
	char buf[MAX_PATH];

	logprintfl (EUCAINFO, "bundling_thread: waiting for instance %s to shut down\n", instance->instanceId);
	// wait until monitor thread changes the state of the instance instance 
	if (wait_state_transition (instance, BUNDLING_SHUTDOWN, BUNDLING_SHUTOFF)) { 
	  if (instance->bundleCanceled) { // cancel request came in while the instance was shutting down
	    logprintfl (EUCAINFO, "bundling_thread: cancelled while bundling instance %s\n", instance->instanceId);
	    cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_CANCELLED);
	  } else {
	    logprintfl (EUCAINFO, "bundling_thread: failed while bundling instance %s\n", instance->instanceId);
	    cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_FAILED);
	  }
	  return NULL;
	}

	logprintfl (EUCAINFO, "bundling_thread: started bundling instance %s\n", instance->instanceId);

	int rc=OK;
	char bundlePath[MAX_PATH];
    bundlePath[0] = '\0';
        if (clone_bundling_backing(instance, params->filePrefix, bundlePath) != OK){
		logprintfl(EUCAERROR, "bundling_thread: could not clone the instance image\n");
                cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_FAILED);
	} else {
		char prefixPath[MAX_PATH];
		snprintf(prefixPath, MAX_PATH, "%s/%s", instance->instancePath, params->filePrefix);
		if (strcmp(bundlePath, prefixPath)!=0 && rename(bundlePath, prefixPath)!=0){
	    	    logprintfl(EUCAERROR, "bundling_thread: could not rename from %s to %s\n", bundlePath, prefixPath);
                    cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_FAILED);	
	    	    return NULL;
		}
		// USAGE: euca-nc-bundle-upload -i <image_path> -d <working dir> -b <bucket>
	        int pid, status;
		
		// set up environment for euca2ools
		snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-cert.pem", params->eucalyptusHomePath);
		setenv("EC2_CERT", buf, 1);
		
		snprintf(buf, MAX_PATH, "IGNORED");
		setenv("EC2_SECRET_KEY", buf, 1);
		
		snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/cloud-cert.pem", params->eucalyptusHomePath);
		setenv("EUCALYPTUS_CERT", buf, 1);
		
		snprintf(buf, MAX_PATH, "%s", params->walrusURL);
		setenv("S3_URL", buf, 1);
		
		snprintf(buf, MAX_PATH, "%s", params->userPublicKey);
		setenv("EC2_ACCESS_KEY", buf, 1);
		
		snprintf(buf, MAX_PATH, "123456789012");
		setenv("EC2_USER_ID", buf, 1);
		
		snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-cert.pem", params->eucalyptusHomePath);
		setenv("EUCA_CERT", buf, 1);
		
		snprintf(buf, MAX_PATH, "%s/var/lib/eucalyptus/keys/node-pk.pem", params->eucalyptusHomePath);
		setenv("EUCA_PRIVATE_KEY", buf, 1);

		// check to see if the bucket exists in advance
		snprintf(cmd, MAX_PATH, "%s -b %s --euca-auth", params->ncCheckBucketCmd, params->bucketName);
		logprintfl(EUCADEBUG, "bundling_thread: running cmd '%s'\n", cmd);
		rc = system(cmd);
		rc = rc>>8;
		instance->bundleBucketExists = rc;

                if (instance->bundleCanceled){
                    logprintfl(EUCAINFO, "bundle task canceled; terminating bundling thread\n");
                    cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_CANCELLED);
		    return NULL;
                }
	
		pid = fork();
		if (!pid) {
		  logprintfl(EUCADEBUG, "bundling_thread: running cmd '%s -i %s -d %s -b %s -c %s --policysignature %s --euca-auth'\n", params->ncBundleUploadCmd, prefixPath, params->workPath, params->bucketName, params->S3Policy, params->S3PolicySig);
		  exit(execlp(params->ncBundleUploadCmd, params->ncBundleUploadCmd, "-i", prefixPath, "-d", params->workPath, "-b", params->bucketName, "-c", params->S3Policy, "--policysignature", params->S3PolicySig, "--euca-auth", NULL));
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
		        // bundler child was cancelled (killed), but should report it as failed 
		        cleanup_bundling_task (instance, params, SHUTOFF, BUNDLING_FAILED);
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
	char *userPublicKey,
	char *S3Policy, 
	char *S3PolicySig)
{
	// sanity checking
	if (instanceId==NULL
		|| bucketName==NULL
		|| filePrefix==NULL
		|| walrusURL==NULL
		|| userPublicKey==NULL
        || S3Policy == NULL
        || S3PolicySig == NULL) {
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
	params->S3Policy = strdup(S3Policy);
	params->S3PolicySig = strdup(S3PolicySig);
	params->eucalyptusHomePath = strdup (nc->home);
	params->ncBundleUploadCmd = strdup (nc->ncBundleUploadCmd);
	params->ncCheckBucketCmd = strdup (nc->ncCheckBucketCmd);
	params->ncDeleteBundleCmd = strdup (nc->ncDeleteBundleCmd);
 
	params->workPath = strdup(instance->instancePath);	
        /***
	params->sizeMb = get_bundling_size (instanceId, instance->userId) / MEGABYTE;
	if (params->sizeMb<1)
		return cleanup_bundling_task (instance, params, NO_STATE, BUNDLING_FAILED);
	params->workPath = alloc_work_path (instanceId, instance->userId, params->sizeMb); // reserve work disk space for bundling
	if (params->workPath==NULL)
		return cleanup_bundling_task (instance, params, NO_STATE, BUNDLING_FAILED);
	params->diskPath = get_disk_path (instanceId, instance->userId); // path of the disk to bundle
	if (params->diskPath==NULL)
		return cleanup_bundling_task (instance, params, NO_STATE, BUNDLING_FAILED);
        ***/

	// terminate the instance
	sem_p (inst_sem);
	instance->bundlingTime = time (NULL);
	change_state (instance, BUNDLING_SHUTDOWN);
	change_bundling_state (instance, BUNDLING_IN_PROGRESS);
	
	int err = find_and_terminate_instance (nc, meta, instanceId, 0, &instance, 1);
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
	if (pthread_create (&tid, &tattr, bundling_thread, (void *)params)!=0) {
		logprintfl (EUCAERROR, "doBundleInstance: failed to start VM bundling thread\n");
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
  instance->bundleCanceled = 1; // record the intent to cancel bundling so that bundling thread can abort
  if (instance->bundlePid > 0 && !check_process(instance->bundlePid, "euca-bundle-upload")) {
    logprintfl(EUCADEBUG, "doCancelBundleTask: found bundlePid '%d', sending kill signal...\n", instance->bundlePid);
    kill(instance->bundlePid, 9);
    instance->bundlePid = 0;
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
    .doAssignAddress     = doAssignAddress,
    .doPowerDown         = doPowerDown,
    .doAttachVolume      = doAttachVolume,
    .doDetachVolume      = doDetachVolume,
    .doCreateImage       = doCreateImage,
    .doBundleInstance    = doBundleInstance,
    .doCancelBundleTask  = doCancelBundleTask,
    .doDescribeBundleTasks    = doDescribeBundleTasks
};


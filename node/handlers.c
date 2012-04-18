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
#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU
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
#include <linux/limits.h>
#include <pwd.h> /* getpwuid_r */
#ifndef MAX_PATH
#define MAX_PATH 4096
#endif

#include "eucalyptus-config.h"
#include "ipc.h"
#include "misc.h"
#include "backing.h"
#include "diskutil.h"
#define HANDLERS_FANOUT
#include "handlers.h"
#include "eucalyptus.h"
#include "euca_auth.h"
#include "xml.h"
#include "vbr.h"
#include "iscsi.h"
#include "hooks.h"

#include "windows-bundle.h"
#define MONITORING_PERIOD (5)
#define MAX_CREATE_TRYS 5
#define CREATE_TIMEOUT_SEC 15
#define PER_INSTANCE_BUFFER_MB 20 // reserve this much extra room (in MB) per instance (for kernel, ramdisk, and metadata overhead)

#ifdef EUCA_COMPILE_TIMESTAMP
static char * compile_timestamp_str = EUCA_COMPILE_TIMESTAMP;
#else
static char * compile_timestamp_str = "";
#endif

/* used by lower level handlers */
sem *hyp_sem;	/* semaphore for serializing domain creation */
sem *inst_sem;	/* guarding access to global instance structs */
sem *addkey_sem;	/* guarding access to global instance structs */
sem *loop_sem; // created in diskutils.c for serializing 'losetup' invocations

bunchOfInstances *global_instances = NULL; 

// declarations of available handlers
extern struct handlers xen_libvirt_handlers;
extern struct handlers kvm_libvirt_handlers;
extern struct handlers default_libvirt_handlers;

const int staging_cleanup_threshold = 60 * 60 * 2; /* after this many seconds any STAGING domains will be cleaned up */
const int booting_cleanup_threshold = 60; /* after this many seconds any BOOTING domains will be cleaned up */
const int bundling_cleanup_threshold = 60 * 60; /* after this many seconds any BUNDLING domains will be cleaned up */
const int createImage_cleanup_threshold = 60 * 60; /* after this many seconds any CREATEIMAGE domains will be cleaned up */
const int teardown_state_duration = 180; /* after this many seconds in TEARDOWN state (no resources), we'll forget about the instance */

#define MIN_BLOBSTORE_SIZE_MB 10 // even with boot-from-EBS one will need work space for kernel and ramdisk
#define FS_BUFFER_PERCENT 0.03 // leave 3% extra when deciding on blobstore sizes automatically
#define WORK_BS_PERCENT 0.33 // give a third of available space to work, the rest to cache

// a NULL-terminated array of available handlers
static struct handlers * available_handlers [] = {
	&default_libvirt_handlers,
	&xen_libvirt_handlers,
	&kvm_libvirt_handlers,
	NULL
};

struct nc_state_t nc_state;

/* utilitarian functions used in the lower level handlers */
int
get_value(	char *s,
		const char *name,
		long long * valp)
{
	char buf [CHAR_BUFFER_SIZE];

	if (s==NULL || name==NULL || valp==NULL)
		return ERROR;
	snprintf (buf, CHAR_BUFFER_SIZE, "%s=%%lld", name);
	return (sscanf_lines (s, buf, valp)==1 ? OK : ERROR);
}

void libvirt_error_handler (	void *userData,
				virErrorPtr error)
{
	if ( error==NULL) {
		logprintfl (EUCAERROR, "libvirt error handler was given a NULL pointer\n");
	} else {
        int log_level = EUCAERROR;
        if (error->code==VIR_ERR_NO_DOMAIN) { // report "domain not found" errors as warnings, since they are expected when instance is being terminated
            log_level = EUCAWARN;
        }
		logprintfl (log_level, "libvirt: %s (code=%d)\n", error->message, error->code);
	}
}

// sets localDevReal (assummed to be at least 32 bytes long)
// to the filename portion of the device path (e.g., "sda" of "/dev/sda")
// also, if non-NULL, sets localDevTag to (e.g., "unknown,requested:/dev/sda")
int convert_dev_names(	const char *localDev,
			char *localDevReal,
			char *localDevTag) 
{
    bzero(localDevReal, 32);
    if (strchr(localDev, '/') != NULL) {
        sscanf(localDev, "/dev/%s", localDevReal);
    } else {
        snprintf(localDevReal, 32, "%s", localDev);
    }
    if (localDevReal[0] == 0) {
        logprintfl(EUCAERROR, "bad input parameter for localDev (should be /dev/XXX): '%s'\n", localDev);
        return(ERROR);
    }
    if (localDevTag) {
        bzero(localDevTag, 256);
        snprintf(localDevTag, 256, "unknown,requested:%s", localDev);
    }
    return 0;
}

void
print_running_domains (void)
{
	bunchOfInstances * head;
	char buf [CHAR_BUFFER_SIZE] = "";

	sem_p (inst_sem);
	for ( head=global_instances; head; head=head->next ) {
		ncInstance * instance = head->instance;
		if (instance->state==STAGING || instance->state==BOOTING
				|| instance->state==RUNNING
				|| instance->state==BLOCKED
				|| instance->state==PAUSED) {
			strcat (buf, " ");
			strcat (buf, instance->instanceId);
		}
	}
	sem_v (inst_sem);
	logprintfl (EUCAINFO, "currently running/booting: %s\n", buf);
}

virConnectPtr *
check_hypervisor_conn()
{

    sem_p (hyp_sem);
    char * uri = NULL;
	if (nc_state.conn == NULL || (uri = virConnectGetURI(nc_state.conn)) == NULL) {
		nc_state.conn = virConnectOpen (nc_state.uri);
	}
    if (uri!=NULL)
        free (uri);
    sem_v (hyp_sem);
    if (nc_state.conn == NULL) {
        logprintfl (EUCAFATAL, "Failed to connect to %s\n", nc_state.uri);
        return NULL;
    }
	return &(nc_state.conn);
}


void change_state(	ncInstance *instance,
			instance_states state)
{
    int old_state = instance->state;
    instance->state = (int) state;
    switch (state) { /* mapping from NC's internal states into external ones */
    case STAGING:
    case BOOTING:
    case CANCELED:
        instance->stateCode = PENDING;
        break;
    case RUNNING:
    case BLOCKED:
    case PAUSED:
        instance->stateCode = EXTANT;
        instance->retries = LIBVIRT_QUERY_RETRIES;
        break;
    case CRASHED:
    case BUNDLING_SHUTDOWN:
    case BUNDLING_SHUTOFF:
    case CREATEIMAGE_SHUTDOWN:
    case CREATEIMAGE_SHUTOFF:
    case SHUTDOWN:
    case SHUTOFF:
        if (instance->stateCode == EXTANT) {
            instance->stateCode = EXTANT;
        } else {
            instance->stateCode = PENDING;
        }
        instance->retries = LIBVIRT_QUERY_RETRIES;
        break;
    case TEARDOWN:
        instance->stateCode = TEARDOWN;
        break;
    default:
        logprintfl (EUCAERROR, "[%s] error: change_sate(): unexpected state (%d)\n", instance->instanceId, instance->state);        
        return;
    }

    safe_strncpy(instance->stateName, instance_state_names[instance->stateCode], CHAR_BUFFER_SIZE);
    if (old_state != state) {
        logprintfl (EUCADEBUG, "[%s] state change for instance: %s -> %s (%s)\n",
                    instance->instanceId, 
                    instance_state_names [old_state],
                    instance_state_names [instance->state],
                    instance_state_names [instance->stateCode]);
    }
}

// waits indefinitely until a state transition takes place
// (timeouts are implemented in the monitoring thread) and
// returns 0 if from_state->to_state transition takes place
// and 1 otherwise
int 
wait_state_transition (ncInstance * instance, 
		       instance_states from_state,
		       instance_states to_state) 
{
  while (1) {
    instance_states current_state = instance->state;
    if (current_state == to_state ) 
      return 0;
    if (current_state != from_state )
      return 1;
    sleep (MONITORING_PERIOD); // no point in checking more frequently
  }
}

static void
refresh_instance_info(	struct nc_state_t *nc,
                        ncInstance *instance)
{
    int old_state = instance->state;
    
    if (! check_hypervisor_conn ())
	    return;
    
    // no need to bug for domains without state on Hypervisor
    if (old_state==TEARDOWN || 
        old_state==STAGING || 
        old_state==BUNDLING_SHUTOFF || 
        old_state==CREATEIMAGE_SHUTOFF)
        return;
    
    sem_p(hyp_sem);
    virDomainPtr dom = virDomainLookupByName (nc_state.conn, instance->instanceId);
    sem_v(hyp_sem);
    if (dom == NULL) { // hypervisor doesn't know about it
        if (old_state==BUNDLING_SHUTDOWN) {
            logprintfl (EUCAINFO, "[%s] detected disappearance of bundled domain\n", instance->instanceId);
            change_state (instance, BUNDLING_SHUTOFF);
        } else if (old_state==CREATEIMAGE_SHUTDOWN) {
            logprintfl (EUCAINFO, "[%s] detected disappearance of createImage domain\n", instance->instanceId);
            change_state (instance, CREATEIMAGE_SHUTOFF);
        } else if (old_state==RUNNING ||
                   old_state==BLOCKED ||
                   old_state==PAUSED ||
                   old_state==SHUTDOWN) {
            // most likely the user has shut it down from the inside
            if (instance->retries) {
                instance->retries--;
                logprintfl (EUCAWARN, "[%s] warning: hypervisor failed to find domain, will retry %d more times\n", instance->instanceId, instance->retries);
            } else {
            	logprintfl (EUCAWARN, "[%s] warning: hypervisor failed to find domain, assuming it was shut off\n", instance->instanceId);
            	change_state (instance, SHUTOFF);
            }
        }
        // else 'old_state' stays in SHUTFOFF, BOOTING, CANCELED, or CRASHED
        return;
    }
    
    int rc;
    rc = get_instance_stats (dom, instance);
    if (rc) {
        logprintfl (EUCAWARN, "[%s] refresh_instances(): cannot get instance stats (block, network)\n", instance->instanceId);
    }
    
    virDomainInfo info;
    sem_p(hyp_sem);
    int error = virDomainGetInfo(dom, &info);
    sem_v(hyp_sem);
    if (error < 0 || info.state == VIR_DOMAIN_NOSTATE) {
        logprintfl (EUCAWARN, "[%s] warning: failed to get information for domain\n", instance->instanceId);
        // what to do? hopefully we'll find out more later
        sem_p(hyp_sem);
        virDomainFree (dom);
        sem_v(hyp_sem);
        return;
    } 
    int new_state = info.state;
    
    switch (old_state) {
    case BOOTING:
    case RUNNING:
    case BLOCKED:
    case PAUSED:
        if (new_state==SHUTOFF ||
            new_state==SHUTDOWN ||
            new_state==CRASHED) {
            logprintfl (EUCAWARN, "[%s] warning: hypervisor reported previously running domain as %s\n", instance->instanceId, instance_state_names [new_state]);
        }
        // change to state, whatever it happens to be
        change_state (instance, new_state);
        break;
    case SHUTDOWN:
    case SHUTOFF:
    case CRASHED:
        if (new_state==RUNNING ||
            new_state==BLOCKED ||
            new_state==PAUSED) {
            // cannot go back!
            logprintfl (EUCAWARN, "[%s] warning: detected prodigal domain, terminating it\n", instance->instanceId);
            sem_p (hyp_sem);
            virDomainDestroy (dom);
            sem_v (hyp_sem);
        } else {
            change_state (instance, new_state);
        }
        break;
    case BUNDLING_SHUTDOWN:
    case CREATEIMAGE_SHUTDOWN:
        logprintfl (EUCADEBUG, "[%s] hypervisor state for bundle/createImage domain is %s\n", instance->instanceId, instance_state_names [new_state]);
        break;
    default:
        logprintfl (EUCAERROR, "[%s] error: refresh...(): unexpected state (%d)\n", instance->instanceId, old_state);
        return;
    }
    sem_p(hyp_sem);
    virDomainFree(dom);
    sem_v(hyp_sem);
    
    // if instance is running, try to find out its IP address
    if (instance->state==RUNNING ||
        instance->state==BLOCKED ||
        instance->state==PAUSED) {
        char *ip=NULL;
        int rc;
        
        if (!strncmp(instance->ncnet.publicIp, "0.0.0.0", 24)) {
            if (!strcmp(nc_state.vnetconfig->mode, "SYSTEM") || !strcmp(nc_state.vnetconfig->mode, "STATIC")) {
                rc = mac2ip(nc_state.vnetconfig, instance->ncnet.privateMac, &ip);
                if (!rc && ip) {
                    logprintfl (EUCAINFO, "[%s] discovered public IP %s for instance\n", instance->instanceId, ip);
                    safe_strncpy(instance->ncnet.publicIp, ip, 24);
                    free(ip);
                }
            }
        }
        if (!strncmp(instance->ncnet.privateIp, "0.0.0.0", 24)) {
            rc = mac2ip(nc_state.vnetconfig, instance->ncnet.privateMac, &ip);
            if (!rc && ip) {
                logprintfl (EUCAINFO, "[%s] discovered private IP %s for instance\n", instance->instanceId, ip);
                safe_strncpy(instance->ncnet.privateIp, ip, 24);
                free(ip);
            }
        }
    }
}

void *
monitoring_thread (void *arg)
{
	int i;
	struct nc_state_t *nc;

	if (arg == NULL) {
		logprintfl (EUCAFATAL, "NULL parameter!\n");
		return NULL;
	}
	nc = (struct nc_state_t*)arg;

	logprintfl (EUCADEBUG, "starting monitoring thread\n");
    
    for (;;) {
        bunchOfInstances *head;
        time_t now = time(NULL);
        FILE *FP=NULL;
        char nfile[MAX_PATH], nfilefinal[MAX_PATH];
        
        sem_p (inst_sem);
        
        snprintf(nfile, MAX_PATH, "%s/var/log/eucalyptus/local-net.stage", nc_state.home);
        snprintf(nfilefinal, MAX_PATH, "%s/var/log/eucalyptus/local-net", nc_state.home);
        FP=fopen(nfile, "w");
        if (!FP) {
            logprintfl(EUCAWARN, "monitoring_thread(): could not open file %s for writing\n", nfile);
        }
        
        for ( head = global_instances; head; head = head->next ) {
            ncInstance * instance = head->instance;
            
            // query for current state, if any
            refresh_instance_info (nc, instance);
            
            // don't touch running or canceled threads
            if (instance->state!=STAGING && instance->state!=BOOTING && 
                instance->state!=SHUTOFF &&
                instance->state!=SHUTDOWN &&
                instance->state!=BUNDLING_SHUTDOWN &&
                instance->state!=BUNDLING_SHUTOFF &&
                instance->state!=CREATEIMAGE_SHUTDOWN &&
                instance->state!=CREATEIMAGE_SHUTOFF &&
                instance->state!=TEARDOWN) {
                
                if (FP && !strcmp(instance->stateName, "Extant")) { // TODO: is this still being used?
                    // have a running instance, write its information to local state file
                    fprintf(FP, "%s %s %s %d %s %s %s\n", 
                            instance->instanceId, 
                            nc_state.vnetconfig->pubInterface, 
                            "NA", 
                            instance->ncnet.vlan, 
                            instance->ncnet.privateMac, 
                            instance->ncnet.publicIp, 
                            instance->ncnet.privateIp);
                }
                continue;
            }
            
            if (instance->state==TEARDOWN) {
                // it's been long enough, we can forget the instance
                if ((now - instance->terminationTime)>teardown_state_duration) {
                    remove_instance (&global_instances, instance);
                    logprintfl (EUCAINFO, "[%s] forgetting about instance\n", instance->instanceId);
                    free_instance (&instance);
                    break;	// need to get out since the list changed
                }
                continue;
            }

            // time out logic for STAGING or BOOTING or BUNDLING instances
            if (instance->state==STAGING  
                && (now - instance->launchTime)   < staging_cleanup_threshold) continue; // hasn't been long enough, spare it
            if (instance->state==BOOTING  
                && (now - instance->bootTime)     < booting_cleanup_threshold) continue;
            if ((instance->state==BUNDLING_SHUTDOWN || instance->state==BUNDLING_SHUTOFF)
                && (now - instance->bundlingTime) < bundling_cleanup_threshold) continue;
            if ((instance->state==CREATEIMAGE_SHUTDOWN || instance->state==CREATEIMAGE_SHUTOFF)
                && (now - instance->createImageTime) < createImage_cleanup_threshold) continue;
            
            //DAN: need to destroy the domain here, just in case...
            if (instance->state == BOOTING) {
                ncInstance *tmpInstance=NULL;
                logprintfl(EUCADEBUG, "[%s] finding and terminating BOOTING instance (%d)\n", instance->instanceId, find_and_terminate_instance (nc, NULL, instance->instanceId, 1, &tmpInstance, 1));
            }

            // ok, it's been condemned => destroy the files
            int destroy_files = !nc_state.save_instance_files;
            if (call_hooks (NC_EVENT_PRE_CLEAN, instance->instancePath)) {
                if (destroy_files) {
                    logprintfl (EUCAERROR, "[%s] cancelled instance cleanup via hooks\n", instance->instanceId);
                    destroy_files = 0;
                }
            }
            logprintfl (EUCAINFO, "[%s] cleaning up state for instance%s\n", instance->instanceId, (destroy_files)?(""):(" (but keeping the files)"));
            if (destroy_instance_backing (instance, destroy_files)) {
                logprintfl (EUCAWARN, "[%s] warning: failed to cleanup instance state\n", instance->instanceId);
            }
            
            // check to see if this is the last instance running on vlan, handle local networking information drop
            int left = 0;
            bunchOfInstances * vnhead;
            for (vnhead = global_instances; vnhead; vnhead = vnhead->next ) {
                ncInstance * vninstance = vnhead->instance;
                if (vninstance->ncnet.vlan == (instance->ncnet).vlan 
                    && strcmp(instance->instanceId, vninstance->instanceId)) {
                    left++;
                }
            }
            if (left==0) {
                logprintfl (EUCAINFO, "[%s] stopping the network (vlan=%d)\n", instance->instanceId, (instance->ncnet).vlan);
                vnetStopNetwork (nc_state.vnetconfig, (instance->ncnet).vlan, NULL, NULL);
            }
            change_state (instance, TEARDOWN); // TEARDOWN = no more resources
            instance->terminationTime = time (NULL);
        }
        if (FP) {
            fclose(FP);
            rename (nfile, nfilefinal);
        }
        sem_v (inst_sem);
        
        if (head) {
            // we got out because of modified list, no need to sleep now
            continue;
        }
        
        sleep (MONITORING_PERIOD);
    }
    
    return NULL;
}

void *startup_thread (void * arg)
{
    ncInstance * instance = (ncInstance *)arg;
    char * disk_path, * xml=NULL;
    char *brname=NULL;
    int error, i;
    
    if (! check_hypervisor_conn ()) {
        logprintfl (EUCAERROR, "[%s] could not contact the hypervisor, abandoning the instance\n", instance->instanceId);
        goto shutoff;
    }
    
    // set up networking
    error = vnetStartNetwork (nc_state.vnetconfig, instance->ncnet.vlan, NULL, NULL, NULL, &brname);
    if (error) {
        logprintfl (EUCAERROR, "[%s] start network failed for instance, terminating it\n", instance->instanceId);
        goto shutoff;
    }

    safe_strncpy (instance->params.guestNicDeviceName, brname, sizeof (instance->params.guestNicDeviceName));

    if (nc_state.config_use_virtio_net) {
        instance->params.nicType = NIC_TYPE_VIRTIO;
    } else {
        if (strstr(instance->platform, "windows")) {
            instance->params.nicType = NIC_TYPE_WINDOWS;
        } else {
            instance->params.nicType = NIC_TYPE_LINUX;
        }
    }
    logprintfl (EUCAINFO, "[%s] started network\n", instance->instanceId);

    safe_strncpy (instance->hypervisorType, nc_state.H->name, sizeof (instance->hypervisorType)); // set the hypervisor type

    instance->hypervisorCapability = nc_state.capability; // set the cap (xen/hw/hw+xen)
    char *s = system_output ("getconf LONG_BIT");
    if (s) {
        int bitness = atoi(s);
        if (bitness == 32 || bitness == 64) {
            instance->hypervisorBitness = bitness;
        } else {
            logprintfl(EUCAWARN, "[%s] can't determine the host's bitness (%s, assuming 64)\n", instance->instanceId, s);
            instance->hypervisorBitness = 64;
        }
        free (s);
    } else {
        logprintfl(EUCAWARN, "[%s] can't determine the host's bitness (assuming 64)\n", instance->instanceId);
        instance->hypervisorBitness = 64;
    }
    instance->combinePartitions = nc_state.convert_to_disk; 
    instance->do_inject_key = nc_state.do_inject_key;

    if ((error = create_instance_backing (instance)) // do the heavy lifting on the disk
        || (error = gen_instance_xml (instance)) // create euca-specific instance XML file
        || (error = gen_libvirt_instance_xml (instance))) { // transform euca-specific XML into libvirt XML
        
        logprintfl (EUCAERROR, "[%s] error: failed to prepare images for instance (error=%d)\n", instance->instanceId, error);
        goto shutoff;
    }
    
    if (instance->state==TEARDOWN) { // timed out in STAGING
        goto free;
    }
    if (instance->state==CANCELED) {
        logprintfl (EUCAERROR, "[%s] cancelled instance startup\n", instance->instanceId);
        goto shutoff;
    }
    if (call_hooks (NC_EVENT_PRE_BOOT, instance->instancePath)) {
        logprintfl (EUCAERROR, "[%s] cancelled instance startup via hooks\n", instance->instanceId);
        goto shutoff;
    }
    xml = file2str (instance->libvirtFilePath);
    
    save_instance_struct (instance); // to enable NC recovery

    // serialize domain creation as hypervisors can get confused with
    // too many simultaneous create requests 
    logprintfl (EUCADEBUG2, "[%s] instance about to boot\n", instance->instanceId);
    
    boolean created = FALSE;
    for (i=0; i<MAX_CREATE_TRYS; i++) { // retry loop
        if (i>0) {
            logprintfl (EUCAINFO, "[%s] attempt %d of %d to create the instance\n", instance->instanceId, i+1, MAX_CREATE_TRYS);
        }
        sem_p (hyp_sem);
        sem_p (loop_sem);

        // We have seen virDomainCreateLinux() on occasion block indefinitely,
        // which freezes all activity on the NC since hyp_sem and loop_sem are
        // being held by the thread. (This is on Lucid with AppArmor enabled.)
        // To protect against that, we invoke the function in a process and 
        // terminate it after CREATE_TIMEOUT_SEC seconds.
        //
        // #0  0x00007f359f0b1f93 in poll () from /lib/libc.so.6
        // #1  0x00007f359a9a44e2 in ?? () from /usr/lib/libvirt.so.0
        // #2  0x00007f359a9a5060 in ?? () from /usr/lib/libvirt.so.0
        // #3  0x00007f359a9ac159 in ?? () from /usr/lib/libvirt.so.0
        // #4  0x00007f359a98d65b in virDomainCreateXML () from /usr/lib/libvirt.so.0
        // #5  0x00007f359b053c8e in startup_thread (arg=0x7f358813bf40) at handlers.c:644
        // #6  0x00007f359f3619ca in start_thread () from /lib/libpthread.so.0
        // #7  0x00007f359f0be70d in clone () from /lib/libc.so.6
        // #8  0x0000000000000000 in ?? ()

        pid_t cpid = fork(); 
        if (cpid<0) { // fork error
            logprintfl (EUCAERROR, "[%s] failed to fork to start instance\n", instance->instanceId);
            
        } else if (cpid==0) { // child process - creates the domain
            virDomainPtr dom = virDomainCreateLinux (nc_state.conn, xml, 0);
            if (dom!=NULL) {
                virDomainFree (dom); // To be safe. Docs are not clear on whether the handle exists outside the process.
                exit (0);
            } else {
                exit (1);
            }
            
        } else { // parent process - waits for the child, kills it if necessary
            int status;
            int rc = timewait (cpid, &status, CREATE_TIMEOUT_SEC);
            boolean try_killing = FALSE;
            if (rc < 0) {
                logprintfl (EUCAERROR, "[%s] failed to wait for forked process: %s\n", instance->instanceId, strerror(errno));
                try_killing = TRUE;

            } else if (rc ==0) {
                logprintfl (EUCAERROR, "[%s] timed out waiting for forked process pid=%d\n", instance->instanceId, cpid);
                try_killing = TRUE;

            } else if (WEXITSTATUS(status) != 0) {
                logprintfl (EUCAERROR, "[%s] hypervisor failed to create the instance\n", instance->instanceId);

            } else {
                created = TRUE;
            }

            if (try_killing) {
                kill (cpid, SIGKILL); // should be able to do
                kill (cpid, 9); // may not be able to do?
            }
        }
        
        sem_v (loop_sem);
        sem_v (hyp_sem);
        if (created)
            break;
        sleep (1);
    }
    if (!created) {
        goto shutoff;
    }

    eventlog("NC", instance->userId, "", "instanceBoot", "begin"); // TODO: bring back correlationId

    sem_p (inst_sem);
    // check one more time for cancellation
    if (instance->state==TEARDOWN) { 
        // timed out in BOOTING
    } else if (instance->state==CANCELED || instance->state==SHUTOFF) {
        logprintfl (EUCAERROR, "[%s] startup of instance was cancelled\n", instance->instanceId);
        change_state (instance, SHUTOFF);
    } else {
        logprintfl (EUCAINFO, "[%s] booting\n", instance->instanceId);
        instance->bootTime = time (NULL);
        change_state (instance, BOOTING);
    }
    sem_v (inst_sem);
    goto free;

 shutoff: // escape point for error conditions
    change_state (instance, SHUTOFF);
    
 free:
    if (xml) free (xml);
    if (brname) free (brname);
    return NULL;
}

void adopt_instances()
{
	int dom_ids[MAXDOMS];
	int num_doms = 0;
	int i;
       	virDomainPtr dom = NULL;

	if (! check_hypervisor_conn())
		return;
        
	logprintfl (EUCAINFO, "looking for existing domains\n");
	virSetErrorFunc (NULL, libvirt_error_handler);
        
	num_doms = virConnectListDomains(nc_state.conn, dom_ids, MAXDOMS);
	if (num_doms == 0) {
		logprintfl (EUCAINFO, "no currently running domains to adopt\n");
		return;
	} if (num_doms < 0) {
		logprintfl (EUCAWARN, "WARNING: failed to find out about running domains\n");
		return;
	}

	for ( i=0; i<num_doms; i++) {
		int error;
		virDomainInfo info;
		const char * dom_name;
		ncInstance * instance;

		sem_p(hyp_sem);
		dom = virDomainLookupByID(nc_state.conn, dom_ids[i]);
		sem_v(hyp_sem);
		if (!dom) {
			logprintfl (EUCAWARN, "WARNING: failed to lookup running domain #%d, ignoring it\n", dom_ids[i]);
			continue;
		}

		sem_p(hyp_sem);
		error = virDomainGetInfo(dom, &info);
		sem_v(hyp_sem);
		if (error < 0 || info.state == VIR_DOMAIN_NOSTATE) {
			logprintfl (EUCAWARN, "WARNING: failed to get info on running domain #%d, ignoring it\n", dom_ids[i]);
			continue;
		}

		if (info.state == VIR_DOMAIN_SHUTDOWN ||
				info.state == VIR_DOMAIN_SHUTOFF ||
				info.state == VIR_DOMAIN_CRASHED ) {
			logprintfl (EUCADEBUG, "ignoring non-running domain #%d\n", dom_ids[i]);
			continue;
		}

		sem_p(hyp_sem);
		if ((dom_name = virDomainGetName(dom))==NULL) {
		        sem_v(hyp_sem);
		        logprintfl (EUCAWARN, "WARNING: failed to get name of running domain #%d, ignoring it\n", dom_ids[i]);
			continue;
		}
		sem_v(hyp_sem);

		if (!strcmp(dom_name, "Domain-0"))
			continue;

		if ((instance = load_instance_struct (dom_name))==NULL) {
			logprintfl (EUCAWARN, "WARNING: failed to recover Eucalyptus metadata of running domain %s, ignoring it\n", dom_name);
			continue;
		}

        if (call_hooks (NC_EVENT_ADOPTING, instance->instancePath)) {
            logprintfl (EUCAINFO, "ignoring running domain %s due to hooks\n", instance->instanceId);
            free_instance (&instance);
            continue;
        }

		change_state (instance, info.state);                    
		sem_p (inst_sem);
		int err = add_instance (&global_instances, instance);
		sem_v (inst_sem);
		if (err) {
			free_instance (&instance);
			continue;
		}

		logprintfl (EUCAINFO, "- adopted running domain %s from user %s\n", instance->instanceId, instance->userId); // TODO: try to re-check IPs?

		sem_p(hyp_sem);
		virDomainFree (dom);
		sem_v(hyp_sem);
	}
}

static int init (void)
{
	static int initialized = 0;
	int do_warn = 0, i, j;
	char configFiles[2][MAX_PATH],
		log[MAX_PATH],
		*bridge=NULL,
		*hypervisor=NULL,
		*s=NULL,
        *tmp=NULL,
        *pubinterface=NULL;
	struct stat mystat;
	struct handlers ** h; 

	if (initialized>0) // 0 => hasn't run, -1 => failed, 1 => ok
		return 0;
	else if (initialized<0)
		return 1;

	bzero (&nc_state, sizeof(struct nc_state_t)); // ensure that MAXes are zeroed out

	// read in configuration - this should be first!

    // determine home ($EUCALYPTUS)
	tmp = getenv(EUCALYPTUS_ENV_VAR_NAME);
	if (!tmp) {
		nc_state.home[0] = '\0'; // empty string means '/'
		do_warn = 1;
	} else {
		strncpy(nc_state.home, tmp, MAX_PATH - 1);
    }

	// set the minimum log for now
	snprintf(log, MAX_PATH, "%s/var/log/eucalyptus/nc.log", nc_state.home);
	logfile(log, EUCAINFO, 4);
        logprintfl (EUCAINFO, "Eucalyptus node controller initializing %s\n", compile_timestamp_str);

	if (do_warn) 
		logprintfl (EUCAWARN, "env variable %s not set, using /\n", EUCALYPTUS_ENV_VAR_NAME);

	// search for the config file
	snprintf(configFiles[1], MAX_PATH, EUCALYPTUS_CONF_LOCATION, nc_state.home);
	if (stat(configFiles[1], &mystat)) {
		logprintfl (EUCAFATAL, "could not open configuration file %s\n", configFiles[1]);
		return 1;
	}
	snprintf(configFiles[0], MAX_PATH, EUCALYPTUS_CONF_OVERRIDE_LOCATION, nc_state.home);
	logprintfl (EUCAINFO, "NC is looking for configuration in %s,%s\n", configFiles[1], configFiles[0]);

	// reset the log level to the requested value
	tmp = getConfString(configFiles, 2, "LOGLEVEL");
	i = EUCADEBUG;
	if (tmp) {
		if (!strcmp(tmp,"INFO")) {i=EUCAINFO;}
		else if (!strcmp(tmp,"WARN")) {i=EUCAWARN;}
		else if (!strcmp(tmp,"ERROR")) {i=EUCAERROR;}
		else if (!strcmp(tmp,"FATAL")) {i=EUCAFATAL;}
        else if (!strcmp(tmp,"DEBUG2")) {i=EUCADEBUG2;}
		free(tmp);
	}
	tmp = getConfString(configFiles, 2, "LOGROLLNUMBER");
	j = 4;
	if (tmp) {
        j = atoi(tmp);
		free(tmp);
	}
	logfile(log, i, j);

    { 
        /* Initialize libvirtd.conf, since some buggy versions of libvirt
         * require it.  At least two versions of libvirt have had this issue,
         * most recently the version in RHEL 6.1.  Note that this happens
         * at each startup of the NC mainly because the location of the
         * required file depends on the process owner's home directory, which
         * may change after the initial installation.
         */
        char libVirtConf [MAX_PATH];
        uid_t uid = geteuid();
        struct passwd *pw;
        FILE * fd;
        struct stat lvcstat;
        pw = getpwuid(uid);
        errno = 0;
        if (pw != NULL) {
            snprintf(libVirtConf, MAX_PATH, "%s/.libvirt/libvirtd.conf", pw->pw_dir);
            if (access(libVirtConf, R_OK) == -1 && errno == ENOENT) {
                libVirtConf[strlen(libVirtConf)-strlen("/libvirtd.conf")] = '\0';
                errno = 0;
                if (stat(libVirtConf, &lvcstat) == -1 && errno == ENOENT) {
                    mkdir(libVirtConf, 0755);
                } else if (errno) {
                     logprintfl (EUCAINFO, "Failed to stat %s/.libvirt\n", pw->pw_dir);
                }
                libVirtConf[strlen(libVirtConf)] = '/';
                errno = 0;
                fd = fopen(libVirtConf, "a");
                if (fd == NULL) {
                    logprintfl (EUCAINFO, "Failed to open %s, error code %d\n", libVirtConf, errno);
                } else {
                    fclose(fd);
                }
            } else if (errno) {
                logprintfl (EUCAINFO, "Failed to access libvirtd.conf, error code %d\n", errno);
            }
        } else {
             logprintfl (EUCAINFO, "Cannot get EUID, not creating libvirtd.conf\n");
        }
    }

    { // initialize hooks if their directory looks ok
        char dir [MAX_PATH];
        snprintf (dir, sizeof (dir), EUCALYPTUS_NC_HOOKS_DIR, nc_state.home);
        // if 'dir' does not exist, init_hooks() will silently fail, 
        // and all future call_hooks() will silently succeed
        init_hooks (nc_state.home, dir); 

        if (call_hooks (NC_EVENT_PRE_INIT, nc_state.home)) {
            logprintfl (EUCAFATAL, "hooks prevented initialization\n");
            return ERROR_FATAL;
        }
    }

#define GET_VAR_INT(var,name,def)                   \
        s = getConfString(configFiles, 2, name); \
	if (s){					\
		var = atoi(s);\
		free (s);\
	} else { \
        var = def; \
    }
	GET_VAR_INT(nc_state.config_max_mem,      CONFIG_MAX_MEM, 0);
	GET_VAR_INT(nc_state.config_max_cores,    CONFIG_MAX_CORES, 0);
	GET_VAR_INT(nc_state.save_instance_files, CONFIG_SAVE_INSTANCES, 0);
    GET_VAR_INT(nc_state.concurrent_disk_ops, CONFIG_CONCURRENT_DISK_OPS, 4);
    int disable_injection; GET_VAR_INT(disable_injection, CONFIG_DISABLE_KEY_INJECTION, 0); 
    nc_state.do_inject_key = !disable_injection;
    strcpy(nc_state.admin_user_id, EUCALYPTUS_ADMIN);
               
    // add three eucalyptus directories with executables to PATH of this process
    add_euca_to_path (nc_state.home); 

    // read in .pem files
	if (euca_init_cert ()) {
	  logprintfl (EUCAERROR, "init(): failed to find cryptographic certificates\n");
	  return ERROR_FATAL;
	}

	//// from now on we have unrecoverable failure, so no point in retrying to re-init ////
	initialized = -1;

	hyp_sem = sem_alloc (1, "mutex");
	inst_sem = sem_alloc (1, "mutex");
	addkey_sem = sem_alloc (1, "mutex");
	if (!hyp_sem || !inst_sem || !addkey_sem) {
		logprintfl (EUCAFATAL, "failed to create and initialize semaphores\n");
		return ERROR_FATAL;
	}

    if (diskutil_init(FALSE) || (loop_sem = diskutil_get_loop_sem())==NULL) { // NC does not need GRUB for now
        logprintfl (EUCAFATAL, "failed to find all dependencies\n");
		return ERROR_FATAL;
    }

    init_iscsi (nc_state.home);

	// set default in the paths. the driver will override
	nc_state.config_network_path[0] = '\0';
	nc_state.xm_cmd_path[0] = '\0';
	nc_state.virsh_cmd_path[0] = '\0';
	nc_state.get_info_cmd_path[0] = '\0';
	snprintf (nc_state.libvirt_xslt_path, MAX_PATH, EUCALYPTUS_LIBVIRT_XSLT, nc_state.home);
	snprintf (nc_state.rootwrap_cmd_path, MAX_PATH, EUCALYPTUS_ROOTWRAP, nc_state.home);

	// determine the hypervisor to use
	hypervisor = getConfString(configFiles, 2, CONFIG_HYPERVISOR);
	if (!hypervisor) {
		logprintfl (EUCAFATAL, "value %s is not set in the config file\n", CONFIG_HYPERVISOR);
		return ERROR_FATAL;
	}

	// let's look for the right hypervisor driver
	for (h = available_handlers; *h; h++ ) {
		if (!strncmp ((*h)->name, "default", CHAR_BUFFER_SIZE))
			nc_state.D = *h; 
		if (!strncmp ((*h)->name, hypervisor, CHAR_BUFFER_SIZE))
			nc_state.H = *h; 
	}
	if (nc_state.H == NULL) {
		logprintfl (EUCAFATAL, "requested hypervisor type (%s) is not available\n", hypervisor);
		free (hypervisor);
		return ERROR_FATAL;
	}
	
	// only load virtio config for kvm
	if (!strncmp("kvm", hypervisor, CHAR_BUFFER_SIZE) ||
		!strncmp("KVM", hypervisor, CHAR_BUFFER_SIZE)) {
		GET_VAR_INT(nc_state.config_use_virtio_net, CONFIG_USE_VIRTIO_NET, 0);
		GET_VAR_INT(nc_state.config_use_virtio_disk, CONFIG_USE_VIRTIO_DISK, 0);
		GET_VAR_INT(nc_state.config_use_virtio_root, CONFIG_USE_VIRTIO_ROOT, 0);
	}
	free (hypervisor);

	// NOTE: this is the only call which needs to be called on both
	// the default and the specific handler! All the others will be
	// either or
	i = nc_state.D->doInitialize(&nc_state);
	if (nc_state.H->doInitialize)
		i += nc_state.H->doInitialize(&nc_state);
	if (i) {
		logprintfl(EUCAFATAL, "ERROR: failed to initialized hypervisor driver!\n");
		return ERROR_FATAL;
	}

	// now that hypervisor-specific initializers have discovered mem_max and cores_max,
    // adjust the values based on configuration parameters, if any
	if (nc_state.config_max_mem && nc_state.config_max_mem < nc_state.mem_max)
		nc_state.mem_max = nc_state.config_max_mem;
	if (nc_state.config_max_cores)
		nc_state.cores_max = nc_state.config_max_cores;
	logprintfl(EUCAINFO, "physical memory available for instances: %lldMB\n", nc_state.mem_max);
	logprintfl(EUCAINFO, "virtual cpu cores available for instances: %lld\n", nc_state.cores_max);
    
    { // backing store configuration
        char * instance_path = getConfString(configFiles, 2, INSTANCE_PATH);

        // determine bytes available on the file system to which instance path belongs
        if (instance_path == NULL) {
            logprintfl (EUCAFATAL, "error: %s is not set\n", INSTANCE_PATH);
            return ERROR_FATAL;
        }
        struct statfs fs;
        if (statfs (instance_path, &fs) == -1) { 
            logprintfl (EUCAERROR, "error: failed to stat %s (%s): %s\n", INSTANCE_PATH, instance_path, strerror(errno));
            free (instance_path);
            return ERROR_FATAL;
        }
        long long fs_avail_mb = (long long)fs.f_bsize * (long long)(fs.f_bavail/MEGABYTE); // TODO: is 2 petabytes enough?
        long long fs_size_mb =  (long long)fs.f_bsize * (long long)(fs.f_blocks/MEGABYTE); 
        if (fs_avail_mb < MIN_BLOBSTORE_SIZE_MB) {
            logprintfl (EUCAERROR, "error: insufficient available disk space (%d) under %s\n", fs_avail_mb, instance_path);
            free (instance_path);
            return ERROR_FATAL;
        }

        // determine how much is used/available in work and cache areas on the backing store
        blobstore_meta work_meta, cache_meta;
        stat_backing_store (instance_path, &work_meta, &cache_meta); // will zero-out work_ and cache_meta
        long long work_bs_size_mb       = work_meta.blocks_limit  ? (work_meta.blocks_limit / 2048) : (-1L); // convert sectors->MB
        long long work_bs_allocated_mb  = work_meta.blocks_limit  ? (work_meta.blocks_allocated / 2048) : 0;
        long long work_bs_reserved_mb   = work_meta.blocks_limit  ? ((work_meta.blocks_locked + work_meta.blocks_unlocked) / 2048) : 0;
        long long cache_bs_size_mb      = cache_meta.blocks_limit ? (cache_meta.blocks_limit / 2048) : (-1L);
        long long cache_bs_allocated_mb = cache_meta.blocks_limit ? (cache_meta.blocks_allocated / 2048) : 0;
        long long cache_bs_reserved_mb  = cache_meta.blocks_limit ? ((cache_meta.blocks_locked + cache_meta.blocks_unlocked) / 2048) : 0;

        // look up configuration file settings for work and cache size
        long long conf_work_size_mb; GET_VAR_INT(conf_work_size_mb,  CONFIG_NC_WORK_SIZE, -1);
        long long conf_cache_size_mb; GET_VAR_INT(conf_cache_size_mb, CONFIG_NC_CACHE_SIZE, -1);
        { // accommodate legacy MAX_DISK setting by converting it
            int max_disk_gb; GET_VAR_INT(max_disk_gb, CONFIG_MAX_DISK, -1);
            if (max_disk_gb != -1) {
                if (conf_work_size_mb == -1) {
                    logprintfl (EUCAWARN, "warning: using deprecated setting %s for the new setting %s\n", CONFIG_MAX_DISK, CONFIG_NC_WORK_SIZE);
                    if (max_disk_gb == 0) {
                        conf_work_size_mb = -1; // change in semantics: 0 used to mean 'unlimited', now 'unset' or -1 means that
                    } else {
                        conf_work_size_mb = max_disk_gb * 1024;
                    }
                } else {
                    logprintfl (EUCAWARN, "warning: ignoring deprecated setting %s in favor of the new setting %s\n", CONFIG_MAX_DISK, CONFIG_NC_WORK_SIZE);
                }
            }
        }

        // decide what work and cache sizes should be, based on all the inputs
        long long work_size_mb = -1;
        long long cache_size_mb = -1;

        // above all, try to respect user-specified limits for work and cache
        if (conf_work_size_mb != -1) {
            if (conf_work_size_mb < MIN_BLOBSTORE_SIZE_MB) {
                logprintfl (EUCAWARN, "warning: ignoring specified work size (%s=%d) that is below acceptable minimum (%d)\n", 
                            CONFIG_NC_WORK_SIZE, conf_work_size_mb, MIN_BLOBSTORE_SIZE_MB);
            } else {
                if (work_bs_size_mb != -1 && work_bs_size_mb != conf_work_size_mb) {
                    logprintfl (EUCAWARN, "warning: specified work size (%s=%d) differs from existing work size (%d), will try resizing\n",
                                CONFIG_NC_WORK_SIZE, conf_work_size_mb, work_bs_size_mb);
                }
                work_size_mb = conf_work_size_mb;
            }
        }
        if (conf_cache_size_mb != -1) { // respect user-specified limit
            if (conf_cache_size_mb < MIN_BLOBSTORE_SIZE_MB) {
                cache_size_mb = 0; // so it won't be used
            } else {
                if (cache_bs_size_mb != -1 && cache_bs_size_mb != conf_cache_size_mb) {
                    logprintfl (EUCAWARN, "warning: specified cache size (%s=%d) differs from existing cache size (%d), will try resizing\n",
                                CONFIG_NC_CACHE_SIZE, conf_cache_size_mb, cache_bs_size_mb);
                }
                cache_size_mb = conf_cache_size_mb;
            }
        }

        // if the user did not specify sizes, try existing blobstores, 
        // if any, whose limits would have been chosen earlier
        if (work_size_mb == -1 && work_bs_size_mb != -1)
            work_size_mb = work_bs_size_mb; 
        if (cache_size_mb == -1 && cache_bs_size_mb != -1)
            cache_size_mb = cache_bs_size_mb; 
        
        // if the user did not specify either or both of the sizes,
        // and blobstores do not exist yet, make reasonable choices
        long long fs_usable_mb = (long long)((double)fs_avail_mb - (double)(fs_avail_mb) * FS_BUFFER_PERCENT);
        if (work_size_mb == -1 && cache_size_mb == -1) {
            work_size_mb = (long long)((double)fs_usable_mb * WORK_BS_PERCENT);
            cache_size_mb = fs_usable_mb - work_size_mb;
        } else if (work_size_mb == -1) {
            work_size_mb = fs_usable_mb - cache_size_mb + cache_bs_allocated_mb;
        } else if (cache_size_mb == -1) {
            cache_size_mb = fs_usable_mb - work_size_mb + work_bs_allocated_mb;
        }

        // sanity-check final results
        if (cache_size_mb < MIN_BLOBSTORE_SIZE_MB)
            cache_size_mb = 0;
        if (work_size_mb < MIN_BLOBSTORE_SIZE_MB) {
            logprintfl (EUCAERROR, "error: insufficient disk space for virtual machines (free space: %dMB, reserved for cache: %dMB)\n", 
                        work_size_mb, fs_usable_mb, cache_size_mb);
            free (instance_path);
            return ERROR_FATAL;
        }
        if ((cache_size_mb + work_size_mb - cache_bs_allocated_mb - work_bs_allocated_mb) > fs_avail_mb) {
            logprintfl (EUCAWARN, "warning: sum of work and cache sizes exceeds available disk space\n");
        }

        if (init_backing_store (instance_path, work_size_mb, cache_size_mb)) {
            logprintfl (EUCAFATAL, "error: failed to initialize backing store\n");
            free (instance_path);
            return ERROR_FATAL;
        }
       
        // record the work-space limit for max_disk 
        long long work_size_gb = (long long)(work_size_mb / MB_PER_DISK_UNIT);
        long long overhead_mb = work_size_gb * PER_INSTANCE_BUFFER_MB; // work_size_gb is also max number of instances
        long long disk_max_mb = work_size_mb - overhead_mb;
        nc_state.disk_max = disk_max_mb / MB_PER_DISK_UNIT;

        logprintfl (EUCAINFO, "disk space for instances: %s/work\n", instance_path);
        logprintfl (EUCAINFO, "                          %06lldMB limit (%.1f%% of the file system) - %lldMB overhead = %lldMB = %lldGB\n",
                    work_size_mb, 
                    ((double)work_size_mb/(double)fs_size_mb)*100.0,
                    overhead_mb,
                    disk_max_mb,
                    nc_state.disk_max);
        logprintfl (EUCAINFO, "                          %06lldMB reserved for use (%.1f%% of limit)\n", 
                    work_bs_reserved_mb, 
                    ((double)work_bs_reserved_mb/(double)work_size_mb)*100.0 );
        logprintfl (EUCAINFO, "                          %06lldMB allocated for use (%.1f%% of limit, %.1f%% of the file system)\n", 
                    work_bs_allocated_mb, 
                    ((double)work_bs_allocated_mb/(double)work_size_mb)*100.0,
                    ((double)work_bs_allocated_mb/(double)fs_size_mb)*100.0 );
        if (cache_size_mb) {
            logprintfl (EUCAINFO, "    disk space for cache: %s/cache\n", instance_path);
            logprintfl (EUCAINFO, "                          %06lldMB limit (%.1f%% of the file system)\n", 
                        cache_size_mb, 
                        ((double)cache_size_mb/(double)fs_size_mb)*100.0 );
            logprintfl (EUCAINFO, "                          %06lldMB reserved for use (%.1f%% of limit)\n", 
                        cache_bs_reserved_mb,
                        ((double)cache_bs_reserved_mb/(double)cache_size_mb)*100.0 );
            logprintfl (EUCAINFO, "                          %06lldMB allocated for use (%.1f%% of limit, %.1f%% of the file system)\n", 
                        cache_bs_allocated_mb, 
                        ((double)cache_bs_allocated_mb/(double)cache_size_mb)*100.0,
                        ((double)cache_bs_allocated_mb/(double)fs_size_mb)*100.0 );
        } else {
            logprintfl (EUCAWARN, "warning: disk cache will not be used\n");
        }
        free (instance_path);
    }

	// adopt running instances -- do this before disk integrity check so we know what can be purged
	adopt_instances();

    if (check_backing_store(&global_instances)!=OK) { // integrity check, cleanup of unused instances and shrinking of cache
        logprintfl (EUCAFATAL, "error: integrity check of the backing store failed");
        return ERROR_FATAL;
    }

	// setup the network
	nc_state.vnetconfig = malloc(sizeof(vnetConfig));
	if (!nc_state.vnetconfig) {
		logprintfl (EUCAFATAL, "Cannot allocate vnetconfig!\n");
		return ERROR_FATAL;
	}
	snprintf (nc_state.config_network_path, MAX_PATH, NC_NET_PATH_DEFAULT, nc_state.home);

	tmp = getConfString(configFiles, 2, "VNET_MODE");
    if (!tmp) {
        logprintfl(EUCAWARN,"WARNING: VNET_MODE is not defined, defaulting to 'SYSTEM'\n"); 
        tmp = strdup("SYSTEM"); 
        if (!tmp) {
            logprintfl(EUCAFATAL,"Out of memory\n"); 
            return ERROR_FATAL;
        }
    }     

    int initFail = 0;
    if(tmp && (!strcmp(tmp, "SYSTEM") || !strcmp(tmp, "STATIC") || !strcmp(tmp, "MANAGED-NOVLAN"))) {
        bridge = getConfString(configFiles, 2, "VNET_BRIDGE");
        if(!bridge) {
            logprintfl(EUCAFATAL,"in 'SYSTEM', 'STATIC' or 'MANAGED-NOVLAN' network mode, you must specify a value for VNET_BRIDGE\n");
            initFail = 1;
        }
    } else if(tmp && !strcmp(tmp, "MANAGED")) {
        pubinterface = getConfString(configFiles, 2, "VNET_PUBINTERFACE");
        if (!pubinterface) 
            pubinterface = getConfString(configFiles, 2, "VNET_INTERFACE");

        if (!pubinterface) {
            logprintfl(EUCAWARN,"WARNING: VNET_PUBINTERFACE is not defined, defaulting to 'eth0'\n"); 
            pubinterface = strdup("eth0"); 
            if (!pubinterface) {
                logprintfl(EUCAFATAL, "out of memory!\n"); 
                initFail = 1;
            }
        } 
    }
	
    if(!initFail) {
        initFail = vnetInit (nc_state.vnetconfig, 
                             tmp, 
                             nc_state.home, 
                             nc_state.config_network_path, 
                             NC, 
                             pubinterface, 
                             pubinterface, 
                             NULL, 
                             NULL, 
                             NULL, 
                             NULL, 
                             NULL, 
                             NULL, 
                             NULL, 
                             NULL, 
                             NULL, 
                             bridge, 
                             NULL, 
                             NULL);
    }
	if (pubinterface) free(pubinterface);
	if (bridge) free(bridge);
	if (tmp) free(tmp);

    if (initFail)
        return ERROR_FATAL;
    
	// set NC helper path
	tmp = getConfString(configFiles, 2, CONFIG_NC_BUNDLE_UPLOAD);
	if (tmp) {
	  snprintf (nc_state.ncBundleUploadCmd, MAX_PATH, "%s", tmp);
	  free(tmp);
	} else {
	  snprintf (nc_state.ncBundleUploadCmd, MAX_PATH, "%s", EUCALYPTUS_NC_BUNDLE_UPLOAD); // default value
	}

	// set NC helper path
	tmp = getConfString(configFiles, 2, CONFIG_NC_CHECK_BUCKET);
	if (tmp) {
	  snprintf (nc_state.ncCheckBucketCmd, MAX_PATH, "%s", tmp);
	  free(tmp);
	} else {
	  snprintf (nc_state.ncCheckBucketCmd, MAX_PATH, "%s", EUCALYPTUS_NC_CHECK_BUCKET); // default value
	}

	// set NC helper path
	tmp = getConfString(configFiles, 2, CONFIG_NC_DELETE_BUNDLE);
	if (tmp) {
	  snprintf (nc_state.ncDeleteBundleCmd, MAX_PATH, "%s", tmp);
	  free(tmp);
	} else {
	  snprintf (nc_state.ncDeleteBundleCmd, MAX_PATH, "%s", EUCALYPTUS_NC_DELETE_BUNDLE); // default value
	}
    
	{ // find and set iqn
        snprintf(nc_state.iqn, CHAR_BUFFER_SIZE, "UNSET");
        char *ptr=NULL, *iqn=NULL, *tmp=NULL, cmd[MAX_PATH];
        snprintf(cmd, MAX_PATH, "%s cat /etc/iscsi/initiatorname.iscsi", nc_state.rootwrap_cmd_path);
        ptr = system_output(cmd);
        if (ptr) {
            iqn = strstr(ptr, "InitiatorName=");
            if (iqn) {
                iqn+=strlen("InitiatorName=");
                tmp=strstr(iqn, "\n");
                if (tmp) *tmp='\0';
                snprintf(nc_state.iqn, CHAR_BUFFER_SIZE, "%s", iqn);
            } 
            free(ptr);
        }
	}
    
	{ // start the monitoring thread
        pthread_t tcb;
        if (pthread_create(&tcb, NULL, monitoring_thread, &nc_state)) {
            logprintfl (EUCAFATAL, "failed to spawn a monitoring thread\n");
            return ERROR_FATAL;
        }
        if (pthread_detach(tcb)) {
            logprintfl (EUCAFATAL, "failed to detach the monitoring thread\n");
            return ERROR_FATAL;
        }
    }

    // post-init hook
    if (call_hooks (NC_EVENT_POST_INIT, nc_state.home)) {
        logprintfl (EUCAFATAL, "hooks prevented initialization\n");
        return ERROR_FATAL;
    }
    
	initialized = 1;
	return OK;
}


int doDescribeInstances (ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
    int ret, len;
	char *file_name;
	FILE *f;
	long long used_mem, used_disk, used_cores;
#define NC_MONIT_FILENAME "/var/run/eucalyptus/nc-stats"
    
	if (init())
		return 1;
    
	logprintfl (EUCADEBUG2, "doDescribeInstances: invoked\n");
    
	if (nc_state.H->doDescribeInstances)
		ret = nc_state.H->doDescribeInstances (&nc_state, meta, instIds, instIdsLen, outInsts, outInstsLen);
	else 
		ret = nc_state.D->doDescribeInstances (&nc_state, meta, instIds, instIdsLen, outInsts, outInstsLen);
    
	if (ret)
		return ret;
    
	for (int i=0; i < (*outInstsLen); i++) {
        ncInstance *instance = (*outInsts)[i];

        // construct a string summarizing the volumes attached to the instance
        char vols_str [128] = "";
        unsigned int vols_count = 0;
        for (int j=0; j < EUCA_MAX_VOLUMES; ++j) {
            ncVolume * volume = &instance->volumes[j];
            if (strlen (volume->volumeId)==0)
                continue;
            vols_count++;
            
            char * s;
            if (! strcmp(volume->stateName, VOL_STATE_ATTACHING))        s = "a";
            if (! strcmp(volume->stateName, VOL_STATE_ATTACHED))         s = "A";
            if (! strcmp(volume->stateName, VOL_STATE_ATTACHING_FAILED)) s = "af";
            if (! strcmp(volume->stateName, VOL_STATE_DETACHING))        s = "d";
            if (! strcmp(volume->stateName, VOL_STATE_DETACHED))         s = "D";
            if (! strcmp(volume->stateName, VOL_STATE_DETACHING_FAILED)) s = "df";
            
            char vol_str [16];
            snprintf (vol_str, sizeof (vol_str), "%s%s:%s", 
                      (vols_count>1)?(","):(""),
                      volume->volumeId, 
                      s);
            if ((strlen (vols_str) + strlen (vol_str)) < sizeof (vols_str)) {
                strcat (vols_str, vol_str);
            }
        }
    
        logprintfl(EUCADEBUG, "[%s] %s pub=%s priv=%s mac=%s vlan=%d net=%d plat=%s vols=%s\n", 
                   instance->instanceId,
                   instance->stateName,
                   instance->ncnet.publicIp, 
                   instance->ncnet.privateIp, 
                   instance->ncnet.privateMac, 
                   instance->ncnet.vlan, 
                   instance->ncnet.networkIndex, 
                   instance->platform,
                   vols_str);
	}
    
	// allocate enough memory
	len = (strlen(EUCALYPTUS_CONF_LOCATION) > strlen(NC_MONIT_FILENAME)) ? strlen(EUCALYPTUS_CONF_LOCATION) : strlen(NC_MONIT_FILENAME);
	len += 2 + strlen(nc_state.home);
	file_name = malloc(sizeof(char) * len);
	if (!file_name) {
		logprintfl(EUCAERROR, "Out of memory!\n");
		return ret;
	}
    
	sprintf(file_name, "%s/%s", nc_state.home, NC_MONIT_FILENAME);
	if (!strcmp(meta->userId, EUCALYPTUS_ADMIN)) {
		f = fopen(file_name, "w");
		if (!f) {
			f = fopen(file_name, "w+");
			if (!f)
				logprintfl(EUCAWARN, "Cannot create %s!\n", file_name);
			else {
				len = fileno(f);
				if (len > 0)
					fchmod(len, S_IRUSR|S_IWUSR);
			}
		}
		if (f) {
			int i;
			ncInstance * instance;
			char myName[CHAR_BUFFER_SIZE];
            
			fprintf(f, "version: %s\n", EUCA_VERSION);
			fprintf(f, "timestamp: %ld\n", time(NULL));
			if (gethostname(myName, CHAR_BUFFER_SIZE) == 0)
				fprintf(f, "node: %s\n", myName);
			fprintf(f, "hypervisor: %s\n", nc_state.H->name);
			fprintf(f, "network: %s\n", nc_state.vnetconfig->mode);
            
			used_disk = used_mem = used_cores = 0;
			for (i=0; i < (*outInstsLen); i++) {
				instance = (*outInsts)[i];
				used_disk += instance->params.disk;
				used_mem += instance->params.mem;
				used_cores += instance->params.cores;
			}
            
			fprintf(f, "memory (max/avail/used) MB: %lld/%lld/%lld\n", nc_state.mem_max, nc_state.mem_max - used_mem, used_mem);
			fprintf(f, "disk (max/avail/used) GB: %lld/%lld/%lld\n", nc_state.disk_max, nc_state.disk_max - used_disk, used_disk);
			fprintf(f, "cores (max/avail/used): %lld/%lld/%lld\n", nc_state.cores_max, nc_state.cores_max - used_cores, used_cores);
            
			for (i=0; i < (*outInstsLen); i++) {
				instance = (*outInsts)[i];
				fprintf(f, "id: %s", instance->instanceId);
				fprintf(f, " userId: %s", instance->userId);
				fprintf(f, " state: %s", instance->stateName);
				fprintf(f, " mem: %d", instance->params.mem);
				fprintf(f, " disk: %d", instance->params.disk);
				fprintf(f, " cores: %d", instance->params.cores);
				fprintf(f, " private: %s", instance->ncnet.privateIp);
				fprintf(f, " public: %s\n", instance->ncnet.publicIp);
			}
			fclose(f);
		}
	}
	free(file_name);
    
	return ret;
}

int doAssignAddress(ncMetadata *meta, char *instanceId, char *publicIp) 
{
    int ret=0;
    
    if (init()) {    
        return(1);
    }
    
    logprintfl(EUCADEBUG, "[%s] doAssignAddress: invoked (publicIp=%s)\n", instanceId, publicIp);
    
    if (nc_state.H->doAssignAddress) 
        ret = nc_state.H->doAssignAddress(&nc_state, meta, instanceId, publicIp);
    else 
        ret = nc_state.D->doAssignAddress(&nc_state, meta, instanceId, publicIp);
    
    return ret;
}

int doPowerDown(ncMetadata *meta) {
	int ret;
    
	if (init())
		return 1;
    
	logprintfl(EUCADEBUG, "doPowerDown: invoked\n");
    
	if (nc_state.H->doPowerDown) 
		ret = nc_state.H->doPowerDown(&nc_state, meta);
	else 
		ret = nc_state.D->doPowerDown(&nc_state, meta);
    
	return ret;
}

int doRunInstance (ncMetadata *meta, char *uuid, char *instanceId, char *reservationId, virtualMachine *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId, char *accountId, char *keyName, netConfig *netparams, char *userData, char *launchIndex, char *platform, int expiryTime, char **groupNames, int groupNamesSize, ncInstance **outInst)
{
    int ret;
    
    if (init())
        return 1;
    
    logprintfl (EUCAINFO, "[%s] doRunInstance: cores=%d disk=%d memory=%d\n", instanceId, params->cores, params->disk, params->mem);
    logprintfl (EUCAINFO, "[%s]                vlan=%d priMAC=%s privIp=%s\n", instanceId, netparams->vlan, netparams->privateMac, netparams->privateIp);
    
    if (vbr_legacy (instanceId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL) != OK)
        return ERROR;
    
    if (nc_state.H->doRunInstance)
        ret = nc_state.H->doRunInstance (&nc_state, meta, uuid, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, ownerId, accountId, keyName, netparams, userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize, outInst);
    else
        ret = nc_state.D->doRunInstance (&nc_state, meta, uuid, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, ownerId, accountId, keyName, netparams, userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize, outInst);
    
    return ret;
}

int doTerminateInstance (ncMetadata *meta, char *instanceId, int force, int *shutdownState, int *previousState)
{
	int ret; 
    
	if (init())
		return 1;
    
	logprintfl (EUCAINFO, "[%s] doTerminateInstance: invoked\n", instanceId);
    
	if (nc_state.H->doTerminateInstance) 
		ret = nc_state.H->doTerminateInstance(&nc_state, meta, instanceId, force, shutdownState, previousState);
	else 
		ret = nc_state.D->doTerminateInstance(&nc_state, meta, instanceId, force, shutdownState, previousState);
    
	return ret;
}

int doRebootInstance (ncMetadata *meta, char *instanceId) 
{
	int ret;
    
	if (init())
		return 1;
    
	logprintfl(EUCAINFO, "[%s] doRebootInstance: invoked\n", instanceId);
    
	if (nc_state.H->doRebootInstance)
		ret = nc_state.H->doRebootInstance (&nc_state, meta, instanceId);
	else
		ret = nc_state.D->doRebootInstance (&nc_state, meta, instanceId);
    
	return ret;
}

int doGetConsoleOutput (ncMetadata *meta, char *instanceId, char **consoleOutput) 
{
	int ret;
    
	if (init())
		return 1;
    
	logprintfl (EUCAINFO, "[%s] doGetConsoleOutput: invoked\n", instanceId);
    
	if (nc_state.H->doGetConsoleOutput) 
		ret = nc_state.H->doGetConsoleOutput (&nc_state, meta, instanceId, consoleOutput);
	else
		ret = nc_state.D->doGetConsoleOutput (&nc_state, meta, instanceId, consoleOutput);
    
	return ret;
}

int doDescribeResource (ncMetadata *meta, char *resourceType, ncResource **outRes)
{
	int ret;
    
	if (init())
		return 1;
    
	if (nc_state.H->doDescribeResource)
		ret = nc_state.H->doDescribeResource (&nc_state, meta, resourceType, outRes);
	else 
		ret = nc_state.D->doDescribeResource (&nc_state, meta, resourceType, outRes);
    
	return ret;
}

int
doStartNetwork (ncMetadata *ccMeta,
                char *uuid,
                char **remoteHosts,
                int remoteHostsLen,
                int port,
                int vlan)
{
	int ret;
    
	if (init())
		return 1;
    
	logprintfl(EUCADEBUG, "doStartNetwork: invoked (remoteHostsLen=%d port=%d vlan=%d)\n", remoteHostsLen, port, vlan);
    
	if (nc_state.H->doStartNetwork) 
        ret = nc_state.H->doStartNetwork (&nc_state, ccMeta, uuid, remoteHosts, remoteHostsLen, port, vlan);
	else 
        ret = nc_state.D->doStartNetwork (&nc_state, ccMeta, uuid, remoteHosts, remoteHostsLen, port, vlan);
	
	return ret;
}

int doAttachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
	int ret;

	if (init())
		return 1;
    
	logprintfl (EUCAINFO, "[%s] doAttachVolume: invoked (vol=%s remote=%s local=%s)\n", instanceId, volumeId, remoteDev, localDev);
    
	if (nc_state.H->doAttachVolume)
		ret = nc_state.H->doAttachVolume(&nc_state, meta, instanceId, volumeId, remoteDev, localDev);
	else
		ret = nc_state.D->doAttachVolume(&nc_state, meta, instanceId, volumeId, remoteDev, localDev);
	
	return ret;
}

int doDetachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force, int grab_inst_sem)
{
	int ret;
    
	if (init())
		return 1;
    
	logprintfl (EUCAINFO, "[%s] doDetachVolume: invoked (vol=%s remote=%s local=%s force=%d)\n", instanceId, volumeId, remoteDev, localDev, force);
    
	if (nc_state.H->doDetachVolume)
		ret = nc_state.H->doDetachVolume (&nc_state, meta, instanceId, volumeId, remoteDev, localDev, force, grab_inst_sem);
	else 
		ret = nc_state.D->doDetachVolume (&nc_state, meta, instanceId, volumeId, remoteDev, localDev, force, grab_inst_sem);
    
	return ret;
}

int doBundleInstance (ncMetadata *meta, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL, char *userPublicKey, char *S3Policy, char *S3PolicySig)
{
	int ret;
    
	if (init())
		return 1;
    
	logprintfl (EUCAINFO, "[%s] doBundleInstance: invoked (bucketName=%s filePrefix=%s walrusURL=%s userPublicKey=%s S3Policy=%s, S3PolicySig=%s)\n", 
                instanceId, bucketName, filePrefix, walrusURL, userPublicKey, S3Policy, S3PolicySig);
    
	if (nc_state.H->doBundleInstance)
        ret = nc_state.H->doBundleInstance (&nc_state, meta, instanceId, bucketName, filePrefix, walrusURL, userPublicKey, S3Policy, S3PolicySig);
	else 
        ret = nc_state.D->doBundleInstance (&nc_state, meta, instanceId, bucketName, filePrefix, walrusURL, userPublicKey, S3Policy, S3PolicySig);
    
	return ret;
}

int doCancelBundleTask (ncMetadata *meta, char *instanceId)
{
	int ret;
    
	if (init())
		return 1;
    
	logprintfl (EUCAINFO, "[%s] doCancelBundleTask: invoked\n", instanceId);
    
	if (nc_state.H->doCancelBundleTask)
        ret = nc_state.H->doCancelBundleTask (&nc_state, meta, instanceId);
	else 
        ret = nc_state.D->doCancelBundleTask (&nc_state, meta, instanceId);
    
	return ret;
}

int doDescribeBundleTasks (ncMetadata *meta, char **instIds, int instIdsLen, bundleTask ***outBundleTasks, int *outBundleTasksLen)
{
	int ret;
    
	if (init())
		return 1;
	
	logprintfl (EUCAINFO, "doDescribeBundleTasks: invoked (for %d instances)\n", instIdsLen);
    
	if (nc_state.H->doDescribeBundleTasks)
        ret = nc_state.H->doDescribeBundleTasks (&nc_state, meta, instIds, instIdsLen, outBundleTasks, outBundleTasksLen);
	else 
        ret = nc_state.D->doDescribeBundleTasks (&nc_state, meta, instIds, instIdsLen, outBundleTasks, outBundleTasksLen);
    
	return ret;
}

int doCreateImage (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev)
{
	int ret;
    
	if (init())
		return 1;
    
	logprintfl (EUCAINFO, "[%s] doCreateImage: invoked (vol=%s remote=%s)\n", instanceId, volumeId, remoteDev);
    
	if (nc_state.H->doCreateImage)
		ret = nc_state.H->doCreateImage (&nc_state, meta, instanceId, volumeId, remoteDev);
	else 
		ret = nc_state.D->doCreateImage (&nc_state, meta, instanceId, volumeId, remoteDev);
    
	return ret;
}

int get_instance_stats(virDomainPtr dom, ncInstance *instance)
{
    char *xml;
    int ret=0, n;
    long long b=0, i=0;
    char bstr[512], istr[512];
    
    // get the block device string from VBR
    bzero(bstr, 512);
    for (n=0; n<instance->params.virtualBootRecordLen; n++) {
        if (strcmp(instance->params.virtualBootRecord[n].guestDeviceName, "none")) {
            if (strlen(bstr) < (510 - strlen(instance->params.virtualBootRecord[n].guestDeviceName))) {
                strcat(bstr, instance->params.virtualBootRecord[n].guestDeviceName);
                strcat(bstr, ",");
            }
        }
    }
    
    // get the name of the network interface from libvirt
    sem_p(hyp_sem);
    xml = virDomainGetXMLDesc(dom, 0);
    sem_v(hyp_sem);
    
    if (xml) {
        char *el;
        el = xpath_content(xml, "domain/devices/interface");
        if (el) {
            char *start, *end;
            start = strstr(el, "target dev='");
            if (start) {
                start += strlen("target dev='");
                end = strstr(start, "'");
                if (end) {
                    *end = '\0';
                    snprintf(istr, 512, "%s", start);
                }
            }
            free(el);
        }
        free(xml);
    }
    
    char cmd[MAX_PATH], *output;
    snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap %s/usr/share/eucalyptus/getstats.pl -i %s -b '%s' -n '%s'", 
             nc_state.home, nc_state.home, instance->instanceId, bstr, istr);
    output = system_output (cmd);
    if (output) {
        sscanf(output, "OUTPUT %lld %lld", &b, &i);
        free(output);
    } else {
        logprintfl(EUCAWARN, "[%s] warning: get_instance_stats: empty output from getstats command\n", instance->instanceId);
        ret = 1;
    }
    
    if (b > 0) {
        instance->blkbytes = b;
    } else {
        instance->blkbytes = 0;
    }
    if (i > 0) {
        instance->netbytes = i;
    } else {
        instance->netbytes = 0;
    }
    logprintfl(EUCADEBUG, "[%s] get_instance_stats: blkdevs=%s, blkbytes=%lld, netdevs=%s, netbytes=%lld\n", 
               instance->instanceId, bstr, instance->blkbytes, istr, instance->netbytes);
    
  return(ret);
}

ncInstance * find_global_instance (const char * instanceId)
{
    return NULL;
}

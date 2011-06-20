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

#include "windows-bundle.h"
#define MONITORING_PERIOD (5)

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
		logprintfl (EUCAERROR, "libvirt: %s (code=%d)\n", error->message, error->code);
	}
}

int convert_dev_names(	char *localDev,
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
	if (nc_state.conn == NULL || virConnectGetURI(nc_state.conn) == NULL) {
		nc_state.conn = virConnectOpen (nc_state.uri);
		if (nc_state.conn == NULL) {
			logprintfl (EUCAFATAL, "Failed to connect to %s\n", nc_state.uri);
			return NULL;
		}
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
    int now = instance->state;
    
    if (! check_hypervisor_conn ())
	    return;
    
    /* no need to bug for domains without state on Hypervisor */
    if (now==TEARDOWN || now==STAGING || now==BUNDLING_SHUTOFF || now==CREATEIMAGE_SHUTOFF)
        return;
    
    sem_p(hyp_sem);
    virDomainPtr dom = virDomainLookupByName (nc_state.conn, instance->instanceId);
    sem_v(hyp_sem);
    if (dom == NULL) { /* hypervisor doesn't know about it */
        if (now==BUNDLING_SHUTDOWN) {
            logprintfl (EUCAINFO, "[%s] detected disappearance of bundled domain\n", instance->instanceId);
            change_state (instance, BUNDLING_SHUTOFF);
        } else if (now==CREATEIMAGE_SHUTDOWN) {
            logprintfl (EUCAINFO, "[%s] detected disappearance of createImage domain\n", instance->instanceId);
            change_state (instance, CREATEIMAGE_SHUTOFF);
        } else if (now==RUNNING ||
                   now==BLOCKED ||
                   now==PAUSED ||
                   now==SHUTDOWN) {
            // Most likely the user has shut it down from the inside
            if (instance->retries) {
                instance->retries--;
                logprintfl (EUCAWARN, "[%s] warning: hypervisor failed to find domain, will retry %d more times\n", instance->instanceId, instance->retries);
            } else {
            	logprintfl (EUCAWARN, "[%s] warning: hypervisor failed to find domain, assuming it was shut off\n", instance->instanceId);
            	change_state (instance, SHUTOFF);
            }
        }
        // else 'now' stays in SHUTFOFF, BOOTING, CANCELED, or CRASHED
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
    int xen = info.state;
    
    switch (now) {
    case BOOTING:
    case RUNNING:
    case BLOCKED:
    case PAUSED:
        // change to state, whatever it happens to be
        change_state (instance, xen);
        break;
    case SHUTDOWN:
    case SHUTOFF:
    case CRASHED:
        if (xen==RUNNING ||
            xen==BLOCKED ||
            xen==PAUSED) {
            // cannot go back!
            logprintfl (EUCAWARN, "[%s] warning: detected prodigal domain, terminating it\n", instance->instanceId);
            sem_p (hyp_sem);
            virDomainDestroy (dom);
            sem_v (hyp_sem);
        } else {
            change_state (instance, xen);
        }
        break;
    case BUNDLING_SHUTDOWN:
    case CREATEIMAGE_SHUTDOWN:
        logprintfl (EUCADEBUG, "[%s] hypervisor state for bundle/createImage domain is %s\n", instance->instanceId, instance_state_names [xen]);
        break;
    default:
        logprintfl (EUCAERROR, "[%s] error: refresh...(): unexpected state (%d)\n", instance->instanceId, now);
        return;
    }
    sem_p(hyp_sem);
    virDomainFree(dom);
    sem_v(hyp_sem);
    
    /* if instance is running, try to find out its IP address */
    if (instance->state==RUNNING ||
        instance->state==BLOCKED ||
        instance->state==PAUSED) {
        char *ip=NULL;
        int rc;
        
        if (!strncmp(instance->ncnet.publicIp, "0.0.0.0", 24)) {
            if (!strcmp(nc_state.vnetconfig->mode, "SYSTEM") || !strcmp(nc_state.vnetconfig->mode, "STATIC")) {
                rc = mac2ip(nc_state.vnetconfig, instance->ncnet.privateMac, &ip);
                if (!rc) {
                    if(ip) {
                        logprintfl (EUCAINFO, "[%s] discovered public IP %s for instance\n", instance->instanceId, ip);
                        safe_strncpy(instance->ncnet.publicIp, ip, 24);
                        free(ip);
                    }
                }
            }
        }
        if (!strncmp(instance->ncnet.privateIp, "0.0.0.0", 24)) {
            rc = mac2ip(nc_state.vnetconfig, instance->ncnet.privateMac, &ip);
            if (!rc) {
                if(ip) {
                    logprintfl (EUCAINFO, "[%s] discovered private IP %s for instance\n", instance->instanceId, ip);
                    safe_strncpy(instance->ncnet.privateIp, ip, 24);
                    free(ip);
                }
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
            
            // ok, it's been condemned => destroy the files
            int destroy_files = !nc_state.save_instance_files;
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
    virDomainPtr dom = NULL;
    char * disk_path, * xml=NULL;
    char *brname=NULL;
    int error, i;
    
    if (! check_hypervisor_conn ()) {
        logprintfl (EUCAFATAL, "[%s] could not contact the hypervisor, abandoning the instance\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        goto free;
    }
    
    // set up networking
    error = vnetStartNetwork (nc_state.vnetconfig, instance->ncnet.vlan, NULL, NULL, NULL, &brname);
    if (error) {
        logprintfl (EUCAFATAL, "[%s] start network failed for instance, terminating it\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        goto free;
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
    instance->combinePartitions = nc_state.convert_to_disk; 
    instance->do_inject_key = nc_state.do_inject_key;

    char xslt_path [1024];
    snprintf (xslt_path, sizeof (xslt_path), "%s/etc/eucalyptus/libvirt.xsl", nc_state.home);
    if ((error = create_instance_backing (instance))
        || (error = gen_instance_xml (instance))
        || (error = gen_libvirt_xml (instance, xslt_path))) {
        
        logprintfl (EUCAFATAL, "[%s] error: failed to prepare images for instance (error=%d)\n", instance->instanceId, error);
        change_state (instance, SHUTOFF);
        goto free;
    }
    xml = file2str (instance->libvirtFilePath);
    
    if (instance->state==TEARDOWN) { // timed out in STAGING
        goto free;
    }
    if (instance->state==CANCELED) {
        logprintfl (EUCAFATAL, "[%s] startup of instance was cancelled\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        goto free;
    }
    
    save_instance_struct (instance); // to enable NC recovery

    // serialize domain creation as hypervisors can get confused with
    // too many simultaneous create requests 
    logprintfl (EUCADEBUG2, "[%s] instance about to boot\n", instance->instanceId);

    for (i=0; i<5 && dom == NULL; i++) {
      sem_p (hyp_sem);
      sem_p (loop_sem);
      dom = virDomainCreateLinux (nc_state.conn, xml, 0);
      sem_v (loop_sem);
      sem_v (hyp_sem);
    }

    if (dom == NULL) {
        logprintfl (EUCAFATAL, "[%s] hypervisor failed to start instance\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        goto free;
    }
    eventlog("NC", instance->userId, "", "instanceBoot", "begin"); // TODO: bring back correlationId
    
    sem_p(hyp_sem);
    virDomainFree(dom);
    sem_v(hyp_sem);

    sem_p (inst_sem);
    // check one more time for cancellation
    if (instance->state==TEARDOWN) { 
        // timed out in BOOTING
    } else if (instance->state==CANCELED || instance->state==SHUTOFF) {
        logprintfl (EUCAFATAL, "[%s] startup of instance was cancelled\n", instance->instanceId);
        change_state (instance, SHUTOFF);
    } else {
        logprintfl (EUCAINFO, "[%s] booting\n", instance->instanceId);
        instance->bootTime = time (NULL);
        change_state (instance, BOOTING);
    }
    sem_v (inst_sem);
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

		change_state (instance, info.state);                    
		sem_p (inst_sem);
		int err = add_instance (&global_instances, instance);
		sem_v (inst_sem);
		if (err) {
			free_instance (&instance);
			continue;
		}

		logprintfl (EUCAINFO, "- adopted running domain %s from user %s\n", instance->instanceId, instance->userId);
		/* TODO: try to look up IPs? */

		sem_p(hyp_sem);
		virDomainFree (dom);
		sem_v(hyp_sem);
	}
}

static int init (void)
{
	static int initialized = 0;
	int do_warn = 0, i;
	char configFiles[2][MAX_PATH],
		log[MAX_PATH],
		*bridge,
		*hypervisor,
		*s,
        *tmp;
	struct stat mystat;
	struct statfs fs;
	struct handlers ** h; 
	long long fs_free_blocks = 0;
	long long fs_block_size  = 0;
	long long instances_bytes = 0; // TODO: get this value from instace backing code
	pthread_t tcb;

	if (initialized>0) /* 0 => hasn't run, -1 => failed, 1 => ok */
		return 0;
	else if (initialized<0)
		return 1;

	bzero (&nc_state, sizeof(struct nc_state_t)); // ensure that MAXes are zeroed out

	/* read in configuration - this should be first! */
	tmp = getenv(EUCALYPTUS_ENV_VAR_NAME);
	if (!tmp) {
		nc_state.home[0] = '\0';
		do_warn = 1;
	} else {
		strncpy(nc_state.home, tmp, MAX_PATH - 1);
    }

	/* set the minimum log for now */
	snprintf(log, MAX_PATH, "%s/var/log/eucalyptus/nc.log", nc_state.home);
	logfile(log, EUCADEBUG);
        logprintfl (EUCAINFO, "Eucalyptus node controller initializing %s\n", compile_timestamp_str);

	if (do_warn) 
		logprintfl (EUCAWARN, "env variable %s not set, using /\n", EUCALYPTUS_ENV_VAR_NAME);

	/* search for the config file */
	snprintf(configFiles[1], MAX_PATH, EUCALYPTUS_CONF_LOCATION, nc_state.home);
	if (stat(configFiles[1], &mystat)) {
		logprintfl (EUCAFATAL, "could not open configuration file %s\n", configFiles[1]);
		return 1;
	}
	snprintf(configFiles[0], MAX_PATH, EUCALYPTUS_CONF_OVERRIDE_LOCATION, nc_state.home);

	logprintfl (EUCAINFO, "NC is looking for configuration in %s,%s\n", configFiles[1], configFiles[0]);

	/* reset the log to the right value */
	tmp = getConfString(configFiles, 2, "LOGLEVEL");
	i = EUCADEBUG;
	if (tmp) {
		if (!strcmp(tmp,"INFO")) {i=EUCAINFO;}
		else if (!strcmp(tmp,"WARN")) {i=EUCAWARN;}
		else if (!strcmp(tmp,"ERROR")) {i=EUCAERROR;}
		else if (!strcmp(tmp,"FATAL")) {i=EUCAFATAL;}
		free(tmp);
	}
	logfile(log, i);

#define GET_VAR_INT(var,name) \
        s = getConfString(configFiles, 2, name); \
	if (s){					\
		var = atoi(s);\
		free (s);\
	}

	GET_VAR_INT(nc_state.config_max_mem,      CONFIG_MAX_MEM);
	GET_VAR_INT(nc_state.config_max_disk,     CONFIG_MAX_DISK);
	GET_VAR_INT(nc_state.config_max_cores,    CONFIG_MAX_CORES);
	GET_VAR_INT(nc_state.save_instance_files, CONFIG_SAVE_INSTANCES);
    int disable_injection = 0;
    GET_VAR_INT(disable_injection, CONFIG_DISABLE_KEY_INJECTION); 
    nc_state.do_inject_key = !disable_injection;
    nc_state.config_network_port = NC_NET_PORT_DEFAULT;
    strcpy(nc_state.admin_user_id, EUCALYPTUS_ADMIN);
                
    add_euca_to_path (nc_state.home); // add three eucalyptus directories with executables to PATH of this process
                
	if (euca_init_cert ()) {
	  logprintfl (EUCAERROR, "init(): failed to find cryptographic certificates\n");
	  return 1;
	}

	/* from now on we have unrecoverable failure, so no point in
	 * retrying to re-init */
	initialized = -1;

	hyp_sem = sem_alloc (1, "mutex");
	inst_sem = sem_alloc (1, "mutex");
	addkey_sem = sem_alloc (1, "mutex");
	if (!hyp_sem || !inst_sem || !addkey_sem) {
		logprintfl (EUCAFATAL, "failed to create and initialize semaphores\n");
		return ERROR_FATAL;
	}

    if (diskutil_init() || (loop_sem = diskutil_get_loop_sem())==NULL) {
        logprintfl (EUCAFATAL, "failed to find all dependencies\n");
		return ERROR_FATAL;
    }

    init_iscsi (nc_state.home);

	/* set default in the paths. the driver will override */
	nc_state.config_network_path[0] = '\0';
	nc_state.gen_libvirt_cmd_path[0] = '\0';
	nc_state.xm_cmd_path[0] = '\0';
	nc_state.virsh_cmd_path[0] = '\0';
	nc_state.get_info_cmd_path[0] = '\0';
	snprintf (nc_state.rootwrap_cmd_path, MAX_PATH, EUCALYPTUS_ROOTWRAP, nc_state.home);

    // backing store configuration
    int cache_size_mb = DEFAULT_NC_CACHE_SIZE;
    int work_size_mb = DEFAULT_NC_WORK_SIZE;
    GET_VAR_INT(cache_size_mb, CONFIG_NC_CACHE_SIZE);
    GET_VAR_INT(work_size_mb, CONFIG_NC_WORK_SIZE);
    char * instance_path = getConfString(configFiles, 2, INSTANCE_PATH);
    if (init_backing_store (instance_path, work_size_mb, cache_size_mb)) {
        logprintfl (EUCAFATAL, "error: failed to initialize backing store\n");
        return ERROR_FATAL;
	}
	if (statfs (instance_path, &fs) == -1) { // TODO: get the values from instance backing code
		logprintfl(EUCAWARN, "Failed to stat %s\n", instance_path);
	}  else {
		nc_state.disk_max = (long long)fs.f_bsize * (long long)fs.f_bavail + instances_bytes; /* max for Euca, not total */
		nc_state.disk_max /= BYTES_PER_DISK_UNIT;
		if (nc_state.config_max_disk && nc_state.config_max_disk < nc_state.disk_max)
			nc_state.disk_max = nc_state.config_max_disk;

		logprintfl (EUCAINFO, "Maximum disk available: %lld (under %s)\n", nc_state.disk_max, instance_path);
	}
    if (instance_path) free (instance_path);

	// determine the hypervisor to use
	
	//if (get_conf_var(config, CONFIG_HYPERVISOR, &hypervisor)<1) {
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
		GET_VAR_INT(nc_state.config_use_virtio_net, CONFIG_USE_VIRTIO_NET);
		GET_VAR_INT(nc_state.config_use_virtio_disk, CONFIG_USE_VIRTIO_DISK);
		GET_VAR_INT(nc_state.config_use_virtio_root, CONFIG_USE_VIRTIO_ROOT);
	}
	free (hypervisor);

    // 

	/* NOTE: this is the only call which needs to be called on both
	 * the default and the specific handler! All the others will be
	 * either or */
	i = nc_state.D->doInitialize(&nc_state);
	if (nc_state.H->doInitialize)
		i += nc_state.H->doInitialize(&nc_state);
	if (i) {
		logprintfl(EUCAFATAL, "ERROR: failed to initialized hypervisor driver!\n");
		return ERROR_FATAL;
	}

	/* adopt running instances */
	adopt_instances();

	/* setup the network */
	nc_state.vnetconfig = malloc(sizeof(vnetConfig));
	if (!nc_state.vnetconfig) {
		logprintfl (EUCAFATAL, "Cannot allocate vnetconfig!\n");
		return 1;
	}
	snprintf (nc_state.config_network_path, MAX_PATH, NC_NET_PATH_DEFAULT, nc_state.home);
	hypervisor = getConfString(configFiles, 2, "VNET_PUBINTERFACE");
	if (!hypervisor) 
		hypervisor = getConfString(configFiles, 2, "VNET_INTERFACE");
	bridge = getConfString(configFiles, 2, "VNET_BRIDGE");
	tmp = getConfString(configFiles, 2, "VNET_MODE");
	
	vnetInit(nc_state.vnetconfig, tmp, nc_state.home, nc_state.config_network_path, NC, hypervisor, hypervisor, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, bridge, NULL, NULL);
	if (hypervisor) free(hypervisor);
	if (bridge) free(bridge);
	if (tmp) free(tmp);

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

	// find and set iqn
	{
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

	/* start the monitoring thread */
	if (pthread_create(&tcb, NULL, monitoring_thread, &nc_state)) {
		logprintfl (EUCAFATAL, "failed to spawn a monitoring thread\n");
		return ERROR_FATAL;
	}
        if (pthread_detach(tcb)) {
          logprintfl(EUCAFATAL, "failed to detach the monitoring thread\n");
          return ERROR_FATAL;
        }


	initialized = 1;

	return OK;
}


int doDescribeInstances (ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
    int ret, len, i;
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
    
    
	for (i=0; i < (*outInstsLen); i++) {
        ncInstance *instance = (*outInsts)[i];
        logprintfl(EUCADEBUG, "[%s] %s publicIp=%s privateIp=%s mac=%s vlan=%d networkIndex=%d platform=%s\n", 
                   instance->instanceId,
                   instance->stateName,
                   instance->ncnet.publicIp, 
                   instance->ncnet.privateIp, 
                   instance->ncnet.privateMac, 
                   instance->ncnet.vlan, 
                   instance->ncnet.networkIndex, 
                   instance->platform);
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

int doRunInstance (ncMetadata *meta, char *uuid, char *instanceId, char *reservationId, virtualMachine *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, netConfig *netparams, char *userData, char *launchIndex, char *platform, int expiryTime, char **groupNames, int groupNamesSize, ncInstance **outInst)
{
    int ret;
    
    if (init())
        return 1;
    
    logprintfl (EUCAINFO, "[%s] doRunInstance: cores=%d disk=%d memory=%d\n", instanceId, params->cores, params->disk, params->mem);
    logprintfl (EUCAINFO, "[%s]                vlan=%d priMAC=%s privIp=%s\n", instanceId, netparams->vlan, netparams->privateMac, netparams->privateIp);
    
    if (vbr_legacy (instanceId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL) != OK)
        return ERROR;
    
    if (nc_state.H->doRunInstance)
        ret = nc_state.H->doRunInstance (&nc_state, meta, uuid, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, netparams, userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize, outInst);
    else
        ret = nc_state.D->doRunInstance (&nc_state, meta, uuid, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, netparams, userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize, outInst);
    
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

int generate_attach_xml(char *localDevReal, char *remoteDev, struct nc_state_t *nc, char *xml) {
        int virtio_dev = 0;
        int rc = 0;
        struct stat statbuf;
        /* only attach using virtio when the device is /dev/vdXX */
        if (localDevReal[5] == 'v' && localDevReal[6] == 'd') {
             virtio_dev = 1;
        }
        if (nc->config_use_virtio_disk && virtio_dev) {
             snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s' bus='virtio'/></disk>", remoteDev, localDevReal);
        } else {
             snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s'/></disk>", remoteDev, localDevReal);
        }
        rc = stat(remoteDev, &statbuf);
        if (rc) {
             logprintfl(EUCAERROR, "AttachVolume(): cannot locate local block device file '%s'\n", remoteDev);
             rc = 1;
        }
        return rc;
}


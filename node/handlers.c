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
#define _FILE_OFFSET_BITS 64
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
#define HANDLERS_FANOUT
#include <handlers.h>
#include <storage.h>
#include <eucalyptus.h>

#define MONITORING_PERIOD (5)

/* used by lower level handlers */
sem *hyp_sem;	/* semaphore for serializing domain creation */
sem *inst_sem;	/* guarding access to global instance structs */
sem *addkey_sem;	/* guarding access to global instance structs */

bunchOfInstances *global_instances = NULL; 

// declarations of available handlers
extern struct handlers xen_libvirt_handlers;
extern struct handlers kvm_libvirt_handlers;
extern struct handlers default_libvirt_handlers;

const int staging_cleanup_threshold = 60 * 60 * 2; /* after this many seconds any STAGING domains will be cleaned up */
const int booting_cleanup_threshold = 60; /* after this many seconds any BOOTING domains will be cleaned up */
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
    case SHUTDOWN:
    case SHUTOFF:
    case CRASHED:
        instance->stateCode = EXTANT;
	instance->retries = LIBVIRT_QUERY_RETRIES;
        break;
    case TEARDOWN:
        instance->stateCode = TEARDOWN;
        break;
    default:
        logprintfl (EUCAERROR, "error: change_sate(): unexpected state (%d) for instance %s\n", instance->state, instance->instanceId);        
        return;
    }

    strncpy(instance->stateName, instance_state_names[instance->stateCode], CHAR_BUFFER_SIZE);
    if (old_state != state) {
        logprintfl (EUCADEBUG, "state change for instance %s: %s -> %s (%s)\n", 
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
    if (now==TEARDOWN || now==STAGING)
        return;
    
    sem_p(hyp_sem);
    virDomainPtr dom = virDomainLookupByName (nc_state.conn, instance->instanceId);
    sem_v(hyp_sem);
    if (dom == NULL) { /* hypervisor doesn't know about it */
      if (now==RUNNING ||
            now==BLOCKED ||
            now==PAUSED ||
            now==SHUTDOWN) {
            /* Most likely the user has shut it down from the inside */
            if (instance->retries) {
		instance->retries--;
		logprintfl (EUCAWARN, "warning: hypervisor failed to find domain %s, will retry %d more times\n", instance->instanceId, instance->retries);	
            } else {
            	logprintfl (EUCAWARN, "warning: hypervisor failed to find domain %s, assuming it was shut off\n", instance->instanceId);
            	change_state (instance, SHUTOFF);
            }
        }
        /* else 'now' stays in SHUTFOFF, BOOTING, CANCELED, or CRASHED */
        return;
    }
    virDomainInfo info;
    sem_p(hyp_sem);
    int error = virDomainGetInfo(dom, &info);
    sem_v(hyp_sem);
    if (error < 0 || info.state == VIR_DOMAIN_NOSTATE) {
        logprintfl (EUCAWARN, "warning: failed to get informations for domain %s\n", instance->instanceId);
        /* what to do? hopefully we'll find out more later */
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
        /* change to state, whatever it happens to be */
        change_state (instance, xen);
        break;
    case SHUTDOWN:
    case SHUTOFF:
    case CRASHED:
        if (xen==RUNNING ||
            xen==BLOCKED ||
            xen==PAUSED) {
            /* cannot go back! */
            logprintfl (EUCAWARN, "warning: detected prodigal domain %s, terminating it\n", instance->instanceId);
            sem_p (hyp_sem);
            virDomainDestroy (dom);
            sem_v (hyp_sem);
        } else {
            change_state (instance, xen);
        }
        break;
    default:
        logprintfl (EUCAERROR, "error: refresh...(): unexpected state (%d) for instance %s\n", now, instance->instanceId);
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
	        logprintfl (EUCAINFO, "discovered public IP %s for instance %s\n", ip, instance->instanceId);
	        strncpy(instance->ncnet.publicIp, ip, 24);
	        free(ip);
	      }
            }
	  }
        }
        if (!strncmp(instance->ncnet.privateIp, "0.0.0.0", 24)) {
            rc = mac2ip(nc_state.vnetconfig, instance->ncnet.privateMac, &ip);
            if (!rc) {
		if(ip) {
                  logprintfl (EUCAINFO, "discovered private IP %s for instance %s\n", ip, instance->instanceId);
                  strncpy(instance->ncnet.privateIp, ip, 24);
	          free(ip);
		}
            }
        }
    }
}

int
get_instance_xml(	const char *gen_libvirt_cmd_path,
			char *userId,
			char *instanceId,
			char *ramdiskId,
			char *kernelId,
			char *disk_path,
			virtualMachine *params,
			char *privMac,
			//			char *privIp,
			char *brname,
			int use_virtio_net,
			int use_virtio_root,
			char **xml)
{
    char buf [MAX_PATH];

    snprintf(buf, MAX_PATH, "%s", gen_libvirt_cmd_path);
    if (strnlen(ramdiskId, CHAR_BUFFER_SIZE)) {
        strncat(buf, " --ramdisk", MAX_PATH);
    }
    if (use_virtio_net) {
        strncat(buf, " --virtionet", MAX_PATH);
    }
    if (use_virtio_root) {
        strncat(buf, " --virtioroot", MAX_PATH);
    }
    
    if (params->disk > 0) { /* TODO: get this info from scMakeImage */
        strncat (buf, " --ephemeral", MAX_PATH);
    }
    * xml = system_output (buf);
    if ( ( * xml ) == NULL ) {
        logprintfl (EUCAFATAL, "%s: %s\n", gen_libvirt_cmd_path, strerror (errno));
        return ERROR;
    }
    
    /* the tags better be not substring of other tags: BA will substitute
     * ABABABAB */
    replace_string (xml, "BASEPATH", disk_path);
    replace_string (xml, "SWAPPATH", disk_path);
    replace_string (xml, "NAME", instanceId);
    replace_string (xml, "PRIVMACADDR", privMac);
    //    replace_string (xml, "PUBMACADDR", pubMac);
    replace_string (xml, "BRIDGEDEV", brname);
    snprintf(buf, CHAR_BUFFER_SIZE, "%d", params->mem * 1024); /* because libvirt wants memory in Kb, while we use Mb */
    replace_string (xml, "MEMORY", buf);
    snprintf(buf, CHAR_BUFFER_SIZE, "%d", params->cores);
    replace_string (xml, "VCPUS", buf);
    
    return 0;
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

	logprintfl (EUCADEBUG, "Starting monitoring thread\n!\n");

    for (;;) {
        bunchOfInstances *head;
        time_t now = time(NULL);
        sem_p (inst_sem);

        for ( head = global_instances; head; head = head->next ) {
            ncInstance * instance = head->instance;

            /* query for current state, if any */
	    refresh_instance_info (nc, instance);

            /* don't touch running or canceled threads */
            if (instance->state!=STAGING && instance->state!=BOOTING && 
                instance->state!=SHUTOFF &&
                instance->state!=SHUTDOWN &&
                instance->state!=TEARDOWN) continue;

            if (instance->state==TEARDOWN) {
                /* it's been long enough, we can forget the instance */
                if ((now - instance->terminationTime)>teardown_state_duration) {
                    remove_instance (&global_instances, instance);
                    logprintfl (EUCAINFO, "forgetting about instance %s\n", instance->instanceId);
                    free_instance (&instance);
		    break;	/* need to get out since the list changed */
                }
                continue;
            }

	    // time out logic for STAGING or BOOTING instances
            if (instance->state==STAGING  
		&& (now - instance->launchTime)   < staging_cleanup_threshold) continue; // hasn't been long enough, spare it
            if (instance->state==BOOTING  
		&& (now - instance->bootTime)     < booting_cleanup_threshold) continue;
            
            /* ok, it's been condemned => destroy the files */
            if (!nc_state.save_instance_files) {
				logprintfl (EUCAINFO, "cleaning up state for instance %s\n", instance->instanceId);
	      if (scCleanupInstanceImage(instance->userId, instance->instanceId)) {
                logprintfl (EUCAWARN, "warning: failed to cleanup instance image %s\n", instance->instanceId);
	      }
			} else {
				logprintfl (EUCAINFO, "cleaning up state for instance %s (but keeping the files)\n", instance->instanceId);
			}
            
            /* check to see if this is the last instance running on vlan */
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
                logprintfl (EUCAINFO, "stopping the network (vlan=%d)\n", (instance->ncnet).vlan);
                vnetStopNetwork (nc_state.vnetconfig, (instance->ncnet).vlan, NULL, NULL);
            }
            change_state (instance, TEARDOWN); /* TEARDOWN = no more resources */
            instance->terminationTime = time (NULL);
        }
        sem_v (inst_sem);

	if (head) {
		/* we got out because of modified list, no need to sleep
		 * now */
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
        logprintfl (EUCAFATAL, "could not start instance %s, abandoning it\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    
    error = vnetStartNetwork (nc_state.vnetconfig, instance->ncnet.vlan, NULL, NULL, &brname);
    if ( error ) {
        logprintfl (EUCAFATAL, "start network failed for instance %s, terminating it\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    logprintfl (EUCAINFO, "network started for instance %s\n", instance->instanceId);
    
    error = scMakeInstanceImage (nc_state.home, 
				 instance->userId, 
                                 instance->imageId, instance->imageURL, 
                                 instance->kernelId, instance->kernelURL, 
                                 instance->ramdiskId, instance->ramdiskURL, 
                                 instance->instanceId, instance->keyName, 
				 &disk_path, 
				 addkey_sem, nc_state.convert_to_disk,
				 instance->params.disk*1024);
    if (error) {
        logprintfl (EUCAFATAL, "Failed to prepare images for instance %s (error=%d)\n", instance->instanceId, error);
        change_state (instance, SHUTOFF);
	if (brname) free(brname);
        return NULL;
    }
	if (instance->state==TEARDOWN) { // timed out in STAGING
		if (brname) free(brname);
        return NULL;
	}
    if (instance->state==CANCELED) {
        logprintfl (EUCAFATAL, "Startup of instance %s was cancelled\n", instance->instanceId);
        change_state (instance, SHUTOFF);
	if (brname) free(brname);
        return NULL;
    }
    
    error = get_instance_xml (nc_state.gen_libvirt_cmd_path,
		              instance->userId, instance->instanceId, 
			      instance->ramdiskId,
			      instance->kernelId,
                              disk_path, 
                              &(instance->params), 
                              instance->ncnet.privateMac, 
                              brname,
                              nc_state.config_use_virtio_net,
                              nc_state.config_use_virtio_root,
                              &xml);

    if (brname) free(brname);
    if (xml) logprintfl (EUCADEBUG2, "libvirt XML config:\n%s\n", xml);
    if (error) {
        logprintfl (EUCAFATAL, "Failed to create libvirt XML config for instance %s\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    
    scStoreStringToInstanceFile (instance->userId, instance->instanceId, "libvirt.xml", xml); /* for debugging */
    scSaveInstanceInfo(instance); /* to enable NC recovery */

    /* we serialize domain creation as hypervisors can get confused with
     * too many simultaneous create requests */
    logprintfl (EUCADEBUG2, "about to start domain %s\n", instance->instanceId);
    print_running_domains ();
    for (i=0; i<5 && dom == NULL; i++) {
      sem_p (hyp_sem);
      dom = virDomainCreateLinux (nc_state.conn, xml, 0);
      sem_v (hyp_sem);
    }
    if (xml) free(xml);
    if (dom == NULL) {
        logprintfl (EUCAFATAL, "hypervisor failed to start domain\n");
        change_state (instance, SHUTOFF);
        return NULL;
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
      logprintfl (EUCAFATAL, "startup of instance %s was cancelled\n", instance->instanceId);
      change_state (instance, SHUTOFF);
    } else {
      logprintfl (EUCAINFO, "booting VM instance %s\n", instance->instanceId);
      instance->bootTime = time (NULL);
      change_state (instance, BOOTING);
    }
    sem_v (inst_sem);
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

		if ((instance = scRecoverInstanceInfo (dom_name))==NULL) {
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
	long long instances_bytes = 0;
	pthread_t tcb;

	if (initialized>0) /* 0 => hasn't run, -1 => failed, 1 => ok */
		return 0;
	else if (initialized<0)
		return 1;

	bzero (&nc_state, sizeof(struct nc_state_t)); // ensure that MAXes are zeroed out

	/* from now on we have unrecoverable failure, so no point in
	 * retrying to re-init */
	initialized = -1;

	/* read in configuration - this should be first! */
	tmp = getenv(EUCALYPTUS_ENV_VAR_NAME);
	if (!tmp) {
		nc_state.home[0] = '\0';
		do_warn = 1;
	} else 
		strncpy(nc_state.home, tmp, MAX_PATH);

	/* set the minimum log for now */
	snprintf(log, MAX_PATH, "%s/var/log/eucalyptus/nc.log", nc_state.home);
	logfile(log, EUCADEBUG);

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

	nc_state.config_network_port = NC_NET_PORT_DEFAULT;
	strcpy(nc_state.admin_user_id, EUCALYPTUS_ADMIN);

	hyp_sem = sem_alloc (1, "mutex");
	inst_sem = sem_alloc (1, "mutex");
	addkey_sem = sem_alloc (1, "mutex");
	if (!hyp_sem || !inst_sem) {
		logprintfl (EUCAFATAL, "failed to create and initialize a semaphore\n");
		return ERROR_FATAL;
	}

	/* set default in the paths. the driver will override */
	nc_state.config_network_path[0] = '\0';
	nc_state.gen_libvirt_cmd_path[0] = '\0';
	nc_state.xm_cmd_path[0] = '\0';
	nc_state.virsh_cmd_path[0] = '\0';
	nc_state.get_info_cmd_path[0] = '\0';
	snprintf (nc_state.rootwrap_cmd_path, MAX_PATH, EUCALYPTUS_ROOTWRAP, nc_state.home);

	/* prompt the SC to read the configuration too */
	if (scInitConfig()) {
		logprintfl (EUCAFATAL, "ERROR: scInitConfig() failed\n");
		return ERROR_FATAL;
	}

	/* determine the hypervisor to use */
	
	//if (get_conf_var(config, CONFIG_HYPERVISOR, &hypervisor)<1) {
	hypervisor = getConfString(configFiles, 2, CONFIG_HYPERVISOR);
	if (!hypervisor) {
		logprintfl (EUCAFATAL, "value %s is not set in the config file\n", CONFIG_HYPERVISOR);
		return ERROR_FATAL;
	}

	/* let's look for the right hypervisor driver */
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
	
	/* only load virtio config for kvm */
	if (!strncmp("kvm", hypervisor, CHAR_BUFFER_SIZE) ||
		!strncmp("KVM", hypervisor, CHAR_BUFFER_SIZE)) {
		GET_VAR_INT(nc_state.config_use_virtio_net, CONFIG_USE_VIRTIO_NET);
		GET_VAR_INT(nc_state.config_use_virtio_disk, CONFIG_USE_VIRTIO_DISK);
		GET_VAR_INT(nc_state.config_use_virtio_root, CONFIG_USE_VIRTIO_ROOT);
	}

	free (hypervisor);

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
	
	vnetInit(nc_state.vnetconfig, tmp, nc_state.home, nc_state.config_network_path, NC, hypervisor, hypervisor, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, bridge, NULL, NULL);
	if (hypervisor) free(hypervisor);
	if (bridge) free(bridge);
	if (tmp) free(tmp);

	/* cleanup from previous runs and verify integrity of
	 * instances directory */
	sem_p (inst_sem);
	instances_bytes = scFSCK (&global_instances);
	sem_v (inst_sem);
	if (instances_bytes < 0) {
		logprintfl (EUCAFATAL, "instances store failed integrity check (error=%lld)\n", instances_bytes);
		return ERROR_FATAL;
	}
	
	/* get disk max */
	strncpy(log, scGetInstancePath(), MAX_PATH);

	if (statfs(log, &fs) == -1) {
		logprintfl(EUCAWARN, "Failed to stat %s\n", log);
	}  else {
		nc_state.disk_max = (long long)fs.f_bsize * (long long)fs.f_bavail + instances_bytes; /* max for Euca, not total */
		nc_state.disk_max /= BYTES_PER_DISK_UNIT;
		if (nc_state.config_max_disk && nc_state.config_max_disk < nc_state.disk_max)
			nc_state.disk_max = nc_state.config_max_disk;

		logprintfl (EUCAINFO, "Maximum disk available: %lld (under %s)\n", nc_state.disk_max, log);
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

	logprintfl(EUCADEBUG, "doDescribeInstances() invoked\n");

	if (nc_state.H->doDescribeInstances)
		ret = nc_state.H->doDescribeInstances (&nc_state, meta, instIds, instIdsLen, outInsts, outInstsLen);
	else 
		ret = nc_state.D->doDescribeInstances (&nc_state, meta, instIds, instIdsLen, outInsts, outInstsLen);

	if (ret)
		return ret;


	for (i=0; i < (*outInstsLen); i++) {
	  ncInstance *instance = (*outInsts)[i];
	  logprintfl(EUCADEBUG, "doDescribeInstances(): instanceId=%s publicIp=%s privateIp=%s mac=%s vlan=%d networkIndex=%d \n", instance->instanceId, instance->ncnet.publicIp, instance->ncnet.privateIp, instance->ncnet.privateMac, instance->ncnet.vlan, instance->ncnet.networkIndex);
	}

	/* allocate enough memory */
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

int doPowerDown(ncMetadata *meta) {
	int ret;

	if (init())
		return 1;

	logprintfl(EUCADEBUG, "doPowerDown() invoked\n");

	if (nc_state.H->doPowerDown) 
		ret = nc_state.H->doPowerDown(&nc_state, meta);
	else 
		ret = nc_state.D->doPowerDown(&nc_state, meta);

	return ret;
}

int doRunInstance (ncMetadata *meta, char *instanceId, char *reservationId, virtualMachine *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, netConfig *netparams, char *userData, char *launchIndex, char **groupNames, int groupNamesSize, ncInstance **outInst)
{
	int ret;

	if (init())
		return 1;

	logprintfl (EUCAINFO, "doRunInstance() invoked (id=%s cores=%d disk=%d memory=%d)\n", instanceId, params->cores, params->disk, params->mem);
	logprintfl (EUCAINFO, "                         image=%s at %s\n", imageId, imageURL);
	if (kernelId && kernelURL)
	  logprintfl (EUCAINFO, "                         krnel=%s at %s\n", kernelId, kernelURL);
	if (ramdiskId && ramdiskURL)
	  logprintfl (EUCAINFO, "                         rmdsk=%s at %s\n", ramdiskId, ramdiskURL);
	logprintfl (EUCAINFO, "                         vlan=%d priMAC=%s privIp=%s\n", netparams->vlan, netparams->privateMac, netparams->privateIp);

	int i;
	for (i=0; i<EUCA_MAX_DEVMAPS; i++) {
	  deviceMapping * dm = &(params->deviceMapping[i]);
	  if (strlen(dm->deviceName)>0) {
	    logprintfl (EUCAINFO, "                         device mapping: %s=%s size=%d format=%s\n", dm->deviceName, dm->virtualName, dm->size, dm->format);
	  }
	}

	if (nc_state.H->doRunInstance)
 	  ret = nc_state.H->doRunInstance (&nc_state, meta, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, netparams, userData, launchIndex, groupNames, groupNamesSize, outInst);
	else
	  ret = nc_state.D->doRunInstance (&nc_state, meta, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, netparams, userData, launchIndex, groupNames, groupNamesSize, outInst);

	return ret;
}

int doTerminateInstance (ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState)
{
	int ret; 

	if (init())
		return 1;

	logprintfl (EUCAINFO, "doTerminateInstance() invoked (id=%s)\n", instanceId);

	if (nc_state.H->doTerminateInstance) 
		ret = nc_state.H->doTerminateInstance(&nc_state, meta, instanceId, shutdownState, previousState);
	else 
		ret = nc_state.D->doTerminateInstance(&nc_state, meta, instanceId, shutdownState, previousState);

	return ret;
}

int doRebootInstance (ncMetadata *meta, char *instanceId) 
{
	int ret;

	if (init())
		return 1;
		
	logprintfl(EUCAINFO, "doRebootInstance() invoked  (id=%s)\n", instanceId);

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

	logprintfl (EUCAINFO, "doGetConsoleOutput() invoked (id=%s)\n", instanceId);

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

	logprintfl(EUCADEBUG, "doDescribeResource() invoked\n");

	if (nc_state.H->doDescribeResource)
		ret = nc_state.H->doDescribeResource (&nc_state, meta, resourceType, outRes);
	else 
		ret = nc_state.D->doDescribeResource (&nc_state, meta, resourceType, outRes);

	return ret;
}

int
doStartNetwork (	ncMetadata *ccMeta,
			char **remoteHosts,
			int remoteHostsLen,
			int port,
			int vlan)
{
	int ret;

	if (init())
		return 1;

	logprintfl(EUCADEBUG, "doStartNetwork() invoked\n");

	if (nc_state.H->doStartNetwork) 
		ret = nc_state.H->doStartNetwork (&nc_state, ccMeta, remoteHosts, remoteHostsLen, port, vlan);
	else 
		ret = nc_state.D->doStartNetwork (&nc_state, ccMeta, remoteHosts, remoteHostsLen, port, vlan);
	
	return ret;
}

int doAttachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
	int ret;

	if (init())
		return 1;

	logprintfl (EUCAINFO, "doAttachVolume() invoked (id=%s vol=%s remote=%s local=%s)\n", instanceId, volumeId, remoteDev, localDev);

	if (nc_state.H->doAttachVolume)
		ret = nc_state.H->doAttachVolume(&nc_state, meta, instanceId, volumeId, remoteDev, localDev);
	else
		ret = nc_state.D->doAttachVolume(&nc_state, meta, instanceId, volumeId, remoteDev, localDev);
	
	return ret;
}

int doDetachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
	int ret;

	if (init())
		return 1;

	logprintfl (EUCAINFO, "doDetachVolume() invoked (id=%s vol=%s remote=%s local=%s force=%d)\n", instanceId, volumeId, remoteDev, localDev, force);

	if (nc_state.H->doDetachVolume)
		ret = nc_state.H->doDetachVolume (&nc_state, meta, instanceId, volumeId, remoteDev, localDev, force);
	else 
		ret = nc_state.D->doDetachVolume (&nc_state, meta, instanceId, volumeId, remoteDev, localDev, force);

	return ret;
}

int check_iscsi(char* dev_string) {
    if(strchr(dev_string, ',') == NULL)
	return 0;
    return 1;
}

void parse_target(char *dev_string) {
    char *delimiter = ",";
    char *brk, *part;
    char dev_name[256];
    snprintf(dev_name, 256, "%s", dev_string);

    for (part = strtok_r(dev_name, delimiter, &brk); part != NULL; part = strtok_r(NULL, delimiter, &brk)) {
    }  
}

char* connect_iscsi_target(const char *storage_cmd_path, char *dev_string) {
    char buf [MAX_PATH];
    char *retval;
    
    snprintf (buf, MAX_PATH, "%s %s", storage_cmd_path, dev_string);
    logprintfl (EUCAINFO, "connect_iscsi_target invoked (dev_string=%s)\n", dev_string);
    if ((retval = system_output(buf)) == NULL) {
	logprintfl (EUCAERROR, "ERROR: connect_iscsi_target failed\n");
    } else {
	logprintfl (EUCAINFO, "Attached device: %s\n", retval);
    } 
    return retval;
}

int disconnect_iscsi_target(const char *storage_cmd_path, char *dev_string) {
    logprintfl (EUCAINFO, "disconnect_iscsi_target invoked (dev_string=%s)\n", dev_string);
    if (vrun("%s %s", storage_cmd_path, dev_string) != 0) {
	logprintfl (EUCAERROR, "ERROR: disconnect_iscsi_target failed\n");
	return -1;
    }
    return 0;
}

char* get_iscsi_target(const char *storage_cmd_path, char *dev_string) {
    char buf [MAX_PATH];
    char *retval;
    
    snprintf (buf, MAX_PATH, "%s %s", storage_cmd_path, dev_string);
    logprintfl (EUCAINFO, "get_iscsi_target invoked (dev_string=%s)\n", dev_string);
    if ((retval = system_output(buf)) == NULL) {
	logprintfl (EUCAERROR, "ERROR: get_iscsi_target failed\n");
    } else {
	logprintfl (EUCAINFO, "Device: %s\n", retval);
    } 
    return retval;
}


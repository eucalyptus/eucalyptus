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
    his list of conditions and the following disclaimer.

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
#define __USE_GNU /* strnlen */
#include <string.h> /* strlen, strcpy */
#include <time.h>
#include <sys/types.h> /* fork */
#include <sys/wait.h> /* waitpid */
#include <unistd.h>
#include <assert.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h> /* SIGINT */
#include <sys/stat.h>
#include <fcntl.h>

#include "ipc.h"
#include "misc.h"
#include "handlers.h"
#include "eucalyptus.h"
#include "euca_auth.h"
#include "backing.h"
#include "xml.h"
#include "diskutil.h"
#include "iscsi.h"

/* coming from handlers.c */
extern sem * hyp_sem;
extern sem * inst_sem;
extern bunchOfInstances * global_instances;
extern struct nc_state_t nc_state;

#define HYPERVISOR_URI "qemu:///system"

static int doInitialize (struct nc_state_t *nc) 
{
	char *s = NULL;
        
	// set up paths of Eucalyptus commands NC relies on
	snprintf (nc->get_info_cmd_path, MAX_PATH, EUCALYPTUS_GET_KVM_INFO,  nc->home, nc->home);
	strcpy(nc->uri, HYPERVISOR_URI);
	nc->convert_to_disk = 1;
    nc->capability = HYPERVISOR_HARDWARE; // TODO: indicate virtio support?

	s = system_output (nc->get_info_cmd_path);
#define GET_VALUE(name,var) \
	if (get_value (s, name, &var)) { \
		logprintfl (EUCAFATAL, "error: did not find %s in output from %s\n", name, nc->get_info_cmd_path); \
		if (s) free (s); \
		return ERROR_FATAL; \
	}

	GET_VALUE("nr_cores", nc->cores_max);
	GET_VALUE("total_memory", nc->mem_max);
	if (s) free(s);

	// we leave 256M to the host
	nc->mem_max -= 256;

	return OK;
}

/* thread that does the actual reboot */
static void * rebooting_thread (void *arg) 
{
    virConnectPtr *conn;
    ncInstance * instance = (ncInstance *)arg;
    struct stat statbuf;
    int rc = 0;

    char * xml = file2str (instance->libvirtFilePath);
    if (xml == NULL) {
        logprintfl (EUCAERROR, "cannot obtain XML file %s\n", instance->libvirtFilePath);
        return NULL;
    }

    conn = check_hypervisor_conn();
    if (! conn) {
        logprintfl (EUCAERROR, "cannot restart instance %s, abandoning it\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        free (xml);
        return NULL;
    }

    sem_p (hyp_sem);
    virDomainPtr dom = virDomainLookupByName(*conn, instance->instanceId);
    sem_v (hyp_sem);
    if (dom == NULL) {
        free (xml);
        return NULL;
    }

    sem_p (hyp_sem);
    // for KVM, must stop and restart the instance
    int error = virDomainDestroy (dom); // TODO: change to Shutdown?  TODO: is this synchronous?
    virDomainFree(dom);
    sem_v (hyp_sem);

    if (error) {
        free (xml);
        return NULL;
    }
    
    // domain is now shut down, create a new one with the same XML
    sem_p (hyp_sem); 
    dom = virDomainCreateLinux (*conn, xml, 0);
    sem_v (hyp_sem);
    free (xml);

    char *remoteDevStr=NULL;
    // re-attach each volume previously attached
    for (int i=0; i < EUCA_MAX_VOLUMES; ++i) {
        ncVolume * volume = &instance->volumes[i];
        if (strcmp (volume->stateName, VOL_STATE_ATTACHED) &&
            strcmp (volume->stateName, VOL_STATE_ATTACHING))
            continue; // skip the entry unless attached or attaching
        
        char attach_xml[1024];
        int rc;
        // get credentials, decrypt them
        remoteDevStr = get_iscsi_target (volume->remoteDev);
        if (!remoteDevStr || !strstr(remoteDevStr, "/dev")) {
            logprintfl(EUCAERROR, "Reattach-volume: failed to get local name of host iscsi device\n");
            rc = 1;
        } else {
            rc = gen_libvirt_attach_xml (volume->volumeId,
                                         instance, 
                                         volume->localDevReal, 
                                         remoteDevStr, 
                                         attach_xml, 
                                         sizeof(attach_xml));
        }

        if (remoteDevStr)
            free (remoteDevStr);

        if (!rc) {
            int err;
            sem_p (hyp_sem);
            err = virDomainAttachDevice (dom, attach_xml);
            sem_v (hyp_sem);      
            if (err) {
                logprintfl (EUCAERROR, "virDomainAttachDevice() failed (err=%d) XML=%s\n", err, attach_xml);
            } else {
                logprintfl (EUCAINFO, "reattached '%s' to '%s' in domain %s\n", volume->remoteDev, volume->localDevReal, instance->instanceId);
            }
        }
    }
    if (dom==NULL) {
        logprintfl (EUCAERROR, "Failed to restart instance %s\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    
    sem_p (hyp_sem);
    virDomainFree(dom);
    sem_v (hyp_sem);
    return NULL;
}

static int
doRebootInstance(	struct nc_state_t *nc,
			ncMetadata *meta,
			char *instanceId) 
{
    sem_p (inst_sem); 
    ncInstance *instance = find_instance (&global_instances, instanceId);
    sem_v (inst_sem);
    if ( instance == NULL ) {
        logprintfl (EUCAERROR, "cannot find instance %s\n", instanceId);
        return ERROR;
    }
    
    pthread_t tcb;
    // since shutdown/restart may take a while, we do them in a thread
    if ( pthread_create (&tcb, NULL, rebooting_thread, (void *)instance) ) {
        logprintfl (EUCAERROR, "failed to spawn a reboot thread\n");
        return ERROR_FATAL;
    }
    if (pthread_detach(tcb)) {
      logprintfl (EUCAERROR, "failed to detach the rebooting thread\n");
      return ERROR_FATAL;
    }
    
    return OK;
}

static int
doGetConsoleOutput(	struct nc_state_t *nc,
			ncMetadata *meta,
			char *instanceId,
			char **consoleOutput) {

  char *console_output=NULL, *console_append=NULL, *console_main=NULL;
  char console_file[MAX_PATH], userId[48];
  int rc, fd, ret, readsize;
  struct stat statbuf;
  ncInstance *instance=NULL;

  *consoleOutput = NULL;
  readsize = 64 * 1024;

  // find the instance record
  sem_p (inst_sem); 
  instance = find_instance(&global_instances, instanceId);
  if (instance) {
          snprintf(console_file, 1024, "%s/console.append.log", instance->instancePath);
          snprintf(userId, 48, "%s", instance->userId);
  }
  sem_v (inst_sem);

  if (!instance) {
    logprintfl(EUCAERROR, "doGetConsoleOutput(): cannot locate instance with instanceId=%s\n", instanceId);
    return(1);
  }

  // read from console.append.log if it exists into dynamically allocated 4K console_append buffer
  rc = stat(console_file, &statbuf);
  if (rc >= 0) {
      if (diskutil_ch (console_file, nc->admin_user_id, nc->admin_user_id, 0) != OK) {
          logprintfl (EUCAERROR, "doGetConsoleOutput(): failed to change ownership of %s\n", console_file);
          return (1);
      }
    fd = open(console_file, O_RDONLY);
    if (fd >= 0) {
      console_append = malloc(4096);
      if (console_append) {
	bzero(console_append, 4096);
	rc = read(fd, console_append, (4096)-1);
	close(fd);          
      }
    }
  }
  
  sem_p (inst_sem); 
  snprintf(console_file, MAX_PATH, "%s/console.log", instance->instancePath);
  sem_v (inst_sem);

  // read the last 64K from console.log or the whole file, if smaller, into dynamically allocated 64K console_main buffer
  rc = stat(console_file, &statbuf);
  if (rc >= 0) {
      if (diskutil_ch (console_file, nc->admin_user_id, nc->admin_user_id, 0) != OK) {
          logprintfl (EUCAERROR, "doGetConsoleOutput(): failed to change ownership of %s\n", console_file);
          if (console_append) 
              free(console_append);
          return (1);
      }
    fd = open(console_file, O_RDONLY);
    if (fd >= 0) {
      rc = lseek(fd, (off_t)(-1 * readsize), SEEK_END);
      if (rc < 0) {
	rc = lseek(fd, (off_t)0, SEEK_SET);
	if (rc < 0) {
	  logprintfl(EUCAERROR, "cannot seek to beginning of file\n");
	  if (console_append) free(console_append);
	  return(1);
	}
      }
      console_main = malloc(readsize);
      if (console_main) {
	bzero(console_main, readsize);
	rc = read(fd, console_main, (readsize)-1);
	close(fd);
      }
    } else {
      logprintfl(EUCAERROR, "cannot open '%s' read-only\n", console_file);
    }
  } else {
    logprintfl(EUCAERROR, "cannot stat console_output file '%s'\n", console_file);
  }
  
  // concatenate console_append with console_main, base64-encode this, and put into dynamically allocated buffer consoleOutput
  ret = 1;
  console_output = malloc( (readsize) + 4096 );
  if (console_output) {
    bzero(console_output, (readsize) + 4096 );
    if (console_append) {
      strncat(console_output, console_append, 4096);
    }
    if (console_main) {
      strncat(console_output, console_main, readsize);
    }
    *consoleOutput = base64_enc((unsigned char *)console_output, strlen(console_output));
    ret = 0;
  }

  if (console_append) free(console_append);
  if (console_main) free(console_main);
  if (console_output) free(console_output);

  return(ret);
}

struct handlers kvm_libvirt_handlers = {
    .name = "kvm",
    .doInitialize        = doInitialize,
    .doDescribeInstances = NULL,
    .doRunInstance       = NULL,
    .doTerminateInstance = NULL,
    .doRebootInstance    = doRebootInstance,
    .doGetConsoleOutput  = doGetConsoleOutput,
    .doDescribeResource  = NULL,
    .doStartNetwork      = NULL,
    .doAssignAddress     = NULL,
    .doPowerDown         = NULL,
    .doAttachVolume      = NULL,
    .doDetachVolume      = NULL
};


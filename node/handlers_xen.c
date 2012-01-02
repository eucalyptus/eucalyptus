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

/* coming from handlers.c */
extern sem * hyp_sem;
extern sem * inst_sem;
extern bunchOfInstances * global_instances;

#define HYPERVISOR_URI "xen:///"

static int doInitialize (struct nc_state_t *nc) 
{
	char *s = NULL;
	virNodeInfo ni;
	long long dom0_min_mem;

	// set up paths of Eucalyptus commands NC relies on
	snprintf (nc->get_info_cmd_path, MAX_PATH, EUCALYPTUS_GET_XEN_INFO, nc->home, nc->home);
	snprintf (nc->virsh_cmd_path, MAX_PATH, EUCALYPTUS_VIRSH, nc->home);
	snprintf (nc->xm_cmd_path, MAX_PATH, EUCALYPTUS_XM);
	snprintf (nc->detach_cmd_path, MAX_PATH, EUCALYPTUS_DETACH, nc->home, nc->home);
	strcpy(nc->uri, HYPERVISOR_URI);
	nc->convert_to_disk = 0;
    nc->capability = HYPERVISOR_XEN_AND_HARDWARE; // TODO: set to XEN_PARAVIRTUALIZED if on older Xen kernel
    
    // check connection is fresh
    if (!check_hypervisor_conn()) {
        return ERROR_FATAL;
	}
    
	// get resources
	if (virNodeGetInfo(nc->conn, &ni)) {
		logprintfl (EUCAFATAL, "error: failed to discover resources\n");
		return ERROR_FATAL;
	}

	// dom0-min-mem has to come from xend config file
	s = system_output (nc->get_info_cmd_path);
	if (get_value (s, "dom0-min-mem", &dom0_min_mem)) {
		logprintfl (EUCAFATAL, "error: did not find dom0-min-mem in output from %s\n", nc->get_info_cmd_path);
		free (s);
		return ERROR_FATAL;
	}
	free (s);

	// calculate the available memory
	nc->mem_max = ni.memory/1024 - 32 - dom0_min_mem;

	// calculate the available cores
	nc->cores_max = ni.cpus;

	// let's adjust the values based on the config values
	if (nc->config_max_mem && nc->config_max_mem < nc->mem_max)
		nc->mem_max = nc->config_max_mem;
	if (nc->config_max_cores)
		nc->cores_max = nc->config_max_cores;

	return OK;
}

static int doRebootInstance(	struct nc_state_t *nc,
				ncMetadata *meta,
				char *instanceId) 
{
    ncInstance *instance;
    virConnectPtr *conn;

    sem_p (inst_sem); 
    instance = find_instance(&global_instances, instanceId);
    sem_v (inst_sem);
    if ( instance == NULL ) return NOT_FOUND;
    
    /* reboot the Xen domain */
    conn = check_hypervisor_conn();
    if (conn) {
        sem_p(hyp_sem);
        virDomainPtr dom = virDomainLookupByName(*conn, instanceId);
	sem_v(hyp_sem);
        if (dom) {
            /* also protect 'reboot', just in case */
            sem_p (hyp_sem);
            int err=virDomainReboot (dom, 0);
            sem_v (hyp_sem);
            if (err==0) {
                logprintfl (EUCAINFO, "rebooting Xen domain for instance %s\n", instanceId);
            }
	    sem_p(hyp_sem);
            virDomainFree(dom); /* necessary? */
	    sem_v(hyp_sem);
        } else {
            if (instance->state != BOOTING && instance->state != STAGING) {
                logprintfl (EUCAWARN, "warning: domain %s to be rebooted not running on hypervisor\n", instanceId);
            }
        }
    }

    return 0;
}

static int
doGetConsoleOutput(	struct nc_state_t *nc,
			ncMetadata *meta,
			char *instanceId,
			char **consoleOutput) {

  char *console_output=NULL, *console_append=NULL, *console_main=NULL, *tmp=NULL;
  char console_file[MAX_PATH], dest_file[MAX_PATH], cmd[MAX_PATH];
  char userId[48];
  int rc, fd, ret;
  struct stat statbuf;
  ncInstance *instance=NULL;

  int bufsize, pid, status;

  *consoleOutput = NULL;

  // find the instance record
  sem_p (inst_sem); 
  instance = find_instance(&global_instances, instanceId);
  if (instance) {
    snprintf(userId, 48, "%s", instance->userId);
  	snprintf(console_file, 1024, "%s/console.append.log", instance->instancePath);
  }
  sem_v (inst_sem);
  if (!instance) {
    logprintfl(EUCAERROR, "doGetConsoleOutput(): cannot locate instance with instanceId=%s\n", instanceId);
    return(1);
  }
  rc = stat(console_file, &statbuf);
  if (rc >= 0) {
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


  bufsize = sizeof(char) * 1024 * 64;
  console_main = malloc(bufsize);
  if (!console_main) {
    logprintfl(EUCAERROR, "doGetConsoleOutput(): out of memory!\n");
    if (console_append) free(console_append);
    return(1);
  }
  bzero(console_main, bufsize);

  if (getuid() != 0) {
    snprintf(console_file, MAX_PATH, "/var/log/xen/console/guest-%s.log", instanceId);
    snprintf(dest_file, MAX_PATH, "%s/console.log", instance->instancePath);
    snprintf(cmd, MAX_PATH, "%s cp %s %s", nc->rootwrap_cmd_path, console_file, dest_file);
    rc = system(cmd);
    if (!rc) {
      // was able to copy xen guest console file, read it
      snprintf(cmd, MAX_PATH, "%s chown %s:%s %s", nc->rootwrap_cmd_path, nc->admin_user_id, nc->admin_user_id, dest_file);
      rc = system(cmd);
      if (!rc) {
	tmp = file2str_seek(dest_file, bufsize, 1);
	if (tmp) {
	  snprintf(console_main, bufsize, "%s", tmp);
	  free(tmp);
	} else {
	  snprintf(console_main, bufsize, "NOT SUPPORTED");
	}
      } else {
	snprintf(console_main, bufsize, "NOT SUPPORTED");
      }
    } else {
      snprintf(console_main, bufsize, "NOT SUPPORTED");
    }
  } else {

    snprintf(console_file, MAX_PATH, "/tmp/consoleOutput.%s", instanceId);
    
    pid = fork();
    if (pid == 0) {
      int fd;
      fd = open(console_file, O_WRONLY | O_TRUNC | O_CREAT, 0644);
      if (fd < 0) {
	// error
      } else {
	dup2(fd, 2);
	dup2(2, 1);
	close(0);
	// TODO: test virsh console:
	// rc = execl(rootwrap_command_path, rootwrap_command_path, "virsh", "console", instanceId, NULL);
	rc = execl("/usr/sbin/xm", "/usr/sbin/xm", "console", instanceId, NULL);
	fprintf(stderr, "execl() failed\n");
	close(fd);
      }
      exit(0);
    } else {
      int count;
      fd_set rfds;
      struct timeval tv;
      struct stat statbuf;
      
      count=0;
      while(count < 10000 && stat(console_file, &statbuf) < 0) {count++;}
      fd = open(console_file, O_RDONLY);
      if (fd < 0) {
	logprintfl (EUCAERROR, "ERROR: could not open consoleOutput file %s for reading\n", console_file);
      } else {
	FD_ZERO(&rfds);
	FD_SET(fd, &rfds);
	tv.tv_sec = 0;
	tv.tv_usec = 500000;
	rc = select(1, &rfds, NULL, NULL, &tv);
	bzero(console_main, bufsize);
	
	count = 0;
	rc = 1;
	while(rc && count < 1000) {
	  rc = read(fd, console_main, bufsize-1);
	  count++;
	}
	close(fd);
      }
      kill(pid, 9);
      wait(&status);
    }
    
    unlink(console_file);
  }
  
  ret = 1;
  console_output = malloc( (64*1024) + 4096 );
  if (console_output) {
    bzero(console_output, (64*1024) + 4096 );
    if (console_append) {
      strncat(console_output, console_append, 4096);
    }
    if (console_main) {
      strncat(console_output, console_main, 1024*64);
    }
    *consoleOutput = base64_enc((unsigned char *)console_output, strlen(console_output));
    ret = 0;
  }
  
  if (console_append) free(console_append);
  if (console_main) free(console_main);
  if (console_output) free(console_output);
  
  return(ret);
}

struct handlers xen_libvirt_handlers = {
    .name = "xen",
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


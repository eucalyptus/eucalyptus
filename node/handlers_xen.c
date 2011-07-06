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
#include "iscsi.h"

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

	logprintfl(EUCADEBUG, "doInitialized() invoked\n");

	/* set up paths of Eucalyptus commands NC relies on */
	snprintf (nc->gen_libvirt_cmd_path, MAX_PATH, EUCALYPTUS_GEN_LIBVIRT_XML, nc->home, nc->home);
	snprintf (nc->get_info_cmd_path, MAX_PATH, EUCALYPTUS_GET_XEN_INFO, nc->home, nc->home);
	snprintf (nc->virsh_cmd_path, MAX_PATH, EUCALYPTUS_VIRSH, nc->home);
	snprintf (nc->xm_cmd_path, MAX_PATH, EUCALYPTUS_XM);
	snprintf (nc->detach_cmd_path, MAX_PATH, EUCALYPTUS_DETACH, nc->home, nc->home);
	strcpy(nc->uri, HYPERVISOR_URI);
	nc->convert_to_disk = 0;
        nc->capability = HYPERVISOR_XEN_AND_HARDWARE; // TODO: set to XEN_PARAVIRTUALIZED if on older Xen kernel

        /* check connection is fresh */
        if (!check_hypervisor_conn()) {
          return ERROR_FATAL;
	}

	/* get resources */
	if (virNodeGetInfo(nc->conn, &ni)) {
		logprintfl (EUCAFATAL, "error: failed to discover resources\n");
		return ERROR_FATAL;
	}

	/* dom0-min-mem has to come from xend config file */
	s = system_output (nc->get_info_cmd_path);
	if (get_value (s, "dom0-min-mem", &dom0_min_mem)) {
		logprintfl (EUCAFATAL, "error: did not find dom0-min-mem in output from %s\n", nc->get_info_cmd_path);
		free (s);
		return ERROR_FATAL;
	}
	free (s);

	/* calculate the available memory */
	nc->mem_max = ni.memory/1024 - 32 - dom0_min_mem;

	/* calculate the available cores */
	nc->cores_max = ni.cpus;

	/* let's adjust the values based on the config values */
	if (nc->config_max_mem && nc->config_max_mem < nc->mem_max)
		nc->mem_max = nc->config_max_mem;
	if (nc->config_max_cores)
		nc->cores_max = nc->config_max_cores;

	logprintfl(EUCAINFO, "Using %lld cores\n", nc->cores_max);
	logprintfl(EUCAINFO, "Using %lld memory\n", nc->mem_max);

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

static int
doAttachVolume (	struct nc_state_t *nc,
			ncMetadata *meta,
			char *instanceId,
			char *volumeId,
			char *remoteDev,
			char *localDev)
{
    int ret = OK, rc;
    ncInstance *instance;
    virConnectPtr *conn;
    char localDevReal[32], remoteDevReal[32];
    struct stat statbuf;
    int is_iscsi_target = 0, have_remote_device = 0;

    // fix up format of incoming local dev name, if we need to
    ret = convert_dev_names (localDev, localDevReal, NULL);
    if (ret)
        return ret;

    // find the instance record
    sem_p (inst_sem); 
    instance = find_instance(&global_instances, instanceId);
    sem_v (inst_sem);
    if ( instance == NULL ) 
        return NOT_FOUND;

    /* try attaching to the KVM domain */
    conn = check_hypervisor_conn();
    if (conn) {
        virDomainPtr dom = virDomainLookupByName(*conn, instanceId);
        if (dom) {
            int err = 0;
            char xml [1024];
            int virtio_dev = 0;

            /* only attach using virtio when the device is /dev/vdXX */
	    /* check to make sure that the remoteDev is ready for hypervisor attach */
            have_remote_device = 0;
	    /* do iscsi connect shellout if removeDev is an iSCSI target */
            if(check_iscsi(remoteDev)) {
	      char *remoteDevStr=NULL;
	      is_iscsi_target = 1;
	      /*get credentials, decrypt them*/
	      /*login to target*/
	      remoteDevStr = connect_iscsi_target(remoteDev);
	      if (!remoteDevStr || !strstr(remoteDevStr, "/dev")) {
		logprintfl(EUCAERROR, "AttachVolume(): failed to connect to iscsi target\n");
		remoteDevReal[0] = '\0';
	      } else { 
		logprintfl(EUCADEBUG, "AttachVolume(): success in iSCSI attach of host device '%s'\n", remoteDevStr);
		snprintf(remoteDevReal, 32, "%s", remoteDevStr);
		have_remote_device = 1;
	      }
	      if (remoteDevStr) free(remoteDevStr);
	    } else {
	      snprintf(remoteDevReal, 32, "%s", remoteDev);
	      have_remote_device = 1;
	    }
	    
	    if (have_remote_device) {
	      if (check_block(remoteDevReal)) {
		logprintfl(EUCAERROR, "AttachVolume(): cannot verify that host device '%s' is available for hypervisor attach\n", remoteDevReal);
		ret = ERROR;
	      } else {
		rc = generate_attach_xml(localDevReal, remoteDevReal, nc, instance, xml);
		if (!rc) {
		  /* protect KVM calls, just in case */
		  sem_p (hyp_sem);
		  err = virDomainAttachDevice (dom, xml);
		  sem_v (hyp_sem);
		  if (err) {
		    logprintfl (EUCAERROR, "AttachVolume(): virDomainAttachDevice() failed (err=%d) XML=%s\n", err, xml);
		    logprintfl (EUCAERROR, "AttachVolume(): failed to attach host device '%s' to guest device '%s' within instance '%s'\n", remoteDevReal, localDevReal, instanceId);
		    ret = ERROR;
		  } else {
		    ncVolume * volume;
		    logprintfl (EUCAINFO, "AttachVolume(): success in attach of host device '%s' to guest device '%s' within instance '%s'\n", remoteDevReal, localDevReal, instanceId);
		    sem_p (inst_sem);
		    volume = add_volume (instance, volumeId, remoteDevReal, localDevReal, localDevReal, "attached");
		    save_instance_struct (instance);
		    sem_v (inst_sem);
		    if ( volume == NULL ) {
		      logprintfl (EUCAERROR, "AttachVolume(): Failed to save the volume record, aborting volume attachment (detaching)\n");
		      sem_p (hyp_sem);
		      err = virDomainDetachDevice (dom, xml);
		      sem_v (hyp_sem);
		      if (err) {
			logprintfl (EUCAERROR, "AttachVolume(): virDomainDetachDevice() failed (err=%d) XML=%s\n", err, xml);
			logprintfl (EUCAERROR, "AttachVolume(): failed to detach host device '%s' to guest device '%s' within instance '%s'\n", remoteDevReal, localDevReal, instanceId);
		      } else {
			logprintfl(EUCADEBUG, "AttachVolume(): success in detach of host device '%s' to guest device '%s' within instance '%s'\n", remoteDevReal, localDevReal, instanceId);
		      }
		      ret = ERROR;
		    } else {
		      // this means that everything worked
		      logprintfl(EUCAINFO, "AttachVolume(): successfully attached volume '%s' to instance '%s'\n", volumeId, instanceId);
		    }
		  }
		} else {
		  logprintfl(EUCAERROR, "AttachVolume(): could not produce attach device xml\n");
		  ret = ERROR;
		}
		virDomainFree(dom);
	      }
	    }
	} else {
	  if (instance->state != BOOTING && instance->state != STAGING) {
	    logprintfl (EUCAWARN, "AttachVolume(): domain %s not running on hypervisor, cannot attach device\n", instanceId);
	  }
	  ret = ERROR;
	}
    } else {
      logprintfl(EUCAERROR, "AttachVolume(): cannot get connection to hypervisor\n");
      ret = ERROR;
    }

    if (ret != OK) {
      // should try to disconnect (if iSCSI) the volume, here
      if(is_iscsi_target && have_remote_device) {
	logprintfl(EUCADEBUG, "AttachVolume(): attempting to disconnect iscsi target due to attachment failure\n");
	if(disconnect_iscsi_target(remoteDev) != 0) {
	  logprintfl (EUCAERROR, "AttachVolume(): disconnect_iscsi_target failed for %s\n", remoteDev);
	}
      }
    }

    return ret;
}

static int xenDetachHelper(struct nc_state_t *nc, char *instanceId, char *localDevReal, char *xml) {
  int err, fd, rc, status;
  pid_t pid;
  char tmpfile[MAX_PATH];
  char cmd[MAX_PATH];

  pid = fork();
  if (!pid) {
    snprintf(tmpfile, 32, "/tmp/detachxml.XXXXXX");
    fd = safe_mkstemp(tmpfile);
    if (fd > 0) {
      write(fd, xml, strlen(xml));
      close(fd);
      snprintf(cmd, MAX_PATH, "%s %s `which virsh` %s %s %s", nc->detach_cmd_path, nc->rootwrap_cmd_path, instanceId, localDevReal, tmpfile);
      rc = system(cmd);
      rc = rc>>8;
      unlink(tmpfile);
    } else {
      logprintfl(EUCAERROR, "xenDetachHelper(): could not write to tmpfile for detach XML: %s\n", tmpfile);
      rc = 1;
    } 
    exit(rc);
  } else {
    rc = timewait(pid, &status, 15);
    if (WEXITSTATUS(status)) {
      logprintfl(EUCAERROR, "xenDetachHelper(): failed to sucessfully run detach helper\n");
      err = 1;
    } else {
      err = 0;
    }
  }
  return(err);
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
    int ret = OK, rc;
    ncInstance *instance;
    virConnectPtr *conn;
    char localDevReal[32], remoteDevReal[32];
    struct stat statbuf;
    int is_iscsi_target = 0, have_remote_device = 0;
    ncVolume * volume;

    // fix up format of incoming local dev name, if we need to
    ret = convert_dev_names (localDev, localDevReal, NULL);
    if (ret)
        return ret;

    // find the instance record
    if (grab_inst_sem) {
      sem_p (inst_sem); 
    }
    instance = find_instance(&global_instances, instanceId);
    if (grab_inst_sem) {
      sem_v (inst_sem);
    }
    if ( instance == NULL ) 
        return NOT_FOUND;

    /* try attaching to the KVM domain */
    conn = check_hypervisor_conn();
    if (conn) {
        virDomainPtr dom = virDomainLookupByName(*conn, instanceId);
        if (dom) {
            int err = 0;
            char xml [1024];
            int virtio_dev = 0;

            /* only attach using virtio when the device is /dev/vdXX */
	    /* check to make sure that the remoteDev is ready for hypervisor attach */
            have_remote_device = 0;
	    /* do iscsi connect shellout if removeDev is an iSCSI target */
            if(check_iscsi(remoteDev)) {
	      char *remoteDevStr=NULL;
	      is_iscsi_target = 1;
	      /*get credentials, decrypt them*/
	      remoteDevStr = get_iscsi_target(remoteDev);
	      if (!remoteDevStr || !strstr(remoteDevStr, "/dev")) {
		logprintfl(EUCAERROR, "DetachVolume(): failed to get local name of host iscsi device\n");
		remoteDevReal[0] = '\0';
	      } else { 
		logprintfl(EUCADEBUG, "DetachVolume(): success in getting local name of host device '%s'\n", remoteDevStr);
		snprintf(remoteDevReal, 32, "%s", remoteDevStr);
		have_remote_device = 1;
	      }
	      if (remoteDevStr) free(remoteDevStr);
	    } else {
	      snprintf(remoteDevReal, 32, "%s", remoteDev);
	      have_remote_device = 1;
	    }
	    
	    if (have_remote_device) {
	      if (check_block(remoteDevReal)) {
		logprintfl(EUCAERROR, "DetachVolume(): cannot verify that host device '%s' is available for hypervisor detach\n", remoteDevReal);
		if (!force) ret = ERROR;
	      } else {
		rc = generate_attach_xml(localDevReal, remoteDevReal, nc, instance, xml);
		if (!rc) {
		  /* protect KVM calls, just in case */
		  sem_p (hyp_sem);
		  err = virDomainDetachDevice (dom, xml);
		  err = xenDetachHelper (nc, instanceId, localDevReal, xml);
		  sem_v (hyp_sem);
		  
		  if (err) {
		    logprintfl (EUCAERROR, "DetachVolume(): virDomainDetachDevice() failed (err=%d) XML=%s\n", err, xml);
		    logprintfl (EUCAERROR, "DetachVolume(): failed to detach host device '%s' from guest device '%s' within instance '%s'\n", remoteDevReal, localDevReal, instanceId);
		    if (!force) ret = ERROR;
		  } else {
		    logprintfl (EUCAINFO, "DetachVolume(): success in detach of host device '%s' from guest device '%s' within instance '%s'\n", remoteDevReal, localDevReal, instanceId);
		  }
		} else {
		  logprintfl(EUCAERROR, "DetachVolume(): could not produce detach device xml\n");
		  ret = ERROR;
		}
	      }
	    }
	    virDomainFree(dom);
	} else {
	  if (instance->state != BOOTING && instance->state != STAGING) {
	    logprintfl (EUCAWARN, "DetachVolume(): domain %s not running on hypervisor, cannot detach device\n", instanceId);
	  }
	  ret = ERROR;
	}
    } else {
      logprintfl(EUCAERROR, "DetachVolume(): cannot get connection to hypervisor\n");
      ret = ERROR;
    }

    // should try to disconnect (if iSCSI) the volume, here
    if(is_iscsi_target && have_remote_device) {
      logprintfl(EUCADEBUG, "DetachVolume(): attempting to disconnect iscsi target\n");
      if(disconnect_iscsi_target(remoteDev) != 0) {
	logprintfl (EUCAERROR, "DetachVolume(): disconnect_iscsi_target failed for %s\n", remoteDev);
	if (!force) ret = ERROR;
      }
    }

    if (grab_inst_sem) {
      sem_p (inst_sem);
    }
    volume = free_volume (instance, volumeId, remoteDevReal, localDevReal);
    save_instance_struct (instance);
    if (grab_inst_sem) {
      sem_v (inst_sem);
    }
    if ( volume == NULL ) {
      logprintfl (EUCAWARN, "DetachVolume(): Failed to free the volume record\n");
    }

    if (ret == OK) {
      logprintfl(EUCAINFO, "DetachVolume(): successfully detached volume '%s' from instance '%s'\n", volumeId, instanceId);
    }
    
    return ret;
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
    .doAttachVolume      = doAttachVolume,
    .doDetachVolume      = doDetachVolume
};


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
#include <handlers.h>
#include <storage.h>
#include <eucalyptus.h>
#include <euca_auth.h>

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
    snprintf (nc->connect_storage_cmd_path, MAX_PATH, EUCALYPTUS_CONNECT_ISCSI, nc->home, nc->home);
    snprintf (nc->disconnect_storage_cmd_path, MAX_PATH, EUCALYPTUS_DISCONNECT_ISCSI, nc->home, nc->home);
    snprintf (nc->get_storage_cmd_path, MAX_PATH, EUCALYPTUS_GET_ISCSI, nc->home, nc->home);
	strcpy(nc->uri, HYPERVISOR_URI);
	nc->convert_to_disk = 0;

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

static int
doRunInstance(		struct nc_state_t *nc,
			ncMetadata *meta,
			char *instanceId,
			char *reservationId,
			virtualMachine *params, 
			char *imageId, char *imageURL, 
			char *kernelId, char *kernelURL, 
			char *ramdiskId, char *ramdiskURL, 
			char *keyName, 
			//			char *privMac, char *privIp, int vlan, 
			netConfig *netparams,
			char *userData, char *launchIndex,
			char **groupNames, int groupNamesSize,
			ncInstance **outInst)
{
    ncInstance * instance = NULL;
    * outInst = NULL;
    pid_t pid;
    netConfig ncnet;
    int error;

    memcpy(&ncnet, netparams, sizeof(netConfig));

    /* check as much as possible before forking off and returning */
    sem_p (inst_sem);
    instance = find_instance (&global_instances, instanceId);
    sem_v (inst_sem);
    if (instance) {
        logprintfl (EUCAFATAL, "Error: instance %s already running\n", instanceId);
        return 1; /* TODO: return meaningful error codes? */
    }
    if (!(instance = allocate_instance (instanceId, 
                                        reservationId,
                                        params, 
                                        imageId, imageURL,
                                        kernelId, kernelURL,
                                        ramdiskId, ramdiskURL,
                                        instance_state_names[PENDING], 
                                        PENDING, 
                                        meta->userId, 
                                        &ncnet, keyName,
                                        userData, launchIndex, groupNames, groupNamesSize))) {
        logprintfl (EUCAFATAL, "Error: could not allocate instance struct\n");
        return 2;
    }
    change_state(instance, STAGING);

    sem_p (inst_sem); 
    error = add_instance (&global_instances, instance);
    sem_v (inst_sem);
    if ( error ) {
        free_instance (&instance);
        logprintfl (EUCAFATAL, "Error: could not save instance struct\n");
        return error;
    }

    instance->launchTime = time (NULL);
    /*
      instance->params.mem = params->mem;
      instance->params.cores = params->cores;
      instance->params.disk = params->disk;
      strcpy (instance->ncnet.privateIp, "0.0.0.0");
      strcpy (instance->ncnet.publicIp, "0.0.0.0");
    */

    /* do the potentially long tasks in a thread */
    pthread_attr_t* attr = (pthread_attr_t*) malloc(sizeof(pthread_attr_t));
    if (!attr) { 
        free_instance (&instance);
        logprintfl (EUCAFATAL, "Warning: out of memory\n");
        return 1;
    }
    pthread_attr_init(attr);
    pthread_attr_setdetachstate(attr, PTHREAD_CREATE_DETACHED);
    
    if ( pthread_create (&(instance->tcb), attr, startup_thread, (void *)instance) ) {
        pthread_attr_destroy(attr);
        logprintfl (EUCAFATAL, "failed to spawn a VM startup thread\n");
        sem_p (inst_sem);
        remove_instance (&global_instances, instance);
        sem_v (inst_sem);
        free_instance (&instance);
	if (attr) free(attr);
        return 1;
    }
    pthread_attr_destroy(attr);
    if (attr) free(attr);

    * outInst = instance;
    return 0;

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

  char *console_output=NULL, *console_append=NULL, *console_main=NULL;
  char console_file[MAX_PATH];
  int rc, fd, ret;
  struct stat statbuf;

  int bufsize, pid, status;

  *consoleOutput = NULL;

  snprintf(console_file, 1024, "%s/%s/%s/console.append.log", scGetInstancePath(), meta->userId, instanceId);
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


  if (getuid() != 0) {
    console_main = strdup("NOT SUPPORTED");
    if (!console_main) {
      fprintf(stderr, "strdup failed (out of memory?)\n");
      if (console_append) free(console_append);
      return 1;
    }
  } else {

  bufsize = sizeof(char) * 1024 * 64;
  console_main = malloc(bufsize);
  if (!console_main) {
    logprintfl(EUCAERROR, "doGetConsoleOutput(): out of memory!\n");
    if (console_append) free(console_append);
    return(1);
  }
  bzero(console_main, bufsize);

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
    ncInstance * instance;
    char localDevReal[32];
    virConnectPtr *conn;
    struct stat statbuf;

    // fix up format of incoming local dev name, if we need to
    ret = convert_dev_names (localDev, localDevReal, NULL);
    if (ret)
        return ret;

    sem_p (inst_sem); 
    instance = find_instance(&global_instances, instanceId);
    sem_v (inst_sem);
    if ( instance == NULL ) 
        return NOT_FOUND;

    /* try attaching to the Xen domain */
    conn = check_hypervisor_conn();
    if (conn) {
        sem_p(hyp_sem);
        virDomainPtr dom = virDomainLookupByName(*conn, instanceId);
	sem_v(hyp_sem);
        if (dom) {

            int err = 0;
            char xml [1024];
            int is_iscsi_target = 0;
            char *local_iscsi_dev;
            rc = 0;
            if(check_iscsi(remoteDev)) {
                is_iscsi_target = 1;
                /*get credentials, decrypt them*/
                //parse_target(remoteDev);
                /*login to target*/
                local_iscsi_dev = connect_iscsi_target(nc->connect_storage_cmd_path, remoteDev);
		if (!local_iscsi_dev || !strstr(local_iscsi_dev, "/dev")) {
		  logprintfl(EUCAERROR, "AttachVolume(): failed to connect to iscsi target\n");
		  rc = 1;
		} else {
		  snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s'/></disk>", local_iscsi_dev, localDevReal);
		}
            } else {
	        snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s'/></disk>", remoteDev, localDevReal);
                rc = stat(remoteDev, &statbuf);
                if (rc) {
                   logprintfl(EUCAERROR, "AttachVolume(): cannot locate local block device file '%s'\n", remoteDev);
                   rc = 1;
                }
	    }
            if (!rc) {
	        /* protect Xen calls, just in case */
                sem_p (hyp_sem);
                err = virDomainAttachDevice (dom, xml);
                sem_v (hyp_sem);
		if (err) {
                   logprintfl (EUCAERROR, "AttachVolume() failed (err=%d) XML=%s\n", err, xml);
//                   rc = doDetachVolume(nc, meta, instanceId, volumeId, remoteDev, localDev, 1);
                   ret = ERROR;
		} else {
                   logprintfl (EUCAINFO, "attached %s to %s in domain %s\n", remoteDev, localDevReal, instanceId);
		}
            } else { 
                ret = ERROR;
	    }

	    sem_p(hyp_sem);
            virDomainFree(dom);
	    sem_v(hyp_sem);
            if(is_iscsi_target) {
	      if (local_iscsi_dev) free(local_iscsi_dev);
            }
        } else {
            if (instance->state != BOOTING && instance->state != STAGING) {
                logprintfl (EUCAWARN, "warning: domain %s not running on hypervisor, cannot attach device\n", instanceId);
            }
            ret = ERROR;
        }
    } else {
        ret = ERROR;
    }

    if (ret==OK) {
        ncVolume * volume;

        sem_p (inst_sem);
        volume = add_volume (instance, volumeId, remoteDev, localDevReal, localDevReal, "attached");
	scSaveInstanceInfo(instance); /* to enable NC recovery */
        sem_v (inst_sem);
        if ( volume == NULL ) {
            logprintfl (EUCAFATAL, "ERROR: Failed to save the volume record, aborting volume attachment\n");
            return ERROR;
        }
    }

    return ret;
}

static int
doDetachVolume (	struct nc_state_t *nc,
			ncMetadata *meta,
			char *instanceId,
			char *volumeId,
			char *remoteDev,
			char *localDev,
			int force)
{
    int ret = OK;
    ncInstance * instance;
    char localDevReal[32];
    virConnectPtr *conn;

    // fix up format of incoming local dev name, if we need to
    ret = convert_dev_names (localDev, localDevReal, NULL);
    if (ret)
        return ret;

    sem_p (inst_sem); 
    instance = find_instance(&global_instances, instanceId);
    sem_v (inst_sem);
    if ( instance == NULL ) 
        return NOT_FOUND;

    /* try attaching to the Xen domain */
    conn = check_hypervisor_conn(); 
    if (conn) {
        sem_p(hyp_sem);
        virDomainPtr dom = virDomainLookupByName(*conn, instanceId);
	sem_v(hyp_sem);
        if (dom) {
	    int err = 0, fd, rc, pid, status;
            char xml [1024], tmpfile[32], cmd[MAX_PATH];
	    FILE *FH;
	            int is_iscsi_target = 0;
            char *local_iscsi_dev;
            if(check_iscsi(remoteDev)) {
                is_iscsi_target = 1;
                /*get credentials, decrypt them*/
                //parse_target(remoteDev);
                /*logout from target*/
                if((local_iscsi_dev = get_iscsi_target(nc->get_storage_cmd_path, remoteDev)) == NULL)
                    return ERROR;
                snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s'/></disk>", local_iscsi_dev, localDevReal);
            } else {
                snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s'/></disk>", remoteDev, localDevReal);
	    }
            /* protect Xen calls, just in case */
            sem_p (hyp_sem);
	    pid = fork();
	    if (!pid) {
	      char cmd[MAX_PATH];
	      snprintf(tmpfile, 32, "/tmp/detachxml.XXXXXX");
	      fd = mkstemp(tmpfile);
	      if (fd > 0) {
		write(fd, xml, strlen(xml));
		close(fd);
		snprintf(cmd, MAX_PATH, "%s %s `which virsh` %s %s %s", nc->detach_cmd_path, nc->rootwrap_cmd_path, instanceId, localDevReal, tmpfile);
		rc = system(cmd);
		rc = rc>>8;
		unlink(tmpfile);
	      } else {
		logprintfl(EUCAERROR, "could not write to tmpfile for detach XML: %s\n", tmpfile);
		rc = 1;
	      } 
	      exit(rc);
	    } else {
	      rc = timewait(pid, &status, 15);
	      if (WEXITSTATUS(status)) {
		logprintfl(EUCAERROR, "failed to sucessfully run detach helper\n");
		err = 1;
	      } else {
		err = 0;
	      }
	    }
#if 0
	    if (!getuid()) {
	      sem_p(hyp_sem);
	      err = virDomainDetachDevice (dom, xml);
	      sem_v(hyp_sem);
	    } else {
	      
	      /* virsh detach function does not work as non-root user on xen (bug). workaround is to shellout to virsh */
	      snprintf(tmpfile, 32, "/tmp/detachxml.XXXXXX");
	      fd = mkstemp(tmpfile);
	      if (fd > 0) {
		write(fd, xml, strlen(xml));
		close(fd);
		snprintf(cmd, MAX_PATH, "%s detach-device %s %s",virsh_command_path, instanceId, tmpfile);
		logprintfl(EUCADEBUG, "Running command: %s\n", cmd);
		err = WEXITSTATUS(system(cmd));
		unlink(tmpfile);
		if (err) {
		  logprintfl(EUCADEBUG, "first workaround command failed (%d), trying second workaround...\n", err);
		  snprintf(cmd, MAX_PATH, "%s block-detach %s %s", xm_command_path, instanceId, localDevReal);
		  logprintfl(EUCADEBUG, "Running command: %s\n", cmd);
		  err = WEXITSTATUS(system(cmd));
		}
	      } else {
		err = 1;
	      }
	    }
#endif
            sem_v (hyp_sem);
	    
            if (err) {
                logprintfl (EUCAERROR, "DetachVolume() failed (err=%d) XML=%s\n", err, xml);
                ret = ERROR;
            } else {
                logprintfl (EUCAINFO, "detached %s as %s in domain %s\n", remoteDev, localDevReal, instanceId);
            }
	    sem_p(hyp_sem);
            virDomainFree(dom);
	    sem_v(hyp_sem);
            if(is_iscsi_target) {
                if(disconnect_iscsi_target(nc->disconnect_storage_cmd_path, remoteDev) != 0) {
                    logprintfl (EUCAERROR, "disconnect_iscsi_target failed for %s\n", remoteDev);
                    ret = ERROR;
                }
                free(local_iscsi_dev);
            }
	} else {
            if (instance->state != BOOTING && instance->state != STAGING) {
                logprintfl (EUCAWARN, "warning: domain %s not running on hypervisor, cannot detach device\n", instanceId);
            }
            ret = ERROR;
        }
    } else {
        ret = ERROR;
    }

    if (ret==OK) {
        ncVolume * volume;

        sem_p (inst_sem);
        volume = free_volume (instance, volumeId, remoteDev, localDevReal);
        sem_v (inst_sem);
        if ( volume == NULL ) {
            logprintfl (EUCAFATAL, "ERROR: Failed to find and remove volume record, aborting volume detachment\n");
            return ERROR;
        }
    }

    return ret;
}

struct handlers xen_libvirt_handlers = {
    .name = "xen",
    .doInitialize        = doInitialize,
    .doDescribeInstances = NULL,
    .doRunInstance       = doRunInstance,
    .doTerminateInstance = NULL,
    .doRebootInstance    = doRebootInstance,
    .doGetConsoleOutput  = doGetConsoleOutput,
    .doDescribeResource  = NULL,
    .doStartNetwork      = NULL,
    .doPowerDown         = NULL,
    .doAttachVolume      = doAttachVolume,
    .doDetachVolume      = doDetachVolume
};


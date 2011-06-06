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
        
	logprintfl(EUCADEBUG, "doInitialized() invoked\n");

	/* set up paths of Eucalyptus commands NC relies on */
	snprintf (nc->gen_libvirt_cmd_path, MAX_PATH, EUCALYPTUS_GEN_KVM_LIBVIRT_XML, nc->home, nc->home);
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

	/* we leave 256M to the host  */
	nc->mem_max -= 256;

	/* let's adjust the values based on the config values */
	if (nc->config_max_mem && nc->config_max_mem < nc->mem_max)
		nc->mem_max = nc->config_max_mem;
	if (nc->config_max_cores)
		nc->cores_max = nc->config_max_cores;

	logprintfl(EUCAINFO, "Using %lld cores\n", nc->cores_max);
	logprintfl(EUCAINFO, "Using %lld memory\n", nc->mem_max);

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
        logprintfl (EUCAFATAL, "cannot restart instance %s, abandoning it\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        free (xml);
        return NULL;
    }
    
    virDomainPtr dom = virDomainLookupByName(*conn, instance->instanceId);
    if (dom == NULL) {
        free (xml);
        return NULL;
    }

    sem_p (hyp_sem);
    // for KVM, must stop and restart the instance
    int error = virDomainDestroy (dom); // TODO: change to Shutdown?  TODO: is this synchronous?
    sem_v (hyp_sem);
    virDomainFree(dom);
    if (error) {
        free (xml);
        return NULL;
    }
    
    // domain is now shut down, create a new one with the same XML
    sem_p (hyp_sem); 
    dom = virDomainCreateLinux (*conn, xml, 0);
    sem_v (hyp_sem);
    free (xml);

    //generate xml for each attached vol and attach them
    int i;
    for (i=0 ; i < instance->volumesSize; ++i) {
        char attach_xml[1024];
        int err = 0;
        int rc = 0;
        ncVolume *volume = &instance->volumes[i];
        rc = generate_attach_xml(volume->localDevReal, volume->remoteDev, &nc_state, attach_xml);
        if(!rc) {
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
        logprintfl (EUCAFATAL, "Failed to restart instance %s\n", instance->instanceId);
        change_state (instance, SHUTOFF);
        return NULL;
    }
    
    virDomainFree(dom);
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
        logprintfl (EUCAFATAL, "failed to spawn a reboot thread\n");
        return ERROR_FATAL;
    }
    if (pthread_detach(tcb)) {
      logprintfl(EUCAFATAL, "failed to detach the monitoring thread\n");
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
  
  sem_p (inst_sem); 
  snprintf(console_file, MAX_PATH, "%s/console.log", instance->instancePath);
  sem_v (inst_sem);

  rc = stat(console_file, &statbuf);
  if (rc >= 0) {
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
    char localDevReal[32], localDevTag[256], remoteDevReal[32];
    struct stat statbuf;
    int is_iscsi_target = 0, have_remote_device = 0;

    // fix up format of incoming local dev name, if we need to
    ret = convert_dev_names (localDev, localDevReal, localDevTag);
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
		rc = generate_attach_xml(localDevReal, remoteDevReal, nc, xml);
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
		    volume = add_volume (instance, volumeId, remoteDevReal, localDevTag, localDevReal, "attached");
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
    char localDevReal[32], localDevTag[256], remoteDevReal[32];
    struct stat statbuf;
    int is_iscsi_target = 0, have_remote_device = 0;
    ncVolume * volume;

    // fix up format of incoming local dev name, if we need to
    ret = convert_dev_names (localDev, localDevReal, localDevTag);
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
		//		ret = ERROR;
	      } else {
		rc = generate_attach_xml(localDevReal, remoteDevReal, nc, xml);
		if (!rc) {
		  /* protect KVM calls, just in case */
		  sem_p (hyp_sem);
		  err = virDomainDetachDevice (dom, xml);
		  sem_v (hyp_sem);
		  if (err) {
		    logprintfl (EUCAERROR, "DetachVolume(): virDomainDetachDevice() failed (err=%d) XML=%s\n", err, xml);
		    logprintfl (EUCAERROR, "DetachVolume(): failed to detach host device '%s' from guest device '%s' within instance '%s'\n", remoteDevReal, localDevReal, instanceId);
		    //		    ret = ERROR;
		  } else {
		    logprintfl (EUCAINFO, "DetachVolume(): success in detach of host device '%s' from guest device '%s' within instance '%s'\n", remoteDevReal, localDevReal, instanceId);
		    logprintfl(EUCAINFO, "DetachVolume(): successfully detached volume '%s' to instance '%s'\n", volumeId, instanceId);
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
      }
    }

    sem_p (inst_sem);
    volume = free_volume (instance, volumeId, remoteDevReal, localDevTag);
    save_instance_struct (instance);
    sem_v (inst_sem);
    if ( volume == NULL ) {
      logprintfl (EUCAWARN, "DetachVolume(): Failed to free the volume record, considering volume detached\n");
    }
    
    return ret;
#if 0
    int ret = OK;
    ncInstance * instance;
    virConnectPtr *conn;
    char localDevReal[32], localDevTag[256];

    // fix up format of incoming local dev name, if we need to
    ret = convert_dev_names (localDev, localDevReal, localDevTag);
    if (ret)
        return ret;

    // find the instance record
    if (grab_inst_sem)  
        sem_p (inst_sem); 
    instance = find_instance(&global_instances, instanceId);
    if (grab_inst_sem)  
        sem_v (inst_sem);
    if ( instance == NULL ) 
        return NOT_FOUND;
    if ( find_volume (instance, volumeId)==NULL )
        return NOT_FOUND;
    
    /* try attaching to the KVM domain */
    conn = check_hypervisor_conn();
    if (!conn) {
        ret = ERROR;

    } else {
        virDomainPtr dom = virDomainLookupByName(*conn, instanceId);
        if (dom) {
            int err = 0;
            char xml [1024];
            int is_iscsi_target = 0;
            char *local_iscsi_dev;
            int virtio_dev = 0;
            /* only attach using virtio when the device is /dev/vdXX */
            if (localDevReal[5] == 'v' && localDevReal[6] == 'd') {
                virtio_dev = 1;
            }
            if(check_iscsi(remoteDev)) {
                is_iscsi_target = 1;
                /*get credentials, decrypt them*/
                //parse_target(remoteDev);
                /*logout from target*/
                if((local_iscsi_dev = get_iscsi_target(remoteDev)) == NULL)
                    return ERROR;
                if (nc->config_use_virtio_disk && virtio_dev) {
                    snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s' bus='virtio'/></disk>", local_iscsi_dev, localDevReal);
                } else {
                    snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s'/></disk>", local_iscsi_dev, localDevReal);
                }
            } else {
                if (nc->config_use_virtio_disk && virtio_dev) {
   		    snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s' bus='virtio'/></disk>", remoteDev, localDevReal);
                } else {
   		    snprintf (xml, 1024, "<disk type='block'><driver name='phy'/><source dev='%s'/><target dev='%s'/></disk>", remoteDev, localDevReal);
                }
	    }
            /* protect KVM calls, just in case */
            sem_p (hyp_sem);
            err = virDomainDetachDevice (dom, xml);
            sem_v (hyp_sem);
            if (err) {
	      logprintfl (EUCAERROR, "virDomainDetachDevice() failed (err=%d) XML=%s\n", err, xml);
	      ret = ERROR;
            } else {
                logprintfl (EUCAINFO, "detached %s as %s in domain %s\n", remoteDev, localDevReal, instanceId);
            }
            virDomainFree(dom);
            if(is_iscsi_target) {
                if(disconnect_iscsi_target(remoteDev) != 0) {
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
    }

    if (ret==OK) {
        ncVolume * volume;
        if (grab_inst_sem)  
            sem_p (inst_sem);
        volume = free_volume (instance, volumeId, remoteDev, localDevTag);
        save_instance_struct (instance); // for surviving restart
        if (grab_inst_sem)  
            sem_v (inst_sem);
        if ( volume == NULL ) {
            logprintfl (EUCAFATAL, "ERROR: Failed to find and remove volume record, aborting volume detachment\n");
            ret =  ERROR;
        }
    }
    
    return ret;
#endif
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
    .doAttachVolume      = doAttachVolume,
    .doDetachVolume      = doDetachVolume
};


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
#ifndef INCLUDE_HANDLERS_H
#define INCLUDE_HANDLERS_H

#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>

#include "vnetwork.h"
#include "data.h"

#define LIBVIRT_QUERY_RETRIES 5
#define MAXDOMS 1024
#define BYTES_PER_DISK_UNIT 1048576 /* disk stats are in Gigs */
#define SWAP_SIZE 512 /* for now, the only possible swap size, in MBs */

/* NC state */
struct nc_state_t {
	struct handlers *H;             // selected handler
	struct handlers *D;             // default  handler
	vnetConfig *vnetconfig;		// network config
	// globals
	int  config_network_port;
	char admin_user_id[CHAR_BUFFER_SIZE];
	int save_instance_files;
	char uri[CHAR_BUFFER_SIZE];
	virConnectPtr conn;
	int convert_to_disk;
	// defined max
	long long config_max_disk;
	long long config_max_mem;
	long long config_max_cores;
	// current max
	long long disk_max;
	long long mem_max;
	long long cores_max;
	// paths
	char home[MAX_PATH];
	char config_network_path [MAX_PATH];
	char gen_libvirt_cmd_path[MAX_PATH];
	char get_info_cmd_path[MAX_PATH];
	char rootwrap_cmd_path[MAX_PATH];
	char virsh_cmd_path[MAX_PATH];
	char xm_cmd_path[MAX_PATH];
	char detach_cmd_path[MAX_PATH];
	char connect_storage_cmd_path[MAX_PATH];
	char disconnect_storage_cmd_path[MAX_PATH];
	char get_storage_cmd_path[MAX_PATH];
	// virtio
	int config_use_virtio_net;	// KVM: use virtio for network
	int config_use_virtio_disk;	// KVM: use virtio for disk attachment
	int config_use_virtio_root;	// KVM: use virtio for root partition
};


struct handlers {
    char name [CHAR_BUFFER_SIZE];
    int (*doInitialize)		(struct nc_state_t *nc);
    int (*doPowerDown)		(struct nc_state_t *nc,
		    		ncMetadata *meta);
    int (*doDescribeInstances)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char **instIds,
				int instIdsLen,
				ncInstance ***outInsts,
				int *outInstsLen);
    int (*doRunInstance)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				char *reservationId,
				virtualMachine *params,
				char *imageId,
				char *imageURL,
				char *kernelId,
				char *kernelURL,
				char *ramdiskId,
				char *ramdiskURL,
				char *keyName,
				netConfig *netparams,
				char *userData,
				char *launchIndex,
				char **groupNames,
				int groupNamesSize,
				ncInstance **outInst);
    int (*doTerminateInstance)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				int *shutdownState,
				int *previousState);
    int (*doRebootInstance)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId);
    int (*doGetConsoleOutput)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				char **consoleOutput);
    int (*doDescribeResource)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *resourceType,
			       	ncResource **outRes);
    int (*doStartNetwork)	(struct nc_state_t *nc,
				ncMetadata *ccMeta,
				char **remoteHosts,
				int remoteHostsLen,
				int port,
				int vlan);
    int (*doAttachVolume)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				char *volumeId,
				char *remoteDev,
				char *localDev);
    int (*doDetachVolume)	(struct nc_state_t *nc,
		    		ncMetadata *meta,
				char *instanceId,
				char *volumeId,
				char *remoteDev,
				char *localDev,
				int force);
};

#ifdef HANDLERS_FANOUT // only declare for the fanout code, not the actual handlers
int doPowerDown			(ncMetadata *meta);
int doDescribeInstances		(ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen);
int doRunInstance		(ncMetadata *meta, char *instanceId, char *reservationId, virtualMachine *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, netConfig *netparams, char *userData, char *launchIndex, char **groupNames, int groupNamesSize, ncInstance **outInst);
int doTerminateInstance		(ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState);
int doRebootInstance		(ncMetadata *meta, char *instanceId);
int doGetConsoleOutput		(ncMetadata *meta, char *instanceId, char **consoleOutput);
int doDescribeResource		(ncMetadata *meta, char *resourceType, ncResource **outRes);
int doStartNetwork		(ncMetadata *ccMeta, char **remoteHosts, int remoteHostsLen, int port, int vlan);
int doAttachVolume		(ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev);
int doDetachVolume		(ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force);
#endif /* HANDLERS_FANOUT */

/* helper functions used by the low level handlers */
int get_value(			char *s,
				const char *name,
				long long *valp);
int convert_dev_names(		char *localDev,
				char *localDevReal,
				char *localDevTag);
void libvirt_error_handler(	void * userData,
				virErrorPtr error);
void print_running_domains(	void);
virConnectPtr *check_hypervisor_conn();
void change_state(		ncInstance * instance,
				instance_states state);
int 
wait_state_transition (         ncInstance * instance, 
				instance_states from_state,
				instance_states to_state);
void adopt_instances();
int get_instance_xml(		const char *gen_libvirt_cmd_path,
				char *userId,
				char *instanceId,
				char *ramdiskId,
				char *kernelId,
				char *disk_path,
				virtualMachine *params,
				char *privMac,
				//				char *pubMac,
				char *brname,
				int use_virtio_net,
				int use_virtio_root,
				char **xml);
void * monitoring_thread(	void *arg);
void * startup_thread(		void *arg);

int check_iscsi(char* dev_string);
void parse_target(char *dev_string);
char* connect_iscsi_target(const char *storage_cmd_path, char *dev_string);
int disconnect_iscsi_target(const char *storage_cmd_path, char *dev_string);
char* get_iscsi_target(const char *storage_cmd_path, char *dev_string);

// bundling structure
struct bundling_params_t {
	ncInstance * instance;
	char * bucketName;
	char * filePrefix;
	char * walrusURL;
	char * userPublicKey;
  	char * S3Policy;
  	char * S3PolicySig;
	char * workPath; // work directory path
	char * diskPath; // disk file path
	char * eucalyptusHomePath; 
	long long sizeMb; // diskPath size
};

#endif /* INCLUDE */


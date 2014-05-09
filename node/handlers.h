// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*******************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/

//!
//! @file node/handlers.h
//! This defines the various hypervisor operations handlers available.
//!

#ifndef _INCLUDE_HANDLERS_H_
#define _INCLUDE_HANDLERS_H_

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <libvirt/libvirt.h>
#include <libvirt/virterror.h>

#include <misc.h>
#include <vnetwork.h>
#include <data.h>
#include <config.h>
#include <sensor.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define LIBVIRT_QUERY_RETRIES            5  //!< Number of query retries for libvirt failure
#define MAXDOMS                          1024   //!< Maximum number of domain
#define BYTES_PER_DISK_UNIT              1073741824 //!< describeResource disk units are GBs
#define MB_PER_DISK_UNIT                 1024   //!< describeResource disk units are GBs

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! NC State structure
struct nc_state_t {
    boolean is_enabled;                //!< flag determining if the node controller is enabled
    char version[CHAR_BUFFER_SIZE];    //!< version of the node controller

    struct handlers *H;                //!< selected handler
    struct handlers *D;                //!< default  handler
    hypervisorCapabilityType capability;
    vnetConfig *vnetconfig;            //!< network config

    //! @{
    //! @name Globals fields
    char admin_user_id[CHAR_BUFFER_SIZE];
    int save_instance_files;
    char uri[CHAR_BUFFER_SIZE];
    char iqn[CHAR_BUFFER_SIZE];
    char ip[HOSTNAME_SIZE];
    virConnectPtr conn;
    boolean convert_to_disk;
    boolean do_inject_key;
    int concurrent_disk_ops, concurrent_cleanup_ops;
    int sc_request_timeout_sec;
    int disable_snapshots;
    int staging_cleanup_threshold;
    int booting_cleanup_threshold;
    int bundling_cleanup_threshold;
    int createImage_cleanup_threshold;
    int teardown_state_duration;
    int migration_ready_threshold;
    int shutdown_grace_period_sec;
    boolean migration_capable;
    //! @}

    //! @{
    //! @name Defined maximum values fields
    long long config_max_mem;
    long long config_max_cores;
    //! @}

    //! @{
    //! @name Defined phylical system limitation
    long long phy_max_mem;
    long long phy_max_cores;
    //! @}

    //! @{
    //! @name Current maximum values fields
    long long disk_max;
    long long mem_max;
    long long cores_max;
    //! @}

    //! @{
    //! @name Paths related fields
    char home[MAX_PATH];
    char configFiles[2][MAX_PATH];
    char config_network_path[MAX_PATH];
    char libvirt_xslt_path[MAX_PATH];
    char get_info_cmd_path[MAX_PATH];
    char rootwrap_cmd_path[MAX_PATH];
    char virsh_cmd_path[MAX_PATH];
    char xm_cmd_path[MAX_PATH];
    char detach_cmd_path[MAX_PATH];
    //! @}

    //! @{
    //! @name Virtual IO related fields
    int config_use_virtio_net;         //!< KVM: use virtio for network
    int config_use_virtio_disk;        //!< KVM: use virtio for disk attachment
    int config_use_virtio_root;        //!< KVM: use virtio for root partition
    //! @}

    //! @{
    //! @name Windows support fields
    char ncBundleUploadCmd[MAX_PATH];
    char ncCheckBucketCmd[MAX_PATH];
    char ncDeleteBundleCmd[MAX_PATH];
    //! @}

    //! @name SC Client config fields
    int config_use_ws_sec;             //!< use WS security in SOAP
    char config_sc_policy_file[MAX_PATH];   //!< policy config file to use for sc client ($EUCALYPTUS/var/lib/eucalyptus/keys/sc-client-policy.xml
    //! @}

    //! @name Service info state for the NC
    serviceStatusType ncStatus;
    serviceInfoType services[16];
    serviceInfoType disabledServices[16];
    serviceInfoType notreadyServices[16];
    int servicesLen;
    int disabledServicesLen;
    int notreadyServicesLen;
    //! @}
};

//! Hypervisor specific operation handlers
struct handlers {
    char name[CHAR_BUFFER_SIZE];
    int (*doInitialize) (struct nc_state_t * nc);
    int (*doAssignAddress) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId, char *publicIp);
    int (*doPowerDown) (struct nc_state_t * nc, ncMetadata * pMeta);
    int (*doDescribeInstances) (struct nc_state_t * nc, ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen);
    int (*doRunInstance) (struct nc_state_t * nc, ncMetadata * pMeta, char *uuid, char *instanceId, char *reservationId, virtualMachine * params,
                          char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId,
                          char *accountId, char *keyName, netConfig * netparams, char *userData, char *launchIndex, char *platform, int expiryTime,
                          char **groupNames, int groupNamesSize, ncInstance ** outInstPtr);
    int (*doTerminateInstance) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState);
    int (*doRebootInstance) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId);
    int (*doGetConsoleOutput) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId, char **consoleOutput);
    int (*doDescribeResource) (struct nc_state_t * nc, ncMetadata * pMeta, char *resourceType, ncResource ** outRes);
    int (*doStartNetwork) (struct nc_state_t * nc, ncMetadata * pMeta, char *uuid, char **remoteHosts, int remoteHostsLen, int port, int vlan);
    int (*doAttachVolume) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev);
    int (*doDetachVolume) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev, int force, int grab_inst_sem);
    int (*doCreateImage) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev);
    int (*doBundleInstance) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL,
                             char *userPublicKey, char *S3Policy, char *S3PolicySig);
    int (*doBundleRestartInstance) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId);
    int (*doCancelBundleTask) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId);
    int (*doDescribeBundleTasks) (struct nc_state_t * nc, ncMetadata * pMeta, char **instIds, int instIdsLen, bundleTask *** outBundleTasks, int *outBundleTasksLen);
    int (*doDescribeSensors) (struct nc_state_t * nc, ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds,
                              int instIdsLen, char **sensorIds, int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen);
    int (*doModifyNode) (struct nc_state_t * nc, ncMetadata * pMeta, char *stateName);
    int (*doMigrateInstances) (struct nc_state_t * nc, ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials);
    int (*doStartInstance) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId);
    int (*doStopInstance) (struct nc_state_t * nc, ncMetadata * pMeta, char *instanceId);
};

 //! bundling structure
struct bundling_params_t {
    ncInstance *instance;
    char *bucketName;
    char *filePrefix;
    char *walrusURL;
    char *userPublicKey;
    char *S3Policy;
    char *S3PolicySig;
    char *workPath;                    //!< work directory path
    char *diskPath;                    //!< disk file path
    char *kernelId;
    char *ramdiskId;
    char *eucalyptusHomePath;
    long long sizeMb;                  //!< diskPath size
    char *ncBundleUploadCmd;
    char *ncCheckBucketCmd;
    char *ncDeleteBundleCmd;
};

 //! image structure
struct createImage_params_t {
    ncInstance *instance;
    char *volumeId;
    char *remoteDev;
    char *workPath;                    //!< work directory path
    char *diskPath;                    //!< disk file path
    char *eucalyptusHomePath;
    long long sizeMb;                  //!< diskPath size
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

extern configEntry configKeysRestartNC[];
extern configEntry configKeysNoRestartNC[];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#ifdef HANDLERS_FANOUT
// only declare for the fanout code, not the actual handlers
int doAssignAddress(ncMetadata * pMeta, char *instanceId, char *publicIp);
int doPowerDown(ncMetadata * pMeta);
int doDescribeInstances(ncMetadata * pMeta, char **instIds, int instIdsLen, ncInstance *** outInsts, int *outInstsLen);
int doRunInstance(ncMetadata * pMeta, char *uuid, char *instanceId, char *reservationId, virtualMachine * params, char *imageId, char *imageURL,
                  char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *ownerId, char *accountId, char *keyName,
                  netConfig * netparams, char *userData, char *launchIndex, char *platform, int expiryTime, char **groupNames, int groupNamesSize, ncInstance ** outInst);
int doTerminateInstance(ncMetadata * pMeta, char *instanceId, int force, int *shutdownState, int *previousState);
int doRebootInstance(ncMetadata * pMeta, char *instanceId);
int doGetConsoleOutput(ncMetadata * pMeta, char *instanceId, char **consoleOutput);
int doDescribeResource(ncMetadata * pMeta, char *resourceType, ncResource ** outRes);
int doStartNetwork(ncMetadata * pMeta, char *uuid, char **remoteHosts, int remoteHostsLen, int port, int vlan);
int doAttachVolume(ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev);
int doDetachVolume(ncMetadata * pMeta, char *instanceId, char *volumeId, char *attachmentToken, char *localDev, int force, int grab_inst_sem);
int doBundleInstance(ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL, char *userPublicKey, char *S3Policy, char *S3PolicySig);
int doBundleRestartInstance(ncMetadata * pMeta, char *instanceId);
int doCancelBundleTask(ncMetadata * pMeta, char *instanceId);
int doDescribeBundleTasks(ncMetadata * pMeta, char **instIds, int instIdsLen, bundleTask *** outBundleTasks, int *outBundleTasksLen);
int doCreateImage(ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev);
int doDescribeSensors(ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds, int instIdsLen, char **sensorIds,
                      int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen);
int doModifyNode(ncMetadata * pMeta, char *stateName);
int doMigrateInstances(ncMetadata * pMeta, ncInstance ** instances, int instancesLen, char *action, char *credentials);
int doStartInstance(ncMetadata * pMeta, char *instanceId);
int doStopInstance(ncMetadata * pMeta, char *instanceId);
#endif /* HANDLERS_FANOUT */

int callBundleInstanceHelper(struct nc_state_t *nc, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL, char *userPublicKey, char *S3Policy, char *S3PolicySig);

// helper functions used by the low level handlers
int get_value(char *s, const char *name, long long *valp);
int convert_dev_names(char *localDev, char *localDevReal, char *localDevTag);
void print_running_domains(void);
virConnectPtr lock_hypervisor_conn(void);
void unlock_hypervisor_conn(void);
void change_state(ncInstance * instance, instance_states state);
int wait_state_transition(ncInstance * instance, instance_states from_state, instance_states to_state);
void adopt_instances();
int get_instance_xml(const char *gen_libvirt_cmd_path, char *userId, char *instanceId, char *platform, char *ramdiskId, char *kernelId,
                     char *disk_path, virtualMachine * params, char *privMac, char *brname, int use_virtio_net, int use_virtio_root, char **xml);
void set_instance_params(ncInstance * instance);
void *monitoring_thread(void *arg);
void *startup_thread(void *arg);
void *terminating_thread(void *arg);

int get_instance_stats(virDomainPtr dom, ncInstance * instance);
ncInstance *find_global_instance(const char *instanceId);
int find_and_terminate_instance(char *instanceId);
int find_and_stop_instance(char *psInstanceId);
int find_and_start_instance(char *psInstanceId);
int shutdown_then_destroy_domain(const char *instanceId, boolean do_destroy);
void copy_instances(void);
int is_migration_dst(const ncInstance * instance);
int is_migration_src(const ncInstance * instance);
int migration_rollback(ncInstance * instance);
int get_service_url(const char *service_type, struct nc_state_t *nc, char *dest_buffer);
int authorize_migration_keys(char *options, char *host, char *credentials, ncInstance * instance, boolean lock_hyp_sem);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_HANDLERS_H_ */

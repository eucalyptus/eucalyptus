// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
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
 ************************************************************************/

#ifndef _INCLUDE_HANDLERS_H_
#define _INCLUDE_HANDLERS_H_

//!
//! @file cluster/handlers.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <eucalyptus.h>
#include <semaphore.h>
#include <data.h>
#include <client-marshal.h>
#include <vnetwork.h>
#include <linux/limits.h>
#include "config.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define OP_TIMEOUT                               60
#define OP_TIMEOUT_PERNODE                       20
#define OP_TIMEOUT_MIN                            5
#define LOG_INTERVAL_SUMMARY_SEC                 60
#define SCHED_TIMEOUT_SEC                         8 //! timeout for user scheduler

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

enum {
    SHARED_MEM,
    SHARED_FILE,
};

enum {
    INIT,
    CONFIG,
    VNET,
    INSTCACHE,
    RESCACHE,
    RESCACHESTAGE,
    REFRESHLOCK,
    BUNDLECACHE,
    SENSORCACHE,
    GLOBALNETWORKINFO,
    NCCALL0,
    NCCALL1,
    NCCALL2,
    NCCALL3,
    NCCALL4,
    NCCALL5,
    NCCALL6,
    NCCALL7,
    NCCALL8,
    NCCALL9,
    NCCALL10,
    NCCALL11,
    NCCALL12,
    NCCALL13,
    NCCALL14,
    NCCALL15,
    NCCALL16,
    NCCALL17,
    NCCALL18,
    NCCALL19,
    NCCALL20,
    NCCALL21,
    NCCALL22,
    NCCALL23,
    NCCALL24,
    NCCALL25,
    NCCALL26,
    NCCALL27,
    NCCALL28,
    NCCALL29,
    NCCALL30,
    NCCALL31,
    ENDLOCK,
};

enum {
    PRIMORDIAL,
    INITIALIZED,
    LOADED,
    DISABLED,
    ENABLED,
    STOPPED,
    NOTREADY,
    SHUTDOWNCC,
};

enum {
    RESDOWN,
    RESUP,
    RESASLEEP,
    RESWAKING,
};

enum {
    INSTINVALID,
    INSTVALID,
    INSTCONFLICT,
};

enum {
    RESINVALID,
    RESVALID,
};

enum {
    MONITOR = 0,
    SENSOR,
    NUM_THREADS,
};

enum {
    CONFIGLOCK,
    CACHELOCK,
    VNETCONFIGLOCK,
};

enum {
    SCHEDGREEDY,
    SCHEDROUNDROBIN,
    SCHEDPOWERSAVE,
    SCHEDUSER,
    SCHEDLAST,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct instance_t {
    char instanceId[16];
    char reservationId[16];

    char amiId[16];
    char kernelId[16];
    char ramdiskId[16];

    char amiURL[512];
    char kernelURL[512];
    char ramdiskURL[512];

    char state[16];
    char ccState[16];
    time_t ts;

    migration_states migration_state;
    char migration_src[HOSTNAME_SIZE];
    char migration_dst[HOSTNAME_SIZE];

    char ownerId[48];
    char accountId[48];
    char keyName[1024];

    netConfig ccnet, ncnet;
    virtualMachine ccvm;

    int ncHostIdx;
    char serviceTag[384];
    char uuid[48];

    char userData[16384];
    char launchIndex[64];

    char platform[64];
    char guestStateName[64];
    char bundleTaskStateName[64];
    char createImageTaskStateName[64];

    int expiryTime;

    char groupNames[64][64];

    ncVolume volumes[EUCA_MAX_VOLUMES];
    int volumesSize;

    long long blkbytes;
    long long netbytes;
} ccInstance;

typedef struct resource_t {
    char ncURL[384];
    char ncService[128];
    int ncPort;
    char hostname[256];
    char mac[24];
    char ip[24];
    char iqn[128];
    int maxMemory;
    int availMemory;
    int maxDisk;
    int availDisk;
    int maxCores;
    int availCores;
    // state information
    int state, lastState, ncState;
    time_t stateChange;
    time_t idleStart;
    int running;
    int lockidx;
    char nodeMessage[1024];
    char nodeStatus[24];
    boolean migrationCapable;
} ccResource;

typedef struct ccResourceCache_t {
    ccResource resources[MAXNODES];
    int cacheState[MAXNODES];
    int numResources;
    int lastResourceUpdate;
    int resourceCacheUpdate;
} ccResourceCache;

typedef struct ccInstanceCache_t {
    ccInstance instances[MAXINSTANCES_PER_CC];
    time_t lastseen[MAXINSTANCES_PER_CC];
    int cacheState[MAXINSTANCES_PER_CC];
    int numInsts;
    int instanceCacheUpdate;
    int dirty;
} ccInstanceCache;

typedef struct ccConfig_t {
    char eucahome[EUCA_MAX_PATH];
    char log_file_path[EUCA_MAX_PATH];
    long log_max_size_bytes;
    int log_roll_number;
    int log_level;
    char log_prefix[64];
    char log_facility[32];
    char proxyPath[EUCA_MAX_PATH];
    char proxyIp[32];
    int use_proxy;
    int proxy_max_cache_size;
    char configFiles[2][EUCA_MAX_PATH];
    int use_wssec;
    int use_tunnels;
    char policyFile[EUCA_MAX_PATH];
    int initialized;
    int kick_dhcp;
    int schedPolicy;
    char schedPath[EUCA_MAX_PATH];
    int schedState;
    int idleThresh;
    int wakeThresh;
    time_t instanceTimeout;
    time_t ncPollingFrequency;
    time_t clcPollingFrequency;
    time_t ncSensorsPollingInterval;
    int threads[NUM_THREADS];
    int ncFanout;
    int ccState;
    int ccLastState;
    int kick_network;
    int kick_enabled;
    int kick_monitor_running;
    int kick_broadcast_network_info;
    uint32_t cloudIp;
    serviceStatusType ccStatus;
    serviceInfoType services[16];
    serviceInfoType disabledServices[16];
    serviceInfoType notreadyServices[16];
    char arbitrators[256];
    int arbitratorFails;
} ccConfig;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

extern configEntry configKeysRestartCC[];
extern configEntry configKeysNoRestartCC[];
extern char *SCHEDPOLICIES[SCHEDLAST];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

void doInitCC(void);
int doBundleInstance(ncMetadata * pMeta, char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL, char *userPublicKey, char *S3Policy, char *S3PolicySig);
int doBundleRestartInstance(ncMetadata * pMeta, char *instanceId);
int doCancelBundleTask(ncMetadata * pMeta, char *instanceId);
int ncClientCall(ncMetadata * pMeta, int timeout, int ncLock, char *ncURL, char *ncOp, ...);
int ncGetTimeout(time_t op_start, time_t op_max, int numCalls, int idx);
int doAttachVolume(ncMetadata * pMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev);
int doDetachVolume(ncMetadata * pMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev, int force);
int doConfigureNetwork(ncMetadata * pMeta, char *accountId, char *type, int namedLen, char **sourceNames, char **userNames, int netLen,
                       char **sourceNets, char *destName, char *destUserName, char *protocol, int minPort, int maxPort);
int doFlushNetwork(ncMetadata * pMeta, char *accountId, char *destName);
int doBroadcastNetworkInfo(ncMetadata * pMeta, char *networkInfo);
int doAssignAddress(ncMetadata * pMeta, char *uuid, char *src, char *dst);
int doDescribePublicAddresses(ncMetadata * pMeta, publicip ** outAddresses, int *outAddressesLen);
int doUnassignAddress(ncMetadata * pMeta, char *src, char *dst);
int doStopNetwork(ncMetadata * pMeta, char *accountId, char *netName, int vlan);
int doDescribeNetworks(ncMetadata * pMeta, char *vmsubdomain, char *nameservers, char **ccs, int ccsLen, vnetConfig * outvnetConfig);
int doStartNetwork(ncMetadata * pMeta, char *accountId, char *uuid, char *netName, int vlan, char *vmsubdomain, char *nameservers, char **ccs, int ccsLen);
int doDescribeResources(ncMetadata * pMeta, virtualMachine ** ccvms, int vmLen, int **outTypesMax, int **outTypesAvail, int *outTypesLen, ccResource ** outNodes, int *outNodesLen);
int changeState(ccResource * in, int newstate);
int refresh_resources(ncMetadata * pMeta, int timeout, int dolock);
int refresh_instances(ncMetadata * pMeta, int timeout, int dolock);
int refresh_sensors(ncMetadata * pMeta, int timeout, int dolock);
int broadcast_network_info(ncMetadata * pMeta, int timeout, int dolock);
int doDescribeInstances(ncMetadata * pMeta, char **instIds, int instIdsLen, ccInstance ** outInsts, int *outInstsLen);
int powerUp(ccResource * res);
int powerDown(ncMetadata * pMeta, ccResource * node);
void print_netConfig(char *prestr, netConfig * in);
int ncInstance_to_ccInstance(ccInstance * dst, ncInstance * src);
int ccInstance_to_ncInstance(ncInstance * dst, ccInstance * src);
int schedule_instance(virtualMachine * vm, char *amiId, char *kernelId, char *ramdiskId, char *instId, char *userData, char *platform, char *targetNode, int *outresid);
int schedule_instance_roundrobin(virtualMachine * vm, int *outresid);
int schedule_instance_explicit(virtualMachine * vm, char *targetNode, int *outresid, boolean is_migration);
int schedule_instance_user(virtualMachine * vm, char *amiId, char *kernelId, char *ramdiskId, char *instId, char *userData, char *platform, int *outresid);
int schedule_instance_greedy(virtualMachine * vm, int *outresid);
int doRunInstances(ncMetadata * pMeta, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char **instIds,
                   int instIdsLen, char **netNames, int netNamesLen, char **macAddrs, int macAddrsLen, int *networkIndexList, int networkIndexListLen,
                   char **uuids, int uuidsLen, char **privateIps, int privateIpsLen, int minCount, int maxCount, char *accountId, char *ownerId,
                   char *reservationId, virtualMachine * ccvm, char *keyName, int vlan, char *userData, char *credential, char *launchIndex,
                   char *platform, int expiryTime, char *targetNode, ccInstance ** outInsts, int *outInstsLen);
int doGetConsoleOutput(ncMetadata * pMeta, char *instanceId, char **consoleOutput);
int doRebootInstances(ncMetadata * pMeta, char **instIds, int instIdsLen);
int doTerminateInstances(ncMetadata * pMeta, char **instIds, int instIdsLen, int force, int **outStatus);
int doCreateImage(ncMetadata * pMeta, char *instanceId, char *volumeId, char *remoteDev);
int doDescribeSensors(ncMetadata * pMeta, int historySize, long long collectionIntervalTimeMs, char **instIds, int instIdsLen, char **sensorIds,
                      int sensorIdsLen, sensorResource *** outResources, int *outResourcesLen);
int doModifyNode(ncMetadata * pMeta, char *nodeName, char *nodeState);
int doMigrateInstances(ncMetadata * pMeta, char *sourceNode, char *instanceId, char **destinationNodes, int destinationNodeCount, int allowHosts, char *nodeAction);
int doStartInstance(ncMetadata * pMeta, char *instanceId);
int doStopInstance(ncMetadata * pMeta, char *instanceId);
int setup_shared_buffer(void **buf, char *bufname, size_t bytes, sem_t ** lock, char *lockname, int mode);
int initialize(ncMetadata * pMeta, boolean authoritative);
int ccIsEnabled(void);
int ccIsDisabled(void);
int ccChangeState(int newstate);
int ccGetStateString(char *statestr, int n);
int ccCheckState(int clcTimer);
int doBrokerPairing(void);
void *monitor_thread(void *in);
int init_pthreads(void);
int init_log(void);
int init_thread(void);
int update_config(void);
int init_config(void);
int syncNetworkState(void);
int checkActiveNetworks(void);
int maintainNetworkState(void);
int restoreNetworkState(void);
int reconfigureNetworkFromCLC(void);
int refreshNodes(ccConfig * config, ccResource ** res, int *numHosts);
void shawn(void);
int allocate_ccResource(ccResource * out, char *ncURL, char *ncService, int ncPort, char *hostname, char *mac, char *ip, int maxMemory,
                        int availMemory, int maxDisk, int availDisk, int maxCores, int availCores, int state, int laststate, time_t stateChange, time_t idleStart);
int free_instanceNetwork(char *mac, int vlan, int force, int dolock);
int allocate_ccInstance(ccInstance * out, char *id, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL,
                        char *ownerId, char *accountId, char *state, char *ccState, time_t ts, char *reservationId, netConfig * ccnet,
                        netConfig * ncnet, virtualMachine * ccvm, int ncHostIdx, char *keyName, char *serviceTag, char *userData, char *launchIndex,
                        char *platform, char *guestStateName, char *bundleTaskStateName, char groupNames[][64], ncVolume * volumes, int volumesSize);
int pubIpCmp(ccInstance * inst, void *ip);
int privIpCmp(ccInstance * inst, void *ip);
int privIpSet(ccInstance * inst, void *ip);
int pubIpSet(ccInstance * inst, void *ip);
int map_instanceCache(int (*match) (ccInstance *, void *), void *matchParam, int (*operate) (ccInstance *, void *), void *operateParam);
void print_instanceCache(void);
void print_ccInstance(char *tag, ccInstance * in);
void set_clean_instanceCache(void);
void set_dirty_instanceCache(void);
int is_clean_instanceCache(void);
void invalidate_instanceCache(void);
int refresh_instanceCache(char *instanceId, ccInstance * in);
int add_instanceCache(char *instanceId, ccInstance * in);
int del_instanceCacheId(char *instanceId);
int find_instanceCacheId(char *instanceId, ccInstance ** out);
int find_instanceCacheIP(char *ip, ccInstance ** out);
void print_resourceCache(void);
void invalidate_resourceCache(void);
int refresh_resourceCache(char *host, ccResource * in);
int add_resourceCache(char *host, ccResource * in);
int del_resourceCacheId(char *host);
int find_resourceCacheId(char *host, ccResource ** out);
void unlock_exit(int code);
int sem_mywait(int lockno);
int sem_mypost(int lockno);
int image_cache(char *id, char *url);
int image_cache_invalidate(void);
int image_cache_proxykick(ccResource * res, int *numHosts);

int writePubPrivIPMap(ccInstance * inst, void *in);

//! For filtering service infos in the meta passed to the NC
void filter_services(ncMetadata * meta, char *filter_partition);

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

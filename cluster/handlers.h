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
#ifndef INCLUDE_HANDLERS_H
#define INCLUDE_HANDLERS_H

#include <eucalyptus.h>
#include <semaphore.h>
#include <data.h>
#include <client-marshal.h>
#include <vnetwork.h>
#include <linux/limits.h>

#ifndef MAX_PATH
#define MAX_PATH 4096
#endif

#define OP_TIMEOUT 60
#define OP_TIMEOUT_PERNODE 20
#define OP_TIMEOUT_MIN 5

enum {SHARED_MEM, SHARED_FILE};
enum {INIT, CONFIG, VNET, INSTCACHE, RESCACHE, RESCACHESTAGE, REFRESHLOCK, BUNDLECACHE, NCCALL0, NCCALL1, NCCALL2, NCCALL3, NCCALL4, NCCALL5, NCCALL6, NCCALL7, NCCALL8, NCCALL9, NCCALL10, NCCALL11, NCCALL12, NCCALL13, NCCALL14, NCCALL15, NCCALL16, NCCALL17, NCCALL18, NCCALL19, NCCALL20, NCCALL21, NCCALL22, NCCALL23, NCCALL24, NCCALL25, NCCALL26, NCCALL27, NCCALL28, NCCALL29, NCCALL30, NCCALL31, ENDLOCK};
enum {PRIMORDIAL, INITIALIZED, LOADED, DISABLED, ENABLED, STOPPED, NOTREADY, SHUTDOWNCC};

typedef struct configEntry_t {
  char *key;
  char *defaultValue;
} configEntry;

static configEntry configKeysRestart[] = {
  {"DISABLE_TUNNELING", "N"},
  {"ENABLE_WS_SECURITY", "Y"},
  {"EUCALYPTUS", "/"},
  {"NC_FANOUT", "1"},
  {"NC_PORT", "8775"},
  {"NC_SERVICE", "axis2/services/EucalyptusNC"},
  {"SCHEDPOLICY", "ROUNDROBIN"},
  {"VNET_ADDRSPERNET", NULL},
  {"VNET_BRIDGE", NULL},
  {"VNET_BROADCAST", NULL},
  {"VNET_DHCPDAEMON", "/usr/sbin/dhcpd3"},
  {"VNET_DHCPUSER", "dhcpd"},
  {"VNET_DNS", NULL},
  {"VNET_DOMAINNAME", "eucalyptus"},
  {"VNET_LOCALIP", NULL},
  {"VNET_MACMAP", NULL},
  {"VNET_MODE", "SYSTEM"},
  {"VNET_NETMASK", NULL},
  {"VNET_PRIVINTERFACE", "eth0"},
  {"VNET_PUBINTERFACE", "eth0"},
  {"VNET_PUBLICIPS", NULL},
  {"VNET_ROUTER", NULL},
  {"VNET_SUBNET", NULL},
  {"VNET_MACPREFIX", "d0:0d"},
  {"POWER_IDLETHRESH", "300"},
  {"POWER_WAKETHRESH", "300"},
  {"CC_IMAGE_PROXY", NULL},
  {"CC_IMAGE_PROXY_CACHE_SIZE", "32768"},
  {"CC_IMAGE_PROXY_PATH", "$EUCALYPTUS/var/lib/eucalyptus/dynserv/"},
  {"LOGLEVEL", "DEBUG"},
  {"LOGROLLNUMBER", "4"},
  {NULL, NULL}
};
static configEntry configKeysNoRestart[] = {
  {"NODES", NULL},
  {"NC_POLLING_FREQUENCY", "6"},
  {"CLC_POLLING_FREQUENCY", "6"},
  {"CC_ARBITRATORS", NULL},
  {NULL, NULL}
};

static char *configValuesRestart[256], *configValuesNoRestart[256];
static int configRestartLen=0, configNoRestartLen=0;

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
  
  char ownerId[48];
  char accountId[48];
  char keyName[1024];
  
  netConfig ccnet, ncnet;
  virtualMachine ccvm;

  int ncHostIdx;
  char serviceTag[64];
  char uuid[48];

  char userData[16384];
  char launchIndex[64];

  char platform[64];
  char bundleTaskStateName[64];
  char createImageTaskStateName[64];

  int expiryTime;

  char groupNames[64][64];

  ncVolume volumes[EUCA_MAX_VOLUMES];
  int volumesSize;

  long long blkbytes, netbytes;
} ccInstance;

int allocate_ccInstance(ccInstance *out, char *id, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char *ownerId, char *accountId, char *state, char *ccState, time_t ts, char *reservationId, netConfig *ccnet, netConfig *ncnet, virtualMachine *ccvm, int ncHostIdx, char *keyName, char *serviceTag, char *userData, char *launchIndex, char *platform, char *bundleTaskStateName, char groupNames[][64], ncVolume *volumes, int volumesSize);
void print_ccInstance(char *tag, ccInstance *in);

enum {RESDOWN, RESUP, RESASLEEP, RESWAKING};
enum {INSTINVALID, INSTVALID, INSTCONFLICT};
enum {RESINVALID, RESVALID};
enum {MONITOR, CLEANUP, CONTROL};
enum {CONFIGLOCK, CACHELOCK, VNETCONFIGLOCK};

typedef struct resource_t {
  char ncURL[128];
  char ncService[128];
  int ncPort;
  char hostname[128], mac[24], ip[24], iqn[128];
  int maxMemory, availMemory, maxDisk, availDisk, maxCores, availCores;
  // state information
  int state, lastState;
  time_t stateChange, idleStart;
  int running;
  int lockidx;
} ccResource;
int allocate_ccResource(ccResource *out, char *ncURL, char *ncService, int ncPort, char *hostname, char *mac, char *ip, int maxMemory, int availMemory, int maxDisk, int availDisk, int maxCores, int availCores, int state, int laststate, time_t stateChange, time_t idleStart);

typedef struct ccResourceCache_t {
  ccResource resources[MAXNODES];
  int cacheState[MAXNODES];
  int numResources;
  int lastResourceUpdate;
  int resourceCacheUpdate;
} ccResourceCache;

typedef struct ccInstanceCache_t {
  ccInstance instances[MAXINSTANCES];
  time_t lastseen[MAXINSTANCES];
  int cacheState[MAXINSTANCES];
  int numInsts;
  int instanceCacheUpdate;
  int dirty;
} ccInstanceCache;

typedef struct ccConfig_t {
  char eucahome[MAX_PATH];
  char proxyPath[MAX_PATH];
  char proxyIp[32];
  int use_proxy;
  int proxy_max_cache_size;
  char configFiles[2][MAX_PATH];
  int use_wssec, use_tunnels;
  char policyFile[MAX_PATH];
  int initialized, kick_dhcp;
  int schedPolicy, schedState;
  int idleThresh, wakeThresh;
  time_t configMtime, instanceTimeout, ncPollingFrequency, clcPollingFrequency;
  int threads[3];
  int ncFanout;
  int ccState, ccLastState, kick_network, kick_enabled, kick_monitor_running;
  uint32_t cloudIp;
  serviceStatusType ccStatus;
  serviceInfoType services[16], disabledServices[16], notreadyServices[16];
  char arbitrators[256];
  int arbitratorFails;
} ccConfig;

enum {SCHEDGREEDY, SCHEDROUNDROBIN, SCHEDPOWERSAVE, SCHEDLAST};
static char *SCHEDPOLICIES[SCHEDLAST] = {"GREEDY", "ROUNDROBIN", "POWERSAVE"};

void doInitCC(void);
int doStartNetwork(ncMetadata *ccMeta, char *accountId, char *uuid, char *netName, int vlan, char *nameserver, char **ccs, int ccsLen);
int doConfigureNetwork(ncMetadata *meta, char *accountId, char *type, int namedLen, char **sourceNames, char **userNames, int netLen, char **sourceNets, char *destName, char *destUserName, char *protocol, int minPort, int maxPort);
int doStopNetwork(ncMetadata *ccMeta, char *accountId, char *netName, int vlan);

int doAttachVolume(ncMetadata *ccMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev);
int doDetachVolume(ncMetadata *ccMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev, int force);

int doBundleInstance(ncMetadata *ccMeta, char *instanceId, char *bucketName, char *filePrefix, char *walrusURL, char *userPublicKey, char *S3Policy, char *S3PolicySig);
int doCancelBundleTask(ncMetadata *ccMeta, char *instanceId);

int doAssignAddress(ncMetadata *ccMeta, char *uuid, char *src, char *dst);
int doUnassignAddress(ncMetadata *ccMeta, char *src, char *dst);
int doDescribePublicAddresses(ncMetadata *ccMeta, publicip **outAddresses, int *outAddressesLen);
int doDescribeNetworks(ncMetadata *ccMeta, char *nameserver, char **ccs, int ccsLen, vnetConfig *outvnetConfig);

int doDescribeInstances(ncMetadata *meta, char **instIds, int instIdsLen, ccInstance **outInsts, int *outInstsLen);
int doRunInstances(ncMetadata *ccMeta, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char **instIds, int instIdsLen, char **netNames, int netNamesLen, char **macAddrs, int macAddrsLen, int *networkIndexList, int networkIndexListLen, char **uuids, int uuidsLen, int minCount, int maxCount, char *accountId, char *ownerId, char *reservationId, virtualMachine *ccvm, char *keyName, int vlan, char *userData, char *launchIndex, char *platform, int expiryTime, char *targetNode, ccInstance **outInsts, int *outInstsLen);
int doGetConsoleOutput(ncMetadata *meta, char *instId, char **consoleOutput);
int doRebootInstances(ncMetadata *meta, char **instIds, int instIdsLen);
int doTerminateInstances(ncMetadata *meta, char **instIds, int instIdsLen, int force, int **outStatus);

int doRegisterImage(ncMetadata *meta, char *amiId, char *location);
int doDescribeResources(ncMetadata *ccMeta, virtualMachine **ccvms, int vmLen, int **outTypesMax, int **outTypesAvail, int *outTypesLen, ccResource **outNodes, int *outNodesLen);
int doFlushNetwork(ncMetadata *ccMeta, char *accountId, char *destName);

int doCreateImage(ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev);

int schedule_instance(virtualMachine *vm, char *targetNode, int *outresid);
int schedule_instance_greedy(virtualMachine *vm, int *outresid);
int schedule_instance_roundrobin(virtualMachine *vm, int *outresid);
int schedule_instance_explicit(virtualMachine *vm, char *targetNode, int *outresid);

int add_instanceCache(char *instanceId, ccInstance *in);
int refresh_instanceCache(char *instanceId, ccInstance *in);
int del_instanceCacheId(char *instanceId);
int find_instanceCacheId(char *instanceId, ccInstance **out);
int find_instanceCacheIP(char *ip, ccInstance **out);
void print_instanceCache(void);
void invalidate_instanceCache(void);
void set_clean_instanceCache(void);
void set_dirty_instanceCache(void);
int is_clean_instanceCache(void);
int map_instanceCache(int (*match)(ccInstance *, void *), void *matchParam, int (*operate)(ccInstance *, void *), void *operateParam);
int privIpCmp(ccInstance *inst, void *ip);
int privIpSet(ccInstance *inst, void *ip);
int pubIpCmp(ccInstance *inst, void *ip);
int pubIpSet(ccInstance *inst, void *ip);
int free_instanceNetwork(char *mac, int vlan, int force, int dolock);
int ccInstance_to_ncInstance(ccInstance *dst, ncInstance *src);

int add_resourceCache(char *host, ccResource *in);
int refresh_resourceCache(char *host, ccResource *in);
int del_resourceCacheHostname(char *host);
int find_resourceCacheHostname(char *host, ccResource **out);
void print_resourceCache(void);
void invalidate_resourceCache(void);

int initialize(ncMetadata *ccMeta);
int init_thread(void);
int init_localstate(void);
int init_config(void);
int update_config(void);
int init_pthreads(void);
int setup_shared_buffer(void **buf, char *bufname, size_t bytes, sem_t **lock, char *lockname, int mode);
void unlock_exit(int);
void shawn(void);
int ncClientCall(ncMetadata *meta, int timeout, int ncLock, char *ncURL, char *ncOp, ...);
int ncGetTimeout(time_t op_start, time_t op_max, int numCalls, int idx);

int refresh_resources(ncMetadata *ccMeta, int timeout, int dolock);
int refresh_instances(ncMetadata *ccMeta, int timeout, int dolock);

int sem_mywait(int lockno);
int sem_mypost(int lockno);

int refreshNodes(ccConfig *config, ccResource **res, int *numHosts);

int syncNetworkState();
int restoreNetworkState();
int maintainNetworkState();
int checkActiveNetworks();
int reconfigureNetworkFromCLC();

int powerDown(ncMetadata *ccMeta, ccResource *node);
int powerUp(ccResource *node);
int changeState(ccResource *in, int newstate);

int ccIsEnabled(void);
int ccIsDisabled(void);
int ccChangeState(int newstate);
int ccCheckState(int clcTimer);
int ccGetStateString(char *outstr, int n);

int readConfigFile(char configFiles[][MAX_PATH], int numFiles);
char *configFileValue(char *key);

void *monitor_thread(void *);

int image_cache(char *id, char *url);
int image_cache_proxykick(ccResource *res, int *numHosts);
int image_cache_invalidate(void);

#endif


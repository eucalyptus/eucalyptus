#ifndef INCLUDE_HANDLERS_H
#define INCLUDE_HANDLERS_H

#include <eucalyptus.h>
#include <semaphore.h>
#include <data.h>
#include <client-marshal.h>
#include <vnetwork.h>

#define OP_TIMEOUT 60
enum {SHARED_MEM, SHARED_FILE};


typedef struct virtualMachine_t {
  int mem, cores, disk;
  char name[64];
} virtualMachine;
int allocate_virtualMachine(virtualMachine *out, int mem, int disk, int cores, char *name);
//void free_virtualMachine(virtualMachine *in);

typedef struct netConfig_t {
  int vlan;
  char publicMac[24], privateMac[24], publicIp[24], privateIp[24];
} netConfig;
int allocate_netConfig(netConfig *out, char *pvMac, char *pbMac, char *pvIp, char *pbIp, int vlan);
//void free_netConfig(netConfig *in);

typedef struct instance_t {
  char instanceId[16];
  char reservationId[16];
  
  char amiId[16];
  char kernelId[16];
  char ramdiskId[16];
  
  char amiURL[64];
  char kernelURL[64];
  char ramdiskURL[64];
  
  char state[16];
  time_t ts;
  
  char ownerId[16];
  char keyName[1024];
  
  netConfig ccnet;
  virtualMachine ccvm;

  int ncHostIdx;
  char serviceTag[64];

  char userData[64];
  char launchIndex[64];
  char groupNames[64][32];

  ncVolume volumes[EUCA_MAX_VOLUMES];
  int volumesSize;
} ccInstance;

int allocate_ccInstance(ccInstance *out, char *id, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char *ownerId, char *state, time_t ts, char *reservationId, netConfig *ccnet, virtualMachine *ccvm, int ncHostIdx, char *keyName, char *serviceTag, char *userData, char *launchIndex, char groupNames[][32], ncVolume *volumes, int volumesSize);
void print_ccInstance(ccInstance *in);
//void free_ccInstance(ccInstance *inInst);

enum {RESDOWN, RESUP, RESASLEEP, RESWAKING};

typedef struct resource_t {
  char ncURL[128];
  char ncService[128];
  int ncPort;
  char hostname[128], mac[24], ip[24];
  int maxMemory, availMemory, maxDisk, availDisk, maxCores, availCores;
  // state information
  int state, lastState;
  time_t stateChange, idleStart;
} resource;

typedef struct ccConfig_t {
  resource resourcePool[MAXNODES];
  char eucahome[1024];
  int numResources;
  int lastResourceUpdate;
  int use_wssec;
  char policyFile[1024];
  int instanceCacheUpdate;
  int initialized;
  int schedPolicy, schedState;
  int idleThresh, wakeThresh;
  time_t configMtime;
} ccConfig;

enum {SCHEDGREEDY, SCHEDROUNDROBIN, SCHEDPOWERSAVE};


int doStartNetwork(ncMetadata *ccMeta, char *netName, int vlan);
int doConfigureNetwork(ncMetadata *meta, char *type, int namedLen, char **sourceNames, char **userNames, int netLen, char **sourceNets, char *destName, char *protocol, int minPort, int maxPort);
int doStopNetwork(ncMetadata *ccMeta, char *netName, int vlan);

int doAttachVolume(ncMetadata *ccMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev);
int doDetachVolume(ncMetadata *ccMeta, char *volumeId, char *instanceId, char *remoteDev, char *localDev, int force);

int doAssignAddress(ncMetadata *ccMeta, char *src, char *dst);
int doUnassignAddress(ncMetadata *ccMeta, char *src, char *dst);
int doDescribePublicAddresses(ncMetadata *ccMeta, publicip **outAddresses, int *outAddressesLen);

int doDescribeInstances(ncMetadata *meta, char **instIds, int instIdsLen, ccInstance **outInsts, int *outInstsLen);
int doRunInstances(ncMetadata *ccMeta, char *amiId, char *kernelId, char *ramdiskId, char *amiURL, char *kernelURL, char *ramdiskURL, char **instIds, int instIdsLen, char **netNames, int netNamesLen, char **macAddrs, int macAddrsLen, int minCount, int maxCount, char *ownerId, char *reservationId, virtualMachine *ccvm, char *keyName, int vlan, char *userData, char *launchIndex, char *targetNode, ccInstance **outInsts, int *outInstsLen);
int doGetConsoleOutput(ncMetadata *meta, char *instId, char **consoleOutput);
int doRebootInstances(ncMetadata *meta, char **instIds, int instIdsLen);
int doTerminateInstances(ncMetadata *meta, char **instIds, int instIdsLen, int **outStatus);

int doRegisterImage(ncMetadata *meta, char *amiId, char *location);
int doDescribeResources(ncMetadata *ccMeta, virtualMachine **ccvms, int vmLen, int **outTypesMax, int **outTypesAvail, int *outTypesLen, char ***outServiceTags, int *outServiceTagsLen);
int doFlushNetwork(ncMetadata *ccMeta, char *destName);

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
int ccInstance_to_ncInstance(ccInstance *dst, ncInstance *src);

int initialize(void);
int init_thread(void);
int init_localstate(void);
int init_config(void);
int setup_shared_buffer(void **buf, char *bufname, size_t bytes, sem_t **lock, char *lockname, int mode);
int refresh_resources(ncMetadata *ccMeta, int timeout);
void shawn(void);
int sem_timewait(sem_t *sem, time_t seconds);
int sem_timepost(sem_t *sem);
int timeread(int fd, void *buf, size_t bytes, int timeout);
int refreshNodes(ccConfig *config, char *configFile, resource **res, int *numHosts);
int restoreNetworkState();
int powerDown(ncMetadata *ccMeta, resource *node);
int powerUp(resource *node);
int changeState(resource *in, int newstate);

#endif


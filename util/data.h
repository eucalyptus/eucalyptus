#ifndef INCLUDE_DATA_H
#define INCLUDE_DATA_H

#include <pthread.h>
#include "eucalyptus.h"

#define CHAR_BUFFER_SIZE 512

typedef struct ncMetadata_t {
    char *correlationId;
    char *userId;
} ncMetadata;

typedef struct ncInstParams_t {
  int memorySize;
  int diskSize;
  int numberOfCores;
} ncInstParams;

typedef struct ncNetConf_t {
  int vlan;
  char publicMac[32], privateMac[32], publicIp[32], privateIp[32];
} ncNetConf;

typedef struct ncVolume_t {
    char volumeId[CHAR_BUFFER_SIZE];
    char remoteDev[CHAR_BUFFER_SIZE];
    char localDev[CHAR_BUFFER_SIZE];
    char stateName[CHAR_BUFFER_SIZE];
} ncVolume;

typedef struct ncInstance_t {
    char instanceId[CHAR_BUFFER_SIZE];
    char imageId[CHAR_BUFFER_SIZE];
    char imageURL[CHAR_BUFFER_SIZE];
    char kernelId[CHAR_BUFFER_SIZE];
    char kernelURL[CHAR_BUFFER_SIZE];
    char ramdiskId[CHAR_BUFFER_SIZE];
    char ramdiskURL[CHAR_BUFFER_SIZE];
    char reservationId[CHAR_BUFFER_SIZE];
    char userId[CHAR_BUFFER_SIZE];
    
    /* state as reported to CC & CLC */
    char stateName[CHAR_BUFFER_SIZE];  /* as string */
    int stateCode; /* as int */
    /* state as NC thinks of it */
    int state;
    
    char keyName[CHAR_BUFFER_SIZE*4];
    char privateDnsName[CHAR_BUFFER_SIZE];
    char dnsName[CHAR_BUFFER_SIZE];
    int launchTime;
    int terminationTime;
    
    ncInstParams params;
    ncNetConf ncnet;
    pthread_t tcb;

    /* passed into NC via runInstances for safekeeping */
    char userData[CHAR_BUFFER_SIZE*10];
    char launchIndex[CHAR_BUFFER_SIZE];
    char groupNames[EUCA_MAX_GROUPS][CHAR_BUFFER_SIZE];
    int groupNamesSize;

    /* updated by NC upon Attach/DetachVolume */
    ncVolume volumes[EUCA_MAX_VOLUMES];
    int volumesSize;
} ncInstance;

typedef struct ncResource_t {
    char nodeStatus[CHAR_BUFFER_SIZE];
    int memorySizeMax;
    int memorySizeAvailable;
    int diskSizeMax;
    int diskSizeAvailable;
    int numberOfCoresMax;
    int numberOfCoresAvailable;
    char publicSubnets[CHAR_BUFFER_SIZE];
} ncResource;

/* TODO: make this into something smarter than a linked list */
typedef struct bunchOfInstances_t {
    ncInstance * instance;
    int count; /* only valid on first node */
    struct bunchOfInstances_t * next;
} bunchOfInstances;

typedef enum instance_error_codes_t {
    OUT_OF_MEMORY = 99,
    DUPLICATE,
    NOT_FOUND,
} instance_error_codes;

instance_error_codes add_instance (bunchOfInstances ** head, ncInstance * instance);
instance_error_codes remove_instance (bunchOfInstances ** head, ncInstance * instance);
instance_error_codes for_each_instance (bunchOfInstances ** headp, void (* function)(bunchOfInstances **, ncInstance *, void *), void *);
ncInstance * find_instance (bunchOfInstances **headp, char * instanceId);
ncInstance * get_instance (bunchOfInstances **head);
int total_instances (bunchOfInstances **head);

ncMetadata * allocate_metadata(char *correlationId, char *userId);
void free_metadata(ncMetadata ** meta);

ncInstance * allocate_instance(char *instanceId, char *reservationId, 
                               ncInstParams *params, 
                               char *imageId, char *imageURL, 
                               char *kernelId, char *kernelURL, 
                               char *ramdiskId, char *ramdiskURL, 
                               char *stateName, int stateCode, char *userId, 
                               ncNetConf *ncnet, char *keyName,
                               char *userData, char *launchIndex, char **groupNames, int groupNamesSize);
void free_instance (ncInstance ** inst);

ncResource * allocate_resource(char *nodeStatus, 
                               int memorySizeMax, int memorySizeAvailable, 
                               int diskSizeMax, int diskSizeAvailable,
                               int numberOfCoresMax, int numberOfCoresAvailable,
                               char *publicSubnets);
void free_resource(ncResource ** res);
ncVolume * find_volume (ncInstance * instance, char *volumeId);
ncVolume *  add_volume (ncInstance * instance, char *volumeId, char *remoteDev, char *localDev);
ncVolume * free_volume (ncInstance * instance, char *volumeId, char *remoteDev, char *localDev);

#endif


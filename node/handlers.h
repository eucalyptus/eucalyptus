#ifndef INCLUDE_HANDLERS_H
#define INCLUDE_HANDLERS_H

#include "data.h"

struct handlers {
    char name [CHAR_BUFFER_SIZE];
    int (*doInitialize) (void);
    int (*doPowerDown) (ncMetadata *meta);
    int (*doDescribeInstances) (ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen);
    int (*doRunInstance)       (ncMetadata *meta, char *instanceId, char *reservationId, ncInstParams *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, char *privMac, char *pubMac, int vlan, char *userData, char *launchIndex, char **groupNames, int groupNamesSize, ncInstance **outInst);
    int (*doTerminateInstance) (ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState);
    int (*doRebootInstance)    (ncMetadata *meta, char *instanceId);
    int (*doGetConsoleOutput)  (ncMetadata *meta, char *instanceId, char **consoleOutput);
    int (*doDescribeResource)  (ncMetadata *meta, char *resourceType, ncResource **outRes);
    int (*doStartNetwork)      (ncMetadata *ccMeta, char **remoteHosts, int remoteHostsLen, int port, int vlan);
    int (*doAttachVolume)      (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev);
    int (*doDetachVolume)      (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force);
};

#ifdef HANDLERS_FANOUT // only declare for the fanout code, not the actual handlers
int doPowerDown (ncMetadata *meta);
int doDescribeInstances (ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen);
int doRunInstance       (ncMetadata *meta, char *instanceId, char *reservationId, ncInstParams *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, char *privMac, char *pubMac, int vlan, char *userData, char *launchIndex, char **groupNames, int groupNamesSize, ncInstance **outInst);
int doTerminateInstance (ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState);
int doRebootInstance(ncMetadata *meta, char *instanceId);
int doGetConsoleOutput(ncMetadata *meta, char *instanceId, char **consoleOutput);
int doDescribeResource  (ncMetadata *meta, char *resourceType, ncResource **outRes);
int doStartNetwork(ncMetadata *ccMeta, char **remoteHosts, int remoteHostsLen, int port, int vlan);
int doAttachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev);
int doDetachVolume (ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force);
#endif /* HANDLERS_FANOUT */

#endif /* INCLUDE */


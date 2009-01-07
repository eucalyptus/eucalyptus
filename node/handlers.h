#ifndef INCLUDE_HANDLERS_H
#define INCLUDE_HANDLERS_H

#include "data.h"

int doDescribeInstances (ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen);
int doRunInstance       (ncMetadata *meta, char *instanceId, char *reservationId, ncInstParams *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, char *privMac, char *pubMac, int vlan, ncInstance **outInst);
int doTerminateInstance (ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState);
int doRebootInstance(ncMetadata *meta, char *instanceId);
int doncGetConsoleOutput(ncMetadata *meta, char *instanceId, char **consoleOutput);

int doDescribeResource  (ncMetadata *meta, char *resourceType, ncResource **outRes);
int doncStartNetwork(ncMetadata *ccMeta, char **remoteHosts, int remoteHostsLen, int port, int vlan);


int init_config();

#endif 


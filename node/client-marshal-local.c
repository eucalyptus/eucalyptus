#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define HANDLERS_FANOUT
#include "handlers.h"
#include "client-marshal.h"

ncStub * ncStubCreate (char *endpoint_uri, char *logfile, char *homedir) 
{
    ncStub * st;
    
    if ((st = malloc (sizeof(ncStub))) != NULL) {
        /* nothing to do */
    } 
    
    return (st);
}

int ncStubDestroy (ncStub * st)
{
    free (st);
    return (0);
}

/************************** stubs **************************/

int ncRunInstanceStub (ncStub *st, ncMetadata *meta, char *instanceId, char *reservationId, ncInstParams *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, char *privMac, char *pubMac, int vlan, char *userData, char *launchIndex, char **groupNames, int groupNamesSize, ncInstance **outInstPtr)
{
    return doRunInstance (meta, instanceId, reservationId, params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, privMac, pubMac, vlan, userData, launchIndex, groupNames, groupNamesSize, outInstPtr);
}

int ncTerminateInstanceStub (ncStub *st, ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState)
{
    return doTerminateInstance (meta, instanceId, shutdownState, previousState);
}
int ncPowerDownStub (ncStub *st, ncMetadata *meta){
  return(0);
}
int ncDescribeInstancesStub (ncStub *st, ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
    return doDescribeInstances (meta, instIds, instIdsLen, outInsts, outInstsLen);
}

int ncDescribeResourceStub (ncStub *st, ncMetadata *meta, char *resourceType, ncResource **outRes)
{
    return doDescribeResource (meta, resourceType, outRes);
}

int ncAttachVolumeStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev)
{
    return doAttachVolume (meta, instanceId, volumeId, remoteDev, localDev);
}

int ncDetachVolumeStub (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
    return doDetachVolume (meta, instanceId, volumeId, remoteDev, localDev, force);
}

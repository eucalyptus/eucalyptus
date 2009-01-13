#ifndef CLIENT_MARSHAL_H
#define CLIENT_MARSHAL_H

#include "axis2_stub_EucalyptusNC.h" /* for axis2_ and axutil_ defs */
#include "data.h" /* for eucalyptus defs */

typedef struct ncStub_t {
  axutil_env_t * env;
  axis2_char_t * client_home;
  axis2_char_t * endpoint_uri;
  axis2_stub_t * stub;
} ncStub;

ncStub * ncStubCreate  (char *endpoint, char *logfile, char *homedir);
int      ncStubDestroy (ncStub * stub);

int ncRunInstanceStub (ncStub *st, ncMetadata *meta, char *instanceId, char *reservationId, ncInstParams *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, char *privMac, char *pubMac, int vlan, char *userData, char *launchIndex, char **groupNames, int groupNamesSize, ncInstance **outInstPtr);
int ncGetConsoleOutputStub (ncStub *stub, ncMetadata *meta, char *instanceId, char **consoleOutput);
int ncRebootInstanceStub (ncStub *stub, ncMetadata *meta, char *instanceId);
int ncTerminateInstanceStub (ncStub *stub, ncMetadata *meta, char *instanceId, int *shutdownState, int *previousState);
int ncDescribeInstancesStub (ncStub *stub, ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen);
int ncDescribeResourceStub  (ncStub *stub, ncMetadata *meta, char *resourceType, ncResource **outRes);
int ncStartNetworkStub  (ncStub *stub, ncMetadata *meta, char **peers, int peersLen, int port, int vlan, char **outStatus);
int ncAttachVolume (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev);
int ncDetachVolume (ncStub *stub, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force);

#endif

#ifndef INCLUDE_CC_CLIENT_MARSHAL_H
#define INCLUDE_CC_CLIENT_MARSHAL_H

#include <stdio.h>
#include <time.h>
#include <misc.h>
#include "axis2_stub_EucalyptusCC.h"

int cc_registerImage(char *imageloc, axutil_env_t *, axis2_stub_t *);
int cc_describeResources(axutil_env_t *, axis2_stub_t *);
int cc_startNetwork(int, char *netName, char **ccs, int ccsLen, axutil_env_t *, axis2_stub_t *);
int cc_stopNetwork(int, char *netName, axutil_env_t *, axis2_stub_t *);
int cc_assignAddress(char *src, char *dst, axutil_env_t *, axis2_stub_t *);
int cc_unassignAddress(char *src, char *dst, axutil_env_t *, axis2_stub_t *);

int cc_attachVolume(char *volumeId, char *instanceId, char *remoteDev, char *localDev, axutil_env_t *env, axis2_stub_t *stub);
int cc_detachVolume(char *volumeId, char *instanceId, char *remoteDev, char *localDev, int force, axutil_env_t *env, axis2_stub_t *stub);

int cc_describePublicAddresses(axutil_env_t *, axis2_stub_t *);
int cc_configureNetwork(char *, char *, char *, int, int, char *, axutil_env_t *, axis2_stub_t *);
int cc_runInstances(char *amiId, char *amiURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, int num, int vlan, char *netName, axutil_env_t *, axis2_stub_t *);
int cc_describeInstances(char **instIds, int instIdsLen, axutil_env_t *, axis2_stub_t *);
int cc_getConsoleOutput(char *instId, axutil_env_t *, axis2_stub_t *);
int cc_rebootInstances(char **instIds, int instIdsLen, axutil_env_t *, axis2_stub_t *);
int cc_terminateInstances(char **instIds, int instIdsLen, axutil_env_t *, axis2_stub_t *);
int cc_killallInstances(axutil_env_t *, axis2_stub_t *);

#endif

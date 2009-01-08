#ifndef SERVER_MARSHAL_H
#define SERVER_MARSHAL_H

#include "axis2_skel_EucalyptusCC.h"
#include <handlers.h>

adb_RunInstancesResponse_t *RunInstancesMarshal(adb_RunInstances_t *runInstances, const axutil_env_t *env);
adb_DescribeInstancesResponse_t *DescribeInstancesMarshal(adb_DescribeInstances_t *describeInstances, const axutil_env_t *env);
adb_DescribeResourcesResponse_t *DescribeResourcesMarshal(adb_DescribeResources_t *describeResources, const axutil_env_t *env);
adb_GetConsoleOutputResponse_t* GetConsoleOutputMarshal (adb_GetConsoleOutput_t* getConsoleOutput, const axutil_env_t *env);
adb_RebootInstancesResponse_t* RebootInstancesMarshal (adb_RebootInstances_t* rebootInstances, const axutil_env_t *env);
adb_TerminateInstancesResponse_t *TerminateInstancesMarshal(adb_TerminateInstances_t *terminateInstances, const axutil_env_t *env);
//adb_RegisterImageResponse_t *RegisterImageMarshal(adb_RegisterImage_t *registerImage, const axutil_env_t *env);

adb_StartNetworkResponse_t *StartNetworkMarshal(adb_StartNetwork_t *startNetwork, const axutil_env_t *env);
adb_StopNetworkResponse_t *StopNetworkMarshal(adb_StopNetwork_t *stopNetwork, const axutil_env_t *env);

adb_AssignAddressResponse_t *AssignAddressMarshal(adb_AssignAddress_t *assignAddress, const axutil_env_t *env);
adb_UnassignAddressResponse_t *UnassignAddressMarshal(adb_UnassignAddress_t *unassignAddress, const axutil_env_t *env);
adb_DescribePublicAddressesResponse_t *DescribePublicAddressesMarshal(adb_DescribePublicAddresses_t *describePublicAddresses, const axutil_env_t *env);
adb_ConfigureNetworkResponse_t *ConfigureNetworkMarshal(adb_ConfigureNetwork_t *configureNetwork, const axutil_env_t *env);

adb_AttachVolumeResponse_t *AttachVolumeMarshal(adb_AttachVolume_t *attachVolume, const axutil_env_t *env);
adb_DetachVolumeResponse_t *DetachVolumeMarshal(adb_DetachVolume_t *detachVolume, const axutil_env_t *env);

void print_adb_ccInstanceType(adb_ccInstanceType_t *in);
int ccInstanceUnmarshal(adb_ccInstanceType_t *dst, ccInstance *src, const axutil_env_t *env);
#endif

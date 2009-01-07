#ifndef SERVER_MARSHAL_H
#define SERVER_MARSHAL_H

#include "axis2_skel_EucalyptusNC.h"

adb_ncDescribeResourceResponse_t*  ncDescribeResourceMarshal  (adb_ncDescribeResource_t* ncDescribeResource, const axutil_env_t *env);
adb_ncRunInstanceResponse_t*       ncRunInstanceMarshal       (adb_ncRunInstance_t* ncRunInstance, const axutil_env_t *env);
adb_ncDescribeInstancesResponse_t* ncDescribeInstancesMarshal (adb_ncDescribeInstances_t* ncDescribeInstances, const axutil_env_t *env);
adb_ncTerminateInstanceResponse_t* ncTerminateInstanceMarshal (adb_ncTerminateInstance_t* ncTerminateInstance, const axutil_env_t *env);
adb_ncStartNetworkResponse_t* ncStartNetworkMarshal (adb_ncStartNetwork_t* ncStartNetwork, const axutil_env_t *env);
adb_ncRebootInstanceResponse_t* ncRebootInstanceMarshal (adb_ncRebootInstance_t* ncRebootInstance,  const axutil_env_t *env);
adb_ncGetConsoleOutputResponse_t* ncGetConsoleOutputMarshal (adb_ncGetConsoleOutput_t* ncGetConsoleOutput, const axutil_env_t *env);

#endif

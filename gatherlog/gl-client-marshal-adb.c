#include <stdio.h>
#include <stdlib.h>
#include <gl-client-marshal.h>
#include <euca_auth.h>

int gl_getLogs(char *service, char **outCClog, char **outNClog, char **outHlog, char **outAlog, axutil_env_t *env, axis2_stub_t *stub) {
  char *outservice;
  
  adb_GetLogsResponse_t *out;
  adb_getLogsResponseType_t *response;

  adb_GetLogs_t *in;
  adb_getLogsType_t *request;

  request = adb_getLogsType_create(env);
  adb_getLogsType_set_userId(request, env, "eucalyptus");
  adb_getLogsType_set_correlationId(request, env, "12345678");
  adb_getLogsType_set_serviceTag(request, env, service);

  in = adb_GetLogs_create(env);
  adb_GetLogs_set_GetLogs(in, env, request);

  out =  axis2_stub_op_EucalyptusGL_GetLogs(stub, env, in);
  if (!out) {
    printf("ERROR: operation call failed\n");
    return(1);
  }
  response = adb_GetLogsResponse_get_GetLogsResponse(out, env);

  outservice = adb_getLogsResponseType_get_serviceTag(response, env);
  *outCClog = adb_getLogsResponseType_get_CCLog(response, env);
  *outNClog = adb_getLogsResponseType_get_NCLog(response, env);
  *outHlog = adb_getLogsResponseType_get_httpdLog(response, env);
  *outAlog = adb_getLogsResponseType_get_axis2Log(response, env);

  return(0);
}

int gl_getKeys(char *service, char **outCCCert, char **outNCCert, axutil_env_t *env, axis2_stub_t *stub) {
  char *outservice;
  
  adb_GetKeysResponse_t *out;
  adb_getKeysResponseType_t *response;
  
  adb_GetKeys_t *in;
  adb_getKeysType_t *request;
  
  request = adb_getKeysType_create(env);
  adb_getKeysType_set_userId(request, env, "eucalyptus");
  adb_getKeysType_set_correlationId(request, env, "12345678");
  adb_getKeysType_set_serviceTag(request, env, service);
  
  in = adb_GetKeys_create(env);
  adb_GetKeys_set_GetKeys(in, env, request);
  
  out =  axis2_stub_op_EucalyptusGL_GetKeys(stub, env, in);
  if (!out) {
    printf("ERROR: operation call failed\n");
    return(1);
  }
  response = adb_GetKeysResponse_get_GetKeysResponse(out, env);
  
  outservice = adb_getKeysResponseType_get_serviceTag(response, env);
  *outCCCert = adb_getKeysResponseType_get_CCcert(response, env);
  *outNCCert = adb_getKeysResponseType_get_NCcert(response, env);

  return(0);
}


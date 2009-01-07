#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <server-marshal.h>

adb_GetLogsResponse_t *GetLogsMarshal(adb_GetLogs_t *getLogs, const axutil_env_t *env) {
  adb_GetLogsResponse_t *ret=NULL;
  adb_getLogsResponseType_t *response=NULL;
  
  adb_getLogsType_t *request=NULL;

  int rc;
  axis2_bool_t status;
  char *userId, *correlationId, *service, statusMessage[256];
  char *outCCLog, *outNCLog, *outHTTPDLog, *outAxis2Log;

  request = adb_GetLogs_get_GetLogs(getLogs, env);
  
  userId = adb_getLogsType_get_userId(request, env);
  correlationId = adb_getLogsType_get_correlationId(request, env);
  service = adb_getLogsType_get_serviceTag(request, env);
  
  response = adb_getLogsResponseType_create(env);

  status = AXIS2_TRUE;
  rc = doGetLogs(service, &outCCLog, &outNCLog, &outHTTPDLog, &outAxis2Log);
  if (rc) {
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    
    if (outCCLog) adb_getLogsResponseType_set_CCLog(response, env, outCCLog);
    if (outNCLog) adb_getLogsResponseType_set_NCLog(response, env, outNCLog);
    if (outHTTPDLog) adb_getLogsResponseType_set_httpdLog(response, env, outHTTPDLog);
    if (outAxis2Log) adb_getLogsResponseType_set_axis2Log(response, env, outAxis2Log);
  }
  adb_getLogsResponseType_set_serviceTag(response, env, service);

  adb_getLogsResponseType_set_userId(response, env, userId);
  adb_getLogsResponseType_set_correlationId(response, env, correlationId);
  adb_getLogsResponseType_set_return(response, env, status);
  if (status == AXIS2_FALSE) {
    adb_getLogsResponseType_set_statusMessage(response, env, statusMessage);
  }

  ret = adb_GetLogsResponse_create(env);
  adb_GetLogsResponse_set_GetLogsResponse(ret, env, response);

  return(ret);
}

adb_GetKeysResponse_t *GetKeysMarshal(adb_GetKeys_t *getKeys, const axutil_env_t *env) {
  adb_GetKeysResponse_t *ret=NULL;
  adb_getKeysResponseType_t *response=NULL;
  
  adb_getKeysType_t *request=NULL;

  int rc;
  axis2_bool_t status;
  char *userId, *correlationId, *service, statusMessage[256];
  char *outCCCert, *outNCCert;
  
  request = adb_GetKeys_get_GetKeys(getKeys, env);
  
  userId = adb_getKeysType_get_userId(request, env);
  correlationId = adb_getKeysType_get_correlationId(request, env);
  service = adb_getKeysType_get_serviceTag(request, env);
  
  response = adb_getKeysResponseType_create(env);

  status = AXIS2_TRUE;
  rc = doGetKeys(service, &outCCCert, &outNCCert);
  if (rc) {
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    if (outCCCert) adb_getKeysResponseType_set_CCcert(response, env, outCCCert);
    if (outNCCert) adb_getKeysResponseType_set_NCcert(response, env, outNCCert);
  }
  
  adb_getKeysResponseType_set_userId(response, env, userId);
  adb_getKeysResponseType_set_correlationId(response, env, correlationId);
  adb_getKeysResponseType_set_return(response, env, status);
  adb_getKeysResponseType_set_serviceTag(response, env, service);

  if (status == AXIS2_FALSE) {
    adb_getKeysResponseType_set_statusMessage(response, env, statusMessage);
  }
  
  ret = adb_GetKeysResponse_create(env);
  adb_GetKeysResponse_set_GetKeysResponse(ret, env, response);
  
  return(ret);
}

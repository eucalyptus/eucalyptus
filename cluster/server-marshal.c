#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

#include <server-marshal.h>
#include <handlers.h>
#include <misc.h>
      
#define DONOTHING 0
#define EVENTLOG 0

adb_AttachVolumeResponse_t *AttachVolumeMarshal(adb_AttachVolume_t *attachVolume, const axutil_env_t *env) {
  adb_AttachVolumeResponse_t *ret=NULL;
  adb_attachVolumeResponseType_t *avrt=NULL;
  
  adb_attachVolumeType_t *avt=NULL;
  
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *volumeId, *instanceId, *remoteDev, *localDev, *cid;
  ncMetadata ccMeta;
  
  avt = adb_AttachVolume_get_AttachVolume(attachVolume, env);
  
  ccMeta.correlationId = adb_attachVolumeType_get_correlationId(avt, env);
  ccMeta.userId = adb_attachVolumeType_get_userId(avt, env);
  
  cid = adb_attachVolumeType_get_correlationId(avt, env);
  
  volumeId = adb_attachVolumeType_get_volumeId(avt, env);
  instanceId = adb_attachVolumeType_get_instanceId(avt, env);
  remoteDev = adb_attachVolumeType_get_remoteDev(avt, env);
  localDev = adb_attachVolumeType_get_localDev(avt, env);

  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doAttachVolume(&ccMeta, volumeId, instanceId, remoteDev, localDev);
    if (rc) {
      logprintf("ERROR: doAttachVolume() returned FAIL\n");
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }
  
  avrt = adb_attachVolumeResponseType_create(env);
  adb_attachVolumeResponseType_set_return(avrt, env, status);
  if (status == AXIS2_FALSE) {
    adb_attachVolumeResponseType_set_statusMessage(avrt, env, statusMessage);
  }

  adb_attachVolumeResponseType_set_correlationId(avrt, env, ccMeta.correlationId);
  adb_attachVolumeResponseType_set_userId(avrt, env, ccMeta.userId);
  
  ret = adb_AttachVolumeResponse_create(env);
  adb_AttachVolumeResponse_set_AttachVolumeResponse(ret, env, avrt);

  return(ret);
}

adb_DetachVolumeResponse_t *DetachVolumeMarshal(adb_DetachVolume_t *detachVolume, const axutil_env_t *env) {
  adb_DetachVolumeResponse_t *ret=NULL;
  adb_detachVolumeResponseType_t *dvrt=NULL;
  
  adb_detachVolumeType_t *dvt=NULL;
  
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *volumeId, *instanceId, *remoteDev, *localDev, *cid;
  int force;
  ncMetadata ccMeta;
  
  dvt = adb_DetachVolume_get_DetachVolume(detachVolume, env);
  
  ccMeta.correlationId = adb_detachVolumeType_get_correlationId(dvt, env);
  ccMeta.userId = adb_detachVolumeType_get_userId(dvt, env);
  
  cid = adb_detachVolumeType_get_correlationId(dvt, env);
  
  volumeId = adb_detachVolumeType_get_volumeId(dvt, env);
  instanceId = adb_detachVolumeType_get_instanceId(dvt, env);
  remoteDev = adb_detachVolumeType_get_remoteDev(dvt, env);
  localDev = adb_detachVolumeType_get_localDev(dvt, env);
  force = adb_detachVolumeType_get_force(dvt, env);

  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doDetachVolume(&ccMeta, volumeId, instanceId, remoteDev, localDev, force);
    if (rc) {
      logprintf("ERROR: doDetachVolume() returned FAIL\n");
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }
  
  dvrt = adb_detachVolumeResponseType_create(env);
  adb_detachVolumeResponseType_set_return(dvrt, env, status);
  if (status == AXIS2_FALSE) {
    adb_detachVolumeResponseType_set_statusMessage(dvrt, env, statusMessage);
  }

  adb_detachVolumeResponseType_set_correlationId(dvrt, env, ccMeta.correlationId);
  adb_detachVolumeResponseType_set_userId(dvrt, env, ccMeta.userId);
  
  ret = adb_DetachVolumeResponse_create(env);
  adb_DetachVolumeResponse_set_DetachVolumeResponse(ret, env, dvrt);

  return(ret);
}

adb_StopNetworkResponse_t *StopNetworkMarshal(adb_StopNetwork_t *stopNetwork, const axutil_env_t *env) {
  adb_StopNetworkResponse_t *ret=NULL;
  adb_stopNetworkResponseType_t *snrt=NULL;
  
  adb_stopNetworkType_t *snt=NULL;
  
  int rc, vlan;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *userName, *netName, *cid;
  ncMetadata ccMeta;
  
  snt = adb_StopNetwork_get_StopNetwork(stopNetwork, env);
  
  ccMeta.correlationId = adb_stopNetworkType_get_correlationId(snt, env);
  ccMeta.userId = adb_stopNetworkType_get_userId(snt, env);
  
  userName = adb_stopNetworkType_get_userId(snt, env);
  cid = adb_stopNetworkType_get_correlationId(snt, env);
  
  vlan = adb_stopNetworkType_get_vlan(snt, env);
  netName = adb_stopNetworkType_get_netName(snt, env);

  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doStopNetwork(&ccMeta, netName, vlan);
    if (rc) {
      logprintf("ERROR: doStopNetwork() returned FAIL\n");
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }
  
  snrt = adb_stopNetworkResponseType_create(env);
  
  adb_stopNetworkResponseType_set_correlationId(snrt, env, ccMeta.correlationId);
  adb_stopNetworkResponseType_set_userId(snrt, env, ccMeta.userId);
  adb_stopNetworkResponseType_set_return(snrt, env, status);
  if (status == AXIS2_FALSE) {
    adb_stopNetworkResponseType_set_statusMessage(snrt, env, statusMessage);
  }

  ret = adb_StopNetworkResponse_create(env);
  adb_StopNetworkResponse_set_StopNetworkResponse(ret, env, snrt);
  return(ret);
}

adb_DescribeNetworksResponse_t *DescribeNetworksMarshal(adb_DescribeNetworks_t *describeNetworks, const axutil_env_t *env) {
  // output vars
  adb_DescribeNetworksResponse_t *ret=NULL;
  adb_describeNetworksResponseType_t *snrt=NULL;
  
  //input vars
  adb_describeNetworksType_t *snt=NULL;

  // working vars
  int rc, i;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];

  char **clusterControllers;
  int clusterControllersLen=0;
  ncMetadata ccMeta;
  
  snt = adb_DescribeNetworks_get_DescribeNetworks(describeNetworks, env);
  ccMeta.correlationId = adb_describeNetworksType_get_correlationId(snt, env);
  ccMeta.userId = adb_describeNetworksType_get_userId(snt, env);
  
  clusterControllersLen = adb_describeNetworksType_sizeof_clusterControllers(snt, env);
  clusterControllers = malloc(sizeof(char *) * clusterControllersLen);
  for (i=0; i<clusterControllersLen; i++) {
    clusterControllers[i] = adb_describeNetworksType_get_clusterControllers_at(snt, env, i);
  }
  
  snrt = adb_describeNetworksResponseType_create(env);
  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doDescribeNetworks(&ccMeta, clusterControllers, clusterControllersLen);
    if (rc) {
      logprintf("ERROR: doDescribeNetworks() returned fail %d\n", rc);
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }
  if (clusterControllers) free(clusterControllers);
  
  adb_describeNetworksResponseType_set_return(snrt, env, status);
  if (status == AXIS2_FALSE) {
    adb_describeNetworksResponseType_set_statusMessage(snrt, env, statusMessage);
  }
  
  adb_describeNetworksResponseType_set_correlationId(snrt, env, ccMeta.correlationId);
  adb_describeNetworksResponseType_set_userId(snrt, env, ccMeta.userId);
  
  ret = adb_DescribeNetworksResponse_create(env);
  adb_DescribeNetworksResponse_set_DescribeNetworksResponse(ret, env, snrt);
  
  return(ret);
}
adb_DescribePublicAddressesResponse_t *DescribePublicAddressesMarshal(adb_DescribePublicAddresses_t *describePublicAddresses, const axutil_env_t *env) {
  adb_describePublicAddressesType_t *dpa=NULL;

  adb_DescribePublicAddressesResponse_t *ret=NULL;
  adb_describePublicAddressesResponseType_t *dpart=NULL;

  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];

  int rc, outAddressesLen, i;
  ncMetadata ccMeta;
  //  char **outAddresses=NULL;
  publicip *outAddresses;

  dpa = adb_DescribePublicAddresses_get_DescribePublicAddresses(describePublicAddresses, env);

  ccMeta.correlationId = adb_describePublicAddressesType_get_correlationId(dpa, env);
  ccMeta.userId = adb_describePublicAddressesType_get_userId(dpa, env);

  if (!DONOTHING) {
    rc = doDescribePublicAddresses(&ccMeta, &outAddresses, &outAddressesLen);
  }
  
  if (rc == 2) {
    snprintf(statusMessage, 256, "NOTSUPPORTED");
    status = AXIS2_FALSE;
    outAddressesLen = 0;
  } else if (rc) {
    logprintf("ERROR: doDescribePublicAddresses() returned FAIL\n");
    status = AXIS2_FALSE;
    outAddressesLen = 0;
  } else {
    status = AXIS2_TRUE;
  }
  
  dpart = adb_describePublicAddressesResponseType_create(env);
  for (i=0; i<outAddressesLen; i++) {
    if (outAddresses[i].ip) {
      adb_describePublicAddressesResponseType_add_sourceAddresses(dpart, env, hex2dot(outAddresses[i].ip));
      if (outAddresses[i].dstip) {
	adb_describePublicAddressesResponseType_add_destAddresses(dpart, env, hex2dot(outAddresses[i].dstip));
      } else {
	adb_describePublicAddressesResponseType_add_destAddresses(dpart, env, "");
      }
    }
  }
  
  adb_describePublicAddressesResponseType_set_correlationId(dpart, env, ccMeta.correlationId);
  adb_describePublicAddressesResponseType_set_userId(dpart, env, ccMeta.userId);
  adb_describePublicAddressesResponseType_set_return(dpart, env, status);
  if (status == AXIS2_FALSE) {
    adb_describePublicAddressesResponseType_set_statusMessage(dpart, env, statusMessage);
  }
  
  ret = adb_DescribePublicAddressesResponse_create(env);
  adb_DescribePublicAddressesResponse_set_DescribePublicAddressesResponse(ret, env, dpart);
  return(ret);
}

adb_AssignAddressResponse_t *AssignAddressMarshal(adb_AssignAddress_t *assignAddress, const axutil_env_t *env) {
  adb_AssignAddressResponse_t *ret=NULL;
  adb_assignAddressResponseType_t *aart=NULL;
  
  adb_assignAddressType_t *aat=NULL;
  
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *src, *dst, *cid;
  ncMetadata ccMeta;
  
  aat = adb_AssignAddress_get_AssignAddress(assignAddress, env);
  
  ccMeta.correlationId = adb_assignAddressType_get_correlationId(aat, env);
  ccMeta.userId = adb_assignAddressType_get_userId(aat, env);
  
  cid = adb_assignAddressType_get_correlationId(aat, env);
  
  src = adb_assignAddressType_get_source(aat, env);
  dst = adb_assignAddressType_get_dest(aat, env);

  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doAssignAddress(&ccMeta, src, dst);
    if (rc) {
      logprintf("ERROR: doAssignAddress() returned FAIL\n");
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }
  
  aart = adb_assignAddressResponseType_create(env);
  adb_assignAddressResponseType_set_return(aart, env, status);
  if (status == AXIS2_FALSE) {
    adb_assignAddressResponseType_set_statusMessage(aart, env, statusMessage);
  }

  adb_assignAddressResponseType_set_correlationId(aart, env, ccMeta.correlationId);
  adb_assignAddressResponseType_set_userId(aart, env, ccMeta.userId);
  
  ret = adb_AssignAddressResponse_create(env);
  adb_AssignAddressResponse_set_AssignAddressResponse(ret, env, aart);

  return(ret);
}

adb_UnassignAddressResponse_t *UnassignAddressMarshal(adb_UnassignAddress_t *unassignAddress, const axutil_env_t *env) {
  adb_UnassignAddressResponse_t *ret=NULL;
  adb_unassignAddressResponseType_t *uart=NULL;
  
  adb_unassignAddressType_t *uat=NULL;
  
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *src, *dst;
  ncMetadata ccMeta;
  
  uat = adb_UnassignAddress_get_UnassignAddress(unassignAddress, env);
  
  ccMeta.correlationId = adb_unassignAddressType_get_correlationId(uat, env);
  ccMeta.userId = adb_unassignAddressType_get_userId(uat, env);
  
  src = adb_unassignAddressType_get_source(uat, env);
  dst = adb_unassignAddressType_get_dest(uat, env);
  
  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doUnassignAddress(&ccMeta, src, dst);
    if (rc) {
      logprintf("ERROR: doUnassignAddress() returned FAIL\n");
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }

  uart = adb_unassignAddressResponseType_create(env);
  adb_unassignAddressResponseType_set_return(uart, env, status);
  if (status == AXIS2_FALSE) {
    adb_unassignAddressResponseType_set_statusMessage(uart, env, statusMessage);
  }
    
  adb_unassignAddressResponseType_set_correlationId(uart, env, ccMeta.correlationId);
  adb_unassignAddressResponseType_set_userId(uart, env, ccMeta.userId);

  ret = adb_UnassignAddressResponse_create(env);
  adb_UnassignAddressResponse_set_UnassignAddressResponse(ret, env, uart);

  return(ret);
}

adb_ConfigureNetworkResponse_t *ConfigureNetworkMarshal(adb_ConfigureNetwork_t *configureNetwork, const axutil_env_t *env) {
  adb_ConfigureNetworkResponse_t *ret=NULL;
  adb_configureNetworkResponseType_t *cnrt=NULL;

  adb_configureNetworkType_t *cnt=NULL;
  adb_networkRule_t *nr=NULL;

  // working vars
  int rc, i, ruleLen, j, done;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];

  char **sourceNets, **userNames, **sourceNames, *cid, *protocol, *user, *destName, *type, *destNameLast;
  int minPort, maxPort, namedLen, netLen;
  ncMetadata ccMeta;
  
  cnt = adb_ConfigureNetwork_get_ConfigureNetwork(configureNetwork, env);
  ccMeta.correlationId = adb_configureNetworkType_get_correlationId(cnt, env);
  ccMeta.userId = adb_configureNetworkType_get_userId(cnt, env);

  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "configureNetwork", "begin");
  
  user = adb_configureNetworkType_get_userId(cnt, env);
  cid = adb_configureNetworkType_get_correlationId(cnt, env);
  
  ruleLen = adb_configureNetworkType_sizeof_rules(cnt, env);
  done=0;
  destNameLast = strdup("EUCAFIRST");

  for (j=0; j<ruleLen && !done; j++) {
    nr = adb_configureNetworkType_get_rules_at(cnt, env, j);

    type = adb_networkRule_get_type(nr, env);
    destName = adb_networkRule_get_destName(nr, env);
    protocol = adb_networkRule_get_protocol(nr, env);
    minPort = adb_networkRule_get_portRangeMin(nr, env);
    maxPort = adb_networkRule_get_portRangeMax(nr, env);
  
    if (strcmp(destName, destNameLast)) {
      doFlushNetwork(&ccMeta, destName);
    }
    if (destNameLast) free(destNameLast);
    destNameLast = strdup(destName);

    userNames = NULL;
    namedLen = adb_networkRule_sizeof_userNames(nr, env);
    if (namedLen) {
      userNames = malloc(sizeof(char *) * namedLen);
    }

    sourceNames=NULL;
    namedLen = adb_networkRule_sizeof_sourceNames(nr, env);
    if (namedLen) {
      sourceNames = malloc(sizeof(char *) * namedLen);
    }        
    
    sourceNets=NULL;
    netLen = adb_networkRule_sizeof_sourceNets(nr, env);
    if (netLen) {
      sourceNets = malloc(sizeof(char *) * netLen);
    }
    
    for (i=0; i<namedLen; i++) {
      if (userNames) {
	userNames[i] = adb_networkRule_get_userNames_at(nr, env, i);
      }
      if (sourceNames) {
	sourceNames[i] = adb_networkRule_get_sourceNames_at(nr, env, i);
      }
    }

    for (i=0; i<netLen; i++) {
      if (sourceNets) {
	sourceNets[i] = adb_networkRule_get_sourceNets_at(nr, env, i);
      }
    }
    
    cnrt = adb_configureNetworkResponseType_create(env);
    
    rc=1;
    if (!DONOTHING) {
      rc = doConfigureNetwork(&ccMeta, type, namedLen, sourceNames, userNames, netLen, sourceNets, destName, protocol, minPort, maxPort);
    }
    
    if (userNames) free(userNames);
    if (sourceNames) free(sourceNames);
    if (sourceNets) free(sourceNets);

    if (rc) {
      done++;
    }
  }

  if (done) {
    logprintf("ERROR: doConfigureNetwork() returned fail %d\n", rc);
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    status = AXIS2_TRUE;
  }
  
  adb_configureNetworkResponseType_set_correlationId(cnrt, env, cid);
  adb_configureNetworkResponseType_set_userId(cnrt, env, user);
  adb_configureNetworkResponseType_set_return(cnrt, env, status);
  if (status == AXIS2_FALSE) {
    adb_configureNetworkResponseType_set_statusMessage(cnrt, env, statusMessage);
  }
  
  ret = adb_ConfigureNetworkResponse_create(env);
  adb_ConfigureNetworkResponse_set_ConfigureNetworkResponse(ret, env, cnrt);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "configureNetwork", "end");
  return(ret);
}

adb_GetConsoleOutputResponse_t* GetConsoleOutputMarshal (adb_GetConsoleOutput_t* getConsoleOutput, const axutil_env_t *env) {
  // output vars
  adb_GetConsoleOutputResponse_t *ret=NULL;
  adb_getConsoleOutputResponseType_t *gcort=NULL;
  
  //input vars
  adb_getConsoleOutputType_t *gcot=NULL;

  // working vars
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *instId, *output=NULL;
  ncMetadata ccMeta;
  
  gcot = adb_GetConsoleOutput_get_GetConsoleOutput(getConsoleOutput, env);
  ccMeta.correlationId = adb_getConsoleOutputType_get_correlationId(gcot, env);
  ccMeta.userId = adb_getConsoleOutputType_get_userId(gcot, env);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "getConsoleOutput", "begin");
  
  instId = adb_getConsoleOutputType_get_instanceId(gcot, env);
  
  gcort = adb_getConsoleOutputResponseType_create(env);
  
  status = AXIS2_TRUE;
  output=NULL;
  if (!DONOTHING) {
    rc = doGetConsoleOutput(&ccMeta, instId, &output);
    if (rc) {
      logprintf("ERROR: doGetConsoleOutput() returned fail %d\n", rc);
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    } else {
      if (output) {
	adb_getConsoleOutputResponseType_set_consoleOutput(gcort, env, output);
      }
    }
  }
  if (output) free(output);
  
  adb_getConsoleOutputResponseType_set_correlationId(gcort, env, ccMeta.correlationId);
  adb_getConsoleOutputResponseType_set_userId(gcort, env, ccMeta.userId);
  adb_getConsoleOutputResponseType_set_return(gcort, env, status);
  if (status == AXIS2_FALSE) {
    adb_getConsoleOutputResponseType_set_statusMessage(gcort, env, statusMessage);
  }
  
  ret = adb_GetConsoleOutputResponse_create(env);
  adb_GetConsoleOutputResponse_set_GetConsoleOutputResponse(ret, env, gcort);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "getConsoleOutput", "end");
  
  return(ret);
}

adb_StartNetworkResponse_t *StartNetworkMarshal(adb_StartNetwork_t *startNetwork, const axutil_env_t *env) {  
  // output vars
  adb_StartNetworkResponse_t *ret=NULL;
  adb_startNetworkResponseType_t *snrt=NULL;

  //input vars
  adb_startNetworkType_t *snt=NULL;

  // working vars
  int rc, i;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];

  char *netName, **clusterControllers;
  int vlan, clusterControllersLen=0;
  ncMetadata ccMeta;
  
  snt = adb_StartNetwork_get_StartNetwork(startNetwork, env);
  ccMeta.correlationId = adb_startNetworkType_get_correlationId(snt, env);
  ccMeta.userId = adb_startNetworkType_get_userId(snt, env);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "startNetwork", "begin");
  
  vlan = adb_startNetworkType_get_vlan(snt, env);
  netName = adb_startNetworkType_get_netName(snt, env);
  
  clusterControllersLen = adb_startNetworkType_sizeof_clusterControllers(snt, env);
  clusterControllers = malloc(sizeof(char *) * clusterControllersLen);
  for (i=0; i<clusterControllersLen; i++) {
    clusterControllers[i] = adb_startNetworkType_get_clusterControllers_at(snt, env, i);
  }
  
  
  snrt = adb_startNetworkResponseType_create(env);
  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doStartNetwork(&ccMeta, netName, vlan, clusterControllers, clusterControllersLen);
    if (rc) {
      logprintf("ERROR: doStartNetwork() returned fail %d\n", rc);
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }
  if (clusterControllers) free(clusterControllers);
  
  adb_startNetworkResponseType_set_return(snrt, env, status);
  if (status == AXIS2_FALSE) {
    adb_startNetworkResponseType_set_statusMessage(snrt, env, statusMessage);
  }
  
  adb_startNetworkResponseType_set_correlationId(snrt, env, ccMeta.correlationId);
  adb_startNetworkResponseType_set_userId(snrt, env, ccMeta.userId);
  
  ret = adb_StartNetworkResponse_create(env);
  adb_StartNetworkResponse_set_StartNetworkResponse(ret, env, snrt);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "startNetwork", "end");
  return(ret);
}

adb_DescribeResourcesResponse_t *DescribeResourcesMarshal(adb_DescribeResources_t *describeResources, const axutil_env_t *env) {
  // output vars
  adb_DescribeResourcesResponse_t *ret=NULL;
  adb_describeResourcesResponseType_t *drrt=NULL;
  
  // input vars
  adb_describeResourcesType_t *drt=NULL;

  // working vars
  int i, rc, *outTypesMax=NULL, *outTypesAvail=NULL;
  int vmLen=0, outTypesLen=0, outServiceTagsLen=0;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char **outServiceTags;
  virtualMachine *vms;
  adb_virtualMachineType_t *vm;
  ncMetadata ccMeta;
  
  drt = adb_DescribeResources_get_DescribeResources(describeResources, env);
  ccMeta.correlationId = adb_describeResourcesType_get_correlationId(drt, env);
  ccMeta.userId = adb_describeResourcesType_get_userId(drt, env);

  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "describeResources", "begin");

  vmLen = adb_describeResourcesType_sizeof_instanceTypes(drt, env);
  vms = malloc(sizeof(virtualMachine) * vmLen);

  for (i=0; i<vmLen; i++) {
    char *name;
    vm = adb_describeResourcesType_get_instanceTypes_at(drt, env, i);
    name = adb_virtualMachineType_get_name(vm, env);
    strncpy(vms[i].name, name, 64);
    vms[i].mem = adb_virtualMachineType_get_memory(vm, env);
    vms[i].cores = adb_virtualMachineType_get_cores(vm, env);
    vms[i].disk = adb_virtualMachineType_get_disk(vm, env);
  }

  // do it
  drrt = adb_describeResourcesResponseType_create(env);
  
  rc=1;
  if (!DONOTHING) {
    rc = doDescribeResources(&ccMeta, &vms, vmLen, &outTypesMax, &outTypesAvail, &outTypesLen, &outServiceTags, &outServiceTagsLen);
  }
  
  if (rc) {
    logprintfl(ERROR, "ERROR: doDescribeResources() failed %d\n", rc);
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    for (i=0; i<outServiceTagsLen; i++) {
      if (outServiceTags[i]) {
	adb_describeResourcesResponseType_add_serviceTags(drrt, env, outServiceTags[i]);
	free(outServiceTags[i]);
      }
    }
    free(outServiceTags);

    for (i=0; i<outTypesLen; i++) {
      adb_ccResourceType_t *rt=NULL;
      
      vm = adb_virtualMachineType_create(env);
      adb_virtualMachineType_set_memory(vm, env, vms[i].mem);
      adb_virtualMachineType_set_cores(vm, env, vms[i].cores);
      adb_virtualMachineType_set_disk(vm, env, vms[i].disk);
      adb_virtualMachineType_set_name(vm, env, vms[i].name);

      rt = adb_ccResourceType_create(env);
      adb_ccResourceType_set_instanceType(rt, env, vm);
      adb_ccResourceType_set_maxInstances(rt, env, outTypesMax[i]);
      adb_ccResourceType_set_availableInstances(rt, env, outTypesAvail[i]);
      adb_describeResourcesResponseType_add_resources(drrt, env, rt);
    }
    if (outTypesMax) free(outTypesMax);
    if (outTypesAvail) free(outTypesAvail);
  }

  if (vms) free(vms);


  adb_describeResourcesResponseType_set_correlationId(drrt, env, ccMeta.correlationId);
  adb_describeResourcesResponseType_set_userId(drrt, env, ccMeta.userId);
  adb_describeResourcesResponseType_set_return(drrt, env, status);
  if (status == AXIS2_FALSE) {
    adb_describeResourcesResponseType_set_statusMessage(drrt, env, statusMessage);
  }
  ret = adb_DescribeResourcesResponse_create(env);
  adb_DescribeResourcesResponse_set_DescribeResourcesResponse(ret, env, drrt);

  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "describeResources", "end");
  return(ret);
}

adb_DescribeInstancesResponse_t *DescribeInstancesMarshal(adb_DescribeInstances_t *describeInstances, const axutil_env_t *env) {
    // output vars
  adb_DescribeInstancesResponse_t *ret=NULL;
  adb_describeInstancesResponseType_t *dirt=NULL;
  
  // input vars
  adb_describeInstancesType_t *dit=NULL;
  
  // working vars
  adb_ccInstanceType_t *it=NULL;
  char **instIds=NULL;
  int instIdsLen, outInstsLen, i, rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];

  ccInstance *outInsts=NULL, *myInstance=NULL;
  ncMetadata ccMeta;
  
  dit = adb_DescribeInstances_get_DescribeInstances(describeInstances, env);
  ccMeta.correlationId = adb_describeInstancesType_get_correlationId(dit, env);
  ccMeta.userId = adb_describeInstancesType_get_userId(dit, env);

  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "describeInstances", "begin");

  instIdsLen = adb_describeInstancesType_sizeof_instanceIds(dit, env);
  instIds = malloc(sizeof(char *) * instIdsLen);
  
  for (i=0; i<instIdsLen; i++) {
    instIds[i] = adb_describeInstancesType_get_instanceIds_at(dit, env, i);
  }

  dirt = adb_describeInstancesResponseType_create(env);

  rc=1;
  if (!DONOTHING) {
    rc = doDescribeInstances(&ccMeta, instIds, instIdsLen, &outInsts, &outInstsLen);
  }
  
  if (instIds) free(instIds);
  if (rc) {
    logprintf("ERROR: doDescribeInstances() failed %d\n", rc);
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");

  } else {
    for (i=0; i<outInstsLen; i++) {
      myInstance = &(outInsts[i]);
      
      it = adb_ccInstanceType_create(env);
  
      rc = ccInstanceUnmarshal(it, myInstance, env);
      adb_describeInstancesResponseType_add_instances(dirt, env, it);
    }
    if (outInsts) free(outInsts);
  }

  adb_describeInstancesResponseType_set_correlationId(dirt, env, ccMeta.correlationId);
  adb_describeInstancesResponseType_set_userId(dirt, env, ccMeta.userId);
  adb_describeInstancesResponseType_set_return(dirt, env, status);
  if (status == AXIS2_FALSE) {
    adb_describeInstancesResponseType_set_statusMessage(dirt, env, statusMessage);
  }
  
  ret = adb_DescribeInstancesResponse_create(env);
  adb_DescribeInstancesResponse_set_DescribeInstancesResponse(ret, env, dirt);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "describeInstances", "end");

  return(ret);
}

int ccInstanceUnmarshal(adb_ccInstanceType_t *dst, ccInstance *src, const axutil_env_t *env) {
  axutil_date_time_t *dt=NULL;
  adb_virtualMachineType_t *vm=NULL;
  adb_netConfigType_t *netconf=NULL;
  adb_volumeType_t *vol=NULL;
  int i;

  dt = axutil_date_time_create_with_offset(env, src->ts - time(NULL));
  
  adb_ccInstanceType_set_instanceId(dst, env, src->instanceId);
  adb_ccInstanceType_set_reservationId(dst, env, src->reservationId);
  adb_ccInstanceType_set_ownerId(dst, env, src->ownerId);
  adb_ccInstanceType_set_imageId(dst, env, src->amiId);
  adb_ccInstanceType_set_kernelId(dst, env, src->kernelId);
  adb_ccInstanceType_set_ramdiskId(dst, env, src->ramdiskId);
  
  adb_ccInstanceType_set_keyName(dst, env, src->keyName);
  adb_ccInstanceType_set_stateName(dst, env, src->state);
  
  adb_ccInstanceType_set_launchTime(dst, env, dt);     
  
  adb_ccInstanceType_set_serviceTag(dst, env, src->serviceTag);
  adb_ccInstanceType_set_userData(dst, env, src->userData);
  adb_ccInstanceType_set_launchIndex(dst, env, src->launchIndex);
  for (i=0; i<64; i++) {
    if (src->groupNames[i][0] != '\0') {
      adb_ccInstanceType_add_groupNames(dst, env, src->groupNames[i]);
    }
  }
  
  for (i=0; i<src->volumesSize; i++) {
    vol = adb_volumeType_create(env);
    adb_volumeType_set_volumeId(vol, env, src->volumes[i].volumeId);
    adb_volumeType_set_remoteDev(vol, env, src->volumes[i].remoteDev);
    adb_volumeType_set_localDev(vol, env, src->volumes[i].localDev);
    adb_volumeType_set_state(vol, env, src->volumes[i].stateName);

    adb_ccInstanceType_add_volumes(dst, env, vol);
  }

  netconf = adb_netConfigType_create(env);
  adb_netConfigType_set_privateMacAddress(netconf, env, src->ccnet.privateMac);
  adb_netConfigType_set_publicMacAddress(netconf, env, src->ccnet.publicMac);
  adb_netConfigType_set_privateIp(netconf, env, src->ccnet.privateIp);
  adb_netConfigType_set_publicIp(netconf, env, src->ccnet.publicIp);
  adb_netConfigType_set_vlan(netconf, env, src->ccnet.vlan);
  adb_ccInstanceType_set_netParams(dst, env, netconf);
  
  vm = adb_virtualMachineType_create(env);
  adb_virtualMachineType_set_memory(vm, env, src->ccvm.mem);
  adb_virtualMachineType_set_cores(vm, env, src->ccvm.cores);
  adb_virtualMachineType_set_disk(vm, env, src->ccvm.disk);
  adb_virtualMachineType_set_name(vm, env, src->ccvm.name);
  adb_ccInstanceType_set_instanceType(dst, env, vm);

  return(0);
}

adb_RunInstancesResponse_t *RunInstancesMarshal(adb_RunInstances_t *runInstances, const axutil_env_t *env) {
  // output vars
  adb_RunInstancesResponse_t *ret=NULL;
  adb_runInstancesResponseType_t *rirt=NULL;
  
  // input vars
  adb_runInstancesType_t *rit=NULL;

  // working vars
  adb_ccInstanceType_t *it=NULL;
  adb_virtualMachineType_t *vm=NULL;
  //  adb_netConfigType_t *netconf=NULL;

  ccInstance *outInsts=NULL, *myInstance=NULL;
  int minCount, maxCount, rc, outInstsLen, i, vlan, instIdsLen, netNamesLen, macAddrsLen;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];

  //  axutil_date_time_t *dt;
  //char numbuf[256];
  char *amiId, *keyName, **instIds=NULL, *reservationId, **netNames=NULL, **macAddrs=NULL, *kernelId, *ramdiskId, *amiURL, *kernelURL, *ramdiskURL, *vmName, *userData, *launchIndex;
  ncMetadata ccMeta;
  
  virtualMachine ccvm;
  
  rit = adb_RunInstances_get_RunInstances(runInstances, env);
  ccMeta.correlationId = adb_runInstancesType_get_correlationId(rit, env);
  ccMeta.userId = adb_runInstancesType_get_userId(rit, env);
  reservationId = adb_runInstancesType_get_reservationId(rit, env);

  {
    char other[256];
    snprintf(other, 256, "begin,%s", reservationId);
    if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "runInstances", other);
  }

  maxCount = adb_runInstancesType_get_maxCount(rit, env);
  minCount = adb_runInstancesType_get_minCount(rit, env);
  keyName = adb_runInstancesType_get_keyName(rit, env);

  amiId = adb_runInstancesType_get_imageId(rit, env);
  kernelId = adb_runInstancesType_get_kernelId(rit, env);
  ramdiskId = adb_runInstancesType_get_ramdiskId(rit, env);

  amiURL = adb_runInstancesType_get_imageURL(rit, env);
  kernelURL = adb_runInstancesType_get_kernelURL(rit, env);
  ramdiskURL = adb_runInstancesType_get_ramdiskURL(rit, env);

  userData = adb_runInstancesType_get_userData(rit, env);
  launchIndex = adb_runInstancesType_get_launchIndex(rit, env);
  
  vm = adb_runInstancesType_get_instanceType(rit, env);
  ccvm.mem = adb_virtualMachineType_get_memory(vm, env);
  ccvm.cores = adb_virtualMachineType_get_cores(vm, env);
  ccvm.disk = adb_virtualMachineType_get_disk(vm, env);
  vmName = adb_virtualMachineType_get_name(vm, env);
  snprintf(ccvm.name, 64, "%s", vmName);

  vlan = adb_runInstancesType_get_vlan(rit, env);

  instIdsLen = adb_runInstancesType_sizeof_instanceIds(rit, env);
  instIds = malloc(sizeof(char *) * instIdsLen);  
  for (i=0; i<instIdsLen; i++) {
    instIds[i] = adb_runInstancesType_get_instanceIds_at(rit, env, i);
  }

  netNamesLen = adb_runInstancesType_sizeof_netNames(rit, env);
  netNames = malloc(sizeof(char *) * netNamesLen);  
  for (i=0; i<netNamesLen; i++) {
    netNames[i] = adb_runInstancesType_get_netNames_at(rit, env, i);
  }

  macAddrsLen = adb_runInstancesType_sizeof_macAddresses(rit, env);
  macAddrs = malloc(sizeof(char *) * macAddrsLen);  
  for (i=0; i<macAddrsLen; i++) {
    macAddrs[i] = adb_runInstancesType_get_macAddresses_at(rit, env, i);
  }
  
  // logic
  rirt = adb_runInstancesResponseType_create(env);

  rc=1;
  if (!DONOTHING) {
    rc = doRunInstances(&ccMeta, amiId, kernelId, ramdiskId, amiURL, kernelURL,ramdiskURL, instIds, instIdsLen, netNames, netNamesLen, macAddrs, macAddrsLen, minCount, maxCount, ccMeta.userId, reservationId, &ccvm, keyName, vlan, userData, launchIndex, NULL, &outInsts, &outInstsLen);
  }
  
  if (rc) {
    logprintf("ERROR: doRunInstances() failed %d\n", rc);
    status=AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    for (i=0; i<outInstsLen; i++) {
      myInstance = &(outInsts[i]);
      
      it = adb_ccInstanceType_create(env);
      
      rc = ccInstanceUnmarshal(it, myInstance, env);
      adb_runInstancesResponseType_add_instances(rirt, env, it);
    }
    if (outInsts) free(outInsts);
  }
  
  adb_runInstancesResponseType_set_correlationId(rirt, env, ccMeta.correlationId);
  adb_runInstancesResponseType_set_userId(rirt, env, ccMeta.userId);
  adb_runInstancesResponseType_set_return(rirt, env, status);
  if (status == AXIS2_FALSE) {
    adb_runInstancesResponseType_set_statusMessage(rirt, env, statusMessage);
  }
  
  ret = adb_RunInstancesResponse_create(env);
  adb_RunInstancesResponse_set_RunInstancesResponse(ret, env, rirt);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "runInstances", "end");
  
  return(ret);
}

adb_RebootInstancesResponse_t* RebootInstancesMarshal (adb_RebootInstances_t* rebootInstances, const axutil_env_t *env) {
  adb_RebootInstancesResponse_t *ret=NULL;
  adb_rebootInstancesResponseType_t *rirt=NULL;

  // input vars
  adb_rebootInstancesType_t *rit=NULL;
  
  // working vars
  char **instIds;
  int instIdsLen, i, rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  ncMetadata ccMeta;

  rit = adb_RebootInstances_get_RebootInstances(rebootInstances, env);
  ccMeta.correlationId = adb_rebootInstancesType_get_correlationId(rit, env);
  ccMeta.userId = adb_rebootInstancesType_get_userId(rit, env);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "rebootInstances", "begin");
  
  instIdsLen = adb_rebootInstancesType_sizeof_instanceIds(rit, env);
  instIds = malloc(sizeof(char *) * instIdsLen);  
  for (i=0; i<instIdsLen; i++) {
    instIds[i] = adb_rebootInstancesType_get_instanceIds_at(rit, env, i);
  }
  
  rc=1;
  if (!DONOTHING) {
    rc = doRebootInstances(&ccMeta, instIds, instIdsLen);
  }
  
  if (instIds) free(instIds);
  
  rirt = adb_rebootInstancesResponseType_create(env);
  if (rc) {
    logprintf("ERROR: doRebootInstances() failed %d\n", rc);
    status=AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    status=AXIS2_TRUE;
  }

  adb_rebootInstancesResponseType_set_correlationId(rirt, env, ccMeta.correlationId);
  adb_rebootInstancesResponseType_set_userId(rirt, env, ccMeta.userId);
  //  adb_rebootInstancesResponseType_set_statusMessage(rirt, env, status);
  adb_rebootInstancesResponseType_set_return(rirt, env, status);
  if (status == AXIS2_FALSE) {
    adb_rebootInstancesResponseType_set_statusMessage(rirt, env, statusMessage);
  }
  
  ret = adb_RebootInstancesResponse_create(env);
  adb_RebootInstancesResponse_set_RebootInstancesResponse(ret, env, rirt);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "rebootInstances", "end");
  
  return(ret);
}

adb_TerminateInstancesResponse_t *TerminateInstancesMarshal(adb_TerminateInstances_t *terminateInstances, const axutil_env_t *env) {
  // OUTPUT VARS
  adb_TerminateInstancesResponse_t *ret=NULL;
  adb_terminateInstancesResponseType_t *tirt=NULL;

  // input vars
  adb_terminateInstancesType_t *tit=NULL;
  
  // working vars
  char **instIds;
  int instIdsLen, i, rc, *outStatus;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];

  ncMetadata ccMeta;
  
  tit = adb_TerminateInstances_get_TerminateInstances(terminateInstances, env);
  ccMeta.correlationId = adb_terminateInstancesType_get_correlationId(tit, env);
  ccMeta.userId = adb_terminateInstancesType_get_userId(tit, env);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "terminateInstances", "begin");
  
  instIdsLen = adb_terminateInstancesType_sizeof_instanceIds(tit, env);
  instIds = malloc(sizeof(char *) * instIdsLen);  
  for (i=0; i<instIdsLen; i++) {
    instIds[i] = adb_terminateInstancesType_get_instanceIds_at(tit, env, i);
  }
  
  rc=1;
  if (!DONOTHING) {
    outStatus = malloc(sizeof(int) * instIdsLen);
    rc = doTerminateInstances(&ccMeta, instIds, instIdsLen, &outStatus);
  }
  
  if (instIds) free(instIds);

  tirt = adb_terminateInstancesResponseType_create(env);
  if (rc) {
    logprintf("ERROR: doTerminateInstances() failed %d\n", rc);
    status=AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    for (i=0; i<instIdsLen; i++) {
      if (outStatus[i]) {
	adb_terminateInstancesResponseType_add_isTerminated(tirt, env, AXIS2_TRUE);
      } else {
	adb_terminateInstancesResponseType_add_isTerminated(tirt, env, AXIS2_FALSE);
      }
    }
  }
  if (outStatus) free(outStatus);
  
  adb_terminateInstancesResponseType_set_correlationId(tirt, env, ccMeta.correlationId);
  adb_terminateInstancesResponseType_set_userId(tirt, env, ccMeta.userId);
  adb_terminateInstancesResponseType_set_return(tirt, env, status);
  if (status == AXIS2_FALSE) {
    adb_terminateInstancesResponseType_set_statusMessage(tirt, env, statusMessage);
  }
  
  ret = adb_TerminateInstancesResponse_create(env);
  adb_TerminateInstancesResponse_set_TerminateInstancesResponse(ret, env, tirt);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "terminateInstances", "end");
  
  return(ret);
}

void print_adb_ccInstanceType(adb_ccInstanceType_t *in) {
  
}

/*
adb_RegisterImageResponse_t *RegisterImageMarshal(adb_RegisterImage_t *registerImage, const axutil_env_t *env) {
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *amiId, *location;

  adb_registerImageType_t *rit=NULL;
  adb_registerImageResponseType_t *rirt=NULL;                               
  adb_RegisterImageResponse_t *ret;
  ncMetadata ccMeta;

  rit = adb_RegisterImage_get_RegisterImage(registerImage, env);
  ccMeta.correlationId = adb_registerImageType_get_correlationId(rit, env);
  ccMeta.userId = adb_registerImageType_get_userId(rit, env);

  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "registerImage", "begin");

  //  newImage = malloc(sizeof(ccImage));
  location = adb_registerImageType_get_imageLocation(rit, env);
  amiId = adb_registerImageType_get_amiId(rit, env);

  rirt = adb_registerImageResponseType_create(env);

  rc=1;
  if (!DONOTHING) {
    rc = doRegisterImage(&ccMeta, amiId, location);
  }

  if (rc != 0) {
    logprintf("ERROR: doRegisterImage() failed %d\n", rc);
    status=AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    adb_registerImageResponseType_set_imageId(rirt, env, amiId);
  }
  
  adb_registerImageResponseType_set_correlationId(rirt, env, ccMeta.correlationId);
  adb_registerImageResponseType_set_userId(rirt, env, ccMeta.userId);
  adb_registerImageResponseType_set_return(rirt, env, status);
  if (status == AXIS2_FALSE) {
    adb_registerImageResponseType_set_statusMessage(rirt, env, statusMessage);
  }

  ret = adb_RegisterImageResponse_create(env);
  adb_RegisterImageResponse_set_RegisterImageResponse(ret, env, rirt);
  
  if (EVENTLOG) eventlog("CC", ccMeta.userId, ccMeta.correlationId, "registerImage", "end");

  return(ret);
}
*/

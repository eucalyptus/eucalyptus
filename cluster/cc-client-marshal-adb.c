#include <stdio.h>
#include <time.h>
#include <misc.h>
#include <cc-client-marshal.h>
#include "axis2_stub_EucalyptusCC.h"
#include <euca_auth.h>
#include <sys/types.h>
#include <unistd.h>

int cc_killallInstances(axutil_env_t *env, axis2_stub_t *stub) {
  int rc, instIdsLen;
  char *instIds[256];
  char *amiId;
  char *state, *str_ret;
  axutil_date_time_t *dt, *dta;
  adb_ccInstanceType_t *it;
  axis2_char_t *resId, *instId=NULL;
  int i, j;
  axis2_bool_t status;

  adb_DescribeInstances_t *diIn=NULL;
  adb_describeInstancesType_t *dit=NULL;

  adb_DescribeInstancesResponse_t *diOut=NULL;
  adb_describeInstancesResponseType_t *dirt=NULL;
  
  adb_netConfigType_t *nct=NULL;
  adb_virtualMachineType_t *vm=NULL;

  dit = adb_describeInstancesType_create(env);
  //    adb_describeInstancesType_add_instanceIds(dit, env, instId);
  adb_describeInstancesType_set_userId(dit, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_describeInstancesType_set_correlationId(dit, env, cidstr);
  }
  
  diIn = adb_DescribeInstances_create(env);
  adb_DescribeInstances_set_DescribeInstances(diIn, env, dit);
  
  diOut = axis2_stub_op_EucalyptusCC_DescribeInstances(stub, env, diIn);
  if (!diOut) {
    printf("ERROR: DI stub failed NULL\n");
    return(1);
  } else {
    dirt = adb_DescribeInstancesResponse_get_DescribeInstancesResponse(diOut, env);
    status = adb_describeInstancesResponseType_get_return(dirt, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_describeInstancesResponseType_get_statusMessage(dirt, env));
    } else {
      instIdsLen = adb_describeInstancesResponseType_sizeof_instances(dirt, env);
      for (i=0; i<instIdsLen; i++) {
	it = adb_describeInstancesResponseType_get_instances_at(dirt, env, i);
	instId = adb_ccInstanceType_get_instanceId(it, env);
	if (instId != NULL) {
	  instIds[i] = strdup(instId);
	}
      }

      rc = cc_terminateInstances(instIds, instIdsLen, env, stub);
    }
  }
  return(0);
}

int cc_getConsoleOutput(char *instId, axutil_env_t *env, axis2_stub_t *stub) {
  adb_GetConsoleOutput_t *tiIn;
  adb_getConsoleOutputType_t *tit;
  
  adb_GetConsoleOutputResponse_t *tiOut;
  adb_getConsoleOutputResponseType_t *tirt;
  int i, j;
  axis2_bool_t status;
  char *output;
  
  
  printf("%s\n", instId);

  tit = adb_getConsoleOutputType_create(env);
  adb_getConsoleOutputType_set_instanceId(tit, env, instId);
  adb_getConsoleOutputType_set_userId(tit, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_getConsoleOutputType_set_correlationId(tit, env, cidstr);
  }
  
  tiIn = adb_GetConsoleOutput_create(env);
  adb_GetConsoleOutput_set_GetConsoleOutput(tiIn, env, tit);
  
  tiOut = axis2_stub_op_EucalyptusCC_GetConsoleOutput(stub, env, tiIn);
  if (!tiOut) {
    printf("ERROR: GCO failed NULL\n");
    return(1);
  } else {
    tirt = adb_GetConsoleOutputResponse_get_GetConsoleOutputResponse(tiOut, env);
    status = adb_getConsoleOutputResponseType_get_return(tirt, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_getConsoleOutputResponseType_get_statusMessage(tirt, env));
      return(1);
    } else {
      output = adb_getConsoleOutputResponseType_get_consoleOutput(tirt, env);
      printf("RAW CONSOLE OUTPUT: %s\n", output);
      printf("RAW CONSOLE OUTPUT: %s\n", base64_dec((unsigned char *)output, strlen(output)));
    }
  }
  return(0);
}

int cc_rebootInstances(char **instIds, int instIdsLen, axutil_env_t *env, axis2_stub_t *stub) {
  adb_RebootInstances_t *tiIn;
  adb_rebootInstancesType_t *tit;

  adb_RebootInstancesResponse_t *tiOut;
  adb_rebootInstancesResponseType_t *tirt;
  int i, j;
  axis2_bool_t status;

  printf("%d %s\n", instIdsLen, instIds[0]);

  tit = adb_rebootInstancesType_create(env);
  for (i=0; i<instIdsLen; i++) {
    adb_rebootInstancesType_add_instanceIds(tit, env, instIds[i]);
  }
  adb_rebootInstancesType_set_userId(tit, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_rebootInstancesType_set_correlationId(tit, env, cidstr);
  }
  
  tiIn = adb_RebootInstances_create(env);
  adb_RebootInstances_set_RebootInstances(tiIn, env, tit);
  
  tiOut = axis2_stub_op_EucalyptusCC_RebootInstances(stub, env, tiIn);
  if (!tiOut) {
    printf("ERROR: RI failed NULL\n");
    return(1);
  } else {
    tirt = adb_RebootInstancesResponse_get_RebootInstancesResponse(tiOut, env);
    status = adb_rebootInstancesResponseType_get_return(tirt, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_rebootInstancesResponseType_get_statusMessage(tirt, env));
    } else {
    }
  }
  return(status);
}

int cc_terminateInstances(char **instIds, int instIdsLen, axutil_env_t *env, axis2_stub_t *stub) {
  adb_TerminateInstances_t *tiIn;
  adb_terminateInstancesType_t *tit;

  adb_TerminateInstancesResponse_t *tiOut;
  adb_terminateInstancesResponseType_t *tirt;
  //  adb_ccTerminatedInstanceType_t *ctit;
  int i;
  printf("%d %s\n", instIdsLen, instIds[0]);
  tit = adb_terminateInstancesType_create(env);
  for (i=0; i<instIdsLen; i++) {
    adb_terminateInstancesType_add_instanceIds(tit, env, instIds[i]);
  }
  adb_terminateInstancesType_set_userId(tit, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_terminateInstancesType_set_correlationId(tit, env, cidstr);
  }

  tiIn = adb_TerminateInstances_create(env);
  adb_TerminateInstances_set_TerminateInstances(tiIn, env, tit);
  
  tiOut = axis2_stub_op_EucalyptusCC_TerminateInstances(stub, env, tiIn);
  if (!tiOut) {
    printf("ERROR: TI failed NULL\n");
    return(1);
  } else {
    axis2_char_t *instId;
    int i, j;
    axis2_bool_t status;

    tirt = adb_TerminateInstancesResponse_get_TerminateInstancesResponse(tiOut, env);
    status = adb_terminateInstancesResponseType_get_return(tirt, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_terminateInstancesResponseType_get_statusMessage(tirt, env));
    } else {
      printf("Term: number: %d\n", adb_terminateInstancesResponseType_sizeof_isTerminated(tirt, env));

    }
  }
  
  return(0);
}

int cc_configureNetwork(char *sourceNet, char *destName, char *protocol, int min, int max, char *type, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  char meh[32];
  adb_ConfigureNetwork_t *input;
  adb_ConfigureNetworkResponse_t *output;
  adb_configureNetworkType_t *cn;
  adb_configureNetworkResponseType_t *cnrt;
  
  adb_networkRule_t *nr=NULL;

  cn = adb_configureNetworkType_create(env);
  input = adb_ConfigureNetwork_create(env);
  
  adb_configureNetworkType_set_userId(cn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_configureNetworkType_set_correlationId(cn, env, cidstr);
  }
  
  nr = adb_networkRule_create(env);

  adb_networkRule_set_type(nr, env, type);
  
  if (protocol) {
    adb_networkRule_set_protocol(nr, env, protocol);
  } else {
    adb_networkRule_set_protocol(nr, env, "all");
  }
  adb_networkRule_set_destName(nr, env, destName);
  
  adb_networkRule_set_portRangeMin(nr, env, min);
  adb_networkRule_set_portRangeMax(nr, env, max);

  {
    char t;
    t = sourceNet[0];
    printf("%s T: %d\n", sourceNet, t);
    if (t < 48 || t > 57 ) {
      adb_networkRule_add_sourceNames(nr, env, sourceNet);
    } else {
      adb_networkRule_add_sourceNets(nr, env, sourceNet);
    }
  }
  adb_networkRule_add_userNames(nr, env, "eucalyptus");
  
  adb_configureNetworkType_add_rules(cn, env, nr);

  adb_ConfigureNetwork_set_ConfigureNetwork(input, env, cn);

  output = axis2_stub_op_EucalyptusCC_ConfigureNetwork(stub, env, input);
  if (!output) {
    printf("ERROR: configureNetwork returned NULL\n");
    return(1);
  }
  cnrt = adb_ConfigureNetworkResponse_get_ConfigureNetworkResponse(output, env);
  printf("configurenetwork returned status %d\n", adb_configureNetworkResponseType_get_return(cnrt, env));
  return(0);

}
int cc_stopNetwork(int vlan, char *netName, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  char meh[32];
  adb_StopNetwork_t *input;
  adb_StopNetworkResponse_t *output;
  adb_stopNetworkType_t *sn;
  adb_stopNetworkResponseType_t *snrt;

  sn = adb_stopNetworkType_create(env);
  input = adb_StopNetwork_create(env);
  
  adb_stopNetworkType_set_userId(sn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_stopNetworkType_set_correlationId(sn, env, cidstr);
  }
  adb_stopNetworkType_set_vlan(sn, env, vlan);
  adb_stopNetworkType_set_netName(sn, env, netName);

  adb_StopNetwork_set_StopNetwork(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_StopNetwork(stub, env, input);
  if (!output) {
    printf("ERROR: stopNetwork returned NULL\n");
    return(1);
  }
  snrt = adb_StopNetworkResponse_get_StopNetworkResponse(output, env);
  printf("stopnetwork returned status %d\n", adb_stopNetworkResponseType_get_return(snrt, env));
  return(0);
}

int cc_assignAddress(char *src, char *dst, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  char meh[32];
  adb_AssignAddress_t *input;
  adb_AssignAddressResponse_t *output;
  adb_assignAddressType_t *sn;
  adb_assignAddressResponseType_t *snrt;

  sn = adb_assignAddressType_create(env);
  input = adb_AssignAddress_create(env);
  
  adb_assignAddressType_set_userId(sn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_assignAddressType_set_correlationId(sn, env, cidstr);
  }
  adb_assignAddressType_set_source(sn, env, src);
  adb_assignAddressType_set_dest(sn, env, dst);

  adb_AssignAddress_set_AssignAddress(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_AssignAddress(stub, env, input);
  if (!output) {
    printf("ERROR: assignAddress returned NULL\n");
    return(1);
  }
  snrt = adb_AssignAddressResponse_get_AssignAddressResponse(output, env);
  printf("assignAddress returned status %d\n", adb_assignAddressResponseType_get_return(snrt, env));
  return(0);
}
int cc_unassignAddress(char *src, char *dst, axutil_env_t *env, axis2_stub_t *stub){
  int i;
  char meh[32];
  adb_UnassignAddress_t *input;
  adb_UnassignAddressResponse_t *output;
  adb_unassignAddressType_t *sn;
  adb_unassignAddressResponseType_t *snrt;
  
  sn = adb_unassignAddressType_create(env);
  input = adb_UnassignAddress_create(env);
  
  adb_unassignAddressType_set_userId(sn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_unassignAddressType_set_correlationId(sn, env, cidstr);
  }
  adb_unassignAddressType_set_source(sn, env, src);
  adb_unassignAddressType_set_dest(sn, env, dst);

  adb_UnassignAddress_set_UnassignAddress(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_UnassignAddress(stub, env, input);
  if (!output) {
    printf("ERROR: unassignAddress returned NULL\n");
    return(1);
  }
  snrt = adb_UnassignAddressResponse_get_UnassignAddressResponse(output, env);
  printf("unassignAddress returned status %d\n", adb_unassignAddressResponseType_get_return(snrt, env));
  return(0);
}

int cc_describePublicAddresses(axutil_env_t *env, axis2_stub_t *stub) {
  int i, len;
  char meh[32];
  adb_DescribePublicAddresses_t *input;
  adb_DescribePublicAddressesResponse_t *output;
  adb_describePublicAddressesType_t *sn;
  adb_describePublicAddressesResponseType_t *snrt;
  
  sn = adb_describePublicAddressesType_create(env);
  input = adb_DescribePublicAddresses_create(env);
  
  adb_describePublicAddressesType_set_userId(sn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_describePublicAddressesType_set_correlationId(sn, env, cidstr);
  }
  adb_DescribePublicAddresses_set_DescribePublicAddresses(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_DescribePublicAddresses(stub, env, input);
  if (!output) {
    printf("ERROR: describePublicAddresses returned NULL\n");
    return(1);
  }
  snrt = adb_DescribePublicAddressesResponse_get_DescribePublicAddressesResponse(output, env);
  len = adb_describePublicAddressesResponseType_sizeof_sourceAddresses(snrt, env);
  for (i=0; i<len; i++) {
    char *ip;
    char *dstip;
    ip = adb_describePublicAddressesResponseType_get_sourceAddresses_at(snrt, env, i);
    dstip = adb_describePublicAddressesResponseType_get_destAddresses_at(snrt, env, i);

    printf("IP: %s ALLOC: %s\n", ip, dstip);
  }
  // len = ...for (i=0....
  //  printf("descibePublicAddresses returned status %d\n", adb_describePublicAddressesResponseType_get_networkStatus(snrt, env));
  return(0);
}

int cc_startNetwork(int vlan, char *netName, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  char meh[32];
  adb_StartNetwork_t *input;
  adb_StartNetworkResponse_t *output;
  adb_startNetworkType_t *sn;
  adb_startNetworkResponseType_t *snrt;

  sn = adb_startNetworkType_create(env);
  input = adb_StartNetwork_create(env);
  
  adb_startNetworkType_set_userId(sn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_startNetworkType_set_correlationId(sn, env, cidstr);
  }
  adb_startNetworkType_set_vlan(sn, env, vlan);
  adb_startNetworkType_set_netName(sn, env, netName);

  adb_StartNetwork_set_StartNetwork(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_StartNetwork(stub, env, input);
  if (!output) {
    printf("ERROR: startNetwork returned NULL\n");
    return(1);
  }
  snrt = adb_StartNetworkResponse_get_StartNetworkResponse(output, env);
  printf("startnetwork returned status %d\n", adb_startNetworkResponseType_get_return(snrt, env));
  return(0);
}

int cc_describeResources(axutil_env_t *env, axis2_stub_t *stub) {
  adb_DescribeResourcesResponse_t *drOut=NULL;
  adb_describeResourcesResponseType_t *drrt=NULL;
  adb_ccResourceType_t *rt=NULL;

  adb_DescribeResources_t *drIn=NULL;
  adb_describeResourcesType_t *drt=NULL;

  adb_virtualMachineType_t *vm;
  int i;
  axis2_bool_t status;

  drt = adb_describeResourcesType_create(env);
  //  adb_describeResourcesType_add_instanceTypes(drt, env, "1");
  //  adb_describeResourcesType_add_instanceTypes(drt, env, "2");
  
  adb_describeResourcesType_set_userId(drt, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_describeResourcesType_set_correlationId(drt, env, cidstr);
  }

  vm = adb_virtualMachineType_create(env);
  adb_virtualMachineType_set_name(vm, env, "large");
  adb_virtualMachineType_set_memory(vm, env, 8192);
  adb_virtualMachineType_set_cores(vm, env, 8);
  adb_virtualMachineType_set_disk(vm, env, 250);
  adb_describeResourcesType_add_instanceTypes(drt, env, vm);

  vm = adb_virtualMachineType_create(env);
  adb_virtualMachineType_set_name(vm, env, "medium");
  adb_virtualMachineType_set_memory(vm, env, 2048);
  adb_virtualMachineType_set_cores(vm, env, 4);
  adb_virtualMachineType_set_disk(vm, env, 100);
  adb_describeResourcesType_add_instanceTypes(drt, env, vm);

  vm = adb_virtualMachineType_create(env);
  adb_virtualMachineType_set_name(vm, env, "small");
  adb_virtualMachineType_set_memory(vm, env, 128);
  adb_virtualMachineType_set_cores(vm, env, 1);
  adb_virtualMachineType_set_disk(vm, env, 10);
  adb_describeResourcesType_add_instanceTypes(drt, env, vm);
  

  drIn = adb_DescribeResources_create(env);
  adb_DescribeResources_set_DescribeResources(drIn, env, drt);

  drOut = axis2_stub_op_EucalyptusCC_DescribeResources(stub, env, drIn);
  if (!drOut) {
    printf("ERROR: DR failed NULL\n");
    return(1);
  }

  drrt = adb_DescribeResourcesResponse_get_DescribeResourcesResponse(drOut, env);
  status = adb_describeResourcesResponseType_get_return(drrt, env);
  if (status == AXIS2_FALSE) {
    printf("operation fault '%s'\n", adb_describeResourcesResponseType_get_statusMessage(drrt, env));
  } else {
    
    for (i=0; i<adb_describeResourcesResponseType_sizeof_serviceTags(drrt, env); i++) {
      printf("%s ", adb_describeResourcesResponseType_get_serviceTags_at(drrt, env, i));
    }
    printf("\n");
    for (i=0; i<adb_describeResourcesResponseType_sizeof_resources(drrt, env); i++) {
      rt = adb_describeResourcesResponseType_get_resources_at(drrt, env, i);
      printf("DescribeResources: %d %d\n", adb_ccResourceType_get_maxInstances(rt, env), adb_ccResourceType_get_availableInstances(rt, env));
    }
  }
  return(0);
}

int cc_describeInstances(char **instIds, int instIdsLen, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  adb_DescribeInstances_t *diIn;
  adb_describeInstancesType_t *dit;

  adb_DescribeInstancesResponse_t *diOut;
  adb_describeInstancesResponseType_t *dirt;

  adb_netConfigType_t *nct=NULL;
  adb_virtualMachineType_t *vm=NULL;

  dit = adb_describeInstancesType_create(env);
  if (instIds == NULL || instIdsLen == 0) {
  } else {
    for (i=0; i<instIdsLen; i++) {
      adb_describeInstancesType_add_instanceIds(dit, env, instIds[i]);
    }
  }
  adb_describeInstancesType_set_userId(dit, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_describeInstancesType_set_correlationId(dit, env, cidstr);
  }

  diIn = adb_DescribeInstances_create(env);
  adb_DescribeInstances_set_DescribeInstances(diIn, env, dit);
  
  diOut = axis2_stub_op_EucalyptusCC_DescribeInstances(stub, env, diIn);
  if (!diOut) {
    printf("ERROR: DI failed NULL\n");
    return(1);
  } else {
    //    adb_reservationInfoType_t *resit;
    adb_ccInstanceType_t *it;
    axis2_char_t *resId, *instId=NULL;
    int i, j;
    axis2_bool_t status;

    dirt = adb_DescribeInstancesResponse_get_DescribeInstancesResponse(diOut, env);
    status = adb_describeInstancesResponseType_get_return(dirt, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_describeInstancesResponseType_get_statusMessage(dirt, env));
    } else {
      for (i=0; i<adb_describeInstancesResponseType_sizeof_instances(dirt, env); i++) {
	char *amiId;
	char *state;
	char *reservationId;
	char *ownerId, *keyName;
	axutil_date_time_t *dt, *dta;
	
	it = adb_describeInstancesResponseType_get_instances_at(dirt, env, i);
	instId = adb_ccInstanceType_get_instanceId(it, env);
	amiId = adb_ccInstanceType_get_imageId(it, env);
	reservationId = adb_ccInstanceType_get_reservationId(it, env);
	ownerId = adb_ccInstanceType_get_ownerId(it, env);
	keyName = adb_ccInstanceType_get_keyName(it, env);
	state = adb_ccInstanceType_get_stateName(it, env);
	nct = adb_ccInstanceType_get_netParams(it, env);
	vm = adb_ccInstanceType_get_instanceType(it, env);

	dt = adb_ccInstanceType_get_launchTime(it, env);
	if (0)
	{
	  time_t ts, tsu, tsdelta, tsdelta_min;
	  struct tm *tmu;
	  ts = time(NULL);
	  tmu = gmtime(&ts);
	  tsu = mktime(tmu);
	  tsdelta = (tsu - ts) / 3600;
	  tsdelta_min = ((tsu - ts) - (tsdelta * 3600)) / 60;

	  struct tm t = {
	    axutil_date_time_get_second(dt, env),
	    axutil_date_time_get_minute(dt, env)-tsdelta_min,
	    axutil_date_time_get_hour(dt, env)-tsdelta,
	    axutil_date_time_get_date(dt, env),
	    axutil_date_time_get_month(dt, env)-1,
	    axutil_date_time_get_year(dt, env)-1900,
	    0,
	    0,
	    0};
	  
	  ts = mktime(&t);
	  //	  printf("TIME: %d %d, %s\n", time(NULL) - mktime(&t), mktime(&t), ctime(&ts));
	}
	
	printf("Desc: %s %s %s %s %s %s %s %s %d %s %s %d %d %d %s %s %s %s\n", instId, reservationId, ownerId, state, adb_netConfigType_get_privateMacAddress(nct, env), adb_netConfigType_get_publicMacAddress(nct, env), adb_netConfigType_get_privateIp(nct, env), adb_netConfigType_get_publicIp(nct, env), adb_netConfigType_get_vlan(nct, env), keyName, adb_virtualMachineType_get_name(vm, env), adb_virtualMachineType_get_cores(vm, env),adb_virtualMachineType_get_memory(vm, env),adb_virtualMachineType_get_disk(vm, env), adb_ccInstanceType_get_serviceTag(it, env), adb_ccInstanceType_get_userData(it, env), adb_ccInstanceType_get_launchIndex(it, env), adb_ccInstanceType_get_groupNames_at(it, env, 0));
	
      }
    }
  }
  return 0;
}


int cc_runInstances(char *amiId, char *amiURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, int num, int vlan, char *netName, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  adb_RunInstances_t *riIn;
  adb_runInstancesType_t *rit;

  adb_RunInstancesResponse_t *riOut;
  adb_runInstancesResponseType_t *rirt;
  
  adb_virtualMachineType_t *vm;
  char mac[32], meh[32];
  
  srand(time(NULL));
  
  vm = adb_virtualMachineType_create(env);
  adb_virtualMachineType_set_name(vm, env, "small");
  adb_virtualMachineType_set_memory(vm, env, 128);
  adb_virtualMachineType_set_cores(vm, env, 1);
  adb_virtualMachineType_set_disk(vm, env, 10);
  
  rit = adb_runInstancesType_create(env);
  adb_runInstancesType_set_imageId(rit, env, amiId);
  adb_runInstancesType_set_imageURL(rit, env, amiURL);
  adb_runInstancesType_set_kernelId(rit, env, kernelId);
  adb_runInstancesType_set_kernelURL(rit, env, kernelURL);
  adb_runInstancesType_set_ramdiskId(rit, env, ramdiskId);
  adb_runInstancesType_set_ramdiskURL(rit, env, ramdiskURL);
  adb_runInstancesType_set_minCount(rit, env, num);
  adb_runInstancesType_set_maxCount(rit, env, num);
  
  adb_runInstancesType_set_userData(rit, env, "I AM USER DATA");
  adb_runInstancesType_set_launchIndex(rit, env, "I AM A LAUNCH INDEX");
  
  adb_runInstancesType_add_netNames(rit, env, netName);
  adb_runInstancesType_add_netNames(rit, env, "another");
  adb_runInstancesType_add_netNames(rit, env, "third");
  
  adb_runInstancesType_set_keyName(rit, env, "ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAvUJ+N859MBn7YZt4nt7KWm/4uX4/a+vHHFbS1yTDDa1hO6vUxcyJRmJYjfPYtUXJSUx/EInhtfbSLFdVioZbd3a8CpLuoJXZGYrxGK9YCiGk/9tevJD1RyMnsBkWPIJk3AMaWRloUnAMkUeFd6N74cMsig44oI1Bvd0aEoHxw0l5qJnOFeoNYkkCBZsrJOU8/muAPS/WAI5ro23p5k3VUSqmh+29OJ/hm/Vb3UvtspUqgJwuAVoIBtf8DlzM/M+X+leO0Ek7hgtxrgX05yhfu3LJMrkcRGDxUASbQ1GiYp9fPu3YbtBCcFSjXI7bgPWHXLw5QlWa1DxdXqYhmYZU3w== nurmi@spinner");
  adb_runInstancesType_set_userId(rit, env, "eucalyptus");

  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_runInstancesType_set_correlationId(rit, env, cidstr);
  }

  adb_runInstancesType_set_instanceType(rit, env, vm);

  //  snprintf(mac, 32, "aa:dd:11:%c%c:%c%c:%c%c", rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65);
  //  adb_runInstancesType_set_privateMacBase(rit, env, mac);
  //  snprintf(mac, 32, "aa:dd:11:%c%c:%c%c:%c%c", rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65);
  //  printf("running MACC: %s VLAN: %d\n", mac, vlan);  
  //  adb_runInstancesType_set_publicMacBase(rit, env, mac);
  //  adb_runInstancesType_set_macLimit(rit, env, 100);

  adb_runInstancesType_set_vlan(rit, env, vlan);
  
  {
    char *instId, c, *resId, *mac;
    int j, i;
    resId = malloc(sizeof(char) * 32);
    instId = malloc(sizeof(char) * 32);
    mac = malloc(sizeof(char) * 32);
    
    for (i=0; i<num; i++) {
      snprintf(mac, 32, "aa:dd:11:%c%c:%c%c:%c%c", rand()%5 + 65,rand()%5 + 65, rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65);
      adb_runInstancesType_add_macAddresses(rit, env, mac);
      
      snprintf(instId, 32, "i-");
      snprintf(resId, 32, "r-");
      for (j=0; j<8; j++) {
	char c[2];
	snprintf(c, 2, "%c",(rand()%26) + 97);
	strncat(instId, c, 32);
      }
      adb_runInstancesType_add_instanceIds(rit, env, instId);

    }
    for (j=0; j<8; j++) {
      char c[2];
      snprintf(c, 2, "%c",(rand()%26) + 97);
      strncat(resId, c, 32);
    }
    adb_runInstancesType_set_reservationId(rit, env, resId);
  }
  //  adb_runInstancesType_set_netParams(rit, env, netconf);

  riIn = adb_RunInstances_create(env);
  adb_RunInstances_set_RunInstances(riIn, env, rit);
  
  riOut = axis2_stub_op_EucalyptusCC_RunInstances(stub, env, riIn);
  if (!riOut) {
    printf("ERROR: RI failed NULL\n");
    return(1);
  } else {
    adb_ccInstanceType_t *it;
    axis2_char_t *resId, *instId;
    int i;
    axis2_bool_t status;

    instId = NULL;
    rirt = adb_RunInstancesResponse_get_RunInstancesResponse(riOut, env);
    status = adb_runInstancesResponseType_get_return(rirt, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_runInstancesResponseType_get_statusMessage(rirt, env));
    } else {
      
      for (i=0; i<adb_runInstancesResponseType_sizeof_instances(rirt, env); i++) {
	it = adb_runInstancesResponseType_get_instances_at(rirt, env, i);
	instId = adb_ccInstanceType_get_instanceId(it, env);
	printf("Run: instance id: %s %s %s %s\n", instId, adb_ccInstanceType_get_reservationId(it, env), adb_ccInstanceType_get_ownerId(it, env),  adb_ccInstanceType_get_serviceTag(it, env));
      }
    }
  }
  
  return 0;
}

/*
int cc_registerImage(char *imageloc, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  adb_RegisterImage_t *riIn;
  adb_registerImageType_t *rit;
  adb_RegisterImageResponse_t *riOut;
  adb_registerImageResponseType_t *rirt;
  
  rit = adb_registerImageType_create(env);
  adb_registerImageType_set_imageLocation(rit, env, imageloc);
  adb_registerImageType_set_amiId(rit, env, "ami-dannurmi");
  adb_registerImageType_set_userId(rit, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_registerImageType_set_correlationId(rit, env, cidstr);
  }
  riIn = adb_RegisterImage_create(env);
  adb_RegisterImage_set_RegisterImage(riIn, env, rit);
  
  riOut = axis2_stub_op_EucalyptusCC_RegisterImage(stub, env, riIn);
  if (!riOut) {
    printf("ERROR: RI failed NULL\n");
    return(1);
  } else {
    axis2_char_t *amiId;
    axis2_bool_t status;

    rirt = adb_RegisterImageResponse_get_RegisterImageResponse(riOut, env);
    status = adb_registerImageResponseType_get_return(rirt, env);
    printf("RI status code %d\n", status);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_registerImageResponseType_get_statusMessage(rirt, env));
    } else {
      amiId = adb_registerImageResponseType_get_imageId(rirt, env);
      printf("AmiId: %s\n", amiId);
    }
  }
  
  return 0;
}
*/

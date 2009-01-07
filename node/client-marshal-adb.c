#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <neethi_policy.h>
#include <neethi_util.h>

#include "axis2_stub_EucalyptusNC.h"
#include "client-marshal.h"
#include "misc.h"

ncStub * ncStubCreate (char *endpoint_uri, char *logfile, char *homedir) 
{
    axutil_env_t * env = NULL;
    axis2_char_t * client_home;
    axis2_stub_t * stub;
    ncStub * st = NULL;

    if ( logfile ) {
      env =  axutil_env_create_all (logfile, AXIS2_LOG_LEVEL_TRACE);
    } else {
      env =  axutil_env_create_all (NULL, 0);
    }
    if ( homedir ) {
        client_home = (axis2_char_t *)homedir;
    } else {
        client_home = AXIS2_GETENV("AXIS2C_HOME");
    }
    
    /* TODO: what if endpoint_uri, home, or env are NULL? */
    stub = axis2_stub_create_EucalyptusNC(env, client_home, (axis2_char_t *)endpoint_uri);

    if (stub && (st = malloc (sizeof(ncStub)))) {
        st->env=env;
        st->client_home=strdup((char *)client_home);
        st->endpoint_uri=(axis2_char_t *)strdup(endpoint_uri);
        st->stub=stub;
    } 
    
    return (st);
}

int ncStubDestroy (ncStub * st)
{
    if (st->client_home) free(st->client_home);
    if (st->endpoint_uri) free(st->endpoint_uri);
    free (st);
    return (0);
}

/************************** stubs **************************/

static int datetime_to_unix (axutil_date_time_t *dt, axutil_env_t *env)
{
  time_t tsu, ts, tsdelta, tsdelta_min;
  struct tm *tmu;
  
  ts = time(NULL);
  tmu = gmtime(&ts);
  tsu = mktime(tmu);
  tsdelta = (tsu - ts) / 3600;
  tsdelta_min = ((tsu - ts) - (tsdelta * 3600)) / 60;

  struct tm t = {
    axutil_date_time_get_second(dt, env),
    axutil_date_time_get_minute(dt, env) - tsdelta_min,
    axutil_date_time_get_hour(dt, env) - tsdelta,
    axutil_date_time_get_date(dt, env),
    axutil_date_time_get_month(dt, env)-1,
    axutil_date_time_get_year(dt, env)-1900,
    0,
    0,
    0
  };
  
  return (int)mktime(&t);
}

int ncRunInstanceStub (ncStub *st, ncMetadata *meta, char *instanceId, char *reservationId, ncInstParams *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, char *privMac, char *pubMac, int vlan, ncInstance **outInstPtr)
{
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;
    adb_ncRunInstance_t     * input   = adb_ncRunInstance_create(env); 
    adb_ncRunInstanceType_t * request = adb_ncRunInstanceType_create(env);
    adb_virtualMachineType_t * vm_type = adb_virtualMachineType_create(env);
    adb_netConfigType_t *netconf = adb_netConfigType_create(env);
    
    /* set input fields */
    if (meta) {
      adb_ncRunInstanceType_set_correlationId(request, env, meta->correlationId);
      adb_ncRunInstanceType_set_userId(request, env, meta->userId);
    }

    adb_ncRunInstanceType_set_instanceId(request, env, instanceId);
    adb_ncRunInstanceType_set_reservationId(request, env, reservationId);
    adb_ncRunInstanceType_set_imageId(request, env, imageId);
    adb_ncRunInstanceType_set_imageURL(request, env, imageURL);
    adb_ncRunInstanceType_set_kernelId(request, env, kernelId);
    adb_ncRunInstanceType_set_kernelURL(request, env, kernelURL);
    adb_ncRunInstanceType_set_ramdiskId(request, env, ramdiskId);
    adb_ncRunInstanceType_set_ramdiskURL(request, env, ramdiskURL);
    adb_ncRunInstanceType_set_keyName(request, env, keyName);
    adb_virtualMachineType_set_memory(vm_type, env, params->memorySize);
    adb_virtualMachineType_set_cores(vm_type, env, params->numberOfCores);
    adb_virtualMachineType_set_disk(vm_type, env, params->diskSize);
    adb_ncRunInstanceType_set_instanceType(request, env, vm_type);

    adb_ncRunInstanceType_set_privateMacAddress(request, env, privMac);
    adb_ncRunInstanceType_set_publicMacAddress(request, env, pubMac);
    adb_ncRunInstanceType_set_vlan(request, env, vlan);

    /* TODO: marshal the network config */
    adb_ncRunInstance_set_ncRunInstance(input, env, request);

    /* do it */
    {
      adb_ncRunInstanceResponse_t * output = axis2_stub_op_EucalyptusNC_ncRunInstance(stub, env, input);
      
      if (!output) {
	logprintfl(EUCAERROR, "RunInstance returned NULL\n");
	return(1);
      } else {
	adb_ncRunInstanceResponseType_t * response = adb_ncRunInstanceResponse_get_ncRunInstanceResponse(output, env);
	adb_ncInstanceType_t * instance    = adb_ncRunInstanceResponseType_get_instance(response, env);
	adb_virtualMachineType_t * vm_type = adb_ncInstanceType_get_instanceType(instance, env);
	adb_netConfigType_t * netconf = adb_ncInstanceType_get_netParams(instance, env);
	axutil_date_time_t * dt = adb_ncInstanceType_get_launchTime(instance, env);
	ncInstParams params = { 
	  adb_virtualMachineType_get_memory(vm_type, env),
	  adb_virtualMachineType_get_cores(vm_type, env),
	  adb_virtualMachineType_get_disk(vm_type, env)
	};
	
	ncNetConf ncnet;

	ncnet.vlan = adb_netConfigType_get_vlan(netconf, env);
	
	strncpy(ncnet.privateMac, adb_netConfigType_get_privateMacAddress(netconf, env), 32);
	strncpy(ncnet.publicMac, adb_netConfigType_get_publicMacAddress(netconf, env), 32);
	strncpy(ncnet.privateIp, adb_netConfigType_get_privateIp(netconf, env), 32);
	strncpy(ncnet.publicIp, adb_netConfigType_get_publicIp(netconf, env), 32);
	
	ncInstance * outInst = allocate_instance(
						 (char *)adb_ncInstanceType_get_instanceId(instance, env),
						 (char *)adb_ncInstanceType_get_reservationId(instance, env),
						 &params,
						 (char *)adb_ncInstanceType_get_imageId(instance, env),
						 (char *)adb_ncInstanceType_get_imageURL(instance, env),
						 (char *)adb_ncInstanceType_get_kernelId(instance, env),
						 (char *)adb_ncInstanceType_get_kernelURL(instance, env),
						 (char *)adb_ncInstanceType_get_ramdiskId(instance, env),
						 (char *)adb_ncInstanceType_get_ramdiskURL(instance, env),
						 (char *)adb_ncInstanceType_get_stateName(instance, env),
						 (int)atoi((char *)adb_ncInstanceType_get_stateCode(instance, env)),
						 (char *)adb_ncInstanceType_get_userId(instance, env), &ncnet, (char *)adb_ncInstanceType_get_keyName(instance, env));

	outInst->launchTime = datetime_to_unix (dt, env);
	axutil_date_time_free(dt, env);
	* outInstPtr = outInst;
      }
    }
    
    return(0);
}

int ncGetConsoleOutputStub (ncStub *st, ncMetadata *meta, char *instanceId, char **consoleOutput) {
  axutil_env_t * env = st->env;
  axis2_stub_t * stub = st->stub;
  
  adb_ncGetConsoleOutput_t     * input   = adb_ncGetConsoleOutput_create(env); 
  adb_ncGetConsoleOutputType_t * request = adb_ncGetConsoleOutputType_create(env);
  
  *consoleOutput = NULL;

  /* set input fields */
  if (meta) {
    adb_ncGetConsoleOutputType_set_correlationId(request, env, meta->correlationId);
    adb_ncGetConsoleOutputType_set_userId(request, env, meta->userId);
  }
  
  adb_ncGetConsoleOutputType_set_instanceId(request, env, instanceId);
  adb_ncGetConsoleOutput_set_ncGetConsoleOutput(input, env, request);
  
  /* do it */
  {
    adb_ncGetConsoleOutputResponse_t * output = axis2_stub_op_EucalyptusNC_ncGetConsoleOutput(stub, env, input);
    if (!output) {
      logprintfl(EUCAERROR, "GetConsoleOutputInstance returned NULL\n");
      *consoleOutput = NULL;
      return(1);
    } else {
      adb_ncGetConsoleOutputResponseType_t * response;
      response      = adb_ncGetConsoleOutputResponse_get_ncGetConsoleOutputResponse(output, env);
      *consoleOutput = adb_ncGetConsoleOutputResponseType_get_consoleOutput(response, env);
      printf("CONSOLE OUTPUT: %s\n", *consoleOutput);
    }
  }
  
  return(0);
}

int ncRebootInstanceStub (ncStub *st, ncMetadata *meta, char *instanceId) {
  int status;
  axutil_env_t * env = st->env;
  axis2_stub_t * stub = st->stub;
  
  adb_ncRebootInstance_t     * input   = adb_ncRebootInstance_create(env); 
  adb_ncRebootInstanceType_t * request = adb_ncRebootInstanceType_create(env);
  
  /* set input fields */
  if (meta) {
    adb_ncRebootInstanceType_set_correlationId(request, env, meta->correlationId);
    adb_ncRebootInstanceType_set_userId(request, env, meta->userId);
  }
  
  adb_ncRebootInstanceType_set_instanceId(request, env, instanceId);
  adb_ncRebootInstance_set_ncRebootInstance(input, env, request);
  
  status = 0;
  /* do it */
  {
    adb_ncRebootInstanceResponse_t * output = axis2_stub_op_EucalyptusNC_ncRebootInstance(stub, env, input);
    
    if (!output) {
      logprintfl(EUCAERROR, "RebootInstanceInstance returned NULL\n");
      return(1);
    } else {
      adb_ncRebootInstanceResponseType_t * response;
      response      = adb_ncRebootInstanceResponse_get_ncRebootInstanceResponse(output, env);
      status = adb_ncRebootInstanceResponseType_get_status(response, env);
    }
  }
  return(status);
}

int ncTerminateInstanceStub (ncStub *st, ncMetadata *meta, char *instId, int *shutdownState, int *previousState)
{
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;

    adb_ncTerminateInstance_t     * input   = adb_ncTerminateInstance_create(env); 
    adb_ncTerminateInstanceType_t * request = adb_ncTerminateInstanceType_create(env);

    /* set input fields */
    if (meta) {
        adb_ncTerminateInstanceType_set_correlationId(request, env, meta->correlationId);
        adb_ncTerminateInstanceType_set_userId(request, env, meta->userId);
    }
    adb_ncTerminateInstanceType_set_instanceId(request, env, instId);
    adb_ncTerminateInstance_set_ncTerminateInstance(input, env, request);
    
    /* do it */
    {
        adb_ncTerminateInstanceResponse_t * output = axis2_stub_op_EucalyptusNC_ncTerminateInstance(stub, env, input);
        
        if (!output) {
	  // suppress this message because we conservatively call on all nodes
	  //logprintfl(EUCAERROR, "TerminateInstance returned NULL\n");
	  return(1);
        } else {
            adb_ncTerminateInstanceResponseType_t * response;
        
            response      = adb_ncTerminateInstanceResponse_get_ncTerminateInstanceResponse(output, env);
            /* TODO: fix the state char->int conversion */
            * shutdownState = 0; //strdup(adb_ncTerminateInstanceResponseType_get_shutdownState(response, env));
            * previousState = 0; //strdup(adb_ncTerminateInstanceResponseType_get_previousState(response, env));
        }
    }
    
    return(0);
}

int ncDescribeInstancesStub (ncStub *st, ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;
    adb_ncDescribeInstances_t     * input   = adb_ncDescribeInstances_create(env); 
    adb_ncDescribeInstancesType_t * request = adb_ncDescribeInstancesType_create(env);
    int i;

    /* set input fields */
    if (meta) {
        adb_ncDescribeInstancesType_set_correlationId(request, env, meta->correlationId);
        adb_ncDescribeInstancesType_set_userId(request, env, meta->userId);
    }
    for (i=0; i<instIdsLen; i++) {
        adb_ncDescribeInstancesType_set_instanceIds_at(request, env, i, instIds[i]);
    }
    adb_ncDescribeInstances_set_ncDescribeInstances(input, env, request);
    
    /* do it */
    {
        adb_ncDescribeInstancesResponse_t * output = axis2_stub_op_EucalyptusNC_ncDescribeInstances(stub, env, input);

        if (!output) {
	  logprintfl(EUCAERROR, "DescribeInstances returned NULL\n");
	  return (1);
        } else {
            adb_ncDescribeInstancesResponseType_t * response = adb_ncDescribeInstancesResponse_get_ncDescribeInstancesResponse(output, env);

            * outInstsLen = adb_ncDescribeInstancesResponseType_sizeof_instances(response, env);
            * outInsts = malloc (sizeof(* outInstsLen));
            if ( * outInsts == NULL ) { 
                * outInstsLen = 0;
                return (2);
            }
            
            for (i=0; i<*outInstsLen; i++) {
                adb_ncInstanceType_t * instance = adb_ncDescribeInstancesResponseType_get_instances_at(response, env, i);
                axutil_date_time_t * dt = adb_ncInstanceType_get_launchTime(instance, env);
		adb_netConfigType_t *netconf;
		ncNetConf ncnet;

		adb_virtualMachineType_t *vm;
		ncInstParams params;

		bzero(&params, sizeof(ncInstParams));
		vm = adb_ncInstanceType_get_instanceType(instance, env);
		if (vm != NULL) {
		  // TODO Type Name
		  params.memorySize = adb_virtualMachineType_get_memory(vm, env);
		  params.diskSize = adb_virtualMachineType_get_disk(vm, env);
		  params.numberOfCores = adb_virtualMachineType_get_cores(vm, env);
		}
		
		bzero(&ncnet, sizeof(ncNetConf));
		netconf = adb_ncInstanceType_get_netParams(instance, env);
		if (netconf != NULL) {
		  ncnet.vlan = adb_netConfigType_get_vlan(netconf, env);
		  strncpy(ncnet.privateMac, adb_netConfigType_get_privateMacAddress(netconf, env), 32);
		  strncpy(ncnet.publicMac, adb_netConfigType_get_publicMacAddress(netconf, env), 32);
		  strncpy(ncnet.privateIp, adb_netConfigType_get_privateIp(netconf, env), 32);
		  strncpy(ncnet.publicIp, adb_netConfigType_get_publicIp(netconf, env), 32);
		}

                ncInstance * outInst = allocate_instance(
                    (char *)adb_ncInstanceType_get_instanceId(instance, env),
                    (char *)adb_ncInstanceType_get_reservationId(instance, env),
		    &params,
                    (char *)adb_ncInstanceType_get_imageId(instance, env),
                    (char *)adb_ncInstanceType_get_imageURL(instance, env),
                    (char *)adb_ncInstanceType_get_kernelId(instance, env),
                    (char *)adb_ncInstanceType_get_kernelURL(instance, env),
                    (char *)adb_ncInstanceType_get_ramdiskId(instance, env),
                    (char *)adb_ncInstanceType_get_ramdiskURL(instance, env),
                    (char *)adb_ncInstanceType_get_stateName(instance, env),
                    atoi(adb_ncInstanceType_get_stateCode(instance, env)),
                    (char *)adb_ncInstanceType_get_userId(instance, env),
		    &ncnet,
                    (char *)adb_ncInstanceType_get_keyName(instance, env));
                outInst->launchTime = datetime_to_unix (dt, env);
                axutil_date_time_free(dt, env);
                (* outInsts)[i] = outInst;
            }
        }
    }
    
    return (0);
}

int ncDescribeResourceStub (ncStub *st, ncMetadata *meta, char *resourceType, ncResource **outRes)
{
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;
    adb_ncDescribeResource_t     * input   = adb_ncDescribeResource_create(env); 
    adb_ncDescribeResourceType_t * request = adb_ncDescribeResourceType_create(env);

    /* set input fields */
    if (meta) {
        adb_ncDescribeResourceType_set_correlationId(request, env, meta->correlationId);
        adb_ncDescribeResourceType_set_userId(request, env, meta->userId);
    }
    if (resourceType) {
        adb_ncDescribeResourceType_set_resourceType(request, env, resourceType);
    }
    adb_ncDescribeResource_set_ncDescribeResource(input, env, request);
    
    /* do it */
    {
        adb_ncDescribeResourceResponse_t * output = axis2_stub_op_EucalyptusNC_ncDescribeResource(stub, env, input);

        if (!output) {
	  logprintfl(EUCAERROR, "DescribeResource returned NULL\n");
	  return(1);
        } else {
            adb_ncDescribeResourceResponseType_t * response = adb_ncDescribeResourceResponse_get_ncDescribeResourceResponse(output, env);
            ncResource * res = allocate_resource(
                (char *)adb_ncDescribeResourceResponseType_get_nodeStatus(response, env),
                (int)adb_ncDescribeResourceResponseType_get_memorySizeMax(response, env),
                (int)adb_ncDescribeResourceResponseType_get_memorySizeAvailable(response, env),
                (int)adb_ncDescribeResourceResponseType_get_diskSizeMax(response, env),
                (int)adb_ncDescribeResourceResponseType_get_diskSizeAvailable(response, env),
                (int)adb_ncDescribeResourceResponseType_get_numberOfCoresMax(response, env),
                (int)adb_ncDescribeResourceResponseType_get_numberOfCoresAvailable(response, env),
                (char *)adb_ncDescribeResourceResponseType_get_publicSubnets(response, env));
            if (!res) return (2);
            * outRes = res;
        }
    }
    
    return(0);
}

int ncStartNetworkStub  (ncStub *st, ncMetadata *meta, char **peers, int peersLen, int port, int vlan, char **outStatus) {
  axutil_env_t * env = st->env;
  axis2_stub_t * stub = st->stub;
  
  adb_ncStartNetwork_t *input;
  adb_ncStartNetworkResponse_t *output;
  adb_ncStartNetworkType_t *sn;
  adb_ncStartNetworkResponseType_t *snrt;

  int i;
  
  sn = adb_ncStartNetworkType_create(env);
  input = adb_ncStartNetwork_create(env);
  
  adb_ncStartNetworkType_set_userId(sn, env, meta->userId);
  adb_ncStartNetworkType_set_correlationId(sn, env, meta->correlationId);
  adb_ncStartNetworkType_set_vlan(sn, env, vlan);
  adb_ncStartNetworkType_set_remoteHostPort(sn, env, port);
  for (i=0; i<peersLen; i++) {
    adb_ncStartNetworkType_add_remoteHosts(sn, env, peers[i]);
  }
  
  adb_ncStartNetwork_set_ncStartNetwork(input, env, sn);
  
  output = axis2_stub_op_EucalyptusNC_ncStartNetwork(stub, env, input);
  if (!output) {
    logprintfl(EUCAERROR, "StartNetwork returned NULL\n");
    return(1);
  }
  snrt = adb_ncStartNetworkResponse_get_ncStartNetworkResponse(output, env);
  //    logprintf("startnetwork returned status %s\n", adb_ncStartNetworkResponseType_get_networkStatus(snrt, env));
  if (outStatus != NULL) {
    *outStatus = strdup(adb_ncStartNetworkResponseType_get_networkStatus(snrt, env));
  }
  return(0);
}

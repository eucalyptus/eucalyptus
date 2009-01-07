#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pthread.h>

#include <server-marshal.h>
#include <handlers.h>
#include <misc.h>

pthread_mutex_t ncHandlerLock = PTHREAD_MUTEX_INITIALIZER;

adb_ncStartNetworkResponse_t* ncStartNetworkMarshal (adb_ncStartNetwork_t* ncStartNetwork, const axutil_env_t *env) {
  pthread_mutex_lock(&ncHandlerLock);
  // output vars
  adb_ncStartNetworkResponse_t *ret=NULL;
  adb_ncStartNetworkResponseType_t *snrt=NULL;
  
  //input vars
  adb_ncStartNetworkType_t *snt=NULL;
  
  // working vars
  int rc, i, status=0;
  char **peers, *user, *cid;
  int peersLen, port, vlan;
  ncMetadata ccMeta;
  
  snt = adb_ncStartNetwork_get_ncStartNetwork(ncStartNetwork, env);
  ccMeta.correlationId = adb_ncStartNetworkType_get_correlationId(snt, env);
  ccMeta.userId = adb_ncStartNetworkType_get_userId(snt, env);
  eventlog("NC", ccMeta.userId, ccMeta.correlationId, "startNetwork", "begin");  
  user = adb_ncStartNetworkType_get_userId(snt, env);
  cid = adb_ncStartNetworkType_get_correlationId(snt, env);

  port = adb_ncStartNetworkType_get_remoteHostPort(snt, env);
  vlan = adb_ncStartNetworkType_get_vlan(snt, env);

  peersLen = adb_ncStartNetworkType_sizeof_remoteHosts(snt, env);
  peers = malloc(sizeof(char *) * peersLen);
  for (i=0; i<peersLen; i++) {
    peers[i] = adb_ncStartNetworkType_get_remoteHosts_at(snt, env, i);
  }

  snrt = adb_ncStartNetworkResponseType_create(env);

  rc = doncStartNetwork(&ccMeta, peers, peersLen, port, vlan);
  
  free(peers);
  if (rc) {
    logprintfl (EUCAERROR, "ERROR: doncStartNetwork() returned fail %d\n", rc);
    adb_ncStartNetworkResponseType_set_networkStatus(snrt, env, "FAIL");
    status = 2;
  } else {
    adb_ncStartNetworkResponseType_set_networkStatus(snrt, env, "SUCCESS");
  }
  
  adb_ncStartNetworkResponseType_set_correlationId(snrt, env, cid);
  adb_ncStartNetworkResponseType_set_userId(snrt, env, user);
  adb_ncStartNetworkResponseType_set_statusMessage(snrt, env, status);

  ret = adb_ncStartNetworkResponse_create(env);
  adb_ncStartNetworkResponse_set_ncStartNetworkResponse(ret, env, snrt);

  pthread_mutex_unlock(&ncHandlerLock);
  eventlog("NC", ccMeta.userId, ccMeta.correlationId, "startNetwork", "end");
  return(ret);
}

adb_ncDescribeResourceResponse_t* ncDescribeResourceMarshal (adb_ncDescribeResource_t* ncDescribeResource, const axutil_env_t *env)
{
  pthread_mutex_lock(&ncHandlerLock);
    adb_ncDescribeResourceType_t * input = adb_ncDescribeResource_get_ncDescribeResource(ncDescribeResource, env);
    adb_ncDescribeResourceResponse_t * response = NULL;

    /* get fields from input */
    axis2_char_t * correlationId = adb_ncDescribeResourceType_get_correlationId(input, env);
    axis2_char_t * userId = adb_ncDescribeResourceType_get_userId(input, env);
    axis2_char_t * resourceType = adb_ncDescribeResourceType_get_resourceType(input, env);

    eventlog("NC", userId, correlationId, "describeResource", "begin");
    { /* do it */
        ncMetadata meta = { correlationId, userId };
        ncResource * outRes;

        int error = doDescribeResource (&meta, resourceType, &outRes);

        if (error) {
            logprintfl (EUCAERROR, "ERROR: doDescribeResource() failed error=%d\n", error);

        } else {
            adb_ncDescribeResourceResponseType_t * output = adb_ncDescribeResourceResponseType_create(env);
            
            /* set fields in output */
            adb_ncDescribeResourceResponseType_set_correlationId(output, env, correlationId);
            adb_ncDescribeResourceResponseType_set_userId(output, env, userId);
            adb_ncDescribeResourceResponseType_set_nodeStatus(output, env, outRes->nodeStatus);
            adb_ncDescribeResourceResponseType_set_memorySizeMax(output, env, outRes->memorySizeMax);
            adb_ncDescribeResourceResponseType_set_memorySizeAvailable(output, env, outRes->memorySizeAvailable);
            adb_ncDescribeResourceResponseType_set_diskSizeMax(output, env, outRes->diskSizeMax);
            adb_ncDescribeResourceResponseType_set_diskSizeAvailable(output, env, outRes->diskSizeAvailable);
            adb_ncDescribeResourceResponseType_set_numberOfCoresMax(output, env, outRes->numberOfCoresMax);
            adb_ncDescribeResourceResponseType_set_numberOfCoresAvailable(output, env, outRes->numberOfCoresAvailable);
            adb_ncDescribeResourceResponseType_set_publicSubnets(output, env, outRes->publicSubnets);
            free_resource ( &outRes);

            /* set response to output */
            response = adb_ncDescribeResourceResponse_create(env);
            adb_ncDescribeResourceResponse_set_ncDescribeResourceResponse(response, env, output);
        }
    }
    pthread_mutex_unlock(&ncHandlerLock);
    eventlog("NC", userId, correlationId, "describeResource", "end");
    return response;
}

adb_ncRunInstanceResponse_t* ncRunInstanceMarshal (adb_ncRunInstance_t* ncRunInstance, const axutil_env_t *env)
{
  pthread_mutex_lock(&ncHandlerLock);
    adb_ncRunInstanceType_t * input = adb_ncRunInstance_get_ncRunInstance(ncRunInstance, env);
    adb_ncRunInstanceResponse_t * response = NULL;
    ncInstParams params;

    /* get fields from input */
    axis2_char_t * correlationId = adb_ncRunInstanceType_get_correlationId(input, env);
    axis2_char_t * userId = adb_ncRunInstanceType_get_userId(input, env);
    axis2_char_t * instanceId = adb_ncRunInstanceType_get_instanceId(input, env);
    axis2_char_t * reservationId = adb_ncRunInstanceType_get_reservationId(input, env);
    adb_virtualMachineType_t * vm_type = adb_ncRunInstanceType_get_instanceType(input, env);
    axis2_char_t * imageId = adb_ncRunInstanceType_get_imageId(input, env);
    axis2_char_t * imageURL = adb_ncRunInstanceType_get_imageURL(input, env);
    axis2_char_t * kernelId = adb_ncRunInstanceType_get_kernelId(input, env);
    axis2_char_t * kernelURL = adb_ncRunInstanceType_get_kernelURL(input, env);
    axis2_char_t * ramdiskId = adb_ncRunInstanceType_get_ramdiskId(input, env);
    axis2_char_t * ramdiskURL = adb_ncRunInstanceType_get_ramdiskURL(input, env);
    axis2_char_t * keyName = adb_ncRunInstanceType_get_keyName(input, env);
    axis2_char_t * privateMac = adb_ncRunInstanceType_get_privateMacAddress(input, env);
    axis2_char_t * publicMac = adb_ncRunInstanceType_get_publicMacAddress(input, env);
    int vlan = adb_ncRunInstanceType_get_vlan(input, env);
    
    {
      char other[256];
      snprintf(other, 256, "begin,%s", reservationId);
      eventlog("NC", userId, correlationId, "runInstance", other);
    }

    params.memorySize = adb_virtualMachineType_get_memory(vm_type, env);
    params.numberOfCores = adb_virtualMachineType_get_cores(vm_type, env);
    params.diskSize = adb_virtualMachineType_get_disk(vm_type, env);
    

    { /* do it */
      ncMetadata meta = { correlationId, userId };
      ncInstance * outInst;
      
      int error = doRunInstance (&meta, instanceId, reservationId, &params, imageId, imageURL, kernelId, kernelURL, ramdiskId, ramdiskURL, keyName, privateMac, publicMac, vlan, &outInst);

        if (error) {
            logprintfl (EUCAERROR, "ERROR: doRunInstance() failed error=%d\n", error);

        } else {
            adb_ncRunInstanceResponseType_t * output = adb_ncRunInstanceResponseType_create(env);
            adb_ncInstanceType_t * instance = adb_ncInstanceType_create(env);
            adb_virtualMachineType_t * vm_type = adb_virtualMachineType_create(env);
	    adb_netConfigType_t *netconf = adb_netConfigType_create(env);
            axutil_date_time_t *dt = axutil_date_time_create_with_offset(env, outInst->launchTime - time(NULL));
            char numbuf [256];

            /* set fields in instance */
            adb_ncInstanceType_set_instanceId(instance, env, outInst->instanceId);
            adb_ncInstanceType_set_reservationId(instance, env, outInst->reservationId);
            adb_ncInstanceType_set_keyName(instance, env, outInst->keyName);

            adb_virtualMachineType_set_memory(vm_type, env, outInst->params.memorySize);
            adb_virtualMachineType_set_cores(vm_type, env, outInst->params.numberOfCores);
            adb_virtualMachineType_set_disk(vm_type, env, outInst->params.diskSize);
            adb_ncInstanceType_set_instanceType(instance, env, vm_type);

	    adb_netConfigType_set_privateMacAddress(netconf, env, outInst->ncnet.privateMac);
	    adb_netConfigType_set_publicMacAddress(netconf, env, outInst->ncnet.publicMac);
	    adb_netConfigType_set_privateIp(netconf, env, outInst->ncnet.privateIp);
	    adb_netConfigType_set_publicIp(netconf, env, outInst->ncnet.publicIp);
	    adb_netConfigType_set_vlan(netconf, env, outInst->ncnet.vlan);
	    adb_ncInstanceType_set_netParams(instance, env, netconf);

            adb_ncInstanceType_set_imageId(instance, env, outInst->imageId);
            adb_ncInstanceType_set_imageURL(instance, env, outInst->imageURL);
            adb_ncInstanceType_set_kernelId(instance, env, outInst->kernelId);
            adb_ncInstanceType_set_kernelURL(instance, env, outInst->kernelURL);
            adb_ncInstanceType_set_ramdiskId(instance, env, outInst->ramdiskId);
            adb_ncInstanceType_set_ramdiskURL(instance, env, outInst->ramdiskURL);
            adb_ncInstanceType_set_stateName(instance, env, outInst->stateName);
            snprintf(numbuf, 256, "%d", outInst->stateCode);
            adb_ncInstanceType_set_stateCode(instance, env, numbuf);
            adb_ncInstanceType_set_userId(instance, env, outInst->userId);
            adb_ncInstanceType_set_launchTime(instance, env, dt);
            /* TODO: should we free_instance(&outInst) here or not? currently you don't have to */

            /* set fields in output */
            adb_ncRunInstanceResponseType_set_instance(output, env, instance);
            adb_ncRunInstanceResponseType_set_correlationId(output, env, correlationId);
            adb_ncRunInstanceResponseType_set_userId(output, env, userId);
            
            /* set response to output */
            response = adb_ncRunInstanceResponse_create(env);
            adb_ncRunInstanceResponse_set_ncRunInstanceResponse(response, env, output);
        }
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "runInstance", "end");
    return response;
}

adb_ncDescribeInstancesResponse_t* ncDescribeInstancesMarshal (adb_ncDescribeInstances_t* ncDescribeInstances, const axutil_env_t *env)
{
  pthread_mutex_lock(&ncHandlerLock);
    adb_ncDescribeInstancesType_t * input = adb_ncDescribeInstances_get_ncDescribeInstances(ncDescribeInstances, env);
    adb_ncDescribeInstancesResponse_t * response = NULL;

    /* get fields from input */
    axis2_char_t * correlationId = adb_ncDescribeInstancesType_get_correlationId(input, env);
    axis2_char_t * userId = adb_ncDescribeInstancesType_get_userId(input, env);
    int instIdsLen = adb_ncDescribeInstancesType_sizeof_instanceIds(input, env);
    char ** instIds = malloc(sizeof(char *) * instIdsLen);
    int i;

    eventlog("NC", userId, correlationId, "describeInstances", "begin");

    if (instIds==NULL) {
      pthread_mutex_unlock(&ncHandlerLock);
      return response; /* malloc failed */
    }

    for (i=0; i<instIdsLen; i++) {
        instIds[i] = adb_ncDescribeInstancesType_get_instanceIds_at(input, env, i);
    }

    { /* do it */
        ncMetadata meta = { correlationId, userId };
        ncInstance **outInsts;
        int outInstsLen;

        int error = doDescribeInstances (&meta, instIds, instIdsLen, &outInsts, &outInstsLen);

        if (error) {
            logprintfl (EUCAERROR, "ERROR: doDescribeInstances() failed error=%d\n", error);
            
        } else {
            adb_ncDescribeInstancesResponseType_t * output = adb_ncDescribeInstancesResponseType_create(env);

            for (i=0; i<outInstsLen; i++) {
                adb_ncInstanceType_t * instance = adb_ncInstanceType_create(env);
                adb_virtualMachineType_t * vm_type = adb_virtualMachineType_create(env);
		adb_netConfigType_t * netconf = adb_netConfigType_create(env);
                ncInstance * outInst = outInsts[i];
                axutil_date_time_t *dt = axutil_date_time_create_with_offset(env, outInst->launchTime - time(NULL));
                char numbuf [256];
        
                adb_ncInstanceType_set_instanceId(instance, env, outInst->instanceId);
                adb_ncInstanceType_set_reservationId(instance, env, outInst->reservationId);
                adb_ncInstanceType_set_keyName(instance, env, outInst->keyName);
                adb_virtualMachineType_set_memory(vm_type, env, outInst->params.memorySize);
                adb_virtualMachineType_set_cores(vm_type, env, outInst->params.numberOfCores);
                adb_virtualMachineType_set_disk(vm_type, env, outInst->params.diskSize);
                adb_ncInstanceType_set_instanceType(instance, env, vm_type);

		adb_netConfigType_set_privateMacAddress(netconf, env, outInst->ncnet.privateMac);
		adb_netConfigType_set_publicMacAddress(netconf, env, outInst->ncnet.publicMac);
		adb_netConfigType_set_privateIp(netconf, env, outInst->ncnet.privateIp);
		adb_netConfigType_set_publicIp(netconf, env, outInst->ncnet.publicIp);
		adb_netConfigType_set_vlan(netconf, env, outInst->ncnet.vlan);
		adb_ncInstanceType_set_netParams(instance, env, netconf);

                adb_ncInstanceType_set_imageId(instance, env, outInst->imageId);
                adb_ncInstanceType_set_imageURL(instance, env, outInst->imageURL);
                adb_ncInstanceType_set_kernelId(instance, env, outInst->kernelId);
                adb_ncInstanceType_set_kernelURL(instance, env, outInst->kernelURL);
                adb_ncInstanceType_set_ramdiskId(instance, env, outInst->ramdiskId);
                adb_ncInstanceType_set_ramdiskURL(instance, env, outInst->ramdiskURL);
                adb_ncInstanceType_set_stateName(instance, env, outInst->stateName);
                snprintf(numbuf, 256, "%d", outInst->stateCode);
                adb_ncInstanceType_set_stateCode(instance, env, numbuf);
                adb_ncInstanceType_set_userId(instance, env, outInst->userId);
                adb_ncInstanceType_set_launchTime(instance, env, dt);
                /* TODO: should we free_instance(&outInst) here or not? currently you only have to free outInsts[] */
                
                adb_ncDescribeInstancesResponseType_add_instances(output, env, instance);
            }
            
            /* set fields in output */
            adb_ncDescribeInstancesResponseType_set_correlationId(output, env, correlationId);
            adb_ncDescribeInstancesResponseType_set_userId(output, env, userId);
            
            /* set response to output */
            response = adb_ncDescribeInstancesResponse_create(env);
            adb_ncDescribeInstancesResponse_set_ncDescribeInstancesResponse(response, env, output);

            if (outInstsLen)
                free ( outInsts );
        }
    }
    pthread_mutex_unlock(&ncHandlerLock);
    eventlog("NC", userId, correlationId, "describeInstances", "end");
    return response;
}

adb_ncRebootInstanceResponse_t* ncRebootInstanceMarshal (adb_ncRebootInstance_t* ncRebootInstance,  const axutil_env_t *env) {
  pthread_mutex_lock(&ncHandlerLock);

  adb_ncRebootInstanceType_t * input = adb_ncRebootInstance_get_ncRebootInstance(ncRebootInstance, env);
  adb_ncRebootInstanceResponse_t * response = NULL;
  
  /* get fields from input */
  axis2_char_t * correlationId = adb_ncRebootInstanceType_get_correlationId(input, env);
  axis2_char_t * userId = adb_ncRebootInstanceType_get_userId(input, env);
  axis2_char_t * instanceId = adb_ncRebootInstanceType_get_instanceId(input, env);
  
  eventlog("NC", userId, correlationId, "rebootInstance", "begin");
  
  { /* do it */
    ncMetadata meta = { correlationId, userId };
    
    int error = doRebootInstance (&meta, instanceId);
    
    if (error) {
      logprintfl (EUCAERROR, "ERROR: doRebootInstance() failed error=%d\n", error);
    } else {
      adb_ncRebootInstanceResponseType_t * output = adb_ncRebootInstanceResponseType_create(env);
      char s[128];
      
      /* set fields in output */
      adb_ncRebootInstanceResponseType_set_correlationId(output, env, correlationId);
      adb_ncRebootInstanceResponseType_set_userId(output, env, userId);
      adb_ncRebootInstanceResponseType_set_status(output, env, 0);
      
      /* set response to output */
      response = adb_ncRebootInstanceResponse_create(env);
      adb_ncRebootInstanceResponse_set_ncRebootInstanceResponse(response, env, output);
    }
  }
  pthread_mutex_unlock(&ncHandlerLock);
  
  eventlog("NC", userId, correlationId, "rebootInstance", "end");
  return response;
}

adb_ncGetConsoleOutputResponse_t* ncGetConsoleOutputMarshal (adb_ncGetConsoleOutput_t* ncGetConsoleOutput, const axutil_env_t *env) {
  pthread_mutex_lock(&ncHandlerLock);

  adb_ncGetConsoleOutputType_t * input = adb_ncGetConsoleOutput_get_ncGetConsoleOutput(ncGetConsoleOutput, env);
  adb_ncGetConsoleOutputResponse_t * response = NULL;
  
  /* get fields from input */
  axis2_char_t * correlationId = adb_ncGetConsoleOutputType_get_correlationId(input, env);
  axis2_char_t * userId = adb_ncGetConsoleOutputType_get_userId(input, env);
  axis2_char_t * instanceId = adb_ncGetConsoleOutputType_get_instanceId(input, env);
  
  char *consoleOutput;
  
  eventlog("NC", userId, correlationId, "getConsoleOutput", "begin");
  
  { /* do it */
    ncMetadata meta = { correlationId, userId };
    
    int error = doncGetConsoleOutput (&meta, instanceId, &consoleOutput);
    
    if (error) {
      logprintfl (EUCAERROR, "ERROR: doGetConsoleOutput() failed error=%d\n", error);
      
    } else {
      adb_ncGetConsoleOutputResponseType_t * output = adb_ncGetConsoleOutputResponseType_create(env);
      char s[128];
      
      /* set fields in output */
      adb_ncGetConsoleOutputResponseType_set_correlationId(output, env, correlationId);
      adb_ncGetConsoleOutputResponseType_set_userId(output, env, userId);
      
      adb_ncGetConsoleOutputResponseType_set_consoleOutput(output, env, consoleOutput);
      
      /* set response to output */
      response = adb_ncGetConsoleOutputResponse_create(env);
      adb_ncGetConsoleOutputResponse_set_ncGetConsoleOutputResponse(response, env, output);
    }
  }
  pthread_mutex_unlock(&ncHandlerLock);
  
  eventlog("NC", userId, correlationId, "getConsoleOutput", "end");
  return response;
}

adb_ncTerminateInstanceResponse_t* ncTerminateInstanceMarshal (adb_ncTerminateInstance_t* ncTerminateInstance, const axutil_env_t *env)
{
  pthread_mutex_lock(&ncHandlerLock);
    adb_ncTerminateInstanceType_t * input = adb_ncTerminateInstance_get_ncTerminateInstance(ncTerminateInstance, env);
    adb_ncTerminateInstanceResponse_t * response = NULL;

    /* get fields from input */
    axis2_char_t * correlationId = adb_ncTerminateInstanceType_get_correlationId(input, env);
    axis2_char_t * userId = adb_ncTerminateInstanceType_get_userId(input, env);
    axis2_char_t * instanceId = adb_ncTerminateInstanceType_get_instanceId(input, env);

    eventlog("NC", userId, correlationId, "terminateInstance", "begin");

    { /* do it */
        ncMetadata meta = { correlationId, userId };
        int shutdownState, previousState;

        int error = doTerminateInstance (&meta, instanceId, &shutdownState, &previousState);

        if (error) {
            logprintfl (EUCAERROR, "ERROR: doTerminateInstance() failed error=%d\n", error);

        } else {
            adb_ncTerminateInstanceResponseType_t * output = adb_ncTerminateInstanceResponseType_create(env);
            char s[128];

            /* set fields in output */
            adb_ncTerminateInstanceResponseType_set_correlationId(output, env, correlationId);
            adb_ncTerminateInstanceResponseType_set_userId(output, env, userId);
            adb_ncTerminateInstanceResponseType_set_instanceId(output, env, instanceId);

            /* TODO: change the WSDL to use the name/code pair */
            snprintf (s, 128, "%d", shutdownState);
            adb_ncTerminateInstanceResponseType_set_shutdownState(output, env, s);
            snprintf (s, 128, "%d", previousState);
            adb_ncTerminateInstanceResponseType_set_previousState(output, env, s);

            /* set response to output */
            response = adb_ncTerminateInstanceResponse_create(env);
            adb_ncTerminateInstanceResponse_set_ncTerminateInstanceResponse(response, env, output);
        }
    }
    pthread_mutex_unlock(&ncHandlerLock);

    eventlog("NC", userId, correlationId, "terminateInstance", "end");
    return response;
}

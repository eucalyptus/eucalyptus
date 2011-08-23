// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <pthread.h>

#include <server-marshal.h>
#define HANDLERS_FANOUT
#include <handlers.h>
#include <misc.h>
#include <data.h>
#include <adb-helpers.h>

#include <windows-bundle.h>

pthread_mutex_t ncHandlerLock = PTHREAD_MUTEX_INITIALIZER;

void adb_InitService(void) {
}

adb_ncAssignAddressResponse_t* ncAssignAddressMarshal (adb_ncAssignAddress_t* ncAssignAddress, const axutil_env_t *env) {
  pthread_mutex_lock(&ncHandlerLock);
  adb_ncAssignAddressType_t * input          = adb_ncAssignAddress_get_ncAssignAddress(ncAssignAddress, env);
  adb_ncAssignAddressResponse_t * response   = adb_ncAssignAddressResponse_create(env);
  adb_ncAssignAddressResponseType_t * output = adb_ncAssignAddressResponseType_create(env);

  char * instanceId = adb_ncAssignAddressType_get_instanceId(input, env);
  char * publicIp = adb_ncAssignAddressType_get_publicIp(input, env);
  
  // get operation-specific fields from input
  { 
    ncMetadata meta;
    EUCA_MESSAGE_UNMARSHAL(ncAssignAddressType, input, (&meta));
    
    int error = doAssignAddress (&meta, instanceId, publicIp);
    
    if (error) {
      logprintfl (EUCAERROR, "ERROR: doAssignAddress() failed error=%d\n", error);
      adb_ncAssignAddressResponseType_set_correlationId(output, env, meta.correlationId);
      adb_ncAssignAddressResponseType_set_userId(output, env, meta.userId);
      adb_ncAssignAddressResponseType_set_return(output, env, AXIS2_FALSE);
      
      // set operation-specific fields in output
      adb_ncAssignAddressResponseType_set_statusMessage(output, env, "2");
      
    } else {
      // set standard fields in output
      adb_ncAssignAddressResponseType_set_return(output, env, AXIS2_TRUE);
      adb_ncAssignAddressResponseType_set_correlationId(output, env, meta.correlationId);
      adb_ncAssignAddressResponseType_set_userId(output, env, meta.userId);
      
      // set operation-specific fields in output
      adb_ncAssignAddressResponseType_set_statusMessage(output, env, "0");
    }
  }
  
  // set response to output
  adb_ncAssignAddressResponse_set_ncAssignAddressResponse(response, env, output);  
  pthread_mutex_unlock(&ncHandlerLock);
  return response;
}

adb_ncPowerDownResponse_t* ncPowerDownMarshal (adb_ncPowerDown_t* ncPowerDown, const axutil_env_t *env) 
{
  //    pthread_mutex_lock(&ncHandlerLock);
    adb_ncPowerDownType_t * input          = adb_ncPowerDown_get_ncPowerDown(ncPowerDown, env);
    adb_ncPowerDownResponse_t * response   = adb_ncPowerDownResponse_create(env);
    adb_ncPowerDownResponseType_t * output = adb_ncPowerDownResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncPowerDownType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncPowerDownType_get_userId(input, env);

    // get operation-specific fields from input
    fprintf(stderr, "powerdown called\n\n");
    //    eventlog("NC", userId, correlationId, "PowerDown", "begin");
    { // do it
      //        ncMetadata meta = { correlationId, userId };
        ncMetadata meta;
	EUCA_MESSAGE_UNMARSHAL(ncPowerDownType, input, (&meta));

        int error = doPowerDown (&meta);

        if (error) {
	  logprintfl (EUCAERROR, "ERROR: doPowerDown() failed error=%d\n", error);
	  adb_ncPowerDownResponseType_set_correlationId(output, env, meta.correlationId);
	  adb_ncPowerDownResponseType_set_userId(output, env, meta.userId);
	  adb_ncPowerDownResponseType_set_return(output, env, AXIS2_FALSE);
	  
	  // set operation-specific fields in output
	  adb_ncPowerDownResponseType_set_statusMessage(output, env, "2");
	  
        } else {
	  // set standard fields in output
	  adb_ncPowerDownResponseType_set_return(output, env, AXIS2_TRUE);
	  adb_ncPowerDownResponseType_set_correlationId(output, env, meta.correlationId);
	  adb_ncPowerDownResponseType_set_userId(output, env, meta.userId);
	  
	  // set operation-specific fields in output
	  adb_ncPowerDownResponseType_set_statusMessage(output, env, "0");
        }
    }

    // set response to output
    adb_ncPowerDownResponse_set_ncPowerDownResponse(response, env, output);
    //    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "PowerDown", "end");
    fprintf(stderr, "powerdown done\n");
    return response;
}

adb_ncStartNetworkResponse_t* ncStartNetworkMarshal (adb_ncStartNetwork_t* ncStartNetwork, const axutil_env_t *env) 
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncStartNetworkType_t * input          = adb_ncStartNetwork_get_ncStartNetwork(ncStartNetwork, env);
    adb_ncStartNetworkResponse_t * response   = adb_ncStartNetworkResponse_create(env);
    adb_ncStartNetworkResponseType_t * output = adb_ncStartNetworkResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncStartNetworkType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncStartNetworkType_get_userId(input, env);

    // get operation-specific fields from input
    char * uuid = adb_ncStartNetworkType_get_uuid(input, env);
    int port = adb_ncStartNetworkType_get_remoteHostPort(input, env);
    int vlan = adb_ncStartNetworkType_get_vlan(input, env);
    int peersLen = adb_ncStartNetworkType_sizeof_remoteHosts(input, env);
    char ** peers = malloc(sizeof(char *) * peersLen);
    int i;
    for (i=0; i<peersLen; i++) {
        peers[i] = adb_ncStartNetworkType_get_remoteHosts_at(input, env, i);
    }

    //    eventlog("NC", userId, correlationId, "StartNetwork", "begin");
    { // do it
      //ncMetadata meta = { correlationId, userId };
        ncMetadata meta;
	EUCA_MESSAGE_UNMARSHAL(ncStartNetworkType, input, (&meta));
        int error = doStartNetwork (&meta, uuid, peers, peersLen, port, vlan);

        if (error) {
            logprintfl (EUCAERROR, "ERROR: doStartNetwork() failed error=%d\n", error);
            adb_ncStartNetworkResponseType_set_return(output, env, AXIS2_FALSE);

            // set operation-specific fields in output
            adb_ncStartNetworkResponseType_set_networkStatus(output, env, "FAIL");
            adb_ncStartNetworkResponseType_set_statusMessage(output, env, "2");

        } else {
            // set standard fields in output
            adb_ncStartNetworkResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncStartNetworkResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncStartNetworkResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncStartNetworkResponseType_set_networkStatus(output, env, "SUCCESS");
            adb_ncStartNetworkResponseType_set_statusMessage(output, env, "0");
        }

        if (peersLen) 
            free (peers);
    }

    // set response to output
    adb_ncStartNetworkResponse_set_ncStartNetworkResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "StartNetwork", "end");
    return response;
}

adb_ncDescribeResourceResponse_t* ncDescribeResourceMarshal (adb_ncDescribeResource_t* ncDescribeResource, const axutil_env_t *env)
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncDescribeResourceType_t * input          = adb_ncDescribeResource_get_ncDescribeResource(ncDescribeResource, env);
    adb_ncDescribeResourceResponse_t * response   = adb_ncDescribeResourceResponse_create(env);
    adb_ncDescribeResourceResponseType_t * output = adb_ncDescribeResourceResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncDescribeResourceType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncDescribeResourceType_get_userId(input, env);

    // get operation-specific fields from input
    axis2_char_t * resourceType = adb_ncDescribeResourceType_get_resourceType(input, env);

    //    eventlog("NC", userId, correlationId, "DescribeResource", "begin");
    { // do it
      //        ncMetadata meta = { correlationId, userId };
        ncMetadata meta;
	EUCA_MESSAGE_UNMARSHAL(ncDescribeResourceType, input, (&meta));
        ncResource * outRes;

        int error = doDescribeResource (&meta, resourceType, &outRes);
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doDescribeResource() failed error=%d\n", error);
            adb_ncDescribeResourceResponseType_set_return(output, env, AXIS2_FALSE);

        } else {
            // set standard fields in output
            adb_ncDescribeResourceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncDescribeResourceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncDescribeResourceResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncDescribeResourceResponseType_set_nodeStatus(output, env, outRes->nodeStatus);
            adb_ncDescribeResourceResponseType_set_iqn(output, env, outRes->iqn);
            adb_ncDescribeResourceResponseType_set_memorySizeMax(output, env, outRes->memorySizeMax);
            adb_ncDescribeResourceResponseType_set_memorySizeAvailable(output, env, outRes->memorySizeAvailable);
            adb_ncDescribeResourceResponseType_set_diskSizeMax(output, env, outRes->diskSizeMax);
            adb_ncDescribeResourceResponseType_set_diskSizeAvailable(output, env, outRes->diskSizeAvailable);
            adb_ncDescribeResourceResponseType_set_numberOfCoresMax(output, env, outRes->numberOfCoresMax);
            adb_ncDescribeResourceResponseType_set_numberOfCoresAvailable(output, env, outRes->numberOfCoresAvailable);
            adb_ncDescribeResourceResponseType_set_publicSubnets(output, env, outRes->publicSubnets);
            free_resource ( &outRes);
            
        }
    }
    // set response to output
    adb_ncDescribeResourceResponse_set_ncDescribeResourceResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "DescribeResource", "end");
    return response;
}

// helper function used by RunInstance and DescribeInstances
static void copy_instance_to_adb (adb_instanceType_t * instance, const axutil_env_t *env, ncInstance * outInst) 
{
    // NOTE: the order of set operations reflects the order in the WSDL

    // passed into runInstances
    adb_instanceType_set_uuid(instance, env, outInst->uuid);
    adb_instanceType_set_reservationId(instance, env, outInst->reservationId);
    adb_instanceType_set_instanceId(instance, env, outInst->instanceId);
    adb_instanceType_set_imageId(instance, env, outInst->imageId);
    adb_instanceType_set_kernelId(instance, env, outInst->kernelId);
    adb_instanceType_set_ramdiskId(instance, env, outInst->ramdiskId);
    adb_instanceType_set_userId(instance, env, outInst->userId);
    adb_instanceType_set_ownerId(instance, env, outInst->ownerId);
    adb_instanceType_set_accountId(instance, env, outInst->accountId);
    adb_instanceType_set_keyName(instance, env, outInst->keyName);
    adb_instanceType_set_instanceType(instance, env, copy_vm_type_to_adb (env, &(outInst->params)));
    
    adb_netConfigType_t * netconf = adb_netConfigType_create(env);            
    adb_netConfigType_set_privateMacAddress(netconf, env, outInst->ncnet.privateMac);
    adb_netConfigType_set_privateIp(netconf, env, outInst->ncnet.privateIp);
    adb_netConfigType_set_publicIp(netconf, env, outInst->ncnet.publicIp);
    adb_netConfigType_set_vlan(netconf, env, outInst->ncnet.vlan);
    adb_netConfigType_set_networkIndex(netconf, env, outInst->ncnet.networkIndex);
    adb_instanceType_set_netParams(instance, env, netconf);
    
    // reported by NC
    adb_instanceType_set_stateName(instance, env, outInst->stateName);
    adb_instanceType_set_bundleTaskStateName(instance, env, outInst->bundleTaskStateName);
    adb_instanceType_set_createImageStateName(instance, env, outInst->createImageTaskStateName);
    axutil_date_time_t * dt = axutil_date_time_create_with_offset(env, outInst->launchTime - time(NULL));
    adb_instanceType_set_launchTime(instance, env, dt);
    adb_instanceType_set_blkbytes(instance, env, outInst->blkbytes);
    adb_instanceType_set_netbytes(instance, env, outInst->netbytes);


    // passed into RunInstances for safekeeping by NC
    adb_instanceType_set_userData(instance, env, outInst->userData);
    adb_instanceType_set_launchIndex(instance, env, outInst->launchIndex);
    adb_instanceType_set_platform(instance, env, outInst->platform);

    for (int i=0; i<outInst->groupNamesSize; i++) {
        adb_instanceType_add_groupNames(instance, env, outInst->groupNames[i]);
    }
    
    // updated by NC upon Attach/DetachVolume 
    for (int i=0; i<EUCA_MAX_VOLUMES; i++) {
        if (strlen(outInst->volumes[i].volumeId)==0)
            continue;
        adb_volumeType_t * volume = adb_volumeType_create(env);
        adb_volumeType_set_volumeId(volume, env, outInst->volumes[i].volumeId);
        adb_volumeType_set_remoteDev(volume, env, outInst->volumes[i].remoteDev);
        adb_volumeType_set_localDev(volume, env, outInst->volumes[i].localDev);
        adb_volumeType_set_state(volume, env, outInst->volumes[i].stateName);
        adb_instanceType_add_volumes(instance, env, volume);
    }
    
    // NOTE: serviceTag seen in the WSDL is unused in NC, used by CC 
}

adb_ncRunInstanceResponse_t* ncRunInstanceMarshal (adb_ncRunInstance_t* ncRunInstance, const axutil_env_t *env)
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncRunInstanceType_t * input          = adb_ncRunInstance_get_ncRunInstance(ncRunInstance, env);
    adb_ncRunInstanceResponse_t * response   = adb_ncRunInstanceResponse_create(env);
    adb_ncRunInstanceResponseType_t * output = adb_ncRunInstanceResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncRunInstanceType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncRunInstanceType_get_userId(input, env);

    // get operation-specific fields from input
    axis2_char_t * uuid = adb_ncRunInstanceType_get_uuid(input, env);
    axis2_char_t * instanceId = adb_ncRunInstanceType_get_instanceId(input, env);
    axis2_char_t * reservationId = adb_ncRunInstanceType_get_reservationId(input, env);
    virtualMachine params;
    copy_vm_type_from_adb (&params, adb_ncRunInstanceType_get_instanceType(input, env), env);
    axis2_char_t * imageId = adb_ncRunInstanceType_get_imageId(input, env);
    axis2_char_t * imageURL = adb_ncRunInstanceType_get_imageURL(input, env);
    axis2_char_t * kernelId = adb_ncRunInstanceType_get_kernelId(input, env);
    axis2_char_t * kernelURL = adb_ncRunInstanceType_get_kernelURL(input, env);
    axis2_char_t * ramdiskId = adb_ncRunInstanceType_get_ramdiskId(input, env);
    axis2_char_t * ramdiskURL = adb_ncRunInstanceType_get_ramdiskURL(input, env);
    axis2_char_t * ownerId = adb_ncRunInstanceType_get_ownerId(input, env);
    axis2_char_t * accountId = adb_ncRunInstanceType_get_accountId(input, env);
    axis2_char_t * keyName = adb_ncRunInstanceType_get_keyName(input, env);
    adb_netConfigType_t *net_type = adb_ncRunInstanceType_get_netParams(input, env);
    netConfig netparams;
    netparams.vlan = adb_netConfigType_get_vlan(net_type, env);
    netparams.networkIndex = adb_netConfigType_get_networkIndex(net_type, env);
    snprintf(netparams.privateMac, 24, "%s", adb_netConfigType_get_privateMacAddress(net_type, env));
    snprintf(netparams.privateIp, 24, "%s", adb_netConfigType_get_privateIp(net_type, env));
    snprintf(netparams.publicIp, 24, "%s", adb_netConfigType_get_publicIp(net_type, env));
    axis2_char_t * userData = adb_ncRunInstanceType_get_userData(input, env);
    axis2_char_t * launchIndex = adb_ncRunInstanceType_get_launchIndex(input, env);
    axis2_char_t * platform = adb_ncRunInstanceType_get_platform(input, env);

    int expiryTime=0;
    axutil_date_time_t *dt = adb_ncRunInstanceType_get_expiryTime(input, env);
    expiryTime = datetime_to_unix(dt, env);

    int groupNamesSize = adb_ncRunInstanceType_sizeof_groupNames(input, env);
    char ** groupNames = calloc (groupNamesSize, sizeof(char *));
    if (groupNames==NULL) {
        logprintfl (EUCAERROR, "ERROR: out of memory in ncRunInstancesMarshall()\n");
        adb_ncRunInstanceResponseType_set_return(output, env, AXIS2_FALSE);

    } else {
        int i;
        for (i=0; i<groupNamesSize; i++) {
            groupNames[i] = adb_ncRunInstanceType_get_groupNames_at(input, env, i);
        }
    
        { // do it
	  //            ncMetadata meta = { correlationId, userId };
	    ncMetadata meta;
	    EUCA_MESSAGE_UNMARSHAL(ncRunInstanceType, input, (&meta));
            ncInstance * outInst;
            
            int error = doRunInstance (&meta, uuid, instanceId, reservationId, &params, 
                                       imageId, imageURL, 
                                       kernelId, kernelURL, 
                                       ramdiskId, ramdiskURL, 
                                       ownerId, accountId,
                                       keyName, 
                                       &netparams, 
                                       userData, launchIndex, platform, expiryTime, groupNames, groupNamesSize,
                                       &outInst);
            
            if (error) {
                logprintfl (EUCAERROR, "ERROR: doRunInstance() failed error=%d\n", error);
                adb_ncRunInstanceResponseType_set_return(output, env, AXIS2_FALSE);
                
            } else {
                ///// set standard fields in output
                adb_ncRunInstanceResponseType_set_return(output, env, AXIS2_TRUE);
                adb_ncRunInstanceResponseType_set_correlationId(output, env, meta.correlationId);
                adb_ncRunInstanceResponseType_set_userId(output, env, meta.userId);
                
                ///// set operation-specific fields in output            
                adb_instanceType_t * instance = adb_instanceType_create(env);
                copy_instance_to_adb (instance, env, outInst); // copy all values outInst->instance
                
                // TODO: should we free_instance(&outInst) here or not? currently you don't have to
                adb_ncRunInstanceResponseType_set_instance(output, env, instance);
            }
            
            if (groupNamesSize)
                free (groupNames);
        }
    }
    
    // set response to output
    adb_ncRunInstanceResponse_set_ncRunInstanceResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "RunInstance", "end");
    return response;
}

adb_ncDescribeInstancesResponse_t* ncDescribeInstancesMarshal (adb_ncDescribeInstances_t* ncDescribeInstances, const axutil_env_t *env)
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncDescribeInstancesType_t * input          = adb_ncDescribeInstances_get_ncDescribeInstances(ncDescribeInstances, env);
    adb_ncDescribeInstancesResponse_t * response   = adb_ncDescribeInstancesResponse_create(env);
    adb_ncDescribeInstancesResponseType_t * output = adb_ncDescribeInstancesResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncDescribeInstancesType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncDescribeInstancesType_get_userId(input, env);

    // get operation-specific fields from input
    int instIdsLen = adb_ncDescribeInstancesType_sizeof_instanceIds(input, env);
    char ** instIds = malloc(sizeof(char *) * instIdsLen);
    if (instIds==NULL) {
        logprintfl (EUCAERROR, "ERROR: out of memory in ncDescribeInstancesMarshal()\n");
        adb_ncDescribeInstancesResponseType_set_return(output, env, AXIS2_FALSE);

    } else {
        int i;
        for (i=0; i<instIdsLen; i++) {
            instIds[i] = adb_ncDescribeInstancesType_get_instanceIds_at(input, env, i);
        }

	//        eventlog("NC", userId, correlationId, "DescribeInstances", "begin");
        { // do it
	  //            ncMetadata meta = { correlationId, userId };
	    ncMetadata meta;
	    EUCA_MESSAGE_UNMARSHAL(ncDescribeInstancesType, input, (&meta));
            ncInstance **outInsts;
            int outInstsLen;

            int error = doDescribeInstances (&meta, instIds, instIdsLen, &outInsts, &outInstsLen);
                                             
            if (error) {
                logprintfl (EUCAERROR, "ERROR: doDescribeInstances() failed error=%d\n", error);
                adb_ncDescribeInstancesResponseType_set_return(output, env, AXIS2_FALSE);
                
            } else {
                // set standard fields in output
                adb_ncDescribeInstancesResponseType_set_return(output, env, AXIS2_TRUE);
                adb_ncDescribeInstancesResponseType_set_correlationId(output, env, meta.correlationId);
                adb_ncDescribeInstancesResponseType_set_userId(output, env, meta.userId);

                // set operation-specific fields in output
                for (i=0; i<outInstsLen; i++) {
                    adb_instanceType_t * instance = adb_instanceType_create(env);
                    copy_instance_to_adb (instance, env, outInsts[i]); // copy all values outInst->instance
                    if (outInsts[i])
		      free(outInsts[i]);

                    /* TODO: should we free_instance(&outInst) here or not? currently you only have to free outInsts[] */
                    adb_ncDescribeInstancesResponseType_add_instances(output, env, instance);
                }
            
                if (outInstsLen)
                    free ( outInsts );
            }
        }
	//        eventlog("NC", userId, correlationId, "DescribeInstances", "end");
    }
    
    // set response to output
    adb_ncDescribeInstancesResponse_set_ncDescribeInstancesResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    return response;
}

adb_ncRebootInstanceResponse_t* ncRebootInstanceMarshal (adb_ncRebootInstance_t* ncRebootInstance,  const axutil_env_t *env) 
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncRebootInstanceType_t * input          = adb_ncRebootInstance_get_ncRebootInstance(ncRebootInstance, env);
    adb_ncRebootInstanceResponse_t * response   = adb_ncRebootInstanceResponse_create(env);
    adb_ncRebootInstanceResponseType_t * output = adb_ncRebootInstanceResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncRebootInstanceType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncRebootInstanceType_get_userId(input, env);

    // get operation-specific fields from input
    axis2_char_t * instanceId = adb_ncRebootInstanceType_get_instanceId(input, env);

    //    eventlog("NC", userId, correlationId, "RebootInstance", "begin");
    { // do it
      //        ncMetadata meta = { correlationId, userId };
        ncMetadata meta;
	EUCA_MESSAGE_UNMARSHAL(ncRebootInstanceType, input, (&meta));

        int error = doRebootInstance (&meta, instanceId);
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doRebootInstance() failed error=%d\n", error);
            adb_ncRebootInstanceResponseType_set_return(output, env, AXIS2_FALSE);

        } else {
            // set standard fields in output
            adb_ncRebootInstanceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncRebootInstanceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncRebootInstanceResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncRebootInstanceResponseType_set_status(output, env, 0);
        }
    }
    // set response to output
    adb_ncRebootInstanceResponse_set_ncRebootInstanceResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "RebootInstance", "end");
    return response;
}

adb_ncGetConsoleOutputResponse_t* ncGetConsoleOutputMarshal (adb_ncGetConsoleOutput_t* ncGetConsoleOutput, const axutil_env_t *env) 
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncGetConsoleOutputType_t * input          = adb_ncGetConsoleOutput_get_ncGetConsoleOutput(ncGetConsoleOutput, env);
    adb_ncGetConsoleOutputResponse_t * response   = adb_ncGetConsoleOutputResponse_create(env);
    adb_ncGetConsoleOutputResponseType_t * output = adb_ncGetConsoleOutputResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncGetConsoleOutputType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncGetConsoleOutputType_get_userId(input, env);

    // get operation-specific fields from input
    axis2_char_t * instanceId = adb_ncGetConsoleOutputType_get_instanceId(input, env);

    //    eventlog("NC", userId, correlationId, "GetConsoleOutput", "begin");
    { // do it
      //        ncMetadata meta = { correlationId, userId };
        ncMetadata meta;
	EUCA_MESSAGE_UNMARSHAL(ncGetConsoleOutputType, input, (&meta));
        char * consoleOutput=NULL;

        int error = doGetConsoleOutput (&meta, instanceId, &consoleOutput);
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doGetConsoleOutput() failed error=%d\n", error);
            adb_ncGetConsoleOutputResponseType_set_return(output, env, AXIS2_FALSE);

        } else {
            // set standard fields in output
            adb_ncGetConsoleOutputResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncGetConsoleOutputResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncGetConsoleOutputResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncGetConsoleOutputResponseType_set_consoleOutput(output, env, consoleOutput);
        }
	if (consoleOutput) free(consoleOutput);
    }
    // set response to output
    adb_ncGetConsoleOutputResponse_set_ncGetConsoleOutputResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "GetConsoleOutput", "end");
    return response;
}

adb_ncTerminateInstanceResponse_t* ncTerminateInstanceMarshal (adb_ncTerminateInstance_t* ncTerminateInstance, const axutil_env_t *env)
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncTerminateInstanceType_t * input          = adb_ncTerminateInstance_get_ncTerminateInstance(ncTerminateInstance, env);
    adb_ncTerminateInstanceResponse_t * response   = adb_ncTerminateInstanceResponse_create(env);
    adb_ncTerminateInstanceResponseType_t * output = adb_ncTerminateInstanceResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncTerminateInstanceType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncTerminateInstanceType_get_userId(input, env);

    // get operation-specific fields from input
    axis2_char_t * instanceId = adb_ncTerminateInstanceType_get_instanceId(input, env);
    axis2_bool_t forceBool = adb_ncTerminateInstanceType_get_force(input, env);
    int force=0;
    if (forceBool == AXIS2_TRUE) {
      force = 1;
    } else {
      force = 0;
    }

    //    eventlog("NC", userId, correlationId, "TerminateInstance", "begin");
    { // do it
      //        ncMetadata meta = { correlationId, userId };
        ncMetadata meta;
	EUCA_MESSAGE_UNMARSHAL(ncTerminateInstanceType, input, (&meta));
        int shutdownState, previousState;

        int error = doTerminateInstance (&meta, instanceId, force, &shutdownState, &previousState);
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doTerminateInstance() failed error=%d\n", error);
            adb_ncTerminateInstanceResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncTerminateInstanceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncTerminateInstanceResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncTerminateInstanceResponseType_set_instanceId(output, env, instanceId);

        } else {
            // set standard fields in output
            adb_ncTerminateInstanceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncTerminateInstanceResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncTerminateInstanceResponseType_set_userId(output, env, meta.userId);

            // set operation-specific fields in output
            adb_ncTerminateInstanceResponseType_set_instanceId(output, env, instanceId);
            // TODO: change the WSDL to use the name/code pair
            char s[128];
            snprintf (s, 128, "%d", shutdownState);
            adb_ncTerminateInstanceResponseType_set_shutdownState(output, env, s);
            snprintf (s, 128, "%d", previousState);
            adb_ncTerminateInstanceResponseType_set_previousState(output, env, s);

        }
    }
    // set response to output
    adb_ncTerminateInstanceResponse_set_ncTerminateInstanceResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "TerminateInstance", "end");
    return response;
}

adb_ncAttachVolumeResponse_t* ncAttachVolumeMarshal (adb_ncAttachVolume_t* ncAttachVolume, const axutil_env_t *env)
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncAttachVolumeType_t * input          = adb_ncAttachVolume_get_ncAttachVolume(ncAttachVolume, env);
    adb_ncAttachVolumeResponse_t * response   = adb_ncAttachVolumeResponse_create(env);
    adb_ncAttachVolumeResponseType_t * output = adb_ncAttachVolumeResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncAttachVolumeType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncAttachVolumeType_get_userId(input, env);

    // get operation-specific fields from input
    axis2_char_t * instanceId = adb_ncAttachVolumeType_get_instanceId(input, env);
    axis2_char_t * volumeId = adb_ncAttachVolumeType_get_volumeId(input, env);
    axis2_char_t * remoteDev = adb_ncAttachVolumeType_get_remoteDev(input, env);
    axis2_char_t * localDev = adb_ncAttachVolumeType_get_localDev(input, env);

    //    eventlog("NC", userId, correlationId, "AttachVolume", "begin");
    { // do it
      //        ncMetadata meta = { correlationId, userId };
	ncMetadata meta;
	EUCA_MESSAGE_UNMARSHAL(ncAttachVolumeType, input, (&meta));

        int error = doAttachVolume (&meta, instanceId, volumeId, remoteDev, localDev);
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doAttachVolume() failed error=%d\n", error);
            adb_ncAttachVolumeResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncAttachVolumeResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncAttachVolumeResponseType_set_userId(output, env, meta.userId);
        } else {
            // set standard fields in output
            adb_ncAttachVolumeResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncAttachVolumeResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncAttachVolumeResponseType_set_userId(output, env, meta.userId);
            // no operation-specific fields in output
        }
    }
    // set response to output
    adb_ncAttachVolumeResponse_set_ncAttachVolumeResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "AttachVolume", "end");
    return response;
}

adb_ncDetachVolumeResponse_t* ncDetachVolumeMarshal (adb_ncDetachVolume_t* ncDetachVolume, const axutil_env_t *env)
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncDetachVolumeType_t * input          = adb_ncDetachVolume_get_ncDetachVolume(ncDetachVolume, env);
    adb_ncDetachVolumeResponse_t * response   = adb_ncDetachVolumeResponse_create(env);
    adb_ncDetachVolumeResponseType_t * output = adb_ncDetachVolumeResponseType_create(env);

    // get standard fields from input
    //    axis2_char_t * correlationId = adb_ncDetachVolumeType_get_correlationId(input, env);
    //    axis2_char_t * userId = adb_ncDetachVolumeType_get_userId(input, env);

    // get operation-specific fields from input
    axis2_char_t * instanceId = adb_ncDetachVolumeType_get_instanceId(input, env);
    axis2_char_t * volumeId = adb_ncDetachVolumeType_get_volumeId(input, env);
    axis2_char_t * remoteDev = adb_ncDetachVolumeType_get_remoteDev(input, env);
    axis2_char_t * localDev = adb_ncDetachVolumeType_get_localDev(input, env);
    int force=0;
    axis2_bool_t forceBool = adb_ncDetachVolumeType_get_force(input, env);
    if (forceBool == AXIS2_TRUE) {
      force = 1;
    } else {
      force = 0;
    }

    //    eventlog("NC", userId, correlationId, "DetachVolume", "begin");
    { // do it
      //        ncMetadata meta = { correlationId, userId };
        ncMetadata meta;
	EUCA_MESSAGE_UNMARSHAL(ncDetachVolumeType, input, (&meta));
        int error = doDetachVolume (&meta, instanceId, volumeId, remoteDev, localDev, force, 1);
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doDetachVolume() failed error=%d\n", error);
            adb_ncDetachVolumeResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncDetachVolumeResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncDetachVolumeResponseType_set_userId(output, env, meta.userId);
        } else {
            // set standard fields in output
            adb_ncDetachVolumeResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncDetachVolumeResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncDetachVolumeResponseType_set_userId(output, env, meta.userId);
            // no operation-specific fields in output
        }
    }
    // set response to output
    adb_ncDetachVolumeResponse_set_ncDetachVolumeResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "DetachVolume", "end");
    return response;
}

adb_ncCreateImageResponse_t* ncCreateImageMarshal (adb_ncCreateImage_t* ncCreateImage, const axutil_env_t *env) {
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncCreateImageType_t * input          = adb_ncCreateImage_get_ncCreateImage(ncCreateImage, env);
    adb_ncCreateImageResponse_t * response   = adb_ncCreateImageResponse_create(env);
    adb_ncCreateImageResponseType_t * output = adb_ncCreateImageResponseType_create(env);

    // get operation-specific fields from input
    axis2_char_t * instanceId = adb_ncCreateImageType_get_instanceId(input, env);
    axis2_char_t * volumeId = adb_ncCreateImageType_get_volumeId(input, env);
    axis2_char_t * remoteDev = adb_ncCreateImageType_get_remoteDev(input, env);

    //    eventlog("NC", userId, correlationId, "CreateImage", "begin");
    { // do it
      //        ncMetadata meta = { correlationId, userId };
        ncMetadata meta;
	EUCA_MESSAGE_UNMARSHAL(ncCreateImageType, input, (&meta));
        int error = doCreateImage (&meta, instanceId, volumeId, remoteDev);
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doCreateImage() failed error=%d\n", error);
            adb_ncCreateImageResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncCreateImageResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncCreateImageResponseType_set_userId(output, env, meta.userId);
        } else {
            // set standard fields in output
            adb_ncCreateImageResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncCreateImageResponseType_set_correlationId(output, env, meta.correlationId);
            adb_ncCreateImageResponseType_set_userId(output, env, meta.userId);
            // no operation-specific fields in output
        }
    }
    // set response to output
    adb_ncCreateImageResponse_set_ncCreateImageResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    //    eventlog("NC", userId, correlationId, "CreateImage", "end");
    return response;
}

adb_ncBundleInstanceResponse_t* ncBundleInstanceMarshal (adb_ncBundleInstance_t* ncBundleInstance, const axutil_env_t *env)
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncBundleInstanceType_t * input          = adb_ncBundleInstance_get_ncBundleInstance(ncBundleInstance, env);
    adb_ncBundleInstanceResponse_t * response   = adb_ncBundleInstanceResponse_create(env);
    adb_ncBundleInstanceResponseType_t * output = adb_ncBundleInstanceResponseType_create(env);

    // get standard fields from input
    axis2_char_t * correlationId = adb_ncBundleInstanceType_get_correlationId(input, env);
    axis2_char_t * userId = adb_ncBundleInstanceType_get_userId(input, env);

    // get operation-specific fields from input
    axis2_char_t * instanceId = adb_ncBundleInstanceType_get_instanceId(input, env);
    axis2_char_t * bucketName = adb_ncBundleInstanceType_get_bucketName(input, env);
    axis2_char_t * filePrefix = adb_ncBundleInstanceType_get_filePrefix(input, env);
    axis2_char_t * walrusURL = adb_ncBundleInstanceType_get_walrusURL(input, env);
    axis2_char_t * userPublicKey = adb_ncBundleInstanceType_get_userPublicKey(input, env);
    axis2_char_t * S3Policy = adb_ncBundleInstanceType_get_S3Policy(input, env);
    axis2_char_t * S3PolicySig = adb_ncBundleInstanceType_get_S3PolicySig(input, env);

    eventlog("NC", userId, correlationId, "BundleInstance", "begin");
    { // do it
        ncMetadata meta = { correlationId, userId };

        int error = doBundleInstance (&meta, instanceId, bucketName, filePrefix, walrusURL, userPublicKey, S3Policy, S3PolicySig);
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doBundleInstance() failed error=%d\n", error);
            adb_ncBundleInstanceResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncBundleInstanceResponseType_set_correlationId(output, env, correlationId);
            adb_ncBundleInstanceResponseType_set_userId(output, env, userId);
        } else {
            // set standard fields in output
            adb_ncBundleInstanceResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncBundleInstanceResponseType_set_correlationId(output, env, correlationId);
            adb_ncBundleInstanceResponseType_set_userId(output, env, userId);
            // no operation-specific fields in output
        }
    }
    // set response to output
    adb_ncBundleInstanceResponse_set_ncBundleInstanceResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    eventlog("NC", userId, correlationId, "BundleInstance", "end");
    return response;
}

adb_ncCancelBundleTaskResponse_t* ncCancelBundleTaskMarshal (adb_ncCancelBundleTask_t* ncCancelBundleTask, const axutil_env_t *env) {
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncCancelBundleTaskType_t * input          = adb_ncCancelBundleTask_get_ncCancelBundleTask(ncCancelBundleTask, env);
    adb_ncCancelBundleTaskResponse_t * response   = adb_ncCancelBundleTaskResponse_create(env);
    adb_ncCancelBundleTaskResponseType_t * output = adb_ncCancelBundleTaskResponseType_create(env);

    // get standard fields from input
    axis2_char_t * correlationId = adb_ncCancelBundleTaskType_get_correlationId(input, env);
    axis2_char_t * userId = adb_ncCancelBundleTaskType_get_userId(input, env);

    // get operation-specific fields from input
    axis2_char_t * instanceId = adb_ncCancelBundleTaskType_get_instanceId(input, env);

    eventlog("NC", userId, correlationId, "CancelBundleTask", "begin");
    { // do it
        ncMetadata meta = { correlationId, userId };

        int error = doCancelBundleTask (&meta, instanceId);
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doCancelBundleTask() failed error=%d\n", error);
            adb_ncCancelBundleTaskResponseType_set_return(output, env, AXIS2_FALSE);
            adb_ncCancelBundleTaskResponseType_set_correlationId(output, env, correlationId);
            adb_ncCancelBundleTaskResponseType_set_userId(output, env, userId);
        } else {
            // set standard fields in output
            adb_ncCancelBundleTaskResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncCancelBundleTaskResponseType_set_correlationId(output, env, correlationId);
            adb_ncCancelBundleTaskResponseType_set_userId(output, env, userId);
            // no operation-specific fields in output
        }
    }
    // set response to output
    adb_ncCancelBundleTaskResponse_set_ncCancelBundleTaskResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    eventlog("NC", userId, correlationId, "CancelBundleTask", "end");
    return response;
}

adb_ncDescribeBundleTasksResponse_t* ncDescribeBundleTasksMarshal (adb_ncDescribeBundleTasks_t* ncDescribeBundleTasks, const axutil_env_t *env)
{
    pthread_mutex_lock(&ncHandlerLock);
    adb_ncDescribeBundleTasksType_t * input          = adb_ncDescribeBundleTasks_get_ncDescribeBundleTasks(ncDescribeBundleTasks, env);
    adb_ncDescribeBundleTasksResponse_t * response   = adb_ncDescribeBundleTasksResponse_create(env);
    adb_ncDescribeBundleTasksResponseType_t * output = adb_ncDescribeBundleTasksResponseType_create(env);

    // get standard fields from input
    axis2_char_t * correlationId = adb_ncDescribeBundleTasksType_get_correlationId(input, env);
    axis2_char_t * userId = adb_ncDescribeBundleTasksType_get_userId(input, env);

    // get operation-specific fields from input
    int instIdsLen = adb_ncDescribeBundleTasksType_sizeof_instanceIds(input, env);
    char ** instIds = malloc(sizeof(char *) * instIdsLen);
    
    bundleTask **outBundleTasks=NULL;
    int outBundleTasksLen=0;

    if (instIds==NULL) {
        logprintfl (EUCAERROR, "ERROR: out of memory in ncDescribeBundleTasksMarshal()\n");
        adb_ncDescribeBundleTasksResponseType_set_return(output, env, AXIS2_FALSE);

    } else {
        int i;
        for (i=0; i<instIdsLen; i++) {
            instIds[i] = adb_ncDescribeBundleTasksType_get_instanceIds_at(input, env, i);
        }

        eventlog("NC", userId, correlationId, "DescribeBundleTasks", "begin");
        { // do it
            ncMetadata meta = { correlationId, userId };

            int error = doDescribeBundleTasks (&meta, instIds, instIdsLen, &outBundleTasks, &outBundleTasksLen);                                             
            if (error) {
                logprintfl (EUCAERROR, "ERROR: doDescribeBundleTasks() failed error=%d\n", error);
                adb_ncDescribeBundleTasksResponseType_set_return(output, env, AXIS2_FALSE);
                
            } else {
                // set standard fields in output
                adb_ncDescribeBundleTasksResponseType_set_return(output, env, AXIS2_TRUE);
                adb_ncDescribeBundleTasksResponseType_set_correlationId(output, env, correlationId);
                adb_ncDescribeBundleTasksResponseType_set_userId(output, env, userId);
				// set operation specific values
				for (i=0; i<outBundleTasksLen; i++) {
					adb_bundleTaskType_t *btt;
					btt = adb_bundleTaskType_create(env);
					adb_bundleTaskType_set_instanceId(btt, env, outBundleTasks[i]->instanceId);
					adb_bundleTaskType_set_state(btt, env, outBundleTasks[i]->state);
					adb_ncDescribeBundleTasksResponseType_add_bundleTasks(output, env, btt);
					free(outBundleTasks[i]);
				}
				free(outBundleTasks);
            }
        }
    }
	
    // set response to output
    adb_ncDescribeBundleTasksResponse_set_ncDescribeBundleTasksResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    eventlog("NC", userId, correlationId, "DescribeBundleTasks", "end");
    return response;
}

/***********************
 template for future ops
 ***********************

    pthread_mutex_lock(&ncHandlerLock);
    adb_ncOPERATIONType_t * input          = adb_ncOPERATION_get_ncOPERATION(ncOPERATION, env);
    adb_ncOPERATIONResponse_t * response   = adb_ncOPERATIONResponse_create(env);
    adb_ncOPERATIONResponseType_t * output = adb_ncOPERATIONResponseType_create(env);

    // get standard fields from input
    axis2_char_t * correlationId = adb_ncOPERATIONType_get_correlationId(input, env);
    axis2_char_t * userId = adb_ncOPERATIONType_get_userId(input, env);

    // get operation-specific fields from input
    // e.g.: axis2_char_t * instanceId = adb_ncOPERATIONType_get_instanceId(input, env);

    eventlog("NC", userId, correlationId, "OPERATION", "begin");
    { // do it
        ncMetadata meta = { correlationId, userId };

        int error = doOPERATION (&meta, instanceId, ...
    
        if (error) {
            logprintfl (EUCAERROR, "ERROR: doOPERATION() failed error=%d\n", error);
            adb_ncOPERATIONResponseType_set_return(output, env, AXIS2_FALSE);

        } else {
            // set standard fields in output
            adb_ncOPERATIONResponseType_set_return(output, env, AXIS2_TRUE);
            adb_ncOPERATIONResponseType_set_correlationId(output, env, correlationId);
            adb_ncOPERATIONResponseType_set_userId(output, env, userId);

            // set operation-specific fields in output
        }
    }
    // set response to output
    adb_ncOPERATIONResponse_set_ncOPERATIONResponse(response, env, output);
    pthread_mutex_unlock(&ncHandlerLock);
    
    eventlog("NC", userId, correlationId, "OPERATION", "end");
    return response;
*/

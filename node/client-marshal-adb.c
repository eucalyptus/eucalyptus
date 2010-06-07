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
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <neethi_policy.h>
#include <neethi_util.h>

#include "axis2_stub_EucalyptusNC.h"
#include "client-marshal.h"
#include "misc.h"
#include "adb-helpers.h"

#define NULL_ERROR_MSG "() could not be invoked (check NC host, port, and credentials)\n"

//#define CORRELATION_ID meta->correlationId
#define CORRELATION_ID NULL

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

    if (client_home == NULL) {
        logprintfl (EUCAERROR, "ERROR: cannot get AXIS2C_HOME");
	return NULL;
    }
    if (endpoint_uri == NULL) {
        logprintfl (EUCAERROR, "ERROR: empty endpoint_url");
	return NULL;
    }

    char * uri = endpoint_uri;

    // extract node name from the endpoint
    char * p = strstr (uri, "://"); // find "http[s]://..."
    if (p==NULL) {
      logprintfl (EUCAERROR, "ncStubCreate received invalid URI %s\n", uri);
      return NULL;
    }
    char * node_name = strdup (p+3); // copy without the protocol prefix
    if (node_name==NULL) {
      logprintfl (EUCAERROR, "ncStubCreate is out of memory\n");
      return NULL;
    }
    if ((p = strchr (node_name, ':')) != NULL) *p = '\0'; // cut off the port
    if ((p = strchr (node_name, '/')) != NULL) *p = '\0'; // if there is no port

    logprintfl (EUCADEBUG, "DEBUG: requested URI %s\n", uri);

    // see if we should redirect to the VMware broker
    if (strstr (uri, "VMwareBroker")) {
      uri = "http://localhost:8773/services/VMwareBroker";
      logprintfl (EUCADEBUG, "DEBUG: redirecting request to %s\n", uri);
    }

    // TODO: what if endpoint_uri, home, or env are NULL?
    stub = axis2_stub_create_EucalyptusNC(env, client_home, (axis2_char_t *)uri);

    if (stub && (st = malloc (sizeof(ncStub)))) {
        st->env=env;
        st->client_home=strdup((char *)client_home);
        st->endpoint_uri=(axis2_char_t *)strdup(endpoint_uri);
	st->node_name=(axis2_char_t *)strdup(node_name);
        st->stub=stub;
	if (st->client_home == NULL || st->endpoint_uri == NULL) {
            logprintfl (EUCAWARN, "WARNING: out of memory");
	}
    } else {
        logprintfl (EUCAWARN, "WARNING: out of memory");
    } 
    
    free (node_name);
    return st;
}

int ncStubDestroy (ncStub * st)
{
    if (st->client_home) free(st->client_home);
    if (st->endpoint_uri) free(st->endpoint_uri);
    if (st->node_name) free(st->node_name);
    free (st);
    return 0;
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
    
    return (int) mktime(&t);
}

static ncInstance * copy_instance_from_adb (adb_instanceType_t * instance, axutil_env_t * env)
{
    int i;
    adb_virtualMachineType_t * vm_type = adb_instanceType_get_instanceType(instance, env);
    virtualMachine params;
    copy_vm_type_from_adb (&params, vm_type, env);
    netConfig ncnet;
    bzero(&ncnet, sizeof(netConfig));
    adb_netConfigType_t * netconf = adb_instanceType_get_netParams(instance, env);
    if (netconf != NULL) {
        ncnet.vlan = adb_netConfigType_get_vlan(netconf, env);
		ncnet.networkIndex = adb_netConfigType_get_networkIndex(netconf, env);
        strncpy(ncnet.privateMac, adb_netConfigType_get_privateMacAddress(netconf, env), 24);
        strncpy(ncnet.privateIp, adb_netConfigType_get_privateIp(netconf, env), 24);
        strncpy(ncnet.publicIp, adb_netConfigType_get_publicIp(netconf, env), 24);
    }

    int groupNamesSize = adb_instanceType_sizeof_groupNames (instance, env);
    char * groupNames [EUCA_MAX_GROUPS];
    for (i = 0; i<EUCA_MAX_GROUPS && i<groupNamesSize; i++) {
        groupNames[i] = adb_instanceType_get_groupNames_at (instance, env, i);
    }

    ncInstance * outInst = allocate_instance(
        (char *)adb_instanceType_get_instanceId(instance, env),
        (char *)adb_instanceType_get_reservationId(instance, env),
        &params,
        (char *)adb_instanceType_get_imageId(instance, env),
        NULL, // URL is NULL
        (char *)adb_instanceType_get_kernelId(instance, env),
        NULL, // URL is NULL
        (char *)adb_instanceType_get_ramdiskId(instance, env),
        NULL, // URL is NULL
        (char *)adb_instanceType_get_stateName(instance, env),
        0,
        (char *)adb_instanceType_get_userId(instance, env), 
        &ncnet, 
        (char *)adb_instanceType_get_keyName(instance, env),
        (char *)adb_instanceType_get_userData(instance, env),
        (char *)adb_instanceType_get_launchIndex(instance, env),
        groupNames, groupNamesSize
        );

    axutil_date_time_t * dt = adb_instanceType_get_launchTime(instance, env);
    if (dt!=NULL) {
        outInst->launchTime = datetime_to_unix (dt, env);
        axutil_date_time_free(dt, env);
    }

    outInst->volumesSize = adb_instanceType_sizeof_volumes (instance, env);
    if (outInst->volumesSize > 0) {
        for (i=0; i<EUCA_MAX_VOLUMES && i<outInst->volumesSize; i++) {
            adb_volumeType_t * volume = adb_instanceType_get_volumes_at (instance, env, i);
            strncpy (outInst->volumes[i].volumeId, adb_volumeType_get_volumeId (volume, env), CHAR_BUFFER_SIZE);
            strncpy (outInst->volumes[i].remoteDev, adb_volumeType_get_remoteDev (volume, env), CHAR_BUFFER_SIZE);
            strncpy (outInst->volumes[i].localDev, adb_volumeType_get_localDev (volume, env), CHAR_BUFFER_SIZE);
			strncpy (outInst->volumes[i].stateName, adb_volumeType_get_state (volume, env), CHAR_BUFFER_SIZE);
        }
    }

    return outInst;
}

int ncRunInstanceStub (ncStub *st, ncMetadata *meta, char *instanceId, char *reservationId, virtualMachine *params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, char *keyName, netConfig *netparams, char *userData, char *launchIndex, char **groupNames, int groupNamesSize, ncInstance **outInstPtr)
{
    int i;
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;
    adb_ncRunInstance_t     * input   = adb_ncRunInstance_create(env); 
    adb_ncRunInstanceType_t * request = adb_ncRunInstanceType_create(env);
    
    // set standard input fields
    adb_ncRunInstanceType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncRunInstanceType_set_correlationId(request, env, CORRELATION_ID);
        adb_ncRunInstanceType_set_userId(request, env, meta->userId);
    }

    // set op-specific input fields
    adb_ncRunInstanceType_set_instanceId(request, env, instanceId);
    adb_ncRunInstanceType_set_reservationId(request, env, reservationId);
    adb_ncRunInstanceType_set_instanceType(request, env, copy_vm_type_to_adb(env, params));

    adb_ncRunInstanceType_set_imageId(request, env, imageId);
    adb_ncRunInstanceType_set_imageURL(request, env, imageURL);
    adb_ncRunInstanceType_set_kernelId(request, env, kernelId);
    adb_ncRunInstanceType_set_kernelURL(request, env, kernelURL);
    adb_ncRunInstanceType_set_ramdiskId(request, env, ramdiskId);
    adb_ncRunInstanceType_set_ramdiskURL(request, env, ramdiskURL);
    adb_ncRunInstanceType_set_keyName(request, env, keyName);
    adb_netConfigType_t *netConfig = adb_netConfigType_create(env);
    adb_netConfigType_set_privateMacAddress(netConfig, env, netparams->privateMac);
    adb_netConfigType_set_privateIp(netConfig, env, netparams->privateIp);
    adb_netConfigType_set_publicIp(netConfig, env, netparams->publicIp);
    adb_netConfigType_set_vlan(netConfig, env, netparams->vlan);
    adb_netConfigType_set_networkIndex(netConfig, env, netparams->networkIndex);
    adb_ncRunInstanceType_set_netParams(request, env, netConfig);
    //    adb_ncRunInstanceType_set_privateMacAddress(request, env, privMac);
    //    adb_ncRunInstanceType_set_privateIp(request, env, privIp);
    //    adb_ncRunInstanceType_set_vlan(request, env, vlan);
    adb_ncRunInstanceType_set_userData(request, env, userData);
    adb_ncRunInstanceType_set_launchIndex(request, env, launchIndex);
    for (i=0; i<groupNamesSize; i++) {
        adb_ncRunInstanceType_add_groupNames(request, env, groupNames[i]);
    }
    
    adb_ncRunInstance_set_ncRunInstance(input, env, request);

    int status = 0;
    { // do it
        adb_ncRunInstanceResponse_t * output = axis2_stub_op_EucalyptusNC_ncRunInstance(stub, env, input);
        
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: RunInstance" NULL_ERROR_MSG);
            status = -1;
            
        } else {
            adb_ncRunInstanceResponseType_t * response = adb_ncRunInstanceResponse_get_ncRunInstanceResponse(output, env);
            if ( adb_ncRunInstanceResponseType_get_return(response, env) == AXIS2_FALSE ) {
                logprintfl (EUCAERROR, "ERROR: RunInstance returned an error\n");
                status = 1;
            }

            adb_instanceType_t * instance = adb_ncRunInstanceResponseType_get_instance(response, env);
            * outInstPtr = copy_instance_from_adb (instance, env);
        }
    }
    
    return status;
}

int ncGetConsoleOutputStub (ncStub *st, ncMetadata *meta, char *instanceId, char **consoleOutput) 
{
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;
    
    adb_ncGetConsoleOutput_t     * input   = adb_ncGetConsoleOutput_create(env); 
    adb_ncGetConsoleOutputType_t * request = adb_ncGetConsoleOutputType_create(env);
    
    /* set input fields */
    adb_ncGetConsoleOutputType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncGetConsoleOutputType_set_correlationId(request, env, CORRELATION_ID);
        adb_ncGetConsoleOutputType_set_userId(request, env, meta->userId);
    }
    
    adb_ncGetConsoleOutputType_set_instanceId(request, env, instanceId);
    adb_ncGetConsoleOutput_set_ncGetConsoleOutput(input, env, request);
    
    /* do it */
    int status = 0;
    {
        adb_ncGetConsoleOutputResponse_t * output = axis2_stub_op_EucalyptusNC_ncGetConsoleOutput(stub, env, input);
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: GetConsoleOutputInstance" NULL_ERROR_MSG);
            * consoleOutput = NULL;
            status = -1;
            
        } else {
            adb_ncGetConsoleOutputResponseType_t * response = adb_ncGetConsoleOutputResponse_get_ncGetConsoleOutputResponse(output, env);
            if ( adb_ncGetConsoleOutputResponseType_get_return(response, env) == AXIS2_FALSE ) {
                logprintfl (EUCAERROR, "ERROR: GetConsoleOutput returned an error\n");
                status = 1;
            }

            * consoleOutput = adb_ncGetConsoleOutputResponseType_get_consoleOutput(response, env);
        }
    }
    
    return status;
}

int ncRebootInstanceStub (ncStub *st, ncMetadata *meta, char *instanceId) 
{
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;
    
    adb_ncRebootInstance_t     * input   = adb_ncRebootInstance_create(env); 
    adb_ncRebootInstanceType_t * request = adb_ncRebootInstanceType_create(env);
    
    /* set input fields */
    adb_ncRebootInstanceType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncRebootInstanceType_set_correlationId(request, env, CORRELATION_ID);
        adb_ncRebootInstanceType_set_userId(request, env, meta->userId);
    }
    
    adb_ncRebootInstanceType_set_instanceId(request, env, instanceId);
    adb_ncRebootInstance_set_ncRebootInstance(input, env, request);
    
    int status = 0;
    {
        adb_ncRebootInstanceResponse_t * output = axis2_stub_op_EucalyptusNC_ncRebootInstance(stub, env, input);
        
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: RebootInstanceInstance" NULL_ERROR_MSG);
            status = -1;
            
        } else {
            adb_ncRebootInstanceResponseType_t * response = adb_ncRebootInstanceResponse_get_ncRebootInstanceResponse(output, env);
            if ( adb_ncRebootInstanceResponseType_get_return(response, env) == AXIS2_FALSE ) {
                logprintfl (EUCAERROR, "ERROR: RebootInstance returned an error\n");
                status = 1;
            }

            status = adb_ncRebootInstanceResponseType_get_status(response, env);
        }
    }
    return status;
}

int ncTerminateInstanceStub (ncStub *st, ncMetadata *meta, char *instId, int *shutdownState, int *previousState)
{
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;
    
    adb_ncTerminateInstance_t     * input   = adb_ncTerminateInstance_create(env); 
    adb_ncTerminateInstanceType_t * request = adb_ncTerminateInstanceType_create(env);
    
    /* set input fields */
    adb_ncTerminateInstanceType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncTerminateInstanceType_set_correlationId(request, env, CORRELATION_ID);
        adb_ncTerminateInstanceType_set_userId(request, env, meta->userId);
    }
    adb_ncTerminateInstanceType_set_instanceId(request, env, instId);
    adb_ncTerminateInstance_set_ncTerminateInstance(input, env, request);
    
    int status = 0;
    { // do it
        adb_ncTerminateInstanceResponse_t * output = axis2_stub_op_EucalyptusNC_ncTerminateInstance(stub, env, input);
        
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: TerminateInstance" NULL_ERROR_MSG);
            status = -1;
            
        } else {
            adb_ncTerminateInstanceResponseType_t * response;
            
            response = adb_ncTerminateInstanceResponse_get_ncTerminateInstanceResponse(output, env);
            if ( adb_ncTerminateInstanceResponseType_get_return(response, env) == AXIS2_FALSE ) {
                // suppress error message because we conservatively call Terminate on all nodes
                //logprintfl (EUCAERROR, "ERROR: TerminateInsance returned an error\n");
                status = 1;
            }

            /* TODO: fix the state char->int conversion */
            * shutdownState = 0; //strdup(adb_ncTerminateInstanceResponseType_get_shutdownState(response, env));
            * previousState = 0; //strdup(adb_ncTerminateInstanceResponseType_get_previousState(response, env));
        }
    }
    
    return status;
}

int ncDescribeInstancesStub (ncStub *st, ncMetadata *meta, char **instIds, int instIdsLen, ncInstance ***outInsts, int *outInstsLen)
{
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;
    adb_ncDescribeInstances_t     * input   = adb_ncDescribeInstances_create(env); 
    adb_ncDescribeInstancesType_t * request = adb_ncDescribeInstancesType_create(env);
    
    /* set input fields */
    adb_ncDescribeInstancesType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncDescribeInstancesType_set_correlationId(request, env, CORRELATION_ID);
        adb_ncDescribeInstancesType_set_userId(request, env, meta->userId);
    }
    int i;
    for (i=0; i<instIdsLen; i++) {
        adb_ncDescribeInstancesType_add_instanceIds(request, env, instIds[i]);
    }
    adb_ncDescribeInstances_set_ncDescribeInstances(input, env, request);
    
    int status = 0;
    {
        adb_ncDescribeInstancesResponse_t * output = axis2_stub_op_EucalyptusNC_ncDescribeInstances(stub, env, input);
        
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: DescribeInstances" NULL_ERROR_MSG);
            status = -1;

        } else {
            adb_ncDescribeInstancesResponseType_t * response = adb_ncDescribeInstancesResponse_get_ncDescribeInstancesResponse(output, env);
            if ( adb_ncDescribeInstancesResponseType_get_return(response, env) == AXIS2_FALSE ) {
                logprintfl (EUCAERROR, "ERROR: DescribeInstances returned an error\n");
                status = 1;
            }
            
            * outInstsLen = adb_ncDescribeInstancesResponseType_sizeof_instances(response, env);
            if (* outInstsLen) {
                * outInsts = malloc (sizeof(ncInstance *) * *outInstsLen);
                if ( * outInsts == NULL ) { 
                    logprintfl (EUCAERROR, "ERROR: out of memory in ncDescribeInstancesStub()\n");
                    * outInstsLen = 0;
                    status = 2;
                } else {
                    for (i=0; i<*outInstsLen; i++) {
                        adb_instanceType_t * instance = adb_ncDescribeInstancesResponseType_get_instances_at(response, env, i);
                        (* outInsts)[i] = copy_instance_from_adb (instance, env);
                    }
                }
            }
        }
    }
    
    return status;
}

int ncDescribeResourceStub (ncStub *st, ncMetadata *meta, char *resourceType, ncResource **outRes)
{
    axutil_env_t * env = st->env;
    axis2_stub_t * stub = st->stub;
    adb_ncDescribeResource_t     * input   = adb_ncDescribeResource_create(env); 
    adb_ncDescribeResourceType_t * request = adb_ncDescribeResourceType_create(env);
    
    /* set input fields */
    adb_ncDescribeResourceType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncDescribeResourceType_set_correlationId(request, env, CORRELATION_ID);
        adb_ncDescribeResourceType_set_userId(request, env, meta->userId);
    }
    if (resourceType) {
        adb_ncDescribeResourceType_set_resourceType(request, env, resourceType);
    }
    adb_ncDescribeResource_set_ncDescribeResource(input, env, request);
    
    int status = 0;
    {
        adb_ncDescribeResourceResponse_t * output = axis2_stub_op_EucalyptusNC_ncDescribeResource(stub, env, input);
        
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: DescribeResource" NULL_ERROR_MSG);
            status = -1;

        } else {
            adb_ncDescribeResourceResponseType_t * response = adb_ncDescribeResourceResponse_get_ncDescribeResourceResponse(output, env);
            if ( adb_ncDescribeResourceResponseType_get_return(response, env) == AXIS2_FALSE ) {
                logprintfl (EUCAERROR, "ERROR: DescribeResource returned an error\n");
                status = 1;
            }

            ncResource * res = allocate_resource(
                (char *)adb_ncDescribeResourceResponseType_get_nodeStatus(response, env),
                (int)adb_ncDescribeResourceResponseType_get_memorySizeMax(response, env),
                (int)adb_ncDescribeResourceResponseType_get_memorySizeAvailable(response, env),
                (int)adb_ncDescribeResourceResponseType_get_diskSizeMax(response, env),
                (int)adb_ncDescribeResourceResponseType_get_diskSizeAvailable(response, env),
                (int)adb_ncDescribeResourceResponseType_get_numberOfCoresMax(response, env),
                (int)adb_ncDescribeResourceResponseType_get_numberOfCoresAvailable(response, env),
                (char *)adb_ncDescribeResourceResponseType_get_publicSubnets(response, env));

            if (!res) {
                logprintfl (EUCAERROR, "ERROR: out of memory in ncDescribeResourceStub()\n");
                status = 2;
            }
            * outRes = res;
        }
    }
    
    return status;
}

int ncPowerDownStub  (ncStub *st, ncMetadata *meta) {
  axutil_env_t * env  = st->env;
  axis2_stub_t * stub = st->stub;
  adb_ncPowerDown_t     * input   = adb_ncPowerDown_create (env); 
  adb_ncPowerDownType_t * request = adb_ncPowerDownType_create (env);
  
  // set standard input fields
  adb_ncPowerDownType_set_nodeName(request, env, st->node_name);
  if (meta) {
    adb_ncPowerDownType_set_correlationId (request, env, CORRELATION_ID);
    adb_ncPowerDownType_set_userId (request, env, meta->userId);
  }
  
  // set op-specific input fields
  adb_ncPowerDown_set_ncPowerDown(input, env, request);
  
  int status = 0;
  { // do it
    adb_ncPowerDownResponse_t * output = axis2_stub_op_EucalyptusNC_ncPowerDown (stub, env, input);
    
    if (!output) {
      logprintfl (EUCAERROR, "ERROR: PowerDown" NULL_ERROR_MSG);
      status = -1;
    } else {
      adb_ncPowerDownResponseType_t * response = adb_ncPowerDownResponse_get_ncPowerDownResponse (output, env);
      if ( adb_ncPowerDownResponseType_get_return(response, env) == AXIS2_FALSE ) {
	logprintfl (EUCAERROR, "ERROR: PowerDown returned an error\n");
	status = 1;
      }
    }
  }
  
  return status;
}

int ncStartNetworkStub  (ncStub *st, ncMetadata *meta, char **peers, int peersLen, int port, int vlan, char **outStatus) 
{
    axutil_env_t * env  = st->env;
    axis2_stub_t * stub = st->stub;
    adb_ncStartNetwork_t     * input   = adb_ncStartNetwork_create (env); 
    adb_ncStartNetworkType_t * request = adb_ncStartNetworkType_create (env);
    
    // set standard input fields
    adb_ncStartNetworkType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncStartNetworkType_set_correlationId (request, env, CORRELATION_ID);
        adb_ncStartNetworkType_set_userId (request, env, meta->userId);
    }
    
    // set op-specific input fields
    adb_ncStartNetworkType_set_vlan(request, env, vlan);
    adb_ncStartNetworkType_set_remoteHostPort(request, env, port);
    int i;
    for (i=0; i<peersLen; i++) {
        adb_ncStartNetworkType_add_remoteHosts(request, env, peers[i]);
    }
    adb_ncStartNetwork_set_ncStartNetwork(input, env, request);

    int status = 0;
    { // do it
        adb_ncStartNetworkResponse_t * output = axis2_stub_op_EucalyptusNC_ncStartNetwork (stub, env, input);
        
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: StartNetwork" NULL_ERROR_MSG);
            status = -1;

        } else {
            adb_ncStartNetworkResponseType_t * response = adb_ncStartNetworkResponse_get_ncStartNetworkResponse (output, env);
            if ( adb_ncStartNetworkResponseType_get_return(response, env) == AXIS2_FALSE ) {
                logprintfl (EUCAERROR, "ERROR: StartNetwork returned an error\n");
                status = 1;
            }

            // extract the fields from reponse
            if (outStatus != NULL) {
                * outStatus = strdup(adb_ncStartNetworkResponseType_get_networkStatus(response, env));
            }
        }
    }
    
    return status;
}

int ncAttachVolumeStub (ncStub *st, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev) 
{
    axutil_env_t * env  = st->env;
    axis2_stub_t * stub = st->stub;
    adb_ncAttachVolume_t     * input   = adb_ncAttachVolume_create (env); 
    adb_ncAttachVolumeType_t * request = adb_ncAttachVolumeType_create (env);
    
    // set standard input fields
    adb_ncAttachVolumeType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncAttachVolumeType_set_correlationId (request, env, CORRELATION_ID);
        adb_ncAttachVolumeType_set_userId (request, env, meta->userId);
    }
    
    // set op-specific input fields
    adb_ncAttachVolumeType_set_instanceId(request, env, instanceId);
    adb_ncAttachVolumeType_set_volumeId(request, env, volumeId);
    adb_ncAttachVolumeType_set_remoteDev(request, env, remoteDev);
    adb_ncAttachVolumeType_set_localDev(request, env, localDev);
    adb_ncAttachVolume_set_ncAttachVolume(input, env, request);

    int status = 0;
    { // do it
        adb_ncAttachVolumeResponse_t * output = axis2_stub_op_EucalyptusNC_ncAttachVolume (stub, env, input);
        
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: AttachVolume" NULL_ERROR_MSG);
            status = -1;

        } else {
            adb_ncAttachVolumeResponseType_t * response = adb_ncAttachVolumeResponse_get_ncAttachVolumeResponse (output, env);
            if ( adb_ncAttachVolumeResponseType_get_return(response, env) == AXIS2_FALSE ) {
                logprintfl (EUCAERROR, "ERROR: AttachVolume returned an error\n");
                status = 1;
            }
        }
    }
    
    return status;
}

int ncDetachVolumeStub (ncStub *st, ncMetadata *meta, char *instanceId, char *volumeId, char *remoteDev, char *localDev, int force)
{
    axutil_env_t * env  = st->env;
    axis2_stub_t * stub = st->stub;
    adb_ncDetachVolume_t     * input   = adb_ncDetachVolume_create (env); 
    adb_ncDetachVolumeType_t * request = adb_ncDetachVolumeType_create (env);
    
    // set standard input fields
    adb_ncDetachVolumeType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncDetachVolumeType_set_correlationId (request, env, CORRELATION_ID);
        adb_ncDetachVolumeType_set_userId (request, env, meta->userId);
    }
    
    // set op-specific input fields
    adb_ncDetachVolumeType_set_instanceId(request, env, instanceId);
    adb_ncDetachVolumeType_set_volumeId(request, env, volumeId);
    adb_ncDetachVolumeType_set_remoteDev(request, env, remoteDev);
    adb_ncDetachVolumeType_set_localDev(request, env, localDev);
    adb_ncDetachVolumeType_set_force(request, env, force);
    adb_ncDetachVolume_set_ncDetachVolume(input, env, request);

    int status = 0;
    { // do it
        adb_ncDetachVolumeResponse_t * output = axis2_stub_op_EucalyptusNC_ncDetachVolume (stub, env, input);
        
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: DetachVolume" NULL_ERROR_MSG);
            status = -1;

        } else {
            adb_ncDetachVolumeResponseType_t * response = adb_ncDetachVolumeResponse_get_ncDetachVolumeResponse (output, env);
            if ( adb_ncDetachVolumeResponseType_get_return(response, env) == AXIS2_FALSE ) {
                logprintfl (EUCAERROR, "ERROR: DetachVolume returned an error\n");
                status = 1;
            }
        }
    }
    
    return status;
}

/*************************
 a template for future ops
 *************************

    axutil_env_t * env  = stub->env;
    axis2_stub_t * stub = stub->stub;
    adb_ncOPERATION_t     * input   = adb_ncOPERATION_create (env); 
    adb_ncOPERATIONType_t * request = adb_ncOPERATIONType_create (env);
    
    // set standard input fields
    adb_ncOPERATIONType_set_nodeName(request, env, st->node_name);
    if (meta) {
        adb_ncOPERATIONType_set_correlationId (request, env, CORRELATION_ID);
        adb_ncOPERATIONType_set_userId (request, env, meta->userId);
    }
    
    // TODO: set op-specific input fields
    // e.g. adb_ncOPERATIONType_set_Z(request, env, Z);
    adb_ncOPERATION_set_ncOPERATION(input, env, request);

    int status = 0;
    { // do it
        adb_ncOPERATIONResponse_t * output = axis2_stub_op_EucalyptusNC_ncOPERATION (stub, env, input);
        
        if (!output) {
            logprintfl (EUCAERROR, "ERROR: OPERATION" NULL_ERROR_MSG);
            status = -1;

        } else {
            adb_ncOPERATIONResponseType_t * response = adb_ncOPERATIONResponse_get_ncOPERATIONResponse (output, env);
            if ( adb_ncOPERATIONResponseType_get_return(response, env) == AXIS2_FALSE ) {
                logprintfl (EUCAERROR, "ERROR: OPERATION returned an error\n");
                status = 1;
            }

            // TODO: extract the fields from reponse
        }
    }
    
    return status;
*/

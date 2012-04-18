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
ÃÂ  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS  DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

#include <server-marshal-state.h>
#include <handlers.h>
#include <misc.h>
#include <vnetwork.h>
#include "adb-helpers.h"
#include <handlers-state.h>

adb_DescribeServicesResponse_t *DescribeServicesMarshal(adb_DescribeServices_t *describeServices, const axutil_env_t *env) {
  adb_DescribeServicesResponse_t *ret=NULL;
  adb_describeServicesResponseType_t *adbresp=NULL;
  adb_describeServicesType_t *adbinput=NULL;
  
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  ncMetadata ccMeta;

  serviceStatusType *outStatuses=NULL;
  serviceInfoType *serviceIds=NULL;
  int serviceIdsLen=0, outStatusesLen=0, i;
  
  adbinput = adb_DescribeServices_get_DescribeServices(describeServices, env);
  adbresp = adb_describeServicesResponseType_create(env);
  
  EUCA_MESSAGE_UNMARSHAL(describeServicesType, adbinput, (&ccMeta));

  adb_describeServicesResponseType_set_correlationId(adbresp, env, adb_describeServicesType_get_correlationId(adbinput, env));
  adb_describeServicesResponseType_set_userId(adbresp, env, adb_describeServicesType_get_userId(adbinput, env));
    
  //  localDev = adb_describeServicesType_get_localDev(adbinput, env);
  serviceIdsLen = adb_describeServicesType_sizeof_serviceIds(adbinput, env);
  serviceIds = malloc(sizeof(serviceInfoType) * serviceIdsLen);
  for (i=0; i<serviceIdsLen; i++) {
    copy_service_info_type_from_adb(&(serviceIds[i]), adb_describeServicesType_get_serviceIds_at(adbinput, env, i), env);
  }

  status = AXIS2_TRUE;
  rc = doDescribeServices(&ccMeta, serviceIds, serviceIdsLen, &outStatuses, &outStatusesLen);

  if (rc) {
    logprintf("ERROR: doDescribeServices() returned FAIL\n");
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  }

  for (i=0; i<outStatusesLen; i++) {
    adb_serviceStatusType_t *stt;
    adb_serviceInfoType_t *sit;
    
    stt = adb_serviceStatusType_create(env);

    adb_serviceStatusType_set_localState(stt, env, outStatuses[i].localState);
    adb_serviceStatusType_set_localEpoch(stt, env, outStatuses[i].localEpoch);
    adb_serviceStatusType_add_details(stt, env, outStatuses[i].details);
    sit = copy_service_info_type_to_adb(env, &(outStatuses[i].serviceId));
    adb_serviceStatusType_set_serviceId(stt, env, sit);

    adb_describeServicesResponseType_add_serviceStatuses(adbresp, env, stt);
  }
  if(outStatuses) free(outStatuses);
  if(serviceIds) free(serviceIds);
  
  adb_describeServicesResponseType_set_return(adbresp, env, status);
  if (status == AXIS2_FALSE) {
    adb_describeServicesResponseType_set_statusMessage(adbresp, env, statusMessage);
  }

  ret = adb_DescribeServicesResponse_create(env);
  adb_DescribeServicesResponse_set_DescribeServicesResponse(ret, env, adbresp);

  return(ret);
}

adb_StartServiceResponse_t *StartServiceMarshal(adb_StartService_t *startService, const axutil_env_t *env) {
  adb_StartServiceResponse_t *ret=NULL;
  adb_startServiceResponseType_t *adbresp=NULL;
  adb_startServiceType_t *adbinput=NULL;
  
  int rc, i;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  ncMetadata ccMeta;
  
  adbinput = adb_StartService_get_StartService(startService, env);
  adbresp = adb_startServiceResponseType_create(env);
  
  // unmarshal eucalyptusMessage into ccMeta
  EUCA_MESSAGE_UNMARSHAL(startServiceType, adbinput, (&ccMeta));

  // set the fields that are simply carried through between input and output messages
  adb_startServiceResponseType_set_correlationId(adbresp, env, adb_startServiceType_get_correlationId(adbinput, env));
  adb_startServiceResponseType_set_userId(adbresp, env, adb_startServiceType_get_userId(adbinput, env));
  for (i=0; i<adb_startServiceType_sizeof_serviceIds(adbinput, env); i++) { adb_startServiceResponseType_add_serviceIds(adbresp, env, adb_startServiceType_get_serviceIds_at(adbinput, env, i)); }

  
  status = AXIS2_TRUE;
  rc = doStartService(&ccMeta);
  if (rc) {
    logprintf("ERROR: doStartService() returned FAIL\n");
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  }

  adb_startServiceResponseType_set_return(adbresp, env, status);
  if (status == AXIS2_FALSE) {
    adb_startServiceResponseType_set_statusMessage(adbresp, env, statusMessage);
  }

  ret = adb_StartServiceResponse_create(env);
  adb_StartServiceResponse_set_StartServiceResponse(ret, env, adbresp);

  return(ret);
}

adb_StopServiceResponse_t *StopServiceMarshal(adb_StopService_t *stopService, const axutil_env_t *env) {
  adb_StopServiceResponse_t *ret=NULL;
  adb_stopServiceResponseType_t *adbresp=NULL;
  adb_stopServiceType_t *adbinput=NULL;
  
  int rc, i;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  ncMetadata ccMeta;
  
  adbinput = adb_StopService_get_StopService(stopService, env);
  adbresp = adb_stopServiceResponseType_create(env);
  
  // unmarshal eucalyptusMessage into ccMeta
  EUCA_MESSAGE_UNMARSHAL(stopServiceType, adbinput, (&ccMeta));

  // set the fields that are simply carried through between input and output messages
  adb_stopServiceResponseType_set_correlationId(adbresp, env, adb_stopServiceType_get_correlationId(adbinput, env));
  adb_stopServiceResponseType_set_userId(adbresp, env, adb_stopServiceType_get_userId(adbinput, env));
  for (i=0; i<adb_stopServiceType_sizeof_serviceIds(adbinput, env); i++) { adb_stopServiceResponseType_add_serviceIds(adbresp, env, adb_stopServiceType_get_serviceIds_at(adbinput, env, i)); }
  
  status = AXIS2_TRUE;
  rc = doStopService(&ccMeta);
  if (rc) {
    logprintf("ERROR: doStopService() returned FAIL\n");
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  }

  adb_stopServiceResponseType_set_return(adbresp, env, status);
  if (status == AXIS2_FALSE) {
    adb_stopServiceResponseType_set_statusMessage(adbresp, env, statusMessage);
  }

  ret = adb_StopServiceResponse_create(env);
  adb_StopServiceResponse_set_StopServiceResponse(ret, env, adbresp);

  return(ret);
}

adb_EnableServiceResponse_t *EnableServiceMarshal(adb_EnableService_t *enableService, const axutil_env_t *env) {
  adb_EnableServiceResponse_t *ret=NULL;
  adb_enableServiceResponseType_t *adbresp=NULL;
  adb_enableServiceType_t *adbinput=NULL;
  
  int rc, i;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  ncMetadata ccMeta;
  
  adbinput = adb_EnableService_get_EnableService(enableService, env);
  adbresp = adb_enableServiceResponseType_create(env);
  
  // unmarshal eucalyptusMessage into ccMeta
  EUCA_MESSAGE_UNMARSHAL(enableServiceType, adbinput, (&ccMeta));

  // set the fields that are simply carried through between input and output messages
  adb_enableServiceResponseType_set_correlationId(adbresp, env, adb_enableServiceType_get_correlationId(adbinput, env));
  adb_enableServiceResponseType_set_userId(adbresp, env, adb_enableServiceType_get_userId(adbinput, env));
  for (i=0; i<adb_enableServiceType_sizeof_serviceIds(adbinput, env); i++) { adb_enableServiceResponseType_add_serviceIds(adbresp, env, adb_enableServiceType_get_serviceIds_at(adbinput, env, i)); }
  
  status = AXIS2_TRUE;
  rc = doEnableService(&ccMeta);
  if (rc) {
    logprintf("ERROR: doEnableService() returned FAIL\n");
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  }
  
  adb_enableServiceResponseType_set_return(adbresp, env, status);
  if (status == AXIS2_FALSE) {
    adb_enableServiceResponseType_set_statusMessage(adbresp, env, statusMessage);
  }

  ret = adb_EnableServiceResponse_create(env);
  adb_EnableServiceResponse_set_EnableServiceResponse(ret, env, adbresp);

  return(ret);
}

adb_DisableServiceResponse_t *DisableServiceMarshal(adb_DisableService_t *disableService, const axutil_env_t *env) {
  adb_DisableServiceResponse_t *ret=NULL;
  adb_disableServiceResponseType_t *adbresp=NULL;
  adb_disableServiceType_t *adbinput=NULL;
  
  int rc, i;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  ncMetadata ccMeta;
  
  adbinput = adb_DisableService_get_DisableService(disableService, env);
  adbresp = adb_disableServiceResponseType_create(env);
  
  // unmarshal eucalyptusMessage into ccMeta
  EUCA_MESSAGE_UNMARSHAL(disableServiceType, adbinput, (&ccMeta));

  // set the fields that are simply carried through between input and output messages
  adb_disableServiceResponseType_set_correlationId(adbresp, env, adb_disableServiceType_get_correlationId(adbinput, env));
  adb_disableServiceResponseType_set_userId(adbresp, env, adb_disableServiceType_get_userId(adbinput, env));
  for (i=0; i<adb_disableServiceType_sizeof_serviceIds(adbinput, env); i++) { adb_disableServiceResponseType_add_serviceIds(adbresp, env, adb_disableServiceType_get_serviceIds_at(adbinput, env, i)); }
  
  status = AXIS2_TRUE;
  rc = doDisableService(&ccMeta);
  if (rc) {
    logprintf("ERROR: doDisableService() returned FAIL\n");
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  }

  adb_disableServiceResponseType_set_return(adbresp, env, status);
  if (status == AXIS2_FALSE) {
    adb_disableServiceResponseType_set_statusMessage(adbresp, env, statusMessage);
  }

  ret = adb_DisableServiceResponse_create(env);
  adb_DisableServiceResponse_set_DisableServiceResponse(ret, env, adbresp);

  return(ret);
}

adb_ShutdownServiceResponse_t *ShutdownServiceMarshal(adb_ShutdownService_t *shutdownService, const axutil_env_t *env) {
  adb_ShutdownServiceResponse_t *ret=NULL;
  adb_shutdownServiceResponseType_t *adbresp=NULL;
  adb_shutdownServiceType_t *adbinput=NULL;
  
  int rc, i;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  ncMetadata ccMeta;
  
  adbinput = adb_ShutdownService_get_ShutdownService(shutdownService, env);
  adbresp = adb_shutdownServiceResponseType_create(env);
  
  // unmarshal eucalyptusMessage into ccMeta
  EUCA_MESSAGE_UNMARSHAL(shutdownServiceType, adbinput, (&ccMeta));

  // set the fields that are simply carried through between input and output messages
  adb_shutdownServiceResponseType_set_correlationId(adbresp, env, adb_shutdownServiceType_get_correlationId(adbinput, env));
  adb_shutdownServiceResponseType_set_userId(adbresp, env, adb_shutdownServiceType_get_userId(adbinput, env));
  for (i=0; i<adb_shutdownServiceType_sizeof_serviceIds(adbinput, env); i++) { adb_shutdownServiceResponseType_add_serviceIds(adbresp, env, adb_shutdownServiceType_get_serviceIds_at(adbinput, env, i)); }
  
  status = AXIS2_TRUE;
  rc = doShutdownService(&ccMeta);
  if (rc) {
    logprintf("ERROR: doShutdownService() returned FAIL\n");
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  }

  adb_shutdownServiceResponseType_set_return(adbresp, env, status);
  if (status == AXIS2_FALSE) {
    adb_shutdownServiceResponseType_set_statusMessage(adbresp, env, statusMessage);
  }

  ret = adb_ShutdownServiceResponse_create(env);
  adb_ShutdownServiceResponse_set_ShutdownServiceResponse(ret, env, adbresp);

  return(ret);
}


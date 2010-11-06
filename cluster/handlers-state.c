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
  THE REGENTS  DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <semaphore.h>
#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <signal.h>

#include "axis2_skel_EucalyptusCC.h"

#include <server-marshal.h>
#include <handlers.h>
#include <storage.h>
#include <vnetwork.h>
#include <misc.h>
#include <ipc.h>
#include <walrus.h>

#include <euca_axis.h>
#include "data.h"
#include "client-marshal.h"

#include <storage-windows.h>
#include <euca_auth.h>
#include <handlers-state.h>

extern ccConfig *config;
extern ccInstanceCache *instanceCache;
extern ccResourceCache *resourceCache;
extern ccResourceCache *resourceCacheStage;
extern vnetConfig *vnetconfig;

int doDescribeServices(ncMetadata *ccMeta, serviceInfoType *serviceIds, int serviceIdsLen, serviceStatusType **outStatuses, int *outStatusesLen) {
  int i, rc, ret=0;

  rc = initialize();
  if (rc || ccIsEnabled()) {
    return(1);
  }

  logprintfl(EUCAINFO, "DescribeServices(): called\n");
  logprintfl(EUCADEBUG, "DescribeServices(): params: userId=%s, serviceIdsLen=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), serviceIdsLen);

  // go through input service descriptions and match with self and node states

  *outStatusesLen = serviceIdsLen;
  *outStatuses = malloc(sizeof(serviceStatusType) * *outStatusesLen);
  for (i=0; i<serviceIdsLen; i++) {
    char statestr[32];
    logprintfl(EUCADEBUG, "DescribeServices(): serviceId=%d type=%s name=%s urisLen=%d\n", i, serviceIds[i].type, serviceIds[i].name, serviceIds[i].urisLen);
    
    ccGetStateString(statestr, 32);
    snprintf((*outStatuses)[i].localState, 32, "%s", statestr);
    
    snprintf((*outStatuses)[i].details, 1024, "%s", config->ccStateDetails);
    (*outStatuses)[i].localEpoch = 0;    
    memcpy(&((*outStatuses)[i].serviceId), &(serviceIds[i]), sizeof(serviceInfoType));
  }

  logprintfl(EUCAINFO, "DescribeServices(): done\n");
  return(ret);
}

int doStartService(ncMetadata *ccMeta) {
  int i, rc, ret=0;

  rc = initialize();
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "StartService(): called\n");
  logprintfl(EUCADEBUG, "StartService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  // this is actually a NOP
  config->ccState = DISABLED;

  logprintfl(EUCAINFO, "StartService(): done\n");

  return(ret);
}

int doStopService(ncMetadata *ccMeta) {
  int i, rc, ret=0;

  rc = initialize();
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "StopService(): called\n");
  logprintfl(EUCADEBUG, "StopService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  config->ccState = STOPPED;

  logprintfl(EUCAINFO, "StopService(): done\n");

  return(ret);
}

int doEnableService(ncMetadata *ccMeta) {
  int i, rc, ret=0;

  rc = initialize();
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "EnableService(): called\n");
  logprintfl(EUCADEBUG, "EnableService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  config->ccState = ENABLED;

  logprintfl(EUCAINFO, "EnableService(): done\n");

  return(ret);
}

int doDisableService(ncMetadata *ccMeta) {
  int i, rc, ret=0;

  rc = initialize();
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "DisableService(): called\n");
  logprintfl(EUCADEBUG, "DisableService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  config->ccState = DISABLED;

  logprintfl(EUCAINFO, "DisableService(): done\n");

  return(ret);
}


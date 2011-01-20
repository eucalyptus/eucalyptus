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

#include <euca_auth.h>
#include <handlers-state.h>

extern ccConfig *config;
extern ccInstanceCache *instanceCache;
extern ccResourceCache *resourceCache;
extern ccResourceCache *resourceCacheStage;
extern vnetConfig *vnetconfig;

int doDescribeServices(ncMetadata *ccMeta, serviceInfoType *serviceIds, int serviceIdsLen, serviceStatusType **outStatuses, int *outStatusesLen) {
  int i, rc, ret=0;

  rc = initialize(ccMeta);
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
    
    snprintf((*outStatuses)[i].localState, 32, "%s", config->ccStatus.localState);    
    snprintf((*outStatuses)[i].details, 1024, "%s", config->ccStatus.details);
    (*outStatuses)[i].localEpoch = config->ccStatus.localEpoch;    
    memcpy(&((*outStatuses)[i].serviceId), &(serviceIds[i]), sizeof(serviceInfoType));
  }
  
  logprintfl(EUCAINFO, "DescribeServices(): done\n");
  return(ret);
}

int doStartService(ncMetadata *ccMeta) {
  int i, rc, ret=0;

  rc = initialize(ccMeta);
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "StartService(): called\n");
  logprintfl(EUCADEBUG, "StartService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  // this is actually a NOP
  sem_mywait(CONFIG);
  ccChangeState(DISABLED);
  sem_mypost(CONFIG);
  
  logprintfl(EUCAINFO, "StartService(): done\n");
  
  return(ret);
}

int doStopService(ncMetadata *ccMeta) {
  int i, rc, ret=0;
  
  rc = initialize(ccMeta);
  if (rc) {
    return(1);
  }
  
  logprintfl(EUCAINFO, "StopService(): called\n");
  logprintfl(EUCADEBUG, "StopService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));
  
  sem_mywait(CONFIG);
  ccChangeState(STOPPED);
  sem_mypost(CONFIG);

  logprintfl(EUCAINFO, "StopService(): done\n");

  return(ret);
}

int doEnableService(ncMetadata *ccMeta) {
  int i, rc, ret=0;

  rc = initialize(ccMeta);
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "EnableService(): called\n");
  logprintfl(EUCADEBUG, "EnableService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  sem_mywait(CONFIG);
  // set state to ENABLED
  config->kick_network = 1;
  config->kick_dhcp = 1;
  ccChangeState(ENABLED);
  sem_mypost(CONFIG);  

  logprintfl(EUCAINFO, "EnableService(): done\n");
  
  return(ret);
}

int doDisableService(ncMetadata *ccMeta) {
  int i, rc, ret=0;

  rc = initialize(ccMeta);
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "DisableService(): called\n");
  logprintfl(EUCADEBUG, "DisableService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  sem_mywait(CONFIG);
  ccChangeState(DISABLED);
  sem_mypost(CONFIG);

  logprintfl(EUCAINFO, "DisableService(): done\n");

  return(ret);
}

int validCmp(ccInstance *inst, void *in) {
  if (!inst) {
    return(1);
  }
  
  if (inst->instanceId[0] == '\0') {
    return(1);
  }
  
  return(0);
}

int instNetParamsSet(ccInstance *inst, void *in) {
  int rc, ret=0, i;

  if (!inst) {
    return(1);
  }

  sem_mywait(VNET);
  if (inst->ccnet.vlan >= 0) {
    // activate network
    vnetconfig->networks[inst->ccnet.vlan].active = 1;
    
    // set up groupName and userName
    if (inst->groupNames[0][0] != '\0' && inst->ownerId[0] != '\0') {
      if ( (vnetconfig->users[inst->ccnet.vlan].netName[0] != '\0' && strcmp(vnetconfig->users[inst->ccnet.vlan].netName, inst->groupNames[0])) || (vnetconfig->users[inst->ccnet.vlan].userName[0] != '\0' && strcmp(vnetconfig->users[inst->ccnet.vlan].userName, inst->ownerId)) ) {
	// this means that there is a pre-existing network with the passed in vlan tag, but with a different netName or userName
	ret = 1;
      } else {
	snprintf(vnetconfig->users[inst->ccnet.vlan].netName, 32, "%s", inst->groupNames[0]);
	snprintf(vnetconfig->users[inst->ccnet.vlan].userName, 32, "%s", inst->ownerId);
      }
    }
  }

  if (!ret) {
    // so far so good
    rc = vnetGenerateNetworkParams(vnetconfig, inst->instanceId, inst->ccnet.vlan, inst->ccnet.networkIndex, inst->ccnet.privateMac, inst->ccnet.publicIp, inst->ccnet.privateIp);
    if (rc) {
      print_ccInstance("instNetParamsSet(): failed to (re)generate network parameters: ", inst);
      ret = 1;
    }
  }
  sem_mypost(VNET);

  return(0);
}

int clean_network_state(void) {
  int rc, i;
  char cmd[MAX_PATH], file[MAX_PATH], rootwrap[MAX_PATH];
  struct stat statbuf;
  vnetConfig *tmpvnetconfig;

  tmpvnetconfig = malloc(sizeof(vnetConfig));
  memcpy(tmpvnetconfig, vnetconfig, sizeof(vnetConfig));
  
  // clean up assigned addrs, iptables, dhcpd (and configs)
  rc = vnetApplySingleTableRule(tmpvnetconfig, "filter", "-F");
  if (rc) {
  }
  rc = vnetApplySingleTableRule(tmpvnetconfig, "nat", "-F");
  if (rc) {
  }
  rc = vnetApplySingleTableRule(tmpvnetconfig, "filter", "-P FORWARD ACCEPT");
  if (rc) {
  }
  
  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del 169.254.169.254/32 dev %s", config->eucahome, tmpvnetconfig->pubInterface);
  logprintfl(EUCAINFO,"clean_network_state(): running cmd %s\n", cmd);
  rc = system(cmd);
  if (rc) {
    logprintfl(EUCAWARN, "clean_network_state(): cannot remove ip 169.254.169.254\n");
  }
  for (i=1; i<NUMBER_OF_PUBLIC_IPS; i++) {
    if (tmpvnetconfig->publicips[i].ip != 0) {
      logprintfl(EUCADEBUG, "clean_network_state(): IP addr: %s\n", hex2dot(tmpvnetconfig->publicips[i].ip));
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del %s/32 dev %s", config->eucahome, hex2dot(tmpvnetconfig->publicips[i].ip), tmpvnetconfig->pubInterface);
      logprintfl(EUCAINFO,"clean_network_state(): running cmd %s\n", cmd);
      rc = system(cmd);
      if (rc) {
	logprintfl(EUCAWARN, "clean_network_state(): cannot remove ip %s\n", hex2dot(tmpvnetconfig->publicips[i].ip));
      }
    }
  }


  // dhcp
  snprintf(file, MAX_PATH, "%s/euca-dhcp.pid", tmpvnetconfig->path);
  snprintf(rootwrap, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", tmpvnetconfig->eucahome);
  rc = safekillfile(file, tmpvnetconfig->dhcpdaemon, 9, rootwrap);
  if (rc) {
    logprintfl(EUCAERROR, "clean_network_state(): could not terminate dhcpd (%s)\n", tmpvnetconfig->dhcpdaemon);
  }

  for (i=2; i<NUMBER_OF_VLANS; i++) {
    if (tmpvnetconfig->networks[i].active) {
      rc = vnetStopNetwork(tmpvnetconfig, i, tmpvnetconfig->users[i].userName, tmpvnetconfig->users[i].netName);
      if (rc) {
	logprintfl(EUCADEBUG, "clean_network_state(): failed to tear down network %d\n");
      }
    }
  }

  if (tmpvnetconfig) free(tmpvnetconfig);
  return(0);
}

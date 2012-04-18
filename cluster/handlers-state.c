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
  serviceStatusType *myStatus=NULL;

  rc = initialize(ccMeta);
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "DescribeServices(): called\n");
  logprintfl(EUCADEBUG, "DescribeServices(): params: userId=%s, serviceIdsLen=%d\n", SP(ccMeta ? ccMeta->userId : "UNSET"), serviceIdsLen);

  // TODO: for now, return error if list of services is passed in as parameter
  /*
  if (serviceIdsLen > 0) {
    logprintfl(EUCAERROR, "DescribeServices(): received non-zero number of input services, returning fail\n");
    *outStatusesLen = 0;
    *outStatuses = NULL;
    return(1);
  }
  */
  sem_mywait(CONFIG);
  if (!strcmp(config->ccStatus.serviceId.name, "self")) {
    for (i=0; i<serviceIdsLen; i++) {
      logprintfl(EUCADEBUG, "DescribeServices(): received input serviceId[%d]\n", i);
      if (strlen(serviceIds[i].type)) {
	if (!strcmp(serviceIds[i].type, "cluster")) {
	  char uri[MAX_PATH], uriType[32], host[MAX_PATH], path[MAX_PATH];
	  int port;
	  snprintf(uri, MAX_PATH, "%s", serviceIds[i].uris[0]);
	  rc = tokenize_uri(uri, uriType, host, &port, path);
	  if (strlen(host)) {
	    logprintfl(EUCADEBUG, "DescribeServices(): setting local serviceId to input serviceId (type=%s name=%s)\n", SP(serviceIds[i].type), SP(serviceIds[i].name));
	    memcpy(&(config->ccStatus.serviceId), &(serviceIds[i]), sizeof(serviceInfoType));
	  }
	}
      }
    }
  }
  sem_mypost(CONFIG);

  for (i=0; i<16; i++) {
    int j;
    if (strlen(config->services[i].type)) {
      logprintfl(EUCADEBUG, "DescribeServices(): internal serviceInfos type=%s name=%s urisLen=%d\n", config->services[i].type, config->services[i].name, config->services[i].urisLen);
      for (j=0; j<8; j++) {
	if (strlen(config->services[i].uris[j])) {
	  logprintfl(EUCADEBUG, "DescribeServices(): internal serviceInfos\t uri[%d]:%s\n", j, config->services[i].uris[j]);
	}
      }
    }
  }

  for (i=0; i<16; i++) {
    int j;
    if (strlen(config->disabledServices[i].type)) {
      logprintfl(EUCADEBUG, "DescribeServices(): internal disabled serviceInfos type=%s name=%s urisLen=%d\n", config->disabledServices[i].type, config->disabledServices[i].name, config->disabledServices[i].urisLen);
      for (j=0; j<8; j++) {
	if (strlen(config->disabledServices[i].uris[j])) {
	  logprintfl(EUCADEBUG, "DescribeServices(): internal disabled serviceInfos\t uri[%d]:%s\n", j, config->disabledServices[i].uris[j]);
	}
      }
    }
  }

  for (i=0; i<16; i++) {
    int j;
    if (strlen(config->notreadyServices[i].type)) {
      logprintfl(EUCADEBUG, "DescribeServices(): internal not ready serviceInfos type=%s name=%s urisLen=%d\n", config->notreadyServices[i].type, config->notreadyServices[i].name, config->notreadyServices[i].urisLen);
      for (j=0; j<8; j++) {
	if (strlen(config->notreadyServices[i].uris[j])) {
	  logprintfl(EUCADEBUG, "DescribeServices(): internal not ready serviceInfos\t uri[%d]:%s\n", j, config->notreadyServices[i].uris[j]);
	}
      }
    }
  }
  
  *outStatusesLen = 1;
  *outStatuses = malloc(sizeof(serviceStatusType));
  if (!*outStatuses) {
    logprintfl(EUCAFATAL, "DescribeServices(): out of memory!\n");
    unlock_exit(1);
  }

  myStatus = *outStatuses;
  snprintf(myStatus->localState, 32, "%s", config->ccStatus.localState);
  snprintf(myStatus->details, 1024, "%s", config->ccStatus.details);
  myStatus->localEpoch = config->ccStatus.localEpoch;
  memcpy(&(myStatus->serviceId), &(config->ccStatus.serviceId), sizeof(serviceInfoType));

  logprintfl(EUCAINFO, "DescribeServices(): done\n");
  return(0);
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
  if (config->ccState == SHUTDOWNCC) {
    logprintfl(EUCAWARN, "StartService(): attempt to start a shutdown CC, skipping.\n");
    ret++;
  } else if (ccCheckState(0)) {
    logprintfl(EUCAWARN, "StartService(): ccCheckState() returned failures, skipping.\n");
    ret++;
  } else {
    logprintfl(EUCADEBUG, "StartService(): starting service\n");
    ret=0;
    config->kick_enabled = 0;
    ccChangeState(DISABLED);
  }
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
  if (config->ccState == SHUTDOWNCC) {
    logprintfl(EUCAWARN, "StopService(): attempt to stop a shutdown CC, skipping.\n");
    ret++;
  } else if (ccCheckState(0)) {
    logprintfl(EUCAWARN, "StopService(): ccCheckState() returned failures, skipping.\n");
    ret++;
  } else {
    logprintfl(EUCADEBUG, "StopService(): stopping service\n");
    ret=0;
    config->kick_enabled = 0;
    ccChangeState(STOPPED);
  }
  sem_mypost(CONFIG);

  logprintfl(EUCAINFO, "StopService(): done\n");

  return(ret);
}

int doEnableService(ncMetadata *ccMeta) {
  int i, rc, ret=0, done=0;

  rc = initialize(ccMeta);
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "EnableService(): called\n");
  logprintfl(EUCADEBUG, "EnableService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  sem_mywait(CONFIG);
  if (config->ccState == SHUTDOWNCC) {
    logprintfl(EUCAWARN, "EnableService(): attempt to enable a shutdown CC, skipping.\n");
    ret++;
  } else if (ccCheckState(0)) {
    logprintfl(EUCAWARN, "EnableService(): ccCheckState() returned failures, skipping.\n");
    ret++;
  } else if (config->ccState != ENABLED) {
    logprintfl(EUCADEBUG, "EnableService(): enabling service\n");
    ret=0;
    // tell monitor thread to (re)enable  
    config->kick_monitor_running = 0;
    config->kick_network = 1;
    config->kick_dhcp = 1;
    config->kick_enabled = 1;
    ccChangeState(ENABLED);
  }
  sem_mypost(CONFIG);  

  if (config->ccState == ENABLED) {
    // wait for a minute to make sure CC is running again
    done=0;
    for (i=0; i<60 && !done; i++) {
      sem_mywait(CONFIG);
      if (config->kick_monitor_running) {
	done++;
      }
      sem_mypost(CONFIG);
      if (!done) {
	logprintfl(EUCADEBUG, "EnableService(): waiting for monitor to re-initialize (%d/60)\n", i);
	sleep(1);
      }
    }
  }

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
  if (config->ccState == SHUTDOWNCC) {
    logprintfl(EUCAWARN, "DisableService(): attempt to disable a shutdown CC, skipping.\n");
    ret++;
  } else if (ccCheckState(0)) {
    logprintfl(EUCAWARN, "DisableService(): ccCheckState() returned failures, skipping.\n");
    ret++;
  } else {
    logprintfl(EUCADEBUG, "DisableService(): disabling service\n");
    ret=0;
    config->kick_enabled = 0;
    ccChangeState(DISABLED);
  }
  sem_mypost(CONFIG);

  logprintfl(EUCAINFO, "DisableService(): done\n");

  return(ret);
}

int doShutdownService(ncMetadata *ccMeta) {
  int i, rc, ret=0;

  rc = initialize(ccMeta);
  if (rc) {
    return(1);
  }

  logprintfl(EUCAINFO, "ShutdownService(): called\n");
  logprintfl(EUCADEBUG, "ShutdownService(): params: userId=%s\n", SP(ccMeta ? ccMeta->userId : "UNSET"));

  sem_mywait(CONFIG);
  config->kick_enabled = 0;
  ccChangeState(SHUTDOWNCC);
  sem_mypost(CONFIG);

  logprintfl(EUCAINFO, "ShutdownService(): done\n");

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

int instIpSync(ccInstance *inst, void *in) {
  int ret=0;

  /*
  if ( (strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant")) || !strcmp(inst->ccState, "ccTeardown")) {
    return(0);
  }
  */

  if (!inst) {
    return(1);
  } else if ( (strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant")) ) {
    return(0);
  }

  logprintfl(EUCADEBUG, "instIpSync(): instanceId=%s CCpublicIp=%s CCprivateIp=%s CCprivateMac=%s CCvlan=%d CCnetworkIndex=%d NCpublicIp=%s NCprivateIp=%s NCprivateMac=%s NCvlan=%d NCnetworkIndex=%d\n", inst->instanceId, inst->ccnet.publicIp, inst->ccnet.privateIp, inst->ccnet.privateMac, inst->ccnet.vlan, inst->ccnet.networkIndex, inst->ncnet.publicIp, inst->ncnet.privateIp, inst->ncnet.privateMac, inst->ncnet.vlan, inst->ncnet.networkIndex);

  if (inst->ccnet.vlan == 0 && inst->ccnet.networkIndex == 0 && inst->ccnet.publicIp[0] == '\0' && inst->ccnet.privateIp[0] == '\0' && inst->ccnet.privateMac[0] == '\0') {
    // ccnet is completely empty, make a copy of ncnet
    logprintfl(EUCADEBUG, "instIpSync(): ccnet is empty, copying ncnet\n");
    memcpy(&(inst->ccnet), &(inst->ncnet), sizeof(netConfig));
    return(1);
  }
  
  // IP cases
  // 1.) local CC cache has no IP info for VM, NC VM has no IP info
  //     - do nothing
  // 2.) local CC cache has no IP info, NC VM has IP info
  //     - ingress NC info, kick_network
  // 3.) local CC cache has IP info, NC VM has no IP info
  //     - send ncAssignAddress
  // 4.) local CC cache has IP info, NC VM has different IP info
  //     - ingress NC info, kick_network
  // 5.) local CC cache has IP info, NC VM has same IP info
  //     - do nothing
  if ((inst->ccnet.publicIp[0] == '\0' || !strcmp(inst->ccnet.publicIp, "0.0.0.0")) && (inst->ncnet.publicIp[0] != '\0' && strcmp(inst->ncnet.publicIp, "0.0.0.0"))) {
    // case 2
    logprintfl(EUCADEBUG, "instIpSync(): CC publicIp is empty, NC publicIp is set\n");
    snprintf(inst->ccnet.publicIp, 24, "%s", inst->ncnet.publicIp);
    ret++;
  } else if (( (inst->ccnet.publicIp[0] != '\0' && strcmp(inst->ccnet.publicIp, "0.0.0.0")) && (inst->ncnet.publicIp[0] != '\0' && strcmp(inst->ncnet.publicIp, "0.0.0.0")) ) && strcmp(inst->ccnet.publicIp, inst->ncnet.publicIp)) {
    // case 4
    logprintfl(EUCADEBUG, "instIpSync(): CC publicIp and NC publicIp differ\n");
    snprintf(inst->ccnet.publicIp, 24, "%s", inst->ncnet.publicIp);
    ret++;
  }

  // VLAN cases
  if (inst->ccnet.vlan != inst->ncnet.vlan) {
    // problem
    logprintfl(EUCAERROR, "instIpSync(): CC and NC vlans differ instanceId=%s CCvlan=%d NCvlan=%d\n", inst->instanceId, inst->ccnet.vlan, inst->ncnet.vlan);
  }
  inst->ccnet.vlan = inst->ncnet.vlan;
  if (inst->ccnet.vlan >= 0) {
    if (!vnetconfig->networks[inst->ccnet.vlan].active) {
      logprintfl(EUCAWARN, "instIpSync(): detected instance from NC that is running in a currently inactive network; will attempt to re-activate network '%d'\n", inst->ccnet.vlan);
      ret++;
    }
  }

  // networkIndex cases
  if (inst->ccnet.networkIndex != inst->ncnet.networkIndex) {
    // problem
    logprintfl(EUCAERROR, "instIpSync(): CC and NC networkIndicies differ instanceId=%s CCnetworkIndex=%d NCnetworkIndex=%d\n", inst->instanceId, inst->ccnet.networkIndex, inst->ncnet.networkIndex);
  }
  inst->ccnet.networkIndex = inst->ncnet.networkIndex;

  // mac addr cases
  if (strcmp(inst->ccnet.privateMac, inst->ncnet.privateMac)) {
    // problem;
    logprintfl(EUCAERROR, "instIpSync(): CC and NC mac addrs differ instanceId=%s CCmac=%s NCmac=%s\n", inst->instanceId, inst->ccnet.privateMac, inst->ncnet.privateMac);
  }
  snprintf(inst->ccnet.privateMac, 24, "%s", inst->ncnet.privateMac);

  // privateIp cases
  if (strcmp(inst->ccnet.privateIp, inst->ncnet.privateIp)) {
     // sync em
     snprintf(inst->ccnet.privateIp, 24, "%s", inst->ncnet.privateIp);
  }

  return(ret);
}

int instNetParamsSet(ccInstance *inst, void *in) {
  int rc, ret=0, i;
  char userToken[64], *cleanGroupName=NULL;

  if (!inst) {
    return(1);
  } else if ( (strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant")) ) {
    return(0);
  }

  logprintfl(EUCADEBUG, "instNetParamsSet(): instanceId=%s publicIp=%s privateIp=%s privateMac=%s vlan=%d\n", inst->instanceId, inst->ccnet.publicIp, inst->ccnet.privateIp, inst->ccnet.privateMac, inst->ccnet.vlan);

  if (inst->ccnet.vlan >= 0) {
    // activate network
    vnetconfig->networks[inst->ccnet.vlan].active = 1;
    
    // set up groupName and userName
    if (inst->groupNames[0][0] != '\0' && inst->accountId[0] != '\0') {
      // logic to strip the username from the supplied network name
      snprintf(userToken, 63, "%s-", inst->accountId);
      cleanGroupName = strstr(inst->groupNames[0], userToken);
      if (cleanGroupName) {
	cleanGroupName = cleanGroupName + strlen(userToken);
      } else {
	cleanGroupName = inst->groupNames[0];
      }

      //      if ( (vnetconfig->users[inst->ccnet.vlan].netName[0] != '\0' && strcmp(vnetconfig->users[inst->ccnet.vlan].netName, inst->groupNames[0])) || (vnetconfig->users[inst->ccnet.vlan].userName[0] != '\0' && strcmp(vnetconfig->users[inst->ccnet.vlan].userName, inst->accountId)) ) {
      if ( (vnetconfig->users[inst->ccnet.vlan].netName[0] != '\0' && strcmp(vnetconfig->users[inst->ccnet.vlan].netName, cleanGroupName)) || (vnetconfig->users[inst->ccnet.vlan].userName[0] != '\0' && strcmp(vnetconfig->users[inst->ccnet.vlan].userName, inst->accountId)) ) {
	// this means that there is a pre-existing network with the passed in vlan tag, but with a different netName or userName
	logprintfl(EUCAERROR, "instNetParamsSet(): input instance vlan<->user<->netname mapping is incompatible with internal state. Internal - userName=%s netName=%s vlan=%d.  Instance - userName=%s netName=%s vlan=%d\n", vnetconfig->users[inst->ccnet.vlan].userName, vnetconfig->users[inst->ccnet.vlan].netName, inst->ccnet.vlan, inst->accountId, cleanGroupName, inst->ccnet.vlan);
	ret = 1;
      } else {
	//	snprintf(vnetconfig->users[inst->ccnet.vlan].netName, 32, "%s", inst->groupNames[0]);
	snprintf(vnetconfig->users[inst->ccnet.vlan].netName, 64, "%s", cleanGroupName);
	snprintf(vnetconfig->users[inst->ccnet.vlan].userName, 48, "%s", inst->accountId);
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

  if (ret) {
    logprintfl(EUCADEBUG, "instNetParamsSet(): sync of network cache with instance data FAILED (instanceId=%s, publicIp=%s, privateIp=%s, vlan=%d, networkIndex=%d\n", inst->instanceId, inst->ccnet.publicIp, inst->ccnet.privateIp, inst->ccnet.vlan, inst->ccnet.networkIndex); 
  } else {
    logprintfl(EUCADEBUG, "instNetParamsSet(): sync of network cache with instance data SUCCESS (instanceId=%s, publicIp=%s, privateIp=%s, vlan=%d, networkIndex=%d\n", inst->instanceId, inst->ccnet.publicIp, inst->ccnet.privateIp, inst->ccnet.vlan, inst->ccnet.networkIndex); 
  }

  return(0);
}

int instNetReassignAddrs(ccInstance *inst, void *in) {
  int rc, ret=0, i;

  if (!inst) {
    return(1);
  } else if ( (strcmp(inst->state, "Pending") && strcmp(inst->state, "Extant")) ) {
    return(0);
  }

  logprintfl(EUCADEBUG, "instNetReassignAddrs(): instanceId=%s publicIp=%s privateIp=%s\n", inst->instanceId, inst->ccnet.publicIp, inst->ccnet.privateIp);
  if (!strcmp(inst->ccnet.publicIp, "0.0.0.0") || !strcmp(inst->ccnet.privateIp, "0.0.0.0")) {
    logprintfl(EUCAWARN, "instNetReassignAddrs(): ignoring instance with unset publicIp/privateIp\n");
  } else { 
    rc = vnetReassignAddress(vnetconfig, "UNSET", inst->ccnet.publicIp, inst->ccnet.privateIp);
    if (rc) {
      logprintfl(EUCAERROR, "instNetReassignAddrs(): cannot reassign address\n");
      ret = 1;
    }
  }

  return(0);
}

int clean_network_state(void) {
  int rc, i;
  char cmd[MAX_PATH], file[MAX_PATH], rootwrap[MAX_PATH], *pidstr=NULL, *ipstr=NULL;
  struct stat statbuf;
  vnetConfig *tmpvnetconfig;

  tmpvnetconfig = malloc(sizeof(vnetConfig));
  if(!tmpvnetconfig) { 
    logprintfl(EUCAERROR, "clean_network_state(): out of memory\n");
    return -1;
  }

  memcpy(tmpvnetconfig, vnetconfig, sizeof(vnetConfig));
  
  rc = vnetUnsetMetadataRedirect(tmpvnetconfig);
  if (rc) {
    logprintfl(EUCAWARN, "clean_network_state(): failed to unset metadata redirect\n");
  } else {
    //    logprintfl(EUCADEBUG, "clean_network_state(): sucessfully unset metadata redirect\n");
  }
  /*
  snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del 169.254.169.254/32 dev %s", config->eucahome, tmpvnetconfig->pubInterface);
  rc = system(cmd);
  rc = rc>>8;
  if (rc && (rc != 2)) {
    logprintfl(EUCAERROR, "clean_network_state(): running cmd '%s' failed: cannot remove ip 169.254.169.254\n", cmd);
  }
  */

  for (i=1; i<NUMBER_OF_PUBLIC_IPS; i++) {
    if (tmpvnetconfig->publicips[i].ip != 0 && tmpvnetconfig->publicips[i].allocated != 0) {
      ipstr = hex2dot(tmpvnetconfig->publicips[i].ip);
      snprintf(cmd, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap ip addr del %s/32 dev %s", config->eucahome, SP(ipstr), tmpvnetconfig->pubInterface);
      logprintfl(EUCADEBUG, "clean_network_state(): running command '%s'\n", cmd);
      rc = system(cmd);
      rc = rc>>8;
      if (rc && rc != 2) {
	logprintfl(EUCAERROR, "clean_network_state(): running cmd '%s' failed: cannot remove ip %s\n", cmd, SP(ipstr));
      }
      if (ipstr) free(ipstr);
    }
  }
  //  logprintfl(EUCADEBUG, "clean_network_state(): finished clearing public IPs\n");


  // dhcp
  snprintf(file, MAX_PATH, "%s/euca-dhcp.pid", tmpvnetconfig->path);
  snprintf(rootwrap, MAX_PATH, "%s/usr/lib/eucalyptus/euca_rootwrap", tmpvnetconfig->eucahome);
  if (!check_file(file)) {
    pidstr = file2str(file);
    if (pidstr) {
      rc = safekillfile(file, tmpvnetconfig->dhcpdaemon, 9, rootwrap);
      if (rc) {
	logprintfl(EUCAERROR, "clean_network_state(): could not terminate dhcpd (%s)\n", tmpvnetconfig->dhcpdaemon);
      }
      free(pidstr);
    }
  }
  //  logprintfl(EUCADEBUG, "clean_network_state(): finished clearing dhcpd\n");

  sem_mywait(VNET);
  // stop all running networks
  for (i=2; i<NUMBER_OF_VLANS; i++) {
    if (vnetconfig->networks[i].active) {
      rc = vnetStopNetwork(vnetconfig, i, vnetconfig->users[i].userName, vnetconfig->users[i].netName);
      if (rc) {
	logprintfl(EUCADEBUG, "clean_network_state(): failed to tear down network %d\n");
      }
    }
  }
  // clear stored cloudIP
  vnetconfig->cloudIp = 0;

  sem_mypost(VNET);
  //  logprintfl(EUCADEBUG, "clean_network_state(): finished stopping virtual networks\n");

  if (!strcmp(tmpvnetconfig->mode, "MANAGED") || !strcmp(tmpvnetconfig->mode, "MANAGED-NOVLAN")) {
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
    
    // ipt preload
    rc = vnetLoadIPTables(tmpvnetconfig);
    if (rc) {
    }
  }

  /*
  // tunnels
  rc = vnetSetCCS(tmpvnetconfig, NULL, 0);
  if (rc) {
  }
  */
  
  if (tmpvnetconfig) free(tmpvnetconfig);
  return(0);
}

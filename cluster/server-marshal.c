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
#include <pthread.h>

#include <server-marshal.h>
#include <handlers.h>
#include <misc.h>
#include <vnetwork.h>
#include "adb-helpers.h"
      
#define DONOTHING 0
#define EVENTLOG 0

void adb_InitService(void) {
  int rc;
  doInitCC();
}

adb_AttachVolumeResponse_t *AttachVolumeMarshal(adb_AttachVolume_t *attachVolume, const axutil_env_t *env) {
  adb_AttachVolumeResponse_t *ret=NULL;
  adb_attachVolumeResponseType_t *avrt=NULL;
  
  adb_attachVolumeType_t *avt=NULL;
  
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *volumeId=NULL, *instanceId=NULL, *remoteDev=NULL, *localDev=NULL;
  ncMetadata ccMeta;
  
  avt = adb_AttachVolume_get_AttachVolume(attachVolume, env);
  
  EUCA_MESSAGE_UNMARSHAL(attachVolumeType, avt, (&ccMeta));
  
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
  axis2_bool_t status=AXIS2_TRUE, forceBool=AXIS2_FALSE;
  char statusMessage[256];
  char *volumeId=NULL, *instanceId=NULL, *remoteDev=NULL, *localDev=NULL;
  int force;
  ncMetadata ccMeta;
  
  dvt = adb_DetachVolume_get_DetachVolume(detachVolume, env);
  
  EUCA_MESSAGE_UNMARSHAL(detachVolumeType, dvt, (&ccMeta));
  
  volumeId = adb_detachVolumeType_get_volumeId(dvt, env);
  instanceId = adb_detachVolumeType_get_instanceId(dvt, env);
  remoteDev = adb_detachVolumeType_get_remoteDev(dvt, env);
  localDev = adb_detachVolumeType_get_localDev(dvt, env);
  forceBool = adb_detachVolumeType_get_force(dvt, env);
  if (forceBool == AXIS2_TRUE) {
    force = 1;
  } else {
    force = 0;
  }

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

adb_BundleInstanceResponse_t *BundleInstanceMarshal(adb_BundleInstance_t *bundleInstance, const axutil_env_t *env) {
  adb_BundleInstanceResponse_t *ret=NULL;
  adb_bundleInstanceResponseType_t *birt=NULL;
  
  adb_bundleInstanceType_t *bit=NULL;
  
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *instanceId=NULL, *bucketName=NULL, *filePrefix=NULL, *walrusURL=NULL, *userPublicKey=NULL, *S3Policy=NULL, *S3PolicySig=NULL;
  ncMetadata ccMeta;
  
  bit = adb_BundleInstance_get_BundleInstance(bundleInstance, env);

  EUCA_MESSAGE_UNMARSHAL(bundleInstanceType, bit, (&ccMeta));
  
  instanceId = adb_bundleInstanceType_get_instanceId(bit, env);
  bucketName = adb_bundleInstanceType_get_bucketName(bit, env);
  filePrefix = adb_bundleInstanceType_get_filePrefix(bit, env);
  walrusURL = adb_bundleInstanceType_get_walrusURL(bit, env);
  userPublicKey = adb_bundleInstanceType_get_userPublicKey(bit, env);
  S3Policy = adb_bundleInstanceType_get_S3Policy(bit, env);
  S3PolicySig = adb_bundleInstanceType_get_S3PolicySig(bit, env);
  
  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doBundleInstance(&ccMeta, instanceId, bucketName, filePrefix, walrusURL, userPublicKey, S3Policy, S3PolicySig);
    if (rc) {
      logprintf("ERROR: doBundleInstance() returned FAIL\n");
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }
  
  birt = adb_bundleInstanceResponseType_create(env);
  adb_bundleInstanceResponseType_set_return(birt, env, status);
  if (status == AXIS2_FALSE) {
    adb_bundleInstanceResponseType_set_statusMessage(birt, env, statusMessage);
  }

  adb_bundleInstanceResponseType_set_correlationId(birt, env, ccMeta.correlationId);
  adb_bundleInstanceResponseType_set_userId(birt, env, ccMeta.userId);
  
  ret = adb_BundleInstanceResponse_create(env);
  adb_BundleInstanceResponse_set_BundleInstanceResponse(ret, env, birt);

  return(ret);
}

adb_CancelBundleTaskResponse_t *CancelBundleTaskMarshal(adb_CancelBundleTask_t *cancelBundleTask, const axutil_env_t *env) {
  adb_CancelBundleTaskResponse_t *ret=NULL;
  adb_cancelBundleTaskResponseType_t *birt=NULL;
  
  adb_cancelBundleTaskType_t *bit=NULL;
  
  int rc;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *instanceId=NULL, *bucketName=NULL, *filePrefix=NULL, *walrusURL=NULL, *userPublicKey=NULL;
  ncMetadata ccMeta;
  
  bit = adb_CancelBundleTask_get_CancelBundleTask(cancelBundleTask, env);

  EUCA_MESSAGE_UNMARSHAL(cancelBundleTaskType, bit, (&ccMeta));
  
  instanceId = adb_cancelBundleTaskType_get_instanceId(bit, env);
  
  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doCancelBundleTask(&ccMeta, instanceId);
    if (rc) {
      logprintf("ERROR: doCancelBundleTask() returned FAIL\n");
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }
  
  birt = adb_cancelBundleTaskResponseType_create(env);
  adb_cancelBundleTaskResponseType_set_return(birt, env, status);
  if (status == AXIS2_FALSE) {
    adb_cancelBundleTaskResponseType_set_statusMessage(birt, env, statusMessage);
  }

  adb_cancelBundleTaskResponseType_set_correlationId(birt, env, ccMeta.correlationId);
  adb_cancelBundleTaskResponseType_set_userId(birt, env, ccMeta.userId);
  
  ret = adb_CancelBundleTaskResponse_create(env);
  adb_CancelBundleTaskResponse_set_CancelBundleTaskResponse(ret, env, birt);

  return(ret);
}

adb_StopNetworkResponse_t *StopNetworkMarshal(adb_StopNetwork_t *stopNetwork, const axutil_env_t *env) {
  adb_StopNetworkResponse_t *ret=NULL;
  adb_stopNetworkResponseType_t *snrt=NULL;
  
  adb_stopNetworkType_t *snt=NULL;
  
  int rc, vlan;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  char *userName=NULL, *netName=NULL, *accountId=NULL;
  ncMetadata ccMeta;
  
  snt = adb_StopNetwork_get_StopNetwork(stopNetwork, env);
  
  EUCA_MESSAGE_UNMARSHAL(stopNetworkType, snt, (&ccMeta));
  
  vlan = adb_stopNetworkType_get_vlan(snt, env);
  netName = adb_stopNetworkType_get_netName(snt, env);
  accountId = adb_stopNetworkType_get_accountId(snt, env);
  if (!accountId) {
    accountId = ccMeta.userId;
  }

  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doStopNetwork(&ccMeta, accountId, netName, vlan);
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
  int rc, i, j;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];

  char **clusterControllers=NULL, *nameserver=NULL, *vnetSubnet=NULL, *vnetNetmask=NULL;
  int clusterControllersLen=0;
  ncMetadata ccMeta;
  vnetConfig *outvnetConfig=NULL;
  
  outvnetConfig = malloc(sizeof(vnetConfig));

  snt = adb_DescribeNetworks_get_DescribeNetworks(describeNetworks, env);
  EUCA_MESSAGE_UNMARSHAL(describeNetworksType, snt, (&ccMeta));
  
  nameserver = adb_describeNetworksType_get_nameserver(snt, env);
  
  clusterControllersLen = adb_describeNetworksType_sizeof_clusterControllers(snt, env);
  clusterControllers = malloc(sizeof(char *) * clusterControllersLen);
  for (i=0; i<clusterControllersLen; i++) {
    char *incc;
    incc = adb_describeNetworksType_get_clusterControllers_at(snt, env, i);
    clusterControllers[i] = host2ip(incc);
  }
  
  snrt = adb_describeNetworksResponseType_create(env);
  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doDescribeNetworks(&ccMeta, nameserver, clusterControllers, clusterControllersLen, outvnetConfig);
    if (rc) {
      logprintf("ERROR: doDescribeNetworks() returned fail %d\n", rc);
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    } else {
      
      if (!strcmp(outvnetConfig->mode, "MANAGED") || !strcmp(outvnetConfig->mode, "MANAGED-NOVLAN")) {
	adb_describeNetworksResponseType_set_useVlans(snrt, env, 1);
      } else {
	adb_describeNetworksResponseType_set_useVlans(snrt, env, 0);
      }
      adb_describeNetworksResponseType_set_mode(snrt, env, outvnetConfig->mode);	
      adb_describeNetworksResponseType_set_addrsPerNet(snrt, env, outvnetConfig->numaddrs);
      adb_describeNetworksResponseType_set_addrIndexMin(snrt, env, outvnetConfig->addrIndexMin);
      adb_describeNetworksResponseType_set_addrIndexMax(snrt, env, outvnetConfig->addrIndexMax);
      
      vnetSubnet = hex2dot(outvnetConfig->nw);
      if (vnetSubnet) {
	adb_describeNetworksResponseType_set_vnetSubnet(snrt, env, vnetSubnet);
	free(vnetSubnet);
      }

      vnetNetmask = hex2dot(outvnetConfig->nm);
      if (vnetNetmask) {
	adb_describeNetworksResponseType_set_vnetNetmask(snrt, env, vnetNetmask);
	free(vnetNetmask);
	}
      adb_describeNetworksResponseType_set_vlanMin(snrt, env, 2);
      adb_describeNetworksResponseType_set_vlanMax(snrt, env, outvnetConfig->max_vlan);
      
      for (i=2; i<NUMBER_OF_VLANS; i++) {
	if (outvnetConfig->networks[i].active) {
	  adb_networkType_t *nt=NULL;
	  nt = adb_networkType_create(env);
	  adb_networkType_set_uuid(nt, env, outvnetConfig->users[i].uuid);
	  adb_networkType_set_vlan(nt, env, i);
	  adb_networkType_set_netName(nt, env, outvnetConfig->users[i].netName);
	  adb_networkType_set_userName(nt, env, outvnetConfig->users[i].userName);
	  for (j=0; j<NUMBER_OF_HOSTS_PER_VLAN; j++) {
	    if (outvnetConfig->networks[i].addrs[j].active) {
	      adb_networkType_add_activeAddrs(nt, env, j);
	    }
	  }
	  adb_describeNetworksResponseType_add_activeNetworks(snrt, env, nt);
	}
      }
      
      status = AXIS2_TRUE;
    }
  }
  for (i=0; i<clusterControllersLen; i++) {
    if (clusterControllers[i]) free(clusterControllers[i]);
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
  
  if (outvnetConfig) free(outvnetConfig);
  return(ret);
}

adb_DescribePublicAddressesResponse_t *DescribePublicAddressesMarshal(adb_DescribePublicAddresses_t *describePublicAddresses, const axutil_env_t *env) {
  adb_describePublicAddressesType_t *dpa=NULL;

  adb_DescribePublicAddressesResponse_t *ret=NULL;
  adb_describePublicAddressesResponseType_t *dpart=NULL;

  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256], *ipstr=NULL;

  int rc, outAddressesLen, i;
  ncMetadata ccMeta;
  //  char **outAddresses=NULL;
  publicip *outAddresses;

  dpa = adb_DescribePublicAddresses_get_DescribePublicAddresses(describePublicAddresses, env);
  EUCA_MESSAGE_UNMARSHAL(describePublicAddressesType, dpa, (&ccMeta));

  if (!DONOTHING) {
    rc = doDescribePublicAddresses(&ccMeta, &outAddresses, &outAddressesLen);
  }
  
  if (rc == 2) {
    snprintf(statusMessage, 256, "NOTSUPPORTED");
    status = AXIS2_FALSE;
    outAddressesLen = 0;
  } else if (rc) {
    logprintf("ERROR: doDescribePublicAddresses() returned FAIL\n");
    snprintf(statusMessage, 256, "ERROR");
    status = AXIS2_FALSE;
    outAddressesLen = 0;
  } else {
    status = AXIS2_TRUE;
  }
  
  dpart = adb_describePublicAddressesResponseType_create(env);
  for (i=0; i<outAddressesLen; i++) {
    if (outAddresses[i].ip) {
      adb_publicAddressType_t *addr;
      addr = adb_publicAddressType_create(env);

      adb_publicAddressType_set_uuid(addr, env, outAddresses[i].uuid);

      ipstr = hex2dot(outAddresses[i].ip);
      adb_publicAddressType_set_sourceAddress(addr, env, ipstr);
      if (ipstr) free(ipstr);

      if (outAddresses[i].dstip) {
	ipstr = hex2dot(outAddresses[i].dstip);
	adb_publicAddressType_set_destAddress(addr, env, ipstr);
	if (ipstr) free(ipstr);
      } else {
	adb_publicAddressType_set_destAddress(addr, env, "0.0.0.0");
      }

      adb_describePublicAddressesResponseType_add_addresses(dpart, env, addr);
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
  char *src=NULL, *dst=NULL, *uuid=NULL;
  ncMetadata ccMeta;
  
  aat = adb_AssignAddress_get_AssignAddress(assignAddress, env);
  
  EUCA_MESSAGE_UNMARSHAL(assignAddressType, aat, (&ccMeta));
  
  src = adb_assignAddressType_get_source(aat, env);
  dst = adb_assignAddressType_get_dest(aat, env);
  uuid = adb_assignAddressType_get_uuid(aat, env);

  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doAssignAddress(&ccMeta, uuid, src, dst);
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
  EUCA_MESSAGE_UNMARSHAL(unassignAddressType, uat, (&ccMeta));
  
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

  char **sourceNets=NULL, **userNames=NULL, **sourceNames=NULL, *protocol=NULL,  *destName=NULL, *type=NULL, *destNameLast=NULL, *destUserName=NULL, *accountId=NULL;
  int minPort, maxPort, namedLen, netLen;
  ncMetadata ccMeta;
  
  cnt = adb_ConfigureNetwork_get_ConfigureNetwork(configureNetwork, env);
  EUCA_MESSAGE_UNMARSHAL(configureNetworkType, cnt, (&ccMeta));

  accountId = adb_configureNetworkType_get_accountId(cnt, env);
  if (!accountId) {
    accountId = ccMeta.userId;
  }

  ruleLen = adb_configureNetworkType_sizeof_rules(cnt, env);
  done=0;
  destNameLast = strdup("EUCAFIRST");
  if (!destNameLast) {
    logprintf("ERROR: out of memory\n");
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
    return ret;
  }
  for (j=0; j<ruleLen && !done; j++) {
    nr = adb_configureNetworkType_get_rules_at(cnt, env, j);
    
    type = adb_networkRule_get_type(nr, env);
    destName = adb_networkRule_get_destName(nr, env);
    destUserName = adb_networkRule_get_destUserName(nr, env);
    protocol = adb_networkRule_get_protocol(nr, env);
    minPort = adb_networkRule_get_portRangeMin(nr, env);
    maxPort = adb_networkRule_get_portRangeMax(nr, env);
  
    if (strcmp(destName, destNameLast)) {
      doFlushNetwork(&ccMeta, accountId, destName);
    }
    if (destNameLast) free(destNameLast);
    destNameLast = strdup(destName);
    if (!destNameLast) {
      logprintf("ERROR: out of memory\n");
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
      return ret;
    }

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
      rc = doConfigureNetwork(&ccMeta, accountId, type, namedLen, sourceNames, userNames, netLen, sourceNets, destName, destUserName, protocol, minPort, maxPort);
    }
    
    if (userNames) free(userNames);
    if (sourceNames) free(sourceNames);
    if (sourceNets) free(sourceNets);
    
    if (rc) {
      done++;
    }
  }
  if (destNameLast) free(destNameLast);
  
  if (done) {
    logprintf("ERROR: doConfigureNetwork() returned fail %d\n", rc);
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    status = AXIS2_TRUE;
  }
  
  adb_configureNetworkResponseType_set_correlationId(cnrt, env, ccMeta.correlationId);
  adb_configureNetworkResponseType_set_userId(cnrt, env, ccMeta.userId);
  adb_configureNetworkResponseType_set_return(cnrt, env, status);
  if (status == AXIS2_FALSE) {
    adb_configureNetworkResponseType_set_statusMessage(cnrt, env, statusMessage);
  }
  
  ret = adb_ConfigureNetworkResponse_create(env);
  adb_ConfigureNetworkResponse_set_ConfigureNetworkResponse(ret, env, cnrt);
  
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
  EUCA_MESSAGE_UNMARSHAL(getConsoleOutputType, gcot, (&ccMeta));
  
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

  char *netName=NULL, **clusterControllers=NULL, *nameserver=NULL, *uuid=NULL, *accountId=NULL;
  
  int vlan, clusterControllersLen=0;
  ncMetadata ccMeta;
  
  snt = adb_StartNetwork_get_StartNetwork(startNetwork, env);
  EUCA_MESSAGE_UNMARSHAL(startNetworkType, snt, (&ccMeta));
  
  vlan = adb_startNetworkType_get_vlan(snt, env);
  netName = adb_startNetworkType_get_netName(snt, env);
  nameserver = adb_startNetworkType_get_nameserver(snt, env);
  uuid = adb_startNetworkType_get_uuid(snt, env);
  accountId = adb_startNetworkType_get_accountId(snt, env);
  if (!accountId) {
    accountId = ccMeta.userId;
  }

  clusterControllersLen = adb_startNetworkType_sizeof_clusterControllers(snt, env);
  clusterControllers = malloc(sizeof(char *) * clusterControllersLen);
  for (i=0; i<clusterControllersLen; i++) {
    clusterControllers[i] = host2ip(adb_startNetworkType_get_clusterControllers_at(snt, env, i));
  }
    
  snrt = adb_startNetworkResponseType_create(env);
  status = AXIS2_TRUE;
  if (!DONOTHING) {
    rc = doStartNetwork(&ccMeta, accountId, uuid, netName, vlan, nameserver, clusterControllers, clusterControllersLen);
    if (rc) {
      logprintf("ERROR: doStartNetwork() returned fail %d\n", rc);
      status = AXIS2_FALSE;
      snprintf(statusMessage, 255, "ERROR");
    }
  }

  for (i=0; i<clusterControllersLen; i++) {
    if (clusterControllers[i]) free(clusterControllers[i]);
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
  int vmLen=0, outTypesLen=0;
  ccResource *outNodes=NULL;
  int outNodesLen=0;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  virtualMachine *vms=NULL;
  adb_virtualMachineType_t *vm=NULL;
  ncMetadata ccMeta;
  adb_serviceInfoType_t *sit=NULL;
  int servicesLen=0, urisLen=0, j;

  drt = adb_DescribeResources_get_DescribeResources(describeResources, env);

  EUCA_MESSAGE_UNMARSHAL(describeResourcesType, drt, (&ccMeta));
  
  vmLen = adb_describeResourcesType_sizeof_instanceTypes(drt, env);
  vms = malloc(sizeof(virtualMachine) * vmLen);

  for (i=0; i<vmLen; i++) {
    char *name;
    vm = adb_describeResourcesType_get_instanceTypes_at(drt, env, i);
    copy_vm_type_from_adb (&(vms[i]), vm, env);
  }

  // do it
  drrt = adb_describeResourcesResponseType_create(env);
  
  rc=1;
  if (!DONOTHING) {
    rc = doDescribeResources(&ccMeta, &vms, vmLen, &outTypesMax, &outTypesAvail, &outTypesLen, &outNodes, &outNodesLen);
  }
  
  if (rc) {
    logprintfl(ERROR, "ERROR: doDescribeResources() failed %d\n", rc);
    status = AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    for (i=0; i<outNodesLen; i++) {
      //      adb_describeResourcesResponseType_add_serviceTags(drrt, env, outNodes[i].ncURL);
      adb_ccNodeType_t *nt=NULL;
      
      nt = adb_ccNodeType_create(env);
      adb_ccNodeType_set_serviceTag(nt, env, outNodes[i].ncURL);
      adb_ccNodeType_set_iqn(nt, env, outNodes[i].iqn);
      adb_describeResourcesResponseType_add_nodes(drrt, env, nt);
      
    }
    if (outNodes) free(outNodes);

    for (i=0; i<outTypesLen; i++) {
      adb_ccResourceType_t *rt=NULL;
  
      vm = copy_vm_type_to_adb (env, &(vms[i]));

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
  EUCA_MESSAGE_UNMARSHAL(describeInstancesType, dit, (&ccMeta));

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
  
      //      myInstance->ccvm.virtualBootRecordLen = 0;
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
  adb_ccInstanceType_set_uuid(dst, env, src->uuid);
  adb_ccInstanceType_set_reservationId(dst, env, src->reservationId);
  adb_ccInstanceType_set_ownerId(dst, env, src->ownerId);
  adb_ccInstanceType_set_accountId(dst, env, src->accountId);
  adb_ccInstanceType_set_imageId(dst, env, src->amiId);
  adb_ccInstanceType_set_kernelId(dst, env, src->kernelId);
  adb_ccInstanceType_set_ramdiskId(dst, env, src->ramdiskId);
  
  adb_ccInstanceType_set_keyName(dst, env, src->keyName);
  adb_ccInstanceType_set_stateName(dst, env, src->state);
  
  adb_ccInstanceType_set_launchTime(dst, env, dt);     
  
  adb_ccInstanceType_set_serviceTag(dst, env, src->serviceTag);
  adb_ccInstanceType_set_userData(dst, env, src->userData);
  adb_ccInstanceType_set_launchIndex(dst, env, src->launchIndex);
  if (strlen(src->platform)) {
    adb_ccInstanceType_set_platform(dst, env, src->platform);
  }
  if (strlen(src->bundleTaskStateName)) {
    adb_ccInstanceType_set_bundleTaskStateName(dst, env, src->bundleTaskStateName);
  }

  adb_ccInstanceType_set_blkbytes(dst, env, src->blkbytes);
  adb_ccInstanceType_set_netbytes(dst, env, src->netbytes);

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
  adb_netConfigType_set_privateIp(netconf, env, src->ccnet.privateIp);
  adb_netConfigType_set_publicIp(netconf, env, src->ccnet.publicIp);
  adb_netConfigType_set_vlan(netconf, env, src->ccnet.vlan);
  adb_netConfigType_set_networkIndex(netconf, env, src->ccnet.networkIndex);
  adb_ccInstanceType_set_netParams(dst, env, netconf);
  
  vm = copy_vm_type_to_adb (env, &(src->ccvm));
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

  ccInstance *outInsts=NULL, *myInstance=NULL;
  int minCount, maxCount, rc, outInstsLen, i, vlan, instIdsLen, netNamesLen, macAddrsLen, *networkIndexList=NULL, networkIndexListLen, uuidsLen, expiryTime;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];

  char *emiId=NULL, *keyName=NULL, **instIds=NULL, *reservationId=NULL, **netNames=NULL, **macAddrs=NULL, *kernelId=NULL, *ramdiskId=NULL, *emiURL=NULL, *kernelURL=NULL, *ramdiskURL=NULL, *vmName=NULL, *userData=NULL, *launchIndex=NULL, *platform=NULL, *tmp=NULL, **uuids=NULL, *accountId=NULL, *ownerId=NULL;
  ncMetadata ccMeta;
  
  virtualMachine ccvm;

  axutil_date_time_t *dt=NULL;
  
  rit = adb_RunInstances_get_RunInstances(runInstances, env);
  EUCA_MESSAGE_UNMARSHAL(runInstancesType, rit, (&ccMeta));

  reservationId = adb_runInstancesType_get_reservationId(rit, env);

  maxCount = adb_runInstancesType_get_maxCount(rit, env);
  minCount = adb_runInstancesType_get_minCount(rit, env);
  keyName = adb_runInstancesType_get_keyName(rit, env);

  emiId = adb_runInstancesType_get_imageId(rit, env);
  kernelId = adb_runInstancesType_get_kernelId(rit, env);
  ramdiskId = adb_runInstancesType_get_ramdiskId(rit, env);

  emiURL = adb_runInstancesType_get_imageURL(rit, env);
  kernelURL = adb_runInstancesType_get_kernelURL(rit, env);
  ramdiskURL = adb_runInstancesType_get_ramdiskURL(rit, env);

  tmp = adb_runInstancesType_get_userData(rit, env);
  if (!tmp) {
    userData = strdup("");
  } else {
    userData = strdup(tmp);
  }

  launchIndex = adb_runInstancesType_get_launchIndex(rit, env);
  platform = adb_runInstancesType_get_platform(rit, env);

  dt = adb_runInstancesType_get_expiryTime(rit, env);
  expiryTime = datetime_to_unix(dt, env);
  
  vm = adb_runInstancesType_get_instanceType(rit, env);
  copy_vm_type_from_adb (&ccvm, vm, env);
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
  if (netNamesLen > 1) {
     netNamesLen = 1;
  }
  for (i=0; i<netNamesLen; i++) {
    netNames[i] = adb_runInstancesType_get_netNames_at(rit, env, i);
  }

  macAddrsLen = adb_runInstancesType_sizeof_macAddresses(rit, env);
  macAddrs = malloc(sizeof(char *) * macAddrsLen);  
  for (i=0; i<macAddrsLen; i++) {
    macAddrs[i] = adb_runInstancesType_get_macAddresses_at(rit, env, i);
  }

  uuidsLen = adb_runInstancesType_sizeof_uuids(rit, env);
  uuids = malloc(sizeof(char *) * uuidsLen);  
  for (i=0; i<uuidsLen; i++) {
    uuids[i] = adb_runInstancesType_get_uuids_at(rit, env, i);
  }

  networkIndexList = NULL;
  networkIndexListLen = adb_runInstancesType_sizeof_networkIndexList(rit, env);
  if (networkIndexListLen) {
    networkIndexList = malloc(sizeof(int) * networkIndexListLen);
    for (i=0; i<networkIndexListLen; i++) {
      networkIndexList[i] = adb_runInstancesType_get_networkIndexList_at(rit, env, i);
    }
  }

  accountId = adb_runInstancesType_get_accountId(rit, env);
  if (!accountId) {
    accountId = ccMeta.userId;
  }
  ownerId = adb_runInstancesType_get_ownerId(rit, env);
  if (!ownerId) {
    ownerId = accountId;
  }

  rirt = adb_runInstancesResponseType_create(env);
  rc=1;
  if (!DONOTHING) {
    rc = doRunInstances(&ccMeta, emiId, kernelId, ramdiskId, emiURL, kernelURL,ramdiskURL, instIds, instIdsLen, netNames, netNamesLen, macAddrs, macAddrsLen, networkIndexList, networkIndexListLen, uuids, uuidsLen, minCount, maxCount, accountId, ownerId, reservationId, &ccvm, keyName, vlan, userData, launchIndex, platform, expiryTime, NULL, &outInsts, &outInstsLen);
  }
  
  if (rc) {
    logprintf("ERROR: doRunInstances() failed %d\n", rc);
    status=AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    for (i=0; i<outInstsLen; i++) {
      myInstance = &(outInsts[i]);
      
      it = adb_ccInstanceType_create(env);
      
      myInstance->ccvm.virtualBootRecordLen = 0;
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
  free(networkIndexList);
  free(macAddrs);
  free(netNames);
  free(instIds);
  free(userData);
  if(uuids != NULL)
     free(uuids);

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
  EUCA_MESSAGE_UNMARSHAL(rebootInstancesType, rit, (&ccMeta));
  
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
  int instIdsLen, i, rc, *outStatus=NULL, force=0;
  axis2_bool_t status=AXIS2_TRUE, forceBool=AXIS2_FALSE;
  char statusMessage[256];

  ncMetadata ccMeta;
  
  tit = adb_TerminateInstances_get_TerminateInstances(terminateInstances, env);
  EUCA_MESSAGE_UNMARSHAL(terminateInstancesType, tit, (&ccMeta));
  
  instIdsLen = adb_terminateInstancesType_sizeof_instanceIds(tit, env);
  instIds = malloc(sizeof(char *) * instIdsLen);  
  for (i=0; i<instIdsLen; i++) {
    instIds[i] = adb_terminateInstancesType_get_instanceIds_at(tit, env, i);
  }
  forceBool = adb_terminateInstancesType_get_force(tit, env);
  if (forceBool == AXIS2_TRUE) {
    force = 1;
  } else {
    force = 0;
  }

  rc=1;
  if (!DONOTHING) {
    outStatus = malloc(sizeof(int) * instIdsLen);
    rc = doTerminateInstances(&ccMeta, instIds, instIdsLen, force, &outStatus);
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
  
  return(ret);
}

adb_CreateImageResponse_t *CreateImageMarshal(adb_CreateImage_t *createImage, const axutil_env_t *env) {
  int rc;
  adb_CreateImageResponse_t *ret=NULL;
  adb_createImageResponseType_t *cirt=NULL;

  // input vars
  adb_createImageType_t *cit=NULL;
  
  // working vars
  char *instanceId=NULL, *volumeId=NULL, *remoteDev=NULL;
  axis2_bool_t status=AXIS2_TRUE;
  char statusMessage[256];
  ncMetadata ccMeta;

  cit = adb_CreateImage_get_CreateImage(createImage, env);

  EUCA_MESSAGE_UNMARSHAL(createImageType, cit, (&ccMeta));
  
  instanceId = adb_createImageType_get_instanceId(cit, env);
  volumeId = adb_createImageType_get_volumeId(cit, env);
  remoteDev = adb_createImageType_get_remoteDev(cit, env);
  
  if (!DONOTHING) {
    rc = doCreateImage(&ccMeta, instanceId, volumeId, remoteDev);
  }
  
  cirt = adb_createImageResponseType_create(env);
  if (rc) {
    logprintf("ERROR: doCreateImage() failed %d\n", rc);
    status=AXIS2_FALSE;
    snprintf(statusMessage, 255, "ERROR");
  } else {
    status=AXIS2_TRUE;
  }

  adb_createImageResponseType_set_correlationId(cirt, env, ccMeta.correlationId);
  adb_createImageResponseType_set_userId(cirt, env, ccMeta.userId);

  adb_createImageResponseType_set_return(cirt, env, status);
  if (status == AXIS2_FALSE) {
    adb_createImageResponseType_set_statusMessage(cirt, env, statusMessage);
  }
  
  ret = adb_CreateImageResponse_create(env);
  adb_CreateImageResponse_set_CreateImageResponse(ret, env, cirt);
  
  return(ret);
}

void print_adb_ccInstanceType(adb_ccInstanceType_t *in) {
  
}

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
#include <time.h>
#include <misc.h>
#include <data.h>
#include <cc-client-marshal.h>
#include "axis2_stub_EucalyptusCC.h"
#include <euca_auth.h>
#include <sys/types.h>
#include <unistd.h>
#include "adb-helpers.h"

extern ncMetadata mymeta;

int cc_killallInstances(axutil_env_t *env, axis2_stub_t *stub) {
  int rc, instIdsLen;
  char *instIds[256];
  adb_ccInstanceType_t *it;
  axis2_char_t *instId=NULL;
  int i;
  axis2_bool_t status;

  adb_DescribeInstances_t *diIn=NULL;
  adb_describeInstancesType_t *dit=NULL;

  adb_DescribeInstancesResponse_t *diOut=NULL;
  adb_describeInstancesResponseType_t *dirt=NULL;
  
  bzero(instIds, 256 * sizeof(instIds[0]));
  //  adb_netConfigType_t *nct=NULL;
  //  adb_virtualMachineType_t *vm=NULL;

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
  int i;
  axis2_bool_t status;
  char *output;
  
  printf("%s\n", instId);

  tit = adb_getConsoleOutputType_create(env);
  adb_getConsoleOutputType_set_instanceId(tit, env, instId);
  
  EUCA_MESSAGE_MARSHAL(getConsoleOutputType, tit, (&mymeta));
  
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
      char *tmp;

      output = adb_getConsoleOutputResponseType_get_consoleOutput(tirt, env);
      printf("RAW CONSOLE OUTPUT: %s\n", output);
      tmp = base64_dec((unsigned char *)output, strlen(output));
      if (tmp) {
         printf("RAW CONSOLE OUTPUT: %s\n", tmp);
         free(tmp);
      } else {
         printf("Out of memory!\n");
      }
    }
  }
  return(0);
}

int cc_rebootInstances(char **instIds, int instIdsLen, axutil_env_t *env, axis2_stub_t *stub) {
  adb_RebootInstances_t *tiIn;
  adb_rebootInstancesType_t *tit;

  adb_RebootInstancesResponse_t *tiOut;
  adb_rebootInstancesResponseType_t *tirt;
  int i;
  axis2_bool_t status;

  printf("%d %s\n", instIdsLen, instIds[0]);

  tit = adb_rebootInstancesType_create(env);
  for (i=0; i<instIdsLen; i++) {
    adb_rebootInstancesType_add_instanceIds(tit, env, instIds[i]);
  }

  EUCA_MESSAGE_MARSHAL(rebootInstancesType, tit, (&mymeta));
  
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
  EUCA_MESSAGE_MARSHAL(terminateInstancesType, tit, (&mymeta));
  /*
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
  adb_terminateInstancesType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_terminateInstancesType_add_services(drt, env, sit);
  */

  tiIn = adb_TerminateInstances_create(env);
  adb_TerminateInstances_set_TerminateInstances(tiIn, env, tit);
  
  tiOut = axis2_stub_op_EucalyptusCC_TerminateInstances(stub, env, tiIn);
  if (!tiOut) {
    printf("ERROR: TI failed NULL\n");
    return(1);
  } else {


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
  adb_ConfigureNetwork_t *input;
  adb_ConfigureNetworkResponse_t *output;
  adb_configureNetworkType_t *cn;
  adb_configureNetworkResponseType_t *cnrt;
  
  adb_networkRule_t *nr=NULL;

  cn = adb_configureNetworkType_create(env);
  input = adb_ConfigureNetwork_create(env);

  EUCA_MESSAGE_MARSHAL(configureNetworkType, cn, (&mymeta));  

  /*  adb_configureNetworkType_set_userId(cn, env, "admin");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_configureNetworkType_set_correlationId(cn, env, cidstr);
  }
  adb_configureNetworkType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_configureNetworkType_add_services(drt, env, sit);
  */
  
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
  adb_StopNetwork_t *input;
  adb_StopNetworkResponse_t *output;
  adb_stopNetworkType_t *sn;
  adb_stopNetworkResponseType_t *snrt;

  sn = adb_stopNetworkType_create(env);
  input = adb_StopNetwork_create(env);

  EUCA_MESSAGE_MARSHAL(stopNetworkType, sn, (&mymeta));
  /*
  adb_stopNetworkType_set_userId(sn, env, "admin");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_stopNetworkType_set_correlationId(sn, env, cidstr);
  }
  adb_stopNetworkType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_stopNetworkType_add_services(drt, env, sit);
  */
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

int cc_attachVolume(char *volumeId, char *instanceId, char *remoteDev, char *localDev, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  //  char meh[32];
  adb_AttachVolume_t *input;
  adb_AttachVolumeResponse_t *output;
  adb_attachVolumeType_t *sn;
  adb_attachVolumeResponseType_t *snrt;

  sn = adb_attachVolumeType_create(env);
  input = adb_AttachVolume_create(env);
  
  EUCA_MESSAGE_MARSHAL(attachVolumeType, sn, (&mymeta));
  /*
  adb_attachVolumeType_set_userId(sn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_attachVolumeType_set_correlationId(sn, env, cidstr);
  }
  adb_attachVolumeType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_attachVolumeType_add_services(drt, env, sit);
  */

  adb_attachVolumeType_set_instanceId(sn, env, instanceId);
  adb_attachVolumeType_set_volumeId(sn, env, volumeId);
  adb_attachVolumeType_set_remoteDev(sn, env, remoteDev);
  adb_attachVolumeType_set_localDev(sn, env, localDev);
  
  adb_AttachVolume_set_AttachVolume(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_AttachVolume(stub, env, input);
  if (!output) {
    printf("ERROR: attachVolume returned NULL\n");
    return(1);
  }
  snrt = adb_AttachVolumeResponse_get_AttachVolumeResponse(output, env);
  printf("attachVolume returned status %d\n", adb_attachVolumeResponseType_get_return(snrt, env));
  return(0);
}

int cc_detachVolume(char *volumeId, char *instanceId, char *remoteDev, char *localDev, int force, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  //  char meh[32];
  adb_DetachVolume_t *input;
  adb_DetachVolumeResponse_t *output;
  adb_detachVolumeType_t *sn;
  adb_detachVolumeResponseType_t *snrt;

  sn = adb_detachVolumeType_create(env);
  input = adb_DetachVolume_create(env);
  
  EUCA_MESSAGE_MARSHAL(detachVolumeType, sn, (&mymeta));
  /*
  adb_detachVolumeType_set_userId(sn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_detachVolumeType_set_correlationId(sn, env, cidstr);
  }
  adb_detachVolumeType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_detachVolumeType_add_services(drt, env, sit);
  */

  adb_detachVolumeType_set_instanceId(sn, env, instanceId);
  adb_detachVolumeType_set_volumeId(sn, env, volumeId);
  adb_detachVolumeType_set_remoteDev(sn, env, remoteDev);
  adb_detachVolumeType_set_localDev(sn, env, localDev);
  adb_detachVolumeType_set_force(sn, env, force);
  
  adb_DetachVolume_set_DetachVolume(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_DetachVolume(stub, env, input);
  if (!output) {
    printf("ERROR: detachVolume returned NULL\n");
    return(1);
  }
  snrt = adb_DetachVolumeResponse_get_DetachVolumeResponse(output, env);
  printf("detachVolume returned status %d\n", adb_detachVolumeResponseType_get_return(snrt, env));
  return(0);
}

int cc_createImage(char *volumeId, char *instanceId, char *remoteDev, axutil_env_t *env, axis2_stub_t *stub) {
  adb_CreateImage_t *input;
  adb_CreateImageResponse_t *output;
  adb_createImageType_t *sn;
  adb_createImageResponseType_t *snrt;

  sn = adb_createImageType_create(env);
  input = adb_CreateImage_create(env);
  
  EUCA_MESSAGE_MARSHAL(createImageType, sn, (&mymeta));

  adb_createImageType_set_instanceId(sn, env, instanceId);
  adb_createImageType_set_volumeId(sn, env, volumeId);
  adb_createImageType_set_remoteDev(sn, env, remoteDev);
  
  adb_CreateImage_set_CreateImage(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_CreateImage(stub, env, input);
  if (!output) {
    printf("ERROR: createImage returned NULL\n");
    return(1);
  }

  snrt = adb_CreateImageResponse_get_CreateImageResponse(output, env);
  printf("createImage returned status %d\n", adb_createImageResponseType_get_return(snrt, env));

  return(0);
}

int cc_bundleInstance(char *instanceId, char *bucketName, char *filePrefix, char *walrusURL, char *userPublicKey, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  //  char meh[32];
  adb_BundleInstance_t *input;
  adb_BundleInstanceResponse_t *output;
  adb_bundleInstanceType_t *sn;
  adb_bundleInstanceResponseType_t *snrt;

  sn = adb_bundleInstanceType_create(env);
  input = adb_BundleInstance_create(env);
  
  adb_bundleInstanceType_set_userId(sn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_bundleInstanceType_set_correlationId(sn, env, cidstr);
  }
  adb_bundleInstanceType_set_instanceId(sn, env, instanceId);
  adb_bundleInstanceType_set_bucketName(sn, env, bucketName);
  adb_bundleInstanceType_set_filePrefix(sn, env, filePrefix);
  adb_bundleInstanceType_set_walrusURL(sn, env, walrusURL);
  adb_bundleInstanceType_set_userPublicKey(sn, env, userPublicKey);
  
  adb_BundleInstance_set_BundleInstance(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_BundleInstance(stub, env, input);
  if (!output) {
    printf("ERROR: bundleInstance returned NULL\n");
    return(1);
  }

  snrt = adb_BundleInstanceResponse_get_BundleInstanceResponse(output, env);
  printf("bundleInstance returned status %d\n", adb_bundleInstanceResponseType_get_return(snrt, env));

  snrt = adb_BundleInstanceResponse_get_BundleInstanceResponse(output, env);
  printf("bundleInstance returned status %d\n", adb_bundleInstanceResponseType_get_return(snrt, env));

  return(0);
}

int cc_assignAddress(char *src, char *dst, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  //  char meh[32];
  adb_AssignAddress_t *input;
  adb_AssignAddressResponse_t *output;
  adb_assignAddressType_t *sn;
  adb_assignAddressResponseType_t *snrt;

  sn = adb_assignAddressType_create(env);
  input = adb_AssignAddress_create(env);
  
  EUCA_MESSAGE_MARSHAL(assignAddressType, sn, (&mymeta));
  /*
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
  adb_assignAddressType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_assignAddressType_add_services(drt, env, sit);
  */
  adb_assignAddressType_set_source(sn, env, src);
  adb_assignAddressType_set_dest(sn, env, dst);
  adb_assignAddressType_set_uuid(sn, env, "the-uuid");

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
  //  char meh[32];
  adb_UnassignAddress_t *input;
  adb_UnassignAddressResponse_t *output;
  adb_unassignAddressType_t *sn;
  adb_unassignAddressResponseType_t *snrt;
  
  sn = adb_unassignAddressType_create(env);
  input = adb_UnassignAddress_create(env);
  
  EUCA_MESSAGE_MARSHAL(unassignAddressType, sn, (&mymeta));
  /*
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
  adb_unassignAddressType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_unassignAddressType_add_services(drt, env, sit);
  */
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
  //  char meh[32];
  adb_DescribePublicAddresses_t *input;
  adb_DescribePublicAddressesResponse_t *output;
  adb_describePublicAddressesType_t *sn;
  adb_describePublicAddressesResponseType_t *snrt;
  
  sn = adb_describePublicAddressesType_create(env);
  input = adb_DescribePublicAddresses_create(env);
  
  EUCA_MESSAGE_MARSHAL(describePublicAddressesType, sn, (&mymeta));
  /*
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
  adb_describePublicAddressesType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_describePublicAddressesType_add_services(drt, env, sit);
  */

  adb_DescribePublicAddresses_set_DescribePublicAddresses(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_DescribePublicAddresses(stub, env, input);
  if (!output) {
    printf("ERROR: describePublicAddresses returned NULL\n");
    return(1);
  }
  snrt = adb_DescribePublicAddressesResponse_get_DescribePublicAddressesResponse(output, env);
  len = adb_describePublicAddressesResponseType_sizeof_addresses(snrt, env);
  for (i=0; i<len; i++) {
    char *ip;
    char *dstip;
    char *uuid;
    adb_publicAddressType_t *addr;
    addr = adb_describePublicAddressesResponseType_get_addresses_at(snrt, env, i);
    ip = adb_publicAddressType_get_sourceAddress(addr, env);
    dstip = adb_publicAddressType_get_destAddress(addr, env);
    uuid = adb_publicAddressType_get_uuid(addr, env);

    printf("UUID: %s IP: %s ALLOC: %s\n", uuid, ip, dstip);
  }
  // len = ...for (i=0....
  //  printf("descibePublicAddresses returned status %d\n", adb_describePublicAddressesResponseType_get_networkStatus(snrt, env));
  return(0);
}

int cc_startNetwork(int vlan, char *netName, char **ccs, int ccsLen, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  //  char meh[32];
  adb_StartNetwork_t *input;
  adb_StartNetworkResponse_t *output;
  adb_startNetworkType_t *sn;
  adb_startNetworkResponseType_t *snrt;

  sn = adb_startNetworkType_create(env);
  input = adb_StartNetwork_create(env);
  
  EUCA_MESSAGE_MARSHAL(startNetworkType, sn, (&mymeta));
  /*
  adb_startNetworkType_set_userId(sn, env, "admin");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_startNetworkType_set_correlationId(sn, env, cidstr);
  }
  adb_startNetworkType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_startNetworkType_add_services(drt, env, sit);
  */

  adb_startNetworkType_set_vlan(sn, env, vlan);
  adb_startNetworkType_set_netName(sn, env, netName);
  adb_startNetworkType_set_uuid(sn, env, "the-uuid");
  
  for (i=0; i<ccsLen; i++) {
    printf("adding %s\n", ccs[i]);
    adb_startNetworkType_add_clusterControllers(sn, env, ccs[i]);
  }

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

int cc_describeNetworks(char *nameserver, char **ccs, int ccsLen, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  //  char meh[32];
  adb_DescribeNetworks_t *input;
  adb_DescribeNetworksResponse_t *output;
  adb_describeNetworksType_t *sn;
  adb_describeNetworksResponseType_t *snrt;

  sn = adb_describeNetworksType_create(env);
  input = adb_DescribeNetworks_create(env);
  
  if (nameserver) {
    adb_describeNetworksType_set_nameserver(sn, env, nameserver);
  }

  EUCA_MESSAGE_MARSHAL(describeNetworksType, sn, (&mymeta));
  /*
  adb_describeNetworksType_set_userId(sn, env, "eucalyptus");
  {
    char cidstr[9];
    bzero(cidstr, 9);
    srand(time(NULL)+getpid());
    for (i=0; i<8; i++) {
      cidstr[i] = rand()%26+'a';
    }
    adb_describeNetworksType_set_correlationId(sn, env, cidstr);
  }
  adb_describeNetworksType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_describeNetworksType_add_services(drt, env, sit);
  */

  
  for (i=0; i<ccsLen; i++) {
    printf("adding %s\n", ccs[i]);
    adb_describeNetworksType_add_clusterControllers(sn, env, ccs[i]);
  }

  adb_DescribeNetworks_set_DescribeNetworks(input, env, sn);

  output = axis2_stub_op_EucalyptusCC_DescribeNetworks(stub, env, input);
  if (!output) {
    printf("ERROR: describeNetworks returned NULL\n");
    return(1);
  }
  snrt = adb_DescribeNetworksResponse_get_DescribeNetworksResponse(output, env);
  printf("describenetworks returned status %d\n", adb_describeNetworksResponseType_get_return(snrt, env));

  printf("useVlans: %d mode: %s addrspernet: %d addrIndexMin: %d addrIndexMax: %d vlanMin: %d vlanMax: %d\n", adb_describeNetworksResponseType_get_useVlans(snrt, env), adb_describeNetworksResponseType_get_mode(snrt, env), adb_describeNetworksResponseType_get_addrsPerNet(snrt, env), adb_describeNetworksResponseType_get_addrIndexMin(snrt, env), adb_describeNetworksResponseType_get_addrIndexMax(snrt, env), adb_describeNetworksResponseType_get_vlanMin(snrt, env), adb_describeNetworksResponseType_get_vlanMax(snrt, env));
  {
    int i, numnets, numaddrs, j;
    numnets = adb_describeNetworksResponseType_sizeof_activeNetworks(snrt, env);
    printf("found %d active nets\n", numnets);
    for (i=0; i<numnets; i++) {
      adb_networkType_t *nt;
      nt = adb_describeNetworksResponseType_get_activeNetworks_at(snrt, env, i);
      printf("\tvlan: %d uuid: %s nnetName: %s userName: %s\n", adb_networkType_get_vlan(nt, env), adb_networkType_get_uuid(nt, env), adb_networkType_get_netName(nt, env), adb_networkType_get_userName(nt, env));
      numaddrs = adb_networkType_sizeof_activeAddrs(nt, env);
      printf("\tnumber of active addrs: %d - ", numaddrs);
      for (j=0; j<numaddrs; j++) {
	printf("%d ", adb_networkType_get_activeAddrs_at(nt, env, j));
      }
      printf("\n");
    }
  }
  return(0);
}

int cc_describeResources(axutil_env_t *env, axis2_stub_t *stub) {
  adb_DescribeResourcesResponse_t *drOut=NULL;
  adb_describeResourcesResponseType_t *drrt=NULL;
  adb_ccResourceType_t *rt=NULL;
  adb_ccNodeType_t *nt=NULL;

  adb_DescribeResources_t *drIn=NULL;
  adb_describeResourcesType_t *drt=NULL;

  adb_virtualMachineType_t *vm;
  int i;
  axis2_bool_t status;
  adb_serviceInfoType_t *sit;

  drt = adb_describeResourcesType_create(env);
  //  adb_describeResourcesType_add_instanceTypes(drt, env, "1");
  //  adb_describeResourcesType_add_instanceTypes(drt, env, "2");
  
  EUCA_MESSAGE_MARSHAL(describeResourcesType, drt, (&mymeta));

  /*
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
  adb_describeResourcesType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_describeResourcesType_add_services(drt, env, sit);
  */

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
    for (i=0; i<adb_describeResourcesResponseType_sizeof_nodes(drrt, env); i++) {
      nt = adb_describeResourcesResponseType_get_nodes_at(drrt, env, i);
      printf(":%s,%s:", adb_ccNodeType_get_serviceTag(nt, env), adb_ccNodeType_get_iqn(nt, env));
    }
    printf("\n");

    /*    for (i=0; i<adb_describeResourcesResponseType_sizeof_serviceTags(drrt, env); i++) {
      printf(":%s:", adb_describeResourcesResponseType_get_serviceTags_at(drrt, env, i));
    }
    printf("\n");*/
    
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

  EUCA_MESSAGE_MARSHAL(describeInstancesType, dit, (&mymeta));
  /*
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
  adb_describeInstancesType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_describeInstancesType_add_services(drt, env, sit);
  */

  diIn = adb_DescribeInstances_create(env);
  adb_DescribeInstances_set_DescribeInstances(diIn, env, dit);
  
  diOut = axis2_stub_op_EucalyptusCC_DescribeInstances(stub, env, diIn);
  if (!diOut) {
    printf("ERROR: DI failed NULL\n");
    return(1);
  } else {
    //    adb_reservationInfoType_t *resit;
    adb_ccInstanceType_t *it;
    axis2_char_t *instId=NULL;
    int i;
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
	char *ownerId, *accountId, *keyName;
	char *uuid;
	int networkIndex;
	
	it = adb_describeInstancesResponseType_get_instances_at(dirt, env, i);
	instId = adb_ccInstanceType_get_instanceId(it, env);
	//amiId = adb_ccInstanceType_get_imageId(it, env);
	reservationId = adb_ccInstanceType_get_reservationId(it, env);
	ownerId = adb_ccInstanceType_get_ownerId(it, env);
	accountId = adb_ccInstanceType_get_accountId(it, env);
	keyName = adb_ccInstanceType_get_keyName(it, env);
	state = adb_ccInstanceType_get_stateName(it, env);
	nct = adb_ccInstanceType_get_netParams(it, env);
	vm = adb_ccInstanceType_get_instanceType(it, env);
	uuid = adb_ccInstanceType_get_uuid(it, env);
	//	networkIndex = adb_ccInstanceType_get_networkIndex(it, env);

	if (0)
	{
	  axutil_date_time_t *dt;
	  time_t ts, tsu, tsdelta, tsdelta_min;
	  struct tm *tmu;
	  ts = time(NULL);
	  tmu = gmtime(&ts);
	  tsu = mktime(tmu);
	  tsdelta = (tsu - ts) / 3600;
	  tsdelta_min = ((tsu - ts) - (tsdelta * 3600)) / 60;
	  dt = adb_ccInstanceType_get_launchTime(it, env);

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
	
	char *volId;
	adb_volumeType_t *vol;
	vol = adb_ccInstanceType_get_volumes_at(it, env, 0);
	volId = adb_volumeType_get_volumeId(vol, env);

	networkIndex = adb_netConfigType_get_networkIndex(nct, env);
	printf("Desc: uuid=%s instanceId=%s reservationId=%s ownerId=%s accountId=%s state=%s privMac=%s privIp=%s pubIp=%s vlan=%d keyName=%s vmTypeName=%s cores=%d mem=%d disk=%d serviceTag=%s userData=%s launchIndex=%s groupName=%s volId=%s networkIndex=%d\n", uuid, instId, reservationId, ownerId, accountId, state, adb_netConfigType_get_privateMacAddress(nct, env), adb_netConfigType_get_privateIp(nct, env), adb_netConfigType_get_publicIp(nct, env), adb_netConfigType_get_vlan(nct, env), keyName, adb_virtualMachineType_get_name(vm, env), adb_virtualMachineType_get_cores(vm, env),adb_virtualMachineType_get_memory(vm, env),adb_virtualMachineType_get_disk(vm, env), adb_ccInstanceType_get_serviceTag(it, env), adb_ccInstanceType_get_userData(it, env), adb_ccInstanceType_get_launchIndex(it, env), adb_ccInstanceType_get_groupNames_at(it, env, 0), volId, networkIndex);
	
      }
    }
  }
  return 0;
}

int cc_runInstances(char *amiId, char *amiURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, int num, int vlan, char *netName, virtualMachine *vm_type, axutil_env_t *env, axis2_stub_t *stub) {
  int i;
  adb_RunInstances_t *riIn;
  adb_runInstancesType_t *rit;

  adb_RunInstancesResponse_t *riOut;
  adb_runInstancesResponseType_t *rirt;
  
  adb_virtualMachineType_t *vm = copy_vm_type_to_adb (env, vm_type);
  
  srand(time(NULL));
  
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
  EUCA_MESSAGE_MARSHAL(runInstancesType, rit, (&mymeta));
  /*
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
  adb_runInstancesType_set_epoch(drt, env, 1);

  sit = adb_serviceInfoType_create(env);
  adb_serviceInfoType_set_type(sit, env, "walrus");
  adb_serviceInfoType_set_name(sit, env, "thewalrus");
  adb_serviceInfoType_add_uris(sit, env, "http://localhost:1234/meh");
  
  adb_runInstancesType_add_services(drt, env, sit);
  */

  adb_runInstancesType_set_instanceType(rit, env, vm);

  //  snprintf(mac, 32, "aa:dd:11:%c%c:%c%c:%c%c", rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65);
  //  adb_runInstancesType_set_privateMacBase(rit, env, mac);
  //  snprintf(mac, 32, "aa:dd:11:%c%c:%c%c:%c%c", rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65);
  //  printf("running MACC: %s VLAN: %d\n", mac, vlan);  
  //  adb_runInstancesType_set_publicMacBase(rit, env, mac);
  //  adb_runInstancesType_set_macLimit(rit, env, 100);

  adb_runInstancesType_set_vlan(rit, env, vlan);
  
  {
    char *instId, *resId, *mac;
    int j, i;
    int SIZE = 32;
    resId = malloc(sizeof(char) * SIZE);
    instId = malloc(sizeof(char) * SIZE);
    mac = malloc(sizeof(char) * SIZE);
    
    for (i=0; i<num; i++) {
      snprintf(mac, SIZE, "aa:dd:11:%c%c:%c%c:%c%c", rand()%5 + 65,rand()%5 + 65, rand()%5 + 65,rand()%5 + 65,rand()%5 + 65,rand()%5 + 65);
      adb_runInstancesType_add_macAddresses(rit, env, mac);
      
      snprintf(instId, SIZE, "i-");
      snprintf(resId, SIZE, "r-");
      for (j=0; j<8; j++) {
	char c[2];
	snprintf(c, 2, "%c",(rand()%26) + 97);
	strncat(instId, c, SIZE - strlen(instId) - 1);
      }
      adb_runInstancesType_add_instanceIds(rit, env, instId);

    }
    for (j=0; j<8; j++) {
      char c[2];
      snprintf(c, 2, "%c",(rand()%26) + 97);
      strncat(resId, c, SIZE - strlen(resId) - 1);
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
    axis2_char_t *instId;
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

int cc_describeServices(axutil_env_t *env, axis2_stub_t *stub) {
  adb_DescribeServices_t *adbrequest;
  adb_describeServicesType_t *adbinput;

  adb_DescribeServicesResponse_t *adbresponse;
  adb_describeServicesResponseType_t *adboutput;

  adb_serviceInfoType_t *sit=NULL;

  int i;
  axis2_bool_t status;

  adbinput = adb_describeServicesType_create(env);

  EUCA_MESSAGE_MARSHAL(describeServicesType, adbinput, (&mymeta));
  
  sit = adb_serviceInfoType_create(env);
  
    adb_serviceInfoType_set_type(sit, env, "cc");
    adb_serviceInfoType_set_name(sit, env, "self");
    adb_serviceInfoType_add_uris(sit, env, "http://localhost:8774");
  
    adb_describeServicesType_add_serviceIds(adbinput, env, sit);

  adbrequest = adb_DescribeServices_create(env);
  adb_DescribeServices_set_DescribeServices(adbrequest, env, adbinput);
  
  adbresponse = axis2_stub_op_EucalyptusCC_DescribeServices(stub, env, adbrequest);
  if (!adbresponse) {
    printf("ERROR: DescribeServices failed NULL\n");
    return(1);
  } else {
    adboutput = adb_DescribeServicesResponse_get_DescribeServicesResponse(adbresponse, env);
    status = adb_describeServicesResponseType_get_return(adboutput, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_describeServicesResponseType_get_statusMessage(adboutput, env));
    } else {
      for (i=0; i<adb_describeServicesResponseType_sizeof_serviceStatuses(adboutput, env); i++) {
	int j;
	adb_serviceStatusType_t *sst=NULL;
	adb_serviceInfoType_t *sit=NULL;
	sst = adb_describeServicesResponseType_get_serviceStatuses_at(adboutput, env, i);
	printf("localState=%s localEpoch=%d details=%s\n", adb_serviceStatusType_get_localState(sst, env), adb_serviceStatusType_get_localEpoch(sst, env), adb_serviceStatusType_get_details_at(sst, env, 0));
	sit = adb_serviceStatusType_get_serviceId(sst, env);
	printf("\ttype=%s name=%s\n", adb_serviceInfoType_get_type(sit, env), adb_serviceInfoType_get_name(sit, env));
	for (j=0; j<adb_serviceInfoType_sizeof_uris(sit, env); j++) {
	  printf("\t\turi=%s\n", adb_serviceInfoType_get_uris_at(sit, env, j));
	}
      }
    }
  }
  return(!status);  
}


int cc_startService(axutil_env_t *env, axis2_stub_t *stub) {
  adb_StartService_t *adbrequest;
  adb_startServiceType_t *adbinput;

  adb_StartServiceResponse_t *adbresponse;
  adb_startServiceResponseType_t *adboutput;
  int i;
  axis2_bool_t status;

  adbinput = adb_startServiceType_create(env);

  EUCA_MESSAGE_MARSHAL(startServiceType, adbinput, (&mymeta));
  
  adbrequest = adb_StartService_create(env);
  adb_StartService_set_StartService(adbrequest, env, adbinput);
  
  adbresponse = axis2_stub_op_EucalyptusCC_StartService(stub, env, adbrequest);
  if (!adbresponse) {
    printf("ERROR: StartService failed NULL\n");
    return(1);
  } else {
    adboutput = adb_StartServiceResponse_get_StartServiceResponse(adbresponse, env);
    status = adb_startServiceResponseType_get_return(adboutput, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_startServiceResponseType_get_statusMessage(adboutput, env));
    } else {
    }
  }
  return(!status);  
}


int cc_stopService(axutil_env_t *env, axis2_stub_t *stub) {
  adb_StopService_t *adbrequest;
  adb_stopServiceType_t *adbinput;

  adb_StopServiceResponse_t *adbresponse;
  adb_stopServiceResponseType_t *adboutput;
  int i;
  axis2_bool_t status;

  adbinput = adb_stopServiceType_create(env);

  EUCA_MESSAGE_MARSHAL(stopServiceType, adbinput, (&mymeta));
  
  adbrequest = adb_StopService_create(env);
  adb_StopService_set_StopService(adbrequest, env, adbinput);
  
  adbresponse = axis2_stub_op_EucalyptusCC_StopService(stub, env, adbrequest);
  if (!adbresponse) {
    printf("ERROR: StopService failed NULL\n");
    return(1);
  } else {
    adboutput = adb_StopServiceResponse_get_StopServiceResponse(adbresponse, env);
    status = adb_stopServiceResponseType_get_return(adboutput, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_stopServiceResponseType_get_statusMessage(adboutput, env));
    } else {
    }
  }
  return(!status);  
}


int cc_enableService(axutil_env_t *env, axis2_stub_t *stub) {
  adb_EnableService_t *adbrequest;
  adb_enableServiceType_t *adbinput;

  adb_EnableServiceResponse_t *adbresponse;
  adb_enableServiceResponseType_t *adboutput;
  int i;
  axis2_bool_t status;

  adbinput = adb_enableServiceType_create(env);

  EUCA_MESSAGE_MARSHAL(enableServiceType, adbinput, (&mymeta));
  
  adbrequest = adb_EnableService_create(env);
  adb_EnableService_set_EnableService(adbrequest, env, adbinput);
  
  adbresponse = axis2_stub_op_EucalyptusCC_EnableService(stub, env, adbrequest);
  if (!adbresponse) {
    printf("ERROR: EnableService failed NULL\n");
    return(1);
  } else {
    adboutput = adb_EnableServiceResponse_get_EnableServiceResponse(adbresponse, env);
    status = adb_enableServiceResponseType_get_return(adboutput, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_enableServiceResponseType_get_statusMessage(adboutput, env));
    } else {
    }
  }
  return(!status);  
}


int cc_disableService(axutil_env_t *env, axis2_stub_t *stub) {
  adb_DisableService_t *adbrequest;
  adb_disableServiceType_t *adbinput;

  adb_DisableServiceResponse_t *adbresponse;
  adb_disableServiceResponseType_t *adboutput;
  int i;
  axis2_bool_t status;

  adbinput = adb_disableServiceType_create(env);

  EUCA_MESSAGE_MARSHAL(disableServiceType, adbinput, (&mymeta));
  
  adbrequest = adb_DisableService_create(env);
  adb_DisableService_set_DisableService(adbrequest, env, adbinput);
  
  adbresponse = axis2_stub_op_EucalyptusCC_DisableService(stub, env, adbrequest);
  if (!adbresponse) {
    printf("ERROR: DisableService failed NULL\n");
    return(1);
  } else {
    adboutput = adb_DisableServiceResponse_get_DisableServiceResponse(adbresponse, env);
    status = adb_disableServiceResponseType_get_return(adboutput, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_disableServiceResponseType_get_statusMessage(adboutput, env));
    } else {
    }
  }
  return(!status);  
}

int cc_shutdownService(axutil_env_t *env, axis2_stub_t *stub) {
  adb_ShutdownService_t *adbrequest;
  adb_shutdownServiceType_t *adbinput;

  adb_ShutdownServiceResponse_t *adbresponse;
  adb_shutdownServiceResponseType_t *adboutput;
  int i;
  axis2_bool_t status;

  adbinput = adb_shutdownServiceType_create(env);

  EUCA_MESSAGE_MARSHAL(shutdownServiceType, adbinput, (&mymeta));
  
  adbrequest = adb_ShutdownService_create(env);
  adb_ShutdownService_set_ShutdownService(adbrequest, env, adbinput);
  
  adbresponse = axis2_stub_op_EucalyptusCC_ShutdownService(stub, env, adbrequest);
  if (!adbresponse) {
    printf("ERROR: ShutdownService failed NULL\n");
    return(1);
  } else {
    adboutput = adb_ShutdownServiceResponse_get_ShutdownServiceResponse(adbresponse, env);
    status = adb_shutdownServiceResponseType_get_return(adboutput, env);
    if (status == AXIS2_FALSE) {
      printf("operation fault '%s'\n", adb_shutdownServiceResponseType_get_statusMessage(adboutput, env));
    } else {
    }
  }
  return(!status);  
}

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
#ifndef INCLUDE_DATA_H
#define INCLUDE_DATA_H

#include <pthread.h>
#include "eucalyptus.h"

#define CHAR_BUFFER_SIZE 512
#define BIG_CHAR_BUFFER_SIZE 1024

typedef struct ncMetadata_t {
    char *correlationId;
    char *userId;
} ncMetadata;

typedef struct deviceMapping_t {
	char deviceName[64];
	char virtualName[64];
	int size;
	char format[64];
} deviceMapping;

typedef struct virtualMachine_t {
	int mem, cores, disk;
	char name[64];
	deviceMapping deviceMapping[EUCA_MAX_DEVMAPS];
} virtualMachine;
int allocate_virtualMachine(virtualMachine *out, const virtualMachine *in);

typedef struct netConfig_t {
  int vlan, networkIndex;
  char privateMac[24], publicIp[24], privateIp[24];
} netConfig;
int allocate_netConfig(netConfig *out, char *pvMac, char *pvIp, char *pbIp, int vlan, int networkIndex);

typedef struct ncVolume_t {
    char volumeId[CHAR_BUFFER_SIZE];
    char remoteDev[CHAR_BUFFER_SIZE];
    char localDev[CHAR_BUFFER_SIZE];
    char localDevReal[CHAR_BUFFER_SIZE];
    char stateName[CHAR_BUFFER_SIZE];
} ncVolume;

typedef struct ncInstance_t {
    char instanceId[CHAR_BUFFER_SIZE];
    char imageId[CHAR_BUFFER_SIZE];
    char imageURL[CHAR_BUFFER_SIZE];
    char kernelId[CHAR_BUFFER_SIZE];
    char kernelURL[CHAR_BUFFER_SIZE];
    char ramdiskId[CHAR_BUFFER_SIZE];
    char ramdiskURL[CHAR_BUFFER_SIZE];
    char reservationId[CHAR_BUFFER_SIZE];
    char userId[CHAR_BUFFER_SIZE];
    int retries;
    
    /* state as reported to CC & CLC */
    char stateName[CHAR_BUFFER_SIZE];  /* as string */
    int stateCode; /* as int */

    /* state as NC thinks of it */
    instance_states state;

    char keyName[CHAR_BUFFER_SIZE*4];
    char privateDnsName[CHAR_BUFFER_SIZE];
    char dnsName[CHAR_BUFFER_SIZE];
    int launchTime; // timestamp of RunInstances request arrival
    int bootTime; // timestamp of STAGING->BOOTING transition
    int terminationTime; // timestamp of when resources are released (->TEARDOWN transition)
    
    virtualMachine params;
    netConfig ncnet;
    pthread_t tcb;

    /* passed into NC via runInstances for safekeeping */
    char userData[CHAR_BUFFER_SIZE*10];
    char launchIndex[CHAR_BUFFER_SIZE];
    char groupNames[EUCA_MAX_GROUPS][CHAR_BUFFER_SIZE];
    int groupNamesSize;

    /* updated by NC upon Attach/DetachVolume */
    ncVolume volumes[EUCA_MAX_VOLUMES];
    int volumesSize;
} ncInstance;

typedef struct ncResource_t {
    char nodeStatus[CHAR_BUFFER_SIZE];
    int memorySizeMax;
    int memorySizeAvailable;
    int diskSizeMax;
    int diskSizeAvailable;
    int numberOfCoresMax;
    int numberOfCoresAvailable;
    char publicSubnets[CHAR_BUFFER_SIZE];
} ncResource;

/* TODO: make this into something smarter than a linked list */
typedef struct bunchOfInstances_t {
    ncInstance * instance;
    int count; /* only valid on first node */
    struct bunchOfInstances_t * next;
} bunchOfInstances;

typedef enum instance_error_codes_t {
    OUT_OF_MEMORY = 99,
    DUPLICATE,
    NOT_FOUND,
} instance_error_codes;

instance_error_codes add_instance (bunchOfInstances ** head, ncInstance * instance);
instance_error_codes remove_instance (bunchOfInstances ** head, ncInstance * instance);
instance_error_codes for_each_instance (bunchOfInstances ** headp, void (* function)(bunchOfInstances **, ncInstance *, void *), void *);
ncInstance * find_instance (bunchOfInstances **headp, char * instanceId);
ncInstance * get_instance (bunchOfInstances **head);
int total_instances (bunchOfInstances **head);

ncMetadata * allocate_metadata(char *correlationId, char *userId);
void free_metadata(ncMetadata ** meta);

ncInstance * allocate_instance(char *instanceId, char *reservationId, 
                               virtualMachine *params, 
                               char *imageId, char *imageURL, 
                               char *kernelId, char *kernelURL, 
                               char *ramdiskId, char *ramdiskURL, 
                               char *stateName, int stateCode, char *userId, 
                               netConfig *ncnet, char *keyName,
                               char *userData, char *launchIndex, char **groupNames, int groupNamesSize);
void free_instance (ncInstance ** inst);

ncResource * allocate_resource(char *nodeStatus, 
                               int memorySizeMax, int memorySizeAvailable, 
                               int diskSizeMax, int diskSizeAvailable,
                               int numberOfCoresMax, int numberOfCoresAvailable,
                               char *publicSubnets);
void free_resource(ncResource ** res);
ncVolume * find_volume (ncInstance * instance, char *volumeId);
ncVolume *  add_volume (ncInstance * instance, char *volumeId, char *remoteDev, char *localDev, char *localDevReal, char *stateName);
ncVolume * free_volume (ncInstance * instance, char *volumeId, char *remoteDev, char *localDev);

#endif


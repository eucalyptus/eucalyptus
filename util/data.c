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
#define __USE_GNU
#include <string.h> 
#include <assert.h>
#include "data.h"

int allocate_virtualMachine(virtualMachine *out, const virtualMachine *in)
{
  if (out != NULL) {
    out->mem = in->mem;
    out->disk = in->disk;
    out->cores = in->cores;
    snprintf(out->name, 64, "%s", in->name);

    int i;
    for (i=0; i<EUCA_MAX_VBRS && i<in->virtualBootRecordLen; i++) { // TODO: dan ask dmitrii
            virtualBootRecord * out_r = out->virtualBootRecord + i;
            const virtualBootRecord * in_r = in->virtualBootRecord + i;
            strncpy (out_r->resourceLocation, in_r->resourceLocation, sizeof (out_r->resourceLocation));
            strncpy (out_r->guestDeviceName, in_r->guestDeviceName, sizeof (out_r->guestDeviceName));
            strncpy (out_r->id, in_r->id, sizeof (out_r->id));
            strncpy (out_r->typeName, in_r->typeName, sizeof (out_r->typeName));
            out_r->size = in_r->size;
            strncpy (out_r->formatName, in_r->formatName, sizeof (out_r->formatName));
    }
  }
  return(0);
}

int allocate_netConfig(netConfig *out, char *pvMac, char *pvIp, char *pbIp, int vlan, int networkIndex) {
  if (out != NULL) {
    if (pvMac) safe_strncpy(out->privateMac,pvMac,24);
    if (pvIp) safe_strncpy(out->privateIp,pvIp,24);
    if (pbIp) safe_strncpy(out->publicIp,pbIp,24);
    out->networkIndex = networkIndex;
    out->vlan = vlan;
  }
  return(0);
}

/* metadata is present in every type of nc request */
ncMetadata * allocate_metadata (char *correlationId, char *userId)
{
    ncMetadata * meta;

    if (!(meta = malloc(sizeof(ncMetadata)))) return NULL;
    if (correlationId) meta->correlationId = strdup(correlationId);
    if (userId)        meta->userId        = strdup(userId);

    return meta;
}

void free_metadata (ncMetadata ** metap)
{
    ncMetadata * meta = * metap;
    if (meta->correlationId) free(meta->correlationId);
    if (meta->userId)        free(meta->userId);
    * metap = NULL;
}

/* instances are present in instance-related requests */
ncInstance * allocate_instance (char *uuid,
                                char *instanceId, char *reservationId, 
                                virtualMachine *params, 
                                char *stateName, int stateCode, char *userId, char *ownerId, char *accountId,
                                netConfig *ncnet, char *keyName,
                                char *userData, char *launchIndex, char *platform, int expiryTime, char **groupNames, int groupNamesSize)
{
    ncInstance * inst;

    /* zeroed out for cleaner-looking checkpoints and
     * strings that are empty unless set */
    inst = calloc(1, sizeof(ncInstance));
    if (!inst) return(NULL);

    if (userData) {
        safe_strncpy(inst->userData, userData, CHAR_BUFFER_SIZE*32);
    }

    if (launchIndex) {
        safe_strncpy(inst->launchIndex, launchIndex, CHAR_BUFFER_SIZE);
    }

    if (platform) {
        safe_strncpy(inst->platform, platform, CHAR_BUFFER_SIZE);
    }

    inst->groupNamesSize = groupNamesSize;
    if (groupNames && groupNamesSize) {
        int i;
        for (i=0; i<groupNamesSize && groupNames[i]; i++) {
            safe_strncpy(inst->groupNames[i], groupNames[i], CHAR_BUFFER_SIZE);
        }
    }
    
    if (ncnet != NULL) {
      memcpy(&(inst->ncnet), ncnet, sizeof(netConfig));
    }
    
    if (uuid) {
      safe_strncpy(inst->uuid, uuid, CHAR_BUFFER_SIZE);
    }

    if (instanceId) {
      safe_strncpy(inst->instanceId, instanceId, CHAR_BUFFER_SIZE);
    }

    if (keyName) {
      safe_strncpy(inst->keyName, keyName, CHAR_BUFFER_SIZE*4);
    }

    if (reservationId) {
      safe_strncpy(inst->reservationId, reservationId, CHAR_BUFFER_SIZE);
    }

    if (stateName) {
      safe_strncpy(inst->stateName, stateName, CHAR_BUFFER_SIZE);
    }
    if (userId) {
      safe_strncpy(inst->userId, userId, CHAR_BUFFER_SIZE);
    }
    if (ownerId) {
        safe_strncpy(inst->ownerId, ownerId, CHAR_BUFFER_SIZE);
    }
    if (accountId) {
        safe_strncpy(inst->accountId, accountId, CHAR_BUFFER_SIZE);
    }

    if (params) {
      memcpy(&(inst->params), params, sizeof(virtualMachine));
    }
    inst->stateCode = stateCode;
    safe_strncpy (inst->bundleTaskStateName, bundling_progress_names [NOT_BUNDLING], CHAR_BUFFER_SIZE);
    inst->expiryTime = expiryTime;
    return inst;
}

void free_instance (ncInstance ** instp) 
{
    ncInstance * inst;

    if (!instp) return;
    inst = * instp;
    * instp = NULL;
    if (!inst) return;

    free (inst);
}

/* resource is used to return information about resources */
ncResource * allocate_resource (char *nodeStatus,
                                char *iqn,
                                int memorySizeMax, int memorySizeAvailable, 
                                int diskSizeMax, int diskSizeAvailable,
                                int numberOfCoresMax, int numberOfCoresAvailable,
                                char *publicSubnets)
{
    ncResource * res;
    
    if (!nodeStatus) return NULL;
    if (!(res = malloc(sizeof(ncResource)))) return NULL;
    bzero(res, sizeof(ncResource));
    safe_strncpy(res->nodeStatus, nodeStatus, CHAR_BUFFER_SIZE);
    if (iqn) {
      safe_strncpy(res->iqn, iqn, CHAR_BUFFER_SIZE);
    }
    if (publicSubnets) {
      safe_strncpy(res->publicSubnets, publicSubnets, CHAR_BUFFER_SIZE);
    }
    res->memorySizeMax = memorySizeMax;
    res->memorySizeAvailable = memorySizeAvailable;
    res->diskSizeMax = diskSizeMax;
    res->diskSizeAvailable = diskSizeAvailable;
    res->numberOfCoresMax = numberOfCoresMax;
    res->numberOfCoresAvailable = numberOfCoresAvailable;
    
    return res;
}

void free_resource (ncResource ** resp)
{
    ncResource * res;

    if (!resp) return;
    res = * resp;
    * resp = NULL;
    if (!res) return;

    free (res);
}

instance_error_codes add_instance (bunchOfInstances ** headp, ncInstance * instance)
{
    bunchOfInstances * new = malloc (sizeof(bunchOfInstances));

    if ( new == NULL ) return OUT_OF_MEMORY;
    new->instance = instance;
    new->next = NULL;

    if ( * headp == NULL) {
        * headp = new;
        (* headp)->count = 1;

    } else {
        bunchOfInstances * last = * headp;
        do {
            if ( !strcmp(last->instance->instanceId, instance->instanceId) ) {
                free (new);
                return DUPLICATE;
            }
        } while ( last->next && (last = last->next) );
        last->next = new;
        (* headp)->count++;
    }        

    return OK;
}

instance_error_codes remove_instance (bunchOfInstances ** headp, ncInstance * instance)
{
    bunchOfInstances * head, * prev = NULL;

    for ( head = * headp; head; ) {
        int count = (* headp)->count;

        if ( !strcmp(head->instance->instanceId, instance->instanceId) ) {
            if ( prev ) {
                prev->next = head->next;
            } else {
                * headp = head->next;
            }
            if (* headp) {
                (* headp)->count = count - 1;
            }
            free (head);
            return OK;
        }
        prev = head;
        head = head->next;
    }
    return NOT_FOUND;
}

instance_error_codes for_each_instance (bunchOfInstances ** headp, void (* function)(bunchOfInstances **, ncInstance *, void *), void *param) 
{
    bunchOfInstances * head;
    
    for ( head = * headp; head; head = head->next ) {
        function ( headp, head->instance, param );
    }
    
    return OK;
}

ncInstance * find_instance (bunchOfInstances **headp, char * instanceId)
{
    bunchOfInstances * head;
    
    for ( head = * headp; head; head = head->next ) {
        if (!strcmp(head->instance->instanceId, instanceId)) {
            return head->instance;
        }
    }
    
    return NULL;
}

ncInstance * get_instance (bunchOfInstances **headp)
{
    static bunchOfInstances * current = NULL;
    
    /* advance static variable, wrapping to head if at the end */
    if ( current == NULL ) current = * headp;
    else current = current->next;
    
    /* return the new value, if any */
    if ( current == NULL ) return NULL;
    else return current->instance;
}

int total_instances (bunchOfInstances **headp)
{
    if ( * headp ) return (* headp)->count;
    else return 0;
}

/* 
 * finds a matching volume 
 * OR returns a pointer to the next empty/avail volume slot 
 * OR if full, returns NULL
 */
static ncVolume * find_volume (ncInstance * instance, const char *volumeId) 
{
    ncVolume * v = instance->volumes;
    ncVolume * match = NULL;
    ncVolume * avail = NULL;
    ncVolume * empty = NULL;

    for (int i=0; i<EUCA_MAX_VOLUMES; i++,v++) {
        // look for matches
        if (! strncmp (v->volumeId, volumeId, CHAR_BUFFER_SIZE)) {
            assert (match==NULL);
            match = v;
        }
        
        // look for the first empty and available slot
        if (! strnlen (v->volumeId, CHAR_BUFFER_SIZE)) {
            if (empty==NULL)
                empty = v;
        } else if (! is_volume_used (v)) {
            if (avail==NULL)
                avail = v;
        }
    }

    if (match) return match;
    if (empty) return empty;
    return avail;
}

// returns 0 if volume slot is not in use and non-zero if it is
int is_volume_used (const ncVolume * v)
{
    if (strlen (v->stateName) == 0) 
        return 0;
    else
        return strcmp (v->stateName, VOL_STATE_ATTACHING_FAILED)
            && strcmp (v->stateName, VOL_STATE_DETACHED);
}

// records volume's information in the instance struct, updating
// the non-NULL values if the record already exists 
ncVolume * save_volume (ncInstance * instance, const char *volumeId, const char *remoteDev, const char *localDev, const char *localDevReal, const char *stateName)
{
    assert (instance!=NULL);
    assert (volumeId!=NULL);
    ncVolume * v = find_volume (instance, volumeId);

    if ( v != NULL) {
        safe_strncpy (v->volumeId, volumeId, CHAR_BUFFER_SIZE);
        if (remoteDev)
            safe_strncpy (v->remoteDev, remoteDev, CHAR_BUFFER_SIZE);
        if (localDev)
            safe_strncpy (v->localDev, localDev, CHAR_BUFFER_SIZE);
        if (localDevReal)
            safe_strncpy (v->localDevReal, localDevReal, CHAR_BUFFER_SIZE);
        if (stateName)
            safe_strncpy (v->stateName, stateName, CHAR_BUFFER_SIZE);
    }
    
    return v;
}

// zeroes out the volume's slot in the instance struct (no longer used)
ncVolume * free_volume (ncInstance * instance, const char *volumeId)
{
    ncVolume * v = find_volume (instance, volumeId);
    
    if ( v == NULL) {
        return NULL; /* not there (and out of room) */
    }

    if ( strncmp (v->volumeId, volumeId, CHAR_BUFFER_SIZE) ) {
        return NULL; /* not there */
	
    } else {
        ncVolume * last_v = instance->volumes+(EUCA_MAX_VOLUMES-1);
        int slots_left = last_v - v;
        /* shift the remaining entries up, empty or not */
        if (slots_left)
            memmove (v, v+1, slots_left*sizeof(ncVolume));
        
        /* empty the last one */
        bzero (last_v, sizeof(ncVolume));
    }
    
    return v;
}

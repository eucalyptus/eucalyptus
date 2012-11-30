// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

//!
//! @file util/data.c
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#define __USE_GNU
#include <string.h>
#include <assert.h>
#include "data.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! List of string to convert the hypervisor capability types enumeration
const char *hypervsorCapabilityTypeNames[] = {
    "unknown",
    "xen",
    "hw",
    "xen+hw"
};

//! List of string to convert the LIBVIRT device type enumeration
const char *libvirtDevTypeNames[] = {
    "disk",
    "floppy",
    "cdrom"
};

//! List of string to convert the LIBVIRT bus types enumeration
const char *libvirtBusTypeNames[] = {
    "ide",
    "scsi",
    "virtio",
    "xen"
};

//! List of string to convert the LIBVIRT source types enumeration
const char *libvirtSourceTypeNames[] = {
    "file",
    "block"
};

//! List of string to convert the LIBVIRT NIC types enumeration
const char *libvirtNicTypeNames[] = {
    "none",
    "e1000",
    "rtl8139",
    "virtio"
};

//! List of string to convert the NC resource types enumeration
const char *ncResourceTypeName[] = {
    "image",
    "ramdisk",
    "kernel",
    "ephemeral",
    "swap",
    "ebs"
};

//! String value of each instance state enumeration entry
const char *instance_state_names[] = {
    "Unknown",
    "Running",
    "Waiting",
    "Paused",
    "Shutdown",
    "Shutoff",
    "Crashed",

    "Staging",
    "Booting",
    "Canceled",

    "Bundling-Shutdown",
    "Bundling-Shutoff",
    "CreateImage-Shutdown",
    "CreateImage-Shutoff",

    "Pending",
    "Extant",
    "Teardown"
};

//! String value of each bundling progress state enumeration entry
const char *bundling_progress_names[] = {
    "none",
    "bundling",
    "succeeded",
    "failed",
    "cancelled"
};

//! String value of each create image progress state enumeration entry
const char *createImage_progress_names[] = {
    "none",
    "creating",
    "succeeded",
    "failed",
    "cancelled"
};

//! String value of each error enumeration entry
const char *euca_error_names[] = {
    "ok",
    "operation error",
    "fatal operation error",
    "entry not found",
    "memory failure",
    "I/O error",
    "hypervisor error",
    "thread error",
    "duplicate entry error",
    "invalid arguments",
    "overflow error",
    "operation unsupported error",
    "operation not permitted error",
    "access denied error",
    "no space available error",
    "timeout error",
    "unknown",
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int allocate_virtualMachine(virtualMachine * out, const virtualMachine * in);
int allocate_netConfig(netConfig * out, char *pvMac, char *pvIp, char *pbIp, int vlan, int networkIndex);

ncMetadata *allocate_metadata(char *correlationId, char *userId);
void free_metadata(ncMetadata ** pMeta);

ncInstance *allocate_instance(char *uuid, char *instanceId, char *reservationId, virtualMachine * params, const char *stateName, int stateCode,
                              char *userId, char *ownerId, char *accountId, netConfig * ncnet, char *keyName, char *userData, char *launchIndex,
                              char *platform, int expiryTime, char **groupNames, int groupNamesSize);
void free_instance(ncInstance ** instp);
int add_instance(bunchOfInstances ** headp, ncInstance * instance);
int remove_instance(bunchOfInstances ** headp, ncInstance * instance);
int for_each_instance(bunchOfInstances ** headp, void (*function) (bunchOfInstances **, ncInstance *, void *), void *param);
ncInstance *find_instance(bunchOfInstances ** headp, char *instanceId);
ncInstance *get_instance(bunchOfInstances ** headp);
int total_instances(bunchOfInstances ** headp);

ncResource *allocate_resource(char *nodeStatus, char *iqn, int memorySizeMax, int memorySizeAvailable, int diskSizeMax, int diskSizeAvailable,
                              int numberOfCoresMax, int numberOfCoresAvailable, char *publicSubnets);
void free_resource(ncResource ** resp);

boolean is_volume_used(const ncVolume * v);
ncVolume *save_volume(ncInstance * instance, const char *volumeId, const char *remoteDev, const char *localDev, const char *localDevReal,
                      const char *stateName);
ncVolume *free_volume(ncInstance * instance, const char *volumeId);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static ncVolume *find_volume(ncInstance * instance, const char *volumeId);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Initialize a virtual machine structure from another
//!
//! @param[out] out a pointer to the resulting virtual machine structure
//! @param[in]  in a pointer to the virtual machine structure to duplicate
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @note The memory for the 'out' pointer should already have been allocated.
//!
int allocate_virtualMachine(virtualMachine * out, const virtualMachine * in)
{
    int i = 0;
    virtualBootRecord *out_r = NULL;
    const virtualBootRecord *in_r = NULL;

    if ((out != NULL) && (in != NULL)) {
        out->mem = in->mem;
        out->disk = in->disk;
        out->cores = in->cores;
        snprintf(out->name, 64, "%s", in->name);

        //! @todo dan ask dmitrii
        for (i = 0; ((i < EUCA_MAX_VBRS) && (i < in->virtualBootRecordLen)); i++) {
            out_r = out->virtualBootRecord + i;
            in_r = in->virtualBootRecord + i;
            strncpy(out_r->resourceLocation, in_r->resourceLocation, sizeof(out_r->resourceLocation));
            strncpy(out_r->guestDeviceName, in_r->guestDeviceName, sizeof(out_r->guestDeviceName));
            strncpy(out_r->id, in_r->id, sizeof(out_r->id));
            strncpy(out_r->typeName, in_r->typeName, sizeof(out_r->typeName));
            out_r->size = in_r->size;
            strncpy(out_r->formatName, in_r->formatName, sizeof(out_r->formatName));
        }

        return (EUCA_OK);
    }

    return (EUCA_ERROR);
}

//!
//! Initialize a network configuration structure with given values.
//!
//! @param[out] out a pointer to the resulting network configuration structure
//! @param[in]  pvMac the private MAC string
//! @param[in]  pvIp the private IP string
//! @param[in]  pbIp the public IP string
//! @param[in]  vlan the network Virtual LAN
//! @param[in]  networkIndex the network index
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @note The memory for the 'out' pointer should already have been allocated.
//!
int allocate_netConfig(netConfig * out, char *pvMac, char *pvIp, char *pbIp, int vlan, int networkIndex)
{
    if (out != NULL) {
        if (pvMac)
            safe_strncpy(out->privateMac, pvMac, 24);

        if (pvIp)
            safe_strncpy(out->privateIp, pvIp, 24);

        if (pbIp)
            safe_strncpy(out->publicIp, pbIp, 24);

        out->networkIndex = networkIndex;
        out->vlan = vlan;

        return (EUCA_OK);
    }

    return (EUCA_ERROR);
}

/*  */
//!
//! Allocate a metadata structure and initialize it. Metadata is present in every type of nc request
//!
//! @param[in] correlationId the correlation identifier string
//! @param[in] userId the user identifier
//!
//! @return a pointer to the newly allocated metadata structure or NULL if any error occured.
//!
ncMetadata *allocate_metadata(char *correlationId, char *userId)
{
    ncMetadata *meta;

    if ((meta = EUCA_ZALLOC(1, sizeof(ncMetadata))) == NULL)
        return (NULL);

    meta->correlationId = ((correlationId != NULL) ? strdup(correlationId) : NULL);
    meta->userId = ((userId != NULL) ? strdup(userId) : NULL);
    return (meta);
}

//!
//! Frees an allocated metadata structure.
//!
//! @param[in,out] pMeta a pointer to the node controller (NC) metadata structure
//!
void free_metadata(ncMetadata ** pMeta)
{
    ncMetadata *meta = NULL;
    if (pMeta != NULL) {
        if ((meta = (*pMeta)) != NULL) {
            EUCA_FREE(meta->correlationId);
            EUCA_FREE(meta->userId);
            EUCA_FREE(meta);
            *pMeta = NULL;
        }
    }
}

//!
//! Allocate and initialize an instance structure with given information. Instances are
//! present in instance-related requests.
//!
//! @param[in] uuid the unique user identifier string
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] reservationId the reservation identifier string
//! @param[in] params a pointer to our virtual machine parametes
//! @param[in] stateName the current instance state name string
//! @param[in] stateCode the current instance state code
//! @param[in] userId the user identifier string
//! @param[in] ownerId the owner identifier string
//! @param[in] accountId the account identifier string
//! @param[in] ncnet a pointer to the network configuration of this instance
//! @param[in] keyName the SSH key name to use
//! @param[in] userData user data string to pass to the instance
//! @param[in] launchIndex the instance's launch index
//! @param[in] platform the instance's platform ty[e
//! @param[in] expiryTime the instance's expiration time before it reaches running
//! @param[in] groupNames a list of group name string
//! @param[in] groupNamesSize the number of group name in the groupNames list
//!
//! @return a pointer to the newly allocated instance structure or NULL if any error occured.
//!
ncInstance *allocate_instance(char *uuid, char *instanceId, char *reservationId, virtualMachine * params, const char *stateName, int stateCode,
                              char *userId, char *ownerId, char *accountId, netConfig * ncnet, char *keyName, char *userData, char *launchIndex,
                              char *platform, int expiryTime, char **groupNames, int groupNamesSize)
{
    int i = 0;
    ncInstance *inst = NULL;

    /* zeroed out for cleaner-looking checkpoints and strings that are empty unless set */
    if ((inst = EUCA_ZALLOC(1, sizeof(ncInstance))) == NULL)
        return (NULL);

    if (userData)
        safe_strncpy(inst->userData, userData, CHAR_BUFFER_SIZE * 32);

    if (launchIndex)
        safe_strncpy(inst->launchIndex, launchIndex, CHAR_BUFFER_SIZE);

    if (platform)
        safe_strncpy(inst->platform, platform, CHAR_BUFFER_SIZE);

    inst->groupNamesSize = groupNamesSize;
    if ((groupNames != NULL) && (groupNamesSize > 0)) {
        for (i = 0; i < groupNamesSize && groupNames[i]; i++)
            safe_strncpy(inst->groupNames[i], groupNames[i], CHAR_BUFFER_SIZE);
    }

    if (ncnet != NULL)
        memcpy(&(inst->ncnet), ncnet, sizeof(netConfig));

    if (uuid)
        safe_strncpy(inst->uuid, uuid, CHAR_BUFFER_SIZE);

    if (instanceId)
        safe_strncpy(inst->instanceId, instanceId, CHAR_BUFFER_SIZE);

    if (keyName)
        safe_strncpy(inst->keyName, keyName, CHAR_BUFFER_SIZE * 4);

    if (reservationId)
        safe_strncpy(inst->reservationId, reservationId, CHAR_BUFFER_SIZE);

    if (stateName)
        safe_strncpy(inst->stateName, stateName, CHAR_BUFFER_SIZE);

    if (userId)
        safe_strncpy(inst->userId, userId, CHAR_BUFFER_SIZE);

    if (ownerId)
        safe_strncpy(inst->ownerId, ownerId, CHAR_BUFFER_SIZE);

    if (accountId)
        safe_strncpy(inst->accountId, accountId, CHAR_BUFFER_SIZE);

    if (params)
        memcpy(&(inst->params), params, sizeof(virtualMachine));

    inst->stateCode = stateCode;
    safe_strncpy(inst->bundleTaskStateName, bundling_progress_names[NOT_BUNDLING], CHAR_BUFFER_SIZE);
    inst->expiryTime = expiryTime;
    return (inst);
}

//!
//! Frees an allocated instance structure.
//!
//! @param[in,out] instp a pointer to the instance structure pointer to free
//!
void free_instance(ncInstance ** instp)
{
    if (instp != NULL) {
        EUCA_FREE((*instp));
    }
}

//!
//! Adds an instance to an instance linked list
//!
//! @param[in] headp a pointer to a pointer to the head of the instance list
//! @param[in] instance a pointer to the instance to add to the list
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_MEMORY_ERROR
//!         and EUCA_DUPLICATE_ERROR.
//!
int add_instance(bunchOfInstances ** headp, ncInstance * instance)
{
    bunchOfInstances *new = NULL;
    bunchOfInstances *last = NULL;

    if ((new = EUCA_ZALLOC(1, sizeof(bunchOfInstances))) == NULL)
        return (EUCA_MEMORY_ERROR);

    new->instance = instance;
    new->next = NULL;

    if (*headp == NULL) {
        *headp = new;
        (*headp)->count = 1;
    } else {
        last = *headp;
        do {
            if (!strcmp(last->instance->instanceId, instance->instanceId)) {
                EUCA_FREE(new);
                return (EUCA_DUPLICATE_ERROR);
            }
        } while (last->next && (last = last->next));

        last->next = new;
        (*headp)->count++;
    }

    return (EUCA_OK);
}

//!
//! Removes an instance from an instance linked list
//!
//! @param[in] headp a pointer to a pointer to the head of the instance list
//! @param[in] instance a pointer to the instance to remove from the list
//!
//! @return EUCA_OK on success or proper error code. Known error code returned include: EUCA_NOT_FOUND_ERROR.
//!
int remove_instance(bunchOfInstances ** headp, ncInstance * instance)
{
    int count = 0;
    bunchOfInstances *head, *prev = NULL;

    for (head = *headp; head; prev = head, head = head->next) {
        count = (*headp)->count;

        if (!strcmp(head->instance->instanceId, instance->instanceId)) {
            if (prev) {
                prev->next = head->next;
            } else {
                *headp = head->next;
            }

            if (*headp) {
                (*headp)->count = count - 1;
            }
            EUCA_FREE(head);
            return (EUCA_OK);
        }
    }
    return (EUCA_NOT_FOUND_ERROR);
}

//!
//! Helper to do something on each instance of a given list
//!
//! @param[in] headp a pointer to a pointer to the head node of the list
//! @param[in] function a pointer to the function to execute on each node
//! @param[in] param a transparent pointer to provide to function
//!
//! @return Always return EUCA_OK
//!
int for_each_instance(bunchOfInstances ** headp, void (*function) (bunchOfInstances **, ncInstance *, void *), void *param)
{
    bunchOfInstances *head = NULL;

    for (head = *headp; head; head = head->next) {
        function(headp, head->instance, param);
    }

    return EUCA_OK;
}

//!
//! Finds an instance in a given list
//!
//! @param[in] headp a pointer to the head of the instance list
//! @param[in] instanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return a pointer to the instance if found. Otherwise, NULL is returned.
//!
ncInstance *find_instance(bunchOfInstances ** headp, char *instanceId)
{
    bunchOfInstances *head = NULL;

    for (head = *headp; head; head = head->next) {
        if (!strcmp(head->instance->instanceId, instanceId)) {
            return (head->instance);
        }
    }

    return (NULL);
}

//!
//! Retrieves the next instance in the list
//!
//! @param[in] headp a pointer to the head of the list
//!
//! @return a pointer ot the next instance in the list
//!
ncInstance *get_instance(bunchOfInstances ** headp)
{
    static bunchOfInstances *current = NULL;

    /* advance static variable, wrapping to head if at the end */
    if (current == NULL)
        current = *headp;
    else
        current = current->next;

    /* return the new value, if any */
    if (current == NULL)
        return (NULL);
    return (current->instance);
}

//!
//! Returns the number of instances assigned to a given instance list
//!
//! @param[in] headp a pointer to the head of the list
//!
//! @return number of instances in the list
//!
int total_instances(bunchOfInstances ** headp)
{
    if (*headp)
        return ((*headp)->count);
    return (0);
}

//!
//! Allocate and initialize an instance structure with given information. Resource is
//! used to return information about resources
//!
//! @param[in] nodeStatus the current node status string
//! @param[in] iqn
//! @param[in] memorySizeMax the maximum amount of memory available on this node
//! @param[in] memorySizeAvailable the current amount of memory available on this node
//! @param[in] diskSizeMax the maximum amount of disk space available on this node
//! @param[in] diskSizeAvailable the current amount of disk space available on this node
//! @param[in] numberOfCoresMax the maximum number of cores available on this node
//! @param[in] numberOfCoresAvailable the current number of cores available on this node
//! @param[in] publicSubnets the available public subnet for this node
//!
//! @return a pointer to the newly allocated resource structure or NULL if any error occured.
//!
ncResource *allocate_resource(char *nodeStatus, char *iqn, int memorySizeMax, int memorySizeAvailable, int diskSizeMax, int diskSizeAvailable,
                              int numberOfCoresMax, int numberOfCoresAvailable, char *publicSubnets)
{
    ncResource *res = NULL;

    if (nodeStatus == NULL)
        return (NULL);

    if ((res = EUCA_ZALLOC(1, sizeof(ncResource))) == NULL)
        return (NULL);

    safe_strncpy(res->nodeStatus, nodeStatus, CHAR_BUFFER_SIZE);
    if (iqn)
        safe_strncpy(res->iqn, iqn, CHAR_BUFFER_SIZE);

    if (publicSubnets)
        safe_strncpy(res->publicSubnets, publicSubnets, CHAR_BUFFER_SIZE);

    res->memorySizeMax = memorySizeMax;
    res->memorySizeAvailable = memorySizeAvailable;
    res->diskSizeMax = diskSizeMax;
    res->diskSizeAvailable = diskSizeAvailable;
    res->numberOfCoresMax = numberOfCoresMax;
    res->numberOfCoresAvailable = numberOfCoresAvailable;

    return (res);
}

//!
//! Frees an allocated resource structure.
//!
//! @param[in,out] resp a pointer to the resource structure pointer to free
//!
void free_resource(ncResource ** resp)
{
    if (resp != NULL) {
        EUCA_FREE((*resp));
    }
}

//!
//! Finds a matching volume OR returns a pointer to the next empty/avail volume slot
//! OR if full, returns NULL.
//!
//! @param[in] instance a pointer to the instance structure the volume should be under
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//!
//! @return a pointer to the matching volume OR returns a pointer to the next empty/avail
//! volume slot OR if full, returns NULL.
//!
static ncVolume *find_volume(ncInstance * instance, const char *volumeId)
{
    int i = 0;
    ncVolume *v = instance->volumes;
    ncVolume *match = NULL;
    ncVolume *avail = NULL;
    ncVolume *empty = NULL;

    for (i = 0; i < EUCA_MAX_VOLUMES; i++, v++) {
        // look for matches
        if (!strncmp(v->volumeId, volumeId, CHAR_BUFFER_SIZE)) {
            assert(match == NULL);
            match = v;
        }
        // look for the first empty and available slot
        if (!strnlen(v->volumeId, CHAR_BUFFER_SIZE)) {
            if (empty == NULL)
                empty = v;
        } else if (!is_volume_used(v)) {
            if (avail == NULL)
                avail = v;
        }
    }

    if (match)
        return (match);

    if (empty)
        return (empty);
    return (avail);
}

//
//!
//! Checks wether or not a volume is in use
//!
//! @param[in] v a pointer to the volume to check
//!
//! @return FALSE if volume slot is not in use and TRUE if it is
//!
boolean is_volume_used(const ncVolume * v)
{
    if (strlen(v->stateName) == 0)
        return (FALSE);
    return (strcmp(v->stateName, VOL_STATE_ATTACHING_FAILED) && strcmp(v->stateName, VOL_STATE_DETACHED));
}

//!
//! records volume's information in the instance struct, updating the non-NULL
//! values if the record already exists
//!
//! @param[in] instance
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] remoteDev
//! @param[in] localDev
//! @param[in] localDevReal
//! @param[in] stateName
//!
//! @return a pointer to the volume if found. Otherwize NULL is returned.
//!
ncVolume *save_volume(ncInstance * instance, const char *volumeId, const char *remoteDev, const char *localDev, const char *localDevReal,
                      const char *stateName)
{
    ncVolume *v = NULL;

    assert(instance != NULL);
    assert(volumeId != NULL);

    if ((v = find_volume(instance, volumeId)) != NULL) {
        safe_strncpy(v->volumeId, volumeId, CHAR_BUFFER_SIZE);
        if (remoteDev)
            safe_strncpy(v->remoteDev, remoteDev, CHAR_BUFFER_SIZE);
        if (localDev)
            safe_strncpy(v->localDev, localDev, CHAR_BUFFER_SIZE);
        if (localDevReal)
            safe_strncpy(v->localDevReal, localDevReal, CHAR_BUFFER_SIZE);
        if (stateName)
            safe_strncpy(v->stateName, stateName, CHAR_BUFFER_SIZE);
    }

    return (v);
}

//!
//! Zeroes out the volume's slot in the instance struct (no longer used)
//!
//! @param[in] instance a pointer to the instance to free a volume from
//! @param[in] volumeId the volume identifier string (vol-XXXXXXXX)
//!
//! @return a pointer to the volume structure if found otherwise NULL is returned
//!
ncVolume *free_volume(ncInstance * instance, const char *volumeId)
{
    int slots_left = 0;
    ncVolume *v = NULL;
    ncVolume *last_v = NULL;

    if ((instance == NULL) || (volumeId == NULL))
        return (NULL);

    if ((v = find_volume(instance, volumeId)) == NULL) {
        /* not there (and out of room) */
        return (NULL);
    }

    if (strncmp(v->volumeId, volumeId, CHAR_BUFFER_SIZE)) {
        /* not there */
        return (NULL);

    } else {
        last_v = instance->volumes + (EUCA_MAX_VOLUMES - 1);
        slots_left = last_v - v;

        /* shift the remaining entries up, empty or not */
        if (slots_left)
            memmove(v, v + 1, slots_left * sizeof(ncVolume));

        /* empty the last one */
        bzero(last_v, sizeof(ncVolume));
    }

    return (v);
}

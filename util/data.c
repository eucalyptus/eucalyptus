// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
#include "euca_string.h"

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
const char *hypervisorCapabilityTypeNames[] = {
    "unknown",
    "xen",
    "hw",
    "xen+hw",
    NULL,
};

//! List of string to convert the LIBVIRT device type enumeration
const char *libvirtDevTypeNames[] = {
    "disk",
    "floppy",
    "cdrom",
    NULL,
};

//! List of string to convert the LIBVIRT bus types enumeration
const char *libvirtBusTypeNames[] = {
    "ide",
    "scsi",
    "virtio",
    "xen",
    NULL,
};

//! List of string to convert the LIBVIRT source types enumeration
const char *libvirtSourceTypeNames[] = {
    "file",
    "block",
    NULL,
};

//! List of string to convert the LIBVIRT NIC types enumeration
const char *libvirtNicTypeNames[] = {
    "none",
    "e1000",
    "rtl8139",
    "virtio",
    NULL,
};

//! List of string to convert the NC resource types enumeration
const char *ncResourceTypeNames[] = {
    "image",
    "ramdisk",
    "kernel",
    "ephemeral",
    "swap",
    "ebs",
    "boot",
    NULL,
};

//! List of strings that match ncResourceLocationType enums, for XML encoding
const char *ncResourceLocationTypeNames[] = {
    "url",
    "objectstorage",
    "clc",
    "sc",
    "none",
    NULL,
};

//! List of strings that match ncResourceFormatType enums, for XML encoding
const char *ncResourceFormatTypeNames[] = {
    "none",
    "ext2",
    "ext3",
    "ntfs",
    "swap",
    NULL,
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
    "Teardown",
    NULL,
};

//! String value of each bundling progress state enumeration entry
const char *bundling_progress_names[] = {
    "none",
    "bundling",
    "succeeded",
    "failed",
    "cancelled",
    NULL,
};

//! String value of each create image progress state enumeration entry
const char *createImage_progress_names[] = {
    "none",
    "creating",
    "succeeded",
    "failed",
    "cancelled",
    NULL,
};

//! String value of each migrate-related state enumeration entry
const char *migration_state_names[] = {
    "none",
    "preparing",
    "ready",
    "migrating",
    "cleaning",
    NULL,
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
    NULL,
};

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static ncVolume *find_volume(ncInstance * pInstance, const char *sVolumeId);

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
//! @param[out] pVirtMachineOut a pointer to the resulting virtual machine structure
//! @param[in]  pVirtMachingIn a pointer to the virtual machine structure to duplicate
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @pre Both \p pVirtMachineOut and \p pVirtMachineIn fields must not be NULL
//!
//! @post The \p pVirtMachineOut structure is set with the values from \p pVirtMachineIn
//!
int allocate_virtualMachine(virtualMachine * pVirtMachineOut, const virtualMachine * pVirtMachingIn)
{
    u32 i = 0;
    virtualBootRecord *pVbrOut = NULL;
    const virtualBootRecord *pVbrIn = NULL;

    if ((pVirtMachineOut != NULL) && (pVirtMachingIn != NULL)) {
        //
        // Initialize the outgoing virtual machine with the incoming
        //
        pVirtMachineOut->mem = pVirtMachingIn->mem;
        pVirtMachineOut->disk = pVirtMachingIn->disk;
        pVirtMachineOut->cores = pVirtMachingIn->cores;
        snprintf(pVirtMachineOut->name, 64, "%s", pVirtMachingIn->name);

        //! @todo dan ask dmitrii
        for (i = 0; ((i < EUCA_MAX_VBRS) && (i < pVirtMachingIn->virtualBootRecordLen)); i++) {
            pVbrOut = pVirtMachineOut->virtualBootRecord + i;
            pVbrIn = pVirtMachingIn->virtualBootRecord + i;

            //
            // Initialize the outgoing virtual bood record with the incoming.
            //
            strncpy(pVbrOut->resourceLocation, pVbrIn->resourceLocation, sizeof(pVbrOut->resourceLocation));
            strncpy(pVbrOut->guestDeviceName, pVbrIn->guestDeviceName, sizeof(pVbrOut->guestDeviceName));
            strncpy(pVbrOut->id, pVbrIn->id, sizeof(pVbrOut->id));
            strncpy(pVbrOut->typeName, pVbrIn->typeName, sizeof(pVbrOut->typeName));
            pVbrOut->sizeBytes = pVbrIn->sizeBytes;
            strncpy(pVbrOut->formatName, pVbrIn->formatName, sizeof(pVbrOut->formatName));
        }

        return (EUCA_OK);
    }

    return (EUCA_ERROR);
}

//!
//! Initialize a network configuration structure with given values.
//!
//! @param[out] pNetCfg a pointer to the resulting network configuration structure
//! @param[in]  sPvMac the private MAC string
//! @param[in]  sPvIp the private IP string
//! @param[in]  sPbIp the public IP string
//! @param[in]  vlan the network Virtual LAN
//! @param[in]  networkIndex the network index
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @pre The \p pNetCfg field must not be NULL.
//!
//! @post The network configuration structure is updated with the provided information
//!
int allocate_netConfig(netConfig * pNetCfg, const char *sPvMac, const char *sPvIp, const char *sPbIp, int vlan, int networkIndex)
{
    // make sure our netconfig parameter isn't NULL
    if (pNetCfg != NULL) {
        if (sPvMac)
            euca_strncpy(pNetCfg->privateMac, sPvMac, MAC_BUFFER_SIZE);

        if (sPvIp)
            euca_strncpy(pNetCfg->privateIp, sPvIp, IP_BUFFER_SIZE);

        if (sPbIp)
            euca_strncpy(pNetCfg->publicIp, sPbIp, IP_BUFFER_SIZE);

        pNetCfg->networkIndex = networkIndex;
        pNetCfg->vlan = vlan;
        return (EUCA_OK);
    }

    return (EUCA_ERROR);
}

//!
//! Allocate a metadata structure and initialize it. Metadata is present in every type of nc request
//!
//! @param[in] sCorrelationId the correlation identifier string
//! @param[in] sUserId the user identifier
//!
//! @return a pointer to the newly allocated metadata structure or NULL if any error occured.
//!
//! @see free_metadata()
//!
//! @post A metadata structure is allocated and initialized with the provided information
//!
//! @note Caller is responsible for freeing the allocated memory using free_metadata() call.
//!
ncMetadata *allocate_metadata(const char *sCorrelationId, const char *sUserId)
{
    ncMetadata *pMeta;

    // Try to allocate the structure
    if ((pMeta = EUCA_ZALLOC(1, sizeof(ncMetadata))) == NULL)
        return (NULL);

    //
    // Initialize with the provided information
    //
    pMeta->correlationId = ((sCorrelationId != NULL) ? strdup(sCorrelationId) : NULL);
    pMeta->userId = ((sUserId != NULL) ? strdup(sUserId) : NULL);
    return (pMeta);
}

//!
//! Frees an allocated metadata structure.
//!
//! @param[in,out] ppMeta a pointer to the node controller (NC) metadata structure
//!
//! @see allocate_metadata()
//!
//! @pre The \p ppMeta field should not be NULL
//!
//! @post If the metadata pointer is valid, the structure is freed and \p (*ppMeta) will be set to NULL.
//!
void free_metadata(ncMetadata ** ppMeta)
{
    ncMetadata *pMeta = NULL;
    if (ppMeta != NULL) {
        if ((pMeta = (*ppMeta)) != NULL) {
            EUCA_FREE(pMeta->correlationId);
            EUCA_FREE(pMeta->userId);
            EUCA_FREE(pMeta);
            *ppMeta = NULL;
        }
    }
}

//!
//! Allocate and initialize an instance structure with given information. Instances are
//! present in instance-related requests.
//!
//! @param[in] sUUID the unique user identifier string
//! @param[in] sInstanceId the instance identifier string (i-XXXXXXXX)
//! @param[in] sReservationId the reservation identifier string
//! @param[in] pVirtMachine a pointer to our virtual machine parametes
//! @param[in] sStateName the current instance state name string
//! @param[in] stateCode the current instance state code
//! @param[in] sUserId the user identifier string
//! @param[in] sOwnerId the owner identifier string
//! @param[in] sAccountId the account identifier string
//! @param[in] pNetCfg a pointer to the network configuration of this instance
//! @param[in] sKeyName the SSH key name to use
//! @param[in] sUserData user data string to pass to the instance
//! @param[in] sLaunchIndex the instance's launch index
//! @param[in] sPlatform the instance's platform type
//! @param[in] expiryTime the instance's expiration time before it reaches running
//! @param[in] asGroupNames an array list of group name string
//! @param[in] groupNamesSize the number of group name in the groupNames list
//!
//! @return a pointer to the newly allocated instance structure or NULL if any error occured.
//!
//! @see add_instance()
//! @see free_instance()
//!
//! @post On succes an instance structure is allocated and initialized with the given information.
//!
ncInstance *allocate_instance(const char *sUUID, const char *sInstanceId, const char *sReservationId, virtualMachine * pVirtMachine,
                              const char *sStateName, int stateCode, const char *sUserId, const char *sOwnerId, const char *sAccountId,
                              netConfig * pNetCfg, const char *sKeyName, const char *sUserData, const char *sLaunchIndex, const char *sPlatform,
                              int expiryTime, char **asGroupNames, int groupNamesSize)
{
    u32 i = 0;
    ncInstance *pInstance = NULL;

    /* zeroed out for cleaner-looking checkpoints and strings that are empty unless set */
    if ((pInstance = EUCA_ZALLOC(1, sizeof(ncInstance))) == NULL)
        return (NULL);

    if (sUserData)
        euca_strncpy(pInstance->userData, sUserData, CHAR_BUFFER_SIZE * 32);

    if (sLaunchIndex)
        euca_strncpy(pInstance->launchIndex, sLaunchIndex, CHAR_BUFFER_SIZE);

    if (sPlatform)
        euca_strncpy(pInstance->platform, sPlatform, CHAR_BUFFER_SIZE);

    pInstance->groupNamesSize = groupNamesSize;
    if ((asGroupNames != NULL) && (groupNamesSize > 0)) {
        for (i = 0; i < groupNamesSize && asGroupNames[i]; i++)
            euca_strncpy(pInstance->groupNames[i], asGroupNames[i], CHAR_BUFFER_SIZE);
    }

    if (pNetCfg != NULL)
        memcpy(&(pInstance->ncnet), pNetCfg, sizeof(netConfig));

    if (sUUID)
        euca_strncpy(pInstance->uuid, sUUID, CHAR_BUFFER_SIZE);

    if (sInstanceId)
        euca_strncpy(pInstance->instanceId, sInstanceId, CHAR_BUFFER_SIZE);

    if (sKeyName)
        euca_strncpy(pInstance->keyName, sKeyName, CHAR_BUFFER_SIZE * 4);

    if (sReservationId)
        euca_strncpy(pInstance->reservationId, sReservationId, CHAR_BUFFER_SIZE);

    if (sStateName)
        euca_strncpy(pInstance->stateName, sStateName, CHAR_BUFFER_SIZE);

    if (sUserId)
        euca_strncpy(pInstance->userId, sUserId, CHAR_BUFFER_SIZE);

    if (sOwnerId)
        euca_strncpy(pInstance->ownerId, sOwnerId, CHAR_BUFFER_SIZE);

    if (sAccountId)
        euca_strncpy(pInstance->accountId, sAccountId, CHAR_BUFFER_SIZE);

    if (pVirtMachine)
        memcpy(&(pInstance->params), pVirtMachine, sizeof(virtualMachine));

    pInstance->stateCode = stateCode;
    euca_strncpy(pInstance->bundleTaskStateName, bundling_progress_names[NOT_BUNDLING], CHAR_BUFFER_SIZE);
    pInstance->expiryTime = expiryTime;
    return (pInstance);
}

//!
//! Clones an existing instance structure
//!
//! @param[in] old_instance a pointer to the instance to duplicate
//!
//! @return A clone of the existing instance of NULL on failure
//!
//! @see free_instance(), allocate_instance()
//!
//! @pre The \p old_instance field must not be NULL
//!
//! @post A clone of our existing instance is created.
//!
//! @note The caller is responsible to free the allocated instance using the free_instance() API.
//!
ncInstance *clone_instance(const ncInstance * old_instance)
{
    ncInstance *new_instance;

    // zeroed out for cleaner-looking checkpoints and strings that are empty unless set
    if ((new_instance = EUCA_ZALLOC(1, sizeof(ncInstance))) == NULL)
        return (NULL);

    //! @TODO do not just copy everything
    memcpy(new_instance, old_instance, sizeof(ncInstance));

    return new_instance;
}

//!
//! Frees an allocated instance structure.
//!
//! @param[in,out] ppInstance a pointer to the instance structure pointer to free
//!
//! @see remove_instance()
//! @see allocate_instance()
//!
//! @pre \li The \p ppInstance field should not be NULL
//!      \li The instance should have been removed from any list using remove_instance()
//!
//! @post The instance is freed and the value pointed by \p ppInstance is set to NULL.
//!
void free_instance(ncInstance ** ppInstance)
{
    if (ppInstance != NULL) {
        EUCA_FREE((*ppInstance));
    }
}

//!
//! Adds an instance to an instance linked list
//!
//! @param[in,out] ppHead a pointer to the pointer to the head of the list
//! @param[in]     pInstance a pointer to the instance to add to the list
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_MEMORY_ERROR: if we fail to allocate memory
//!         \li EUCA_INVALID_ERROR: if any of our parameter does not meet the pre-condition
//!         \li EUCA_DUPLICATE_ERROR: if the instance is already part of this list
//!
//! @pre \li Both \p ppHead and \p pInstance field must not be NULL.
//!      \li The instance must not be part of the list
//!
//! @post The instance is added to the list. If this is the first instance in the list,
//!       the \p ppHead value is updated to point to this instance.
//!
int add_instance(bunchOfInstances ** ppHead, ncInstance * pInstance)
{
    bunchOfInstances *pNew = NULL;
    bunchOfInstances *pLast = NULL;
    bunchOfInstances *pNext = NULL;

    // Make sure our paramters are valid
    if ((ppHead == NULL) || (pInstance == NULL))
        return (EUCA_INVALID_ERROR);

    // Try to allocate memory for our instance list node
    if ((pNew = EUCA_ZALLOC(1, sizeof(bunchOfInstances))) == NULL)
        return (EUCA_MEMORY_ERROR);

    // Initialize our node
    pNew->instance = pInstance;
    pNew->next = NULL;

    // Are we the first item in this list?
    if (*ppHead == NULL) {
        *ppHead = pNew;
        (*ppHead)->count = 1;
    } else {
        pNext = *ppHead;

        //
        // Process the list to make sure we're not trying to add a duplicate
        //
        do {
            pLast = pNext;
            if (!strcmp(pLast->instance->instanceId, pInstance->instanceId)) {
                EUCA_FREE(pNew);
                return (EUCA_DUPLICATE_ERROR);
            }
            pNext = pLast->next;
        } while (pLast->next);

        // We're at the end so add it there.
        pLast->next = pNew;
        (*ppHead)->count++;
    }

    return (EUCA_OK);
}

//!
//! Removes an instance from an instance linked list
//!
//! @param[in,out] ppHead a pointer to the pointer to the head of the list
//! @param[in]     pInstance a pointer to the instance to remove from the list
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any of our parameters do not meet the pre-conditions
//!         \li EUCA_NOT_FOUND_ERROR: if the instance is not part of this list
//!
//! @pre \li Both \p ppHead and \p pInstance field must not be NULL
//!      \li The instance must exist in this list
//!
//! @post The instance is removed from the list. If this instance was the head of the list,
//!       the \p ppHead field will be updated to point to the new head (next instance in list
//!       from previous head).
//!
int remove_instance(bunchOfInstances ** ppHead, ncInstance * pInstance)
{
    u32 count = 0;
    bunchOfInstances *pHead = NULL;
    bunchOfInstances *pPrevious = NULL;

    // Make sure our parameters are valid
    if (ppHead && pInstance) {
        for (pHead = *ppHead; pHead; pPrevious = pHead, pHead = pHead->next) {
            count = (*ppHead)->count;

            if (!strcmp(pHead->instance->instanceId, pInstance->instanceId)) {
                if (pPrevious) {
                    pPrevious->next = pHead->next;
                } else {
                    *ppHead = pHead->next;
                }

                if (*ppHead) {
                    (*ppHead)->count = count - 1;
                }
                EUCA_FREE(pHead);
                return (EUCA_OK);
            }
        }
        return (EUCA_NOT_FOUND_ERROR);
    }
    return (EUCA_INVALID_ERROR);
}

//!
//! Helper to do something on each instance of a given list
//!
//! @param[in] ppHead a pointer to the pointer to the head of the list
//! @param[in] pFunction a pointer to the function to execute on each node
//! @param[in] pParam a transparent pointer to provide to pFunction
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any of our parameters do not meet the pre-conditions
//!
//! @pre Both \p ppHead and \p pFunction fields must not be NULL
//!
//! @post The function \p pFunction is applied to each member of the instance list.
//!
int for_each_instance(bunchOfInstances ** ppHead, void (*pFunction) (bunchOfInstances **, ncInstance *, void *), void *pParam)
{
    bunchOfInstances *pHead = NULL;

    // Make sure our parameters aren't NULL
    if (ppHead && pFunction) {
        for (pHead = *ppHead; pHead; pHead = pHead->next) {
            pFunction(ppHead, pHead->instance, pParam);
        }

        return (EUCA_OK);
    }
    return (EUCA_INVALID_ERROR);
}

//!
//! Finds an instance in a given list based on the given instance identifier
//!
//! @param[in] ppHead a pointer to the pointer to the head of the list
//! @param[in] sInstanceId the instance identifier string (i-XXXXXXXX)
//!
//! @return a pointer to the instance if found. Otherwise, NULL is returned.
//!
//! @pre Both \p ppHead and \p sInstanceId must not be NULL.
//!
ncInstance *find_instance(bunchOfInstances ** ppHead, const char *sInstanceId)
{
    bunchOfInstances *pHead = NULL;

    // Make sure our parameters aren't NULL
    if (ppHead && sInstanceId) {
        for (pHead = *ppHead; pHead; pHead = pHead->next) {
            if (!strcmp(pHead->instance->instanceId, sInstanceId)) {
                return (pHead->instance);
            }
        }
    }
    return (NULL);
}

//!
//! Retrieves the next instance in the list
//!
//! @param[in] ppHead a pointer to the pointer to the head of the list
//!
//! @return a pointer ot the next instance in the list or NULL if no list is set
//!
//! @pre The \p ppHead field must not be NULL if the static list pointer is NULL.
//!
ncInstance *get_instance(bunchOfInstances ** ppHead)
{
    static bunchOfInstances *pCurrent = NULL;

    // advance static variable, wrapping to head if at the end
    if (pCurrent == NULL)
        pCurrent = ((ppHead == NULL) ? NULL : (*ppHead));
    else
        pCurrent = pCurrent->next;

    // return the new value, if any
    if (pCurrent == NULL)
        return (NULL);
    return (pCurrent->instance);
}

//!
//! Returns the number of instances assigned to a given instance list
//!
//! @param[in] ppHead a pointer to the pointer to the head of the list
//!
//! @return number of instances in the list. If \p ppHead is NULL or \p (*ppHead) is NULL, 0 will be returned.
//!
//! @pre The \p ppHead field must not be NULL
//!
int total_instances(bunchOfInstances ** ppHead)
{
    if (ppHead) {
        if (*ppHead)
            return ((*ppHead)->count);
    }
    return (0);
}

//!
//! Allocate and initialize a resource structure with given information. Resource is
//! used to return information about resources
//!
//! @param[in] sNodeStatus the current node status string
//! @param[in] migrationCapable flag indicating whether node can participate in live migration
//! @param[in] sIQN
//! @param[in] memorySizeMax the maximum amount of memory available on this node
//! @param[in] memorySizeAvailable the current amount of memory available on this node
//! @param[in] diskSizeMax the maximum amount of disk space available on this node
//! @param[in] diskSizeAvailable the current amount of disk space available on this node
//! @param[in] numberOfCoresMax the maximum number of cores available on this node
//! @param[in] numberOfCoresAvailable the current number of cores available on this node
//! @param[in] sPublicSubnets the available public subnet for this node
//!
//! @return a pointer to the newly allocated resource structure or NULL if any error occured.
//!
//! @see free_resource()
//!
//! @pre The \p sNodeStatus field must not be NULL.
//!
//! @post On success, a resource structure is allocated and initialized with the given information
//!
//! @note Caller is responsible to free the allocated memory using the free_resource() function call.
//!
ncResource *allocate_resource(const char *sNodeStatus, boolean migrationCapable, const char *sIQN, int memorySizeMax, int memorySizeAvailable, int diskSizeMax,
                              int diskSizeAvailable, int numberOfCoresMax, int numberOfCoresAvailable, const char *sPublicSubnets)
{
    ncResource *pResource = NULL;

    // Make sure we have a valid parameter
    if (sNodeStatus == NULL)
        return (NULL);

    // See if we can allocate our resource structure
    if ((pResource = EUCA_ZALLOC(1, sizeof(ncResource))) == NULL)
        return (NULL);

    //
    // Initialize the structure with the given values
    //
    euca_strncpy(pResource->nodeStatus, sNodeStatus, CHAR_BUFFER_SIZE);
    if (sIQN)
        euca_strncpy(pResource->iqn, sIQN, CHAR_BUFFER_SIZE);
    pResource->migrationCapable = migrationCapable;

    if (sPublicSubnets)
        euca_strncpy(pResource->publicSubnets, sPublicSubnets, CHAR_BUFFER_SIZE);

    pResource->memorySizeMax = memorySizeMax;
    pResource->memorySizeAvailable = memorySizeAvailable;
    pResource->diskSizeMax = diskSizeMax;
    pResource->diskSizeAvailable = diskSizeAvailable;
    pResource->numberOfCoresMax = numberOfCoresMax;
    pResource->numberOfCoresAvailable = numberOfCoresAvailable;
    return (pResource);
}

//!
//! Frees an allocated resource structure.
//!
//! @param[in,out] ppResource a pointer to the resource structure pointer to free
//!
//! @see allocate_resource()
//!
//! @pre The \p ppResource field should not be NULL
//!
//! @post The resource will be freed and \p (*ppResource) will be set to NULL.
//!
void free_resource(ncResource ** ppResource)
{
    if (ppResource != NULL) {
        EUCA_FREE((*ppResource));
    }
}

//!
//! Finds a matching volume OR returns a pointer to the next empty/avail volume slot
//! OR if full, returns NULL.
//!
//! @param[in] pInstance a pointer to the instance structure the volume should be under
//! @param[in] sVolumeId the volume identifier string (vol-XXXXXXXX)
//!
//! @return a pointer to the matching volume OR returns a pointer to the next empty/avail
//!         volume slot OR if full, returns NULL.
//!
//! @pre Both \p pInstance and \p sVolumeId fields must not be NULL
//!
//! @todo There's gotta be a way to improve and not scan the whole list all the time
//!
static ncVolume *find_volume(ncInstance * pInstance, const char *sVolumeId)
{
    ncVolume *pVol = NULL;
    ncVolume *pMatch = NULL;
    ncVolume *pAvail = NULL;
    ncVolume *pEmpty = NULL;
    register u32 i = 0;

    // Make sure our given parameters aren't NULL
    if ((pInstance != NULL) && (sVolumeId != NULL)) {
        for (i = 0, pVol = pInstance->volumes; i < EUCA_MAX_VOLUMES; i++, pVol++) {
            // look for matches
            if (!strncmp(pVol->volumeId, sVolumeId, CHAR_BUFFER_SIZE)) {
                assert(pMatch == NULL);
                pMatch = pVol;
            }
            // look for the first empty and available slot
            if (!strnlen(pVol->volumeId, CHAR_BUFFER_SIZE)) {
                if (pEmpty == NULL)
                    pEmpty = pVol;
            } else if (!is_volume_used(pVol)) {
                if (pAvail == NULL)
                    pAvail = pVol;
            }
        }

        // Return match first if any are found
        if (pMatch)
            return (pMatch);

        // then return the empty slot
        if (pEmpty)
            return (pEmpty);

        // If nothing else, return the first available slot.
        return (pAvail);
    }

    return (NULL);
}

//
//!
//! Checks wether or not a volume is in use
//!
//! @param[in] pVolume a pointer to the volume to validate
//!
//! @return FALSE if volume slot is not in use or if NULL and TRUE if it is in use
//!
//! @pre The \p pVol field must not be NULL.
//!
boolean is_volume_used(const ncVolume * pVolume)
{
    if (pVolume != NULL) {
        if (strlen(pVolume->stateName) == 0)
            return (FALSE);
        return (strcmp(pVolume->stateName, VOL_STATE_ATTACHING_FAILED) && strcmp(pVolume->stateName, VOL_STATE_DETACHED));
    }
    return (FALSE);
}

//!
//! Records volume's information in the instance struct, updating the non-NULL values if the record
//! already exists
//!
//! @param[in] pInstance a pointer to our instance containing the volume information to save
//! @param[in] sVolumeId the volume identifier string (vol-XXXXXXXX)
//! @param[in] sVolumeAttachmentToken the attachment token associated with this volume and attachment
//! @param[in] sConnectionString the connection string info specific to this host's volume attachment
//! @param[in] sLocalDev the local device name
//! @param[in] sLocalDevReal the local real device name
//! @param[in] sStateName the current volume state name
//!
//! @return a pointer to the volume if found. Otherwise NULL is returned.
//!
//! @pre \li Both \p pInstance and \p sVolumeId fields must not be NULL
//!      \li A volume with \p sVolumeId for \p pInstance should exists
//!      \li If such volume does not exists, we must have an empty slot in the volume list
//!
//! @post \li If any of \p pInstance or \p sVolumeId is NULL, the application will throw a SIGABRT signal
//!       \li If the volume is found or if we have an empty slot, the volume information will be saved
//!       \li If the volume is not found and if we do not have empty slot, NULL is returned and nothing is saved
//!
ncVolume *save_volume(ncInstance * pInstance, const char *sVolumeId, const char *sVolumeAttachmentToken, const char *sConnectionString, const char *sLocalDev,
                      const char *sLocalDevReal, const char *sStateName)
{
    ncVolume *pVol = NULL;

    // Make sure pInstance and sVolumeId aren't NULL
    assert(pInstance != NULL);
    assert(sVolumeId != NULL);

    // Lookup for our device
    if ((pVol = find_volume(pInstance, sVolumeId)) != NULL) {
        //
        // Save our volume information
        //
        euca_strncpy(pVol->volumeId, sVolumeId, CHAR_BUFFER_SIZE);

        if (sVolumeAttachmentToken)
            euca_strncpy(pVol->attachmentToken, sVolumeAttachmentToken, CHAR_BUFFER_SIZE);

        if (sConnectionString)
            euca_strncpy(pVol->connectionString, sConnectionString, VERY_BIG_CHAR_BUFFER_SIZE);

        if (sLocalDev)
            euca_strncpy(pVol->localDev, sLocalDev, CHAR_BUFFER_SIZE);

        if (sLocalDevReal)
            euca_strncpy(pVol->localDevReal, sLocalDevReal, CHAR_BUFFER_SIZE);

        if (sStateName)
            euca_strncpy(pVol->stateName, sStateName, CHAR_BUFFER_SIZE);
    }

    return (pVol);
}

//!
//! Zeroes out the volume's slot in the instance struct (no longer used)
//!
//! @param[in] pInstance a pointer to the instance to free a volume from
//! @param[in] sVolumeId the volume identifier string (vol-XXXXXXXX)
//!
//! @return a pointer to the volume structure if found otherwise NULL is returned
//!
//! @pre \li Both the \p pInstance and \p sVolumeId fields must not be NULL.
//!      \li The volume specified by \p sVolumeId must exists
//!
//! @post On success, the volume entry is erased from the instance volume list
//!
ncVolume *free_volume(ncInstance * pInstance, const char *sVolumeId)
{
    int slotsLeft = 0;
    ncVolume *pVol = NULL;
    ncVolume *pLastVol = NULL;

    // Make sure our given parameters are valid
    if ((pInstance == NULL) || (sVolumeId == NULL))
        return (NULL);

    // Check if this volume exists in our volume list
    if ((pVol = find_volume(pInstance, sVolumeId)) == NULL) {
        return (NULL);
    }
    // Make sure this is the volume we're looking for
    if (strncmp(pVol->volumeId, sVolumeId, CHAR_BUFFER_SIZE)) {
        return (NULL);
    }

    pLastVol = pInstance->volumes + (EUCA_MAX_VOLUMES - 1);
    slotsLeft = pLastVol - pVol;

    /* shift the remaining entries up, empty or not */
    if (slotsLeft)
        memmove(pVol, (pVol + 1), (slotsLeft * sizeof(ncVolume)));

    /* empty the last one */
    bzero(pLastVol, sizeof(ncVolume));
    return (pVol);
}

//!
//! Allocate a new bundle task for the given instance
//!
//! @param[in] pInstance pointer to the instance we're creating a bundling task for
//!
//! @return A newly allocated pointer to the bundle task structure if successful or NULL if
//!         the given pInstance structure is NULL or if we cannot allocate memory.
//!
//! @pre The \p pInstance field must not be NULL
//!
//! @post A newly allocated structure is allocated and initialized.
//!
//! @note The caller is responsible for freeing the allocated memory
//!
bundleTask *allocate_bundleTask(ncInstance * pInstance)
{
    bundleTask *pBundle = NULL;

    // Make sure out given parameter is valid
    if (pInstance != NULL) {
        if ((pBundle = EUCA_ZALLOC(1, sizeof(bundleTask))) == NULL) {
            LOGERROR("out of memory\n");
            return (NULL);
        }
        // initialize our newly allocated structure.
        snprintf(pBundle->instanceId, CHAR_BUFFER_SIZE, "%s", pInstance->instanceId);
        snprintf(pBundle->state, CHAR_BUFFER_SIZE, "%s", pInstance->bundleTaskStateName);
        return (pBundle);
    }

    return (NULL);
}

static int get_str_index(const char **array, const char *str)
{
    assert(array);

    if (str != NULL) {
        // scan array of strings looking for a match
        for (int i = 0; array[i] != NULL; i++) {
            if (!strcmp(array[i], str)) {
                return (i);
            }
        }
    }

    return (-1);
}

//!
//!
//!
//! @param[in] instance_state_name
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
instance_states instance_state_from_string(const char *instance_state_name)
{
    return (instance_states) get_str_index(instance_state_names, instance_state_name);
}

//!
//!
//!
//! @param[in] bundling_progress_name
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
bundling_progress bundling_progress_from_string(const char *bundling_progress_name)
{
    return (bundling_progress) get_str_index(bundling_progress_names, bundling_progress_name);
}

//!
//!
//!
//! @param[in] createImage_progress_name
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
createImage_progress createImage_progress_from_string(const char *createImage_progress_name)
{
    return (createImage_progress) get_str_index(createImage_progress_names, createImage_progress_name);
}

//!
//!
//!
//! @param[in] migration_state_name
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
migration_states migration_state_from_string(const char *migration_state_name)
{
    return (migration_states) get_str_index(migration_state_names, migration_state_name);
}

//!
//!
//!
//! @param[in] str
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
hypervisorCapabilityType hypervisorCapabilityType_from_string(const char *str)
{
    return (hypervisorCapabilityType) get_str_index(hypervisorCapabilityTypeNames, str);
}

//!
//!
//!
//! @param[in] str
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
ncResourceType ncResourceType_from_string(const char *str)
{
    return (ncResourceType) get_str_index(ncResourceTypeNames, str);
}

//!
//!
//!
//! @param[in] str
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
ncResourceLocationType ncResourceLocationType_from_string(const char *str)
{
    return (ncResourceLocationType) get_str_index(ncResourceLocationTypeNames, str);
}

//!
//!
//!
//! @param[in] str
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
ncResourceFormatType ncResourceFormatType_from_string(const char *str)
{
    return (ncResourceFormatType) get_str_index(ncResourceFormatTypeNames, str);
}

//!
//!
//!
//! @param[in] str
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
libvirtDevType libvirtDevType_from_string(const char *str)
{
    return (libvirtDevType) get_str_index(libvirtDevTypeNames, str);
}

//!
//!
//!
//! @param[in] str
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
libvirtBusType libvirtBusType_from_string(const char *str)
{
    return (libvirtBusType) get_str_index(libvirtBusTypeNames, str);
}

//!
//!
//!
//! @param[in] str
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
libvirtSourceType libvirtSourceType_from_string(const char *str)
{
    return (libvirtSourceType) get_str_index(libvirtSourceTypeNames, str);
}

//!
//!
//!
//! @param[in] str
//!
//! @return result of get_str_index()
//!
//! @see get_str_index()
//!
libvirtNicType libvirtNicType_from_string(const char *str)
{
    return (libvirtNicType) get_str_index(libvirtNicTypeNames, str);
}

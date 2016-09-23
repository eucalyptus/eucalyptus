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
//! @file net/dev_handler.c
//! Implements a network device handling API. Anything that relates to network devices
//! IP addresses, Ethernet addresses, VLANs, etc. should be covered in this API. All
//! function names MUST start with the 'dev_' string and be properly documented.
//!
//! Coding Standard:
//! Every function that has multiple words must follow the word1_word2_word3() naming
//! convention and variables must follow the 'word1Word2Word3()' convention were no
//! underscore is used and every word, except for the first one, starts with a capitalized
//! letter. Whenever possible (not mendatory but strongly encouraged), prefixing a variable
//! name with one or more of the following qualifier would help reading code:
//!     - p - indicates a variable is a pointer (example: int *pAnIntegerPointer)
//!     - s - indicates a string variable (examples: char sThisString[10], char *psAnotherString). When 's' is used on its own, this mean a static string.
//!     - a - indicates an array of objects (example: int aAnArrayOfInteger[10])
//!     - g - indicates a variable with global scope to the file or application (example: static eucanetdConfig gConfig)
//!
//! Any other function implemented must have its name start with "dev" followed by an underscore
//! and the rest of the function name with every words separated with an underscore character. For
//! example: dev_this_is_a_good_function_name().
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pwd.h>
#include <math.h>
#include <config.h>
#include <dirent.h>
#include <errno.h>
#include <netdb.h>
#include <net/if.h>
#include <net/ethernet.h>
#include <netinet/if_ether.h>
#include <netinet/ip.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <ifaddrs.h>
#include <linux/if_link.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <euca_network.h>
#include <log.h>
#include <atomic_file.h>

#include "dev_handler.h"
#include "euca_gni.h"
#include "eucanetd.h"
#include "eucanetd_util.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define BRCTL_PATH                               "/usr/sbin/brctl"
#define VCONFIG_PATH                             "/sbin/vconfig"

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

//! Array of string mapping to the device types enumeration
const char *asDevTypeNames[] = {
    "INVALID",
    "INTERFACE",
    "BRIDGE",
    "TUNNEL",
    "ANY",
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

//! API to validate if a given VLAN is valid (i.e. between 0 and 4095)
static inline boolean dev_is_vlan_valid(u16 vlan);

//! API to force remove a bridge device
static int dev_remove_bridge_forced(dev_handler *devh, const char *psBridgeName);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to validate if a VLAN is valid
#define IS_VLAN_VALID(_vlan)                     dev_is_vlan_valid((_vlan))

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/**
 * Initialize the IP Set handler structure
 *
 * @param devh [in] pointer to the device handler structure
 * @param cmdprefix [in] a string pointer to the prefix to use to run commands
 *
 * @return 0 on success. 1 on any failure.
 *
 * @pre
 *    - The devh pointer should not be NULL
 *     - We should be able to create temporary files on the system
 *
 * @post
 *     - If cmdprefix was provided, the table's cmdprefix field will be set with it
 *
 */
int dev_handler_init(dev_handler *devh, const char *cmdprefix) {
    if (!devh) {
        LOGERROR("invalid argument: cannot initialize NULL dev_handler\n");
        return (1);
    }

    memset(devh, 0, sizeof(dev_handler));

    if (cmdprefix) {
        snprintf(devh->cmdprefix, EUCA_MAX_PATH, "%s", cmdprefix);
    } else {
        devh->cmdprefix[0] = '\0';
    }
    
    devh->init = 1;
    return (0);
}

/**
 * Release resources of the given device handler and reinitializes the handler.
 * @param devh [in] pointer to the device handler
 * @return 0 on success. 1 on failure.
 */
int dev_handler_free(dev_handler *devh) {
    char saved_cmdprefix[EUCA_MAX_PATH] = "";

    if (!devh || !devh->init) {
        return (1);
    }
    snprintf(saved_cmdprefix, EUCA_MAX_PATH, "%s", devh->cmdprefix);

    devh->numberOfDevices = 0;
    devh->numberOfNetworks = 0;
    EUCA_FREE(devh->pDevices);
    EUCA_FREE(devh->pNetworks);

    return (dev_handler_init(devh, saved_cmdprefix));
}

/**
 * Releases all resources of the given dev_handler.
 * @param devh [in] pointer to the device handler
 * @return 0 on success. 1 on failure.
 */
int dev_handler_close(dev_handler *devh) {
    if (!devh || !devh->init) {
        LOGDEBUG("Invalid argument. NULL or uninitialized dev_handler.\n");
        return (1);
    }
    EUCA_FREE(devh->pDevices);
    EUCA_FREE(devh->pNetworks);
    memset(devh, 0, sizeof (dev_handler));
    return (0);
}

/**
 * Retrieves the current device state from the system.
 * @param devh [in] pointer to device handler
 * @return 0 on success. 1 on failure.
 */
int dev_handler_repopulate(dev_handler *devh) {
    int rc = 0;
    struct timeval tv = { 0 };

    eucanetd_timer_usec(&tv);
    if (!devh || !devh->init) {
        return (1);
    }

    rc = dev_handler_free(devh);
    if (rc) {
        LOGERROR("could not reinitialize dev handler.\n");
        return (1);
    }
    // Retrieve our system network device information
    if ((rc = dev_get_list(devh, NULL, &devh->pDevices, &devh->numberOfDevices)) != 0) {
        LOGERROR("Cannot retrieve system network device information.\n");
        dev_handler_free(devh);
        return (1);
    }
    // Retrieve our system network device information
    if ((rc = dev_get_ips(NULL, &devh->pNetworks, &devh->numberOfNetworks)) != 0) {
        LOGERROR("Cannot retrieve system network information.\n");
        dev_handler_free(devh);
        return (1);
    }

    LOGDEBUG("devices populated in %.2f ms.\n", eucanetd_timer_usec(&tv) / 1000.0);
    return (0);
}

/**
 * Retrieves a list of devices that support IP traffic. The caller can filter using the
 * cpsSearch parameter. If cpsSearch is set to NULL or "*", the list isn't filtered. If
 * cpsSearch is terminated with a "*" character, than any device name that starts with
 * the string prepending the "*" will be in the final list and if there are no "*", then
 * the list will only contain the interface with the given name.
 *
 * @param devh [in] pointer to the device handler
 * @param cpsSearch [in] a constant string pointer to the filter. (see description)
 * @param pDevices [in,out] a pointer to a list of string that will contain the device names
 * @param pNbDevices [in,out] a pointer to the integer indicating how many devices we found
 * @param deviceType [in] the device type to filter on. The values are define in the dev_type_t enum.
 *
 * @return 0 on success or 1 on failure
 *
 * @see dev_free_list()
 *
 * @pre
 *     Both ppsDevNames and pNumberOfDevices MUST not be NULL.
 *
 * @post
 *     On successful completion, ppsDevNames contains the list of device names found on the system for
 *     the given search criterias and pNumberOfDevices contains the number of elements in the list. On
 *     failure, both pNumberOfDevices and ppsDevNames are non-deterministic.
 *
 * @note
 *     Caller is responsible to free the dynamically allocated list of device name entries
 *     using the dev_free_list() API.
 */
int dev_get(dev_handler *devh, const char *cpsSearch, dev_entry **pDevices, int *pNbDevices, dev_type deviceType) {
    int i = 0;
    dev_entry *pPtr = NULL;
    boolean found = FALSE;
    struct ifaddrs *pIfa = NULL;
    struct ifaddrs *pIfAddr = NULL;

    // Make sure we have the proper pointers.
    if ((pDevices == NULL) || (pNbDevices == NULL)) {
        return (1);
    }
    // Set the list and number of items to NULL and 0
    (*pDevices) = NULL;
    (*pNbDevices) = 0;

    // get the list of network devices
    if (getifaddrs(&pIfAddr) == -1) {
        LOGERROR("Failed to retrieve the list of network devices.\n");
        return (1);
    }
    // Scan the list for AF_INET devices type
    for (pIfa = pIfAddr; pIfa != NULL; pIfa = pIfa->ifa_next) {
        // Validate the ifaddr
        if (pIfa->ifa_addr == NULL)
            continue;

        // Only IP devices
        if (pIfa->ifa_addr->sa_family != AF_PACKET)
            continue;

        // Check if we need to filter this name
        if ((cpsSearch != NULL) && strcmp(cpsSearch, "*")) {
            // Is this an exact match?
            if (cpsSearch[strlen(cpsSearch) - 1] == '*') {
                // if the name does not start with the search name then skip
                if (strncmp(pIfa->ifa_name, cpsSearch, (strlen(cpsSearch) - 1))) {
                    continue;
                }
            } else {
                // if the name does not match the search name then skip
                if (strcmp(pIfa->ifa_name, cpsSearch)) {
                    continue;
                }
            }
        }
        // Check if we already have this name in the list
        for (i = 0, found = FALSE; ((i < (*pNbDevices)) && !found); i++) {
            if (!strcmp((*pDevices)[i].sDevName, pIfa->ifa_name)) {
                found = TRUE;
            }
        }

        // Did we find it?
        if (found)
            continue;

        // Do we have to filter on type?
        if (deviceType != DEV_TYPE_ANY) {
            if (deviceType == DEV_TYPE_BRIDGE) {
                // Skip if this is not a bridge device
                if (!dev_is_bridge(pIfa->ifa_name))
                    continue;
            } else if (deviceType == DEV_TYPE_TUNNEL) {
                // Skip if this is not a tunnel device
                if (!dev_is_tunnel(pIfa->ifa_name))
                    continue;
            } else if (deviceType == DEV_TYPE_INTERFACE) {
                // Skip if we are a bridge or a tunnel device
                if (dev_is_bridge(pIfa->ifa_name) || dev_is_tunnel(pIfa->ifa_name))
                    continue;
            }
        }
        // Alright, new one, allocate some memory
        if ((pPtr = EUCA_REALLOC((*pDevices), ((*pNbDevices) + 1), sizeof(dev_entry))) == NULL) {
            LOGERROR("Memory allocation failure.\n");
            dev_free_list(pDevices, (*pNbDevices));
            (*pNbDevices) = 0;
            freeifaddrs(pIfAddr);
            return (1);
        }
        // re-adjust out pointers
        (*pDevices) = pPtr;

        // Store the information we just got
        snprintf((*pDevices)[(*pNbDevices)].sDevName, IF_NAME_LEN, "%s", pIfa->ifa_name);
        snprintf((*pDevices)[(*pNbDevices)].sMacAddress, ENET_ADDR_LEN, "%s", dev_get_mac(pIfa->ifa_name));
        (*pDevices)[(*pNbDevices)].isBridge = dev_is_bridge(pIfa->ifa_name);
        (*pNbDevices)++;
    }

    freeifaddrs(pIfAddr);
    return (0);
}

/**
 * Checks whether or not a device exists.
 *
 * @param psDeviceName [in] a string pointer to the device name we are checking
 *
 * @return TRUE if the device exists otherwise FALSE is returned
 *
 * @pre
 *     psDeviceName MUST not be NULL and not empty
 */
boolean dev_exist(const char *psDeviceName) {
#define MAX_PATH_LEN              64

    char sPath[MAX_PATH_LEN] = "";

    // Make sure our given parameter is not NULL
    if (!psDeviceName || (psDeviceName[0] == '\0'))
        return (FALSE);

    // Each device has its path under /sys/class/net/[device]/
    snprintf(sPath, MAX_PATH_LEN, "/sys/class/net/%s/", psDeviceName);

    // If the path is a directory, than its a valid device.
    if (check_directory(sPath))
        return (FALSE);
    return (TRUE);

#undef MAX_PATH_LEN
}

/**
 * Checks wether or not a given device is currently UP
 *
 * @param psDeviceName [in] a string pointer to the device name we are checking
 *
 * @return the return value description
 *
 * @see euca_strncpy(), euca_strdupcat()
 *
 * @pre
 *     - psDeviceName MUST not be NULL
 *     - psDeviceName must be a valid device
 */
boolean dev_is_up(const char *psDeviceName) {
#define MAX_PATH_LEN                 64
#define OPERATING_STATE_LEN          32

    char *p = NULL;
    char sPath[EUCA_MAX_PATH] = "";
    char sOperState[OPERATING_STATE_LEN] = "";
    FILE *pFh = NULL;
    boolean ret = FALSE;

    // Make sure the given string isn't NULL
    if (!psDeviceName)
        return (FALSE);

    // Each device has its path under /sys/class/net/[device]/
    snprintf(sPath, MAX_PATH_LEN, "/sys/class/net/%s/operstate", psDeviceName);

    // Open the operstate net file... If the device is invalid and does not exists
    // then this will fail
    if ((pFh = fopen(sPath, "r")) == NULL)
        return (FALSE);

    // Read the first line. We should get either up or down
    if (fgets(sOperState, OPERATING_STATE_LEN, pFh)) {
        // remove the '\n' character.
        if ((p = strchr(sOperState, '\n')) != NULL)
            *p = '\0';

        // Does it say down? Up can also be considered unknown for bridge
        if (strncmp(sOperState, "down", OPERATING_STATE_LEN)) {
            ret = TRUE;
        }
    }

    fclose(pFh);
    return (ret);

#undef MAX_PATH_LEN
#undef OPERATING_STATE_LEN
}

/**
 * Enables a network device.
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a string pointer to the device name to enable
 *
 * @return 0 on success or 1 if any failure occured
 *
 * @see dev_down(), dev_is_up()
 *
 * @pre
 *     The newtork device name must be valid and the network device must exists on this system
 *
 * @post
 *     On success, the device is enabled. If any failure occured, the device state will
 *     remain unchanged.
 */
int dev_up(dev_handler *devh, const char *psDeviceName) {
    int rc = 0;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure we have a valid device
    if (!dev_exist(psDeviceName)) {
        return (1);
    }
    // enable the device
    if (euca_execlp(&rc, devh->cmdprefix, "ip", "link", "set", "dev", psDeviceName, "up", NULL) != EUCA_OK) {
        LOGERROR("Fail to enable device '%s'. error=%d\n", psDeviceName, rc);
        return (1);
    }
    return (0);
}

/**
 * Disables a network device.
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a string pointer to the device name to disable
 *
 * @return 0 on success or 1 if any failure occured
 *
 * @see dev_up(), dev_is_up()
 *
 * @pre
 *     The newtork device name must be valid and the network device must exists on this system
 *
 * @post
 *     On success, the device is disabled. If any failure occured, the device state will
 *     remain unchanged.
 */
int dev_down(dev_handler *devh, const char *psDeviceName) {
    int rc = 0;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure we have a valid device
    if (!dev_exist(psDeviceName)) {
        return (1);
    }
    // disable the device
    if (euca_execlp(&rc, devh->cmdprefix, "ip", "link", "set", "dev", psDeviceName, "down", NULL) != EUCA_OK) {
        LOGERROR("Fail to enable device '%s'. error=%d\n", psDeviceName, rc);
        return (1);
    }
    return (0);
}

/**
 * Renames a device on the system. This will achieve the following tasks:
 *     - Check if the device is a valid device
 *     - Check to make sure the new name is valid
 *     - Make sure the new device name isn't already in use
 *     - ip link set dev [psDeviceName] down
 *     - ip link set dev [psDeviceName] name [psNewDevName]
 *     - ip link set dev [psNewDevName] up
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a constant string pointer to the device name we need to rename
 * @param psNewDevName [in] a constant string pointer to the new device name
 *
 * @return TRUE if the VLAN is valid otherwise FALSE is returned
 *
 * @see dev_up(), dev_down(), dev_exist()
 *
 * @pre
 *     - Both string pointer must not be null
 *     - The psDeviceName must be the device name of an existing device on this system
 *     - The psDeviceName must be the device name of a non-existing device on this system
 *     - The psNewDevName must have at least one character
 *
 * @post
 *     - On success the device has been renamed
 *     - On failure the system state is left undetermined. Either the device has been renamed
 *       or not or the state of the device is up or down.
 *
 * @note
 */
int dev_rename(dev_handler *devh, const char *psDeviceName, const char *psNewDevName) {
    int rc = 0;

    // Make sure both pointers are valid and that the new name is of at least 1 character
    if (!psDeviceName || !psNewDevName || (strlen(psNewDevName) == 0)) {
        return (1);
    }
    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure the old device name exists on this system
    if (!dev_exist(psDeviceName)) {
        LOGERROR("Fail to rename network device '%s' to '%s'. Device not on this system!\n", psDeviceName, psNewDevName);
        return (1);
    }
    // Make sure the new device name isn't in use
    if (dev_exist(psNewDevName)) {
        LOGERROR("Fail to rename network device '%s' to '%s'. Device name '%s' already in use!\n", psDeviceName, psNewDevName, psNewDevName);
        return (1);
    }
    // Disable the device
    if (dev_down(devh, psDeviceName) != 0) {
        LOGERROR("Fail to rename network device '%s' to '%s'. Fail to disable '%s'!\n", psDeviceName, psNewDevName, psDeviceName);
        return (1);
    }
    // disable the device
    if (euca_execlp(&rc, devh->cmdprefix, "ip", "link", "set", "dev", psDeviceName, "name", psNewDevName, NULL) != EUCA_OK) {
        LOGERROR("Fail to rename network device '%s' to '%s'. error=%d\n", psDeviceName, psNewDevName, rc);
        return (1);
    }
    // Enable the device using the new name and just WARN on error
    if (dev_up(devh, psNewDevName) != 0) {
        LOGWARN("Fail to rename network device '%s' to '%s'. Fail to enable '%s'!\n", psDeviceName, psNewDevName, psNewDevName);
    }

    return (0);
}

/**
 * Checks wether or not a given VLAN identifier is valid. It should be between 0 and 4095.
 *
 * @param vlan [in] the VLAN identifier to validate
 *
 * @return TRUE if the VLAN is valid otherwise FALSE is returned
 */
static inline boolean dev_is_vlan_valid(u16 vlan) {
    if ((vlan >= MIN_VLAN_802_1Q) && (vlan <= MAX_VLAN_802_1Q))
        return (TRUE);
    return (FALSE);
}

/**
 * This function retrieves the name of a VLAN device based on its given base name
 * and VLAN number. The return value is a pointer into a statically-allocated buffer.
 * Subsequent calls will overwrite the same buffer, so you should copy the string if
 * you need to save it.
 *
 * In multi-threaded programs each thread has its own 8 statically-allocated buffer. But
 * still more than 8 subsequent calls of euca_ntoa in the same thread will overwrite
 * the result of the previous calls. if the result isn't saved.
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a string pointer to the base device name to which we will add the VLAN
 * @param vlan [in] the VLAN identifier
 *
 * @return A pointer to the statically-allocated buffer containing the dot representation
 *         of the address.
 */
const char *dev_get_vlan_name(const char *psDeviceName, u16 vlan) {
#define NB_BUFFERS                 8
#define MAX_VDEV_LEN              32

    char *psVlanDev = NULL;

    static u32 bufferIdx = 0;
    static char asVlanDev[NB_BUFFERS][MAX_VDEV_LEN] = { "" };

    // Retrieve the next buffer in line
    psVlanDev = asVlanDev[((bufferIdx++) % NB_BUFFERS)];

    // Creating the vlan device resulting name
    snprintf(psVlanDev, MAX_VDEV_LEN, "%s.%u", psDeviceName, vlan);

    return (psVlanDev);

#undef NB_BUFFERS
#undef MAX_VDEV_LEN
}

/**
 * This function retrieves the VLAN identifier portion of a VLAN device name. A
 * VLAN device name is of the "[base_name].[vlanId]" format.
 *
 * @param psDeviceName [in] a string pointer to the VLAN device name
 *
 * @return The associated VLAN identifier if this is a valid VLAN device name or -1 on failure.
 */
int dev_get_vlan_id(const char *psDeviceName) {
    char *psVlanId = NULL;

    // Make sure the given device name isn't NULL
    if (!psDeviceName) {
        return (-1);
    }
    // Does it have a valid format?
    if ((psVlanId = strstr(psDeviceName, ".")) == NULL) {
        return (-1);
    }

    return (atoi(psVlanId + 1));
}

/**
 * Checks wether or not a given VLAN is configured on a given device. Under Linux,
 * when adding a VLAN to a network device, this results in creating a new device
 * with the name set as [device_name].[vlan].
 *
 * @param psDeviceName [in] a constant string pointer to the base device name
 * @param vlan [in] the VLAN identifier to check
 *
 * @return TRUE if the given VLAN is configured on the given device otherwise FALSE
 *         is returned.
 *
 * @pre
 *     The psDeviceName parameter must not be NULL and the vlan parameter should be valid
 */
boolean dev_has_vlan(const char *psDeviceName, u16 vlan) {
    // Make sure the given string isn't NULL
    if (!psDeviceName)
        return (FALSE);

    // Does this VLAN device exist?
    return (dev_exist(dev_get_vlan_name(psDeviceName, vlan)));
}

/**
 * Configures a VLAN on a given device.
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a constant string pointer to the device name on which we are adding the VLAN
 * @param vlan [in] the VLAN identifier to add on the device
 *
 * @return A pointer to the newly created VLAN device or NULL on failure.
 *
 * @see dev_has_vlan(), dev_remove_vlan()
 *
 * @pre
 *     - The psDeviceName must not be null and the device should exist
 *     - The VLAN identifier must be valid
 *
 * @post
 *     On success the VLAN has been configured on the device. On failure, the VLAN is not
 *     configured on the network device.
 *
 * @note
 *     Since the return value is dynamically allocated, caller is responsible for freeing the memory
 */
dev_entry *dev_create_vlan(dev_handler *devh, const char *psDeviceName, u16 vlan) {
    int rc = 0;
    int nbDevices = 0;
    char sVlan[8] = "";
    dev_entry *pDevice = NULL;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (NULL);
    }
    // Make sure the given string isn't NULL
    if (!psDeviceName)
        return (NULL);

    // Make sure the VLAN is valid
    if (!IS_VLAN_VALID(vlan))
        return (NULL);

    // Check if we already have the VLAN configured
    if (dev_has_vlan(psDeviceName, vlan)) {
        // This must work since we know the vlan exists
        dev_get_list(devh, dev_get_vlan_name(psDeviceName, vlan), &pDevice, &nbDevices);
        return (pDevice);
    }
    // Execute the request
    snprintf(sVlan, 8, "%u", vlan);
    if (euca_execlp(&rc, devh->cmdprefix, VCONFIG_PATH, "add", psDeviceName, sVlan, NULL) != EUCA_OK) {
        LOGERROR("Fail to add VLAN '%s' to device '%s'. error=%d\n", sVlan, psDeviceName, rc);
        return (NULL);
    }
    // If the device exist then success
    if (!dev_has_vlan(psDeviceName, vlan))
        return (NULL);

    // This must work since we know the device exists
    dev_get_list(devh, dev_get_vlan_name(psDeviceName, vlan), &pDevice, &nbDevices);
    return (pDevice);
}

/**
 * Unconfigures a given VLAN from a given network device.
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a constant string pointer to the device name on which we are removing the VLAN
 * @param vlan [in] the VLAN identifier to add on the device
 *
 * @return 0 on success or 1 if any failure occured
 *
 * @see dev_has_vlan(), dev_create_vlan(), dev_remove_vlan_interface()
 *
 * @pre
 *     - The psDeviceName must not be null and the device should exist
 *     - The VLAN identifier should be valid
 *
 * @post
 *     On success the VLAN has been removed from the device. On failure, the VLAN is not
 *     removed From the network device.
 */
int dev_remove_vlan(dev_handler *devh, const char *psDeviceName, u16 vlan) {
    // Make sure the given string isn't NULL
    if (!psDeviceName)
        return (1);

    // Check if the device exists
    return (dev_remove_vlan_interface(devh, dev_get_vlan_name(psDeviceName, vlan)));
}

/**
 * Removes a given VLAN interface
 *
 * @param devh [in] pointer to the device handler
 * @param " [in] psVlanInterfaceName a constant string pointer to the VLAN device name of the "[devname].[VLAN] format
 *
 * @return 0 on success or 1 if any failure occured
 *
 * @see dev_has_vlan(), dev_remove_vlan()
 *
 * @pre
 *     - The psVlanInterfaceName must not be null and the device should exist
 *     - The name should be of the "devname.VLAN" format.
 *
 * @post
 *     On success the VLAN has been removed from the device. On failure, the VLAN is not
 *     removed From the network device.
 */
int dev_remove_vlan_interface(dev_handler *devh, const char *psVlanInterfaceName) {
    int rc = 0;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure the given string isn't NULL
    if (!psVlanInterfaceName)
        return (1);

    // Make sure its of the peoper format. Lets not go too crazy
    if (strstr(psVlanInterfaceName, ".") == NULL)
        return (1);

    // Check if the device exists
    if (!dev_exist(psVlanInterfaceName))
        return (0);

    // Execute the request
    if (euca_execlp(&rc, devh->cmdprefix, VCONFIG_PATH, "rem", psVlanInterfaceName, NULL) != EUCA_OK) {
        LOGERROR("Fail to remove vlan interface '%s'. error=%d\n", psVlanInterfaceName, rc);
        return (1);
    }
    // If the device does not exist then success
    if (dev_exist(psVlanInterfaceName))
        return (1);
    return (0);
}

/**
 * Checks whether or not a given device is a bridge device
 *
 * @param psDeviceName [in] a string pointer to the device name we are checking
 *
 * @return TRUE if the device is a bridge device otherwise FALSE is returned
 *
 * @see
 *
 * @pre
 *     - psDeviceName MUST not be NULL
 *     - psDeviceName must be a valid device
 */
boolean dev_is_bridge(const char *psDeviceName) {
#define MAX_PATH_LEN             64

    char sPath[MAX_PATH_LEN] = "";

    // Make sure the given string isn't NULL
    if (!psDeviceName)
        return (FALSE);

    // Each device has its path under /sys/class/net/[device]/
    snprintf(sPath, MAX_PATH_LEN, "/sys/class/net/%s/bridge/", psDeviceName);

    // If this device does not have a 'bridge' path, this isn't a bridge device
    if (check_directory(sPath))
        return (FALSE);
    return (TRUE);

#undef MAX_PATH_LEN
}

/**
 * Checks wether or not a given device is a bridged interface. If the bridge
 * device name is provided, it will also check if the device is a member of
 * the given bridge device.
 *
 * @param psDeviceName [in] a string pointer to the device name we are checking
 * @param psBridgeName [in] an optional string pointer to the bridge device name
 *
 * @return TRUE if this is a bridge interface and, if the bridge name is provided,
 *         that the device is a member of the bridge. Otherwise FALSE is returned.
 * @pre
 *     - psDeviceName MUST not be NULL
 *     - Both psDeviceName and psBridgeName must be valid devices
 */
boolean dev_is_bridge_interface(const char *psDeviceName, const char *psBridgeName) {
#define MAX_PATH_LEN             128

    char sPath[MAX_PATH_LEN] = "";

    // Make sure the given string isn't NULL
    if (!psDeviceName)
        return (FALSE);

    // Each device has its path under /sys/class/net/[device]/
    snprintf(sPath, MAX_PATH_LEN, "/sys/class/net/%s/brport/", psDeviceName);

    // If this device does not have a 'brport' path, this isn't a bridge device
    if (check_directory(sPath))
        return (FALSE);

    // Do we want to validate if we are part of a given bridge?
    if (psBridgeName) {
        // Each device has its path under /sys/class/net/[device]/
        snprintf(sPath, MAX_PATH_LEN, "/sys/class/net/%s/brif/%s/", psBridgeName, psDeviceName);

        // are we part of this bridge?
        if (check_directory(sPath)) {
            return (FALSE);
        }
    }
    return (TRUE);

#undef MAX_PATH_LEN
}

/**
 * Checks wether or not a given bridge device has associated interfaces.
 *
 * @param devh [in] pointer to the device handler
 * @param psBridgeName [in] a constant string pointer to the bridge device name to validate
 *
 * @return TRUE if this bridge device has associated interface. Otherwise FALSE is returned.
 *
 * @see dev_is_bridge(), dev_get_bridge_interfaces()
 *
 * @pre
 *     - psBridgeName MUST not be NULL
 *     - psBridgeName must be a valid bridge device on this system
 */
boolean dev_has_bridge_interfaces(dev_handler *devh, const char *psBridgeName) {
    int nbInterfaces = 0;
    dev_entry *pInterfaces = NULL;

    // Make sure we have a valid name
    if (!psBridgeName)
        return (FALSE);

    //
    // See if we have any associated interfaces. This will also validate
    // psBridgeName to be a valid bridge device on this system
    //
    if (dev_get_bridge_interfaces(devh, psBridgeName, &pInterfaces, &nbInterfaces) != 0)
        return (FALSE);

    // Done with the interface list
    dev_free_list(&pInterfaces, nbInterfaces);
    return ((nbInterfaces > 0) ? TRUE : FALSE);
}

/**
 * Retrieves the bridge device name for which the given psDeviceName is a member of
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a string pointer to the device name we are checking
 *
 * @return a pointer to the dynamically allocated string or NULL on failure.
 *
 * @see dev_is_bridge_interface()
 *
 * @pre
 *     - Both psDeviceName and psOutBridgeName MUST not be NULL
 *     - psDeviceName must be a valid device
 *
 * @post
 *     On success, the psOutBridgeName is set appropriately. On Failure, psOutBridgeName
 *     is set to NULL.
 *
 * @note
 *     Since the result is a dynamic string, the caller is responsible for freeing it.
 */
char *dev_get_interface_bridge(dev_handler *devh, const char *psDeviceName) {
#define INTFC_LINE_STRING             "INTERFACE="
#define MAX_LINE_LEN                  64
#define MAX_PATH_LEN                  64

    char *p = NULL;
    char *psOutBridgeName = NULL;
    char sLine[MAX_LINE_LEN] = "";
    char sPath[MAX_PATH_LEN] = "";
    FILE *pFh = NULL;

    // Make sure the given string aren't NULL
    if (!psDeviceName)
        return (NULL);

    // Is this a bridged interface?
    if (!dev_is_bridge_interface(psDeviceName, NULL))
        return (NULL);

    // Each device has its path under /sys/class/net/[device]/
    snprintf(sPath, MAX_PATH_LEN, "/sys/class/net/%s/brport/bridge/uevent", psDeviceName);

    // If this device does not have a 'brpor/bridge/ueventt' file, this isn't a bridged device
    if ((pFh = fopen(sPath, "r")) == NULL)
        return (NULL);

    // Read until we reach the line starting with 'INTERFACE='
    while (fgets(sLine, MAX_LINE_LEN, pFh)) {
        // remove the '\n' character.
        if ((p = strchr(sLine, '\n')) != NULL)
            *p = '\0';

        // Is this the "INTERFACE" line
        if (!strncmp(sLine, INTFC_LINE_STRING, strlen(INTFC_LINE_STRING))) {
            // We got it, retrieve the bridge device name after the '=' character
            p = sLine + strlen(INTFC_LINE_STRING);
            psOutBridgeName = strdup(p);
            break;
        }
    }

    fclose(pFh);
    return (psOutBridgeName);

#undef INTFC_LINE_STRING
#undef MAX_LINE_LEN
#undef MAX_PATH_LEN
}

/**
 * Retrieves the list of assigned interfaces to a bridge device.
 *
 * @param devh [in] pointer to the device handler
 * @param psBridgeName [in] a constant string pointer to the bridge device name
 * @param pOutDevices [in,out] a pointer to our outgoing device structure
 * @param pOutNbDevices [in,out] a pointer to the counter that will contain the number of devices found
 *
 * @return 0 on success or 1 if any failure occured
 *
 * @pre
 *     All of our pointers must not be NULL. The Bridge device must be a valid device and a bridge device.
 *
 * @post
 *     On success, the list is filled with the interfaces found. On failure, the list is empty.
 *
 * @note
 *     Since this list is dynamically allocated, the caller is responsible to free the list
 */
int dev_get_bridge_interfaces(dev_handler *devh, const char *psBridgeName, dev_entry **pOutDevices, int *pOutNbDevices) {
#define MAX_PATH_LEN           128

    DIR *pDh = NULL;
    char sBrIfPath[MAX_PATH_LEN] = "";
    boolean done = FALSE;
    dev_entry *pDevices = NULL;
    struct dirent dent = { 0 };
    struct dirent *pResult = NULL;

    // Make sure the given pointers are valid
    if (!psBridgeName || !pOutDevices || !pOutNbDevices)
        return (1);

    // Set our list and counter to NULL/0
    (*pOutDevices) = NULL;
    (*pOutNbDevices) = 0;

    // The device must be a valid bridge
    if (!dev_is_bridge(psBridgeName))
        return (1);

    // Our assigned interface are listed under /sys/class/net/[device]/brif/
    snprintf(sBrIfPath, MAX_PATH_LEN, "/sys/class/net/%s/brif/", psBridgeName);

    // Open the directory and scan it for our assigned device name
    if ((pDh = opendir(sBrIfPath)) != NULL) {
        while (!done && (readdir_r(pDh, &dent, &pResult) == 0)) {
            // Make sure the given result is valid
            if (pResult == NULL) {
                done = TRUE;
                continue;
            }
            // Skip the . and ..
            if (strcmp(pResult->d_name, ".") && strcmp(pResult->d_name, "..")) {
                // Reallocate the memory as we need
                if ((pDevices = EUCA_REALLOC((*pOutDevices), ((*pOutNbDevices) + 1), sizeof(dev_entry))) != NULL) {
                    (*pOutDevices) = pDevices;

                    // Setup the structure
                    snprintf((*pOutDevices)[(*pOutNbDevices)].sDevName, IF_NAME_LEN, "%s", pResult->d_name);
                    snprintf((*pOutDevices)[(*pOutNbDevices)].sMacAddress, ENET_ADDR_LEN, "%s", dev_get_mac(pResult->d_name));
                    (*pOutDevices)[(*pOutNbDevices)].isBridge = 0;
                    (*pOutNbDevices)++;
                } else {
                    LOGERROR("Out of memory!\n");
                    dev_free_list(pOutDevices, (*pOutNbDevices));
                    (*pOutDevices) = NULL;
                    (*pOutNbDevices) = 0;
                    done = TRUE;
                }
            }
        }
        closedir(pDh);
    }

    return (0);

#undef MAX_PATH_LEN
}

/**
 * Sets the STP state on a given bridge device
 *
 * @param devh [in] pointer to the device handler
 * @param psBridgeName [in] a constant string pointer to the bridge device name
 * @param psStpState [in] a constant string pointer to the STP state. Must be either "on" or "off"
 *
 * @return 0 on success or 1 on failure
 *
 * @see dev_create_bridge()
 *
 * @pre
 *     - The psBridgeName parameter must not be NULL and if the device exists, it should be a valid bridge device
 *     - The psStpState must not be NULL and must be either "on" or "off"
 *
 * @post
 *     On success, the bridge device STP state has been updated. On failure, nothing has changed
 *     on the system.
 *
 * @note
 *     Since the return value is dynamically allocated, caller is responsible for freeing the memory
 */
int dev_set_bridge_stp(dev_handler *devh, const char *psBridgeName, const char *psStpState) {
    int rc = 0;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure the pointer isn't NULL
    if (!psBridgeName || !psStpState)
        return (1);

    // Is the STP state string valid?
    if (strcmp(psStpState, BRIDGE_STP_ON) && strcmp(psStpState, BRIDGE_STP_OFF))
        return (1);

    // If the bridge device does not exists then that's bad!!!
    if (!dev_exist(psBridgeName))
        return (1);

    // Is this a valid bridge device?
    if (!dev_is_bridge(psBridgeName))
        return (1);

    // Set the STP state
    if (euca_execlp(&rc, devh->cmdprefix, BRCTL_PATH, "stp", psBridgeName, psStpState, NULL) != EUCA_OK) {
        LOGERROR("Fail to set STP to '%s' on bridge device '%s'. error=%d\n", psStpState, psBridgeName, rc);
        return (1);
    }

    return (0);
}

/**
 * Creates a bridge device. If the device already exists and is a bridge, this is
 * basically a no-op.
 *
 * @param devh [in] pointer to the device handler
 * @param psBridgeName [in] a constant string pointer to the bridge device name
 * @param psStpState [in] a constant string pointer to the STP state. Must be either "on" or "off"
 *
 * @return A pointer to the newly created bridge device or NULL on failure.
 *
 * @see dev_remove_bridge()
 *
 * @pre
 *     - The psBridgeName parameter must not be NULL and if the device exists, it should be a valid bridge device
 *     - The psStpState must not be NULL and must be either "on" or "off"
 *
 * @post
 *     On success, the bridge device is created. On failure, nothing changed on the rc = system
 *
 * @note
 *     Since the return value is dynamically allocated, caller is responsible for freeing the memory
 */
dev_entry *dev_create_bridge(dev_handler *devh, const char *psBridgeName, const char *psStpState) {
    int rc = 0;
    int nbBridges = 0;
    dev_entry *pBridge = NULL;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (NULL);
    }
    // Make sure the pointer isn't NULL
    if (!psBridgeName || !psStpState)
        return (NULL);

    // Is the STP state string valid?
    if (strcmp(psStpState, BRIDGE_STP_ON) && strcmp(psStpState, BRIDGE_STP_OFF))
        return (NULL);

    // If the bridge already exists, then we're good
    if (dev_exist(psBridgeName)) {
        // Is this a valid bridge device?
        if (!dev_is_bridge(psBridgeName))
            return (NULL);

        // Retrieve the device it should succeed because we know its a bridge
        dev_get_bridges(devh, psBridgeName, &pBridge, &nbBridges);
        return (pBridge);
    }
    // Create the bridge device
    if (euca_execlp(&rc, devh->cmdprefix, BRCTL_PATH, "addbr", psBridgeName, NULL) != EUCA_OK) {
        LOGERROR("Fail to create bridge device '%s'. error=%d\n", psBridgeName, rc);
    }
    // Did it work?
    if (!dev_exist(psBridgeName))
        return (NULL);

    // Set the STP state
    if (euca_execlp(&rc, devh->cmdprefix, BRCTL_PATH, "stp", psBridgeName, psStpState, NULL) != EUCA_OK) {
        LOGERROR("Fail to set STP state '%s' on bridge device '%s'. error=%d\n", psStpState, psBridgeName, rc);
    }
    // Set the forwarding delay
    if (euca_execlp(&rc, devh->cmdprefix, BRCTL_PATH, "setfd", psBridgeName, "2", NULL) != EUCA_OK) {
        LOGERROR("Fail to set forwarding delay on bridge device '%s'. error=%d\n", psBridgeName, rc);
    }
    // Set the hello time
    if (euca_execlp(&rc, devh->cmdprefix, BRCTL_PATH, "sethello", psBridgeName, "2", NULL) != EUCA_OK) {
        LOGERROR("Fail to set hello time on bridge device '%s'. error=%d\n", psBridgeName, rc);
    }
    // RHEL7/CentOS7 - set bridge interface in promiscuous mode
    if (euca_execlp(&rc, devh->cmdprefix, "ip", "link", "set", "dev", psBridgeName, "promisc", "on", NULL) != EUCA_OK) {
        LOGERROR("Fail to set bridge device '%s' in promisc. error=%d\n", psBridgeName, rc);
    }
    // This must work since we know the device exists
    dev_get_bridges(devh, psBridgeName, &pBridge, &nbBridges);
    return (pBridge);
}

/**
 * Force remove a bridge device from the system. This will ensure that all assigned
 * network devices are unassigned first.
 *
 * @param devh [in] pointer to the device handler
 * @param psBridgeName [in] a constant string pointer to the bridge device name
 *
 * @return 0 on success or 1 if any failure occured.
 *
 * @see dev_remove_bridge(), dev_create_bridge(), dev_bridge_delete_interface()
 *
 * @pre
 *     At this point, psBridgeName should have been validated not to be NULL, to
 *     be a valid bridge device.
 *
 * @post
 *     On success, the bridge associated interfaces are unassigned and the bridge
 *     device removed from the system. On failure, the state of the bridge device
 *     and its associated network device is non-deterministic.
 *
 * @note
 */
static int dev_remove_bridge_forced(dev_handler *devh, const char *psBridgeName) {
    int i = 0;
    int rc = 0;
    int nbDevices = 0;
    dev_entry *pDevices = NULL;

    // Retrieved our assigned interfaces
    if (dev_get_bridge_interfaces(devh, psBridgeName, &pDevices, &nbDevices) != 0)
        return (1);

    // Remove our assigned interfaces
    for (i = 0; i < nbDevices; i++) {
        rc |= dev_bridge_delete_interface(devh, psBridgeName, pDevices[i].sDevName);
    }

    // ok, free the device list
    dev_free_list(&pDevices, nbDevices);

    // Did is worked?
    if (rc) {
        // We failed :(
        return (1);
    }
    // DO NOT CALL WITH TRUE HERE TO AVOID RECURSIVE LOOP
    return (dev_remove_bridge(devh, psBridgeName, FALSE));
}

/**
 * Creates a bridge device. If the device already exists and is a bridge, this is
 * basically a no-op.
 *
 * @param devh [in] pointer to the device handler
 * @param psBridgeName [in] a constant string pointer to the bridge device name
 * @param forced [in] set to TRUE to force remove any assigned interface. Otherwise, set to FALSE.
 *
 * @return 0 on success or 1 if any failure occured
 *
 * @see dev_add_bridge(), dev_remove_bridge_forced()
 *
 * @pre
 *     The psBridgeName parameter must not be NULL and should be a valid bridge device
 *
 * @post
 *     On success, the bridge device is removed. On failure, nothing should have changed
 *     on the system.
 *
 * @note
 */
int dev_remove_bridge(dev_handler *devh, const char *psBridgeName, boolean forced) {
    int rc = 0;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure the pointer isn't NULL
    if (!psBridgeName)
        return (1);

    // If the bridge does not exists, then we're good
    if (!dev_exist(psBridgeName))
        return (0);

    // Is this a valid bridge device?
    if (!dev_is_bridge(psBridgeName))
        return (1);

    // Remove the bridge device
    if (euca_execlp(&rc, devh->cmdprefix, BRCTL_PATH, "delbr", psBridgeName, NULL) != EUCA_OK) {
        // Lets follow through in case we can do something else
        LOGERROR("Fail to delete bridge device '%s'. error=%d\n", psBridgeName, rc);
    }
    // Did it work?
    if (dev_exist(psBridgeName)) {
        // Do we absolutely need to remove it?
        if (forced) {
            return (dev_remove_bridge_forced(devh, psBridgeName));
        }
        return (1);
    }

    return (0);
}

/**
 * Adds a network devices to a given bridge device
 *
 * @param devh [in] pointer to the device handler
 * @param psBridgeName [in] a constant string pointer to the bridge device name
 * @param psDeviceName [in] a constant string pointer to the network device name
 *
 * @return 0 on success or 1 if any failure occured
 *
 * @see dev_bridge_delete_interface()
 *
 * @pre
 *     - Both psBridgeName and psDeviceName must not be NULL
 *     - The bridge device must exists and be a bridge type device
 *     - The network device must exists and unassociated from any bridge device
 *
 * @post
 *     On success, the network device is unassign from the bridge device. On failure,
 *     nothing has changed on the system.
 *
 * @note
 */
int dev_bridge_assign_interface(dev_handler *devh, const char *psBridgeName, const char *psDeviceName) {
    int rc = 0;
    char *pStr = NULL;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure the pointer isn't NULL
    if (!psBridgeName || !psDeviceName)
        return (1);

    // If the bridge does not exists or if the assigned device does not exists, then we're in trouble
    if (!dev_exist(psBridgeName) || !dev_exist(psDeviceName))
        return (1);

    // The network device should not be assigned to a bridge. If it is, is should be to this bridge
    if ((pStr = dev_get_interface_bridge(devh, psDeviceName)) != NULL) {
        if (!strcmp(psBridgeName, pStr)) {
            EUCA_FREE(pStr);
            return (0);
        } else {
            EUCA_FREE(pStr);
            return (1);
        }
    }
    // Add the network device to the bridge
    if (euca_execlp(&rc, devh->cmdprefix, BRCTL_PATH, "addif", psBridgeName, psDeviceName, NULL) != EUCA_OK) {
        LOGERROR("Fail to add interface '%s' to bridge device '%s'. error=%d\n", psDeviceName, psBridgeName, rc);
    }
    // Did it work?
    if (!dev_is_bridge_interface(psDeviceName, psBridgeName))
        return (1);
    return (0);
}

/**
 * Removes a network device from a bridge device
 *
 * @param devh [in] pointer to the device handler
 * @param psBridgeName [in] a constant string pointer to the bridge device name
 * @param psDeviceName [in] a constant string pointer to the network device name
 *
 * @return 0 on success or 1 if any failure occured
 *
 * @see dev_bridge_assign_interface()
 *
 * @pre
 *     - Both psBridgeName and psDeviceName must not be NULL
 *     - The bridge device must exists and be a bridge type device
 *     - The network device must exists and associated with this bridge
 *
 * @post
 *     On success, the network device is unassign from the bridge device. On failure,
 *     nothing has changed on the system.
 *
 * @note
 */
int dev_bridge_delete_interface(dev_handler *devh, const char *psBridgeName, const char *psDeviceName) {
    int rc = 0;
    char *pStr = NULL;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure the pointer isn't NULL
    if (!psBridgeName || !psDeviceName)
        return (1);

    // If the bridge does not exists or if the assigned device does not exists, then we're in trouble
    if (!dev_exist(psBridgeName) || !dev_exist(psDeviceName))
        return (1);

    // The network device should be assigned to this bridge
    if ((pStr = dev_get_interface_bridge(devh, psDeviceName)) != NULL) {
        if (strcmp(psBridgeName, pStr)) {
            // Assigned to another bridge device
            EUCA_FREE(pStr);
            return (1);
        }

        EUCA_FREE(pStr);
    } else {
        // Not a bridge device!!!
        return (1);
    }

    // Remove the network device from the bridge
    if (euca_execlp(&rc, devh->cmdprefix, BRCTL_PATH, "delif", psBridgeName, psDeviceName, NULL) != EUCA_OK) {
        LOGERROR("Fail to remove interface '%s' from bridge device '%s'. error=%d\n", psDeviceName, psBridgeName, rc);
    }
    // Did it work?
    if (dev_is_bridge_interface(psDeviceName, psBridgeName))
        return (1);
    return (0);
}

/**
 * This checks wether or not a given device is a tunnel device. For our current
 * purpose, any tunnel device has its nane starting with "tap-".
 *
 * @param psDeviceName [in] a constant string pointer to the device name we are assessing
 *
 * @return TRUE if this is a tunnel device or FALSE if it is not or if we were given
 *         a NULL string parameter.
 *
 * @see
 *
 * @pre
 *     The psDeviceName parameter MUST not be NULL
 *
 * @post
 *
 * @note
 */
boolean dev_is_tunnel(const char *psDeviceName) {
    // Check if our parameter is NULL
    if (!psDeviceName)
        return (FALSE);

    // Does it start with the tunnel device name prefix?
    if (!strncmp(psDeviceName, TUNNEL_NAME_PREFIX, strlen(TUNNEL_NAME_PREFIX)))
        return (TRUE);
    return (FALSE);
}

/**
 * This function retrieves the MAC address of a given device. This is done by reading the
 * value contained within the /sys/class/net/[device]/address system file.
 *
 * In multi-threaded programs each thread has its own 8 statically-allocated buffer. But
 * still more than 8 subsequent calls of euca_ntoa in the same thread will overwrite
 * the result of the previous calls. if the result isn't saved.
 *
 * @param psDeviceName [in] a string pointer to the device name for which we are looking for a MAC address
 *
 * @return a pointer to the static buffer containg the MAC address or NULL if a failure occured
 *
 * @see dev_exist()
 *
 * @pre
 *     - The psDeviceName must not be NULL
 *     - The device must be present on the system
 *
 * @post
 *     A static buffer is allocated for this MAC address and returned with the value
 *
 * @note
 */
char *dev_get_mac(const char *psDeviceName) {
#define MAX_LINE_LEN                  32
#define MAX_PATH_LEN                  64
#define MAX_STRING_BUFFER              8

    char *p = NULL;
    char *psOutMac = NULL;
    char sLine[MAX_LINE_LEN] = "";
    char sPath[MAX_PATH_LEN] = "";
    FILE *pFh = NULL;

    static u32 idx = 0;
    static char asBuffer[MAX_STRING_BUFFER][ENET_ADDR_LEN] = { {""} };

    // Make sure the given device string isn't NULL
    if (!psDeviceName)
        return (NULL);

    // Make sure this device exists
    if (!dev_exist(psDeviceName))
        return (NULL);

    // Each device has its path under /sys/class/net/[device]/
    snprintf(sPath, MAX_PATH_LEN, "/sys/class/net/%s/address", psDeviceName);

    // If the device
    if ((pFh = fopen(sPath, "r")) == NULL)
        return (NULL);

    // Read the first line. We should get the mac address
    if (fgets(sLine, MAX_LINE_LEN, pFh)) {
        // remove the '\n' character.
        if ((p = strchr(sLine, '\n')) != NULL)
            *p = '\0';

        psOutMac = asBuffer[(idx++ % MAX_STRING_BUFFER)];
        snprintf(psOutMac, ENET_ADDR_LEN, "%s", sLine);
    }

    fclose(pFh);
    return (psOutMac);

#undef MAX_LINE_LEN
#undef MAX_PATH_LEN
#undef MAX_STRING_BUFFER
}

/**
 * Retrieve a list of IP information for a given device. If the device is not provided
 * (i.e. NULL is passed for psDeviceName), then we retrieve the list of all IPs on the
 * system.
 *
 * @param psDeviceName [in] an optional string pointer to the device name we want to filter on.
 * @param pOutIps [in,out] a pointer to the returned IP list.
 * @param pNumberOfIps [in,out] a pointer to the field that will return the number of IPs in the list
 *
 * @return 0 on success or 1 on failure
 *
 * @see dev_free_ips()
 *
 * @pre
 *     Both pOutIps and pNumberOfIps MUST not be NULL.
 *
 * @post
 *     On successful completion, pOutIps contains the list of IPs found on the system for
 *     the given device (or entire system) and pNumberIps contains the number of elements
 *     in the list. On failure, both pNumberOfIps and pOutIps are non-deterministic.
 *
 * @note
 *     Caller is responsible to free the dynamically allocated list of IP entries.
 */
int dev_get_ips(const char *psDeviceName, in_addr_entry **pOutIps, int *pNumberOfIps) {
    int rc = 0;
    char sAddress[NI_MAXHOST] = "";
    char sMask[NI_MAXHOST] = "";
    in_addr_entry *pEntry = NULL;
    struct ifaddrs *pIfa = NULL;
    struct ifaddrs *pIfAddr = NULL;

    // Make sure we have the proper pointers.
    if ((pOutIps == NULL) || (pNumberOfIps == NULL)) {
        return (1);
    }
    // Set the list and number of items to NULL and 0
    (*pOutIps) = NULL;
    (*pNumberOfIps) = 0;

    // get the list of network devices
    if (getifaddrs(&pIfAddr) == -1) {
        LOGERROR("Failed to retrieve the list of network devices.\n");
        return (1);
    }
    // Scan the list for AF_INET devices type
    for (pIfa = pIfAddr; pIfa != NULL; pIfa = pIfa->ifa_next) {
        // Validate the ifaddr
        if (pIfa->ifa_addr == NULL)
            continue;

        // Only IP devices
        if (pIfa->ifa_addr->sa_family != AF_INET)
            continue;

        // Were we provided a filter?
        if (psDeviceName) {
            // if the name does not match the device name then skip
            if (strcmp(pIfa->ifa_name, psDeviceName)) {
                continue;
            }
        }
        // Alright, new one, allocate some memory
        if ((pEntry = EUCA_REALLOC((*pOutIps), ((*pNumberOfIps) + 1), sizeof(in_addr_entry))) == NULL) {
            LOGERROR("Failed to retrieve IP address list for device %s: Memory allocation failure.\n", psDeviceName);
            dev_free_ips(pOutIps);
            (*pNumberOfIps) = 0;
            freeifaddrs(pIfAddr);
            return (1);
        }
        // Retrieve the IP address information
        if ((rc = getnameinfo(pIfa->ifa_addr, sizeof(struct sockaddr_in), sAddress, NI_MAXHOST, NULL, 0, NI_NUMERICHOST)) != 0) {
            LOGERROR("Failed to retrieve IP address list for device %s: %s\n", psDeviceName, gai_strerror(rc));
            dev_free_ips(pOutIps);
            (*pNumberOfIps) = 0;
            freeifaddrs(pIfAddr);
            EUCA_FREE(pEntry);
            return (1);
        }
        // Retrieve the netmask address information
        if ((rc = getnameinfo(pIfa->ifa_netmask, sizeof(struct sockaddr_in), sMask, NI_MAXHOST, NULL, 0, NI_NUMERICHOST)) != 0) {
            LOGERROR("Failed to retrieve Netmask address list for device %s: %s\n", psDeviceName, gai_strerror(rc));
            dev_free_ips(pOutIps);
            (*pNumberOfIps) = 0;
            freeifaddrs(pIfAddr);
            EUCA_FREE(pEntry);
            return (1);
        }
        // Fill in our structure
        (*pOutIps) = pEntry;
        dev_in_addr_entry(&((*pOutIps)[(*pNumberOfIps)]), pIfa->ifa_name, dot2hex(sAddress), dot2hex(sMask));
        (*pNumberOfIps)++;
    }

    // Free before leaving
    freeifaddrs(pIfAddr);
    return (0);
}

/**
 * Checks whether or not an IP is installed on the given device. If NULL, this act as
 * looking for the IP address on the entire system
 *
 * @param psDeviceName [in] an optional string pointer to the device name to lookup
 * @param ip [in] the IP address to lookup on the device
 *
 * @return TRUE if the IP is found on the device otherwise FALSE is returned
 *
 * @pre
 *     The psDeviceName should be a valid device if provided
 */
boolean dev_has_ip(const char *psDeviceName, in_addr_t ip) {
    int i = 0;
    int nbIps = 0;
    boolean found = FALSE;
    in_addr_entry *pIps = NULL;

    // Can we retrieve the IP address list for this device?
    if (dev_get_ips(psDeviceName, &pIps, &nbIps)) {
        LOGERROR("Failure to lookup IP information for device '%s'.", psDeviceName);
        return (FALSE);
    }
    // Do we have any IP assigned with this device?
    if (nbIps == 0) {
        EUCA_FREE(pIps);
        return (FALSE);
    }
    // Check if the given IP is part of our list
    for (i = 0; ((i < nbIps) && !found); i++) {
        if (pIps[i].address == ip)
            found = TRUE;
    }

    // Free our list and return what we found
    dev_free_ips(&pIps);
    return (found);
}

/**
 * Checks wether or not an IP host is installed on the given device. If NULL, this act as
 * looking for the IP address on the entire system
 *
 * @param psDeviceName [in] an optional string pointer to the device name to lookup
 * @param ip [in] the IP address to lookup on the device
 * @param netmask [in] the netmask address to lookup on the device
 *
 * @return TRUE if the IP/netmask association is found on the device or system
 *         otherwise FALSE is returned
 *
 * @pre
 *     The psDeviceName should be a valid device if provided
 */
boolean dev_has_host(const char *psDeviceName, in_addr_t ip, in_addr_t netmask) {
    int i = 0;
    int nbIps = 0;
    boolean found = FALSE;
    in_addr_entry *pIps = NULL;

    // Can we retrieve the IP address list for this device?
    if (dev_get_ips(psDeviceName, &pIps, &nbIps)) {
        LOGERROR("Failure to lookup IP information for device '%s'.", psDeviceName);
        return (FALSE);
    }
    // Do we have any IP assigned with this device?
    if (nbIps == 0) {
        EUCA_FREE(pIps);
        return (FALSE);
    }
    // Check if the given IP is part of our list
    for (i = 0; ((i < nbIps) && !found); i++) {
        if ((pIps[i].address == ip) && (pIps[i].netmask == netmask))
            found = TRUE;
    }

    // Free our list and return what we found
    dev_free_ips(&pIps);
    return (found);
}

/**
 * Flushes all IP addresses installed on a given network device
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a string pointer to the device name for which we are installing the address
 *
 * @return 0 on success or 1 if any failure occured
 *
 * @pre
 *     The psDeviceName must be a valid device on the system.
 *
 * @post
 *     The device is stripped of all its IP configuration
 */
int dev_flush_ips(dev_handler *devh, const char *psDeviceName) {
    int rc = 0;

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure out device exists
    if (!dev_exist(psDeviceName)) {
        return (1);
    }
    // Ok, we're good. Now lets flush the IP addresses
    if (euca_execlp(&rc, devh->cmdprefix, "ip", "addr", "flush", psDeviceName, NULL) != EUCA_OK) {
        LOGERROR("Fail to flush ip addresses on network device '%s'. error=%d\n", psDeviceName, rc);
        return (1);
    }

    return (0);
}

/**
 * Install an IP/netmask address on a given device.
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a string pointer to the device name for which we are installing the address
 * @param address [in] the address to install
 * @param netmask [in] the network mask associated with this address
 * @param broadcast [in] the network broadcast address
 * @param psScope [in] a constant string pointer to the scope of the address (SCOPE_GLOBAL, SCOPE_SITE, SCOPE_LINK, SCOPE_HOST)
 *
 * @return 0 on success or 1 on failure.
 *
 * @see dev_move_ip(), dev_move_ips(), dev_install_ip(), dev_remove_ip(), dev_remove_ips()
 *
 * @pre
 *     The psDeviceName must be a valid device on the system.
 *
 * @post
 *     The IP is installed on their respective devices
 *
 * @note
 */
int dev_install_ip(dev_handler *devh, const char *psDeviceName, in_addr_t address, in_addr_t netmask, in_addr_t broadcast, const char *psScope) {
    int rc = 0;
    u32 slashnet = NETMASK_TO_SLASHNET(netmask);
    char sHost[NETWORK_ADDR_LEN] = "";

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure out device exists
    if (!dev_exist(psDeviceName)) {
        return (1);
    }
    // Set our host address
    snprintf(sHost, NETWORK_ADDR_LEN, "%s/%u", euca_ntoa(address), slashnet);

    //
    // If the address is already assigned, this will simply update if anything needs
    // to be updated. Changing the scope/netmask of an installed address is a valid
    // optration.
    //
    if (broadcast) {
        if (euca_execlp(&rc, devh->cmdprefix, "ip", "addr", "add", sHost, "broadcast", euca_ntoa(broadcast), "scope", psScope, "dev", psDeviceName, NULL) != EUCA_OK) {
            LOGERROR("Failed to install host '%s' Broadcast '%s' with scope '%s' on network device '%s'. error=%d\n", sHost, euca_ntoa(broadcast), psScope, psDeviceName, rc);
            return (1);
        }
    } else {
        if (euca_execlp(&rc, devh->cmdprefix, "ip", "addr", "add", sHost, "scope", psScope, "dev", psDeviceName, NULL) != EUCA_OK) {
            LOGERROR("Failed to install host '%s' with scope '%s' on network device '%s'. error=%d\n", sHost, psScope, psDeviceName, rc);
            return (1);
        }
    }
    return (0);
}

/**
 * Install a set of IP addresses.
 *
 * @param devh [in] pointer to the device handler
 * @param pIps [in] a pointer to the set of IP address entry to install
 * @param nbIps [in] the number of IP entry in the set
 * @param psScope [in] a constant string pointer to the scope of the address (SCOPE_GLOBAL, SCOPE_SITE, SCOPE_LINK, SCOPE_HOST)
 *
 * @return the number of IP address installed successfully from the set
 *
 * @see dev_move_ip(), dev_move_ips(), dev_install_ip(), dev_remove_ip(), dev_remove_ips()
 *
 * @pre
 *     The pIps parameter should not be NULL
 *
 * @post
 *     The IPs are installed on their respective devices
 */
int dev_install_ips(dev_handler *devh, in_addr_entry *pIps, int nbIps, const char *psScope) {
    int i = 0;
    int installed = 0;

    // Make sure we have a valid list
    if (!pIps)
        return (0);

    for (i = 0; i < nbIps; i++) {
        if (dev_install_ip(devh, pIps[i].sDevName, pIps[i].address, pIps[i].netmask, pIps[i].broascast, psScope) == 0)
            installed++;
    }

    return (installed);
}

/**
 * Moves an IP/netmask address from a device onto another given device. If the IP address is assigned
 * to another network device, this API will remove the IP address prior installing it on the given
 * network device.
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a string pointer to the device name for which we are moving the address
 * @param address [in] the address to move
 * @param netmask [in] the network mask associated with this address
 * @param broadcast [in] the network broadcast address
 * @param psScope [in] a constant string pointer to the scope of the address (SCOPE_GLOBAL, SCOPE_SITE, SCOPE_LINK, SCOPE_HOST)
 *
 * @return 0 on success or 1 on failure.
 *
 * @see dev_move_ips(), dev_install_ip(), dev_remove_ip(), dev_remove_ips()
 *
 * @pre
 *     The psDeviceName must be a valid device on the system and the scope must be a valid scope
 *
 * @post
 *     The IP is installed/updated on its respective devices and removed from any other interface
 *     it may have been assigned too.
 *
 * @note
 */
int dev_move_ip(dev_handler *devh, const char *psDeviceName, in_addr_t address, in_addr_t netmask, in_addr_t broadcast, const char *psScope) {
    int i = 0;
    int nbOfIps = 0;
    boolean found = FALSE;
    boolean needInstall = TRUE;
    in_addr_entry *pIps = NULL;

    // Make sure out device exists
    if (!dev_exist(psDeviceName)) {
        return (1);
    }
    // Retrieve the list of IPs installed on this system
    if (dev_get_ips(NULL, &pIps, &nbOfIps)) {
        return (1);
    }
    // See if we have this IP installed somewhere
    for (i = 0, needInstall = TRUE, found = FALSE; ((i < nbOfIps) && !found); i++) {
        // If this IP is found and its not on the same device, remove it
        if (pIps[i].address == address) {
            if (strcmp(pIps[i].sDevName, psDeviceName)) {
                // remove the IP. We will readd it shortly
                dev_remove_ip(devh, pIps[i].sDevName, pIps[i].address, pIps[i].netmask);
                found = TRUE;
            } else {
                needInstall = FALSE;
            }
        }
    }

    // Free our IP list
    dev_free_ips(&pIps);
    if (needInstall) {
        return (dev_install_ip(devh, psDeviceName, address, netmask, broadcast, psScope));
    }
    return (0);
}

/**
 * Moves a set of IP/netmask address from a device onto another given device. If any IP address is assigned
 * to another network device, this API will remove the IP address prior installing it on the given
 * network device.
 *
 * @param devh [in] pointer to the device handler
 * @param pIps [in] a pointer to the set of IP address entry to install
 * @param nbIps [in] the number of IP entry in the set
 * @param psScope [in] a constant string pointer to the scope of the address (SCOPE_GLOBAL, SCOPE_SITE, SCOPE_LINK, SCOPE_HOST)
 *
 * @return the number of IP address installed successfully from the set
 *
 * @see dev_move_ip(), dev_install_ip(), dev_remove_ip(), dev_remove_ips()
 *
 * @pre
 *     The pIps parameter should not be NULL and the scope should be valid
 *
 * @post
 *     The IP are installed/updated on their respective devices and removed from any other interface
 *     they may have been assigned too.
 *
 * @note
 */
int dev_move_ips(dev_handler *devh, in_addr_entry *pIps, int nbIps, const char *psScope) {
    int i = 0;
    int moved = 0;

    // Make sure we have a valid list
    if (!pIps)
        return (0);

    for (i = 0; i < nbIps; i++) {
        if (dev_move_ip(devh, pIps[i].sDevName, pIps[i].address, pIps[i].netmask, pIps[i].broascast, psScope) == 0) {
            moved++;
        }
    }

    return (moved);
}

/**
 * Remove a given ip/netmask association from a given device
 *
 * @param devh [in] pointer to the device handler
 * @param psDeviceName [in] a string pointer to the device name for which we are removing the address
 * @param address [in] the address to remove
 * @param netmask [in] the network mask associated with the address to remove
 *
 * @return 0 on success or 1 on failure
 *
 * @see dev_install_ip(), dev_install_ips(), dev_remove_ip()
 *
 * @pre
 *     The device must be a valid device on this system
 *
 * @post
 *     The IP is removed from their respective devices
 *
 * @note
 */
int dev_remove_ip(dev_handler *devh, const char *psDeviceName, in_addr_t address, in_addr_t netmask) {
    int rc = 0;
    u32 slashnet = NETMASK_TO_SLASHNET(netmask);
    char sHost[NETWORK_ADDR_LEN] = "";

    if (!devh) {
        LOGWARN("Invalid argument: null device handler\n");
        return (1);
    }
    // Make sure we have a valid device
    if (!dev_exist(psDeviceName)) {
        return (1);
    }
    // If this IP is not on the device, no-op
    if (!dev_has_host(psDeviceName, address, netmask)) {
        return (0);
    }

    snprintf(sHost, NETWORK_ADDR_LEN, "%s/%u", euca_ntoa(address), slashnet);
    if (euca_execlp(&rc, devh->cmdprefix, "ip", "addr", "del", sHost, "dev", psDeviceName, NULL) != EUCA_OK) {
        LOGERROR("Fail to remove host '%s' from network device '%s'. error=%d\n", sHost, psDeviceName, rc);
        return (1);
    }

    return (0);
}

/**
 * Remove a set of IP addresses.
 *
 * @param devh [in] pointer to the device handler
 * @param pIps [in] a pointer to the set of IP address entry to remove
 * @param nbIps [in] the number of IP entry in the set
 *
 * @return the number of IP address removed successfully from the set
 *
 * @see dev_install_ip(), dev_install_ips(), dev_remove_ip()
 *
 * @pre
 *     The pIps parameter should not be NULL
 *
 * @post
 *     The IPs are removed from their respective devices
 */
int dev_remove_ips(dev_handler *devh, in_addr_entry *pIps, int nbIps) {
    int i = 0;
    int removed = 0;

    // Make sure we have a valid list
    if (!pIps)
        return (0);

    for (i = 0; i < nbIps; i++) {
        if (dev_remove_ip(devh, pIps[i].sDevName, pIps[i].address, pIps[i].netmask) == 0)
            removed++;
    }

    return (removed);
}


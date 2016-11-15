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
//! @file util/euca_network.c
//! Implements various networking APIs used by Eucalyptus software
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
#include <pwd.h>
#include <dirent.h>
#include <errno.h>
#include <log.h>
#include <math.h>
#include <ctype.h>
#include <netdb.h>

#include <arpa/inet.h>
#include <net/if.h>
#include <net/ethernet.h>
#include <netinet/if_ether.h>
#include <netinet/ip.h>
#include <netinet/in.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/socket.h>

#include <eucalyptus.h>
#include <misc.h>
#include <euca_string.h>
#include <euca_network.h>
#include <euca_file.h>

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

//! List of supported network mode string
const char *asNetModes[] = {
    NETMODE_EDGE,
    NETMODE_VPCMIDO,
    NULL
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
//! This function converts the IPv4 Internet host address addr to a string in the
//! standard numbers-and-dots notation. The return value is a pointer into a
//! statically-allocated buffer. Subsequent calls will overwrite the same buffer,
//! so you should copy the string if you need to save it.
//!
//! In multi-threaded programs each thread has its own 8 statically-allocated buffer. But
//! still more than 8 subsequent calls of euca_ntoa in the same thread will overwrite
//! the result of the previous calls. if the result isn't saved.
//!
//! @param[in] address the address to convert
//!
//! @return A pointer to the statically-allocated buffer containing the dot representation
//!         of the address.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
const char *euca_ntoa(const in_addr_t address)
{
#define NB_BUFFER              8

    char *psIp = NULL;

    static u32 idx = 0;
    static char asDot[NB_BUFFER][INET_ADDR_LEN] = { "" };

    psIp = asDot[(idx++ % NB_BUFFER)];

    snprintf(psIp, INET_ADDR_LEN, "%u.%u.%u.%u", ((address & 0xFF000000) >> 24), ((address & 0x00FF0000) >> 16), ((address & 0x0000FF00) >> 8), (address & 0x000000FF));
    return (psIp);

#undef NB_BUFFER
}

//!
//! This function converts a given MAC address to a string in the standard notation. The
//! return value is a pointer into a statically-allocated buffer. Subsequent calls will
//! overwrite the same buffer, so you should copy the string if you need to save it.
//!
//! In multi-threaded programs each thread has its own 8 statically-allocated buffer. But
//! still more than 8 subsequent calls of euca_etoa in the same thread will overwrite
//! the result of the previous calls. if the result isn't saved.
//!
//! @param[in] pMac the MAC address to convert
//!
//! @return A pointer to the statically-allocated buffer containing the standard representation
//!         of the address.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
const char *euca_etoa(const u8 * pMac)
{
#define NB_BUFFER              8

    char *psMac = NULL;

    static u32 idx = 0;
    static char asDot[NB_BUFFER][ENET_ADDR_LEN] = { "" };

    psMac = asDot[(idx++ % NB_BUFFER)];

    snprintf(psMac, ENET_ADDR_LEN, "%02X:%02X:%02X:%02X:%02X:%02X", pMac[0], pMac[1], pMac[2], pMac[3], pMac[4], pMac[5]);
    return (psMac);

#undef NB_BUFFER
}

//!
//! Find an associated IP address for a given MAC address
//!
//! @param[in]  psMac a constant string pointer to the MAC address we want to find a matching IP
//! @param[out] psIp a pointer to a string buffer that will contain the matching IP when found
//!
//! @return EUCA_OK on success and \p psIp is set properly or the following error code:
//!         - EUCA_ACCESS_ERROR: if we cannot open the "/proc/net/arp" file for reading
//!         - EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         - EUCA_NOT_FOUND_ERROR: if the corresponding MAC isn't found in the ARP table of the system
//!
//! @pre
//!     Both pointers must not be NULL
//!
//! @post
//!     On success, the psIp field will point to a newly allocated string containing the matching IP address. On
//!     failure, the field will point to NULL and the proper error code will be returned.
//!
//! @note
//!     The (*psIp) field should be set to NULL already and the caller is responsible to free the allocated memory for ip.
//!
int euca_mac2ip(const char *psMac, char **psIp)
{
#define BUFFER_LEN      256

    int i = 0;
    char *psTok = NULL;
    char sRdBuf[BUFFER_LEN] = "";
    char sLowCaseBuf[BUFFER_LEN] = "";
    char sLowCaseMac[BUFFER_LEN] = "";
    FILE *pFh = NULL;

    // Make sure our given parameters aren't NULL
    if ((psMac == NULL) || (psIp == NULL)) {
        // If psIp is valid, then set it to NULL to meet our post criteria
        if (psIp)
            (*psIp) = NULL;
        return (EUCA_INVALID_ERROR);
    }
    // Caller should have freed memory prior
    (*psIp) = NULL;

    // open the ARP table
    if ((pFh = fopen("/proc/net/arp", "r")) == NULL) {
        return (EUCA_ACCESS_ERROR);
    }
    // Convert the MAC address to lower case
    // TODO: Use euca_strtolower() from euca_string.h
    bzero(sLowCaseMac, BUFFER_LEN);
    for (i = 0; i < strlen(psMac); i++) {
        sLowCaseMac[i] = tolower(psMac[i]);
    }

    // Scan the entire ARP table line-by-line to find the matching MAC
    while (fgets(sRdBuf, BUFFER_LEN, pFh) != NULL) {
        // Convert our buffer content to lower case
        // TODO: use euca_strtolower() from euca_string.h
        bzero(sLowCaseBuf, BUFFER_LEN);
        for (i = 0; i < strlen(sRdBuf); i++) {
            sLowCaseBuf[i] = tolower(sRdBuf[i]);
        }

        // Is the MAC address present in the buffer
        if (strstr(sLowCaseBuf, sLowCaseMac)) {
            // Retrieve the IP address from the line (should be the first token)
            if ((psTok = strtok(sLowCaseBuf, " ")) != NULL) {
                (*psIp) = strdup(psTok);
                fclose(pFh);
                return (EUCA_OK);
            }
        }
    }

    // Not found... Oups!
    fclose(pFh);
    return (EUCA_NOT_FOUND_ERROR);

#undef BUFFER_LEN
}

//!
//! Find an associated MAC address for a given IP address
//!
//! @param[in]  psIp a constant string pointer to the IP address we want to find a matching MAC
//! @param[out] psMac a pointer to a string buffer that will contain the matching MAC when found
//!
//! @return EUCA_OK on success and psMac is set properly or the following error code:
//!         - EUCA_ACCESS_ERROR: if we cannot open the "/proc/net/arp" file for reading
//!         - EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         - EUCA_NOT_FOUND_ERROR: if the corresponding IP isn't found in the ARP table of the system
//!
//! @pre
//!     psIp and psMac must not be NULL.
//!
//! @post
//!     On success, the psMac field will point to a newly allocated string containing the matching MAC address. On
//!     failure, the field will point to NULL and the proper error code will be returned.
//!
//! @note
//!     The (*psMac) field should be NULL already and the caller is responsible to free the allocated memory for psMac.
//!
int euca_ip2mac(const char *psIp, char **psMac)
{
#define BUFFER_LEN      256

    int count = 0;
    char *psTok = NULL;
    char sRdBuf[BUFFER_LEN] = "";
    char sIpSpace[INET_ADDR_LEN] = "";
    FILE *pFh = NULL;

    // Make sure our given parameters are valid
    if ((psMac == NULL) || (psIp == NULL)) {
        // To meet our post criteria
        if (psMac)
            (*psMac) = NULL;
        return (EUCA_INVALID_ERROR);
    }
    // Caller should have freed memory prior
    (*psMac) = NULL;

    // Open up the ARP table for reading
    if ((pFh = fopen("/proc/net/arp", "r")) == NULL) {
        return (EUCA_ACCESS_ERROR);
    }
    // Copy the IP into a local buffer and make sure we have a blank space at the end
    // so we don't match 192.168.1.2 for 192.168.1.20 or 192.168.1.200 (because we use
    // the strstr() API).
    snprintf(sIpSpace, INET_ADDR_LEN, "%s ", psIp);

    // Scan the entire ARP table line-by-line to find the matching IP
    while (fgets(sRdBuf, BUFFER_LEN, pFh) != NULL) {
        // Does this line contain our IP address?
        if (strstr(sRdBuf, sIpSpace)) {
            // Ok, split the line and the MAC address will be the 4th token on the line
            count = 0;
            psTok = strtok(sRdBuf, " ");
            while (psTok && (count < 4)) {
                count++;
                if (count < 4) {
                    psTok = strtok(NULL, " ");
                }
            }

            // Make sure the token is valid
            if (psTok != NULL) {
                (*psMac) = strdup(psTok);
                fclose(pFh);
                return (EUCA_OK);
            }
        }
    }

    // Not found. Oups!
    fclose(pFh);
    return (EUCA_NOT_FOUND_ERROR);

#undef BUFFER_LEN
}

//!
//! Converts a given host name to a matching IP address.
//!
//! @param[in]  psHostName a string pointer to the hostname that we are looking for
//! @param[out] psIp a pointer to a string buffer that will contain the matching IP or NULL
//!
//! @return EUCA_OK on success and psIp is set properly or the following error code:
//!         - EUCA_ERROR: if we fail to retrieve the hostname information details
//!         - EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         - EUCA_NOT_FOUND_ERROR: if the corresponding hostname isn't found on the system
//!
//! @pre
//!     psHostName must not be NULL and must be a valid hostname or IP address.
//!
//! @post
//!     On success, the psHostName field will point to a newly allocated string containing the
//!     matching IP address. On failure, the field will point to NULL and the proper error code will be returned.
//!
//! @note
//!     The caller is responsible to free the memory for the returned value
//!
int euca_host2ip(const char *psHostName, char **psIp)
{
#define BUFFER_LEN      256

    int rc = 0;
    int ret = EUCA_OK;
    char sHostBuf[BUFFER_LEN] = "";
    struct addrinfo hints = { 0 };
    struct addrinfo *pResult = NULL;

    // Make sure our parameters are valid
    if (!psHostName || !psIp) {
        // To meet our post criteria
        if (psIp)
            (*psIp) = NULL;
        return (EUCA_INVALID_ERROR);
    }
    // Check if we're looking for "localhost", this should speedup the process
    if (!strcmp(psHostName, "localhost")) {
        (*psIp) = strdup(LOOPBACK_IP_STRING);
        return (EUCA_OK);
    }
    // Caller should already have freed memory
    (*psIp) = NULL;

    // Check if we can find this hostname on the system
    bzero(&hints, sizeof(struct addrinfo));
    if ((rc = getaddrinfo(psHostName, NULL, &hints, &pResult)) == 0) {
        // Ok we know about this host
        rc = getnameinfo(pResult->ai_addr, pResult->ai_addrlen, sHostBuf, BUFFER_LEN, NULL, 0, NI_NUMERICHOST);
        if (!rc && ISDOTIP(sHostBuf)) {
            (*psIp) = strdup(sHostBuf);
        } else {
            ret = EUCA_ERROR;
        }
    } else {
        // Not found
        ret = EUCA_NOT_FOUND_ERROR;
    }

    // Free the address info if not NULL
    if (pResult)
        freeaddrinfo(pResult);

    return (ret);

#undef BUFFER_LEN
}

//!
//! Creates a MAC address based on the given prefix and instance identifier. The resulting MAC address
//! will be formated as following:
//!     - the first 2 bytes will be the prefix
//!     - the remaining 4 bytes will be the instance identifier numerical portion.
//!
//! @param[in]  psMacPrefix a string pointer to the MAC prefix to use for the first 2 bytes
//! @param[in]  psInstanceId a string pointer to the instance identifier to use for the remaining 4 bytes
//! @param[out] psOutMac a pointer to a string buffer that will contain the resulting MAC address
//!
//! @return EUCA_OK on success and the psOutMac field is set properly or the following error code:
//!         - EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre
//!     - psMacPrefix, psInstanceId and psOutMac must not be NULL.
//!     - psMacPrefix must be a valid string of the following format: xx:xx (where xx is any hexadecimal value)
//!     - psInstanceId must be a valid instance identifier string starting with "i-" followed by 8 digits
//!     - psOutMac must be a buffer capable of holding a minimum of ENET_ADDR_LEN characters
//!
//! @post
//!     On success, the psOutMac field will point to a newly allocated string containing the
//!     resulting MAC address. On failure, the field will point to NULL and the proper error code
//!     will be returned.
//!
//! @note
//!     The caller is responsible to free the memory for the returned value
//!
int euca_inst2mac(const char *psMacPrefix, const char *psInstanceId, char **psOutMac)
{
    int i = 0;
    char *p = NULL;
    char dst[ENET_ADDR_LEN] = "";

    // Make sure our given parameters are valid
    if (!psInstanceId || !psOutMac) {
        // to meet our post criteria
        if (psOutMac)
            (*psOutMac) = NULL;
        return (EUCA_INVALID_ERROR);
    }
    // Caller should have already freed the memory
    (*psOutMac) = NULL;

    // The instance identifier should be of 10 characters (e.g. i-xxxxxxxx)
    if (strlen(psInstanceId) != (INSTANCE_ID_LEN - 1)) {
        return (EUCA_INVALID_ERROR);
    }
    // Make sure the given instance identifier is valid and starts with an "i-"
    if ((p = strstr(psInstanceId, "i-")) == NULL) {
        return (EUCA_INVALID_ERROR);
    }
    // Skip the "i-" in the instance identifier
    p += 2;

    // Make sure we were give a MAC prefix
    if (!psMacPrefix) {
        return (EUCA_INVALID_ERROR);
    }
    // it should be at least 5 characters
    if (strlen(psMacPrefix) != (ENET_MACPREFIX_LEN - 1)) {
        return (EUCA_INVALID_ERROR);
    }
    // Concatenate both prefix and numerical portion of the instance identifier
    strncat(dst, psMacPrefix, ENET_MACPREFIX_LEN);
    for (i = 0; i < 4; i++) {
        strncat(dst, ":", 1);
        strncat(dst, p, 2);
        p += 2;
    }

    (*psOutMac) = strdup(dst);
    return (EUCA_OK);
}

//!
//! Checks wether or not a given IP is a valid readable IP format.
//!
//! @param[in] psIpAddr the IP field to validate
//!
//! @return TRUE if the given IP field is a Dotted IP formatted string. Otherwise, FALSE is returned
//!
//! @pre
//!     The psIpAddr field must not be NULL
//!
boolean euca_ip_is_dot(const char *psIpAddr)
{
    int a = 0;
    int b = 0;
    int c = 0;
    int d = 0;
    int rc = 0;

    // Make sure we didn't get a NULL
    if (!psIpAddr) {
        return (FALSE);
    }
    // Try and retrieve the 4 octets
    rc = sscanf(psIpAddr, "%d.%d.%d.%d", &a, &b, &c, &d);

    // Validate the result
    if ((rc != 4) || ((a < 0) || (a > 255)) || ((b < 0) || (b > 255)) || ((c < 0) || (c > 255)) || ((d < 0) || (d > 255))) {
        return (FALSE);
    }
    return (TRUE);
}

//!
//! Counts the number of known local IPs we are keeping track of on this system
//!
//! @param[in] pEucaNet a pointer to the eucalyptus network information structure
//!
//! @return the number of local IPs set
//!
//! @see euca_ip_add(), euca_ip_remove(), euca_ip_is_local()
//!
//! @pre
//!     The psEucaNet pointer must not be NULL
//!
//! @post
//!
//! @note
//!
u32 euca_ip_count(euca_network * pEucaNet)
{
    int i = 0;
    int count = 0;

    // Make sure pEucaNet isn't NULL
    if (pEucaNet) {
        // Count any non-0 IPs
        for (i = 0, count = 0; i < LOCAL_IP_SIZE; i++) {
            if (pEucaNet->aLocalIps[i] != 0) {
                count++;
            }
        }
    }
    return (count);
}

//!
//! Checks whether an IP address is local to this system or not
//!
//! @param[in] pEucaNet a pointer to the eucalyptus network information structure
//! @param[in] ip the IP address to validate
//!
//! @return TRUE if the IP address is a known local address or FALSE otherwise
//!
//! @see euca_ip_add(), euca_ip_remove(), euca_ip_count()
//!
//! @pre
//!     The pEucaNet field must not be NULL
//!
//! @post
//!
//! @note
//!
boolean euca_ip_is_local(euca_network * pEucaNet, in_addr_t ip)
{
    int i = 0;

    // local address? (127.0.0.0/8)
    if ((ip >= 0x7F000000) && (ip <= 0x7FFFFFFF))
        return (TRUE);

    // Is this a known local IP from our known list?
    for (i = 0; i < LOCAL_IP_SIZE; i++) {
        if (pEucaNet->aLocalIps[i] == ip) {
            return (TRUE);
        }
    }

    // Nope
    return (FALSE);
}

//!
//! Adds an IP address to our known local IP address list
//!
//! @param[in] pEucaNet a pointer to the eucalyptus network information structure
//! @param[in] ip the IP address to add to the list
//!
//! @return 0 on success or 1 if any failure occured
//!
//! @see euca_ip_remove(), euca_ip_is_local(), euca_ip_count()
//!
//! @pre
//!     The pEucaNet field must not be NULL
//!
//! @post
//!
//! @note
//!
int euca_ip_add(euca_network * pEucaNet, u32 ip)
{
    int i = 0;

    // Check if we know of this IP first
    if (euca_ip_is_local(pEucaNet, ip)) {
        return (0);
    }
    // Find the next available slot
    for (i = 0; i < LOCAL_IP_SIZE; i++) {
        if (pEucaNet->aLocalIps[i] == 0) {
            pEucaNet->aLocalIps[i] = ip;
            return (0);
        }
    }

    // No empty slot
    return (1);
}

//!
//! removes an IP address from our known local IP address list
//!
//! @param[in] pEucaNet a pointer to the eucalyptus network information structure
//! @param[in] ip the IP address to remove from the list
//!
//! @return 0 on success or 1 if any failure occured
//!
//! @see euca_ip_add(), euca_ip_is_local(), euca_ip_count()
//!
//! @pre
//!     The pEucaNet field must not be NULL
//!
//! @post
//!
//! @note
//!
int euca_ip_remove(euca_network * pEucaNet, u32 ip)
{
    int i = 0;

    // Find the IP in our list
    for (i = 0; i < LOCAL_IP_SIZE; i++) {
        if (pEucaNet->aLocalIps[i] == ip) {
            pEucaNet->aLocalIps[i] = 0;
            return (0);
        }
    }

    // Not found
    return (1);
}

//!
//! Finds a network device associated with a given MAC address
//!
//! @param[in] mac a string pointer to the MAC address we are looking for
//!
//! @return a string pointer containing the network device name.
//!
//! @see euca_intfc2mac()
//!
//! @pre the given \p mac string pointer must not be NULL
//!
//! @post
//!
//! @note The string is allocated dynamically and the caller is responsible for freeing
//!       the allocated memory.
//!
char *euca_mac2intfc(const char *psMac)
{
#define READ_BUFFER_SIZE       64

    int rc = 0;
    char *pIfName = NULL;
    char *pStrA = NULL;
    char *pStrB = NULL;
    char sMacBuf[READ_BUFFER_SIZE] = "";
    char sMacFile[EUCA_MAX_PATH] = "";
    DIR *pDir = NULL;
    FILE *pFile = NULL;
    boolean match = FALSE;
    struct dirent dirEnt = { 0 };
    struct dirent *pResult = NULL;

    // Make sure the given MAC address is not NULL
    if (!psMac) {
        return (NULL);
    }
    // We'll look through the interfaces under the /sys/class/net location
    if ((pDir = opendir("/sys/class/net/")) != NULL) {
        rc = readdir_r(pDir, &dirEnt, &pResult);
        while (!match && !rc && pResult) {
            if (strcmp(pResult->d_name, ".") && strcmp(pResult->d_name, "..")) {
                snprintf(sMacFile, EUCA_MAX_PATH, "/sys/class/net/%s/address", pResult->d_name);
                LOGTRACE("attempting to read mac from file '%s'\n", sMacFile);
                if ((pFile = fopen(sMacFile, "r")) != NULL) {
                    sMacBuf[0] = '\0';
                    if (fgets(sMacBuf, READ_BUFFER_SIZE, pFile)) {
                        // remove the '\n' character.
                        if ((pStrA = strchr(sMacBuf, '\n')) != NULL)
                            *pStrA = '\0';

                        // Skip the first byte since it does not seem to agree with what we want
                        pStrA = strchr(sMacBuf, ':');
                        pStrB = strchr(psMac, ':');
                        if (pStrA && pStrB) {
                            if (!strcasecmp(pStrA, pStrB)) {
                                pIfName = strdup(pResult->d_name);
                                LOGDEBUG("found: matching mac/interface mapping: interface=%s foundmac=%s inputmac=%s\n", SP(pIfName), SP(pStrA), SP(pStrB));
                                match = TRUE;
                            }
                        } else {
                            LOGDEBUG("skipping: parse error extracting mac (malformed) from sys interface file: file=%s macstr=%s\n", sMacFile, sMacBuf);
                            pIfName = NULL;
                        }
                    } else {
                        LOGDEBUG("skipping: parse error extracting mac from sys interface file: file=%s fscanf_rc=%d\n", sMacFile, rc);
                        pIfName = NULL;
                    }

                    fclose(pFile);
                } else {
                    LOGDEBUG("skipping: could not open sys interface file for read '%s': check permissions\n", sMacFile);
                    pIfName = NULL;
                }
            }
            // Move to the next interface
            rc = readdir_r(pDir, &dirEnt, &pResult);
        }

        closedir(pDir);
    } else {
        LOGERROR("could not open sys dir for read '/sys/class/net/': check permissions\n");
        pIfName = NULL;
    }

    return (pIfName);

#undef READ_BUFFER_SIZE
}

//!
//! Finds the MAC address associated with a given network device.
//!
//! @param[in] dev a string pointer to the device name
//!
//! @return a string pointer containing the MAC address string value
//!
//! @see euca_mac2intfc()
//!
//! @pre The given \p dev string pointer must not be NULL
//!
//! @post
//!
//! @note The string is allocated dynamically and the caller is responsible for freeing
//!       the allocated memory.
//!
char *euca_intfc2mac(const char *psDevName)
{
    char *psMac = NULL;
    char *pNewLine = NULL;
    char devpath[EUCA_MAX_PATH] = "";

    // Make sure we were given a valid device name
    if (!psDevName) {
        return (NULL);
    }
    // Get the MAC from the device address file
    snprintf(devpath, EUCA_MAX_PATH, "/sys/class/net/%s/address", psDevName);
    psMac = file2str(devpath);

    // remove the '\n' character.
    if ((pNewLine = strchr(psMac, '\n')) != NULL)
        (*pNewLine) = '\0';

    return (psMac);
}

/**
 * Converts a string description of network mode to euca_netmode code.
 * EDGE and VPCMIDO are recognized.
 * @param psNetMode [in] a string representation of a network mode.
 * @return the euca_netmode code of the given network mode string
 */
euca_netmode euca_netmode_atoi(const char *psNetMode) {
    if (!strcmp(psNetMode, NETMODE_EDGE)) {
        return NM_EDGE;
    }
    if (!strcmp(psNetMode, NETMODE_VPCMIDO)) {
        return NM_VPCMIDO;
    }

    return (NM_INVALID);
}

/**
 * Gets the IP address that corresponds to the given hostname. The IP address is
 * determined through system name resolution. If an IP address is detected in the
 * input hostname, no action is taken. The resulting IP address is returned in ipout,
 * which is allocated using malloc(), and the caller is responsible to release this memory.
 * @param hostname [in] the host/IP of interest
 * @param ipout [out] optional pointer to a string that will contain the resulting
 * IP address
 * @return hex representation of the resulting IP address. 0 can be returned on
 * error or if hostname corresponds to 0.0.0.0
 */
u32 euca_getaddr(const char *hostname, char **ipout) {
    struct addrinfo hints = { 0 };
    struct addrinfo *ainfo = NULL;
    struct addrinfo *p = NULL;
    struct sockaddr_in *sa = NULL;
    char res[INET_ADDR_LEN] = { 0 };
    int rc = 0;
    
    if (ISDOTIP(hostname)) {
        snprintf(res, INET_ADDR_LEN, "%s", hostname);
    } else {
        hints.ai_family = AF_INET;
        hints.ai_socktype = SOCK_STREAM;
        
        rc = getaddrinfo(hostname, NULL, &hints, &ainfo);
        if (rc) {
            LOGWARN("Failed to resolve %s: %s\n", hostname, gai_strerror(rc));
            return (0);
        }
        
        for (p = ainfo; p != NULL; p = p->ai_next) {
            sa = (struct sockaddr_in *) p->ai_addr;
            snprintf(res, INET_ADDR_LEN, "%s", inet_ntoa(sa->sin_addr));
            // Use the first entry of the results
            break;
        }
        freeaddrinfo(ainfo);
    }
    
    if (ipout) {
        *ipout = strdup(res);
    }
    return (dot2hex(res));
}

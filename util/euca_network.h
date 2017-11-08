// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

#ifndef _INCLUDE_EUCA_NETWORK_H_
#define _INCLUDE_EUCA_NETWORK_H_

//!
//! @file util/euca_network.h
//! Defines various networking API used by Eucalyptus.
//!
//! For every constant definition, use the following naming rules. When defining
//! the size of an array or byte buffer, use the suffix "_SIZE". When defining a
//! constant represending the length of a string INCLUDING the NULL terminating
//! character, use the suffix "_LEN".
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <math.h>
#include <net/if.h>
#include <net/ethernet.h>
#include <netinet/if_ether.h>
#include <netinet/ip.h>
#include <netinet/in.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name Defines the various supported network mode names

#define NETMODE_EDGE                             "EDGE"           //!< EDGE networking mode
#define NETMODE_MANAGED                          "MANAGED"        //!< MANAGED networking mode
#define NETMODE_MANAGED_NOVLAN                   "MANAGED-NOVLAN" //!< MANAGED-NOVLAN networking mode
#define NETMODE_VPCMIDO                          "VPCMIDO"        //!< MIDONET VPC networking mode
#define NETMODE_SYSTEM                           "SYSTEM"         //!< SYSTEM networking mode
#define NETMODE_STATIC                           "STATIC"         //!< STATIC networking mode
#define NETMODE_INVALID                          "INVALID"        //!< INVALID networking mode

//! @}

//! Defines the length and size of network mode string name
#define NETMODE_LEN                              32

//! Defines the length and size of a network interface name
#define IF_NAME_LEN                              IFNAMSIZ

//! Defines the size in bytes of an Ethernet address (MAC) buffer
#define ENET_BUF_SIZE                            6

//! Defines the length and maximum size of an Ethernet address (MAC) string
#define ENET_ADDR_LEN                            18

//! Defines the length of an Ethernet MAC prefix String. The MAC prefix must be of the xx:xx\0 format (e.g. 00:00)
#define ENET_MACPREFIX_LEN                       6

//! Defines the length of an Internet Address (IP) string
#define INET_ADDR_LEN                            INET_ADDRSTRLEN

//! Defines the length of a HOST address string AAA.BBB.CCC.DDD/XX
#define NETWORK_ADDR_LEN                         (INET_ADDR_LEN + 3)

//! Defines the maximum supported hostname string length
//! TODO: Replace with the value here
#define HOSTNAME_LEN                             256

//! Defines the maximum supported length of uri
#define URI_LEN                                  2048

//! @{
//! @name Various known IP address strings

#define LOOPBACK_IP_STRING                       "127.0.0.1"    //!< Loopback string representation
#define METADATA_IP_STRING                       "169.254.169.254"  //!< Metadata IP address representation

//! @}

//! The number of LOCAL IPs to keep track of
#define LOCAL_IP_SIZE                            32
#define LOCAL_IP_BUF_SIZE                        32

//! @{
//! @name Definition of various VLAN related constants

#define MIN_VLAN_802_1Q                             0   //!< 802.1Q minimum VLAN index
#define MAX_VLAN_802_1Q                          4095   //!< 802.1Q maximum VLAN index
#define NB_VLAN_802_1Q                           4096   //!< 802.1Q number of supported VLAN
#define MIN_VLAN_EUCA                               2   //!< We tend to skip vlan 0 and 1
#define MAX_VLAN_EUCA                            MAX_VLAN_802_1Q    //!< the high end of our VLAN range

//! @}

//! @{
//! @name Definition of various Eucalyptus networking components information constants

#define NUMBER_OF_CCS                             8 //!< Maximum number of CC to keep track of in a single cloud
#define NUMBER_OF_NAME_SERVERS                   32 //!< Number of domain name server to manage for VMs
#define MAX_ETH_DEV_PATH                         16
#define MAX_SEC_GROUPS                           NB_VLAN_802_1Q //!< Maximum number of security group supported

//! @}

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
//! Enumeration of eucanetd network modes
typedef enum euca_netmode_t {
    NM_INVALID = 0,
    NM_EDGE = 1,
    NM_STATIC = 2,
    NM_SYSTEM = 3,
    NM_MANAGED = 4,
    NM_MANAGED_NOVLAN = 5,
    NM_VPCMIDO = 6,
    NM_MAX = 7,
} euca_netmode;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Global Eucalyptus Network Specific Information Structure
typedef struct euca_network_t {
    char sMode[NETMODE_LEN];           //!< Network mode name
    char sPublicDevice[IF_NAME_LEN];   //!< Name of the public device
    char sPrivateDevice[IF_NAME_LEN];  //!< Name of the public device
    char sBridgeDevice[IF_NAME_LEN];   //!< Name of the bridge device
    char sMacPrefix[ENET_MACPREFIX_LEN];    //!< Mac prefix to combine with the host IP
    u8 aReserved[3];                   //!< for 32 bits alignment
    in_addr_t cloudIp;                 //!< IP address of the CLC communicating with us
    in_addr_t aLocalIps[LOCAL_IP_BUF_SIZE]; //!< List of local IPs residing on this system
} euca_network;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! List of supported network mode string
extern const char *asNetModes[];

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! @{
//! @name APIs to convert IP and MAC addresses to strings
const char *euca_ntoa(const in_addr_t address);
const char *euca_etoa(const u8 * pMac);
//! @}

//! @{
//! @name APIs to convert between MAC, HOST, IP, etc.
int euca_mac2ip(const char *psMac, char **psIp);
int euca_ip2mac(const char *psIp, char **psMac);
int euca_host2ip(const char *psHostName, char **psIp);
int euca_inst2mac(const char *psMacPrefix, const char *psInstanceId, char **psOutMac);
//! @}

//! Validate wether or not a dot IP is a valid address
boolean euca_ip_is_dot(const char *psIpAddr);

//! @{
//! @name Local IP list APIs
u32 euca_ip_count(euca_network * pEucaNet);
boolean euca_ip_is_local(euca_network * pEucaNet, in_addr_t ip);
int euca_ip_add(euca_network * pEucaNet, u32 ip);
int euca_ip_remove(euca_network * pEucaNet, u32 ip);
//! @}

//! @{
//! @name APIs to find interfaces based on mac and vice-versa
char *euca_mac2intfc(const char *psMac);
char *euca_intfc2mac(const char *psDevName);
//! @}

//! @{
//! @name APIs to manipulate network mode names
euca_netmode euca_netmode_atoi(const char *psNetMode);
//! @}

u32 euca_getaddr(const char *hostname, char **ipout);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! Macro to retrieve the size of a netmask in bits
#define NETMASK_TO_SLASHNET(_netmask)            (32 - ((int)(log2((double)((0xFFFFFFFF - (_netmask)) + 1)))))

//! Macro alias to euca_mac2ip()
#define MAC2IP(_mac, _ip)                        euca_mac2ip((_mac), (_ip))

//! Macro alias to euca_ip2mac()
#define IP2MAC(_ip, _mac)                        euca_ip2mac((_ip), (_mac))

//! Macro alias to euca_host2ip()
#define HOST2IP(_host, _mac)                     euca_host2ip((_host), (_mac))

//! Macro alias to euca_ip_is_valid()
#define ISDOTIP(_ip)                             euca_ip_is_dot((_ip))

//! Macro alias to euca_mac2intfc()
#define MAC2INTFC(_mac)                          euca_mac2intfc((_mac))

//! Macro alias to euca_intfc2mac()
#define INTFC2MAC(_ifname)                       euca_intfc2mac((_ifname))

//! @{
//! @name Macro aliases to work with local IP array

#define ADD_LOCAL_IP(_ip)                        euca_ip_add((_ip))
#define REMOVE_LOCAL_IP(_ip)                     euca_ip_remove((_ip))
#define IS_LOCAL_IP(_ip)                         euca_ip_is_local((_ip))
#define COUNT_LOCAL_IP()                         euca_ip_count()

//! @}

//! @{
//! @name APIs to check networking mode
//! parameter expected to be a pointer to a structure with a valid nmCode field,
//! with enumeration euca_netmode_t type.
#define IS_NETMODE_VALID(_p)             (((_p->nmCode) > NM_INVALID) && ((_p->nmCode) < NM_MAX))
#define IS_NETMODE_EDGE(_p)              ((_p->nmCode) == NM_EDGE)
#define IS_NETMODE_VPCMIDO(_p)           ((_p->nmCode) == NM_VPCMIDO)
#define IS_NETMODE_MANAGED(_p)           ((_p->nmCode) == NM_MANAGED)
#define IS_NETMODE_MANAGED_NOVLAN(_p)    ((_p->nmCode) == NM_MANAGED_NOVLAN)
//! @}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCA_NETWORK_H_ */

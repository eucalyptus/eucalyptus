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
//! @file
//! Implements the Virtual Network library.
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define _FILE_OFFSET_BITS 64           // so large-file support works on 32-bit systems
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <math.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pthread.h>
#include <sys/types.h>
#include <fcntl.h>
#include <stdarg.h>
#include <ifaddrs.h>
#include <math.h>                      /* log2 */
#include <sys/socket.h>
#include <netdb.h>
#include <arpa/inet.h>

#include <sys/ioctl.h>
#include <net/if.h>

#include <eucalyptus.h>
#include <misc.h>
#include <data.h>
#include <vnetwork.h>
#include <hash.h>

#include <fault.h>
#include <euca_string.h>

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

char *iptablesCache = NULL;            //!< contains the IP tables cache

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
//! Initialize the given network configuration structure based on the given parameters. This API validates the given parameters
//! prior initializing the network configuration structure.
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] mode the network mode (ie. SYSTEM, STATIC, EDGE, MANAGED-NOVLAN, MANAGED)
//! @param[in] eucahome
//! @param[in] path
//! @param[in] role
//! @param[in] pubInterface
//! @param[in] privInterface
//! @param[in] numberofaddrs
//! @param[in] network
//! @param[in] netmask
//! @param[in] broadcast
//! @param[in] nameserver
//! @param[in] domainname
//! @param[in] router
//! @param[in] daemon
//! @param[in] dhcpuser
//! @param[in] bridgedev
//! @param[in] localIp
//! @param[in] macPrefix
//!
//! @return EUCA_OK on success or proper error code on failure. Known error code returned include:
//!             \li EUCA_ERROR: For any other issues executing this function
//!             \li EUCA_INVALID_ERROR: If any parameter does not meet the preconditions
//!
//! @pre \li \p vnetconfig, \p mode and \p role must not be NULL
//!      \li \p vnetconfig, if not initialized already, should have been zeroed out prior calling this API.
//!
//! @post On success, the passed \p vnetconfig structure has been set appropriately. On failure, the structure
//!       content is non-deterministic and should not be used moving forward.
//!
int vnetInit(vnetConfig * vnetconfig, char *mode, char *eucahome, char *path, int role, char *pubInterface, char *privInterface, char *numberofaddrs,
             char *network, char *netmask, char *broadcast, char *nameserver, char *domainname, char *router, char *daemon, char *dhcpuser,
             char *bridgedev, char *localIp, char *macPrefix)
{
    u32 nw = 0;
    u32 nm = 0;
    u32 unw = 0;
    u32 unm = 0;
    u32 dns = 0;
    u32 bc = 0;
    u32 rt = 0;
    u32 rc = 0;
    int vlan = 0;
    int bits = 1;
    int numaddrs = 1;
    int numberofaddrs_i = 0;
    char cmd[256] = "";
    char *ipbuf = NULL;

    if (vnetconfig == NULL) {
        LOGERROR("bad input params: vnetconfig:%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    if (!vnetconfig->initialized) {
        // always need 'mode' set
        if (mode) {
            euca_strncpy(vnetconfig->mode, mode, 32);
        } else {
            LOGERROR("VNET_MODE is not set\n");
            return (EUCA_INVALID_ERROR);
        }

        if (role != CLC && role != NC) {
            LOGERROR("bad role specified\n");
            return (EUCA_INVALID_ERROR);
        }
        //check mode specific parameters
        if (!strcmp(mode, NETMODE_SYSTEM)) {
            if (role == CLC) {
            } else if (role == NC) {
                if (!bridgedev || check_bridge(bridgedev)) {
                    LOGERROR("cannot verify VNET_BRIDGE(%s), please check parameters and bridge device\n", SP(bridgedev));
                    return (EUCA_ERROR);
                }
            }
        } else if (!strcmp(mode, NETMODE_STATIC)) {
            if (role == CLC) {
                if (!daemon || check_file(daemon)) {
                    LOGERROR("cannot verify VNET_DHCPDAEMON (%s), please check parameter and location\n", SP(daemon));
                    return (EUCA_ERROR);
                }
                if (!privInterface || check_device(privInterface)) {
                    LOGERROR("cannot verify VNET_PRIVINTERFACE (%s), please check parameter and device name\n", SP(privInterface));
                    return (EUCA_ERROR);
                }
                if (!network || !netmask || !broadcast || !nameserver || !router) {
                    LOGERROR("cannot verify network settings (VNET_SUBNET(%s), VNET_NETMASK(%s), VNET_BROADCAST(%s), VNET_DNS(%s), VNET_ROUTER(%s)), "
                             "please check parameters\n", SP(network), SP(netmask), SP(broadcast), SP(nameserver), SP(router));
                    return (EUCA_ERROR);
                }
            }
        } else if (!strcmp(mode, NETMODE_EDGE)) {
            if (role == NC) {
                if (!pubInterface || check_device(pubInterface)) {
                    LOGERROR("cannot verify VNET_PUBINTERFACE(%s), please check parameters and device\n", SP(pubInterface));
                    return (EUCA_ERROR);
                }
            }
        } else if (!strcmp(mode, NETMODE_MANAGED_NOVLAN)) {
            if (role == CLC) {
                if (!daemon || check_file(daemon)) {
                    LOGERROR("cannot verify VNET_DHCPDAEMON (%s), please check parameter and location\n", SP(daemon));
                    return (EUCA_ERROR);
                }
                if (!pubInterface || check_device(pubInterface)) {
                    LOGERROR("cannot verify VNET_PUBINTERFACE (%s), please check parameter and device name\n", SP(pubInterface));
                    return (EUCA_ERROR);
                }
                if (!privInterface || check_device(privInterface)) {
                    LOGERROR("cannot verify VNET_PRIVINTERFACE (%s), please check parameter and device name\n", SP(privInterface));
                    return (EUCA_ERROR);
                }
                if (!network || !netmask || !nameserver) {
                    LOGERROR("cannot verify network settings (VNET_SUBNET(%s), VNET_NETMASK(%s), VNET_DNS(%s), please check parameters\n",
                             SP(network), SP(netmask), SP(nameserver));
                    return (EUCA_ERROR);
                }
            } else if (role == NC) {
                if (!bridgedev || check_bridge(bridgedev)) {
                    LOGERROR("cannot verify VNET_BRIDGE(%s), please check parameters and bridge device\n", SP(bridgedev));
                    return (EUCA_ERROR);
                }
            }
        } else if (!strcmp(mode, NETMODE_MANAGED)) {
            if (role == CLC) {
                if (!daemon || check_file(daemon)) {
                    LOGERROR("cannot verify VNET_DHCPDAEMON (%s), please check parameter and location\n", SP(daemon));
                    return (EUCA_ERROR);
                }
                if (!pubInterface || check_device(pubInterface)) {
                    LOGERROR("cannot verify VNET_PUBINTERFACE (%s), please check parameter and device name\n", SP(pubInterface));
                    return (EUCA_ERROR);
                }
                if (!privInterface || check_device(privInterface)) {
                    LOGERROR("cannot verify VNET_PRIVINTERFACE (%s), please check parameter and device name\n", SP(privInterface));
                    return (EUCA_ERROR);
                }
                if (!network || !netmask || !nameserver) {
                    LOGERROR("cannot verify network settings (VNET_SUBNET(%s), VNET_NETMASK(%s), VNET_DNS(%s)), please check parameters\n",
                             SP(network), SP(netmask), SP(nameserver));
                    return (EUCA_ERROR);
                }
            } else if (role == NC) {
            }
        } else {
            LOGERROR("invalid networking mode %s, please check VNET_MODE parameter\n", SP(mode));
            return (EUCA_INVALID_ERROR);
        }

        if (macPrefix)
            euca_strncpy(vnetconfig->macPrefix, macPrefix, 6);

        if (path)
            euca_strncpy(vnetconfig->path, path, EUCA_MAX_PATH);

        if (eucahome)
            euca_strncpy(vnetconfig->eucahome, eucahome, EUCA_MAX_PATH);

        if (pubInterface)
            euca_strncpy(vnetconfig->pubInterface, pubInterface, 32);

        if (bridgedev)
            euca_strncpy(vnetconfig->bridgedev, bridgedev, 32);

        if (daemon)
            euca_strncpy(vnetconfig->dhcpdaemon, daemon, EUCA_MAX_PATH);

        if (privInterface)
            euca_strncpy(vnetconfig->privInterface, privInterface, 32);

        if (dhcpuser)
            euca_strncpy(vnetconfig->dhcpuser, dhcpuser, 32);

        if (domainname) {
            euca_strncpy(vnetconfig->eucaDomainName, domainname, sizeof(vnetconfig->eucaDomainName));
        } else {
            strncpy(vnetconfig->eucaDomainName, "eucalyptus", strlen("eucalyptus") + 1);
        }

        if (localIp) {
            if ((ipbuf = host2ip(localIp)) != NULL) {
                vnetAddLocalIP(vnetconfig, dot2hex(ipbuf));
                EUCA_FREE(ipbuf);
            }
        }

        vnetconfig->tunnels.localIpId = -1;
        vnetconfig->tunnels.localIpIdLast = -1;
        vnetconfig->tunnels.tunneling = 0;
        vnetconfig->role = role;
        vnetconfig->enabled = TRUE;
        vnetconfig->initialized = TRUE;
        vnetconfig->max_vlan = NUMBER_OF_VLANS;
        if (numberofaddrs) {
            numberofaddrs_i = atoi(numberofaddrs);
            if (numberofaddrs_i > NUMBER_OF_HOSTS_PER_VLAN) {
                LOGWARN("specified ADDRSPERNET exceeds maximum addresses per network (%d), setting to maximum.\n", NUMBER_OF_HOSTS_PER_VLAN);
                vnetconfig->numaddrs = NUMBER_OF_HOSTS_PER_VLAN;
                log_eucafault("1001", "component", "CC", NULL);
            } else if (numberofaddrs_i <= NUMBER_OF_CCS) {
                //! @fixme Why is NUMBER_OF_CCS not hard-coded here, but the absolute minimum setting of 16 is?
                //! (Note: this 16 also appears hard-coded in the non-power-of-2 case.)
                LOGWARN("specified ADDRSPERNET lower than absolute minimum (16), setting to minimum.\n");
                vnetconfig->numaddrs = 16;
                log_eucafault("1001", "component", "CC", NULL);
            } else if (numberofaddrs_i && (!(numberofaddrs_i & (numberofaddrs_i - 1)))) {
                // Is a legal power of 2.
                vnetconfig->numaddrs = numberofaddrs_i;
            } else {
                bits = 1;
                while (numberofaddrs_i >>= 1) {
                    bits <<= 1;
                }
                // Not a power of 2, so reduce to next power of 2 (but not below 16).
                //! @fixme Use real address here!
                vnetconfig->numaddrs = bits < 16 ? 16 : bits;
                LOGWARN("specified ADDRSPERNET not a power of 2, setting to next lower power of 2 (%d).\n", vnetconfig->numaddrs);
                log_eucafault("1001", "component", "CC", NULL);
            }
        }

        vnetconfig->addrIndexMin = NUMBER_OF_CCS + 1;
        vnetconfig->addrIndexMax = vnetconfig->numaddrs - 2;

        if (network)
            vnetconfig->nw = dot2hex(network);

        if (netmask)
            vnetconfig->nm = dot2hex(netmask);

        // populate networks
        bzero(vnetconfig->users, sizeof(userEntry) * NUMBER_OF_VLANS);
        bzero(vnetconfig->networks, sizeof(networkEntry) * NUMBER_OF_VLANS);
        bzero(vnetconfig->etherdevs, NUMBER_OF_VLANS * MAX_ETH_DEV_PATH);
        bzero(vnetconfig->publicips, sizeof(publicip) * NUMBER_OF_PUBLIC_IPS);

        if (role != NC) {
            if (network)
                nw = dot2hex(network);

            if (netmask)
                nm = dot2hex(netmask);

            if (nameserver)
                dns = dot2hex(nameserver);

            if (broadcast)
                bc = dot2hex(broadcast);

            if (router)
                rt = dot2hex(router);

            if (numberofaddrs)
                numaddrs = atoi(numberofaddrs);

            numaddrs -= 1;
            if (!strcmp(mode, NETMODE_MANAGED) || !strcmp(mode, NETMODE_MANAGED_NOVLAN)) {
                // do some parameter checking
                if ((numaddrs + 1) < 4) {
                    LOGERROR("NUMADDRS must be >= 4, instances will not start with current value of '%d'\n", numaddrs + 1);
                }
                // check to make sure our specified range is big enough for all VLANs
                if ((0xFFFFFFFF - nm) < (NUMBER_OF_VLANS * (numaddrs + 1))) {
                    // not big enough
                    vnetconfig->max_vlan = (0xFFFFFFFF - nm) / (numaddrs + 1);
                    LOGWARN("private network is not large enough to support all vlans, restricting to max vlan '%d'\n", vnetconfig->max_vlan);
                    if (vnetconfig->max_vlan < 2) {
                        LOGWARN("Instances will not run with current max vlan '%d'.  Either increase the size of your private subnet "
                                "(VNET_SUBNET/VNET_NETMASK) or decrease the number of addrs per group (VNET_ADDRSPERNET).\n", vnetconfig->max_vlan);
                    }
                } else {
                    vnetconfig->max_vlan = NUMBER_OF_VLANS;
                }

                // set up iptables
                snprintf(cmd, 256, EUCALYPTUS_ROOTWRAP " iptables -L -n", vnetconfig->eucahome);
                rc = system(cmd);

                // Initialize our IP Tables
                vnetIptReInit(vnetconfig, TRUE);

                unm = 0xFFFFFFFF - numaddrs;
                unw = nw;
                for (vlan = 2; vlan < vnetconfig->max_vlan; vlan++) {
                    vnetconfig->networks[vlan].nw = unw;
                    vnetconfig->networks[vlan].nm = unm;
                    vnetconfig->networks[vlan].bc = unw + numaddrs;
                    vnetconfig->networks[vlan].dns = dns;
                    vnetconfig->networks[vlan].router = unw + 1;
                    unw += numaddrs + 1;
                }
            } else if (!strcmp(mode, NETMODE_STATIC)) {
                for (vlan = 0; vlan < vnetconfig->max_vlan; vlan++) {
                    vnetconfig->networks[vlan].nw = nw;
                    vnetconfig->networks[vlan].nm = nm;
                    vnetconfig->networks[vlan].bc = bc;
                    vnetconfig->networks[vlan].dns = dns;
                    vnetconfig->networks[vlan].router = rt;
                    vnetconfig->numaddrs = 0xFFFFFFFF - nm;
                    if (vnetconfig->numaddrs > NUMBER_OF_PUBLIC_IPS) {
                        vnetconfig->numaddrs = NUMBER_OF_PUBLIC_IPS;
                    }
                    vnetconfig->addrIndexMin = NUMBER_OF_CCS + 1;
                    vnetconfig->addrIndexMax = vnetconfig->numaddrs - 2;
                }
            } else if (!strcmp(mode, NETMODE_EDGE)) {
                vnetconfig->numaddrs = NUMBER_OF_PUBLIC_IPS;
                vnetconfig->addrIndexMin = NUMBER_OF_CCS + 1;
                vnetconfig->addrIndexMax = vnetconfig->numaddrs - 2;
            }
        } else {
            // This is the NC, we need to setup some IPT rules...
            if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM)) {
                // set up iptables rule to log DHCP replies to syslog
                snprintf(cmd, 256, "-A FORWARD -p udp -m udp --sport 67:68 --dport 67:68 -j LOG --log-level 6");
                if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
                    LOGWARN("could not add logging rule for DHCP replies, may not see instance IPs as they are assigned by system DHCP server");
                }
            }

            if (strcmp(vnetconfig->mode, NETMODE_MANAGED)) {
                /*
                   // if we're not in MANAGED mode, set up ebtables
                   snprintf(cmd, 256, EUCALYPTUS_ROOTWRAP " ebtables -F FORWARD", vnetconfig->eucahome);
                   rc = system(cmd);
                   if (rc) {
                   LOGWARN("could not flush ebtables FORWARD rules\n");
                   }

                   snprintf(cmd, 256, EUCALYPTUS_ROOTWRAP " ebtables -P FORWARD DROP", vnetconfig->eucahome);
                   rc = system(cmd);
                   if (rc) {
                   LOGWARN("could set default ebtables FORWARD policy to DROP\n");
                   }

                   // forward non-VM traffic
                   snprintf(cmd, 256, "-A FORWARD -i %s -j ACCEPT", vnetconfig->pubInterface);
                   rc = vnetApplySingleEBTableRule(vnetconfig, "filter", cmd);
                   if (rc) {
                   LOGWARN("could set up default ebtables rule '%s'\n", cmd);
                   }

                   // allow VM DHCP traffic
                   snprintf(cmd, 256, "-A FORWARD -p IPv4 -d Broadcast -i ! %s --ip-proto udp --ip-dport 67:68 -j ACCEPT", vnetconfig->pubInterface);
                   rc = vnetApplySingleEBTableRule(vnetconfig, "filter", cmd);
                   if (rc) {
                   LOGWARN("could set up default ebtables rule '%s'\n", cmd);
                   }
                 */
            }
        }

        LOGINFO(" VNET Configuration: eucahome=%s,\n", SP(vnetconfig->eucahome));
        LOGINFO("                     path=%s,\n", SP(vnetconfig->path));
        LOGINFO("                     dhcpdaemon=%s,\n", SP(vnetconfig->dhcpdaemon));
        LOGINFO("                     dhcpuser=%s,\n", SP(vnetconfig->dhcpuser));
        LOGINFO("                     pubInterface=%s,\n", SP(vnetconfig->pubInterface));
        LOGINFO("                     privInterface=%s,\n", SP(vnetconfig->privInterface));
        LOGINFO("                     bridgedev=%s,\n", SP(vnetconfig->bridgedev));
        LOGINFO("                     networkMode=%s\n", SP(vnetconfig->mode));
    }
    return (EUCA_OK);
}

//!
//! Initialize the system IP Table and install all the default filters. Based on the current state of the component,
//! different sets of rules will be applied to the table
//!
//! @param[in] pVnetCfg a pointer to the virtual network configuration structure
//! @param[in] isActive set to TRUE if this is an ACTIVE component or FALSE if this is a STANDBY component
//!
//! @return EUCA_OK on success or proper error code on failure
//!
//! @see
//!
//! @pre The pVnetCfg field must not be NULL and should have previously been initialized properly
//!
//! @post On success the system IP table will be initialized. On failure, the system will be left in
//!       a non-deterministic state.
//!
//! @note
//!
int vnetIptReInit(vnetConfig * pVnetCfg, boolean isActive)
{
    int rc = 0;
    u32 slashnet = 0;
    char *psDotBuf = NULL;
    char sCmdBuffer[256] = "";

    if (pVnetCfg->role != NC) {
        if (!strcmp(pVnetCfg->mode, NETMODE_MANAGED) || !strcmp(pVnetCfg->mode, NETMODE_MANAGED_NOVLAN)) {
            // clean up assigned addrs, iptables, dhcpd (and configs)
            if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", "-F")) != 0) {
                LOGDEBUG("Fail to flush filter rules, active=%d rc=%x\n", isActive, rc);
            }
            // clean up the NAT rules
            if ((rc = vnetApplySingleTableRule(pVnetCfg, "nat", "-F")) != 0) {
                LOGDEBUG("Fail to flush nat rules, active=%d rc=%x\n", isActive, rc);
            }
            // Different rules order depending if we're active or standby
            if (isActive) {
                psDotBuf = hex2dot(pVnetCfg->nw);

                // Load IPT preloads first
                if ((rc = vnetLoadIPTables(pVnetCfg)) != 0) {
                    LOGDEBUG("Fail to load IP table rules, active=%d rc=%x\n", isActive, rc);
                }
                // Set the default forward rule to drop
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", "-P FORWARD DROP")) != 0) {
                    LOGDEBUG("Fail install default DROP filter, active=%d rc=%x\n", isActive, rc);
                }

                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", "-A FORWARD -m conntrack --ctstate ESTABLISHED -j ACCEPT")) != 0) {
                    LOGDEBUG("Fail to set the forwarding rules, active=%d rc=%x\n", isActive, rc);
                }

                slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - pVnetCfg->nm)) + 1);
                snprintf(sCmdBuffer, 256, "-A FORWARD ! -d %s/%u -j ACCEPT", psDotBuf, slashnet);
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", sCmdBuffer)) != 0) {
                    LOGDEBUG("Fail to add forwarding rules to dest %s/%u, active=%d rc=%x\n", psDotBuf, slashnet, isActive, rc);
                }

                snprintf(sCmdBuffer, 256, "-A POSTROUTING ! -d %s/%u -s %s/%u -j MASQUERADE", psDotBuf, slashnet, psDotBuf, slashnet);
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "nat", sCmdBuffer)) != 0) {
                    LOGDEBUG("Fail to add masquerade rules from %s/%u to %s/%u, active=%d rc=%x\n", psDotBuf, slashnet, psDotBuf, slashnet, isActive, rc);
                }
                // Provides rules for doing internal/external network reporting/stats.
                snprintf(sCmdBuffer, 256, "-N EUCA_COUNTERS_IN");
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", sCmdBuffer)) != 0) {
                    LOGDEBUG("Fail to create inbound counters chain, active=%d rc=%x\n", isActive, rc);
                }

                snprintf(sCmdBuffer, 256, "-N EUCA_COUNTERS_OUT");
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", sCmdBuffer)) != 0) {
                    LOGDEBUG("Fail to create outbound counters chain, active=%d rc=%x\n", isActive, rc);
                }

                snprintf(sCmdBuffer, 256, "-A EUCA_COUNTERS_IN -d %s/%u", psDotBuf, slashnet);
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", sCmdBuffer)) != 0) {
                    LOGDEBUG("Fail to add inboud network counter rules with dest %s/%u, active=%d rc=%x\n", psDotBuf, slashnet, isActive, rc);
                }

                snprintf(sCmdBuffer, 256, "-A EUCA_COUNTERS_OUT -s %s/%u", psDotBuf, slashnet);
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", sCmdBuffer)) != 0) {
                    LOGDEBUG("Fail to add outboud network counter rules with dest %s/%u, active=%d rc=%x\n", psDotBuf, slashnet, isActive, rc);
                }

                snprintf(sCmdBuffer, 256, "-I FORWARD -j EUCA_COUNTERS_IN");
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", sCmdBuffer)) != 0) {
                    LOGDEBUG("Fail to flush nat rules, active=%d rc=%x\n", isActive, rc);
                }

                snprintf(sCmdBuffer, 256, "-I FORWARD -j EUCA_COUNTERS_OUT");
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", sCmdBuffer)) != 0) {
                    LOGDEBUG("Fail to flush nat rules, active=%d rc=%x\n", isActive, rc);
                }
                // Now free our buffer
                EUCA_FREE(psDotBuf);
            } else {
                // add default forwarding rule
                if ((rc = vnetApplySingleTableRule(pVnetCfg, "filter", "-P FORWARD ACCEPT")) != 0) {
                    LOGDEBUG("Fail to add forward rule, active=%d rc=%x\n", isActive, rc);
                }
                // Load IPT preloads
                if ((rc = vnetLoadIPTables(pVnetCfg)) != 0) {
                    LOGDEBUG("Fail to load IP table rules, active=%d rc=%x\n", isActive, rc);
                }
            }
        }
    }
    return (EUCA_OK);
}

//!
//! Installs an IP table rule redirecting the metadata request towards the cloud controller.
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return EUCA_OK on success or EUCA_INVALID_ERROR if any parameters does not meet the preconditions.
//!
//! @see vnetUnsetMetadataRedirect()
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li the cloud IP must be set
//!
//! @post The IP table rule has been installed on success. If any failure occured, the rule isn't is not
//!       to be considered installed
//!
int vnetSetMetadataRedirect(vnetConfig * vnetconfig)
{
    int rc = 0;
    char cmd[256] = "";
    char *ipbuf = NULL;

    if (!vnetconfig) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    snprintf(cmd, 256, EUCALYPTUS_ROOTWRAP " ip addr add 169.254.169.254 scope link dev %s", vnetconfig->eucahome, vnetconfig->privInterface);
    rc = system(cmd);

    if (vnetconfig->cloudIp != 0) {
        ipbuf = hex2dot(vnetconfig->cloudIp);
        snprintf(cmd, 256, "-A PREROUTING -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", ipbuf);
        EUCA_FREE(ipbuf);
        rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    } else {
        LOGWARN("cloudIp is not yet set, not installing redirect rule\n");
    }

    return (EUCA_OK);
}

//!
//! Uninstall the metadata request redirect rule from the IP table
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return EUCA_OK on success or EUCA_INVALID_ERROR if any parameters does not meet the preconditions.
//!
//! @see vnetSetMetadataRedirect()
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li the cloud IP must be set
//!
//! @post The IP table rule has been installed on success. If any failure occured, the rule isn't is not
//!       to be considered installed
//!
int vnetUnsetMetadataRedirect(vnetConfig * vnetconfig)
{
    int rc = 0;
    char cmd[256] = "";
    char *ipbuf = NULL;

    if (!vnetconfig) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    snprintf(cmd, 256, EUCALYPTUS_ROOTWRAP " ip addr del 169.254.169.254 scope link dev %s", vnetconfig->eucahome, vnetconfig->privInterface);
    rc = system(cmd);

    if (vnetconfig->cloudIp != 0) {
        ipbuf = hex2dot(vnetconfig->cloudIp);
        snprintf(cmd, 256, "-D PREROUTING -d 169.254.169.254/32 -p tcp -m tcp --dport 80 -j DNAT --to-destination %s:8773", ipbuf);
        EUCA_FREE(ipbuf);
        rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
    } else {
        LOGDEBUG("cloudIp is not yet set, not removing redirect rule\n");
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_ERROR: if we fail to initialize the tunelling
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @pre \p vnetconfig must not be NULL.
//!
int vnetInitTunnels(vnetConfig * vnetconfig)
{
    int ret = EUCA_OK;
    int rc = 0;
    char file[EUCA_MAX_PATH] = "";
    char *template = NULL;
    char *pass = NULL;
    char *newl = NULL;
    boolean done = FALSE;

    if (!vnetconfig) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    vnetconfig->tunnels.tunneling = 0;
    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
        if (vnetCountLocalIP(vnetconfig) <= 0) {
            // localIp not set, no tunneling
            LOGWARN("VNET_LOCALIP not set, tunneling is disabled\n");
            return (EUCA_OK);
        } else if (!strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN) && check_bridge(vnetconfig->privInterface)) {
            LOGWARN("in MANAGED-NOVLAN mode, priv interface '%s' must be a bridge, tunneling disabled\n", vnetconfig->privInterface);
            return (EUCA_OK);
        } else {
            ret = EUCA_OK;
            snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_KEYS_DIR "/vtunpass", vnetconfig->eucahome);
            if (check_file(file)) {
                LOGWARN("cannot locate tunnel password file '%s', tunneling disabled\n", file);
                ret = EUCA_ERROR;
            } else if (!check_file_newer_than(file, vnetconfig->tunnels.tunpassMtime)) {
                ret = EUCA_ERROR;
                LOGDEBUG("tunnel password file has changed, reading new value\n");
                if ((pass = file2str(file)) != NULL) {
                    if ((newl = strchr(pass, '\n')) != NULL)
                        *newl = '\0';

                    snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_DATA_DIR "/vtunall.conf.template", vnetconfig->eucahome);
                    template = file2str(file);
                    if (template) {
                        euca_strreplace(&template, "VPASS", pass);
                        vnetconfig->tunnels.tunpassMtime = time(NULL);
                        done = TRUE;
                    }

                    EUCA_FREE(pass);
                }

                if (done) {
                    // success
                    snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_KEYS_DIR "/vtunall.conf", vnetconfig->eucahome);
                    if ((rc = write2file(file, template)) != 0) {
                        // error
                        LOGERROR("cannot write vtun config file '%s', tunneling disabled\n", file);
                    } else {
                        vnetconfig->tunnels.tunneling = 1;
                        ret = EUCA_OK;
                    }
                } else {
                    LOGERROR("cannot set up tunnel configuration file, tunneling is disabled\n");
                }

                EUCA_FREE(template);
            } else {
                ret = EUCA_OK;
            }
        }
    }
    // enable tunneling if all went well
    if (!ret) {
        vnetconfig->tunnels.tunneling = 1;
    }
    return (ret);
}

//!
//! Adds a given host \p mac / \p ip to our network configuration.
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] mac the host MAC address string
//! @param[in] ip the host IP address string
//! @param[in] vlan the Virtual LAN index
//! @param[in] idx the host index
//!
//! @return EUCA_OK on success or the following error codes;
//!         \li EUCA_ERROR: if we fail to add the given host
//!         \li EUCA_PERMISSION_ERROR: if virtual network support is not enabled
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @see vnetDelHost(), vnetRefreshHost()
//!
//! @pre \li \p vnetconfig and \p mac must not be NULL.
//!      \li \p vlan must be between [0..NUMBER_OF_VLANS].
//!      \li \p idx must be less than 0 or between [vnetconfig->addrIndexMin..vnetconfig->addrIndexMax].
//!      \li \p virtual network support must be enabled.
//!
//! @post On success, the host has been added our our network
//!
int vnetAddHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan, int idx)
{
    int i = 0;
    int found = 0;
    int start = 0;
    int stop = 0;
    char *newip = NULL;
    boolean done = FALSE;

    if (param_check("vnetAddHost", vnetconfig, mac, ip, vlan)) {
        LOGERROR("bad input params: vnetconfig=%p, mac=%s, ip=%s, idx=%d\n", vnetconfig, SP(mac), SP(ip), idx);
        return (EUCA_INVALID_ERROR);
    }

    if (!vnetconfig->enabled) {
        LOGDEBUG("network support is not enabled\n");
        return (EUCA_PERMISSION_ERROR);
    }

    if (idx < 0) {
        start = vnetconfig->addrIndexMin;
        stop = vnetconfig->addrIndexMax;
    } else if ((idx >= vnetconfig->addrIndexMin) && (idx <= vnetconfig->addrIndexMax)) {
        start = idx;
        stop = idx;
    } else {
        LOGERROR("index out of bounds: idx=%d, min=%d max=%d\n", idx, vnetconfig->addrIndexMin, vnetconfig->addrIndexMax);
        return (EUCA_INVALID_ERROR);
    }

    for (i = start, done = FALSE, found = 0; ((i <= stop) && !done); i++) {
        if (!maczero(vnetconfig->networks[vlan].addrs[i].mac)) {
            if (!found)
                found = i;
        } else if (!machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) {
            done = TRUE;
        }
    }

    if (done) {
        // duplicate IP found
        LOGWARN("attempting to add duplicate macmap entry, ignoring\n");
    } else if (found) {
        if (mac2hex(mac, vnetconfig->networks[vlan].addrs[found].mac) != NULL) {
            if (ip) {
                vnetconfig->networks[vlan].addrs[found].ip = dot2hex(ip);
            } else {
                if ((newip = hex2dot(vnetconfig->networks[vlan].nw + found)) == NULL) {
                    LOGWARN("Out of memory\n");
                } else {
                    vnetconfig->networks[vlan].addrs[found].ip = dot2hex(newip);
                    EUCA_FREE(newip);
                }
            }
            vnetconfig->networks[vlan].numhosts++;
        } else {
            LOGERROR("failed to convers mac address '%s on vlan %d'\n", mac, vlan);
        }
    } else {
        LOGERROR("failed to add host %s on vlan %d\n", mac, vlan);
        return (EUCA_ERROR);
    }
    return (EUCA_OK);
}

//!
//! Deletes a given host \p mac / \p ip address from our network configuration
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] mac the host mac address string
//! @param[in] ip the host IP address string
//! @param[in] vlan the Virtual LAN index
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if we fail to find the host in our list
//!         \li EUCA_PERMISSION_ERROR: if virtual network support is not enabled
//!
//! @see vnetAddHost(), vnetRefreshHost()
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li \p mac and/or \p ip must not be NULL.
//!      \li \p vlan must be in the [0..NUMBER_OF_VLANS] range.
//!      \li \p virtual network support must be enabled.
//!
//! @post on success, the host has been removed from our configuration
//!
int vnetDelHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan)
{
    int i = 0;

    if (param_check("vnetDelHost", vnetconfig, mac, ip, vlan)) {
        LOGERROR("bad input params: vnetconfig=%p, mac=%s, ip=%s, vlan=%d\n", vnetconfig, SP(mac), SP(ip), vlan);
        return (EUCA_INVALID_ERROR);
    }

    if (!vnetconfig->enabled) {
        LOGDEBUG("network support is not enabled\n");
        return (EUCA_PERMISSION_ERROR);
    }

    for (i = vnetconfig->addrIndexMin; i <= vnetconfig->addrIndexMax; i++) {
        if ((!mac || !machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)))) {
            bzero(&(vnetconfig->networks[vlan].addrs[i]), sizeof(netEntry));
            vnetconfig->networks[vlan].numhosts--;
            return (EUCA_OK);
        }
    }

    return (EUCA_NOT_FOUND_ERROR);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] mac
//! @param[in] ip
//! @param[in] vlan the Virtual LAN index
//! @param[in] idx
//!
//! @return If the host is not part of our list, the result of vnetAddHost() is returned. In
//!         any other cases, EUCA_OK on success is returned or the following error codes:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_PERMISSION_ERROR: if the virtual network support is disabled
//!
//! @see vnetAddHost(), vnetDelHost()
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li \p mac and/or \p ip must not be NULL.
//!      \li \p vlan must be in the [0..NUMBER_OF_VLAN] range.
//!      \li \p idx must be less than 0 or in the [vnetconfig->addrIndexMin..vnetconfig->addrIndexMax] range.
//!      \li \p virtual network support must be enabled.
//!
int vnetRefreshHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan, int idx)
{
    int i = 0;
    int found = 0;
    int start = 0;
    int stop = 0;
    boolean done = FALSE;

    if ((vnetconfig == NULL) || ((mac == NULL) && (ip == NULL)) || (vlan < 0) || (vlan >= NUMBER_OF_VLANS)) {
        LOGERROR("bad input params: vnetconfig=%p, mac=%s, ip=%s, vlan=%d, idx=%d\n", vnetconfig, SP(mac), SP(ip), vlan, idx);
        return (EUCA_INVALID_ERROR);
    }

    if (!vnetconfig->enabled) {
        LOGDEBUG("network support is not enabled\n");
        return (EUCA_PERMISSION_ERROR);
    }

    if (idx < 0) {
        start = vnetconfig->addrIndexMin;
        stop = vnetconfig->addrIndexMax;
    } else if (idx >= vnetconfig->addrIndexMin && idx <= (vnetconfig->addrIndexMax)) {
        start = idx;
        stop = idx;
    } else {
        LOGERROR("index out of bounds: idx=%d, min=%d max=%d\n", idx, vnetconfig->addrIndexMin, vnetconfig->addrIndexMax);
        return (EUCA_INVALID_ERROR);
    }

    for (i = start, done = FALSE, found = 0; ((i <= stop) && !done); i++) {
        if (ip) {
            if (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)) {
                found = i;
                done = TRUE;
            }
        }

        if (mac) {
            if (!machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) {
                found = i;
                done = TRUE;
            }
        }
    }

    if (!done) {
        return (vnetAddHost(vnetconfig, mac, ip, vlan, idx));
    } else {
        if (mac) {
            mac2hex(mac, vnetconfig->networks[vlan].addrs[found].mac);
        }

        if (ip) {
            vnetconfig->networks[vlan].addrs[found].ip = dot2hex(ip);
        }
    }

    return (EUCA_OK);
}

//!
//! Activate networking for a given host
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] mac
//! @param[in] ip
//! @param[in] vlan the Virtual LAN index
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if we fail to find the host
//!         \li EUCA_PERMISSION_ERROR: if the virtual network support is not available
//!
//! @see vnetDisableHost()
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li \p mac and/or \p ip must not be NULL.
//!      \li \p vlan must be in the [0..NUMBER_OF_VLANS] range.
//!      \li virtual network support must be enabled.
//!
int vnetEnableHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan)
{
    int i = 0;

    if (param_check("vnetEnableHost", vnetconfig, mac, ip, vlan)) {
        LOGERROR("bad input params: vnetconfig=%p, mac=%s, ip=%s, vlan=%d\n", vnetconfig, SP(mac), SP(ip), vlan);
        return (EUCA_INVALID_ERROR);
    }

    if (!vnetconfig->enabled) {
        LOGDEBUG("network support is not enabled\n");
        return (EUCA_PERMISSION_ERROR);
    }

    for (i = vnetconfig->addrIndexMin; i <= vnetconfig->addrIndexMax; i++) {
        if ((!mac || !machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)))) {
            vnetconfig->networks[vlan].addrs[i].active = 1;
            return (EUCA_OK);
        }
    }

    return (EUCA_NOT_FOUND_ERROR);
}

//!
//! Deactivate networking for a given host
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] mac the host mac address string
//! @param[in] ip the host IP address string
//! @param[in] vlan the Virtual LAN index
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if we cannot find the host in our list
//!         \li EUCA_PERMISSION_ERROR: if virtual network support isn't available
//!
//! @see vnetEnableHost()
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li \p mac and/or \p ip must not be NULL.
//!      \li \p vlan must be in the [0..NUMBER_OF_VLANS] range.
//!      \li virtual network support must be enabled.
//!
int vnetDisableHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan)
{
    int i = 0;

    if ((vnetconfig == NULL) || ((mac == NULL) && (ip == NULL)) || (vlan < 0) || (vlan >= NUMBER_OF_VLANS)) {
        LOGERROR("bad input params: vnetconfig=%p, mac=%s, ip=%s, vlan=%d\n", vnetconfig, SP(mac), SP(ip), vlan);
        return (EUCA_INVALID_ERROR);
    }

    if (!vnetconfig->enabled) {
        LOGDEBUG("network support is not enabled\n");
        return (EUCA_PERMISSION_ERROR);
    }

    for (i = vnetconfig->addrIndexMin; i <= vnetconfig->addrIndexMax; i++) {
        if ((!mac || !machexcmp(mac, vnetconfig->networks[vlan].addrs[i].mac)) && (!ip || (vnetconfig->networks[vlan].addrs[i].ip == dot2hex(ip)))) {
            vnetconfig->networks[vlan].addrs[i].active = 0;
            return (EUCA_OK);
        }
    }

    return (EUCA_NOT_FOUND_ERROR);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] userName
//! @param[in] netName
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_ERROR: if we fail to delete the chain
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @pre \p vnetconfig, \p userName and \p netName must not be NULL.
//!
int vnetDeleteChain(vnetConfig * vnetconfig, char *userName, char *netName)
{
    char cmd[256] = "";
    int rc = 0;
    int runcount = 0;
    char *hashChain = NULL;
    char userNetString[EUCA_MAX_PATH] = "";

    if (param_check("vnetDeleteChain", vnetconfig, userName, netName)) {
        LOGERROR("bad input params: vnetconfig=%p, userName=%p, netName=%p\n", vnetconfig, userName, netName);
        return (EUCA_INVALID_ERROR);
    }

    snprintf(userNetString, EUCA_MAX_PATH, "%s%s", userName, netName);
    if ((rc = hash_b64enc_string(userNetString, &hashChain)) != 0) {
        LOGERROR("cannot hash user/net string (userNetString=%s)\n", userNetString);
        return (EUCA_ERROR);
    }

    rc = check_chain(vnetconfig, userName, netName);
    LOGDEBUG("params: userName=%s, netName=%s, rc=%d\n", SP(userName), SP(netName), rc);
    if (!rc) {
        snprintf(cmd, 256, "-D FORWARD -j %s", hashChain);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
            LOGERROR("'%s' failed; cannot remove link to chain %s\n", cmd, hashChain);
        }

        runcount = 0;
        while (!rc && (runcount < 10)) {
            LOGDEBUG("duplicate rule found, removing others: %d/%d\n", runcount, 10);
            rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
            runcount++;
        }

        LOGDEBUG("vnetDeleteChain(): flushing 'filter' table\n");
        snprintf(cmd, 256, "-F %s", hashChain);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
            LOGERROR("'%s' failed; cannot flush chain %s\n", cmd, hashChain);
        }

        snprintf(cmd, 256, "-X %s", hashChain);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
            LOGERROR("'%s' failed; cannot remove chain %s\n", cmd, hashChain);
        }

        runcount = 0;
        while (!rc && (runcount < 10)) {
            LOGDEBUG("duplicate rule found, removing others: %d/%d\n", runcount, 10);
            rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
            runcount++;
        }
    }

    EUCA_FREE(hashChain);
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] userName the chain's user name
//! @param[in] netName the chain's network name
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_ERROR: if we fail to create a chain
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @pre \p vnetconfig, \p userName and \p netName must not be NULL.
//!
int vnetCreateChain(vnetConfig * vnetconfig, char *userName, char *netName)
{
    char cmd[256] = "";
    int rc = 0;
    int ret = EUCA_OK;
    int count = 0;
    char *hashChain = NULL;
    char userNetString[EUCA_MAX_PATH] = "";

    if (param_check("vnetCreateChain", vnetconfig, userName, netName)) {
        LOGERROR("bad input params: vnetconfig=%p, userName=%p, netName=%p\n", vnetconfig, userName, netName);
        return (EUCA_INVALID_ERROR);
    }

    snprintf(userNetString, EUCA_MAX_PATH, "%s%s", userName, netName);
    if ((rc = hash_b64enc_string(userNetString, &hashChain)) != 0) {
        LOGERROR("cannot hash user/net string (userNetString=%s)\n", userNetString);
        return (EUCA_ERROR);
    }

    ret = EUCA_OK;
    if ((rc = check_chain(vnetconfig, userName, netName)) != 0) {
        snprintf(cmd, 256, "-N %s", hashChain);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
            LOGERROR("'%s' failed; cannot create chain %s\n", cmd, hashChain);
            ret = EUCA_ERROR;
        }
    }

    if (!ret) {
        snprintf(cmd, 256, "-D FORWARD -j %s", hashChain);
        rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
        count = 0;
        while (!rc && (count < 10)) {
            rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
            count++;
        }

        snprintf(cmd, 256, "-A FORWARD -j %s", hashChain);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
            LOGERROR("'%s' failed; cannot link to chain %s\n", cmd, hashChain);
            ret = EUCA_ERROR;
        }
    }

    EUCA_FREE(hashChain);
    return (ret);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] userName
//! @param[in] netName
//!
//! @return EUCA_ERROR on failure or the result of the vnetApplySingleTableRule() call.
//!
//! @see vnetApplySingleTableRule()
//!
//! @pre \p vnetconfig, \p userName and \p netName must not be NULL.
//!
int vnetFlushTable(vnetConfig * vnetconfig, char *userName, char *netName)
{
    int rc = 0;
    int ret = EUCA_ERROR;
    char *hashChain = NULL;
    char cmd[256] = "";
    char userNetString[EUCA_MAX_PATH] = "";

    if ((vnetconfig == NULL) || (userName == NULL) || (netName == NULL)) {
        LOGERROR("bad input params: vnetconfig=%p, userName=%p, netName=%p\n", vnetconfig, userName, netName);
        return (EUCA_INVALID_ERROR);
    }

    snprintf(userNetString, EUCA_MAX_PATH, "%s%s", userName, netName);
    if ((rc = hash_b64enc_string(userNetString, &hashChain)) != 0) {
        LOGERROR("cannot hash user/net string (userNetString=%s)\n", userNetString);
        return (EUCA_ERROR);
    }

    LOGDEBUG("flushing 'filter' table\n");
    if ((userName && netName) && !check_chain(vnetconfig, userName, netName)) {
        snprintf(cmd, 256, "-F %s", hashChain);
        ret = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    }

    EUCA_FREE(hashChain);
    return ret;
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] table
//! @param[in] rule
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_ERROR: if we fail to apply the EB table rule
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @pre \p vnetconfig, \p table and \p rule must not be NULL.
//!
int vnetApplySingleEBTableRule(vnetConfig * vnetconfig, char *table, char *rule)
{
    int rc = 0;
    char cmd[EUCA_MAX_PATH] = "";

    if (!rule || !table || !vnetconfig) {
        LOGERROR("bad input params: vnetconfig=%p, table=%s, rule=%s\n", vnetconfig, SP(table), SP(rule));
        return (EUCA_INVALID_ERROR);
    }

    snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ebtables -t %s %s\n", vnetconfig->eucahome, table, rule);
    LOGDEBUG("running cmd '%s'\n", cmd);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc)
        return (EUCA_ERROR);
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] table
//! @param[in] rule
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_ERROR: if we fail to apply teh table and rule
//!         \li EUCA_MEMORY_ERROR: if we fail to allocate memory
//!         \li EUCA_PERMISSION_ERROR: if we fail to create a temp file
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_ACCESS_ERROR: if we fail to open our temp file for writting
//!
//! @pre \p vnetconfig, \p table and \p rule must not be NULL.
//!
int vnetApplySingleTableRule(vnetConfig * vnetconfig, char *table, char *rule)
{
    int rc = 0;
    int fd = 0;
    int ret = EUCA_OK;
    char *file = NULL;
    char cmd[256] = "";
    FILE *FH = NULL;

    if (!rule || !table || !vnetconfig) {
        LOGERROR("bad input params: vnetconfig=%p, table=%s, rule=%s\n", vnetconfig, SP(table), SP(rule));
        return (EUCA_INVALID_ERROR);
    }

    LOGDEBUG("applying single table (%s) rule (%s)\n", table, rule);

    if ((file = strdup("/tmp/euca-ipt-XXXXXX")) == NULL) {
        return (EUCA_MEMORY_ERROR);
    }

    if ((fd = safe_mkstemp(file)) < 0) {
        EUCA_FREE(file);
        return (EUCA_PERMISSION_ERROR);
    }

    chmod(file, 0644);
    if ((FH = fdopen(fd, "w")) == NULL) {
        close(fd);
        unlink(file);
        EUCA_FREE(file);
        return (EUCA_ACCESS_ERROR);
    }

    fprintf(FH, "%s\n", rule);
    fclose(FH);
    close(fd);

    snprintf(cmd, 256, EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/euca_ipt %s %s", vnetconfig->eucahome, vnetconfig->eucahome, table, file);
    if ((rc = system(cmd)) != 0) {
        ret = EUCA_ERROR;
    }

    unlink(file);
    EUCA_FREE(file);
    return (ret);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] type
//! @param[in] destUserName
//! @param[in] destName
//! @param[in] sourceUserName
//! @param[in] sourceNet
//! @param[in] sourceNetName
//! @param[in] protocol
//! @param[in] minPort
//! @param[in] maxPort
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any non-specific failure occured.
//!         \li EUCA_INVALID_ERROR: if any of our parameter does not meet the precondition
//!
//! @pre \li \p vnetconfig, \p type, \p destUserName, \p destName must not be NULL.
//!      \li at least 1 of the following fields must not be NULL: \p sourceUserName, \p sourceNet, \p sourceNetName.
//!
int vnetTableRule(vnetConfig * vnetconfig, char *type, char *destUserName, char *destName, char *sourceUserName, char *sourceNet, char *sourceNetName,
                  char *protocol, int minPort, int maxPort)
{
    int rc = 0;
    int destVlan = 0;
    int srcVlan = 0;
    int slashnet = 0;
    char rule[1024] = "";
    char newrule[1024] = "";
    char srcNet[32] = "";
    char dstNet[32] = "";
    char *tmp = NULL;
    char *hashChain = NULL;
    char userNetString[EUCA_MAX_PATH] = "";

    if (param_check("vnetTableRule", vnetconfig, type, destUserName, destName, sourceNet, sourceUserName, sourceNetName)) {
        LOGERROR("bad input params: vnetconfig=%p, type=%s, destUserName=%s, destName=%s, sourceNet=%s, sourceUserName=%p, sourceNetName=%p\n",
                 vnetconfig, SP(type), SP(destUserName), SP(destName), SP(sourceNet), sourceUserName, sourceNetName);
        return (EUCA_INVALID_ERROR);
    }

    snprintf(userNetString, EUCA_MAX_PATH, "%s%s", destUserName, destName);
    if ((rc = hash_b64enc_string(userNetString, &hashChain)) != 0) {
        LOGERROR("cannot hash user/net string (userNetString=%s)\n", userNetString);
        return (EUCA_ERROR);
    }

    if ((destVlan = vnetGetVlan(vnetconfig, destUserName, destName)) < 0) {
        LOGERROR("no vlans associated with active network %s/%s\n", destUserName, destName);
        EUCA_FREE(hashChain);
        return (EUCA_ERROR);
    }

    slashnet = 32 - ((int)(log2((double)((0xFFFFFFFF - vnetconfig->networks[destVlan].nm) + 1))));
    tmp = hex2dot(vnetconfig->networks[destVlan].nw);
    snprintf(dstNet, 32, "%s/%d", tmp, slashnet);
    EUCA_FREE(tmp);

    if (sourceNetName) {
        if ((srcVlan = vnetGetVlan(vnetconfig, sourceUserName, sourceNetName)) < 0) {
            LOGWARN("cannot locate active source vlan for network %s/%s, skipping\n", sourceUserName, sourceNetName);
            EUCA_FREE(hashChain);
            return (EUCA_OK);
        } else {
            tmp = hex2dot(vnetconfig->networks[srcVlan].nw);
            snprintf(srcNet, 32, "%s/%d", tmp, slashnet);
            EUCA_FREE(tmp);
        }
    } else {
        snprintf(srcNet, 32, "%s", sourceNet);
    }

    if (!strcmp(type, "firewall-open")) {
        snprintf(rule, 1024, "-A %s", hashChain);
    } else if (!strcmp(type, "firewall-close")) {
        snprintf(rule, 1024, "-D %s", hashChain);
    }

    EUCA_FREE(hashChain);
    snprintf(newrule, 1024, "%s -s %s -d %s", rule, srcNet, dstNet);
    strcpy(rule, newrule);

    if (protocol) {
        snprintf(newrule, 1024, "%s -p %s", rule, protocol);
        strcpy(rule, newrule);
    }

    if (minPort && maxPort) {
        if (protocol && (!strcmp(protocol, "tcp") || !strcmp(protocol, "udp"))) {
            if (minPort != maxPort) {
                snprintf(newrule, 1024, "%s -m %s --dport %d:%d", rule, protocol, minPort, maxPort);
            } else {
                snprintf(newrule, 1024, "%s -m %s --dport %d", rule, protocol, minPort);
            }
            strcpy(rule, newrule);
        }
    }

    snprintf(newrule, 1024, "%s -j ACCEPT", rule);
    strcpy(rule, newrule);

    if (!strcmp(type, "firewall-close")) {
        // this means that the network should already be flushed and empty (default policy == drop)
    } else {
        LOGINFO("applying iptables rule: %s\n", rule);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", rule)) != 0) {
            LOGERROR("iptables rule application failed: %d\n", rc);
            return (EUCA_ERROR);
        }
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] vlan the Virtual LAN index
//! @param[in] uuid
//! @param[in] user
//! @param[in] network
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any of vnetconfig, vlan, user ot network parameters are invalid
//!
int vnetSetVlan(vnetConfig * vnetconfig, int vlan, char *uuid, char *user, char *network)
{
    if (param_check("vnetSetVlan", vnetconfig, vlan, user, network)) {
        LOGERROR("bad input params: vnetconfig=%p, vlan=%d, user=%p, network=%p\n", vnetconfig, vlan, user, network);
        return (EUCA_INVALID_ERROR);
    }
    // Lets make sure both our pointers aren't the same... 'restrict'
    if (user && (user != vnetconfig->users[vlan].userName))
        euca_strncpy(vnetconfig->users[vlan].userName, user, 48);

    // Lets make sure both our pointers aren't the same... 'restrict'
    if (network && (network != vnetconfig->users[vlan].netName))
        euca_strncpy(vnetconfig->users[vlan].netName, network, 64);

    // Lets make sure both our pointers aren't the same... 'restrict'
    if (uuid && (uuid != vnetconfig->users[vlan].uuid))
        euca_strncpy(vnetconfig->users[vlan].uuid, uuid, 48);
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] user
//! @param[in] network
//!
//! @return The Virtual LAN (VLAN) number or -1 if not found. A value lesser than -1 indicates
//!         that the given network (absolute value) exists but is currently inactive
//!
//! @pre \p vnetconfig, \p user and \p network must not be NULL.
//!
int vnetGetVlan(vnetConfig * vnetconfig, char *user, char *network)
{
    int i = 0;

    if (!vnetconfig || !user || !network) {
        LOGERROR("bad input params: vnetconfig=%p, user=%p, network=%p\n", vnetconfig, user, network);
        return (-1);
    }

    for (i = 0; i < vnetconfig->max_vlan; i++) {
        if (!strcmp(vnetconfig->users[i].userName, user) && !strcmp(vnetconfig->users[i].netName, network)) {
            if (!vnetconfig->networks[i].active) {
                // network exists, but is inactive
                return (-1 * i);
            }
            return (i);
        }
    }
    return (-1);
}

//!
//!
//!
//! @param[in]  vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[out] outusers
//! @param[out] outnets
//! @param[out] len
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any of our parameter does not meet the precondition
//!         \li EUCA_MEMORY_ERROR: if we fail to allocate memory for our 'out' parameters
//!
//! @pre \li \p vnetconfig, \p outusers, \p outnets and \p len must not be NULL.
//!      \li \p (*outusers) and \p (*outnets) should point to NULL.
//!
int vnetGetAllVlans(vnetConfig * vnetconfig, char ***outusers, char ***outnets, int *len)
{
    int i = 0;
    int rc = 0;
    char userNetString[EUCA_MAX_PATH] = { 0 };
    char netslash[24] = { 0 };
    char *net = NULL;
    char *chain = NULL;
    int slashnet = 0;

    if (!vnetconfig || !outusers || !outnets || !len) {
        LOGERROR("bad input parameters: vnetconfig=%p, outusers=%p, outnets=%p, len=%p\n", vnetconfig, outusers, outnets, len);
        return (EUCA_INVALID_ERROR);
    }

    if ((*outusers = EUCA_ALLOC(vnetconfig->max_vlan, sizeof(char *))) == NULL) {
        LOGFATAL("out of memory!\n");
        return (EUCA_MEMORY_ERROR);
    }

    if ((*outnets = EUCA_ALLOC(vnetconfig->max_vlan, sizeof(char *))) == NULL) {
        LOGFATAL("out of memory!\n");
        EUCA_FREE(*outusers);
        return (EUCA_MEMORY_ERROR);
    }

    *len = 0;
    for (i = 0; i < vnetconfig->max_vlan; i++) {
        if (vnetconfig->networks[i].active) {
            snprintf(userNetString, EUCA_MAX_PATH, "%s%s", vnetconfig->users[i].userName, vnetconfig->users[i].netName);
            if ((rc = hash_b64enc_string(userNetString, &chain)) != 0) {
                LOGERROR("cannot hash user/net string (userNetString=%s)\n", userNetString);
            } else {
                net = hex2dot(vnetconfig->networks[i].nw);
                slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[i].nm)) + 1);
                if (net && (slashnet >= 0) && (slashnet <= 32)) {
                    netslash[0] = '\0';
                    snprintf(netslash, 24, "%s/%d", net, slashnet);
                    (*outusers)[(*len)] = strdup(chain);
                    (*outnets)[(*len)] = strdup(netslash);
                    (*len)++;
                }
                EUCA_FREE(net);
            }
            EUCA_FREE(chain);
        }
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in]     vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in]     instId instance identifier string
//! @param[in]     vlan the Virtual LAN index
//! @param[in]     nidx network index.
//! @param[in,out] outmac
//! @param[out]    outpubip
//! @param[in,out] outprivip
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_ERROR: if any error occured while executing this operation
//!         \li EUCA_INVALID_ERROR: if any of our parameter does not meet the precondition
//!
//! @pre \p vnetconfig, \p instId, \p outmac and \p outpubip must not be NULL.
//!
int vnetGenerateNetworkParams(vnetConfig * vnetconfig, char *instId, int vlan, int nidx, char *outmac, char *outpubip, char *outprivip)
{
    int i = 0;
    int rc = 0;
    int ret = EUCA_OK;
    int networkIdx = 0;
    u32 inip = 0;
    boolean found = FALSE;

    if (!vnetconfig || !instId || !outmac || !outpubip || !outprivip) {
        LOGERROR("bad input params: vnetconfig=%p, instId=%s, outmac=%s, outpubip=%s outprivip=%s\n", vnetconfig, SP(instId), SP(outmac), SP(outpubip), SP(outprivip));
        return (EUCA_INVALID_ERROR);
    }

    ret = EUCA_ERROR;
    // define/get next mac and allocate IP
    if (!strcmp(vnetconfig->mode, NETMODE_STATIC)) {
        // search for existing entry
        inip = dot2hex(outprivip);
        found = FALSE;

        for (i = vnetconfig->addrIndexMin; ((i < vnetconfig->addrIndexMax) && !found); i++) {
            if (!machexcmp(outmac, vnetconfig->networks[0].addrs[i].mac) && (vnetconfig->networks[0].addrs[i].ip == inip)) {
                vnetconfig->networks[0].addrs[i].active = 1;
                found = TRUE;
                ret = EUCA_OK;
            }
        }

        // get the next valid mac/ip pairing for this vlan
        if (!found) {
            outmac[0] = '\0';
            if ((rc = vnetGetNextHost(vnetconfig, outmac, outprivip, 0, -1)) == 0) {
                snprintf(outpubip, IP_BUFFER_SIZE, "%s", outprivip);
                ret = EUCA_OK;
            }
        }
    } else if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        if (!strlen(outmac)) {
            if ((rc = instId2mac(vnetconfig, instId, outmac)) != 0) {
                LOGERROR("unable to convert instanceId (%s) to mac address\n", instId);
                return (EUCA_ERROR);
            }
        }
        ret = EUCA_OK;
    } else if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
        if (!strlen(outmac)) {
            if ((rc = instId2mac(vnetconfig, instId, outmac)) != 0) {
                LOGERROR("unable to convert instanceId (%s) to mac address\n", instId);
                return (EUCA_ERROR);
            }
        }

        if (nidx == -1) {
            networkIdx = -1;
        } else {
            networkIdx = nidx;
        }

        // add the mac address to the virtual network
        if ((rc = vnetAddHost(vnetconfig, outmac, NULL, vlan, networkIdx)) == 0) {
            // get the next valid mac/ip pairing for this vlan
            if ((rc = vnetGetNextHost(vnetconfig, outmac, outprivip, vlan, networkIdx)) == 0) {
                ret = EUCA_OK;
            }
        }
    }
    return (ret);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] mac
//! @param[in] ip
//! @param[in] vlan the Virtual LAN index
//! @param[in] idx
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any of our parameter does not meet the precondition
//!         \li EUCA_NOT_FOUND_ERROR: if we couldn't select the next host
//!         \li EUCA_PERMISSION_ERROR: if the network support is not enabled
//!
//! @pre \li \p vnetconfig, \p mac, \p ip must not be NULL.
//!      \li \p vlan must be in the [0..NUMBER_OF_VLANS] range.
//!      \li \p idx must be less than 0 or in the [vnetconfig->addrIndexMin..vnetconfig->addrIndexMax] range.
//!      \li virtual network support must be enabled.
//!
int vnetGetNextHost(vnetConfig * vnetconfig, char *mac, char *ip, int vlan, int idx)
{
    int i = 0;
    int start = 0;
    int stop = 0;
    char *newip = NULL;
    char *newmac = NULL;

    if (param_check("vnetGetNextHost", vnetconfig, mac, ip, vlan)) {
        LOGERROR("bad input params: vnetconfig=%p, mac=%s, ip=%s, vlan=%d\n", vnetconfig, SP(mac), SP(ip), vlan);
        return (EUCA_INVALID_ERROR);
    }

    if (!vnetconfig->enabled) {
        LOGDEBUG("network support is not enabled\n");
        return (EUCA_PERMISSION_ERROR);
    }

    if (idx < 0) {
        start = vnetconfig->addrIndexMin;
        stop = vnetconfig->addrIndexMax;
    } else if (idx >= vnetconfig->addrIndexMin && idx <= (vnetconfig->addrIndexMax)) {
        start = idx;
        stop = idx;
    } else {
        LOGERROR("index out of bounds: idx=%d, min=%d max=%d\n", idx, vnetconfig->addrIndexMin, vnetconfig->addrIndexMax);
        return (EUCA_INVALID_ERROR);
    }

    for (i = start; i <= stop; i++) {
        if (maczero(vnetconfig->networks[vlan].addrs[i].mac) && (vnetconfig->networks[vlan].addrs[i].ip != 0) && (vnetconfig->networks[vlan].addrs[i].active == 0)) {
            hex2mac(vnetconfig->networks[vlan].addrs[i].mac, &newmac);
            strncpy(mac, newmac, strlen(newmac));
            EUCA_FREE(newmac);
            newip = hex2dot(vnetconfig->networks[vlan].addrs[i].ip);
            strncpy(ip, newip, 16);
            EUCA_FREE(newip);
            vnetconfig->networks[vlan].addrs[i].active = 1;
            return (EUCA_OK);
        }
    }
    return (EUCA_NOT_FOUND_ERROR);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return the number of local IPs set
//!
//! @pre in order to get a valid result, \p vnetconfig must not be NULL.
//!
int vnetCountLocalIP(vnetConfig * vnetconfig)
{
    int i = 0;
    int count = 0;

    if (!vnetconfig) {
        return (0);
    }

    for (i = 0, count = 0; i < 32; i++) {
        if (vnetconfig->localIps[i] != 0) {
            count++;
        }
    }
    return (count);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] ip
//!
//! @return EUCA_OK on success and \p ip is a local IP; or the following error code:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if the given IP is not a valid local IP.
//!
//! @pre \p vnetconfig must not be NULL.
//!
int vnetCheckLocalIP(vnetConfig * vnetconfig, u32 ip)
{
    int i = 0;

    if (!vnetconfig) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }
    // local address? (127.0.0.0/8)
    if ((ip >= 0x7F000000) && (ip <= 0x7FFFFFFF))
        return (EUCA_OK);

    for (i = 0; i < 32; i++) {
        if (vnetconfig->localIps[i] == ip) {
            return (EUCA_OK);
        }
    }
    return (EUCA_NOT_FOUND_ERROR);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] ip
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NO_SPACE_ERROR: if we fail to find a free spot for the IP in the local IP list
//!
//! @pre \p vnetconfig must not be NULL.
//!
int vnetAddLocalIP(vnetConfig * vnetconfig, u32 ip)
{
    int i = 0;

    if (!vnetconfig) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    for (i = 0; i < 32; i++) {
        if (vnetconfig->localIps[i] == ip) {
            return (EUCA_OK);
        }

        if (vnetconfig->localIps[i] == 0) {
            vnetconfig->localIps[i] = ip;
            return (EUCA_OK);
        }
    }

    return (EUCA_NO_SPACE_ERROR);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] dev
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NO_SPACE_ERROR: if we fail to find a free spot for the device in the device list
//!
//! @pre \p vnetconfig and \p dev must not be NULL.
//!
int vnetAddDev(vnetConfig * vnetconfig, char *dev)
{
    int i = 0;
    int foundone = -1;

    if (param_check("vnetAddDev", vnetconfig, dev)) {
        LOGERROR("bad input params: vnetconfig=%p, dev=%s\n", vnetconfig, SP(dev));
        return (EUCA_INVALID_ERROR);
    }

    for (i = 0; i < vnetconfig->max_vlan; i++) {
        if (!strcmp(vnetconfig->etherdevs[i], dev)) {
            return (EUCA_ERROR);
        }

        if (vnetconfig->etherdevs[i][0] == '\0') {
            foundone = i;
            //            euca_strncpy(vnetconfig->etherdevs[i], dev, MAX_ETH_DEV_PATH);
            //            return (EUCA_OK);
        }
    }
    if ((foundone >= 0) && (foundone < vnetconfig->max_vlan)) {
        euca_strncpy(vnetconfig->etherdevs[foundone], dev, MAX_ETH_DEV_PATH);
        return (EUCA_OK);
    }
    return (EUCA_NO_SPACE_ERROR);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] dev
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @pre \p vnetconfig and \p dev must not be NULL.
//!
int vnetDelDev(vnetConfig * vnetconfig, char *dev)
{
    int i = 0;

    if (param_check("vnetDelDev", vnetconfig, dev)) {
        LOGERROR("bad input params: vnetconfig=%p, dev=%s\n", vnetconfig, SP(dev));
        return (EUCA_INVALID_ERROR);
    }

    for (i = 0; i < vnetconfig->max_vlan; i++) {
        if (!strncmp(vnetconfig->etherdevs[i], dev, MAX_ETH_DEV_PATH)) {
            bzero(vnetconfig->etherdevs[i], MAX_ETH_DEV_PATH);
            return (EUCA_OK);
        }
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] numHosts
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_ACCESS_ERROR: if we cannot open the DHCP configuration file in writing mode
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @pre \p vnetconfig and \p numHosts should not be NULL.
//!
int vnetGenerateDHCP(vnetConfig * vnetconfig, int *numHosts)
{
    int i = 0;
    int j = 0;
    int k = 0;
    char *nameServerDup = NULL;
    char *network = NULL;
    char *netmask = NULL;
    char *broadcast = NULL;
    char *sName = NULL;
    char *nameserver = NULL;
    char *router = NULL;
    char *euca_nameserver = NULL;
    char *mac = NULL;
    char *newip = NULL;
    char nameservers[1024] = "";
    char fname[EUCA_MAX_PATH] = "";
    FILE *fp = NULL;
    boolean hasNameServer = FALSE;

    if (param_check("vnetGenerateDHCP", vnetconfig) || (numHosts == NULL)) {
        LOGERROR("bad input params: vnetconfig=%p, numHosts=%p\n", vnetconfig, numHosts);
        return (EUCA_INVALID_ERROR);
    }

    *numHosts = 0;
    snprintf(fname, EUCA_MAX_PATH, "%s/euca-dhcp.conf", vnetconfig->path);

    if ((fp = fopen(fname, "w")) == NULL) {
        return (EUCA_ACCESS_ERROR);
    }
    fprintf(fp, "# automatically generated config file for DHCP server\ndefault-lease-time 86400;\nmax-lease-time 86400;\nddns-update-style none;\n\n");

    fprintf(fp, "shared-network euca {\n");
    for (i = 0; i < vnetconfig->max_vlan; i++) {
        if (vnetconfig->networks[i].numhosts > 0) {
            network = hex2dot(vnetconfig->networks[i].nw);
            netmask = hex2dot(vnetconfig->networks[i].nm);
            broadcast = hex2dot(vnetconfig->networks[i].bc);
            nameserver = hex2dot(vnetconfig->networks[i].dns);
            if (vnetconfig->tunnels.localIpId < 0) {
                router = hex2dot(vnetconfig->networks[i].router);
            } else {
                router = hex2dot(vnetconfig->networks[i].router + vnetconfig->tunnels.localIpId);
            }

            // Add our name servers provided by the CLC
            k = 0;
            hasNameServer = FALSE;
            while (vnetconfig->eucaNameServer[k] != 0) {
                if (vnetconfig->eucaNameServer[k] != LOCALHOST_HEX) {
                    if ((sName = hex2dot(vnetconfig->eucaNameServer[k])) != NULL) {
                        if (hasNameServer) {
                            if ((nameServerDup = strdup(nameservers)) != NULL) {
                                snprintf(nameservers, sizeof(nameservers), "%s,%s", nameServerDup, sName);
                                EUCA_FREE(nameServerDup);
                            }
                        } else {
                            snprintf(nameservers, sizeof(nameservers), "%s", sName);
                        }
                        hasNameServer = TRUE;
                        EUCA_FREE(sName);
                    }
                }
                k++;
            }

            // Add the nameserver from eucalyptus.conf
            if (vnetconfig->networks[i].dns != 0) {
                if (hasNameServer) {
                    if ((nameServerDup = strdup(nameservers)) != NULL) {
                        snprintf(nameservers, sizeof(nameservers), "%s,%s", nameServerDup, nameserver);
                        EUCA_FREE(nameServerDup);
                    }
                } else {
                    snprintf(nameservers, sizeof(nameservers), "%s", nameserver);
                }
            }
            // If our nameservers list is empty, add localhost
            if (strlen(nameservers) == 0) {
                snprintf(nameservers, sizeof(nameservers), "%s", LOCALHOST_STRING);
            }

            fprintf(fp, "subnet %s netmask %s {\n  option subnet-mask %s;\n  option broadcast-address %s;\n  option domain-name \"%s\";\n  option domain-name-servers %s;\n"
                    "  option routers %s;\n}\n", network, netmask, netmask, broadcast, vnetconfig->eucaDomainName, nameservers, router);

            EUCA_FREE(euca_nameserver);
            EUCA_FREE(nameserver);
            EUCA_FREE(network);
            EUCA_FREE(netmask);
            EUCA_FREE(broadcast);
            EUCA_FREE(router);

            for (j = vnetconfig->addrIndexMin; j <= vnetconfig->addrIndexMax; j++) {
                if (vnetconfig->networks[i].addrs[j].active == 1) {
                    newip = hex2dot(vnetconfig->networks[i].addrs[j].ip);
                    hex2mac(vnetconfig->networks[i].addrs[j].mac, &mac);
                    fprintf(fp, "\nhost node-%s {\n  hardware ethernet %s;\n  fixed-address %s;\n}\n", newip, mac, newip);
                    (*numHosts)++;
                    EUCA_FREE(mac);
                    EUCA_FREE(newip);
                }
            }
        }
    }
    fprintf(fp, "}\n");
    fclose(fp);

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return EUCA_OK on success or the following error code:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NO_SPACE_ERROR: if we're running out of buffer space
//!
//! @pre \p vnetconfig must not be NULL.
//!
int vnetKickDHCP(vnetConfig * vnetconfig)
{
    struct stat statbuf = { 0 };
    char dstring[EUCA_MAX_PATH] = "";
    char buf[EUCA_MAX_PATH] = "";
    char file[EUCA_MAX_PATH] = "";
    char rootwrap[EUCA_MAX_PATH] = "";
    char *tmpstr = NULL;
    int tmppid = 0;
    int rc = EUCA_OK;
    int i = 0;
    int numHosts = 0;

    if (param_check("vnetKickDHCP", vnetconfig)) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM)) {
        return (EUCA_OK);
    }

    if ((rc = vnetGenerateDHCP(vnetconfig, &numHosts)) != 0) {
        LOGERROR("failed to (re)create DHCP config (%s/euca-dhcp.conf)\n", vnetconfig->path);
        return (rc);
    } else if (numHosts <= 0) {
        // nothing to do
        return (EUCA_OK);
    }

    for (i = 0; i < vnetconfig->max_vlan; i++) {
        if (vnetconfig->etherdevs[i][0] != '\0') {
            strncat(dstring, " ", EUCA_MAX_PATH - 1);

            if ((EUCA_MAX_PATH - strlen(dstring) - 1) < MAX_ETH_DEV_PATH) {
                LOGERROR("not enough buffer length left to copy ethernet dev name\n");
                return (EUCA_NO_SPACE_ERROR);
            }
            strncat(dstring, vnetconfig->etherdevs[i], MAX_ETH_DEV_PATH);
        }
    }

    /* force dhcpd to reload the conf */
    snprintf(file, EUCA_MAX_PATH, "%s/euca-dhcp.pid", vnetconfig->path);
    if (stat(file, &statbuf) == 0) {
        snprintf(rootwrap, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, vnetconfig->eucahome);
        snprintf(buf, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/net/euca-dhcp.pid", vnetconfig->eucahome);

        // little chunk of code to work-around bad dhcpd that takes some time to populate the pidfile...
        if ((tmpstr = file2str(buf)) != NULL) {
            tmppid = atoi(tmpstr);
            EUCA_FREE(tmpstr);
        }

        for (i = 0; i < 4 && tmppid <= 0; i++) {
            usleep(250000);
            if ((tmpstr = file2str(buf)) != NULL) {
                tmppid = atoi(tmpstr);
                EUCA_FREE(tmpstr);
            }
        }

        if ((rc = safekillfile(buf, vnetconfig->dhcpdaemon, 9, rootwrap)) != 0) {
            LOGWARN("failed to kill previous dhcp daemon\n");
        }
        usleep(250000);
    }

    snprintf(buf, EUCA_MAX_PATH, "%s/euca-dhcp.trace", vnetconfig->path);
    unlink(buf);

    snprintf(buf, EUCA_MAX_PATH, "%s/euca-dhcp.leases", vnetconfig->path);
    if ((rc = open(buf, O_WRONLY | O_CREAT, 0644)) != -1) {
        close(rc);
    } else {
        if (check_file(buf)) {
            LOGWARN("not creating/opening %s\n", buf);
        }
    }

    if (strncmp(vnetconfig->dhcpuser, "root", 32) && strncmp(vnetconfig->path, "/", EUCA_MAX_PATH) && strstr(vnetconfig->path, "eucalyptus/net")) {
        snprintf(buf, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " chgrp -R %s %s", vnetconfig->eucahome, vnetconfig->dhcpuser, vnetconfig->path);
        LOGDEBUG("executing: %s\n", buf);
        rc = system(buf);

        snprintf(buf, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " chmod -R 0775 %s", vnetconfig->eucahome, vnetconfig->path);
        LOGDEBUG("executing: %s\n", buf);
        rc = system(buf);
    }

    snprintf(buf, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " %s -cf %s/euca-dhcp.conf -lf %s/euca-dhcp.leases -pf %s/euca-dhcp.pid -tf %s/euca-dhcp.trace %s",
             vnetconfig->eucahome, vnetconfig->dhcpdaemon, vnetconfig->path, vnetconfig->path, vnetconfig->path, vnetconfig->path, dstring);
    LOGDEBUG("executing: %s\n", buf);

    // cannot use 'daemonrun()' here, dhcpd3 is too picky about FDs and signal handlers...
    rc = system(buf);
    LOGTRACE("RC from cmd: %d\n", rc);
    return (rc);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] cc
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NO_SPACE_ERROR: if we fail to add to the list
//!
//! @pre \p vnetconfig must not be NULL.
//!
int vnetAddCCS(vnetConfig * vnetconfig, u32 cc)
{
    int i = 0;

    if (vnetconfig == NULL) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    for (i = 0; i < NUMBER_OF_CCS; i++) {
        if (vnetconfig->tunnels.ccs[i] == 0) {
            vnetconfig->tunnels.ccs[i] = cc;
            return (EUCA_OK);
        }
    }
    return (EUCA_NO_SPACE_ERROR);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] cc
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if we cannot find the cc in the list
//!
//! @pre \p vnetconfig must not be NULL.
//!
int vnetDelCCS(vnetConfig * vnetconfig, u32 cc)
{
    int i = 0;
    int rc = 0;
    char file[EUCA_MAX_PATH] = "";
    char rootwrap[EUCA_MAX_PATH] = "";

    if (vnetconfig == NULL) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    snprintf(rootwrap, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, vnetconfig->eucahome);

    for (i = 0; i < NUMBER_OF_CCS; i++) {
        if (vnetconfig->tunnels.ccs[i] == cc) {
            // bring down the tunnel

            snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/vtund-client-%d-%d.pid", vnetconfig->eucahome, vnetconfig->tunnels.localIpId, i);
            rc = safekillfile(file, "vtund", 9, rootwrap);

            vnetconfig->tunnels.ccs[i] = 0;
            return (EUCA_OK);
        }
    }
    return (EUCA_NOT_FOUND_ERROR);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] ccs
//! @param[in] ccsLen
//!
//! @return EUCA_OK on success or EUCA_INVALID_ERROR if any of our provided parameters does not meet the preconditions
//!
//! @pre \li \p vnetconfig and \p ccs must not be NULL.
//!      \li \p ccsLen must range between [0..NUMBER_OF_CCS].
//!
int vnetSetCCS(vnetConfig * vnetconfig, char **ccs, int ccsLen)
{
    int i = 0;
    int rc = 0;
    u32 tmpccs[NUMBER_OF_CCS] = { 0 };
    boolean found = FALSE;

    if ((vnetconfig == NULL) || (ccs == NULL)) {
        LOGERROR("bad input params: vnetconfig=%p, ccs=%p\n", vnetconfig, ccs);
        return (EUCA_INVALID_ERROR);
    }

    if ((ccsLen < 0) || (ccsLen > NUMBER_OF_CCS)) {
        LOGERROR("specified number of cluster controllers out of bounds (in=%d, min=%d, max=%d)\n", ccsLen, 0, NUMBER_OF_CCS);
        return (EUCA_INVALID_ERROR);
    }

    bzero(tmpccs, sizeof(u32) * NUMBER_OF_CCS);
    found = FALSE;
    for (i = 0; i < ccsLen; i++) {
        LOGDEBUG("input CC%d=%s\n", i, ccs[i]);
        tmpccs[i] = dot2hex(ccs[i]);
        rc = vnetCheckLocalIP(vnetconfig, tmpccs[i]);
        if (!rc && !found) {
            LOGDEBUG("local IP found in input list of CCs, setting localIpId: %d\n", i);
            vnetconfig->tunnels.localIpIdLast = vnetconfig->tunnels.localIpId;
            vnetconfig->tunnels.localIpId = i;
            found = TRUE;
        }
    }

    if (memcmp(tmpccs, vnetconfig->tunnels.ccs, sizeof(u32) * NUMBER_OF_CCS)) {
        // internal list is different from new list, teardown and re-construct tunnels
        LOGINFO("list of CCs has changed, initiating re-construction of tunnels\n");
        if ((rc = vnetTeardownTunnels(vnetconfig)) != 0) {
            LOGERROR("unable to teardown tunnels\n");
        }
        memcpy(vnetconfig->tunnels.ccs, tmpccs, sizeof(u32) * NUMBER_OF_CCS);
    }

    if (!found) {
        LOGDEBUG("local IP not found in input list of CCs, setting localIpId: %d\n", -1);
        vnetconfig->tunnels.localIpIdLast = vnetconfig->tunnels.localIpId;
        vnetconfig->tunnels.localIpId = -1;
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] vlan the Virtual LAN index
//! @param[in] publicIp
//! @param[in] privateIp
//! @param[in] macaddr
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @note at this point this function only returns EUCA_OK and does not do anything.
//!
int vnetStartInstanceNetwork(vnetConfig * vnetconfig, int vlan, char *publicIp, char *privateIp, char *macaddr)
{
    int rc = EUCA_OK;
    int ret = EUCA_OK;
    int i = 0;
    int numrules = 3;
    char rule[EUCA_MAX_PATH] = "";
    char rules[4][EUCA_MAX_PATH] = { "" };
    boolean done = FALSE;

    return (EUCA_OK);
    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {

    } else {
        // do ebtables to provide MAC/IP spoofing protection
        snprintf(rules[0], EUCA_MAX_PATH, "FORWARD ! -i %s -p IPv4 -s %s --ip-src %s -j ACCEPT", vnetconfig->pubInterface, macaddr, privateIp);
        snprintf(rules[1], EUCA_MAX_PATH, "FORWARD ! -i %s -p IPv4 -s %s ! --ip-src %s -j DROP", vnetconfig->pubInterface, macaddr, privateIp);
        snprintf(rules[2], EUCA_MAX_PATH, "FORWARD ! -i %s -s %s -j ACCEPT", vnetconfig->pubInterface, macaddr);

        done = FALSE;
        for (i = 0; ((i < numrules) && !done); i++) {
            snprintf(rule, EUCA_MAX_PATH, "-A %s\n", rules[i]);
            if ((rc = vnetApplySingleEBTableRule(vnetconfig, "filter", rule)) != 0) {
                LOGERROR("could not apply ebtables rule '%s'\n", rule);
                done = TRUE;
                ret = EUCA_ERROR;
            }
        }

        if (done) {
            // one of the rules failed, tear them down
            for (i = 0; i < numrules; i++) {
                snprintf(rule, EUCA_MAX_PATH, "-D %s\n", rules[i]);
                rc = vnetApplySingleEBTableRule(vnetconfig, "filter", rule);
            }
        }
    }
    return (ret);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] vlan the Virtual LAN index
//! @param[in] publicIp
//! @param[in] privateIp
//! @param[in] macaddr
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @note at this point this function only returns EUCA_OK and does not do anything.
//!
int vnetStopInstanceNetwork(vnetConfig * vnetconfig, int vlan, char *publicIp, char *privateIp, char *macaddr)
{
    int i = 0;
    int rc = EUCA_OK;
    int ret = EUCA_OK;
    int numrules = 3;
    char rule[EUCA_MAX_PATH] = "";
    char rules[3][EUCA_MAX_PATH] = { "" };
    boolean done = FALSE;

    return (EUCA_OK);

    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {

    } else {
        snprintf(rules[0], EUCA_MAX_PATH, "FORWARD ! -i %s -p IPv4 -s %s --ip-src %s -j ACCEPT", vnetconfig->pubInterface, macaddr, privateIp);
        snprintf(rules[1], EUCA_MAX_PATH, "FORWARD ! -i %s -p IPv4 -s %s ! --ip-src %s -j DROP", vnetconfig->pubInterface, macaddr, privateIp);
        snprintf(rules[2], EUCA_MAX_PATH, "FORWARD ! -i %s -s %s -j ACCEPT", vnetconfig->pubInterface, macaddr);
        for (i = 0, done = FALSE; i < numrules && !done; i++) {
            snprintf(rule, EUCA_MAX_PATH, "-D %s\n", rules[i]);
            rc = vnetApplySingleEBTableRule(vnetconfig, "filter", rule);
        }
    }
    return (ret);
}

//!
//!
//!
//! @param[in]  vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in]  vlan the Virtual LAN index
//! @param[in]  uuid the unique user identifier (UNUSED)
//! @param[in]  userName the user name string
//! @param[in]  netName the network name string
//! @param[out] outbrname the bridge device name string
//!
//! @return EUCA_OK on success and the variable \p outbrname will be set appropriately. On failure,
//!         the following error codes will be returned:
//!         \li EUCA_ERROR: if any error occured
//!         \li EUCA_INVALID_ERROR: if any of our parameter does not meet the precondition
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li \p vlan must be between 0 and \p vnetconfig->max_vlan
//!
//! @note Caller is responsible to free the memory pointed by the \p outbrname variable.
//!
int vnetStartNetworkManaged(vnetConfig * vnetconfig, int vlan, char *uuid, char *userName, char *netName, char **outbrname)
{
    int i = 0;
    int rc = 0;
    int slashnet = 0;
    char *network = NULL;
    char cmd[EUCA_MAX_PATH] = "";
    char newdevname[32] = "";
    char newbrname[32] = "";

    // check input params...
    if (!vnetconfig || !outbrname) {
        if (!vnetconfig) {
            LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
            return (EUCA_INVALID_ERROR);
        } else {
            return (EUCA_OK);
        }
    }

    LOGDEBUG("params: vnetconfig=%p, vlan=%d, uuid=%s, userName=%s, netName=%s\n", vnetconfig, vlan, SP(uuid), SP(userName), SP(netName));

    *outbrname = NULL;

    if ((vlan < 0) || (vlan > vnetconfig->max_vlan)) {
        LOGERROR("supplied vlan '%d' is out of range (%d - %d), cannot start network\n", vlan, 0, vnetconfig->max_vlan);
        return (EUCA_INVALID_ERROR);
    }

    if (vnetconfig->role == NC && vlan > 0) {
        // first, create tagged interface
        if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {
            snprintf(newdevname, 32, "%s.%d", vnetconfig->privInterface, vlan);
            if ((rc = check_device(newdevname)) != 0) {
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " vconfig add %s %d", vnetconfig->eucahome, vnetconfig->privInterface, vlan);
                if ((rc = system(cmd)) != 0) {
                    // failed to create vlan tagged device
                    LOGERROR("cannot create new vlan device %s.%d\n", vnetconfig->privInterface, vlan);
                    return (EUCA_ERROR);
                }
            }
            // create new bridge
            snprintf(newbrname, 32, "eucabr%d", vlan);
            if ((rc = check_bridge(newbrname)) != 0) {
                // bridge does not yet exist
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl addbr %s", vnetconfig->eucahome, newbrname);
                if ((rc = system(cmd)) != 0) {
                    LOGERROR("could not create new bridge %s\n", newbrname);
                    return (EUCA_ERROR);
                }
            }
            // add if to bridge
            snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl addif %s %s", vnetconfig->eucahome, newbrname, newdevname);
            rc = system(cmd);

            // bring br up
            if (check_deviceup(newbrname)) {
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip link set dev %s up", vnetconfig->eucahome, newbrname);
                rc = system(cmd);
            }
            // bring if up
            if (check_deviceup(newdevname)) {
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip link set dev %s up", vnetconfig->eucahome, newdevname);
                rc = system(cmd);
            }
        } else {
            snprintf(newbrname, 32, "%s", vnetconfig->bridgedev);
            if (!strcmp(vnetconfig->mode, NETMODE_EDGE)) {
                //ebtables rule(s) here, need mac/ip mapping and ethernet device
            }
        }

        *outbrname = strdup(newbrname);
    } else if ((vlan > 0) && ((vnetconfig->role == CC) || (vnetconfig->role == CLC))) {
        vnetconfig->networks[vlan].active = 1;
        vnetconfig->networks[vlan].createTime = time(NULL);
        for (i = 0; i <= NUMBER_OF_CCS; i++) {
            vnetconfig->networks[vlan].addrs[i].active = 1;
        }
        vnetconfig->networks[vlan].addrs[vnetconfig->numaddrs - 1].active = 1;

        rc = vnetSetVlan(vnetconfig, vlan, uuid, userName, netName);
        rc = vnetCreateChain(vnetconfig, userName, netName);

        // allow traffic on this net to flow freely
        slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
        network = hex2dot(vnetconfig->networks[vlan].nw);
        snprintf(cmd, 256, "-A FORWARD -s %s/%d -d %s/%d -j ACCEPT", network, slashnet, network, slashnet);
        rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
        EUCA_FREE(network);

        if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {
            snprintf(newdevname, 32, "%s.%d", vnetconfig->privInterface, vlan);
            if ((rc = check_device(newdevname)) != 0) {
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " vconfig add %s %d", vnetconfig->eucahome, vnetconfig->privInterface, vlan);
                if ((rc = system(cmd)) != 0) {
                    LOGERROR("could not tag %s with vlan %d\n", vnetconfig->privInterface, vlan);
                    return (EUCA_ERROR);
                }
            }
            // create new bridge
            snprintf(newbrname, 32, "eucabr%d", vlan);
            if ((rc = check_bridge(newbrname)) != 0) {
                // bridge does not yet exist
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl addbr %s", vnetconfig->eucahome, newbrname);
                if ((rc = system(cmd)) != 0) {
                    LOGERROR("could not create new bridge %s\n", newbrname);
                    return (EUCA_ERROR);
                }
                // DAN temporary
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl stp %s off", vnetconfig->eucahome, newbrname);
                if ((rc = system(cmd)) != 0) {
                    LOGWARN("could not enable stp on bridge %s\n", newbrname);
                }

                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl setfd %s 2", vnetconfig->eucahome, newbrname);
                if ((rc = system(cmd)) != 0) {
                    LOGWARN("could not set fd time to 2 on bridge %s\n", newbrname);
                }

                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl sethello %s 2", vnetconfig->eucahome, newbrname);
                if ((rc = system(cmd)) != 0) {
                    LOGWARN("could not set hello time to 2 on bridge %s\n", newbrname);
                }
            }

            snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl addif %s %s", vnetconfig->eucahome, newbrname, newdevname);
            rc = system(cmd);

            // bring br up
            if (check_deviceup(newbrname)) {
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip link set dev %s up", vnetconfig->eucahome, newbrname);
                rc = system(cmd);
            }

            snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip addr flush %s", vnetconfig->eucahome, newbrname);
            rc = system(cmd);

            // bring if up
            if (check_deviceup(newdevname)) {
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip link set dev %s up", vnetconfig->eucahome, newdevname);
                rc = system(cmd);
            }
            // attach tunnel(s)
            if ((rc = vnetAttachTunnels(vnetconfig, vlan, newbrname)) != 0) {
                LOGWARN("failed to attach tunnels for vlan %d on bridge %s\n", vlan, newbrname);
            }

            snprintf(newdevname, 32, "%s", newbrname);
        } else {
            // attach tunnel(s)
            if ((rc = vnetAttachTunnels(vnetconfig, vlan, vnetconfig->privInterface)) != 0) {
                LOGWARN("failed to attach tunnels for vlan %d on bridge %s\n", vlan, vnetconfig->privInterface);
            }

            snprintf(newdevname, 32, "%s", vnetconfig->privInterface);
        }

        if ((rc = vnetAddGatewayIP(vnetconfig, vlan, newdevname, vnetconfig->tunnels.localIpId)) != 0) {
            LOGWARN("failed to add gateway IP to device %s\n", newdevname);
        }

        *outbrname = strdup(newdevname);
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] vlan
//! @param[in] newbrname
//!
//! @return EUCA_OK on success or EUCA_INVALID_ERROR if any of our provided parameters does not meet the preconditions.
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li \p vlan must be between 0 and \p NUMBER_OF_VLANS
//!      \li \p newbrname must not be NULL and must be a valid bridge device
//!
int vnetAttachTunnels(vnetConfig * vnetconfig, int vlan, char *newbrname)
{
    int rc = 0;
    int i = 0;
    char cmd[EUCA_MAX_PATH] = "";
    char tundev[32] = "";
    char tunvlandev[32] = "";

    if (!vnetconfig) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    if (!vnetconfig->tunnels.tunneling) {
        return (EUCA_OK);
    }

    if ((vlan < 0) || (vlan >= NUMBER_OF_VLANS) || !newbrname || check_bridge(newbrname)) {
        LOGERROR("bad input params: vnetconfig=%p, vlan=%d, newbrname=%s\n", vnetconfig, vlan, SP(newbrname));
        return (EUCA_INVALID_ERROR);
    }

    if (check_bridgestp(newbrname)) {
        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl stp %s on", vnetconfig->eucahome, newbrname);
        if ((rc = system(cmd)) != 0) {
            LOGWARN("could enable stp on bridge %s\n", newbrname);
        }
    }

    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN)) {
        for (i = 0; i < NUMBER_OF_CCS; i++) {
            if (i != vnetconfig->tunnels.localIpId) {
                snprintf(tundev, 32, "tap-%d-%d", vnetconfig->tunnels.localIpId, i);
                if (!check_device(tundev) && !check_device(newbrname)) {
                    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {
                        snprintf(tunvlandev, 32, "tap-%d-%d.%d", vnetconfig->tunnels.localIpId, i, vlan);
                        if (check_device(tunvlandev)) {
                            snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " vconfig add %s %d", vnetconfig->eucahome, tundev, vlan);
                            LOGDEBUG("running cmd '%s'\n", cmd);
                            rc = system(cmd);
                            rc = rc >> 8;
                        }
                    } else {
                        snprintf(tunvlandev, 32, "%s", tundev);
                    }

                    if (check_bridgedev(newbrname, tunvlandev)) {
                        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl addif %s %s", vnetconfig->eucahome, newbrname, tunvlandev);
                        LOGDEBUG("running cmd '%s'\n", cmd);
                        rc = system(cmd);
                        rc = rc >> 8;
                    }

                    if (check_deviceup(tunvlandev)) {
                        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip link set up dev %s", vnetconfig->eucahome, tunvlandev);
                        LOGDEBUG("running cmd '%s'\n", cmd);
                        rc = system(cmd);
                        rc = rc >> 8;
                    }
                }

                snprintf(tundev, 32, "tap-%d-%d", i, vnetconfig->tunnels.localIpId);
                if (!check_device(tundev) && !check_device(newbrname)) {
                    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {
                        snprintf(tunvlandev, 32, "tap-%d-%d.%d", i, vnetconfig->tunnels.localIpId, vlan);
                        if (check_device(tunvlandev)) {
                            snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " vconfig add %s %d", vnetconfig->eucahome, tundev, vlan);
                            LOGDEBUG("running cmd '%s'\n", cmd);
                            rc = system(cmd);
                            rc = rc >> 8;
                        }
                    } else {
                        snprintf(tunvlandev, 32, "%s", tundev);
                    }

                    if (check_bridgedev(newbrname, tunvlandev)) {
                        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " brctl addif %s %s", vnetconfig->eucahome, newbrname, tunvlandev);
                        LOGDEBUG("running cmd '%s'\n", cmd);
                        rc = system(cmd);
                        rc = rc >> 8;
                    }

                    if (check_deviceup(tunvlandev)) {
                        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip link set up dev %s", vnetconfig->eucahome, tunvlandev);
                        LOGDEBUG("running cmd '%s'\n", cmd);
                        rc = system(cmd);
                        rc = rc >> 8;
                    }
                }
            }
        }
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] vlan
//! @param[in] newbrname
//!
//! @return EUCA_OK on success or EUCA_INVALID_ERROR if any parameters does not meet the preconditions.
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li \p vlan must be between 0 and \p NUMBER_OF_VLANS.
//!
int vnetDetachTunnels(vnetConfig * vnetconfig, int vlan, char *newbrname)
{
    int rc = 0;
    int i = 0;
    int slashnet = 0;
    char cmd[EUCA_MAX_PATH] = "";
    char tundev[32] = "";
    char tunvlandev[32] = "";
    char *network = NULL;

    if ((vnetconfig == NULL) || (vlan < 0) || (vlan >= NUMBER_OF_VLANS)) {
        LOGERROR("bad input params: vnetconfig=%p, vlan=%d, newbrname=%s\n", vnetconfig, vlan, SP(newbrname));
        return (EUCA_INVALID_ERROR);
    }

    slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
    network = hex2dot(vnetconfig->networks[vlan].nw);
    snprintf(cmd, EUCA_MAX_PATH, "-D FORWARD -s %s/%d -d %s/%d -j ACCEPT", network, slashnet, network, slashnet);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
    EUCA_FREE(network);

    for (i = 0; i < NUMBER_OF_CCS; i++) {
        if (i != vnetconfig->tunnels.localIpId) {
            snprintf(tundev, 32, "tap-%d-%d", vnetconfig->tunnels.localIpId, i);
            if (!check_device(tundev) && !check_device(newbrname)) {
                snprintf(tunvlandev, 32, "tap-%d-%d.%d", vnetconfig->tunnels.localIpId, i, vlan);
                if (!check_device(tunvlandev)) {
                    snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " vconfig rem %s", vnetconfig->eucahome, tunvlandev);
                    LOGDEBUG("running cmd '%s'\n", cmd);
                    rc = system(cmd);
                    rc = rc >> 8;
                }
            }

            snprintf(tundev, 32, "tap-%d-%d", i, vnetconfig->tunnels.localIpId);
            if (!check_device(tundev) && !check_device(newbrname)) {
                snprintf(tunvlandev, 32, "tap-%d-%d.%d", i, vnetconfig->tunnels.localIpId, vlan);
                if (!check_device(tunvlandev)) {
                    snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " vconfig rem %s", vnetconfig->eucahome, tunvlandev);
                    LOGDEBUG("running cmd '%s'\n", cmd);
                    rc = system(cmd);
                    rc = rc >> 8;
                }
            }
        }
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return the results of the vnetTeardownTunnelsVTUN() call.
//!
//! @see vnetTeardownTunnelsVTUN()
//!
int vnetTeardownTunnels(vnetConfig * vnetconfig)
{
    return (vnetTeardownTunnelsVTUN(vnetconfig));
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return EUCA_OK on succes or EUCA_INVALID_ERROR if any parameter does not meet the preconditions
//!
//! @pre \p vnetconfig must not be NULL.
//!
int vnetTeardownTunnelsVTUN(vnetConfig * vnetconfig)
{
    int i = 0;
    int rc = 0;
    char file[EUCA_MAX_PATH] = "";
    char rootwrap[EUCA_MAX_PATH] = "";

    if (vnetconfig == NULL)
        return (EUCA_INVALID_ERROR);

    snprintf(rootwrap, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, vnetconfig->eucahome);

    snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/vtund-server.pid", vnetconfig->eucahome);
    rc = safekillfile(file, "vtund", 9, rootwrap);

    if (vnetconfig->tunnels.localIpId != -1) {
        for (i = 0; i < NUMBER_OF_CCS; i++) {
            if (vnetconfig->tunnels.ccs[i] != 0) {
                snprintf(file, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/vtund-client-%d-%d.pid", vnetconfig->eucahome, vnetconfig->tunnels.localIpId, i);
                rc = safekillfile(file, "vtund", 9, rootwrap);
            }
        }
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return the result of the vnetSetupTunnelsVTUN() call
//!
//! @see vnetSetupTunnelsVTUN()
//!
int vnetSetupTunnels(vnetConfig * vnetconfig)
{
    return (vnetSetupTunnelsVTUN(vnetconfig));
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return EUCA_OK on success or EUCA_INVALID_ERROR if any parameter does not meet the preconditions
//!
//! @pre \p vnetconfig must not be NULL
//!
int vnetSetupTunnelsVTUN(vnetConfig * vnetconfig)
{
    int i = 0;
    int rc = 0;
    char cmd[EUCA_MAX_PATH] = "";
    char tundev[32] = "";
    char *remoteIp = NULL;
    char pidfile[EUCA_MAX_PATH] = "";
    char rootwrap[EUCA_MAX_PATH] = "";

    if (vnetconfig == NULL) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    if (!vnetconfig->tunnels.tunneling || (vnetconfig->tunnels.localIpId == -1)) {
        return (EUCA_OK);
    }
    snprintf(rootwrap, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP, vnetconfig->eucahome);

    snprintf(pidfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/vtund-server.pid", vnetconfig->eucahome);
    snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " vtund -s -n -f " EUCALYPTUS_KEYS_DIR "/vtunall.conf", vnetconfig->eucahome, vnetconfig->eucahome);
    if ((rc = daemonmaintain(cmd, "vtund", pidfile, 0, rootwrap)) != 0) {
        LOGERROR("cannot run tunnel server: '%s'\n", cmd);
    }

    for (i = 0; i < NUMBER_OF_CCS; i++) {
        if (vnetconfig->tunnels.ccs[i] != 0) {
            remoteIp = hex2dot(vnetconfig->tunnels.ccs[i]);
            if (vnetconfig->tunnels.localIpId != i) {
                snprintf(tundev, 32, "tap-%d-%d", vnetconfig->tunnels.localIpId, i);
                if ((rc = check_device(tundev)) != 0) {
                    LOGDEBUG("maintaining tunnel for endpoint: %s\n", remoteIp);
                    snprintf(pidfile, EUCA_MAX_PATH, EUCALYPTUS_RUN_DIR "/vtund-client-%d-%d.pid", vnetconfig->eucahome, vnetconfig->tunnels.localIpId, i);
                    snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " vtund -n -f " EUCALYPTUS_KEYS_DIR "/vtunall.conf -p tun-%d-%d %s",
                             vnetconfig->eucahome, vnetconfig->eucahome, vnetconfig->tunnels.localIpId, i, remoteIp);
                    if ((rc = daemonmaintain(cmd, "vtund", pidfile, 0, rootwrap)) != 0) {
                        LOGERROR("cannot run tunnel client: '%s'\n", cmd);
                    } else {
                        LOGDEBUG("ran cmd '%s'\n", cmd);
                    }
                }
            }
            EUCA_FREE(remoteIp);
        }
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] vlan
//! @param[in] devname
//! @param[in] localIpId
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any error occured while adding the gateway IP
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li \p vlan must be between 0 and \p NUMBER_OF_VLANS
//!
//! @note if \p localIpId is less than 0, a default value of 0 will be assumed.
//!
int vnetAddGatewayIP(vnetConfig * vnetconfig, int vlan, char *devname, int localIpId)
{
    char *newip = NULL;
    char *broadcast = NULL;
    int rc = 0;
    int slashnet = 0;
    char cmd[EUCA_MAX_PATH] = "";

    if ((vnetconfig == NULL) || (vlan < 0) || (vlan >= NUMBER_OF_VLANS)) {
        LOGERROR("bad input params: vnetconfig=%p vlan=%d, devname=%s, lovalIpId=%d\n", vnetconfig, vlan, SP(devname), localIpId);
        return (EUCA_INVALID_ERROR);
    }

    if (localIpId < 0) {
        LOGWARN("negative localIpId supplied, defaulting to base gw\n");
        localIpId = 0;
    }

    newip = hex2dot(vnetconfig->networks[vlan].router + localIpId);
    broadcast = hex2dot(vnetconfig->networks[vlan].bc);
    LOGDEBUG("adding gateway IP: %s\n", newip);

    slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
    snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip addr add %s/%d broadcast %s dev %s", vnetconfig->eucahome, newip, slashnet, broadcast, devname);

    LOGDEBUG("running cmd '%s'\n", cmd);
    rc = system(cmd);
    rc = rc >> 8;
    if (rc && (rc != 2)) {
        LOGERROR("could not bring up new device %s with ip %s\n", devname, newip);
        EUCA_FREE(newip);
        EUCA_FREE(broadcast);
        return (EUCA_ERROR);
    }
    EUCA_FREE(newip);
    EUCA_FREE(broadcast);

    if (check_deviceup(devname)) {
        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip link set dev %s up", vnetconfig->eucahome, devname);
        rc = system(cmd);
        rc = rc >> 8;
        if (rc) {
            LOGERROR("could not bring up interface '%s'\n", devname);
            return (EUCA_ERROR);
        }
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to apply the ARP table rules on the system
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_MEMORY_ERROR: if we fail to allocate memory for our temp file name
//!         \li EUCA_PERMISSION_ERROR: if we fail to create our temporary file
//!         \li EUCA_ACCESS_ERROR: if we fail to open out temporary file for writting
//!
//! @pre \p vnetconfig must not be NULL.
//!
int vnetApplyArpTableRules(vnetConfig * vnetconfig)
{
    int rc = 0;
    int fd = 0;
    int ret = EUCA_OK;
    int i = 0;
    int j = 0;
    int k = 0;
    int done = 0;
    int slashnet = 0;
    char *file = NULL;
    char cmd[256] = "";
    char *net = NULL;
    char *gw = NULL;
    char *ip = NULL;
    FILE *FH = NULL;

    if (!vnetconfig) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    LOGDEBUG("applying arptable rules\n");
    if ((file = strdup("/tmp/euca-arpt-XXXXXX")) == NULL) {
        return (EUCA_MEMORY_ERROR);
    }

    if ((fd = safe_mkstemp(file)) < 0) {
        EUCA_FREE(file);
        return (EUCA_PERMISSION_ERROR);
    }

    chmod(file, 0644);
    if ((FH = fdopen(fd, "w")) == NULL) {
        close(fd);
        unlink(file);
        EUCA_FREE(file);
        return (EUCA_ACCESS_ERROR);
    }

    for (i = 0; i < NUMBER_OF_VLANS; i++) {
        if (vnetconfig->networks[i].active) {
            net = hex2dot(vnetconfig->networks[i].nw);
            gw = hex2dot(vnetconfig->networks[i].router);

            for (j = 0; j < NUMBER_OF_HOSTS_PER_VLAN; j++) {
                if (vnetconfig->networks[i].addrs[j].ip && vnetconfig->networks[i].addrs[j].active) {
                    for (k = 0, done = 0; k < NUMBER_OF_PUBLIC_IPS && !done; k++) {
                        if (vnetconfig->publicips[k].allocated && (vnetconfig->publicips[k].dstip == vnetconfig->networks[i].addrs[j].ip)) {
                            if ((ip = hex2dot(vnetconfig->networks[i].addrs[j].ip)) != NULL) {
                                if (gw) {
                                    fprintf(FH, "IP=%s,%s\n", ip, gw);
                                    done++;
                                }
                                EUCA_FREE(ip);
                            }
                        }
                    }
                }
            }

            for (k = 0; k < NUMBER_OF_PUBLIC_IPS; k++) {
                if (vnetconfig->publicips[k].allocated && vnetconfig->publicips[k].dstip) {
                    if ((ip = hex2dot(vnetconfig->publicips[k].dstip)) != NULL) {
                        if (gw) {
                            fprintf(FH, "IP=%s,%s\n", ip, gw);
                        }
                        EUCA_FREE(ip);
                    }
                }
            }

            if (net && gw) {
                slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[i].nm)) + 1);
                fprintf(FH, "NET=%s/%d,%s\n", net, slashnet, gw);
            }
            EUCA_FREE(gw);
            EUCA_FREE(net);
        }
    }

    fclose(FH);
    close(fd);

    snprintf(cmd, 256, EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/euca_arpt %s", vnetconfig->eucahome, vnetconfig->eucahome, file);
    if ((rc = system(cmd)) != 0) {
        ret = EUCA_ERROR;
    }

    unlink(file);
    EUCA_FREE(file);
    return (ret);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] vlan
//! @param[in] devname
//! @param[in] localIpId
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to remove the gateway IP on the device
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @pre \li \p vnetconfig must not be NULL.
//!      \li \p vlan must be between 0 and \p NUMBER_OF_VLANS.
//!      \li \p devname must not be NULL.
//!
//! @note if \p localIpId is less than 0, a default value of 0 will be assumed.
//!
int vnetDelGatewayIP(vnetConfig * vnetconfig, int vlan, char *devname, int localIpId)
{
    int rc = 0;
    int ret = EUCA_OK;
    int slashnet = 0;
    char *newip = NULL;
    char *broadcast = NULL;
    char cmd[EUCA_MAX_PATH] = "";

    if ((vnetconfig == NULL) || (vlan < 0) || (vlan >= NUMBER_OF_VLANS) || (devname == NULL)) {
        LOGERROR("bad input params: vnetconfig=%p, vlan=%d, devname=%s, localIpId=%d\n", vnetconfig, vlan, SP(devname), localIpId);
        return (EUCA_INVALID_ERROR);
    }

    if (localIpId < 0) {
        LOGWARN("negative localIpId supplied, defaulting to base gw\n");
        localIpId = 0;
    }

    newip = hex2dot(vnetconfig->networks[vlan].router + localIpId);
    broadcast = hex2dot(vnetconfig->networks[vlan].bc);
    LOGDEBUG("removing gateway IP: %s\n", newip);
    slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
    snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip addr del %s/%d broadcast %s dev %s", vnetconfig->eucahome, newip, slashnet, broadcast, devname);
    if ((rc = system(cmd)) != 0) {
        LOGERROR("could not bring down new device %s with ip %s\n", devname, newip);
        ret = EUCA_ERROR;
    }

    EUCA_FREE(newip);
    EUCA_FREE(broadcast);
    return (ret);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] vlan
//! @param[in] userName
//! @param[in] netName
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to stop the network
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @pre \p vnetconfig must not be NULL.
//!
//! @note If \p vlan is outside the [0..vnetconfig->max_vlan] range, then we have nothing to
//!       do and EUCA_OK will be returned.
//!
int vnetStopNetworkManaged(vnetConfig * vnetconfig, int vlan, char *userName, char *netName)
{
    int rc = 0;
    int ret = EUCA_OK;
    int slashnet = 0;
    char *network = NULL;
    char cmd[EUCA_MAX_PATH] = "";
    char newdevname[32] = "";
    char newbrname[32] = "";

    if (vnetconfig == NULL) {
        LOGERROR("bad input params: vnetconfig=%p\n", vnetconfig);
        return (EUCA_INVALID_ERROR);
    }

    if ((vlan < 0) || (vlan > vnetconfig->max_vlan)) {
        LOGWARN("supplied vlan '%d' is out of range (%d - %d), nothing to do\n", vlan, 0, vnetconfig->max_vlan);
        return (EUCA_OK);
    }

    vnetconfig->networks[vlan].active = 0;
    bzero(vnetconfig->networks[vlan].addrs, sizeof(netEntry) * NUMBER_OF_HOSTS_PER_VLAN);

    if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {
        snprintf(newbrname, 32, "eucabr%d", vlan);
        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip link set dev %s down", vnetconfig->eucahome, newbrname);
        if ((rc = system(cmd)) != 0) {
            LOGERROR("cmd '%s' failed\n", cmd);
            ret = EUCA_ERROR;
        }
        // DAN temporary for QA, re-enable for release
        snprintf(newdevname, 32, "%s.%d", vnetconfig->privInterface, vlan);
        if ((rc = check_device(newdevname)) == 0) {
            snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip link set dev %s down", vnetconfig->eucahome, newdevname);
            if ((rc = system(cmd)) != 0) {
                LOGERROR("cmd '%s' failed\n", cmd);
                ret = EUCA_ERROR;
            }

            snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " vconfig rem %s", vnetconfig->eucahome, newdevname);
            if ((rc = system(cmd)) != 0) {
                LOGERROR("cmd '%s' failed\n", cmd);
                ret = EUCA_ERROR;
            }
        }
        snprintf(newdevname, 32, "%s", newbrname);
    } else {
        snprintf(newdevname, 32, "%s", vnetconfig->privInterface);
    }

    if ((vnetconfig->role == CC || vnetconfig->role == CLC)) {
        // disallow traffic on this net from flowing freely
        slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->networks[vlan].nm)) + 1);
        network = hex2dot(vnetconfig->networks[vlan].nw);
        snprintf(cmd, EUCA_MAX_PATH, "-D FORWARD -s %s/%d -d %s/%d -j ACCEPT", network, slashnet, network, slashnet);
        rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);
        EUCA_FREE(network);

        if (!strcmp(vnetconfig->mode, NETMODE_MANAGED)) {
            if ((rc = vnetDetachTunnels(vnetconfig, vlan, newbrname)) != 0) {
                LOGWARN("failed to detach tunnels\n");
            }

            if ((rc = vnetDelDev(vnetconfig, newdevname)) != 0) {
                LOGWARN("could not remove '%s' from list of interfaces\n", newdevname);
            }
        }

        if ((rc = vnetDelGatewayIP(vnetconfig, vlan, newdevname, vnetconfig->tunnels.localIpId)) != 0) {
            LOGWARN("failed to delete gateway IP from interface %s\n", newdevname);
        }

        if (userName && netName) {
            if ((rc = vnetDeleteChain(vnetconfig, userName, netName)) != 0) {
                LOGERROR("could not delete chain (%s/%s)\n", userName, netName);
                ret = EUCA_ERROR;
            }
        }
    }

    return (ret);
}

//!
//!
//!
//! @param[in]  vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in]  vlan
//! @param[in]  uuid
//! @param[in]  userName
//! @param[in]  netName
//! @param[out] outbrname
//!
//! @return If we are set for managed network mode, then the result of vnetStartNetworkManaged() is
//!         returned. If any other network mode, EUCA_OK is returned on success and \p ourbrname is
//!         set properly or the following error codes on failure:
//!         \li EUCA_MEMORY_ERROR: if we fail to allocate memory for \p outbrname
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!
//! @see vnetStartNetworkManaged()
//!
//! @pre \p vnetconfig and \p outbrname must not be NULL.
//!
//! @note Caller is responsible to free memory allocated for \p outbrname.
//!
int vnetStartNetwork(vnetConfig * vnetconfig, int vlan, char *uuid, char *userName, char *netName, char **outbrname)
{
    int rc = EUCA_OK;

    if ((vnetconfig == NULL) || (outbrname == NULL)) {
        LOGERROR("bad input params: vnetconfig=%p, outbrname=%p\n", vnetconfig, outbrname);
        return (EUCA_INVALID_ERROR);
    }

    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC) || !strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        if (vnetconfig->role == NC) {
            *outbrname = strdup(vnetconfig->bridgedev);
        } else {
            *outbrname = strdup(vnetconfig->privInterface);
        }

        if (*outbrname == NULL) {
            LOGERROR("out of memory!\n");
            return (EUCA_MEMORY_ERROR);
        }

        rc = EUCA_OK;
    } else {
        rc = vnetStartNetworkManaged(vnetconfig, vlan, uuid, userName, netName, outbrname);
    }

    if ((vnetconfig->role != NC) && outbrname && *outbrname) {
        vnetAddDev(vnetconfig, *outbrname);
    }
    return (rc);
}

//!
//!
//!
//! @param[in]  vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in]  ip
//! @param[out] dstip
//! @param[out] allocated
//! @param[out] addrdevno
//!
//! @return EUCA_OK on success and \p allocated, \p addrdevno and \p dstip (if not NULL) are being
//!         set appropriately. If any error occured the following error codes are returned:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if we cannot find the given IP in our public IP list
//!
//! @pre \p vnetconfig, \p ip, \p allocated and \p addrdevno must not be NULL.
//!
int vnetGetPublicIP(vnetConfig * vnetconfig, char *ip, char **dstip, int *allocated, int *addrdevno)
{
    int i = 0;
    boolean done = FALSE;

    // @todo CHUCK vnetGetPublicIP not found in param_check???
    if (param_check("vnetGetPublicIP", vnetconfig, ip, allocated, addrdevno)) {
        LOGERROR("bad input params: vnetconfig=%p, ip=%s, allocated=%p, addrdevno=%p\n", vnetconfig, SP(ip), allocated, addrdevno);
        return (EUCA_INVALID_ERROR);
    }

    if ((vnetconfig == NULL) || (ip == NULL) || (allocated == NULL) || (addrdevno == NULL)) {
        LOGERROR("bad input params: vnetconfig=%p, ip=%s, allocated=%p, addrdevno=%p\n", vnetconfig, SP(ip), allocated, addrdevno);
        return (EUCA_INVALID_ERROR);
    }

    *allocated = *addrdevno = 0;
    for (i = 1, done = FALSE; ((i < NUMBER_OF_PUBLIC_IPS) && !done); i++) {
        if (vnetconfig->publicips[i].ip == dot2hex(ip)) {
            if (dstip != NULL) {
                *dstip = hex2dot(vnetconfig->publicips[i].dstip);
            }
            *allocated = vnetconfig->publicips[i].allocated;
            *addrdevno = i;
            done = TRUE;
        }
    }

    if (!done) {
        LOGERROR("could not find ip %s in list of allocateable publicips\n", ip);
        return (EUCA_NOT_FOUND_ERROR);
    }
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] ip
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_INVALID_ERROR: if any parameters does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if the given IP is not part of our known public IPs
//!
//! @pre \p vnetconfig and \p ip must not be NULL.
//!
int vnetCheckPublicIP(vnetConfig * vnetconfig, char *ip)
{
    int i = 0;
    u32 theip = 0;

    if (!vnetconfig || !ip) {
        LOGERROR("bad input params: vnetconfig=%p, ip=%s\n", vnetconfig, SP(ip));
        return (EUCA_INVALID_ERROR);
    }

    theip = dot2hex(ip);

    for (i = 0; i < NUMBER_OF_PUBLIC_IPS; i++) {
        if (vnetconfig->publicips[i].ip == theip) {
            return (EUCA_OK);
        }
    }
    return (EUCA_NOT_FOUND_ERROR);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] inip
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_NO_SPACE_ERROR: if we have no space left in the public IPs list
//!
//! @pre \p vnetconfig and \p inip must not be NULL.
//!
int vnetAddPublicIP(vnetConfig * vnetconfig, char *inip)
{
    int i = 0;
    int j = 0;
    int slashnet = 0;
    int numips = 0;
    int found = 0;
    u32 minip = 0;
    u32 theip = 0;
    char *ip = NULL;
    char *ptr = NULL;
    boolean done = FALSE;

    if (param_check("vnetAddPublicIP", vnetconfig, inip)) {
        LOGERROR("bad input params: vnetconfig=%p, inip=%s\n", vnetconfig, SP(inip));
        return (EUCA_INVALID_ERROR);
    }

    if (inip[0] == '!') {
        // remove mode
        ip = inip + 1;

        theip = dot2hex(ip);
        for (i = 1, done = FALSE; ((i < NUMBER_OF_PUBLIC_IPS) && !done); i++) {
            if (vnetconfig->publicips[i].ip == theip) {
                vnetconfig->publicips[i].ip = 0;
                done = TRUE;
            }
        }
    } else {
        // add mode
        ip = inip;
        slashnet = 0;
        if ((ptr = strchr(ip, '/'))) {
            *ptr = '\0';
            ptr++;
            theip = dot2hex(ip);
            slashnet = atoi(ptr);
            minip = theip + 1;
            numips = pow(2.0, (double)(32 - slashnet)) - 2;
        } else if ((ptr = strchr(ip, '-'))) {
            *ptr = '\0';
            ptr++;
            minip = dot2hex(ip);
            theip = dot2hex(ptr);
            numips = (theip - minip) + 1;
            // check (ip >= 0x7F000000 && ip <= 0x7FFFFFFF) looks for ip in lo range
            if (numips <= 0 || numips > 256 || (minip >= 0x7F000000 && minip <= 0x7FFFFFFF) || (theip >= 0x7F000000 && theip <= 0x7FFFFFFF)) {
                LOGERROR("incorrect PUBLICIPS range specified: %s-%s\n", ip, ptr);
                numips = 0;
            }
        } else {
            minip = dot2hex(ip);
            numips = 1;
        }

        for (j = 0; j < numips; j++) {
            theip = minip + j;
            for (i = 1, done = FALSE, found = 0; ((i < NUMBER_OF_PUBLIC_IPS) && !done); i++) {
                if (!vnetconfig->publicips[i].ip) {
                    if (!found)
                        found = i;
                } else if (vnetconfig->publicips[i].ip == theip) {
                    done = TRUE;
                }
            }

            if (done) {
                //already there
            } else if (found) {
                vnetconfig->publicips[found].ip = theip;
            } else {
                LOGERROR("cannot add any more public IPS (limit:%d)\n", NUMBER_OF_PUBLIC_IPS);
                return (EUCA_NO_SPACE_ERROR);
            }
        }
    }

    return (EUCA_OK);
}

int vnetAddPrivateIP(vnetConfig * vnetconfig, char *inip)
{
    int i = 0;
    int slashnet = 0;
    int numips = 0;
    int j = 0;
    int found = 0;
    u32 minip = 0;
    u32 theip = 0;
    char *ip = NULL;
    char *ptr = NULL;
    char *theipstr = NULL;
    char *themacstr = NULL;
    boolean done = FALSE;

    if (param_check("vnetAddPrivateIP", vnetconfig, inip)) {
        LOGERROR("bad input params: vnetconfig=%p, inip=%s\n", vnetconfig, SP(inip));
        return (EUCA_INVALID_ERROR);
    }

    if (inip[0] == '!') {
        // remove mode
        ip = inip + 1;

        theip = dot2hex(ip);
        for (i = 1, done = FALSE; ((i < NUMBER_OF_PRIVATE_IPS) && !done); i++) {
            if (vnetconfig->privateips[i].ip == theip) {
                vnetconfig->privateips[i].ip = 0;
                done = TRUE;
            }
        }
    } else {
        // add mode
        ip = inip;
        slashnet = 0;
        if ((ptr = strchr(ip, '/'))) {
            *ptr = '\0';
            ptr++;
            theip = dot2hex(ip);
            slashnet = atoi(ptr);
            minip = theip + 1;
            numips = pow(2.0, (double)(32 - slashnet)) - 2;
        } else if ((ptr = strchr(ip, '-'))) {
            *ptr = '\0';
            ptr++;
            minip = dot2hex(ip);
            theip = dot2hex(ptr);
            numips = (theip - minip) + 1;
            // check (ip >= 0x7F000000 && ip <= 0x7FFFFFFF) looks for ip in lo range
            if (numips <= 0 || numips > 256 || (minip >= 0x7F000000 && minip <= 0x7FFFFFFF) || (theip >= 0x7F000000 && theip <= 0x7FFFFFFF)) {
                LOGERROR("incorrect PRIVATEIPS range specified: %s-%s\n", ip, ptr);
                numips = 0;
            }
        } else {
            minip = dot2hex(ip);
            numips = 1;
        }

        for (j = 0; j < numips; j++) {
            theip = minip + j;
            for (i = 1, done = FALSE, found = 0; ((i < NUMBER_OF_PRIVATE_IPS) && !done); i++) {
                if (!vnetconfig->privateips[i].ip) {
                    if (!found)
                        found = i;
                } else if (vnetconfig->privateips[i].ip == theip) {
                    done = TRUE;
                }
            }

            if (done) {
                //already there
            } else if (found) {
                if (!strcmp(vnetconfig->mode, NETMODE_EDGE)) {
                    theipstr = hex2dot(theip);
                    if (theipstr)
                        themacstr = ipdot2macdot(theipstr, vnetconfig->macPrefix);

                    if (theipstr && themacstr) {
                        vnetRefreshHost(vnetconfig, themacstr, theipstr, 0, -1);
                    }
                    EUCA_FREE(themacstr);
                    EUCA_FREE(theipstr);
                } else {
                    vnetconfig->privateips[found].ip = theip;
                }
            } else {
                LOGERROR("cannot add any more privatec IPS (limit:%d)\n", NUMBER_OF_PRIVATE_IPS);
                return (EUCA_NO_SPACE_ERROR);
            }
        }
    }

    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] src
//! @param[in] dst
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to assign the address
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre \p vnetconfig, \p src and \p dst must not be NULL.
//!
int vnetAssignAddress(vnetConfig * vnetconfig, char *src, char *dst)
{
    int rc = 0;
    int slashnet = 0;
    int ret = EUCA_OK;
    char cmd[EUCA_MAX_PATH] = "";
    char *network = NULL;

    if ((vnetconfig == NULL) || (src == NULL) || (dst == NULL)) {
        LOGERROR("bad input params: vnetconfig=%p, src=%s, dst=%s\n", vnetconfig, SP(src), SP(dst));
        return (EUCA_INVALID_ERROR);
    }

    if (((vnetconfig->role == CC) || (vnetconfig->role == CLC)) && (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN))) {
        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip addr add %s/32 dev %s", vnetconfig->eucahome, src, vnetconfig->pubInterface);
        LOGDEBUG("running cmd %s\n", cmd);
        rc = system(cmd);
        rc = rc >> 8;
        if (rc && (rc != 2)) {
            LOGERROR("failed to assign IP address '%s'\n", cmd);
            ret = EUCA_ERROR;
        }

        snprintf(cmd, EUCA_MAX_PATH, "-A PREROUTING -d %s -j DNAT --to-destination %s", src, dst);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd)) != 0) {
            LOGERROR("failed to apply DNAT rule '%s'\n", cmd);
            ret = EUCA_ERROR;
        }

        snprintf(cmd, EUCA_MAX_PATH, "-A OUTPUT -d %s -j DNAT --to-destination %s", src, dst);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd)) != 0) {
            LOGERROR("failed to apply DNAT rule '%s'\n", cmd);
            ret = EUCA_ERROR;
        }

        slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->nm)) + 1);
        network = hex2dot(vnetconfig->nw);
        snprintf(cmd, EUCA_MAX_PATH, "-I POSTROUTING -s %s -j SNAT --to-source %s", dst, src);
        EUCA_FREE(network);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd)) != 0) {
            LOGERROR("failed to apply SNAT rule '%s'\n", cmd);
            ret = EUCA_ERROR;
        }
        // For reporting traffic statistics.
        snprintf(cmd, EUCA_MAX_PATH, "-A EUCA_COUNTERS_IN -d %s", dst);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
            LOGERROR("vnetAssignAddress(): failed to apply EUCA_COUNTERS_IN rule '%s'\n", cmd);
            ret = EUCA_ERROR;
        }

        snprintf(cmd, EUCA_MAX_PATH, "-A EUCA_COUNTERS_OUT -s %s", dst);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
            LOGERROR("vnetAssignAddress(): failed to apply EUCA_COUNTERS_OUT rule '%s'\n", cmd);
            ret = EUCA_ERROR;
        }
    }
    return (ret);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] uuid
//! @param[in] ip
//! @param[in] dstip
//!
//! @return the result of the vnetSetPublicIP() call.
//!
//! @see vnetSetPublicIP()
//!
int vnetAllocatePublicIP(vnetConfig * vnetconfig, char *uuid, char *ip, char *dstip)
{
    return (vnetSetPublicIP(vnetconfig, uuid, ip, dstip, 1));
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] uuid
//! @param[in] ip
//! @param[in] dstip
//!
//! @return the result of the vnetSetPublicIP() call.
//!
//! @see vnetSetPublicIP()
//!
int vnetDeallocatePublicIP(vnetConfig * vnetconfig, char *uuid, char *ip, char *dstip)
{
    return (vnetSetPublicIP(vnetconfig, uuid, ip, NULL, 0));
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] uuid
//! @param[in] ip
//! @param[in] dstip
//! @param[in] setval
//!
//! @return EUCA_OK on success or EUCA_INVALID_ERROR if any parameter does not meet the preconditions
//!
//! @pre \p vnetconfig and \p ip must not be NULL.
//!
int vnetSetPublicIP(vnetConfig * vnetconfig, char *uuid, char *ip, char *dstip, int setval)
{
    int i = 0;
    u32 hip = 0;

    //! @todo CHUCK -> vnetSetPublicIP not set for param_check()
    if (param_check("vnetSetPublicIP", vnetconfig, ip, setval)) {
        LOGERROR("bad input params: vnetconfig=%p, ip=%s, setval=%d\n", vnetconfig, SP(ip), setval);
        return (EUCA_INVALID_ERROR);
    }

    hip = dot2hex(ip);
    for (i = 1; i < NUMBER_OF_PUBLIC_IPS; i++) {
        if (vnetconfig->publicips[i].ip == hip) {
            if (dstip) {
                vnetconfig->publicips[i].dstip = dot2hex(dstip);
            } else {
                vnetconfig->publicips[i].dstip = 0;
            }

            vnetconfig->publicips[i].allocated = setval;
            if (uuid) {
                if (setval) {
                    snprintf(vnetconfig->publicips[i].uuid, 48, "%s", uuid);
                } else {
                    bzero(vnetconfig->publicips[i].uuid, sizeof(char) * 48);
                }
            } else {
                bzero(vnetconfig->publicips[i].uuid, sizeof(char) * 48);
            }
            return (EUCA_OK);
        }
    }
    return (EUCA_OK);

}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] uuid
//! @param[in] src
//! @param[in] dst
//!
//! @return EUCA_OK on success; or the result of vnetUnassignAddress(), vnetAssignAddress(); or the
//!         following error codes:
//!         \li EUCA_INVALID_ERROR:
//!         \li EUCA_NOT_FOUND_ERROR:
//!
//! @see vnetAssignAddress()
//! @see vnetUnassignAddress()
//!
//! @pre \p vnetconfig, \p uuid, \p src and \p dst must not be NULL.
//!
int vnetReassignAddress(vnetConfig * vnetconfig, char *uuid, char *src, char *dst)
{
    int i = 0;
    int isallocated = 0;
    int pubidx = 0;
    int rc = EUCA_OK;
    char *currdst = NULL;
    boolean done = FALSE;

    // assign address if unassigned, unassign/reassign if assigned
    if (!vnetconfig || !src || !dst) {
        LOGERROR("bad input params: vnetconfig=%p, uuid=%s, src=%s, dst=%s\n", vnetconfig, SP(uuid), SP(src), SP(dst));
        return (EUCA_INVALID_ERROR);
    }
    // get the publicIP of interest
    isallocated = 0;
    pubidx = 0;
    currdst = NULL;
    for (i = 1, done = FALSE; ((i < NUMBER_OF_PUBLIC_IPS) && !done); i++) {
        if (vnetconfig->publicips[i].ip == dot2hex(src)) {
            currdst = hex2dot(vnetconfig->publicips[i].dstip);
            isallocated = vnetconfig->publicips[i].allocated;
            pubidx = i;
            done = TRUE;
        }
    }

    if (!done) {
        LOGERROR("could not find ip %s in list of allocateable publicips\n", src);
        return (EUCA_NOT_FOUND_ERROR);
    }

    LOGDEBUG("deciding what to do: src=%s dst=%s allocated=%d currdst=%s\n", SP(src), SP(dst), isallocated, SP(currdst));
    // determine if reassign must happen
    if (isallocated && strcmp(currdst, dst)) {
        if ((rc = vnetUnassignAddress(vnetconfig, src, currdst)) != 0) {
            EUCA_FREE(currdst);
            return (EUCA_ERROR);
        }
    }
    // not used anymore
    EUCA_FREE(currdst);

    // do the (re)assign
    if (!dst || !strcmp(dst, "0.0.0.0")) {
        vnetconfig->publicips[pubidx].dstip = 0;
        vnetconfig->publicips[pubidx].allocated = 0;
    } else {
        if ((rc = vnetAssignAddress(vnetconfig, src, dst)) != 0) {
            return (EUCA_ERROR);
        }
        vnetconfig->publicips[pubidx].dstip = dot2hex(dst);
        vnetconfig->publicips[pubidx].allocated = 1;
    }

    if (uuid) {
        snprintf(vnetconfig->publicips[pubidx].uuid, 48, "%s", uuid);
    }
    LOGDEBUG("successfully set src=%s to dst=%s with uuid=%s, allocated=%d\n", SP(src), SP(dst), SP(uuid), vnetconfig->publicips[pubidx].allocated);
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] src
//! @param[in] dst
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if we fail to unassign the IP address.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre \p vnetconfig, \p src and \p dst must not be NULL.
//!
int vnetUnassignAddress(vnetConfig * vnetconfig, char *src, char *dst)
{
    int rc = 0;
    int count = 0;
    int slashnet = 0;
    int ret = EUCA_OK;
    char cmd[EUCA_MAX_PATH] = "";
    char *network = NULL;

    if ((vnetconfig == NULL) || (src == NULL) || (dst == NULL)) {
        LOGERROR("bad input params: vnetconfig=%p, src=%s, dst=%s\n", vnetconfig, SP(src), SP(dst));
        return (EUCA_INVALID_ERROR);
    }

    if (((vnetconfig->role == CC) || (vnetconfig->role == CLC)) && (!strcmp(vnetconfig->mode, NETMODE_MANAGED) || !strcmp(vnetconfig->mode, NETMODE_MANAGED_NOVLAN))) {
        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " ip addr del %s/32 dev %s", vnetconfig->eucahome, src, vnetconfig->pubInterface);
        LOGDEBUG("running cmd %s\n", cmd);
        rc = system(cmd);
        rc = rc >> 8;
        if (rc && (rc != 2)) {
            LOGERROR("failed to assign IP address '%s'\n", cmd);
            ret = EUCA_ERROR;
        }
        // If a rule cannot be removed, the assumption is it's not present,
        // so failure to remove it will not be treated as a fatal error.
        // (Fixes EUCA-7945.)
        snprintf(cmd, EUCA_MAX_PATH, "-D PREROUTING -d %s -j DNAT --to-destination %s", src, dst);
        rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
        count = 0;
        while (rc != 0 && count < 10) {
            rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
            count++;
        }

        if (rc) {
            LOGERROR("failed to remove DNAT rule '%s'\n", cmd);
            //            ret = EUCA_ERROR;
        }

        snprintf(cmd, EUCA_MAX_PATH, "-D OUTPUT -d %s -j DNAT --to-destination %s", src, dst);
        rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
        count = 0;
        while (rc != 0 && count < 10) {
            rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
            count++;
        }

        if (rc) {
            LOGERROR("failed to remove DNAT rule '%s'\n", cmd);
            //            ret = EUCA_ERROR;
        }

        slashnet = 32 - ((int)log2((double)(0xFFFFFFFF - vnetconfig->nm)) + 1);
        network = hex2dot(vnetconfig->nw);
        snprintf(cmd, EUCA_MAX_PATH, "-D POSTROUTING -s %s -j SNAT --to-source %s", dst, src);
        EUCA_FREE(network);
        rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
        count = 0;
        while (rc != 0 && count < 10) {
            rc = vnetApplySingleTableRule(vnetconfig, "nat", cmd);
            count++;
        }

        if (rc) {
            LOGERROR("failed to remove SNAT rule '%s'\n", cmd);
            //            ret = EUCA_ERROR;
        }
        // For reporting traffic statistics.
        //
        // If a rule cannot be removed, the assumption is it's not present,
        // so failure to remove it will not be treated as a fatal error.
        // (Fixes EUCA-7163.)
        snprintf(cmd, EUCA_MAX_PATH, "-D EUCA_COUNTERS_IN -d %s", dst);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
            LOGWARN("vnetUnassignAddress(): cannot remove EUCA_COUNTERS_IN rule '%s'\n", cmd);
            // ret = EUCA_ERROR;
        }

        snprintf(cmd, EUCA_MAX_PATH, "-D EUCA_COUNTERS_OUT -s %s", dst);
        if ((rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd)) != 0) {
            LOGWARN("vnetUnassignAddress(): cannot remove EUCA_COUNTERS_OUT rule '%s'\n", cmd);
            // ret = EUCA_ERROR;
        }
    }
    return (ret);
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] vlan
//! @param[in] userName
//! @param[in] netName
//!
//! @return EUCA_OK if the virtual network configuration mode is set for SYSTEM, STATIC or EDGE. If the mode
//!         is set to anything else, then the result of vnetStopNetworkManaged() is returned.
//!
//! @see vnetStopNetworkManaged()
//!
int vnetStopNetwork(vnetConfig * vnetconfig, int vlan, char *userName, char *netName)
{
    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM) || !strcmp(vnetconfig->mode, NETMODE_STATIC) || !strcmp(vnetconfig->mode, NETMODE_EDGE)) {
        return (EUCA_OK);
    }
    return (vnetStopNetworkManaged(vnetconfig, vlan, userName, netName));
}

//!
//!
//!
//! @param[in]  vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in]  instId
//! @param[out] outmac
//!
//! @return EUCA_OK on success and the \p outmac field is set properly or the following error code:
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre \li \p vnetconfig, \p instId and \p outmac must not be NULL.
//!      \li \p instId must be a valid intance identifier string starting with "i-" followed by 8 digits
//!      \li \p outmac must be a buffer capable of holding a minimum of 24 characters
//!
int instId2mac(vnetConfig * vnetconfig, char *instId, char *outmac)
{
    char *p = NULL;
    char dst[24] = "";
    int i = 0;

    if (!vnetconfig || !instId || !outmac) {
        return (EUCA_INVALID_ERROR);
    }
    dst[0] = '\0';

    if ((p = strstr(instId, "i-")) == NULL) {
        LOGWARN("invalid instId=%s\n", SP(instId));
        return (EUCA_INVALID_ERROR);
    }

    p += 2;
    if (strlen(p) == 8) {
        strncat(dst, vnetconfig->macPrefix, 5);
        for (i = 0; i < 4; i++) {
            strncat(dst, ":", 1);
            strncat(dst, p, 2);
            p += 2;
        }
    } else {
        LOGWARN("invalid instId=%s\n", SP(instId));
        return (EUCA_INVALID_ERROR);
    }

    snprintf(outmac, 24, "%s", dst);
    return (EUCA_OK);
}

//!
//!
//!
//! @param[in]  vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in]  ip
//! @param[out] mac
//!
//! @return EUCA_OK on success and \p mac is set properly or the following error code:
//!         \li EUCA_ACCESS_ERROR: if we cannot open the "/proc/net/arp" file for reading
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if the corresponding IP isn't found in the ARP table of the system
//!
//! @pre \p ip and \p mac must not be NULL.
//!
//! @note The \p (*mac) field should be NULL already and the caller is responsible to free the allocated memory for \p mac.
//!
int ip2mac(vnetConfig * vnetconfig, char *ip, char **mac)
{
    int count = 0;
    char rbuf[256] = "";
    char *tok = NULL;
    char ipspace[25] = "";
    FILE *FH = NULL;

    if ((mac == NULL) || (ip == NULL)) {
        return (EUCA_INVALID_ERROR);
    }

    *mac = NULL;

    if ((FH = fopen("/proc/net/arp", "r")) == NULL) {
        return (EUCA_ACCESS_ERROR);
    }

    snprintf(ipspace, 25, "%s ", ip);
    while (fgets(rbuf, 256, FH) != NULL) {
        if (strstr(rbuf, ipspace)) {
            tok = strtok(rbuf, " ");
            count = 0;
            while (tok && count < 4) {
                count++;
                if (count < 4) {
                    tok = strtok(NULL, " ");
                }
            }

            if (tok != NULL) {
                *mac = strdup(tok);
                fclose(FH);
                return (EUCA_OK);
            }
        }
    }

    fclose(FH);
    return (EUCA_NOT_FOUND_ERROR);
}

//!
//!
//!
//! @param[in]  vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in]  mac
//! @param[out] ip
//!
//! @return EUCA_OK on success and \p mac is set properly or the following error code:
//!         \li EUCA_ACCESS_ERROR: if we cannot open the "/proc/net/arp" file for reading
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if the corresponding MAC isn't found in the ARP table of the system
//!
//! @pre \p vnetconfig, \p ip and \p mac must not be NULL.
//!
//! @note The \p (*ip) field should be set to NULL already and the caller is responsible to free the allocated memory for \p ip.
//!
int mac2ip(vnetConfig * vnetconfig, char *mac, char **ip)
{
    int rc = 0;
    int i = 0;
    char *tok = NULL;
    char rbuf[256] = "";
    char lowbuf[256] = "";
    char lowmac[256] = "";
    char cmd[EUCA_MAX_PATH] = "";
    FILE *FH = NULL;

    if ((vnetconfig == NULL) || (mac == NULL) || (ip == NULL)) {
        return (EUCA_INVALID_ERROR);
    }

    *ip = NULL;
    if (!strcmp(vnetconfig->mode, NETMODE_SYSTEM)) {
        // try to fill up the arp cache
        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " " EUCALYPTUS_HELPER_DIR "/populate_arp.pl", vnetconfig->eucahome, vnetconfig->eucahome);
        if ((rc = system(cmd)) != 0) {
            LOGWARN("could not execute arp cache populator script, check httpd log for errors\n");
        }
    }

    if ((FH = fopen("/proc/net/arp", "r")) == NULL) {
        return (EUCA_ACCESS_ERROR);
    }

    bzero(lowmac, 256);
    for (i = 0; i < strlen(mac); i++) {
        lowmac[i] = tolower(mac[i]);
    }

    while (fgets(rbuf, 256, FH) != NULL) {
        bzero(lowbuf, 256);
        for (i = 0; i < strlen(rbuf); i++) {
            lowbuf[i] = tolower(rbuf[i]);
        }

        if (strstr(lowbuf, lowmac)) {
            tok = strtok(lowbuf, " ");
            if (tok != NULL) {
                *ip = strdup(tok);
                fclose(FH);
                return (EUCA_OK);
            }
        }
    }

    fclose(FH);
    return (EUCA_NOT_FOUND_ERROR);
}

//!
//! Retrieves a given device information (assigned IPs and NMS).
//!
//! @param[in]  dev
//! @param[out] outips
//! @param[out] outnms
//! @param[out] len
//!
//! @return EUCA_OK on success and the out fields will be set properly. On failure the
//!         following error codes are returned:
//!         \li EUCA_ERROR: if we fail to retrieve the interfaces addresses.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre \p dev, \p outips, \p outnms and \p len must not be NULL.
//!
int getdevinfo(char *dev, u32 ** outips, u32 ** outnms, int *len)
{
    int rc = 0;
    int count = 0;
    char host[NI_MAXHOST] = "";
    char buf[32] = "";
    void *tmpAddrPtr = NULL;
    struct ifaddrs *ifaddr = NULL;
    struct ifaddrs *ifa = NULL;
    struct sockaddr_in *ifs = NULL;

    if ((dev == NULL) || (outips == NULL) || (outnms == NULL) || (len == NULL))
        return (EUCA_INVALID_ERROR);

    if ((rc = getifaddrs(&ifaddr)) != 0) {
        return (EUCA_ERROR);
    }

    *outips = *outnms = NULL;
    *len = 0;

    count = 0;
    for (ifa = ifaddr; ifa != NULL; ifa = ifa->ifa_next) {
        if (!strcmp(dev, "all") || !strcmp(ifa->ifa_name, dev)) {
            if (ifa->ifa_addr && ifa->ifa_addr->sa_family == AF_INET) {
                if ((rc = getnameinfo(ifa->ifa_addr, sizeof(struct sockaddr_in), host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST)) == 0) {
                    count++;

                    //! @todo handle graceful out of memory condition and report it
                    *outips = EUCA_REALLOC(*outips, count, sizeof(u32));
                    *outnms = EUCA_REALLOC(*outnms, count, sizeof(u32));

                    (*outips)[count - 1] = dot2hex(host);

                    ifs = ((struct sockaddr_in *)ifa->ifa_netmask);
                    tmpAddrPtr = &ifs->sin_addr;
                    if (inet_ntop(AF_INET, tmpAddrPtr, buf, 32)) {
                        (*outnms)[count - 1] = dot2hex(buf);
                    }
                }
            }
        }
    }

    freeifaddrs(ifaddr);
    *len = count;
    return (EUCA_OK);
}

//!
//! Converts a readable IP address to a readable MAC format value using a prefix.
//!
//! @param[in] ip the IP address to convert. If NULL, then "127.0.0.1" is assumed.
//! @param[in] macprefix the MAC address prefix to use ("xx:xx"). If NULL then "D0:0D" is assumed.
//!
//! @return The converted value if all is valid or NULL if any problem occured.
//!
//! @note The caller is responsible to free the allocated memory for the returned value. If the
//!       provided IP address is no valid or if \p ip is NULL, then it is assumed the IP to be
//!       "127.0.0.1".
//!
char *ipdot2macdot(char *ip, char *macprefix)
{
    int a = 0;
    int b = 0;
    int c = 0;
    int d = 0;
    int rc = 0;
    char *ret = NULL;

    if (ip != NULL) {
        rc = sscanf(ip, "%d.%d.%d.%d", &a, &b, &c, &d);
        if ((rc != 4) || ((a < 0) || (a > 255)) || ((b < 0) || (b > 255)) || ((c < 0) || (c > 255)) || ((d < 0) || (d > 255))) {
            a = 127;
            b = 0;
            c = 0;
            d = 1;
        }
    }

    if ((ret = EUCA_ZALLOC(24, sizeof(char))) != NULL) {
        if (macprefix) {
            snprintf(ret, 24, "%s:%02X:%02X:%02X:%02X", macprefix, a, b, c, d);
        } else {
            snprintf(ret, 24, "%s:%02X:%02X:%02X:%02X", "D0:0D", a, b, c, d);
        }
    }
    return (ret);
}

//!
//! Loads the IP tables from the "[path]/iptables-preload" file
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//!
//! @return EUCA_OK on success or the following error codes:
//!         \li EUCA_ERROR: if any issue occured loading the tables.
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!
//! @pre \p vnetconfig must not be NULL.
//!
//! @note
//!
int vnetLoadIPTables(vnetConfig * vnetconfig)
{
    int rc = 0;
    int ret = EUCA_OK;
    char cmd[EUCA_MAX_PATH] = "";
    char oldfile[EUCA_MAX_PATH] = "";
    char newpath[EUCA_MAX_PATH] = "";
    struct stat statbuf = { 0 };

    // Check if the preload file exists under the Eucalyptus home directory
    snprintf(newpath, EUCA_MAX_PATH, EUCALYPTUS_CONF_DIR "/%s", vnetconfig->eucahome, "iptables-preload");
    if (stat(newpath, &statbuf) != 0) {
        // The file isn't under Eucalyptus Home path, perhaps under the running path?
        snprintf(oldfile, EUCA_MAX_PATH, "%s/iptables-preload", vnetconfig->path);
        if (stat(oldfile, &statbuf) == 0) {
            // The file is present in the var/run path, copy it to the Eucalyptus home directory
            snprintf(cmd, EUCA_MAX_PATH, "cp  %s %s", oldfile, newpath);
            rc = system(cmd);
            ret = WEXITSTATUS(rc);

            // Were we able to copy?
            if (stat(newpath, &statbuf) != 0) {
                // Log the failure, restore IP tables using the old file path and bail!
                LOGINFO("copied %s to %s (err %x) failed\n", oldfile, newpath, rc);
                snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " iptables-restore < %s", vnetconfig->eucahome, oldfile);
                rc = system(cmd);
                if ((ret = WEXITSTATUS(rc)) != 0) {
                    LOGDEBUG("%s returned %x\n", cmd, rc);
                }
                return (((ret == 0) ? EUCA_OK : EUCA_ERROR));
            }
        }
    }
    // At this point, we may or not have a preload file. Lets check again. If we have one,
    // load it and save the result to memory
    if (stat(newpath, &statbuf) == 0) {
        snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " iptables-restore < %s", vnetconfig->eucahome, newpath);
        rc = system(cmd);
        if ((ret = WEXITSTATUS(rc)) != 0) {
            LOGDEBUG("%s returned %x\n", cmd, rc);
        }
    }

    return (((ret == 0) ? EUCA_OK : EUCA_ERROR));
}

//!
//!
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] userName
//! @param[in] netName
//!
//! @return The result of the vnetApplySingleTableRule() call or the following error codes:
//!         \li EUCA_ERROR: if we fail to hash the user/net strings
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @see vnetApplySingleTableRule()
//!
//! @pre \p vnetconfig, \p userName and \p netName must not be NULL.
//!
int check_chain(vnetConfig * vnetconfig, char *userName, char *netName)
{
    int rc = EUCA_OK;
    char *hashChain = NULL;
    char cmd[EUCA_MAX_PATH] = "";
    char userNetString[EUCA_MAX_PATH] = "";

    if ((vnetconfig == NULL) || (userName == NULL) || (netName == NULL)) {
        LOGERROR("bad input params: vnetconfig=%p, userName=%p, netName=%p\n", vnetconfig, userName, netName);
        return (EUCA_INVALID_ERROR);
    }

    snprintf(userNetString, EUCA_MAX_PATH, "%s%s", userName, netName);
    if ((rc = hash_b64enc_string(userNetString, &hashChain)) != 0) {
        LOGERROR("cannot hash user/net string (userNetString=%s)\n", userNetString);
        return (EUCA_ERROR);
    }

    snprintf(cmd, EUCA_MAX_PATH, "-L %s -n", hashChain);
    rc = vnetApplySingleTableRule(vnetconfig, "filter", cmd);

    EUCA_FREE(hashChain);
    return (rc);
}

//!
//! Checks wether or not a device is enabled.
//!
//! @param[in] dev the device name to validate
//!
//! @return EUCA_OK if the device is UP otherwize error codes from check_device() can be
//!         returned as well we the following error codes:
//!         \li EUCA_IO_ERROR: if we cannot confirm the device is UP or if the device is down.
//!         \li EUCA_ACCESS_ERROR: if we cannot open the device operstate file for reading
//!
//! @pre The \p dev fiels must not be NULL.
//!
//! @note Only the EUCA_OK is deterministic about the device being up. Any other results
//!       returned is not 100% deterministic of the device status.
//!
int check_deviceup(char *dev)
{
    int ret = 0;
    char *p = NULL;
    char rbuf[EUCA_MAX_PATH] = "";
    FILE *FH = NULL;

    if ((ret = check_device(dev)) != EUCA_OK) {
        return (ret);
    }

    snprintf(rbuf, EUCA_MAX_PATH, "/sys/class/net/%s/operstate", dev);
    if ((FH = fopen(rbuf, "r")) == NULL) {
        return (EUCA_ACCESS_ERROR);
    }

    ret = EUCA_IO_ERROR;
    bzero(rbuf, EUCA_MAX_PATH);
    if (fgets(rbuf, EUCA_MAX_PATH, FH)) {
        if ((p = strchr(rbuf, '\n')) != NULL)
            *p = '\0';

        //! @fixme we should look for up to be deterministic???
        if (strncmp(rbuf, "down", EUCA_MAX_PATH)) {
            ret = EUCA_OK;
        }
    }

    fclose(FH);
    return (ret);
}

//!
//! Checks wether or not a device is a valid device on this system.
//!
//! @param[in] dev the device name to validate.
//!
//! @return EUCA_OK if the device is valid otherwise any error code indicates the
//!         device is invalid. Error code returned are:
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if the device does not exists on this system
//!
//! @pre The \p dev field must not be NULL.
//!
int check_device(char *dev)
{
    char file[EUCA_MAX_PATH] = "";

    if (!dev) {
        LOGERROR("bad input params: dev=%s\n", SP(dev));
        return (EUCA_INVALID_ERROR);
    }

    snprintf(file, EUCA_MAX_PATH, "/sys/class/net/%s/", dev);
    if (check_directory(file)) {
        return (EUCA_NOT_FOUND_ERROR);
    }
    return (EUCA_OK);
}

//!
//! Checks wehter or not a bridge device is running Spanning Tree Protocol (STP).
//!
//! @param[in] br the bridge device name
//!
//! @return EUCA_OK if the device is running STP or any error code otherwise. The following
//!         error codes are returned:
//!         \li EUCA_ERROR: if the device isn't running STP
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!
//! @pre The \p br field must not be NULL and must be a valid bridge device.
//!
int check_bridgestp(char *br)
{
    int ret = EUCA_ERROR;
    char *buf = NULL;
    char file[EUCA_MAX_PATH] = "";

    if (!br || check_bridge(br)) {
        LOGERROR("bad input params: br=%s\n", SP(br));
        return (EUCA_INVALID_ERROR);
    }

    snprintf(file, EUCA_MAX_PATH, "/sys/class/net/%s/bridge/stp_state", br);
    if ((buf = file2str(file)) != NULL) {
        if (atoi(buf) != 0) {
            ret = EUCA_OK;
        }
        EUCA_FREE(buf);
    }
    return (ret);
}

//!
//! Checks wether or not a given device is part of the given bridge.
//!
//! @param[in] br the bridge device name
//! @param[in] dev the device name to check
//!
//! @return EUCA_OK if the device is part of the bridge otherwise an error code
//!         is returned. The following error codes are returned:
//!         \li EUCA_ERROR: if the device is not part of the bridge
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the precondition
//!
//! @pre Both \p br and \p dev must not be NULL and both must be valid devices.
//!
int check_bridgedev(char *br, char *dev)
{
    char file[EUCA_MAX_PATH] = "";

    if (!br || !dev || check_device(br) || check_device(dev)) {
        LOGERROR("bad input params: br=%s, dev=%s\n", SP(br), SP(dev));
        return (EUCA_INVALID_ERROR);
    }

    snprintf(file, EUCA_MAX_PATH, "/sys/class/net/%s/brif/%s/", br, dev);
    if (check_directory(file)) {
        return (EUCA_ERROR);
    }
    return (EUCA_OK);
}

//!
//! Checks wether or not this device is a bridge device.
//!
//! @param[in] brname the bridge device name
//!
//! @return EUCA_OK if this is a valid bridge device or any error to indicate this is not a
//!         valid bridge device. The following error codes are returned:
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions
//!         \li EUCA_NOT_FOUND_ERROR: if the device is not a bridge device
//!
//! @pre The \p brname field must not be NULL and must be a valid device.
//!
int check_bridge(char *brname)
{
    char file[EUCA_MAX_PATH] = "";

    if (!brname) {
        LOGERROR("bad input params: brname=%s\n", SP(brname));
        return (EUCA_INVALID_ERROR);
    }

    if (check_device(brname)) {
        return (EUCA_NOT_FOUND_ERROR);
    }

    snprintf(file, EUCA_MAX_PATH, "/sys/class/net/%s/bridge/", brname);
    if (check_directory(file)) {
        return (EUCA_NOT_FOUND_ERROR);
    }
    return (EUCA_OK);
}

//!
//! Checks that a given rule is part of a table.
//!
//! @param[in] vnetconfig a pointer to the Virtual Network Configuration information structure
//! @param[in] table the table name to check against
//! @param[in] rule the rule to validate
//!
//! @return EUCA_OK if the table and rule are valid or the following error codes:
//!         \li EUCA_ERROR: if the rule is not part of the table
//!         \li EUCA_IO_ERROR: if we fail to retrieve the iptable information
//!         \li EUCA_INVALID_ERROR: if any parameter does not meet the preconditions.
//!
//! @pre The \p vnetconfig, \p table and \p rule fields must not be NULL.
//!
//! @note Only EUCA_OK and EUCA_ERROR are deterministic in confirming the given rule is
//!       or is not part of the table. Any other error returned only indicate an error
//!       occured with the system or request therefore being non deterministic in confirming
//!       this request.
//!
int check_tablerule(vnetConfig * vnetconfig, char *table, char *rule)
{
    char *out = NULL;
    char *ptr = NULL;
    char cmd[EUCA_MAX_PATH] = "";

    if (!vnetconfig || !table || !rule) {
        LOGERROR("bad input params: vnetconfig=%p, table=%s, rule=%s\n", vnetconfig, SP(table), SP(rule));
        return (EUCA_INVALID_ERROR);
    }

    snprintf(cmd, EUCA_MAX_PATH, EUCALYPTUS_ROOTWRAP " iptables -S -t %s", vnetconfig->eucahome, table);
    if ((out = system_output(cmd)) == NULL) {
        return (EUCA_IO_ERROR);
    }

    ptr = strstr(out, rule);
    EUCA_FREE(out);
    if (!ptr) {
        return (EUCA_ERROR);
    }
    return (EUCA_OK);
}

//!
//! Checks wether or not a given IP is a valid readable IP format.
//!
//! @param[in] ip the IP field to validate
//!
//! @return EUCA_OK if this is a valid IP field or EUCA_INVALID_ERROR if the \p ip field is NULL
//!         or invalid.
//!
int check_isip(char *ip)
{
    int a = 0;
    int b = 0;
    int c = 0;
    int d = 0;
    int rc = 0;

    if (!ip) {
        LOGERROR("bad input params: ip=%s\n", SP(ip));
        return (EUCA_INVALID_ERROR);
    }

    rc = sscanf(ip, "%d.%d.%d.%d", &a, &b, &c, &d);
    if ((rc != 4) || ((a < 0) || (a > 255)) || ((b < 0) || (b > 255)) || ((c < 0) || (c > 255)) || ((d < 0) || (d > 255))) {
        return (EUCA_INVALID_ERROR);
    }
    return (EUCA_OK);
}

//!
//! Converts a given host name to a matching IP address.
//!
//! @param[in] host the hostname that we are looking for
//!
//! @return NULL if not found or invalid otherwise returns the IP string related to the hostname.
//!
//! @pre \p host must not be NULL and must be a valid hostname or IP address.
//!
//! @note The caller is responsible to free the memory for the returned value
//!
char *host2ip(char *host)
{
    int rc = 0;
    char *ret = NULL;
    char hostbuf[256] = "";
    struct addrinfo hints = { 0 };
    struct addrinfo *result = NULL;

    if (!host) {
        LOGERROR("bad input params: host=%s\n", SP(host));
        return (NULL);
    }

    if (!strcmp(host, "localhost")) {
        ret = strdup("127.0.0.1");
        return (ret);
    }

    bzero(&hints, sizeof(struct addrinfo));
    if ((rc = getaddrinfo(host, NULL, &hints, &result)) == 0) {
        // Ok we know about this host
        rc = getnameinfo(result->ai_addr, result->ai_addrlen, hostbuf, 256, NULL, 0, NI_NUMERICHOST);
        if (!rc && !check_isip(hostbuf)) {
            ret = strdup(hostbuf);
        }
    }

    if (result)
        freeaddrinfo(result);

    if (!ret) {
        //! @fixme thinking of this logic, if the hostname is not an IP and
        //!        we don't know about it we just returned a non-IP????
        ret = strdup(host);
    }
    return (ret);
}

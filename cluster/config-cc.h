// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

#ifndef INCLUDE_CONFIG_CC_H
#define INCLUDE_CONFIG_CC_H

#include "stats.h"

configEntry configKeysRestartCC[] = {
    {"DISABLE_TUNNELING", "N"}
    ,
    {"ENABLE_WS_SECURITY", "Y"}
    ,
    {"EUCALYPTUS", "/"}
    ,
    {"NC_FANOUT", "1"}
    ,
    {"NC_PORT", "8775"}
    ,
    {"NC_SERVICE", "axis2/services/EucalyptusNC"}
    ,
    {"SCHEDPOLICY", "ROUNDROBIN"}
    ,
    {"VNET_ADDRSPERNET", NULL}
    ,
    {"VNET_BRIDGE", NULL}
    ,
    {"VNET_BROADCAST", NULL}
    ,
    {"VNET_DHCPDAEMON", "/usr/sbin/dhcpd3"}
    ,
    {"VNET_DHCPUSER", "dhcpd"}
    ,
    {"VNET_DNS", NULL}
    ,
    {"VNET_DOMAINNAME", "eucalyptus.internal"}
    ,
    {"VNET_LOCALIP", NULL}
    ,
    {"VNET_MACMAP", NULL}
    ,
    {"VNET_MODE", "SYSTEM"}
    ,
    {"VNET_NETMASK", NULL}
    ,
    {"VNET_PRIVINTERFACE", "eth0"}
    ,
    {"VNET_PUBINTERFACE", "eth0"}
    ,
    {"VNET_PUBLICIPS", NULL}
    ,
    {"VNET_PRIVATEIPS", NULL}
    ,
    {"VNET_ROUTER", NULL}
    ,
    {"VNET_SUBNET", NULL}
    ,
    {"VNET_MACPREFIX", "d0:0d"}
    ,
    {"POWER_IDLETHRESH", "300"}
    ,
    {"POWER_WAKETHRESH", "300"}
    ,
    {"CC_IMAGE_PROXY", NULL}
    ,
    {"CC_IMAGE_PROXY_CACHE_SIZE", "32768"}
    ,
    {"CC_IMAGE_PROXY_PATH", NULL}
    ,
    {NULL, NULL}
    ,
};

configEntry configKeysNoRestartCC[] = {
    {"NODES", NULL}
    ,
    {"NC_POLLING_FREQUENCY", "6"}
    ,
    {"CLC_POLLING_FREQUENCY", "6"}
    ,
    {"CC_ARBITRATORS", NULL}
    ,
    {"LOGLEVEL", "INFO"}
    ,
    {"LOGROLLNUMBER", "10"}
    ,
    {"LOGMAXSIZE", "104857600"}
    ,
    {"LOGPREFIX", ""}
    ,
    {"LOGFACILITY", ""}
    ,
    {SENSOR_LIST_CONF_PARAM_NAME, SENSOR_LIST_CONF_PARAM_DEFAULT}
    ,
    {NULL, NULL}
    ,
};

#endif

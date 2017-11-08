// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

#ifndef INCLUDE_CONFIG_CC_H
#define INCLUDE_CONFIG_CC_H

#include "stats.h"

configEntry configKeysRestartCC[] = {
    {"ENABLE_WS_SECURITY", "Y"}
    ,
    {"EUCALYPTUS", "/"}
    ,
    {"NC_FANOUT", "1"}
    ,
    {"NC_PORT", "8775"}
    ,
    {"SCHEDPOLICY", "ROUNDROBIN"}
    ,
    {"VNET_BRIDGE", NULL}
    ,
    {"VNET_MODE", NETMODE_INVALID}
    ,
    {"VNET_PUBINTERFACE", "eth0"}
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
    {"MAX_INSTANCES_PER_CC", NULL}
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
    {"INSTANCE_TIMEOUT", NULL}
    ,
    {SENSOR_LIST_CONF_PARAM_NAME, SENSOR_LIST_CONF_PARAM_DEFAULT}
    ,
    {NULL, NULL}
    ,
};

#endif

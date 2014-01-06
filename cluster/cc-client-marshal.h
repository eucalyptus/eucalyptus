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

#ifndef _INCLUDE_CC_CLIENT_MARSHAL_H_
#define _INCLUDE_CC_CLIENT_MARSHAL_H_

//!
//! @file cluster/cc-client-marshal.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <time.h>
#include <misc.h>
#include <data.h>
#include <sensor.h>
#include "axis2_stub_EucalyptusCC.h"

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
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int cc_killallInstances(axutil_env_t * env, axis2_stub_t * pStub);
int cc_getConsoleOutput(char *instId, axutil_env_t * env, axis2_stub_t * pStub);
int cc_rebootInstances(char **instIds, int instIdsLen, axutil_env_t * env, axis2_stub_t * pStub);
int cc_terminateInstances(char **instIds, int instIdsLen, axutil_env_t * env, axis2_stub_t * pStub);
int cc_configureNetwork(char *sourceNet, char *destName, char *protocol, int min, int max, char *type, axutil_env_t * env, axis2_stub_t * pStub);
int cc_stopNetwork(int vlan, char *netName, axutil_env_t * env, axis2_stub_t * pStub);
int cc_attachVolume(char *volumeId, char *instanceId, char *remoteDev, char *localDev, axutil_env_t * env, axis2_stub_t * pStub);
int cc_detachVolume(char *volumeId, char *instanceId, char *remoteDev, char *localDev, int force, axutil_env_t * env, axis2_stub_t * pStub);
int cc_createImage(char *volumeId, char *instanceId, char *remoteDev, axutil_env_t * env, axis2_stub_t * pStub);
int cc_bundleInstance(char *instanceId, char *bucketName, char *filePrefix, char *objectStorageURL, char *userPublicKey, axutil_env_t * env, axis2_stub_t * pStub);
int cc_bundleRestartInstance(char *instanceId, axutil_env_t * env, axis2_stub_t * pStub);
int cc_broadcastNetworkInfo(char *networkInfo, axutil_env_t * env, axis2_stub_t * pStub);
int cc_assignAddress(char *src, char *dst, axutil_env_t * env, axis2_stub_t * pStub);
int cc_unassignAddress(char *src, char *dst, axutil_env_t * env, axis2_stub_t * pStub);
int cc_describePublicAddresses(axutil_env_t * env, axis2_stub_t * pStub);
int cc_startNetwork(int vlan, char *netName, char **ccs, int ccsLen, axutil_env_t * env, axis2_stub_t * pStub);
int cc_describeNetworks(char *nameserver, char **ccs, int ccsLen, axutil_env_t * env, axis2_stub_t * pStub);
int cc_describeResources(axutil_env_t * env, axis2_stub_t * pStub);
int cc_describeInstances(char **instIds, int instIdsLen, axutil_env_t * env, axis2_stub_t * pStub);
int cc_runInstances(char *amiId, char *amiURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL, int num, int vlan, char *netName,
                    virtualMachine * vm_type, axutil_env_t * env, axis2_stub_t * pStub);
#if 0
int cc_registerImage(char *imageloc, axutil_env_t * env, axis2_stub_t * pStub);
#endif /* 0 */
int cc_describeSensors(int historySize, long long collectionIntervalTimeMs, char **instIds, int instIdsLen, char **sensorIds, int sensorIdsLen,
                       sensorResource *** outResources, int *outResourcesLen, axutil_env_t * env, axis2_stub_t * pStub);
int cc_describeServices(axutil_env_t * env, axis2_stub_t * pStub);
int cc_startService(axutil_env_t * env, axis2_stub_t * pStub);
int cc_stopService(axutil_env_t * env, axis2_stub_t * pStub);
int cc_enableService(axutil_env_t * env, axis2_stub_t * pStub);
int cc_disableService(axutil_env_t * env, axis2_stub_t * pStub);
int cc_shutdownService(axutil_env_t * env, axis2_stub_t * pStub);
int cc_modifyNode(char *nodeName, char *stateName, axutil_env_t * env, axis2_stub_t * pStub);
int cc_migrateInstances(char *nodeName, axutil_env_t * env, axis2_stub_t * pStub);
int cc_startInstance(char *instanceId, axutil_env_t * env, axis2_stub_t * pStub);
int cc_stopInstance(char *instanceId, axutil_env_t * env, axis2_stub_t * pStub);

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

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_CC_CLIENT_MARSHAL_H_ */

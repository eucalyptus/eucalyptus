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
//! @file util/adb-service-helpers.h
//! Need to provide description
//!

#ifndef _INCLUDE_ADB_SERVICE_HELPERS_H_
#define _INCLUDE_ADB_SERVICE_HELPERS_H_

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

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

//! Macro to unmarshal a message from the server
#define EUCA_MESSAGE_UNMARSHAL(_thefunc, _theadb, _themeta)                                                             \
{                                                                                                                       \
	int i = 0;                                                                                                          \
	int j = 0;                                                                                                          \
	adb_serviceInfoType_t *sit = NULL;                                                                                  \
	bzero((_themeta), sizeof(ncMetadata));                                                                              \
	(_themeta)->correlationId = adb_##_thefunc##_get_correlationId((_theadb), env);                                     \
	(_themeta)->userId = adb_##_thefunc##_get_userId((_theadb), env);                                                   \
	(_themeta)->epoch = adb_##_thefunc##_get_epoch((_theadb), env);                                                     \
	(_themeta)->servicesLen = adb_##_thefunc##_sizeof_services((_theadb), env);                                         \
	for (i = 0; ((i < (_themeta)->servicesLen) && (i < 16)); i++) {                                                     \
		sit = adb_##_thefunc##_get_services_at((_theadb), env, i);                                                      \
		snprintf((_themeta)->services[i].type, 32, "%s", adb_serviceInfoType_get_type(sit, env));                       \
		snprintf((_themeta)->services[i].name, 32, "%s", adb_serviceInfoType_get_name(sit, env));                       \
		snprintf((_themeta)->services[i].partition, 32, "%s", adb_serviceInfoType_get_partition(sit, env));             \
		(_themeta)->services[i].urisLen = adb_serviceInfoType_sizeof_uris(sit, env);                                    \
		for (j = 0; ((j < (_themeta)->services[i].urisLen) && (j < 8)); j++) {                                          \
			snprintf((_themeta)->services[i].uris[j], 512, "%s", adb_serviceInfoType_get_uris_at(sit, env, j));         \
		}                                                                                                               \
	}                                                                                                                   \
	(_themeta)->disabledServicesLen = adb_##_thefunc##_sizeof_disabledServices((_theadb), env);                         \
	for (i = 0; ((i < (_themeta)->disabledServicesLen) && (i < 16)); i++) {                                             \
		sit = adb_##_thefunc##_get_disabledServices_at((_theadb), env, i);                                              \
		snprintf((_themeta)->disabledServices[i].type, 32, "%s", adb_serviceInfoType_get_type(sit, env));               \
		snprintf((_themeta)->disabledServices[i].name, 32, "%s", adb_serviceInfoType_get_name(sit, env));               \
		snprintf((_themeta)->disabledServices[i].partition, 32, "%s", adb_serviceInfoType_get_partition(sit, env));     \
		(_themeta)->disabledServices[i].urisLen = adb_serviceInfoType_sizeof_uris(sit, env);                            \
		for (j = 0; ((j < (_themeta)->disabledServices[i].urisLen) && (j < 8)); j++) {                                  \
			snprintf((_themeta)->disabledServices[i].uris[j], 512, "%s", adb_serviceInfoType_get_uris_at(sit, env, j)); \
		}                                                                                                               \
	}                                                                                                                   \
	(_themeta)->notreadyServicesLen = adb_##_thefunc##_sizeof_notreadyServices((_theadb), env);                         \
	for (i = 0; ((i < (_themeta)->notreadyServicesLen) && (i < 16)); i++) {                                             \
		sit = adb_##_thefunc##_get_notreadyServices_at((_theadb), env, i);                                              \
		snprintf((_themeta)->notreadyServices[i].type, 32, "%s", adb_serviceInfoType_get_type(sit, env));               \
		snprintf((_themeta)->notreadyServices[i].name, 32, "%s", adb_serviceInfoType_get_name(sit, env));               \
		snprintf((_themeta)->notreadyServices[i].partition, 32, "%s", adb_serviceInfoType_get_partition(sit, env));     \
		(_themeta)->notreadyServices[i].urisLen = adb_serviceInfoType_sizeof_uris(sit, env);                            \
		for (j = 0; ((j < (_themeta)->notreadyServices[i].urisLen) && (j < 8)); j++) {                                  \
			snprintf((_themeta)->notreadyServices[i].uris[j], 512, "%s", adb_serviceInfoType_get_uris_at(sit, env, j)); \
		}                                                                                                               \
	}                                                                                                                   \
}

//! Macro to marshal a message to the client
#define EUCA_MESSAGE_MARSHAL(_thefunc, _theadb, _themeta)                               \
{                                                                                       \
	int i = 0;                                                                          \
	int j = 0;                                                                          \
	adb_serviceInfoType_t *sit = NULL;                                                  \
	adb_##_thefunc##_set_correlationId((_theadb), env, (_themeta)->correlationId);      \
	adb_##_thefunc##_set_userId((_theadb), env, (_themeta)->userId);                    \
	adb_##_thefunc##_set_epoch((_theadb), env,  (_themeta)->epoch);                     \
	for (i = 0; ((i < (_themeta)->servicesLen) && (i < 16)); i++) {                     \
		sit = adb_serviceInfoType_create(env);                                          \
		adb_serviceInfoType_set_type(sit, env, (_themeta)->services[i].type);           \
		adb_serviceInfoType_set_name(sit, env, (_themeta)->services[i].name);           \
		adb_serviceInfoType_set_partition(sit, env, (_themeta)->services[i].partition); \
		for (j = 0; ((j < (_themeta)->services[i].urisLen) && (j < 8)); j++) {	        \
			adb_serviceInfoType_add_uris(sit, env, (_themeta)->services[i].uris[j]);    \
		}                                                                               \
		adb_##_thefunc##_add_services((_theadb), env, sit);                             \
	}                                                                                   \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_ADB_HELPERS_H_ */

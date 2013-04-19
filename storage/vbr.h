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

#ifndef _INCLUDE_VBR_H_
#define _INCLUDE_VBR_H_
//!
//! @file storage/vbr.h
//! Need to provide description
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include "data.h"
#include "blobstore.h"

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! This is two hours; note that using multiplication with longs in defines causes 32bit compile warnings
#define INSTANCE_PREP_TIMEOUT_USEC               7200000000L    //!< @TODO change the timeout?

#define MAX_ARTIFACT_DEPS                            16
#define MAX_ARTIFACT_SIG                         262144
#define MAX_SSHKEY_SIZE                          262144

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

//! Defines the artifact structure. Artifact can be located either in a blobstore or on a file system:
typedef struct _artifact {
    char id[EUCA_MAX_PATH];            //!< either ID or PATH to the artifact
    boolean id_is_path;                //!< if set, id is a PATH

    char sig[MAX_ARTIFACT_SIG];        //!< unique signature for the artifact (IGNORED for a sentinel)
    boolean may_be_cached;             //!< the underlying blob may reside in cache (it will not be modified by an instance)
    boolean is_in_cache;               //!< indicates if the artifact is known to reside in cache (value only valid after artifact allocation)
    boolean must_be_file;              //!< the bits for this artifact must reside in a regular file (rather than just on a block device)
    boolean must_be_hollow;            //!< create the artifact with BLOBSTORE_FLAG_HOLLOW set (so its size won count toward the limit)
    boolean do_not_download;           //!< flag that tells creator functions to avoid actual content download (useful on migration destinations)
    int (*creator) (struct _artifact * a);  //!< function that can create this artifact based on info in this struct (must be NULL for a sentinel)
    long long size_bytes;              //!< size of the artifact, in bytes (OPTIONAL for some types)
    virtualBootRecord *vbr;            //!< VBR associated with the artifact (OPTIONAL for some types)
    boolean do_make_bootable;          //!< tells 'disk_creator' whether to make the disk bootable
    boolean do_tune_fs;                //!< tells 'copy_creator' whether to tune the file system
    boolean is_partition;              //!< this artifact is a partition for a disk to be constructed
    char sshkey[MAX_SSHKEY_SIZE];      //!< the key to inject into the artifact (OPTIONAL for all except keyed_disk_creator)
    blockblob *bb;                     //!< blockblob handle for the artifact, when it is open
    struct _artifact *deps[MAX_ARTIFACT_DEPS];  //!< array of pointers to artifacts that this artifact depends on
    int seq;                           //!< sequence number of the artifact
    int refs;                          //!< reference counter (1 or more if contained in deps[] of others)
    char instanceId[32];               //!< here purely for annotating logs
    void *internal;                    //!< OPTIONAL pointer to any other artifact-specific data 'creator' may need
} artifact;

//! Struct for local host config to use if making remote calls.
//! Needed for calls to the SC
typedef struct host_config {
	char iqn[CHAR_BUFFER_SIZE];
	char ip[HOSTNAME_SIZE];
	char ws_sec_policy_file[EUCA_MAX_PATH];
	int use_ws_sec;
	char sc_url[512]; //!< Sized to the same as the serviceInfoType. Should be updated on each epoch change.
} host_config;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/
host_config localhost_config;

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/
int vbr_init_hostconfig(char *hostIqn, char *hostIp, char *ws_sec_policy_file, int use_ws_sec);
int vbr_update_hostconfig_scurl(char *new_sc_url);
int get_localhost_sc_url(char *dest);

int vbr_add_ascii(const char *spec_str, virtualMachine * vm_type);
int vbr_parse(virtualMachine * vm, ncMetadata * pMeta);
int vbr_legacy(const char *instanceId, virtualMachine * params, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL);

int art_add_dep(artifact * a, artifact * dep);
void art_free(artifact * a);
void arts_free(artifact * array[], unsigned int array_len);

boolean tree_uses_blobstore(artifact * a);
boolean tree_uses_cache(artifact * a);

artifact *art_alloc(const char *id, const char *sig, long long size_bytes, boolean may_be_cached, boolean must_be_file, boolean must_be_hollow,
                    int (*creator) (artifact * a), virtualBootRecord * vbr);

void art_set_instanceId(const char *instanceId);
artifact *vbr_alloc_tree(virtualMachine * vm, boolean do_make_bootable, boolean do_make_work_copy, boolean is_migration_dest, const char *sshkey, const char *instanceId);
int art_implement_tree(artifact * root, blobstore * work_bs, blobstore * cache_bs, const char *work_prefix, long long timeout_usec);

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

#endif /* ! _INCLUDE_VBR_H_ */

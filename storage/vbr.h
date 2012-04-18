// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*
  Copyright (c) 2009  Eucalyptus Systems, Inc.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, only version 3 of the License.

  This file is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  for more details.

  You should have received a copy of the GNU General Public License along
  with this program.  If not, see <http://www.gnu.org/licenses/>.

  Please contact Eucalyptus Systems, Inc., 130 Castilian
  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
  if you need additional information or have any questions.

  This file may incorporate work covered under the following copyright and
  permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California


  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

  Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

  Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/

#include "data.h"
#include "blobstore.h"

/* This is two hours; note that using multiplication with longs in defines causes 32bit compile warnings */
#define INSTANCE_PREP_TIMEOUT_USEC 7200000000L // TODO: change the timeout?

#define MAX_ARTIFACT_DEPS 16
#define MAX_ARTIFACT_SIG 4096
#define MAX_SSHKEY_SIZE 4096

typedef struct _artifact {
    // artifact can be located either in a blobstore or on a file system:
    char id [EUCA_MAX_PATH]; // either ID or PATH to the artifact
    boolean id_is_path; // if set, id is a PATH

    char sig [MAX_ARTIFACT_SIG]; // unique signature for the artifact (IGNORED for a sentinel)
    boolean may_be_cached; // the underlying blob may reside in cache (it will not be modified by an instance)
    boolean must_be_file; // the bits for this artifact must reside in a regular file (rather than just on a block device)
    boolean must_be_hollow; // create the artifact with BLOBSTORE_FLAG_HOLLOW set (so its size won count toward the limit)
    int (* creator) (struct _artifact * a); // function that can create this artifact based on info in this struct (must be NULL for a sentinel)
    long long size_bytes; // size of the artifact, in bytes (OPTIONAL for some types)
    virtualBootRecord * vbr; // VBR associated with the artifact (OPTIONAL for some types)
    boolean do_make_bootable; // tells 'disk_creator' whether to make the disk bootable
    boolean do_tune_fs; // tells 'copy_creator' whether to tune the file system
    boolean is_partition; // this artifact is a partition for a disk to be constructed
    char sshkey [MAX_SSHKEY_SIZE]; // the key to inject into the artifact (OPTIONAL for all except keyed_disk_creator)
    blockblob * bb; // blockblob handle for the artifact, when it is open
    struct _artifact * deps [MAX_ARTIFACT_DEPS]; // array of pointers to artifacts that this artifact depends on
    int seq; // sequence number of the artifact
    int refs; // reference counter (1 or more if contained in deps[] of others)
    char instanceId [32]; // here purely for annotating logs
    void * internal; // OPTIONAL pointer to any other artifact-specific data 'creator' may need
} artifact;

int vbr_add_ascii (const char * spec_str, virtualMachine * vm_type);
int vbr_legacy (const char * instanceId, virtualMachine * vm, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL);
int vbr_parse (virtualMachine * vm, ncMetadata * meta);
artifact * vbr_alloc_tree (virtualMachine * vm, boolean do_make_bootable, boolean do_make_work_copy, const char * sshkey, const char * instanceId);
void art_set_instanceId (const char * instanceId);
int art_implement_tree (artifact * root, blobstore * work_bs, blobstore * cache_bs, const char * work_prefix, long long timeout);
artifact * art_alloc (const char * id, const char * sig, long long size_bytes, boolean may_be_cached, boolean must_be_file, boolean must_be_hollow, int (* creator) (artifact * a), virtualBootRecord * vbr);
int art_add_dep (artifact * a, artifact * dep);
void art_free (artifact * a);
boolean tree_uses_blobstore (artifact * a);
boolean tree_uses_cache (artifact * a);

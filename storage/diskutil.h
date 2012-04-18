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

#ifndef HELPERS_H
#define HELPERS_H

#include "misc.h" // bolean
#include "ipc.h" // sem

#define MBR_BLOCKS 63 // the size of "DOS-compatibility region" partially used by 'grub'
#define SECTOR_SIZE 512

int diskutil_init (int require_grub);
sem * diskutil_get_loop_sem (void);
int diskutil_cleanup (void);
int diskutil_ddzero (const char * path, const long long sectors, boolean zero_fill);
int diskutil_dd (const char * in, const char * out, const int bs, const long long count);
int diskutil_dd2 (const char * in, const char * out, const int bs, const long long count, const long long seek, const long long skip);
int diskutil_mbr (const char * path, const char * type);
int diskutil_part (const char * path, char * part_type, const char * fs_type, const long long first_sector, const long long last_sector);
int diskutil_loop (const char * path, const long long offset, char * lodev, int lodev_size);
int diskutil_loop_check (const char * path, const char * lodev);
int diskutil_loop_clean (const char * path);
int diskutil_unloop (const char * lodev);
int diskutil_mkswap (const char * lodev, const long long size_bytes);
int diskutil_mkfs (const char * lodev, const long long size_bytes);
int diskutil_tune (const char * lodev);
int diskutil_sectors (const char * path, const int part, long long * first, long long * last);
int diskutil_mount (const char * dev, const char * mnt_pt);
int diskutil_umount (const char * dev);
int diskutil_write2file (const char * file, const char * str);
int diskutil_grub (const char * path, const char * mnt_pt, const int part, const char * kernel, const char * ramdisk);
int diskutil_grub_files (const char * mnt_pt, const int part, const char * kernel, const char * ramdisk);
int diskutil_grub_mbr (const char * path, const int part);
int diskutil_grub2_mbr (const char * path, const int part, const char * mnt_pt);
int diskutil_ch (const char * path, const char * user, const char * group, const int perms);
int diskutil_mkdir (const char * path);
int diskutil_cp (const char * from, const char * to);
long long round_up_sec   (long long bytes);
long long round_down_sec (long long bytes);
#endif

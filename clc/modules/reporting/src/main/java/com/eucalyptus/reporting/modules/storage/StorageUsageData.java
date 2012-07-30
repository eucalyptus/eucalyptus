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

package com.eucalyptus.reporting.modules.storage;

import javax.persistence.*;

@Embeddable
public class StorageUsageData
{
	@Column(name="volumes_num", nullable=false)
	protected Long volumesNum;
	@Column(name="volumes_megs", nullable=false)
	protected Long volumesMegs;
	@Column(name="snapshot_num", nullable=false)
	protected Long snapshotsNum;
	@Column(name="snapshot_megs", nullable=false)
	protected Long snapshotsMegs;
	
	public StorageUsageData()
	{
		this.volumesNum     = new Long(0);
		this.volumesMegs    = new Long(0);
		this.snapshotsNum   = new Long(0);
		this.snapshotsMegs  = new Long(0);
	}

	public StorageUsageData(Long volumesNum, Long volumesMegs, Long snapshotsNum,
			Long snapshotsMegs)
	{
		if (volumesNum == null || volumesMegs == null || snapshotsNum == null
				|| snapshotsMegs == null)
		{
			throw new IllegalArgumentException("args can't be null");
		}
		this.volumesNum     = volumesNum;
		this.volumesMegs    = volumesMegs;
		this.snapshotsNum   = snapshotsNum;
		this.snapshotsMegs  = snapshotsMegs;
	}

	public Long getVolumesNum()
	{
		return volumesNum;
	}
	
	public Long getVolumesMegs()
	{
		return volumesMegs;
	}
	
	public Long getSnapshotsNum()
	{
		return snapshotsNum;
	}
	
	public Long getSnapshotsMegs()
	{
		return snapshotsMegs;
	}
	
	public void setVolumesNum(Long volumesNum)
	{
		if (volumesNum==null) throw new IllegalArgumentException("arg can't be null");
		this.volumesNum = volumesNum;
	}

	public void setVolumesMegs(Long volumesMegs)
	{
		if (volumesMegs==null) throw new IllegalArgumentException("arg can't be null");
		this.volumesMegs = volumesMegs;
	}

	public void setSnapshotsNum(Long snapshotsNum)
	{
		if (snapshotsNum==null) throw new IllegalArgumentException("arg can't be null");
		this.snapshotsNum = snapshotsNum;
	}

	public void setSnapshotsMegs(Long snapshotsMegs)
	{
		if (snapshotsMegs==null) throw new IllegalArgumentException("arg can't be null");
		this.snapshotsMegs = snapshotsMegs;
	}

	private static Long sumLongs(Long a, Long b)
	{
		return new Long(a.longValue() + b.longValue());
	}

	public StorageUsageData sumFrom(StorageUsageData other)
	{
		if (other == null) return null;
		return new StorageUsageData(
				sumLongs(this.volumesNum, other.volumesNum),
				sumLongs(this.volumesMegs, other.volumesMegs),
				sumLongs(this.snapshotsNum, other.snapshotsNum),
				sumLongs(this.snapshotsMegs, other.snapshotsMegs)
				);
	}

	public String toString()
	{
		return String.format("[vols:%d,volsMegs:%d,snaps:%d,snapsMegs:%d]",
				volumesNum, volumesMegs, snapshotsNum,
				snapshotsMegs);
	}


}

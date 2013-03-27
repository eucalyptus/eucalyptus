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
package com.eucalyptus.reporting.art.entity;

import com.eucalyptus.reporting.art.ArtObject;
import com.google.common.base.Objects;

public class InstanceUsageArtEntity
	implements ArtObject
{
	private long durationMs           = 0l;
	private int  instanceCnt          = 0;

	private Long cpuUtilizationMs;
	private Long diskReadBytes;
	private Long diskWriteBytes;
	private Long diskReadOps;
	private Long diskWriteOps;
	private Long netTotalInBytes;
	private Long netTotalOutBytes;
	private Long netExternalInBytes;
	private Long netExternalOutBytes;
	private Long diskReadTime;
	private Long diskWriteTime;

	public boolean isEmpty() {
		return
				cpuUtilizationMs == null &&
				diskReadBytes == null &&
				diskWriteBytes == null &&
				diskReadOps == null &&
				diskWriteOps == null &&
				netTotalInBytes == null &&
				netTotalOutBytes == null &&
				netExternalInBytes == null &&
				netExternalOutBytes == null &&
				diskReadTime == null &&
				diskWriteTime == null;
	}

	public long getDurationMs()
	{
		return durationMs;
	}

	public void addDurationMs(long durationMs)
	{
		this.durationMs += durationMs;
	}
	
	public void setDurationMs(long durationMs)
	{
		this.durationMs = durationMs;
	}

	public int getInstanceCnt()
	{
		return instanceCnt;
	}

	public void addInstanceCnt(int instanceCnt)
	{
		this.instanceCnt += instanceCnt;
	}

	public Long getCpuUtilizationMs()
	{
		return cpuUtilizationMs;
	}

	public void addCpuUtilizationMs(Long cpuUtilizationMs)
	{
		this.cpuUtilizationMs = plus(this.cpuUtilizationMs, cpuUtilizationMs);
	}

	public Long getDiskReadBytes()
	{
		return diskReadBytes;
	}

	public void addDiskReadBytes( Long diskReadBytes )
	{
		this.diskReadBytes = plus(this.diskReadBytes, diskReadBytes);
	}

	public Long getDiskWriteBytes()
	{
		return diskWriteBytes;
	}

	public void addDiskWriteBytes( Long diskWriteBytes )
	{
		this.diskWriteBytes = plus(this.diskWriteBytes, diskWriteBytes);
	}

	public Long getDiskReadOps() {
		return diskReadOps;
	}

	public void addDiskReadOps( Long diskReadOps )
	{
		this.diskReadOps = plus(this.diskReadOps, diskReadOps);
	}

	public Long getDiskWriteOps() {
		return diskWriteOps;
	}

	public void addDiskWriteOps( Long diskWriteOps )
	{
		this.diskWriteOps = plus(this.diskWriteOps, diskWriteOps);
	}

	public Long getDiskReadTime() {
		return diskReadTime;
	}

	public void addDiskReadTime( Long diskReadTime )
	{
		this.diskReadTime = plus(this.diskReadTime, diskReadTime);
	}

	public Long getDiskWriteTime() {
		return diskWriteTime;
	}

	public void addDiskWriteTime( Long diskWriteTime )
	{
		this.diskWriteTime = plus(this.diskWriteTime, diskWriteTime);
	}

	public Long getNetTotalInBytes()
	{
		return netTotalInBytes;
	}

	public void addNetTotalInBytes(Long netTotalInBytes)
	{
		this.netTotalInBytes = plus(this.netTotalInBytes, netTotalInBytes);
	}

	public Long getNetTotalOutBytes()
	{
		return netTotalOutBytes;
	}

	public void addNetTotalOutBytes(Long netTotalOutBytes)
	{
		this.netTotalOutBytes = plus(this.netTotalOutBytes, netTotalOutBytes);
	}

	public Long getNetExternalInBytes()
	{
		return netExternalInBytes;
	}

	public void addNetExternalInBytes(Long netExternalInBytes)
	{
		this.netExternalInBytes = plus(this.netExternalInBytes, netExternalInBytes);
	}

	public Long getNetExternalOutBytes()
	{
		return netExternalOutBytes;
	}

	public void addNetExternalOutBytes(Long netExternalOutBytes)
	{
		this.netExternalOutBytes = plus(this.netExternalOutBytes, netExternalOutBytes);
	}

    public void addUsage( final InstanceUsageArtEntity usage )
    {
        addDurationMs( usage.getDurationMs() );
        addCpuUtilizationMs( usage.getCpuUtilizationMs() );
        addDiskReadBytes( usage.getDiskReadBytes() );
        addDiskWriteBytes( usage.getDiskWriteBytes() );
        addDiskReadOps( usage.getDiskReadOps() );
        addDiskWriteOps( usage.getDiskWriteOps() );
        addDiskReadTime( usage.getDiskReadTime() );
        addDiskWriteTime( usage.getDiskWriteTime() );
        addNetTotalInBytes( usage.getNetTotalInBytes() );
        addNetTotalOutBytes( usage.getNetTotalOutBytes() );
        addNetExternalInBytes( usage.getNetExternalInBytes() );
        addNetExternalOutBytes( usage.getNetExternalOutBytes() );
        addInstanceCnt( 1 );
    }
	
	private static Long plus(Long a, Long b)
	{
		if (a!=null || b!=null) {
			return Objects.firstNonNull(a, 0L) + Objects.firstNonNull(b, 0L);
		} else {
			return null;
		}
	}

}

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

public class InstanceUsageArtEntity
	implements ArtObject
{
	private long durationMs           = 0l;
	private int  instanceCnt          = 0;

	private Long cpuUtilizationMs     = null;
	private Long diskInMegs           = null;
	private Long diskOutMegs          = null;
	private Long netInternalInMegs    = null;
	private Long netInternalOutMegs   = null;
	private Long netTotalInMegs       = null;
	private Long netTotalOutMegs      = null;

	public InstanceUsageArtEntity()
	{
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

	private static Long plus(Long a, Long b)
	{
		if (a!=null && b!=null) {
			return a.longValue()+b.longValue();
		} else if (a!=null) {
			return a.longValue();
		} else if (b!=null) {
			return b.longValue();
		} else {
			return null;
		}
	}
	
	public void addCpuUtilizationMs(Long cpuUtilizationMs)
	{
		this.cpuUtilizationMs = plus(this.cpuUtilizationMs, cpuUtilizationMs);
	}

	public Long getDiskInMegs()
	{
		return diskInMegs;
	}

	public void addDiskInMegs(Long diskInMegs)
	{
		this.diskInMegs = plus(this.diskInMegs, diskInMegs);
	}

	public Long getDiskOutMegs()
	{
		return diskOutMegs;
	}

	public void addDiskOutMegs(Long diskOutMegs)
	{
		this.diskOutMegs = plus(this.diskOutMegs, diskOutMegs);
	}

	public Long getNetInternalInMegs()
	{
		return netInternalInMegs;
	}

	public void addNetInternalInMegs(Long netInternalInMegs)
	{
		this.netInternalInMegs = plus(this.netInternalInMegs, netInternalInMegs);
}

	public Long getNetInternalOutMegs()
	{
		return netInternalOutMegs;
	}

	public void addNetInternalOutMegs(Long netInternalOutMegs)
	{
		this.netInternalOutMegs = plus(this.netInternalOutMegs, netInternalOutMegs);
	}

	public Long getNetTotalInMegs()
	{
		return netTotalInMegs;
	}

	public void addNetTotalInMegs(Long netTotalInMegs)
	{
		this.netTotalInMegs = plus(this.netTotalInMegs, netTotalInMegs);
	}

	public Long getNetTotalOutMegs()
	{
		return netTotalOutMegs;
	}

	public void addNetTotalOutMegs(Long netExternalOutMegs)
	{
		this.netTotalOutMegs = plus(this.netTotalOutMegs, netTotalOutMegs);
	}

}

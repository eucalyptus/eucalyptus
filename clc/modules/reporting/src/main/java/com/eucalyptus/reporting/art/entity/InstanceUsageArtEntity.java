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
	/* Attributes for cpu utilization percent */
	private Double cpuPercentAvg = null;
	private long durationMs = 0l;

	/* Attributes for usage-days */
	private Long diskIoMegSecs = null;
	private Long netIoWithinZoneInMegSecs = null;
	private Long netIoBetweenZoneInMegSecs = null;
	private Long netIoPublicIpInMegSecs = null;
	private Long netIoWithinZoneOutMegSecs = null;
	private Long netIoBetweenZoneOutMegSecs = null;
	private Long netIoPublicIpOutMegSecs = null;

	/* Attributes for max usage */
	private Long diskIoMegMax = null;
	private Long netIoWithinZoneInMegMax = null;
	private Long netIoBetweenZoneInMegMax = null;
	private Long netIoPublicIpInMegMax = null;
	private Long netIoWithinZoneOutMegMax = null;
	private Long netIoBetweenZoneOutMegMax = null;
	private Long netIoPublicIpOutMegMax = null;
	
	public InstanceUsageArtEntity()
	{
	}

	public InstanceUsageArtEntity(Double cpuPercentAvg,
			Long diskIoMegSecs, Long netIoWithinZoneInMegSecs,
			Long netIoBetweenZoneInMegSecs, Long netIoPublicIpInMegSecs,
			Long netIoWithinZoneOutMegSecs, Long netIoBetweenZoneOutMegSecs,
			Long netIoPublicIpOutMegSecs, Long diskIoMegMax,
			Long netIoWithinZoneInMegMax, Long netIoBetweenZoneInMegMax,
			Long netIoPublicIpInMegMax, Long netIoWithinZoneOutMegMax,
			Long netIoBetweenZoneOutMegMax, Long netIoPublicIpOutMegMax)
	{
		this.cpuPercentAvg = cpuPercentAvg;
		this.diskIoMegSecs = diskIoMegSecs;
		this.netIoWithinZoneInMegSecs = netIoWithinZoneInMegSecs;
		this.netIoBetweenZoneInMegSecs = netIoBetweenZoneInMegSecs;
		this.netIoPublicIpInMegSecs = netIoPublicIpInMegSecs;
		this.netIoWithinZoneOutMegSecs = netIoWithinZoneOutMegSecs;
		this.netIoBetweenZoneOutMegSecs = netIoBetweenZoneOutMegSecs;
		this.netIoPublicIpOutMegSecs = netIoPublicIpOutMegSecs;
		this.diskIoMegMax = diskIoMegMax;
		this.netIoWithinZoneInMegMax = netIoWithinZoneInMegMax;
		this.netIoBetweenZoneInMegMax = netIoBetweenZoneInMegMax;
		this.netIoPublicIpInMegMax = netIoPublicIpInMegMax;
		this.netIoWithinZoneOutMegMax = netIoWithinZoneOutMegMax;
		this.netIoBetweenZoneOutMegMax = netIoBetweenZoneOutMegMax;
		this.netIoPublicIpOutMegMax = netIoPublicIpOutMegMax;
	}

	public Double getCpuPercentAvg()
	{
		return cpuPercentAvg;
	}

	public long getDurationMs()
	{
		return durationMs;
	}
	
	public Long getDiskIoMegSecs()
	{
		return diskIoMegSecs;
	}

	public Long getNetIoWithinZoneInMegSecs()
	{
		return netIoWithinZoneInMegSecs;
	}

	public Long getNetIoBetweenZoneInMegSecs()
	{
		return netIoBetweenZoneInMegSecs;
	}

	public Long getNetIoPublicIpInMegSecs()
	{
		return netIoPublicIpInMegSecs;
	}

	public Long getNetIoWithinZoneOutMegSecs()
	{
		return netIoWithinZoneOutMegSecs;
	}

	public Long getNetIoBetweenZoneOutMegSecs()
	{
		return netIoBetweenZoneOutMegSecs;
	}

	public Long getNetIoPublicIpOutMegSecs()
	{
		return netIoPublicIpOutMegSecs;
	}

	public Long getDiskIoMegMax()
	{
		return diskIoMegMax;
	}

	public Long getNetIoWithinZoneInMegMax()
	{
		return netIoWithinZoneInMegMax;
	}

	public Long getNetIoBetweenZoneInMegMax()
	{
		return netIoBetweenZoneInMegMax;
	}

	public Long getNetIoPublicIpInMegMax()
	{
		return netIoPublicIpInMegMax;
	}

	public Long getNetIoWithinZoneOutMegMax()
	{
		return netIoWithinZoneOutMegMax;
	}

	public Long getNetIoBetweenZoneOutMegMax()
	{
		return netIoBetweenZoneOutMegMax;
	}

	public Long getNetIoPublicIpOutMegMax()
	{
		return netIoPublicIpOutMegMax;
	}

	public void setCpuPercentAvg(Double cpuPercentAvg)
	{
		this.cpuPercentAvg = cpuPercentAvg;
	}
	
	public void addDurationMs(long addMs)
	{
		this.durationMs = addMs;
	}

	public void setDiskIoMegSecs(Long diskIoMegSecs)
	{
		this.diskIoMegSecs = diskIoMegSecs;
	}

	public void setNetIoWithinZoneInMegSecs(Long netIoWithinZoneInMegSecs)
	{
		this.netIoWithinZoneInMegSecs = netIoWithinZoneInMegSecs;
	}

	public void setNetIoBetweenZoneInMegSecs(Long netIoBetweenZoneInMegSecs)
	{
		this.netIoBetweenZoneInMegSecs = netIoBetweenZoneInMegSecs;
	}

	public void setNetIoPublicIpInMegSecs(Long netIoPublicIpInMegSecs)
	{
		this.netIoPublicIpInMegSecs = netIoPublicIpInMegSecs;
	}

	public void setNetIoWithinZoneOutMegSecs(Long netIoWithinZoneOutMegSecs)
	{
		this.netIoWithinZoneOutMegSecs = netIoWithinZoneOutMegSecs;
	}

	public void setNetIoBetweenZoneOutMegSecs(Long netIoBetweenZoneOutMegSecs)
	{
		this.netIoBetweenZoneOutMegSecs = netIoBetweenZoneOutMegSecs;
	}

	public void setNetIoPublicIpOutMegSecs(Long netIoPublicIpOutMegSecs)
	{
		this.netIoPublicIpOutMegSecs = netIoPublicIpOutMegSecs;
	}

	public void setDiskIoMegMax(Long diskIoMegMax)
	{
		this.diskIoMegMax = diskIoMegMax;
	}

	public void setNetIoWithinZoneInMegMax(Long netIoWithinZoneInMegMax)
	{
		this.netIoWithinZoneInMegMax = netIoWithinZoneInMegMax;
	}

	public void setNetIoBetweenZoneInMegMax(Long netIoBetweenZoneInMegMax)
	{
		this.netIoBetweenZoneInMegMax = netIoBetweenZoneInMegMax;
	}

	public void setNetIoPublicIpInMegMax(Long netIoPublicIpInMegMax)
	{
		this.netIoPublicIpInMegMax = netIoPublicIpInMegMax;
	}

	public void setNetIoWithinZoneOutMegMax(Long netIoWithinZoneOutMegMax)
	{
		this.netIoWithinZoneOutMegMax = netIoWithinZoneOutMegMax;
	}

	public void setNetIoBetweenZoneOutMegMax(Long netIoBetweenZoneOutMegMax)
	{
		this.netIoBetweenZoneOutMegMax = netIoBetweenZoneOutMegMax;
	}

	public void setNetIoPublicIpOutMegMax(Long netIoPublicIpOutMegMax)
	{
		this.netIoPublicIpOutMegMax = netIoPublicIpOutMegMax;
	}

	public String toString()
	{
		return String.format("(%3f %4d %4d %4d %4d %4d %4d %4d %4d %4d %4d %4d %4d %4d %4d)",
				cpuPercentAvg, diskIoMegSecs, netIoWithinZoneInMegSecs, netIoBetweenZoneInMegSecs,
				netIoPublicIpInMegSecs, netIoWithinZoneOutMegSecs, netIoBetweenZoneOutMegSecs,
				netIoPublicIpOutMegMax, diskIoMegMax, netIoWithinZoneInMegMax, netIoBetweenZoneInMegMax,
				netIoPublicIpInMegMax, netIoWithinZoneOutMegMax, netIoBetweenZoneOutMegMax,
				netIoPublicIpOutMegMax);
	}

}

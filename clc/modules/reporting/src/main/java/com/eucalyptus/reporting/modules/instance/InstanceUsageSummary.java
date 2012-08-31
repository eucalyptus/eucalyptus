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

package com.eucalyptus.reporting.modules.instance;

import com.eucalyptus.reporting.Period;

/**
 * <p>InstanceUsageSummary is a summary of all resource usage for some entity
 * over a time period. It includes summations of the instance types,
 * numbers, running times, resource usage, etc, for a given user,
 * account or other entity.
 */
public class InstanceUsageSummary
{
	private Long m1SmallNum;
	private Long m1SmallTimeSecs;
	private Long c1MediumNum;
	private Long c1MediumTimeSecs;
	private Long m1LargeNum;
	private Long m1LargeTimeSecs;
	private Long m1XLargeNum;
	private Long m1XLargeTimeSecs;
	private Long c1XLargeNum;
	private Long c1XLargeTimeSecs;
	private Long networkIoMegs;
	private Long diskIoMegs;
	
	public InstanceUsageSummary()
	{
		this.m1SmallNum = new Long(0);
		this.m1SmallTimeSecs = new Long(0);
		this.c1MediumNum = new Long(0);
		this.c1MediumTimeSecs = new Long(0);
		this.m1LargeNum = new Long(0);
		this.m1LargeTimeSecs = new Long(0);
		this.m1XLargeNum = new Long(0);
		this.m1XLargeTimeSecs = new Long(0);
		this.c1XLargeNum = new Long(0);
		this.c1XLargeTimeSecs = new Long(0);
		this.networkIoMegs = new Long(0);
		this.diskIoMegs = new Long(0);
	}

	public InstanceUsageSummary(Long m1SmallNum, Long m1SmallTimeSecs,
			Long c1MediumNum, Long c1MediumTimeSecs, Long m1LargeNum,
			Long m1LargeTimeSecs, Long m1XLargeNum, Long m1XLargeTimeSecs,
			Long c1XLargeNum, Long c1XLargeTimeSecs, Long networkIoMegs,
			Long diskIoMegs)
	{
		//Check that no args are null
		if (m1SmallNum == null || m1SmallTimeSecs == null || c1MediumNum == null
			|| c1MediumTimeSecs == null || m1LargeNum == null || m1LargeTimeSecs == null
			|| m1XLargeNum == null || m1XLargeTimeSecs == null || c1XLargeNum == null
			|| c1XLargeTimeSecs == null || networkIoMegs == null || diskIoMegs == null)
		{
			throw new IllegalArgumentException("ctor args cannot be null");
		}
		
		/* Check all values are positive as negative values are erroneous here
		 */
		if (m1SmallNum!=null && m1SmallNum.longValue()<0)
			throw new IllegalArgumentException("m1SmallNum<0");
		if (m1SmallTimeSecs!=null && m1SmallTimeSecs.longValue()<0)
			throw new IllegalArgumentException("m1SmallTimeSecs<0");
		if (c1MediumNum!=null && c1MediumNum.longValue()<0)
			throw new IllegalArgumentException("c1MediumNum<0");
		if (c1MediumTimeSecs!=null && c1MediumTimeSecs.longValue()<0)
			throw new IllegalArgumentException("c1MediumTimeSecs<0");
		if (m1LargeNum!=null && m1LargeNum.longValue()<0)
			throw new IllegalArgumentException("m1LargeNum<0");
		if (m1LargeTimeSecs!=null && m1LargeTimeSecs.longValue()<0)
			throw new IllegalArgumentException("m1LargeTimeSecs<0");
		if (m1XLargeNum!=null && m1XLargeNum.longValue()<0)
			throw new IllegalArgumentException("m1XLargeNum<0");
		if (m1XLargeTimeSecs!=null && m1XLargeTimeSecs.longValue()<0)
			throw new IllegalArgumentException("m1XLargeTimeSecs<0");
		if (c1XLargeNum!=null && c1XLargeNum.longValue()<0)
			throw new IllegalArgumentException("c1XLargeNum<0");
		if (c1XLargeTimeSecs!=null && c1XLargeTimeSecs.longValue()<0)
			throw new IllegalArgumentException("c1XLargeTimeSecs<0");
		if (networkIoMegs!=null && networkIoMegs.longValue()<0)
			throw new IllegalArgumentException("networkIoMegs<0");
		if (diskIoMegs!=null && diskIoMegs.longValue()<0)
			throw new IllegalArgumentException("diskIoMegs<0");
		
		
		this.m1SmallNum = m1SmallNum;
		this.m1SmallTimeSecs = m1SmallTimeSecs;
		this.c1MediumNum = c1MediumNum;
		this.c1MediumTimeSecs = c1MediumTimeSecs;
		this.m1LargeNum = m1LargeNum;
		this.m1LargeTimeSecs = m1LargeTimeSecs;
		this.m1XLargeNum = m1XLargeNum;
		this.m1XLargeTimeSecs = m1XLargeTimeSecs;
		this.c1XLargeNum = c1XLargeNum;
		this.c1XLargeTimeSecs = c1XLargeTimeSecs;
		this.networkIoMegs = networkIoMegs;
		this.diskIoMegs = diskIoMegs;
	}

	public Long getM1SmallNum()
	{
		return m1SmallNum;
	}

	public void setM1SmallNum(Long m1SmallNum)
	{
		if (m1SmallNum == null || m1SmallNum.longValue() < 0)
			throw new IllegalArgumentException("arg cant be null or negative");
		this.m1SmallNum = m1SmallNum;
	}
	
	public Long getM1SmallTimeSecs()
	{
		return m1SmallTimeSecs;
	}

	public void setM1SmallTimeSecs(Long m1SmallTimeSecs)
	{
		if (m1SmallTimeSecs == null || m1SmallTimeSecs.longValue() < 0)
			throw new IllegalArgumentException("arg cant be null or negative");
		this.m1SmallTimeSecs = m1SmallTimeSecs;
	}

	public Long getC1MediumNum()
	{
		return c1MediumNum;
	}

	public void setC1MediumNum(Long c1MediumNum)
	{
		if (c1MediumNum == null || c1MediumNum.longValue() < 0)
			throw new IllegalArgumentException("arg cant be null or negative");
		this.c1MediumNum = c1MediumNum;
	}

	public Long getC1MediumTimeSecs()
	{
		return c1MediumTimeSecs;
	}

	public void setC1MediumTimeSecs(Long c1MediumTimeSecs)
	{
		if (c1MediumTimeSecs == null || c1MediumTimeSecs.longValue() < 0)
			throw new IllegalArgumentException("arg cant be null or negative");
		this.c1MediumTimeSecs = c1MediumTimeSecs;
	}

	public Long getM1LargeNum()
	{
		return m1LargeNum;
	}

	public void setM1LargeNum(Long m1LargeNum)
	{
		if (m1LargeNum == null || m1LargeNum.longValue() < 0)
			throw new IllegalArgumentException("arg cant be null or negative");
		this.m1LargeNum = m1LargeNum;
	}

	public Long getM1LargeTimeSecs()
	{
		return m1LargeTimeSecs;
	}

	public void setM1LargeTimeSecs(Long m1LargeTimeSecs)
	{
		if (m1LargeTimeSecs == null || m1LargeTimeSecs.longValue() < 0)
			throw new IllegalArgumentException("arg cant be null or negative");
		this.m1LargeTimeSecs = m1LargeTimeSecs;
	}

	public Long getM1XLargeNum()
	{
		return m1XLargeNum;
	}

	public void setM1XLargeNum(Long m1XLargeNum)
	{
		if (m1XLargeNum == null || m1XLargeNum.longValue() < 0)
			throw new IllegalArgumentException("arg cant be null or negative");
		this.m1XLargeNum = m1XLargeNum;
	}

	public Long getM1XLargeTimeSecs()
	{
		return m1XLargeTimeSecs;
	}

	public void setM1XLargeTimeSecs(Long m1XLargeTimeSecs)
	{
		if (m1XLargeTimeSecs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.m1XLargeTimeSecs = m1XLargeTimeSecs;
	}

	public Long getC1XLargeNum()
	{
		return c1XLargeNum;
	}

	public void setC1XLargeNum(Long c1XLargeNum)
	{
		if (c1XLargeNum == null || c1XLargeNum.longValue() < 0)
			throw new IllegalArgumentException("arg cant be null or negative");
		this.c1XLargeNum = c1XLargeNum;
	}

	public Long getC1XLargeTimeSecs()
	{
		return c1XLargeTimeSecs;
	}

	public void setC1XLargeTimeSecs(Long c1XLargeTimeSecs)
	{
		if (c1XLargeTimeSecs == null)
			throw new IllegalArgumentException("arg can't be null");
		this.c1XLargeTimeSecs = c1XLargeTimeSecs;
	}

	public Long getDiskIoMegs()
	{
		return diskIoMegs;
	}

	public void setDiskIoMegs(Long diskIoMegs)
	{
		if (diskIoMegs== null || diskIoMegs.longValue()<0)
			throw new IllegalArgumentException("diskIo cant be null or negative");
		this.diskIoMegs = diskIoMegs;
	}

	public Long getNetworkIoMegs()
	{
		return networkIoMegs;
	}

	public void setNetworkIoMegs(Long networkIoMegs)
	{
		if (networkIoMegs== null || networkIoMegs.longValue()<0)
			throw new IllegalArgumentException("networkIo cant be null or negative");
		this.networkIoMegs = networkIoMegs;
	}

	void addM1SmallNum(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.m1SmallNum = new Long(this.m1SmallNum.longValue() + addBy);
	}

	void addM1SmallTimeSecs(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.m1SmallTimeSecs = new Long(this.m1SmallTimeSecs.longValue() + addBy);
	}

	void addC1MediumNum(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.c1MediumNum = new Long(this.c1MediumNum.longValue() + addBy);
	}

	void addC1MediumTimeSecs(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.c1MediumTimeSecs = new Long(this.c1MediumTimeSecs.longValue() + addBy);
	}

	void addM1LargeNum(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.m1LargeNum = new Long(this.m1LargeNum.longValue() + addBy);
	}

	void addM1LargeTimeSecs(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.m1LargeTimeSecs = new Long(this.m1LargeTimeSecs.longValue() + addBy);
	}

	void addM1XLargeNum(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.m1XLargeNum = new Long(this.m1XLargeNum.longValue() + addBy);
	}

	void addM1XLargeTimeSecs(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.m1XLargeTimeSecs = new Long(this.m1XLargeTimeSecs.longValue() + addBy);
	}

	void addC1XLargeNum(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.c1XLargeNum = new Long(this.c1XLargeNum.longValue() + addBy);
	}

	void addC1XLargeTimeSecs(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.c1XLargeTimeSecs = new Long(this.c1XLargeTimeSecs.longValue() + addBy);
	}

	void addNetworkIoMegs(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.networkIoMegs = new Long(this.networkIoMegs.longValue() + addBy);
	}

	void addDiskIoMegs(long addBy)
	{
		if (addBy<0) throw new IllegalArgumentException("addBy<0");
		this.diskIoMegs = new Long(this.diskIoMegs.longValue() + addBy);
	}

	public void addUsage(InstanceUsageSummary summary)
	{
		this.diskIoMegs = addLongs(this.diskIoMegs, summary.diskIoMegs);
		this.networkIoMegs = addLongs(this.networkIoMegs, summary.networkIoMegs);
		this.m1SmallNum = addLongs(this.m1SmallNum, summary.m1SmallNum);
		this.c1MediumNum = addLongs(this.c1MediumNum, summary.c1MediumNum);
		this.m1LargeNum = addLongs(this.m1LargeNum, summary.m1LargeNum);
		this.m1XLargeNum = addLongs(this.m1XLargeNum, summary.m1XLargeNum);
		this.c1XLargeNum = addLongs(this.c1XLargeNum, summary.c1XLargeNum);
		this.m1SmallTimeSecs = addLongs(this.m1SmallTimeSecs, summary.m1SmallTimeSecs);
		this.c1MediumTimeSecs = addLongs(this.c1MediumTimeSecs, summary.c1MediumTimeSecs);
		this.m1LargeTimeSecs = addLongs(this.m1LargeTimeSecs, summary.m1LargeTimeSecs);
		this.m1XLargeTimeSecs = addLongs(this.m1XLargeTimeSecs, summary.m1XLargeTimeSecs);
		this.c1XLargeTimeSecs = addLongs(this.c1XLargeTimeSecs, summary.c1XLargeTimeSecs);
	}
	
	private static Long addLongs(Long a, Long b)
	{
		if (a != null && b != null) {
			return new Long(a.longValue() + b.longValue());
		} else {
			return null;
		}
	}
	
	public void sumFromUsageData(InstanceUsageData ud)
	{
		//Autoboxing should work because I prevented nulls everywhere
		this.diskIoMegs = this.diskIoMegs + ud.getDiskIoMegs();
		this.networkIoMegs = this.networkIoMegs + ud.getNetworkIoMegs();
	}
	
	public void sumFromDurationSecsAndType(long durationSecs, String type)
	{
		if (durationSecs < 0) {
			throw new IllegalArgumentException ("durationSecs cant be <0");
		}
		if (type == null) {
			throw new IllegalArgumentException("args can't be null");
		}
		if (durationSecs==0) {
			/* If the instance ran for 0 seconds then it didnt run at all and
			 * should not add to the instance count.
			 */
			return;
		}

		//TODO: the strings here should be in an enum or something. same with events?
		//Autoboxing should work because we prevented nulls everywhere
		if (type.equalsIgnoreCase("m1.small")) {
			this.m1SmallNum = this.m1SmallNum + 1;
			this.m1SmallTimeSecs = this.m1SmallTimeSecs + durationSecs;
		} else if (type.equalsIgnoreCase("c1.medium")) {
			this.c1MediumNum = this.c1MediumNum + 1;
			this.c1MediumTimeSecs = this.c1MediumTimeSecs + durationSecs;
		} else if (type.equalsIgnoreCase("m1.large")) {
			this.m1LargeNum = this.m1LargeNum + 1;
			this.m1LargeTimeSecs = this.m1LargeTimeSecs + durationSecs;
		} else if (type.equalsIgnoreCase("m1.xlarge")) {
			this.m1XLargeNum = this.m1XLargeNum + 1;
			this.m1XLargeTimeSecs = this.m1XLargeTimeSecs + durationSecs;
		} else if (type.equalsIgnoreCase("c1.xlarge")) {
			this.c1XLargeNum = this.c1XLargeNum + 1;
			this.c1XLargeTimeSecs = this.c1XLargeTimeSecs + durationSecs;
		} else {
			throw new RuntimeException("Unrecognized type:" + type);
		}
	}
	
	/**
	 * toString() for logging and debugging
	 */
	public String toString()
	{
		return String.format("[num,timeSecs m1Small:%d,%d c1Medium:%d,%d m1Large"
				+ ":%d,%d m1XLarge:%d,%d c1XLarge:%d,%d disk:%d net:%d]",
				this.m1SmallNum, this.m1SmallTimeSecs, this.c1MediumNum,
				this.c1MediumTimeSecs, this.m1LargeNum, this.m1LargeTimeSecs,
				this.m1XLargeNum, this.m1XLargeTimeSecs, this.c1XLargeNum,
				this.c1XLargeTimeSecs, this.diskIoMegs, this.networkIoMegs);
	}
}

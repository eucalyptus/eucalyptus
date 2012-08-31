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

/**
 * <p>StorageUsageSummary is a summary of storage usage for some entity over
 * time. It contains statistics of aggregate usage like megs-seconds and maximum
 * usage.
 * 
 * <p>StorageUsageSummary is not a bean. StorageUsageSummary <i>accumulates</i>
 * data through the <pre>update</pre> method and then returns resulting
 * statistics using the various <pre>get</pre> methods.
 */
public class StorageUsageSummary
{
	private long volumesMegsMax;
	private long volumesMegsSecs;
	private long snapshotsMegsMax;
	private long snapshotsMegsSecs;

	public StorageUsageSummary()
	{
		this.volumesMegsMax    = 0l;
		this.volumesMegsSecs   = 0l;
		this.snapshotsMegsMax  = 0l;
		this.snapshotsMegsSecs = 0l;
	}

	public long getVolumesMegsMax()
	{
		return volumesMegsMax;
	}

	public long getVolumesMegsSecs()
	{
		return volumesMegsSecs;
	}

	public long getSnapshotsMegsMax()
	{
		return snapshotsMegsMax;
	}

	public long getSnapshotsMegsSecs()
	{
		return snapshotsMegsSecs;
	}
	
	

	void setVolumesMegsMax(long volumesMegsMax)
	{
		this.volumesMegsMax = volumesMegsMax;
	}

	void setVolumesMegsSecs(long volumesMegsSecs)
	{
		this.volumesMegsSecs = volumesMegsSecs;
	}

	void setSnapshotsMegsMax(long snapshotsMegsMax)
	{
		this.snapshotsMegsMax = snapshotsMegsMax;
	}

	void setSnapshotsMegsSecs(long snapshotsMegsSecs)
	{
		this.snapshotsMegsSecs = snapshotsMegsSecs;
	}
	
	public void addVolumesMegsSecs(long volumesMegsSecs)
	{
		this.volumesMegsSecs += volumesMegsSecs;
	}

	public void addSnapshotsMegsSecs(long snapshotsMegsSecs)
	{
		this.snapshotsMegsSecs += snapshotsMegsSecs;
	}

	void addUsage(StorageUsageSummary summary)
	{
		this.volumesMegsMax   = Math.max(this.volumesMegsMax, summary.getVolumesMegsMax());
		this.snapshotsMegsMax   = Math.max(this.snapshotsMegsMax, summary.getSnapshotsMegsMax());
		
		this.volumesMegsSecs  += summary.getVolumesMegsSecs();
		this.snapshotsMegsSecs  += summary.getSnapshotsMegsSecs();
	}


	public void updateValues(long volumesMegs, long snapshotsMegs,
			long durationSecs)
	{
		this.volumesMegsMax   = Math.max(this.volumesMegsMax, volumesMegs);
		this.snapshotsMegsMax = Math.max(this.snapshotsMegsMax, snapshotsMegs);
		
		this.volumesMegsSecs   += volumesMegs   * durationSecs;
		this.snapshotsMegsSecs += snapshotsMegs * durationSecs;
	}
	
	@Override
	public String toString()
	{
		return String.format("[volsMegsSecs:%d,volsMegsMax:%d,snapsMegsSecs:%d,"
				+ "snapsMegsMax:%d]",
				volumesMegsSecs, volumesMegsMax, snapshotsMegsSecs,
				snapshotsMegsMax);
	}

}

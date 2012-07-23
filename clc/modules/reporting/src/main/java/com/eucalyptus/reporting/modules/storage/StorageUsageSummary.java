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

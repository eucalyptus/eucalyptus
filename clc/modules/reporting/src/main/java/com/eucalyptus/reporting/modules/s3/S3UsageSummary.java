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

package com.eucalyptus.reporting.modules.s3;

/**
 * <p>S3UsageSummary is a summary of storage usage for some entity over
 * time. It contains statistics of aggregate usage like megs-seconds and maximum
 * usage.
 */
public class S3UsageSummary
{
	private long bucketsNumMax;
	private long objectsMegsMax;
	private long objectsMegsSecs;

	public S3UsageSummary()
	{
		this.bucketsNumMax     = 0l;
		this.objectsMegsMax    = 0l;
		this.objectsMegsSecs   = 0l;
	}

	public long getBucketsNumMax()
	{
		return bucketsNumMax;
	}

	public long getObjectsMegsMax()
	{
		return objectsMegsMax;
	}

	public long getObjectsMegsSecs()
	{
		return objectsMegsSecs;
	}
	
	public void setBucketsNumMax(long bucketsNumMax)
	{
		this.bucketsNumMax = bucketsNumMax;
	}

	public void setObjectsMegsMax(long objectsMegsMax)
	{
		this.objectsMegsMax = objectsMegsMax;
	}
	
	public void addObjectsMegsSecs(long objectsMegsSecs)
	{
		this.objectsMegsSecs += objectsMegsSecs;
	}

	public void setObjectsMegsSecs(long objectsMegsSecs)
	{
		this.objectsMegsSecs = objectsMegsSecs;
	}

	public void updateValues(long objectsMegs, long bucketsNum, long durationSecs)
	{
		this.objectsMegsMax   = Math.max(this.objectsMegsMax, objectsMegs);
		this.bucketsNumMax    = Math.max(this.bucketsNumMax, bucketsNum);
		
		this.objectsMegsSecs   += objectsMegs   * durationSecs;
	}
	
	void addUsage(S3UsageSummary summary)
	{
		this.objectsMegsMax   = Math.max(this.objectsMegsMax, summary.getObjectsMegsMax());
		this.bucketsNumMax    = Math.max(this.bucketsNumMax, summary.getBucketsNumMax());
		
		this.objectsMegsSecs  += summary.getObjectsMegsSecs();		
	}
	
	@Override
	public String toString()
	{
		return String.format("[bucketsNumMax:%d,objsMegsSecs:%d,objsMegsMax:%d]",
				bucketsNumMax, objectsMegsSecs, objectsMegsMax);
	}

}

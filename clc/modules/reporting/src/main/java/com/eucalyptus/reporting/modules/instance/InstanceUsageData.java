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

package com.eucalyptus.reporting.modules.instance;

/**
 * <p>UsageData represents usage of resources (like disk bandwidth, etc)
 * by an instance over a single period. UsageData is immutable.
 * 
 * <p>Null values for a field indicate unknown usage, not zero usage.
 */
public class InstanceUsageData
{
	private final Long networkIoMegs;
	private final Long diskIoMegs;

	public InstanceUsageData(Long totalNetworkIoMegs, Long totalDiskIoMegs)
	{
		this.networkIoMegs = totalNetworkIoMegs;
		this.diskIoMegs = totalDiskIoMegs;
	}

	public Long getNetworkIoMegs()
	{
		return this.networkIoMegs;
	}

	public Long getDiskIoMegs()
	{
		return this.diskIoMegs;
	}

	/**
	 * Sum the numeric fields of two UsageData objects. If either operand is null
	 * for any field then the resultant field is null.
	 */
	public InstanceUsageData sum(InstanceUsageData other)
	{
		final Long sumNetworkIoMegs =
			(this.networkIoMegs==null || other.networkIoMegs==null)
			? null
			: new Long(other.networkIoMegs.longValue() + this.networkIoMegs.longValue());

		final Long sumDiskIoMegs =
			(this.diskIoMegs==null || other.diskIoMegs==null)
			? null
			: new Long(other.diskIoMegs.longValue() + this.diskIoMegs.longValue());

		return new InstanceUsageData(sumNetworkIoMegs, sumDiskIoMegs);
	}

	/**
	 * Subtract the numeric fields of one UsageData from another. If either 
	 * operand is null for any field then the resultant field is null.
	 */
	public InstanceUsageData subtractFrom(InstanceUsageData other)
	{
		final Long subtractedNetworkIoMegs =
			(this.networkIoMegs==null || other.networkIoMegs==null)
			? null
			: new Long(other.networkIoMegs.longValue() - this.networkIoMegs.longValue());

		final Long subtractedDiskIoMegs =
			(this.diskIoMegs==null || other.diskIoMegs==null)
			? null
			: new Long(other.diskIoMegs.longValue() - this.diskIoMegs.longValue());

		return new InstanceUsageData(subtractedNetworkIoMegs, subtractedDiskIoMegs);
	}
	
	
	/**
	 * toString() for debugging and logs
	 */
	public String toString()
	{
		return String.format("[disk:%d,net:%d]", this.diskIoMegs, this.networkIoMegs);
	}

}

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
 * UsageSnapshot of the resource usage of some instance at some point in time.
 * Contains <i>cumulative</i> usage data so it's populated with
 * all resource usage which has occurred up until this snapshot was
 * instantiated. 
 */
class UsageSnapshot
{
	private final Long timestampMs;
	private final InstanceUsageData cumulativeUsageData;

	/**
	 * For hibernate usage only; don't extend this class
	 */
	protected UsageSnapshot()
	{
		this.timestampMs = null;
		this.cumulativeUsageData = null;
	}

	public UsageSnapshot(long timestampMs, InstanceUsageData cumulativeUsageData)
	{
		this.timestampMs = new Long(timestampMs);
		this.cumulativeUsageData = cumulativeUsageData;
	}

	public long getTimestampMs()
	{
		assert this.timestampMs != null;
		return this.timestampMs.longValue();
	}

	public InstanceUsageData getCumulativeUsageData()
	{
		return this.cumulativeUsageData;
	}

}

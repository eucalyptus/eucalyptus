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

import com.eucalyptus.reporting.ReportLine;
import com.eucalyptus.reporting.units.*;

public class StorageReportLine
	implements Comparable<StorageReportLine>, ReportLine
{
	private static final Units INTERNAL_UNITS =
		new Units(TimeUnit.SECS, SizeUnit.MB, TimeUnit.SECS, SizeUnit.MB);
	
	private final StorageReportLineKey key;
	private final StorageUsageSummary summary;
	private final Units units;

	StorageReportLine(StorageReportLineKey key,
			StorageUsageSummary summary, Units units)
	{
		this.key = key;
		this.summary = summary;
		this.units = units;
	}

	public String getLabel()
	{
		return key.getLabel();
	}

	public String getGroupBy()
	{
		return key.getGroupByLabel();
	}

	public Long getVolumesSizeMax()
	{
		return UnitUtil.convertSize(summary.getVolumesMegsMax(),
				INTERNAL_UNITS.getSizeUnit(), units.getSizeUnit());
	}

	public Long getVolumesSizeTime()
	{
		return UnitUtil.convertSizeTime(summary.getVolumesMegsSecs(),
				INTERNAL_UNITS.getSizeTimeSizeUnit(), units.getSizeTimeSizeUnit(),
				INTERNAL_UNITS.getSizeTimeTimeUnit(), units.getSizeTimeTimeUnit());
	}

	public Long getSnapshotsSizeMax()
	{
		return UnitUtil.convertSize(summary.getSnapshotsMegsMax(),
				INTERNAL_UNITS.getSizeUnit(), units.getSizeUnit());
	}

	public Long getSnapshotsSizeTime()
	{
		return UnitUtil.convertSizeTime(summary.getSnapshotsMegsSecs(),
				INTERNAL_UNITS.getSizeTimeSizeUnit(), units.getSizeTimeSizeUnit(),
				INTERNAL_UNITS.getSizeTimeTimeUnit(), units.getSizeTimeTimeUnit());
	}

	public Units getUnits()
	{
		return units;
	}
	
	void addUsage(StorageUsageSummary summary)
	{
		this.summary.addUsage(summary);
	}

	public String toString()
	{
		return String.format("[label:%s,groupBy:%s,volSizeMax:%d,volSizeTime:%d,snapsSizeMax:%d,snapsSizeTime:%d]",
						getLabel(), getGroupBy(), getVolumesSizeMax(),
						getVolumesSizeTime(), getSnapshotsSizeMax(),
						getSnapshotsSizeTime());
	}

	@Override
	public int compareTo(StorageReportLine other)
	{
		return key.compareTo(other.key);
	}


}

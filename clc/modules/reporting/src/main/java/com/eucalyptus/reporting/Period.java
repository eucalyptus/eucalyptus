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

package com.eucalyptus.reporting;

/**
 * <p>Period represents a time period. Period is immutable.
 */
public final class Period
{
	private final Long beginningMs;
	private final Long endingMs;

	public Period(long beginningMs, long endingMs)
	{
		this.beginningMs = new Long(beginningMs);
		this.endingMs = new Long(endingMs);
	}

	public long getBeginningMs()
	{
		return this.beginningMs.longValue();
	}

	public long getEndingMs()
	{
		return this.endingMs.longValue();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((beginningMs == null) ? 0 : beginningMs.hashCode());
		result = prime * result
				+ ((endingMs == null) ? 0 : endingMs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Period other = (Period) obj;
		if (beginningMs == null) {
			if (other.beginningMs != null)
				return false;
		} else if (!beginningMs.equals(other.beginningMs))
			return false;
		if (endingMs == null) {
			if (other.endingMs != null)
				return false;
		} else if (!endingMs.equals(other.endingMs))
			return false;
		return true;
	}

	public String toString()
	{
		return String.format("[%d-%d]", beginningMs, endingMs);
	}

}

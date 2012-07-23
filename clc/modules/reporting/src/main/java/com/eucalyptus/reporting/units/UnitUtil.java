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

package com.eucalyptus.reporting.units;

public class UnitUtil
{
	public static Long convertSize(Long size, SizeUnit from, SizeUnit to)
	{
		return (size==null)
			? null
			: new Long((size.longValue() * from.getFactor()) / to.getFactor());
	}
	
	public static Long convertTime(Long time, TimeUnit from, TimeUnit to)
	{
		return (time==null)
			? null
			: new Long((time.longValue() * from.getFactor()) / to.getFactor());
	}
	
	public static Long convertSizeTime(Long sizeTime, SizeUnit sizeFrom,
			SizeUnit sizeTo, TimeUnit timeFrom, TimeUnit timeTo)
	{
		return convertTime(convertSize(sizeTime, sizeFrom, sizeTo), timeFrom, timeTo);
	}
}

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
package com.eucalyptus.reporting.art.util;

import java.util.*;

/**
 * <p>AttachDurationCalculator determines an attachment duration, given attachment
 * and detachment times. First you feed in the attachment times, then the detachment
 * times. It stores the attachment times, then subtracts the detachment
 * times later when those are fed in.
 * 
 * <p>This result cannot be found by just retaining a Map of attachment times and subtracting
 * later. There can be multiple attachments and detachments of a resource, to and from the
 * same thing. A simple Map would retain only the most recent attachment time and would
 * overwrite any previous time.
 * 
 * <p>This result cannot be found be joining the attachment and detachment tables in the
 * database and subtracting the timestamps. Again, there can be multiple attachment and
 * detachments of a resource, to and from the same thing, so a join would yield a cartesian
 * product of attachments and detachments.
 * 
 * <p>This class handles repeated attachments and detachments of the same resource, to and from
 * the same thing.
 *  
 * <p>This is used to scan through all attachment times in a database, then scan through
 * all detachment times, then find all attachment durations. Thus, finding all attachment
 * durations takes two full table scans. It's not possible to have a single table for
 * attachment and detachment times, then lookup the row and fill in the missing value 
 * upon detachment, as these tables have no indexes for faster insertion.
 * 
 * <p>In all cases, durations will be truncated according to report beginning and end.
 */
public class AttachDurationCalculator<A, B>
{
	// resourceId -> attachedResourceId -> SortedSet -> timestampMs
	private final Map<A,Map<B,TreeSet<Long>>> attachments;
	private final long reportBeginMs;
	private final long reportEndMs;
	
	public AttachDurationCalculator(long reportBeginMs, long reportEndMs)
	{
		this.attachments = new HashMap<A,Map<B,TreeSet<Long>>>();
		this.reportBeginMs = reportBeginMs;
		this.reportEndMs = reportEndMs;
	}

	public void attach(A resourceKey, B attachedKey, long timestampMs)
	{
		if (timestampMs > reportEndMs) return;  //Attachment falls entirely outside report boundaries
		if (! attachments.containsKey(resourceKey)) {
			attachments.put(resourceKey, new HashMap<B,TreeSet<Long>>());
		}
		Map<B,TreeSet<Long>> innerMap = attachments.get(resourceKey);
		if (!innerMap.containsKey(attachedKey)) {
			innerMap.put(attachedKey, new TreeSet<Long>());
		}
		innerMap.get(attachedKey).add(Math.max(reportBeginMs, timestampMs));
	}
	
	/**
	 * @return duration in milliseconds of how long the attachment was
	 */
	public long detach(A resourceKey, B attachedResourceKey, long timestampMs)
	{
		if (timestampMs < reportBeginMs) return 0l; //Attachment falls entirely outside report boundaries
		if (! attachments.containsKey(resourceKey)) {
			attachments.put(resourceKey, new HashMap<B,TreeSet<Long>>());
		}
		Map<B,TreeSet<Long>> innerMap = attachments.get(resourceKey);
		if (!innerMap.containsKey(attachedResourceKey)) {
			innerMap.put(attachedResourceKey, new TreeSet<Long>());
		}
		TreeSet<Long> timestamps = innerMap.get(attachedResourceKey);
		Long attachTimestamp = timestamps.floor(timestampMs);
		if (attachTimestamp != null) {
			return DurationCalculator.boundDuration(reportBeginMs, reportEndMs,
					attachTimestamp.longValue(), timestampMs);
		} else {
			return 0;
		}
	}
	
}

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
			return (Math.min(timestampMs, reportEndMs) - attachTimestamp.longValue());
		} else {
			return 0;
		}
	}
	
}

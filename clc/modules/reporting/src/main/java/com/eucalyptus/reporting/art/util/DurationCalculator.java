package com.eucalyptus.reporting.art.util;

import java.util.*;

/**
 * Can have repeated start and end times, and can have a start time at the end
 * with no corresponding end time. 
 */
public class DurationCalculator<T>
{
	private final Map<T, TreeSet<Long>> startTimesMs;
	private final Map<T, TreeSet<Long>> endTimesMs;
	private final long defaultEndMs;
	
	/**
	 * @param defaultEndMs A default end time for objects with start times
	 *   but no end times (i.e. created but not yet deleted). For example,
	 *   report end time.
	 */
	public DurationCalculator(long defaultEndMs)
	{
		startTimesMs = new HashMap<T, TreeSet<Long>>();
		endTimesMs = new HashMap<T, TreeSet<Long>>();
		this.defaultEndMs = defaultEndMs;
	}
	
	public void addStart(T key, long timestampMs)
	{
		if (!startTimesMs.containsKey(key)) {
			startTimesMs.put(key, new TreeSet<Long>());
		}
		if (!endTimesMs.containsKey(key)) {
			endTimesMs.put(key, new TreeSet<Long>());
		}
		startTimesMs.get(key).add(timestampMs);
	}
	
	public void addEnd(T key, long timestampMs)
	{
		if (!startTimesMs.containsKey(key)) {
			startTimesMs.put(key, new TreeSet<Long>());
		}
		if (!endTimesMs.containsKey(key)) {
			endTimesMs.put(key, new TreeSet<Long>());
		}
		endTimesMs.get(key).add(timestampMs);
	}
	
	public Map<T,Long> getDurationMap()
	{
		Map<T,Long> rv = new HashMap<T,Long>();
		for (T key: startTimesMs.keySet()) {
			for (Long startMs : startTimesMs.get(key)) {
				Long endMs = endTimesMs.get(key).higher(startMs);
				if (endMs==null) endMs = defaultEndMs;
				rv.put(key, endMs-startMs);
			}
		}
		return rv;
	}
	
}

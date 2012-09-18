package com.eucalyptus.reporting.art.util;

import java.util.*;

public class DurationCalculator<T>
{
	private final Map<T, TreeSet<Long>> startTimesMs;
	
	public DurationCalculator()
	{
		startTimesMs = new HashMap<T, TreeSet<Long>>();
	}
	
	public void addStart(T key, long timestampMs)
	{
		if (!startTimesMs.containsKey(key)) {
			startTimesMs.put(key, new TreeSet<Long>());
		}
		startTimesMs.get(key).add(timestampMs);
	}
	
	public long getDuration(T key, long timestampMs)
	{
		if (!startTimesMs.containsKey(key)) return 0l;
		TreeSet<Long> treeSet = startTimesMs.get(key);
		if (treeSet.size()==0) return 0l;
		return treeSet.floor(timestampMs);
	}
	
}

package com.eucalyptus.reporting.instance;

import java.util.*;

/**
 * <p>LogResultSet contains the results of a log query from InstanceUsageLog.
 * LogResultSet is conceptually similar to a jdbc ResultSet; it has a
 * "cursor" which you can move forward by calling <i>next()</i>, and a set of
 * objects for each cursor position.
 * 
 * <p>LogResultSet is lazily loading.
 * 
 * @author tom.werges
 */
public class LogResultSet
{
	private final Iterator iter;
	private final Period period;

	private UsageSnapshot nextSnapshot;
	private InstanceAttributes nextInsAttrs;

	private List<UsageSnapshot> snapshotList;
	private InstanceAttributes insAttrs = null;

	LogResultSet(Iterator iter, Period period)
	{
		this.iter = iter;
		this.period = period;
	}

	public boolean next()
	{
		if (nextSnapshot != null) {
			snapshotList = new ArrayList<UsageSnapshot>();
			snapshotList.add(this.nextSnapshot);
			insAttrs = this.nextInsAttrs;
			nextSnapshot = null;
		}

		while (iter.hasNext()) {
			Object[] row = (Object[]) iter.next();
			InstanceAttributes ia = (InstanceAttributes) row[0];
			InstanceUsageSnapshot ius = (InstanceUsageSnapshot) row[1];
			UsageSnapshot us = ius.getUsageSnapshot();

			if (insAttrs == null) {
				insAttrs = ia;
				snapshotList = new ArrayList<UsageSnapshot>();
				snapshotList.add(us);
			} else	if (insAttrs.getUuid().equals(ia.getUuid())) {
				snapshotList.add(us);
			} else {
				nextInsAttrs = ia;
				nextSnapshot = us;
				break;
			}
		}

		return (iter.hasNext() || nextSnapshot != null);
	}

	public InstanceAttributes getInstanceAttributes()
	{
		return insAttrs;
	}

	public PeriodUsageData getPeriodUsageData()
	{
		return new PeriodUsageData(period, snapshotList);
	}

}

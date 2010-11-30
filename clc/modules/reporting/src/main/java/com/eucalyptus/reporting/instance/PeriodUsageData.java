package com.eucalyptus.reporting.instance;

import java.util.List;

/**
 * <p>PeriodUsageData records usage by some instance over some period.
 * Usage data is not cumulative; usage data represents only the usage of
 * resources over a single period.
 * 
 * @author tom.werges
 */
public class PeriodUsageData
{
	private final Period period;
	private final List<UsageSnapshot> snapshotList;

	public PeriodUsageData(Period period, List<UsageSnapshot> snapshotList)
	{
		if (snapshotList.size() < 1) {
			throw new IllegalArgumentException("snapshotList must have at least one element");
		}
		this.period = period;
		this.snapshotList = snapshotList;
	}

	public Period getPeriod()
	{
		return period;
	}

	/**
	 * Get the usage data for the entire period.
	 */
	public UsageData getUsageData()
	{
		return getUsageData(period);
	}

	/**
	 * Get the usage data for some subset of the period of this PeriodUsageData.
	 */
	public UsageData getUsageData(Period period)
	{
        assert snapshotList.size() > 0;  //verified in ctor
        UsageSnapshot latestSnapshot = snapshotList.get(0);
        UsageSnapshot earliestSnapshot = snapshotList.get(0);

        /* SnapshotList isn't ordered.
         */
        for (UsageSnapshot snapshot: snapshotList) {
            if (snapshot.getTimestampMs() < earliestSnapshot.getTimestampMs()) {
                earliestSnapshot = snapshot;
            } else if (snapshot.getTimestampMs() > latestSnapshot.getTimestampMs()) {
                latestSnapshot = snapshot;
            }
        }

        UsageData earliestUsageData = earliestSnapshot.getCumulativeUsageData();
        UsageData latestUsageData = latestSnapshot.getCumulativeUsageData();
        return earliestUsageData.subtractFrom(latestUsageData);
	}

}

package com.eucalyptus.reporting.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.units.Units;

public class InstanceDisplayDb
{
	private InstanceUsageLog usageLog;

	private InstanceDisplayDb(InstanceUsageLog usageLog)
	{
		this.usageLog = usageLog;
	}

	private static InstanceDisplayDb instance = null;
	
	public static InstanceDisplayDb getInstance()
	{
		if (instance == null) {
			instance = new InstanceDisplayDb(InstanceUsageLog.getInstanceUsageLog());
		}
		return instance;
	}
	
	public List<InstanceDisplayBean> search(Period period, GroupByCriterion criterion,
			Units displayUnits)
	{
		Map<String, InstanceUsageSummary> summaryMap =
			usageLog.scanSummarize(period, criterion);
		List<InstanceDisplayBean> items =
			new ArrayList<InstanceDisplayBean>(summaryMap.size());
		for (String key: summaryMap.keySet()) {
			items.add(new InstanceDisplayBean(key, null, summaryMap.get(key),
					displayUnits));
		}
		return items;
	}
	
	public List<InstanceDisplayBean> searchGroupBy(Period period, GroupByCriterion groupByCrit,
			GroupByCriterion crit, Units displayUnits)
	{
		Map<String, Map<String, InstanceUsageSummary>> summaryMap =
			usageLog.scanSummarize(period, groupByCrit, crit);
		List<InstanceDisplayBean> items =
			new ArrayList<InstanceDisplayBean>(summaryMap.size());
		for (String outerKey: summaryMap.keySet()) {
			Map<String, InstanceUsageSummary> innerMap =
				summaryMap.get(outerKey);
			for (String innerKey: innerMap.keySet()) {
				items.add(new InstanceDisplayBean(innerKey, outerKey,
							innerMap.get(innerKey), displayUnits));
			}
		}
		return items;
	}

}

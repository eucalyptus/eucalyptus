package com.eucalyptus.reporting.storage;

import java.util.*;

import com.eucalyptus.reporting.*;
import com.eucalyptus.reporting.units.*;

public class StorageDisplayDb
{
	private StorageUsageLog usageLog;

	private StorageDisplayDb(StorageUsageLog usageLog)
	{
		this.usageLog = usageLog;
	}

	private static StorageDisplayDb instance = null;

	public static StorageDisplayDb getInstance()
	{
		if (instance == null) {
			instance = new StorageDisplayDb(StorageUsageLog.getStorageUsageLog());
		}
		return instance;
	}

	public List<StorageDisplayBean> search(Period period, GroupByCriterion criterion,
			Units displayUnits)
	{
		Map<String, StorageUsageSummary> summaryMap =
			usageLog.scanSummarize(period, criterion);
		List<StorageDisplayBean> items =
			new ArrayList<StorageDisplayBean>(summaryMap.size());
		for (String key: summaryMap.keySet()) {
			items.add(new StorageDisplayBean(key, null, summaryMap.get(key),
					displayUnits));
		}
		return items;
	}

	public List<StorageDisplayBean> searchGroupBy(
			Period period, GroupByCriterion groupByCrit,
			GroupByCriterion crit, Units displayUnits)
	{
		Map<String, Map<String, StorageUsageSummary>> summaryMap =
			usageLog.scanSummarize(period, groupByCrit, crit);
		List<StorageDisplayBean> items = 
			new ArrayList<StorageDisplayBean>(summaryMap.size());
		for (String outerKey: summaryMap.keySet()) {
			Map<String, StorageUsageSummary> innerMap =
				summaryMap.get(outerKey);
			for (String innerKey: innerMap.keySet()) {
				items.add(new StorageDisplayBean(innerKey, outerKey,
							innerMap.get(innerKey), displayUnits));
			}
		}
		return items;
	}

}

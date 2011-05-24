package com.eucalyptus.reporting.storage;

import java.util.*;

import org.mortbay.log.Log;

import com.eucalyptus.reporting.ReportingCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.units.Units;

public class StorageReportLineGenerator
{
	private static StorageReportLineGenerator instance;
	
	public static StorageReportLineGenerator getInstance()
	{
		if (instance == null) {
			instance = new StorageReportLineGenerator();
		}
		return instance;
	}
	
	private StorageReportLineGenerator()
	{
		
	}
	
	public List<StorageReportLine> getReportLines(Period period,
			ReportingCriterion criterion,	Units displayUnits)
	{
		return getReportLines(period, null, criterion, displayUnits);
	}

	public List<StorageReportLine> getReportLines(Period period, ReportingCriterion groupByCrit,
			ReportingCriterion crit, Units displayUnits)
	{
		Map<StorageReportLineKey, StorageReportLine> reportLineMap =
			new HashMap<StorageReportLineKey, StorageReportLine>();
		
		StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();
		Map<StorageSummaryKey, StorageUsageSummary> usageMap = 
			usageLog.getUsageSummaryMap(period);
		for (StorageSummaryKey key: usageMap.keySet()) {
			Log.info("Adding key:" + key + " data:" + usageMap.get(key));
			String critVal = getAttributeValue(crit, key);
			String groupVal = (groupByCrit==null) ? null : getAttributeValue(groupByCrit, key);
			StorageReportLineKey lineKey = new StorageReportLineKey(critVal, groupVal);
			if (!reportLineMap.containsKey(lineKey)) {
				reportLineMap.put(lineKey, new StorageReportLine(lineKey,
						new StorageUsageSummary(), displayUnits));
			}
			StorageReportLine reportLine = reportLineMap.get(lineKey);
			StorageUsageSummary summary = usageMap.get(key);
			reportLine.addUsage(summary);
		}

		final List<StorageReportLine> results = new ArrayList<StorageReportLine>();
		for (StorageReportLineKey lineKey: reportLineMap.keySet()) {
			results.add(reportLineMap.get(lineKey));
		}
		
		return results;
	}

	private static String getAttributeValue(ReportingCriterion criterion,
			StorageSummaryKey key)
	{
		switch (criterion) {
			case ACCOUNT:
				return key.getAccountId();
			case USER:
				return key.getOwnerId();
			case CLUSTER:
				return key.getClusterName();
			case AVAILABILITY_ZONE:
				return key.getAvailabilityZone();
			default:
				return key.getOwnerId();
		}
	}



}

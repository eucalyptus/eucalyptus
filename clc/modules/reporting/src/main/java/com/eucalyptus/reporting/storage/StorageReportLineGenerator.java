package com.eucalyptus.reporting.storage;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.ReportLineGenerator;
import com.eucalyptus.reporting.ReportingCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

public class StorageReportLineGenerator
	implements ReportLineGenerator<StorageReportLine>
{
	private static Logger log = Logger.getLogger( StorageReportLineGenerator.class );

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

		Collections.sort(results);

		return results;
	}

	private static String getAttributeValue(ReportingCriterion criterion,
			StorageSummaryKey key)
	{
		log.debug("owner id:" + key.getOwnerId() + " account id:" + key.getAccountId());

		switch (criterion) {
			case ACCOUNT:
				return ReportingAccountDao.getInstance().getAccountName(key.getAccountId());
			case USER:
				return ReportingUserDao.getInstance().getUserName(key.getOwnerId());
			case CLUSTER:
				return key.getClusterName();
			case AVAILABILITY_ZONE:
				return key.getAvailabilityZone();
			default:
				return ReportingUserDao.getInstance().getUserName(key.getOwnerId());
		}
	}



}

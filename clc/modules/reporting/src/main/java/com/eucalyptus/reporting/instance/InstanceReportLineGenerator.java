package com.eucalyptus.reporting.instance;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.ReportLineGenerator;
import com.eucalyptus.reporting.ReportingCriterion;
import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

public class InstanceReportLineGenerator
	implements ReportLineGenerator<InstanceReportLine>
{
	private static Logger log = Logger.getLogger( InstanceReportLineGenerator.class );

	private static InstanceReportLineGenerator instance;
	
	public static InstanceReportLineGenerator getInstance()
	{
		if (instance == null) {
			instance = new InstanceReportLineGenerator();
		}
		return instance;
	}
	
	private InstanceReportLineGenerator()
	{
		
	}
	
	public List<InstanceReportLine> getReportLines(Period period,
			ReportingCriterion criterion,	Units displayUnits)
	{
		return getReportLines(period, null, criterion, displayUnits);
	}

	public List<InstanceReportLine> getReportLines(Period period, ReportingCriterion groupByCrit,
			ReportingCriterion crit, Units displayUnits)
	{
		if (period==null || crit==null || displayUnits==null) {
			throw new IllegalArgumentException("Args can't be null");
		}
		
		Map<InstanceReportLineKey, InstanceReportLine> reportLineMap =
			new HashMap<InstanceReportLineKey, InstanceReportLine>();
		
		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<InstanceSummaryKey, InstanceUsageSummary> usageMap = 
			usageLog.getUsageSummaryMap(period);
		for (InstanceSummaryKey key: usageMap.keySet()) {
			String critVal = getAttributeValue(crit, key);
			String groupVal = (groupByCrit==null) ? null : getAttributeValue(groupByCrit, key);
			InstanceReportLineKey lineKey = new InstanceReportLineKey(critVal, groupVal);
			if (!reportLineMap.containsKey(lineKey)) {
				reportLineMap.put(lineKey, new InstanceReportLine(lineKey,
						new InstanceUsageSummary(), displayUnits));
			}
			InstanceReportLine reportLine = reportLineMap.get(lineKey);
			InstanceUsageSummary summary = usageMap.get(key);
			reportLine.addUsage(summary);
		}

		final List<InstanceReportLine> results = new ArrayList<InstanceReportLine>();
		for (InstanceReportLineKey lineKey: reportLineMap.keySet()) {
			results.add(reportLineMap.get(lineKey));
		}
		
		Collections.sort(results);
		return results;
	}

	private static String getAttributeValue(ReportingCriterion criterion,
			InstanceSummaryKey key)
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
				return key.getOwnerId();
		}
	}



}

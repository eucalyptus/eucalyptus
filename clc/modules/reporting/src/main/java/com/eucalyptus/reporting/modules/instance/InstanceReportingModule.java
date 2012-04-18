package com.eucalyptus.reporting.modules.instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.ReportingModule;
import com.eucalyptus.reporting.ReportingCriterion;
import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

public class InstanceReportingModule
	implements ReportingModule<InstanceReportLine>
{
	private static Logger log = Logger.getLogger( InstanceReportingModule.class );

	public InstanceReportingModule()
	{
		
	}
	
	public List<InstanceReportLine> getReportLines(Period period,
			ReportingCriterion criterion, Units displayUnits,
			String accountId)
	{
		return getReportLines(period, null, criterion, displayUnits, accountId);
	}

	public List<InstanceReportLine> getReportLines(Period period, ReportingCriterion groupByCrit,
			ReportingCriterion crit, Units displayUnits, String accountId)
	{
		if (period==null || crit==null || displayUnits==null) {
			throw new IllegalArgumentException("Args can't be null");
		}
		
		Map<InstanceReportLineKey, InstanceReportLine> reportLineMap =
			new HashMap<InstanceReportLineKey, InstanceReportLine>();
		
		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<InstanceSummaryKey, InstanceUsageSummary> usageMap = 
			usageLog.getUsageSummaryMap(period, accountId);
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

	@Override
	public String getJrxmlFilename()
	{
		return "instance.jrxml";
	}

	@Override
	public String getNestedJrxmlFilename()
	{
		return "nested_instance.jrxml";
	}

}

package com.eucalyptus.reporting.modules.s3;

import java.util.*;

import org.apache.log4j.Logger;
import org.mortbay.log.Log;

import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.ReportingModule;
import com.eucalyptus.reporting.ReportingCriterion;
import com.eucalyptus.reporting.units.Units;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

public class S3ReportingModule
	implements ReportingModule<S3ReportLine>
{
	private static Logger log = Logger.getLogger( S3ReportingModule.class );

	public S3ReportingModule()
	{
		
	}
	
	public List<S3ReportLine> getReportLines(Period period,
			ReportingCriterion criterion, Units displayUnits,
			String accountId)
	{
		return getReportLines(period, null, criterion, displayUnits, accountId);
	}

	public List<S3ReportLine> getReportLines(Period period, ReportingCriterion groupByCrit,
			ReportingCriterion crit, Units displayUnits, String accountId)
	{
		Map<S3ReportLineKey, S3ReportLine> reportLineMap =
			new HashMap<S3ReportLineKey, S3ReportLine>();
		
		S3UsageLog usageLog = S3UsageLog.getS3UsageLog();
		Map<S3SummaryKey, S3UsageSummary> usageMap = 
			usageLog.getUsageSummaryMap(period, accountId);
		for (S3SummaryKey key: usageMap.keySet()) {
			Log.info("Adding key:" + key + " data:" + usageMap.get(key));
			String critVal = getAttributeValue(crit, key);
			String groupVal = (groupByCrit==null) ? null : getAttributeValue(groupByCrit, key);
			S3ReportLineKey lineKey = new S3ReportLineKey(critVal, groupVal);
			if (!reportLineMap.containsKey(lineKey)) {
				reportLineMap.put(lineKey, new S3ReportLine(lineKey,
						new S3UsageSummary(), displayUnits));
			}
			S3ReportLine reportLine = reportLineMap.get(lineKey);
			S3UsageSummary summary = usageMap.get(key);
			reportLine.addUsage(summary);
		}

		final List<S3ReportLine> results = new ArrayList<S3ReportLine>();
		for (S3ReportLineKey lineKey: reportLineMap.keySet()) {
			results.add(reportLineMap.get(lineKey));
		}
		
		Collections.sort(results);
		return results;
	}

	
	private static String getAttributeValue(ReportingCriterion criterion,
			S3SummaryKey key)
	{
		log.debug("owner id:" + key.getOwnerId() + " account id:" + key.getAccountId());

		switch (criterion) {
			case ACCOUNT:
				return ReportingAccountDao.getInstance().getAccountName(key.getAccountId());
			case USER:
				return ReportingUserDao.getInstance().getUserName(key.getOwnerId());
			default:
				return ReportingUserDao.getInstance().getUserName(key.getOwnerId());
		}
	}
	
	@Override
	public String getJrxmlFilename()
	{
		return "s3.jrxml";
	}

	@Override
	public String getNestedJrxmlFilename()
	{
		return "nested_s3.jrxml";
	}


}

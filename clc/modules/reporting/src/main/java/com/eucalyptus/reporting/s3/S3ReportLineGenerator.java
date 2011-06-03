package com.eucalyptus.reporting.s3;

import java.util.*;

import org.mortbay.log.Log;

import com.eucalyptus.reporting.ReportLineGenerator;
import com.eucalyptus.reporting.ReportingCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.units.Units;

public class S3ReportLineGenerator
	implements ReportLineGenerator<S3ReportLine>
{
	private static S3ReportLineGenerator instance;
	
	public static S3ReportLineGenerator getInstance()
	{
		if (instance == null) {
			instance = new S3ReportLineGenerator();
		}
		return instance;
	}
	
	private S3ReportLineGenerator()
	{
		
	}
	
	public List<S3ReportLine> getReportLines(Period period,
			ReportingCriterion criterion,	Units displayUnits)
	{
		return getReportLines(period, null, criterion, displayUnits);
	}

	public List<S3ReportLine> getReportLines(Period period, ReportingCriterion groupByCrit,
			ReportingCriterion crit, Units displayUnits)
	{
		Map<S3ReportLineKey, S3ReportLine> reportLineMap =
			new HashMap<S3ReportLineKey, S3ReportLine>();
		
		S3UsageLog usageLog = S3UsageLog.getS3UsageLog();
		Map<S3SummaryKey, S3UsageSummary> usageMap = 
			usageLog.getUsageSummaryMap(period);
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
		switch (criterion) {
			case ACCOUNT:
				return key.getAccountId();
			case USER:
				return key.getOwnerId();
			default:
				return key.getOwnerId();
		}
	}
	
	
}

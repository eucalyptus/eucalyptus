package com.eucalyptus.reporting.s3;

import java.util.*;

import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.units.Units;

public class S3ReportLineGenerator
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
			GroupByCriterion criterion,	Units displayUnits)
	{
		return getReportLines(period, null, criterion, displayUnits);
	}

	public List<S3ReportLine> getReportLines(Period period, GroupByCriterion groupByCrit,
			GroupByCriterion crit, Units displayUnits)
	{
		Map<ReportLineKey, S3ReportLine> reportLineMap =
			new HashMap<ReportLineKey, S3ReportLine>();
		
		S3UsageLog usageLog = S3UsageLog.getS3UsageLog();
		Map<S3SnapshotKey, S3UsageSummary> usageMap = 
			usageLog.getUsageSummaryMap(period);
		for (S3SnapshotKey key: usageMap.keySet()) {
			String critVal = getAttributeValue(crit, key);
			String groupVal = getAttributeValue(groupByCrit, key);
			ReportLineKey lineKey = new ReportLineKey(critVal, groupVal);
			if (!reportLineMap.containsKey(lineKey)) {
				reportLineMap.put(lineKey, new S3ReportLine(critVal, groupVal,
						new S3UsageSummary(), displayUnits));
			}
			S3ReportLine reportLine = reportLineMap.get(lineKey);
			S3UsageSummary summary = usageMap.get(key);
			reportLine.addUsage(summary);
		}

		final List<S3ReportLine> results = new ArrayList<S3ReportLine>();
		for (ReportLineKey lineKey: reportLineMap.keySet()) {
			results.add(reportLineMap.get(lineKey));
		}
		
		return results;
	}

	
	private static String getAttributeValue(GroupByCriterion criterion,
			S3SnapshotKey key)
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
	
	
	private class ReportLineKey
	{
		private String critVal;
		private String groupByVal;
		
		public ReportLineKey(String critVal, String groupByVal)
		{
			super();
			this.critVal = critVal;
			this.groupByVal = groupByVal;
		}
		
		public String getCritVal()
		{
			return critVal;
		}
		
		public String getGroupByVal()
		{
			return groupByVal;
		}
		
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((critVal == null) ? 0 : critVal.hashCode());
			result = prime * result
					+ ((groupByVal == null) ? 0 : groupByVal.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReportLineKey other = (ReportLineKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (critVal == null) {
				if (other.critVal != null)
					return false;
			} else if (!critVal.equals(other.critVal))
				return false;
			if (groupByVal == null) {
				if (other.groupByVal != null)
					return false;
			} else if (!groupByVal.equals(other.groupByVal))
				return false;
			return true;
		}
		
		private S3ReportLineGenerator getOuterType()
		{
			return S3ReportLineGenerator.this;
		}
		
	}
}

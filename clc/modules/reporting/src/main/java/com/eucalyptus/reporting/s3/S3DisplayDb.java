package com.eucalyptus.reporting.s3;

import java.util.*;

import com.eucalyptus.reporting.*;
import com.eucalyptus.reporting.units.*;

public class S3DisplayDb
{
	private S3UsageLog usageLog;

	private S3DisplayDb(S3UsageLog usageLog)
	{
		this.usageLog = usageLog;
	}

	private static S3DisplayDb instance = null;

	public static S3DisplayDb getInstance()
	{
		if (instance == null) {
			instance = new S3DisplayDb(S3UsageLog.getS3UsageLog());
		}
		return instance;
	}

	public List<S3DisplayBean> search(Period period, GroupByCriterion criterion,
			Units displayUnits)
	{
		Map<String, S3UsageSummary> summaryMap =
			usageLog.scanSummarize(period, criterion);
		List<S3DisplayBean> items =
			new ArrayList<S3DisplayBean>(summaryMap.size());
		for (String key: summaryMap.keySet()) {
			items.add(new S3DisplayBean(key, null, summaryMap.get(key),
					displayUnits));
		}
		return items;
	}

	public List<S3DisplayBean> searchGroupBy(
			Period period, GroupByCriterion groupByCrit,
			GroupByCriterion crit, Units displayUnits)
	{
		Map<String, Map<String, S3UsageSummary>> summaryMap =
			usageLog.scanSummarize(period, groupByCrit, crit);
		List<S3DisplayBean> items = 
			new ArrayList<S3DisplayBean>(summaryMap.size());
		for (String outerKey: summaryMap.keySet()) {
			Map<String, S3UsageSummary> innerMap =
				summaryMap.get(outerKey);
			for (String innerKey: innerMap.keySet()) {
				items.add(new S3DisplayBean(innerKey, outerKey,
							innerMap.get(innerKey), displayUnits));
			}
		}
		return items;
	}

}

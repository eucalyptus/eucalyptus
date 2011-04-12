package com.eucalyptus.reporting.instance;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;

/**
 * <p>InstanceUsageLog is the main API for accessing usage information which
 * has been stored in the usage log.
 * 
 * <p>The data in the log is not indexed, in order to minimize write time.
 * As a result, you can't search through the log for specific instances or
 * specific data; you can only get a full dump between dates. However, there
 * exist groovy scripts to transfer the data into a data warehouse for
 * subsequent searching.
 * 
 * <p>The usage data in logs is <i>sampled</i>, meaning data is collected
 * every <i>n</i> seconds and written. As a result, some small error will
 * be introduced if the boundaries of desired periods (ie months) do not
 * exactly correspond to the boundaries of the samples. In that case, the
 * reporting mechanism will be unable to determine how much of the usage in
 * a sample belongs to which of the two periods whose boundaries it crosses,
 * so it will assign usage to one period based on a rule.
 * 
 * <p>Very recent information (within the prior five minutes, for example)
 * may not have been acquired yet, in which case, an empty period or a
 * period with incomplete information may be returned.
 * 
 * @author tom.werges
 */
@ConfigurableClass(root="instanceLog", alias="basic", description="Configuration for instance usage sampling and logging", singleton=true)
public class InstanceUsageLog
{
	private static Logger log = Logger.getLogger( InstanceUsageLog.class );

	private static InstanceUsageLog singletonInstance = null;

	private InstanceUsageLog()
	{
	}

	public static synchronized InstanceUsageLog getInstanceUsageLog()
	{
		if (singletonInstance==null) {
			singletonInstance = new InstanceUsageLog();
		}
		return singletonInstance;
	}

	/**
	 * <p>Scans through the usage log, then summarizes the data it finds.
	 * Returns a map of instance usage, keyed by the value of the criterion you
	 * specified. For example, if you provide the criterion of "user", it scans
	 * through the usage log and adds up all usage data for each user, then
	 * returns a Map of UserId (String) -> UsageSummary.
	 * 
	 * <p>This method is used for generating visual reports. For example, it
	 * could be used to generate a report of user id's in a left column with
	 * usage stats in various right-hand cols.
	 *
	 * @return A summary of all usage and instance data for a given criterion
	 *   keyed by the criterion value.
	 */
	public Map<String, InstanceUsageSummary> scanSummarize(Period period,
			GroupByCriterion criterion)
	{
		Map<String, InstanceUsageSummary> results = new HashMap<String, InstanceUsageSummary>();

		for (LogScanResult result: scanLog(period)) {
			Period resPeriod = result.getPeriod();
			UsageData usageData = result.getUsageData();
			String attrValue = getAttributeValue(criterion,
					result.getInstanceAttributes());
			InstanceUsageSummary summary;
			if (results.containsKey(attrValue)) {
				summary = results.get(attrValue);
			} else {
				summary = new InstanceUsageSummary();
				results.put(attrValue, summary);
			}
			String insType = result.getInstanceAttributes().getInstanceType();
			summary.sumFromPeriodType(resPeriod, insType);
			summary.sumFromUsageData(usageData);
		}
		
		return results;
	}
	

	/**
	 * <p>Scans through the usage log, then summarizes and groups the data it finds.
	 * For example, scans through the usage log and adds up all usage data
	 * for each user, within each Availability Zone, then returns the results
	 * as <pre>AvailZoneId->UserId->UsageSummary</pre>.
	 * 
	 * <p>This is used to generate nested reports in which we list (for example)
	 * usage by user, grouped by availability zone. 
	 *
	 * @return A summary of all usage and instance data for given criteria
	 *   keyed by the criterion values. 
	 */
	public Map<String, Map<String, InstanceUsageSummary>> scanSummarize(Period period,
			GroupByCriterion outerCriterion, GroupByCriterion innerCriterion)
	{
		Map<String, Map<String, InstanceUsageSummary>> results =
			new HashMap<String, Map<String, InstanceUsageSummary>>();

		for (LogScanResult result: scanLog(period)) {
			Period resPeriod = result.getPeriod();
			UsageData usageData = result.getUsageData();
			String outerAttrValue = getAttributeValue(outerCriterion,
					result.getInstanceAttributes());
			Map<String, InstanceUsageSummary> innerMap;
			if (results.containsKey(outerAttrValue)) {
				innerMap = results.get(outerAttrValue);
			} else {
				innerMap = new HashMap<String, InstanceUsageSummary>();
				results.put(outerAttrValue, innerMap);
			}
			String innerAttrValue = getAttributeValue(innerCriterion,
					result.getInstanceAttributes());
			InstanceUsageSummary summary;
			if (innerMap.containsKey(innerAttrValue)) {
				summary = innerMap.get(innerAttrValue);
			} else {
				summary = new InstanceUsageSummary();
				innerMap.put(innerAttrValue, summary);
			}
			String insType = result.getInstanceAttributes().getInstanceType();
			summary.sumFromPeriodType(resPeriod, insType);
			summary.sumFromUsageData(usageData);
		}
		
		return results;
	}

	private static String getAttributeValue(GroupByCriterion criterion, InstanceAttributes insAttrs)
	{
		switch (criterion) {
			case ACCOUNT:
				return insAttrs.getAccountId();
			case USER:
				return insAttrs.getUserId();
			case CLUSTER:
				return insAttrs.getClusterName();
			case AVAILABILITY_ZONE:
				return insAttrs.getAvailabilityZone();
			default:
				return insAttrs.getUserId();
		}
	}

	/**
	 * <p>LogScanResult is conceptually similar to a ResultSet. It contains the
	 * instance data and usage data for a particular period.
	 * 
	 * @author tom.werges
	 */
	public class LogScanResult
	{
		private final InstanceAttributes insAttrs;
		private final Period period;
		private final UsageData usageData;
		
		public LogScanResult(InstanceAttributes insAttrs, Period period,
				UsageData usageData)
		{
			this.insAttrs = insAttrs;
			this.period = period;
			this.usageData = usageData;
		}

		public InstanceAttributes getInstanceAttributes()
		{
			return insAttrs;
		}
		
		public Period getPeriod()
		{
			return period;
		}
		
		public UsageData getUsageData()
		{
			return usageData;
		}
	}

	/**
	 * <p>Scans log for all results during some period. This method is used
	 * internally by the scanSummarize methods.
	 */
	public List<LogScanResult> scanLog(Period period)
	{
		Map<String, InstanceData> instanceDataMap =
			new HashMap<String, InstanceData>();
		EntityWrapper<InstanceAttributes> entityWrapper = EntityWrapper.get(InstanceAttributes.class);
		try {
			/* TODO: Hibernate's iterate is stupid and doesn't do what you'd want.
			 * It executes one query per row! Replace with something more
			 * efficient.
			 */
			@SuppressWarnings("rawtypes")
			Iterator iter = entityWrapper.createQuery(
				"from InstanceAttributes as ia, InstanceUsageSnapshot as ius"
				+ " where ia.uuid = ius.uuid"
				+ " and ius.timestampMs > ?"
				+ " and ius.timestampMs < ?")
				.setLong(0, period.getBeginningMs())
				.setLong(1, period.getEndingMs())
				.iterate();

			/* Gather instance attributes, and earliest and latest timestamp, and
			 * earliest and latest usage stats for each instance.
			 */
			while (iter.hasNext()) {
				Object[] row = (Object[]) iter.next();
				InstanceAttributes insAttrs = (InstanceAttributes) row[0];
				InstanceUsageSnapshot snapshot = (InstanceUsageSnapshot) row[1];
				UsageData usageData = new UsageData(
					snapshot.getCumulativeNetworkIoMegs(),
					snapshot.getCumulativeDiskIoMegs());
				String uuid = insAttrs.getUuid();
				if (instanceDataMap.containsKey(uuid)) {
					InstanceData insData = instanceDataMap.get(uuid);
					long timestamp = snapshot.getTimestampMs().longValue();
					if (insData.getBeginMs() > timestamp) {
						insData.setBeginMs(timestamp);
						insData.setEarliestUsageData(usageData);
					}
					if (insData.getEndMs() < timestamp) {
						insData.setEndMs(timestamp);
						insData.setLatestUsageData(usageData);
					}
				} else {
					InstanceData insData = new InstanceData(insAttrs,
							snapshot.getTimestampMs(), snapshot.getTimestampMs(),
							usageData, usageData);
					instanceDataMap.put(uuid, insData);
				}
			}
			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
		
		/* Generate results by subtracting earliest, latest usage stats for each
		 * instance and converting data structure
		 */
		final List<LogScanResult> results =
			new ArrayList<LogScanResult>(instanceDataMap.keySet().size());
		for (InstanceData insData: instanceDataMap.values()) {
			//System.out.println("-> InstanceData:" + insData);
			Period resultPeriod = new Period(insData.getBeginMs(), insData.getEndMs());
			UsageData resultUsage =
				insData.getEarliestUsageData().subtractFrom(insData.getLatestUsageData());
			LogScanResult newResult = new LogScanResult(insData.getInsAttrs(),
					resultPeriod, resultUsage);
			results.add(newResult);
		}
		return results;
	}
	
	/**
	 * Permanently purges data older than a certain timestamp from the log. 
	 */
	public void purgeLog(long earlierThanMs)
	{
		log.info(String.format("purge earlierThan:%d ", earlierThanMs));

		EntityWrapper<InstanceAttributes> entityWrapper = EntityWrapper.get(InstanceAttributes.class);
		try {
			
			/* Delete older instance snapshots
			 */
			entityWrapper.createSQLQuery("DELETE FROM instance_usage_snapshot WHERE timestamp_ms < ?")
				.setLong(0, new Long(earlierThanMs))
				.executeUpdate();

			/* Delete all reporting instances which no longer have even a
			 * a single corresponding instance usage snapshot, using
			 * MySQL's fancy multi-table delete with left outer join syntax.
			 */
			entityWrapper.createSQLQuery(
					"DELETE reporting_instance" 
					+ " FROM reporting_instance"
					+ " LEFT OUTER JOIN instance_usage_snapshot"
					+ " ON reporting_instance.uuid = instance_usage_snapshot.uuid"
					+ " WHERE instance_usage_snapshot.uuid IS NULL")
				.executeUpdate();
			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
	}

	
	private class InstanceData
	{
		private InstanceAttributes insAttrs;
		private long beginMs;
		private long endMs;
		private UsageData earliestUsageData;
		private UsageData latestUsageData;
		
		public InstanceData(InstanceAttributes insAttrs, long beginMs,
				long endMs, UsageData earliestUsageData,
				UsageData latestUsageData)
		{
			this.insAttrs = insAttrs;
			this.beginMs = beginMs;
			this.endMs = endMs;
			this.earliestUsageData = earliestUsageData;
			this.latestUsageData = latestUsageData;
		}

		public InstanceAttributes getInsAttrs()
		{
			return insAttrs;
		}
		
		public long getBeginMs()
		{
			return beginMs;
		}
		
		public void setBeginMs(long beginMs)
		{
			this.beginMs = beginMs;
		}
		
		public long getEndMs()
		{
			return endMs;
		}
		
		public void setEndMs(long endMs)
		{
			this.endMs = endMs;
		}
		
		public UsageData getEarliestUsageData()
		{
			return earliestUsageData;
		}
		
		public void setEarliestUsageData(UsageData earliestUsageData)
		{
			this.earliestUsageData = earliestUsageData;
		}
		
		public UsageData getLatestUsageData()
		{
			return latestUsageData;
		}
		
		public void setLatestUsageData(UsageData latestUsageData)
		{
			this.latestUsageData = latestUsageData;
		}

		/**
		 * toString() for logs and debugging
		 */
		public String toString()
		{
			return String.format("[uuid:%s,time:%d-%d,earlyUsage:%s,lateUsage:%s]",
					insAttrs.getUuid(), beginMs, endMs, earliestUsageData,
					latestUsageData);
		}
	}

}

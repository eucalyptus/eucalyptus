package com.eucalyptus.reporting.instance;

import org.hibernate.*;
import org.apache.log4j.*;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.entities.EntityWrapper;

import java.util.*;

/**
 * <p>InstanceUsageLog is the main API for accessing usage information which
 * has been stored in the usage log.
 * 
 * <p>The data in the log is not indexed, in order to minimize write time.
 * As a result, you can't search through the log for specific instances or
 * specific data; you can only get a full dump between dates. However, there
 * exist groovy scripts to filter the data, or to insert the data into a
 * data warehouse for subsequent searching.
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

	public enum GroupByCriterion
	{
		USER, ACCOUNT, CLUSTER, AVAILABILITY_ZONE;
	}


	/**
	 * <p>Scans through the usage log and summarizes and groups the data it finds.
	 * For example, scans through the usage log and adds up all usage data
	 * for each user, then returns a Map of UserId:UsageSummary. 
	 *
	 * @return A summary of all usage and instance data for a given criterion
	 *   keyed by the criterion value. For example, a summary of all instance
	 *   and usage data for each user, keyed by user. 
	 */
	public Map<String, UsageSummary> scanSummarize(Period period,
			GroupByCriterion criterion)
	{
		Map<String, UsageSummary> results = new HashMap<String, UsageSummary>();

		for (LogScanResult result: scanLog(period)) {
			Period resPeriod = result.getPeriod();
			UsageData usageData = result.getUsageData();
			String attrValue = getAttributeValue(criterion,
					result.getInstanceAttributes());
			UsageSummary summary;
			if (results.containsKey(attrValue)) {
				summary = results.get(attrValue);
			} else {
				summary = new UsageSummary();
				results.put(attrValue, summary);
			}
			String insType = result.getInstanceAttributes().getInstanceType();
			summary.sumFromPeriodType(resPeriod, insType);
			summary.sumFromUsageData(usageData);
		}
		
		return results;
	}
	

	/**
	 * <p>Scans through the usage log, and summarizes and groups the data it finds.
	 * For example, scans through the usage log and adds up all usage data
	 * for each user, within each Availability Zone, then returns the results
	 * as <pre>AvailZone->UserId->UsageSummary</pre>. 
	 *
	 * @return A summary of all usage and instance data for a given criterion
	 *   keyed by the criterion values. 
	 */
	public Map<String, Map<String, UsageSummary>> scanSummarize(Period period,
			GroupByCriterion outerCriterion, GroupByCriterion innerCriterion)
	{
		Map<String, Map<String, UsageSummary>> results =
			new HashMap<String, Map<String, UsageSummary>>();

		for (LogScanResult result: scanLog(period)) {
			Period resPeriod = result.getPeriod();
			UsageData usageData = result.getUsageData();
			String outerAttrValue = getAttributeValue(outerCriterion,
					result.getInstanceAttributes());
			Map<String, UsageSummary> innerMap;
			if (results.containsKey(outerAttrValue)) {
				innerMap = results.get(outerAttrValue);
			} else {
				innerMap = new HashMap<String, UsageSummary>();
				results.put(outerAttrValue, innerMap);
			}
			String innerAttrValue = getAttributeValue(innerCriterion,
					result.getInstanceAttributes());
			UsageSummary summary;
			if (innerMap.containsKey(innerAttrValue)) {
				summary = innerMap.get(innerAttrValue);
			} else {
				summary = new UsageSummary();
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
				return insAttrs.getUserId();
			case USER:
				return insAttrs.getUserId();
			case AVAILABILITY_ZONE:
				return insAttrs.getAvailabilityZone();
			case CLUSTER:
				return insAttrs.getClusterName();
			default:
				return insAttrs.getUserId();
		}
	}

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
	 * <p>Scans log for all results during some period.
	 */
	public List<LogScanResult> scanLog(Period period)
	{
		Map<String, InstanceData> instanceDataMap =
			new HashMap<String, InstanceData>();
		EntityWrapper<InstanceAttributes> entityWrapper = EntityWrapper.get(InstanceAttributes.class);
		Session sess = null;
		try {
			sess = entityWrapper.getSession();
			@SuppressWarnings("rawtypes")
			Iterator iter = sess.createQuery(
				"from InstanceAttributes, InstanceUsageSnapshot"
				+ " where InstanceAttributes.uuid = InstanceUsageSnapshot.uuid"
				+ " and InstanceUsageSnapshot.timestampMs > ?"
				+ " and InstanceUsageSnapshot.timestampMs < ?")
				.setLong(0, period.getBeginningMs())
				.setLong(1, period.getEndingMs())
				.iterate();

			/* Gather instance attributes, earliest and latest timestamp, earliest
			 * and latest usage stats for each instance.
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
		Session sess = null;
		try {
			
			/* Delete older instance snapshots
			 */
			sess = entityWrapper.getSession();
			sess.createSQLQuery("DELETE FROM instance_usage_snapshot WHERE timestamp_ms < ?")
				.setLong(0, new Long(earlierThanMs))
				.executeUpdate();

			/* Delete all reporting instances which no longer have even a
			 * a single corresponding instance usage snapshot, using
			 * MySQL's fancy multi-table delete with left outer join syntax.
			 */
			sess.createSQLQuery(
					"DELETE instance_attributes" 
					+ " FROM instance_attributes"
					+ " LEFT OUTER JOIN instance_usage_snapshot"
					+ " ON instance_attributes.uuid = instance_usage_snapshot.uuid"
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
		
	}

}

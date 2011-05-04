package com.eucalyptus.reporting.s3;

import java.util.*;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.instance.InstanceAttributes;

/**
 * <p>S3UsageLog is the main API for accessing storage usage information.
 * 
 * @author tom.werges
 */
public class S3UsageLog
{
	private static Logger log = Logger.getLogger( S3UsageLog.class );

	private static S3UsageLog instance;
	
	private S3UsageLog()
	{
	}
	
	public static S3UsageLog getS3UsageLog()
	{
		if (instance == null) {
			instance = new S3UsageLog();
		}
		return instance;
	}

	public Iterator<S3UsageSnapshot> scanLog(Period period)
	{
		EntityWrapper<InstanceAttributes> entityWrapper =
			EntityWrapper.get( InstanceAttributes.class );
		try {
			/* TODO: Hibernate's iterate is stupid and doesn't do what you'd want.
			 * It executes one query per row! Replace with something more
			 * efficient.
			 */
			@SuppressWarnings("rawtypes")
			Iterator iter = entityWrapper.createQuery(
				"from S3UsageSnapshot as sus"
				+ " where sus.key.timestampMs > ?"
				+ " and sus.key.timestampMs < ?"
				+ " order by sus.key.timestampMs")
				.setLong(0, period.getBeginningMs())
				.setLong(1, period.getEndingMs())
				.iterate();

			return new S3UsageSnapshotIterator(iter);
		} catch (Exception ex) {
			log.error(ex);
			throw new RuntimeException(ex);
		}

	}
	
	private class S3UsageSnapshotIterator
			implements Iterator<S3UsageSnapshot>
	{
		private final Iterator resultSetIter;

		S3UsageSnapshotIterator(Iterator resultSetIter)
		{
			this.resultSetIter = resultSetIter;
		}

		@Override
		public boolean hasNext()
		{
			return resultSetIter.hasNext();
		}

		@Override
		public S3UsageSnapshot next()
		{
			return (S3UsageSnapshot) resultSetIter.next();
		}

		@Override
		public void remove()
		{
			resultSetIter.remove();
		}

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

	private class SummaryInfo
	{
		private long lastTimestamp;
		private S3UsageSummary summary;
		private S3UsageData lastData;
		
		SummaryInfo(long lastTimestamp, S3UsageSummary summary,
				S3UsageData lastData)
		{
			this.lastTimestamp = lastTimestamp;
			this.summary = summary;
			this.lastData = lastData;
		}

		long getLastTimestamp()
		{
			return lastTimestamp;
		}

		void setLastTimestamp(long lastTimestamp)
		{
			this.lastTimestamp = lastTimestamp;
		}

		S3UsageSummary getSummary()
		{
			return summary;
		}

		S3UsageData getLastData()
		{
			return lastData;
		}

		void setLastData(S3UsageData lastData)
		{
			this.lastData = lastData;
		}

	}

	public Map<String, S3UsageSummary> scanSummarize(Period period,
			GroupByCriterion criterion)
	{
		final Map<String, SummaryInfo> infoMap =
			new HashMap<String, SummaryInfo>();
		
		Iterator<S3UsageSnapshot> iter = scanLog(period);
		while (iter.hasNext()) {
			S3UsageSnapshot snapshot = iter.next();
			long timestampMs = snapshot.getSnapshotKey().getTimestampMs().longValue();
			String critVal = getAttributeValue(criterion, snapshot.getSnapshotKey());
			if (infoMap.containsKey(critVal)) {
				SummaryInfo info = infoMap.get(critVal);
				S3UsageData lastData = info.getLastData();
				long durationSecs = (timestampMs - info.getLastTimestamp()) / 1000;
				info.getSummary().updateValues(lastData.getObjectsMegs(), lastData.getBucketsNum(),
						durationSecs);
				info.setLastTimestamp(timestampMs);
				info.setLastData(snapshot.getUsageData());
			} else {
				SummaryInfo info = new SummaryInfo(timestampMs,
						new S3UsageSummary(), snapshot.getUsageData());
				infoMap.put(critVal, info);
			}
		}

		return convertOneCriterion(infoMap);
	}

	/**
	 * <p>Convert Map<String,SummaryInfo> to Map<String, S3UsageSummary>. 
	 */
	private static Map<String, S3UsageSummary> convertOneCriterion(
			Map<String, SummaryInfo> infoMap)
	{
		final Map<String, S3UsageSummary> resultMap =
			new HashMap<String, S3UsageSummary>();
		for (String key: infoMap.keySet()) {
			resultMap.put(key, infoMap.get(key).getSummary());
		}
		return resultMap;
	}

	/**
	 * <p>Convert Map<String, Map<String, SummaryInfo>> to
	 *  Map<String, Map<String, S3UsageSummary>>.
	 */
	private static Map<String, Map<String, S3UsageSummary>> convertTwoCriteria(
			Map<String, Map<String, SummaryInfo>> infoMap)
	{
		final Map<String, Map<String, S3UsageSummary>> results =
			new HashMap<String, Map<String, S3UsageSummary>>();
		for (String key: infoMap.keySet()) {
			results.put(key, convertOneCriterion(infoMap.get(key)));
		}
		return results;
	}
	
	public Map<String, Map<String, S3UsageSummary>> scanSummarize(
			Period period, GroupByCriterion outerCriterion,
			GroupByCriterion innerCriterion)
	{
		final Map<String, Map<String, SummaryInfo>> infoMap =
			new HashMap<String, Map<String, SummaryInfo>>();

		Iterator<S3UsageSnapshot> iter = scanLog(period);
		while (iter.hasNext()) {
			S3UsageSnapshot snapshot = iter.next();
			long timestampMs = snapshot.getSnapshotKey().getTimestampMs().longValue();
			String outerCritVal = getAttributeValue(outerCriterion,
					snapshot.getSnapshotKey());
			Map<String, SummaryInfo> innerMap = null;
			if (infoMap.containsKey(outerCritVal)) {
				innerMap = infoMap.get(outerCritVal);
			} else {
				innerMap = new HashMap<String, SummaryInfo>();
				infoMap.put(outerCritVal, innerMap);
			}
			String innerCritVal = getAttributeValue(innerCriterion, snapshot.getSnapshotKey());
			if (innerMap.containsKey(innerCritVal)) {
				SummaryInfo info = innerMap.get(innerCritVal);
				S3UsageData lastData = info.getLastData();
				long durationSecs = (timestampMs - info.getLastTimestamp()) / 1000;
				System.out.println("info:" + info + " summary:" + info.getSummary() + " lastData:" + lastData);
				info.getSummary().updateValues(lastData.getObjectsMegs(),
						lastData.getBucketsNum(), durationSecs);
				info.setLastTimestamp(timestampMs);
				info.setLastData(snapshot.getUsageData());
			} else {
				SummaryInfo info = new SummaryInfo(timestampMs,
						new S3UsageSummary(), snapshot.getUsageData());
				innerMap.put(innerCritVal, info);
			}
		}

		return convertTwoCriteria(infoMap);
	}

	public void purgeLog(long earlierThanMs)
	{
		log.info(String.format("purge earlierThan:%d ", earlierThanMs));

		EntityWrapper<S3UsageSnapshot> entityWrapper =
			EntityWrapper.get(S3UsageSnapshot.class);
		Session sess = null;
		try {
			
			/* Delete older instance snapshots
			 */
			entityWrapper.createSQLQuery("DELETE FROM s3_usage_snapshot "
				+ "WHERE timestamp_ms < ?")
				.setLong(0, new Long(earlierThanMs))
				.executeUpdate();

			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
	}

}

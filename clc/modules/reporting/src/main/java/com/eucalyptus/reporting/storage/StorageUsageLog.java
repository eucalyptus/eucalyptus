package com.eucalyptus.reporting.storage;

import java.util.*;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.instance.InstanceAttributes;

/**
 * <p>StorageUsageLog
 * 
 * @author twerges
 */
public class StorageUsageLog
{
	private static Logger log = Logger.getLogger( StorageUsageLog.class );

	private static StorageUsageLog instance;
	
	private StorageUsageLog()
	{
	}
	
	public static StorageUsageLog getStorageUsageLog()
	{
		if (instance == null) {
			instance = new StorageUsageLog();
		}
		return instance;
	}

	public Iterator<StorageUsageSnapshot> scanLog(Period period)
	{
		EntityWrapper<InstanceAttributes> entityWrapper =
			EntityWrapper.get( InstanceAttributes.class );
		Session sess = null;
		try {
			sess = entityWrapper.getSession();
			@SuppressWarnings("rawtypes")
			Iterator iter = sess.createQuery(
				"from StorageUsageSnapshot as sus"
				+ " where sus.timestampMs > ?"
				+ " and sus.timestampMs < ?"
				+ " order by sus.timestampMs")
				.setLong(0, period.getBeginningMs())
				.setLong(1, period.getEndingMs())
				.iterate();

			return new StorageUsageSnapshotIterator(iter);
		} catch (Exception ex) {
			log.error(ex);
			throw new RuntimeException(ex);
		}

	}
	
	private class StorageUsageSnapshotIterator
			implements Iterator<StorageUsageSnapshot>
	{
		private final Iterator resultSetIter;

		StorageUsageSnapshotIterator(Iterator resultSetIter)
		{
			this.resultSetIter = resultSetIter;
		}

		@Override
		public boolean hasNext()
		{
			return resultSetIter.hasNext();
		}

		@Override
		public StorageUsageSnapshot next()
		{
			Object[] row = (Object[]) resultSetIter.next();
			return (StorageUsageSnapshot) row[0];
		}

		@Override
		public void remove()
		{
			resultSetIter.remove();
		}

	}

	private static String getAttributeValue(GroupByCriterion criterion,
			SnapshotKey key)
	{
		switch (criterion) {
			case ACCOUNT:
				return key.getAccountId();
			case USER:
				return key.getOwnerId();
			case CLUSTER:
				return key.getClusterName();
			case AVAILABILITY_ZONE:
				return key.getAvailabilityZone();
			default:
				return key.getOwnerId();
		}
	}

	private class SummaryInfo
	{
		private long lastTimestamp;
		private StorageUsageSummary summary;
		private StorageUsageData lastData;
		
		SummaryInfo(long lastTimestamp, StorageUsageSummary summary,
				StorageUsageData lastData)
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

		StorageUsageSummary getSummary()
		{
			return summary;
		}

		StorageUsageData getLastData()
		{
			return lastData;
		}

		void setLastData(StorageUsageData lastData)
		{
			this.lastData = lastData;
		}

	}

	public Map<String, StorageUsageSummary> scanSummarize(Period period,
			GroupByCriterion criterion)
	{
		final Map<String, SummaryInfo> infoMap =
			new HashMap<String, SummaryInfo>();
		
		Iterator<StorageUsageSnapshot> iter = scanLog(period);
		while (iter.hasNext()) {
			StorageUsageSnapshot snapshot = iter.next();
			long timestampMs = snapshot.getSnapshotKey().getTimestampMs().longValue();
			String critVal = getAttributeValue(criterion, snapshot.getSnapshotKey());
			SummaryInfo info = null;
			if (infoMap.containsKey(critVal)) {
				info = infoMap.get(critVal);
				long durationMs = timestampMs - info.getLastTimestamp();
				info.getSummary().addUsage(info.getLastData(), durationMs);
				info.setLastTimestamp(timestampMs);
				info.setLastData(snapshot.getUsageData());
			} else {
				info = new SummaryInfo(timestampMs, new StorageUsageSummary(),
						snapshot.getUsageData());
				infoMap.put(critVal, info);
			}
		}

		/* Convert summary to result type
		 */
		final Map<String, StorageUsageSummary> resultMap =
			new HashMap<String, StorageUsageSummary>();
		for (String key: infoMap.keySet()) {
			resultMap.put(key, infoMap.get(key).getSummary());
		}
		return resultMap;
	}

	public Map<String, Map<String, StorageUsageSummary>> scanSummarize(
			Period period, GroupByCriterion outerCriterion,
			GroupByCriterion innerCriterion)
	{
		final Map<String, Map<String, StorageUsageSummary>> results =
			new HashMap<String, Map<String, StorageUsageSummary>>();

		Iterator<StorageUsageSnapshot> iter = scanLog(period);
		while (iter.hasNext()) {
			StorageUsageSnapshot snapshot = iter.next();
			String outerCritVal = getAttributeValue(outerCriterion,
					snapshot.getSnapshotKey());
			Map<String, StorageUsageData> innerMap = null;
			if (results.containsKey(outerCritVal)) {
				//innerMap = results.get(outerCritVal);
			} else {
				innerMap = new HashMap<String, StorageUsageData>();
				//results.put(outerCritVal, innerMap);
			}
			String innerCritVal = getAttributeValue(innerCriterion,
					snapshot.getSnapshotKey());
			StorageUsageData data = null;
			if (innerMap.containsKey(innerCritVal)) {
				data = innerMap.get(innerCritVal);
			} else {
				data = new StorageUsageData();
				innerMap.put(innerCritVal, data);
			}
			data.sumFrom(snapshot.getUsageData());
		}

		return results;
	}
	
	public void purgeLog(long earlierThanMs)
	{
		log.info(String.format("purge earlierThan:%d ", earlierThanMs));

		EntityWrapper<StorageUsageSnapshot> entityWrapper =
			EntityWrapper.get(StorageUsageSnapshot.class);
		Session sess = null;
		try {
			
			/* Delete older instance snapshots
			 */
			sess = entityWrapper.getSession();
			sess.createSQLQuery("DELETE FROM storage_usage_snapshot "
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

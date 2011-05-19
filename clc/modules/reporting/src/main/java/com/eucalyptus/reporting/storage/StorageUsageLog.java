package com.eucalyptus.reporting.storage;

import java.util.*;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.instance.InstanceAttributes;

/**
 * <p>StorageUsageLog is the main API for accessing storage usage information.
 * 
 * @author tom.werges
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
		try {
			/* TODO: Hibernate's iterate is stupid and doesn't do what you'd want.
			 * It executes one query per row! Replace with something more
			 * efficient.
			 */
			@SuppressWarnings("rawtypes")
			Iterator iter = entityWrapper.createQuery(
				"from StorageUsageSnapshot as sus"
				+ " where sus.key.timestampMs > ?"
				+ " and sus.key.timestampMs < ?"
				+ " order by sus.key.timestampMs")
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
			return (StorageUsageSnapshot) resultSetIter.next();
		}

		@Override
		public void remove()
		{
			resultSetIter.remove();
		}

	}

	private static String getAttributeValue(GroupByCriterion criterion,
			StorageSnapshotKey key)
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
			if (infoMap.containsKey(critVal)) {
				SummaryInfo info = infoMap.get(critVal);
				StorageUsageData lastData = info.getLastData();
				long durationSecs = (timestampMs - info.getLastTimestamp()) / 1000;
				info.getSummary().updateValues(lastData.getVolumesMegs(),
						lastData.getSnapshotsMegs(), durationSecs);
				info.setLastTimestamp(timestampMs);
				info.setLastData(snapshot.getUsageData());
			} else {
				SummaryInfo info = new SummaryInfo(timestampMs,
						new StorageUsageSummary(), snapshot.getUsageData());
				infoMap.put(critVal, info);
			}
		}

		return convertOneCriterion(infoMap);
	}

	/**
	 * <p>Convert Map<String,SummaryInfo> to Map<String, StorageUsageSummary>. 
	 */
	private static Map<String, StorageUsageSummary> convertOneCriterion(
			Map<String, SummaryInfo> infoMap)
	{
		final Map<String, StorageUsageSummary> resultMap =
			new HashMap<String, StorageUsageSummary>();
		for (String key: infoMap.keySet()) {
			resultMap.put(key, infoMap.get(key).getSummary());
		}
		return resultMap;
	}

	/**
	 * <p>Convert Map<String, Map<String, SummaryInfo>> to
	 *  Map<String, Map<String, StorageUsageSummary>>.
	 */
	private static Map<String, Map<String, StorageUsageSummary>> convertTwoCriteria(
			Map<String, Map<String, SummaryInfo>> infoMap)
	{
		final Map<String, Map<String, StorageUsageSummary>> results =
			new HashMap<String, Map<String, StorageUsageSummary>>();
		for (String key: infoMap.keySet()) {
			results.put(key, convertOneCriterion(infoMap.get(key)));
		}
		return results;
	}
	
	public Map<String, Map<String, StorageUsageSummary>> scanSummarize(
			Period period, GroupByCriterion outerCriterion,
			GroupByCriterion innerCriterion)
	{
		final Map<String, Map<String, SummaryInfo>> infoMap =
			new HashMap<String, Map<String, SummaryInfo>>();

		Iterator<StorageUsageSnapshot> iter = scanLog(period);
		while (iter.hasNext()) {
			StorageUsageSnapshot snapshot = iter.next();
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
				StorageUsageData lastData = info.getLastData();
				long durationSecs = (timestampMs - info.getLastTimestamp()) / 1000;
				System.out.println("info:" + info + " summary:" + info.getSummary() + " lastData:" + lastData);
				info.getSummary().updateValues(lastData.getVolumesMegs(),
						lastData.getSnapshotsMegs(), durationSecs);
				info.setLastTimestamp(timestampMs);
				info.setLastData(snapshot.getUsageData());
			} else {
				SummaryInfo info = new SummaryInfo(timestampMs,
						new StorageUsageSummary(), snapshot.getUsageData());
				innerMap.put(innerCritVal, info);
			}
		}

		return convertTwoCriteria(infoMap);
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
			entityWrapper.createSQLQuery("DELETE FROM storage_usage_snapshot "
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

	
	/**
	 * <p>Find the latest allSnapshot before timestampMs, by iteratively
	 * querying before the period beginning, moving backward in exponentially
	 * growing intervals
	 */
	long findLatestAllSnapshotBefore(long timestampMs)
	{
		long foundTimestampMs = 0l;

		EntityWrapper<StorageUsageSnapshot> entityWrapper =
			EntityWrapper.get(StorageUsageSnapshot.class);
		try {

	    	final long oneHourMs = 60*60*1000;
			for ( int i=2 ;
				  (timestampMs - oneHourMs*(long)i) > 0 ;
				  i=(int)Math.pow(i, 2))
			{
				
				long startingMs = timestampMs - (oneHourMs*i);
				log.info("Searching for latest timestamp before beginning:" + startingMs);
				@SuppressWarnings("rawtypes")
				Iterator iter =
					entityWrapper.createQuery(
						"from StorageUsageSnapshot as sus"
						+ " WHERE sus.key.timestampMs > ?"
						+ " AND sus.key.timestampMs < ?"
						+ " AND sus.key.allSnapshot = true")
						.setLong(0, new Long(startingMs))
						.setLong(1, new Long(timestampMs))
						.iterate();
				while (iter.hasNext()) {
					StorageUsageSnapshot snapshot = (StorageUsageSnapshot) iter.next();
					foundTimestampMs = snapshot.getSnapshotKey().getTimestampMs();
				}
				if (foundTimestampMs != 0l) break;
			}
			log.info("Found latest timestamp before beginning:"
					+ foundTimestampMs);			
			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
		
		return foundTimestampMs;
	}

	
	Map<StorageSummaryKey, StorageUsageData> findLatestUsageData()
	{
    	log.info("LoadLastUsageData");
    	final Map<StorageSummaryKey, StorageUsageData> usageMap =
    		new HashMap<StorageSummaryKey, StorageUsageData>();
    	
    	EntityWrapper<StorageUsageSnapshot> entityWrapper =
			EntityWrapper.get(StorageUsageSnapshot.class);

    	try {
			long latestSnapshotBeforeMs =
				findLatestAllSnapshotBefore(System.currentTimeMillis());
			@SuppressWarnings("rawtypes")

			Iterator iter = entityWrapper.createQuery(
					"from StorageUsageSnapshot as sus"
					+ " WHERE sus.key.timestampMs > ?")
					.setLong(0, new Long(latestSnapshotBeforeMs))
					.iterate();
			
			while (iter.hasNext()) {
				
				StorageUsageSnapshot snapshot = (StorageUsageSnapshot) iter.next();
				StorageSnapshotKey snapshotKey = snapshot.getSnapshotKey();
				StorageSummaryKey summaryKey = new StorageSummaryKey(snapshotKey);

				usageMap.put(summaryKey, snapshot.getUsageData());
			}
    		
			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
		return usageMap;
	}

	
	/**
	 * <p>Gather a Map of all Storage resource usage for a period.
	 */
    public Map<StorageSummaryKey, StorageUsageSummary> getUsageSummaryMap(Period period)
    {
    	log.info("GetUsageSummaryMap period:" + period);
    	final Map<StorageSummaryKey, StorageUsageSummary> usageMap =
    		new HashMap<StorageSummaryKey, StorageUsageSummary>();

    	EntityWrapper<StorageUsageSnapshot> entityWrapper =
			EntityWrapper.get(StorageUsageSnapshot.class);
		try {


			/* Start query from last snapshot before report beginning, iterate
			 * through the data, and accumulate all reporting info through the
			 * report end. We will accumulate a fraction of a snapshot at the
			 * beginning and end, since report boundaries will not likely
			 * coincide with sampling period boundaries.
			 */
			Map<StorageSummaryKey,StorageDataAccumulator> dataAccumulatorMap =
				new HashMap<StorageSummaryKey,StorageDataAccumulator>();
			
			long latestSnapshotBeforeMs =
				findLatestAllSnapshotBefore(period.getBeginningMs());

			@SuppressWarnings("rawtypes")
			Iterator iter = entityWrapper.createQuery(
					"from StorageUsageSnapshot as sus"
					+ " WHERE sus.key.timestampMs > ?"
					+ " AND sus.key.timestampMs < ?")
					.setLong(0, new Long(latestSnapshotBeforeMs))
					.setLong(1, new Long(period.getEndingMs()))
					.iterate();
			
			while (iter.hasNext()) {
				
				StorageUsageSnapshot snapshot = (StorageUsageSnapshot) iter.next();
				StorageSnapshotKey snapshotKey = snapshot.getSnapshotKey();
				StorageSummaryKey summaryKey = new StorageSummaryKey(snapshotKey);

				if ( snapshotKey.getTimestampMs() < period.getBeginningMs()
					 || !dataAccumulatorMap.containsKey(summaryKey) ) {

					//new accumulator, discard earlier accumulators from before report beginning
					StorageDataAccumulator accumulator =
						new StorageDataAccumulator(snapshotKey.getTimestampMs(),
								snapshot.getUsageData(), new StorageUsageSummary());
					dataAccumulatorMap.put(summaryKey, accumulator);

				} else {
					
					/* Within interval; accumulate resource usage by adding
					 * to accumulator, for this key.
					 */
					StorageDataAccumulator accumulator =	dataAccumulatorMap.get( summaryKey );

					/* Extrapolate fractional usage for snapshots which occurred
					 * before report beginning.
					 */
					long beginningMs = Math.max( period.getBeginningMs(),
							accumulator.getLastTimestamp() );
					//query above specifies timestamp is before report end
					long endingMs = snapshotKey.getTimestampMs()-1;
					long durationSecs = (endingMs - beginningMs) / 1000;

					accumulator.accumulateUsage( durationSecs );		
					accumulator.setLastTimestamp(snapshotKey.getTimestampMs());
					accumulator.setLastUsageData(snapshot.getUsageData());
					log.info(String.format("Accumulate usage, %d-%d, key:%s",
							beginningMs, endingMs, summaryKey));

				}

			}

			/* Accumulate fractional data usage at end of reporting period.
			 */
			for ( StorageSummaryKey key: dataAccumulatorMap.keySet() ) {
				
				StorageDataAccumulator accumulator =	dataAccumulatorMap.get( key );
				long beginningMs = Math.max( period.getBeginningMs(),
						accumulator.getLastTimestamp() );
				long endingMs = period.getEndingMs() - 1;
				long durationSecs = ( endingMs-beginningMs ) / 1000;
				accumulator.accumulateUsage( durationSecs );
				log.info(String.format("Accumulate endUsage, %d-%d, key:%s",
						beginningMs, endingMs, key));

				//add to results
				usageMap.put( key, accumulator.getCurrentSummary() );
			}

			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}

        return usageMap;
    }

	
    private class StorageDataAccumulator
    {
    	private Long lastTimestamp;
    	private StorageUsageData lastUsageData;
    	private StorageUsageSummary currentSummary;
    	
    	public StorageDataAccumulator(Long lastTimestamp,
				StorageUsageData lastUsageData, StorageUsageSummary currentSummary)
		{
			super();
			this.lastTimestamp = lastTimestamp;
			this.lastUsageData = lastUsageData;
			this.currentSummary = currentSummary;
		}

		public Long getLastTimestamp()
		{
			return lastTimestamp;
		}
		
    	public void setLastTimestamp(Long lastTimestamp)
		{
			this.lastTimestamp = lastTimestamp;
		}
		
		public void setLastUsageData(StorageUsageData lastUsageData)
		{
			this.lastUsageData = lastUsageData;
		}

		public StorageUsageSummary getCurrentSummary()
		{
			return currentSummary;
		}
		
		/**
		 * <p>Accumulate usage. We've been holding on to the last usage data
		 * seen. Now we know how long that usage data prevails. Add the usage
		 * to the summary. 
		 */
    	public void accumulateUsage( long durationSecs )
    	{
    		
    		if (lastUsageData != null) {
    			
				currentSummary.addVolumesMegsSecs(
						lastUsageData.getVolumesMegs() * durationSecs);

				currentSummary.addSnapshotsMegsSecs(
						lastUsageData.getSnapshotsMegs() * durationSecs);

				currentSummary.setVolumesMegsMax(
						Math.max(currentSummary.getVolumesMegsMax(),
								 lastUsageData.getVolumesMegs()));
				currentSummary.setSnapshotsMegsMax(
						Math.max(currentSummary.getSnapshotsMegsMax(),
								lastUsageData.getSnapshotsMegs()));

				log.info("Accumulate "
						+ (lastUsageData.getVolumesMegs() * durationSecs)
						+ " " + (lastUsageData.getSnapshotsMegs() * durationSecs));
				this.lastUsageData = null;

    		}    		
    	}
    	
    }

	
}

package com.eucalyptus.reporting.modules.storage;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.Period;

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

	public void purgeLog(long earlierThanMs)
	{
		log.info(String.format("purge earlierThan:%d ", earlierThanMs));

		EntityWrapper<StorageUsageSnapshot> entityWrapper =
			EntityWrapper.get(StorageUsageSnapshot.class);
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
	 * <p>Find the latest allSnapshot before timestampMs, or null if none
	 */
	Long findLatestAllSnapshotBefore(long timestampMs)
	{
		Long foundTimestampMs = null;

				
		/* Iteratively query before startingMs, moving backward in
		 * exponentially growing intervals, starting at 3 hrs before
		 */
        for (double minsBefore=180; /* 3 hrs */
        	 System.currentTimeMillis()-(long)(minsBefore*60*1000) > 0;
        	 minsBefore=Math.pow(minsBefore, 1.1))
        {
            long queryStartMs = System.currentTimeMillis()-(long)(minsBefore*60*1000);

            EntityWrapper<StorageUsageSnapshot> entityWrapper =
    			EntityWrapper.get(StorageUsageSnapshot.class);

			log.info("Searching for latest timestamp before beginning:" + queryStartMs);
			try {
				@SuppressWarnings("rawtypes")
				List list = entityWrapper.createQuery(
							"from StorageUsageSnapshot as sus"
							+ " WHERE sus.key.timestampMs > ?"
							+ " AND sus.key.timestampMs < ?"
							+ " AND sus.allSnapshot = true")
							.setLong(0, new Long(queryStartMs))
							.setLong(1, new Long(timestampMs))
							.list();
				for (Object obj : list) {
					StorageUsageSnapshot snapshot = (StorageUsageSnapshot) obj;
					foundTimestampMs = snapshot.getSnapshotKey().getTimestampMs();
				}
				entityWrapper.commit();
			} catch (Exception ex) {
				log.error(ex);
				entityWrapper.rollback();
				throw new RuntimeException(ex);
			}
			if (foundTimestampMs != null) break;
		}
		log.info("Found latest timestamp before beginning:"
				+ foundTimestampMs);			
		
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
			Long latestSnapshotBeforeMs =
				findLatestAllSnapshotBefore(System.currentTimeMillis());
			@SuppressWarnings("rawtypes")

			List list = entityWrapper.createQuery(
					"from StorageUsageSnapshot as sus"
					+ " WHERE sus.key.timestampMs > ?")
					.setLong(0, (latestSnapshotBeforeMs!=null ? latestSnapshotBeforeMs : 0l))
					.list();
			
			for (Object obj: list) {
				
				StorageUsageSnapshot snapshot = (StorageUsageSnapshot) obj;
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
    public Map<StorageSummaryKey, StorageUsageSummary> getUsageSummaryMap(
    		Period period, String accountId)
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
			
			Long latestSnapshotBeforeMs =
				findLatestAllSnapshotBefore(period.getBeginningMs());

			@SuppressWarnings("rawtypes")
			List list = null;

			if (accountId == null) {
				list = entityWrapper.createQuery(
					"from StorageUsageSnapshot as sus"
					+ " WHERE sus.key.timestampMs > ?"
					+ " AND sus.key.timestampMs < ?"
					+ " ORDER BY sus.key.timestampMs")
					.setLong(0, (latestSnapshotBeforeMs!=null ? latestSnapshotBeforeMs : 0l))
					.setLong(1, new Long(period.getEndingMs()))
					.list();
			} else {
				list = entityWrapper.createQuery(
						"from StorageUsageSnapshot as sus"
						+ " WHERE sus.key.timestampMs > ?"
						+ " AND sus.key.timestampMs < ?"
						+ " AND sus.key.accountId = ?"
						+ " ORDER BY sus.key.timestampMs")
						.setLong(0, (latestSnapshotBeforeMs!=null ? latestSnapshotBeforeMs : 0l))
						.setLong(1, new Long(period.getEndingMs()))
						.setString(2, accountId)
						.list();
			}
			
			for (Object obj: list) {
				
				StorageUsageSnapshot snapshot = (StorageUsageSnapshot) obj;
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

					log.info(String.format("Accumulate usage, %d-%d, key:%s",
							beginningMs, endingMs, summaryKey));
					accumulator.accumulateUsage( durationSecs );		
					accumulator.setLastTimestamp(snapshotKey.getTimestampMs());
					accumulator.setLastUsageData(snapshot.getUsageData());

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
				log.info(String.format("Accumulate endUsage, %d-%d, key:%s",
						beginningMs, endingMs, key));
				accumulator.accumulateUsage( durationSecs );

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

				log.info(String.format("Accumulated durationSecs:%d raw:%d,%d multDurationSecs:%d,%d",durationSecs,
						lastUsageData.getVolumesMegs(), lastUsageData.getSnapshotsMegs(),
						(lastUsageData.getVolumesMegs() * durationSecs),
						(lastUsageData.getSnapshotsMegs() * durationSecs)));
				this.lastUsageData = null;

    		}    		
    	}
    	
    }


}

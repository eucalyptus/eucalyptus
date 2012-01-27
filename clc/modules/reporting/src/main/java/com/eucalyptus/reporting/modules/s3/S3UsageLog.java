package com.eucalyptus.reporting.modules.s3;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.Period;

/**
 * <p>S3UsageLog is the main API for accessing S3 usage information.
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

	public void purgeLog(long earlierThanMs)
	{
		log.info(String.format("purge earlierThan:%d ", earlierThanMs));

		EntityWrapper<S3UsageSnapshot> entityWrapper =
			EntityWrapper.get(S3UsageSnapshot.class);
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
	
	/**
	 * <p>Find the latest allSnapshot before timestampMs
	 */
	long findLatestAllSnapshotBefore(long timestampMs)
	{
		long foundTimestampMs = 0l;

		EntityWrapper<S3UsageSnapshot> entityWrapper =
			EntityWrapper.get(S3UsageSnapshot.class);
		try {

			/* Iteratively query before startingMs, moving backward in
			 * exponentially growing intervals, starting at 3 hrs before
			 */
	        for (double minsBefore=180; /* 3 hrs */
	        	 System.currentTimeMillis()-(long)(minsBefore*60*1000) > 0;
	        	 minsBefore=Math.pow(minsBefore, 1.1))
	        {
	            long queryStartMs = System.currentTimeMillis()-(long)(minsBefore*60*1000);
				
				log.info("Searching for latest timestamp before beginning:" + queryStartMs);
				@SuppressWarnings("rawtypes")
				List list =
					entityWrapper.createQuery(
						"from S3UsageSnapshot as sus"
						+ " WHERE sus.key.timestampMs > ?"
						+ " AND sus.key.timestampMs < ?"
						+ " AND sus.allSnapshot = true")
						.setLong(0, new Long(queryStartMs))
						.setLong(1, new Long(timestampMs))
						.list();
				for (Object obj: list) {
					S3UsageSnapshot snapshot = (S3UsageSnapshot) obj;
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

	Map<S3SummaryKey, S3UsageData> findLatestUsageData()
	{
    	log.info("LoadLastUsageData");
    	final Map<S3SummaryKey, S3UsageData> usageMap =
    		new HashMap<S3SummaryKey, S3UsageData>();
    	
    	EntityWrapper<S3UsageSnapshot> entityWrapper =
			EntityWrapper.get(S3UsageSnapshot.class);

    	try {
			long latestSnapshotBeforeMs =
				findLatestAllSnapshotBefore(System.currentTimeMillis());
			@SuppressWarnings("rawtypes")

			List list = entityWrapper.createQuery(
					"from S3UsageSnapshot as sus"
					+ " WHERE sus.key.timestampMs > ?")
					.setLong(0, new Long(latestSnapshotBeforeMs))
					.list();
			
			for (Object obj: list) {
				
				S3UsageSnapshot snapshot = (S3UsageSnapshot) obj;
				S3SnapshotKey snapshotKey = snapshot.getSnapshotKey();
				S3SummaryKey summaryKey = new S3SummaryKey(snapshotKey);

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
	 * <p>Gather a Map of all S3 resource usage for a period.
	 */
    public Map<S3SummaryKey, S3UsageSummary> getUsageSummaryMap(Period period,
    		String accountId)
    {
    	log.info("GetUsageSummaryMap period:" + period);
    	final Map<S3SummaryKey, S3UsageSummary> usageMap =
    		new HashMap<S3SummaryKey, S3UsageSummary>();

    	EntityWrapper<S3UsageSnapshot> entityWrapper =
			EntityWrapper.get(S3UsageSnapshot.class);
		try {


			/* Start query from last snapshot before report beginning, iterate
			 * through the data, and accumulate all reporting info through the
			 * report end. We will accumulate a fraction of a snapshot at the
			 * beginning and end, since report boundaries will not likely
			 * coincide with sampling period boundaries.
			 */
			Map<S3SummaryKey,S3DataAccumulator> dataAccumulatorMap =
				new HashMap<S3SummaryKey,S3DataAccumulator>();
			
			long latestSnapshotBeforeMs =
				findLatestAllSnapshotBefore(period.getBeginningMs());

			@SuppressWarnings("rawtypes")
			List list = null;
			
			if (accountId == null) {
				list = entityWrapper.createQuery(
					"from S3UsageSnapshot as sus"
					+ " WHERE sus.key.timestampMs > ?"
					+ " AND sus.key.timestampMs < ?"
					+ " ORDER BY sus.key.timestampMs")
					.setLong(0, new Long(latestSnapshotBeforeMs))
					.setLong(1, new Long(period.getEndingMs()))
					.list();
			} else {
				list = entityWrapper.createQuery(
						"from S3UsageSnapshot as sus"
						+ " WHERE sus.key.timestampMs > ?"
						+ " AND sus.key.timestampMs < ?"
						+ " AND sus.key.accountId = ?"
						+ " ORDER BY sus.key.timestampMs")
						.setLong(0, new Long(latestSnapshotBeforeMs))
						.setLong(1, new Long(period.getEndingMs()))
						.setString(2, accountId)
						.list();				
			}
			
			for (Object obj: list) {
				
				S3UsageSnapshot snapshot = (S3UsageSnapshot) obj;
				S3SnapshotKey snapshotKey = snapshot.getSnapshotKey();
				S3SummaryKey summaryKey = new S3SummaryKey(snapshotKey);

				if ( snapshotKey.getTimestampMs() < period.getBeginningMs()
					 || !dataAccumulatorMap.containsKey(summaryKey) ) {

					//new accumulator, discard earlier accumulators from before report beginning
					S3DataAccumulator accumulator =
						new S3DataAccumulator(snapshotKey.getTimestampMs(),
								snapshot.getUsageData(), new S3UsageSummary());
					dataAccumulatorMap.put(summaryKey, accumulator);

				} else {
					
					/* Within interval; accumulate resource usage by adding
					 * to accumulator, for this key.
					 */
					S3DataAccumulator accumulator =	dataAccumulatorMap.get( summaryKey );

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
			for ( S3SummaryKey key: dataAccumulatorMap.keySet() ) {
				
				S3DataAccumulator accumulator =	dataAccumulatorMap.get( key );
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

    private class S3DataAccumulator
    {
    	private Long lastTimestamp;
    	private S3UsageData lastUsageData;
    	private S3UsageSummary currentSummary;
    	
    	public S3DataAccumulator(Long lastTimestamp,
				S3UsageData lastUsageData, S3UsageSummary currentSummary)
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
		
		public void setLastUsageData(S3UsageData lastUsageData)
		{
			this.lastUsageData = lastUsageData;
		}

		public S3UsageSummary getCurrentSummary()
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
    			
				currentSummary.addObjectsMegsSecs(
						lastUsageData.getObjectsMegs() * durationSecs);

				currentSummary.setObjectsMegsMax(
						Math.max(currentSummary.getObjectsMegsMax(),
								 lastUsageData.getObjectsMegs()));
				currentSummary.setBucketsNumMax(
						Math.max(currentSummary.getBucketsNumMax(),
								lastUsageData.getBucketsNum()));
				log.info(String.format("Accumulated durationSecs:%d raw:%d multDurationSecs:%d",durationSecs,
						lastUsageData.getObjectsMegs(),
						(lastUsageData.getObjectsMegs() * durationSecs)));
				this.lastUsageData = null;

    		}    		
    	}
    	
    }


}

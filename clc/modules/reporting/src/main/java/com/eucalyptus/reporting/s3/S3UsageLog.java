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

	Iterator<S3UsageSnapshot> scanLog(Period period)
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
	
	/**
	 * <p>Gather a Map of all S3 resource usage for a period.
	 */
    public Map<S3SnapshotKey, S3UsageSummary> getUsageSummaryMap(Period period)
    {
    	log.info("GetUsageSummaryMap period:" + period);
    	final Map<S3SnapshotKey, S3UsageSummary> usageMap =
    		new HashMap<S3SnapshotKey, S3UsageSummary>();


    	EntityWrapper<S3UsageSummary> entityWrapper =
			EntityWrapper.get(S3UsageSummary.class);
		try {

			/* Find the latest snapshot before the period beginning, by
			 * iteratively querying before the period beginning, moving
			 * backward in exponentially growing intervals
			 */
			long foundTimestampMs = 0l;
	    	final long oneHourMs = 60*60*1000;
			for ( int i=2 ;
				  (period.getBeginningMs() - oneHourMs*(long)i) > 0 ;
				  i=(int)Math.pow(i, 2))
			{
				
				long startingMs = period.getBeginningMs() - (oneHourMs*i);
				@SuppressWarnings("rawtypes")
				Iterator iter =
					entityWrapper.createQuery(
						"from S3UsageSnapshot as sus"
						+ " WHERE sus.key.timestampMs > ?"
						+ " AND sus.key.timestampMs < ?"
						+ " AND sus.key.allSnapshot = true")
						.setLong(0, new Long(startingMs))
						.setLong(1, new Long(period.getBeginningMs()))
						.iterate();
				while (iter.hasNext()) {
					S3UsageSnapshot snapshot = (S3UsageSnapshot) iter.next();
					foundTimestampMs = snapshot.getSnapshotKey().getTimestampMs();
					log.info("Found latest timestamp before beginning:"
							+ foundTimestampMs);
				}
				if (foundTimestampMs != 0l) break;
				
			}

			/* Start query from last snapshot before report beginning, iterate
			 * through the data, and accumulate all reporting info through the
			 * report end. We will accumulate a fraction of a snapshot at the
			 * beginning and end, since report boundaries will not likely
			 * coincide with sampling period boundaries.
			 */
			Map<S3SnapshotKey,S3DataAccumulator> dataAccumulatorMap =
				new HashMap<S3SnapshotKey,S3DataAccumulator>();
			
			@SuppressWarnings("rawtypes")
			Iterator iter = entityWrapper.createQuery(
					"from S3UsageSnapshot as sus"
					+ " WHERE sus.key.timestampMs > ?"
					+ " AND sus.key.timestampMs < ?")
					.setLong(0, new Long(foundTimestampMs))
					.setLong(1, new Long(period.getEndingMs()))
					.iterate();
			
			while (iter.hasNext()) {
				
				S3UsageSnapshot snapshot = (S3UsageSnapshot) iter.next();
				S3SnapshotKey key = snapshot.getSnapshotKey();
				key = new S3SnapshotKey(key); //use copy to prevent hibernate writes
				key.setAllSnapshot(false);

				if ( key.getTimestampMs() < period.getBeginningMs()
					 || !dataAccumulatorMap.containsKey(key) ) {

					//new accumulator, discard earlier accumulators from before report beginning
					S3DataAccumulator accumulator =
						new S3DataAccumulator(key.getTimestampMs(),
								snapshot.getUsageData(), new S3UsageSummary());
					dataAccumulatorMap.put(key, accumulator);

				} else {
					
					/* Within interval; accumulate resource usage by adding
					 * to accumulator, for this key.
					 */
					S3DataAccumulator accumulator =	dataAccumulatorMap.get( key );

					/* Extrapolate fractional usage for snapshots which occurred
					 * before report beginning.
					 */
					long beginningMs = Math.max( period.getBeginningMs(),
							accumulator.getLastTimestamp() );
					//query above specifies timestamp is before report end
					long endingMs = key.getTimestampMs()-1;
					long durationSecs = (endingMs - beginningMs) / 1000;

					accumulator.accumulateUsage( durationSecs );		
					accumulator.setLastTimestamp(key.getTimestampMs());
					accumulator.setLastUsageData(snapshot.getUsageData());
					log.info("Accumulate usage, begin:" + beginningMs
							+ " end:" + endingMs);

				}

			}

			/* Accumulate fractional data usage at end of reporting period.
			 */
			for ( S3SnapshotKey key: dataAccumulatorMap.keySet() ) {
				
				S3DataAccumulator accumulator =	dataAccumulatorMap.get( key );
				long beginningMs = Math.max( period.getBeginningMs(),
						accumulator.getLastTimestamp() );
				long endingMs = period.getEndingMs() - 1;
				long durationSecs = ( endingMs-beginningMs ) / 1000;
				accumulator.accumulateUsage( durationSecs );
				log.info("Accumulate end usage, begin:" + beginningMs
						+ " end:" + endingMs);

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
				this.lastUsageData = null;
				log.info("Accumulate "
						+ (lastUsageData.getObjectsMegs() * durationSecs));

    		}    		
    	}
    	
    }

    

}

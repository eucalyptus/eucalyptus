package com.eucalyptus.reporting.modules.instance;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.Period;

/**
 * <p>InstanceUsageLog is the main API for accessing usage information which
 * has been stored in the usage log.
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
	
	
	/**
	 * <p>Find the latest snapshot before timestampMs.
	 */
	long findLatestAllSnapshotBefore(long timestampMs)
	{
		long foundTimestampMs = 0l;

		EntityWrapper<InstanceUsageSnapshot> entityWrapper = null;
		try {

			/* Iteratively query before startingMs, moving backward in
			 * exponentially growing intervals, starting at 3 hrs before
			 */
	        for (double minsBefore=180; /* 3 hrs */
	        	 System.currentTimeMillis()-(long)(minsBefore*60*1000) > 0;
	        	 minsBefore=Math.pow(minsBefore, 1.1))
	        {
	            long queryStartMs = System.currentTimeMillis()-(long)(minsBefore*60*1000);

	        	entityWrapper = EntityWrapper.get(InstanceUsageSnapshot.class);
				
				log.info("Searching for latest timestamp before beginning:" + queryStartMs);
				@SuppressWarnings("rawtypes")
				List iuses =
					entityWrapper.createQuery(
						"from InstanceUsageSnapshot as ius"
						+ " WHERE ius.timestampMs > ?"
						+ " AND ius.timestampMs < ?")
						.setLong(0, new Long(queryStartMs))
						.setLong(1, new Long(timestampMs))
						.list();
				for (Object obj: iuses) {
					InstanceUsageSnapshot snapshot = (InstanceUsageSnapshot) obj;
					foundTimestampMs = snapshot.getTimestampMs();
				}
				entityWrapper.commit();
				if (foundTimestampMs != 0l) break;
			}
			log.info("Found latest timestamp before beginning:"
					+ foundTimestampMs);			
		} catch (Exception ex) {
			log.error(ex);
			if (entityWrapper != null) entityWrapper.rollback();
			throw new RuntimeException(ex);
		}
		
		return foundTimestampMs;	
	}

	
	
	/**
	 * <p>Gather a Map of all Instance resource usage for a period.
	 */
    public Map<InstanceSummaryKey, InstanceUsageSummary> getUsageSummaryMap(
    		Period period, String accountId)
    {
    	log.info("GetUsageSummaryMap period:" + period);

		final Map<InstanceSummaryKey, InstanceUsageSummary> usageMap =
    		new HashMap<InstanceSummaryKey, InstanceUsageSummary>();

		EntityWrapper<InstanceUsageSnapshot> entityWrapper =
			EntityWrapper.get(InstanceUsageSnapshot.class);
		try {

			/* Start query from last snapshot before report beginning, and
			 * iterate through the data until after the end. We'll truncate and
			 * extrapolate.
			 */
			long latestSnapshotBeforeMs =
				findLatestAllSnapshotBefore(period.getBeginningMs());
			long afterEnd = period.getEndingMs() 
					+ ((period.getBeginningMs()-latestSnapshotBeforeMs)*2);
			log.debug("latestSnapshotBeforeMs:" + latestSnapshotBeforeMs + " afterEndMs:" + afterEnd);

			
			@SuppressWarnings("rawtypes")
			List list = null;
			
			if (accountId == null) {
				list = entityWrapper.createQuery(
					"from InstanceAttributes as ia, InstanceUsageSnapshot as ius"
					+ " where ia.uuid = ius.uuid"
					+ " and ius.timestampMs > ?"
					+ " and ius.timestampMs < ?")
					.setLong(0, latestSnapshotBeforeMs)
					.setLong(1, afterEnd)
					.list();
			} else {
				list = entityWrapper.createQuery(
						"from InstanceAttributes as ia, InstanceUsageSnapshot as ius"
						+ " where ia.uuid = ius.uuid"
						+ " and ia.accountId = ?"
						+ " and ius.timestampMs > ?"
						+ " and ius.timestampMs < ?")
						.setString(0, accountId)
						.setLong(1, latestSnapshotBeforeMs)
						.setLong(2, afterEnd)
						.list();		
			}
			

			
			/* Accumulate data over timeline, by instance, keyed by instance uuid.
			 * Accumulated data consists of the instance running time, network
			 * io megs, and disk io megs for each instance.
			 */
			Map<String,InstanceDataAccumulator> dataAccumulatorMap =
				new HashMap<String,InstanceDataAccumulator>();
			
			for (Object obj: list) {

				Object[] row = (Object[]) obj;
				InstanceAttributes insAttrs = (InstanceAttributes) row[0];
				InstanceUsageSnapshot snapshot = (InstanceUsageSnapshot) row[1];

				log.debug("Found row attrs:" + insAttrs + " snapshot:" + snapshot);
				
				String uuid = insAttrs.getUuid();
				if ( !dataAccumulatorMap.containsKey( uuid ) ) {
					InstanceDataAccumulator accumulator =
						new InstanceDataAccumulator( insAttrs, snapshot, period );
					dataAccumulatorMap.put( uuid, accumulator );
				} else {
					InstanceDataAccumulator accumulator =
						dataAccumulatorMap.get( uuid );
					accumulator.update( snapshot );
				}

			}

			
			/* Summarize usage for each (zone,cluster,acct,user) key, by
			 * summing all usage for all instances for each key. Populate
			 * the usageMap, which is what we return.
			 */
			for (String uuid: dataAccumulatorMap.keySet()) {
				log.debug("Instance uuid:" + uuid);
				InstanceDataAccumulator accumulator =
					dataAccumulatorMap.get(uuid);
				InstanceSummaryKey key =
					new InstanceSummaryKey(accumulator.getInstanceAttributes());
				if (! usageMap.containsKey(key)) {
					usageMap.put(key, new InstanceUsageSummary());
				}
				InstanceUsageSummary ius = usageMap.get(key);
				ius.addDiskIoMegs(accumulator.getDiskIoMegs());
				ius.addNetworkIoMegs(accumulator.getNetIoMegs());
				ius.sumFromPeriodType(accumulator.getDurationPeriod(),
						accumulator.getInstanceAttributes().getInstanceType());
			}

			entityWrapper.commit();
		} catch (Exception ex) {
			log.error(ex);
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}

		
		if (log.isDebugEnabled()) {
			log.debug("Printing usageMap");
			for (InstanceSummaryKey key: usageMap.keySet()) {
				log.debug("key:" + key + " summary:" + usageMap.get(key));
			}
		}

        return usageMap;
    }


    /**
     * InstanceDataAccumulator will accumulate a series of
     * InstanceUsageSnapshot if you call the <code>update</code> method with
     * each snapshot, after which it can return the total duration of the
     * instance, disk io usage, and net io usage for the instance.
     */
    private class InstanceDataAccumulator
    {
    	private final InstanceAttributes insAttrs;
    	private InstanceUsageSnapshot firstSnapshot;
    	private InstanceUsageSnapshot lastSnapshot;
    	private Period period;
    	
    	public InstanceDataAccumulator(InstanceAttributes insAttrs,
    			InstanceUsageSnapshot snapshot, Period period)
		{
			super();
			this.insAttrs = insAttrs;
			this.firstSnapshot = snapshot;
			this.lastSnapshot = snapshot;
			this.period = period;
		}
    	
    	public void update(InstanceUsageSnapshot snapshot)
    	{
    		final long timeMs = snapshot.getTimestampMs().longValue();
    		if (timeMs > lastSnapshot.getTimestampMs().longValue()) {
        		this.lastSnapshot = snapshot;    			
    		} else if (timeMs < firstSnapshot.getTimestampMs().longValue()) {
    			this.firstSnapshot = snapshot;
    		}
    	}

    	public InstanceAttributes getInstanceAttributes()
    	{
    		return this.insAttrs;
    	}
    	
    	public long getDurationSecs()
    	{
    		long truncatedBeginMs = Math.max(period.getBeginningMs(), firstSnapshot.getTimestampMs());
    		long truncatedEndMs   = Math.min(period.getEndingMs(), lastSnapshot.getTimestampMs());
    		return ( truncatedEndMs-truncatedBeginMs ) / 1000;
    	}
    	
    	public Period getDurationPeriod()
    	{
    		long truncatedBeginMs = Math.max(period.getBeginningMs(), firstSnapshot.getTimestampMs());
    		long truncatedEndMs   = Math.min(period.getEndingMs(), lastSnapshot.getTimestampMs());
    		return new Period(truncatedBeginMs, truncatedEndMs);
    	}
    	
    	public long getDiskIoMegs()
    	{
			double duration = (double)(period.getEndingMs()-period.getBeginningMs());
			double gap = 0d;
    		double result =
    			(double)lastSnapshot.getCumulativeDiskIoMegs() -
    			(double)firstSnapshot.getCumulativeDiskIoMegs();
    		log.debug("Unadjusted disk io megs:" + result);
			/* Extrapolate fractional usage for snapshots which occurred
			 * before report beginning or after report end.
			 */
    		if (firstSnapshot.getTimestampMs() < period.getBeginningMs()) {
    			gap = (double)(period.getBeginningMs()-firstSnapshot.getTimestampMs());
    			result *= 1d-(gap/duration);
    		}
    		if (lastSnapshot.getTimestampMs() > period.getEndingMs()) {
    			gap = (double)(lastSnapshot.getTimestampMs()-period.getEndingMs());
    			result *= 1d-(gap/duration);
    		}
    		log.debug("Extrapolated disk io megs:" + result);
    		return (long) result;
    	}
    	
    	public long getNetIoMegs()
    	{
			double duration = (double)(period.getEndingMs()-period.getBeginningMs());
			double gap = 0d;
    		double result =
    			(double)lastSnapshot.getCumulativeNetworkIoMegs() -
    			(double)firstSnapshot.getCumulativeNetworkIoMegs();
    		log.debug("Unadjusted net IO megs:" + result);
			/* Extrapolate fractional usage for snapshots which occurred
			 * before report beginning or after report end.
			 */
    		if (firstSnapshot.getTimestampMs() < period.getBeginningMs()) {
    			gap = (double)(period.getBeginningMs()-firstSnapshot.getTimestampMs());
    			result *= 1d-(gap/duration);
    		}
    		if (lastSnapshot.getTimestampMs() > period.getEndingMs()) {
    			gap = (double)(lastSnapshot.getTimestampMs()-period.getEndingMs());
    			result *= 1d-(gap/duration);
    		}
    		log.debug("Extrapolated net IO megs:" + result);
    		return (long) result;
    	}

    }


}

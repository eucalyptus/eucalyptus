package com.eucalyptus.reporting.s3;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.event.S3Event;

public class S3EventListener
	implements EventListener<Event>
{
	private static Logger LOG = Logger.getLogger( S3EventListener.class );
	
	private static long WRITE_INTERVAL_MS = 10800000l; //write every 3 hrs

	private Map<UsageDataKey, S3UsageData> usageDataMap;
	private long lastAllSnapshotMs = 0l;

	public S3EventListener()
	{
	}
	
	@Override
	public void fireEvent(Event event)
	{
		if (event instanceof S3Event) {
			S3Event s3Event = (S3Event) event;
			long timeMillis = getCurrentTimeMillis();

			final S3UsageLog usageLog = S3UsageLog.getS3UsageLog();

			EntityWrapper<S3UsageSnapshot> entityWrapper =
				EntityWrapper.get( S3UsageSnapshot.class );
			try {

				LOG.info("Receive event:" + s3Event.toString());

				/* Load usageDataMap if starting up
				 */
				if (usageDataMap == null) {
					this.usageDataMap = new HashMap<UsageDataKey, S3UsageData>();
					// TODO: optimize so we don't have to scan entire log on
					// startup
					Iterator<S3UsageSnapshot> iter = usageLog
							.scanLog(new Period(0l, Long.MAX_VALUE));
					while (iter.hasNext()) {
						S3UsageSnapshot snapshot = iter.next();
						UsageDataKey key = new UsageDataKey(
								snapshot.getSnapshotKey());
						usageDataMap.put(key, snapshot.getUsageData());
						if (snapshot.getSnapshotKey().getAllSnapshot()) {
							lastAllSnapshotMs = timeMillis;							
						}
						System.out.println("Loaded key:" + key);
					}
					LOG.info("Loaded usageDataMap");
				}

				
				/* Update usageDataMap
				 */
				UsageDataKey key = new UsageDataKey(s3Event.getOwnerId(),
						s3Event.getAccountId());
				S3UsageData usageData;
				if (usageDataMap.containsKey(key)) {
					usageData = usageDataMap.get(key);
				} else {
					usageData = new S3UsageData();
					usageDataMap.put(key, usageData);
				}
				long addNum = (s3Event.isCreateOrDelete()) ? 1 : -1;

				if (s3Event.isObjectOrBucket()) {
					
					long addAmountMegs = (s3Event.isCreateOrDelete())
						? s3Event.getSizeMegs()
						: -s3Event.getSizeMegs();

					Long newObjectsNum =
						addLong(usageData.getObjectsNum(), addNum);
					usageData.setObjectsNum(newObjectsNum);
					Long newObjectsMegs =
						addLong(usageData.getObjectsMegs(), addAmountMegs);
					usageData.setObjectsMegs(newObjectsMegs);
					
				} else {
					
					Long newBucketsNum =
						addLong(usageData.getBucketsNum(), addNum);
					usageData.setBucketsNum(newBucketsNum);
					
				}

				/* Write data to DB
				 */
				if ((timeMillis - lastAllSnapshotMs) > WRITE_INTERVAL_MS) {
					/* Write all snapshots
					 */
					LOG.info("Starting allSnapshot...");
					for (UsageDataKey udk: usageDataMap.keySet()) {
						S3SnapshotKey snapshotKey = udk.newSnapshotKey(timeMillis);
						S3UsageSnapshot sus =
							new S3UsageSnapshot(snapshotKey, usageDataMap.get(key));
						sus.getSnapshotKey().setAllSnapshot(true);
						LOG.info("Storing part of allSnapshot:" + sus);
						entityWrapper.add(sus);
						lastAllSnapshotMs = timeMillis;
					}
					LOG.info("Ending allSnapshot...");
				} else {
					/* Write this snapshot
					 */
					S3SnapshotKey snapshotKey = key.newSnapshotKey(timeMillis);
					S3UsageSnapshot sus =
						new S3UsageSnapshot(snapshotKey, usageDataMap.get(key));
					LOG.info("Storing:" + sus);
					entityWrapper.add(sus);
				}

				entityWrapper.commit();
			} catch (Exception ex) {
				entityWrapper.rollback();
				LOG.error(ex);		
			}
		}
	}
	
	private static final Long addLong(Long a, long b)
	{
		return new Long(a.longValue() + b);
	}

	protected long getCurrentTimeMillis()
	{
		return (this.testCurrentTimeMillis == null)
				? System.currentTimeMillis()
				: this.testCurrentTimeMillis.longValue();
	}

	private Long testCurrentTimeMillis = null;

	/**
	 * Used only for testing. Sets the poller into test mode and overrides the
	 * actual timestamp with a false one.
	 */
	protected void setCurrentTimeMillis(long testCurrentTimeMillis)
	{
		this.testCurrentTimeMillis = new Long(testCurrentTimeMillis);
	}

	private class UsageDataKey
	{
		private final String ownerId;
		private final String accountId;

		public UsageDataKey(String ownerId, String accountId)
		{
			this.ownerId = ownerId;
			this.accountId = accountId;
		}
		
		public UsageDataKey(S3SnapshotKey key)
		{
			this.ownerId = key.getOwnerId();
			this.accountId = key.getAccountId();
		}

		public String getOwnerId()
		{
			return ownerId;
		}

		public String getAccountId()
		{
			return accountId;
		}

		public S3SnapshotKey newSnapshotKey(long timestampMs)
		{
			return new S3SnapshotKey(ownerId, accountId, timestampMs);
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((accountId == null) ? 0 : accountId.hashCode());
			result = prime * result
					+ ((ownerId == null) ? 0 : ownerId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			UsageDataKey other = (UsageDataKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (accountId == null) {
				if (other.accountId != null)
					return false;
			} else if (!accountId.equals(other.accountId))
				return false;
			if (ownerId == null) {
				if (other.ownerId != null)
					return false;
			} else if (!ownerId.equals(other.ownerId))
				return false;
			return true;
		}

		private S3EventListener getOuterType()
		{
			return S3EventListener.this;
		}

		
	}
}

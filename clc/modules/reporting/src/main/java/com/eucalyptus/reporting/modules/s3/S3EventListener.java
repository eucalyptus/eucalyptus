package com.eucalyptus.reporting.modules.s3;

import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.event.S3Event;
import com.eucalyptus.reporting.user.ReportingAccountDao;
import com.eucalyptus.reporting.user.ReportingUserDao;

public class S3EventListener
	implements EventListener<Event>
{
	private static Logger LOG = Logger.getLogger( S3EventListener.class );
	
	private static long WRITE_INTERVAL_MS = 10800000l; //write every 3 hrs

	private Map<S3SummaryKey, S3UsageData> usageDataMap;
	private long lastAllSnapshotMs = 0l;

	public S3EventListener()
	{
	}
	
	@Override
	public void fireEvent(Event event)
	{
		if (event instanceof S3Event) {
			S3Event s3Event = (S3Event) event;

			/* Retain records of all account and user id's and names encountered
			 * even if they're subsequently deleted.
			 */
			ReportingAccountDao.getInstance().addUpdateAccount(
					s3Event.getAccountId(), s3Event.getAccountName());
			ReportingUserDao.getInstance().addUpdateUser(s3Event.getOwnerId(),
					s3Event.getOwnerName());

			long timeMillis = getCurrentTimeMillis();

			final S3UsageLog usageLog = S3UsageLog.getS3UsageLog();

			EntityWrapper<S3UsageSnapshot> entityWrapper =
				EntityWrapper.get( S3UsageSnapshot.class );
			try {

				LOG.info("Receive event:" + s3Event.toString());

				/* Load usageDataMap if starting up
				 */
				if (usageDataMap == null) {

					this.usageDataMap = usageLog.findLatestUsageData();
					LOG.info("Loaded usageDataMap");

				}

				
				/* Update usageDataMap
				 */
				S3SummaryKey key = new S3SummaryKey(s3Event.getOwnerId(),
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
					for (S3SummaryKey summaryKey: usageDataMap.keySet()) {
						S3SnapshotKey snapshotKey = new S3SnapshotKey(
								summaryKey.getOwnerId(), summaryKey.getAccountId(),
								timeMillis);
						S3UsageSnapshot sus =
							new S3UsageSnapshot(snapshotKey, usageDataMap.get(key));
						sus.setAllSnapshot(true);
						LOG.info("Storing part of allSnapshot:" + sus);
						entityWrapper.add(sus);
						lastAllSnapshotMs = timeMillis;
					}
					LOG.info("Ending allSnapshot...");
				} else {
					/* Write this snapshot
					 */
					S3SnapshotKey snapshotKey = new S3SnapshotKey(
							key.getOwnerId(), key.getAccountId(),
							timeMillis);
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

	/**
	 * Overridable for the purpose of testing. Testing generates fake events
	 * with fake times.
	 */
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
	
}

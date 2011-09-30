package com.eucalyptus.reporting.storage;

import java.util.Iterator;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.event.StorageEvent;
import com.eucalyptus.reporting.modules.storage.*;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.util.ExposedCommand;

public class FalseDataGenerator
{
	private static final int  NUM_USERS    = 32;
	private static final int  NUM_ACCOUNTS = 16;
	private static final int  NUM_CLUSTERS = 4;
	private static final int  NUM_ZONES    = 2;
	private static final int  SNAPSHOTS_PER_USER = 256;
	private static final long START_TIME = 1104566400000l; //Jan 1, 2005 12:00AM
	private static final int  TIME_USAGE_APART = 1000000; //ms
	private static final long MAX_MS = ((SNAPSHOTS_PER_USER+1) * TIME_USAGE_APART) + START_TIME;
	private static final long ERROR_MARGIN_MS = 60*60*1000;

	@ExposedCommand
	public static void generateFalseData()
	{
		System.out.println(" ----> GENERATING FALSE DATA");

		QueueSender queueSender = QueueFactory.getInstance().getSender(QueueIdentifier.STORAGE);

		TestEventListener listener = new TestEventListener();
		listener.setCurrentTimeMillis(START_TIME);
		QueueReceiver queueReceiver = QueueFactory.getInstance().getReceiver(QueueIdentifier.STORAGE);
		
		queueReceiver.removeAllListeners(); //Remove non-test listeners set up by bootstrapper
		queueReceiver.addEventListener(listener);
	
		
		for (int i = 0; i < SNAPSHOTS_PER_USER; i++) {
			
			long timestampMs = (i * TIME_USAGE_APART) + START_TIME;
			listener.setCurrentTimeMillis(timestampMs);

			for (int j = 0; j < NUM_USERS; j++) {
				String userId = String.format("fakeUserId-%d", j);
				String userName = String.format("fakeUserName:%d", j);
				String accountId = String.format("fakeAccountId-%d",
						(j % NUM_ACCOUNTS));
				String accountName = String.format("fakeAccountName:%d", (j % NUM_ACCOUNTS));
				String clusterId = String.format("cluster-%d", (j % NUM_CLUSTERS));
				String zoneId = String.format("zone-%d", (j % NUM_ZONES));

				for (int k = 0; k < StorageEvent.EventType.values().length; k++) {
					long sizeMegs = 1024 + (i * k);
					StorageEvent.EventType eventType =
						StorageEvent.EventType.values()[k];
					StorageEvent event = new StorageEvent(eventType, true,
							sizeMegs, userId, userName, accountId, accountName,
							clusterId, zoneId);
					queueSender.send(event);
					System.out.printf("Sending event %d for %d,%d\n", k, i, j);
				}
				
			}
		}

	}
	
	private static boolean isWithinError(long val, long correctVal, long error)
	{
		return ((correctVal - error) < val) && (val < (correctVal + error));
	}
	

	@ExposedCommand
	public static void removeFalseData()
	{
		System.out.println(" ----> REMOVING FALSE DATA");

		StorageUsageLog.getStorageUsageLog().purgeLog(MAX_MS);
	}

	@ExposedCommand
	public static void removeAllData()
	{
		System.out.println(" ----> REMOVING ALL DATA");

		StorageUsageLog.getStorageUsageLog().purgeLog(Long.MAX_VALUE);
	}

	/**
	 * <p>containsRecentRows checks if there are recent rows in 
	 * StorageUsageSnapshot. This is used for testing: we delete
	 * all data, then set up volumes, then determine if rows made
	 * it to the DB.
	 * 
	 * @param hasRows Indicates whether there should be any
	 * rows in StorageUsageSnapshot. If true, and there are no rows,
	 * and Exception is thrown; if false, and there are rows, an
	 * Exception is thrown. If true, rows are checked to verify they
	 * are relatively recent (within 1 hr). 
	 */
	@ExposedCommand
	public static void containsRecentRows(String hasRows)
	{
		boolean containsRows =
			(hasRows!=null && hasRows.equalsIgnoreCase("true"));
		System.out.println(" ----> CONTAINS RECENT ROWS:" + containsRows);
		
		EntityWrapper<StorageUsageSnapshot> entityWrapper =
			EntityWrapper.get(StorageUsageSnapshot.class);
		
		try {
			int rowCnt = 0;
			@SuppressWarnings("rawtypes")
			Iterator iter =
				entityWrapper.createQuery(
					"from StorageUsageSnapshot as sus")
					.iterate();
			while (iter.hasNext()) {
				if (!containsRows) {
					throw new RuntimeException("Found >0 rows where 0 expected");
				}
				rowCnt++;
				StorageUsageSnapshot snapshot = (StorageUsageSnapshot) iter.next();
				long foundTimestampMs = snapshot.getSnapshotKey().getTimestampMs();
				long nowMs = System.currentTimeMillis();
				if (!isWithinError(nowMs, foundTimestampMs, ERROR_MARGIN_MS)) {
					throw new RuntimeException(String.format(
							"Row outside error margin, expected:%d found:%d",
							nowMs, foundTimestampMs));
				}
			}
			if (rowCnt==0 && containsRows) {
				throw new RuntimeException("Found 0 rows where >0 expected");
			}
			entityWrapper.commit();
		} catch (Exception ex) {
			entityWrapper.rollback();
			throw new RuntimeException(ex);
		}

	}

	public static void printFalseData()
	{
		System.out.println(" ----> PRINTING FALSE DATA");

//		StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();
//		Iterator<StorageUsageSnapshot> iter = usageLog.scanLog(new Period(0, MAX_MS));
//		while (iter.hasNext()) {
//			StorageUsageSnapshot snapshot = iter.next();
//			System.out.println(snapshot);
//		}
	}

	/**
	 * TestEventListener provides fake times which you can modify.
	 * 
	 * @author twerges
	 */
	private static class TestEventListener
		extends StorageEventListener
	{
		private long fakeCurrentTimeMillis = 0l;

		protected void setCurrentTimeMillis(long currentTimeMillis)
		{
			this.fakeCurrentTimeMillis = currentTimeMillis;
		}
		
		protected long getCurrentTimeMillis()
		{
			return fakeCurrentTimeMillis;
		}
	}

}

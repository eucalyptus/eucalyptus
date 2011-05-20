package com.eucalyptus.reporting.s3;

import java.util.*;

import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.event.S3Event;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.util.ExposedCommand;

public class FalseDataGenerator
{
	private static final int  NUM_USERS    = 32;
	private static final int  NUM_ACCOUNTS = 16;
	private static final int  SNAPSHOTS_PER_USER = 64;
	private static final long START_TIME = 1104566400000l; //Jan 1, 2005 12:00AM
	private static final int  TIME_USAGE_APART = 500000; //ms
	private static final long MAX_MS = ((SNAPSHOTS_PER_USER+1) * TIME_USAGE_APART) + START_TIME;

	@ExposedCommand
	public static void generateFalseData()
	{
		System.out.println(" ----> GENERATING FALSE DATA");

		QueueSender queueSender = QueueFactory.getInstance().getSender(QueueIdentifier.S3);

		TestEventListener listener = new TestEventListener();
		listener.setCurrentTimeMillis(START_TIME);
		QueueReceiver queueReceiver = QueueFactory.getInstance().getReceiver(QueueIdentifier.S3);
		queueReceiver.removeAllListeners(); //Remove non-test listeners set up by bootstrapper
		queueReceiver.addEventListener(listener);
	
		
		for (int i = 0; i < SNAPSHOTS_PER_USER; i++) {
			
			long timestampMs = (i * TIME_USAGE_APART) + START_TIME;
			listener.setCurrentTimeMillis(timestampMs);

			for (int j = 0; j < NUM_USERS; j++) {
				String userId = String.format("user-%d", j);
				String accountId = String.format("account-%d",
						(j % NUM_ACCOUNTS));

				long sizeMegs = 1024 + i;
				S3Event event = new S3Event(true, sizeMegs, userId, accountId);
				queueSender.send(event);
				if (i % 10 == 0) {
					event = new S3Event(true, userId, accountId);
					queueSender.send(event);
				}
				System.out.printf("Sending event for %d,%d\n", i, j);

			}
		}

	}


	private static final long CORRECT_SIZE = 145000l;
	private static final long CORRECT_TIME  = 460000000l;
	private static final double ERROR_FACTOR = 0.2;
	private static final double TIME_ERROR_FACTOR = 0.2;
	
	private static final long RANGE = MAX_MS - START_TIME;
	private static final long DISTANCE =  RANGE / 5;

	private static final int NUM_TESTS = 16;

	@ExposedCommand
	public static void printUsageSummaryMap(String beginMsStr, String endMsStr)
	{
		
		long beginMs = Long.parseLong(beginMsStr);
		long endMs = Long.parseLong(endMsStr);
		Period period = new Period(beginMs, endMs);
		System.out.println(" ----> PRINT USAGE SUMMARY MAP:" + period);

		Map<S3SummaryKey, S3UsageSummary> usageSummaryMap =
			S3UsageLog.getS3UsageLog().getUsageSummaryMap(period);
		for (S3SummaryKey key: usageSummaryMap.keySet()) {
			System.out.println("key:" + key + " summary:"
					+ usageSummaryMap.get(key));
		}
	}
	
	@SuppressWarnings("unused")
	private static boolean isWithinError(long val, long correctVal, double errorPercent)
	{
		return isWithinError((double)val, (double)correctVal, errorPercent);
	}

	private static boolean isWithinError(double val, double correctVal, double errorPercent)
	{
		return correctVal * (1-errorPercent) < val
				&& val < correctVal * (1+errorPercent);
	}


	@ExposedCommand
	public static void removeFalseData()
	{
		System.out.println(" ----> REMOVING FALSE DATA");

		S3UsageLog.getS3UsageLog().purgeLog(MAX_MS);
	}

	@ExposedCommand
	public static void removeAllData()
	{
		System.out.println(" ----> REMOVING ALL DATA");

		S3UsageLog.getS3UsageLog().purgeLog(Long.MAX_VALUE);		
	}
	
	public static void printFalseData()
	{
		System.out.println(" ----> PRINTING FALSE DATA");

//		S3UsageLog usageLog = S3UsageLog.getS3UsageLog();
//		Iterator<S3UsageSnapshot> iter = usageLog.scanLog(new Period(0, MAX_MS));
//		while (iter.hasNext()) {
//			S3UsageSnapshot snapshot = iter.next();
//			System.out.println(snapshot);
//		}
	}

	private static GroupByCriterion getCriterion(String name)
	{
		if (name.equalsIgnoreCase("zone"))
			return GroupByCriterion.AVAILABILITY_ZONE;
		else
			/* throws an IllegalArgument which we allow to percolate up
			 */
			return GroupByCriterion.valueOf(name.toUpperCase());
	}

	/**
	 * TestEventListener provides fake times which you can modify.
	 * 
	 * @author twerges
	 */
	private static class TestEventListener
		extends S3EventListener
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

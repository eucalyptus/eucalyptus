package com.eucalyptus.reporting.storage;

import java.util.*;

import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.event.StorageEvent;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.util.ExposedCommand;

public class FalseDataGenerator
{
	private static final int  NUM_USERS    = 32;
	private static final int  NUM_ACCOUNTS = 16;
	private static final int  NUM_CLUSTERS = 4;
	private static final int  NUM_ZONES    = 2;
	private static final int  SNAPSHOTS_PER_USER = 64;
	private static final long START_TIME = 1104566400000l; //Jan 1, 2005 12:00AM
	private static final int  TIME_USAGE_APART = 100000; //ms
	private static final long MAX_MS = ((SNAPSHOTS_PER_USER+1) * TIME_USAGE_APART) + START_TIME;

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
				String userId = String.format("user-%d", j);
				String accountId = String.format("account-%d",
						(j % NUM_ACCOUNTS));
				String clusterId = String.format("cluster-%d", (j % NUM_CLUSTERS));
				String zoneId = String.format("zone-%d", (j % NUM_ZONES));

				for (int k = 0; k < StorageEvent.EventType.values().length; k++) {
					long sizeMegs = 1024 + (i * k);
					StorageEvent.EventType eventType =
						StorageEvent.EventType.values()[k];
					StorageEvent event = new StorageEvent(eventType, true,
							sizeMegs, userId, accountId, clusterId, zoneId);
					queueSender.send(event);
					System.out.printf("Sending event %d for %d,%d\n", k, i, j);
				}
				
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
	public static void testFalseData()
	{
		boolean allTestsPassed = true;
		System.out.println(" ----> TESTING FALSE DATA");

		StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();
		Map<String, StorageUsageSummary> summary = usageLog.scanSummarize(new Period(START_TIME, MAX_MS), GroupByCriterion.USER);
		for (String key: summary.keySet()) {
			StorageUsageSummary sus = summary.get(key);
			long totalSize = sus.getSnapshotsMegsMax() + sus.getVolumesMegsMax();
			long totalTime = sus.getSnapshotsMegsSecs() + sus.getVolumesMegsSecs();
			System.out.printf("%s: %d,%d\n", key, totalSize, totalTime);
		}

		final Random rand = new Random();
		for (int i=0; i<NUM_TESTS; i++) {

			/* Calculate a random range of starting and ending times, of at
			 * least a certain duration and within the range of generated false
			 * data.
			 */
			final long lowerBound = rand.nextInt((int)(RANGE - DISTANCE)) + START_TIME;
			final long upperBound = rand.nextInt((int)(MAX_MS - (lowerBound + DISTANCE))) + lowerBound + DISTANCE;
			final double fraction =  ((double)upperBound - (double)lowerBound) / ((double)MAX_MS - (double)START_TIME);
			final double adjustedCorrectSize = (double)CORRECT_SIZE*fraction;
			final double adjustedCorrectTime  = (double)CORRECT_TIME*fraction;
			final double adjustedError = ERROR_FACTOR * (1.0 + (1.0-fraction));
			final double adjustedTimeError = TIME_ERROR_FACTOR * (1.0 + (1.0-fraction));

			System.out.printf("#:%3d correct:(%d,%d) fraction:%3.3f adjusted:(%3.3f , %3.3f)"
					   + " adjustedError:(%3.3f , %3.3f)\n",
					i, CORRECT_SIZE, CORRECT_TIME,
					fraction, adjustedCorrectSize, adjustedCorrectTime,
					adjustedError, adjustedTimeError);

			summary = usageLog.scanSummarize(new Period(lowerBound, upperBound), GroupByCriterion.USER);
			for (String userId: summary.keySet()) {
				StorageUsageSummary sus = summary.get(userId);
				long totalSize = sus.getSnapshotsMegsMax() + sus.getVolumesMegsMax();
				long totalTime = sus.getSnapshotsMegsSecs() + sus.getVolumesMegsSecs();

				final double sizeError = (double)totalSize / adjustedCorrectSize;
				final double timeError = (double)totalTime / adjustedCorrectTime;;
				final boolean sizeWithin = isWithinError(totalSize, adjustedCorrectSize, adjustedError);
				final boolean timeWithin = isWithinError(totalTime, adjustedCorrectTime, adjustedError);

				final boolean correct = (sizeWithin && timeWithin);
				System.out.printf(" %8s:(%d/%3.3f , %d/%3.3f) " +
						  "error:(%3.3f,%3.3f) isWithin:(%s,%s) %s\n",
						userId, totalSize, adjustedCorrectSize,
						totalTime, adjustedCorrectTime,
						sizeError, timeError,
						sizeWithin, timeWithin, (correct?"":"INCORRECT"));

				if (!correct)
				{
					System.out.println("Incorrect result for user:" + userId);
					allTestsPassed = false;
				}
			}
		}
		if (!allTestsPassed) throw new RuntimeException("Test Failed"); //TODO: different exception here
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

		StorageUsageLog.getStorageUsageLog().purgeLog(MAX_MS);
	}

	public static void printFalseData()
	{
		System.out.println(" ----> PRINTING FALSE DATA");

		StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();
		Iterator<StorageUsageSnapshot> iter = usageLog.scanLog(new Period(0, MAX_MS));
		while (iter.hasNext()) {
			StorageUsageSnapshot snapshot = iter.next();
			System.out.println(snapshot);
		}
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
	 * This method takes a String parameter rather than a GroupByCriterion,
	 * because it's intended to be called from the command-line test harness.
	 * 
	 * @throws IllegalArgumentException if criterion does not match
	 *   any GroupByCriterion
	 */
	@ExposedCommand
	public static void summarizeFalseDataOneCriterion(
			String criterion)
	{
		GroupByCriterion crit = getCriterion(criterion);
		System.out.println(" ----> PRINTING FALSE DATA BY " + crit);

		StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();

		Map<String, StorageUsageSummary> summaryMap = usageLog.scanSummarize(
				new Period(0L, MAX_MS), crit);
		for (String critVal: summaryMap.keySet()) {
			System.out.printf("%s:%s Summary:%s\n", crit, critVal,
					summaryMap.get(critVal));
		}
	}

	/**
	 * This method takes Strings as parameters rather than GroupByCriterion's,
	 * because it's intended to be called from the command-line test harness.
	 * 
	 * @throws IllegalArgumentException if either criterion does not match
	 *   any GroupByCriterion
	 */
	@ExposedCommand
	public static void summarizeFalseDataTwoCriteria(
			String outerCriterion,
			String innerCriterion)
	{
		GroupByCriterion outerCrit = getCriterion(outerCriterion);
		GroupByCriterion innerCrit = getCriterion(innerCriterion);
		System.out.printf(" ----> PRINTING FALSE DATA BY %s,%s\n", outerCrit,
				innerCrit);

		StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();

		Map<String, Map<String, StorageUsageSummary>> summaryMap =
			usageLog.scanSummarize(new Period(0L, MAX_MS),
					outerCrit, innerCrit);
		for (String outerCritVal: summaryMap.keySet()) {
			Map<String, StorageUsageSummary> innerMap = summaryMap.get(outerCritVal);
			for (String innerCritVal: innerMap.keySet()) {
				System.out.printf("%s:%s %s:%s Summary:%s\n", outerCrit,
						outerCritVal, innerCrit, innerCritVal,
						innerMap.get(innerCritVal));
			}
		}

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

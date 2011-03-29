package com.eucalyptus.reporting.storage;

import java.util.*;

import com.eucalyptus.reporting.*;
import com.eucalyptus.reporting.event.*;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;

public class FalseDataGenerator
{
	private static final int  NUM_USERS    = 128;
	private static final int  NUM_ACCOUNTS = 64;
	private static final int  NUM_CLUSTERS = 16;
	private static final int  NUM_ZONES    = 4;
	private static final int  SNAPSHOTS_PER_USER = 64;
	private static final long START_TIME = 1104566400000l; //Jan 1, 2005 12:00AM
	private static final int  TIME_USAGE_APART = 100000; //ms
	private static final long MAX_MS = ((SNAPSHOTS_PER_USER+1) * TIME_USAGE_APART) + START_TIME;

	private static ReportingBootstrapper reportingBootstrapper;

	public static void generateFalseData()
	{
		System.out.println(" ----> GENERATING FALSE DATA");

		reportingBootstrapper = new ReportingBootstrapper();
		reportingBootstrapper.start();
		StorageEventPoller storagePoller = reportingBootstrapper.getOverriddenStorageEventPoller();

		QueueSender queueSender = QueueFactory.getInstance().getSender(QueueIdentifier.STORAGE);
		
		
		for (int i = 0; i < SNAPSHOTS_PER_USER; i++) {
			
			long timestampMs = (i * TIME_USAGE_APART) + START_TIME;
			storagePoller.setTestTimestampMs(timestampMs);

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

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		reportingBootstrapper.stop();

	}
	
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

	public static void runTest()
	{
		removeFalseData();
		printFalseData();
		generateFalseData();
		printFalseData();
		removeFalseData();
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

	public static void main(String[] args)
		throws Exception
	{
		String methodName = args[0];
		Object[] methodArgsArray = new Object[args.length - 1];
		Class[] paramTypes = new Class[args.length - 1];
		System.out.println("Executing " + methodName);
		for (int i = 1; i < args.length; i++) {
			paramTypes[i - 1] = String.class;
			methodArgsArray[i - 1] = args[i];
			System.out.println(" param:" + args[i-1]);
		}
		FalseDataGenerator.class.getDeclaredMethod(methodName, paramTypes)
				.invoke(null, methodArgsArray);
	}

}

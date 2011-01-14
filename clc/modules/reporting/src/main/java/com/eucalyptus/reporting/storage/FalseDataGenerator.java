package com.eucalyptus.reporting.storage;

import java.util.*;

import com.eucalyptus.reporting.GroupByCriterion;
import com.eucalyptus.reporting.Period;
import com.eucalyptus.reporting.event.InstanceEvent;
import com.eucalyptus.reporting.instance.*;

public class FalseDataGenerator
{	
	private static final long MAX_MS = ((NUM_USAGE+1) * TIME_USAGE_APART);

	public static void generateFalseData()
	{
		List<InstanceAttributes> fakeInstances =
				new ArrayList<InstanceAttributes>();
		
		System.out.println(" ----> GENERATING FALSE DATA");

		StorageUsageLog usageLog = StorageUsageLog.getStorageUsageLog();

		
		for (int i = 0; i < NUM_INSTANCE; i++) {
			
		}

		TestEventListener listener = new TestEventListener();
		for (int i=0; i<NUM_USAGE; i++) {
			listener.setCurrentTimeMillis(i * TIME_USAGE_APART);
			for (InstanceAttributes insAttrs : fakeInstances) {
				long instanceNum = Long.parseLong(insAttrs.getUuid());
				long netIo = instanceNum + i;
				long diskIo = instanceNum + i*2;
				InstanceEvent event = new InstanceEvent(insAttrs.getUuid(),
						insAttrs.getInstanceId(), insAttrs.getInstanceType(),
						insAttrs.getUserId(), insAttrs.getAccountId(),
						insAttrs.getClusterName(),
						insAttrs.getAvailabilityZone(), new Long(netIo),
						new Long(diskIo));
				System.out.println("Generating:" + i);
				listener.receiveEvent(event);
			}
		}

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

	private static GroupByCriterion getCriterion(String name)
	{
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

		Map<String, StorageUsageData> summaryMap = usageLog.scanSummarize(
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

		Map<String, Map<String, StorageUsageData>> summaryMap =
			usageLog.scanSummarize(new Period(0L, MAX_MS),
					outerCrit, innerCrit);
		for (String outerCritVal: summaryMap.keySet()) {
			Map<String, StorageUsageData> innerMap = summaryMap.get(outerCritVal);
			for (String innerCritVal: innerMap.keySet()) {
				System.out.printf("%s:%s %s:%s Summary:%s\n", outerCrit,
						outerCritVal, innerCrit, innerCritVal,
						innerMap.get(innerCritVal));
			}
		}

	}

}

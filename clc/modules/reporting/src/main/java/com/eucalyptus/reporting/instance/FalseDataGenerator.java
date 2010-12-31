package com.eucalyptus.reporting.instance;

import java.util.*;

import com.eucalyptus.reporting.event.InstanceEvent;
import com.eucalyptus.upgrade.TestDescription;

/**
 * <p>FalseDataGenerator generates false data about instances,
 * including fake starting and ending times, imaginary resource
 * usage, fictitious clusters, and non-existent accounts and users.
 * 
 * <p>False data can be used for testing.
 * 
 * <p><i>"False data can come from many sources: academic, social,
 * professional... False data can cause one to make stupid mistakes..."</i>
 *   - L. Ron Hubbard
 */
@TestDescription("Generates false instance reporting data")
public class FalseDataGenerator
{
	private static final int NUM_USAGE = 32;
	private static final int NUM_INSTANCE = 16;
	private static final int TIME_USAGE_APART = 100000; //ms
	private static final long MAX_MS = ((NUM_USAGE+1) * TIME_USAGE_APART);

	private static final int NUM_USER = 32;
	private static final int NUM_CLUSTER = 4;
	private static final int NUM_AVAIL_ZONE = 2;

	private enum FalseInstanceType
	{
		M1SMALL, C1MEDIUM, M1LARGE, M1XLARGE, C1XLARGE;
	}

	public static void generateFalseData()
	{
		List<InstanceAttributes> fakeInstances =
				new ArrayList<InstanceAttributes>();
		
		System.out.println(" ----> GENERATING FALSE DATA");

		for (int i = 0; i < NUM_INSTANCE; i++) {
			String uuid = new Long(i).toString();
			String instanceId = String.format("instance-%d", (i % NUM_INSTANCE));
			String userId = String.format("user-%d", (i % NUM_USER));
			String clusterName = String.format("cluster-%d", (i % NUM_CLUSTER));
			String availZone = String.format("zone-%d", (i % NUM_AVAIL_ZONE));
			FalseInstanceType[] vals = FalseInstanceType.values();
			String instanceType = vals[i % vals.length].toString();

			InstanceAttributes insAttrs = new InstanceAttributes(uuid,
					instanceId, instanceType, userId, clusterName, availZone);
			fakeInstances.add(insAttrs);
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
						insAttrs.getUserId(), insAttrs.getClusterName(),
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

		InstanceUsageLog.getInstanceUsageLog().purgeLog(MAX_MS);
	}
	
	public static void printFalseData()
	{
		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		System.out.println(" ----> PRINTING FALSE DATA");
		for (InstanceUsageLog.LogScanResult result: usageLog.scanLog(new Period(0L, MAX_MS))) {

			InstanceAttributes insAttrs = result.getInstanceAttributes();
			Period period = result.getPeriod();
			UsageData usageData = result.getUsageData();
			
			System.out.printf("instance:%s type:%s user:%s cluster:%s zone:%s period:%d-%d"
					+ " netIo:%d diskIo:%d\n",
					insAttrs.getInstanceId(), insAttrs.getInstanceType(), insAttrs.getUserId(),
					insAttrs.getClusterName(), insAttrs.getAvailabilityZone(),
					period.getBeginningMs(), period.getEndingMs(), usageData.getNetworkIoMegs(),
					usageData.getDiskIoMegs());
		}
	}

	private static InstanceUsageLog.GroupByCriterion getCriterion(String name)
	{
		/* throws an IllegalArgument if no such criterion exists, which is the
		 * appropriate exception for our uses here, so we let it percolate up.
		 */
		return InstanceUsageLog.GroupByCriterion.valueOf(name.toUpperCase());
	}

	/**
	 * This method takes a String as a parameter rather than an
	 * InstanceUsageLog.GroupByCriterion, because it's intended to be called
	 * from the command-line test harness. It converts the String into a
	 * GroupByCriterion. Possible values include "user","cluster",etc.
	 * 
	 * @throws IllegalArgumentException if criterion does not match
	 *   any InstanceUsageLog.GroupByCriterion
	 */
	public static void summarizeFalseDataOneCriterion(
			String criterion)
	{
		InstanceUsageLog.GroupByCriterion crit = getCriterion(criterion);
		System.out.println(" ----> PRINTING FALSE DATA BY " + crit);

		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<String, UsageSummary> summaryMap = usageLog.scanSummarize(
				new Period(0L, MAX_MS), crit);
		for (String critVal: summaryMap.keySet()) {
			System.out.printf("%s:%s Summary:%s\n", crit, critVal,
					summaryMap.get(critVal));
		}
	}

	/**
	 * This method takes Strings as parameters rather than
	 * InstanceUsageLog.GroupByCriterion's, because it's intended to be called
	 * from the command-line test harness. It converts the Strings into 
	 * GroupByCriterions. Possible values include "user","cluster",etc.
	 * 
	 * @throws IllegalArgumentException if criterion does not match
	 *   any InstanceUsageLog.GroupByCriterion
	 */
	public static void summarizeFalseDataTwoCriteria(
			String outerCriterion,
			String innerCriterion)
	{
		InstanceUsageLog.GroupByCriterion outerCrit = getCriterion(outerCriterion);
		InstanceUsageLog.GroupByCriterion innerCrit = getCriterion(innerCriterion);
		System.out.printf(" ----> PRINTING FALSE DATA BY %s,%s\n", outerCrit,
				innerCrit);

		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<String, Map<String, UsageSummary>> summaryMap =
			usageLog.scanSummarize(new Period(0L, MAX_MS),
					outerCrit, innerCrit);
		for (String outerCritVal: summaryMap.keySet()) {
			Map<String, UsageSummary> innerMap = summaryMap.get(outerCritVal);
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
		extends InstanceEventListener
	{
		private long fakeCurrentTimeMillis = 0l;
		
		void setCurrentTimeMillis(long currentTimeMillis)
		{
			this.fakeCurrentTimeMillis = currentTimeMillis;
		}
		
		protected long getCurrentTimeMillis()
		{
			return fakeCurrentTimeMillis;
		}
	}

}

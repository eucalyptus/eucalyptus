package com.eucalyptus.reporting.instance;

import java.util.*;

import com.eucalyptus.reporting.*;
import com.eucalyptus.reporting.event.InstanceEvent;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.upgrade.TestDescription;

/**
 * <p>FalseDataGenerator generates false data about instances. It generates
 * fake starting and ending times, imaginary resource usage, fictitious
 * clusters, and non-existent accounts and users.
 * 
 * <p>FalseDataGenerator is meant to be called from the command-line tool,
 * <pre>clc/tools/runTest.sh</pre>, specifying the class name and a method
 * below.
 * 
 * <p>False data should be deleted afterward by calling the
 * <pre>deleteFalseData</pre> method.
 * 
 * <p><i>"False data can come from many sources: academic, social,
 * professional... False data can cause one to make stupid mistakes..."</i>
 *   - L. Ron Hubbard
 */
@TestDescription("Generates false reporting data")
public class FalseDataGenerator
{
	private static final int NUM_USAGE    = 256;
	private static final int NUM_INSTANCE = 64;
	private static final long START_TIME  = 1104566400000l; //Jan 1, 2005 12:00AM
	private static final int TIME_USAGE_APART = 10000; //ms
	private static final long MAX_MS = ((NUM_USAGE+1) * TIME_USAGE_APART) + START_TIME;

	private static final int NUM_USER       = 32;
	private static final int NUM_ACCOUNT    = 16;
	private static final int NUM_CLUSTER    = 4;
	private static final int NUM_AVAIL_ZONE = 2;
	
	private static ReportingBootstrapper reportingBootstrapper = null;

	private enum FalseInstanceType
	{
		M1SMALL, C1MEDIUM, M1LARGE, M1XLARGE, C1XLARGE;
	}

	public static void generateFalseData()
	{
		System.out.println(" ----> GENERATING FALSE DATA");

		TestEventListener listener = new TestEventListener();
		listener.setCurrentTimeMillis(START_TIME);

		reportingBootstrapper = new ReportingBootstrapper();
		reportingBootstrapper.setOverriddenInstanceEventListener(listener);
		reportingBootstrapper.start();

		QueueSender	queueSender = QueueFactory.getInstance().getSender(QueueIdentifier.INSTANCE);


		List<InstanceAttributes> fakeInstances =
				new ArrayList<InstanceAttributes>();

		for (int i = 0; i < NUM_INSTANCE; i++) {

			String uuid = new Long(i).toString();
			String instanceId = String.format("instance-%d", (i % NUM_INSTANCE));
			String userId = String.format("user-%d", (i % NUM_USER));
			String accountId = String.format("account-%d", (i % NUM_ACCOUNT));
			String clusterName = String.format("cluster-%d", (i % NUM_CLUSTER));
			String availZone = String.format("zone-%d", (i % NUM_AVAIL_ZONE));
			FalseInstanceType[] vals = FalseInstanceType.values();
			String instanceType = vals[i % vals.length].toString();

			InstanceAttributes insAttrs = new InstanceAttributes(uuid,
					instanceId, instanceType, userId, accountId, clusterName,
					availZone);
			fakeInstances.add(insAttrs);
		}

		for (int i=0; i<NUM_USAGE; i++) {
			listener.setCurrentTimeMillis(START_TIME + (i * TIME_USAGE_APART));
			for (InstanceAttributes insAttrs : fakeInstances) {
				long instanceNum = Long.parseLong(insAttrs.getUuid());
				long netIoMegs = (instanceNum + i) * 1024;
				long diskIoMegs = (instanceNum + i*2) * 1024;
				InstanceEvent event = new InstanceEvent(insAttrs.getUuid(),
						insAttrs.getInstanceId(), insAttrs.getInstanceType(),
						insAttrs.getUserId(), insAttrs.getAccountId(),
						insAttrs.getClusterName(),
						insAttrs.getAvailabilityZone(), new Long(netIoMegs),
						new Long(diskIoMegs));
				System.out.println("Generating:" + i);
				queueSender.send(event);
			}
		}

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		reportingBootstrapper.stop();

	}

	private static final long CORRECT_DISK_USAGE = 990000l;
	private static final long CORRECT_NET_USAGE  = 495000l;
	private static final double ERROR_FACTOR = 0.1;
	
	
	public static void testFalseData()
	{
		System.out.println(" ----> TESTING FALSE DATA");
		
		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<String, InstanceUsageSummary> summary = usageLog.scanSummarize(new Period(START_TIME, MAX_MS), GroupByCriterion.USER);
		for (String key: summary.keySet()) {
			System.out.println(key + summary.get(key));
		}

		Map<String, TestResult> testResults = new HashMap<String, TestResult>();
		long sliceMs = (MAX_MS - START_TIME) / 10;
		for (long l=START_TIME; l < MAX_MS; l+=sliceMs) {
			System.out.printf(" Period:%d-%d\n", l, l+sliceMs-1);
			summary = usageLog.scanSummarize(new Period(l, l+sliceMs-1), GroupByCriterion.USER);
			for (String key: summary.keySet()) {
				InstanceUsageSummary ius = summary.get(key);
				System.out.println("  " + key + ius);
				if (!testResults.containsKey(key)) {
					testResults.put(key, new TestResult());
				}
				TestResult testResult = testResults.get(key);
				testResult.totalDiskUsage += ius.getDiskIoMegs();
				testResult.totalNetUsage  += ius.getNetworkIoMegs();
			}
		}
		
		System.out.println("Totals:");
		for (String key: testResults.keySet()) {
			TestResult testResult = testResults.get(key);
			System.out.printf(" %s:(disk:%d,net:%d) error:(disk:%5.3f,net:%5.3f) isWithin:(disk:%s,net:%s)\n",
					key, testResult.totalDiskUsage,	testResult.totalNetUsage,
					((double)testResult.totalDiskUsage / (double)CORRECT_DISK_USAGE),
					((double)testResult.totalNetUsage / (double)CORRECT_NET_USAGE),
					isWithinError(testResult.totalDiskUsage, CORRECT_DISK_USAGE, ERROR_FACTOR),
					isWithinError(testResult.totalNetUsage, CORRECT_NET_USAGE, ERROR_FACTOR));

			if (!isWithinError(testResult.totalDiskUsage, CORRECT_DISK_USAGE, ERROR_FACTOR)
				 || !isWithinError(testResult.totalNetUsage, CORRECT_NET_USAGE, ERROR_FACTOR))
			{
				throw new RuntimeException("Incorrect result for user:" + key);
			}
			
		}
		System.out.println("Test passed");
	}

	private static boolean isWithinError(long val, long correctVal, double errorPercent)
	{
		return ((double)correctVal * (1-(double)errorPercent)) < val
				&& val < ((double)correctVal * (1+(double)errorPercent));
	}

	private static class TestResult
	{
		TestResult() { }
		long totalDiskUsage = 0l;
		long totalNetUsage = 0l;
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

			System.out.printf("instance:%s type:%s user:%s account:%s cluster:%s"
					+ " zone:%s period:%d-%d netIo:%d diskIo:%d\n",
					insAttrs.getInstanceId(), insAttrs.getInstanceType(),
					insAttrs.getUserId(), insAttrs.getAccountId(),
					insAttrs.getClusterName(), insAttrs.getAvailabilityZone(),
					period.getBeginningMs(), period.getEndingMs(),
					usageData.getNetworkIoMegs(), usageData.getDiskIoMegs());
		}
	}

	public static void runTest()
	{
		removeFalseData();
		printFalseData();
		generateFalseData();
		printFalseData();
		testFalseData();
		removeFalseData();
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

		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<String, InstanceUsageSummary> summaryMap = usageLog.scanSummarize(
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

		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<String, Map<String, InstanceUsageSummary>> summaryMap =
			usageLog.scanSummarize(new Period(0L, MAX_MS),
					outerCrit, innerCrit);
		for (String outerCritVal: summaryMap.keySet()) {
			Map<String, InstanceUsageSummary> innerMap = summaryMap.get(outerCritVal);
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

	public static void main(String[] args) throws Exception
	{
		String methodName = args[0];
		Object[] methodArgsArray = new Object[args.length - 1];
		@SuppressWarnings("rawtypes")
		Class[] paramTypes = new Class[args.length - 1];
		System.out.println("Executing " + methodName);
		for (int i = 1; i < args.length; i++) {
			paramTypes[i - 1] = String.class;
			methodArgsArray[i - 1] = args[i];
			System.out.println(" param:" + args[i - 1]);
		}
		FalseDataGenerator.class.getDeclaredMethod(methodName, paramTypes)
				.invoke(null, methodArgsArray);
	}
	
}

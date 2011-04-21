package com.eucalyptus.reporting.instance;

import java.util.*;

import com.eucalyptus.reporting.*;
import com.eucalyptus.reporting.event.InstanceEvent;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.util.ExposedCommand;

/**
 * <p>FalseDataGenerator generates false data about instances. It generates
 * fake starting and ending times, imaginary resource usage, fictitious
 * clusters, and non-existent accounts and users.
 * 
 * <p>FalseDataGenerator is meant to be called from the
 * <code>CommandServlet</code>
 * 
 * <p>False data should be deleted afterward by calling the
 * <pre>deleteFalseData</pre> method.
 * 
 * <p><i>"False data can come from many sources: academic, social,
 * professional... False data can cause one to make stupid mistakes..."</i>
 *   - L. Ron Hubbard
 */
public class FalseDataGenerator
{
	private static final int NUM_USAGE    = 512;
	private static final int NUM_INSTANCE = 16;
	private static final long START_TIME  = 1104566400000l; //Jan 1, 2005 12:00AM
	private static final int TIME_USAGE_APART = 10000; //ms
	private static final long MAX_MS = ((NUM_USAGE+1) * TIME_USAGE_APART) + START_TIME;

	private static final int NUM_USER       = 8;
	private static final int NUM_ACCOUNT    = 4;
	private static final int NUM_CLUSTER    = 2;
	private static final int NUM_AVAIL_ZONE = 1;
	
	private enum FalseInstanceType
	{
		M1SMALL, C1MEDIUM, M1LARGE, M1XLARGE, C1XLARGE;
	}

	@ExposedCommand
	public static void generateFalseData()
	{
		System.out.println(" ----> GENERATING FALSE DATA");

		QueueSender	queueSender = QueueFactory.getInstance().getSender(QueueIdentifier.INSTANCE);

		QueueReceiver queueReceiver = QueueFactory.getInstance().getReceiver(QueueIdentifier.INSTANCE);
		TestEventListener listener = new TestEventListener();
		listener.setCurrentTimeMillis(START_TIME);
		queueReceiver.removeAllListeners(); //Remove non-test listeners set up by bootstrapper
		queueReceiver.addEventListener(listener);

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

	}

	private static final long CORRECT_DISK_USAGE = 1010000l;
	private static final long CORRECT_NET_USAGE  = 506000l;
	private static final long CORRECT_INSTANCE_USAGE = 4150l;
	private static final double ERROR_FACTOR = 0.2;
	private static final double INSTANCE_ERROR_FACTOR = 0.2;
	
	private static final long RANGE = MAX_MS - START_TIME;
	private static final long DISTANCE =  RANGE / 5;

	private static final int NUM_TESTS = 16;

	
	@ExposedCommand
	public static void testFalseData()
	{
		boolean allTestsPassed = true;
		System.out.println(" ----> TESTING FALSE DATA");

		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
		Map<String, InstanceUsageSummary> summary = usageLog.scanSummarize(new Period(START_TIME, MAX_MS), GroupByCriterion.USER);
		for (String key: summary.keySet()) {
			System.out.println(key + summary.get(key));
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
			final double adjustedCorrectDisk = (double)CORRECT_DISK_USAGE*fraction;
			final double adjustedCorrectNet  = (double)CORRECT_NET_USAGE*fraction;
			final double adjustedCorrectTime = (double)CORRECT_INSTANCE_USAGE*fraction;
			final double adjustedError = ERROR_FACTOR * (1.0 + (1.0-fraction));
			final double adjustedInstanceError = INSTANCE_ERROR_FACTOR * (1.0 + (1.0-fraction));

			System.out.printf("#:%3d correct:(%d,%d,%d) fraction:%3.3f adjusted:(%3.3f , %3.3f , %3.3f)"
					   + " adjustedError:(%3.3f , %3.3f)\n",
					i, CORRECT_DISK_USAGE, CORRECT_NET_USAGE, CORRECT_INSTANCE_USAGE,
					fraction, adjustedCorrectDisk, adjustedCorrectNet, adjustedCorrectTime,
					adjustedError, adjustedInstanceError);

			summary = usageLog.scanSummarize(new Period(lowerBound, upperBound), GroupByCriterion.USER);
			for (String userId: summary.keySet()) {
				InstanceUsageSummary ius = summary.get(userId);
				long totalUsageSecs = ius.getM1SmallTimeSecs()
						+ ius.getC1MediumTimeSecs() + ius.getM1LargeTimeSecs()
						+ ius.getM1XLargeTimeSecs() + ius.getC1XLargeTimeSecs();

				final double diskError = (double)ius.getDiskIoMegs() / adjustedCorrectDisk;
				final double netError = (double)ius.getNetworkIoMegs() / adjustedCorrectNet;;
				final double usageError = ((double)totalUsageSecs / adjustedCorrectTime);
				final boolean diskWithin = isWithinError(ius.getDiskIoMegs(), adjustedCorrectDisk, adjustedError);
				final boolean netWithin = isWithinError(ius.getNetworkIoMegs(), adjustedCorrectNet, adjustedError);
				final boolean usageWithin = isWithinError(totalUsageSecs, adjustedCorrectTime, adjustedInstanceError);

				System.out.printf(" %8s:(%d/%3.3f , %d/%3.3f , %d/%3.3f) " +
						  "error:(%3.3f,%3.3f,%3.3f) isWithin:(%s,%s,%s)\n",
						userId, ius.getDiskIoMegs(), adjustedCorrectDisk,
						ius.getNetworkIoMegs(), adjustedCorrectNet, totalUsageSecs,
						adjustedCorrectTime, diskError, netError, usageError,
						diskWithin, netWithin, usageWithin);

				if (!diskWithin || !netWithin || !usageWithin)
				{
					System.out.println("Incorrect result for user:" + userId);
					allTestsPassed = false;
				}

			}

		}



		/* Divide the entire test period into 10 intervals and verify that each
		 * of the ten intervals has the correct usage within some error margin.
		 */
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
				testResult.m1SmallTimeSecs += ius.getM1SmallTimeSecs();
				testResult.c1MediumTimeSecs += ius.getC1MediumTimeSecs();
				testResult.m1LargeTimeSecs += ius.getM1LargeTimeSecs();
				testResult.m1XLargeTimeSecs += ius.getM1XLargeTimeSecs();
				testResult.c1XLargeTimeSecs += ius.getC1XLargeTimeSecs();
			}
		}
		
		/* Verify that the sum of the usage during the ten intervals adds up to
		 * roughly the total for the entire interval.
		 */
		System.out.println("Totals:");
		for (String key: testResults.keySet()) {
			TestResult testResult = testResults.get(key);
			
			/* Calculate data, to verify and to print in chart
			 */
			long totalUsageSecs = testResult.m1SmallTimeSecs
					+ testResult.c1MediumTimeSecs + testResult.m1LargeTimeSecs
					+ testResult.m1XLargeTimeSecs + testResult.c1XLargeTimeSecs;
			double diskError = ((double)testResult.totalDiskUsage / (double)CORRECT_DISK_USAGE);
			double netError = ((double)testResult.totalNetUsage / (double)CORRECT_NET_USAGE);
			boolean diskWithin = isWithinError(testResult.totalDiskUsage, CORRECT_DISK_USAGE, ERROR_FACTOR);
			boolean netWithin = isWithinError(testResult.totalNetUsage, CORRECT_NET_USAGE, ERROR_FACTOR);
			double totalError = ((double)totalUsageSecs / (double)CORRECT_INSTANCE_USAGE);
			boolean totalWithin = isWithinError(totalUsageSecs, CORRECT_INSTANCE_USAGE, INSTANCE_ERROR_FACTOR);
			
			/* One big fat printf which prints all the test results in a single big ass chart
			 */
			System.out.printf(" %8s:(disk:%d,net:%d) error:(disk:%5.3f,net:%5.3f) isWithin:(disk:%s,net:%s)"
					+ " instance:(m1s:%d,c1m:%d,m1l:%d,m1xl:%d,c1xl:%d) totalError:%5.3f isWithin:%s\n",
					key, testResult.totalDiskUsage,	testResult.totalNetUsage,
					diskError, netError, diskWithin, netWithin,	testResult.m1SmallTimeSecs,
					testResult.c1MediumTimeSecs, testResult.m1LargeTimeSecs,
					testResult.m1XLargeTimeSecs, testResult.c1XLargeTimeSecs,
					totalError, totalWithin);

			if (!diskWithin || !netWithin || !totalWithin)
			{
				System.out.println("Incorrect result for user:" + key);
			}
			
		}

		if (! allTestsPassed) throw new RuntimeException("Test failed"); //TODO: throw different exception here
	}

	private static boolean isWithinError(long val, long correctVal, double errorPercent)
	{
		return isWithinError((double)val, (double)correctVal, errorPercent);
	}

	private static boolean isWithinError(double val, double correctVal, double errorPercent)
	{
		return correctVal * (1-errorPercent) < val
				&& val < correctVal * (1+errorPercent);
	}

	private static class TestResult
	{
		TestResult() { }
		long totalDiskUsage = 0l;
		long totalNetUsage = 0l;
		long m1SmallTimeSecs = 0l;
		long c1MediumTimeSecs = 0l;
		long m1LargeTimeSecs = 0l;
		long m1XLargeTimeSecs = 0l;
		long c1XLargeTimeSecs = 0l;
	}
	
	@ExposedCommand
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
	@ExposedCommand
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
	@ExposedCommand
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

}

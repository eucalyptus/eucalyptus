package com.eucalyptus.reporting.instance;

import java.io.*;
import java.util.*;

import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.reporting.*;
import com.eucalyptus.reporting.event.InstanceEvent;
import com.eucalyptus.reporting.modules.instance.*;
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
	private static final int NUM_USAGE    = 2048;
	private static final int NUM_INSTANCE = 32;
	private static final long START_TIME  = 1104566400000l; //Jan 1, 2005 12:00AM
	private static final int TIME_USAGE_APART = 150000; //ms
	private static final long MAX_MS = ((NUM_USAGE+1) * TIME_USAGE_APART) + START_TIME;

	private static final int NUM_USER       = 16;
	private static final int NUM_ACCOUNT    = 8;
	private static final int NUM_CLUSTER    = 4;
	private static final int NUM_AVAIL_ZONE = 2;
	
	private enum FalseInstanceType
	{
		M1SMALL("m1.small"),
		C1MEDIUM("c1.medium"),
		M1LARGE("m1.large"),
		M1XLARGE("m1.xlarge"),
		C1XLARGE("c1.xlarge");
		
		private final String name;
		
		private FalseInstanceType(String name) {
			this.name = name;
		}
		
		public String toString() { return name; }
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

		for (int i=0; i<NUM_USAGE; i++) {
			
			listener.setCurrentTimeMillis(START_TIME + (i * TIME_USAGE_APART));
			for (int j=0; j<NUM_INSTANCE; j++) {
				
				String uuid = new Long(j).toString();
				String instanceId = String.format("instance-%d", (j % NUM_INSTANCE));
				String userId = String.format("fakeUserId-%d", (j % NUM_USER));
				String userName = String.format("fakeUserName:%d", (j % NUM_USER));
				String accountId = String.format("fakeAccountId-%d", (j % NUM_ACCOUNT));
				String accountName = String.format("fakeAccountName:%d", (j % NUM_ACCOUNT));
				String clusterName = String.format("cluster-%d", (j % NUM_CLUSTER));
				String availZone = String.format("zone-%d", (j % NUM_AVAIL_ZONE));
				FalseInstanceType[] vals = FalseInstanceType.values();
				String instanceType = vals[j % vals.length].toString();
				long instanceNum = Long.parseLong(uuid);
				long netIoMegs = (instanceNum + i) * 1024;
				long diskIoMegs = (instanceNum + i*2) * 1024;

				InstanceEvent event = new InstanceEvent(uuid, instanceId,
						instanceType, userId, userName, accountId,
						accountName, clusterName, availZone, new Long(netIoMegs),
						new Long(diskIoMegs));

				System.out.println("Generating:" + i);
				queueSender.send(event);
				
			}
			
		}

	}

	private static final long ERROR_MARGIN_MS = 60*60*1000;


	private static boolean isWithinError(long val, long correctVal, long error)
	{
		return ((correctVal - error) < val) && (val < (correctVal + error));
	}


	@ExposedCommand
	public static void generateTestReport()
	{
		System.out.println(" ----> GENERATING TEST REPORT");

		try {
			OutputStream os = new FileOutputStream("/tmp/testReport.csv");
			ReportGenerator.getInstance().generateReport("Instance", ReportFormat.CSV, new Period(1104566480000l, 1104571200000l),
					ReportingCriterion.USER, null, null, os, "admin");
			os.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@ExposedCommand
	public static void removeFalseData()
	{
		System.out.println(" ----> REMOVING FALSE DATA");

		InstanceUsageLog.getInstanceUsageLog().purgeLog(MAX_MS);
	}


	@ExposedCommand
	public static void removeAllData()
	{
		System.out.println(" ----> REMOVING ALL DATA");

		InstanceUsageLog.getInstanceUsageLog().purgeLog(Long.MAX_VALUE);		
	}

	/**
	 * <p>containsRecentRows checks if there are recent rows in 
	 * InstanceUsageSnapshot. This is used for testing: we delete
	 * all data, then set up volumes, then determine if rows made
	 * it to the DB.
	 * 
	 * @param hasRows Indicates whether there should be any
	 * rows in InstanceUsageSnapshot. If true, and there are no rows,
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
		
		EntityWrapper<InstanceUsageSnapshot> entityWrapper =
			EntityWrapper.get(InstanceUsageSnapshot.class);
		
		try {
			int rowCnt = 0;
			@SuppressWarnings("rawtypes")
			Iterator iter =
				entityWrapper.createQuery(
					"from InstanceUsageSnapshot as sus")
					.iterate();
			while (iter.hasNext()) {
				if (!containsRows) {
					throw new RuntimeException("Found >0 rows where 0 expected");
				}
				rowCnt++;
				InstanceUsageSnapshot snapshot = (InstanceUsageSnapshot) iter.next();
				long foundTimestampMs = snapshot.getTimestampMs();
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
//		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
//		System.out.println(" ----> PRINTING FALSE DATA");
//		for (InstanceUsageLog.LogScanResult result: usageLog.scanLog(new Period(0L, MAX_MS))) {
//
//			InstanceAttributes insAttrs = result.getInstanceAttributes();
//			Period period = result.getPeriod();
//			InstanceUsageData usageData = result.getUsageData();
//
//			System.out.printf("instance:%s type:%s user:%s account:%s cluster:%s"
//					+ " zone:%s period:%d-%d netIo:%d diskIo:%d\n",
//					insAttrs.getInstanceId(), insAttrs.getInstanceType(),
//					insAttrs.getUserId(), insAttrs.getAccountId(),
//					insAttrs.getClusterName(), insAttrs.getAvailabilityZone(),
//					period.getBeginningMs(), period.getEndingMs(),
//					usageData.getNetworkIoMegs(), usageData.getDiskIoMegs());
//		}
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
		
		@Override
		protected long getCurrentTimeMillis()
		{
			System.out.println("Fake time millis:" + fakeCurrentTimeMillis);
			return fakeCurrentTimeMillis;
		}
	}

}

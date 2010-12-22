package com.eucalyptus.reporting.instance;

import java.util.*;

import org.junit.*;
import com.eucalyptus.reporting.event.InstanceEvent;

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
public class FalseDataGenerator
{
	private static final int NUM_USAGE = 4096;
	private static final int NUM_INSTANCE = 128;
	private static final int TIME_USAGE_APART = 1000; //ms
	private static final long MAX_MS = ((NUM_USAGE+1) * TIME_USAGE_APART);

	private static final int NUM_USER = 32;
	private static final int NUM_CLUSTER = 4;
	private static final int NUM_AVAIL_ZONE = 2;

	private enum FalseInstanceType
	{
		TINY, SMALL, MEDIUM, BIG;
	}

	@Test
	public void generateFalseData()
	{
		List<InstanceAttributes> fakeInstances =
				new ArrayList<InstanceAttributes>();
		
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
				listener.receiveEvent(event);
			}
		}
		
	}

	@Test
	public void removeFalseData()
	{
		InstanceUsageLog.getInstanceUsageLog().purgeLog(MAX_MS);
	}
	
	@Test
	public void printFalseData()
	{
		InstanceUsageLog usageLog = InstanceUsageLog.getInstanceUsageLog();
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

	/**
	 * TestEventListener provides fake times which you can modify.
	 * 
	 * @author twerges
	 */
	private class TestEventListener
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

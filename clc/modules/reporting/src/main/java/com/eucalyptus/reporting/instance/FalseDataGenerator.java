package com.eucalyptus.reporting.instance;

import java.util.*;

import com.eucalyptus.reporting.event.EventListener;
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
	private static final int DEFAULT_NUM_USAGE = 4096;
	private static final int DEFAULT_NUM_INSTANCE = 128;
	private static final int DEFAULT_TIME_USAGE_APART = 1000; //ms

	private static final int NUM_USER = 32;
	private static final int NUM_CLUSTER = 4;
	private static final int NUM_AVAIL_ZONE = 2;

	private enum FalseInstanceType
	{
		TINY, SMALL, MEDIUM, BIG;
	}

	/**
	 * @return The maximum timestamp of any fake instance (so you can
	 * 		subsequently purge the fake instances using InstanceUsageLog
	 * 		.purge()).
	 */
	public long generateFakeInstances(int numInstance, int numUsage,
			int timeUsageApartMs)
	{
		long maxTimeMs = 0l;
		List<InstanceAttributes> fakeInstances =
				new ArrayList<InstanceAttributes>();
		
		for (int i = 0; i < numInstance; i++) {
			String uuid = new Long(i).toString();
			String instanceId = String.format("instance-%d", (i % numInstance));
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
		for (int i=0; i<numUsage; i++) {
			listener.setCurrentTimeMillis(i * timeUsageApartMs);
			maxTimeMs = Math.max(maxTimeMs, i * timeUsageApartMs);
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
		
		return maxTimeMs+1;
	}

	public static void main(String[] args)
		throws Exception
	{
		int numInstances = (args.length > 0)
				? Integer.parseInt(args[0])
				: DEFAULT_NUM_INSTANCE;
		int numUsages = (args.length > 1)
				? Integer.parseInt(args[1])
				: DEFAULT_NUM_USAGE;
		int timeUsageApartMs = (args.length > 2)
				? Integer.parseInt(args[2])
				: DEFAULT_TIME_USAGE_APART;

		long maxMs =
			new FalseDataGenerator().generateFakeInstances(numInstances,
					numUsages, timeUsageApartMs);
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

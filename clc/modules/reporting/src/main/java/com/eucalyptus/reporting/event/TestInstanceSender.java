package com.eucalyptus.reporting.event;

import com.eucalyptus.reporting.star_queue.StarSender;

public class TestInstanceSender
{
	private static final int NUM_INSTANCES = 1000;
	private static final int NUM_PERIODS = 10000;

	public TestInstanceSender()
	{
	}

	/**
	 * This relies upon the broker having been started already by the uber-test.
	 */
	private void test()
		throws Exception
	{
		for (int i = 0; i < NUM_PERIODS; i++) {
			long cumulativeNetIoMegs = i * 2;
			long cumulativeDiskIoMegs = i * 3;
			for (int j = 0; j < NUM_INSTANCES; j++) {
				String uuid = String.format("uuid:%d", j);
				//period is embedded in instanceId during test, used for test elsewhere
				String instanceId = String.format("instanceId:%d:periodId:%d", j, i);
				String instanceType = "small";
				String userId = String.format("user:%d", j % 20);
				String clusterName = String.format("cluster:%d", j % 4);
				String availabilityZone = String.format("zone:%d", j % 2);
				InstanceEvent event = new InstanceEvent(uuid, instanceId,
						instanceType, userId, clusterName, availabilityZone,
						cumulativeNetIoMegs, cumulativeDiskIoMegs);
				StarSender.getInstance().send(event);
			}
		}
	}

}

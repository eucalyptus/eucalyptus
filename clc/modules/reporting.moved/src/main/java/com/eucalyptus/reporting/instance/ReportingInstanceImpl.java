package com.eucalyptus.reporting.instance;

import java.util.*;

public class ReportingInstanceImpl
	implements ReportingInstance
{
	private final String uuid;
	private final String instanceId;
	private final String instanceType;
	private final String userId;
	private final String clusterName;
	private final String availabilityZone;
	private final List<UsageSnapshot> snapshotList;
	
	ReportingInstanceImpl(String uuid, String instanceId, String instanceType,
			String userId, String clusterName, String availabilityZone)
	{
		this.uuid = uuid;
		if (uuid == null)
			throw new IllegalArgumentException("Uuid cannot be null");
		this.instanceId = instanceId;
		this.instanceType = instanceType;
		this.userId = userId;
		this.clusterName = clusterName;
		this.availabilityZone = availabilityZone;
		this.snapshotList = new ArrayList<UsageSnapshot>();
	}
	
	@Override
	public String getUuid()
	{
		return uuid;
	}
	
	@Override
	public String getInstanceId()
	{
		return instanceId;
	}
	
	@Override
	public String getInstanceType()
	{
		return instanceType;
	}
	
	@Override
	public String getUserId()
	{
		return userId;
	}
	
	@Override
	public String getClusterName()
	{
		return clusterName;
	}
	
	@Override
	public String getAvailabilityZone()
	{
		return availabilityZone;
	}

	void addSnapshot(UsageSnapshot snapshot)
	{
		this.snapshotList.add(snapshot);
	}
	
	@Override
	public UsageData getUsageData(Period period)
	{
		assert snapshotList.size() > 0;  //always filled by db join with >=1 periods
		UsageSnapshot latestSnapshot = snapshotList.get(0);
		UsageSnapshot earliestSnapshot = snapshotList.get(0);
		
		for (UsageSnapshot snapshot: snapshotList) {
			if (snapshot.getTimestampMs() < earliestSnapshot.getTimestampMs()) {
				earliestSnapshot = snapshot;
			}
			if (snapshot.getTimestampMs() > latestSnapshot.getTimestampMs()) {
				latestSnapshot = snapshot;
			}
		}

		UsageData earliestUsageData = earliestSnapshot.getCumulativeUsageData();
		UsageData latestUsageData = latestSnapshot.getCumulativeUsageData();
		return earliestUsageData.subtractFrom(latestUsageData);
	}

}

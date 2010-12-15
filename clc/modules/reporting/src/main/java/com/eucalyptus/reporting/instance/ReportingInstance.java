package com.eucalyptus.reporting.instance;

public interface ReportingInstance
{
	public String getUuid();

	public String getInstanceId();

	public String getInstanceType();

	public String getUserId();

	public String getClusterName();

	public String getAvailabilityZone();

	/**
	 * Get the usage of this instance over some period. Note that this object
	 * contains only a <i>subset</i> of the data in the log. 
	 */
	public UsageData getUsageData(Period period);
}
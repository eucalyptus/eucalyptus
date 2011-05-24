package com.eucalyptus.reporting;

/**
 * <p>ReportingCriterion is a criterion for elements in a visual report;
 * for example, you could generate a report by "user" or "cluster".
 * 
 * @author tom.werges
 */
public enum ReportingCriterion
{
	AVAILABILITY_ZONE("Availability Zone"),
	CLUSTER("Cluster"),
	ACCOUNT("Account"),
	USER("User");
	
	private final String name;
	
	private ReportingCriterion(String name)
	{
		this.name = name;
	}
	
	public String toString()
	{
		return this.name;
	}
	
}

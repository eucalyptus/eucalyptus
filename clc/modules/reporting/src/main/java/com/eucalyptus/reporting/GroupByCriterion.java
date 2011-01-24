package com.eucalyptus.reporting;

/**
 * <p>GroupByCriterion is a criterion which can be used for grouping or 
 * aggregation of elements in a visual report.
 * 
 * @author tom.werges
 */
public enum GroupByCriterion
{
	USER("User"),
	ACCOUNT("Account"),
	CLUSTER("Cluster"),
	AVAILABILITY_ZONE("Availability Zone");
	
	private final String name;
	
	private GroupByCriterion(String name)
	{
		this.name = name;
	}
	
	public String toString()
	{
		return this.name;
	}
	
}

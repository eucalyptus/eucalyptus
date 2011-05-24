package com.eucalyptus.reporting;

public enum ReportType
{
	S3
		("S3", "s3.jrxml", "nested_s3.jrxml", ReportingCriterion.USER,
		ReportingCriterion.ACCOUNT),

	STORAGE
		("Storage", "storage.jrxml", "nested_storage.jrxml",
		ReportingCriterion.USER, ReportingCriterion.ACCOUNT,
		ReportingCriterion.CLUSTER, ReportingCriterion.AVAILABILITY_ZONE),

	INSTANCE
		("Instance", "instance.jrxml", "nested_instance.jrxml",
		ReportingCriterion.USER, ReportingCriterion.ACCOUNT,
		ReportingCriterion.CLUSTER, ReportingCriterion.AVAILABILITY_ZONE);
	
	private final String reportName;
	private final String jrxmlFilename;
	private final String nestedJrxmlFilename;
	private final ReportingCriterion[] allowedCriteria;
	
	private ReportType(String reportName, String jrxmlFilename,
			String nestedJrxmlFilename, ReportingCriterion... allowedCriteria)
	{
		this.reportName = reportName;
		this.jrxmlFilename = jrxmlFilename;
		this.nestedJrxmlFilename = nestedJrxmlFilename;
		this.allowedCriteria = allowedCriteria;
	}
	
	public String getReportName()
	{
		return this.reportName;
	}
	
	public String getJrxmlFilename()
	{
		return this.jrxmlFilename;
	}
	
	public String getNestedJrxmlFilename()
	{
		return this.nestedJrxmlFilename;
	}
	
	public ReportingCriterion[] getAllowedCriteria()
	{
		return this.allowedCriteria;
	}
	
}

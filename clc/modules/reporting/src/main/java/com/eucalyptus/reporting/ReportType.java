package com.eucalyptus.reporting;

public enum ReportType
{
	S3
		("S3", "s3.jrxml", "nested_s3.jrxml"),

	STORAGE
		("Storage", "storage.jrxml", "nested_storage.jrxml"),

	INSTANCE
		("Instance", "instance.jrxml", "nested_instance.jrxml");
	
	private final String reportName;
	private final String jrxmlFilename;
	private final String nestedJrxmlFilename;
	
	private ReportType(String reportName, String jrxmlFilename,
			String nestedJrxmlFilename)
	{
		this.reportName = reportName;
		this.jrxmlFilename = jrxmlFilename;
		this.nestedJrxmlFilename = nestedJrxmlFilename;
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
	
	
}

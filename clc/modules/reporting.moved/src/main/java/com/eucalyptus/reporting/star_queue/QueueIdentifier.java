package com.eucalyptus.reporting.star_queue;

enum QueueIdentifier
{
	INSTANCE ("InstanceReportingQueue"),
	S3 ("S3ReportingQueue"),
	EBS ("EbsReportingQueue");

	private final String queueName;

	private QueueIdentifier(String queueName)
	{
		this.queueName = queueName;
	}

	public String getQueueName()
	{
		return this.queueName;
	}
}

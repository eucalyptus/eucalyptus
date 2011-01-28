package com.eucalyptus.reporting.star_queue;

public class QueueRuntimeException
	extends RuntimeException
{
	public QueueRuntimeException(Exception ex)
	{
		super(ex);
	}
}


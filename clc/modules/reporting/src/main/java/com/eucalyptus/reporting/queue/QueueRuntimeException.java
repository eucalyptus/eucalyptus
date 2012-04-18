package com.eucalyptus.reporting.queue;

public class QueueRuntimeException
	extends RuntimeException
{
	public QueueRuntimeException(String msg)
	{
		super(msg);
	}
	
	public QueueRuntimeException(Exception ex)
	{
		super(ex);
	}
}


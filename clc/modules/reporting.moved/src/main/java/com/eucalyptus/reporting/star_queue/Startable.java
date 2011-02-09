package com.eucalyptus.reporting.star_queue;

public interface Startable
{
	public void startup()
		throws StartableException;

	public void shutdown()
		throws StartableException;

}

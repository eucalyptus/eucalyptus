package com.eucalyptus.reporting.event;

public interface Event
	extends com.eucalyptus.event.Event
{
	public boolean requiresReliableTransmission();
}

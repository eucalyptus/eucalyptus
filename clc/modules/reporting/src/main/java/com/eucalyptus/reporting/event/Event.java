package com.eucalyptus.reporting.event;

public interface Event
	extends java.io.Serializable
{
	public boolean requiresReliableTransmission();
}

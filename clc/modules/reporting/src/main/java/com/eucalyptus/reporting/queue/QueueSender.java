package com.eucalyptus.reporting.queue;

import com.eucalyptus.reporting.event.Event;

public interface QueueSender
{
	public void send(Event e);
}

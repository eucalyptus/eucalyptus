package com.eucalyptus.reporting.queue;

import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.event.EventListener;

public interface QueueReceiver
{
	public void addEventListener(EventListener<Event> el);

	public void removeEventListener(EventListener<Event> el);
	
	public void removeAllListeners();
	
	/**
	 * @return Null if no event is available
	 */
	public Event receiveEventNoWait();
}

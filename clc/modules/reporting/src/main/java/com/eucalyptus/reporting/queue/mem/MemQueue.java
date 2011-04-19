package com.eucalyptus.reporting.queue.mem;

import java.util.*;

import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.queue.QueueReceiver;
import com.eucalyptus.reporting.queue.QueueSender;

public class MemQueue
	implements QueueReceiver, QueueSender
{
	private LinkedList<Event> linkedList = new LinkedList<Event>();
	private List<EventListener<Event>> listeners = new ArrayList<EventListener<Event>>();
	
	@Override
	public void send(Event e)
	{
		if (linkedList.size() > 0) {
			for (EventListener<Event> listener : listeners) {
				listener.fireEvent(e);
			}
		} else {
			linkedList.offer(e);
		}
	}

	@Override
	public void addEventListener(EventListener<Event> el)
	{
		listeners.add(el);
	}

	@Override
	public void removeEventListener(EventListener<Event> el)
	{
		listeners.remove(el);
	}

	@Override
	public Event receiveEventNoWait()
	{
		return linkedList.poll();
	}
}

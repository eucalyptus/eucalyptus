/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.reporting.queue.mem;

import java.util.*;

import org.apache.log4j.Logger;

import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.queue.*;

public class MemQueue
	implements QueueReceiver, QueueSender
{
	private static Logger log = Logger.getLogger( MemQueue.class );

	private LinkedList<Event> linkedList = new LinkedList<Event>();
	private List<EventListener<Event>> listeners = new ArrayList<EventListener<Event>>();
	
	@Override
	public void send(Event e)
	{
		log.info("Event sent: " + e);

		for (EventListener<Event> listener : listeners) {
			listener.fireEvent(e);
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

	@Override
	public void removeAllListeners()
	{
		listeners.clear();
	}
}

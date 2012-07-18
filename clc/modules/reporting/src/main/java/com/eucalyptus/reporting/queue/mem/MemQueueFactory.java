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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;

public class MemQueueFactory
	implements InternalQueueFactory
{
	private static Logger log = Logger.getLogger( MemQueueFactory.class );

	private Map<QueueIdentifier, MemQueue> queueMap;

	@Override
	public void startup()
	{
		queueMap = new HashMap<QueueIdentifier, MemQueue>();
		for (QueueIdentifier id : QueueIdentifier.values()) {
			queueMap.put(id, new MemQueue());
		}
		log.info("MemQueueFactory started");
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public QueueReceiver getReceiver(QueueIdentifier identifier)
	{
		return queueMap.get(identifier);
	}

	@Override
	public QueueSender getSender(QueueIdentifier idenfitifer)
	{
		return queueMap.get(idenfitifer);
	}

}

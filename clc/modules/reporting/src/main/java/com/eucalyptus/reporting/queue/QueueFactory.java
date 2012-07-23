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

package com.eucalyptus.reporting.queue;

import org.apache.log4j.Logger;

import com.eucalyptus.reporting.queue.mem.MemQueueFactory;
import com.eucalyptus.reporting.queue.mq.MqQueueFactory;

public class QueueFactory
{
	private static Logger log = Logger.getLogger( QueueFactory.class );

	private static QueueFactory queueFactory = null;
	private InternalQueueFactory internalFactory = null;
	
	private static final boolean USE_MEM_QUEUE = true;
	
	public static QueueFactory getInstance()
	{
		if (queueFactory == null) {
			queueFactory = new QueueFactory();
		}
		return queueFactory;
	}
	
	private QueueFactory()
	{
		internalFactory = USE_MEM_QUEUE
				? new MemQueueFactory()
				: new MqQueueFactory();
	}

	public enum QueueIdentifier
	{
		INSTANCE("InstanceQueue"),
		STORAGE("StorageQueue"),
		S3("S3");
		
		private final String queueName;

		private QueueIdentifier(String queueName)
		{
			this.queueName = queueName;
		}

		public String getQueueName()
		{
			return this.queueName;
		}
	}

	public void startup()
	{
		internalFactory.startup();
	}
	
	public void shutdown()
	{
		internalFactory.shutdown();
	}
	
	public QueueSender getSender(QueueIdentifier identifier)
	{
		return internalFactory.getSender(identifier);
	}

	public QueueReceiver getReceiver(QueueIdentifier identifier)
	{
		return internalFactory.getReceiver(identifier);
	}

}

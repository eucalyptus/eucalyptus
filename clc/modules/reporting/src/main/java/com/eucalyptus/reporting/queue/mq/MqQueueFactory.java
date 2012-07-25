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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.reporting.queue.mq;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.eucalyptus.event.EventListener;
import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;

public class MqQueueFactory
	implements InternalQueueFactory
{
	private static Logger log = Logger.getLogger( MqQueueFactory.class );

	private Map<QueueIdentifier,MqQueueSenderImpl>   senders;
	private Map<QueueIdentifier,MqQueueReceiverImpl> receivers;
	private boolean started = false;

	//TODO:
	private static String clientUrl = "failover:(" + QueueBroker.DEFAULT_URL + ")?initialReconnectDelay=10000&maxReconnectAttempts=10";

	public MqQueueFactory()
	{
		this.senders   = new HashMap<QueueIdentifier,MqQueueSenderImpl>();
		this.receivers = new HashMap<QueueIdentifier,MqQueueReceiverImpl>();
	}

	public void startup()
	{
		if (!started) {
			started = true;
			log.info("QueueFactory started");
		} else {
			log.warn("QueueFactory started redundantly");
		}
	}
	
	public void shutdown()
	{
		if (started) {
			for (QueueIdentifier identifier : senders.keySet()) {
				senders.get(identifier).shutdown();
			}
			for (QueueIdentifier identifier : receivers.keySet()) {
				receivers.get(identifier).shutdown();
			}
			log.info("QueueFactory stopped");
		} else {
			log.warn("QueueFactory.shutdown called when not started");
		}
	}
	
	public QueueSender getSender(QueueIdentifier identifier)
	{
		if (senders.containsKey(identifier)) {
			return senders.get(identifier);
		} else {
			log.info("Client url:" + clientUrl);
			MqQueueSenderImpl sender = new MqQueueSenderImpl(clientUrl, identifier);
			sender.startup();
			senders.put(identifier, sender);
			log.info("Sender " + identifier + " started");
			return sender;
		}
	}

	public QueueReceiver getReceiver(QueueIdentifier identifier)
	{
		if (receivers.containsKey(identifier)) {
			return receivers.get(identifier);
		} else {
			log.info("Client url:" + clientUrl);
			MqQueueReceiverImpl receiver = new MqQueueReceiverImpl(clientUrl,
					identifier);
			receiver.startup();
			receivers.put(identifier, receiver);
			log.info("Receiver " + identifier + " started");
			return receiver;
		}		
	}
	
	public static void main(String[] args)
		throws Exception
	{
		QueueIdentifier identifier =
			(args[0].equalsIgnoreCase("storage"))
				? QueueIdentifier.STORAGE
				: QueueIdentifier.INSTANCE;
		boolean listener = (args[1].equalsIgnoreCase("nowait")) ? false : true;
		System.out.println("Running listener for queue " + identifier + " as " + (listener ? "listener" : "noWait"));
		QueueFactory queueFactory = QueueFactory.getInstance();
		QueueReceiver receiver = queueFactory.getReceiver(identifier);
		if (listener) {
			receiver.addEventListener(new EventListener<Event>()
			{
				@Override
				public void fireEvent(Event e)
				{
					System.out.println("Event received:" + e);
				}

			});
		} else {
			for (Event event = receiver.receiveEventNoWait();
					event != null;
					event = receiver.receiveEventNoWait())
			{
				System.out.println("Event received:" + event);				
			}

		}
	}


}

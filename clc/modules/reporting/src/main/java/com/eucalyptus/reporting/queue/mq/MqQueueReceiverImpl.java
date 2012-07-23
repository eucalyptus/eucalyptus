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

package com.eucalyptus.reporting.queue.mq;

import java.util.*;

import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.log4j.Logger;

import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueReceiver;
import com.eucalyptus.event.EventListener;

class MqQueueReceiverImpl
	implements QueueReceiver, MessageListener
{
	private static Logger log = Logger.getLogger( MqQueueReceiverImpl.class );

	private final String brokerUrl;
	private final QueueFactory.QueueIdentifier identifier;
	
	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private MessageConsumer consumer;
	private List<EventListener<Event>> listeners;

	MqQueueReceiverImpl(String brokerUrl, QueueFactory.QueueIdentifier identifier)
	{
		this.brokerUrl = brokerUrl;
		this.identifier = identifier;
		this.listeners = new ArrayList<EventListener<Event>>();
	}

	void startup()
	{
		try {
			connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			ActiveMQQueue queue = new ActiveMQQueue(identifier.getQueueName());
			this.consumer = session.createConsumer(queue);
		} catch (JMSException jmse) {
			throw new QueueRuntimeException(jmse);
		}
		log.info("MqQueueReceiverImpl started");

	}
	
	void shutdown()
	{
		try {
			session.close();
			connection.stop();
		} catch (JMSException jmse) {
			throw new QueueRuntimeException(jmse);
		}	
		log.info("MqQueueReceiverImpl stopped");
	}
	
	@Override
	public void addEventListener(EventListener<Event> el)
	{
		try {
			if (this.listeners.size()==0) {
				this.consumer.setMessageListener(this);
			}
		} catch (JMSException ex) {
			throw new QueueRuntimeException(ex);
		}
		this.listeners.add(el);
	}

	@Override
	public void removeEventListener(EventListener<Event> el)
	{
		this.listeners.remove(el);
	}

	@Override
	public Event receiveEventNoWait()
	{
		try {
			ObjectMessage objMessage = (ObjectMessage) this.consumer.receiveNoWait();
			return (objMessage != null) ? (Event) objMessage.getObject() : null;
		} catch (JMSException jmse) {
			throw new QueueRuntimeException(jmse);
		}		
	}

	@Override
	public void onMessage(Message message)
	{
		log.debug("Message received");
		Event event = null;
		try {
			event = (Event) ((ObjectMessage)message).getObject();
		} catch (ClassCastException cce) {
			log.error("Message received of invalid type");
		} catch (JMSException jmse) {
			throw new QueueRuntimeException(jmse);
		}
		for (EventListener<Event> el: listeners) {
			el.fireEvent(event);
		}
	}

	@Override
	public void removeAllListeners()
	{
		listeners.clear();
	}
}

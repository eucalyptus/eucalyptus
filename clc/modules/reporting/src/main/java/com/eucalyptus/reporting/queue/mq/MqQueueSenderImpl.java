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

import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.log4j.Logger;

import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.reporting.queue.QueueSender;

class MqQueueSenderImpl
	implements QueueSender
{
	private static Logger log = Logger.getLogger( MqQueueSenderImpl.class );

	private final String brokerUrl;
	private final QueueFactory.QueueIdentifier queueIdentifier;

	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private ActiveMQQueue queue;
	private MessageProducer producer;


	MqQueueSenderImpl(String brokerUrl, QueueIdentifier queueIdentifier)
	{
		this.brokerUrl = brokerUrl;
		this.queueIdentifier = queueIdentifier;
	}

	void startup()
	{
		try {
			connectionFactory =
				new ActiveMQConnectionFactory(brokerUrl);
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			queue = new ActiveMQQueue(queueIdentifier.getQueueName());
			producer = session.createProducer(queue);
			log.debug("Queue started:" + queueIdentifier);
		} catch (JMSException jmse) {
			throw new QueueRuntimeException(jmse);
		}
	}

	void shutdown()
	{
		try {
			session.close();
			connection.stop();
			log.debug("Queue stopped:" + queueIdentifier);
		} catch (JMSException jmse) {
			throw new QueueRuntimeException(jmse);
		}

	}

	@Override
	public void send(Event e)
	{
		try {
			final javax.jms.Message msg = session.createObjectMessage(e);
			producer.send(msg);
			log.info("Message sent:" + queueIdentifier);
		} catch (JMSException jmse) {
			throw new QueueRuntimeException(jmse);
		}
	}

}

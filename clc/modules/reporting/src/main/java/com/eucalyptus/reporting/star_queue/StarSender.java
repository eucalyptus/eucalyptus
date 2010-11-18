package com.eucalyptus.reporting.star_queue;

import javax.jms.*;

import org.apache.activemq.*;
import org.apache.activemq.command.*;
import org.apache.log4j.*;

import com.eucalyptus.reporting.event.*;

/**
 * <p>StarSender is a sender for a <i>star queue</i>, where a star queue is
 * a reliable asynchronous message queue arranged in a star topology, with many
 * senders and one receiver; see the package documentation (package.html)
 * for further explanation of a star queue.
 *
 * <p>Each machine with a component (S3, EBS, CLC) has its own StarSender instance,
 * and all StarSenders send their messages to the single StarReceiver within the
 * reporting mechanism.
 *
 * <p>StarSender implements EventListener, so it can receive events from components
 * and communicate those events to the reporting mechanism for storage and
 * generating reports.
 *
 * <p>StarSender is the <i>only</i> class in the queue package which any component
 * other than the reporting mechanism should interact with.
 *
 * <p>Usage: yourComponent.addEventListener(StarSender.getInstance());
 *
 * <p>After the sender is added as an event listener, all events fired from your
 * component will be sent over the network, stored in the reporting database,
 * summarized, and used to generate reports.
 */
public final class StarSender
	implements EventListener, Startable
{
	private static Logger log = Logger.getLogger( StarSender.class );

	private static StarSender instance = null;

	//Must be configurable and set by configuration
	static QueueIdentifier queueIdentifier = null;

	public static synchronized StarSender getInstance()
	{
		if (instance == null) {
			instance = new StarSender(queueIdentifier);
		}
		return instance;
	}

	private QueueIdentifier queueId;
	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private ActiveMQQueue queue;
	private MessageProducer producer;
	private boolean started = false;

	private StarSender(QueueIdentifier queueId)
	{
		this.queueId = queueId;
	}

	public void startup()
		throws StartableException
	{
		if (!started) {
			try {
				StarBroker broker = StarBroker.getInstance();
				connectionFactory = new ActiveMQConnectionFactory(broker.getBrokerUrl());
				connection = connectionFactory.createConnection();
				connection.start();
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				queue = new ActiveMQQueue(queueId.getQueueName());
				producer = session.createProducer(queue);
				started=true;
			} catch (JMSException jmse) {
				throw new StartableException(jmse);
			}
			log.info("StarSender started");
	 	} else {
			log.warn("StarSender started redundantly");
		}
	}

	public void shutdown()
		throws StartableException
	{
		if (started) {
			try {
				session.close();
				connection.stop();
				started=false;
			} catch (JMSException jmse) {
				throw new StartableException(jmse);
			}
			log.info("StarSender stopped");
		} else {
			log.warn("StarSender stopped redundantly");
		}
	}

	public void send(Event e)
	{
		if (started) {
			try {
				final javax.jms.Message msg = session.createObjectMessage(e);
				producer.send(msg);
				log.debug("Message sent:" + queueId);
			} catch (JMSException jmse) {
				throw new QueueRuntimeException(jmse);
			}
		} else {
			throw new java.lang.IllegalStateException("StarSender not started");
		}
	}


	/**
	 * Implements EventListener.receiveEvent(Event).
	 */
	public void receiveEvent(Event e)
	{
		send(e);
	}


}


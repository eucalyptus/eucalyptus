package com.eucalyptus.reporting.star_queue;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.log4j.*;

import com.eucalyptus.reporting.event.*;

/**
 * <p>StarReceiver is the single endpoint for multiple StarSenders in a star
 * queue; see the package documentation (package.html) for further details.
 * StarReceiver is used only be the reporting mechanism.
 */
public final class StarReceiver
	implements Startable, MessageListener
{
	private static Logger log = Logger.getLogger( StarReceiver.class );

	private static StarReceiver instance = null;

	public static synchronized StarReceiver getInstance()
	{
		if (instance == null) {
			instance = new StarReceiver();
		}
		return instance;
	}

	private ActiveMQConnectionFactory connectionFactory;
	private Connection connection;
	private Session session;
	private List<MessageConsumer> consumers;
	private List<EventListener> listeners;
	private boolean started = false;


	private StarReceiver()
	{
		this.listeners = new ArrayList<EventListener>();
		this.consumers = new ArrayList<MessageConsumer>();
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
				for (QueueIdentifier queueId: QueueIdentifier.values()) {
					ActiveMQQueue queue = new ActiveMQQueue(queueId.getQueueName());
					MessageConsumer consumer = session.createConsumer(queue);
					consumers.add(consumer);
					consumer.setMessageListener(this);
				}
				started = true;
			} catch (JMSException jmse) {
				throw new StartableException(jmse);
			}
			log.info("StarReceiver started");
	 	} else {
			log.warn("StarReceiver started redundantly");
		}
	}

	public void shutdown()
		throws StartableException
	{
		if (started) {
			try {
				session.close();
				connection.stop();
			} catch (JMSException jmse) {
				throw new StartableException(jmse);
			}
			started = false;
			log.info("StarReceiver stopped");
		} else {
			log.warn("StarReceiver stopped redundantly");
		}
	}

	public void addEventListener(EventListener el)
	{
		this.listeners.add(el);
	}

	public void removeEventListener(EventListener el)
	{
		this.listeners.remove(el);
	}

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
		for (EventListener el: listeners) {
			el.receiveEvent(event);
		}
	}

}


package com.eucalyptus.reporting.queue;

import java.util.*;

import org.apache.log4j.Logger;

public class QueueFactory
{
	private static Logger log = Logger.getLogger( QueueFactory.class );

	private static QueueFactory queueFactory = null;

	/* These may be configurable properties in the future */
	private static final String BROKER_URL  = "tcp://localhost:61616";
	private static final String BROKER_NAME = "reportingBroker";
	private static final String BROKER_DIR  = "/tmp";

	public static QueueFactory getInstance()
	{
		if (queueFactory == null) {
			queueFactory = new QueueFactory();
		}
		return queueFactory;
	}

	private Map<QueueIdentifier,QueueSenderImpl>   senders;
	private Map<QueueIdentifier,QueueReceiverImpl> receivers;
	private QueueBroker broker;
	private boolean started = false;
	
	private QueueFactory()
	{
		this.senders   = new HashMap<QueueIdentifier,QueueSenderImpl>();
		this.receivers = new HashMap<QueueIdentifier,QueueReceiverImpl>();
		this.broker    = new QueueBroker(BROKER_NAME, BROKER_URL, BROKER_DIR);
	}

	public enum QueueIdentifier
	{
		INSTANCE("InstanceQueue"),
		STORAGE("StorageQueue");
		
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
		if (!started) {
			this.broker.startup();
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
			this.broker.shutdown();
			log.info("QueueFactory stopped");
		} else {
			log.warn("QueueFactory.shutdown called when not started");
		}
	}
	
	public QueueSender getSender(QueueIdentifier identifier)
	{
		if (started) {
			if (senders.containsKey(identifier)) {
				return senders.get(identifier);
			} else {
				QueueSenderImpl sender = new QueueSenderImpl(broker, identifier);
				sender.startup();
				senders.put(identifier, sender);
				log.info("Sender " + identifier + " started");
				return sender;
			}
		} else {
			throw new QueueRuntimeException("QueueFactory not started");
		}
	}

	public QueueReceiver getReceiver(QueueIdentifier identifier)
	{
		if (started) {
			if (receivers.containsKey(identifier)) {
				return receivers.get(identifier);
			} else {
				QueueReceiverImpl receiver = new QueueReceiverImpl(broker,
						identifier);
				receiver.startup();
				receivers.put(identifier, receiver);
				log.info("Receiver " + identifier + " started");
				return receiver;
			}
		} else {
			throw new QueueRuntimeException("QueueFactory not started");			
		}
		
	}

}

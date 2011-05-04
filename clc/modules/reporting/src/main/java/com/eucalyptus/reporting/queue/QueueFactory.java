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

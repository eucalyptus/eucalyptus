package com.eucalyptus.reporting.queue;

import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;

public interface InternalQueueFactory
{
	public void startup();
	public void shutdown();
	public QueueReceiver getReceiver(QueueIdentifier identifier);
	public QueueSender getSender(QueueIdentifier idenfitifer);
}

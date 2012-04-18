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

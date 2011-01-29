package com.eucalyptus.reporting.queue;

import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.event.EventListener;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;

public class QueueTest
{
	public static void runListenerTest()
	{
		QueueFactory queueFactory = QueueFactory.getInstance();
		queueFactory.startup();
		QueueSender sender = queueFactory.getSender(QueueFactory.QueueIdentifier.INSTANCE);
		for (int i=0; i < 100; i++) {
			sender.send(new TestEvent(i));
			System.out.println("Sent event:" + i);
		}
		try {
			System.out.println("Waiting 20 secs");
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		QueueReceiver receiver = queueFactory.getReceiver(QueueFactory.QueueIdentifier.INSTANCE);
		TestEventListener listener = new TestEventListener();
		receiver.addEventListener(listener);
		System.out.println("Total:" + listener.getTotalNum());
	}
	
	public static void runReceiveNoWaitTest()
	{
		int totalNum = 0;
		
		QueueFactory queueFactory = QueueFactory.getInstance();
		queueFactory.startup();
		QueueSender sender = queueFactory.getSender(QueueFactory.QueueIdentifier.INSTANCE);
		for (int i=0; i < 100; i++) {
			sender.send(new TestEvent(i));
			System.out.println("Sent event:" + i);
		}
		
		try {
			System.out.println("Waiting 20 secs...");
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		QueueReceiver receiver = queueFactory.getReceiver(QueueFactory.QueueIdentifier.INSTANCE);
		for (Event event = receiver.receiveEventNoWait();
			event != null;
			event = receiver.receiveEventNoWait())
		{
			TestEvent testEvent = (TestEvent) event;
			System.out.println("Received Event:" + testEvent.getNum());
			totalNum += testEvent.getNum();	
		}
		
		System.out.println("Total:" + totalNum);
	}
	
	public static void runBroker()
	{
		QueueFactory queueFactory = QueueFactory.getInstance();
		queueFactory.startup();
	}
	
	public static void runSender()
	{
		QueueSenderImpl sender = new QueueSenderImpl(QueueBroker.DEFAULT_URL, QueueIdentifier.INSTANCE);
		sender.startup();
		for (int i=0; i < 100; i++) {
			sender.send(new TestEvent(i));
			System.out.println("Sent event:" + i);
		}		
	}
	
	public static void runReceiver()
	{
		QueueReceiverImpl receiver = new QueueReceiverImpl(QueueBroker.DEFAULT_URL, QueueIdentifier.INSTANCE);
		receiver.startup();
		TestEventListener listener = new TestEventListener();
		receiver.addEventListener(listener);
		
	}

	public static void main(String[] args)
		throws Exception
	{
		if (args[0].equals("broker")) {
			runBroker();
		} else if (args[0].equals("sender")) {
			runSender();
		} else if (args[0].equals("receiver")) {
			runReceiver();
		} else {
			System.err.println("Unrecognized command:" + args[0]);
		}
	}
	
	private static class TestEventListener
		implements EventListener
	{
		private int totalNum;
		
		TestEventListener()
		{
			
		}
		
		int getTotalNum()
		{
			return this.totalNum;
		}
		
		@Override
		public void fireEvent(Event e)
		{
			TestEvent testEvent = (TestEvent) e;
			System.out.println("Received Event:" + testEvent.getNum());
			this.totalNum += testEvent.getNum();
		}
	}
	
	@SuppressWarnings("serial")
	private static class TestEvent
		implements Event
	{
		private final int num;
		
		TestEvent(int num) {
			this.num = num;
		}
		
		int getNum()
		{
			return this.num;
		}
		
		@Override
		public boolean requiresReliableTransmission()
		{
			return false;
		}
		
	}
}

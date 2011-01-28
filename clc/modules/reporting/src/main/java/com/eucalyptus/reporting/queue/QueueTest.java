package com.eucalyptus.reporting.queue;

import com.eucalyptus.reporting.event.Event;
import com.eucalyptus.reporting.event.EventListener;

public class QueueTest
{
	public static void runListenerTest()
	{
		QueueFactory queueFactory = QueueFactory.getInstance();
		queueFactory.startup();
		QueueReceiver receiver = queueFactory.getReceiver(QueueFactory.QueueIdentifier.INSTANCE);
		TestEventListener listener = new TestEventListener();
		receiver.addEventListener(listener);
		QueueSender sender = queueFactory.getSender(QueueFactory.QueueIdentifier.INSTANCE);
		for (int i=0; i < 100; i++) {
			sender.send(new TestEvent(i));
			System.out.println("Sent event:" + i);
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Total:" + listener.getTotalNum());
	}
	
	public static void runReceiveNoWaitTest()
	{
		int totalNum = 0;
		
		QueueFactory queueFactory = QueueFactory.getInstance();
		queueFactory.startup();
		QueueReceiver receiver = queueFactory.getReceiver(QueueFactory.QueueIdentifier.INSTANCE);
		QueueSender sender = queueFactory.getSender(QueueFactory.QueueIdentifier.INSTANCE);
		for (int i=0; i < 100; i++) {
			sender.send(new TestEvent(i));
			System.out.println("Sent event:" + i);
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
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
		public void receiveEvent(Event e)
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

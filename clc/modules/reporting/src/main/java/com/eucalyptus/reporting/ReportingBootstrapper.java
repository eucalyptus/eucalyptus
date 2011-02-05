package com.eucalyptus.reporting;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.*;
import com.eucalyptus.reporting.instance.InstanceEventListener;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.reporting.storage.StorageEventPoller;

@Provides(Component.reporting)
@RunDuring(Bootstrap.Stage.RemoteServicesInit)
public class ReportingBootstrapper
	extends Bootstrapper
{
	private static Logger log = Logger.getLogger( ReportingBootstrapper.class );

	/**
	 * Sets up listeners etc in test mode whereby they generate fake times for
	 * incoming data.
	 */
	private static final boolean TEST = true;  //TODO: CHANGE!!!
	
	private static long POLLER_DELAY_MS = 10000l;
	
	private StorageEventPoller storagePoller;
	private InstanceEventListener instanceListener;
	private QueueFactory queueFactory;
	private Timer timer;
	

	public ReportingBootstrapper()
	{
	}

	@Override
	public boolean check()
	{
		return true;
	}

	@Override
	public void destroy()
	{
		return;
	}

	@Override
	public boolean enable()
	{
		return true;
	}

	@Override
	public boolean disable()
	{
		return true;
	}

	@Override
	public boolean load()
	{
		try {
			return true;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to load", ex);
			return false;
		}
	}

	
	
	@Override
	public boolean start()
	{
		try {
			//TODO: brokers must FIND EACH OTHER here...

			/* Start queue broker
			 */
			queueFactory = QueueFactory.getInstance();
			//QueueFactory has been started in SystemBootstrapper.init()
			
			/* Start storage receiver and storage queue poller thread
			 */
			QueueReceiver storageReceiver = queueFactory.getReceiver(QueueIdentifier.STORAGE);
			final StorageEventPoller poller;
			//Start fake pollers and listeners if this is a test
			if (TEST) {
				poller = new TestStorageEventPoller(storageReceiver);
				instanceListener = new TestInstanceEventListener();
			} else {
				poller = new StorageEventPoller(storageReceiver);
				instanceListener = new InstanceEventListener();
			}
			timer = new Timer(true);
			timer.schedule(new TimerTask() {
				@Override
				public void run()
				{
					poller.writeEvents();
				}
			}, 0, POLLER_DELAY_MS);
			this.storagePoller = poller;
			log.info("Storage queue poller started");
			
			/* Start instance receiver and instance listener
			 */
			QueueReceiver instanceReceiver =
				queueFactory.getReceiver(QueueIdentifier.INSTANCE);
			instanceReceiver.addEventListener(instanceListener);
			
			log.info("ReportingBootstrapper started");
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("ReportingBootstrapper failed to start", ex);
			return false;
		}
	}

	public static void startTest()
	{
		ReportingBootstrapper bootstrapper = new ReportingBootstrapper();
		bootstrapper.load();
		bootstrapper.start();
		try {
			System.out.println("Sleeping for 60 secs");
			Thread.sleep(60000);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public boolean stop()
	{
		try {
			log.info("ReportingBootstrapper stopped");
			timer.cancel();
			instanceListener.flush();
			storagePoller.writeEvents();
			queueFactory.shutdown();
			return true;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to stop", ex);
			return false;
		}
	}
	
	private static final long START_TIME = 1104566400000l; //Jan 1, 2005 12:00AM
	private static final int TIME_USAGE_APART = 100000; //ms

	private class TestInstanceEventListener
		extends InstanceEventListener
	{
		private long timeMs = START_TIME;
		
		@Override
		protected long getCurrentTimeMillis()
		{
			long oldTimeMs = timeMs;
			timeMs += TIME_USAGE_APART;
			return oldTimeMs;
		}
	}
	
	private class TestStorageEventPoller
		extends StorageEventPoller
	{
		private long timeMs = START_TIME;
		
		public TestStorageEventPoller(QueueReceiver receiver)
		{
			super(receiver);
		}

		@Override
		protected long getTimestampMs()
		{
			long oldTimeMs = timeMs;
			timeMs += TIME_USAGE_APART;
			return oldTimeMs;
		}		
	}
	
}

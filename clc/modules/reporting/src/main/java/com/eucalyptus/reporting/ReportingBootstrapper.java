package com.eucalyptus.reporting;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.*;

import com.eucalyptus.bootstrap.*;
import com.eucalyptus.reporting.instance.*;
import com.eucalyptus.reporting.storage.*;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;
import com.eucalyptus.system.Threads;

@Provides(Component.reporting)
@RunDuring(Bootstrap.Stage.RemoteServicesInit)
public class ReportingBootstrapper
	extends Bootstrapper
{
	private static Logger log = Logger.getLogger( ReportingBootstrapper.class );

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
			final StorageEventPoller poller = new StorageEventPoller(storageReceiver);
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
			instanceListener = new InstanceEventListener();
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
			instanceListener.flush();
			timer.cancel();
			storagePoller.writeEvents();
			queueFactory.shutdown();
			return true;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to stop", ex);
			return false;
		}
	}

}

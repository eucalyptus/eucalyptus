package com.eucalyptus.reporting;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.*;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.reporting.instance.*;
import com.eucalyptus.reporting.storage.*;
import com.eucalyptus.reporting.queue.*;
import com.eucalyptus.reporting.queue.QueueFactory.QueueIdentifier;

@Provides(Reporting.class)
@RunDuring(Bootstrap.Stage.RemoteServicesInit)
public class ReportingBootstrapper
	extends Bootstrapper
{
	private static Logger log = Logger.getLogger( ReportingBootstrapper.class );

	private static long POLLER_DELAY_MS = 1000l;
	
	private StorageEventPoller storagePoller;
	private InstanceEventListener instanceListener;
	private QueueBroker broker;
	private Timer timer;
	

	public ReportingBootstrapper()
	{
	}

	public boolean check()
	{
		return true;
	}

	public void destroy()
	{
		return;
	}

	public boolean enable()
	{
		return true;
	}

	public boolean disable()
	{
		return true;
	}

	public boolean load()
	{
		try {
			broker = QueueBroker.getInstance();
			timer = new Timer(true);
			return true;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to load", ex);
			return false;
		}
	}

	public boolean start()
	{
		try {
			//TODO: brokers must FIND EACH OTHER here...
			broker.startup();
			log.info("Queue broker started");
			QueueFactory queueFactory = QueueFactory.getInstance();
			final StorageEventPoller poller = new StorageEventPoller(queueFactory.getReceiver(QueueIdentifier.STORAGE));
			timer.schedule(new TimerTask() {
				@Override
				public void run()
				{
					poller.writeEvents();
				}
			}, POLLER_DELAY_MS);
			this.storagePoller = poller;
			log.info("Storage queue poller started");
			this.instanceListener = new InstanceEventListener();
			log.info("ReportingBootstrapper started");
			return true;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to start", ex);
			return false;
		}
	}

	public boolean stop()
	{
		try {
			log.info("ReportingBootstrapper stopped");
			instanceListener.flush();
			timer.cancel();
			storagePoller.writeEvents();
			broker.shutdown();
			return true;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to stop", ex);
			return false;
		}
	}

}

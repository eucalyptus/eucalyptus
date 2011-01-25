package com.eucalyptus.reporting;

import org.apache.log4j.*;

import com.eucalyptus.reporting.instance.*;
import com.eucalyptus.reporting.storage.*;

public class ReportingBootstrapper
	//extends Bootstrapper
{
	private static Logger log = Logger.getLogger( ReportingBootstrapper.class );

//	private final StorageEventPoller storageListener;
//	private final InstanceEventListener instanceListener;
//	private StarReceiver starReceiver;
	//private final QueueBroker starBroker;

	public ReportingBootstrapper()
	{
//		this.storageListener = new StorageEventPoller();
//		this.instanceListener = new InstanceEventListener();
//		this.starBroker = QueueBroker.getInstance();
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
		return true;
	}

	public boolean start()
	{
		try {
//			starBroker.startup();
//			this.starReceiver = StarReceiver.getInstance();
//			this.starReceiver.addEventListener(instanceListener);
//			this.starReceiver.addEventListener(storageListener);
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
//			starBroker.startup();
//			this.starReceiver = StarReceiver.getInstance();
//			this.starReceiver.removeEventListener(instanceListener);
//			this.starReceiver.removeEventListener(storageListener);
			log.info("ReportingBootstrapper stopped");
			return false;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to stop", ex);
			return false;
		}
	}

}

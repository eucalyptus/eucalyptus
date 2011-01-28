package com.eucalyptus.reporting;

import org.apache.log4j.*;

import com.eucalyptus.reporting.star_queue.*;
import com.eucalyptus.reporting.instance.*;
import com.eucalyptus.reporting.storage.*;

public class ReportingBootstrapper
	//extends Bootstrapper
{
	private static Logger log = Logger.getLogger( ReportingBootstrapper.class );

	private final StorageEventListener storageListener;
	private final InstanceEventListener instanceListener;
	private StarReceiver starReceiver;
	private final StarBroker starBroker;

	public ReportingBootstrapper()
	{
		this.storageListener = new StorageEventListener();
		this.instanceListener = new InstanceEventListener();
		this.starBroker = StarBroker.getInstance();
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
			starBroker.startup();
			this.starReceiver = StarReceiver.getInstance();
			this.starReceiver.addEventListener(instanceListener);
			this.starReceiver.addEventListener(storageListener);
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
			starBroker.startup();
			this.starReceiver = StarReceiver.getInstance();
			this.starReceiver.removeEventListener(instanceListener);
			this.starReceiver.removeEventListener(storageListener);
			log.info("ReportingBootstrapper stopped");
			return false;
		} catch (Exception ex) {
			log.error("ReportingBootstrapper failed to stop", ex);
			return false;
		}
	}

}

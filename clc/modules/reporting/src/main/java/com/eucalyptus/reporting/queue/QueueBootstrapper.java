package com.eucalyptus.reporting.queue;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.*;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.reporting.ReportingBootstrapper;

@Provides(Reporting.class)
@RunDuring(Bootstrap.Stage.PrivilegedConfiguration)
public class QueueBootstrapper extends Bootstrapper
{
	private static Logger log = Logger.getLogger( QueueBootstrapper.class );

	@Override
	public boolean load() throws Exception
	{
		/* NOTE: a workaround was moved to SystemBootstrapper.init() because
		 * log4j brain damage prevents ActiveMQ broker from starting.
		 */
//		try {
//	        QueueFactory.getInstance().startup();
//	        log.info("broker started");
//			return true;			
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			return false;
//		}
		return true;
	}

	@Override
	public boolean start() throws Exception
	{
		return true;
	}

	@Override
	public boolean enable() throws Exception
	{
		return true;
	}

	@Override
	public boolean stop() throws Exception
	{
		return true;
	}

	@Override
	public void destroy() throws Exception
	{
	}

	@Override
	public boolean disable() throws Exception
	{
		return true;
	}

	@Override
	public boolean check() throws Exception
	{
		return true;
	}

}

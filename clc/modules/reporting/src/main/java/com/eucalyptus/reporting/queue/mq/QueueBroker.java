package com.eucalyptus.reporting.queue.mq;

import java.util.*;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.log4j.*;

import com.eucalyptus.component.*;
import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.reporting.queue.QueueRuntimeException;
import com.eucalyptus.system.SubDirectory;

public class QueueBroker
{
	private static Logger log = Logger.getLogger( QueueBroker.class );

	public static final String DEFAULT_NAME = "reportingBroker";
	public static final String DEFAULT_DIR  = SubDirectory.QUEUE.toString();
	public static final int    DEFAULT_PORT = 63636;
	public static final String DEFAULT_URL  = "tcp://localhost:" + DEFAULT_PORT;
	public static final String DEFAULT_REMOTE_URL_FORMAT = "static:(tcp://%s:%d)";
		
	private boolean started = false;

	private String brokerName;
	private String brokerDataDir;
	private String brokerUrl;
	
	private List<ActiveMQDestination> destinations;

	private BrokerService brokerService;
	private JmsBrokerThread brokerThread;

	private QueueBroker(String brokerName, String brokerUrl, String brokerDataDir)
	{
		this.brokerName = brokerName;
		this.brokerUrl = brokerUrl;
		this.brokerDataDir = brokerDataDir;
		this.destinations = new ArrayList<ActiveMQDestination>();
	}
	
	private static QueueBroker instance;
	
	public static QueueBroker getInstance()
	{
		if (instance == null) {
			return instance = new QueueBroker(DEFAULT_NAME, DEFAULT_URL,
					DEFAULT_DIR);
		}
		return instance;
	}

	public void addDestination(String destName)
	{
		ActiveMQDestination dest =
			ActiveMQDestination.createDestination(destName, ActiveMQDestination.QUEUE_TYPE);
		this.destinations.add(dest);
	}
	
	public void startup()
	{
		/* Find remote broker if we're not running on the reporting machine
		 */
		String remoteBrokerUrl = null;
		Component reportingComponent = Components.lookup(Reporting.class);
		if (null!=reportingComponent && !reportingComponent.isEnabledLocally()) {
			log.info("Searching for remote reporting broker");
			//TODO: merge in
			//NavigableSet<Service> services = reportingComponent.;
//			for (Service service: services) {
//				remoteBrokerUrl = String.format(DEFAULT_REMOTE_URL_FORMAT,
//						service.getServiceConfiguration().getHostName(), DEFAULT_PORT);
//			}
			if (remoteBrokerUrl==null) {
				throw new QueueRuntimeException("Unable to locate reporting broker over network");
			}
		} else {
			log.info("Reporting broker will run locally");
			remoteBrokerUrl = null;
		}
		
		/* Startup BrokerService in separate thread and provide url and remoteUrl
		 */
		try {
			brokerService = new BrokerService();
			brokerService.setBrokerName(brokerName);
			brokerService.setDataDirectory(brokerDataDir);
			brokerService.addConnector(brokerUrl);
			if (remoteBrokerUrl != null) {
				brokerService.addNetworkConnector(remoteBrokerUrl);
			}
			brokerService.setUseJmx(false);
			brokerService.start();
//			brokerThread = new JmsBrokerThread(brokerService);
//			brokerThread.start();
//			Thread.sleep(1000); // give the broker a moment to startup; TODO:
//								// fix this
//			if (brokerThread.getStartException() != null) {
//				throw brokerThread.getStartException();
//			}
		} catch (Exception ex) {
			throw new QueueRuntimeException(ex);
		}
		log.info("Broker started");
	}

	public void shutdown()
	{
		try {
			brokerService.stop();
		} catch (Exception ex) {
			throw new QueueRuntimeException(ex);
		}
		log.info("Broker stopped");
	}

	public String getBrokerName()
	{
		return this.brokerName;
	}

	public String getBrokerUrl()
	{
		return this.brokerUrl;
	}

	public static void main(String[] args)
		throws Exception
	{
		String remoteBrokerUrl = null;
		if (args.length > 0) {
			remoteBrokerUrl = args[0];
		}
		QueueBroker broker = new QueueBroker(DEFAULT_NAME, DEFAULT_URL,
				DEFAULT_DIR);
		broker.startup();
	}


	/**
	 * The JMS broker must run in a separate thread if it's embedded because
	 * brokerService.start() never returns.
	 */
	private static class JmsBrokerThread
		extends Thread
	{
		private final BrokerService brokerService;
		private Exception exception = null;

		JmsBrokerThread(final BrokerService brokerService)
		{
			this.brokerService = brokerService;
		}

		public void run()
		{
			try {
				brokerService.start();
			} catch (Exception ex) {
				log.error(ex);
				exception = ex;
			}
		}

		public Exception getStartException()
		{
			return exception;
		}

	}

}

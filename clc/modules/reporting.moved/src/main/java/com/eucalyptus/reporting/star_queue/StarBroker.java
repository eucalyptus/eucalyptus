package com.eucalyptus.reporting.star_queue;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.log4j.*;

/**
 * StarBroker manages communication between multiple star senders and the
 * single star receiver. StarBroker is started automatically and is only
 * accessed by the StarSender or StarReceiver. 
 */
public class StarBroker
	implements Startable
{
	private static Logger log = Logger.getLogger( StarBroker.class );

	private static StarBroker instance = null;

	private boolean started = false;

	private String brokerName;
	private String brokerDataDir;
	private String brokerUrl;

	private BrokerService brokerService;
	private JmsBrokerThread brokerThread;

	/* These may be a configurable properties in the future */
	private static final String BROKER_URL  = "tcp://localhost:61616";
	private static final String BROKER_NAME = "reportingBroker";

	private StarBroker(String brokerName, String brokerDataDir, String brokerUrl)
	{
		this.brokerName = brokerName;
		this.brokerDataDir = brokerDataDir;
		this.brokerUrl = brokerUrl;
	}

	public static synchronized StarBroker getInstance()
	{
		if (instance == null) {
			instance = new StarBroker(BROKER_NAME, "/tmp", BROKER_URL); //TODO: fix location with configuration
		}
		return instance;
	}

	public void startup()
		throws StartableException
	{
		if (!started) {
			try {
				brokerService = new BrokerService();
				brokerService.setBrokerName(brokerName);
				brokerService.setDataDirectory(brokerDataDir);
				brokerService.addConnector(brokerUrl);
				brokerThread = new JmsBrokerThread(brokerService);
				brokerThread.start();
				Thread.sleep(1000); //give the broker a moment to startup; TODO: fix this
				if (brokerThread.getStartException() != null) {
					throw brokerThread.getStartException();
				}
			} catch (Exception ex) {
				throw new StartableException(ex);
			}
			started = true;
			log.info("Broker started");
		} else {
			log.warn("Broker started redundantly");
		}
	}

	public void shutdown()
		throws StartableException
	{
		if (started) {
			try {
				brokerService.stop();
			} catch (Exception ex) {
				throw new StartableException(ex);
			}
			started = false;
			log.info("Broker stopped");
		} else {
			log.warn("Broker stopped redundantly");
		}
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
		StarBroker broker = StarBroker.getInstance();
		broker.startup();
	}


	/**
	 * The JMS broker must run in a separate thread if it's embedded because
	 * brokerService.start() never returns.
	 */
	private static class JmsBrokerThread
		extends Thread
	{
		private static Logger log = Logger.getLogger( JmsBrokerThread.class );

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


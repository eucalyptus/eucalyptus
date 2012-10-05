package com.eucalyptus.bootstrap.listeners;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.troubleshooting.LoggingResetter;


public class LogLevelListener implements PropertyChangeListener {
	private static final Logger LOG = Logger.getLogger(LogLevelListener.class);
	private final String[] logLevels = new String[] { "ALL", "DEBUG",
			"INFO", "WARN", "ERROR", "FATAL", "TRACE", "OFF", "EXTREME" };

	/**
	 * @see com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty,
	 *      java.lang.Object)
	 */
	@Override
	public void fireChange(ConfigurableProperty t, Object newValue)
			throws ConfigurablePropertyException {
		if (newValue == null) {
			newValue = "";
		}
		String newLogLevel = (String) newValue;
		newLogLevel = newLogLevel.trim().toUpperCase();
		if (!(newLogLevel.isEmpty() || Arrays.asList(logLevels).contains(
				newLogLevel))) {
			throw new ConfigurablePropertyException("Invalid log level "
					+ newLogLevel);
		}
		// TODO: add error checking for property and configuration
		LOG.warn("Change occurred to property " + t.getQualifiedName()
				+ " with new value " + newValue + ".");
		try {
			t.getField().set(null, t.getTypeParser().apply(newValue));
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
			throw new ConfigurablePropertyException(e1);
		} catch (IllegalAccessException e1) {
			e1.printStackTrace();
			throw new ConfigurablePropertyException(e1);
		}
		if (!newLogLevel.isEmpty()) {
			System.setProperty("euca.log.level", newLogLevel.toUpperCase());
			LoggingResetter.resetLoggingWithXML();
			//triggerDBFault();
		}
		LOG.fatal("test level FATAL");
		LOG.error("test level ERROR");
		LOG.warn("test level WARN");
		LOG.info("test level INFO");
		LOG.debug("test level DEBUG");
		LOG.trace("test level TRACE");
	}
	private static void triggerDBFault() {
		try {
			for (int j = 0; j < 5; j++) {
				Class.forName("org.logicalcobwebs.proxool.ProxoolDriver");
				Connection[] cons = new Connection[505];
				for (int i = 0; i < 505; i++) {
					cons[i] = DriverManager
							.getConnection("proxool.eucalyptus_walrus:net.sf.hajdbc.sql.Driver:jdbc:ha-jdbc:eucalyptus_walrus");
				}
				Thread.sleep(30000L);
				for (int i = 0; i < 505; i++) {
					cons[i].close();
				}
				Thread.sleep(30000L);
			}
		} catch (Exception ex) {
			LOG.error(ex, ex);
		}
	}
}

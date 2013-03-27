/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.troubleshooting.changelisteners;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.system.log.LoggingResetter;


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

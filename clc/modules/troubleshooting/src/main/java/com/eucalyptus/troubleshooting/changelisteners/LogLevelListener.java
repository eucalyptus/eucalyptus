/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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


public class LogLevelListener implements PropertyChangeListener<Object> {
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
			t.getField().set(null, t.getTypeParser().apply(newValue.toString()));
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

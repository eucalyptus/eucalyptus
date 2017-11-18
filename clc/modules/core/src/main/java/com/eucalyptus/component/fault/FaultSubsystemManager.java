/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.component.fault;

import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.SubDirectory;

public class FaultSubsystemManager {
	private static final Logger LOG = Logger.getLogger(FaultSubsystemManager.class);
	private Map<String, FaultLogger> loggerMap = new ConcurrentHashMap<String, FaultLogger>();
	private FaultRegistry faultRegistry = null;
	private static final String DEFAULT_LOCALE = "en_US";

	public FaultSubsystemManager() {
		faultRegistry = new FaultRegistry();
		String locale = System.getenv("LOCALE");
		
		// Start with the system default locale fault dir
		faultRegistry.crawlDirectory(SubDirectory.SYSFAULTS.getChildFile(DEFAULT_LOCALE));

		// Add the system locale-specific fault dir
		if (locale != null && !locale.equals(DEFAULT_LOCALE)) {
			faultRegistry.crawlDirectory(SubDirectory.SYSFAULTS.getChildFile(locale));
		}
		// next check the custom default locale fault dir

		faultRegistry.crawlDirectory(SubDirectory.CUSTOMFAULTS.getChildFile(DEFAULT_LOCALE));

		// Add the system locale-specific fault dir
		if (locale != null && !locale.equals(DEFAULT_LOCALE)) {
			faultRegistry.crawlDirectory(SubDirectory.CUSTOMFAULTS.getChildFile(locale));
		}
	}
	
	/*
	The convention here is that for a given component, we will have an appender called
	<component>-fault-log that will log to a file called ${component}-fault.log in
	the ${euca.log.dir} directory.  The logger that will be used to attach these
	appenders to will be called com.eucalyptus.troubleshooting.fault.${component}.log
	
	These are the steps to construct the logger/appender relationship.
	1) First, check all existing logs for the log named above.  If it is not there,
	   it was not created in the initial log4j.xml, so it will be created with defaults.
	2) Second, see if the named appender described above has been instantiated.  Log4j has
	   no global getAppender() method, so every logger will be traversed.  If it can not be
	   found, the appender will be created based on defaults. 
	3) The named appender will be added to the logger, and the logger will be used in the 
	   component.
	   
	The case where there is a defined appender in the log4j.xml file but no corresponding
	logger could occur if we wanted to lazily load the loggers.  
	
	Defaults are:
	
	<appender name="${component}-fault-log" class="org.apache.log4j.RollingFileAppender">
	  <param name="File" value="${euca.log.dir}/nc-fault.log" />
	  <param name="MaxFileSize" value="10MB" />
	  <param name="MaxBackupIndex" value="10" />
	  <param name="Threshold" value="FATAL" />
	  <layout class="org.apache.log4j.PatternLayout">
	    <param name="ConversionPattern" value="%m%n" />
	  </layout>
	</appender>
	  
	<category name="com.eucalyptus.troubleshooting.fault.${component}.log" additivity="false">
	  <priority value="FATAL" />
	  <appender-ref ref="${component}-fault-log" />
	</category>
 */
	

	public synchronized FaultLogger getFaultLogger(Class <? extends ComponentId> componentIdClass) {
		if (componentIdClass == null) throw new IllegalArgumentException("componentIdClass is null");
		ComponentId componentId = ComponentIds.lookup(componentIdClass);
		if (componentId == null) throw new IllegalArgumentException("componentId is null");
		String faultLogPrefix = componentId.getFaultLogPrefix();
		FaultLogger logger = loggerMap.get(faultLogPrefix);
		if (logger == null) {
			logger = initLogger(faultLogPrefix);
			loggerMap.put(faultLogPrefix, logger);
		}
		return logger;
	}

	private FaultLogger initLogger(String faultLogPrefix) {
		final String targetLoggerName = "com.eucalyptus.troubleshooting.fault." + faultLogPrefix + ".log";
		final String targetAppenderName = faultLogPrefix + "-fault-log";
		final String targetLogFileName = faultLogPrefix + "-fault.log";
				
		// first find the actual logger (if it exists). Should be named com.eucalyptus.troubleshooting.fault.${component}.log
		// Scan the loggers to see if it already exists.  Otherwise add it
		LOG.debug("looking for logger " + targetLoggerName);
		Logger logger = null;
		Enumeration logEnum = LogManager.getCurrentLoggers();
		while (logEnum.hasMoreElements()) {
			Logger currentLogger = (Logger) logEnum.nextElement();
			if (logger == null && currentLogger.getName().equals(targetLoggerName)) {
				logger = currentLogger;
				LOG.debug("Found logger " + targetLoggerName);
				break;
			}
		}
		if (logger == null) {
			LOG.debug("Didn't find logger " + targetLoggerName + ", creating it now");
			logger = Logger.getLogger(targetLoggerName);
			logger.setAdditivity(false);
			logger.setLevel(Level.FATAL);
		}
		// Check the log registry for the appender named ${component}-fault-log.  Need to check all loggers (unfortunately)
		LOG.debug("Checking root logger for appender " + targetAppenderName);
		Appender appender = checkAppender(LogManager.getRootLogger(), targetAppenderName);
		if (appender == null) {
			logEnum = LogManager.getCurrentLoggers();
			while (logEnum.hasMoreElements()) {
				if (appender == null) {
					Logger currentLogger = (Logger) logEnum.nextElement();
					LOG.debug("Checking " + currentLogger.getName() + " for appender " + targetAppenderName);
					appender = checkAppender(currentLogger, targetAppenderName);
				} else {
					break; // found it
				}
			}
		}
		if (appender == null) { // still?
			// creating a RollingFileAppender reference because Appender doesn't have most of the setters
			RollingFileAppender rAppender = new RollingFileAppender(); 
			rAppender.setFile(BaseDirectory.LOG.getChildFile(targetLogFileName).toString());
			rAppender.setMaxFileSize("10MB");
			rAppender.setMaxBackupIndex(10);
			rAppender.setLayout(new PatternLayout("%m%n"));
			rAppender.setThreshold(Level.FATAL);
			rAppender.activateOptions();
			rAppender.setName(targetAppenderName);
			appender = rAppender;
		}

		// add the appender to the logger if we need to
		if (checkAppender(logger, targetAppenderName) == null) {
			logger.addAppender(appender);
		}
		FaultLogger faultLogger = new FaultLogger(logger);
		return faultLogger;
	}

	private Appender checkAppender(Logger logger, String targetAppenderName) {
		if (logger == null) return null;
		Enumeration appenderEnum = logger.getAllAppenders();
		while (appenderEnum.hasMoreElements()) {
			Appender currentAppender = (Appender) appenderEnum.nextElement();
			if (currentAppender.getName() != null && currentAppender.getName().equals(targetAppenderName)) {
				return currentAppender;
			}
		}
		return null;
	}

	public FaultRegistry getFaultRegistry() {
		return faultRegistry;
	}
	public void init() {
		// preload as many appenders as we can right now...
		for (ComponentId componentId: ComponentIds.list()) {
			// TODO: don't forget to bridge the components
			getFaultLogger(componentId.getClass());
		}
	}

}

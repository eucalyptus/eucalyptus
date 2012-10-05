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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.troubleshooting;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.xml.Log4jEntityResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.eucalyptus.records.Logs;



public class LoggingResetter {
	private static final Logger LOG = Logger.getLogger(LoggingResetter.class);
	private static final String LOG4J_CONFIGURATION_TAG = "log4j:configuration";
	private static final String LOG4J_OLD_CONFIGURATION_TAG = "configuration";
	private static final String LOG4J_APPENDER_TAG = "appender";
	private static final String LOG4J_PARAM_TAG = "param";
	private static final String LOG4J_CATEGORY_TAG = "category";
	private static final String LOG4J_ROOT_TAG = "root";
	private static final String LOG4J_LOGGER_TAG = "logger";
	private static final String LOG4J_VALUE_ATTRIBUTE = "value";
	private static final String LOG4J_NAME_ATTRIBUTE = "name";
	private static final String LOG4J_LEVEL_TAG = "level";
	private static final String LOG4J_PRIORITY_TAG = "priority";
	private static final String LOG4J_THRESHOLD_ATTRIBUTE_VALUE = "Threshold";
	
	private static class SmallLoggingConfiguration {
		public Map<String, Level> appenderThresholdLevels = new HashMap<String, Level>();
		public Level rootLogLevel = null;
		public Map<String, Level> loggerLoggingLevels = new HashMap<String, Level>();
	}
		
	public static synchronized void resetLoggingWithXML() {
		LOG.info("Resetting log levels to " + System.getProperty("euca.log.level"));
		// To avoid the value EXTREME in the log level, we "reset" it (for now)
		Logs.reInit();
		// This is a little evil.  Due to issues with calling DOMConfigurator.configure()
		// more than once, we re-read the log4j.xml file, query the Threshold level of all
		// of the appenders and loggers and set them without creating additional items
		SmallLoggingConfiguration smallLoggingConfiguration = null;
		InputStream in = null;
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			dBuilder.setEntityResolver(new Log4jEntityResolver());
			URL url = Thread.currentThread().getContextClassLoader().getResource("log4j.xml");
			if (url != null) {
				in = url.openStream();
				InputSource inputSource = new InputSource(in);
				inputSource.setSystemId("dummy://log4j.dtd");
				Document document = dBuilder.parse(inputSource);
				Element documentElement = document.getDocumentElement();
				smallLoggingConfiguration = parse(documentElement);
			}
			resetRootLogLevel(LogManager.getRootLogger(), smallLoggingConfiguration);
			Enumeration e =  LogManager.getCurrentLoggers();
			while (e.hasMoreElements()) {
				resetLogLevel((Logger) e.nextElement(), smallLoggingConfiguration);
			}
			LOG.info("Finished resetting log levels");
		} catch (IOException ex) { // nothing we can really do here...
			LOG.error(ex);
			LOG.warn("Unable to reset log levels");
		} catch (ParserConfigurationException ex) { // nothing we can really do here...
			LOG.error(ex);
			LOG.warn("Unable to reset log levels");
		} catch (SAXException ex) { // nothing we can really do here...
			LOG.error(ex);
			LOG.warn("Unable to reset log levels");
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ex) {
					LOG.debug(ex);
				}
			}
		}
	}

	private static void resetRootLogLevel(Logger rootLogger,
			SmallLoggingConfiguration smallLoggingConfiguration) {
		if (rootLogger == null) {
			throw new IllegalArgumentException("root logger is null");
		}
		Level level = smallLoggingConfiguration.rootLogLevel; 
		if (level != null) {
			LOG.debug("Resetting root logger level to " + level);
			rootLogger.setLevel(level);
		} else {
			LOG.debug("Root logger level unspecified, leaving as is.");
		}
		Enumeration appenders = rootLogger.getAllAppenders();
		while (appenders.hasMoreElements()) {
			resetLogLevel((Appender) appenders.nextElement(), smallLoggingConfiguration);
		}
	}

	private static void resetLogLevel(Logger logger,
			SmallLoggingConfiguration smallLoggingConfiguration) {
		if (logger == null) {
			throw new IllegalArgumentException("logger is null");
		}
		String name = logger.getName();
		if (name == null) {
			throw new IllegalArgumentException("logger name is null");
		}
		Level level = smallLoggingConfiguration.loggerLoggingLevels.get(name);
		if (level != null) {
			LOG.debug("Resetting logger " + name + " level to " + level);
			logger.setLevel(level);
		} else {
			LOG.debug("Logger " + name + " level unspecified, leaving as is.");
		}
		Enumeration appenders = logger.getAllAppenders();
		while (appenders.hasMoreElements()) {
			resetLogLevel((Appender) appenders.nextElement(), smallLoggingConfiguration);
		}
	}

	private static void resetLogLevel(Appender appender,
			SmallLoggingConfiguration smallLoggingConfiguration) {
		if (appender == null) {
			throw new IllegalArgumentException("appender is null");
		}
		String name = appender.getName();
		if (name == null) {
			throw new IllegalArgumentException("appender name is null");
		}
		Level level = smallLoggingConfiguration.appenderThresholdLevels.get(name);
		if (!(appender instanceof AppenderSkeleton)) {
			// TODO: set via reflection?
			LOG.debug("Unable to set threshold of appender " + name + ", class " + appender.getClass() + " not an AppenderSkeletion");
		}
		if (level != null) {
			LOG.debug("Resetting appender threshold " + name + " level to " + level);
			((AppenderSkeleton) appender).setThreshold(level);
		} else {
			LOG.debug("Appender " + name + " threshold unspecified, leaving as is.");
		}
	}

	private static SmallLoggingConfiguration parse(Element element) {
		SmallLoggingConfiguration smallLoggingConfiguration = new SmallLoggingConfiguration();
		if (element == null) {
			LOG.error("Log4j XML file has no document element");
		} else 	if (element.getTagName() == null) {
			LOG.error("Log4j XML file has a null document element tag");
		} else if (!element.getTagName().equalsIgnoreCase(LOG4J_CONFIGURATION_TAG) && 
				!element.getTagName().equalsIgnoreCase(LOG4J_OLD_CONFIGURATION_TAG)) {
			LOG.error("Log4j XML file has an invalid top level tag " + element.getTagName());
		} else {
			NodeList children = element.getChildNodes();
			final int length = children.getLength();
			for (int i = 0; i < length; i++) {
				Node currentNode = children.item(i);
				if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
					Element currentElement = (Element) currentNode;
					String tagName = currentElement.getTagName();
					if (tagName == null) continue;
					if (tagName.equalsIgnoreCase(LOG4J_APPENDER_TAG)) {
						parseAppender(currentElement, smallLoggingConfiguration);
					} else if (tagName.equalsIgnoreCase(LOG4J_ROOT_TAG)) {
						parseRoot(currentElement, smallLoggingConfiguration);
					} else if (tagName.equalsIgnoreCase(LOG4J_CATEGORY_TAG) || tagName.equalsIgnoreCase(LOG4J_LOGGER_TAG)) {
						parseLogger(currentElement, smallLoggingConfiguration);
					}
				}
			}
		}
		return smallLoggingConfiguration;
	}

	private static void parseAppender(Element element,
			SmallLoggingConfiguration smallLoggingConfiguration) {
		if (element == null) return;
		Level level = null;
		String name = subst(element.getAttribute(LOG4J_NAME_ATTRIBUTE));
		NodeList children = element.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node currentNode = children.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element currentElement = (Element) currentNode;
				String tagName = currentElement.getTagName();
				if (tagName == null) continue;
				if (tagName.equalsIgnoreCase(LOG4J_PARAM_TAG)) {
					String paramName = subst(currentElement.getAttribute(LOG4J_NAME_ATTRIBUTE));
					String paramVal = subst(currentElement.getAttribute(LOG4J_VALUE_ATTRIBUTE));
					if (paramName != null && paramName.equalsIgnoreCase(LOG4J_THRESHOLD_ATTRIBUTE_VALUE)
							&& paramVal != null && !paramVal.isEmpty()) {
						level = Level.toLevel(unExtremify(paramVal));
					}
				}
			}
		}
		if (level != null && name != null) {
			smallLoggingConfiguration.appenderThresholdLevels.put(name,  level);
		}
	}
		
	private static String subst(String s) {
		return OptionConverter.substVars(s,  System.getProperties());
	}

	private static void parseRoot(Element element,
			SmallLoggingConfiguration smallLoggingConfiguration) {
		if (element == null) return;
		Level level = null;
		NodeList children = element.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node currentNode = children.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element currentElement = (Element) currentNode;
				String tagName = currentElement.getTagName();
				if (tagName == null) continue;
				if (tagName.equalsIgnoreCase(LOG4J_PRIORITY_TAG) || tagName.equalsIgnoreCase(LOG4J_LEVEL_TAG)) {
					String levelStr = subst(currentElement.getAttribute(LOG4J_VALUE_ATTRIBUTE));
					if ((levelStr != null) && !levelStr.isEmpty()) {
						level = Level.toLevel(unExtremify(levelStr));
					}
				}
			}
		}
		if (level != null) {
			smallLoggingConfiguration.rootLogLevel = level;
		}
	}

	private static void parseLogger(Element element,
			SmallLoggingConfiguration smallLoggingConfiguration) {
		if (element == null) return;
		Level level = null;
		String name = subst(element.getAttribute(LOG4J_NAME_ATTRIBUTE));
		NodeList children = element.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++) {
			Node currentNode = children.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element currentElement = (Element) currentNode;
				String tagName = currentElement.getTagName();
				if (tagName == null) continue;
				if (tagName.equalsIgnoreCase(LOG4J_PRIORITY_TAG) || tagName.equalsIgnoreCase(LOG4J_LEVEL_TAG)) {
					String levelStr = subst(currentElement.getAttribute(LOG4J_VALUE_ATTRIBUTE));
					if ((levelStr != null) && !levelStr.isEmpty()) {
						level = Level.toLevel(unExtremify(levelStr));
					}
				}
			}
		}
		if (level != null && name != null) {
			smallLoggingConfiguration.loggerLoggingLevels.put(name,  level);
		}
	}
	
	private static String unExtremify(String levelStr) {
		if ("EXTREME".equalsIgnoreCase(levelStr) || "EXHAUST".equalsIgnoreCase(levelStr)) {
			return "TRACE";
		} else return levelStr;
	}
	
}

package com.eucalyptus.troubleshooting;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;

@ConfigurableClass( root = "troubleshooting",
description = "Parameters controlling troubleshooting information." )
public class LoggingConfiguration {
	private static final Logger LOG = Logger.getLogger(LoggingConfiguration.class);
	@ConfigurableField( description = "'true' if we override system log levels.",
			initial = "false", 
			changeListener = LogLevelOverrideListener.class,
			displayName = "euca.dynamic.log.level" )
	private static String ENABLE_TROUBLESHOOTING_LOG_LEVEL_OVERRIDE = "false";
	@ConfigurableField( description = "Log level for dynamic override.",
			initial = "", //TODO: figure out how to link System.property("euca.log.level")
			changeListener = LogLevelListener.class,
			displayName = "euca.dynamic.log.level" )
	private static String TROUBLESHOOTING_LOG_LEVEL = "";

	public static class LogLevelOverrideListener implements PropertyChangeListener {
		private final String[] logLevels = new String[] {"ALL", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "TRACE"};
		/**
		 * @see com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty,
		 *      java.lang.Object)
		 */
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			LOG.warn( "Change occurred to property " + t.getQualifiedName( ) + " with new value " + newValue + " which will reset logging levels." );
			try {
				t.getField( ).set( null, t.getTypeParser( ).apply( newValue ) );
			} catch ( IllegalArgumentException e1 ) {
				e1.printStackTrace();
				throw new ConfigurablePropertyException( e1 );
			} catch ( IllegalAccessException e1 ) {
				e1.printStackTrace();
				throw new ConfigurablePropertyException( e1 );
			}
		}
	}

	public static class LogLevelListener implements PropertyChangeListener {
		private final String[] logLevels = new String[] {"ALL", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "TRACE"};
		/**
		 * @see com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty,
		 *      java.lang.Object)
		 */
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			String newLogLevel = (String) newValue;
			if (newLogLevel == null || !Arrays.asList(logLevels).contains(newLogLevel.toUpperCase())) {
				throw new ConfigurablePropertyException("Invalid log level " + newLogLevel);
			}
			// TODO: add error checking for property and configuration
			LOG.warn( "Change occurred to property " + t.getQualifiedName( ) + " with new value " + newValue + " which will reset logging levels." );
			try {
				t.getField( ).set( null, t.getTypeParser( ).apply( newValue ) );
			} catch ( IllegalArgumentException e1 ) {
				e1.printStackTrace();
				throw new ConfigurablePropertyException( e1 );
			} catch ( IllegalAccessException e1 ) {
				e1.printStackTrace();
				throw new ConfigurablePropertyException( e1 );
			}
			if (Boolean.parseBoolean(ENABLE_TROUBLESHOOTING_LOG_LEVEL_OVERRIDE)) {
				System.setProperty("euca.log.level", newLogLevel.toUpperCase());
				LoggingResetter.resetLoggingLevels();
			}
			LOG.fatal("test level FATAL");
			LOG.error("test level ERROR");
			LOG.warn("test level WARN");
			LOG.info("test level INFO");
			LOG.debug("test level DEBUG");
			LOG.trace("test level TRACE");
		}
	}
}

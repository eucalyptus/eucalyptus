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

package com.eucalyptus.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.troubleshooting.LoggingResetter;
import com.eucalyptus.troubleshooting.TestFaultTrigger;
import com.eucalyptus.troubleshooting.fault.FaultSubsystem;
import com.eucalyptus.troubleshooting.resourcefaults.DBResourceCheck;
import com.eucalyptus.troubleshooting.resourcefaults.DiskResourceCheck;
import com.eucalyptus.troubleshooting.resourcefaults.DiskResourceCheck.Checker;
import com.eucalyptus.troubleshooting.resourcefaults.DiskResourceCheck.LocationInfo;
import com.eucalyptus.troubleshooting.resourcefaults.MXBeanMemoryResourceCheck;
import com.eucalyptus.troubleshooting.resourcefaults.SimpleMemoryResourceCheck;
@Provides(Empyrean.class)
@RunDuring( Bootstrap.Stage.CloudServiceInit )
@ConfigurableClass( root = "cloud",
description = "Parameters controlling troubleshooting information." )
public class TroubleshootingBootstrapper extends Bootstrapper {
	private static final Logger          LOG = Logger.getLogger( TroubleshootingBootstrapper.class );

	  @Override
	  public boolean load( ) throws Exception {
	    return true;
	  }
	  
	  @Override
	  public boolean start( ) throws Exception {
	    LOG.info( "Starting troubleshooting interface." );
	    //DiskResourceCheck check = new DiskResourceCheck();
	    // TOOD: we should use a property, but for now use 2% of the log directory
	    File logFileDir = BaseDirectory.LOG.getFile();
	    //check.addLocationInfo(logFileDir, (long) (0.02 * logFileDir.getTotalSpace()));
	    //check.start();
	    LocationInfo location = new LocationInfo(logFileDir, (long) (0.02 * logFileDir.getTotalSpace()));
	    DiskResourceCheck.start(new Checker(location));
	    new DBResourceCheck().start();
	    //	    new SimpleMemoryResourceCheck(1).start(512 * 1024).start(); // 512K left, also arbitrary
	    //new MXBeanMemoryResourceCheck().start(); // 512K left, also arbitrary
	    FaultSubsystem.init();
	    return true;
	  }
	  
	  /**
	   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
	   */
	  @Override
	  public boolean enable( ) throws Exception {
	    return true;
	  }
	  
	  /**
	   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
	   */
	  @Override
	  public boolean stop( ) throws Exception {
	    return true;
	  }
	  
	  /**
	   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
	   */
	  @Override
	  public void destroy( ) throws Exception {}
	  
	  /**
	   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
	   */
	  @Override
	  public boolean disable( ) throws Exception {
	    return true;
	  }
	  
	  /**
	   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
	   */
	  @Override
	  public boolean check( ) throws Exception {
	    return true;
	  }
	  


//	@ConfigurableField( description = "Fault id last used to trigger test",
//			initial = "", //TODO: figure out how to link System.property("euca.log.level")
//			changeListener = TriggerFaultListener.class,
//			displayName = "trigger.fault" )
//	public static String TRIGGER_FAULT = "";
//
//
//	public static class TriggerFaultListener implements PropertyChangeListener {
//		/**
//		 * @see com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty,
//		 *      java.lang.Object)
//		 */
//		@Override
//		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
//			if (newValue == null) {
//				newValue = "";
//			}
//			LOG.info( "Triggering fault with params " + newValue);
//			List<String> args = split((String) newValue);
//			if (args == null) throw new ConfigurablePropertyException("Invalid params for fault trigger " + newValue);
//			if (args.size() < 1) throw new ConfigurablePropertyException("No fault id specified for fault trigger " + newValue);
//			int faultId = -1;
//			try {
//				faultId = Integer.parseInt(args.get(0));
//			} catch (Exception ex) {
//				throw new ConfigurablePropertyException("Invalid params for fault trigger " + newValue);
//			}
//			// There should be an odd number of arguments.  A fault id, and an even number of key value
//			// pairs.  If there are not, discard the last value
//			if (args.size() % 2 == 0) {
//				LOG.warn("Unmatched key/value pairs in fault trigger, ignoring last value " + args.get(args.size() - 1));
//				args.remove(args.size() - 1);
//			}
//			Properties varProps = new Properties();
//			for (int i=1;i<args.size();i+=2) {
//				varProps.setProperty(args.get(i), args.get(i+1));
//			}
//			TestFaultTrigger.triggerFault(faultId, varProps);
//			LOG.info("Triggered fault with params " + newValue);
//			throw new ConfigurablePropertyException("Fault triggered, value not persisted");
//		}
//
//		private List<String> split(String s) {
//			ArrayList<String> retVal = new ArrayList<String>();
//			if (s != null) {
//				StringTokenizer stok = new StringTokenizer(s);
//				while (stok.hasMoreTokens()) {
//					retVal.add(stok.nextToken());
//				}
//			}
//			return retVal;
//		}
//	}
//
	
	@ConfigurableField( description = "Log level for dynamic override.",
			initial = "", //TODO: figure out how to link System.property("euca.log.level")
			changeListener = LogLevelListener.class,
			displayName = "euca.log.level" )
	public static String EUCA_LOG_LEVEL = "";

	public static class LogLevelListener implements PropertyChangeListener {
		private final String[] logLevels = new String[] {"ALL", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", "TRACE", "OFF", "EXTREME"};
		/**
		 * @see com.eucalyptus.configurable.PropertyChangeListener#fireChange(com.eucalyptus.configurable.ConfigurableProperty,
		 *      java.lang.Object)
		 */
		@Override
		public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
			if (newValue == null) {
				newValue = "";
			}
			String newLogLevel = (String) newValue;
			newLogLevel = newLogLevel.trim().toUpperCase();
			if (!(newLogLevel.isEmpty() || Arrays.asList(logLevels).contains(newLogLevel))) {
				throw new ConfigurablePropertyException("Invalid log level " + newLogLevel);
			}
			// TODO: add error checking for property and configuration
			LOG.warn( "Change occurred to property " + t.getQualifiedName( ) + " with new value " + newValue + "." );
			try {
				t.getField( ).set( null, t.getTypeParser( ).apply( newValue ) );
			} catch ( IllegalArgumentException e1 ) {
				e1.printStackTrace();
				throw new ConfigurablePropertyException( e1 );
			} catch ( IllegalAccessException e1 ) {
				e1.printStackTrace();
				throw new ConfigurablePropertyException( e1 );
			}
			if (!newLogLevel.isEmpty()) {
				System.setProperty("euca.log.level", newLogLevel.toUpperCase());
				LoggingResetter.resetLoggingWithXML();
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

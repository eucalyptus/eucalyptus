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

package com.eucalyptus.util;

import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import org.apache.log4j.Logger;

public class DNSProperties {
	private static Logger LOG = Logger.getLogger( DNSProperties.class );
	public static final String LOCALHOST_DOMAIN = "localhost";
	public static final String LOCALHOST_NS = "nshost." + LOCALHOST_DOMAIN;
	public static final String LOCALHOST_IP = "127.0.0.1";
	public static String DB_NAME             = "eucalyptus_dns";
	public static String ADDRESS = "0.0.0.0";
	public static int PORT = 53;
	public static int MAX_MESSAGE_SIZE = 1024;
	public static String DOMAIN = LOCALHOST_DOMAIN;
	public static String NS_HOST = "nshost." + DOMAIN;
	public static String NS_IP = LOCALHOST_IP;
	
	
	public static String getDomain() {
	  final SystemConfiguration systemConfiguration = SystemConfiguration.getSystemConfiguration();
	  return systemConfiguration.getDnsDomain();
	}
	
	//// doesn't seem to be called from anywhere
	public static void update() {
		SystemConfiguration systemConfiguration = SystemConfiguration.getSystemConfiguration();
		DOMAIN = systemConfiguration.getDnsDomain();
		NS_HOST = systemConfiguration.getNameserver();
		NS_IP = systemConfiguration.getNameserverAddress();
	}
}

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

package com.eucalyptus.webui.server;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.eucalyptus.system.BaseDirectory;
import com.google.common.base.Strings;

public class WebProperties {
  
  public static final String VERSION = "version";
  
  private static final Logger LOG = Logger.getLogger( WebProperties.class );
  
  private static String PROPERTIES_FILE =  BaseDirectory.CONF.toString() + File.separator + "eucalyptus-web.properties";
  
  private static final String EUCA_VERSION = "euca.version";
  
  public static final String ACCOUNT_SIGNUP_SUBJECT = "account-signup-subject";
  public static final String ACCOUNT_SIGNUP_SUBJECT_DEFAULT = "[Eucalyptus] New account has been signed up";

  public static final String USER_SIGNUP_SUBJECT = "user-signup-subject";
  public static final String USER_SIGNUP_SUBJECT_DEFAULT = "[Eucalyptus] New user has signed up";

  public static final String ACCOUNT_APPROVAL_SUBJECT = "account-approval-subject";
  public static final String ACCOUNT_APPROVAL_SUBJECT_DEFAULT = "Your Eucalyptus account application was approved";

  public static final String ACCOUNT_REJECTION_SUBJECT = "account-rejection-subject";
  public static final String ACCOUNT_REJECTION_SUBJECT_DEFAULT = "Your Eucalyptus account application was rejected";

  public static final String ACCOUNT_REJECTION_MESSAGE = "account-rejection-message";
  public static final String ACCOUNT_REJECTION_MESSAGE_DEFAULT = "To whom it may concern: \n\n I'm sorry to let your know that " +
                                                                 "your application for an Eucalyptus account was rejected. " +
                                                                 "Please contact your system administrator for further information." +
                                                                 "\n\n --Registration Admin";

  public static final String USER_APPROVAL_SUBJECT = "user-approval-subject";
  public static final String USER_APPROVAL_SUBJECT_DEFAULT = "Your Eucalyptus user application was approved";

  public static final String USER_REJECTION_SUBJECT = "user-rejection-subject";
  public static final String USER_REJECTION_SUBJECT_DEFAULT = "Your Eucalyptus user application was rejected";
  
  public static final String USER_REJECTION_MESSAGE = "user-rejection-message";
  public static final String USER_REJECTION_MESSAGE_DEFAULT = "To whom it may concern: \n\n I'm sorry to let your know that " +
                                                                 "your application for an Eucalyptus user account was rejected. " +
                                                                 "Please contact your system administrator for further information." +
                                                                 "\n\n --Registration Admin";
  
  public static final String PASSWORD_RESET_SUBJECT = "password-reset-subject";
  public static final String PASSWORD_RESET_SUBJECT_DEFAULT = "Request to reset your Eucalyptus password";

  public static final String PASSWORD_RESET_MESSAGE = "password-reset-message";
  public static final String PASSWORD_RESET_MESSAGE_DEFAULT = "You or someone pretending to be you made a request to " + 
                                                              "reset the password on a Eucalyptus elastic cloud system. " +
                                                              "Disregard this message if resetting the password was not your intention, " +
                                                              "but if it was, click the following link to change of password:";

  public static final String RIGHTSCALE_WHOAMI_URL = "rightscale-whoami-url";
  public static final String RIGHTSCALE_WHOAMI_URL_DEFAULT = "https://my.rightscale.com/whoami?api_version=1.0&cloud=0";
  
  public static final String TOOL_DOWNLOAD_URL = "tool-download-url";
  public static final String TOOL_DOWNLOAD_URL_DEFAULT = "http://tools.eucalyptus.com/list.php?version=";
  
  public static HashMap<String, String> getProperties( ) {
    Properties props = new Properties( );
    FileInputStream input = null;
    try {
      input = new FileInputStream( PROPERTIES_FILE );
      props.load( input );
      props.setProperty( VERSION, "Eucalyptus " + System.getProperty( EUCA_VERSION ) );    
    } catch ( Exception e ) {
      LOG.error( "Failed to load web properties", e );
    } finally {
      if ( input != null ) {
        try {
          input.close( );
        } catch ( Exception e ) { }
      }
    }
    return new HashMap<String, String>( ( Map ) props );
  }
  
  public static String getProperty( String key, String defaultValue ) {
    String subject = getProperties( ).get( key );
    if ( Strings.isNullOrEmpty( subject ) ) {
      subject = defaultValue;
    }
    return subject;
  }
  
  public static String getVersion( ) {
    return System.getProperty( EUCA_VERSION );
  }
  
}

/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.bootstrap;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;
import org.jgroups.util.ThreadFactory;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;

public class MembershipConfiguration {
  private static Logger LOG                        = Logger.getLogger( MembershipConfiguration.class );
  private static String        multicastAddress           = "228.7.7.3";
  private static Integer       multicastPort              = 8773;
  private static Integer       threadPoolMaxThreads       = 25;
  private static Integer       threadPoolMinThreads       = 2;
  private static Integer       threadPoolKeepAliveTime    = 5000;
  private static Boolean       threadPoolQueueEnabled     = Boolean.TRUE;
  private static String        regularRejectionPolicy     = "RUN";
  private static String        oobRejectionPolicy         = "RUN";
  private static Integer       oobThreadPoolMaxThreads    = 25;
  private static Integer       oobThreadPoolMinThreads    = 2;
  private static Integer       oobThreadPoolKeepAliveTime = 5000;
  
  public static ThreadPool getThreadPool( ) {
    return Threads.lookup( Empyrean.class, HostManager.class );
  }
  
  public static ThreadPool getNormalThreadPool( ) {
    return Threads.lookup( Empyrean.class, HostManager.class, "normal-pool" );
  }
  
  public static ThreadPool getOOBThreadPool( ) {
    return Threads.lookup( Empyrean.class, HostManager.class, "oob-pool" );
  }
  
  public static String getMulticastAddress( ) {
    return MembershipConfiguration.multicastAddress;
  }
  
  public static InetAddress getMulticastInetAddress( ) {
    try {
      return InetAddress.getByName( MembershipConfiguration.multicastAddress );
    } catch ( UnknownHostException ex ) {
      LOG.error( ex, ex );
      BootstrapException.throwFatal( "Failed to construct membership protocol because of " + ex.getMessage( ), ex );
      throw new RuntimeException( ex );//never executed because of System.exit( -1 ) in throwFatal.
    }
  }
  
  public static void setMulticastAddress( String multicastAddress ) {
    MembershipConfiguration.multicastAddress = multicastAddress;
  }
  
  public static Integer getMulticastPort( ) {
    return MembershipConfiguration.multicastPort;
  }
  
  public static void setMulticastPort( Integer multicastPort ) {
    MembershipConfiguration.multicastPort = multicastPort;
  }
  
  public static Integer getThreadPoolMaxThreads( ) {
    return MembershipConfiguration.threadPoolMaxThreads;
  }
  
  public static void setThreadPoolMaxThreads( Integer threadPoolMaxThreads ) {
    MembershipConfiguration.threadPoolMaxThreads = threadPoolMaxThreads;
  }
  
  public static Integer getThreadPoolMinThreads( ) {
    return MembershipConfiguration.threadPoolMinThreads;
  }
  
  public static void setThreadPoolMinThreads( Integer threadPoolMinThreads ) {
    MembershipConfiguration.threadPoolMinThreads = threadPoolMinThreads;
  }
  
  public static Integer getThreadPoolKeepAliveTime( ) {
    return MembershipConfiguration.threadPoolKeepAliveTime;
  }
  
  public static void setThreadPoolKeepAliveTime( Integer threadPoolKeepAliveTime ) {
    MembershipConfiguration.threadPoolKeepAliveTime = threadPoolKeepAliveTime;
  }
  
  public static Boolean getThreadPoolQueueEnabled( ) {
    return MembershipConfiguration.threadPoolQueueEnabled;
  }
  
  public static void setThreadPoolQueueEnabled( Boolean threadPoolQueueEnabled ) {
    MembershipConfiguration.threadPoolQueueEnabled = threadPoolQueueEnabled;
  }
  
  public static String getRegularRejectionPolicy( ) {
    return MembershipConfiguration.regularRejectionPolicy;
  }
  
  public static void setRegularRejectionPolicy( String regularRejectionPolicy ) {
    MembershipConfiguration.regularRejectionPolicy = regularRejectionPolicy;
  }
  
  public static Integer getOobThreadPoolMaxThreads( ) {
    return MembershipConfiguration.oobThreadPoolMaxThreads;
  }
  
  public static void setOobThreadPoolMaxThreads( Integer oobThreadPoolMaxThreads ) {
    MembershipConfiguration.oobThreadPoolMaxThreads = oobThreadPoolMaxThreads;
  }
  
  public static Integer getOobThreadPoolMinThreads( ) {
    return MembershipConfiguration.oobThreadPoolMinThreads;
  }
  
  public static void setOobThreadPoolMinThreads( Integer oobThreadPoolMinThreads ) {
    MembershipConfiguration.oobThreadPoolMinThreads = oobThreadPoolMinThreads;
  }
  
  public static Integer getOobThreadPoolKeepAliveTime( ) {
    return MembershipConfiguration.oobThreadPoolKeepAliveTime;
  }
  
  public static void setOobThreadPoolKeepAliveTime( Integer oobThreadPoolKeepAliveTime ) {
    MembershipConfiguration.oobThreadPoolKeepAliveTime = oobThreadPoolKeepAliveTime;
  }
  
  public static String getOobRejectionPolicy( ) {
    return MembershipConfiguration.oobRejectionPolicy;
  }
  
  public static void setOobRejectionPolicy( String oobRejectionPolicy ) {
    MembershipConfiguration.oobRejectionPolicy = oobRejectionPolicy;
  }
  
}

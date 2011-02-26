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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.bootstrap;

import org.jgroups.util.ThreadFactory;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.system.Threads.ThreadPool;

public class MembershipConfiguration {
  private String  multicastAddress        = "228.7.7.3";
  private Integer multicastPort           = 8772;
  private Integer threadPoolMaxThreads    = 25;
  private Integer threadPoolMinThreads    = 2;
  private Integer threadPoolKeepAliveTime = 5000;
  private Boolean threadPoolQueueEnabled  = Boolean.TRUE;
  private String  regularRejectionPolicy  = "RUN";
  private String  oobRejectionPolicy  = "RUN";
  private Integer oobThreadPoolMaxThreads    = 25;
  private Integer oobThreadPoolMinThreads    = 2;
  private Integer oobThreadPoolKeepAliveTime = 5000;
  
  private MembershipConfiguration( ) {}
  
  public static MembershipConfiguration getInstance( ) {
    return new MembershipConfiguration( );
  }
  
  public ThreadPool getThreadPool( ) {
    return Threads.lookup( Empyrean.class, MembershipManager.class );
  }

  public ThreadPool getNormalThreadPool( ) {
    return Threads.lookup( Empyrean.class, MembershipManager.class, "normal-pool" );
  }

  public ThreadPool getOOBThreadPool( ) {
    return Threads.lookup( Empyrean.class, MembershipManager.class, "oob-pool" );
  }

  
  public String getMulticastAddress( ) {
    return this.multicastAddress;
  }
  
  public void setMulticastAddress( String multicastAddress ) {
    this.multicastAddress = multicastAddress;
  }
  
  public Integer getMulticastPort( ) {
    return this.multicastPort;
  }
  
  public void setMulticastPort( Integer multicastPort ) {
    this.multicastPort = multicastPort;
  }
  

  public Integer getThreadPoolMaxThreads( ) {
    return this.threadPoolMaxThreads;
  }

  public void setThreadPoolMaxThreads( Integer threadPoolMaxThreads ) {
    this.threadPoolMaxThreads = threadPoolMaxThreads;
  }

  public Integer getThreadPoolMinThreads( ) {
    return this.threadPoolMinThreads;
  }

  public void setThreadPoolMinThreads( Integer threadPoolMinThreads ) {
    this.threadPoolMinThreads = threadPoolMinThreads;
  }

  public Integer getThreadPoolKeepAliveTime( ) {
    return this.threadPoolKeepAliveTime;
  }

  public void setThreadPoolKeepAliveTime( Integer threadPoolKeepAliveTime ) {
    this.threadPoolKeepAliveTime = threadPoolKeepAliveTime;
  }

  public Boolean getThreadPoolQueueEnabled( ) {
    return this.threadPoolQueueEnabled;
  }

  public void setThreadPoolQueueEnabled( Boolean threadPoolQueueEnabled ) {
    this.threadPoolQueueEnabled = threadPoolQueueEnabled;
  }

  public String getRegularRejectionPolicy( ) {
    return this.regularRejectionPolicy;
  }

  public void setRegularRejectionPolicy( String regularRejectionPolicy ) {
    this.regularRejectionPolicy = regularRejectionPolicy;
  }

  public Integer getOobThreadPoolMaxThreads( ) {
    return this.oobThreadPoolMaxThreads;
  }

  public void setOobThreadPoolMaxThreads( Integer oobThreadPoolMaxThreads ) {
    this.oobThreadPoolMaxThreads = oobThreadPoolMaxThreads;
  }

  public Integer getOobThreadPoolMinThreads( ) {
    return this.oobThreadPoolMinThreads;
  }

  public void setOobThreadPoolMinThreads( Integer oobThreadPoolMinThreads ) {
    this.oobThreadPoolMinThreads = oobThreadPoolMinThreads;
  }

  public Integer getOobThreadPoolKeepAliveTime( ) {
    return this.oobThreadPoolKeepAliveTime;
  }

  public void setOobThreadPoolKeepAliveTime( Integer oobThreadPoolKeepAliveTime ) {
    this.oobThreadPoolKeepAliveTime = oobThreadPoolKeepAliveTime;
  }

  public String getOobRejectionPolicy( ) {
    return this.oobRejectionPolicy;
  }

  public void setOobRejectionPolicy( String oobRejectionPolicy ) {
    this.oobRejectionPolicy = oobRejectionPolicy;
  }

}

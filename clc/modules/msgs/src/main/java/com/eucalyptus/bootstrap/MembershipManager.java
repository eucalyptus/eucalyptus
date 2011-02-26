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

import java.net.InetAddress;
import org.jgroups.JChannel;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.FC;
import org.jgroups.protocols.FD;
import org.jgroups.protocols.FD_SOCK;
import org.jgroups.protocols.FRAG2;
import org.jgroups.protocols.MERGE2;
import org.jgroups.protocols.MFC;
import org.jgroups.protocols.PING;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.UFC;
import org.jgroups.protocols.UNICAST;
import org.jgroups.protocols.VERIFY_SUSPECT;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;
import org.jgroups.stack.ProtocolStack;

public class MembershipManager {
  public static short PROTOCOL_ID = 512;
  public static JChannel buildChannel( ) throws Exception {
    
    final JChannel channel = new JChannel( false );
    ProtocolStack stack = new ProtocolStack( ) {
      {
        channel.setProtocolStack( this );
        this.addProtocol( new UDP( ) {
          {
            ClassConfigurator.addProtocol( PROTOCOL_ID++, this.getClass( ) );
            setMulticastAddress( InetAddress.getByName( MembershipConfiguration.getInstance( ).getMulticastAddress( ) ) );
            setMulticastPort( MembershipConfiguration.getInstance( ).getMulticastPort( ) );
            setDiscardIncompatiblePackets( true );
            setMaxBundleSize( 60000 );
            setMaxBundleTimeout( 30 );
            
            setDefaultThreadPool( MembershipConfiguration.getInstance( ).getThreadPool( ) );
            setDefaultThreadPoolThreadFactory( MembershipConfiguration.getInstance( ).getThreadPool( ) );
            
            setThreadFactory( MembershipConfiguration.getInstance( ).getNormalThreadPool( ) );
            setThreadPoolMaxThreads( MembershipConfiguration.getInstance( ).getThreadPoolMaxThreads( ) );
            setThreadPoolKeepAliveTime( MembershipConfiguration.getInstance( ).getThreadPoolKeepAliveTime( ) );
            setThreadPoolMinThreads( MembershipConfiguration.getInstance( ).getThreadPoolMinThreads( ) );
            setThreadPoolQueueEnabled( MembershipConfiguration.getInstance( ).getThreadPoolQueueEnabled( ) );
            setRegularRejectionPolicy( MembershipConfiguration.getInstance( ).getRegularRejectionPolicy( ) );
            
            setOOBThreadPoolThreadFactory( MembershipConfiguration.getInstance( ).getOOBThreadPool( ) );
            setOOBThreadPool( MembershipConfiguration.getInstance( ).getOOBThreadPool( ) );
            setOOBThreadPoolMaxThreads( MembershipConfiguration.getInstance( ).getOobThreadPoolMaxThreads( ) );
            setOOBThreadPoolKeepAliveTime( MembershipConfiguration.getInstance( ).getOobThreadPoolKeepAliveTime( ) );
            setOOBThreadPoolMinThreads( MembershipConfiguration.getInstance( ).getOobThreadPoolMinThreads( ) );
            setOOBRejectionPolicy( MembershipConfiguration.getInstance( ).getOobRejectionPolicy( ) );
          }
        } );
        this.addProtocol( new PING( ) {
          {
            ClassConfigurator.addProtocol( PROTOCOL_ID++, this.getClass( ) );
            this.setTimeout( 2000 );
            this.setNumInitialMembers( 2 );
          }
        } );
        this.addProtocol( new MERGE2( ) );
        this.addProtocol( new FD_SOCK( ) );
        this.addProtocol( new FD( ) {
          {
            ClassConfigurator.addProtocol( PROTOCOL_ID++, this.getClass( ) );
            this.setTimeout( 10000 );
            this.setMaxTries( 5 );
            this.setShun( true );
          }
        } );
        this.addProtocol( new VERIFY_SUSPECT( ) );
//        this.addProtocol( new BARRIER( ) );
        this.addProtocol( new NAKACK( ) {
          {
            ClassConfigurator.addProtocol( PROTOCOL_ID++, this.getClass( ) );
            this.setUseMcastXmit( false );
            this.setDiscardDeliveredMsgs( true );
            this.setGcLag( 0 );
//            this.setProperty( "retransmit_timeout", "300,600,1200,2400,4800" );
          }
        } );
        this.addProtocol( new UNICAST( ) );
        this.addProtocol( new STABLE( ) {
          {
            ClassConfigurator.addProtocol( PROTOCOL_ID++, this.getClass( ) );
            this.setDesiredAverageGossip( 50000 );
            this.setMaxBytes( 400000 );
//            this.setStabilityDelay( 1000 );
          }
        } );
        this.addProtocol( new GMS( ) {
          {
            ClassConfigurator.addProtocol( PROTOCOL_ID++, this.getClass( ) );
            this.setPrintLocalAddress( true );
            this.setJoinTimeout( 3000 );
            this.setShun( false );
            this.setViewBundling( true );
          }
        } );
        this.addProtocol( new FC( ) {
          {
            ClassConfigurator.addProtocol( PROTOCOL_ID++, this.getClass( ) );
            this.setMaxCredits( 20000000 );
            this.setMinThreshold( 0.1 );
          }
        } );
//        this.addProtocol( new UFC( ) );
//        this.addProtocol( new MFC( ) );
        this.addProtocol( new FRAG2( ) {
          {
            ClassConfigurator.addProtocol( PROTOCOL_ID++, this.getClass( ) );
          }
        } );
        this.addProtocol( new STATE_TRANSFER( ) );
      }
    };
    stack.init( );
    return channel;
  }
}

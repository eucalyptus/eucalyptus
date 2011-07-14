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

import java.net.UnknownHostException;
import java.util.List;
import org.apache.log4j.Logger;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.BARRIER;
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
import org.jgroups.stack.Protocol;
import com.eucalyptus.util.Internets;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

public class Protocols {
  
  private static Logger LOG         = Logger.getLogger( Protocols.class );
  public static short   PROTOCOL_ID = 512;
  
  private static String registerProtocol( Protocol p ) {
    if ( ClassConfigurator.getProtocolId( p.getClass( ) ) == 0 ) {
      ClassConfigurator.addProtocol( ++PROTOCOL_ID, p.getClass( ) );
    }
    return "euca-" + ( p.getClass( ).isAnonymousClass( )
      ? p.getClass( ).getSuperclass( ).getSimpleName( ).toLowerCase( )
      : p.getClass( ).getSimpleName( ).toLowerCase( ) ) + "-protocol";
  }
  
  public static List<Protocol> getMembershipProtocolStack( ) {
    return Lists.newArrayList( udp.get( ), ping.get( ), merge2.get( ), fdSocket.get( ), fd.get( ), verifySuspect.get( ), nakack.get( ), unicast.get( ),
                               stable.get( ), groupMemberShip.get( ), flowControl.get( ), fragmentation.get( ), stateTransfer.get( ) );
  }
  
  private static final Supplier<Protocol> udp                  = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   UDP protocol = new UDP( );
                                                                   try {
                                                                     protocol.setBindAddress( Internets.localHostAddress( ) );
                                                                     protocol.setBindPort( 8773 );
                                                                     protocol.setBindToAllInterfaces( true );
                                                                   } catch ( UnknownHostException ex ) {
                                                                     LOG.error( ex, ex );
                                                                   }
                                                                   protocol.setMulticastAddress( MembershipConfiguration.getMulticastInetAddress( ) );
                                                                   protocol.setMulticastPort( MembershipConfiguration.getMulticastPort( ) );
                                                                   protocol.setDiscardIncompatiblePackets( true );
                                                                   protocol.setLogDiscardMessages( false );
                                                                   protocol.setMaxBundleSize( 60000 );
                                                                   protocol.setMaxBundleTimeout( 30 );
                                                                   
                                                                   protocol.setDefaultThreadPool( MembershipConfiguration.getThreadPool( ) );
                                                                   protocol.setDefaultThreadPoolThreadFactory( MembershipConfiguration.getThreadPool( ) );
                                                                   
                                                                   protocol.setThreadFactory( MembershipConfiguration.getNormalThreadPool( ) );
                                                                   protocol.setThreadPoolMaxThreads( MembershipConfiguration.getThreadPoolMaxThreads( ) );
                                                                   protocol.setThreadPoolKeepAliveTime( MembershipConfiguration.getThreadPoolKeepAliveTime( ) );
                                                                   protocol.setThreadPoolMinThreads( MembershipConfiguration.getThreadPoolMinThreads( ) );
                                                                   protocol.setThreadPoolQueueEnabled( MembershipConfiguration.getThreadPoolQueueEnabled( ) );
                                                                   protocol.setRegularRejectionPolicy( MembershipConfiguration.getRegularRejectionPolicy( ) );
                                                                   
                                                                   protocol.setOOBThreadPoolThreadFactory( MembershipConfiguration.getOOBThreadPool( ) );
                                                                   protocol.setOOBThreadPool( MembershipConfiguration.getOOBThreadPool( ) );
                                                                   protocol.setOOBThreadPoolMaxThreads( MembershipConfiguration.getOobThreadPoolMaxThreads( ) );
                                                                   protocol.setOOBThreadPoolKeepAliveTime( MembershipConfiguration.getOobThreadPoolKeepAliveTime( ) );
                                                                   protocol.setOOBThreadPoolMinThreads( MembershipConfiguration.getOobThreadPoolMinThreads( ) );
                                                                   protocol.setOOBRejectionPolicy( MembershipConfiguration.getOobRejectionPolicy( ) );
                                                                   return protocol;
                                                                 }
                                                               };
  private static final Supplier<Protocol> ping                 = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   PING protocol = new PING( );
                                                                   protocol.setTimeout( 2000 );
                                                                   protocol.setNumInitialMembers( 2 );
                                                                   return protocol;
                                                                 }
                                                               };
  private static final Supplier<Protocol> merge2               = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   return new MERGE2( );
                                                                 }
                                                               };
  
  private static final Supplier<Protocol> fdSocket             = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   return new FD_SOCK( );
                                                                 }
                                                               };
  private static final Supplier<Protocol> fd                   = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   FD protocol = new FD( );
                                                                   protocol.setTimeout( 10000 );
                                                                   protocol.setMaxTries( 5 );
                                                                   protocol.setShun( true );
                                                                   return protocol;
                                                                 }
                                                               };
  private static final Supplier<Protocol> verifySuspect        = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   return new VERIFY_SUSPECT( );
                                                                 }
                                                               };
  private static final Supplier<Protocol> barrier              = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   return new BARRIER( );
                                                                 }
                                                               };
  private static final Supplier<Protocol> nakack               = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   NAKACK protocol = new NAKACK( );
                                                                   protocol.setUseMcastXmit( true );
                                                                   protocol.setDiscardDeliveredMsgs( true );
                                                                   protocol.setGcLag( 25 );
                                                                   protocol.setMaxXmitBufSize( 50 );
//                                                                   protocol.setProperty( "retransmit_timeout", "300,600,1200,2400,4800" );
                                                                   return protocol;
                                                                 }
                                                               };
  
  private static final Supplier<Protocol> unicast              = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   return new UNICAST( );
                                                                 }
                                                               };
  private static final Supplier<Protocol> stable               = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   STABLE protocol = new STABLE( );
                                                                   protocol.setDesiredAverageGossip( 50000 );
                                                                   protocol.setMaxBytes( 400000 );
//                                                                   protocol.setStabilityDelay( 1000 );
                                                                   return protocol;
                                                                 }
                                                               };
  private static final Supplier<Protocol> groupMemberShip      = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   GMS protocol = new GMS( );
                                                                   protocol.setPrintLocalAddress( true );
                                                                   protocol.setJoinTimeout( 3000 );
                                                                   protocol.setShun( false );
                                                                   protocol.setViewBundling( true );
                                                                   return protocol;
                                                                 }
                                                               };
  private static final Supplier<Protocol> flowControl          = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   FC protocol = new FC( );
                                                                   protocol.setMaxCredits( 20000000 );
                                                                   protocol.setMinThreshold( 0.1 );
                                                                   return protocol;
                                                                 }
                                                               };
  private static final Supplier<Protocol> unicastFlowControl   = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   return new UFC( );
                                                                 }
                                                               };
  private static final Supplier<Protocol> multicastFlowControl = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   return new MFC( );
                                                                 }
                                                               };
  private static final Supplier<Protocol> fragmentation        = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   return new FRAG2( );
                                                                 }
                                                               };
  private static final Supplier<Protocol> stateTransfer        = new Supplier<Protocol>( ) {
                                                                 
                                                                 @Override
                                                                 public Protocol get( ) {
                                                                   return new STATE_TRANSFER( );
                                                                 }
                                                               };
  
}

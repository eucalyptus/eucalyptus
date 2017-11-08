/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

import org.apache.log4j.Logger
import org.jgroups.protocols.Discovery
import org.jgroups.protocols.FC
import org.jgroups.protocols.FD
import org.jgroups.protocols.FD_SOCK
import org.jgroups.protocols.FRAG2
import org.jgroups.protocols.MERGE2
import org.jgroups.protocols.PING
import org.jgroups.protocols.TCP
import org.jgroups.protocols.TCPGOSSIP;
import org.jgroups.protocols.TP
import org.jgroups.protocols.UDP
import org.jgroups.protocols.UNICAST
import org.jgroups.protocols.VERIFY_SUSPECT
import org.jgroups.protocols.pbcast.GMS
import org.jgroups.protocols.pbcast.NAKACK
import org.jgroups.protocols.pbcast.STABLE
import org.jgroups.protocols.pbcast.STATE_TRANSFER
import org.jgroups.stack.GossipRouter;
import com.eucalyptus.bootstrap.BootstrapArgs
import com.eucalyptus.bootstrap.Hosts
import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.empyrean.Empyrean
import com.eucalyptus.system.Threads
import com.eucalyptus.util.Internets
import com.google.common.collect.Sets;

Logger LOG = Logger.getLogger( "com.eucalyptus.scripts.setup_membership" );

/**
 * UDP/Multicast configuration, use multicastAddress '228.7.7.3' for compatibility with pre-4.3 default
 */
String        multicastAddress           = System.getProperty('euca.mcast.addr') ?: '239.193.7.3';
InetAddress   multicastInetAddress       = InetAddress.getByName( multicastAddress );
Integer       multicastPort              = 8773;
/**
 * TCP/TCPGOSSIP configuration
 */
Integer       tcpPortBase                = 8700;
Integer       tcpPortRange               = 100;
Integer       gossipPort                 = 8778;
String        gossipBindAddr             = Internets.localHostAddress( );
/**
 * TCP Failure Detection
 */
Integer       tcpFailurePortBase         = 8779;
Integer       tcpFailurePortRange        = 50
/**
 * General Transport thread configuration
 */
Integer       threadPoolMaxThreads       = 25;
Integer       threadPoolMinThreads       = 2;
Integer       threadPoolKeepAliveTime    = 5000;
Boolean       threadPoolQueueEnabled     = Boolean.TRUE;
String        regularRejectionPolicy     = "RUN";
String        oobRejectionPolicy         = "RUN";
Integer       oobThreadPoolMaxThreads    = 25;
Integer       oobThreadPoolMinThreads    = 2;
Integer       oobThreadPoolKeepAliveTime = 5000;

defaultThreads = Threads.lookup( Empyrean.class, Hosts.class );
normalThreads = Threads.lookup( Empyrean.class, Hosts.class, "normal-pool" );
oobThreads = Threads.lookup( Empyrean.class, Hosts.class, "oob-pool" );

def udpTransport = {
  UDP udp = new UDP( );
  try {
    LOG.info( "Setting membership addres: " + Internets.localHostAddress( ) );
    udp.setBindAddress( Internets.localHostInetAddress( ) );
    udp.setBindPort( 8773 );
    udp.setBindToAllInterfaces( false );//this sets receive_on_all_interfaces
  } catch ( UnknownHostException ex ) {
    LOG.error( ex, ex );
  }
  udp.setMulticastAddress( multicastInetAddress );
  udp.setMulticastPort( multicastPort );
  udp.setDiscardIncompatiblePackets( true );
  udp.setLogDiscardMessages( false );
  udp.setMaxBundleSize( 64000 );
  udp.setMaxBundleTimeout( 30 );
  udp
}

def udpDiscovery = {
  PING pingDiscovery = new PING( );
  pingDiscovery.setTimeout( 2000 );
  pingDiscovery.setNumInitialMembers( 2 );
  
  pingDiscovery
}

def tcpTransport = {
  TCP tcp = new TCP()
  tcp.setBindAddress( Internets.localHostInetAddress( ) )
  tcp.setBindPort( tcpPortBase )
  tcp.setReaperInterval( 30000 )
  tcp.setPortRange( tcpPortRange )//go from 8776-9000
  tcp.setMaxBundleSize( 64000 )
  tcp
}

def tcpDiscovery = {
  TCPGOSSIP tcpGossip = new TCPGOSSIP( );
  tcpGossip.setTimeout( 10000 )
  tcpGossip.setNumInitialMembers( 2 )
  initialHosts = Sets.newHashSet( BootstrapArgs.parseBootstrapHosts( ) ).collect{ new InetSocketAddress( it, gossipPort ) }
  LOG.info( "TCPGOSSIP: ${initialHosts}" )
  tcpGossip.setInitialHosts( initialHosts )
  tcpGossip
}

def gossipRouter = {
  GossipRouter router = new GossipRouter(gossipPort, gossipBindAddr,true)
  OrderedShutdown.registerPreShutdownHook{
    if(router.isRunning( )) {
      router.stop( );
    }
  }
  LOG.info( "GossipRouter starting on: ${gossipBindAddr}:${gossipPort}" )
  try {
    router.start( );
    LOG.info( "GossipRouter started on:  ${gossipBindAddr}:${gossipPort} (see jmx object jgroups:name=GossipRouter)" )
  } catch( Exception e ) {
    LOG.error( "GossipRouter failed to start: ${e.getMessage()}", e );
  }
  router
}

def transportSupplier = udpTransport
def discoverySupplier = udpDiscovery

if ( !BootstrapArgs.parseBootstrapHosts( ).isEmpty( ) ) {
  gossipRouter()
  transportSupplier = tcpTransport
  discoverySupplier = tcpDiscovery
}

TP transport = transportSupplier()
transport.setValue( "singleton_name", SystemIds.membershipUdpMcastTransportName( ) );
//transport.setDefaultThreadPool( defaultThreads );
transport.setDefaultThreadPoolThreadFactory( defaultThreads );
//transport thread factories
transport.setThreadFactory( normalThreads );
transport.setThreadPoolMaxThreads( threadPoolMaxThreads );
transport.setThreadPoolKeepAliveTime( threadPoolKeepAliveTime );
transport.setThreadPoolMinThreads( threadPoolMinThreads );
transport.setThreadPoolQueueEnabled( threadPoolQueueEnabled );
transport.setRegularRejectionPolicy( regularRejectionPolicy );
//transport OOB thread factories
transport.setOOBThreadPoolThreadFactory( oobThreads );
//transport.setOOBThreadPool( oobThreads );
transport.setOOBThreadPoolMaxThreads( oobThreadPoolMaxThreads );
transport.setOOBThreadPoolKeepAliveTime( oobThreadPoolKeepAliveTime );
transport.setOOBThreadPoolMinThreads( oobThreadPoolMinThreads );
transport.setOOBRejectionPolicy( oobRejectionPolicy );

Discovery discovery = discoverySupplier()

MERGE2 mergeHandler = new MERGE2( );
mergeHandler.setMaxInterval( 30000 );
mergeHandler.setMinInterval( 10000 );

FD_SOCK fdSocket = new FD_SOCK();
fdSocket.setValue("bind_addr", Internets.localHostInetAddress( ) )
fdSocket.setValue("start_port", tcpFailurePortBase )
fdSocket.setValue("port_range", tcpFailurePortRange )

NAKACK negackBroadcast = new NAKACK( );
negackBroadcast.setUseMcastXmit( false );
negackBroadcast.setDiscardDeliveredMsgs( true );

UNICAST reliableUnicast = new UNICAST( );

STABLE stableBroadcast = new STABLE( );
//stableBroadcast.setDesiredAverageGossip( 20000 );
stableBroadcast.setMaxBytes( 400000 );

GMS groupMembership = new GMS( );
//if( !BootstrapArgs.isCloudController( ) ) {
//  groupMembership.setValue( "disable_initial_coord", true );
//}
groupMembership.setPrintLocalAddress( true );
groupMembership.setJoinTimeout( 3000 );
groupMembership.setViewBundling( true );

FC flowControl = new FC( );
flowControl.setMaxCredits( 20000000 );
flowControl.setMinThreshold( 0.1 );

return [
  transport,
  discovery,
  mergeHandler,
  fdSocket,
  new FD(),
  new VERIFY_SUSPECT(),
  negackBroadcast,
  new UNICAST(),
  stableBroadcast,
  groupMembership,
  flowControl,
  new FRAG2( fragSize: 60000 ),
  new STATE_TRANSFER()
];

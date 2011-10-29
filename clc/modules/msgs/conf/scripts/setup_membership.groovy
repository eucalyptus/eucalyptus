import org.jgroups.protocols.FD
import org.jgroups.protocols.FD_SOCK
import org.jgroups.protocols.UNICAST
import org.jgroups.protocols.VERIFY_SUSPECT

import org.jgroups.protocols.FD
import org.jgroups.protocols.FD_SOCK
import java.net.InetAddress
import java.net.UnknownHostException
import org.apache.log4j.Logger
import org.jgroups.protocols.FC
import org.jgroups.protocols.FD
import org.jgroups.protocols.FD_SOCK
import org.jgroups.protocols.FRAG2
import org.jgroups.protocols.MERGE2
import org.jgroups.protocols.PING
import org.jgroups.protocols.UDP
import org.jgroups.protocols.UNICAST
import org.jgroups.protocols.VERIFY_SUSPECT
import org.jgroups.protocols.pbcast.GMS
import org.jgroups.protocols.pbcast.NAKACK
import org.jgroups.protocols.pbcast.STABLE
import org.jgroups.protocols.pbcast.STATE_TRANSFER
import com.eucalyptus.bootstrap.BootstrapArgs
import com.eucalyptus.bootstrap.Hosts
import com.eucalyptus.bootstrap.SystemIds
import com.eucalyptus.empyrean.Empyrean
import com.eucalyptus.system.Threads
import com.eucalyptus.util.Internets

Logger LOG = Logger.getLogger( "setup_membership" );

String        multicastAddress           = "228.7.7.3";
InetAddress   multicastInetAddress       = InetAddress.getByName( multicastAddress );
Integer       multicastPort              = 8773;
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


UDP udp = new UDP( );
try {
  LOG.info( "Setting membership addres: " + Internets.localHostAddress( ) );
  udp.setBindAddress( Internets.localHostAddress( ) );
  udp.setBindPort( 8773 );
  udp.setBindToAllInterfaces( false );//this sets receive_on_all_interfaces
} catch ( UnknownHostException ex ) {
  LOG.error( ex, ex );
}
udp.setMulticastAddress( multicastInetAddress );
udp.setMulticastPort( multicastPort );
udp.setDiscardIncompatiblePackets( true );
udp.setLogDiscardMessages( false );
udp.setMaxBundleSize( 60000 );
udp.setMaxBundleTimeout( 30 );
udp.setValue( "singleton_name", SystemIds.membershipUdpMcastTransportName( ) );

//udp.setDefaultThreadPool( defaultThreads );
udp.setDefaultThreadPoolThreadFactory( defaultThreads );

udp.setThreadFactory( normalThreads );
udp.setThreadPoolMaxThreads( threadPoolMaxThreads );
udp.setThreadPoolKeepAliveTime( threadPoolKeepAliveTime );
udp.setThreadPoolMinThreads( threadPoolMinThreads );
udp.setThreadPoolQueueEnabled( threadPoolQueueEnabled );
udp.setRegularRejectionPolicy( regularRejectionPolicy );

udp.setOOBThreadPoolThreadFactory( oobThreads );
//udp.setOOBThreadPool( oobThreads );
udp.setOOBThreadPoolMaxThreads( oobThreadPoolMaxThreads );
udp.setOOBThreadPoolKeepAliveTime( oobThreadPoolKeepAliveTime );
udp.setOOBThreadPoolMinThreads( oobThreadPoolMinThreads );
udp.setOOBRejectionPolicy( oobRejectionPolicy );

PING pingDiscovery = new PING( );
pingDiscovery.setTimeout( 2000 );
pingDiscovery.setNumInitialMembers( 2 );

MERGE2 mergeHandler = new MERGE2( );
mergeHandler.setMaxInterval( 30000 );
mergeHandler.setMinInterval( 10000 );

FD_SOCK fdSocket = new FD_SOCK();
fdSocket.setValue("bind_addr", Internets.localHostInetAddress( ) )

NAKACK negackBroadcast = new NAKACK( );
negackBroadcast.setUseMcastXmit( false );
negackBroadcast.setDiscardDeliveredMsgs( true );
negackBroadcast.setGcLag( 20 );

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
groupMembership.setShun( false );
groupMembership.setViewBundling( true );

FC flowControl = new FC( );
flowControl.setMaxCredits( 20000000 );
flowControl.setMinThreshold( 0.1 );

return [
  udp,
  pingDiscovery,
  mergeHandler,
  fdSocket,
  new FD(),
  new VERIFY_SUSPECT(),
  negackBroadcast, 
  new UNICAST(),
  stableBroadcast,
  groupMembership,
  flowControl,
  new FRAG2( ),
  new STATE_TRANSFER()
];

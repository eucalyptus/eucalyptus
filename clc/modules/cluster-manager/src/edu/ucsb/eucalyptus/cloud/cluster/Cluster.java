package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.ClusterInfo;
import edu.ucsb.eucalyptus.cloud.net.AddressUpdateCallback;
import edu.ucsb.eucalyptus.keys.*;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.transport.Axis2MessageDispatcher;
import edu.ucsb.eucalyptus.transport.util.Defaults;
import edu.ucsb.eucalyptus.constants.HasName;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import java.util.concurrent.*;

public class Cluster implements HasName {

  private static Logger LOG = Logger.getLogger( Cluster.class );

  private ClusterInfo clusterInfo;
  private ConcurrentNavigableMap<String, NodeInfo> nodeMap;
  private ClusterState state;
  private ClusterMessageQueue messageQueue;
  private ResourceUpdateCallback rscUpdater;
  private AddressUpdateCallback addrUpdater;
  private VmUpdateCallback vmUpdater;
  private NodeLogCallback nodeLogUpdater;
  private NodeCertCallback nodeCertUpdater;
  private Thread rscThread;
  private Thread vmThread;
  private Thread addrThread;
  private Thread mqThread;
  private Thread logThread;
  private Thread keyThread;

  public Cluster( ClusterInfo clusterInfo ) {
    this.clusterInfo = clusterInfo;
    this.state = new ClusterState( this );
    this.messageQueue = new ClusterMessageQueue( this );
    this.rscUpdater = new ResourceUpdateCallback( this );
    this.addrUpdater = new AddressUpdateCallback( this );
    this.vmUpdater = new VmUpdateCallback( this );
    this.nodeLogUpdater = new NodeLogCallback( this );
    this.nodeCertUpdater = new NodeCertCallback( this );
    this.nodeMap = new ConcurrentSkipListMap<String, NodeInfo>();
    this.waitForCerts();
    this.start();
  }

  private void waitForCerts() {
    Axis2MessageDispatcher dispatcher = Defaults.getMessageDispatcher( Defaults.getInsecureOutboundEndpoint( clusterInfo.getInsecureUri(), ClusterInfo.NAMESPACE, 15, 1, 1 ) );
    GetKeysResponseType reply = null;
    do {
      try {
        reply = ( GetKeysResponseType ) dispatcher.getClient().send( new GetKeysType( "self" ) );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
      try {Thread.sleep( 5000 );} catch ( InterruptedException ignored ) {}
    } while ( reply == null || !this.checkCerts( reply ) );
  }

  private boolean checkCerts( final GetKeysResponseType reply ) {
    NodeCertInfo certs = reply.getCerts();
    if ( certs == null ) return false;
    String ccCert = new String( Base64.decode( certs.getCcCert() ) );
    String ncCert = new String( Base64.decode( certs.getNcCert() ) );
    boolean ret = true;
    LOG.info( "===============================================================" );
    LOG.info( " Trying to verify the certificates for " + this.getClusterInfo().getName() );
    LOG.info( "---------------------------------------------------------------" );
    try {
      X509Certificate x509 = AbstractKeyStore.pemToX509( ccCert );
      String alias = ServiceKeyStore.getInstance().getCertificateAlias( x509 );
      LOG.info( "FOUND: alias " + alias );
    }
    catch ( GeneralSecurityException e ) {
      LOG.error( e );
      ret = false;
    }
    LOG.info( "---------------------------------------------------------------" );
    try {
      String alias = ServiceKeyStore.getInstance().getCertificateAlias( ncCert );
      LOG.info( "FOUND: alias " + alias );
    }
    catch ( GeneralSecurityException e ) {
      ret = false;
      LOG.error( e );
    }
    LOG.info( "===============================================================" );
    return ret;
  }

  public synchronized void start() {
    //:: should really be organized as a thread group etc etc :://
    LOG.info( "Starting cluster: " + this.clusterInfo.getUri() );
    if ( this.mqThread == null || this.mqThread.isAlive() )
      this.mqThread = this.startNamedThread( messageQueue );

    if ( this.rscThread == null || this.rscThread.isAlive() )
      this.rscThread = this.startNamedThread( rscUpdater );

    if ( this.vmThread == null || this.vmThread.isAlive() )
      this.vmThread = this.startNamedThread( vmUpdater );

    if ( this.addrThread == null || this.addrThread.isAlive() )
      this.addrThread = this.startNamedThread( addrUpdater );
    this.fireNodeThreads();
  }

  private Thread startNamedThread( Runnable r ) {
    Thread t = new Thread( r );
    t.setName( String.format( "%s-%s@%X", r.getClass().getSimpleName(), this.getName(), t.hashCode() ) );
    t.start();
    LOG.info( "Starting threads for [ " + this.getName() + " ] " + t.getName() );
    return t;
  }

  public void fireNodeThreads() {
//    if ( this.keyThread != null && !this.keyThread.isAlive() )
//      ( this.keyThread = new Thread( nodeCertUpdater, nodeCertUpdater.getClass().getSimpleName() + "-" + this.getName() ) ).start();
//    if ( this.logThread != null && !this.logThread.isAlive() )
//      ( this.logThread = new Thread( nodeLogUpdater, nodeLogUpdater.getClass().getSimpleName() + "-" + this.getName() ) ).start();
  }

  public void stop() throws InterruptedException {
    LOG.info( "Stopping cluster: " + this.clusterInfo.getUri() );
    this.rscUpdater.stop();
    this.addrUpdater.stop();
    this.vmUpdater.stop();
    this.nodeLogUpdater.stop();
    this.nodeCertUpdater.stop();
    this.messageQueue.stop();
    this.rscThread.join();
    this.vmThread.join();
    this.addrThread.join();
    this.keyThread.join();
    this.logThread.join();
    this.mqThread.join();
  }

  public ClusterInfoType getInfo() {
    return new ClusterInfoType( this.clusterInfo.getName(), this.clusterInfo.getHost() );
  }

  public ClusterState getState() {
    return state;
  }

  public ClusterMessageQueue getMessageQueue() {
    return messageQueue;
  }

  public int compareTo( final Object o ) {
    Cluster that = ( Cluster ) o;
    return this.getName().compareTo( that.getName() );
  }

  public void updateNodeInfo( List<String> nodeTags ) {
    NodeInfo ret = null;
    for ( String tag : nodeTags )
      if ( ( ret = this.nodeMap.putIfAbsent( tag, new NodeInfo( tag ) ) ) != null )
        ret.touch();
  }

  public void updateNodeCerts( NavigableSet<NodeCertInfo> nodeCerts ) {
    NodeInfo ret = null;
    for ( NodeCertInfo cert : nodeCerts ) {
      NodeInfo newNodeInfo = new NodeInfo( cert );
      if ( ( ret = this.nodeMap.putIfAbsent( cert.getServiceTag(), newNodeInfo ) ) != null )
        ret.setCerts( cert );
    }
  }

  public void updateNodeLogs( NavigableSet<NodeLogInfo> nodeCerts ) {
    NodeInfo ret = null;
    for ( NodeLogInfo log : nodeCerts )
      if ( ( ret = this.nodeMap.putIfAbsent( log.getServiceTag(), new NodeInfo( log ) ) ) != null )
        ret.setLogs( log );
  }

  public NavigableSet<String> getNodeTags() {
    return this.nodeMap.navigableKeySet();
  }

  public NavigableSet<NodeInfo> getNodes() {
    return new ConcurrentSkipListSet<NodeInfo>( this.nodeMap.values() );
  }

  public NodeInfo getNode( String serviceTag ) {
    return this.nodeMap.get( serviceTag );
  }

  public ClusterStateType getWeb() {
    String host = this.getClusterInfo().getUri();
    int port = 0;
    try {
      URI uri = new URI( this.getClusterInfo().getUri() );
      host = uri.getHost();
      port = uri.getPort();
    }
    catch ( URISyntaxException e ) {}
    return new ClusterStateType( this.getName(), host, port );
  }

  public ClusterInfo getClusterInfo() {
    return clusterInfo;
  }

  public String getName() {
    return this.clusterInfo.getName();
  }

  @Override
  public String toString() {
    return "Cluster{" +
           "clusterInfo=" + clusterInfo +
           "\n" + this.getName() + ".state=" + state +
           "\n" + this.getName() + ".messageQueue=" + messageQueue +
           "\n" + this.getName() + ".nodeMap=" + nodeMap +
           '}';
  }
}

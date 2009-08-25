package com.eucalyptus.cluster;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.config.ClusterConfiguration;

import edu.ucsb.eucalyptus.cloud.cluster.NodeCertCallback;
import edu.ucsb.eucalyptus.cloud.cluster.NodeLogCallback;
import edu.ucsb.eucalyptus.cloud.cluster.ResourceUpdateCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmUpdateCallback;
import edu.ucsb.eucalyptus.cloud.net.AddressUpdateCallback;
import edu.ucsb.eucalyptus.msgs.GetKeysResponseType;
import edu.ucsb.eucalyptus.msgs.NodeCertInfo;

public class ClusterThreadGroup extends ThreadGroup {
  private static Logger          LOG = Logger.getLogger( ClusterThreadGroup.class );
  private ClusterConfiguration   configuration;

  private Thread                 rscThread;
  private Thread                 vmThread;
  private Thread                 addrThread;
  private Thread                 mqThread;
  private Thread                 logThread;
  private Thread                 keyThread;

  private ClusterMessageQueue    messageQueue;
  private ResourceUpdateCallback rscUpdater;
  private AddressUpdateCallback  addrUpdater;
  private VmUpdateCallback       vmUpdater;
  private NodeLogCallback        nodeLogUpdater;
  private NodeCertCallback       nodeCertUpdater;

  public ClusterThreadGroup( String clusterName, ClusterConfiguration clusterConfig ) {
    super( clusterName );
    this.configuration = clusterConfig;
    this.messageQueue = new ClusterMessageQueue( this.configuration );
    this.rscUpdater = new ResourceUpdateCallback( this.configuration );
    this.addrUpdater = new AddressUpdateCallback( this.configuration );
    this.vmUpdater = new VmUpdateCallback( this.configuration );
    this.nodeLogUpdater = new NodeLogCallback( this.configuration );
    this.nodeCertUpdater = new NodeCertCallback( this.configuration );
    this.init( );
  }

  private void init( ) {
    LOG.warn( "Starting cluster: " + this.configuration.getName( ) );
    this.waitForCerts( );
    if ( this.mqThread == null || this.mqThread.isAlive( ) ) this.mqThread = this.createNamedThread( messageQueue );
    if ( this.rscThread == null || this.rscThread.isAlive( ) ) this.rscThread = this.createNamedThread( rscUpdater );
    if ( this.vmThread == null || this.vmThread.isAlive( ) ) this.vmThread = this.createNamedThread( vmUpdater );
    if ( this.addrThread == null || this.addrThread.isAlive( ) ) this.addrThread = this.createNamedThread( addrUpdater );
    if ( this.keyThread == null || this.keyThread.isAlive( ) ) this.keyThread = this.createNamedThread( nodeCertUpdater );// ,
    // nodeCertUpdater.getClass().getSimpleName()
    // +
    // "-"
    // +
    // this.getName()
    // )
    // ).start();
    // if ( this.logThread != null && !this.logThread.isAlive() )
    // ( this.logThread = new Thread( nodeLogUpdater,
    // nodeLogUpdater.getClass().getSimpleName() + "-" + this.getName() )
    // ).start();
  }

  private Thread createNamedThread( Runnable r ) {
    Thread t = new Thread( this, r );
    t.setName( String.format( "%s-%s@%X", r.getClass( ).getSimpleName( ), this.getName( ), t.hashCode( ) ) );
    LOG.warn( "-> Creating threads for [ " + this.getName( ) + " ] " + t.getName( ) );
    return t;
  }
  private void startNamedThread( Thread t ) {
    LOG.warn( "-> Starting threads for [ " + this.getName( ) + " ] " + t.getName( ) );
    t.start();    
  }
  
  public void startThreads() {
    this.startNamedThread( this.mqThread );
    this.startNamedThread( this.rscThread );
    this.startNamedThread( this.vmThread );
    this.startNamedThread( this.addrThread );
    this.startNamedThread( this.keyThread );
  }

  @Override
  public void uncaughtException( Thread t, Throwable e ) {
    LOG.error( "Caught exception from " + t.getName( ) + ": " + e.getClass( ) );
    LOG.error( t.getName( ) + ": " + e.getMessage( ), e );
    super.uncaughtException( t, e );
  }

  private void waitForCerts( ) {
    // Axis2MessageDispatcher dispatcher = Defaults.getMessageDispatcher(
    // Defaults.getInsecureOutboundEndpoint( clusterInfo.getInsecureUri(),
    // ClusterInfo.NAMESPACE, 15, 1, 1 ) );
    // GetKeysResponseType reply = null;
    // do {
    // try {
    // reply = ( GetKeysResponseType ) dispatcher.getClient().send( new
    // GetKeysType( "self" ) );
    // reachable = true;
    // } catch ( Exception e ) {
    // LOG.debug( e, e );
    // }
    // try {Thread.sleep( 5000 );} catch ( InterruptedException ignored ) {}
    // } while ( ( reply == null || !this.checkCerts( reply ) ) && !stopped );
  }

  private boolean checkCerts( final GetKeysResponseType reply ) {
    NodeCertInfo certs = reply.getCerts( );
    if ( certs == null ) return false;
    String ccCert = new String( Base64.decode( certs.getCcCert( ) ) );
    String ncCert = new String( Base64.decode( certs.getNcCert( ) ) );
    boolean ret = true;
    LOG.info( "===============================================================" );
    LOG.info( " Trying to verify the certificates for " + this.getName( ) );
    LOG.info( "---------------------------------------------------------------" );
    // try {
    // TODO: IMPORTANT fix me
    // X509Certificate x509 = AbstractKeyStore.pemToX509( ccCert );
    // String alias = ServiceKeyStore.getInstance().getCertificateAlias( x509 );
    // LOG.info( "FOUND: alias " + alias );
    // }
    // catch ( GeneralSecurityException e ) {
    // LOG.error( e );
    // ret = false;
    // }
    LOG.info( "---------------------------------------------------------------" );
    // try {
    // String alias = ServiceKeyStore.getInstance().getCertificateAlias( ncCert
    // );
    // LOG.info( "FOUND: alias " + alias );
    // }
    // catch ( GeneralSecurityException e ) {
    // ret = false;
    // LOG.error( e );
    // }
    LOG.info( "===============================================================" );
    return ret;
  }

  class ClusterStartupWatchdog extends Thread {
    ClusterThreadGroup cluster = null;

    ClusterStartupWatchdog( final ClusterThreadGroup cluster ) {
      super( cluster.getName( ) + "-ClusterStartupWatchdog" );
      this.cluster = cluster;
    }

    public void run( ) {
      LOG.info( "Calling startup on cluster: " + cluster.getName( ) );
      cluster.init( );
    }
  }

  public void start( ) {
    ( new ClusterStartupWatchdog( this ) ).start( );
  }

  public ClusterMessageQueue getMessageQueue( ) {
    return messageQueue;
  }
  
}

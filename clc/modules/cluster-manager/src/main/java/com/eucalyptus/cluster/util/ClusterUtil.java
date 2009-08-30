package com.eucalyptus.cluster.util;

import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterThreadGroup;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.cluster.event.InitializeClusterEvent;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.cluster.handlers.AbstractClusterMessageDispatcher;
import com.eucalyptus.cluster.handlers.ClusterCertificateHandler;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.ws.BindingException;

import edu.ucsb.eucalyptus.msgs.GetKeysResponseType;
import edu.ucsb.eucalyptus.msgs.NodeCertInfo;

public class ClusterUtil {
  private static Logger LOG = Logger.getLogger( ClusterUtil.class );
  public static boolean checkCerts( final GetKeysResponseType reply, final Cluster cluster ) {
    NodeCertInfo certs = reply.getCerts( );
    if ( certs == null || certs.getCcCert( ) == null || certs.getNcCert( ) == null ) { return false; }
    LOG.info( "---------------------------------------------------------------" );
    byte[] ccCert = Base64.decode( certs.getCcCert( ) );
    X509Certificate clusterx509 = Hashes.getPemCert( ccCert );
    X509Certificate realClusterx509 = X509Cert.toCertificate( cluster.getCredentials( ).getClusterCertificate( ) );
    boolean cc = realClusterx509.equals( clusterx509 );
    LOG.info( "-> [ " + cluster.getName( ) + " ] Cluster certificate valid=" + cc );
    byte[] ncCert = Base64.decode( certs.getNcCert( ) );
    X509Certificate nodex509 = Hashes.getPemCert( ncCert );
    X509Certificate realNodex509 = X509Cert.toCertificate( cluster.getCredentials( ).getNodeCertificate( ) );
    boolean nc = realNodex509.equals( nodex509 );
    LOG.info( "-> [ " + cluster.getName( ) + " ] Node certificate valid=" + nc );
    LOG.info( "---------------------------------------------------------------" );
    if( !cc ) {
      LOG.debug( LogUtil.subheader( "EXPECTED CERTIFICATE" ) + X509Cert.toCertificate( cluster.getCredentials( ).getClusterCertificate( ) ) );
      LOG.debug( LogUtil.subheader( "RECEIVED CERTIFICATE" ) + clusterx509 );
    }
    if( !nc ) {
      LOG.debug( LogUtil.subheader( "EXPECTED CERTIFICATE" ) + X509Cert.toCertificate( cluster.getCredentials( ).getNodeCertificate( ) ) );
      LOG.debug( LogUtil.subheader( "RECEIVED CERTIFICATE" ) + nodex509 );
    }
    return cc && nc;
  }
  public static Cluster createCluster( ClusterConfiguration c ) throws EucalyptusCloudException {
    ClusterCredentials credentials = null;
    EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
    try {
      credentials = credDb.getUnique( new ClusterCredentials( c.getName( ) ) );
    } catch ( EucalyptusCloudException e ) {
      LOG.error( "Failed to load credentials for cluster: " + c.getName( ) );
      throw e;
    } finally {
      credDb.rollback( );
    }
    Cluster newCluster = new Cluster( c, credentials );
    Clusters.getInstance( ).register( newCluster );
    return newCluster;
  }

  public static void registerClusterStateHandler( Cluster newCluster, AbstractClusterMessageDispatcher dispatcher ) throws EventVetoedException {
    ListenerRegistry.getInstance( ).register( InitializeClusterEvent.class, dispatcher );
    ListenerRegistry.getInstance( ).register( TeardownClusterEvent.class, dispatcher );
    ListenerRegistry.getInstance( ).register( ClockTick.class, dispatcher );
  }
  public static void deregisterClusterStateHandler( Cluster removeCluster, AbstractClusterMessageDispatcher dispatcher ) throws EventVetoedException {
    ListenerRegistry.getInstance( ).deregister( InitializeClusterEvent.class, dispatcher );
    ListenerRegistry.getInstance( ).deregister( TeardownClusterEvent.class, dispatcher );
    ListenerRegistry.getInstance( ).deregister( ClockTick.class, dispatcher );
  }
  
}

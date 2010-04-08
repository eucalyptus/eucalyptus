package com.eucalyptus.cluster.util;

import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.util.B64;
import com.eucalyptus.auth.util.PEMFiles;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.event.InitializeClusterEvent;
import com.eucalyptus.cluster.event.TeardownClusterEvent;
import com.eucalyptus.cluster.handlers.AbstractClusterMessageDispatcher;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.LogUtil;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import edu.ucsb.eucalyptus.msgs.GetKeysResponseType;
import edu.ucsb.eucalyptus.msgs.NodeCertInfo;

public class ClusterUtil {
  private static Logger LOG = Logger.getLogger( ClusterUtil.class );
  public static boolean checkCerts( final GetKeysResponseType reply, final Cluster cluster ) {
    NodeCertInfo certs = reply.getCerts( );
    if ( certs == null || certs.getCcCert( ) == null || certs.getNcCert( ) == null ) { return false; }

    X509Certificate realClusterx509 = X509Cert.toCertificate( cluster.getCredentials( ).getClusterCertificate( ) );
    X509Certificate realNodex509 = X509Cert.toCertificate( cluster.getCredentials( ).getNodeCertificate( ) );

    X509Certificate clusterx509 = PEMFiles.getCert( B64.dec( certs.getCcCert( ) ) );
    X509Certificate nodex509 = PEMFiles.getCert( B64.dec( certs.getNcCert( ) ) );
    
    Boolean cc = realClusterx509.equals( clusterx509 );
    Boolean nc = realNodex509.equals( nodex509 );

    LOG.info( EventRecord.here( Component.cluster, EventType.CLUSTER_CERT, cluster.getName(), "cc", cc.toString( ), "nc", nc.toString( ) ) );
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

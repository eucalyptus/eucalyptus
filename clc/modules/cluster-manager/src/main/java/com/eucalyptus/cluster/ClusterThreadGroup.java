/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.cluster;

import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Hashes;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.NioClient;
import com.eucalyptus.ws.client.pipeline.LogClientPipeline;
import com.eucalyptus.ws.handlers.NioResponseHandler;

import edu.ucsb.eucalyptus.cloud.cluster.NodeCertCallback;
import edu.ucsb.eucalyptus.cloud.cluster.NodeLogCallback;
import edu.ucsb.eucalyptus.cloud.cluster.ResourceUpdateCallback;
import edu.ucsb.eucalyptus.cloud.cluster.VmUpdateCallback;
import edu.ucsb.eucalyptus.cloud.net.AddressUpdateCallback;
import edu.ucsb.eucalyptus.msgs.GetKeysResponseType;
import edu.ucsb.eucalyptus.msgs.GetKeysType;
import edu.ucsb.eucalyptus.msgs.NodeCertInfo;

public class ClusterThreadGroup extends ThreadGroup {
  private static Logger          LOG            = Logger.getLogger( ClusterThreadGroup.class );
  private ClusterConfiguration   configuration;

  private Thread                 rscThread;
  private Thread                 vmThread;
  private Thread                 addrThread;
  private Thread                 mqThread;
//  private Thread                 logThread;
  private Thread                 keyThread;

  private ClusterMessageQueue    messageQueue;
  private ResourceUpdateCallback rscUpdater;
  private AddressUpdateCallback  addrUpdater;
  private VmUpdateCallback       vmUpdater;
//  private NodeLogCallback        nodeLogUpdater;
  private NodeCertCallback       nodeCertUpdater;
  private ClusterCredentials     credentials;
  private X509Certificate        clusterCert;
  private X509Certificate        nodeCert;
  private ClusterStartupWatchdog watchdog;

  private boolean                stopped        = false;
  private boolean                certsConfirmed = false;

  public ClusterThreadGroup( ClusterConfiguration clusterConfig, ClusterCredentials credentials ) {
    super( clusterConfig.getName( ) );
    this.credentials = credentials;
    this.configuration = clusterConfig;
    this.messageQueue = new ClusterMessageQueue( this.configuration );
    this.rscUpdater = new ResourceUpdateCallback( this.configuration );
    this.addrUpdater = new AddressUpdateCallback( this.configuration );
    this.vmUpdater = new VmUpdateCallback( this.configuration );
//    this.nodeLogUpdater = new NodeLogCallback( this.configuration );
    this.nodeCertUpdater = new NodeCertCallback( this.configuration );
    this.clusterCert = X509Cert.toCertificate( credentials.getClusterCertificate( ) );
    this.nodeCert = X509Cert.toCertificate( credentials.getNodeCertificate( ) );
    this.init( );
  }

  private void init( ) {
    LOG.warn( "Starting cluster: " + this.configuration.getName( ) );
    if ( this.mqThread == null || !this.mqThread.isAlive( ) ) this.mqThread = this.createNamedThread( messageQueue );
    if ( this.rscThread == null || !this.rscThread.isAlive( ) ) this.rscThread = this.createNamedThread( rscUpdater );
    if ( this.vmThread == null || !this.vmThread.isAlive( ) ) this.vmThread = this.createNamedThread( vmUpdater );
    if ( this.addrThread == null || !this.addrThread.isAlive( ) ) this.addrThread = this.createNamedThread( addrUpdater );
    if ( this.keyThread == null || !this.keyThread.isAlive( ) ) this.keyThread = this.createNamedThread( nodeCertUpdater );
  }

  private Thread createNamedThread( Runnable r ) {
    Thread t = new Thread( this, r );
    t.setName( String.format( "%s-%s@%X", r.getClass( ).getSimpleName( ), this.getName( ), t.hashCode( ) ) );
    LOG.warn( "-> [ " + this.getName( ) + " ] Creating threads " + t.getName( ) );
    return t;
  }

  private void startNamedThread( Thread t ) {
    if ( !t.isAlive( ) && !this.stopped ) {
      LOG.warn( "-> [ " + this.getName( ) + " ] Starting threads " + t.getName( ) );
      t.start( );
    }
  }

  private void startThreads( ) {
    if ( !certsConfirmed ) {
      this.waitForCerts( );
    }
    this.startNamedThread( this.mqThread );
    this.startNamedThread( this.rscThread );
    this.startNamedThread( this.vmThread );
    this.startNamedThread( this.addrThread );
    this.startNamedThread( this.keyThread );
  }

  public void stopThreads( ) {
    this.stopped = true;
    this.messageQueue.stop( );
    LOG.warn( "-> [ " + this.getName( ) + " ] Stopping threads " + this.mqThread );
    this.rscUpdater.stop( );
    LOG.warn( "-> [ " + this.getName( ) + " ] Stopping threads " + this.rscThread );
    this.vmUpdater.stop( );
    LOG.warn( "-> [ " + this.getName( ) + " ] Stopping threads " + this.vmThread );
    this.addrUpdater.stop( );
    LOG.warn( "-> [ " + this.getName( ) + " ] Stopping threads " + this.addrThread );
    this.nodeCertUpdater.stop( );
    LOG.warn( "-> [ " + this.getName( ) + " ] Stopping threads " + this.keyThread );
  }

  @Override
  public void uncaughtException( Thread t, Throwable e ) {
    LOG.error( "Caught exception from " + t.getName( ) + ": " + e.getClass( ) );
    LOG.error( t.getName( ) + ": " + e.getMessage( ), e );
    super.uncaughtException( t, e );
  }

  private void waitForCerts( ) {
    GetKeysResponseType reply = null;
    do {
      try {
        NioClient client = new NioClient( this.configuration.getHostName( ), this.configuration.getPort( ), this.configuration.getInsecureServicePath( ), new LogClientPipeline( new NioResponseHandler( ) ) );
        reply = ( GetKeysResponseType ) client.send( new GetKeysType( "self" ) );
        this.certsConfirmed = this.checkCerts( reply );
        if( this.certsConfirmed ) break;
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
      try {
        Thread.sleep( 5000 );
      } catch ( InterruptedException ignored ) {
      }
    } while ( !this.certsConfirmed && !this.stopped );
  }

  private boolean checkCerts( final GetKeysResponseType reply ) {
    NodeCertInfo certs = reply.getCerts( );
    if ( certs == null || certs.getCcCert( ) == null || certs.getNcCert( ) == null ) { return false; }
    LOG.info( "---------------------------------------------------------------" );
    byte[] ccCert = Base64.decode( certs.getCcCert( ) );
    X509Certificate clusterx509 = Hashes.getPemCert( ccCert );
    boolean cc = this.clusterCert.equals( clusterx509 );
    LOG.info( "-> [ " + this.getName( ) + " ] Cluster certificate valid=" + cc );
    byte[] ncCert = Base64.decode( certs.getNcCert( ) );
    X509Certificate nodex509 = Hashes.getPemCert( ncCert );
    boolean nc = this.nodeCert.equals( nodex509 );
    LOG.info( "-> [ " + this.getName( ) + " ] Node certificate valid=" + nc );
    LOG.info( "---------------------------------------------------------------" );
    return cc && nc;
  }

  class ClusterStartupWatchdog extends Thread {
    ClusterThreadGroup cluster = null;

    ClusterStartupWatchdog( final ClusterThreadGroup cluster ) {
      super( cluster, cluster.getName( ) + "-ClusterStartupWatchdog" );
      this.cluster = cluster;
    }

    public void run( ) {
      LOG.info( "Calling startup on cluster: " + cluster.getName( ) );
      cluster.startThreads( );
    }
  }

  public void create( ) {
    if ( this.watchdog == null ) {
      this.watchdog = new ClusterStartupWatchdog( this );
      this.watchdog.start( );
    }
  }

  public ClusterMessageQueue getMessageQueue( ) {
    return messageQueue;
  }

}

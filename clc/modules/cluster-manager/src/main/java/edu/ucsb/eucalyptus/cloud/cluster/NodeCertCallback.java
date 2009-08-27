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
package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.*;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterMessageQueue;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;

public class NodeCertCallback extends QueuedEventCallback<GetKeysType> implements Runnable {

  private static Logger LOG = Logger.getLogger( NodeCertCallback.class );

  private static int SLEEP_TIMER = 30 * 1000;
  private NavigableSet<GetKeysType> requests = null;
  private NavigableSet<NodeCertInfo> results = null;
  
  
  public NodeCertCallback( ClusterConfiguration config ) {
    super( config );
    this.requests = new ConcurrentSkipListSet<GetKeysType>();
    this.results = new ConcurrentSkipListSet<NodeCertInfo>();
  }

  public void process( final Client cluster, final GetKeysType msg ) throws Exception {
    try {
      GetKeysResponseType reply = ( GetKeysResponseType ) cluster.send( msg );
      NodeCertInfo certInfo = reply.getCerts();
      if ( certInfo != null ) {
        certInfo.setServiceTag( certInfo.getServiceTag().replaceAll( "EucalyptusGL", "EucalyptusNC" ) );
        if ( certInfo.getCcCert() != null && certInfo.getCcCert().length() > 0 ) {
          certInfo.setCcCert( new String( Base64.decode( certInfo.getCcCert() ) ) );
        }
        if ( certInfo.getNcCert() != null && certInfo.getNcCert().length() > 0 ) {
          certInfo.setNcCert( new String( Base64.decode( certInfo.getNcCert() ) ) );
        }
        results.add( certInfo );
      }
      requests.remove( msg );
    }
    catch ( Exception e ) {
      LOG.error( e );
    }
  }

  @Override
  public void notifyHandler() {
    if ( requests.isEmpty() ) {
      super.notifyHandler();
    }
  }

  public void run() {
    do {
      Cluster cluster = Clusters.getInstance( ).lookup( this.getConfig( ).getName( ) );
      if ( !cluster.getNodeTags().isEmpty() ) {
        LOG.debug( "Querying all known service tags:" );
        for ( String serviceTag : cluster.getNodeTags() ) {
          LOG.debug( "- " + serviceTag );
          GetKeysType msg = new GetKeysType( serviceTag.replaceAll( "EucalyptusNC", "EucalyptusGL" ) );
          this.requests.add( msg );
          cluster.getMessageQueue().enqueue( new QueuedLogEvent( this, msg ) );
        }
        this.waitForEvent();
        //TODO: FIXME
//        cluster.updateNodeCerts( results );
        this.results.clear();
      }
    } while ( !this.isStopped() && this.sleep( SLEEP_TIMER ) );
  }

}

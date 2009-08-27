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
package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.msgs.*;

import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.ws.client.Client;
import org.apache.log4j.Logger;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class NodeLogCallback extends QueuedEventCallback<GetLogsType> implements Runnable {

  private static Logger             LOG         = Logger.getLogger( NodeLogCallback.class );
  private static int                SLEEP_TIMER = 60 * 1000;
  private NavigableSet<NodeLogInfo> results     = null;
  private NavigableSet<GetLogsType> requests    = null;

  public NodeLogCallback( ClusterConfiguration config ) {
    super( config );
    this.results = new ConcurrentSkipListSet<NodeLogInfo>( );
    this.requests = new ConcurrentSkipListSet<GetLogsType>( );
  }

  public void process( final Client cluster, final GetLogsType msg ) throws Exception {
    // :: TODO-1.6: enable this again for testing :://
    // try
    // {
    // GetLogsResponseType reply = ( GetLogsResponseType ) cluster.send( msg );
    // NodeLogInfo logInfo = reply.getLogs();
    // logInfo.setServiceTag( logInfo.getServiceTag().replaceAll(
    // "EucalyptusGL", "EucalyptusNC" ) );
    // :: REMEMBER TO DO BASE64 DECODE HERE ::/
    // results.add( logInfo );
    requests.remove( msg );
    // }
    // catch ( AxisFault axisFault )
    // {
    // LOG.error( axisFault, axisFault );
    // }
  }

  @Override
  public void notifyHandler( ) {
    if ( requests.isEmpty( ) ) super.notifyHandler( );
  }

  public void run( ) {
//    do {
//      if ( !this.parent.getNodeTags( ).isEmpty( ) ) {
//        for ( String serviceTag : this.parent.getNodeTags( ) ) {
//          GetLogsType msg = new GetLogsType( serviceTag.replaceAll( "EucalyptusNC", "EucalyptusGL" ) );
//          this.requests.add( msg );
//          this.parent.getMessageQueue( ).enqueue( new QueuedLogEvent( this, msg ) );
//        }
//        this.waitForEvent( );
//        this.parent.updateNodeLogs( results );
//        this.results.clear( );
//      }
//    } while ( !this.isStopped( ) && this.sleep( SLEEP_TIMER ) );
  }

}

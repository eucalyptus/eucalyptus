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

import edu.ucsb.eucalyptus.util.*;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.ws.util.Messaging;

/**
 * User: decker
 * Date: Dec 11, 2008
 * Time: 11:28:09 AM
 */
public class ClusterEnvelope {

  private String clusterName;
  private QueuedEvent event;

  public static void dispatch( String name, QueuedEvent event ) {
    Messaging.dispatch( Component.clusters.getUri( ).toASCIIString( ), new ClusterEnvelope( name , event ) );
  }

  public ClusterEnvelope( final String clusterName, final QueuedEvent event ) {
    this.clusterName = clusterName;
    this.event = event;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName( final String clusterName ) {
    this.clusterName = clusterName;
  }

  public QueuedEvent getEvent() {
    return event;
  }

  public void setEvent( final QueuedEvent event ) {
    this.event = event;
  }
}

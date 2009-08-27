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
package edu.ucsb.eucalyptus.cloud;

import edu.ucsb.eucalyptus.cloud.cluster.*;
import edu.ucsb.eucalyptus.util.*;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;
import groovy.lang.*;
import org.apache.log4j.Logger;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.ClusterNodeState;
import com.eucalyptus.cluster.ClusterState;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.BaseDirectory;

import javax.script.ScriptEngineManager;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public class SLAs {

  private static Logger LOG = Logger.getLogger( SLAs.class );

  static String RULES_DIR_NAME = BaseDirectory.CONF.toString() + File.separator + "rules";
  static String ALLOC_RULES_DIR_NAME = RULES_DIR_NAME + File.separator + "allocation";
  static String TIMER_RULES_DIR_NAME = RULES_DIR_NAME + File.separator + "timer";
  static String STATE_RULES_DIR_NAME = RULES_DIR_NAME + File.separator + "state";

  ScriptEngineManager mgr = new ScriptEngineManager();

  public List<ResourceToken> doVmAllocation( VmAllocationInfo vmAllocInfo ) throws FailScriptFailException, NotEnoughResourcesAvailable {
    Collection<Cluster> clusterList = Clusters.getInstance().getEntries();
    SortedSet<ClusterNodeState> clusterStateList = new ConcurrentSkipListSet<ClusterNodeState>( ClusterNodeState.getComparator( vmAllocInfo.getVmTypeInfo() ) );

    //:: prepare the cluster state list :://
    for ( Cluster c : clusterList ) clusterStateList.add( c.getNodeState() );

    //:: find the right allocator to invoke :://
    Allocator blah = this.getAllocator();
    RunInstancesType request = vmAllocInfo.getRequest();
    List<ResourceToken> allocTokenList = blah.allocate( request.getCorrelationId(), request.getUserId(),
                                                        vmAllocInfo.getVmTypeInfo().getName(),
                                                        request.getMinCount(), request.getMaxCount(),
                                                        clusterStateList );
    return allocTokenList;
  }

  public void doNetworkAllocation( String userId, List<ResourceToken> rscTokens, List<Network> networks ) throws NotEnoughResourcesAvailable {
    for ( ResourceToken token : rscTokens ) /*<--- for each cluster */
      for ( Network network : networks ) {/*<--- for each network to allocate */
        try {
          Networks.getInstance().lookup( network.getName() );
        } catch ( NoSuchElementException e ) {
          Networks.getInstance().register( network );
        }
        try {
          token.getNetworkTokens().add( allocateClusterVlan( userId, token.getCluster(), network.getName() ) );
        } catch ( NetworkAlreadyExistsException e ) {}
      }
  }

  private NetworkToken allocateClusterVlan( final String userId, final String clusterName, final String networkName ) throws NotEnoughResourcesAvailable, NetworkAlreadyExistsException {
    ClusterState clusterState = Clusters.getInstance().lookup( clusterName ).getState();
    Network existingNet = Networks.getInstance().lookup( networkName );

    NetworkToken networkToken = clusterState.getNetworkAllocation( userId, existingNet.getNetworkName() );
    LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.preallocate, networkToken ) );

    if ( existingNet.hasToken( networkToken.getCluster() ) ) {
      LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.returned, networkToken ) );
      clusterState.releaseNetworkAllocation( networkToken );
      throw new NetworkAlreadyExistsException();
    } else {
      LOG.info( String.format( EucalyptusProperties.DEBUG_FSTRING, EucalyptusProperties.TokenState.accepted, networkToken ) );
      existingNet.addTokenIfAbsent( networkToken );
      return networkToken;
    }
  }

  private Allocator getAllocator() throws FailScriptFailException {
    Object blah = null;
    try {
      blah = this.getGroovyObject( ALLOC_RULES_DIR_NAME + File.separator + "default.groovy" );
    }
    catch ( FailScriptFailException e ) {
      LOG.error( e, e );
    }
    if ( !( blah instanceof Allocator ) ) throw new FailScriptFailException( blah.getClass() + " does not implement " + Allocator.class );
    return ( Allocator ) blah;
  }

  public Object getGroovyObject( String fileName ) throws FailScriptFailException {
    GroovyObject groovyObject = null;
    try {
      ClassLoader parent = getClass().getClassLoader();
      GroovyClassLoader loader = new GroovyClassLoader( parent );
      Class groovyClass = loader.parseClass( new File( fileName ) );

      groovyObject = ( GroovyObject ) groovyClass.newInstance();
    }
    catch ( Exception e ) {
      throw new FailScriptFailException( e );
    }
    return groovyObject;
  }

}

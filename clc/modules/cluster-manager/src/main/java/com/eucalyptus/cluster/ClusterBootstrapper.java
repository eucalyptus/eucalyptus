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

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

@Provides( resource = Resource.RemoteServices )
@Depends( local = Component.eucalyptus )
public class ClusterBootstrapper extends Bootstrapper implements Runnable {
  private static Logger LOG         = Logger.getLogger( ClusterBootstrapper.class );
  private Thread        thread;
  private boolean       initialized = false;
  private boolean       finished    = false;

  @Override
  public boolean load( Resource current ) throws Exception {
    LOG.info( "Creating the cluster bootstrap thread." );
    this.thread = new Thread( this );
    this.run( );
    return true;
  }

  public static Cluster getCluster( ClusterConfiguration c ) throws EucalyptusCloudException {
    ClusterCredentials credentials = null;
    EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
    try {
      credentials = credDb.getUnique( new ClusterCredentials(c.getName( )) );
    } catch ( EucalyptusCloudException e ) {
      LOG.error("Failed to load credentials for cluster: " + c.getName( ) );
      throw e;
    } finally {
      credDb.rollback( );
    }
    ClusterThreadGroup threadGroup = new ClusterThreadGroup( c, credentials );
    Cluster newCluster = new Cluster( threadGroup, c, credentials );
    return newCluster;
  }

  @Override
  public boolean start( ) throws Exception {
    if( !this.thread.isAlive( ) ) {
      this.thread.start();
    }
    return true;
  }

  public void run( ) {
    while ( !finished ) {
      List<String> clusterNames = Lists.newArrayList( );
      try {
        for ( ClusterConfiguration c : Configuration.getClusterConfigurations( ) ) {
          clusterNames.add( c.getName( ) );
          try {
            Cluster foundCluster = Clusters.getInstance( ).lookup( c.getName( ) );
            if(initialized) {
              foundCluster.getThreadGroup().create( );
            }
          } catch ( NoSuchElementException e ) {
            try {
              Cluster newCluster = ClusterBootstrapper.getCluster( c );
              Clusters.getInstance( ).register( newCluster );
              if(initialized) {
                newCluster.getThreadGroup().create( );
              }
              LOG.info( "Registering cluster: " + newCluster.getName( ) );
            } catch ( Exception e1 ) {
              LOG.error( "Error loading cluster configuration: " + c.getName( ) );
              LOG.error( e1, e1 );
            }
          }
        }
        List<String> registeredClusters = Lists.newArrayList( Clusters.getInstance( ).listKeys( ) );
        registeredClusters.removeAll( clusterNames );
        for ( String removeClusterName : registeredClusters ) {
          try {
            Cluster removeCluster = Clusters.getInstance( ).lookup( removeClusterName );
            removeCluster.getThreadGroup( ).stopThreads( );
            Clusters.getInstance( ).deregister( removeCluster.getName( ) );
          } catch ( NoSuchElementException e ) {
          }
        }
      } catch ( EucalyptusCloudException e ) {
        LOG.error( "Failed to load cluster configurations: " + e.getMessage( ) );
        LOG.error( e, e );
      }
      if ( !initialized ) {
        initialized = true;
        return;
      } else {
        try {
          this.start( );
          Thread.sleep( 5000 );
        } catch ( Exception e ) {
          LOG.error( e,e );
        }
      }
    }
  }

}

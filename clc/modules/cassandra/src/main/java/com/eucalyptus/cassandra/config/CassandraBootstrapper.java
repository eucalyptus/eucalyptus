/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.cassandra.config;

import static com.eucalyptus.bootstrap.Bootstrap.Stage.DatabaseInit;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.cassandra.common.Cassandra;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 *
 */
@RunDuring( DatabaseInit )
@Provides( Cassandra.class )
public class CassandraBootstrapper extends Bootstrapper {

  @Override
  public boolean check( ) throws Exception {
    return CassandraSysUtil.checkCassandra( );
  }

  @Override
  public void destroy( ) throws Exception {
  }

  @Override
  public boolean disable( ) throws Exception {
    return true;
  }

  @Override
  public boolean enable( ) throws Exception {
    return true;
  }

  @Override
  public boolean load( ) throws Exception {
    CassandraSysUtil.createDirectories( );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    if ( !check( ) ) {
      throw new EucalyptusCloudException( "Cannot start cassandra (waiting for seed?)" );
    }
    CassandraSysUtil.createDirectories( );
    CassandraSysUtil.writeConfiguration( );
    CassandraSysUtil.startCassandra( );
    CassandraSysUtil.createKeyspaces( );
    return true;
  }

  @Override
  public boolean stop( ) throws Exception {
    CassandraSysUtil.stopCassandra( );
    return true;
  }
}

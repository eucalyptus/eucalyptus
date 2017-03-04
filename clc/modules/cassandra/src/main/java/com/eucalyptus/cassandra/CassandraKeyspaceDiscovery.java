/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.cassandra;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.cassandra.common.CassandraComponent;
import com.eucalyptus.cassandra.common.CassandraKeyspace;
import com.eucalyptus.system.Ats;

/**
 *
 */
public class CassandraKeyspaceDiscovery  extends ServiceJarDiscovery {
  private static final Logger logger = Logger.getLogger( CassandraKeyspaceDiscovery.class );

  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    if ( CassandraComponent.class.isAssignableFrom( candidate ) &&
        Ats.from( candidate ).has( CassandraKeyspace.class ) ) {
      try {
        final CassandraKeyspace keyspace = Ats.from( candidate ).get( CassandraKeyspace.class );
        logger.info( "Registering keyspace : " + keyspace.value( ) );
        CassandraKeyspaces.register( keyspace.value( ), keyspace.replicas( ) );
        return true;
      } catch ( final Exception ex ) {
        logger.error( "Error in cassandra keyspace discovery for " + candidate, ex );
        return false;
      }

    } else {
      return false;
    }
  }

  @Override
  public Double getPriority( ) {
    return 0.1d;
  }
}


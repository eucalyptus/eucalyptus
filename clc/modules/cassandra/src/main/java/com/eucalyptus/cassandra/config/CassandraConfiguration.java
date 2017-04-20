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
package com.eucalyptus.cassandra.config;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import com.eucalyptus.cassandra.common.Cassandra;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.config.ComponentConfiguration;

/**
 *
 */
@Entity
@PersistenceContext( name="eucalyptus_config" )
@ComponentPart( Cassandra.class )
public class CassandraConfiguration extends ComponentConfiguration implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final String SERVICE_PATH= "/services/Cassandra";

  public CassandraConfiguration( ) { }

  public CassandraConfiguration(
      final String partition,
      final String name,
      final String hostName,
      final Integer port
  ) {
    super( partition, name, hostName, port, SERVICE_PATH );
  }

  @Override
  public int compareTo( final ServiceConfiguration that ) {
    return getHostName( ).compareTo( that.getHostName( ) );
  }
}
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

import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.cassandra.common.Cassandra;
import com.eucalyptus.cassandra.common.config.DeregisterCassandraType;
import com.eucalyptus.cassandra.common.config.DescribeCassandraType;
import com.eucalyptus.cassandra.common.config.ModifyCassandraAttributeType;
import com.eucalyptus.cassandra.common.config.RegisterCassandraType;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;

/**
 *
 */
@ComponentPart( Cassandra.class )
@Handles( {
    DeregisterCassandraType.class,
    DescribeCassandraType.class,
    ModifyCassandraAttributeType.class,
    RegisterCassandraType.class,
} )
public class CassandraServiceBuilder extends AbstractServiceBuilder<CassandraConfiguration> {

  @Override
  public CassandraConfiguration newInstance( ) {
    return new CassandraConfiguration( );
  }

  @Override
  public CassandraConfiguration newInstance(
      final String partition,
      final String name,
      final String host,
      final Integer port
  ) {
    return new CassandraConfiguration( partition, name, host, port );
  }

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( Cassandra.class );
  }

  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException { }

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException { }
}

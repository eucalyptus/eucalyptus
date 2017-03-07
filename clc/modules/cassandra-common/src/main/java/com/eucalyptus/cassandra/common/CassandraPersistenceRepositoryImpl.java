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
package com.eucalyptus.cassandra.common;

import java.io.Serializable;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.data.cassandra.repository.query.CassandraEntityInformation;
import org.springframework.data.cassandra.repository.support.BasicMapId;
import org.springframework.data.cassandra.repository.support.SimpleCassandraRepository;
import com.eucalyptus.util.Parameters;

/**
 * Implementation for eucalyptus specific repository functionality.
 */
public class CassandraPersistenceRepositoryImpl<E> extends SimpleCassandraRepository<E,MapId>
    implements CassandraPersistenceRepository<E> {

  private final CassandraPersistenceTemplate template;

  public CassandraPersistenceRepositoryImpl(
      final CassandraEntityInformation<E, MapId> metadata,
      final CassandraPersistenceTemplate template ) {
    super( metadata, template );
    this.template = template;
  }

  public CassandraPersistenceTemplate template( ) {
    return template;
  }

  public MapId id( ) {
    return new NullCheckingBasicMapId( );
  }

  private static class NullCheckingBasicMapId extends BasicMapId {
    private static final long serialVersionUID = 1L;

    @Override
    public BasicMapId with( final String name, final Serializable value ) {
      return super.with( name, Parameters.checkParamNotNull( name, value ) );
    }
  }
}

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
package com.eucalyptus.cassandra.common;

import java.io.Serializable;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.MapId;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Extension of spring data CassandraRepository with eucalyptus specific functionality.
 */
@NoRepositoryBean
public interface CassandraPersistenceRepository<E> extends CassandraRepository<E> {

  /**
   * Create a new MapId that checks for null values.
   *
   * @return The new MapId instance.
   * @see MapId#with(String, Serializable)
   */
  MapId id( );

  /**
   * Access underlying CassandraPersistenceTemplate.
   *
   * @return The template
   */
  CassandraPersistenceTemplate template( );
}

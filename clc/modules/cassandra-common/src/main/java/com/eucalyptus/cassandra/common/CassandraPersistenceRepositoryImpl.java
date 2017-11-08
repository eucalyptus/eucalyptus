/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import com.eucalyptus.cassandra.common.CassandraPersistence;
import com.eucalyptus.cassandra.common.CassandraReplicas;
import com.eucalyptus.cassandra.common.util.CqlUtil;
import com.eucalyptus.util.Either;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Parameters;
import com.eucalyptus.util.ThrowingFunction;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import javaslang.Tuple2;

/**
 *
 */
public class CassandraKeyspaces {
  private static final ConcurrentMap<String,KeyspaceSpec> keyspaceMap = Maps.newConcurrentMap( );
  private static final String KEYSPACE_CQL =
      "CREATE KEYSPACE IF NOT EXISTS %s WITH replication = {'class': 'SimpleStrategy', 'replication_factor': %d};";
  private static final String KEYSPACE_RESOURCE_TEMPLATE = "classpath*:*-*-*-%s-*.cql";

  /**
   * Process all keyspaces, returning any failures.
   */
  public static List<Tuple2<String,Optional<Throwable>>> all( ) {
    return keyspaceMap.values( ).stream( )
        .map( KeyspaceSpec::name )
        .sorted( String.CASE_INSENSITIVE_ORDER )
        .map( FUtils.tuple(
            FUtils.eitherThrowable( FUtils.function( CassandraKeyspaces::keyspace ) ).andThen( Either.leftOption( ) )
        ) )
        .collect( Collectors.toList( ) );
  }

  /**
   * Process the specified keyspace
   */
  public static void keyspace( final String name ) {
    final KeyspaceSpec keyspaceSpec = keyspaceMap.get( name );
    if ( keyspaceSpec == null ) {
      throw new IllegalArgumentException( "Unknown keyspace: " + name );
    }
    CassandraPersistence.doWithSession(
        CassandraPersistence.SessionUsage.Admin,
        name,
        session -> {
          session.execute( String.format(
              KEYSPACE_CQL,
              keyspaceSpec.name( ),
              keyspaceSpec.replicas( ).replicas( 1  ) ) );
          scripts( name )
              .flatMap( ThrowingFunction.undeclared( CqlUtil::splitCql ).andThen( Collection::stream ) )
              .forEach( session::execute );
          return null;
        } );
  }

  static void register(
      @Nonnull final String keyspace,
      @Nonnull final CassandraReplicas replicas
  ) {
    Parameters.checkParamNotNull( "keyspace", keyspace );
    keyspaceMap.putIfAbsent( keyspace, new KeyspaceSpec( keyspace, replicas ) );
  }

  private static Stream<String> scripts( final String keyspace ) {
    // discover and sort cql scriptFilenames
    final List<String> scripts = Lists.newArrayList( );
    final PathMatchingResourcePatternResolver resolver =
        new PathMatchingResourcePatternResolver( CassandraKeyspaces.class.getClassLoader( ) );
    try {
      final String pattern =  String.format( KEYSPACE_RESOURCE_TEMPLATE, scriptName( keyspace ) );
      final Resource[] resources = resolver.getResources( pattern );
      final List<String> scriptFilenames = Lists.newArrayList( );
      for ( final Resource resource : resources ) {
        scriptFilenames.add( resource.getFilename( ) );
      }
      Collections.sort( scriptFilenames );
      for ( final String resourceName : scriptFilenames ) {
        scripts.add( Resources.toString( Resources.getResource( resourceName ), StandardCharsets.UTF_8 ) );
      }
    } catch ( IOException e ) {
      throw Exceptions.toUndeclared( e);
    }

    return scripts.stream( );
  }

  private static String scriptName( final String keyspace ) {
    return keyspace.replace( '_', '-' ).toLowerCase( );
  }

  private static final class KeyspaceSpec {
    private final String name;
    private final CassandraReplicas replicas;

    private KeyspaceSpec( final String name, final CassandraReplicas replicas ) {
      this.name = Parameters.checkParamNotNullOrEmpty( "name", name );
      this.replicas = Parameters.checkParamNotNull( "replicas", replicas );
    }

    public String name( ) {
      return name;
    }

    public CassandraReplicas replicas( ) {
      return replicas;
    }
  }
}

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
import javaslang.Function3;
import javaslang.Tuple2;

/**
 *
 */
public class CassandraKeyspaces {
  private static final ConcurrentMap<String,KeyspaceSpec> keyspaceMap = Maps.newConcurrentMap( );
  private static final String KEYSPACE_CQL =
      "CREATE KEYSPACE IF NOT EXISTS %1$s WITH REPLICATION = {'class': 'NetworkTopologyStrategy', '%2$s': %3$d};";
  private static final String KEYSPACE_RESOURCE_TEMPLATE = "classpath*:*-*-*-%s-*.cql";

  /**
   * Process all keyspaces, returning any failures.
   */
  public static List<Tuple2<String,Optional<Throwable>>> all( final String dc, final int dcNodes ) {
    return keyspaceMap.values( ).stream( )
        .map( KeyspaceSpec::name )
        .sorted( String.CASE_INSENSITIVE_ORDER )
        .map( FUtils.tuple( FUtils.eitherThrowable(
            Function3.of( CassandraKeyspaces::keyspace ).reversed( ).apply( dcNodes, dc )
        ).andThen( Either.leftOption( ) ) ) )
        .collect( Collectors.toList( ) );
  }

  /**
   * Process the specified keyspace
   */
  public static boolean keyspace( final String name, final String dc, final int dcNodes ) {
    final KeyspaceSpec keyspaceSpec = keyspaceMap.get( name );
    if ( keyspaceSpec == null ) {
      throw new IllegalArgumentException( "Unknown keyspace: " + name );
    }
    return CassandraPersistence.doWithSession(
        CassandraPersistence.SessionUsage.Admin,
        name,
        session -> {
          session.execute( String.format(
              KEYSPACE_CQL,
              keyspaceSpec.name( ),
              dc,
              keyspaceSpec.replicas( ).replicas( dcNodes  ) ) );
          scripts( name )
              .flatMap( ThrowingFunction.undeclared( CqlUtil::splitCql ).andThen( Collection::stream ) )
              .forEach( session::execute );
          return true;
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

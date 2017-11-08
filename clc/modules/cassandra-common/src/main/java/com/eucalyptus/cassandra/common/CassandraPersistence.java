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

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.cassandra.repository.support.CassandraRepositoryFactory;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.NettyOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.ThreadingOptions;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.Parameters;
import com.eucalyptus.util.ThrowingFunction;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * API for accessing a cassandra store
 */
public class CassandraPersistence {

  private static final ConcurrentMap<String,Session> sessionMap = Maps.newConcurrentMap( ); // sessions by keyspace
  private static final ConcurrentMap<Pair<String,Class<? extends CassandraPersistenceRepository>>,
      CassandraPersistenceRepository> repositoryMap = Maps.newConcurrentMap( ); // repositories by keyspace/type
  private static final Lock sessionLock = new ReentrantLock( );
  private static final RetryTemplate template =
      buildRetryTemplate( NoSuchElementException.class, 15_000L, TimeUnit.MINUTES.toMillis( 5 ) );
  private static final RetryTemplate startupRetryTemplate =
      buildRetryTemplate( NoHostAvailableException.class, 15_000L, TimeUnit.MINUTES.toMillis( 1 ) );

  /**
   * Perform work using a datastax session in a callback.
   *
   * Using template or repository callbacks is preferred.
   *
   * @param keyspace The keyspace for the session
   * @param callbackFunction The callback that will perform work
   * @param <R> The result type
   * @return The result from the callback which can be null
   * @see #doWithTemplate(String, Function)
   * @see #doWithRepository(Class, Function)
   */
  public static <R> R doWithSession(
      final String keyspace,
      final Function<? super Session,? extends R> callbackFunction
  ) {
    return doWithSession( SessionUsage.Service, keyspace, callbackFunction );
  };

  /**
   * Perform work using a datastax session in a callback.
   *
   * Using template or repository callbacks is preferred.
   *
   * @param keyspace The keyspace for the session
   * @param callbackFunction The callback that will perform work and may throw an exception
   * @param <R> The result type
   * @param <E> The exception type
   * @return The result from the callback which can be null
   * @throws E if thrown from the callback
   * @see #doThrowsWithTemplate(String, ThrowingFunction)
   * @see #doThrowsWithRepository(Class, ThrowingFunction)
   */
  public static <R,E extends Throwable> R doThrowsWithSession(
      final String keyspace,
      final ThrowingFunction<? super Session,? extends R, ? extends E> callbackFunction
  ) throws E {
    return doThrowsWithSession( SessionUsage.Service, keyspace, callbackFunction );
  };

  /**
   * Perform work using a datastax session in a callback.
   *
   * Using template or repository callbacks is preferred.
   *
   * @param keyspace The keyspace for the session
   * @param usage The session usage, typically SessionUsage.Service
   * @param callbackFunction The callback that will perform work
   * @param <R> The result type
   * @return The result from the callback which can be null
   * @see #doWithTemplate(String, Function)
   * @see #doWithRepository(Class, Function)
   */
  public static <R> R doWithSession(
      final SessionUsage usage,
      final String keyspace,
      final Function<? super Session,? extends R> callbackFunction
  ) {
    final Session session = getSession( usage, keyspace );
    try {
      return callbackFunction.apply( session );
    } finally {
      releaseSession( usage, session, keyspace );
    }
  };

  /**
   * Perform work using a datastax session in a callback.
   *
   * Using template or repository callbacks is preferred.
   *
   * @param keyspace The keyspace for the session
   * @param usage The session usage, typically SessionUsage.Service
   * @param callbackFunction The callback that will perform work and may throw an exception
   * @param <R> The result type
   * @param <E> The exception type
   * @return The result from the callback which can be null
   * @throws E if thrown from the callback
   * @see #doThrowsWithTemplate(String, ThrowingFunction)
   * @see #doThrowsWithRepository(Class, ThrowingFunction)
   */
  public static <R,E extends Throwable> R doThrowsWithSession(
      final SessionUsage usage,
      final String keyspace,
      final ThrowingFunction<? super Session,? extends R, ? extends E> callbackFunction
  ) throws E {
    final Session session = getSession( usage, keyspace );
    try {
      return callbackFunction.apply( session );
    } finally {
      releaseSession( usage, session, keyspace );
    }
  };

  /**
   * Perform work using a spring data cassandra template in a callback.
   *
   * @param keyspace The keyspace for the underlying cassandra session
   * @param callbackFunction The callback that will perform work
   * @param <R> The result type
   * @return The result from the callback which can be null
   */
  public static <R> R doWithTemplate(
      final String keyspace,
      final Function<? super CassandraPersistenceTemplate,? extends R> callbackFunction
  ) {
    return callbackFunction.apply(
        new CassandraPersistenceTemplate( getSession( SessionUsage.Service, keyspace ), keyspace ) );
  }

  /**
   * Perform work using a spring data cassandra template in a callback.
   *
   * @param keyspace The keyspace for the underlying cassandra session
   * @param callbackFunction The callback that will perform work and may throw an exception
   * @param <R> The result type
   * @param <E> The exception type
   * @return The result from the callback which can be null
   * @throws E if thrown from the callback
   */
  public static <R,E extends Throwable> R doThrowsWithTemplate(
      final String keyspace,
      final ThrowingFunction<? super CassandraPersistenceTemplate,? extends R, ? extends E> callbackFunction
  ) throws E {
    return callbackFunction.apply(
        new CassandraPersistenceTemplate( getSession( SessionUsage.Service, keyspace ), keyspace ) );
  }

  /**
   * Perform work using the given service specific repository.
   *
   * @param repositoryType Class for the repository type
   * @param callbackFunction The callback that will perform work
   * @param <R> The result type
   * @param <RT> The repository type
   * @return The result from the callback which can be null
   */
  public static <R,RT extends CassandraPersistenceRepository> R doWithRepository(
      final Class<RT> repositoryType,
      final Function<? super RT,? extends R> callbackFunction
  ) {
    return callbackFunction.apply( getRepository( repositoryType ) );
  }

  /**
   * Perform work using the given service specific repository.
   *
   * @param repositoryType Class for the repository type
   * @param callbackFunction The callback that will perform work and may throw an exception
   * @param <R> The result type
   * @param <RT> The repository type
   * @param <E> The exception type
   * @return The result from the callback which can be null
   * @throws E if thrown from the callback
   */
  public static <R,RT extends CassandraPersistenceRepository,E extends Throwable> R doThrowsWithRepository(
      final Class<RT> repositoryType,
      final ThrowingFunction<? super RT,? extends R, ? extends E> callbackFunction
  ) throws E {
    return callbackFunction.apply( getRepository( repositoryType ) );
  }

  private static String keyspace( final Class<?> repositoryType  ) {
    final Ats repositoryAts = Ats.from( repositoryType );
    return
        repositoryAts.getOption( CassandraKeyspace.class )
            .orElse( repositoryAts.getOption( ComponentPart.class )
                .flatMap( componentPart -> Ats.from( componentPart.value( ) ).getOption( CassandraKeyspace.class ) ) )
            .map( CassandraKeyspace::value )
            .getOrElse( (String)null );
  }

  @SuppressWarnings( "unchecked" )
  private static <RT extends CassandraPersistenceRepository> RT getRepository( final Class<RT> repositoryType ) {
    final String keyspace = keyspace( repositoryType );
    final Pair<String,Class<? extends CassandraPersistenceRepository>> key = Pair.of( keyspace, repositoryType );
    return (RT) repositoryMap.computeIfAbsent( key, keyPair -> {
      final CassandraRepositoryFactory factory =  new CassandraRepositoryFactory(
          new CassandraPersistenceTemplate( getSession( SessionUsage.Service, keyspace ), keyspace ) );
      factory.setRepositoryBaseClass( CassandraPersistenceRepositoryImpl.class );
      return factory.getRepository( repositoryType );
    } );
  }

  private static Session getSession( final SessionUsage usage, final String keyspace ) {
    Parameters.checkParamNotNull( "usage", usage );
    Session session = sessionMap.get( Parameters.checkParamNotNullOrEmpty( "keyspace", keyspace ) );
    if ( session == null ) {
      final List<ServiceConfiguration> configurations = usage.getCassandraServiceConfigurations( );
      try ( final LockResource lockResource = LockResource.lock( sessionLock ) ) {
        session = sessionMap.get( keyspace );
        if ( session == null ) {
          session = usage.buildSession( configurations, keyspace );
          sessionMap.put( keyspace, session );
        }
      }
    }
    return session;
  }

  private static Session buildSession( final List<ServiceConfiguration> configurations, final String keyspace ) {
    final Cluster cluster = Cluster.builder( )
        .addContactPointsWithPorts( configurations.stream( ).map( ServiceConfiguration::getSocketAddress ).collect( Collectors.toList( ) ) )
        //.withLoadBalancingPolicy(  ) //TODO topology aware policy?
        .withNettyOptions( new NettyOptions( ) {
          @Override
          public EventLoopGroup eventLoopGroup( final ThreadFactory threadFactory ) {
            return new NioEventLoopGroup( 0, threadFactory );
          }
        } )
        .withReconnectionPolicy( new ExponentialReconnectionPolicy( 1_000L, 60_000L ) )
        .withRetryPolicy( DefaultRetryPolicy.INSTANCE )
        //.withSSL( new NettySSLOptions( ) ) //TODO use ssl
        .withThreadingOptions( new ThreadingOptions( ) {
          @Override
          public ThreadFactory createThreadFactory( final String clusterName, final String executorName ) {
            return super.createThreadFactory( "cassandra-client", executorName );
          }
        } )
        .withoutJMXReporting( )
        .build( );
    return cluster.connect( keyspace );
  }

  private static void releaseSession( final SessionUsage usage, final Session session, final String keyspace ) {
    usage.releaseSession( session, keyspace );
  }

  private static RetryTemplate buildRetryTemplate(
      final Class<? extends Throwable> thrownType,
      final long maxBackoffInterval,
      final long timeout
  ) {
    // retry with timeout on expected exception
    final TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy( );
    timeoutRetryPolicy.setTimeout( timeout );
    final ExceptionClassifierRetryPolicy exceptionRetryPolicy = new ExceptionClassifierRetryPolicy( );
    exceptionRetryPolicy.setPolicyMap( Collections.singletonMap( thrownType, timeoutRetryPolicy ) );

    // use exponential backoff
    final ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy( );
    backOffPolicy.setMaxInterval( maxBackoffInterval );

    final RetryTemplate template = new RetryTemplate( );
    template.setRetryPolicy( exceptionRetryPolicy );
    template.setBackOffPolicy( backOffPolicy );
    return template;
  }

  public enum SessionUsage {
    /**
     * Administrative session usage, should not be used by services
     */
    Admin {
      @Override
      List<ServiceConfiguration> getCassandraServiceConfigurations( ) {
        try {
          return Lists.newArrayList( Topology.lookupAtLeastOne( Cassandra.class ) );
        } catch ( final NoSuchElementException e ) {
          return Collections.singletonList( Components.lookup( Cassandra.class ).getLocalServiceConfiguration( ) );
        }
      }

      @Override
      Session buildSession( final List<ServiceConfiguration> configurations, final String keyspace ) {
        return startupRetryTemplate.execute( retryContext ->
            CassandraPersistence.buildSession( configurations, null ) ); // keyspace may not be created at this point
      }

      @Override
      void releaseSession( final Session session, final String keyspace ) {
        session.execute( "USE " + keyspace );
      }
    },
    /**
     * General purpose session usage for services (etc)
     */
    Service {
      @Override
      List<ServiceConfiguration> getCassandraServiceConfigurations( ) {
        return template.execute( retryContext -> Lists.newArrayList( Topology.lookupAtLeastOne( Cassandra.class ) ) );
      }
    },
    ;

    abstract List<ServiceConfiguration> getCassandraServiceConfigurations( );
    Session buildSession( final List<ServiceConfiguration> configurations, final String keyspace ) {
      return CassandraPersistence.buildSession( configurations, keyspace );
    }
    void releaseSession( final Session session, final String keyspace ) { }
  }
}

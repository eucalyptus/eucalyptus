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

import java.io.File;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.NettyOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.ThreadingOptions;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.LockResource;
import com.eucalyptus.util.ThrowingFunction;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 *
 */
public class CassandraPersistence {

  private static final AtomicReference<Session> sessionRef = new AtomicReference<>( );
  private static final Lock sessionLock = new ReentrantLock( );
  private static final RetryTemplate template =
      buildRetryTemplate( NoSuchElementException.class, 15_000L, TimeUnit.MINUTES.toMillis( 5 ) );
  private static final boolean cassandraInstalled = new File( "/usr/sbin/cassandra" ).exists( );

  public static <R> R doWithSession(
      final String sessionKey, // currently unused
      final Function<? super Session,? extends R> callbackFunction
  ) {
    return callbackFunction.apply( getSession( ) );
  };

  public static <R,E extends Throwable> R doWithSession(
      final String sessionKey, // currently unused
      final ThrowingFunction<? super Session,? extends R, E> callbackFunction
  ) throws E {
    return callbackFunction.apply( getSession( ) );
  };

  private static Session getSession( ) {
    Session session = sessionRef.get( );
    if ( session == null ) {
      try ( final LockResource lockResource = LockResource.lock( sessionLock ) ) {
        session = sessionRef.get( );
        if ( session == null ) {
          session = buildSession( getCassandraServiceConfiguration( ) );
          sessionRef.set( session );
        }
      }
    }
    return session;
  }

  /**
   * Temporary helper used to check if cassandra is available.
   *
   * TODO:STEVE: remove
   *
   * @deprecated
   */
  @Deprecated
  public static boolean isAvailable( ) {
    return cassandraInstalled;
  }

  private static ServiceConfiguration getCassandraServiceConfiguration( ) {
    return template.execute( retryContext -> Topology.lookup( Cassandra.class ) );
  }

  private static Session buildSession( final ServiceConfiguration configuration ) {
    final Cluster cluster = Cluster.builder( )
        .addContactPoints( configuration.getInetAddress( ) )
        //.withLoadBalancingPolicy(  ) //TODO topology aware policy?
        .withNettyOptions( new NettyOptions( ) {
          @Override
          public EventLoopGroup eventLoopGroup( final ThreadFactory threadFactory ) {
            return new NioEventLoopGroup( 0, threadFactory );
          }
        } )
        .withPort( configuration.getPort( ) )
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
    return cluster.connect( );
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
}

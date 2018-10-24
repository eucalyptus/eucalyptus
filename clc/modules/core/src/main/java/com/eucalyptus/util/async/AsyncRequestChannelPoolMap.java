/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.util.async;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.MoreObjects;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;

/**
 *
 */
class AsyncRequestChannelPoolMap extends AbstractChannelPoolMap<AsyncRequestChannelPoolMap.ChannelPoolKey, SimpleChannelPool> {

  private static final AtomicLong CHANNEL_CREATES  = new AtomicLong( 0L );
  private static final AtomicLong CHANNEL_RELEASES = new AtomicLong( 0L );
  private static final AtomicLong CHANNEL_ACQUIRES = new AtomicLong( 0L );

  private static final AttributeKey<Long> POOL_RELEASED = AttributeKey.newInstance("asyncReleasedTime");
  private static final AttributeKey<Integer> POOL_REQUESTS = AttributeKey.newInstance("asyncRequestCount");

  private static final String CHANNEL_REUSE_MAX_IDLE_PROP     = "com.eucalyptus.util.async.channelReuseMaxIdle";
  private static final String CHANNEL_REUSE_MAX_REQUESTS_PROP = "com.eucalyptus.util.async.channelReuseMaxRequests";

  private static long CHANNEL_REUSE_MAX_IDLE =
      MoreObjects.firstNonNull( Longs.tryParse( System.getProperty( CHANNEL_REUSE_MAX_IDLE_PROP, "" ) ), 25_000L );
  private static int  CHANNEL_REUSE_MAX_REQUESTS =
      MoreObjects.firstNonNull( Ints.tryParse( System.getProperty( CHANNEL_REUSE_MAX_REQUESTS_PROP, "" ) ), 75 );

  @Override
  protected SimpleChannelPool newPool( final ChannelPoolKey key ) {
    final Bootstrap bootstrap = key.bootstrap.remoteAddress( key.address );
    final ChannelPoolHandler handler = new AsyncRequestsChannelPoolHandler( key.initializer );
    final ChannelHealthChecker checker = new AsyncRequestsChannelHealthChecker( );
    if ( key.size <= 0 ) {
      return new SimpleChannelPool(
          bootstrap,
          handler,
          checker );
    } else {
      return new FixedChannelPool(
          bootstrap,
          handler,
          checker,
          FixedChannelPool.AcquireTimeoutAction.FAIL,
          MoreObjects.firstNonNull( StackConfiguration.CLIENT_HTTP_POOL_ACQUIRE_TIMEOUT, 60_000L ),
          key.size,
          Integer.MAX_VALUE );
    }
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "size", size( ) )
        .add( "channelsCreated", CHANNEL_CREATES.get( ) )
        .add( "channelsAcquired", CHANNEL_ACQUIRES.get( ) )
        .add( "channelsReleased", CHANNEL_RELEASES.get( ) )
        .toString( );
  }

  static final class ChannelPoolKey {
    private final Bootstrap bootstrap;
    private final ChannelInitializer<?> initializer;
    private final InetSocketAddress address;
    private final int size;

    ChannelPoolKey(
        final Bootstrap bootstrap,
        final ChannelInitializer<?> initializer,
        final InetSocketAddress address,
        final int size
    ) {
      this.bootstrap = bootstrap;
      this.initializer = initializer;
      this.address = address;
      this.size = size;
    }

    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass( ) != o.getClass( ) ) return false;
      final ChannelPoolKey that = (ChannelPoolKey) o;
      return size == that.size &&
          Objects.equals( initializer, that.initializer ) &&
          Objects.equals( address, that.address );
    }

    @Override
    public int hashCode() {
      return Objects.hash( initializer, address, size );
    }

    @Override
    public String toString( ) {
      return MoreObjects.toStringHelper( this )
          .add( "address", address )
          .add( "initializer", initializer.getClass( ).getSimpleName( ) )
          .add( "size", size )
          .toString( );
    }
  }

  private static final class AsyncRequestsChannelPoolHandler extends AbstractChannelPoolHandler {
    private final ChannelInitializer<?> initializer;

    AsyncRequestsChannelPoolHandler( final ChannelInitializer<?> initializer ) {
      this.initializer = initializer;
    }

    @Override
    public void channelCreated( final Channel ch ) {
      CHANNEL_CREATES.incrementAndGet( );
      ch.attr( POOL_RELEASED ).set( System.currentTimeMillis( ) );
      ch.attr( POOL_REQUESTS ).set( 0 );
      ch.pipeline( ).addLast( initializer );
    }

    @Override
    public void channelReleased( final Channel ch ) {
      CHANNEL_RELEASES.incrementAndGet( );
      ch.attr( POOL_RELEASED ).set( System.currentTimeMillis( ) );
      ch.pipeline( ).remove( AsyncRequestHandler.class );
    }

    @Override
    public void channelAcquired( final Channel ch ) {
      CHANNEL_ACQUIRES.incrementAndGet( );
      ch.attr( POOL_REQUESTS ).set( ch.attr( POOL_REQUESTS ).get( ) + 1 );
    }
  }

  private static final class AsyncRequestsChannelHealthChecker implements ChannelHealthChecker {
    @Override
    public Future<Boolean> isHealthy( final Channel channel ) {
      // do not reuse channel if idle too long or has handled too many requests
      final long timeSinceUsed = System.currentTimeMillis( ) - channel.attr( POOL_RELEASED ).get( );
      final int requestCount = channel.attr( POOL_REQUESTS ).get( );
      if ( timeSinceUsed > CHANNEL_REUSE_MAX_IDLE || requestCount > CHANNEL_REUSE_MAX_REQUESTS ) {
        return channel.eventLoop( ).newSucceededFuture( Boolean.FALSE );
      }
      // reuse if active
      return ChannelHealthChecker.ACTIVE.isHealthy( channel );
    }
  }
}



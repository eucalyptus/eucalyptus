/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.network.applicator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.OrderedShutdown;
import com.eucalyptus.cluster.common.broadcast.BNI;
import com.eucalyptus.cluster.common.broadcast.BNetworkInfo;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.network.NetworkGroups;
import com.eucalyptus.network.NetworkMode;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.Pair;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 *
 */
@SuppressWarnings( { "Guava", "WeakerAccess" } )
public class AppliedVersionApplicator extends ModeSpecificApplicator {

  private static final String APPLIED_VERSION_FILE = "global_network_info.version";

  private static final Logger logger = Logger.getLogger( AppliedVersionApplicator.class );
  private static final AtomicReference<Pair<WatchService,WatchKey>> watchContext = new AtomicReference<>( );
  private static final AtomicReference<Pair<Long,String>> lastAppliedVersion = new AtomicReference<>( );
  private static final Supplier<String> faultSupplier = Suppliers.memoizeWithExpiration(
      () -> Faults.forComponent( Eucalyptus.class ).havingId( 1016 ).log( ),
      15, TimeUnit.MINUTES );

  static {
    OrderedShutdown.registerPreShutdownHook( AppliedVersionApplicator::shutdown );
  }

  public AppliedVersionApplicator( ) {
    super( EnumSet.of( NetworkMode.VPCMIDO ) );
  }

  @Override
  protected void modeApply(
      final NetworkMode mode,
      final ApplicatorContext context,
      final ApplicatorChain chain
  ) throws ApplicatorException {
    final BNetworkInfo info = context.getNetworkInfo( );
    final Pair<Long,String> lastAppliedVersion = getLastAppliedVersion( );
    boolean alreadyApplied = false;
    if ( lastAppliedVersion != null ) {
      context.setAttribute(
          ApplicatorContext.INFO_KEY,
          BNI.networkInfo( ).from( info )
              .setValueAppliedTime( Timestamps.formatIso8601Timestamp( new Date( lastAppliedVersion.getLeft( ) ) ) )
              .setValueAppliedVersion( lastAppliedVersion.getRight( ) )
              .o( ) );
      MarshallingApplicatorHelper.clearMarshalledNetworkInfoCache( context );
      alreadyApplied = info.version( ).get( ).equals( lastAppliedVersion.getRight( ) );
    }

    // initial broadcast
    chain.applyNext( context );

    // wait for eucanetd to apply
    boolean applied = false;
    final Path path = BaseDirectory.RUN.getChildFile( APPLIED_VERSION_FILE ).toPath( );
    final long until = System.currentTimeMillis( ) + TimeUnit.SECONDS.toMillis( NetworkGroups.MAX_BROADCAST_APPLY );
    long now;
    waitloop:
    while( !Bootstrap.isShuttingDown( ) && !alreadyApplied && ( now = System.currentTimeMillis( ) ) < until ) try {
      final WatchKey key = getWatchService( ).poll( until - now, TimeUnit.MILLISECONDS );
      if ( key != null && key.isValid( ) ) try {
        for ( final WatchEvent<?> event: key.pollEvents( ) ) {
          if ( path.getFileName( ).equals( event.context( ) ) ) {
            try {
              final Pair<Long,String> appliedVersion = readAppliedVersion( path );
              if ( !info.version( ).get( ).equals( appliedVersion.getRight( ) ) ) {
                continue waitloop; // wait until timeout
              }
              context.setAttribute(
                  ApplicatorContext.INFO_KEY,
                  BNI.networkInfo( ).from( info )
                      .setValueAppliedTime( Timestamps.formatIso8601Timestamp( new Date( appliedVersion.getLeft( ) ) ) )
                      .setValueAppliedVersion( appliedVersion.getRight( ) )
                      .o( ) );
              MarshallingApplicatorHelper.clearMarshalledNetworkInfoCache( context );
              applied = true;
              break waitloop;
            } catch ( IOException e ) {
              logger.error( "Error reading last applied network broadcast version" );
            }
          }
        }
      } finally {
        key.reset( );
      }
    } catch ( ClosedWatchServiceException | InterruptedException e ) {
      break;
    }

    // broadcast with updated info
    if ( applied ) {
      chain.applyNext( context );
    } else if ( !alreadyApplied ) {
      faultSupplier.get( );
    }
  }

  @Nullable
  private Pair<Long,String> getLastAppliedVersion( ) throws ApplicatorException {
    Pair<Long,String> lastApplied = lastAppliedVersion.get( );
    final Path path = BaseDirectory.RUN.getChildFile( APPLIED_VERSION_FILE ).toPath( );
    if ( lastApplied == null && path.toFile( ).canRead( ) ) {
      try {
        lastApplied = readAppliedVersion( path );
      } catch ( IOException e ) {
        throw new ApplicatorException( "Error reading applied version", e );
      }
    }
    return lastApplied;
  }

  private Pair<Long,String> readAppliedVersion( final Path path ) throws IOException {
    final String appliedVersion =
        com.google.common.io.Files.toString( path.toFile( ), StandardCharsets.UTF_8 ).trim( );
    final long appliedTime = System.currentTimeMillis( );
    final Pair<Long,String> appliedVersionPair = Pair.pair( appliedTime, appliedVersion );
    lastAppliedVersion.set( appliedVersionPair );
    return appliedVersionPair;
  }

  private WatchService getWatchService( ) throws ApplicatorException {
    Pair<WatchService,WatchKey> watchPair = watchContext.get( );
    if ( watchPair == null ) try {
      final Path path = BaseDirectory.RUN.getFile( ).toPath( );
      final WatchService watcher = path.getFileSystem( ).newWatchService( );
      final WatchKey watckKey = path.register( watcher,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_MODIFY
      );
      watchPair = Pair.pair( watcher, watckKey );
      watchContext.set( watchPair );
    } catch ( IOException e ) {
      throw new ApplicatorException( "Error setting up file watch", e );
    }
    return watchPair.getLeft( );
  }

  private static void shutdown( ) {
    final Pair<WatchService,WatchKey> watchPair = watchContext.get( );
    if ( watchPair != null ) {
      watchPair.getRight( ).cancel( );
      try {
        watchPair.getLeft( ).close( );
      } catch ( IOException e ) {
        logger.error( "Error closing watch service", e );
      }
    }
  }
}

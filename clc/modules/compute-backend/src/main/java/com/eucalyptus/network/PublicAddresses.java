package com.eucalyptus.network;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 *
 */
public class PublicAddresses {

  private static final Map<String, String> dirtyAddresses = Maps.newConcurrentMap( );

  public static void markDirty( String address, String partition ) {
    dirtyAddresses.put( address, partition );
  }

  public static boolean clearDirty( final Collection<String> inUse, final String partition ) {
    final AtomicBoolean cleared = new AtomicBoolean( false );
    dirtyAddresses.forEach( ( address, addressPartition ) -> {
      if ( partition.equals( addressPartition ) && !inUse.contains( address ) ) {
        cleared.set( dirtyAddresses.remove( address ) != null || cleared.get( ) );
      }
    } );
    return cleared.get( );
  }

  public static boolean clearDirty( String address ) {
    return dirtyAddresses.remove( address ) != null;
  }

  public static Set<String> dirtySnapshot( ) {
    return Sets.newHashSet( dirtyAddresses.keySet( ) );
  }
}

package com.eucalyptus.network;

import java.util.ServiceLoader;
import javax.annotation.Nullable;
import com.eucalyptus.compute.common.internal.util.NotEnoughResourcesException;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.util.CompatFunction;
import com.google.common.net.InetAddresses;

/**
 * Private address functionality
 */
public class PrivateAddresses {

  private static final PrivateAddressAllocator allocator =
      ServiceLoader.load( PrivateAddressAllocator.class ).iterator( ).next( );

  @SuppressWarnings( "unused" )
  public static CompatFunction<String, Integer> asInteger( ) {
    return AddressStringToInteger.INSTANCE;
  }

  public static int asInteger( final String address ) {
    return InetAddresses.coerceToInteger( InetAddresses.forString( address ) );
  }

  public static CompatFunction<Integer, String> fromInteger( ) {
    return AddressIntegerToString.INSTANCE;
  }

  public static String fromInteger( final Number address ) {
    return InetAddresses.toAddrString( InetAddresses.fromInteger( address.intValue( ) ) );
  }

  /**
   * Allocate a private address.
   *
   * <p>There must not be an active transaction for private addresses.</p>
   */
  public static String allocate( String scope, String tag, Iterable<Integer> addresses, int addressCount, int allocatedCount ) throws NotEnoughResourcesException {
    return allocator.allocate( scope, tag, addresses, addressCount, allocatedCount );
  }

  public static void associate( String address, VmInstance instance ) throws ResourceAllocationException {
    allocator.associate( address, instance );
  }

  public static void associate( String address, NetworkInterface networkInterface ) throws ResourceAllocationException {
    allocator.associate( address, networkInterface );
  }

  /**
   * Release a private address.
   *
   * <p>There must not be an active transaction for private addresses.</p>
   *
   * @return The tag for the address (if any)
   */
  public static String release( String scope, String address, String ownerId ) {
    return allocator.release( scope, address, ownerId );
  }

  public static boolean verify( String scope, String address, String ownerId ) {
    return allocator.verify( scope, address, ownerId );
  }

  static boolean releasing( Iterable<String> activeAddresses, String partition ) {
    return allocator.releasing( activeAddresses, partition );
  }

  protected PrivateAddressAllocator allocator( ) {
    return allocator;
  }

  private enum AddressIntegerToString implements CompatFunction<Integer, String> {
    INSTANCE;

    @Override
    public String apply( @Nullable final Integer address ) {
      return address == null ? null : fromInteger( address );
    }
  }

  private enum AddressStringToInteger implements CompatFunction<String, Integer> {
    INSTANCE;

    @Override
    public Integer apply( @Nullable final String address ) {
      return address == null ? null : asInteger( address );
    }
  }
}

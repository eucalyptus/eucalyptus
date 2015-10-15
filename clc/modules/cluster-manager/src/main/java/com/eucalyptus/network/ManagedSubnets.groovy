/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.network

import com.eucalyptus.network.config.ManagedSubnet
import com.eucalyptus.network.config.NetworkConfiguration
import com.eucalyptus.util.Pair
import com.google.common.base.Function
import com.google.common.primitives.UnsignedInteger
import groovy.transform.PackageScope

import javax.annotation.Nonnull

/**
 *
 */
class ManagedSubnets {

  static boolean validSegmentForSubnet(
      @Nonnull final ManagedSubnet managedSubnet,
      @Nonnull final Integer segmentId
  ) {
    segmentId == restrictToMaximumSegment( managedSubnet, segmentId )
  }

  static Integer restrictToMaximumSegment(
      @Nonnull final ManagedSubnet managedSubnet,
      @Nonnull final Integer segmentId
  ) {
    restrictToMaximumSegment(
        managedSubnet.subnet,
        managedSubnet.netmask,
        segmentSize( managedSubnet ).intValue( ),
        segmentId )
  }

  static Integer restrictToMaximumSegment(
      @Nonnull final String subnet,
      @Nonnull final String netmask,
      @Nonnull final Integer segmentSize,
      @Nonnull final Integer segmentId
  ) {
    Math.min(
        (int) segmentId,
        (int) ( IPRange.fromSubnet( subnet, netmask ).size( ) + 2 ) / segmentSize )
  }

  /**
   * Retrieves a private IP address from a given network segment index and an IP
   * address index from within the segment.
   *
   * @param segmentId the network segment index to select
   * @param ipIndex the IP address index from within the network segment
   *
   * @return the matching IP address in a String format
   */
  @PackageScope
  @Nonnull
  static String indexToAddress(
      @Nonnull final ManagedSubnet managedSubnet,
      @Nonnull final Integer segmentId,
      @Nonnull final Long ipIndex
  ) {
    final UnsignedInteger subnetInt = subnet( managedSubnet )
    final UnsignedInteger segmentSizeInt = segmentSize( managedSubnet )

    final UnsignedInteger privateIp = subnetInt
        .plus( segmentSizeInt.times( unsigned( segmentId ).minus( unsigned( ManagedSubnet.MIN_VLAN ) ) ) )
        .plus( unsigned( ipIndex.intValue( ) ) )

    PrivateAddresses.fromInteger( privateIp )
  }

  @PackageScope
  static Pair<Integer,Long> addressToIndex(
      @Nonnull final ManagedSubnet managedSubnet,
      @Nonnull final String address
  ) {
    final UnsignedInteger subnetInt = subnet( managedSubnet )
    final UnsignedInteger segmentSizeInt = segmentSize( managedSubnet )

    final UnsignedInteger rebasedAddressInt = unsigned( PrivateAddresses.asInteger( address ) ).minus( subnetInt )

    Pair.pair(
      rebasedAddressInt.dividedBy( segmentSizeInt ).plus( unsigned( ManagedSubnet.MIN_VLAN ) ).intValue( ),
      rebasedAddressInt.mod( segmentSizeInt ).longValue( )
    )
  }

  @PackageScope
  static Function<String,Pair<Integer,Long>> addressToIndex(
      @Nonnull final ManagedSubnet managedSubnet
  ) {
    { String address -> addressToIndex( managedSubnet, address ) } as Function<String,Pair<Integer,Long>>
  }

  @PackageScope
  static Function<NetworkConfiguration,ManagedSubnet> managedSubnet( ) {
    {  NetworkConfiguration networkConfiguration -> networkConfiguration?.managedSubnet } as Function<NetworkConfiguration,ManagedSubnet>
  }

  private static UnsignedInteger subnet( final ManagedSubnet managedSubnet ) {
    unsigned( PrivateAddresses.asInteger( managedSubnet.subnet ) )
  }

  private static UnsignedInteger segmentSize( final ManagedSubnet managedSubnet ) {
    unsigned( managedSubnet.segmentSize ?: ManagedSubnet.DEF_SEGMENT_SIZE )
  }

  private static UnsignedInteger unsigned( int value ) {
    UnsignedInteger.fromIntBits( value )
  }
}

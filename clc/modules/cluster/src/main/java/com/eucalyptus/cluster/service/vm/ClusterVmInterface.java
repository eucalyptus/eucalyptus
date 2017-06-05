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
 ************************************************************************/package com.eucalyptus.cluster.service.vm;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.cluster.common.msgs.NetConfigType;
import com.google.common.base.MoreObjects;

/**
 *
 */
public final class ClusterVmInterface {
  private static final String ADDRESS_NONE = "0.0.0.0";

  private final String interfaceId;
  private final String attachmentId;
  private final Integer device;
  private final String mac;
  private final String privateAddress;
  private final String publicAddress;

  private ClusterVmInterface(
      final String interfaceId,
      final String attachmentId,
      final Integer device,
      final String mac,
      final String privateAddress,
      final String publicAddress
  ) {
    this.interfaceId = interfaceId;
    this.attachmentId = attachmentId;
    this.device = device;
    this.mac = mac;
    this.privateAddress = privateAddress;
    this.publicAddress = Objects.toString( publicAddress, ADDRESS_NONE );
  }

  private ClusterVmInterface(
      final ClusterVmInterface vmInterface,
      final String publicAddress
  ) {
    this(
        vmInterface.getInterfaceId( ),
        vmInterface.getAttachmentId( ),
        vmInterface.getDevice( ),
        vmInterface.getMac(),
        vmInterface.getPrivateAddress( ),
        publicAddress
    );
  }

  public static ClusterVmInterface of(
      final String interfaceId,
      final String attachmentId,
      final Integer device,
      final String mac,
      final String privateAddress,
      final String publicAddress
  ) {
    return new ClusterVmInterface(
        interfaceId,
        attachmentId,
        device,
        mac,
        privateAddress,
        publicAddress
    );
  }

  public static ClusterVmInterface fromNodeInterface( @Nonnull final NetConfigType netConfig ) {
    return of(
        netConfig.getInterfaceId( ),
        netConfig.getAttachmentId( ),
        netConfig.getDevice( ),
        netConfig.getPrivateMacAddress( ),
        netConfig.getPrivateIp(),
        netConfig.getPublicIp( )
    );
  }

  @SuppressWarnings( "WeakerAccess" )
  public ClusterVmInterface withPublic( final String publicAddress ) {
    final String newAddress = Objects.toString( publicAddress, ADDRESS_NONE );
    boolean assign = !newAddress.equals( this.publicAddress );
    if ( assign ) {
      return new ClusterVmInterface( this, newAddress );
    }
    return this;
  }

  public String getAttachmentId() {
    return attachmentId;
  }

  public Integer getDevice() {
    return device;
  }

  public String getInterfaceId() {
    return interfaceId;
  }

  public String getMac() {
    return mac;
  }

  public String getPrivateAddress() {
    return privateAddress;
  }

  public String getPublicAddress() {
    return publicAddress;
  }

  @SuppressWarnings( "WeakerAccess" )
  @Nullable
  public String getDisplayPublicAddress() {
    return ADDRESS_NONE.equals( publicAddress ) ? null : publicAddress;
  }

  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .omitNullValues( )
        .add( "id", getInterfaceId() )
        .add( "attachment-id", getAttachmentId( ) )
        .add( "device", getDevice( ) )
        .add( "mac", getMac( ) )
        .add( "private-address", getPrivateAddress( ) )
        .add( "public-address", getDisplayPublicAddress( ) )
        .toString( );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ClusterVmInterface that = (ClusterVmInterface) o;
    return Objects.equals( getInterfaceId( ), that.getInterfaceId( ) ) &&
        Objects.equals( getAttachmentId( ), that.getAttachmentId( ) ) &&
        Objects.equals( getDevice( ), that.getDevice( ) ) &&
        Objects.equals( getMac( ), that.getMac( ) ) &&
        Objects.equals( getPrivateAddress( ), that.getPrivateAddress( ) ) &&
        Objects.equals( getPublicAddress( ), that.getPublicAddress( ) );
  }

  @Override
  public int hashCode() {
    return Objects.hash( getInterfaceId( ), getAttachmentId( ), getDevice( ), getMac( ), getPrivateAddress( ), getPublicAddress( ) );
  }
}

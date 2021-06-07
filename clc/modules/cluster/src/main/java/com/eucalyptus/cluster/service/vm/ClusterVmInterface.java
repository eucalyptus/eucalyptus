/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

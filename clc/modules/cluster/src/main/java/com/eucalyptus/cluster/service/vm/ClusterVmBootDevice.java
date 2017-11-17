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
 ************************************************************************/
package com.eucalyptus.cluster.service.vm;

import java.util.Objects;
import javax.annotation.Nonnull;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecord;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecordType;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;

/**
 *
 */
public class ClusterVmBootDevice {

  private final String device;
  private final String type;
  private final long size;
  private final String format;
  private final Option<Tuple2<String,String>> resource;

  private ClusterVmBootDevice(
      final String device,
      final String type,
      final long size,
      final String format,
      final Option<Tuple2<String, String>> resource
  ) {
    this.device = Assert.notNull( device, "device" );
    this.type = Assert.notNull( type, "type" );
    this.size = Assert.arg( size, size >= -1L, "invalid size" );
    this.format = Assert.notNull( format, "format" );
    this.resource = Assert.notNull( resource, "resource" );
    this.resource.forEach( r -> {
      Assert.notNull( r._1, "resource identifier" );
      Assert.notNull( r._2, "resource location" );
    } );
  }

  public static ClusterVmBootDevice of(
      @Nonnull final String device,
      @Nonnull final String type,
      final long size,
      @Nonnull final String format,
      @Nonnull final Option<Tuple2<String, String>> resource
  ) {
    return new ClusterVmBootDevice( device, type, size, format, resource );
  }

  public static ClusterVmBootDevice from( final VirtualBootRecord virtualBootRecord ) {
    return of(
        virtualBootRecord.getGuestDeviceName( ),
        virtualBootRecord.getType( ),
        MoreObjects.firstNonNull( virtualBootRecord.getSize(), -1L ),
        virtualBootRecord.getFormat( ),
        "none".equals( Objects.toString( Strings.emptyToNull( virtualBootRecord.getId( ) ), "none" ) ) ?
            Option.none( ) :
            Option.of( Tuple.of( virtualBootRecord.getId( ), virtualBootRecord.getResourceLocation( ) ) )
    );
  }

  public static ClusterVmBootDevice from( final VirtualBootRecordType virtualBootRecord ) {
    return of(
        virtualBootRecord.getGuestDeviceName( ),
        virtualBootRecord.getType( ),
        MoreObjects.firstNonNull( virtualBootRecord.getSize(), -1L ),
        virtualBootRecord.getFormat( ),
        "none".equals( Objects.toString( Strings.emptyToNull( virtualBootRecord.getId( ) ), "none" ) ) ?
            Option.none( ) :
            Option.of( Tuple.of( virtualBootRecord.getId( ), virtualBootRecord.getResourceLocation( ) ) )
    );
  }

  @Nonnull
  public String getDevice() {
    return device;
  }

  /**
   * Type ( boot, ebs, ephemeral, kernel, machine, ramdisk, swap )
   */
  @Nonnull
  public String getType() {
    return type;
  }

  /**
   * Device size in bytes
   */
  public long getSize() {
    return size;
  }

  /**
   * Format for the device
   */
  @Nonnull
  public String getFormat() {
    return format;
  }

  /**
   * Resource id/url tuple (optional)
   */
  @Nonnull
  public Option<Tuple2<String, String>> getResource() {
    return resource;
  }

  @Nonnull
  public String toString() {
    return MoreObjects.toStringHelper( this )
        .add( "device", getDevice( ) )
        .add( "type", getType( ) )
        .add( "size", getSize( ) )
        .add( "format", getFormat( ) )
        .add( "resource", getResource( ) )
        .toString( );
  }
}

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
package com.eucalyptus.cluster.service.vm;

import java.util.Objects;
import javax.annotation.Nonnull;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecord;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecordType;
import com.eucalyptus.util.Assert;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.control.Option;

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

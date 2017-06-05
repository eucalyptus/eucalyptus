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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.apache.http.util.Args;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecord;
import com.eucalyptus.cluster.common.msgs.VirtualBootRecordType;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import javaslang.collection.Stream;

/**
 * Track the root volume and any additional ephemeral/swap disks
 */
public class ClusterVmBootRecord {

  private final List<ClusterVmBootDevice> devices;

  private ClusterVmBootRecord( @Nonnull final Iterable<ClusterVmBootDevice> devices ) {
    this.devices = ImmutableList.copyOf( Args.notNull( devices, "devices" ) );
  }

  public static ClusterVmBootRecord none( ) {
    return new ClusterVmBootRecord( Collections.emptyList( ) );
  }

  public static ClusterVmBootRecord of( @Nonnull final Iterable<ClusterVmBootDevice> devices ) {
    return new ClusterVmBootRecord( devices );
  }

  public static ClusterVmBootRecord from( final Collection<VirtualBootRecord> virtualBootRecords ) {
    return of(
        // everything but non-root ebs volumes
        Stream.ofAll( virtualBootRecords )
            .filter( r -> !"ebs".equals( r.getType( ) ) || r.getGuestDeviceName( ).endsWith( "a" ) || r.getGuestDeviceName( ).endsWith( "a0" )  )
            .map( ClusterVmBootDevice::from )
            .toList( ) );
  }

  public static ClusterVmBootRecord fromNodeRecord( final Collection<VirtualBootRecordType> virtualBootRecords ) {
    return of(
        // everything but non-root ebs volumes
        Stream.ofAll( virtualBootRecords )
            .filter( r -> !"ebs".equals( r.getType( ) ) || r.getGuestDeviceName( ).endsWith( "a" ) || r.getGuestDeviceName( ).endsWith( "a0" )  )
            .map( ClusterVmBootDevice::from )
            .toList( ) );
  }

  public List<ClusterVmBootDevice> getDevices( ) {
    return devices;
  }

  public ClusterVmBootRecord or( final ClusterVmBootRecord other ) {
    return none( ).equals( this ) ? other : this;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final ClusterVmBootRecord that = (ClusterVmBootRecord) o;
    return Objects.equals( getDevices( ), that.getDevices( ) );
  }

  @Override
  public int hashCode() {
    return Objects.hash( getDevices( ) );
  }

  @Nonnull
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "devices", getDevices( ) )
        .toString( );
  }
}

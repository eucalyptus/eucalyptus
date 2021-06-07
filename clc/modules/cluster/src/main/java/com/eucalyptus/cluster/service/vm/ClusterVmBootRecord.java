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
import io.vavr.collection.Stream;

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

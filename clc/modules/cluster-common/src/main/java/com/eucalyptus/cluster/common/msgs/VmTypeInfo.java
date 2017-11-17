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
package com.eucalyptus.cluster.common.msgs;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import com.eucalyptus.binding.HttpEmbedded;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import io.vavr.collection.Stream;

public class VmTypeInfo extends EucalyptusData implements Cloneable {

  private String name;
  private Integer memory;
  private Integer disk;
  private Integer cores;
  private String rootDeviceName;
  @HttpEmbedded( multiple = true )
  private ArrayList<VirtualBootRecord> virtualBootRecord = new ArrayList<>( );

  public VmTypeInfo( ) {
  }

  public VmTypeInfo( String name, Integer memory, Integer disk, Integer cores, String rootDevice ) {
    this.name = name;
    this.memory = memory;
    this.disk = disk;
    this.cores = cores;
    this.rootDeviceName = rootDevice;
  }

  public VmTypeInfo child( ) {
    VmTypeInfo child = new VmTypeInfo( this.name, this.memory, this.disk, this.cores, this.rootDeviceName );
    child.virtualBootRecord.addAll( Stream.ofAll( this.virtualBootRecord ).map( VirtualBootRecord::clone ).toJavaList( ) );
    return child;
  }

  @Override
  public String toString( ) {
    return "VmTypeInfo " + name + " mem=" + String.valueOf( memory ) + " disk=" + String.valueOf( disk ) + " cores=" + String.valueOf( cores );
  }

  public String dump( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "VmTypeInfo " + name + " mem=" + String.valueOf( memory ) + " disk=" + String.valueOf( disk ) + " cores=" + String.valueOf( cores ) + " rootDeviceName=" + rootDeviceName + " " );
    for ( VirtualBootRecord vbr : this.virtualBootRecord ) {
      sb.append( "{VirtualBootRecord deviceName=" ).append( vbr.getGuestDeviceName( ) ).append( " resourceLocation=" ).append( vbr.getResourceLocation( ) ).append( " size=" ).append( vbr.getSize( ) ).append( "} " );
    }

    return sb.toString( );
  }

  public void setEbsRoot( final String imageId, final String iqn, final Long sizeBytes ) {
    final VirtualBootRecord record = new VirtualBootRecord( );
    record.setId( imageId );
    record.setSize( sizeBytes );
    record.setResourceLocation( iqn );
    record.setGuestDeviceName( this.rootDeviceName );
    record.setType( "ebs" );
    this.virtualBootRecord.add( record );
  }

  public void setRoot( String imageId, String location, Long sizeBytes ) {
    final VirtualBootRecord record = new VirtualBootRecord( );
    record.setId( imageId );
    record.setSize( sizeBytes );
    record.setResourceLocation( location );
    record.setGuestDeviceName( this.rootDeviceName );
    record.setType( "machine" );
    this.virtualBootRecord.add( record );
  }

  public void setKernel( String imageId, String location, Long sizeBytes ) {
    final VirtualBootRecord record = new VirtualBootRecord( );
    record.setId( imageId );
    record.setSize( sizeBytes );
    record.setResourceLocation( location );
    record.setType( "kernel" );
    this.virtualBootRecord.add( record );
  }

  public void setRamdisk( String imageId, String location, Long sizeBytes ) {
    final VirtualBootRecord record = new VirtualBootRecord( );
    record.setId( imageId );
    record.setSize( sizeBytes );
    record.setResourceLocation( location );
    record.setType( "ramdisk" );
    this.virtualBootRecord.add( record );
  }

  protected void setSwap( String deviceName, Long sizeBytes, String formatName ) {
    final VirtualBootRecord record = new VirtualBootRecord( );
    record.setGuestDeviceName( deviceName );
    record.setSize( sizeBytes );
    record.setType( "swap" );
    record.setFormat( formatName );
    this.virtualBootRecord.add( record );
  }

  public void setEphemeral( Integer index, String deviceName, Long sizeBytes, String formatName ) {
    final VirtualBootRecord record = new VirtualBootRecord( );
    record.setGuestDeviceName( deviceName );
    record.setSize( sizeBytes );
    record.setType( "ephemeral" );
    record.setFormat( formatName );
    this.virtualBootRecord.add( record );
  }

  public VirtualBootRecord lookupRoot( ) throws NoSuchElementException {
    VirtualBootRecord ret = null;
    if ( ( ret = Stream.ofAll( this.virtualBootRecord ).find( vbr ->  "machine".equals( vbr.getType( ) ) ).getOrElse( ret ) ) == null ) {
      ret = Stream.ofAll( this.virtualBootRecord )
          .find( vbr -> vbr.getType( ).equals( "ebs" ) && ( vbr.getGuestDeviceName( ).equals( VmTypeInfo.this.rootDeviceName ) || vbr.getGuestDeviceName( ).equals( "xvda" ) ) )
          .getOrElse( ret );
    }

    if ( ret != null ) {
      return ret;
    } else {
      throw new NoSuchElementException( "Failed to find virtual boot record of type machine among: " +
          Stream.ofAll( this.virtualBootRecord ).map( Object::toString ).mkString( ", " ) );
    }
  }

  public VirtualBootRecord lookupKernel( ) throws NoSuchElementException {
    VirtualBootRecord ret = null;
    if ( ( ret = Stream.ofAll( this.virtualBootRecord ).find( vbr -> "kernel".equals( vbr.getType( ) ) ).getOrElse( ret ) ) == null ) {
      throw new NoSuchElementException("Failed to find virtual boot record of type kernel among: " +
          Stream.ofAll( this.virtualBootRecord ).map( Object::toString ).mkString( ", " ));
    } else {
      return ret;
    }
  }

  public VirtualBootRecord lookupRamdisk( ) throws NoSuchElementException {
    VirtualBootRecord ret = null;
    if ( ( ret = Stream.ofAll( this.virtualBootRecord ).find( vbr -> "ramdisk".equals( vbr.getType( ) ) ).getOrElse( ret ) ) == null ) {
      throw new NoSuchElementException("Failed to find virtual boot record of type ramdisk among: " +
          Stream.ofAll( this.virtualBootRecord ).map( Object::toString ).mkString( ", " ));
    } else {
      return ret;
    }
  }

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public Integer getMemory( ) {
    return memory;
  }

  public void setMemory( Integer memory ) {
    this.memory = memory;
  }

  public Integer getDisk( ) {
    return disk;
  }

  public void setDisk( Integer disk ) {
    this.disk = disk;
  }

  public Integer getCores( ) {
    return cores;
  }

  public void setCores( Integer cores ) {
    this.cores = cores;
  }

  public String getRootDeviceName( ) {
    return rootDeviceName;
  }

  public void setRootDeviceName( String rootDeviceName ) {
    this.rootDeviceName = rootDeviceName;
  }

  public ArrayList<VirtualBootRecord> getVirtualBootRecord( ) {
    return virtualBootRecord;
  }

  public void setVirtualBootRecord( ArrayList<VirtualBootRecord> virtualBootRecord ) {
    this.virtualBootRecord = virtualBootRecord;
  }
}

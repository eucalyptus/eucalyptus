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
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpEmbedded;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class BlockDeviceMappingItemType extends EucalyptusData {

  private String virtualName;
  private String deviceName;
  private Integer size;
  private String format;
  private Boolean noDevice;
  @HttpEmbedded( multiple = true )
  private EbsDeviceMapping ebs;

  public BlockDeviceMappingItemType( final String virtualName, final String deviceName ) {
    this.virtualName = virtualName;
    this.deviceName = deviceName;
  }

  public BlockDeviceMappingItemType( ) {
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( deviceName == null ) ? 0 : deviceName.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    BlockDeviceMappingItemType other = (BlockDeviceMappingItemType) obj;
    if ( deviceName == null ) {
      if ( other.deviceName != null ) return false;
    } else if ( !deviceName.equals( other.deviceName ) ) return false;
    return true;
  }

  public String getVirtualName( ) {
    return virtualName;
  }

  public void setVirtualName( String virtualName ) {
    this.virtualName = virtualName;
  }

  public String getDeviceName( ) {
    return deviceName;
  }

  public void setDeviceName( String deviceName ) {
    this.deviceName = deviceName;
  }

  public Integer getSize( ) {
    return size;
  }

  public void setSize( Integer size ) {
    this.size = size;
  }

  public String getFormat( ) {
    return format;
  }

  public void setFormat( String format ) {
    this.format = format;
  }

  public Boolean getNoDevice( ) {
    return noDevice;
  }

  public void setNoDevice( Boolean noDevice ) {
    this.noDevice = noDevice;
  }

  public EbsDeviceMapping getEbs( ) {
    return ebs;
  }

  public void setEbs( EbsDeviceMapping ebs ) {
    this.ebs = ebs;
  }
}

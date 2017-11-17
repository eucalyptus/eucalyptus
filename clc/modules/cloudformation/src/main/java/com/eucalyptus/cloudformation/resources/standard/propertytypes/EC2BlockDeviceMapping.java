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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class EC2BlockDeviceMapping {

  @Property
  @Required
  private String deviceName;

  @Property
  private EC2EBSBlockDevice ebs;

  @Property

  private Object noDevice;

  @Property
  private String virtualName;

  public String getDeviceName( ) {
    return deviceName;
  }

  public void setDeviceName( String deviceName ) {
    this.deviceName = deviceName;
  }

  public EC2EBSBlockDevice getEbs( ) {
    return ebs;
  }

  public void setEbs( EC2EBSBlockDevice ebs ) {
    this.ebs = ebs;
  }

  public Object getNoDevice( ) {
    return noDevice;
  }

  public void setNoDevice( Object noDevice ) {
    this.noDevice = noDevice;
  }

  public String getVirtualName( ) {
    return virtualName;
  }

  public void setVirtualName( String virtualName ) {
    this.virtualName = virtualName;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final EC2BlockDeviceMapping that = (EC2BlockDeviceMapping) o;
    return Objects.equals( getDeviceName( ), that.getDeviceName( ) ) &&
        Objects.equals( getEbs( ), that.getEbs( ) ) &&
        Objects.equals( getNoDevice( ), that.getNoDevice( ) ) &&
        Objects.equals( getVirtualName( ), that.getVirtualName( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getDeviceName( ), getEbs( ), getNoDevice( ), getVirtualName( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "deviceName", deviceName )
        .add( "ebs", ebs )
        .add( "noDevice", noDevice )
        .add( "virtualName", virtualName )
        .toString( );
  }
}

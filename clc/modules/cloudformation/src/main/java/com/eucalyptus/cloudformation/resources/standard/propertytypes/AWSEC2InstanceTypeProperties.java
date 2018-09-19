/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class AWSEC2InstanceTypeProperties implements ResourceProperties {

  @Property
  @Required
  private String name;

  @Property
  private Integer cpu;

  @Property
  private Integer disk;

  @Property
  private Integer memory;

  @Property
  private Integer networkInterfaces;

  @Property
  private Boolean enabled;

  public String getName( ) {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  public Integer getCpu( ) {
    return cpu;
  }

  public void setCpu( final Integer cpu ) {
    this.cpu = cpu;
  }

  public Integer getDisk( ) {
    return disk;
  }

  public void setDisk( final Integer disk ) {
    this.disk = disk;
  }

  public Integer getMemory( ) {
    return memory;
  }

  public void setMemory( final Integer memory ) {
    this.memory = memory;
  }

  public Integer getNetworkInterfaces( ) {
    return networkInterfaces;
  }

  public void setNetworkInterfaces( final Integer networkInterfaces ) {
    this.networkInterfaces = networkInterfaces;
  }

  public Boolean getEnabled( ) {
    return enabled;
  }

  public void setEnabled( final Boolean enabled ) {
    this.enabled = enabled;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "name", name )
        .add( "cpu", cpu )
        .add( "disk", disk )
        .add( "memory", memory )
        .add( "networkInterfaces", networkInterfaces )
        .add( "enabled", enabled )
        .toString( );
  }
}

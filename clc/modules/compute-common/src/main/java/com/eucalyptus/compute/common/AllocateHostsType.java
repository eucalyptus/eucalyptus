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
package com.eucalyptus.compute.common;

public class AllocateHostsType extends HostComputeMessage {

  private String autoPlacement;
  private String availabilityZone;
  private String clientToken;
  private String instanceType;
  private Integer quantity;

  public String getAutoPlacement( ) {
    return autoPlacement;
  }

  public void setAutoPlacement( String autoPlacement ) {
    this.autoPlacement = autoPlacement;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }

  public String getInstanceType( ) {
    return instanceType;
  }

  public void setInstanceType( String instanceType ) {
    this.instanceType = instanceType;
  }

  public Integer getQuantity( ) {
    return quantity;
  }

  public void setQuantity( Integer quantity ) {
    this.quantity = quantity;
  }
}

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

import java.util.Date;

public class RequestSpotInstancesType extends SpotInstanceMessage {

  private String spotPrice;
  private Integer instanceCount;
  private String type;
  private Date validFrom;
  private Date validUntil;
  private String launchGroup;
  private String AvailabilityZoneGroup;
  private LaunchSpecificationRequestType launchSpecification;

  public String getSpotPrice( ) {
    return spotPrice;
  }

  public void setSpotPrice( String spotPrice ) {
    this.spotPrice = spotPrice;
  }

  public Integer getInstanceCount( ) {
    return instanceCount;
  }

  public void setInstanceCount( Integer instanceCount ) {
    this.instanceCount = instanceCount;
  }

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public Date getValidFrom( ) {
    return validFrom;
  }

  public void setValidFrom( Date validFrom ) {
    this.validFrom = validFrom;
  }

  public Date getValidUntil( ) {
    return validUntil;
  }

  public void setValidUntil( Date validUntil ) {
    this.validUntil = validUntil;
  }

  public String getLaunchGroup( ) {
    return launchGroup;
  }

  public void setLaunchGroup( String launchGroup ) {
    this.launchGroup = launchGroup;
  }

  public String getAvailabilityZoneGroup( ) {
    return AvailabilityZoneGroup;
  }

  public void setAvailabilityZoneGroup( String AvailabilityZoneGroup ) {
    this.AvailabilityZoneGroup = AvailabilityZoneGroup;
  }

  public LaunchSpecificationRequestType getLaunchSpecification( ) {
    return launchSpecification;
  }

  public void setLaunchSpecification( LaunchSpecificationRequestType launchSpecification ) {
    this.launchSpecification = launchSpecification;
  }
}

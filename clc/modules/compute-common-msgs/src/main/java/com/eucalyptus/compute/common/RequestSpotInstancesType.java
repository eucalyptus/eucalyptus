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

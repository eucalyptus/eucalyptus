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

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SpotFleetRequestConfigType extends EucalyptusData {

  private String clientToken;
  private String iamFleetRole;
  @HttpEmbedded( multiple = true )
  private ArrayList<LaunchSpecificationRequestType> launchSpecifications;
  private String spotPrice;
  private Integer targetCapacity;
  private Boolean terminateInstancesWithExpiration;
  private String validFrom;
  private String validUntil;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }

  public String getIamFleetRole( ) {
    return iamFleetRole;
  }

  public void setIamFleetRole( String iamFleetRole ) {
    this.iamFleetRole = iamFleetRole;
  }

  public ArrayList<LaunchSpecificationRequestType> getLaunchSpecifications( ) {
    return launchSpecifications;
  }

  public void setLaunchSpecifications( ArrayList<LaunchSpecificationRequestType> launchSpecifications ) {
    this.launchSpecifications = launchSpecifications;
  }

  public String getSpotPrice( ) {
    return spotPrice;
  }

  public void setSpotPrice( String spotPrice ) {
    this.spotPrice = spotPrice;
  }

  public Integer getTargetCapacity( ) {
    return targetCapacity;
  }

  public void setTargetCapacity( Integer targetCapacity ) {
    this.targetCapacity = targetCapacity;
  }

  public Boolean getTerminateInstancesWithExpiration( ) {
    return terminateInstancesWithExpiration;
  }

  public void setTerminateInstancesWithExpiration( Boolean terminateInstancesWithExpiration ) {
    this.terminateInstancesWithExpiration = terminateInstancesWithExpiration;
  }

  public String getValidFrom( ) {
    return validFrom;
  }

  public void setValidFrom( String validFrom ) {
    this.validFrom = validFrom;
  }

  public String getValidUntil( ) {
    return validUntil;
  }

  public void setValidUntil( String validUntil ) {
    this.validUntil = validUntil;
  }
}

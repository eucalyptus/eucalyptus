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

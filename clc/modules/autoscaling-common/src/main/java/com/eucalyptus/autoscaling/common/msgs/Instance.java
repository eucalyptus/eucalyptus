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
package com.eucalyptus.autoscaling.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Instance extends EucalyptusData {

  private String instanceId;
  private String availabilityZone;
  private String lifecycleState;
  private String healthStatus;
  private String launchConfigurationName;
  private Boolean protectedFromScaleIn;

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public String getLifecycleState( ) {
    return lifecycleState;
  }

  public void setLifecycleState( String lifecycleState ) {
    this.lifecycleState = lifecycleState;
  }

  public String getHealthStatus( ) {
    return healthStatus;
  }

  public void setHealthStatus( String healthStatus ) {
    this.healthStatus = healthStatus;
  }

  public String getLaunchConfigurationName( ) {
    return launchConfigurationName;
  }

  public void setLaunchConfigurationName( String launchConfigurationName ) {
    this.launchConfigurationName = launchConfigurationName;
  }

  public Boolean getProtectedFromScaleIn( ) {
    return protectedFromScaleIn;
  }

  public void setProtectedFromScaleIn( Boolean protectedFromScaleIn ) {
    this.protectedFromScaleIn = protectedFromScaleIn;
  }
}

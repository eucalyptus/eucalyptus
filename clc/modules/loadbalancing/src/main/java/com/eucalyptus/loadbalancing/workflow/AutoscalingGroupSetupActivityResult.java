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
package com.eucalyptus.loadbalancing.workflow;

import java.util.Set;

public class AutoscalingGroupSetupActivityResult {

  private Set<String> groupNames = null;
  private Set<String> launchConfigNames = null;
  private Set<String> createdGroupNames = null;
  private Set<String> createdLaunchConfigNames = null;
  private Integer numVMsPerZone = null;

  public Set<String> getGroupNames( ) {
    return groupNames;
  }

  public void setGroupNames( Set<String> groupNames ) {
    this.groupNames = groupNames;
  }

  public Set<String> getLaunchConfigNames( ) {
    return launchConfigNames;
  }

  public void setLaunchConfigNames( Set<String> launchConfigNames ) {
    this.launchConfigNames = launchConfigNames;
  }

  public Set<String> getCreatedGroupNames( ) {
    return createdGroupNames;
  }

  public void setCreatedGroupNames( Set<String> createdGroupNames ) {
    this.createdGroupNames = createdGroupNames;
  }

  public Set<String> getCreatedLaunchConfigNames( ) {
    return createdLaunchConfigNames;
  }

  public void setCreatedLaunchConfigNames( Set<String> createdLaunchConfigNames ) {
    this.createdLaunchConfigNames = createdLaunchConfigNames;
  }

  public Integer getNumVMsPerZone( ) {
    return numVMsPerZone;
  }

  public void setNumVMsPerZone( Integer numVMsPerZone ) {
    this.numVMsPerZone = numVMsPerZone;
  }
}

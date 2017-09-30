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
import com.eucalyptus.binding.HttpParameterMapping;

public class StopInstancesType extends VmControlMessage {

  @HttpParameterMapping( parameter = "InstanceId" )
  private ArrayList<String> instancesSet = new ArrayList<String>( );
  private Boolean force;

  public ArrayList<String> getInstancesSet( ) {
    return instancesSet;
  }

  public void setInstancesSet( ArrayList<String> instancesSet ) {
    this.instancesSet = instancesSet;
  }

  public Boolean getForce( ) {
    return force;
  }

  public void setForce( Boolean force ) {
    this.force = force;
  }
}

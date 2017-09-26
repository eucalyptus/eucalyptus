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
import java.util.Collection;
import com.eucalyptus.binding.HttpParameterMapping;

public class DescribeInstanceTypesType extends VmTypeMessage {

  private Boolean verbose = false;
  private Boolean availability = false;
  @HttpParameterMapping( parameter = "InstanceType" )
  private ArrayList<String> instanceTypes = new ArrayList<String>( );

  public DescribeInstanceTypesType( ) {
  }

  public DescribeInstanceTypesType( Collection<String> instanceTypes ) {
    this.instanceTypes.addAll( instanceTypes );
  }

  public Boolean getVerbose( ) {
    return verbose;
  }

  public void setVerbose( Boolean verbose ) {
    this.verbose = verbose;
  }

  public Boolean getAvailability( ) {
    return availability;
  }

  public void setAvailability( Boolean availability ) {
    this.availability = availability;
  }

  public ArrayList<String> getInstanceTypes( ) {
    return instanceTypes;
  }

  public void setInstanceTypes( ArrayList<String> instanceTypes ) {
    this.instanceTypes = instanceTypes;
  }
}

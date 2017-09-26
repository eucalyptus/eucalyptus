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
import com.eucalyptus.binding.HttpParameterMapping;

public class DescribeReservedInstancesType extends ReservedInstanceMessage {

  private ArrayList<String> reservedInstancesId = new ArrayList<String>( );
  private String offeringType;
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );

  public ArrayList<String> getReservedInstancesId( ) {
    return reservedInstancesId;
  }

  public void setReservedInstancesId( ArrayList<String> reservedInstancesId ) {
    this.reservedInstancesId = reservedInstancesId;
  }

  public String getOfferingType( ) {
    return offeringType;
  }

  public void setOfferingType( String offeringType ) {
    this.offeringType = offeringType;
  }

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }
}

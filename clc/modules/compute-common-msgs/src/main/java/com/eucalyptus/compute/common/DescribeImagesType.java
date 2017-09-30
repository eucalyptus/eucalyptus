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

public class DescribeImagesType extends VmImageMessage {

  @HttpParameterMapping( parameter = "ExecutableBy" )
  private ArrayList<String> executableBySet = new ArrayList<String>( );
  @HttpParameterMapping( parameter = "ImageId" )
  private ArrayList<String> imagesSet = new ArrayList<String>( );
  @HttpParameterMapping( parameter = "Owner" )
  private ArrayList<String> ownersSet = new ArrayList<String>( );
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filterSet = new ArrayList<Filter>( );

  public ArrayList<String> getExecutableBySet( ) {
    return executableBySet;
  }

  public void setExecutableBySet( ArrayList<String> executableBySet ) {
    this.executableBySet = executableBySet;
  }

  public ArrayList<String> getImagesSet( ) {
    return imagesSet;
  }

  public void setImagesSet( ArrayList<String> imagesSet ) {
    this.imagesSet = imagesSet;
  }

  public ArrayList<String> getOwnersSet( ) {
    return ownersSet;
  }

  public void setOwnersSet( ArrayList<String> ownersSet ) {
    this.ownersSet = ownersSet;
  }

  public ArrayList<Filter> getFilterSet( ) {
    return filterSet;
  }

  public void setFilterSet( ArrayList<Filter> filterSet ) {
    this.filterSet = filterSet;
  }
}

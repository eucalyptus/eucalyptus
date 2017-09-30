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

/*********************************************************************************/
public class ImportInstanceType extends VmImportMessage {

  private String description;
  private ImportInstanceLaunchSpecification launchSpecification;
  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "DiskImage" )
  private ArrayList<DiskImage> diskImageSet = new ArrayList<DiskImage>( );
  private Boolean keepPartialImports;
  private String platform;

  public void ImportInstance( ) {
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public ImportInstanceLaunchSpecification getLaunchSpecification( ) {
    return launchSpecification;
  }

  public void setLaunchSpecification( ImportInstanceLaunchSpecification launchSpecification ) {
    this.launchSpecification = launchSpecification;
  }

  public ArrayList<DiskImage> getDiskImageSet( ) {
    return diskImageSet;
  }

  public void setDiskImageSet( ArrayList<DiskImage> diskImageSet ) {
    this.diskImageSet = diskImageSet;
  }

  public Boolean getKeepPartialImports( ) {
    return keepPartialImports;
  }

  public void setKeepPartialImports( Boolean keepPartialImports ) {
    this.keepPartialImports = keepPartialImports;
  }

  public String getPlatform( ) {
    return platform;
  }

  public void setPlatform( String platform ) {
    this.platform = platform;
  }
}

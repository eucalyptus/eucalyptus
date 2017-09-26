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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class DiskImage extends EucalyptusData {

  private DiskImageDetail image;
  private String description;
  private DiskImageVolume volume;

  public DiskImageDetail getImage( ) {
    return image;
  }

  public void setImage( DiskImageDetail image ) {
    this.image = image;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public DiskImageVolume getVolume( ) {
    return volume;
  }

  public void setVolume( DiskImageVolume volume ) {
    this.volume = volume;
  }
}

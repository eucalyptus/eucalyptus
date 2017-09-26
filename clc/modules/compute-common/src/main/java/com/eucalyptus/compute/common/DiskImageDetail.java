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

public class DiskImageDetail extends EucalyptusData {

  private String format;
  private Long bytes;
  private String importManifestUrl;

  public String getFormat( ) {
    return format;
  }

  public void setFormat( String format ) {
    this.format = format;
  }

  public Long getBytes( ) {
    return bytes;
  }

  public void setBytes( Long bytes ) {
    this.bytes = bytes;
  }

  public String getImportManifestUrl( ) {
    return importManifestUrl;
  }

  public void setImportManifestUrl( String importManifestUrl ) {
    this.importManifestUrl = importManifestUrl;
  }
}

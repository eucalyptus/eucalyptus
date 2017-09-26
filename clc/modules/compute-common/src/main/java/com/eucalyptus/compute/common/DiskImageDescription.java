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
import net.sf.json.JSONObject;

public class DiskImageDescription extends EucalyptusData {

  private String format;
  private Long size;
  private String importManifestUrl;
  private String checksum;

  public DiskImageDescription( ) {
  }

  public DiskImageDescription( String format, Long size, String importManifestUrl, String checksum ) {
    this.format = format;
    this.size = size;
    this.importManifestUrl = importManifestUrl;
    this.checksum = checksum;
  }

  public DiskImageDescription( JSONObject obj ) {
    if ( obj != null ) {
      format = obj.optString( "format", null );
      size = obj.optLong( "size" );
      importManifestUrl = obj.optString( "importManifestUrl", null );
      checksum = obj.optString( "checksum", null );
    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    obj.put( "format", format );
    obj.put( "size", size );
    obj.put( "importManifestUrl", importManifestUrl );
    obj.put( "checksum", checksum );
    return obj;
  }

  public String getFormat( ) {
    return format;
  }

  public void setFormat( String format ) {
    this.format = format;
  }

  public Long getSize( ) {
    return size;
  }

  public void setSize( Long size ) {
    this.size = size;
  }

  public String getImportManifestUrl( ) {
    return importManifestUrl;
  }

  public void setImportManifestUrl( String importManifestUrl ) {
    this.importManifestUrl = importManifestUrl;
  }

  public String getChecksum( ) {
    return checksum;
  }

  public void setChecksum( String checksum ) {
    this.checksum = checksum;
  }
}

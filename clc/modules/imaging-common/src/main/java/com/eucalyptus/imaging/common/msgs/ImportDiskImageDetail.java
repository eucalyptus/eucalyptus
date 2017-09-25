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
package com.eucalyptus.imaging.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import net.sf.json.JSONObject;

public class ImportDiskImageDetail extends EucalyptusData {

  private String id;
  private String format;
  private Long bytes;
  private String downloadManifestUrl;

  public ImportDiskImageDetail( ) {
  }

  public ImportDiskImageDetail( JSONObject obj ) {
    if ( obj != null ) {
      id = obj.optString( "id", null );
      format = obj.optString( "format", null );
      bytes = obj.optLong( "bytes", 0L );
      downloadManifestUrl = obj.optString( "downloadManifestUrl", null );
    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    obj.put( "id", id );
    obj.put( "format", format );
    obj.put( "bytes", bytes );
    obj.put( "downloadManifestUrl", downloadManifestUrl );
    return obj;
  }

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }

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

  public String getDownloadManifestUrl( ) {
    return downloadManifestUrl;
  }

  public void setDownloadManifestUrl( String downloadManifestUrl ) {
    this.downloadManifestUrl = downloadManifestUrl;
  }
}

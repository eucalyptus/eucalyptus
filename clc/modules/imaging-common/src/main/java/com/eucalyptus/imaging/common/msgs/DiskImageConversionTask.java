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

public class DiskImageConversionTask extends EucalyptusData {

  private String conversionTaskId;
  private String expirationTime;
  private String state;
  private String statusMessage;
  private ImportDiskImage importDisk;

  public DiskImageConversionTask( ) {
  }

  public DiskImageConversionTask( JSONObject obj ) {
    if ( obj != null ) {
      conversionTaskId = obj.optString( "conversionTaskId" );
      expirationTime = obj.optString( "expirationTime" );
      JSONObject diskDetail = obj.optJSONObject( "importDisk" );
      if ( diskDetail != null ) importDisk = new ImportDiskImage( diskDetail );

      state = obj.optString( "state", null );
      statusMessage = obj.optString( "statusMessage", null );
    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    obj.put( "conversionTaskId", conversionTaskId );
    obj.put( "expirationTime", expirationTime );
    if ( importDisk != null ) obj.put( "importDisk", importDisk.toJSON( ) );
    obj.put( "state", state );
    obj.put( "statusMessage", statusMessage );
    return obj;
  }

  public String getConversionTaskId( ) {
    return conversionTaskId;
  }

  public void setConversionTaskId( String conversionTaskId ) {
    this.conversionTaskId = conversionTaskId;
  }

  public String getExpirationTime( ) {
    return expirationTime;
  }

  public void setExpirationTime( String expirationTime ) {
    this.expirationTime = expirationTime;
  }

  public String getState( ) {
    return state;
  }

  public void setState( String state ) {
    this.state = state;
  }

  public String getStatusMessage( ) {
    return statusMessage;
  }

  public void setStatusMessage( String statusMessage ) {
    this.statusMessage = statusMessage;
  }

  public ImportDiskImage getImportDisk( ) {
    return importDisk;
  }

  public void setImportDisk( ImportDiskImage importDisk ) {
    this.importDisk = importDisk;
  }
}

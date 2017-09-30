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

public class ImportInstanceVolumeDetail extends EucalyptusData {

  private Long bytesConverted;
  private String availabilityZone;
  private DiskImageDescription image;
  private String description;
  private DiskImageVolumeDescription volume;
  private String status;
  private String statusMessage;

  public ImportInstanceVolumeDetail( ) {
  }

  public ImportInstanceVolumeDetail( String status, String statusMessage, Long bytesConverted, String availabilityZone, String description, DiskImageDescription image, DiskImageVolumeDescription volume ) {
    this.bytesConverted = bytesConverted;
    this.availabilityZone = availabilityZone;
    this.description = description;
    this.status = status;
    this.statusMessage = statusMessage;
    this.image = image;
    this.volume = volume;
  }

  public ImportInstanceVolumeDetail( JSONObject obj ) {
    if ( obj != null ) {
      bytesConverted = obj.optLong( "bytesConverted" );
      availabilityZone = obj.optString( "availabilityZone", null );
      description = obj.optString( "description", null );
      JSONObject diskDescription = obj.optJSONObject( "image" );
      if ( diskDescription != null ) image = new DiskImageDescription( diskDescription );
      diskDescription = obj.optJSONObject( "volume" );
      if ( diskDescription != null ) volume = new DiskImageVolumeDescription( diskDescription );
      status = obj.optString( "status", null );
      statusMessage = obj.optString( "statusMessage", null );
    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    obj.put( "bytesConverted", bytesConverted );
    obj.put( "availabilityZone", availabilityZone );
    obj.put( "image", image.toJSON( ) );
    obj.put( "description", description );
    obj.put( "volume", volume.toJSON( ) );
    obj.put( "status", status );
    obj.put( "statusMessage", statusMessage );
    return obj;
  }

  public Long getBytesConverted( ) {
    return bytesConverted;
  }

  public void setBytesConverted( Long bytesConverted ) {
    this.bytesConverted = bytesConverted;
  }

  public String getAvailabilityZone( ) {
    return availabilityZone;
  }

  public void setAvailabilityZone( String availabilityZone ) {
    this.availabilityZone = availabilityZone;
  }

  public DiskImageDescription getImage( ) {
    return image;
  }

  public void setImage( DiskImageDescription image ) {
    this.image = image;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public DiskImageVolumeDescription getVolume( ) {
    return volume;
  }

  public void setVolume( DiskImageVolumeDescription volume ) {
    this.volume = volume;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public String getStatusMessage( ) {
    return statusMessage;
  }

  public void setStatusMessage( String statusMessage ) {
    this.statusMessage = statusMessage;
  }
}

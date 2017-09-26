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

public class ImportVolumeTaskDetails extends EucalyptusData {

  private Long bytesConverted;
  private String availabilityZone;
  private String description;
  private DiskImageDescription image;
  private DiskImageVolumeDescription volume;

  public ImportVolumeTaskDetails( ) {
  }

  public ImportVolumeTaskDetails( JSONObject obj ) {
    if ( obj != null ) {
      bytesConverted = obj.optLong( "bytesConverted" );
      availabilityZone = obj.optString( "availabilityZone", null );
      description = obj.optString( "description", null );
      JSONObject diskDescription = obj.optJSONObject( "image" );
      if ( diskDescription != null ) image = new DiskImageDescription( diskDescription );
      diskDescription = obj.optJSONObject( "volume" );
      if ( diskDescription != null ) volume = new DiskImageVolumeDescription( diskDescription );
    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    obj.put( "bytesConverted", bytesConverted );
    obj.put( "availabilityZone", availabilityZone );
    obj.put( "description", description );
    if ( image != null ) obj.put( "image", image.toJSON( ) );
    if ( volume != null ) obj.put( "volume", volume.toJSON( ) );
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

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public DiskImageDescription getImage( ) {
    return image;
  }

  public void setImage( DiskImageDescription image ) {
    this.image = image;
  }

  public DiskImageVolumeDescription getVolume( ) {
    return volume;
  }

  public void setVolume( DiskImageVolumeDescription volume ) {
    this.volume = volume;
  }
}

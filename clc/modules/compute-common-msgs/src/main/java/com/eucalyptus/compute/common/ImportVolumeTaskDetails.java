/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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

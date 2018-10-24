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

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ImportInstanceTaskDetails extends EucalyptusData {

  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "Volume" )
  private ArrayList<ImportInstanceVolumeDetail> volumes = new ArrayList<ImportInstanceVolumeDetail>( );
  private String instanceId;
  private String platform;
  private String description;

  public ImportInstanceTaskDetails( ) {
  }

  public ImportInstanceTaskDetails( String instanceId, String platform, String description, ArrayList<ImportInstanceVolumeDetail> volumes ) {
    this.volumes = volumes;
    this.instanceId = instanceId;
    this.platform = platform;
    this.description = description;
  }

  public ImportInstanceTaskDetails( JSONObject obj ) {
    if ( obj != null ) {
      description = obj.optString( "description", null );
      instanceId = obj.optString( "instanceId", null );
      platform = obj.optString( "platform", null );
      JSONArray arr = obj.optJSONArray( "volumes" );
      if ( arr != null ) {
        for ( int i = 0; i < arr.size( ); i++ ) volumes.add( new ImportInstanceVolumeDetail( arr.getJSONObject( i ) ) );
      } else {
        JSONObject vol = obj.optJSONObject( "volumes" );
        if ( vol != null ) volumes.add( new ImportInstanceVolumeDetail( vol ) );
      }

    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    for ( ImportInstanceVolumeDetail vol : volumes ) obj.accumulate( "volumes", vol.toJSON( ) );
    obj.put( "instanceId", instanceId );
    obj.put( "platform", platform );
    obj.put( "description", description );
    return obj;
  }

  public ArrayList<ImportInstanceVolumeDetail> getVolumes( ) {
    return volumes;
  }

  public void setVolumes( ArrayList<ImportInstanceVolumeDetail> volumes ) {
    this.volumes = volumes;
  }

  public String getInstanceId( ) {
    return instanceId;
  }

  public void setInstanceId( String instanceId ) {
    this.instanceId = instanceId;
  }

  public String getPlatform( ) {
    return platform;
  }

  public void setPlatform( String platform ) {
    this.platform = platform;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }
}

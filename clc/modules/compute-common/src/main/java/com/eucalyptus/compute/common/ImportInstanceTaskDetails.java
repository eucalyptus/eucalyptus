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

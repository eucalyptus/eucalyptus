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

public class ConversionTask extends EucalyptusData {

  private String conversionTaskId;
  private String expirationTime;
  private ImportVolumeTaskDetails importVolume;
  private ImportInstanceTaskDetails importInstance;
  private String state;
  private String statusMessage;
  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "ResourceTag" )
  private ArrayList<ImportResourceTag> resourceTagSet = new ArrayList<ImportResourceTag>( );

  public ConversionTask( ) {
  }

  public ConversionTask( JSONObject obj ) {
    conversionTaskId = obj.optString( "conversionTaskId" );
    expirationTime = obj.optString( "expirationTime" );
    JSONObject importDetails = obj.optJSONObject( "importVolume" );
    if ( importDetails != null ) importVolume = new ImportVolumeTaskDetails( importDetails );
    importDetails = obj.optJSONObject( "importInstance" );
    if ( importDetails != null ) importInstance = new ImportInstanceTaskDetails( importDetails );
    state = obj.optString( "state", null );
    statusMessage = obj.optString( "statusMessage", null );
    JSONArray arr = obj.optJSONArray( "resourceTagSet" );
    if ( arr != null ) {
      for ( int i = 0; i < arr.size( ); i++ ) resourceTagSet.add( new ImportResourceTag( arr.getJSONObject( i ) ) );
    } else {
      JSONObject res = obj.optJSONObject( "resourceTagSet" );
      if ( res != null ) resourceTagSet.add( new ImportResourceTag( res ) );
    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    obj.put( "conversionTaskId", conversionTaskId );
    obj.put( "expirationTime", expirationTime );
    if ( importVolume != null ) obj.put( "importVolume", importVolume.toJSON( ) );
    if ( importInstance != null ) obj.put( "importInstance", importInstance.toJSON( ) );
    obj.put( "state", state );
    obj.put( "statusMessage", statusMessage );
    for ( ImportResourceTag tag : resourceTagSet ) obj.accumulate( "resourceTagSet", tag.toJSON( ) );
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

  public ImportVolumeTaskDetails getImportVolume( ) {
    return importVolume;
  }

  public void setImportVolume( ImportVolumeTaskDetails importVolume ) {
    this.importVolume = importVolume;
  }

  public ImportInstanceTaskDetails getImportInstance( ) {
    return importInstance;
  }

  public void setImportInstance( ImportInstanceTaskDetails importInstance ) {
    this.importInstance = importInstance;
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

  public ArrayList<ImportResourceTag> getResourceTagSet( ) {
    return resourceTagSet;
  }

  public void setResourceTagSet( ArrayList<ImportResourceTag> resourceTagSet ) {
    this.resourceTagSet = resourceTagSet;
  }
}

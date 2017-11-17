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

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
package com.eucalyptus.imaging.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ImportDiskImage extends EucalyptusData {

  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "ImportDiskImageDetail" )
  private ArrayList<ImportDiskImageDetail> diskImageSet = new ArrayList<ImportDiskImageDetail>( );
  private ConvertedImageDetail convertedImage;
  private String description;
  private String accessKey;
  private String uploadPolicy;
  private String uploadPolicySignature;

  public ImportDiskImage( ) {
  }

  public ImportDiskImage( JSONObject obj ) {
    if ( obj != null ) {
      JSONArray arr = obj.optJSONArray( "diskImageSet" );
      if ( arr != null ) {
        for ( int i = 0; i < arr.size( ); i++ ) diskImageSet.add( new ImportDiskImageDetail( arr.getJSONObject( i ) ) );
      } else {
        JSONObject disk = obj.optJSONObject( "diskImageSet" );
        if ( disk != null ) diskImageSet.add( new ImportDiskImageDetail( disk ) );
      }


      description = obj.optString( "description", null );
      JSONObject convertObj = obj.optJSONObject( "convertedImage" );
      if ( convertObj != null ) convertedImage = new ConvertedImageDetail( convertObj );
      accessKey = obj.optString( "accessKey", null );
      uploadPolicy = obj.optString( "uploadPolicy", null );
      uploadPolicySignature = obj.optString( "uploadPolicySignature", null );
    }

  }

  public JSONObject toJSON( ) {
    JSONObject obj = new JSONObject( );
    obj.put( "description", description );
    for ( ImportDiskImageDetail disk : diskImageSet ) obj.accumulate( "diskImageSet", disk.toJSON( ) );
    if ( convertedImage != null ) obj.put( "convertedImage", convertedImage.toJSON( ) );
    obj.put( "accessKey", accessKey );
    obj.put( "uploadPolicy", uploadPolicy );
    obj.put( "uploadPolicySignature", uploadPolicySignature );
    return obj;
  }

  public ArrayList<ImportDiskImageDetail> getDiskImageSet( ) {
    return diskImageSet;
  }

  public void setDiskImageSet( ArrayList<ImportDiskImageDetail> diskImageSet ) {
    this.diskImageSet = diskImageSet;
  }

  public ConvertedImageDetail getConvertedImage( ) {
    return convertedImage;
  }

  public void setConvertedImage( ConvertedImageDetail convertedImage ) {
    this.convertedImage = convertedImage;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getAccessKey( ) {
    return accessKey;
  }

  public void setAccessKey( String accessKey ) {
    this.accessKey = accessKey;
  }

  public String getUploadPolicy( ) {
    return uploadPolicy;
  }

  public void setUploadPolicy( String uploadPolicy ) {
    this.uploadPolicy = uploadPolicy;
  }

  public String getUploadPolicySignature( ) {
    return uploadPolicySignature;
  }

  public void setUploadPolicySignature( String uploadPolicySignature ) {
    this.uploadPolicySignature = uploadPolicySignature;
  }
}

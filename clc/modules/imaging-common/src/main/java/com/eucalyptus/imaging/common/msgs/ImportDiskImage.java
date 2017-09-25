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

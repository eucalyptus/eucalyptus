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

public class InstanceStoreTask extends EucalyptusData {

  private String accountId;
  private String accessKey;
  private String uploadPolicy;
  private String uploadPolicySignature;
  private String s3Url;
  private String serviceCertArn;
  @HttpEmbedded( multiple = true )
  @HttpParameterMapping( parameter = "ImportImage" )
  private ArrayList<ImportDiskImageDetail> importImageSet = new ArrayList<ImportDiskImageDetail>( );
  private ConvertedImageDetail convertedImage;

  public InstanceStoreTask( ) {
  }

  public String getAccountId( ) {
    return accountId;
  }

  public void setAccountId( String accountId ) {
    this.accountId = accountId;
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

  public String getS3Url( ) {
    return s3Url;
  }

  public void setS3Url( String s3Url ) {
    this.s3Url = s3Url;
  }

  public String getServiceCertArn( ) {
    return serviceCertArn;
  }

  public void setServiceCertArn( String serviceCertArn ) {
    this.serviceCertArn = serviceCertArn;
  }

  public ArrayList<ImportDiskImageDetail> getImportImageSet( ) {
    return importImageSet;
  }

  public void setImportImageSet( ArrayList<ImportDiskImageDetail> importImageSet ) {
    this.importImageSet = importImageSet;
  }

  public ConvertedImageDetail getConvertedImage( ) {
    return convertedImage;
  }

  public void setConvertedImage( ConvertedImageDetail convertedImage ) {
    this.convertedImage = convertedImage;
  }
}

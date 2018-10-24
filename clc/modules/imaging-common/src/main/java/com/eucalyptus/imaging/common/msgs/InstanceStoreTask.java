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

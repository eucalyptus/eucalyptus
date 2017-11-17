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
package com.eucalyptus.objectstorage.msgs;

import java.util.ArrayList;
import java.util.List;
import com.eucalyptus.storage.msgs.s3.CommonPrefixesEntry;
import com.eucalyptus.storage.msgs.s3.Upload;

public class ListMultipartUploadsResponseType extends ObjectStorageDataResponseType {

  private String bucket;
  private String keyMarker;
  private String uploadIdMarker;
  private String nextKeyMarker;
  private String nextUploadIdMarker;
  private String delimiter;
  private String prefix;
  private Integer maxUploads;
  private Boolean isTruncated;
  private ArrayList<Upload> uploads = new ArrayList<>( );
  private ArrayList<CommonPrefixesEntry> commonPrefixes = new ArrayList<>( );

  public String getBucket( ) {
    return bucket;
  }

  public void setBucket( String bucket ) {
    this.bucket = bucket;
  }

  public String getKeyMarker( ) {
    return keyMarker;
  }

  public void setKeyMarker( String keyMarker ) {
    this.keyMarker = keyMarker;
  }

  public String getUploadIdMarker( ) {
    return uploadIdMarker;
  }

  public void setUploadIdMarker( String uploadIdMarker ) {
    this.uploadIdMarker = uploadIdMarker;
  }

  public String getNextKeyMarker( ) {
    return nextKeyMarker;
  }

  public void setNextKeyMarker( String nextKeyMarker ) {
    this.nextKeyMarker = nextKeyMarker;
  }

  public String getNextUploadIdMarker( ) {
    return nextUploadIdMarker;
  }

  public void setNextUploadIdMarker( String nextUploadIdMarker ) {
    this.nextUploadIdMarker = nextUploadIdMarker;
  }

  public String getDelimiter( ) {
    return delimiter;
  }

  public void setDelimiter( String delimiter ) {
    this.delimiter = delimiter;
  }

  public String getPrefix( ) {
    return prefix;
  }

  public void setPrefix( String prefix ) {
    this.prefix = prefix;
  }

  public Integer getMaxUploads( ) {
    return maxUploads;
  }

  public void setMaxUploads( Integer maxUploads ) {
    this.maxUploads = maxUploads;
  }

  public Boolean getIsTruncated( ) {
    return isTruncated;
  }

  public void setIsTruncated( Boolean isTruncated ) {
    this.isTruncated = isTruncated;
  }

  public ArrayList<Upload> getUploads( ) {
    return uploads;
  }

  public void setUploads( ArrayList<Upload> uploads ) {
    this.uploads = uploads;
  }

  public ArrayList<CommonPrefixesEntry> getCommonPrefixes( ) {
    return commonPrefixes;
  }

  public void setCommonPrefixes( ArrayList<CommonPrefixesEntry> commonPrefixes ) {
    this.commonPrefixes = commonPrefixes;
  }
}

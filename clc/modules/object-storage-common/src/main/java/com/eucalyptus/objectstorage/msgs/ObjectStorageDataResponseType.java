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
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;

public class ObjectStorageDataResponseType extends ObjectStorageStreamingResponseType implements ObjectStorageCommonResponseType {

  private String etag;
  private Date lastModified;
  private Long size;
  private ArrayList<MetaDataEntry> metaData = new ArrayList<>( );
  private Integer errorCode;
  private String contentType;
  private String contentDisposition;
  private String versionId;
  private Map<String, String> responseHeaderOverrides;
  private String cacheControl;
  private String contentEncoding;
  private String expires;
  private String origin;
  private String httpMethod;
  private String bucket;
  private String bucketName;
  private String bucketUuid;
  private String allowedOrigin;
  private String allowedMethods;
  private String exposeHeaders;
  private String maxAgeSeconds;
  private String allowCredentials;
  private String vary;

  public String getAllowedOrigin( ) {
    return allowedOrigin;
  }

  public void setAllowedOrigin( String allowedOrigin ) {
    this.allowedOrigin = allowedOrigin;
  }

  public String getAllowedMethods( ) {
    return allowedMethods;
  }

  public void setAllowedMethods( String allowedMethods ) {
    this.allowedMethods = allowedMethods;
  }

  public String getExposeHeaders( ) {
    return exposeHeaders;
  }

  public void setExposeHeaders( String exposeHeaders ) {
    this.exposeHeaders = exposeHeaders;
  }

  public String getMaxAgeSeconds( ) {
    return maxAgeSeconds;
  }

  public void setMaxAgeSeconds( String maxAgeSeconds ) {
    this.maxAgeSeconds = maxAgeSeconds;
  }

  public String getAllowCredentials( ) {
    return allowCredentials;
  }

  public void setAllowCredentials( String allowCredentials ) {
    this.allowCredentials = allowCredentials;
  }

  public String getVary( ) {
    return vary;
  }

  public void setVary( String vary ) {
    this.vary = vary;
  }

  public String getEtag( ) {
    return etag;
  }

  public void setEtag( String etag ) {
    this.etag = etag;
  }

  public Date getLastModified( ) {
    return lastModified;
  }

  public void setLastModified( Date lastModified ) {
    this.lastModified = lastModified;
  }

  public Long getSize( ) {
    return size;
  }

  public void setSize( Long size ) {
    this.size = size;
  }

  public ArrayList<MetaDataEntry> getMetaData( ) {
    return metaData;
  }

  public void setMetaData( ArrayList<MetaDataEntry> metaData ) {
    this.metaData = metaData;
  }

  public Integer getErrorCode( ) {
    return errorCode;
  }

  public void setErrorCode( Integer errorCode ) {
    this.errorCode = errorCode;
  }

  public String getContentType( ) {
    return contentType;
  }

  public void setContentType( String contentType ) {
    this.contentType = contentType;
  }

  public String getContentDisposition( ) {
    return contentDisposition;
  }

  public void setContentDisposition( String contentDisposition ) {
    this.contentDisposition = contentDisposition;
  }

  public String getVersionId( ) {
    return versionId;
  }

  public void setVersionId( String versionId ) {
    this.versionId = versionId;
  }

  public Map<String, String> getResponseHeaderOverrides( ) {
    return responseHeaderOverrides;
  }

  public void setResponseHeaderOverrides( Map<String, String> responseHeaderOverrides ) {
    this.responseHeaderOverrides = responseHeaderOverrides;
  }

  public String getCacheControl( ) {
    return cacheControl;
  }

  public void setCacheControl( String cacheControl ) {
    this.cacheControl = cacheControl;
  }

  public String getContentEncoding( ) {
    return contentEncoding;
  }

  public void setContentEncoding( String contentEncoding ) {
    this.contentEncoding = contentEncoding;
  }

  public String getExpires( ) {
    return expires;
  }

  public void setExpires( String expires ) {
    this.expires = expires;
  }

  public String getOrigin( ) {
    return origin;
  }

  public void setOrigin( String origin ) {
    this.origin = origin;
  }

  public String getHttpMethod( ) {
    return httpMethod;
  }

  public void setHttpMethod( String httpMethod ) {
    this.httpMethod = httpMethod;
  }

  public String getBucket( ) {
    return bucket;
  }

  public void setBucket( String bucket ) {
    this.bucket = bucket;
  }

  public String getBucketName( ) {
    return bucketName;
  }

  public void setBucketName( String bucketName ) {
    this.bucketName = bucketName;
  }

  public String getBucketUuid( ) {
    return bucketUuid;
  }

  public void setBucketUuid( String bucketUuid ) {
    this.bucketUuid = bucketUuid;
  }
}

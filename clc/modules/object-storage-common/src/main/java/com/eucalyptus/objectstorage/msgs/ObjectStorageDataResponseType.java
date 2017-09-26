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

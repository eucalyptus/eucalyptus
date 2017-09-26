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

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.objectstorage.ObjectStorage;

@ComponentMessage( ObjectStorage.class )
public class ObjectStorageResponseType extends ObjectStorageRequestType implements ObjectStorageCommonResponseType {

  protected String bucketName;
  protected String bucketUuid;
  protected String allowedOrigin;
  protected String allowedMethods;
  protected String exposeHeaders;
  protected String maxAgeSeconds;
  protected String allowCredentials;
  protected String vary;
  private HttpResponseStatus status;

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

  public HttpResponseStatus getStatus( ) {
    return status;
  }

  public void setStatus( HttpResponseStatus status ) {
    this.status = status;
  }
}

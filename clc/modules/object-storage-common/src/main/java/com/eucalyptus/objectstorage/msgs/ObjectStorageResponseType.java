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

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
import com.eucalyptus.storage.msgs.BucketLogData;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ComponentMessage( ObjectStorage.class )
public class ObjectStorageErrorMessageType extends BaseMessage {

  private String message;
  private String code;
  private HttpResponseStatus status;
  private String resourceType;
  private String resource;
  private String accessKeyId;
  private String stringToSign;
  private String signatureProvided;
  private String stringToSignBytes;
  private String canonicalRequest;
  private String canonicalRequestBytes;
  private String requestId;
  private String hostId;
  private BucketLogData logData;
  private String method;

  public ObjectStorageErrorMessageType( ) {
  }

  public ObjectStorageErrorMessageType( String message, String code, HttpResponseStatus status, String resourceType, String resource, String requestId, String hostId, BucketLogData logData ) {
    this.message = message;
    this.code = code;
    this.status = status;
    this.resourceType = resourceType;
    this.resource = resource;
    this.requestId = requestId;
    this.hostId = hostId;
    this.logData = logData;
  }

  public ObjectStorageErrorMessageType( String message, String code, HttpResponseStatus status, String resourceType, String resource, String requestId, String hostId, BucketLogData logData, String method ) {
    this.message = message;
    this.code = code;
    this.status = status;
    this.resourceType = resourceType;
    this.resource = resource;
    this.requestId = requestId;
    this.hostId = hostId;
    this.logData = logData;
    this.method = method;
  }

  public String getMessage( ) {
    return message;
  }

  public void setMessage( String message ) {
    this.message = message;
  }

  public String getCode( ) {
    return code;
  }

  public void setCode( String code ) {
    this.code = code;
  }

  public HttpResponseStatus getStatus( ) {
    return status;
  }

  public void setStatus( HttpResponseStatus status ) {
    this.status = status;
  }

  public String getResourceType( ) {
    return resourceType;
  }

  public void setResourceType( String resourceType ) {
    this.resourceType = resourceType;
  }

  public String getResource( ) {
    return resource;
  }

  public void setResource( String resource ) {
    this.resource = resource;
  }

  public String getAccessKeyId( ) {
    return accessKeyId;
  }

  public void setAccessKeyId( String accessKeyId ) {
    this.accessKeyId = accessKeyId;
  }

  public String getStringToSign( ) {
    return stringToSign;
  }

  public void setStringToSign( String stringToSign ) {
    this.stringToSign = stringToSign;
  }

  public String getSignatureProvided( ) {
    return signatureProvided;
  }

  public void setSignatureProvided( String signatureProvided ) {
    this.signatureProvided = signatureProvided;
  }

  public String getStringToSignBytes( ) {
    return stringToSignBytes;
  }

  public void setStringToSignBytes( String stringToSignBytes ) {
    this.stringToSignBytes = stringToSignBytes;
  }

  public String getCanonicalRequest( ) {
    return canonicalRequest;
  }

  public void setCanonicalRequest( String canonicalRequest ) {
    this.canonicalRequest = canonicalRequest;
  }

  public String getCanonicalRequestBytes( ) {
    return canonicalRequestBytes;
  }

  public void setCanonicalRequestBytes( String canonicalRequestBytes ) {
    this.canonicalRequestBytes = canonicalRequestBytes;
  }

  public String getRequestId( ) {
    return requestId;
  }

  public void setRequestId( String requestId ) {
    this.requestId = requestId;
  }

  public String getHostId( ) {
    return hostId;
  }

  public void setHostId( String hostId ) {
    this.hostId = hostId;
  }

  public BucketLogData getLogData( ) {
    return logData;
  }

  public void setLogData( BucketLogData logData ) {
    this.logData = logData;
  }

  public String getMethod( ) {
    return method;
  }

  public void setMethod( String method ) {
    this.method = method;
  }
}

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
